package com.nihad64.rotam.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihad64.rotam.AppContainer
import com.nihad64.rotam.data.model.Plan
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

enum class CalendarMode(val label: String) { DAY("Gün"), WEEK("Hafta"), MONTH("Ay"), YEAR("Yıl") }

data class CalendarState(
    val mode: CalendarMode = CalendarMode.MONTH,
    val selected: LocalDate = LocalDate.now(),
    val anchor: LocalDate = LocalDate.now(),
    val rangePlans: List<Plan> = emptyList()
) {
    val plannedDays: Set<LocalDate> get() = rangePlans.map { LocalDate.parse(it.date) }.toSet()
    val selectedPlans: List<Plan> get() = rangePlans.filter { it.date == selected.toString() }
    val monthCounts: Map<Int, Int> get() = rangePlans.groupingBy { LocalDate.parse(it.date).monthValue }.eachCount()
}

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(private val c: AppContainer) : ViewModel() {
    private val mode = MutableStateFlow(CalendarMode.MONTH)
    private val selected = MutableStateFlow(LocalDate.now())
    private val anchor = MutableStateFlow(LocalDate.now())

    private val plans = combine(mode, anchor) { m, a -> m to a }.flatMapLatest { (m, a) ->
        val (from, to) = when (m) {
            CalendarMode.DAY -> a to a
            CalendarMode.WEEK -> a.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) to a.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            CalendarMode.MONTH -> a.withDayOfMonth(1).minusDays(7) to a.with(TemporalAdjusters.lastDayOfMonth()).plusDays(7)
            CalendarMode.YEAR -> a.withDayOfYear(1) to a.with(TemporalAdjusters.lastDayOfYear())
        }
        c.planRepo.observeRange(from, to)
    }

    val state = combine(mode, selected, anchor, plans) { m, s, a, p -> CalendarState(m, s, a, p) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CalendarState())

    fun setMode(m: CalendarMode) { mode.value = m; anchor.value = selected.value }
    fun select(d: LocalDate) { selected.value = d; if (mode.value == CalendarMode.DAY) anchor.value = d }
    fun openMonth(month: Int) {
        val d = anchor.value.withMonth(month).withDayOfMonth(1)
        anchor.value = d; selected.value = d; mode.value = CalendarMode.MONTH
    }

    fun shift(dir: Int) {
        val a = anchor.value
        val n = when (mode.value) {
            CalendarMode.DAY -> a.plusDays(dir.toLong())
            CalendarMode.WEEK -> a.plusWeeks(dir.toLong())
            CalendarMode.MONTH -> a.plusMonths(dir.toLong())
            CalendarMode.YEAR -> a.plusYears(dir.toLong())
        }
        anchor.value = n
        if (mode.value != CalendarMode.YEAR) selected.value = n
    }

    fun toggle(plan: Plan) = viewModelScope.launch { c.completePlan(plan) }
    fun skip(plan: Plan) = viewModelScope.launch { c.skipPlan(plan) }
    fun delete(plan: Plan) = viewModelScope.launch { c.deletePlan(plan) }
}
