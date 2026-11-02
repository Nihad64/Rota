package com.nihad64.rotam.domain.usecase
import com.nihad64.rotam.data.model.*
import java.time.*
import java.time.temporal.ChronoUnit

enum class TimeKind {UNTIMED,START,DEADLINE,INTERVAL}
object PlanTime {
 fun kind(p:Plan)=when{p.startTime!=null&&p.endTime!=null->TimeKind.INTERVAL;p.startTime!=null->TimeKind.START;p.endTime!=null->TimeKind.DEADLINE;else->TimeKind.UNTIMED}
 fun start(p:Plan):LocalDateTime?=p.startTime?.let{LocalDate.parse(p.date).atTime(LocalTime.parse(it))}
 fun end(p:Plan):LocalDateTime?=p.endTime?.let{LocalDate.parse(p.endDate?:p.date).atTime(LocalTime.parse(it))}
 fun durationMinutes(p:Plan):Long?=if(start(p)!=null&&end(p)!=null) ChronoUnit.MINUTES.between(start(p),end(p)) else null
 fun overdue(p:Plan,now:LocalDateTime=LocalDateTime.now())=!p.isCompleted&&!p.isSkipped&&!p.isDeleted&&end(p)?.isBefore(now)==true
 fun reminder(p:Plan):LocalDateTime?=when{!p.reminderEnabled->null;p.reminderTime!=null->LocalDate.parse(p.date).atTime(LocalTime.parse(p.reminderTime));(p.remindAtEnd&&p.endTime!=null)||p.startTime==null->end(p)?.minusMinutes(p.reminderMinutes.toLong());else->start(p)?.minusMinutes(p.reminderMinutes.toLong())}
 fun label(p:Plan):String {val next=if(p.endDate!=null&&p.endDate!=p.date) " · ${p.endDate}" else "";return when(kind(p)){
 TimeKind.UNTIMED->"Saat yok";TimeKind.START->"Başlangıç ${p.startTime}";TimeKind.DEADLINE->"${p.endTime} · Son teslim$next";TimeKind.INTERVAL->"${p.startTime} – ${p.endTime}$next"}}
 fun overlaps(a:Plan,b:Plan):Boolean {
  if(a.id!=0L&&a.id==b.id || b.isDeleted || b.isCompleted || b.isSkipped)return false
  val aStart=start(a)?:return false;val aEnd=end(a)?:return false
  val bStart=start(b)?:return false;val bEnd=end(b)?:return false
  return aStart<bEnd && bStart<aEnd
 }
 fun moved(p:Plan,date:LocalDate):Plan {val d=ChronoUnit.DAYS.between(LocalDate.parse(p.date),date);return p.copy(date=date.toString(),endDate=p.endDate?.let{LocalDate.parse(it).plusDays(d).toString()})}
 fun validate(p:Plan):String? {
 if(p.title.isBlank())return "Planına bir ad ver.";if(p.title.length>300)return "Plan adı en fazla 300 karakter olabilir."
 return try {val date=LocalDate.parse(p.date);p.startTime?.let{LocalTime.parse(it)};p.endTime?.let{LocalTime.parse(it)};p.reminderTime?.let{LocalTime.parse(it)}
 if(p.endDate!=null&&p.endTime==null)return "Bitiş tarihi için bitiş saati seç."
 if(p.endDate!=null&&LocalDate.parse(p.endDate)<date)return "Bitiş tarihi başlangıçtan önce olamaz."
 if(start(p)!=null&&end(p)!=null&&!end(p)!!.isAfter(start(p)))return "Bitiş başlangıçtan sonra olmalı. Geceyi aşıyorsa bitiş tarihini seç."
 if(p.reminderEnabled&&reminder(p)==null)return "Saatsiz plan için ayrı bir hatırlatma saati seç."
 if(p.reminderMinutes !in 0..10080)return "Hatırlatma süresi geçerli değil."
 if(p.repeatEvery !in 1..365)return "Tekrar aralığı 1–365 arasında olmalı."
 if(p.repeatType=="custom"&&p.repeatDays.split(',').none{it.toIntOrNull() in 1..7})return "En az bir tekrar günü seç."
 if(p.repeatUntil!=null&&LocalDate.parse(p.repeatUntil)<date)return "Tekrar bitişi ilk günden önce olamaz."
 if(RepeatType.entries.none{it.key==p.repeatType})return "Tekrar türü geçerli değil."
 null }catch(_:Exception){"Tarih veya saat biçimini kontrol et."}}
}
object Recurrence {
 fun occurs(p:Plan,anchor:LocalDate,day:LocalDate):Boolean {
 if(day<anchor||p.repeatUntil?.let{day>LocalDate.parse(it)}==true)return false
 val days=ChronoUnit.DAYS.between(anchor,day)
 return when(p.repeatType){"daily"->true;"weekdays"->day.dayOfWeek.value<=5;"weekly"->day.dayOfWeek==anchor.dayOfWeek&&days/7%p.repeatEvery==0L
 "custom"->day.dayOfWeek.value in p.repeatDays.split(',').mapNotNull{it.toIntOrNull()};"interval"->days%p.repeatEvery==0L
 "monthly"->ChronoUnit.MONTHS.between(anchor.withDayOfMonth(1),day.withDayOfMonth(1))%p.repeatEvery==0L&&day.dayOfMonth==(if(p.monthLastDay)minOf(anchor.dayOfMonth,day.lengthOfMonth())else anchor.dayOfMonth)
 else->day==anchor}}
 fun dates(p:Plan,anchor:LocalDate,from:LocalDate,to:LocalDate):List<LocalDate>{val result=mutableListOf<LocalDate>();var d=maxOf(from,anchor);while(d<=to){if(occurs(p,anchor,d))result.add(d);d=d.plusDays(1)};return result}
}
