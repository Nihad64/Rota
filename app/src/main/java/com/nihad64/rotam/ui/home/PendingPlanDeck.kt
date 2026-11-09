package com.nihad64.rotam.ui.home
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nihad64.rotam.data.model.Plan
import com.nihad64.rotam.domain.usecase.PlanOutcome
import com.nihad64.rotam.domain.usecase.PlanTime
import com.nihad64.rotam.ui.components.*
import com.nihad64.rotam.ui.newplan.RotamField
import com.nihad64.rotam.ui.theme.*
import java.time.LocalDate

@Composable fun PendingPlanDeck(plans:List<Plan>,vm:HomeViewModel){
 val p=plans.firstOrNull()?:return
 val busy by vm.reporting.collectAsStateWithLifecycle();val error by vm.outcomeError.collectAsStateWithLifecycle()
 var partial by remember(p.uid){mutableStateOf(false)};var postponing by remember(p.uid){mutableStateOf(false)}
 var amount by remember(p.uid){mutableStateOf("")};var target by remember(p.uid){mutableStateOf("")}
 var drag by remember(p.uid){mutableFloatStateOf(0f)}
 val threshold=with(LocalDensity.current){100.dp.toPx()}
 Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
  Text("Yanıtını bekleyen ${plans.size} plan",style=MaterialTheme.typography.titleMedium)
  Box(Modifier.fillMaxWidth().padding(top=8.dp,bottom=8.dp)){
   for(i in minOf(2,plans.size-1) downTo 1)Surface(Modifier.fillMaxWidth().height(190.dp).graphicsLayer{rotationZ=if(i%2==0)-3f else 3f;translationY=i*6.dp.toPx()},shape=RoundedCornerShape(28.dp),color=SurfaceVariant){}
   RotamCard(Modifier.fillMaxWidth().graphicsLayer{translationX=drag;rotationZ=(drag/threshold)*5f}
    .pointerInput(p.uid,busy){if(!busy)detectHorizontalDragGestures(onHorizontalDrag={change,delta->change.consume();drag+=delta},onDragCancel={drag=0f},onDragEnd={val direction=drag;drag=0f;if(direction>=threshold)vm.report(p,PlanOutcome.Status.COMPLETED)else if(direction<=-threshold)vm.report(p,PlanOutcome.Status.NOT_DONE)})}){
    Text(p.title,style=MaterialTheme.typography.titleLarge);Text("${p.date} · ${PlanTime.label(p)}",color=SubtleText)
    Text("Yapabildin mi?",style=MaterialTheme.typography.headlineSmall,modifier=Modifier.padding(vertical=12.dp))
    Text("← Yapmadım                 Yaptım →",color=SubtleText,style=MaterialTheme.typography.bodySmall)
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){TextButton(onClick={vm.report(p,PlanOutcome.Status.NOT_DONE)},enabled=!busy){Text("Yapmadım")};TextButton(onClick={vm.report(p,PlanOutcome.Status.COMPLETED)},enabled=!busy){Text("Yaptım")}}
    Row{TextButton(onClick={partial=true},enabled=!busy){Text("Kısmen yaptım")};TextButton(onClick={postponing=true},enabled=!busy){Text("Ertele")}}
   }
  }
  error?.let{Text(it,color=Danger)}
 }
 if(partial)AlertDialog(onDismissRequest={partial=false},title={Text("Ne kadarını yaptın?")},text={Column{Text("Aynı birimle gir: örneğin 10 / 20 sayfa.");RotamField(amount,{amount=it},"Yaptığın miktar",number=true);RotamField(target,{target=it},"Toplam hedef",number=true)}},confirmButton={
  val a=amount.replace(',','.').toDoubleOrNull();val t=target.replace(',','.').toDoubleOrNull()
  TextButton(onClick={vm.report(p,PlanOutcome.Status.PARTIAL,a,t);partial=false},enabled=!busy&&a!=null&&t!=null&&a.isFinite()&&t.isFinite()&&a>0&&t>a){Text("Kaydet")}
 },dismissButton={TextButton(onClick={partial=false}){Text("Vazgeç")}})
 if(postponing)DateChooser(LocalDate.now().plusDays(1).toString(),{postponing=false}){date->vm.postpone(p,LocalDate.parse(date))}
}
