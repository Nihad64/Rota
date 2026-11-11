package com.nihad64.rotam.data.sync

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/** Atomic personal snapshot with portable record identities and bounded decoding. */
data class SyncEnvelope(val revision:Long,val records:Map<RecordKey,CloudRecord>)
object SyncEnvelopeCodec {
 const val MaxCompressed=700*1024
 private const val MaxExpanded=20*1024*1024
 fun encode(records:Map<RecordKey,CloudRecord>):ByteArray {
  val rows=JSONArray(records.values.sortedWith(compareBy<CloudRecord>{it.key.kind.name}.thenBy{it.key.uid}).map{r->JSONObject().put("kind",r.key.kind.name).put("uid",r.key.uid).put("body",r.body)})
  val raw=JSONObject().put("schema",2).put("records",rows).toString().toByteArray(Charsets.UTF_8)
  require(raw.size<=MaxExpanded){"Kayıtlar eşitleme sınırını aşıyor. Yerel kayıtların korunuyor."}
  val out=ByteArrayOutputStream();GZIPOutputStream(out).use{it.write(raw)}
  return out.toByteArray().also{require(it.size<=MaxCompressed){"Kayıtlar tek eşitleme sınırını aşıyor. Yerel kayıtların korunuyor; dışa aktarabilirsin."}}
 }
 fun decode(bytes:ByteArray):Map<RecordKey,CloudRecord> {
  require(bytes.size<=MaxCompressed){"Bulut kaydı çok büyük"}
  val out=ByteArrayOutputStream()
  GZIPInputStream(ByteArrayInputStream(bytes)).use{input->
   val buffer=ByteArray(8192)
   while(true){val n=input.read(buffer);if(n<0)break;require(out.size()+n<=MaxExpanded){"Bulut kaydı açılma sınırını aşıyor"};out.write(buffer,0,n)}
  }
  val root=JSONObject(out.toString("UTF-8"));require(root.getInt("schema") in 1..2){"Bulut biçimi desteklenmiyor"}
  val rows=root.getJSONArray("records");require(rows.length()<=100000)
  val records=(0 until rows.length()).map{i->val r=rows.getJSONObject(i);CloudRecord(RecordKey(RecordKind.valueOf(r.getString("kind")),r.getString("uid")),r.getString("body"))}
  require(records.map{it.key}.distinct().size==records.size){"Bulutta yinelenen kimlik var"}
  return records.associateBy{it.key}
 }
}
