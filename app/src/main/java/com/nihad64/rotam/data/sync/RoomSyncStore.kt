package com.nihad64.rotam.data.sync

import android.content.Context
import android.util.AtomicFile
import androidx.room.withTransaction
import com.nihad64.rotam.data.backup.*
import com.nihad64.rotam.data.db.AppDatabase
import org.json.JSONObject
import java.io.File

class RoomSyncStore(private val context:Context,private val db:AppDatabase,private val ownerUid:String?=null,private val allowed:()->Boolean={true}):SyncLocalStore {
 private suspend fun data(allMemory:Boolean=false)=BackupData(db.planDao().all(),db.goalDao().all(),db.seriesDao().all(),db.memoryDao().all().filter{allMemory||(it.cloudApproved&&(ownerUid==null||it.ownerUid==ownerUid))})
 override suspend fun records()=db.withTransaction{check(allowed());CloudRecords.encode(data())}
 override suspend fun applyIfUnchanged(expected:Map<RecordKey,CloudRecord>,incoming:Map<RecordKey,CloudRecord>):Boolean {
  check(allowed())
  if(incoming==expected)return db.withTransaction{check(allowed());CloudRecords.encode(data())==expected}
  val previous=db.withTransaction{data(allMemory=true)}
  val decoded=CloudRecords.decode(incoming,previous)
  // A recoverable copy is written before changing the database. It is never uploaded.
  val file=AtomicFile(File(context.noBackupFilesDir,"before-cloud-sync.json"))
  val stream=file.startWrite()
  try{stream.write(BackupCodec.encode(previous).toByteArray());file.finishWrite(stream)}catch(e:Exception){file.failWrite(stream);throw e}
  return db.withTransaction {
   check(allowed())
   if(CloudRecords.encode(data())!=expected)return@withTransaction false
   for(m in decoded.memories){
    val old=db.memoryDao().byUid(m.uid)
    check(old==null||(old.ownerUid==m.ownerUid&&(old.cloudApproved||old==m))){"Cihazda özel tutulan hafıza kaydı korunuyor. Önce Gizlilik bölümündeki paylaşımını kontrol et."}
   }
   db.planDao().clear();db.seriesDao().clear();db.goalDao().clear();db.memoryDao().clearCloudApproved(ownerUid)
   decoded.goals.forEach{db.goalDao().insert(it)}
   decoded.series.forEach{db.seriesDao().insert(it)}
   check(db.planDao().insertAll(decoded.plans).all{it>0}){"Bir kayıt eklenemedi; işlem geri alındı."}
   decoded.memories.forEach{db.memoryDao().save(it)}
   true
  }
 }
}
class FileSyncBaseline(context:Context,uid:String,preferences:Boolean=false):SyncBaselineStore {
 private val id=java.security.MessageDigest.getInstance("SHA-256").digest(uid.toByteArray()).joinToString(""){"%02x".format(it)}
 private val file=AtomicFile(File(context.noBackupFilesDir,if(preferences)"preferences-sync-$id.json"else"sync-$id.json"))
 override fun read():SyncEnvelope {
  if(!file.baseFile.exists())return SyncEnvelope(0,emptyMap())
  val obj=JSONObject(String(file.readFully(),Charsets.UTF_8))
  val revision=obj.getLong("revision");require(revision>=0)
  return SyncEnvelope(revision,SyncEnvelopeCodec.decode(android.util.Base64.decode(obj.getString("records"),android.util.Base64.NO_WRAP)))
 }
 override fun write(value:SyncEnvelope){
  val obj=JSONObject().put("revision",value.revision).put("records",android.util.Base64.encodeToString(SyncEnvelopeCodec.encode(value.records),android.util.Base64.NO_WRAP))
  val stream=file.startWrite()
  try{stream.write(obj.toString().toByteArray());file.finishWrite(stream)}catch(e:Exception){file.failWrite(stream);throw e}
 }
}
