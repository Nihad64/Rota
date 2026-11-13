package com.nihad64.rotam.data.sync

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.nihad64.rotam.AppContainer
import com.nihad64.rotam.data.prefs.PortablePreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/** Separate opt-in: existing plan-sync consent never silently uploads device preferences. */
class PreferenceSyncCoordinator(private val c:AppContainer) {
 private val prefs=c.app.getSharedPreferences("rotam_preferences_sync",0)
 private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
 private val enabled=MutableStateFlow(prefs.getBoolean("enabled",false))
 val isEnabled=enabled.asStateFlow()
 private val mutable=MutableStateFlow<CloudStatus>(CloudStatus.Off);val status=mutable.asStateFlow()
 private val requests=Channel<Unit>(Channel.CONFLATED);private val lock=Mutex();private var started=false
 private data class Session(val uid:String,val engine:SyncEngine,var review:SyncResult.Conflicts?=null)
 private var session:Session?=null
 private fun allowed(uid:String)=enabled.value&&prefs.getString("bound_uid",null)==uid&&c.account.account.value?.uid==uid&&c.sync.enabledForCurrentAccount()
 fun start(){
  if(started)return;started=true
  scope.launch {
   combine(c.account.account,enabled,c.sync.status){a,e,_->a?.uid?.takeIf{e&&allowed(it)}}.distinctUntilChanged().collectLatest{uid->
    mutable.value=when{!enabled.value->CloudStatus.Off;c.account.account.value==null->CloudStatus.SignedOut;uid==null->CloudStatus.Off;else->CloudStatus.Working}
    if(uid==null)return@collectLatest
    coroutineScope {
     val registration=FirebaseFirestore.getInstance().document("users/$uid/preferences/snapshot").addSnapshotListener(MetadataChanges.INCLUDE){snapshot,error->
      if(allowed(uid)){if(error!=null)mutable.value=CloudStatus.Error("Tercihler için bulut bağlantısı doğrulanamadı. Cihaz tercihlerin korunuyor.")else if(snapshot!=null&&!snapshot.metadata.isFromCache)requests.trySend(Unit)}
     }
     try {
      launch {c.settings.settings.map{PortablePreferences.values(it)}.distinctUntilChanged().collect{request()}}
      requests.trySend(Unit)
      for(signal in requests){delay(1500);if(session?.review==null)synchronize()}
     }finally{registration.remove()}
    }
   }
  }
 }
 fun enable():String? {
  val uid=c.account.account.value?.uid?:return "Önce Google hesabınla giriş yap."
  if(!c.sync.enabledForCurrentAccount())return "Önce planlarını mevcut hesabına bağla ve eşitle. Tercihlerin şimdilik cihazında kalır."
  val bound=prefs.getString("bound_uid",null)
  if(bound!=null&&bound!=uid)return "Bu cihazın tercih eşitlemesi başka bir hesaba bağlı. Bağlı hesabınla giriş yap."
  if(!prefs.edit().putString("bound_uid",uid).putBoolean("enabled",true).commit())return "Tercih kaydedilemedi. Yeniden dene."
  enabled.value=true;request();return null
 }
 fun disable(){if(prefs.edit().putBoolean("enabled",false).commit()){enabled.value=false;mutable.value=CloudStatus.Off}}
 fun request(){if(enabled.value){requests.trySend(Unit);c.sync.request()}}
 private fun getSession(uid:String):Session {
  session?.takeIf{it.uid==uid}?.let{return it}
  val writer=prefs.getString("writer",null)?:UUID.randomUUID().toString().also{check(prefs.edit().putString("writer",it).commit())}
  val remote=FirestoreSyncEndpoint(uid,writer,preferences=true,authenticatedUid={if(allowed(uid))uid else null})
  return Session(uid,SyncEngine(PreferenceLocalStore(c.settings){allowed(uid)},FileSyncBaseline(c.app,uid,preferences=true),remote){PreferenceRecords.decode(it);Unit}).also{session=it}
 }
 suspend fun synchronize():Boolean=lock.withLock {
  val uid=c.account.account.value?.uid?:return@withLock true
  if(!allowed(uid))return@withLock true
  val s=getSession(uid);if(s.review!=null)return@withLock true
  mutable.value=CloudStatus.Working
  try{accept(s,s.engine.synchronize());true}
  catch(e:CancellationException){throw e}catch(e:Exception){if(allowed(uid))mutable.value=CloudStatus.Error(e.message?.take(250)?:"Tercihler eşitlenemedi. Cihazdaki seçimlerin korunuyor.");false}
 }
 fun resolve(useLocal:Boolean){scope.launch{lock.withLock{
  val s=session?:return@withLock;val review=s.review?:return@withLock;if(!allowed(s.uid))return@withLock
  mutable.value=CloudStatus.Working
  try{val result=s.engine.resolve(review,useLocal);s.review=null;accept(s,result)}
  catch(e:CancellationException){throw e}catch(_:Exception){s.review=null;mutable.value=CloudStatus.Error("Tercihler incelemeden sonra değişti. Güncel seçimleri yeniden eşitle.")}
 }}}
 private fun accept(s:Session,result:SyncResult){
  if(!allowed(s.uid))return
  when(result){is SyncResult.Complete->{s.review=null;mutable.value=CloudStatus.Current(System.currentTimeMillis())};is SyncResult.Conflicts->{s.review=result;mutable.value=CloudStatus.Review(result.merged.conflicts)};SyncResult.LocalChanged->{s.review=null;requests.trySend(Unit)}}
 }
}
