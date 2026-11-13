import SwiftUI
import Combine

private struct FocusSession:Codable {
 var planID:String
 var segment=UUID().uuidString.lowercased()
 var wallStart=Int64(Date().timeIntervalSince1970*1000)
 var bootEstimate=Date().timeIntervalSince1970-ProcessInfo.processInfo.systemUptime
 var uptime=ProcessInfo.processInfo.systemUptime
 var focus:Int64=0
 var breaks:Int64=0
 var paused=true
 var breakUptime:Double?
 var segmentSeconds:Int64=0
}
@MainActor final class FocusClock:ObservableObject {
 @Published private var session:FocusSession?
 @Published var elapsed:Int64=0
 @Published var breakSeconds:Int64=0
 @Published var paused=false
 @Published var summary=false
 private weak var model:RotamModel?
 private var file:URL?
 private var ticker:AnyCancellable?
 private var ticking=false
 var planID:String?{session?.planID}
 func attach(_ model:RotamModel,uid:String)async {
  if session != nil{return};self.model=model;file=model.directory.appendingPathComponent("active-focus.json")
  do{
   if let file,FileManager.default.fileExists(atPath:file.path){session=try JSONDecoder().decode(FocusSession.self,from:Data(contentsOf:file))}
   if session==nil{session=FocusSession(planID:uid)}
   guard let s=session else{return}
   guard s.planID==uid else{model.error="Başka bir planın açık odak oturumu var. Önce o planın Odaklan ekranına dönerek oturumu bitir.";session=nil;return}
   if abs(Date().timeIntervalSince1970-ProcessInfo.processInfo.systemUptime-s.bootEstimate)>5 || ProcessInfo.processInfo.systemUptime<s.uptime{session?.focus += session?.segmentSeconds ?? 0;session?.segmentSeconds=0;session?.paused=true;session?.breakUptime=nil;model.toast="Telefon yeniden başlatılmış. Ölçülen süre korundu; devam etmek için molayı bitir."}

   try persist();await tick()
   ticker=Timer.publish(every:1,on:.main,in:.common).autoconnect().sink{[weak self]_ in Task{@MainActor [weak self] in await self?.tick()}}
  }catch{model.report(error)}
 }
 private func persist()throws{guard let file,let session else{return};try JSONEncoder().encode(session).write(to:file,options:[.atomic,.completeFileProtectionUntilFirstUserAuthentication])}
 private func currentSegment()->Int64{guard let s=session,!s.paused else{return session?.segmentSeconds ?? 0};return max(s.segmentSeconds,Int64(max(0,ProcessInfo.processInfo.systemUptime-s.uptime)))}
 private func currentBreak()->Int64{guard let u=session?.breakUptime else{return 0};return Int64(max(0,ProcessInfo.processInfo.systemUptime-u))}
 func tick()async{
  guard !ticking,let s=session,let model else{return};ticking=true;defer{ticking=false}
  elapsed=s.focus+currentSegment();breakSeconds=s.breaks+currentBreak();paused=s.paused
  if !s.paused,elapsed%10==0{do{let seconds=currentSegment();try await model.store?.measuredFocus(uid:s.planID,segmentID:s.segment,startedAt:s.wallStart,seconds:seconds);session?.segmentSeconds=seconds;try persist();await model.reload()}catch{ticker?.cancel();model.report(error)}}
 }
 func togglePause()async {
  guard var s=session,let model else{return}
  do{
   if !s.paused{let seconds=currentSegment();try await model.store?.measuredFocus(uid:s.planID,segmentID:s.segment,startedAt:s.wallStart,seconds:seconds);s.focus+=seconds;s.segmentSeconds=0;s.paused=true;s.breakUptime=ProcessInfo.processInfo.systemUptime}
   else{if let p=model.plans.first(where:{$0.id==s.planID}){try await model.store?.replace(PlanActions.markStarted(p),expected:p.record)};s.breaks+=currentBreak();s.paused=false;s.breakUptime=nil;s.segment=UUID().uuidString.lowercased();s.wallStart=Int64(Date().timeIntervalSince1970*1000);s.uptime=ProcessInfo.processInfo.systemUptime;s.bootEstimate=Date().timeIntervalSince1970-s.uptime;s.segmentSeconds=0}
   session=s;try persist();await model.reload();await tick()
  }catch{model.report(error)}
 }
 func finish()async{
  guard var s=session,let model else{return}
  do{
   let seconds=currentSegment();if !s.paused{try await model.store?.measuredFocus(uid:s.planID,segmentID:s.segment,startedAt:s.wallStart,seconds:seconds);s.focus+=seconds;s.segmentSeconds=0}else{s.breaks+=currentBreak()}
   s.paused=true;s.breakUptime=nil;session=s;elapsed=s.focus;breakSeconds=s.breaks;paused=true
   try persist();ticker?.cancel();ticker=nil;summary=true;await model.reload();model.requestSync()
  }catch{model.report(error)}
 }
 func close()throws{if let file,FileManager.default.fileExists(atPath:file.path){try FileManager.default.removeItem(at:file)};session=nil;ticker?.cancel();ticker=nil}
}
struct FocusView:View {
 @EnvironmentObject var model:RotamModel
 @Environment(\.dismiss) private var dismiss
 @Environment(\.scenePhase) private var scene
 @StateObject private var clock=FocusClock()
 let uid:String
 var body:some View{ScrollView{VStack(alignment:.leading,spacing:28){if let p=model.plans.first(where:{$0.id==uid}){
  Image(systemName:clock.summary ? "checkmark.circle":Brand.icon(p.category)).font(.system(size:58,weight:.light)).foregroundStyle(Brand.orange)
  Text(clock.summary ? "\(clock.elapsed/60) dakika odaklandın.":p.title).brandFont(34,weight:800)
  if clock.summary{Text(p.title).brandFont(18).foregroundStyle(Brand.muted);HStack{VStack(alignment:.leading){Text("Odak süresi").brandFont(14);Text(duration(clock.elapsed)).brandFont(27,weight:800)};Spacer();VStack(alignment:.leading){Text("Mola").brandFont(14);Text(duration(clock.breakSeconds)).brandFont(27,weight:800)}}.surfaceCard();Text("Planını da tamamladın mı?").brandFont(25,weight:800);Text("Odak süren kaydedildi. Planın durumu yalnız sen onaylarsan değişir.").brandFont(16).foregroundStyle(Brand.muted);Button("Evet, planı tamamla"){Task{model.error=nil;await model.outcome(p,"COMPLETED");if model.error==nil{close()}}}.buttonStyle(PrimaryButton());Button("Sonra devam edeceğim"){close()}.buttonStyle(QuietButton())}
  else{Text("Serbest odak").brandFont(18).foregroundStyle(Brand.muted);HeroCard{VStack(alignment:.leading,spacing:16){HStack{Diamond();Text(clock.paused ? "MOLADASIN":"ŞİMDİ").brandFont(13,weight:800).tracking(2).foregroundStyle(Brand.muted)};Text(timer(clock.paused ? clock.breakSeconds:clock.elapsed)).brandFont(58,weight:800).monospacedDigit().minimumScaleFactor(0.5);Text(clock.paused ? "Mola süresi":"Geçen süre").brandFont(18).foregroundStyle(Brand.muted)}}action:{Button{Task{await clock.togglePause()}}label:{Image(systemName:clock.paused ? "play.fill":"pause.fill").font(.title2).foregroundStyle(Brand.background).frame(width:76,height:76).background(Brand.ink,in:Circle())}.accessibilityLabel(clock.paused ? "Odaklanmaya devam et":"Mola ver")};Text("Kendi ritminde devam et.").brandFont(17).foregroundStyle(Brand.muted);Button("Bitir"){Task{await clock.finish()}}.buttonStyle(QuietButton())}
 }else{EmptyState(symbol:"timer",title:"Plan bulunamadı.",detail:"Planın silinmiş olabilir. Kayıtlı odak sürelerin korunur.")}}
 .padding(28).frame(maxWidth:760)}.background(Brand.background).foregroundStyle(Brand.ink).navigationTitle(clock.summary ? "Oturum özeti":"Odaklan").navigationBarTitleDisplayMode(.inline).task{await clock.attach(model,uid:uid)}.onChange(of:scene){_,value in if value == .active{Task{await clock.tick()}}}}
 private func timer(_ seconds:Int64)->String{String(format:"%02d:%02d",seconds/60,seconds%60)}
 private func duration(_ seconds:Int64)->String{seconds>=3600 ? "\(seconds/3600) sa \(seconds%3600/60) dk":"\(seconds/60) dk \(seconds%60) sn"}
 private func close(){do{try clock.close();dismiss()}catch{model.report(error)}}
}
