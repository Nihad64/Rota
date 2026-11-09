package com.nihad64.rotam.ui.ai
import androidx.lifecycle.*
import com.nihad64.rotam.AppContainer
import com.nihad64.rotam.data.model.EditScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ChatMessage(val id:Long,val isUser:Boolean,val text:String,val suggestions:List<PlanSuggestion> = emptyList(),val added:Set<Int> = emptySet(),val isError:Boolean=false)
data class AiState(val messages:List<ChatMessage> = emptyList(),val loading:Boolean=false,val applying:Boolean=false,val input:String="",val applyError:String?=null)
class AiViewModel(private val c:AppContainer):ViewModel(){
 private val _state=MutableStateFlow(AiState());val state=_state.asStateFlow();private var id=1L
 val settings=c.settings.settings.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),com.nihad64.rotam.data.prefs.Settings())
 val starters=listOf("Yarın için saatsiz bir okuma planı öner","Bu hafta için çalışma planı hazırlayalım")
 private var planningDate:LocalDate?=null
 fun setPlanningDate(date:LocalDate?){planningDate=date}
 fun setInput(v:String){_state.update{it.copy(input=v.take(5000))}}
 fun send(text:String=state.value.input){if(text.isBlank()||state.value.loading)return
  _state.update{it.copy(messages=it.messages+ChatMessage(id++,true,text.trim()),input="",loading=true,applyError=null)}
  viewModelScope.launch{try{val cfg=c.settings.current();val ctx=if(cfg.sharePlans)c.planRepo.range(LocalDate.now().minusDays(1),LocalDate.now().plusDays(14)).take(150)else emptyList()
   val history=state.value.messages.filter{!it.isErro…2510 tokens truncated…
  is PromptBlockedException,is ContentBlockedException,is ResponseStoppedException->"AI bu istek için kullanılabilir bir yanıt üretemedi. Hiçbir plan kaydedilmedi; elle ekleyebilirsin."
  is SerializationException->"AI yanıtının biçimi doğrulanamadı. Hiçbir plan kaydedilmedi."
  is UnsupportedUserLocationException->"AI hizmeti bulunduğun bölgede erişime izin vermedi. Elle plan ekleyebilirsin."
  is ServerException->serverMessage(error.message.orEmpty())
  else->"AI hizmetine ulaşılamadı. Yazdıkların korunuyor; yeniden dene veya elle ekle."
 }
 // The SDK currently exposes server failures as text rather than a structured HTTP code.
 internal fun serverMessage(message:String):String {
  val m=message.lowercase(java.util.Locale.ROOT)
  return when {
   "app check" in m||"attestation" in m->"Uygulamanın güvenli AI erişimi doğrulanamadı. Planların değişmedi; elle ekleyebilirsin."
   "unauthenticated" in m||"authentication" in m->"Google oturumunu yeniden açıp tekrar dene. Planların korunuyor."
   "not found" in m||"not supported" in m||"no access" in m->"Gemini 3.5 Flash-Lite bu proje için kullanılamıyor. Başka modele geçilmedi; elle plan ekleyebilirsin."
   "permission" in m||"forbidden" in m->"AI hizmeti erişime izin vermedi. Planların değişmedi; elle ekleyebilirsin."
   else->"AI hizmeti isteği tamamlayamadı. Yazdıkların korunuyor; yeniden dene veya elle ekle."
  }
 }
}
