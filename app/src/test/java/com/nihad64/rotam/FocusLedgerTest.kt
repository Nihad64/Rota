package com.nihad64.rotam
import com.nihad64.rotam.data.model.Plan
import com.nihad64.rotam.domain.usecase.*
import org.junit.Assert.*
import org.junit.Test
import java.time.*
class FocusLedgerTest {
 private val zone=ZoneId.of("Europe/Berlin")
 private fun start(date:String)=LocalDateTime.parse(date).atZone(zone).toInstant().toEpochMilli()
 @Test fun measuredMidnightSessionStaysOnActualDatesAfterPostponement(){
  val p=FocusLedger.record(Plan(title="Oku",date="2026-11-11"),"session",start("2026-11-11T23:59:30"),90,90)
  val moved=PlanTime.moved(p,LocalDate.parse("2026-12-10"))
  val daily=FocusLedger.byDay(listOf(moved),LocalDate.parse("2026-11-11"),LocalDate.parse("2026-11-12"),zone)
  assertEquals(30L,daily[LocalDate.parse("2026-11-11")]);assertEquals(60L,daily[LocalDate.parse("2026-11-12")])
  assertTrue(FocusLedger.byDay(listOf(moved),LocalDate.parse("2026-12-10"),LocalDate.parse("2026-12-10"),zone).isEmpty())
 }
 @Test fun repeatedRecoveryOfTheSameIntervalNeverDoubleCounts(){
  val p=Plan(title="Oku",date="2026-11-11",extraData="{\"note\":\"Koru\"}")
  val first=FocusLedger.record(p,"segment",start("2026-11-11T12:00"),60,60)
  val repeated=FocusLedger.record(first,"segment",start("2026-11-11T12:01"),90,90)
  assertEquals(1,FocusLedger.segments(repeated).size);assertEquals(90,FocusLedger.segments(repeated).single().seconds)
  assertTrue(repeated.extraData.contains("Koru"));assertEquals(0,FocusLedger.unassigned(listOf(repeated)))
 }
 @Test fun legacyUndatedTimeIsExplicitAndNewRoutineDoesNotInheritOldMeasuredIntervals(){
  val legacy=Plan(title="Eski",date="2026-11-11",focusSeconds=120)
  assertEquals(120,FocusLedger.unassigned(listOf(legacy)))
  assertTrue(FocusLedger.byDay(listOf(legacy),LocalDate.parse(legacy.date),LocalDate.parse(legacy.date),zone).isEmpty())
  val tracked=FocusLedger.record(legacy,"segment",start("2026-11-11T12:00"),60,180)
  assertTrue(FocusLedger.segments(PlanOutcome.fresh(tracked)).isEmpty())
 }
}
