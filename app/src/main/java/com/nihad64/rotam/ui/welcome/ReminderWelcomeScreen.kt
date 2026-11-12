package com.nihad64.rotam.ui.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.nihad64.rotam.ui.components.*
import com.nihad64.rotam.ui.home.RotamLogo
import com.nihad64.rotam.ui.theme.*

@Composable fun ReminderWelcomeScreen(busy:Boolean=false,error:String?=null,onEnable:()->Unit,onSkip:()->Unit){
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),verticalArrangement=Arrangement.spacedBy(24.dp)){
  RotamLogo(42.dp)
  Text("Zamanı gelince\nhaberin olsun.",style=MaterialTheme.typography.displaySmall)
  Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart=36.dp,topEnd=36.dp,bottomStart=36.dp,bottomEnd=72.dp)).background(Brush.linearGradient(listOf(HeroSurface,Surface))).padding(40.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(18.dp)){
   Icon(Icons.Outlined.NotificationsNone,null,tint=Primary,modifier=Modifier.size(98.dp));Diamond(16.dp)
  }
  Text("Planlarını, senin ritmine göre hatırlatalım.",style=MaterialTheme.typography.headlineSmall)
  Text("Plan saatine ve sessiz saatlerine göre hatırlatır. Başladığında veya tamamladığında takip durur; bildirimleri kapatmana saygı duyar.",color=SubtleText,style=MaterialTheme.typography.bodyLarge)
  Text("Bir sonraki adımda telefonunun bildirim izni açılır. Şimdi değil dersen Ayarlar → Hatırlatmalar'dan açabilirsin.",color=SubtleText,style=MaterialTheme.typography.bodyMedium)
  error?.let{Text(it,color=Danger)}
  PrimaryButton(if(busy)"Devam ediliyor…"else"Hatırlatmaları etkinleştir",onEnable,Modifier.fillMaxWidth(),enabled=!busy)
  TextButton(onClick=onSkip,enabled=!busy,modifier=Modifier.fillMaxWidth()){Text("Şimdi değil",color=SubtleText)}
 }
}
