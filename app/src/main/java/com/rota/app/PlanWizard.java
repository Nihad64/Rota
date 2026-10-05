package com.rota.app;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import org.json.JSONObject;

public final class PlanWizard {
    public interface Callback { void saved(); }
    private PlanWizard(){}

    private static final int BG=Color.rgb(14,16,20), SURFACE=Color.rgb(24,27,33), SURFACE2=Color.rgb(32,36,43);
    private static final int TEXT=Color.rgb(245,245,245), MUTED=Color.rgb(157,163,172), ORANGE=Color.rgb(255,139,0);
    private static final Locale TR=new Locale("tr","TR");

    private static class Draft {
        String key,title,icon,date,type=Plan.HOURLY,start="",end="",repeat=Plan.REPEAT_NONE;
        JSONObject details=new JSONObject();
        Draft(String key,String title,String icon,String date){this.key=key;this.title=title;this.icon=icon;this.date=date;}
    }

    public static void show(Activity a,List<Plan> plans,String initialDate,Callback callback){
        Draft d=new Draft("","","",initialDate);
        showCategory(a,plans,d,callback);
    }

    private static void showCategory(Activity a,List<Plan> plans,Draft draft,Callback callback){
        final Dialog dialog=dialog(a);LinearLayout box=box(a);
        header(a,box,"Ne planlıyoruz?","Kategori seç. Rotam sonraki adımları buna göre sadeleştirir.",dialog);
        GridLayout grid=new GridLayout(a);grid.setColumnCount(2);
        String[][] cats={{"📚","Kitap / Okuma","Kitap, Risale, Kur'an","book"},{"💼","İş","Çalışma ve vardiya","work"},{"🎬","Video Edit","Aşamalarıyla proje","video"},{"🏋️","Spor","Gym, yürüyüş, antrenman","sport"},{"🕌","İbadet","Oruç, zikir, dua","faith"},{"📍","Randevu","Doktor, dişçi, görüşme","appointment"},{"🍳","Yemek","Hazırlık ve meal prep","food"},{"◉","Kişisel","Günlük işler","personal"}};
        for(String[] c:cats){
            LinearLayout card=categoryCard(a,c[0],c[1],c[2]);
            GridLayout.LayoutParams gp=new GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED,1f),GridLayout.spec(GridLayout.UNDEFINED,1f));
            gp.width=0;gp.height=dp(a,94);gp.setMargins(dp(a,4),dp(a,4),dp(a,4),dp(a,4));grid.addView(card,gp);
            card.setOnClickListener(v->{draft.key=c[3];draft.title="";draft.icon=c[0];draft.details=new JSONObject();dialog.dismiss();showDetails(a,plans,draft,callback);});
        }
        box.addView(grid,mt(a,10));dialog.setContentView(box);dialog.show();fit(dialog,a,.96f);
    }

    private static void showDetails(Activity a,List<Plan> plans,Draft d,Callback callback){
        final Dialog dialog=dialog(a);LinearLayout box=box(a);
        header(a,box,d.icon+"  "+dLabel(d.key),"1 / 3  ·  Bu kategoriye özel bilgiler.",dialog);
        List<EditText> fields=new ArrayList<>();String[] labels=labels(d.key);
        for(int i=0;i<labels.length;i++){
            EditText e=input(a,labels[i]);
            if("video".equals(d.key)&&i==1){e.setSingleLine(false);e.setMinLines(5);e.setGravity(Gravity.TOP|Gravity.START);e.setPadding(dp(a,14),dp(a,12),dp(a,14),dp(a,12));}
            fields.add(e);box.addView(e,mt(a,i==0?14:8));
        }
        TextView next=button(a,"Devam et",Color.WHITE,ORANGE);box.addView(next,mt(a,14));
        next.setOnClickListener(v->{String first=fields.get(0).getText().toString().trim();if(first.isEmpty()){fields.get(0).setError("Gerekli");return;}d.title=first;
            try{for(int i=0;i<fields.size();i++)d.details.put("field"+i,fields.get(i).getText().toString().trim());}catch(Exception ignored){}
            dialog.dismiss();showTiming(a,plans,d,callback);
        });
        dialog.setContentView(box);dialog.show();fit(dialog,a,.94f);
    }

    private static String[] labels(String key){
        if("book".equals(key))return new String[]{"Kitap adı","Güncel sayfa","Bugün kaç sayfa okuyacaksın?"};
        if("work".equals(key))return new String[]{"İş yeri","Çalışma düzeni (ör. 14:00–22:00)","Ek bilgi"};
        if("video".equals(key))return new String[]{"Video / proje adı","Aşamalar (her satıra bir aşama)","Ek bilgi"};
        if("sport".equals(key))return new String[]{"Spor türü","Hedef veya süre","Ek bilgi"};
        if("faith".equals(key))return new String[]{"İbadet / hedef","Kitap veya detay","Hedef miktar / süre"};
        if("appointment".equals(key))return new String[]{"Randevu","Nerede / kiminle?","Yol için kaç dakika ayırmalıyım?","Not"};
        if("food".equals(key))return new String[]{"Yemek / hazırlık","Miktar / kaç kişilik","Ek bilgi"};
        return new String[]{"Görev","Ek bilgi"};
    }

    private static void showTiming(Activity a,List<Plan> plans,Draft d,Callback callback){
        final Dialog dialog=dialog(a);LinearLayout box=box(a);
        header(a,box,"Ne zaman?","2 / 3  ·  Tarih, saat ve tekrar.",dialog);
        final String[] date={d.date},start={d.start},end={d.end};
        box.addView(label(a,"Tarih"),mt(a,18));TextView dateB=choice(a,prettyDate(date[0]));box.addView(dateB,mt(a,7));
        dateB.setOnClickListener(v->{Calendar c=parse(date[0]);android.app.DatePickerDialog p=new android.app.DatePickerDialog(a,(view,y,m,day)->{Calendar x=Calendar.getInstance();x.set(y,m,day);date[0]=key(x);dateB.setText(prettyDate(date[0]));},c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH));p.setOnShowListener(x->{p.getButton(-1).setTextColor(ORANGE);p.getButton(-2).setTextColor(MUTED);});p.show();});
        box.addView(label(a,"Plan türü"),mt(a,15));LinearLayout tr=row(a);
        TextView timed=chip(a,"Saatli",true),all=chip(a,"Tüm gün",false);tr.addView(timed,weight(a,1,.0f,48));gap(tr,a,8);tr.addView(all,weight(a,1,.0f,48));box.addView(tr,mt(a,7));
        LinearLayout times=row(a);TextView sb=choice(a,start[0].isEmpty()?"Başlangıç":start[0]),eb=choice(a,end[0].isEmpty()?"Bitiş (isteğe bağlı)":end[0]);
        times.addView(sb,weight(a,1,.0f,68));gap(times,a,8);times.addView(eb,weight(a,1,.0f,68));box.addView(times,mt(a,12));
        box.addView(label(a,"Tekrar"),mt(a,15));TextView rep=choice(a,repeatLabel(d.repeat));box.addView(rep,mt(a,7));
        TextView note=txt(a,"Bitiş saati isteğe bağlı. Hatırlatma süresi Ayarlar'dan kontrol edilir.",10,MUTED,false);box.addView(note,mt(a,8));
        TextView next=button(a,"Özeti gör",Color.WHITE,ORANGE);box.addView(next,mt(a,14));

        sb.setOnClickListener(v->{showTime(a,start[0],x->{start[0]=x;sb.setText(x);});});
        eb.setOnClickListener(v->{showTime(a,end[0],x->{end[0]=x;eb.setText(x);});});
        timed.setOnClickListener(v->{d.type=Plan.HOURLY;setChip(timed,true);setChip(all,false);times.setVisibility(View.VISIBLE);});
        all.setOnClickListener(v->{d.type=Plan.ALL_DAY;start[0]="";end[0]="";setChip(timed,false);setChip(all,true);times.setVisibility(View.GONE);});
        rep.setOnClickListener(v->{String[] names={"Tek sefer","Her gün","Hafta içi","Her hafta"};String[] vals={Plan.REPEAT_NONE,Plan.REPEAT_DAILY,Plan.REPEAT_WEEKDAYS,Plan.REPEAT_WEEKLY};final Dialog rd=dialog(a);LinearLayout rb=box(a);header(a,rb,"Tekrar seç","Planın ne sıklıkla tekrarlansın?",rd);for(int i=0;i<names.length;i++){final String val=vals[i];TextView b=button(a,names[i],TEXT,SURFACE2);rb.addView(b,mt(a,7));b.setOnClickListener(x->{d.repeat=val;rep.setText(repeatLabel(val));rd.dismiss();});}rd.setContentView(rb);rd.show();fit(rd,a,.84f);});
        next.setOnClickListener(v->{if(!Plan.ALL_DAY.equals(d.type)&&start[0].isEmpty()){sb.performClick();return;}d.date=date[0];d.start=start[0];d.end=end[0];dialog.dismiss();showConfirm(a,plans,d,callback);});
        dialog.setContentView(box);dialog.show();fit(dialog,a,.93f);
    }

    private interface TimeDone{void done(String value);}
    private static void showTime(Activity a,String existing,TimeDone done){
        final Dialog dialog=dialog(a);LinearLayout box=box(a);header(a,box,"Saat seç","Kaydır; seçilen değer merkezde kalır.",dialog);
        Calendar c=Calendar.getInstance();if(existing!=null&&existing.matches("\\d{2}:\\d{2}")){c.set(Calendar.HOUR_OF_DAY,Integer.parseInt(existing.substring(0,2)));c.set(Calendar.MINUTE,Integer.parseInt(existing.substring(3,5)));}
        final int[] hm={c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE)};TextView preview=txt(a,String.format(Locale.GERMANY,"%02d:%02d",hm[0],hm[1]),40,TEXT,true);preview.setGravity(Gravity.CENTER);preview.setBackground(round(SURFACE2,22));box.addView(preview,mt(a,12));
        LinearLayout wheels=row(a);Wheel w1=new Wheel(a,24,hm[0],v->{hm[0]=v;preview.setText(fmt(hm));});Wheel w2=new Wheel(a,60,hm[1],v->{hm[1]=v;preview.setText(fmt(hm));});wheels.addView(w1,weight(a,1,.0f,220));TextView colon=txt(a,":",34,TEXT,true);colon.setGravity(Gravity.CENTER);wheels.addView(colon,lp(a,24,80));wheels.addView(w2,weight(a,1,.0f,220));box.addView(wheels,mt(a,8));
        LinearLayout labels=row(a);labels.addView(txt(a,"SAAT",9,MUTED,true),weight(a,1,.0f,20));gap(labels,a,24);labels.addView(txt(a,"DAKİKA",9,MUTED,true),weight(a,1,.0f,20));box.addView(labels);
        LinearLayout act=row(a);TextView cancel=button(a,"Vazgeç",MUTED,SURFACE2),save=button(a,"Saati seç",Color.WHITE,ORANGE);act.addView(cancel,weight(a,100,.0f,52));gap(act,a,8);act.addView(save,weight(a,1,.0f,52));box.addView(act,mt(a,12));
        cancel.setOnClickListener(v->dialog.dismiss());save.setOnClickListener(v->{done.done(fmt(hm));dialog.dismiss();});dialog.setContentView(box);dialog.show();fit(dialog,a,.92f);
    }

    private static void showConfirm(Activity a,List<Plan> plans,Draft d,Callback callback){
        final Dialog dialog=dialog(a);LinearLayout box=box(a);header(a,box,"Planın hazır","3 / 3  ·  Son kontrol.",dialog);
        LinearLayout card=column(a);card.setPadding(dp(a,16),dp(a,15),dp(a,16),dp(a,15));card.setBackground(round(SURFACE2,20));
        card.addView(txt(a,d.icon+"  "+dLabel(d.key),11,ORANGE,true));card.addView(txt(a,d.title,19,TEXT,true),mt(a,6));card.addView(txt(a,prettyDate(d.date),12,MUTED,false),mt(a,5));
        String when=Plan.ALL_DAY.equals(d.type)?"Tüm gün":d.start+(d.end.isEmpty()?"":" – "+d.end);card.addView(txt(a,"⏰  "+when,13,TEXT,true),mt(a,6));card.addView(txt(a,"↻  "+repeatLabel(d.repeat),12,MUTED,false),mt(a,4));
        try{if("book".equals(d.key))card.addView(txt(a,"📖  "+d.details.optString("field1","")+" · bugün "+d.details.optString("field2","")+" sayfa",12,MUTED,false),mt(a,7));
            if("video".equals(d.key))card.addView(txt(a,"Aşamalar: "+d.details.optString("field1","").replace("\\n"," · "),12,MUTED,false),mt(a,7));}catch(Exception ignored){}
        box.addView(card,mt(a,14));LinearLayout act=row(a);TextView back=button(a,"Geri",MUTED,SURFACE2),create=button(a,"Planı oluştur",Color.WHITE,ORANGE);act.addView(back,weight(a,92,.0f,54));gap(act,a,8);act.addView(create,weight(a,1,.0f,54));box.addView(act,mt(a,15));
        back.setOnClickListener(v->{dialog.dismiss();showTiming(a,plans,d,callback);});create.setOnClickListener(v->{createPlan(a,plans,d);dialog.dismiss();if(callback!=null)callback.saved();});
        dialog.setContentView(box);dialog.show();fit(dialog,a,.93f);
    }

    private static void createPlan(Activity a,List<Plan> plans,Draft d){
        Plan p=new Plan(System.currentTimeMillis(),d.date,d.title,d.type,d.start,Plan.durationBetween(d.start,d.end),false);
        p.startTime=Plan.ALL_DAY.equals(d.type)?"":d.start;p.endTime=Plan.ALL_DAY.equals(d.type)?"":d.end;p.time=p.startTime;p.repeat=d.repeat;p.category=d.key;p.details=d.details.toString();p.status=Plan.STATUS_PLANNED;
        plans.add(p);PlanStore.save(a,plans);ReminderScheduler.schedule(a,p,a.getSharedPreferences("rotam_settings",0).getInt("reminder_minutes",15),a.getSharedPreferences("rotam_settings",0).getBoolean("reminders",true));
    }

    private static Dialog dialog(Activity a){Dialog d=new Dialog(a);d.setCancelable(true);return d;}
    private static LinearLayout box(Activity a){LinearLayout b=column(a);b.setPadding(dp(a,18),dp(a,16),dp(a,20),dp(a,20));b.setBackground(round(BG,28));return b;}
    private static void header(Activity a,LinearLayout box,String title,String sub,Dialog d){LinearLayout h=row(a);LinearLayout t=column(a);t.addView(txt(a,title,25,TEXT,true));t.addView(txt(a,sub,11,MUTED,false),mt(a,5));h.addView(t,weight(a,1,.0f,60));TextView x=button(a,"×",TEXT,SURFACE);h.addView(x,lp(a,44,44));x.setOnClickListener(v->d.dismiss());box.addView(h);}
    private static LinearLayout categoryCard(Activity a,String icon,String title,String sub){LinearLayout c=column(a);c.setPadding(dp(a,13),dp(a,11),dp(a,10),dp(a,10));c.setBackground(round(SURFACE,19));c.addView(txt(a,icon,23,TEXT,false),lp(a,-1,30));c.addView(txt(a,title,14,TEXT,true),mt(a,3));c.addView(txt(a,sub,10,MUTED,false),mt(a,2));return c;}
    private static LinearLayout row(Activity a){LinearLayout l=column(a);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private static LinearLayout column(Activity a){LinearLayout l=new LinearLayout(a);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private static EditText input(Activity a,String hint){EditText e=new EditText(a);e.setSingleLine(true);e.setTextColor(TEXT);e.setHintTextColor(MUTED);e.setHint(hint);e.setTextSize(15);e.setPadding(dp(a,14),0,dp(a,14),0);e.setBackground(round(SURFACE2,18));return e;}
    private static TextView txt(Activity a,String s,float size,int color,boolean bold){TextView t=new TextView(a);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setTypeface(Typeface.create("sans",bold?Typeface.BOLD:Typeface.NORMAL));t.setIncludeFontPadding(false);return t;}
    private static TextView label(Activity a,String s){return txt(a,s.toUpperCase(TR),10,MUTED,true);}
    private static TextView button(Activity a,String s,int fg,int bg){TextView t=txt(a,s,13,fg,true);t.setGravity(Gravity.CENTER);t.setBackground(round(bg,17));return t;}
    private static TextView choice(Activity a,String s){TextView t=txt(a,s,14,TEXT,true);t.setGravity(Gravity.CENTER_VERTICAL);t.setPadding(dp(a,15),0,dp(a,15),0);t.setBackground(round(SURFACE2,17));return t;}
    private static TextView chip(Activity a,String s,boolean sel){return button(a,s,sel?Color.WHITE:TEXT,sel?ORANGE:SURFACE2);}
    private static void setChip(TextView t,boolean sel){t.setTextColor(sel?Color.WHITE:TEXT);t.setBackground(round(sel?ORANGE:SURFACE2,17));}
    private static LinearLayout.LayoutParams mt(Activity a,int px,boolean dummy){LinearLayout.LayoutParams p=lp(a,-1,-2);p.topMargin=dp(a,px);return p;}
    private static LinearLayout.LayoutParams mt(Activity a,int px){return margin(a,px);}
    private static LinearLayout.LayoutParams margin(Activity a,int top){LinearLayout.LayoutParams p=lp(a,-1,-2);p.topMargin=dp(a,top);return p;}
    private static LinearLayout.LayoutParams lp(Activity a,int w,int h){return new LinearLayout.LayoutParams(w<0?w:dp(a,w),h<0?h:dp(a,h));}
    private static LinearLayout.LayoutParams weight(Activity a,float wt,float unused,int h){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(a,h),wt);return p;}
    private static void gap(LinearLayout l,Activity a,int w){l.addView(new View(a),lp(a,w,1));}
    private static GradientDrawable round(int color,int r){android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable();g.setColor(color);g.setCornerRadius(r);return g;}
    private static void fit(Dialog d,Activity a,float f){Window w=d.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.setLayout((int)(a.getResources().getDisplayMetrics().widthPixels*f),-2);}}
    private static int dp(Activity a,int v){return (int)(v*a.getResources().getDisplayMetrics().density+.5f);}
    private static String dLabel(String k){if("book".equals(k))return "Kitap / Okuma";if("work".equals(k))return "İş";if("video".equals(k))return "Video Edit";if("sport".equals(k))return "Spor";if("faith".equals(k))return "İbadet";if("appointment".equals(k))return "Randevu";if("food".equals(k))return "Yemek";return "Kişisel";}
    private static String fmt(int[] hm){return String.format(Locale.GERMANY,"%02d:%02d",hm[0],hm[1]);}
    private static String repeatLabel(String v){if(Plan.REPEAT_DAILY.equals(v))return "Her gün";if(Plan.REPEAT_WEEKDAYS.equals(v))return "Hafta içi";if(Plan.REPEAT_WEEKLY.equals(v))return "Her hafta";return "Tek sefer";}
    private static Calendar parse(String k){Calendar c=Calendar.getInstance();try{String[] p=k.split("-");c.set(Integer.parseInt(p[0]),Integer.parseInt(p[1])-1,Integer.parseInt(p[2]));}catch(Exception ignored){}return c;}
    private static String key(Calendar c){return String.format(Locale.GERMANY,"%04d-%02d-%02d",c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH));}
    private static String prettyDate(String k){return new SimpleDateFormat("EEEE · d MMMM yyyy",TR).format(parse(k).getTime());}

    private interface ValueChange{void value(int v);}
    private static class Wheel extends View{
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final int max;private int value;private float off,lastY;private final ValueChange cb;
        Wheel(Activity a,int max,int value,ValueChange cb){super(a);this.max=max;this.value=value;this.cb=cb;setBackground(round(SURFACE2,20));}
        private int rh(){return (int)(52*getResources().getDisplayMetrics().density);}
        protected void onDraw(Canvas c){super.onDraw(c);float center=getHeight()/2f,r=rh();for(int i=-2;i<=2;i++){float y=center+i*r+off;float dist=Math.abs(y-center);p.setAlpha((int)(255*Math.max(.2,1-dist/(r*2.5))));p.setColor(dist<r*.45?TEXT:MUTED);p.setTypeface(Typeface.create("sans",dist<r*.45?Typeface.BOLD:Typeface.NORMAL));p.setTextSize((int)((dist<r*.45?31:24)*getResources().getDisplayMetrics().density));String s=String.format(Locale.GERMANY,"%02d",wrap(value+i));Paint.FontMetrics fm=p.getFontMetrics();c.drawText(s,getWidth()/2f-p.measureText(s)/2f,y-(fm.ascent+fm.descent)/2f,p);}p.setAlpha(255);p.setColor(ORANGE);p.setStrokeWidth(2*getResources().getDisplayMetrics().density);c.drawLine(10,center-r/2,getWidth()-10,center-r/2,p);c.drawLine(10,center+r/2,getWidth()-10,center+r/2,p);}
        private int wrap(int n){int x=n%max;return x<0?x+max:x;}
        private void consume(){int r=rh();while(off<=-r){off+=r;value=wrap(value+1);cb.value(value);}while(off>=r){off-=r;value=wrap(value-1);cb.value(value);}}
        private void snap(){int r=rh();int dir=Math.abs(off)>r/2?(off<0?1:-1):0;if(dir!=0){value=wrap(value+dir);cb.value(value);}android.animation.ValueAnimator a=android.animation.ValueAnimator.ofFloat(off,0);a.setDuration(150);a.setInterpolator(new android.view.animation.DecelerateInterpolator());a.addUpdateListener(v->{off=(float)v.getAnimatedValue();invalidate();});a.start();}
        public boolean onTouchEvent(MotionEvent e){if(e.getActionMasked()==MotionEvent.ACTION_DOWN){lastY=e.getY();return true;}if(e.getActionMasked()==MotionEvent.ACTION_MOVE){float dy=e.getY()-lastY;lastY=e.getY();off+=dy;consume();invalidate();return true;}if(e.getActionMasked()==MotionEvent.ACTION_UP){snap();return true;}return true;}
    }
}
