package com.nihad64.rotam.data.model

import java.util.UUID

/** Portable identity never depends on a device's autoincrement sequence. */
object RecordIdentity {
 fun occurrence(seriesUid:String,date:String)="series:$seriesUid:$date"
 fun legacySeries(templateUid:String)=stable("series:$templateUid")
 fun legacyGoal(id:Long,title:String,dueDate:String)=stable("goal:$id:$title:$dueDate")
 private fun stable(value:String)=UUID.nameUUIDFromBytes(value.toByteArray(Charsets.UTF_8)).toString()
}
