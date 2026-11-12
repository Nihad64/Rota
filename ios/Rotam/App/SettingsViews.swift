import SwiftUI
import UniformTypeIdentifiers
import UserNotifications

struct SettingsView:View {
 var body:some View{ScrollView{VStack(alignment:.leading,spacing:24){Text("Ayarlar").brandFont(38,weight:800);Text("Rotam, senin istediğin gibi.").brandFont(18).foregroundStyle(Brand.muted)
  group("HESAP"){row("Hesabım",detail:"Google hesabı ve profil",icon:"person",route:.account)}
  group("TERCİHLER"){row("Hatırlatmalar",detail:"Bildirimler, sessiz saatler ve ses",icon:"bell",route:.reminders)}
  group("VERİLER VE GİZLİLİK"){row("Verilerim",detail:"Senkronizasyon ve yedekleme",icon:"cloud",route:.data);Divider();row("Gizlilik ve hafıza",detail:"AI ile paylaşılan bilgiler",icon:"hand.raised",route:.privacy)}
  group("UYGULAMA"){row("Rotam hakkında",detail:"Sürüm bilgisi ve yenilikler",icon:"info.circle",route:.about)}
 }.padding(24).frame(maxWidth:760)}.background(Brand.background).foregroundStyle(Brand.ink).toolbar(.hidden,for:.navigationBar)}
 private func group<Content:View>(_ title:String,@ViewBuilder content:()->Content)->some View{VStack(alignment:.leading,spacing:16){Text(title).brandFont(12,weight:800).tracking(1.8).foregroundStyle(Brand.orange);VStack(spacing:0,content:content)}}
 private func row(_ title:String,detail:String,icon:String,route:Route)->some View{NavigationLink(value:route){HStack(spacing:18){Image(systemName:icon).font(.title2).foregroundStyle(Brand.orange).frame(width:32);VStack(alignment:.leading,spacing:5){Text(title).brandFont(21,weight:750);Text(detail).brandFont(14).foregroundStyle(Brand.muted)};Spacer();Image(systemName:"chevron.right").foregroundStyle(Brand.muted)}.padding(.vertical,18)}.buttonStyle(.plain)}
}
struct AccountView:View {
 @EnvironmentObject var model:RotamModel
 @State private var signOut=false
 var body:some View{ScrollView{VStack(alignment:.leading,spacing:24){HeroCard{VStack(alignment:.leading,spacing:14){Image(systemName:"person.crop.circle").font(.largeTitle).foregroundStyle(Brand.orange);Text(model.account?.name ?? "Hesabını bağla.").brandFont(30,weight:800);if let email=model.account?.email{Text(email).brandFont(15).foregroundStyle(Brand.muted)};Text(model.account==nil ? "Planların hesapsız da bu cihazda saklanır.":"Google ile bağlı").brandFont(15).foregroundStyle(Brand.muted)}}action:{EmptyView()}
  if model.account==nil{Button{Task{await model.signIn()}}label:{HStack{Text("Google ile devam et");if model.busy{ProgressView()}}}.buttonStyle(PrimaryButton()).disabled(model.busy)}else{NavigationLink(value:Route.data){Label("Eşitleme ve yedekleme",systemImage:"cloud")}.buttonStyle(QuietButton());Text("Profil adın Google hesabından gelir. Ayrı bir selamlama adı girmen gerekmez.").brandFont(15).foregroundStyle(Brand.muted);Button("Hesaptan çık"){signOut=true}.buttonStyle(QuietButton())}
 }.padding(24).frame(maxWidth:760)}.background(Brand.background).navigationTitle("Hesabım").confirmationDialog("Hesaptan çıkılsın mı?",isPresented:$signOut,titleVisibility:.visible){Button("Çıkış yap"){Task{await model.signOut()}};Button("Vazgeç",role:.cancel){}}message:{Text("Bu cihazdaki kayıtların korunur. Hesap bağlantısı ve bulut aktarımı durur.")}}
}
struct ReminderSettingsView:View {
 @EnvironmentObject var model:RotamModel
 @Environment(\.openURL) private var openURL
 @State private var permission="Kontrol ediliyor…"
 var body:some View{Form{Section("Hatırlatmalar"){Toggle("Plan hatırlatmaları",isOn:bool("reminders"));Text(permission).foregroundStyle(Brand.muted);Button("Bildirim iznini aç"){Task{_ = await ReminderService.shared.request();await refresh()}};Button("iPhone bildirim ayarları"){if let url=URL(string:UIApplication.openNotificationSettingsURLString){openURL(url)}}}footer:{Text("Zamanlama planına göre ayarlanır. Başladığın veya tamamladığın planın otomatik takibi durur. Yanıt vermemek, yapılmadı anlamına gelmez.")}
  Section("Sessiz saatler"){Toggle("Sessiz saatleri kullan",isOn:bool("quiet"));DatePicker("Başlangıç",selection:minutes("quiet_start_minutes"),displayedComponents:.hourAndMinute);DatePicker("Bitiş",selection:minutes("quiet_end_minutes"),displayedComponents:.hourAndMinute)}
  Section("Uygulama geri bildirimi"){Toggle("Kısa onay sesi",isOn:bool("ui_sounds"));Toggle("Dokunsal geri bildirim",isOn:bool("haptics"));Toggle("Animasyonlar",isOn:bool("animations"))}footer:{Text("Sesler isteğe bağlıdır ve sessiz moda uyar. Hareketi Azalt açıkken yoğun animasyon kullanılmaz.")}
  Button("Hatırlatmaları yeniden zamanla"){Task{await ReminderService.shared.reschedule();model.toast="Hatırlatmalar kontrol edildi"}}
 }.scrollContentBackground(.hidden).background(Brand.background).navigationTitle("Hatırlatmalar").task{await refresh()}}
 private func bool(_ key:String)->Binding<Bool>{Binding(get:{model.prefValues[key]?.boolean ?? false},set:{model.setPreference(key,.bool($0))})}
 private func minutes(_ key:String)->Binding<Date>{Binding(get:{let n=Int(model.prefValues[key]?.integer ?? 0);return Calendar.current.date(bySettingHour:n/60,minute:n%60,second:0,of:Date())!},set:{let c=Calendar.current.dateComponents([.hour,.minute],from:$0);model.setPreference(key,.integer(Int64(c.hour!*60+c.minute!)))})}
 private func refresh()async{let value=await UNUserNotificationCenter.current().notificationSettings();permission=value.authorizationStatus == .authorized||value.authorizationStatus == .provisional ? "Bildirim izni açık":"Bildirim izni kapalı. Planların kaydedilmeye devam eder."}
}
struct PrivacyView:View {
 @EnvironmentObject var model:RotamModel
 @State private var text=""
 @State private var cloud=false
 @State private var editing:WireRecord?
 var body:some View{ScrollView{VStack(alignment:.leading,spacing:24){HeroCard{VStack(alignment:.leading,spacing:14){Image(systemName:"shield.lefthalf.filled").font(.largeTitle);Text("Kontrol sende.").brandFont(32,weight:800);Text("AI paylaşımı, hafıza ve eşitleme için seçimlerin ayrı tutulur.").brandFont(16).foregroundStyle(Brand.muted)}}action:{EmptyView()}
  Toggle("Plan bağlamını AI ile paylaş",isOn:Binding(get:{model.sharePlans},set:{model.setSharePlans($0)}));Text("En fazla 150 planın başlığı, günü, saati ve durumu. Plan notları eklenmez. Yazdığın mesaj, konuşmanın son bölümü ve seçili gün isteğin içinde gönderilir.").brandFont(14).foregroundStyle(Brand.muted)
  Toggle("Onaylı hafızayı AI’da kullan",isOn:Binding(get:{model.useMemory},set:{model.setUseMemory($0)}));Text("Bu seçimi kapatmak bilgileri silmez. Aşağıdan ayrı ayrı unutturabilirsin.").brandFont(14).foregroundStyle(Brand.muted)
  SectionTitle(title:"Hatırlanan bilgiler")
  if model.account==nil{Text("Hafıza hesabına göre ayrılır. Bilgi kaydetmek için Google ile giriş yap.").brandFont(16).foregroundStyle(Brand.muted)}else{
   ForEach(model.memory,id:\.uid){record in VStack(alignment:.leading,spacing:12){Text((try? record.fields()["text"]?.string) ?? "").brandFont(17);HStack{Button("Düzenle"){editing=record;text=(try? record.fields()["text"]?.string) ?? ""};Spacer();Button("Unut",role:.destructive){Task{await model.forget(record)}}}}.surfaceCard()}
   if model.memory.isEmpty{Text("Henüz onayladığın bir bilgi yok.").brandFont(16).foregroundStyle(Brand.muted)}
   VStack(alignment:.leading,spacing:14){Text(editing==nil ? "Hatırlamamı istediğin bilgi":"Bilgiyi düzenle").brandFont(19,weight:750);TextField("Örneğin: Akşam vardiyasında çalışıyorum",text:$text,axis:.vertical).lineLimit(2...6).padding(14).background(Brand.raised,in:RoundedRectangle(cornerRadius:16));Toggle("Bu bilgi hesabımda da eşitlensin",isOn:$cloud);Text("Kapalıysa yalnız bu cihazda saklanır. Metni kaydetmen AI kullanımına açıkça onay vermendir; kullanımını üstteki seçimden kapatabilirsin.").brandFont(13).foregroundStyle(Brand.muted);Button("Onayla ve hatırla"){Task{model.error=nil;await model.remember(text,cloud:cloud,before:editing);if model.error==nil{text="";editing=nil}}}.buttonStyle(PrimaryButton()).disabled(text.trimmingCharacters(in:.whitespacesAndNewlines).isEmpty||model.busy);if editing != nil{Button("Vazgeç"){editing=nil;text=""}}}.surfaceCard()
  }
 }.padding(24).frame(maxWidth:760)}.background(Brand.background).navigationTitle("Gizlilik ve hafıza")}
}
struct BackupDocument:FileDocument {
 static var readableContentTypes:[UTType]{[.data]}
 var data:Data
 init(data:Data){self.data=data}
 init(configuration:ReadConfiguration)throws{guard let data=configuration.file.regularFileContents else{throw RotamDataError.invalid("Yedek dosyası okunamadı.")};self.data=data}
 func fileWrapper(configuration:WriteConfiguration)throws->FileWrapper{FileWrapper(regularFileWithContents:data)}
}
struct DataView:View {
 @EnvironmentObject var model:RotamModel
 @State private var consent=false
 @State private var preferenceConsent=false
 @State private var exporting=false
 @State private var importing=false
 @State private var document=BackupDocument(data:Data())
 @State private var incoming:[WireRecord]?
 @State private var original:[WireRecord]=[]
 var body:some View{ScrollView{VStack(alignment:.leading,spacing:24){HeroCard{VStack(alignment:.leading,spacing:14){Image(systemName:"cloud").font(.largeTitle);Text(model.syncEnabled ? "Planların bağlı.":"Planların güvende.").brandFont(30,weight:800);Text(model.syncStatus).brandFont(16).foregroundStyle(Brand.muted)}}action:{EmptyView()}
  if model.account==nil{NavigationLink(value:Route.account){Text("Google hesabını bağla")}.buttonStyle(PrimaryButton())}
  else if !model.syncEnabled{Button("Bağla ve eşitle"){consent=true}.buttonStyle(PrimaryButton())}
  else{Button("Şimdi eşitle"){Task{await model.sync()}}.buttonStyle(PrimaryButton());Button("Plan eşitlemesini durdur"){Task{await model.pauseSync()}}.buttonStyle(QuietButton())}
  SectionTitle(title:"Tercih eşitlemesi")
  Text("Bildirim seçimi, sessiz saatler, uygulama sesi, titreşim ve animasyon tercihi. AI paylaşım izni, işletim sistemi izinleri ve hesap bilgileri aktarılmaz.").brandFont(15).foregroundStyle(Brand.muted)
  Text(model.preferencesStatus).brandFont(14).foregroundStyle(Brand.muted)
  if model.preferencesEnabled{Button("Tercih eşitlemesini durdur"){Task{await model.pausePreferences()}}.buttonStyle(QuietButton())}else{Button("Tercihleri ayrıca eşitle"){preferenceConsent=true}.buttonStyle(QuietButton()).disabled(!model.syncEnabled)}
  SectionTitle(title:"Plan yedeği")
  Text("Planlar, rutinler, hedefler, notlar ve odak kayıtları. Hafıza bilgileri bu dosyaya eklenmez. Dosya seçtiğin konuma kaydedilir; kişisel bilgilerini içerebilir.").brandFont(15).foregroundStyle(Brand.muted)
  Button{do{document=BackupDocument(data:try SyncCodec.encode(model.records.filter{$0.kind != .memory}));exporting=true}catch{model.report(error)}}label:{Label("Plan yedeğini dışa aktar",systemImage:"square.and.arrow.up")}.buttonStyle(QuietButton());Button{importing=true}label:{Label("Plan yedeği içe aktar",systemImage:"square.and.arrow.down")}.buttonStyle(QuietButton())
 }.padding(24).frame(maxWidth:760)}.background(Brand.background).navigationTitle("Verilerim")
 .confirmationDialog("Planlarını hesabına eşitleyelim mi?",isPresented:$consent,titleVisibility:.visible){Button("Bağla ve eşitle"){Task{await model.enableSync()}};Button("Vazgeç",role:.cancel){}}message:{Text("Planların, rutinlerin, hedeflerin, notların ve onayladığın paylaşılan hafıza Google hesabına bağlı Avrupa’daki Rotam veritabanına aktarılır. Cihaz verileri korunur; farklı değişiklikler sana sorulur.")}
 .confirmationDialog("Tercihlerin de eşitlensin mi?",isPresented:$preferenceConsent,titleVisibility:.visible){Button("Tercihleri eşitle"){Task{await model.enablePreferences()}};Button("Vazgeç",role:.cancel){}}message:{Text("Bu izin plan eşitlemesinden ayrıdır. Mevcut bulut tercihi farklıysa önce inceleme açılır.")}
 .fileExporter(isPresented:$exporting,document:document,contentType:.data,defaultFilename:"Rotam-plan-yedegi.rotam"){result in if case .failure(let error)=result{model.report(error)}}
 .fileImporter(isPresented:$importing,allowedContentTypes:[.data]){result in do{let url=try result.get(),allowed=url.startAccessingSecurityScopedResource();defer{if allowed{url.stopAccessingSecurityScopedResource()}};let data=try Data(contentsOf:url);let rows=try SyncCodec.decode(data);guard rows.allSatisfy({$0.kind != .memory&&$0.kind != .preference})else{throw RotamDataError.invalid("Bu dosya yalnız plan yedeği değil. Hafıza ve izinler otomatik içe aktarılmaz.")};try CoreValidator.validate(rows);original=model.records;incoming=rows}catch{model.report(error)}}
 .confirmationDialog("Yedek incelendi",isPresented:Binding(get:{incoming != nil},set:{if !$0{incoming=nil}}),titleVisibility:.visible){Button("Onayla ve birleştir"){guard let incoming else{return};let before=original;Task{await model.perform{try await model.store?.replaceMany(incoming,expected:before);await model.reload();model.requestSync();await ReminderService.shared.reschedule();model.toast="Plan yedeği birleştirildi"}};self.incoming=nil};Button("Vazgeç",role:.cancel){incoming=nil}}message:{Text("\(incoming?.count ?? 0) kayıt okunacak. Aynı kimlikteki kayıt dosyadaki sürümle değişir; diğer cihaz kayıtların korunur. İçe aktarmadan önce dışa yedek alabilirsin.")}
 }
}
struct SyncReviewView:View {
 @EnvironmentObject var model:RotamModel
 let review:SyncReview
 var body:some View{NavigationStack{ScrollView{VStack(alignment:.leading,spacing:20){Text(review.preferences ? "Tercihlerini karşılaştıralım.":"İki cihazda farklı değişiklik var.").brandFont(30,weight:800);Text("Sessizce üzerine yazmadım. Çakışan kayıtlar için kullanmak istediğin sürümü seç.").brandFont(16).foregroundStyle(Brand.muted)
  ForEach(review.merged.conflicts){c in VStack(alignment:.leading,spacing:10){Text(review.preferences ? preferenceName(c.key.uid):recordTitle(c.local ?? c.remote)).brandFont(19,weight:750);Text("Bu cihaz · "+summary(c.local)).brandFont(15);Text("Bulut · "+summary(c.remote)).brandFont(15).foregroundStyle(Brand.muted)}.surfaceCard()}
  Button("Bu cihazdaki sürümleri kullan"){Task{await model.resolveSync(local:true)}}.buttonStyle(PrimaryButton()).disabled(model.busy);Button("Buluttaki sürümleri kullan"){Task{await model.resolveSync(local:false)}}.buttonStyle(QuietButton()).disabled(model.busy);Button("Şimdilik durdur"){Task{if review.preferences{await model.pausePreferences()}else{await model.pauseSync()};model.review=nil}}.buttonStyle(QuietButton())
 }.padding(24)}.background(Brand.background).navigationTitle("Eşitleme incelemesi")}}
 private func recordTitle(_ r:WireRecord?)->String{(try? r?.fields()["title"]?.string) ?? (r?.kind == .memory ? "Hafıza bilgisi":"Kayıt")}
 private func summary(_ r:WireRecord?)->String{guard let r,let d=try? r.fields()else{return "Silinmiş"};if review.preferences{if let value=d["value"]?.boolean{return value ? "Açık":"Kapalı"};return d["value"]?.integer.map{String($0)} ?? ""};return d["text"]?.string ?? (d["title"]?.string ?? "Kayıt")+" · "+(d["date"]?.string ?? "")}
 private func preferenceName(_ key:String)->String{["reminders":"Hatırlatmalar","quiet":"Sessiz saatler","ui_sounds":"Uygulama sesi","haptics":"Dokunsal geri bildirim","animations":"Animasyonlar","reminder_minutes":"Özel hatırlatma süresi","quiet_start_minutes":"Sessiz saat başlangıcı","quiet_end_minutes":"Sessiz saat bitişi"][key] ?? "Tercih"}
}
struct AboutView:View {var body:some View{ScrollView{VStack(alignment:.leading,spacing:24){BrandMark();Text("Planla. Odaklan. İlerle.").brandFont(30,weight:800);Text("Rotam 3.1 · iOS geliştirme sürümü").brandFont(17).foregroundStyle(Brand.muted);Text("Plan onayı, rutinler, odak takibi, Google hesabı ve ayrı izinlerle eşitleme. AI önerileri sen onaylamadan kaydedilmez.").brandFont(17);Link("GitHub’da sorun bildir",destination:URL(string:"https://github.com/Nihad64/Rota/issues/new")!).buttonStyle(QuietButton());Text("Manrope yazı tipi · SIL Open Font License 1.1").brandFont(14).foregroundStyle(Brand.muted)}.padding(24)}.background(Brand.background).navigationTitle("Rotam hakkında")}}
