package com.rota.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.Calendar;
import java.util.List;

public final class ReminderScheduler {
    private ReminderScheduler(){}

    public static boolean exactAlarmAllowed(Context context){
        if(Build.VERSION.SDK_INT<31)return true;
        AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        return am!=null&&am.canScheduleExactAlarms();
    }

    public static void schedule(Context context,Plan p,int minutesBefore,boolean enabled){
        cancel(context,p);
        if(!enabled||p==null||p.done||Plan.STATUS_CANCELLED.equals(p.status)||!Plan.HOURLY.equals(p.type)||p.startTime==null||!p.startTime.matches("\\d{2}:\\d{2}"))return;
        Calendar alarm=Calendar.getInstance();
        try{String[] d=p.date.split("-");alarm.set(Integer.parseInt(d[0]),Integer.parseInt(d[1])-1,Integer.parseInt(d[2]),
                Integer.parseInt(p.startTime.substring(0,2)),Integer.parseInt(p.startTime.substring(3)),0);alarm.set(Calendar.MILLISECOND,0);}catch(Exception e){return;}
        alarm.add(Calendar.MINUTE,-minutesBefore);if(alarm.getTimeInMillis()<=System.currentTimeMillis())return;
        Intent i=new Intent(context,ReminderReceiver.class);i.putExtra("title",p.title);i.putExtra("planId",p.id);i.putExtra("minutesBefore",minutesBefore);
        PendingIntent pi=PendingIntent.getBroadcast(context,(int)(p.id&0x7fffffff),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);if(am==null)return;
        try{
            if(Build.VERSION.SDK_INT>=31&&am.canScheduleExactAlarms())am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,alarm.getTimeInMillis(),pi);
            else if(Build.VERSION.SDK_INT>=23)am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,alarm.getTimeInMillis(),pi);
            else am.set(AlarmManager.RTC_WAKEUP,alarm.getTimeInMillis(),pi);
        }catch(SecurityException e){try{am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,alarm.getTimeInMillis(),pi);}catch(Exception ignored){}}
    }

    public static void cancel(Context context,Plan p){
        if(p==null)return;
        Intent i=new Intent(context,ReminderReceiver.class);
        PendingIntent pi=PendingIntent.getBroadcast(context,(int)(p.id&0x7fffffff),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);if(am!=null)am.cancel(pi);
        try{pi.cancel();}catch(Exception ignored){}
    }

    public static void rescheduleAll(Context context,List<Plan> plans){
        android.content.SharedPreferences s=context.getSharedPreferences("rotam_settings",Context.MODE_PRIVATE);
        boolean enabled=s.getBoolean("reminders",true);int mins=s.getInt("reminder_minutes",15);
        for(Plan p:plans)schedule(context,p,mins,enabled);
    }
}
