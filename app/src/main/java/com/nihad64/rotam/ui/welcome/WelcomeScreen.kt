package com.nihad64.rotam.ui.welcome

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nihad64.rotam.ui.home.RotamLogo
import com.nihad64.rotam.ui.settings.SettingsViewModel
import com.nihad64.rotam.ui.theme.*

@Composable fun WelcomeScreen(vm:SettingsViewModel,onContinue:()->Unit,onPrivacy:()->Unit){
 val context=LocalContext.current
 val account by vm.account.collectAsStateWithLifecycle()
 val busy by vm.authBusy.collectAsStateWithLifecycle()
 val error by vm.authMessage.collectAsStateWithLifecycle()
 LaunchedEffect(account){if(account!=null)onContinue()}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=28.dp,vertical=24.dp),verticalArrangement=Arrangement.spacedBy(22.dp)){
  RotamLogo(44.dp)
  Text("Gününe yer aç.",fontSize=40.sp,lineHeight=44.sp,fontWeight=FontWeight.Bold)
  Text("Aklındakini anlat. Birlikte planlayalım, kendi ritminde ilerle.",style=MaterialTheme.typography.bodyLarge,color=SubtleText)
  Canvas(Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(topStart=32.dp,topEnd=32.dp,bottomStart=32.dp,bottomEnd=72.dp)).background(Surface)){
   val path=Path().apply{moveTo(size.width*.15f,size.height*1.1f);cubicTo(size.width*.8f,size.height*.68f,size.width*.1f,size.height*.6f,size.width*.62f,size.height*.24f)}
   drawPath(path,Primary,style=Stroke(width=24.dp.toPx(),cap=StrokeCap.Round))
   val c=androidx.compose.ui.geometry.Offset(size.width*.62f,size.height*.24f)
   val diamond=Path().apply{moveTo(c.x,c.y-12.dp.toPx());lineTo(c.x+12.dp.toPx(),c.y);lineTo(c.x,c.y+12.dp.toPx());lineTo(c.x-12.dp.toPx(),c.y);close()}
   drawPath(diamond,Accent)
  }
  Button(onClick={vm.signIn(context)},enabled=!busy&&vm.accountConfigured,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp),colors=ButtonDefaults.buttonColors(containerColor=Primary,contentColor=Background)){
   if(busy){CircularProgressIndicator(Modifier.size(20.dp),strokeWidth=2.dp);Spacer(Modifier.width(12.dp))}
   Text(if(busy)"Hesabın bağlanıyor…"else"Google ile devam et")
  }
  error?.let{Text(it,color=OnSurface,style=MaterialTheme.typography.bodyMedium)}
  TextButton(onClick=onContinue,enabled=!busy,modifier=Modifier.fillMaxWidth()){Text("Önce uygulamaya göz at",color=SubtleText)}
  TextButton(onClick=onPrivacy,modifier=Modifier.fillMaxWidth()){Text("Verilerim nasıl kullanılıyor?",color=SubtleText)}
 }
}
