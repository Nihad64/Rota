package com.rota.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public final class PlanStore {
    private static final String PREFS="rota_store",KEY="plans";
    private PlanStore(){}
    public static List<Plan> load(Context context){
        List<Plan> list=new ArrayList<>();String raw=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY,"[]");
        try{JSONArray a=new JSONArray(raw);for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);
            Plan p=new Plan(o.optLong("id",System.currentTimeMillis()+i),o.optString("date",""),o.optString("title",""),o.optString("type",Plan.FLEX),o.optString("time",""),o.optString("duration",""),o.optBoolean("done",false));
            p.startTime=o.optString("startTime",p.time);p.endTime=o.optString("endTime","");p.note=o.optString("note","");p.repeat=o.optString("repeat",Plan.REPEAT_NONE);
            p.category=o.optString("category","personal");p.details=o.optString("details","");p.status=o.optString("status",p.done?Plan.STATUS_DONE:Plan.STATUS_PLANNED);if(p.done)p.status=Plan.STATUS_DONE;list.add(p);
        }}catch(Exception ignored){}return list;
    }
    public static void save(Context context,List<Plan> plans){
        JSONArray a=new JSONArray();try{for(Plan p:plans){JSONObject o=new JSONObject();
            o.put("id",p.id);o.put("date",p.date);o.put("title",p.title);o.put("type",p.type);o.put("time",p.time);o.put("duration",p.duration);o.put("done",p.done);
            o.put("startTime",p.startTime==null?"":p.startTime);o.put("endTime",p.endTime==null?"":p.endTime);o.put("note",p.note==null?"":p.note);
            o.put("repeat",p.repeat==null?Plan.REPEAT_NONE:p.repeat);o.put("category",p.category==null?"personal":p.category);o.put("details",p.details==null?"":p.details);
            o.put("status",p.status==null?Plan.STATUS_PLANNED:p.status);a.put(o);
        }}catch(Exception ignored){}context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(KEY,a.toString()).apply();
    }
}
