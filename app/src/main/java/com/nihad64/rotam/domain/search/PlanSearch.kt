package com.nihad64.rotam.domain.search

import com.nihad64.rotam.data.model.*
import com.nihad64.rotam.domain.usecase.*
import org.json.JSONObject
import java.text.Normalizer
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

data class SearchPeriod(val from:LocalDate,val to:LocalDate,val label:String)
data class PlanSearchResult(val query:String,val plans:List<Plan>,val explanation:String?=null,val answer:String?=null,val needsAssistant:Boolean=false)
object PlanSearch {
 fun normalize(text:String):String=Normalizer.normalize(text.lowercase(Locale.forLanguageTag("tr")).replace('ı','i'),Normalizer.Form.NFD).replace(Regex("\\p{M}+"),"")
 fun needsAssistant(query:String):Boolean {
  val q=normalize(query)
  return Regex("\\b(tasi|tasiyabilir\\w*|ertele|erteleyebilir\\w*|duzenle|duzenler\\w*|degistir\\w*|olustur|planla|oner|nasil)\\b").containsMatchIn(q)||"cakisma" in q
 }
 fun period(query:String,today:LocalDate=LocalDate.now()):SearchPeriod? {
  val q=normalize(query);val monday=today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
  fun span(a:LocalDate,b:LocalDate,label:String)=SearchPeriod(a,b,label)
  when {
   "gecen hafta" in q->return span(monday.minusWeeks(1),monday.minusDays(1),"Geçen hafta")
   "gelecek hafta" in q||"onumuzdeki hafta" in q->return span(monday.plusWeeks(1),monday.plusWeeks(2).minusDays(1),"Gelecek hafta")
   "bu hafta" in q->return span(monday,monday.plusDays(6),"Bu hafta")
   "gecen ay" in q->return span(today.minusMonths(1).withDayOfMonth(1),today.withDayOfMonth(1).minusDays(1),"Geçen ay")
   "gelecek ay" in q->return span(today.plusMonths(1).withDayOfMonth(1),today.plusMonths(2).withDayOfMonth(1).minusDays(1),"Gelecek ay")
   "bu ay" in q->return span(today.withDayOfMonth(1),today.withDayOfMonth(1).plusMonths(1).minusDays(1),"Bu ay")
   Regex("\\bbugun\\b").containsMatchIn(q)->return span(today,today,"Bugün")
   Regex("\\byarin\\b").containsMatchIn(q)->return span(today.plusDays(1),today.plusDays(1),"Yarın")
   Regex("\\bdun\\b").containsMatchIn(q)->return span(today.minusDays(1),today.minusDays(1),"Dün")
  }
  Regex("\\b\\d{4}-\\d{2}-\\d{2}\\b").find(q)?.value?.let{raw->runCatching{LocalDate.parse(raw)}.getOrNull()?.let{return span(it,it,raw)}}
  Regex("\\b\\d{1,2}\\.\\d{1,2}\\.\\d{4}\\b").find(q)?.value?.let{raw->runCatching{LocalDate.parse(raw,DateTimeFormatter.ofPattern("d.M.uuuu"))}.getOrNull()?.let{return span(it,it,raw)}}
  val months=listOf("ocak","subat","mart","nisan","mayis","haziran","temmuz","agustos","eylul","ekim","kasim","aralik")
  for((i,name)in months.withIndex())Regex("\\b$name(?:\\s+(\\d{4}))?\\b").find(q)?.let{m->
   val year=m.groupValues[1].toIntOrNull()?:today.year
   if(year in 1900..2200){val date=LocalDate.of(year,i+1,1);return span(date,date.plusMonths(1).minusDays(1),m.value)}
  }
  val dayNames=listOf("pazartesi","sali","carsamba","persembe","cuma","cumartesi","pazar")
  for((i,name)in dayNames.withIndex())if(Regex("\\b$name\\b").containsMatchIn(q)){
   val date=today.with(TemporalAdjusters.nextOrSame(DayOfWeek.of(i+1)));return span(date,date,name)
  }
  Regex("^\\s*(19\\d{2}|20\\d{2}|21\\d{2}|2200)\\s*$").matchEntire(q)?.let{val d=LocalDate.of(it.groupValues[1].toInt(),1,1);return span(d,d.plusYears(1).minusDays(1),q.trim())}
  return null
 }
 fun localQuestion(query:String):Boolean {
  if(needsAssistant(query))return false
  val q=normalize(query)
  return Regex("\\bkac\\b").containsMatchIn(q)||("hangi gun" in q&&period(query)!=null)||("planlarim" in q&&period(query)!=null)
 }
 fun search(query:String,all:List<Plan>,now:LocalDateTime=LocalDateTime.now()):PlanSearchResult {
  val q=normalize(query).trim();val span=period(query,now.toLocalDate());val assistant=needsAssistant(query)
  val words=Regex("[a-z0-9]+").findAll(q).map{it.value}.toList()
  val count=Regex("\\bkac\\b").containsMatchIn(q)
  val completed=Regex("tamamlan|yaptiklar|bitirdik|yaptim|tamamladim|bitirdim|gittim").containsMatchIn(q)&&!Regex("yapmad|yapamad|tamamlanma[yd]").containsMatchIn(q)
  val partial=Regex("kismi|kismen|yarim").containsMatchIn(q)
  val postponed=Regex("ertelen|erteled").containsMatchIn(q)
  val notDone=Regex("yapmadik|yapamadik|yapmadim|yapamadim|yapilmayan|atlanan").containsMatchIn(q)
  val awaiting=Regex("yanitsiz|cevapsiz|yanit bekle").containsMatchIn(q)
  val category=when {
   words.any{it in setOf("spor","egzersiz")}->Category.SPOR
   words.any{it in setOf("okuma","kitap")}->Category.KITAP
   words.any{it=="is"||it.startsWith("calis")}->Category.IS
   words.any{it in setOf("ibadet","namaz")}->Category.IBADET
   words.any{it in setOf("yemek","ogun")}->Category.YEMEK
   else->null
  }
  val filler=setOf("bu","gecen","gelecek","onumuzdeki","hafta","ay","bugun","yarin","dun","icin","kac","kere","kez","defa","tane","plan","planlar","planlarim","planim","rutin","rutinim","rutinler","rutinlerim","var","hangi","gun","gunler","gunlerde","yaptim","tamamladim","bitirdim","gittim","yaptiklarim","tamamlanan","bitirdiklerim","kismen","kismi","yarim","yapilan","yanitsiz","cevapsiz","ertelenen","ertelediklerim","yapmadiklarim","yapamadiklarim","yapmadim","yapamadim","yapilmayan","atlanan","spor","egzersiz","okuma","kitap","is","ibadet","namaz","yemek","ogun","mi","mu")
  val dateWords=span?.let{Regex("[a-z0-9]+").findAll(normalize(it.label)).map{m->m.value}.toSet()}?:emptySet()
  val terms=words.filter{it !in filler&&it !in dateWords&&!it.startsWith("calis")&&!(span!=null&&it.all(Char::isDigit))}
  fun historyDates(p:Plan):List<String>{val history=JSONObject(p.extraData).optJSONArray("outcomeHistory")?:return emptyList();return (0 until history.length()).mapNotNull{i->history.optJSONObject(i)?.takeIf{it.optString("status")==PlanOutcome.Status.POSTPONED.name}?.optString("date")}}
  fun text(p:Plan):String {val e=JSONObject(p.extraData);return normalize(p.title+" "+p.categoryEnum.label+" "+e.optString("note")+" "+e.optString("subtasks"))}
  val relevant=all.distinctBy{it.uid}.filterNot{it.isDeleted}.filter{p->
   val dates=if(postponed)historyDates(p)+p.date else listOf(p.date)
   val dateMatches=span==null||dates.any{it>=span.from.toString()&&it<=span.to.toString()}||(p.date<=span.to.toString()&&(p.endDate?:p.date)>=span.from.toString())
   val body=text(p)
   dateMatches&&(category==null||p.categoryEnum==category||(!count&&normalize(category.label) in body))&&terms.all{it in body}
  }
  val results=relevant.filter{p->
   (!completed||(p.isCompleted&&(!count||p.date<=now.toLocalDate().toString())))&&
   (!partial||PlanOutcome.status(p)==PlanOutcome.Status.PARTIAL)&&
   (!postponed||(PlanOutcome.status(p)==PlanOutcome.Status.POSTPONED||historyDates(p).isNotEmpty()))&&
   (!notDone||PlanOutcome.status(p)==PlanOutcome.Status.NOT_DONE)&&
   (!awaiting||(PlanReviewPolicy.awaiting(p,now)))
  }.sortedWith(compareBy<Plan>{it.date}.thenBy{it.startTime?:it.endTime?:"99:99"}.thenBy{it.title})
  val description=span?.let{"${it.from} – ${it.to}"}?.plus(if(postponed)" · Ertelemeden önceki plan tarihi de aranır."else" · Plan tarihlerine göre.")
  val answer=if(localQuestion(query)){
   if(count&&terms.isEmpty()){
    val label=if(completed)"tamamlanmış plan"else if(notDone)"yapılmadı bildirilen plan"else if(partial)"kısmen yapılan plan"else"kayıtlı plan"
    val routineCount=results.filter{it.seriesId!=0L}.map{it.seriesId}.distinct().size
    val countText=if(!completed&&!notDone&&!partial&&!postponed&&!awaiting&&routineCount>0&&!Regex("\\b(kere|kez|defa)\\b").containsMatchIn(q))"Bu cihazdaki kayıtlara göre ${results.count{it.seriesId==0L}} tek seferlik plan ve $routineCount rutin var. Rutinlerin bu kapsamda ${results.count{it.seriesId!=0L}} tekrarı bulunuyor."else "Bu cihazdaki kayıtlara göre ${results.size} $label var."
    countText+
     (if(completed){val unknown=relevant.count{it.date<=now.toLocalDate().toString()&&PlanOutcome.status(it)==PlanOutcome.Status.UNKNOWN};val part=relevant.count{PlanOutcome.status(it)==PlanOutcome.Status.PARTIAL};" $part kısmi ve $unknown sonucu bildirilmemiş kayıt tam tamamlanma sayısına eklenmedi."}else"")
   }else if(count)null else if(results.isEmpty())"Bu kapsamda kayıtlı plan yok."else results.take(20).joinToString("\n"){"${it.date} · ${it.title} · ${PlanTime.label(it)}"}+(if(results.size>20)"\nİlk 20 plan gösterildi; kalan kayıtlar aramada bulunur."else"")
  }else null
  return PlanSearchResult(query,results,description,answer,assistant)
 }
}
