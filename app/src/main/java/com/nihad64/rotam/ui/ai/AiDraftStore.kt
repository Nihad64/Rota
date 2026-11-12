package com.nihad64.rotam.ui.ai

import android.util.AtomicFile
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

data class AiDraft(val input:String="",val pending:String="")
/** Private device draft; never part of backup, sync, or automatic network requests. */
class AiDraftStore(private val directory:File) {
 val error=kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
 private val cache=ConcurrentHashMap<String,AiDraft>()
 private val lock=Mutex()
 private val writer=CoroutineScope(SupervisorJob()+Dispatchers.IO)
 private fun key(owner:String?)=owner?:"local-guest"
 private fun file(owner:String?)=AtomicFile(File(directory,"ai-draft-"+MessageDigest.getInstance("SHA-256").digest(key(owner).toByteArray()).joinToString(""){"%02x".format(it)}+".json"))
 suspend fun load(owner:String?):AiDraft=withContext(Dispatchers.IO){lock.withLock{
  cache[key(owner)]?:run {
   val f=file(owner)
   val draft=if(!f.baseFile.exists())AiDraft()else {
    require(f.baseFile.length()<=65536){"AI taslağı çok büyük; kaynak dosya korundu."}
    val root=JSONObject(String(f.readFully(),Charsets.UTF_8));require(root.getInt("version")==1)
    AiDraft(root.getString("input"),root.getString("pending")).also{require(it.input.length<=5000&&it.pending.length<=5000)}
   }
   cache.putIfAbsent(key(owner),draft)?:draft
  }
 }}
 fun update(owner:String?,draft:AiDraft) {
  require(draft.input.length<=5000&&draft.pending.length<=5000)
  cache[key(owner)]=draft
  writer.launch{try{flush(owner)}catch(e:CancellationException){throw e}catch(_:Exception){error.value="AI metni cihazda saklanamadı. Uygulamayı kapatmadan önce metnini kopyala ve boş alanı kontrol et."}}
 }
 suspend fun flush(owner:String?)=withContext(Dispatchers.IO){lock.withLock{
  val draft=cache[key(owner)]?:return@withLock
  val f=file(owner);val stream=f.startWrite()
  try{stream.write(JSONObject().put("version",1).put("input",draft.input).put("pending",draft.pending).toString().toByteArray());f.finishWrite(stream);error.value=null}catch(e:Exception){f.failWrite(stream);throw e}
 }}
}
