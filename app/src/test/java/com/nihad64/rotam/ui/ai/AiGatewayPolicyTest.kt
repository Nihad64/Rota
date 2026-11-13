package com.nihad64.rotam.ui.ai
import org.junit.Assert.*
import org.junit.Test
class AiGatewayPolicyTest {
 @Test fun preservesApprovedModelWithoutSilentFallback(){assertEquals("gemini-3.5-flash-lite",GeminiService.MODEL)}
 @Test fun differentiatesAttestationAuthenticationAndModelAccess(){
  assertTrue(AiFailure.serverMessage("Invalid App Check token").contains("güvenli AI erişimi"))
  assertTrue(AiFailure.serverMessage("UNAUTHENTICATED").contains("Google oturumunu"))
  assertTrue(AiFailure.serverMessage("models/gemini-3.5-flash-lite not found").contains("Başka modele geçilmedi"))
  assertFalse(AiFailure.serverMessage("service temporarily unavailable").contains("model"))
 }
 @Test fun parsesSdkTextThroughTheSameDraftValidation(){
  val r=AiResponseParser.parseText("""{"reply":"Önce kontrol et","plans":[{"title":"Oku","date":"2026-11-10"}]}""")
  assertEquals(1,r.suggestions.size);assertNull(r.suggestions.single().plan.startTime)
  assertThrows(GeminiException::class.java){AiResponseParser.parseText("invalid json")}
 }
}
