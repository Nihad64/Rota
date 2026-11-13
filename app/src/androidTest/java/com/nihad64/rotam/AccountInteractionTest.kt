package com.nihad64.rotam

import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nihad64.rotam.ui.settings.SettingsScreen
import com.nihad64.rotam.ui.settings.SettingsViewModel
import com.nihad64.rotam.ui.theme.RotamTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountInteractionTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 @Test fun googleButtonStartsAuthenticationAndShowsFailureBesideButton() {
  var calls=0
  val vm=SettingsViewModel(RotamApp.container,authenticate={activity->
   assertSame(ui.activity,activity)
   calls++
   throw IllegalStateException("Test bağlantı hatası")
  })
  ui.activity.runOnUiThread{ui.activity.setContent{RotamTheme{SettingsScreen(vm,{}, {})}}}
  ui.onNodeWithTag("account-settings-entry").performScrollTo().performClick()
  ui.onNodeWithText("Google ile giriş yap").performScrollTo().performClick()
  ui.waitUntil(5000){calls==1 && !vm.authBusy.value}
  ui.onNodeWithText("Google girişi tamamlanamadı",substring=true).assertIsDisplayed()
  ui.onNodeWithText("Google ile giriş yap").assertIsEnabled()
  // Explicitly verify ContextWrapper resolution, which the previous safe cast skipped silently.
  ui.runOnIdle { vm.signIn(android.view.ContextThemeWrapper(ui.activity,android.R.style.Theme_DeviceDefault)) }
  ui.waitUntil(5000){calls==2 && !vm.authBusy.value}
 }
}
