import SwiftUI

struct PlanDetailView:View {
 @EnvironmentObject var model:RotamModel
 let uid:String
 @Binding var path:[Route]
 @State private var editing=false
 @State private var postpone=false
 @State private var partial=false
 @State private var deletion=false
 @State private var ai=false
 var plan:PortablePlan?{model.plans.first{$0.id==uid}}
 var body:some View{Group{if let p=plan{ScrollView{VStack(alignment:.leading,spacing:24){Image(systemName:Brand.icon(p.category)).font(.largeTitle).foregroundStyle(Brand.orange);Text(p.title).brandFont(38,weight:800);Text(p.date+" · "+p.scheduleLabel).brandFont(18).foregroundStyle(Brand.muted)
   if p.seriesUID != nil{Label("Rutin · değişiklikler yalnız bu örneğe uygulanır",systemImage:"repeat").brandFont(14).foregroundStyle(Brand.muted)}
   if p.completed{Label("Tamamlandı",systemImage:"checkmark.circle.fill").foregroundStyle(Brand.orange)}else if p.skipped{Label("Yapılmadı",systemImage:"minus.circle").foregroundStyle(Brand.muted)}else if p.outcome=="PARTIAL"{Label("Kısmen tamamlandı",systemImage:"circle.lefthalf.filled").foregroundStyle(Brand.orange)}
   Button{editing=true}label:{HStack{Label("Gün ve saat",systemImage:"calendar");Spacer();Text("Düzenle");Image(systemName:"chevron.right")}.brandFont(17).surfaceCard()}.buttonStyle(.plain)
   if let note=p.metadata["note"]?.string,!note.isEmpty{Text(note).brandFont(17).textSelection(.enabled).surfaceCard()}
   let tasks=(p.metadata["subtasks"]?.string ?? "").split(separator:"\n").map(String.init),checked=Set((p.metadata["checkedSubtasks"]?.string ?? "").split(separator:"\n").map(String.init))
   if !tasks.isEmpty{SectionTitle(title:"Yapılacaklar");ForEach(tasks,id:\.self){task in Button{Task{await model.toggleSubtask(p,task:task,done:!checked.contains(task))}}label:{HStack(spacing:12){Image(systemName:checked.contains(task) ? "checkmark.square.fill":"square").foregroundStyle(checked.contains(task) ? Brand.orange:Brand.muted);Text(task).brandFont(17);Spacer()}.frame(minHeight:48)}.buttonStyle(.plain).accessibilityValue(checked.contains(task) ? "Tamamlandı":"Bekliyor")}}
   if p.focusSeconds>0{Label("Ölçülen odak · \(p.focusSeconds/60) dk",systemImage:"timer").brandFont(16).foregroundStyle(Brand.muted)}
   Button{path.append(.focus(p.id))}label:{Label("Odaklan",systemImage:"timer")}.buttonStyle(QuietButton())
   Button{model.setInput("Bu planı düzenlemek istiyorum: "+p.title);model.setSharePlans(true);ai=true}label:{Label("Rotam AI ile düzenle",systemImage:"sparkles")}.buttonStyle(QuietButton())
   Button(p.completed ? "Tamamlanmayı geri al":"Tamamladım"){Task{await model.outcome(p,p.completed ? "UNKNOWN":"COMPLETED")}}.buttonStyle(PrimaryButton()).disabled(model.busy)
   ViewThatFits{HStack{secondary(p)};VStack{secondary(p)}}
   if let history=p.metadata["outcomeHistory"]?.items,!history.isEmpty{DisclosureGroup("Geçmiş hareketler"){ForEach(Array(history.enumerated()),id:\.offset){_,value in let d=value.fields ?? [:];Text((d["at"]?.string ?? "")+" · "+historyLabel(d["status"]?.string ?? "")+" · "+(d["date"]?.string ?? "")).brandFont(13).foregroundStyle(Brand.muted).frame(maxWidth:.infinity,alignment:.leading).padding(.vertical,8)}}}
  }.padding(24).frame(maxWidth:760)}}else{ContentUnavailableView("Plan bulunamadı",systemImage:"calendar.badge.exclamationmark",description:Text("Silinmiş veya başka bir cihazda değiştirilmiş olabilir."))}}.background(Brand.background).foregroundStyle(Brand.ink).navigationTitle("Plan detayı").navigationBarTitleDisplayMode(.inline)
 .toolbar{ToolbarItem(placement:.topBarTrailing){Button(role:.destructive){deletion=true}label:{Image(systemName:"trash").frame(width:44,height:44)}.accessibilityLabel("Planı sil")}}
 .sheet(isPresented:$editing){if let p=plan{NavigationStack{PlanEditorView(record:p.record,day:p.date){next in await model.edit(next,before:p.record)}}}}
 .sheet(isPresented:$postpone){if let p=plan{NavigationStack{PostponeView(plan:p)}}}
 .sheet(isPresented:$partial){if let p=plan{NavigationStack{PartialView(plan:p)}}}
 .sheet(isPresented:$ai){NavigationStack{AiView()}}
 .confirmationDialog("Bu plan silinsin mi?",isPresented:$deletion,titleVisibility:.visible){Button("Sil",role:.destructive){if let p=plan{Task{await model.delete(p)}}};Button("Vazgeç",role:.cancel){}}message:{Text("Rutinse yalnız bu günkü örnek silinir. İşlemi geri alabilirsin.")}
 }
 @ViewBuilder private func secondary(_ p:PortablePlan)->some View{Button("Kısmen yaptım"){partial=true}.buttonStyle(QuietButton());Button("Ertele"){postpone=true}.buttonStyle(QuietButton());Button(p.skipped ? "Yanıtı geri al":"Yapmadım"){Task{await model.outcome(p,p.skipped ? "UNKNOWN":"NOT_DONE")}}.buttonStyle(QuietButton())}
 private func historyLabel(_ value:String)->String{["COMPLETED":"Tamamlandı","PARTIAL":"Kısmen yapıldı","UNKNOWN":"Yanıt geri alındı","NOT_DONE":"Yapılmadı","POSTPONED":"Ertelendi"][value] ?? value}
}
struct PartialView:View {
 @EnvironmentObject var model:RotamModel
 @Environment(\.dismiss) private var dismiss
 let plan:PortablePlan
 @State private var amount=""
 @State private var target=""
 var body:some View{Form{Section{Text(plan.title);TextField("Yaptığın miktar",text:$amount).keyboardType(.decimalPad);TextField("Toplam hedef",text:$target).keyboardType(.decimalPad)}footer:{Text("Örneğin 10 sayfanın 4 sayfası. Ölçülen odak süresi bu miktardan hesaplanmaz.")};Button("Kısmi ilerlemeyi kaydet"){guard let a=Double(amount.replacingOccurrences(of:",",with:".")),let t=Double(target.replacingOccurrences(of:",",with:"."))else{model.error="Miktarları sayı olarak yaz.";return};Task{model.error=nil;await model.outcome(plan,"PARTIAL",amount:a,target:t);if model.error==nil{dismiss()}}}}.navigationTitle("Kısmi ilerleme").toolbar{ToolbarItem(placement:.cancellationAction){Button("Vazgeç"){dismiss()}}}}
}
struct PostponeView:View {
 @EnvironmentObject var model:RotamModel
 @Environment(\.dismiss) private var dismiss
 let plan:PortablePlan
 @State private var date=Date().addingTimeInterval(86400)
 @State private var time=Date()
 var body:some View{ScrollView{VStack(alignment:.leading,spacing:24){Text("\(plan.title) ne zamana taşınsın?").brandFont(30,weight:800);Text("Şu an · \(plan.date) · \(plan.scheduleLabel)").brandFont(16).foregroundStyle(Brand.muted);DatePicker("Yeni gün",selection:$date,displayedComponents:.date);if plan.start != nil||plan.end != nil{DatePicker(plan.start != nil ? "Başlangıç":"Son teslim",selection:$time,displayedComponents:.hourAndMinute)};Text(plan.start != nil && plan.end != nil ? "Süre ve varsa ertesi güne uzanan bitiş korunur.":plan.start==nil && plan.end==nil ? "Saati olmayan plana saat eklenmez.":"Diğer saat alanı boş kalır.").brandFont(15).foregroundStyle(Brand.muted);if plan.seriesUID != nil{Text("Yalnız bu günkü örnek değişir. Sonraki tekrarlar korunur.").brandFont(15).foregroundStyle(Brand.muted)};Button("Ertelemeyi onayla"){let c=Calendar.current.dateComponents([.hour,.minute],from:time),clock=String(format:"%02d:%02d",c.hour!,c.minute!);Task{model.error=nil;await model.postpone(plan,date:PlanDates.key(date),time:clock);if model.error==nil{dismiss()}}}.buttonStyle(PrimaryButton())}.padding(24)}.background(Brand.background).navigationTitle("Ertele").toolbar{ToolbarItem(placement:.cancellationAction){Button("Vazgeç"){dismiss()}}}.onAppear{time=(try? PlanDates.instant(day:plan.date,time:plan.start ?? plan.end ?? "12:00")) ?? Date();date=(try? PlanDates.instant(day:PlanDates.addDays(plan.date,1),time:"12:00")) ?? Date()}}
}
