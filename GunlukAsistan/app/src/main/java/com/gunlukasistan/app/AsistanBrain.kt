package com.gunlukasistan.app

import android.content.Context
import java.util.Calendar
import java.util.Locale

/**
 * Cihaz üzerinde çalışan kural tabanlı akıllı asistan.
 * Verileri analiz eder, günlük plan üretir ve sohbet içinden
 * görev/not/konu EKLEME komutlarını gerçekten çalıştırır.
 */
object AsistanBrain {

    /** Asistanın cevabı: metin + (isteğe bağlı) çalıştırılabilir eylem butonu. */
    class Reply(
        val text: String,
        val actionLabel: String? = null,
        val action: (() -> String)? = null
    )

    private fun norm(text: String): String = text.lowercase(Locale("tr", "TR"))

    fun greeting(context: Context): Reply {
        val topics = Store.loadTopics(context)
        val (streak, _) = Store.streakInfo(context)
        return Reply(
            "Merhaba! Ben senin çalışma asistanınım ✨\n\n" +
                "Şu an ${topics.size} konun var, serin $streak gün.\n\n" +
                "Bana yazarak da ekleme yapabilirsin:\n" +
                "• \"görev ekle: 20 paragraf çöz\"\n" +
                "• \"konu ekle: KPSS Tarih\"\n" +
                "• \"günlük plan yap\" → saatli plan çıkarırım 🗓️"
        )
    }

    fun reply(context: Context, input: String): Reply {
        val raw = input.trim()
        val text = norm(raw)
        return when {
            // --- Eylemli komutlar (önce bunlar) ---
            text.contains("kpss") && (text.contains("ekle") || text.contains("yükle") ||
                text.contains("aç")) -> addKpss(context)
            text.contains("görev ekle") -> addTask(context, raw, "görev ekle")
            text.contains("not ekle") -> addNote(context, raw, "not ekle")
            text.contains("konu ekle") -> addTopic(context, raw, "konu ekle")
            text.contains("madde ekle") || text.contains("alt madde ekle") ->
                addSubtopic(context, raw)

            // --- Soru sayacı ("120 soru çözdüm") ---
            Regex(".*\\d+\\s*soru.*") matches text -> addQuestionsCommand(context, text)

            // --- Plan üretimi ---
            text.contains("günlük plan") || text.contains("günün planı") ||
                text.contains("bugünün planı") || text.contains("plan yap") ||
                text.contains("plan oluştur") || (text.contains("plan") &&
                    !text.contains("haftalık")) -> dailyPlan(context)
            text.contains("haftalık") || text.contains("hafta planı") || text.contains("program") ->
                Reply(weeklyPlan(context))

            // --- Bilgi ve analiz ---
            text.contains("değerlendir") || text.contains("analiz") || text.contains("durum") ->
                Reply(evaluate(context))
            text.contains("nasıl eklerim") || text.contains("komut") ||
                text.contains("ne yazabilirim") -> Reply(howToAdd())
            text.contains("kpss") -> Reply(kpssInfo())
            text.contains("deneme") && (text.contains("ekle") || text.contains("gir")) ->
                Reply("Deneme neti eklemek için Denemelerim ekranını kullan 📊\n\n" +
                    "Ana Sayfa → \"📊 Denemeler\" kartı → + menüsünden \"Deneme sonucu ekle\". " +
                    "Gelişim grafiğin orada çizilir!")
            text.contains("deneme") || text.contains("net") -> Reply(denemeSummary(context))
            text.contains("tüyo") || text.contains("tavsiye") || text.contains("teknik") ||
                text.contains("nasıl çalış") || text.contains("verim") -> Reply(studyTips(context))
            text.contains("motivas") || text.contains("isteksiz") || text.contains("sıkıld") ||
                text.contains("bırak") || text.contains("yorgun") -> Reply(motivate(context))
            text.contains("pomodoro") -> Reply(pomodoroInfo())
            text.contains("tekrar") || text.contains("unut") -> Reply(repetitionAdvice())
            text.contains("uyku") -> Reply(sleepAdvice())
            text.contains("sınav") -> Reply(examAdvice(context))
            text.contains("telefon") || text.contains("dikkat") || text.contains("odaklanam") ->
                Reply(focusAdvice())
            text.contains("merhaba") || text.contains("selam") || text.contains("naber") ||
                text.contains("nasılsın") -> Reply(
                "Selam! 👋 Ben süperim — senin ilerlemeni takip etmeye her zaman hazırım.\n\n" +
                    "\"durumumu değerlendir\" ya da \"günlük plan yap\" de, birlikte bakalım. 💪"
            )
            text.contains("teşekkür") || text.contains("sağol") || text.contains("eyvallah") ->
                Reply("Rica ederim! 🌟 Yardımcı olabildiysem ne mutlu. Başarılar!")
            else -> Reply(fallback())
        }
    }

    // ---------------- Ekleme komutları ----------------

    private fun afterKeyword(raw: String, keyword: String): String {
        val i = norm(raw).indexOf(keyword)
        if (i < 0) return ""
        return raw.substring(i + keyword.length)
            .trimStart(':', '–', '-', ' ')
            .trim()
    }

    private fun capitalize(s: String): String =
        if (s.isEmpty()) s else s.substring(0, 1)
            .uppercase(Locale("tr", "TR")) + s.substring(1)

    private fun addTask(context: Context, raw: String, keyword: String): Reply {
        val content = afterKeyword(raw, keyword)
        if (content.isEmpty()) {
            return Reply(
                "Ne ekleyeceğimi yazmadın 😊 Örnek:\n\n" +
                    "• görev ekle: 20 paragraf çöz\n" +
                    "• görev ekle: türev tekrarı\n\n" +
                    "İpucu: tarih/saat ve alarmı Görevler ekranından ekleyebilirsin ⏰"
            )
        }
        val tasks = Store.loadTasks(context)
        tasks.add(
            Store.Task(
                id = System.currentTimeMillis(),
                text = capitalize(content),
                done = false,
                createdAt = System.currentTimeMillis()
            )
        )
        Store.saveTasks(context, tasks)
        return Reply("✅ \"${capitalize(content)}\" görevlere eklendi!\n\n" +
            "Tarih/saat + alarm eklemek istersen Görevler ekranındaki + ile de ekleyebilirsin.")
    }

    private fun addNote(context: Context, raw: String, keyword: String): Reply {
        val content = afterKeyword(raw, keyword)
        if (content.isEmpty()) {
            return Reply("Not başlığını yazmadın 😊 Örnek:\n\nnot ekle: Türev formülleri")
        }
        val notes = Store.loadNotes(context)
        notes.add(
            Store.Note(
                id = System.currentTimeMillis(),
                title = capitalize(content),
                content = "",
                createdAt = System.currentTimeMillis()
            )
        )
        Store.saveNotes(context, notes)
        return Reply("📝 \"${capitalize(content)}\" notlara eklendi! İçini Notlarım ekranında doldurabilirsin.")
    }

    private fun addTopic(context: Context, raw: String, keyword: String): Reply {
        val content = afterKeyword(raw, keyword)
        if (content.isEmpty()) {
            return Reply("Konu adını yazmadın 😊 Örnek:\n\nkonu ekle: KPSS Coğrafya")
        }
        val topics = Store.loadTopics(context)
        topics.add(
            Store.Topic(
                id = System.currentTimeMillis(),
                title = capitalize(content),
                createdAt = System.currentTimeMillis(),
                items = mutableListOf()
            )
        )
        Store.saveTopics(context, topics)
        return Reply("📚 \"${capitalize(content)}\" konulara eklendi!\n\n" +
            "Alt madde eklemek için: \"madde ekle $content: madde adı\" yaz ya da Konular ekranını kullan.")
    }

    private fun addSubtopic(context: Context, raw: String): Reply {
        // Format: "madde ekle <konu adı>: <madde>" veya "matematik konusuna madde ekle: türev"
        val text = norm(raw)
        val after = afterKeyword(raw, "madde ekle")
        val sepIndex = after.indexOf(':')
        if (sepIndex <= 0) {
            return Reply(
                "Alt madde için şöyle yaz 😊\n\nmadde ekle KONU ADI: madde\n\nÖrnek:\nmadde ekle KPSS Tarih: Lozan Antlaşması"
            )
        }
        val topicPart = norm(after.substring(0, sepIndex)).replace("konusuna", "").trim()
        val subText = after.substring(sepIndex + 1).trim()
        if (subText.isEmpty()) return Reply("Madde metnini yazmadın 😊 ':' sonrasına yaz.")
        val topics = Store.loadTopics(context)
        val topic = topics.firstOrNull { norm(it.title).contains(topicPart) }
            ?: return Reply("❓ \"$topicPart\" adında bir konu bulamadım.\n\nKonuların: " +
                topics.take(5).joinToString(", ") { it.title })
        topic.items.add(
            Store.SubItem(
                id = System.currentTimeMillis(),
                text = capitalize(subText),
                done = false,
                createdAt = System.currentTimeMillis()
            )
        )
        Store.saveTopics(context, topics)
        return Reply("✅ \"${topic.title}\" konusuna \"${capitalize(subText)}\" eklendi!")
    }

    private fun addQuestionsCommand(context: Context, text: String): Reply {
        val number = Regex("\\d+").find(text)?.value?.toIntOrNull()
            ?: return Reply("Kaç soru olduğunu anlayamadım 😊 Örnek: \"bugün 120 soru çözdüm\"")
        Store.addQuestions(context, number)
        val today = Store.getTodayQuestions(context)
        return Reply("🔢 $number soru işlendi! Bugünkü toplamın: $today soru.\n\n" +
            "Isı haritana ve rozetlerine de yansıdı — böyle devam! 💪")
    }

    private fun addKpss(context: Context): Reply {
        val added = KpssPack.addMissing(context)
        return if (added > 0) {
            Reply("🎓 KPSS müfredatı eklendi!\n\n$added ders kartı ve " +
                "${KpssPack.subtopicCount()} alt konu Konular ekranında seni bekliyor. " +
                "İlerledikçe maddeleri işaretle, yüzdenin arttığını izle! 📈\n\n" +
                "Tarihe not: 2026 GY-GK sınavı Eylül başında — düzenli tempo şart! 💪")
        } else {
            Reply("KPSS müfredatı zaten Konular ekranında duruyor 😊\n\n" +
                "Yeni ders eklememi istersen önce o kartı silersen yeniden ekleyebilirim.")
        }
    }

    // ---------------- Günlük plan ----------------

    private data class PlanRow(val millis: Long, val label: String)

    private fun dailyPlan(context: Context): Reply {
        val topics = Store.loadTopics(context)
            .filter { it.items.isNotEmpty() }
            .sortedBy { it.percent }
        if (topics.isEmpty()) {
            return Reply(
                "Plan çıkarabilmem için önce birkaç konu ve alt maddeye ihtiyacım var 📚\n\n" +
                    "• \"kpss ekle\" → hazır müfredatı saniyeler içinde yükle, ya da\n" +
                    "• \"konu ekle: ders adı\" ile kendi konunu aç."
            )
        }

        // Bugünün kalanına plan: şu andan sonraki tam saatten başla
        val cal = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val slotHours = intArrayOf(0, 1, 3, 5, 7) // başlangıçtan ofsetler
        val rows = mutableListOf<PlanRow>()
        val sb = StringBuilder("🗓️ Bugün için planın hazır!\n\n")
        val fmt = java.text.SimpleDateFormat("HH:mm", Locale.US)

        fun slotText(offset: Int): Long =
            cal.timeInMillis + offset * 60L * 60L * 1000L

        val coreTopics = topics.take(2)
        rows.add(PlanRow(slotText(slotHours[0]), "Hafif tekrarla güne dön (10 dk okuma)"))
        coreTopics.forEachIndexed { i, t ->
            val pending = t.items.filter { !it.done }.take(2)
            val names = if (pending.isEmpty()) "genel tekrar" else
                pending.joinToString(" + ") { it.text.take(28) }
            rows.add(
                PlanRow(
                    slotText(slotHours[i + 1]),
                    "${t.title}: $names (2×25 dk Pomodoro)"
                )
            )
        }
        rows.add(PlanRow(slotText(slotHours[3]), "✍️ 20 soru çözümü + yanlışlarını nota geçir"))
        rows.add(PlanRow(slotText(slotHours[4]), "🔁 Günün tekrarı (aralıklı tekrar 1. gün)"))

        rows.forEach { r -> sb.append("⏰ ${fmt.format(r.millis)}  —  ${r.label}\n") }
        sb.append(
            "\nToplam açık madden: ${topics.sumOf { t -> t.items.count { !it.done } }} — " +
                "bu planla bugün sağlam ilerlersin! 🚀"
        )

        return Reply(
            sb.toString(),
            actionLabel = "✅ Planı görevlere ekle (saat + alarm)",
            action = {
                val tasks = Store.loadTasks(context)
                var added = 0
                val now = System.currentTimeMillis()
                rows.forEachIndexed { i, r ->
                    val task = Store.Task(
                        id = now + i + 1,
                        text = "🗓️ ${fmt.format(r.millis)} — ${r.label}",
                        done = false,
                        createdAt = now,
                        dueAt = r.millis
                    )
                    tasks.add(task)
                    AlarmScheduler.schedule(context, task.id, r.label, r.millis)
                    added++
                }
                Store.saveTasks(context, tasks)
                "✅ $added plan maddesi görevlere eklendi!\n\n" +
                    "Saati gelenler için hatırlatma da kuruldu ⏰ Görevler ekranından takip edebilirsin."
            }
        )
    }

    // ---------------- Bilgi cevapları ----------------

    private fun denemeSummary(context: Context): String {
        val exams = Store.loadExams(context)
        if (exams.isEmpty()) {
            return "Henüz deneme sonucu girmedin 📊\n\n" +
                "Ana Sayfa'daki \"📊 Denemeler\" kartından ilk netini ekle, " +
                "her denemede gelişim grafiğini izleyelim!"
        }
        val avg = exams.map { it.totalNet }.average().toInt()
        val best = exams.maxOf { it.totalNet }
        val last = exams.maxByOrNull { it.createdAt }?.totalNet ?: 0
        return "📊 Deneme durumun:\n\n" +
            "• Toplam deneme: ${exams.size}\n" +
            "• Ortalama net: $avg\n• En iyi: $best\n• Son deneme: $last\n\n" +
            "Ayrıntılı grafik için Ana Sayfa → 📊 Denemeler."
    }

    private fun howToAdd(): String {
        return "➕ Sohbetten ekleme komutlarım:\n\n" +
            "• görev ekle: paragraf 20 soru\n" +
            "• not ekle: türev formülleri\n" +
            "• konu ekle: KPSS Tarih\n" +
            "• madde ekle KONU: madde adı\n" +
            "• kpss ekle → 2026 müfredatı (7 ders, ${KpssPack.subtopicCount()} alt konu)\n" +
            "• günlük plan yap → saatli plan + görevlere aktarma 🗓️\n\n" +
            "Tarih/saat ve alarm istersen Görevler ekranındaki + butonunu kullan ⏰"
    }

    private fun kpssInfo(): String {
        return "🎓 2026 KPSS GY-GK özeti:\n\n" +
            "• Toplam 120 soru / 130 dakika\n" +
            "• Türkçe 30 · Matematik 30 (GY)\n" +
            "• Tarih 27 · Coğrafya 18 · Vatandaşlık 9 · Güncel 6 (GK)\n\n" +
            "En çok soru getirenler: Paragraf, Problemler, İnkılap Tarihi, Ekonomik Coğrafya.\n\n" +
            "💡 \"kpss ekle\" yazarsan tüm müfredatı alt konularıyla Konular'a yüklerim!"
    }

    private fun dataSummary(context: Context): Triple<Int, Int, Int> {
        val topics = Store.loadTopics(context)
        val allSubs = topics.flatMap { it.items }
        val done = allSubs.count { it.done }
        val percent = if (allSubs.isEmpty()) 0 else done * 100 / allSubs.size
        return Triple(allSubs.size, done, percent)
    }

    private fun weakestTopic(context: Context): Store.Topic? {
        return Store.loadTopics(context)
            .filter { it.items.isNotEmpty() }
            .minByOrNull { it.percent }
    }

    private fun evaluate(context: Context): String {
        val (total, done, percent) = dataSummary(context)
        val (streak, best) = Store.streakInfo(context)
        val now = Calendar.getInstance()
        val monthFocus = Store.monthFocus(context, now.get(Calendar.YEAR), now.get(Calendar.MONTH))
        val weakest = weakestTopic(context)

        val sb = StringBuilder("📊 İşte durum analizin:\n\n")
        sb.append("• Genel ilerleme: %$percent ($done/$total madde)\n")
        sb.append("• Seri: $streak gün (rekor: $best)\n")
        sb.append("• Bu ay odaklanma: $monthFocus dk\n\n")

        if (total == 0) {
            sb.append("Henüz alt madde yok. İstersen \"kpss ekle\" ile hazır müfredatı yükle, " +
                "istersen \"konu ekle: ders adı\" ile kendi konunu aç! 🔑")
        } else {
            if (weakest != null && weakest.percent < 50) {
                sb.append("🎯 Öncelik önerim: \"${weakest.title}\" şu an %${weakest.percent}'te. " +
                    "Bugünkü 25 dakikalık ilk Pomodoro'nu bu konuya ayır bence.\n\n")
            }
            when {
                percent >= 80 -> sb.append("Harika gidiyorsun! Bitirme moduna geç: " +
                    "kalan maddeleri bu hafta kapatmayı hedefle. 🏁")
                percent >= 50 -> sb.append("Yolun yarısını geçtin! Momentum kaybetmeden " +
                    "günde en az 1 odak seansı koruyalım. 🔥")
                else -> sb.append("Daha yolun başındasın — bu iyi haber! Her gün 25 dakikalık " +
                    "tek seans, 2 haftada büyük fark yaratır. İstersen \"günlük plan yap\" de! 🌱")
            }
        }
        return sb.toString()
    }

    private fun weeklyPlan(context: Context): String {
        val topics = Store.loadTopics(context).filter { it.items.isNotEmpty() }
        if (topics.isEmpty()) {
            return "Önce birkaç konu ve alt madde ekle, sonra sana kişiye özel bir " +
                "haftalık plan çıkarayım! 📚\n\n(\"kpss ekle\" deyince hazır müfredat gelir.)"
        }
        val sorted = topics.sortedBy { it.percent }
        val days = listOf("Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar")
        val sb = StringBuilder("📅 Bu hafta için plan önerin:\n\n")

        val remaining = sorted.sumOf { it.items.count { s -> !s.done } }
        sorted.take(5).forEachIndexed { i, topic ->
            val day = days[i % 7]
            val pending = topic.items.filter { !it.done }.take(3)
            val pendingText = pending.joinToString(", ") { it.text.take(22) }
            sb.append("• $day: ${topic.title} (%${topic.percent})")
            if (pending.isNotEmpty()) sb.append("\n  → $pendingText")
            sb.append("\n")
        }
        sb.append("\n💡 Her gün: 2×25 dk odak + 5 dk mola. " +
            "Toplam $remaining açık madden var — istersen \"günlük plan yap\" de, bugünü saatlere böleyim! 🚀")
        return sb.toString()
    }

    private fun studyTips(context: Context): String {
        val (_, _, percent) = dataSummary(context)
        return "💡 Kanıtlanmış 4 teknik:\n\n" +
            "1️⃣ Pomodoro: 25 dk tam odak + 5 dk mola. Sayaç ekranında hazır!\n" +
            "2️⃣ Aralıklı tekrar: Bugün öğrendiğini 1-3-7 gün sonra tekrar et.\n" +
            "3️⃣ Aktif hatırlama: Nota bakmadan kendine soru sor — en güçlü yöntem bu.\n" +
            "4️⃣ Küçük parçalar: Her seansta 1-2 alt madde bitir.\n\n" +
            "Şu an genel ilerlemen %$percent. Bugünün hedefi: tek bir alt madde daha ✅"
    }

    private fun motivate(context: Context): String {
        val (streak, best) = Store.streakInfo(context)
        return "Seni anlıyorum — herkes bazen böyle hisseder. 🌿\n\n" +
            "Ama şunu unutma: $streak günlük bir seri kurdun (rekorun $best gün!). " +
            "Bu, istikrarın kanıtı.\n\n" +
            "Bugün sadece 10 dakika çalış. 'Başlamak' motivasyonu getirir, " +
            "motivasyonu bekleme. Küçük adım, büyük alışkanlık. 💪🔥"
    }

    private fun pomodoroInfo(): String {
        return "🍅 Pomodoro tekniği:\n\n" +
            "• 25 dk kesintisiz çalış (telefon uzağa!)\n" +
            "• 5 dk mola ver\n" +
            "• 4 turdan sonra 15-20 dk uzun mola\n\n" +
            "Sayaç ekranında hazır 25/5/15 butonları var; odak seslerini açarsan " +
            "çalarken ekolayzır animasyonu da gösteririm! 🎧"
    }

    private fun repetitionAdvice(): String {
        return "🔁 Unutma eğrisini yenmek için:\n\n" +
            "• Aynı gün: öğrendiğin maddeleri hemen gözden geçir\n" +
            "• Ertesi gün: notlara bakmadan hatırlamaya çalış\n" +
            "• 3. ve 7. gün: kısa tekrar turları\n\n" +
            "Görevlere tarih+saat eklersen tekrar günlerini sana hatırlatırım! ⏰"
    }

    private fun sleepAdvice(): String {
        return "😴 Uyku = hafıza. Öğrendiklerin uyurken pekişir:\n\n" +
            "• Sınav dönemi bile olsa 7-8 saatten kısma\n" +
            "• Yatmadan 30 dk önce son tekrar → uykuda işlenir\n" +
            "• Gece 01:00 sonrası çalışma verimi ciddi düşer\n\n" +
            "Bu gece için öneri: 23:00'e 15 dk'lık hafif bir tekrar koy, sonra uyku. 🌙"
    }

    private fun examAdvice(context: Context): String {
        val weakest = weakestTopic(context)
        val sb = StringBuilder("📝 Sınav stratejisi:\n\n")
        if (weakest != null) {
            sb.append("En kırılgan noktan: \"${weakest.title}\" (%${weakest.percent}) — önce burayı kapat. ")
        }
        sb.append("Güçlü konulara harcanan her saat, az bilinenden daha az puan getirir.\n\n" +
            "• Deneme sınavlarını gerçek saatlerde çöz (saat biyolojisi!)\n" +
            "• Yanlışlarını konuya dönüştür: yanlış → alt madde → tekrar\n" +
            "• Sınav sabahı yeni konu YOK, sadece hafif göz gezdirme. 🧘")
        return sb.toString()
    }

    private fun focusAdvice(): String {
        return "📵 Odaklanamıyorsan deneyelim:\n\n" +
            "• Telefonu başka odaya koy (ya da uçak modu)\n" +
            "• Masada sadece o anki konuya ait şeyler olsun\n" +
            "• 25 dk'lık sayacı başlat — 'sadece 25 dk' beynin itirazını azaltır\n" +
            "• Dikkatin dağılırsa 'sonra bakacağım' diye nota yaz, devam et\n\n" +
            "Doğa sesleri de yardımcı olabilir: Sayaç → Odak sesleri 🌧️"
    }

    private fun fallback(): String {
        return "Bunu tam anlayamadım ama şunlarda iyiyim: 😊\n\n" +
            "• \"durumumu değerlendir\" → kişisel analiz\n" +
            "• \"günlük plan yap\" → saatli plan + görev aktarımı 🗓️\n" +
            "• \"görev ekle: ...\" / \"konu ekle: ...\" → sohbetten ekleme ➕\n" +
            "• \"kpss ekle\" → hazır müfredat 🎓\n" +
            "• \"motivasyon ver\" → biraz gaz 🔥"
    }
}
