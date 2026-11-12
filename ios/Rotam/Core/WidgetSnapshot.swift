import Foundation
public struct WidgetPlan:Codable,Sendable,Identifiable {public let id:String;public let title:String;public let schedule:String;public let completed:Bool;public init(_ plan:PortablePlan){id=plan.id;title=plan.title;schedule=plan.scheduleLabel;completed=plan.completed}}
public struct WidgetSnapshot:Codable,Sendable {
 public var day:String
 public var completed:Int
 public var total:Int
 public var plans:[WidgetPlan]
 public init(day:String,completed:Int,total:Int,plans:[WidgetPlan]){self.day=day;self.completed=completed;self.total=total;self.plans=plans}
}
