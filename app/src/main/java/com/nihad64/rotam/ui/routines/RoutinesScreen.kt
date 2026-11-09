package com.nihad64.rotam.ui.routines

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
import com.nihad64.rotam.ui.theme.*
import org.json.JSONObject
import java.time.LocalDate

/** Lists actual recurring plan series; legacy goal records remain available separately. */
@Composable fun RoutinesScreen(c:AppContainer,onNew:()->Unit,onEdit:(Long)->Unit,onGoals:()->Unit) {
 val series by remember(c){c.db.seriesDao().observeAll()}.collectAsStateWithLifecycle(emptyList())
 val plans by remember(c){c.planRepo.observeAll()}.collectAsStateWithLifecycle(emptyList())
 val today=LocalDate.now().toString()
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(24.dp,20.dp,24.dp,48.dp),verticalArrangement=Arrangement.spacedBy(20.dp)){
  item { PageHeader("Rutinler","Küçük adımlar, kendi ritminde.") }
  item { PrimaryButton("Yeni rutin oluştur",onNew,Modifier.fillMaxWidth()) }
  if(series.isEmpty())item { RotamCard(Modifier.fillMaxWidth()) {
   Diamond();Spacer(Modifier.height(18.dp));Text("Tekrar eden şeylere yer aç.",style=MaterialTheme.typography.headlineSmall)
   Spacer(Modifier.height(10.dp));Text("Okuma, spor veya çalışma… Plan oluştururken tekrar günlerini seç. Günü gelen rutinlerin Bugün ve Takvim’de görünür.",color=SubtleText)
  }}
  items(series,key={it.id}){s->
   val template=remember(s.template){runCatching{PlanJson.decode(JSONObject(s.template))}.getOrNull()}
   val instances=plans.filter{it.seriesId==s.id}
   val next=instances.filter{it.date>=today&&!it.isCompleted&&!it.isSkipped}.minWithOrNull(compareBy<Plan>{it.date}.thenBy{it.startTime?:"99:99"})
   val last=instances.maxByOrNull{it.date}
   RotamCard(Modifier.fillMaxWidth()) {
    Text(template?.title ?: "Rutin bilgisi okunamadı",style=MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
    template?.let {Text(RepeatType.fromKey(it.repeatType).label+if(it.startTime!=null)" · ${it.startTime}" else " · Gün içinde",color=SubtleText)}
    Spacer(Modifier.height(16.dp))
    Text(if(next!=null)"Sıradaki · ${next.date} · ${PlanTime.label(next)}" else "Yaklaşan plan yok",style=MaterialTheme.typography.bodyMedium)
    val target=next?:last
    if(target!=null)TextButton(onClick={onEdit(target.id)}){Text("Rutini düzenle")}
   }
  }
  item { TextButton(onClick=onGoals){Text("Önceki hedeflerim") } }
 }
}
