package com.nihad64.rotam.data.sync

import com.nihad64.rotam.data.backup.BackupData
import com.nihad64.rotam.data.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/** Local autoincrement ids never cross the device boundary. */
enum class RecordKind { PLAN, GOAL, SERIES, MEMORY }
data class RecordKey(val kind:RecordKind,val uid:String)
data class CloudRecord(val key:RecordKey,val body:String) {
 init {require(key.uid.isNotBlank()&&key.uid.length<=512);require(body.toByteArray().size<=256*1024)}
 val documentId:String get()=MessageDigest.getInstance("SHA-256").digest("${key.kind}:${key.uid}".toByteArray()).joinToString(""){"%02x".format(it)}
}
object CloudRecords {
 fun encode(data:BackupData):Map<RecordKey,CloudRecord> {
  val goals=data.goals.associateBy{it.id};val series=data.series.associateBy{it.id}
  fun portable(p:Plan):JSONObject=PlanJson.encode(p).apply{
   remove("id");remove("seriesId");remove("goalId");remove("occurrenceKey")
   if(p.seriesId!=0L)put("seriesUid",series[p.seriesId]?.uid?:error("Rutin bağlantısı eksik"))
   p.goalId?.let{put("goalUid",goals[it]?.uid?:error("Hedef bağlantısı eksik"))}
  }
  val all=mutableListOf<CloudRecord>()
  fun add(kind:RecordKind,uid:String,json:JSONObject){all+=CloudRecord(RecordKey(kind,uid),canonical(json))}
  data.goals.forEach{g->add(RecordKind.GOAL,g.uid,JSONObject().put("uid",g.uid).put("title",g.title).put("period",g.period).put("targetCount",g.targetCount).put("currentCount",g.currentCount).put("category",g.category).put("dueDate",g.dueDate).put("isCompleted",g.isCompleted))}
  data.series.forEach{s->add(RecordKind.SERIES,s.uid,JSONObject().put("uid",s.uid).put("anchor",s.anchor).put("untilDate",s.untilDate).put("template",portable(PlanJson.decode(JSONObject(s.template))))) }
  data.plans.forEach{p->add(RecordKind.PLAN,p.uid,portable(p))}
  data.memories.filter{it.cloudApproved}.forEach{m->add(RecordKind.MEMORY,m.uid,com.nihad64.rotam.data.backup.BackupCodec.memoryJson(m).apply{remove("cloudApproved")})}
  require(all.map{it.key}.distinct().size==all.size){"Yinelenen bulut kimliği"}
  return all.associateBy{it.key}
 }
 fun decode(records:Map<RecordKey,CloudRecord>,existing:BackupData=BackupData(emptyList(),emptyList(),emptyList())):BackupData {
  records.forEach{(key,value)->require(key==value.key&&JSONObject(value.body).getString("uid")==key.uid){"Bulut kayıt kimliği uyuşmuyor"}}
  fun objects(kind:RecordKind)=records.values.filter{it.key.kind==kind}.sortedBy{it.key.uid}.map{JSONObject(it.body)}
  fun ids(existing:Map<String,Long>,uids:List<String>):Map<String,Long>{
   var next=(existing.values.maxOrNull()?:0L)+1
   return uids.associateWith{existing[it]?:next++}
  }
  val goalJson=objects(RecordKind.GOAL);val seriesJson=objects(RecordKind.SERIES);val planJson=objects(RecordKind.PLAN)
  val goalIds=ids(existing.goals.associate{it.uid to it.id},goalJson.map{it.getString("uid")})
  val seriesIds=ids(existing.series.associate{it.uid to it.id},seriesJson.map{it.getString("uid")})
  val planIds=ids(existing.plans.associate{it.uid to it.id},planJson.map{it.getString("uid")})
  fun local(json:JSONObject,id:Long):Plan {
   val copy=JSONObject(json.toString()).put("id",id)
   val sid=json.nullString("seriesUid")?.let{seriesIds[it]?:error("Bulut rutin kaydı henüz eksik")}?:0L
   copy.put("seriesId",sid)
   json.nullString("goalUid")?.let{copy.put("goalId",goalIds[it]?:error("Bulut hedef kaydı henüz eksik"))}
   if(sid!=0L)copy.put("occurrenceKey","$sid:${json.nullString("originalDate")?:json.getString("date")}")
   return PlanJson.decode(copy)
  }
  val goals=goalJson.map{g->Goal(goalIds.getValue(g.getString("uid")),g.getString("title"),g.getString("period"),g.getInt("targetCount"),g.getInt("currentCount"),g.getString("category"),g.getString("dueDate"),g.getBoolean("isCompleted"),g.getString("uid"))}
  val series=seriesJson.map{s->PlanSeries(seriesIds.getValue(s.getString("uid")),PlanJson.encode(local(s.getJSONObject("template"),0)).toString(),s.getString("anchor"),s.nullString("untilDate"),s.getString("uid"))}
  val plans=planJson.map{local(it,planIds.getValue(it.getString("uid")))}
  // Use the same validation contract as portable backups before touching Room.
  val memories=objects(RecordKind.MEMORY).map{com.nihad64.rotam.data.backup.BackupCodec.memoryFromJson(it.put("cloudApproved",true))}
  val data=BackupData(plans,goals,series,memories)
  return com.nihad64.rotam.data.backup.BackupCodec.decode(com.nihad64.rotam.data.backup.BackupCodec.encode(data))
 }
 private fun canonical(value:Any?):String=when(value){
  null,JSONObject.NULL->"null"
  is JSONObject->value.keys().asSequence().toList().sorted().joinToString(",","{","}"){JSONObject.quote(it)+":"+canonical(value.get(it))}
  is JSONArray->(0 until value.length()).joinToString(",","[","]"){canonical(value.get(it))}
  is String->JSONObject.quote(value)
  else->value.toString()
 }
}

data class SyncConflict(val key:RecordKey,val base:CloudRecord?,val local:CloudRecord?,val remote:CloudRecord?)
data class MergeResult(val records:Map<RecordKey,CloudRecord>,val conflicts:List<SyncConflict>){val canCommit:Boolean get()=conflicts.isEmpty()}
object SyncMerge {
 /** Three-way merge. Concurrent edits/deletion are surfaced, never silently overwritten. */
 fun merge(base:Map<RecordKey,CloudRecord>,local:Map<RecordKey,CloudRecord>,remote:Map<RecordKey,CloudRecord>):MergeResult {
  val out=mutableMapOf<RecordKey,CloudRecord>();val conflicts=mutableListOf<SyncConflict>()
  for(key in base.keys+local.keys+remote.keys){
   val b=base[key];val l=local[key];val r=remote[key]
   val chosen=when{l==r->l;l==b->r;r==b->l;else->{conflicts+=SyncConflict(key,b,l,r);l}}
   chosen?.let{out[key]=it}
  }
  return MergeResult(out,conflicts)
 }
}
