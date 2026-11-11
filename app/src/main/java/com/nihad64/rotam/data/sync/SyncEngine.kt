package com.nihad64.rotam.data.sync

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface SyncEndpoint {
 suspend fun read():SyncEnvelope
 suspend fun write(expectedRevision:Long,records:Map<RecordKey,CloudRecord>):Long
}
interface SyncLocalStore {
 suspend fun records():Map<RecordKey,CloudRecord>
 /** Must compare and apply atomically; false means a local edit occurred during network IO. */
 suspend fun applyIfUnchanged(expected:Map<RecordKey,CloudRecord>,incoming:Map<RecordKey,CloudRecord>):Boolean
}
interface SyncBaselineStore {
 fun read():SyncEnvelope
 fun write(value:SyncEnvelope)
}
sealed interface SyncResult {
 data class Complete(val revision:Long):SyncResult
 data class Conflicts(val remoteRevision:Long,val local:Map<RecordKey,CloudRecord>,val merged:MergeResult):SyncResult
 data object LocalChanged:SyncResult
}
class SyncEngine(private val local:SyncLocalStore,private val baseline:SyncBaselineStore,private val remote:SyncEndpoint){
 private val mutex=Mutex()
 suspend fun synchronize():SyncResult=mutex.withLock {
  val base=baseline.read();val before=local.records();val server=remote.read()
  check(base.revision==0L||server.revision>0){"Önceki bulut kopyası bulunamadı. Yerel kayıtlar korundu; eşitleme durduruldu."}
  val merged=reviewableMerge(base.records,before,server.records)
  if(!merged.canCommit)return@withLock SyncResult.Conflicts(server.revision,before,merged)
  CloudRecords.decode(merged.records)
  val revision=if(merged.records==server.records)server.revision else remote.write(server.revision,merged.records)
  if(!local.applyIfUnchanged(before,merged.records))return@withLock SyncResult.LocalChanged
  baseline.write(SyncEnvelope(revision,merged.records))
  SyncResult.Complete(revision)
 }
 /** A parent deletion plus a new child can be individually nonconflicting but invalid together.
  * Review the entire divergent graph in that case, so either choice is internally consistent. */
 private fun reviewableMerge(base:Map<RecordKey,CloudRecord>,local:Map<RecordKey,CloudRecord>,remote:Map<RecordKey,CloudRecord>):MergeResult {
  val merged=SyncMerge.merge(base,local,remote)
  fun choice(useLocal:Boolean):Map<RecordKey,CloudRecord> = merged.records.toMutableMap().apply {
   merged.conflicts.forEach{conflict->val value=if(useLocal)conflict.local else conflict.remote;if(value==null)remove(conflict.key)else put(conflict.key,value)}
  }
  val valid=runCatching{CloudRecords.decode(choice(true));CloudRecords.decode(choice(false))}.isSuccess
  if(valid)return merged
  val conflicts=(local.keys+remote.keys).filter{local[it]!=remote[it]}.map{SyncConflict(it,base[it],local[it],remote[it])}
  check(conflicts.isNotEmpty()){"Bulut kayıtları doğrulanamadı."}
  return MergeResult(local,conflicts)
 }
 /** Choices must refer to exactly the reviewed local and remote revisions. */
 suspend fun resolve(review:SyncResult.Conflicts,useLocal:Boolean):SyncResult=mutex.withLock {
  if(local.records()!=review.local)return@withLock SyncResult.LocalChanged
  val server=remote.read()
  if(server.revision!=review.remoteRevision)throw RemoteChangedException()
  val chosen=review.merged.records.toMutableMap()
  for(conflict in review.merged.conflicts){
   val value=if(useLocal)conflict.local else conflict.remote
   if(value==null)chosen.remove(conflict.key)else chosen[conflict.key]=value
  }
  CloudRecords.decode(chosen)
  val revision=if(chosen==server.records)server.revision else remote.write(server.revision,chosen)
  if(!local.applyIfUnchanged(review.local,chosen))return@withLock SyncResult.LocalChanged
  baseline.write(SyncEnvelope(revision,chosen))
  SyncResult.Complete(revision)
 }
}
