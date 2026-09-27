package com.rota.app
import android.app.NotificationChannel
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
class ReminderReceiver:BroadcastReceiver(){override fun onReceive(c:Context,i:Intent){val m=c.getSystemService(android.app.NotificationManager::class.java);m.createNotificationChannel(NotificationChannel("rota","Rota",NotificationChannel.IMPORTANCE_HIGH));m.notify((System.currentTimeMillis()%100000).toInt(),NotificationCompat.Builder(c,"rota").setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle(i.getStringExtra("title")?:"Rota").setContentText("Planın başladı.").setAutoCancel(true).build())}}
