package com.nihad64.rotam.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihad64.rotam.AppContainer
import com.nihad64.rotam.data.model.*
import com.nihad64.rotam.domain.usecase.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import java.time.*

data class DayStat(val date:LocalDate,val total:Int,val done:Int,val focusSeconds:Long=0)
data class RoutineStat(val title:String,val done:Int,val total:Int,val paused:Boolean)
data class StatsWindow(val month:Boolean=false,val anchor:LocalDate=LocalDate.now(),val selected:LocalDate?=null)
data class StatsState(
 val from:LocalDate=LocalDate.now(),val to:LocalDate=LocalDate.now(),val month:Boolean=false,
 val week:List<DayStat> = emptyList(),val byCategory:List<Pair<Category,Int>> = emptyList(),val total:Int=0,val totalDone:Int=0,
 val pending:Int=0,val awaiting:Int=0,val partial:Int=0,val notDone:Int=0,val focusSeconds:Long=0,val legacyFocusSeconds:Long=0,
 val plannedMinutes:Long=0,val unknownDuration:Int=0,val routines:List<RoutineStat> = emptyList(),val selected:LocalDate=LocalDate.now(),val selectedPlans:List<Plan> = emptyList()
)
object StatsBuilder {
 fun build(all:List<Plan>,series:List<PlanSeries>,window:StatsWindow,now:LocalDateTime):StatsState {
  val from=if(window.month)window.anchor.withDayOfMonth(1)else window.anchor.minusDays(window.anchor.dayOfWeek.value-1L)
  val to=if(window.month)from.plusMonths(1).minusDays(1)else from.plusDays(6)
  val visible=all.filterNot{it.isDeleted}.distinctBy{it.uid}
  val plans=visible.filter{it.date>=from.toString()&&it.date<=to.toString()}
  val focus=FocusLedger.byDay(all,from,to)
  val days=generateSequence(from){it.plusDays(1)}.takeWhile{it<=to}.map{d->val p=plans.filter{it.date==d.toString()};DayStat(d,p.size,p.count{it.isCompleted},focus[d]?:0)}.toList()
  val cats=plans.filter{it.isCompleted}.groupingBy{it.categoryEnum}.eachCount().toList().sortedByDescending{it.second}
  var awaiting=0;var pending=0;var partial=0;var notDone=0
  for(p in plans)when(PlanOutcome.status(p)) {
   PlanOutcome.Status.COMPLETED->Unit
   PlanOutcome.Status.NOT_DONE->notDone++
   PlanOutcome.Status.PARTIAL->partial++
   else->if((p.date<now.toLocalDate().toString()&&PlanTime.end(p)?.let{it>now}!=true)||PlanTime.overdue(p,now))awaiting++else pending++
  }
  val routines=series.mapNotNull{s->val p=plans.filter{it.seriesId==s.id};if(p.isEmpty())null else {
   val template=PlanJson.decode(JSONObject(s.template));RoutineStat(template.title,p.count{it.isCompleted},p.size,RoutineState.paused(template))
  }}
  val selected=window.selected?.takeIf{it in from..to}?:now.toLocalDate().takeIf{it in from..to}?:from
  return StatsState(from,to,window.month,days,cats,plans.size,plans.count{it.isCompleted},pending,awaiting,partial,notDone,
   focus.values.sum(),FocusLedger.unassigned(all),plans.mapNotNull{PlanTime.durationMinutes(it)}.sum(),plans.count{PlanTime.durationMinutes(it)==null},routines,selected,
   plans.filter{it.date==selected.toString()}.sortedWith(compareBy<Plan>{it.startTime?:it.endTime?:"99:99"}.thenBy{it.title}))
 }
}
class StatsViewModel(c:AppContainer):ViewModel() {
 private val window=MutableStateFlow(StatsWindow())
 private val clock=flow{while(true){emit(LocalDateTime.now());delay(60000)}}
 fun setPeriod(days:Int){require(days==7||days==30);window.update{it.copy(month=days==30,selected=null)}}
 fun shift(direction:Int){require(direction in -1..1);window.update{it.copy(anchor=if(it.month)it.anchor.plusMonths(direction.toLong())else it.anchor.plusWeeks(direction.toLong()),selected=null)}}
 fun today(){window.update{it.copy(anchor=LocalDate.now(),selected=LocalDate.now())}}
 fun select(date:LocalDate){window.update{it.copy(anchor=date,selected=date)}}
 val state=combine(window,clock,c.db.planDao().observeSyncRecords(),c.db.seriesDao().observeAll()){w,now,all,series->StatsBuilder.build(all,series,w,now)}
  .flowOn(kotlinx.coroutines.Dispatchers.Default).stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),StatsState())
}
