import WidgetKit
import SwiftUI

struct RotamEntry:TimelineEntry {let date:Date;let snapshot:WidgetSnapshot?}
struct RotamProvider:TimelineProvider {
 func placeholder(in context:Context)->RotamEntry{RotamEntry(date:Date(),snapshot:nil)}
 func getSnapshot(in context:Context,completion:@escaping(RotamEntry)->Void){completion(load())}
 func getTimeline(in context:Context,completion:@escaping(Timeline<RotamEntry>)->Void){let entry=load();completion(Timeline(entries:[entry],policy:.after(Calendar.current.date(byAdding:.minute,value:30,to:Date())!)))}
 private func load()->RotamEntry{
  let data=FileManager.default.containerURL(forSecurityApplicationGroupIdentifier:"group.com.rota.app").flatMap{try? Data(contentsOf:$0.appendingPathComponent("widget.json"))},value=data.flatMap{try? JSONDecoder().decode(WidgetSnapshot.self,from:$0)}
  return RotamEntry(date:Date(),snapshot:value?.day==PlanDates.key(Date()) ? value:nil)
 }
}
struct RotamWidgetView:View {
 @Environment(\.widgetFamily) private var family
 let entry:RotamEntry
 private let orange=Color(red:1,green:0.5,blue:0.247)
 var body:some View{VStack(alignment:.leading,spacing:12){HStack{Text("◆").foregroundStyle(orange).accessibilityHidden(true);Text("Bugünün rotası").font(.headline);Spacer();Link(destination:URL(string:"rotam://add")!){Image(systemName:"plus.circle.fill").foregroundStyle(orange)}.accessibilityLabel("Plan ekle")}
  if let s=entry.snapshot{Text("\(s.completed) / \(s.total) tamamlandı").font(.caption).foregroundStyle(.secondary);if s.plans.isEmpty{Text(s.total==0 ? "Gününe yer aç.":"Bugünün planları tamam.").font(.title3).bold()}else{ForEach(Array(s.plans.prefix(family == .systemSmall ? 1:family == .systemMedium ? 2:5))){p in Link(destination:URL(string:"rotam://plan?uid="+p.id.addingPercentEncoding(withAllowedCharacters:.urlQueryAllowed)!)!){HStack(alignment:.top){Image(systemName:"calendar").foregroundStyle(orange);VStack(alignment:.leading,spacing:4){Text(p.title).font(.subheadline).bold().lineLimit(2);Text(p.schedule).font(.caption).foregroundStyle(.secondary)};Spacer()}}}}}
  else{Text("Bugününü aç,\nrotanı görelim.").font(.title3).bold();Link("Rotam’ı aç",destination:URL(string:"rotam://today")!).font(.caption).foregroundStyle(orange)};Spacer(minLength:0)
 }.containerBackground(Color(red:0.06,green:0.07,blue:0.08),for:.widget).widgetURL(URL(string:"rotam://today")!)}
}
@main struct RotamWidget:Widget {
 var body:some WidgetConfiguration{StaticConfiguration(kind:"RotamToday",provider:RotamProvider()){entry in RotamWidgetView(entry:entry)}.configurationDisplayName("Bugünün rotası").description("Planlarını küçük, orta veya büyük görünümde takip et.").supportedFamilies([.systemSmall,.systemMedium,.systemLarge])}
}
