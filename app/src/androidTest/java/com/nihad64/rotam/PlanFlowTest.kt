package com.nihad64.rotam

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nihad64.rotam.data.model.Plan
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.io.File

@RunWith(AndroidJUnit4::class)
class PlanFlowTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    private fun shell(command:String) {
        val automation=InstrumentationRegistry.getInstrumentation().uiAutomation
        android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).use{it.readBytes()}
    }
    private fun shot(name:String) {
        ui.mainClock.advanceTimeByFrame()
        ui.waitForIdle()
        // PixelCopy waits for this Compose root to draw, unlike shell screencap
        // which can return a stale compositor frame during navigation.
        val bitmap=ui.onRoot().captureToImage().asAndroidBitmap()
        val file=File(ui.activity.getExternalFilesDir(null),"$name.png")
        file.outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
        // Copy before UTP uninstalls the target app and removes its files.
        shell("mkdir -p /sdcard/Download/rotam-test-screenshots")
        shell("cp ${file.absolutePath} /sdcard/Download/rotam-test-screenshots/$name.png")
    }
    private fun assertAiAbove(action:String) {
        val ai=ui.onNode(hasText("Rotam AI") and hasClickAction()).getUnclippedBoundsInRoot()
        val primary=ui.onNode(hasText(action) and hasClickAction()).getUnclippedBoundsInRoot()
        org.junit.Assert.assertTrue("AI must not cover the primary action",ai.bottom<=primary.top)
    }
    @Test fun createUntimedPlanAndEditInOnePage() {
        ui.waitUntil(20000){RotamApp.container.ready.value}
        // Close only the unrelated launcher ANR on Google APIs emulator images.
        shell("am force-stop com.google.android.apps.nexuslauncher")
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
        ui.waitUntil(10000){ui.onAllNodesWithText("0 / 2 tamamlandı").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithContentDescription("Odak oturumunu başlat").assertIsDisplayed()
        shot("01-home")
        ui.onAllNodesWithText("Plan ekle").onFirst().performClick()
        ui.waitUntil(10000){ui.onAllNodesWithText("Ne yapacaksın?").fetchSemanticsNodes().isNotEmpty()}
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
        ui.waitUntil(10000){ui.onAllNodesWithText("Planını düzenle").fetchSemanticsNodes().isNotEmpty()}
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
        ui.waitUntil(10000){ui.onAllNodesWithContentDescription("Takvim").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithContentDescription("Takvim").performClick()
        ui.waitUntil(10000){ui.onAllNodesWithText("Günlerine uzaktan bir bakış.").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithText("Gün").performClick()
        shot("05-calendar-day")
        ui.onNodeWithContentDescription("Hedefler").performClick()
        ui.waitUntil(10000){ui.onAllNodesWithText("Yeni hedef").fetchSemanticsNodes().isNotEmpty()}
        shot("06-goals")
        ui.onNodeWithContentDescription("Ayarlar").performClick()
        ui.waitUntil(10000){ui.onAllNodesWithText("İsmin (selamlama için)").fetchSemanticsNodes().isNotEmpty()}
        shot("07-settings")
        ui.onNodeWithText("Gör").performClick()
        ui.waitUntil(10000){ui.onAllNodesWithText("İlerlemeni gör, ritmini koru.").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithText("Son 30 gün").performClick()
        shot("08-statistics")
    }
}
