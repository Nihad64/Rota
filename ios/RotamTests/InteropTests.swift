import XCTest
#if canImport(RotamDomain)
@testable import RotamDomain
#else
@testable import Rotam
#endif

@MainActor final class InteropTests:XCTestCase {
 private func fixture()throws->Data{
  #if SWIFT_PACKAGE
  let bundle=Bundle.module
  #else
  let bundle=Bundle(for:Self.self)
  #endif
  guard let url=bundle.url(forResource:"core-wire",withExtension:"bin",subdirectory:"Fixtures") ?? bundle.url(forResource:"core-wire",withExtension:"bin")else{throw RotamDataError.invalid("Ortak test fikstürü bulunamadı.")}
  return try Data(contentsOf:url)
 }
 func testAndroidGzipAndPortableIdentitiesDecodeWithoutLosingNotes()throws{
  let records=try SyncCodec.decode(fixture());XCTAssertEqual(4,records.count)
  try CoreValidator.validate(records,owner:"contract-owner")
  let p=try PortablePlan(XCTUnwrap(records.first{$0.kind == .plan}))
  XCTAssertEqual("series:contract-series:2026-11-12",p.id);XCTAssertEqual("contract-series",p.seriesUID);XCTAssertNil(p.start);XCTAssertEqual("18:00",p.end)
  XCTAssertEqual("İş / öğrenme\nİkinci satır: ığüşçö",p.metadata["note"]?.string)
  XCTAssertEqual("Korunmalı",p.metadata["unknownFutureField"]?.string)
  let repacked=try SyncCodec.decode(SyncCodec.encode(records))
  XCTAssertEqual(try SyncEnvelope(records:records).index(),try SyncEnvelope(records:repacked).index())
 }
 func testTruncatedOrTamperedGzipIsRejected()throws{
  let data=try fixture();XCTAssertThrowsError(try SyncCodec.decode(data.dropLast()))
  var changed=data;changed[changed.count-5]^=0x7F;XCTAssertThrowsError(try SyncCodec.decode(changed))
 }
 func testParentDeletionAndNewChildCannotBeMergedIntoAnInvalidGraph()throws{
  let rows=try SyncCodec.decode(fixture());let base=try SyncEnvelope(records:rows).index()
  let parent=rows.first{$0.kind == .series}!
  var local=base;local.removeValue(forKey:parent.key);local=local.filter{$0.key.kind != .plan}
  var remote=base;let plan=try PortablePlan(rows.first{$0.kind == .plan}!)
  let changed=try plan.record.changing(["title":.string("Diğer cihaz")]);remote[changed.key]=changed
  let review=try CoreValidator.merge(base:base,local:local,remote:remote,owner:"contract-owner")
  XCTAssertFalse(review.conflicts.isEmpty)
  try CoreValidator.validate(Array(review.choosingLocal(true).values));try CoreValidator.validate(Array(review.choosingLocal(false).values))
 }
 func testUntimedAndDeadlinePlansStayIndependentAndUnknownMetadataSurvives()throws{
  let untimed=try PortablePlan.make(title:"Oku",date:"2026-11-12");let p=try PortablePlan(untimed);XCTAssertNil(p.start);XCTAssertNil(p.end)
  let deadline=try PortablePlan.make(title:"Teslim",date:"2026-11-12",end:"18:00");XCTAssertNil(try PortablePlan(deadline).start)
  XCTAssertThrowsError(try PortablePlan.make(title:"Geçersiz",date:"2026-02-30"))
  XCTAssertThrowsError(try PortablePlan.make(title:"Geçersiz",date:"2026-11-12",start:"19:00",end:"18:00"))
  let overnight=try PortablePlan.make(title:"Gece",date:"2026-11-12",start:"22:00",end:"06:00",endDate:"2026-11-13");try CoreValidator.validate([overnight])
 }
 func testRecordStoreRejectsStaleImportAndKeepsPrivateMemoryOffTheCloud()async throws{
  let root=FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
  defer{try? FileManager.default.removeItem(at:root)}
  let store=try RecordStore(directory:root);let p=try PortablePlan.make(title:"Oku",date:"2026-11-12");try await store.replace(p)
  try await store.saveMemory(owner:"owner",text:"Yalnız bu cihazda",cloud:false)
  try await store.bind("owner")
  let before=await store.cloudRecords(owner:"owner");XCTAssertEqual(1,before.count)
  let changed=try p.changing(["title":.string("Yeni yerel başlık")]);try await store.replace(changed,expected:p)
  let applied=try await store.applyRemote(expected:before,incoming:before,baseline:SyncEnvelope(revision:1,records:before),owner:"owner");XCTAssertFalse(applied)
  let current=await store.all();XCTAssertEqual("Yeni yerel başlık",try PortablePlan(current.first{$0.kind == .plan}!).title)
  let memories=await store.memory(owner:"owner");XCTAssertEqual(1,memories.count)
  let reloaded=try RecordStore(directory:root);let same=await reloaded.memory(owner:"owner");XCTAssertEqual(memories,same)
  try await store.forgetMemory(owner:"owner",record:memories[0]);let forgotten=await store.memory(owner:"owner");XCTAssertTrue(forgotten.isEmpty)
 }
}
