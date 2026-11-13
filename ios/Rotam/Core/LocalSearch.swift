import Foundation
public struct SearchResult:Sendable {public let plans:[PortablePlan];public let answer:String?;public let needsAI:Bool}
public enum LocalSearch {
 public static func normalize(_ s:String)->String{s.lowercased(with:Locale(identifier:"tr_TR")).replacingOccurrences(of:"ı",with:"i").folding(options:[.diacriticInsensitive],locale:Locale(identifier:"tr_TR"))}
 public static func search(_ query:String,plans:[PortablePlan],now:Date=Date())->SearchResult{
  let q=normalize(query),calendar=Calendar.current
  var from:String?,to:String?,remaining=q
  func range(_ interval:DateInterval){from=PlanDates.key(interval.start);to=PlanDates.key(interval.end.addingTimeInterval(-1))}
  let periods=[("gecen hafta",Calendar.Component.weekOfYear,-1),("bu hafta",.weekOfYear,0),("gelecek hafta",.weekOfYear,1),("gecen ay",.month,-1),("bu ay",.month,0),("gelecek ay",.month,1),("gecen yil",.year,-1),("bu yil",.year,0)]
  for(phrase,unit,offset)in periods where q.contains(phrase){if let base=calendar.date(byAdding:unit,value:offset,to:now),let interval=calendar.dateInterval(of:unit,for:base){range(interval)};remaining=remaining.replacingOccurrences(of:phrase,with:"");break}
  for(phrase,offset)in [("bugun",0),("yarin",1),("dun",-1)] where remaining.split(whereSeparator:{!$0.isLetter}).contains(Substring(phrase)){let d=calendar.date(byAdding:.day,value:offset,to:now)!;from=PlanDates.key(d);to=from;remaining=remaining.replacingOccurrences(of:phrase,with:"")}
  let months=["ocak","subat","mart","nisan","mayis","haziran","temmuz","agustos","eylul","ekim","kasim","aralik"]
  if let year=q.split(whereSeparator:{!$0.isNumber}).compactMap({Int($0)}).first(where:{(1900...2200).contains($0)}){
   if let month=months.firstIndex(where:{q.contains($0)}),let first=try? PlanDates.instant(day:String(format:"%04d-%02d-01",year,month+1),time:"12:00"),let interval=calendar.dateInterval(of:.month,for:first){range(interval);remaining=remaining.replacingOccurrences(of:months[month],with:"").replacingOccurrences(of:String(year),with:"")}
  }
  if let regex=try? NSRegularExpression(pattern:"\\b\\d{4}-\\d{2}-\\d{2}\\b"),let match=regex.firstMatch(in:q,range:NSRange(q.startIndex...,in:q)),let r=Range(match.range,in:q){let day=String(q[r]);if (try? PlanDates.day(day)) != nil{from=day;to=day;remaining=remaining.replacingOccurrences(of:day,with:"")}}
  let done=["yaptiklarim","yaptim","yaptigim","tamamladim","tamamlanan","tamamlanmis","yapmisim"].contains{q.contains($0)},notDone=q.contains("yapmadim")||q.contains("yapilmadi"),partial=q.contains("kismen")||q.contains("yarim"),awaiting=q.contains("yanitsiz")||q.contains("yanit bekliyor"),postponed=q.contains("ertelen")||q.contains("erteledigim")
  let categories=[("spor","spor"),("okuma","kitap"),("kitap","kitap"),("is","is"),("calis","is"),("yemek","yemek"),("randevu","randevu"),("ogrenme","ogrenme")]
  let words=q.split(whereSeparator:{!$0.isLetter}).map(String.init)
  let category=categories.first{entry in words.contains(entry.0)||words.contains(where:{$0.hasPrefix(entry.0)&&entry.0.count>=4})}?.1
  let count=q.contains("kac")||q.contains("sayisi"),schedule=q.contains("hangi gun")&&category=="is"
  let localQuestion=count||schedule
  let command=["tasi","tasir","tasiyabilir","ertele","erteleyebilir","degistir","duzenle","ekle","sil","olustur","iptal"].contains{word in words.contains(word)}
  if command{return SearchResult(plans:[],answer:nil,needsAI:true)}
  let ignored=Set(["kac","kere","kez","tane","plan","plani","planlar","planlarim","yaptiklarim","yaptim","yaptigim","tamamladim","tamamlanan","tamamlanmis","yapmisim","yapmadim","yapilmadi","kismen","yarim","yanitsiz","yanit","bekliyor","ertelenen","ertelenmis","erteledigim","var","icin","ve","ne","kadar","hangi","gun","gunler","gunleri","calisiyorum","calisacagim","mi","miyim","sayisi"])
  let categoryWords=Set(categories.filter{$0.1==category}.map{$0.0})
  remaining=remaining.split(whereSeparator:{!$0.isLetter && !$0.isNumber}).map(String.init).filter{!ignored.contains($0) && !categoryWords.contains($0)}.joined(separator:" ")
  let rows=plans.filter{p in
   guard !p.deleted else{return false}
   let history=p.metadata["outcomeHistory"]?.items?.compactMap{row->String? in row.fields?["status"]?.string=="POSTPONED" ? row.fields?["date"]?.string:nil} ?? []
   let dates=postponed ? history+[p.date]:[p.date]
   if let from,let to,!dates.contains(where:{$0>=from&&$0<=to}){return false}
   if let category,p.category != category{return false}
   if done && !p.completed{return false};if notDone && !p.skipped{return false};if partial && p.outcome != "PARTIAL"{return false};if awaiting && !p.awaiting(now:now){return false};if postponed && p.outcome != "POSTPONED" && history.isEmpty{return false}
   let needle=remaining.trimmingCharacters(in:.whitespacesAndNewlines)
   return needle.isEmpty||normalize(p.title+" "+(p.metadata["note"]?.string ?? "")).contains(needle)
  }
  if localQuestion{
   guard from != nil||category != nil||done||notDone||partial||awaiting||postponed else{return SearchResult(plans:rows,answer:nil,needsAI:true)}
   let answer=schedule ? (rows.isEmpty ? "Bu dönemde kayıtlı çalışma planı yok.":rows.map{$0.date+" · "+$0.scheduleLabel}.joined(separator:"\n")):"Kayıtlarına göre \(rows.count) plan bulundu."+(done ? " Yalnız tamamladıkların sayıldı.":" Gelecekteki ve yanıtsız planlar yapılmadı sayılmaz.")
   if count && !done && !notDone && !partial && !awaiting && !postponed && !q.contains("kere") && !q.contains("kez"){
    let routines=Set(rows.compactMap(\.seriesUID)).count
    if routines>0{return SearchResult(plans:rows,answer:"Bu kapsamda \(rows.filter{$0.seriesUID==nil}.count) tek seferlik plan ve \(routines) rutin var. Rutinlerin \(rows.filter{$0.seriesUID != nil}.count) tekrarı bulunuyor.",needsAI:false)}
   }
   return SearchResult(plans:rows,answer:answer,needsAI:false)
  }
  return SearchResult(plans:rows,answer:nil,needsAI:false)
 }
}
