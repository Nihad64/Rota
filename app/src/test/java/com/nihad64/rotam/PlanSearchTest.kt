package com.nihad64.rotam
import com.nihad64.rotam.data.model.Plan
import com.nihad64.rotam.domain.search.PlanSearch
import com.nihad64.rotam.domain.usecase.PlanOutcome
import org.junit.Assert.*
import org.junit.Test
import java.time.*
class PlanSearchTest {
 private val now=LocalDateTime.parse("2026-11-11T12:00")
 @Test fun turkishMonthAndPreviousWeekUseExactCalendarRanges(){
  val p=Plan(title="Market",date="2024-05-10")
  assertEquals(listOf(p),PlanSearch.search("Mayıs 2024",listOf(p,p.copy(uid="other",date="2024-06-01")),now).plans)
  val done=p.copy(uid="done",date="2026-11-03",isCompleted=true)
  val pending=done.copy(uid="pending",isCompleted=false)
  assertEquals(listOf(done),PlanSearch.search("Geçen hafta yaptıklarım",listOf(done,pending),now).plans)
 }
 @Test fun completionCountsExcludeUnknownPartialDeletedAndFutureRecords(){
  val p=Plan(title="Spor",category="spor",date="2026-10-10",isCompleted=true)
  val unknown=p.copy(uid="unknown",isCompleted=false)
  val partial=PlanOutcome.record(p.copy(uid="partial"),PlanOutcome.Status.PARTIAL,2.0,5.0)
  val result=PlanSearch.search("Geçen ay kaç kere spor yaptım?",listOf(p,unknown,partial,p.copy(uid="deleted",isDeleted=true),p.copy(uid="future",date="2026-12-10")),now)
  assertEquals(listOf(p),result.plans);assertTrue(result.answer!!.contains("1 tamamlanmış"));assertTrue(result.answer.contains("1 kısmi ve 1 sonucu"))
 }
 @Test fun currentWeekWorkScheduleIsLocalAndComplexChangesRouteToAssistant(){
  val p=Plan(title="Vardiya",category="is",date="2026-11-12",startTime="14:00",endTime="22:00")
  assertEquals(listOf(p),PlanSearch.search("Bu hafta hangi günler çalışıyorum?",listOf(p),now).plans)
  assertNotNull(PlanSearch.search("Bu hafta hangi günler çalışıyorum?",listOf(p),now).answer)
  assertTrue(PlanSearch.needsAssistant("Perşembeki sporu işimle çakışmayacak şekilde cumaya taşı"))
 }
 @Test fun postponedHistoryMatchesOldDateOnceAndInternalMetadataIsNotKeywordSearchable(){
  val p=Plan(title="Alışveriş",date="2024-05-10",extraData="{\"note\":\"Süt ve yumurta\"}")
  val moved=com.nihad64.rotam.domain.usecase.PlanTime.moved(PlanOutcome.record(p,PlanOutcome.Status.POSTPONED),LocalDate.parse("2024-06-01"))
  assertFalse(PlanSearch.needsAssistant("Mayıs 2024 ertelenen"))
  assertFalse(PlanSearch.needsAssistant("Video düzenleme"))
  assertEquals(listOf(moved),PlanSearch.search("Mayıs 2024 ertelenen",listOf(moved,moved.copy(id=7)),now).plans)
  assertTrue(PlanSearch.search("POSTPONED",listOf(moved),now).plans.isEmpty())
  assertEquals(listOf(moved),PlanSearch.search("süt",listOf(moved),now).plans)
 }
}
