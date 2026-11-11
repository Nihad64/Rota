package com.nihad64.rotam.ui.focus

import androidx.compose.foundation.layout.*
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.saveable.rememberSaveable
import com.nihad64.rotam.domain.usecase.FocusSessionSummary
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nihad64.rotam.AppContainer
import com.nihad64.rotam.data.model.Plan
import com.nihad64.rotam.domain.usecase.PlanTime
import com.nihad64.rotam.ui.components.*
import com.nihad64.rotam.ui.theme.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class FocusState(val plan:Plan?=null,val seconds:Long=0,val breakSeconds:Long=0,val running:Boolean=false,val loading:Boolean=true,val busy:Boolean=false,val error:String?=null,val summary:FocusSessionSummary?=null,val finished:Boolean=false)
class FocusViewModel(private val c:AppContainer,private val id:Long):ViewModel() {
 private val mutable=MutableStateFlow(FocusState());val state=mutable.asStateFlow()
 init {viewModelScope.launch {while(isActive){refresh();delay(1000)}}}
 private suspend fun refresh(){
  val p=c.planRepo.byId(id)?.takeUnless{it.isDeleted}
  mutable.update{it.copy(plan=p,seconds=c.focus.sessionSeconds(id),breakSeconds=c.focus.breakSeconds(id),running=c.focus.active.value==id,loading=false,summary=c.focus.summary(id))}
 }
 fun toggle()=perform{if(c.focus.active.value==id)c.focus.pause()else c.focus.start(id);refresh()}
 fun finish()=perform{c.focus.finish(id);refresh()}
 fun later()=perform{c.focus.dismissSummary(id);mutable.update{it.copy(finished=true)}}
 fun complete()=perform{
  if(c.focus.active.value==id)c.focus.pause()
  val p=c.planRepo.byId(id)?:return@perform
  c.completePlan(p,true);c.focus.dismissSummary(id)
  mutable.update{it.copy(plan=c.planRepo.byId(id),running=false,finished=true)}
  c.offerUndo("Plan tamamlandı",p)
 }
 private fun perform(action:suspend ()->Unit){
  if(state.value.busy)return
  mutable.update{it.copy(busy=true,error=null)}
  viewModelScope.launch{try{action()}catch(e:CancellationException){throw e}catch(_:Exception){mutable.update{it.copy(error="İşlem tamamlanamadı. Süren korunuyor; yeniden dene.")}}finally{mutable.update{it.copy(busy=false)}}}
 }
}

@Composable fun FocusScreen(vm:FocusViewModel,onBack:()->Unit) {
 val s by vm.state.collectAsStateWithLifecycle()
 var countdown by rememberSaveable{mutableStateOf(false)}
 val motion=rememberMotionEnabled()
 val feedback=rememberCompletionFeedback()
 LaunchedEffect(s.finished){if(s.finished){if(s.plan?.isCompleted==true)feedback();onBack()}}
 BackHandler(s.summary!=null){vm.later()}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=24.dp,vertical=18.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(24.dp)) {
  PageHeader(if(s.summary!=null)"Oturum özeti"else"Odaklan",onBack=if(s.summary!=null)({vm.later()})else onBack)
  if(s.loading){CircularProgressIndicator();return@Column}
  val p=s.plan
  if(p==null){EmptyState("Bu plan bulunamadı.","Plan silinmiş olabilir. Kayıtlı diğer planların korunuyor.","Geri dön",onBack);return@Column}
  AnimatedContent(targetState=s.summary!=null,transitionSpec={fadeIn(tween(if(motion)RotamMotion.Replace else 0)) togetherWith fadeOut(tween(if(motion)RotamMotion.Feedback else 0))},label="focus-session-summary") { summaryVisible->
   Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(24.dp)) {
    if(summaryVisible){
     val summary=s.summary
     if(summary!=null){
      SessionCard {
       Diamond(12.dp)
       Spacer(Modifier.height(20.dp))
       Text(focusDuration(summary.focusSeconds)+" odaklandın.",style=MaterialTheme.typography.headlineMedium)
       Spacer(Modifier.height(12.dp));Text(p.title,color=SubtleText,style=MaterialTheme.typography.bodyLarge)
       Spacer(Modifier.height(20.dp));Icon(Icons.Outlined.CheckCircle,null,tint=Accent,modifier=Modifier.size(42.dp))
      }
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly) {
       SessionMetric("Odak süresi",focusDuration(summary.focusSeconds))
       SessionMetric("Mola",focusDuration(summary.breakSeconds))
      }
      HorizontalDivider(color=Outline)
      Text(if(p.isCompleted)"Planın tamamlandı."else"Planını da tamamladın mı?",style=MaterialTheme.typography.titleLarge,textAlign=TextAlign.Center)
      Text("Ölçülen odak süren kaydedildi.",color=SubtleText,textAlign=TextAlign.Center)
      if(!p.isCompleted&&!p.isSkipped)PrimaryButton("Evet, planı tamamla",vm::complete,Modifier.fillMaxWidth(),enabled=!s.busy)
      OutlinedButton(onClick=vm::later,modifier=Modifier.fillMaxWidth().heightIn(min=54.dp),enabled=!s.busy){Text(if(p.isCompleted)"Günüme dön"else"Sonra devam edeceğim")}
     }
    }else {
     CategoryIcon(p.categoryEnumCompat(),64.dp)
     Text(p.title,style=MaterialTheme.typography.headlineMedium,textAlign=TextAlign.Center)
     val duration=PlanTime.durationMinutes(p)?.times(60)
     Text(if(duration==null)"Serbest odak"else PlanTime.label(p),color=SubtleText,style=MaterialTheme.typography.bodyLarge)
     if(duration!=null)SegmentedToggle(listOf("Geçen süre","Geri sayım"),if(countdown)1 else 0,{countdown=it==1})
     val shown=if(countdown&&duration!=null)(duration-s.seconds).coerceAtLeast(0)else s.seconds
     SessionCard {
      Row(verticalAlignment=Alignment.CenterVertically){Diamond();Spacer(Modifier.width(10.dp));Text(if(s.running)"ŞİMDİ"else if(s.seconds>0)"MOLA"else"HAZIR OLDUĞUNDA",style=MaterialTheme.typography.labelMedium,color=SubtleText)}
      Spacer(Modifier.height(24.dp))
      Text(focusClock(shown),fontSize=54.sp,lineHeight=60.sp,letterSpacing=(-2).sp,fontWeight=FontWeight.SemiBold,maxLines=1)
      Spacer(Modifier.height(8.dp));Text(if(countdown)"Kalan süre"else"Geçen odak süresi",color=SubtleText)
      Spacer(Modifier.height(24.dp))
      if(!p.isCompleted&&!p.isSkipped)PrimaryButton(if(s.running)"Mola ver"else if(s.seconds>0)"Devam et"else"Başla",vm::toggle,Modifier.fillMaxWidth(),icon=if(s.running)Icons.Outlined.Pause else Icons.Outlined.PlayArrow,enabled=!s.busy)
     }
     Text(when{p.isCompleted->"Bu plan tamamlandı.";countdown&&duration!=null&&s.seconds>=duration->"Süre doldu. Hazırsan oturumunu bitirebilirsin.";s.running->"Kendi ritminde devam et.";s.seconds>0->"Molanın tadını çıkar. Hazır olunca devam et.";else->"Küçük bir başlangıç yeter."},color=SubtleText,textAlign=TextAlign.Center)
     OutlinedButton(onClick=vm::finish,modifier=Modifier.fillMaxWidth().heightIn(min=54.dp),enabled=!s.busy){Text("Bitir ve özeti gör")}
     Text("Mola süresi odağa eklenmez. Oturumu bitirmek planını kendiliğinden tamamlamaz.",color=SubtleText,style=MaterialTheme.typography.bodySmall,textAlign=TextAlign.Center)
    }
   }
  }
  s.error?.let{Text(it,color=Danger)}
  Spacer(Modifier.height(16.dp))
 }
}
@Composable private fun SessionCard(content:@Composable ColumnScope.()->Unit) {
 Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart=32.dp,topEnd=32.dp,bottomStart=32.dp,bottomEnd=64.dp)).background(Brush.linearGradient(listOf(SurfaceVariant,Surface))).padding(28.dp),content=content)
}
@Composable private fun SessionMetric(label:String,value:String){Column(horizontalAlignment=Alignment.CenterHorizontally){Text(label,color=SubtleText);Spacer(Modifier.height(8.dp));Text(value,style=MaterialTheme.typography.headlineSmall)}}
private fun focusClock(seconds:Long)=if(seconds>=3600)"%d:%02d:%02d".format(seconds/3600,seconds/60%60,seconds%60)else"%02d:%02d".format(seconds/60,seconds%60)
private fun focusDuration(seconds:Long)=when{seconds>=3600->"${seconds/3600} sa ${seconds/60%60} dk";seconds>=60->"${seconds/60} dk";else->"$seconds sn"}
private fun Plan.categoryEnumCompat()=com.nihad64.rotam.data.model.Category.fromKey(category)
