package com.nihad64.rotam.ui.plan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nihad64.rotam.AppContainer
import com.nihad64.rotam.data.model.*
import com.nihad64.rotam.domain.usecase.*
import com.nihad64.rotam.ui.TrFormat
import com.nihad64.rotam.ui.components.*
import com.nihad64.rotam.ui.theme.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import java.time.*

class PlanDetailViewModel(private val c:AppContainer,val id:Long):ViewModel() {
 val plan=c.db.planDao().observeSyncRecords().map{all->all.firstOrNull{it.id==id&&!it.isDeleted}}.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),null)
 val busy=MutableStateFlow(false);val error=MutableStateFlow<String?>(null);val saved=MutableStateFlow(false)
 fun complete()=perform{old->c.completePlan(old);c.offerUndo(if(old.isCompleted)"Plan yeniden açıldı"else"Plan tamamlandı",old)}
 fun started(value:Boolean)=perform{old->c.planRepo.setStarted(old.id,old.uid,value);c.scheduler.rescheduleAll()}
 fun delete()=perform{old->c.deletePlan(old);c.offerUndo("Plan silindi",old);saved.value=true}
 fun subtask(task:String,done:Boolean)=perform{old->
  val json=JSONObject(old.extraData);val checked=json.optString("checkedSubtasks").lines().filter{it.isNotBlank()}.toSet()
  json.put("checkedSubtasks",(if(done)checked+task else checked-task).joinToString("\n"));c.planRepo.update(old.copy(extraData=json.toString()))
 }
 fun postpone(date:LocalDate,time:String?,scope:EditScope)=perform{old->
  if(scope!=EditScope.THIS&&old.repeatUntil!=null)check(date<=LocalDate.parse(old.repeatUntil)){"Yeni ilk gün tekrar bitişinden sonra. Planı düzenle bölümünde bitiş gününü de güncelle."}
  val proposed=ReschedulePlan.propose(old,date,time)
  val clashes=c.planRepo.range(LocalDate.parse(proposed.date).minusDays(7),LocalDate.parse(proposed.endDate?:proposed.date)).filter{PlanTime.overlaps(proposed,it)}
  check(clashes.isEmpty()){"Bu aralıkta ${clashes.size} planın daha var. Çakışmayı incelemek için Planı düzenle bölümünü kullan."}
  c.focus.pauseIf(old.id)
  val current=c.planRepo.byId(old.id)?:error("Plan bulunamadı.")
  val moved=proposed.copy(focusSeconds=current.focusSeconds)
  if(scope!=EditScope.THIS)com.nihad64.rotam.data.backup.BackupStore(c.app,c.db).checkpoint()
  c.planRepo.edit(current,moved,scope);c.scheduler.rescheduleAll()
  if(scope==EditScope.THIS)c.offerUndo("Plan ertelendi",current)
  else c.notices.send(com.nihad64.rotam.Notice("Sonraki tekrarlar güncellendi. Önceki kayıtların geri dönüş yedeğinde."))
  saved.value=true
 }
 private fun perform(action:suspend (Plan)->Unit){
  if(busy.value)return
  val shown=plan.value?:return
  busy.value=true;error.value=null
  viewModelScope.launch{try{
   val current=c.planRepo.byId(id)?:error("Plan bulunamadı.")
   check(current==shown){"Plan başka bir işlemde değişti. Güncel bilgileri kontrol edip yeniden dene."}
   action(current)
  }catch(e:CancellationException){throw e}catch(e:Exception){error.value=e.message?:"İşlem tamamlanamadı. Kayıtların korunuyor."}finally{busy.value=false}}
 }
}

@Composable fun PlanDetailScreen(vm:PlanDetailViewModel,onBack:()->Unit,onEdit:()->Unit,onFocus:()->Unit,onPostpone:()->Unit,onAi:()->Unit) {
 val p by vm.plan.collectAsStateWithLifecycle();val busy by vm.busy.collectAsStateWithLifecycle();val error by vm.error.collectAsStateWithLifecycle();val removed by vm.saved.collectAsStateWithLifecycle()
 var delete by remember{mutableStateOf(false)};var share by remember{mutableStateOf(false)}
 val feedback=rememberCompletionFeedback()
 var priorDone by remember{mutableStateOf<Boolean?>(null)}
 LaunchedEffect(p?.isCompleted){if(priorDone==false&&p?.isCompleted==true)feedback();priorDone=p?.isCompleted}
 LaunchedEffect(removed){if(removed)onBack()}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=24.dp,vertical=18.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
  PageHeader("Plan detayı",onBack=onBack,action={TextButton(onClick=onEdit,enabled=p!=null&&!busy){Text("Düzenle")}})
  val plan=p
  if(plan==null){Text("Plan yükleniyor veya artık mevcut değil.",color=SubtleText);return@Column}
  Row(verticalAlignment=Alignment.CenterVertically){CategoryIcon(plan.categoryEnum,48.dp);Spacer(Modifier.width(12.dp));Text(plan.categoryEnum.label,color=SubtleText)}
  Text(plan.title,style=MaterialTheme.typography.displaySmall)
  Text(TrFormat.full(LocalDate.parse(plan.date))+" · "+PlanTime.label(plan),style=MaterialTheme.typography.bodyLarge)
  if(plan.seriesId!=0L)Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.Repeat,null,tint=Accent);Spacer(Modifier.width(10.dp));Text(RepeatType.fromKey(plan.repeatType).label,color=SubtleText)}
  HorizontalDivider(color=Outline)
  DetailRow("Gün",TrFormat.full(LocalDate.parse(plan.date)),onEdit)
  DetailRow("Saat",PlanTime.label(plan),onEdit)
  val json=remember(plan.extraData){JSONObject(plan.extraData)}
  val tasks=json.optString("subtasks").lines().filter{it.isNotBlank()};val checked=json.optString("checkedSubtasks").lines().toSet()
  if(tasks.isNotEmpty()) {
   Text("Yapılacaklar",style=MaterialTheme.typography.titleLarge)
   tasks.forEach{task->Row(verticalAlignment=Alignment.CenterVertically){Checkbox(task in checked,{vm.subtask(task,it)},enabled=!busy);Text(task,modifier=Modifier.weight(1f))}}
  }
  json.optString("note").takeIf{it.isNotBlank()}?.let{Text(it,style=MaterialTheme.typography.bodyLarge,color=SubtleText)}
  DetailRow("Hatırlatmalar",ReminderPolicy.label(plan),onEdit)
  if(plan.focusSeconds>0)Text("Kaydedilen odak · ${plan.focusSeconds/60} dk ${plan.focusSeconds%60} sn",color=SubtleText)
  if(!plan.isCompleted&&!plan.isSkipped)TextButton(onClick={vm.started(!ReminderPolicy.started(plan))},enabled=!busy){Text(if(ReminderPolicy.started(plan))"Başlandı · geri al"else"Başladım · hatırlatmaları durdur")}
  if(!plan.isCompleted&&!plan.isSkipped)OutlinedButton(onClick=onFocus,modifier=Modifier.fillMaxWidth().heightIn(min=54.dp)){Icon(Icons.Outlined.PlayArrow,null);Spacer(Modifier.width(8.dp));Text("Odaklan")}
  OutlinedButton(onClick={share=true},modifier=Modifier.fillMaxWidth().heightIn(min=54.dp)){Icon(Icons.Outlined.AutoAwesome,null,tint=Accent);Spacer(Modifier.width(8.dp));Text("AI ile değişiklik öner")}
  PrimaryButton(if(plan.isCompleted)"Tamamlamayı geri al"else"Tamamladım",vm::complete,Modifier.fillMaxWidth(),icon=Icons.Outlined.Check,enabled=!busy)
  if(!plan.isCompleted)TextButton(onClick=onPostpone,enabled=!busy,modifier=Modifier.align(Alignment.CenterHorizontally)){Text("Ertele")}
  TextButton(onClick={delete=true},enabled=!busy,modifier=Modifier.align(Alignment.CenterHorizontally)){Text("Planı sil",color=Danger)}
  error?.let{Text(it,color=Danger)}
  Spacer(Modifier.height(24.dp))
 }
 if(delete)AlertDialog(onDismissRequest={delete=false},title={Text("Bu plan silinsin mi?")},text={Text("Tekrarlıysa yalnız bu örnek silinir. Ardından geri alabilirsin.")},confirmButton={TextButton(onClick={delete=false;vm.delete()}){Text("Sil",color=Danger)}},dismissButton={TextButton(onClick={delete=false}){Text("Vazgeç")}})
 if(share)AlertDialog(onDismissRequest={share=false},title={Text("Bu planı AI ile düzenle")},text={Text("Bu planın başlığı, kategorisi, tarihi ve saatleri öneri üretmek için Google Gemini'ye gönderilecek. Değişiklik yalnız önizlemeyi onayladığında kaydedilir.")},confirmButton={TextButton(onClick={share=false;onAi()}){Text("Paylaş ve devam et")}},dismissButton={TextButton(onClick={share=false}){Text("Vazgeç")}})
}

@Composable fun PostponeScreen(vm:PlanDetailViewModel,onBack:()->Unit) {
 val plan by vm.plan.collectAsStateWithLifecycle();val busy by vm.busy.collectAsStateWithLifecycle();val error by vm.error.collectAsStateWithLifecycle();val saved by vm.saved.collectAsStateWithLifecycle()
 var date by rememberSaveable{mutableStateOf(LocalDate.now().plusDays(1).toString())};var time by rememberSaveable{mutableStateOf<String?>(null)};var initialized by rememberSaveable{mutableStateOf(false)}
 var datePicker by remember{mutableStateOf(false)};var timePicker by remember{mutableStateOf(false)};var future by rememberSaveable{mutableStateOf(false)}
 LaunchedEffect(plan?.uid){if(!initialized&&plan!=null){time=plan?.startTime?:plan?.endTime;initialized=true}}
 LaunchedEffect(saved){if(saved)onBack()}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(22.dp)) {
  PageHeader("Ertele",onBack=onBack)
  val p=plan
  if(p==null){CircularProgressIndicator();return@Column}
  Text("${p.title} ne zamana taşınsın?",style=MaterialTheme.typography.headlineMedium)
  Text("Şu an · ${p.date} · ${PlanTime.label(p)}",color=SubtleText)
  TextButton(onClick={date=LocalDate.now().plusDays(1).toString();time=p.startTime?:p.endTime}){Text("Yarın aynı saatte")}
  DetailRow("Yeni gün",TrFormat.full(LocalDate.parse(date))){datePicker=true}
  DetailRow(if(p.startTime==null&&p.endTime!=null)"Yeni son teslim saati"else"Yeni saat",time?:"Saat yok"){timePicker=true}
  Text(if(PlanTime.durationMinutes(p)!=null)"Planın süresi korunur."else"Belirtmediğin bir bitiş saati eklenmez.",color=SubtleText)
  if(p.seriesId!=0L){
   Text("Değişiklik hangi günleri kapsasın?",style=MaterialTheme.typography.titleLarge)
   Row(verticalAlignment=Alignment.CenterVertically){RadioButton(!future,{future=false});Text("Yalnız bu gün")}
   Row(verticalAlignment=Alignment.CenterVertically){RadioButton(future,{future=true});Text("Bu ve sonraki tekrarlar",modifier=Modifier.weight(1f))}
   if(future)Text("Tamamlanan geçmiş örnekler korunur. Önceki kayıtların geri dönüş yedeği alınır.",color=SubtleText,style=MaterialTheme.typography.bodySmall)
  }
  error?.let{Text(it,color=Danger)}
  PrimaryButton(if(busy)"Kaydediliyor…"else"Ertelemeyi onayla",{vm.postpone(LocalDate.parse(date),time,if(future)EditScope.FUTURE else EditScope.THIS)},Modifier.fillMaxWidth(),enabled=!busy)
 }
 if(datePicker)DateChooser(date,{datePicker=false}){date=it}
 if(timePicker)TimeChooser(time,"Plan saati",{timePicker=false}){time=it}
}
