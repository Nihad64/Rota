package com.rota.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public final class PlanStore {
    private static final String PREFS = "rota_store";
    private static final String KEY = "plans";

    private PlanStore() {}

    public static List<Plan> load(Context context) {
        List<Plan> list = new ArrayList<>();
        String raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]");
        try {
            JSONArray a = new JSONArray(raw);
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Plan p = new Plan(
                        o.optLong("id", System.currentTimeMillis() + i),
                        o.optString("date", ""),
                        o.optString("title", ""),
                        o.optString("type", Plan.FLEX),
                        o.optString("time", ""),
                        o.optString("duration", ""),
                        o.optBoolean("done", false)
                );
                p.startTime = o.optString("startTime", p.time);
                p.endTime = o.optString("endTime", "");
                p.note = o.optString("note", "");
                p.repeat = o.optString("repeat", Plan.REPEAT_NONE);
                p.category = o.optString("category", "personal");
                p.details = o.optString("details", "");
                p.status = o.optString("status", p.done ? Plan.STATUS_DONE : Plan.STATUS_PLANNED);
                if (p.done) p.status = Plan.STATUS_DONE;
                list.add(p);
            }
        } catch (Exception ignored) {}
        return list;
    }

    public static void save(Context context, List<Plan> plans) {
        JSONArray a = new JSONArray();
        try {
            for (Plan p : plans) {
                JSONObject o = new JSONObject();
                o.put("id", p.id);
                o.put("date", p.date);
                o.put("title", p.title);
                o.put("type", p.type);
                o.put("time", p.time);
                o.put("duration", p.duration);
                o.put("done", p.done);
                o.put("startTime", p.startTime == null ? "" : p.startTime);
                o.put("endTime", p.endTime == null ? "" : p.endTime);
                o.put("note", p.note == null ? "" : p.note);
                o.put("repeat", p.repeat == null ? Plan.REPEAT_NONE : p.repeat);
                o.put("category", p.category == null ? "personal" : p.category);
                o.put("details", p.details == null ? "" : p.details);
                o.put("status", p.status == null ? Plan.STATUS_PLANNED : p.status);
                a.put(o);
            }
        } catch (Exception ignored) {}
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY, a.toString()).apply();
    }

    public static long nextId(List<Plan> plans) {
        long max = System.currentTimeMillis();
        for (Plan p : plans) {
            if (p.id >= max) max = p.id + 1;
        }
        return max;
    }

    /** Adds the requested plan and, for repeated plans, concrete upcoming occurrences. */
    public static void addWithRepeat(List<Plan> plans, Plan first) {
        if (first == null) return;
        plans.add(first);
        if (Plan.REPEAT_NONE.equals(first.repeat) || first.date == null || first.date.isEmpty()) return;

        Calendar day = parseDate(first.date);
        if (day == null) return;

        long id = nextId(plans);
        // Materialise a practical 45-day horizon. This keeps notifications reliable without
        // creating an unbounded number of alarms.
        for (int i = 0; i < 45; i++) {
            day.add(Calendar.DAY_OF_MONTH, 1);
            if (!matchesRepeat(first.repeat, day, first)) continue;

            Plan n = new Plan(id++, key(day), first.title, first.type, first.time, first.duration, false);
            n.startTime = first.startTime == null ? "" : first.startTime;
            n.endTime = first.endTime == null ? "" : first.endTime;
            n.time = n.startTime;
            n.note = first.note == null ? "" : first.note;
            // Occurrences are concrete instances; the first plan remains the series anchor.
            n.repeat = Plan.REPEAT_NONE;
            n.category = first.category == null ? "personal" : first.category;
            n.details = first.details == null ? "" : first.details;
            n.status = Plan.STATUS_PLANNED;
            plans.add(n);
        }
    }

    private static boolean matchesRepeat(String repeat, Calendar day, Plan first) {
        if (Plan.REPEAT_DAILY.equals(repeat)) return true;
        if (Plan.REPEAT_WEEKLY.equals(repeat)) return day.get(Calendar.DAY_OF_WEEK) == anchorDayOfWeek(first);
        if (Plan.REPEAT_WEEKDAYS.equals(repeat)) {
            int w = day.get(Calendar.DAY_OF_WEEK);
            return w != Calendar.SATURDAY && w != Calendar.SUNDAY;
        }
        return false;
    }

    private static int anchorDayOfWeek(Plan first) {\n        Calendar c = parseDate(first.date);\n        return c == null ? Calendar.MONDAY : c.get(Calendar.DAY_OF_WEEK);\n    }\n\n    private static Calendar parseDate(String value) {
        try {
            String[] p = value.split("-");
            if (p.length != 3) return null;
            Calendar c = Calendar.getInstance();
            c.setLenient(false);
            c.set(Integer.parseInt(p[0]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[2]), 0, 0, 0);
            c.set(Calendar.MILLISECOND, 0);
            c.getTime();
            return c;
        } catch (Exception e) {
            return null;
        }
    }

    private static String key(Calendar c) {
        return String.format(java.util.Locale.GERMANY, "%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }
}
