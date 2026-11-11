package com.nihad64.rotam
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nihad64.rotam.data.model.Plan
import com.nihad64.rotam.ui.SearchScreen
import com.nihad64.rotam.ui.theme.RotamTheme
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class SearchInteractionTest {
 @get:Rule val ui=createComposeRule()
 @Test fun sameFieldFindsTurkishDatesAndCountsWithoutModelThenRequiresSharingForComplexChange(){
  val c=RotamApp.container
  val p=runBlocking{c.initialize();c.settings.setSharePlans(false);c.planRepo.create(Plan(title="Sentetik Mayıs arama planı",date="2024-05-10")).single()}
  var asked=0
  try{
   ui.setContent{RotamTheme{SearchScreen(c,{}, {},onAsk={asked++})}}
   ui.onNode(hasSetTextAction()).performTextInput("Mayıs 2024")
   ui.onNode(hasSetTextAction()).performImeAction()
   ui.waitUntil(10000){ui.onAllNodesWithText(p.title).fetchSemanticsNodes().isNotEmpty()}
   ui.onNodeWithTag("search-list").performScrollToNode(hasText(p.title));ui.onNodeWithText(p.title).assertIsDisplayed()
   ui.onNode(hasSetTextAction()).performScrollTo().performTextReplacement("Mayıs 2024 kaç plan var")
   ui.onNode(hasSetTextAction()).performImeAction()
   ui.waitUntil(10000){ui.onAllNodesWithText("Bu cihazdaki kayıtlara göre",substring=true).fetchSemanticsNodes().isNotEmpty()}
   ui.runOnIdle{Assert.assertEquals(0,asked)}
   ui.onNode(hasSetTextAction()).performScrollTo().performTextReplacement("Sporu cumaya taşı")
   ui.onNode(hasSetTextAction()).performImeAction()
   ui.waitUntil(10000){ui.onAllNodesWithText("Kayıtlarına bakarak yardımcı olayım mı?").fetchSemanticsNodes().isNotEmpty()}
   ui.onNodeWithText("Vazgeç").performClick()
   ui.onNode(hasSetTextAction()).assertTextContains("Sporu cumaya taşı")
   ui.runOnIdle{Assert.assertEquals(0,asked)}
   Assert.assertFalse(runBlocking{c.settings.current().sharePlans})
  }finally{runBlocking{c.planRepo.delete(c.planRepo.byId(p.id)!!)}}
 }
}
