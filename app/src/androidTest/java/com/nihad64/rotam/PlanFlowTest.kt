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
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class PlanFlowTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    private fun shell(command:String) {
        val automation=InstrumentationRegistry.getInstrumentation().uiAutomation
        android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).use{it.readBytes()}
    }
    private fun shot(name:String) {
        // The Google APIs emulator can leave an unrelated Pixel Launcher ANR above
        // the test app. Stop only that launcher; never suppress Rotam errors.
        shell("am force-stop com.google.android.apps.nexuslauncher")
        ui.waitForIdle()
        // UTP uninstalls the app after testing, removing its external-files directory.
        // Keep review screenshots in a shell-owned directory that survives cleanup.
        for(command in listOf("mkdir -p /sdcard/Download/rotam-test-screenshots", "screencap -p /sdcard/Download/rotam-test-screenshots/$name.png")) {
            shell(command)
        }
    }
    private fun assertAiAbove(action:String) {
        val ai=ui.onNode(hasText("Rotam AI") and hasClickAction()).getUnclippedBoundsInRoot()
        val primary=ui.onNode(hasText(action) and hasClickAction()).getUnclippedBoundsInRoot()
        org.junit.Assert.assertTrue("AI must not cover the primary action",ai.bottom<=primary.top)
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
        ui.waitUntil(10000){ui.onAllNodesWithText("Video düzenleme").fetchSemanticsNodes().isNotEmpty()}
        shot("01-home")
        ui.onAllNodesWithText("Plan ekle").onFirst().performClick()
        ui.onNodeWithText("Ne yapacaksın?").assertIsDisplayed()
        assertAiAbove("Devam et")
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
        ui.waitUntil(10000){ui.onAllNodesWithTag("home-list").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithTag("home-list").performScrollToIndex(0)
        ui.onNodeWithContentDescription("Planlarda ara").performClick()
        ui.onNode(hasSetTextAction()).performTextInput("Saatsiz test planı")
        ui.waitUntil(10000){ui.onAllNodes(hasText("Saatsiz test planı") and !hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()}
        ui.onNode(hasText("Saatsiz test planı") and !hasSetTextAction()).performScrollTo().performClick()
        ui.onNodeWithText("Planını düzenle").assertIsDisplayed()
        assertAiAbove("Değişiklikleri kaydet")
        shot("04-edit")
        ui.onNode(hasText("Saatsiz test planı") and hasSetTextAction()).performTextReplacement("Düzenlenmiş saatsiz plan")
        ui.onNodeWithText("Ne zaman?").performScrollTo().assertIsDisplayed()
        ui.onNodeWithText("Tekrar").performScrollTo().assertIsDisplayed()
        ui.onNodeWithText("Değişiklikleri kaydet").performClick()
        ui.waitUntil(10000){runBlocking{RotamApp.container.db.planDao().all().any{it.id==plan.id&&it.title=="Düzenlenmiş saatsiz plan"&&it.startTime==null&&it.endTime==null}}}
        ui.waitUntil(10000){ui.onAllNodesWithText("Planını düzenle").fetchSemanticsNodes().isEmpty()}
        ui.onNodeWithContentDescription("Geri").performClick()
        ui.onNodeWithContentDescription("Takvim").performClick()
        ui.onNodeWithText("Gün").performClick()
        shot("05-calendar-day")
        ui.onNodeWithContentDescription("Hedefler").performClick()
        shot("06-goals")
        ui.onNodeWithContentDescription("Ayarlar").performClick()
        shot("07-settings")
        ui.onNodeWithText("Gör").performClick()
        ui.onNodeWithText("Son 30 gün").performClick()
        shot("08-statistics")
    }
}
