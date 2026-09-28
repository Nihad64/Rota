package com.rota.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public final class PlanStore {
    private static final String PREFS = "rota_store";
    private static final String KEY = "plans";

    private PlanStore() {}

    public static List<Plan> load(Context context) {
        List<Plan> plans = new ArrayList<>();
        String raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                plans.add(new Plan(
                        o.getLong("id"),
                        o.getString("date"),
                        o.getString("title"),
                        o.getString("type"),
                        o.optString("time"),
                        o.optString("duration"),
                        o.optBoolean("done")
                ));
            }
        } catch (Exception ignored) {
        }
        return plans;
    }

    public static void save(Context context, List<Plan> plans) {
        JSONArray array = new JSONArray();
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
                array.put(o);
            }
        } catch (Exception ignored) {
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY, array.toString()).apply();
    }
}
