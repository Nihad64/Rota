import Foundation
import CryptoKit

public enum PlanActions {
 public static func ownerHash(_ uid:String)->String{SHA256.hash(data:Data(uid.utf8)).map{String(format:"%02x",$0)}.joined()}
 private static func localTimestamp(_ date:Date)->String{let f=DateFormatter();f.locale=Locale(identifier:"en_US_POSIX");f.dateFormat="yyyy-MM-dd'T'HH:mm:ss";return f.string(from:date)}
 public static func outcome(_ record:WireRecord,status:String,amount:Double?=nil,target:Double?=nil,now:Date=Date())throws->WireRecord{
  guard ["UNKNOWN","COMPLETED","PARTIAL","NOT_DONE","POSTPONED"].contains(status)else{throw RotamDataError.invalid("Plan durumu desteklenmiyor.")}
  let p=try PortablePlan(record);var e=p.metadata
  if status=="PARTIAL"{guard let amount,let target,amount.isFinite,target.isFinite,amount>0,target>amount else{throw RotamDataError.invalid("Kısmi miktar sıfırdan büyük ve hedeften küçük olmalı.")};e["completedAmount"] = .number(amount);e["targetAmount"] = .number(target)}else{e.removeValue(forKey:"completedAmount");e.removeValue(forKey:"targetAmount")}
  var history=e["outcomeHistory"]?.items ?? []
  var event:[String:JSONValue]=["status":.string(status),"at":.string(localTimestamp(now)),"date":.string(p.date),"startTime":p.start.map(JSONValue.string) ?? .null,"endTime":p.end.map(JSONValue.string) ?? .null,"endDate":p.fields["endDate"] ?? .null]
  if let amount,let target,status=="PARTIAL"{event["amount"] = .number(amount);event["target"] = .number(target)}
  history.append(.object(event));e["outcomeHistory"] = .array(history);e["outcome"] = .string(status)
  return try record.changing(["extraData":.string(JSONValue.object(e).canonical()),"isCompleted":.bool(status=="COMPLETED"),"isSkipped":.bool(status=="NOT_DONE")])
 }
 public static func preserveSchedule(_ old:WireRecord,proposed:WireRecord,now:Date=Date())throws->WireRecord {
  let old=try PortablePlan(old),new=try PortablePlan(proposed)
  guard old.date != new.date || old.start != new.start || old.end != new.end || old.endDate != new.endDate else{return proposed}
  let prior=old.metadata["outcomeHistory"]?.items ?? [];var e=new.metadata,history=e["outcomeHistory"]?.items ?? prior
  if history.count>prior.count,let last=history.last?.fields,last["status"]?.string=="POSTPONED",last["date"]?.string==old.date{return proposed}
  history.append(.object(["status":.string("POSTPONED"),"at":.string(localTimestamp(now)),"date":.string(old.date),"startTime":old.start.map(JSONValue.string) ?? .null,"endTime":old.end.map(JSONValue.string) ?? .null,"endDate":old.fields["endDate"] ?? .null]))
  e["outcomeHistory"] = .array(history);return try proposed.changing(["extraData":.string(JSONValue.object(e).canonical())])
 }
 public static func postpone(_ record:WireRecord,date:String,time:String?,now:Date=Date())throws->WireRecord{
  let p=try PortablePlan(record);let moved=try outcome(record,status:"POSTPONED",now:now)
  let oldDay=try PlanDates.day(p.date),newDay=try PlanDates.day(date)
  let shift=Int(newDay.timeIntervalSince(oldDay)/86400)
  var changes:[String:JSONValue]=["date":.string(date)]
  if let oldStart=p.start,let oldEnd=p.end,let time{
   let duration=try PlanDates.day(p.endDate).timeIntervalSince(oldDay)+PlanDates.time(oldEnd)-PlanDates.time(oldStart)
   let newEnd=try PlanDates.time(time)+duration
   let dayOffset=Int(floor(newEnd/86400));let seconds=Int(newEnd.truncatingRemainder(dividingBy:86400))
   changes["startTime"] = .string(time);changes["endTime"] = .string(String(format:"%02d:%02d",seconds/3600,seconds%3600/60));changes["endDate"] = dayOffset==0 ? .null:.string(try PlanDates.addDays(date,dayOffset))
  }else if p.start != nil{changes["startTime"]=time.map(JSONValue.string) ?? .null;if p.fields["endDate"]?.string != nil{changes["endDate"] = .string(try PlanDates.addDays(p.endDate,shift))}}
  else if p.end != nil{changes["endTime"]=time.map(JSONValue.string) ?? .null;changes["endDate"] = .null}
  // Untimed remains untimed; explicit plan-time edits belong to the regular editor.
  return try moved.changing(changes)
 }
 public static func reminderSignature(_ p:PortablePlan)->String{
  let mode:String
  if p.metadata["reminderMode"]?.string=="auto"{mode="AUTO"}else if p.metadata["reminderMode"]?.string=="off"{mode="OFF"}else{mode=p.fields["reminderEnabled"]?.boolean==true ? "CUSTOM":"OFF"}
  let fields=[p.id,p.date,p.start ?? "null",p.end ?? "null",p.fields["endDate"]?.string ?? "null",mode,String(p.fields["reminderMinutes"]?.integer ?? 15),p.fields["reminderTime"]?.string ?? "null",p.fields["remindAtEnd"]?.boolean==true ? "true":"false"]
  return SHA256.hash(data:Data(fields.joined(separator:"|").utf8)).map{String(format:"%02x",$0)}.joined()
 }
 public static func started(_ p:PortablePlan)->Bool{p.metadata["startedSchedule"]?.string==reminderSignature(p)}
 public static func markStarted(_ p:PortablePlan)throws->WireRecord{try p.changingMetadata(["startedSchedule":.string(reminderSignature(p))])}
 public static func focus(_ record:WireRecord,uid:String,start:Int64,seconds:Int64)throws->WireRecord{
  guard seconds>=0,start>=0 else{throw RotamDataError.invalid("Odak süresi geçerli değil.")}
  let p=try PortablePlan(record);var e=p.metadata;var segments=e["focusSegments"]?.items ?? []
  let oldIndex=segments.firstIndex{$0.fields?["uid"]?.string==uid}
  let oldSeconds=oldIndex.flatMap{segments[$0].fields?["seconds"]?.integer} ?? 0
  let duration=max(oldSeconds,seconds);let firstStart=oldIndex.flatMap{segments[$0].fields?["startedAt"]?.integer} ?? start
  let segment:JSONValue = .object(["uid":.string(uid),"startedAt":.integer(firstStart),"seconds":.integer(duration)])
  if let oldIndex{segments[oldIndex]=segment}else{segments.append(segment)}
  e["focusSegments"] = .array(segments)
  let (total,overflow)=p.focusSeconds.addingReportingOverflow(duration-oldSeconds);guard !overflow else{throw RotamDataError.invalid("Odak süresi sınırı aşıldı.")}
  return try record.changing(["extraData":.string(JSONValue.object(e).canonical()),"focusSeconds":.integer(total)])
 }
 public static func subtask(_ record:WireRecord,task:String,done:Bool)throws->WireRecord{
  let p=try PortablePlan(record);var checked=Set((p.metadata["checkedSubtasks"]?.string ?? "").split(separator:"\n").map(String.init));if done{checked.insert(task)}else{checked.remove(task)}
  return try p.changingMetadata(["checkedSubtasks":.string(checked.sorted().joined(separator:"\n"))])
 }
}
