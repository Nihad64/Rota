package com.nihad64.rotam
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nihad64.rotam.data.db.*
import com.nihad64.rotam.data.model.*
import com.nihad64.rotam.data.repository.*
import com.nihad64.rotam.data.backup.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28], application=android.app.Application::class)
class RepositoryTest {
 private lateinit var db:AppDatabase
 private lateinit var repo:PlanRepository
 private val context:Context get()=ApplicationProvider.getApplicationContext()
 @Before fun setup(){db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).allowMainThreadQueries().build();repo=PlanRepository(db)}
 @After fun close(){db.close()}
 @Test fun focusSurvivesTrackerRecreationAndPreservesConcurrentPlanChanges()=runBlocking {
  context.getSharedPreferences("rotam_focus",0).edit().clear().commit()
  val p=repo.create(Plan(title="Odak",date="2026-11-02")).single()
  val tracker=com.nihad64.rotam.domain.usecase.FocusTracker(context,repo)
  tracker.start(p.id)
  org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofSeconds(65))
  repo.update(p.copy(title="Yeni başlık"))
  val reopened=com.nihad64.rotam.domain.usecase.FocusTracker(context,repo)
  assertEquals(p.id,reopened.active.value)
  reopened.pause()
  assertTrue(repo.byId(p.id)!!.focusSeconds>=65)
  assertEquals("Yeni başlık",repo.byId(p.id)!!.title)
  assertNull(reopened.active.value)
 }
 @Test fun deletingGoalClearsLinksInOccurrencesAndFutureTemplates()=runBlocking {
  val goals=GoalRepository(db)
  val goal=Goal(title="Kitap",period="weekly",targetCount=3,dueDate="2026-11-09")
  val id=goals.create(goal)
  repo.create(Plan(title="Oku",date="2026-11-02",repeatType="daily",goalId=id))
  goals.delete(goal.copy(id=id))
  repo.ensure(LocalDate.parse("2028-01-01"),LocalDate.parse("2028-01-02"))
  assertTrue(db.planDao().all().all{it.goalId==null})
  BackupCodec.decode(BackupStore(context,db).export())
  Unit
 }
 @Test fun oneDaysPartialHistoryDoesNotLeakIntoFutureOccurrences()=runBlocking {
  val list=repo.create(Plan(title="Oku",date="2026-11-02",repeatType="daily"))
  val old=list.first();val partial=com.nihad64.rotam.domain.usecase.PlanOutcome.record(old,com.nihad64.rotam.domain.usecase.PlanOutcome.Status.PARTIAL,10.0,20.0)
  repo.update(partial)
  repo.edit(partial,partial.copy(title="Yeni rutin"),EditScope.FUTURE)
  val future=repo.range(LocalDate.parse("2026-11-03"),LocalDate.parse("2026-11-03")).single()
  assertEquals(com.nihad64.rotam.domain.usecase.PlanOutcome.Status.UNKNOWN,com.nihad64.rotam.domain.usecase.PlanOutcome.status(future))
  assertFalse(future.extraData.contains("outcomeHistory"))
  assertFalse(repo.byId(old.id)!!.isDeleted)
  assertEquals(com.nihad64.rotam.domain.usecase.PlanOutcome.Status.PARTIAL,com.nihad64.rotam.domain.usecase.PlanOutcome.status(repo.byId(old.id)!!))
 }
 @Test fun retryingARecurringSuggestionDoesNotCreateAnotherSeries()=runBlocking {
  val p=Plan(title="Tek öneri",date="2026-11-02",repeatType="weekly")
  val first=repo.create(p);val retry=repo.create(p)
  assertEquals(first.map{it.id},retry.map{it.id});assertEquals(1,db.seriesDao().all().size)
 }
 @Test fun generatedOccurrencesAreIdempotentAndDeletedDaysStayDeleted()=runBlocking {
  val p=Plan(title="Oku",date="2026-11-02",repeatType="daily")
  val created=repo.create(p);assertTrue(created.size>60)
  val first=created.first();repo.delete(first)
  repo.ensure(LocalDate.parse(p.date),LocalDate.parse(p.date).plusDays(700))
  assertTrue(repo.byId(first.id)!!.isDeleted)
  val all=db.planDao().all();assertEquals(all.size,all.map{it.occurrenceKey}.distinct().size)
  assertEquals(1,repo.range(LocalDate.parse(p.date).plusDays(700),LocalDate.parse(p.date).plusDays(700)).size)
 }
 @Test fun editingOneOccurrenceDoesNotChangeItsSiblings()=runBlocking {
  val list=repo.create(Plan(title="Spor",date="2026-11-02",repeatType="daily"));val first=list.first();val second=list[1]
  repo.edit(first,first.copy(endTime="18:00"),EditScope.THIS)
  assertNull(repo.byId(second.id)!!.endTime);assertEquals("18:00",repo.byId(first.id)!!.endTime)
 }
 @Test fun backupRestoresGoalsAndSeriesWithoutDuplicates()=runBlocking {
  val gid=db.goalDao().insert(Goal(title="Oku",period="weekly",targetCount=3,dueDate="2026-11-09"))
  repo.create(Plan(title="Kitap",date="2026-11-02",repeatType="weekly",goalId=gid))
  val store=BackupStore(context,db);val data=BackupCodec.decode(store.export())
  store.restore(data);store.restore(data)
  assertEquals(data.plans.size,db.planDao().all().size);assertEquals(1,db.goalDao().all().size);assertEquals(1,db.seriesDao().all().size)
 }
 @Test fun legacyMigrationCanRetryWithoutDuplicatingData()=runBlocking {
  context.getSharedPreferences("rotam_migrations",0).edit().clear().commit()
  context.getSharedPreferences("rota_store",0).edit().putString("plans","""[{"id":5,"title":"Önceki iş","date":"2026-10-10","type":"Saatsiz","note":"notum"}]""").commit()
  val migration=LegacyMigration(context,db);assertEquals(1,migration.run());assertEquals(0,migration.run());assertEquals(1,db.planDao().all().size)
  assertTrue(db.planDao().all()[0].extraData.contains("notum"))
 }
}
