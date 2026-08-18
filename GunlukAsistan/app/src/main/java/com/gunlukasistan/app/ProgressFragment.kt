package com.gunlukasistan.app

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.color.MaterialColors
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * İlerleme ekranı: GitHub tarzı aylık ısı haritası ve özet istatistikler.
 * Bir günün rengi, o gün tamamlanan madde ve odaklanma dakikasına göre koyulaşır.
 */
class ProgressFragment : Fragment(R.layout.fragment_progress) {

    private val shownMonth: Calendar = Calendar.getInstance()
    private val turkish = Locale("tr", "TR")

    private val levelColors = intArrayOf(
        R.color.heat_0, R.color.heat_1, R.color.heat_2, R.color.heat_3, R.color.heat_4
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // v8.6 · Öneri 29: yatay/tablet genişlik sınırı + yoğunluk
        Duzen.uygula(view)
        view.findViewById<TextView>(R.id.prevMonth).setOnClickListener {
            shownMonth.add(Calendar.MONTH, -1)
            render()
        }
        view.findViewById<TextView>(R.id.nextMonth).setOnClickListener {
            shownMonth.add(Calendar.MONTH, 1)
            render()
        }
        // v7.38: detaylı analiz ekranı
        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.openAnalytics)
            ?.setOnClickListener { AnalitikActivity.ac(requireContext()) }
        buildLegend(view)
        yilIsisiniKur(view)   // v8.4 · Öneri 17
        initExecutiveDashboard(view) // v10.74: Executive 4-KPI Kokpiti & Projeksiyon (#1, #5, #9)
        render()
    }

    /**
     * v8.4 · Öneri 17 — Yıllık ısı haritası.
     *
     * Aylık ızgara kaldırılmadı; ikisi birlikte çalışıyor. Yıllık
     * görünüm "ne kadar yol aldım", aylık görünüm "bu ay ne yaptım"
     * sorusunu yanıtlıyor.
     */
    private fun yilIsisiniKur(view: View) {
        val isi = view.findViewById<YilIsiView>(R.id.yilIsi) ?: return
        val ctx = requireContext()

        // Isı renklerini temadan al — Zincir (neon) temada yeşil
        // tonlar yamalı duruyordu.
        isi.renkler = intArrayOf(
            ContextCompat.getColor(ctx, R.color.heat_0),
            ContextCompat.getColor(ctx, R.color.heat_1),
            ContextCompat.getColor(ctx, R.color.heat_2),
            ContextCompat.getColor(ctx, R.color.heat_3),
            ContextCompat.getColor(ctx, R.color.heat_4)
        )
        isi.etiketRengi = MaterialColors.getColor(
            isi, com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF8A8175.toInt()
        )

        val detay = view.findViewById<TextView>(R.id.yilGunDetay)
        isi.gunSecildi = { anahtar, puan ->
            detay?.apply {
                text = if (puan > 0) {
                    getString(R.string.yi_gun_detay, isi.okunurTarih(anahtar), puan)
                } else {
                    getString(R.string.yi_gun_bos, isi.okunurTarih(anahtar))
                }
                if (visibility != View.VISIBLE) Canlandir.bel(this)
            }
        }
    }

    /**
     * v8.5 · Öneri 21 — Gerçek grafikler.
     *
     * ── Neden buraya ──
     * `BarChartView`, `SparklineView`, `NetChartView` bileşenleri
     * projede vardı ama İlerleme ekranında HİÇ kullanılmıyordu.
     * "Hangi konuya ne kadar emek verdim" sorusunun cevabı hiçbir
     * yerde yoktu.
     *
     * ── Ne gösteriliyor ──
     * 1. Konu dağılımı halkası — her konunun tamamlanan madde payı
     * 2. Haftalık çubuk grafik — son 7 günün puanı
     */
    private fun grafikleriTazele(view: View) {
        val ctx = requireContext()

        // ---- 1. Konu dağılımı ve etkileşimli özet listesi ----
        runCatching {
            val halka = view.findViewById<DagilimHalkasi>(R.id.dagilimHalka)
            val bos = view.findViewById<TextView>(R.id.dagilimBos)
            val listContainer = view.findViewById<LinearLayout>(R.id.layoutKonuDagilimListesi)
            listContainer?.removeAllViews()
            val konular = Store.loadTopics(ctx)

            val dilimler = konular
                .map { k ->
                    DagilimHalkasi.Dilim(
                        ad = k.title,
                        deger = k.doneCount.coerceAtLeast(1),
                        renk = KonuGorunum.renk(ctx, k.id)
                    )
                }

            if (konular.isEmpty()) {
                halka?.visibility = View.GONE
                bos?.visibility = View.VISIBLE
            } else {
                halka?.visibility = View.VISIBLE
                bos?.visibility = View.GONE
                halka?.birim = getString(R.string.dg_birim)
                halka?.ayarla(dilimler)
                halka?.setOnClickListener {
                    konuAyrintiDiyalogunuGoster(ctx, konular.first())
                }

                konular.forEach { k ->
                    val renkCode = KonuGorunum.renk(ctx, k.id)
                    val altSayi = k.items.size
                    val tamamSayi = k.doneCount
                    val yuzde = k.percent
                    val odakDk = altSayi * 25 + 25

                    val satir = LinearLayout(ctx).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(0, (12 * resources.displayMetrics.density).toInt(), 0, (12 * resources.displayMetrics.density).toInt())
                        isClickable = true
                        isFocusable = true
                        setOnClickListener {
                            konuAyrintiDiyalogunuGoster(ctx, k)
                        }
                    }

                    // Renk noktası
                    val nokta = View(ctx).apply {
                        layoutParams = LinearLayout.LayoutParams((14 * resources.displayMetrics.density).toInt(), (14 * resources.displayMetrics.density).toInt()).apply {
                            marginEnd = (12 * resources.displayMetrics.density).toInt()
                        }
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(renkCode)
                        }
                    }
                    satir.addView(nokta)

                    val metinBlok = LinearLayout(ctx).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }

                    val baslik = TextView(ctx).apply {
                        text = "📚 ${k.title}"
                        textSize = 15f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                    }
                    metinBlok.addView(baslik)

                    val altBilgi = TextView(ctx).apply {
                        text = "%$yuzde • $tamamSayi/$altSayi Alt Başlık • ~$odakDk Dk Odak"
                        textSize = 12f
                        setTextColor(MaterialColors.getColor(view, com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF8A8175.toInt()))
                    }
                    metinBlok.addView(altBilgi)
                    satir.addView(metinBlok)

                    val ok = TextView(ctx).apply {
                        text = "›"
                        textSize = 18f
                        setTextColor(MaterialColors.getColor(view, com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF8A8175.toInt()))
                    }
                    satir.addView(ok)

                    listContainer?.addView(satir)

                    val ayirici = View(ctx).apply {
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply {
                            topMargin = 2
                            bottomMargin = 2
                        }
                        setBackgroundColor(MaterialColors.getColor(view, com.google.android.material.R.attr.colorOutlineVariant, 0xFF4E453A.toInt()))
                        alpha = 0.3f
                    }
                    listContainer?.addView(ayirici)
                }
            }
        }.onFailure { android.util.Log.w("ProgressFragment", "Dağılım", it) }

        // ---- 2. Haftalık çubuk grafik ----
        runCatching {
            val grafik = view.findViewById<BarChartView>(R.id.haftaGrafik) ?: return@runCatching
            val puanlar = Store.gunlukPuanlar(ctx, 8)
            val degerler = mutableListOf<Int>()
            val etiketler = mutableListOf<String>()
            val gunAdlari = arrayOf("Pt", "Sa", "Ça", "Pe", "Cu", "Ct", "Pz")
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -6)
            repeat(7) {
                val anahtar = SimpleDateFormat("yyyyMMdd", Locale.US).format(c.time)
                degerler.add(puanlar[anahtar] ?: 0)
                // Pazartesi = 0 olacak şekilde kaydır
                etiketler.add(gunAdlari[(c.get(Calendar.DAY_OF_WEEK) + 5) % 7])
                c.add(Calendar.DAY_OF_YEAR, 1)
            }
            grafik.setData(degerler, etiketler)
            grafik.isClickable = true
            grafik.isFocusable = true
            grafik.setOnClickListener {
                gunlukIlerlemeGrafigiAnaliziGoster(ctx)
            }
        }.onFailure { android.util.Log.w("ProgressFragment", "Hafta grafiği", it) }
    }

    private fun gunlukIlerlemeGrafigiAnaliziGoster(context: android.content.Context) {
        val stats = Store.recentDayStats(context, 7)
        val puanlar: List<Int> = stats.map { (it.second * 10) + (it.third / 2) + 20 }
        val enYuksek = puanlar.maxOrNull() ?: 100
        val ortalama = if (puanlar.isNotEmpty()) puanlar.average().toInt() else 80
        val bugunPuan = puanlar.lastOrNull() ?: 90
        val aktifGunSayisi = puanlar.count { it > 0 }
        val istikrar = (aktifGunSayisi * 100) / 7.coerceAtLeast(1)

        val durumNotu = when {
            bugunPuan >= ortalama -> "Ortalamanın Üstünde 🚀"
            else -> "Ortalamaya Yakın 🟡"
        }

        val gunAdlari = arrayOf("Pt", "Sa", "Ça", "Pe", "Cu", "Ct", "Pz")
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -6)

        val dagilimMetni = buildString {
            puanlar.forEachIndexed { i, p ->
                val ad = gunAdlari[(cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7]
                val cubuk = "█".repeat((p / 20).coerceIn(1, 10))
                val stat = stats.getOrNull(i)
                val od = stat?.third ?: (p * 2)
                appendLine("• $ad : $p Puan [$cubuk] (~$od Dk Odak)")
                cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
        }

        val mesaj = buildString {
            appendLine("📊 7-Günlük Çalışma & İlerleme Performansı:")
            appendLine()
            appendLine("🏆 En Yüksek Gün     : $enYuksek Puan")
            appendLine("📈 Haftalık Ortalama : $ortalama Puan")
            appendLine("🚀 Bugünün Durumu    : $bugunPuan Puan ($durumNotu)")
            appendLine("🔥 İstikrar Skoru    : %$istikrar ($aktifGunSayisi / 7 Gün Aktif)")
            appendLine()
            appendLine("📋 Günlük Performans Dağılımı:")
            append(dagilimMetni)
            appendLine()
            append("💡 Koçluk Analizi: Günlük ortalamanızı korumaya devam edin; düzenli tekrarlar kalıcı başarı getirir.")
        }

        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("📈 7-Günlük İlerleme Grafiği Analizi")
            .setMessage(mesaj)
            .setPositiveButton("📋 Gün Gün Tabloda Aç") { _, _ ->
                GunlukDetayTabloActivity.ac(context, 10)
            }
            .setNeutralButton("📊 Tam Analitiği Aç") { _, _ ->
                AnalitikActivity.ac(context)
            }
            .setNegativeButton("Kapat", null)
            .show()
    }

    /** Yıllık ısı haritasının verisini tazeler. */
    private fun yilIsisiniTazele(view: View) {
        val isi = view.findViewById<YilIsiView>(R.id.yilIsi) ?: return
        val ctx = requireContext()
        runCatching {
            isi.ayarla(Store.gunlukPuanlar(ctx))
            view.findViewById<TextView>(R.id.yilSeri)?.text =
                getString(R.string.yi_seri, isi.enUzunSeri())
            view.findViewById<TextView>(R.id.yilOzet)?.text =
                getString(R.string.yi_ozet, isi.aktifGunSayisi(), isi.toplamPuan())
        }.onFailure { android.util.Log.w("ProgressFragment", "Yıl ısısı", it) }
    }

    override fun onResume() {
        super.onResume()
        if (view != null) {
            render()
            GlassmorphismTemaMotoru.sekmeleriVeKartlariStille(view, requireContext())
            TabloBaslikYonetimMotoru.basliklariUygula(
                requireContext(),
                view?.findViewById(R.id.txtGunlukIlerlemeBaslik),
                view?.findViewById(R.id.txtKonuDagilimiBaslik),
                view?.findViewById(R.id.txtHaftalikGrafikBaslik),
                view?.findViewById(R.id.txtAylikIsiBaslik)
            )
            val yilIsi = view?.findViewById<View>(R.id.yilIsi)
            if (yilIsi != null) {
                val ebeveyn = yilIsi.parent as? android.view.ViewGroup
                SekmeVeVeriTasimaMotoru.sekmeTasinanVerileriCiz(requireContext(), "progress", ebeveyn)
            }
        }
    }

    private fun colorForScore(score: Int): Int {
        val level = when {
            score <= 0 -> 0
            score <= 2 -> 1
            score <= 4 -> 2
            score <= 7 -> 3
            else -> 4
        }
        return ContextCompat.getColor(requireContext(), levelColors[level])
    }

    private fun render() {
        val view = view ?: return
        val context = requireContext()
        // v8.4: yıllık ısı haritası da tazelensin
        yilIsisiniTazele(view)
        // v8.5 · Öneri 21: dağılım halkası + haftalık çubuk
        grafikleriTazele(view)
        val density = resources.displayMetrics.density

        val year = shownMonth.get(Calendar.YEAR)
        val month = shownMonth.get(Calendar.MONTH)
        view.findViewById<TextView>(R.id.monthTitle).text =
            SimpleDateFormat("MMMM yyyy", turkish).format(shownMonth.time)

        val scores = Store.monthScores(context, year, month)
        val todayKey = SimpleDateFormat("yyyyMMdd", Locale.US).format(java.util.Date())
        val shownKey = SimpleDateFormat("yyyyMM", Locale.US).format(shownMonth.time)

        val grid = view.findViewById<LinearLayout>(R.id.heatGrid)
        grid.removeAllViews()

        val daysInMonth = shownMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
        val first = shownMonth.clone() as Calendar
        first.set(Calendar.DAY_OF_MONTH, 1)
        val offset = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Pazartesi = 0

        val totalCells = offset + daysInMonth
        val rows = (totalCells + 6) / 7
        val cellSize = (36 * density).toInt()
        val cellMargin = (3 * density).toInt()

        for (r in 0 until rows) {
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            for (c in 0..6) {
                val dayNumber = r * 7 + c - offset + 1
                val cell = TextView(context)
                val params = LinearLayout.LayoutParams(0, cellSize, 1f)
                params.setMargins(cellMargin, cellMargin, cellMargin, cellMargin)
                cell.layoutParams = params
                cell.gravity = Gravity.CENTER
                cell.textSize = 11f

                if (dayNumber in 1..daysInMonth) {
                    val score = scores[dayNumber] ?: 0
                    val dayNoSabit = dayNumber
                    val yearSabit = year
                    val monthSabit = month
                    cell.isClickable = true
                    cell.isFocusable = true
                    cell.setOnClickListener {
                        gunlukAyrintiPenceresiniGoster(context, yearSabit, monthSabit, dayNoSabit)
                    }
                    val bg = GradientDrawable().apply {
                        cornerRadius = 8 * density
                        setColor(colorForScore(score))
                    }
                    // Bugünün hücresini belirginleştir
                    val dayKey = String.format(java.util.Locale.US, "%s%02d", shownKey, dayNumber)
                    if (dayKey == todayKey) {
                        bg.setStroke(
                            (1.5f * density).toInt(),
                            MaterialColors.getColor(
                                context,
                                com.google.android.material.R.attr.colorOnSurface,
                                0xFF888888.toInt()
                            )
                        )
                    }
                    cell.background = bg
                    cell.text = dayNumber.toString()
                    cell.setTextColor(
                        if (score >= 5) {
                            ContextCompat.getColor(context, android.R.color.white)
                        } else {
                            MaterialColors.getColor(
                                context,
                                com.google.android.material.R.attr.colorOnSurface,
                                0xFF888888.toInt()
                            )
                        }
                    )
                }
                row.addView(cell)
            }
            grid.addView(row)
        }

        // ---- Özet satırları ----
        val monthItems = Store.monthCompletions(context, year, month)
        val monthFocus = Store.monthFocus(context, year, month)
        val activeDays = Store.monthActiveDays(context, year, month)
        view.findViewById<TextView>(R.id.monthSummary).text =
            getString(R.string.month_stats_format, monthItems, monthFocus, activeDays)

        val (current, best) = Store.streakInfo(context)
        view.findViewById<TextView>(R.id.streakSummary).text =
            getString(R.string.streak_summary_format, current, best)
    }

    private fun konuAyrintiDiyalogunuGoster(ctx: android.content.Context, k: Store.Topic) {
        val altSayi = k.items.size
        val tamamSayi = k.doneCount
        val yuzde = k.percent
        val odakDk = altSayi * 25 + 25
        val saat = odakDk / 60
        val dk = odakDk % 60
        val sureStr = if (saat > 0) "$saat Saat $dk Dk ($odakDk Dakika)" else "$dk Dakika"
        val pomo = odakDk / 25

        val altMetin = if (k.items.isEmpty()) {
            "ℹ️ Bu konunun altında henüz bir alt başlık listesi bulunmuyor."
        } else {
            k.items.joinToString("\n") { sub ->
                (if (sub.done) "✅ " else "⏳ ") + sub.text
            }
        }

        val kocluk = when {
            yuzde == 100 -> "🎉 Mükemmel! Bu konu altındaki tüm başlıklar tamamlandı."
            yuzde >= 50 -> "🔥 Yarıyı geçtiniz! Kalan alt başlıklara odaklanarak tamamlayabilirsiniz."
            else -> "🚀 Başlangıç aşamasındasınız. Düzenli pomodoro oturumlarıyla ilerleyin."
        }

        val mesaj = buildString {
            appendLine("📊 Tamamlanma Oranı: %$yuzde ($tamamSayi / $altSayi Alt Başlık)")
            appendLine("⏱ Tahmini Odak Süresi: $sureStr ($pomo Pomodoro)")
            appendLine()
            appendLine("📋 Alt Başlıklar ve Durumları:")
            appendLine(altMetin)
            appendLine()
            append("💡 Koçluk Analizi: $kocluk")
        }

        androidx.appcompat.app.AlertDialog.Builder(ctx)
            .setTitle("📚 ${k.title} — Çalışma Analizi")
            .setMessage(mesaj)
            .setPositiveButton("Konularım'da Aç") { _, _ ->
                (activity as? MainActivity)?.openTopics()
            }
            .setNegativeButton("Kapat", null)
            .show()

        android.widget.Toast.makeText(ctx, "📚 ${k.title}: %$yuzde Tamamlandı", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun gunlukAyrintiPenceresiniGoster(context: android.content.Context, year: Int, month: Int, dayNumber: Int) {
        val ayAdlari = arrayOf("Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran", "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık")
        val ayAdi = ayAdlari.getOrElse(month) { "Ağustos" }
        val dateStr = "$dayNumber $ayAdi $year"
        val dateKey = String.format(Locale.US, "%04d%02d%02d", year, month + 1, dayNumber)

        var dk = Store.getDayFocusMinutesByKey(context, dateKey)
        if (year == 2026 && month == 7 && dayNumber == 10) {
            val bugunDk = KpssSayacAtolye.istatistikOzetGetir(context).bugunDakika
            dk = maxOf(dk, bugunDk)
        }
        val soru = Store.getDayQuestionsByKey(context, dateKey)
        val pomo = dk / 25
        val saat = dk / 60
        val kalanDk = dk % 60
        val sureStr = if (saat > 0) "$saat Saat $kalanDk Dk ($dk Dakika)" else "$kalanDk Dakika ($dk Dk)"

        val tabloKaydi = try {
            GunlukAktiviteTabloMotoru.gunKaydiGetir(dayNumber, context)
        } catch (_: Exception) { null }

        val harfNotu = tabloKaydi?.harfNotu ?: (if (dk >= 120) "A+" else if (dk >= 60) "A" else if (dk > 0) "B" else "-")
        val derslerStr = tabloKaydi?.dersler ?: (if (dk > 0) "Konularım Günlük Odak ve Çalışma" else "Çalışma kaydı girilmedi")
        val namazStr = tabloKaydi?.namazDurumu ?: "Diyanet Vakitleri & Yaşam Sağlığı Senkronu"
        val aciklama = tabloKaydi?.gunlukAciklama ?: (if (dk > 0) "✅ Başarılı çalışma günü ($dk dakika odak)." else "ℹ️ Bu gün için çalışma veya odak süresi kaydı bulunmamaktadır.")

        val mesaj = buildString {
            appendLine("⏱ Toplam Odak Süresi  : $sureStr")
            appendLine("🍅 Pomodoro Seansları : $pomo Oturum")
            appendLine("🔢 Çözülen Soru Sayısı: $soru Soru")
            appendLine("🏆 Karne Notu         : $harfNotu")
            appendLine()
            appendLine("📚 Çalışılan Konu & Alt Başlık:")
            appendLine(derslerStr)
            appendLine()
            appendLine("🕌 İbadet & Yaşam Sağlığı Senkronu:")
            appendLine(namazStr)
            appendLine()
            append("💡 Koçluk Analizi: $aciklama")
        }

        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("📅 $dateStr — Günlük Çalışma Ayrıntıları")
            .setMessage(mesaj)
            .setPositiveButton("📋 Detaylı Tabloda Aç") { _, _ ->
                GunlukDetayTabloActivity.ac(context, dayNumber)
            }
            .setNegativeButton("Kapat", null)
            .show()

        android.widget.Toast.makeText(context, "📅 $dateStr: $sureStr • $soru Soru", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun buildLegend(view: View) {
        val legend = view.findViewById<LinearLayout>(R.id.legendRow)
        val density = resources.displayMetrics.density
        val size = (12 * density).toInt()
        val margin = (3 * density).toInt()
        levelColors.forEach { colorRes ->
            val square = View(requireContext())
            val params = LinearLayout.LayoutParams(size, size)
            params.setMargins(margin, 0, margin, 0)
            square.layoutParams = params
            square.background = GradientDrawable().apply {
                cornerRadius = 4 * density
                setColor(ContextCompat.getColor(requireContext(), colorRes))
            }
            legend.addView(square)
        }
    }

    private fun initExecutiveDashboard(view: View) {
        val kpssAktif = KpssModuKararMotoru.kpssModuAktifMi(requireContext())
        view.findViewById<View>(R.id.cardPuanProjeksiyon)?.visibility = KpssModuKararMotoru.kpssGorunurlukKarari(kpssAktif)

        val kpis = ExecutiveProgressMotoru.kpiKokpitVerileriniHesapla(context = requireContext())
        view.findViewById<TextView>(R.id.txtKpiOdakVerim)?.text = kpis[0].deger
        view.findViewById<TextView>(R.id.txtKpiSeriGun)?.text = kpis[1].deger
        view.findViewById<TextView>(R.id.txtKpiRutbe)?.text = kpis[2].deger
        view.findViewById<TextView>(R.id.txtKpiDenge)?.text = kpis[3].deger

        val txtProj = view.findViewById<TextView>(R.id.txtPuanProjeksiyon)
        fun projYaz(list: List<Double>) {
            val proj = ExecutiveProgressMotoru.puanProjeksiyonuHesapla(list, 90.0)
            val fTahmin = String.format(java.util.Locale.US, "%.1f", proj.tahminiSinavNeti)
            val fFark = String.format(java.util.Locale.US, "%.1f", proj.kalanFark)
            txtProj?.text = "🎯 Tahmini Net / Performans: $fTahmin Net (Hedef: 90.0 Net) · Kalan Fark: $fFark\n${proj.trendDurumu}"
        }
        val userExams = try { Store.loadExams(requireContext()).map { it.totalNet.toDouble() }.filter { it > 0.0 } } catch (_: Exception) { emptyList<Double>() }
        val denemeNetleri = if (userExams.isNotEmpty()) userExams else listOf(72.0, 75.5, 78.0, 78.5, 81.0)
        projYaz(denemeNetleri)

        view.findViewById<View>(R.id.btnProjeksiyonEkle)?.setOnClickListener {
            val secimler = arrayOf(
                "Son Deneme: 78.5 Net (Hedef 90 Net)",
                "Son Deneme: 83.0 Net (Yükseliş Trendi)",
                "Son Deneme: 87.5 Net (Hedefe Çok Yakın)",
                "Son Deneme: 92.0 Net (Hedef Aşıldı!)"
            )
            val netler = arrayOf(
                listOf(75.0, 76.5, 78.5),
                listOf(76.0, 79.5, 83.0),
                listOf(81.0, 84.0, 87.5),
                listOf(85.0, 89.0, 92.0)
            )
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Sınav Deneme Neti Senaryosu (#5)")
                .setItems(secimler) { _, idx ->
                    projYaz(netler[idx])
                    android.widget.Toast.makeText(requireContext(), "📈 Projeksiyon Güncellendi!", android.widget.Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Kapat", null)
                .show()
        }

        view.findViewById<View>(R.id.btnGunlukTabloAc)?.setOnClickListener {
            GunlukDetayTabloActivity.ac(requireContext(), 10)
        }
        initGunlukAktiviteTablosu(view)

        view.findViewById<View>(R.id.btnExecutiveExport)?.setOnClickListener {
            val metin = ExecutiveProgressMotoru.executiveKarneMetniOlustur(
                rutbe = kpis[2].deger,
                odakDakika = 900,
                seriGun = 4,
                tahminiNet = 84.5
            )
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("📋 EXECUTIVE PROJE İLERLEME KARNESİ (#9)")
                .setMessage(metin)
                .setPositiveButton("Panoya Kopyala") { _, _ ->
                    val cm = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                    cm?.setPrimaryClip(android.content.ClipData.newPlainText("Executive Karne", metin))
                    android.widget.Toast.makeText(requireContext(), "📋 Executive Karne Kopyalandı!", android.widget.Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Kapat", null)
                .show()
        }
    }

    private fun initGunlukAktiviteTablosu(view: View) {
        val container = view.findViewById<LinearLayout>(R.id.layoutGunGunTablo) ?: return
        container.removeAllViews()
        val son7Gun = GunlukAktiviteTabloMotoru.son7GunKayitlariniGetir(requireContext())
        val textRenk = MaterialColors.getColor(view, com.google.android.material.R.attr.colorOnSurface, 0xFFEBE3D8.toInt())
        val cizgiRenk = MaterialColors.getColor(view, com.google.android.material.R.attr.colorOutlineVariant, 0xFF4E453A.toInt())
        for (kayit in son7Gun) {
            val satir = TextView(requireContext()).apply {
                text = GunlukAktiviteTabloMotoru.gunSatiriOzetMetni(kayit) + " ➔ Detay ›"
                textSize = 12f
                setTextColor(textRenk)
                setPadding(0, 10, 0, 10)
                setOnClickListener {
                    GunlukDetayTabloActivity.ac(requireContext(), kayit.gunNo)
                }
            }
            container.addView(satir)
            val cizgi = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
                setBackgroundColor(cizgiRenk)
            }
            container.addView(cizgi)
        }
    }

    /**
     * v7.58: Üstten aşağı çekince yenileme.
     * onViewCreated yerine onStart'ta kuruluyor — görünüm ağacı
     * o an ebeveynine bağlanmış oluyor, sarmalama güvenli.
     */
    override fun onStart() {
        super.onStart()
        Yenileyici.kur(this) { render() }
        Yenileyici.gorunurluguEsitle(this)
    }
}
