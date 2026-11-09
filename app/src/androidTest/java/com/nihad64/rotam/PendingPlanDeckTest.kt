package com.nihad64.rotam
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nihad64.rotam.data.model.Plan
import com.nihad64.rotam.domain.usecase.PlanOutcome
import com.nihad64.rotam.ui.home.*
import com.nihad64.rotam.ui.theme.RotamTheme
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import java.time.LocalDate
@RunWith(AndroidJUnit4::class)
class PendingPlanDeckTest {
 @get:Rule val ui=createComposeRule()
 @Test fun explicitPartialAndUndoThenLeftSwipe(){
  val c=RotamApp.container
  val p=runBlocking{c.initialize();while(c.notices.tryReceive().isSuccess){};c.planRepo.create(Plan(title="Kart testi: 20 sayfa",date=LocalDate.now().minusDays(1).toString())).single()}
  ui.setContent{val all by c.planRepo.observeAll().collectAsState(initial=emptyList());val vm=remember{HomeViewModel(c)}
   RotamTheme{PendingPlanDeck(all.filter{it.id==p.id&&PlanOutcome.status(it)==PlanOutcome.Status.UNKNOWN},vm)}
  }
  ui.waitUntil(10000){ui.onAllNodes(hasText("Kısmen yaptım") and hasClickAction()).fetchSemanticsNodes().size==1}
  ui.waitForIdle()
  ui.onNode(hasText("Kısmen yaptım") and hasClickAction()).performClick()
  ui.onNodeWithText("Yaptığın miktar").performTextInput("10")
  ui.onNodeWithText("Toplam hedef").performTextInput("20")
  ui.onNodeWithText("Kaydet").performClick()
  ui.waitUntil(10000){runBlocking{PlanOutcome.status(c.planRepo.byId(p.id)!!)==PlanOutcome.Status.PARTIAL}}
  runBlocking{Assert.assertEquals(0.5,PlanOutcome.fraction(c.planRepo.byId(p.id)!!)!!,0.001);c.notices.receive().undo!!.invoke()}
  ui.waitUntil(10000){ui.onAllNodesWithText("Yapabildin mi?").fetchSemanticsNodes().isNotEmpty()}
  ui.onNodeWithText("Yapabildin mi?").performTouchInput{swipeLeft()}
  ui.waitUntil(10000){runBlocking{c.planRepo.byId(p.id)!!.isSkipped}}
  runBlocking{Assert.assertFalse(c.planRepo.byId(p.id)!!.isCompleted);c.planRepo.delete(c.planRepo.byId(p.id)!!);while(c.notices.tryReceive().isSuccess){}}
 }
}
