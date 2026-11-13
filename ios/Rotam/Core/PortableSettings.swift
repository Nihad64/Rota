import Foundation

public enum PortableSettings {
 public static let defaults:[String:JSONValue]=["reminders":.bool(true),"quiet":.bool(true),"ui_sounds":.bool(false),"haptics":.bool(true),"animations":.bool(true),"reminder_minutes":.integer(15),"quiet_start_minutes":.integer(1380),"quiet_end_minutes":.integer(480)]
 public static func encode(_ values:[String:JSONValue])throws->[WireRecord]{
  try validate(values);return try values.map{try WireRecord(kind:.preference,fields:["uid":.string($0.key),"value":$0.value])}
 }
 public static func decode(_ records:[WireRecord])throws->[String:JSONValue]{
  var values:[String:JSONValue]=[:]
  for r in records{let d=try r.fields();guard r.kind == .preference,d.keys.sorted()==["uid","value"],d["uid"]?.string==r.uid,values[r.uid]==nil,let value=d["value"]else{throw RotamDataError.invalid("Tercih biçimi doğrulanamadı.")};values[r.uid]=value}
  try validate(values);return values
 }
 public static func validate(_ values:[String:JSONValue])throws{
  guard Set(values.keys)==Set(defaults.keys)else{throw RotamDataError.invalid("Tercih sürümü desteklenmiyor. Mevcut seçimlerin korundu.")}
  for(k,v)in values{
   if k=="reminder_minutes"{guard let i=v.integer,(0...10080).contains(i)else{throw RotamDataError.invalid("Hatırlatma süresi geçerli değil.")}}
   else if k=="quiet_start_minutes"||k=="quiet_end_minutes"{guard let i=v.integer,(0...1439).contains(i)else{throw RotamDataError.invalid("Sessiz saat geçerli değil.")}}
   else{guard v.boolean != nil else{throw RotamDataError.invalid("Tercih türü geçerli değil.")}}
  }
 }
}
