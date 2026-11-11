package com.nihad64.rotam.ui.home
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nihad64.rotam.data.model.Plan
import com.nihad64.rotam.domain.usecase.PlanOutcome
import com.nihad64.rotam.domain.usecase.PlanTime
import com.nihad64.rotam.ui.components.*
import com.nihad64.rotam.ui.newplan.RotamField
import com.nihad64.rotam.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable fun PendingPlanDeck(plans:List<Plan>,vm:HomeViewModel,onPostpone:((Long)->Unit)?=null,topPadding:Dp=0.dp){
 val busy by vm.reporting.collectAsStateWithLifecycle();val error by vm.outcomeError.collectAsStateWithLifecycle()
 val motion=rememberMotionEnabled();var direction by remember{mutableIntStateOf(1)}
 val transition=updateTransition(plans.firstOrNull(),label="pending-plan")
 if(transition.currentState==null&&transition.targetState==null)return
 Column(Modifier.padding(top=topPadding),verticalArrangement=Arrangement.spacedBy(12.dp)){
  Text("Yanıtını bekleyen ${plans.size.coerceAtLeast(1)} plan",style=MaterialTheme.typography.titleMedium)
  Box(Modifier.fillMaxWidth().padding(top=10.dp,bottom=10.dp)){
   for(i in minOf(2,plans.size-1) downTo 1)Surface(Modifier.matchParentSize().graphicsLayer{scaleX=1f-i*.035f;scaleY=1f-i*.025f;rotationZ=if(motion)(if(i%2==0)-2f else 2f)else 0f;translationY=i*8.dp.toPx()},shape=RoundedCornerShape(28.dp),color=SurfaceVariant){}
   transition.AnimatedContent(
    contentKey={it?.uid},
    transitionSpec={
     (fadeIn(tween(if(motion)180 else 0,if(motion)70 else 0))+scaleIn(tween(if(motion)220 else 0),initialScale=if(motion).96f else 1f)) togetherWith
      (fadeOut(tween(if(motion)160 else 0))+slideOutHorizontally(tween(if(motion)240 else 0)){if(motion)it*direction else 0}) using SizeTransform(clip=false)
    }
   ){p->if(p!=null)PendingCard(p,busy,error,motion,onReport={status,amount,target->direction=if(status==PlanOutcome.Status.NOT_DONE)-1 else 1;vm.report(p,status,amount,target)},onPostpone=onPostpone,onMoveDate={vm.postpone(p,it)})}
  }
  error?.let{Text(it,color=Danger)}
 }
}
@Composable private fun PendingCard(p:Plan,busy:Boolean,error:String?,motion:Boolean,onReport:(PlanOutcome.Status,Double?,Double?)->Unit,onPostpone:((Long)->Unit)?,onMoveDate:(LocalDate)->Unit){
 var partial by remember(p.uid){mutableStateOf(false)};var postponing by remember(p.uid){mutableStateOf(false)}
 var amount by remember(p.uid){mutableStateOf("")};var target by remember(p.uid){mutableStateOf("")}
 var drag by remember(p.uid){mutableFloatStateOf(0f)};var settling by remember{mutableStateOf<Job?>(null)}
 val scope=rememberCoroutineScope();val threshold=with(LocalDensity.current){100.dp.toPx()}
 fun settle(){settling?.cancel();settling=scope.launch{if(motion)Animatable(drag).animateTo(0f,tween(180)){drag=value}else drag=0f}}
 LaunchedEffect(error,busy){if(!busy&&error!=null)settle()}
 RotamCard(Modifier.fillMaxWidth().graphicsLayer{translationX=drag;rotationZ=if(motion)(drag/threshold)*5f else 0f}
  .pointerInput(p.uid,busy,motion){if(!busy)detectHorizontalDragGestures(
   onDragStart={settling?.cancel()},onHorizontalDrag={change,delta->change.consume();drag+=delta},onDragCancel={settle()},
   onDragEnd={when{drag>=threshold->onReport(PlanOutcome.Status.COMPLETED,null,null);drag<=-threshold->onReport(PlanOutcome.Status.NOT_DONE,null,null);else->settle()}}
  )}){
  Diamond();Spacer(Modifier.height(16.dp))
  Text(p.title,style=MaterialTheme.typography.titleLarge);Text("${p.date} · ${PlanTime.label(p)}",color=SubtleText)
  Text("Yapabildin mi?",style=MaterialTheme.typography.headlineSmall,modifier=Modifier.padding(vertical=12.dp))
  Text("← Yapmadım                 Yaptım →",color=SubtleText,style=MaterialTheme.typography.bodySmall)
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){TextButton(onClick={onReport(PlanOutcome.Status.NOT_DONE,null,null)},enabled=!busy){Text("Yapmadım")};TextButton(onClick={onReport(PlanOutcome.Status.COMPLETED,null,null)},enabled=!busy){Text("Yaptım")}}
  Row{TextButton(onClick={partial=true},enabled=!busy){Text("Kısmen yaptım")};TextButton(onClick={if(onPostpone!=null)onPostpone(p.id)else postponing=true},enabled=!busy){Text("Ertele")}}
  if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
 }
 if(partial)AlertDialog(onDismissRequest={partial=false},title={Text("Ne kadarını yaptın?")},text={Column{Text("Aynı birimle gir: örneğin 10 / 20 sayfa.");RotamField(amount,{amount=it},"Yaptığın miktar",number=true);RotamField(target,{target=it},"Toplam hedef",number=true)}},confirmButton={
  val a=amount.replace(',','.').toDoubleOrNull();val t=target.replace(',','.').toDoubleOrNull()
  TextButton(onClick={onReport(PlanOutcome.Status.PARTIAL,a,t);partial=false},enabled=!busy&&a!=null&&t!=null&&a.isFinite()&&t.isFinite()&&a>0&&t>a){Text("Kaydet")}
 },dismissButton={TextButton(onClick={partial=false}){Text("Vazgeç")}})
 if(postponing)DateChooser(LocalDate.now().plusDays(1).toString(),{postponing=false}){date->onMoveDate(LocalDate.parse(date))}
}
