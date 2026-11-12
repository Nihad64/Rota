package com.nihad64.rotam

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nihad64.rotam.data.model.Plan
import com.nihad64.rotam.domain.usecase.PlanTime
import com.nihad64.rotam.ui.ai.PlanSuggestionReview
import com.nihad64.rotam.ui.theme.RotamTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlanReviewAccessibilityTest {
 @get:Rule val ui=createComposeRule()
 @Test fun largeTextReviewKeepsActionsReachableAndInvalidDraftStaysOpen(){
  var applied:Plan?=null;var dismissed=false
  ui.setContent{
   val density=LocalDensity.current
   CompositionLocalProvider(LocalDensity provides Density(density.density,1.8f)) {
    RotamTheme{PlanSuggestionReview(Plan(title="Kitap oku",date="2026-11-12"),{dismissed=true}){p->PlanTime.validate(p).also{if(it==null)applied=p}}}
   }
  }
  ui.onNode(hasSetTextAction() and hasText("Plan adı")).performTextReplacement("")
  ui.onNodeWithText("Önizlemeye uygula").assertIsDisplayed().performClick()
  ui.runOnIdle{assertNull(applied);assertFalse(dismissed)}
  ui.onNode(hasSetTextAction() and hasText("Plan adı")).performScrollTo().performTextReplacement("20 sayfa oku")
  ui.onNodeWithText("Önizlemeye uygula").performClick()
  ui.runOnIdle{assertEquals("20 sayfa oku",applied!!.title);assertNull(applied!!.startTime);assertNull(applied!!.endTime);assertTrue(dismissed)}
 }
}
