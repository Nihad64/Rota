package com.nihad64.rotam.ui.settings

import android.content.Intent
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.time.LocalTime
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.layout.*
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
    val s by vm.settings.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var name by remember(s.userName) { mutableStateOf(s.userName) }
    var key by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    val pending by vm.pending.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    var model by remember(s.geminiModel) { mutableStateOf(s.geminiModel) }
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
        Text("Ayarlar", style = MaterialTheme.typography.displaySmall, color = OnBackground)
        Text("Rotam'ı kendi çalışma biçimine göre ayarla.", style = MaterialTheme.typography.bodyMedium, color = SubtleText)

        RotamCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RotamLogo(52.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Rotam", style = MaterialTheme.typography.titleLarge, color = OnBackground)
                    Text("Sürüm ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", style = MaterialTheme.typography.bodySmall, color = SubtleText)
                }
            }
        }

        ActionRow(Icons.Outlined.Search,"Planlarında ara","Başlık, not ve geçmiş planlar.","Ara",onClick=onSearch)
        Section("Hesabım")
        val account by vm.account.collectAsStateWithLifecycle()
        Text(account?.email ?: "Planların bu cihazda saklanıyor.", color=SubtleText)
        Text("Google girişi tek başına senkronizasyon değildir. Bulut veri aktarımı henüz etkin değil; cihazındaki kayıtlar korunur.",color=SubtleText,style=MaterialTheme.typography.bodySmall)
        if(account==null) OutlinedButton(onClick={ (ctx as? android.app.Activity)?.let(vm::signIn) },enabled=!busy&&vm.accountConfigured) { Text("Google ile giriş yap") }
        else TextButton(onClick=vm::signOut,enabled=!busy) { Text("Çıkış yap") }
        if(!vm.accountConfigured) Text("Hesap hizmeti bu geliştirme derlemesinde henüz yapılandırılmadı.",color=SubtleText)

        RotamCard(Modifier.fillMaxWidth()) {
            Text("İsmin (selamlama için)", style = MaterialTheme.typography.bodyMedium, color = OnSurface)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RotamField(name, { name = it }, "Örn. Nihad", Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { vm.setName(name) }, enabled = name != s.userName) { Text("Kaydet", color = Primary) }
            }
        }

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
        SwitchRow(Icons.Outlined.VolumeUp, "UI sesleri", "Kısa mekanik tık sesi.", s.uiSounds, vm::setSounds)
        SwitchRow(Icons.Outlined.Vibration, "Titreşim", "Hafif dokunsal geri bildirim.", s.haptics, vm::setHaptics)

        SwitchRow(Icons.Outlined.Animation,"Animasyonlar","Kısa ekran ve grafik geçişleri.",s.animations,vm::setAnimations)

        Section("Rotam AI")
        Text("Rotam AI için API anahtarı gerekmez. Google hesabınla güvenli hizmete bağlanır. Öneriler yalnız onayından sonra kaydedilir.", color=SubtleText)
        SwitchRow(Icons.Outlined.PrivacyTip,"Planları AI ile paylaş","İzin verirsen son 1 gün ve gelecek 14 gün içindeki en fazla 150 planın özeti AI hizmetine gönderilir. Yazdığın mesajlar her istekte gönderilir.",s.sharePlans,vm::setSharePlans)
        Section("Yedekleme")
        Text("Planlar, hedefler, tekrar kuralları ve odak süreleri korunur. API anahtarı yedeğe eklenmez.",color=SubtleText,style=MaterialTheme.typography.bodySmall)
        PrimaryButton("Yedeği dışa aktar",{export.launch("Rotam-${java.time.LocalDate.now()}.json")},Modifier.fillMaxWidth(),enabled=!busy)
        OutlinedButton(onClick={import.launch(arrayOf("application/json","text/plain","application/octet-stream"))},enabled=!busy,modifier=Modifier.fillMaxWidth()){Text("Yedekten geri yükle")}
        TextButton(onClick={vm.previewRecovery()},enabled=!busy){Text("Son işlemden önceki kayıtlara dön")}
        message?.let{Text(it,color=SubtleText)}
        if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())

    }

    if(quietTarget!=null) TimeChooser(if(quietTarget=="start")clock(s.quietStart) else clock(s.quietEnd),"Sessiz saat",{quietTarget=null}) { value->
        value?.let{val m=LocalTime.parse(it).toSecondOfDay()/60;if(quietTarget=="start")vm.setQuietTimes(m,s.quietEnd) else vm.setQuietTimes(s.quietStart,m)}
    }
    pending?.let { data-> AlertDialog(onDismissRequest={vm.dismissPreview()},title={Text("Yedeği geri yükle?")},text={Text("${data.visiblePlans} plan, ${data.goals.size} hedef ve ${data.series.size} tekrar kuralı bulundu. Mevcut kayıtlar bu yedekle değiştirilecek. İşlemden önce geri dönüş yedeği alınır.")},confirmButton={TextButton(onClick={vm.restore()},enabled=!busy){Text("Geri yükle")}},dismissButton={TextButton(onClick={vm.dismissPreview()},enabled=!busy){Text("Vazgeç")}}) }

}

@Composable
private fun Section(text: String) =
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = SubtleText, modifier = Modifier.padding(top = 10.dp))

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
