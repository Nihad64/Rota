package com.nihad64.rotam.ui.goals

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nihad64.rotam.AppContainer
import com.nihad64.rotam.data.model.Category
import com.nihad64.rotam.data.model.Goal
import com.nihad64.rotam.data.model.GoalPeriod
import com.nihad64.rotam.ui.components.*
import com.nihad64.rotam.ui.newplan.RotamField
import com.nihad64.rotam.ui.theme.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class GoalsViewModel(private val c: AppContainer) : ViewModel() {
    val goals = combine(c.goalRepo.observeAll(),c.planRepo.observeAll()) { goals,plans ->
        goals.map { g->val linked=plans.count{it.goalId==g.id && it.isCompleted};val count=g.currentCount+linked;g.copy(currentCount=count,isCompleted=count>=g.targetCount) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(title: String, period: GoalPeriod, target: Int, category: Category) = viewModelScope.launch {
        val due = when (period) {
            GoalPeriod.DAILY -> LocalDate.now()
            GoalPeriod.WEEKLY -> LocalDate.now().plusWeeks(1)
            GoalPeriod.MONTHLY -> LocalDate.now().plusMonths(1)
            GoalPeriod.YEARLY -> LocalDate.now().plusYears(1)
        }
        c.goalRepo.create(Goal(title = title.trim(), period = period.key, targetCount = target, category = category.key, dueDate = due.toString()))
    }

    fun increment(g: Goal) = viewModelScope.launch { c.goalRepo.adjust(g.id,1) }
    fun decrement(g: Goal) = viewModelScope.launch { c.goalRepo.adjust(g.id,-1) }

    fun delete(g: Goal) = viewModelScope.launch { c.goalRepo.delete(g) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoalsScreen(vm: GoalsViewModel, onBack: () -> Unit) {
    val goals by vm.goals.collectAsStateWithLifecycle()
    var deleteTarget by remember { mutableStateOf<Goal?>(null) }
    var title by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("5") }
    var period by remember { mutableStateOf(GoalPeriod.WEEKLY) }
    var category by remember { mutableStateOf(Category.KISISEL) }

    LazyColumn(
        Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Geri", tint = OnBackground) }
                Text("Hedefler", style = MaterialTheme.typography.headlineMedium, color = OnBackground)
            }
        }
        item {
            RotamCard(Modifier.fillMaxWidth()) {
                Text("Yeni hedef", style = MaterialTheme.typography.titleMedium, color = OnBackground)
                Text("Bağlı bir plan tamamlanınca ilerleme otomatik artar; geri alındığında azalır.",color=SubtleText,style=MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(10.dp))
                RotamField(title, { title = it }, "Örn. Haftada 3 kez spor", Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoalPeriod.entries.forEach { SelectChip(it.label, it == period, { period = it }) }
                }
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Category.entries.forEach { SelectChip(it.label, it == category, { category = it }) }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RotamField(target, { target = it.filter(Char::isDigit).take(3) }, "Hedef sayı", Modifier.width(130.dp), number = true)
                    Spacer(Modifier.width(12.dp))
                    PrimaryButton("Ekle", {
                        vm.add(title, period, target.toIntOrNull()?.coerceAtLeast(1) ?: 1, category); title = ""
                    }, Modifier.weight(1f), icon = Icons.Outlined.Add, enabled = title.isNotBlank())
                }
            }
        }
        if (goals.isEmpty()) item {
            Text("Henüz hedef yok. Günlük, haftalık, aylık veya yıllık bir hedef belirle.", color = SubtleText, style = MaterialTheme.typography.bodyMedium)
        }
        items(goals, key = { it.id }) { g ->
            val cat = Category.fromKey(g.category)
            RotamCard(Modifier.fillMaxWidth(), highlight = g.isCompleted) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryIcon(cat, 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(g.title, style = MaterialTheme.typography.titleSmall, color = OnBackground)
                        Text("${GoalPeriod.fromKey(g.period).label} · ${g.currentCount}/${g.targetCount}", style = MaterialTheme.typography.labelSmall, color = SubtleText)
                    }
                    IconButton(onClick = { vm.increment(g) }, enabled = !g.isCompleted) {
                        Icon(if (g.isCompleted) Icons.Outlined.EmojiEvents else Icons.Outlined.AddCircleOutline, "İlerle",
                            tint = if (g.isCompleted) CategoryIbadet else Primary)
                    }
                    IconButton(onClick = { vm.decrement(g) }, enabled=g.currentCount>0) { Icon(Icons.Outlined.RemoveCircleOutline,"Elle eklenen ilerlemeyi azalt",tint=SubtleText) }
                    IconButton(onClick = { deleteTarget=g }) { Icon(Icons.Outlined.Delete, "Sil", tint = SubtleText) }
                }
                Spacer(Modifier.height(8.dp))
                LinearBar(g.currentCount.toFloat() / g.targetCount.coerceAtLeast(1), Modifier.fillMaxWidth())
            }
        }
    }
    deleteTarget?.let { g -> AlertDialog(onDismissRequest={deleteTarget=null},title={Text("Hedef silinsin mi?")},text={Text("Bağlı planların korunacak; yalnız hedef bağlantısı kaldırılacak.")},confirmButton={TextButton(onClick={vm.delete(g);deleteTarget=null}){Text("Sil",color=Danger)}},dismissButton={TextButton(onClick={deleteTarget=null}){Text("Vazgeç")}}) }

}
