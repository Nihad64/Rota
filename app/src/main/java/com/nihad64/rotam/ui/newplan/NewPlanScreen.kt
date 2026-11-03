package com.nihad64.rotam.ui.newplan
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nihad64.rotam.data.model.*
import com.nihad64.rotam.domain.usecase.*
import com.nihad64.rotam.ui.TrFormat
import com.nihad64.rotam.ui.components.*
import com.nihad64.rotam.ui.theme.*
import org.json.JSONObject
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable fun NewPlanScreen(vm:NewPlanViewModel,onClose:()->Unit,onDuplicate:(Long)->Unit={},aiAction:(@Composable ()->Unit)?=null){
 val s by vm.state.collectAsStateWithLifecycle();val goals by vm.goals.collectAsStateWithLifecycle();val templates by vm.templates.collectAsStateWithLifecycle()
 var exit by remember{mutableStateOf(false)};var deleting by remember{mutableStateOf(false)}
 val close={if(vm.dirty())exit=true else onClose()}
 BackHandler {if(!s.editing&&s.step>0)vm.step(s.step-1)else close()}
 LaunchedEffect(vm){for(event in vm.events)if(event=="saved")onClose()}
 if(s.loading){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator()};return}
 Column(Modifier.fillMaxSize().imePadding()){
  Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=24.dp).padding(top=16.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
   PageHeader(if(s.editing)"Planını düzenle"else"Yeni bir adım",if(s.editing)"Tüm ayrıntılar bir arada."else"${s.step+1} / 5",onBack={if(!s.editing&&s.step>0)vm.step(s.step-1)else close()})
   if(!s.editing)Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){repeat(5){i->Box(Modifier.weight(1f).height(3.dp).clip(CircleShape).background(if(i<=s.step)Primary else Outline))}}
   if(s.restored&&!s.editing&&s.step==0)Text("Taslağın kaldığın yerden açıldı.",color=SubtleText,style=MaterialTheme.typography.bodySmall)
   if(s.editing||s.step==0){Text("Ne yapacaksın?",style=MaterialTheme.typography.headlineMedium);RotamField(s.plan.title,{vm.change{p->p.copy(title=it)}},"Planına bir ad ver",Modifier.fillMaxWidth(),singleLine=false)
    if(!s.editing&&templates.isNotEmpty()){Text("Şablonların",style=MaterialTheme.typography.titleMedium);FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){templates.forEach{t->SelectChip(t.title,false,{vm.useTemplate(t)})}}}}
   if(s.editing||s.step==1){Text("Sana göre ayrıntılar",style=MaterialTheme.typography.headlineSmall)
    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){Category.entries.forEach{cat->SelectChip(cat.label,s.plan.category==cat.key,{vm.change{it.copy(category=cat.key)}})}}
    CategoryFields(s.plan,vm::extra)
    RotamField(JSONObject(s.plan.extraData).optString("note"),{vm.extra("note",it)},"Not ekle · isteğe bağlı",Modifier.fillMaxWidth(),singleLine=false)
    RotamField(JSONObject(s.plan.extraData).optString("subtasks"),{vm.extra("subtasks",it)},"Alt adımlar · her satıra bir tane",Modifier.fillMaxWidth(),singleLine=false)
    if(s.editing){val extras=JSONObject(s.plan.extraData);val subtasks=extras.optString("subtasks").lines().filter{it.isNotBlank()};val checked=extras.optString("checkedSubtasks").lines().toSet();subtasks.forEach{task->Row(verticalAlignment=Alignment.CenterVertically){Checkbox(task in checked,{yes->vm.extra("checkedSubtasks",(if(yes)checked+task else checked-task).filter{it.isNotBlank()}.joinToString("\n"))});Text(task)}}}
    if(goals.isNotEmpty()){Text("Bir hedefe bağla",style=MaterialTheme.typography.titleMedium);FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){SelectChip("Bağlantı yok",s.plan.goalId==null,{vm.change{it.copy(goalId=null)}});goals.filter{!it.isCompleted}.forEach{g->SelectChip(g.title,s.plan.goalId==g.id,{vm.change{it.copy(goalId=g.id)}})}}}
   }
   if(s.editing||s.step==2){TimingFields(s.plan){vm.change(it)}}
   if(s.editing||s.step==3){
    if(s.editing&&s.plan.seriesId!=0L){Text("Değişiklik hangi planlara uygulansın?",style=MaterialTheme.typography.titleMedium);FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){EditScope.entries.forEach{SelectChip(it.label,s.scope==it,{vm.scope(it)})}};Text("Tamamlanan ve atlanan geçmiş planlar korunur.",color=SubtleText,style=MaterialTheme.typography.bodySmall)}
    RepeatFields(s.plan,enabled=!s.editing||s.plan.seriesId==0L||s.scope!=EditScope.THIS){vm.change(it)}
    ReminderFields(s.plan){vm.change(it)}
   }
   if(!s.editing&&s.step==4){Text("Son bir bakış",style=MaterialTheme.typography.headlineMedium);RotamCard(Modifier.fillMaxWidth()){
    DetailRow("Plan",s.plan.title){vm.step(0)};DetailRow("Kategori",s.plan.categoryEnum.label){vm.step(1)};DetailRow("Tarih ve saat","${TrFormat.dayMonth(LocalDate.parse(s.plan.date))} · ${PlanTime.label(s.plan)}"){vm.step(2)};DetailRow("Tekrar",RepeatType.fromKey(s.plan.repeatType).label){vm.step(3)};Text(if(s.plan.reminderEnabled)"Hatırlatma açık"else"Hatırlatma yok",color=SubtleText)
   };val note=JSONObject(s.plan.extraData).optString("note");if(note.isNotBlank())Text(note,color=SubtleText)}
   if(s.error!=null)Text(s.error!!,color=Danger)
   if(s.editing){HorizontalDivider(color=Outline);FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
    TextButton(onClick={vm.action("done")},enabled=!s.busy){Text(if(s.plan.isCompleted)"Tamamlamayı geri al"else"Tamamla")}
    TextButton(onClick={vm.action("tomorrow")},enabled=!s.busy){Text("Yarına taşı")};TextButton(onClick={vm.action("skip")},enabled=!s.busy){Text("Atla")}
    TextButton(onClick={onDuplicate(s.plan.id)},enabled=!s.busy){Text("Çoğalt")};TextButton(onClick={vm.saveTemplate()}){Text("Şablon yap")};TextButton(onClick={deleting=true},enabled=!s.busy){Text("Sil",color=Danger)}
   }}
  }
  Column(Modifier.fillMaxWidth().background(Background).padding(horizontal=24.dp,vertical=12.dp)){
   if(aiAction!=null)Box(Modifier.fillMaxWidth().padding(bottom=12.dp),contentAlignment=Alignment.CenterEnd){aiAction()}
   PrimaryButton(if(s.busy)"Kaydediliyor…"else if(s.editing)"Değişiklikleri kaydet"else if(s.step==4)"Planı oluştur"else"Devam et",{if(s.editing||s.step==4)vm.save()else vm.next()},Modifier.fillMaxWidth(),enabled=!s.busy)
   if(!s.editing&&s.step==0&&s.plan.title.isNotBlank())TextButton(onClick={vm.save()},modifier=Modifier.align(Alignment.CenterHorizontally),enabled=!s.busy){Text("Bu bilgilerle kaydet")}
  }
 }
 if(exit)AlertDialog(onDismissRequest={exit=false},title={Text(if(s.editing)"Değişiklikler kaydedilmedi"else"Taslağın korunsun mu?")},text={Text(if(s.editing)"Çıkarsan son değişiklikler uygulanmaz."else"Daha sonra aynı yerden devam edebilirsin.")},confirmButton={TextButton(onClick={exit=false;onClose()}){Text(if(s.editing)"Kaydetmeden çık"else"Taslağı tut")}},dismissButton={TextButton(onClick={if(s.editing)exit=false else{vm.discard();onClose()}}){Text(if(s.editing)"Düzenlemeye dön"else"Taslağı sil")}})
 if(s.conflicts.isNotEmpty())AlertDialog(onDismissRequest={vm.dismissConflicts()},title={Text("Bu aralıkta başka planların var")},text={Text(s.conflicts.take(5).joinToString("\n"))},confirmButton={TextButton(onClick={vm.save(true)},enabled=!s.busy){Text("Yine de kaydet")}},dismissButton={TextButton(onClick={vm.dismissConflicts();if(!s.editing)vm.step(2)}){Text("Saati değiştir")}})
 if(deleting)AlertDialog(onDismissRequest={deleting=false},title={Text("Bu plan silinsin mi?")},text={Text("Tekrarlıysa yalnız bu örnek silinir. İşlemden sonra geri alabilirsin.")},confirmButton={TextButton(onClick={deleting=false;vm.action("delete")}){Text("Sil",color=Danger)}},dismissButton={TextButton(onClick={deleting=false}){Text("Vazgeç")}})
}
@Composable fun RotamField(value:String,onChange:(String)->Unit,placeholder:String,modifier:Modifier=Modifier,singleLine:Boolean=true,number:Boolean=false,isError:Boolean=false){OutlinedTextField(value,onChange,modifier=modifier,label={Text(placeholder)},singleLine=singleLine,minLines=if(singleLine)1 else 2,isError=isError,shape=RoundedCornerShape(20.dp),keyboardOptions=KeyboardOptions(keyboardType=if(number)KeyboardType.Number else KeyboardType.Text),colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=Primary,unfocusedBorderColor=Outline,focusedContainerColor=Surface,unfocusedContainerColor=Surface))}
@Composable private fun CategoryFields(p:Plan,set:(String,String)->Unit){val e=JSONObject(p.extraData);val fields=when(p.categoryEnum){
 Category.KITAP->listOf("book" to "Kitap / kaynak","pageGoal" to "Sayfa hedefi")
 Category.IS->listOf("workplace" to "İş yeri / proje","shift" to "Vardiya / mola notu")
 Category.VIDEO->listOf("project" to "İçerik konusu","platform" to "Platform / aşama")
 Category.SPOR->listOf("activity" to "Aktivite","target" to "Süre / tekrar hedefi")
 Category.IBADET->listOf("activity" to "Faaliyet","target" to "Miktar / tekrar hedefi")
 Category.RANDEVU->listOf("place" to "Yer","person" to "Kişi / hazırlık")
 Category.YEMEK->listOf("meal" to "Yemek adı","shopping" to "Alışveriş / hazırlık")
 Category.OGRENME->listOf("subject" to "Konu / ders","source" to "Kaynak / çalışma hedefi")
 Category.KISISEL->emptyList()};fields.forEach{(key,label)->RotamField(e.optString(key),{set(key,it)},"$label · isteğe bağlı",Modifier.fillMaxWidth())};if(e.optString("legacyDetails").isNotBlank())RotamField(e.optString("legacyDetails"),{set("legacyDetails",it)},"Önceki ayrıntılar",Modifier.fillMaxWidth(),singleLine=false)}
@OptIn(ExperimentalLayoutApi::class)
@Composable private fun TimingFields(p:Plan,change:((Plan)->Plan)->Unit){var dateTarget by remember{mutableStateOf<String?>(null)};var timeTarget by remember{mutableStateOf<String?>(null)}
 Text("Ne zaman?",style=MaterialTheme.typography.headlineSmall);DetailRow("Gün",TrFormat.full(LocalDate.parse(p.date))){dateTarget="date"}
 Text("Başlangıç ve bitiş birbirinden bağımsız. İkisi de boş kalabilir.",color=SubtleText,style=MaterialTheme.typography.bodyMedium)
 DetailRow("Başlangıç",p.startTime?:"Saat ekle · isteğe bağlı"){timeTarget="start"};DetailRow("Bitiş / son teslim",p.endTime?:"Saat ekle · isteğe bağlı"){timeTarget="end"}
 if(p.endTime!=null){FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){SelectChip("Aynı gün",p.endDate==null||p.endDate==p.date,{change{it.copy(endDate=null)}});SelectChip("Ertesi gün",p.endDate==LocalDate.parse(p.date).plusDays(1).toString(),{change{it.copy(endDate=LocalDate.parse(it.date).plusDays(1).toString())}})};DetailRow("Bitiş tarihi",p.endDate?:p.date){dateTarget="end"}}
 Text(PlanTime.label(p),color=Primary,style=MaterialTheme.typography.titleMedium)
 if(dateTarget!=null)DateChooser(if(dateTarget=="end")p.endDate?:p.date else p.date,{dateTarget=null}){value->if(dateTarget=="end")change{it.copy(endDate=value)}else change{PlanTime.moved(it,LocalDate.parse(value))}}
 if(timeTarget!=null)TimeChooser(if(timeTarget=="start")p.startTime else p.endTime,if(timeTarget=="start")"Başlangıç"else"Bitiş / son teslim",{timeTarget=null}){v->if(timeTarget=="start")change{it.copy(startTime=v,reminderTime=null)}else change{it.copy(endTime=v,endDate=if(v==null)null else it.endDate,reminderTime=null)}}
}
@OptIn(ExperimentalLayoutApi::class)
@Composable private fun RepeatFields(p:Plan,enabled:Boolean,change:((Plan)->Plan)->Unit){var until by remember{mutableStateOf(false)};Text("Tekrar",style=MaterialTheme.typography.headlineSmall)
 if(!enabled)Text("Tekrar kuralını değiştirmek için tüm seriyi veya bu ve sonrakileri seç.",color=SubtleText)else{
 FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){RepeatType.entries.forEach{r->SelectChip(r.label,p.repeatType==r.key,{change{it.copy(repeatType=r.key)}})}}
 if(p.repeatType=="custom")FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){TrFormat.dayShort.forEachIndexed{i,d->val days=p.repeatDays.split(',').mapNotNull{it.toIntOrNull()}.toSet();SelectChip(d,i+1 in days,{change{it.copy(repeatDays=(if(i+1 in days)days-(i+1)else days+(i+1)).sorted().joinToString(","))}})}}
 if(p.repeatType in listOf("interval","weekly","monthly"))RotamField(p.repeatEvery.toString(),{v->v.toIntOrNull()?.let{n->change{it.copy(repeatEvery=n)}}},if(p.repeatType=="interval")"Kaç günde bir?"else if(p.repeatType=="weekly")"Kaç haftada bir?"else"Kaç ayda bir?",Modifier.fillMaxWidth(),number=true)
 if(p.repeatType=="monthly"){Text("Seçili gün o ay yoksa",color=SubtleText);FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){SelectChip("O ayı atla",!p.monthLastDay,{change{it.copy(monthLastDay=false)}});SelectChip("Ayın son günü",p.monthLastDay,{change{it.copy(monthLastDay=true)}})}}
 if(p.repeatType!="none"){DetailRow("Tekrarın bitişi",p.repeatUntil?:"Bitiş yok"){until=true};if(p.repeatUntil!=null)TextButton(onClick={change{it.copy(repeatUntil=null)}}){Text("Bitiş sınırını kaldır")}}
 }
 if(until)DateChooser(p.repeatUntil?:p.date,{until=false}){v->change{it.copy(repeatUntil=v)}}
}
@OptIn(ExperimentalLayoutApi::class)
@Composable private fun ReminderFields(p:Plan,change:((Plan)->Plan)->Unit){var picker by remember{mutableStateOf(false)};Row(verticalAlignment=Alignment.CenterVertically){Text("Hatırlatma",style=MaterialTheme.typography.headlineSmall,modifier=Modifier.weight(1f));Switch(p.reminderEnabled,{v->change{it.copy(reminderEnabled=v)}})}
 if(p.reminderEnabled){
 if(p.startTime==null&&p.endTime==null){DetailRow("Ayrı hatırlatma saati",p.reminderTime?:"Saat seç"){picker=true};Text("Bu seçim planına başlangıç saati eklemez.",color=SubtleText,style=MaterialTheme.typography.bodySmall)}else{
 if(p.startTime!=null&&p.endTime!=null)FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){SelectChip("Başlangıçta",!p.remindAtEnd,{change{it.copy(remindAtEnd=false,reminderTime=null)}});SelectChip("Bitişte",p.remindAtEnd,{change{it.copy(remindAtEnd=true,reminderTime=null)}})}
 FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf(0,5,15,30,60).forEach{m->SelectChip(if(m==0)"Tam zamanında"else"$m dk önce",p.reminderMinutes==m,{change{it.copy(reminderMinutes=m,reminderTime=null)}})}}
 }
 Text("Bildirim izinleri ve sessiz saat tercihlerin uygulanır.",color=SubtleText,style=MaterialTheme.typography.bodySmall)
 }
 if(picker)TimeChooser(p.reminderTime,"Hatırlatma saati",{picker=false}){v->change{it.copy(reminderTime=v)}}
}
