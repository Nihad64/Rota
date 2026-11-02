package com.nihad64.rotam.ui.calendar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nihad64.rotam.data.model.Plan
import com.nihad64.rotam.domain.usecase.PlanTime
import com.nihad64.rotam.ui.theme.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class TimelineBlock(val plan:Plan,val from:Int,val to:Int,val lane:Int)
object TimelineLayout {
    fun blocks(plans:List<Plan>,day:LocalDate):List<TimelineBlock> {
        val start=day.atStartOfDay();val end=day.plusDays(1).atStartOfDay()
        val intervals=plans.filter{!it.isDeleted}.mapNotNull { p->
            val a=PlanTime.start(p)?:return@mapNotNull null;val b=PlanTime.end(p)?:return@mapNotNull null
            if(a>=end || b<=start)return@mapNotNull null
            Triple(p,ChronoUnit.MINUTES.between(start,maxOf(start,a)).toInt(),ChronoUnit.MINUTES.between(start,minOf(end,b)).toInt())
        }.sortedWith(compareBy({it.second},{it.third},{it.first.id}))
        val laneEnds=mutableListOf<Int>()
        return intervals.map { (p,a,b)->var lane=laneEnds.indexOfFirst{it<=a};if(lane<0){lane=laneEnds.size;laneEnds.add(b)}else laneEnds[lane]=b;TimelineBlock(p,a,b,lane) }
    }
}

@Composable fun DayTimeline(plans:List<Plan>,day:LocalDate,onEdit:(Long)->Unit) {
    val blocks=TimelineLayout.blocks(plans,day)
    if(blocks.isEmpty())return
    val firstHour=(blocks.minOf{it.from}/60).coerceAtLeast(0)
    val lastHour=((blocks.maxOf{it.to}+59)/60).coerceIn(firstHour+1,24)
    val minutes=(lastHour-firstHour)*60
    val scale=1.2f
    val lanes=blocks.maxOf{it.lane}+1
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Zaman aralıkları",style=MaterialTheme.typography.titleMedium)
        Text("Yalnız başlangıcı ve bitişi belli planlar burada yer kaplar.",color=SubtleText,style=MaterialTheme.typography.bodySmall)
        BoxWithConstraints(Modifier.fillMaxWidth().height((minutes*scale+24).dp)) {
            val labelWidth=48.dp
            val laneWidth=(maxWidth-labelWidth)/lanes
            Canvas(Modifier.fillMaxSize()) {
                for(hour in firstHour..lastHour){val y=((hour-firstHour)*60*scale).dp.toPx();drawLine(Outline,Offset(labelWidth.toPx(),y),Offset(size.width,y),1.dp.toPx())}
            }
            for(hour in firstHour..lastHour)Text("%02d:00".format(hour),modifier=Modifier.offset(y=((hour-firstHour)*60*scale).dp),style=MaterialTheme.typography.labelSmall,color=SubtleText)
            blocks.forEach { b->
                val height=((b.to-b.from)*scale).dp
                Column(Modifier.offset(x=labelWidth+laneWidth*b.lane,y=((b.from-firstHour*60)*scale).dp)
                    .width(laneWidth-4.dp).height(height).clip(RoundedCornerShape(12.dp)).background(if(b.plan.isCompleted)Surface else SurfaceVariant)
                    .semantics{contentDescription="${b.plan.title}, ${PlanTime.label(b.plan)}"}.clickable{onEdit(b.plan.id)}.padding(horizontal=10.dp,vertical=6.dp)) {
                    if(height>=30.dp)Text(b.plan.title,style=MaterialTheme.typography.labelLarge,color=if(b.plan.isCompleted)SubtleText else Primary,maxLines=if(height>=80.dp)2 else 1,overflow=TextOverflow.Ellipsis)
                    if(height>=64.dp)Text("${b.plan.startTime} – ${b.plan.endTime}",style=MaterialTheme.typography.labelSmall,color=SubtleText,maxLines=1,overflow=TextOverflow.Ellipsis)
                }
            }
        }
    }
}
