package com.nihad64.rotam

import com.nihad64.rotam.data.backup.BackupData
import com.nihad64.rotam.data.model.Plan
import com.nihad64.rotam.data.sync.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=android.app.Application::class)
class SyncEngineTest {
 private fun records(title:String,uid:String="plan")=CloudRecords.encode(BackupData(listOf(Plan(id=1,title=title,date="2026-11-10",createdAt=1000,uid=uid)),emptyList(),emptyList()))
 private class Local(var value:Map<RecordKey,CloudRecord>):SyncLocalStore {
  override suspend fun records()=value
  override suspend fun applyIfUnchanged(expected:Map<RecordKey,CloudRecord>,incoming:Map<RecordKey,CloudRecord>):Boolean {if(value!=expected)return false;value=incoming;return true}
 }
 private class Baseline(var value:SyncEnvelope=SyncEnvelope(0,emptyMap())):SyncBaselineStore {override fun read()=value;override fun write(value:SyncEnvelope){this.value=value}}
 private class Remote(var value:SyncEnvelope=SyncEnvelope(0,emptyMap()),var beforeWrite:()->Unit={}):SyncEndpoint {
  var writes=0
  override suspend fun read()=value
  override suspend fun write(expectedRevision:Long,records:Map<RecordKey,CloudRecord>):Long {if(expectedRevision!=value.revision)throw RemoteChangedException();beforeWrite();writes++;value=SyncEnvelope(value.revision+1,records);return value.revision}
 }
 @Test fun firstUploadAndSecondDeviceRestoreShareStableIdentity()=runBlocking {
  val remote=Remote();val first=Local(records("Korunan plan"));val firstBase=Baseline()
  assertTrue(SyncEngine(first,firstBase,remote).synchronize() is SyncResult.Complete)
  val second=Local(emptyMap());val secondBase=Baseline()
  assertTrue(SyncEngine(second,secondBase,remote).synchronize() is SyncResult.Complete)
  assertEquals(first.value,second.value);assertEquals(1,remote.writes)
 }
 @Test fun localEditDuringUploadIsNotOverwrittenAndBaselineIsNotFalselyAdvanced()=runBlocking {
  val original=records("Önce");val local=Local(original);val baseline=Baseline()
  val remote=Remote(beforeWrite={local.value=records("Ağ beklenirken düzenlendi")})
  assertEquals(SyncResult.LocalChanged,SyncEngine(local,baseline,remote).synchronize())
  assertEquals(records("Ağ beklenirken düzenlendi"),local.value);assertEquals(0,baseline.value.revision)
 }
 @Test fun conflictingVersionsRequireAChoiceAndStaleChoiceCannotOverwriteNewLocalEdit()=runBlocking {
  val base=records("Önce");val local=Local(records("Telefon"));val baseline=Baseline(SyncEnvelope(1,base));val remote=Remote(SyncEnvelope(2,records("Tablet")))
  val engine=SyncEngine(local,baseline,remote);val review=engine.synchronize() as SyncResult.Conflicts
  assertEquals(0,remote.writes);assertEquals(records("Telefon"),local.value)
  local.value=records("Sonraki düzenleme")
  assertEquals(SyncResult.LocalChanged,engine.resolve(review,false));assertEquals(0,remote.writes)
 }
 @Test fun offlineFailureDoesNotEraseLocalChangesOrBaseline()=runBlocking {
  val local=Local(records("Çevrimdışı"));val baseline=Baseline()
  val endpoint=object:SyncEndpoint {override suspend fun read():SyncEnvelope=throw java.io.IOException("offline");override suspend fun write(expectedRevision:Long,records:Map<RecordKey,CloudRecord>)=error("No write expected")}
  try{SyncEngine(local,baseline,endpoint).synchronize();fail()}catch(_:java.io.IOException){}
  assertEquals(records("Çevrimdışı"),local.value);assertEquals(0,baseline.value.revision)
 }
 @Test fun parentDeletionVersusNewLinkedPlanRequiresReviewOfConsistentVersions()=runBlocking {
  val goal=com.nihad64.rotam.data.model.Goal(id=7,uid="goal",title="Hedef",period="weekly",targetCount=3,dueDate="2026-11-16")
  val plan=Plan(id=1,title="Yeni bağlı plan",date="2026-11-11",goalId=7)
  val base=CloudRecords.encode(BackupData(emptyList(),listOf(goal),emptyList()))
  val local=Local(emptyMap())
  val remoteRecords=CloudRecords.encode(BackupData(listOf(plan),listOf(goal),emptyList()))
  val remote=Remote(SyncEnvelope(2,remoteRecords));val baseline=Baseline(SyncEnvelope(1,base))
  val engine=SyncEngine(local,baseline,remote)
  val review=engine.synchronize() as SyncResult.Conflicts
  assertEquals(0,remote.writes);assertEquals(2,review.merged.conflicts.size)
  assertTrue(engine.resolve(review,false) is SyncResult.Complete)
  assertEquals(remoteRecords,local.value)
 }

 @Test fun missingPreviouslySyncedCloudCopyDoesNotDeleteLocalRecords()=runBlocking {
  val data=records("Korunacak kayıt");val local=Local(data);val baseline=Baseline(SyncEnvelope(3,data));val remote=Remote()
  try{SyncEngine(local,baseline,remote).synchronize();fail()}catch(_:IllegalStateException){}
  assertEquals(data,local.value);assertEquals(3,baseline.value.revision);assertEquals(0,remote.writes)
 }

}
