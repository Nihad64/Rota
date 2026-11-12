package com.nihad64.rotam.ui.components
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.nihad64.rotam.data.model.*
import com.nihad64.rotam.domain.usecase.PlanTime
import com.nihad64.rotam.domain.usecase.PlanOutcome
import com.nihad64.rotam.ui.theme.*
fun Category.icon():ImageVector=when(this){Category.KITAP->Icons.AutoMirrored.Outlined.MenuBook;Category.IS->Icons.Outlined.WorkOutline;Category.VIDEO->Icons.Outlined.Movie;Category.SPOR->Icons.Outlined.FitnessCenter;Category.IBADET->Icons.Outlined.NightsStay;Category.RANDEVU->Icons.Outlined.Place;Category.YEMEK->Icons.Outlined.Restaurant;Category.OGRENME->Icons.Outlined.School;Category.KISISEL->Icons.Outlined.PersonOutline}
@Composable fun CategoryIcon(category:Category,size:Dp=40.dp,modifier:Modifier=Modifier){Box(modifier.size(size),contentAlignment=Alignment.Center){Icon(category.icon(),category.label,tint=SubtleText,modifier=Modifier.size(size*.55f))}}
@Composable fun RotamCard(modifier:Modifier=Modifier,onClick:(()->Unit)?=null,highlight:Boolean=false,content:@Composable ColumnScope.()->Unit){Column(modifier.clip(RoundedCornerShape(28.dp)).background(Surface).then(if(onClick!=null)Modifier.clickable(onClick=onClick)else Modifier).padding(22.dp),content=content)}
@Composable fun ProgressRing(progress:Float,modifier:Modifier=Modifier,size:Dp=110.dp,stroke:Dp=10.dp,center:@Composable ()->Unit={}){Box(modifier.size(size),contentAlignment=Alignment.Center){Canvas(Modifier.fillMaxSize()){drawArc(Outline,-90f,360f,false,style=Stroke(stroke.toPx(),cap=StrokeCap.Round));drawArc(Primary,-90f,360f*progress.coerceIn(0f,1f),false,style=Stroke(stroke.toPx(),cap=StrokeCap.Round))};center()}}
@Composable fun LinearBar(progress:Float,modifier:Modifier=Modifier){LinearProgressIndicator(progress={progress.coerceIn(0f,1f)},modifier=modifier.fillMaxWidth().height(4.dp).clip(CircleShape),color=Primary,trackColor=Outline)}
@Composable fun PlanCard(plan:Plan,onToggle:()->Unit,onLongPress:()->Unit={},modifier:Modifier=Modifier){
 Row(modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick=onLongPress).padding(vertical=14.dp),verticalAlignment=Alignment.CenterVertically){
  Column(Modifier.width(65.dp)){Text(plan.startTime?:plan.endTime?:"Gün içi",style=MaterialTheme.typography.titleMedium,color=if(plan.isCompleted)SubtleText else Primary);if(plan.startTime==null&&plan.endTime!=null)Text("son teslim",style=MaterialTheme.typography.labelSmall,color=SubtleText)}
  Box(Modifier.width(2.dp).height(42.dp).background(Outline));Spacer(Modifier.width(16.dp))
  Column(Modifier.weight(1f)){Text(plan.title,style=MaterialTheme.typography.titleMedium,color=if(plan.isCompleted||plan.isSkipped)SubtleText else Primary,maxLines=2,overflow=TextOverflow.Ellipsis,textDecoration=if(plan.isCompleted)TextDecoration.LineThrough else TextDecoration.None)
  Text(when{plan.isCompleted->"Tamamlandı";plan.isSkipped->"Yapılmadı";PlanOutcome.status(plan)==PlanOutcome.Status.PARTIAL->"Kısmen yapıldı · %${((PlanOutcome.fraction(plan)?:0.0)*100).toInt()}";PlanOutcome.status(plan)==PlanOutcome.Status.POSTPONED->"Ertelendi";PlanTime.overdue(plan)->"Son teslim geçti";else->if(plan.startTime!=null&&plan.endTime!=null)PlanTime.label(plan)else plan.categoryEnum.label},style=MaterialTheme.typography.bodySmall,color=SubtleText)}
  IconButton(onClick=onToggle){Icon(if(plan.isCompleted)Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,if(plan.isCompleted)"Tamamlamayı geri al"else"Planı tamamla",tint=if(plan.isCompleted)Primary else SubtleText,modifier=Modifier.size(23.dp))}
 }
}
@Composable fun SegmentedToggle(options:List<String>,selected:Int,onSelect:(Int)->Unit,modifier:Modifier=Modifier){Row(modifier.fillMaxWidth().clip(CircleShape).background(Surface).padding(4.dp)){options.forEachIndexed{i,t->Box(Modifier.weight(1f).clip(CircleShape).background(if(i==selected)Primary else Color.Transparent).clickable{onSelect(i)}.padding(vertical=12.dp),contentAlignment=Alignment.Center){Text(t,color=if(i==selected)Background else SubtleText,style=MaterialTheme.typography.labelMedium)}}}}
@Composable fun SectionTitle(text:String,modifier:Modifier=Modifier,trailing:(@Composable ()->Unit)?=null){Row(modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(text,style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f));trailing?.invoke()}}
@Composable fun EmptyState(title:String,subtitle:String,action:String?,onAction:()->Unit){Column(Modifier.fillMaxWidth().padding(vertical=36.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Diamond(14.dp);Text(title,style=MaterialTheme.typography.headlineSmall);Text(subtitle,color=SubtleText,style=MaterialTheme.typography.bodyMedium);if(action!=null)TextButton(onClick=onAction){Text(action)}}}
@Composable fun PrimaryButton(text:String,onClick:()->Unit,modifier:Modifier=Modifier,icon:ImageVector?=null,enabled:Boolean=true){val feedback=rememberFeedback();Button({feedback();onClick()},modifier=modifier.heightIn(min=54.dp),enabled=enabled,colors=ButtonDefaults.buttonColors(containerColor=Accent,contentColor=Background),shape=CircleShape,contentPadding=PaddingValues(horizontal=24.dp,vertical=16.dp)){if(icon!=null){Icon(icon,null,Modifier.size(20.dp));Spacer(Modifier.width(8.dp))};Text(text)}}
@Composable fun SelectChip(text:String,selected:Boolean,onClick:()->Unit,modifier:Modifier=Modifier){val feedback=rememberFeedback();FilterChip(selected,{feedback();onClick()},label={Text(text)},modifier=modifier.heightIn(min=48.dp),shape=CircleShape,colors=FilterChipDefaults.filterChipColors(selectedContainerColor=Primary,selectedLabelColor=Background,containerColor=Surface,labelColor=SubtleText),border=null)}
@Composable fun ConfettiBurst(trigger:Int,modifier:Modifier=Modifier){}
@Composable fun Diamond(size:Dp=10.dp){Box(Modifier.size(size).rotate(45f).clip(RoundedCornerShape(2.dp)).background(Accent))}
@Composable fun PageHeader(title:String,subtitle:String?=null,onBack:(()->Unit)?=null,action:(@Composable ()->Unit)?=null){
 Column(Modifier.fillMaxWidth().padding(bottom=20.dp)){Row(verticalAlignment=Alignment.CenterVertically){if(onBack!=null){IconButton(onClick=onBack){Icon(Icons.AutoMirrored.Outlined.ArrowBack,"Geri")};Spacer(Modifier.width(4.dp))};Text(title,style=MaterialTheme.typography.headlineMedium,modifier=Modifier.weight(1f));action?.invoke()};if(subtitle!=null)Text(subtitle,color=SubtleText,style=MaterialTheme.typography.bodyMedium,modifier=Modifier.padding(top=8.dp))}}
@Composable fun DetailRow(label:String,value:String,onClick:()->Unit){Row(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(vertical=16.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(label,style=MaterialTheme.typography.labelMedium,color=SubtleText);Text(value,style=MaterialTheme.typography.bodyLarge,modifier=Modifier.padding(top=4.dp))};Icon(Icons.Outlined.ChevronRight,null,tint=SubtleText)}}

@Composable fun SegmentedProgress(done:Int,total:Int,modifier:Modifier=Modifier){
 val enabled=rememberMotionEnabled()
 val target=if(total<=0)0f else done.toFloat()/total
 val value by androidx.compose.animation.core.animateFloatAsState(target,androidx.compose.animation.core.tween(if(enabled)RotamMotion.Navigate else 0),label="daily-progress")
 val segments=total.coerceIn(1,7)
 Canvas(modifier.fillMaxWidth().height(6.dp)) {
  val gap=5.dp.toPx();val width=(size.width-gap*(segments-1))/segments
  repeat(segments){i->
   val left=i*(width+gap);val fraction=(value*segments-i).coerceIn(0f,1f)
   drawRoundRect(Outline,androidx.compose.ui.geometry.Offset(left,0f),androidx.compose.ui.geometry.Size(width,size.height),androidx.compose.ui.geometry.CornerRadius(size.height/2))
   if(fraction>0f)drawRoundRect(Accent,androidx.compose.ui.geometry.Offset(left,0f),androidx.compose.ui.geometry.Size(width*fraction,size.height),androidx.compose.ui.geometry.CornerRadius(size.height/2))
  }
 }
}
