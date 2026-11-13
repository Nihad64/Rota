package com.nihad64.rotam

import com.nihad64.rotam.data.model.Plan
import com.nihad64.rotam.data.prefs.Settings
import com.nihad64.rotam.domain.usecase.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=android.app.Application::class)
class ReminderPolicyTest {
 private val settings=Settings(quietEnabled=false)
 private fun auto(p:Plan)=ReminderPolicy.withMode(p,ReminderPolicy.Mode.AUTO)
 @Test fun oldDisabledChoicesStayDisabledAndNewAutomaticPlansUseCategoryContext() {
  val legacy=Plan(title="İş",date="2026-11-12",startTime="14:00",endTime="22:00",category="is")
  assertEquals(ReminderPolicy.Mode.OFF,ReminderPolicy.mode(legacy));assertTrue(ReminderPolicy.events(legacy,settings).isEmpty())
  val events=ReminderPolicy.events(auto(legacy),settings)
  assertEquals(listOf("2026-11-12T13:40","2026-11-12T14:00","2026-11-12T22:30","2026-11-13T18:00"),events.map{it.at.toString()})
  assertEquals("2026-11-12T13:30",ReminderPolicy.events(auto(legacy.copy(category="randevu")),settings).first().at.toString())
 }
 @Test fun untimedPlanGetsCheckInWithoutInventingPlanTimeAndStopsAfterFinalFollowUp(){
  val p=auto(Plan(title="10 sayfa oku",date="2026-11-12"));val events=ReminderPolicy.events(p,settings)
  assertNull(p.startTime);assertNull(p.endTime);assertEquals(2,events.size)
  assertEquals(listOf("2026-11-12T19:00","2026-11-13T18:00"),events.map{it.at.toString()})
  assertTrue(ReminderPolicy.events(ReminderPolicy.markStarted(p),settings).isEmpty())
  assertTrue(ReminderPolicy.events(p.copy(isCompleted=true),settings).isEmpty());assertTrue(ReminderPolicy.events(p.copy(isSkipped=true),settings).isEmpty())
 }
 @Test fun quietHoursCoalesceStagesAndKnownWorkMovesCheckIn(){
  val p=auto(Plan(title="Kitap",date="2026-11-12",startTime="23:30",endTime="23:45"))
  val events=ReminderPolicy.events(p,Settings())
  assertEquals("2026-11-13T08:00",events.first().at.toString());assertEquals(events.size,events.map{it.at}.distinct().size)
  val untimed=auto(Plan(title="Oku",date="2026-11-12"))
  val work=Plan(title="İş",date=untimed.date,startTime="14:00",endTime="22:00",category="is")
  assertEquals("2026-11-12T22:15",ReminderPolicy.events(untimed,settings,listOf(work)).first().at.toString())
 }
 @Test fun oldCustomIsSingleAndStartedMarkerIsSpecificToSchedule(){
  val p=Plan(title="Görüşme",date="2026-11-12",startTime="10:00",reminderEnabled=true,reminderMinutes=5)
  assertEquals(listOf("2026-11-12T09:55"),ReminderPolicy.events(p,settings).map{it.at.toString()})
  val started=ReminderPolicy.markStarted(p);assertTrue(ReminderPolicy.started(started))
  assertFalse(ReminderPolicy.started(started.copy(date="2026-11-13")))
  assertFalse(ReminderPolicy.started(PlanOutcome.fresh(started)))
  assertEquals(ReminderPolicy.signature(p),ReminderPolicy.signature(p.copy(title="Yeni başlık",focusSeconds=300)))
  assertEquals(LocalDateTime.parse("2026-11-13T08:00"),ReminderPolicy.quietAdjusted(LocalDateTime.parse("2026-11-12T23:30"),Settings()))
 }
}
