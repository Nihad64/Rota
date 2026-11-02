package com.nihad64.rotam.notification
import android.content.Context
import androidx.work.*
import com.nihad64.rotam.RotamApp
import java.util.concurrent.TimeUnit
object MissedPlanManager {fun enqueue(context:Context){WorkManager.getInstance(context).enqueueUniquePeriodicWork("rotam-maintenance",ExistingPeriodicWorkPolicy.UPDATE,PeriodicWorkRequestBuilder<ReminderMaintenance>(6,TimeUnit.HOURS).build())}}
class ReminderMaintenance(context:Context,params:WorkerParameters):CoroutineWorker(context,params){override suspend fun doWork():Result=try{RotamApp.container.initialize();RotamApp.container.scheduler.rescheduleAll();Result.success()}catch(_:Exception){Result.retry()}}
