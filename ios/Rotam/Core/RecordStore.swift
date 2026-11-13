import Foundation

public struct LocalDataset:Codable,Sendable {
 public var version=1
 public var records:[WireRecord]=[]
 public var privateMemories:[WireRecord]=[]
 public var boundUID:String?
 public var syncEnabled=false
 public var baseline=SyncEnvelope()
 public init(){}
}
/** A single actor owns local mutations; remote imports compare the exact reviewed state. */
public actor RecordStore {
 private let directory:URL
 private let file:URL
 private var state:LocalDataset
 private var draftRevisions:[URL:Int64]=[:]
 public init(directory:URL)throws {
  self.directory=directory;file=directory.appendingPathComponent("records.json")
  try FileManager.default.createDirectory(at:directory,withIntermediateDirectories:true)
  #if os(iOS)
  var protectedDirectory=directory;var flags=URLResourceValues();flags.isExcludedFromBackup=true;try protectedDirectory.setResourceValues(flags)
  #endif
  if FileManager.default.fileExists(atPath:file.path){
   let bytes=try Data(contentsOf:file);guard bytes.count<=SyncCodec.maxExpanded else{throw RotamDataError.invalid("Cihaz kaydı beklenenden büyük. Dosya korundu.")}
   let decoded=try JSONDecoder().decode(LocalDataset.self,from:bytes)
   guard decoded.version==1 else{throw RotamDataError.invalid("Cihaz kayıt sürümü desteklenmiyor.")}
   try CoreValidator.validate(decoded.records)
   try CoreValidator.validate(decoded.privateMemories)
   guard decoded.privateMemories.allSatisfy({$0.kind == .memory})else{throw RotamDataError.invalid("Özel hafıza biçimi doğrulanamadı.")}
   state=decoded
  }else{state=LocalDataset()}
 }
 private func write(_ value:LocalDataset,to url:URL)throws {
  let e=JSONEncoder();e.outputFormatting=[.sortedKeys,.withoutEscapingSlashes];let data=try e.encode(value)
  #if os(iOS)
  try data.write(to:url,options:[.atomic,.completeFileProtectionUntilFirstUserAuthentication])
  #else
  try data.write(to:url,options:.atomic)
  #endif
 }
 private func commit(_ next:LocalDataset)throws{try CoreValidator.validate(next.records);try write(next,to:file);state=next}
 public func snapshot()->LocalDataset{state}
 public func all()->[WireRecord]{state.records}
 public func cloudRecords(owner:String)->[WireRecord]{state.records.filter{r in r.kind != .memory || (try? r.fields()["ownerUid"]?.string)==owner}}
 public func bind(_ uid:String)throws{
  guard !uid.isEmpty,uid.utf16.count<=128,state.boundUID==nil || state.boundUID==uid else{throw RotamDataError.invalid("Bu cihazın kayıtları başka bir hesaba bağlı. Bağlı hesabınla devam et.")}
  var next=state;next.boundUID=uid;next.syncEnabled=true;try commit(next)
 }
 public func pauseSync()throws{var next=state;next.syncEnabled=false;try commit(next)}
 public func replace(_ record:WireRecord,expected:WireRecord?=nil)throws {
  var map=try SyncEnvelope(records:state.records).index()
  if let expected{guard map[record.key]==expected else{throw RotamDataError.invalid("Plan başka bir işlemde değişti. Güncel kaydı kontrol et.")}}
  map[record.key]=record
  var next=state;next.records=Array(map.values);try commit(next)
 }
 public func replaceMany(_ incoming:[WireRecord],expected:[WireRecord]?=nil)throws {
  if let expected{guard try SyncEnvelope(records:state.records).index()==SyncEnvelope(records:expected).index()else{throw RotamDataError.invalid("Kayıtlar değişti. Güncel önizlemeyi kontrol et.")}}
  var map=try SyncEnvelope(records:state.records).index();for r in incoming{map[r.key]=r}
  var next=state;next.records=Array(map.values);try commit(next)
 }
 public func applyRemote(expected:[WireRecord],incoming:[WireRecord],baseline:SyncEnvelope,owner:String)throws->Bool {
  guard state.boundUID==owner,state.syncEnabled else{throw RotamDataError.invalid("Hesap bağlantısı veya eşitleme izni değişti.")}
  guard try SyncEnvelope(records:cloudRecords(owner:owner)).index()==SyncEnvelope(records:expected).index()else{return false}
  try CoreValidator.validate(incoming,owner:owner)
  try write(state,to:directory.appendingPathComponent("before-cloud-sync.json"))
  let privateKeys=Set(state.privateMemories.map(\.key))
  guard incoming.allSatisfy({!privateKeys.contains($0.key)})else{throw RotamDataError.invalid("Cihazda özel tutulan hafıza korunuyor. Paylaşımını önce Gizlilik bölümünde kontrol et.")}
  let foreignMemories=state.records.filter{r in r.kind == .memory && (try? r.fields()["ownerUid"]?.string) != owner}
  var next=state;next.records=incoming+foreignMemories;next.baseline=baseline;try commit(next);return true
 }
 public func memory(owner:String)->[WireRecord]{(state.records+state.privateMemories).filter{r in guard r.kind == .memory,let d=try? r.fields()else{return false};return d["ownerUid"]?.string==owner&&d["isDeleted"]?.boolean != true}}
 public func saveMemory(owner:String,text:String,cloud:Bool,uid:String?=nil,expected:WireRecord?=nil)throws {
  let value=text.trimmingCharacters(in:.whitespacesAndNewlines)
  guard !owner.isEmpty,value.utf16.count>0,value.utf16.count<=500,(!cloud || state.boundUID==nil || state.boundUID==owner)else{throw RotamDataError.invalid("Hafıza metni veya hesap bağlantısı geçerli değil.")}
  let all=state.records+state.privateMemories;let key=uid ?? UUID().uuidString.lowercased()
  let old=all.first{$0.kind == .memory&&$0.uid==key}
  if let expected{guard old==expected else{throw RotamDataError.invalid("Hafıza başka bir işlemde değişti.")}}
  if old==nil{guard memory(owner:owner).count<50 else{throw RotamDataError.invalid("En fazla 50 bilgi hatırlanabilir.")}}
  var fields=try old?.fields() ?? ["uid":.string(key),"ownerUid":.string(owner),"createdAt":.integer(Int64(Date().timeIntervalSince1970*1000))]
  guard fields["ownerUid"]?.string==owner else{throw RotamDataError.invalid("Hafıza başka bir hesaba ait.")}
  fields["text"] = .string(value);fields["updatedAt"] = .integer(Int64(Date().timeIntervalSince1970*1000));fields["isDeleted"] = .bool(false)
  let record=try WireRecord(kind:.memory,fields:fields)
  var next=state;next.records.removeAll{$0.key==record.key};next.privateMemories.removeAll{$0.key==record.key}
  if cloud{next.records.append(record)}else{next.privateMemories.append(record)}
  try CoreValidator.validate([record],owner:owner);try commit(next)
 }
 public func forgetMemory(owner:String,record:WireRecord)throws {
  let d=try record.fields();guard d["ownerUid"]?.string==owner else{throw RotamDataError.invalid("Hafıza başka bir hesaba ait.")}
  let tombstone=try record.changing(["text":.string(""),"isDeleted":.bool(true),"updatedAt":.integer(Int64(Date().timeIntervalSince1970*1000))])
  if state.records.contains(where:{$0.key==record.key}){try replace(tombstone,expected:record)}else{var next=state;next.privateMemories.removeAll{$0.key==record.key};next.privateMemories.append(tombstone);try commit(next)}
 }
}

public struct LocalDraft:Codable,Sendable {
 public var input:String
 public var pending:String
}
extension RecordStore {
 public func saveDraft(url:URL,input:String,pending:String,revision:Int64)throws {
  guard url.deletingLastPathComponent().standardizedFileURL==directory.standardizedFileURL else{throw RotamDataError.invalid("Taslak konumu geçerli değil.")}
  guard revision>(draftRevisions[url] ?? -1)else{return}
  guard input.utf16.count<=10000,pending.utf16.count<=10000 else{throw RotamDataError.invalid("Taslak çok uzun.")}
  let data=try JSONEncoder().encode(LocalDraft(input:input,pending:pending))
  #if os(iOS)
  try data.write(to:url,options:[.atomic,.completeFileProtectionUntilFirstUserAuthentication])
  #else
  try data.write(to:url,options:.atomic)
  #endif
  draftRevisions[url]=revision
 }
 public func loadDraft(url:URL)throws->LocalDraft {
  guard url.deletingLastPathComponent().standardizedFileURL==directory.standardizedFileURL else{throw RotamDataError.invalid("Taslak konumu geçerli değil.")}
  guard FileManager.default.fileExists(atPath:url.path)else{return LocalDraft(input:"",pending:"")}
  let data=try Data(contentsOf:url);guard data.count<=100000 else{throw RotamDataError.invalid("Taslak okunamadı. Dosya korundu.")}
  return try JSONDecoder().decode(LocalDraft.self,from:data)
 }
}

extension RecordStore {
 public func createPlan(_ plan:WireRecord)throws {
  if state.records.contains(where:{$0.key==plan.key}){return}
  if state.records.contains(where:{$0.kind == .series && (try? $0.fields()["template"]?.fields?["uid"]?.string)==plan.uid}){return}
  let p=try PortablePlan(plan)
  let created=try p.fields["repeatType"]?.string == "none" || p.fields["repeatType"]==nil ? [plan]:RecurrenceRules.make(plan,through:max(PlanDates.addDays(PlanDates.key(Date()),400),PlanDates.addDays(p.date,60)))
  try replaceMany(created)
 }
 public func measuredFocus(uid:String,segmentID:String,startedAt:Int64,seconds:Int64)throws {
  guard let current=state.records.first(where:{$0.kind == .plan&&$0.uid==uid}),try !PortablePlan(current).deleted else{return}
  try replace(PlanActions.focus(current,uid:segmentID,start:startedAt,seconds:seconds),expected:current)
 }
 public func updatePlan(_ proposed:WireRecord,before:WireRecord)throws {
  guard proposed.key==before.key,let current=state.records.first(where:{$0.key==before.key})else{throw RotamDataError.invalid("Plan bulunamadı.")}
  var currentFields=try current.fields(),oldFields=try before.fields()
  for key in ["focusSeconds"]{currentFields.removeValue(forKey:key);oldFields.removeValue(forKey:key)}
  func withoutFocus(_ field:JSONValue?)throws->JSONValue{
   var e=try JSONValue.parse(field?.string ?? "{}").fields ?? [:];e.removeValue(forKey:"focusSegments");return .string(try JSONValue.object(e).canonical())
  }
  currentFields["extraData"] = try withoutFocus(currentFields["extraData"]);oldFields["extraData"] = try withoutFocus(oldFields["extraData"])
  guard currentFields==oldFields else{throw RotamDataError.invalid("Plan başka bir işlemde değişti. Güncel bilgileri kontrol et.")}
  let scheduled=try PlanActions.preserveSchedule(current,proposed:proposed)
  var extra=try PortablePlan(scheduled).metadata;let existing=try PortablePlan(current)
  extra["focusSegments"]=existing.metadata["focusSegments"]
  let next=try scheduled.changing(["extraData":.string(JSONValue.object(extra).canonical()),"focusSeconds":.integer(existing.focusSeconds)])
  try replace(next,expected:current)
 }
}
