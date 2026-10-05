package com.rota.app;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Calendar;
import java.util.List;

public final class MissedPlanManager {
    public interface LaterCallback { void reopen(Plan plan); }
    private static final int BG=Color.rgb(14,16,20),SURFACE2=Color.rgb(32,36,43),TEXT=Color.rgb(245,245,245),MUTED=Color.rgb(157,163,172),ORANGE=Color.rgb(255,139,0),GREEN=Color.rgb(70,175,112),RED=Color.rgb(210,68,75);
    private static boolean showing;
    private MissedPlanManager(){}

    public static void check(Activity a,List<Plan> plans,Runnable refresh,LaterCallback later){
        if(showing)return;Plan target=null;long now=System.currentTimeMillis();
        for(Plan p:plans){
            if(p.done||Plan.STATUS_DONE.equals(p.status)||Plan.STATUS_CANCELLED.equals(p.status))continue;
            long end=endMillis(p);
            if(end>0&&end<now){target=p;break;}
        }
        if(target!=null)show(a,plans,target,refresh,later);
    }

    private static long endMillis(Plan p){
        Calendar c=Calendar.getInstance();
        try{String[] d=p.date.split("-");c.set(Integer.parseInt(d[0]),Integer.parseInt(d[1])-1,Integer.parseInt(d[2]),0,0,0);c.set(Calendar.MILLISECOND,0);}catch(Exception e){return 0;}
        if(Plan.ALL_DAY.equals(p.type))return c.getTimeInMillis()+86400000L-1;
        if(p.endTime!=null&&p.endTime.matches("\\d{2}:\\d{2}")){c.set(Calendar.HOUR_OF_DAY,Integer.parseInt(p.endTime.substring(0,2)));c.set(Calendar.MINUTE,Integer.parseInt(p.endTime.substring(3)));return c.getTimeInMillis();}
        if(p.startTime!=null&&p.startTime.matches("\\d{2}:\\d{2}")){c.set(Calendar.HOUR_OF_DAY,Integer.parseInt(p.startTime.substring(0,2)));c.set(Calendar.MINUTE,Integer.parseInt(p.startTime.substring(3)));return c.getTimeInMillis()+3600000L;}
        return 0;
    }

    private static void show(Activity a,List<Plan> plans,Plan p,Runnable refresh,LaterCallback later){
        showing=true;if(!Plan.STATUS_PENDING_REVIEW.equals(p.status)){p.status=Plan.STATUS_PENDING_REVIEW;PlanStore.save(a,plans);}
        Dialog d=new Dialog(a);d.setCancelable(false);LinearLayout box=col(a);box.setPadding(dp(a,20),dp(a,18),dp(a,20),dp(a,20));box.setBackground(round(BG,28));
        box.addView(t(a,"Bu planı ne yaptın?",24,TEXT,true));
        box.addView(t(a,"Planın zamanı geçti. Rotam kendiliğinden tamamlanmış saymıyor.",12,MUTED,false),mt(a,5));
        LinearLayout info=col(a);info.setPadding(dp(a,15),dp(a,14),dp(a,15),dp(a,14));info.setBackground(round(SURFACE2,18));
        info.addView(t(a,p.title,18,TEXT,true));info.addView(t(a,p.date+" · "+safe(p.startTime)+" – "+safe(p.endTime),11,ORANGE,true),mt(a,5));box.addView(info,mt(a,13));
        LinearLayout r1=row(a),r2=row(a);TextView done=btn(a,"✓  Tamamladım",Color.WHITE,GREEN),miss=btn(a,"✕  Yapamadım",Color.WHITE,RED),later=btn(a,"↻  Sonra yapacağım",TEXT,SURFACE2),cancel=btn(a,"İptal",Color.rgb(255,150,150),SURFACE2);
        r1.addView(done,weight(a,1,52));gap(r1,a,7);r1.addView(miss,weight(a,1,52));r2.addView(later,weight(a,1,52));gap(r2,a,7);r2.addView(cancel,weight(a,1,52));
        box.addView(r1,mt(a,14));box.addView(r2,mt(a,7));
        done.setOnClickListener(v->{p.done=true;p.status=Plan.STATUS_DONE;ReminderScheduler.cancel(a,p);finish(d,a,plans,refresh);});
        miss.setOnClickListener(v->{p.done=false;p.status=Plan.STATUS_MISSED;finish(d,a,plans,refresh);});
        later.setOnClickListener(v->{d.dismiss();showing=false;p.status=Plan.STATUS_DEFERRED;PlanStore.save(a,plans);if(later!=null)later.reopen(p);});
        cancel.setOnClickListener(v->{ReminderScheduler.cancel(a,p);plans.remove(p);finish(d,a,plans,refresh);});
        d.setContentView(box);d.show();fit(d,a,.94f);
    }

    private static void finish(Dialog d,Activity a,List<Plan> plans,Runnable refresh){PlanStore.save(a,plans);d.dismiss();showing=false;if(refresh!=null)refresh.run();new android.os.Handler(a.getMainLooper()).postDelayed(() -> check(a,plans,refresh,later),250);}
    private static String safe(String s){return s==null||s.isEmpty()?"":s;}
    private static LinearLayout col(Activity a){LinearLayout l=new LinearLayout(a);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private static LinearLayout row(Activity a){LinearLayout l=col(a);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private static TextView t(Activity a,String s,float z,int c,boolean b){TextView v=new TextView(a);v.setText(s);v.setTextSize(z);v.setTextColor(c);v.setTypeface(Typeface.create("sans",b?Typeface.BOLD:Typeface.NORMAL));v.setIncludeFontPadding(false);return v;}
    private static TextView btn(Activity a,String s,int fg,int bg){TextView v=t(a,s,13,fg,true);v.setGravity(Gravity.CENTER);v.setBackground(round(bg,17));return v;}
    private static LinearLayout.LayoutParams weight(Activity a,float w,int h){return new LinearLayout.LayoutParams(0,dp(a,h),w);}
    private static LinearLayout.LayoutParams mt(Activity a,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(a,top);return p;}
    private static void gap(LinearLayout l,Activity a,int w){l.addView(new android.view.View(a),new LinearLayout.LayoutParams(dp(a,w),1));}
    private static int dp(Activity a,int v){return (int)(v*a.getResources().getDisplayMetrics().density+.5f);}
    private static android.graphics.drawable.GradientDrawable round(int c,int r){android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable();g.setColor(c);g.setCornerRadius(r);return g;}
    private static void fit(Dialog d,Activity a,float f){if(d.getWindow()!=null){d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);d.getWindow().setLayout((int)(a.getResources().getDisplayMetrics().widthPixels*f),-2);}}
    private static void toast(Activity a,String s){android.widget.Toast.makeText(a,s,android.widget.Toast.LENGTH_SHORT).show();}
}
