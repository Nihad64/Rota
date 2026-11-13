package com.nihad64.rotam

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nihad64.rotam.data.db.AppDatabase
import com.nihad64.rotam.data.model.Plan
import com.nihad64.rotam.data.repository.PlanRepository
import com.nihad64.rotam.domain.usecase.FocusTracker
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=android.app.Application::class)
class FocusSessionTest {
 @Test fun breaksNeverInflateFocusAndSummarySurvivesProcessRecreation()=runBlocking {
  val ctx=ApplicationProvider.getApplicationContext<Context>()
  ctx.getSharedPreferences("rotam_focus",0).edit().clear().commit()
  ctx.getSharedPreferences("rotam_focus_session",0).edit().clear().commit()
  val db=Room.inMemoryDatabaseBuilder(ctx,AppDatabase::class.java).allowMainThreadQueries().build()
  try {
   val repo=PlanRepository(db);val p=repo.create(Plan(title="Oku",date="2026-11-11",focusSeconds=120)).single()
   var clock=1000L
   val tracker=FocusTracker(ctx,repo){clock};tracker.start(p.id)
   clock+=20000;tracker.pause();clock+=300000
   val reopened=FocusTracker(ctx,repo){clock}
   assertEquals(20,reopened.sessionSeconds(p.id));assertEquals(300,reopened.breakSeconds(p.id))
   reopened.start(p.id);clock+=40000
   val result=reopened.finish(p.id)
   assertEquals(60,result.focusSeconds);assertEquals(300,result.breakSeconds)
   assertEquals(180,repo.byId(p.id)!!.focusSeconds);assertFalse(repo.byId(p.id)!!.isCompleted)
   val third=FocusTracker(ctx,repo){clock};assertEquals(result,third.summary(p.id));assertNull(third.active.value)
   third.dismissSummary(p.id);assertNull(third.summary(p.id))
  } finally {db.close()}
 }
 @Test fun finishingAnOldScreenCannotDestroyAnotherPlansActiveSession()=runBlocking {
  val ctx=ApplicationProvider.getApplicationContext<Context>()
  ctx.getSharedPreferences("rotam_focus",0).edit().clear().commit();ctx.getSharedPreferences("rotam_focus_session",0).edit().clear().commit()
  val db=Room.inMemoryDatabaseBuilder(ctx,AppDatabase::class.java).allowMainThreadQueries().build()
  try {
   val repo=PlanRepository(db);val a=repo.create(Plan(title="A",date="2026-11-11")).single();val b=repo.create(Plan(title="B",date=a.date)).single()
   var clock=1000L;val tracker=FocusTracker(ctx,repo){clock}
   tracker.start(a.id);clock+=10000;tracker.start(b.id);clock+=15000
   try{tracker.finish(a.id);fail()}catch(_:IllegalStateException){}
   assertEquals(b.id,tracker.active.value);assertEquals(15,tracker.sessionSeconds(b.id));assertEquals(10,repo.byId(a.id)!!.focusSeconds)
  } finally {db.close()}
 }
}
