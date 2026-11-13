import XCTest
#if canImport(RotamDomain)
@testable import RotamDomain
#else
@testable import Rotam
#endif

@MainActor final class PlannerTests:XCTestCase {
 func testNativeNullNormalizationAndUnknownNullFieldRoundTrip()throws{
  let r=try PortablePlan.make(title:"Saat bağımsız",date:"2026-11-12")
  let d=try r.fields();XCTAssertNil(d["startTime"]);XCTAssertNil(d["endTime"]);XCTAssertNil(d["endDate"])
  let future=try r.changing(["futureNullable":.null]);XCTAssertEqual(.null,try future.fields()["futureNullable"])
 }
 func testRecurrenceUsesCanonicalUIDAndClampsToRepeatUntil()throws{
  let p=try PortablePlan.make(title:"Gece",date:"2026-11-12",start:"22:00",end:"06:00",endDate:"2026-11-13")
  let repeated=try p.changing(["repeatType":.string("daily"),"repeatUntil":.string("2026-11-14")])
  let records=try RecurrenceRules.make(repeated,through:"2040-01-01");try CoreValidator.validate(records)
  let series=try XCTUnwrap(records.first{$0.kind == .series}),plans=try records.filter{$0.kind == .plan}.map(PortablePlan.init)
  XCTAssertEqual(3,plans.count);XCTAssertTrue(plans.allSatisfy{$0.id=="series:\(series.uid):\($0.date)"});XCTAssertEqual("2026-11-15",plans.last?.endDate)
 }
 func testFocusUpsertIsIdempotentAndDoesNotErasePlanNotes()throws{
  let p=try PortablePlan.make(title:"Oku",date:"2026-11-12").changing(["extraData":.string("{\"note\":\"Korunmalı\"}")])
  let first=try PlanActions.focus(p,uid:"segment",start:1000,seconds:10)
  let next=try PlanActions.focus(first,uid:"segment",start:9999,seconds:30)
  let duplicate=try PlanActions.focus(next,uid:"segment",start:9999,seconds:20)
  let plan=try PortablePlan(duplicate);XCTAssertEqual(30,plan.focusSeconds);XCTAssertEqual("Korunmalı",plan.metadata["note"]?.string);XCTAssertEqual(1000,plan.metadata["focusSegments"]?.items?.first?.fields?["startedAt"]?.integer)
 }
 func testAIUpdateCannotEraseOmittedTimesOrInventIdentity()throws{
  let p=try PortablePlan.make(title:"İş",date:"2026-11-12",start:"14:00",end:"22:00",uid:"existing")
  XCTAssertThrowsError(try AiDraftParser.parse("{\"plans\":[{\"planId\":\"existing\",\"title\":\"İş\",\"date\":\"2026-11-13\"}]}",existing:[p],memories:[]))
  XCTAssertThrowsError(try AiDraftParser.parse("{\"plans\":[{\"planId\":\"invented\",\"title\":\"İş\",\"date\":\"2026-11-13\",\"startTime\":null,\"endTime\":null,\"endDate\":null}]}",existing:[p],memories:[]))
  let reply=try AiDraftParser.parse("{\"reply\":\"İncele\",\"plans\":[{\"planId\":null,\"title\":\"Alışveriş\",\"date\":\"2026-11-13\",\"startTime\":null,\"endTime\":null,\"endDate\":null}]}",existing:[],memories:[])
  XCTAssertNil(try PortablePlan(reply.plans[0].plan).start)
 }
 func testReminderBudgetQuietHoursAndStartedCancellation()throws{
  let now=try PlanDates.instant(day:"2026-11-11",time:"00:00"),p=try PortablePlan.make(title:"Oku",date:"2026-11-12")
  let rows=try (0..<10).map{try PortablePlan(p.changing(["uid":.string("plan-\($0)")]))},events=try ReminderRules.events(rows,settings:PortableSettings.defaults,now:now)
  XCTAssertLessThanOrEqual(events.filter{PlanDates.key($0.at)=="2026-11-12"}.count,6)
  XCTAssertTrue(events.allSatisfy{Calendar.current.component(.hour,from:$0.at)>=8})
  let started=try PortablePlan(PlanActions.markStarted(rows[0]));XCTAssertTrue(try ReminderRules.events([started],settings:PortableSettings.defaults,now:now).isEmpty)
 }
 func testPreferencesUseOwnWireSchemaAndRejectPrivacyFlags()throws{
  let rows=try PortableSettings.encode(PortableSettings.defaults);XCTAssertEqual(PortableSettings.defaults,try PortableSettings.decode(SyncCodec.decode(SyncCodec.encode(rows))))
  var values=PortableSettings.defaults;values["ai-share-plans"] = .bool(true);XCTAssertThrowsError(try PortableSettings.validate(values));XCTAssertThrowsError(try CoreValidator.validate(rows))
 }
 func testDraftRevisionPreventsLateWriteAndReopensPendingInput()async throws{
  let root=FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
  defer{try? FileManager.default.removeItem(at:root)}
  let store=try RecordStore(directory:root),url=root.appendingPathComponent("draft-owner.json")
  try await store.saveDraft(url:url,input:"Yeni",pending:"Gönderilen",revision:2)
  try await store.saveDraft(url:url,input:"Eski",pending:"",revision:1)
  let draft=try await store.loadDraft(url:url);XCTAssertEqual("Yeni",draft.input);XCTAssertEqual("Gönderilen",draft.pending)
  let fresh=try RecordStore(directory:root),same=try await fresh.loadDraft(url:url);XCTAssertEqual(draft.input,same.input)
 }
 func testPausedGapCannotBeBackfilledAfterResume()async throws{
  let root=FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
  defer{try? FileManager.default.removeItem(at:root)}
  let store=try RecordStore(directory:root),p=try PortablePlan.make(title:"Oku",date:"2026-11-02").changing(["repeatType":.string("daily")])
  let batch=try RecurrenceRules.make(p,through:"2026-11-04"),series=batch.first{$0.kind == .series}!
  try await store.replaceMany(batch);try await store.pauseRoutine(series.uid,paused:true,day:"2026-11-05");try await store.pauseRoutine(series.uid,paused:false,day:"2026-11-08");try await store.ensure(from:"2026-11-02",to:"2026-11-10")
  let rows=await store.all(),plans=try rows.filter{$0.kind == .plan}.map(PortablePlan.init)
  XCTAssertFalse(plans.contains{$0.date>="2026-11-05"&&$0.date<"2026-11-08" && !$0.deleted})
  XCTAssertTrue(plans.contains{$0.date=="2026-11-08" && !$0.deleted})
 }
 func testNaturalSearchKeepsUnknownKeywordInsideKnownPeriod()throws{
  let now=try PlanDates.instant(day:"2026-11-12",time:"12:00")
  let market=try PortablePlan(PortablePlan.make(title:"Market",date:"2026-11-03")),work=try PortablePlan(PortablePlan.make(title:"İş",date:"2026-11-04",category:"is"))
  XCTAssertEqual([market.id],LocalSearch.search("Bu ay market",plans:[market,work],now:now).plans.map(\.id))
  let count=LocalSearch.search("Bu ay market kaç kere yaptım",plans:[market,work],now:now)
  XCTAssertEqual(0,count.plans.count);XCTAssertNotNil(count.answer)
  let complete=try PortablePlan(PlanActions.outcome(market.record,status:"COMPLETED"))
  XCTAssertEqual(1,LocalSearch.search("Kasım 2026 market kaç kere yaptım",plans:[complete,work],now:now).plans.count)
  XCTAssertTrue(LocalSearch.search("İşi yarına taşı",plans:[market,work],now:now).needsAI)
 }
 func testAIProposalUsesAppOwnedApprovalTextAndRetainsShoppingDetails()throws{
  let reply=try AiDraftParser.parse("{\"reply\":\"Planınız eklendi.\",\"plans\":[{\"title\":\"Alışveriş\",\"date\":\"2026-11-13\",\"note\":\"İşten önce\",\"subtasks\":[\"Süt al\",\"Yumurta al\"]}]}",existing:[],memories:[])
  XCTAssertTrue(reply.text.contains("Henüz kaydedilmedi"));XCTAssertFalse(reply.text.contains("eklendi"))
  let p=try PortablePlan(reply.plans[0].plan);XCTAssertNil(p.start);XCTAssertNil(p.end);XCTAssertEqual("İşten önce",p.metadata["note"]?.string);XCTAssertEqual("Süt al\nYumurta al",p.metadata["subtasks"]?.string)
 }
 func testDeletedRoutinePreservesResultsAndDoesNotRegenerate()async throws{
  let root=FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
  defer{try? FileManager.default.removeItem(at:root)}
  let store=try RecordStore(directory:root),p=try PortablePlan.make(title:"Okul",date:"2026-11-10").changing(["repeatType":.string("daily")]),batch=try RecurrenceRules.make(p,through:"2026-11-15"),rule=batch.first{$0.kind == .series}!,first=batch.first{$0.kind == .plan}!
  try await store.replaceMany(batch);let done=try PlanActions.outcome(first,status:"COMPLETED");try await store.replace(done,expected:first)
  let receipt=try await store.deleteRoutine(rule.uid,day:"2026-11-13"),deleted=await store.all()
  XCTAssertTrue(deleted.contains(done));XCTAssertTrue(try deleted.filter{$0.kind == .plan && $0.uid != first.uid}.allSatisfy{try PortablePlan($0).deleted})
  let generated=try await store.ensure(from:"2026-11-16",to:"2027-01-01");XCTAssertFalse(generated)
  try await store.undoRoutineDeletion(receipt);let restored=await store.all();XCTAssertTrue(restored.contains(done));XCTAssertTrue(restored.contains(rule))
 }
 func testOneRoutineProducesOneSearchRowAndPastStartOnlyPlanNeedsReview()throws{
  let p=try PortablePlan.make(title:"Okul",date:"2026-11-13",start:"06:00").changing(["repeatType":.string("daily")]),batch=try RecurrenceRules.make(p,through:"2027-03-16"),plans=try batch.filter{$0.kind == .plan}.map(PortablePlan.init)
  let rows=SearchPresentation.rows(plans,records:batch,query:"Okul",today:"2026-11-13")
  XCTAssertEqual("1 rutin",SearchPresentation.label(rows));XCTAssertEqual(1,rows.count)
  XCTAssertTrue(plans[0].awaiting(now:try PlanDates.instant(day:"2026-11-13",time:"10:00")));XCTAssertNil(plans[0].end)
 }
}
