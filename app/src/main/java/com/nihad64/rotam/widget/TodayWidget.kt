package com.nihad64.rotam.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.nihad64.rotam.*
import com.nihad64.rotam.data.model.Plan
import com.nihad64.rotam.domain.usecase.PlanTime
import kotlinx.coroutines.*
import java.time.LocalDate
import java.time.LocalDateTime

/** Keep the old provider identity so installed medium widgets survive updates. */
open class TodayWidget:AppWidgetProvider() {
 override fun onUpdate(context:Context,manager:AppWidgetManager,ids:IntArray) {}
 override fun onAppWidgetOptionsChanged(context:Context,manager:AppWidgetManager,id:Int,options:Bundle){}
 override fun onReceive(context:Context,intent:Intent) {
  super.onReceive(context,intent)
  if(intent.action!=Done&&intent.action?.startsWith("android.appwidget.")!=true)return
  val pending=goAsync()
  CoroutineScope(SupervisorJob()+Dispatchers.IO).launch {
   try {
    val c=RotamApp.container;c.initialize()
    val p=c.planRepo.byId(intent.getLongExtra("planId",-1))
    // Stale widgets must never complete a different record after restoring a backup.
    if(p!=null&&p.uid==intent.getStringExtra("planUid")&&!p.isDeleted&&!p.isCompleted&&!p.isSkipped)c.completePlan(p,true)
    updateAll(context)
   }catch(e:CancellationException){throw e}catch(e:Exception){android.util.Log.e("RotamWidget","Widget action failed: ${e.javaClass.simpleName}")}
   finally {pending.finish()}
  }
 }
 companion object {
  private const val Done="com.rota.app.WIDGET_DONE"
  fun refresh(context:Context){CoroutineScope(SupervisorJob()+Dispatchers.IO).launch{try{updateAll(context)}catch(e:CancellationException){throw e}catch(e:Exception){android.util.Log.e("RotamWidget","Widget refresh failed: ${e.javaClass.simpleName}")}}}
  suspend fun updateAll(context:Context) {
   val manager=AppWidgetManager.getInstance(context)
   val groups=listOf(TodayWidget::class.java to R.layout.widget_today,SmallTodayWidget::class.java to R.layout.widget_small,LargeTodayWidget::class.java to R.layout.widget_large)
   val installed=groups.map{(provider,layout)->manager.getAppWidgetIds(ComponentName(context,provider)) to layout}
   if(installed.all{it.first.isEmpty()})return
   val c=RotamApp.container;c.initialize();c.planRepo.range(LocalDate.now(),LocalDate.now())
   val snapshot=WidgetSnapshot.build(c.db.planDao().all(),LocalDateTime.now())
   fun open(action:String,p:Plan?=null)=PendingIntent.getActivity(context,0,Intent(context,MainActivity::class.java)
    .setData(Uri.parse("rotam://widget/$action/${p?.uid?:"home"}"))
    .putExtra("planId",p?.id?:0).putExtra("planUid",p?.uid).putExtra("newPlan",action=="new"),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
   fun done(p:Plan)=PendingIntent.getBroadcast(context,0,Intent(context,TodayWidget::class.java).setAction(Done)
    .setData(Uri.parse("rotam://widget/done/${p.uid}")).putExtra("planId",p.id).putExtra("planUid",p.uid),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
   val next=snapshot.pending.firstOrNull()
   for((ids,layout)in installed)for(id in ids) {
    val views=RemoteViews(context.packageName,layout)
    views.setTextViewText(R.id.widget_progress,"${snapshot.done} / ${snapshot.total}")
    views.setProgressBar(R.id.widget_bar,100,snapshot.progress,false)
    views.setTextViewText(R.id.widget_title,next?.title?:if(snapshot.total>0)"Bugün için bekleyen plan yok."else"Gününe yer aç.")
    views.setTextViewText(R.id.widget_time,next?.let{PlanTime.label(it)}?:"Yeni bir plan ekleyebilirsin.")
    views.setOnClickPendingIntent(R.id.widget_body,open("open",next))
    views.setOnClickPendingIntent(R.id.widget_add,open("new"))
    views.setContentDescription(R.id.widget_add,"AI ile plan ekle")
    views.setViewVisibility(R.id.widget_done,View.GONE)
    if(next!=null)views.setOnClickPendingIntent(R.id.widget_done,done(next))
    views.setTextViewText(R.id.widget_awaiting,"${snapshot.awaiting} plan yanıtını bekliyor")
    views.setViewVisibility(R.id.widget_awaiting,if(snapshot.awaiting>0)View.VISIBLE else View.GONE)
    views.setOnClickPendingIntent(R.id.widget_awaiting,open("review"))
    if(layout!=R.layout.widget_small){
     views.removeAllViews(R.id.widget_rows)
     val requestedHeight=manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,if(layout==R.layout.widget_large)300 else 180)
     val height=if(requestedHeight>0)requestedHeight else if(layout==R.layout.widget_large)300 else 180
     val available=height-110-if(snapshot.awaiting>0)36 else 0
     val maxRows=(available/56).coerceIn(1,if(layout==R.layout.widget_large)6 else 3)
     for(p in snapshot.pending.take(maxRows)) {
      val row=RemoteViews(context.packageName,R.layout.widget_plan_row)
      row.setTextViewText(R.id.widget_row_title,p.title)
      row.setTextViewText(R.id.widget_row_time,if(p.startTime==null&&p.endTime==null)"Gün içinde"else PlanTime.label(p))
      row.setOnClickPendingIntent(R.id.widget_row_body,open("open",p))
      row.setOnClickPendingIntent(R.id.widget_row_done,done(p))
      row.setContentDescription(R.id.widget_row_done,"${p.title} planını tamamla")
      views.addView(R.id.widget_rows,row)
     }
     views.setViewVisibility(R.id.widget_rows,if(next!=null)View.VISIBLE else View.GONE)
     views.setViewVisibility(R.id.widget_title,if(next==null)View.VISIBLE else View.GONE)
     views.setViewVisibility(R.id.widget_time,if(next==null)View.VISIBLE else View.GONE)
    }
    manager.updateAppWidget(id,views)
   }
  }
 }
}
class SmallTodayWidget:TodayWidget()
class LargeTodayWidget:TodayWidget()
