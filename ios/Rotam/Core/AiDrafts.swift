import Foundation

public struct PlanProposal:Sendable {
 public let plan:WireRecord
 public let before:WireRecord?
 public init(plan:WireRecord,before:WireRecord?=nil){self.plan=plan;self.before=before}
}
public struct MemoryProposal:Sendable {public let text:String;public let before:WireRecord?}
public struct ParsedAiReply:Sendable {public let text:String;public let plans:[PlanProposal];public let memories:[MemoryProposal]}
public enum AiDraftParser {
 public static func parse(_ raw:String,existing:[WireRecord],memories:[WireRecord])throws->ParsedAiReply {
  var text=raw.trimmingCharacters(in:.whitespacesAndNewlines)
  if text.hasPrefix("```json"){text=String(text.dropFirst(7))}else if text.hasPrefix("```"){text=String(text.dropFirst(3))}
  if text.hasSuffix("```"){text=String(text.dropLast(3))}
  guard let root=try JSONValue.parse(text).fields else{throw RotamDataError.invalid("AI yanıtının biçimi doğrulanamadı. Hiçbir plan kaydedilmedi.")}
  let rows=root["plans"]?.items ?? [];guard rows.count<=20 else{throw RotamDataError.invalid("Tek yanıtta çok fazla plan var. Daha küçük bir bölüm iste.")}
  var proposals:[PlanProposal]=[],seen=Set<String>()
  for row in rows {
   guard let d=row.fields,let title=d["title"]?.string,let date=d["date"]?.string else{throw RotamDataError.invalid("AI planın başlığını veya gününü eksik verdi. Hiçbir plan kaydedilmedi.")}
   let before:WireRecord?
   if let uid=d["planId"]?.string{guard seen.insert(uid).inserted,let found=existing.first(where:{$0.kind == .plan&&$0.uid==uid}),try !PortablePlan(found).deleted else{throw RotamDataError.invalid("Değiştirilecek plan bulunamadı veya yinelenmiş. Hiçbir değişiklik yapılmadı.")};before=found}else{guard d["planId"]==nil||d["planId"] == .null else{throw RotamDataError.invalid("AI plan kimliği doğrulanamadı.")};before=nil}
   for key in ["startTime","endTime","endDate"] {
    if before != nil&&d[key]==nil{throw RotamDataError.invalid("AI mevcut planın saatlerini eksik verdi. Eski saatler korundu; yeniden iste.")}
    guard d[key]==nil||d[key] == .null||d[key]?.string != nil else{throw RotamDataError.invalid("AI saat bilgisi geçerli değil.")}
   }
   let base=try before ?? PortablePlan.make(title:title,date:date)
   var changes:[String:JSONValue]=["title":.string(title),"date":.string(date),"category":d["category"] ?? .string("kisisel")]
   for key in ["startTime","endTime","endDate"]{changes[key]=d[key] ?? .null}
   if before==nil{for key in ["repeatType","repeatDays","repeatEvery","repeatUntil"]{if let v=d[key]{changes[key]=v}}}
   let proposed=try base.changing(changes)
   var graph=existing.filter{$0.key != proposed.key};graph.append(proposed);try CoreValidator.validate(graph)
   proposals.append(PlanProposal(plan:proposed,before:before))
  }
  let memoryRows=root["remember"]?.items ?? [];guard memoryRows.count<=5 else{throw RotamDataError.invalid("Çok fazla hafıza önerisi var. Hiçbiri kaydedilmedi.")}
  let memory=try memoryRows.map{row->MemoryProposal in
   guard let d=row.fields,let value=d["text"]?.string?.trimmingCharacters(in:.whitespacesAndNewlines),!value.isEmpty,value.utf16.count<=500 else{throw RotamDataError.invalid("Hafıza önerisi doğrulanamadı.")}
   let before:WireRecord?
   if let uid=d["replaceUid"]?.string{guard let found=memories.first(where:{$0.uid==uid})else{throw RotamDataError.invalid("Değiştirilecek hafıza bulunamadı.")};before=found}else{before=nil}
   return MemoryProposal(text:value,before:before)
  }
  return ParsedAiReply(text:root["reply"]?.string ?? "Taslakları inceleyebilirsin.",plans:proposals,memories:memory)
 }
}
extension RecordStore {
 public func approve(_ proposals:[PlanProposal])throws {
  let original=all();var result:[WireRecord]=[]
  for p in proposals{
   if let before=p.before{guard original.first(where:{$0.key==before.key})==before else{throw RotamDataError.invalid("Önizlemeden sonra plan değişti. Güncel planla tekrar iste.")};result.append(try PlanActions.preserveSchedule(before,proposed:p.plan))}
   else{
    if original.contains(where:{$0.key==p.plan.key}){continue}
    let fields=try p.plan.fields()
    if fields["repeatType"]?.string != "none",fields["repeatType"] != nil{
     result += try RecurrenceRules.make(p.plan,through:max(PlanDates.addDays(PlanDates.key(Date()),400),PlanDates.addDays(fields["date"]?.string ?? "",60)))
    }else{result.append(p.plan)}
   }
  }
  try replaceMany(result,expected:original)
 }
}
