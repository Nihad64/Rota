import SwiftUI

struct AiView:View {
 @EnvironmentObject var model:RotamModel
 @Environment(\.dismiss) private var dismiss
 @StateObject private var speech=SpeechInput()
 @State private var manual=false
 @State private var editing:DraftEdit?
 @State private var memoryChoice:MemoryChoice?
 @State private var speaker=AVSpeechSynthesizer()
 var body:some View{ScrollViewReader{proxy in
  ScrollView{LazyVStack(alignment:.leading,spacing:24){
   if model.messages.isEmpty{VStack(alignment:.leading,spacing:18){BrandMark();Text("Aklındakini anlat.\nBirlikte planlayalım.").brandFont(35,weight:800);Text("Yaz veya söyle. Hazırlanan planları sen onayladıktan sonra kaydederim.").brandFont(17).foregroundStyle(Brand.muted);Button("Örnek: Yarın 14–22 çalışıyorum"){model.setInput("Yarın 14–22 çalışıyorum.")}.buttonStyle(QuietButton())}.padding(.vertical,16)}
   ForEach(model.messages){message in messageView(message)}
   if model.asking{HStack(spacing:12){ProgressView();Text("Planını düşünüyorum…").brandFont(15);Spacer();Button("Durdur"){model.cancelAi()}}.surfaceCard()}
   Color.clear.frame(height:1).id("bottom")
  }.padding(24).frame(maxWidth:760)}
  .onChange(of:model.messages.count){_,_ in withAnimation{proxy.scrollTo("bottom",anchor:.bottom)}}
  .onChange(of:model.asking){_,_ in proxy.scrollTo("bottom",anchor:.bottom)}
 }.background(Brand.background).navigationTitle("Rotam AI").navigationBarTitleDisplayMode(.inline)
 .toolbar{ToolbarItem(placement:.topBarLeading){Button("Kapat"){speech.stop();dismiss()}};ToolbarItem(placement:.topBarTrailing){Button("Elle ekle"){manual=true}}}
 .safeAreaInset(edge:.bottom){composer}
 .sheet(isPresented:$manual){NavigationStack{PlanEditorView(day:model.selectedDay){p in await model.create(p)}}}
 .sheet(item:$editing){draft in NavigationStack{PlanEditorView(record:draft.plan,day:model.selectedDay){p in model.reviseDraft(message:draft.message,draft:draft.draft,plan:p)}}}
 .confirmationDialog("Bu bilgi nerede hatırlansın?",isPresented:Binding(get:{memoryChoice != nil},set:{if !$0{memoryChoice=nil}}),titleVisibility:.visible){if let c=memoryChoice{Button("Yalnız bu cihazda"){Task{await model.approveMemory(message:c.message,draft:c.draft,cloud:false)};memoryChoice=nil};Button("Hesabımda da eşitlensin"){Task{await model.approveMemory(message:c.message,draft:c.draft,cloud:true)};memoryChoice=nil}};Button("Vazgeç",role:.cancel){memoryChoice=nil}}message:{Text("Yalnız açıkça onayladığın bilgi kaydedilir. Gizlilik bölümünden değiştirebilir veya unutturabilirsin.")}
 .alert("Sesle yazma",isPresented:Binding(get:{speech.error != nil},set:{if !$0{speech.error=nil}})){Button("Tamam"){speech.error=nil}}message:{Text(speech.error ?? "")}
 .onDisappear{speech.stop();speaker.stopSpeaking(at:.immediate)}
 }
 @ViewBuilder private func messageView(_ message:AiMessage)->some View{
  VStack(alignment:.leading,spacing:16){
   HStack(alignment:.top){if !message.user{Diamond().padding(.top,4)};Text(message.text).brandFont(17).textSelection(.enabled).fixedSize(horizontal:false,vertical:true)}.padding(message.user ? 18:0).background(message.user ? Brand.raised:Color.clear,in:RoundedRectangle(cornerRadius:24))
   if !message.user && !message.error{Button{if speaker.isSpeaking{speaker.stopSpeaking(at:.immediate)}else{let utterance=AVSpeechUtterance(string:message.text);utterance.voice=AVSpeechSynthesisVoice(language:"tr-TR");speaker.speak(utterance)}}label:{Label("Dinle",systemImage:"speaker.wave.2")}.buttonStyle(QuietButton())}
   ForEach(message.plans){draft in if let p=try? PortablePlan(draft.plan){HStack(alignment:.top,spacing:16){Image(systemName:Brand.icon(p.category)).font(.title);VStack(alignment:.leading,spacing:7){Text(p.title).brandFont(23,weight:800);Text(p.date+" · "+p.scheduleLabel).brandFont(15).foregroundStyle(Brand.muted);if draft.before != nil{Text("Mevcut planı güncelleyecek").brandFont(13).foregroundStyle(Brand.orange)}};Spacer();if draft.saved{Image(systemName:"checkmark.circle.fill").foregroundStyle(Brand.orange).accessibilityLabel("Kaydedildi")}else{Button{editing=DraftEdit(message:message.id,draft:draft.id,plan:draft.plan)}label:{Image(systemName:"slider.horizontal.3").frame(width:48,height:48)}.accessibilityLabel("\(p.title) taslağını düzenle")}}.surfaceCard()}}
   if message.plans.contains(where:{!$0.saved}){Text("Onayını bekliyor").brandFont(14).foregroundStyle(Brand.muted);Button("Onayla ve ekle"){Task{await model.approveMessage(message.id)}}.buttonStyle(PrimaryButton()).disabled(model.busy)}
   ForEach(message.memories){draft in VStack(alignment:.leading,spacing:12){Label("Hatırlamamı ister misin?",systemImage:"brain").brandFont(16,weight:750);Text(draft.text).brandFont(16);if draft.saved{Label("Onayınla kaydedildi",systemImage:"checkmark.circle").foregroundStyle(Brand.orange)}else{Button("İncele ve hatırla"){memoryChoice=MemoryChoice(message:message.id,draft:draft.id)}.buttonStyle(QuietButton())}}.surfaceCard()}
  }
 }
 private var composer:some View{VStack(alignment:.leading,spacing:10){
  DisclosureGroup("AI ile paylaşılan bilgiler"){Toggle("Plan bağlamını paylaş",isOn:Binding(get:{model.sharePlans},set:{model.setSharePlans($0)}));Text("Açıksa en fazla 150 planın başlığı, günü, saatleri ve durumu gönderilir. Notların gönderilmez. Yazdığın mesaj ve seçili takvim günü her isteğin parçasıdır.").brandFont(13).foregroundStyle(Brand.muted);Toggle("Onayladığım hafızayı kullan",isOn:Binding(get:{model.useMemory},set:{model.setUseMemory($0)}))}.brandFont(13)
  HStack(alignment:.bottom,spacing:8){TextField("Planını yaz…",text:Binding(get:{model.input},set:{model.setInput($0)}),axis:.vertical).lineLimit(1...5).brandFont(17).padding(.vertical,12).accessibilityIdentifier("ai-input");Button{Task{await speech.start{model.setInput($0)}}}label:{Image(systemName:speech.recording ? "stop.circle.fill":"mic").font(.title2).frame(width:48,height:48)}.accessibilityLabel(speech.recording ? "Kaydı bitir":"Sesle yaz");Button{speech.stop();model.send()}label:{Image(systemName:"arrow.up").font(.title2).foregroundStyle(Brand.background).frame(width:48,height:48).background(Brand.orange,in:Circle())}.disabled(model.asking||model.input.trimmingCharacters(in:.whitespacesAndNewlines).isEmpty).accessibilityLabel("Rotam AI’ya gönder")}.padding(.horizontal,12).padding(.vertical,6).background(Brand.surface,in:RoundedRectangle(cornerRadius:25))
 }.padding(16).background(Brand.background)}
}
import AVFoundation
private struct DraftEdit:Identifiable{let id=UUID();let message:UUID;let draft:UUID;let plan:WireRecord}
private struct MemoryChoice:Identifiable{let id=UUID();let message:UUID;let draft:UUID}

struct PlanEditorView:View {
 @EnvironmentObject var model:RotamModel
 @Environment(\.dismiss) private var dismiss
 let record:WireRecord?
 let onSave:(WireRecord)async->Void
 @State private var title:String
 @State private var date:Date
 @State private var startEnabled:Bool
 @State private var endEnabled:Bool
 @State private var start:Date
 @State private var end:Date
 @State private var finishDay:Date
 @State private var category:String
 @State private var note:String
 @State private var repeatType:String
 @State private var repeatDays:String
 @State private var saving=false
 @State private var validation:String?
 init(record:WireRecord?=nil,day:String,onSave:@escaping(WireRecord)async->Void){
  self.record=record;self.onSave=onSave;let p=record.flatMap{try? PortablePlan($0)},d=(try? PlanDates.instant(day:p?.date ?? day,time:"12:00")) ?? Date()
  _title=State(initialValue:p?.title ?? "");_date=State(initialValue:d);_startEnabled=State(initialValue:p?.start != nil);_endEnabled=State(initialValue:p?.end != nil)
  _start=State(initialValue:(try? PlanDates.instant(day:p?.date ?? day,time:p?.start ?? "09:00")) ?? d);_end=State(initialValue:(try? PlanDates.instant(day:p?.endDate ?? day,time:p?.end ?? "10:00")) ?? d);_finishDay=State(initialValue:(try? PlanDates.instant(day:p?.endDate ?? day,time:"12:00")) ?? d)
  _category=State(initialValue:p?.category ?? "kisisel");_note=State(initialValue:p?.metadata["note"]?.string ?? "");_repeatType=State(initialValue:p?.fields["repeatType"]?.string ?? "none");_repeatDays=State(initialValue:p?.fields["repeatDays"]?.string ?? "")
 }
 var body:some View{ScrollView{VStack(alignment:.leading,spacing:22){Text(record==nil ? "Gününe bir yer aç.":"Planını düzenle.").brandFont(32,weight:800);VStack(alignment:.leading,spacing:10){Text("Plan adı").brandFont(14,weight:700);TextField("Ne yapmak istiyorsun?",text:$title,axis:.vertical).brandFont(23,weight:750).padding(16).background(Brand.surface,in:RoundedRectangle(cornerRadius:18)).accessibilityIdentifier("plan-title")}
   DatePicker("Gün",selection:$date,displayedComponents:.date).frame(minHeight:48)
   VStack(spacing:16){Toggle("Başlangıç saati",isOn:$startEnabled);if startEnabled{DatePicker("Başlangıç",selection:$start,displayedComponents:.hourAndMinute)};Divider();Toggle("Bitiş / son teslim",isOn:$endEnabled);if endEnabled{DatePicker("Bitiş",selection:$end,displayedComponents:.hourAndMinute);DatePicker("Bitiş günü",selection:$finishDay,in:Calendar.current.startOfDay(for:date)...,displayedComponents:.date)};if !startEnabled && endEnabled{Text("Bu bir son teslim saati. Başlangıç eklenmez.").brandFont(14).foregroundStyle(Brand.muted)}}.surfaceCard()
   Picker("Kategori",selection:$category){ForEach([("kisisel","Kişisel"),("kitap","Okuma"),("is","İş"),("video","İçerik"),("spor","Spor"),("ibadet","İbadet"),("randevu","Randevu"),("yemek","Yemek"),("ogrenme","Öğrenme")],id:\.0){Text($0.1).tag($0.0)}}.frame(minHeight:48)
   if (try? record.map{try PortablePlan($0).seriesUID}) == nil{Picker("Tekrar",selection:$repeatType){ForEach([("none","Tek sefer"),("daily","Her gün"),("weekdays","Hafta içi"),("weekly","Her hafta"),("custom","Seçili günler"),("monthly","Her ay")],id:\.0){Text($0.1).tag($0.0)}}.frame(minHeight:48);if repeatType=="custom"{TextField("Günler · 1=Pzt, 7=Paz (ör. 1,4,6)",text:$repeatDays).keyboardType(.numbersAndPunctuation).padding(14).background(Brand.surface,in:RoundedRectangle(cornerRadius:16))}}
   VStack(alignment:.leading,spacing:8){Text("Not · isteğe bağlı").brandFont(14,weight:700);TextField("Eklemek istediğin ayrıntılar",text:$note,axis:.vertical).lineLimit(3...8).padding(16).background(Brand.surface,in:RoundedRectangle(cornerRadius:18))}
   if let validation{Label(validation,systemImage:"exclamationmark.circle").brandFont(15).foregroundStyle(Brand.orange)}
   Button{save()}label:{HStack{Text("Kaydet");if saving{ProgressView()}}}.buttonStyle(PrimaryButton()).disabled(saving||title.trimmingCharacters(in:.whitespacesAndNewlines).isEmpty).accessibilityIdentifier("save-plan")
  }.padding(24).frame(maxWidth:760)}.background(Brand.background).foregroundStyle(Brand.ink).navigationTitle(record==nil ? "Plan ekle":"Planı düzenle").navigationBarTitleDisplayMode(.inline).toolbar{ToolbarItem(placement:.cancellationAction){Button("Vazgeç"){dismiss()}}}.onChange(of:date){_,d in if finishDay<d{finishDay=d}}
 }
 private func clock(_ d:Date)->String{let c=Calendar.current.dateComponents([.hour,.minute],from:d);return String(format:"%02d:%02d",c.hour!,c.minute!)}
 private func save(){do{
  let day=PlanDates.key(date),last=PlanDates.key(finishDay),base=try record ?? PortablePlan.make(title:title.trimmingCharacters(in:.whitespacesAndNewlines),date:day)
  var changes:[String:JSONValue]=["title":.string(title.trimmingCharacters(in:.whitespacesAndNewlines)),"date":.string(day),"startTime":startEnabled ? .string(clock(start)):.null,"endTime":endEnabled ? .string(clock(end)):.null,"endDate":endEnabled&&last != day ? .string(last):.null,"category":.string(category)]
  var metadata=try PortablePlan(base).metadata;metadata["note"] = .string(note);changes["extraData"] = .string(try JSONValue.object(metadata).canonical())
  if try PortablePlan(base).seriesUID==nil{changes["repeatType"] = .string(repeatType);changes["repeatDays"] = .string(repeatDays)}
  let plan=try base.changing(changes);var graph=model.records.filter{$0.key != plan.key};graph.append(plan);try CoreValidator.validate(graph)
  validation=nil;saving=true;model.error=nil
  Task{await onSave(plan);saving=false;if model.error==nil{dismiss()}}
 }catch{validation=error.localizedDescription}}
}
