import SwiftUI
import UserNotifications
import AVFoundation
import AudioToolbox
import Speech

@MainActor final class ReminderService:NSObject,UNUserNotificationCenterDelegate {
 static let shared=ReminderService()
 weak var model:RotamModel?
 var openPlan:((String)->Void)?
 func configure(_ model:RotamModel){
  self.model=model;let center=UNUserNotificationCenter.current();center.delegate=self
  center.setNotificationCategories([UNNotificationCategory(identifier:"rotam-plan",actions:[UNNotificationAction(identifier:"started",title:"Başladım"),UNNotificationAction(identifier:"done",title:"Tamamladım"),UNNotificationAction(identifier:"snooze",title:"30 dakika sonra")],intentIdentifiers:[],options:.customDismissAction)])
 }
 func request()async->Bool{do{return try await UNUserNotificationCenter.current().requestAuthorization(options:[.alert,.sound,.badge])}catch{model?.report(error);return false}}
 func reschedule()async {
  guard let model else{return}
  do{
   let center=UNUserNotificationCenter.current(),settings=await center.notificationSettings()
   guard settings.authorizationStatus == .authorized||settings.authorizationStatus == .provisional else{return}
   let events=try ReminderRules.events(model.plans,settings:model.prefValues)
   let old=await center.pendingNotificationRequests();center.removePendingNotificationRequests(withIdentifiers:old.filter{$0.identifier.hasPrefix("rotam|")}.map(\.identifier))
   for event in events{
    guard let plan=model.plans.first(where:{$0.id==event.planID})else{continue}
    let content=UNMutableNotificationContent();content.title=plan.title;content.body=["after","last","checkin"].contains(event.key) ? "Yapabildin mi? Yanıtını bekliyor; yapılmadı sayılmadı.":event.key=="before" ? "\(plan.scheduleLabel) · Hazırlanmak için küçük bir adım.":"Zamanı geldi. Hazırsan başlayalım."
    content.categoryIdentifier="rotam-plan";content.sound = .default
    content.userInfo=["uid":plan.id,"signature":event.signature,"snoozeCount":0]
    let trigger=UNCalendarNotificationTrigger(dateMatching:Calendar.current.dateComponents([.year,.month,.day,.hour,.minute,.second],from:event.at),repeats:false)
    try await center.add(UNNotificationRequest(identifier:"rotam|"+PlanActions.ownerHash(event.id),content:content,trigger:trigger))
   }
  }catch{model.report(error)}
 }
 nonisolated func userNotificationCenter(_ center:UNUserNotificationCenter,didReceive response:UNNotificationResponse)async{
  let info=response.notification.request.content.userInfo,uid=info["uid"] as? String,signature=info["signature"] as? String,count=info["snoozeCount"] as? Int ?? 0,action=response.actionIdentifier
  await handle(uid:uid,signature:signature,action:action,count:count)
 }
 private func handle(uid:String?,signature:String?,action:String,count:Int)async{
  guard let uid,let model,let p=model.plans.first(where:{$0.id==uid}),signature==PlanActions.reminderSignature(p),!p.completed,!p.skipped else{return}
  if action==UNNotificationDefaultActionIdentifier{openPlan?(uid);return}
  if action=="done"{await model.outcome(p,"COMPLETED")}
  else if action=="started"{await model.perform{try await model.store?.replace(PlanActions.markStarted(p),expected:p.record);await model.reload()}}
  else if action=="snooze",count<3{
   let c=UNMutableNotificationContent();c.title=p.title;c.body="Biraz sonra demiştin. Şimdi uygun mu?";c.categoryIdentifier="rotam-plan";c.sound = .default;c.userInfo=["uid":uid,"signature":signature ?? "","snoozeCount":count+1]
   do{try await UNUserNotificationCenter.current().add(UNNotificationRequest(identifier:"rotam|snooze|"+PlanActions.ownerHash(uid),content:c,trigger:UNTimeIntervalNotificationTrigger(timeInterval:1800,repeats:false)))}catch{model.report(error)}
  }
  let pending=await UNUserNotificationCenter.current().pendingNotificationRequests()
  if action != UNNotificationDefaultActionIdentifier{UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers:pending.filter{($0.content.userInfo["uid"] as? String)==uid && !$0.identifier.contains("|snooze|")}.map(\.identifier))}
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
 func start(update:@escaping @MainActor(String)->Void)async {
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
