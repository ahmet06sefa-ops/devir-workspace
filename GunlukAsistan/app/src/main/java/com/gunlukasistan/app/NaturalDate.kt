package com.gunlukasistan.app

import java.util.Calendar
import java.util.Locale

/**
 * v7.2 — Türkçe doğal dil tarih/saat ayrıştırıcı.
 *
 * "yarın 14:00 dişçi" → görev "dişçi", yarın saat 14:00 hatırlatma
 * "her salı 19:00 spor" → tekrarlayan (şimdilik ilk salıya kurulur)
 * "3 gün sonra kira"    → 3 gün sonrası
 * "15 ağustos düğün"    → o tarih
 *
 * Tamamen çevrimdışı çalışır, ağ gerektirmez.
 */
object NaturalDate {

    /**
     * Ayrıştırma sonucu.
     * @param text tarih ifadeleri temizlenmiş, geriye kalan asıl metin
     * @param millis hatırlatma zamanı (0 = tarih bulunamadı)
     * @param hasTime kullanıcı açıkça saat belirtti mi
     * @param repeatDow tekrar eden haftanın günü (Calendar sabiti, 0 = tekrar yok)
     * @param matched metinde yakalanan tarih ifadesi (kullanıcıya gösterilir)
     */
    data class Result(
        val text: String,
        val millis: Long,
        val hasTime: Boolean,
        val repeatDow: Int = 0,
        val matched: String = ""
    ) {
        val found: Boolean get() = millis > 0L
    }

    /** "her gün" için özel tekrar kodu (Calendar gün sabitleriyle çakışmaz). */
    const val REPEAT_DAILY = -1

    /** Haftanın günleri — Türkçe adları ve Calendar karşılıkları. */
    private val WEEKDAYS = listOf(
        Triple(listOf("pazartesi", "pzt"), Calendar.MONDAY, "Pazartesi"),
        Triple(listOf("salı", "sali"), Calendar.TUESDAY, "Salı"),
        Triple(listOf("çarşamba", "carsamba", "çrş"), Calendar.WEDNESDAY, "Çarşamba"),
        Triple(listOf("perşembe", "persembe", "prş"), Calendar.THURSDAY, "Perşembe"),
        Triple(listOf("cuma"), Calendar.FRIDAY, "Cuma"),
        Triple(listOf("cumartesi", "cmt"), Calendar.SATURDAY, "Cumartesi"),
        Triple(listOf("pazar"), Calendar.SUNDAY, "Pazar")
    )

    /** Ay adları — 0 tabanlı. */
    private val MONTHS = listOf(
        listOf("ocak") to 0,
        listOf("şubat", "subat") to 1,
        listOf("mart") to 2,
        listOf("nisan") to 3,
        listOf("mayıs", "mayis") to 4,
        listOf("haziran") to 5,
        listOf("temmuz") to 6,
        listOf("ağustos", "agustos") to 7,
        listOf("eylül", "eylul") to 8,
        listOf("ekim") to 9,
        listOf("kasım", "kasim") to 10,
        listOf("aralık", "aralik") to 11
    )

    /**
     * Günün belirli bölümleri → (varsayılan saat, 12 saat eklensin mi).
     * İkinci değer "akşam 8" gibi kullanımlar içindir: 8 → 20:00.
     */
    private val DAYPARTS = listOf(
        Triple(listOf("sabah"), 9, false),
        Triple(listOf("öğleden sonra", "ogleden sonra"), 15, true),
        Triple(listOf("öğlen", "oglen", "öğle", "ogle"), 12, true),
        Triple(listOf("ikindi"), 16, true),
        Triple(listOf("akşamüstü", "aksamustu"), 18, true),
        Triple(listOf("akşam", "aksam"), 20, true),
        Triple(listOf("gece", "geceyarısı", "geceyarisi"), 22, true)
    )

    /** Küçük harfe çevirirken Türkçe I/İ sorununu önler. */
    private fun lower(s: String): String = s.lowercase(Locale("tr", "TR"))

    private fun cal(): Calendar = Calendar.getInstance().apply {
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }

    /**
     * Metni ayrıştırır. Tarih bulunamazsa `millis = 0` döner ve
     * `text` girdiyle aynı kalır.
     */
    fun parse(input: String): Result {
        val original = input.trim()
        if (original.isEmpty()) return Result(original, 0L, false)

        var work = " " + lower(original) + " "
        val eaten = StringBuilder()

        val c = cal()
        var dayFound = false
        var hour = -1
        var minute = 0
        var repeatDow = 0

        /** Yakalanan ifadeyi metinden siler ve etiketi kaydeder. */
        fun eat(regex: Regex, label: String? = null): MatchResult? {
            val m = regex.find(work) ?: return null
            if (label != null) {
                if (eaten.isNotEmpty()) eaten.append(" ")
                eaten.append(label)
            }
            work = work.replaceRange(m.range, " ")
            return m
        }

        // ---- 1) Saat: "14:00", "14.30", "saat 9", "9'da" ----
        // Not: aralık 00-23 / 00-59 ile sınırlı — yoksa "25.12" (tarih) saat sanılır.
        eat(Regex("""\b([01]?\d|2[0-3])[:.]([0-5]\d)\b"""))?.let { m ->
            val h = m.groupValues[1].toInt()
            val mi = m.groupValues[2].toInt()
            hour = h; minute = mi
            if (eaten.isNotEmpty()) eaten.append(" ")
            eaten.append(String.format(Locale.US, "%02d:%02d", h, mi))
        }
        if (hour < 0) {
            eat(Regex("""\bsaat\s+(\d{1,2})\b"""))?.let { m ->
                val h = m.groupValues[1].toInt()
                if (h in 0..23) {
                    hour = h
                    if (eaten.isNotEmpty()) eaten.append(" ")
                    eaten.append(String.format(Locale.US, "%02d:00", h))
                }
            }
        }
        if (hour < 0) {
            // "9'da", "14te", "19da"
            eat(Regex("""\b(\d{1,2})['’]?\s?(?:da|de|ta|te)\b"""))?.let { m ->
                val h = m.groupValues[1].toInt()
                if (h in 0..23) {
                    hour = h
                    if (eaten.isNotEmpty()) eaten.append(" ")
                    eaten.append(String.format(Locale.US, "%02d:00", h))
                }
            }
        }

        // ---- 2) Günün bölümü: sabah / akşam / öğleden sonra ... ----
        var daypartHour = -1
        var daypartPm = false
        for ((words, h, pm) in DAYPARTS) {
            var hit = false
            for (w in words) {
                if (eat(Regex("""\b$w\b"""), w.replaceFirstChar { it.uppercase() }) != null) {
                    hit = true; break
                }
            }
            if (hit) { daypartHour = h; daypartPm = pm; break }
        }

        // Gün bölümü verildi ama saat henüz bulunmadıysa çıplak sayıyı saat say:
        // "akşam 7 buluşma" → 19:00.  ("her gün su iç" gibi durumlarda sayı yok.)
        if (daypartHour >= 0 && hour < 0) {
            eat(Regex("""\b([01]?\d|2[0-3])\b"""))?.let { m ->
                val h = m.groupValues[1].toInt()
                hour = h
                if (eaten.isNotEmpty()) eaten.append(" ")
                eaten.append(String.format(Locale.US, "%02d:00", h))
            }
        }

        // "akşam 8" / "öğlen 1" → 12 saat ekle (8 → 20:00, 1 → 13:00)
        if (daypartPm && hour in 1..11) {
            hour += 12
            // Etiketi de düzelt
            val fixed = String.format(Locale.US, "%02d:%02d", hour, minute)
            val idx = eaten.indexOf(String.format(Locale.US, "%02d:%02d", hour - 12, minute))
            if (idx >= 0) eaten.replace(idx, idx + 5, fixed)
        }

        // ---- 3) Mutlak tarih: "15 ağustos", "15.08", "15/08/2026" ----
        run {
            // gün + ay adı
            for ((words, mIndex) in MONTHS) {
                for (w in words) {
                    val m = eat(Regex("""\b(\d{1,2})\s+$w\b""")) ?: continue
                    val day = m.groupValues[1].toInt()
                    if (day in 1..31) {
                        c.set(Calendar.MONTH, mIndex)
                        c.set(Calendar.DAY_OF_MONTH, day)
                        // Tarih geçtiyse gelecek yıl
                        if (c.timeInMillis < System.currentTimeMillis() - 86_400_000L) {
                            c.add(Calendar.YEAR, 1)
                        }
                        dayFound = true
                        if (eaten.isNotEmpty()) eaten.append(" ")
                        eaten.append("$day ${w.replaceFirstChar { it.uppercase() }}")
                    }
                    break
                }
                if (dayFound) break
            }
            // gg.aa veya gg/aa (isteğe bağlı yıl)
            if (!dayFound) {
                eat(Regex("""\b(\d{1,2})[./](\d{1,2})(?:[./](\d{2,4}))?\b"""))?.let { m ->
                    val d = m.groupValues[1].toInt()
                    val mo = m.groupValues[2].toInt()
                    val yRaw = m.groupValues[3]
                    if (d in 1..31 && mo in 1..12) {
                        c.set(Calendar.DAY_OF_MONTH, d)
                        c.set(Calendar.MONTH, mo - 1)
                        if (yRaw.isNotEmpty()) {
                            val y = yRaw.toInt()
                            c.set(Calendar.YEAR, if (y < 100) 2000 + y else y)
                        } else if (c.timeInMillis < System.currentTimeMillis() - 86_400_000L) {
                            c.add(Calendar.YEAR, 1)
                        }
                        dayFound = true
                        if (eaten.isNotEmpty()) eaten.append(" ")
                        eaten.append("$d.$mo")
                    }
                }
            }
        }

        // ---- 4) Göreli gün: bugün / yarın / öbür gün ----
        if (!dayFound) {
            when {
                eat(Regex("""\byarın\b|\byarin\b"""), "Yarın") != null -> {
                    c.add(Calendar.DAY_OF_YEAR, 1); dayFound = true
                }
                eat(Regex("""\böbür\s*gün\b|\bobur\s*gun\b"""), "Öbür gün") != null -> {
                    c.add(Calendar.DAY_OF_YEAR, 2); dayFound = true
                }
                eat(Regex("""\bbugün\b|\bbugun\b"""), "Bugün") != null -> {
                    dayFound = true
                }
            }
        }

        // ---- 5a) "N saat/dakika sonra" — şimdiki zamana eklenir ----
        var relativeTime = false
        if (hour < 0 && daypartHour < 0) {
            eat(Regex("""\b(\d{1,3})\s*(saat|dakika|dk)\s*sonra\b"""))?.let { m ->
                val n = m.groupValues[1].toInt()
                val unit = m.groupValues[2]
                c.timeInMillis = System.currentTimeMillis()
                c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
                if (unit == "saat") c.add(Calendar.HOUR_OF_DAY, n)
                else c.add(Calendar.MINUTE, n)
                dayFound = true
                relativeTime = true
                if (eaten.isNotEmpty()) eaten.append(" ")
                eaten.append("$n $unit sonra")
            }
        }

        // ---- 5b) "N gün/hafta/ay sonra" ----
        if (!dayFound) {
            eat(Regex("""\b(\d{1,3})\s*(gün|gun|hafta|ay)\s*sonra\b"""))?.let { m ->
                val n = m.groupValues[1].toInt()
                val unit = m.groupValues[2]
                when {
                    unit.startsWith("g") -> c.add(Calendar.DAY_OF_YEAR, n)
                    unit == "hafta" -> c.add(Calendar.DAY_OF_YEAR, n * 7)
                    else -> c.add(Calendar.MONTH, n)
                }
                dayFound = true
                if (eaten.isNotEmpty()) eaten.append(" ")
                eaten.append("$n $unit sonra")
            }
        }

        // ---- 5c) "her gün" — günlük tekrar ----
        var everyDay = false
        if (!dayFound) {
            if (Regex("""\bher\s*(gün|gun)\b""").containsMatchIn(work)) {
                eat(Regex("""\bher\s*(gün|gun)\b"""), "Her gün")
                everyDay = true
                dayFound = true
            }
        }

        // ---- 6) Haftanın günü (+ "her" ile tekrar, "gelecek/haftaya") ----
        if (!dayFound) {
            val isEvery = Regex("""\bher\b""").containsMatchIn(work)
            val isNext = Regex("""\bgelecek\b|\bhaftaya\b|\böň?ümüzdeki\b""")
                .containsMatchIn(work)
            for ((words, dow, label) in WEEKDAYS) {
                var hit = false
                for (w in words) {
                    if (eat(Regex("""\b$w\b""")) != null) { hit = true; break }
                }
                if (!hit) continue

                // "her" ve "gelecek" kelimelerini de temizle
                eat(Regex("""\bher\b"""))
                eat(Regex("""\bgelecek\b|\bhaftaya\b"""))

                var delta = (dow - c.get(Calendar.DAY_OF_WEEK) + 7) % 7
                if (delta == 0) {
                    // Bugün o günse: saat geçtiyse haftaya at
                    val probeHour = if (hour >= 0) hour else if (daypartHour >= 0) daypartHour else 9
                    val probe = (c.clone() as Calendar).apply {
                        set(Calendar.HOUR_OF_DAY, probeHour); set(Calendar.MINUTE, minute)
                    }
                    if (probe.timeInMillis <= System.currentTimeMillis()) delta = 7
                }
                if (isNext && delta < 7) delta += 7
                c.add(Calendar.DAY_OF_YEAR, delta)
                dayFound = true
                if (isEvery) repeatDow = dow
                if (eaten.isNotEmpty()) eaten.append(" ")
                eaten.append(if (isEvery) "Her $label" else label)
                break
            }
        }

        // ---- 7) Saat ve gün birleştirme ----
        val effectiveHour = when {
            hour >= 0 -> hour
            daypartHour >= 0 -> daypartHour
            else -> -1
        }
        val hasTime = hour >= 0 || daypartHour >= 0 || relativeTime

        if (!dayFound && !hasTime) {
            return Result(original, 0L, false)
        }

        // "2 saat sonra" zaten tam zamanı kurdu — saati ezme
        if (!relativeTime) {
            if (effectiveHour >= 0) {
                c.set(Calendar.HOUR_OF_DAY, effectiveHour)
                c.set(Calendar.MINUTE, minute)
            } else {
                // Yalnızca gün verildiyse sabah 9
                c.set(Calendar.HOUR_OF_DAY, 9)
                c.set(Calendar.MINUTE, 0)
            }
        }

        // "her gün" saatsiz verilmişse yarına değil bugüne/ilk uygun ana kur
        if (everyDay && c.timeInMillis <= System.currentTimeMillis()) {
            c.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Yalnızca saat verildiyse ve o saat geçtiyse yarına at
        if (!dayFound && c.timeInMillis <= System.currentTimeMillis()) {
            c.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Geçmiş kaldıysa anlamsız — iptal
        if (c.timeInMillis <= System.currentTimeMillis() - 60_000L) {
            return Result(original, 0L, false)
        }

        // ---- 8) Kalan metni temizle ----
        // Tarih ifadesinin yanında anlamını yitiren yardımcı kelimeleri at
        var rest = work
            .replace(Regex("""\bsaat\b"""), " ")
            .replace(Regex("""\b(günü|gunu|gününde|gununde|günlerde)\b"""), " ")
            .replace(Regex("""\bbu\b"""), " ")
            .replace(Regex("""\bde\b|\bda\b|\bte\b|\bta\b"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
        // Baştaki/sondaki bağlaç ve noktalama
        rest = rest.trim(' ', ',', '.', '-', ':', ';')

        // Orijinal büyük/küçük harf düzenini korumaya çalış:
        // temizlenmiş kelimeleri orijinalden eşleştirerek al
        val restFinal = restoreCase(original, rest)

        return Result(
            text = restFinal.ifBlank { original },
            millis = c.timeInMillis,
            hasTime = hasTime,
            repeatDow = if (everyDay) REPEAT_DAILY else repeatDow,
            matched = eaten.toString()
        )
    }

    /**
     * Küçültülmüş kalan metni, orijinaldeki büyük/küçük harflerle eşler.
     * "dişçi randevusu" → orijinalde "Dişçi Randevusu" ise onu döndürür.
     */
    private fun restoreCase(original: String, lowered: String): String {
        if (lowered.isBlank()) return ""
        val keep = lowered.split(" ").filter { it.isNotBlank() }.toMutableList()
        val out = StringBuilder()
        for (word in original.split(Regex("""\s+"""))) {
            val clean = lower(word).trim(',', '.', ':', ';', '-', '\'', '’')
            val idx = keep.indexOfFirst { it == clean }
            if (idx >= 0) {
                keep.removeAt(idx)
                if (out.isNotEmpty()) out.append(" ")
                out.append(word)
            }
        }
        return out.toString().trim(' ', ',', '.', '-', ':', ';')
    }

    /** Ayrıştırılan zamanı kullanıcıya gösterilecek kısa metne çevirir. */
    fun describe(millis: Long, hasTime: Boolean, repeatDow: Int = 0): String {
        if (millis <= 0L) return ""
        val target = Calendar.getInstance().apply { timeInMillis = millis }
        val now = cal()
        val tr = Locale("tr", "TR")

        if (repeatDow == REPEAT_DAILY) {
            val time = java.text.SimpleDateFormat("HH:mm", tr).format(target.time)
            return if (hasTime) "🔁 Her gün $time" else "🔁 Her gün"
        }
        if (repeatDow > 0) {
            val name = WEEKDAYS.firstOrNull { it.second == repeatDow }?.third ?: ""
            val time = java.text.SimpleDateFormat("HH:mm", tr).format(target.time)
            return "🔁 Her $name $time"
        }

        val todayKey = now.get(Calendar.YEAR) * 1000 + now.get(Calendar.DAY_OF_YEAR)
        val targetKey = target.get(Calendar.YEAR) * 1000 + target.get(Calendar.DAY_OF_YEAR)
        val diff = targetKey - todayKey

        val timePart = if (hasTime) {
            " " + java.text.SimpleDateFormat("HH:mm", tr).format(target.time)
        } else ""

        return when (diff) {
            0 -> "⏰ Bugün$timePart"
            1 -> "⏰ Yarın$timePart"
            2 -> "⏰ Öbür gün$timePart"
            in 3..6 -> "⏰ " + java.text.SimpleDateFormat("EEEE", tr)
                .format(target.time) + timePart
            else -> "⏰ " + java.text.SimpleDateFormat("d MMM", tr)
                .format(target.time) + timePart
        }
    }
}
