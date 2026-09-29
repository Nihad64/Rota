package com.rota.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.Dialog;
import android.widget.FrameLayout;
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
    private static final int BG = Color.rgb(247, 247, 249);
    private static final int CARD = Color.WHITE;
    private static final int TEXT = Color.rgb(20, 21, 25);
    private static final int MUTED = Color.rgb(112, 114, 122);
    private static final int LINE = Color.rgb(229, 230, 234);
    private static final int ACCENT = Color.rgb(20, 21, 25);

    private final Locale tr = new Locale("tr", "TR");
    private List<Plan> plans = new ArrayList<>();
    private LinearLayout content;
    private int tab = 0;
    private String selectedDate;
    private Calendar monthCursor;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
        }

        plans = PlanStore.load(this);
        selectedDate = key(Calendar.getInstance());
        monthCursor = Calendar.getInstance();
        buildShell();
        render();
    }

    private void buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        FrameLayout page = new FrameLayout(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setBackgroundColor(BG);
        page.addView(content, new FrameLayout.LayoutParams(-1, -1));

        TextView fab = makeText("+", 32, Color.WHITE, true);
        fab.setGravity(Gravity.CENTER);
        fab.setBackground(round(ACCENT, 22));
        fab.setElevation(dp(8));
        FrameLayout.LayoutParams fp = new FrameLayout.LayoutParams(dp(64), dp(64), Gravity.BOTTOM | Gravity.END);
        fp.setMargins(0, 0, dp(20), dp(18));
        page.addView(fab, fp);
        fab.setOnClickListener(v -> showPlanEditor(null, selectedDate));

        root.addView(page, new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(buildBottomBar(), new LinearLayout.LayoutParams(-1, dp(82)));
        setContentView(root);
    }

    private LinearLayout buildBottomBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        bar.setPadding(dp(10), dp(8), dp(10), dp(8));
        bar.setBackgroundColor(Color.WHITE);

        String[] labels = {"Bugün", "Plan", "Asistan"};
        String[] icons = {"⌂", "▦", "✦"};
        for (int i = 0; i < 3; i++) {
            final int index = i;
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(dp(8), 0, dp(8), 0);
            TextView icon = makeText(icons[i], 20, i == tab ? TEXT : MUTED, false);
            icon.setGravity(Gravity.CENTER);
            TextView label = makeText(labels[i], 12, i == tab ? TEXT : MUTED, true);
            label.setGravity(Gravity.CENTER);
            item.addView(icon, new LinearLayout.LayoutParams(-1, dp(28)));
            item.addView(label, new LinearLayout.LayoutParams(-1, dp(24)));
            if (i == tab) item.setBackground(round(Color.rgb(243,243,247), 20));
            item.setOnClickListener(v -> { tab = index; render(); });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(58), 1);
            p.setMargins(dp(4), 0, dp(4), 0);
            bar.addView(item, p);
        }
        return bar;
    }

    private void render() {
        content.removeAllViews();
        if (tab == 0) renderToday();
        else if (tab == 1) renderCalendar();
        else renderAssistant();
        rebuildBottomBar();
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
        selectedDate = key(now);
        List<Plan> today = plansFor(selectedDate);
        int done = 0;
        Plan next = null;
        for (Plan p : today) {
            if (p.done) done++;
            else if (next == null) next = p;
        }

        LinearLayout page = scrollColumn();
        addHeader(page, "Bugün",
                new SimpleDateFormat("EEEE · d MMMM", tr).format(now.getTime()),
                "Takvime git");

        LinearLayout progress = cardColumn();
        LinearLayout pr = row();
        LinearLayout left = column();
        left.addView(makeText("BUGÜNÜN AKIŞI", 12, MUTED, true));
        left.addView(makeText(done + " / " + today.size(), 28, TEXT, true));
        pr.addView(left, new LinearLayout.LayoutParams(0, -2, 1));
        pr.addView(makeText(today.isEmpty() ? "0%" :
                Math.round(done * 100f / today.size()) + "%", 14, MUTED, true));
        progress.addView(pr);
        progress.addView(makeProgressBar(done, today.size()));
        page.addView(progress, marginParams());

        if (next != null) {
            LinearLayout nextCard = cardColumn();
            nextCard.setBackground(round(Color.rgb(28,29,33), 24));
            TextView small = makeText("SIRADAKİ", 11, Color.rgb(150,151,158), true);
            TextView title = makeText(next.title, 20, Color.WHITE, true);
            title.setMaxLines(2);
            TextView meta = makeText(planMeta(next), 13, Color.rgb(205,206,211), false);
            nextCard.addView(small);
            nextCard.addView(title);
            nextCard.addView(meta);
            page.addView(nextCard, marginParams());
        }

        LinearLayout titleRow = row();
        titleRow.addView(makeText("Bugünün planı", 20, TEXT, true),
                new LinearLayout.LayoutParams(0, -2, 1));
        TextView add = makeText("+ Ekle", 14, TEXT, true);
        titleRow.addView(add);
        add.setOnClickListener(v -> showPlanEditor(null, selectedDate));
        page.addView(titleRow, marginParams(20, 20, 20, 10));

        if (today.isEmpty()) {
            LinearLayout empty = cardColumn();
            empty.addView(makeText("Günün boş.", 19, TEXT, true));
            empty.addView(makeText("Saatli, esnek veya tüm gün plan ekleyebilirsin.",
                    14, MUTED, false));
            page.addView(empty, marginParams());
        } else {
            for (Plan p : today) addPlanCard(page, p);
        }
    }

    private View makeProgressBar(int done, int total) {
        FrameLayout wrap = new FrameLayout(this);
        wrap.setBackground(round(LINE, 10));
        int width = total == 0 ? 0 : Math.max(8, (int) (300f * done / total));
        View fill = new View(this);
        fill.setBackground(round(ACCENT, 10));
        wrap.addView(fill, new FrameLayout.LayoutParams(dp(width), dp(8)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(8));
        lp.topMargin = dp(12);
        wrap.setLayoutParams(lp);
        return wrap;
    }

    private void addPlanCard(LinearLayout parent, Plan p) {
        LinearLayout card = cardColumn();
        LinearLayout top = row();
        LinearLayout main = column();
        TextView title = makeText(p.title, 17, p.done ? MUTED : TEXT, true);
        title.setMaxLines(2);
        main.addView(title);
        main.addView(makeText(planMeta(p), 13, MUTED, false));
        top.addView(main, new LinearLayout.LayoutParams(0, -2, 1));

        TextView check = makeText(p.done ? "✓" : "○", 25,
                p.done ? TEXT : Color.rgb(175,177,184), true);
        check.setGravity(Gravity.CENTER);
        top.addView(check, new LinearLayout.LayoutParams(dp(44), dp(44)));
        card.addView(top);

        LinearLayout actions = row();
        actions.setVisibility(View.GONE);
        TextView edit = makeText("Düzenle", 13, TEXT, true);
        TextView del = makeText("Sil", 13, Color.rgb(185,50,50), true);
        actions.addView(edit, new LinearLayout.LayoutParams(0, dp(36), 1));
        actions.addView(del, new LinearLayout.LayoutParams(0, dp(36), 1));
        card.addView(actions);

        check.setOnClickListener(v -> {
            p.done = !p.done;
            PlanStore.save(this, plans);
            render();
        });
        top.setOnClickListener(v -> {
            if (actions.getVisibility() == View.GONE) actions.setVisibility(View.VISIBLE);
            else showPlanEditor(p, p.date);
        });
        edit.setOnClickListener(v -> showPlanEditor(p, p.date));
        del.setOnClickListener(v -> confirmDelete(p));
        parent.addView(card, marginParams(20, 0, 20, 10));
    }

    private void renderCalendar() {
        LinearLayout page = scrollColumn();
        addHeader(page, "Plan", "Takvim ve tüm günlerin", "Bugüne dön");

        LinearLayout monthBar = row();
        TextView prev = makeText("‹", 32, TEXT, false);
        prev.setGravity(Gravity.CENTER);
        TextView month = makeText(new SimpleDateFormat("MMMM yyyy", tr)
                .format(monthCursor.getTime()), 19, TEXT, true);
        month.setGravity(Gravity.CENTER);
        TextView next = makeText("›", 32, TEXT, false);
        next.setGravity(Gravity.CENTER);
        monthBar.addView(prev, new LinearLayout.LayoutParams(dp(52), dp(52)));
        monthBar.addView(month, new LinearLayout.LayoutParams(0, dp(52), 1));
        monthBar.addView(next, new LinearLayout.LayoutParams(dp(52), dp(52)));
        prev.setOnClickListener(v -> { monthCursor.add(Calendar.MONTH, -1); render(); });
        next.setOnClickListener(v -> { monthCursor.add(Calendar.MONTH, 1); render(); });
        page.addView(monthBar, marginParams(20, 8, 20, 8));

        LinearLayout week = row();
        String[] names = {"Pzt","Sal","Çar","Per","Cum","Cmt","Paz"};
        for (String n : names) {
            TextView t = makeText(n, 12, MUTED, true);
            t.setGravity(Gravity.CENTER);
            week.addView(t, new LinearLayout.LayoutParams(0, dp(30), 1));
        }
        page.addView(week, marginParams(20, 0, 20, 4));

        LinearLayout grid = column();
        Calendar first = (Calendar) monthCursor.clone();
        first.set(Calendar.DAY_OF_MONTH, 1);
        int offset = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        int days = monthCursor.getActualMaximum(Calendar.DAY_OF_MONTH);
        int cell = 0;

        for (int r = 0; r < 6; r++) {
            LinearLayout line = row();
            for (int c = 0; c < 7; c++) {
                int day = cell - offset + 1;
                TextView d = makeText("", 14, TEXT, true);
                d.setGravity(Gravity.CENTER);
                if (day > 0 && day <= days) {
                    d.setText(String.valueOf(day));
                    Calendar date = (Calendar) monthCursor.clone();
                    date.set(Calendar.DAY_OF_MONTH, day);
                    String k = key(date);
                    boolean selected = k.equals(selectedDate);
                    boolean isToday = k.equals(key(Calendar.getInstance()));
                    if (selected) {
                        d.setBackground(round(ACCENT, 16));
                        d.setTextColor(Color.WHITE);
                    } else if (isToday) {
                        d.setBackground(round(Color.rgb(232,232,237), 16));
                    }
                    if (!plansFor(k).isEmpty()) d.setText(day + "\n•");
                    d.setOnClickListener(v -> { selectedDate = k; render(); });
                }
                line.addView(d, new LinearLayout.LayoutParams(0, dp(48), 1));
                cell++;
            }
            grid.addView(line, new LinearLayout.LayoutParams(-1, dp(48)));
        }
        page.addView(grid, marginParams(20, 0, 20, 12));

        LinearLayout selectedHeader = row();
        selectedHeader.addView(makeText(prettyKey(selectedDate), 19, TEXT, true),
                new LinearLayout.LayoutParams(0, -2, 1));
        TextView add = makeText("+ Plan", 14, TEXT, true);
        selectedHeader.addView(add);
        add.setOnClickListener(v -> showPlanEditor(null, selectedDate));
        page.addView(selectedHeader, marginParams(20, 8, 20, 10));

        List<Plan> selectedPlans = plansFor(selectedDate);
        if (selectedPlans.isEmpty()) {
            LinearLayout empty = cardColumn();
            empty.addView(makeText("Bu gün boş", 18, TEXT, true));
            empty.addView(makeText("Plan eklemek için + Plan'a dokun.",
                    14, MUTED, false));
            page.addView(empty, marginParams());
        } else {
            for (Plan p : selectedPlans) addPlanCard(page, p);
        }
    }

    private void renderAssistant() {
        LinearLayout page = scrollColumn();
        addHeader(page, "Asistan", "Planlarını daha hızlı kur", "");

        LinearLayout hero = cardColumn();
        hero.setBackground(round(Color.rgb(28,29,33), 26));
        hero.addView(makeText("Yarınını tek seferde kur.", 24, Color.WHITE, true));
        hero.addView(makeText("İş, okul, ibadet ve kişisel zamanını aynı akışta düzenle.",
                14, Color.rgb(205,206,211), false));
        page.addView(hero, marginParams(20, 0, 20, 12));

        addActionCard(page, "Yarını planla",
                "Çalışma saatini ve yapmak istediklerini gir.",
                () -> showPlanEditor(null, key(addDays(Calendar.getInstance(), 1))));
        addActionCard(page, "Şimdi ne yapmalıyım?",
                "Bugünkü ilk tamamlanmamış planını göster.",
                this::showNextTask);
        addActionCard(page, "Bugünü düzenle",
                "Bugünkü planlarını hızlıca gözden geçir.",
                () -> { tab = 0; render(); });

        LinearLayout tips = cardColumn();
        tips.addView(makeText("Kısa yollar", 17, TEXT, true));
        tips.addView(makeText(
                "• Saatli planlar için başlangıç ve bitiş saatini seç.\n" +
                "• Takvimden istediğin güne doğrudan plan ekle.\n" +
                "• Bir plana dokunarak düzenle/sil seçeneklerini aç.",
                14, MUTED, false));
        page.addView(tips, marginParams());
    }

    private void addActionCard(LinearLayout parent, String title, String sub,
                               View.OnClickListener click) {
        LinearLayout c = cardColumn();
        LinearLayout r = row();
        LinearLayout text = column();
        text.addView(makeText(title, 17, TEXT, true));
        text.addView(makeText(sub, 13, MUTED, false));
        r.addView(text, new LinearLayout.LayoutParams(0, -2, 1));
        TextView arrow = makeText("›", 28, MUTED, false);
        arrow.setGravity(Gravity.CENTER);
        r.addView(arrow, new LinearLayout.LayoutParams(dp(34), dp(44)));
        c.addView(r);
        c.setOnClickListener(click);
        parent.addView(c, marginParams(20, 0, 20, 10));
    }

    private void showNextTask() {
        for (Plan p : plansFor(key(Calendar.getInstance()))) {
            if (!p.done) {
                new AlertDialog.Builder(this)
                        .setTitle("Şimdi")
                        .setMessage(p.title + "\n\n" + planMeta(p))
                        .setPositiveButton("Tamam", null).show();
                return;
            }
        }
        new AlertDialog.Builder(this)
                .setTitle("Şimdi")
                .setMessage("Bugün tamamlanmamış bir planın yok.")
                .setPositiveButton("Tamam", null).show();
    }

    private void showPlanEditor(Plan editing, String initialDate) {
        final Dialog dialog = new Dialog(this);
        LinearLayout box = column();
        box.setPadding(dp(22), dp(18), dp(22), dp(18));
        box.setBackground(round(CARD, 28));

        box.addView(makeText(editing == null ? "Yeni plan" : "Planı düzenle",
                22, TEXT, true));

        TextView dateButton = makeField("Tarih", prettyKey(initialDate));
        box.addView(dateButton, fieldParams());

        TextView titleField = makeField("Plan adı",
                editing == null ? "" : editing.title);
        box.addView(titleField, fieldParams());

        box.addView(makeText("Plan türü", 12, MUTED, true));

        LinearLayout types = row();
        String[] typeNames = {Plan.HOURLY, Plan.FLEX, Plan.ALL_DAY};
        TextView[] typeViews = new TextView[3];
        String[] selectedType = {editing == null ? Plan.HOURLY : editing.type};

        for (int i = 0; i < 3; i++) {
            final String type = typeNames[i];
            TextView chip = makeText(type, 12,
                    type.equals(selectedType[0]) ? Color.WHITE : TEXT, true);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(8), 0, dp(8), 0);
            chip.setBackground(round(type.equals(selectedType[0])
                    ? ACCENT : Color.rgb(242,242,246), 18));
            typeViews[i] = chip;
            chip.setOnClickListener(v -> {
                selectedType[0] = type;
                refreshChips(typeViews, type);
            });
            types.addView(chip, new LinearLayout.LayoutParams(0, dp(42), 1));
            if (i < 2) types.addView(new View(this),
                    new LinearLayout.LayoutParams(dp(6), 1));
        }
        box.addView(types, fieldParams());

        TextView start = makeField("Başlangıç",
                editing == null ? "Saat seç" :
                        safe(editing.startTime, editing.time));
        TextView end = makeField("Bitiş",
                editing == null || editing.endTime.isEmpty()
                        ? "Saat seç" : editing.endTime);
        box.addView(start, fieldParams());
        box.addView(end, fieldParams());
        box.addView(makeText(
                "Süre, başlangıç ve bitiş saatinden otomatik hesaplanır.",
                12, MUTED, false));

        final String[] chosenDate = {initialDate};
        dateButton.setOnClickListener(v -> {
            Calendar c = parseKey(chosenDate[0]);
            new DatePickerDialog(this,
                    (view, year, month, day) -> {
                        Calendar n = Calendar.getInstance();
                        n.set(year, month, day);
                        chosenDate[0] = key(n);
                        dateButton.setText(prettyKey(chosenDate[0]));
                    },
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH)).show();
        });

        titleField.setOnClickListener(v ->
                inputDialog("Plan adı",
                        editing == null ? "" : editing.title,
                        value -> titleField.setText(value)));

        start.setOnClickListener(v ->
                chooseTime(start, editing == null ? "" :
                        safe(editing.startTime, editing.time)));
        end.setOnClickListener(v ->
                chooseTime(end, editing == null ? "" : editing.endTime));

        LinearLayout buttons = row();
        buttons.setGravity(Gravity.END);
        TextView cancel = makeText("Vazgeç", 14, MUTED, true);
        TextView save = makeText(editing == null ? "Planı ekle" : "Kaydet",
                14, TEXT, true);
        buttons.addView(cancel, new LinearLayout.LayoutParams(dp(90), dp(48)));
        buttons.addView(save, new LinearLayout.LayoutParams(dp(110), dp(48)));
        box.addView(buttons);

        cancel.setOnClickListener(v -> dialog.dismiss());
        save.setOnClickListener(v -> {
            String title = titleField.getText().toString().trim();
            if (title.isEmpty()) {
                inputDialog("Plan adı", "", value -> titleField.setText(value));
                return;
            }

            Plan p = editing == null
                    ? new Plan(System.currentTimeMillis(), chosenDate[0], title,
                    selectedType[0], "", "", false)
                    : editing;

            p.date = chosenDate[0];
            p.title = title;
            p.type = selectedType[0];
            p.startTime = start.getText().toString().equals("Saat seç")
                    ? "" : start.getText().toString();
            p.endTime = end.getText().toString().equals("Saat seç")
                    ? "" : end.getText().toString();

            if (Plan.ALL_DAY.equals(p.type)) {
                p.startTime = "";
                p.endTime = "";
                p.time = "";
                p.duration = "";
            } else {
                p.time = p.startTime;
                p.duration = Plan.durationBetween(p.startTime, p.endTime);
            }

            if (editing == null) plans.add(p);
            PlanStore.save(this, plans);
            scheduleReminder(p);
            selectedDate = p.date;
            monthCursor = parseKey(p.date);
            dialog.dismiss();
            render();
        });

        dialog.setContentView(box);
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.90),
                    -2);
        }
    }

    private void refreshChips(TextView[] chips, String selected) {
        for (TextView c : chips) {
            boolean on = c.getText().toString().equals(selected);
            c.setTextColor(on ? Color.WHITE : TEXT);
            c.setBackground(round(on ? ACCENT : Color.rgb(242,242,246), 18));
        }
    }

    private void inputDialog(String title, String value, ValueCallback callback) {
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setSingleLine(true);
        input.setText(value);
        input.setTextSize(16);
        input.setPadding(dp(4), 0, dp(4), 0);
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(input)
                .setNegativeButton("Vazgeç", null)
                .setPositiveButton("Tamam",
                        (d, w) -> callback.value(input.getText().toString().trim()))
                .show();
    }

    private interface ValueCallback { void value(String v); }

    private void chooseTime(TextView target, String existing) {
        Calendar c = Calendar.getInstance();
        if (existing != null && existing.matches("\\d{2}:\\d{2}")) {
            c.set(Calendar.HOUR_OF_DAY,
                    Integer.parseInt(existing.substring(0, 2)));
            c.set(Calendar.MINUTE,
                    Integer.parseInt(existing.substring(3)));
        }
        new TimePickerDialog(this,
                (view, hour, minute) ->
                        target.setText(String.format(Locale.GERMANY,
                                "%02d:%02d", hour, minute)),
                c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE), true).show();
    }

    private void confirmDelete(Plan p) {
        new AlertDialog.Builder(this)
                .setTitle("Planı sil?")
                .setMessage(p.title)
                .setNegativeButton("Vazgeç", null)
                .setPositiveButton("Sil", (d, w) -> {
                    cancelReminder(p);
                    plans.remove(p);
                    PlanStore.save(this, plans);
                    render();
                }).show();
    }

    private void scheduleReminder(Plan p) {
        cancelReminder(p);
        if (!Plan.HOURLY.equals(p.type) || p.startTime == null ||
                !p.startTime.matches("\\d{2}:\\d{2}")) return;

        Calendar alarm = parseKey(p.date);
        alarm.set(Calendar.HOUR_OF_DAY,
                Integer.parseInt(p.startTime.substring(0, 2)));
        alarm.set(Calendar.MINUTE,
                Integer.parseInt(p.startTime.substring(3)));
        alarm.set(Calendar.SECOND, 0);
        alarm.set(Calendar.MILLISECOND, 0);

        if (alarm.getTimeInMillis() <= System.currentTimeMillis()) return;

        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("title", p.title);
        PendingIntent pi = PendingIntent.getBroadcast(
                this, (int) (p.id & 0x7fffffff), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am != null) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                    alarm.getTimeInMillis(), pi);
        }
    }

    private void cancelReminder(Plan p) {
        Intent i = new Intent(this, ReminderReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(
                this, (int) (p.id & 0x7fffffff), i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am != null) am.cancel(pi);
        pi.cancel();
    }

    private String planMeta(Plan p) {
        if (Plan.ALL_DAY.equals(p.type)) return "Tüm gün";
        if (!p.startTime.isEmpty() && !p.endTime.isEmpty()) {
            return p.startTime + " – " + p.endTime + " · " + p.duration;
        }
        if (!p.startTime.isEmpty()) {
            return p.startTime + (p.duration.isEmpty() ? "" : " · " + p.duration);
        }
        return Plan.FLEX.equals(p.type) ? "Esnek" : "Saat seçilmedi";
    }

    private String safe(String a, String b) {
        return a != null && !a.isEmpty() ? a : (b == null ? "" : b);
    }

    private List<Plan> plansFor(String date) {
        List<Plan> out = new ArrayList<>();
        for (Plan p : plans) if (date.equals(p.date)) out.add(p);
        Collections.sort(out, Comparator
                .comparing((Plan p) ->
                        p.startTime == null || p.startTime.isEmpty()
                                ? "99:99" : p.startTime)
                .thenComparing(p -> p.title));
        return out;
    }

    private String prettyKey(String k) {
        return new SimpleDateFormat("EEEE · d MMMM", tr)
                .format(parseKey(k).getTime());
    }

    private String key(Calendar c) {
        return String.format(Locale.GERMANY, "%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1,
                c.get(Calendar.DAY_OF_MONTH));
    }

    private Calendar parseKey(String k) {
        Calendar c = Calendar.getInstance();
        try {
            String[] a = k.split("-");
            c.set(Integer.parseInt(a[0]), Integer.parseInt(a[1]) - 1,
                    Integer.parseInt(a[2]), 0, 0, 0);
            c.set(Calendar.MILLISECOND, 0);
        } catch (Exception ignored) {}
        return c;
    }

    private Calendar addDays(Calendar c, int n) {
        Calendar x = (Calendar) c.clone();
        x.add(Calendar.DAY_OF_MONTH, n);
        return x;
    }

    private void addHeader(LinearLayout page, String title,
                           String sub, String action) {
        LinearLayout h = row();
        h.setPadding(dp(20), dp(20), dp(20), dp(10));
        LinearLayout tx = column();
        tx.addView(makeText("ROTA", 11, MUTED, true));
        tx.addView(makeText(title, 38, TEXT, true));
        tx.addView(makeText(sub, 14, MUTED, false));
        h.addView(tx, new LinearLayout.LayoutParams(0, -2, 1));

        if (action != null && !action.isEmpty()) {
            TextView a = makeText(action, 12, TEXT, true);
            a.setGravity(Gravity.CENTER);
            a.setBackground(round(Color.WHITE, 18));
            a.setPadding(dp(12), 0, dp(12), 0);
            a.setOnClickListener(v -> {
                if ("Takvime git".equals(action)) {
                    tab = 1;
                    render();
                } else {
                    selectedDate = key(Calendar.getInstance());
                    monthCursor = Calendar.getInstance();
                    render();
                }
            });
            h.addView(a, new LinearLayout.LayoutParams(-2, dp(42)));
        }
        page.addView(h);
    }

    private LinearLayout scrollColumn() {
        ScrollView s = new ScrollView(this);
        s.setFillViewport(true);
        s.setBackgroundColor(BG);
        LinearLayout c = column();
        s.addView(c, new ScrollView.LayoutParams(-1, -2));
        content.addView(s, new LinearLayout.LayoutParams(-1, 0, 1));
        return c;
    }

    private LinearLayout cardColumn() {
        LinearLayout c = column();
        c.setPadding(dp(18), dp(16), dp(18), dp(16));
        c.setBackground(round(CARD, 22));
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

    private TextView makeText(String text, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setTypeface(Typeface.create("sans",
                bold ? Typeface.BOLD : Typeface.NORMAL));
        t.setIncludeFontPadding(false);
        return t;
    }

    private TextView makeField(String label, String value) {
        TextView t = makeText(value, 16, TEXT, false);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(dp(14), 0, dp(14), 0);
        t.setBackground(round(Color.rgb(244,244,247), 16));
        t.setTag(label);
        return t;
    }

    private LinearLayout.LayoutParams fieldParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(54));
        p.setMargins(0, dp(8), 0, 0);
        return p;
    }

    private LinearLayout.LayoutParams marginParams() {
        return marginParams(20, 0, 20, 10);
    }

    private LinearLayout.LayoutParams marginParams(int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(dp(l), dp(t), dp(r), dp(b));
        return p;
    }

    private GradientDrawable round(int color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp((int) radius));
        return g;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
