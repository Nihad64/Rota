package com.nihad64.rotam

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nihad64.rotam.data.db.AppDatabase
import com.nihad64.rotam.data.model.Plan
import com.nihad64.rotam.data.prefs.SettingsStore
import com.nihad64.rotam.data.repository.PlanRepository
import com.nihad64.rotam.domain.usecase.*
import com.nihad64.rotam.notification.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=android.app.Application::class)
class ReminderSchedulerTest {
 private fun at(value:String)=LocalDateTime.parse(value).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
 private class Fixture {
  val ctx=ApplicationProvider.getApplicationContext<Context>()
  val db=Room.inMemoryDatabaseBuilder(ctx,AppDatabase::class.java).allowMainThreadQueries().build()
  val repo=PlanRepository(db);val settings=SettingsStore(ctx)
  var clock=LocalDateTime.parse("2026-11-12T18:00").atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
  val scheduler=ReminderScheduler(ctx,repo,settings){clock};val alarms=shadowOf(ctx.getSystemService(AlarmManager::class.java))
  suspend fun init(){ctx.getSharedPreferences("rotam_alarm_state",0).edit().clear().commit();settings.setQuiet(false);Notifications.createChannels(ctx)}
  fun alarm(id:Long):Intent=Intent(shadowOf(alarms.scheduledAlarms.first{shadowOf(it.operation).savedIntent.getLongExtra("id",0)==id}.operation).savedIntent)
  suspend fun post(p:Plan,i:Intent,onPost:()->Unit={})=scheduler.deliver(p,i.getStringExtra("eventToken"),i.getStringExtra("event")!!,i.getLongExtra("at",0),onPost)
 }
 @Test fun dismissalStopsFollowUpsWithoutMarkingFailureAndOldActionsCannotTouchAnotherRecord()=runBlocking {
  val f=Fixture();try{
   f.init();val p=f.repo.create(ReminderPolicy.withMode(Plan(title="Oku",date="2026-11-12"),ReminderPolicy.Mode.AUTO)).single()
   f.scheduler.schedule(p);val notice=f.alarm(p.id);f.clock=notice.getLongExtra("at",0);assertFalse(f.scheduler.deliver(p,notice.getStringExtra("eventToken"),"tampered",notice.getLongExtra("at",0)){fail()});assertTrue(f.post(p,notice))
   assertTrue(f.scheduler.validAction(p,p.uid,notice.getStringExtra("signature"),notice.getStringExtra("eventToken")))
   assertFalse(f.scheduler.validAction(p.copy(uid="another-row"),p.uid,notice.getStringExtra("signature"),notice.getStringExtra("eventToken")))
   assertFalse(f.scheduler.validAction(p.copy(date="2026-11-13"),p.uid,notice.getStringExtra("signature"),notice.getStringExtra("eventToken")))
   f.scheduler.dismiss(p);f.scheduler.rescheduleAll();assertTrue(f.alarms.scheduledAlarms.isEmpty())
   assertEquals(PlanOutcome.Status.UNKNOWN,PlanOutcome.status(f.repo.byId(p.id)!!))
  }finally{f.db.close()}
 }
 @Test fun simultaneousAutomaticAlertsSpreadOutAndGlobalDailyBudgetSurvivesRecreation()=runBlocking {
  val f=Fixture();try{
   f.init();val plans=(1..7).map{f.repo.create(ReminderPolicy.withMode(Plan(title="Plan $it",date="2026-11-12"),ReminderPolicy.Mode.AUTO)).single()}
   f.scheduler.rescheduleAll();f.clock=at("2026-11-12T19:00");assertTrue(f.post(plans[0],f.alarm(plans[0].id)))
   assertFalse(f.post(plans[1],f.alarm(plans[1].id)))
   f.scheduler.rescheduleAll();assertEquals(at("2026-11-12T19:15"),f.alarm(plans[1].id).getLongExtra("at",0))
   f.clock=at("2026-11-12T19:15");assertTrue(f.post(plans[1],f.alarm(plans[1].id)))
   for(n in 0..3){
    val start=LocalTime.of(21,0).plusMinutes(n*30L)
    val p=f.repo.create(ReminderPolicy.withMode(Plan(title="Sonraki $n",date="2026-11-12",startTime=start.toString()),ReminderPolicy.Mode.AUTO)).single()
    f.scheduler.schedule(p);val i=f.alarm(p.id);f.clock=i.getLongExtra("at",0);assertTrue(f.post(p,i))
   }
   val last=f.repo.create(ReminderPolicy.withMode(Plan(title="Günlük sınır",date="2026-11-12",startTime="23:30"),ReminderPolicy.Mode.AUTO)).single()
   val reopened=ReminderScheduler(f.ctx,f.repo,f.settings){f.clock}
   reopened.schedule(last)
   assertFalse(f.alarms.scheduledAlarms.any{shadowOf(it.operation).savedIntent.getLongExtra("id",0)==last.id})
  }finally{f.db.close()}
 }
 @Test fun startedPlanAndCompletionCancelAlarmsAndSnoozeDoesNotMoveSchedule()=runBlocking {
  val f=Fixture();try{
   f.init();val p=f.repo.create(Plan(title="Özel",date="2026-11-12",startTime="19:00",reminderEnabled=true,reminderMinutes=0)).single()
   f.scheduler.schedule(p);val i=f.alarm(p.id);f.clock=at("2026-11-12T19:00");assertTrue(f.post(p,i))
   f.scheduler.snooze(p);assertEquals(at("2026-11-12T19:30"),f.alarm(p.id).getLongExtra("at",0));assertEquals("19:00",f.repo.byId(p.id)!!.startTime)
   f.repo.setStarted(p.id,p.uid);f.scheduler.rescheduleAll();assertTrue(f.alarms.scheduledAlarms.isEmpty())
   assertFalse(f.post(f.repo.byId(p.id)!!,i))
  }finally{f.db.close()}
 }
}
