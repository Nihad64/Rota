import SwiftUI
import GoogleSignIn

@main struct RotamApp:App {
 @StateObject private var model=RotamModel()
 var body:some Scene{WindowGroup{RootView().environmentObject(model).preferredColorScheme(.dark).tint(Brand.orange).task{await model.start();ReminderService.shared.configure(model);await ReminderService.shared.reschedule()}.onOpenURL{url in if !GIDSignIn.sharedInstance.handle(url){NotificationCenter.default.post(name:.rotamLink,object:url)}}}}
}
extension Notification.Name{static let rotamLink=Notification.Name("rotamLink")}
enum Route:Hashable{case plan(String),focus(String),insights,search,account,reminders,privacy,data,about}
struct RootView:View {
 @EnvironmentObject var model:RotamModel
 @Environment(\.accessibilityReduceMotion) private var reduced
 @Environment(\.scenePhase) private var scene
 @State private var tab=0
 @State private var path:[Route]=[]
 @State private var adding=false
 var body:some View{
  NavigationStack(path:$path){
   Group{
    if !model.ready{VStack(spacing:20){BrandMark();ProgressView("Kayıtların açılıyor…");if let error=model.error{Text(error)}}}
    else if model.showWelcome{WelcomeView()}
    else{TabView(selection:$tab){HomeView(path:$path,adding:$adding).tabItem{Label("Bugün",systemImage:"house")}.tag(0);CalendarView(path:$path,adding:$adding).tabItem{Label("Takvim",systemImage:"calendar")}.tag(1);RoutinesView(path:$path,adding:$adding).tabItem{Label("Rutinler",systemImage:"square.3.layers.3d")}.tag(2);SettingsView().tabItem{Label("Ayarlar",systemImage:"gearshape")}.tag(3)}.toolbarBackground(Brand.background,for:.tabBar).toolbarBackground(.visible,for:.tabBar)}
   }
   .background(Brand.background.ignoresSafeArea()).foregroundStyle(Brand.ink)
   .navigationDestination(for:Route.self){route in destination(route)}
  }
  .sheet(isPresented:$adding){NavigationStack{AiView()}.environmentObject(model).preferredColorScheme(.dark)}
  .alert("İşlem tamamlanamadı",isPresented:Binding(get:{model.error != nil},set:{if !$0{model.error=nil}})){Button("Tamam"){model.error=nil}}message:{Text(model.error ?? "")}
  .sheet(item:$model.review){review in SyncReviewView(review:review).environmentObject(model).interactiveDismissDisabled()}
  .safeAreaInset(edge:.bottom){if let toast=model.toast{HStack{Text(toast).brandFont(14);Spacer();Button("Geri al"){Task{await model.undo()}};Button{model.toast=nil}label:{Image(systemName:"xmark").frame(width:44,height:44)}.accessibilityLabel("Bildirimi kapat")}.padding(.leading,16).background(Brand.raised,in:RoundedRectangle(cornerRadius:18)).padding(.horizontal).transition(.move(edge:.bottom).combined(with:.opacity))}}
  .animation(reduced || !model.motion ? nil:.spring(response:0.3,dampingFraction:0.85),value:model.toast)
  .onChange(of:scene){_,value in if value == .active{model.requestSync();Task{await ReminderService.shared.reschedule()}}}
  .onReceive(NotificationCenter.default.publisher(for:.rotamLink)){note in if let url=note.object as? URL{if url.host=="add"{adding=true}else if url.host=="plan",let uid=URLComponents(url:url,resolvingAgainstBaseURL:false)?.queryItems?.first(where:{$0.name=="uid"})?.value{path.append(.plan(uid))}}}
  .onAppear{ReminderService.shared.openPlan={uid in path.append(.plan(uid))}}
 }
 @ViewBuilder private func destination(_ route:Route)->some View {
  switch route{
  case .plan(let uid):PlanDetailView(uid:uid,path:$path)
  case .focus(let uid):FocusView(uid:uid)
  case .insights:InsightsView()
  case .search:SearchView(path:$path)
  case .account:AccountView()
  case .reminders:ReminderSettingsView()
  case .privacy:PrivacyView()
  case .data:DataView()
  case .about:AboutView()
  }
 }
}
struct WelcomeView:View {
 @EnvironmentObject var model:RotamModel
 var body:some View{ScrollView{VStack(alignment:.leading,spacing:28){BrandMark().padding(.top,28);Text(model.notificationStep ? "Zamanı gelince\nhaberin olsun.":"Bugün,\nsenin ritmin.").brandFont(42,weight:800).fixedSize(horizontal:false,vertical:true);Text(model.notificationStep ? "Planların ve rutinlerin için hatırlatmalar. Kontrol her zaman sende.":"Planlarını anlat, gününü birlikte düzenleyelim.").brandFont(19).foregroundStyle(Brand.muted)
   HeroCard{VStack(alignment:.leading,spacing:24){Image(systemName:model.notificationStep ? "bell":"point.bottomleft.forward.to.point.topright.scurvepath").font(.system(size:90,weight:.light)).foregroundStyle(Brand.ink).frame(maxWidth:.infinity).padding(.vertical,34);Diamond()}}action:{EmptyView()}
   if model.notificationStep{Button("Hatırlatmaları etkinleştir"){Task{_ = await ReminderService.shared.request();model.finishWelcome();await ReminderService.shared.reschedule()}}.buttonStyle(PrimaryButton());Button("Şimdi değil"){model.finishWelcome()}.buttonStyle(QuietButton())}
   else{Button{Task{await model.signIn()}}label:{HStack{Image(systemName:"person.crop.circle");Text("Google ile devam et");if model.busy{ProgressView()}}}.buttonStyle(PrimaryButton()).disabled(model.busy);Button("Hesapsız devam et"){model.notificationStep=true}.buttonStyle(QuietButton());Text("Google hesabı cihazlar arası bağlantı içindir. Verilerin, eşitlemeyi ayrıca onayladığında aktarılır.").brandFont(14).foregroundStyle(Brand.muted)}
  }.padding(24).frame(maxWidth:680)}.background(Brand.background)}
}
