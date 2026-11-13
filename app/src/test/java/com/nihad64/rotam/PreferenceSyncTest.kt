package com.nihad64.rotam

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.nihad64.rotam.data.prefs.*
import com.nihad64.rotam.data.sync.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=android.app.Application::class)
class PreferenceSyncTest {
 private class Baseline:SyncBaselineStore{var value=SyncEnvelope(0,emptyMap());override fun read()=value;override fun write(value:SyncEnvelope){this.value=value}}
 private class Remote:SyncEndpoint{var value=SyncEnvelope(0,emptyMap());override suspend fun read()=value;override suspend fun write(expectedRevision:Long,records:Map<RecordKey,CloudRecord>):Long{check(expectedRevision==value.revision);value=SyncEnvelope(expectedRevision+1,records);return value.revision}}
 @Test fun newDeviceReviewsDefaultsAndRestoresPreferencesWithoutEnablingAiSharing()=runBlocking {
  val context=ApplicationProvider.getApplicationContext<Context>()
  val scopes=List(2){CoroutineScope(SupervisorJob()+Dispatchers.IO)}
  val settings=List(2){i->val file=File(context.filesDir,"prefs-${UUID.randomUUID()}.preferences_pb");SettingsStore(context,PreferenceDataStoreFactory.create(scope=scopes[i],produceFile={file}))}
  try {
   settings[0].setSounds(true);settings[0].setSharePlans(true);settings[0].setUseMemory(false)
   val remote=Remote();val engines=List(2){i->SyncEngine(PreferenceLocalStore(settings[i]){true},Baseline(),remote){PreferenceRecords.decode(it);Unit}}
   assertTrue(engines[0].synchronize() is SyncResult.Complete)
   val review=engines[1].synchronize() as SyncResult.Conflicts
   assertFalse(settings[1].current().uiSounds)
   assertTrue(engines[1].resolve(review,false) is SyncResult.Complete)
   assertTrue(settings[1].current().uiSounds);assertFalse(settings[1].current().sharePlans);assertTrue(settings[1].current().useMemory)
   settings[0].setHaptics(false);settings[1].setAnimations(false)
   engines[0].synchronize();engines[1].synchronize();engines[0].synchronize()
   assertFalse(settings[0].current().animations);assertFalse(settings[1].current().haptics)
   settings[0].setQuietTimes(1260,480);settings[1].setQuietTimes(1200,480)
   engines[0].synchronize();val changed=engines[1].synchronize() as SyncResult.Conflicts
   settings[1].setQuietTimes(1140,480)
   assertEquals(SyncResult.LocalChanged,engines[1].resolve(changed,false));assertEquals(1140,settings[1].current().quietStart)
   val latest=engines[1].synchronize() as SyncResult.Conflicts
   engines[1].resolve(latest,false);assertEquals(1260,settings[1].current().quietStart)
  }finally{scopes.forEach{it.cancel()}}
 }
 @Test fun whitelistAndAtomicComparisonProtectPrivateOrNewerLocalChoices()=runBlocking {
  val context=ApplicationProvider.getApplicationContext<Context>();val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
  val file=File(context.filesDir,"prefs-${UUID.randomUUID()}.preferences_pb")
  val settings=SettingsStore(context,PreferenceDataStoreFactory.create(scope=scope,produceFile={file}))
  try {
   val store=PreferenceLocalStore(settings){true};val before=store.records()
   assertFalse(before.keys.any{it.uid.contains("key")||it.uid.contains("share")||it.uid.contains("memory")||it.uid.contains("welcome")})
   val incoming=PreferenceRecords.encode(PortablePreferences.values(Settings(uiSounds=true)))
   settings.setHaptics(false);assertFalse(store.applyIfUnchanged(before,incoming));assertFalse(settings.current().uiSounds);assertFalse(settings.current().haptics)
   try{PreferenceRecords.encode(PortablePreferences.values(Settings())+mapOf("gemini_key" to "synthetic-secret"));fail()}catch(_:IllegalArgumentException){}
   try{CloudRecords.decode(incoming);fail()}catch(_:IllegalArgumentException){}
   val restored=SyncEnvelopeCodec.decode(SyncEnvelopeCodec.encode(incoming));assertEquals(incoming,restored)
   try{PreferenceLocalStore(settings){false}.applyIfUnchanged(store.records(),incoming);fail()}catch(_:IllegalStateException){}
  }finally{scope.cancel()}
 }
}
