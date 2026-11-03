package com.nihad64.rotam.data.backup

import android.content.Context
import androidx.room.withTransaction
import com.nihad64.rotam.data.db.AppDatabase
import com.nihad64.rotam.data.model.*
import com.nihad64.rotam.domain.usecase.PlanTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.*
import java.io.File
import java.time.LocalDate

// Credentials and local notification delivery tokens never enter a portable backup.
data class BackupData(val plans:List<Plan>,val goals:List<Goal>,val series:List<PlanSeries>) {
    val visiblePlans get()=plans.count{!it.isDeleted}
}
object BackupCodec {
    fun encode(data:BackupData):String=JSONObject().put("format","rotam-backup").put("version",1)
        .put("plans",JSONArray(data.plans.map{PlanJson.encode(it)}))
        .put("goals",JSONArray(data.goals.map{g->JSONObject().put("id",g.id).put("title",g.title).put("period",g.period).put("targetCount",g.targetCount).put("currentCount",g.currentCount).put("category",g.category).put("dueDate",g.dueDate).put("isCompleted",g.isCompleted)}))
        .put("series",JSONArray(data.series.map{s->JSONObject().put("id",s.id).put("template",s.template).put("anchor",s.anchor).put("untilDate",s.untilDate)})).toString(2)

    fun decode(raw:String):BackupData {
        val obj=JSONObject(raw)
        require(obj.getString("format")=="rotam-backup" && obj.getInt("version")==1){"Bu yedek biçimi desteklenmiyor."}
        fun array(key:String)=obj.getJSONArray(key).also{require(it.length()<=100000){"Yedek çok büyük."}}
        val pa=array("plans");val ga=array("goals");val sa=array("series")
        val plans=(0 until pa.length()).map{PlanJson.decode(pa.getJSONObject(it))}
        val goals=(0 until ga.length()).map { i->val g=ga.getJSONObject(i);Goal(g.getLong("id"),g.getString("title"),g.getString("period"),g.getInt("targetCount"),g.getInt("currentCount"),g.getString("category"),g.getString("dueDate"),g.getBoolean("isCompleted")) }
        val series=(0 until sa.length()).map { i->val s=sa.getJSONObject(i);PlanSeries(s.getLong("id"),s.getString("template"),s.getString("anchor"),s.nullString("untilDate")) }
        require(plans.map{it.id}.distinct().size==plans.size && plans.map{it.uid}.distinct().size==plans.size){"Yedekte yinelenen kayıt var."}
        val keys=plans.mapNotNull{it.occurrenceKey};require(keys.distinct().size==keys.size){"Yedekte yinelenen tekrar var."}
        require(goals.map{it.id}.distinct().size==goals.size && series.map{it.id}.distinct().size==series.size)
        goals.forEach { require(it.id>0 && it.title.isNotBlank() && it.targetCount>0 && it.currentCount>=0);LocalDate.parse(it.dueDate) }
        series.forEach { s->require(s.id>0);val p=PlanJson.decode(JSONObject(s.template));require(PlanTime.validate(p)==null);require(LocalDate.parse(s.anchor)==LocalDate.parse(p.date));s.untilDate?.let{LocalDate.parse(it)} }
        plans.forEach { p->require(p.id>0 && p.uid.isNotBlank() && p.focusSeconds>=0);require(PlanTime.validate(p)==null){"Yedekte geçersiz bir plan var."};JSONObject(p.extraData)
            require(p.seriesId==0L || series.any{it.id==p.seriesId}){"Yedekte bir tekrar kuralı eksik."}
            require(p.goalId==null || goals.any{it.id==p.goalId}){"Yedekte bir hedef eksik."}
            if(p.seriesId!=0L){val original=p.originalDate ?: error("Tekrar tarihi eksik");LocalDate.parse(original);require(p.occurrenceKey=="${p.seriesId}:$original")}
        }
        return BackupData(plans,goals,series)
    }
}
class BackupStore(private val context:Context,private val db:AppDatabase) {
    suspend fun export():String=db.withTransaction{BackupCodec.encode(BackupData(db.planDao().all(),db.goalDao().all(),db.seriesDao().all()))}
    suspend fun checkpoint():File=withContext(Dispatchers.IO) {
        val raw=export();val file=File(context.noBackupFilesDir,"before-restore-${System.currentTimeMillis()}.json")
        file.writeText(raw);check(file.length()>0);file
    }
    suspend fun restore(data:BackupData) {
        checkpoint()
        db.withTransaction {
            db.planDao().clear();db.seriesDao().clear();db.goalDao().clear()
            data.goals.forEach{db.goalDao().insert(it)}
            data.series.forEach{db.seriesDao().insert(it)}
            db.planDao().insertAll(data.plans)
        }
    }
    suspend fun recovery():String=withContext(Dispatchers.IO){
        val file=context.noBackupFilesDir.listFiles()?.filter{it.name.startsWith("before-restore-") && it.extension=="json"}?.maxByOrNull{it.lastModified()} ?: error("Henüz geri dönüş yedeği yok.")
        file.readText()
    }
}
