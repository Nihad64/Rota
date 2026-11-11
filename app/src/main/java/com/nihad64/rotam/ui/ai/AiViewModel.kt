package com.nihad64.rotam.ui.ai
import androidx.lifecycle.*
import com.nihad64.rotam.AppContainer
import com.nihad64.rotam.data.model.EditScope
import com.nihad64.rotam.domain.search.PlanSearch
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ChatMessage(val id:Long,val isUser:Boolean,val text:String,val suggestions:List<PlanSuggestion> = emptyList(),val added:Set<Int> = emptySet(),val isError:Boolean=false)
data class AiState(val messages:List<ChatMessage> = emptyList(),val loading:Boolean=false,val applying:Boolean=false,val input:String="",val applyError:String?=null)
class AiViewModel(private val c:AppContainer):ViewModel(){
 private val _state=MutableStateFlow(AiState());val state=_state.asStateFlow();private var id=1L
 val settings=c.settings.settings.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),com.nihad64.rotam.data.prefs.Settings())
 val starters=listOf("Yarın için saatsiz bir okuma planı öner","Bu hafta için çalışma planı hazırlayalım")
 private var editingPlanId:Long?=null
 val editingSelection=MutableStateFlow(false)
 fun setEditingPlan(id:Long?){editingPlanId=id;editingSelection.value=id!=null}
 private var planningDate:LocalDate?=null
 fun setPlanningDate(date:LocalDate?){planningDate=date}
 fun setInput(v:String){_state.update{it.copy(input=v.take(5000))}}
 fun send(text:String=state.value.input){if(text.isBlank()||state.value.loading)return
  _state.update{it.copy(messages=it.messages+ChatMessage(id++,true,text.trim()),input="",loading=true,applyError=null)}
  viewModelScope.launch{try{val cfg=c.settings.current()
   val date=if(cfg.aiContextVersion>=2)PlanSearch.period(text)else null
   val ctx=if(editingPlanId!=null)listOfNotNull(c.planRepo.byId(editingPlanId!!)?.takeUnless{it.isDeleted})else if(cfg.sharePlans)c.planRepo.range(date?.from?:LocalDate.now().minusDays(1),date?.to?:LocalDate.now().plusDays(14)).take(150)else emptyList()
   val history=state.value.messages.filter{!it.isError}.map{it.isUser to it.text}
   val local=if(PlanSearch.localQuestion(text))PlanSearch.search(text,c.planRepo.observeAll().first()).answer else null
   val r=if(local!=null)AiReply(local,emptyList())else c.gemini.ask(history,ctx,planningDate);_state.update{it.copy(messages=it.messages+ChatMessage(id++,false,r.text,r.suggestions))}
  }catch(e:kotlinx.coroutines.CancellationException){throw e}catch(e:Exception){_state.update{it.copy(input=if(it.input.isBlank())text else it.input,messages=it.messages+ChatMessage(id++,false,if(e is GeminiException)e.message.orEmpty()else"Bağlantı kurulamadı. İnternetini kontrol edip yeniden dene.",isError=true))}}finally{_state.update{it.copy(loading=false)}}}
 }
 fun retry(){if(state.value.loading||state.value.messages.lastOrNull()?.isError!=true)return
  val last=state.value.messages.lastOrNull{it.isUser}?:return
  _state.update{it.copy(messages=it.messages.takeWhile{m->m.id<last.id})};send(last.text)}
 fun addSuggestion(messageId:Long,index:Int)=apply(messageId,setOf(index))
 fun addAll(messageId:Long){val m=state.value.messages.firstOrNull{it.id==messageId}?:return;apply(messageId,m.suggestions.indices.toSet())}
 private fun apply(mid:Long,indices:Set<Int>){if(state.value.applying)return;_state.update{it.copy(applying=true,applyError=null)}
  viewModelScope.launch{try{for(index in indices){val m=state.value.messages.firstOrNull{it.id==mid}?:break;if(index in m.added)continue;val s=m.suggestions.getOrNull(index)?:continue
   if(s.before!=null){if(c.planRepo.byId(s.before.id)!=s.before)throw GeminiException("Bu plan öneriden sonra değişmiş. Güncel haliyle yeni bir öneri iste.");c.focus.pauseIf(s.before.id);c.planRepo.edit(s.before,s.plan,EditScope.THIS)}else c.planRepo.create(s.plan)
   _state.update{st->st.copy(messages=st.messages.map{if(it.id==mid)it.copy(added=it.added+index)else it})}
  };c.scheduler.rescheduleAll()
  }catch(e:Exception){_state.update{it.copy(applyError=e.message?:"Öneri kaydedilemedi. Kaydedilenler işaretli; kalanları yeniden deneyebilirsin.")}}finally{_state.update{it.copy(applying=false)}}}
 }
 fun clear(){if(!state.value.loading&&!state.value.applying)_state.value=AiState()}
}
