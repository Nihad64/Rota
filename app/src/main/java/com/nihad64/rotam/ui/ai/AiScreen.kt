package com.nihad64.rotam.ui.ai
import android.speech.tts.TextToSpeech
import android.speech.RecognizerIntent
import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nihad64.rotam.domain.usecase.PlanTime
import com.nihad64.rotam.ui.components.*
import com.nihad64.rotam.ui.newplan.RotamField
import com.nihad64.rotam.ui.theme.*
import java.util.Locale
@Composable fun AiScreen(vm:AiViewModel,onManual:()->Unit={},onBack:(()->Unit)?=null){val s by vm.state.collectAsStateWithLifecycle();val settings by vm.settings.collectAsStateWithLifecycle();val context=LocalContext.current;var tts by remember{mutableStateOf<TextToSpeech?>(null)};var ttsReady by remember{mutableStateOf(false)};var speechError by remember{mutableStateOf<String?>(null)}
 val completionFeedback=rememberCompletionFeedback()
 val added=s.messages.sumOf{it.added.size}
 var previouslyAdded by remember(vm){mutableIntStateOf(added)}
 LaunchedEffect(added){if(added>previouslyAdded)completionFeedback();previouslyAdded=added}
 val dictate=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
  if(result.resultCode==Activity.RESULT_OK)result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let{vm.setInput(listOf(s.input,it).filter{v->v.isNotBlank()}.joinToString(" "))}
 }
 DisposableEffect(context){var engine:TextToSpeech?=null;engine=TextToSpeech(context){status->if(status==TextToSpeech.SUCCESS){val result=engine?.setLanguage(Locale.forLanguageTag("tr-TR"));ttsReady=result!=TextToSpeech.LANG_MISSING_DATA&&result!=TextToSpeech.LANG_NOT_SUPPORTED}};tts=engine;onDispose{engine?.stop();engine?.shutdown()}}
 Column(Modifier.fillMaxSize().imePadding().padding(horizontal=24.dp)){
  PageHeader("Birlikte planlayalım",onBack=onBack,action={TextButton(onClick=onManual){Text("Elle ekle")}})
  Text(if(settings.sharePlans)"Mesajların ve önümüzdeki 14 günün plan özetleri yanıt için Google Gemini'ye gönderilir."else"Yazdıkların yanıt için Google Gemini'ye gönderilir. Mevcut planların paylaşımı kapalı.",style=MaterialTheme.typography.bodySmall,color=SubtleText)
  val list=rememberLazyListState();LaunchedEffect(s.messages.size,s.loading){if(s.messages.isNotEmpty())list.animateScrollToItem(s.messages.lastIndex)}
  LazyColumn(state=list,modifier=Modifier.weight(1f),contentPadding=PaddingValues(vertical=20.dp),verticalArrangement=Arrangement.spacedBy(22.dp)){
   if(s.messages.isEmpty())item{EmptyState("Birlikte planlayalım.","Saat belli değilse söylemen yeterli. Her değişiklik önce burada gösterilir.",null,{});vm.starters.forEach{q->TextButton(onClick={vm.send(q)}){Text(q)}}}
   items(s.messages,key={it.id}){m->Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
    Text(if(m.isUser)"SEN"else"ROTAM",style=MaterialTheme.typography.labelSmall,color=SubtleText)
    Text(m.text,color=if(m.isError)Danger else Primary,style=MaterialTheme.typography.bodyLarge)
    if(!m.isUser&&!m.isError)TextButton(onClick={if(ttsReady)tts?.speak(m.text,TextToSpeech.QUEUE_FLUSH,null,"reply-${m.id}")else speechError="Türkçe ses verisi bulunamadı. Telefonunun metin okuma ayarlarını kontrol et."}){Icon(Icons.Outlined.VolumeUp,null,Modifier.size(18.dp));Spacer(Modifier.width(6.dp));Text("Sesli oku")}
    m.suggestions.forEachIndexed{i,item->RotamCard(Modifier.fillMaxWidth()){
     Text(if(item.before==null)"YENİ PLAN"else"DEĞİŞİKLİK ÖNERİSİ",style=MaterialTheme.typography.labelSmall,color=SubtleText)
     if(item.before!=null){Text("Önce: ${item.before.title} · ${item.before.date} · ${PlanTime.label(item.before)}",style=MaterialTheme.typography.bodySmall,color=SubtleText,modifier=Modifier.padding(top=10.dp))}
     Text(item.plan.title,style=MaterialTheme.typography.titleLarge,modifier=Modifier.padding(top=10.dp));Text("${item.plan.date} · ${PlanTime.label(item.plan)}",color=SubtleText,modifier=Modifier.padding(top=8.dp))
     if(item.plan.repeatType!="none")Text(com.nihad64.rotam.data.model.RepeatType.fromKey(item.plan.repeatType).label,color=SubtleText)
     TextButton(onClick={vm.addSuggestion(m.id,i)},enabled=i !in m.added&&!s.applying){Text(if(i in m.added)"Kaydedildi"else if(item.before==null)"Onayla ve ekle"else"Değişikliği onayla")}
    }}
    if(m.suggestions.size>1)PrimaryButton("Kalan önerileri onayla",{vm.addAll(m.id)},enabled=!s.applying&&m.added.size<m.suggestions.size)
   }}
   if(s.loading)item{Row(verticalAlignment=Alignment.CenterVertically){CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp);Text("Düşünüyor…",modifier=Modifier.padding(start=12.dp),color=SubtleText)}}
  }
  if(s.applyError!=null)Text(s.applyError!!,color=Danger,style=MaterialTheme.typography.bodySmall)
  if(speechError!=null)Text(speechError!!,color=SubtleText,style=MaterialTheme.typography.bodySmall)
  if(s.messages.lastOrNull()?.isError==true)TextButton(onClick={vm.retry()},enabled=!s.loading){Text("Yeniden dene")}
  Row(Modifier.padding(bottom=20.dp),verticalAlignment=Alignment.CenterVertically){RotamField(s.input,vm::setInput,"Birlikte ne planlayalım?",Modifier.weight(1f),singleLine=false);IconButton(onClick={try{dictate.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_LANGUAGE,"tr-TR").putExtra(RecognizerIntent.EXTRA_PROMPT,"Planını anlat"))}catch(_:android.content.ActivityNotFoundException){speechError="Bu cihazda sesli giriş bulunamadı. Yazarak devam edebilirsin."}},enabled=!s.loading){Icon(Icons.Outlined.Mic,"Planını sesle anlat")};IconButton(onClick={vm.send()},enabled=!s.loading&&s.input.isNotBlank()){Icon(Icons.Outlined.ArrowUpward,"Mesajı gönder")}}
 }
}
