package com.nihad64.rotam.ui.components
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalView
import com.nihad64.rotam.data.prefs.Settings
val LocalRotamSettings=staticCompositionLocalOf { Settings() }
@Composable fun rememberFeedback():()->Unit {
 val view=LocalView.current;val settings=LocalRotamSettings.current
 return { if(settings.haptics)view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }
}

/** Local, optional success sound. Never overrides silent/vibrate mode. */
@Composable fun rememberCompletionFeedback():()->Unit {
 val context=androidx.compose.ui.platform.LocalContext.current
 val view=LocalView.current
 val settings=LocalRotamSettings.current
 val audio=remember(context){context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager}
 val pool=remember{android.media.SoundPool.Builder().setMaxStreams(1).setAudioAttributes(android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()}
 var sound by remember{mutableIntStateOf(0)}
 var loaded by remember{mutableStateOf(false)}
 DisposableEffect(pool){
  pool.setOnLoadCompleteListener{_,_,status->loaded=status==0}
  sound=pool.load(context,com.nihad64.rotam.R.raw.rotam_confirm,1)
  onDispose{pool.setOnLoadCompleteListener(null);pool.release()}
 }
 return {
  if(settings.haptics)view.performHapticFeedback(if(android.os.Build.VERSION.SDK_INT>=30)HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY)
  if(settings.uiSounds&&loaded&&audio.ringerMode==android.media.AudioManager.RINGER_MODE_NORMAL)pool.play(sound,.35f,.35f,1,0,1f)
 }
}
