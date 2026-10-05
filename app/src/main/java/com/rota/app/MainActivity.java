package com.rota.app;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int BG=Color.rgb(12,14,18), SURFACE=Color.rgb(21,24,30), SURFACE2=Color.rgb(29,33,40), TEXT=Color.rgb(246,246,247), MUTED=Color.rgb(151,157,167), LINE=Color.rgb(49,54,63), ORANGE=Color.rgb(255,139,0), GREEN=Color.rgb(73,183,121), RED=Color.rgb(220,75,82);
    private final Locale TR=new Locale("tr","TR");
    private final List<Plan> plans=new ArrayList<>();
    private LinearLayout content;
    private int tab=0;
    private String selectedDate;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        selectedDate=key(Calendar.getInstance());
        plans.addAll(PlanStore.load(this));
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},100);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);
        buildShell();render();
    }

    @Override protected void onResume(){
        super.onResume();
        if(plans.isEmpty()==false) ReminderScheduler.rescheduleAll(this,plans);
        new android.os.Handler(getMainLooper()).postDelayed(()->MissedPlanManager.check(this,plans,this::render,this::reopenMissed),250);
    }

    private void buildShell(){
        LinearLayout root=column();root.setBackgroundColor(BG);
        root.setPadding(0,statusBar(),0,0);
        FrameLayout page=new FrameLayout(this);
        content=column();page.addView(content,new FrameLayout.LayoutParams(-1,-1));
        TextView ai=text("✦",27,Color.WHITE,true);ai.setGravity(Gravity.CENTER);ai.setBackground(round(ORANGE,32));ai.setElevation(dp(12));ai.setContentDescription("Rotam AI");
        FrameLayout.LayoutParams ap=new FrameLayout.LayoutParams(dp(64),dp(64),Gravity.BOTTOM|Gravity.END);ap.setMargins(0,0,dp(18),dp(18));page.addView(ai,ap);
        ai.setOnClickListener(v->{tap(v);openAi();});
        root.addView(page,new LinearLayout.LayoutParams(-1,0,1));root.addView(nav(),new LinearLayout.LayoutParams(-1,dp(76)));setContentView(root);
    }

    private int statusBar(){int id=getResources().getIdentifier("status_bar_height","dimen","android");return id==0?dp(24):getResources().getDimensionPixelSize(id);}

    private LinearLayout nav(){
        LinearLayout bar=row();bar.setPadding(dp(12),dp(8),dp(12),dp(8));bar.setBackgroundColor(BG);
        String[] labels={"Bugün","Planlar","Ayarlar"};int[] icons={R.drawable.ic_nav_home,R.drawable.ic_nav_calendar,R.drawable.ic_nav_settings};
        for(int i=0;i<3;i++){final int n=i;LinearLayout item=column();item.setGravity(Gravity.CENTER);ImageView im=new ImageView(this);im.setImageResource(icons[i]);im.setColorFilter(i==tab?ORANGE:MUTED);item.addView(im,new LinearLayout.LayoutParams(-1,dp(28)));TextView t=text(labels[i],11,i==tab?TEXT:MUTED,true);t.setGravity(Gravity.CENTER);item.addView(t,new LinearLayout.LayoutParams(-1,dp(23)));if(i==tab)item.setBackground(round(SURFACE,18));item.setOnClickListener(v->{tap(v);tab=n;render();});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(58),1);p.setMargins(dp(4),0,dp(4),0);bar.addView(item,p);}
        return bar;
    }

    private void render(){
        content.removeAllViews();
        if(tab==0)home();else if(tab==1)calendar();else settingsPage();
        LinearLayout root=(LinearLayout)content.getParent().getParent();if(root.getChildCount()>1){root.removeViewAt(1);root.addView(nav(),new LinearLayout.LayoutParams(-1,dp(76)));}
        content.setAlpha(0f);content.animate().alpha(1f).setDuration(180).start();
    }

    private void home(){
        LinearLayout page=scroll();Calendar now=Calendar.getInstance();selectedDate=key(now);List<Plan> today=forDate(selectedDate);int done=0;Plan next=null;for(Plan p:today){if(p.done)done++;else if(next==null)next=p;}
        LinearLayout head=row();LinearLayout ht=column();ht.addView(text("Bugün",31,TEXT,true));ht.addView(text(cap(new SimpleDateFormat("EEEE · d MMMM",TR).format(now.getTime())),13,MUTED,false),top(4));head.addView(ht,new LinearLayout.LayoutParams(0,-2,1));TextView date=text(new SimpleDateFormat("d",TR).format(now.getTime()),18,ORANGE,true);date.setGravity(Gravity.CENTER);date.setBackground(round(SURFACE,18));head.addView(date,new LinearLayout.LayoutParams(dp(48),dp(48)));page.addView(head,margin(18,18,18,16));
        LinearLayout hero=card();TextView over=text("BUGÜN",10,ORANGE,true);hero.addView(over);hero.addView(text(next==null?(today.isEmpty()?"Takvim boş":"Bugünün planları tamamlandı"):next.title,22,TEXT,true),top(7));hero.addView(text(today.isEmpty()?"İlk planını birkaç dokunuşla oluştur.":done+" / "+today.size()+" plan tamamlandı",12,MUTED,false),top(5));LinearLayout bar=progress(done,today.size());hero.addView(bar,top(15));page.addView(hero,margin(18,0,18,12));
        LinearLayout actions=row();actions.addView(action("＋","Plan oluştur",v->{tap(v);openWizard();}),new LinearLayout.LayoutParams(0,76,1));gap(actions,8);actions.addView(action("✦","AI ile planla",v->{tap(v);openAi();}),new LinearLayout.LayoutParams(0,76,1));page.addView(actions,margin(18,0,18,20));
        LinearLayout title=row();title.addView(text("Bugünün akışı",20,TEXT,true),new LinearLayout.LayoutParams(0,-2,1));title.addView(text(today.size()+" plan",11,MUTED,true));page.addView(title,margin(18,0,18,10));
        if(today.isEmpty()){LinearLayout empty=card();empty.setGravity(Gravity.CENTER);empty.addView(text("Henüz plan yok",18,TEXT,true));TextView b=button("İlk planı oluştur",Color.WHITE,ORANGE);empty.addView(b,margin(16,40,16,0));b.setOnClickListener(v->{tap(v);openWizard();});page.addView(empty,margin(18,0,18,30));}
        else for(Plan p:today) page.addView(planCard(p),margin(18,0,18,9));
    }

    private LinearLayout planCard(Plan p){
        LinearLayout c=card();c.setPadding(dp(16),dp(14),dp(14),dp(14));LinearLayout r=row();TextView dot=text(p.done?"✓":"•",18,p.done?GREEN:ORANGE,true);dot.setGravity(Gravity.CENTER);r.addView(dot,new LinearLayout.LayoutParams(dp(30),dp(42)));LinearLayout tx=column();tx.addView(text(p.title,16,p.done?MUTED:TEXT,true));String when=Plan.ALL_DAY.equals(p.type)?"Tüm gün":safe(p.startTime)+(p.endTime==null||p.endTime.isEmpty()?"":" – "+p.endTime);tx.addView(text(when,11,MUTED,false),top(4));r.addView(tx,new LinearLayout.LayoutParams(0,-2,1));TextView edit=button("…",TEXT,SURFACE2);r.addView(edit,new LinearLayout.LayoutParams(dp(42),dp(42)));c.addView(r);edit.setOnClickListener(v->{tap(v);showPlanActions(p);});c.setOnClickListener(v->{if(p.done)return;toggleDone(p);});return c;
    }

    private void showPlanActions(Plan p){Dialog d=dialog();LinearLayout b=dialogBox();b.addView(text(p.title,21,TEXT,true));TextView done=button(p.done?"Tamamlanmadı olarak işaretle":"Tamamlandı",Color.WHITE,p.done?SURFACE2:GREEN);b.addView(done,top(14));TextView edit=button("Planı düzenle",Color.WHITE,ORANGE);b.addView(edit,top(7));TextView del=button("Planı sil",Color.WHITE,RED);b.addView(del,top(7));TextView close=button("Kapat",MUTED,SURFACE2);b.addView(close,top(7));done.setOnClickListener(v->{p.done=!p.done;p.status=p.done?Plan.STATUS_DONE:Plan.STATUS_PLANNED;if(p.done)ReminderScheduler.cancel(this,p);else schedule(p);save();d.dismiss();render();});del.setOnClickListener(v->{plans.remove(p);ReminderScheduler.cancel(this,p);save();d.dismiss();render();});edit.setOnClickListener(v->{d.dismiss();editPlan(p);});close.setOnClickListener(v->d.dismiss());d.setContentView(b);d.show();fit(d,.9f);}

    private void editPlan(Plan p){
        Dialog d=dialog();LinearLayout b=dialogBox();b.addView(text("Planı düzenle",22,TEXT,true));EditText title=input("Plan adı");title.setText(p.title);b.addView(title,top(14));EditText date=input("Tarih YYYY-MM-DD");date.setText(p.date);b.addView(date,top(7));EditText start=input("Başlangıç HH:MM");start.setText(p.startTime);b.addView(start,top(7));EditText end=input("Bitiş HH:MM (isteğe bağlı)");end.setText(p.endTime);b.addView(end,top(7));EditText note=input("Not");note.setText(p.note);b.addView(note,top(7));TextView save=button("Değişiklikleri kaydet",Color.WHITE,ORANGE);b.addView(save,top(12));save.setOnClickListener(v->{String t=title.getText().toString().trim();if(t.isEmpty()){title.setError("Gerekli");return;}p.title=t;p.date=date.getText().toString().trim();p.startTime=start.getText().toString().trim();p.endTime=end.getText().toString().trim();p.time=p.startTime;p.note=note.getText().toString().trim();p.type=p.startTime.isEmpty()?Plan.ALL_DAY:Plan.HOURLY;p.duration=Plan.durationBetween(p.startTime,p.endTime);p.status=Plan.STATUS_PLANNED;p.done=false;save();schedule(p);d.dismiss();render();});d.setContentView(b);d.show();fit(d,.94f);}

    private void calendar(){
        LinearLayout page=scroll();page.addView(text("Planlar",31,TEXT,true),margin(18,20,18,4));page.addView(text("Gününü gör, planına dokun ve değiştir.",13,MUTED,false),margin(18,0,18,18));
        Calendar c=Calendar.getInstance();LinearLayout days=row();for(int i=0;i<7;i++){Calendar x=(Calendar)c.clone();x.add(Calendar.DAY_OF_MONTH,i);String k=key(x);TextView d=text(new SimpleDateFormat("EEE",TR).format(x.getTime()).substring(0,2),10,k.equals(selectedDate)?Color.WHITE:MUTED,true);TextView n=text(String.valueOf(x.get(Calendar.DAY_OF_MONTH)),18,k.equals(selectedDate)?Color.WHITE:TEXT,true);LinearLayout cell=column();cell.setGravity(Gravity.CENTER);cell.addView(d);cell.addView(n,top(4));if(k.equals(selectedDate))cell.setBackground(round(ORANGE,17));cell.setOnClickListener(v->{tap(v);selectedDate=k;calendar();});days.addView(cell,new LinearLayout.LayoutParams(0,68,1));if(i<6)gap(days,5);}page.addView(days,margin(18,0,18,18));List<Plan> list=forDate(selectedDate);if(list.isEmpty())page.addView(cardText("Bu gün için plan yok."),margin(18,0,18,10));for(Plan p:list)page.addView(planCard(p),margin(18,0,18,9));TextView add=button("＋  Bu güne plan ekle",Color.WHITE,ORANGE);add.setOnClickListener(v->{tap(v);openWizard();});page.addView(add,margin(18,16,18,30));
    }

    private void settingsPage(){
        LinearLayout page=scroll();page.addView(text("Ayarlar",31,TEXT,true),margin(18,20,18,18));
        page.addView(text("GÖRÜNÜM",10,MUTED,true),margin(18,0,18,8));page.addView(setting("Tema","Rotam şu an gece görünümünde","Gece",v->{}),margin(18,0,18,8));
        page.addView(text("BİLDİRİMLER",10,MUTED,true),margin(18,12,18,8));TextView rem=button("Hatırlatma · "+reminder()+" dk önce",TEXT,SURFACE);page.addView(rem,margin(18,0,18,8));rem.setOnClickListener(v->{tap(v);chooseReminder(rem);});
        TextView test=button("Bildirimleri yeniden planla",TEXT,SURFACE);page.addView(test,margin(18,0,18,8));test.setOnClickListener(v->{tap(v);ReminderScheduler.rescheduleAll(this,plans);toast("Bildirimler yeniden planlandı.");});
        page.addView(text("GEMINI AI",10,MUTED,true),margin(18,12,18,8));TextView ai=button(hasKey()?"Gemini bağlı · Ayarı aç":"Gemini API anahtarını bağla",Color.WHITE,ORANGE);page.addView(ai,margin(18,0,18,8));ai.setOnClickListener(v->{tap(v);geminiSetup();});
        TextView about=button("Rotam hakkında",TEXT,SURFACE);page.addView(about,margin(18,0,18,30));about.setOnClickListener(v->{tap(v);toast("Rotam · kişisel planlama");});
    }

    private void geminiSetup(){Dialog d=dialog();LinearLayout b=dialogBox();b.addView(text("Google Gemini",22,TEXT,true));b.addView(text("AI butonu gerçek Gemini bağlantısını kullanır. Anahtar yalnızca bu cihazda saklanır.",12,MUTED,false),top(5));EditText key=input("AIza…");key.setInputType(0x00000081);if(hasKey())key.setHint("Mevcut anahtar kayıtlı");b.addView(key,top(12));TextView save=button("Anahtarı kaydet",Color.WHITE,ORANGE);b.addView(save,top(10));TextView close=button("Kapat",MUTED,SURFACE2);b.addView(close,top(7));save.setOnClickListener(v->{String s=key.getText().toString().trim();if(s.isEmpty()){toast("Yeni anahtarı gir.");return;}getSharedPreferences("rotam_settings",0).edit().putString("gemini_api_key",s).apply();d.dismiss();render();});close.setOnClickListener(v->d.dismiss());d.setContentView(b);d.show();fit(d,.92f);}

    private void chooseReminder(TextView target){Dialog d=dialog();LinearLayout b=dialogBox();b.addView(text("Hatırlatma zamanı",22,TEXT,true));int[] vals={5,10,15,30,60};for(int n:vals){TextView x=button(n+" dakika önce",n==reminder()?Color.WHITE:TEXT,n==reminder()?ORANGE:SURFACE2);b.addView(x,top(7));x.setOnClickListener(v->{getSharedPreferences("rotam_settings",0).edit().putInt("reminder_minutes",n).apply();target.setText("Hatırlatma · "+n+" dk önce");for(Plan p:plans)schedule(p);d.dismiss();});}d.setContentView(b);d.show();fit(d,.88f);}

    private void openWizard(){PlanWizard.show(this,plans,selectedDate,()->{save();render();});}

    private void openAi(){InlineAiPanel.show(this,plans,()->{save();render();});}

    private void reopenMissed(Plan p){editPlan(p);}

    private void save(){PlanStore.save(this,plans);}
    private void schedule(Plan p){android.content.SharedPreferences s=getSharedPreferences("rotam_settings",0);ReminderScheduler.schedule(this,p,s.getInt("reminder_minutes",15),s.getBoolean("reminders",true));}
    private int reminder(){return getSharedPreferences("rotam_settings",0).getInt("reminder_minutes",15);}
    private boolean hasKey(){String k=getSharedPreferences("rotam_settings",0).getString("gemini_api_key","");return !k.trim().isEmpty();}
    private void toggleDone(Plan p){p.done=!p.done;p.status=p.done?Plan.STATUS_DONE:Plan.STATUS_PLANNED;if(p.done)ReminderScheduler.cancel(this,p);else schedule(p);save();render();}

    private List<Plan> forDate(String d){List<Plan> r=new ArrayList<>();for(Plan p:plans)if(d.equals(p.date))r.add(p);Collections.sort(r,Comparator.comparing(p->p.startTime==null||p.startTime.isEmpty()?"99:99":p.startTime));return r;}
    private String key(Calendar c){return String.format(Locale.GERMANY,"%04d-%02d-%02d",c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH));}
    private String cap(String s){return s==null||s.isEmpty()?s:s.substring(0,1).toUpperCase(TR)+s.substring(1);}
    private String safe(String s){return s==null?"":s;}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable round(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=column();l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private LinearLayout scroll(){ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setClipToPadding(false);LinearLayout c=column();c.setPadding(0,0,0,dp(24));s.addView(c,new ScrollView.LayoutParams(-1,-2));content.addView(s,new LinearLayout.LayoutParams(-1,0,1));return c;}
    private LinearLayout card(){LinearLayout c=column();c.setPadding(dp(16),dp(16),dp(16),dp(16));c.setBackground(round(SURFACE,22));return c;}
    private LinearLayout cardText(String s){LinearLayout c=card();c.addView(text(s,16,TEXT,true));return c;}
    private LinearLayout progress(int done,int total){LinearLayout c=column();LinearLayout track=column();track.setBackground(round(LINE,5));LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,dp(7));track.addView(new View(this),new LinearLayout.LayoutParams(total==0?0:(int)(getResources().getDisplayMetrics().widthPixels*.01f*Math.min(100,done*100/Math.max(1,total))),dp(7)));c.addView(track);return c;}
    private LinearLayout action(String icon,String label,View.OnClickListener l){LinearLayout c=column();c.setGravity(Gravity.CENTER);c.setBackground(round(SURFACE2,20));TextView i=text(icon,24,ORANGE,true);i.setGravity(Gravity.CENTER);c.addView(i,new LinearLayout.LayoutParams(-1,35));TextView t=text(label,12,TEXT,true);t.setGravity(Gravity.CENTER);c.addView(t);c.setOnClickListener(l);return c;}
    private LinearLayout setting(String a,String b,String v,View.OnClickListener l){LinearLayout c=card();LinearLayout r=row();LinearLayout tx=column();tx.addView(text(a,15,TEXT,true));tx.addView(text(b,11,MUTED,false),top(3));r.addView(tx,new LinearLayout.LayoutParams(0,-2,1));r.addView(text(v,12,ORANGE,true));c.addView(r);c.setOnClickListener(l);return c;}
    private TextView text(String s,float size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setTypeface(Typeface.create("sans",bold?Typeface.BOLD:Typeface.NORMAL));t.setIncludeFontPadding(false);return t;}
    private TextView button(String s,int fg,int bg){TextView t=text(s,13,fg,true);t.setGravity(Gravity.CENTER);t.setPadding(dp(10),0,dp(10),0);t.setBackground(round(bg,17));return t;}
    private EditText input(String hint){EditText e=new EditText(this);e.setSingleLine(true);e.setHint(hint);e.setHintTextColor(MUTED);e.setTextColor(TEXT);e.setTextSize(15);e.setPadding(dp(14),0,dp(14),0);e.setBackground(round(SURFACE2,17));return e;}
    private LinearLayout.LayoutParams top(int n){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(n);return p;}
    private LinearLayout.LayoutParams margin(int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(dp(l),dp(t),dp(r),dp(b));return p;}
    private void gap(LinearLayout l,int w){l.addView(new View(this),new LinearLayout.LayoutParams(dp(w),1));}
    private Dialog dialog(){Dialog d=new Dialog(this);d.setCancelable(true);return d;}
    private LinearLayout dialogBox(){LinearLayout b=column();b.setPadding(dp(18),dp(18),dp(18),dp(18));b.setBackground(round(BG,28));return b;}
    private void fit(Dialog d,float f){d.show();Window w=d.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.setLayout((int)(getResources().getDisplayMetrics().widthPixels*f),-2);w.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);}}
    private void tap(View v){v.animate().scaleX(.97f).scaleY(.97f).setDuration(55).withEndAction(()->v.animate().scaleX(1).scaleY(1).setDuration(110).start()).start();}
    private void toast(String s){android.widget.Toast.makeText(this,s,android.widget.Toast.LENGTH_SHORT).show();}
}
