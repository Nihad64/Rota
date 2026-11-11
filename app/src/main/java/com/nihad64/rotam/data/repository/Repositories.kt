package com.nihad64.rotam.data.repository
import androidx.room.withTransaction
import com.nihad64.rotam.data.db.*
import com.nihad64.rotam.data.model.*
import com.nihad64.rotam.domain.usecase.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID
class PlanRepository(private val db:AppDatabase){
 private val dao=db.planDao();private val lock=Mutex()
 fun observeDay(d:LocalDate):Flow<List<Plan>> = flow{ensure(d.minusDays(7),d);emitAll(dao.observeByDate(d.toString()))}
 fun observeRange(f:LocalDate,t:LocalDate):Flow<List<Plan>> = flow{ensure(f.minusDays(7),t);emitAll(dao.observeRange(f.toString(),t.toString()))}
 fun observeAll()=dao.observeAll()
 fun observeCompletedByCategory()=dao.observeCompletedByCategory();fun observeCompletedDays()=dao.observeCompletedDays()
 suspend fun range(f:LocalDate,t:LocalDate):List<Plan>{ensure(f,t);return dao.range(f.toString(),t.toString())}
 suspend fun byId(id:Long)=dao.byId(id)
 suspend fun upcoming(f:LocalDate)=dao.upcoming(f.toString())
 suspend fun pendingBetween(f:LocalDate,t:LocalDate)=dao.pendingBetween(f.toString(),t.toString())
 suspend fun update(p:Plan){require(PlanTime.validate(p)==null){PlanTime.validate(p).orEmpty()};dao.update(p)}
 suspend fun setFocusSeconds(id:Long,seconds:Long)=dao.setFocusSeconds(id,seconds)
 suspend fun recordFocusSegment(id:Long,uid:String,startedAt:Long,seconds:Long,total:Long)=db.withTransaction{
  val current=dao.byId(id)?:return@withTransaction
  dao.update(FocusLedger.record(current,uid,startedAt,seconds,total))
 }
 suspend fun restore(p:Plan)=dao.update(p)
 suspend fun delete(p:Plan)=dao.update(RoutineState.deliberateDeletion(p))
 suspend fun clear()=db.withTransaction{dao.clear();db.seriesDao().clear()}
 suspend fun create(p:Plan):List<Plan> = lock.withLock{db.withTransaction{
  require(PlanTime.validate(p)==null){PlanTime.validate(p).orEmpty()}
  val existing=dao.byUid(p.uid);if(existing!=null)return@withTransaction listOf(existing)
  val existingSeries=db.seriesDao().all().firstOrNull{PlanJson.decode(JSONObject(it.template)).uid==p.uid}
  if(existingSeries!=null)return@withTransaction dao.bySeries(existingSeries.id)
  if(p.repeatType=="none"){val id=dao.insert(p.copy(id=0));listOf(p.copy(id=id))}else{
   val id=db.seriesDao().insert(PlanSeries(template=PlanJson.encode(p).toString(),anchor=p.date,untilDate=p.repeatUntil))
   generate(db.seriesDao().byId(id)!!,LocalDate.parse(p.date),maxOf(LocalDate.now().plusDays(400),LocalDate.parse(p.date).plusDays(60)))
   dao.bySeries(id)
  }
 }}
 suspend fun ensure(f:LocalDate,t:LocalDate)=lock.withLock{db.withTransaction{db.seriesDao().all().forEach{generate(it,f,t)}}}
 private suspend fun generate(s:PlanSeries,f:LocalDate,t:LocalDate){
  val p=PlanOutcome.fresh(PlanJson.decode(JSONObject(s.template))).copy(repeatUntil=s.untilDate)
  if(RoutineState.paused(p))return
  val days=Recurrence.dates(p,LocalDate.parse(s.anchor),f,t)
  dao.insertAll(days.map{d->val key="${s.id}:$d";PlanTime.moved(p,d).copy(id=0,uid=RecordIdentity.occurrence(s.uid,d.toString()),seriesId=s.id,occurrenceKey=key,originalDate=d.toString(),isCompleted=false,isSkipped=false,isDeleted=false,focusSeconds=0)})
 }
 suspend fun setRoutinePaused(id:Long,paused:Boolean,date:LocalDate=LocalDate.now())=lock.withLock{db.withTransaction{
  val series=db.seriesDao().byId(id)?:error("Rutin bulunamadı.")
  val template=PlanJson.decode(JSONObject(series.template))
  if(RoutineState.paused(template)==paused)return@withTransaction
  db.seriesDao().update(series.copy(template=PlanJson.encode(RoutineState.change(template,paused,date)).toString()))
  val instances=dao.bySeries(id)
  if(paused){
   instances.filter{!it.isDeleted&&!it.isCompleted&&!it.isSkipped&&PlanOutcome.status(it)!=PlanOutcome.Status.PARTIAL&&it.date>=date.toString()}
    .forEach{dao.update(RoutineState.suspendOccurrence(it))}
  }else{
   instances.filter{it.isDeleted&&RoutineState.suspended(it)&&it.date>=date.toString()}.forEach{dao.update(RoutineState.resumeOccurrence(it))}
   generate(db.seriesDao().byId(id)!!,date,date.plusDays(400))
  }
 }}
 suspend fun edit(old:Plan,proposed:Plan,scope:EditScope)=lock.withLock{db.withTransaction{
  val current=dao.byId(old.id) ?: error("Plan bulunamadı.")
  check(current.copy(focusSeconds=old.focusSeconds,extraData=FocusLedger.withoutLedger(current.extraData))==old.copy(extraData=FocusLedger.withoutLedger(old.extraData))){"Plan öneriden sonra değişmiş. Güncel bilgileri kontrol edip yeniden dene."}
  val p=FocusLedger.retainLedger(proposed,current)
  require(PlanTime.validate(p)==null){PlanTime.validate(p).orEmpty()}
  if(scope!=EditScope.THIS&&p.repeatUntil!=null)require(p.repeatUntil>=p.date){"Tekrar bitişi yeni ilk günden önce olamaz."}
  val s=if(old.seriesId==0L)null else db.seriesDao().byId(old.seriesId)
  if(s==null){
   if(p.repeatType=="none"){dao.update(p.copy(id=old.id,uid=old.uid))}else{
    val sid=db.seriesDao().insert(PlanSeries(template=PlanJson.encode(p).toString(),anchor=p.date,untilDate=p.repeatUntil))
    dao.update(p.copy(id=old.id,uid=RecordIdentity.occurrence(db.seriesDao().byId(sid)!!.uid,p.date),seriesId=sid,occurrenceKey="$sid:${p.date}",originalDate=p.date))
    generate(db.seriesDao().byId(sid)!!,LocalDate.parse(p.date),maxOf(LocalDate.now().plusDays(400),LocalDate.parse(p.date).plusDays(60)))
   }
  }else if(scope==EditScope.THIS){dao.update(p.copy(id=old.id,uid=old.uid,seriesId=old.seriesId,occurrenceKey=old.occurrenceKey,originalDate=old.originalDate,repeatType=old.repeatType))}
  else{
   val from=if(scope==EditScope.ALL)LocalDate.parse(s.anchor)else LocalDate.parse(old.originalDate?:old.date)
   val shift=java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(old.date),LocalDate.parse(p.date))
   val all=dao.bySeries(s.id);db.seriesDao().update(s.copy(untilDate=from.minusDays(1).toString()))
   all.filter{LocalDate.parse(it.originalDate?:it.date)>=from&&!it.isCompleted&&!it.isSkipped&&PlanOutcome.status(it)!=PlanOutcome.Status.PARTIAL}.forEach{dao.update(it.copy(isDeleted=true))}
   val anchor=from.plusDays(shift)
   val fresh=PlanOutcome.fresh(PlanTime.moved(p,anchor)).copy(id=0,uid=UUID.randomUUID().toString(),seriesId=0,occurrenceKey=null,originalDate=null,isCompleted=false,isSkipped=false,isDeleted=false,focusSeconds=0)
   if(fresh.repeatType=="none")dao.insert(fresh)else{
    val id=db.seriesDao().insert(PlanSeries(template=PlanJson.encode(fresh).toString(),anchor=anchor.toString(),untilDate=fresh.repeatUntil))
    // Completed/skipped history and deleted exceptions remain single occurrences.
    all.filter{LocalDate.parse(it.originalDate?:it.date)>=from&&(it.isCompleted||it.isSkipped||it.isDeleted||PlanOutcome.status(it)==PlanOutcome.Status.PARTIAL)}.forEach{
     val d=LocalDate.parse(it.originalDate?:it.date).plusDays(shift);val key="$id:$d"
     dao.insert(PlanTime.moved(fresh,d).copy(uid=RecordIdentity.occurrence(db.seriesDao().byId(id)!!.uid,d.toString()),seriesId=id,occurrenceKey=key,originalDate=d.toString(),isDeleted=true))
    }
    generate(db.seriesDao().byId(id)!!,anchor,maxOf(LocalDate.now().plusDays(400),anchor.plusDays(60)))
   }
  }
 }}
}
class GoalRepository(private val db:AppDatabase) {
 private val dao=db.goalDao()
 fun observeAll()=dao.observeAll()
 suspend fun create(g:Goal):Long { require(g.title.isNotBlank() && g.targetCount>0);return dao.insert(g) }
 suspend fun adjust(id:Long,delta:Int)=db.withTransaction {
  val g=dao.byId(id) ?: return@withTransaction
  val count=(g.currentCount+delta).coerceIn(0,g.targetCount)
  dao.update(g.copy(currentCount=count,isCompleted=count>=g.targetCount))
 }
 suspend fun update(g:Goal)=dao.update(g)
 suspend fun delete(g:Goal)=db.withTransaction {
  db.planDao().all().filter{it.goalId==g.id}.forEach{db.planDao().update(it.copy(goalId=null))}
  db.seriesDao().all().forEach{s->val p=PlanJson.decode(JSONObject(s.template));if(p.goalId==g.id)db.seriesDao().update(s.copy(template=PlanJson.encode(p.copy(goalId=null)).toString()))}
  dao.delete(g)
 }
 suspend fun clear()=dao.clear()
}
