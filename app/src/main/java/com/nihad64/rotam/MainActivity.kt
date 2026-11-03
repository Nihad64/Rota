package com.nihad64.rotam
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.nihad64.rotam.ui.RotamRoot
import com.nihad64.rotam.ui.theme.RotamTheme
class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);enableEdgeToEdge(statusBarStyle=SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),navigationBarStyle=SystemBarStyle.dark(android.graphics.Color.TRANSPARENT));setContent{RotamTheme{RotamRoot()}}}
 override fun onResume(){super.onResume();lifecycleScope.launch{runCatching{RotamApp.container.initialize();RotamApp.container.scheduler.rescheduleAll()}}}
}
