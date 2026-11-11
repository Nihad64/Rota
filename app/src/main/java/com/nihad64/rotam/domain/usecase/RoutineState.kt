package com.nihad64.rotam.domain.usecase
import com.nihad64.rotam.data.model.Plan
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Additive rule metadata; pause gaps are not failed occurrences. */
object RoutineState {
 private const val Paused="routinePaused"
 private const val Removed="pausedByRoutine"
 fun paused(template:Plan)=JSONObject(template.extraData).optBoolean(Paused,false)
 fun change(template:Plan,paused:Boolean,date:LocalDate):Plan {
  val extra=JSONObject(template.extraData)
  val history=extra.optJSONArray("routinePauseHistory")?:JSONArray()
  if(paused)history.put(JSONObject().put("from",date.toString()))
  else if(history.length()>0){val last=history.getJSONObject(history.length()-1);if(!last.has("to"))last.put("to",date.toString())}
  extra.put(Paused,paused).put("routinePauseHistory",history)
  return template.copy(extraData=extra.toString())
 }
 fun suspended(p:Plan)=JSONObject(p.extraData).optBoolean(Removed,false)
 fun suspendOccurrence(p:Plan)=p.copy(isDeleted=true,extraData=JSONObject(p.extraData).put(Removed,true).toString())
 fun resumeOccurrence(p:Plan):Plan {val extra=JSONObject(p.extraData);extra.remove(Removed);return p.copy(isDeleted=false,extraData=extra.toString())}
 fun deliberateDeletion(p:Plan):Plan {val extra=JSONObject(p.extraData);extra.remove(Removed);return p.copy(isDeleted=true,extraData=extra.toString())}
}
