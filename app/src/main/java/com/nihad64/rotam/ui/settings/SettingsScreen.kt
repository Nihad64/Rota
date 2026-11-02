package com.nihad64.rotam.ui.settings

import android.content.Intent
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
fun SettingsScreen(vm: SettingsViewModel) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var name by remember(s.userName) { mutableStateOf(s.userName) }
    var key by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }

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

        Section("Profil")
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
        SwitchRow(Icons.Outlined.NotificationsActive, "Plan hatırlatmaları", "Saatli planlardan önce bildirim.", s.remindersEnabled, vm::setReminders)
        RotamCard(Modifier.fillMaxWidth()) {
            Text("Hatırlatma zamanı", style = MaterialTheme.typography.titleSmall, color = OnBackground)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5 to "5 dk", 15 to "15 dk", 30 to "30 dk", 60 to "1 saat").forEach { (m, l) ->
                    SelectChip("$l önce", s.reminderMinutes == m, { vm.setReminderMinutes(m) })
                }
            }
        }
        SwitchRow(Icons.Outlined.Bedtime, "Sessiz saatler", "%02d:00 – %02d:00 arası bildirimleri sustur.".format(s.quietStart, s.quietEnd), s.quietEnabled, vm::setQuiet)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val granted = vm.canScheduleExact()
            ActionRow(Icons.Outlined.AlarmOn, "Kesin bildirim zamanı",
                if (granted) "İzin verildi. Bildirimler tam saatinde gelir." else "Bildirimlerin tam saatinde gelmesi için Android izni gerekli.",
                if (granted) "Açık" else "İzin ver") {
                ctx.startActivity(Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}")))
            }
        }

        Section("Ses · Hareket")
        SwitchRow(Icons.Outlined.VolumeUp, "UI sesleri", "Kısa mekanik tık sesi.", s.uiSounds, vm::setSounds)
        SwitchRow(Icons.Outlined.Vibration, "Titreşim", "Hafif dokunsal geri bildirim.", s.haptics, vm::setHaptics)

        Section("Tema")
        RotamCard(Modifier.fillMaxWidth()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("dark" to "Koyu", "light" to "Açık", "system" to "Sistem").forEach { (k, l) ->
                    SelectChip(l, s.theme == k, { vm.setTheme(k) })
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("Şimdilik yalnızca koyu tema kullanılıyor; açık tema yakında.", style = MaterialTheme.typography.bodySmall, color = SubtleText)
        }

        Section("Rotam AI")
        RotamCard(Modifier.fillMaxWidth()) {
            Text("Gemini API anahtarı", style = MaterialTheme.typography.titleSmall, color = OnBackground)
            Text(
                when {
                    s.geminiKey.isNotBlank() -> "Kendi anahtarın kayıtlı (••••${s.geminiKey.takeLast(4)})."
                    vm.hasBuildKey -> "Uygulamayla gelen anahtar kullanılıyor."
                    else -> "Anahtar yok. Rotam AI için bir anahtar ekle."
                },
                style = MaterialTheme.typography.bodySmall, color = SubtleText
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = key, onValueChange = { key = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text("AIza...", color = SubtleText) },
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(if (showKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, "Göster", tint = SubtleText)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = Outline,
                    focusedTextColor = OnBackground, unfocusedTextColor = OnBackground)
            )
            Row {
                TextButton(onClick = { vm.setGeminiKey(key); key = "" }, enabled = key.isNotBlank()) { Text("Kaydet", color = Primary) }
                if (s.geminiKey.isNotBlank()) TextButton(onClick = { vm.setGeminiKey("") }) { Text("Kaldır", color = SubtleText) }
            }
        }

        Section("Veri")
        ActionRow(Icons.Outlined.DeleteForever, "Tüm planları sıfırla", "Bütün planlar ve hedefler kalıcı olarak silinir.", "Sıfırla", danger = true) {
            confirmReset = true
        }
    }

    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        containerColor = Surface,
        title = { Text("Emin misin?", color = OnBackground) },
        text = { Text("Tüm planlar ve hedefler silinecek. Bu işlem geri alınamaz.", color = OnSurface) },
        confirmButton = { TextButton(onClick = { vm.resetAll(); confirmReset = false }) { Text("Sil", color = Danger) } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Vazgeç", color = OnSurface) } }
    )
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
