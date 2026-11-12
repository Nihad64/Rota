package com.nihad64.rotam.ui.home
import androidx.lifecycle.*
import com.nihad64.rotam.AppContainer
import com.nihad64.rotam.data.model.Plan
import com.nihad64.rotam.domain.usecase.PlanTime
import com.nihad64.rotam.domain.usecase.PlanOutcome
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.*
data class HomeState(val userName:String="",val today:LocalDate=LocalDate.now(),val now:LocalDateTime=LocalDateTime.now(),val todayPlans:List<Plan> = emptyList(),val missed:List<Plan> = emptyList(),val activeId:Long?=null){
 val total get()=todayPlans.size;val done get()=todayPlans.count{it.isCompleted};val remaining get()=todayPlans.count{!it.isCompleted&&!it.isSkipped}
 val progress get()=if(total==0)0f else done.toFloat()/total
 val hero:Plan? get(){val p=todayPlans.filter{!it.isCompleted&&!it.isSkipped&&(it.id==activeId||missed.none{m->m.uid==it.uid})};return p.firstOrNull{it.id==activeId}?:p.firstOrNull{PlanTime.start(it)?.let{t->t<=now}==true&&(PlanTime.end(it)==null||PlanTime.end(it)!!>now)}?:p.filter{(PlanTime.start(it)?:PlanTime.end(it))?.let{t->t>=now}==true}.minByOrNull{PlanTime.start(it)?:PlanTime.end(it)!!}?:p.firstOrNull{it.startTime==null&&it.endTime==null&&!missed.any{m->m.uid==it.uid}}}
 fun label(p:Plan)=when{p.id==activeId->"ODAKTASIN";PlanTime.overdue(p,now)->"SON TESLİM GEÇTİ";p.startTime==null&&p.endTime!=null->"SON TESLİM";PlanTime.start(p)?.let{it<=now}==true->"ŞİMDİ";p.startTime!=null->"SIRADAKİ";else->"BUGÜN İÇİN"}
}
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(private val c:AppContainer):ViewModel(){
 private val clock=flow{while(true){emit(LocalDateTime.now());delay(30000)}}
 private val today=clock.map{it.toLocalDate()}.distinctUntilChanged()
 val state=combine(c.settings.settings,today.flatMapLatest{c.planRepo.observeDay(it)},c.planRepo.observeAll(),clock,c.focus.active){s,p,all,now,active->HomeState(s.userName,now.toLocalDate(),now,p,all.filter{it.id!=active&&PlanOutcome.status(it) !in listOf(PlanOutcome.Status.COMPLETED,PlanOutcome.Status.NOT_DONE,PlanOutcome.Status.PARTIAL)&&((it.date<now.toLocalDate().toString()&&(PlanTime.end(it)?.let{end->end>now}!=true))||PlanTime.overdue(it,now))},active)}.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),HomeState())
 fun toggle(p:Plan)=viewModelScope.launch{c.completePlan(p);c.offerUndo(if(p.isCompleted)"Plan yeniden açıldı"else"Plan tamamlandı",p)}
 val outcomeError=MutableStateFlow<String?>(null)
 val reporting=MutableStateFlow(false)
 fun report(p:Plan,status:PlanOutcome.Status,amount:Double?=null,target:Double?=null){if(reporting.value)return;reporting.value=true;outcomeError.value=null
  viewModelScope.launch{try{
   val current=c.planRepo.byId(p.id)?:error("Plan bulunamadı.")
   check(current==p){"Plan değişti. Güncel kartı kontrol et."}
   c.focus.pauseIf(p.id)
   val updated=PlanOutcome.record(c.planRepo.byId(p.id)?:current,status,amount,target)
   c.planRepo.update(updated);c.scheduler.cancel(p.id);c.offerUndo("Plan durumu kaydedildi",current)
  }catch(e:Exception){outcomeError.value=e.message?:"Durum kaydedilemedi."}finally{reporting.value=false}}
 }
 fun postpone(p:Plan,date:LocalDate){if(reporting.value)return;reporting.value=true;outcomeError.value=null
  viewModelScope.launch{try{val current=c.planRepo.byId(p.id)?:error("Plan bulunamadı.");check(current==p){"Plan değişti. Güncel kartı kontrol et."}
   c.focus.pauseIf(p.id)
   val moved=PlanTime.moved(PlanOutcome.record(c.planRepo.byId(p.id)?:current,PlanOutcome.Status.POSTPONED),date)
   c.planRepo.update(moved);c.scheduler.schedule(moved);c.offerUndo("Plan ertelendi",current)
  }catch(e:Exception){outcomeError.value=e.message?:"Erteleme kaydedilemedi."}finally{reporting.value=false}}
 }
 fun moveMissed()=viewModelScope.launch{state.value.missed.forEach{c.movePlanToTomorrow(it)}}
}
