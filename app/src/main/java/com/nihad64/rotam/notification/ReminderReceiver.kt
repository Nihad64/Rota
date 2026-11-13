package com.nihad64.rotam.notification

import android.app.*
import android.content.*
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.nihad64.rotam.*
import com.nihad64.rotam.domain.usecase.ReminderPolicy
import kotlinx.coroutines.*

class ReminderReceiver:BroadcastReceiver(){
 override fun onReceive(context:Context,intent:Intent){val pending=goAsync();CoroutineScope(SupervisorJob()+Dispatchers.IO).launch{try{
  val c=RotamApp.container;c.initialize();val id=intent.getLongExtra("id",-1);val p=c.planRepo.byId(id)?:return@launch
  if(p.isDeleted||p.uid!=intent.getStringExtra("uid")||ReminderPolicy.signature(p)!=intent.getStringExtra("signature"))return@launch
  val token=intent.getStringExtra("eventToken")
  if(intent.action!="show"&&!c.scheduler.validAction(p,intent.getStringExtra("uid"),intent.getStringExtra("signature"),token))return@launch
  when(intent.action){
   "done"->c.completePlan(p,true)
   "started"->{c.planRepo.setStarted(p.id,p.uid);c.scheduler.cancel(p.id)}
   "snooze"->c.scheduler.snooze(p)
   "dismiss"->c.scheduler.dismiss(p)
   "show"->{
    val event=intent.getStringExtra("event").orEmpty()
    fun action(name:String)=PendingIntent.getBroadcast(context,0,Intent(context,ReminderReceiver::class.java).setAction(name).setData(Uri.parse("rotam://$name/$id")).putExtra("id",id).putExtra("uid",p.uid).putExtra("signature",ReminderPolicy.signature(p)).putExtra("eventToken",token),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val open=PendingIntent.getActivity(context,0,Intent(context,MainActivity::class.java).setData(Uri.parse("rotam://plan/$id")).putExtra("planId",id),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val body=ReminderPolicy.body(p,event)
    val n=NotificationCompat.Builder(context,Notifications.CHANNEL_REMINDERS).setSmallIcon(R.drawable.ic_notification).setContentTitle(p.title).setContentText(body).setStyle(NotificationCompat.BigTextStyle().bigText(body)).setContentIntent(open).setDeleteIntent(action("dismiss")).setAutoCancel(true).setOnlyAlertOnce(false).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setCategory(NotificationCompat.CATEGORY_REMINDER).addAction(0,"Başladım",action("started")).addAction(0,"Tamamladım",action("done")).addAction(0,"Biraz sonra",action("snooze")).build()
    c.scheduler.deliver(p,token,event,intent.getLongExtra("at",0)){context.getSystemService(NotificationManager::class.java).notify("plan:$id",0,n)}
   }
  }
  c.scheduler.rescheduleAll()
 }catch(e:CancellationException){throw e}catch(_:Exception){RotamApp.container.scheduler.reportFailure();MissedPlanManager.enqueue(context)}finally{pending.finish()}}}
}
class BootReceiver:BroadcastReceiver(){override fun onReceive(context:Context,intent:Intent){val pending=goAsync();CoroutineScope(SupervisorJob()+Dispatchers.IO).launch{try{RotamApp.container.initialize();RotamApp.container.scheduler.rescheduleAll();MissedPlanManager.enqueue(context)}catch(e:CancellationException){throw e}catch(_:Exception){RotamApp.container.scheduler.reportFailure()}finally{pending.finish()}}}}
