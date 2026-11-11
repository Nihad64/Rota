package com.nihad64.rotam.data.sync

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

class RemoteChangedException:Exception("Bulut kaydı değişti; güncel hâliyle yeniden eşitlenecek.")

/** Revision CAS prevents a stale device overwriting another device's changes. */
class FirestoreSyncEndpoint(private val uid:String,private val writer:String):SyncEndpoint {
 private val ref get()=FirebaseFirestore.getInstance().document("users/$uid/sync/snapshot")
 private fun checkAccount(){check(FirebaseAuth.getInstance().currentUser?.uid==uid){"Hesap değişti; eşitleme durduruldu."}}
 override suspend fun read():SyncEnvelope {
  checkAccount()
  val doc=ref.get(Source.SERVER).await();checkAccount()
  if(!doc.exists())return SyncEnvelope(0,emptyMap())
  check(doc.getLong("schema")==1L){"Bulut sürümü desteklenmiyor."}
  val revision=doc.getLong("revision")?:error("Bulut sürümü eksik")
  check(revision>0)
  val records=SyncEnvelopeCodec.decode(doc.getBlob("payload")?.toBytes()?:error("Bulut verisi eksik"))
  // Validate all types and relationships before the caller may import anything.
  CloudRecords.decode(records)
  return SyncEnvelope(revision,records)
 }
 override suspend fun write(expectedRevision:Long,records:Map<RecordKey,CloudRecord>):Long {
  checkAccount();CloudRecords.decode(records)
  val payload=SyncEnvelopeCodec.encode(records)
  val revision=FirebaseFirestore.getInstance().runTransaction{tx->
   checkAccount()
   val snapshot=tx.get(ref)
   val current=if(snapshot.exists())snapshot.getLong("revision")?:error("Bulut sürümü eksik")else 0L
   if(current!=expectedRevision)throw RemoteChangedException()
   tx.set(ref,mapOf("schema" to 1L,"revision" to current+1,"writer" to writer,"updatedAt" to FieldValue.serverTimestamp(),"payload" to Blob.fromBytes(payload)))
   current+1
  }.await();checkAccount();return revision
 }
}
