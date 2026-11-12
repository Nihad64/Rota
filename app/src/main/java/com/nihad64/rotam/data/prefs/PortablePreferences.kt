package com.nihad64.rotam.data.prefs

/** Deliberately excludes consent, account/session tokens, API keys and onboarding state. */
object PortablePreferences {
 val booleans=setOf("reminders","quiet","ui_sounds","haptics","animations")
 val integers=mapOf("reminder_minutes" to 0..10080,"quiet_start_minutes" to 0..1439,"quiet_end_minutes" to 0..1439)
 val keys=booleans+integers.keys
 fun values(s:Settings):Map<String,Any> = mapOf("reminders" to s.remindersEnabled,"quiet" to s.quietEnabled,"ui_sounds" to s.uiSounds,"haptics" to s.haptics,"animations" to s.animations,"reminder_minutes" to s.reminderMinutes,"quiet_start_minutes" to s.quietStart,"quiet_end_minutes" to s.quietEnd)
 fun validate(values:Map<String,Any>){
  require(values.keys==keys){"Tercih biçimi desteklenmiyor. Uygulamanı güncelle; cihaz tercihlerin korunuyor."}
  values.forEach{(k,v)->if(k in booleans)require(v is Boolean)else require((v is Int||v is Long)&&(v as Number).toLong() in integers.getValue(k).let{it.first.toLong()..it.last.toLong()})}
 }
 fun label(key:String)=when(key){"reminders"->"Hatırlatmalar";"quiet"->"Sessiz saatler";"ui_sounds"->"Onay sesleri";"haptics"->"Titreşim";"animations"->"Hareket";"reminder_minutes"->"Özel hatırlatma ön süresi";"quiet_start_minutes"->"Sessiz saat başlangıcı";"quiet_end_minutes"->"Sessiz saat bitişi";else->key}
 fun display(key:String,value:Any?):String=when{value is Boolean->if(value)"Açık"else"Kapalı";key.startsWith("quiet_")&&value is Number->"%02d:%02d".format(value.toInt()/60,value.toInt()%60);value is Number->"$value dk";else->"Yok"}
}
