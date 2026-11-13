package com.nihad64.rotam.ui.memory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nihad64.rotam.ui.newplan.RotamField
import com.nihad64.rotam.ui.theme.*

@Composable fun MemoryDialog(initial:String,previous:String?=null,initialCloud:Boolean=false,canCloud:Boolean=true,syncActive:Boolean=false,busy:Boolean=false,error:String?=null,onDismiss:()->Unit,onSave:(String,Boolean)->Unit) {
 var text by remember(initial){mutableStateOf(initial)};var cloud by remember(initialCloud){mutableStateOf(initialCloud&&canCloud)}
 AlertDialog(onDismissRequest={if(!busy)onDismiss()},title={Text(if(previous!=null)"Hatırlanan bilgiyi güncelle"else"Bunu hatırlayayım mı?")},text={
  Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(14.dp)) {
   Text("Yalnız senin onayladığın bilgi hatırlanır. Hafıza açıkken AI yanıtlarında kullanılmak için Google Gemini'ye gönderilir.",style=MaterialTheme.typography.bodyMedium)
   previous?.takeIf{it!=initial}?.let{Text("Önce: $it",color=SubtleText,style=MaterialTheme.typography.bodyMedium)}
   RotamField(text,{text=it},"Hatırlanacak bilgi",Modifier.fillMaxWidth(),singleLine=false,isError=text.length>500)
   Text("${text.length} / 500",color=if(text.length>500)Danger else SubtleText,style=MaterialTheme.typography.bodySmall)
   Row(Modifier.fillMaxWidth().toggleable(value=cloud,enabled=canCloud&&!busy,role=Role.Checkbox,onValueChange={cloud=it}),verticalAlignment=Alignment.CenterVertically){Checkbox(cloud,null,enabled=canCloud&&!busy);Text("Diğer cihazlarım için de eşitle",modifier=Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium)}
   Text(if(!canCloud)"Bu cihazın eşitlemesi başka bir hesaba bağlı. Bu bilgi yalnız bu cihazda saklanır."else if(cloud&&syncActive)"Ayrıca mevcut Google hesabına bağlı bulut kopyasına eklenir."else if(cloud)"Eşitleme şu an kapalı. Verilerim'den açtığında bu bilgi de hesabına aktarılır."else"Bu bilgi cihazında saklanır; diğer cihazlarına aktarılmaz. AI yanıtında kullanımı ayrı tercihtir.",color=SubtleText,style=MaterialTheme.typography.bodySmall)
   error?.let{Text(it,color=Danger)}
  }
 },confirmButton={TextButton(onClick={onSave(text,cloud)},enabled=!busy&&text.trim().length in 1..500){Text(if(busy)"Kaydediliyor…"else"Onayla ve hatırla")}},dismissButton={TextButton(onClick=onDismiss,enabled=!busy){Text("Vazgeç")}})
}
