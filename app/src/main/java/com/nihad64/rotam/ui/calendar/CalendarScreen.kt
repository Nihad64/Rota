package com.nihad64.rotam.ui.calendar
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nihad64.rotam.data.model.Category
import com.nihad64.rotam.ui.TrFormat
import com.nihad64.rotam.ui.components.*
import com.nihad64.rotam.ui.theme.*
import java.time.LocalDate
@OptIn(ExperimentalLayoutApi::class)
@Composable fun CalendarScreen(vm:CalendarViewModel,onNewPlan:()->Unit,onEdit:(Long)->Unit){val s by vm.state.collectAsStateWithLifecycle()
 var showFilters by remember{mutableStateOf(false)}
 var category by remember{mutableStateOf<String?>(null)};var status by remember{mutableIntStateOf(0)}
 val visible=s.selectedPlans.filter{p->(category==null||p.category==category)&&when(status){1->!p.isCompleted&&!p.isSkipped;2->p.isCompleted;3->p.isSkipped;else->true}}
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(24.dp,24.dp,24.dp,88.dp),verticalArrangement=Arrangement.spacedBy(20.dp)){
 item{PageHeader("Takvim","Günlerine uzaktan bir bakış.",action={TextButton(onClick={vm.select(LocalDate.now());vm.setMode(CalendarMode.MONTH)}){Text("Bugün")}})}
 item{SegmentedToggle(CalendarMode.entries.map{it.label},s.mode.ordinal,{vm.setMode(CalendarMode.entries[it])})}
 item{Row(verticalAlignment=Alignment.CenterVertically){IconButton(onClick={vm.shift(-1)}){Icon(Icons.Outlined.ChevronLeft,"Önceki dönem")};Text(if(s.mode==CalendarMode.YEAR)s.anchor.year.toString()else TrFormat.monthYear(s.anchor),style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f));IconButton(onClick={vm.shift(1)}){Icon(Icons.Outlined.ChevronRight,"Sonraki dönem")}}}
 if(s.mode==CalendarMode.MONTH||s.mode==CalendarMode.WEEK)item{
  val start=if(s.mode==CalendarMode.MONTH)s.anchor.withDayOfMonth(1).minusDays((s.anchor.withDayOfMonth(1).dayOfWeek.value-1).toLong())else s.anchor.minusDays((s.anchor.dayOfWeek.value-1).toLong())
  val count=if(s.mode==CalendarMode.MONTH)42 else 7
  Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
   Row{TrFormat.dayShort.forEach{Text(it,style=MaterialTheme.typography.labelSmall,color=SubtleText,modifier=Modifier.weight(1f),textAlign=androidx.compose.ui.text.style.TextAlign.Center)}}
   repeat(count/7){week->Row{repeat(7){off->val d=start.plusDays((week*7+off).toLong());val selected=d==s.selected
    Column(Modifier.weight(1f).heightIn(min=48.dp).clip(RoundedCornerShape(16.dp)).background(if(selected)Primary else if(d==LocalDate.now())Surface else Background).clickable{vm.select(d)}.padding(vertical=9.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(d.dayOfMonth.toString(),color=if(selected)Background else if(d.month!=s.anchor.month)SubtleText else Primary,style=MaterialTheme.typography.bodyMedium);Spacer(Modifier.height(4.dp));Box(Modifier.size(3.dp).clip(CircleShape).background(if(d in s.plannedDays)Accent else androidx.compose.ui.graphics.Color.Transparent))}
   }}}
  }
 }
 if(s.mode==CalendarMode.YEAR)items((1..12).toList()){month->DetailRow(TrFormat.months[month-1],"${s.monthCounts[month]?:0} plan"){vm.openMonth(month)}}else{
 item{HorizontalDivider(color=Outline);Spacer(Modifier.height(20.dp));SectionTitle(TrFormat.dayMonth(s.selected),trailing={Row{IconButton(onClick={showFilters=!showFilters}){Icon(Icons.Outlined.FilterList,"Filtreler",tint=if(category!=null||status!=0)Accent else SubtleText)};TextButton(onClick=onNewPlan){Text("Plan ekle")}}})}
 if(showFilters)item{FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("Tümü","Bekleyen","Tamamlanan","Atlanan").forEachIndexed{i,label->SelectChip(label,status==i,{status=i})}}}
 if(showFilters)item{FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){SelectChip("Her kategori",category==null,{category=null});Category.entries.forEach{cat->SelectChip(cat.label,category==cat.key,{category=cat.key})}}}
 if(s.mode==CalendarMode.DAY||s.mode==CalendarMode.WEEK)item{DayTimeline(visible,s.selected,onEdit)}
 if(visible.isEmpty())item{EmptyState(if(s.selectedPlans.isEmpty())"Bu gün henüz boş."else"Bu filtrede plan yok.","Saatli veya saatsiz bir plan ekleyebilir ya da filtreyi değiştirebilirsin.","Plan ekle",onNewPlan)}
 items(visible,key={it.uid}){p->PlanCard(p,{vm.toggle(p)},{onEdit(p.id)})}
 }
 }
}
