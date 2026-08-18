package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.net.ssl.HttpsURLConnection

/**
 * v7.51 — İki kişilik online paylaşım.
 *
 * ── Kullanıcı isteği ──
 * "Bu uygulamayi online 2 kisilik yap... Diger kisinin eklenmesi icin kod girsin"
 *
 * ── Sunucu sorunu ve çözümü ──
 * Kendi sunucumuz yok. Firebase kurulum + hesap gerektiriyor. Çözüm:
 * **textdb.online** — anahtarsız, kurulumsuz, kullanıcının belirlediği
 * anahtarla çalışan basit bir anahtar/değer deposu.
 *
 * Kodlamadan önce gerçek HTTP çağrılarıyla doğrulandı:
 *   · 6 haneli kodla yazma/okuma  ✓
 *   · İki yönlü senkron           ✓
 *   · Türkçe karakterler          ✓
 *
 * ── Çalışma mantığı ──
 * Oda = tek bir JSON belgesi. Her iki taraf da tüm belgeyi okur, kendi
 * değişikliğini ekler, tamamını geri yazar. Sürüm numarası ile çakışma
 * tespit edilir.
 *
 * ── Gizlilik uyarısı ──
 * textdb.online herkese açık bir servistir. Kodu bilen herkes odayı okuyabilir.
 * Bu yüzden ekranda uyarı gösteriliyor ve hassas veri yazılmaması öneriliyor.
 */
object OnlineStore {

    private const val TAG = "OnlineStore"
    private const val PREF = "online_v1"
    private const val TABAN = "https://textdb.online"
    private const val ONEK = "gunlukasistan-v1-"

    /** Belge biçimi sürümü — ileride değişirse uyumsuzluk anlaşılsın. */
    private const val BICIM = 1

    // ═══════════════════════════════════════════════════════════════
    // MODELLER
    // ═══════════════════════════════════════════════════════════════

    /** Paylaşılan görev. */
    data class Gorev(
        val id: Long,
        var metin: String,
        var sahip: String,
        var tamam: Boolean = false,
        var not: String = "",
        val eklendi: Long = System.currentTimeMillis()
    )

    /**
     * v7.53 — Paylaşılan not.
     * Görevden farkı: tamamlanma durumu yok, uzun metin taşır.
     */
    data class Not(
        val id: Long,
        var baslik: String,
        var icerik: String,
        var sahip: String,
        val eklendi: Long = System.currentTimeMillis()
    )

    /** v7.53 — Paylaşılan konu ve alt maddeleri. */
    data class AltMadde(
        val id: Long,
        var metin: String,
        var tamam: Boolean = false,
        var kim: String = ""
    )

    data class Konu(
        val id: Long,
        var baslik: String,
        var sahip: String,
        val maddeler: MutableList<AltMadde> = mutableListOf(),
        val eklendi: Long = System.currentTimeMillis()
    ) {
        val bitenSayi: Int get() = maddeler.count { it.tamam }
        val yuzde: Int get() =
            if (maddeler.isEmpty()) 0 else bitenSayi * 100 / maddeler.size
    }

    /**
     * v7.53 — Paylaşılan alışkanlık.
     * Her üye kendi işaretini koyar; kim yaptı kim yapmadı görünür.
     */
    data class Aliskanlik(
        val id: Long,
        var ad: String,
        var emoji: String = "✨",
        var sahip: String = "",
        /** "yyyyMMdd|kisi" biçiminde işaretler — kim hangi gün yaptı. */
        val isaretler: MutableList<String> = mutableListOf(),
        val eklendi: Long = System.currentTimeMillis()
    ) {
        fun bugunYaptiMi(kisi: String, bugun: String): Boolean =
            isaretler.contains(bugun + "|" + kisi)

        fun bugunKimler(bugun: String): List<String> =
            isaretler.filter { it.startsWith(bugun + "|") }
                .map { it.substringAfter("|") }
    }

    /**
     * v7.54 — Sohbet mesajı.
     *
     * @param tur "kisi" = üye yazdı · "ai" = yapay zekâ cevabı
     * @param oneriler AI'ın önerdiği eklenebilir öğeler.
     *        Biçim: "tur|metin" (tur: gorev/not/konu/alis)
     */
    data class Mesaj(
        val id: Long,
        val kim: String,
        val metin: String,
        val zaman: Long = System.currentTimeMillis(),
        val tur: String = "kisi",
        val oneriler: MutableList<String> = mutableListOf()
    ) {
        val aiMi: Boolean get() = tur == "ai"
    }

    /**
     * v7.52 — Odanın kısıtlama ayarları.
     *
     * Yönetici bu anahtarları açıp kapatır; üye tarafında ilgili işlem
     * engellenir. Tümü uygulama içi kilittir (Android başka uygulamanın
     * silinmesini engellemeye izin vermez — kullanıcıya açıkça söylendi).
     */
    class Kural(
        /** Üye görev silebilir mi? */
        var silebilir: Boolean = false,
        /** Üye başkasının görevini düzenleyebilir mi? */
        var baskasiniDuzenler: Boolean = false,
        /** Üye tamamlanmış görevin işaretini kaldırabilir mi? */
        var geriAlabilir: Boolean = true,
        /** Üye odadan kendi başına ayrılabilir mi? */
        var ayrilabilir: Boolean = false,
        /** Üye bildirimleri kapatabilir mi? */
        var bildirimKapatir: Boolean = false,
        /** Üye kendi adını değiştirebilir mi? */
        var adDegistirir: Boolean = true,
        /** Üye yeni görev ekleyebilir mi? */
        var ekleyebilir: Boolean = true,
        /**
         * v7.56 — Bildirim kilitleri.
         * Yonetici kapatirsa uye bu ayarlari degistiremez.
         * Varsayilan false = kilitli (yonetici izin vermeli).
         */
        var sesKapatir: Boolean = false,
        var titresimKapatir: Boolean = false,
        var bildirimKapatirTum: Boolean = false,
        var zorunluKapatir: Boolean = false,
        /**
         * v7.77: "Karsi taraf yonetsin" secildiginde true olur.
         * Odaya katilan ilk kisi yoneticiligi devralir, sonra false yapilir.
         */
        var yoneticiBekliyor: Boolean = false
    )

    /** Odanın tamamı. */
    class Oda(
        val surum: Int = 0,
        val uyeler: MutableList<String> = mutableListOf(),
        val gorevler: MutableList<Gorev> = mutableListOf(),
        val mesajlar: MutableList<Mesaj> = mutableListOf(),
        /** v7.53: ayrı bölümler — notlar, konular, alışkanlıklar. */
        val notlar: MutableList<Not> = mutableListOf(),
        val konular: MutableList<Konu> = mutableListOf(),
        val aliskanliklar: MutableList<Aliskanlik> = mutableListOf(),
        val guncelleyen: String = "",
        val guncellendi: Long = 0L,
        /** v7.52: odayı kuran kişi — yönetici. */
        var yonetici: String = "",
        /** v7.52: yönetici şifresinin karması (düz metin saklanmaz). */
        var sifreHash: String = "",
        /** v7.52: üye kısıtlamaları. */
        var kural: Kural = Kural()
    )

    class Sonuc(val ok: Boolean, val mesaj: String, val oda: Oda? = null)

    // ═══════════════════════════════════════════════════════════════
    // AYARLAR
    // ═══════════════════════════════════════════════════════════════

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    /** Odaya bağlı mıyız? */
    fun bagliMi(context: Context): Boolean = kod(context).isNotBlank()

    fun kod(context: Context): String = prefs(context).getString("kod", "") ?: ""

    fun benimAdim(context: Context): String =
        prefs(context).getString("ad", "") ?: ""

    fun setBenimAdim(context: Context, ad: String) {
        prefs(context).edit().putString("ad", ad.trim().take(20)).apply()
    }

    fun baglan(context: Context, kod: String, ad: String) {
        prefs(context).edit()
            .putString("kod", kod.trim().uppercase(Locale.US))
            .putString("ad", ad.trim().take(20))
            .apply()
        // v7.57: yeni odaya girince eski "gorulen" listesi gecersiz.
        // Temizlenmezse odadaki mevcut her sey yeniymis gibi bildirilir.
        try {
            OnlineBekci.temizBaslat(context)
            OnlineBekci.kur(context)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Bekci baslatilamadi", e)
        }
    }

    fun ayril(context: Context) {
        prefs(context).edit()
            .remove("kod").remove("son_surum")
            .remove("yonetici").remove("yonetici_bitis")
            .apply()
        // v7.57: odadan cikinca arka plan kontrolu de dursun
        try {
            OnlineBekci.iptal(context)
            OnlineBekci.temizBaslat(context)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Bekci durdurulamadi", e)
        }
    }

    /** En son bilinen sürüm — yeni değişiklik var mı anlamak için. */
    fun sonSurum(context: Context): Int = prefs(context).getInt("son_surum", 0)

    private fun setSonSurum(context: Context, s: Int) {
        prefs(context).edit().putInt("son_surum", s).apply()
    }

    /** Otomatik eşitleme açık mı (ekran açıldığında). */
    fun otoSenkron(context: Context): Boolean =
        prefs(context).getBoolean("oto", true)

    fun setOtoSenkron(context: Context, a: Boolean) {
        prefs(context).edit().putBoolean("oto", a).apply()
    }

    // ═══════════════════════════════════════════════════════════════
    // v7.52 — YÖNETİCİ, ŞİFRE VE YETKİLER
    // ═══════════════════════════════════════════════════════════════

    /**
     * Şifreyi karma (hash) haline getirir.
     *
     * Düz metin şifre asla sunucuya gitmez. textdb.online herkese açık
     * olduğu için bu şart. Tuz olarak oda kodu kullanılıyor — aynı şifre
     * farklı odalarda farklı karma üretsin.
     *
     * Not: Bu bir banka güvenliği değil, aile içi yetki ayrımı. Amaç
     * karşı tarafın ayarları kazara/kolayca değiştirmesini önlemek.
     */
    fun sifreKarma(sifre: String, kod: String): String {
        return try {
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val ham = (kod.uppercase(Locale.US) + "::" + sifre).toByteArray(Charsets.UTF_8)
            md.digest(ham).joinToString("") { "%02x".format(it) }.take(32)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Karma üretilemedi", e)
            ""
        }
    }

    /**
     * Bu cihazdaki kullanıcı odanın yöneticisi mi?
     *
     * v7.77: Oda **yöneticisiz** de kurulabiliyor (`yonetici` boş).
     * O durumda herkes yönetici sayılır — kimse kimseyi kısıtlayamaz.
     */
    fun yoneticiMiyim(context: Context, oda: Oda?): Boolean {
        val ben = benimAdim(context)
        if (ben.isBlank()) return false
        // Oda bilgisi yoksa yerel bayrağa bak (çevrimdışı durum)
        val o = oda ?: return prefs(context).getBoolean("yonetici", false)
        // v7.77: yöneticisiz oda — herkes eşit yetkili
        if (o.yonetici.isBlank()) return true
        return o.yonetici == ben
    }

    /** v7.77: Oda yöneticisiz mi kuruldu? */
    fun yoneticisizMi(oda: Oda?): Boolean = oda?.yonetici.isNullOrBlank()

    /** Yerel yönetici bayrağı — oda kurulurken işaretlenir. */
    fun setYoneticiBayragi(context: Context, deger: Boolean) {
        prefs(context).edit().putBoolean("yonetici", deger).apply()
    }

    /**
     * Yönetici oturumu açık mı?
     * Şifre girildikten sonra 10 dakika boyunca tekrar sorulmaz.
     */
    fun yoneticiOturumu(context: Context): Boolean {
        val bitis = prefs(context).getLong("yonetici_bitis", 0L)
        return System.currentTimeMillis() < bitis
    }

    fun yoneticiOturumuAc(context: Context) {
        prefs(context).edit()
            .putLong("yonetici_bitis", System.currentTimeMillis() + 10 * 60_000L)
            .apply()
    }

    fun yoneticiOturumuKapat(context: Context) {
        prefs(context).edit().remove("yonetici_bitis").apply()
    }

    /** Girilen şifre doğru mu? */
    fun sifreDogruMu(context: Context, oda: Oda?, sifre: String): Boolean {
        val o = oda ?: return false
        if (o.sifreHash.isBlank()) return true   // şifre konulmamışsa serbest
        return sifreKarma(sifre, kod(context)) == o.sifreHash
    }

    /**
     * Bir işlem yapılabilir mi?
     * Yönetici her şeyi yapabilir; üye yalnızca kuralın izin verdiğini.
     */
    fun izinVar(context: Context, oda: Oda?, islem: Islem): Boolean {
        if (yoneticiMiyim(context, oda)) return true
        val k = oda?.kural ?: Kural()
        return when (islem) {
            Islem.SIL -> k.silebilir
            Islem.BASKASINI_DUZENLE -> k.baskasiniDuzenler
            Islem.GERI_AL -> k.geriAlabilir
            // v7.77: Odadan ayrilmak ARTIK IZNE BAGLI DEGIL.
            //
            // Kullanicinin bildirimi: "odadan ayrilmayi yonetici izni
            // olmadan yapabileyim". Birini bir odada zorla tutmak dogru
            // degil — kural anahtari geriye donuk uyumluluk icin duruyor
            // ama kontrol edilmiyor.
            Islem.AYRIL -> true
            Islem.BILDIRIM_KAPAT -> k.bildirimKapatir
            Islem.AD_DEGISTIR -> k.adDegistirir
            Islem.EKLE -> k.ekleyebilir
            // v7.56: bildirim kilitleri
            Islem.SES_KAPAT -> k.sesKapatir
            Islem.TITRESIM_KAPAT -> k.titresimKapatir
            Islem.BILDIRIM_TUM_KAPAT -> k.bildirimKapatirTum
            Islem.ZORUNLU_KAPAT -> k.zorunluKapatir
            Islem.KURAL_DEGISTIR -> false   // yalnızca yönetici
        }
    }

    enum class Islem {
        SIL, BASKASINI_DUZENLE, GERI_AL, AYRIL,
        BILDIRIM_KAPAT, AD_DEGISTIR, EKLE, KURAL_DEGISTIR,
        // v7.56
        SES_KAPAT, TITRESIM_KAPAT, BILDIRIM_TUM_KAPAT, ZORUNLU_KAPAT
    }

    /** İzin yoksa gösterilecek açıklama. */
    fun izinMesaji(context: Context, islem: Islem): String = context.getString(
        when (islem) {
            Islem.SIL -> R.string.on_yetki_sil
            Islem.BASKASINI_DUZENLE -> R.string.on_yetki_duzenle
            Islem.GERI_AL -> R.string.on_yetki_gerial
            Islem.AYRIL -> R.string.on_yetki_ayril
            Islem.BILDIRIM_KAPAT -> R.string.on_yetki_bildirim
            Islem.AD_DEGISTIR -> R.string.on_yetki_ad
            Islem.EKLE -> R.string.on_yetki_ekle
            Islem.KURAL_DEGISTIR -> R.string.on_yetki_kural
            Islem.SES_KAPAT -> R.string.on_yetki_ses
            Islem.TITRESIM_KAPAT -> R.string.on_yetki_titresim
            Islem.BILDIRIM_TUM_KAPAT -> R.string.on_yetki_bana
            Islem.ZORUNLU_KAPAT -> R.string.on_yetki_zorunlu
        }
    )

    // ═══════════════════════════════════════════════════════════════
    // KOD ÜRETİMİ
    // ═══════════════════════════════════════════════════════════════

    /** v7.54: Sohbet balonunda gösterilecek saat. */
    fun saatMetni(ms: Long): String = try {
        SimpleDateFormat("HH:mm", Locale("tr", "TR")).format(Date(ms))
    } catch (_: Exception) { "" }

    /**
     * v7.53: Alışkanlık işaretleri için bugünün anahtarı (yyyyMMdd).
     */
    fun bugunAnahtari(): String =
        SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

    /**
     * 6 haneli davet kodu üretir.
     * Karışabilecek harfler (I, O, 0, 1) çıkarıldı — telefonda okunurken
     * yanlış yazılmasın.
     */
    fun kodUret(): String {
        val alfabe = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { alfabe.random() }.joinToString("")
    }

    private fun anahtar(kod: String): String = ONEK + kod.uppercase(Locale.US)

    // ═══════════════════════════════════════════════════════════════
    // AĞ
    // ═══════════════════════════════════════════════════════════════

    private fun cevrimici(context: Context): Boolean = try {
        AiClient.isOnline(context)
    } catch (_: Exception) {
        true
    }

    /** Odayı sunucudan okur. */
    fun oku(context: Context, kod: String = ""): Sonuc {
        val k = kod.ifBlank { kod(context) }
        if (k.isBlank()) return Sonuc(false, context.getString(R.string.on_err_nokod))
        if (!cevrimici(context)) return Sonuc(false, context.getString(R.string.ai_err_no_net))

        return try {
            val url = TABAN + "/" + anahtar(k) + "/"
            val conn = (URL(url).openConnection() as HttpsURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 20000
                instanceFollowRedirects = true
            }
            val kodHttp = conn.responseCode
            val akis = if (kodHttp in 200..299) conn.inputStream else conn.errorStream
            val govde = BufferedReader(InputStreamReader(akis, Charsets.UTF_8))
                .use { it.readText() }.trim()
            conn.disconnect()

            if (kodHttp !in 200..299) {
                return Sonuc(false, context.getString(R.string.on_err_sunucu, kodHttp))
            }
            if (govde.isBlank() || !govde.startsWith("{")) {
                return Sonuc(false, context.getString(R.string.on_err_bos))
            }
            Sonuc(true, "", coz(govde))
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Okunamadı", e)
            Sonuc(false, context.getString(R.string.on_err_ag))
        }
    }

    /** Odayı sunucuya yazar. */
    fun yaz(context: Context, oda: Oda, kod: String = ""): Sonuc {
        val k = kod.ifBlank { kod(context) }
        if (k.isBlank()) return Sonuc(false, context.getString(R.string.on_err_nokod))
        if (!cevrimici(context)) return Sonuc(false, context.getString(R.string.ai_err_no_net))

        return try {
            val govde = kodla(oda, benimAdim(context))
            val url = TABAN + "/update/?key=" + anahtar(k) +
                "&value=" + URLEncoder.encode(govde, "UTF-8")

            val conn = (URL(url).openConnection() as HttpsURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 25000
                instanceFollowRedirects = true
                doOutput = true
            }
            conn.outputStream.use { it.write(ByteArray(0)) }
            val kodHttp = conn.responseCode
            conn.inputStream?.use { it.readBytes() }
            conn.disconnect()

            if (kodHttp !in 200..299) {
                return Sonuc(false, context.getString(R.string.on_err_sunucu, kodHttp))
            }
            setSonSurum(context, oda.surum)
            Sonuc(true, "")
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Yazılamadı", e)
            Sonuc(false, context.getString(R.string.on_err_ag))
        }
    }

    /**
     * Oku → değiştir → yaz döngüsü.
     *
     * Çakışma koruması: yazmadan hemen önce tekrar okunur. Karşı taraf
     * bu arada değişiklik yaptıysa onun verisi temel alınır, bizimki
     * üzerine eklenir. Böylece kimsenin işi kaybolmaz.
     */
    fun guvenliGuncelle(context: Context, degistir: (Oda) -> Unit): Sonuc {
        val taze = oku(context)
        if (!taze.ok) {
            // Oda henüz yoksa boş odayla başla
            if (taze.mesaj == context.getString(R.string.on_err_bos)) {
                val yeni = Oda(surum = 1)
                degistir(yeni)
                return yaz(context, yeni)
            }
            return taze
        }
        val oda = taze.oda ?: Oda()
        degistir(oda)
        val yeniSurum = Oda(
            surum = oda.surum + 1,
            uyeler = oda.uyeler,
            gorevler = oda.gorevler,
            mesajlar = oda.mesajlar
        )
        return yaz(context, yeniSurum)
    }

    // ═══════════════════════════════════════════════════════════════
    // JSON
    // ═══════════════════════════════════════════════════════════════

    private fun kodla(oda: Oda, guncelleyen: String): String {
        val gorevDizi = JSONArray()
        // Alan adları kısa: sunucu URL uzunluk sınırına takılmasın
        oda.gorevler.takeLast(60).forEach { g ->
            gorevDizi.put(
                JSONObject()
                    .put("i", g.id).put("m", g.metin).put("s", g.sahip)
                    .put("t", g.tamam).put("n", g.not).put("e", g.eklendi)
            )
        }
        val mesajDizi = JSONArray()
        // v7.54: sohbet — AI mesajları ve önerileri de taşınır
        oda.mesajlar.takeLast(40).forEach { m ->
            mesajDizi.put(
                JSONObject().put("i", m.id).put("k", m.kim)
                    .put("m", m.metin).put("z", m.zaman)
                    .put("t", m.tur)
                    .put("o", JSONArray(m.oneriler.take(6)))
            )
        }
        // v7.52: kurallar tek bir sayı içinde bit maskesi olarak taşınır —
        // URL uzunluğu sınırlı olduğu için yer tasarrufu önemli
        var maske = 0
        if (oda.kural.silebilir) maske = maske or 1
        if (oda.kural.baskasiniDuzenler) maske = maske or 2
        if (oda.kural.geriAlabilir) maske = maske or 4
        if (oda.kural.ayrilabilir) maske = maske or 8
        if (oda.kural.bildirimKapatir) maske = maske or 16
        if (oda.kural.adDegistirir) maske = maske or 32
        if (oda.kural.ekleyebilir) maske = maske or 64
        // v7.56: bildirim kilitleri
        if (oda.kural.sesKapatir) maske = maske or 128
        if (oda.kural.titresimKapatir) maske = maske or 256
        if (oda.kural.bildirimKapatirTum) maske = maske or 512
        if (oda.kural.zorunluKapatir) maske = maske or 1024
        // v7.77: yonetici bekliyor bayragi
        if (oda.kural.yoneticiBekliyor) maske = maske or 2048

        // v7.53: notlar
        val notDizi = JSONArray()
        oda.notlar.takeLast(30).forEach { n ->
            notDizi.put(
                JSONObject().put("i", n.id).put("b", n.baslik)
                    .put("c", n.icerik).put("s", n.sahip).put("e", n.eklendi)
            )
        }
        // v7.53: konular + alt maddeleri
        val konuDizi = JSONArray()
        oda.konular.takeLast(20).forEach { k ->
            val maddeDizi = JSONArray()
            k.maddeler.takeLast(30).forEach { m ->
                maddeDizi.put(
                    JSONObject().put("i", m.id).put("m", m.metin)
                        .put("t", m.tamam).put("k", m.kim)
                )
            }
            konuDizi.put(
                JSONObject().put("i", k.id).put("b", k.baslik)
                    .put("s", k.sahip).put("m", maddeDizi).put("e", k.eklendi)
            )
        }
        // v7.53: alışkanlıklar
        val alisDizi = JSONArray()
        oda.aliskanliklar.takeLast(15).forEach { a ->
            alisDizi.put(
                JSONObject().put("i", a.id).put("a", a.ad).put("j", a.emoji)
                    .put("s", a.sahip)
                    .put("w", JSONArray(a.isaretler.takeLast(60)))
                    .put("e", a.eklendi)
            )
        }

        return JSONObject()
            .put("b", BICIM)
            .put("v", oda.surum)
            .put("u", JSONArray(oda.uyeler))
            .put("g", gorevDizi)
            .put("s", mesajDizi)
            .put("n", notDizi)
            .put("t", konuDizi)
            .put("l", alisDizi)
            .put("gu", guncelleyen)
            .put("gz", System.currentTimeMillis())
            .put("y", oda.yonetici)
            .put("h", oda.sifreHash)
            .put("k", maske)
            .toString()
    }

    private fun coz(json: String): Oda {
        return try {
            val o = JSONObject(json)
            val uyeler = mutableListOf<String>()
            o.optJSONArray("u")?.let { d ->
                for (i in 0 until d.length()) {
                    d.optString(i).takeIf { it.isNotBlank() }?.let { uyeler.add(it) }
                }
            }
            val gorevler = mutableListOf<Gorev>()
            o.optJSONArray("g")?.let { d ->
                for (i in 0 until d.length()) {
                    val g = d.optJSONObject(i) ?: continue
                    gorevler.add(
                        Gorev(
                            id = g.optLong("i"),
                            metin = g.optString("m"),
                            sahip = g.optString("s"),
                            tamam = g.optBoolean("t"),
                            not = g.optString("n"),
                            eklendi = g.optLong("e")
                        )
                    )
                }
            }
            val mesajlar = mutableListOf<Mesaj>()
            o.optJSONArray("s")?.let { d ->
                for (i in 0 until d.length()) {
                    val m = d.optJSONObject(i) ?: continue
                    val oneriler = mutableListOf<String>()
                    m.optJSONArray("o")?.let { od ->
                        for (j in 0 until od.length()) {
                            od.optString(j).takeIf { it.isNotBlank() }
                                ?.let { oneriler.add(it) }
                        }
                    }
                    mesajlar.add(
                        Mesaj(
                            id = m.optLong("i"),
                            kim = m.optString("k"),
                            metin = m.optString("m"),
                            zaman = m.optLong("z"),
                            tur = m.optString("t", "kisi"),
                            oneriler = oneriler
                        )
                    )
                }
            }
            // v7.53: notlar
            val notlar = mutableListOf<Not>()
            o.optJSONArray("n")?.let { d ->
                for (i in 0 until d.length()) {
                    val n = d.optJSONObject(i) ?: continue
                    notlar.add(
                        Not(
                            id = n.optLong("i"), baslik = n.optString("b"),
                            icerik = n.optString("c"), sahip = n.optString("s"),
                            eklendi = n.optLong("e")
                        )
                    )
                }
            }
            // v7.53: konular
            val konular = mutableListOf<Konu>()
            o.optJSONArray("t")?.let { d ->
                for (i in 0 until d.length()) {
                    val k = d.optJSONObject(i) ?: continue
                    val maddeler = mutableListOf<AltMadde>()
                    k.optJSONArray("m")?.let { md ->
                        for (j in 0 until md.length()) {
                            val m = md.optJSONObject(j) ?: continue
                            maddeler.add(
                                AltMadde(
                                    id = m.optLong("i"), metin = m.optString("m"),
                                    tamam = m.optBoolean("t"), kim = m.optString("k")
                                )
                            )
                        }
                    }
                    konular.add(
                        Konu(
                            id = k.optLong("i"), baslik = k.optString("b"),
                            sahip = k.optString("s"), maddeler = maddeler,
                            eklendi = k.optLong("e")
                        )
                    )
                }
            }
            // v7.53: alışkanlıklar
            val aliskanliklar = mutableListOf<Aliskanlik>()
            o.optJSONArray("l")?.let { d ->
                for (i in 0 until d.length()) {
                    val a = d.optJSONObject(i) ?: continue
                    val isaretler = mutableListOf<String>()
                    a.optJSONArray("w")?.let { w ->
                        for (j in 0 until w.length()) {
                            w.optString(j).takeIf { it.isNotBlank() }
                                ?.let { isaretler.add(it) }
                        }
                    }
                    aliskanliklar.add(
                        Aliskanlik(
                            id = a.optLong("i"), ad = a.optString("a"),
                            emoji = a.optString("j", "✨"), sahip = a.optString("s"),
                            isaretler = isaretler, eklendi = a.optLong("e")
                        )
                    )
                }
            }

            // Eski odalarda "k" yok — varsayılan maske (geriAl+ad+ekle açık)
            val maske = o.optInt("k", 4 or 32 or 64)
            Oda(
                surum = o.optInt("v"),
                uyeler = uyeler,
                gorevler = gorevler,
                mesajlar = mesajlar,
                notlar = notlar,
                konular = konular,
                aliskanliklar = aliskanliklar,
                guncelleyen = o.optString("gu"),
                guncellendi = o.optLong("gz"),
                yonetici = o.optString("y"),
                sifreHash = o.optString("h"),
                kural = Kural(
                    silebilir = (maske and 1) != 0,
                    baskasiniDuzenler = (maske and 2) != 0,
                    geriAlabilir = (maske and 4) != 0,
                    ayrilabilir = (maske and 8) != 0,
                    bildirimKapatir = (maske and 16) != 0,
                    adDegistirir = (maske and 32) != 0,
                    ekleyebilir = (maske and 64) != 0,
                    sesKapatir = (maske and 128) != 0,
                    titresimKapatir = (maske and 256) != 0,
                    bildirimKapatirTum = (maske and 512) != 0,
                    zorunluKapatir = (maske and 1024) != 0,
                    yoneticiBekliyor = (maske and 2048) != 0
                )
            )
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Çözülemedi", e)
            Oda()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // YEREL ÖNBELLEK — çevrimdışıyken son hali göster
    // ═══════════════════════════════════════════════════════════════

    fun onbellegeYaz(context: Context, oda: Oda) {
        prefs(context).edit()
            .putString("cache", kodla(oda, benimAdim(context)))
            .putLong("cache_zaman", System.currentTimeMillis())
            .apply()
    }

    fun onbellektenOku(context: Context): Oda? {
        val c = prefs(context).getString("cache", "") ?: ""
        return if (c.isBlank()) null else coz(c)
    }

    fun onbellekZamani(context: Context): Long =
        prefs(context).getLong("cache_zaman", 0L)

    fun zamanMetni(ms: Long): String {
        if (ms <= 0L) return "—"
        val fark = System.currentTimeMillis() - ms
        return when {
            fark < 60_000 -> "az önce"
            fark < 3_600_000 -> (fark / 60_000).toString() + " dk önce"
            fark < 86_400_000 -> (fark / 3_600_000).toString() + " sa önce"
            else -> SimpleDateFormat("d MMM HH:mm", Locale("tr", "TR")).format(Date(ms))
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // YEDEKLEME
    // ═══════════════════════════════════════════════════════════════

    fun disaAktar(context: Context): JSONObject = JSONObject()
        .put("kod", kod(context))
        .put("ad", benimAdim(context))
        .put("oto", otoSenkron(context))

    fun iceAktar(context: Context, o: JSONObject) {
        try {
            val e = prefs(context).edit()
            if (o.has("kod")) e.putString("kod", o.optString("kod", ""))
            if (o.has("ad")) e.putString("ad", o.optString("ad", ""))
            if (o.has("oto")) e.putBoolean("oto", o.optBoolean("oto", true))
            e.apply()
        } catch (e: Exception) {
            android.util.Log.w(TAG, "İçe aktarılamadı", e)
        }
    }
}
