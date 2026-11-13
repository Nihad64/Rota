package com.nihad64.rotam.ui.ai

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nihad64.rotam.data.model.Plan
import com.nihad64.rotam.domain.usecase.*
import com.nihad64.rotam.ui.TrFormat
import com.nihad64.rotam.ui.components.*
import com.nihad64.rotam.ui.newplan.RotamField
import com.nihad64.rotam.ui.theme.*
import java.time.LocalDate

@Composable fun PlanSuggestionReview(initial:Plan,onDismiss:()->Unit,onSave:(Plan)->String?) {
 var plan by remember(initial.uid){mutableStateOf(initial)}
 var field by remember{mutableStateOf<String?>(null)};var error by remember{mutableStateOf<String?>(null)}
 AlertDialog(onDismissRequest=onDismiss,title={Text("Öneriyi düzenle")},text={
  Column(Modifier.heightIn(max=480.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
   Text("Henüz kaydedilmedi. Buradaki değişiklik yalnız önizlemeye uygulanır.",color=SubtleText,style=MaterialTheme.typography.bodySmall)
   RotamField(plan.title,{plan=plan.copy(title=it)},"Plan adı",Modifier.fillMaxWidth())
   DetailRow("Gün",TrFormat.full(LocalDate.parse(plan.date))){field="date"}
   DetailRow("Başlangıç",plan.startTime?:"Saat yok"){field="start"}
   DetailRow("Bitiş / son teslim",plan.endTime?:"Saat yok"){field="end"}
   if(plan.endTime!=null)DetailRow("Bitiş günü",TrFormat.full(LocalDate.parse(plan.endDate?:plan.date))){field="endDate"}
   val extra=org.json.JSONObject(plan.extraData)
   RotamField(extra.optString("note"),{plan=plan.copy(extraData=org.json.JSONObject(plan.extraData).put("note",it).toString())},"Not · isteğe bağlı",Modifier.fillMaxWidth(),singleLine=false)
   if(extra.optString("subtasks").isNotBlank())Text(extra.optString("subtasks"),color=SubtleText,style=MaterialTheme.typography.bodyMedium)
   Text(ReminderPolicy.label(plan),color=SubtleText,style=MaterialTheme.typography.bodySmall)
   error?.let{Text(it,color=Danger)}
  }
 },confirmButton={TextButton(onClick={error=onSave(plan);if(error==null)onDismiss()}){Text("Önizlemeye uygula")}},dismissButton={TextButton(onClick=onDismiss){Text("Vazgeç")}})
 if(field=="date")DateChooser(plan.date,{field=null}){day->plan=PlanTime.moved(plan,LocalDate.parse(day))}
 if(field=="endDate")DateChooser(plan.endDate?:plan.date,{field=null}){day->plan=plan.copy(endDate=day)}
 if(field=="start"||field=="end"){val target=field;TimeChooser(if(target=="start")plan.startTime else plan.endTime,if(target=="start")"Başlangıç"else"Bitiş / son teslim",{field=null}){time->plan=if(target=="start")plan.copy(startTime=time)else plan.copy(endTime=time,endDate=if(time==null)null else plan.endDate)}}
}
