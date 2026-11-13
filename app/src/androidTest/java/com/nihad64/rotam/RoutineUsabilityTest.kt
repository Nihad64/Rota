package com.nihad64.rotam

import androidx.compose.ui.test.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nihad64.rotam.data.model.Plan
import com.nihad64.rotam.domain.usecase.RoutineState
import com.nihad64.rotam.data.model.PlanJson
import com.nihad64.rotam.ui.SearchScreen
import com.nihad64.rotam.ui.routines.RoutinesScreen
import com.nihad64.rotam.ui.theme.RotamTheme
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.*
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class RoutineUsabilityTest {
 @get:Rule val ui=createComposeRule()
 private fun shot(name:String){
  ui.waitForIdle();val device=InstrumentationRegistry.getInstrumentation()
  val file=File(device.targetContext.getExternalFilesDir(null),"$name.png")
  // Include the separate dialog window, not an arbitrarily ordered Compose root.
  val bitmap=checkNotNull(device.uiAutomation.takeScreenshot());file.outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
  for(command in listOf("mkdir -p /sdcard/Download/rotam-test-screenshots","cp ${file.absolutePath} /sdcard/Download/rotam-test-screenshots/$name.png")){android.os.ParcelFileDescriptor.AutoCloseInputStream(device.uiAutomation.executeShellCommand(command)).use{it.readBytes()}}
 }
 @Test fun aYearOfSchoolIsOneCollapsedSearchResult(){
  val c=RotamApp.container;val day=LocalDate.now()
  val rows=runBlocking{c.initialize();c.planRepo.create(Plan(title="Sentetik okul grubu",date=day.toString(),repeatType="daily",repeatUntil=day.plusDays(123).toString()))}
  try{
   ui.setContent{RotamTheme{Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background,contentColor=MaterialTheme.colorScheme.onBackground){SearchScreen(c,{}, {})}}}
   ui.onNode(hasSetTextAction()).performTextInput("Sentetik okul grubu")
   ui.waitUntil(10000){ui.onAllNodesWithText("1 rutin").fetchSemanticsNodes().isNotEmpty()}
   ui.onNodeWithText("1 rutin").assertExists();ui.onNodeWithText("124 plan").assertDoesNotExist()
   ui.onNodeWithTag("search-list").performScrollToNode(hasText("Günleri göster"))
   shot("10-grouped-routine-search")
   ui.onNodeWithText("Günleri göster").performClick()
   ui.onNodeWithText("Günleri gizle").assertExists()
  }finally{runBlocking{c.planRepo.deleteRoutine(rows.first().seriesId)}}
 }
 @Test fun routineMenuCanDeleteTheRuleAndStopsFutureInstances(){
  val c=RotamApp.container
  val rows=runBlocking{c.initialize();c.planRepo.create(Plan(title="Sentetik evden çıkış",date=LocalDate.now().toString(),repeatType="daily",repeatUntil=LocalDate.now().plusDays(3).toString()))}
  val sid=rows.first().seriesId
  try{
   ui.setContent{RotamTheme{Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background,contentColor=MaterialTheme.colorScheme.onBackground){RoutinesScreen(c,{}, {}, {})}}}
   ui.waitUntil(10000){ui.onAllNodesWithText("Sentetik evden çıkış").fetchSemanticsNodes().isNotEmpty()}
   ui.onNodeWithTag("routines-list").performScrollToNode(hasTestTag("routine-menu-$sid"))
   ui.onNodeWithTag("routine-menu-$sid").performClick()
   ui.onNodeWithText("Rutini sil").performClick()
   ui.onNodeWithText("Rutin silinsin mi?").assertIsDisplayed()
   shot("11-delete-routine-confirmation")
   ui.onNodeWithText("Rutini sil").performClick()
   ui.waitUntil(10000){runBlocking{c.db.seriesDao().byId(sid)?.let{RoutineState.deleted(PlanJson.decode(JSONObject(it.template)))}==true}}
   ui.waitUntil(10000){ui.onAllNodesWithText("Sentetik evden çıkış").fetchSemanticsNodes().isEmpty()}
   ui.onNodeWithText("Sentetik evden çıkış").assertDoesNotExist()
   Assert.assertTrue(runBlocking{c.db.planDao().bySeries(sid).all{it.isDeleted}})
  }finally{runBlocking{c.planRepo.deleteRoutine(sid)}}
 }
}
