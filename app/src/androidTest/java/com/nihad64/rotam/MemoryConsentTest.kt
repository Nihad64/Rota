package com.nihad64.rotam
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nihad64.rotam.ui.memory.MemoryDialog
import com.nihad64.rotam.ui.theme.RotamTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class MemoryConsentTest {
 @get:Rule val ui=createComposeRule()
 @Test fun memoryNeverSavesBeforeExplicitConfirmationAndRejectsOverlongText(){
  var saves=0;var dismissed=0
  ui.setContent{RotamTheme{MemoryDialog("Ulaşımım 30 dakika",initialCloud=false,onDismiss={dismissed++},onSave={_,cloud->assertFalse(cloud);saves++})}}
  ui.runOnIdle{assertEquals(0,saves)}
  ui.onNode(hasSetTextAction()).performTextReplacement("a".repeat(501))
  ui.onNodeWithText("Onayla ve hatırla").assertIsNotEnabled()
  ui.onNode(hasSetTextAction()).performTextReplacement("Ulaşımım 20 dakika")
  ui.onNodeWithText("Onayla ve hatırla").performClick()
  ui.runOnIdle{assertEquals(1,saves)}
  ui.onNodeWithText("Vazgeç").performClick()
  ui.runOnIdle{assertEquals(1,dismissed);assertEquals(1,saves)}
 }
}
