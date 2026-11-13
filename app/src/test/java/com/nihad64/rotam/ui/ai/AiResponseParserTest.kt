package com.nihad64.rotam.ui.ai
import com.nihad64.rotam.data.model.Plan
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject
import org.json.JSONArray
class AiResponseParserTest {
 private fun raw(plans:String):String=JSONObject().put("candidates",JSONArray().put(JSONObject().put("content",JSONObject().put("parts",JSONArray().put(JSONObject().put("text","""{"reply":"Önce kontrol et","plans":$plans}""")))))).toString()
 @Test fun twoDraftsKeepIndependentTimesAndDoNotRequireCategory(){
  val r=AiResponseParser.parse(raw("""[{"title":"İş","date":"2026-11-10","startTime":"14:00","endTime":"22:00"},{"title":"Süt ve yumurta al","date":"2026-11-10"}]"""))
  assertEquals(2,r.suggestions.size);assertEquals("14:00",r.suggestions[0].plan.startTime)
  assertNull(r.suggestions[1].plan.startTime);assertNull(r.suggestions[1].plan.endTime)
  assertNotEquals(r.suggestions[0].plan.uid,r.suggestions[1].plan.uid)
 }
 @Test fun startAndDeadlineRemainIndependent(){
  val r=AiResponseParser.parse(raw("""[{"title":"Başlangıç","date":"2026-11-10","startTime":"10:00"},{"title":"Teslim","date":"2026-11-10","endTime":"18:00"}]"""))
  assertNull(r.suggestions[0].plan.endTime);assertNull(r.suggestions[1].plan.startTime)
 }
 @Test fun rejectsUnknownPlanIdInvalidDateAndMalformedResponse(){
  for(p in listOf("""[{"planId":999,"title":"Sil","date":"2026-11-10"}]""","""[{"title":"Oku","date":"invalid"}]""")){
   assertThrows(GeminiException::class.java){AiResponseParser.parse(raw(p))}
  }
  assertThrows(GeminiException::class.java){AiResponseParser.parse("not json")}
 }
 @Test fun editKeepsExistingIdentityNotesAndFocus(){
  val p=Plan(id=10,title="Oku",date="2026-11-09",extraData="""{"note":"koru"}""",focusSeconds=60)
  val r=AiResponseParser.parse(raw("""[{"planId":10,"title":"Yeni başlık","date":"2026-11-10","startTime":null,"endTime":null,"endDate":null}]"""),listOf(p)).suggestions.single()
  assertEquals(p,r.before);assertEquals(p.uid,r.plan.uid);assertEquals(p.extraData,r.plan.extraData);assertEquals(60L,r.plan.focusSeconds)
 }
 @Test fun existingPlanRequiresExplicitTimeFieldsAndExactNumericIdentity(){
  val p=Plan(id=10,title="İş",date="2026-11-09",startTime="14:00",endTime="22:00")
  for(row in listOf("""[{"planId":10,"title":"İş","date":"2026-11-10"}]""","""[{"planId":10.5,"title":"İş","date":"2026-11-10","startTime":null,"endTime":null,"endDate":null}]""")){
   assertThrows(GeminiException::class.java){AiResponseParser.parse(raw(row),listOf(p))}
  }
  val r=AiResponseParser.parse(raw("""[{"planId":10,"title":"İş","date":"2026-11-10","startTime":"14:00","endTime":"22:00","endDate":null}]"""),listOf(p)).suggestions.single()
  assertEquals("14:00",r.plan.startTime);assertEquals("22:00",r.plan.endTime)
 }
 @Test fun modelCannotClaimThatUnapprovedDraftsWereSaved(){
  val r=AiResponseParser.parseText("""{"reply":"Planınız eklendi.","plans":[{"title":"Oku","date":"2026-11-12"}]}""")
  assertTrue(r.text.contains("Henüz kaydedilmedi"));assertFalse(r.text.contains("eklendi"))
 }
 @Test fun shoppingItemsAndRelativeConstraintsSurviveWithoutInventingTime(){
  val r=AiResponseParser.parseText("""{"plans":[{"title":"Alışveriş","date":"2026-11-13","note":"İşten önce","subtasks":["Süt al","Yumurta al"]}]}""").suggestions.single().plan
  val e=JSONObject(r.extraData);assertEquals("İşten önce",e.getString("note"));assertEquals("Süt al\nYumurta al",e.getString("subtasks"));assertNull(r.startTime);assertNull(r.endTime)
 }
}
