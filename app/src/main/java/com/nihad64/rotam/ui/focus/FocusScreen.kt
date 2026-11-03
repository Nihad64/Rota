package com.nihad64.rotam.ui.focus

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nihad64.rotam.AppContainer
import com.nihad64.rotam.data.model.Plan
import com.nihad64.rotam.domain.usecase.PlanTime
import com.nihad64.rotam.ui.components.*
import com.nihad64.rotam.ui.theme.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class FocusState(val plan: Plan? = null, val seconds: Long = 0, val running: Boolean = false, val loading: Boolean = true, val busy: Boolean = false, val error: String? = null)
class FocusViewModel(private val c: AppContainer, private val id: Long): ViewModel() {
    private val mutable = MutableStateFlow(FocusState())
    val state = mutable.asStateFlow()
    init { viewModelScope.launch {
        while (isActive) {
            val p = c.planRepo.byId(id)?.takeUnless { it.isDeleted }
            mutable.update { it.copy(plan=p, seconds=c.focus.seconds(id, p?.focusSeconds ?: 0), running=c.focus.active.value==id, loading=false) }
            delay(1000)
        }
    } }
    fun toggle() = perform { if(c.focus.active.value==id)c.focus.pause() else c.focus.start(id) }
    fun complete() = perform {
        if(c.focus.active.value==id)c.focus.pause()
        val p=c.planRepo.byId(id) ?: return@perform
        c.completePlan(p, true)
        mutable.update { it.copy(plan=c.planRepo.byId(id), running=false) }
        c.offerUndo("Plan tamamlandı",p)
    }
    private fun perform(action: suspend ()->Unit) {
        if(state.value.busy)return
        mutable.update { it.copy(busy=true,error=null) }
        viewModelScope.launch { try { action() } catch(_:Exception) { mutable.update { it.copy(error="İşlem tamamlanamadı. Tekrar dene.") } } finally { mutable.update { it.copy(busy=false) } } }
    }
}

@Composable fun FocusScreen(vm: FocusViewModel, onBack: ()->Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    var countdown by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment=Alignment.CenterHorizontally, verticalArrangement=Arrangement.spacedBy(24.dp)) {
        PageHeader("Odak", "Bir seferde bir adım.", onBack)
        if(s.loading) { CircularProgressIndicator(); return@Column }
        val p=s.plan
        if(p==null) { Text("Bu plan bulunamadı."); return@Column }
        CategoryIcon(p.categoryEnumCompat(),56.dp)
        Text(p.title,style=MaterialTheme.typography.headlineMedium,textAlign=TextAlign.Center)
        Text(PlanTime.label(p),color=SubtleText,textAlign=TextAlign.Center)
        val duration=PlanTime.durationMinutes(p)?.times(60)
        if(duration!=null) SegmentedToggle(listOf("Kronometre","Geri sayım"),if(countdown)1 else 0,{countdown=it==1})
        val shown=if(countdown && duration!=null)(duration-s.seconds).coerceAtLeast(0) else s.seconds
        Spacer(Modifier.height(20.dp))
        Text("%02d:%02d:%02d".format(shown/3600,shown/60%60,shown%60),style=MaterialTheme.typography.displayMedium)
        Text(when { p.isCompleted->"Tamamlandı"; countdown && duration!=null && s.seconds>=duration->"Süre doldu. Bitirdiğinde tamamlayabilirsin."; s.running->"Odaktasın"; else->"Hazır olduğunda başla" },color=SubtleText,textAlign=TextAlign.Center)
        if(!p.isCompleted && !p.isSkipped) {
            PrimaryButton(if(s.running)"Duraklat" else if(s.seconds>0)"Devam et" else "Başlat",{vm.toggle()},Modifier.fillMaxWidth(),icon=if(s.running)Icons.Outlined.Pause else Icons.Outlined.PlayArrow,enabled=!s.busy)
            OutlinedButton(onClick={vm.complete()},modifier=Modifier.fillMaxWidth().heightIn(min=54.dp),enabled=!s.busy) { Text("Planı tamamla") }
        }
        Text("Ölçülen çalışma süren ayrı kaydedilir. Sayaç bitince plan kendiliğinden tamamlanmaz.",color=SubtleText,style=MaterialTheme.typography.bodySmall,textAlign=TextAlign.Center)
        s.error?.let { Text(it,color=Danger) }
    }
}
private fun Plan.categoryEnumCompat() = com.nihad64.rotam.data.model.Category.fromKey(category)
