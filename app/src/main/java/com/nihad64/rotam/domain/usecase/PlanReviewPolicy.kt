package com.nihad64.rotam.domain.usecase

import com.nihad64.rotam.data.model.Plan
import java.time.LocalDateTime

/** A past known start can be reviewed without inventing an end or marking failure. */
object PlanReviewPolicy {
 fun awaiting(p:Plan,now:LocalDateTime=LocalDateTime.now()):Boolean {
  if(p.isDeleted||PlanOutcome.status(p) !in setOf(PlanOutcome.Status.UNKNOWN,PlanOutcome.Status.POSTPONED))return false
  val end=PlanTime.end(p)
  if(end!=null)return end<=now
  val start=PlanTime.start(p)
  if(start!=null)return start<=now
  return p.date<now.toLocalDate().toString()
 }
}
