package com.nihad64.rotam.ui.ai

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nihad64.rotam.data.model.categoryEnum
import com.nihad64.rotam.domain.usecase.PlanTime
import com.nihad64.rotam.ui.TrFormat
import com.nihad64.rotam.ui.components.*
import com.nihad64.rotam.ui.memory.MemoryDialog
import com.nihad64.rotam.ui.newplan.RotamField
import com.nihad64.rotam.ui.theme.*
import java.time.LocalDate
import java.util.Locale

@Composable fun AiScreen(vm:AiViewModel,onManual:()->Unit={},onBack:(()->Unit)?=null) {
 val s by vm.state.collectAsStateWithLifecycle();val settings by vm.settings.collectAsStateWithLifecycle()
 val selectedPlan by vm.editingSelection.collectAsStateWithLifecycle();val memories by vm.memories.collectAsStateWithLifecycle()
 val memoryBusy by vm.memoryBusy.collectAsStateWithLifecycle();val memoryError by vm.memoryError.collectAsStateWithLifecycle()
 val cloudStatus by vm.syncStatus.collectAsStateWithLifecycle();val draftError by vm.draftError.collectAsStateWithLifecycle()
 val context=LocalContext.current;val motion=rememberMotionEnabled();val focusManager=LocalFocusManager.current;val keyboard=LocalSoftwareKeyboardController.current
 var tts by remember{mutableStateOf<TextToSpeech?>(null)};var ttsReady by remember{mutableStateOf(false)}
 var speechError by remember{mutableStateOf<String?>(null)};var sharingInfo by remember{mutableStateOf(false)}
 var remembering by remember{mutableStateOf<Pair<Long,Int>?>(null)};var reviewing by remember{mutableStateOf<Pair<Long,Int>?>(null)}
 val feedback=rememberCompletionFeedback();val added=s.messages.sumOf{it.added.size+it.remembered.size}
 var previouslyAdded by remember(vm){mutableIntStateOf(added)}
 LaunchedEffect(added){if(added>previouslyAdded)feedback();previouslyAdded=added}
 val dictate=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
  if(result.resultCode==Activity.RESULT_OK)result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let{vm.setInput(listOf(s.input,it).filter{v->v.isNotBlank()}.joinToString(" "))}
 }
 DisposableEffect(context){var engine:TextToSpeech?=null;engine=TextToSpeech(context){status->if(status==TextToSpeech.SUCCESS){val result=engine?.setLanguage(Locale.forLanguageTag("tr-TR"));ttsReady=result!=TextToSpeech.LANG_MISSING_DATA&&result!=TextToSpeech.LANG_NOT_SUPPORTED}};tts=engine;onDispose{engine?.stop();engine?.shutdown()}}
 Column(Modifier.fillMaxSize().imePadding().padding(horizontal=24.dp)) {
  PageHeader(if(selectedPlan)"Planı düzenle"else"Plan ekle",onBack=onBack,action={TextButton(onClick=onManual){Text("Kendin oluştur")}})
  Row(verticalAlignment=Alignment.CenterVertically) {
   Icon(Icons.Outlined.Shield,null,Modifier.size(16.dp),tint=SubtleText)
   Text("Mesajların Gemini’ye gönderilir.",style=MaterialTheme.typography.bodySmall,color=SubtleText,modifier=Modifier.weight(1f).padding(start=6.dp))
   TextButton(onClick={sharingInfo=true}){Text("Paylaşım",style=MaterialTheme.typography.labelMedium)}
  }
  val list=rememberLazyListState()
  LaunchedEffect(s.messages.size,s.loading){if(s.messages.isNotEmpty()){if(motion)list.animateScrollToItem(s.messages.lastIndex)else list.scrollToItem(s.messages.lastIndex)}}
  LazyColumn(state=list,modifier=Modifier.weight(1f),contentPadding=PaddingValues(vertical=20.dp),verticalArrangement=Arrangement.spacedBy(22.dp)) {
   if(s.messages.isEmpty())item {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart=32.dp,topEnd=32.dp,bottomEnd=56.dp,bottomStart=32.dp)).background(Surface).padding(24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
     Diamond(15.dp);Text("Birlikte planlayalım",style=MaterialTheme.typography.headlineLarge)
     Text("Aklındakini yaz veya söyle. Saat belli değilse öyle kalsın; birlikte netleştiririz.",color=SubtleText,style=MaterialTheme.typography.bodyLarge)
     Text("Anlat  ·  İncele  ·  Onayla",color=Accent,style=MaterialTheme.typography.labelLarge)
    }
    Text("Bir yerden başlayalım",style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(top=28.dp,bottom=12.dp))
    vm.starters.forEach{q->OutlinedButton(onClick={vm.setInput(q)},modifier=Modifier.fillMaxWidth().padding(bottom=8.dp),contentPadding=PaddingValues(16.dp),shape=RoundedCornerShape(20.dp)){Text(q,modifier=Modifier.weight(1f));Icon(Icons.Outlined.NorthWest,null,Modifier.size(18.dp))}}
   }
   items(s.messages,key={it.id}) {m->Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
    if(m.isUser)Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) {
     Text(m.text,modifier=Modifier.widthIn(max=340.dp).clip(RoundedCornerShape(topStart=24.dp,topEnd=24.dp,bottomStart=24.dp,bottomEnd=6.dp)).background(SurfaceVariant).padding(18.dp),style=MaterialTheme.typography.bodyLarge)
    }else {
     Row(verticalAlignment=Alignment.CenterVertically){Diamond(9.dp);Text("ROTAM",style=MaterialTheme.typography.labelSmall,color=SubtleText,modifier=Modifier.padding(start=10.dp))}
     var visible by remember(m.id){mutableStateOf(!motion)};LaunchedEffect(m.id){visible=true}
     AnimatedVisibility(visible,enter=fadeIn(tween(if(motion)RotamMotion.Navigate else 0))+slideInVertically(tween(if(motion)RotamMotion.Replace else 0)){it/8}){Text(m.text,color=if(m.isError)Danger else Primary,style=MaterialTheme.typography.bodyLarge)}
     if(!m.isError)TextButton(onClick={if(ttsReady)tts?.speak(m.text,TextToSpeech.QUEUE_FLUSH,null,"reply-${m.id}")else speechError="Türkçe ses verisi bulunamadı. Telefonunun metin okuma ayarlarını kontrol et."}){Icon(Icons.Outlined.VolumeUp,null,Modifier.size(18.dp));Spacer(Modifier.width(6.dp));Text("Sesli oku")}
    }
    m.suggestions.forEachIndexed{i,item->RotamCard(Modifier.fillMaxWidth()) {
     Row(verticalAlignment=Alignment.CenterVertically) {
      Icon(item.plan.categoryEnum.icon(),null,tint=Primary,modifier=Modifier.size(28.dp));Spacer(Modifier.width(14.dp))
      Column(Modifier.weight(1f)){Text(item.plan.title,style=MaterialTheme.typography.titleLarge);Text("${TrFormat.full(LocalDate.parse(item.plan.date))} · ${PlanTime.label(item.plan)}",color=SubtleText,style=MaterialTheme.typography.bodyMedium,modifier=Modifier.padding(top=6.dp))}
      if(i !in m.added){var menu by remember(m.id,i){mutableStateOf(false)};Box{IconButton(onClick={menu=true},enabled=!s.applying){Icon(Icons.Outlined.MoreHoriz,"Taslak seçenekleri")};DropdownMenu(expanded=menu,onDismissRequest={menu=false}){DropdownMenuItem(text={Text("Taslağı çıkar")},onClick={menu=false;vm.discardSuggestion(m.id,i)})}}}

     }
     val details=org.json.JSONObject(item.plan.extraData);val note=details.optString("note");val tasks=details.optString("subtasks")
     if(note.isNotBlank())Text(note,color=SubtleText,style=MaterialTheme.typography.bodyMedium,modifier=Modifier.padding(top=12.dp))
     if(tasks.isNotBlank())Text(tasks.lines().joinToString("\n"){"• $it"},color=SubtleText,style=MaterialTheme.typography.bodyMedium,modifier=Modifier.padding(top=8.dp))
     if(item.before!=null)Text("Önce · ${item.before.title} · ${TrFormat.full(LocalDate.parse(item.before.date))} · ${PlanTime.label(item.before)}",style=MaterialTheme.typography.bodySmall,color=SubtleText,modifier=Modifier.padding(top=14.dp))
     if(item.plan.repeatType!="none")Text(com.nihad64.rotam.data.model.RepeatType.fromKey(item.plan.repeatType).label,color=SubtleText,modifier=Modifier.padding(top=10.dp))
     Row(Modifier.fillMaxWidth().padding(top=14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
      TextButton(onClick={reviewing=m.id to i},enabled=i !in m.added&&!s.applying){Icon(Icons.Outlined.Tune,null,Modifier.size(18.dp));Spacer(Modifier.width(6.dp));Text("Düzenle")}
      TextButton(onClick={vm.addSuggestion(m.id,i)},enabled=i !in m.added&&!s.applying){Icon(if(i in m.added)Icons.Outlined.CheckCircle else Icons.Outlined.Check,null,Modifier.size(18.dp));Spacer(Modifier.width(6.dp));Text(if(i in m.added)"Kaydedildi"else if(item.before==null)"Onayla ve ekle"else"Değişikliği onayla")}
     }
    }}
    m.memories.forEachIndexed{index,memory->RotamCard(Modifier.fillMaxWidth()) {
     Text(if(memory.before==null)"Bunu sonraki seferler için hatırlayayım mı?"else"Hatırlanan bilgiyi güncelleyelim mi?",style=MaterialTheme.typography.titleMedium)
     Spacer(Modifier.height(10.dp));Text(memory.text,style=MaterialTheme.typography.bodyLarge)
     TextButton(onClick={vm.memoryError.value=null;remembering=m.id to index},enabled=index !in m.remembered&&!memoryBusy){Text(if(index in m.remembered)"Hafızaya kaydedildi"else"İncele ve onayla")}
    }}
    if(m.suggestions.size>1)PrimaryButton("Kalan önerileri onayla",{vm.addAll(m.id)},enabled=!s.applying&&m.added.size<m.suggestions.size)
   }}
   if(s.loading)item{Row(verticalAlignment=Alignment.CenterVertically){CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp);Text("Düşünüyor…",modifier=Modifier.padding(start=12.dp),color=SubtleText)}}
  }
  if(s.restored)Text("Yarım kalan metnin geri geldi. Kendiliğinden gönderilmedi.",color=SubtleText,style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(bottom=8.dp))
  draftError?.let{Text(it,color=Danger,style=MaterialTheme.typography.bodySmall)}
  s.applyError?.let{Text(it,color=Danger,style=MaterialTheme.typography.bodySmall)}
  speechError?.let{Text(it,color=SubtleText,style=MaterialTheme.typography.bodySmall)}
  if(s.messages.lastOrNull()?.isError==true)TextButton(onClick={focusManager.clearFocus();keyboard?.hide();vm.retry()},enabled=!s.loading){Text("Yeniden dene")}
  if(s.input.length>=4900)Text("${s.input.length} / 5000",style=MaterialTheme.typography.bodySmall,color=SubtleText)
  Row(Modifier.padding(bottom=20.dp),verticalAlignment=Alignment.CenterVertically) {
   RotamField(s.input,vm::setInput,"Yaz veya söyle…",Modifier.weight(1f).heightIn(max=156.dp),singleLine=false,minLines=1)
   IconButton(onClick={try{dictate.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_LANGUAGE,"tr-TR").putExtra(RecognizerIntent.EXTRA_PROMPT,"Planını anlat"))}catch(_:android.content.ActivityNotFoundException){speechError="Bu cihazda sesli giriş bulunamadı. Yazarak devam edebilirsin."}},enabled=!s.loading&&!s.restoring){Icon(Icons.Outlined.Mic,"Planını sesle anlat")}
   FilledIconButton(onClick={focusManager.clearFocus();keyboard?.hide();vm.send()},enabled=!s.loading&&!s.restoring&&s.input.isNotBlank(),colors=IconButtonDefaults.filledIconButtonColors(containerColor=Accent,contentColor=Background)){Icon(Icons.Outlined.ArrowUpward,"Mesajı gönder")}
  }
 }
 if(sharingInfo)AlertDialog(onDismissRequest={sharingInfo=false},title={Text("AI ile paylaşılanlar")},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
  Text(if(selectedPlan)"Mesajların ve düzenlemek için seçtiğin planın başlık/tarih/saat bilgileri Google Gemini’ye gönderilir."else if(settings.sharePlans&&settings.aiContextVersion>=2)"Mesajların ve soruna göre seçilen en fazla 150 plan özeti Google Gemini’ye gönderilir."else if(settings.sharePlans)"Mesajların ve önümüzdeki 14 günün plan özetleri yanıt için Google Gemini'ye gönderilir."else"Yazdıkların yanıt için Google Gemini'ye gönderilir. Mevcut planların paylaşımı kapalı.",style=MaterialTheme.typography.bodyMedium,color=SubtleText)
  if(settings.useMemory&&memories.isNotEmpty())Text("${memories.size} onaylı hafıza bilgisi de yanıt için kullanılır.",color=SubtleText,style=MaterialTheme.typography.bodyMedium)
  Text("Planların onayından sonra kaydedilir. Yazdığın metin cihazında taslak olarak tutulur; kendiliğinden yeniden gönderilmez.",color=SubtleText,style=MaterialTheme.typography.bodyMedium)
 }},confirmButton={TextButton(onClick={sharingInfo=false}){Text("Anladım")}})
 reviewing?.let{(mid,index)->s.messages.firstOrNull{it.id==mid}?.suggestions?.getOrNull(index)?.let{suggestion->PlanSuggestionReview(suggestion.plan,{reviewing=null}){vm.reviseSuggestion(mid,index,it)}}}
 remembering?.let{(mid,index)->val message=s.messages.firstOrNull{it.id==mid};val suggestion=message?.memories?.getOrNull(index)
  if(suggestion!=null){LaunchedEffect(message.remembered){if(index in message.remembered)remembering=null};MemoryDialog(suggestion.text,previous=suggestion.before?.text,initialCloud=suggestion.before?.cloudApproved?:vm.syncActive(),canCloud=vm.canCloudMemory(),syncActive=vm.syncActive(),busy=memoryBusy,error=memoryError,onDismiss={remembering=null},onSave={value,cloud->vm.remember(mid,index,value,cloud)})}
 }
}
