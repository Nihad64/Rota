package com.rota.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.Calendar;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        String action=intent.getAction();
        if(!Intent.ACTION_BOOT_COMPLETED.equals(action)&&!Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)&&
           !"android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED".equals(action))return;
        android.content.SharedPreferences s=context.getSharedPreferences("rotam_settings",Context.MODE_PRIVATE);
        if(!s.getBoolean("reminders",true))return;
        int mins=s.getInt("reminder_minutes",15);
        AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);if(am==null)return;
        for(Plan p:PlanStore.load(context)){
            if(p.done||Plan.STATUS_CANCELLED.equals(p.status)||!Plan.HOURLY.equals(p.type)||p.startTime==null||!p.startTime.matches("\\d{2}:\\d{2}"))continue;
            Calendar c=Calendar.getInstance();
            try{String[] d=p.date.split("-");c.set(Integer.parseInt(d[0]),Integer.parseInt(d[1])-1,Integer.parseInt(d[2]),
                Integer.parseInt(p.startTime.substring(0,2)),Integer.parseInt(p.startTime.substring(3)),0);c.set(Calendar.MILLISECOND,0);}catch(Exception e){continue;}
            c.add(Calendar.MINUTE,-mins);if(c.getTimeInMillis()<=System.currentTimeMillis())continue;
            Intent ai=new Intent(context,ReminderReceiver.class);ai.putExtra("title",p.title);ai.putExtra("planId",p.id);ai.putExtra("minutesBefore",mins);
            PendingIntent pi=PendingIntent.getBroadcast(context,(int)(p.id&0x7fffffff),ai,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            try{
                if(Build.VERSION.SDK_INT>=31&&am.canScheduleExactAlarms())am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,c.getTimeInMillis(),pi);
                else if(Build.VERSION.SDK_INT>=23)am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,c.getTimeInMillis(),pi);
                else am.set(AlarmManager.RTC_WAKEUP,c.getTimeInMillis(),pi);
            }catch(SecurityException e){try{am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,c.getTimeInMillis(),pi);}catch(Exception ignored){}}
        }
    }
}