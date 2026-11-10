package com.nihad64.rotam.ui.theme

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.nihad64.rotam.ui.components.LocalRotamSettings

object RotamMotion {
 const val Feedback=120
 const val Replace=180
 const val Navigate=260
 const val Exit=160
}

/** Follow OS motion preferences without depending on animation completion for state. */
@Composable fun rememberMotionEnabled():Boolean {
 val resolver=LocalContext.current.contentResolver
 val preference=LocalRotamSettings.current.animations
 fun enabled()=Settings.Global.getFloat(resolver,Settings.Global.ANIMATOR_DURATION_SCALE,1f)>0f
 var systemEnabled by remember(resolver){mutableStateOf(enabled())}
 DisposableEffect(resolver){
  val observer=object:ContentObserver(Handler(Looper.getMainLooper())){
   override fun onChange(selfChange:Boolean){systemEnabled=enabled()}
  }
  resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),false,observer)
  onDispose{resolver.unregisterContentObserver(observer)}
 }
 return preference&&systemEnabled
}
