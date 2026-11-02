package com.nihad64.rotam

import android.app.Application
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
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
            if(planRepo.byId(before.id)==expected){planRepo.restore(before.copy(focusSeconds=expected?.focusSeconds?:before.focusSeconds));scheduler.rescheduleAll()}
        })
    }
    val ready = MutableStateFlow(false)
    val startupError = MutableStateFlow<String?>(null)
    private val initLock = Mutex()
    suspend fun initialize() = initLock.withLock {
        if (ready.value) return@withLock
        try { LegacyMigration(app, db).run(); settings.migrate()
            planRepo.ensure(java.time.LocalDate.now().minusDays(30), java.time.LocalDate.now().plusDays(400))
            ready.value=true; startupError.value=null
        } catch (e:Exception) { startupError.value="Eski veriler aktarılırken işlem durdu. Kaynak veriler ve yedeği korundu. Yeniden deneyebilirsin."; throw e }
    }

    val db = AppDatabase.get(app)
    val settings = SettingsStore(app)
    val planRepo = PlanRepository(db)
    val goalRepo = GoalRepository(db)
    val focus = FocusTracker(app, planRepo)
    val scheduler = ReminderScheduler(app, planRepo, settings)
    val gemini = GeminiService(settings)

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
        CoroutineScope(SupervisorJob()+Dispatchers.IO).launch { runCatching { container.initialize(); container.scheduler.rescheduleAll() } }
    }

    companion object {
        lateinit var instance: RotamApp
            private set
        val container: AppContainer get() = instance.container
    }
}
