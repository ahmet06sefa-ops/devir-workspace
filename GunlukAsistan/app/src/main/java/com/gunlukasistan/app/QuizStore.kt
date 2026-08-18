package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * v7.29 — Quiz ve aralıklı tekrar (spaced repetition) veri katmanı.
 *
 * İki sistem birlikte çalışır:
 *   1. QUIZ      — ders sonunda 5 soru, öğrenildi mi ölçer
 *   2. TEKRAR    — Leitner kutu sistemi, unutmadan önce hatırlatır
 *
 * Aralıklı tekrar mantığı (Leitner):
 *   Kutu 0 → 1 gün sonra    Kutu 3 → 14 gün sonra
 *   Kutu 1 → 3 gün sonra    Kutu 4 → 30 gün sonra
 *   Kutu 2 → 7 gün sonra    Kutu 5 → 90 gün (öğrenildi sayılır)
 *
 * Doğru cevap → bir üst kutuya çıkar (aralık uzar)
 * Yanlış cevap → kutu 0'a düşer (yakında tekrar sorulur)
 */
object QuizStore {

    private const val TAG = "QuizStore"
    private const val PREF = "quiz_store"
    private const val K_SORULAR = "sorular_json"
    private const val K_TEKRAR = "tekrar_json"
    private const val K_SONUC = "sonuclar_json"

    /** Leitner kutularının gün aralıkları. */
    private val ARALIKLAR = intArrayOf(1, 3, 7, 14, 30, 90)

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    private fun bugun(): String =
        SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

    private fun gunEkle(gun: Int): String {
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_YEAR, gun)
        return SimpleDateFormat("yyyyMMdd", Locale.US).format(c.time)
    }

    // ═══════════════════════════════════════════════════════════════
    // SORU MODELİ
    // ═══════════════════════════════════════════════════════════════

    /**
     * Tek bir çoktan seçmeli soru.
     * @param dogru doğru şıkkın dizini (0-3)
     * @param aciklama cevaptan sonra gösterilen kısa açıklama
     */
    data class Soru(
        val id: Long,
        val lessonId: Long,
        val metin: String,
        val siklar: List<String>,
        val dogru: Int,
        val aciklama: String = ""
    ) {
        val gecerli: Boolean
            get() = metin.isNotBlank() && siklar.size >= 2 && dogru in siklar.indices
    }

    /** Bir dersin quiz geçmişi. */
    data class QuizSonuc(
        val lessonId: Long,
        val dogruSayisi: Int,
        val toplam: Int,
        val tarih: Long
    ) {
        val yuzde: Int get() = if (toplam == 0) 0 else dogruSayisi * 100 / toplam
        val gecti: Boolean get() = yuzde >= 60
    }

    /** Aralıklı tekrar kaydı — hangi ders ne zaman tekrar edilecek. */
    data class TekrarKaydi(
        val lessonId: Long,
        var kutu: Int,
        var sonrakiGun: String,
        var dogruUstUste: Int = 0,
        var toplamTekrar: Int = 0
    ) {
        val ogrenildi: Boolean get() = kutu >= ARALIKLAR.size - 1
    }

    // ═══════════════════════════════════════════════════════════════
    // SORU DEPOSU
    // ═══════════════════════════════════════════════════════════════

    fun sorulariYukle(context: Context): MutableList<Soru> {
        val ham = prefs(context).getString(K_SORULAR, "[]") ?: "[]"
        val liste = mutableListOf<Soru>()
        try {
            val dizi = JSONArray(ham)
            for (i in 0 until dizi.length()) {
                val o = dizi.optJSONObject(i) ?: continue
                val sd = o.optJSONArray("siklar") ?: continue
                val siklar = (0 until sd.length()).map { sd.optString(it) }
                liste.add(
                    Soru(
                        id = o.optLong("id"),
                        lessonId = o.optLong("lessonId"),
                        metin = o.optString("metin"),
                        siklar = siklar,
                        dogru = o.optInt("dogru", 0),
                        aciklama = o.optString("aciklama", "")
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Sorular okunamadı", e)
        }
        return liste
    }

    fun sorulariKaydet(context: Context, liste: List<Soru>) {
        try {
            val dizi = JSONArray()
            liste.forEach { s ->
                dizi.put(
                    JSONObject()
                        .put("id", s.id)
                        .put("lessonId", s.lessonId)
                        .put("metin", s.metin)
                        .put("siklar", JSONArray(s.siklar))
                        .put("dogru", s.dogru)
                        .put("aciklama", s.aciklama)
                )
            }
            prefs(context).edit().putString(K_SORULAR, dizi.toString()).apply()
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Sorular kaydedilemedi", e)
        }
    }

    fun dersinSorulari(context: Context, lessonId: Long): List<Soru> =
        sorulariYukle(context).filter { it.lessonId == lessonId && it.gecerli }

    fun soruVarMi(context: Context, lessonId: Long): Boolean =
        sorulariYukle(context).any { it.lessonId == lessonId && it.gecerli }

    /** Üretilen soruları derse ekler; aynı ders için eskiler silinir. */
    fun dersSorulariniAyarla(context: Context, lessonId: Long, yeni: List<Soru>) {
        val liste = sorulariYukle(context).filterNot { it.lessonId == lessonId }.toMutableList()
        liste.addAll(yeni.filter { it.gecerli })
        sorulariKaydet(context, liste)
    }

    /**
     * v8.0 — Soruları **havuza ekler** (öneri 8).
     *
     * ── Neden gerekli ──
     * [dersSorulariniAyarla] eski soruları siliyordu: "Soruları yenile"
     * dendiğinde havuz hep 6 soruda kalıyordu. Aynı 6 soruyu tekrar
     * çözmek ezberi ödüllendirir; kullanıcı kavramı değil şık sırasını
     * hatırlar.
     *
     * Bu fonksiyon eskiyi koruyup yenileri ekliyor. Böylece her üretimde
     * havuz büyüyor ve sınavda farklı sorular çıkıyor.
     *
     * ── Tekrar koruması ──
     * Model bazen aynı soruyu farklı kelimelerle üretiyor. Birebir aynı
     * metinler süzülüyor; yakın benzerlik için metin karşılaştırması
     * yapılmıyor — yanlış pozitif riski, tekrar sorudan daha kötü.
     *
     * @return havuza eklenen yeni soru sayısı
     */
    fun havuzaEkle(context: Context, lessonId: Long, yeni: List<Soru>): Int {
        val liste = sorulariYukle(context)
        val mevcutMetinler = liste
            .filter { it.lessonId == lessonId }
            .map { it.metin.trim().lowercase() }
            .toSet()

        val eklenecek = yeni.filter {
            it.gecerli && it.metin.trim().lowercase() !in mevcutMetinler
        }
        if (eklenecek.isEmpty()) return 0

        val guncel = liste.toMutableList()
        guncel.addAll(eklenecek)

        // Ders başına tavan: 60 soru. Aşarsa en eskiler atılır.
        val dersinkiler = guncel.filter { it.lessonId == lessonId }
        if (dersinkiler.size > 60) {
            val atilacak = dersinkiler.sortedBy { it.id }.take(dersinkiler.size - 60)
            guncel.removeAll(atilacak.toSet())
        }

        sorulariKaydet(context, guncel)
        return eklenecek.size
    }

    /** Bir dersin havuzundaki soru sayısı. */
    fun havuzSayisi(context: Context, lessonId: Long): Int =
        sorulariYukle(context).count { it.lessonId == lessonId && it.gecerli }

    /**
     * v8.0 — Havuzdan rastgele sınav çeker.
     *
     * Aynı dersi ikinci kez çözerken farklı sorular gelsin diye
     * karıştırılıyor.
     */
    fun havuzdanSinav(context: Context, lessonId: Long, adet: Int = 10): List<Soru> =
        sorulariYukle(context)
            .filter { it.lessonId == lessonId && it.gecerli }
            .shuffled()
            .take(adet)

    /** Bir bölümün/kursun tüm sorularından karışık sınav. */
    fun karisikSinav(context: Context, lessonIds: List<Long>, adet: Int = 20): List<Soru> =
        sorulariYukle(context)
            .filter { it.lessonId in lessonIds && it.gecerli }
            .shuffled()
            .take(adet)

    // ═══════════════════════════════════════════════════════════════
    // QUIZ SONUÇLARI
    // ═══════════════════════════════════════════════════════════════

    fun sonuclariYukle(context: Context): MutableList<QuizSonuc> {
        val ham = prefs(context).getString(K_SONUC, "[]") ?: "[]"
        val liste = mutableListOf<QuizSonuc>()
        try {
            val dizi = JSONArray(ham)
            for (i in 0 until dizi.length()) {
                val o = dizi.optJSONObject(i) ?: continue
                liste.add(
                    QuizSonuc(
                        lessonId = o.optLong("lessonId"),
                        dogruSayisi = o.optInt("dogru"),
                        toplam = o.optInt("toplam"),
                        tarih = o.optLong("tarih")
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Sonuçlar okunamadı", e)
        }
        return liste
    }

    fun sonucKaydet(context: Context, sonuc: QuizSonuc) {
        try {
            val liste = sonuclariYukle(context)
            liste.add(sonuc)
            // Son 500 sonuç yeter — dosya şişmesin
            val kirp = if (liste.size > 500) liste.takeLast(500) else liste
            val dizi = JSONArray()
            kirp.forEach { s ->
                dizi.put(
                    JSONObject()
                        .put("lessonId", s.lessonId)
                        .put("dogru", s.dogruSayisi)
                        .put("toplam", s.toplam)
                        .put("tarih", s.tarih)
                )
            }
            prefs(context).edit().putString(K_SONUC, dizi.toString()).apply()
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Sonuç kaydedilemedi", e)
        }
    }

    /** Dersin en iyi quiz sonucu. */
    fun enIyiSonuc(context: Context, lessonId: Long): QuizSonuc? =
        sonuclariYukle(context).filter { it.lessonId == lessonId }.maxByOrNull { it.yuzde }

    // ═══════════════════════════════════════════════════════════════
    // ARALIKLI TEKRAR (Leitner)
    // ═══════════════════════════════════════════════════════════════

    fun tekrarlariYukle(context: Context): MutableList<TekrarKaydi> {
        val ham = prefs(context).getString(K_TEKRAR, "[]") ?: "[]"
        val liste = mutableListOf<TekrarKaydi>()
        try {
            val dizi = JSONArray(ham)
            for (i in 0 until dizi.length()) {
                val o = dizi.optJSONObject(i) ?: continue
                liste.add(
                    TekrarKaydi(
                        lessonId = o.optLong("lessonId"),
                        kutu = o.optInt("kutu", 0),
                        sonrakiGun = o.optString("sonraki", bugun()),
                        dogruUstUste = o.optInt("seri", 0),
                        toplamTekrar = o.optInt("toplam", 0)
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Tekrarlar okunamadı", e)
        }
        return liste
    }

    fun tekrarlariKaydet(context: Context, liste: List<TekrarKaydi>) {
        try {
            val dizi = JSONArray()
            liste.forEach { t ->
                dizi.put(
                    JSONObject()
                        .put("lessonId", t.lessonId)
                        .put("kutu", t.kutu)
                        .put("sonraki", t.sonrakiGun)
                        .put("seri", t.dogruUstUste)
                        .put("toplam", t.toplamTekrar)
                )
            }
            prefs(context).edit().putString(K_TEKRAR, dizi.toString()).apply()
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Tekrarlar kaydedilemedi", e)
        }
    }

    /**
     * Ders tamamlandığında tekrar programına alır.
     * Zaten programdaysa dokunmaz.
     */
    fun tekraraAl(context: Context, lessonId: Long) {
        val liste = tekrarlariYukle(context)
        if (liste.any { it.lessonId == lessonId }) return
        liste.add(
            TekrarKaydi(
                lessonId = lessonId,
                kutu = 0,
                sonrakiGun = gunEkle(ARALIKLAR[0])
            )
        )
        tekrarlariKaydet(context, liste)
    }

    /**
     * Tekrar sonucunu işler ve bir sonraki tarihi hesaplar.
     * @param basarili quiz'i geçtiyse true
     */
    fun tekrarSonucu(context: Context, lessonId: Long, basarili: Boolean) {
        val liste = tekrarlariYukle(context)
        val kayit = liste.firstOrNull { it.lessonId == lessonId }
            ?: TekrarKaydi(lessonId, 0, bugun()).also { liste.add(it) }

        kayit.toplamTekrar++
        if (basarili) {
            kayit.kutu = (kayit.kutu + 1).coerceAtMost(ARALIKLAR.size - 1)
            kayit.dogruUstUste++
        } else {
            // Yanlışta başa dön — yakında tekrar sorulsun
            kayit.kutu = 0
            kayit.dogruUstUste = 0
        }
        kayit.sonrakiGun = gunEkle(ARALIKLAR[kayit.kutu])
        tekrarlariKaydet(context, liste)
    }

    /** Bugün (ve geçmişte kalan) tekrar edilmesi gereken ders kimlikleri. */
    fun bugunTekrarEdilecekler(context: Context): List<Long> {
        val b = bugun()
        return tekrarlariYukle(context)
            .filter { !it.ogrenildi && it.sonrakiGun <= b }
            .sortedBy { it.sonrakiGun }
            .map { it.lessonId }
    }

    fun tekrarSayisi(context: Context): Int = bugunTekrarEdilecekler(context).size

    /** Öğrenildi sayılan (kutu 5) ders sayısı. */
    fun ogrenilenSayisi(context: Context): Int =
        tekrarlariYukle(context).count { it.ogrenildi }

    /** Bir dersin tekrar durumu — kart üzerinde göstermek için. */
    fun tekrarDurumu(context: Context, lessonId: Long): TekrarKaydi? =
        tekrarlariYukle(context).firstOrNull { it.lessonId == lessonId }

    /** Kutu numarasından okunabilir aralık metni. */
    fun aralikMetni(kutu: Int): Int = ARALIKLAR.getOrElse(kutu) { 1 }

    // ═══════════════════════════════════════════════════════════════
    // YEDEKLEME
    // ═══════════════════════════════════════════════════════════════

    /** v7.29: Quiz verisi yedeğe dahil edilsin diye dışa aktarım. */
    fun disaAktar(context: Context): JSONObject = JSONObject()
        .put("sorular", prefs(context).getString(K_SORULAR, "[]"))
        .put("tekrar", prefs(context).getString(K_TEKRAR, "[]"))
        .put("sonuclar", prefs(context).getString(K_SONUC, "[]"))

    fun iceAktar(context: Context, o: JSONObject) {
        try {
            val e = prefs(context).edit()
            if (o.has("sorular")) e.putString(K_SORULAR, o.optString("sorular", "[]"))
            if (o.has("tekrar")) e.putString(K_TEKRAR, o.optString("tekrar", "[]"))
            if (o.has("sonuclar")) e.putString(K_SONUC, o.optString("sonuclar", "[]"))
            e.apply()
        } catch (ex: Exception) {
            android.util.Log.w(TAG, "Quiz verisi geri yüklenemedi", ex)
        }
    }
}
