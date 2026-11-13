package com.nihad64.rotam

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nihad64.rotam.data.backup.BackupData
import com.nihad64.rotam.data.db.AppDatabase
import com.nihad64.rotam.data.model.*
import com.nihad64.rotam.data.sync.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=android.app.Application::class)
class RoomSyncStoreTest {
 @Test fun incomingRecordsKeepExistingIdsAndAreRecoverable()=runBlocking {
  val ctx=ApplicationProvider.getApplicationContext<Context>()
  val db=Room.inMemoryDatabaseBuilder(ctx,AppDatabase::class.java).allowMainThreadQueries().build()
  try {
   val p=Plan(id=41,uid="existing",title="Yerel",date="2026-11-11",extraData="{\"note\":\"Korumalı\"}")
   db.planDao().insert(p)
   val store=RoomSyncStore(ctx,db);val before=store.records()
   val incoming=CloudRecords.encode(BackupData(listOf(p.copy(id=1,title="Tablet"),Plan(id=2,uid="new",title="Yeni",date=p.date)),emptyList(),emptyList()))
   assertTrue(store.applyIfUnchanged(before,incoming))
   assertEquals(41,db.planDao().byUid(p.uid)!!.id)
   assertEquals(p.extraData,db.planDao().byUid(p.uid)!!.extraData)
   assertEquals(2,db.planDao().all().size)
   val backup=com.nihad64.rotam.data.backup.BackupCodec.decode(java.io.File(ctx.noBackupFilesDir,"before-cloud-sync.json").readText())
   assertEquals(listOf(p),backup.plans)
  } finally {db.close()}
 }
 @Test fun editingDuringNetworkWaitPreventsDatabaseReplacement()=runBlocking {
  val ctx=ApplicationProvider.getApplicationContext<Context>()
  val db=Room.inMemoryDatabaseBuilder(ctx,AppDatabase::class.java).allowMainThreadQueries().build()
  try {
   val p=Plan(id=1,title="Önce",date="2026-11-11");db.planDao().insert(p)
   val store=RoomSyncStore(ctx,db);val before=store.records()
   db.planDao().update(p.copy(title="Yeni yerel değişiklik"))
   assertFalse(store.applyIfUnchanged(before,emptyMap()))
   assertEquals("Yeni yerel değişiklik",db.planDao().byId(1)!!.title)
  } finally {db.close()}
 }
 @Test fun accountSwitchPreventsImportAndCorruptBaselineNeverMeansEmptyCloud()=runBlocking {
  val ctx=ApplicationProvider.getApplicationContext<Context>()
  val db=Room.inMemoryDatabaseBuilder(ctx,AppDatabase::class.java).allowMainThreadQueries().build()
  try {
   val p=Plan(id=1,title="Korunacak",date="2026-11-11");db.planDao().insert(p)
   var allowed=true;val store=RoomSyncStore(ctx,db){allowed};val before=store.records();allowed=false
   try{store.applyIfUnchanged(before,emptyMap());fail()}catch(_:IllegalStateException){}
   assertEquals(listOf(p),db.planDao().all())
   val uid="baseline-test";val baseline=FileSyncBaseline(ctx,uid)
   baseline.write(SyncEnvelope(3,before));assertEquals(3,baseline.read().revision)
   val id=java.security.MessageDigest.getInstance("SHA-256").digest(uid.toByteArray()).joinToString(""){"%02x".format(it)}
   java.io.File(ctx.noBackupFilesDir,"sync-$id.json").writeText("corrupt")
   try{baseline.read();fail("A corrupt baseline must stop sync")}catch(_:org.json.JSONException){}
  } finally {db.close()}
 }
}
