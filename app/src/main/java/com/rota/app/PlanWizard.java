package com.rota.app;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;

public final class PlanWizard {
    public interface Callback { void saved(); }

    private static final int BG = Color.rgb(10, 12, 16);
    private static final int SURFACE = Color.rgb(18, 22, 29);
    private static final int SURFACE2 = Color.rgb(25, 30, 38);
    private static final int SURFACE3 = Color.rgb(31, 37, 47);
    private static final int TEXT = Color.rgb(247, 248, 250);
    private static final int MUTED = Color.rgb(145, 153, 165);
    private static final int BORDER = Color.rgb(40, 47, 58);
    private static final int ORANGE = Color.rgb(255, 139, 0);
    private static final Locale TR = new Locale("tr", "TR");

    private static final String[][] CATS = {
            {"📚", "Kitap / Okuma", "book"},
            {"💼", "İş", "work"},
            {"🎬", "Video Edit", "video"},
            {"🏋️", "Spor", "sport"},
            {"🕌", "İbadet", "faith"},
            {"📍", "Randevu", "appointment"},
            {"🍳", "Yemek", "food"},
            {"•", "Kişisel", "personal"}
    };

    private static final class Draft {
        String category = "personal";
        String date;
        String type = Plan.HOURLY;
        String title = "";
        String start = "";
        String end = "";
        String repeat = Plan.REPEAT_NONE;
        String note = "";
        JSONObject details = new JSONObject();
        Draft(String date) { this.date = date; }
    }

    private static final class Field {
        final String key;
        final String hint;
        final boolean multi;
        Field(String key, String hint, boolean multi) {
            this.key = key; this.hint = hint; this.multi = multi;
        }
    }

    private PlanWizard() {}

    public static void show(Activity a, List<Plan> plans, String date, Callback cb) {
        showEditor(a, plans, new Draft(date), null, cb);
    }

    public static void showEdit(Activity a, List<Plan> plans, Plan existing, Callback cb) {
        Draft d = new Draft(existing.date);
        d.category = existing.category == null ? "personal" : existing.category;
        d.type = existing.type == null ? Plan.HOURLY : existing.type;
        d.title = existing.title == null ? "" : existing.title;
        d.start = existing.startTime == null ? "" : existing.startTime;
        d.end = existing.endTime == null ? "" : existing.endTime;
        d.repeat = existing.repeat == null ? Plan.REPEAT_NONE : existing.repeat;
        d.note = existing.note == null ? "" : existing.note;
        try { d.details = new JSONObject(existing.details == null || existing.details.isEmpty() ? "{}" : existing.details); }
        catch (Exception ignored) { d.details = new JSONObject(); }
        showEditor(a, plans, d, existing, cb);
    }

    private static void showEditor(Activity a, List<Plan> plans, Draft d, Plan editing, Callback cb) {
        showCategoryStep(a, plans, d, editing, cb);
    }

    private static void showCategoryStep(Activity a, List<Plan> plans, Draft d, Plan editing, Callback cb) {
        if (editing != null) {
            showDetailsStep(a, plans, d, editing, cb);
            return;
        }

        Dialog dialog = new Dialog(a);
        LinearLayout box = box(a);
        header(a, box, "Yeni plan", "1 / 3  ·  Önce ne yapmak istediğini seç.", dialog);

        GridLayout grid = new GridLayout(a);
        grid.setColumnCount(2);
        for (String[] c : CATS) {
            LinearLayout card = categoryCard(a, c[0], c[1], c[2].equals(d.category));
            GridLayout.LayoutParams gp = new GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED, 1f),
                    GridLayout.spec(GridLayout.UNDEFINED, 1f));
            gp.width = 0;
            gp.height = dp(a, 78);
            gp.setMargins(dp(a, 4), dp(a, 4), dp(a, 4), dp(a, 4));
            grid.addView(card, gp);

            card.setOnClickListener(v -> {
                d.category = c[2];
                dialog.dismiss();
                showDetailsStep(a, plans, d, null, cb);
            });
        }

        box.addView(grid, mt(a, 10));
        dialog.setContentView(box);
        dialog.show();
        fit(dialog, a);
    }

    private static void showDetailsStep(Activity a, List<Plan> plans, Draft d, Plan editing, Callback cb) {
        Dialog dialog = new Dialog(a);
        ScrollView scroll = new ScrollView(a);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);

        LinearLayout box = box(a);
        header(a, box, "Detaylar", "2 / 3  ·  " + labelFor(d.category) + " için gerekli bilgiler.", dialog);

        EditText title = input(a, "Plan adı · örn. Diş doktoru / Risale okumak");
        title.setText(d.title == null ? "" : d.title);
        title.setTextSize(16);
        box.addView(textLabel(a, "PLAN ADI"), mt(a, 15));
        box.addView(title, mt(a, 7));

        box.addView(textLabel(a, "KATEGORİYE ÖZEL"), mt(a, 16));

        final List<EditText> fields = new ArrayList<>();
        final List<EditText> stages = new ArrayList<>();
        final LinearLayout details = col(a);
        box.addView(details, mt(a, 6));

        rebuildCategoryDetails(a, d, details, fields, stages);

        LinearLayout actions = row(a);
        TextView back = button(a, "‹  Geri", MUTED, SURFACE2);
        TextView next = button(a, "Devam et  ›", Color.WHITE, ORANGE);
        actions.addView(back, weight(a, .34f, 52));
        gap(actions, a, 8);
        actions.addView(next, weight(a, .66f, 52));
        box.addView(actions, mt(a, 14));

        back.setOnClickListener(v -> {
            dialog.dismiss();
            if (editing == null) showCategoryStep(a, plans, d, null, cb);
            else dialog.dismiss();
        });

        next.setOnClickListener(v -> {
            String t = title.getText().toString().trim();
            if (t.isEmpty()) {
                title.setError("Plan adı gerekli");
                return;
            }
            d.title = t;
            try {
                d.details = new JSONObject(buildDetails(d.category, fields, stages, d.details));
            } catch (Exception ignored) {
                d.details = new JSONObject();
            }
            dialog.dismiss();
            showTimingStep(a, plans, d, editing, cb);
        });

        scroll.addView(box);
        dialog.setContentView(scroll);
        dialog.show();
        fit(dialog, a);
    }

    private static void rebuildCategoryDetails(Activity a, Draft d, LinearLayout details,
                                               List<EditText> fields, List<EditText> stages) {
        details.removeAllViews();
        fields.clear();
        stages.clear();

        if ("video".equals(d.category)) {
            details.addView(textLabel(a, "PROJE"), mt(a, 4));
            EditText project = input(a, "Video / proje adı");
            project.setText(readDetail(d.details, "project", 0));
            details.addView(project, mt(a, 6));
            fields.add(project);

            details.addView(textLabel(a, "AŞAMALAR"), mt(a, 12));
            LinearLayout stageBox = col(a);
            JSONArray saved = detailArray(d.details, "stages");
            if (saved != null && saved.length() > 0) {
                for (int i = 0; i < saved.length(); i++) {
                    addStage(a, stageBox, stages, saved.optString(i, ""), 0);
                }
            } else {
                addStage(a, stageBox, stages, "Kaba kurgu", 0);
                addStage(a, stageBox, stages, "İnce kurgu", 0);
                addStage(a, stageBox, stages, "Yazılar / altyazı", 0);
                addStage(a, stageBox, stages, "Müzik / ses", 0);
            }
            details.addView(stageBox);

            TextView add = button(a, "＋  Aşama ekle", ORANGE, SURFACE2);
            details.addView(add, mt(a, 7));
            add.setOnClickListener(v -> addStage(a, stageBox, stages, "", 0));

            details.addView(textLabel(a, "EK BİLGİ"), mt(a, 12));
            EditText extra = multiInput(a, "İstediğin ek bilgiyi buraya yaz...");
            extra.setText(readDetail(d.details, "extra", 1));
            details.addView(extra, mt(a, 6));
            fields.add(extra);
            return;
        }

        Field[] defs = fieldsFor(d.category);
        for (int i = 0; i < defs.length; i++) {
            Field f = defs[i];
            EditText e = f.multi ? multiInput(a, f.hint) : input(a, f.hint);
            e.setText(readDetail(d.details, f.key, i));
            details.addView(e, mt(a, i == 0 ? 4 : 8));
            fields.add(e);
        }
    }

    private static void showTimingStep(Activity a, List<Plan> plans, Draft d, Plan editing, Callback cb) {
        Dialog dialog = new Dialog(a);
        LinearLayout box = box(a);
        header(a, box, "Zaman", "3 / 3  ·  Tarih, saat ve tekrar.", dialog);

        final String[] date = {d.date};
        final String[] start = {d.start};
        final String[] end = {d.end};
        final String[] type = {d.type == null ? Plan.HOURLY : d.type};
        final String[] repeat = {d.repeat == null ? Plan.REPEAT_NONE : d.repeat};

        box.addView(textLabel(a, "TARİH"), mt(a, 15));
        TextView dateBtn = choice(a, pretty(date[0]));
        box.addView(dateBtn, mt(a, 6));

        dateBtn.setOnClickListener(v -> pickDate(a, date[0], picked -> {
            date[0] = picked;
            dateBtn.setText(pretty(picked));
        }));

        box.addView(textLabel(a, "PLAN TÜRÜ"), mt(a, 15));
        LinearLayout typeRow = row(a);
        TextView hourly = button(a, "Saatli", Plan.HOURLY.equals(type[0]) ? Color.WHITE : TEXT, Plan.HOURLY.equals(type[0]) ? ORANGE : SURFACE2);
        TextView flex = button(a, "Esnek", Plan.FLEX.equals(type[0]) ? Color.WHITE : TEXT, Plan.FLEX.equals(type[0]) ? ORANGE : SURFACE2);
        TextView allDay = button(a, "Tüm gün", Plan.ALL_DAY.equals(type[0]) ? Color.WHITE : TEXT, Plan.ALL_DAY.equals(type[0]) ? ORANGE : SURFACE2);
        typeRow.addView(hourly, weight(a, 1f, 48));
        gap(typeRow, a, 7);
        typeRow.addView(flex, weight(a, 1f, 48));
        gap(typeRow, a, 7);
        typeRow.addView(allDay, weight(a, 1f, 48));
        box.addView(typeRow, mt(a, 6));

        LinearLayout times = row(a);
        TextView startBtn = choice(a, start[0].isEmpty() ? "Başlangıç" : start[0]);
        TextView endBtn = choice(a, end[0].isEmpty() ? "Bitiş · isteğe bağlı" : end[0]);
        times.addView(startBtn, weight(a, 1f, 62));
        gap(times, a, 8);
        times.addView(endBtn, weight(a, 1f, 62));
        box.addView(times, mt(a, 9));
        times.setVisibility(Plan.ALL_DAY.equals(type[0]) ? View.GONE : View.VISIBLE);

        startBtn.setOnClickListener(v -> timePicker(a, start[0], value -> {
            start[0] = value;
            startBtn.setText(value);
        }));
        endBtn.setOnClickListener(v -> timePicker(a, end[0], value -> {
            end[0] = value;
            endBtn.setText(value);
        }));

        View.OnClickListener typeListener = v -> {
            if (v == hourly) type[0] = Plan.HOURLY;
            else if (v == flex) type[0] = Plan.FLEX;
            else type[0] = Plan.ALL_DAY;
            setChip(hourly, Plan.HOURLY.equals(type[0]));
            setChip(flex, Plan.FLEX.equals(type[0]));
            setChip(allDay, Plan.ALL_DAY.equals(type[0]));
            times.setVisibility(Plan.ALL_DAY.equals(type[0]) ? View.GONE : View.VISIBLE);
            if (Plan.ALL_DAY.equals(type[0])) {
                start[0] = "";
                end[0] = "";
                startBtn.setText("Başlangıç");
                endBtn.setText("Bitiş · isteğe bağlı");
            }
        };
        hourly.setOnClickListener(typeListener);
        flex.setOnClickListener(typeListener);
        allDay.setOnClickListener(typeListener);

        box.addView(textLabel(a, "TEKRAR"), mt(a, 15));
        TextView repeatBtn = choice(a, repeatLabel(repeat[0]));
        box.addView(repeatBtn, mt(a, 6));
        repeatBtn.setOnClickListener(v -> chooseRepeat(a, repeat[0], value -> {
            repeat[0] = value;
            repeatBtn.setText(repeatLabel(value));
        }));

        box.addView(textLabel(a, "NOT"), mt(a, 15));
        EditText note = multiInput(a, "Bu plan için ek bilgi...");
        note.setText(d.note == null ? "" : d.note);
        box.addView(note, mt(a, 6));

        box.addView(textHint(a, "Hatırlatma süresi Ayarlar bölümündeki global ayardan kontrol edilir."),
                mt(a, 8));

        LinearLayout actions = row(a);
        TextView back = button(a, "‹  Geri", MUTED, SURFACE2);
        TextView create = button(a, editing == null ? "✓  Planı oluştur" : "✓  Kaydet",
                Color.WHITE, ORANGE);
        actions.addView(back, weight(a, .34f, 54));
        gap(actions, a, 8);
        actions.addView(create, weight(a, .66f, 54));
        box.addView(actions, mt(a, 15));

        back.setOnClickListener(v -> {
            dialog.dismiss();
            showDetailsStep(a, plans, d, editing, cb);
        });

        create.setOnClickListener(v -> {
            if (Plan.HOURLY.equals(type[0]) && start[0].isEmpty()) {
                startBtn.setError("Başlangıç saatini seç");
                return;
            }
            if (Plan.HOURLY.equals(type[0]) && !end[0].isEmpty() && start[0].equals(end[0])) {
                endBtn.setError("Bitiş saati başlangıçla aynı olamaz");
                return;
            }

            d.date = validDate(date[0]) ? date[0] : key(Calendar.getInstance());
            d.start = start[0];
            d.end = end[0];
            d.type = type[0];
            d.repeat = repeat[0];
            d.note = note.getText().toString().trim();

            if (editing == null) {
                Plan p = new Plan(PlanStore.nextId(plans), d.date, d.title, d.type, d.start,
                        Plan.durationBetween(d.start, d.end), false);
                p.startTime = d.start;
                p.endTime = d.end;
                p.time = d.start;
                p.repeat = d.repeat;
                p.category = d.category;
                p.note = d.note;
                p.details = d.details == null ? "" : d.details.toString();
                p.status = Plan.STATUS_PLANNED;
                PlanStore.addWithRepeat(plans, p);
            } else {
                editing.date = d.date;
                editing.title = d.title;
                editing.type = d.type;
                editing.startTime = d.start;
                editing.endTime = d.end;
                editing.time = d.start;
                editing.duration = Plan.durationBetween(d.start, d.end);
                editing.repeat = d.repeat;
                editing.category = d.category;
                editing.note = d.note;
                editing.details = d.details == null ? "" : d.details.toString();
                editing.status = Plan.STATUS_PLANNED;
                editing.done = false;
            }

            PlanStore.save(a, plans);
            ReminderScheduler.rescheduleAll(a, plans);
            dialog.dismiss();
            if (cb != null) cb.saved();
        });

        dialog.setContentView(box);
        dialog.show();
        fit(dialog, a);
    }

    private static TextView textLabel(Activity a, String s) {
        return txt(a, s, 10, MUTED, true);
    }

    private static String labelFor(String k) {
        if ("book".equals(k)) return "Kitap / Okuma";
        if ("work".equals(k)) return "İş";
        if ("video".equals(k)) return "Video Edit";
        if ("sport".equals(k)) return "Spor";
        if ("faith".equals(k)) return "İbadet";
        if ("appointment".equals(k)) return "Randevu";
        if ("food".equals(k)) return "Yemek";
        return "Kişisel";
    }

    private static String buildDetails(String category, List<EditText> fields,
                                       List<EditText> stages, JSONObject previous) {
        JSONObject out = new JSONObject();
        try {
            if ("video".equals(category)) {
                if (fields.size() > 0) out.put("project", fields.get(0).getText().toString().trim());
                if (fields.size() > 1) out.put("extra", fields.get(1).getText().toString().trim());
                JSONArray a = new JSONArray();
                for (EditText e : stages) {
                    String s = e.getText().toString().trim();
                    if (!s.isEmpty()) a.put(s);
                }
                out.put("stages", a);
            } else {
                Field[] defs = fieldsFor(category);
                for (int i = 0; i < fields.size() && i < defs.length; i++)
                    out.put(defs[i].key, fields.get(i).getText().toString().trim());
            }
        } catch (Exception ignored) {}
        return out.toString();
    }

    private static Field[] fieldsFor(String k) {
        if ("book".equals(k)) return new Field[]{
                new Field("book", "Kitap adı", false),
                new Field("currentPage", "Şu an hangi sayfadasın?", false),
                new Field("pagesToday", "Bugün kaç sayfa okuyacaksın?", false)
        };
        if ("work".equals(k)) return new Field[]{
                new Field("workplace", "İş yeri / proje", false),
                new Field("break", "Mola süresi · örn. 30 dk", false),
                new Field("schedule", "Çalışma düzeni · örn. 14:00–22:00", false),
                new Field("extra", "Ek bilgi", true)
        };
        if ("sport".equals(k)) return new Field[]{
                new Field("sport", "Spor türü", false),
                new Field("goal", "Hedef / süre", false),
                new Field("extra", "Ek bilgi", true)
        };
        if ("faith".equals(k)) return new Field[]{
                new Field("target", "İbadet / hedef", false),
                new Field("detail", "Kitap / sure / detay", false),
                new Field("amount", "Hedef miktar / süre", false)
        };
        if ("appointment".equals(k)) return new Field[]{
                new Field("appointment", "Randevu", false),
                new Field("where", "Nerede / kiminle?", false),
                new Field("travel", "Yol için kaç dakika ayırmalıyım?", false),
                new Field("extra", "Not", true)
        };
        if ("food".equals(k)) return new Field[]{
                new Field("meal", "Yemek / hazırlık", false),
                new Field("people", "Miktar / kaç kişilik", false),
                new Field("extra", "Ek bilgi", true)
        };
        return new Field[]{new Field("task", "Görev", false), new Field("extra", "Ek bilgi", true)};
    }

    private static String readDetail(JSONObject o, String key, int fallbackIndex) {
        String value = o == null ? "" : o.optString(key, "");
        if (!value.isEmpty()) return value;
        return o == null ? "" : o.optString("field" + fallbackIndex, "");
    }

    private static JSONArray detailArray(JSONObject o, String key) {
        if (o == null) return null;
        JSONArray a = o.optJSONArray(key);
        return a;
    }

    private static void addStage(Activity a, LinearLayout box, List<EditText> stages, String value, int unused) {
        LinearLayout r = row(a);
        EditText e = input(a, "Aşama");
        e.setText(value);
        r.addView(e, weight(a, 1, 48));
        TextView del = button(a, "×", MUTED, SURFACE2);
        r.addView(del, lp(a, 46, 48));
        box.addView(r, mt(a, 6));
        stages.add(e);
        del.setOnClickListener(v -> {
            stages.remove(e);
            box.removeView(r);
        });
    }

    private interface DateDone { void value(String date); }
    private static void pickDate(Activity a, String old, DateDone done) {
        Calendar c = parse(old);
        DatePickerDialog d = new DatePickerDialog(a, (DatePicker v, int y, int m, int day) -> {
            Calendar out = Calendar.getInstance();
            out.set(y, m, day);
            done.value(key(out));
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        d.show();
    }

    private interface TimeDone { void value(String time); }
    private static void timePicker(Activity a, String old, TimeDone done) {
        Dialog d = new Dialog(a);
        LinearLayout b = box(a);
        header(a, b, "Saat seç", "Kaydır, ortadaki değeri seç ve onayla.", d);

        Calendar c = Calendar.getInstance();
        if (old != null && old.matches("\\d{2}:\\d{2}")) {
            c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(old.substring(0, 2)));
            c.set(Calendar.MINUTE, Integer.parseInt(old.substring(3)));
        }
        final int[] hm = {c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)};
        TextView preview = txt(a, fmt(hm), 38, TEXT, true);
        preview.setGravity(Gravity.CENTER);
        preview.setBackground(round(SURFACE3, 20));
        b.addView(preview, mt(a, 10));

        LinearLayout wheels = row(a);
        Wheel hour = new Wheel(a, 24, hm[0], value -> {
            hm[0] = value; preview.setText(fmt(hm));
        });
        Wheel minute = new Wheel(a, 60, hm[1], value -> {
            hm[1] = value; preview.setText(fmt(hm));
        });
        wheels.addView(hour, weight(a, 1, 210));
        TextView colon = txt(a, ":", 32, TEXT, true);
        colon.setGravity(Gravity.CENTER);
        wheels.addView(colon, lp(a, 28, 80));
        wheels.addView(minute, weight(a, 1, 210));
        b.addView(wheels, mt(a, 8));

        LinearLayout acts = row(a);
        TextView cancel = button(a, "Vazgeç", MUTED, SURFACE2);
        TextView ok = button(a, "Saati seç", Color.WHITE, ORANGE);
        acts.addView(cancel, weight(a, 1, 50)); gap(acts, a, 8); acts.addView(ok, weight(a, 1, 50));
        b.addView(acts, mt(a, 10));

        cancel.setOnClickListener(v -> d.dismiss());
        ok.setOnClickListener(v -> { done.value(fmt(hm)); d.dismiss(); });
        d.setContentView(b);
        d.show();
        fitSmall(d, a, .92f, .62f);
    }

    private static void chooseRepeat(Activity a, String current, TimeDone done) {
        Dialog d = new Dialog(a);
        LinearLayout b = box(a);
        header(a, b, "Tekrar", "Plan tek sefer mi, düzenli mi?", d);
        String[] labels = {"Tek sefer", "Her gün", "Hafta içi", "Her hafta"};
        String[] values = {Plan.REPEAT_NONE, Plan.REPEAT_DAILY, Plan.REPEAT_WEEKDAYS, Plan.REPEAT_WEEKLY};
        for (int i = 0; i < labels.length; i++) {
            final int n = i;
            TextView v = button(a, labels[i],
                    values[i].equals(current) ? Color.WHITE : TEXT,
                    values[i].equals(current) ? ORANGE : SURFACE2);
            b.addView(v, mt(a, 7));
            v.setOnClickListener(x -> { done.value(values[n]); d.dismiss(); });
        }
        d.setContentView(b);
        d.show();
        fitSmall(d, a, .88f, .58f);
    }

    private static class Wheel extends View {
        private final android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final int max;
        private int value;
        private float offset;
        private float lastY;
        private final ValueChanged callback;

        Wheel(Activity a, int max, int initial, ValueChanged callback) {
            super(a);
            this.max = max; this.value = initial; this.callback = callback;
            setBackground(round(SURFACE2, 20));
            setFocusable(true);
        }

        private int rowHeight() { return dp(getContext(), 52); }

        @Override protected void onDraw(android.graphics.Canvas canvas) {
            super.onDraw(canvas);
            float mid = getHeight() / 2f;
            float rh = rowHeight();
            for (int i = -2; i <= 2; i++) {
                float y = mid + i * rh + offset;
                float dist = Math.abs(y - mid);
                boolean selected = dist < rh * .46f;
                paint.setColor(selected ? TEXT : MUTED);
                paint.setTypeface(Typeface.create("sans", selected ? Typeface.BOLD : Typeface.NORMAL));
                paint.setTextSize(dp(getContext(), selected ? 30 : 23));
                String s = String.format(Locale.GERMANY, "%02d", wrap(value + i));
                canvas.drawText(s, getWidth() / 2f - paint.measureText(s) / 2f,
                        y - (paint.ascent() + paint.descent()) / 2f, paint);
            }
            paint.setColor(ORANGE);
            paint.setStrokeWidth(dp(getContext(), 2));
            canvas.drawLine(dp(getContext(), 12), mid - rh / 2, getWidth() - dp(getContext(), 12), mid - rh / 2, paint);
            canvas.drawLine(dp(getContext(), 12), mid + rh / 2, getWidth() - dp(getContext(), 12), mid + rh / 2, paint);
        }

        private void step(int delta) {
            value = wrap(value + delta);
            callback.changed(value);
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            invalidate();
        }

        private void consume() {
            int rh = rowHeight();
            while (offset <= -rh) { offset += rh; step(1); }
            while (offset >= rh) { offset -= rh; step(-1); }
        }

        private void snap() {
            int rh = rowHeight();
            if (Math.abs(offset) > rh / 2f) step(offset < 0 ? 1 : -1);
            android.animation.ValueAnimator anim = android.animation.ValueAnimator.ofFloat(offset, 0f);
            anim.setDuration(140);
            anim.addUpdateListener(v -> { offset = (float) v.getAnimatedValue(); invalidate(); });
            anim.start();
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                lastY = e.getY();
                return true;
            }
            if (e.getActionMasked() == MotionEvent.ACTION_MOVE) {
                float dy = e.getY() - lastY;
                lastY = e.getY();
                offset += dy;
                consume();
                invalidate();
                return true;
            }
            if (e.getActionMasked() == MotionEvent.ACTION_UP || e.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                snap();
                return true;
            }
            return true;
        }

        private int wrap(int n) {
            int x = n % max;
            return x < 0 ? x + max : x;
        }
    }

    private interface ValueChanged { void changed(int value); }

    private static LinearLayout categoryCard(Activity a, String icon, String name, boolean selected) {
        LinearLayout c = col(a);
        c.setGravity(Gravity.CENTER_VERTICAL);
        c.setPadding(dp(a, 12), dp(a, 8), dp(a, 12), dp(a, 8));
        c.setBackground(round(selected ? ORANGE : SURFACE, 18));
        TextView i = txt(a, icon, 19, selected ? Color.WHITE : ORANGE, false);
        c.addView(i);
        c.addView(txt(a, name, 11, selected ? Color.WHITE : TEXT, true), mt(a, 6));
        return c;
    }

    private static void setChip(TextView t, boolean selected) {
        t.setTextColor(selected ? Color.WHITE : MUTED);
        t.setBackground(round(selected ? ORANGE : SURFACE2, 16));
    }

    private static TextView choice(Activity a, String s) {
        TextView t = txt(a, s, 13, TEXT, true);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(dp(a, 14), 0, dp(a, 14), 0);
        t.setBackground(round(SURFACE2, 16));
        return t;
    }

    private static EditText input(Activity a, String hint) {
        EditText e = new EditText(a);
        e.setSingleLine(true);
        e.setHint(hint);
        e.setHintTextColor(MUTED);
        e.setTextColor(TEXT);
        e.setTextSize(14);
        e.setPadding(dp(a, 13), 0, dp(a, 13), 0);
        e.setBackground(round(SURFACE2, 16));
        return e;
    }

    private static EditText multiInput(Activity a, String hint) {
        EditText e = input(a, hint);
        e.setSingleLine(false);
        e.setMinLines(3);
        e.setGravity(Gravity.TOP | Gravity.START);
        e.setPadding(dp(a, 13), dp(a, 11), dp(a, 13), dp(a, 11));
        return e;
    }

    private static TextView textHint(Activity a, String s) {
        return txt(a, s, 10, MUTED, false);
    }

    private static LinearLayout box(Activity a) {
        LinearLayout b = col(a);
        b.setPadding(dp(a, 18), dp(a, 17), dp(a, 18), dp(a, 18));
        b.setBackground(round(BG, 28));
        return b;
    }

    private static void header(Activity a, LinearLayout b, String title, String subtitle, Dialog d) {
        LinearLayout r = row(a);
        LinearLayout t = col(a);
        t.addView(txt(a, title, 23, TEXT, true));
        t.addView(txt(a, subtitle, 10, MUTED, false), mt(a, 4));
        r.addView(t, weight(a, 1, -2));
        TextView close = button(a, "×", TEXT, SURFACE2);
        r.addView(close, lp(a, 44, 44));
        close.setOnClickListener(v -> d.dismiss());
        b.addView(r);
    }

    private static LinearLayout col(Activity a) {
        LinearLayout l = new LinearLayout(a);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private static LinearLayout row(Activity a) {
        LinearLayout l = col(a);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private static TextView txt(Activity a, String s, float size, int color, boolean bold) {
        TextView t = new TextView(a);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setTypeface(Typeface.create("sans", bold ? Typeface.BOLD : Typeface.NORMAL));
        t.setIncludeFontPadding(false);
        return t;
    }

    private static TextView button(Activity a, String s, int fg, int bg) {
        TextView t = txt(a, s, 13, fg, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(a, 8), 0, dp(a, 8), 0);
        t.setBackground(round(bg, 16));
        return t;
    }

    private static LinearLayout.LayoutParams weight(Activity a, float w, int h) {
        return new LinearLayout.LayoutParams(0, h < 0 ? h : dp(a, h), w);
    }

    private static LinearLayout.LayoutParams lp(Activity a, int w, int h) {
        return new LinearLayout.LayoutParams(dp(a, w), dp(a, h));
    }

    private static LinearLayout.LayoutParams mt(Activity a, int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(a, top);
        return p;
    }

    private static void gap(LinearLayout l, Activity a, int w) {
        l.addView(new View(a), lp(a, w, 1));
    }

    private static int dp(Context c, int n) {
        return (int) (n * c.getResources().getDisplayMetrics().density + .5f);
    }

    private static GradientDrawable round(int color, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radius);
        return g;
    }

    private static void fit(Dialog d, Activity a) {
        Window w = d.getWindow();
        if (w == null) return;
        w.setBackgroundDrawableResource(android.R.color.transparent);
        w.setLayout((int) (a.getResources().getDisplayMetrics().widthPixels * .96f),
                (int) (a.getResources().getDisplayMetrics().heightPixels * .90f));
        w.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    private static void fitSmall(Dialog d, Activity a, double width, double height) {
        Window w = d.getWindow();
        if (w == null) return;
        w.setBackgroundDrawableResource(android.R.color.transparent);
        w.setLayout((int) (a.getResources().getDisplayMetrics().widthPixels * width),
                (int) (a.getResources().getDisplayMetrics().heightPixels * height));
    }

    private static Calendar parse(String key) {
        Calendar c = Calendar.getInstance();
        try {
            String[] p = key.split("-");
            c.set(Integer.parseInt(p[0]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[2]), 0, 0, 0);
            c.set(Calendar.MILLISECOND, 0);
        } catch (Exception ignored) {}
        return c;
    }

    private static boolean validDate(String key) {
        if (key == null || !key.matches("\\d{4}-\\d{2}-\\d{2}")) return false;
        try {
            Calendar c = parse(key);
            String normalized = key(c);
            return key.equals(normalized);
        } catch (Exception e) { return false; }
    }

    private static String key(Calendar c) {
        return String.format(Locale.GERMANY, "%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    private static String pretty(String date) {
        return new SimpleDateFormat("EEEE · d MMMM yyyy", TR).format(parse(date).getTime());
    }

    private static String fmt(int[] hm) {
        return String.format(Locale.GERMANY, "%02d:%02d", hm[0], hm[1]);
    }

    private static String repeatLabel(String s) {
        if (Plan.REPEAT_DAILY.equals(s)) return "Her gün";
        if (Plan.REPEAT_WEEKDAYS.equals(s)) return "Hafta içi";
        if (Plan.REPEAT_WEEKLY.equals(s)) return "Her hafta";
        return "Tek sefer";
    }
}
