package com.nihad64.rotam

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nihad64.rotam.data.db.AppDatabase
import com.nihad64.rotam.data.model.*
import com.nihad64.rotam.data.repository.PlanRepository
import com.nihad64.rotam.domain.search.*
import com.nihad64.rotam.domain.usecase.*
import com.nihad64.rotam.ui.calendar.CalendarState
import com.nihad64.rotam.ui.home.HomeState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.json.JSONObject
import java.time.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=android.app.Application::class)
class PlannerUsabilityTest {
 @Test fun pastStartOnlyPrayerIsReviewedWithoutInventedEndOrFailure(){
  val prayer=Plan(id=1,title="Sabah namazı",date="2026-11-13",startTime="06:00",category="ibadet")
  val now=LocalDateTime.parse("2026-11-13T10:00")
  assertTrue(PlanReviewPolicy.awaiting(prayer,now));assertNull(HomeState(today=now.toLocalDate(),now=now,todayPlans=listOf(prayer)).hero)
  assertNull(prayer.endTime);assertEquals(PlanOutcome.Status.UNKNOWN,PlanOutcome.status(prayer))
  assertFalse(PlanReviewPolicy.awaiting(prayer.copy(startTime="19:00"),now))
  assertFalse(PlanReviewPolicy.awaiting(prayer.copy(endTime="11:00"),now))
  assertFalse(PlanReviewPolicy.awaiting(PlanOutcome.record(prayer,PlanOutcome.Status.PARTIAL,1.0,2.0),now))
 }
 @Test fun calendarDotsExcludeDailyRoutinesButSelectedDayStillIncludesThem(){
  val today=LocalDate.parse("2026-11-13");val routine=(0..30).map{Plan(id=it+1L,title="Okul",date=today.plusDays(it.toLong()).toString(),seriesId=9)}
  val appointment=Plan(id=100,title="Randevu",date=today.plusDays(2).toString())
  val state=CalendarState(selected=today,rangePlans=routine+appointment)
  assertEquals(setOf(today.plusDays(2)),state.plannedDays);assertEquals(1,state.monthCounts.values.sum());assertEquals(listOf(routine.first()),state.selectedPlans)
 }
 @Test fun searchShowsOneRoutineAndKeepsFactualOccurrenceCounts(){
  val today=LocalDate.parse("2026-11-13");val template=Plan(title="Okul",date=today.toString(),repeatType="daily");val rule=PlanSeries(id=9,template=PlanJson.encode(template).toString(),anchor=template.date)
  val rows=(0..123).map{template.copy(id=it+1L,uid="occurrence-$it",seriesId=9,date=today.plusDays(it.toLong()).toString())}
  val single=Plan(id=1000,title="Market",date=today.toString())
  val entries=SearchPresentation.entries(rows+single,listOf(rule),"",today)
  assertEquals("1 plan · 1 rutin",SearchPresentation.countLabel(entries));assertEquals(124,(entries.first() as SearchEntry.Routine).occurrences.size)
  assertEquals(1,SearchPresentation.entries(rows,listOf(rule),"Okul",today).size)
  val answer=PlanSearch.search("Kaç planım var",rows+single,today.atTime(12,0)).answer.orEmpty()
  assertTrue(answer.contains("1 tek seferlik plan ve 1 rutin"))
  val deleted=rule.copy(template=PlanJson.encode(RoutineState.deletion(template,today)).toString())
  assertTrue(SearchPresentation.entries(rows,listOf(deleted),"",today).isEmpty())
  assertEquals(1,SearchPresentation.entries(rows,listOf(deleted),"geçen ay okul",today).size)
 }
 @Test fun deletingRoutineStopsRegenerationPreservesKnownResultsAndCanBeUndone()=runBlocking{
  val context=ApplicationProvider.getApplicationContext<Context>();val db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).allowMainThreadQueries().build()
  try{
   val repo=PlanRepository(db);val day=LocalDate.parse("2026-11-13");val rows=repo.create(Plan(title="Evden çıkış",date=day.minusDays(3).toString(),repeatType="daily",extraData="{\"note\":\"Koru\"}"))
   val done=rows[0].copy(isCompleted=true);repo.update(done)
   val partial=PlanOutcome.record(rows[1],PlanOutcome.Status.PARTIAL,1.0,2.0);repo.update(partial)
   val snapshot=repo.deleteRoutine(rows.first().seriesId,day)!!
   assertEquals(done,repo.byId(done.id));assertEquals(partial,repo.byId(partial.id))
   assertTrue(repo.byId(rows[2].id)!!.isDeleted);assertEquals("Koru",JSONObject(repo.byId(rows[3].id)!!.extraData).getString("note"))
   val count=db.planDao().all().size;repo.ensure(day.plusYears(2),day.plusYears(2).plusDays(4));assertEquals(count,db.planDao().all().size)
   assertNull(repo.deleteRoutine(rows.first().seriesId,day))
   repo.undoRoutineDeletion(snapshot);assertEquals(rows[2],repo.byId(rows[2].id));assertEquals(rows[3],repo.byId(rows[3].id))
   assertEquals(done,repo.byId(done.id));assertEquals(partial,repo.byId(partial.id))
  }finally{db.close()}
 }
}
