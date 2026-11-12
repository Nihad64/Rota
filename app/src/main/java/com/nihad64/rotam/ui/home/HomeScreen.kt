package com.nihad64.rotam.ui.home
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nihad64.rotam.R
import com.nihad64.rotam.data.model.*
import com.nihad64.rotam.domain.usecase.PlanTime
import com.nihad64.rotam.ui.TrFormat
import com.nihad64.rotam.ui.components.*
import com.nihad64.rotam.ui.theme.*
import java.util.Locale
@Composable fun HomeScreen(vm:HomeViewModel,onNewPlan:()->Unit,onEdit:(Long)->Unit,onFocus:(Long)->Unit,onSearch:()->Unit,onStats:()->Unit,onSettings:()->Unit,onPostpone:(Long)->Unit={onEdit(it)}){
 val s by vm.state.collectAsStateWithLifecycle();val hero=s.hero
 LazyColumn(Modifier.fillMaxSize().testTag("home-list"),contentPadding=PaddingValues(start=24.dp,end=24.dp,top=18.dp,bottom=88.dp),verticalArrangement=Arrangement.spacedBy(24.dp)){
  item{Row(verticalAlignment=Alignment.CenterVertically){Icon(painterResource(R.drawable.rotam_mark_vector),"Rotam",Modifier.width(30.dp).height(34.dp),tint=Color.Unspecified);Text("rotam",fontSize=25.sp,fontWeight=FontWeight.Medium,modifier=Modifier.padding(start=8.dp).weight(1f));IconButton(onClick=onSearch){Icon(Icons.Outlined.Search,"Planlarda ara",tint=SubtleText)}}}
  item {
   Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
    Text("${TrFormat.dayName(s.today)}, ${TrFormat.dayMonth(s.today)}".uppercase(Locale.forLanguageTag("tr")),color=SubtleText,letterSpacing=2.sp,style=MaterialTheme.typography.labelMedium)
    Text("Bugün,\nsenin ritmin.",fontSize=36.sp,lineHeight=40.sp,letterSpacing=(-1.2).sp,fontWeight=FontWeight.SemiBold)
    if(s.total>0) {
     Row(Modifier.fillMaxWidth().clickable(onClick=onStats).heightIn(min=48.dp).padding(top=8.dp),verticalAlignment=Alignment.CenterVertically) {
      Text("${s.done} / ${s.total} tamamlandı",color=SubtleText,modifier=Modifier.weight(1f))
      Icon(Icons.Outlined.BarChart,"İstatistikler",tint=SubtleText,modifier=Modifier.size(20.dp))
     }
     SegmentedProgress(s.done,s.total)
    }
   }
  }
  item{Column{if(hero!=null)HeroPlan(hero,s.label(hero),{onEdit(hero.id)},{onFocus(hero.id)})else if(s.missed.isEmpty())Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart=32.dp,topEnd=32.dp,bottomStart=32.dp,bottomEnd=70.dp)).background(Brush.linearGradient(listOf(HeroSurface,Surface))).padding(26.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
   Row(verticalAlignment=Alignment.CenterVertically){Diamond();Spacer(Modifier.width(10.dp));Text("BİRLİKTE PLANLAYALIM",color=SubtleText,style=MaterialTheme.typography.labelMedium)}
   Text(if(s.total>0)"Yeni bir şeye yer aç." else "İlk planını birlikte oluşturalım.",color=Primary,style=MaterialTheme.typography.headlineMedium)
   Text("Yaz veya söyle. Eklenmeden önce sen onayla.",color=SubtleText,style=MaterialTheme.typography.bodyMedium)
   PrimaryButton("Plan ekle",onNewPlan,icon=Icons.Outlined.ArrowForward)
  }
  PendingPlanDeck(s.missed,vm,onPostpone,24.dp)
  }}
  item{SectionTitle(if(hero!=null)"Günün devamı"else"Günün akışı",trailing={TextButton(onClick=onNewPlan){Icon(Icons.Outlined.AddCircleOutline,null,Modifier.size(18.dp));Spacer(Modifier.width(6.dp));Text("Plan ekle",color=SubtleText)}})}
  val visible=s.todayPlans.filter{it.uid!=hero?.uid&&!s.missed.any{m->m.uid==it.uid}}
  val timed=visible.filter{it.startTime!=null||it.endTime!=null}
  items(timed,key={it.uid}){p->PlanCard(p,{vm.toggle(p)},{onEdit(p.id)})}
  val untimed=visible.filter{it.startTime==null&&it.endTime==null}
  if(untimed.isNotEmpty())item{Text("BUGÜN İÇİNDE",style=MaterialTheme.typography.labelSmall,color=SubtleText,letterSpacing=1.5.sp)}
  items(untimed,key={it.uid}){p->PlanCard(p,{vm.toggle(p)},{onEdit(p.id)})}
  item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){Surface(onClick=onNewPlan,shape=CircleShape,color=Surface){Row(Modifier.padding(horizontal=18.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically){Diamond();Spacer(Modifier.width(10.dp));Text("Rotam AI",style=MaterialTheme.typography.labelLarge)}}}}
 }
}
@Composable private fun HeroPlan(p:Plan,label:String,edit:()->Unit,focus:()->Unit){
 val density=LocalDensity.current
 val shape=GenericShape{size,_->val w=size.width;val h=size.height;val r=with(density){40.dp.toPx()};val notch=with(density){90.dp.toPx()};moveTo(r,0f);lineTo(w-r,0f);quadraticBezierTo(w,0f,w,r);lineTo(w,h-notch-with(density){28.dp.toPx()});quadraticBezierTo(w,h-notch,w-with(density){32.dp.toPx()},h-notch);cubicTo(w-notch,h-notch,w-notch,h-with(density){50.dp.toPx()},w-notch,h-with(density){36.dp.toPx()});quadraticBezierTo(w-notch,h,w-notch-with(density){36.dp.toPx()},h);lineTo(r,h);quadraticBezierTo(0f,h,0f,h-r);lineTo(0f,r);quadraticBezierTo(0f,0f,r,0f);close()}
 Box(Modifier.fillMaxWidth()){
  Column(Modifier.fillMaxWidth().heightIn(min=230.dp).clip(shape).background(Brush.linearGradient(listOf(SurfaceVariant,Surface))).clickable(onClick=edit).padding(26.dp).padding(end=66.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
   Row(verticalAlignment=Alignment.CenterVertically){Diamond();Spacer(Modifier.width(10.dp));Text(label,color=SubtleText,style=MaterialTheme.typography.labelMedium,letterSpacing=1.3.sp)}
   Text(p.title,fontSize=28.sp,lineHeight=33.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(top=4.dp),maxLines=4)
   Text(PlanTime.label(p),color=SubtleText,style=MaterialTheme.typography.bodyLarge);Text(p.categoryEnum.label,color=SubtleText,style=MaterialTheme.typography.bodyMedium);Spacer(Modifier.height(12.dp))
  }
  Column(Modifier.align(Alignment.BottomEnd).width(80.dp).padding(bottom=5.dp),horizontalAlignment=Alignment.CenterHorizontally){FilledIconButton(onClick=focus,modifier=Modifier.size(62.dp),colors=IconButtonDefaults.filledIconButtonColors(containerColor=Primary,contentColor=Background)){Icon(Icons.Outlined.PlayArrow,"Odak oturumunu başlat",Modifier.size(32.dp))};Text("Başlat",style=MaterialTheme.typography.labelLarge,modifier=Modifier.padding(top=8.dp))}
 }
}

@Composable fun RotamLogo(size:Dp=40.dp) { Icon(painterResource(R.drawable.rotam_mark_vector),"Rotam",Modifier.size(size),tint=Color.Unspecified) }
