import Foundation
public enum SearchPresentationRow:Identifiable,Sendable {
 case plan(PortablePlan)
 case routine(uid:String,title:String,occurrences:[PortablePlan],representative:PortablePlan)
 public var id:String{switch self{case .plan(let p):return "plan:"+p.id;case .routine(let uid,_,_,_):return "routine:"+uid}}
}
public enum SearchPresentation {
 public static func rows(_ plans:[PortablePlan],records:[WireRecord],query:String,today:String=PlanDates.key(Date()))->[SearchPresentationRow]{
  let rules=Dictionary(uniqueKeysWithValues:records.filter{$0.kind == .series}.map{($0.uid,$0)})
  var added=Set<String>(),rows:[SearchPresentationRow]=[]
  for p in plans{
   guard let uid=p.seriesUID else{rows.append(.plan(p));continue}
   guard added.insert(uid).inserted else{continue}
   let rule=rules[uid],fields=(try? rule?.fields()) ?? [:],template=fields["template"]?.fields ?? [:],metadata=(try? JSONValue.parse(template["extraData"]?.string ?? "{}").fields) ?? [:]
   if query.isEmpty && (metadata["routineDeleted"]?.boolean==true||(fields["untilDate"]?.string ?? "9999-12-31")<today){continue}
   let children=plans.filter{$0.seriesUID==uid}.sorted{($0.date,$0.start ?? $0.end ?? "99:99")<($1.date,$1.start ?? $1.end ?? "99:99")}
   let representative=children.first{$0.date>=today && !$0.completed && !$0.skipped} ?? children.last!
   rows.append(.routine(uid:uid,title:template["title"]?.string ?? p.title,occurrences:children,representative:representative))
  }
  return rows
 }
 public static func label(_ rows:[SearchPresentationRow])->String{
  let plans=rows.filter{if case .plan = $0{return true};return false}.count,routines=rows.count-plans
  return routines==0 ? "\(plans) plan":plans==0 ? "\(routines) rutin":"\(plans) plan · \(routines) rutin"
 }
}
