package com.rota.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        NotificationManager manager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);if(manager==null)return;
        if(Build.VERSION.SDK_INT>=26){NotificationChannel ch=new NotificationChannel("rota_reminders","Rotam plan bildirimleri",NotificationManager.IMPORTANCE_HIGH);ch.setDescription("Plan ve hatırlatmalar");ch.enableVibration(true);manager.createNotificationChannel(ch);}
        String title=intent.getStringExtra("title");if(title==null||title.trim().isEmpty())title="Rotam";
        int minutes=intent.getIntExtra("minutesBefore",0);String msg=minutes>0?minutes+" dakika sonra planın başlayacak.":"Planının zamanı geldi.";
        Intent open=new Intent(context,MainActivity.class);open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi=PendingIntent.getActivity(context,1001,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder nb=Build.VERSION.SDK_INT>=26?new Notification.Builder(context,"rota_reminders"):new Notification.Builder(context);
        Notification n=nb.setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(msg).setContentIntent(pi).setAutoCancel(true).setDefaults(Notification.DEFAULT_ALL).setCategory(Notification.CATEGORY_REMINDER).build();
        manager.notify((int)(System.currentTimeMillis()&0x7fffffff),n);
    }
}
