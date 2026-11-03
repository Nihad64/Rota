package com.nihad64.rotam.data.prefs
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
/** Device-bound key. Never included in JSON backups or the APK. */
class KeyVault(context:Context){
 private val prefs=context.getSharedPreferences("rotam_private_key",Context.MODE_PRIVATE)
 private fun key():SecretKey{val ks=KeyStore.getInstance("AndroidKeyStore").apply{load(null)}
  (ks.getKey("rotam.gemini",null) as? SecretKey)?.let{return it}
  return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply{init(KeyGenParameterSpec.Builder("rotam.gemini",KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())}.generateKey()}
 @Synchronized fun read():String=runCatching{val data=prefs.getString("cipher",null)?:return "";val parts=data.split(':');val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,Base64.decode(parts[0],Base64.NO_WRAP)));String(cipher.doFinal(Base64.decode(parts[1],Base64.NO_WRAP)),Charsets.UTF_8)}.getOrDefault("")
 @Synchronized fun write(v:String){if(v.isBlank()){check(prefs.edit().clear().commit());return};val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());val text=Base64.encodeToString(cipher.iv,Base64.NO_WRAP)+":"+Base64.encodeToString(cipher.doFinal(v.toByteArray(Charsets.UTF_8)),Base64.NO_WRAP);check(prefs.edit().putString("cipher",text).commit())}
}
