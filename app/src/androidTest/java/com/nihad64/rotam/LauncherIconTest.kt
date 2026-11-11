package com.nihad64.rotam
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class LauncherIconTest {
 private fun raster(drawable:Drawable):Bitmap=Bitmap.createBitmap(1024,1024,Bitmap.Config.ARGB_8888).also{drawable.setBounds(0,0,1024,1024);drawable.draw(Canvas(it))}
 @Test fun packageManagerResolvesOriginalSilhouetteWithPureWhiteRAndThemedMask(){
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val component=android.content.ComponentName(context.packageName,"com.rota.app.MainActivity")
  val launcher=context.packageManager.getActivityInfo(component,0)
  assertEquals("com.nihad64.rotam.MainActivity",launcher.targetActivity)
  val resolved=context.packageManager.resolveActivity(android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_LAUNCHER).setPackage(context.packageName),0)
  assertEquals(component.className,resolved?.activityInfo?.name)
  val icon=context.packageManager.getActivityIcon(component)
  assertTrue(icon is AdaptiveIconDrawable)
  val foreground=(icon as AdaptiveIconDrawable).foreground
  assertTrue(foreground is VectorDrawable)
  val bitmap=raster(foreground)
  val round=context.getDrawable(R.mipmap.ic_launcher_round)
  assertTrue(round is AdaptiveIconDrawable)
  val roundBitmap=raster((round as AdaptiveIconDrawable).foreground)
  assertTrue(bitmap.sameAs(roundBitmap))
  val expected=android.graphics.BitmapFactory.decodeResource(context.resources,R.drawable.rotam_launcher_mask)
  if(android.os.Build.VERSION.SDK_INT>=33){
   for(candidate in listOf(icon,round)){
    val mask=candidate.monochrome
    assertTrue("Themed icon must retain the approved silhouette",mask is BitmapDrawable)
    assertTrue(expected.sameAs((mask as BitmapDrawable).bitmap))
   }
  }
  assertEquals("The R must be pure white, not ivory",Color.WHITE,bitmap.getPixel(330,400))
  assertEquals("The orange diamond must remain",Color.rgb(255,128,63),bitmap.getPixel(410,388))
  var intersection=0;var union=0
  for(y in 0 until 1024)for(x in 0 until 1024){
   val a=Color.alpha(bitmap.getPixel(x,y))>=128;val b=Color.alpha(expected.getPixel(x,y))>=128
   if(a&&b)intersection++;if(a||b)union++
  }
  assertTrue("Native vector must preserve at least 99.5% of the original silhouette",intersection.toDouble()/union>=0.995)
 }
}
