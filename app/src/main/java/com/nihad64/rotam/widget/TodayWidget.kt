package com.nihad64.rotam.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.nihad64.rotam.*
import com.nihad64.rotam.domain.usecase.PlanTime
import kotlinx.coroutines.*
import java.time.LocalDate

class TodayWidget:AppWidgetProvider() {
    override fun onUpdate(context:Context,manager:AppWidgetManager,ids:IntArray) {}
    override fun onReceive(context:Context,intent:Intent) {
        super.onReceive(context,intent)
        if(intent.action!="com.rota.app.WIDGET_DONE" && intent.action!=AppWidgetManager.ACTION_APPWIDGET_UPDATE)return
        val pending=goAsync()
        CoroutineScope(SupervisorJob()+Dispatchers.IO).launch {
            try {val c=RotamApp.container;c.initialize();if(intent.action=="com.rota.app.WIDGET_DONE"){val p=c.planRepo.byId(intent.getLongExtra("planId",-1));if(p!=null&&!p.isDeleted)c.completePlan(p,true)};updateAll(context)} catch(_:Exception) { /* The next refresh retries. */ } finally {pending.finish()}
        }
    }
    companion object {
        fun refresh(context:Context) { CoroutineScope(SupervisorJob()+Dispatchers.IO).launch {runCatching{updateAll(context)}} }
        suspend fun updateAll(context:Context) {
            val manager=AppWidgetManager.getInstance(context)
            val ids=manager.getAppWidgetIds(ComponentName(context,TodayWidget::class.java));if(ids.isEmpty())return
            val c=RotamApp.container;c.initialize()
            val plans=c.planRepo.range(LocalDate.now(),LocalDate.now())
            val next=plans.filter{!it.isCompleted&&!it.isSkipped}.minByOrNull{PlanTime.start(it)?:PlanTime.end(it)?:LocalDate.now().plusDays(1).atStartOfDay()}
            fun open(action:String,id:Long=0)=PendingIntent.getActivity(context,0,Intent(context,MainActivity::class.java).setData(Uri.parse("rotam://widget/$action/$id")).putExtra("planId",id).putExtra("newPlan",action=="new"),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            for(id in ids) {
                val views=RemoteViews(context.packageName,R.layout.widget_today)
                views.setTextViewText(R.id.widget_progress,"${plans.count{it.isCompleted}} / ${plans.size} tamamlandı")
                views.setTextViewText(R.id.widget_title,next?.title?:"Gününe yer aç.")
                views.setTextViewText(R.id.widget_time,next?.let{PlanTime.label(it)}?:"Yeni bir plan ekleyebilirsin.")
                views.setOnClickPendingIntent(R.id.widget_body,open("open",next?.id?:0))
                views.setOnClickPendingIntent(R.id.widget_add,open("new"))
                views.setViewVisibility(R.id.widget_done,if(next!=null)View.VISIBLE else View.GONE)
                if(next!=null)views.setOnClickPendingIntent(R.id.widget_done,PendingIntent.getBroadcast(context,0,Intent(context,TodayWidget::class.java).setAction("com.rota.app.WIDGET_DONE").setData(Uri.parse("rotam://widget/done/${next.id}")).putExtra("planId",next.id),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                manager.updateAppWidget(id,views)
            }
        }
    }
}
