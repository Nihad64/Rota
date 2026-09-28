package com.rota.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "rota_reminders",
                    "Rota hatırlatıcıları",
                    NotificationManager.IMPORTANCE_HIGH
            );
            manager.createNotificationChannel(channel);
        }

        String title = intent.getStringExtra("title");
        if (title == null || title.trim().isEmpty()) title = "Rota";

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, "rota_reminders")
                : new Notification.Builder(context);

        Notification notification = builder
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title)
                .setContentText("Planın başladı.")
                .setAutoCancel(true)
                .build();

        manager.notify((int) (System.currentTimeMillis() & 0x7fffffff), notification);
    }
}
