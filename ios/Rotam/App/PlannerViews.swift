import SwiftUI
import Charts

struct HomeView:View {
 @EnvironmentObject var model:RotamModel
 @Binding var path:[Route]
 @Binding var adding:Bool
 @State private var deckIndex=0
 var body:some View{
  let today=model.today,completed=today.filter(\.completed).count,current=today.first{!$0.completed && !$0.skipped && !$0.awaiting()},awaiting=model.pending
  ScrollView{VStack(alignment:.leading,spacing:24){
   HStack{BrandMark();Spacer();Button{path.append(.search)}label:{Image(systemName:"magnifyingglass").font(.title2).frame(width:48,height:48)}.accessibilityLabel("Planlarında ara")}
   Text(Date().formatted(.dateTime.weekday(.wide).day().month(.wide)).uppercased(with:Locale(identifier:"tr_TR"))).brandFont(13,weight:750).tracking(2).foregroundStyle(Brand.muted)
   Text("Bugün,\nsenin ritmin.").brandFont(42,weight:800).fixedSize(horizontal:false,vertical:true)
   HStack{Text(today.isEmpty ? "Gününe yer aç.":"\(completed) / \(today.count) tamamlandı").brandFont(17).foregroundStyle(Brand.muted);Spacer();Button{path.append(.insights)}label:{Image(systemName:"chart.bar.fill").frame(width:48,height:44)}.accessibilityLabel("İlerlemem")}
   if !today.isEmpty{HStack(spacing:6){ForEach(0..<min(today.count,30),id:\.self){i in Capsule().fill(i<completed ? Brand.orange:Brand.raised).frame(height:7)}}.accessibilityElement(children:.ignore).accessibilityLabel("Bugün \(today.count) planın \(completed) tanesi tamamlandı")}
   if let current{HeroCard{VStack(alignment:.leading,spacing:12){HStack{Diamond();Text("SIRADAKİ").brandFont(13,weight:800).tracking(2).foregroundStyle(Brand.muted)};Text(current.title).brandFont(31,weight:800);Text(current.scheduleLabel).brandFont(18).foregroundStyle(Brand.muted);Button("Ayrıntıları gör"){path.append(.plan(current.id))}.brandFont(15,weight:700)}}action:{Button{path.append(.focus(current.id))}label:{Image(systemName:"play.fill").font(.title2).foregroundStyle(Brand.background).frame(width:76,height:76).background(Brand.ink,in:Circle())}.accessibilityLabel("\(current.title) için odaklan")}}
   else if today.isEmpty{HeroCard{VStack(alignment:.leading,spacing:15){Diamond();Text("İlk adım sana ait.").brandFont(29,weight:800);Text("Aklındakini yaz veya söyle. Önce birlikte inceleyelim.").brandFont(17).foregroundStyle(Brand.muted);Button("Plan ekle"){adding=true}.buttonStyle(QuietButton())}}action:{EmptyView()}}
   if !awaiting.isEmpty{let plan=awaiting[min(deckIndex,awaiting.count-1)]
    VStack(alignment:.leading,spacing:14){HStack{Text("Yanıtını bekliyor").brandFont(20,weight:800);Spacer();Text("\(min(deckIndex+1,awaiting.count)) / \(awaiting.count)").brandFont(13).foregroundStyle(Brand.muted)};Text(plan.title).brandFont(23,weight:800);Text(plan.date+" · "+plan.scheduleLabel).brandFont(14).foregroundStyle(Brand.muted);Text("Geçmiş olması yapılmadığı anlamına gelmez.").brandFont(14).foregroundStyle(Brand.muted);ViewThatFits{HStack{deckButtons(plan)};VStack{deckButtons(plan)}};if awaiting.count>1{Button("Diğer plan"){deckIndex=(deckIndex+1)%awaiting.count}.frame(minHeight:44)}}.surfaceCard().id(plan.id).transition(.opacity)
   }
   HStack{SectionTitle(title:"Günün devamı");Button{adding=true}label:{Label("Plan ekle",systemImage:"plus.circle")}.brandFont(15,weight:700)}
   let remainder=today.filter{$0.id != current?.id && !$0.awaiting()}
   if remainder.isEmpty{Text(today.isEmpty ? "İlk planın burada görünecek.":"Bugünün kalan akışı burada görünecek.").brandFont(16).foregroundStyle(Brand.muted)}
   ForEach(remainder){p in Button{path.append(.plan(p.id))}label:{PlanRow(plan:p)}.buttonStyle(.plain);Divider().overlay(Brand.line)}
   Button{adding=true}label:{HStack{Diamond();Text("Rotam AI ile planla");Spacer();Image(systemName:"arrow.up.right")}.brandFont(17,weight:750).surfaceCard()}.buttonStyle(.plain).padding(.top,12)
  }.padding(24).padding(.bottom,24).frame(maxWidth:760)}.background(Brand.background).toolbar(.hidden,for:.navigationBar)
 }
 @ViewBuilder private func deckButtons(_ p:PortablePlan)->some View{Button("Yapmadım"){Task{await model.outcome(p,"NOT_DONE")}}.buttonStyle(QuietButton());Button("İncele"){path.append(.plan(p.id))}.buttonStyle(QuietButton());Button("Tamamladım"){Task{await model.outcome(p,"COMPLETED")}}.buttonStyle(QuietButton())}
}
struct CalendarView:View {
 @EnvironmentObject var model:RotamModel
 @Binding var path:[Route]
 @Binding var adding:Bool
 @State private var date=Date()
 @State private var mode=2
 private let modes=["Gün","Hafta","Ay","Yıl"]
 var body:some View{ScrollView{VStack(alignment:.leading,spacing:20){HStack{Text("Takvim").brandFont(38,weight:800);Spacer();Button("İlerlemem"){path.append(.insights)}.brandFont(14,weight:700)};Text("Günlerine uzaktan bir bakış.").brandFont(17).foregroundStyle(Brand.muted)
   Picker("Görünüm",selection:$mode){ForEach(0..<modes.count,id:\.self){Text(modes[$0]).tag($0)}}.pickerStyle(.segmented)
   if mode==2{DatePicker("Gün",selection:$date,displayedComponents:.date).datePickerStyle(.graphical)}else if mode==3{yearGrid}else{DatePicker("Gün seç",selection:$date,displayedComponents:.date).datePickerStyle(.compact).frame(minHeight:48)}
   HStack{SectionTitle(title:mode==1 ? "Bu hafta":mode==3 ? "Seçili ay":"Seçili gün");Button{model.selectedDay=PlanDates.key(date);adding=true}label:{Image(systemName:"plus.circle").font(.title2).frame(width:48,height:48)}.accessibilityLabel("Seçili güne plan ekle")}
   let rows=filtered
   if rows.isEmpty{EmptyState(symbol:"calendar",title:"Gününe yer var.",detail:"Plan eklediğinde burada görebilirsin.")}
   ForEach(rows){p in Button{path.append(.plan(p.id))}label:{VStack(alignment:.leading,spacing:0){if mode==1||mode==3{Text(p.date).brandFont(13).foregroundStyle(Brand.orange)};PlanRow(plan:p)}}.buttonStyle(.plain);Divider()}
  }.padding(24).frame(maxWidth:760)}.background(Brand.background).toolbar(.hidden,for:.navigationBar).onChange(of:date){_,d in model.selectedDay=PlanDates.key(d)}
 }
 private var filtered:[PortablePlan]{let key=PlanDates.key(date),cal=Calendar.current
  if mode==3{return model.plans.filter{$0.date.prefix(7)==key.prefix(7)}}
  if mode==1,let interval=cal.dateInterval(of:.weekOfYear,for:date){let a=PlanDates.key(interval.start),b=PlanDates.key(interval.end);return model.plans.filter{$0.date>=a&&$0.date<b}}
  return model.plans.filter{$0.date==key}
 }
 private var yearGrid:some View{VStack(alignment:.leading,spacing:12){HStack{Button{date=Calendar.current.date(byAdding:.year,value:-1,to:date)!}label:{Image(systemName:"chevron.left").frame(width:48,height:48)}.accessibilityLabel("Önceki yıl");Spacer();Text(date.formatted(.dateTime.year())).brandFont(25,weight:800);Spacer();Button{date=Calendar.current.date(byAdding:.year,value:1,to:date)!}label:{Image(systemName:"chevron.right").frame(width:48,height:48)}.accessibilityLabel("Sonraki yıl")};LazyVGrid(columns:Array(repeating:GridItem(.flexible()),count:3),spacing:12){ForEach(1...12,id:\.self){month in Button{var c=Calendar.current.dateComponents([.year],from:date);c.month=month;c.day=1;date=Calendar.current.date(from:c)!}label:{Text(Calendar.current.shortMonthSymbols[month-1]).brandFont(16,weight:700).frame(maxWidth:.infinity,minHeight:52).background(Calendar.current.component(.month,from:date)==month ? Brand.orange:Brand.surface,in:RoundedRectangle(cornerRadius:14)).foregroundStyle(Calendar.current.component(.month,from:date)==month ? Brand.background:Brand.ink)}}}}}
}
struct RoutinesView:View {
 @EnvironmentObject var model:RotamModel
 @Binding var path:[Route]
 @Binding var adding:Bool
 var series:[WireRecord]{model.records.filter{$0.kind == .series && (try? $0.fields()["isDeleted"]?.boolean) != true}}
 var body:some View{ScrollView{VStack(alignment:.leading,spacing:24){Text("Rutinler").brandFont(38,weight:800);Text("Günlerine yerleşen küçük düzenler.").brandFont(17).foregroundStyle(Brand.muted);Button{adding=true}label:{Label("Yeni rutinini yaz veya söyle",systemImage:"sparkles")}.buttonStyle(QuietButton())
   if series.isEmpty{EmptyState(symbol:"repeat",title:"Küçük bir düzenle başla.",detail:"Örneğin: Her pazartesi, perşembe ve cumartesi spor.")}
   ForEach(series,id:\.uid){r in let children=model.plans.filter{$0.seriesUID==r.uid},next=children.first{!$0.completed && !$0.skipped && $0.date>=PlanDates.key(Date())},template=(try? r.fields()["template"]?.fields) ?? [:]
    VStack(alignment:.leading,spacing:14){HStack{Image(systemName:Brand.icon(template["category"]?.string ?? "")).font(.title);Text(template["title"]?.string ?? "Rutin").brandFont(25,weight:800);Spacer()};Text(routineLabel(template)).brandFont(16).foregroundStyle(Brand.muted);let week=children.filter{isCurrentWeek($0.date)},done=week.filter(\.completed).count
     if !week.isEmpty{ProgressView(value:Double(done),total:Double(week.count)).tint(Brand.orange);Text("Bu hafta \(done) / \(week.count) tamamlandı").brandFont(14).foregroundStyle(Brand.muted)}
     if let next{Text("Sıradaki · \(next.date) · \(next.scheduleLabel)").brandFont(14).foregroundStyle(Brand.muted);Button("Ayrıntıları gör"){path.append(.plan(next.id))}.buttonStyle(QuietButton())}
    }.surfaceCard()
   };Text("Günü gelen rutinler Bugün ekranında da görünür.").brandFont(14).foregroundStyle(Brand.muted)
  }.padding(24).frame(maxWidth:760)}.background(Brand.background).toolbar(.hidden,for:.navigationBar)}
 private func isCurrentWeek(_ date:String)->Bool{guard let d=try? PlanDates.day(date)else{return false};return Calendar.current.isDate(d,equalTo:Date(),toGranularity:.weekOfYear)}
 private func routineLabel(_ d:[String:JSONValue])->String{let type=d["repeatType"]?.string ?? "none";return ["daily":"Her gün","weekdays":"Hafta içi","weekly":"Her hafta","custom":"Seçili günler","monthly":"Her ay","interval":"Belirli aralıklarla"][type] ?? "Rutin"}
}
struct SearchView:View {
 @EnvironmentObject var model:RotamModel
 @Binding var path:[Route]
 @State private var text=""
 var body:some View{ScrollView{LazyVStack(alignment:.leading,spacing:12){let normalized=text.folding(options:[.caseInsensitive,.diacriticInsensitive],locale:Locale(identifier:"tr_TR")),rows=model.plans.filter{p in text.isEmpty || (p.title+" "+p.date+" "+(p.metadata["note"]?.string ?? "")).folding(options:[.caseInsensitive,.diacriticInsensitive],locale:Locale(identifier:"tr_TR")).contains(normalized)}
   Text("\(rows.count) plan").brandFont(14).foregroundStyle(Brand.muted)
   ForEach(rows){p in Button{path.append(.plan(p.id))}label:{VStack(alignment:.leading,spacing:0){Text(p.date).brandFont(13).foregroundStyle(Brand.orange);PlanRow(plan:p)}}.buttonStyle(.plain);Divider()}
   if rows.isEmpty{EmptyState(symbol:"magnifyingglass",title:"Eşleşen plan yok.",detail:"Başlık, not veya tarih ile arayabilirsin.")}
  }.padding(24)}.background(Brand.background).navigationTitle("Planlarında ara").searchable(text:$text,prompt:"Başlık, not veya tarih")}
}
struct InsightsView:View {
 @EnvironmentObject var model:RotamModel
 @State private var month=false
 @State private var date=Date()
 @State private var selected:String?
 var body:some View{let interval=Calendar.current.dateInterval(of:month ? .month:.weekOfYear,for:date)!,a=PlanDates.key(interval.start),b=PlanDates.key(interval.end),rows=model.plans.filter{$0.date>=a&&$0.date<b},done=rows.filter(\.completed).count,pending=rows.filter{$0.awaiting()}.count,notDone=rows.filter(\.skipped).count
  ScrollView{VStack(alignment:.leading,spacing:24){Picker("Dönem",selection:$month){Text("Hafta").tag(false);Text("Ay").tag(true)}.pickerStyle(.segmented);HStack{Button{move(-1)}label:{Image(systemName:"chevron.left").frame(width:48,height:48)}.accessibilityLabel("Önceki dönem");Spacer();Text(interval.start.formatted(.dateTime.day().month())+" – "+interval.end.addingTimeInterval(-1).formatted(.dateTime.day().month())).brandFont(16,weight:700);Spacer();Button{move(1)}label:{Image(systemName:"chevron.right").frame(width:48,height:48)}.accessibilityLabel("Sonraki dönem")}
   HeroCard{VStack(alignment:.leading,spacing:12){Diamond();Text("Planlarım").brandFont(16).foregroundStyle(Brand.muted);Text("\(done) / \(rows.count)").brandFont(42,weight:800);Text("plan tamamlandı").brandFont(17);ProgressView(value:Double(done),total:Double(max(rows.count,1))).tint(Brand.orange);Text("\(pending) yanıt bekliyor · \(notDone) yapılmadı").brandFont(14).foregroundStyle(Brand.muted)}}action:{EmptyView()}
   SectionTitle(title:"Odak sürem")
   let bars=focusBars(interval),total=bars.reduce(Int64(0)){$0+$1.seconds}
   Text("\(total/3600) sa \(total%3600/60) dk").brandFont(31,weight:800)
   Chart(bars,id:\.day){bar in BarMark(x:.value("Gün",bar.day),y:.value("Dakika",Double(bar.seconds)/60)).foregroundStyle(Brand.orange).cornerRadius(5)}.frame(height:180).accessibilityLabel("Bu dönemde toplam \(total/60) dakika ölçülen odak süresi")
   Text("Yalnız ölçülen süreler gösterilir. Tarihi olmayan eski süreler bu grafiğe eklenmez.").brandFont(14).foregroundStyle(Brand.muted)
   SectionTitle(title:"Planların")
   ForEach(rows){p in VStack(alignment:.leading,spacing:4){Text(p.title).brandFont(17,weight:700);Text(p.date+" · "+status(p)).brandFont(14).foregroundStyle(Brand.muted)}.padding(.vertical,8)}
  }.padding(24).frame(maxWidth:760)}.background(Brand.background).navigationTitle("İlerlemem")}
 private func move(_ n:Int){date=Calendar.current.date(byAdding:month ? .month:.weekOfYear,value:n,to:date)!}
 private func status(_ p:PortablePlan)->String{p.completed ? "Tamamlandı":p.skipped ? "Yapılmadı":p.outcome=="PARTIAL" ? "Kısmen tamamlandı":p.awaiting() ? "Yanıt bekliyor":"Planlandı"}
 private func focusBars(_ period:DateInterval)->[(day:String,seconds:Int64)]{
  var counts:[String:Int64]=[:];var c=Calendar(identifier:.gregorian);c.timeZone = .current
  for p in model.plans{for item in p.metadata["focusSegments"]?.items ?? []{guard let fields=item.fields,let ms=fields["startedAt"]?.integer,let seconds=fields["seconds"]?.integer,seconds>0 else{continue};var start=Date(timeIntervalSince1970:Double(ms)/1000),remaining=Double(seconds)
    while remaining>0{let next=c.date(byAdding:.day,value:1,to:c.startOfDay(for:start))!,chunk=min(remaining,next.timeIntervalSince(start));let end=start.addingTimeInterval(chunk),from=max(start,period.start),to=min(end,period.end);if to>from{counts[PlanDates.key(start),default:0]+=Int64(to.timeIntervalSince(from))};start=end;remaining-=chunk}
   }}
  return counts.sorted{$0.key<$1.key}.map{($0.key,$0.value)}
 }
}
