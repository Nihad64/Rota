package com.nihad64.rotam.notification

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.nihad64.rotam.data.model.Plan
import com.nihad64.rotam.data.prefs.*
import com.nihad64.rotam.data.repository.PlanRepository
import com.nihad64.rotam.domain.usecase.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.*

class ReminderScheduler(private val context:Context,private val repo:PlanRepository,private val settings:SettingsStore,private val clock:()->Long={System.currentTimeMillis()}){
 private val alarms=context.getSystemService(AlarmManager::class.java)
 private val manager=context.getSystemService(NotificationManager::class.java)
 private val prefs=context.getSharedPreferences("rotam_alarm_state",0)
 private val lock=Mutex()
 val error=MutableStateFlow(prefs.getString("last_error",null))
 fun reportFailure(){error.value="Hatırlatmalar yeniden planlanamadı. Kayıtların korunuyor; Hatırlatmalar bölümünden yeniden dene.";prefs.edit().putString("last_error",error.value).apply()}
 fun canScheduleExact()=Build.VERSION.SDK_INT<31||alarms.canScheduleExactAlarms()
 fun notificationsAllowed()=NotificationManagerCompat.from(context).areNotificationsEnabled()&&(Build.VERSION.SDK_INT<33||ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED)&&(manager.getNotificationChannel(Notifications.CHANNEL_REMINDERS)?.importance!=NotificationManager.IMPORTANCE_NONE)
 private fun intent(id:Long,uid:String="",signature:String="",token:String="",event:String="",at:Long=0)=PendingIntent.getBroadcast(context,0,Intent(context,ReminderReceiver::class.java).setAction("show").setData(Uri.parse("rotam://reminder/$id")).putExtra("id",id).putExtra("uid",uid).putExtra("signature",signature).putExtra("eventToken",token).putExtra("event",event).putExtra("at",at),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
 private fun removeAlarm(id:Long){alarms.cancel(intent(id));prefs.edit().remove("v2_token_$id").apply()}
 fun cancel(id:Long){removeAlarm(id);manager.cancel("plan:$id",0);prefs.edit().remove("v2_snooze_$id").apply();com.nihad64.rotam.widget.TodayWidget.refresh(context)}
 private fun prepare(p:Plan){
  val signature=ReminderPolicy.signature(p)
  if(prefs.getString("v2_signature_${p.id}",null)==signature)return
  val e=prefs.edit();listOf("signature","done","muted","snooze","snoozes","shown","token","retry_at","retry_event").forEach{e.remove("v2_${it}_${p.id}")}
  e.putString("v2_signature_${p.id}",signature).apply();manager.cancel("plan:${p.id}",0)
 }
 private fun millis(t:LocalDateTime)=t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
 private fun day(now:Long)=Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate().toString()
 private fun dailyCount(now:Long)=if(prefs.getString("v2_day",null)==day(now))prefs.getInt("v2_count",0)else 0
 private data class Target(val plan:Plan,val at:Long,val key:String){val token get()="${ReminderPolicy.signature(plan)}:$key:$at"}
 private fun target(p:Plan,s:Settings,all:List<Plan>,now:Long=clock()):Target? {
  prepare(p)
  if(!s.remindersEnabled||!ReminderPolicy.enabled(p)||prefs.getBoolean("v2_muted_${p.id}",false))return null
  val automatic=ReminderPolicy.mode(p)==ReminderPolicy.Mode.AUTO
  if(automatic&&dailyCount(now)>=ReminderPolicy.MaxPerDay)return null
  val retry=prefs.getLong("v2_retry_at_${p.id}",0)
  if(retry>now)return Target(p,retry,prefs.getString("v2_retry_event_${p.id}",null)?:return null)
  val snooze=prefs.getLong("v2_snooze_${p.id}",0)
  if(snooze>now){val time=LocalDateTime.ofInstant(Instant.ofEpochMilli(snooze),ZoneId.systemDefault());return Target(p,millis(ReminderPolicy.quietAdjusted(time,s)),"snooze${prefs.getInt("v2_snoozes_${p.id}",0)}")}
  val done=prefs.getStringSet("v2_done_${p.id}",emptySet()).orEmpty()
  return ReminderPolicy.events(p,s,all).filter{it.key !in done}.map{Target(p,millis(it.at),it.key)}.firstOrNull{t->t.at>now&&!(t.key=="custom"&&prefs.getLong("delivered_${p.id}",-1)==millis(PlanTime.reminder(p)!!))}
 }
 suspend fun schedule(p:Plan)=lock.withLock{
  removeAlarm(p.id);if(!ReminderPolicy.enabled(p))manager.cancel("plan:${p.id}",0)
  if(notificationsAllowed())target(p,settings.current(),repo.upcoming(LocalDate.now().minusDays(2)))?.let{install(it)}
 }
 private fun install(target:Target){
  val p=target.plan;val pi=intent(p.id,p.uid,ReminderPolicy.signature(p),target.token,target.key,target.at)
  try{
   if(canScheduleExact())try{alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,target.at,pi)}catch(_:SecurityException){alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,target.at,pi)}
   else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,target.at,pi)
   prefs.edit().putString("v2_token_${p.id}",target.token).putStringSet("scheduled",prefs.getStringSet("scheduled",emptySet()).orEmpty()+p.id.toString()).apply()
  }catch(e:RuntimeException){reportFailure()}
 }
 fun validAction(p:Plan,uid:String?,signature:String?,token:String?)=p.uid==uid&&ReminderPolicy.signature(p)==signature&&token!=null&&prefs.getString("v2_shown_${p.id}",null)==token&&!p.isDeleted&&!p.isCompleted&&!p.isSkipped
 suspend fun snooze(p:Plan)=lock.withLock {
  val count=prefs.getInt("v2_snoozes_${p.id}",0)
  manager.cancel("plan:${p.id}",0)
  if(count>=3){prefs.edit().putBoolean("v2_muted_${p.id}",true).apply();removeAlarm(p.id);return@withLock}
  prefs.edit().putInt("v2_snoozes_${p.id}",count+1).putLong("v2_snooze_${p.id}",clock()+30*60000L).apply()
  removeAlarm(p.id);target(p,settings.current(),emptyList())?.let{install(it)}
 }
 suspend fun dismiss(p:Plan)=lock.withLock{prefs.edit().putBoolean("v2_muted_${p.id}",true).apply();cancel(p.id)}
 /** Posting and the global automatic budget share a lock, including simultaneous alarms. */
 suspend fun deliver(p:Plan,token:String?,event:String,at:Long,post:()->Unit):Boolean=lock.withLock {
  val now=clock();val s=settings.current()
  if(token==null||token!="${ReminderPolicy.signature(p)}:$event:$at"||prefs.getString("v2_token_${p.id}",null)!=token||!s.remindersEnabled||!ReminderPolicy.enabled(p)||prefs.getBoolean("v2_muted_${p.id}",false)||!notificationsAllowed())return@withLock false
  if(at>now+1000||now-at>6*3600000L)return@withLock false
  if(ReminderPolicy.quietAdjusted(LocalDateTime.ofInstant(Instant.ofEpochMilli(now),ZoneId.systemDefault()),s)!=LocalDateTime.ofInstant(Instant.ofEpochMilli(now),ZoneId.systemDefault()))return@withLock false
  val automatic=ReminderPolicy.mode(p)==ReminderPolicy.Mode.AUTO
  if(automatic&&dailyCount(now)>=ReminderPolicy.MaxPerDay)return@withLock false
  val last=prefs.getLong("v2_last_auto",0)
  if(automatic&&now-last<15*60000L){
   val deferred=millis(ReminderPolicy.quietAdjusted(LocalDateTime.ofInstant(Instant.ofEpochMilli(last+15*60000L),ZoneId.systemDefault()),s))
   prefs.edit().putLong("v2_retry_at_${p.id}",deferred).putString("v2_retry_event_${p.id}",event).apply()
   return@withLock false
  }
  post()
  val consumed=prefs.getStringSet("v2_done_${p.id}",emptySet()).orEmpty().toMutableSet();consumed.add(event)
  if(event.startsWith("snooze"))ReminderPolicy.events(p,s).filter{millis(it.at)<=now}.forEach{consumed.add(it.key)}
  val e=prefs.edit().putStringSet("v2_done_${p.id}",consumed).putString("v2_shown_${p.id}",token).remove("v2_token_${p.id}").remove("v2_snooze_${p.id}").remove("v2_retry_at_${p.id}").remove("v2_retry_event_${p.id}")
  if(automatic)e.putLong("v2_last_auto",now).putString("v2_day",day(now)).putInt("v2_count",dailyCount(now)+1)
  e.apply();true
 }
 suspend fun resetState()=lock.withLock{
  prefs.getStringSet("scheduled",emptySet()).orEmpty().mapNotNull{it.toLongOrNull()}.forEach{cancel(it)}
  prefs.edit().clear().commit();error.value=null
 }
 suspend fun rescheduleAll()=lock.withLock{
  com.nihad64.rotam.widget.TodayWidget.refresh(context)
  val today=Instant.ofEpochMilli(clock()).atZone(ZoneId.systemDefault()).toLocalDate();repo.ensure(today,today.plusDays(400));val s=settings.current()
  val pending=repo.pendingBetween(LocalDate.of(1970,1,1),today.plusDays(400))
  val byId=pending.associateBy{it.id}
  prefs.getStringSet("scheduled",emptySet()).orEmpty().mapNotNull{it.toLongOrNull()}.forEach{id->removeAlarm(id);if(byId[id]?.let{!ReminderPolicy.enabled(it)}!=false)manager.cancel("plan:$id",0)}
  prefs.edit().putStringSet("scheduled",emptySet()).apply()
  if(!s.remindersEnabled||!notificationsAllowed())return@withLock
  val until=clock()+14*86400000L
  pending.mapNotNull{target(it,s,pending)}.filter{it.at<=until}.sortedBy{it.at}.take(100).forEach{install(it)}
  if(error.value==null)prefs.edit().remove("last_error").apply()
 }
}
