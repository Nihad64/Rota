import SwiftUI
import UserNotifications
import AVFoundation
import AudioToolbox
@preconcurrency import Speech

@MainActor final class ReminderService:NSObject,UNUserNotificationCenterDelegate {
 static let shared=ReminderService()
 weak var model:RotamModel?
 var openPlan:((String)->Void)?
 private var rescheduling=false
 private var needsReschedule=false
 private var delivery:[String:ReminderDeliveryState]{
  get{guard let data=UserDefaults.standard.data(forKey:"reminder-delivery-v1")else{return [:]};return (try? JSONDecoder().decode([String:ReminderDeliveryState].self,from:data)) ?? [:]}
  set{if let data=try? JSONEncoder().encode(newValue){UserDefaults.standard.set(data,forKey:"reminder-delivery-v1")}}
 }
 func configure(_ model:RotamModel){
  self.model=model;let center=UNUserNotificationCenter.current();center.delegate=self
  center.setNotificationCategories([UNNotificationCategory(identifier:"rotam-plan",actions:[UNNotificationAction(identifier:"started",title:"Başladım"),UNNotificationAction(identifier:"done",title:"Tamamladım"),UNNotificationAction(identifier:"snooze",title:"30 dakika sonra")],intentIdentifiers:[],options:.customDismissAction)])
 }
 func request()async->Bool{do{return try await UNUserNotificationCenter.current().requestAuthorization(options:[.alert,.sound,.badge])}catch{model?.report(error);return false}}
 func reschedule()async {
  needsReschedule=true;guard !rescheduling else{return}
  rescheduling=true;defer{rescheduling=false}
  while needsReschedule{needsReschedule=false;await applySchedule()}
 }
 private func applySchedule()async {
  guard let model else{return}
  do{
   let center=UNUserNotificationCenter.current(),settings=await center.notificationSettings()
   guard settings.authorizationStatus == .authorized||settings.authorizationStatus == .provisional else{return}
   let events=try ReminderRules.events(model.plans,settings:model.prefValues,delivery:delivery)
   let old=await center.pendingNotificationRequests();center.removePendingNotificationRequests(withIdentifiers:old.filter{$0.identifier.hasPrefix("rotam|")}.map(\.identifier))
   for event in events{
    guard let plan=model.plans.first(where:{$0.id==event.planID})else{continue}
    let content=UNMutableNotificationContent();content.title=plan.title;content.body=["after","last","checkin"].contains(event.key) ? "Yapabildin mi? Yanıtını bekliyor; yapılmadı sayılmadı.":event.key=="before" ? "\(plan.scheduleLabel) · Hazırlanmak için küçük bir adım.":"Zamanı geldi. Hazırsan başlayalım."
    content.categoryIdentifier="rotam-plan";content.sound = .default
    content.userInfo=["uid":plan.id,"signature":event.signature]
    let trigger=UNCalendarNotificationTrigger(dateMatching:Calendar.current.dateComponents([.year,.month,.day,.hour,.minute,.second],from:event.at),repeats:false)
    try await center.add(UNNotificationRequest(identifier:"rotam|"+PlanActions.ownerHash(event.id),content:content,trigger:trigger))
   }
  }catch{model.report(error)}
 }
 nonisolated func userNotificationCenter(_ center:UNUserNotificationCenter,didReceive response:UNNotificationResponse)async{
  let info=response.notification.request.content.userInfo,uid=info["uid"] as? String,signature=info["signature"] as? String,action=response.actionIdentifier
  await handle(uid:uid,signature:signature,action:action)
 }
 private func handle(uid:String?,signature:String?,action:String)async{
  guard let uid,let model,let p=model.plans.first(where:{$0.id==uid}),signature==PlanActions.reminderSignature(p),!p.completed,!p.skipped else{return}
  if action==UNNotificationDefaultActionIdentifier{openPlan?(uid);return}
  if action=="done"{await model.outcome(p,"COMPLETED")}
  else if action=="started"{await model.perform{try await model.store?.replace(PlanActions.markStarted(p),expected:p.record);await model.reload()}}
  else if action=="snooze"||action==UNNotificationDismissActionIdentifier{
   let current=PlanActions.reminderSignature(p)
   var states=delivery,local=states[uid].flatMap{$0.signature==current ? $0:nil} ?? ReminderDeliveryState(signature:current)
   if action=="snooze"{local.snooze(now:Date())}else{local.muted=true;local.snoozeUntil=nil}
   states[uid]=local;delivery=states
  }
  await reschedule()
 }
 nonisolated func userNotificationCenter(_ center:UNUserNotificationCenter,willPresent notification:UNNotification)async->UNNotificationPresentationOptions{[.banner,.sound]}
}

@MainActor final class SpeechInput:ObservableObject {
 @Published var recording=false
 @Published var error:String?
 private let engine=AVAudioEngine()
 private var request:SFSpeechAudioBufferRecognitionRequest?
 private var task:SFSpeechRecognitionTask?
 private var hasTap=false
 func start(update:@escaping @MainActor @Sendable (String)->Void)async {
  guard !recording else{stop();return}
  let status=await withCheckedContinuation{continuation in SFSpeechRecognizer.requestAuthorization{continuation.resume(returning:$0)}}
  let microphone=await AVAudioApplication.requestRecordPermission()
  guard status == .authorized,microphone else{error="Sesle yazmak için mikrofon ve konuşma tanıma iznini Ayarlar’dan açabilirsin.";return}
  guard let recognizer=SFSpeechRecognizer(locale:Locale(identifier:"tr-TR")),recognizer.isAvailable else{error="Türkçe ses tanıma şu an kullanılamıyor. Yazarak devam edebilirsin.";return}
  do{
   try AVAudioSession.sharedInstance().setCategory(.record,mode:.measurement,options:.duckOthers);try AVAudioSession.sharedInstance().setActive(true)
   let req=SFSpeechAudioBufferRecognitionRequest();req.shouldReportPartialResults=true;request=req
   let node=engine.inputNode,format=node.outputFormat(forBus:0)
   node.installTap(onBus:0,bufferSize:1024,format:format){buffer,_ in req.append(buffer)};hasTap=true
   engine.prepare();try engine.start();recording=true
   task=recognizer.recognitionTask(with:req){[weak self] result,error in
    let text=result?.bestTranscription.formattedString,finished=result?.isFinal==true,failed=error != nil
    Task{@MainActor [weak self] in if let text{update(text)};if finished||failed{self?.stop();if failed{self?.error="Ses tanıma tamamlanamadı. Yazılan metin korundu."}}}
   }
  }catch{stop();self.error="Mikrofon başlatılamadı. Yazarak devam edebilirsin."}
 }
 func stop(){engine.stop();if hasTap{engine.inputNode.removeTap(onBus:0);hasTap=false};request?.endAudio();task?.cancel();task=nil;request=nil;recording=false;try? AVAudioSession.sharedInstance().setActive(false,options:.notifyOthersOnDeactivation)}
}
