package com.nihad64.rotam.ui.ai
import com.nihad64.rotam.data.model.UserMemory
import org.junit.Assert.*
import org.junit.Test
class MemoryProposalTest {
 @Test fun memoryIsOnlyAValidatedProposalAndExistingIdentityMustBeKnown(){
  val old=UserMemory(uid="known",ownerUid="owner",text="Ulaşım 30 dakika")
  val reply=AiResponseParser.parseText("""{"reply":"Onayını bekliyorum","plans":[],"remember":[{"text":"Ulaşım 20 dakika","replaceUid":"known"}]}""",memories=listOf(old))
  assertEquals(1,reply.memories.size);assertEquals(old,reply.memories.single().before);assertTrue(reply.suggestions.isEmpty())
  val rejected=AiResponseParser.parseText("""{"reply":"Öneri","remember":[{"text":"Başka kayıt","replaceUid":"unknown"}]}""",memories=listOf(old))
  assertTrue(rejected.memories.isEmpty());assertTrue(rejected.text.contains("doğrulanamadı"))
 }
 @Test fun oversizedAndNonTextMemoryCannotBecomePersistentFacts(){
  val json=org.json.JSONObject().put("reply","Öneri").put("remember",org.json.JSONArray().put(org.json.JSONObject().put("text","a".repeat(501))).put(org.json.JSONObject().put("text",123)))
  val result=AiResponseParser.parseText(json.toString());assertTrue(result.memories.isEmpty());assertTrue(result.text.contains("2 hafıza önerisi"))
 }
}
