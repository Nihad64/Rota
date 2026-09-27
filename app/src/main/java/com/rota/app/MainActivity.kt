package com.rota.app
import android.Manifest
import android.app.*
import android.content.*
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

class MainActivity:ComponentActivity(){
 private val permission=registerForActivityResult(ActivityResultContracts.RequestPermission()){}
 override fun onCreate(b:Bundle?){super.onCreate(b);if(android.os.Build.VERSION.SDK_INT>=33)permission.launch(Manifest.permission.POST_NOTIFICATIONS);setContent{RotaApp()}}
 @Composable fun RotaApp(){
  var tab by remember{mutableIntStateOf(0)};var plans by remember{mutableStateOf(PlanStore.load(this))};var add by remember{mutableStateOf(false)}
  MaterialTheme(colorScheme=lightColorScheme(primary=Color(0xFF17181B),surface=Color(0xFFF7F7F9))){
   Scaffold(containerColor=Color(0xFFF7F7F9),bottomBar={NavigationBar(containerColor=Color.White){
    val names=listOf("Bugün","Plan","Asistan");val icons=listOf(Icons.Outlined.Home,Icons.Outlined.CalendarMonth,Icons.Outlined.AutoAwesome)
    names.forEachIndexed{i,n->NavigationBarItem(selected=tab==i,onClick={tab=i},icon={Icon(icons[i],n)},label={Text(n)})}
   }},floatingActionButton={FloatingActionButton(onClick={add=true},containerColor=Color(0xFF17181B),contentColor=Color.White){Icon(Icons.Outlined.Add,"Ekle")}}){pad->
    when(tab){0->Today(plans,{p->plans=plans.map{if(it.id==p.id)it.copy(done=!it.done)else it};PlanStore.save(this,plans)},Modifier.padding(pad));1->PlanPage(plans,Modifier.padding(pad));else->Assistant(Modifier.padding(pad))}
   }
   if(add)AddDialog({p->plans=plans+p;PlanStore.save(this,plans);add=false;schedule(p)},{add=false})
  }
 }
 private fun schedule(p:Plan){if(p.type!="Saatli")return;val q=p.time.split(":");if(q.size!=2)return;val c=Calendar.getInstance();c.set(Calendar.HOUR_OF_DAY,q[0].toInt());c.set(Calendar.MINUTE,q[1].toInt());c.set(Calendar.SECOND,0);if(c.timeInMillis<=System.currentTimeMillis())return;val pi=PendingIntent.getBroadcast(this,p.id.toInt(),Intent(this,ReminderReceiver::class.java).putExtra("title",p.title),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE);getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,c.timeInMillis,pi)}
}

@Composable fun Header(t:String,s:String){Column(Modifier.padding(24.dp)){Text("ROTA",fontSize=13.sp,fontWeight=FontWeight.Bold,color=Color.Gray,letterSpacing=2.sp);Text(t,fontSize=40.sp,fontWeight=FontWeight.Bold);Text(s,color=Color.Gray)}}
@Composable fun Today(plans:List<Plan>,toggle:(Plan)->Unit,m:Modifier){Column(m.fillMaxSize()){Header("Bugün","Pazar · 27 Eylül");val next=plans.firstOrNull{!it.done};if(next!=null)Card(Modifier.padding(horizontal=20.dp).fillMaxWidth(),shape=RoundedCornerShape(24.dp)){Column(Modifier.padding(20.dp)){Text("SIRADAKİ",fontSize=12.sp,fontWeight=FontWeight.Bold,color=Color.Gray);Text(next.title,fontSize=24.sp,fontWeight=FontWeight.Bold);Text(if(next.type=="Saatli")next.time+" · "+next.duration else next.type,color=Color.Gray)}};LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){items(plans){p->PlanRow(p,toggle)};if(plans.isEmpty())item{Text("Bugün için henüz plan yok.",color=Color.Gray)}}}}
@Composable fun PlanRow(p:Plan,toggle:(Plan)->Unit){Card(Modifier.fillMaxWidth().clickable{toggle(p)},shape=RoundedCornerShape(18.dp)){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(p.title,fontWeight=FontWeight.SemiBold,fontSize=17.sp);Text(if(p.type=="Saatli")p.time+" · "+p.duration else p.type,color=Color.Gray,fontSize=14.sp)};Checkbox(checked=p.done,onCheckedChange={toggle(p)})}}}
@Composable fun PlanPage(plans:List<Plan>,m:Modifier){Column(m.fillMaxSize()){Header("Plan","Yaklaşan planların");LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){items(plans){p->PlanRow(p,{})}}}}
@Composable fun Assistant(m:Modifier){Column(m.fillMaxSize()){Header("Asistan","Sadece planların için");Card(Modifier.padding(20.dp).fillMaxWidth(),shape=RoundedCornerShape(24.dp)){Column(Modifier.padding(20.dp)){Text("Bugününü birlikte düzenleyelim.",fontSize=22.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(8.dp));Text("Yarın 14–22 çalışıyorum. Sabah spor ve Risale okumak istiyorum.",color=Color.Gray);Spacer(Modifier.height(18.dp));Button(onClick={}){Text("Yarını planla")}}}}}
@Composable fun AddDialog(onAdd:(Plan)->Unit,onDismiss:()->Unit){var title by remember{mutableStateOf("")};var type by remember{mutableStateOf("Saatli")};var time by remember{mutableStateOf("09:00")};var duration by remember{mutableStateOf("60 dk")};AlertDialog(onDismissRequest=onDismiss,title={Text("Yeni plan")},text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){OutlinedTextField(title,{title=it},label={Text("Ne yapacaksın?")},singleLine=true);Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("Saatli","Tüm gün","Esnek").forEach{FilterChip(selected=type==it,onClick={type=it},label={Text(it)})}};if(type=="Saatli"){OutlinedTextField(time,{time=it},label={Text("Saat")},singleLine=true);OutlinedTextField(duration,{duration=it},label={Text("Süre")},singleLine=true)}}},confirmButton={TextButton(onClick={if(title.isNotBlank())onAdd(Plan(System.currentTimeMillis(),title,type,time,duration))}){Text("Ekle")}},dismissButton={TextButton(onClick=onDismiss){Text("Vazgeç")}})}
