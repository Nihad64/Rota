package com.nihad64.rotam.ui.newplan
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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

@OptIn(ExperimentalLayoutApi::class,ExperimentalMaterial3Api::class)
@Composable fun NewPlanScreen(vm:NewPlanViewModel,onClose:()->Unit,onDuplicate:(Long)->Unit={}){
 val focus=LocalFocusManager.current;val keyboard=LocalSoftwareKeyboardController.current
 val s by vm.state.collectAsStateWithLifecycle();val goals by vm.goals.collectAsStateWithLifecycle();val templates by vm.templates.collectAsStateWithLifecycle()
 var exit by remember{mutableStateOf(false)};var deleting by remember{mutableStateOf(false)}
 var categoryPicker by remember{mutableStateOf(false)};var details by remember{mutableStateOf(false)};var repeats by remember{mutableStateOf(false)};var reminders by remember{mutableStateOf(false)};var options by remember{mutableStateOf(false)}
 val close={if(vm.dirty())exit=true else onClose()}
 BackHandler {close()}
 LaunchedEffect(vm){for(event in vm.events)if(event=="saved")onClose()}
 if(s.loading){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator()};return}
 val extra=JSONObject(s.plan.extraData)
 Column(Modifier.fillMaxSize().imePadding()){
  Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=24.dp).padding(top=16.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)){
   PageHeader(if(s.editing)"Planını düzenle"else"Yeni plan",onBack=close,action={if(s.editing||templates.isNotEmpty())IconButton(onClick={options=true}){Icon(Icons.Outlined.MoreHoriz,"Diğer plan seçenekleri")}})
   Text(if(s.editing)"Gereken ayrıntıları güncelle."else"Bir isim yeter. Diğer ayrıntılar sana bağlı.",style=MaterialTheme.typography.bodyMedium,color=SubtleText)
   if(s.restored&&!s.editing)Text("Kaydetmediğin taslak geri geldi.",color=SubtleText,style=MaterialTheme.typography.bodySmall)
   RotamField(s.plan.title,{vm.change{p->p.copy(title=it)}},"Plan adı",Modifier.fillMaxWidth(),singleLine=false,minLines=1)
   RotamCard(Modifier.fillMaxWidth()){TimingFields(s.plan){vm.change(it)}}
   DetailRow("Kategori",s.plan.categoryEnum.label){categoryPicker=!categoryPicker}
   if(categoryPicker)FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Category.entries.forEach{cat->SelectChip(cat.label,s.plan.category==cat.key,{vm.change{it.copy(category=cat.key)};categoryPicker=false})}}
   HorizontalDivider(color=Outline)
   DetailRow("Tekrar",RepeatType.fromKey(s.plan.repeatType).label){repeats=!repeats}
   if(repeats){
    if(s.editing&&s.plan.seriesId!=0L){Text("Değişiklik hangi günlere uygulansın?",style=MaterialTheme.typography.titleMedium);FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){EditScope.entries.filter{it!=EditScope.ALL}.forEach{SelectChip(it.label,s.scope==it,{vm.scope(it)})}}}
    RepeatFields(s.plan,enabled=!s.editing||s.plan.seriesId==0L||s.scope!=EditScope.THIS){vm.change(it)}
   }
   DetailRow("Hatırlatma",when(ReminderPolicy.mode(s.plan)){ReminderPolicy.Mode.AUTO->"Otomatik";ReminderPolicy.Mode.CUSTOM->"Seçtiğin saatte";ReminderPolicy.Mode.OFF->"Kapalı"}){reminders=!reminders}
   if(reminders)ReminderFields(s.plan){vm.change(it)}
   DetailRow("Not ve adımlar",if(extra.optString("note").isBlank()&&extra.optString("subtasks").isBlank())"İsteğe bağlı"else"Ayrıntıları gör"){details=!details}
   if(details){
    RotamField(extra.optString("note"),{vm.extra("note",it)},"Not",Modifier.fillMaxWidth(),singleLine=false)
    RotamField(extra.optString("subtasks"),{vm.extra("subtasks",it)},"Adımlar · her satıra bir tane",Modifier.fillMaxWidth(),singleLine=false)
    CategoryFields(s.plan,vm::extra)
    if(goals.isNotEmpty()){Text("Hedef bağlantısı",style=MaterialTheme.typography.titleMedium);FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){SelectChip("Yok",s.plan.goalId==null,{vm.change{it.copy(goalId=null)}});goals.forEach{g->SelectChip(g.title,s.plan.goalId==g.id,{vm.change{it.copy(goalId=g.id)}})}}}
   }
   if(s.error!=null)Text(s.error!!,color=Danger)
  }
  Column(Modifier.fillMaxWidth().background(Background).padding(horizontal=24.dp,vertical=12.dp)){
   PrimaryButton(if(s.busy)"Kaydediliyor…"else if(s.editing)"Değişiklikleri kaydet"else"Planı kaydet",{focus.clearFocus();keyboard?.hide();vm.save()},Modifier.fillMaxWidth(),enabled=!s.busy&&s.plan.title.isNotBlank())
  }
 }
 if(options)ModalBottomSheet(onDismissRequest={options=false},containerColor=Surface){Column(Modifier.fillMaxWidth().padding(horizontal=24.dp).padding(bottom=32.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
  if(s.editing){TextButton(onClick={options=false;onDuplicate(s.plan.id)}){Text("Kopyasını oluştur")};TextButton(onClick={options=false;vm.saveTemplate()}){Text("Şablon olarak sakla")};TextButton(onClick={options=false;deleting=true}){Text("Planı sil",color=Danger)}}
  else{Text("Kayıtlı şablonlar",style=MaterialTheme.typography.titleLarge);templates.forEach{t->TextButton(onClick={options=false;vm.useTemplate(t)}){Text(t.title)}}}
 }}
 if(exit)AlertDialog(onDismissRequest={exit=false},title={Text(if(s.editing)"Değişiklikler kaydedilmedi"else"Taslağın korunsun mu?")},text={Text(if(s.editing)"Çıkarsan son değişiklikler uygulanmaz."else"Daha sonra kaldığın yerden devam edebilirsin.")},confirmButton={TextButton(onClick={exit=false;onClose()}){Text(if(s.editing)"Kaydetmeden çık"else"Taslağı tut")}},dismissButton={TextButton(onClick={if(s.editing)exit=false else{vm.discard();onClose()}}){Text(if(s.editing)"Düzenlemeye dön"else"Taslağı sil")}})
 if(s.conflicts.isNotEmpty())AlertDialog(onDismissRequest={vm.dismissConflicts()},title={Text("Bu aralıkta başka planların var")},text={Text(s.conflicts.take(5).joinToString("\n"))},confirmButton={TextButton(onClick={vm.save(true)},enabled=!s.busy){Text("Yine de kaydet")}},dismissButton={TextButton(onClick={vm.dismissConflicts()}){Text("Saati değiştir")}})
 if(deleting)AlertDialog(onDismissRequest={deleting=false},title={Text("Bu plan silinsin mi?")},text={Text("Rutinse yalnız bu günkü örnek silinir. Rutinin tamamını Rutinler ekranından kaldırabilirsin.")},confirmButton={TextButton(onClick={deleting=false;vm.action("delete")}){Text("Sil",color=Danger)}},dismissButton={TextButton(onClick={deleting=false}){Text("Vazgeç")}})
}
@OptIn(ExperimentalFoundationApi::class)
@Composable fun RotamField(value:String,onChange:(String)->Unit,placeholder:String,modifier:Modifier=Modifier,singleLine:Boolean=true,number:Boolean=false,isError:Boolean=false,onSearch:(()->Unit)?=null,minLines:Int=if(singleLine)1 else 2){
 val requester=remember{BringIntoViewRequester()};var focused by remember{mutableStateOf(false)}
 val imeBottom=WindowInsets.ime.getBottom(LocalDensity.current)
 LaunchedEffect(focused,imeBottom){if(focused)requester.bringIntoView()}
 OutlinedTextField(value,onChange,modifier=modifier.bringIntoViewRequester(requester).onFocusChanged{focused=it.isFocused},label={Text(placeholder)},singleLine=singleLine,minLines=minLines,isError=isError,shape=RoundedCornerShape(20.dp),keyboardOptions=KeyboardOptions(keyboardType=if(number)KeyboardType.Number else KeyboardType.Text,imeAction=if(onSearch!=null)androidx.compose.ui.text.input.ImeAction.Search else androidx.compose.ui.text.input.ImeAction.Default),keyboardActions=androidx.compose.foundation.text.KeyboardActions(onSearch={onSearch?.invoke()}),colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=Primary,unfocusedBorderColor=Outline,focusedContainerColor=Surface,unfocusedContainerColor=Surface))}
@Composable private fun CategoryFields(p:Plan,set:(String,String)->Unit){val e=JSONObject(p.extraData);val fields=when(p.categoryEnum){
 Category.KITAP->listOf("book" to "Kitap / kaynak","pageGoal" to "Sayfa hedefi")
 Category.IS->listOf("workplace" to "İş yeri / proje","shift" to "Vardiya / mola notu")
 Category.VIDEO->listOf("project" to "İçerik konusu","platform" to "Platform / aşama")
 Category.SPOR->listOf("activity" to "Aktivite","target" to "Süre / tekrar hedefi")
 Category.IBADET->listOf("activity" to "Faaliyet","target" to "Miktar / tekrar hedefi")
 Category.RANDEVU->listOf("place" to "Yer","person" to "Kişi / hazırlık")
 Category.YEMEK->listOf("meal" to "Yemek adı","shopping" to "Alışveriş / hazırlık")
 Category.OGRENME->listOf("subject" to "Konu / ders","source" to "Kaynak / çalışma hedefi")
 Category.KISISEL->emptyList()};fields.filter{e.optString(it.first).isNotBlank()}.forEach{(key,label)->RotamField(e.optString(key),{set(key,it)},"$label · isteğe bağlı",Modifier.fillMaxWidth())};if(e.optString("legacyDetails").isNotBlank())RotamField(e.optString("legacyDetails"),{set("legacyDetails",it)},"Önceki ayrıntılar",Modifier.fillMaxWidth(),singleLine=false)}
@OptIn(ExperimentalLayoutApi::class)
@Composable private fun TimingFields(p:Plan,change:((Plan)->Plan)->Unit){var dateTarget by remember{mutableStateOf<String?>(null)};var timeTarget by remember{mutableStateOf<String?>(null)}
 Text("Zaman",style=MaterialTheme.typography.titleMedium);DetailRow("Gün",TrFormat.full(LocalDate.parse(p.date))){dateTarget="date"}

 DetailRow("Başlangıç",p.startTime?:"Saat yok"){timeTarget="start"};DetailRow("Bitiş / son teslim",p.endTime?:"Saat yok"){timeTarget="end"}
 if(p.endTime!=null){FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){SelectChip("Aynı gün",p.endDate==null||p.endDate==p.date,{change{it.copy(endDate=null)}});SelectChip("Ertesi gün",p.endDate==LocalDate.parse(p.date).plusDays(1).toString(),{change{it.copy(endDate=LocalDate.parse(it.date).plusDays(1).toString())}})};DetailRow("Bitiş tarihi",p.endDate?:p.date){dateTarget="end"}}

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
@Composable private fun ReminderFields(p:Plan,change:((Plan)->Plan)->Unit){
 var picker by remember{mutableStateOf(false)}
 val mode=ReminderPolicy.mode(p)
 Text("Hatırlatmalar",style=MaterialTheme.typography.headlineSmall)
 FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
  ReminderPolicy.Mode.entries.forEach{m->SelectChip(when(m){ReminderPolicy.Mode.AUTO->"Otomatik";ReminderPolicy.Mode.CUSTOM->"Özel";ReminderPolicy.Mode.OFF->"Kapalı"},mode==m,{change{ReminderPolicy.withMode(it,m)}})}
 }
 if(mode==ReminderPolicy.Mode.AUTO)Text(if(p.startTime==null&&p.endTime==null)"Saat eklemeden gün içinde durumunu sorar. Yanıtsızsa ertesi gün son bir kez hatırlatır."else"Planına göre hazırlık, zamanında uyarı ve sınırlı takip. Başladığında veya tamamladığında durur.",color=SubtleText,style=MaterialTheme.typography.bodyMedium)
 if(mode==ReminderPolicy.Mode.CUSTOM){
  if(p.startTime==null&&p.endTime==null){DetailRow("Ayrı hatırlatma saati",p.reminderTime?:"Saat seç"){picker=true};Text("Bu seçim planına başlangıç saati eklemez.",color=SubtleText,style=MaterialTheme.typography.bodySmall)}else{
   if(p.startTime!=null&&p.endTime!=null)FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){SelectChip("Başlangıçta",!p.remindAtEnd,{change{it.copy(remindAtEnd=false,reminderTime=null)}});SelectChip("Bitişte",p.remindAtEnd,{change{it.copy(remindAtEnd=true,reminderTime=null)}})}
   FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf(0,5,15,30,60).forEach{m->SelectChip(if(m==0)"Tam zamanında"else"$m dk önce",p.reminderMinutes==m,{change{it.copy(reminderMinutes=m,reminderTime=null)}})}}
  }
 }
 if(mode!=ReminderPolicy.Mode.OFF)Text("Bildirim izinleri ve sessiz saatler korunur. Bildirimi kapatırsan o planın takibi durur.",color=SubtleText,style=MaterialTheme.typography.bodySmall)
 if(picker)TimeChooser(p.reminderTime,"Hatırlatma saati",{picker=false}){v->change{it.copy(reminderTime=v)}}
}
