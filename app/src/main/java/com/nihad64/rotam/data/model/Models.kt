package com.nihad64.rotam.data.model
import androidx.room.*
import java.util.UUID
@Entity(tableName="plans", indices=[Index("date"),Index(value=["uid"],unique=true),Index(value=["occurrenceKey"],unique=true)])
data class Plan(
 @PrimaryKey(autoGenerate=true) val id:Long=0,
 val title:String, val category:String="kisisel", val date:String,
 val startTime:String?=null, val endTime:String?=null,
 val isCompleted:Boolean=false, val isSkipped:Boolean=false, val repeatType:String="none",
 val extraData:String="{}", val createdAt:Long=System.currentTimeMillis(), val reminderMinutes:Int=15,
 val seriesId:Long=0, val uid:String=UUID.randomUUID().toString(), val endDate:String?=null,
 val reminderEnabled:Boolean=false,val reminderTime:String?=null,val remindAtEnd:Boolean=false,
 val repeatEvery:Int=1,val repeatDays:String="",val repeatUntil:String?=null,val monthLastDay:Boolean=false,
 val occurrenceKey:String?=null,val originalDate:String?=null,val isDeleted:Boolean=false,
 val focusSeconds:Long=0,val goalId:Long?=null)
val Plan.categoryEnum:Category get()=Category.fromKey(category)
@Entity(tableName="series")
data class PlanSeries(@PrimaryKey(autoGenerate=true) val id:Long=0,val template:String,val anchor:String,val untilDate:String?=null)
@Entity(tableName="goals")
data class Goal(@PrimaryKey(autoGenerate=true) val id:Long=0,val title:String,val period:String,val targetCount:Int,
 val currentCount:Int=0,val category:String="kisisel",val dueDate:String,val isCompleted:Boolean=false)
enum class Category(val key:String,val label:String){
 KITAP("kitap","Okuma"),IS("is","İş"),VIDEO("video","İçerik"),SPOR("spor","Spor"),IBADET("ibadet","İbadet"),
 RANDEVU("randevu","Randevu"),YEMEK("yemek","Yemek"),OGRENME("ogrenme","Öğrenme"),KISISEL("kisisel","Kişisel");
 companion object {fun fromKey(k:String?):Category=entries.firstOrNull{it.key==k || it.label.equals(k,true)} ?: when(k){
 "book","reading"->KITAP;"work","iş"->IS;"content"->VIDEO;"sport","sports","exercise"->SPOR;"prayer","worship"->IBADET;
 "appointment"->RANDEVU;"meal","food"->YEMEK;"learning","study"->OGRENME;else->KISISEL}}}
enum class RepeatType(val key:String,val label:String){NONE("none","Tek sefer"),DAILY("daily","Her gün"),WEEKDAYS("weekdays","Hafta içi"),WEEKLY("weekly","Haftalık"),CUSTOM("custom","Seçili günler"),INTERVAL("interval","Gün aralığı"),MONTHLY("monthly","Aylık");
 companion object{fun fromKey(k:String)=entries.firstOrNull{it.key==k}?:NONE}}
enum class EditScope(val label:String){THIS("Yalnız bu plan"),FUTURE("Bu ve sonrakiler"),ALL("Tüm seri")}
enum class GoalPeriod(val key:String,val label:String){DAILY("daily","Günlük"),WEEKLY("weekly","Haftalık"),MONTHLY("monthly","Aylık"),YEARLY("yearly","Yıllık");companion object{fun fromKey(k:String)=entries.firstOrNull{it.key==k}?:WEEKLY}}
data class CategoryCount(val category:String,val count:Int)
