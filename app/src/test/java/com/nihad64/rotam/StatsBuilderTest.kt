package com.nihad64.rotam
import com.nihad64.rotam.data.model.Plan
import com.nihad64.rotam.domain.usecase.PlanOutcome
import com.nihad64.rotam.ui.stats.*
import org.junit.Assert.*
import org.junit.Test
import java.time.*
class StatsBuilderTest {
 private val now=LocalDateTime.parse("2026-11-11T12:00")
 @Test fun futureAndUnknownPlansAreNotMarkedAsNotDone(){
  val future=Plan(title="Yarın",date="2026-11-12");val unknown=Plan(title="Dün",date="2026-11-10")
  val partial=PlanOutcome.record(Plan(title="Kısmi",date="2026-11-10"),PlanOutcome.Status.PARTIAL,2.0,5.0)
  val s=StatsBuilder.build(listOf(future,unknown,partial),emptyList(),StatsWindow(anchor=now.toLocalDate()),now)
  assertEquals(1,s.pending);assertEquals(1,s.awaiting);assertEquals(1,s.partial);assertEquals(0,s.notDone)
 }
 @Test fun fullCalendarMonthAndWeekBoundariesAreUsed(){
  val window=StatsWindow(month=true,anchor=LocalDate.parse("2026-02-15"))
  val s=StatsBuilder.build(emptyList(),emptyList(),window,now)
  assertEquals(LocalDate.parse("2026-02-01"),s.from);assertEquals(LocalDate.parse("2026-02-28"),s.to);assertEquals(28,s.week.size)
  val week=StatsBuilder.build(emptyList(),emptyList(),StatsWindow(anchor=LocalDate.parse("2026-11-11")),now)
  assertEquals(LocalDate.parse("2026-11-09"),week.from);assertEquals(LocalDate.parse("2026-11-15"),week.to)
 }
 @Test fun deletedAndDuplicatePlansDoNotInflateCountsAndLegacyTimeIsNotAssignedToADate(){
  val p=Plan(title="Bitti",date="2026-11-11",isCompleted=true,focusSeconds=120)
  val s=StatsBuilder.build(listOf(p,p.copy(id=8),p.copy(uid="deleted",isDeleted=true,focusSeconds=0)),emptyList(),StatsWindow(anchor=now.toLocalDate()),now)
  assertEquals(1,s.total);assertEquals(1,s.totalDone);assertEquals(0,s.focusSeconds);assertEquals(120,s.legacyFocusSeconds)
 }
}
