package com.nihad64.rotam
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nihad64.rotam.data.db.AppDatabase
import com.nihad64.rotam.data.model.*
import com.nihad64.rotam.data.repository.PlanRepository
import com.nihad64.rotam.domain.usecase.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=android.app.Application::class)
class RoutinePauseTest {
 @Test fun pauseAndResumePreserveCompletedPartialAndManuallyDeletedExceptions()=runBlocking {
  val context=ApplicationProvider.getApplicationContext<Context>();val db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).allowMainThreadQueries().build()
  try{
   val repo=PlanRepository(db);val start=LocalDate.parse("2026-11-11")
   val list=repo.create(Plan(title="Oku",date=start.toString(),repeatType="daily",extraData="{\"note\":\"Korunacak\"}"))
   val byDate=list.associateBy{it.date};val series=list.first().seriesId
   val done=byDate.getValue(start.toString()).copy(isCompleted=true);repo.update(done)
   val partial=PlanOutcome.record(byDate.getValue(start.plusDays(1).toString()),PlanOutcome.Status.PARTIAL,2.0,5.0);repo.update(partial)
   val deleted=byDate.getValue(start.plusDays(2).toString());repo.delete(deleted)
   val future=byDate.getValue(start.plusDays(3).toString())
   repo.setRoutinePaused(series,true,start)
   assertEquals(done,repo.byId(done.id));assertEquals(partial,repo.byId(partial.id));assertTrue(repo.byId(future.id)!!.isDeleted)
   repo.setRoutinePaused(series,false,start.plusDays(1))
   assertEquals(done,repo.byId(done.id));assertEquals(partial,repo.byId(partial.id));assertTrue(repo.byId(deleted.id)!!.isDeleted)
   assertFalse(repo.byId(future.id)!!.isDeleted);assertEquals(future.uid,repo.byId(future.id)!!.uid)
  }finally{db.close()}
 }
 @Test fun pausedRoutinesDoNotGenerateNewFutureRowsAndGapDoesNotReturnAsFailure()=runBlocking {
  val context=ApplicationProvider.getApplicationContext<Context>();val db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).allowMainThreadQueries().build()
  try{
   val repo=PlanRepository(db);val day=LocalDate.parse("2026-11-11")
   val list=repo.create(Plan(title="Spor",date=day.toString(),repeatType="daily"));val sid=list.first().seriesId
   repo.setRoutinePaused(sid,true,day);val count=db.planDao().all().size
   repo.ensure(day.plusYears(5),day.plusYears(5).plusDays(10));assertEquals(count,db.planDao().all().size)
   repo.setRoutinePaused(sid,true,day);assertEquals(count,db.planDao().all().size)
   repo.setRoutinePaused(sid,false,day.plusDays(3))
   assertTrue(list.filter{it.date<day.plusDays(3).toString()}.all{repo.byId(it.id)!!.isDeleted})
   assertFalse(repo.byId(list.first{it.date==day.plusDays(3).toString()}.id)!!.isDeleted)
  }finally{db.close()}
 }
}
