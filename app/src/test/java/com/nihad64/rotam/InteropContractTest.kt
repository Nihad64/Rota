package com.nihad64.rotam

import com.nihad64.rotam.data.backup.BackupData
import com.nihad64.rotam.data.model.*
import com.nihad64.rotam.data.sync.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=android.app.Application::class)
class InteropContractTest {
 @Test fun androidAndAppleUseTheSamePortableRecordAndGzipContract(){
  val goal=Goal(id=17,uid="contract-goal",title="3 kitap",period="weekly",targetCount=3,dueDate="2026-11-18")
  val extra=JSONObject().put("note","İş / öğrenme\nİkinci satır: ığüşçö").put("unknownFutureField","Korunmalı").toString()
  val template=Plan(uid="contract-template",title="İş / öğrenme: ığüşçö",date="2026-11-12",category="ogrenme",endTime="18:00",repeatType="weekly",createdAt=1234,extraData=extra,goalId=17)
  val series=PlanSeries(id=13,uid="contract-series",template=PlanJson.encode(template).toString(),anchor=template.date)
  val plan=template.copy(id=41,uid=RecordIdentity.occurrence(series.uid,template.date),seriesId=13,originalDate=template.date,occurrenceKey="13:${template.date}",focusSeconds=62)
  val memory=UserMemory(uid="contract-memory",ownerUid="contract-owner",text="Sentetik ulaşım tercihi",createdAt=1000,updatedAt=2000,cloudApproved=true)
  val expected=CloudRecords.encode(BackupData(listOf(plan),listOf(goal),listOf(series),listOf(memory)))
  if(System.getenv("ROTAM_WRITE_CONTRACT")=="1"){
   val file=java.io.File("src/test/resources/core-wire.bin");file.parentFile?.mkdirs();file.writeBytes(SyncEnvelopeCodec.encode(expected))
  }
  val bytes=javaClass.getResourceAsStream("/core-wire.bin")?.use{it.readBytes()}?:java.io.File("src/test/resources/core-wire.bin").readBytes()
  assertEquals(expected,SyncEnvelopeCodec.decode(bytes));assertEquals(4,expected.size)
  val apple=javaClass.getResourceAsStream("/apple-wire.bin")?.use{it.readBytes()}?:java.io.File("src/test/resources/apple-wire.bin").readBytes()
  val changed=CloudRecords.encode(BackupData(listOf(plan.copy(title="Swift'ten gelen plan",focusSeconds=75)),listOf(goal),listOf(series),listOf(memory)))
  val imported=SyncEnvelopeCodec.decode(apple)
  assertEquals(changed,imported.filterKeys{it in changed.keys})
  assertEquals(5,imported.size)
  assertEquals("Swift yeni saatsiz plan",JSONObject(imported.getValue(RecordKey(RecordKind.PLAN,"contract-native-new")).body).getString("title"))
  assertEquals(imported,CloudRecords.encode(CloudRecords.decode(imported)))
 }
}
