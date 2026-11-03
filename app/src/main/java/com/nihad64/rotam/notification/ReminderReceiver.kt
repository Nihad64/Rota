package com.nihad64.rotam.notification
import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.nihad64.rotam.*
import com.nihad64.rotam.domain.usecase.PlanTime
import kotlinx.coroutines.*
class ReminderReceiver:BroadcastReceiver(){
 override fun onReceive(context:Context,intent:Intent){val pending=goAsync();CoroutineScope(SupervisorJob()+Dispatchers.IO).launch{try{
  val c=RotamApp.container;c.initialize();val id=intent.getLongExtra("id",-1);val p=c.planRepo.byId(id)?:return@launch
  if(p.isDeleted)return@launch
  when(intent.action){
   "done"->c.completePlan(p,true);"skip"->c.skipPlan(p);"snooze"->c.scheduler.snooze(id,30)
   "show"->{val token=intent.getLongExtra("token",-1);if(!c.scheduler.valid(id,token)||p.isCompleted||p.isSkipped||!c.settings.current().remindersEnabled)return@launch
    if(Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return@launch
    val open=PendingIntent.getActivity(context,0,Intent(context,MainActivity::class.java).setData(Uri.parse("rotam://plan/$id")).putExtra("planId",id),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    fun action(name:String)=PendingIntent.getBroadcast(context,0,Intent(context,ReminderReceiver::class.java).setAction(name).setData(Uri.parse("rotam://$name/$id")).putExtra("id",id),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val n=NotificationCompat.Builder(context,Notifications.CHANNEL_REMINDERS).setSmallIcon(R.drawable.ic_notification).setContentTitle(p.title).setContentText(PlanTime.label(p)).setContentIntent(open).setAutoCancel(true).addAction(0,"Yaptım",action("done")).addAction(0,"30 dk ertele",action("snooze")).addAction(0,"Atla",action("skip")).build()
    context.getSystemService(NotificationManager::class.java).notify("plan:$id",0,n);c.scheduler.delivered(id,token)
   }
  }
 }catch(_:Exception){/* Keep stored tasks; next periodic maintenance retries scheduling. */}finally{pending.finish()}}}
}
class BootReceiver:BroadcastReceiver(){override fun onReceive(context:Context,intent:Intent){val pending=goAsync();CoroutineScope(SupervisorJob()+Dispatchers.IO).launch{try{RotamApp.container.initialize();RotamApp.container.scheduler.rescheduleAll();MissedPlanManager.enqueue(context)}catch(_:Exception){}finally{pending.finish()}}}}
