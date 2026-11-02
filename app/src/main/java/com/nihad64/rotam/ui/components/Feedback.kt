package com.nihad64.rotam.ui.components
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalView
import com.nihad64.rotam.data.prefs.Settings
val LocalRotamSettings=staticCompositionLocalOf { Settings() }
@Composable fun rememberFeedback():()->Unit {
 val view=LocalView.current;val settings=LocalRotamSettings.current
 return { if(settings.haptics)view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);if(settings.uiSounds)view.playSoundEffect(SoundEffectConstants.CLICK) }
}
