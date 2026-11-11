package com.nihad64.rotam.ui.settings

import android.content.Intent
import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.time.LocalTime
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import com.nihad64.rotam.data.sync.*
import org.json.JSONObject
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nihad64.rotam.BuildConfig
import com.nihad64.rotam.ui.components.*
import com.nihad64.rotam.ui.home.RotamLogo
import com.nihad64.rotam.ui.newplan.RotamField
import com.nihad64.rotam.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel, onStats: ()->Unit, onSearch: ()->Unit) {
    var page by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(page!=null){page=null}
    val s by vm.settings.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val pending by vm.pending.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val account by vm.account.collectAsStateWithLifecycle()
    val cloud by vm.cloudStatus.collectAsStateWithLifecycle()
    var confirmCloud by remember { mutableStateOf(false) }
    var confirmAi by remember { mutableStateOf(false) }
    val authBusy by vm.authBusy.collectAsStateWithLifecycle()
    val authMessage by vm.authMessage.collectAsStateWithLifecycle()
    var quietTarget by remember { mutableStateOf<String?>(null) }
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){it?.let(vm::export)}
    val import=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){it?.let(vm::preview)}
    val notificationPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){}
    fun clock(minutes:Int)="%02d:%02d".format(minutes/60,minutes%60)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PageHeader(page ?: "Ayarlar",onBack=if(page!=null)({page=null})else null)
        Text("Rotam'ı kendi çalışma biçimine göre ayarla.", style = MaterialTheme.typography.bodyMedium, color = SubtleText)

        if(page=="Rotam hakkında") RotamCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RotamLogo(52.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Rotam", style = MaterialTheme.typography.titleLarge, color = OnBackground)
                    Text("Sürüm ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", style = MaterialTheme.typography.bodySmall, color = SubtleText)
                }
            }
        }

        if(page==null) {
            Spacer(Modifier.height(4.dp))
            SettingsHero(Modifier.fillMaxWidth()) {
                Row(Modifier.testTag("account-settings-entry").clickable {page="Hesabım"}.padding(8.dp),verticalAlignment=Alignment.CenterVertically) {
                    RotamLogo(44.dp)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(account?.name ?: "Kendi ritmini kur.",style=MaterialTheme.typography.titleLarge)
                        Text(if(account!=null) "Google hesabınla bağlı" else "Hesabın ve sana ait tercihler",color=SubtleText,style=MaterialTheme.typography.bodyMedium)
                    }
                    Icon(Icons.Outlined.ChevronRight,"Hesabım",tint=Accent)
                }
            }
            SettingsGroup("TERCİHLER") {
                SettingsLink(Icons.Outlined.NotificationsActive,"Hatırlatmalar","Ses, titreşim ve hatırlatma zamanı"){page="Hatırlatmalar"}
                HorizontalDivider(color=Outline)
                SettingsLink(Icons.Outlined.AutoAwesome,"Rotam AI","Birlikte planla, onaylayarak kaydet"){page="Rotam AI"}
            }
            SettingsGroup("VERİLER VE GİZLİLİK") {
                SettingsLink(Icons.Outlined.CloudQueue,"Verilerim","Eşitleme ve yedekleme"){page="Verilerim"}
                HorizontalDivider(color=Outline)
                SettingsLink(Icons.Outlined.Shield,"Gizlilik","AI ile paylaştıkların üzerinde kontrol"){page="Gizlilik"}
            }
            SettingsGroup("UYGULAMA") {
                SettingsLink(Icons.Outlined.Info,"Rotam hakkında","Sürüm ve yenilikler"){page="Rotam hakkında"}
                HorizontalDivider(color=Outline)
                SettingsLink(Icons.Outlined.ChatBubbleOutline,"Sorun bildir","Gördüğün sorunu paylaş"){page="Sorun bildir"}
            }
        }
        if(page=="Hesabım") {
        Section("Hesabım")
        Text(account?.email ?: "Planların bu cihazda saklanıyor.", color=SubtleText)
        Text("Planlarının diğer cihazlarında da görünmesini Verilerim bölümünden etkinleştirebilirsin. Eşitlemeyi açmadan kayıtların yalnız bu cihazda kalır.",color=SubtleText,style=MaterialTheme.typography.bodySmall)
        if(account==null) OutlinedButton(onClick={ vm.signIn(ctx) },enabled=!authBusy&&vm.accountConfigured) { Text(if(authBusy) "Google hesabı bekleniyor…" else "Google ile giriş yap") }
        else TextButton(onClick=vm::signOut,enabled=!busy) { Text("Çıkış yap") }
        if(authBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
        authMessage?.let { Text(it, color=OnSurface, style=MaterialTheme.typography.bodyMedium) }
        if(!vm.accountConfigured) Text("Hesap hizmeti bu geliştirme derlemesinde henüz yapılandırılmadı.",color=SubtleText)

        }
        if(page=="Hatırlatmalar") {
        Section("Bildirimler")
        SwitchRow(Icons.Outlined.NotificationsActive, "Plan hatırlatmaları", "Her planın kendi hatırlatma seçimi uygulanır.", s.remindersEnabled, vm::setReminders)
        if(Build.VERSION.SDK_INT>=33) ActionRow(Icons.Outlined.Notifications,"Android bildirim izni","Hatırlatmalar için telefonun bildirim iznini aç.","Kontrol et") { notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }
        RotamCard(Modifier.fillMaxWidth()) {
            Text("Yeni planlarda varsayılan süre", style = MaterialTheme.typography.titleSmall, color = OnBackground)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5 to "5 dk", 15 to "15 dk", 30 to "30 dk", 60 to "1 saat").forEach { (m, l) ->
                    SelectChip("$l önce", s.reminderMinutes == m, { vm.setReminderMinutes(m) })
                }
            }
        }
        SwitchRow(Icons.Outlined.Bedtime, "Sessiz saatler", "${clock(s.quietStart)} – ${clock(s.quietEnd)} arası hatırlatmaları sessiz saatlerin sonuna taşı.", s.quietEnabled, vm::setQuiet)
        if(s.quietEnabled) {
            DetailRow("Sessiz saat başlangıcı",clock(s.quietStart)){quietTarget="start"}
            DetailRow("Sessiz saat bitişi",clock(s.quietEnd)){quietTarget="end"}
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val granted = vm.canScheduleExact()
            ActionRow(Icons.Outlined.AlarmOn, "Kesin bildirim zamanı",
                if (granted) "Kesin alarm izni açık. Telefonun pil ve bildirim ayarları da uygulanır." else "Bildirimlerin tam saatinde gelmesi için Android izni gerekli.",
                if (granted) "Açık" else "İzin ver") {
                ctx.startActivity(Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}")))
            }
        }

        Section("Ses · Hareket")
        SwitchRow(Icons.Outlined.VolumeUp, "Onay sesleri", "Başarılı kayıtta kısa ses; telefonun sessiz modu korunur.", s.uiSounds, vm::setSounds)
        SwitchRow(Icons.Outlined.Vibration, "Titreşim", "Hafif dokunsal geri bildirim.", s.haptics, vm::setHaptics)

        SwitchRow(Icons.Outlined.Animation,"Animasyonlar","Kısa ekran ve grafik geçişleri.",s.animations,vm::setAnimations)

        }
        if(page=="Rotam AI" || page=="Gizlilik") {
        Section("Rotam AI")
        Text("Rotam AI için API anahtarı gerekmez. Google hesabınla güvenli hizmete bağlanır. Öneriler yalnız onayından sonra kaydedilir.", color=SubtleText)
        SwitchRow(Icons.Outlined.PrivacyTip,"Planları AI ile paylaş","İzin verirsen sorundaki tarih aralığından en fazla 150 planın başlık, kategori ve tarih/saat özeti AI hizmetine gönderilir. Yazdığın mesajlar her istekte gönderilir.",s.sharePlans,{if(it)confirmAi=true else vm.setSharePlans(false)})
        }
        if((page=="Rotam AI"||page=="Gizlilik")&&s.sharePlans&&s.aiContextVersion<2)TextButton(onClick={confirmAi=true}){Text("Geçmiş kayıtlar için paylaşımı gözden geçir")}
        if(page=="Verilerim") {
        ActionRow(Icons.Outlined.Search,"Planlarında ara","Başlık, not ve geçmiş planlar.","Ara",onClick=onSearch)
        CloudSyncSection(cloud,account?.email,onEnable={confirmCloud=true},onDisable=vm::disableSync,onSync=vm::syncNow,onResolve=vm::resolveSync)
        if(account==null) OutlinedButton(onClick={page="Hesabım"}){Text("Google hesabını bağla")}
        Section("Yedekleme")
        Text("Planlar, hedefler, tekrar kuralları ve odak süreleri korunur. API anahtarı yedeğe eklenmez.",color=SubtleText,style=MaterialTheme.typography.bodySmall)
        PrimaryButton("Yedeği dışa aktar",{export.launch("Rotam-${java.time.LocalDate.now()}.json")},Modifier.fillMaxWidth(),enabled=!busy)
        OutlinedButton(onClick={import.launch(arrayOf("application/json","text/plain","application/octet-stream"))},enabled=!busy,modifier=Modifier.fillMaxWidth()){Text("Yedekten geri yükle")}
        TextButton(onClick={vm.previewRecovery()},enabled=!busy){Text("Son işlemden önceki kayıtlara dön")}
        message?.let{Text(it,color=SubtleText)}
        if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        if(page=="Sorun bildir") {
            Text("Sorunu anlatırken ne yaptığını, ne beklediğini ve gördüğün hata mesajını ekle. Hesap şifreni veya giriş kodunu paylaşma.",color=SubtleText)
            PrimaryButton("Sorun metnini paylaş",{
                val intent=Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,"Rotam ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\nAndroid ${Build.VERSION.RELEASE}\n\nYaptığım işlem:\nBeklediğim sonuç:\nGördüğüm sorun:")
                ctx.startActivity(Intent.createChooser(intent,"Sorunu paylaş"))
            })
        }
    }

    if(confirmAi) AlertDialog(onDismissRequest={confirmAi=false},title={Text("Planları AI ile paylaş")},text={Text("Soruna göre geçmişten veya gelecekten seçilen en fazla 150 planın başlığı, kategorisi, tarih/saatleri ve tamamlanma bilgisi Google Gemini'ye gönderilir. Tüm arşiv her istekte gönderilmez. İstediğin zaman bu paylaşımı kapatabilirsin. Yerel arama ve sayım için bu izin gerekmez.")},confirmButton={TextButton(onClick={confirmAi=false;vm.setSharePlans(true)}){Text("Paylaşımı aç")}},dismissButton={TextButton(onClick={confirmAi=false}){Text("Vazgeç")}})
    if(confirmCloud) AlertDialog(onDismissRequest={confirmCloud=false},title={Text("Planlarını hesabına bağla?")},text={Text("Planların, notların, hedeflerin, rutinlerin ve odak sürelerin ${account?.email ?: "Google hesabın"} hesabına bağlı olarak Frankfurt'taki bulut veritabanında saklanır. Aynı hesapla diğer cihazlarında eşitlenir. Mevcut kayıtlar karşılaştırılarak birleştirilir; çakışan değişikliklerde seçim istenir. Ses ve bildirim tercihlerin bu cihazda kalır. İstediğin zaman eşitlemeyi durdurabilirsin.")},confirmButton={TextButton(onClick={confirmCloud=false;vm.enableSync()}){Text("Bağla ve eşitle")}},dismissButton={TextButton(onClick={confirmCloud=false}){Text("Şimdi değil")}})
    if(quietTarget!=null) TimeChooser(if(quietTarget=="start")clock(s.quietStart) else clock(s.quietEnd),"Sessiz saat",{quietTarget=null}) { value->
        value?.let{val m=LocalTime.parse(it).toSecondOfDay()/60;if(quietTarget=="start")vm.setQuietTimes(m,s.quietEnd) else vm.setQuietTimes(s.quietStart,m)}
    }
    pending?.let { data-> AlertDialog(onDismissRequest={vm.dismissPreview()},title={Text("Yedeği geri yükle?")},text={Text("${data.visiblePlans} plan, ${data.goals.size} hedef ve ${data.series.size} tekrar kuralı bulundu. Mevcut kayıtlar bu yedekle değiştirilecek. İşlemden önce geri dönüş yedeği alınır.")},confirmButton={TextButton(onClick={vm.restore()},enabled=!busy){Text("Geri yükle")}},dismissButton={TextButton(onClick={vm.dismissPreview()},enabled=!busy){Text("Vazgeç")}}) }

}

@Composable
private fun Section(text: String) =
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = Accent, modifier = Modifier.padding(top = 10.dp))

@Composable
private fun SwitchRow(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    RotamCard(Modifier.fillMaxWidth(), onClick = { onChange(!checked) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Primary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = OnBackground)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SubtleText)
            }
            Switch(checked, onChange, colors = SwitchDefaults.colors(checkedTrackColor = Primary, checkedThumbColor = OnBackground,
                uncheckedTrackColor = SurfaceVariant, uncheckedBorderColor = Outline))
        }
    }
}

@Composable
private fun ActionRow(icon: ImageVector, title: String, subtitle: String, action: String, danger: Boolean = false, onClick: () -> Unit) {
    RotamCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (danger) Danger else Primary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = OnBackground)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SubtleText)
            }
            FilledTonalButton(onClick = onClick, colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = (if (danger) Danger else Primary).copy(alpha = 0.18f),
                contentColor = if (danger) Danger else PrimaryVariant)) { Text(action) }
        }
    }
}

@Composable
private fun SettingsGroup(title:String,content:@Composable ColumnScope.()->Unit) {
    Section(title)
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(0.dp),content=content)
}
@Composable
private fun SettingsLink(icon:ImageVector,title:String,subtitle:String,onClick:()->Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(vertical=20.dp),verticalAlignment=Alignment.CenterVertically) {
        Icon(icon,null,tint=Accent,modifier=Modifier.size(28.dp))
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(title,style=MaterialTheme.typography.titleLarge)
            Text(subtitle,style=MaterialTheme.typography.bodyMedium,color=SubtleText)
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Outlined.ChevronRight,null,tint=OnSurface)
    }
}
@Composable
private fun CloudSyncSection(status:CloudStatus,email:String?,onEnable:()->Unit,onDisable:()->Unit,onSync:()->Unit,onResolve:(Boolean)->Unit) {
    Section("Cihazlar arası eşitleme")
    val title=when(status){
        CloudStatus.Off->"Planların bu cihazda."
        CloudStatus.SignedOut->"Eşitleme için giriş yap."
        CloudStatus.DifferentAccount->"Bağlı hesabını kullan."
        CloudStatus.Working->"Planların eşitleniyor…"
        is CloudStatus.Current->"Planların güncel."
        is CloudStatus.Review->"İki farklı değişiklik var."
        is CloudStatus.Error->"Eşitleme bekliyor."
    }
    SettingsHero(Modifier.fillMaxWidth()) {
        Icon(Icons.Outlined.CloudQueue,null,tint=Accent,modifier=Modifier.size(32.dp))
        Spacer(Modifier.height(12.dp))
        Text(title,style=MaterialTheme.typography.headlineSmall)
        Text(when(status){
            CloudStatus.Off->"Aynı Google hesabıyla cihazların arasında devam et."
            CloudStatus.SignedOut->"Yerel kayıtların korundu. Bağlı Google hesabınla giriş yaptığında devam eder."
            CloudStatus.DifferentAccount->"Bu cihazın kayıtları başka bir hesaba bağlı. Hesaplar arasında otomatik aktarım yapılmaz."
            CloudStatus.Working->"Yerel kayıtların kullanılmaya devam eder."
            is CloudStatus.Current->"Son eşitleme: "+java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(status.time))
            is CloudStatus.Review->"Aynı kayıt başka cihazda da değişmiş. Aşağıdaki sürümleri incele ve korumak istediğini seç."
            is CloudStatus.Error->status.message
        },style=MaterialTheme.typography.bodyMedium,color=SubtleText)
        if(status==CloudStatus.Working){Spacer(Modifier.height(12.dp));LinearProgressIndicator(Modifier.fillMaxWidth())}
    }
    if(status is CloudStatus.Review){
        status.conflicts.forEach { conflict->
            fun label(record:CloudRecord?):String {
                if(record==null)return "Silinmiş kayıt"
                val json=JSONObject(record.body)
                val name=if(conflict.key.kind==RecordKind.SERIES)json.getJSONObject("template").optString("title")else json.optString("title")
                val date=json.optString("date","")
                val outcome=if(json.optBoolean("isDeleted"))" · Silindi" else if(json.optBoolean("isCompleted"))" · Tamamlandı" else ""
                return name+(if(date.isNotBlank())" · $date" else "")+outcome
            }
            Text("Bu cihaz: "+label(conflict.local),style=MaterialTheme.typography.bodyMedium)
            Text("Bulut: "+label(conflict.remote),style=MaterialTheme.typography.bodyMedium,color=SubtleText)
        }
        PrimaryButton("Bu cihazdaki sürümleri koru",{onResolve(true)},Modifier.fillMaxWidth())
        OutlinedButton(onClick={onResolve(false)},modifier=Modifier.fillMaxWidth()){Text("Buluttaki sürümleri kullan")}
        Text("Seçimin yalnız çakışan kayıtlara uygulanır. Diğer değişiklikler birleştirilir.",color=SubtleText,style=MaterialTheme.typography.bodySmall)
    }
    if(status==CloudStatus.Off)PrimaryButton("Eşitlemeyi etkinleştir",onEnable,Modifier.fillMaxWidth(),enabled=email!=null)
    else {
        OutlinedButton(onClick=onSync,enabled=status!=CloudStatus.Working&&status !is CloudStatus.Review,modifier=Modifier.fillMaxWidth()){Text("Şimdi eşitle")}
        TextButton(onClick=onDisable){Text("Eşitlemeyi durdur")}
    }
}

@Composable
private fun SettingsHero(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit) {
 Column(modifier.clip(RoundedCornerShape(topStart=28.dp,topEnd=28.dp,bottomStart=28.dp,bottomEnd=52.dp)).background(Brush.linearGradient(listOf(HeroSurface,Surface))).padding(24.dp),content=content)
}
