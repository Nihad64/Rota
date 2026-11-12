import Foundation
import RotamDomain

func check(_ condition:Bool,_ message:String)throws{if !condition{throw RotamDataError.invalid("Sözleşme kontrolü: \(message)")}}
func rejects(_ action:()throws->Void)throws{do{try action()}catch{return};throw RotamDataError.invalid("Geçersiz veri kabul edildi.")}

@main struct ContractCheck {
 static func main()async throws{
  guard (3...4).contains(CommandLine.arguments.count) else{throw RotamDataError.invalid("core-wire.bin ve çıktı dosyası gerekli.")}
  let input=try Data(contentsOf:URL(fileURLWithPath:CommandLine.arguments[1]));let rows=try SyncCodec.decode(input)
  try check(rows.count==4,"kayıt sayısı");try CoreValidator.validate(rows,owner:"contract-owner")
  let p=try PortablePlan(rows.first{$0.kind == .plan}!)
  try check(p.id=="series:contract-series:2026-11-12","kalıcı örnek UID")
  try check(p.start==nil&&p.end=="18:00","bağımsız son teslim")
  try check(p.metadata["note"]?.string=="İş / öğrenme\nİkinci satır: ığüşçö","Türkçe/not/satır sonu")
  try check(p.metadata["unknownFutureField"]?.string=="Korunmalı","bilinmeyen metadata")
  let packed=try SyncCodec.decode(SyncCodec.encode(rows));try check(SyncEnvelope(records:packed).index()==SyncEnvelope(records:rows).index(),"kayıpsız paketleme")
  try rejects{_ = try SyncCodec.decode(input.dropLast())}
  var corrupt=input;corrupt[corrupt.count-5]^=0x7f;try rejects{_ = try SyncCodec.decode(corrupt)}
  let changed=try p.record.changing(["title":.string("Swift'ten gelen plan"),"focusSeconds":.integer(75)])
  let native=try PortablePlan.make(title:"Swift yeni saatsiz plan",date:"2026-11-13",uid:"contract-native-new",createdAt:123456789)
  let edited=rows.map{$0.key==changed.key ? changed:$0}+[native];try CoreValidator.validate(edited,owner:"contract-owner")
  try SyncCodec.encode(edited).write(to:URL(fileURLWithPath:CommandLine.arguments[2]),options:.atomic)
  if CommandLine.arguments.count==4{
   let golden=try SyncCodec.decode(Data(contentsOf:URL(fileURLWithPath:CommandLine.arguments[3])))
   try check(SyncEnvelope(records:edited).index()==SyncEnvelope(records:golden).index(),"Android'in doğruladığı Apple paketiyle aynı kayıtlar")
  }
  let untimed=try PortablePlan(PortablePlan.make(title:"Oku",date:"2026-11-12"));try check(untimed.start==nil&&untimed.end==nil,"saatsiz plan")
  try rejects{_ = try PortablePlan.make(title:"Yanlış",date:"2026-02-30")}
  let root=FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString);defer{try? FileManager.default.removeItem(at:root)}
  let store=try RecordStore(directory:root);try await store.replace(untimed.record);try await store.saveMemory(owner:"owner",text:"Yerel sentetik tercih",cloud:false);try await store.bind("owner")
  let before=await store.cloudRecords(owner:"owner");try check(before.count==1,"özel hafızanın dışarı çıkmaması")
  try await store.replace(untimed.record.changing(["title":.string("Yeni yerel metin")]),expected:untimed.record)
  let applied=try await store.applyRemote(expected:before,incoming:before,baseline:SyncEnvelope(revision:1,records:before),owner:"owner");try check(!applied,"eski ağ yanıtının yerel metni ezmemesi")
  let restored=try RecordStore(directory:root);let memory=await restored.memory(owner:"owner");try check(memory.count==1,"özel hafızanın yeniden açılışı")
  try await restored.forgetMemory(owner:"owner",record:memory[0]);let forgotten=await restored.memory(owner:"owner");try check(forgotten.isEmpty,"unutma")
  print("Apple core contract: Android gzip, identities, notes, unknown metadata, reverse payload, corruption, private memory and stale import verified")
 }
}
