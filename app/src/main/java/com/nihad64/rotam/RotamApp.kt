package com.nihad64.rotam

import android.app.Application
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.nihad64.rotam.data.db.LegacyMigration
import com.nihad64.rotam.data.db.AppDatabase
import com.nihad64.rotam.data.prefs.SettingsStore
import com.nihad64.rotam.data.repository.GoalRepository
import com.nihad64.rotam.data.repository.PlanRepository
import com.nihad64.rotam.domain.usecase.*
import com.nihad64.rotam.notification.MissedPlanManager
import com.nihad64.rotam.notification.Notifications
import com.nihad64.rotam.notification.ReminderScheduler
import com.nihad64.rotam.ui.ai.GeminiService

data class Notice(val text:String,val undo:(suspend ()->Unit)?=null)

/** Basit manuel bağımlılık kabı (Hilt yerine; küçük proje için daha sade). */
class AppContainer(val app: Application) {
    val notices = kotlinx.coroutines.channels.Channel<Notice>(kotlinx.coroutines.channels.Channel.BUFFERED)
    suspend fun offerUndo(message:String, before:com.nihad64.rotam.data.model.Plan) {
        val expected=planRepo.byId(before.id)
        notices.send(Notice(message) {
            if(planRepo.byId(before.id)==expected){planRepo.restore(if(expected==null)before else FocusLedger.retainLedger(before,expected));scheduler.rescheduleAll()}
        })
    }
    val ready = MutableStateFlow(false)
    val startupError = MutableStateFlow<String?>(null)
    private val initLock = Mutex()
    suspend fun initialize() = initLock.withLock {
        if (ready.value) return@withLock
        try { LegacyMigration(app, db).run(); settings.migrate()
            planRepo.ensure(java.time.LocalDate.now().minusDays(30), java.time.LocalDate.now().plusDays(400))
            ready.value=true; startupError.value=null; sync.start(); preferenceSync.start()
            startReminderObserver()
        } catch (e:Exception) { startupError.value="Eski veriler aktarılırken işlem durdu. Kaynak veriler ve yedeği korundu. Yeniden deneyebilirsin."; throw e }
    }

    @OptIn(FlowPreview::class)
    private fun startReminderObserver() {
        CoroutineScope(SupervisorJob()+Dispatchers.IO).launch {
            combine(planRepo.observeAll().map { plans->plans.map{p->listOf(ReminderPolicy.signature(p),p.isDeleted,p.isCompleted,p.isSkipped,ReminderPolicy.started(p)).joinToString(":")}.sorted() }.distinctUntilChanged(),settings.settings.map{s->listOf(s.remindersEnabled,s.quietEnabled,s.quietStart,s.quietEnd)}.distinctUntilChanged()){a,b->a to b}.debounce(350).collect {
                try{scheduler.rescheduleAll()}catch(e:CancellationException){throw e}catch(_:Exception){scheduler.reportFailure()}
            }
        }
    }
    val db = AppDatabase.get(app)
    val settings = SettingsStore(app)
    val planRepo = PlanRepository(db)
    val goalRepo = GoalRepository(db)
    val focus = FocusTracker(app, planRepo)
    val scheduler = ReminderScheduler(app, planRepo, settings)
    val account = com.nihad64.rotam.data.account.AccountService(app)
    val memory = com.nihad64.rotam.data.repository.MemoryRepository(db)
    val gemini = GeminiService(settings, account,memory)
    val sync = com.nihad64.rotam.data.sync.SyncCoordinator(this)
    val preferenceSync=com.nihad64.rotam.data.sync.PreferenceSyncCoordinator(this)

    val getPlansForDay = GetPlansForDay(planRepo)
    val createPlan = CreatePlan(planRepo, scheduler)
    val completePlan = CompletePlan(planRepo, scheduler, focus)
    val skipPlan = SkipPlan(planRepo, scheduler, focus)
    val deletePlan = DeletePlan(planRepo, scheduler, focus)
    val movePlanToTomorrow = MovePlanToTomorrow(planRepo, scheduler, focus)
}

class RotamApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = AppContainer(this)
        Notifications.createChannels(this)
        MissedPlanManager.enqueue(this)
        CoroutineScope(SupervisorJob()+Dispatchers.IO).launch { try{container.initialize(); container.scheduler.rescheduleAll()}catch(e:CancellationException){throw e}catch(_:Exception){container.scheduler.reportFailure()} }
    }

    companion object {
        lateinit var instance: RotamApp
            private set
        val container: AppContainer get() = instance.container
    }
}
