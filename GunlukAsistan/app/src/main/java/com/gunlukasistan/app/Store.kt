package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Basit yerel depolama katmanı.
 * Tüm veriler telefonda SharedPreferences içinde JSON olarak saklanır.
 * Ayrıca günlük aktivite kaydı tutulur (seri ve ısı haritası için).
 */
object Store {

    private const val TAG = "Store"
    private const val PREF_NAME = "gunluk_asistan_store"
    private const val KEY_NOTES = "notes_json"
    private const val KEY_TASKS = "tasks_json"
    private const val KEY_TOPICS = "topics_json"
    private const val KEY_DAILY_LOG = "daily_log_json"
    private const val KEY_SEEDED = "seeded_v1"
    private const val KEY_SEEDED_V2 = "seeded_v2"

    // ---------------- Veri modelleri ----------------

    data class Note(
        val id: Long,
        var title: String,
        var content: String,
        val createdAt: Long,
        var image: String = ""
    )

    /**
     * Görev.
     *
     * v7.70: tekrar alanları eklendi. Eski yedeklerde bu alanlar yok —
     * okurken varsayılan ("yok" / 0) atanır, geriye dönük uyumlu.
     */
    data class Task(
        val id: Long,
        var text: String,
        var done: Boolean,
        val createdAt: Long,
        var dueAt: Long = 0L,
        /** v7.70: tekrar kodu — bkz. [Tekrar]. "yok" = tekrarsız. */
        var tekrar: String = Tekrar.YOK,
        /** v7.70: tekrarın biteceği tarih (ms). 0 = süresiz. */
        var tekrarBitis: Long = 0L,
        /** v7.70: kaç kez tamamlandı (tekrarlı görevlerde artar). */
        var yapildi: Int = 0,
        /** v7.73: alt adımlar (checklist). Boşsa basit görev. */
        val adimlar: MutableList<SubItem> = mutableListOf(),
        /** v7.74: etiket kodu — bkz. [Etiket]. Boş = etiketsiz. */
        var etiket: String = Etiket.YOK,
        /**
         * v7.75: arşive taşındı mı?
         *
         * Arşivlenen görev ana listede görünmez ama silinmez —
         * istatistikler ve "bu ay kaç iş bitirdin" bilgisi korunur.
         */
        var arsiv: Boolean = false,
        /** v7.75: arşive taşınma zamanı (ms). */
        var arsivZaman: Long = 0L
    ) {
        val tekrarliMi: Boolean get() = Tekrar.aktifMi(tekrar)

        /** v7.73: kaç adım bitti. */
        val bitenAdim: Int get() = adimlar.count { it.done }
        val adimYuzde: Int
            get() = if (adimlar.isEmpty()) 0 else bitenAdim * 100 / adimlar.size
    }

    /** KPSS deneme sonucu: ders -> net sayısı. */
    data class Exam(
        val id: Long,
        var title: String,
        val createdAt: Long,
        val nets: LinkedHashMap<String, Int>
    ) {
        val totalNet: Int get() = nets.values.sum()
    }

    val EXAM_SUBJECTS = listOf(
        "Türkçe", "Matematik", "Tarih", "Coğrafya", "Vatandaşlık", "Güncel"
    )

    /**
     * Kişisel geri sayım etkinliği (v5.2).
     * dateKey "yyyyMMdd" biçiminde tutulur, böylece saat dilimi sorunları yaşanmaz.
     */
    data class DayEvent(
        val id: Long,
        var title: String,
        var dateKey: String,
        var emoji: String = "🎯",
        var pinned: Boolean = false,
        val createdAt: Long
    ) {
        /** Etkinliğin gece yarısı zaman damgası (ms). Hatalıysa 0. */
        val millis: Long
            get() = try {
                SimpleDateFormat("yyyyMMdd", Locale.US).parse(dateKey)?.time ?: 0L
            } catch (_: Exception) {
                0L
            }

        /** Bugünden kaç gün kaldı? Bugün ise 0, geçmişse negatif. */
        val daysLeft: Int
            get() {
                val target = Calendar.getInstance().apply {
                    timeInMillis = millis
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                return ((target.timeInMillis - today.timeInMillis) / 86_400_000L).toInt()
            }

        val isPast: Boolean get() = daysLeft < 0
    }

    /** Etkinlik eklerken sunulan hazır simgeler. */
    val EVENT_EMOJIS = listOf(
        "🎯", "📝", "🎓", "💍", "🎂", "✈️", "🏖️", "🎉",
        "❤️", "🏥", "🚗", "🏠", "💼", "⚽", "🎵", "📅"
    )

    /**
     * Günlük alışkanlık (v5.4).
     * Hedef sayaç mantığı: günde [target] kez işaretlenir (su için 8, spor için 1 gibi).
     * Günlük ilerleme ayrı bir JSON'da tutulur: { "20260728": { "habitId": 3 } }
     */
    data class Habit(
        val id: Long,
        var title: String,
        var emoji: String = "💧",
        var target: Int = 1,
        var colorIndex: Int = 0,
        var archived: Boolean = false,
        val createdAt: Long
    )

    /** Alışkanlık eklerken sunulan hazır simgeler. */
    val HABIT_EMOJIS = listOf(
        "💧", "🏃", "📖", "💊", "🧘", "🥗", "😴", "🚭",
        "🦷", "🏋️", "🎹", "✍️", "🙏", "🚶", "🍎", "☀️"
    )

    /** Alışkanlık kartlarının vurgu renkleri (ThemeManager'dan bağımsız sabit palet). */
    val HABIT_COLORS = listOf(
        "#7C9070", "#B08968", "#C97C5D", "#7A8FA6", "#8E7CC3", "#1489A6"
    )

    /**
     * Kitaplığa eklenen PDF/dosya kaydı (v6.6).
     * [uri] SAF kalıcı izinli içerik adresidir; dosya kopyalanmaz.
     */
    data class Book(
        val id: Long,
        var title: String,
        val uri: String,
        var pages: Int = 0,
        var lastPage: Int = 0,
        var color: Int = 0,
        val addedAt: Long,
        /** Bölüm ise ana kitabın kimliği, değilse 0. */
        var parentId: Long = 0L,
        /** Bölüm sırası (1'den başlar), ana kitapta 0. */
        var chapterNo: Int = 0,
        /** Bu kitabın kaç bölüme ayrıldığı (ana kitapta dolu). */
        var chapterCount: Int = 0
    ) {
        val isChapter: Boolean get() = parentId != 0L

        val progress: Int
            get() = if (pages > 0) (lastPage * 100 / pages).coerceIn(0, 100) else 0
    }


    // ═══════════════════════════════════════════════════════════════════
    // v7.4 — Mühendislik kursları (Kurs → Bölüm → Ders)
    // ═══════════════════════════════════════════════════════════════════

    /** Bir kurs (ör. "AutoCAD 2D"). */
    data class Course(
        val id: Long,
        var title: String,
        var emoji: String,
        var color: Int,
        var desc: String,
        val createdAt: Long,
        var order: Int = 0
    )

    /** Kurs içindeki bölüm (ör. "1. Tanışma ve Arayüz"). */
    data class Section(
        val id: Long,
        val courseId: Long,
        var title: String,
        var order: Int = 0
    )

    /** Bölüm içindeki tek ders. */
    data class Lesson(
        val id: Long,
        val courseId: Long,
        val sectionId: Long,
        var title: String,
        var minutes: Int = 0,
        var desc: String = "",
        var link: String = "",
        var note: String = "",
        var done: Boolean = false,
        var order: Int = 0,
        /** v7.5: assets içindeki ders PDF yolu (boşsa PDF yok). */
        var pdfAsset: String = "",
        /** v7.8: kullanıcı bu dersi yer imine ekledi mi. */
        var fav: Boolean = false
    )

    data class SubItem(val id: Long, var text: String, var done: Boolean, val createdAt: Long)

    data class Topic(
        val id: Long,
        var title: String,
        val createdAt: Long,
        val items: MutableList<SubItem>
    ) {
        val doneCount: Int get() = items.count { it.done }
        val percent: Int get() = if (items.isEmpty()) 0 else doneCount * 100 / items.size
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private fun now() = System.currentTimeMillis()

    private fun dayKey(date: Date = Date()): String =
        SimpleDateFormat("yyyyMMdd", Locale.US).format(date)

    private fun monthKeyOf(year: Int, monthZeroBased: Int): String =
        String.format(Locale.US, "%04d%02d", year, monthZeroBased + 1)

    // ---------------- İlk açılış örnek içeriği ----------------

    private fun seedNotesTasksIfNeeded(context: Context) {
        val p = prefs(context)
        if (p.getBoolean(KEY_SEEDED, false)) return

        val notes = JSONArray().put(
            JSONObject()
                .put("id", now())
                .put("title", context.getString(R.string.welcome_note_title))
                .put("content", context.getString(R.string.welcome_note_content))
                .put("createdAt", now())
        )
        val tasks = JSONArray().put(
            JSONObject()
                .put("id", now())
                .put("text", context.getString(R.string.welcome_task))
                .put("done", false)
                .put("createdAt", now())
        )
        p.edit()
            .putString(KEY_NOTES, notes.toString())
            .putString(KEY_TASKS, tasks.toString())
            .putBoolean(KEY_SEEDED, true)
            .apply()
        // v8.9: örnek veri yazıldı — önbellek boş olmalı ama garanti
        Onbellek.boz(Onbellek.K_TASKS, Onbellek.K_NOTES)
    }

    private fun seedTopicsIfNeeded(context: Context) {
        val p = prefs(context)
        if (p.getBoolean(KEY_SEEDED_V2, false)) return

        p.edit()
            .putString(KEY_TOPICS, "[]")
            .putBoolean(KEY_SEEDED_V2, true)
            .apply()
        Onbellek.boz(Onbellek.K_TOPICS)
    }

    // ---------------- Konular ----------------

    /**
     * v8.9 · Öneri 15 — Önbellekli okuma.
     *
     * ── Ölçüm ──
     * Bu fonksiyon 32 yerde çağrılıyor. Her çağrı diskten metin
     * okuyup tüm JSON'u ayrıştırıyordu; 200 maddelik bir listede
     * her seferinde yüzlerce nesne yaratılıyordu.
     *
     * ── 🔴 Neden DERİN kopya ──
     * `Topic.items` bir `MutableList` ve `SubItem.done` bir `var`.
     * Çağıranlar bunları doğrudan değiştiriyor
     * (`sub.done = true; saveTopics(...)`). Önbellekten aynı nesneyi
     * dönseydik, bir ekrandaki değişiklik henüz kaydedilmeden diğer
     * ekranda görünürdü — kaydetme başarısız olsa bile. Daha kötüsü:
     * kullanıcı "iptal" dese bile değişiklik bellekte kalırdı.
     *
     * Derin kopya bunu engelliyor. JSON ayrıştırma (pahalı) bir kez,
     * kopyalama (ucuz) her çağrıda.
     */
    fun loadTopics(context: Context): MutableList<Topic> {
        seedTopicsIfNeeded(context)
        return Onbellek.al(
            Onbellek.K_TOPICS,
            kopyala = { liste: MutableList<Topic> ->
                liste.mapTo(mutableListOf()) { t ->
                    t.copy(items = t.items.mapTo(mutableListOf()) { it.copy() })
                }
            },
            uret = { loadTopicsDisk(context) }
        )
    }

    private fun loadTopicsDisk(context: Context): MutableList<Topic> {
        val json = prefs(context).getString(KEY_TOPICS, "[]") ?: "[]"
        val array = JSONArray(json)
        val list = mutableListOf<Topic>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            val itemsArray = o.optJSONArray("items") ?: JSONArray()
            val items = mutableListOf<SubItem>()
            for (j in 0 until itemsArray.length()) {
                val s = itemsArray.getJSONObject(j)
                items.add(
                    SubItem(
                        id = s.optLong("id", now()),
                        text = s.optString("text", ""),
                        done = s.optBoolean("done", false),
                        createdAt = s.optLong("createdAt", now())
                    )
                )
            }
            list.add(
                Topic(
                    id = o.optLong("id", now()),
                    title = o.optString("title", ""),
                    createdAt = o.optLong("createdAt", now()),
                    items = items
                )
            )
        }
        return list
    }

    fun saveTopics(context: Context, topics: List<Topic>) {
        val array = JSONArray()
        topics.forEach { topic ->
            val items = JSONArray()
            topic.items.forEach { subItem ->
                items.put(
                    JSONObject()
                        .put("id", subItem.id)
                        .put("text", subItem.text)
                        .put("done", subItem.done)
                        .put("createdAt", subItem.createdAt)
                )
            }
            array.put(
                JSONObject()
                    .put("id", topic.id)
                    .put("title", topic.title)
                    .put("createdAt", topic.createdAt)
                    .put("items", items)
            )
        }
        prefs(context).edit().putString(KEY_TOPICS, array.toString()).apply()
        Onbellek.boz(Onbellek.K_TOPICS)
        widgetTazele(context)
        maybeAutoBackup(context)
    }

    // ---------------- Notlar ----------------

    fun loadNotes(context: Context): MutableList<Note> {
        seedNotesTasksIfNeeded(context)
        // v8.1: Room köprüsü (öneri 3). İmza değişmedi; çağıran dosyaların
        // hiçbiri düzenlenmedi. Room hata verirse otomatik JSON'a düşülür.
        return com.gunlukasistan.app.veri.NotDepo.oku(context) { loadNotesJson(context) }
    }

    /** v8.1: Eski JSON okuma yolu — yedek olarak korundu. */
    private fun loadNotesJson(context: Context): MutableList<Note> {
        val json = prefs(context).getString(KEY_NOTES, "[]") ?: "[]"
        val array = JSONArray(json)
        val list = mutableListOf<Note>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            list.add(
                Note(
                    id = o.optLong("id", now()),
                    title = o.optString("title", ""),
                    content = o.optString("content", ""),
                    createdAt = o.optLong("createdAt", now()),
                    image = o.optString("image", "")
                )
            )
        }
        return list
    }

    fun saveNotes(context: Context, notes: List<Note>) {
        // v8.1: Room'a yaz, JSON'u gölge kopya olarak güncel tut.
        // Yedekleme JSON'u okuduğu için exportJson aynen çalışır.
        com.gunlukasistan.app.veri.NotDepo.yaz(context, notes)
        saveNotesJson(context, notes)
    }

    /** v8.1: JSON gölge kopyasını yazar. */
    private fun saveNotesJson(context: Context, notes: List<Note>) {
        val array = JSONArray()
        notes.forEach { note ->
            array.put(
                JSONObject()
                    .put("id", note.id)
                    .put("title", note.title)
                    .put("content", note.content)
                    .put("createdAt", note.createdAt)
                    .put("image", note.image)
            )
        }
        prefs(context).edit().putString(KEY_NOTES, array.toString()).apply()
        widgetTazele(context)
        maybeAutoBackup(context)
    }

    // ---------------- Görevler ----------------

    /**
     * v7.76: Gorevler artik Room'dan okunuyor.
     *
     * Imza degismedi — cagiran 76 dosyanin hicbiri duzenlenmedi.
     * Room kullanilamazsa otomatik olarak eski JSON yoluna dusuluyor.
     */
    fun loadTasks(context: Context): MutableList<Task> {
        seedNotesTasksIfNeeded(context)
        return com.gunlukasistan.app.veri.GorevDepo.oku(context) { loadTasksJson(context) }
    }

    /** v7.76: Eski JSON okuma yolu — yedek olarak korundu. */
    private fun loadTasksJson(context: Context): MutableList<Task> {
        val json = prefs(context).getString(KEY_TASKS, "[]") ?: "[]"
        val array = JSONArray(json)
        val list = mutableListOf<Task>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            list.add(
                Task(
                    id = o.optLong("id", now()),
                    text = o.optString("text", ""),
                    done = o.optBoolean("done", false),
                    createdAt = o.optLong("createdAt", now()),
                    dueAt = o.optLong("dueAt", 0L),
                    tekrar = o.optString("tekrar", Tekrar.YOK),
                    tekrarBitis = o.optLong("tekrarBitis", 0L),
                    yapildi = o.optInt("yapildi", 0),
                    etiket = o.optString("etiket", Etiket.YOK),
                    arsiv = o.optBoolean("arsiv", false),
                    arsivZaman = o.optLong("arsivZaman", 0L),
                    adimlar = run {
                        // v7.73: alt adımlar — eski kayıtlarda yok
                        val liste = mutableListOf<SubItem>()
                        o.optJSONArray("adimlar")?.let { d ->
                            for (j in 0 until d.length()) {
                                val a2 = d.optJSONObject(j) ?: continue
                                liste.add(
                                    SubItem(
                                        id = a2.optLong("id", now()),
                                        text = a2.optString("text", ""),
                                        done = a2.optBoolean("done", false),
                                        createdAt = a2.optLong("createdAt", now())
                                    )
                                )
                            }
                        }
                        liste
                    }
                )
            )
        }
        return list
    }

    /**
     * v7.76: Gorevleri Room'a yazar, JSON'u golge kopya olarak tazeler.
     *
     * JSON neden hala yaziliyor: yedekleme (`exportJson`) ve olasi
     * geri donus icin. Iki kaynak da her zaman ayni.
     */
    fun saveTasks(context: Context, tasks: List<Task>) {
        com.gunlukasistan.app.veri.GorevDepo.yaz(context, tasks)
        saveTasksJson(context, tasks)
    }

    /** v7.76: JSON golge kopyasini yazar. */
    private fun saveTasksJson(context: Context, tasks: List<Task>) {
        val array = JSONArray()
        tasks.forEach { task ->
            array.put(
                JSONObject()
                    .put("id", task.id)
                    .put("text", task.text)
                    .put("done", task.done)
                    .put("createdAt", task.createdAt)
                    .put("dueAt", task.dueAt)
                    .put("tekrar", task.tekrar)
                    .put("tekrarBitis", task.tekrarBitis)
                    .put("yapildi", task.yapildi)
                    .put("etiket", task.etiket)
                    .put("arsiv", task.arsiv)
                    .put("arsivZaman", task.arsivZaman)
                    .put("adimlar", JSONArray().also { d ->
                        task.adimlar.forEach { a2 ->
                            d.put(
                                JSONObject()
                                    .put("id", a2.id)
                                    .put("text", a2.text)
                                    .put("done", a2.done)
                                    .put("createdAt", a2.createdAt)
                            )
                        }
                    })
            )
        }
        prefs(context).edit().putString(KEY_TASKS, array.toString()).apply()
        maybeAutoBackup(context)
        // Widget'ta "sıradaki görev" gösterildiği için liste değişince tazele
        try {
            WidgetCommon.refreshAll(context)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "saveTasks başarısız", e)
        }
    }

    // ---------------- Günlük aktivite kaydı ----------------
    // Her gün için: { "20260727": {"c": tamamlanan madde, "f": odak dakikası} }

    /**
     * v7.41: Veri değişince widget'ları tazeler.
     *
     * Eskiden yalnızca saveTasks / saveHabits / bumpToday tazeliyordu; konu,
     * not, etkinlik, kurs ve ders değişiklikleri widget'lara HİÇ yansımıyordu.
     * Kullanıcı "widgetlar güncellenmiyor, senkronizasyonu yok" diye bildirdi.
     *
     * Hata yutulur: widget tazeleme başarısız olsa bile veri kaydı bozulmamalı.
     */
    private fun widgetTazele(context: Context) {
        try {
            // v7.68: "Anlik senkron" acikken 400 ms kisitlamasi atlanir —
            // gorev isaretledigin an widget guncellenir, gecikme olmaz.
            val anlik = try {
                WidgetTema.anlikSenkron(context)
            } catch (_: Exception) {
                true
            }
            WidgetCommon.refreshAll(context, anlik)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Widget tazelenemedi", e)
        }
    }

    /**
     * v7.61: Gunluk kayit onbellekli okunur.
     *
     * Tek ekran ciziminde 17 ayri yerden cagriliyordu ve her seferinde
     * tum JSON bastan ayristiriliyordu. 800 ms'lik kisa onbellek
     * ayni cizim turundaki tekrarlari eler; veri degisince
     * [saveLogRoot] onbellegi gecersiz kilar.
     */
    private fun logRoot(context: Context): JSONObject {
        val ham = Performans.onbellekli("log_root", 800L) {
            prefs(context).getString(KEY_DAILY_LOG, "{}") ?: "{}"
        }
        return try {
            JSONObject(ham)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Gunluk kayit cozulemedi", e)
            JSONObject()
        }
    }

    /**
     * v7.38: Analitik katmanının ham günlük kayda erişimi.
     * logRoot private; dışarıdan anahtar adı tahmin etmek kırılgan olurdu.
     */
    fun gunlukKayitKopyasi(context: Context): JSONObject = try {
        logRoot(context)
    } catch (e: Exception) {
        android.util.Log.w(TAG, "Günlük kayıt okunamadı", e)
        JSONObject()
    }

    private fun saveLogRoot(context: Context, root: JSONObject) {
        prefs(context).edit().putString(KEY_DAILY_LOG, root.toString()).apply()
        // v7.61: onbellek bayatlamasin
        Performans.onbellegiTemizle("log_root")
        maybeAutoBackup(context)
    }

    private fun bumpToday(context: Context, completions: Int, focus: Int, questions: Int = 0) {
        val root = logRoot(context)
        val key = dayKey()
        val day = root.optJSONObject(key) ?: JSONObject().put("c", 0).put("f", 0).put("q", 0)
        day.put("c", day.optInt("c") + completions)
        day.put("f", day.optInt("f") + focus)
        day.put("q", day.optInt("q") + questions)

        // v7.38: SAAT DAĞILIMI — "hangi saatte verimlisin" analizi için.
        // "h" alanı 24 elemanlı dizi: her saatin ağırlıklı puanı.
        // Eskiden hiç saat bilgisi tutulmuyordu; bu yüzden analitik yapılamıyordu.
        try {
            val saat = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val saatler = day.optJSONArray("h") ?: JSONArray().also { d ->
                repeat(24) { d.put(0) }
            }
            // Dizi bozuksa/eksikse 24'e tamamla
            while (saatler.length() < 24) saatler.put(0)
            val puan = completions * 3 + focus + questions / 4
            if (puan > 0) {
                saatler.put(saat, saatler.optInt(saat) + puan)
                day.put("h", saatler)
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Saat dağılımı yazılamadı", e)
        }

        root.put(key, day)
        saveLogRoot(context, root)
        // Günlük veri her değiştiğinde ana ekran widget'ını tazele
        try {
            WidgetCommon.refreshAll(context)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "bumpToday başarısız", e)
        }
    }

    /** Bir alt madde veya görev tamamlandığında çağrılır. */
    fun recordCompletion(context: Context) {
        bumpToday(context, 1, 0)
        // v7.43: rozet ve rekor anlık kontrol edilsin (öneri 11, 12)
        anlikBildirimKontrol(context)
    }

    /** Odaklanma süresi bittiğinde çağrılır. */
    fun addTodayFocusMinutes(context: Context, minutes: Int) {
        bumpToday(context, 0, minutes)
        // v7.43: hedef tamamlandı mı, rozet kazanıldı mı (öneri 10, 11)
        anlikBildirimKontrol(context)
    }

    /**
     * v7.43: Veri değişiminde tetiklenen anlık bildirim kontrolleri.
     * Hata yutulur — bildirim başarısız olsa da veri akışı bozulmamalı.
     */
    private fun anlikBildirimKontrol(context: Context) {
        try {
            BildirimUretici.hedefTamamlandi(context)
            BildirimUretici.rozetKontrol(context)
            BildirimUretici.seriRekoru(context)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Anlık bildirim kontrolü başarısız", e)
        }
    }

    fun getTodayFocusMinutes(context: Context): Int {
        val day = logRoot(context).optJSONObject(dayKey()) ?: return 0
        return day.optInt("f", 0)
    }

    /** Günlük soru sayacına ekleme yapar. */
    fun addQuestions(context: Context, count: Int) = bumpToday(context, 0, 0, count)

    fun getTodayQuestions(context: Context): Int {
        val day = logRoot(context).optJSONObject(dayKey()) ?: return 0
        return day.optInt("q", 0)
    }

    fun getDayFocusMinutesByKey(context: Context, dateKey: String): Int {
        val day = logRoot(context).optJSONObject(dateKey) ?: return 0
        return day.optInt("f", 0)
    }

    fun getDayQuestionsByKey(context: Context, dateKey: String): Int {
        val day = logRoot(context).optJSONObject(dateKey) ?: return 0
        return day.optInt("q", 0)
    }

    private fun scoreOf(day: JSONObject?): Int {
        if (day == null) return 0
        return day.optInt("c", 0) + day.optInt("f", 0) / 25 + day.optInt("q", 0) / 20
    }

    /** Tüm zamanlardaki toplam tamamlanan madde sayısı. */
    fun allTimeCompletions(context: Context): Int {
        val root = logRoot(context)
        var total = 0
        root.keys().forEach { total += root.optJSONObject(it)?.optInt("c", 0) ?: 0 }
        return total
    }

    /** Verilen ayın ("yyyyMM") istatistikleri. */
    fun monthCompletions(context: Context, year: Int, monthZeroBased: Int): Int {
        val prefix = monthKeyOf(year, monthZeroBased)
        val root = logRoot(context)
        var total = 0
        root.keys().forEach { key ->
            if (key.startsWith(prefix)) total += root.optJSONObject(key)?.optInt("c", 0) ?: 0
        }
        return total
    }

    fun monthFocus(context: Context, year: Int, monthZeroBased: Int): Int {
        val prefix = monthKeyOf(year, monthZeroBased)
        val root = logRoot(context)
        var total = 0
        root.keys().forEach { key ->
            if (key.startsWith(prefix)) total += root.optJSONObject(key)?.optInt("f", 0) ?: 0
        }
        return total
    }

    fun monthActiveDays(context: Context, year: Int, monthZeroBased: Int): Int {
        val prefix = monthKeyOf(year, monthZeroBased)
        val root = logRoot(context)
        var days = 0
        root.keys().forEach { key ->
            if (key.startsWith(prefix) && scoreOf(root.optJSONObject(key)) > 0) days++
        }
        return days
    }

    /** Ayın her günü için aktivite puanı (ısı haritası): gün numarası -> puan. */
    fun monthScores(context: Context, year: Int, monthZeroBased: Int): Map<Int, Int> {
        val prefix = monthKeyOf(year, monthZeroBased)
        val root = logRoot(context)
        val map = mutableMapOf<Int, Int>()
        root.keys().forEach { key ->
            if (key.startsWith(prefix) && key.length == 8) {
                val day = key.substring(6, 8).toIntOrNull() ?: return@forEach
                map[day] = scoreOf(root.optJSONObject(key))
            }
        }
        return map
    }

    /**
     * v8.4 — Yıllık ısı haritası için tüm günlük puanlar (öneri 17).
     *
     * `monthScores` ay ay okuyordu; 53 haftalık ızgara için 13 çağrı
     * gerekirdi. Bu, günlük kayıt kökünü bir kez gezip yyyyMMdd → puan
     * eşlemesi döndürüyor.
     *
     * @param gunSayisi bugünden geriye kaç gün (varsayılan 400 — 53 hafta + pay)
     */
    fun gunlukPuanlar(context: Context, gunSayisi: Int = 400): Map<String, Int> {
        val root = logRoot(context)
        val sonuc = HashMap<String, Int>(gunSayisi)
        val esik = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -gunSayisi)
        }
        val esikAnahtar = SimpleDateFormat("yyyyMMdd", Locale.US).format(esik.time)
        root.keys().forEach { key ->
            if (key.length == 8 && key >= esikAnahtar) {
                val p = scoreOf(root.optJSONObject(key))
                if (p > 0) sonuc[key] = p
            }
        }
        return sonuc
    }

    /** Mevcut seri ve en iyi seri (aktif gün = en az 1 tamamlanan veya odak). */
    fun streakInfo(context: Context): Pair<Int, Int> {
        val root = logRoot(context)
        fun active(date: Date): Boolean = scoreOf(root.optJSONObject(dayKey(date))) > 0

        var current = 0
        val cal = Calendar.getInstance()
        if (!active(cal.time)) cal.add(Calendar.DAY_OF_YEAR, -1) // bugün henüz boş olabilir
        while (active(cal.time)) {
            current++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }

        // En iyi seri: tüm aktif günleri sırala, ardışıklık ölç
        val activeDays = mutableListOf<String>()
        root.keys().forEach { key ->
            if (scoreOf(root.optJSONObject(key)) > 0) activeDays.add(key)
        }
        activeDays.sort()
        var best = 0
        var run = 0
        var previous: String? = null
        val dayFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
        activeDays.forEach { key ->
            val onceki = previous
            run = if (onceki != null) {
                val prevDate = dayFormat.parse(onceki)
                val thisDate = dayFormat.parse(key)
                if (prevDate != null && thisDate != null &&
                    thisDate.time - prevDate.time == 24L * 60 * 60 * 1000
                ) {
                    run + 1
                } else 1
            } else 1
            if (run > best) best = run
            previous = key
        }
        return Pair(current, maxOf(best, current))
    }

    /** v10.47: Kullanıcı maddesi #9 — Gün serisini (streak) elle ayarlamak için geçmiş günleri aktif/pasif yapar. */
    fun setStreakDays(context: Context, targetStreak: Int) {
        val n = targetStreak.coerceIn(0, 9999)
        val root = logRoot(context)
        val cal = Calendar.getInstance()
        for (i in 0 until n) {
            val k = dayKey(cal.time)
            val day = root.optJSONObject(k) ?: JSONObject().put("c", 0).put("f", 0).put("q", 0)
            if (day.optInt("f", 0) <= 0) day.put("f", 30)
            root.put(k, day)
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        val kSonra = dayKey(cal.time)
        val daySonra = root.optJSONObject(kSonra)
        if (daySonra != null) {
            daySonra.put("c", 0).put("f", 0).put("q", 0)
            root.put(kSonra, daySonra)
        }
        saveLogRoot(context, root)
    }

    // ---------------- Uygulama ayarları ----------------

    fun getNotifEnabled(context: Context): Boolean =
        prefs(context).getBoolean("pref_notif", true)

    fun setNotifEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean("pref_notif", value).apply()
    }

    fun getSoundEnabled(context: Context): Boolean =
        prefs(context).getBoolean("pref_sound", true)

    fun setSoundEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean("pref_sound", value).apply()
    }

    // v10.59: Ana ekrandaki atölye/modül kısayol butonlarını gösterme/gizleme
    // Varsayılan: FALSE (Orijinal v2 minimalist sade görünüm — yalnızca Zamanlayıcı ⏱ ve Ayarlar ⚙)
    fun getAtolyeButonlariGoster(context: Context): Boolean =
        prefs(context).getBoolean("pref_atolye_goster", false)

    fun setAtolyeButonlariGoster(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean("pref_atolye_goster", value).apply()
    }

    /** v10.48: Otonom AI Ajanı ve Kütüphaneci tarafından kolay görev ekleme köprüsü. */
    fun addTask(context: Context, title: String): Task {
        val list = loadTasks(context).toMutableList()
        val t = Task(
            id = System.currentTimeMillis(),
            text = title,
            done = false,
            createdAt = System.currentTimeMillis()
        )
        list.add(0, t)
        saveTasks(context, list)
        return t
    }

    /** v10.50: Oturum Sonu Çıktı Hasadı köprüsü. */
    fun addNote(context: Context, icerik: String, baslik: String = "Odak Hasadı"): Note {
        val list = loadNotes(context).toMutableList()
        val n = Note(
            id = System.currentTimeMillis(),
            title = baslik,
            content = icerik,
            createdAt = System.currentTimeMillis()
        )
        list.add(0, n)
        saveNotes(context, list)
        return n
    }

    /** v10.48: Otopilot anahtarı. */
    fun getOtopilotAcik(context: Context): Boolean =
        prefs(context).getBoolean("otopilot_acik", false)

    fun setOtopilotAcik(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean("otopilot_acik", value).apply()
    }

    fun getVibEnabled(context: Context): Boolean =
        prefs(context).getBoolean("pref_vib", true)

    fun setVibEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean("pref_vib", value).apply()
    }

    // ---------------- Denemeler (KPSS net takibi) ----------------

    private const val KEY_EXAMS = "exams_json"

    fun loadExams(context: Context): MutableList<Exam> {
        val json = prefs(context).getString(KEY_EXAMS, "[]") ?: "[]"
        val array = JSONArray(json)
        val list = mutableListOf<Exam>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            val netsObj = o.optJSONObject("nets") ?: JSONObject()
            val nets = LinkedHashMap<String, Int>()
            EXAM_SUBJECTS.forEach { s -> nets[s] = netsObj.optInt(s, 0) }
            list.add(
                Exam(
                    id = o.optLong("id", now()),
                    title = o.optString("title", ""),
                    createdAt = o.optLong("createdAt", now()),
                    nets = nets
                )
            )
        }
        return list
    }

    fun saveExams(context: Context, exams: List<Exam>) {
        val array = JSONArray()
        exams.forEach { exam ->
            val nets = JSONObject()
            exam.nets.forEach { (k, v) -> nets.put(k, v) }
            array.put(
                JSONObject()
                    .put("id", exam.id)
                    .put("title", exam.title)
                    .put("createdAt", exam.createdAt)
                    .put("nets", nets)
            )
        }
        prefs(context).edit().putString(KEY_EXAMS, array.toString()).apply()
        widgetTazele(context)
        maybeAutoBackup(context)
    }

    // ---------------- Geri sayım etkinlikleri (v5.2) ----------------

    private const val KEY_EVENTS = "events_json"

    fun loadEvents(context: Context): MutableList<DayEvent> {
        val json = prefs(context).getString(KEY_EVENTS, "[]") ?: "[]"
        val list = mutableListOf<DayEvent>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                list.add(
                    DayEvent(
                        id = o.optLong("id", now()),
                        title = o.optString("title", ""),
                        dateKey = o.optString("dateKey", ""),
                        emoji = o.optString("emoji", "🎯"),
                        pinned = o.optBoolean("pinned", false),
                        createdAt = o.optLong("createdAt", now())
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "loadEvents başarısız", e)
        }
        return list
    }

    fun saveEvents(context: Context, events: List<DayEvent>) {
        val array = JSONArray()
        events.forEach { e ->
            array.put(
                JSONObject()
                    .put("id", e.id)
                    .put("title", e.title)
                    .put("dateKey", e.dateKey)
                    .put("emoji", e.emoji)
                    .put("pinned", e.pinned)
                    .put("createdAt", e.createdAt)
            )
        }
        prefs(context).edit().putString(KEY_EVENTS, array.toString()).apply()
        widgetTazele(context)
        maybeAutoBackup(context)
    }

    fun addEvent(context: Context, title: String, dateKey: String, emoji: String): DayEvent {
        val list = loadEvents(context)
        val event = DayEvent(
            id = now(),
            title = title,
            dateKey = dateKey,
            emoji = emoji,
            createdAt = now()
        )
        list.add(event)
        saveEvents(context, list)
        return event
    }

    fun deleteEvent(context: Context, id: Long) {
        val list = loadEvents(context)
        list.removeAll { it.id == id }
        saveEvents(context, list)
    }

    /** Sabitlenmiş etkinliği değiştirir; aynı anda yalnızca biri sabit olabilir. */
    fun setPinnedEvent(context: Context, id: Long) {
        val list = loadEvents(context)
        val wasPinned = list.firstOrNull { it.id == id }?.pinned == true
        list.forEach { it.pinned = false }
        if (!wasPinned) list.firstOrNull { it.id == id }?.pinned = true
        saveEvents(context, list)
    }

    /**
     * Ana ekranda/widget'ta gösterilecek etkinlik:
     * sabitlenen varsa o, yoksa gelecekteki en yakın tarihli olan.
     */
    fun highlightEvent(context: Context): DayEvent? {
        val list = loadEvents(context)
        list.firstOrNull { it.pinned }?.let { return it }
        return list.filter { !it.isPast }.minByOrNull { it.daysLeft }
    }

    /** Gelecek etkinlikler (yakın tarih önce), geçmişler sona alınır. */
    fun upcomingEvents(context: Context): List<DayEvent> {
        val list = loadEvents(context)
        val future = list.filter { !it.isPast }.sortedBy { it.daysLeft }
        val past = list.filter { it.isPast }.sortedByDescending { it.daysLeft }
        return future + past
    }

    // ---------------- Alışkanlıklar (v5.4) ----------------

    private const val KEY_HABITS = "habits_json"
    private const val KEY_HABIT_LOG = "habit_log_json"

    /**
     * v8.9 · Öneri 15 — Önbellekli okuma.
     *
     * `Habit` düz bir veri sınıfı (iç içe koleksiyon yok), bu
     * yüzden `copy()` ile sığ kopya yeterli. Çağıranlar alanları
     * değiştirdiğinde önbellekteki asıl nesne etkilenmiyor.
     */
    fun loadHabits(context: Context): MutableList<Habit> = Onbellek.al(
        Onbellek.K_HABITS,
        kopyala = { liste: MutableList<Habit> -> liste.mapTo(mutableListOf()) { it.copy() } },
        uret = { loadHabitsDisk(context) }
    )

    private fun loadHabitsDisk(context: Context): MutableList<Habit> {
        val json = prefs(context).getString(KEY_HABITS, "[]") ?: "[]"
        val list = mutableListOf<Habit>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                list.add(
                    Habit(
                        id = o.optLong("id", now()),
                        title = o.optString("title", ""),
                        emoji = o.optString("emoji", "💧"),
                        target = o.optInt("target", 1).coerceAtLeast(1),
                        colorIndex = o.optInt("colorIndex", 0),
                        archived = o.optBoolean("archived", false),
                        createdAt = o.optLong("createdAt", now())
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "loadHabits başarısız", e)
        }
        return list
    }

    fun saveHabits(context: Context, habits: List<Habit>) {
        val array = JSONArray()
        habits.forEach { h ->
            array.put(
                JSONObject()
                    .put("id", h.id)
                    .put("title", h.title)
                    .put("emoji", h.emoji)
                    .put("target", h.target)
                    .put("colorIndex", h.colorIndex)
                    .put("archived", h.archived)
                    .put("createdAt", h.createdAt)
            )
        }
        prefs(context).edit().putString(KEY_HABITS, array.toString()).apply()
        Onbellek.boz(Onbellek.K_HABITS)
        invalidateHabitCache()
        maybeAutoBackup(context)
        try {
            WidgetCommon.refreshAll(context)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "saveHabits başarısız", e)
        }
    }

    fun addHabit(context: Context, title: String, emoji: String, target: Int, colorIndex: Int): Habit {
        val list = loadHabits(context)
        val habit = Habit(
            id = now(), title = title, emoji = emoji,
            target = target.coerceAtLeast(1), colorIndex = colorIndex, createdAt = now()
        )
        list.add(habit)
        saveHabits(context, list)
        return habit
    }

    fun deleteHabit(context: Context, id: Long) {
        saveHabits(context, loadHabits(context).filterNot { it.id == id })
        // Geçmiş kayıtları da temizle
        try {
            val root = habitRoot(context)
            root.keys().asSequence().toList().forEach { day ->
                root.optJSONObject(day)?.remove(id.toString())
            }
            saveHabitRoot(context, root)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "deleteHabit başarısız", e)
        }
    }

    /**
     * Alışkanlık günlüğü önbelleği (v7.0).
     * Widget listelerinde satır başına birkaç kez okunuyordu; her seferinde
     * JSON.parse yapmak "Yükleniyor" takılmasına yol açıyordu.
     */
    @Volatile private var habitRootCache: JSONObject? = null
    @Volatile private var habitRootStamp = 0L

    private fun habitRoot(context: Context): JSONObject {
        val cached = habitRootCache
        // 2 saniyelik pencere: aynı çizim turundaki tüm okumalar tek parse kullanır
        if (cached != null && System.currentTimeMillis() - habitRootStamp < 2000L) {
            return cached
        }
        val fresh = try {
            JSONObject(prefs(context).getString(KEY_HABIT_LOG, "{}") ?: "{}")
        } catch (_: Exception) {
            JSONObject()
        }
        habitRootCache = fresh
        habitRootStamp = System.currentTimeMillis()
        return fresh
    }

    /** Veri değişince önbelleği düşürür. */
    private fun invalidateHabitCache() {
        habitRootCache = null
        habitRootStamp = 0L
    }

    private fun saveHabitRoot(context: Context, root: JSONObject) {
        habitRootCache = root
        habitRootStamp = System.currentTimeMillis()
        prefs(context).edit().putString(KEY_HABIT_LOG, root.toString()).apply()
        widgetTazele(context)
    }

    /** Belirli bir günde alışkanlığın kaç kez yapıldığı. */
    fun habitCount(context: Context, habitId: Long, date: Date = Date()): Int =
        habitRoot(context).optJSONObject(dayKey(date))?.optInt(habitId.toString(), 0) ?: 0

    /** Bugünkü sayacı bir artırır; hedefe ulaşıldıysa başa döner (tekrar dokunmak sıfırlar). */
    fun toggleHabit(context: Context, habit: Habit): Int {
        val root = habitRoot(context)
        val key = dayKey()
        val day = root.optJSONObject(key) ?: JSONObject()
        val current = day.optInt(habit.id.toString(), 0)
        val next = if (current >= habit.target) 0 else current + 1
        day.put(habit.id.toString(), next)
        root.put(key, day)
        saveHabitRoot(context, root)
        maybeAutoBackup(context)
        try {
            WidgetCommon.refreshAll(context)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "toggleHabit başarısız", e)
        }
        return next
    }

    /** Bugün tamamlanan alışkanlık sayısı / toplam aktif alışkanlık. */
    fun habitProgressToday(context: Context): Pair<Int, Int> {
        val active = loadHabits(context).filterNot { it.archived }
        if (active.isEmpty()) return 0 to 0
        val done = active.count { habitCount(context, it.id) >= it.target }
        return done to active.size
    }

    /**
     * Bir alışkanlığın kesintisiz gün serisi (bugün henüz yapılmadıysa dünden sayar).
     * v10.39 · Katalog #42: moladaki günler seriyi kırmaz ama sayılmaz —
     * seri mola süresince DONAR, dönüşte kaldığı yerden devam eder.
     */
    fun habitStreak(context: Context, habit: Habit): Int {
        val root = habitRoot(context)
        val molaBas = AliskanlikMola.baslangic(context, habit.id)
        val molaKapali = AliskanlikMola.kapatilanlar(context, habit.id)
        fun doneOn(date: Date): Boolean =
            (root.optJSONObject(dayKey(date))?.optInt(habit.id.toString(), 0) ?: 0) >= habit.target
        fun moladaOn(date: Date): Boolean =
            AliskanlikMola.moladaMiPure(dayKey(date).toInt(), molaBas, molaKapali)

        var streak = 0
        val cal = Calendar.getInstance()
        if (!doneOn(cal.time) && !moladaOn(cal.time)) cal.add(Calendar.DAY_OF_YEAR, -1)
        // Üst sınır: bozuk veride sonsuz döngüyü önler (mola günleri de adımı tüketir)
        var adim = 0
        while (adim < 400) {
            adim++
            when {
                doneOn(cal.time) -> {
                    streak++
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                }
                moladaOn(cal.time) -> cal.add(Calendar.DAY_OF_YEAR, -1)
                else -> return streak
            }
        }
        return streak
    }

    /** Son [days] günün tamamlanma durumu (eskiden yeniye) — mini ısı şeridi için. */
    fun habitRecent(context: Context, habit: Habit, days: Int = 7): List<Boolean> {
        val root = habitRoot(context)
        val result = mutableListOf<Boolean>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -(days - 1))
        repeat(days) {
            val count = root.optJSONObject(dayKey(cal.time))?.optInt(habit.id.toString(), 0) ?: 0
            result.add(count >= habit.target)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return result
    }

    /** Son [days] gün içinde alışkanlığın tutturulma yüzdesi. */
    fun habitRate(context: Context, habit: Habit, days: Int = 30): Int {
        val recent = habitRecent(context, habit, days)
        if (recent.isEmpty()) return 0
        return recent.count { it } * 100 / recent.size
    }

    /**
     * v10.40 · Katalog #45/#52: hedefini tutturduğu TÜM gün anahtarları
     * (yyyyMMdd kümesi). 21 gün kuralı sayacı ve [SeriAnaliz] bu kümeyi
     * okur; [sinir] bozuk veride fren.
     */
    fun habitTumGunler(context: Context, habit: Habit, sinir: Int = 400): Set<Int> {
        val root = habitRoot(context)
        val out = LinkedHashSet<Int>()
        val anahtarlar = root.keys()
        while (anahtarlar.hasNext() && out.size < sinir) {
            val gun = anahtarlar.next()
            val adet = root.optJSONObject(gun)?.optInt(habit.id.toString(), 0) ?: 0
            if (adet >= habit.target) runCatching { out.add(gun.toInt()) }
        }
        return out
    }

    // ---------------- Kitaplık (v6.6) ----------------

    private const val KEY_BOOKS = "books_json"

    fun loadBooks(context: Context): MutableList<Book> {
        val json = prefs(context).getString(KEY_BOOKS, "[]") ?: "[]"
        val list = mutableListOf<Book>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                list.add(
                    Book(
                        id = o.optLong("id", now()),
                        title = o.optString("title", ""),
                        uri = o.optString("uri", ""),
                        pages = o.optInt("pages", 0),
                        lastPage = o.optInt("lastPage", 0),
                        color = o.optInt("color", 0),
                        addedAt = o.optLong("addedAt", now()),
                        parentId = o.optLong("parentId", 0L),
                        chapterNo = o.optInt("chapterNo", 0),
                        chapterCount = o.optInt("chapterCount", 0)
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "loadBooks başarısız", e)
        }
        return list
    }

    fun saveBooks(context: Context, books: List<Book>) {
        val array = JSONArray()
        books.forEach { b ->
            array.put(
                JSONObject()
                    .put("id", b.id)
                    .put("title", b.title)
                    .put("uri", b.uri)
                    .put("pages", b.pages)
                    .put("lastPage", b.lastPage)
                    .put("color", b.color)
                    .put("addedAt", b.addedAt)
                    .put("parentId", b.parentId)
                    .put("chapterNo", b.chapterNo)
                    .put("chapterCount", b.chapterCount)
            )
        }
        prefs(context).edit().putString(KEY_BOOKS, array.toString()).apply()
        widgetTazele(context)
        maybeAutoBackup(context)
    }

    fun addBook(context: Context, title: String, uri: String): Book {
        val list = loadBooks(context)
        val book = Book(
            id = now(),
            title = title,
            uri = uri,
            color = list.size % 6,
            addedAt = now()
        )
        list.add(book)
        saveBooks(context, list)
        return book
    }

    fun deleteBook(context: Context, id: Long) {
        // Ana kitap silinirse bölümleri de gider
        saveBooks(context, loadBooks(context).filterNot { it.id == id || it.parentId == id })
    }

    /** Bölünmüş bir bölümü kitaplığa ekler. */
    fun addChapter(
        context: Context,
        parent: Book,
        title: String,
        uri: String,
        no: Int,
        pages: Int
    ) {
        val list = loadBooks(context)
        list.add(
            Book(
                id = now() + no,
                title = title,
                uri = uri,
                pages = pages,
                color = parent.color,
                addedAt = now(),
                parentId = parent.id,
                chapterNo = no
            )
        )
        list.firstOrNull { it.id == parent.id }?.chapterCount = no
        saveBooks(context, list)
    }

    /** Ana kitapları (bölüm olmayanlar) döndürür. */
    fun rootBooks(context: Context): List<Book> =
        loadBooks(context).filter { !it.isChapter }.sortedByDescending { it.addedAt }

    /** Bir kitabın bölümlerini sırayla döndürür. */
    fun chaptersOf(context: Context, parentId: Long): List<Book> =
        loadBooks(context).filter { it.parentId == parentId }.sortedBy { it.chapterNo }

    /** Okuma ilerlemesini günceller. */
    fun updateBookProgress(context: Context, id: Long, lastPage: Int, pages: Int) {
        val list = loadBooks(context)
        list.firstOrNull { it.id == id }?.apply {
            this.lastPage = lastPage.coerceAtLeast(0)
            if (pages > 0) this.pages = pages
        }
        saveBooks(context, list)
    }

    // ---------------- Soru sayacı istatistikleri ----------------

    fun weekQuestions(context: Context): Int {
        val root = logRoot(context)
        var total = 0
        val cal = Calendar.getInstance()
        repeat(7) {
            total += root.optJSONObject(dayKey(cal.time))?.optInt("q", 0) ?: 0
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return total
    }

    fun monthQuestions(context: Context, year: Int, monthZeroBased: Int): Int {
        val prefix = monthKeyOf(year, monthZeroBased)
        val root = logRoot(context)
        var total = 0
        root.keys().forEach { key ->
            if (key.startsWith(prefix)) total += root.optJSONObject(key)?.optInt("q", 0) ?: 0
        }
        return total
    }

    fun allTimeQuestions(context: Context): Int {
        val root = logRoot(context)
        var total = 0
        root.keys().forEach { total += root.optJSONObject(it)?.optInt("q", 0) ?: 0 }
        return total
    }

    // ---------------- Kişisel hedefler / sınav tarihi / söz ----------------

    fun getGoalMinutes(context: Context): Int =
        prefs(context).getInt("pref_goal_min", 100)

    fun setGoalMinutes(context: Context, minutes: Int) {
        prefs(context).edit().putInt("pref_goal_min", minutes).apply()
    }

    /** Sınav tarihi "yyyyMMdd" biçiminde. */
    fun getExamDate(context: Context): String =
        prefs(context).getString("pref_exam_date", "20260906") ?: "20260906"

    fun setExamDate(context: Context, key: String) {
        prefs(context).edit().putString("pref_exam_date", key).apply()
    }

    fun getExamDateMillis(context: Context): Long {
        return try {
            SimpleDateFormat("yyyyMMdd", Locale.US).parse(getExamDate(context))?.time ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    fun getQuote(context: Context): String =
        prefs(context).getString("pref_quote_text", "") ?: ""

    fun setQuote(context: Context, text: String) {
        prefs(context).edit().putString("pref_quote_text", text).apply()
    }

    // ---------------- Yedekleme / Geri yükleme ----------------

    /** Tüm veriyi tek bir JSON metni olarak döndürür. */
    fun exportJson(context: Context): String {
        val p = prefs(context)
        return JSONObject()
            .put("app", "GunlukAsistan")
            // v7.98: biçim 18 — modül depoları eklendi (bkz. PrefYedek)
            .put("version", YEDEK_BICIM)
            .put("exported_at", now())
            .put(KEY_NOTES, JSONArray(p.getString(KEY_NOTES, "[]") ?: "[]"))
            .put(KEY_TASKS, JSONArray(p.getString(KEY_TASKS, "[]") ?: "[]"))
            .put(KEY_TOPICS, JSONArray(p.getString(KEY_TOPICS, "[]") ?: "[]"))
            .put(KEY_DAILY_LOG, JSONObject(p.getString(KEY_DAILY_LOG, "{}") ?: "{}"))
            .put(KEY_EXAMS, JSONArray(p.getString(KEY_EXAMS, "[]") ?: "[]"))
            .put(KEY_EVENTS, JSONArray(p.getString(KEY_EVENTS, "[]") ?: "[]"))
            .put(KEY_HABITS, JSONArray(p.getString(KEY_HABITS, "[]") ?: "[]"))
            .put(KEY_BOOKS, JSONArray(p.getString(KEY_BOOKS, "[]") ?: "[]"))
            .put(KEY_HABIT_LOG, JSONObject(p.getString(KEY_HABIT_LOG, "{}") ?: "{}"))
            // v7.17: kurs verileri — ders ilerlemesi, notlar, yer imleri
            .put(KEY_COURSES, JSONArray(p.getString(KEY_COURSES, "[]") ?: "[]"))
            .put(KEY_SECTIONS, JSONArray(p.getString(KEY_SECTIONS, "[]") ?: "[]"))
            .put(KEY_LESSONS, JSONArray(p.getString(KEY_LESSONS, "[]") ?: "[]"))
            // v7.20: derse eklenen internet kaynakları
            .put(KEY_KAYNAKLAR, JSONArray(p.getString(KEY_KAYNAKLAR, "[]") ?: "[]"))
            // v7.29: quiz soruları, sonuçlar ve tekrar programı
            .put("quiz", QuizStore.disaAktar(context))
            // v7.33: bilgi kartları
            .put("kart", KartStore.disaAktar(context))
            // v7.37: özel öğretmen oturumları, seviye, zayıf noktalar
            .put("ogretmen", OgretmenStore.disaAktar(context))
            // v7.46: namaz ayarları ve vakit arası plan
            .put("namaz", NamazVakti.disaAktar(context))
            .put("namaz_plan", NamazPlan.disaAktar(context))
            .put("zorunlu_uyari", ZorunluUyari.disaAktar(context))
            .put("ai_sohbetler", SohbetGecmisi.disaAktar(context))
            // v7.49: film/dizi izleme listesi ve tercihler
            .put("film", FilmStore.disaAktar(context))
            // v7.51: online oda bilgisi (paylaşılan veri sunucuda kalır)
            .put("online", OnlineStore.disaAktar(context))
            // v7.17: kurs çalışma serisi
            .put(KEY_SERI_GUN, p.getString(KEY_SERI_GUN, "") ?: "")
            .put(KEY_SERI_SAYI, p.getInt(KEY_SERI_SAYI, 0))
            .put(KEY_SERI_REKOR, p.getInt(KEY_SERI_REKOR, 0))
            .put("son_ders_id", p.getLong("son_ders_id", 0L))
            .put("pref_goal_min", p.getInt("pref_goal_min", 100))
            .put("pref_exam_date", p.getString("pref_exam_date", "20260906"))
            .put("pref_quote_text", p.getString("pref_quote_text", ""))
            // v7.34: yapay zekâ ücret tercihi (anahtarlar GÜVENLİK GEREĞİ yedeğe girmez)
            .put("pref_ai_ucretsiz", AiSettings.isUcretsizMod(context))
            // v7.98 — KRİTİK EKSİK GİDERİLDİ:
            // v7.78'den beri eklenen 11 modülün verisi yedeğe hiç girmiyordu.
            // Program ilerlemesi, hata defteri, sözlük, koç karnesi, pomodoro,
            // çalışma oturumları, haftalık plan, PDF yer imleri, kanıt kayıtları,
            // sayaç ve okuma ayarları... Telefon değişiminde hepsi kayboluyordu.
            //
            // Tek tek disaAktar yazmak yerine PrefYedek tüm depoları geziyor;
            // böylece bundan sonra eklenen modüller de otomatik kapsanıyor.
            .put("moduller", PrefYedek.disaAktar(context, anlatimlariDahilEt = false))
            .toString()
    }

    /**
     * v7.17: Yedeğin içeriğini özetler — geri yüklemeden önce kullanıcıya gösterilir.
     * Bozuk/yabancı dosyalarda null döner.
     */
    data class YedekOzet(
        val surum: Int,
        val tarih: Long,
        val ders: Int,
        val bitenDers: Int,
        val dersNotu: Int,
        val gorev: Int,
        val not: Int,
        val konu: Int,
        val aliskanlik: Int,
        val seriRekor: Int
    )

    fun yedekOzeti(text: String): YedekOzet? {
        return try {
            val o = JSONObject(text)
            if (o.optString("app") != "GunlukAsistan") return null
            fun dizi(k: String) = try {
                JSONArray(o.optString(k, "[]"))
            } catch (_: Exception) {
                o.optJSONArray(k) ?: JSONArray()
            }
            val dersler = dizi(KEY_LESSONS)
            var biten = 0
            var notlu = 0
            for (i in 0 until dersler.length()) {
                val d = dersler.optJSONObject(i) ?: continue
                if (d.optBoolean("done", false)) biten++
                if (d.optString("note", "").isNotBlank()) notlu++
            }
            YedekOzet(
                surum = o.optInt("version", 0),
                tarih = o.optLong("exported_at", 0L),
                ders = dersler.length(),
                bitenDers = biten,
                dersNotu = notlu,
                gorev = dizi(KEY_TASKS).length(),
                not = dizi(KEY_NOTES).length(),
                konu = dizi(KEY_TOPICS).length(),
                aliskanlik = dizi(KEY_HABITS).length(),
                seriRekor = o.optInt(KEY_SERI_REKOR, 0)
            )
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Yedek özeti okunamadı", e)
            null
        }
    }

    /** Daha önce dışa aktarılmış JSON metnini geri yükler. Başarılıysa true. */
    /**
     * v8.8 · Öneri 4 — Yedek biçim sürümü.
     *
     * Her yedeğe yazılıyor. Geri yüklerken bu sayı uygulamanın
     * desteklediğinden BÜYÜKSE yükleme reddediliyor: daha yeni bir
     * sürümde eklenen alanları eski kod tanımaz ve sessizce veri
     * kaybına yol açardı (örneğin yeni bir alan eski biçimde
     * yazılırken silinirdi).
     */
    // v9.7: biçim 19 — takip_v1, butce_v1, konum_hatirlatma_v1 depoları eklendi
    const val YEDEK_BICIM = 19

    /** Geri yükleme sonucu — kullanıcıya doğru mesajı göstermek için. */
    sealed class IceAktarSonuc {
        object Basarili : IceAktarSonuc()
        /** Dosya bu uygulamaya ait değil. */
        object YanlisUygulama : IceAktarSonuc()
        /** Yedek daha yeni bir sürümden; uygulamayı güncellemek gerek. */
        data class CokYeni(val yedekSurum: Int, val destek: Int) : IceAktarSonuc()
        /** JSON bozuk veya okunamadı. */
        data class Bozuk(val neden: String) : IceAktarSonuc()
    }

    /**
     * v8.8 · Öneri 4 + 5 — Denetimli geri yükleme.
     *
     * ── Öneri 4: sürüm kontrolü ──
     * Yedeğin biçim sürümü desteklenenden büyükse reddediliyor.
     *
     * ── Öneri 5: geri alınabilirlik ──
     * Yükleme ÖNCESİ mevcut verinin anlık görüntüsü alınıyor.
     * Yanlış yedek yüklenirse [yuklemeyiGeriAl] ile dönülebiliyor.
     * Eskiden yanlış yedek = veri gitti demekti.
     */
    fun iceAktarDenetimli(context: Context, text: String): IceAktarSonuc {
        val obj = try {
            JSONObject(text)
        } catch (e: Exception) {
            // Bozuk içeriği kenara koy — kullanıcı elle kurtarabilsin
            GuvenliDosya.bozukOlarakSakla(
                context.getExternalFilesDir(null) ?: context.filesDir,
                "yedek_geri_yukleme", text
            )
            return IceAktarSonuc.Bozuk(e.message ?: "JSON ayrıştırılamadı")
        }

        if (obj.optString("app") != "GunlukAsistan") {
            return IceAktarSonuc.YanlisUygulama
        }

        val yedekSurum = obj.optInt("version", 0)
        if (yedekSurum > YEDEK_BICIM) {
            return IceAktarSonuc.CokYeni(yedekSurum, YEDEK_BICIM)
        }

        // Öneri 5: yükleme öncesi anlık görüntü
        yuklemeOncesiKaydet(context)

        return if (importJson(context, text)) {
            IceAktarSonuc.Basarili
        } else {
            IceAktarSonuc.Bozuk("İçe aktarma başarısız")
        }
    }

    private fun yuklemeOncesiDosya(context: Context) =
        java.io.File(context.filesDir, "yukleme_oncesi.json")

    /** Geri yüklemeden hemen önce mevcut durumu saklar. */
    private fun yuklemeOncesiKaydet(context: Context) {
        runCatching {
            GuvenliDosya.yaz(yuklemeOncesiDosya(context), exportJson(context))
        }.onFailure { android.util.Log.w(TAG, "Yükleme öncesi kayıt", it) }
    }

    /** Yükleme öncesi durum var mı? (düğmeyi göstermek için) */
    fun yuklemeGeriAlinabilirMi(context: Context): Boolean =
        runCatching { yuklemeOncesiDosya(context).let { it.exists() && it.length() > 0 } }
            .getOrDefault(false)

    /** Yanlış yüklenen yedeği geri alır. */
    fun yuklemeyiGeriAl(context: Context): Boolean {
        val icerik = GuvenliDosya.oku(yuklemeOncesiDosya(context)) ?: return false
        return runCatching {
            val oldu = importJson(context, icerik)
            if (oldu) yuklemeOncesiDosya(context).delete()
            oldu
        }.getOrDefault(false)
    }

    fun importJson(context: Context, text: String): Boolean {
        return try {
            val obj = JSONObject(text)
            if (obj.optString("app") != "GunlukAsistan") return false
            val p = prefs(context)
            val edit = p.edit()
            if (obj.has(KEY_NOTES)) edit.putString(KEY_NOTES, obj.getJSONArray(KEY_NOTES).toString())
            if (obj.has(KEY_TASKS)) edit.putString(KEY_TASKS, obj.getJSONArray(KEY_TASKS).toString())
            if (obj.has(KEY_TOPICS)) edit.putString(KEY_TOPICS, obj.getJSONArray(KEY_TOPICS).toString())
            if (obj.has(KEY_DAILY_LOG)) edit.putString(KEY_DAILY_LOG, obj.getJSONObject(KEY_DAILY_LOG).toString())
            if (obj.has(KEY_EXAMS)) edit.putString(KEY_EXAMS, obj.getJSONArray(KEY_EXAMS).toString())
            if (obj.has(KEY_EVENTS)) edit.putString(KEY_EVENTS, obj.getJSONArray(KEY_EVENTS).toString())
            if (obj.has(KEY_HABITS)) edit.putString(KEY_HABITS, obj.getJSONArray(KEY_HABITS).toString())
            if (obj.has(KEY_BOOKS)) edit.putString(KEY_BOOKS, obj.getJSONArray(KEY_BOOKS).toString())
            if (obj.has(KEY_HABIT_LOG)) edit.putString(KEY_HABIT_LOG, obj.getJSONObject(KEY_HABIT_LOG).toString())
            // v7.17: kurs verileri (eski yedeklerde yoksa atlanır)
            if (obj.has(KEY_COURSES)) edit.putString(KEY_COURSES, obj.getJSONArray(KEY_COURSES).toString())
            if (obj.has(KEY_SECTIONS)) edit.putString(KEY_SECTIONS, obj.getJSONArray(KEY_SECTIONS).toString())
            if (obj.has(KEY_LESSONS)) edit.putString(KEY_LESSONS, obj.getJSONArray(KEY_LESSONS).toString())
            if (obj.has(KEY_KAYNAKLAR)) edit.putString(KEY_KAYNAKLAR, obj.getJSONArray(KEY_KAYNAKLAR).toString())
            obj.optJSONObject("quiz")?.let { QuizStore.iceAktar(context, it) }
            obj.optJSONObject("kart")?.let { KartStore.iceAktar(context, it) }
            // v7.37: öğretmen modu verileri
            obj.optJSONObject("ogretmen")?.let { OgretmenStore.iceAktar(context, it) }
            // v7.46: namaz ayarları ve planı
            obj.optJSONObject("namaz")?.let { NamazVakti.iceAktar(context, it) }
            if (obj.has("namaz_plan")) {
                NamazPlan.iceAktar(context, obj.optString("namaz_plan", "[]"))
            }
            // v10.25 🔴 F1 DÜZELTME: ZorunluUyari ve SohbetGecmisi geri
            // yükleme yanlışlıkla `has("namaz_plan")` koşulunun İÇİNDEYDİ
            // (kopyala-yapıştır kalıntısı). Namaz planı olmayan yedekte
            // zorunlu uyarılar ve AI sohbet geçmişi sessizce atlanırdı.
            // Ayrıca korumasız optString("ai_sohbetler","[]") çağrısı,
            // sohbet ALANI OLMAYAN eski bir yedek yüklenirken mevcut
            // sohbet geçmişini boş listeyle EZERDİ — her ikisi de
            // `has(...)` kapısıyla ayrı ayrı korunuyor.
            if (obj.has("zorunlu_uyari")) {
                ZorunluUyari.iceAktar(context, obj.optJSONObject("zorunlu_uyari"))
            }
            if (obj.has("ai_sohbetler")) {
                SohbetGecmisi.iceAktar(context, obj.optString("ai_sohbetler", "[]"))
            }
            // v7.49: film verileri
            obj.optJSONObject("film")?.let { FilmStore.iceAktar(context, it) }
            obj.optJSONObject("online")?.let { OnlineStore.iceAktar(context, it) }
            // v7.98: modül depoları (program, hata defteri, sözlük, koç,
            // pomodoro, oturumlar, haftalık plan, yer imleri, ayarlar...)
            // Eski yedeklerde bu alan yok — sessizce atlanır.
            obj.optJSONObject("moduller")?.let { PrefYedek.iceAktar(context, it) }
            // v7.17: çalışma serisi
            if (obj.has(KEY_SERI_GUN)) edit.putString(KEY_SERI_GUN, obj.getString(KEY_SERI_GUN))
            if (obj.has(KEY_SERI_SAYI)) edit.putInt(KEY_SERI_SAYI, obj.getInt(KEY_SERI_SAYI))
            if (obj.has(KEY_SERI_REKOR)) edit.putInt(KEY_SERI_REKOR, obj.getInt(KEY_SERI_REKOR))
            if (obj.has("son_ders_id")) edit.putLong("son_ders_id", obj.getLong("son_ders_id"))
            if (obj.has("pref_goal_min")) edit.putInt("pref_goal_min", obj.getInt("pref_goal_min"))
            if (obj.has("pref_exam_date")) edit.putString("pref_exam_date", obj.getString("pref_exam_date"))
            if (obj.has("pref_quote_text")) edit.putString("pref_quote_text", obj.getString("pref_quote_text"))
            // v7.34: ücretsiz mod tercihi
            if (obj.has("pref_ai_ucretsiz")) {
                AiSettings.setUcretsizMod(context, obj.getBoolean("pref_ai_ucretsiz"))
            }
            // Yeniden örnek veri eklenmesin
            edit.putBoolean(KEY_SEEDED, true)
            edit.putBoolean(KEY_SEEDED_V2, true)
            val ok = edit.commit()
            // v8.9 · Öneri 15 KRİTİK: geri yükleme her şeyi değiştirdi.
            // Önbellek temizlenmezse kullanıcı yedeği yükledikten sonra
            // ESKİ veriyi görmeye devam ederdi — "yedek çalışmıyor"
            // sanırdı. Bu, önbellek eklerken yapılabilecek en kolay
            // ve en yıkıcı hata.
            Onbellek.hepsiniBoz()
            // v7.76 KRITIK: geri yukleme JSON'u degistirdi; Room hala eski
            // veriyi tutuyor olurdu. JSON'u kaynak kabul edip veritabanini
            // yeniden kur. Bu satir olmazsa kullanici yedegi geri yukleyince
            // gorevler eski haliyle gorunurdu.
            try {
                com.gunlukasistan.app.veri.GorevDepo
                    .jsondanTazele(context, loadTasksJson(context))
                // v8.1: notlar da Room'da — aynı tazeleme gerekli
                com.gunlukasistan.app.veri.NotDepo
                    .jsondanTazele(context, loadNotesJson(context))
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Room tazelenemedi", e)
            }
            ok
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Yedek geri yüklenemedi", e)
            false
        }
    }

    /**
     * v7.17: Geri yüklemeden hemen önce mevcut durumun güvenlik kopyasını alır.
     * Yanlış yedek yüklenirse `geriAlYedek()` ile dönülebilir.
     */
    private const val UNDO_FILE_NAME = "geri-al-yedegi.json"

    /** Güvenlik kopyası dosyası — prefs'i şişirmemek için ayrı tutulur. */
    private fun undoFile(context: Context) =
        java.io.File(context.filesDir, UNDO_FILE_NAME)

    fun guvenlikKopyasiAl(context: Context) {
        try {
            // v8.8 · Öneri 7: atomik yazma (geri alma anlık görüntüsü)
            GuvenliDosya.yaz(undoFile(context), exportJson(context))
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Güvenlik kopyası alınamadı", e)
        }
    }

    /** v7.17: Son geri yüklemeden önceki duruma döner. */
    fun geriAlYedek(context: Context): Boolean {
        return try {
            val f = undoFile(context)
            if (!f.exists()) return false
            val onceki = f.readText()
            if (onceki.isBlank()) return false
            val ok = importJson(context, onceki)
            if (ok) f.delete()
            ok
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Geri alma başarısız", e)
            false
        }
    }

    /** v7.17: Geri alınabilecek bir yedek var mı. */
    fun geriAlinabilirYedekVar(context: Context): Boolean =
        try {
            undoFile(context).let { it.exists() && it.length() > 0 }
        } catch (_: Exception) {
            false
        }

    // ---------------- Son gün istatistikleri (detay ekranları için) ----------------

    private val trLocale = Locale("tr", "TR")

    /** Son [days] günün (yeniden eskiye) tamamlanma/odak değerleri: Triple(etiket, c, f) */
    // ---------------- v6.1: Grafik ve ızgara verileri ----------------

    /**
     * Son [days] günün günlük aktivite puanı (eskiden yeniye).
     * Çizgi grafik için: tamamlanan madde + odak dakikası + soru birleşik puanı.
     */
    fun dailyTrend(context: Context, days: Int = 21): List<Float> {
        val root = logRoot(context)
        val out = mutableListOf<Float>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -(days - 1))
        repeat(days) {
            val day = root.optJSONObject(dayKey(cal.time))
            val c = day?.optInt("c", 0) ?: 0
            val f = day?.optInt("f", 0) ?: 0
            val q = day?.optInt("q", 0) ?: 0
            // Farklı ölçekleri dengeleyen basit puan
            out.add((c * 3f + f * 0.6f + q * 0.15f))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return out
    }

    /**
     * Alışkanlığın son 4 haftalık günlük durumu.
     * Dönen dizi: 4 hafta × 5 gün (en eski haftadan bugüne).
     */
    fun habitWeeks(context: Context, habit: Habit): Array<BooleanArray> {
        val root = habitRoot(context)
        val weeks = Array(4) { BooleanArray(5) }
        val cal = Calendar.getInstance()
        // 20 gün geriye git (4 hafta × 5 gün)
        cal.add(Calendar.DAY_OF_YEAR, -19)
        for (w in 0 until 4) {
            for (d in 0 until 5) {
                val count = root.optJSONObject(dayKey(cal.time))
                    ?.optInt(habit.id.toString(), 0) ?: 0
                weeks[w][d] = count >= habit.target
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return weeks
    }

    fun recentDayStats(context: Context, days: Int): List<Triple<String, Int, Int>> {
        val root = logRoot(context)
        val out = mutableListOf<Triple<String, Int, Int>>()
        val cal = Calendar.getInstance()
        val label = SimpleDateFormat("d MMM EEE", trLocale)
        repeat(days) {
            val key = dayKey(cal.time)
            val day = root.optJSONObject(key)
            out.add(
                Triple(
                    label.format(cal.time),
                    day?.optInt("c", 0) ?: 0,
                    day?.optInt("f", 0) ?: 0
                )
            )
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return out
    }

    /** Son 7 günün odak toplamı (dk). */
    fun weekFocus(context: Context): Int = recentDayStats(context, 7).sumOf { it.third }

    /** Son 7 günün tamamlanan madde toplamı. */
    fun weekCompletions(context: Context): Int = recentDayStats(context, 7).sumOf { it.second }

    /** Tüm zamanların odak toplamı (dk). */
    fun allTimeFocus(context: Context): Int {
        val root = logRoot(context)
        var total = 0
        root.keys().forEach { total += root.optJSONObject(it)?.optInt("f", 0) ?: 0 }
        return total
    }

    /** Verilen gün aktivitesi var mı? */
    fun dayWasActive(context: Context, dayCountBack: Int): Boolean {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -dayCountBack)
        return scoreOf(logRoot(context).optJSONObject(dayKey(cal.time))) > 0
    }

    // ---------------- Otomatik yedek senkronu (cihaz içi) ----------------

    private const val KEY_AUTO_BACKUP = "auto_backup_v1"
    private const val KEY_BACKUP_TS = "last_backup_ts"
    const val BACKUP_FILE_NAME = "yedek-gunluk-asistan.txt"

    fun getAutoBackupEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_BACKUP, true)

    fun setAutoBackupEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO_BACKUP, value).apply()
        if (value) autoBackupNow(context)
    }

    fun lastBackupLabel(context: Context): String {
        val ts = prefs(context).getLong(KEY_BACKUP_TS, 0L)
        if (ts <= 0L) return "henüz yok"
        return SimpleDateFormat("d MMM HH:mm", trLocale).format(Date(ts))
    }

    /** Veriler her değiştiğinde cihaz hafızasına yedek dosyasını tazeler. */
    /**
     * v7.61 — DONMA DUZELTMESI.
     *
     * Eskiden bu fonksiyon ana is parcaciginda `autoBackupNow()` cagiriyordu;
     * o da tum veriyi JSON'a cevirip iki dosyaya + MediaStore'a yaziyordu.
     * Tek bir gorev isaretlemek bile yuzlerce ms disk G/C tetikliyor,
     * ekran donuyordu.
     *
     * Artik: son degisiklikten 2,5 sn sonra, ARKA PLANDA, tek seferde.
     * Art arda 10 degisiklik olursa 10 kez degil 1 kez yedeklenir.
     * Uygulama arka plana alinirken bekleyen yedek hemen tamamlanir
     * (bkz. MainActivity.onStop -> Performans.tumunuBitir).
     */
    const val YEDEK_ISI = "oto_yedek"

    /**
     * Veri değişti — yedek al.
     *
     * v9.8 · Öneri 47: WorkManager'a taşındı.
     *
     * ── Neden iki katmanlı ──
     * Eskiden yalnızca `Performans.geciktir` vardı: uygulama
     * açıkken 2,5 saniye sonra yedek alıyordu. Kullanıcı hemen
     * çıkarsa veya sistem uygulamayı öldürürse **iş kayboluyordu**.
     *
     * Şimdi ikisi birden çalışıyor ve bu bilinçli:
     *
     *   1. `Performans.geciktir` → hızlı yol. Uygulama açıkken
     *      2,5 saniyede yedek hazır, kullanıcı anında görüyor.
     *   2. `ArkaPlanIs.yedekKuyrugaAl` → güvenlik ağı. Uygulama
     *      ölse bile WorkManager işi diskten okuyup çalıştırıyor.
     *
     * Çift yedekleme maliyeti yok: `autoBackupNow` aynı içeriği
     * aynı dosyaya yazıyor, ikinci yazma zararsız. Kaybolmuş bir
     * yedek ise geri getirilemez — asimetrik risk, çift çalışmaya
     * değer.
     */
    fun maybeAutoBackup(context: Context) {
        if (!getAutoBackupEnabled(context)) return
        val uygulama = context.applicationContext
        // Hızlı yol — uygulama açıkken
        Performans.geciktir(YEDEK_ISI, 2500L) { autoBackupNow(uygulama) }
        // Güvenlik ağı — uygulama ölse bile
        runCatching { ArkaPlanIs.yedekKuyrugaAl(uygulama, 15) }
    }

    /** Uygulamadan cikarken bekleyen yedegi hemen tamamlar. */
    fun bekleyenYedegiBitir() {
        Performans.hemenBitir(YEDEK_ISI)
    }

    fun autoBackupNow(context: Context) {
        // v7.98: içerik bir kez üretilip iki yere yazılıyor.
        // exportJson pahalı bir işlem (tüm veriyi JSON'a çevirir);
        // rotasyon için ikinci kez çağırmak gereksiz maliyet olurdu.
        val icerik = try {
            exportJson(context)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Yedek içeriği üretilemedi", e)
            return
        }

        try {
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            // v8.8 · Öneri 7: ATOMİK yazma.
            //
            // Eskiden `writeText` kullanılıyordu: önce dosyayı sıfırlar,
            // sonra yazar. İki adım arasında sistem uygulamayı öldürürse
            // (2 GB RAM'li cihazlarda sık) hem yeni yedek yazılmamış hem
            // ESKİ YEDEK SİLİNMİŞ olurdu. Kullanıcı bunu ancak telefonunu
            // kaybedip geri yüklemeye çalışınca fark ederdi.
            //
            // Artık geçici dosyaya yazılıp atomik rename yapılıyor;
            // ayrıca bir önceki sürüm `.onceki` olarak korunuyor.
            val hedef = java.io.File(dir, BACKUP_FILE_NAME)
            if (GuvenliDosya.yazVeOncekiniKoru(hedef, icerik)) {
                prefs(context).edit().putLong(KEY_BACKUP_TS, System.currentTimeMillis()).apply()
            } else {
                android.util.Log.w(TAG, "autoBackupNow: yedek yazılamadı")
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "autoBackupNow başarısız", e)
        }

        // v7.98: günlük tarihli kopya (öneri 2).
        // Tek dosyaya yazmak, bozuk veri yazılırsa sağlam sürüm bırakmıyordu.
        try {
            YedekRotasyon.gerekirseAl(context, icerik)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Rotasyon yedeği alınamadı", e)
        }
        // v7.18: uygulama silinse bile kalacak KALICI yedek
        kaliciYedekYaz(context)
        // v7.43: yedek bildirimi (öneri 24) — varsayılan kapalı
        try {
            val kayit = loadTasks(context).size + loadNotes(context).size +
                loadTopics(context).size + loadLessons(context).size
            BildirimUretici.yedekAlindi(context, kayit)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Yedek bildirimi gönderilemedi", e)
        }
        // Ana ekran widget'ını da tazele (v7.61: ana iş parçacığında)
        try {
            Performans.anaIs { WidgetCommon.refreshAll(context) }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "autoBackupNow başarısız", e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // v7.18 — KALICI YEDEK (uygulama kaldırılsa bile silinmez)
    // ═══════════════════════════════════════════════════════════════
    //
    // getExternalFilesDir() -> /Android/data/<paket>/files/
    //   Bu klasör uygulama KALDIRILINCA Android tarafından SİLİNİR.
    //   Yani oradaki yedek, en çok ihtiyaç duyulduğu anda yok oluyor.
    //
    // Downloads klasörü ise uygulamadan bağımsızdır ve silinmez.

    const val KALICI_YEDEK_ADI = "GunlukAsistan-otomatik-yedek.json"

    /** İndirilenler klasöründeki kalıcı yedek dosyası. */
    private fun kaliciYedekDosyasi(): java.io.File? = try {
        val indirilenler = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_DOWNLOADS
        )
        if (indirilenler != null) java.io.File(indirilenler, KALICI_YEDEK_ADI) else null
    } catch (e: Exception) {
        android.util.Log.w(TAG, "İndirilenler klasörü bulunamadı", e)
        null
    }

    /**
     * v7.18: Yedeği İndirilenler klasörüne yazar.
     * Android 10+ üzerinde MediaStore kullanılır (izin gerekmez).
     */
    fun kaliciYedekYaz(context: Context): Boolean {
        val json = try {
            exportJson(context)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Kalıcı yedek üretilemedi", e)
            return false
        }
        // Android 10+ : MediaStore ile Downloads'a yaz
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            return mediaStoreYedekYaz(context, json)
        }
        // Android 9 ve öncesi : doğrudan dosya
        return try {
            val f = kaliciYedekDosyasi() ?: return false
            // v8.8 · Öneri 7: atomik yazma
            GuvenliDosya.yazVeOncekiniKoru(f, json)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Kalıcı yedek yazılamadı", e)
            false
        }
    }

    @android.annotation.TargetApi(android.os.Build.VERSION_CODES.Q)
    private fun mediaStoreYedekYaz(context: Context, json: String): Boolean {
        return try {
            val cr = context.contentResolver
            val koleksiyon = android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI
            // Aynı adlı eski kaydı bul
            var hedef: android.net.Uri? = null
            cr.query(
                koleksiyon,
                arrayOf(android.provider.MediaStore.Downloads._ID),
                "${android.provider.MediaStore.Downloads.DISPLAY_NAME}=?",
                arrayOf(KALICI_YEDEK_ADI),
                null
            )?.use { c ->
                if (c.moveToFirst()) {
                    hedef = android.content.ContentUris.withAppendedId(koleksiyon, c.getLong(0))
                }
            }
            if (hedef == null) {
                val degerler = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Downloads.DISPLAY_NAME, KALICI_YEDEK_ADI)
                    put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/json")
                }
                hedef = cr.insert(koleksiyon, degerler)
            }
            val uri = hedef ?: return false
            cr.openOutputStream(uri, "wt")?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            true
        } catch (e: Exception) {
            android.util.Log.w(TAG, "MediaStore yedeği yazılamadı", e)
            false
        }
    }

    /** v7.18: İndirilenler klasöründeki kalıcı yedeği okur. */
    fun kaliciYedekOku(context: Context): String? {
        // Android 10+ : MediaStore
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            try {
                val cr = context.contentResolver
                val koleksiyon = android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI
                cr.query(
                    koleksiyon,
                    arrayOf(android.provider.MediaStore.Downloads._ID),
                    "${android.provider.MediaStore.Downloads.DISPLAY_NAME}=?",
                    arrayOf(KALICI_YEDEK_ADI),
                    null
                )?.use { c ->
                    if (c.moveToFirst()) {
                        val uri = android.content.ContentUris.withAppendedId(koleksiyon, c.getLong(0))
                        cr.openInputStream(uri)?.use { g ->
                            return g.readBytes().toString(Charsets.UTF_8)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w(TAG, "MediaStore yedeği okunamadı", e)
            }
        }
        // Dosya yolu (eski Android veya MediaStore başarısızsa)
        return try {
            val f = kaliciYedekDosyasi()
            if (f != null && f.exists()) f.readText() else null
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Kalıcı yedek okunamadı", e)
            null
        }
    }

    /**
     * v7.18: Uygulamada veri var mı? Kurtarma teklifi için kullanılır.
     * Kurs/görev/not/konu hiçbiri yoksa "boş" sayılır.
     */
    fun veriBosMu(context: Context): Boolean {
        return try {
            val p = prefs(context)
            fun bos(k: String) = (p.getString(k, "[]") ?: "[]").length <= 2
            bos(KEY_LESSONS) && bos(KEY_TASKS) && bos(KEY_NOTES) &&
                bos(KEY_TOPICS) && bos(KEY_HABITS)
        } catch (_: Exception) {
            false
        }
    }

    /** v7.18: Kurtarma teklifi bir kez gösterilir, reddedilirse tekrar sorulmaz. */
    private const val KEY_KURTARMA_SORULDU = "kurtarma_soruldu_v1"

    fun kurtarmaSoruldu(context: Context): Boolean =
        prefs(context).getBoolean(KEY_KURTARMA_SORULDU, false)

    fun kurtarmaSoruldu(context: Context, deger: Boolean) {
        prefs(context).edit().putBoolean(KEY_KURTARMA_SORULDU, deger).apply()
    }

    /** Otomatik yedek dosyasının içeriğini okur (geri yükleme için). */
    fun readAutoBackup(context: Context): String? {
        return try {
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            val f = java.io.File(dir, BACKUP_FILE_NAME)
            if (f.exists()) f.readText() else null
        } catch (_: Exception) {
            null
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // v7.4 — Kurs veri katmanı
    // ═══════════════════════════════════════════════════════════════════

    private const val KEY_COURSES = "courses_json"
    private const val KEY_SECTIONS = "sections_json"
    private const val KEY_LESSONS = "lessons_json"

    /**
     * v8.9 · Öneri 15 — Önbellekli okuma.
     *
     * `Course` düz bir veri sınıfı (iç içe koleksiyon yok), bu
     * yüzden `copy()` ile sığ kopya yeterli. Çağıranlar alanları
     * değiştirdiğinde önbellekteki asıl nesne etkilenmiyor.
     */
    fun loadCourses(context: Context): MutableList<Course> = Onbellek.al(
        Onbellek.K_COURSES,
        kopyala = { liste: MutableList<Course> -> liste.mapTo(mutableListOf()) { it.copy() } },
        uret = { loadCoursesDisk(context) }
    )

    private fun loadCoursesDisk(context: Context): MutableList<Course> {
        val json = prefs(context).getString(KEY_COURSES, "[]") ?: "[]"
        val list = mutableListOf<Course>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                list.add(
                    Course(
                        id = o.optLong("id", now()),
                        title = o.optString("title", ""),
                        emoji = o.optString("emoji", "\uD83D\uDCD8"),
                        color = o.optInt("color", 0),
                        desc = o.optString("desc", ""),
                        createdAt = o.optLong("createdAt", now()),
                        order = o.optInt("order", 0)
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "loadCourses başarısız", e)
        }
        return list.sortedBy { it.order }.toMutableList()
    }

    fun saveCourses(context: Context, courses: List<Course>) {
        val array = JSONArray()
        courses.forEach { c ->
            array.put(
                JSONObject()
                    .put("id", c.id).put("title", c.title).put("emoji", c.emoji)
                    .put("color", c.color).put("desc", c.desc)
                    .put("createdAt", c.createdAt).put("order", c.order)
            )
        }
        prefs(context).edit().putString(KEY_COURSES, array.toString()).apply()
        Onbellek.boz(Onbellek.K_COURSES)
        widgetTazele(context)
    }

    fun addCourse(
        context: Context, title: String, emoji: String, color: Int, desc: String
    ): Course {
        val list = loadCourses(context)
        val course = Course(
            id = System.nanoTime(), title = title, emoji = emoji,
            color = color, desc = desc, createdAt = now(),
            order = (list.maxOfOrNull { it.order } ?: 0) + 1
        )
        list.add(course)
        saveCourses(context, list)
        return course
    }

    fun deleteCourse(context: Context, courseId: Long) {
        saveCourses(context, loadCourses(context).filter { it.id != courseId })
        saveSections(context, loadSections(context).filter { it.courseId != courseId })
        saveLessons(context, loadLessons(context).filter { it.courseId != courseId })
    }

    /**
     * v8.9 · Öneri 15 — Önbellekli okuma.
     *
     * `Section` düz bir veri sınıfı (iç içe koleksiyon yok), bu
     * yüzden `copy()` ile sığ kopya yeterli. Çağıranlar alanları
     * değiştirdiğinde önbellekteki asıl nesne etkilenmiyor.
     */
    fun loadSections(context: Context): MutableList<Section> = Onbellek.al(
        Onbellek.K_SECTIONS,
        kopyala = { liste: MutableList<Section> -> liste.mapTo(mutableListOf()) { it.copy() } },
        uret = { loadSectionsDisk(context) }
    )

    private fun loadSectionsDisk(context: Context): MutableList<Section> {
        val json = prefs(context).getString(KEY_SECTIONS, "[]") ?: "[]"
        val list = mutableListOf<Section>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                list.add(
                    Section(
                        id = o.optLong("id", now()),
                        courseId = o.optLong("courseId", 0L),
                        title = o.optString("title", ""),
                        order = o.optInt("order", 0)
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "loadSections başarısız", e)
        }
        return list
    }

    fun saveSections(context: Context, sections: List<Section>) {
        val array = JSONArray()
        sections.forEach { s ->
            array.put(
                JSONObject().put("id", s.id).put("courseId", s.courseId)
                    .put("title", s.title).put("order", s.order)
            )
        }
        prefs(context).edit().putString(KEY_SECTIONS, array.toString()).apply()
        Onbellek.boz(Onbellek.K_SECTIONS)
        widgetTazele(context)
    }

    fun addSection(context: Context, courseId: Long, title: String): Section {
        val list = loadSections(context)
        val mine = list.filter { it.courseId == courseId }
        val section = Section(
            id = System.nanoTime(), courseId = courseId, title = title,
            order = (mine.maxOfOrNull { it.order } ?: 0) + 1
        )
        list.add(section)
        saveSections(context, list)
        return section
    }

    fun deleteSection(context: Context, sectionId: Long) {
        saveSections(context, loadSections(context).filter { it.id != sectionId })
        saveLessons(context, loadLessons(context).filter { it.sectionId != sectionId })
    }

    /**
     * v8.9 · Öneri 15 — Önbellekli okuma.
     *
     * `Lesson` düz bir veri sınıfı (iç içe koleksiyon yok), bu
     * yüzden `copy()` ile sığ kopya yeterli. Çağıranlar alanları
     * değiştirdiğinde önbellekteki asıl nesne etkilenmiyor.
     */
    fun loadLessons(context: Context): MutableList<Lesson> = Onbellek.al(
        Onbellek.K_LESSONS,
        kopyala = { liste: MutableList<Lesson> -> liste.mapTo(mutableListOf()) { it.copy() } },
        uret = { loadLessonsDisk(context) }
    )

    private fun loadLessonsDisk(context: Context): MutableList<Lesson> {
        val json = prefs(context).getString(KEY_LESSONS, "[]") ?: "[]"
        val list = mutableListOf<Lesson>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                list.add(
                    Lesson(
                        id = o.optLong("id", now()),
                        courseId = o.optLong("courseId", 0L),
                        sectionId = o.optLong("sectionId", 0L),
                        title = o.optString("title", ""),
                        minutes = o.optInt("minutes", 0),
                        desc = o.optString("desc", ""),
                        link = o.optString("link", ""),
                        note = o.optString("note", ""),
                        done = o.optBoolean("done", false),
                        order = o.optInt("order", 0),
                        pdfAsset = o.optString("pdfAsset", ""),
                        fav = o.optBoolean("fav", false)
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "loadLessons başarısız", e)
        }
        return list
    }

    fun saveLessons(context: Context, lessons: List<Lesson>) {
        val array = JSONArray()
        lessons.forEach { l ->
            array.put(
                JSONObject().put("id", l.id).put("courseId", l.courseId)
                    .put("sectionId", l.sectionId).put("title", l.title)
                    .put("minutes", l.minutes).put("desc", l.desc)
                    .put("link", l.link).put("note", l.note)
                    .put("done", l.done).put("order", l.order)
                    .put("pdfAsset", l.pdfAsset).put("fav", l.fav)
            )
        }
        prefs(context).edit().putString(KEY_LESSONS, array.toString()).apply()
        Onbellek.boz(Onbellek.K_LESSONS)
        widgetTazele(context)
    }

    fun addLesson(
        context: Context, courseId: Long, sectionId: Long,
        title: String, minutes: Int, desc: String, pdfAsset: String = ""
    ): Lesson {
        val list = loadLessons(context)
        val mine = list.filter { it.sectionId == sectionId }
        val lesson = Lesson(
            id = System.nanoTime(), courseId = courseId, sectionId = sectionId,
            title = title, minutes = minutes, desc = desc, pdfAsset = pdfAsset,
            order = (mine.maxOfOrNull { it.order } ?: 0) + 1
        )
        list.add(lesson)
        saveLessons(context, list)
        return lesson
    }

    fun deleteLesson(context: Context, lessonId: Long) {
        saveLessons(context, loadLessons(context).filter { it.id != lessonId })
    }

    /** Bir dersin tamamlanma durumunu değiştirir; tamamlandıysa seriye yazar. */
    fun toggleLesson(context: Context, lessonId: Long): Boolean {
        val list = loadLessons(context)
        val lesson = list.firstOrNull { it.id == lessonId } ?: return false
        lesson.done = !lesson.done
        saveLessons(context, list)
        if (lesson.done) {
            recordCompletion(context)
            if (lesson.minutes > 0) addTodayFocusMinutes(context, lesson.minutes)
            kursGunuIsaretle(context)
            // v7.29: tamamlanan ders aralıklı tekrar programına girer
            try {
                QuizStore.tekraraAl(context, lessonId)
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Tekrara alınamadı", e)
            }
        }
        return lesson.done
    }

    // ─────────────────── v7.15: kurs çalışma serisi ───────────────────

    private const val KEY_SERI_GUN = "kurs_seri_gun"      // en son çalışılan gün (yyyyMMdd)
    private const val KEY_SERI_SAYI = "kurs_seri_sayi"    // üst üste kaç gün
    private const val KEY_SERI_REKOR = "kurs_seri_rekor"  // en uzun seri

    /** Bir tarihten n gün öncesinin gün anahtarı. */
    private fun gunAnahtari(gunOnce: Int): String {
        val c = java.util.Calendar.getInstance()
        c.add(java.util.Calendar.DAY_OF_YEAR, -gunOnce)
        return dayKey(c.time)
    }

    /**
     * v7.15: Bugün ders tamamlandığını işaretler ve seriyi günceller.
     * Dün çalışıldıysa seri artar, ara verildiyse 1'den başlar.
     */
    fun kursGunuIsaretle(context: Context) {
        val p = prefs(context)
        val bugun = dayKey()
        val sonGun = p.getString(KEY_SERI_GUN, "").orEmpty()
        if (sonGun == bugun) return  // bugün zaten sayıldı

        val dun = gunAnahtari(1)
        val yeniSeri = if (sonGun == dun) p.getInt(KEY_SERI_SAYI, 0) + 1 else 1
        val rekor = maxOf(p.getInt(KEY_SERI_REKOR, 0), yeniSeri)
        p.edit()
            .putString(KEY_SERI_GUN, bugun)
            .putInt(KEY_SERI_SAYI, yeniSeri)
            .putInt(KEY_SERI_REKOR, rekor)
            .apply()
    }

    /** v7.15: kurs çalışma serisi bilgisi. */
    data class KursSeri(
        val gunSayisi: Int,
        val rekor: Int,
        val bugunCalisildi: Boolean,
        val dunCalisildi: Boolean
    ) {
        /** Seri kopmuş mu — ne bugün ne dün çalışılmış. */
        val kopuk: Boolean get() = !bugunCalisildi && !dunCalisildi && gunSayisi > 0
    }

    /** v7.15: güncel seri durumunu okur. */
    fun kursSeri(context: Context): KursSeri {
        val p = prefs(context)
        val sonGun = p.getString(KEY_SERI_GUN, "").orEmpty()
        val bugun = dayKey()
        val dun = gunAnahtari(1)
        val ham = p.getInt(KEY_SERI_SAYI, 0)
        val bugunVar = sonGun == bugun
        val dunVar = sonGun == dun
        // Ara verildiyse seri fiilen sıfırdır
        val gecerli = if (bugunVar || dunVar) ham else 0
        return KursSeri(
            gunSayisi = gecerli,
            rekor = p.getInt(KEY_SERI_REKOR, 0),
            bugunCalisildi = bugunVar,
            dunCalisildi = dunVar
        )
    }

    /** v7.15: son 7 günün çalışma haritası — bugün en sonda. */
    fun kursSonYediGun(context: Context): List<Boolean> {
        val p = prefs(context)
        val sonGun = p.getString(KEY_SERI_GUN, "").orEmpty()
        val seri = p.getInt(KEY_SERI_SAYI, 0)
        // Seri kesintisiz olduğu için son N günü geriye doğru işaretleyebiliriz
        val isaretli = mutableSetOf<String>()
        if (sonGun.isNotBlank() && seri > 0) {
            // sonGun'ün kaç gün önce olduğunu bul
            var offset = -1
            for (i in 0..7) if (gunAnahtari(i) == sonGun) { offset = i; break }
            if (offset >= 0) {
                for (k in 0 until seri) {
                    val g = offset + k
                    if (g > 6) break
                    isaretli.add(gunAnahtari(g))
                }
            }
        }
        return (6 downTo 0).map { isaretli.contains(gunAnahtari(it)) }
    }

    fun sectionsOf(context: Context, courseId: Long): List<Section> =
        loadSections(context).filter { it.courseId == courseId }.sortedBy { it.order }

    fun lessonsOf(context: Context, sectionId: Long): List<Lesson> =
        loadLessons(context).filter { it.sectionId == sectionId }.sortedBy { it.order }

    /** v7.8: dersin yer imi durumunu değiştirir, yeni durumu döndürür. */
    fun toggleLessonFav(context: Context, lessonId: Long): Boolean {
        val list = loadLessons(context)
        val lesson = list.firstOrNull { it.id == lessonId } ?: return false
        lesson.fav = !lesson.fav
        saveLessons(context, list)
        return lesson.fav
    }

    /** v7.12: en son açılan dersi kaydeder (hızlı devam için). */
    fun setSonDers(context: Context, lessonId: Long) {
        prefs(context).edit().putLong("son_ders_id", lessonId).apply()
    }

    /** v7.12: en son açılan ders — yoksa ilk tamamlanmamış ders. */
    fun sonDers(context: Context): Lesson? {
        val dersler = loadLessons(context)
        if (dersler.isEmpty()) return null
        val id = prefs(context).getLong("son_ders_id", 0L)
        return dersler.firstOrNull { it.id == id }
            ?: dersler.filter { !it.done }.minByOrNull { it.order }
    }

    /** v7.11: kurs çalışma istatistikleri. */
    data class KursIstatistik(
        val toplamDers: Int,
        val bitenDers: Int,
        val toplamDakika: Int,
        val bitenDakika: Int,
        val yerImi: Int,
        val pdfliDers: Int
    ) {
        val yuzde: Int get() = if (toplamDers == 0) 0 else bitenDers * 100 / toplamDers
        val kalanDakika: Int get() = (toplamDakika - bitenDakika).coerceAtLeast(0)
        /** Günde verilen dakika ile kaç günde biter. */
        fun kalanGun(gunlukDakika: Int): Int =
            if (gunlukDakika <= 0) 0 else (kalanDakika + gunlukDakika - 1) / gunlukDakika
    }

    /** v7.11: tüm kursların toplu istatistiği. */
    fun kursIstatistik(context: Context): KursIstatistik {
        val dersler = loadLessons(context)
        return KursIstatistik(
            toplamDers = dersler.size,
            bitenDers = dersler.count { it.done },
            toplamDakika = dersler.sumOf { it.minutes },
            bitenDakika = dersler.filter { it.done }.sumOf { it.minutes },
            yerImi = dersler.count { it.fav },
            pdfliDers = dersler.count { it.pdfAsset.isNotBlank() }
        )
    }

    /** v7.8: yer imine eklenmiş tüm dersler. */
    fun favLessons(context: Context): List<Lesson> =
        loadLessons(context).filter { it.fav }

    /** v7.14: dersin notunu kaydeder. Boş metin notu siler. */
    fun setLessonNote(context: Context, lessonId: Long, note: String) {
        val list = loadLessons(context)
        val lesson = list.firstOrNull { it.id == lessonId } ?: return
        lesson.note = note.trim()
        saveLessons(context, list)
    }

    /** v7.14: dersin kayıtlı notu (yoksa boş metin). */
    fun lessonNote(context: Context, lessonId: Long): String =
        loadLessons(context).firstOrNull { it.id == lessonId }?.note.orEmpty()

    /** v7.14: notu olan tüm dersler — not defteri görünümü için. */
    fun notluDersler(context: Context): List<Lesson> =
        loadLessons(context).filter { it.note.isNotBlank() }.sortedBy { it.order }

    // ═══════════════════════════════════════════════════════════════
    // v7.20 — Derse eklenen internet kaynakları (PDF / video / sayfa)
    // ═══════════════════════════════════════════════════════════════

    private const val KEY_KAYNAKLAR = "ders_kaynaklari_json"

    /** Bir derse eklenmiş internet kaynağı. */
    data class DersKaynak(
        val id: Long,
        val lessonId: Long,
        var baslik: String,
        var url: String,
        var tur: String,          // "pdf" | "video" | "sayfa"
        var aciklama: String = "",
        var kanal: String = "",
        val eklendi: Long = System.currentTimeMillis(),
        /** İndirildiyse cihazdaki dosya yolu. */
        var yerelDosya: String = ""
    )

    fun loadKaynaklar(context: Context): MutableList<DersKaynak> {
        val ham = prefs(context).getString(KEY_KAYNAKLAR, "[]") ?: "[]"
        val liste = mutableListOf<DersKaynak>()
        try {
            val dizi = JSONArray(ham)
            for (i in 0 until dizi.length()) {
                val o = dizi.optJSONObject(i) ?: continue
                liste.add(
                    DersKaynak(
                        id = o.optLong("id"),
                        lessonId = o.optLong("lessonId"),
                        baslik = o.optString("baslik"),
                        url = o.optString("url"),
                        tur = o.optString("tur", "sayfa"),
                        aciklama = o.optString("aciklama", ""),
                        kanal = o.optString("kanal", ""),
                        eklendi = o.optLong("eklendi", 0L),
                        yerelDosya = o.optString("yerelDosya", "")
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Kaynaklar okunamadı", e)
        }
        return liste
    }

    fun saveKaynaklar(context: Context, liste: List<DersKaynak>) {
        try {
            val dizi = JSONArray()
            liste.forEach { k ->
                dizi.put(
                    JSONObject()
                        .put("id", k.id)
                        .put("lessonId", k.lessonId)
                        .put("baslik", k.baslik)
                        .put("url", k.url)
                        .put("tur", k.tur)
                        .put("aciklama", k.aciklama)
                        .put("kanal", k.kanal)
                        .put("eklendi", k.eklendi)
                        .put("yerelDosya", k.yerelDosya)
                )
            }
            prefs(context).edit().putString(KEY_KAYNAKLAR, dizi.toString()).apply()
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Kaynaklar kaydedilemedi", e)
        }
    }

    /** Derse yeni kaynak ekler. Aynı adres varsa tekrar eklemez. */
    fun kaynakEkle(
        context: Context,
        lessonId: Long,
        baslik: String,
        url: String,
        tur: String,
        aciklama: String = "",
        kanal: String = ""
    ): DersKaynak? {
        val liste = loadKaynaklar(context)
        val sade = url.trim().trimEnd('/').lowercase()
        if (liste.any { it.lessonId == lessonId && it.url.trim().trimEnd('/').lowercase() == sade }) {
            return null   // zaten var
        }
        val yeni = DersKaynak(
            id = System.currentTimeMillis() + liste.size,
            lessonId = lessonId,
            baslik = baslik.trim().ifBlank { url.take(50) },
            url = url.trim(),
            tur = tur,
            aciklama = aciklama.trim(),
            kanal = kanal.trim()
        )
        liste.add(yeni)
        saveKaynaklar(context, liste)
        return yeni
    }

    fun kaynaklariOf(context: Context, lessonId: Long): List<DersKaynak> =
        loadKaynaklar(context).filter { it.lessonId == lessonId }
            .sortedByDescending { it.eklendi }

    fun kaynakSil(context: Context, kaynakId: Long): DersKaynak? {
        val liste = loadKaynaklar(context)
        val silinen = liste.firstOrNull { it.id == kaynakId } ?: return null
        liste.remove(silinen)
        saveKaynaklar(context, liste)
        return silinen
    }

    /** Silinen kaynağı geri koyar (geri alma için). */
    fun kaynakGeriEkle(context: Context, kaynak: DersKaynak) {
        val liste = loadKaynaklar(context)
        if (liste.none { it.id == kaynak.id }) {
            liste.add(kaynak)
            saveKaynaklar(context, liste)
        }
    }

    /** Kaynağı olan tüm dersler — kaynak merkezinde listelemek için. */
    fun kaynakliDersler(context: Context): Map<Long, List<DersKaynak>> =
        loadKaynaklar(context).groupBy { it.lessonId }

    fun kaynakSayisi(context: Context): Int = loadKaynaklar(context).size

    /** Kursun ilerlemesi: tamamlanan ders / toplam ders. */
    fun courseProgress(context: Context, courseId: Long): Pair<Int, Int> {
        val mine = loadLessons(context).filter { it.courseId == courseId }
        return mine.count { it.done } to mine.size
    }

    /** Kursun toplam ve tamamlanan süresi (dakika). */
    fun courseMinutes(context: Context, courseId: Long): Pair<Int, Int> {
        val mine = loadLessons(context).filter { it.courseId == courseId }
        return mine.filter { it.done }.sumOf { it.minutes } to mine.sumOf { it.minutes }
    }

    // ═══════════════════════════════════════════════════════════════════
    // v7.7 — Geri alma (undo) desteği
    // Silinen veriler kısa süre bellekte tutulur, kullanıcı geri alabilir.
    // ═══════════════════════════════════════════════════════════════════

    /** Son silme işleminin geri alma bilgisi. */
    private var sonSilme: (() -> Unit)? = null

    /** Bir geri alma işlemi kaydeder. */
    fun kaydetGeriAlma(islem: () -> Unit) {
        sonSilme = islem
    }

    /** Son silmeyi geri alır. Başarılıysa true. */
    fun geriAl(): Boolean {
        val islem = sonSilme ?: return false
        sonSilme = null
        return try {
            islem()
            true
        } catch (e: Exception) {
            android.util.Log.w("Store", "Geri alma başarısız", e)
            false
        }
    }

    /** Geri alınabilecek bir işlem var mı. */
    fun geriAlinabilir(): Boolean = sonSilme != null

    /** Kursu geri alınabilir biçimde siler. */
    fun deleteCourseUndoable(context: Context, courseId: Long) {
        val kurslar = loadCourses(context)
        val bolumler = loadSections(context)
        val dersler = loadLessons(context)
        val yedekKurs = kurslar.filter { it.id == courseId }
        val yedekBolum = bolumler.filter { it.courseId == courseId }
        val yedekDers = dersler.filter { it.courseId == courseId }
        deleteCourse(context, courseId)
        kaydetGeriAlma {
            val k = loadCourses(context); k.addAll(yedekKurs); saveCourses(context, k)
            val b = loadSections(context); b.addAll(yedekBolum); saveSections(context, b)
            val d = loadLessons(context); d.addAll(yedekDers); saveLessons(context, d)
        }
    }

    /** Bölümü geri alınabilir biçimde siler. */
    fun deleteSectionUndoable(context: Context, sectionId: Long) {
        val yedekBolum = loadSections(context).filter { it.id == sectionId }
        val yedekDers = loadLessons(context).filter { it.sectionId == sectionId }
        deleteSection(context, sectionId)
        kaydetGeriAlma {
            val b = loadSections(context); b.addAll(yedekBolum); saveSections(context, b)
            val d = loadLessons(context); d.addAll(yedekDers); saveLessons(context, d)
        }
    }

    /** Dersi geri alınabilir biçimde siler. */
    fun deleteLessonUndoable(context: Context, lessonId: Long) {
        val yedek = loadLessons(context).filter { it.id == lessonId }
        deleteLesson(context, lessonId)
        kaydetGeriAlma {
            val d = loadLessons(context); d.addAll(yedek); saveLessons(context, d)
        }
    }

    /** Görevi geri alınabilir biçimde siler. */
    fun deleteTaskUndoable(context: Context, taskId: Long) {
        val tasks = loadTasks(context)
        val yedek = tasks.filter { it.id == taskId }
        saveTasks(context, tasks.filter { it.id != taskId })
        kaydetGeriAlma {
            val t = loadTasks(context); t.addAll(yedek); saveTasks(context, t)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // v7.75 — ARŞİV
    // ═══════════════════════════════════════════════════════════════
    //
    // Tamamlanan görevler listede birikiyordu. Silmek istatistiği bozuyor,
    // tutmak listeyi şişiriyordu. Arşiv ikisinin arasını buluyor:
    // görev listeden çıkar ama veri durur.

    /** Arşivdeki görevler — en son arşivlenen üstte. */
    fun arsivGorevleri(context: Context): List<Task> =
        loadTasks(context).filter { it.arsiv }.sortedByDescending { it.arsivZaman }

    /** Ana listede görünecek görevler (arşivlenmemiş). */
    fun aktifGorevler(context: Context): List<Task> =
        loadTasks(context).filter { !it.arsiv }

    /** Tamamlanmış ve henüz arşivlenmemiş görev sayısı. */
    fun arsivlenebilirSayi(context: Context): Int =
        loadTasks(context).count { it.done && !it.arsiv && !it.tekrarliMi }

    /**
     * Tamamlanan görevleri arşive taşır.
     *
     * Tekrarlı görevler atlanır — onlar zaten sürekli yenileniyor,
     * arşivlenirse zincir kopar.
     *
     * @return taşınan görev sayısı
     */
    fun bitenleriArsivle(context: Context): Int {
        val liste = loadTasks(context)
        val hedef = liste.filter { it.done && !it.arsiv && !it.tekrarliMi }
        if (hedef.isEmpty()) return 0
        val simdi = now()
        hedef.forEach { it.arsiv = true; it.arsivZaman = simdi }
        saveTasks(context, liste)
        val idler = hedef.map { it.id }.toSet()
        kaydetGeriAlma {
            val l = loadTasks(context)
            l.filter { idler.contains(it.id) }.forEach { it.arsiv = false; it.arsivZaman = 0L }
            saveTasks(context, l)
        }
        return hedef.size
    }

    /** Tek görevi arşive taşır / arşivden çıkarır. */
    fun arsiveTasi(context: Context, taskId: Long, arsivle: Boolean) {
        val liste = loadTasks(context)
        liste.firstOrNull { it.id == taskId }?.apply {
            arsiv = arsivle
            arsivZaman = if (arsivle) now() else 0L
        }
        saveTasks(context, liste)
    }

    /** Arşivi kalıcı olarak siler. */
    fun arsiviTemizle(context: Context): Int {
        val liste = loadTasks(context)
        val silinecek = liste.filter { it.arsiv }
        if (silinecek.isEmpty()) return 0
        saveTasks(context, liste.filter { !it.arsiv })
        kaydetGeriAlma {
            val l = loadTasks(context); l.addAll(silinecek); saveTasks(context, l)
        }
        return silinecek.size
    }

    /** Bu ay tamamlanan görev sayısı (arşiv dahil). */
    fun buAyBitenGorev(context: Context): Int {
        val ayBasi = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis
        return loadTasks(context).count {
            it.done && (it.arsivZaman >= ayBasi || it.createdAt >= ayBasi)
        }
    }

    /** v7.72: Notu geri alınabilir biçimde siler. */
    fun deleteNoteUndoable(context: Context, noteId: Long) {
        val hepsi = loadNotes(context)
        val yedek = hepsi.filter { it.id == noteId }
        saveNotes(context, hepsi.filter { it.id != noteId })
        kaydetGeriAlma {
            val liste = loadNotes(context); liste.addAll(yedek); saveNotes(context, liste)
        }
    }

    /** v7.72: Konuyu geri alınabilir biçimde siler. */
    fun deleteTopicUndoable(context: Context, topicId: Long) {
        val hepsi = loadTopics(context)
        val yedek = hepsi.filter { it.id == topicId }
        saveTopics(context, hepsi.filter { it.id != topicId })
        kaydetGeriAlma {
            val liste = loadTopics(context); liste.addAll(yedek); saveTopics(context, liste)
        }
    }

    /** v7.72: Birden çok görevi tek işlemde, geri alınabilir siler. */
    fun deleteTasksUndoable(context: Context, ids: Set<Long>) {
        if (ids.isEmpty()) return
        val hepsi = loadTasks(context)
        val yedek = hepsi.filter { ids.contains(it.id) }
        saveTasks(context, hepsi.filter { !ids.contains(it.id) })
        kaydetGeriAlma {
            val liste = loadTasks(context); liste.addAll(yedek); saveTasks(context, liste)
        }
    }

    /** v7.72: Toplu görev güncellemesini geri alınabilir yapar. */
    fun gorevleriGuncelleUndoable(
        context: Context,
        ids: Set<Long>,
        degistir: (Task) -> Unit
    ) {
        if (ids.isEmpty()) return
        val oncekiler = loadTasks(context).filter { ids.contains(it.id) }.map { it.copy() }
        val liste = loadTasks(context)
        liste.filter { ids.contains(it.id) }.forEach(degistir)
        saveTasks(context, liste)
        kaydetGeriAlma {
            val simdi = loadTasks(context)
            oncekiler.forEach { eski ->
                val i = simdi.indexOfFirst { it.id == eski.id }
                if (i >= 0) simdi[i] = eski
            }
            saveTasks(context, simdi)
        }
    }

    /** Alışkanlığı geri alınabilir biçimde siler. */
    fun deleteHabitUndoable(context: Context, habitId: Long) {
        val yedek = loadHabits(context).filter { it.id == habitId }
        deleteHabit(context, habitId)
        kaydetGeriAlma {
            val h = loadHabits(context); h.addAll(yedek); saveHabits(context, h)
        }
    }

    /** v7.9: Kitabı (ve bölümlerini) geri alınabilir biçimde siler. */
    fun deleteBookUndoable(context: Context, bookId: Long) {
        val hepsi = loadBooks(context)
        // kitabın kendisi + varsa bölümleri
        val yedek = hepsi.filter { it.id == bookId || it.parentId == bookId }
        saveBooks(context, hepsi.filter { it.id != bookId && it.parentId != bookId })
        kaydetGeriAlma {
            val b = loadBooks(context)
            b.addAll(yedek)
            saveBooks(context, b)
        }
    }

    /** Etkinliği geri alınabilir biçimde siler. */
    fun deleteEventUndoable(context: Context, eventId: Long) {
        val yedek = loadEvents(context).filter { it.id == eventId }
        deleteEvent(context, eventId)
        kaydetGeriAlma {
            val e = loadEvents(context); e.addAll(yedek); saveEvents(context, e)
        }
    }
}
