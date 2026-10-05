package com.rota.app;

public class Plan {
    public static final String HOURLY="Saatli";
    public static final String ALL_DAY="Tüm gün";
    public static final String FLEX="Esnek";
    public static final String REPEAT_NONE="none";
    public static final String REPEAT_DAILY="daily";
    public static final String REPEAT_WEEKDAYS="weekdays";
    public static final String REPEAT_WEEKLY="weekly";
    public static final String STATUS_PLANNED="planned";
    public static final String STATUS_DONE="done";
    public static final String STATUS_PENDING_REVIEW="pending_review";
    public static final String STATUS_MISSED="missed";
    public static final String STATUS_DEFERRED="deferred";
    public static final String STATUS_CANCELLED="cancelled";

    public long id; public String date,title,type,time,duration; public boolean done;
    public String startTime,endTime,note,repeat,category,details,status;

    public Plan(long id,String date,String title,String type,String time,String duration,boolean done){
        this.id=id;this.date=date==null?"":date;this.title=title==null?"":title;this.type=type==null?FLEX:type;
        this.time=time==null?"":time;this.duration=duration==null?"":duration;this.done=done;
        this.startTime=this.time;this.endTime="";this.note="";this.repeat=REPEAT_NONE;this.category="personal";this.details="";
        this.status=done?STATUS_DONE:STATUS_PLANNED;
    }

    public static String durationBetween(String start,String end){
        if(start==null||end==null||!start.matches("\\d{2}:\\d{2}")||!end.matches("\\d{2}:\\d{2}"))return "";
        try{
            int sh=Integer.parseInt(start.substring(0,2)),sm=Integer.parseInt(start.substring(3));
            int eh=Integer.parseInt(end.substring(0,2)),em=Integer.parseInt(end.substring(3));
            int m=(eh*60+em)-(sh*60+sm);if(m<=0)m+=1440;int h=m/60,mm=m%60;
            if(h>0&&mm>0)return h+"s "+mm+"dk";if(h>0)return h+"s";return mm+"dk";
        }catch(Exception e){return "";}
    }
}
