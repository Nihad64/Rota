package com.nihad64.rotam.data.sync

import com.nihad64.rotam.data.prefs.*
import org.json.JSONObject

object PreferenceRecords {
 fun encode(values:Map<String,Any>):Map<RecordKey,CloudRecord> {
  PortablePreferences.validate(values)
  return values.map{(key,value)->val id=RecordKey(RecordKind.PREFERENCE,key);id to CloudRecord(id,JSONObject().put("uid",key).put("value",value).toString())}.toMap()
 }
 fun decode(records:Map<RecordKey,CloudRecord>):Map<String,Any> {
  val values=records.map{(key,record)->
   require(key==record.key&&key.kind==RecordKind.PREFERENCE)
   val o=JSONObject(record.body);require(o.keys().asSequence().toSet()==setOf("uid","value")&&o.getString("uid")==key.uid)
   key.uid to o.get("value")
  }.toMap()
  PortablePreferences.validate(values);return values
 }
 fun display(record:CloudRecord?):String=if(record==null)"Yok"else PortablePreferences.display(record.key.uid,JSONObject(record.body).get("value"))
}
class PreferenceLocalStore(private val settings:SettingsStore,private val allowed:()->Boolean):SyncLocalStore {
 override suspend fun records():Map<RecordKey,CloudRecord>{check(allowed());return PreferenceRecords.encode(PortablePreferences.values(settings.current()))}
 override suspend fun applyIfUnchanged(expected:Map<RecordKey,CloudRecord>,incoming:Map<RecordKey,CloudRecord>):Boolean {
  check(allowed());return settings.applyPortable(PreferenceRecords.decode(expected),PreferenceRecords.decode(incoming),allowed)
 }
}
