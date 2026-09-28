package com.rota.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private RotaView view;
    private List<Plan> plans = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.rgb(246, 246, 248));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setNavigationBarColor(Color.rgb(17, 18, 22));

        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
        }

        plans = PlanStore.load(this);
        view = new RotaView(this);
        setContentView(view);
    }

    private String todayKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).format(new Date());
    }

    private String prettyDate() {
        return new SimpleDateFormat("EEEE · d MMMM", new Locale("tr", "TR")).format(new Date());
    }

    private void showAddDialog() {
        final LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(4);
        box.setPadding(pad, 0, pad, 0);

        final EditText title = new EditText(this);
        title.setHint("Ne yapmak istiyorsun?");
        title.setSingleLine(true);
        box.addView(title);

        final TextView info = new TextView(this);
        info.setText("Tür: Saatli");
        info.setPadding(0, dp(10), 0, dp(4));
        box.addView(info);

        final String[] types = {"Saatli", "Tüm gün", "Esnek"};
        final int[] selected = {0};

        info.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Plan türü")
                        .setSingleChoiceItems(types, selected[0], new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface dialog, int which) {
                                selected[0] = which;
                                info.setText("Tür: " + types[which]);
                                dialog.dismiss();
                            }
                        }).show();
            }
        });

        final EditText time = new EditText(this);
        time.setHint("Saat  ·  örn. 10:00");
        time.setSingleLine(true);
        box.addView(time);

        final EditText duration = new EditText(this);
        duration.setHint("Süre  ·  örn. 60 dk");
        duration.setSingleLine(true);
        box.addView(duration);

        new AlertDialog.Builder(this)
                .setTitle("Yeni plan")
                .setView(box)
                .setNegativeButton("Vazgeç", null)
                .setPositiveButton("Ekle", null)
                .create()
                .setOnShowListener(dialog -> {
                    AlertDialog d = (AlertDialog) dialog;
                    d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                        String name = title.getText().toString().trim();
                        if (name.isEmpty()) {
                            title.setError("Bir isim yaz");
                            return;
                        }

                        String type = types[selected[0]];
                        String t = time.getText().toString().trim();
                        String dur = duration.getText().toString().trim();

                        if (!Plan.HOURLY.equals(type)) {
                            t = "";
                            dur = "";
                        }

                        Plan p = new Plan(System.currentTimeMillis(), todayKey(), name, type, t, dur, false);
                        plans.add(p);
                        PlanStore.save(MainActivity.this, plans);
                        if (Plan.HOURLY.equals(type)) scheduleReminder(p);

                        d.dismiss();
                        view.invalidate();
                    });
                }).show();
    }

    private void scheduleReminder(Plan plan) {
        if (plan.time == null || !plan.time.matches("\\d{1,2}:\\d{2}")) return;
        try {
            String[] parts = plan.time.split(":");
            Calendar alarm = Calendar.getInstance();
            alarm.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
            alarm.set(Calendar.MINUTE, Integer.parseInt(parts[1]));
            alarm.set(Calendar.SECOND, 0);
            alarm.set(Calendar.MILLISECOND, 0);
            if (alarm.getTimeInMillis() <= System.currentTimeMillis()) return;

            Intent intent = new Intent(this, ReminderReceiver.class);
            intent.putExtra("title", plan.title);

            PendingIntent pi = PendingIntent.getBroadcast(
                    this,
                    (int) (plan.id & 0x7fffffff),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarm.getTimeInMillis(), pi);
        } catch (Exception ignored) {
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private class RotaView extends View {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        float density;
        int tab = 0;
        RectF fab = new RectF();
        RectF[] taskRects = new RectF[80];

        RotaView(Context c) {
            super(c);
            density = getResources().getDisplayMetrics().density;
            p.setTypeface(android.graphics.Typeface.create("sans", android.graphics.Typeface.NORMAL));
            setBackgroundColor(Color.rgb(246, 246, 248));
        }

        float d(float n) { return n * density; }

        void text(Canvas c, String s, float x, float y, float size, int color, boolean bold) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(color);
            p.setTextSize(d(size));
            p.setTypeface(android.graphics.Typeface.create("sans", bold ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL));
            c.drawText(s, d(x), d(y), p);
        }

        void round(Canvas c, float l, float t, float r, float b, float radius, int color) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(color);
            c.drawRoundRect(new RectF(d(l), d(t), d(r), d(b)), d(radius), d(radius), p);
        }

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float w = getWidth() / density;
            float h = getHeight() / density;

            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.rgb(246,246,248));
            c.drawRect(0,0,getWidth(),getHeight(),p);

            if (tab == 0) drawToday(c, w, h);
            else if (tab == 1) drawPlan(c, w, h);
            else drawAssistant(c, w, h);

            drawBottom(c, w, h);
            drawFab(c, w, h);
        }

        void drawHeader(Canvas c, String title, String subtitle) {
            text(c, "ROTA", 24, 31, 12, Color.rgb(128,128,134), true);
            text(c, title, 24, 72, 38, Color.rgb(22,23,26), true);
            if (subtitle != null) text(c, subtitle, 24, 98, 14, Color.rgb(110,111,118), false);
        }

        List<Plan> todayPlans() {
            List<Plan> out = new ArrayList<>();
            String key = todayKey();
            for (Plan p : plans) if (key.equals(p.date)) out.add(p);
            Collections.sort(out, new Comparator<Plan>() {
                @Override public int compare(Plan a, Plan b) {
                    if (a.time.isEmpty() && b.time.isEmpty()) return a.title.compareToIgnoreCase(b.title);
                    if (a.time.isEmpty()) return 1;
                    if (b.time.isEmpty()) return -1;
                    return a.time.compareTo(b.time);
                }
            });
            return out;
        }

        void drawToday(Canvas c, float w, float h) {
            drawHeader(c, "Bugün", prettyDate());
            List<Plan> today = todayPlans();
            int total = today.size();
            int done = 0;
            Plan next = null;
            for (Plan p : today) {
                if (p.done) done++;
                if (next == null && !p.done) next = p;
            }

            round(c, 20, 116, w-20, 190, 24, Color.WHITE);
            text(c, "Bugünün akışı", 38, 143, 13, Color.rgb(116,117,124), true);
            text(c, done + " / " + total, 38, 171, 25, Color.rgb(22,23,26), true);
            text(c, "tamamlandı", 96, 171, 14, Color.rgb(116,117,124), false);
            round(c, 220, 140, w-38, 156, 8, Color.rgb(235,235,239));
            if (total > 0) {
                float progress = (w-258) * (done / (float) total);
                round(c, 220, 140, 220+progress, 156, 8, Color.rgb(22,23,26));
            }

            float y = 214;
            if (next != null) {
                round(c, 20, y, w-20, y+76, 22, Color.rgb(22,23,26));
                text(c, "SIRADAKİ", 38, y+25, 11, Color.rgb(180,181,187), true);
                text(c, next.title, 38, y+50, 19, Color.WHITE, true);
                String meta = Plan.HOURLY.equals(next.type) ? next.time + " · " + next.duration : next.type;
                text(c, meta, w-160, y+50, 12, Color.rgb(205,206,211), false);
                y += 92;
            }

            text(c, "Bugünün planı", 24, y+4, 16, Color.rgb(22,23,26), true);
            y += 22;

            if (today.isEmpty()) {
                round(c, 20, y, w-20, y+104, 22, Color.WHITE);
                text(c, "Bugün için henüz plan yok.", 38, y+44, 16, Color.rgb(22,23,26), true);
                text(c, "＋ ile ilk planını ekle.", 38, y+70, 13, Color.rgb(116,117,124), false);
                return;
            }

            int index = 0;
            for (Plan plan : today) {
                if (index >= taskRects.length) break;
                RectF r = new RectF(d(20), d(y), d(w-20), d(y+66));
                taskRects[index++] = r;

                round(c, 20, y, w-20, y+66, 18, Color.WHITE);
                String left = Plan.HOURLY.equals(plan.type) ? plan.time : " ";
                text(c, left, 38, y+27, 12, Color.rgb(122,123,130), true);
                text(c, plan.title, 86, y+27, 16, plan.done ? Color.rgb(150,151,157) : Color.rgb(22,23,26), true);
                String meta = Plan.HOURLY.equals(plan.type) ? plan.duration : plan.type;
                text(c, meta, 86, y+48, 12, Color.rgb(122,123,130), false);

                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(d(2));
                p.setColor(plan.done ? Color.rgb(22,23,26) : Color.rgb(190,191,197));
                c.drawCircle(d(w-44), d(y+33), d(11), p);
                if (plan.done) {
                    p.setStyle(Paint.Style.FILL);
                    p.setColor(Color.rgb(22,23,26));
                    c.drawCircle(d(w-44), d(y+33), d(11), p);
                    text(c, "✓", w-50, y+39, 13, Color.WHITE, true);
                }
                y += 76;
            }
        }

        void drawPlan(Canvas c, float w, float h) {
            drawHeader(c, "Plan", "Önümüzdeki günler");
            round(c, 20, 116, w-20, 166, 20, Color.WHITE);
            text(c, "←", 38, 148, 22, Color.rgb(22,23,26), true);
            text(c, "Eylül 2026", w/2-46, 148, 16, Color.rgb(22,23,26), true);
            text(c, "→", w-58, 148, 22, Color.rgb(22,23,26), true);

            String[] days = {"Pzt","Sal","Çar","Per","Cum","Cmt","Paz"};
            int[] cols = new int[7];
            for (int i=0;i<7;i++) {
                float x = 20 + i*((w-40)/7f);
                text(c, days[i], x+5, 196, 11, Color.rgb(126,127,133), true);
            }

            List<Plan> sorted = new ArrayList<>(plans);
            Collections.sort(sorted, new Comparator<Plan>() {
                @Override public int compare(Plan a, Plan b) { return a.date.compareTo(b.date); }
            });

            float y=224;
            int count=0;
            for (Plan p : sorted) {
                if (count++ > 7) break;
                round(c, 20, y, w-20, y+58, 18, Color.WHITE);
                text(c, p.date, 36, y+23, 11, Color.rgb(126,127,133), true);
                text(c, p.title, 104, y+24, 15, Color.rgb(22,23,26), true);
                text(c, p.type, 104, y+45, 12, Color.rgb(126,127,133), false);
                y += 68;
            }
            if (sorted.isEmpty()) {
                round(c, 20, y, w-20, y+94, 20, Color.WHITE);
                text(c, "Takvim henüz boş.", 38, y+40, 16, Color.rgb(22,23,26), true);
                text(c, "Plan ekledikçe burada görünecek.", 38, y+66, 13, Color.rgb(126,127,133), false);
            }
        }

        void drawAssistant(Canvas c, float w, float h) {
            drawHeader(c, "Asistan", "Planların için");
            round(c, 20, 116, w-20, 208, 24, Color.WHITE);
            text(c, "Yarını birlikte kuralım.", 38, 148, 21, Color.rgb(22,23,26), true);
            text(c, "İş, okul, ibadet ve kişisel zamanı", 38, 173, 13, Color.rgb(112,113,120), false);
            text(c, "tek planda dengeleyebilirim.", 38, 192, 13, Color.rgb(112,113,120), false);

            round(c, 20, 230, w-20, 286, 18, Color.rgb(22,23,26));
            text(c, "Yarını planla", 40, 264, 15, Color.WHITE, true);

            round(c, 20, 304, w-20, 362, 18, Color.WHITE);
            text(c, "Şimdi ne yapmalıyım?", 40, 339, 15, Color.rgb(22,23,26), true);

            text(c, "Örnek", 24, 396, 12, Color.rgb(126,127,133), true);
            text(c, "“Yarın 14–22 çalışıyorum.", 24, 424, 14, Color.rgb(22,23,26), false);
            text(c, "Sabah Risale ve spor istiyorum.”", 24, 446, 14, Color.rgb(22,23,26), false);
        }

        void drawBottom(Canvas c, float w, float h) {
            float top = h - 76;
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.WHITE);
            c.drawRect(0, d(top), getWidth(), getHeight(), p);

            String[] labels = {"Bugün","Plan","Asistan"};
            String[] icons = {"⌂","▦","✦"};
            for (int i=0;i<3;i++) {
                float cx = w*(i+0.5f)/3f;
                if (tab == i) round(c, cx-54, top+10, cx+54, top+66, 18, Color.rgb(242,242,246));
                text(c, icons[i], cx-7, top+31, 19, tab==i ? Color.rgb(22,23,26) : Color.rgb(142,143,150), false);
                text(c, labels[i], cx-27, top+56, 12, tab==i ? Color.rgb(22,23,26) : Color.rgb(142,143,150), true);
            }
        }

        void drawFab(Canvas c, float w, float h) {
            float cx = w - 58;
            float cy = h - 108;
            fab.set(d(cx-30), d(cy-30), d(cx+30), d(cy+30));
            round(c, cx-30, cy-30, cx+30, cy+30, 19, Color.rgb(22,23,26));
            text(c, "+", cx-12, cy+10, 35, Color.WHITE, false);
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            if (e.getAction() != MotionEvent.ACTION_UP) return true;
            float x = e.getX()/density;
            float y = e.getY()/density;
            float h = getHeight()/density;
            float w = getWidth()/density;

            if (y > h-78) {
                if (x < w/3f) tab=0;
                else if (x < 2*w/3f) tab=1;
                else tab=2;
                invalidate();
                return true;
            }

            if (fab.contains(d(x), d(y))) {
                showAddDialog();
                return true;
            }

            if (tab == 0) {
                List<Plan> today = todayPlans();
                for (int i=0;i<today.size() && i<taskRects.length;i++) {
                    RectF r = taskRects[i];
                    if (r != null && r.contains(d(x), d(y))) {
                        Plan selected = today.get(i);
                        selected.done = !selected.done;
                        PlanStore.save(MainActivity.this, plans);
                        invalidate();
                        return true;
                    }
                }
            }
            return true;
        }
    }
}
