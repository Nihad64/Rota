import Foundation
public enum RoutineRules {
 public static func suspendedOn(_ template:[String:JSONValue],day:String)throws->Bool {
  let metadata=try JSONValue.parse(template["extraData"]?.string ?? "{}").fields ?? [:]
  for row in metadata["routinePauseHistory"]?.items ?? []{
   guard let from=row.fields?["from"]?.string else{throw RotamDataError.invalid("Rutin ara verme geçmişi doğrulanamadı.")}
   _ = try PlanDates.day(from)
   if let to=row.fields?["to"]?.string{_ = try PlanDates.day(to);if day>=from&&day<to{return true}}else if day>=from{return true}
  }
  return false
 }
}
extension RecordStore {
 @discardableResult public func ensure(from:String,to:String)throws->Bool {
  let original=all(),known=Set(original.map(\.key));var added:[WireRecord]=[]
  for series in original where series.kind == .series{added += try RecurrenceRules.instances(series:series,from:from,to:to).filter{!known.contains($0.key)}}
  if added.isEmpty{return false};try replaceMany(added,expected:original);return true
 }
 public func pauseRoutine(_ uid:String,paused:Bool,day:String=PlanDates.key(Date()))throws {
  let original=all();guard let series=original.first(where:{$0.kind == .series&&$0.uid==uid}),var template=try series.fields()["template"]?.fields else{throw RotamDataError.invalid("Rutin bulunamadı.")}
  var metadata=try JSONValue.parse(template["extraData"]?.string ?? "{}").fields ?? [:]
  if metadata["routinePaused"]?.boolean==paused{return}
  var history=metadata["routinePauseHistory"]?.items ?? []
  if paused{history.append(.object(["from":.string(day)]))}else if var last=history.last?.fields,last["to"]==nil{last["to"] = .string(day);history[history.count-1] = .object(last)}
  metadata["routinePaused"] = .bool(paused);metadata["routinePauseHistory"] = .array(history);template["extraData"] = .string(try JSONValue.object(metadata).canonical())
  let changed=try series.changing(["template":.object(template)]);var updates=[changed]
  for r in original where r.kind == .plan{
   let p=try PortablePlan(r);guard p.seriesUID==uid,p.date>=day else{continue}
   if paused && !p.deleted && !p.completed && !p.skipped && p.outcome != "PARTIAL"{updates.append(try p.changingMetadata(["pausedByRoutine":.bool(true)]).changing(["isDeleted":.bool(true)]))}
   else if !paused && p.deleted && p.metadata["pausedByRoutine"]?.boolean==true{var e=p.metadata;e.removeValue(forKey:"pausedByRoutine");updates.append(try r.changing(["isDeleted":.bool(false),"extraData":.string(JSONValue.object(e).canonical())]))}
  }
  if !paused{
   let known=Set((original+updates).map(\.key));updates += try RecurrenceRules.instances(series:changed,from:day,to:PlanDates.addDays(day,400)).filter{!known.contains($0.key)}
  }
  try replaceMany(updates,expected:original)
 }
 public func editFuture(_ proposed:WireRecord,before:WireRecord)throws {
  let original=all();guard original.first(where:{$0.key==before.key})==before else{throw RotamDataError.invalid("Plan değişti. Güncel sürümü incele.")}
  let old=try PortablePlan(before),proposal=try PortablePlan(proposed)
  guard let seriesUID=old.seriesUID,let series=original.first(where:{$0.kind == .series&&$0.uid==seriesUID})else{try updatePlan(proposed,before:before);return}
  let from=old.fields["originalDate"]?.string ?? old.date,shift=Int(try PlanDates.day(proposal.date).timeIntervalSince(PlanDates.day(old.date))/86400),anchor=try PlanDates.addDays(from,shift)
  var updates=[try series.changing(["untilDate":.string(PlanDates.addDays(from,-1))])]
  let children=try original.filter{$0.kind == .plan}.map(PortablePlan.init).filter{$0.seriesUID==seriesUID&&($0.fields["originalDate"]?.string ?? $0.date)>=from}
  for p in children where !p.completed && !p.skipped && p.outcome != "PARTIAL"{updates.append(try p.record.changing(["isDeleted":.bool(true)]))}
  var fields=try proposed.fields();fields["uid"] = .string(UUID().uuidString.lowercased());fields.removeValue(forKey:"seriesUid");fields.removeValue(forKey:"originalDate");fields["date"] = .string(anchor);fields["focusSeconds"] = .integer(0);fields["isCompleted"] = .bool(false);fields["isSkipped"] = .bool(false);fields["isDeleted"] = .bool(false);fields["extraData"] = .string(try RecurrenceRules.freshMetadata(fields["extraData"]?.string ?? "{}"))
  if let finish=proposal.fields["endDate"]?.string{fields["endDate"] = .string(try PlanDates.addDays(finish,Int(try PlanDates.day(anchor).timeIntervalSince(PlanDates.day(proposal.date))/86400)))}
  let fresh=try WireRecord(kind:.plan,fields:fields)
  if fields["repeatType"]?.string=="none"{updates.append(fresh)}else{
   let batch=try RecurrenceRules.make(fresh,through:max(PlanDates.addDays(PlanDates.key(Date()),400),PlanDates.addDays(anchor,60))),newSeries=batch.first{$0.kind == .series}!
   let protected=Set(children.filter{$0.completed||$0.skipped||$0.deleted||$0.outcome=="PARTIAL"}.compactMap{p in try? PlanDates.addDays(p.fields["originalDate"]?.string ?? p.date,shift)})
   for r in batch{if r.kind == .plan,let day=try r.fields()["date"]?.string,protected.contains(day){updates.append(try r.changing(["isDeleted":.bool(true)]))}else{updates.append(r)}}
   _ = newSeries
  }
  try replaceMany(updates,expected:original)
 }
}

public struct RoutineDeletionReceipt:Sendable {
 public let before:[WireRecord]
 public let after:[WireRecord]
}
extension RecordStore {
 public func deleteRoutine(_ uid:String,day:String=PlanDates.key(Date()))throws->RoutineDeletionReceipt {
  let original=all();guard let rule=original.first(where:{$0.kind == .series&&$0.uid==uid}),var fields=try rule.fields()["template"]?.fields else{throw RotamDataError.invalid("Rutin bulunamadı.")}
  var e=try JSONValue.parse(fields["extraData"]?.string ?? "{}").fields ?? [:]
  guard e["routineDeleted"]?.boolean != true else{return RoutineDeletionReceipt(before:[],after:[])}
  e["routineDeleted"] = .bool(true);e["routineDeletedAt"] = .string(day);fields["extraData"] = .string(try JSONValue.object(e).canonical())
  let yesterday=try PlanDates.addDays(day,-1)
  let until=min((try rule.fields())["untilDate"]?.string ?? yesterday,yesterday)
  var before=[rule],after=[try rule.changing(["template":.object(fields),"untilDate":.string(until)])]
  for r in original where r.kind == .plan{let p=try PortablePlan(r);if p.seriesUID==uid && !p.deleted && !p.completed && !p.skipped && p.outcome != "PARTIAL"{var metadata=p.metadata;metadata.removeValue(forKey:"pausedByRoutine");before.append(r);after.append(try r.changing(["isDeleted":.bool(true),"extraData":.string(JSONValue.object(metadata).canonical())]))}}
  try replaceMany(after,expected:original);return RoutineDeletionReceipt(before:before,after:after)
 }
 public func undoRoutineDeletion(_ receipt:RoutineDeletionReceipt)throws {
  let original=all(),map=try SyncEnvelope(records:original).index()
  guard receipt.after.allSatisfy({map[$0.key]==$0})else{throw RotamDataError.invalid("Rutin başka bir işlemde değişti. Güncel kayıtlar korunarak geri alma durdu.")}
  try replaceMany(receipt.before,expected:original)
 }
}
