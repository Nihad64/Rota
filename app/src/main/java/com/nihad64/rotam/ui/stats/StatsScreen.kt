package com.nihad64.rotam.ui.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nihad64.rotam.data.model.categoryEnum
import com.nihad64.rotam.ui.TrFormat
import com.nihad64.rotam.ui.components.*
import com.nihad64.rotam.ui.theme.*
import java.time.LocalDate

@Composable fun StatsScreen(vm:StatsViewModel,onBack:()->Unit,onPlan:(Long)->Unit={}) {
 val s by vm.state.collectAsStateWithLifecycle();val motion=rememberMotionEnabled();var picker by remember{mutableStateOf(false)}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=24.dp,vertical=18.dp),verticalArrangement=Arrangement.spacedBy(22.dp)) {
  PageHeader("İlerlemem",onBack=onBack,action={TextButton(onClick=vm::today){Text("Bugün")}})
  Text("İlerlemeni gör, ritmini koru.",color=SubtleText)
  SegmentedToggle(listOf("Hafta","Ay"),if(s.month)1 else 0,{vm.setPeriod(if(it==0)7 else 30)})
  Row(verticalAlignment=Alignment.CenterVertically){
   IconButton(onClick={vm.shift(-1)}){Icon(Icons.Outlined.ChevronLeft,"Önceki dönem")}
   Text("${TrFormat.dayMonth(s.from)} – ${TrFormat.dayMonth(s.to)} ${s.to.year}",modifier=Modifier.weight(1f),textAlign=TextAlign.Center,style=MaterialTheme.typography.titleSmall)
   IconButton(onClick={vm.shift(1)}){Icon(Icons.Outlined.ChevronRight,"Sonraki dönem")}
  }
  RotamCard(Modifier.fillMaxWidth()) {
   Row(verticalAlignment=Alignment.CenterVertically){Diamond();Spacer(Modifier.width(10.dp));Text("Planlarım",color=SubtleText)}
   Spacer(Modifier.height(12.dp));Text("${s.totalDone} / ${s.total}",style=MaterialTheme.typography.displaySmall)
   Text("plan tamamlandı",color=SubtleText)
   Spacer(Modifier.height(18.dp))
   LinearProgressIndicator(progress={if(s.total==0)0f else s.totalDone.toFloat()/s.total},modifier=Modifier.fillMaxWidth().height(6.dp),color=Accent,trackColor=Outline)
   Spacer(Modifier.height(14.dp))
   Text("${s.pending} sırada · ${s.awaiting} yanıt bekliyor",color=SubtleText,style=MaterialTheme.typography.bodyMedium)
   if(s.partial>0||s.notDone>0)Text("${s.partial} kısmen yapıldı · ${s.notDone} yapılmadı bildirildi",color=SubtleText,style=MaterialTheme.typography.bodySmall)
   if(s.total==0)Text("Bu dönemde kayıt yok. İlk adımınla başlar.",color=SubtleText,modifier=Modifier.padding(top=10.dp))
  }
  if(s.routines.isNotEmpty()) {
   Text("Rutinlerim",style=MaterialTheme.typography.titleLarge)
   s.routines.forEach{r->
    Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.Repeat,null,tint=Accent);Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(r.title,style=MaterialTheme.typography.titleMedium);if(r.paused)Text("Ara verildi",color=SubtleText,style=MaterialTheme.typography.bodySmall)};Text("${r.done} / ${r.total}")}
    LinearProgressIndicator(progress={r.done.toFloat()/r.total.coerceAtLeast(1)},modifier=Modifier.fillMaxWidth().height(5.dp),color=Accent,trackColor=Outline)
   }
  }
  HorizontalDivider(color=Outline)
  Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
   Text("Odak sürem",style=MaterialTheme.typography.titleLarge)
   Text(duration(s.focusSeconds),style=MaterialTheme.typography.displaySmall)
   FocusChart(s.week,motion,vm::select)
   Text("Yalnız ölçülen odak süresi gösterilir. Molalar eklenmez.",color=SubtleText,style=MaterialTheme.typography.bodySmall)
   if(s.legacyFocusSeconds>0)Text("Eski kayıtlardaki ${duration(s.legacyFocusSeconds)} için gün bilgisi yok. Bu süre günlük grafiğe dağıtılmadı.",color=SubtleText,style=MaterialTheme.typography.bodySmall)
  }
  HorizontalDivider(color=Outline)
  Row(verticalAlignment=Alignment.CenterVertically){Text(TrFormat.full(s.selected),style=MaterialTheme.typography.titleMedium,modifier=Modifier.weight(1f));OutlinedButton(onClick={picker=true}){Text("Günü seç")}}
  if(s.selectedPlans.isEmpty())Text("Bu gün için plan kaydı yok.",color=SubtleText)
  s.selectedPlans.forEach{p->
   Row(Modifier.fillMaxWidth().clickable{onPlan(p.id)}.padding(vertical=10.dp),verticalAlignment=Alignment.CenterVertically){
    CategoryIcon(p.categoryEnum,40.dp);Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(p.title,style=MaterialTheme.typography.titleMedium);Text(com.nihad64.rotam.domain.usecase.PlanTime.label(p),color=SubtleText,style=MaterialTheme.typography.bodyMedium)}
    Icon(if(p.isCompleted)Icons.Outlined.CheckCircle else Icons.Outlined.ChevronRight,null,tint=if(p.isCompleted)Accent else SubtleText)
   }
  }
  if(s.byCategory.isNotEmpty()) {
   HorizontalDivider(color=Outline);Text("Tamamlanan planlar",style=MaterialTheme.typography.titleLarge)
   s.byCategory.forEach{(category,count)->Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){CategoryIcon(category,32.dp);Spacer(Modifier.width(10.dp));Text(category.label,modifier=Modifier.weight(1f));Text("$count")}}
  }
  Text("Planlanan aralıklar: ${s.plannedMinutes/60} sa ${s.plannedMinutes%60} dk · ${s.unknownDuration} planın süresi belirtilmedi.",color=SubtleText,style=MaterialTheme.typography.bodySmall)
  Spacer(Modifier.height(32.dp))
 }
 if(picker)DateChooser(s.selected.toString(),{picker=false}){vm.select(LocalDate.parse(it))}
}
@Composable private fun FocusChart(days:List<DayStat>,motion:Boolean,onDay:(LocalDate)->Unit) {
 val values=days.map{it.focusSeconds};val max=values.maxOrNull()?.coerceAtLeast(1)?:1
 val target=if(values.any{it>0})1f else 0f
 val animated=remember{Animatable(0f)}
 LaunchedEffect(values,motion){animated.snapTo(if(motion)0f else target);if(motion)animated.animateTo(target,tween(RotamMotion.Navigate))}
 val animation=animated.value
 Canvas(Modifier.fillMaxWidth().height(150.dp).semantics{contentDescription="Günlük odak süresi grafiği. Ayrıntılar için Günü seç düğmesini kullan."}.pointerInput(days){detectTapGestures{offset->if(days.isNotEmpty())onDay(days[(offset.x/size.width*days.size).toInt().coerceIn(0,days.lastIndex)].date)}}) {
  if(days.isEmpty())return@Canvas
  val slot=size.width/days.size;val width=slot*.68f
  days.forEachIndexed{i,d->val x=i*slot+(slot-width)/2;val h=(size.height*d.focusSeconds.toFloat()/max*animation).coerceAtLeast(3.dp.toPx())
   drawRoundRect(if(d.focusSeconds>0)Accent else Outline,Offset(x,size.height-h),Size(width,h),CornerRadius(4.dp.toPx()))
  }
 }
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
  val labels=if(days.size<=7)days else listOfNotNull(days.firstOrNull(),days.getOrNull(days.size/2),days.lastOrNull())
  labels.forEach{d->Text(if(days.size<=7)TrFormat.short(d.date.dayOfWeek)else TrFormat.dayMonth(d.date),style=MaterialTheme.typography.labelSmall,color=SubtleText)}
 }
}
private fun duration(seconds:Long)=if(seconds>=3600)"${seconds/3600} sa ${seconds/60%60} dk"else if(seconds>=60)"${seconds/60} dk"else"$seconds sn"
