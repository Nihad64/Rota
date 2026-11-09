package com.nihad64.rotam
import com.nihad64.rotam.data.model.Plan
import com.nihad64.rotam.domain.usecase.*
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject
import java.time.LocalDate
class PlanOutcomeTest {
 private val p=Plan(id=5,title="20 sayfa oku",date="2026-11-01",extraData="""{"note":"koru"}""")
 @Test fun timePassingNeverTurnsUnknownIntoFailure(){assertEquals(PlanOutcome.Status.UNKNOWN,PlanOutcome.status(p));assertNull(PlanOutcome.fraction(p))}
 @Test fun partialAmountIsExplicitAndPreservesIdentityAndNotes(){
  val partial=PlanOutcome.record(p,PlanOutcome.Status.PARTIAL,10.0,20.0)
  assertEquals(0.5,PlanOutcome.fraction(partial)!!,0.001);assertEquals(p.uid,partial.uid);assertFalse(partial.isCompleted);assertFalse(partial.isSkipped)
  assertEquals("koru",JSONObject(partial.extraData).getString("note"))
  for(a in listOf(0.0,-1.0,20.0,21.0,Double.NaN))assertThrows(IllegalArgumentException::class.java){PlanOutcome.record(p,PlanOutcome.Status.PARTIAL,a,20.0)}
 }
 @Test fun postponingKeepsOldScheduleAndDoesNotDuplicateThePlan(){
  val moved=PlanTime.moved(PlanOutcome.record(p,PlanOutcome.Status.POSTPONED),LocalDate.parse("2026-11-10"))
  assertEquals(p.id,moved.id);assertEquals(p.uid,moved.uid);assertEquals("2026-11-01",JSONObject(moved.extraData).getJSONArray("outcomeHistory").getJSONObject(0).getString("date"))
  assertFalse(moved.isCompleted);assertFalse(moved.isSkipped)
 }
 @Test fun explicitDoneAndNotDoneRemainDistinctAndCanBeReopened(){
  val done=PlanOutcome.record(p,PlanOutcome.Status.COMPLETED);assertTrue(done.isCompleted)
  val skipped=PlanOutcome.record(done,PlanOutcome.Status.NOT_DONE);assertTrue(skipped.isSkipped);assertFalse(skipped.isCompleted)
  val reopened=PlanOutcome.record(skipped,PlanOutcome.Status.UNKNOWN);assertFalse(reopened.isSkipped);assertNull(PlanOutcome.fraction(reopened))
  assertEquals(3,JSONObject(reopened.extraData).getJSONArray("outcomeHistory").length())
 }
}
