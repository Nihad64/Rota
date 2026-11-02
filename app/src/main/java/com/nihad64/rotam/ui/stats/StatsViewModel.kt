package com.nihad64.rotam.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihad64.rotam.AppContainer
import com.nihad64.rotam.data.model.Category
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class DayStat(val date: LocalDate, val total: Int, val done: Int) {
    val ratio get() = if (total == 0) 0f else done.toFloat() / total
}

data class StatsState(
    val week: List<DayStat> = emptyList(),
    val byCategory: List<Pair<Category, Int>> = emptyList(),
    val streak: Int = 0,
    val totalDone: Int = 0
) {
    val topCategory get() = byCategory.maxByOrNull { it.second }?.first
    val weekRatio get() = week.sumOf { it.done }.toFloat() / week.sumOf { it.total }.coerceAtLeast(1)
}

class StatsViewModel(c: AppContainer) : ViewModel() {
    private val today = LocalDate.now()
    val state = combine(
        c.planRepo.observeRange(today.minusDays(6), today),
        c.planRepo.observeCompletedByCategory(),
        c.planRepo.observeCompletedDays()
    ) { weekPlans, cats, days ->
        val week = (6 downTo 0).map { off ->
            val d = today.minusDays(off.toLong())
            val ps = weekPlans.filter { it.date == d.toString() }
            DayStat(d, ps.size, ps.count { it.isCompleted })
        }
        val merged = cats.groupBy { Category.fromKey(it.category) }.mapValues { e -> e.value.sumOf { it.count } }
            .toList().sortedByDescending { it.second }
        StatsState(week, merged, streak(days.map { LocalDate.parse(it) }.toSet()), merged.sumOf { it.second })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsState())

    /** Bugünden (bugün henüz boşsa dünden) geriye doğru ardışık aktif gün sayısı. */
    private fun streak(days: Set<LocalDate>): Int {
        var d = if (today in days) today else today.minusDays(1)
        var n = 0
        while (d in days) { n++; d = d.minusDays(1) }
        return n
    }
}
