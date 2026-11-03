package com.nihad64.rotam
import com.nihad64.rotam.data.model.*
import com.nihad64.rotam.domain.usecase.*
import com.nihad64.rotam.data.db.LegacyParser
import com.nihad64.rotam.data.backup.*
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class PlanRulesTest {
 private val p=Plan(id=1,title="Okuma",date="2026-11-02")
 @Test fun conflictsRequireTwoKnownEndpointsAndExcludeTouchingEdges() {
  val a=p.copy(startTime="10:00",endTime="11:00")
  assertTrue(PlanTime.overlaps(a,p.copy(id=2,startTime="10:30",endTime="12:00")))
  assertFalse(PlanTime.overlaps(a,p.copy(id=2,startTime="11:00",endTime="12:00")))
  assertFalse(PlanTime.overlaps(a,p.copy(id=2,startTime="10:30")))
  assertFalse(PlanTime.overlaps(a,p.copy(id=2,endTime="10:30")))
 }
 @Test fun calendarClipsOvernightBlocksAndSeparatesOverlaps() {
  val day=LocalDate.parse("2026-11-03")
  val night=p.copy(startTime="22:00",endTime="02:00",endDate=day.toString())
  val overlap=p.copy(id=2,date=day.toString(),startTime="01:00",endTime="03:00")
  val deadline=p.copy(id=3,date=day.toString(),endTime="18:00")
  val blocks=com.nihad64.rotam.ui.calendar.TimelineLayout.blocks(listOf(night,overlap,deadline),day)
  assertEquals(2,blocks.size);assertEquals(0,blocks[0].from);assertEquals(120,blocks[0].to);assertNotEquals(blocks[0].lane,blocks[1].lane)
 }
 @Test fun fourIndependentTimeKinds() {
  assertEquals(TimeKind.UNTIMED,PlanTime.kind(p));assertNull(PlanTime.durationMinutes(p))
  val start=p.copy(startTime="10:00");assertEquals(TimeKind.START,PlanTime.kind(start));assertFalse(PlanTime.overdue(start,LocalDateTime.parse("2026-11-04T12:00")))
  val deadline=p.copy(endTime="18:00");assertEquals(TimeKind.DEADLINE,PlanTime.kind(deadline));assertNull(PlanTime.durationMinutes(deadline));assertTrue(PlanTime.overdue(deadline,LocalDateTime.parse("2026-11-02T18:01")))
  val range=start.copy(endTime="11:30");assertEquals(TimeKind.INTERVAL,PlanTime.kind(range));assertEquals(90L,PlanTime.durationMinutes(range))
 }
 @Test fun crossingMidnightRequiresExplicitDateAndMovesTogether() {
  val wrong=p.copy(startTime="22:00",endTime="02:00");assertNotNull(PlanTime.validate(wrong))
  val right=wrong.copy(endDate="2026-11-03");assertNull(PlanTime.validate(right));assertEquals(240L,PlanTime.durationMinutes(right))
  val moved=PlanTime.moved(right,LocalDate.parse("2026-11-05"));assertEquals("2026-11-06",moved.endDate)
 }
 @Test fun remindersUsePerPlanOffsetAndDoNotInventStart() {
  val separate=p.copy(reminderEnabled=true,reminderTime="19:00");assertEquals("2026-11-02T19:00",PlanTime.reminder(separate).toString());assertNull(separate.startTime)
  assertNotNull(PlanTime.validate(p.copy(reminderEnabled=true)))
  assertEquals("2026-11-02T17:30",PlanTime.reminder(p.copy(endTime="18:00",reminderEnabled=true,reminderMinutes=30)).toString())
 }
 @Test fun removingEndTimeFallsBackToStartReminder() {
  val start=p.copy(startTime="10:00",reminderEnabled=true,remindAtEnd=true,reminderMinutes=15)
  assertNull(PlanTime.validate(start))
  assertEquals("2026-11-02T09:45",PlanTime.reminder(start).toString())
 }
 @Test fun recurrenceContinuesBeyondSixtyDaysAndHandlesShortMonths() {
  val anchor=LocalDate.parse(p.date);assertEquals(1,Recurrence.dates(p.copy(repeatType="daily"),anchor,anchor.plusDays(600),anchor.plusDays(600)).size)
  val january=LocalDate.parse("2027-01-31");val feb=LocalDate.parse("2027-02-28")
  assertFalse(Recurrence.occurs(p.copy(repeatType="monthly"),january,feb))
  assertTrue(Recurrence.occurs(p.copy(repeatType="monthly",monthLastDay=true),january,feb))
 }
 @Test fun backupRoundTripPreservesNullTimesAndRejectsMissingSeries() {
  val data=BackupData(listOf(p.copy(focusSeconds=123)),emptyList(),emptyList())
  assertEquals(data,BackupCodec.decode(BackupCodec.encode(data)))
  val broken=data.copy(plans=listOf(p.copy(seriesId=9)))
  assertThrows(IllegalArgumentException::class.java){BackupCodec.decode(BackupCodec.encode(broken))}
 }
 @Test fun legacyUntimedDoesNotBecomeMidnightAndNightShiftSurvives() {
  val plans=LegacyParser.parse("""[{"id":1,"title":"Oku","date":"2026-11-02","type":"Saatsiz","time":"00:00"},{"id":2,"title":"İş","date":"2026-11-02","type":"Saatli","startTime":"22:00","endTime":"02:00","done":true}]""")
  assertNull(plans[0].startTime);assertNull(plans[0].endTime);assertEquals("2026-11-03",plans[1].endDate);assertTrue(plans[1].isCompleted)
 }
}
