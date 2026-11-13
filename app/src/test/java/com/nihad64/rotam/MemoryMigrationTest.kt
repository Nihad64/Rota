package com.nihad64.rotam
import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.nihad64.rotam.data.db.*
import com.nihad64.rotam.data.model.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=android.app.Application::class)
class MemoryMigrationTest {
 @Test fun v3UpgradeAddsEmptyMemoryWithoutChangingExistingPlan()=runBlocking {
  val ctx=ApplicationProvider.getApplicationContext<Context>();val name="memory-migration-${java.util.UUID.randomUUID()}.db"
  val plan=Plan(id=41,title="Korunan ölçüm",date="2026-11-11",focusSeconds=61,extraData="{\"note\":\"Korunan not\"}")
  val helper=FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(ctx).name(name).callback(object:SupportSQLiteOpenHelper.Callback(3){
   override fun onCreate(db:SupportSQLiteDatabase){
    val schema=JSONObject(javaClass.classLoader!!.getResource("room-v3.json")!!.readText()).getJSONObject("database").getJSONArray("entities")
    for(i in 0 until schema.length()){
     val e=schema.getJSONObject(i);val table=e.getString("tableName");db.execSQL(e.getString("createSql").replace("\${TABLE_NAME}",table))
     val indices=e.getJSONArray("indices");for(j in 0 until indices.length())db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}",table))
    }
    val json=PlanJson.encode(plan);val keys=json.keys().asSequence().toList()
    val values=keys.map{when(val value=json.get(it)){JSONObject.NULL->null;is Boolean->if(value)1 else 0;else->value}}.toTypedArray()
    db.execSQL("INSERT INTO plans (${keys.joinToString()}) VALUES (${keys.joinToString{"?"}})",values)
   }
   override fun onUpgrade(db:SupportSQLiteDatabase,oldVersion:Int,newVersion:Int)=error("Fixture cannot upgrade itself")
  }).build())
  helper.writableDatabase;helper.close()
  val db=Room.databaseBuilder(ctx,AppDatabase::class.java,name).addMigrations(Migrations.V3_4).allowMainThreadQueries().build()
  try{assertEquals(plan,db.planDao().byId(41));assertTrue(db.memoryDao().all().isEmpty())}finally{db.close();ctx.deleteDatabase(name)}
 }
}
