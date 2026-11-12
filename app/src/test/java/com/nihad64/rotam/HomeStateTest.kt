package com.nihad64.rotam

import com.nihad64.rotam.data.model.Plan
import com.nihad64.rotam.ui.home.HomeState
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=android.app.Application::class)
class HomeStateTest {
 @Test fun overdueCardIsNotRepeatedAsHeroAndAnActiveSessionStillHasPriority() {
  val past=Plan(id=1,title="Video",date="2026-11-11",startTime="10:00",endTime="11:00")
  val next=Plan(id=2,title="Spor",date=past.date,startTime="19:00")
  val state=HomeState(today=LocalDate.parse(past.date),now=LocalDateTime.parse("2026-11-11T16:00"),todayPlans=listOf(past,next),missed=listOf(past))
  assertEquals(next,state.hero);assertNull(state.copy(todayPlans=listOf(past)).hero)
  assertEquals(past,state.copy(activeId=past.id).hero)
 }
}
