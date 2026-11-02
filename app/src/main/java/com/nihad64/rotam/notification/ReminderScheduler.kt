package com.nihad64.rotam.notification
import android.app.*
import android.content.*
import android.net.Uri
import android.os.Build
import com.nihad64.rotam.data.model.Plan
import com.nihad64.rotam.data.prefs.*
import com.nihad64.rotam.data.repository.PlanRepository
import com.nihad64.rotam.domain.usecase.PlanTime
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.*
class ReminderScheduler(private val context:Context,private val repo:PlanRepository,private val settings:SettingsStore){
 private val alarms=context.getSystemService(AlarmManager::class.java)
 private val prefs=context.getSharedPreferences("rotam_alarm_state",0)
 private val lock=Mutex()
 fun canScheduleExact()=Build.VERSION.SDK_INT<31||alarms.canScheduleExactAlarms()
 private fun intent(id:Long,token:Long=0)=PendingIntent.getBroadcast(context,0,Intent(context,ReminderReceiver::class.java).setAction("show").setData(Uri.parse("rotam://reminder/$id")).putExtra("id",id).putExtra("token",token),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
 private fun remove(id:Long){alarms.cancel(intent(id));context.getSystemService(NotificationManager::class.java).cancel("plan:$id",0)}
 fun cancel(id:Long){remove(id);com.nihad64.rotam.widget.TodayWidget.refresh(context);prefs.edit().remove("snooze_$id").remove("token_$id").apply()}
 private fun quietAdjusted(time:LocalDateTime,s:Settings):LocalDateTime{
  if(!s.quietEnabled||s.quietStart==s.quietEnd)return time
  val m=time.hour*60+time.minute;val quiet=if(s.quietStart<s.quietEnd)m>=s.quietStart&&m<s.quietEnd else m>=s.quietStart||m<s.quietEnd
  if(!quiet)return time
  val day=if(s.quietStart>s.quietEnd&&m>=s.quietStart)time.toLocalDate().plusDays(1)else time.toLocalDate()
  return day.atStartOfDay().plusMinutes(s.quietEnd.toLong())
 }
 private fun millis(t:LocalDateTime)=t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
 private fun target(p:Plan,s:Settings):Pair<Long,Long>?{
  if(!s.remindersEnabled||p.isCompleted||p.isSkipped||p.isDeleted||!p.reminderEnabled)return null
  val base=PlanTime.reminder(p)?:return null;val token=millis(base)
  val snooze=prefs.getLong("snooze_${p.id}",0)
  if(snooze>System.currentTimeMillis())return millis(quietAdjusted(LocalDateTime.ofInstant(Instant.ofEpochMilli(snooze),ZoneId.systemDefault()),s)) to token
  if(prefs.getLong("delivered_${p.id}",-1)==token)return null
  val at=millis(quietAdjusted(base,s));if(at<=System.currentTimeMillis())return null
  return at to token
 }
 suspend fun schedule(p:Plan)=lock.withLock{remove(p.id);prefs.edit().remove("token_${p.id}").apply();target(p,settings.current())?.let{install(p.id,it.first,it.second)}}
 private fun install(id:Long,at:Long,token:Long){
  val pi=intent(id,token);prefs.edit().putLong("token_$id",token).apply()
  try{if(canScheduleExact())alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi)else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi)}catch(_:SecurityException){alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi)}
  val ids=prefs.getStringSet("scheduled",emptySet()).orEmpty().toMutableSet();ids.add(id.toString());prefs.edit().putStringSet("scheduled",ids).apply()
 }
 suspend fun snooze(id:Long,minutes:Int=30){val p=repo.byId(id)?:return;prefs.edit().putLong("snooze_$id",System.currentTimeMillis()+minutes*60000L).apply();schedule(p)}
 fun valid(id:Long,token:Long)=prefs.getLong("token_$id",-1)==token
 fun delivered(id:Long,token:Long){prefs.edit().putLong("delivered_$id",token).remove("snooze_$id").remove("token_$id").apply()}
 suspend fun resetState()=lock.withLock{
  prefs.getStringSet("scheduled",emptySet()).orEmpty().mapNotNull{it.toLongOrNull()}.forEach{remove(it)}
  prefs.edit().clear().commit()
 }
 suspend fun rescheduleAll()=lock.withLock{
  com.nihad64.rotam.widget.TodayWidget.refresh(context)
  val today=LocalDate.now();repo.ensure(today,today.plusDays(400));val s=settings.current()
  prefs.getStringSet("scheduled",emptySet()).orEmpty().mapNotNull{it.toLongOrNull()}.forEach{remove(it);prefs.edit().remove("token_$it").apply()}
  prefs.edit().putStringSet("scheduled",emptySet()).apply()
  if(!s.remindersEnabled)return@withLock
  val until=System.currentTimeMillis()+14*86400000L
  repo.pendingBetween(LocalDate.of(1970,1,1),today.plusDays(400)).mapNotNull{p->target(p,s)?.let{Triple(p.id,it.first,it.second)}}.filter{it.second<=until}.sortedBy{it.second}.take(100).forEach{install(it.first,it.second,it.third)}
 }
}
