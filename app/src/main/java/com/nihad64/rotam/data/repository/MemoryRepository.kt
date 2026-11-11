package com.nihad64.rotam.data.repository

import androidx.room.withTransaction
import com.nihad64.rotam.data.db.AppDatabase
import com.nihad64.rotam.data.model.UserMemory

class MemoryRepository(private val db:AppDatabase) {
 fun observeAll()=db.memoryDao().observeAll()
 suspend fun forAccount(uid:String)=db.memoryDao().all().filter{it.ownerUid==uid&&!it.isDeleted}.sortedByDescending{it.updatedAt}.take(50)
 suspend fun save(ownerUid:String,text:String,cloudApproved:Boolean,uid:String?=null,expected:UserMemory?=null):UserMemory=db.withTransaction {
  val value=text.trim();require(ownerUid.isNotBlank()&&ownerUid.length<=128);require(value.length in 1..500){"Hafıza bilgisi 1–500 karakter olmalı."}
  val all=db.memoryDao().all();val existing=if(uid!=null)all.firstOrNull{it.uid==uid}else all.firstOrNull{it.ownerUid==ownerUid&&!it.isDeleted&&it.text.equals(value,true)}
  if(expected!=null)check(existing==expected){"Hafıza başka bir işlemde değişmiş. Güncel kaydı kontrol et."}
  if(uid!=null)check(existing!=null&&!existing.isDeleted){"Bu hafıza kaydı değişmiş veya silinmiş. Listeyi kontrol et."}
  check(existing==null||existing.ownerUid==ownerUid){"Bu hafıza başka bir hesaba ait."}
  if(existing==null)require(all.count{it.ownerUid==ownerUid&&!it.isDeleted}<50){"En fazla 50 bilgi hatırlanabilir. Önce bir kaydı düzenle veya sil."}
  val result=existing?.copy(text=value,updatedAt=System.currentTimeMillis(),cloudApproved=cloudApproved)
   ?:UserMemory(ownerUid=ownerUid,text=value,cloudApproved=cloudApproved)
  db.memoryDao().save(result);result
 }
 suspend fun remove(ownerUid:String,uid:String)=db.withTransaction {
  val old=db.memoryDao().byUid(uid)?:return@withTransaction
  check(old.ownerUid==ownerUid){"Bu hafıza başka bir hesaba ait."}
  // Keep a content-free tombstone so another offline device cannot revive the forgotten text.
  db.memoryDao().save(old.copy(text="",isDeleted=true,updatedAt=System.currentTimeMillis()))
 }
}
