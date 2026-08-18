package com.gunlukasistan.app

import android.content.Context
import java.util.Calendar

/**
 * v7.70 — Tekrarlayan görevler.
 *
 * ── Kullanıcının isteği ──
 * "Her salı çöp", "her ayın 1'i kira", "her 3 ayda bir filtre" gibi
 * işleri bir kere kurup kendi kendine yenilenmesini sağlamak.
 *
 * ── Tasarım kararı: görev silinmez, taşınır ──
 * Tamamlanan tekrarlı görev listeden kaybolmaz; `dueAt` bir sonraki
 * tarihe alınır ve `done` tekrar false yapılır. Böylece:
 *   · Alarm zinciri kopmaz (yeni tarihe yeniden kurulur)
 *   · Görev kimliği sabit kalır — widget/bildirim referansları bozulmaz
 *   · Kullanıcı "kaç kez yaptım" bilgisini kaybetmez ([yapildi] sayacı)
 *
 * ── Kodlama biçimi ──
 * `Store.Task` içine iki alan eklendi:
 *   · `tekrar`: "yok" | "gun" | "hafta" | "2hafta" | "ay" | "3ay" |
 *               "6ay" | "yil" | "gunler:1,3,5" | "ozel:10"
 *   · `tekrarBitis`: 0 = süresiz, aksi hâlde son tarih (ms)
 *
 * Metin tabanlı saklama seçildi çünkü JSON'a tek alan olarak sığıyor ve
 * eski yedeklerle uyumlu (alan yoksa "yok" varsayılır).
 */
object Tekrar {

    private const val TAG = "Tekrar"

    const val YOK = "yok"
    const val GUN = "gun"
    const val HAFTA = "hafta"
    const val IKI_HAFTA = "2hafta"
    const val AY = "ay"
    const val UC_AY = "3ay"
    const val ALTI_AY = "6ay"
    const val YIL = "yil"

    /** "gunler:1,3,5" — 1=Pazartesi … 7=Pazar */
    const val ON_GUNLER = "gunler:"

    /** "ozel:10" — her 10 günde bir */
    const val ON_OZEL = "ozel:"

    /** Tekrar tanımlı mı? */
    fun aktifMi(kod: String?): Boolean =
        !kod.isNullOrBlank() && kod != YOK

    // ═══════════════════════════════════════════════════════════════
    // OKUNABİLİR AD
    // ═══════════════════════════════════════════════════════════════

    /** Kullanıcıya gösterilecek kısa ad ("Her salı", "3 ayda bir"…). */
    fun ad(context: Context, kod: String?): String {
        if (!aktifMi(kod)) return context.getString(R.string.tk_yok)
        val k = kod!!
        return try {
            when {
                k == GUN -> context.getString(R.string.tk_gunluk)
                k == HAFTA -> context.getString(R.string.tk_haftalik)
                k == IKI_HAFTA -> context.getString(R.string.tk_2hafta)
                k == AY -> context.getString(R.string.tk_aylik)
                k == UC_AY -> context.getString(R.string.tk_3ay)
                k == ALTI_AY -> context.getString(R.string.tk_6ay)
                k == YIL -> context.getString(R.string.tk_yillik)
                k.startsWith(ON_OZEL) -> context.getString(
                    R.string.tk_ozel, k.removePrefix(ON_OZEL).toIntOrNull() ?: 1
                )
                k.startsWith(ON_GUNLER) -> gunlerAdi(context, k)
                else -> context.getString(R.string.tk_yok)
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Ad üretilemedi", e)
            context.getString(R.string.tk_yok)
        }
    }

    /** "Pzt · Çar · Cum" biçiminde gün listesi. */
    private fun gunlerAdi(context: Context, kod: String): String {
        val gunler = gunleriCoz(kod)
        if (gunler.isEmpty()) return context.getString(R.string.tk_yok)
        val adlar = listOf(
            R.string.tk_g_pzt, R.string.tk_g_sal, R.string.tk_g_car,
            R.string.tk_g_per, R.string.tk_g_cum, R.string.tk_g_cmt,
            R.string.tk_g_paz
        )
        return gunler.sorted().joinToString(" · ") { context.getString(adlar[it - 1]) }
    }

    /** "gunler:1,3,5" → [1,3,5] */
    fun gunleriCoz(kod: String?): List<Int> {
        if (kod == null || !kod.startsWith(ON_GUNLER)) return emptyList()
        return kod.removePrefix(ON_GUNLER)
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..7 }
            .distinct()
    }

    fun gunleriKodla(gunler: List<Int>): String =
        ON_GUNLER + gunler.filter { it in 1..7 }.distinct().sorted().joinToString(",")

    // ═══════════════════════════════════════════════════════════════
    // SONRAKİ TARİH HESABI
    // ═══════════════════════════════════════════════════════════════

    /**
     * Bir sonraki tekrar tarihini hesaplar.
     *
     * @param oncekiMs baz alınacak tarih (görevin mevcut `dueAt`i).
     *        0 ise şu an baz alınır.
     * @return yeni tarih (ms) veya 0 (tekrar yok / süresi doldu)
     *
     * Kritik nokta: hesaplanan tarih **geçmişte kalmamalı**. Kullanıcı
     * günlük görevi 3 gün açmadıysa, tarih 3 gün geriden gelmemeli —
     * bugüne/geleceğe kadar ileri sarılır.
     */
    fun sonraki(kod: String?, oncekiMs: Long, bitisMs: Long = 0L): Long {
        if (!aktifMi(kod)) return 0L
        return try {
            val simdi = System.currentTimeMillis()
            val cal = Calendar.getInstance().apply {
                timeInMillis = if (oncekiMs > 0) oncekiMs else simdi
            }

            // Gün-bazlı tekrar (ör. her salı) ayrı ele alınır
            if (kod!!.startsWith(ON_GUNLER)) {
                val sonuc = sonrakiHaftaGunu(cal, gunleriCoz(kod), simdi)
                return if (bitisMs > 0 && sonuc > bitisMs) 0L else sonuc
            }

            // Diğerlerinde sabit aralık eklenir, geçmişte kalırsa ileri sarılır
            var guvenlik = 0
            do {
                ilerlet(cal, kod)
                guvenlik++
            } while (cal.timeInMillis <= simdi && guvenlik < 400)

            val sonuc = cal.timeInMillis
            if (bitisMs > 0 && sonuc > bitisMs) 0L else sonuc
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Sonraki tarih hesaplanamadı", e)
            0L
        }
    }

    /** Takvimi tekrar koduna göre bir adım ileri alır. */
    private fun ilerlet(cal: Calendar, kod: String) {
        when {
            kod == GUN -> cal.add(Calendar.DAY_OF_YEAR, 1)
            kod == HAFTA -> cal.add(Calendar.DAY_OF_YEAR, 7)
            kod == IKI_HAFTA -> cal.add(Calendar.DAY_OF_YEAR, 14)
            kod == AY -> cal.add(Calendar.MONTH, 1)
            kod == UC_AY -> cal.add(Calendar.MONTH, 3)
            kod == ALTI_AY -> cal.add(Calendar.MONTH, 6)
            kod == YIL -> cal.add(Calendar.YEAR, 1)
            kod.startsWith(ON_OZEL) -> {
                val n = kod.removePrefix(ON_OZEL).toIntOrNull()?.coerceIn(1, 365) ?: 1
                cal.add(Calendar.DAY_OF_YEAR, n)
            }
            else -> cal.add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    /**
     * Seçili haftanın günlerinden bir sonrakini bulur.
     * Saat bilgisi korunur (ör. her salı 09:00).
     */
    private fun sonrakiHaftaGunu(baz: Calendar, gunler: List<Int>, simdi: Long): Long {
        if (gunler.isEmpty()) return 0L
        val cal = Calendar.getInstance().apply { timeInMillis = baz.timeInMillis }
        // En fazla 8 gün ileri bak — mutlaka bir eşleşme çıkar
        for (i in 1..8) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            if (gunler.contains(pazartesiBazli(cal)) && cal.timeInMillis > simdi) {
                return cal.timeInMillis
            }
        }
        return 0L
    }

    /** Calendar.DAY_OF_WEEK → 1=Pazartesi … 7=Pazar */
    private fun pazartesiBazli(cal: Calendar): Int =
        when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            else -> 7
        }

    // ═══════════════════════════════════════════════════════════════
    // GÖREV YENİLEME
    // ═══════════════════════════════════════════════════════════════

    /**
     * Tamamlanan tekrarlı görevi bir sonraki tarihe taşır.
     *
     * Üç yerden çağrılır: Görevler ekranı, bildirim "Tamam" düğmesi,
     * görev widget'ı. Bu yüzden mantık burada toplandı.
     *
     * @return yeni tarih (ms) · 0 ise tekrar bitti, görev normal tamamlandı
     */
    fun gorevYenile(context: Context, gorev: Store.Task): Long {
        if (!aktifMi(gorev.tekrar)) return 0L
        return try {
            val yeni = sonraki(gorev.tekrar, gorev.dueAt, gorev.tekrarBitis)
            if (yeni <= 0L) {
                // Süre doldu: tekrarı kapat, görev tamamlanmış kalsın
                gorev.tekrar = YOK
                return 0L
            }
            gorev.done = false
            // v10.15 · C16: öğrenen hatırlatıcı — son "yapıldı" saatlerinden
            // kelepçeli (±45 dk) kaydırım; veri yetersiz/dağınıksa aynen yeni döner.
            val kaydirilmis = OgrenenHatirlatici.uygula(context, gorev.id, yeni)
            gorev.dueAt = kaydirilmis
            gorev.yapildi += 1
            // Alarmı yeni tarihe kur
            try {
                AlarmScheduler.schedule(context, gorev.id, gorev.text, kaydirilmis)
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Alarm kurulamadı", e)
            }
            kaydirilmis
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Görev yenilenemedi", e)
            0L
        }
    }

    /** "12 Ağu Sal" biçiminde kısa tarih. */
    fun tarihMetni(ms: Long): String = try {
        if (ms <= 0) "" else java.text.SimpleDateFormat(
            "d MMM EEE", java.util.Locale("tr", "TR")
        ).format(java.util.Date(ms))
    } catch (e: Exception) {
        android.util.Log.w(TAG, "Tarih biçimlenemedi", e)
        ""
    }
}
