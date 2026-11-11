package com.nihad64.rotam.domain.usecase

import com.nihad64.rotam.data.model.Plan
import com.nihad64.rotam.data.prefs.Settings
import org.json.JSONObject
import java.security.MessageDigest
import java.time.*

/** A notification time is never written back as a plan's start or end time. */
object ReminderPolicy {
 enum class Mode { AUTO, CUSTOM, OFF }
 data class Event(val key:String,val at:LocalDateTime,val followUp:Boolean=false)
 const val MaxPerDay=6
 fun mode(p:Plan):Mode=when(JSONObject(p.extraData).optString("reminderMode")){"auto"->Mode.AUTO;"off"->Mode.OFF;else->if(p.reminderEnabled)Mode.CUSTOM else Mode.OFF}
 fun withMode(p:Plan,mode:Mode)=p.copy(reminderEnabled=mode==Mode.CUSTOM,extraData=JSONObject(p.extraData).put("reminderMode",mode.name.lowercase()).toString())
 fun signature(p:Plan):String {
  val text=listOf(p.uid,p.date,p.startTime,p.endTime,p.endDate,mode(p),p.reminderMinutes,p.reminderTime,p.remindAtEnd).joinToString("|")
  return MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString(""){"%02x".format(it)}
 }
 fun started(p:Plan)=JSONObject(p.extraData).optString("startedSchedule")==signature(p)
 fun markStarted(p:Plan,value:Boolean=true):Plan {
  val e=JSONObject(p.extraData);if(value)e.put("startedSchedule",signature(p))else e.remove("startedSchedule")
  return p.copy(extraData=e.toString())
 }
 fun enabled(p:Plan)=!p.isDeleted&&!p.isCompleted&&!p.isSkipped&&!started(p)&&mode(p)!=Mode.OFF
 fun label(p:Plan)=when(mode(p)){Mode.AUTO->if(started(p))"Başlandı · takip durdu"else"Otomatik · planına göre";Mode.CUSTOM->PlanTime.reminder(p)?.let{"${it.toLocalDate()} · ${it.toLocalTime()}"}?:"Özel saat seç";Mode.OFF->"Kapalı"}
 fun quietAdjusted(time:LocalDateTime,s:Settings):LocalDateTime {
  if(!s.quietEnabled||s.quietStart==s.quietEnd)return time
  val m=time.hour*60+time.minute
  val quiet=if(s.quietStart<s.quietEnd)m>=s.quietStart&&m<s.quietEnd else m>=s.quietStart||m<s.quietEnd
  if(!quiet)return time
  val date=if(s.quietStart>s.quietEnd&&m>=s.quietStart)time.toLocalDate().plusDays(1)else time.toLocalDate()
  return date.atStartOfDay().plusMinutes(s.quietEnd.toLong())
 }
 fun events(p:Plan,s:Settings,otherPlans:List<Plan> = emptyList()):List<Event> {
  if(!s.remindersEnabled||!enabled(p))return emptyList()
  if(mode(p)==Mode.CUSTOM)return listOfNotNull(PlanTime.reminder(p)?.let{Event("custom",quietAdjusted(it,s))})
  val day=LocalDate.parse(p.date);val start=PlanTime.start(p);val end=PlanTime.end(p)
  val raw=mutableListOf<Event>()
  if(start!=null){
   val lead=when(p.category){"randevu"->30L;"is"->20L;"spor"->15L;else->10L}
   raw+=Event("before",start.minusMinutes(lead));raw+=Event("time",start)
   raw+=Event("after",(end?:start.plusMinutes(30)).plusMinutes(30),true)
  }else if(end!=null){raw+=Event("before",end.minusMinutes(30));raw+=Event("time",end);raw+=Event("after",end.plusHours(2),true)}
  else{raw+=Event("checkin",day.atTime(19,0),true)}
  val finish=(end?:start?:day.atTime(19,0))
  raw+=Event("last",finish.toLocalDate().plusDays(1).atTime(18,0),true)
  val adjusted=raw.map{event->
   var time=quietAdjusted(event.at,s)
   if(event.followUp){
    // A known work/appointment interval is factual context; no travel time is invented.
    repeat(otherPlans.size.coerceAtMost(100)){
     val busy=otherPlans.filter{it.uid!=p.uid&&!it.isDeleted&&!it.isSkipped}.mapNotNull{q->val a=PlanTime.start(q);val b=PlanTime.end(q);if(a!=null&&b!=null&&time>=a&&time<b)b else null}.maxOrNull()
     if(busy!=null)time=quietAdjusted(busy.plusMinutes(15),s)
    }
   }
   event.copy(at=time)
  }.filter{it.at<=finish.plusDays(2)}.sortedBy{it.at}
  // Quiet hours can collapse several stages onto one instant. Send just one then.
  return adjusted.distinctBy{it.at}.take(4)
 }
 fun body(p:Plan,event:String):String=when(event){
  "custom"->"${PlanTime.label(p)} · Seçtiğin hatırlatma zamanı."
  "before"->"${PlanTime.label(p)} · Hazırlanmak için küçük bir adım."
  "time"->if(PlanTime.kind(p)==TimeKind.DEADLINE)"Son teslim zamanı. Tamamlayabildin mi?"else"Zamanı geldi. Hazırsan başlayalım."
  "after","last","checkin"->if(PlanOutcome.status(p)==PlanOutcome.Status.PARTIAL)"Kaldığın yerden devam etmek ister misin?"else"Yapabildin mi? Yanıtını bekliyor; yapılmadı sayılmadı."
  else->"Biraz sonra demiştin. Şimdi uygun mu?"
 }
}
