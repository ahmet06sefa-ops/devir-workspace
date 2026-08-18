package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * v7.59 — Yapay zekâ sohbetlerinin kalıcı geçmişi.
 *
 * ── Kullanıcının isteği ──
 * "Yapay zeka konuşmasını yan sekmeden hatırlasın ve tıklayınca
 *  o konuşmadan devam edebileyim"
 *
 * ── Nasıl çalışıyor ──
 * Her sohbet bir [Sohbet] nesnesi: kimlik, başlık, mesaj listesi ve
 * son değişiklik zamanı. Asistan ekranında her mesaj yazıldığında
 * aktif sohbete eklenir ve diske yazılır — uygulama kapansa bile durur.
 *
 * Yan panelden bir sohbete dokunulduğunda o sohbet "aktif" yapılır;
 * asistan ekranı açılınca mesajlar baloncuk olarak yeniden çizilir ve
 * `AiClient.chat()` geçmişi de doldurulur — yani model kaldığı yerden
 * bağlamı hatırlayarak devam eder.
 *
 * ── Sınırlar (bilinçli) ──
 * · En fazla [MAX_SOHBET] sohbet saklanır; en eskisi silinir.
 * · Bir sohbette en fazla [MAX_MESAJ] mesaj tutulur.
 * · Modele gönderilen geçmiş son 16 mesajla sınırlı (token bütçesi).
 */
object SohbetGecmisi {

    private const val TAG = "SohbetGecmisi"
    private const val PREF = "ai_sohbet_v1"
    private const val K_LISTE = "sohbetler"
    private const val K_AKTIF = "aktif_id"

    /** Saklanacak en fazla sohbet sayısı. */
    private const val MAX_SOHBET = 40

    /** Bir sohbette saklanacak en fazla mesaj. */
    private const val MAX_MESAJ = 200

    /** Modele gönderilecek en fazla geçmiş mesaj (token bütçesi). */
    const val MODEL_GECMIS = 16

    // ═══════════════════════════════════════════════════════════════
    // MODEL
    // ═══════════════════════════════════════════════════════════════

    /**
     * Tek bir mesaj.
     * @param rol "user" = kullanıcı · "assistant" = yapay zekâ
     */
    data class Mesaj(
        val rol: String,
        val metin: String,
        val zaman: Long = System.currentTimeMillis()
    ) {
        val kullaniciMi: Boolean get() = rol == "user"
    }

    /** Bir sohbet oturumu. */
    data class Sohbet(
        val id: Long,
        var baslik: String,
        val mesajlar: MutableList<Mesaj> = mutableListOf(),
        var guncellendi: Long = System.currentTimeMillis(),
        /** v7.75: sabitlenmis sohbet listede en ustte kalir. */
        var sabit: Boolean = false
    ) {
        /** Listede gösterilecek önizleme — son mesajın başı. */
        val onizleme: String
            get() = mesajlar.lastOrNull()?.metin?.replace("\n", " ")?.take(70).orEmpty()
    }

    // ═══════════════════════════════════════════════════════════════
    // OKUMA / YAZMA
    // ═══════════════════════════════════════════════════════════════

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    /** Tüm sohbetler — en yeni en üstte. */
    fun tumu(context: Context): MutableList<Sohbet> {
        val liste = mutableListOf<Sohbet>()
        try {
            val dizi = JSONArray(prefs(context).getString(K_LISTE, "[]") ?: "[]")
            for (i in 0 until dizi.length()) {
                val o = dizi.optJSONObject(i) ?: continue
                val mesajlar = mutableListOf<Mesaj>()
                o.optJSONArray("m")?.let { md ->
                    for (j in 0 until md.length()) {
                        val mo = md.optJSONObject(j) ?: continue
                        mesajlar.add(
                            Mesaj(
                                rol = mo.optString("r", "user"),
                                metin = mo.optString("t"),
                                zaman = mo.optLong("z")
                            )
                        )
                    }
                }
                liste.add(
                    Sohbet(
                        id = o.optLong("i"),
                        baslik = o.optString("b"),
                        mesajlar = mesajlar,
                        guncellendi = o.optLong("g"),
                        sabit = o.optBoolean("s", false)
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Sohbetler okunamadı", e)
        }
        // v7.75: sabitlenenler her zaman en ustte
        liste.sortWith(compareByDescending<Sohbet> { it.sabit }.thenByDescending { it.guncellendi })
        return liste
    }

    private fun kaydet(context: Context, liste: List<Sohbet>) {
        try {
            // En yeniler kalsın
            val kirpik = liste.sortedByDescending { it.guncellendi }.take(MAX_SOHBET)
            val dizi = JSONArray()
            kirpik.forEach { s ->
                val md = JSONArray()
                s.mesajlar.takeLast(MAX_MESAJ).forEach { m ->
                    md.put(
                        JSONObject().put("r", m.rol).put("t", m.metin).put("z", m.zaman)
                    )
                }
                dizi.put(
                    JSONObject()
                        .put("i", s.id)
                        .put("b", s.baslik)
                        .put("g", s.guncellendi)
                        .put("s", s.sabit)
                        .put("m", md)
                )
            }
            prefs(context).edit().putString(K_LISTE, dizi.toString()).apply()
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Sohbetler yazılamadı", e)
        }
    }

    fun bul(context: Context, id: Long): Sohbet? = tumu(context).firstOrNull { it.id == id }

    fun adet(context: Context): Int = tumu(context).size

    // ═══════════════════════════════════════════════════════════════
    // AKTİF SOHBET
    // ═══════════════════════════════════════════════════════════════

    fun aktifId(context: Context): Long = prefs(context).getLong(K_AKTIF, 0L)

    fun setAktif(context: Context, id: Long) {
        prefs(context).edit().putLong(K_AKTIF, id).apply()
    }

    /**
     * Yeni boş sohbet başlatır ve aktif yapar.
     * Mesaj eklenene kadar listeye yazılmaz — boş sohbet birikmesin.
     */
    fun yeniBaslat(context: Context): Long {
        val id = System.currentTimeMillis()
        setAktif(context, id)
        return id
    }

    /** Aktif sohbeti döndürür; yoksa null. */
    fun aktifSohbet(context: Context): Sohbet? {
        val id = aktifId(context)
        if (id == 0L) return null
        return bul(context, id)
    }

    // ═══════════════════════════════════════════════════════════════
    // MESAJ EKLEME
    // ═══════════════════════════════════════════════════════════════

    /**
     * Aktif sohbete mesaj ekler. Sohbet yoksa oluşturur.
     *
     * Başlık ilk kullanıcı mesajından türetilir — kullanıcı elle
     * isim vermek zorunda kalmasın.
     */
    fun mesajEkle(context: Context, rol: String, metin: String) {
        if (metin.isBlank()) return
        try {
            var id = aktifId(context)
            if (id == 0L) id = yeniBaslat(context)

            val liste = tumu(context)
            var sohbet = liste.firstOrNull { it.id == id }
            if (sohbet == null) {
                sohbet = Sohbet(
                    id = id,
                    baslik = context.getString(R.string.sg_baslik_yeni)
                )
                liste.add(sohbet)
            }
            sohbet.mesajlar.add(Mesaj(rol, metin.take(4000)))
            sohbet.guncellendi = System.currentTimeMillis()

            // İlk kullanıcı mesajı başlığı belirlesin
            if (rol == "user" &&
                sohbet.baslik == context.getString(R.string.sg_baslik_yeni)
            ) {
                sohbet.baslik = baslikUret(metin)
            }
            kaydet(context, liste)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Mesaj eklenemedi", e)
        }
    }

    /** Mesaj metninden okunabilir bir başlık üretir. */
    private fun baslikUret(metin: String): String {
        val tek = metin.replace("\n", " ").trim()
        return if (tek.length <= 38) tek else tek.take(36).trimEnd() + "…"
    }

    // ═══════════════════════════════════════════════════════════════
    // DÜZENLEME
    // ═══════════════════════════════════════════════════════════════

    fun adDegistir(context: Context, id: Long, yeniAd: String) {
        if (yeniAd.isBlank()) return
        val liste = tumu(context)
        liste.firstOrNull { it.id == id }?.baslik = yeniAd.trim().take(60)
        kaydet(context, liste)
    }

    fun sil(context: Context, id: Long) {
        kaydet(context, tumu(context).filterNot { it.id == id })
        if (aktifId(context) == id) {
            prefs(context).edit().remove(K_AKTIF).apply()
        }
    }

    /**
     * v7.75 — Sohbetlerde arama.
     * Baslik ve mesaj icerigi taranir; Turkce duyarli.
     */
    fun ara(context: Context, sorgu: String): List<Sohbet> {
        val q = Arama.normalle(sorgu)
        if (q.length < 2) return tumu(context)
        return tumu(context).filter { s ->
            Arama.normalle(s.baslik).contains(q) ||
                s.mesajlar.any { Arama.normalle(it.metin).contains(q) }
        }
    }

    /** v7.75: Sohbeti sabitler / sabiti kaldirir. */
    fun sabitDegistir(context: Context, id: Long) {
        val liste = tumu(context)
        liste.firstOrNull { it.id == id }?.let { it.sabit = !it.sabit }
        kaydet(context, liste)
    }

    /**
     * v7.75 — Sohbeti dosyaya yazar.
     *
     * Uygulamanin disa aktarma klasorune (`Downloads` degil, uygulama
     * klasoru) yazilir; paylas menusuyle disari gonderilebilir.
     *
     * @return olusan dosya, hata olursa null
     */
    fun dosyayaYaz(context: Context, id: Long): java.io.File? = try {
        val metin = metneCevir(context, id)
        if (metin.isBlank()) null else {
            val klasor = context.getExternalFilesDir(null) ?: context.filesDir
            val ad = "sohbet-" + id + ".txt"
            java.io.File(klasor, ad).apply { writeText(metin) }
        }
    } catch (e: Exception) {
        android.util.Log.w(TAG, "Dosyaya yazilamadi", e)
        null
    }

    fun tumunuSil(context: Context) {
        prefs(context).edit().remove(K_LISTE).remove(K_AKTIF).apply()
    }

    /**
     * Modele gönderilecek geçmiş — (rol, metin) çiftleri.
     * Son [MODEL_GECMIS] mesajla sınırlı.
     */
    fun modelGecmisi(context: Context): List<Pair<String, String>> {
        val s = aktifSohbet(context) ?: return emptyList()
        return s.mesajlar.takeLast(MODEL_GECMIS).map { it.rol to it.metin }
    }

    /** Sohbeti paylaşılabilir düz metne çevirir. */
    fun metneCevir(context: Context, id: Long): String {
        val s = bul(context, id) ?: return ""
        return buildString {
            append(s.baslik).append("\n\n")
            s.mesajlar.forEach { m ->
                append(if (m.kullaniciMi) "🧑 " else "🤖 ")
                append(m.metin).append("\n\n")
            }
        }.trim()
    }

    /** "3 saat önce" gibi okunabilir zaman. */
    fun zamanMetni(ms: Long): String {
        if (ms <= 0) return ""
        val fark = System.currentTimeMillis() - ms
        val dk = fark / 60_000
        return when {
            dk < 1 -> "az önce"
            dk < 60 -> dk.toString() + " dk önce"
            dk < 1440 -> (dk / 60).toString() + " saat önce"
            dk < 2880 -> "dün"
            else -> try {
                java.text.SimpleDateFormat("d MMM", java.util.Locale("tr", "TR"))
                    .format(java.util.Date(ms))
            } catch (_: Exception) {
                ""
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // YEDEKLEME
    // ═══════════════════════════════════════════════════════════════

    fun disaAktar(context: Context): String =
        prefs(context).getString(K_LISTE, "[]") ?: "[]"

    fun iceAktar(context: Context, json: String) {
        try {
            JSONArray(json)   // geçerlilik kontrolü
            prefs(context).edit().putString(K_LISTE, json).apply()
        } catch (e: Exception) {
            android.util.Log.w(TAG, "İçe aktarılamadı", e)
        }
    }
}
