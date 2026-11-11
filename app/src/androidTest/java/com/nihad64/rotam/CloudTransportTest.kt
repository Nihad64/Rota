package com.nihad64.rotam

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Source
import com.nihad64.rotam.data.db.AppDatabase
import com.nihad64.rotam.data.model.Plan
import com.nihad64.rotam.data.sync.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Never uses the user's account, default Firebase app, or primary Room database. */
@RunWith(AndroidJUnit4::class)
class CloudTransportTest {
 private class Baseline(var value:SyncEnvelope=SyncEnvelope(0,emptyMap())):SyncBaselineStore {
  override fun read()=value
  override fun write(value:SyncEnvelope){this.value=value}
 }
 @Test fun isolatedSdkClientsRestoreOfflineEditsAndReviewConcurrentChanges()=runBlocking {
  check(android.os.Build.MODEL.contains("sdk_gphone")||android.os.Build.FINGERPRINT.contains("generic")){"Synthetic cloud tests run only on the CI emulator"}
  withTimeout(120000){
   val context=InstrumentationRegistry.getInstrumentation().targetContext
   val options=FirebaseOptions.Builder().setProjectId("demo-rotam-sync").setApplicationId("1:123456789:android:1234567890abcdef").setApiKey("fake-api-key").build()
   val apps=List(3){FirebaseApp.initializeApp(context,options,"sync-test-${UUID.randomUUID()}")}
   val auth=apps.map{FirebaseAuth.getInstance(it).also{a->a.useEmulator("10.0.2.2",9099)}}
   val cloud=apps.map{FirebaseFirestore.getInstance(it).also{f->
    f.firestoreSettings=FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build()
    f.useEmulator("10.0.2.2",8080)
   }}
   val email="synthetic-${UUID.randomUUID()}@example.test";val password="SyntheticOnly123!"
   val uid=auth[0].createUserWithEmailAndPassword(email,password).await().user!!.uid
   auth[1].signInWithEmailAndPassword(email,password).await()
   auth[2].createUserWithEmailAndPassword("other-$email",password).await()
   val rooms=List(2){Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build()}
   try {
    val bases=List(2){Baseline()}
    val endpoints=List(2){i->FirestoreSyncEndpoint(uid,"synthetic-client-$i",cloud[i]){auth[i].currentUser?.uid}}
    val engines=List(2){i->SyncEngine(RoomSyncStore(context,rooms[i],ownerUid=uid),bases[i],endpoints[i])}
    val original=Plan(id=41,uid="shared-${UUID.randomUUID()}",title="Sentetik okuma",date="2026-11-11",extraData="{\"note\":\"Sentetik not\"}")
    val memoryRepo=com.nihad64.rotam.data.repository.MemoryRepository(rooms[0])
    val memory=memoryRepo.save(uid,"Sentetik ulaşım tercihi",true)
    val localMemory=memoryRepo.save(uid,"Yalnız ilk cihazdaki sentetik bilgi",false)
    rooms[0].planDao().insert(original)
    assertTrue(engines[0].synchronize() is SyncResult.Complete)
    assertTrue(engines[1].synchronize() is SyncResult.Complete)
    val restored=rooms[1].planDao().byUid(original.uid)!!
    assertEquals(memory.text,rooms[1].memoryDao().byUid(memory.uid)!!.text)
    assertNull(rooms[1].memoryDao().byUid(localMemory.uid))
    assertNotEquals(original.id,restored.id);assertEquals(original.extraData,restored.extraData)
    // Device A advances while device B is offline and edits the same plan.
    rooms[0].planDao().update(original.copy(title="Cihaz A"));engines[0].synchronize()
    cloud[1].disableNetwork().await()
    rooms[1].planDao().update(restored.copy(title="Çevrimdışı B"))
    val oldRevision=bases[1].value.revision
    try{engines[1].synchronize();fail("Offline server read must fail")}catch(_:FirebaseFirestoreException){}
    assertEquals(oldRevision,bases[1].value.revision)
    assertEquals("Çevrimdışı B",rooms[1].planDao().byUid(original.uid)!!.title)
    cloud[1].enableNetwork().await()
    val review=engines[1].synchronize() as SyncResult.Conflicts
    assertEquals("Cihaz A",CloudRecords.decode(endpoints[0].read().records).plans.single().title)
    assertTrue(engines[1].resolve(review,false) is SyncResult.Complete)
    assertEquals("Cihaz A",rooms[1].planDao().byUid(original.uid)!!.title)
    // Both installations can allocate the same numeric row ID without merging unrelated plans.
    rooms[0].planDao().insert(Plan(id=42,title="A eklemesi",date=original.date))
    rooms[1].planDao().insert(Plan(id=42,title="B eklemesi",date=original.date))
    engines[0].synchronize();engines[1].synchronize();engines[0].synchronize()
    assertEquals(3,rooms[0].planDao().all().size);assertEquals(3,rooms[1].planDao().all().size)
    // Another authenticated account is rejected by real Firestore rules.
    try{cloud[2].document("users/$uid/sync/snapshot").get(Source.SERVER).await();fail("Other account must be denied")}
    catch(e:FirebaseFirestoreException){assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED,e.code)}
    // Tombstones propagate without recreating a removed occurrence.
    rooms[0].planDao().update(rooms[0].planDao().byUid(original.uid)!!.copy(isDeleted=true))
    engines[0].synchronize();engines[1].synchronize()
    assertTrue(rooms[1].planDao().byUid(original.uid)!!.isDeleted)
    memoryRepo.remove(uid,memory.uid);engines[0].synchronize();engines[1].synchronize()
    assertEquals("",rooms[1].memoryDao().byUid(memory.uid)!!.text)
    assertTrue(rooms[1].memoryDao().byUid(memory.uid)!!.isDeleted)
    assertEquals(localMemory,rooms[0].memoryDao().byUid(localMemory.uid))
   } finally {
    rooms.forEach{it.close()}
    cloud.forEach{it.terminate().await()}
    apps.forEach{it.delete()}
   }
  }
 }
}
