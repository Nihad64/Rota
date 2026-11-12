import Foundation

public enum RecurrenceRules {
 private static func freshMetadata(_ text:String)throws->String{
  guard var e=try JSONValue.parse(text).fields else{throw RotamDataError.invalid("Rutin bilgisi doğrulanamadı.")}
  let removed=["outcome","outcomeHistory","completedAmount","targetAmount","focusSegments","startedSchedule"]
  if removed.allSatisfy({e[$0]==nil}){return text}
  for key in removed{e.removeValue(forKey:key)};return try JSONValue.object(e).canonical()
 }
 public static func instances(series:WireRecord,from:String,to:String)throws->[WireRecord]{
  let s=try series.fields();guard series.kind == .series,let anchor=s["anchor"]?.string,let template=s["template"]?.fields else{throw RotamDataError.invalid("Rutin kuralı bulunamadı.")}
  let metadata=try freshMetadata(template["extraData"]?.string ?? "{}")
  if (try? JSONValue.parse(metadata).fields?["routinePaused"]?.boolean)==true{return []}
  let first=try max(PlanDates.day(from),PlanDates.day(anchor));let last=min(try PlanDates.day(to),s["untilDate"]?.string.flatMap{try? PlanDates.day($0)} ?? .distantFuture)
  if first>last{return []};guard last.timeIntervalSince(first)<=3660*86400 else{throw RotamDataError.invalid("Tek seferde çok uzun bir rutin dönemi istendi. Başlangıç veya bitiş gününü daralt.")}
  var calendar=Calendar(identifier:.gregorian);calendar.timeZone=TimeZone(secondsFromGMT:0)!
  let anchorDate=try PlanDates.day(anchor),anchorDay=calendar.component(.day,from:anchorDate)
  let repeatType=template["repeatType"]?.string ?? "none";let every=Int(template["repeatEvery"]?.integer ?? 1)
  guard (1...365).contains(every)else{throw RotamDataError.invalid("Tekrar aralığı geçerli değil.")}
  let chosen=Set((template["repeatDays"]?.string ?? "").split(separator:",").compactMap{Int($0)})
  let endOffset=try template["endDate"]?.string.map{Int(try PlanDates.day($0).timeIntervalSince(anchorDate)/86400)}
  var output:[WireRecord]=[],date=first
  while date<=last{
   let days=Int(date.timeIntervalSince(anchorDate)/86400),weekday=(calendar.component(.weekday,from:date)+5)%7+1
   let matches:Bool
   switch repeatType{
   case "daily":matches=true
   case "weekdays":matches=weekday<=5
   case "weekly":matches=calendar.component(.weekday,from:date)==calendar.component(.weekday,from:anchorDate)&&(days/7)%every==0
   case "custom":matches=chosen.contains(weekday)
   case "interval":matches=days%every==0
   case "monthly":let months=calendar.dateComponents([.month],from:calendar.dateInterval(of:.month,for:anchorDate)!.start,to:calendar.dateInterval(of:.month,for:date)!.start).month!;let target=template["monthLastDay"]?.boolean==true ? min(anchorDay,calendar.range(of:.day,in:.month,for:date)!.count):anchorDay;matches=months%every==0&&calendar.component(.day,from:date)==target
   default:matches=date==anchorDate
   }
   if matches{
    let day=PlanDates.key(date,local:false);var d=template
    d["uid"] = .string("series:\(series.uid):\(day)");d["seriesUid"] = .string(series.uid);d["originalDate"] = .string(day);d["date"] = .string(day)
    d["isCompleted"] = .bool(false);d["isSkipped"] = .bool(false);d["isDeleted"] = .bool(false);d["focusSeconds"] = .integer(0);d["extraData"] = .string(metadata)
    if let endOffset{d["endDate"] = .string(try PlanDates.addDays(day,endOffset))}
    if let until=s["untilDate"]{d["repeatUntil"]=until}else{d.removeValue(forKey:"repeatUntil")}
    output.append(try WireRecord(kind:.plan,fields:d))
   }
   date=calendar.date(byAdding:.day,value:1,to:date)!
  }
  return output
 }
 public static func make(_ plan:WireRecord,through:String)throws->[WireRecord]{
  var template=try plan.fields()
  template.removeValue(forKey:"seriesUid");template.removeValue(forKey:"originalDate")
  template["extraData"] = .string(try freshMetadata(template["extraData"]?.string ?? "{}"));template["focusSeconds"] = .integer(0)
  let uid=UUID().uuidString.lowercased(),anchor=template["date"]?.string ?? ""
  var s:[String:JSONValue]=["uid":.string(uid),"anchor":.string(anchor),"template":.object(template)]
  if let until=template["repeatUntil"]{s["untilDate"]=until}
  let series=try WireRecord(kind:.series,fields:s)
  return [series]+(try instances(series:series,from:anchor,to:through))
 }
}
