import Foundation
public struct ReminderEvent:Sendable,Identifiable {
 public var id:String{planID+"|"+signature+"|"+key}
 public let planID:String;public let signature:String;public let key:String;public var at:Date;public let custom:Bool
}
public enum ReminderRules {
 public static func events(_ plans:[PortablePlan],settings:[String:JSONValue],now:Date=Date())throws->[ReminderEvent] {
  try PortableSettings.validate(settings);guard settings["reminders"]?.boolean==true else{return []}
  var calendar=Calendar(identifier:.gregorian);calendar.timeZone = .current
  func quiet(_ date:Date)->Date{
   guard settings["quiet"]?.boolean==true else{return date}
   let start=Int(settings["quiet_start_minutes"]!.integer!),end=Int(settings["quiet_end_minutes"]!.integer!),parts=calendar.dateComponents([.hour,.minute],from:date),minute=parts.hour!*60+parts.minute!
   guard start != end,(start<end ? minute>=start&&minute<end:minute>=start||minute<end)else{return date}
   let next=start>end&&minute>=start ? calendar.date(byAdding:.day,value:1,to:date)!:date
   return calendar.date(bySettingHour:end/60,minute:end%60,second:0,of:next)!
  }
  var events:[ReminderEvent]=[]
  for p in plans where !p.deleted && !p.completed && !p.skipped && !PlanActions.started(p) {
   let mode=p.metadata["reminderMode"]?.string ?? (p.fields["reminderEnabled"]?.boolean==true ? "custom":"off")
   if mode=="off"{continue}
   let signature=PlanActions.reminderSignature(p)
   let start=try p.start.map{try PlanDates.instant(day:p.date,time:$0)},end=try p.end.map{try PlanDates.instant(day:p.endDate,time:$0)}
   if mode=="custom"{
    let base:Date?
    if let t=p.fields["reminderTime"]?.string{base=try PlanDates.instant(day:p.date,time:t)}
    else if p.fields["remindAtEnd"]?.boolean==true{base=end}else{base=start}
    if let base{let at=quiet(p.fields["reminderTime"]?.string != nil ? base:base.addingTimeInterval(-Double(p.fields["reminderMinutes"]?.integer ?? 15)*60));if at>now{events.append(ReminderEvent(planID:p.id,signature:signature,key:"custom",at:at,custom:true))}}
    continue
   }
   var raw:[(String,Date,Bool)]=[]
   let finish=try end ?? start ?? PlanDates.instant(day:p.date,time:"19:00")
   if let start{let lead=p.category=="randevu" ? 30:p.category=="is" ? 20:p.category=="spor" ? 15:10;raw += [("before",start.addingTimeInterval(Double(-lead*60)),false),("time",start,false),("after",(end ?? start.addingTimeInterval(1800)).addingTimeInterval(1800),true)]}
   else if let end{raw += [("before",end.addingTimeInterval(-1800),false),("time",end,false),("after",end.addingTimeInterval(7200),true)]}
   else{raw.append(("checkin",finish,true))}
   let nextDay=calendar.date(byAdding:.day,value:1,to:finish)!
   raw.append(("last",calendar.date(bySettingHour:18,minute:0,second:0,of:nextDay)!,true))
   var seen=Set<Date>()
   for(key,date,followup)in raw{
    var at=quiet(date)
    if followup{for _ in 0..<min(plans.count,100){
     let busy=plans.filter{$0.id != p.id && !$0.deleted && !$0.skipped}.compactMap{q->Date? in guard let a=q.start,let b=q.end,let from=try? PlanDates.instant(day:q.date,time:a),let to=try? PlanDates.instant(day:q.endDate,time:b),at>=from,at<to else{return nil};return to}.max()
     guard let busy else{break};at=quiet(busy.addingTimeInterval(900))
    }}
    if at>now,at<=calendar.date(byAdding:.day,value:2,to:finish)!,seen.insert(at).inserted{events.append(ReminderEvent(planID:p.id,signature:signature,key:key,at:at,custom:false))}
   }
  }
  var counts:[String:Int]=[:],last:Date?,accepted:[ReminderEvent]=[]
  for event in events.sorted(by:{$0.at<$1.at}){
   if !event.custom{let day=PlanDates.key(event.at);if counts[day,default:0]>=6{continue};if let last,event.at.timeIntervalSince(last)<900{continue};counts[day,default:0]+=1;last=event.at}
   accepted.append(event)
  }
  return Array(accepted.prefix(60))
 }
}
