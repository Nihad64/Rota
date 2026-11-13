package com.nihad64.rotam
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nihad64.rotam.data.db.AppDatabase
import com.nihad64.rotam.data.repository.MemoryRepository
import com.nihad64.rotam.data.backup.*
import com.nihad64.rotam.data.sync.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=android.app.Application::class)
class MemoryRepositoryTest {
 @Test fun memoriesBelongToOneAccountAndStaleEditCannotOverwriteNewValue()=runBlocking {
  val ctx=ApplicationProvider.getApplicationContext<Context>();val db=Room.inMemoryDatabaseBuilder(ctx,AppDatabase::class.java).allowMainThreadQueries().build()
  try {
   val repo=MemoryRepository(db);val original=repo.save("owner-a","İşe ulaşımım 30 dakika",false)
   repo.save("owner-b","Başka hesabın bilgisi",true)
   assertEquals(listOf(original),repo.forAccount("owner-a"))
   val updated=repo.save("owner-a","İşe ulaşımım 20 dakika",false,original.uid,original)
   try{repo.save("owner-a","Eski editör",false,original.uid,original);fail()}catch(_:IllegalStateException){}
   assertEquals(listOf(updated),repo.forAccount("owner-a"))
   try{repo.save("owner-b","Sahiplik değiştirilemez",true,updated.uid);fail()}catch(_:IllegalStateException){}
  } finally {db.close()}
 }
 @Test fun localOnlyMemoryNeverEntersCloudRecordsAndSurvivesRemoteApply()=runBlocking {
  val ctx=ApplicationProvider.getApplicationContext<Context>();val db=Room.inMemoryDatabaseBuilder(ctx,AppDatabase::class.java).allowMainThreadQueries().build()
  try {
   val repo=MemoryRepository(db)
   val local=repo.save("owner-a","Yalnız bu cihazda saklanan bilgi",false)
   val approved=repo.save("owner-a","Eşitlemeye izin verilen bilgi",true)
   val other=repo.save("owner-b","Başka hesaba ait onaylı bilgi",true)
   val store=RoomSyncStore(ctx,db,ownerUid="owner-a");val records=store.records()
   assertEquals(listOf(approved.uid),records.keys.map{it.uid})
   val changed=approved.copy(text="Diğer cihazdaki yeni bilgi")
   val incoming=CloudRecords.encode(BackupData(emptyList(),emptyList(),emptyList(),listOf(changed)))
   assertTrue(store.applyIfUnchanged(records,incoming))
   assertEquals(local,db.memoryDao().byUid(local.uid));assertEquals(other,db.memoryDao().byUid(other.uid));assertEquals(changed,db.memoryDao().byUid(approved.uid))
  } finally {db.close()}
 }
 @Test fun forgettingRedactsContentAndBackupPreservesDeletionMarker()=runBlocking {
  val ctx=ApplicationProvider.getApplicationContext<Context>();val db=Room.inMemoryDatabaseBuilder(ctx,AppDatabase::class.java).allowMainThreadQueries().build()
  try {
   val repo=MemoryRepository(db);val saved=repo.save("owner-a","Unutulacak sentetik bilgi",true)
   repo.remove("owner-a",saved.uid)
   assertTrue(repo.forAccount("owner-a").isEmpty())
   val tombstone=db.memoryDao().byUid(saved.uid)!!;assertTrue(tombstone.isDeleted);assertEquals("",tombstone.text)
   val backup=BackupStore(ctx,db).export();assertFalse(backup.contains(saved.text))
   assertEquals(listOf(tombstone),BackupCodec.decode(backup).memories)
  } finally {db.close()}
 }
}
