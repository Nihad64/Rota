package com.nihad64.rotam.domain.usecase

import com.nihad64.rotam.data.model.Plan
import java.time.LocalDate
import java.time.LocalTime

/** Preserve identity, notes and original occurrence while recording the prior schedule. */
object ReschedulePlan {
 fun propose(plan:Plan,date:LocalDate,time:String?):Plan {
  val duration=PlanTime.durationMinutes(plan)
  var moved=PlanTime.moved(PlanOutcome.record(plan,PlanOutcome.Status.POSTPONED),date)
  if(time==null)moved=moved.copy(startTime=null,endTime=null,endDate=null,reminderEnabled=plan.reminderTime!=null&&plan.reminderEnabled)
  else if(plan.startTime==null&&plan.endTime!=null)moved=moved.copy(startTime=null,endTime=LocalTime.parse(time).toString(),endDate=null)
  else {
   val start=date.atTime(LocalTime.parse(time));val end=duration?.let{start.plusMinutes(it)}
   moved=moved.copy(startTime=start.toLocalTime().toString(),endTime=end?.toLocalTime()?.toString(),endDate=end?.toLocalDate()?.takeIf{it!=date}?.toString())
  }
  require(PlanTime.validate(moved)==null){PlanTime.validate(moved).orEmpty()}
  return moved
 }
}
