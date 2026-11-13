package com.nihad64.rotam

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.nihad64.rotam.data.db.*
import com.nihad64.rotam.data.model.*
import com.nihad64.rotam.data.backup.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=android.app.Application::class)
class SyncIdentityMigrationTest {
 @Test fun v2MigrationPreservesHistoryRelationsAndPortableIdentity()=runBlocking {
  val ctx=ApplicationProvider.getApplicationContext<Context>()
  val name="migration-${java.util.UUID.randomUUID()}.db"
  val original=Plan(id=41,title="Korunan okuma",date="2026-11-10",seriesId=4,goalId=7,uid="series:4:2026-11-10",repeatType="weekly",occurrenceKey="4:2026-11-10",originalDate="2026-11-10",extraData="{\"note\":\"Korunan not\"}",focusSeconds=120)
  val template=original.copy(id=0,uid="original-template",seriesId=0,occurrenceKey=null,originalDate=null)
  val helper=FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(ctx).name(name).callback(object:SupportSQLiteOpenHelper.Callback(2){
   override fun onCreate(db:SupportSQLiteDatabase){
    val schema=JSONObject(javaClass.classLoader!!.getResource("room-v2.json")!!.readText()).getJSONObject("database")
    val entities=schema.getJSONArray("entities")
    for(i in 0 until entities.length()){
     val e=entities.getJSONObject(i);val table=e.getString("tableName")
     db.execSQL(e.getString("createSql").replace("\${TABLE_NAME}",table))
     val indices=e.getJSONArray("indices")
     for(j in 0 until indices.length())db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}",table))
    }
    db.execSQL("INSERT INTO goals VALUES(7,'Kitap','weekly',3,1,'kitap','2026-11-16',0)")
    db.execSQL("INSERT INTO series VALUES(4,?,'2026-11-10',NULL)",arrayOf(PlanJson.encode(template).toString()))
    val json=PlanJson.encode(original);val keys=json.keys().asSequence().toList()
    val values=keys.map{key->when(val v=json.get(key)){JSONObject.NULL->null;is Boolean->if(v)1 else 0;else->v}}.toTypedArray()
    db.execSQL("INSERT INTO plans (${keys.joinToString()}) VALUES(${keys.joinToString{ "?" }})",values)
   }
   override fun onUpgrade(db:SupportSQLiteDatabase,oldVersion:Int,newVersion:Int)=error("Fixture upgrade not expected")
  }).build())
  helper.writableDatabase;helper.close()
  val db=Room.databaseBuilder(ctx,AppDatabase::class.java,name).addMigrations(Migrations.V2_3,Migrations.V3_4).allowMainThreadQueries().build()
  try {
   val plan=db.planDao().byId(41)!!;val series=db.seriesDao().byId(4)!!;val goal=db.goalDao().byId(7)!!
   assertEquals(original.title,plan.title);assertEquals(original.extraData,plan.extraData);assertEquals(120,plan.focusSeconds)
   assertEquals(4,plan.seriesId);assertEquals(7L,plan.goalId);assertEquals(original.occurrenceKey,plan.occurrenceKey)
   assertEquals(RecordIdentity.legacySeries("original-template"),series.uid)
   assertEquals(RecordIdentity.occurrence(series.uid,"2026-11-10"),plan.uid)
   val restored=BackupCodec.decode(BackupStore(ctx,db).export())
   assertEquals(series.uid,restored.series.single().uid);assertEquals(goal.uid,restored.goals.single().uid)
  } finally {db.close();ctx.deleteDatabase(name)}
 }
 @Test fun restoringV1BackupDerivesSameIdentityInsteadOfCreatingDuplicateRoutine(){
  val template=Plan(title="Oku",date="2026-11-10",repeatType="daily",uid="template-uid")
  val raw=JSONObject().put("format","rotam-backup").put("version",1).put("plans",org.json.JSONArray())
   .put("goals",org.json.JSONArray()).put("series",org.json.JSONArray().put(JSONObject().put("id",4).put("anchor",template.date).put("template",PlanJson.encode(template).toString())))
  assertEquals(RecordIdentity.legacySeries(template.uid),BackupCodec.decode(raw.toString()).series.single().uid)
 }
}
