package com.nihad64.rotam.ui.newplan
import androidx.lifecycle.*
import com.nihad64.rotam.AppContainer
import com.nihad64.rotam.data.model.*
import com.nihad64.rotam.domain.usecase.PlanTime
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import org.json.*
import java.time.LocalDate
import java.util.UUID

data class FormState(val plan:Plan=Plan(title="",date=LocalDate.now().toString()),val step:Int=0,val editing:Boolean=false,val busy:Boolean=false,val loading:Boolean=true,val error:String?=null,val restored:Boolean=false,val scope:EditScope=EditScope.THIS)
class NewPlanViewModel(private val c:AppContainer,date:LocalDate?=null,private val editId:Long?=null,copyId:Long?=null):ViewModel(){
 private val prefs=c.app.getSharedPreferences("rotam_drafts",0)
 private val _state=MutableStateFlow(FormState());val state=_state.asStateFlow()
 private var original:Plan?=null
 val events=Channel<String>(Channel.BUFFERED)
 val goals=c.goalRepo.observeAll().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val templates=MutableStateFlow(loadTemplates())
 init{viewModelScope.launch{
  try{val old=editId?.let{c.planRepo.byId(it)};original=old
   if(editId!=null&&old==null){_state.value=FormState(loading=false,error="Plan bulunamadı.");return@launch}
   val copy=copyId?.let{c.planRepo.byId(it)}
   val draft=if(editId==null&&copyId==null)runCatching{prefs.getString("new",null)?.let{JSONObject(it)}}.getOrNull()else null
   val plan=old?:copy?.copy(id=0,uid=UUID.randomUUID().toString(),seriesId=0,occurrenceKey=null,originalDate=null,isCompleted=false,isSkipped=false,isDeleted=false,focusSeconds=0)?:draft?.getJSONObject("plan")?.let{PlanJson.decode(it)}?:Plan(title="",date=(date?:LocalDate.now()).toString(),reminderMinutes=c.settings.current().reminderMinutes)
   _state.value=FormState(plan=plan,step=draft?.optInt("step",0)?.coerceIn(0,4)?:0,editing=old!=null,loading=false,restored=draft!=null)
  }catch(_:Exception){_state.update{it.copy(loading=false,error="Plan yüklenemedi. Geri dönüp yeniden dene.")}}
 }}
 fun change(f:(Plan)->Plan){if(state.value.busy)return;_state.update{it.copy(plan=f(it.plan),error=null)};persist()}
 fun extra(key:String,value:String)=change{p->val o=JSONObject(p.extraData);o.put(key,value);p.copy(extraData=o.toString())}
 fun scope(s:EditScope){_state.update{it.copy(scope=s)}}
 fun step(n:Int){_state.update{it.copy(step=n.coerceIn(0,4),error=null)};persist()}
 fun next(){if(state.value.step==0&&state.value.plan.title.isBlank()){_state.update{it.copy(error="Planına bir ad ver.")};return};step(state.value.step+1)}
 fun persist(){if(!state.value.editing&&!state.value.loading)prefs.edit().putString("new",JSONObject().put("plan",PlanJson.encode(state.value.plan)).put("step",state.value.step).toString()).apply()}
 fun discard(){prefs.edit().remove("new").apply()}
 fun save(){val st=state.value;if(st.busy||st.loading)return;val error=PlanTime.validate(st.plan);if(error!=null){_state.update{it.copy(error=error)};return}
  _state.update{it.copy(busy=true,error=null)};viewModelScope.launch{try{val old=original;if(old==null)c.createPlan(st.plan.copy(title=st.plan.title.trim()))else{c.planRepo.edit(old,st.plan.copy(title=st.plan.title.trim()),st.scope);c.scheduler.rescheduleAll()};discard();events.send("saved")}catch(e:Exception){_state.update{it.copy(error=e.message?:"Kaydedilemedi. Bilgilerin korunuyor.")}}finally{_state.update{it.copy(busy=false)}}}}
 fun action(name:String){val p=original?:return;if(state.value.busy)return;_state.update{it.copy(busy=true)};viewModelScope.launch{try{when(name){"done"->c.completePlan(p);"skip"->c.skipPlan(p);"tomorrow"->c.movePlanToTomorrow(p);"delete"->c.deletePlan(p)};c.offerUndo("Plan güncellendi",p);events.send("saved")}catch(_:Exception){_state.update{it.copy(error="İşlem tamamlanamadı.")}}finally{_state.update{it.copy(busy=false)}}}}
 fun dirty()=if(original!=null)state.value.plan!=original else state.value.plan.title.isNotBlank()
 private fun loadTemplates():List<Plan> = runCatching{val a=JSONArray(prefs.getString("templates","[]"));(0 until a.length()).map{PlanJson.decode(a.getJSONObject(it))}}.getOrDefault(emptyList())
 fun saveTemplate(){val p=state.value.plan;if(p.title.isBlank())return;val list=(loadTemplates().filter{it.title!=p.title}+p).takeLast(20);prefs.edit().putString("templates",JSONArray(list.map{PlanJson.encode(it)}).toString()).apply();templates.value=list;_state.update{it.copy(error="Şablon kaydedildi.")}}
 fun useTemplate(t:Plan){change{PlanTime.moved(t,LocalDate.parse(it.date)).copy(id=0,uid=UUID.randomUUID().toString(),seriesId=0,occurrenceKey=null,originalDate=null,isCompleted=false,isSkipped=false,isDeleted=false,focusSeconds=0)};step(1)}
}
