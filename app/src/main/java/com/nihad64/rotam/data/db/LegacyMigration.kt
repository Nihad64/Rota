package com.nihad64.rotam.data.db
import android.content.Context
import androidx.room.withTransaction
import com.nihad64.rotam.data.model.*
import com.nihad64.rotam.domain.usecase.*
import org.json.*
import java.io.File
import java.time.*
object LegacyParser {
 fun parse(raw:String):List<Plan>{val a=JSONArray(raw);return (0 until a.length()).map{i->
  val o=a.getJSONObject(i);val date=LocalDate.parse(o.getString("date"));val timed=o.optString("type")=="Saatli"
  fun time(k:String):String?=o.nullString(k)?.let{runCatching{LocalTime.parse(it).toString()}.getOrNull()}
  val start=if(timed)time("startTime")?:time("time") else null;val end=if(timed)time("endTime") else null
  val next=if(start!=null&&end!=null&&LocalTime.parse(end)<=LocalTime.parse(start))date.plusDays(1).toString()else null
  val extras=JSONObject().put("note",o.optString("note")).put("legacyDetails",o.optString("details")).put("legacyStatus",o.optString("status")).put("legacyDuration",o.optString("duration")).put("legacyRepeat",o.optString("repeat","none"))
  Plan(uid="legacy-${o.optLong("id")}-${i}",title=o.optString("title").ifBlank{"Adsız plan"},date=date.toString(),category=Category.fromKey(o.optString("category")).key,startTime=start,endTime=end,endDate=next,isCompleted=o.optBoolean("done")||o.optString("status")=="done",isSkipped=o.optString("status")=="cancelled",repeatType=RepeatType.fromKey(o.optString("repeat","none")).key,extraData=extras.toString(),reminderEnabled=timed&&(start!=null||end!=null))
 }}
}
class LegacyMigration(private val context:Context,private val db:AppDatabase){
 suspend fun run():Int{
  val prefs=context.getSharedPreferences("rotam_migrations",0)
  if(prefs.getBoolean("legacy_v2",false))return 0
  val raw=context.getSharedPreferences("rota_store",0).getString("plans","[]")?:"[]"
  val backup=File(context.noBackupFilesDir,"legacy-plans-before-v3.json")
  if(!backup.exists()){val tmp=File(context.noBackupFilesDir,"legacy-plans.tmp");tmp.writeText(raw);check(tmp.renameTo(backup))}
  val plans=LegacyParser.parse(raw)
  db.withTransaction{
   plans.forEach{if(db.planDao().byUid(it.uid)==null)db.planDao().insert(it)}
   // Old releases stored unlinked copies for 45 days. Do not guess ownership when anchors overlap.
   val anchors=plans.filter{it.repeatType!="none"}
   fun signature(p:Plan):String {val x=JSONObject(p.extraData);return listOf(p.title,p.category,p.startTime,p.endTime,x.optString("note"),x.optString("legacyDetails")).joinToString("|#|")}
   for(a in anchors){
    val stored=db.planDao().byUid(a.uid)?:continue;if(stored.seriesId!=0L)continue
    if(anchors.count{signature(it)==signature(a)}!=1)continue
    val start=LocalDate.parse(a.date);val horizon=start.plusDays(45)
    val sid=db.seriesDao().insert(PlanSeries(template=PlanJson.encode(a).toString(),anchor=a.date))
    val copies=plans.filter{signature(it)==signature(a)&&LocalDate.parse(it.date) in start..horizon&&Recurrence.occurs(a,start,LocalDate.parse(it.date))}
    val dates=mutableSetOf<String>()
    for(copy in copies){val p=db.planDao().byUid(copy.uid)?:continue;if(dates.add(p.date))db.planDao().update(p.copy(seriesId=sid,originalDate=p.date,occurrenceKey="$sid:${p.date}",repeatType=a.repeatType))}
    for(day in Recurrence.dates(a,start,start,horizon)){if(day.toString() !in dates){val key="$sid:$day";db.planDao().insert(PlanTime.moved(a,day).copy(uid="series:$key",seriesId=sid,occurrenceKey=key,originalDate=day.toString(),isDeleted=true))}}
   }
  }
  check(prefs.edit().putBoolean("legacy_v2",true).commit())
  return plans.size
 }
}
