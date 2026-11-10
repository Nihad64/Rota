package com.nihad64.rotam.ui.ai

import com.nihad64.rotam.data.model.Plan
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class LocalPlanQuestionTest {
 private val today=LocalDate.of(2026,11,10)
 @Test fun lastMonthUsesCalendarBoundariesAndOnlyReportedCompletions(){
  val plans=listOf(Plan(title="Spor",category="spor",date="2026-10-01",isCompleted=true),Plan(title="Spor",category="spor",date="2026-10-31",isCompleted=true),Plan(title="Spor",category="spor",date="2026-09-30",isCompleted=true),Plan(title="Spor",category="spor",date="2026-11-01",isCompleted=true),Plan(title="Oku",category="kitap",date="2026-10-10",isCompleted=true),Plan(title="Spor",category="spor",date="2026-10-10"))
  val answer=LocalPlanQuestion.answer("Geçen ay kaç kere spor yaptım?",plans,today)!!
  assertTrue(answer.contains("2 tamamlanmış"));assertTrue(answer.contains("yapılmadı saymadım"))
 }
 @Test fun futureAndDeletedPlansNeverInflateCurrentWeek(){
  val p=Plan(title="Spor",category="spor",date="2026-11-09",isCompleted=true)
  val answer=LocalPlanQuestion.answer("Bu hafta kaç kez spor yaptım?",listOf(p,p,p.copy(uid="deleted",isDeleted=true),p.copy(uid="future",date="2026-11-11")),today)!!
  assertTrue(answer.contains("1 tamamlanmış"))
 }
 @Test fun ambiguousOrDifferentIntentFallsThroughInsteadOfInventingAnswer(){
  assertNull(LocalPlanQuestion.answer("Kaç kere spor ve okuma yaptım?",emptyList(),today))
  assertNull(LocalPlanQuestion.answer("Geçen ay kaç kez spor yapmadım?",emptyList(),today))
  assertNull(LocalPlanQuestion.answer("Bu hafta kaç tane spor planım var?",emptyList(),today))
  assertNull(LocalPlanQuestion.answer("Sporu yarına taşı",emptyList(),today))
 }
}
