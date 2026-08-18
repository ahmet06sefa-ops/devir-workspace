package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * v7.83 — Yanlış cevaplanan soruların defteri.
 *
 * ── Neden gerekli ──
 * [QuizStore] sonucu **ders bazında** tutuyor: "8/10 doğru". Ama hangi
 * sorunun yanlış olduğunu bilmiyor. Öğrenmede asıl değer burada:
 * doğru bildiğin soruyu tekrar çözmek zaman kaybı, yanlış bildiğini
 * tekrar çözmek öğrenmenin kendisi.
 *
 * ── Aralıklı tekrar (Leitner) ──
 * [QuizStore.TekrarKaydi] ders seviyesinde aralıklı tekrar yapıyor.
 * Burada aynı mantık **soru seviyesinde** işliyor:
 *   · Yanlış → kutu 0'a düşer, yarın tekrar sorulur
 *   · Doğru  → kutu +1, aralık uzar (1 · 3 · 7 · 16 · 35 gün)
 *   · Kutu 4'ü geçen soru "öğrenildi" sayılır ve listeden çıkar
 *
 * Bu, ders bazlı tekrarın yerine geçmiyor; onu tamamlıyor.
 */
object Hatalarim {

    private const val TAG = "Hatalarim"
    private const val PREF = "hatalarim_v1"
    private const val K_KAYIT = "kayitlar_json"

    /** Leitner aralıkları (gün). Son kutuyu geçen soru öğrenilmiş sayılır. */
    private val ARALIKLAR = intArrayOf(1, 3, 7, 16, 35)

    /** Defterde en fazla kaç soru tutulur — sonsuz büyümesin. */
    private const val TAVAN = 400

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    private fun bugun(): String =
        SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

    private fun gunEkle(gun: Int): String {
        val c = java.util.Calendar.getInstance()
        c.add(java.util.Calendar.DAY_OF_YEAR, gun)
        return SimpleDateFormat("yyyyMMdd", Locale.US).format(c.time)
    }

    // ═══════════════════════════════════════════════════════════════
    // MODEL
    // ═══════════════════════════════════════════════════════════════

    /**
     * Yanlış cevaplanmış bir soru.
     *
     * Sorunun kendisi burada **kopyalanarak** saklanıyor, [QuizStore]'a
     * referans verilmiyor. Sebep: kullanıcı "Soruları yenile" derse eski
     * sorular silinir; hata defteri o zaman boşalırdı. Kopya sayesinde
     * defter bağımsız yaşıyor.
     */
    data class Hata(
        val soruId: Long,
        val lessonId: Long,
        val kaynak: String,
        val metin: String,
        val siklar: List<String>,
        val dogru: Int,
        val aciklama: String,
        var kutu: Int = 0,
        var sonrakiGun: String = "",
        var yanlisSayisi: Int = 1,
        var dogruUstUste: Int = 0,
        var sonGorulme: Long = 0L
    ) {
        val ogrenildi: Boolean get() = kutu >= ARALIKLAR.size

        fun soruya(): QuizStore.Soru = QuizStore.Soru(
            id = soruId,
            lessonId = lessonId,
            metin = metin,
            siklar = siklar,
            dogru = dogru,
            aciklama = aciklama
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // OKUMA / YAZMA
    // ═══════════════════════════════════════════════════════════════

    fun hepsi(context: Context): MutableList<Hata> {
        val ham = prefs(context).getString(K_KAYIT, "[]") ?: "[]"
        val liste = mutableListOf<Hata>()
        try {
            val dizi = JSONArray(ham)
            for (i in 0 until dizi.length()) {
                val o = dizi.optJSONObject(i) ?: continue
                val sikDizi = o.optJSONArray("siklar") ?: JSONArray()
                val siklar = mutableListOf<String>()
                for (j in 0 until sikDizi.length()) siklar.add(sikDizi.optString(j))
                if (siklar.size < 2) continue

                liste.add(
                    Hata(
                        soruId = o.optLong("id"),
                        lessonId = o.optLong("lid"),
                        kaynak = o.optString("kaynak"),
                        metin = o.optString("metin"),
                        siklar = siklar,
                        dogru = o.optInt("dogru"),
                        aciklama = o.optString("aciklama"),
                        kutu = o.optInt("kutu"),
                        sonrakiGun = o.optString("sonraki"),
                        yanlisSayisi = o.optInt("yanlis", 1),
                        dogruUstUste = o.optInt("dust"),
                        sonGorulme = o.optLong("son")
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Hata defteri okunamadı", e)
        }
        return liste
    }

    private fun yaz(context: Context, liste: List<Hata>) {
        // En çok yanlış yapılanlar korunsun; tavanı aşarsa öğrenilmişleri at
        val kirpik = if (liste.size <= TAVAN) liste
        else liste.sortedByDescending { it.yanlisSayisi }.take(TAVAN)

        val dizi = JSONArray()
        kirpik.forEach { h ->
            dizi.put(
                JSONObject()
                    .put("id", h.soruId).put("lid", h.lessonId)
                    .put("kaynak", h.kaynak).put("metin", h.metin)
                    .put("siklar", JSONArray(h.siklar))
                    .put("dogru", h.dogru).put("aciklama", h.aciklama)
                    .put("kutu", h.kutu).put("sonraki", h.sonrakiGun)
                    .put("yanlis", h.yanlisSayisi).put("dust", h.dogruUstUste)
                    .put("son", h.sonGorulme)
            )
        }
        prefs(context).edit().putString(K_KAYIT, dizi.toString()).apply()
    }

    // ═══════════════════════════════════════════════════════════════
    // KAYIT
    // ═══════════════════════════════════════════════════════════════

    /**
     * Yanlış cevaplanan soruyu deftere ekler (varsa günceller).
     *
     * @param kaynak sorunun geldiği ders/konu adı — listede gösterilir
     */
    fun yanlisEkle(context: Context, soru: QuizStore.Soru, kaynak: String) {
        if (!soru.gecerli) return
        val liste = hepsi(context)
        val mevcut = liste.firstOrNull { it.soruId == soru.id }

        if (mevcut != null) {
            // Tekrar yanlış: kutuyu sıfırla, yarın yine sor
            mevcut.kutu = 0
            mevcut.sonrakiGun = gunEkle(1)
            mevcut.yanlisSayisi++
            mevcut.dogruUstUste = 0
            mevcut.sonGorulme = System.currentTimeMillis()
        } else {
            liste.add(
                Hata(
                    soruId = soru.id,
                    lessonId = soru.lessonId,
                    kaynak = kaynak,
                    metin = soru.metin,
                    siklar = soru.siklar,
                    dogru = soru.dogru,
                    aciklama = soru.aciklama,
                    kutu = 0,
                    sonrakiGun = gunEkle(1),
                    sonGorulme = System.currentTimeMillis()
                )
            )
        }
        yaz(context, liste)
    }

    /**
     * Defterdeki bir soru doğru cevaplandı.
     *
     * Kutu ilerler, aralık uzar. Son kutuyu geçerse soru **defterden
     * silinir** — öğrenilmiş demektir, listeyi şişirmesin.
     */
    fun dogruCevaplandi(context: Context, soruId: Long) {
        val liste = hepsi(context)
        val h = liste.firstOrNull { it.soruId == soruId } ?: return

        h.kutu++
        h.dogruUstUste++
        h.sonGorulme = System.currentTimeMillis()

        if (h.kutu >= ARALIKLAR.size) {
            // Öğrenildi — defterden çıkar
            yaz(context, liste.filterNot { it.soruId == soruId })
            return
        }
        h.sonrakiGun = gunEkle(ARALIKLAR[h.kutu])
        yaz(context, liste)
    }

    fun sil(context: Context, soruId: Long) {
        yaz(context, hepsi(context).filterNot { it.soruId == soruId })
    }

    fun temizle(context: Context) {
        prefs(context).edit().remove(K_KAYIT).apply()
    }

    // ═══════════════════════════════════════════════════════════════
    // SORGULAR
    // ═══════════════════════════════════════════════════════════════

    /** Bugün tekrar edilmesi gereken sorular. */
    fun bugunkuler(context: Context): List<Hata> {
        val b = bugun()
        return hepsi(context)
            .filter { it.sonrakiGun.isBlank() || it.sonrakiGun <= b }
            .sortedByDescending { it.yanlisSayisi }
    }

    fun bugunkuSayi(context: Context): Int = bugunkuler(context).size

    fun toplamSayi(context: Context): Int = hepsi(context).size

    /** En çok yanlış yapılan sorular — "zayıf noktaların" listesi. */
    fun enZorlar(context: Context, adet: Int = 10): List<Hata> =
        hepsi(context).sortedByDescending { it.yanlisSayisi }.take(adet)

    /** Kaynağa göre gruplu sayım — hangi konuda zayıfsın. */
    fun kaynakDagilimi(context: Context): List<Pair<String, Int>> =
        hepsi(context)
            .groupBy { it.kaynak.ifBlank { "—" } }
            .map { (k, v) -> k to v.size }
            .sortedByDescending { it.second }

    /**
     * Tekrar oturumu için soruları hazırlar.
     *
     * Önce bugün vadesi gelenler, yetmezse en çok yanlış yapılanlar.
     * Böylece "bugün tekrar yok" deyip kullanıcıyı boş çevirmiyoruz.
     */
    fun tekrarSorulari(context: Context, adet: Int = 10): List<QuizStore.Soru> {
        val bugunku = bugunkuler(context)
        if (bugunku.size >= adet) return bugunku.take(adet).map { it.soruya() }

        val kalan = adet - bugunku.size
        val digerleri = hepsi(context)
            .filterNot { h -> bugunku.any { it.soruId == h.soruId } }
            .sortedByDescending { it.yanlisSayisi }
            .take(kalan)

        return (bugunku + digerleri).map { it.soruya() }
    }

    fun tarihMetni(gun: String): String =
        if (gun.length == 8) "${gun.substring(6)}.${gun.substring(4, 6)}" else gun

    // ═══════════════════════════════════════════════════════════════
    // ÖZET
    // ═══════════════════════════════════════════════════════════════

    data class Ozet(val toplam: Int, val bugun: Int, val ogrenilen: Int)

    private const val K_OGRENILEN = "ogrenilen_sayi"

    /** Öğrenilip defterden çıkan soru sayısı — motivasyon göstergesi. */
    fun ogrenilenSayisi(context: Context): Int =
        prefs(context).getInt(K_OGRENILEN, 0)

    fun ogrenileniArtir(context: Context) {
        prefs(context).edit()
            .putInt(K_OGRENILEN, ogrenilenSayisi(context) + 1).apply()
    }

    fun ozet(context: Context): Ozet = Ozet(
        toplam = toplamSayi(context),
        bugun = bugunkuSayi(context),
        ogrenilen = ogrenilenSayisi(context)
    )

    // ═══════════════════════════════════════════════════════════════
    // v7.84 — BENZER SORU ÜRETİMİ
    // ═══════════════════════════════════════════════════════════════

    class BenzerSonuc(
        val ok: Boolean,
        val sorular: List<QuizStore.Soru> = emptyList(),
        val hata: String = ""
    )

    /**
     * Yanlış yapılan sorudan **aynı kavramı farklı açıdan ölçen** yeni
     * sorular üretir.
     *
     * ── Neden gerekli ──
     * Aynı soruyu tekrar tekrar çözmek ezberi ödüllendirir: kullanıcı
     * kavramı değil, "C şıkkı" cevabını hatırlar. Aynı kavramı farklı
     * biçimde sormak gerçekten öğrenilip öğrenilmediğini ölçer.
     *
     * Üretilen sorular hata defterine **eklenmez**; yalnızca o oturumda
     * çözülür. Yanlış yapılırsa zaten [yanlisEkle] ile deftere düşer.
     *
     * **Ağ isteği yapar — arka planda çağır.**
     */
    fun benzerUret(context: Context, hata: Hata, adet: Int = 3): BenzerSonuc {
        if (!AiSettings.isReady(context)) {
            return BenzerSonuc(false, hata = context.getString(R.string.kn_ai_hazir_degil))
        }

        val dogruSik = hata.siklar.getOrNull(hata.dogru).orEmpty()
        val baglam = if (hata.kaynak.isBlank()) "" else "\nKonu: ${hata.kaynak}"

        val istem = """
Bir öğrenci aşağıdaki soruyu YANLIŞ cevapladı. Aynı kavramı ölçen ama
FARKLI sorulmuş ${adet.coerceIn(1, 5)} yeni soru hazırla.$baglam

ÖĞRENCİNİN YANLIŞ YAPTIĞI SORU:
${hata.metin}
Doğru cevabı: $dogruSik
${if (hata.aciklama.isBlank()) "" else "Açıklama: ${hata.aciklama}"}

KURALLAR:
1. AYNI kavramı ölç ama soruyu farklı kur — kelimesi kelimesine kopyalama.
2. Farklı açılardan sor: örnek üzerinden, tersinden, uygulamalı durum.
3. Öğrenci ezberle değil ANLAYARAK çözebilsin.
4. Her soruda tam 4 şık; biri kesin doğru, diğerleri mantıklı ama yanlış.
5. "aciklama" alanında doğru cevabın nedenini 1-2 cümleyle yaz.
6. Konunun alanını yukarıdaki sorudan anla; kendi uzmanlık alanını varsayma.
7. Türkçe yaz.

SADECE şu JSON'u döndür:
{"sorular":[{"soru":"...","siklar":["A","B","C","D"],"dogru":0,"aciklama":"..."}]}
        """.trim()

        return try {
            val cevap = AiClient.sadeIstek(context, istem, butce = 1800)
            if (!cevap.ok) return BenzerSonuc(false, hata = cevap.text)

            val sorular = benzerAyristir(cevap.text, hata.lessonId)
            if (sorular.isEmpty()) {
                BenzerSonuc(false, hata = context.getString(R.string.quiz_err_parse))
            } else {
                BenzerSonuc(true, sorular)
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Benzer soru üretilemedi", e)
            BenzerSonuc(false, hata = e.message.orEmpty())
        }
    }

    private fun benzerAyristir(ham: String, lessonId: Long): List<QuizStore.Soru> {
        val temiz = ham.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()

        val sonuc = mutableListOf<QuizStore.Soru>()
        try {
            val bas = temiz.indexOf('{')
            val son = temiz.lastIndexOf('}')
            if (bas !in 0 until son) return emptyList()

            val dizi = JSONObject(temiz.substring(bas, son + 1))
                .optJSONArray("sorular") ?: return emptyList()

            for (i in 0 until dizi.length()) {
                val o = dizi.optJSONObject(i) ?: continue
                val metin = o.optString("soru").trim()
                val sikDizi = o.optJSONArray("siklar") ?: continue
                val siklar = mutableListOf<String>()
                for (j in 0 until sikDizi.length()) {
                    sikDizi.optString(j).trim().takeIf { it.isNotBlank() }?.let { siklar.add(it) }
                }
                val dogru = o.optInt("dogru", -1)
                if (metin.isBlank() || siklar.size < 2 || dogru !in siklar.indices) continue

                sonuc.add(
                    QuizStore.Soru(
                        // Negatif ve zamana bağlı: kalıcı sorularla çakışmasın
                        id = -(System.currentTimeMillis() + i),
                        lessonId = lessonId,
                        metin = metin,
                        siklar = siklar,
                        dogru = dogru,
                        aciklama = o.optString("aciklama").trim()
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Benzer sorular ayrıştırılamadı", e)
        }
        return sonuc
    }

    /**
     * Geçici soru havuzu — [QuizActivity] benzer soruları buradan okur.
     *
     * Kalıcı depoya yazmıyoruz: bunlar tek seferlik pekiştirme soruları.
     * Bellekte tutmak yeterli; Activity yeniden oluşsa bile aynı süreçte
     * yaşadığı için erişilebilir.
     */
    @Volatile
    private var geciciHavuz: List<QuizStore.Soru> = emptyList()

    @Volatile
    var geciciBaslik: String = ""
        private set

    fun geciciAyarla(sorular: List<QuizStore.Soru>, baslik: String) {
        geciciHavuz = sorular
        geciciBaslik = baslik
    }

    fun geciciAl(): List<QuizStore.Soru> = geciciHavuz

    fun geciciTemizle() {
        geciciHavuz = emptyList()
        geciciBaslik = ""
    }
}
