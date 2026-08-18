package com.gunlukasistan.app

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * v7.78 — Resimli kanıt deposu.
 *
 * ── Kullanıcının isteği ──
 * "Görevleri vb şeyleri resimli kanıt iste ve yapay zeka kontrolü ekle"
 *
 * ── Neden görev kaydına sütun EKLENMEDİ ──
 * `Store.Task` bir Room tablosuna (v7.76) bağlı. Oraya alan eklemek şema
 * sürümünü yükseltir; veritabanı `fallbackToDestructiveMigration` ile
 * kurulduğu için yanlış bir adımda **tüm görevler silinebilirdi**.
 * v7.76 geçişi henüz gerçek cihazda doğrulanmadığı için üstüne şema
 * değişikliği koymak riski katlıyordu.
 *
 * Bunun yerine kanıtlar kendi deposunda (SharedPreferences + JSON) durur
 * ve göreve **id ile** bağlanır. Room şeması hiç değişmez, veri kaybı
 * riski sıfırdır. Bedeli: silinen görevin kanıt kaydı artık kalır —
 * [artiklariTemizle] bunu toplar.
 *
 * ── Fotoğraflar nerede ──
 * `filesDir/kanit/` altında. Cache değil çünkü kanıt geçmişi kalıcı
 * olmalı; sistem cache'i istediği an siler.
 */
object Kanit {

    private const val TAG = "Kanit"
    private const val PREF = "kanit_v1"
    private const val K_KAYITLAR = "kayitlar_json"
    private const val K_ISTEYENLER = "isteyen_gorevler"

    // ═══════════════════════════════════════════════════════════════
    // DURUMLAR
    // ═══════════════════════════════════════════════════════════════

    /** Henüz kanıt yok. */
    const val YOK = 0

    /** Fotoğraf var ama yapay zekâ denetlemedi (çevrimdışı / anahtar yok). */
    const val BEKLIYOR = 1

    /** Yapay zekâ kanıtı kabul etti. */
    const val ONAYLI = 2

    /** Yapay zekâ kanıtı reddetti. */
    const val RED = 3

    /** Kullanıcı itiraz etti, elle onaylandı. */
    const val ITIRAZ = 4

    // ═══════════════════════════════════════════════════════════════
    // POLİTİKA — hangi görevler kanıt ister
    // ═══════════════════════════════════════════════════════════════

    const val POL_KAPALI = 0
    const val POL_ISARETLI = 1
    const val POL_ETIKETLI = 2
    const val POL_HEPSI = 3

    // ═══════════════════════════════════════════════════════════════
    // KATILIK — yapay zekâ ne kadar sert denetler
    // ═══════════════════════════════════════════════════════════════

    const val KATI_GEVSEK = 0
    const val KATI_NORMAL = 1
    const val KATI_SERT = 2

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ═══════════════════════════════════════════════════════════════
    // MODEL
    // ═══════════════════════════════════════════════════════════════

    /**
     * Bir görevin kanıt kaydı.
     *
     * @param guven yapay zekânın kararına güveni (0-100)
     * @param deneme kaç kez fotoğraf çekildi — ısrarcı red sayacı
     */
    data class Kayit(
        val gorevId: Long,
        var yol: String = "",
        var durum: Int = YOK,
        var guven: Int = 0,
        var gerekce: String = "",
        var zaman: Long = 0L,
        var deneme: Int = 0
    ) {
        val onaylandiMi: Boolean get() = durum == ONAYLI || durum == ITIRAZ
        val fotoVar: Boolean get() = yol.isNotBlank() && File(yol).exists()
    }

    // ═══════════════════════════════════════════════════════════════
    // AYARLAR
    // ═══════════════════════════════════════════════════════════════

    fun politika(context: Context): Int =
        prefs(context).getInt("politika", POL_ISARETLI)

    fun setPolitika(context: Context, p: Int) {
        prefs(context).edit().putInt("politika", p.coerceIn(0, 3)).apply()
    }

    fun katilik(context: Context): Int =
        prefs(context).getInt("katilik", KATI_NORMAL)

    fun setKatilik(context: Context, k: Int) {
        prefs(context).edit().putInt("katilik", k.coerceIn(0, 2)).apply()
    }

    /** Politika ETIKETLI iken hangi etiketler kanıt ister (kod listesi). */
    fun etiketler(context: Context): Set<String> =
        prefs(context).getStringSet("etiketler", setOf("o", "c")) ?: emptySet()

    fun setEtiketler(context: Context, kodlar: Set<String>) {
        prefs(context).edit().putStringSet("etiketler", kodlar).apply()
    }

    /**
     * Reddedilen kanıtta görev yine de tamamlansın mı?
     *
     * Varsayılan **false**: kullanıcı "beni zorla" dedi. Red gelirse görev
     * açık kalır. İsteyen gevşetebilir.
     */
    fun redEngeller(context: Context): Boolean =
        prefs(context).getBoolean("red_engeller", true)

    fun setRedEngeller(context: Context, engeller: Boolean) {
        prefs(context).edit().putBoolean("red_engeller", engeller).apply()
    }

    /** Çevrimdışıyken (AI yokken) kanıt fotoğrafı yeterli sayılsın mı. */
    fun cevrimdisiKabul(context: Context): Boolean =
        prefs(context).getBoolean("cevrimdisi_kabul", true)

    fun setCevrimdisiKabul(context: Context, kabul: Boolean) {
        prefs(context).edit().putBoolean("cevrimdisi_kabul", kabul).apply()
    }

    // ═══════════════════════════════════════════════════════════════
    // "BU GÖREV KANIT İSTER" İŞARETİ
    // ═══════════════════════════════════════════════════════════════

    private fun isteyenler(context: Context): MutableSet<String> =
        HashSet(prefs(context).getStringSet(K_ISTEYENLER, emptySet()) ?: emptySet())

    fun isaretliMi(context: Context, gorevId: Long): Boolean =
        isteyenler(context).contains(gorevId.toString())

    fun isaretle(context: Context, gorevId: Long, ister: Boolean) {
        val küme = isteyenler(context)
        if (ister) küme.add(gorevId.toString()) else küme.remove(gorevId.toString())
        prefs(context).edit().putStringSet(K_ISTEYENLER, küme).apply()
    }

    /**
     * Bu görev tamamlanırken kanıt istenmeli mi?
     *
     * Tekrarlı görevlerde de çalışır: görev id'si sabit kaldığı için
     * işaret her tekrarda geçerlidir.
     */
    fun gerekliMi(context: Context, gorev: Store.Task): Boolean =
        when (politika(context)) {
            POL_KAPALI -> false
            POL_ISARETLI -> isaretliMi(context, gorev.id)
            POL_ETIKETLI -> etiketler(context).contains(gorev.etiket) ||
                isaretliMi(context, gorev.id)
            POL_HEPSI -> true
            else -> false
        }

    // ═══════════════════════════════════════════════════════════════
    // KAYIT OKUMA / YAZMA
    // ═══════════════════════════════════════════════════════════════

    fun hepsi(context: Context): MutableList<Kayit> {
        val ham = prefs(context).getString(K_KAYITLAR, "[]") ?: "[]"
        val liste = mutableListOf<Kayit>()
        try {
            val dizi = JSONArray(ham)
            for (i in 0 until dizi.length()) {
                val o = dizi.optJSONObject(i) ?: continue
                liste.add(
                    Kayit(
                        gorevId = o.optLong("id"),
                        yol = o.optString("yol"),
                        durum = o.optInt("durum"),
                        guven = o.optInt("guven"),
                        gerekce = o.optString("gerekce"),
                        zaman = o.optLong("zaman"),
                        deneme = o.optInt("deneme")
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Kanıtlar okunamadı", e)
        }
        return liste
    }

    private fun yaz(context: Context, liste: List<Kayit>) {
        val dizi = JSONArray()
        liste.forEach { k ->
            dizi.put(
                JSONObject()
                    .put("id", k.gorevId)
                    .put("yol", k.yol)
                    .put("durum", k.durum)
                    .put("guven", k.guven)
                    .put("gerekce", k.gerekce)
                    .put("zaman", k.zaman)
                    .put("deneme", k.deneme)
            )
        }
        prefs(context).edit().putString(K_KAYITLAR, dizi.toString()).apply()
    }

    fun bul(context: Context, gorevId: Long): Kayit? =
        hepsi(context).firstOrNull { it.gorevId == gorevId }

    fun kaydet(context: Context, kayit: Kayit) {
        val liste = hepsi(context)
        val yer = liste.indexOfFirst { it.gorevId == kayit.gorevId }
        if (yer >= 0) liste[yer] = kayit else liste.add(kayit)
        yaz(context, liste)
    }

    fun sil(context: Context, gorevId: Long) {
        val liste = hepsi(context)
        liste.firstOrNull { it.gorevId == gorevId }?.let { k ->
            if (k.yol.isNotBlank()) runCatching { File(k.yol).delete() }
        }
        yaz(context, liste.filter { it.gorevId != gorevId })
    }

    /**
     * Tekrarlı görev yenilendiğinde çağrılır.
     *
     * Kanıt kaydı sıfırlanır ama fotoğraf **silinmez** — geçmiş arşivde
     * kalsın diye [gecmiseAt] ile taşınır.
     */
    fun tekrarIcinSifirla(context: Context, gorevId: Long) {
        val kayit = bul(context, gorevId) ?: return
        if (kayit.fotoVar) gecmiseAt(context, gorevId, kayit)
        kaydet(context, Kayit(gorevId))
    }

    // ═══════════════════════════════════════════════════════════════
    // GEÇMİŞ — tamamlanmış kanıtların arşivi
    // ═══════════════════════════════════════════════════════════════

    private const val K_GECMIS = "gecmis_json"
    private const val GECMIS_TAVAN = 200

    data class GecmisKayit(
        val gorevId: Long,
        val baslik: String,
        val yol: String,
        val durum: Int,
        val zaman: Long
    )

    fun gecmis(context: Context): MutableList<GecmisKayit> {
        val ham = prefs(context).getString(K_GECMIS, "[]") ?: "[]"
        val liste = mutableListOf<GecmisKayit>()
        try {
            val dizi = JSONArray(ham)
            for (i in 0 until dizi.length()) {
                val o = dizi.optJSONObject(i) ?: continue
                liste.add(
                    GecmisKayit(
                        gorevId = o.optLong("id"),
                        baslik = o.optString("baslik"),
                        yol = o.optString("yol"),
                        durum = o.optInt("durum"),
                        zaman = o.optLong("zaman")
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Kanıt geçmişi okunamadı", e)
        }
        return liste
    }

    fun gecmiseAt(context: Context, gorevId: Long, kayit: Kayit, baslik: String = "") {
        val liste = gecmis(context)
        liste.add(0, GecmisKayit(gorevId, baslik, kayit.yol, kayit.durum, kayit.zaman))
        // Tavanı aşan en eski kayıtların fotoğrafını da sil — disk şişmesin
        while (liste.size > GECMIS_TAVAN) {
            val atilan = liste.removeAt(liste.size - 1)
            runCatching { File(atilan.yol).delete() }
        }
        val dizi = JSONArray()
        liste.forEach { g ->
            dizi.put(
                JSONObject()
                    .put("id", g.gorevId).put("baslik", g.baslik)
                    .put("yol", g.yol).put("durum", g.durum).put("zaman", g.zaman)
            )
        }
        prefs(context).edit().putString(K_GECMIS, dizi.toString()).apply()
    }

    // ═══════════════════════════════════════════════════════════════
    // DOSYA
    // ═══════════════════════════════════════════════════════════════

    /** Kanıt fotoğraflarının klasörü. */
    fun klasor(context: Context): File =
        File(context.filesDir, "kanit").apply { if (!exists()) mkdirs() }

    /** Yeni bir kanıt fotoğrafı için hedef dosya. */
    fun yeniDosya(context: Context, gorevId: Long): File =
        File(klasor(context), "k_${gorevId}_${System.currentTimeMillis()}.jpg")

    /** Kamera için FileProvider adresi. */
    fun uriVer(context: Context, dosya: File): Uri =
        androidx.core.content.FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", dosya
        )

    /**
     * Artık var olmayan görevlerin kanıtlarını temizler.
     *
     * Kanıt ayrı depoda durduğu için görev silindiğinde kayıt sızıntı
     * yapar. Bu, ayarlar ekranından ve arşivleme sırasında çağrılır.
     *
     * @return silinen kayıt sayısı
     */
    fun artiklariTemizle(context: Context): Int {
        val yasayan = Store.loadTasks(context).map { it.id }.toSet()
        val liste = hepsi(context)
        val olenler = liste.filter { it.gorevId !in yasayan }
        if (olenler.isEmpty()) return 0
        olenler.forEach { k ->
            if (k.yol.isNotBlank()) runCatching { File(k.yol).delete() }
        }
        yaz(context, liste.filter { it.gorevId in yasayan })
        // İşaret kümesinden de düş
        val küme = isteyenler(context)
        val temiz = küme.filter { it.toLongOrNull() in yasayan }.toSet()
        prefs(context).edit().putStringSet(K_ISTEYENLER, temiz).apply()
        return olenler.size
    }

    /** Kanıt klasörünün toplam boyutu (bayt). */
    fun diskKullanimi(context: Context): Long =
        klasor(context).listFiles()?.sumOf { it.length() } ?: 0L

    fun boyutMetni(bayt: Long): String = when {
        bayt < 1024 -> "$bayt B"
        bayt < 1024 * 1024 -> "${bayt / 1024} KB"
        else -> String.format(Locale.US, "%.1f MB", bayt / 1048576.0)
    }

    fun zamanMetni(ms: Long): String =
        if (ms <= 0) "" else SimpleDateFormat("d MMM HH:mm", Locale("tr")).format(Date(ms))

    // ═══════════════════════════════════════════════════════════════
    // İSTATİSTİK
    // ═══════════════════════════════════════════════════════════════

    data class Ozet(val toplam: Int, val onayli: Int, val red: Int) {
        val yuzde: Int get() = if (toplam == 0) 0 else onayli * 100 / toplam
    }

    fun ozet(context: Context): Ozet {
        val tum = gecmis(context)
        return Ozet(
            toplam = tum.size,
            onayli = tum.count { it.durum == ONAYLI || it.durum == ITIRAZ },
            red = tum.count { it.durum == RED }
        )
    }

    fun durumMetni(context: Context, durum: Int): String = context.getString(
        when (durum) {
            ONAYLI -> R.string.kn_durum_onayli
            RED -> R.string.kn_durum_red
            BEKLIYOR -> R.string.kn_durum_bekliyor
            ITIRAZ -> R.string.kn_durum_itiraz
            else -> R.string.kn_durum_yok
        }
    )
}
