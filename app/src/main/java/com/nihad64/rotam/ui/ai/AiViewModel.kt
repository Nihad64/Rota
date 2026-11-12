package com.nihad64.rotam.ui.ai
import androidx.lifecycle.*
import com.nihad64.rotam.AppContainer
import com.nihad64.rotam.data.model.EditScope
import com.nihad64.rotam.domain.search.PlanSearch
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.*
import java.time.LocalDate

data class ChatMessage(val id:Long,val isUser:Boolean,val text:String,val suggestions:List<PlanSuggestion> = emptyList(),val added:Set<Int> = emptySet(),val isError:Boolean=false,val memories:List<MemorySuggestion> = emptyList(),val remembered:Set<Int> = emptySet())
data class AiState(val messages:List<ChatMessage> = emptyList(),val loading:Boolean=false,val applying:Boolean=false,val input:String="",val applyError:String?=null,val restored:Boolean=false,val restoring:Boolean=true)
class AiViewModel(private val c:AppContainer):ViewModel(){
 private val drafts=AiDraftStore(c.app.noBackupFilesDir)
 private var owner=c.account.account.value?.uid
 private var pendingInput=""
 private var request:Job?=null
 private var restoring:Job?=null
 private var generation=0L
 private var draftWritable=false
 private fun persist(){if(draftWritable)drafts.update(owner,AiDraft(state.value.input,pendingInput))}
 private fun loadDraft(){val who=owner;val turn=generation;restoring=viewModelScope.launch{
  try{val draft=drafts.load(who);if(owner==who&&turn==generation){draftWritable=true;pendingInput="";_state.value=AiState(input=draft.input.ifBlank{draft.pending},restored=draft.input.isNotBlank()||draft.pending.isNotBlank(),restoring=false)}}
  catch(e:CancellationException){throw e}catch(_:Exception){if(owner==who)_state.value=AiState(restoring=false,applyError="Önceki AI taslağı okunamadı. Dosya korundu; yeni bir taslağı kaydetmeden önce yeniden açmayı deneyebilirsin.")}
 }}
 private val _state=MutableStateFlow(AiState());val state=_state.asStateFlow();private var id=1L
 val settings=c.settings.settings.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),com.nihad64.rotam.data.prefs.Settings())
 val memories=combine(c.memory.observeAll(),c.account.account){all,account->all.filter{!it.isDeleted&&it.ownerUid==account?.uid}}.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val draftError=drafts.error
 val syncStatus=c.sync.status
 val memoryError=MutableStateFlow<String?>(null)
 val memoryBusy=MutableStateFlow(false)
 val starters=listOf("Yarın için saatsiz bir okuma planı öner","Bu hafta için çalışma planı hazırlayalım")
 private var editingPlanId:Long?=null
 val editingSelection=MutableStateFlow(false)
 init {
  loadDraft()
  viewModelScope.launch{c.account.account.map{it?.uid}.distinctUntilChanged().collect{uid->if(uid!=owner){
   persist();generation++;request?.cancel();restoring?.cancel();owner=uid;draftWritable=false;pendingInput="";editingPlanId=null;planningDate=null;editingSelection.value=false;memoryError.value=null;_state.value=AiState();loadDraft()
  }}}
 }
 fun setEditingPlan(id:Long?){editingPlanId=id;editingSelection.value=id!=null}
 private var planningDate:LocalDate?=null
 fun setPlanningDate(date:LocalDate?){planningDate=date}
 fun setInput(v:String){if(state.value.restoring)restoring?.cancel();draftWritable=true;if(!state.value.loading)pendingInput="";_state.update{it.copy(input=v.take(5000),restored=false,restoring=false)};persist()}
 fun send(text:String=state.value.input)=sendInternal(text,false)
 private fun sendInternal(text:String,keepInput:Boolean){if(text.isBlank()||text.length>5000||state.value.loading||state.value.restoring)return
  if(c.account.account.value?.uid!=owner)return
  val who=owner;val turn=generation;pendingInput=text.trim()
  _state.update{it.copy(messages=it.messages+ChatMessage(id++,true,text.trim()),input=if(keepInput)it.input else "",loading=true,applyError=null,restored=false)}
  persist()
  request=viewModelScope.launch{try{try{drafts.flush(who)}catch(e:Exception){throw GeminiException("Metin cihazda taslak olarak saklanamadı. Boş alanı kontrol et; metnin ekranda korunuyor.",e)};check(c.account.account.value?.uid==who);val cfg=c.settings.current()
   val date=if(cfg.aiContextVersion>=2)PlanSearch.period(text)else null
   val ctx=if(editingPlanId!=null)listOfNotNull(c.planRepo.byId(editingPlanId!!)?.takeUnless{it.isDeleted})else if(cfg.sharePlans)c.planRepo.range(date?.from?:LocalDate.now().minusDays(1),date?.to?:LocalDate.now().plusDays(14)).take(150)else emptyList()
   val history=state.value.messages.filter{!it.isError}.map{it.isUser to it.text}
   val local=if(PlanSearch.localQuestion(text))PlanSearch.search(text,c.planRepo.observeAll().first()).answer else null
   val r=if(local!=null)AiReply(local,emptyList())else c.gemini.ask(history,ctx,planningDate,who);if(owner!=who||turn!=generation)return@launch;pendingInput="";_state.update{it.copy(messages=it.messages+ChatMessage(id++,false,r.text,r.suggestions,memories=r.memories))}
  }catch(e:kotlinx.coroutines.CancellationException){throw e}catch(e:Exception){if(owner!=who||turn!=generation)return@launch;_state.update{it.copy(input=if(it.input.isBlank())text else it.input,messages=it.messages+ChatMessage(id++,false,if(e is GeminiException)e.message.orEmpty()else"Bağlantı kurulamadı. İnternetini kontrol edip yeniden dene.",isError=true))}}finally{if(owner==who&&turn==generation){_state.update{it.copy(loading=false)};persist()}}}
 }
 fun retry(){if(state.value.loading||state.value.messages.lastOrNull()?.isError!=true)return
  val last=state.value.messages.lastOrNull{it.isUser}?:return
  _state.update{it.copy(messages=it.messages.takeWhile{m->m.id<last.id})};sendInternal(last.text,state.value.input.isNotBlank()&&state.value.input!=last.text)}
 fun reviseSuggestion(mid:Long,index:Int,plan:com.nihad64.rotam.data.model.Plan):String? {
  val m=state.value.messages.firstOrNull{it.id==mid}?:return "Öneri artık mevcut değil."
  val old=m.suggestions.getOrNull(index)?:return "Öneri bulunamadı."
  if(index in m.added||state.value.applying)return "Bu öneri zaten kaydedildi. Plan detayından düzenleyebilirsin."
  val issue=com.nihad64.rotam.domain.usecase.PlanTime.validate(plan);if(issue!=null)return issue
  check(plan.uid==old.plan.uid&&plan.id==old.plan.id)
  _state.update{st->st.copy(messages=st.messages.map{msg->if(msg.id!=mid)msg else msg.copy(suggestions=msg.suggestions.mapIndexed{i,v->if(i==index)v.copy(plan=plan)else v})})}
  return null
 }
 fun addSuggestion(messageId:Long,index:Int)=apply(messageId,setOf(index))
 fun addAll(messageId:Long){val m=state.value.messages.firstOrNull{it.id==messageId}?:return;apply(messageId,m.suggestions.indices.toSet())}
 private fun apply(mid:Long,indices:Set<Int>){if(state.value.applying||c.account.account.value?.uid!=owner)return;val who=owner;val turn=generation;_state.update{it.copy(applying=true,applyError=null)}
  viewModelScope.launch{try{for(index in indices){check(c.account.account.value?.uid==who&&generation==turn){"Hesap değişti. Yeni hesabında yeniden öneri iste."};val m=state.value.messages.firstOrNull{it.id==mid}?:break;if(index in m.added)continue;val s=m.suggestions.getOrNull(index)?:continue
   if(s.before!=null){if(c.planRepo.byId(s.before.id)!=s.before)throw GeminiException("Bu plan öneriden sonra değişmiş. Güncel haliyle yeni bir öneri iste.");c.focus.pauseIf(s.before.id);c.planRepo.edit(s.before,s.plan,EditScope.THIS)}else c.planRepo.create(s.plan)
   _state.update{st->st.copy(messages=st.messages.map{if(it.id==mid)it.copy(added=it.added+index)else it})}
  };c.scheduler.rescheduleAll()
  }catch(e:Exception){_state.update{it.copy(applyError=e.message?:"Öneri kaydedilemedi. Kaydedilenler işaretli; kalanları yeniden deneyebilirsin.")}}finally{_state.update{it.copy(applying=false)}}}
 }
 fun canCloudMemory()=c.sync.canApproveMemoryForCurrentAccount()
 fun syncActive()=c.sync.enabledForCurrentAccount()
 fun remember(mid:Long,index:Int,text:String,cloud:Boolean){
  if(memoryBusy.value)return
  val message=state.value.messages.firstOrNull{it.id==mid}?:return
  val suggestion=message.memories.getOrNull(index)?:return
  if(index in message.remembered)return
  memoryBusy.value=true;memoryError.value=null
  viewModelScope.launch{try{
   val uid=c.account.account.value?.uid?:error("Önce Google hesabınla giriş yap.")
   check(uid==owner){"Hesap değişti. Yeni hesabında yeniden öneri iste."}
   check(!cloud||canCloudMemory()){"Eşitleme başka bir hesaba bağlı."}
   c.memory.save(uid,text,cloud,suggestion.before?.uid,suggestion.before)
   _state.update{st->st.copy(messages=st.messages.map{if(it.id==mid)it.copy(remembered=it.remembered+index)else it})}
  }catch(e:kotlinx.coroutines.CancellationException){throw e}catch(e:Exception){memoryError.value=e.message?:"Hafıza kaydedilemedi. Yeniden dene."}finally{memoryBusy.value=false}}
 }
 fun clear(){if(!state.value.loading&&!state.value.applying){draftWritable=true;pendingInput="";_state.value=AiState(restoring=false);persist()}}
}
