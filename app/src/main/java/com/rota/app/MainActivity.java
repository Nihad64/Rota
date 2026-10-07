package com.rota.app;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
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
    private static final int BG = Color.rgb(9, 11, 15);
    private static final int SURFACE = Color.rgb(18, 22, 29);
    private static final int SURFACE2 = Color.rgb(25, 30, 38);
    private static final int SURFACE3 = Color.rgb(31, 37, 47);
    private static final int BORDER = Color.rgb(39, 46, 57);
    private static final int TEXT = Color.rgb(247, 248, 250);
    private static final int MUTED = Color.rgb(145, 153, 165);
    private static final int ORANGE = Color.rgb(255, 139, 0);
    private static final int GREEN = Color.rgb(62, 190, 125);
    private static final int RED = Color.rgb(225, 82, 89);
    private static final Locale TR = new Locale("tr", "TR");

    private final List<Plan> plans = new ArrayList<>();
    private LinearLayout root;
    private FrameLayout page;
    private LinearLayout content;
    private LinearLayout navBar;
    private TextView aiFab;
    private int tab = 0;
    private int calendarMode = 1; // 0 day, 1 week, 2 month
    private String selectedDate;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        selectedDate = key(Calendar.getInstance());
        plans.addAll(PlanStore.load(this));
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(Color.BLACK);
        getWindow().getDecorView().setSystemUiVisibility(0);
        buildShell();
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!plans.isEmpty()) ReminderScheduler.rescheduleAll(this, plans);
        new android.os.Handler(getMainLooper()).postDelayed(
                () -> MissedPlanManager.check(this, plans, this::render, this::reopenMissed), 220);
    }

    private void buildShell() {
        root = column();
        root.setBackgroundColor(BG);
        root.setPadding(0, statusBar(), 0, 0);

        page = new FrameLayout(this);
        content = column();
        page.addView(content, new FrameLayout.LayoutParams(-1, -1));

        aiFab = text("✦", 27, Color.WHITE, true);
        aiFab.setGravity(Gravity.CENTER);
        aiFab.setBackground(round(ORANGE, 30));
        aiFab.setElevation(dp(14));
        aiFab.setContentDescription("Rotam AI");
        FrameLayout.LayoutParams fp = new FrameLayout.LayoutParams(dp(60), dp(60),
                Gravity.BOTTOM | Gravity.END);
        fp.setMargins(0, 0, dp(18), dp(20));
        page.addView(aiFab, fp);
        aiFab.setOnClickListener(v -> {
            press(v);
            openAi();
        });

        root.addView(page, new LinearLayout.LayoutParams(-1, 0, 1));
        navBar = nav();
        root.addView(navBar, new LinearLayout.LayoutParams(-1, dp(80)));
        setContentView(root);
    }

    private LinearLayout nav() {
        LinearLayout bar = column();
        bar.setPadding(dp(12), dp(8), dp(12), dp(8));
        bar.setBackgroundColor(BG);

        String[] labels = {"Bugün", "Planlar", "Ayarlar"};
        int[] icons = {R.drawable.ic_nav_home, R.drawable.ic_nav_calendar, R.drawable.ic_nav_settings};
        for (int i = 0; i < 3; i++) {
            final int n = i;
            LinearLayout item = column();
            item.setGravity(Gravity.CENTER);
            ImageView im = new ImageView(this);
            im.setImageResource(icons[i]);
            im.setColorFilter(i == tab ? ORANGE : MUTED);
            item.addView(im, new LinearLayout.LayoutParams(-1, dp(28)));
            TextView t = text(labels[i], 11, i == tab ? TEXT : MUTED, true);
            t.setGravity(Gravity.CENTER);
            item.addView(t, new LinearLayout.LayoutParams(-1, dp(24)));
            if (i == tab) item.setBackground(round(SURFACE, 20));
            item.setOnClickListener(v -> {
                press(v);
                tab = n;
                if (tab == 0) selectedDate = key(Calendar.getInstance());
                aiFab.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);
                render();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(62), 1);
            lp.setMargins(dp(3), 0, dp(3), 0);
            bar.addView(item, lp);
        }
        return bar;
    }

    private void render() {
        content.removeAllViews();
        if (tab == 0) home();
        else if (tab == 1) plansPage();
        else settingsPage();

        navBar.removeAllViews();
        LinearLayout fresh = nav();
        for (int i = 0; i < fresh.getChildCount(); i++) {
            View child = fresh.getChildAt(i);
            fresh.removeView(child);
            navBar.addView(child);
            i--;
        }
        aiFab.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);
        content.setAlpha(0f);
        content.animate().alpha(1f).setDuration(180).start();
    }

    private void home() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        LinearLayout pageContent = column();
        pageContent.setPadding(0, dp(6), 0, dp(118));
        scroll.addView(pageContent);
        content.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        Calendar now = Calendar.getInstance();
        selectedDate = key(now);
        List<Plan> today = forDate(selectedDate);
        int done = 0;
        Plan next = null;
        for (Plan p : today) {
            if (p.done || Plan.STATUS_DONE.equals(p.status)) done++;
            else if (next == null) next = p;
        }

        // Header
        LinearLayout head = row();
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.rotam_logo);
        head.addView(logo, new LinearLayout.LayoutParams(dp(38), dp(38)));
        LinearLayout ht = column();
        ht.addView(text("Rotam", 16, TEXT, true));
        ht.addView(text(cap(new SimpleDateFormat("EEEE · d MMMM", TR).format(now.getTime())),
                12, MUTED, false), top(3));
        head.addView(ht, weight(1, -2));
        TextView day = text(new SimpleDateFormat("d", TR).format(now.getTime()), 18, ORANGE, true);
        day.setGravity(Gravity.CENTER);
        day.setBackground(round(SURFACE2, 18));
        head.addView(day, new LinearLayout.LayoutParams(dp(48), dp(48)));
        pageContent.addView(head, margins(18, 16, 18, 16));

        // Main summary
        LinearLayout summary = card();
        LinearLayout sr = row();
        LinearLayout st = column();
        st.addView(text("Bugünün özeti", 11, ORANGE, true));
        String title = today.isEmpty() ? "Bugününü planla." :
                (next == null ? "Bugünün planları tamam." : next.title);
        st.addView(text(title, 23, TEXT, true), top(7));
        String sub = today.isEmpty()
                ? "Birkaç dokunuşla gününü düzenleyebilirsin."
                : done + " / " + today.size() + " plan tamamlandı";
        st.addView(text(sub, 12, MUTED, false), top(5));
        sr.addView(st, weight(1, -2));

        int percent = today.isEmpty() ? 0 : (done * 100 / today.size());
        TextView pct = text(percent + "%", 21, ORANGE, true);
        pct.setGravity(Gravity.CENTER);
        pct.setBackground(round(SURFACE3, 22));
        sr.addView(pct, new LinearLayout.LayoutParams(dp(70), dp(70)));
        summary.addView(sr);

        FrameLayout track = new FrameLayout(this);
        track.setBackground(round(BORDER, 5));
        View fill = new View(this);
        fill.setBackground(round(ORANGE, 5));
        track.addView(fill, new FrameLayout.LayoutParams(0, dp(7), Gravity.START));
        track.post(() -> {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) fill.getLayoutParams();
            lp.width = percent == 0 ? 0 : Math.max(dp(7), (int) (track.getWidth() * (percent / 100f)));
            fill.setLayoutParams(lp);
        });
        summary.addView(track, top(14));
        pageContent.addView(summary, margins(18, 0, 18, 12));

        // Quick actions
        LinearLayout quick = row();
        quick.addView(actionCard("＋", "Plan oluştur", "Manuel olarak ekle.", v -> {
            press(v); openWizard();
        }), weight(1, dp(86)));
        gap(quick, 8);
        quick.addView(actionCard("✦", "Gemini", "Gününü bana bırak.", v -> {
            press(v); openAi();
        }), weight(1, dp(86)));
        pageContent.addView(quick, margins(18, 0, 18, 20));

        // Missed warning
        int missed = unresolvedCount();
        if (missed > 0) {
            LinearLayout alert = card();
            alert.setBackground(round(Color.rgb(37, 25, 18), 20));
            LinearLayout ar = row();
            TextView icon = text("!", 19, ORANGE, true);
            icon.setGravity(Gravity.CENTER);
            icon.setBackground(round(Color.rgb(57, 38, 19), 18));
            ar.addView(icon, new LinearLayout.LayoutParams(dp(42), dp(42)));
            LinearLayout at = column();
            at.addView(text(missed + " plan yanıt bekliyor", 15, TEXT, true));
            at.addView(text("Kaçırılan planları şimdi gözden geçir.", 11, MUTED, false), top(3));
            ar.addView(at, weight(1, -2));
            TextView go = text("›", 27, ORANGE, true);
            go.setGravity(Gravity.CENTER);
            ar.addView(go, new LinearLayout.LayoutParams(dp(34), dp(44)));
            alert.addView(ar);
            alert.setOnClickListener(v -> showNextMissed());
            pageContent.addView(alert, margins(18, 0, 18, 18));
        }

        // Agenda
        LinearLayout titleRow = row();
        titleRow.addView(text("Bugünün akışı", 20, TEXT, true), weight(1, -2));
        titleRow.addView(text(today.size() + " plan", 11, MUTED, true));
        pageContent.addView(titleRow, margins(18, 0, 18, 10));

        if (today.isEmpty()) {
            LinearLayout empty = card();
            LinearLayout er = row();
            TextView eicon = text("○", 22, ORANGE, true);
            eicon.setGravity(Gravity.CENTER);
            eicon.setBackground(round(SURFACE3, 17));
            er.addView(eicon, new LinearLayout.LayoutParams(dp(46), dp(46)));
            LinearLayout et = column();
            et.addView(text("Bugün için plan yok", 16, TEXT, true));
            et.addView(text("Tek bir plan bile günün akışını netleştirir.", 11, MUTED, false), top(4));
            er.addView(et, weight(1, -2));
            empty.addView(er);
            TextView add = button("İlk planı ekle", Color.WHITE, ORANGE);
            empty.addView(add, top(14));
            add.setOnClickListener(v -> { press(v); openWizard(); });
            pageContent.addView(empty, margins(18, 0, 18, 12));
        } else {
            for (Plan p : today) {
                pageContent.addView(planCard(p), margins(18, 0, 18, 9));
            }
        }
    }

    private LinearLayout actionCard(String icon, String title, String sub, View.OnClickListener l) {
        LinearLayout c = card();
        c.setPadding(dp(14), dp(13), dp(14), dp(13));
        TextView i = text(icon, 21, ORANGE, true);
        i.setGravity(Gravity.CENTER);
        i.setBackground(round(SURFACE3, 15));
        c.addView(i, new LinearLayout.LayoutParams(dp(38), dp(38)));
        c.addView(text(title, 13, TEXT, true), top(9));
        c.addView(text(sub, 10, MUTED, false), top(3));
        c.setOnClickListener(l);
        return c;
    }

    private LinearLayout planCard(Plan p) {
        LinearLayout c = card();
        c.setPadding(dp(14), dp(13), dp(12), dp(13));
        LinearLayout r = row();

        LinearLayout time = column();
        time.setGravity(Gravity.CENTER);
        String tm = Plan.ALL_DAY.equals(p.type) ? "GÜN" : safe(p.startTime);
        time.addView(text(tm, 12, p.done ? MUTED : ORANGE, true));
        if (p.endTime != null && !p.endTime.isEmpty()) {
            time.addView(text(p.endTime, 10, MUTED, false), top(3));
        }
        r.addView(time, new LinearLayout.LayoutParams(dp(58), dp(54)));

        View divider = new View(this);
        divider.setBackgroundColor(p.done ? BORDER : ORANGE);
        LinearLayout.LayoutParams dpv = new LinearLayout.LayoutParams(dp(2), dp(42));
        dpv.setMargins(dp(4), 0, dp(13), 0);
        r.addView(divider, dpv);

        LinearLayout tx = column();
        String shown = p.title.isEmpty() ? "Adsız plan" : p.title;
        TextView tt = text(shown, 16, p.done ? MUTED : TEXT, true);
        tx.addView(tt);
        LinearLayout meta = row();
        String cat = categoryLabel(p.category);
        TextView chip = text(cat, 10, p.done ? MUTED : ORANGE, true);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(8), 0, dp(8), 0);
        chip.setBackground(round(SURFACE3, 12));
        meta.addView(chip, new LinearLayout.LayoutParams(-2, dp(24)));
        String dur = safe(p.duration);
        if (!dur.isEmpty()) meta.addView(text("· " + dur, 10, MUTED, false), left(7));
        if (Plan.REPEAT_NONE.equals(p.repeat) == false)
            meta.addView(text("· ↻ " + repeatLabel(p.repeat), 10, MUTED, false), left(7));
        tx.addView(meta, top(7));

        String note = safe(p.note);
        if (!note.isEmpty()) tx.addView(text(note, 10, MUTED, false), top(5));

        r.addView(tx, weight(1, -2));

        TextView check = button(p.done ? "✓" : "○", p.done ? Color.WHITE : TEXT,
                p.done ? GREEN : SURFACE3);
        check.setTextSize(18);
        r.addView(check, new LinearLayout.LayoutParams(dp(44), dp(44)));
        check.setOnClickListener(v -> { press(v); toggleDone(p); });
        c.addView(r);
        c.setOnClickListener(v -> showPlanActions(p));
        return c;
    }

    private void plansPage() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        LinearLayout p = column();
        p.setPadding(0, dp(6), 0, dp(118));
        scroll.addView(p);
        content.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        p.addView(text("Planlar", 30, TEXT, true), margins(18, 16, 18, 4));
        p.addView(text("Gün, hafta ve ay görünümünden planlarını yönet.", 12, MUTED, false),
                margins(18, 0, 18, 15));

        LinearLayout modes = row();
        String[] labels = {"Gün", "Hafta", "Ay"};
        for (int i = 0; i < 3; i++) {
            final int n = i;
            TextView b = button(labels[i], i == calendarMode ? Color.WHITE : MUTED,
                    i == calendarMode ? ORANGE : SURFACE2);
            modes.addView(b, weight(1, dp(44)));
            if (i < 2) gap(modes, 7);
            b.setOnClickListener(v -> { press(v); calendarMode = n; plansPage(); });
        }
        p.addView(modes, margins(18, 0, 18, 16));

        if (calendarMode == 0) dayView(p);
        else if (calendarMode == 1) weekView(p);
        else monthView(p);
    }

    private void dayView(LinearLayout p) {
        Calendar d = parse(selectedDate);
        addDateNavigator(p, "EEEE · d MMMM yyyy", d);
        addAgenda(p, selectedDate);
    }

    private void weekView(LinearLayout p) {
        Calendar start = parse(selectedDate);
        while (start.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY)
            start.add(Calendar.DAY_OF_MONTH, -1);

        LinearLayout days = row();
        for (int i = 0; i < 7; i++) {
            Calendar d = (Calendar) start.clone();
            d.add(Calendar.DAY_OF_MONTH, i);
            String k = key(d);
            LinearLayout cell = column();
            cell.setGravity(Gravity.CENTER);
            cell.setPadding(dp(3), dp(7), dp(3), dp(7));
            if (k.equals(selectedDate)) cell.setBackground(round(ORANGE, 17));
            TextView dow = text(dayShort(d), 9, k.equals(selectedDate) ? Color.WHITE : MUTED, true);
            dow.setGravity(Gravity.CENTER);
            TextView num = text(String.valueOf(d.get(Calendar.DAY_OF_MONTH)), 17,
                    k.equals(selectedDate) ? Color.WHITE : TEXT, true);
            num.setGravity(Gravity.CENTER);
            cell.addView(dow);
            cell.addView(num, top(4));
            int count = forDate(k).size();
            if (count > 0) cell.addView(text("•", 16, k.equals(selectedDate) ? Color.WHITE : ORANGE, true),
                    top(1));
            cell.setOnClickListener(v -> { press(v); selectedDate = k; plansPage(); });
            days.addView(cell, weight(1, 80));
            if (i < 6) gap(days, 3);
        }
        p.addView(days, margins(18, 0, 18, 14));
        addAgenda(p, selectedDate);
    }

    private void monthView(LinearLayout p) {
        Calendar shown = parse(selectedDate);
        shown.set(Calendar.DAY_OF_MONTH, 1);
        LinearLayout head = row();
        TextView prev = button("‹", TEXT, SURFACE2);
        TextView title = text(cap(new SimpleDateFormat("LLLL yyyy", TR).format(shown.getTime())), 18, TEXT, true);
        title.setGravity(Gravity.CENTER);
        head.addView(prev, new LinearLayout.LayoutParams(dp(44), dp(44)));
        head.addView(title, weight(1, -2));
        TextView next = button("›", TEXT, SURFACE2);
        head.addView(next, new LinearLayout.LayoutParams(dp(44), dp(44)));
        p.addView(head, margins(18, 0, 18, 10));

        prev.setOnClickListener(v -> {
            shown.add(Calendar.MONTH, -1);
            selectedDate = key(shown);
            plansPage();
        });
        next.setOnClickListener(v -> {
            shown.add(Calendar.MONTH, 1);
            selectedDate = key(shown);
            plansPage();
        });

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(7);
        String[] names = {"Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz"};
        for (String n : names) {
            TextView h = text(n, 10, MUTED, true);
            h.setGravity(Gravity.CENTER);
            GridLayout.LayoutParams hp = new GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED, 1f),
                    GridLayout.spec(GridLayout.UNDEFINED, 1f));
            hp.width = 0;
            hp.height = dp(28);
            grid.addView(h, hp);
        }

        Calendar first = (Calendar) shown.clone();
        int dow = first.get(Calendar.DAY_OF_WEEK);
        int mondayOffset = (dow + 5) % 7;
        first.add(Calendar.DAY_OF_MONTH, -mondayOffset);

        for (int i = 0; i < 42; i++) {
            Calendar d = (Calendar) first.clone();
            d.add(Calendar.DAY_OF_MONTH, i);
            String k = key(d);
            boolean currentMonth = d.get(Calendar.MONTH) == shown.get(Calendar.MONTH);
            boolean selected = k.equals(selectedDate);
            LinearLayout cell = column();
            cell.setGravity(Gravity.CENTER);
            cell.setPadding(0, dp(6), 0, dp(4));
            if (selected) cell.setBackground(round(ORANGE, 14));
            TextView n = text(String.valueOf(d.get(Calendar.DAY_OF_MONTH)), 13,
                    selected ? Color.WHITE : (currentMonth ? TEXT : Color.rgb(74, 81, 92)), true);
            n.setGravity(Gravity.CENTER);
            cell.addView(n);
            int count = forDate(k).size();
            if (count > 0) cell.addView(text("•", 15, selected ? Color.WHITE : ORANGE, true),
                    top(1));
            GridLayout.LayoutParams cp = new GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED, 1f),
                    GridLayout.spec(GridLayout.UNDEFINED, 1f));
            cp.width = 0;
            cp.height = dp(52);
            cp.setMargins(dp(2), dp(2), dp(2), dp(2));
            grid.addView(cell, cp);
            cell.setOnClickListener(v -> { press(v); selectedDate = k; plansPage(); });
        }
        p.addView(grid, margins(18, 0, 18, 14));
        addAgenda(p, selectedDate);
    }

    private void addDateNavigator(LinearLayout p, String format, Calendar d) {
        LinearLayout row = row();
        TextView prev = button("‹", TEXT, SURFACE2);
        TextView title = text(cap(new SimpleDateFormat(format, TR).format(d.getTime())), 16, TEXT, true);
        title.setGravity(Gravity.CENTER);
        TextView next = button("›", TEXT, SURFACE2);
        row.addView(prev, new LinearLayout.LayoutParams(dp(44), dp(44)));
        row.addView(title, weight(1, -2));
        row.addView(next, new LinearLayout.LayoutParams(dp(44), dp(44)));
        p.addView(row, margins(18, 0, 18, 12));
        prev.setOnClickListener(v -> {
            Calendar x = parse(selectedDate);
            x.add(Calendar.DAY_OF_MONTH, -1);
            selectedDate = key(x);
            plansPage();
        });
        next.setOnClickListener(v -> {
            Calendar x = parse(selectedDate);
            x.add(Calendar.DAY_OF_MONTH, 1);
            selectedDate = key(x);
            plansPage();
        });
    }

    private void addAgenda(LinearLayout p, String date) {
        List<Plan> list = forDate(date);
        p.addView(text("Günün akışı", 18, TEXT, true), margins(18, 0, 18, 9));
        if (list.isEmpty()) {
            p.addView(cardText("Bu gün için plan yok."), margins(18, 0, 18, 10));
        } else {
            for (Plan q : list) p.addView(planCard(q), margins(18, 0, 18, 9));
        }
        TextView add = button("＋  Bu güne plan ekle", Color.WHITE, ORANGE);
        add.setOnClickListener(v -> { press(v); openWizard(); });
        p.addView(add, margins(18, 5, 18, 20));
    }

    // Small helper to keep addAgenda compact without Kotlin-style constructs.
    private LinearLayout alsoNoop(LinearLayout v) { return v; }

    private void settingsPage() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        LinearLayout p = column();
        p.setPadding(0, dp(6), 0, dp(118));
        scroll.addView(p);
        content.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        p.addView(text("Ayarlar", 30, TEXT, true), margins(18, 16, 18, 4));
        p.addView(text("Rotam'ı kendi çalışma şekline göre düzenle.", 12, MUTED, false),
                margins(18, 0, 18, 18));

        p.addView(sectionTitle("Bildirimler"), margins(18, 0, 18, 8));
        p.addView(settingRow("Bildirimler", "Planlarını zamanında hatırlat.", notificationEnabled()
                ? "Açık" : "Kapalı", notificationEnabled() ? ORANGE : SURFACE2, v -> {
            boolean next = !notificationEnabled();
            getSharedPreferences("rotam_settings", 0).edit().putBoolean("reminders", next).apply();
            if (next) requestNotificationPermission();
            ReminderScheduler.rescheduleAll(this, plans);
            render();
        }), margins(18, 0, 18, 8));

        p.addView(settingRow("Hatırlatma", "Plan saatinden ne kadar önce?", reminder() + " dk önce",
                SURFACE2, v -> chooseReminder()), margins(18, 0, 18, 8));

        boolean exact = ReminderScheduler.exactAlarmAllowed(this);
        p.addView(settingRow("Tam zamanında alarm", exact
                        ? "Kesin alarm izni açık." : "Android ayarından kesin alarm izni gerekli.",
                exact ? "Hazır" : "Ayarla", exact ? GREEN : ORANGE,
                v -> openExactAlarmSettings()), margins(18, 0, 18, 8));

        p.addView(settingRow("Bildirim testi", "Hemen örnek bir Rotam bildirimi gönder.",
                "Dene", SURFACE2, v -> testNotification()), margins(18, 0, 18, 14));

        p.addView(sectionTitle("Gemini AI"), margins(18, 4, 18, 8));
        String key = geminiKey();
        p.addView(settingRow("Google Gemini", key.isEmpty()
                        ? "API anahtarı henüz bağlanmadı." : "Gerçek Gemini bağlantısı hazır.",
                key.isEmpty() ? "Bağla" : "Düzenle", ORANGE, v -> geminiSetup()),
                margins(18, 0, 18, 8));
        p.addView(cardText("Rotam AI, planlarını okuyabilir, yeni planlar oluşturabilir ve açıkça istediğinde mevcut planlarını değiştirebilir."),
                margins(18, 0, 18, 14));

        p.addView(sectionTitle("Görünüm"), margins(18, 4, 18, 8));
        p.addView(settingRow("Tema", "Şimdilik odak modu: koyu.", "Gece", SURFACE2, v -> {}), margins(18, 0, 18, 8));

        p.addView(sectionTitle("Rotam"), margins(18, 4, 18, 8));
        p.addView(cardText("Rotam 2.2.0\nKişisel planlama + Gemini AI\nVeriler bu cihazda saklanır."),
                margins(18, 0, 18, 28));
    }

    private TextView sectionTitle(String s) {
        return text(s.toUpperCase(TR), 10, MUTED, true);
    }

    private LinearLayout settingRow(String title, String sub, String value, int valueBg,
                                    View.OnClickListener l) {
        LinearLayout c = card();
        c.setPadding(dp(15), dp(13), dp(12), dp(13));
        LinearLayout r = row();
        LinearLayout tx = column();
        tx.addView(text(title, 14, TEXT, true));
        tx.addView(text(sub, 11, MUTED, false), top(4));
        r.addView(tx, weight(1, -2));
        TextView val = text(value, 10, valueBg == ORANGE ? ORANGE : (valueBg == GREEN ? GREEN : TEXT), true);
        val.setGravity(Gravity.CENTER);
        val.setPadding(dp(10), 0, dp(10), 0);
        val.setBackground(round(valueBg, 13));
        r.addView(val, new LinearLayout.LayoutParams(-2, dp(30)));
        c.addView(r);
        c.setOnClickListener(l);
        return c;
    }

    private void showPlanActions(Plan p) {
        Dialog d = dialog();
        LinearLayout b = dialogBox();
        b.addView(text(p.title, 21, TEXT, true));
        b.addView(text(categoryLabel(p.category) + " · " + safe(p.startTime),
                11, MUTED, false), top(5));

        TextView mark = button(p.done ? "Tamamlanmadı olarak işaretle" : "✓  Tamamlandı",
                Color.WHITE, p.done ? SURFACE3 : GREEN);
        b.addView(mark, top(14));
        TextView edit = button("Planı düzenle", Color.WHITE, ORANGE);
        b.addView(edit, top(7));
        TextView postpone = button("Yeniden planla", TEXT, SURFACE2);
        b.addView(postpone, top(7));
        TextView del = button("Planı sil", Color.WHITE, RED);
        b.addView(del, top(7));
        TextView close = button("Kapat", MUTED, SURFACE2);
        b.addView(close, top(7));

        mark.setOnClickListener(v -> {
            p.done = !p.done;
            p.status = p.done ? Plan.STATUS_DONE : Plan.STATUS_PLANNED;
            save();
            ReminderScheduler.rescheduleAll(this, plans);
            d.dismiss();
            render();
        });
        edit.setOnClickListener(v -> {
            d.dismiss();
            PlanWizard.showEdit(this, plans, p, () -> { save(); render(); });
        });
        postpone.setOnClickListener(v -> {
            d.dismiss();
            p.status = Plan.STATUS_DEFERRED;
            PlanWizard.showEdit(this, plans, p, () -> {
                p.status = Plan.STATUS_PLANNED;
                p.done = false;
                save();
                render();
            });
        });
        del.setOnClickListener(v -> {
            plans.remove(p);
            ReminderScheduler.cancel(this, p);
            save();
            d.dismiss();
            render();
        });
        close.setOnClickListener(v -> d.dismiss());

        d.setContentView(b);
        d.show();
        fit(d, .92f);
    }

    private void reopenMissed(Plan p) {
        PlanWizard.showEdit(this, plans, p, () -> {
            p.done = false;
            p.status = Plan.STATUS_PLANNED;
            save();
            ReminderScheduler.rescheduleAll(this, plans);
            render();
        });
    }

    private void showNextMissed() {
        Plan target = null;
        long best = Long.MIN_VALUE;
        long now = System.currentTimeMillis();
        for (Plan p : plans) {
            if (p.done || Plan.STATUS_DONE.equals(p.status) || Plan.STATUS_CANCELLED.equals(p.status)) continue;
            long end = MissedPlanManager.endMillisForExternal(p);
            if (end > 0 && end < now && end > best) {
                best = end;
                target = p;
            }
        }
        if (target != null) MissedPlanManager.showExternal(this, plans, target, this::render);
    }

    private void toggleDone(Plan p) {
        p.done = !p.done;
        p.status = p.done ? Plan.STATUS_DONE : Plan.STATUS_PLANNED;
        if (p.done) ReminderScheduler.cancel(this, p);
        else schedule(p);
        save();
        render();
    }

    private void openWizard() {
        PlanWizard.show(this, plans, selectedDate, () -> {
            save();
            ReminderScheduler.rescheduleAll(this, plans);
            render();
        });
    }

    private void openAi() {
        InlineAiPanel.show(this, plans, () -> {
            save();
            ReminderScheduler.rescheduleAll(this, plans);
            render();
        });
    }

    private void chooseReminder() {
        Dialog d = dialog();
        LinearLayout b = dialogBox();
        b.addView(text("Hatırlatma zamanı", 21, TEXT, true));
        b.addView(text("Bu ayar bütün planlar için geçerlidir.", 11, MUTED, false), top(5));
        int[] vals = {5, 10, 15, 30, 60};
        for (int n : vals) {
            TextView x = button(n + " dakika önce",
                    n == reminder() ? Color.WHITE : TEXT,
                    n == reminder() ? ORANGE : SURFACE2);
            b.addView(x, top(7));
            x.setOnClickListener(v -> {
                getSharedPreferences("rotam_settings", 0).edit().putInt("reminder_minutes", n).apply();
                ReminderScheduler.rescheduleAll(this, plans);
                d.dismiss();
                render();
            });
        }
        d.setContentView(b);
        d.show();
        fit(d, .88f);
    }

    private void geminiSetup() {
        Dialog d = dialog();
        LinearLayout b = dialogBox();
        b.addView(text("Google Gemini", 21, TEXT, true));
        b.addView(text("Rotam AI gerçek Gemini API bağlantısını kullanır. Anahtar bu cihazdaki uygulama ayarlarında saklanır.",
                11, MUTED, false), top(5));
        EditText key = input("AIza…");
        key.setInputType(0x00000081);
        if (!geminiKey().isEmpty()) key.setHint("Mevcut anahtar kayıtlı — yenisini gir");
        b.addView(key, top(12));
        TextView save = button("Anahtarı kaydet", Color.WHITE, ORANGE);
        b.addView(save, top(10));
        TextView clear = button("Anahtarı sil", TEXT, SURFACE2);
        b.addView(clear, top(7));
        TextView close = button("Kapat", MUTED, SURFACE2);
        b.addView(close, top(7));

        save.setOnClickListener(v -> {
            String s = key.getText().toString().trim();
            if (s.length() < 20) {
                key.setError("Geçerli Gemini API anahtarını gir.");
                return;
            }
            getSharedPreferences("rotam_settings", 0).edit().putString("gemini_api_key", s).apply();
            d.dismiss();
            render();
        });
        clear.setOnClickListener(v -> {
            getSharedPreferences("rotam_settings", 0).edit().remove("gemini_api_key").apply();
            d.dismiss();
            render();
        });
        close.setOnClickListener(v -> d.dismiss());
        d.setContentView(b);
        d.show();
        fit(d, .92f);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 301);
        }
    }

    private void testNotification() {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestNotificationPermission();
            toast("Önce bildirim iznini ver.");
            return;
        }
        Intent i = new Intent(this, ReminderReceiver.class);
        i.putExtra("title", "Rotam bildirim testi");
        i.putExtra("minutesBefore", 0);
        new ReminderReceiver().onReceive(this, i);
        toast("Test bildirimi gönderildi.");
    }

    private void openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT < 31) {
            toast("Bu cihazda ayrıca kesin alarm izni gerekmiyor.");
            return;
        }
        try {
            Intent i = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:" + getPackageName()));
            startActivity(i);
        } catch (Exception e) {
            try { startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS)); }
            catch (Exception ignored) {}
        }
    }

    private boolean notificationEnabled() {
        return getSharedPreferences("rotam_settings", 0).getBoolean("reminders", true);
    }

    private String geminiKey() {
        return getSharedPreferences("rotam_settings", 0).getString("gemini_api_key", "");
    }

    private int reminder() {
        return getSharedPreferences("rotam_settings", 0).getInt("reminder_minutes", 15);
    }

    private int unresolvedCount() {
        int n = 0;
        long now = System.currentTimeMillis();
        for (Plan p : plans) {
            if (p.done || Plan.STATUS_DONE.equals(p.status) || Plan.STATUS_CANCELLED.equals(p.status)
                    || Plan.STATUS_MISSED.equals(p.status)) continue;
            long end = MissedPlanManager.endMillisForExternal(p);
            if (end > 0 && end < now) n++;
        }
        return n;
    }

    private List<Plan> forDate(String date) {
        List<Plan> out = new ArrayList<>();
        for (Plan p : plans) if (date.equals(p.date)) out.add(p);
        Collections.sort(out, Comparator.comparing(p ->
                p.startTime == null || p.startTime.isEmpty() ? "99:99" : p.startTime));
        return out;
    }

    private LinearLayout cardText(String s) {
        LinearLayout c = card();
        c.addView(text(s, 15, TEXT, true));
        return c;
    }

    private LinearLayout card() {
        LinearLayout c = column();
        c.setPadding(dp(16), dp(16), dp(16), dp(16));
        c.setBackground(round(SURFACE, 22));
        return c;
    }

    private TextView text(String s, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setTypeface(Typeface.create("sans", bold ? Typeface.BOLD : Typeface.NORMAL));
        t.setIncludeFontPadding(false);
        return t;
    }

    private TextView button(String s, int fg, int bg) {
        TextView t = text(s, 13, fg, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(10), 0, dp(10), 0);
        t.setBackground(round(bg, 16));
        return t;
    }

    private EditText input(String hint) {
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setHint(hint);
        e.setHintTextColor(MUTED);
        e.setTextColor(TEXT);
        e.setTextSize(15);
        e.setPadding(dp(14), 0, dp(14), 0);
        e.setBackground(round(SURFACE2, 16));
        return e;
    }

    private LinearLayout dialogBox() {
        LinearLayout b = column();
        b.setPadding(dp(18), dp(18), dp(18), dp(18));
        b.setBackground(round(BG, 28));
        return b;
    }

    private Dialog dialog() {
        Dialog d = new Dialog(this);
        d.setCancelable(true);
        return d;
    }

    private void fit(Dialog d, float frac) {
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawableResource(android.R.color.transparent);
            w.setLayout((int) (getResources().getDisplayMetrics().widthPixels * frac), -2);
            w.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }

    private void save() { PlanStore.save(this, plans); }

    private void schedule(Plan p) {
        android.content.SharedPreferences s = getSharedPreferences("rotam_settings", 0);
        ReminderScheduler.schedule(this, p, s.getInt("reminder_minutes", 15),
                s.getBoolean("reminders", true));
    }

    private void press(View v) {
        v.animate().scaleX(.97f).scaleY(.97f).setDuration(55)
                .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(105).start())
                .start();
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout row() {
        LinearLayout l = column();
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private void gap(LinearLayout l, int w) {
        l.addView(new View(this), new LinearLayout.LayoutParams(dp(w), 1));
    }

    private LinearLayout.LayoutParams weight(float weight, int height) {
        return new LinearLayout.LayoutParams(0, height, weight);
    }

    private LinearLayout.LayoutParams top(int n) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(n);
        return p;
    }

    private LinearLayout.LayoutParams left(int n) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, -2);
        p.leftMargin = dp(n);
        return p;
    }

    private LinearLayout.LayoutParams margins(int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(dp(l), dp(t), dp(r), dp(b));
        return p;
    }

    private int dp(int n) {
        return (int) (n * getResources().getDisplayMetrics().density + .5f);
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        return g;
    }

    private int statusBar() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id == 0 ? dp(24) : getResources().getDimensionPixelSize(id);
    }

    private String key(Calendar c) {
        return String.format(Locale.GERMANY, "%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    private Calendar parse(String k) {
        Calendar c = Calendar.getInstance();
        try {
            String[] p = k.split("-");
            c.set(Integer.parseInt(p[0]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[2]), 0, 0, 0);
            c.set(Calendar.MILLISECOND, 0);
        } catch (Exception ignored) {}
        return c;
    }

    private String safe(String s) { return s == null ? "" : s; }

    private String cap(String s) {
        return s == null || s.isEmpty() ? s : s.substring(0, 1).toUpperCase(TR) + s.substring(1);
    }

    private String dayShort(Calendar d) {
        return new SimpleDateFormat("EE", TR).format(d.getTime()).replace(".", "");
    }

    private String repeatLabel(String s) {
        if (Plan.REPEAT_DAILY.equals(s)) return "her gün";
        if (Plan.REPEAT_WEEKDAYS.equals(s)) return "hafta içi";
        if (Plan.REPEAT_WEEKLY.equals(s)) return "her hafta";
        return "";
    }

    private void toast(String s) { android.widget.Toast.makeText(this, s, android.widget.Toast.LENGTH_SHORT).show(); }

    private String categoryLabel(String k) {
        if ("book".equals(k)) return "Kitap";
        if ("work".equals(k)) return "İş";
        if ("video".equals(k)) return "Video";
        if ("sport".equals(k)) return "Spor";
        if ("faith".equals(k)) return "İbadet";
        if ("appointment".equals(k)) return "Randevu";
        if ("food".equals(k)) return "Yemek";
        return "Kişisel";
    }
}
