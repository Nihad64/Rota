package com.nihad64.rotam

import com.nihad64.rotam.data.model.Plan
import com.nihad64.rotam.domain.usecase.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ReschedulePlanTest {
 @Test fun overnightDurationAndPriorScheduleRemainIntact(){
  val p=Plan(id=41,title="Gece vardiyası",date="2026-11-11",startTime="22:00",endTime="06:00",endDate="2026-11-12",extraData="{\"note\":\"Korunacak\"}",focusSeconds=120)
  val moved=ReschedulePlan.propose(p,LocalDate.parse("2026-11-13"),"23:30")
  assertEquals("07:30",moved.endTime);assertEquals("2026-11-14",moved.endDate);assertEquals(480L,PlanTime.durationMinutes(moved))
  assertEquals(p.uid,moved.uid);assertEquals(120,moved.focusSeconds);assertEquals("Korunacak",JSONObject(moved.extraData).getString("note"))
  val event=JSONObject(moved.extraData).getJSONArray("outcomeHistory").getJSONObject(0)
  assertEquals(p.date,event.getString("date"));assertEquals(p.startTime,event.getString("startTime"))
 }
 @Test fun deadlinesAndUntimedPlansDoNotGainInventedStartOrEnd(){
  val deadline=Plan(title="Teslim",date="2026-11-11",endTime="18:00")
  val changed=ReschedulePlan.propose(deadline,LocalDate.parse("2026-11-12"),"20:00")
  assertNull(changed.startTime);assertEquals("20:00",changed.endTime)
  val untimed=ReschedulePlan.propose(Plan(title="Oku",date=deadline.date),LocalDate.parse("2026-11-13"),null)
  assertNull(untimed.startTime);assertNull(untimed.endTime);assertNull(untimed.endDate)
 }
 @Test fun oneOccurrenceMayMovePastSeriesEndWithoutChangingItsOriginalIdentity(){
  val p=Plan(id=1,title="Spor",date="2026-11-11",startTime="18:00",repeatType="weekly",seriesId=7,repeatUntil="2026-11-11",originalDate="2026-11-11",occurrenceKey="7:2026-11-11")
  val moved=ReschedulePlan.propose(p,LocalDate.parse("2026-11-13"),"19:00")
  assertEquals(p.uid,moved.uid);assertEquals(p.originalDate,moved.originalDate);assertEquals(p.occurrenceKey,moved.occurrenceKey)
  assertNull(PlanTime.validate(moved));assertEquals(p.repeatUntil,moved.repeatUntil)
 }
}
