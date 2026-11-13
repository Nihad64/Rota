package com.nihad64.rotam.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName="memories",indices=[Index("ownerUid")])
data class UserMemory(
 @PrimaryKey val uid:String=UUID.randomUUID().toString(),val ownerUid:String,val text:String,
 val createdAt:Long=System.currentTimeMillis(),val updatedAt:Long=createdAt,
 val isDeleted:Boolean=false,val cloudApproved:Boolean=false
)
