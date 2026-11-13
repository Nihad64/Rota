package com.nihad64.rotam

import com.nihad64.rotam.data.backup.BackupData
import com.nihad64.rotam.data.model.*
import com.nihad64.rotam.data.sync.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=android.app.Application::class)
class CloudRecordsTest {
 @Test fun differentLocalIdsRoundTripWithoutBreakingRelations(){
  val goal=Goal(id=7,title="Kitap",period="weekly",targetCount=3,dueDate="2026-11-16")
  val template=Plan(title="Oku",date="2026-11-10",repeatType="daily",goalId=7)
  val series=PlanSeries(4,PlanJson.encode(template).toString(),template.date)
  val plan=template.copy(id=41,uid=RecordIdentity.occurrence(series.uid,template.date),seriesId=4,occurrenceKey="4:${template.date}",originalDate=template.date)
  val data=BackupData(listOf(plan),listOf(goal),listOf(series))
  val cloud=CloudRecords.encode(data)
  val other=CloudRecords.decode(cloud)
  assertNotEquals(7,other.goals.single().id);assertEquals(other.goals.single().id,other.plans.single().goalId)
  assertEquals(other.series.single().id,other.plans.single().seriesId)
  assertEquals(cloud,CloudRecords.encode(other))
  assertEquals(cloud,CloudRecords.encode(CloudRecords.decode(cloud,data)))
 }
 @Test fun independentDeviceInsertionsWithSameNumericIdRemainDistinct(){
  val a=Plan(id=1,title="A",date="2026-11-10");val b=Plan(id=1,title="B",date="2026-11-10")
  fun records(p:Plan)=CloudRecords.encode(BackupData(listOf(p),emptyList(),emptyList()))
  val merged=SyncMerge.merge(emptyMap(),records(a),records(b))
  assertTrue(merged.conflicts.isEmpty());assertEquals(2,merged.records.size)
  assertEquals(2,CloudRecords.decode(merged.records).plans.map{it.id}.distinct().size)
 }
 @Test fun concurrentEditVersusDeletionIsAConflictAndRetainsBothChoices(){
  val p=Plan(id=1,title="Önce",date="2026-11-10")
  fun records(p:Plan)=CloudRecords.encode(BackupData(listOf(p),emptyList(),emptyList()))
  val base=records(p);val local=records(p.copy(title="Benim değişikliğim"))
  val merge=SyncMerge.merge(base,local,emptyMap())
  assertEquals(1,merge.conflicts.size);assertNotNull(merge.conflicts.single().local);assertNull(merge.conflicts.single().remote)
  assertEquals(local,merge.records)
 }
 @Test fun malformedRemoteReferenceFailsBeforeAnyDatabaseMutation(){
  val p=Plan(id=1,title="Plan",date="2026-11-10")
  val record=CloudRecords.encode(BackupData(listOf(p),emptyList(),emptyList())).values.single()
  val invalid=record.copy(body=JSONObject(record.body).put("goalUid","missing-parent").toString())
  assertThrows(IllegalStateException::class.java){CloudRecords.decode(mapOf(invalid.key to invalid))}
 }

 @Test fun compressedSnapshotRoundTripsAndRejectsUnknownSchema(){
  val p=Plan(id=1,title="Spor",date="2026-11-10")
  val records=CloudRecords.encode(BackupData(listOf(p),emptyList(),emptyList()))
  assertEquals(records,SyncEnvelopeCodec.decode(SyncEnvelopeCodec.encode(records)))
  val out=java.io.ByteArrayOutputStream()
  java.util.zip.GZIPOutputStream(out).use{it.write("{\"schema\":999,\"records\":[]}".toByteArray())}
  assertThrows(IllegalArgumentException::class.java){SyncEnvelopeCodec.decode(out.toByteArray())}
 }
 @Test fun concurrentEditsBlockCommitUntilAChoiceIsMade(){
  val p=Plan(id=1,title="Önce",date="2026-11-10")
  fun records(p:Plan)=CloudRecords.encode(BackupData(listOf(p),emptyList(),emptyList()))
  val result=SyncMerge.merge(records(p),records(p.copy(title="Telefon")),records(p.copy(title="Tablet")))
  assertFalse(result.canCommit);assertEquals(1,result.conflicts.size)
 }
}
