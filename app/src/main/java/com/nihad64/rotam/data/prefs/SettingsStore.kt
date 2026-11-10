package com.nihad64.rotam.data.prefs
import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.*
import java.time.LocalTime
private val Context.dataStore by preferencesDataStore(name="rotam_settings")
data class Settings(val userName:String="",val remindersEnabled:Boolean=true,val reminderMinutes:Int=15,
 val quietEnabled:Boolean=true,val quietStart:Int=1380,val quietEnd:Int=480,val uiSounds:Boolean=false,val haptics:Boolean=true,
 val theme:String="dark",val geminiKey:String="",val animations:Boolean=true,val sharePlans:Boolean=false,val geminiModel:String="gemini-3.5-flash-lite",val welcomeCompleted:Boolean=false)
class SettingsStore(private val context:Context){
 private val vault=KeyVault(context)
 private fun s(k:String)=stringPreferencesKey(k);private fun b(k:String)=booleanPreferencesKey(k);private fun i(k:String)=intPreferencesKey(k)
 val settings:Flow<Settings> = context.dataStore.data.map{p->Settings(p[s("user_name")]?:"",p[b("reminders")]?:true,p[i("reminder_minutes")]?:15,p[b("quiet")]?:true,p[i("quiet_start_minutes")]?:1380,p[i("quiet_end_minutes")]?:480,p[b("ui_sounds")]?:false,p[b("haptics")]?:true,"dark",vault.read(),p[b("animations")]?:true,p[b("share_plans")]?:false,p[s("gemini_model")]?:"gemini-3.5-flash-lite",p[b("welcome_completed")]?:false)}
 suspend fun completeWelcome()=context.dataStore.edit{it[b("welcome_completed")]=true}
 suspend fun current()=settings.first()
 suspend fun setName(v:String)=context.dataStore.edit{it[s("user_name")]=v.trim().take(50)}
 suspend fun setReminders(v:Boolean)=context.dataStore.edit{it[b("reminders")]=v}
 suspend fun setReminderMinutes(v:Int)=context.dataStore.edit{it[i("reminder_minutes")]=v.coerceIn(0,10080)}
 suspend fun setQuiet(v:Boolean)=context.dataStore.edit{it[b("quiet")]=v}
 suspend fun setQuietTimes(start:Int,end:Int)=context.dataStore.edit{it[i("quiet_start_minutes")]=start;it[i("quiet_end_minutes")]=end}
 suspend fun setSounds(v:Boolean)=context.dataStore.edit{it[b("ui_sounds")]=v}
 suspend fun setHaptics(v:Boolean)=context.dataStore.edit{it[b("haptics")]=v}
 suspend fun setAnimations(v:Boolean)=context.dataStore.edit{it[b("animations")]=v}
 suspend fun setSharePlans(v:Boolean)=context.dataStore.edit{it[b("share_plans")]=v}
 suspend fun setTheme(v:String)=Unit
 suspend fun setGeminiKey(v:String){vault.write(v.trim());context.dataStore.edit{it[longPreferencesKey("key_version")]=System.currentTimeMillis();it.remove(s("gemini_key"))}}
 suspend fun setGeminiModel(v:String){require(v.matches(Regex("[a-zA-Z0-9._-]{1,100}")));context.dataStore.edit{it[s("gemini_model")]=v}}
 suspend fun migrate(){
  val pref=context.dataStore.data.first();if(pref[b("legacy_migrated")]==true)return
  val old=context.getSharedPreferences("rotam_settings",Context.MODE_PRIVATE)
  val key=pref[s("gemini_key")]?:old.getString("gemini_api_key","").orEmpty();if(key.isNotBlank())vault.write(key)
  fun minutes(k:String,default:Int)=runCatching{LocalTime.parse(old.getString(k,null)).toSecondOfDay()/60}.getOrDefault(default)
  context.dataStore.edit{v->
   if(old.contains("reminders"))v[b("reminders")]=old.getBoolean("reminders",true)
   if(old.contains("reminder_minutes"))v[i("reminder_minutes")]=old.getInt("reminder_minutes",15)
   if(old.contains("haptics"))v[b("haptics")]=old.getBoolean("haptics",true)
   if(old.contains("sounds"))v[b("ui_sounds")]=old.getBoolean("sounds",false)
   if(old.contains("animations"))v[b("animations")]=old.getBoolean("animations",true)
   v[i("quiet_start_minutes")]=minutes("quiet_start",(pref[i("quiet_start")]?:23)*60)
   v[i("quiet_end_minutes")]=minutes("quiet_end",(pref[i("quiet_end")]?:8)*60)
   v.remove(s("gemini_key"));v[b("legacy_migrated")]=true
  }
  old.edit().remove("gemini_api_key").commit()
 }
}
