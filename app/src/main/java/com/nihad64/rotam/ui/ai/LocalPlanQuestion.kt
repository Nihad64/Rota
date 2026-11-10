package com.nihad64.rotam.ui.ai

import com.nihad64.rotam.data.model.*
import com.nihad64.rotam.domain.usecase.PlanOutcome
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** Answer an unambiguous count from local records; no model or private-data transfer. */
object LocalPlanQuestion {
 fun answer(question:String,plans:List<Plan>,today:LocalDate=LocalDate.now()):String? {
  val q=question.lowercase(Locale.forLanguageTag("tr"))
  if(!Regex("\\bkaç\\b").containsMatchIn(q)||!Regex("kere|kez|defa|tane|plan").containsMatchIn(q))return null
  if(!Regex("yaptım|tamamladım|bitirdim|gittim").containsMatchIn(q))return null
  val period=when {
   "geçen ay" in q->Triple(today.minusMonths(1).withDayOfMonth(1),today.withDayOfMonth(1).minusDays(1),"Geçen ay")
   "bu ay" in q->Triple(today.withDayOfMonth(1),today,"Bu ay")
   "geçen hafta" in q->{val monday=today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));Triple(monday.minusWeeks(1),monday.minusDays(1),"Geçen hafta")}
   "bu hafta" in q->Triple(today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),today,"Bu hafta")
   else->return null
  }
  val candidates=listOf(Category.SPOR to listOf("spor","egzersiz"),Category.KITAP to listOf("okuma","kitap"),Category.IS to listOf("iş","çalışma"))
   .filter{(_,words)->words.any{Regex("\\b${Regex.escape(it)}\\b").containsMatchIn(q)}}
  if(candidates.size!=1)return null
  // Never interpret an explicit incomplete/skipped query as a completion count.
  if(listOf("yapmad","atlad","ertele","yarım","kısmen","planlad").any{it in q})return null
  val category=candidates.single().first
  val relevant=plans.distinctBy{it.uid}.filter{!it.isDeleted&&it.categoryEnum==category&&it.date>=period.first.toString()&&it.date<=period.second.toString()}
  val completed=relevant.count{it.isCompleted}
  val partial=relevant.count{PlanOutcome.status(it)==PlanOutcome.Status.PARTIAL}
  val unknown=relevant.count{PlanOutcome.status(it)==PlanOutcome.Status.UNKNOWN}
  return "${period.third} kayıtlarda ${completed} tamamlanmış ${category.label.lowercase(Locale.forLanguageTag("tr"))} planın var."+
   (if(partial>0)" Ayrıca $partial plan kısmen yapılmış; tam tamamlanma sayısına eklemedim."else"")+
   (if(unknown>0)" $unknown planın sonucu henüz bildirilmemiş; yapılmadı saymadım."else"")
 }
}
