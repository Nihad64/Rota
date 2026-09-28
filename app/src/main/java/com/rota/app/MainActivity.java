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

        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
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
        box.setPadding(dp(4), 0, dp(4), 0);

        final EditText title = new EditText(this);
        title.setHint("Ne yapmak istiyorsun?");
        title.setSingleLine(true);
        box.addView(title);

        final TextView typeButton = new TextView(this);
        typeButton.setText("Tür: Saatli");
        typeButton.setTextSize(15);
        typeButton.setTextColor(Color.rgb(30, 31, 35));
        typeButton.setPadding(0, dp(14), 0, dp(8));
        box.addView(typeButton);

        final String[] types = {"Saatli", "Tüm gün", "Esnek"};
        final int[] selected = {0};

        typeButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Plan türü")
                        .setSingleChoiceItems(types, selected[0], new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface dialog, int which) {
                                selected[0] = which;
                                typeButton.setText("Tür: " + types[which]);
                                dialog.dismiss();
                            }
                        })
                        .show();
            }
        });

        final EditText time = new EditText(this);
        time.setHint("Saat · örn. 10:00");
        time.setSingleLine(true);
        box.addView(time);

        final EditText duration = new EditText(this);
        duration.setHint("Süre · örn. 60 dk");
        duration.setSingleLine(true);
        box.addView(duration);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Yeni plan")
                .setView(box)
                .setNegativeButton("Vazgeç", null)
                .setPositiveButton("Ekle", null)
                .create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override public void onShow(DialogInterface dialogInterface) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
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

                        Plan plan = new Plan(
                                System.currentTimeMillis(),
                                todayKey(),
                                name,
                                type,
                                t,
                                dur,
                                false
                        );

                        plans.add(plan);
                        PlanStore.save(MainActivity.this, plans);

                        if (Plan.HOURLY.equals(type)) {
                            scheduleReminder(plan);
                        }

                        dialog.dismiss();
                        view.invalidate();
                    }
                });
            }
        });

        dialog.show();
    }

    private void scheduleReminder(Plan plan) {
        if (plan.time == null || !plan.time.matches("\\d{1,2}:\\d{2}")) {
            return;
        }

        try {
            String[] parts = plan.time.split(":");
            Calendar alarm = Calendar.getInstance();
            alarm.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
            alarm.set(Calendar.MINUTE, Integer.parseInt(parts[1]));
            alarm.set(Calendar.SECOND, 0);
            alarm.set(Calendar.MILLISECOND, 0);

            if (alarm.getTimeInMillis() <= System.currentTimeMillis()) {
                return;
            }

            Intent intent = new Intent(this, ReminderReceiver.class);
            intent.putExtra("title", plan.title);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    (int) (plan.id & 0x7fffffff),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
            manager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    alarm.getTimeInMillis(),
                    pendingIntent
            );
        } catch (Exception ignored) {
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private class RotaView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float density;
        private int tab = 0;
        private final RectF fab = new RectF();
        private final RectF[] taskRects = new RectF[80];

        RotaView(Context context) {
            super(context);
            density = getResources().getDisplayMetrics().density;
            setBackgroundColor(Color.rgb(246, 246, 248));
        }

        float d(float n) {
            return n * density;
        }

        void text(Canvas canvas, String s, float x, float y, float size, int color, boolean bold) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color);
            paint.setTextSize(d(size));
            paint.setTypeface(android.graphics.Typeface.create(
                    "sans",
                    bold ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL
            ));
            canvas.drawText(s, d(x), d(y), paint);
        }

        void round(Canvas canvas, float l, float t, float r, float b, float radius, int color) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color);
            canvas.drawRoundRect(
                    new RectF(d(l), d(t), d(r), d(b)),
                    d(radius),
                    d(radius),
                    paint
            );
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            float width = getWidth() / density;
            float height = getHeight() / density;

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(246, 246, 248));
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);

            if (tab == 0) {
                drawToday(canvas, width, height);
            } else if (tab == 1) {
                drawPlan(canvas, width, height);
            } else {
                drawAssistant(canvas, width, height);
            }

            drawBottom(canvas, width, height);
            drawFab(canvas, width, height);
        }

        void drawHeader(Canvas canvas, String title, String subtitle) {
            text(canvas, "ROTA", 24, 31, 12, Color.rgb(128, 128, 134), true);
            text(canvas, title, 24, 72, 38, Color.rgb(22, 23, 26), true);
            if (subtitle != null) {
                text(canvas, subtitle, 24, 98, 14, Color.rgb(110, 111, 118), false);
            }
        }

        List<Plan> todayPlans() {
            List<Plan> result = new ArrayList<>();
            String key = todayKey();

            for (Plan plan : plans) {
                if (key.equals(plan.date)) {
                    result.add(plan);
                }
            }

            Collections.sort(result, new Comparator<Plan>() {
                @Override public int compare(Plan a, Plan b) {
                    if (a.time.isEmpty() && b.time.isEmpty()) {
                        return a.title.compareToIgnoreCase(b.title);
                    }
                    if (a.time.isEmpty()) return 1;
                    if (b.time.isEmpty()) return -1;
                    return a.time.compareTo(b.time);
                }
            });

            return result;
        }

        void drawToday(Canvas canvas, float width, float height) {
            drawHeader(canvas, "Bugün", prettyDate());

            List<Plan> today = todayPlans();
            int total = today.size();
            int done = 0;
            Plan next = null;

            for (Plan plan : today) {
                if (plan.done) done++;
                if (next == null && !plan.done) next = plan;
            }

            round(canvas, 20, 116, width - 20, 190, 24, Color.WHITE);
            text(canvas, "Bugünün akışı", 38, 143, 13, Color.rgb(116,117,124), true);
            text(canvas, done + " / " + total, 38, 171, 25, Color.rgb(22,23,26), true);
            text(canvas, "tamamlandı", 96, 171, 14, Color.rgb(116,117,124), false);

            round(canvas, 220, 140, width - 38, 156, 8, Color.rgb(235,235,239));

            if (total > 0) {
                float progress = (width - 258) * (done / (float) total);
                round(canvas, 220, 140, 220 + progress, 156, 8, Color.rgb(22,23,26));
            }

            float y = 214;

            if (next != null) {
                round(canvas, 20, y, width - 20, y + 76, 22, Color.rgb(22,23,26));
                text(canvas, "SIRADAKİ", 38, y + 25, 11, Color.rgb(180,181,187), true);
                text(canvas, next.title, 38, y + 50, 19, Color.WHITE, true);

                String meta = Plan.HOURLY.equals(next.type)
                        ? next.time + " · " + next.duration
                        : next.type;

                text(canvas, meta, width - 160, y + 50, 12, Color.rgb(205,206,211), false);
                y += 92;
            }

            text(canvas, "Bugünün planı", 24, y + 4, 16, Color.rgb(22,23,26), true);
            y += 22;

            if (today.isEmpty()) {
                round(canvas, 20, y, width - 20, y + 104, 22, Color.WHITE);
                text(canvas, "Bugün için henüz plan yok.", 38, y + 44, 16, Color.rgb(22,23,26), true);
                text(canvas, "＋ ile ilk planını ekle.", 38, y + 70, 13, Color.rgb(116,117,124), false);
                return;
            }

            int index = 0;

            for (Plan plan : today) {
                if (index >= taskRects.length) break;

                RectF rect = new RectF(d(20), d(y), d(width - 20), d(y + 66));
                taskRects[index] = rect;

                round(canvas, 20, y, width - 20, y + 66, 18, Color.WHITE);

                String left = Plan.HOURLY.equals(plan.type) ? plan.time : " ";
                text(canvas, left, 38, y + 27, 12, Color.rgb(122,123,130), true);
                text(canvas, plan.title, 86, y + 27, 16,
                        plan.done ? Color.rgb(150,151,157) : Color.rgb(22,23,26), true);

                String meta = Plan.HOURLY.equals(plan.type) ? plan.duration : plan.type;
                text(canvas, meta, 86, y + 48, 12, Color.rgb(122,123,130), false);

                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(d(2));
                paint.setColor(plan.done ? Color.rgb(22,23,26) : Color.rgb(190,191,197));
                canvas.drawCircle(d(width - 44), d(y + 33), d(11), paint);

                if (plan.done) {
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(Color.rgb(22,23,26));
                    canvas.drawCircle(d(width - 44), d(y + 33), d(11), paint);
                    text(canvas, "✓", width - 50, y + 39, 13, Color.WHITE, true);
                }

                index++;
                y += 76;
            }
        }

        void drawPlan(Canvas canvas, float width, float height) {
            drawHeader(canvas, "Plan", "Önümüzdeki günler");

            round(canvas, 20, 116, width - 20, 166, 20, Color.WHITE);
            text(canvas, "←", 38, 148, 22, Color.rgb(22,23,26), true);
            text(canvas, "Eylül 2026", width / 2 - 46, 148, 16, Color.rgb(22,23,26), true);
            text(canvas, "→", width - 58, 148, 22, Color.rgb(22,23,26), true);

            String[] days = {"Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz"};

            for (int i = 0; i < 7; i++) {
                float x = 20 + i * ((width - 40) / 7f);
                text(canvas, days[i], x + 5, 196, 11, Color.rgb(126,127,133), true);
            }

            List<Plan> sorted = new ArrayList<>(plans);
            Collections.sort(sorted, new Comparator<Plan>() {
                @Override public int compare(Plan a, Plan b) {
                    return a.date.compareTo(b.date);
                }
            });

            float y = 224;
            int count = 0;

            for (Plan plan : sorted) {
                if (count++ > 7) break;

                round(canvas, 20, y, width - 20, y + 58, 18, Color.WHITE);
                text(canvas, plan.date, 36, y + 23, 11, Color.rgb(126,127,133), true);
                text(canvas, plan.title, 104, y + 24, 15, Color.rgb(22,23,26), true);
                text(canvas, plan.type, 104, y + 45, 12, Color.rgb(126,127,133), false);
                y += 68;
            }

            if (sorted.isEmpty()) {
                round(canvas, 20, y, width - 20, y + 94, 20, Color.WHITE);
                text(canvas, "Takvim henüz boş.", 38, y + 40, 16, Color.rgb(22,23,26), true);
                text(canvas, "Plan ekledikçe burada görünecek.", 38, y + 66, 13, Color.rgb(126,127,133), false);
            }
        }

        void drawAssistant(Canvas canvas, float width, float height) {
            drawHeader(canvas, "Asistan", "Planların için");

            round(canvas, 20, 116, width - 20, 208, 24, Color.WHITE);
            text(canvas, "Yarını birlikte kuralım.", 38, 148, 21, Color.rgb(22,23,26), true);
            text(canvas, "İş, okul, ibadet ve kişisel zamanı", 38, 173, 13, Color.rgb(112,113,120), false);
            text(canvas, "tek planda dengeleyebilirim.", 38, 192, 13, Color.rgb(112,113,120), false);

            round(canvas, 20, 230, width - 20, 286, 18, Color.rgb(22,23,26));
            text(canvas, "Yarını planla", 40, 264, 15, Color.WHITE, true);

            round(canvas, 20, 304, width - 20, 362, 18, Color.WHITE);
            text(canvas, "Şimdi ne yapmalıyım?", 40, 339, 15, Color.rgb(22,23,26), true);

            text(canvas, "Örnek", 24, 396, 12, Color.rgb(126,127,133), true);
            text(canvas, "“Yarın 14–22 çalışıyorum.", 24, 424, 14, Color.rgb(22,23,26), false);
            text(canvas, "Sabah Risale ve spor istiyorum.”", 24, 446, 14, Color.rgb(22,23,26), false);
        }

        void drawBottom(Canvas canvas, float width, float height) {
            float top = height - 76;

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            canvas.drawRect(0, d(top), getWidth(), getHeight(), paint);

            String[] labels = {"Bugün", "Plan", "Asistan"};
            String[] icons = {"⌂", "▦", "✦"};

            for (int i = 0; i < 3; i++) {
                float centerX = width * (i + 0.5f) / 3f;

                if (tab == i) {
                    round(canvas, centerX - 54, top + 10, centerX + 54, top + 66, 18, Color.rgb(242,242,246));
                }

                text(canvas, icons[i], centerX - 7, top + 31, 19,
                        tab == i ? Color.rgb(22,23,26) : Color.rgb(142,143,150), false);

                text(canvas, labels[i], centerX - 27, top + 56, 12,
                        tab == i ? Color.rgb(22,23,26) : Color.rgb(142,143,150), true);
            }
        }

        void drawFab(Canvas canvas, float width, float height) {
            float centerX = width - 58;
            float centerY = height - 108;

            fab.set(d(centerX - 30), d(centerY - 30), d(centerX + 30), d(centerY + 30));
            round(canvas, centerX - 30, centerY - 30, centerX + 30, centerY + 30, 19, Color.rgb(22,23,26));
            text(canvas, "+", centerX - 12, centerY + 10, 35, Color.WHITE, false);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() != MotionEvent.ACTION_UP) return true;

            float x = event.getX() / density;
            float y = event.getY() / density;
            float height = getHeight() / density;
            float width = getWidth() / density;

            if (y > height - 78) {
                if (x < width / 3f) {
                    tab = 0;
                } else if (x < 2 * width / 3f) {
                    tab = 1;
                } else {
                    tab = 2;
                }
                invalidate();
                return true;
            }

            if (fab.contains(d(x), d(y))) {
                showAddDialog();
                return true;
            }

            if (tab == 0) {
                List<Plan> today = todayPlans();

                for (int i = 0; i < today.size() && i < taskRects.length; i++) {
                    RectF rect = taskRects[i];

                    if (rect != null && rect.contains(d(x), d(y))) {
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
