package com.nihad64.rotam.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.nihad64.rotam.domain.search.*
import com.nihad64.rotam.domain.usecase.PlanOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nihad64.rotam.AppContainer
import com.nihad64.rotam.data.model.*
import com.nihad64.rotam.domain.usecase.PlanTime
import com.nihad64.rotam.ui.components.*
import com.nihad64.rotam.ui.newplan.RotamField
import com.nihad64.rotam.ui.theme.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale
@OptIn(ExperimentalLayoutApi::class)
@Composable fun SearchScreen(c:AppContainer,onBack:()->Unit,onEdit:(Long)->Unit,onAsk:(String)->Unit={}){val all by c.planRepo.observeAll().collectAsStateWithLifecycle(emptyList());val series by remember(c){c.db.seriesDao().observeAll()}.collectAsStateWithLifecycle(emptyList());var query by rememberSaveable{mutableStateOf("")};var filter by rememberSaveable{mutableIntStateOf(0)};var category by rememberSaveable{mutableStateOf<String?>(null)};val scope=rememberCoroutineScope();var bulk by remember{mutableStateOf(false)};var showFilters by remember{mutableStateOf(false)}
 val today=LocalDate.now().toString()
 val keyboard=LocalSoftwareKeyboardController.current
 val cfg by c.settings.settings.collectAsStateWithLifecycle(com.nihad64.rotam.data.prefs.Settings())
 var pendingAi by remember{mutableStateOf<String?>(null)}
 var found by remember{mutableStateOf(PlanSearchResult("",emptyList()))}
 LaunchedEffect(query,all,today){delay(120);found=withContext(Dispatchers.Default){PlanSearch.search(query,all)}}
 val searching=found.query!=query
 fun submit(){keyboard?.hide();if(PlanSearch.needsAssistant(query)){if(cfg.sharePlans&&cfg.aiContextVersion>=2)onAsk(query)else pendingAi=query}}
 val list=found.plans.filter{p->(category==null||p.category==category)&&when(filter){1->!p.isCompleted&&!p.isSkipped;2->p.isCompleted;3->com.nihad64.rotam.domain.usecase.PlanReviewPolicy.awaiting(p);4->p.isSkipped;else->true}}
 val entries=remember(list,series,query,today){SearchPresentation.entries(list,series,query)}
 LazyColumn(modifier=Modifier.fillMaxSize().testTag("search-list"),contentPadding=PaddingValues(24.dp,24.dp,24.dp,100.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
 item{PageHeader("Planlarında ara","Bir sözcük, bir tarih veya aklındaki soru.",onBack)}
 item{Row{RotamField(query,{query=it.take(5000)},"Örn. Mayıs 2024 · geçen hafta yaptıklarım",Modifier.weight(1f),onSearch={submit()});IconButton(onClick={showFilters=!showFilters}){Icon(Icons.Outlined.FilterList,"Arama filtreleri")}}}
 if(showFilters)item{FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("Tümü","Bekleyen","Tamamlanan","Gözden geçir","Atlanan").forEachIndexed{i,t->SelectChip(t,filter==i,{filter=i})}}}
 if(showFilters)item{FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){SelectChip("Her kategori",category==null,{category=null});Category.entries.forEach{cat->SelectChip(cat.label,category==cat.key,{category=cat.key})}}}
 if(searching)item{LinearProgressIndicator(Modifier.fillMaxWidth())}
 if(!searching&&found.explanation!=null)item{Text(found.explanation!!,color=SubtleText,style=MaterialTheme.typography.bodySmall)}
 if(!searching&&found.answer!=null&&PlanSearch.normalize(query).contains("kac"))item{RotamCard(Modifier.fillMaxWidth()){Diamond();Spacer(Modifier.height(12.dp));Text(found.answer!!,style=MaterialTheme.typography.bodyLarge)}}
 if(PlanSearch.needsAssistant(query))item{Text("Değişiklik önce taslak olarak gösterilir. Sen onaylamadan kaydedilmez.",color=SubtleText);PrimaryButton("Soruyu gönder",{submit()},Modifier.fillMaxWidth())}
 item{SectionTitle(SearchPresentation.countLabel(entries),trailing={if(filter==3&&list.isNotEmpty())TextButton(onClick={bulk=true}){Text("Yarına taşı")}})}
 if(entries.isEmpty()&&!searching&&!found.needsAssistant)item{EmptyState("Eşleşen plan yok.","Başka bir sözcük veya filtre dene.",null,{})}
 items(entries,key={it.key}){entry->when(entry){
  is SearchEntry.Single->{val p=entry.plan;Column{Text(TrFormat.full(LocalDate.parse(p.date)),style=MaterialTheme.typography.labelSmall,color=SubtleText);PlanCard(p,{scope.launch{c.completePlan(p);c.offerUndo("Plan güncellendi",p)}},{onEdit(p.id)})}}
  is SearchEntry.Routine->RoutineSearchCard(entry,onEdit)
 }}
 }
 pendingAi?.let{question->AlertDialog(onDismissRequest={pendingAi=null},title={Text("Kayıtlarına bakarak yardımcı olayım mı?")},text={Text("Bu soruyu yanıtlamak için sorundaki tarih aralığından en fazla 150 planın başlık, kategori, tarih/saat ve tamamlanma özeti Google Gemini'ye gönderilir. Plan paylaşımını Ayarlar → Gizlilik bölümünden kapatabilirsin. Yerel arama ve sayım için bu izin gerekmez.")},confirmButton={TextButton(onClick={pendingAi=null;scope.launch{c.settings.setSharePlans(true);onAsk(question)}}){Text("Plan paylaşımını aç ve sor")}},dismissButton={TextButton(onClick={pendingAi=null}){Text("Vazgeç")}})}
 if(bulk)AlertDialog(onDismissRequest={bulk=false},title={Text("${list.size} plan yarına taşınsın mı?")},text={Text("Saat seçimleri korunur. Tekrarlı planların yalnız bu örnekleri taşınır.")},confirmButton={TextButton(onClick={bulk=false;val before=list.toList();scope.launch{before.forEach{c.movePlanToTomorrow(it)};val after=before.associate{it.id to c.planRepo.byId(it.id)};c.notices.send(com.nihad64.rotam.Notice("${before.size} plan taşındı"){before.forEach{if(c.planRepo.byId(it.id)==after[it.id])c.planRepo.restore(it.copy(focusSeconds=after[it.id]?.focusSeconds?:it.focusSeconds))};c.scheduler.rescheduleAll()})}}){Text("Taşı")}},dismissButton={TextButton(onClick={bulk=false}){Text("Vazgeç")}})
}


@Composable private fun RoutineSearchCard(entry:SearchEntry.Routine,onEdit:(Long)->Unit){
 var expanded by rememberSaveable(entry.seriesId){mutableStateOf(false)};var limit by rememberSaveable(entry.seriesId){mutableIntStateOf(15)}
 RotamCard(Modifier.fillMaxWidth()){
  Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.Repeat,null,tint=Accent);Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(entry.template.title,style=MaterialTheme.typography.titleLarge);Text("Rutin · "+com.nihad64.rotam.ui.routines.routineSchedule(entry.template),color=SubtleText,style=MaterialTheme.typography.bodyMedium)}}
  Spacer(Modifier.height(12.dp));Text("Bir rutin · ${entry.occurrences.size} tekrar",color=SubtleText,style=MaterialTheme.typography.bodySmall)
  val next=entry.representative
  Text("${TrFormat.full(LocalDate.parse(next.date))} · ${PlanTime.label(next)}",color=SubtleText,style=MaterialTheme.typography.bodyMedium,modifier=Modifier.padding(top=8.dp))
  Row{TextButton(onClick={onEdit(next.id)}){Text("Ayrıntılar")};TextButton(onClick={expanded=!expanded}){Text(if(expanded)"Günleri gizle"else"Günleri göster")}}
  if(expanded){entry.occurrences.take(limit).forEach{p->Column(Modifier.fillMaxWidth().clickable{onEdit(p.id)}.padding(vertical=12.dp)){Text(TrFormat.full(LocalDate.parse(p.date)),style=MaterialTheme.typography.titleSmall);Text(PlanTime.label(p)+(if(p.isCompleted)" · Tamamlandı"else if(p.isSkipped)" · Yapılmadı"else""),color=SubtleText,style=MaterialTheme.typography.bodySmall)}};if(entry.occurrences.size>limit)TextButton(onClick={limit+=15}){Text("Daha fazla gün göster")}}
 }
}
