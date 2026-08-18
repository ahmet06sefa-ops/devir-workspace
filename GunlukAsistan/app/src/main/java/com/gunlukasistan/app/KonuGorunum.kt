package com.gunlukasistan.app

import android.content.Context
import org.json.JSONObject

/**
 * v8.3 — Konu renk kodu ve simgesi (öneri 13).
 *
 * ── Sorun ──
 * Konular listesi 30 satıra çıkınca hepsi birbirine benziyordu: aynı
 * beyaz kart, aynı yazı tipi, tek fark başlık metni. Aradığını bulmak
 * için okumak gerekiyordu. Görevlerde etiket rengi şeridi vardı
 * (`etiketSerit`, v7.74) ve işe yarıyordu; konularda yoktu.
 *
 * ── Neden `Store.Topic` modeline alan EKLENMEDİ ──
 * `Topic` sınıfı JSON'a serileştiriliyor, yedeğe giriyor, Room
 * geçişinde kullanılıyor ve 12 dosyada okunuyor. Oraya alan eklemek
 * `exportJson` sürümünü artırmayı, geriye dönük okuma dalı yazmayı ve
 * 12 dosyayı gözden geçirmeyi gerektirirdi — tek bir renk için ağır
 * bir bedel. Bunun yerine kimlik → görünüm eşlemesi ayrı bir depoda.
 * Konu silinirse burada artık bir kayıt kalır; zararsız (birkaç bayt)
 * ve [temizle] ile toplanabiliyor.
 *
 * ── Renk paleti neden sabit ──
 * Rastgele renk seçtirmek karmaşa yaratıyor; 10 renklik özenli bir
 * palet hem uyumlu duruyor hem de koyu/açık temanın ikisinde de
 * okunabiliyor (hepsi orta doygunlukta, ne çok soluk ne çok parlak).
 */
object KonuGorunum {

    private const val TAG = "KonuGorunum"
    private const val PREF = "konu_gorunum_v1"

    private const val K_RENK = "renk_"
    private const val K_SIMGE = "simge_"

    /** Kart kenarında ve rozette kullanılan renkler. */
    val RENKLER = intArrayOf(
        0xFFB0685A.toInt(),  // kiremit
        0xFFD08C3E.toInt(),  // amber
        0xFF9AA234.toInt(),  // zeytin
        0xFF4E9A62.toInt(),  // yeşil
        0xFF3A9AA0.toInt(),  // deniz
        0xFF4B7FC4.toInt(),  // mavi
        0xFF7A6BC7.toInt(),  // lavanta
        0xFFB05FA0.toInt(),  // orkide
        0xFFC25E7C.toInt(),  // gül
        0xFF7C7C86.toInt()   // gri
    )

    /** Konuya atanabilecek simgeler. */
    val SIMGELER = arrayOf(
        "📘", "📐", "🧮", "🔬", "⚗️", "🧬", "🌍", "🏛", "⚖️", "💻",
        "🏗", "⚙️", "🔌", "📊", "💡", "🎯", "🗣", "🎨", "🩺", "📎"
    )

    private fun p(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ------------------------------------------------------------------

    /** Konunun renk indeksi. -1 = atanmamış. */
    fun renkIndeksi(c: Context, konuId: Long): Int =
        p(c).getInt("$K_RENK$konuId", -1)

    /** Gerçek renk değeri. Atanmamışsa kimlikten türetilir. */
    fun renk(c: Context, konuId: Long): Int {
        val i = renkIndeksi(c, konuId)
        if (i in RENKLER.indices) return RENKLER[i]
        // Atanmamışsa: kimliğe göre kararlı bir renk. Böylece kullanıcı
        // hiç renk seçmese bile liste tekdüze kalmıyor, ama aynı konu
        // her açılışta aynı rengi alıyor.
        val h = (konuId % RENKLER.size).toInt()
        return RENKLER[if (h < 0) h + RENKLER.size else h]
    }

    /** Kullanıcı bilinçli renk seçti mi? */
    fun renkAtandiMi(c: Context, konuId: Long): Boolean =
        renkIndeksi(c, konuId) in RENKLER.indices

    fun renkAta(c: Context, konuId: Long, indeks: Int) {
        p(c).edit().putInt("$K_RENK$konuId", indeks).apply()
    }

    /** Konunun simgesi. Boş = atanmamış. */
    fun simge(c: Context, konuId: Long): String =
        p(c).getString("$K_SIMGE$konuId", "") ?: ""

    fun simgeAta(c: Context, konuId: Long, simge: String) {
        p(c).edit().putString("$K_SIMGE$konuId", simge).apply()
    }

    /** Konunun görünüm ayarlarını siler. */
    fun sifirla(c: Context, konuId: Long) {
        p(c).edit().remove("$K_RENK$konuId").remove("$K_SIMGE$konuId").apply()
    }

    /**
     * Artık var olmayan konulara ait kayıtları siler.
     *
     * Konu silindiğinde burada kayıt kalıyor. Tek başına zararsız ama
     * yıllar içinde birikir; açılışta bir kez temizlemek yeter.
     */
    fun temizle(c: Context, mevcutIdler: Set<Long>) {
        runCatching {
            val d = p(c)
            val silinecek = d.all.keys.filter { anahtar ->
                val id = anahtar.substringAfter('_').toLongOrNull() ?: return@filter false
                id !in mevcutIdler
            }
            if (silinecek.isEmpty()) return
            val e = d.edit()
            silinecek.forEach { e.remove(it) }
            e.apply()
        }.onFailure { android.util.Log.w(TAG, "temizle", it) }
    }

    // ------------------------------------------------------------------

    /** Konu başlığını simgesiyle birlikte döndürür. */
    fun baslikla(c: Context, konuId: Long, baslik: String): String {
        val s = simge(c, konuId)
        return if (s.isBlank()) baslik else "$s  $baslik"
    }

    // ------------------------------------------------------------------
    // Yedek köprüsü — PrefYedek listesine "konu_gorunum_v1" eklendi,
    // ayrıca elle dışa/içe aktarıma gerek yok.
    // ------------------------------------------------------------------

    /** Tanılama için özet. */
    fun ozet(c: Context): JSONObject = JSONObject().apply {
        runCatching {
            val d = p(c).all
            put("renk_sayisi", d.keys.count { it.startsWith(K_RENK) })
            put("simge_sayisi", d.keys.count { it.startsWith(K_SIMGE) })
        }
    }
}
