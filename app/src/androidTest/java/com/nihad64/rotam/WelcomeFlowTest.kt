package com.nihad64.rotam

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nihad64.rotam.ui.settings.SettingsViewModel
import com.nihad64.rotam.ui.theme.RotamTheme
import com.nihad64.rotam.ui.welcome.WelcomeScreen
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WelcomeFlowTest {
 @get:Rule val ui=createComposeRule()
 @Test fun welcomeKeepsOfflineEntryAndShowsAuthenticationFailureNearAction(){
  var signIns=0;var continued=0;var privacy=0
  val vm=SettingsViewModel(RotamApp.container,authenticate={signIns++;throw IllegalStateException("Test bağlantı hatası")})
  ui.setContent{RotamTheme{WelcomeScreen(vm,{continued++},{privacy++})}}
  ui.onNodeWithText("Google ile devam et").performScrollTo().performClick()
  ui.waitUntil(5000){signIns==1&&!vm.authBusy.value}
  ui.onNodeWithText("Google girişi tamamlanamadı",substring=true).performScrollTo().assertIsDisplayed()
  ui.onNodeWithText("Verilerim nasıl kullanılıyor?").performScrollTo().performClick()
  ui.runOnIdle{assertEquals(1,privacy)}
  ui.onNodeWithText("Önce uygulamaya göz at").performScrollTo().performClick()
  ui.runOnIdle{assertEquals(1,continued)}
 }
}
