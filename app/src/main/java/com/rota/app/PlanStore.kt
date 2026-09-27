package com.rota.app
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
data class Plan(val id:Long,val title:String,val type:String,val time:String="",val duration:String="",val done:Boolean=false)
object PlanStore{
 private const val P="rota_plans"; private const val K="plans"
 fun load(c:Context):List<Plan>{val a=JSONArray(c.getSharedPreferences(P,0).getString(K,"[]"));return List(a.length()){val o=a.getJSONObject(it);Plan(o.getLong("id"),o.getString("title"),o.getString("type"),o.optString("time"),o.optString("duration"),o.optBoolean("done"))}}
 fun save(c:Context,p:List<Plan>){val a=JSONArray();p.forEach{a.put(JSONObject().apply{put("id",it.id);put("title",it.title);put("type",it.type);put("time",it.time);put("duration",it.duration);put("done",it.done)})};c.getSharedPreferences(P,0).edit().putString(K,a.toString()).apply()}
}
