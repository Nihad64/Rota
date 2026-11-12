package com.nihad64.rotam.ui.routines

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nihad64.rotam.AppContainer
import com.nihad64.rotam.data.model.*
import com.nihad64.rotam.domain.usecase.*
import com.nihad64.rotam.ui.components.*
import com.nihad64.rotam.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.LocalDate

@Composable fun RoutinesScreen(c:AppContainer,onNew:()->Unit,onEdit:(Long)->Unit,onGoals:()->Unit) {
 val goals by remember(c){c.goalRepo.observeAll()}.collectAsStateWithLifecycle(emptyList())
 val series by remember(c){c.db.seriesDao().observeAll()}.collectAsStateWithLifecycle(emptyList())
 val plans by remember(c){c.planRepo.observeAll()}.collectAsStateWithLifecycle(emptyList())
 val today=LocalDate.now();val monday=today.minusDays((today.dayOfWeek.value-1).toLong());val sunday=monday.plusDays(6)
 val scope=rememberCoroutineScope();var busy by remember{mutableStateOf<Long?>(null)};var error by remember{mutableStateOf<String?>(null)};var pausing by remember{mutableStateOf<PlanSeries?>(null)};var archive by remember{mutableStateOf(false)}
 fun pause(s:PlanSeries,value:Boolean){if(busy!=null)return;busy=s.id;error=null
  scope.launch{try{
   val active=c.focus.active.value?.let{c.planRepo.byId(it)}
   if(value&&active?.seriesId==s.id)c.focus.pauseIf(active.id)
   c.planRepo.setRoutinePaused(s.id,value);c.scheduler.rescheduleAll()
  }catch(e:kotlinx.coroutines.CancellationException){throw e}catch(e:Exception){error=e.message?:"Rutin güncellenemedi. Kayıtların korundu."}finally{busy=null}}
 }
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(24.dp,20.dp,24.dp,48.dp),verticalArrangement=Arrangement.spacedBy(22.dp)) {
  item{PageHeader("Rutinler","Günlerine yerleşen küçük düzenler.")}
  item{PrimaryButton("Yeni rutin oluştur",onNew,Modifier.fillMaxWidth(),icon=Icons.Outlined.Add)}
  if(series.isEmpty())item{EmptyState("Tekrar eden şeylere yer aç.","Yaz veya söyle: ‘Haftada üç gün spor yapacağım.’ Günü gelen rutinler Bugün ve Takvim’de görünür.",null,{})}
  error?.let{message->item{Text(message,color=Danger)}}
  items(series.filter{archive||it.untilDate==null||it.untilDate>=today.toString()},key={it.uid}){s->
   val template=remember(s.template){runCatching{PlanJson.decode(JSONObject(s.template))}.getOrNull()}
   val paused=template?.let{RoutineState.paused(it)}?:false
   val instances=plans.filter{it.seriesId==s.id}
   val next=instances.filter{it.date>=today.toString()&&!it.isCompleted&&!it.isSkipped}.minWithOrNull(compareBy<Plan>{it.date}.thenBy{it.startTime?:"99:99"})
   val last=instances.maxByOrNull{it.date};val target=next?:last
   var menu by remember(s.uid){mutableStateOf(false)}
   RotamCard(Modifier.fillMaxWidth()) {
    Row(verticalAlignment=Alignment.CenterVertically){
     template?.let{CategoryIcon(it.categoryEnum,48.dp);Spacer(Modifier.width(12.dp))}
     Column(Modifier.weight(1f)){
      Text(template?.title?:"Rutin bilgisi okunamadı",style=MaterialTheme.typography.titleLarge)
      if(template!=null)Text(routineSchedule(template),color=SubtleText,style=MaterialTheme.typography.bodyMedium)
     }
     Box{IconButton(onClick={menu=true},enabled=busy==null){Icon(Icons.Outlined.MoreHoriz,"Rutin seçenekleri")}
      DropdownMenu(expanded=menu,onDismissRequest={menu=false}){
       if(target!=null)DropdownMenuItem(text={Text("Plan ayrıntısını aç")},onClick={menu=false;onEdit(target.id)})
       if(template!=null)DropdownMenuItem(text={Text(if(paused)"Rutine devam et"else"Rutine ara ver")},onClick={menu=false;if(paused)pause(s,false)else pausing=s})
      }
     }
    }
    if(s.untilDate!=null&&s.untilDate<today.toString())Text("Dönem bitti · ${s.untilDate}",color=SubtleText,style=MaterialTheme.typography.bodySmall)
    Spacer(Modifier.height(20.dp))
    if(paused){
     Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.PauseCircleOutline,null,tint=Accent);Spacer(Modifier.width(8.dp));Text("Ara verildi",color=Accent)}
     Text("Geçmişin korundu. Hazır olduğunda devam et.",color=SubtleText,style=MaterialTheme.typography.bodyMedium,modifier=Modifier.padding(top=8.dp))
     TextButton(onClick={pause(s,false)},enabled=busy==null){Text("Devam et")}
    }else {
     val week=instances.filter{it.date>=monday.toString()&&it.date<=sunday.toString()}
     if(template?.categoryEnum in setOf(Category.SPOR,Category.KITAP,Category.KISISEL)&&week.isNotEmpty()){
      val completed=week.count{it.isCompleted}
      Text("Bu hafta $completed / ${week.size} tamamlandı",color=SubtleText)
      Spacer(Modifier.height(10.dp))
      SegmentedProgress(completed,week.size)
      Spacer(Modifier.height(16.dp))
     }
     Text(if(next!=null)"Sıradaki · ${next.date} · ${PlanTime.label(next)}"else"Yaklaşan plan yok",color=SubtleText,style=MaterialTheme.typography.bodyMedium)
     if(target!=null)TextButton(onClick={onEdit(target.id)},enabled=busy==null){Text(if(next!=null)"Planı aç"else"Son kaydı aç")}
    }
    if(busy==s.id)LinearProgressIndicator(Modifier.fillMaxWidth())
   }
  }
  if(series.isNotEmpty())item{Text("Günü gelen rutinler, Bugün ekranında da görünür.",color=SubtleText,style=MaterialTheme.typography.bodyMedium)}
  if(series.any{it.untilDate!=null&&it.untilDate<today.toString()})item{TextButton(onClick={archive=!archive}){Text(if(archive)"Geçmiş rutinleri gizle"else"Geçmiş rutinleri göster")}}
  if(goals.isNotEmpty())item{TextButton(onClick=onGoals){Text("Önceki hedeflerim")}}
 }
 pausing?.let{s->AlertDialog(onDismissRequest={pausing=null},title={Text("Bu rutine ara verilsin mi?")},text={Text("Bugünden itibaren bekleyen örnekler ve hatırlatmaları durur. Tamamlanan, kısmen yapılan ve geçmiş kayıtların korunur. Yeniden devam ettiğinde aradaki günler başarısız sayılmaz.")},confirmButton={TextButton(onClick={pausing=null;pause(s,true)}){Text("Ara ver")}},dismissButton={TextButton(onClick={pausing=null}){Text("Vazgeç")}})}
}
private fun routineSchedule(p:Plan):String {
 val days=listOf("Pzt","Sal","Çar","Per","Cum","Cmt","Paz")
 val repeat=when(p.repeatType){
  "custom"->p.repeatDays.split(',').mapNotNull{it.toIntOrNull()?.takeIf{d->d in 1..7}?.let{d->days[d-1]}}.joinToString(" · ")
  "weekly"->(if(p.repeatEvery>1)"${p.repeatEvery} haftada bir · "else"")+days[LocalDate.parse(p.date).dayOfWeek.value-1]
  "interval"->"${p.repeatEvery} günde bir"
  else->RepeatType.fromKey(p.repeatType).label
 }
 return repeat+" · "+(p.startTime?:p.endTime?:"Gün içinde")
}
