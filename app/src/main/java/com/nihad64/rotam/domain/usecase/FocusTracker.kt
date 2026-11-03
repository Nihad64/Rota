package com.nihad64.rotam.domain.usecase

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import com.nihad64.rotam.data.repository.PlanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** A single persisted session. Monotonic time survives process death; reboot pauses it. */
class FocusTracker(context: Context, private val repo: PlanRepository) {
    private val prefs=context.getSharedPreferences("rotam_focus",0)
    private val boot=Settings.Global.getInt(context.contentResolver,Settings.Global.BOOT_COUNT,0)
    private val lock=Mutex()
    private val mutable=MutableStateFlow<Long?>(null)
    val active=mutable.asStateFlow()
    init {
        if(prefs.getInt("boot",-1)==boot) mutable.value=prefs.getLong("id",0).takeIf{it>0}
        else prefs.edit().clear().commit()
    }
    fun seconds(id: Long, saved: Long): Long {
        if(mutable.value!=id)return saved
        return maxOf(saved,prefs.getLong("base",saved)+(SystemClock.elapsedRealtime()-prefs.getLong("since",SystemClock.elapsedRealtime())).coerceAtLeast(0)/1000)
    }
    suspend fun start(id: Long)=lock.withLock {
        if(mutable.value==id)return@withLock
        pauseLocked()
        val p=repo.byId(id) ?: return@withLock
        if(p.isDeleted||p.isCompleted||p.isSkipped)return@withLock
        check(prefs.edit().putLong("id",id).putLong("base",p.focusSeconds).putLong("since",SystemClock.elapsedRealtime()).putInt("boot",boot).commit())
        mutable.value=id
    }
    suspend fun pauseIf(id:Long)=lock.withLock { if(mutable.value==id)pauseLocked() }
    suspend fun pause()=lock.withLock { pauseLocked() }
    private suspend fun pauseLocked() {
        val id=mutable.value ?: return
        val p=repo.byId(id)
        if(p!=null)repo.setFocusSeconds(id,seconds(id,p.focusSeconds))
        check(prefs.edit().clear().commit())
        mutable.value=null
    }
}
