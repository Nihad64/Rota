package com.nihad64.rotam.ui

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

object TrFormat {
    val months = listOf("Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran", "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık")
    /** Pazartesi'den başlayarak kısa gün adları (Pz = Pazartesi, Pa = Pazar). */
    val dayShort = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
    val dayLong = listOf("Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar")

    fun dayName(d: LocalDate) = dayLong[d.dayOfWeek.value - 1]
    fun short(d: DayOfWeek) = dayShort[d.value - 1]
    fun full(d: LocalDate) = "${d.dayOfMonth} ${months[d.monthValue - 1]} ${d.year} · ${dayName(d)}"
    fun dayMonth(d: LocalDate) = "${d.dayOfMonth} ${months[d.monthValue - 1]}"
    fun monthYear(d: LocalDate) = "${months[d.monthValue - 1]} ${d.year}"

    fun greeting(now: LocalTime = LocalTime.now()) = when (now.hour) {
        in 5..11 -> "Günaydın"
        in 12..17 -> "İyi günler"
        in 18..22 -> "İyi akşamlar"
        else -> "İyi geceler"
    }

    private val quotes = listOf(
        "Her adım, seni hedefine yaklaştırır.",
        "Küçük adımlar, büyük yolculuklar başlatır.",
        "Bugün planla, yarın gülümse.",
        "Disiplin, motivasyon bittiğinde devreye girer.",
        "Odaklan. Başla. Bitir.",
        "En iyi zaman şimdi.",
        "Az ama düzenli; işte gerçek ilerleme.",
        "Rotanı sen çiz, gerisi gelir."
    )
    fun quoteOfDay(d: LocalDate = LocalDate.now()) = quotes[(d.dayOfYear) % quotes.size]
}
