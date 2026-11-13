package com.nihad64.rotam.domain.usecase

import com.nihad64.rotam.data.model.Plan
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime

/** Additive metadata: old Room rows and versioned backups keep their identity and notes. */
object PlanOutcome {
 enum class Status { UNKNOWN, COMPLETED, PARTIAL, NOT_DONE, POSTPONED }
 fun status(p:Plan):Status = when {
  p.isCompleted -> Status.COMPLETED
  p.isSkipped -> Status.NOT_DONE
  else -> runCatching{Status.valueOf(JSONObject(p.extraData).optString("outcome","UNKNOWN"))}.getOrDefault(Status.UNKNOWN)
 }
 fun record(p:Plan,status:Status,amount:Double?=null,target:Double?=null,now:LocalDateTime=LocalDateTime.now()):Plan {
  require(status!=Status.PARTIAL || (amount!=null&&target!=null&&amount.isFinite()&&target.isFinite()&&amount>0&&target>amount)) { "Kısmi miktar sıfırdan büyük ve hedeften küçük olmalı." }
  val extra=JSONObject(p.extraData)
  val history=extra.optJSONArray("outcomeHistory")?:JSONArray()
  val event=JSONObject().put("status",status.name).put("at",now.toString()).put("date",p.date)
   .put("startTime",p.startTime?:JSONObject.NULL).put("endTime",p.endTime?:JSONObject.NULL).put("endDate",p.endDate?:JSONObject.NULL)
  if(status==Status.PARTIAL){event.put("amount",amount).put("target",target);extra.put("completedAmount",amount).put("targetAmount",target)}
  else {extra.remove("completedAmount");extra.remove("targetAmount")}
  history.put(event);extra.put("outcomeHistory",history).put("outcome",status.name)
  return p.copy(extraData=extra.toString(),isCompleted=status==Status.COMPLETED,isSkipped=status==Status.NOT_DONE)
 }
 /** Editing through AI or the regular form must retain the previous schedule too. */
 fun preserveSchedule(old:Plan,proposed:Plan,now:LocalDateTime=LocalDateTime.now()):Plan {
  if(listOf(old.date,old.startTime,old.endTime,old.endDate)==listOf(proposed.date,proposed.startTime,proposed.endTime,proposed.endDate))return proposed
  val extra=JSONObject(proposed.extraData);val prior=JSONObject(old.extraData).optJSONArray("outcomeHistory")?:JSONArray()
  val history=extra.optJSONArray("outcomeHistory")?:JSONArray(prior.toString())
  val last=history.optJSONObject(history.length()-1)
  // Dedicated postponement has already appended this exact old schedule.
  if(history.length()>prior.length()&&last?.optString("status")==Status.POSTPONED.name&&last.optString("date")==old.date)return proposed
  history.put(JSONObject().put("status",Status.POSTPONED.name).put("at",now.toString()).put("date",old.date).put("startTime",old.startTime?:JSONObject.NULL).put("endTime",old.endTime?:JSONObject.NULL).put("endDate",old.endDate?:JSONObject.NULL))
  extra.put("outcomeHistory",history)
  return proposed.copy(extraData=extra.toString())
 }
 fun fresh(p:Plan):Plan {
  val e=JSONObject(p.extraData)
  listOf("outcome","outcomeHistory","completedAmount","targetAmount","focusSegments","startedSchedule").forEach{e.remove(it)}
  return p.copy(extraData=e.toString(),isCompleted=false,isSkipped=false)
 }
 fun fraction(p:Plan):Double?=when(status(p)) {
  Status.COMPLETED->1.0
  Status.PARTIAL->runCatching{val e=JSONObject(p.extraData);val a=e.getDouble("completedAmount");val t=e.getDouble("targetAmount");require(a.isFinite()&&t.isFinite()&&a>0&&t>a);a/t}.getOrNull()
  Status.NOT_DONE->0.0
  else->null
 }
}
