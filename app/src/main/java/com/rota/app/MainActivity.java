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

    private void tap(View v) {
        if (v != null) {
            v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(70)
                    .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(110).start()).start();
            if (hapticsOn()) {
                try { v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); } catch (Exception ignored) {}
            }
        }
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

        int done = 0;
        Plan next = null;
        for (Plan p : today) {
            if (p.done) done++;
            else if (next == null) next = p;
        }

        LinearLayout page = scrollColumn();
        addTopBar(page, true, false);

        LinearLayout intro = column();
        TextView greeting = text("Bugün", 31, TEXT, true);
        TextView date = text(capitalize(new SimpleDateFormat("EEEE · d MMMM", tr).format(now.getTime())),
                14, MUTED, false);
        intro.addView(greeting);
        intro.addView(date);
        page.addView(intro, margin(20, 8, 20, 16));

        LinearLayout hero = card();
        LinearLayout hrow = row();
        LinearLayout hleft = column();
        hleft.addView(text("Günün akışı", 13, MUTED, true));
        hleft.addView(text(done + " / " + today.size() + " tamamlandı", 23, TEXT, true));
        hleft.addView(text(today.isEmpty() ? "Bugün için bir plan ekle." :
                "Küçük adımlar, daha net bir gün.", 12, MUTED, false));
        hrow.addView(hleft, new LinearLayout.LayoutParams(0, -2, 1));

        ProgressRing ring = new ProgressRing(this);
        ring.setProgress(today.isEmpty() ? 0f : done / (float) today.size());
        ring.setLabel(today.isEmpty() ? "0%" : Math.round(done * 100f / today.size()) + "%");
        hrow.addView(ring, new LinearLayout.LayoutParams(dp(90), dp(90)));
        hero.addView(hrow);
        hero.addView(progressBar(done, today.size()));
        page.addView(hero, margin(20, 0, 20, 14));

        LinearLayout stats = row();
        stats.addView(statBox(String.valueOf(today.size()), "Plan"), new LinearLayout.LayoutParams(0, dp(72), 1));
        stats.addView(space(8), new LinearLayout.LayoutParams(dp(8), 1));
        stats.addView(statBox(String.valueOf(done), "Tamamlandı"), new LinearLayout.LayoutParams(0, dp(72), 1));
        stats.addView(space(8), new LinearLayout.LayoutParams(dp(8), 1));
        stats.addView(statBox(freeTimeText(today), "Boş zaman"), new LinearLayout.LayoutParams(0, dp(72), 1));
        page.addView(stats, margin(20, 0, 20, 14));

        LinearLayout smart = card();
        smart.setBackground(round(Color.rgb(20, 23, 28), 22));
        LinearLayout sr = row();
        TextView aiDot = text("✦", 18, ORANGE, true);
        sr.addView(aiDot, new LinearLayout.LayoutParams(dp(30), dp(36)));
        LinearLayout aiText = column();
        aiText.addView(text("ROTAM AI", 10, ORANGE, true));
        aiText.addView(text(smartMessage(today), 15, TEXT, true));
        aiText.addView(text("Programına göre küçük bir öneri.", 12, MUTED, false));
        sr.addView(aiText, new LinearLayout.LayoutParams(0, -2, 1));
        TextView go = text("›", 26, TEXT, false);
        go.setGravity(Gravity.CENTER);
        sr.addView(go, new LinearLayout.LayoutParams(dp(32), dp(44)));
        smart.addView(sr);
        smart.setOnClickListener(v -> { tap(v); showSmartDialog(); });
        page.addView(smart, margin(20, 0, 20, 18));

        LinearLayout titleRow = row();
        titleRow.addView(text("Bugünün planı", 20, TEXT, true),
                new LinearLayout.LayoutParams(0, -2, 1));
        TextView all = text(today.isEmpty() ? "+ Plan ekle" : "Takvime git", 12, MUTED, true);
        titleRow.addView(all);
        all.setOnClickListener(v -> {
            tap(v);
            tab = 1;
            selectedDate = todayKey;
            monthCursor = now;
            render();
        });
        page.addView(titleRow, margin(20, 0, 20, 10));

        if (today.isEmpty()) {
            LinearLayout empty = card();
            empty.addView(text("Günün boş.", 18, TEXT, true));
            empty.addView(text("Saatli, esnek veya tüm gün bir plan oluştur.", 13, MUTED, false));
            TextView action = buttonText("İlk planı ekle", ORANGE);
            empty.addView(action, marginTop(14));
            action.setOnClickListener(v -> { tap(v); showPlanEditor(null, todayKey); });
            page.addView(empty, margin(20, 0, 20, 20));
        } else {
            for (int i = 0; i < today.size(); i++) {
                View c = addPlanCard(today.get(i), i);
                page.addView(c, margin(20, 0, 20, 10));
            }
        }
    }

    private void renderPlan() {
        Calendar today = Calendar.getInstance();
        String todayKey = key(today);
        if (parseKey(selectedDate).before(startOfDay(today))) {
            selectedDate = todayKey;
            monthCursor = (Calendar) today.clone();
        }

        LinearLayout page = scrollColumn();
        addTopBar(page, false, true);

        LinearLayout title = column();
        title.addView(text("Plan", 28, TEXT, true));
        title.addView(text("Takvimin ve yaklaşan günlerin.", 14, MUTED, false));
        page.addView(title, margin(20, 8, 20, 12));

        LinearLayout week = row();
        Calendar cursor = startOfDay(today);
        for (int i = 0; i < 7; i++) {
            Calendar d = addDays(cursor, i);
            String k = key(d);
            boolean selected = k.equals(selectedDate);
            LinearLayout cell = column();
            cell.setGravity(Gravity.CENTER);
            cell.setPadding(dp(8), dp(9), dp(8), dp(9));
            cell.setBackground(round(selected ? ORANGE : SURFACE, 18));
            TextView dn = text(new SimpleDateFormat("EEE", tr).format(d.getTime()).substring(0, 1).toUpperCase(tr),
                    11, selected ? Color.WHITE : MUTED, true);
            TextView num = text(String.valueOf(d.get(Calendar.DAY_OF_MONTH)), 19,
                    selected ? Color.WHITE : TEXT, true);
            num.setGravity(Gravity.CENTER);
            cell.addView(dn);
            cell.addView(num);
            boolean has = !plansFor(k).isEmpty();
            if (has) cell.addView(text("•", 12, selected ? Color.WHITE : ORANGE, true));
            else cell.addView(text(" ", 12, selected ? Color.WHITE : MUTED, true));
            cell.setOnClickListener(v -> { tap(v); selectedDate = k; monthCursor = d; render(); });
            week.addView(cell, new LinearLayout.LayoutParams(0, dp(80), 1));
            if (i < 6) week.addView(space(5), new LinearLayout.LayoutParams(dp(5), 1));
        }
        page.addView(week, margin(20, 0, 20, 16));

        LinearLayout calHeader = row();
        String monthLabel = capitalize(new SimpleDateFormat("MMMM yyyy", tr).format(monthCursor.getTime()));
        calHeader.addView(text(monthLabel, 18, TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));

        TextView prev = iconButton("‹", false);
        TextView next = iconButton("›", false);
        prev.setAlpha(isCurrentOrFutureMonth(monthCursor) && sameMonth(monthCursor, today) ? 0.28f : 1f);
        prev.setOnClickListener(v -> {
            if (sameMonth(monthCursor, today)) return;
            tap(v);
            monthCursor.add(Calendar.MONTH, -1);
            Calendar now = Calendar.getInstance();
            Calendar candidate = (Calendar) monthCursor.clone();
            if (candidate.before(newMonthStart(now))) monthCursor = newMonthStart(now);
            render();
        });
        next.setOnClickListener(v -> { tap(v); monthCursor.add(Calendar.MONTH, 1); render(); });
        LinearLayout arrows = row();
        arrows.addView(prev, new LinearLayout.LayoutParams(dp(44), dp(44)));
        arrows.addView(space(8), new LinearLayout.LayoutParams(dp(8), 1));
        arrows.addView(next, new LinearLayout.LayoutParams(dp(44), dp(44)));
        calHeader.addView(arrows);
        page.addView(calHeader, margin(20, 0, 20, 8));

        LinearLayout weekNames = row();
        String[] names = {"P","S","Ç","P","C","C","P"};
        for (String n : names) {
            TextView t = text(n, 11, MUTED, true);
            t.setGravity(Gravity.CENTER);
            weekNames.addView(t, new LinearLayout.LayoutParams(0, dp(30), 1));
        }
        page.addView(weekNames, margin(20, 0, 20, 2));

        LinearLayout grid = column();
        Calendar first = (Calendar) monthCursor.clone();
        first.set(Calendar.DAY_OF_MONTH, 1);
        int offset = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        int days = monthCursor.getActualMaximum(Calendar.DAY_OF_MONTH);
        int cellIndex = 0;

        for (int r = 0; r < 6; r++) {
            LinearLayout line = row();
            for (int c = 0; c < 7; c++) {
                int day = cellIndex - offset + 1;
                TextView d = text("", 13, TEXT, true);
                d.setGravity(Gravity.CENTER);
                if (day > 0 && day <= days) {
                    d.setText(String.valueOf(day));
                    Calendar date = (Calendar) monthCursor.clone();
                    date.set(Calendar.DAY_OF_MONTH, day);
                    String k = key(date);
                    boolean past = startOfDay(date).before(startOfDay(today));
                    boolean selected = k.equals(selectedDate);
                    boolean isToday = k.equals(todayKey);
                    boolean has = !plansFor(k).isEmpty();

                    if (selected) {
                        d.setTextColor(Color.WHITE);
                        d.setBackground(round(ORANGE, 16));
                    } else if (isToday) {
                        d.setTextColor(TEXT);
                        d.setBackground(round(SURFACE_3, 16));
                    } else if (past) {
                        d.setTextColor(Color.rgb(68, 73, 81));
                    } else {
                        d.setTextColor(TEXT);
                    }

                    if (has && !selected) d.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                    if (has) d.setText(day + "\n•");

                    if (!past) {
                        d.setOnClickListener(v -> { tap(v); selectedDate = k; monthCursor = date; render(); });
                    } else {
                        d.setClickable(false);
                        d.setAlpha(0.34f);
                    }
                }
                line.addView(d, new LinearLayout.LayoutParams(0, dp(50), 1));
                cellIndex++;
            }
            grid.addView(line, new LinearLayout.LayoutParams(-1, dp(50)));
        }
        LinearLayout calCard = card();
        calCard.addView(grid);
        page.addView(calCard, margin(20, 0, 20, 16));

        LinearLayout selectedHeader = row();
        String selectedTitle = capitalize(new SimpleDateFormat("EEEE · d MMMM", tr).format(parseKey(selectedDate).getTime()));
        selectedHeader.addView(text(selectedTitle, 18, TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));
        TextView add = buttonText("+ Plan", ORANGE);
        selectedHeader.addView(add);
        add.setOnClickListener(v -> { tap(v); showPlanEditor(null, selectedDate); });
        page.addView(selectedHeader, margin(20, 0, 20, 10));

        List<Plan> selected = plansFor(selectedDate);
        if (selected.isEmpty()) {
            LinearLayout empty = card();
            empty.addView(text("Bu gün için plan yok.", 16, TEXT, true));
            empty.addView(text("Yeni bir plan ekleyerek gününü doldur.", 13, MUTED, false));
            page.addView(empty, margin(20, 0, 20, 18));
        } else {
            for (int i = 0; i < selected.size(); i++) {
                page.addView(addPlanCard(selected.get(i), i), margin(20, 0, 20, 10));
            }
        }
    }

    private void renderSettings() {
        LinearLayout page = scrollColumn();
        addTopBar(page, false, false);

        LinearLayout title = column();
        title.addView(text("Ayarlar", 28, TEXT, true));
        title.addView(text("Rotam'ı kendi akışına göre ayarla.", 14, MUTED, false));
        page.addView(title, margin(20, 8, 20, 16));

        LinearLayout brand = card();
        LinearLayout br = row();
        ImageView logo = logo(54);
        br.addView(logo, new LinearLayout.LayoutParams(dp(54), dp(54)));
        LinearLayout bx = column();
        bx.addView(text("Rotam", 18, TEXT, true));
        bx.addView(text("v0.4.0  ·  Daha düzenli bir sen.", 12, MUTED, false));
        br.addView(bx, new LinearLayout.LayoutParams(0, -2, 1));
        TextView arrow = text("›", 25, MUTED, false);
        arrow.setGravity(Gravity.CENTER);
        br.addView(arrow, new LinearLayout.LayoutParams(dp(30), dp(44)));
        brand.addView(br);
        page.addView(brand, margin(20, 0, 20, 16));

        page.addView(sectionTitle("Bildirimler"), margin(20, 0, 20, 7));
        page.addView(settingRow("Bildirimler", "Plan saatinde hatırlatıcı göster", remindersOn(), true,
                enabled -> settings().edit().putBoolean("reminders", enabled).apply()), margin(20, 0, 20, 8));
        page.addView(settingRow("Dokunma titreşimi", "Butonlarda hafif fiziksel geri bildirim", hapticsOn(), true,
                enabled -> settings().edit().putBoolean("haptics", enabled).apply()), margin(20, 0, 20, 8));
        page.addView(settingRow("Hızlı plan", "Yeni plan açarken seçili günü koru", quickPlanOn(), true,
                enabled -> settings().edit().putBoolean("quick_plan", enabled).apply()), margin(20, 0, 20, 16));

        page.addView(sectionTitle("Görünüm"), margin(20, 0, 20, 7));
        LinearLayout themeCard = settingCard();
        themeCard.addView(text("Tema", 16, TEXT, true));
        themeCard.addView(text("Sadece iki seçenek: gece veya gündüz.", 12, MUTED, false), marginTop(3));
        LinearLayout themeChoices = row();
        TextView dark = themeChoice("Gece", "dark".equals(settings().getString("theme", "dark")));
        TextView light = themeChoice("Gündüz", "light".equals(settings().getString("theme", "dark")));
        themeChoices.addView(dark, new LinearLayout.LayoutParams(0, dp(44), 1));
        themeChoices.addView(space(8), new LinearLayout.LayoutParams(dp(8), 1));
        themeChoices.addView(light, new LinearLayout.LayoutParams(0, dp(44), 1));
        themeCard.addView(themeChoices, marginTop(12));
        page.addView(themeCard, margin(20, 0, 20, 14));

        page.addView(sectionTitle("Rotam AI"), margin(20, 8, 20, 7));
        LinearLayout ai = settingCard();
        LinearLayout ar = row();
        TextView sparkle = text("✦", 22, ORANGE, true);
        ar.addView(sparkle, new LinearLayout.LayoutParams(dp(34), dp(44)));
        LinearLayout ax = column();
        ax.addView(text("Akıllı planlama", 16, TEXT, true));
        ax.addView(text("Gününü analiz et ve öneriler al", 12, MUTED, false));
        ar.addView(ax, new LinearLayout.LayoutParams(0, -2, 1));
        TextView aiArrow = text("›", 25, MUTED, false);
        aiArrow.setGravity(Gravity.CENTER);
        ar.addView(aiArrow, new LinearLayout.LayoutParams(dp(32), dp(44)));
        ai.addView(ar);
        ai.setOnClickListener(v -> { tap(v); showSmartDialog(); });
        page.addView(ai, margin(20, 0, 20, 8));

        LinearLayout about = settingCard();
        about.addView(text("Hakkında", 16, TEXT, true));
        about.addView(text("Rotam · Sade planlama, net bir gün.", 12, MUTED, false));
        page.addView(about, margin(20, 0, 20, 22));
    }

    private void addTopBar(LinearLayout page, boolean showBell, boolean showCalendar) {
        LinearLayout top = row();
        ImageView logo = logo(38);
        top.addView(logo, new LinearLayout.LayoutParams(dp(38), dp(38)));
        LinearLayout tx = column();
        tx.setPadding(dp(10), 0, 0, 0);
        tx.addView(text("Rotam", 16, TEXT, true));
        tx.addView(text("Daha düzenli bir sen.", 10, MUTED, false));
        top.addView(tx, new LinearLayout.LayoutParams(0, dp(44), 1));

        if (showBell) {
            ImageView bell = new ImageView(this);
            bell.setImageResource(R.drawable.ic_notification_bell);
            bell.setPadding(dp(10), dp(10), dp(10), dp(10));
            bell.setBackground(round(SURFACE, 16));
            bell.setOnClickListener(v -> { tap(v); showNotificationInfo(); });
            top.addView(bell, new LinearLayout.LayoutParams(dp(44), dp(44)));
        }
        if (showCalendar) {
            TextView cal = iconButton("▦", false);
            cal.setOnClickListener(v -> { tap(v); selectedDate = key(Calendar.getInstance()); monthCursor = Calendar.getInstance(); render(); });
            top.addView(cal, new LinearLayout.LayoutParams(dp(44), dp(44)));
        }
        page.addView(top, margin(20, 10, 20, 0));
    }

    private View addPlanCard(Plan p, int index) {
        LinearLayout card = card();
        card.setAlpha(0f);
        card.setTranslationY(dp(10));
        card.animate().alpha(1f).translationY(0).setStartDelay(Math.min(index * 35, 180)).setDuration(220).start();

        LinearLayout line = row();
        View rail = new View(this);
        rail.setBackground(round(p.done ? Color.rgb(57, 66, 76) : ORANGE, 8));
        line.addView(rail, new LinearLayout.LayoutParams(dp(4), dp(64)));

        LinearLayout mid = column();
        mid.setPadding(dp(12), 0, dp(8), 0);
        String meta = planMeta(p);
        mid.addView(text(meta, 11, p.done ? MUTED : ORANGE, true));
        TextView title = text(p.title, 16, p.done ? MUTED : TEXT, true);
        title.setMaxLines(2);
        mid.addView(title);
        mid.addView(text(typeLabel(p.type), 11, MUTED, false));
        line.addView(mid, new LinearLayout.LayoutParams(0, -2, 1));

        TextView check = text(p.done ? "✓" : "○", 23, p.done ? ORANGE : MUTED, true);
        check.setGravity(Gravity.CENTER);
        if (p.done) check.setBackground(round(ORANGE_SOFT, 18));
        line.addView(check, new LinearLayout.LayoutParams(dp(48), dp(48)));
        card.addView(line);

        LinearLayout actions = row();
        actions.setPadding(dp(16), dp(8), dp(4), 0);
        TextView edit = actionText("Düzenle", TEXT);
        TextView del = actionText("Sil", RED);
        actions.addView(edit, new LinearLayout.LayoutParams(0, dp(38), 1));
        actions.addView(del, new LinearLayout.LayoutParams(0, dp(38), 1));
        actions.setVisibility(View.GONE);
        card.addView(actions);

        check.setOnClickListener(v -> {
            tap(v);
            p.done = !p.done;
            PlanStore.save(this, plans);
            if (p.done) cancelReminder(p);
            else scheduleReminder(p);
            render();
        });

        card.setOnClickListener(v -> {
            tap(v);
            if (actions.getVisibility() == View.GONE) {
                actions.setVisibility(View.VISIBLE);
                actions.setAlpha(0f);
                actions.animate().alpha(1f).setDuration(160).start();
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
        final Dialog dialog = styledDialog();
        LinearLayout box = dialogBox();

        LinearLayout header = row();
        LinearLayout htext = column();
        htext.addView(text(editing == null ? "Yeni plan" : "Planı düzenle", 24, TEXT, true));
        htext.addView(text(editing == null ? "Hedeflerini planla, adım adım ilerle." : "Planındaki bilgileri güncelle.",
                12, MUTED, false));
        header.addView(htext, new LinearLayout.LayoutParams(0, -2, 1));
        TextView close = iconButton("×", false);
        close.setOnClickListener(v -> { tap(v); dialog.dismiss(); });
        header.addView(close, new LinearLayout.LayoutParams(dp(44), dp(44)));
        box.addView(header);

        final String[] chosenDate = {initialDate};
        TextView titleField = field("Plan adı",
                editing == null ? "" : editing.title);
        box.addView(titleField, marginTop(14));
        titleField.setOnClickListener(v -> {
            tap(v);
            String currentTitle = "Plan adı ekle".equals(titleField.getText().toString()) ? "" : titleField.getText().toString();
            showInputDialog("Plan adı", currentTitle, value -> titleField.setText(value));
        });

        TextView dateField = field("Tarih", prettyDate(chosenDate[0]));
        box.addView(dateField, marginTop(8));
        dateField.setOnClickListener(v -> { tap(v); showDateChooser(chosenDate[0], picked -> {
            chosenDate[0] = picked;
            dateField.setText(prettyDate(picked));
        }); });

        box.addView(text("Plan türü", 11, MUTED, true), marginTop(14));
        LinearLayout chips = row();
        final LinearLayout[] timeRowHolder = {null};
        final TextView[] durationHintHolder = {null};
        TextView[] chipViews = new TextView[3];
        String[] selectedType = {editing == null ? Plan.HOURLY : editing.type};
        String[] typeNames = {Plan.HOURLY, Plan.FLEX, Plan.ALL_DAY};
        for (int i = 0; i < 3; i++) {
            String type = typeNames[i];
            TextView chip = text(type, 12, type.equals(selectedType[0]) ? Color.WHITE : TEXT, true);
            chip.setGravity(Gravity.CENTER);
            chip.setBackground(round(type.equals(selectedType[0]) ? ORANGE : SURFACE_3, 17));
            chip.setPadding(dp(6), 0, dp(6), 0);
            final String typeCopy = type;
            chip.setOnClickListener(v -> {
                tap(v);
                selectedType[0] = typeCopy;
                refreshChips(chipViews, typeCopy);
                if (timeRowHolder[0] != null) {
                    timeRowHolder[0].setVisibility(Plan.ALL_DAY.equals(typeCopy) ? View.GONE : View.VISIBLE);
                }
                if (durationHintHolder[0] != null) {
                    durationHintHolder[0].setVisibility(Plan.ALL_DAY.equals(typeCopy) ? View.GONE : View.VISIBLE);
                }
            });
            chipViews[i] = chip;
            chips.addView(chip, new LinearLayout.LayoutParams(0, dp(42), 1));
            if (i < 2) chips.addView(space(6), new LinearLayout.LayoutParams(dp(6), 1));
        }
        box.addView(chips, marginTop(8));

        TextView start = field("Başlangıç", editing == null ? "" : safe(editing.startTime, editing.time));
        TextView end = field("Bitiş", editing == null ? "" : editing.endTime);
        LinearLayout timeRow = row();
        timeRow.addView(start, new LinearLayout.LayoutParams(0, dp(58), 1));
        timeRow.addView(space(8), new LinearLayout.LayoutParams(dp(8), 1));
        timeRow.addView(end, new LinearLayout.LayoutParams(0, dp(58), 1));
        box.addView(timeRow, marginTop(8));
        start.setOnClickListener(v -> { tap(v); showTimeChooser(start, actualTime(start.getText().toString())); });
        end.setOnClickListener(v -> { tap(v); showTimeChooser(end, actualTime(end.getText().toString())); });
        TextView durationHint = text("Süre başlangıç ve bitiş saatlerinden otomatik hesaplanır.", 11, MUTED, false);
        box.addView(durationHint, marginTop(7));
        timeRowHolder[0] = timeRow;
        durationHintHolder[0] = durationHint;
        if (Plan.ALL_DAY.equals(selectedType[0])) {
            timeRow.setVisibility(View.GONE);
            durationHint.setVisibility(View.GONE);
        }

        TextView note = field("Not", "");
        box.addView(note, marginTop(12));
        note.setOnClickListener(v -> {
            tap(v);
            String currentNote = "Not ekle".equals(note.getText().toString()) ? "" : note.getText().toString();
            showInputDialog("Not", currentNote, value -> note.setText(value));
        });

        LinearLayout reminder = settingCard();
        LinearLayout rr = row();
        LinearLayout rt = column();
        rt.addView(text("Hatırlatma", 14, TEXT, true));
        rt.addView(text("Plan başlamadan 15 dk önce", 11, MUTED, false));
        rr.addView(rt, new LinearLayout.LayoutParams(0, -2, 1));
        TextView reminderState = toggle(remindersOn());
        rr.addView(reminderState, new LinearLayout.LayoutParams(dp(62), dp(34)));
        reminder.addView(rr);
        reminder.setOnClickListener(v -> {
            tap(v);
            boolean on = "Kapalı".equals(String.valueOf(reminderState.getTag()));
            setToggle(reminderState, on);
        });
        box.addView(reminder, marginTop(12));

        LinearLayout buttons = row();
        TextView cancel = buttonText("Vazgeç", MUTED);
        TextView save = buttonText(editing == null ? "Planı oluştur" : "Kaydet", Color.WHITE);
        save.setGravity(Gravity.CENTER);
        save.setBackground(round(ORANGE, 20));
        buttons.addView(cancel, new LinearLayout.LayoutParams(dp(100), dp(52)));
        buttons.addView(space(8), new LinearLayout.LayoutParams(dp(8), 1));
        buttons.addView(save, new LinearLayout.LayoutParams(0, dp(52), 1));
        box.addView(buttons, marginTop(14));

        cancel.setOnClickListener(v -> { tap(v); dialog.dismiss(); });
        save.setOnClickListener(v -> {
            tap(v);
            String t = titleField.getText().toString().trim();
            if ("Plan adı ekle".equals(t)) t = "";
            if (t.isEmpty()) {
                showInputDialog("Plan adı", "", value -> titleField.setText(value));
                return;
            }

            Plan p = editing == null ?
                    new Plan(System.currentTimeMillis(), chosenDate[0], t, selectedType[0], "", "", false) : editing;
            p.date = chosenDate[0];
            p.title = t;
            p.type = selectedType[0];
            p.startTime = actualTime(start.getText().toString());
            p.endTime = actualTime(end.getText().toString());

            if (Plan.ALL_DAY.equals(p.type)) {
                p.startTime = "";
                p.endTime = "";
                p.time = "";
                p.duration = "";
            } else {
                p.time = p.startTime == null ? "" : p.startTime;
                p.duration = Plan.durationBetween(p.startTime, p.endTime);
            }

            if (editing == null) plans.add(p);
            PlanStore.save(this, plans);

            if (reminderEnabled(reminderState)) scheduleReminder(p);
            else cancelReminder(p);

            selectedDate = p.date;
            monthCursor = parseKey(p.date);
            dialog.dismiss();
            render();
        });

        dialog.setContentView(box);
        dialog.show();
        fitDialog(dialog, 0.92f);
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

    private void showTimeChooser(TextView target, String existing) {
        final Dialog dialog = styledDialog();
        LinearLayout box = dialogBox();

        LinearLayout head = row();
        LinearLayout ht = column();
        ht.addView(text("Saat seç", 23, TEXT, true));
        ht.addView(text("Başlangıç veya bitiş saatini belirle.", 12, MUTED, false));
        head.addView(ht, new LinearLayout.LayoutParams(0, -2, 1));
        TextView close = iconButton("×", false);
        head.addView(close, new LinearLayout.LayoutParams(dp(44), dp(44)));
        box.addView(head);

        Calendar c = Calendar.getInstance();
        if (existing != null && existing.matches("\\d{2}:\\d{2}")) {
            c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(existing.substring(0,2)));
            c.set(Calendar.MINUTE, Integer.parseInt(existing.substring(3)));
        }
        final int[] hm = {c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)};

        TextView preview = text(String.format(Locale.GERMANY, "%02d:%02d", hm[0], hm[1]), 34, TEXT, true);
        preview.setGravity(Gravity.CENTER);
        preview.setBackground(round(SURFACE_2, 20));
        box.addView(preview, new LinearLayout.LayoutParams(-1, dp(72)));

        LinearLayout wheel = row();
        wheel.setGravity(Gravity.CENTER);
        wheel.addView(timeColumn("Saat", hm, 0, preview), new LinearLayout.LayoutParams(0, dp(220), 1));
        wheel.addView(text(":", 34, TEXT, true), new LinearLayout.LayoutParams(dp(34), dp(80)));
        wheel.addView(timeColumn("Dakika", hm, 1, preview), new LinearLayout.LayoutParams(0, dp(220), 1));
        box.addView(wheel, marginTop(14));

        LinearLayout buttons = row();
        TextView cancel = buttonText("Vazgeç", MUTED);
        TextView save = buttonText("Saati seç", Color.WHITE);
        save.setGravity(Gravity.CENTER);
        save.setBackground(round(ORANGE, 20));
        buttons.addView(cancel, new LinearLayout.LayoutParams(dp(100), dp(52)));
        buttons.addView(space(8), new LinearLayout.LayoutParams(dp(8), 1));
        buttons.addView(save, new LinearLayout.LayoutParams(0, dp(52), 1));
        box.addView(buttons, marginTop(14));

        close.setOnClickListener(v -> { tap(v); dialog.dismiss(); });
        cancel.setOnClickListener(v -> { tap(v); dialog.dismiss(); });
        save.setOnClickListener(v -> {
            tap(v);
            target.setText(String.format(Locale.GERMANY, "%02d:%02d", hm[0], hm[1]));
            dialog.dismiss();
        });

        dialog.setContentView(box);
        dialog.show();
        fitDialog(dialog, 0.88f);
    }

    private LinearLayout timeColumn(String label, int[] hm, int index, TextView preview) {
        LinearLayout c = column();
        c.setGravity(Gravity.CENTER);
        c.setPadding(dp(4), 0, dp(4), 0);
        c.addView(text(label, 11, MUTED, true));

        LinearLayout wheel = column();
        wheel.setGravity(Gravity.CENTER);
        wheel.setBackground(round(SURFACE_2, 22));

        TextView before = text("", 17, MUTED, false);
        before.setGravity(Gravity.CENTER);
        TextView value = text(String.format(Locale.GERMANY, "%02d", hm[index]), 34, TEXT, true);
        value.setGravity(Gravity.CENTER);
        TextView after = text("", 17, MUTED, false);
        after.setGravity(Gravity.CENTER);

        wheel.addView(before, new LinearLayout.LayoutParams(-1, dp(34)));
        wheel.addView(value, new LinearLayout.LayoutParams(-1, dp(58)));
        wheel.addView(after, new LinearLayout.LayoutParams(-1, dp(34)));

        Runnable refresh = () -> {
            int max = index == 0 ? 23 : 59;
            int current = hm[index];
            before.setText(String.format(Locale.GERMANY, "%02d", (current - 1 + max + 1) % (max + 1)));
            value.setText(String.format(Locale.GERMANY, "%02d", current));
            after.setText(String.format(Locale.GERMANY, "%02d", (current + 1) % (max + 1)));
            preview.setText(String.format(Locale.GERMANY, "%02d:%02d", hm[0], hm[1]));
        };
        refresh.run();

        final float[] downY = {0f};
        final boolean[] moved = {false};
        wheel.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downY[0] = event.getY();
                    moved[0] = false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dy = event.getY() - downY[0];
                    if (Math.abs(dy) >= dp(24)) {
                        int max = index == 0 ? 23 : 59;
                        hm[index] = dy < 0 ? (hm[index] + 1) % (max + 1) : (hm[index] - 1 + max + 1) % (max + 1);
                        downY[0] = event.getY();
                        moved[0] = true;
                        refresh.run();
                        tickSound();
                        wheel.animate().scaleX(0.985f).scaleY(0.985f).setDuration(45)
                                .withEndAction(() -> wheel.animate().scaleX(1f).scaleY(1f).setDuration(70).start()).start();
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    if (!moved[0]) {
                        float center = wheel.getHeight() / 2f;
                        int limit = index == 0 ? 24 : 60;
                        if (event.getY() < center) hm[index] = (hm[index] - 1 + limit) % limit;
                        else hm[index] = (hm[index] + 1) % limit;
                        refresh.run();
                        tickSound();
                    }
                    return true;
            }
            return true;
        });

        c.addView(wheel, new LinearLayout.LayoutParams(-1, dp(126)));
        TextView hint = text("Yukarı / aşağı kaydır", 10, MUTED, false);
        hint.setGravity(Gravity.CENTER);
        c.addView(hint, new LinearLayout.LayoutParams(-1, dp(26)));
        return c;
    }

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
        box.addView(text(remindersOn() ? "Hatırlatmalar açık. Saatli planlar 15 dakika önce bildirim oluşturur." :
                "Hatırlatmalar kapalı.", 13, MUTED, false), marginTop(6));
        TextView close = buttonText("Tamam", Color.WHITE);
        close.setGravity(Gravity.CENTER);
        close.setBackground(round(ORANGE, 18));
        box.addView(close, marginTop(14));
        close.setOnClickListener(v -> { tap(v); d.dismiss(); });
        d.setContentView(box);
        d.show();
        fitDialog(d, 0.86f);
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
        try {
            ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_SYSTEM, 35);
            tg.startTone(ToneGenerator.TONE_PROP_BEEP, 22);
            new android.os.Handler().postDelayed(tg::release, 80);
        } catch (Exception ignored) {}
    }

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
        box.setPadding(dp(20), dp(18), dp(20), dp(18));
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
        alarm.add(Calendar.MINUTE, -15);
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

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
