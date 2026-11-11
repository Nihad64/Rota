package com.nihad64.rotam.data.sync

import android.content.Context
import androidx.work.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.nihad64.rotam.AppContainer
import com.nihad64.rotam.RotamApp
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.TimeUnit

sealed interface CloudStatus {
 data object Off:CloudStatus
 data object SignedOut:CloudStatus
 data object DifferentAccount:CloudStatus
 data object Working:CloudStatus
 data class Current(val time:Long):CloudStatus
 data class Review(val conflicts:List<SyncConflict>):CloudStatus
 data class Error(val message:String):CloudStatus
}

/** A local dataset stays bound to its first explicitly selected account, including after sign-out. */
class SyncCoordinator(private val c:AppContainer) {
 private val prefs=c.app.getSharedPreferences("rotam_cloud_sync",Context.MODE_PRIVATE)
 private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
 private val enabled=MutableStateFlow(prefs.getBoolean("enabled",false))
 private val mutable=MutableStateFlow<CloudStatus>(CloudStatus.Off)
 val status=mutable.asStateFlow()
 private val requests=Channel<Unit>(Channel.CONFLATED)
 private val lock=Mutex()
 private var started=false
 private data class Session(val uid:String,val engine:SyncEngine,var review:SyncResult.Conflicts?=null)
 private var session:Session?=null
 private fun allowed(uid:String)=enabled.value&&prefs.getString("bound_uid",null)==uid&&c.account.account.value?.uid==uid
 private fun stateWithoutSession():CloudStatus=when {
  !enabled.value->CloudStatus.Off
  c.account.account.value==null->CloudStatus.SignedOut
  prefs.getString("bound_uid",null)!=c.account.account.value?.uid->CloudStatus.DifferentAccount
  else->CloudStatus.Working
 }
 fun start() {
  if(started)return
  started=true
  scope.launch {
   combine(c.account.account,enabled){a,e->if(e)a?.uid else null}.collectLatest { uid->
    mutable.value=stateWithoutSession()
    if(uid==null||!allowed(uid))return@collectLatest
    session?.takeIf{it.uid==uid}?.review?.let{mutable.value=CloudStatus.Review(it.merged.conflicts)}
    schedulePeriodic()
    coroutineScope {
     val registration=FirebaseFirestore.getInstance().document("users/$uid/sync/snapshot")
      .addSnapshotListener(MetadataChanges.INCLUDE){snapshot,error->
       if(error!=null){mutable.value=CloudStatus.Error("Bulut bağlantısı tamamlanamadı. Kayıtların cihazında korunuyor.")}
       else if(snapshot!=null&&!snapshot.metadata.isFromCache)requests.trySend(Unit)
      }
     try {
      launch {
       combine(c.db.planDao().observeSyncRecords(),c.db.goalDao().observeAll(),c.db.seriesDao().observeAll()){_,_,_->Unit}
        .collect { request() }
      }
      requests.trySend(Unit)
      for(signal in requests) {
       delay(1500)
       if(session?.review==null)synchronize()
      }
     } finally { registration.remove() }
    }
   }
  }
 }
 fun enable():String? {
  val uid=c.account.account.value?.uid?:return "Önce Google hesabınla giriş yap."
  val bound=prefs.getString("bound_uid",null)
  if(bound!=null&&bound!=uid)return "Bu cihazın kayıtları başka bir hesaba bağlı. Hesapları karıştırmamak için önce bağlı hesabınla giriş yap."
  if(!prefs.edit().putString("bound_uid",uid).putBoolean("enabled",true).commit())return "Tercih kaydedilemedi. Yeniden dene."
  enabled.value=true;request();return null
 }
 fun disable() {
  // Binding remains deliberately: another account must never inherit these records.
  if(!prefs.edit().putBoolean("enabled",false).commit())return
  enabled.value=false;mutable.value=CloudStatus.Off
  WorkManager.getInstance(c.app).cancelUniqueWork(PeriodicWork)
  WorkManager.getInstance(c.app).cancelUniqueWork(PendingWork)
 }
 fun request() {
  requests.trySend(Unit)
  if(enabled.value)WorkManager.getInstance(c.app).enqueueUniqueWork(PendingWork,ExistingWorkPolicy.KEEP,
   OneTimeWorkRequestBuilder<CloudSyncWorker>().setConstraints(network()).setInitialDelay(10,TimeUnit.SECONDS).build())
 }
 private fun getSession(uid:String):Session {
  session?.takeIf{it.uid==uid}?.let{return it}
  val writer=prefs.getString("writer",null)?:UUID.randomUUID().toString().also{check(prefs.edit().putString("writer",it).commit())}
  val store=RoomSyncStore(c.app,c.db){allowed(uid)}
  return Session(uid,SyncEngine(store,FileSyncBaseline(c.app,uid),FirestoreSyncEndpoint(uid,writer))).also{session=it}
 }
 suspend fun synchronize():Boolean=lock.withLock {
  val uid=c.account.account.value?.uid
  if(uid==null||!allowed(uid)){mutable.value=stateWithoutSession();return@withLock true}
  val s=getSession(uid)
  if(s.review!=null)return@withLock true
  mutable.value=CloudStatus.Working
  try {
   val result=s.engine.synchronize()
   if(allowed(uid))accept(s,result)
   true
  } catch(e:CancellationException){throw e}
  catch(e:Exception){
   if(allowed(uid))mutable.value=CloudStatus.Error(when(e){
    is IllegalArgumentException->e.message?:"Bulut kaydı doğrulanamadı. Yerel kayıtlar korundu."
    is RemoteChangedException->"Diğer cihazın kayıtları değişti. Yeniden eşitleyebilirsin."
    else->"Eşitleme tamamlanamadı. İnternet geldiğinde yeniden denenecek; kayıtların cihazında korunuyor."
   })
   false
  }
 }
 fun resolve(useLocal:Boolean) {scope.launch {lock.withLock {
  val s=session?:return@withLock
  val review=s.review?:return@withLock
  if(!allowed(s.uid))return@withLock
  mutable.value=CloudStatus.Working
  try {
   val result=s.engine.resolve(review,useLocal)
   s.review=null
   if(allowed(s.uid))accept(s,result)
  } catch(e:CancellationException){throw e}
  catch(_:Exception){s.review=null;mutable.value=CloudStatus.Error("Seçim uygulanamadı. Kayıtlar değişmiş olabilir; güncel sürümleri yeniden eşitle.")}
 }}}
 private suspend fun accept(s:Session,result:SyncResult) {
  when(result){
   is SyncResult.Complete->{
    s.review=null
    c.focus.active.value?.let{id->val plan=c.planRepo.byId(id);if(plan==null||plan.isDeleted||plan.isCompleted||plan.isSkipped)c.focus.pauseIf(id)}
    c.scheduler.rescheduleAll()
    mutable.value=CloudStatus.Current(System.currentTimeMillis())
   }
   is SyncResult.Conflicts->{s.review=result;mutable.value=CloudStatus.Review(result.merged.conflicts)}
   SyncResult.LocalChanged->{s.review=null;mutable.value=CloudStatus.Working;requests.trySend(Unit)}
  }
 }
 private fun schedulePeriodic() {WorkManager.getInstance(c.app).enqueueUniquePeriodicWork(PeriodicWork,ExistingPeriodicWorkPolicy.KEEP,
  PeriodicWorkRequestBuilder<CloudSyncWorker>(15,TimeUnit.MINUTES).setConstraints(network()).build())}
 private fun network()=Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
 companion object {private const val PeriodicWork="rotam-cloud-periodic";private const val PendingWork="rotam-cloud-pending"}
}
class CloudSyncWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params) {
 override suspend fun doWork():Result {
  val c=RotamApp.container
  return try {c.initialize();if(c.sync.synchronize())Result.success()else if(runAttemptCount<5)Result.retry()else Result.failure()}
  catch(e:CancellationException){throw e}catch(_:Exception){if(runAttemptCount<5)Result.retry()else Result.failure()}
 }
}
