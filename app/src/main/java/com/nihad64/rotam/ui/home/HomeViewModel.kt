package com.nihad64.rotam.ui.home
import androidx.lifecycle.*
import com.nihad64.rotam.AppContainer
import com.nihad64.rotam.data.model.Plan
import com.nihad64.rotam.domain.usecase.PlanTime
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.*
data class HomeState(val userName:String="",val today:LocalDate=LocalDate.now(),val now:LocalDateTime=LocalDateTime.now(),val todayPlans:List<Plan> = emptyList(),val missed:List<Plan> = emptyList(),val activeId:Long?=null){
 val total get()=todayPlans.size;val done get()=todayPlans.count{it.isCompleted};val remaining get()=todayPlans.count{!it.isCompleted&&!it.isSkipped}
 val progress get()=if(total==0)0f else done.toFloat()/total
 val hero:Plan? get(){val p=todayPlans.filter{!it.isCompleted&&!it.isSkipped};return p.firstOrNull{it.id==activeId}?:p.firstOrNull{PlanTime.start(it)?.let{t->t<=now}==true&&(PlanTime.end(it)==null||PlanTime.end(it)!!>now)}?:p.filter{(PlanTime.start(it)?:PlanTime.end(it))?.let{t->t>=now}==true}.minByOrNull{PlanTime.start(it)?:PlanTime.end(it)!!}?:p.firstOrNull()}
 fun label(p:Plan)=when{p.id==activeId->"ODAKTASIN";PlanTime.overdue(p,now)->"SON TESLİM GEÇTİ";p.startTime==null&&p.endTime!=null->"SON TESLİM";PlanTime.start(p)?.let{it<=now}==true->"ŞİMDİ";p.startTime!=null->"SIRADAKİ";else->"BUGÜN İÇİN"}
}
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(private val c:AppContainer):ViewModel(){
 private val clock=flow{while(true){emit(LocalDateTime.now());delay(30000)}}
 private val today=clock.map{it.toLocalDate()}.distinctUntilChanged()
 val state=combine(c.settings.settings,today.flatMapLatest{c.planRepo.observeDay(it)},c.planRepo.observeAll(),clock,c.focus.active){s,p,all,now,active->HomeState(s.userName,now.toLocalDate(),now,p,all.filter{!it.isCompleted&&!it.isSkipped&&(it.date<now.toLocalDate().toString()||PlanTime.overdue(it,now))},active)}.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),HomeState())
 fun toggle(p:Plan)=viewModelScope.launch{c.completePlan(p);c.offerUndo(if(p.isCompleted)"Plan yeniden açıldı"else"Plan tamamlandı",p)}
 fun moveMissed()=viewModelScope.launch{state.value.missed.forEach{c.movePlanToTomorrow(it)}}
}
