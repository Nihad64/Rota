package com.nihad64.rotam.ui.components
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.*
import androidx.compose.ui.text.font.FontWeight
import com.nihad64.rotam.ui.theme.*
import java.time.*
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun DateChooser(value:String,onDismiss:()->Unit,onSelect:(String)->Unit){val date=runCatching{LocalDate.parse(value)}.getOrDefault(LocalDate.now());val state=rememberDatePickerState(initialSelectedDateMillis=date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli());DatePickerDialog(onDismissRequest=onDismiss,confirmButton={TextButton(onClick={state.selectedDateMillis?.let{onSelect(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString())};onDismiss()}){Text("Seç")}},dismissButton={TextButton(onClick=onDismiss){Text("Vazgeç")}}){DatePicker(state)}}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun TimeChooser(value:String?,label:String,onDismiss:()->Unit,onSelect:(String?)->Unit){
 val initial=runCatching{LocalTime.parse(value)}.getOrDefault(LocalTime.NOON)
 var hour by remember{mutableIntStateOf(initial.hour)};var minute by remember{mutableIntStateOf(initial.minute)};var typing by remember{mutableStateOf(false)}
 val input=rememberTimePickerState(initial.hour,initial.minute,true)
 ModalBottomSheet(onDismissRequest=onDismiss,containerColor=Surface,contentColor=Primary){Column(Modifier.fillMaxWidth().padding(horizontal=24.dp).padding(bottom=28.dp),horizontalAlignment=Alignment.CenterHorizontally){
 Text(label,style=MaterialTheme.typography.headlineSmall);Text("24 saat biçimi",color=SubtleText,modifier=Modifier.padding(top=6.dp,bottom=20.dp))
 if(typing)TimeInput(input)else Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center){TimeWheel(24,hour,{hour=it});Text(":",style=MaterialTheme.typography.headlineMedium,modifier=Modifier.padding(horizontal=16.dp));TimeWheel(60,minute,{minute=it})}
 TextButton(onClick={typing=!typing}){Text(if(typing)"Kaydırarak seç"else"Klavyeyle gir")}
 PrimaryButton("Saati seç",{val t=if(typing)LocalTime.of(input.hour,input.minute)else LocalTime.of(hour,minute);onSelect(t.toString());onDismiss()},Modifier.fillMaxWidth())
 TextButton(onClick={onSelect(null);onDismiss()}){Text("Saati kaldır")}
 }}
}
@Composable private fun TimeWheel(count:Int,value:Int,selected:(Int)->Unit){val list=rememberLazyListState(value);LaunchedEffect(list){snapshotFlow{list.isScrollInProgress to list.firstVisibleItemIndex}.collect{(scroll,i)->if(!scroll)selected(i.coerceIn(0,count-1))}}
 Box(Modifier.width(90.dp).height(192.dp),contentAlignment=Alignment.Center){Box(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(14.dp)).background(SurfaceVariant));LazyColumn(state=list,flingBehavior=rememberSnapFlingBehavior(list),contentPadding=PaddingValues(vertical=72.dp),horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.fillMaxSize()){items(count){i->Box(Modifier.fillMaxWidth().height(48.dp),contentAlignment=Alignment.Center){Text(i.toString().padStart(2,'0'),color=if(i==value)Primary else SubtleText,style=MaterialTheme.typography.headlineMedium,fontWeight=if(i==value)FontWeight.Bold else FontWeight.Normal)}}}}
}
