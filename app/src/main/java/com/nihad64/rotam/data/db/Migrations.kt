package com.nihad64.rotam.data.db
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
object Migrations {val V1_2=object:Migration(1,2){override fun migrate(db:SupportSQLiteDatabase){
 db.execSQL("ALTER TABLE plans RENAME TO plans_v1");db.execSQL("DROP INDEX IF EXISTS index_plans_date")
 db.execSQL("""CREATE TABLE plans (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,title TEXT NOT NULL,category TEXT NOT NULL,date TEXT NOT NULL,startTime TEXT,endTime TEXT,isCompleted INTEGER NOT NULL,isSkipped INTEGER NOT NULL,repeatType TEXT NOT NULL,extraData TEXT NOT NULL,createdAt INTEGER NOT NULL,reminderMinutes INTEGER NOT NULL,seriesId INTEGER NOT NULL,uid TEXT NOT NULL,endDate TEXT,reminderEnabled INTEGER NOT NULL,reminderTime TEXT,remindAtEnd INTEGER NOT NULL,repeatEvery INTEGER NOT NULL,repeatDays TEXT NOT NULL,repeatUntil TEXT,monthLastDay INTEGER NOT NULL,occurrenceKey TEXT,originalDate TEXT,isDeleted INTEGER NOT NULL,focusSeconds INTEGER NOT NULL,goalId INTEGER)""")
 db.execSQL("""INSERT INTO plans (id,title,category,date,startTime,endTime,isCompleted,isSkipped,repeatType,extraData,createdAt,reminderMinutes,seriesId,uid,reminderEnabled,remindAtEnd,repeatEvery,repeatDays,monthLastDay,isDeleted,focusSeconds,endDate) SELECT id,title,category,date,NULLIF(startTime,''),NULLIF(endTime,''),isCompleted,isSkipped,repeatType,extraData,createdAt,reminderMinutes,seriesId,'room-v1-'||id,1,0,1,'',0,0,0,CASE WHEN endTime<=startTime AND endTime!='' THEN date(date,'+1 day') ELSE NULL END FROM plans_v1""")
 db.execSQL("DROP TABLE plans_v1");db.execSQL("CREATE INDEX index_plans_date ON plans(date)");db.execSQL("CREATE UNIQUE INDEX index_plans_uid ON plans(uid)");db.execSQL("CREATE UNIQUE INDEX index_plans_occurrenceKey ON plans(occurrenceKey)")
 db.execSQL("CREATE TABLE series(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,template TEXT NOT NULL,anchor TEXT NOT NULL,untilDate TEXT)")
}}}
