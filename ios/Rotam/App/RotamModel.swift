import SwiftUI
import Combine
import FirebaseAuth
import FirebaseFirestore
import WidgetKit

struct SyncReview:Identifiable,Sendable {let id=UUID();let uid:String;let remoteRevision:Int64;let local:[WireRecord];let merged:WireMerge;let preferences:Bool}
struct AiPlanDraft:Identifiable,Sendable {let id=UUID();var plan:WireRecord;let before:WireRecord?;var saved=false}
struct AiMemoryDraft:Identifiable,Sendable {let id=UUID();let text:String;let before:WireRecord?;var saved=false}
struct AiMessage:Identifiable,Sendable {let id=UUID();let user:Bool;let text:String;var plans:[AiPlanDraft]=[];var memories:[AiMemoryDraft]=[];var error=false}

@MainActor final class RotamModel:ObservableObject {
 @Published var records:[WireRecord]=[]
 @Published var account:RotamAccount?
 @Published var ready=false
 @Published var busy=false
 @Published var error:String?
 @Published var toast:String?
 @Published var syncStatus="Planların bu cihazda saklanıyor."
 @Published var syncEnabled=false
 @Published var review:SyncReview?
 @Published var preferencesEnabled=false
 @Published var preferencesStatus="Tercihlerin bu cihazda saklanıyor."
 @Published var prefValues=PortableSettings.defaults
 @Published var messages:[AiMessage]=[]
 @Published var input=""
 @Published var asking=false
 @Published var showWelcome=false
 @Published var notificationStep=false
 var store:RecordStore?
 var preferenceStore:PreferenceStore?
 private var preferenceWatch:ListenerRegistration?
 let gateway=FirebaseGateway()
 private var authHandle:AuthStateDidChangeListenerHandle?
 private var cloudWatch:ListenerRegistration?
 private var syncTask:Task<Void,Never>?
 private var requestTask:Task<Void,Never>?
 private var syncing=false,needsSync=false,profileSyncing=false
 private var pendingInput=""
 private var draftRevision:Int64=0
 private var undoValue:(before:WireRecord,after:WireRecord)?
 let defaults=UserDefaults.standard
 let directory:URL
 var plans:[PortablePlan]{records.filter{$0.kind == .plan}.compactMap{try? PortablePlan($0)}.filter{!$0.deleted}.sorted{($0.date,$0.start ?? $0.end ?? "99:99")<($1.date,$1.start ?? $1.end ?? "99:99")}}
 var today:[PortablePlan]{let day=PlanDates.key(Date());return plans.filter{$0.date==day||($0.date<day&&$0.endDate>=day)}}
 var pending:[PortablePlan]{plans.filter{$0.awaiting()}}
 var sharePlans:Bool{defaults.bool(forKey:"ai-share-plans")}
 var useMemory:Bool{defaults.object(forKey:"ai-use-memory") as? Bool ?? true}
 var motion:Bool{prefValues["animations"]?.boolean ?? true}
 @Published var memory:[WireRecord]=[]
 var selectedDay=PlanDates.key(Date())
 init(){
  directory=FileManager.default.urls(for:.applicationSupportDirectory,in:.userDomainMask)[0].appendingPathComponent("Rotam",isDirectory:true)
  for(k,fallback)in PortableSettings.defaults{let key="portable-\(k)";if fallback.boolean != nil{prefValues[k] = .bool(defaults.object(forKey:key) as? Bool ?? fallback.boolean!)}else{prefValues[k] = .integer((defaults.object(forKey:key) as? NSNumber)?.int64Value ?? fallback.integer!)}}
  account=gateway.account
  authHandle=Auth.auth().addStateDidChangeListener{[weak self] _,user in
   let a=user.map{RotamAccount(uid:$0.uid,name:$0.displayName,email:$0.email)}
   Task{@MainActor [weak self] in await self?.accountChanged(a)}
  }
 }
 func start()async {
  if ready{return}
  do{
   let root=directory
   let marker=root.appendingPathComponent("install-consent-v1")
   if !FileManager.default.fileExists(atPath:marker.path){
    defaults.set(false,forKey:"ai-share-plans");defaults.removeObject(forKey:"active-sync-uid");defaults.removeObject(forKey:"active-preferences-uid")
    if !FileManager.default.fileExists(atPath:root.appendingPathComponent("records.json").path){defaults.set(false,forKey:"welcome-completed")}
   }
   let opened=try await Task.detached{try RecordStore(directory:root)}.value
   store=opened;try await opened.ensure(from:PlanDates.addDays(PlanDates.key(Date()),-7),to:PlanDates.addDays(PlanDates.key(Date()),400));preferenceStore=try PreferenceStore(directory:directory);try Data("1".utf8).write(to:marker,options:[.atomic,.completeFileProtectionUntilFirstUserAuthentication]);ready=true;await reload();await restoreDraft();await publishWidgets()
   showWelcome = !defaults.bool(forKey:"welcome-completed")&&plans.isEmpty
   notificationStep=account != nil
   await configureSync()
  }catch{self.error="Cihaz kayıtları açılamadı. Dosyalar korundu. \(error.localizedDescription)"}
 }
 func reload()async {
  guard let store else{return};records=await store.all()
  memory=if let uid=account?.uid{await store.memory(owner:uid)}else{[]}
  syncEnabled=(await store.snapshot()).syncEnabled
 }
 private func accountChanged(_ value:RotamAccount?)async{
  let previous=account?.uid
  if previous != value?.uid{saveDraft(owner:previous);requestTask?.cancel();asking=false;messages=[];pendingInput="";input="";defaults.set(false,forKey:"ai-share-plans")}
  account=value
  if ready{await reload();if previous != value?.uid{await restoreDraft()};await configureSync()}
 }
 func signIn()async{await perform{try await self.gateway.signIn();await self.accountChanged(self.gateway.account);self.notificationStep=true}}
 func signOut()async{await perform{try self.gateway.signOut();await self.accountChanged(nil)}}
 func finishWelcome(){defaults.set(true,forKey:"welcome-completed");showWelcome=false}
 func setPreference(_ key:String,_ value:JSONValue){
  guard PortableSettings.defaults[key] != nil else{return}
  var changed=prefValues;changed[key]=value
  do{try PortableSettings.validate(changed);prefValues=changed
   switch value{case .bool(let b):defaults.set(b,forKey:"portable-\(key)");case .integer(let n):defaults.set(n,forKey:"portable-\(key)");default:break}
   requestSync();Task{await syncPreferences();await ReminderService.shared.reschedule()}
  }catch{report(error)}
 }
 func setSharePlans(_ value:Bool){defaults.set(value,forKey:"ai-share-plans");objectWillChange.send()}
 func setUseMemory(_ value:Bool){defaults.set(value,forKey:"ai-use-memory");objectWillChange.send()}
 func create(_ plan:WireRecord)async{await perform{guard let store=self.store else{return};try await store.createPlan(plan);await self.changed("Plan kaydedildi")}}
 func edit(_ proposal:WireRecord,before:WireRecord,future:Bool=false)async{await perform{guard let store=self.store else{return};if future{try await store.editFuture(proposal,before:before)}else{try await store.updatePlan(proposal,before:before)};await self.changed("Plan güncellendi")}}
 func outcome(_ plan:PortablePlan,_ status:String,amount:Double?=nil,target:Double?=nil)async{
  await perform{guard let store=self.store else{return};let next=try PlanActions.outcome(plan.record,status:status,amount:amount,target:target);try await store.replace(next,expected:plan.record);self.undoValue=(plan.record,next);await self.changed("Plan durumu kaydedildi")}
 }
 func delete(_ plan:PortablePlan)async{await perform{guard let store=self.store else{return};var e=plan.metadata;e.removeValue(forKey:"pausedByRoutine");let next=try plan.record.changing(["isDeleted":.bool(true),"extraData":.string(JSONValue.object(e).canonical())]);try await store.replace(next,expected:plan.record);self.undoValue=(plan.record,next);await self.changed("Plan silindi")}}
 func undo()async{await perform{guard let pair=self.undoValue,let store=self.store else{return};try await store.replace(pair.before,expected:pair.after);self.undoValue=nil;await self.changed("İşlem geri alındı")}}
 func postpone(_ plan:PortablePlan,date:String,time:String?,future:Bool=false)async{await perform{guard let store=self.store else{return};let next=try PlanActions.postpone(plan.record,date:date,time:time);if future{try await store.editFuture(next,before:plan.record);self.undoValue=nil}else{try await store.replace(next,expected:plan.record);self.undoValue=(plan.record,next)};await self.changed("Plan ertelendi")}}
 func toggleSubtask(_ plan:PortablePlan,task:String,done:Bool)async{await perform{guard let store=self.store else{return};try await store.replace(PlanActions.subtask(plan.record,task:task,done:done),expected:plan.record);await self.changed(nil)}}
 func remember(_ text:String,cloud:Bool,before:WireRecord?=nil)async{await perform{guard let uid=self.account?.uid,let store=self.store else{throw RotamDataError.invalid("Önce Google hesabınla giriş yap.")};try await store.saveMemory(owner:uid,text:text,cloud:cloud,uid:before?.uid,expected:before);await self.changed("Hafızaya kaydedildi")}}
 func forget(_ record:WireRecord)async{await perform{guard let uid=self.account?.uid,let store=self.store else{return};try await store.forgetMemory(owner:uid,record:record);await self.changed("Bu bilgi unutuldu")}}
 func enableSync()async{await perform{guard let uid=self.account?.uid,let store=self.store else{throw RotamDataError.invalid("Önce Google hesabınla giriş yap.")};try await store.bind(uid);await self.configureSync();await self.sync()}}
 func pauseSync()async{await perform{try await self.store?.pauseSync();self.defaults.removeObject(forKey:"active-sync-uid");self.cloudWatch?.remove();self.cloudWatch=nil;self.preferenceWatch?.remove();self.preferenceWatch=nil;self.defaults.removeObject(forKey:"active-preferences-uid");self.syncEnabled=false;self.syncStatus="Eşitleme durdu. Kayıtların cihazında ve hesabında korunuyor."}}
 private func configureSync()async{
  cloudWatch?.remove();cloudWatch=nil;preferenceWatch?.remove();preferenceWatch=nil;defaults.removeObject(forKey:"active-sync-uid");defaults.removeObject(forKey:"active-preferences-uid")
  guard let store,let uid=account?.uid else{syncStatus="Planların bu cihazda saklanıyor.";return}
  let data=await store.snapshot()
  guard data.syncEnabled,data.boundUID==uid else{syncStatus=data.boundUID != nil&&data.boundUID != uid ? "Bu cihazın kayıtları başka bir hesaba bağlı.":"Planların bu cihazda saklanıyor.";return}
  defaults.set(uid,forKey:"active-sync-uid")
  cloudWatch=Firestore.firestore().document("users/\(uid)/sync/snapshot").addSnapshotListener{[weak self] snapshot,error in
   let shouldRefresh=snapshot.map{!$0.metadata.isFromCache} ?? false
   if shouldRefresh{Task{@MainActor [weak self] in self?.requestSync()}}
   if error != nil{Task{@MainActor [weak self] in self?.syncStatus="Bulut bağlantısı tamamlanamadı. Cihaz kayıtların korunuyor."}}
  }
  if let checkpoint=await preferenceStore?.snapshot(),checkpoint.enabled,checkpoint.uid==uid{
   preferencesEnabled=true;defaults.set(uid,forKey:"active-preferences-uid")
   preferenceWatch=Firestore.firestore().document("users/\(uid)/preferences/snapshot").addSnapshotListener{[weak self] snapshot,_ in
    if snapshot?.metadata.isFromCache==false{Task{@MainActor [weak self] in await self?.syncPreferences()}}
   }
  }else{preferencesEnabled=false}
  requestSync()
 }
 func requestSync(){
  guard defaults.string(forKey:"active-sync-uid") != nil else{return}
  syncTask?.cancel();syncTask=Task{try? await Task.sleep(for:.seconds(1));guard !Task.isCancelled else{return};await sync()}
 }
 func sync()async{
  if syncing{needsSync=true;return}
  guard review==nil,let uid=account?.uid,let store,defaults.string(forKey:"active-sync-uid")==uid else{return}
  syncing=true;syncStatus="Planların eşitleniyor…"
  defer{syncing=false;if needsSync{needsSync=false;requestSync()}}
  do{
   let data=await store.snapshot();let before=await store.cloudRecords(owner:uid)
   let remote=try await gateway.read(uid:uid)
   guard remote.revision>=data.baseline.revision else{throw RotamDataError.invalid("Önceki bulut kopyası bulunamadı. Yerel kayıtlar korunarak eşitleme durdu.")}
   let merged=try CoreValidator.merge(base:data.baseline.index(),local:SyncEnvelope(records:before).index(),remote:remote.index(),owner:uid)
   if !merged.conflicts.isEmpty{review=SyncReview(uid:uid,remoteRevision:remote.revision,local:before,merged:merged,preferences:false);syncStatus="Farklı değişiklikler var; incelemeni bekliyor.";return}
   let next=Array(merged.records.values);let revision=merged.records == (try remote.index()) ? remote.revision:try await gateway.write(uid:uid,expected:remote.revision,records:next)
   let applied=try await store.applyRemote(expected:before,incoming:next,baseline:SyncEnvelope(revision:revision,records:next),owner:uid)
   if !applied{needsSync=true;syncStatus="Yeni yerel değişiklik korunuyor; tekrar eşitlenecek.";return}
   await reload();await publishWidgets();await ReminderService.shared.reschedule();syncStatus="Planların güncel.";await syncPreferences()
  }catch{syncStatus=friendly(error)}
 }
 func resolveSync(local:Bool)async{
  guard let current=review,let store,account?.uid==current.uid else{return}
  if current.preferences{await resolvePreferences(current,local:local);return}
  await perform{
   let before=await store.cloudRecords(owner:current.uid)
   guard try SyncEnvelope(records:before).index()==SyncEnvelope(records:current.local).index()else{self.review=nil;self.requestSync();throw RotamDataError.invalid("Kayıtlar incelemeden sonra değişti. Güncel sürüm tekrar gösterilecek.")}
   let remote=try await self.gateway.read(uid:current.uid)
   guard remote.revision==current.remoteRevision else{self.review=nil;self.requestSync();throw RotamDataError.invalid("Diğer cihazın verisi değişti. Güncel hâlini yeniden incele.")}
   let chosen=Array(current.merged.choosingLocal(local).values);try CoreValidator.validate(chosen,owner:current.uid)
   let revision=try await self.gateway.write(uid:current.uid,expected:remote.revision,records:chosen)
   guard try await store.applyRemote(expected:before,incoming:chosen,baseline:SyncEnvelope(revision:revision,records:chosen),owner:current.uid)else{throw RotamDataError.invalid("Yeni yerel değişiklik korunuyor. Yeniden eşitle.")}
   self.review=nil;await self.reload();self.syncStatus="Planların güncel."
  }
 }
 private func changed(_ message:String?)async{await reload();if let message{toast=message};requestSync();await publishWidgets();await ReminderService.shared.reschedule();WidgetCenter.shared.reloadAllTimelines();Feedback.shared.success(prefValues)}
 func perform(_ action:()async throws->Void)async{if busy{return};busy=true;defer{busy=false};do{try await action()}catch is CancellationError{}catch{report(error)}}
 func report(_ error:Error){self.error=friendly(error)}
 func friendly(_ error:Error)->String{
  if error is RotamDataError{return error.localizedDescription}
  let text=error.localizedDescription.lowercased()
  if text.contains("app check")||text.contains("attest"){return "Uygulama doğrulaması tamamlanamadı. Bu iOS kurulumunun güvenli bağlantısı kontrol edilmeli; yerel kayıtların korunuyor."}
  if text.contains("quota")||text.contains("429")||text.contains("resource_exhausted"){return "Hizmetin kullanım sınırına ulaşıldı. Bir süre sonra yeniden deneyebilirsin; kayıtların korunuyor."}
  if text.contains("permission")||text.contains("403"){return "Hizmet erişimi doğrulanamadı. Hesap ve uygulama bağlantısı kontrol edilmeli; yerel kayıtların korunuyor."}
  return "Bağlantı veya işlem tamamlanamadı. Yeniden deneyebilirsin. Yerel kayıtların korunuyor."
 }
 private func draftURL(_ owner:String?)->URL{directory.appendingPathComponent("draft-"+PlanActions.ownerHash(owner ?? "local-guest")+".json")}
 func setInput(_ value:String){input=String(value.prefix(5000));if !asking{pendingInput=""};saveDraft(owner:account?.uid)}
 private func saveDraft(owner:String?){
  draftRevision+=1;let revision=draftRevision;let text=input,pending=pendingInput,url=draftURL(owner)
  Task{do{try await store?.saveDraft(url:url,input:text,pending:pending,revision:revision)}catch{report(error)}}
 }
 private func restoreDraft()async{
  guard let store else{return};let owner=account?.uid
  do{let draft=try await store.loadDraft(url:draftURL(owner));guard account?.uid==owner else{return};input=draft.input.isEmpty ? draft.pending:draft.input}
  catch{report(error)}
 }
}


extension RotamModel {
 func send() {
  let text=input.trimmingCharacters(in:.whitespacesAndNewlines)
  guard !text.isEmpty,!asking else{return}
  let local=LocalSearch.search(text,plans:plans)
  if let answer=local.answer,!local.needsAI{messages.append(AiMessage(user:true,text:text));messages.append(AiMessage(user:false,text:answer));input="";saveDraft(owner:account?.uid);return}
  guard let owner=account?.uid else{error="Rotam AI için Google hesabınla giriş yap. Elle plan eklemek için hesap gerekmez.";return}
  pendingInput=text;input="";saveDraft(owner:owner);messages.append(AiMessage(user:true,text:text));asking=true
  let context=sharePlans ? plans:[],memoryContext=useMemory ? memory:[],day=selectedDay
  let conversation=messages.suffix(12).map{($0.user ? "Kullanıcı: ":"Rotam: ")+$0.text},existing=records
  requestTask=Task {
   defer{if account?.uid==owner{asking=false}}
   do{
    let raw=try await gateway.ask(messages:conversation,plans:context,memories:memoryContext,owner:owner,day:day)
    try Task.checkCancellation();guard account?.uid==owner else{return}
    let parsed=try AiDraftParser.parse(raw,existing:sharePlans ? existing:[],memories:memoryContext)
    messages.append(AiMessage(user:false,text:parsed.text,plans:parsed.plans.map{AiPlanDraft(plan:$0.plan,before:$0.before)},memories:parsed.memories.map{AiMemoryDraft(text:$0.text,before:$0.before)}))
    pendingInput="";saveDraft(owner:owner)
   }catch is CancellationError{}catch{
    guard account?.uid==owner else{return}
    messages.append(AiMessage(user:false,text:friendly(error),error:true))
    if input.isEmpty{input=text};pendingInput="";saveDraft(owner:owner)
   }
  }
 }
 func cancelAi(){requestTask?.cancel();requestTask=nil;asking=false;if input.isEmpty{input=pendingInput};pendingInput="";saveDraft(owner:account?.uid)}
 func approveMessage(_ id:UUID)async {
  await perform {
   guard let index=self.messages.firstIndex(where:{$0.id==id}),let store=self.store else{return}
   let drafts=self.messages[index].plans.filter{!$0.saved}
   try await store.approve(drafts.map{PlanProposal(plan:$0.plan,before:$0.before)})
   for i in self.messages[index].plans.indices{self.messages[index].plans[i].saved=true}
   await self.changed("Planlar onaylandı ve kaydedildi")
  }
 }
 func reviseDraft(message:UUID,draft:UUID,plan:WireRecord){
  guard let m=messages.firstIndex(where:{$0.id==message}),let p=messages[m].plans.firstIndex(where:{$0.id==draft}),!messages[m].plans[p].saved else{return}
  messages[m].plans[p].plan=plan
 }
 func approveMemory(message:UUID,draft:UUID,cloud:Bool)async {
  guard let m=messages.firstIndex(where:{$0.id==message}),let d=messages[m].memories.firstIndex(where:{$0.id==draft}),!messages[m].memories[d].saved else{return}
  let proposal=messages[m].memories[d]
  await perform {
   guard let owner=self.account?.uid,let store=self.store else{return}
   try await store.saveMemory(owner:owner,text:proposal.text,cloud:cloud,uid:proposal.before?.uid,expected:proposal.before)
   self.messages[m].memories[d].saved=true;await self.changed("Bilgi onayınla hatırlandı")
  }
 }
}


extension RotamModel {
 func enablePreferences()async{await perform{guard let uid=self.account?.uid,self.defaults.string(forKey:"active-sync-uid")==uid else{throw RotamDataError.invalid("Önce planlarını hesabına bağla ve eşitle.")};try await self.preferenceStore?.enable(uid);await self.configureSync();await self.syncPreferences()}}
 func pausePreferences()async{await perform{try await self.preferenceStore?.pause();self.preferencesEnabled=false;self.defaults.removeObject(forKey:"active-preferences-uid");self.preferenceWatch?.remove();self.preferenceWatch=nil;self.preferencesStatus="Tercih eşitleme durdu. Cihaz seçimlerin korundu."}}
 func syncPreferences()async{
  guard !profileSyncing,review==nil,let uid=account?.uid,defaults.string(forKey:"active-preferences-uid")==uid,let preferenceStore else{return}
  profileSyncing=true;defer{profileSyncing=false}
  do{
   let checkpoint=await preferenceStore.snapshot(),values=prefValues,local=try PortableSettings.encode(values),remote=try await gateway.read(uid:uid,preferences:true)
   guard remote.revision>=checkpoint.baseline.revision else{throw RotamDataError.invalid("Tercih bulut sürümü geriye gitti. Seçimlerin korundu.")}
   let merged=try ThreeWayMerge.merge(base:checkpoint.baseline.index(),local:SyncEnvelope(records:local).index(),remote:remote.index())
   if !merged.conflicts.isEmpty{review=SyncReview(uid:uid,remoteRevision:remote.revision,local:local,merged:merged,preferences:true);preferencesStatus="Tercih farkları onayını bekliyor.";return}
   let records=Array(merged.records.values),next=try PortableSettings.decode(records),revision=merged.records == (try remote.index()) ? remote.revision:try await gateway.write(uid:uid,expected:remote.revision,records:records,preferences:true)
   guard prefValues==values,account?.uid==uid,defaults.string(forKey:"active-preferences-uid")==uid else{preferencesStatus="Yeni yerel tercih korundu. Tekrar eşitle.";return}
   applyPreferences(next);try await preferenceStore.checkpoint(SyncEnvelope(revision:revision,records:records),uid:uid);preferencesStatus="Tercihlerin güncel."
  }catch{preferencesStatus=friendly(error)}
 }
 private func applyPreferences(_ values:[String:JSONValue]){
  prefValues=values
  for(k,v)in values{switch v{case .bool(let b):defaults.set(b,forKey:"portable-"+k);case .integer(let n):defaults.set(n,forKey:"portable-"+k);default:break}}
  Task{await ReminderService.shared.reschedule()}
 }
 private func resolvePreferences(_ current:SyncReview,local:Bool)async{
  await perform{
   guard self.account?.uid==current.uid,try SyncEnvelope(records:PortableSettings.encode(self.prefValues)).index()==SyncEnvelope(records:current.local).index()else{self.review=nil;throw RotamDataError.invalid("Tercihler incelemeden sonra değişti. Yeniden eşitle.")}
   let remote=try await self.gateway.read(uid:current.uid,preferences:true)
   guard remote.revision==current.remoteRevision else{self.review=nil;throw RotamDataError.invalid("Diğer cihazın tercihleri değişti. Yeniden eşitle.")}
   let rows=Array(current.merged.choosingLocal(local).values),values=try PortableSettings.decode(rows),before=self.prefValues
   let revision=try await self.gateway.write(uid:current.uid,expected:remote.revision,records:rows,preferences:true)
   guard self.prefValues==before,self.account?.uid==current.uid,self.defaults.string(forKey:"active-preferences-uid")==current.uid else{self.review=nil;throw RotamDataError.invalid("Yeni cihaz tercihi korundu. Yeniden eşitle.")}
   self.applyPreferences(values);try await self.preferenceStore?.checkpoint(SyncEnvelope(revision:revision,records:rows),uid:current.uid);self.review=nil;self.preferencesStatus="Tercihlerin güncel."
  }
 }
}


extension RotamModel {
 func setRoutinePaused(_ uid:String,paused:Bool)async{await perform{try await self.store?.pauseRoutine(uid,paused:paused);await self.changed(paused ? "Rutine ara verildi":"Rutin devam ediyor")}}
 func ensureRange(from:String,to:String)async{
  do{if try await store?.ensure(from:from,to:to)==true{await reload();requestSync();await publishWidgets();await ReminderService.shared.reschedule()}}catch{report(error)}
 }
}
