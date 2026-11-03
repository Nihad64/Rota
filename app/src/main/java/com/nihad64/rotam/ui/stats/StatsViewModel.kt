package com.nihad64.rotam.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihad64.rotam.AppContainer
import com.nihad64.rotam.data.model.Category
import com.nihad64.rotam.domain.usecase.PlanTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import java.time.LocalDate

data class DayStat(val date:LocalDate,val total:Int,val done:Int) { val ratio get()=if(total==0)0f else done.toFloat()/total }
data class StatsState(
 val week:List<DayStat> = emptyList(),val byCategory:List<Pair<Category,Int>> = emptyList(),val streak:Int=0,val totalDone:Int=0,
 val focusSeconds:Long=0,val plannedMinutes:Long=0,val unknownDuration:Int=0,val skipped:Int=0,val days:Int=7
) {
 val topCategory get()=byCategory.maxByOrNull{it.second}?.first
 val weekRatio get()=week.sumOf{it.done}.toFloat()/week.sumOf{it.total}.coerceAtLeast(1)
}
class StatsViewModel(c:AppContainer):ViewModel() {
 private val period=MutableStateFlow(7)
 private val today=flow{while(true){emit(LocalDate.now());delay(60000)}}.distinctUntilChanged()
 fun setPeriod(days:Int){require(days==7||days==30);period.value=days}
 val state=combine(period,today,c.planRepo.observeAll()) { count,now,all->
  val from=now.minusDays(count-1L);val plans=all.filter{it.date>=from.toString()&&it.date<=now.toString()}
  val days=(count-1 downTo 0).map{off->val d=now.minusDays(off.toLong());val p=plans.filter{it.date==d.toString()};DayStat(d,p.size,p.count{it.isCompleted})}
  val cats=plans.filter{it.isCompleted}.groupingBy{Category.fromKey(it.category)}.eachCount().toList().sortedByDescending{it.second}
  val completed=all.filter{it.isCompleted}.map{LocalDate.parse(it.date)}.toSet()
  var d=if(now in completed)now else now.minusDays(1);var streak=0
  while(d in completed){streak++;d=d.minusDays(1)}
  StatsState(days,cats,streak,plans.count{it.isCompleted},plans.sumOf{it.focusSeconds},plans.mapNotNull{PlanTime.durationMinutes(it)}.sum(),plans.count{PlanTime.durationMinutes(it)==null},plans.count{it.isSkipped},count)
 }.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),StatsState())
}
