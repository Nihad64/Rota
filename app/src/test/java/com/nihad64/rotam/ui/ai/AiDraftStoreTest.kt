package com.nihad64.rotam.ui.ai

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=android.app.Application::class)
class AiDraftStoreTest {
 @Test fun pendingRequestAndNewerInputSurviveRestartWithoutCrossingAccounts()=runBlocking {
  val dir=File(ApplicationProvider.getApplicationContext<Context>().noBackupFilesDir,"draft-test");dir.mkdirs()
  val first=AiDraftStore(dir)
  first.update("owner-a",AiDraft("Yarın çalışıyorum","Önceki bekleyen istek"))
  first.update("owner-a",AiDraft("Yarın 14–22 çalışıyorum","Önceki bekleyen istek"))
  first.update("owner-b",AiDraft("Başka hesabın taslağı"))
  first.flush("owner-a");first.flush("owner-b")
  val reopened=AiDraftStore(dir)
  assertEquals(AiDraft("Yarın 14–22 çalışıyorum","Önceki bekleyen istek"),reopened.load("owner-a"))
  assertEquals(AiDraft("Başka hesabın taslağı"),reopened.load("owner-b"))
  assertEquals(AiDraft(),reopened.load(null))
  assertTrue(dir.listFiles()!!.none{it.name.contains("owner-")})
 }
 @Test fun invalidDraftIsReportedAndSourceIsNotOverwrittenAsAnEmptyDraft()=runBlocking {
  val dir=File(ApplicationProvider.getApplicationContext<Context>().noBackupFilesDir,"bad-draft");dir.mkdirs()
  val hash=java.security.MessageDigest.getInstance("SHA-256").digest("a".toByteArray()).joinToString(""){"%02x".format(it)}
  val file=File(dir,"ai-draft-$hash.json");file.writeText("broken-json")
  try{AiDraftStore(dir).load("a");fail()}catch(_:org.json.JSONException){}
  assertEquals("broken-json",file.readText())
 }
}
