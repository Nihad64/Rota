package com.nihad64.rotam
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class AccountConfigurationTest {
 @Test fun realProjectConfigAndGoogleClientArePackagedAndAiRequiresSignIn()=runBlocking {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val c=RotamApp.container;c.initialize()
  assertEquals("rotam-nihad64",FirebaseApp.getInstance().options.projectId)
  assertEquals("1:612038382105:android:7bc018e5a6872a32d91088",FirebaseApp.getInstance().options.applicationId)
  val id=context.resources.getIdentifier("default_web_client_id","string",context.packageName)
  assertTrue(id!=0);assertTrue(context.getString(id).endsWith(".apps.googleusercontent.com"))
  assertTrue(c.account.configured)
  // The emulator fixture has no Google account. This must fail before any model request.
  assertNull(c.account.account.value)
  try{c.gemini.ask(listOf(true to "Yarın oku"),emptyList());fail("AI requires a signed-in user")}
  catch(e:com.nihad64.rotam.ui.ai.GeminiException){assertTrue(e.message.orEmpty().contains("Google ile giriş"))}
 }
}
