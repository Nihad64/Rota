package com.nihad64.rotam
import android.view.View
import android.widget.FrameLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class WidgetLayoutTest {
 @Test fun allThreeSizesInflateAsRealRemoteViewsWithReachablePlanAndAddAction(){
  val instrumentation=InstrumentationRegistry.getInstrumentation();val context=instrumentation.targetContext
  instrumentation.runOnMainSync {
   for(layout in listOf(R.layout.widget_small,R.layout.widget_today,R.layout.widget_large)) {
    val remote=RemoteViews(context.packageName,layout)
    remote.setTextViewText(R.id.widget_title,"Sentetik plan")
    remote.setTextViewText(R.id.widget_time,"Gün içinde")
    remote.setTextViewText(R.id.widget_progress,"1 / 3")
    remote.setViewVisibility(R.id.widget_done,View.GONE)
    remote.setViewVisibility(R.id.widget_awaiting,View.GONE)
    remote.setContentDescription(R.id.widget_add,"AI ile plan ekle")
    val root=remote.apply(context,FrameLayout(context))
    val width=((if(layout==R.layout.widget_small)170 else 350)*context.resources.displayMetrics.density).toInt()
    val height=((if(layout==R.layout.widget_small)190 else if(layout==R.layout.widget_large)330 else 230)*context.resources.displayMetrics.density).toInt()
    root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));root.layout(0,0,width,height)
    assertEquals("Sentetik plan",root.findViewById<TextView>(R.id.widget_title).text.toString())
    val title=root.findViewById<TextView>(R.id.widget_title);assertTrue(title.height>0)
    assertEquals("AI ile plan ekle",root.findViewById<View>(R.id.widget_add).contentDescription)
    assertTrue(root.findViewById<View>(R.id.widget_add).height>=48*context.resources.displayMetrics.density-1)
   }
  }
 }
}
