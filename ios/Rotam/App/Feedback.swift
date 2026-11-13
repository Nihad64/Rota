import SwiftUI
import AVFoundation
import WidgetKit

@MainActor final class Feedback {
 static let shared=Feedback()
 private var player:AVAudioPlayer?
 func success(_ values:[String:JSONValue]){
  if values["haptics"]?.boolean==true{UINotificationFeedbackGenerator().notificationOccurred(.success)}
  guard values["ui_sounds"]?.boolean==true,let url=Bundle.main.url(forResource:"rotam_success",withExtension:"wav")else{return}
  do{try AVAudioSession.sharedInstance().setCategory(.ambient,mode:.default,options:.mixWithOthers);player=try AVAudioPlayer(contentsOf:url);player?.volume=0.3;player?.play()}catch{/* Feedback must not change a successful persistence result. */}
 }
}
extension RotamModel {
 func publishWidgets()async{
  guard let directory=FileManager.default.containerURL(forSecurityApplicationGroupIdentifier:"group.com.rota.app")else{return}
  let day=PlanDates.key(Date()),today=plans.filter{$0.date==day},snapshot=WidgetSnapshot(day:day,completed:today.filter(\.completed).count,total:today.count,plans:Array(today.filter{!$0.completed && !$0.skipped}.prefix(5)).map(WidgetPlan.init))
  do{try JSONEncoder().encode(snapshot).write(to:directory.appendingPathComponent("widget.json"),options:[.atomic,.completeFileProtectionUntilFirstUserAuthentication]);WidgetCenter.shared.reloadAllTimelines()}catch{report(error)}
 }
}
