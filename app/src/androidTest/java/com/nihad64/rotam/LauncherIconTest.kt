package com.nihad64.rotam
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class LauncherIconTest {
 @Test fun packageManagerResolvesApprovedBitmapAndThemedMask(){
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val icon=context.packageManager.getApplicationIcon(context.packageName)
  assertTrue(icon is AdaptiveIconDrawable)
  val foreground=(icon as AdaptiveIconDrawable).foreground
  assertTrue(foreground is BitmapDrawable)
  val bitmap=(foreground as BitmapDrawable).bitmap
  assertEquals(1024,bitmap.width);assertEquals(1024,bitmap.height)
  // The original ivory R and orange diamond occupy these known source coordinates.
  val ivory=bitmap.getPixel(330,400);val orange=bitmap.getPixel(410,388)
  assertTrue(android.graphics.Color.red(ivory)>200)
  assertTrue(android.graphics.Color.red(orange)>200&&android.graphics.Color.green(orange) in 60..180)
 }
}
