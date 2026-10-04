package com.rota.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import android.util.Base64;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private int BG = Color.rgb(11, 13, 17);
    private int SURFACE = Color.rgb(23, 26, 32);
    private int SURFACE_2 = Color.rgb(31, 35, 42);
    private int SURFACE_3 = Color.rgb(39, 43, 50);
    private int TEXT = Color.rgb(245, 245, 245);
    private int MUTED = Color.rgb(157, 163, 172);
    private int LINE = Color.rgb(57, 62, 71);
    private int ORANGE = Color.rgb(255, 139, 0);
    private int ORANGE_SOFT = Color.rgb(64, 40, 12);
    private int GREEN = Color.rgb(86, 190, 132);
    private int RED = Color.rgb(235, 86, 92);

    private final Locale tr = new Locale("tr", "TR");
    private final List<Plan> plans = new ArrayList<>();
    private LinearLayout content;
    private int tab = 0;
    private String selectedDate;
    private Calendar monthCursor;
    private String plannerView = "Ay";
    private android.media.SoundPool uiSoundPool;
    private int uiTickId;
    private boolean uiSoundLoaded;
    private final android.content.SharedPreferences prefs;

    public MainActivity() {
        prefs = null;
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        selectedDate = key(Calendar.getInstance());
        monthCursor = Calendar.getInstance();
        plans.addAll(PlanStore.load(this));
        applyThemeColors();

        applySystemBars();
        if (Build.VERSION.SDK_INT >= 29) {
            getWindow().setStatusBarContrastEnforced(false);
            getWindow().setNavigationBarContrastEnforced(false);
        }

        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
        }

        buildShell();
        render();
    }

    private android.content.SharedPreferences settings() {
        return getSharedPreferences("rotam_settings", MODE_PRIVATE);
    }

    private boolean hapticsOn() {
        return settings().getBoolean("haptics", true);
    }

    private boolean remindersOn() {
        return settings().getBoolean("reminders", true);
    }

    private boolean quickPlanOn() {
        return settings().getBoolean("quick_plan", true);
    }

    private int reminderMinutes() {
        return settings().getInt("reminder_minutes", 15);
    }

    private String reminderLabel() {
        int m = reminderMinutes();
        return m + " dakika önce";
    }

    private boolean hasAiKey() {
        return loadAiKey() != null && !loadAiKey().trim().isEmpty();
    }

    private String loadAiKey() {
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            if (!ks.containsAlias("rotam_ai_key")) return "";
            SecretKey key = ((KeyStore.SecretKeyEntry) ks.getEntry("rotam_ai_key", null)).getSecretKey();
            String packed = settings().getString("ai_key_blob", "");
            if (packed.isEmpty()) return "";
            JSONObject o = new JSONObject(packed);
            byte[] iv = Base64.decode(o.getString("iv"), Base64.DEFAULT);
            byte[] data = Base64.decode(o.getString("data"), Base64.DEFAULT);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(data), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return "";
        }
    }

    private void saveAiKey(String value) {
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            SecretKey key;
            if (ks.containsAlias("rotam_ai_key")) {
                key = ((KeyStore.SecretKeyEntry) ks.getEntry("rotam_ai_key", null)).getSecretKey();
            } else {
                KeyGenerator kg = KeyGenerator.getInstance("AES", "AndroidKeyStore");
                kg.init(256);
                key = kg.generateKey();
            }
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            byte[] data = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            JSONObject o = new JSONObject();
            o.put("iv", Base64.encodeToString(iv, Base64.NO_WRAP));
            o.put("data", Base64.encodeToString(data, Base64.NO_WRAP));
            settings().edit().putString("ai_key_blob", o.toString()).apply();
        } catch (Exception ignored) {}
    }

    private void clearAiKey() {
        settings().edit().remove("ai_key_blob").apply();
    }

    private void tap(View v) {
        if (v != null) {
            v.animate().scaleX(0.965f).scaleY(0.965f).setDuration(65)
                    .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(125).start()).start();
            clickSound();
            if (hapticsOn()) {
                try { v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); } catch (Exception ignored) {}
            }
        }
    }

    private void clickSound() {
        tickSound();
    }

    private void buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(0, getStatusBarHeight(), 0, 0);

        FrameLayout page = new FrameLayout(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setBackgroundColor(BG);
        page.addView(content, new FrameLayout.LayoutParams(-1, -1));

        TextView fab = text("+", 34, Color.WHITE, true);
        fab.setGravity(Gravity.CENTER);
        fab.setBackground(round(ORANGE, 22));
        fab.setElevation(dp(10));
        FrameLayout.LayoutParams fp = new FrameLayout.LayoutParams(dp(62), dp(62),
                Gravity.BOTTOM | Gravity.END);
        fp.setMargins(0, 0, dp(18), dp(16));
        page.addView(fab, fp);
        fab.setOnClickListener(v -> {
            tap(v);
            showPlanEditor(null, quickPlanOn() ? selectedDate : key(Calendar.getInstance()));
        });

        root.addView(page, new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(buildBottomBar(), new LinearLayout.LayoutParams(-1, dp(82)));
        setContentView(root);
    }

    private int getStatusBarHeight() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id == 0 ? dp(24) : getResources().getDimensionPixelSize(id);
    }

    private void applySystemBars() {
        boolean dark = "dark".equals(settings().getString("theme", "dark"));
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(dark ? Color.BLACK : BG);
        int flags = 0;
        if (!dark) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        getWindow().getDecorView().setSystemUiVisibility(flags);
        if (Build.VERSION.SDK_INT >= 29) {
            getWindow().setStatusBarContrastEnforced(false);
            getWindow().setNavigationBarContrastEnforced(false);
        }
    }

    private void applyThemeColors() {
        boolean dark = "dark".equals(settings().getString("theme", "dark"));
        if (dark) {
            BG = Color.rgb(11, 13, 17);
            SURFACE = Color.rgb(23, 26, 32);
            SURFACE_2 = Color.rgb(31, 35, 42);
            SURFACE_3 = Color.rgb(39, 43, 50);
            TEXT = Color.rgb(245, 245, 245);
            MUTED = Color.rgb(157, 163, 172);
            LINE = Color.rgb(57, 62, 71);
            ORANGE = Color.rgb(255, 139, 0);
            ORANGE_SOFT = Color.rgb(64, 40, 12);
            GREEN = Color.rgb(86, 190, 132);
            RED = Color.rgb(235, 86, 92);
        } else {
            BG = Color.rgb(247, 245, 241);
            SURFACE = Color.rgb(255, 255, 255);
            SURFACE_2 = Color.rgb(239, 237, 233);
            SURFACE_3 = Color.rgb(228, 225, 220);
            TEXT = Color.rgb(28, 29, 31);
            MUTED = Color.rgb(105, 108, 113);
            LINE = Color.rgb(215, 211, 205);
            ORANGE = Color.rgb(238, 119, 0);
            ORANGE_SOFT = Color.rgb(255, 234, 207);
            GREEN = Color.rgb(46, 145, 91);
            RED = Color.rgb(196, 52, 59);
        }
    }

    private LinearLayout buildBottomBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        bar.setPadding(dp(10), dp(8), dp(10), dp(8));
        bar.setBackgroundColor(BG);

        String[] labels = {"Bugün", "Plan", "Ayarlar"};
        String[] icons = {"", "", ""};
        for (int i = 0; i < 3; i++) {
            final int index = i;
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(dp(8), 0, dp(8), 0);

            ImageView icon = new ImageView(this);
            int[] navIcons = {R.drawable.ic_nav_home, R.drawable.ic_nav_calendar, R.drawable.ic_nav_settings};
            icon.setImageResource(navIcons[i]);
            icon.setColorFilter(index == tab ? ORANGE : MUTED);
            icon.setPadding(dp(3), dp(3), dp(3), dp(3));
            TextView label = text(labels[i], 11, index == tab ? TEXT : MUTED, true);
            label.setGravity(Gravity.CENTER);
            item.addView(icon, new LinearLayout.LayoutParams(-1, dp(30)));
            item.addView(label, new LinearLayout.LayoutParams(-1, dp(24)));

            if (index == tab) item.setBackground(round(SURFACE, 20));

            item.setOnClickListener(v -> {
                tap(v);
                if (tab == 0 && index == 1) {
                    selectedDate = key(Calendar.getInstance());
                    monthCursor = Calendar.getInstance();
                }
                tab = index;
                render();
            });

            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(58), 1);
            p.setMargins(dp(4), 0, dp(4), 0);
            bar.addView(item, p);
        }
        return bar;
    }

    private void render() {
        content.removeAllViews();
        if (tab == 0) renderToday();
        else if (tab == 1) renderPlan();
        else renderSettings();
        rebuildBottomBar();
        content.setAlpha(0f);
        content.animate().alpha(1f).setDuration(180).start();
    }

    private void rebuildBottomBar() {
        ViewGroup root = (ViewGroup) content.getParent().getParent();
        if (root.getChildCount() > 1) {
            root.removeViewAt(1);
            root.addView(buildBottomBar(), new LinearLayout.LayoutParams(-1, dp(82)));
        }
    }


    private void renderToday() {
        Calendar now = Calendar.getInstance();
        String todayKey = key(now);
        selectedDate = todayKey;
        List<Plan> today = plansFor(todayKey);
        int done = countDone(today);
        Plan nextPlan = null;
        for (Plan p : today) {
            if (!p.done) { nextPlan = p; break; }
        }

        LinearLayout page = scrollColumn();

        LinearLayout header = row();
        LinearLayout headText = column();
        headText.addView(text("Bugün", 32, TEXT, true));
        headText.addView(text(capitalize(new SimpleDateFormat("EEEE · d MMMM", tr).format(now.getTime())),
                13, MUTED, false), marginTop(4));
        header.addView(headText, new LinearLayout.LayoutParams(0, -2, 1));

        TextView bell = iconButton("♧", false);
        bell.setText("⌁");
        bell.setTextSize(23);
        bell.setTextColor(TEXT);
        bell.setBackground(round(SURFACE, 17));
        bell.setOnClickListener(v -> { tap(v); showNotificationInfo(); });
        header.addView(bell, new LinearLayout.LayoutParams(dp(46), dp(46)));
        page.addView(header, margin(18, 18, 18, 14));

        LinearLayout progress = card();
        progress.setPadding(dp(18), dp(18), dp(18), dp(18));
        LinearLayout pr = row();
        LinearLayout ptxt = column();
        ptxt.addView(text("BUGÜNÜN DURUMU", 10, ORANGE, true));
        String headline = today.isEmpty() ? "Henüz bir plan yok" :
                nextPlan == null ? "Bugünün akışı tamamlandı" : nextPlan.title;
        ptxt.addView(text(headline, 20, TEXT, true), marginTop(6));
        ptxt.addView(text(today.isEmpty() ? "İlk planını ekleyerek başlayabilirsin." :
                done + " / " + today.size() + " plan tamamlandı", 12, MUTED, false), marginTop(5));
        pr.addView(ptxt, new LinearLayout.LayoutParams(0, -2, 1));

        ProgressRing ring = new ProgressRing(this);
        float pct = today.isEmpty() ? 0f : done / (float) today.size();
        ring.setProgress(pct);
        ring.setLabel((int)(pct * 100f) + "%");
        pr.addView(ring, new LinearLayout.LayoutParams(dp(82), dp(82)));
        progress.addView(pr);
        progress.addView(progressBar(done, today.size()), marginTop(15));
        page.addView(progress, margin(18, 0, 18, 12));

        LinearLayout quick = row();
        quick.addView(quickAction("＋", "Plan ekle", v -> {
            tap(v); showPlanEditor(null, todayKey);
        }), new LinearLayout.LayoutParams(0, dp(74), 1));
        addGap(quick, 8);
        quick.addView(quickAction("AI", "Günün özeti", v -> {
            tap(v); showAiDialog();
        }), new LinearLayout.LayoutParams(0, dp(74), 1));
        page.addView(quick, margin(18, 0, 18, 18));

        LinearLayout section = row();
        section.addView(text("Bugünün akışı", 20, TEXT, true),
                new LinearLayout.LayoutParams(0, -2, 1));
        TextView count = text(today.size() + " plan", 11, MUTED, true);
        section.addView(count);
        page.addView(section, margin(18, 0, 18, 9));

        if (today.isEmpty()) {
            LinearLayout empty = card();
            empty.setPadding(dp(18), dp(22), dp(18), dp(22));
            TextView icon = text("＋", 28, ORANGE, true);
            icon.setGravity(Gravity.CENTER);
            empty.addView(icon, new LinearLayout.LayoutParams(-1, dp(36)));
            TextView eTitle = text("Takvimini boş bırakma.", 18, TEXT, true);
            eTitle.setGravity(Gravity.CENTER);
            empty.addView(eTitle, marginTop(8));
            TextView eSub = text("Bir saat, hedef veya tüm gün sürecek bir plan oluştur.", 12, MUTED, false);
            eSub.setGravity(Gravity.CENTER);
            empty.addView(eSub, marginTop(5));
            TextView first = buttonText("İlk planı oluştur", Color.WHITE);
            first.setBackground(round(ORANGE, 16));
            empty.addView(first, margin(40, 15, 40, 0));
            first.setOnClickListener(v -> { tap(v); showPlanEditor(null, todayKey); });
            page.addView(empty, margin(18, 0, 18, 24));
        } else {
            for (int i = 0; i < today.size(); i++) {
                page.addView(addPlanCard(today.get(i), i), margin(18, 0, 18, 9));
            }
        }

        if (settings().getBoolean("ai_suggestions", true)) {
            LinearLayout ai = row();
            ai.setPadding(dp(16), dp(14), dp(16), dp(14));
            ai.setBackground(round(SURFACE_2, 18));
            TextView mark = text("✦", 21, ORANGE, true);
            mark.setGravity(Gravity.CENTER);
            ai.addView(mark, new LinearLayout.LayoutParams(dp(34), dp(44)));
            LinearLayout at = column();
            at.addView(text("ROTAM AI", 10, ORANGE, true));
            at.addView(text(hasAiKey() ? "Gemini hazır. Bugünü birlikte düzenleyelim." :
                    "Gemini'yi Ayarlar'dan bağla.", 13, TEXT, true), marginTop(4));
            ai.addView(at, new LinearLayout.LayoutParams(0, -2, 1));
            TextView arrow = text("›", 28, MUTED, false);
            arrow.setGravity(Gravity.CENTER);
            ai.addView(arrow, new LinearLayout.LayoutParams(dp(28), dp(44)));
            ai.setOnClickListener(v -> { tap(v); showAiDialog(); });
            page.addView(ai, margin(18, 8, 18, 22));
        }
    }

    private LinearLayout quickAction(String icon, String label, View.OnClickListener listener) {
        LinearLayout q = column();
        q.setGravity(Gravity.CENTER);
        q.setPadding(dp(8), dp(8), dp(8), dp(8));
        q.setBackground(round(SURFACE, 18));
        TextView i = text(icon, 18, ORANGE, true);
        i.setGravity(Gravity.CENTER);
        q.addView(i, new LinearLayout.LayoutParams(-1, dp(28)));
        TextView l = text(label, 11, TEXT, true);
        l.setGravity(Gravity.CENTER);
        q.addView(l, marginTop(3));
        q.setOnClickListener(listener);
        return q;
    }


    private void renderPlan() {
        Calendar today=Calendar.getInstance();
        if(parseKey(selectedDate).before(startOfDay(today))) selectedDate=key(today);
        LinearLayout page=scrollColumn();
        addTopBar(page,false,false);
        page.addView(text("Plan",30,TEXT,true),margin(18,0,18,3));
        page.addView(text("Zamanını tek bakışta yönet.",13,MUTED,false),margin(18,0,18,14));

        LinearLayout modes=row();
        for(String mode:new String[]{"Gün","Hafta","Ay"}){
            TextView b=text(mode,12,mode.equals(plannerView)?Color.WHITE:TEXT,true);
            b.setGravity(Gravity.CENTER);b.setBackground(round(mode.equals(plannerView)?ORANGE:SURFACE_2,15));
            b.setOnClickListener(v->{tap(v);plannerView=mode;render();});
            modes.addView(b,new LinearLayout.LayoutParams(0,dp(42),1));
            if(!mode.equals("Ay")) addGap(modes,7);
        }
        page.addView(modes,margin(18,0,18,14));

        if("Gün".equals(plannerView)){
            Calendar day=parseKey(selectedDate);
            LinearLayout nav=row();
            TextView prev=iconButton("‹",false),next=iconButton("›",false);
            TextView date=text(prettyDate(selectedDate),14,TEXT,true);date.setGravity(Gravity.CENTER);
            nav.addView(prev,new LinearLayout.LayoutParams(dp(44),dp(44)));
            nav.addView(date,new LinearLayout.LayoutParams(0,dp(44),1));
            nav.addView(next,new LinearLayout.LayoutParams(dp(44),dp(44)));
            prev.setOnClickListener(v->{tap(v);selectedDate=key(addDays(day,-1));render();});
            next.setOnClickListener(v->{tap(v);selectedDate=key(addDays(day,1));render();});
            page.addView(nav,margin(18,0,18,10));
            List<Plan> list=plansFor(selectedDate);
            if(list.isEmpty()) page.addView(buildCalendarEmptyCard(),margin(18,0,18,12));
            else for(int i=0;i<list.size();i++) page.addView(addPlanCard(list.get(i),i),margin(18,0,18,9));
            TextView add=buttonText("+ Plan ekle",ORANGE);add.setGravity(Gravity.CENTER);add.setBackground(round(ORANGE_SOFT,17));
            add.setOnClickListener(v->{tap(v);showPlanEditor(null,selectedDate);});
            page.addView(add,margin(18,6,18,22));
            return;
        }

        if("Hafta".equals(plannerView)){
            Calendar base=startOfDay(parseKey(selectedDate));
            LinearLayout week=row();
            for(int i=0;i<7;i++){
                Calendar d=addDays(base,i);String k=key(d);boolean sel=k.equals(selectedDate);
                LinearLayout cell=column();cell.setGravity(Gravity.CENTER);cell.setPadding(dp(3),dp(8),dp(3),dp(8));
                cell.setBackground(round(sel?ORANGE:SURFACE,18));
                cell.addView(text(new SimpleDateFormat("EEE",tr).format(d.getTime()).substring(0,1).toUpperCase(tr),10,sel?Color.WHITE:MUTED,true));
                cell.addView(text(String.valueOf(d.get(Calendar.DAY_OF_MONTH)),19,sel?Color.WHITE:TEXT,true),marginTop(4));
                List<Plan> ls=plansFor(k);cell.addView(text(ls.isEmpty()?"·":String.valueOf(ls.size()),10,sel?Color.WHITE:ORANGE,true),marginTop(3));
                cell.setOnClickListener(v->{tap(v);selectedDate=k;render();});
                week.addView(cell,new LinearLayout.LayoutParams(0,dp(82),1));if(i<6)addGap(week,4);
            }
            page.addView(week,margin(18,0,18,16));
            LinearLayout summary=card();summary.addView(text("Haftanın akışı",18,TEXT,true));
            int total=0,done=0;
            for(int i=0;i<7;i++){String k=key(addDays(base,i));List<Plan> ls=plansFor(k);total+=ls.size();done+=countDone(ls);if(!ls.isEmpty())summary.addView(text("• "+prettyLabel(k)+"  "+countDone(ls)+"/"+ls.size(),12,MUTED,false),marginTop(8));}
            summary.addView(text(total==0?"Henüz plan yok.":done+" / "+total+" tamamlandı",12,ORANGE,true),marginTop(12));
            page.addView(summary,margin(18,0,18,20));
            return;
        }

        Calendar cursor=(Calendar)monthCursor.clone();
        LinearLayout mh=row();TextView prev=iconButton("‹",false),next=iconButton("›",false);
        TextView ml=text(capitalize(new SimpleDateFormat("MMMM yyyy",tr).format(cursor.getTime())),18,TEXT,true);ml.setGravity(Gravity.CENTER);
        mh.addView(prev,new LinearLayout.LayoutParams(dp(44),dp(44)));mh.addView(ml,new LinearLayout.LayoutParams(0,dp(44),1));mh.addView(next,new LinearLayout.LayoutParams(dp(44),dp(44)));
        prev.setOnClickListener(v->{tap(v);Calendar now=Calendar.getInstance();if(sameMonth(monthCursor,now))return;monthCursor.add(Calendar.MONTH,-1);render();});
        next.setOnClickListener(v->{tap(v);monthCursor.add(Calendar.MONTH,1);render();});
        page.addView(mh,margin(18,0,18,8));

        LinearLayout cal=card();LinearLayout nr=row();
        for(String n:new String[]{"P","S","Ç","P","C","C","P"}){TextView t=text(n,10,MUTED,true);t.setGravity(Gravity.CENTER);nr.addView(t,new LinearLayout.LayoutParams(0,dp(28),1));}
        cal.addView(nr);
        Calendar first=(Calendar)cursor.clone();first.set(Calendar.DAY_OF_MONTH,1);int off=(first.get(Calendar.DAY_OF_WEEK)+5)%7;int days=cursor.getActualMaximum(Calendar.DAY_OF_MONTH);int idx=0;
        Calendar todayStart=startOfDay(Calendar.getInstance());
        for(int r=0;r<6;r++){LinearLayout line=row();for(int c=0;c<7;c++){int day=idx-off+1;TextView cell=text("",12,TEXT,true);cell.setGravity(Gravity.CENTER);
            if(day>0&&day<=days){Calendar d=(Calendar)cursor.clone();d.set(Calendar.DAY_OF_MONTH,day);String k=key(d);boolean past=startOfDay(d).before(todayStart),sel=k.equals(selectedDate),istoday=k.equals(key(Calendar.getInstance()));int count=plansFor(k).size();
                cell.setText(count>0?day+"\n•":String.valueOf(day));cell.setTextColor(sel?Color.WHITE:past?MUTED:TEXT);cell.setBackground(round(sel?ORANGE:istoday?SURFACE_3:Color.TRANSPARENT,15));
                if(!past)cell.setOnClickListener(v->{tap(v);selectedDate=k;render();});
            } line.addView(cell,new LinearLayout.LayoutParams(0,dp(50),1));idx++;}cal.addView(line);}
        page.addView(cal,margin(18,0,18,14));
        page.addView(text(prettyDate(selectedDate),18,TEXT,true),margin(18,0,18,8));
        List<Plan> selected=plansFor(selectedDate);
        if(selected.isEmpty())page.addView(buildCalendarEmptyCard(),margin(18,0,18,20));
        else for(int i=0;i<selected.size();i++)page.addView(addPlanCard(selected.get(i),i),margin(18,0,18,9));
    }

    private void renderSettings() {
        LinearLayout page=scrollColumn();addTopBar(page,false,false);
        page.addView(text("Ayarlar",30,TEXT,true),margin(18,0,18,3));
        page.addView(text("Rotam'ı kendi çalışma biçimine göre ayarla.",13,MUTED,false),margin(18,0,18,18));

        LinearLayout identity=card();LinearLayout ir=row();ImageView li=logo(54);ir.addView(li,new LinearLayout.LayoutParams(dp(54),dp(54)));
        LinearLayout it=column();it.addView(text("Rotam",18,TEXT,true));it.addView(text("Büyük güncelleme · 1.0",11,MUTED,false),marginTop(3));ir.addView(it,new LinearLayout.LayoutParams(0,-2,1));identity.addView(ir);
        page.addView(identity,margin(18,0,18,18));

        page.addView(sectionTitle("Bildirimler"),margin(18,0,18,7));
        page.addView(settingRow("Plan hatırlatmaları","Saatli planlardan önce bildirim.",remindersOn(),true,on->{settings().edit().putBoolean("reminders",on).apply();for(Plan p:plans){if(on)scheduleReminder(p);else cancelReminder(p);}}),margin(18,0,18,7));
        LinearLayout rc=settingCard();LinearLayout rr=row();LinearLayout rt=column();rt.addView(text("Hatırlatma zamanı",15,TEXT,true));rt.addView(text("Tek bir global ayardan kontrol edilir.",11,MUTED,false));rr.addView(rt,new LinearLayout.LayoutParams(0,-2,1));TextView rv=buttonText(reminderLabel(),ORANGE);rv.setGravity(Gravity.CENTER);rv.setBackground(round(ORANGE_SOFT,14));rr.addView(rv,new LinearLayout.LayoutParams(dp(92),dp(36)));rc.addView(rr);rc.setOnClickListener(v->{tap(v);showReminderChooser(rv);});page.addView(rc,margin(18,0,18,7));
        page.addView(settingAction("Sessiz saatler","Gece bildirimlerini sustur.",quietHoursLabel(),v->showQuietHoursDialog()),margin(18,0,18,7));

        page.addView(sectionTitle("Ses · hareket"),margin(18,12,18,7));
        page.addView(settingRow("UI sesleri","Kısa mekanik tık sesi.",settings().getBoolean("sounds",true),true,on->settings().edit().putBoolean("sounds",on).apply()),margin(18,0,18,7));
        page.addView(settingRow("Titreşim","Hafif dokunsal geri bildirim.",hapticsOn(),true,on->settings().edit().putBoolean("haptics",on).apply()),margin(18,0,18,7));
        page.addView(settingRow("Animasyonlar","Geçişler ve mikro etkileşimler.",settings().getBoolean("animations",true),true,on->settings().edit().putBoolean("animations",on).apply()),margin(18,0,18,7));

        page.addView(sectionTitle("Görünüm"),margin(18,12,18,7));
        LinearLayout theme=settingCard();LinearLayout trr=row();LinearLayout ttx=column();ttx.addView(text("Tema",15,TEXT,true));ttx.addView(text("Gece veya açık.",11,MUTED,false));trr.addView(ttx,new LinearLayout.LayoutParams(0,-2,1));
        boolean dark="dark".equals(settings().getString("theme","dark"));
        TextView night=buttonText("Gece",dark?Color.WHITE:TEXT);night.setGravity(Gravity.CENTER);night.setBackground(round(dark?ORANGE:SURFACE_2,14));
        TextView light=buttonText("Açık",!dark?Color.WHITE:TEXT);light.setGravity(Gravity.CENTER);light.setBackground(round(!dark?ORANGE:SURFACE_2,14));
        trr.addView(night,new LinearLayout.LayoutParams(dp(58),dp(34)));addGap(trr,6);trr.addView(light,new LinearLayout.LayoutParams(dp(58),dp(34)));theme.addView(trr);
        night.setOnClickListener(v->{tap(v);settings().edit().putString("theme","dark").apply();applyThemeColors();applySystemBars();buildShell();render();});
        light.setOnClickListener(v->{tap(v);settings().edit().putString("theme","light").apply();applyThemeColors();applySystemBars();buildShell();render();});
        page.addView(theme,margin(18,0,18,7));

        page.addView(sectionTitle("Rotam AI · Gemini"),margin(18,12,18,7));
        LinearLayout ai=settingCard();LinearLayout ar=row();TextView star=text("✦",24,ORANGE,true);star.setGravity(Gravity.CENTER);ar.addView(star,new LinearLayout.LayoutParams(dp(40),dp(46)));LinearLayout ax=column();ax.addView(text("Google Gemini",15,TEXT,true));ax.addView(text(hasAiKey()?"Bağlı · ücretsiz API katmanı kullanılabilir.":"Bağlı değil · Gemini API anahtarı gerekli.",11,hasAiKey()?GREEN:MUTED,false),marginTop(3));ar.addView(ax,new LinearLayout.LayoutParams(0,-2,1));TextView go=buttonText("Ayarla",ORANGE);ar.addView(go,new LinearLayout.LayoutParams(dp(72),dp(40)));ai.addView(ar);ai.setOnClickListener(v->{tap(v);showAiSetupDialog();});page.addView(ai,margin(18,0,18,7));
        page.addView(settingRow("AI önerileri","Ana ekranda Rotam AI kartını göster.",settings().getBoolean("ai_suggestions",true),true,on->settings().edit().putBoolean("ai_suggestions",on).apply()),margin(18,0,18,7));

        page.addView(sectionTitle("Veriler"),margin(18,12,18,7));
        LinearLayout ex=settingCard();LinearLayout er=row();LinearLayout et=column();et.addView(text("Planları dışa aktar",14,TEXT,true));et.addView(text("JSON yedek oluştur.",10,MUTED,false));er.addView(et,new LinearLayout.LayoutParams(0,-2,1));TextView eb=buttonText("Yedekle",ORANGE);er.addView(eb);ex.addView(er);ex.setOnClickListener(v->{tap(v);exportPlans();});page.addView(ex,margin(18,0,18,7));
        page.addView(text("Rotam · Yerel planlama ve odak uygulaması.",10,MUTED,false),margin(18,14,18,28));
    }

    private void addTopBar(LinearLayout page, boolean showBell, boolean showCalendar) {
        LinearLayout top = row();
        ImageView logoView = new ImageView(this);
        logoView.setImageResource(R.drawable.rotam_logo);
        logoView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        top.addView(logoView, new LinearLayout.LayoutParams(dp(46), dp(46)));

        LinearLayout tx = column();
        tx.setPadding(dp(10), 0, 0, 0);
        tx.addView(text("Rotam", 18, TEXT, true));
        tx.addView(text("Daha düzenli bir sen.", 11, MUTED, false), marginTop(3));
        top.addView(tx, new LinearLayout.LayoutParams(0, -2, 1));

        if (showBell) {
            ImageView bell = new ImageView(this);
            bell.setImageResource(R.drawable.ic_notification_bell);
            bell.setColorFilter(TEXT);
            bell.setPadding(dp(10), dp(10), dp(10), dp(10));
            bell.setBackground(round(SURFACE, 18));
            bell.setOnClickListener(v -> { tap(v); showNotificationInfo(); });
            top.addView(bell, new LinearLayout.LayoutParams(dp(46), dp(46)));
        }
        page.addView(top, margin(18, 12, 18, 16));
    }


    private View addPlanCard(Plan p, int index) {
        LinearLayout card = column();
        card.setPadding(dp(15), dp(14), dp(10), dp(10));
        card.setBackground(round(p.done ? SURFACE_2 : SURFACE, 20));
        card.setAlpha(0f);
        card.setTranslationY(dp(8));
        card.animate().alpha(1f).translationY(0)
                .setStartDelay(Math.min(index * 35, 180)).setDuration(220).start();

        LinearLayout main = row();

        TextView time = text(planMeta(p), 12, p.done ? MUTED : ORANGE, true);
        time.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        main.addView(time, new LinearLayout.LayoutParams(dp(78), dp(48)));

        View rail = new View(this);
        rail.setBackground(round(p.done ? LINE : ORANGE, 6));
        main.addView(rail, new LinearLayout.LayoutParams(dp(3), dp(48)));

        LinearLayout mid = column();
        mid.setPadding(dp(12), 0, dp(8), 0);
        TextView title = text(p.title, 16, p.done ? MUTED : TEXT, true);
        title.setMaxLines(2);
        mid.addView(title);
        mid.addView(text(typeLabel(p.type), 11, MUTED, false), marginTop(4));
        if (p.note != null && !p.note.trim().isEmpty()) {
            TextView note = text(p.note, 11, MUTED, false);
            note.setMaxLines(1);
            mid.addView(note, marginTop(4));
        }
        main.addView(mid, new LinearLayout.LayoutParams(0, -2, 1));

        TextView check = text(p.done ? "✓" : "○", 24, p.done ? ORANGE : MUTED, true);
        check.setGravity(Gravity.CENTER);
        check.setBackground(round(p.done ? ORANGE_SOFT : SURFACE_2, 17));
        main.addView(check, new LinearLayout.LayoutParams(dp(46), dp(46)));
        card.addView(main);

        LinearLayout actions = row();
        actions.setPadding(dp(88), dp(8), 0, 0);
        TextView edit = actionText("Düzenle", TEXT);
        TextView del = actionText("Sil", RED);
        actions.addView(edit, new LinearLayout.LayoutParams(dp(86), dp(34)));
        actions.addView(del, new LinearLayout.LayoutParams(dp(70), dp(34)));
        actions.setVisibility(View.GONE);
        card.addView(actions);

        check.setOnClickListener(v -> {
            tap(v);
            p.done = !p.done;
            PlanStore.save(this, plans);
            if (p.done) cancelReminder(p); else scheduleReminder(p);
            render();
        });

        card.setOnClickListener(v -> {
            tap(v);
            if (actions.getVisibility() == View.GONE) {
                actions.setVisibility(View.VISIBLE);
                actions.setAlpha(0f);
                actions.animate().alpha(1f).setDuration(150).start();
            } else {
                showPlanEditor(p, p.date);
            }
        });
        edit.setOnClickListener(v -> { tap(v); showPlanEditor(p, p.date); });
        del.setOnClickListener(v -> { tap(v); showDeleteDialog(p); });
        return card;
    }


    private LinearLayout statBox(String value, String label) {
        LinearLayout c = column();
        c.setGravity(Gravity.CENTER);
        c.setBackground(round(SURFACE, 18));
        c.addView(text(value, 18, TEXT, true));
        c.addView(text(label, 10, MUTED, false));
        return c;
    }

    private View progressBar(int done, int total) {
        FrameLayout wrap = new FrameLayout(this);
        wrap.setBackground(round(Color.rgb(38, 43, 50), 10));
        View fill = new View(this);
        fill.setBackground(round(ORANGE, 10));
        FrameLayout.LayoutParams fp = new FrameLayout.LayoutParams(
                total == 0 ? 0 : Math.max(dp(6), (int) (dp(300) * done / (float) total)),
                dp(6));
        fp.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
        wrap.addView(fill, fp);
        return wrap;
    }

    private String freeTimeText(List<Plan> list) {
        Calendar now = Calendar.getInstance();
        int minuteNow = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        int next = -1;
        for (Plan p : list) {
            if (p.done || p.startTime == null || !p.startTime.matches("\\d{2}:\\d{2}")) continue;
            int m = Integer.parseInt(p.startTime.substring(0,2)) * 60 + Integer.parseInt(p.startTime.substring(3));
            if (m > minuteNow && (next < 0 || m < next)) next = m;
        }
        if (next < 0) return "—";
        int mins = next - minuteNow;
        if (mins >= 60) return (mins / 60) + "s " + (mins % 60 == 0 ? "" : (mins % 60) + "dk");
        return mins + "dk";
    }

    private String smartMessage(List<Plan> today) {
        if (today.isEmpty()) return "Bugün için bir akış oluşturabilirsin.";
        Plan next = null;
        for (Plan p : today) if (!p.done) { next = p; break; }
        if (next == null) return "Bugünkü bütün planların tamamlandı.";
        String gap = freeTimeText(today);
        if (!"—".equals(gap)) return "Sıradaki planından önce " + gap + " boş vaktin var.";
        return "Sıradaki adım: " + next.title;
    }

    private String typeLabel(String type) {
        if (Plan.HOURLY.equals(type)) return "Saatli plan";
        if (Plan.ALL_DAY.equals(type)) return "Tüm gün";
        return "Esnek plan";
    }

    private String planMeta(Plan p) {
        if (Plan.ALL_DAY.equals(p.type)) return "TÜM GÜN";
        if (p.startTime != null && p.endTime != null && !p.startTime.isEmpty() && !p.endTime.isEmpty()) {
            return p.startTime + " – " + p.endTime;
        }
        if (p.startTime != null && !p.startTime.isEmpty()) return p.startTime;
        return "SAATSİZ";
    }


    private void showPlanEditor(Plan editing, String initialDate) {
        final Dialog d = styledDialog();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);

        LinearLayout box = column();
        box.setPadding(dp(20), dp(18), dp(20), dp(22));
        box.setBackground(round(Color.rgb(14, 16, 20), 30));

        LinearLayout header = row();
        LinearLayout ht = column();
        ht.addView(text(editing == null ? "Yeni plan" : "Planı düzenle", 26, TEXT, true));
        ht.addView(text("Zamanını netleştir, gerisini Rotam halletsin.", 11, MUTED, false), marginTop(5));
        header.addView(ht, new LinearLayout.LayoutParams(0, -2, 1));
        TextView close = iconButton("×", false);
        header.addView(close, new LinearLayout.LayoutParams(dp(44), dp(44)));
        box.addView(header);

        final String[] date = { initialDate == null || initialDate.isEmpty() ? key(Calendar.getInstance()) : initialDate };
        final String[] kind = { editing == null ? Plan.HOURLY : editing.type };
        final String[] start = { editing == null ? "" : actualTime(editing.startTime) };
        final String[] end = { editing == null ? "" : actualTime(editing.endTime) };
        final String[] repeat = { editing == null ? Plan.REPEAT_NONE :
                (editing.repeat == null ? Plan.REPEAT_NONE : editing.repeat) };

        box.addView(fieldTitle("Plan"), marginTop(18));
        EditText title = input("Örn. İş, Gym, Risale okumak");
        title.setText(editing == null ? "" : editing.title);
        title.setTextSize(17);
        title.setPadding(dp(16), 0, dp(16), 0);
        box.addView(title, marginTop(7));

        box.addView(fieldTitle("Plan türü"), marginTop(16));
        LinearLayout types = row();
        TextView hourly = editorChip("Saatli", Plan.HOURLY.equals(kind[0]));
        TextView flex = editorChip("Esnek", Plan.FLEX.equals(kind[0]));
        TextView allDay = editorChip("Tüm gün", Plan.ALL_DAY.equals(kind[0]));
        types.addView(hourly, new LinearLayout.LayoutParams(0, dp(48), 1));
        addGap(types, 7);
        types.addView(flex, new LinearLayout.LayoutParams(0, dp(48), 1));
        addGap(types, 7);
        types.addView(allDay, new LinearLayout.LayoutParams(0, dp(48), 1));
        box.addView(types, marginTop(7));

        box.addView(fieldTitle("Tarih"), marginTop(16));
        LinearLayout dateRow = row();
        TextView dateBox = editorChoice(prettyDate(date[0]), "⌄");
        dateRow.addView(dateBox, new LinearLayout.LayoutParams(0, dp(54), 1));
        box.addView(dateRow, marginTop(7));

        LinearLayout timeSection = column();
        TextView timeLabel = fieldTitle("Saat");
        timeSection.addView(timeLabel);
        LinearLayout times = row();
        TextView startBox = editorChoice(start[0].isEmpty() ? "Başlangıç" : start[0], "⌄");
        TextView endBox = editorChoice(end[0].isEmpty() ? "Bitiş (isteğe bağlı)" : end[0], "⌄");
        times.addView(startBox, new LinearLayout.LayoutParams(0, dp(68), 1));
        addGap(times, 8);
        times.addView(endBox, new LinearLayout.LayoutParams(0, dp(68), 1));
        timeSection.addView(times, marginTop(7));
        TextView duration = text(durationText(start[0], end[0]), 11, MUTED, false);
        timeSection.addView(duration, marginTop(5));
        box.addView(timeSection, marginTop(16));

        box.addView(fieldTitle("Not"), marginTop(16));
        EditText note = input("Eklemek istediğin bir detay...");
        note.setSingleLine(false);
        note.setMinLines(3);
        note.setGravity(Gravity.TOP | Gravity.START);
        note.setPadding(dp(15), dp(13), dp(15), dp(13));
        note.setText(editing == null || editing.note == null ? "" : editing.note);
        box.addView(note, marginTop(7));

        box.addView(fieldTitle("Tekrar"), marginTop(16));
        TextView repeatBox = editorChoice(repeatLabel(repeat[0]), "⌄");
        box.addView(repeatBox, marginTop(7));

        TextView hint = text("Hatırlatma Ayarlar'daki global bildirim süresine göre çalışır.",
                10, MUTED, false);
        box.addView(hint, marginTop(8));

        LinearLayout actions = row();
        TextView cancel = buttonText("Vazgeç", MUTED);
        TextView save = buttonText(editing == null ? "Planı oluştur" : "Değişiklikleri kaydet", Color.WHITE);
        save.setBackground(round(ORANGE, 18));
        cancel.setBackground(round(SURFACE_2, 18));
        actions.addView(cancel, new LinearLayout.LayoutParams(dp(100), dp(56)));
        addGap(actions, 8);
        actions.addView(save, new LinearLayout.LayoutParams(0, dp(56), 1));
        box.addView(actions, marginTop(18));

        dateBox.setOnClickListener(v -> {
            tap(v);
            showDateChooser(date[0], picked -> {
                date[0] = picked;
                dateBox.setText(prettyDate(picked));
            });
        });

        View.OnClickListener chooseStart = v -> {
            tap(v);
            showTimeChooser(val -> {
                start[0] = val;
                startBox.setText(val);
                duration.setText(durationText(start[0], end[0]));
            }, start[0]);
        };
        View.OnClickListener chooseEnd = v -> {
            tap(v);
            showTimeChooser(val -> {
                end[0] = val;
                endBox.setText(val);
                duration.setText(durationText(start[0], end[0]));
            }, end[0]);
        };
        startBox.setOnClickListener(chooseStart);
        endBox.setOnClickListener(chooseEnd);

        View.OnClickListener typeListener = v -> {
            tap(v);
            if (v == hourly) kind[0] = Plan.HOURLY;
            else if (v == flex) kind[0] = Plan.FLEX;
            else kind[0] = Plan.ALL_DAY;
            setEditorChip(hourly, Plan.HOURLY.equals(kind[0]));
            setEditorChip(flex, Plan.FLEX.equals(kind[0]));
            setEditorChip(allDay, Plan.ALL_DAY.equals(kind[0]));
            timeSection.setVisibility(Plan.ALL_DAY.equals(kind[0]) ? View.GONE : View.VISIBLE);
        };
        hourly.setOnClickListener(typeListener);
        flex.setOnClickListener(typeListener);
        allDay.setOnClickListener(typeListener);
        if (Plan.ALL_DAY.equals(kind[0])) timeSection.setVisibility(View.GONE);

        repeatBox.setOnClickListener(v -> {
            tap(v);
            showRepeatChooserCompat(repeatBox, val -> repeat[0] = val);
        });

        close.setOnClickListener(v -> { tap(v); d.dismiss(); });
        cancel.setOnClickListener(v -> { tap(v); d.dismiss(); });

        save.setOnClickListener(v -> {
            tap(v);
            String t = title.getText().toString().trim();
            if (t.isEmpty()) { title.setError("Plan adı gerekli"); return; }

            if (!Plan.ALL_DAY.equals(kind[0]) && start[0].isEmpty()) {
                toast("Başlangıç saatini seç.");
                return;
            }
            if (!Plan.ALL_DAY.equals(kind[0]) && !end[0].isEmpty() &&
                    start[0].equals(end[0])) {
                toast("Başlangıç ve bitiş aynı olamaz.");
                return;
            }

            Plan p = editing == null
                    ? new Plan(System.currentTimeMillis(), date[0], t, kind[0], start[0],
                    durationBetweenSafe(start[0], end[0]), false)
                    : editing;

            p.date = date[0];
            p.title = t;
            p.type = kind[0];
            p.startTime = Plan.ALL_DAY.equals(kind[0]) ? "" : start[0];
            p.endTime = Plan.ALL_DAY.equals(kind[0]) ? "" : end[0];
            p.time = p.startTime;
            p.duration = durationBetweenSafe(p.startTime, p.endTime);
            p.note = note.getText().toString().trim();
            p.repeat = repeat[0];

            if (editing == null) plans.add(p);
            PlanStore.save(this, plans);
            scheduleReminder(p);
            selectedDate = p.date;
            monthCursor = parseKey(p.date);
            d.dismiss();
            render();
        });

        scroll.addView(box);
        d.setContentView(scroll);
        d.show();
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawableResource(android.R.color.transparent);
            w.setDimAmount(0.58f);
            android.view.WindowManager.LayoutParams lp = w.getAttributes();
            lp.gravity = Gravity.BOTTOM;
            w.setAttributes(lp);
            w.setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.96f),
                    (int)(getResources().getDisplayMetrics().heightPixels * 0.88f));
        }
    }

    private TextView fieldTitle(String s) {
        return text(s.toUpperCase(tr), 10, MUTED, true);
    }

    private TextView editorChoice(String value, String trailing) {
        LinearLayout dummy = new LinearLayout(this);
        TextView t = text(value, 14, TEXT, true);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(dp(16), 0, dp(14), 0);
        t.setBackground(round(SURFACE_2, 17));
        t.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        return t;
    }

    private TextView editorChip(String s, boolean selected) {
        TextView t = text(s, 12, selected ? Color.WHITE : TEXT, true);
        t.setGravity(Gravity.CENTER);
        t.setBackground(round(selected ? ORANGE : SURFACE_2, 16));
        return t;
    }

    private void setEditorChip(TextView t, boolean selected) {
        t.setTextColor(selected ? Color.WHITE : TEXT);
        t.setBackground(round(selected ? ORANGE : SURFACE_2, 16));
    }


    private boolean reminderEnabled(TextView state) {
        Object tag = state.getTag();
        return tag != null && "Açık".equals(tag.toString());
    }

    private void refreshChips(TextView[] chips, String selected) {
        for (TextView c : chips) {
            boolean on = c.getText().toString().equals(selected);
            c.setTextColor(on ? Color.WHITE : TEXT);
            c.setBackground(round(on ? ORANGE : SURFACE_3, 17));
        }
    }

    private void showTimeChooser(TextView target,String existing){showTimeChooser(val->target.setText(val),existing);}

    private void showTimeChooser(TimeCallback callback,String existing){
        final Dialog d=styledDialog();LinearLayout box=dialogBox();
        box.addView(text("Saat seç",24,TEXT,true));
        box.addView(text("Yukarı / aşağı kaydır. Bıraktığında saat merkezde kilitlenir.",11,MUTED,false),marginTop(4));
        Calendar c=Calendar.getInstance();
        if(existing!=null&&existing.matches("\\d{2}:\\d{2}")){c.set(Calendar.HOUR_OF_DAY,Integer.parseInt(existing.substring(0,2)));c.set(Calendar.MINUTE,Integer.parseInt(existing.substring(3,5)));}
        final int[] hm={c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE)};
        TextView preview=text(String.format(Locale.GERMANY,"%02d:%02d",hm[0],hm[1]),40,TEXT,true);preview.setGravity(Gravity.CENTER);preview.setBackground(round(SURFACE_2,22));box.addView(preview,marginTop(13));
        LinearLayout wheels=row();
        WheelNumber hours=new WheelNumber(this,24,hm[0],v->{hm[0]=v;preview.setText(String.format(Locale.GERMANY,"%02d:%02d",hm[0],hm[1]));});
        WheelNumber mins=new WheelNumber(this,60,hm[1],v->{hm[1]=v;preview.setText(String.format(Locale.GERMANY,"%02d:%02d",hm[0],hm[1]));});
        wheels.addView(hours,new LinearLayout.LayoutParams(0,dp(220),1));TextView colon=text(":",34,TEXT,true);colon.setGravity(Gravity.CENTER);wheels.addView(colon,new LinearLayout.LayoutParams(dp(24),dp(80)));wheels.addView(mins,new LinearLayout.LayoutParams(0,dp(220),1));box.addView(wheels,marginTop(8));
        LinearLayout labels=row();TextView h=text("SAAT",9,MUTED,true),m=text("DAKİKA",9,MUTED,true);h.setGravity(Gravity.CENTER);m.setGravity(Gravity.CENTER);labels.addView(h,new LinearLayout.LayoutParams(0,dp(20),1));labels.addView(space(24),new LinearLayout.LayoutParams(dp(24),1));labels.addView(m,new LinearLayout.LayoutParams(0,dp(20),1));box.addView(labels);
        LinearLayout actions=row();TextView cancel=buttonText("Vazgeç",MUTED),save=buttonText("Saati seç",Color.WHITE);save.setGravity(Gravity.CENTER);save.setBackground(round(ORANGE,18));actions.addView(cancel,new LinearLayout.LayoutParams(dp(100),dp(52)));addGap(actions,8);actions.addView(save,new LinearLayout.LayoutParams(0,dp(52),1));box.addView(actions,marginTop(12));
        cancel.setOnClickListener(v->{tap(v);d.dismiss();});save.setOnClickListener(v->{tap(v);callback.value(String.format(Locale.GERMANY,"%02d:%02d",hm[0],hm[1]));d.dismiss();});
        d.setContentView(box);d.show();fitDialog(d,0.92f);
    }

    private class WheelNumber extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int max;private int value;private float offset;private float lastY;private android.view.VelocityTracker vt;private final android.widget.OverScroller scroller;private final ValueCallbackInt callback;private long lastTick;
        WheelNumber(Context c,int max,int initial,ValueCallbackInt cb){super(c);this.max=max;this.value=Math.max(0,Math.min(max-1,initial));this.callback=cb;this.scroller=new android.widget.OverScroller(c);setBackground(round(SURFACE_2,20));setFocusable(true);}
        private float rh(){return dp(52);}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);float center=getHeight()/2f,rh=rh();
            for(int i=-2;i<=2;i++){float y=center+i*rh+offset;float dist=Math.abs(y-center);float alpha=Math.max(.18f,1f-dist/(rh*2.7f));paint.setAlpha((int)(255*alpha));paint.setColor(dist<rh*.46f?TEXT:MUTED);paint.setTypeface(Typeface.create("sans",dist<rh*.46f?Typeface.BOLD:Typeface.NORMAL));paint.setTextSize(dp(dist<rh*.46f?31:25));String s=String.format(Locale.GERMANY,"%02d",wrap(value+i));Paint.FontMetrics fm=paint.getFontMetrics();canvas.drawText(s,getWidth()/2f-paint.measureText(s)/2f,y-(fm.ascent+fm.descent)/2f,paint);}
            paint.setAlpha(255);paint.setColor(ORANGE);paint.setStrokeWidth(dp(2));canvas.drawLine(dp(12),center-rh/2f,getWidth()-dp(12),center-rh/2f,paint);canvas.drawLine(dp(12),center+rh/2f,getWidth()-dp(12),center+rh/2f,paint);
            if(!scroller.isFinished()){scroller.computeScrollOffset();int sy=scroller.getCurrY();float delta=sy;float targetOffset=-delta;float movement=targetOffset-offset;offset+=movement;consume();postInvalidateOnAnimation();}
        }
        private void consume(){float r=rh();while(offset<=-r){offset+=r;value=wrap(value+1);notifyVal();wheelTick();}while(offset>=r){offset-=r;value=wrap(value-1);notifyVal();wheelTick();}}
        private void notifyVal(){callback.value(value);}
        private void wheelTick(){long now=System.currentTimeMillis();if(now-lastTick>30){lastTick=now;tickSound();}}
        private void snap(){float r=rh();int dir=Math.abs(offset)>r/2?(offset<0?1:-1):0;if(dir!=0){value=wrap(value+dir);notifyVal();wheelTick();}android.animation.ValueAnimator a=android.animation.ValueAnimator.ofFloat(offset,0);a.setDuration(155);a.setInterpolator(new android.view.animation.DecelerateInterpolator());a.addUpdateListener(v->{offset=(float)v.getAnimatedValue();invalidate();});a.start();}
        @Override public boolean onTouchEvent(MotionEvent e){switch(e.getActionMasked()){
            case MotionEvent.ACTION_DOWN:if(!scroller.isFinished())scroller.forceFinished(true);lastY=e.getY();vt=android.view.VelocityTracker.obtain();vt.addMovement(e);return true;
            case MotionEvent.ACTION_MOVE:float dy=e.getY()-lastY;lastY=e.getY();offset+=dy;consume();vt.addMovement(e);invalidate();return true;
            case MotionEvent.ACTION_UP:vt.addMovement(e);vt.computeCurrentVelocity(1000);float vy=vt.getYVelocity();vt.recycle();vt=null;if(Math.abs(vy)>420){int flingDy=(int)(-Math.max(-1700,Math.min(1700,vy))*0.22f);scroller.startScroll(0,0,0,flingDy,330);postInvalidateOnAnimation();}else snap();return true;
            case MotionEvent.ACTION_CANCEL:if(vt!=null){vt.recycle();vt=null;}snap();return true;
        }return true;}
        private int wrap(int n){int x=n%max;return x<0?x+max:x;}
    }

    

    private interface ValueCallbackInt { void value(int value); }

    private TextView findPreview(ViewGroup parent) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View v = parent.getChildAt(i);
            if (v instanceof TextView) {
                TextView t = (TextView) v;
                if (t.getText().toString().contains(":") && t.getTextSize() > 25) return t;
            }
        }
        return null;
    }

    private void showDateChooser(String initial, DateCallback callback) {
        final Dialog dialog = styledDialog();
        LinearLayout box = dialogBox();

        LinearLayout head = row();
        LinearLayout ht = column();
        ht.addView(text("Tarih seç", 23, TEXT, true));
        ht.addView(text("Geçmiş günler yeni plan için seçilemez.", 12, MUTED, false));
        head.addView(ht, new LinearLayout.LayoutParams(0, -2, 1));
        TextView close = iconButton("×", false);
        head.addView(close, new LinearLayout.LayoutParams(dp(44), dp(44)));
        box.addView(head);

        Calendar cursor = parseKey(initial);
        Calendar today = startOfDay(Calendar.getInstance());
        if (cursor.before(today)) cursor = today;
        final Calendar[] month = {(Calendar) cursor.clone()};

        LinearLayout monthRow = row();
        TextView prev = iconButton("‹", false);
        TextView label = text(capitalize(new SimpleDateFormat("MMMM yyyy", tr).format(month[0].getTime())),
                17, TEXT, true);
        label.setGravity(Gravity.CENTER);
        TextView next = iconButton("›", false);
        monthRow.addView(prev, new LinearLayout.LayoutParams(dp(44), dp(44)));
        monthRow.addView(label, new LinearLayout.LayoutParams(0, dp(44), 1));
        monthRow.addView(next, new LinearLayout.LayoutParams(dp(44), dp(44)));
        box.addView(monthRow, marginTop(12));

        LinearLayout calendarHost = column();
        box.addView(calendarHost, marginTop(6));

        Runnable rebuild = () -> buildDateGrid(calendarHost, month[0], today, selected -> {
            callback.pick(selected);
            dialog.dismiss();
        }, label);
        rebuild.run();

        prev.setOnClickListener(v -> {
            if (sameMonth(month[0], today)) return;
            tap(v);
            month[0].add(Calendar.MONTH, -1);
            if (month[0].before(newMonthStart(today))) month[0] = newMonthStart(today);
            rebuild.run();
        });
        next.setOnClickListener(v -> { tap(v); month[0].add(Calendar.MONTH, 1); rebuild.run(); });
        close.setOnClickListener(v -> { tap(v); dialog.dismiss(); });

        TextView todayBtn = buttonText("Bugün", ORANGE);
        todayBtn.setGravity(Gravity.CENTER);
        box.addView(todayBtn, marginTop(8));
        todayBtn.setOnClickListener(v -> {
            tap(v);
            callback.pick(key(today));
            dialog.dismiss();
        });

        dialog.setContentView(box);
        dialog.show();
        fitDialog(dialog, 0.92f);
    }

    private void buildDateGrid(LinearLayout host, Calendar month, Calendar today,
                               DateCallback callback, TextView label) {
        host.removeAllViews();
        label.setText(capitalize(new SimpleDateFormat("MMMM yyyy", tr).format(month.getTime())));
        LinearLayout names = row();
        for (String s : new String[]{"P","S","Ç","P","C","C","P"}) {
            TextView t = text(s, 10, MUTED, true);
            t.setGravity(Gravity.CENTER);
            names.addView(t, new LinearLayout.LayoutParams(0, dp(28), 1));
        }
        host.addView(names);

        Calendar first = (Calendar) month.clone();
        first.set(Calendar.DAY_OF_MONTH, 1);
        int offset = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        int days = month.getActualMaximum(Calendar.DAY_OF_MONTH);
        int ix = 0;

        for (int r = 0; r < 6; r++) {
            LinearLayout line = row();
            for (int c = 0; c < 7; c++) {
                int day = ix - offset + 1;
                TextView tv = text("", 13, TEXT, true);
                tv.setGravity(Gravity.CENTER);
                if (day > 0 && day <= days) {
                    tv.setText(String.valueOf(day));
                    Calendar date = (Calendar) month.clone();
                    date.set(Calendar.DAY_OF_MONTH, day);
                    boolean past = startOfDay(date).before(today);
                    if (past) {
                        tv.setTextColor(Color.rgb(63, 69, 77));
                    } else if (key(date).equals(key(today))) {
                        tv.setBackground(round(SURFACE_3, 15));
                    }
                    if (!past) tv.setOnClickListener(v -> { tap(v); callback.pick(key(date)); });
                }
                line.addView(tv, new LinearLayout.LayoutParams(0, dp(46), 1));
                ix++;
            }
            host.addView(line, new LinearLayout.LayoutParams(-1, dp(46)));
        }
    }

    private void showInputDialog(String title, String current, ValueCallback callback) {
        final Dialog dialog = styledDialog();
        LinearLayout box = dialogBox();

        box.addView(text(title, 22, TEXT, true));
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(current == null ? "" : current);
        input.setTextColor(TEXT);
        input.setTextSize(16);
        input.setHintTextColor(MUTED);
        input.setHint("Yaz...");
        input.setPadding(dp(14), 0, dp(14), 0);
        input.setBackground(round(SURFACE_2, 16));
        box.addView(input, marginTop(14));

        LinearLayout actions = row();
        TextView cancel = buttonText("Vazgeç", MUTED);
        TextView save = buttonText("Tamam", Color.WHITE);
        save.setGravity(Gravity.CENTER);
        save.setBackground(round(ORANGE, 18));
        actions.addView(cancel, new LinearLayout.LayoutParams(dp(90), dp(48)));
        actions.addView(space(8), new LinearLayout.LayoutParams(dp(8), 1));
        actions.addView(save, new LinearLayout.LayoutParams(0, dp(48), 1));
        box.addView(actions, marginTop(12));

        cancel.setOnClickListener(v -> { tap(v); dialog.dismiss(); });
        save.setOnClickListener(v -> { tap(v); callback.value(input.getText().toString().trim()); dialog.dismiss(); });

        dialog.setContentView(box);
        dialog.show();
        fitDialog(dialog, 0.90f);
        input.requestFocus();
        Window w = dialog.getWindow();
        if (w != null) w.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    private void showDeleteDialog(Plan p) {
        final Dialog dialog = styledDialog();
        LinearLayout box = dialogBox();
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView warn = text("Planı sil?", 23, TEXT, true);
        warn.setGravity(Gravity.CENTER);
        box.addView(warn);
        TextView title = text(p.title, 14, MUTED, false);
        title.setGravity(Gravity.CENTER);
        box.addView(title, marginTop(6));

        LinearLayout actions = row();
        TextView cancel = buttonText("Vazgeç", MUTED);
        TextView del = buttonText("Sil", Color.WHITE);
        del.setGravity(Gravity.CENTER);
        del.setBackground(round(RED, 18));
        actions.addView(cancel, new LinearLayout.LayoutParams(0, dp(50), 1));
        actions.addView(space(8), new LinearLayout.LayoutParams(dp(8), 1));
        actions.addView(del, new LinearLayout.LayoutParams(0, dp(50), 1));
        box.addView(actions, marginTop(18));

        cancel.setOnClickListener(v -> { tap(v); dialog.dismiss(); });
        del.setOnClickListener(v -> {
            tap(v);
            cancelReminder(p);
            plans.remove(p);
            PlanStore.save(this, plans);
            dialog.dismiss();
            render();
        });

        dialog.setContentView(box);
        dialog.show();
        fitDialog(dialog, 0.82f);
    }

    private void showSmartDialog() {
        final Dialog dialog = styledDialog();
        LinearLayout box = dialogBox();
        box.addView(text("Rotam AI", 23, TEXT, true));
        box.addView(text("Programını hızlıca gözden geçir.", 12, MUTED, false), marginTop(4));

        List<Plan> today = plansFor(key(Calendar.getInstance()));
        Plan next = null;
        for (Plan p : today) if (!p.done) { next = p; break; }

        LinearLayout insight = settingCard();
        insight.addView(text("Bugünün önerisi", 11, ORANGE, true));
        insight.addView(text(smartMessage(today), 16, TEXT, true));
        insight.addView(text(next == null ? "Yeni bir plan ekleyebilirsin." :
                "Sıradaki plan: " + next.title, 12, MUTED, false));
        box.addView(insight, marginTop(14));

        TextView addTomorrow = buttonText("Yarını planla", Color.WHITE);
        addTomorrow.setGravity(Gravity.CENTER);
        addTomorrow.setBackground(round(ORANGE, 18));
        box.addView(addTomorrow, marginTop(12));
        addTomorrow.setOnClickListener(v -> {
            tap(v);
            dialog.dismiss();
            showPlanEditor(null, key(addDays(Calendar.getInstance(), 1)));
        });

        TextView done = buttonText("Kapat", MUTED);
        box.addView(done, marginTop(4));
        done.setOnClickListener(v -> { tap(v); dialog.dismiss(); });

        dialog.setContentView(box);
        dialog.show();
        fitDialog(dialog, 0.88f);
    }

    private void showNotificationInfo() {
        final Dialog d = styledDialog();
        LinearLayout box = dialogBox();
        box.addView(text("Bildirimler", 22, TEXT, true));
        box.addView(text(remindersOn()
                ? "Saatli planlarda " + reminderLabel() + " önce bildirim gönderilir."
                : "Bildirimler kapalı.", 13, MUTED, false), marginTop(6));
        TextView settingsBtn = buttonText("Bildirim ayarları", Color.WHITE);
        settingsBtn.setGravity(Gravity.CENTER);
        settingsBtn.setBackground(round(ORANGE, 18));
        box.addView(settingsBtn, marginTop(14));
        TextView close = buttonText("Kapat", MUTED);
        box.addView(close, marginTop(4));
        settingsBtn.setOnClickListener(v -> {
            tap(v);
            d.dismiss();
            tab = 2;
            render();
        });
        close.setOnClickListener(v -> { tap(v); d.dismiss(); });
        d.setContentView(box);
        d.show();
        fitDialog(d, 0.86f);
    }

    private void showReminderChooser(TextView target) {
        final Dialog d = styledDialog();
        LinearLayout box = dialogBox();
        box.addView(text("Hatırlatma zamanı", 22, TEXT, true));
        box.addView(text("Saatli planlardan ne kadar önce bildirim gelsin?", 12, MUTED, false), marginTop(4));

        int[] values = {5, 10, 15, 30, 60};
        String[] labels = {"5 dakika", "10 dakika", "15 dakika", "30 dakika", "1 saat"};
        for (int i = 0; i < values.length; i++) {
            final int minutes = values[i];
            TextView option = buttonText(labels[i], minutes == reminderMinutes() ? Color.WHITE : TEXT);
            option.setGravity(Gravity.CENTER_VERTICAL);
            option.setPadding(dp(14), 0, dp(14), 0);
            option.setBackground(round(minutes == reminderMinutes() ? ORANGE : SURFACE_2, 15));
            box.addView(option, marginTop(i == 0 ? 12 : 6));
            option.setOnClickListener(v -> {
                tap(v);
                settings().edit().putInt("reminder_minutes", minutes).apply();
                target.setText(reminderLabel());
                for (Plan p : plans) scheduleReminder(p);
                d.dismiss();
            });
        }
        TextView cancel = buttonText("Vazgeç", MUTED);
        box.addView(cancel, marginTop(10));
        cancel.setOnClickListener(v -> { tap(v); d.dismiss(); });
        d.setContentView(box);
        d.show();
        fitDialog(d, 0.84f);
    }

    private void showAiSetupDialog() {
        final Dialog d=styledDialog();LinearLayout box=dialogBox();
        box.addView(text("Google Gemini",23,TEXT,true));
        box.addView(text("Rotam AI için Gemini API anahtarını ekle. Ücretsiz API katmanı ile başlayabilirsin.",12,MUTED,false),marginTop(4));
        EditText keyInput=input(hasAiKey()?"AIza… · kayıtlı anahtarı değiştirmek için yeni anahtar":"AIza…");
        keyInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        box.addView(keyInput,marginTop(12));
        TextView connect=buttonText(hasAiKey()?"Anahtarı güncelle":"Gemini'yi bağla",Color.WHITE);connect.setGravity(Gravity.CENTER);connect.setBackground(round(ORANGE,18));box.addView(connect,marginTop(10));
        if(hasAiKey()){
            TextView test=buttonText("Bağlantıyı test et",TEXT);test.setGravity(Gravity.CENTER);test.setBackground(round(SURFACE_2,18));box.addView(test,marginTop(7));
            test.setOnClickListener(v->{tap(v);new Thread(()->{String ans=callGemini("Sadece OK yaz.");runOnUiThread(()->toast(ans.equalsIgnoreCase("OK")?"Gemini bağlantısı çalışıyor.":ans));}).start();});
            TextView clear=buttonText("Gemini bağlantısını kaldır",RED);box.addView(clear,marginTop(7));clear.setOnClickListener(v->{tap(v);clearAiKey();d.dismiss();render();});
        }
        TextView close=buttonText("Kapat",MUTED);box.addView(close,marginTop(8));
        connect.setOnClickListener(v->{tap(v);String k=keyInput.getText().toString().trim();if(k.isEmpty()){toast("Gemini API anahtarını gir.");return;}saveAiKey(k);d.dismiss();render();});
        close.setOnClickListener(v->{tap(v);d.dismiss();});
        d.setContentView(box);d.show();fitDialog(d,0.91f);
    }

    private void showAiDialog() {
        if(!hasAiKey()){showAiSetupDialog();return;}
        final Dialog d=styledDialog();LinearLayout box=dialogBox();
        box.addView(text("Rotam AI",24,TEXT,true));
        box.addView(text("Gemini, planlarını ve boş zamanını birlikte değerlendirir.",12,MUTED,false),marginTop(4));
        LinearLayout presets=row();TextView a=buttonText("Bugünü analiz et",TEXT),b=buttonText("Planı sadeleştir",TEXT);a.setGravity(Gravity.CENTER);b.setGravity(Gravity.CENTER);a.setBackground(round(SURFACE_2,17));b.setBackground(round(SURFACE_2,17));
        presets.addView(a,new LinearLayout.LayoutParams(0,dp(48),1));addGap(presets,7);presets.addView(b,new LinearLayout.LayoutParams(0,dp(48),1));box.addView(presets,marginTop(12));
        EditText q=input("Örn. Bugün neyi öne almalıyım?");q.setSingleLine(false);q.setMinLines(2);q.setGravity(Gravity.TOP|Gravity.START);q.setPadding(dp(14),dp(12),dp(14),dp(12));box.addView(q,marginTop(10));
        TextView ask=buttonText("Gemini'ye sor",Color.WHITE);ask.setGravity(Gravity.CENTER);ask.setBackground(round(ORANGE,18));box.addView(ask,marginTop(9));
        TextView result=text("Yanıt burada görünecek.",13,MUTED,false);result.setPadding(dp(2),dp(12),dp(2),0);box.addView(result);
        Runnable run=()->{String prompt=q.getText().toString().trim();if(prompt.isEmpty())prompt="Bugünkü planlarımı analiz et ve en fazla 3 kısa, uygulanabilir öneri ver.";String payload=prompt+"\n\nRotam verisi:\n"+buildAiContext()+"\n\nTürkçe, kısa ve somut cevap ver.";result.setText("Gemini düşünüyor…");ask.setEnabled(false);new Thread(()->{String ans=callGemini(payload);runOnUiThread(()->{result.setText(ans);ask.setEnabled(true);});}).start();};
        ask.setOnClickListener(v->{tap(v);run.run();});
        a.setOnClickListener(v->{tap(v);q.setText("Bugünkü planlarımı analiz et. Öncelikleri, boşlukları ve olası çakışmaları söyle.");run.run();});
        b.setOnClickListener(v->{tap(v);q.setText("Bugünkü planımı daha gerçekçi hale getir. Gereksiz yoğunlukları bul ve sadeleştirme öner.");run.run();});
        TextView close=buttonText("Kapat",MUTED);box.addView(close,marginTop(8));close.setOnClickListener(v->{tap(v);d.dismiss();});
        d.setContentView(box);d.show();fitDialog(d,0.95f);
    }

    private String buildAiContext(){
        StringBuilder sb=new StringBuilder();String today=key(Calendar.getInstance());
        for(Plan p:plansFor(today)){sb.append("- ").append(p.title).append(" | ").append(planMeta(p));if(p.done)sb.append(" | tamamlandı");if(p.note!=null&&!p.note.isEmpty())sb.append(" | ").append(p.note);sb.append("\n");}
        if(sb.length()==0)sb.append("- Bugün için henüz plan yok.\n");return sb.toString();
    }

    private String callGemini(String prompt) {
        HttpURLConnection conn=null;
        try{
            String key=loadAiKey();if(key==null||key.isEmpty())return "Gemini anahtarı bulunamadı.";
            String model="gemini-3.5-flash-lite";
            URL url=new URL("https://generativelanguage.googleapis.com/v1beta/models/"+model+":generateContent?key="+java.net.URLEncoder.encode(key,StandardCharsets.UTF_8.name()));
            conn=(HttpURLConnection)url.openConnection();conn.setRequestMethod("POST");conn.setConnectTimeout(15000);conn.setReadTimeout(30000);conn.setDoOutput(true);conn.setRequestProperty("Content-Type","application/json");
            JSONObject body=new JSONObject();JSONArray contents=new JSONArray();JSONObject content=new JSONObject();JSONArray parts=new JSONArray();JSONObject part=new JSONObject();
            part.put("text","Sen Rotam uygulamasının planlama asistanısın. Kullanıcının planlarını gerçekçi biçimde düzenlemesine yardım et. Plan dışı bilgi uydurma. "+prompt);
            parts.put(part);content.put("parts",parts);contents.put(content);body.put("contents",contents);JSONObject cfg=new JSONObject();cfg.put("maxOutputTokens",450);body.put("generationConfig",cfg);
            try(OutputStream out=conn.getOutputStream()){out.write(body.toString().getBytes(StandardCharsets.UTF_8));}
            int code=conn.getResponseCode();InputStream in=code>=200&&code<300?conn.getInputStream():conn.getErrorStream();String raw=readStream(in);
            if(code<200||code>=300)return "Gemini bağlantısı başarısız ("+code+"). API anahtarını ve API projesini kontrol et.";
            JSONObject root=new JSONObject(raw);JSONArray candidates=root.optJSONArray("candidates");if(candidates==null||candidates.length()==0)return "Gemini boş yanıt verdi.";
            JSONObject cand=candidates.optJSONObject(0);JSONObject cont=cand==null?null:cand.optJSONObject("content");JSONArray out=cont==null?null:cont.optJSONArray("parts");if(out==null)return "Gemini boş yanıt verdi.";
            StringBuilder sb=new StringBuilder();for(int i=0;i<out.length();i++){JSONObject p=out.optJSONObject(i);if(p!=null){String t=p.optString("text","");if(!t.isEmpty())sb.append(t);}}
            return sb.length()==0?"Gemini boş yanıt verdi.":sb.toString().trim();
        }catch(Exception e){return "Gemini bağlantısı kurulamadı. İnternetini ve API anahtarını kontrol et.";}finally{if(conn!=null)conn.disconnect();}
    }

    private String readStream(InputStream in) throws Exception {
        if (in == null) return "";
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        return sb.toString();
    }

    private String extractOutputText(JSONObject root) {
        try {
            String direct = root.optString("output_text", "");
            if (!direct.isEmpty()) return direct;
            JSONArray output = root.optJSONArray("output");
            if (output == null) return "";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < output.length(); i++) {
                JSONObject item = output.optJSONObject(i);
                if (item == null) continue;
                JSONArray content = item.optJSONArray("content");
                if (content == null) continue;
                for (int j = 0; j < content.length(); j++) {
                    JSONObject part = content.optJSONObject(j);
                    if (part != null) {
                        String t = part.optString("text", "");
                        if (!t.isEmpty()) sb.append(t).append("\n");
                    }
                }
            }
            return sb.toString().trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    private LinearLayout settingRow(String title, String sub, boolean on, boolean showState, ToggleCallback cb) {
        LinearLayout card = settingCard();
        LinearLayout r = row();
        LinearLayout tx = column();
        tx.addView(text(title, 15, TEXT, true));
        tx.addView(text(sub, 11, MUTED, false));
        r.addView(tx, new LinearLayout.LayoutParams(0, -2, 1));
        TextView state = toggle(on);
        r.addView(state, new LinearLayout.LayoutParams(dp(62), dp(34)));
        card.addView(r);
        card.setOnClickListener(v -> {
            tap(v);
            boolean next = !"Açık".equals(state.getTag());
            setToggle(state, next);
            cb.change(next);
        });
        return card;
    }

    private TextView toggle(boolean on) {
        TextView t = text(on ? "Açık" : "Kapalı", 11, on ? Color.WHITE : MUTED, true);
        t.setGravity(Gravity.CENTER);
        t.setTag(on ? "Açık" : "Kapalı");
        t.setBackground(round(on ? ORANGE : SURFACE_3, 17));
        return t;
    }

    private void setToggle(TextView t, boolean on) {
        t.setTag(on ? "Açık" : "Kapalı");
        t.setText(on ? "Açık" : "Kapalı");
        t.setTextColor(on ? Color.WHITE : MUTED);
        t.setBackground(round(on ? ORANGE : SURFACE_3, 17));
    }

    private LinearLayout settingCard() {
        LinearLayout c = column();
        c.setPadding(dp(16), dp(14), dp(16), dp(14));
        c.setBackground(round(SURFACE, 18));
        c.setClickable(true);
        return c;
    }

    private TextView sectionTitle(String s) {
        return text(s.toUpperCase(tr), 10, MUTED, true);
    }

    private TextView field(String label, String value) {
        String display = value == null || value.isEmpty() ? fieldPlaceholder(label) : value;
        TextView t = text(display, 14, TEXT, false);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(dp(14), 0, dp(14), 0);
        t.setBackground(round(SURFACE_2, 16));
        t.setHint(label);
        t.setMaxLines(1);
        return t;
    }

    private String fieldPlaceholder(String label) {
        if ("Başlangıç".equals(label)) return "Başlangıç saati";
        if ("Bitiş".equals(label)) return "Bitiş saati";
        if ("Not".equals(label)) return "Not ekle";
        if ("Plan adı".equals(label)) return "Plan adı ekle";
        return label;
    }

    private String actualTime(String value) {
        return value != null && value.matches("\\d{2}:\\d{2}") ? value : "";
    }

    private TextView themeChoice(String label, boolean selected) {
        TextView t = buttonText(label, selected ? Color.WHITE : TEXT);
        t.setGravity(Gravity.CENTER);
        t.setBackground(round(selected ? ORANGE : SURFACE_2, 16));
        t.setOnClickListener(v -> {
            tap(v);
            settings().edit().putString("theme", "Gece".equals(label) ? "dark" : "light").apply();
            applyThemeColors();
            applySystemBars();
            buildShell();
            render();
        });
        return t;
    }

    private void tickSound() {
        if (!settings().getBoolean("sounds", true)) return;
        try {
            if (uiSoundPool == null) {
                android.media.AudioAttributes attrs = new android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();
                uiSoundPool = new android.media.SoundPool.Builder().setAudioAttributes(attrs).setMaxStreams(8).build();
                uiSoundPool.setOnLoadCompleteListener((pool, id, status) -> uiSoundLoaded = status == 0);
                uiTickId = uiSoundPool.load(this, R.raw.ui_tick, 1);
                return;
            }
            if (uiSoundLoaded && uiTickId != 0) uiSoundPool.play(uiTickId, 0.45f, 0.45f, 1, 0, 1.0f);
        } catch (Exception ignored) {}
    }

    private TextView label(String s){return text(s.toUpperCase(tr),10,MUTED,true);}
    private EditText input(String hint){EditText e=new EditText(this);e.setSingleLine(true);e.setTextColor(TEXT);e.setHintTextColor(MUTED);e.setHint(hint);e.setTextSize(15);e.setPadding(dp(14),0,dp(14),0);e.setBackground(round(SURFACE_2,18));return e;}
    private TextView bigChoice(String value,int ignored){TextView t=text(value,15,TEXT,true);t.setGravity(Gravity.CENTER_VERTICAL);t.setPadding(dp(15),0,dp(15),0);t.setBackground(round(SURFACE_2,18));return t;}
    private TextView chip(String s,boolean selected){TextView t=text(s,12,selected?Color.WHITE:TEXT,true);t.setGravity(Gravity.CENTER);t.setBackground(round(selected?ORANGE:SURFACE_2,16));return t;}
    private void setChip(TextView t,boolean selected){t.setTextColor(selected?Color.WHITE:TEXT);t.setBackground(round(selected?ORANGE:SURFACE_2,16));}
    private String durationBetweenSafe(String a,String b){return Plan.durationBetween(a,b);}
    private LinearLayout buildCalendarEmptyCard(){LinearLayout c=card();c.addView(text("Bu gün için plan yok.",16,TEXT,true));c.addView(text("Takvimden bir gün seç veya yeni plan oluştur.",12,MUTED,false),marginTop(5));return c;}

    private String durationText(String a,String b){if(a==null||a.isEmpty())return "Başlangıç saatini seç.";if(b==null||b.isEmpty())return "Bitiş saati isteğe bağlı.";return "Süre: "+Plan.durationBetween(a,b);}
    private int countDone(List<Plan> list){int n=0;for(Plan p:list)if(p.done)n++;return n;}
    private String prettyLabel(String key){return capitalize(new SimpleDateFormat("EEE d MMM",tr).format(parseKey(key).getTime()));}
    private String repeatLabel(String v){if(Plan.REPEAT_DAILY.equals(v))return "Her gün";if(Plan.REPEAT_WEEKDAYS.equals(v))return "Hafta içi";if(Plan.REPEAT_WEEKLY.equals(v))return "Her hafta";return "Tek sefer";}
    private void addGap(LinearLayout l,int v){l.addView(space(v),new LinearLayout.LayoutParams(dp(v),1));}
    private LinearLayout settingAction(String title,String sub,String value,View.OnClickListener listener){LinearLayout c=settingCard();LinearLayout r=row();LinearLayout tx=column();tx.addView(text(title,15,TEXT,true));tx.addView(text(sub,11,MUTED,false),marginTop(3));r.addView(tx,new LinearLayout.LayoutParams(0,-2,1));TextView val=buttonText(value,ORANGE);val.setGravity(Gravity.CENTER);val.setBackground(round(ORANGE_SOFT,14));r.addView(val,new LinearLayout.LayoutParams(dp(92),dp(36)));c.addView(r);c.setOnClickListener(listener);return c;}
    private String quietStart(){return settings().getString("quiet_start","23:00");}
    private String quietEnd(){return settings().getString("quiet_end","08:00");}
    private String quietHoursLabel(){return quietStart()+" – "+quietEnd();}
    private void showQuietHoursDialog(){final Dialog d=styledDialog();LinearLayout box=dialogBox();box.addView(text("Sessiz saatler",22,TEXT,true));box.addView(text("Bu saatlerde plan bildirimi gönderilmez.",12,MUTED,false),marginTop(4));EditText s=input("Başlangıç · 23:00"),e=input("Bitiş · 08:00");s.setText(quietStart());e.setText(quietEnd());box.addView(s,marginTop(12));box.addView(e,marginTop(7));TextView save=buttonText("Kaydet",Color.WHITE);save.setGravity(Gravity.CENTER);save.setBackground(round(ORANGE,18));box.addView(save,marginTop(10));save.setOnClickListener(v->{tap(v);if(actualTime(s.getText().toString()).isEmpty()||actualTime(e.getText().toString()).isEmpty()){toast("Saatleri 24 saat formatında gir.");return;}settings().edit().putString("quiet_start",s.getText().toString()).putString("quiet_end",e.getText().toString()).apply();for(Plan p:plans)scheduleReminder(p);d.dismiss();render();});d.setContentView(box);d.show();fitDialog(d,0.9f);}
    private void exportPlans(){try{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,"rotam-yedek.json");startActivityForResult(i,909);}catch(Exception e){toast("Yedekleme açılamadı.");}}
    private void showRepeatChooserCompat(TextView target,ValueCallback callback){final Dialog d=styledDialog();LinearLayout box=dialogBox();box.addView(text("Tekrar",22,TEXT,true));String[] labels={"Tek sefer","Her gün","Hafta içi","Her hafta"};String[] vals={Plan.REPEAT_NONE,Plan.REPEAT_DAILY,Plan.REPEAT_WEEKDAYS,Plan.REPEAT_WEEKLY};for(int i=0;i<labels.length;i++){final String val=vals[i];TextView b=buttonText(labels[i],TEXT);b.setGravity(Gravity.CENTER_VERTICAL);b.setBackground(round(SURFACE_2,17));box.addView(b,marginTop(7));b.setOnClickListener(v->{tap(v);callback.value(val);d.dismiss();});}TextView close=buttonText("Kapat",MUTED);box.addView(close,marginTop(8));close.setOnClickListener(v->{tap(v);d.dismiss();});d.setContentView(box);d.show();fitDialog(d,0.84f);}
    
    private TextView buttonText(String s, int color) {
        TextView t = text(s, 13, color, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(10), 0, dp(10), 0);
        return t;
    }

    private TextView actionText(String s, int color) {
        TextView t = text(s, 12, color, true);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private TextView iconButton(String s, boolean border) {
        TextView t = text(s, 22, TEXT, false);
        t.setGravity(Gravity.CENTER);
        t.setBackground(round(border ? SURFACE_2 : SURFACE, 15));
        return t;
    }

    private ImageView logo(int size) {
        ImageView v = new ImageView(this);
        v.setImageResource(R.drawable.rotam_logo);
        v.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        v.setPadding(dp(2), dp(2), dp(2), dp(2));
        return v;
    }

    private Dialog styledDialog() {
        Dialog d = new Dialog(this);
        d.setCancelable(true);
        return d;
    }

    private LinearLayout dialogBox() {
        LinearLayout box = column();
        box.setPadding(dp(18), dp(16), dp(18), dp(16));
        box.setBackground(round(Color.rgb(14, 17, 21), 28));
        return box;
    }

    private void fitDialog(Dialog d, float widthFactor) {
        if (d.getWindow() != null) {
            d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            d.getWindow().setLayout((int) (getResources().getDisplayMetrics().widthPixels * widthFactor), -2);
        }
    }

    private LinearLayout scrollColumn() {
        ScrollView s = new ScrollView(this);
        s.setFillViewport(true);
        s.setBackgroundColor(BG);
        s.setClipToPadding(false);
        LinearLayout c = column();
        c.setPadding(0, 0, 0, dp(22));
        s.addView(c, new ScrollView.LayoutParams(-1, -2));
        content.addView(s, new LinearLayout.LayoutParams(-1, 0, 1));
        return c;
    }

    private LinearLayout card() {
        LinearLayout c = column();
        c.setPadding(dp(16), dp(16), dp(16), dp(16));
        c.setBackground(round(SURFACE, 22));
        c.setElevation(dp(0));
        return c;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private View space(int px) {
        View v = new View(this);
        return v;
    }

    private TextView text(String s, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setTypeface(Typeface.create("sans", bold ? Typeface.BOLD : Typeface.NORMAL));
        t.setIncludeFontPadding(false);
        t.setLineSpacing(0f, 1.08f);
        return t;
    }

    private LinearLayout.LayoutParams margin(int l, int top, int r, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(dp(l), dp(top), dp(r), dp(bottom));
        return p;
    }

    private LinearLayout.LayoutParams marginTop(int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(top);
        return p;
    }

    private LinearLayout.LayoutParams newMargin(int l, int top, int r, int bottom) {
        return margin(l, top, r, bottom);
    }

    private GradientDrawable round(int color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp((int) radius));
        return g;
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) return value;
        return value.substring(0, 1).toUpperCase(tr) + value.substring(1);
    }

    private String prettyDate(String key) {
        return capitalize(new SimpleDateFormat("EEEE · d MMMM yyyy", tr).format(parseKey(key).getTime()));
    }

    private String safe(String a, String b) {
        return a != null && !a.isEmpty() ? a : (b == null ? "" : b);
    }

    private List<Plan> plansFor(String date) {
        List<Plan> out = new ArrayList<>();
        for (Plan p : plans) if (date.equals(p.date)) out.add(p);
        Collections.sort(out, Comparator
                .comparing((Plan p) -> p.startTime == null || p.startTime.isEmpty() ? "99:99" : p.startTime)
                .thenComparing(p -> p.title == null ? "" : p.title));
        return out;
    }

    private String key(Calendar c) {
        return String.format(Locale.GERMANY, "%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    private Calendar parseKey(String k) {
        Calendar c = Calendar.getInstance();
        try {
            String[] p = k.split("-");
            c.set(Integer.parseInt(p[0]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[2]), 0, 0, 0);
            c.set(Calendar.MILLISECOND, 0);
        } catch (Exception ignored) {}
        return c;
    }

    private Calendar addDays(Calendar c, int n) {
        Calendar x = (Calendar) c.clone();
        x.add(Calendar.DAY_OF_MONTH, n);
        x.set(Calendar.HOUR_OF_DAY, 0);
        x.set(Calendar.MINUTE, 0);
        x.set(Calendar.SECOND, 0);
        x.set(Calendar.MILLISECOND, 0);
        return x;
    }

    private Calendar startOfDay(Calendar c) {
        Calendar x = (Calendar) c.clone();
        x.set(Calendar.HOUR_OF_DAY, 0);
        x.set(Calendar.MINUTE, 0);
        x.set(Calendar.SECOND, 0);
        x.set(Calendar.MILLISECOND, 0);
        return x;
    }

    private Calendar newMonthStart(Calendar c) {
        Calendar x = startOfDay(c);
        x.set(Calendar.DAY_OF_MONTH, 1);
        return x;
    }

    private boolean sameMonth(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.MONTH) == b.get(Calendar.MONTH);
    }

    private boolean isCurrentOrFutureMonth(Calendar a) {
        Calendar today = newMonthStart(Calendar.getInstance());
        Calendar x = newMonthStart(a);
        return !x.before(today);
    }

    private void scheduleReminder(Plan p) {
        cancelReminder(p);
        if (!remindersOn() || p.done || !Plan.HOURLY.equals(p.type) || p.startTime == null ||
                !p.startTime.matches("\\d{2}:\\d{2}")) return;

        Calendar alarm = parseKey(p.date);
        int hour = Integer.parseInt(p.startTime.substring(0, 2));
        int minute = Integer.parseInt(p.startTime.substring(3));
        alarm.set(Calendar.HOUR_OF_DAY, hour);
        alarm.set(Calendar.MINUTE, minute);
        alarm.set(Calendar.SECOND, 0);
        alarm.set(Calendar.MILLISECOND, 0);
        alarm.add(Calendar.MINUTE, -reminderMinutes());
        if (alarm.getTimeInMillis() <= System.currentTimeMillis()) return;

        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("title", p.title);
        PendingIntent pi = PendingIntent.getBroadcast(
                this, (int) (p.id & 0x7fffffff), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am != null) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarm.getTimeInMillis(), pi);
    }

    private void cancelReminder(Plan p) {
        Intent i = new Intent(this, ReminderReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(
                this, (int) (p.id & 0x7fffffff), i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am != null) am.cancel(pi);
        try { pi.cancel(); } catch (Exception ignored) {}
    }

    private interface TimeCallback { void value(String value); }
    private interface DateCallback { void pick(String date); }
    private interface ValueCallback { void value(String value); }
    private interface ToggleCallback { void change(boolean enabled); }

    private class ProgressRing extends View {
        private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint progress = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float value;
        private String text = "0%";

        ProgressRing(Context context) {
            super(context);
            track.setStyle(Paint.Style.STROKE);
            track.setStrokeWidth(7f);
            track.setColor(Color.rgb(45, 50, 58));
            progress.setStyle(Paint.Style.STROKE);
            progress.setStrokeWidth(7f);
            progress.setStrokeCap(Paint.Cap.ROUND);
            progress.setColor(ORANGE);
            label.setColor(TEXT);
            label.setTextSize(22f);
            label.setTypeface(Typeface.DEFAULT_BOLD);
        }

        void setProgress(float p) {
            value = Math.max(0f, Math.min(1f, p));
            invalidate();
        }

        void setLabel(String t) {
            text = t;
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float radius = Math.min(getWidth(), getHeight()) / 2f - 8f;
            RectF r = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);
            canvas.drawArc(r, -90, 360, false, track);
            canvas.drawArc(r, -90, 360 * value, false, progress);
            float tw = label.measureText(text);
            Paint.FontMetrics fm = label.getFontMetrics();
            float th = fm.descent - fm.ascent;
            canvas.drawText(text, cx - tw / 2f, cy - (fm.ascent + fm.descent) / 2f, label);
        }
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==909&&resultCode==RESULT_OK&&data!=null&&data.getData()!=null){
            try{
                JSONArray array=new JSONArray();
                for(Plan p:plans){JSONObject o=new JSONObject();o.put("id",p.id);o.put("date",p.date);o.put("title",p.title);o.put("type",p.type);o.put("time",p.time);o.put("duration",p.duration);o.put("done",p.done);o.put("startTime",p.startTime==null?"":p.startTime);o.put("endTime",p.endTime==null?"":p.endTime);o.put("note",p.note==null?"":p.note);o.put("repeat",p.repeat==null?Plan.REPEAT_NONE:p.repeat);array.put(o);}
                OutputStream out=getContentResolver().openOutputStream(data.getData());if(out!=null){out.write(array.toString(2).getBytes(StandardCharsets.UTF_8));out.close();toast("Rotam yedeği kaydedildi.");}
            }catch(Exception ignored){toast("Yedek oluşturulamadı.");}
        }
    }

    private void toast(String message){android.widget.Toast.makeText(this,message,android.widget.Toast.LENGTH_SHORT).show();}

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
