package com.nihad64.rotam.domain.search

import com.nihad64.rotam.data.model.*
import com.nihad64.rotam.domain.usecase.RoutineState
import org.json.JSONObject
import java.time.LocalDate

sealed interface SearchEntry {
 val key:String
 data class Single(val plan:Plan):SearchEntry {override val key get()="plan:${plan.uid}"}
 data class Routine(val seriesId:Long,val template:Plan,val occurrences:List<Plan>,val representative:Plan):SearchEntry {override val key get()="routine:$seriesId"}
}
/** Presentation groups a rule once; factual count answers still use individual occurrences. */
object SearchPresentation {
 fun entries(plans:List<Plan>,series:List<PlanSeries>,query:String,today:LocalDate=LocalDate.now()):List<SearchEntry> {
  val rules=series.associateBy{it.id};val result=mutableListOf<SearchEntry>();val added=mutableSetOf<Long>()
  for(p in plans){
   if(p.seriesId==0L){result+=SearchEntry.Single(p);continue}
   if(!added.add(p.seriesId))continue
   val rule=rules[p.seriesId];val template=rule?.let{runCatching{PlanJson.decode(JSONObject(it.template))}.getOrNull()}?:p
   if(query.isBlank()&&(RoutineState.deleted(template)||rule?.untilDate?.let{it<today.toString()}==true))continue
   val children=plans.filter{it.seriesId==p.seriesId}.sortedWith(compareBy<Plan>{it.date}.thenBy{it.startTime?:it.endTime?:"99:99"})
   val representative=children.firstOrNull{it.date>=today.toString()&&!it.isCompleted&&!it.isSkipped}?:children.last()
   result+=SearchEntry.Routine(p.seriesId,template,children,representative)
  }
  return result
 }
 fun countLabel(entries:List<SearchEntry>):String {
  val plans=entries.count{it is SearchEntry.Single};val routines=entries.count{it is SearchEntry.Routine}
  return when{routines==0->"$plans plan";plans==0->"$routines rutin";else->"$plans plan · $routines rutin"}
 }
}
