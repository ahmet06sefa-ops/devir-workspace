package com.gunlukasistan.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

/**
 * v10.15 · ULTRA-30 / C17 — Haftalık rapora ISI HARİTASI kartı.
 *
 * ── Tarama düzeltmesi (dürüstlük) ──
 * Öneri "Raporlar metin; görsel özet yok" diyordu — bu YANLIŞTI:
 * v10.3'ten beri `RaporGrafigi` ile haftalık bildirime BigPictureStyle
 * çubuk grafik ekleniyor. Gerçek boşluk: odak dakikalarının gün ×
 * saat-dilimi dağılımını gösteren ISI HARİTASI yoktu — "sabah mı akşam
 * mı çalışıyorum, hangi günler kayıyor" sorusunun görseli. Bu kart
 * mevcut çubuğun ALTINA ikinci bölüm olarak birleştirilir; eski kart
 * ve testleri aynen korunur.
 *
 * ── Veri ──
 * `RaporGrafigi` ile aynı kaynak: tamamlanmış odak oturumları
 * (`SureAnalizi.PomodoroKayit.tamamlandi`); yarım oturumlar ısıyı
 * şişirmemeli.
 *
 * ── Izgara ──
 * 7 gün (Pzt→Paz) × 6 dilim (4 saat). Hücre kademesi 0–4″▲, haftanın
 * en yoğun hücresine göre ölçeklenir (göreli ısı — az çalışan haftada
 * da okunaklı kalır).
 *
 * Saf bölge ([gunSaatKademeleri], [hucreKademe]) birim testlidir;
 * bitmap üretimi testlerde çağrılmaz.
 */
object RaporIsi {

    /** Gün başına 4 saatlik dilim sayısı. */
    const val DILIM: Int = 6

    private const val GUN_MS = 86_400_000L
    private val GUNLER = arrayOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")

    /**
     * 7×6 dakika matrisi. Satır 0 = Pazartesi … satır 6 = Pazar.
     * `simdi`'nin haftası (Pazartesi başlangıç) dikkate alınır; gelecek
     * hücreler 0 kalır (rapor pazar akşamı üretildiğinde doludur).
     * Testlerde determinizm için [simdi] parametrile beslenir.
     */
    fun gunSaatMatrisi(
        kayitlar: List<SureAnalizi.PomodoroKayit>,
        simdi: Long = System.currentTimeMillis(),
    ): Array<IntArray> {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = simdi }
        // Pazartesi başlangıçlı haftanın ilk günü 00:00
        val gunOfset = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7 // Pzt=0..Paz=6
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0); cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0); cal.set(java.util.Calendar.MILLISECOND, 0)
        val pztBasi = cal.timeInMillis - gunOfset * GUN_MS

        val m = Array(7) { IntArray(DILIM) }
        for (k in kayitlar) {
            if (!k.tamamlandi) continue
            if (k.zaman < pztBasi || k.zaman >= pztBasi + 7 * GUN_MS) continue
            val gun = ((k.zaman - pztBasi) / GUN_MS).toInt().coerceIn(0, 6)
            val saat = ((k.zaman - pztBasi - gun * GUN_MS) / 3_600_000L).toInt()
                .coerceIn(0, 23) // saat alanı yerine zaman'dan türet — kayıt anı yerel
            m[gun][(saat / 4).coerceIn(0, DILIM - 1)] += k.sureDk
        }
        return m
    }

    /**
     * Hücreyi 0-4 kademeye çevirir. Ölçek haftanın maksimumuna göre:
     * 0 dk → 0; maks→4; arası eşit üç bölme (1-3). maks<=0 → hep 0.
     */
    fun hucreKademe(dk: Int, maksDk: Int): Int = when {
        maksDk <= 0 || dk <= 0 -> 0
        dk >= maksDk -> 4
        else -> 1 + (dk * 3 - 1) / maksDk // 1..3 dağılımı
    }

    fun matrisMaks(m: Array<IntArray>): Int = m.maxOf { it.max() }

    /** Kademe → dolgu rengi (koyu zemine uygun, 5 basamak). */
    private fun kademeRenk(k: Int): Int = when (k) {
        0 -> 0xFF2A2622.toInt(); 1 -> 0xFF3D4A3A.toInt(); 2 -> 0xFF4F7A52.toInt()
        3 -> 0xFF6FB86F.toInt(); else -> 0xFF9BE89B.toInt()
    }

    /**
     * Isı haritası bitmap'i (mevcut çubuk kartla aynı genişlik: 1056),
     * altına birleştirmek üzere tasarlandı. <256KB PNG disiplini:
     * yalnız 6 renk + slogansız düz alanlar; PNG sıkışması mükemmeldir.
     */
    fun olustur(matris: Array<IntArray>, maksDk: Int): Bitmap {
        val w = 1056; val h = 560
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(0xFF1C1814.toInt())
        val boya = Paint(Paint.ANTI_ALIAS_FLAG)
        val yazi = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFD9CFC2.toInt(); textSize = 30f }

        boya.color = 0xFF8F8578.toInt(); boya.textSize = 34f
        c.drawText("Odak ısısı · gün × saat", 28f, 52f, boya)

        val ust = 92f
        val sol = 120f
        val hucreW = (w - sol - 24f) / DILIM
        val hucreH = (h - ust - 24f) / 7f

        // Dilim başlıkları (0-4, 4-8, … 20-24)
        yazi.textSize = 24f; yazi.color = 0xFF8F8578.toInt()
        for (d in 0 until DILIM) {
            c.drawText("%02d–%02d".format(d * 4, d * 4 + 4), sol + d * hucreW + 8f, ust - 12f, yazi)
        }
        val r = RectF()
        for (g in 0..6) {
            val y = ust + g * hucreH
            yazi.textSize = 26f
            c.drawText(GUNLER[g], 28f, y + hucreH * 0.62f, yazi)
            for (d in 0 until DILIM) {
                val x = sol + d * hucreW
                r.set(x + 4f, y + 4f, x + hucreW - 4f, y + hucreH - 4f)
                boya.color = kademeRenk(hucreKademe(matris[g][d], maksDk))
                c.drawRoundRect(r, 10f, 10f, boya)
                if (matris[g][d] > 0) {
                    yazi.color = Color.WHITE; yazi.textSize = 24f
                    c.drawText("${matris[g][d]}d", x + 14f, y + hucreH * 0.62f, yazi)
                    yazi.color = 0xFF8F8578.toInt()
                }
            }
        }
        return bmp
    }

    /** İki bitmapi dikey birleştirir (üst: mevcut çubuk kart, alt: ısı). */
    fun birlestir(ust: Bitmap, alt: Bitmap): Bitmap {
        val w = maxOf(ust.width, alt.width)
        val bmp = Bitmap.createBitmap(w, ust.height + alt.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(0xFF1C1814.toInt())
        c.drawBitmap(ust, ((w - ust.width) / 2).toFloat(), 0f, null)
        c.drawBitmap(alt, ((w - alt.width) / 2).toFloat(), ust.height.toFloat(), null)
        return bmp
    }
}
