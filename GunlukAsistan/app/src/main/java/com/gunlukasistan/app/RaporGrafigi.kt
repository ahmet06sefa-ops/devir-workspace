package com.gunlukasistan.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import java.util.Calendar

/**
 * v10.3 · Öneri B16 — Haftalık rapora grafikli kart.
 *
 * ── Dürüst not ──
 * Haftalık rapor bildirimi zaten vardı (`WeeklyReportReceiver`,
 * her pazar 20:00, metin tabanlı). Bu sınıf metnin yanına son 7
 * günün odak dakikalarını çubuk grafik olarak çizen **bitmap
 * üretir** (`BigPictureStyle`).
 *
 * ── Veri ──
 * Kaynak `SureAnalizi.pomodorolar`: tamamlanmış odak oturumlarının
 * dakikası. Tahmin/gerçek kayıtları (Kayit) süre analizi içindir;
 * grafik "fiilen odaklanılan dakika"yı göstermeli, bu yüzden
 * pomodoro/odak kayıtları kullanılır ve yalnızca `tamamlandi`
 * olanlar sayılır — yarım kalan oturum "başarı grafiği"ni
 * şişirmemeli.
 *
 * ── Saf bölge ──
 * [gunlukOdakDakikalari] `simdi` parametresi aldığı için birim
 * testinde deterministik çalışır; cihazda varsayılan değer
 * kullanılır. Bitmap üretimi testlerde çağrılmaz.
 */
object RaporGrafigi {

    /** Hafta kısaltmaları — grafik altı gün etiketleri. */
    private val GUNLER = arrayOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")

    private const val GUN_MS = 86_400_000L

    /** `simdi`'nin ait olduğu günün yerel gece yarısı. */
    fun gunBasi(simdi: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = simdi
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * Son 7 günün tamamlanmış odak dakikaları.
     *
     * Dönüş dizisinin `6`. indeksi bugün, `0`. indeksi 6 gün önce.
     * Gelecek damgalı kayıtlar (saat kayması vb.) bugüne yazılmaz,
     * yok sayılır — grafiğin tepesi asla bozuk veriyle patlamaz.
     */
    fun gunlukOdakDakikalari(
        kayitlar: List<SureAnalizi.PomodoroKayit>,
        simdi: Long = System.currentTimeMillis()
    ): IntArray {
        val bugunBasi = gunBasi(simdi)
        val ilkBasi = bugunBasi - 6 * GUN_MS
        val sonuc = IntArray(7)
        for (k in kayitlar) {
            if (!k.tamamlandi || k.sureDk <= 0) continue
            // Pencere dışı ve gelecek damgalar elenir: saati ileri
            // alınmış cihaz kaydı grafiğin bugün sütununu şişirmesin.
            if (k.zaman < ilkBasi || k.zaman > simdi) continue
            val indeks = ((k.zaman - ilkBasi) / GUN_MS).toInt().coerceIn(0, 6)
            sonuc[indeks] += k.sureDk
        }
        return sonuc
    }

    /** Grafiğin ölçek dayanağı — sıfır haftada bölme hatası olmasın. */
    fun olcekUstu(dakikalar: IntArray): Int =
        (dakikalar.maxOrNull() ?: 0).coerceAtLeast(1)

    /**
     * Açık zeminli çubuk kart üretir (640×360).
     *
     * Boyut bilinçli tutuldu: bildirim parcel'i binder üzerinden
     * taşınır ve uygulamalar arası bellek paylaşılmaz; ~1 MB üzeri
     * bitmapler kimi cihazda TransactionTooLarge ile düşürülüyor.
     * 640×360×4 bayt ≈ 0,9 MB güvenli sınırın altında kalır ve
     * genişletilmiş panelde (≈400 dp) hâlâ net görünür.
     *
     * Açık zemin bilinçli: bildirim paneli koyu tema kullanıyorsa bile
     * büyük resim alanı zaten "ekran görüntüsü" gibi davranıyor; açık
     * kart her iki temada da okunuyor.
     */
    fun olustur(dakikalar: IntArray): Bitmap {
        val g = 640
        val y = 360
        val bmp = Bitmap.createBitmap(g, y, Bitmap.Config.ARGB_8888)
        val tuval = Canvas(bmp)

        // Zemin
        tuval.drawColor(Color.WHITE)

        val kenar = 30f
        val ustBosluk = 40f
        val etiketYukseklik = 36f
        val grafikAlt = y - kenar - etiketYukseklik
        val grafikUst = ustBosluk + 30f
        val tuz = Paint(Paint.ANTI_ALIAS_FLAG)

        // Başlık
        tuz.color = Color.rgb(0x33, 0x30, 0x3A)
        tuz.textSize = 25f
        tuz.typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD
        )
        tuz.textAlign = Paint.Align.LEFT
        val toplam = dakikalar.sum()
        tuval.drawText("Son 7 gün · toplam $toplam dk odak", kenar, ustBosluk, tuz)

        val ust = olcekUstu(dakikalar)
        val alanGenislik = g - 2 * kenar
        val cizgiGenislik = alanGenislik / 7
        val barGenislik = cizgiGenislik * 0.62f
        val bugunIndeks = 6

        val barBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(0x67, 0x50, 0xA4) }
        val barBugun = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(0xB3, 0x3C, 0x86) }
        val siri = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0xE6, 0xE2, 0xEE)
        }
        val etiket = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0x55, 0x52, 0x60)
            textSize = 19f
            textAlign = Paint.Align.CENTER
        }
        val degerYazi = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0x40, 0x3A, 0x52)
            textSize = 17f
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD
            )
        }

        // Bugün hangi haftanın günü? (Pzt=0 … Paz=6)
        val cal = Calendar.getInstance()
        val bugunGun = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7

        for (i in 0..6) {
            val merkezX = kenar + cizgiGenislik * i + cizgiGenislik / 2f
            val sol = merkezX - barGenislik / 2f
            val sag = merkezX + barGenislik / 2f

            // İz çizgi (boş günlerde bile sütun ritmi görünsün)
            tuval.drawRoundRect(RectF(sol, grafikUst, sag, grafikAlt), 6f, 6f, siri)

            val dk = dakikalar[i]
            if (dk > 0) {
                val yukseklik = (grafikAlt - grafikUst) * dk / ust
                val barAlt = grafikAlt
                val barUst = grafikAlt - yukseklik
                tuval.drawRoundRect(
                    RectF(sol, barUst, sag, barAlt), 7f, 7f,
                    if (i == bugunIndeks) barBugun else barBoya
                )
                tuval.drawText("$dk", merkezX, barUst - 8f, degerYazi)
            }

            // Gün etiketi: dizideki 6. indeks bugün; geriye doğru geri sar
            val gunIndeks = (bugunGun - (6 - i) + 700) % 7
            etiket.color = if (i == bugunIndeks) {
                Color.rgb(0xB3, 0x3C, 0x86)
            } else {
                Color.rgb(0x55, 0x52, 0x60)
            }
            tuval.drawText(GUNLER[gunIndeks], merkezX, y - kenar + 4f, etiket)
        }
        return bmp
    }
}
