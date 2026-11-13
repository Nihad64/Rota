package com.nihad64.rotam.data.db
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
object Migrations {val V1_2=object:Migration(1,2){override fun migrate(db:SupportSQLiteDatabase){
 db.execSQL("ALTER TABLE plans RENAME TO plans_v1");db.execSQL("DROP INDEX IF EXISTS index_plans_date")
 db.execSQL("""CREATE TABLE plans (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,title TEXT NOT NULL,category TEXT NOT NULL,date TEXT NOT NULL,startTime TEXT,endTime TEXT,isCompleted INTEGER NOT NULL,isSkipped INTEGER NOT NULL,repeatType TEXT NOT NULL,extraData TEXT NOT NULL,createdAt INTEGER NOT NULL,reminderMinutes INTEGER NOT NULL,seriesId INTEGER NOT NULL,uid TEXT NOT NULL,endDate TEXT,reminderEnabled INTEGER NOT NULL,reminderTime TEXT,remindAtEnd INTEGER NOT NULL,repeatEvery INTEGER NOT NULL,repeatDays TEXT NOT NULL,repeatUntil TEXT,monthLastDay INTEGER NOT NULL,occurrenceKey TEXT,originalDate TEXT,isDeleted INTEGER NOT NULL,focusSeconds INTEGER NOT NULL,goalId INTEGER)""")
 db.execSQL("""INSERT INTO plans (id,title,category,date,startTime,endTime,isCompleted,isSkipped,repeatType,extraData,createdAt,reminderMinutes,seriesId,uid,reminderEnabled,remindAtEnd,repeatEvery,repeatDays,monthLastDay,isDeleted,focusSeconds,endDate) SELECT id,title,category,date,NULLIF(startTime,''),NULLIF(endTime,''),isCompleted,isSkipped,repeatType,extraData,createdAt,reminderMinutes,seriesId,'room-v1-'||id,1,0,1,'',0,0,0,CASE WHEN endTime<=startTime AND endTime!='' THEN date(date,'+1 day') ELSE NULL END FROM plans_v1""")
 db.execSQL("DROP TABLE plans_v1");db.execSQL("CREATE INDEX index_plans_date ON plans(date)");db.execSQL("CREATE UNIQUE INDEX index_plans_uid ON plans(uid)");db.execSQL("CREATE UNIQUE INDEX index_plans_occurrenceKey ON plans(occurrenceKey)")
 db.execSQL("CREATE TABLE series(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,template TEXT NOT NULL,anchor TEXT NOT NULL,untilDate TEXT)")
}}
val V2_3=object:Migration(2,3){override fun migrate(db:SupportSQLiteDatabase){
 db.execSQL("ALTER TABLE goals ADD COLUMN uid TEXT NOT NULL DEFAULT ''")
 db.execSQL("ALTER TABLE series ADD COLUMN uid TEXT NOT NULL DEFAULT ''")
 db.query("SELECT id,title,dueDate FROM goals").use{c->while(c.moveToNext()){
  val uid=com.nihad64.rotam.data.model.RecordIdentity.legacyGoal(c.getLong(0),c.getString(1),c.getString(2))
  db.execSQL("UPDATE goals SET uid=? WHERE id=?",arrayOf(uid,c.getLong(0)))
 }}
 db.query("SELECT id,template FROM series").use{c->while(c.moveToNext()){
  val template=org.json.JSONObject(c.getString(1))
  val uid=com.nihad64.rotam.data.model.RecordIdentity.legacySeries(template.getString("uid"))
  db.execSQL("UPDATE series SET uid=? WHERE id=?",arrayOf(uid,c.getLong(0)))
 }}
 db.execSQL("CREATE UNIQUE INDEX index_goals_uid ON goals(uid)")
 db.execSQL("CREATE UNIQUE INDEX index_series_uid ON series(uid)")
 db.execSQL("UPDATE plans SET uid='series:'||(SELECT uid FROM series WHERE series.id=plans.seriesId)||':'||COALESCE(originalDate,date) WHERE seriesId!=0 AND EXISTS(SELECT 1 FROM series WHERE series.id=plans.seriesId)")
}}
val V3_4=object:Migration(3,4){override fun migrate(db:SupportSQLiteDatabase){
 db.execSQL("CREATE TABLE IF NOT EXISTS memories(uid TEXT NOT NULL,ownerUid TEXT NOT NULL,text TEXT NOT NULL,createdAt INTEGER NOT NULL,updatedAt INTEGER NOT NULL,isDeleted INTEGER NOT NULL,cloudApproved INTEGER NOT NULL,PRIMARY KEY(uid))")
 db.execSQL("CREATE INDEX index_memories_ownerUid ON memories(ownerUid)")
}}

}
