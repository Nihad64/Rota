package com.nihad64.rotam.ui.stats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nihad64.rotam.ui.TrFormat
import com.nihad64.rotam.ui.components.*
import com.nihad64.rotam.ui.theme.*

@Composable
fun StatsScreen(vm: StatsViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("İstatistikler", style = MaterialTheme.typography.displaySmall, color = OnBackground)
        Text("İlerlemeni gör, ritmini koru.", style = MaterialTheme.typography.bodyMedium, color = SubtleText)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(Icons.Outlined.LocalFireDepartment, "${s.streak} gün", "Seri", Modifier.weight(1f))
            StatTile(Icons.Outlined.TaskAlt, "${s.totalDone}", "Tamamlanan", Modifier.weight(1f))
            StatTile(Icons.Outlined.Percent, "%${(s.weekRatio * 100).toInt()}", "Bu hafta", Modifier.weight(1f))
        }

        RotamCard(Modifier.fillMaxWidth()) {
            Text("Son 7 gün", style = MaterialTheme.typography.titleMedium, color = OnBackground)
            Text("Günlük tamamlanma oranı", style = MaterialTheme.typography.bodySmall, color = SubtleText)
            Spacer(Modifier.height(16.dp))
            WeekChart(s.week)
        }

        RotamCard(Modifier.fillMaxWidth()) {
            Text("Kategori dağılımı", style = MaterialTheme.typography.titleMedium, color = OnBackground)
            s.topCategory?.let {
                Text("En çok: ${it.label}", style = MaterialTheme.typography.bodySmall, color = PrimaryVariant)
            }
            Spacer(Modifier.height(12.dp))
            if (s.byCategory.isEmpty()) {
                Text("Planlarını tamamladıkça burada dağılımı göreceksin.", style = MaterialTheme.typography.bodyMedium, color = SubtleText)
            } else {
                val max = s.byCategory.maxOf { it.second }.coerceAtLeast(1)
                s.byCategory.forEach { (cat, count) ->
                    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        CategoryIcon(cat, 32.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Row {
                                Text(cat.label, style = MaterialTheme.typography.bodyMedium, color = OnSurface, modifier = Modifier.weight(1f))
                                Text("$count", style = MaterialTheme.typography.labelLarge, color = OnBackground)
                            }
                            Spacer(Modifier.height(4.dp))
                            val p by animateFloatAsState(count.toFloat() / max, tween(900), label = "cat")
                            LinearProgressIndicator(
                                progress = { p }, modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = cat.color(), trackColor = SurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(icon: ImageVector, value: String, label: String, modifier: Modifier) {
    RotamCard(modifier) {
        Icon(icon, null, tint = Primary)
        Spacer(Modifier.height(8.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, color = OnBackground)
        Text(label, style = MaterialTheme.typography.labelSmall, color = SubtleText)
    }
}

@Composable
private fun WeekChart(week: List<DayStat>) {
    val anim by animateFloatAsState(if (week.isEmpty()) 0f else 1f, tween(1000), label = "chart")
    Canvas(Modifier.fillMaxWidth().height(140.dp)) {
        if (week.isEmpty()) return@Canvas
        val slot = size.width / week.size
        val barW = slot * 0.5f
        week.forEachIndexed { i, d ->
            val x = i * slot + (slot - barW) / 2
            drawRoundRect(SurfaceVariant, Offset(x, 0f), Size(barW, size.height), CornerRadius(12f))
            val h = size.height * d.ratio * anim
            if (h > 0f) drawRoundRect(
                Brush.verticalGradient(listOf(PrimaryVariant, Primary), startY = size.height - h, endY = size.height),
                Offset(x, size.height - h), Size(barW, h), CornerRadius(12f)
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth()) {
        week.forEach {
            Text(TrFormat.short(it.date.dayOfWeek), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall,
                color = if (it.date == java.time.LocalDate.now()) Primary else SubtleText, textAlign = TextAlign.Center)
        }
    }
}
