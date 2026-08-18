package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * v10.7 · Öneri A6 — Çok aşamalı zincir sayaç.
 *
 * ── Ne eksikti ──
 * [Pomodoro] sabit üç evrelidir ve kuralı koda gömülüdür: çalışma,
 * kısa mola, uzun mola. Kullanıcının "Tabata yapayım", "ısınma →
 * sprint → yürüyüş dizisi", "okuma → not alma → tekrar" gibi
 * kendi dizisini kurmasının yolu yoktu; evre sayısı, süreleri ve
 * sırası değiştirilemezdi.
 *
 * ── Bu ne yapar ──
 * [Zincir] ad üzerinden keyfi evre dizileri tanımlanır, [tekrar]
 * kadar üst üste koşulur. Bir evre bitince sıradaki otomatik
 * kurulur (arka planda [TimerActionReceiver] üzerinden). Süre
 * birimi **saniye**: Tabata'nın 20/10 ritmi dakika tabanlı
 * sistemle imkânsızdı.
 *
 * ── Pomodoro ile ilişki ──
 * Zincir koşarken öncelik zincirindir ([TimerActionReceiver]
 * önce zincire bakar). Kullanıcının pomodoro ayarları
 * değiştirilmez; zincir kapanınca pomodoro kaldığı yerden
 * çalışmaya devam eder.
 *
 * ── Neden JSON ──
 * Evre listesi değişken uzunlukta; anahtar-değer düzlüğüne
 * sığmaz. `org.json` Android'de hazır — ek bağımlılık yok.
 * Şablon adları veri kataloğu olarak burada Türkçe literalle
 * tutulur; arayüz metinleri (diyalog, bildirim, toast) yine
 * strings.xml'den gelir.
 */
object SayacZincir {

    private const val PREF = "sayac_zincir_v1"

    private const val K_KAYITLI = "kayitli"
    private const val K_AKTIF_ID = "aktif_id"
    private const val K_ADIM = "adim"
    private const val K_KOSUYOR = "kosuyor"
    private const val K_OTO = "oto_devam"
    private const val K_SONRAKI_ID = "sonraki_id"

    /** Aktif zincir yok işareti. */
    const val ID_YOK = 0L

    /** Bir zincirdeki en fazla evre — diyalogdaki satır sınırıyla aynı. */
    const val MAKS_EVRE = 8

    const val MAKS_TEKRAR = 10

    /** Tek evrenin süre sınırları (saniye): Tabata 20 sn → uzun oturum 2 sa. */
    const val MIN_EVRE_SN = 5
    const val MAKS_EVRE_SN = 7200

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ═══════════════════════════════════════════════════════════════
    // VERİ MODELİ
    // ═══════════════════════════════════════════════════════════════

    /**
     * Tek evre: ad, süre (saniye), odak sayılması.
     *
     * [odakMi] false olan evreler (mola, dinlenme, ısınma…) ders
     * kaydına yazılmaz — [TimerActionReceiver.odagiKaydet] buna bakar.
     */
    data class Evre(
        val ad: String,
        val emoji: String,
        val sn: Int,
        val odakMi: Boolean = true
    ) {
        fun json(): JSONObject = JSONObject()
            .put("ad", ad)
            .put("emoji", emoji)
            .put("sn", sn)
            .put("odak", odakMi)

        companion object {
            fun jsondan(o: JSONObject): Evre? {
                val ad = o.optString("ad", "").trim()
                val sn = o.optInt("sn", 0)
                if (ad.isEmpty() || sn <= 0) return null
                return Evre(
                    ad = ad.take(18),
                    emoji = o.optString("emoji", "").take(4),
                    sn = sn.coerceIn(MIN_EVRE_SN, MAKS_EVRE_SN),
                    odakMi = o.optBoolean("odak", true)
                )
            }
        }
    }

    /** Evre dizisi + tekrar sayısı + kimlik. */
    data class Zincir(
        val id: Long,
        val ad: String,
        val emoji: String,
        val evreler: List<Evre>,
        val tekrar: Int
    ) {
        /** Koşu boyunca atılacak toplam adım (evre × tekrar). */
        val toplamAdim: Int get() = evreler.size * tekrar

        /** Toplam süre (saniye) — şablon listesinde gösterilir. */
        val toplamSn: Int get() = evreler.sumOf { it.sn } * tekrar

        fun json(): JSONObject {
            val dizi = JSONArray()
            evreler.forEach { dizi.put(it.json()) }
            return JSONObject()
                .put("id", id)
                .put("ad", ad)
                .put("emoji", emoji)
                .put("tekrar", tekrar)
                .put("evreler", dizi)
        }

        companion object {
            fun jsondan(o: JSONObject): Zincir? {
                val evreDizi = o.optJSONArray("evreler") ?: return null
                val evreler = mutableListOf<Evre>()
                for (i in 0 until evreDizi.length()) {
                    evreDizi.optJSONObject(i)?.let { Evre.jsondan(it) }?.let { evreler.add(it) }
                }
                if (evreler.isEmpty()) return null
                val ad = o.optString("ad", "").trim().ifEmpty { "Zincir" }
                return Zincir(
                    id = o.optLong("id", ID_YOK),
                    ad = ad.take(24),
                    emoji = o.optString("emoji", "⛓").take(4),
                    evreler = evreler,
                    tekrar = o.optInt("tekrar", 1).coerceIn(1, MAKS_TEKRAR)
                )
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // SAF MANTIK — ilerleme, doğrulama, tahmin
    // ═══════════════════════════════════════════════════════════════

    /**
     * Düz adım numarasındaki evre.
     *
     * Dizi tekrar başa sardığı için mod alınır: 2 evreli zincirin
     * 3. turu, adım 5 → evreler[1].
     */
    fun adimdaki(z: Zincir, adim: Int): Evre {
        if (z.evreler.isEmpty()) return Evre("", "", 0)
        val guvenli = adim.coerceIn(0, (z.toplamAdim - 1).coerceAtLeast(0))
        return z.evreler[guvenli % z.evreler.size]
    }

    /**
     * Biten [adim]'den sonraki adım. Zincir tükendiyse null —
     * çağıran "tamamlandı" akışına geçer.
     */
    fun sonrakiAdim(z: Zincir, adim: Int): Int? =
        if (adim + 1 < z.toplamAdim) adim + 1 else null

    /**
     * "3/16" — kaçıncı adımda olduğumuzun gösterim çifti.
     * Sona taşmış adım numaraları diziye sıkıştırılır.
     */
    fun kacinciAdim(z: Zincir, adim: Int): Pair<Int, Int> {
        val n = (adim + 1).coerceIn(1, z.toplamAdim.coerceAtLeast(1))
        return n to z.toplamAdim
    }

    /**
     * Adı mola/rahatlatma çağrıştıran evreler odak sayılmasın.
     *
     * Kullanıcı "Mola", "Dinlenme", "Nefes" diye adlandırınca
     * ders kaydına yazılması anlamsız olurdu. Türkçe katlama:
     * ı→i birleşimi tek kanaldan aranır (BİLDİRİM aramasında
     * öğrendiğimiz ders).
     */
    fun molaBenzeriMi(ad: String): Boolean {
        val katlanmis = ad.replace('ı', 'i').replace('İ', 'i').lowercase()
        return MOLA_ANLAMLILAR.any { katlanmis.contains(it) }
    }

    private val MOLA_ANLAMLILAR = listOf(
        "mola", "dinlen", "nefes", "esneme", "bekleme", "bekle",
        "yuruyus", "yürüyüş", "isınma", "isinma", "soguma", "soğuma"
    )

    /**
     * Evre adına göre küçük bir emoji önerisi.
     *
     * Kullanıcı zincir kurarken evreye isim yazar; emoji seçtirmek
     * diyaloğu büyütürdü. Sözcüğe göre makul bir görsel atanır —
     * tanınmayan adda nötr ⏱ kalır.
     */
    fun emojiOner(ad: String): String {
        val k = ad.replace('ı', 'i').replace('İ', 'i').lowercase()
        return when {
            molaBenzeriMi(ad) -> "☕"
            k.contains("sprint") || k.contains("kosu") || k.contains("koşu") -> "⚡"
            k.contains("oku") -> "📖"
            k.contains("kod") || k.contains("program") -> "💻"
            k.contains("yaz") -> "✏️"
            k.contains("dinle") || k.contains("podcast") -> "🎧"
            k.contains("egzers") || k.contains("spor") || k.contains("antrenman") -> "🏃"
            k.contains("tekrar") || k.contains("gozden") || k.contains("gözden") -> "🔁"
            k.contains("film") || k.contains("video") || k.contains("izle") -> "🎬"
            else -> "⏱️"
        }
    }

    /** Diyalog doğrulama sonuçları. */
    enum class Hata { YOK, EVRE_YOK, FAZLA_EVRE, SURE_GECERSIZ, TEKRAR_GECERSIZ }

    /**
     * Kurulmak istenen diziyi doğrular.
     *
     * @param evreler boş satırlar elenmiş hâli
     * @return ilk bulunan hata, temizse [Hata.YOK]
     */
    fun dogrula(evreler: List<Evre>, tekrar: Int): Hata {
        if (evreler.isEmpty()) return Hata.EVRE_YOK
        if (evreler.size > MAKS_EVRE) return Hata.FAZLA_EVRE
        if (tekrar !in 1..MAKS_TEKRAR) return Hata.TEKRAR_GECERSIZ
        if (evreler.any { it.sn !in MIN_EVRE_SN..MAKS_EVRE_SN }) return Hata.SURE_GECERSIZ
        return Hata.YOK
    }

    /** Toplam saniyeyi "2:00" / "0:20" gibi gösterir (saat varsa "1:24:00"). */
    fun sureMetni(sn: Int): String {
        val s = sn.coerceAtLeast(0)
        val saat = s / 3600
        val dk = s % 3600 / 60
        val kalan = s % 60
        return if (saat > 0) {
            String.format(java.util.Locale.US, "%d:%02d:%02d", saat, dk, kalan)
        } else {
            String.format(java.util.Locale.US, "%d:%02d", dk, kalan)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // JSON
    // ═══════════════════════════════════════════════════════════════

    /** Kayıtlı kullanıcı zincirlerini tek metne kodlar. */
    fun kodla(liste: List<Zincir>): String {
        val dizi = JSONArray()
        liste.forEach { dizi.put(it.json()) }
        return dizi.toString()
    }

    /** [kodla]'nın tersi. Bozuk satırlar atlanır, çöp metin boş liste verir. */
    fun cozle(metin: String): List<Zincir> {
        if (metin.isBlank()) return emptyList()
        return try {
            val dizi = JSONArray(metin)
            val liste = mutableListOf<Zincir>()
            for (i in 0 until dizi.length()) {
                dizi.optJSONObject(i)?.let { Zincir.jsondan(it) }?.let { liste.add(it) }
            }
            liste
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ŞABLONLAR — veri kataloğu (Türkçe literaller bilinçli)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Hazır diziler. Kimlikleri negatiftir — kullanıcı
     * zincirleriyle asla çakışmaz. Adlar veri kataloğudur
     * (Tabata protokol adı gibi); diyalog ve bildirim metinleri
     * strings.xml'dedir.
     */
    fun sablonlar(): List<Zincir> = listOf(
        Zincir(
            id = -1, ad = "Tabata", emoji = "🔥",
            evreler = listOf(
                Evre("Çalış", "💪", 20, odakMi = true),
                Evre("Dinlenme", "🧘", 10, odakMi = false)
            ),
            tekrar = 8
        ),
        Zincir(
            id = -2, ad = "Pomodoro döngüsü", emoji = "🍅",
            evreler = listOf(
                Evre("Çalışma", "📖", 25 * 60, odakMi = true),
                Evre("Mola", "☕", 5 * 60, odakMi = false)
            ),
            tekrar = 4
        ),
        Zincir(
            id = -3, ad = "52/17 kuralı", emoji = "📚",
            evreler = listOf(
                Evre("Derin çalışma", "📖", 52 * 60, odakMi = true),
                Evre("Mola", "☕", 17 * 60, odakMi = false)
            ),
            tekrar = 3
        ),
        Zincir(
            id = -4, ad = "Egzersiz turu", emoji = "🏃",
            evreler = listOf(
                Evre("Isınma", "🧘", 120, odakMi = false),
                Evre("Sprint", "⚡", 30, odakMi = true),
                Evre("Yürüyüş", "🚶", 90, odakMi = false),
                Evre("Sprint", "⚡", 30, odakMi = true),
                Evre("Yürüyüş", "🚶", 90, odakMi = false),
                Evre("Sprint", "⚡", 30, odakMi = true),
                Evre("Soğuma", "🧘", 120, odakMi = false)
            ),
            tekrar = 1
        ),
        // v10.12 · Grup D / D19 — nefes stüdyosunun zincir hâli:
        // uyku öncesi dört tur. Şablonlar da doğrulamadan geçtiği için
        // evreler MIN_EVRE_SN (5 sn) altına inemez; ritim bu yüzden
        // 5-7-8'dir (4-7-8'in yavaşlatılmış türevi — stüdyoda gerçek
        // 4-7-8 koşulur). Evreler odak sayılmaz: uyku hazırlığı odak
        // istatistiğine karışmasın.
        Zincir(
            id = -5, ad = "Uyku öncesi nefes", emoji = "🌙",
            evreler = listOf(
                Evre("Nefes al", "🌬️", 5, odakMi = false),
                Evre("Tut", "⏸️", 7, odakMi = false),
                Evre("Nefes ver", "💨", 8, odakMi = false)
            ),
            tekrar = 4
        )
    )

    // ═══════════════════════════════════════════════════════════════
    // KALICILIK — kayıtlar, aktif zincir, koşu durumu
    // ═══════════════════════════════════════════════════════════════

    /** Kullanıcının kaydettiği zincirler (şablonlar hariç). */
    fun kayitlilar(context: Context): List<Zincir> =
        cozle(prefs(context).getString(K_KAYITLI, "[]") ?: "[]")

    /**
     * Yeni kullanıcı zincirini kaydeder; gerçek kimliği döner.
     * Şablonların aktifleşmesi için bu fonksiyon KULLANILMAZ —
     * onlar kalıcı kayda gerekmeden kimlikleriyle seçilir.
     */
    fun kaydet(context: Context, taslak: Zincir): Zincir {
        val p = prefs(context)
        val yeniId = p.getLong(K_SONRAKI_ID, 1L)
        val gercek = taslak.copy(id = yeniId)
        val liste = kayitlilar(context).toMutableList()
        liste.add(gercek)
        p.edit()
            .putString(K_KAYITLI, kodla(liste))
            .putLong(K_SONRAKI_ID, yeniId + 1)
            .apply()
        return gercek
    }

    /** Kullanıcı zincirini siler; aktifse seçimi de kaldırır. */
    fun sil(context: Context, id: Long) {
        val liste = kayitlilar(context).filterNot { it.id == id }
        val e = prefs(context).edit().putString(K_KAYITLI, kodla(liste))
        if (aktifId(context) == id) {
            e.putLong(K_AKTIF_ID, ID_YOK).putInt(K_ADIM, 0).putBoolean(K_KOSUYOR, false)
        }
        e.apply()
    }

    /** Şablonlar + kayıtlılar içinden kimliğe göre bulur. */
    fun idIleBul(context: Context, id: Long): Zincir? =
        (sablonlar() + kayitlilar(context)).firstOrNull { it.id == id }

    // ── Seçim ve koşu ──

    fun aktifId(context: Context): Long = prefs(context).getLong(K_AKTIF_ID, ID_YOK)

    /** Şu an seçili zincir (koşuyor olması gerekmez). */
    fun aktif(context: Context): Zincir? {
        val id = aktifId(context)
        return if (id == ID_YOK) null else idIleBul(context, id)
    }

    /**
     * Zinciri seçili yapar; koşu sayacını sıfırlar.
     * Şablonlar dahil her kimlikle çalışır.
     */
    fun aktiflestir(context: Context, id: Long) {
        prefs(context).edit()
            .putLong(K_AKTIF_ID, id)
            .putInt(K_ADIM, 0)
            .putBoolean(K_KOSUYOR, false)
            .apply()
    }

    /** Seçimi tamamen kaldırır. */
    fun secimiKaldir(context: Context) {
        prefs(context).edit()
            .putLong(K_AKTIF_ID, ID_YOK)
            .putInt(K_ADIM, 0)
            .putBoolean(K_KOSUYOR, false)
            .apply()
    }

    fun kosuyor(context: Context): Boolean =
        prefs(context).getBoolean(K_KOSUYOR, false) && aktif(context) != null

    /** Koşuyu başlatır (adım korunur — devam da bu kapıdan). */
    fun baslat(context: Context) {
        if (aktif(context) == null) return
        prefs(context).edit().putBoolean(K_KOSUYOR, true).apply()
    }

    /** Koşuyu duraklatır; adım yerinde kalır, devam edilebilir. */
    fun durdur(context: Context) {
        prefs(context).edit().putBoolean(K_KOSUYOR, false).apply()
    }

    fun adim(context: Context): Int = prefs(context).getInt(K_ADIM, 0).coerceAtLeast(0)

    fun setAdim(context: Context, adim: Int) {
        prefs(context).edit().putInt(K_ADIM, adim.coerceAtLeast(0)).apply()
    }

    /** Koşu sayacını başa sarar (koşuyor bayrağına dokunmaz). */
    fun sifirla(context: Context) {
        prefs(context).edit().putInt(K_ADIM, 0).apply()
    }

    /**
     * Evre bitince sıradaki evre otomatik mi başlasın?
     * Varsayılan açık: zincirin ruhu akıştır (Tabata beklemez).
     */
    fun otoDevam(context: Context): Boolean = prefs(context).getBoolean(K_OTO, true)

    fun setOtoDevam(context: Context, acik: Boolean) {
        prefs(context).edit().putBoolean(K_OTO, acik).apply()
    }
}
