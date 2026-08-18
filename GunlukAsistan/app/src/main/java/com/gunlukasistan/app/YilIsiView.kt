package com.gunlukasistan.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.util.Calendar
import java.util.Locale

/**
 * v8.4 — Yıllık ısı haritası (öneri 17).
 *
 * ── Sorun ──
 * İlerleme ekranında aylık ızgara vardı (`ProgressFragment`): 30 kare,
 * ay ay geziliyordu. Bir yıllık emeği görmek için 12 kez ok tuşuna
 * basmak gerekiyordu ve hiçbir zaman bütünü görülemiyordu.
 *
 * ── Neden GitHub düzeni ──
 * 53 sütun (hafta) × 7 satır (gün) düzeni 365 günü tek ekrana
 * sığdırıyor. Dikey eksen haftanın günü olduğu için "hafta sonları
 * boş" gibi desenler gözle görülüyor — aylık ızgarada bu kaybolur.
 *
 * ── Neden custom View ──
 * 371 hücreyi ayrı `View` olarak eklemek RecyclerView'sız 371 nesne
 * demek; kaydırma kekelerdi. Tek `onDraw` içinde 371 `drawRoundRect`
 * ölçülebilir şekilde hızlı (birkaç ms).
 *
 * ── Dokunma ──
 * Kareye dokununca [gunSecildi] tetikleniyor; fragment o günün
 * özetini gösteriyor. Dokunulan kare çerçeveleniyor.
 */
class YilIsiView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    /** gunAnahtari (yyyyMMdd) → puan */
    private var puanlar: Map<String, Int> = emptyMap()

    /** Izgaranın ilk günü (en sol sütunun pazartesisi). */
    private var baslangic: Calendar = Calendar.getInstance()

    /** Kaç hafta çizilecek. */
    private var haftaSayisi = 53

    private val hucreBoya = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cerceve = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val yaziBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.LEFT
    }
    private val dikdortgen = RectF()

    private var seciliAnahtar: String? = null

    /** Isı renkleri: 0 = boş, 4 = en yoğun. */
    var renkler: IntArray = intArrayOf(
        0xFFECEFE6.toInt(), 0xFFC4DCB9.toInt(), 0xFF97C98A.toInt(),
        0xFF64AE5B.toInt(), 0xFF3F8A3A.toInt()
    )
        set(v) { field = v; invalidate() }

    /** Yazı rengi (ay etiketleri, gün harfleri). */
    var etiketRengi: Int = 0xFF8A8175.toInt()
        set(v) { field = v; invalidate() }

    /** Bir güne dokunulduğunda: (anahtar, puan). */
    var gunSecildi: ((String, Int) -> Unit)? = null

    private val turkce = Locale("tr", "TR")
    private val aylar = arrayOf("Oca", "Şub", "Mar", "Nis", "May", "Haz",
        "Tem", "Ağu", "Eyl", "Eki", "Kas", "Ara")
    private val gunHarfleri = arrayOf("P", "S", "Ç", "P", "C", "C", "P")

    // ------------------------------------------------------------------

    /**
     * Veriyi ayarlar.
     *
     * @param puanlar yyyyMMdd → günlük puan
     * @param bitis son gün (genellikle bugün); ızgara buradan geriye 52 hafta
     */
    fun ayarla(puanlar: Map<String, Int>, bitis: Calendar = Calendar.getInstance()) {
        this.puanlar = puanlar

        // Bitiş gününün haftasının pazartesisini bul, oradan 52 hafta geri git
        val son = (bitis.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // Pazartesi = 1 olacak şekilde kaydır (Calendar'da Pazar = 1)
        val haftaninGunu = (son.get(Calendar.DAY_OF_WEEK) + 5) % 7
        son.add(Calendar.DAY_OF_YEAR, -haftaninGunu)   // bu haftanın pazartesisi
        son.add(Calendar.DAY_OF_YEAR, -7 * (haftaSayisi - 1))
        baslangic = son

        requestLayout()
        invalidate()
    }

    // ------------------------------------------------------------------

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val yg = resources.displayMetrics.density

        val solPay = yg * 16          // gün harfleri
        val ustPay = yg * 14          // ay etiketleri
        val bosluk = yg * 2.2f

        // Hücre boyutu genişliğe göre; en fazla 14dp
        val kullanilabilir = w - solPay - yg * 4
        val hucre = ((kullanilabilir - bosluk * (haftaSayisi - 1)) / haftaSayisi)
            .coerceIn(yg * 5f, yg * 14f)

        val h = (ustPay + 7 * hucre + 6 * bosluk + yg * 6).toInt()
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val yg = resources.displayMetrics.density
        val solPay = yg * 16
        val ustPay = yg * 14
        val bosluk = yg * 2.2f

        val kullanilabilir = width - solPay - yg * 4
        val hucre = ((kullanilabilir - bosluk * (haftaSayisi - 1)) / haftaSayisi)
            .coerceIn(yg * 5f, yg * 14f)
        val kose = hucre * 0.25f

        yaziBoya.color = etiketRengi
        yaziBoya.textSize = yg * 8.5f

        // ---- Gün harfleri (sol sütun) — bir atlayarak, sığmıyor ----
        for (g in 0 until 7 step 2) {
            val y = ustPay + g * (hucre + bosluk) + hucre * 0.72f
            canvas.drawText(gunHarfleri[g], 0f, y, yaziBoya)
        }

        // ---- Hücreler ----
        val imlec = baslangic.clone() as Calendar
        var oncekiAy = -1

        for (hafta in 0 until haftaSayisi) {
            for (gun in 0 until 7) {
                val x = solPay + hafta * (hucre + bosluk)
                val y = ustPay + gun * (hucre + bosluk)

                val anahtar = anahtarla(imlec)
                val puan = puanlar[anahtar] ?: 0
                val gelecek = imlec.timeInMillis > System.currentTimeMillis()

                hucreBoya.color = if (gelecek) {
                    // Gelecek günler daha soluk — henüz yaşanmadı
                    Color.argb(60, Color.red(renkler[0]), Color.green(renkler[0]), Color.blue(renkler[0]))
                } else {
                    renkler[seviye(puan)]
                }

                dikdortgen.set(x, y, x + hucre, y + hucre)
                canvas.drawRoundRect(dikdortgen, kose, kose, hucreBoya)

                // Seçili kare çerçevesi
                if (anahtar == seciliAnahtar) {
                    cerceve.color = etiketRengi
                    cerceve.strokeWidth = yg * 1.6f
                    canvas.drawRoundRect(dikdortgen, kose, kose, cerceve)
                }

                // Ay etiketi: ayın ilk haftasının üstüne
                if (gun == 0) {
                    val ay = imlec.get(Calendar.MONTH)
                    if (ay != oncekiAy) {
                        // Ayın 1-7'si bu sütuna denk geliyorsa etiket yaz
                        if (imlec.get(Calendar.DAY_OF_MONTH) <= 7) {
                            canvas.drawText(aylar[ay], x, ustPay - yg * 4, yaziBoya)
                            oncekiAy = ay
                        }
                    }
                }

                imlec.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
    }

    // ------------------------------------------------------------------

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) return super.onTouchEvent(event)

        val yg = resources.displayMetrics.density
        val solPay = yg * 16
        val ustPay = yg * 14
        val bosluk = yg * 2.2f
        val kullanilabilir = width - solPay - yg * 4
        val hucre = ((kullanilabilir - bosluk * (haftaSayisi - 1)) / haftaSayisi)
            .coerceIn(yg * 5f, yg * 14f)

        val hafta = ((event.x - solPay) / (hucre + bosluk)).toInt()
        val gun = ((event.y - ustPay) / (hucre + bosluk)).toInt()
        if (hafta !in 0 until haftaSayisi || gun !in 0..6) return false

        val hedef = (baslangic.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, hafta * 7 + gun)
        }
        if (hedef.timeInMillis > System.currentTimeMillis()) return false

        val anahtar = anahtarla(hedef)
        seciliAnahtar = anahtar
        invalidate()
        Titresim.tik(this)
        gunSecildi?.invoke(anahtar, puanlar[anahtar] ?: 0)
        return true
    }

    // ------------------------------------------------------------------

    private fun seviye(puan: Int): Int = when {
        puan <= 0 -> 0
        puan <= 2 -> 1
        puan <= 4 -> 2
        puan <= 7 -> 3
        else -> 4
    }

    private fun anahtarla(c: Calendar): String =
        String.format(
            Locale.US, "%04d%02d%02d",
            c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)
        )

    // ------------------------------------------------------------------
    // Özet bilgiler (fragment başlıkta gösteriyor)
    // ------------------------------------------------------------------

    /** Izgaradaki toplam aktif gün. */
    fun aktifGunSayisi(): Int = puanlar.count { it.value > 0 }

    /** En uzun kesintisiz seri (ızgara aralığında). */
    fun enUzunSeri(): Int {
        var enIyi = 0
        var simdiki = 0
        val imlec = baslangic.clone() as Calendar
        repeat(haftaSayisi * 7) {
            if (imlec.timeInMillis <= System.currentTimeMillis()) {
                val p = puanlar[anahtarla(imlec)] ?: 0
                if (p > 0) {
                    simdiki++
                    if (simdiki > enIyi) enIyi = simdiki
                } else {
                    simdiki = 0
                }
            }
            imlec.add(Calendar.DAY_OF_YEAR, 1)
        }
        return enIyi
    }

    /** Izgaradaki toplam puan. */
    fun toplamPuan(): Int = puanlar.values.sum()

    /** Tarih anahtarını okunur metne çevirir: "14 Mart Cuma". */
    fun okunurTarih(anahtar: String): String = runCatching {
        val y = anahtar.substring(0, 4).toInt()
        val a = anahtar.substring(4, 6).toInt() - 1
        val g = anahtar.substring(6, 8).toInt()
        val c = Calendar.getInstance().apply { set(y, a, g) }
        java.text.SimpleDateFormat("d MMMM EEEE", turkce).format(c.time)
    }.getOrDefault(anahtar)
}
