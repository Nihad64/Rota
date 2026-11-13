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
        val bitmap=ui.onAllNodes(isRoot()).onLast().captureToImage().asAndroidBitmap()
        val file=File(ui.activity.getExternalFilesDir(null),"$name.png")
        file.outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
        // Copy before UTP uninstalls the target app and removes its files.
        shell("mkdir -p /sdcard/Download/rotam-test-screenshots")
        shell("cp ${file.absolutePath} /sdcard/Download/rotam-test-screenshots/$name.png")
    }
    @org.junit.After fun finalScreenshot(){runCatching{shot("99-test-end")}}
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
        ui.waitUntil(10000){ui.onAllNodesWithText("Video düzenleme").fetchSemanticsNodes().isNotEmpty()}
        ui.waitUntil(10000){ui.onAllNodesWithText("0 / 2 tamamlandı").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithContentDescription("Plan ayrıntılarını aç").assertIsDisplayed()
        ui.onNodeWithText("Başlat").assertDoesNotExist()
        ui.onNodeWithText("Rotam AI").assertDoesNotExist()
        shot("01-home")
        ui.onNodeWithTag("home-list").performScrollToNode(hasText("Plan ekle"))
        ui.onAllNodesWithText("Plan ekle").onFirst().performClick()
        ui.onNodeWithText("Birlikte planlayalım").assertIsDisplayed()
        shot("02-ai-first")
        ui.onNodeWithText("Kendin oluştur").performClick()
        ui.waitUntil(10000){ui.onAllNodesWithText("Yeni plan").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithText("Yeni plan").assertIsDisplayed()
        ui.onNodeWithText("Planı kaydet").assertIsDisplayed()
        shot("02-create")
        ui.onNode(hasSetTextAction()).performTextInput("Saatsiz test planı")
        ui.waitUntil(10000){ui.onNode(hasSetTextAction()).isDisplayed()}
        ui.onNode(hasSetTextAction()).assertIsDisplayed()
        ui.onNodeWithText("Zaman").performScrollTo().assertIsDisplayed()
        // Optional endpoints stay collapsed until requested. Opening them must
        // not assign a default time to an otherwise untimed plan.
        ui.onNodeWithText("Saat").performScrollTo().performClick()
        ui.onAllNodesWithText("Saat yok").onFirst().performScrollTo().assertIsDisplayed()
        shot("03-optional-times")
        ui.onNodeWithText("Planı kaydet").performClick()
        ui.waitUntil(10000){runBlocking{RotamApp.container.db.planDao().all().any{it.title=="Saatsiz test planı"}}}
        val plan=runBlocking{RotamApp.container.db.planDao().all().first{it.title=="Saatsiz test planı"}}
        org.junit.Assert.assertNull(plan.startTime);org.junit.Assert.assertNull(plan.endTime)
        ui.waitUntil(10000){ui.onAllNodesWithTag("home-list").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithTag("home-list").performScrollToIndex(0)
        ui.onNodeWithContentDescription("Planlarda ara").performClick()
        ui.onNode(hasSetTextAction()).performTextInput("Saatsiz test planı")
        ui.waitUntil(10000){ui.onNode(hasSetTextAction()).isDisplayed()}
        ui.onNode(hasSetTextAction()).assertIsDisplayed()
        ui.onNodeWithTag("search-list").performScrollToNode(hasText("Saatsiz test planı") and !hasSetTextAction())
        ui.onNode(hasText("Saatsiz test planı") and !hasSetTextAction()).performScrollTo().performClick()
        ui.waitUntil(10000){ui.onAllNodesWithText("Plan detayı").fetchSemanticsNodes().isNotEmpty()}
        shot("04-plan-detail")
        ui.onNodeWithText("Odaklan").performScrollTo().performClick()
        ui.onNodeWithText("Başla").performScrollTo().performClick()
        ui.waitUntil(5000){RotamApp.container.focus.active.value==plan.id}
        ui.onNodeWithText("Bitir ve özeti gör").performScrollTo().performClick()
        ui.waitUntil(10000){ui.onAllNodesWithText("Oturum özeti").fetchSemanticsNodes().isNotEmpty()}
        org.junit.Assert.assertFalse(runBlocking{RotamApp.container.planRepo.byId(plan.id)!!.isCompleted})
        shot("04-focus-summary")
        ui.onNodeWithText("Sonra devam edeceğim").performScrollTo().performClick()
        ui.waitUntil(10000){ui.onAllNodesWithText("Plan detayı").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithText("Düzenle").performClick()
        ui.waitUntil(10000){ui.onAllNodesWithText("Planını düzenle").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithText("Planını düzenle").assertIsDisplayed()
        ui.onNodeWithText("Değişiklikleri kaydet").assertIsDisplayed()
        shot("04-edit")
        ui.onNodeWithText("Zaman").performScrollTo().assertIsDisplayed()
        ui.onNodeWithText("Tekrar").performScrollTo().assertIsDisplayed()
        ui.onNode(hasText("Saatsiz test planı") and hasSetTextAction()).performScrollTo().performTextReplacement("Düzenlenmiş saatsiz plan")
        // Invoke the already-verified button without racing the soft keyboard's
        // platform inset animation after editing a multiline field.
        ui.onNodeWithText("Değişiklikleri kaydet").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick){it()}
        ui.waitUntil(10000){runBlocking{RotamApp.container.db.planDao().all().any{it.id==plan.id&&it.title=="Düzenlenmiş saatsiz plan"&&it.startTime==null&&it.endTime==null}}}
        ui.waitUntil(10000){ui.onAllNodesWithText("Planını düzenle").fetchSemanticsNodes().isEmpty()}
        ui.onNodeWithContentDescription("Geri").performClick()
        ui.waitUntil(10000){ui.onAllNodesWithTag("search-list").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithContentDescription("Geri").performClick()
        ui.waitUntil(10000){ui.onAllNodesWithContentDescription("Takvim").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithContentDescription("Takvim").performClick()
        ui.waitUntil(10000){ui.onAllNodesWithText("Günlerine uzaktan bir bakış.").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithText("Gün").performClick()
        shot("05-calendar-day")
        ui.onNodeWithContentDescription("Rutinler").performClick()
        ui.waitUntil(10000){ui.onAllNodesWithText("Yeni rutin oluştur").fetchSemanticsNodes().isNotEmpty()}
        shot("06-routines")
        ui.onNodeWithContentDescription("Ayarlar").performClick()
        ui.waitUntil(10000){ui.onAllNodesWithTag("account-settings-entry").fetchSemanticsNodes().isNotEmpty()}
        shot("07-settings")
        ui.onNodeWithContentDescription("Takvim").performClick()
        ui.onNodeWithText("İlerlemem").performClick()
        ui.waitUntil(10000){ui.onAllNodesWithText("İlerlemeni gör, ritmini koru.").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithText("Ay").performClick()
        shot("08-statistics")
    }
}
