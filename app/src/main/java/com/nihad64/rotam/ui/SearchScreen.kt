package com.nihad64.rotam.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
@Composable fun SearchScreen(c:AppContainer,onBack:()->Unit,onEdit:(Long)->Unit){val all by c.planRepo.observeAll().collectAsStateWithLifecycle(emptyList());var query by remember{mutableStateOf("")};var filter by remember{mutableIntStateOf(0)};var category by remember{mutableStateOf<String?>(null)};val scope=rememberCoroutineScope();var bulk by remember{mutableStateOf(false)}
 val locale=Locale.forLanguageTag("tr");val today=LocalDate.now().toString()
 val list=all.filter{p->val hit=(p.title+" "+p.extraData+" "+p.categoryEnum.label).lowercase(locale).contains(query.lowercase(locale));hit&&(category==null||p.category==category)&&when(filter){1->!p.isCompleted&&!p.isSkipped;2->p.isCompleted;3->!p.isCompleted&&!p.isSkipped&&(p.date<today||PlanTime.overdue(p));4->p.isSkipped;else->true}}
 LazyColumn(contentPadding=PaddingValues(24.dp,24.dp,24.dp,100.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
 item{PageHeader("Plan arşivi","Geçmişin ve sıradaki adımların.",onBack)}
 item{RotamField(query,{query=it},"Başlık, not veya kategori ara",Modifier.fillMaxWidth())}
 item{FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("Tümü","Bekleyen","Tamamlanan","Gözden geçir","Atlanan").forEachIndexed{i,t->SelectChip(t,filter==i,{filter=i})}}}
 item{FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){SelectChip("Her kategori",category==null,{category=null});Category.entries.forEach{cat->SelectChip(cat.label,category==cat.key,{category=cat.key})}}}
 item{SectionTitle("${list.size} plan",trailing={if(filter==3&&list.isNotEmpty())TextButton(onClick={bulk=true}){Text("Yarına taşı")}})}
 if(list.isEmpty())item{EmptyState("Eşleşen plan yok.","Başka bir sözcük veya filtre dene.",null,{})}
 items(list,key={it.uid}){p->Column{Text(TrFormat.full(LocalDate.parse(p.date)),style=MaterialTheme.typography.labelSmall,color=SubtleText);PlanCard(p,{scope.launch{c.completePlan(p);c.offerUndo("Plan güncellendi",p)}},{onEdit(p.id)})}}
 }
 if(bulk)AlertDialog(onDismissRequest={bulk=false},title={Text("${list.size} plan yarına taşınsın mı?")},text={Text("Saat seçimleri korunur. Tekrarlı planların yalnız bu örnekleri taşınır.")},confirmButton={TextButton(onClick={bulk=false;val before=list.toList();scope.launch{before.forEach{c.movePlanToTomorrow(it)};c.notices.send(com.nihad64.rotam.Notice("${before.size} plan taşındı"){before.forEach{c.planRepo.restore(it)};c.scheduler.rescheduleAll()})}}){Text("Taşı")}},dismissButton={TextButton(onClick={bulk=false}){Text("Vazgeç")}})
}
