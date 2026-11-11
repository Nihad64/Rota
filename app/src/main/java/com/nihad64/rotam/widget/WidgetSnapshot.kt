package com.nihad64.rotam.widget

import com.nihad64.rotam.data.model.Plan
import com.nihad64.rotam.domain.usecase.PlanOutcome
import com.nihad64.rotam.domain.usecase.PlanTime
import java.time.LocalDateTime

data class WidgetSnapshot(val today:List<Plan>,val pending:List<Plan>,val done:Int,val awaiting:Int) {
 val total get()=today.size
 val progress get()=if(total==0)0 else done*100/total
 companion object {
  fun build(all:List<Plan>,now:LocalDateTime):WidgetSnapshot {
   val visible=all.filterNot{it.isDeleted}.distinctBy{it.uid}
   val date=now.toLocalDate().toString()
   val today=visible.filter{it.date==date||(it.date<date&&it.endDate?.let{d->d>=date}==true)}
   val pending=today.filter{!it.isCompleted&&!it.isSkipped}.sortedWith(compareBy<Plan>{PlanTime.start(it)?:PlanTime.end(it)?:now.toLocalDate().plusDays(1).atStartOfDay()}.thenBy{it.title})
   val awaiting=visible.count{PlanOutcome.status(it) !in setOf(PlanOutcome.Status.COMPLETED,PlanOutcome.Status.NOT_DONE,PlanOutcome.Status.PARTIAL)&&((it.date<date&&PlanTime.end(it)?.let{end->end>now}!=true)||PlanTime.overdue(it,now))}
   return WidgetSnapshot(today,pending,today.count{it.isCompleted},awaiting)
  }
 }
}
