package com.nihad64.rotam

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nihad64.rotam.data.model.Plan
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class PlanFlowTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    private fun shot(name:String) {
        ui.waitForIdle()
        val bitmap=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val dir=File(ui.activity.getExternalFilesDir(null),"screenshots").apply{mkdirs()}
        File(dir,"$name.png").outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
        bitmap.recycle()
    }
    @Test fun createUntimedPlanAndEditInOnePage() {
        ui.waitUntil(20000){RotamApp.container.ready.value}
        runBlocking {
            val c=RotamApp.container
            val legacy=c.db.planDao().all().firstOrNull{it.title=="Önceki sürümden plan"}
            org.junit.Assert.assertNotNull("The in-place upgrade must preserve the v2 fixture",legacy)
            org.junit.Assert.assertTrue(legacy!!.extraData.contains("Korunan not"))
            org.junit.Assert.assertEquals("10:00",legacy.startTime)
            c.planRepo.clear()
            c.planRepo.create(Plan(title="Video düzenleme",date=LocalDate.now().toString(),startTime="10:00",endTime="11:00",category="video"))
            c.planRepo.create(Plan(title="Almanca çalış",date=LocalDate.now().toString(),category="ogrenme"))
        }
        ui.waitUntil(10000){ui.onAllNodesWithText("Günün akışı").fetchSemanticsNodes().isNotEmpty()}
        shot("01-home")
        ui.onAllNodesWithText("Plan ekle").onFirst().performClick()
        ui.onNodeWithText("Ne yapacaksın?").assertIsDisplayed()
        shot("02-create")
        ui.onNode(hasSetTextAction()).performTextInput("Saatsiz test planı")
        ui.onNodeWithText("Devam et").performClick()
        ui.onNodeWithText("Sana göre ayrıntılar").assertIsDisplayed()
        ui.onNodeWithText("Devam et").performClick()
        ui.onNodeWithText("Ne zaman?").assertIsDisplayed()
        ui.onNodeWithText("Saat yok").assertIsDisplayed()
        shot("03-optional-times")
        ui.onNodeWithText("Devam et").performClick()
        ui.onNodeWithText("Devam et").performClick()
        ui.onNodeWithText("Planı oluştur").performClick()
        ui.waitUntil(10000){runBlocking{RotamApp.container.db.planDao().all().any{it.title=="Saatsiz test planı"}}}
        val plan=runBlocking{RotamApp.container.db.planDao().all().first{it.title=="Saatsiz test planı"}}
        org.junit.Assert.assertNull(plan.startTime);org.junit.Assert.assertNull(plan.endTime)
        ui.onNodeWithContentDescription("Planlarda ara").performClick()
        ui.onNode(hasSetTextAction()).performTextInput("Saatsiz test planı")
        ui.onNode(hasText("Saatsiz test planı") and !hasSetTextAction()).performScrollTo().performClick()
        ui.onNodeWithText("Planını düzenle").assertIsDisplayed()
        shot("04-edit")
        ui.onNodeWithText("Ne zaman?").performScrollTo().assertIsDisplayed()
        ui.onNodeWithText("Tekrar").performScrollTo().assertIsDisplayed()
    }
}
