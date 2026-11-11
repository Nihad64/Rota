package com.nihad64.rotam.domain.usecase

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import com.nihad64.rotam.data.repository.PlanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class FocusSessionSummary(val planId:Long,val focusSeconds:Long,val breakSeconds:Long)

/** Monotonic clocks survive process death. A reboot pauses the session instead of inventing time. */
class FocusTracker(context:Context,private val repo:PlanRepository,private val now:()->Long={SystemClock.elapsedRealtime()}) {
 private val prefs=context.getSharedPreferences("rotam_focus",0)
 private val session=context.getSharedPreferences("rotam_focus_session",0)
 private val boot=Settings.Global.getInt(context.contentResolver,Settings.Global.BOOT_COUNT,0)
 private val lock=Mutex()
 private val mutable=MutableStateFlow<Long?>(null)
 val active=mutable.asStateFlow()
 init {
  if(prefs.getInt("boot",-1)==boot)mutable.value=prefs.getLong("id",0).takeIf{it>0}
  else check(prefs.edit().clear().commit())
  if(session.getInt("boot",boot)!=boot)check(session.edit().remove("break_since").putInt("boot",boot).commit())
  // Adopt an already running pre-update timer without losing its measured interval.
  if(mutable.value!=null&&session.getLong("id",0)!=mutable.value)check(session.edit().clear().putLong("id",mutable.value!!).putInt("boot",boot).commit())
 }
 private fun elapsed(since:Long)=(now()-since).coerceAtLeast(0)/1000
 fun seconds(id:Long,saved:Long):Long=if(mutable.value!=id)saved else maxOf(saved,prefs.getLong("base",saved)+elapsed(prefs.getLong("since",now())))
 fun sessionSeconds(id:Long):Long {
  if(session.getLong("id",0)!=id)return 0
  return session.getLong("focus",0)+if(mutable.value==id)elapsed(prefs.getLong("since",now()))else 0
 }
 fun breakSeconds(id:Long):Long {
  if(session.getLong("id",0)!=id)return 0
  return session.getLong("break",0)+if(session.contains("break_since"))elapsed(session.getLong("break_since",now()))else 0
 }
 fun summary(id:Long):FocusSessionSummary?=if(session.getLong("summary_id",0)==id)FocusSessionSummary(id,session.getLong("summary_focus",0),session.getLong("summary_break",0))else null
 suspend fun start(id:Long)=lock.withLock {
  if(mutable.value==id)return@withLock
  pauseLocked()
  val p=repo.byId(id)?:return@withLock
  if(p.isDeleted||p.isCompleted||p.isSkipped)return@withLock
  if(session.getLong("id",0)!=id)check(session.edit().clear().putLong("id",id).putInt("boot",boot).commit())
  else check(session.edit().putLong("break",breakSeconds(id)).remove("break_since").remove("summary_id").commit())
  check(prefs.edit().putLong("id",id).putLong("base",p.focusSeconds).putLong("since",now()).putLong("started_at",System.currentTimeMillis()).putString("segment_uid",java.util.UUID.randomUUID().toString()).putInt("boot",boot).commit())
  mutable.value=id
 }
 suspend fun pauseIf(id:Long)=lock.withLock{if(mutable.value==id)pauseLocked()}
 suspend fun pause()=lock.withLock{pauseLocked()}
 private suspend fun pauseLocked() {
  val id=mutable.value?:return
  val p=repo.byId(id)
  if(p!=null){
   val duration=elapsed(prefs.getLong("since",now()))
   val uid=prefs.getString("segment_uid",null)?:"legacy-focus:$boot:${prefs.getLong("since",0)}:${p.uid}"
   repo.recordFocusSegment(id,uid,prefs.getLong("started_at",System.currentTimeMillis()-duration*1000),duration,seconds(id,p.focusSeconds))
  }
  val measured=sessionSeconds(id)
  check(session.edit().putLong("id",id).putLong("focus",measured).putLong("break_since",now()).putInt("boot",boot).commit())
  check(prefs.edit().clear().commit());mutable.value=null
 }
 suspend fun finish(id:Long):FocusSessionSummary=lock.withLock {
  check(session.getLong("id",id)==id){"Önce diğer planın açık odak oturumunu bitir."}
  if(mutable.value==id)pauseLocked()
  val result=FocusSessionSummary(id,sessionSeconds(id),breakSeconds(id))
  check(session.edit().remove("id").remove("break_since").putLong("summary_id",id).putLong("summary_focus",result.focusSeconds).putLong("summary_break",result.breakSeconds).commit())
  result
 }
 suspend fun dismissSummary(id:Long)=lock.withLock {
  if(session.getLong("summary_id",0)==id)check(session.edit().remove("summary_id").commit())
 }
}
