package com.nihad64.rotam.domain.usecase

import com.nihad64.rotam.data.model.Plan
import org.json.JSONArray
import org.json.JSONObject
import java.time.*

/** Measured intervals stay on their actual dates when a plan is moved later. */
object FocusLedger {
 data class Segment(val uid:String,val startedAt:Long,val seconds:Long)
 fun segments(plan:Plan):List<Segment> {
  val array=JSONObject(plan.extraData).optJSONArray("focusSegments")?:return emptyList()
  return (0 until array.length()).mapNotNull{i->runCatching{
   val e=array.getJSONObject(i);val id=e.getString("uid");val start=e.getLong("startedAt");val seconds=e.getLong("seconds")
   require(id.isNotBlank()&&seconds>=0);Math.addExact(start,Math.multiplyExact(seconds,1000))
   Segment(id,start,seconds)
  }.getOrNull()}.distinctBy{it.uid}
 }
 fun record(plan:Plan,uid:String,startedAt:Long,seconds:Long,total:Long):Plan {
  require(seconds>=0&&total>=0)
  val existing=segments(plan).associateBy{it.uid}.toMutableMap()
  val old=existing[uid]
  existing[uid]=Segment(uid,old?.startedAt?:startedAt,maxOf(old?.seconds?:0,seconds))
  val data=JSONArray(existing.values.map{JSONObject().put("uid",it.uid).put("startedAt",it.startedAt).put("seconds",it.seconds)})
  return plan.copy(focusSeconds=maxOf(plan.focusSeconds,total),extraData=JSONObject(plan.extraData).put("focusSegments",data).toString())
 }
 fun withoutLedger(raw:String):String {val value=JSONObject(raw);value.remove("focusSegments");return value.toString()}
 fun retainLedger(proposed:Plan,current:Plan):Plan {
  val next=JSONObject(proposed.extraData);val old=JSONObject(current.extraData).optJSONArray("focusSegments")
  if(old==null)next.remove("focusSegments")else next.put("focusSegments",old)
  return proposed.copy(focusSeconds=current.focusSeconds,extraData=next.toString())
 }
 fun byDay(plans:List<Plan>,from:LocalDate,to:LocalDate,zone:ZoneId=ZoneId.systemDefault()):Map<LocalDate,Long> {
  require(!to.isBefore(from))
  val windowStart=from.atStartOfDay(zone).toInstant().toEpochMilli();val windowEnd=to.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
  val out=mutableMapOf<LocalDate,Long>();val seen=mutableSetOf<String>()
  for(p in plans.distinctBy{it.uid})for(e in segments(p)) {
   if(!seen.add(e.uid))continue
   val end=runCatching{Math.addExact(e.startedAt,Math.multiplyExact(e.seconds,1000))}.getOrNull()?:continue
   var cursor=maxOf(e.startedAt,windowStart);val stop=minOf(end,windowEnd)
   while(cursor<stop){
    val day=Instant.ofEpochMilli(cursor).atZone(zone).toLocalDate()
    val boundary=day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();val until=minOf(stop,boundary)
    out[day]=(out[day]?:0)+(until-cursor)/1000;cursor=until
   }
  }
  return out
 }
 fun unassigned(plans:List<Plan>):Long=plans.distinctBy{it.uid}.sumOf{p->(p.focusSeconds-segments(p).sumOf{it.seconds}).coerceAtLeast(0)}
}
