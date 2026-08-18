package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.color.MaterialColors
import java.util.Calendar
import java.util.Locale

/**
 * v7.38 — Detaylı ilerleme analitiği ekranı.
 *
 * ProgressFragment (168 satır) yalnızca ısı haritası gösteriyordu.
 * Bu ekran veriyi ANLAMLI hale getirir: hangi saat, hangi gün, hangi hız,
 * ne zaman biter.
 *
 * Tüm hesaplar Analitik nesnesinde; burada yalnızca çizim yapılır.
 */
class AnalitikActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            context.startActivity(Intent(context, AnalitikActivity::class.java))
        }
    }

    private val yogunluk get() = resources.displayMetrics.density

    /**
     * v8.6 · Öneri 27 — Kullanıcının yazı boyutu tercihini uygular.
     *
     * `Configuration.fontScale` tüm `sp` birimlerini bir kerede
     * ölçekliyor; 71 layout'a tek tek dokunmaya gerek kalmıyor.
     */
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(GorunumAyar.yaziOlcegiUygula(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.styleFor(this))
        ThemeManager.applyAccent(this)
        // v8.3 · Öneri 10: Material You (açıksa duvar kâğıdı paleti)
        ThemeManager.dinamikRengiUygula(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analitik)

        findViewById<TextView>(R.id.anClose).setOnClickListener { finish() }
        findViewById<TextView>(R.id.anShare).setOnClickListener { raporPaylas() }

        cikarimlariCiz()
        // v7.75: bu hafta vs gecen hafta karsilastirmasi
        karsilastirmaCiz()
        saatleriCiz()
        gunleriCiz()
        egilimCiz()
        kurslariCiz()
        aylariCiz()
        // v10.14 · E26/E27/E30: kronotip kartı, duygu haritası, sene filmi kapısı
        kronotipCiz()
        duyguHaritasiCiz()
        seneFilmiKapi()
    }

    /**
     * v7.75 — "Bu hafta geçen haftaya göre %20 fazla çalıştın".
     *
     * Ham sayı tek başına bir şey ifade etmiyordu; karşılaştırma
     * olmadan kullanıcı iyi mi kötü mü gittiğini bilemiyordu.
     * Çıkarım kartının en üstüne eklenir.
     */
    private fun karsilastirmaCiz() {
        try {
            val kap = findViewById<LinearLayout>(R.id.anInsights) ?: return
            val k = Analitik.haftaKarsilastir(this)
            val dp = resources.displayMetrics.density

            val metin = when {
                !k.yeterliVeri -> getString(R.string.kr_veri_yok)
                k.yuzde > 5 -> getString(R.string.kr_artis, k.yuzde)
                k.yuzde < -5 -> getString(R.string.kr_azalis, -k.yuzde)
                else -> getString(R.string.kr_ayni)
            }

            kap.addView(
                TextView(this).apply {
                    text = getString(R.string.kr_baslik)
                    textSize = 12f
                    alpha = 0.7f
                    setPadding(0, (8 * dp).toInt(), 0, (2 * dp).toInt())
                },
                0
            )
            kap.addView(
                TextView(this).apply {
                    text = metin
                    textSize = 15f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(
                        when {
                            !k.yeterliVeri -> com.google.android.material.color.MaterialColors
                                .getColor(
                                    this,
                                    com.google.android.material.R.attr.colorOnSurface, 0
                                )
                            k.yuzde > 5 -> GrafikDili.BASARI
                            k.yuzde < -5 -> GrafikDili.HATA
                            else -> com.google.android.material.color.MaterialColors.getColor(
                                this, com.google.android.material.R.attr.colorOnSurface, 0
                            )
                        }
                    )
                },
                1
            )
            if (k.yeterliVeri) {
                kap.addView(
                    TextView(this).apply {
                        text = getString(R.string.kr_bu_hafta) + ": " +
                            getString(R.string.kr_dakika, k.buHafta) + "  ·  " +
                            getString(R.string.kr_gecen_hafta) + ": " +
                            getString(R.string.kr_dakika, k.gecenHafta)
                        textSize = 11.5f
                        alpha = 0.7f
                        setPadding(0, (3 * dp).toInt(), 0, (8 * dp).toInt())
                    },
                    2
                )
            }
        } catch (e: Exception) {
            android.util.Log.w("AnalitikActivity", "Karsilastirma cizilemedi", e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 1) AKILLI ÇIKARIMLAR
    // ═══════════════════════════════════════════════════════════════

    private fun cikarimlariCiz() {
        val kap = findViewById<LinearLayout>(R.id.anInsights)
        val kart = findViewById<View>(R.id.anInsightCard)
        // v7.97: mevcut çıkarımlara program/zayıf konu/oturum analizi eklendi
        val liste = Analitik.cikarimlar(this) + Analitik.derinCikarimlar(this)

        // v7.75: kart her durumda gorunur — karsilastirma bolumu
        // cikarim olmasa bile ustte yer aliyor.
        kart.visibility = View.VISIBLE
        if (liste.isEmpty()) {
            // Yeni kullanıcı — henüz analiz için veri yok
            kap.addView(satirYazi(getString(R.string.an_no_data), 13f, 0.8f))
            return
        }
        liste.forEach { metin ->
            kap.addView(
                TextView(this).apply {
                    text = "• " + metin
                    textSize = 13.5f
                    setLineSpacing(3f * yogunluk, 1f)
                    setPadding(0, (3 * yogunluk).toInt(), 0, (3 * yogunluk).toInt())
                }
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 2) SAAT DAĞILIMI
    // ═══════════════════════════════════════════════════════════════

    private fun saatleriCiz() {
        val grafik = findViewById<BarChartView>(R.id.anHourChart)
        val altYazi = findViewById<TextView>(R.id.anHourSub)
        val dilimKabi = findViewById<LinearLayout>(R.id.anSlots)

        if (!Analitik.saatVerisiVarMi(this)) {
            altYazi.text = getString(R.string.an_hours_empty)
            grafik.visibility = View.GONE
            return
        }

        val dagilim = Analitik.saatDagilimi(this)
        // 24 saat etiketi dar ekranda sığmaz — 3'te bir göster
        val etiketler = (0..23).map { if (it % 3 == 0) it.toString() else "" }
        grafik.setData(dagilim.toList(), etiketler, 1)

        val enIyi = Analitik.enVerimliSaat(this)
        altYazi.text = if (enIyi >= 0) {
            getString(
                R.string.an_hours_best,
                String.format(Locale.US, "%02d:00", enIyi),
                String.format(Locale.US, "%02d:00", (enIyi + 1) % 24)
            )
        } else {
            getString(R.string.an_hours_sub)
        }

        // Dilim özetleri (sabah/öğle/akşam/gece)
        val dilimler = Analitik.saatDilimleri(this)
        val toplam = dilimler.sumOf { it.puan }.coerceAtLeast(1)
        dilimler.sortedByDescending { it.puan }.forEach { d ->
            val yuzde = d.puan * 100 / toplam
            dilimKabi.addView(oranSatiri(d.ad, yuzde, d.puan > 0))
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 3) HAFTANIN GÜNLERİ
    // ═══════════════════════════════════════════════════════════════

    private fun gunleriCiz() {
        val grafik = findViewById<BarChartView>(R.id.anDayChart)
        val altYazi = findViewById<TextView>(R.id.anDaySub)

        val gunler = Analitik.gunDagilimi(this)
        val degerler = gunler.map { it.ortalama }

        if (degerler.all { it == 0 }) {
            altYazi.text = getString(R.string.an_days_empty)
            grafik.visibility = View.GONE
            return
        }

        grafik.setData(degerler, gunler.map { it.kisaAd }, 1)

        val enIyi = gunler.filter { it.aktifGun > 0 }.maxByOrNull { it.ortalama }
        altYazi.text = if (enIyi != null) {
            getString(R.string.an_days_best, enIyi.ad)
        } else {
            getString(R.string.an_days_sub)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 4) HAFTALIK EĞİLİM
    // ═══════════════════════════════════════════════════════════════

    private fun egilimCiz() {
        val grafik = findViewById<BarChartView>(R.id.anTrendChart)
        val altYazi = findViewById<TextView>(R.id.anTrendSub)

        val haftalar = Analitik.haftalikEgilim(this, 8)
        if (haftalar.isEmpty() || haftalar.all { it.dakika == 0 && it.madde == 0 }) {
            altYazi.text = getString(R.string.an_trend_empty)
            grafik.visibility = View.GONE
            return
        }

        grafik.setData(
            haftalar.map { it.dakika },
            haftalar.mapIndexed { i, _ -> if (i % 2 == 0) "" else "" },
            1
        )

        // Son iki haftayı karşılaştır
        val son = haftalar.lastOrNull()?.dakika ?: 0
        val onceki = haftalar.getOrNull(haftalar.size - 2)?.dakika ?: 0
        altYazi.text = when {
            onceki == 0 && son > 0 -> getString(R.string.an_trend_new, son)
            onceki == 0 -> getString(R.string.an_trend_sub)
            son > onceki -> getString(
                R.string.an_trend_up, (son - onceki) * 100 / onceki
            )
            son < onceki -> getString(
                R.string.an_trend_down, (onceki - son) * 100 / onceki
            )
            else -> getString(R.string.an_trend_same)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 5) KURS HIZI VE BİTİŞ TAHMİNİ
    // ═══════════════════════════════════════════════════════════════

    private fun kurslariCiz() {
        val kap = findViewById<LinearLayout>(R.id.anCourses)
        val kart = findViewById<View>(R.id.anCourseCard)

        val kurslar = Analitik.kursHizlari(this).filter { it.toplamDers > 0 }
        if (kurslar.isEmpty()) {
            kart.visibility = View.GONE
            return
        }

        kurslar.take(8).forEach { k ->
            val satir = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, (7 * yogunluk).toInt(), 0, (7 * yogunluk).toInt())
            }

            // Başlık satırı: emoji + ad + yüzde
            satir.addView(
                LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    addView(TextView(this@AnalitikActivity).apply {
                        text = k.emoji + " " + k.kursAdi
                        textSize = 13.5f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        layoutParams = LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                        )
                        maxLines = 1
                        ellipsize = android.text.TextUtils.TruncateAt.END
                    })
                    addView(TextView(this@AnalitikActivity).apply {
                        text = "%" + k.yuzde
                        textSize = 13.5f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                    })
                }
            )

            // İlerleme çubuğu
            satir.addView(cubuk(k.yuzde))

            // Detay: X/Y ders · ders başı Z dk
            satir.addView(
                satirYazi(
                    getString(
                        R.string.an_course_detail,
                        k.bitenDers, k.toplamDers, k.dersBasiDakika
                    ),
                    11.5f, 0.75f
                )
            )

            // Bitiş tahmini
            val tahmin = when {
                k.kalanDers == 0 -> getString(R.string.an_course_done)
                k.tahminiTarih.isNotBlank() -> getString(
                    R.string.an_course_eta, k.kalanHafta, k.tahminiTarih
                )
                else -> getString(R.string.an_course_no_eta)
            }
            satir.addView(satirYazi(tahmin, 11.5f, 0.9f))

            kap.addView(satir)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 6) AYLIK KARŞILAŞTIRMA
    // ═══════════════════════════════════════════════════════════════

    private fun aylariCiz() {
        val kap = findViewById<LinearLayout>(R.id.anMonths)
        val altYazi = findViewById<TextView>(R.id.anMonthSub)

        val aylar = Analitik.aylikOzet(this, 6).filter {
            it.madde > 0 || it.dakika > 0 || it.aktifGun > 0
        }
        if (aylar.isEmpty()) {
            altYazi.text = getString(R.string.an_months_empty)
            return
        }

        val degisim = Analitik.aylikDegisim(this)
        altYazi.text = when {
            degisim > 0 -> getString(R.string.an_month_up, degisim)
            degisim < 0 -> getString(R.string.an_month_down, -degisim)
            else -> getString(R.string.an_months_sub)
        }

        val enBuyuk = aylar.maxOf { it.dakika }.coerceAtLeast(1)
        aylar.forEach { a ->
            val satir = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, (6 * yogunluk).toInt(), 0, (6 * yogunluk).toInt())
            }
            satir.addView(
                LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    addView(TextView(this@AnalitikActivity).apply {
                        text = a.ad
                        textSize = 13f
                        layoutParams = LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                        )
                    })
                    addView(TextView(this@AnalitikActivity).apply {
                        text = a.dakika.toString() + " dk"
                        textSize = 13f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                    })
                }
            )
            satir.addView(cubuk(a.dakika * 100 / enBuyuk))
            satir.addView(
                satirYazi(
                    getString(R.string.an_month_detail, a.madde, a.aktifGun, a.soru),
                    11.5f, 0.75f
                )
            )
            kap.addView(satir)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // PAYLAŞ
    // ═══════════════════════════════════════════════════════════════

    // ═══════════════════════════════════════════════════════════════
    // v10.14 · E26 — KRONOTİP KARTI (uyku defteri + saat analizi tek kart)
    // ═══════════════════════════════════════════════════════════════

    private fun kronotipCiz() {
        val kap = findViewById<LinearLayout>(R.id.anContent) ?: return
        kap.addView(bolumBaslik(getString(R.string.ge_kronotip_baslik)))

        val uyanislar = UykuCerceve.defter(this).mapNotNull { gun ->
            if (gun.uyandiMs <= 0L) null
            else Calendar.getInstance().apply { timeInMillis = gun.uyandiMs }
                .let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
        }
        if (uyanislar.size < 5) {
            kap.addView(satirYazi(getString(R.string.ge_kronotip_veri_az), 13f, 0.8f))
            return
        }

        val ort = Kronotip.ortUyanis(uyanislar)
        val tip = Kronotip.tip(ort)
        val tipAd = getString(
            when (tip) {
                Kronotip.Tip.SERCE -> R.string.ge_tip_serce
                Kronotip.Tip.GECE_KUSU -> R.string.ge_tip_gece
                else -> R.string.ge_tip_guvencin
            }
        )
        kap.addView(satirYazi(Kronotip.tipEmoji(tip) + "  " + tipAd, 16f, 1f))
        kap.addView(
            satirYazi(
                getString(
                    R.string.ge_kronotip_uyanis,
                    UykuCerceve.saatMetni(ort),
                    UykuCerceve.sureKisa(Kronotip.sapma(uyanislar) * 60_000L)
                ),
                12.5f, 0.75f
            )
        )

        val enIyi = Analitik.enVerimliSaat(this)
        if (enIyi < 0) {
            kap.addView(satirYazi(getString(R.string.ge_kronotip_odak_veri_az), 12.5f, 0.75f))
            return
        }
        val bas = Kronotip.odakPenceresi(enIyi)
        kap.addView(
            satirYazi(getString(R.string.ge_kronotip_odak, Kronotip.saatAralik(bas)), 13f, 0.9f)
        )
        kap.addView(
            aksiyonCip(kronotipAksiyonMetni(bas)) { kronotipAksiyon(bas) }
        )
    }

    private fun kronotipAksiyonMetni(bas: Int): String {
        val simdi = Calendar.getInstance()
        val simdiDk = simdi.get(Calendar.HOUR_OF_DAY) * 60 + simdi.get(Calendar.MINUTE)
        return if (Kronotip.penceredeMi(simdiDk, bas)) {
            getString(R.string.ge_kronotip_simdi)
        } else {
            getString(R.string.ge_kronotip_kur, String.format(Locale.US, "%02d:00", bas))
        }
    }

    /** Penceredeyse 25 dk odak yayını; değilse pencereye hatırlatma kur. */
    private fun kronotipAksiyon(bas: Int) {
        val simdi = Calendar.getInstance()
        val simdiDk = simdi.get(Calendar.HOUR_OF_DAY) * 60 + simdi.get(Calendar.MINUTE)
        if (Kronotip.penceredeMi(simdiDk, bas)) {
            sendBroadcast(
                Intent(this, TimerActionReceiver::class.java).apply {
                    action = TimerActionReceiver.ACTION_BASLAT_DK
                    putExtra(TimerActionReceiver.EXTRA_DAKIKA, 25)
                }
            )
            return
        }
        val hedef = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, bas)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        try {
            val gorevler = Store.loadTasks(this)
            val yeni = Store.Task(
                id = System.currentTimeMillis(),
                text = getString(R.string.ge_odak_hatirlatma_metin),
                done = false,
                createdAt = System.currentTimeMillis(),
                dueAt = hedef.timeInMillis
            )
            gorevler.add(yeni)
            Store.saveTasks(this, gorevler)
            runCatching { AlarmScheduler.schedule(this, yeni.id, yeni.text, yeni.dueAt) }
            runCatching { WidgetCommon.refreshAll(this, true) }
            android.widget.Toast.makeText(
                this,
                getString(R.string.ge_kronotip_kuruldu, String.format(Locale.US, "%02d:00", bas)),
                android.widget.Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            android.util.Log.w("AnalitikActivity", "Odak hatırlatması kurulamadı", e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // v10.14 · E27 — DUYGU HARİTASI (30 günlük mikro günlük şeridi)
    // ═══════════════════════════════════════════════════════════════

    private fun duyguHaritasiCiz() {
        val kap = findViewById<LinearLayout>(R.id.anContent) ?: return
        kap.addView(bolumBaslik(getString(R.string.ge_duygu_baslik)))

        val kayitlar = MikroGunluk.sonKac(this, 30)
        if (kayitlar.isEmpty()) {
            kap.addView(satirYazi(getString(R.string.ge_duygu_bos), 13f, 0.8f))
        } else {
            val serit = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            kayitlar.forEach { (anahtar, g) ->
                serit.addView(
                    TextView(this).apply {
                        text = (if (g.puan > 0) MikroGunluk.emojiFor(g.puan) else "▫") +
                            "\n" + anahtar.takeLast(2)
                        textSize = 10f
                        gravity = Gravity.CENTER
                        setPadding(
                            (5 * yogunluk).toInt(), (3 * yogunluk).toInt(),
                            (5 * yogunluk).toInt(), (3 * yogunluk).toInt()
                        )
                    }
                )
            }
            kap.addView(
                android.widget.HorizontalScrollView(this).apply {
                    isHorizontalScrollBarEnabled = false
                    addView(serit)
                }
            )
            val puanlar = kayitlar.map { it.second.puan }.filter { it > 0 }
            if (puanlar.isNotEmpty()) {
                kap.addView(
                    satirYazi(
                        getString(
                            R.string.ge_duygu_ozet,
                            MikroGunluk.ortalama(puanlar).toDouble(),
                            MikroGunluk.iyiSayisi(puanlar)
                        ),
                        12.5f, 0.8f
                    )
                )
            }
        }
        kap.addView(
            aksiyonCip(getString(R.string.ge_duygu_ekle)) {
                startActivity(Intent(this, MikroGunlukActivity::class.java))
            }
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // v10.14 · E30 — SENENİN FİLMİ KAPISI (Aralık'ta ana ekranda önerilir)
    // ═══════════════════════════════════════════════════════════════

    private fun seneFilmiKapi() {
        val kap = findViewById<LinearLayout>(R.id.anContent) ?: return
        kap.addView(bolumBaslik(getString(R.string.ge_film_bolum_baslik)))
        kap.addView(satirYazi(getString(R.string.ge_film_bolum_alt), 12.5f, 0.75f))
        kap.addView(
            aksiyonCip(getString(R.string.ge_film_izle)) {
                startActivity(Intent(this, SeneFilmiActivity::class.java))
            }
        )
    }

    // ---------------- v10.14 yardımcı görünümler ----------------

    private fun bolumBaslik(metin: String) = TextView(this).apply {
        text = metin
        textSize = 16f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, (22 * yogunluk).toInt(), 0, (4 * yogunluk).toInt())
    }

    private fun aksiyonCip(metin: String, tikla: () -> Unit): TextView {
        val vurgu = MaterialColors.getColor(
            this, com.google.android.material.R.attr.colorPrimary, 0
        )
        return TextView(this).apply {
            text = metin
            textSize = 13.5f
            setTextColor(vurgu)
            setPadding(
                (16 * yogunluk).toInt(), (10 * yogunluk).toInt(),
                (16 * yogunluk).toInt(), (10 * yogunluk).toInt()
            )
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 16 * yogunluk
                setStroke((1.5f * yogunluk).toInt(), vurgu)
                setColor((vurgu and 0x00FFFFFF) or 0x22000000)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (8 * yogunluk).toInt() }
            isClickable = true
            setOnClickListener { tikla() }
        }
    }

    private fun raporPaylas() {
        val sb = StringBuilder()
        sb.append(getString(R.string.an_report_head)).append("\n\n")

        (Analitik.cikarimlar(this) + Analitik.derinCikarimlar(this))
            .forEach { sb.append("• ").append(it).append("\n") }

        val (seri, rekor) = Store.streakInfo(this)
        sb.append("\n").append(getString(R.string.an_report_streak, seri, rekor)).append("\n")
        sb.append(getString(R.string.an_report_days, Analitik.toplamAktifGun(this))).append("\n")
        sb.append(getString(R.string.an_report_focus, Store.allTimeFocus(this))).append("\n")

        val kurslar = Analitik.kursHizlari(this).filter { it.toplamDers > 0 }
        if (kurslar.isNotEmpty()) {
            sb.append("\n").append(getString(R.string.an_courses_title)).append("\n")
            kurslar.take(6).forEach { k ->
                sb.append("• ").append(k.kursAdi).append(": %").append(k.yuzde)
                if (k.tahminiTarih.isNotBlank()) {
                    sb.append(" → ").append(k.tahminiTarih)
                }
                sb.append("\n")
            }
        }

        try {
            startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, sb.toString().trim())
                    },
                    getString(R.string.an_share)
                )
            )
        } catch (e: Exception) {
            android.util.Log.w("AnalitikActivity", "Rapor paylaşılamadı", e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GÖRSEL YARDIMCILAR
    // ═══════════════════════════════════════════════════════════════

    private fun satirYazi(metin: String, boyut: Float, saydam: Float): TextView =
        TextView(this).apply {
            text = metin
            textSize = boyut
            alpha = saydam
            setPadding(0, (2 * yogunluk).toInt(), 0, 0)
        }

    /** İnce ilerleme çubuğu. */
    private fun cubuk(yuzde: Int): View {
        val kap = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (5 * yogunluk).toInt()
            ).apply {
                topMargin = (4 * yogunluk).toInt()
                bottomMargin = (2 * yogunluk).toInt()
            }
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 3 * yogunluk
                setColor(
                    (MaterialColors.getColor(
                        this@AnalitikActivity,
                        com.google.android.material.R.attr.colorOnSurface, 0
                    ) and 0x00FFFFFF) or 0x22000000
                )
            }
        }
        val dolu = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT,
                yuzde.coerceIn(0, 100).toFloat()
            )
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 3 * yogunluk
                setColor(
                    MaterialColors.getColor(
                        this@AnalitikActivity,
                        com.google.android.material.R.attr.colorPrimary, 0
                    )
                )
            }
        }
        val bos = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT,
                (100 - yuzde.coerceIn(0, 100)).toFloat()
            )
        }
        kap.addView(dolu)
        kap.addView(bos)
        return kap
    }

    /** "Sabah  %45" biçiminde oran satırı. */
    private fun oranSatiri(ad: String, yuzde: Int, aktif: Boolean): View {
        val satir = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, (4 * yogunluk).toInt(), 0, (4 * yogunluk).toInt())
            alpha = if (aktif) 1f else 0.45f
        }
        satir.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(TextView(this@AnalitikActivity).apply {
                    text = ad
                    textSize = 12.5f
                    layoutParams = LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                    )
                })
                addView(TextView(this@AnalitikActivity).apply {
                    text = "%" + yuzde
                    textSize = 12.5f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                })
            }
        )
        satir.addView(cubuk(yuzde))
        return satir
    }
}
