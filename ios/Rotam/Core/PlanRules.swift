import Foundation

public enum PlanDates {
 public static func day(_ text:String)throws->Date{
  let f=DateFormatter();f.calendar=Calendar(identifier:.gregorian);f.locale=Locale(identifier:"en_US_POSIX");f.timeZone=TimeZone(secondsFromGMT:0);f.dateFormat="yyyy-MM-dd";f.isLenient=false
  guard text.count==10,let date=f.date(from:text),f.string(from:date)==text else{throw RotamDataError.invalid("Plan tarihi geçerli değil.")};return date
 }
 public static func key(_ date:Date,local:Bool=true)->String{let f=DateFormatter();f.locale=Locale(identifier:"en_US_POSIX");f.calendar=Calendar(identifier:.gregorian);f.timeZone=local ? .current:TimeZone(secondsFromGMT:0);f.dateFormat="yyyy-MM-dd";return f.string(from:date)}
 public static func addDays(_ key:String,_ days:Int)throws->String{var c=Calendar(identifier:.gregorian);c.timeZone=TimeZone(secondsFromGMT:0)!;return self.key(c.date(byAdding:.day,value:days,to:try day(key))!,local:false)}
 public static func time(_ value:String)throws->Double{
  let parts=value.split(separator:":",omittingEmptySubsequences:false)
  guard (2...3).contains(parts.count),parts[0].count==2,parts[1].count==2,let h=Int(parts[0]),let m=Int(parts[1]),(0...23).contains(h),(0...59).contains(m)else{throw RotamDataError.invalid("Saat biçimi geçerli değil.")}
  let second=parts.count==3 ? Double(parts[2]):0
  guard let second,(0..<60).contains(second)else{throw RotamDataError.invalid("Saat biçimi geçerli değil.")};return Double(h*3600+m*60)+second
 }
 public static func instant(day:String,time:String)throws->Date{
  _ = try self.day(day);let seconds=try self.time(time);let bits=day.split(separator:"-").map{Int($0)!}
  var c=Calendar(identifier:.gregorian);c.timeZone = .current
  guard let date=c.date(from:DateComponents(year:bits[0],month:bits[1],day:bits[2],hour:Int(seconds)/3600,minute:Int(seconds)%3600/60,second:Int(seconds)%60))else{throw RotamDataError.invalid("Tarih ve saat birleştirilemedi.")};return date
 }
}
public struct PortablePlan:Sendable,Identifiable {
 public let record:WireRecord;public var id:String{record.uid};public let fields:[String:JSONValue]
 public init(_ record:WireRecord)throws{guard record.kind == .plan else{throw RotamDataError.invalid("Plan türü bekleniyordu.")};self.record=record;fields=try record.fields()}
 public var title:String{fields["title"]?.string ?? ""};public var date:String{fields["date"]?.string ?? ""}
 public var start:String?{fields["startTime"]?.string};public var end:String?{fields["endTime"]?.string};public var endDate:String{fields["endDate"]?.string ?? date}
 public var completed:Bool{fields["isCompleted"]?.boolean ?? false};public var skipped:Bool{fields["isSkipped"]?.boolean ?? false};public var deleted:Bool{fields["isDeleted"]?.boolean ?? false}
 public var category:String{fields["category"]?.string ?? "kisisel"};public var seriesUID:String?{fields["seriesUid"]?.string}
 public var focusSeconds:Int64{fields["focusSeconds"]?.integer ?? 0}
 public var metadata:[String:JSONValue]{(try? JSONValue.parse(fields["extraData"]?.string ?? "{}").fields) ?? [:]}
 public var outcome:String{completed ? "COMPLETED":skipped ? "NOT_DONE":metadata["outcome"]?.string ?? "UNKNOWN"}
 public var scheduleLabel:String{if let start,let end{return "\(start) – \(end)"};if let start{return "Başlangıç · \(start)"};if let end{return "Son teslim · \(end)"};return "Saat yok"}
 public func awaiting(now:Date=Date())->Bool{guard !deleted,!completed,!skipped,outcome != "PARTIAL"else{return false};if let end,let instant=try? PlanDates.instant(day:endDate,time:end){return instant<now};return date<PlanDates.key(now)}
 public func changingMetadata(_ changes:[String:JSONValue])throws->WireRecord{var e=metadata;for(k,v)in changes{e[k]=v};return try record.changing(["extraData":.string(JSONValue.object(e).canonical())])}
 public static func make(title:String,date:String,start:String?=nil,end:String?=nil,endDate:String?=nil,category:String="kisisel",uid:String=UUID().uuidString.lowercased(),createdAt:Int64=Int64(Date().timeIntervalSince1970*1000))throws->WireRecord{
  let d:[String:JSONValue]=["uid":.string(uid),"title":.string(title),"date":.string(date),"category":.string(category),"startTime":start.map(JSONValue.string) ?? .null,"endTime":end.map(JSONValue.string) ?? .null,"endDate":endDate.map(JSONValue.string) ?? .null,"isCompleted":.bool(false),"isSkipped":.bool(false),"isDeleted":.bool(false),"extraData":.string("{\"reminderMode\":\"auto\"}"),"createdAt":.integer(createdAt),"focusSeconds":.integer(0),"repeatType":.string("none"),"repeatEvery":.integer(1),"repeatDays":.string(""),"monthLastDay":.bool(false),"reminderEnabled":.bool(false),"reminderMinutes":.integer(15),"remindAtEnd":.bool(false)]
  let r=try WireRecord(kind:.plan,fields:d);try CoreValidator.validate([r]);return r
 }
}
public enum CoreValidator {
 public static func validate(_ records:[WireRecord],owner:String?=nil)throws {
  let index=try SyncEnvelope(records:records).index()
  for r in records{
   let d=try r.fields()
   guard d["uid"]?.string==r.uid,!r.uid.isEmpty,r.uid.utf16.count<=512,r.body.utf8.count<=256*1024 else{throw RotamDataError.invalid("Kayıt kimliği veya boyutu uyuşmuyor.")}
   switch r.kind{
   case .plan:
    guard let title=d["title"]?.string,!title.trimmingCharacters(in:.whitespacesAndNewlines).isEmpty,title.utf16.count<=300,let day=d["date"]?.string else{throw RotamDataError.invalid("Plan başlığı veya tarihi eksik.")}
    _ = try PlanDates.day(day)
    let start=d["startTime"]?.string,end=d["endTime"]?.string,finish=d["endDate"]?.string
    if let start{_ = try PlanDates.time(start)};if let end{_ = try PlanDates.time(end)}
    if let finish{guard end != nil,try PlanDates.day(finish)>=PlanDates.day(day)else{throw RotamDataError.invalid("Bitiş günü geçerli değil.")}}
    if let start,let end{guard try PlanDates.day(finish ?? day).timeIntervalSince(PlanDates.day(day))+PlanDates.time(end)>PlanDates.time(start)else{throw RotamDataError.invalid("Bitiş başlangıçtan sonra olmalı.")}}
    if let reminder=d["reminderTime"]?.string{_ = try PlanDates.time(reminder)}
    guard (0...10080).contains(d["reminderMinutes"]?.integer ?? 15),(1...365).contains(d["repeatEvery"]?.integer ?? 1),(d["focusSeconds"]?.integer ?? 0)>=0 else{throw RotamDataError.invalid("Plan süresi veya tekrar kuralı geçerli değil.")}
    guard ["none","daily","weekdays","weekly","custom","interval","monthly"].contains(d["repeatType"]?.string ?? "none")else{throw RotamDataError.invalid("Tekrar türü desteklenmiyor.")}
    if let until=d["repeatUntil"]?.string{_ = try PlanDates.day(until)}
    guard let extra=try JSONValue.parse(d["extraData"]?.string ?? "{}").fields else{throw RotamDataError.invalid("Plan notları doğrulanamadı.")};_ = extra
    if let sid=d["seriesUid"]?.string{
     guard index[WireKey(.series,sid)] != nil,let original=d["originalDate"]?.string,r.uid=="series:\(sid):\(original)"else{throw RotamDataError.invalid("Rutin bağlantısı veya örnek kimliği eksik.")};_ = try PlanDates.day(original)
    }
    if let gid=d["goalUid"]?.string{guard index[WireKey(.goal,gid)] != nil else{throw RotamDataError.invalid("Hedef bağlantısı eksik.")}}
   case .series:
    guard let anchor=d["anchor"]?.string,let t=d["template"]?.fields,t["date"]?.string==anchor,let title=t["title"]?.string,!title.isEmpty else{throw RotamDataError.invalid("Rutin kuralı doğrulanamadı.")};_ = try PlanDates.day(anchor)
    if let until=d["untilDate"]?.string{_ = try PlanDates.day(until)}
   case .goal:
    guard let title=d["title"]?.string,!title.isEmpty,let due=d["dueDate"]?.string,(d["targetCount"]?.integer ?? 0)>0,(d["currentCount"]?.integer ?? 0)>=0 else{throw RotamDataError.invalid("Hedef kaydı doğrulanamadı.")};_ = try PlanDates.day(due)
   case .memory:
    let deleted=d["isDeleted"]?.boolean ?? false
    guard let uid=d["ownerUid"]?.string,!uid.isEmpty,uid.utf16.count<=128,owner==nil || owner==uid,let text=d["text"]?.string,(deleted && text.isEmpty) || (!deleted && !text.trimmingCharacters(in:.whitespacesAndNewlines).isEmpty && text.utf16.count<=500),(d["createdAt"]?.integer ?? -1)>=0,(d["updatedAt"]?.integer ?? -1)>=0 else{throw RotamDataError.invalid("Hafıza biçimi veya hesabı uyuşmuyor.")}
   case .preference:throw RotamDataError.invalid("Tercihler plan verisine karıştırılamaz.")
   }
  }
 }
 public static func merge(base:[WireKey:WireRecord],local:[WireKey:WireRecord],remote:[WireKey:WireRecord],owner:String?=nil)throws->WireMerge{
  let merged=ThreeWayMerge.merge(base:base,local:local,remote:remote)
  do{try validate(Array(merged.choosingLocal(true).values),owner:owner);try validate(Array(merged.choosingLocal(false).values),owner:owner);return merged}
  catch{
   let changed=Set(local.keys).union(remote.keys).filter{local[$0] != remote[$0]}
   guard !changed.isEmpty else{throw error}
   return WireMerge(records:local,conflicts:changed.map{WireConflict(key:$0,base:base[$0],local:local[$0],remote:remote[$0])})
  }
 }
}
