package com.nihad64.rotam.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.nihad64.rotam.AppContainer
import com.nihad64.rotam.data.backup.*
import com.nihad64.rotam.data.prefs.Settings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class SettingsViewModel(private val c:AppContainer, private val authenticate:suspend (android.app.Activity)->Unit = {c.account.signIn(it)}):ViewModel() {
    val reminderError=c.scheduler.error
    fun retryReminders()=run{c.scheduler.error.value=null;c.scheduler.rescheduleAll()}
    val cloudStatus=c.sync.status
    fun enableSync(){message.value=c.sync.enable()}
    fun disableSync()=c.sync.disable()
    fun syncNow()=c.sync.request()
    fun resolveSync(useLocal:Boolean)=c.sync.resolve(useLocal)
    data class MemoryDraft(val before:com.nihad64.rotam.data.model.UserMemory?=null)
    val memoryDraft=MutableStateFlow<MemoryDraft?>(null)
    val memories=combine(c.memory.observeAll(),c.account.account){all,account->all.filter{it.ownerUid==account?.uid&&!it.isDeleted}}.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    fun editMemory(before:com.nihad64.rotam.data.model.UserMemory?=null){message.value=null;memoryDraft.value=MemoryDraft(before)}
    fun dismissMemory(){if(!busy.value)memoryDraft.value=null}
    fun canCloudMemory()=c.sync.canApproveMemoryForCurrentAccount()
    fun syncActive()=c.sync.enabledForCurrentAccount()
    fun saveMemory(text:String,cloud:Boolean)=run {
        val uid=c.account.account.value?.uid?:error("Önce Google hesabınla giriş yap.")
        check(!cloud||canCloudMemory()){"Eşitleme başka bir hesaba bağlı."}
        val before=memoryDraft.value?.before
        c.memory.save(uid,text,cloud,before?.uid,before)
        memoryDraft.value=null;c.notices.send(com.nihad64.rotam.Notice("Hafızaya kaydedildi"))
    }
    fun deleteMemory(uid:String)=run {
        val owner=c.account.account.value?.uid?:error("Önce Google hesabınla giriş yap.")
        c.memory.remove(owner,uid);c.notices.send(com.nihad64.rotam.Notice("Bu bilgi hafızadan silindi"))
    }
    fun setUseMemory(value:Boolean)=run{c.settings.setUseMemory(value)}
    val account=c.account.account
    val accountConfigured=c.account.configured
    val authMessage=MutableStateFlow<String?>(null)
    val authBusy=MutableStateFlow(false)
    fun signIn(context:android.content.Context) {
        val activity=generateSequence(context) { (it as? android.content.ContextWrapper)?.baseContext }
            .filterIsInstance<android.app.Activity>().firstOrNull()
        if(activity==null) { authMessage.value="Google girişi açılamadı. Uygulamayı kapatıp yeniden aç."; return }
        if(authBusy.value)return
        authBusy.value=true;authMessage.value="Google hesabı seçimi açılıyor…"
        viewModelScope.launch {
            try {
                completeSignIn(activity)
                authMessage.value="Google hesabınla giriş yapıldı."
            } catch(e:CancellationException){throw e}
            catch(e:androidx.credentials.exceptions.GetCredentialCancellationException){authMessage.value="Hesap seçimi iptal edildi. Yeniden deneyebilirsin."}
            catch(e:androidx.credentials.exceptions.NoCredentialException){authMessage.value="Google hesabı bulunamadı. Telefonunun hesap ayarlarını ve Google Play Hizmetleri’ni kontrol et."}
            catch(e:Exception){authMessage.value="Google girişi tamamlanamadı (${e.javaClass.simpleName}). ${e.localizedMessage ?: "Yeniden deneyebilirsin."}"}
            finally{authBusy.value=false}
        }
    }
    private suspend fun completeSignIn(activity:android.app.Activity) { authenticate(activity); c.settings.setSharePlans(false); if(c.settings.current().userName.isBlank()) c.account.account.value?.name?.let { c.settings.setName(it) } }
    fun signOut()=run { c.settings.setSharePlans(false); c.account.signOut() }
    val settings=c.settings.settings.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),Settings())
    private val backup=BackupStore(c.app,c.db)
    val pending=MutableStateFlow<BackupData?>(null)
    val message=MutableStateFlow<String?>(null)
    val busy=MutableStateFlow(false)
    fun canScheduleExact()=c.scheduler.canScheduleExact()
    fun setName(v:String)=run { c.settings.setName(v) }
    fun setReminders(v:Boolean)=run { c.settings.setReminders(v);c.scheduler.rescheduleAll() }
    fun setReminderMinutes(v:Int)=run { c.settings.setReminderMinutes(v) }
    fun setQuiet(v:Boolean)=run { c.settings.setQuiet(v);c.scheduler.rescheduleAll() }
    fun setQuietTimes(start:Int,end:Int)=run { c.settings.setQuietTimes(start,end);c.scheduler.rescheduleAll() }
    fun setSounds(v:Boolean)=run { c.settings.setSounds(v) }
    fun setHaptics(v:Boolean)=run { c.settings.setHaptics(v) }
    fun setAnimations(v:Boolean)=run { c.settings.setAnimations(v) }
    fun setSharePlans(v:Boolean)=run { c.settings.setSharePlans(v) }
    fun setModel(v:String)=run { c.settings.setGeminiModel(v) }
    fun setGeminiKey(v:String)=run { c.settings.setGeminiKey(v) }
    fun export(uri:Uri)=run {
        c.focus.pause()
        val raw=backup.export()
        withContext(Dispatchers.IO){c.app.contentResolver.openOutputStream(uri,"wt")?.bufferedWriter()?.use{it.write(raw)} ?: error("Dosya açılamadı.")}
        message.value="Yedek kaydedildi."
    }
    fun preview(uri:Uri)=run {
        val raw=withContext(Dispatchers.IO){c.app.contentResolver.openInputStream(uri)?.use { stream->
            val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
            while(true){val n=stream.read(buffer);if(n<0)break;require(out.size()+n<=20*1024*1024){"Dosya 20 MB sınırını aşıyor."};out.write(buffer,0,n)}
            out.toString("UTF-8")
        } ?: error("Dosya okunamadı.")}
        pending.value=withContext(Dispatchers.Default){BackupCodec.decode(raw)}
    }
    fun previewRecovery()=run { pending.value=BackupCodec.decode(backup.recovery()) }
    fun dismissPreview(){if(!busy.value)pending.value=null}
    fun restore()=run {
        val data=pending.value ?: return@run
        c.focus.pause();backup.restore(data);c.scheduler.resetState();c.scheduler.rescheduleAll();pending.value=null
        message.value="Yedek geri yüklendi. Önceki kayıtların geri dönüş yedeğinde korunuyor."
    }
    fun resetAll()=run {
        c.focus.pause();backup.checkpoint()
        c.db.withTransaction{c.planRepo.clear();c.goalRepo.clear()}
        c.scheduler.resetState();message.value="Planlar ve hedefler sıfırlandı. Geri dönüş yedeği alındı."
    }
    private fun run(action:suspend ()->Unit) {
        if(busy.value)return
        busy.value=true;message.value=null
        viewModelScope.launch { try{action()}catch(e:kotlinx.coroutines.CancellationException){throw e}catch(e:Exception){message.value=if(e is IllegalStateException||e is IllegalArgumentException)e.message else "İşlem tamamlanamadı. Bilgilerin korunuyor; yeniden deneyebilirsin."}finally{busy.value=false} }
    }
}
