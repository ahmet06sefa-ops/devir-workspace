package com.gunlukasistan.app

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Ana Sayfa: selamlama, istatistik kartları, hızlı erişim ve konu özeti.
 */
class HomeFragment : Fragment(R.layout.fragment_home) {

    private val turkish = Locale("tr", "TR")

    private val accentColors = intArrayOf(
        R.color.sage,
        R.color.caramel,
        R.color.terracotta,
        R.color.dusty_blue
    )
    private val accentEmojis = arrayOf("📚", "🧪", "📐", "🌍")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.chipNotes).setOnClickListener {
            (activity as? MainActivity)?.openNotes()
        }
        view.findViewById<View>(R.id.chipTasks).setOnClickListener {
            (activity as? MainActivity)?.openTasks()
        }
        view.findViewById<View>(R.id.chipAsistan).setOnClickListener {
            (activity as? MainActivity)?.openAsistan()
        }
        view.findViewById<View>(R.id.openBinMaddeAtolye)?.setOnClickListener {
            BinMaddeKontrolActivity.ac(requireContext())
        }
        // v11.11: Canva Çalışma Ekranı (10 Uygulama Arayüzü)
        view.findViewById<View>(R.id.openCanvaAtolye)?.setOnClickListener {
            CanvaCalismaAtolyeActivity.ac(requireContext())
        }
        // v11.04: Kişisel Gelişim ve Farkındalık Merkezi (Retroperspektif, Manifesto, SWOT, Derin Çalışma, Reset Günü)
        view.findViewById<View>(R.id.openKisiselGelisimAtolye)?.setOnClickListener {
            KisiselGelisimActivity.ac(requireContext())
        }
        // v10.92: 10.000-Madde Evrensel Görünüm ve Arayüz Atölyesi (#1..#10000)
        view.findViewById<View>(R.id.openGorunumAtolye)?.setOnClickListener {
            EvrenselGorunumActivity.ac(requireContext())
        }
        view.findViewById<View>(R.id.openSettings).setOnClickListener {
            (activity as? MainActivity)?.openSettings()
        }
        // v11.53: zamanlayıcı düğmesi — tıklayınca direkt sayaç ekranı açılır.
        // (Eski davranış: kısa dokunuş seçim penceresi açıyordu; kullanıcı
        // isteğiyle artık tek dokunuş sayaç ekranına gidiyor.)
        view.findViewById<View>(R.id.openTimerMenu).setOnClickListener {
            (activity as? MainActivity)?.openTimer()
        }
        view.findViewById<View>(R.id.openTimerMenu).setOnLongClickListener {
            (activity as? MainActivity)?.openTimer()
            true
        }
        // v10.47: Ana ekran sağ üst 🎛️ — Manuel Kontrol Merkezi
        view.findViewById<View>(R.id.openManuelKontrol)?.setOnClickListener {
            ManuelKontrolActivity.ac(requireContext())
        }
        // v10.48: Ana ekran sağ üst 🤖 — Otonom AI Ajanı ve Otopilot Merkezi
        view.findViewById<View>(R.id.openOtonomMerkez)?.setOnClickListener {
            OtonomMerkezActivity.ac(requireContext())
        }
        // v10.53: Ana ekran sağ üst 🎨 — 32 Maddelik Tasarım ve Yerleşim Atölyesi
        view.findViewById<View>(R.id.openTasarimAtolye)?.setOnClickListener {
            TasarimAtolyeActivity.ac(requireContext())
        }
        // v10.54: Ana ekran sağ üst 🏆 — Sesli Brifing ve Haftalık Verimlilik Karnesi
        view.findViewById<View>(R.id.openKarne)?.setOnClickListener {
            KarneActivity.ac(requireContext())
        }
        // v10.55: Ana ekran sağ üst 🧭 — 10 Özel Yaşam Modülü ve Manuel Kontrol Merkezi (#1..#10)
        view.findViewById<View>(R.id.openYasamModulleri)?.setOnClickListener {
            YasamModulleriActivity.ac(requireContext())
        }
        // v10.56: Ana ekran sağ üst 🚀 — C, D, E, G, H, I ve J Kategorileri Gelişmiş Hayat Atölyesi
        view.findViewById<View>(R.id.openGelismiAtolye)?.setOnClickListener {
            GelismiAtolyeActivity.ac(requireContext())
        }
        // v10.57: Ana ekran sağ üst 🔬 — Faz 2: C-D-E-G-H-I-J Uzman Modülleri & Özel Ekranlar
        view.findViewById<View>(R.id.openUzmanModuller)?.setOnClickListener {
            UzmanModullerActivity.ac(requireContext())
        }
        // v10.58: Ana ekran sağ üst 🎓 — 10 Uzman Öğrenme & Kolaylık Modülü (#1..#10 + 100 Öneri)
        view.findViewById<View>(R.id.openDersKolaylik)?.setOnClickListener {
            DersKolaylikActivity.ac(requireContext())
        }
        view.findViewById<View>(R.id.openDersIleriFaz)?.setOnClickListener {
            DersIleriFazActivity.ac(requireContext())
        }
        view.findViewById<View>(R.id.openDersUzmanMerkez)?.setOnClickListener {
            DersUzmanMerkezActivity.ac(requireContext())
        }
        view.findViewById<View>(R.id.openYasamSaglikFinans)?.setOnClickListener {
            YasamSaglikFinansActivity.ac(requireContext())
        }
        view.findViewById<View>(R.id.openDersUzmanFaz6)?.setOnClickListener {
            DersUzmanFaz6Activity.ac(requireContext())
        }
        view.findViewById<View>(R.id.openYasamSaglikFinansFaz3)?.setOnClickListener {
            YasamSaglikFinansFaz3Activity.ac(requireContext())
        }
        view.findViewById<View>(R.id.openEvrenselOtonomMerkez)?.setOnClickListener {
            EvrenselOtonomMerkezActivity.ac(requireContext())
        }
        view.findViewById<View>(R.id.openAkilliGundemMerkezi)?.setOnClickListener {
            AkilliGundemVeAsistanMerkeziActivity.ac(requireContext())
        }
        view.findViewById<View>(R.id.openNamazAylikYonetim)?.setOnClickListener {
            NamazAylikYonetimActivity.ac(requireContext())
        }
        atolyeButonlariniGuncelle(view)
        initMotivasyonManseti(view)
        GlassmorphismTemaMotoru.sekmeleriVeKartlariStille(view, requireContext())
        anaEkranKartlariniVeBoyutunuKur(view)
        // v9.9 · Görsel öneri 8: kaydırınca selamlama soluyor.
        //
        // CollapsingToolbarLayout'a geçmedim — ana ekran düz bir
        // ScrollView ve v8.5'teki blok sıralama sistemi ona bağlı.
        // Kaydırma dinleyicisi aynı hissi veriyor, hiçbir layout
        // değişmiyor. (Gerekçe: DaralanBaslik.kt başlığı)
        runCatching {
            DaralanBaslik.bagla(
                kaydirici = view as? android.widget.ScrollView,
                buyukBaslik = view.findViewById(R.id.greetingText),
                kucukBaslik = null,
                arkaPlan = null
            )
        }
        view.findViewById<View>(R.id.chipSoru).setOnClickListener { showQuestionsDialog() }
        view.findViewById<View>(R.id.chipDeneme).setOnClickListener {
            (activity as? MainActivity)?.openExams()
        }
        // 📅 ikonu: sınav tarihini değiştir · sayıya/başlığa dokun: geri sayım listesi
        view.findViewById<View>(R.id.countdownEdit).setOnClickListener { showExamDatePicker() }
        view.findViewById<View>(R.id.countdownNumber).setOnClickListener {
            (activity as? MainActivity)?.openEvents()
        }
        view.findViewById<View>(R.id.countdownCaption).setOnClickListener {
            (activity as? MainActivity)?.openEvents()
        }
        // v8.6 · Öneri 29: yatay/tablet genişlik sınırı + yoğunluk
        //
        // 🔴 v8.6'da bu satır ATLANMIŞTI: toplu ekleme betiği
        // `onViewCreated(` deseniyle arıyordu ama HomeFragment'ta
        // imza satırı farklı biçimlenmişti. 17 fragment'ın 16'sına
        // eklendi, ANA EKRAN eksik kaldı — en çok bakılan ekran.
        Duzen.uygula(view)
        // v8.5 · Öneri 16: kayıtlı kart sırası ve gizleme
        AnaEkranDuzen.uygula(requireContext(), view)
        // v10.18 · Ekran Atölyesi: blok basılı tut → yerinde düzenleme
        duzenSeridi = DuzenSeridi(requireContext(), anaKapi())
        duzenSeridi?.bagla(view)
        view.findViewById<View>(R.id.goalText).setOnClickListener { showGoalPicker() }
        view.findViewById<View>(R.id.quoteText).setOnClickListener { showQuoteEditor() }
        pofiyiKur(view)  // v10.8 · D43
        // v7.4: mühendislik kursları kartı
        view.findViewById<View>(R.id.coursesCard).setOnClickListener {
            (activity as? MainActivity)?.open(13)
        }

        // İstatistik kartları: dokun → ayrıntılı görünüm
        view.findViewById<View>(R.id.statCardStreak).setOnClickListener {
            StatsDetails.showStreak(requireContext())
        }
        view.findViewById<View>(R.id.statCardTotal).setOnClickListener {
            StatsDetails.showTotal(requireContext())
        }
        view.findViewById<View>(R.id.statCardProgress).setOnClickListener {
            StatsDetails.showProgress(requireContext())
        }
        view.findViewById<View>(R.id.statCardFocus).setOnClickListener {
            StatsDetails.showFocus(requireContext())
        }
    }

    private var duzenSeridi: DuzenSeridi? = null

    /** v10.18 — Ana ekranın yerinde düzenleme kapısı (saklama: AnaEkranDuzen). */
    private fun anaKapi(): DuzenSeridi.Kapi = object : DuzenSeridi.Kapi {
        override val bloklar get() = AnaEkranDuzen.bloklar
        override fun sira() = AnaEkranDuzen.sira(requireContext())
        override fun tasi(kod: String, yon: Int) =
            AnaEkranDuzen.tasi(requireContext(), kod, yon)
        override fun gizliMi(kod: String) = AnaEkranDuzen.gizliMi(requireContext(), kod)
        override fun gizle(kod: String, gizli: Boolean) =
            AnaEkranDuzen.gizle(requireContext(), kod, gizli)
        override fun boyutKademe(kod: String) =
            AnaEkranDuzen.boyutKademe(requireContext(), kod)
        override fun boyutYaz(kod: String, kademe: Int) =
            AnaEkranDuzen.setBoyutKademe(requireContext(), kod, kademe)
        override fun katliMi(kod: String) = AnaEkranDuzen.katliMi(requireContext(), kod)
        override fun katlaYaz(kod: String, katli: Boolean) =
            AnaEkranDuzen.setKatli(requireContext(), kod, katli)
        override fun sifirla() = AnaEkranDuzen.varsayilanaDon(requireContext())
        override fun kok(): View? = view
        override fun tazele() {
            view?.let { AnaEkranDuzen.uygula(requireContext(), it) }
            bindData()
        }
    }

    private val streakGizleyici = Runnable {
        view?.findViewById<android.view.View>(R.id.statCardStreak)?.visibility = android.view.View.GONE
    }

    override fun onResume() {
        super.onResume()
        // v8.5: düzenleyiciden dönülmüş olabilir — sıra/gizleme tazelensin
        view?.let {
            AnaEkranDuzen.uygula(requireContext(), it)
            atolyeButonlariniGuncelle(it)
            TabloBaslikYonetimMotoru.basliklariUygula(
                requireContext(),
                it.findViewById(R.id.txtHomeGunlukIlerlemeBaslik),
                it.findViewById(R.id.txtHomeGridBaslik),
                it.findViewById(R.id.txtHomeHizliErisimBaslik),
                it.findViewById(R.id.txtHomeKonularimBaslik)
            )
            val streakCard = it.findViewById<android.view.View>(R.id.statCardStreak)
            streakCard?.visibility = android.view.View.VISIBLE
            streakCard?.removeCallbacks(streakGizleyici)
            streakCard?.postDelayed(streakGizleyici, 3000L) // v10.87: gün seriniz yazısı anlık gösterilip kaybolsun
        }
        bindData()
        // v9.9 · Öz denetimde bulunan boşluk:
        //
        // Ekranlar `hide()`/`show()` ile yönetiliyor. Başka bir
        // sekmeye gidip döndüğünde ScrollView'ın kaydırma konumu
        // KORUNUYOR ama kaydırma dinleyicisi tetiklenmiyor.
        // Sonuç: aşağıda bırakıp dönünce selamlama solmuş
        // kalıyordu (alfa 0) ve bir daha görünmüyordu.
        runCatching {
            DaralanBaslik.tazele(
                kaydirici = view as? android.widget.ScrollView,
                buyukBaslik = view?.findViewById(R.id.greetingText)
            )
        }
        // v10.14 · E25: uyandım sonrası sabah planı diyaloğu (tek seferlik)
        runCatching { SabahPlani.maybeGoster(requireActivity()) }
        // v10.14 · E30: Aralık'ta senenin filmi önerisi (yılda bir)
        runCatching { SeneFilmi.aralikOnerisi(requireActivity()) }
    }

    /** Dışarıdan (örn. + menüsünden) tazelemek için. */
    fun refreshData() {
        if (view != null) bindData()
    }

    // ---------------- v10.8 · D43: Pofi (uygulama maskotu) ----------------

    /**
     * Oturumluk dokunma kayması: aynı gün içinde ardışık dokunuşlar
     * sıradaki mesajı verir; gün değişince dizi tekrar başa döner.
     */
    private var pofirKayma = 0

    /** Renk/etkileşim kurulumu — temaya giydirme tek seferlik. */
    private fun pofiyiKur(view: View) {
        val satir = view.findViewById<View>(R.id.maskotSatir) ?: return
        val mView = view.findViewById<MaskotView>(R.id.maskotView) ?: return
        val ctx = requireContext()
        fun renk(attr: Int, yedek: Int) =
            com.google.android.material.color.MaterialColors.getColor(ctx, attr, yedek)
        mView.renkleriAyarla(
            govde = renk(com.google.android.material.R.attr.colorPrimary, 0xFF7C5CBF.toInt()),
            cizgi = renk(com.google.android.material.R.attr.colorOnPrimary, 0xFFFFFFFF.toInt()),
            zemin = renk(com.google.android.material.R.attr.colorPrimaryContainer, 0x337C6BF5)
        )
        // Dokun: Pofi başka bir şey söylesin
        satir.setOnClickListener {
            pofirKayma++
            Titresim.dokunus(satir)
            pofirMesajYaz()
        }
        // Uzun bas: v10.11 · A5 gardırop (tanıtım diyalogun içinde yaşar)
        satir.setOnLongClickListener {
            pofiGardropAc(ctx, mView)
            true
        }
    }

    /** v10.11 · ULTRA-30 A3: gün ışığı şeridini veriyle besler. */
    private fun gunIsiginiYenile(context: android.content.Context) {
        val v = view ?: return
        val blok = v.findViewById<View>(R.id.gunIsigiBlok) ?: return
        val serit = v.findViewById<GunIsigiView>(R.id.gunIsigiView) ?: return
        val metin = v.findViewById<TextView>(R.id.gunIsigiMetin) ?: return

        val simdi = java.util.Calendar.getInstance().let {
            it.get(java.util.Calendar.HOUR_OF_DAY) * 60 + it.get(java.util.Calendar.MINUTE)
        }
        val (dogus, batis) = runCatching {
            val gun = NamazVakti.bugun(context)
            gun.dakika(NamazVakti.Vakit.GUNES) to gun.dakika(NamazVakti.Vakit.AKSAM)
        }.getOrDefault(-1 to -1)
        if (dogus < 0 || batis <= dogus) {
            blok.visibility = View.GONE
            return
        }
        blok.visibility = View.VISIBLE
        val yuzde = GunIsigiView.yuzde(dogus, batis, simdi)
        val vurgu = com.google.android.material.color.MaterialColors.getColor(
            context, com.google.android.material.R.attr.colorPrimary, 0xFFE69F00.toInt()
        )
        val zemin = (
            com.google.android.material.color.MaterialColors.getColor(
                context, com.google.android.material.R.attr.colorOnSurface, 0
            ) and 0x00FFFFFF
            ) or 0x1A000000
        serit.ayarla(yuzde, vurgu, zemin)
        metin.text = if (yuzde >= 0) {
            getString(
                R.string.ax_gi_kararmaya,
                UykuCerceve.sureKisa((batis - simdi) * 60_000L)
            )
        } else {
            getString(R.string.ax_gi_gece, UykuCerceve.saatMetni(dogus))
        }
    }

    /** v10.11 · ULTRA-30 A5: gardırop diyaloğu (açık aksesuarlar giydirilir). */
    private fun pofiGardropAc(ctx: android.content.Context, mView: MaskotView) {
        val d = resources.displayMetrics.density
        fun dp(x: Int) = (x * d).toInt()
        val giris = MaskotGardrop.girisTopla(ctx)

        val satirlar = listOf(
            listOf(MaskotGardrop.BERE, getString(R.string.ax_gard_bere), getString(R.string.ax_gard_bere_sart)),
            listOf(MaskotGardrop.GOZLUK, getString(R.string.ax_gard_gozluk), getString(R.string.ax_gard_gozluk_sart)),
            listOf(MaskotGardrop.ESARP, getString(R.string.ax_gard_esarp), getString(R.string.ax_gard_esarp_sart)),
            listOf(MaskotGardrop.TAC, getString(R.string.ax_gard_tac), getString(R.string.ax_gard_tac_sart))
        )
        val govde = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(6), 0, dp(6), 0)
        }
        satirlar.forEach { parca ->
            val anahtar = parca[0]
            val acik = MaskotGardrop.acikMi(anahtar, giris)
            val satir = TextView(ctx).apply {
                textSize = 15f
                setPadding(dp(8), dp(10), dp(8), 0)
            }
            val durum = TextView(ctx).apply {
                textSize = 12f
                setPadding(dp(8), dp(1), dp(8), dp(10))
            }
            fun yaz() {
                val giyili = MaskotGardrop.giyilenMi(ctx, anahtar)
                satir.text = parca[1] + (if (giyili) " ✓" else "")
                durum.text = if (!acik) {
                    "🔒 " + parca[2]
                } else if (giyili) {
                    getString(R.string.ax_gard_giyili)
                } else {
                    getString(R.string.ax_gard_acik_dokun)
                }
                durum.alpha = if (acik) 0.8f else 0.5f
                satir.alpha = if (acik) 1f else 0.45f
            }
            yaz()
            val kutu = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                if (acik) {
                    isClickable = true
                    setOnClickListener {
                        Titresim.dokunus(it)
                        MaskotGardrop.giy(ctx, anahtar, !MaskotGardrop.giyilenMi(ctx, anahtar))
                        mView.setAksesuarlar(MaskotGardrop.giyilenler(ctx))
                        yaz()
                    }
                }
                addView(satir)
                addView(durum)
            }
            govde.addView(kutu)
        }
        com.google.android.material.dialog.MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.ax_gard_baslik)
            .setView(govde)
            .setPositiveButton(R.string.ok, null)
            .setNeutralButton(R.string.pf_tani_baslik) { _, _ -> pofiTanitim(ctx) }
            .show()
    }

    /** Eski uzun-bas içeriği unutulmadı — gardrobun içinden de ulaşılır. */
    private fun pofiTanitim(ctx: android.content.Context) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.pf_tani_baslik)
            .setMessage(getString(R.string.pf_tani_metin, Maskot.ALEV_ESIK))
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    /** Ruh haline göre yüz + mesaj (bindData'dan ve etkileşimden çağrılır). */
    private fun maskotuDuzenle(context: android.content.Context) {
        val v = view ?: return
        val mView = v.findViewById<MaskotView>(R.id.maskotView) ?: return
        val (seri, _) = Store.streakInfo(context)
        // Mola sayılması: pomodoro molada ya da zincirin odaksız evresi
        // koşuyor olmalı. Zincir ısınma/yürüyüş de bu gruba girer.
        val molada = (Pomodoro.acikMi(context) && Pomodoro.molada(context)) ||
            (SayacZincir.kosuyor(context) && SayacZincir.aktif(context)?.let {
                !SayacZincir.adimdaki(it, SayacZincir.adim(context)).odakMi
            } == true)
        val girdi = Maskot.Girdi(
            saat = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            seriGun = seri,
            sayacCalisiyor = TimerEngine.isRunning(context),
            molada = molada,
            bugunOdakDk = Store.getTodayFocusMinutes(context)
        )
        mView.guncelle(Maskot.ruhHali(girdi))
        // v10.11 · A5 + A3: giyilen aksesuarlar ve gün ışığı şeridi
        mView.setAksesuarlar(MaskotGardrop.giyilenler(context))
        gunIsiginiYenile(context)
        pofirMesajYaz()
    }

    private fun pofirMesajYaz() {
        val v = view ?: return
        val mesajView = v.findViewById<TextView>(R.id.maskotMesaj) ?: return
        val mView = v.findViewById<MaskotView>(R.id.maskotView) ?: return
        val diziRes = when (mView.ruh) {
            Maskot.Ruh.ODAKLI -> R.array.pf_mesaj_odakli
            Maskot.Ruh.MOLADA -> R.array.pf_mesaj_molada
            Maskot.Ruh.ALEV -> R.array.pf_mesaj_alev
            Maskot.Ruh.GURURLU -> R.array.pf_mesaj_gururlu
            Maskot.Ruh.UYKULU -> R.array.pf_mesaj_uykulu
            Maskot.Ruh.NESHALI -> R.array.pf_mesaj_neshali
        }
        val adaylar = resources.getStringArray(diziRes)
        val gunNo = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        mesajView.text = adaylar.getOrElse(
            Maskot.mesajSira(gunNo, pofirKayma, adaylar.size)
        ) { adaylar.first() }
        mView.contentDescription = getString(
            when (mView.ruh) {
                Maskot.Ruh.ODAKLI -> R.string.pf_ruh_odakli
                Maskot.Ruh.MOLADA -> R.string.pf_ruh_molada
                Maskot.Ruh.ALEV -> R.string.pf_ruh_alev
                Maskot.Ruh.GURURLU -> R.string.pf_ruh_gururlu
                Maskot.Ruh.UYKULU -> R.string.pf_ruh_uykulu
                Maskot.Ruh.NESHALI -> R.string.pf_ruh_neshali
            }
        )
    }

    // ---------------- Kahraman kart (geri sayım, hedef, söz, rozetler) ----------------

    private val dailyQuotes = arrayOf(
        "Başlamak, bitirmenin yarısıdır. 🌱",
        "Bugünün 25 dakikası, yarının 5 neti demek. 🔥",
        "Disiplin, motivasyonun bittiği yerde devam eder. 💪",
        "Küçük adımlar büyük sınavlar kazandırır. 🎯",
        "Rakiplerin uyurken sen bir Pomodoro daha. ⏰",
        "Netler, tekrar ettikçe yükselir. 📈",
        "Vazgeçmek yok — sadece yavaşlamak serbest. 🚶",
        "Her deneme bir prova, asıl sahne Eylül'de. 🎓",
        "Bugün çözmediğin soru, sınavda karşına çıkar. 😄✍️",
        "Planlı çalışan, şanslı olana gerek duymaz. 🗓️"
    )

    private fun bindHero(context: android.content.Context) {
        val view = view ?: return
        // Geri sayım — sabitlenmiş/yaklaşan kişisel etkinlik varsa onu, yoksa sınav tarihini göster
        val numberView = view.findViewById<TextView>(R.id.countdownNumber)
        val captionView = view.findViewById<TextView>(R.id.countdownCaption)
        val highlight = Store.highlightEvent(context)
        if (highlight != null) {
            val left = highlight.daysLeft
            numberView.text = when {
                left > 0 -> "${highlight.emoji} $left gün"
                left == 0 -> "${highlight.emoji} Bugün!"
                else -> "${highlight.emoji} geçti"
            }
            captionView.text = "${highlight.title} · tüm geri sayımlar için dokun"
        } else {
            val examMillis = Store.getExamDateMillis(context)
            if (examMillis <= 0L) {
                numberView.text = "📅 ?"
            } else {
                val days = ((examMillis - System.currentTimeMillis()) / 86_400_000L).toInt() + 1
                numberView.text = if (days > 0) {
                    getString(R.string.cd_days_left, days)
                } else {
                    getString(R.string.cd_exam_now)
                }
            }
            captionView.setText(R.string.cd_caption)
        }

        // v8.3 · Öneri 14: kahraman kart degradesi.
        //
        // Aciliyet, kalan güne göre 0..1 arasında. Eşik 45 gün:
        // ondan uzaksa kart sakin, yaklaştıkça degradenin alt ucu
        // uyarı rengine kayıyor. Sayıya bakmadan da hissediliyor.
        runCatching {
            val kalanGun = when {
                highlight != null -> highlight.daysLeft
                else -> {
                    val ms = Store.getExamDateMillis(context)
                    if (ms <= 0L) 999
                    else ((ms - System.currentTimeMillis()) / 86_400_000L).toInt() + 1
                }
            }
            val aciliyet = when {
                kalanGun < 0 -> 0f                       // geçmiş: sakin
                kalanGun == 0 -> 1f                      // bugün: en yüksek
                kalanGun >= 45 -> 0f
                else -> 1f - (kalanGun / 45f)
            }
            view.findViewById<DegradeArka>(R.id.heroDegrade)?.apply {
                kose = 20 * resources.displayMetrics.density
                this.aciliyet = aciliyet
            }
            view.findViewById<View>(R.id.heroDegrade)?.parent?.let { parentView ->
                (parentView as? View)?.visibility = if (GorunumAyar.heroGizliMi(requireContext())) View.GONE else View.VISIBLE
            }
        }

        // Günlük hedef çubuğu (odak dk + soru)
        val goal = Store.getGoalMinutes(context)
        val todayFocus = Store.getTodayFocusMinutes(context)
        val todayQ = Store.getTodayQuestions(context)
        Canlandir.cubuk(
            view.findViewById<LinearProgressIndicator>(R.id.goalBar),
            (todayFocus * 100 / goal).coerceIn(0, 100)
        )
        view.findViewById<TextView>(R.id.goalText).text =
            getString(R.string.goal_today_format, todayFocus, goal, todayQ)
        // Günün sözü
        val custom = Store.getQuote(context)
        val quote = custom.ifEmpty {
            val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            dailyQuotes[dayOfYear % dailyQuotes.size]
        }
        view.findViewById<TextView>(R.id.quoteText).text = "🌟 $quote"

        // v7.4: kurs kartı özeti
        try {
            val lessons = Store.loadLessons(requireContext())
            val doneCount = lessons.count { it.done }
            val sub = view.findViewById<TextView>(R.id.coursesCardSub)
            val bar = view.findViewById<View>(R.id.coursesCardBar)
            val barBg = view.findViewById<View>(R.id.coursesCardBarBg)
            if (lessons.isEmpty()) {
                sub.text = getString(R.string.co_card_empty)
                bar.layoutParams = bar.layoutParams.apply { width = 0 }
            } else {
                val pct = doneCount * 100 / lessons.size
                // v7.12: sıradaki dersi göster
                val sonraki = Store.sonDers(requireContext())
                sub.text = if (sonraki != null && !sonraki.done) {
                    getString(R.string.co_card_next, pct, sonraki.title.take(28))
                } else {
                    getString(R.string.co_card, doneCount, lessons.size, pct)
                }
                bar.setBackgroundColor(ThemeManager.NEON_PALETTE[2])
                barBg.post {
                    bar.layoutParams = bar.layoutParams.apply {
                        width = barBg.width * pct / 100
                    }
                }
            }
        } catch (_: Exception) {
        }
        // v9.9 · Görsel öneri 7: günün tek odağı
        bindOdak(context)
        // v10.51 #9: Ana Sayfa Ekranı (özet şerit & kompakt konular)
        if (GorunumAyar.ozetSeridModu(context)) {
            view?.findViewById<TextView>(R.id.coursesCardSub)?.text = "Tüm konuları gör ›"
        }
        // Rozet şeridi
        bindBadges(context)
    }

    /**
     * v9.9 — Hero karttaki "bugün ne yapmalıyım" satırı.
     *
     * ══════════════════════════════════════════════════════════
     * NEDEN ARKA PLANDA DEĞİL
     * ══════════════════════════════════════════════════════════
     * `GunOdak.bul` disk okuyor (görevler + takip + tekrar). Ama
     * `bindHero` zaten `Store.loadTasks` benzeri çağrılar yapıyor
     * ve bunlar `Onbellek` üzerinden geliyor — ikinci okuma
     * bellekten dönüyor.
     *
     * Arka plana atsaydım satır 100-200 ms sonra belirir, kart
     * "zıplardı". Hero kartın ilk karede tam görünmesi daha önemli.
     *
     * ── Yapılacak bir şey yoksa ne oluyor ──
     * Satır GİZLENİYOR. Boş bir "önerin yok" satırı göstermek
     * kartı gereksiz uzatır ve hiçbir şey söylemez.
     */
    private fun bindOdak(context: android.content.Context) {
        val view = view ?: return
        val satir = view.findViewById<View>(R.id.odakSatir) ?: return

        val odak = runCatching { GunOdak.bul(context) }.getOrNull()
        if (odak == null) {
            satir.visibility = View.GONE
            return
        }

        satir.visibility = View.VISIBLE
        view.findViewById<TextView>(R.id.odakEmoji)?.text = odak.emoji
        view.findViewById<TextView>(R.id.odakMetin)?.text = odak.metin

        // Aciliyet şeridi — renk anlamı GrafikDili'nden (tek kaynak)
        // v10.0: tema duyarlı — koyu temada açılmış tonlar
        val renk = when (odak.aciliyet) {
            2 -> GrafikDili.hata(context)
            1 -> GrafikDili.uyari(context)
            else -> GrafikDili.basari(context)
        }
        view.findViewById<View>(R.id.odakSerit)?.setBackgroundColor(renk)

        // Erişilebilirlik: tek dokunuşla ne olacağı okunabilmeli
        satir.contentDescription = "${odak.metin} — ${getString(R.string.od_baslik)}"

        satir.setOnClickListener {
            Titresim.dokunus(it)
            // NOT: bu blok bilinçli olarak `runCatching { }` içinde
            // DEĞİL. Kotlin `runCatching`'in son ifadesini DEĞER
            // olarak görüyor; içindeki `when` ve `if` exhaustive
            // olmak zorunda kalıyor ve okunmaz hâle geliyor.
            // `try/catch` böyle bir kısıt getirmiyor.
            try {
                val hedef = odak.aktivite
                if (hedef != null) {
                    // v9.9 · Görsel öneri 9: satırdan büyüyerek açılsın
                    val acildi = KartAcilis.ac(it, hedef)
                    if (!acildi) {
                        startActivity(android.content.Intent(requireContext(), hedef))
                        Canlandir.activityGirisi(activity)
                    }
                } else if (odak.ekranIndeksi >= 0) {
                    (activity as? MainActivity)?.open(odak.ekranIndeksi)
                }
            } catch (e: Exception) {
                android.util.Log.w("HomeFragment", "Odak açılamadı", e)
            }
        }
    }

    private fun bindBadges(context: android.content.Context) {
        val view = view ?: return
        val row = view.findViewById<LinearLayout>(R.id.badgesRow)
        row.removeAllViews()
        val dp = resources.displayMetrics.density
        val earnedIds = Badges.earned(context).map { it.id }.toSet()
        val ordered = Badges.all.sortedByDescending { it.id in earnedIds }
        ordered.take(8).forEach { badge ->
            val earned = badge.id in earnedIds
            val chip = TextView(context).apply {
                text = (if (earned) badge.emoji else "🔒") + " " + badge.title
                textSize = 11.5f
                alpha = if (earned) 1f else 0.55f
                setPadding((12 * dp).toInt(), (8 * dp).toInt(), (12 * dp).toInt(), (8 * dp).toInt())
                background = GradientDrawable().apply {
                    cornerRadius = 16 * dp
                    if (earned) {
                        setColor(
                            com.google.android.material.color.MaterialColors.getColor(
                                context,
                                com.google.android.material.R.attr.colorSecondaryContainer,
                                0xFFE4E9DC.toInt()
                            )
                        )
                    } else {
                        setColor(0x11000000)
                    }
                }
                setTextColor(
                    com.google.android.material.color.MaterialColors.getColor(
                        context,
                        com.google.android.material.R.attr.colorOnSurface,
                        0xFF888888.toInt()
                    )
                )
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = (8 * dp).toInt() }
                setOnClickListener { showBadgesDialog() }
            }
            row.addView(chip)
        }
    }

    private fun showBadgesDialog() {
        val context = requireContext()
        val earnedIds = Badges.earned(context).map { it.id }.toSet()
        val sb = StringBuilder()
        sb.append(getString(R.string.badges_progress, earnedIds.size, Badges.all.size)).append("\n\n")
        Badges.all.forEach { b ->
            val mark = if (b.id in earnedIds) b.emoji else "🔒"
            sb.append("$mark ${b.title} — ${b.desc}\n")
        }
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.badges_title)
            .setMessage(sb.toString().trim())
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    // ---------------- Küçük diyaloglar ----------------

    private fun showQuestionsDialog() {
        val context = requireContext()
        val picker = NumberPicker(context).apply {
            minValue = 5
            maxValue = 500
            value = 50
        }
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.q_dialog_title)
            .setView(picker)
            .setPositiveButton(R.string.add) { _, _ ->
                Store.addQuestions(context, picker.value)
                Toast.makeText(
                    context,
                    getString(R.string.q_added, picker.value, Store.getTodayQuestions(context)),
                    Toast.LENGTH_LONG
                ).show()
                bindData()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showExamDatePicker() {
        val context = requireContext()
        val cal = Calendar.getInstance().apply {
            val m = Store.getExamDateMillis(context)
            if (m > 0) timeInMillis = m
        }
        DatePickerDialog(
            context,
            { _, y, m, d ->
                Store.setExamDate(context, String.format(Locale.US, "%04d%02d%02d", y, m + 1, d))
                bindData()
                Toast.makeText(context, R.string.cd_saved, Toast.LENGTH_SHORT).show()
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showGoalPicker() {
        val context = requireContext()
        val picker = NumberPicker(context).apply {
            val steps = (25..300 step 25).toList()
            displayedValues = steps.map { "$it dk" }.toTypedArray()
            minValue = 0
            maxValue = steps.size - 1
            value = steps.indexOf(Store.getGoalMinutes(context)).coerceAtLeast(1)
        }
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.goal_set_title)
            .setView(picker)
            .setPositiveButton(R.string.save) { _, _ ->
                val minutes = (picker.value + 1) * 25
                Store.setGoalMinutes(context, minutes)
                bindData()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showQuoteEditor() {
        val context = requireContext()
        val input = EditText(context).apply {
            setText(Store.getQuote(context))
            hint = getString(R.string.quote_hint)
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.quote_edit_title)
            .setView(input)
            .setPositiveButton(R.string.save) { _, _ ->
                Store.setQuote(context, input.text.toString().trim())
                bindData()
            }
            .setNeutralButton(R.string.quote_reset) { _, _ ->
                Store.setQuote(context, "")
                bindData()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun bindData() {
        val view = view ?: return
        val context = requireContext()
        bindHero(context)
        maskotuDuzenle(context)  // v10.8 · D43

        // ---- Selamlama ----
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greetingRes = when (hour) {
            in 5..11 -> R.string.greet_morning
            in 12..17 -> R.string.greet_day
            in 18..22 -> R.string.greet_evening
            else -> R.string.greet_night
        }
        view.findViewById<TextView>(R.id.greetingText).setText(greetingRes)
        view.findViewById<TextView>(R.id.dateText).text =
            SimpleDateFormat("d MMMM, EEEE", turkish).format(Date())

        // ---- İstatistik kartları ----
        val palette = ThemeManager.NEON_PALETTE
        val (currentStreak, bestStreak) = Store.streakInfo(context)
        // v8.2 · Öneri 3: sayılar 0'dan hedefe sayarak yazılır.
        // Eskiden dört istatistik de anında basılıyordu; ana ekran
        // açılışı ölü bir tabloya benziyordu.
        Canlandir.sayi(view.findViewById(R.id.streakValue), currentStreak) {
            getString(R.string.stat_streak_inline, it, bestStreak)
        }
        view.findViewById<StatRingView>(R.id.ringStreak).apply {
            icon = "🔥"; ringColor = palette[0]
            Canlandir.halka(this, (currentStreak * 100 / 30).coerceIn(0, 100))
        }

        val totalDone = Store.allTimeCompletions(context)
        Canlandir.sayi(view.findViewById(R.id.totalValue), totalDone)
        view.findViewById<StatRingView>(R.id.ringTotal).apply {
            icon = "✅"; ringColor = palette[1]
            Canlandir.halka(this, (totalDone * 100 / 500).coerceIn(0, 100))
        }

        val topics = Store.loadTopics(context)
        val allSubs = topics.flatMap { it.items }
        val overallPercent = if (allSubs.isEmpty()) 0 else {
            allSubs.count { it.done } * 100 / allSubs.size
        }
        Canlandir.sayi(view.findViewById(R.id.progressValue), overallPercent) {
            getString(R.string.percent_format, it)
        }
        view.findViewById<StatRingView>(R.id.ringProgress).apply {
            icon = "📈"; ringColor = palette[2]
            Canlandir.halka(this, overallPercent)
        }

        val now = Calendar.getInstance()
        val thisMonthFocus = Store.monthFocus(context, now.get(Calendar.YEAR), now.get(Calendar.MONTH))
        Canlandir.sayi(view.findViewById(R.id.focusValue), thisMonthFocus)
        view.findViewById<StatRingView>(R.id.ringFocus).apply {
            icon = "⏱️"; ringColor = palette[4]
            Canlandir.halka(this, (thisMonthFocus * 100 / 1500).coerceIn(0, 100))
        }



        // ---- v6.1: Günlük ilerleme grafiği ----
        bindChart(context)
        // ---- v6.1: Haftalık alışkanlık ızgarası ----
        bindGrid(context)

        // ---- Konu özeti listesi ----
        val container = view.findViewById<LinearLayout>(R.id.topicsContainer)
        container.removeAllViews()
        val emptyText = view.findViewById<TextView>(R.id.homeTopicsEmpty)

        if (topics.isEmpty()) {
            emptyText.visibility = View.VISIBLE
        } else {
            emptyText.visibility = View.GONE
            val inflater = LayoutInflater.from(context)
            topics.take(4).forEachIndexed { index, topic ->
                val row = inflater.inflate(R.layout.item_home_topic, container, false)

                val accent = ContextCompat.getColor(
                    context, accentColors[index % accentColors.size]
                )
                row.findViewById<View>(R.id.accentBar).setBackgroundColor(accent)
                row.findViewById<TextView>(R.id.topicEmoji).text =
                    accentEmojis[index % accentEmojis.size]
                row.findViewById<TextView>(R.id.homeTopicTitle).text = topic.title

                row.findViewById<TextView>(R.id.homeTopicCaption).text = if (topic.items.isEmpty()) {
                    getString(R.string.no_subtopics)
                } else {
                    getString(R.string.completed_of, topic.doneCount, topic.items.size)
                }
                row.findViewById<TextView>(R.id.homeTopicPercent).text =
                    getString(R.string.percent_format, topic.percent)

                row.setOnClickListener { (activity as? MainActivity)?.openTopics() }
                container.addView(row)
            }

            if (topics.size > 4) {
                val more = inflater.inflate(R.layout.item_subtopic_add, container, false) as TextView
                more.setText(R.string.show_all_topics)
                more.setOnClickListener { (activity as? MainActivity)?.openTopics() }
                container.addView(more)
            }
        }
    }
    // ---------------- v6.1: Grafik ve ızgara ----------------

    /** Son 21 günün aktivite eğrisini çizer, haftalık değişimi hesaplar. */
    private fun bindChart(context: android.content.Context) {
        val view = view ?: return
        val chart = view.findViewById<SparklineView>(R.id.dailyChart) ?: return
        val data = Store.dailyTrend(context, 21)
        chart.lineColor = ThemeManager.NEON_PALETTE[0]
        chart.setData(data)
        chart.isClickable = true
        chart.isFocusable = true
        chart.setOnClickListener {
            gunlukIlerlemeGrafigiAnaliziGoster(context)
        }

        // Son 7 gün ile önceki 7 günü karşılaştır
        val summary = view.findViewById<TextView>(R.id.chartSummary) ?: return
        if (data.size >= 14) {
            val recent = data.takeLast(7).sum()
            val previous = data.subList(data.size - 14, data.size - 7).sum()
            summary.text = when {
                previous <= 0.01f && recent > 0f -> getString(R.string.chart_trend_up, 100)
                previous <= 0.01f -> getString(R.string.chart_trend_flat)
                else -> {
                    val change = ((recent - previous) / previous * 100f).toInt()
                    when {
                        change > 2 -> getString(R.string.chart_trend_up, change)
                        change < -2 -> getString(R.string.chart_trend_down, -change)
                        else -> getString(R.string.chart_trend_flat)
                    }
                }
            }
        } else {
            summary.text = ""
        }
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

    /** Alışkanlıkların son 4 haftalık ✓ ızgarasını doldurur. */
    private fun bindGrid(context: android.content.Context) {
        val view = view ?: return
        val grid = view.findViewById<HabitGridView>(R.id.habitGrid) ?: return
        val empty = view.findViewById<TextView>(R.id.gridEmpty) ?: return

        // Tüm aktif alışkanlıklar listelenir (v6.2: eskiden 5 ile sınırlıydı)
        val habits = Store.loadHabits(context).filterNot { it.archived }
        if (habits.isEmpty()) {
            grid.visibility = View.GONE
            empty.visibility = View.VISIBLE
            return
        }
        grid.visibility = View.VISIBLE
        empty.visibility = View.GONE

        grid.setRows(
            habits.map { habit ->
                HabitGridView.Row(
                    emoji = habit.emoji,
                    title = habit.title,
                    weeks = Store.habitWeeks(context, habit),
                    percent = Store.habitRate(context, habit, 28)
                )
            }
        )
    }


    /**
     * v7.58: Üstten aşağı çekince yenileme.
     * onViewCreated yerine onStart'ta kuruluyor — görünüm ağacı
     * o an ebeveynine bağlanmış oluyor, sarmalama güvenli.
     */
    override fun onStart() {
        super.onStart()
        Yenileyici.kur(this) { refreshData() }
        Yenileyici.gorunurluguEsitle(this)
    }

    // v10.59: Ana ekrandaki atölye/modül butonlarını açma/kapama kuralını uygula
    private fun atolyeButonlariniGuncelle(view: View) {
        // v11.13: İlk açılıştaki atölye butonları kalıcı olarak kaldırıldı.
        // Ana ekranda yalnızca temel butonlar (openTimerMenu, openSettings)
        // kalır; tüm atölye/modül kısayol butonları GONE yapılır.
        for (id in AnaEkranButonKarari.ATOLYE_BUTON_IDLERI) {
            val resId = resources.getIdentifier(id, "id", requireContext().packageName)
            if (resId != 0) {
                view.findViewById<View>(resId)?.visibility = View.GONE
            }
        }
    }

    private fun initMotivasyonManseti(view: View) {
        val card = view.findViewById<View>(R.id.cardMotivasyonManset) ?: return
        val goster = MotivasyonMansetMotoru.mansetGosterilsinMi(requireContext())
        card.visibility = MotivasyonMansetMotoru.gorunurlukKarari(goster)
        if (!goster) return

        val txtSoz = view.findViewById<TextView>(R.id.txtMotivasyonSozu) ?: return
        fun yaz() {
            val soz = MotivasyonMansetMotoru.aktifSozuGetir(requireContext())
            txtSoz.text = MotivasyonMansetMotoru.sozMetniFormatla(soz)
        }
        yaz()

        view.findViewById<View>(R.id.btnMotivasyonYenile)?.setOnClickListener {
            MotivasyonMansetMotoru.sonrakiIndexeGec(requireContext())
            yaz()
            Toast.makeText(requireContext(), "📜 Yeni Motivasyon Sözü Yüklendi!", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.btnMotivasyonSabitle)?.setOnClickListener {
            val input = android.widget.EditText(requireContext()).apply {
                hint = "Örn: Hedef 450 Puan — Vazgeçmek Yok!"
            }
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("📌 Kişisel Motto Yaz veya Sabitle (#12)")
                .setView(input)
                .setPositiveButton("Sabitle") { _, _ ->
                    val metin = input.text.toString()
                    MotivasyonMansetMotoru.kisiselSozuKaydet(requireContext(), metin, "Kişisel Motto")
                    yaz()
                    Toast.makeText(requireContext(), "📌 Motto Sabitlendi!", Toast.LENGTH_SHORT).show()
                }
                .setNeutralButton("Sabitlemeyi Kaldır") { _, _ ->
                    MotivasyonMansetMotoru.sabitlemeyiKaldir(requireContext())
                    yaz()
                    Toast.makeText(requireContext(), "Sabitleme Kaldırıldı, Döngüye Dönüldü.", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("İptal", null)
                .show()
        }

        view.findViewById<View>(R.id.btnMotivasyonPaylas)?.setOnClickListener {
            val soz = MotivasyonMansetMotoru.aktifSozuGetir(requireContext())
            val metin = MotivasyonMansetMotoru.sozMetniFormatla(soz)
            val cm = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            cm?.setPrimaryClip(android.content.ClipData.newPlainText("Motivasyon Sozu", metin))
            Toast.makeText(requireContext(), "↗️ Motivasyon Sözü Kopyalandı!", Toast.LENGTH_SHORT).show()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // v11.07: GÜNÜN AKIŞI YERİNİ VB YERLERİN BOYUTUNU / SIRASINI AYARLAMA
    // ═══════════════════════════════════════════════════════════════
    private fun anaEkranKartlariniVeBoyutunuKur(kok: View) {
        val kartlar = listOf(
            Pair(kok.findViewById<View>(R.id.blokHero), "☀️ Günün Akışı"),
            Pair(kok.findViewById<View>(R.id.cardMotivasyonManset), "💡 Motivasyon Manşeti"),
            Pair(kok.findViewById<View>(R.id.coursesCard), "🎓 Kurs ve Atölyeler"),
            Pair(kok.findViewById<View>(R.id.gridCard), "📊 Modüller ve Özet")
        )
        kartlar.forEach { (v, ad) ->
            v?.setOnLongClickListener {
                anaEkranKartMenusu(ad, v)
                true
            }
        }
        anaEkranSiraVeBoyutUygula(kok)
    }

    private fun anaEkranKartMenusu(kartAdi: String, view: View) {
        val ctx = context ?: return
        val secenekler = arrayOf(
            "⬆️ Bu Kartı Yukarı Taşı (Ana Ekranda Sırasını Değiştir)",
            "⬇️ Bu Kartı Aşağı Taşı (Ana Ekranda Sırasını Değiştir)",
            "⚡ Bu Kartı / Veriyi Başka Bir Sekmeye Taşı veya Kopyala (Bugün ⇄ Konular ⇄ İlerleme ⇄ Plan)",
            "📐 Günün Akışı & Kart Boyutu Ölçeğini Değiştir (${GorunumAyar.kartBoyutuAd(ctx)})",
            "🚀 İlk Açılış Ekranını Seç (${GorunumAyar.acilisEkranAd(ctx)})",
            "✨ Ana Ekran Sıralamasını ve Boyutlarını Varsayılana Sıfırla"
        )
        com.google.android.material.dialog.MaterialAlertDialogBuilder(ctx)
            .setTitle("🔀 '$kartAdi' — Kart Yönetimi & Boyut")
            .setItems(secenekler) { _, idx ->
                when (idx) {
                    0 -> kartSirasiDegistir(view, true)
                    1 -> kartSirasiDegistir(view, false)
                    2 -> SekmeVeVeriTasimaMotoru.sekmeArasiTasimaDiyalogu(ctx, "home", kartAdi, "Ana ekrandan taşınan blok") {
                        view.rootView?.let { anaEkranSiraVeBoyutUygula(it) }
                    }
                    3 -> kartBoyutuDegistirDiyalogu()
                    4 -> (activity as? MainActivity)?.openSettings()
                    5 -> {
                        GorunumAyar.anaEkranSirasiniSifirla(ctx)
                        GorunumAyar.setKartBoyutuOlcegi(ctx, 1)
                        view.rootView?.let { anaEkranSiraVeBoyutUygula(it) }
                        Toast.makeText(ctx, "✨ Ana ekran sıralaması ve kart boyutları sıfırlandı!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    private fun kartSirasiDegistir(view: View, yukariMi: Boolean) {
        val ebeveyn = view.parent as? android.view.ViewGroup ?: return
        val pos = ebeveyn.indexOfChild(view)
        val hedefPos = if (yukariMi) pos - 1 else pos + 1
        if (hedefPos in 0 until ebeveyn.childCount) {
            ebeveyn.removeViewAt(pos)
            ebeveyn.addView(view, hedefPos)
            val idler = mutableListOf<String>()
            for (i in 0 until ebeveyn.childCount) {
                val c = ebeveyn.getChildAt(i)
                if (c.id != View.NO_ID) idler.add(c.id.toString())
            }
            GorunumAyar.setAnaEkranSiralama(requireContext(), idler)
            Toast.makeText(requireContext(), "🔀 Kart sırası değiştirildi!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "⚠️ Daha fazla taşınamaz.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun kartBoyutuDegistirDiyalogu() {
        val ctx = context ?: return
        val secenekler = arrayOf("Kompakt (%85)", "Normal (%100)", "Geniş (%115)", "Devasa (%130)")
        com.google.android.material.dialog.MaterialAlertDialogBuilder(ctx)
            .setTitle("📐 Günün Akışı & Kart Boyutu Seç")
            .setItems(secenekler) { _, idx ->
                GorunumAyar.setKartBoyutuOlcegi(ctx, idx)
                view?.let { anaEkranSiraVeBoyutUygula(it) }
                Toast.makeText(ctx, "📐 Kart boyutu '${secenekler[idx]}' olarak ayarlandı!", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun anaEkranSiraVeBoyutUygula(kok: View) {
        val ctx = context ?: return
        val blokHero = kok.findViewById<View>(R.id.blokHero)
        if (blokHero != null) {
            blokHero.scaleX = 1.0f // v11.08: Yanlardan küçülmeyi daima önle (%100 tam genişlik)
            blokHero.scaleY = 1.0f
            val kademe = GorunumAyar.kartBoyutuOlcegi(ctx)
            val yog = resources.displayMetrics.density
            val padV = when (kademe) {
                0 -> (6 * yog).toInt()   // Kompakt
                2 -> (18 * yog).toInt()  // Geniş
                3 -> (26 * yog).toInt()  // Devasa
                else -> (12 * yog).toInt() // Normal (%100)
            }
            blokHero.setPadding(0, padV, 0, padV)
        }
        val ebeveyn = blokHero?.parent as? android.view.ViewGroup ?: return
        SekmeVeVeriTasimaMotoru.sekmeTasinanVerileriCiz(ctx, "home", ebeveyn)
        SekmeVeVeriTasimaMotoru.siralamayiVeBoyutuUygula(ctx, "home", ebeveyn)
    }
}
