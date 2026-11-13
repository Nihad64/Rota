package com.nihad64.rotam
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nihad64.rotam.ui.theme.RotamTheme
import com.nihad64.rotam.ui.welcome.ReminderWelcomeScreen
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class ReminderWelcomeTest {
 @get:Rule val ui=createComposeRule()
 @Test fun notificationPermissionIsNotRequestedByCompositionAndSkippingRemainsAvailable(){
  var requests=0;var skips=0
  ui.setContent{RotamTheme{ReminderWelcomeScreen(onEnable={requests++},onSkip={skips++})}}
  ui.runOnIdle{assertEquals(0,requests)}
  ui.onNodeWithText("Şimdi değil").performScrollTo().performClick()
  ui.runOnIdle{assertEquals(1,skips);assertEquals(0,requests)}
  ui.onNodeWithText("Hatırlatmaları etkinleştir").performScrollTo().performClick()
  ui.runOnIdle{assertEquals(1,requests)}
 }
}
