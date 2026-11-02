package com.nihad64.rotam.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object Notifications {
    const val CHANNEL_REMINDERS = "rotam_reminders"
    const val CHANNEL_MISSED = "rotam_missed"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS, "Plan hatırlatmaları", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Saatli planlardan önce bildirim" }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_MISSED, "Kaçırılan planlar", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Tamamlanmamış planlar için taşıma önerisi" }
        )
    }
}
