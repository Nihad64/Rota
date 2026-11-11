package com.nihad64.rotam
import com.nihad64.rotam.data.model.Plan
import com.nihad64.rotam.domain.usecase.PlanOutcome
import com.nihad64.rotam.widget.WidgetSnapshot
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime
class WidgetSnapshotTest {
 private val now=LocalDateTime.parse("2026-11-11T15:00")
 @Test fun timeAndUntimedPlansRemainVisibleWithoutCountingFutureAsFailure(){
  val untimed=Plan(title="Oku",date="2026-11-11")
  val timed=Plan(title="İş",date="2026-11-11",startTime="16:00")
  val future=Plan(title="Yarın",date="2026-11-12")
  val snapshot=WidgetSnapshot.build(listOf(untimed,timed,future),now)
  assertEquals(listOf(timed,untimed),snapshot.pending);assertEquals(2,snapshot.total);assertEquals(0,snapshot.awaiting)
 }
 @Test fun deletedAndDuplicateRecordsDoNotInflateProgressOrAwaitingCount(){
  val done=Plan(title="Bitti",date="2026-11-11",isCompleted=true)
  val deleted=Plan(title="Silindi",date=done.date,isDeleted=true)
  val unknown=Plan(title="Yanıtsız",date="2026-11-10")
  val partial=PlanOutcome.record(Plan(title="Kısmi",date="2026-11-10"),PlanOutcome.Status.PARTIAL,2.0,5.0)
  val snapshot=WidgetSnapshot.build(listOf(done,done.copy(id=9),deleted,unknown,partial),now)
  assertEquals(1,snapshot.total);assertEquals(100,snapshot.progress);assertEquals(1,snapshot.awaiting)
 }
 @Test fun ongoingOvernightPlanIsNotMisclassifiedAsMissedYesterday(){
  val p=Plan(title="Uzun iş",date="2026-11-10",startTime="22:00",endTime="18:00",endDate="2026-11-11")
  val snapshot=WidgetSnapshot.build(listOf(p),now)
  assertEquals(listOf(p),snapshot.pending);assertEquals(0,snapshot.awaiting)
 }
}
