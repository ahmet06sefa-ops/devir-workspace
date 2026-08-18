package com.gunlukasistan.app

/**
 * v8.9 — Bellek içi veri önbelleği (öneri 15).
 *
 * ══════════════════════════════════════════════════════════════════
 * ÖLÇÜLEN SORUN
 * ══════════════════════════════════════════════════════════════════
 * `Store.loadTopics` 32 yerde, `loadTasks` 31 yerde, `loadLessons`
 * 29 yerde çağrılıyor. Her çağrı şunları yapıyor:
 *
 *   1. SharedPreferences'tan metin oku (disk)
 *   2. `JSONArray(json)` — tüm metni ayrıştır
 *   3. Her öğe için `JSONObject` oku, veri sınıfına dönüştür
 *
 * Bir ekran tazelendiğinde bu iş defalarca tekrarlanıyor. Örnek:
 * `HomeFragment.bindData()` içinde `loadTopics` bir kez çağrılıyor,
 * ama `TodayFragment`, `ProgressFragment` ve widget'lar da aynı
 * veriyi ayrı ayrı okuyor. 200 maddelik bir konu listesinde her
 * okuma yüzlerce nesne yaratıyor.
 *
 * ══════════════════════════════════════════════════════════════════
 * ÇÖZÜM VE SINIRLARI
 * ══════════════════════════════════════════════════════════════════
 * Basit bir anahtar→değer önbelleği. Yazma işlemi önbelleği
 * geçersiz kılıyor.
 *
 * ── Neden LruCache değil ──
 * Önbelleklenen şey sabit sayıda liste (11 tür). LRU'nun tahliye
 * mantığına gerek yok; hepsi bellekte kalabilir. Toplam boyut
 * kullanıcının verisi kadar — zaten diskten okunup nesneye
 * dönüştürülüyordu, aynı nesneler tutuluyor.
 *
 * ── 🔴 KRİTİK: dönen liste MUTABLE ──
 * `Store.loadTasks` `MutableList` döndürüyor ve çağıranlar bunu
 * değiştiriyor (`tasks.clear()`, `task.done = true`). Önbellekten
 * AYNI nesneyi dönersek bir ekrandaki değişiklik diğerini de
 * etkiler — hayalet hatalar doğar.
 *
 * Bu yüzden [al] her seferinde **kopya** döndürüyor. Kazanç yine de
 * büyük: JSON ayrıştırma (pahalı) bir kez yapılıyor, kopyalama
 * (ucuz) her seferinde.
 *
 * ── İş parçacığı güvenliği ──
 * Widget'lar arka planda okuyor, ekranlar ana iş parçacığında.
 * `synchronized` ile korunuyor.
 */
object Onbellek {

    private const val TAG = "Onbellek"

    private val kilit = Any()
    private val veri = HashMap<String, Any>()
    private val zaman = HashMap<String, Long>()

    /** İstatistik — tanılama ekranında gösteriliyor. */
    @Volatile var isabet = 0L
        private set
    @Volatile var kacak = 0L
        private set

    /**
     * Önbellekten okur, yoksa [uret] ile üretip saklar.
     *
     * @param kopyala önbellekteki nesneden bağımsız kopya üretir.
     *   Değiştirilebilir listeler için ZORUNLU.
     */
    fun <T : Any> al(anahtar: String, kopyala: (T) -> T, uret: () -> T): T {
        synchronized(kilit) {
            @Suppress("UNCHECKED_CAST")
            val mevcut = veri[anahtar] as? T
            if (mevcut != null) {
                isabet++
                return kopyala(mevcut)
            }
        }
        // Üretimi kilit DIŞINDA yap: disk okuma uzun sürebilir,
        // bu sırada başka iş parçacıklarını bloklamayalım.
        val yeni = uret()
        synchronized(kilit) {
            veri[anahtar] = yeni
            zaman[anahtar] = System.currentTimeMillis()
            kacak++
        }
        return kopyala(yeni)
    }

    /** Bir anahtarı geçersiz kılar (yazma sonrası). */
    fun boz(anahtar: String) {
        synchronized(kilit) {
            veri.remove(anahtar)
            zaman.remove(anahtar)
        }
    }

    /** Birden çok anahtarı geçersiz kılar. */
    fun boz(vararg anahtarlar: String) {
        synchronized(kilit) {
            anahtarlar.forEach { veri.remove(it); zaman.remove(it) }
        }
    }

    /**
     * Tümünü temizler.
     *
     * Yedek geri yükleme, tema değişimi gibi her şeyin değiştiği
     * durumlarda çağrılıyor.
     */
    fun hepsiniBoz() {
        synchronized(kilit) {
            veri.clear()
            zaman.clear()
        }
    }

    /** Şu an önbellekte kaç kayıt var. */
    fun boyut(): Int = synchronized(kilit) { veri.size }

    /** İsabet oranı (tanılama). */
    fun isabetOrani(): Int {
        val toplam = isabet + kacak
        return if (toplam == 0L) 0 else (isabet * 100 / toplam).toInt()
    }

    fun istatistikSifirla() {
        isabet = 0; kacak = 0
    }

    // ══════════════════════════════════════════════════════════
    // Anahtar sabitleri — yazım hatası olmasın
    // ══════════════════════════════════════════════════════════

    const val K_TOPICS = "topics"
    const val K_TASKS = "tasks"
    const val K_NOTES = "notes"
    const val K_COURSES = "courses"
    const val K_LESSONS = "lessons"
    const val K_SECTIONS = "sections"
    const val K_HABITS = "habits"
    const val K_EXAMS = "exams"
    const val K_EVENTS = "events"
    const val K_KAYNAKLAR = "kaynaklar"
    const val K_LOG = "gunluk_log"
}
