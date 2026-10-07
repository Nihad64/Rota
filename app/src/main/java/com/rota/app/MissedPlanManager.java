package com.rota.app;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Calendar;
import java.util.List;

public final class MissedPlanManager {
    public interface LaterCallback { void reopen(Plan plan); }

    private static final int BG=Color.rgb(10,12,16),SURFACE2=Color.rgb(25,30,38),TEXT=Color.rgb(247,248,250);
    private static final int MUTED=Color.rgb(145,153,165),ORANGE=Color.rgb(255,139,0),GREEN=Color.rgb(62,190,125),RED=Color.rgb(225,82,89);
    private static boolean showing;

    private MissedPlanManager(){}

    public static void check(Activity a,List<Plan> plans,Runnable refresh,LaterCallback later){
        if(showing)return;
        Plan target=null; long latest=Long.MIN_VALUE; long now=System.currentTimeMillis();
        for(Plan p:plans){
            if(p.done||Plan.STATUS_DONE.equals(p.status)||Plan.STATUS_CANCELLED.equals(p.status)||Plan.STATUS_MISSED.equals(p.status))continue;
            long end=endMillisForExternal(p);
            if(end>0&&end<now&&end>latest){latest=end;target=p;}
        }
        if(target!=null)show(a,plans,target,refresh,later);
    }

    public static long endMillisForExternal(Plan p){
        if(p==null||p.date==null||p.date.isEmpty())return 0;
        Calendar c=Calendar.getInstance();
        try{
            String[] d=p.date.split("-");
            if(d.length!=3)return 0;
            c.set(Integer.parseInt(d[0]),Integer.parseInt(d[1])-1,Integer.parseInt(d[2]),0,0,0);
            c.set(Calendar.MILLISECOND,0);
        }catch(Exception e){return 0;}
        if(Plan.ALL_DAY.equals(p.type))return c.getTimeInMillis()+86400000L-1;
        if(p.endTime!=null&&p.endTime.matches("\\d{2}:\\d{2}")){
            c.set(Calendar.HOUR_OF_DAY,Integer.parseInt(p.endTime.substring(0,2)));
            c.set(Calendar.MINUTE,Integer.parseInt(p.endTime.substring(3)));
            return c.getTimeInMillis();
        }
        if(p.startTime!=null&&p.startTime.matches("\\d{2}:\\d{2}")){
            c.set(Calendar.HOUR_OF_DAY,Integer.parseInt(p.startTime.substring(0,2)));
            c.set(Calendar.MINUTE,Integer.parseInt(p.startTime.substring(3)));
            return c.getTimeInMillis()+3600000L;
        }
        return 0;
    }

    public static void showExternal(Activity a,List<Plan> plans,Plan p,Runnable refresh){
        show(a,plans,p,refresh,null);
    }

    private static void show(Activity a,List<Plan> plans,Plan p,Runnable refresh,LaterCallback later){
        showing=true;
        p.status=Plan.STATUS_PENDING_REVIEW;
        PlanStore.save(a,plans);

        Dialog d=new Dialog(a);
        d.setCancelable(false);
        LinearLayout box=box(a);

        box.addView(t(a,"Planın zamanı geçti",22,TEXT,true));
        box.addView(t(a,"Rotam tamamlandı varsaymaz. Ne olduğunu seç.",11,MUTED,false),mt(a,4));

        LinearLayout info=col(a);
        info.setPadding(dp(a,14),dp(a,13),dp(a,14),dp(a,13));
        info.setBackground(round(a,SURFACE2,18));
        info.addView(t(a,p.title,17,TEXT,true));
        String time=Plan.ALL_DAY.equals(p.type)?"Tüm gün":safe(p.startTime)+(safe(p.endTime).isEmpty()?"":" – "+safe(p.endTime));
        info.addView(t(a,prettyDate(p.date)+"  ·  "+time,10,ORANGE,true),mt(a,5));
        box.addView(info,mt(a,13));

        TextView done=btn(a,"✓  Tamamladım",Color.WHITE,GREEN);
        TextView missed=btn(a,"✕  Yapamadım",Color.WHITE,RED);
        TextView laterBtn=btn(a,"↻  Yeniden planla",TEXT,SURFACE2);
        TextView remove=btn(a,"Sil",MUTED,SURFACE2);
        box.addView(done,mt(a,14));box.addView(missed,mt(a,7));box.addView(laterBtn,mt(a,7));box.addView(remove,mt(a,7));

        done.setOnClickListener(v->{p.done=true;p.status=Plan.STATUS_DONE;ReminderScheduler.cancel(a,p);finish(d,a,plans,refresh);});
        missed.setOnClickListener(v->{p.done=false;p.status=Plan.STATUS_MISSED;ReminderScheduler.cancel(a,p);finish(d,a,plans,refresh);});
        laterBtn.setOnClickListener(v->{d.dismiss();showing=false;p.status=Plan.STATUS_DEFERRED;PlanStore.save(a,plans);if(later!=null)later.reopen(p);});
        remove.setOnClickListener(v->{plans.remove(p);ReminderScheduler.cancel(a,p);finish(d,a,plans,refresh);});

        d.setContentView(box);d.show();fit(d,a);
    }

    private static void finish(Dialog d,Activity a,List<Plan> plans,Runnable refresh){
        PlanStore.save(a,plans);d.dismiss();showing=false;if(refresh!=null)refresh.run();
    }

    private static String prettyDate(String s){
        try{
            String[] p=s.split("-");Calendar c=Calendar.getInstance();
            c.set(Integer.parseInt(p[0]),Integer.parseInt(p[1])-1,Integer.parseInt(p[2]));
            return new java.text.SimpleDateFormat("d MMMM",new java.util.Locale("tr","TR")).format(c.getTime());
        }catch(Exception e){return safe(s);}
    }
    private static String safe(String s){return s==null?"":s;}

    private static LinearLayout box(Activity a){LinearLayout b=col(a);b.setPadding(dp(a,18),dp(a,18),dp(a,18),dp(a,18));b.setBackground(round(a,BG,28));return b;}
    private static LinearLayout col(Activity a){LinearLayout l=new LinearLayout(a);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private static TextView t(Activity a,String s,float z,int c,boolean bold){TextView v=new TextView(a);v.setText(s);v.setTextSize(z);v.setTextColor(c);v.setTypeface(Typeface.create("sans",bold?Typeface.BOLD:Typeface.NORMAL));v.setIncludeFontPadding(false);return v;}
    private static TextView btn(Activity a,String s,int fg,int bg){TextView v=t(a,s,13,fg,true);v.setGravity(Gravity.CENTER);v.setBackground(round(a,bg,16));return v;}
    private static LinearLayout.LayoutParams mt(Activity a,int n){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(a,n);return p;}
    private static int dp(Activity a,int n){return (int)(n*a.getResources().getDisplayMetrics().density+.5f);}
    private static android.graphics.drawable.GradientDrawable round(Activity a,int c,int r){android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable();g.setColor(c);g.setCornerRadius(dp(a,r));return g;}
    private static void fit(Dialog d,Activity a){Window w=d.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.setLayout((int)(a.getResources().getDisplayMetrics().widthPixels*.92f),(int)(a.getResources().getDisplayMetrics().heightPixels*.60f));}}
    private static LinearLayout row(Activity a){LinearLayout l=col(a);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
}
