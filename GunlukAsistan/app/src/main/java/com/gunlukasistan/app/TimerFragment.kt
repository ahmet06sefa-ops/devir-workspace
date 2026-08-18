package com.gunlukasistan.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale

/**
 * Sayaç ekranı: Pomodoro tarzı geri sayım + kronometre + ortam (odak) sesleri.
 * Biten her odaklanma süresi günlük istatistiklere eklenir.
 */
class TimerFragment : Fragment(R.layout.fragment_timer) {

    companion object {
        private const val MODE_WATCH = 0
        private const val MODE_DOWN = 1
        /** v10.24: ileri sayım — motor DIŞI, damga-temelli üçüncü mod. */
        private const val MODE_ILE = 2
        private const val CHANNEL_ID = "calisma_zamanlayici"
        private const val NOTIF_ID = 1001
        private const val ACTION_PIP_KONTROL = "com.gunlukasistan.app.PIP_KONTROL"
        private const val EXTRA_PIP_KOD = "pip_kod"
    }

    private var mode = MODE_DOWN
    private var running = false
    private var pipAlici: android.content.BroadcastReceiver? = null
    private var sonPipCalisiyorState: Boolean? = null

    /**
     * v10.19 · S1 — kurulum sırasında programatik `toggle.check()` de
     * dinleyiciyi tetikler; bu kullanıcı eylemi SAYILMAZ ve motora
     * dokunmamalıdır. `false` durumda dinleyici yalnız görsel mod
     * bayrağını günceller; onViewCreated sonunda true yapılır.
     */
    private var kurulumBitti = false
    private var notifTickCounter = 0

    /** v7.89: bildirim izni uyarısı bu oturumda gösterildi mi. */
    private var izinUyarisiGosterildi = false

    // Kronometre durumu
    private var watchAccumulated = 0L
    private var watchStartStamp = 0L
    private val laps = mutableListOf<Long>()

    // Geri sayım durumu
    private var countTotal = 25 * 60_000L
    private var countRemaining = 25 * 60_000L
    private var countEndStamp = 0L

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            tick()
        }
    }

    /**
     * v11.13 — tık çevrimi emniyet sarmalayıcısı.
     *
     * Gövde (`tikGovdesi`) içinde bir istisna olsa bile çevrim asla durmaz;
     * `tik()` false döndürür ve bir sonraki tık zamanlanır. Bu, "saat
     * takılıyor" şikayetinin kök sebebini (postDelayed en son satıra
     * yazıldığı için gövde hatasının tık zincirini koparması) giderir.
     */
    private val cevrimliTik = CevrimliTik(
        govde = { tikGovdesi() },
        hataRaporla = { e -> android.util.Log.w("TimerFragment", "Tik hatası — akış korundu", e) }
    )

    // v10.4 · A9: sesli geri sayım — yalnız bu ekran öndeyken yaşar
    private var sesli: SayacSesli? = null
    private val soylenenler = mutableSetOf<Int>()

    private lateinit var timeText: TextView

    /** v7.85: halka kadran göstergesi. */
    private lateinit var kadran: SayacKadraniView
    private lateinit var mainAction: Button
    private lateinit var resetButton: Button
    private lateinit var lapButton: Button
    private lateinit var lapCard: View
    private lateinit var lapText: TextView
    private lateinit var presetRow: LinearLayout
    private lateinit var todayStat: TextView
    private lateinit var toggle: MaterialButtonToggleGroup

    /** v10.12 · D20: kendinle maç satırı. */
    private lateinit var ghostText: TextView

    // v10.12 · D20/D23: 5 saniyelik önbellekle hesaplanan görünüm durumu
    private var sonMacMs = 0L
    private var sonMacSen = 0f
    private var sonMacRakip = 0f
    private var sonIsaretDolu = 0
    private var sonIsaretToplam = 0
    private lateinit var soundRow: LinearLayout

    // ---------------- Odak sesleri ----------------

    // v10.12 · D22: liste tek kaynaktan — motor ile ekran sırayı paylaşır
    private val sounds = SesManzarasi.SESLER

    // v10.12 · D22: çalma artık SesManzarasi motorunda (tek akış; sekme
    // değişse, arka plana geçse de sürer). Bu sınıf motorun durumunu
    // kartlara YANSITIR; kullanıcı dokunuşunu motora iletir.
    private val soundCards = mutableListOf<MaterialCardView>()
    private var eqView: EqualizerView? = null

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // v8.6 · Öneri 29: yatay/tablet genişlik sınırı + yoğunluk
        Duzen.uygula(view)
        initKpssSayacAtolye(view) // v10.78: KPSS Sayaç Oturum Hapı & Butonları
        timeText = view.findViewById(R.id.timeText)
        kadran = view.findViewById(R.id.sayacKadran)
        // v10.41 · Kullanıcı maddesi #2: kadran yazı ölçeği ayarı
        kadran.yaziOlcek = SayacAyar.kadranOlcek(requireContext())
        kadranRenkleriniAyarla()
        // Kadrana dokunmak başlat/duraklat — büyük ve doğal bir hedef
        kadran.setOnClickListener {
            if (mode == MODE_ILE) ileriAnaDugme() else if (running) pause() else start()
        }
        // v10.7 · A3: dış halkayı sürükleyerek süre seçimi.
        // Koşul: geri sayım modu + sayaç boşta (taze, başındayken) +
        // zincir koşmuyor (evre süresini zincir yönetir).
        kadran.sureSecici = object : SayacKadraniView.SureSecici {
            override fun izinVar(): Boolean {
                val ctx = context ?: return false
                if (mode != MODE_DOWN || running) return false
                if (SayacZincir.kosuyor(ctx)) return false
                return TimerEngine.remainingMs(ctx) == TimerEngine.totalMs(ctx)
            }

            override fun secimBasladi() {
                secimAktif = true
            }

            override fun dakikaSecildi(dakika: Int) {
                secimGuncelle(dakika)
            }

            override fun secimBitti(dakika: Int) {
                secimAktif = false
                // Bırakılan dakika kalıcı kurulumla uygulanır
                setPreset(dakika)
            }

            override fun secimIptal() {
                secimAktif = false
                // Jest yarıda kesildi: canlı gösterimde kalan süreyi
                // motordaki gerçek değere geri sar (motor hiç
                // değişmedi; yalnızca gösterim oynadı).
                val ctx = context ?: return
                countTotal = TimerEngine.totalMs(ctx)
                updateDisplay()
            }
        }
        mainAction = view.findViewById(R.id.mainAction)
        resetButton = view.findViewById(R.id.resetButton)
        lapButton = view.findViewById(R.id.lapButton)
        lapCard = view.findViewById(R.id.lapCard)
        lapText = view.findViewById(R.id.lapText)
        presetRow = view.findViewById(R.id.presetRow)
        todayStat = view.findViewById(R.id.todayStat)
        toggle = view.findViewById(R.id.modeToggle)
        ghostText = view.findViewById(R.id.ghostText)
        soundRow = view.findViewById(R.id.soundRow)
        eqView = view.findViewById(R.id.eqView)

        createNotificationChannel()
        if (Store.getNotifEnabled(requireContext()) && Build.VERSION.SDK_INT >= 33) {
            notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        // v6.5: bildirim kapalıysa uyarı şeridi — dokununca sistem ayarına götürür
        view.findViewById<View>(R.id.notifBanner).setOnClickListener {
            openNotificationSettings()
        }

        toggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val yeniMod = when (checkedId) {
                R.id.watchButton -> MODE_WATCH
                R.id.ileriButton -> MODE_ILE
                else -> MODE_DOWN
            }
            // v10.19 · S1 DÜZELTMESİ — bildirime dokunup uygulama yeniden
            // açıldığında sayacın sıfırlanmasının kök sebebi:
            // kurulumdaki programatik check() burada resetAll()
            // çalıştırıyor ve motoru sessizce sıfırlıyordu. Kuruluma
            // kadar dinleyici motora DOKUNMASIN; durum onResume'da
            // motordan okunur.
            if (!kurulumBitti) {
                mode = yeniMod
                return@addOnButtonCheckedListener
            }
            // Aynı düğmeye tekrar dokunmak sıfırlama değildir
            if (yeniMod == mode) return@addOnButtonCheckedListener

            // v11.05: Sayaç çalışırken mod değişimine basılınca sayaç sıfırlanmasın
            val aktifVeyaBeklemedeMi = running || TimerEngine.isRunning(requireContext()) || IleriSayim.calismakta(requireContext())
            if (aktifVeyaBeklemedeMi) {
                Toast.makeText(
                    requireContext(),
                    "⏱️ Sayacınız çalışırken mod değiştirilemez. Önce duraklatın / sıfırlayın.",
                    Toast.LENGTH_SHORT
                ).show()
                kurulumBitti = false
                toggle.check(
                    when (mode) {
                        MODE_ILE -> R.id.ileriButton
                        MODE_WATCH -> R.id.watchButton
                        else -> R.id.countdownButton
                    }
                )
                kurulumBitti = true
                return@addOnButtonCheckedListener
            }

            mode = yeniMod
            resetAll()
            // v10.24: ileri moda geçişte düğme etiketleri/görünüm kendisine aittir
            if (mode == MODE_ILE) ileriUiTazele()
        }
        // Motor o an hangi moddaysa ekran da o modda açılır — böylece
        // çalışan kronometre varken geri sayım seçimiyle kayıp olmaz.
        runCatching {
            toggle.check(
                when {
                    // v10.24: açık ileri sayım oturumu varsa mod seçimi oraya düşer
                    IleriSayim.aktifMi(requireContext()) -> R.id.ileriButton
                    TimerEngine.mode(requireContext()) == TimerEngine.MODE_WATCH -> {
                        R.id.watchButton
                    }
                    else -> R.id.countdownButton
                }
            )
        }
        kurulumBitti = true
        if (mode == MODE_ILE) ileriUiTazele()

        // v10.26 · öneri #61: İleri düğmesine UZUN basış oturum adı diyaloğunu
        // açar; kısa dokunuş her zamanki mod seçimidir (çakışmaz, iki ayrı olay).
        view.findViewById<View>(R.id.ileriButton)?.setOnLongClickListener {
            if (mode == MODE_ILE) { ileriUzunMenu(); true } else false
        }

        view.findViewById<Button>(R.id.presetFocus).setOnClickListener { setPreset(25) }
        view.findViewById<Button>(R.id.presetShort).setOnClickListener { setPreset(5) }
        view.findViewById<Button>(R.id.presetLong).setOnClickListener { setPreset(15) }
        view.findViewById<Button>(R.id.presetCustom).setOnClickListener { showCustomPicker() }

        // v10.4 · A5: oturum etiketi çipi — hazır ayar satırının hemen
        // altında küçük bir köprü; bildirimde alt metin olarak görünür.
        etiketCipiniKur(view)
        // v10.7 · A6: zincir çipi — etiket çipinin hemen altına
        zincirChipiniKur(view)
        // v10.19 · S2: sayaçsız çalışılan süreler için manuel odak çipi
        manuelOdakCipiniKur(view)
        // v10.50: Göreve Bağlı Sayaç (#2) ve Proje/Ders Seç (#8) çipleri
        odakBaglantiCipleriniKur(view)

        // v5.7: yatay tam ekran flip saat
        view.findViewById<View>(R.id.fullscreenButton).setOnClickListener { openFullscreen() }

        // v10.12 · D19: nefes stüdyosu
        view.findViewById<View>(R.id.nefesButton).setOnClickListener {
            startActivity(android.content.Intent(requireContext(), NefesActivity::class.java))
        }
        // v10.45 · Madde #7: mini uygulama düğmesi
        view.findViewById<View>(R.id.miniModButton)?.setOnClickListener { pipGir() }
        // v7.86: sağ üst ⋮ — zamanlayıcı ayarları
        view.findViewById<View>(R.id.timerSettings).setOnClickListener {
            SayacAyarActivity.ac(requireContext())
        }

        mainAction.setOnClickListener {
            if (mode == MODE_ILE) ileriAnaDugme() else if (running) pause() else start()
        }
        resetButton.setOnClickListener {
            if (mode == MODE_ILE) ileriBitirKaydet() else resetAll()
        }
        lapButton.setOnClickListener {
            laps.add(0, TimerEngine.elapsedMs(requireContext()))
            renderLaps()
        }

        buildSoundCards()
        guncellePresetButonlari(view)
        guncelleOdakSesVeTemaGorunurlugu(view)
        initArkaPlanMedyaKumandasi(view)
        updateDisplay()
        refreshTodayStat()
        pipAliciKur()
    }

    private fun initArkaPlanMedyaKumandasi(view: View) {
        val acik = SayacAyar.arkaPlanMedyaKumandasiAcikMi(requireContext())
        val card = view.findViewById<View>(R.id.cardArkaPlanMedya) ?: return
        if (!acik) {
            card.visibility = View.GONE
            return
        }
        card.visibility = View.VISIBLE

        val txtDurum = view.findViewById<TextView>(R.id.txtArkaPlanMedyaDurum)
        fun guncelleDurum() {
            val caliyor = ArkaPlanMedyaKumandasi.muzikCaliyorMu(requireContext())
            txtDurum?.text = ArkaPlanMedyaKumandasi.durumMetniGetir(caliyor)
        }
        guncelleDurum()

        view.findViewById<Button>(R.id.btnMedyaGeri)?.setOnClickListener {
            ArkaPlanMedyaKumandasi.medyaEylemiGonder(requireContext(), ArkaPlanMedyaKumandasi.Eylem.ONCEKI)
            guncelleDurum()
            android.widget.Toast.makeText(requireContext(), "|◀ Geri · Önceki parça komutu iletildi", android.widget.Toast.LENGTH_SHORT).show()
        }

        view.findViewById<Button>(R.id.btnMedyaOynatDur)?.setOnClickListener {
            ArkaPlanMedyaKumandasi.medyaEylemiGonder(requireContext(), ArkaPlanMedyaKumandasi.Eylem.OYNAT_DURDUR)
            guncelleDurum()
            android.widget.Toast.makeText(requireContext(), "▶/⏸ Oynat/Dur · Medya komutu iletildi", android.widget.Toast.LENGTH_SHORT).show()
        }

        view.findViewById<Button>(R.id.btnMedyaIleri)?.setOnClickListener {
            ArkaPlanMedyaKumandasi.medyaEylemiGonder(requireContext(), ArkaPlanMedyaKumandasi.Eylem.SONRAKI)
            guncelleDurum()
            android.widget.Toast.makeText(requireContext(), "▶| İleri · Sonraki parça komutu iletildi", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun guncellePresetButonlari(view: View) {
        val presetler = SayacPreset.getPresetler(requireContext())
        val p1 = presetler.getOrElse(0) { 5 }
        val p2 = presetler.getOrElse(1) { 10 }
        val p3 = presetler.getOrElse(2) { 25 }

        view.findViewById<Button>(R.id.presetShort)?.apply {
            text = "$p1 dk"
            setOnClickListener { setPreset(p1) }
        }
        view.findViewById<Button>(R.id.presetLong)?.apply {
            text = "$p2 dk"
            setOnClickListener { setPreset(p2) }
        }
        view.findViewById<Button>(R.id.presetFocus)?.apply {
            text = "$p3 dk"
            setOnClickListener { setPreset(p3) }
        }
    }

    private fun guncelleOdakSesVeTemaGorunurlugu(view: View) {
        val acik = SayacAyar.odakSesVeTemaAcikMi(requireContext())
        val soundScroll = view.findViewById<View>(R.id.soundRow)?.parent as? View
        val eq = view.findViewById<View>(R.id.eqView)
        if (!acik) {
            soundScroll?.visibility = View.GONE
            view.findViewById<View>(R.id.soundRow)?.visibility = View.GONE
            eq?.visibility = View.GONE
            stopSound()
        } else {
            soundScroll?.visibility = View.VISIBLE
            view.findViewById<View>(R.id.soundRow)?.visibility = View.VISIBLE
            eq?.visibility = View.VISIBLE
        }
        TabloBaslikYonetimMotoru.basliklariUygula(
            requireContext(),
            view.findViewById(R.id.txtOdakSesleriBaslik),
            view.findViewById(R.id.txtArkaPlanMedyaBaslik)
        )
    }

    // ---------------- Odak sesleri kartları ----------------

    /** Yatay tam ekran Fliqlo tarzı saati açar; mevcut süre ve ses aktarılır. */
    private fun openFullscreen() {
        val intent = android.content.Intent(requireContext(), FullscreenTimerActivity::class.java)
        intent.putExtra(
            FullscreenTimerActivity.EXTRA_MODE,
            if (mode == MODE_WATCH) 1 else 0
        )
        intent.putExtra(FullscreenTimerActivity.EXTRA_TOTAL_MS, countTotal)
        // v10.12 · D22: ses motorda yaşar — ekranlar arası kesilmez
        intent.putExtra(FullscreenTimerActivity.EXTRA_SOUND, SesManzarasi.calanIndeks)
        startActivity(intent)
    }

    private fun dp(v: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics
        ).toInt()

    private fun buildSoundCards() {
        val context = requireContext()
        soundRow.removeAllViews()
        soundCards.clear()
        sounds.forEachIndexed { index, ambient ->
            val card = MaterialCardView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = dp(10) }
                radius = dp(16).toFloat()
                cardElevation = 0f
                isClickable = true
                isFocusable = true
            }
            val inner = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(14), dp(12), dp(14), dp(12))
            }
            inner.addView(TextView(context).apply {
                text = ambient.emoji
                textSize = 24f
                gravity = Gravity.CENTER
            })
            inner.addView(TextView(context).apply {
                setText(ambient.adRes)
                textSize = 11f
                setTextColor(
                    MaterialColors.getColor(
                        context, com.google.android.material.R.attr.colorOnSurface, 0
                    )
                )
                gravity = Gravity.CENTER
                maxLines = 1
            })
            card.addView(inner)
            card.dalgaEkle()
            card.setOnClickListener { onSoundClicked(index) }
            soundCards.add(card)
            soundRow.addView(card)
        }
        refreshSoundSelection()
    }

    private fun refreshSoundSelection() {
        val context = context ?: return
        val primary = MaterialColors.getColor(
            context, com.google.android.material.R.attr.colorPrimary, 0
        )
        val primaryContainer = MaterialColors.getColor(
            context, com.google.android.material.R.attr.colorPrimaryContainer, 0
        )
        val secondaryContainer = MaterialColors.getColor(
            context, com.google.android.material.R.attr.colorSecondaryContainer, 0
        )
        soundCards.forEachIndexed { index, card ->
            if (index == SesManzarasi.calanIndeks) {
                card.setCardBackgroundColor(primaryContainer)
                card.strokeColor = primary
                card.strokeWidth = dp(2)
            } else {
                card.setCardBackgroundColor(secondaryContainer)
                card.strokeWidth = 0
            }
        }
        // Çalma görsel efekti
        val isPlaying = SesManzarasi.calanIndeks != -1
        eqView?.visibility = if (isPlaying) View.VISIBLE else View.GONE
        if (isPlaying) eqView?.start() else eqView?.stop()
    }

    private fun onSoundClicked(index: Int) {
        if (SesManzarasi.calanIndeks == index) {
            stopSound()
        } else {
            playSound(index)
        }
    }

    private fun playSound(index: Int) {
        // v10.12 · D22: ses kartı artık motoru sürüyor
        SesManzarasi.manuelCal(requireContext(), index)
        refreshSoundSelection()
    }

    private fun stopSound() {
        SesManzarasi.manuelDur(requireContext())
        refreshSoundSelection()
    }

    // ---------------- Zaman hesapları ----------------

    private fun currentWatchElapsedLocal(): Long =
        watchAccumulated + if (running && mode == MODE_WATCH) {
            SystemClock.elapsedRealtime() - watchStartStamp
        } else 0

    private fun currentCountRemaining(): Long = TimerEngine.remainingMs(requireContext())

    /**
     * v11.13 — Tık gövdesi güvenli sarmalayıcıdan çalışır.
     * `true` dönerse çevrim bitirilir; `false` ise çağıran bir sonraki tıkı
     * zamanlar (akış hata olsa bile sürer → saat takılmaz).
     */
    private fun tick() {
        if (!cevrimliTik.tik()) {
            handler.postDelayed(ticker, 100)
        }
    }

    /** Tek tık adımı. `true` = dur; `false` = devam et. */
    private fun tikGovdesi(): Boolean {
        // v10.24: ileri sayım — motor yerine kendi damga deposu okunur.
        // Pil dostu: yalnız çalışırken tik sürer; beklerken/devre dışıyken durur.
        if (mode == MODE_ILE) {
            updateDisplay()
            pipTazele()
            (activity as? MainActivity)?.yuzenSeritiTazele()
            macTazele()
            val ileriCtx = context
            // Çalışmıyorsa çevrimi bitir (beklerken tık durur)
            return if (ileriCtx != null && IleriSayim.calismakta(ileriCtx)) {
                false
            } else {
                true
            }
        }
        if (mode == MODE_DOWN && running && currentCountRemaining() <= 0L) {
            if (SayacAyar.tasmaAcik(requireContext())) {
                val tasmaMs = OdakMotoru.tasmaSuresiHesapla(countEndStamp, System.currentTimeMillis(), true)
                timeText.text = OdakMotoru.tasmaMetni(tasmaMs)
                return false
            } else {
                onCountdownFinished()
                return true
            }
        }
        updateDisplay()
        pipTazele()
        (activity as? MainActivity)?.yuzenSeritiTazele()
        // v10.12 · D20/D23: maç satırı ve işaretler (içeride 5 sn kısıtlı)
        macTazele()
        // v10.4 · A9: kalan süreyi kulağa söyle (eşikler SayacSes'te)
        sesli?.let { s ->
            if (mode == MODE_DOWN && running) {
                val kalanSn = (currentCountRemaining() / 1000L).toInt()
                if (SayacSes.soylenmeli(kalanSn, soylenenler)) {
                    soylenenler.add(kalanSn)
                    SayacSes.konusmaMetni(kalanSn)?.let { s.soyle(it) }
                }
            }
        }
        // Bildirimi ~5 saniyede bir tazele (ilerleme çubuğu ve durum senkron kalsın)
        notifTickCounter++
        // v7.89: 5 sn yerine 2 sn — duraklat/devam sonrası panel çabuk güncellensin
        if (notifTickCounter % 20 == 0) TimerNotifier.show(requireContext())
        return false
    }

    private fun updateDisplay() {
        if (view == null) return
        // v10.24: ileri sayım görünümü — büyük saat + halka, kronometre gibi dolar
        if (mode == MODE_ILE) {
            val gecen = IleriSayim.gecenSimdi(requireContext(), System.currentTimeMillis())
            displayIleri(gecen)
            return
        }
        val millis = TimerEngine.displayMs(requireContext())
        // v10.49 · #7: Zen Odak vs Canlı Kadran Modu
        if (GorunumAyar.zenOdakMi(requireContext()) && running) {
            timeText.text = SayacAyar.kalanSureDakikaSaniyeMetni(millis)
        } else {
            Rulo.yaz(timeText, formatTime(millis))
        }
        kadraniTazele(millis)
    }

    /**
     * v7.85 — Halka kadranı günceller.
     *
     * Geri sayımda halka **boşalır** (kalan oran), kronometrede dakika
     * içindeki saniyeye göre **dolar** — sürekli hareket eden bir gösterge
     * olsun, sabit durmasın.
     */
    private fun kadraniTazele(millis: Long) {
        if (!::kadran.isInitialized) return
        val ctx = context ?: return
        // v10.7 · A3: halkadan süre seçilirken kadran seçim
        // gösteriminde kalır; normal tazeleme görüntüyü ezmemeli.
        if (secimAktif) return

        // v10.12 · D20/D23: hayalet yaylar + seans işaretleri önbellekten
        kadran.isaretleriAyarla(sonIsaretDolu, sonIsaretToplam)
        kadran.maciAyarla(sonMacSen, sonMacRakip)

        if (mode == MODE_WATCH) {
            val saniye = (millis / 1000L) % 60L
            kadran.guncelle(
                kalanOran = saniye / 60f,
                sure = formatTime(millis),
                ust = getString(R.string.stopwatch),
                alt = "",
                aktif = running
            )
            return
        }

        val toplam = TimerEngine.totalMs(ctx).coerceAtLeast(1L)
        val dakika = (toplam / 60_000L).toInt()

        // Bitiş saati — "🔔 12:26"
        val altMetin = if (running && millis > 0) {
            val bitis = System.currentTimeMillis() + millis
            "🔔 " + android.text.format.DateFormat.getTimeFormat(ctx)
                .format(java.util.Date(bitis))
        } else if (millis <= 0L) {
            getString(R.string.sy_bitti)
        } else if (!SayacZincir.kosuyor(ctx) && millis == toplam) {
            // v10.7 · A3: seçici hazır — sürükleme ipucu görünür.
            // (Taze sayaç "duraklatıldı" yazıyordu; yanıltıcıydı.)
            getString(R.string.hl_ipucu)
        } else {
            getString(R.string.sy_duraklatildi)
        }

        // v10.7 · A6: zincir koşarken üst satır adım bilgisidir
        // ("⛓ 💪 Çalış · 3/16"); pomodoro satırı geriye düşer.
        val ustMetin = zincirUstMetni(ctx) ?: if (Pomodoro.acikMi(ctx)) {
            Pomodoro.evreAdi(ctx) + " · " + getString(R.string.sy_dakika_kisa, dakika)
        } else {
            getString(R.string.sy_dakika_kisa, dakika)
        }

        kadran.guncelle(
            kalanOran = millis.toFloat() / toplam.toFloat(),
            sure = formatTime(millis),
            ust = ustMetin,
            alt = altMetin,
            aktif = running,
            // v8.4 · Öneri 19: kalan saniye — renk geçişi ve nabız için
            kalanSn = millis / 1000L,
            // v10.2 · A4: spurt bölgesi toplamın %10'una genişledi
            toplamMs = toplam
        )

        // v8.4: sayaç bittiği anda bir kez parlama
        if (millis <= 0L && sonKalan > 0L) {
            kadran.bitisParlamasi()
        }
        sonKalan = millis

        // v10.7 · A6: evre arka planda ilerlediyse çip de tazelensin.
        // Okuma ucuzdur (prefs bellekte); Pomodoro satırı da her tık
        // yeniden okunuyor — aynı kalıp.
        zincirChipiYaz()
    }

    /** v8.4: bitiş anını bir kez yakalamak için önceki kalan süre. */
    private var sonKalan = Long.MAX_VALUE

    /** Kadran renklerini temadan alır — koyu/açık temada okunur kalsın. */
    /**
     * v7.86 — Sayaç ekranı açıkken ekranın sönmesini engeller.
     *
     * Bayrak pencereye takılıyor; fragment görünmez olunca
     * [onPause] içinde temizleniyor, yoksa başka sekmelerde de
     * ekran açık kalıp pil yakardı.
     */
    /**
     * v7.89 — Sayaç başlatılırken bildirim izni denetimi.
     *
     * İzin yoksa bildirim paneli boş kalır; kullanıcı "sayaç çalışmıyor"
     * sanır. Oturumda bir kez uyarılıyor — her başlatmada rahatsız etmek
     * yerine tek seferlik bir hatırlatma yeterli.
     */
    private fun bildirimIzniniDenetle(ctx: android.content.Context) {
        if (izinUyarisiGosterildi) return
        val acik = try {
            androidx.core.app.NotificationManagerCompat.from(ctx).areNotificationsEnabled()
        } catch (e: Exception) {
            true
        }
        if (acik && Store.getNotifEnabled(ctx) && SayacAyar.miniGoster(ctx)) return

        izinUyarisiGosterildi = true
        val mesaj = when {
            !acik -> getString(R.string.tf_izin_yok)
            !Store.getNotifEnabled(ctx) -> getString(R.string.tf_bildirim_kapali)
            else -> getString(R.string.tf_mini_kapali)
        }
        com.google.android.material.dialog.MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.tf_bildirim_uyari_baslik)
            .setMessage(mesaj)
            .setPositiveButton(R.string.tf_duzelt) { _, _ ->
                if (!acik) openNotificationSettings()
                else SayacAyarActivity.ac(ctx)
            }
            .setNegativeButton(R.string.ok, null)
            .show()
    }

    private fun ekranBayragi() {
        val pencere = activity?.window ?: return
        if (SayacAyar.ekranAcikKalsin(requireContext())) {
            pencere.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            pencere.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun kadranRenkleriniAyarla() {
        val ctx = context ?: return
        fun renk(attr: Int, varsayilan: Int) =
            com.google.android.material.color.MaterialColors.getColor(ctx, attr, varsayilan)

        val vurgu = renk(com.google.android.material.R.attr.colorPrimary, 0xFF7C6BF5.toInt())
        val yuzey = renk(
            com.google.android.material.R.attr.colorSurfaceVariant, 0xFF2A2A2E.toInt()
        )
        val metin = renk(com.google.android.material.R.attr.colorOnSurface, 0xFF000000.toInt())
        kadran.renkleriAyarla(
            vurgu = vurgu,
            metin = metin,
            ikincil = (metin and 0x00FFFFFF) or 0xB0000000.toInt(),
            daire = yuzey,
            soluk = (metin and 0x00FFFFFF) or 0x30000000
        )
    }

    private fun formatTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val h = totalSeconds / 3600
        val m = totalSeconds % 3600 / 60
        val s = totalSeconds % 60
        return if (h > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.US, "%02d:%02d", m, s)
        }
    }

    // ---------------- Kontroller ----------------

    private fun start() {
        val ctx = requireContext()
        // v7.89: bildirim izni yoksa kullanıcı sayacı panelde göremez ve
        // nedenini bilemez. Sessizce başlatmak yerine bir kez uyar.
        bildirimIzniniDenetle(ctx)
        // v10.50 #7: Çarpışma Bekçisi — yaklaşan vakitle çakışma varsa uyar
        if (SayacAyar.carpismaBekcisiAcik(ctx) && mode == MODE_DOWN && NamazVakti.acikMi(ctx)) {
            val gun = NamazVakti.bugun(ctx)
            val (_, kalanDk) = gun.sonraki(NamazVakti.simdiDakika())
            val istenenDk = (countRemaining / 60_000L).toInt()
            val carpisma = OdakMotoru.carpismaDenetimi(istenenDk, kalanDk)
            if (carpisma.carpismaVar) {
                MaterialAlertDialogBuilder(ctx)
                    .setTitle(R.string.om_carpisma_bekcisi)
                    .setMessage(carpisma.uyariMetni)
                    .setPositiveButton("Evet (${carpisma.guvenliDk} dk)") { _, _ ->
                        countRemaining = carpisma.guvenliDk * 60_000L
                        countTotal = countRemaining
                        TimerEngine.setTotalMs(ctx, countTotal)
                        gercektenBaslat(ctx)
                    }
                    .setNegativeButton("Hayır (Değiştirme)") { _, _ ->
                        gercektenBaslat(ctx)
                    }
                    .show()
                return
            }
        }
        // v10.2 · Öneri A11: 3-2-1 başlangıç ritüeli.
        // Animasyon kapalıysa ya da kullanıcı ayarından sildiyse atlanır.
        if (!rituelAktif && SayacAyar.baslangic321(ctx) && GorunumAyar.animasyonAcik(ctx)) {
            geriSayimRitueli { gercektenBaslat(ctx) }
            return
        }
        gercektenBaslat(ctx)
    }

    // ---------------- v10.4 · A5: oturum etiketi ----------------

    private var etiketChip: TextView? = null

    private fun etiketCipiniKur(view: View) {
        val ctx = requireContext()
        val yogunluk = resources.displayMetrics.density
        fun dp(v: Int) = (v * yogunluk).toInt()
        val chip = TextView(ctx).apply {
            textSize = 13f
            visibility = View.GONE // v11.05: Etiket ekle gizlendi
            setPadding(dp(14), dp(8), dp(14), dp(8))
            // RippleTutarlilikTest deseni: seçilebilir zemin + ripple
            val ta = ctx.obtainStyledAttributes(
                intArrayOf(android.R.attr.selectableItemBackground)
            )
            foreground = ta.getDrawable(0)
            ta.recycle()
            isClickable = true
            isFocusable = true
            setOnClickListener { etiketDiyalog() }
        }
        etiketChip = chip
        (presetRow.parent as? android.view.ViewGroup)?.let { ebeveyn ->
            val siradaki = ebeveyn.indexOfChild(presetRow) + 1
            if (siradaki in 0..ebeveyn.childCount) {
                ebeveyn.addView(chip, siradaki)
            } else {
                ebeveyn.addView(chip)
            }
        }
        etiketYaz()
    }

    private fun etiketYaz() {
        val e = SayacAyar.etiket(requireContext())
        etiketChip?.text = if (e.isBlank()) {
            getString(R.string.tf_etiket_ekle)
        } else {
            getString(R.string.tf_etiket_aktif, e)
        }
        etiketChip?.visibility = View.GONE // v11.05: Etiket yazısı gizlendi
    }

    private fun etiketDiyalog() {
        val ctx = requireContext()
        val girdi = android.widget.EditText(ctx).apply {
            setText(SayacAyar.etiket(ctx))
            hint = getString(R.string.tf_etiket_ipucu)
            filters = arrayOf(android.text.InputFilter.LengthFilter(24))
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setSelection(text.length)
        }
        android.app.AlertDialog.Builder(ctx)
            .setTitle(R.string.tf_etiket_diyalog)
            .setView(girdi)
            .setPositiveButton(R.string.save) { _, _ ->
                SayacAyar.setEtiket(ctx, girdi.text.toString())
                etiketYaz()
                // Açık bildirim varsa alt metni hemen tazele
                TimerNotifier.show(ctx)
            }
            .setNeutralButton(R.string.tf_etiket_temizle) { _, _ ->
                SayacAyar.setEtiket(ctx, "")
                etiketYaz()
                TimerNotifier.show(ctx)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ---------------- v10.7 · A6: zincir sayaç ----------------

    /** v10.7 · A3: halkadan süre seçimi sürerken true. */
    private var secimAktif = false

    /** v10.7 · A3: sürükleme sırasında kadranı canlı tazeler. */
    private fun secimGuncelle(dakika: Int) {
        if (!::kadran.isInitialized) return
        countTotal = dakika * 60_000L
        kadran.guncelle(
            kalanOran = dakika / 60f,
            sure = formatTime(countTotal),
            ust = getString(R.string.hl_hedef, dakika),
            alt = getString(R.string.hl_birak),
            aktif = false
        )
    }

    /** Kadranın üst satırı — yalnız zincir koşarken adım bilgisi. */
    private fun zincirUstMetni(ctx: android.content.Context): String? {
        if (!SayacZincir.kosuyor(ctx)) return null
        val z = SayacZincir.aktif(ctx) ?: return null
        val adim = SayacZincir.adim(ctx)
        val (n, toplam) = SayacZincir.kacinciAdim(z, adim)
        val evre = SayacZincir.adimdaki(z, adim)
        return getString(R.string.zk_ust, "${evre.emoji} ${evre.ad}", n, toplam)
    }

    private var zincirChip: TextView? = null

    private fun zincirChipiniKur(view: View) {
        val ctx = requireContext()
        val yogunluk = resources.displayMetrics.density
        fun dp(v: Int) = (v * yogunluk).toInt()
        val chip = TextView(ctx).apply {
            textSize = 13f
            visibility = View.GONE // v11.05: Zincir kur yazısı gizlendi
            setPadding(dp(14), dp(8), dp(14), dp(8))
            val ta = ctx.obtainStyledAttributes(
                intArrayOf(android.R.attr.selectableItemBackground)
            )
            foreground = ta.getDrawable(0)
            ta.recycle()
            isClickable = true
            isFocusable = true
            setOnClickListener { zincirDiyalog() }
        }
        zincirChip = chip
        // Etiket çipinin hemen altına yerleşir (ikisi de presetRow'un
        // aşağısında programatik durur; layout dosyası değişmez).
        val ebeveyn = (etiketChip?.parent as? android.view.ViewGroup)
            ?: (presetRow.parent as? android.view.ViewGroup)
        ebeveyn?.let { ust ->
            val tutamac = if (etiketChip != null) {
                ust.indexOfChild(etiketChip) + 1
            } else {
                ust.indexOfChild(presetRow) + 1
            }
            if (tutamac in 1..ust.childCount) {
                ust.addView(chip, tutamac)
            } else {
                ust.addView(chip)
            }
        }
        zincirChipiYaz()
    }

    private fun zincirChipiYaz() {
        val ctx = context ?: return
        val chip = zincirChip ?: return
        chip.visibility = View.GONE // v11.05: Zincir kur yazısı gizlendi
        val z = SayacZincir.aktif(ctx)
        chip.text = when {
            z == null -> getString(R.string.zk_chip_kur)
            SayacZincir.kosuyor(ctx) -> {
                val (n, t) = SayacZincir.kacinciAdim(z, SayacZincir.adim(ctx))
                getString(R.string.zk_chip_kosuyor, z.ad, n, t)
            }
            SayacZincir.adim(ctx) > 0 -> {
                val (n, t) = SayacZincir.kacinciAdim(z, SayacZincir.adim(ctx))
                getString(R.string.zk_chip_kaldi, z.ad, n, t)
            }
            else -> getString(R.string.zk_chip_hazir, z.ad)
        }
        chip.contentDescription = chip.text
    }

    /** Seçili zinciri kaldığı adımdan (ya da baştan) başlatır. */
    private fun zincirBaslat() {
        val ctx = requireContext()
        val z = SayacZincir.aktif(ctx) ?: return
        // ÖNCE mod değişimi: check() dinleyici üzerinden resetAll
        // çağırır; resetAll koşan zinciri duraklatır. baslat() buna
        // kurban gitmesin diye toggle/check en başta.
        if (mode != MODE_DOWN) {
            toggle.check(R.id.countdownButton)
        }
        mode = MODE_DOWN
        // Şimdi zincir koşu sayacı açılır
        SayacZincir.baslat(ctx)
        val adim = SayacZincir.adim(ctx)
        val evre = SayacZincir.adimdaki(z, adim)
        countTotal = evre.sn * 1000L
        TimerEngine.setMode(ctx, TimerEngine.MODE_DOWN)
        TimerEngine.setTotalMs(ctx, countTotal)
        zincirChipiYaz()
        // 3-2-1 ritüeli dahil normal başlangıç akışı
        start()
    }

    private fun zincirDiyalog() {
        val ctx = requireContext()
        val z = SayacZincir.aktif(ctx)
        val yogunluk = resources.displayMetrics.density
        fun dp(v: Int) = (v * yogunluk).toInt()

        val kok = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), 0, dp(20), 0)
        }

        fun satir(metin: String, tikla: () -> Unit): TextView =
            TextView(ctx).apply {
                text = metin
                textSize = 15f
                setPadding(0, dp(14), 0, dp(14))
                val ta = ctx.obtainStyledAttributes(
                    intArrayOf(android.R.attr.selectableItemBackground)
                )
                foreground = ta.getDrawable(0)
                ta.recycle()
                isClickable = true
                isFocusable = true
                setOnClickListener { tikla() }
            }

        val durum = TextView(ctx).apply {
            textSize = 14f
            setPadding(0, dp(10), 0, dp(10))
        }
        kok.addView(durum)

        durum.text = when {
            z == null -> getString(R.string.zk_durum_yok)
            SayacZincir.kosuyor(ctx) -> {
                val (n, t) = SayacZincir.kacinciAdim(z, SayacZincir.adim(ctx))
                getString(R.string.zk_durum_kosuyor, z.emoji, z.ad, n, t)
            }
            SayacZincir.adim(ctx) > 0 -> {
                val (n, t) = SayacZincir.kacinciAdim(z, SayacZincir.adim(ctx))
                getString(R.string.zk_durum_durakti, z.emoji, z.ad, n, t)
            }
            else -> getString(
                R.string.zk_durum_hazir, z.emoji, z.ad, z.toplamAdim,
                SayacZincir.sureMetni(z.toplamSn)
            )
        }

        val dialog = MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.zk_diyalog_baslik)
            .setView(kok)
            .setNegativeButton(R.string.cancel, null)
            .create()

        kok.addView(satir(getString(R.string.zk_sablonlar)) {
            dialog.dismiss()
            zincirSecimDiyalog()
        })
        kok.addView(satir(getString(R.string.zk_kendi)) {
            dialog.dismiss()
            zincirBuilderDiyalog()
        })

        // Evreler otomatik aksın mı — zincirlerin ruhu akıştır ama
        // adım adım ilerlemek isteyene de kapı açık.
        kok.addView(
            androidx.appcompat.widget.SwitchCompat(ctx).apply {
                text = getString(R.string.zk_oto)
                isChecked = SayacZincir.otoDevam(ctx)
                setOnCheckedChangeListener { _, acik ->
                    SayacZincir.setOtoDevam(ctx, acik)
                }
            }
        )

        if (z != null) {
            if (SayacZincir.kosuyor(ctx)) {
                kok.addView(satir(getString(R.string.zk_durdur)) {
                    SayacZincir.durdur(ctx)
                    if (TimerEngine.isRunning(ctx)) pause()
                    zincirChipiYaz()
                    dialog.dismiss()
                })
            } else {
                val eylem = if (SayacZincir.adim(ctx) > 0) {
                    getString(R.string.zk_devam)
                } else {
                    getString(R.string.zk_baslat)
                }
                kok.addView(satir(eylem) {
                    if (TimerEngine.isRunning(ctx)) {
                        Toast.makeText(
                            ctx, R.string.zk_kosuyor_uyari, Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        dialog.dismiss()
                        zincirBaslat()
                    }
                })
            }
            if (SayacZincir.adim(ctx) > 0) {
                kok.addView(satir(getString(R.string.zk_bastan)) {
                    SayacZincir.sifirla(ctx)
                    zincirChipiYaz()
                    dialog.dismiss()
                    // Güncel durum metniyle yeniden aç
                    zincirDiyalog()
                })
            }
            if (z.id > 0) {
                kok.addView(satir(getString(R.string.zk_sil)) {
                    SayacZincir.sil(ctx, z.id)
                    Toast.makeText(ctx, R.string.zk_silindi, Toast.LENGTH_SHORT).show()
                    zincirChipiYaz()
                    dialog.dismiss()
                })
            }
            kok.addView(satir(getString(R.string.zk_secim_kaldir)) {
                SayacZincir.secimiKaldir(ctx)
                zincirChipiYaz()
                dialog.dismiss()
            })
        }

        dialog.show()
    }

    /** Şablonlar + kullanıcının kaydettikleri arasından seçim. */
    private fun zincirSecimDiyalog() {
        val ctx = requireContext()
        val liste = SayacZincir.sablonlar() + SayacZincir.kayitlilar(ctx)
        val adlar = liste.map { z ->
            getString(
                R.string.zk_zincir_ozele,
                "${z.emoji} ${z.ad}",
                z.toplamAdim,
                SayacZincir.sureMetni(z.toplamSn)
            )
        }.toTypedArray()
        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.zk_sablon_baslik)
            .setItems(adlar) { _, hangi ->
                val z = liste[hangi]
                SayacZincir.aktiflestir(ctx, z.id)
                Toast.makeText(
                    ctx, getString(R.string.zk_secildi, z.ad), Toast.LENGTH_SHORT
                ).show()
                zincirChipiYaz()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Kendi zincirini kurma diyaloğu.
     *
     * Dinamik satırlar: boş bırakılan satır sessizce atlanır (satır
     * silme düğmesi yerine — daha az görsel kalabalık). Doğrulama
     * tamamen [SayacZincir.dogrula] üzerinden; hata varsa diyalog
     * açık kalır, toast ile sebep söylenir.
     */
    private fun zincirBuilderDiyalog() {
        val ctx = requireContext()
        val yogunluk = resources.displayMetrics.density
        fun dp(v: Int) = (v * yogunluk).toInt()

        val govde = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(6), dp(20), dp(6))
        }
        val sarici = android.widget.ScrollView(ctx).apply { addView(govde) }

        val adGirdi = android.widget.EditText(ctx).apply {
            hint = getString(R.string.zk_ad_hint)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS
            filters = arrayOf(android.text.InputFilter.LengthFilter(24))
        }
        govde.addView(adGirdi)

        val satirKutusu = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
        }
        govde.addView(satirKutusu)

        val satirlar = mutableListOf<Pair<android.widget.EditText, android.widget.EditText>>()

        fun satirEkle() {
            if (satirlar.size >= SayacZincir.MAKS_EVRE) {
                Toast.makeText(
                    ctx,
                    getString(R.string.zk_hata_fazla, SayacZincir.MAKS_EVRE),
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
            val adE = android.widget.EditText(ctx).apply {
                hint = getString(R.string.zk_evre_hint, satirlar.size + 1)
                inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS
                filters = arrayOf(android.text.InputFilter.LengthFilter(18))
            }
            val dkE = android.widget.EditText(ctx).apply {
                hint = getString(R.string.zk_dk_hint)
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                filters = arrayOf(android.text.InputFilter.LengthFilter(3))
                gravity = Gravity.CENTER
            }
            val satir = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            satir.addView(
                adE,
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            )
            satir.addView(
                dkE,
                LinearLayout.LayoutParams(dp(72), LinearLayout.LayoutParams.WRAP_CONTENT)
                    .apply { marginStart = dp(8) }
            )
            satirKutusu.addView(satir)
            satirlar.add(adE to dkE)
        }

        // İki satırla açılır — çoğu zincir çalışma/mola ikilisi
        satirEkle()
        satirEkle()

        val ekleBtn = TextView(ctx).apply {
            text = getString(R.string.zk_evre_ekle)
            textSize = 14f
            setTextColor(
                MaterialColors.getColor(
                    ctx, com.google.android.material.R.attr.colorPrimary, 0
                )
            )
            setPadding(0, dp(12), 0, dp(12))
            isClickable = true
            isFocusable = true
            setOnClickListener { satirEkle() }
        }
        govde.addView(ekleBtn)

        val tekrarSatir = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        tekrarSatir.addView(
            TextView(ctx).apply {
                setText(R.string.zk_tekrar)
                textSize = 14f
            },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        )
        val tekrarPicker = NumberPicker(ctx).apply {
            minValue = 1
            maxValue = SayacZincir.MAKS_TEKRAR
            value = 1
        }
        tekrarSatir.addView(tekrarPicker)
        govde.addView(tekrarSatir)

        govde.addView(
            TextView(ctx).apply {
                setText(R.string.zk_mola_notu)
                textSize = 12f
                alpha = 0.7f
                setPadding(0, dp(8), 0, dp(4))
            }
        )

        val diyalog = MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.zk_builder_baslik)
            .setView(sarici)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .show()

        // Kaydet: hata varsa diyalog KAPANMASIN — kullanıcı girdiyi
        // kaybetmeden düzeltebilsin.
        diyalog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val evreler = mutableListOf<SayacZincir.Evre>()
            var hataVar = false
            satirlar.forEachIndexed { i, (adE, dkE) ->
                if (hataVar) return@forEachIndexed
                val ad = adE.text.toString().trim()
                val dkMetin = dkE.text.toString().trim()
                if (ad.isEmpty() && dkMetin.isEmpty()) return@forEachIndexed
                val dk = dkMetin.toIntOrNull()
                if (dk == null || dk !in 1..120) {
                    Toast.makeText(ctx, R.string.zk_hata_sure, Toast.LENGTH_SHORT).show()
                    hataVar = true
                    return@forEachIndexed
                }
                val gercekAd = ad.ifEmpty { getString(R.string.zk_evre_oto, i + 1) }
                evreler.add(
                    SayacZincir.Evre(
                        ad = gercekAd,
                        emoji = SayacZincir.emojiOner(gercekAd),
                        sn = dk * 60,
                        odakMi = !SayacZincir.molaBenzeriMi(gercekAd)
                    )
                )
            }
            if (hataVar) return@setOnClickListener

            val tekrar = tekrarPicker.value
            when (val h = SayacZincir.dogrula(evreler, tekrar)) {
                SayacZincir.Hata.YOK -> {
                    val ad = adGirdi.text.toString().trim()
                        .ifEmpty { getString(R.string.zk_varsayilan_ad) }
                    val gercek = SayacZincir.kaydet(
                        ctx, SayacZincir.Zincir(0L, ad, "⛓", evreler, tekrar)
                    )
                    SayacZincir.aktiflestir(ctx, gercek.id)
                    Toast.makeText(
                        ctx,
                        getString(R.string.zk_kaydedildi, gercek.ad),
                        Toast.LENGTH_SHORT
                    ).show()
                    zincirChipiYaz()
                    diyalog.dismiss()
                }
                SayacZincir.Hata.EVRE_YOK ->
                    Toast.makeText(ctx, R.string.zk_hata_evre_yok, Toast.LENGTH_SHORT).show()
                SayacZincir.Hata.FAZLA_EVRE ->
                    Toast.makeText(
                        ctx,
                        getString(R.string.zk_hata_fazla, SayacZincir.MAKS_EVRE),
                        Toast.LENGTH_SHORT
                    ).show()
                SayacZincir.Hata.SURE_GECERSIZ ->
                    Toast.makeText(ctx, R.string.zk_hata_sure, Toast.LENGTH_SHORT).show()
                SayacZincir.Hata.TEKRAR_GECERSIZ ->
                    Toast.makeText(ctx, R.string.zk_hata_tekrar, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun gercektenBaslat(ctx: android.content.Context) {
        running = true
        TimerEngine.setMode(ctx, if (mode == MODE_WATCH) TimerEngine.MODE_WATCH else TimerEngine.MODE_DOWN)
        TimerEngine.start(ctx)
        TimerAlarm.reschedule(ctx)
        mainAction.text = getString(R.string.pause)
        // v10.4 · A9: sesli geri sayım hazırlığı (yalnız geri sayım + ayar açık)
        sesli?.kapat()
        sesli = if (mode == MODE_DOWN && SayacAyar.tts(ctx)) SayacSesli(ctx) else null
        soylenenler.clear()
        handler.removeCallbacks(ticker)
        handler.post(ticker)
    }

    /** v10.2 · A11 — büyük sayılarla 3-2-1, sonra sayaç başlar. */
    private var rituelAktif = false

    private fun geriSayimRitueli(sonra: () -> Unit) {
        val kok = view ?: return sonra()
        rituelAktif = true
        val adimlar = listOf("3", "2", "1")
        var adim = 0
        val is_ = object : Runnable {
            override fun run() {
                // Fragment yok olduysa ritüeli bırak; sayaç da başlamasın
                if (!isAdded || view == null) {
                    rituelAktif = false
                    return
                }
                if (adim < adimlar.size) {
                    runCatching {
                        timeText.text = adimlar[adim]
                        timeText.scaleX = 1.5f
                        timeText.scaleY = 1.5f
                        timeText.alpha = 0.15f
                        timeText.animate()
                            .scaleX(1f).scaleY(1f).alpha(1f)
                            .setDuration(380L)
                            .start()
                        Titresim.dokunus(timeText)
                    }
                    adim++
                    view?.postDelayed(this, 480L)
                } else {
                    rituelAktif = false
                    sonra()
                }
            }
        }
        kok.post(is_)
    }

    private fun pause() {
        val ctx = requireContext()
        TimerEngine.creditWatch(ctx)
        TimerEngine.pause(ctx)
        TimerAlarm.cancel(ctx)
        running = false
        mainAction.text = getString(R.string.resume)
        updateDisplay()
        // v10.50 #1 & #3: Kesinti kaydı ve yorgunluk radarı
        if (SayacAyar.kesintiKaydiAcik(ctx) && mode == MODE_DOWN) {
            kesintiSebepSoruDiyalogu()
        }
    }

    private fun kesintiSebepSoruDiyalogu() {
        val ctx = context ?: return
        val secenekler = arrayOf(
            getString(R.string.om_ks_telefon),
            getString(R.string.om_ks_kapi),
            getString(R.string.om_ks_ihtiyac),
            getString(R.string.om_ks_zihin)
        )
        val sebepler = arrayOf(
            OdakMotoru.KesintiSebep.TELEFON,
            OdakMotoru.KesintiSebep.KAPI,
            OdakMotoru.KesintiSebep.IHTIYAC,
            OdakMotoru.KesintiSebep.ZIHIN
        )
        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.om_kesinti_soru)
            .setItems(secenekler) { _, i ->
                val sebep = sebepler[i]
                SayacAyar.kesintiKaydet(ctx, sebep)
                val kayitlar = SayacAyar.kesintiListesi(ctx)
                val gecenDk = ((countTotal - countRemaining) / 60_000L).toInt()
                val radar = OdakMotoru.yorgunlukRadari(kayitlar.size, gecenDk)
                if (radar.riskliMi) {
                    MaterialAlertDialogBuilder(ctx)
                        .setTitle("🧠 Yorgunluk Radarı")
                        .setMessage(radar.mesaj)
                        .setPositiveButton("5 Dk Mola") { _, _ -> setPreset(5) }
                        .setNegativeButton("Kapat", null)
                        .show()
                } else {
                    Toast.makeText(ctx, "Kaydedildi: ${secenekler[i]}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Geç", null)
            .show()
    }

    private fun resetAll() {
        val ctx = requireContext()
        TimerEngine.creditWatch(ctx)
        TimerEngine.reset(ctx)
        TimerAlarm.cancel(ctx)
        // v10.7 · A6: kullanıcı sayacı sıfırladıysa koşan zincir de
        // duraklasın — yoksa evre bitişi gelmeden zincir takılı kalırdı.
        if (SayacZincir.kosuyor(ctx)) {
            SayacZincir.durdur(ctx)
            zincirChipiYaz()
        }
        running = false
        handler.removeCallbacks(ticker)
        watchAccumulated = 0
        countRemaining = countTotal
        laps.clear()
        renderLaps()
        mainAction.text = getString(R.string.start)
        // v10.24: ileri mod "Bitir" etiketini değiştirir — motora dönünce geri al
        resetButton.setText(R.string.reset)
        lapButton.visibility = if (mode == MODE_WATCH) View.VISIBLE else View.GONE
        presetRow.visibility = if (mode == MODE_DOWN) View.VISIBLE else View.GONE
        updateDisplay()
    }

    private fun setPreset(minutes: Int) {
        val ctx = requireContext()
        // v10.7 · A6: zincir koşarken preset/evre süresi elle değişmez —
        // senkron bozulur. Önce çipten duraklatılır.
        if (SayacZincir.kosuyor(ctx)) {
            Toast.makeText(ctx, R.string.zk_kosuyor_uyari, Toast.LENGTH_SHORT).show()
            return
        }
        running = false
        handler.removeCallbacks(ticker)
        countTotal = minutes * 60_000L
        countRemaining = countTotal
        TimerEngine.setTotalMs(ctx, countTotal)
        TimerAlarm.cancel(ctx)
        TimerNotifier.cancel(ctx)
        mainAction.text = getString(R.string.start)
        updateDisplay()
    }

    private fun showCustomPicker() {
        val picker = NumberPicker(requireContext()).apply {
            minValue = 1
            maxValue = 180
            value = (countTotal / 60_000L).toInt()
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.custom_minutes_title)
            .setView(picker)
            .setPositiveButton(R.string.save) { _, _ -> setPreset(picker.value) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun renderLaps() {
        if (laps.isEmpty()) {
            lapCard.visibility = View.GONE
            return
        }
        lapCard.visibility = View.VISIBLE
        lapText.text = laps.mapIndexed { index, lap ->
            getString(R.string.lap_format, laps.size - index, formatTime(lap))
        }.joinToString("\n")
    }

    // ---------------- Geri sayım bitti ----------------

    private fun onCountdownFinished() {
        running = false
        handler.removeCallbacks(ticker)
        countRemaining = countTotal
        // Motor tarafını da kapat (alarm zaten tetiklenmiş olabilir)
        TimerEngine.reset(requireContext())
        TimerAlarm.cancel(requireContext())
        mainAction.text = getString(R.string.start)
        updateDisplay()

        val context = requireContext()
        val minutes = (countTotal / 60_000L).toInt().coerceAtLeast(1)
        Store.addTodayFocusMinutes(context, minutes)
        refreshTodayStat()

        // v11.13: bitiş sesi + titreşim merkez motordan çalınır.
        // Motor döngülü alarm sesi çalar ve güç düğmesine basılınca
        // (ACTION_SCREEN_OFF) sesi + titreşimi anında susturur; otomatik
        // süre (SayacAyar.sesSureSn) sonunda da kendiliğinden susar.
        // Motor kendi ayar denetimini (sesÇalınsınMı, titreşimEtkinMi) yapar.
        if (Store.getSoundEnabled(context) || Store.getVibEnabled(context)) {
            BitisSesMotoru.cal(context)
        }

        // Bildirim (ayara bağlı — uygulama arka plandayken de görünür)
        if (Store.getNotifEnabled(context)) {
            try {
                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_timer)
                    .setContentTitle(getString(R.string.time_up_title))
                    .setContentText(getString(R.string.time_up_text))
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build()
                NotificationManagerCompat.from(context).notify(NOTIF_ID, notification)
            } catch (_: Exception) {
                Toast.makeText(context, R.string.time_up_title, Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, R.string.time_up_title, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * v7.90 — Sayaç bitiş kanalı.
     *
     * ── Neden hâlâ ayrı bir kanal ──
     * Bu kanal yalnızca "süre doldu" bildirimi için. Canlı sayaç kanalı
     * ([TimerNotifier.CHANNEL_ID]) sessiz ve düşük profilli olmalı; bitiş
     * bildiriminin ise dikkat çekmesi gerekiyor. İkisini tek kanalda
     * toplamak, birinin ayarını değiştirince diğerini bozardı.
     *
     * ── v7.90 düzeltmesi ──
     * Kanal her `onViewCreated`'da yeniden oluşturuluyordu. Android var
     * olan kanalı yeniden oluşturmayı yok sayar, ama gereksiz sistem
     * çağrısı. Artık yoksa kuruluyor.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val yonetici = requireContext()
            .getSystemService(NotificationManager::class.java) ?: return
        if (yonetici.getNotificationChannel(CHANNEL_ID) != null) return
        yonetici.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.timer_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            )
        )
    }

    private fun refreshTodayStat() {
        odakSatiriYaz()
        // v10.12 · Grup D: maç satırı + seans halkası + hedef kutlaması
        val ctx = context ?: return
        macTazele(zorla = true)
        kutlamaKontrol(ctx)
    }

    /**
     * v10.27 (öneri #76) — tek satırda gün + hafta ilerlemesi.
     * Haftalık hedef ayarsızdır: günlük hedef × 7 (OdakHafta).
     * Manuel ekleme ve İleri Sayım kaydı da bu satırdan geçer.
     */
    private fun odakSatiriYaz() {
        if (!::todayStat.isInitialized) return
        val ctx = context ?: return
        val bugun = Store.getTodayFocusMinutes(ctx)
        val hafta = Store.weekFocus(ctx)
        val hedef = OdakHafta.haftalikHedef(Store.getGoalMinutes(ctx))
        todayStat.text = getString(
            R.string.w24_hafta_odak, bugun, hafta, hedef, OdakHafta.yuzde(hafta, hedef)
        )
    }

    /**
     * v10.12 · D20/D23 — Maç satırı, hayalet yaylar ve seans işaretleri.
     *
     * Oturum günlüğünün JSON ayrıştırması ucuz ama 100 ms tike bedava
     * değil; 5 saniyelik kısıtla çalışır, "zorla" ile anında tazelenir.
     */
    private fun macTazele(zorla: Boolean = false) {
        val ctx = context ?: return
        if (!::ghostText.isInitialized) return
        val simdi = System.currentTimeMillis()
        if (!zorla && simdi - sonMacMs < 5000L) return
        sonMacMs = simdi

        val mac = Hayalet.mac(ctx)
        if (mac == null) {
            ghostText.visibility = View.GONE
            sonMacSen = 0f
            sonMacRakip = 0f
        } else {
            ghostText.visibility = View.VISIBLE
            ghostText.text = Hayalet.metin(ctx, mac)
            sonMacSen = mac.senOran
            sonMacRakip = mac.rakipOran
        }
        val hedef = OdakRitim.hedef(ctx)
        sonIsaretDolu = OdakRitim.doluIsaret(OdakRitim.bugunSeans(ctx), hedef)
        sonIsaretToplam = hedef
        if (::kadran.isInitialized) {
            kadran.isaretleriAyarla(sonIsaretDolu, sonIsaretToplam)
            kadran.maciAyarla(sonMacSen, sonMacRakip)
        }
    }

    /** v10.12 · D23 — Hedefe ilk varışta günde bir kez kutlama + parlama. */
    private fun kutlamaKontrol(ctx: android.content.Context) {
        val hedef = OdakRitim.hedef(ctx)
        if (hedef <= 0) return
        val bugun = OdakRitim.bugunSeans(ctx)
        if (OdakRitim.kutlamaGerekliMi(bugun, hedef, OdakRitim.kutlandiMi(ctx))) {
            OdakRitim.kutlamayiIsaretle(ctx)
            if (::kadran.isInitialized) kadran.bitisParlamasi()
            Toast.makeText(ctx, R.string.fo_ritim_tamam, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pipAlici?.let { runCatching { context?.unregisterReceiver(it) } }
        pipAlici = null
        // v6.4: Sekme değişimi / geri tuşu sayacı DURDURMAZ.
        // Durum TimerEngine'de yaşar, bildirim üzerinden takip edilir.
        handler.removeCallbacksAndMessages(null)
        // v10.12 · D22: sayaç koşarken manzara sürer; sayaç duruyorsa bu
        // yalnızca ön dinlemeydi — ekran kapanınca ses de susar.
        runCatching { SesManzarasi.ekranKapandi(requireContext()) }
        eqView?.stop()
        // v10.4 · A9: ekrandan çıkınca sesli geri sayım da kapansın
        sesli?.kapat()
        sesli = null
    }

    /** v10.19 · S2 — bugünkü odak satırının yanına "manuel ekle" çipi. */
    private fun manuelOdakCipiniKur(kok: View) {
        val hedef = kok.findViewById<View>(R.id.todayStat) ?: return
        val ust = hedef.parent as? android.view.ViewGroup ?: return
        val ctx = requireContext()
        val yg = resources.displayMetrics.density
        val cip = TextView(ctx).apply {
            text = getString(R.string.mo_chip)
            textSize = 12f
            gravity = android.view.Gravity.CENTER
            setPadding(
                (10 * yg).toInt(), (6 * yg).toInt(),
                (10 * yg).toInt(), (6 * yg).toInt()
            )
            val vurgu = com.google.android.material.color.MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorPrimary, 0
            )
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 14 * yg
                setColor((vurgu and 0x00FFFFFF) or 0x22000000)
                setStroke((1f * yg).toInt(), (vurgu and 0x00FFFFFF) or 0x55000000)
            }
            setTextColor(vurgu)
            isClickable = true
            setOnClickListener { manuelOdakDiyalog() }
        }
        val idx = ust.indexOfChild(hedef)
        ust.addView(cip, (idx + 1).coerceAtMost(ust.childCount))
    }

    /** v10.19 · S2 — dakika seçiciyle gün toplamına manuel odak ekler. */
    private fun manuelOdakDiyalog() {
        val ctx = requireContext()
        val secici = android.widget.NumberPicker(ctx).apply {
            minValue = 1
            maxValue = 480
            value = 25
            wrapSelectorWheel = true
        }
        androidx.appcompat.app.AlertDialog.Builder(ctx)
            .setTitle(R.string.mo_baslik)
            .setMessage(
                getString(R.string.mo_su_an, Store.getTodayFocusMinutes(ctx)) +
                    "\n\n" + getString(R.string.mo_not)
            )
            .setView(secici)
            .setPositiveButton(R.string.save) { _, _ ->
                val dk = OdakManuel.kelepcele(secici.value)
                Store.addTodayFocusMinutes(ctx, dk)
                runCatching { WidgetCommon.refreshAll(ctx, false) }
                // v10.27: manuel ekleme de tek satır birleşik gösterime gider
                odakSatiriYaz()
                macTazele(zorla = true)
                Toast.makeText(
                    ctx, getString(R.string.mo_eklendi, dk), Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ═══════════════════════════════════════════════════════════════
    // v10.50 — Göreve Bağlı Sayaç (#2) & Proje Bütçesi (#8) & Çıktı Hasadı (#5)
    // ═══════════════════════════════════════════════════════════════

    private var bagliGorevId: Long = 0L
    private var bagliGorevAd: String? = null
    private var bagliProjeAd: String? = null
    private var gorevCipi: TextView? = null
    private var projeCipi: TextView? = null

    private fun odakBaglantiCipleriniKur(kok: View) {
        val hedef = kok.findViewById<View>(R.id.todayStat) ?: return
        val ust = hedef.parent as? android.view.ViewGroup ?: return
        val ctx = requireContext()
        val yg = resources.displayMetrics.density

        fun cipOlustur(varsayilanMetin: String, tıkla: () -> Unit): TextView {
            return TextView(ctx).apply {
                text = varsayilanMetin
                textSize = 12f
                gravity = android.view.Gravity.CENTER
                setPadding((10 * yg).toInt(), (6 * yg).toInt(), (10 * yg).toInt(), (6 * yg).toInt())
                val vurgu = com.google.android.material.color.MaterialColors.getColor(
                    this, com.google.android.material.R.attr.colorPrimary, 0
                )
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = 14 * yg
                    setColor((vurgu and 0x00FFFFFF) or 0x22000000)
                    setStroke((1f * yg).toInt(), (vurgu and 0x00FFFFFF) or 0x55000000)
                }
                setTextColor(vurgu)
                isClickable = true
                setOnClickListener { tıkla() }
            }
        }

        gorevCipi = cipOlustur(getString(R.string.om_gorev_bagla)) { gorevBaglaDiyalogu() }
        projeCipi = cipOlustur(getString(R.string.om_proje_bagla)) { projeBaglaDiyalogu() }

        val sat = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_HORIZONTAL
            val p = (4 * yg).toInt()
            setPadding(0, p, 0, p)
            addView(gorevCipi, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = p
            })
            addView(projeCipi, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = p
            })
            if (GorunumAyar.sayacAltMenu(ctx)) {
                kok.findViewById<View>(R.id.fullscreenButton)?.visibility = View.GONE
                kok.findViewById<View>(R.id.nefesButton)?.visibility = View.GONE
                kok.findViewById<View>(R.id.miniModButton)?.visibility = View.GONE
                val menuCipi = cipOlustur("⋮ Diğer Araçlar") {
                    val secenekler = arrayOf(
                        "🖥️ Tam Ekran Fliqlo Saat",
                        "🌬️ 4-7-8 Nefes Stüdyosu",
                        "▦ PiP Mini Mod Penceresi",
                        "📋 Görev Bağla (${bagliGorevAd ?: "Yok"})",
                        "📁 Proje / Ders Seç (${bagliProjeAd ?: "Yok"})",
                        "⚙️ Zamanlayıcı Ayarları"
                    )
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Odak & Zamanlayıcı Araçları")
                        .setItems(secenekler) { _, idx ->
                            when (idx) {
                                0 -> openFullscreen()
                                1 -> startActivity(android.content.Intent(requireContext(), NefesActivity::class.java))
                                2 -> pipGir()
                                3 -> gorevBaglaDiyalogu()
                                4 -> projeBaglaDiyalogu()
                                5 -> SayacAyarActivity.ac(requireContext())
                            }
                        }
                        .show()
                }
                addView(menuCipi, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = p
                })
            }
        }
        val idx = ust.indexOfChild(hedef)
        ust.addView(sat, if (idx >= 0) idx + 1 else ust.childCount)
    }

    private fun gorevBaglaDiyalogu() {
        val gorevler = Store.loadTasks(requireContext()).filter { !it.done }
        if (gorevler.isEmpty()) {
            Toast.makeText(requireContext(), "Listede bekleyen görev yok", Toast.LENGTH_SHORT).show()
            return
        }
        val adlar = gorevler.map { it.text }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.om_gorev_bagla))
            .setItems(adlar) { _, i ->
                val g = gorevler[i]
                bagliGorevId = g.id
                bagliGorevAd = g.text
                gorevCipi?.text = "📋: " + g.text.take(16)
                Toast.makeText(requireContext(), "Görev bağlandı: ${g.text}", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Bağlantıyı Kaldır") { _, _ ->
                bagliGorevId = 0L
                bagliGorevAd = null
                gorevCipi?.text = getString(R.string.om_gorev_bagla)
            }
            .show()
    }

    private fun projeBaglaDiyalogu() {
        val konular = Store.loadTopics(requireContext()).map { it.title }.distinct()
        val varsayilanlar = listOf("Revit Eğitimi", "Autocad", "İngilizce", "Matematik", "Genel Çalışma")
        val liste = (konular + varsayilanlar).distinct().toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.om_proje_bagla))
            .setItems(liste) { _, i ->
                bagliProjeAd = liste[i]
                projeCipi?.text = "📁: " + liste[i].take(14)
                Toast.makeText(requireContext(), "Proje seçildi: ${liste[i]}", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Kaldır") { _, _ ->
                bagliProjeAd = null
                projeCipi?.text = getString(R.string.om_proje_bagla)
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        // v7.86: "ekran açık kalsın" ayarı
        ekranBayragi()
        // v10.4 · A5: etiket başka ekrandan değiştiyse çip tazelensin
        if (::presetRow.isInitialized) etiketYaz()
        view?.let {
            initKpssSayacAtolye(it)
            guncellePresetButonlari(it)
            guncelleOdakSesVeTemaGorunurlugu(it)
            initArkaPlanMedyaKumandasi(it)
        } // v10.78, v10.83 & v10.84: Sayaç çipleri, özel preset, tema görünürlüğü ve arka plan medya kumandası tazelensin
        // v10.7 · A6: zincir başka ekrandan/bildirimden ilerlediyse de görünsün
        zincirChipiYaz()
        // Motordaki güncel duruma göre arayüzü tazele
        val ctx = requireContext()
        running = TimerEngine.isRunning(ctx)
        // v10.4 · A9: sayaç dışarıdan (bildirim/widget) başlatılmışsa
        // motor ekrana geldiğinde devreye girsin; geçmiş eşikler zaten
        // söylenmiş kabul edilir (küme boş ama eşikler geleceğe bakar).
        if (sesli == null && running && mode == MODE_DOWN && SayacAyar.tts(ctx)) {
            sesli = SayacSesli(ctx)
            soylenenler.clear()
        }
        // v10.24: açık ileri sayım oturumu, motor modunun önüne geçer
        mode = when {
            IleriSayim.aktifMi(ctx) -> MODE_ILE
            TimerEngine.mode(ctx) == TimerEngine.MODE_WATCH -> MODE_WATCH
            else -> MODE_DOWN
        }
        countTotal = TimerEngine.totalMs(ctx)
        if (mode == MODE_ILE) {
            ileriUiTazele()
        } else {
            mainAction.text = when {
                running -> getString(R.string.pause)
                TimerEngine.displayMs(ctx) != countTotal -> getString(R.string.resume)
                else -> getString(R.string.start)
            }
        }
        // v10.41: ölçek değiştiyse dönüşte devreye girsin
        if (::kadran.isInitialized) kadran.yaziOlcek = SayacAyar.kadranOlcek(ctx)
        // v10.41 · Kullanıcı maddesi #1: ileri sayım bildirimi durumla eşitlenir
        IleriSayimBildirim.tazele(ctx)
        updateDisplay()
        // v10.12 · Grup D: dönüşte maç satırı hemen doğru görünsün
        macTazele(zorla = true)
        refreshNotifBanner()
        if (running || (mode == MODE_ILE && IleriSayim.calismakta(ctx))) {
            handler.removeCallbacks(ticker)
            handler.post(ticker)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // v10.45/v10.46 — Kullanıcı maddesi #7 & #8: mini uygulama (Picture-in-Picture)
    // ═══════════════════════════════════════════════════════════════
    //
    // Kullanıcı isteği: "Zamanlayıcı açıkken uygulama mini hâline
    // gelebilsin, diğer taraftan başka işlerimi halledebileyim."
    // Sistem PiP'i kullanılır — sayaç Activity yaşadığı için tik
    // kesintisiz sürer; geri sayım/kronometre/ileri sayım üçü de
    // zaten damga-temelli olduğundan pencere küçülse de doğru kalır.
    // v10.46: PiP modundayken ekrandaki butonlar gizlenip kadran ölçeklenir,
    // kontrol sistemin sağladığı RemoteAction (Başlat/Bekle, Sıfırla, +5dk) ile yapılır.

    /** PiP'te gizlenen satırlar — kadran kalır, ekrandaki butonlar gizlenir. */
    private val pipGizli = intArrayOf(
        R.id.notifBanner, R.id.modeToggle, R.id.presetRow, R.id.soundRow,
        R.id.lapButton, R.id.lapCard, R.id.fullscreenButton, R.id.nefesButton,
        R.id.timerSettings, R.id.todayStat, R.id.ghostText, R.id.eqView,
        R.id.miniModButton, R.id.mainAction, R.id.resetButton
    )

    private fun pipGir() {
        val a = activity ?: return
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O ||
            !a.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE)
        ) {
            Toast.makeText(context, R.string.w45_pip_yok, Toast.LENGTH_LONG).show()
            return
        }
        runCatching {
            val oran = MiniMod.pipOrani(
                resources.displayMetrics.widthPixels,
                resources.displayMetrics.heightPixels
            )
            val params = android.app.PictureInPictureParams.Builder()
                .setAspectRatio(android.util.Rational(oran.first, oran.second))
                .setActions(pipAksiyonListesi())
                .build()
            a.enterPictureInPictureMode(params)
        }.onFailure {
            Toast.makeText(context, R.string.w45_pip_yok, Toast.LENGTH_LONG).show()
        }
    }

    override fun onPictureInPictureModeChanged(pip: Boolean) {
        super.onPictureInPictureModeChanged(pip)
        val v = view ?: return
        pipGizli.forEach { id ->
            v.findViewById<View>(id)?.visibility = if (pip) View.GONE else View.VISIBLE
        }
        val kadran = v.findViewById<View>(R.id.sayacKadran)
        kadran?.scaleX = MiniMod.pipOlcegi(pip)
        kadran?.scaleY = MiniMod.pipOlcegi(pip)
        val dolgu = dp(MiniMod.pipDolguDp(pip))
        (v as? android.view.ViewGroup)?.getChildAt(0)?.setPadding(dolgu, dolgu, dolgu, dolgu)
        if (pip) {
            pipAksiyonlariniGuncelle()
        }
    }

    private fun pipAksiyonListesi(): List<android.app.RemoteAction> {
        val ctx = context ?: return emptyList()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return emptyList()
        val calisiyor = if (mode == MODE_ILE) IleriSayim.calismakta(ctx) else TimerEngine.isRunning(ctx)
        val geriSayimMi = (mode == MODE_DOWN)
        val kodlar = MiniMod.aksiyonKodlari(calisiyor, geriSayimMi)
        val list = mutableListOf<android.app.RemoteAction>()
        kodlar.forEach { kod ->
            val (ikonRes, baslikRes) = when (kod) {
                101 -> if (calisiyor) {
                    android.R.drawable.ic_media_pause to R.string.pause
                } else {
                    android.R.drawable.ic_media_play to R.string.start
                }
                102 -> android.R.drawable.ic_menu_close_clear_cancel to R.string.reset
                103 -> android.R.drawable.ic_input_add to R.string.sb_uzat
                else -> android.R.drawable.ic_media_play to R.string.start
            }
            val intent = android.content.Intent(ACTION_PIP_KONTROL)
                .setPackage(ctx.packageName)
                .putExtra(EXTRA_PIP_KOD, kod)
            val pi = android.app.PendingIntent.getBroadcast(
                ctx,
                kod,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            val ikon = android.graphics.drawable.Icon.createWithResource(ctx, ikonRes)
            val bMetin = getString(baslikRes)
            list.add(android.app.RemoteAction(ikon, bMetin, bMetin, pi))
        }
        return list
    }

    private fun pipAksiyonlariniGuncelle() {
        val a = activity ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !a.isInPictureInPictureMode) return
        runCatching {
            val params = android.app.PictureInPictureParams.Builder()
                .setActions(pipAksiyonListesi())
                .build()
            a.setPictureInPictureParams(params)
        }
    }

    private fun pipTazele() {
        if (activity?.isInPictureInPictureMode == true) {
            val ctx = context ?: return
            val calisiyor = if (mode == MODE_ILE) IleriSayim.calismakta(ctx) else TimerEngine.isRunning(ctx)
            if (sonPipCalisiyorState != calisiyor) {
                sonPipCalisiyorState = calisiyor
                pipAksiyonlariniGuncelle()
            }
        }
    }

    private fun pipAliciKur() {
        val ctx = context ?: return
        val alici = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                if (intent?.action != ACTION_PIP_KONTROL) return
                val ctxIn = context ?: return
                when (intent.getIntExtra(EXTRA_PIP_KOD, 0)) {
                    101 -> {
                        if (mode == MODE_ILE) {
                            ileriAnaDugme()
                        } else if (TimerEngine.isRunning(ctxIn)) {
                            pause()
                        } else {
                            start()
                        }
                    }
                    102 -> {
                        if (mode == MODE_ILE) ileriBitirKaydet() else resetAll()
                    }
                    103 -> {
                        if (mode == MODE_DOWN) {
                            TimerEngine.uzat(ctxIn, 5 * 60_000L)
                            TimerNotifier.show(ctxIn)
                        }
                    }
                }
                pipAksiyonlariniGuncelle()
                updateDisplay()
            }
        }
        pipAlici = alici
        val filtre = android.content.IntentFilter(ACTION_PIP_KONTROL)
        if (Build.VERSION.SDK_INT >= 33) {
            ctx.registerReceiver(alici, filtre, android.content.Context.RECEIVER_NOT_EXPORTED)
        } else {
            ctx.registerReceiver(alici, filtre)
        }
    }

    /** Bildirim kapalıysa uyarı şeridini gösterir. */
    private fun refreshNotifBanner() {
        val v = view?.findViewById<View>(R.id.notifBanner) ?: return
        val ctx = context ?: return
        v.visibility = if (TimerNotifier.isReady(ctx)) View.GONE else View.VISIBLE
    }

    /** Sistemin bildirim ayarları ekranını açar. */
    private fun openNotificationSettings() {
        val ctx = context ?: return
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                startActivity(
                    android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                )
            } else {
                startActivity(
                    android.content.Intent(
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        android.net.Uri.parse("package:" + ctx.packageName)
                    )
                )
            }
        } catch (_: Exception) {
            Toast.makeText(ctx, R.string.tn_banner_hint, Toast.LENGTH_LONG).show()
        }
    }

    override fun onPause() {
        super.onPause()
        // v7.86: bayrağı bırak — diğer sekmelerde ekran sönebilsin
        runCatching {
            activity?.window?.clearFlags(
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        // v10.12 · D22: sayaç koşuyorsa manzara arka planda da sürer;
        // koşmuyorsa ön dinleme sessizce kapanır (pil dostu 🌱).
        if (view != null) {
            runCatching { SesManzarasi.ekranKapandi(requireContext()) }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // v10.24 — İleri Sayım (MODE_ILE): damga-temelli, motorsuz mod
    // ═══════════════════════════════════════════════════════════
    //
    // Kullanıcı isteği: "...bekle dersem bekletsin, dursun orda; ekranı
    // kapatsam bile sonra devam ettirebileyim; durdurunca dakikayı ders
    // saati yerine eklesin." Durum IleriSayim deposunda (prefs) yaşar —
    // ekran/süreç kapansa da birikim ve çalışma damgası kaybolmaz. Motor,
    // alarm, bildirim ve zincir BU MODDA DEVREYE GİRMEZ; dokunulan tek
    // şey görünüm katmanıdır (v10.19 sayaç sıfırlanma dersi: bölge kırılgan).

    /** Büyük saat + halka + düğmeler — ileri sayımın tüm görünümü. */
    private fun displayIleri(gecenMs: Long) {
        Rulo.yaz(timeText, formatTime(gecenMs))
        kadranIleriTazele(gecenMs)
        ileriUiTazele()
    }

    /** Halka kronometre gibi dolar; üst/alt metinler ileri sayıma aittir. */
    // v10.28 · Katalog #62: alt satırda bugün/dün toplamı — tik'te IO olmasın
    // diye yalnız mod geçişinde ve kayıtta tazelenen önbellek.
    private var gecmisBugunDk = 0
    private var gecmisDunDk = 0

    private fun gecmisOzetTazele() {
        val c = context ?: return
        gecmisBugunDk = IleriSayim.bugunToplam(c)
        gecmisDunDk = IleriSayim.dunToplam(c)
    }

    private fun kadranIleriTazele(millis: Long) {
        if (!::kadran.isInitialized) return
        val ctx = context ?: return
        // Maç/hayalet katmanı motor modlarıyla aynı önbelleği kullanır
        kadran.isaretleriAyarla(sonIsaretDolu, sonIsaretToplam)
        kadran.maciAyarla(sonMacSen, sonMacRakip)
        val calisiyor = IleriSayim.calismakta(ctx)
        val saniye = (millis / 1000L) % 60L
        val dk = IleriSayim.dakikayaDonustur(millis)
        val alt = when {
            millis <= 0L && (gecmisBugunDk > 0 || gecmisDunDk > 0) ->
                getString(R.string.w28_gecmis_ozet, gecmisBugunDk, gecmisDunDk)
            millis <= 0L -> getString(R.string.w23_alt_ipucu)
            !calisiyor -> getString(R.string.w23_alt_beklemede, dk)
            dk > 0 -> getString(R.string.w23_alt_birikti, dk)
            else -> getString(R.string.w23_alt_calisiyor)
        }
        kadran.guncelle(
            kalanOran = saniye / 60f,
            sure = formatTime(millis),
            // v10.26: isimli oturumda üst satır oturum adına aittir
            ust = IleriSayim.ad(ctx).ifBlank { getString(R.string.w23_ust) },
            alt = alt,
            aktif = calisiyor
        )
    }

    /** Düğme etiketleri/görünürlük: Başlat ⇄ Bekle ⇄ Devam + Bitir. */
    private fun ileriUiTazele() {
        if (!::mainAction.isInitialized || !::resetButton.isInitialized) return
        if (!::lapButton.isInitialized || !::presetRow.isInitialized) return
        if (!::lapCard.isInitialized) return
        val ctx = context ?: return
        val durum = IleriSayim.durum(ctx)
        mainAction.text = when {
            durum.calisiyor -> getString(R.string.w23_bekle)
            durum.birikenMs > 0L -> getString(R.string.w23_devam)
            else -> getString(R.string.w23_baslat)
        }
        val dk = IleriSayim.dakikayaDonustur(
            IleriSayim.gecenMs(durum, System.currentTimeMillis())
        )
        resetButton.text = getString(R.string.w23_bitir_format, dk)
        lapButton.visibility = View.GONE
        lapCard.visibility = View.GONE
        presetRow.visibility = View.GONE
        gecmisOzetTazele()
    }

    /** Ana düğme: Başlat ⇄ Bekle ⇄ Devam — geçiş damga-anında depoya yazılır. */
    private fun ileriAnaDugme() {
        val ctx = context ?: return
        val simdi = System.currentTimeMillis()
        IleriSayim.anaDugme(ctx, simdi)
        runCatching { Titresim.dokunus(mainAction) }
        // Pil dostu tik: yalnız çalışırken sürer, beklerken durur
        handler.removeCallbacks(ticker)
        if (IleriSayim.calismakta(ctx)) handler.post(ticker)
        // v10.41: beklet/devam geçişi canlı bildirime de yansır
        IleriSayimBildirim.tazele(ctx)
        updateDisplay()
    }

    /**
     * Bitir: dakikayı ders saatine yazar ve sayacı sıfırlar.
     * Kayıt kanalı v10.19 manuel odakla BİREBİR aynıdır
     * (Store.addTodayFocusMinutes + WidgetCommon.refreshAll) — kullanıcının
     * "odaklanma saati yerine yaz" dediği yerin ta kendisi.
     * 1 dakikadan kısaysa hiçbir şey kaybolmaz; süre bekletilir.
     */
    private fun ileriBitirKaydet() {
        val ctx = context ?: return
        val simdi = System.currentTimeMillis()
        // Çalışan sayaç önce damga-anında bekletilsin ki dakika tam hesaplansın
        if (IleriSayim.calismakta(ctx)) IleriSayim.anaDugme(ctx, simdi)
        handler.removeCallbacks(ticker)
        val dk = IleriSayim.bekleyenDakika(ctx, simdi)
        when {
            dk < 1 -> {
                Toast.makeText(ctx, R.string.w23_kisa, Toast.LENGTH_LONG).show()
                updateDisplay()
            }
            IleriSayim.onayGerekliMi(dk) -> {
                MaterialAlertDialogBuilder(ctx)
                    .setTitle(R.string.w23_uzun_baslik)
                    .setMessage(getString(R.string.w23_uzun_mesaj, dk))
                    .setPositiveButton(R.string.w23_uzun_ekle) { _, _ ->
                        ileriKaydetVeSifirla(ctx, dk)
                    }
                    .setNegativeButton(R.string.cancel) { _, _ ->
                        // Vazgeç: süre bekletilmiş durumda korunur, devam edilebilir
                        updateDisplay()
                    }
                    .show()
            }
            else -> ileriKaydetVeSifirla(ctx, dk)
        }
    }

    /** v10.19 manuel odakla birebir aynı kayıt + widget/istatistik tazeleme. */
    private fun ileriKaydetVeSifirla(ctx: android.content.Context, dk: Int) {
        // İsim sıfırlamadan ÖNCE okunur; toast'a " · Matematik" eki düşer.
        val adEk = IleriSayim.ad(ctx).let { if (it.isBlank()) "" else " · $it" }
        Store.addTodayFocusMinutes(ctx, dk)
        // v10.28 · Katalog #62: tamamlanan oturum kalıcı geçmişe işlenir
        IleriSayim.gecmiseIsle(ctx, System.currentTimeMillis(), dk)
        gecmisOzetTazele()
        IleriSayim.sifirla(ctx)
        IleriSayimBildirim.gizle(ctx)
        runCatching { WidgetCommon.refreshAll(ctx, false) }
        updateDisplay()
        refreshTodayStat()
        Toast.makeText(ctx, getString(R.string.w23_kaydedildi, dk, adEk), Toast.LENGTH_LONG).show()
    }

    /**
     * v10.28 · Katalog #62 — İleri düğmesine uzun basış menüsü:
     * adlandırma diyaloğu, oturum geçmişi, (varsa) adı temizleme.
     */
    private fun ileriUzunMenu() {
        val ctx = context ?: return
        val adVar = IleriSayim.ad(ctx).isNotBlank()
        val secenekler = mutableListOf(
            getString(R.string.w28_menu_adlandir),
            getString(R.string.w28_menu_gecmis)
        )
        if (adVar) secenekler.add(getString(R.string.w24_ileri_ad_temizle))
        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.w23_ust)
            .setItems(secenekler.toTypedArray()) { _, i ->
                when (secenekler[i]) {
                    getString(R.string.w28_menu_adlandir) -> ileriAdDiyalog()
                    getString(R.string.w28_menu_gecmis) -> ileriGecmisDiyalog(ctx)
                    else -> {
                        IleriSayim.adYaz(ctx, "")
                        updateDisplay()
                    }
                }
            }
            .show()
    }

    /** v10.28 · Katalog #62: kayıtlı oturumların listesi + bugün/dün özeti. */
    private fun ileriGecmisDiyalog(ctx: android.content.Context) {
        val liste = IleriSayim.gecmis(ctx)
        val kurucu = MaterialAlertDialogBuilder(ctx).setTitle(R.string.w28_gecmis_baslik)
        if (liste.isEmpty()) {
            kurucu.setMessage(R.string.w28_gecmis_bos)
                .setPositiveButton(R.string.ok, null)
                .show()
            return
        }
        val fmt = java.text.SimpleDateFormat("d MMM EEE · HH:mm", java.util.Locale("tr", "TR"))
        val satirlar = liste.take(30).joinToString("\n") {
            getString(R.string.w28_gecmis_madde, fmt.format(java.util.Date(it.bitisMs)), it.dakika)
        }
        val ozet = getString(
            R.string.w28_gecmis_ozet, IleriSayim.bugunToplam(ctx), IleriSayim.dunToplam(ctx)
        )
        kurucu.setMessage(ozet + "\n\n" + satirlar)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    /**
     * v10.26 · öneri #61 — İleri Sayım oturumuna isim verme diyalogu.
     * İleri düğmesine uzun basışla açılır. Boş kaydetmek ismi siler;
     * isim kadranın üst satırında ve Bitir toastında görünür.
     */
    private fun ileriAdDiyalog() {
        val ctx = context ?: return
        val giris = android.widget.EditText(ctx).apply {
            setText(IleriSayim.ad(ctx))
            hint = getString(R.string.w24_ileri_ad_hint)
            setSingleLine(true)
            setSelectAllOnFocus(true)
        }
        val kutu = android.widget.FrameLayout(ctx).apply {
            val bo = (20 * resources.displayMetrics.density).toInt()
            setPadding(bo, 0, bo, 0)
            addView(giris)
        }
        val kurucu = MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.w24_ileri_ad_baslik)
            .setView(kutu)
            .setPositiveButton(R.string.save) { _, _ ->
                IleriSayim.adYaz(ctx, giris.text.toString())
                updateDisplay()
            }
            .setNegativeButton(R.string.cancel, null)
        if (IleriSayim.ad(ctx).isNotBlank()) {
            kurucu.setNeutralButton(R.string.w24_ileri_ad_temizle) { _, _ ->
                IleriSayim.adYaz(ctx, "")
                updateDisplay()
            }
        }
        val diyalog = kurucu.show()
        // Klavye açık gelsin — kullanıcı uzun bastı, yazacağı belli
        diyalog.window?.setSoftInputMode(
            android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
        )
        giris.requestFocus()
    }

    private fun initKpssSayacAtolye(view: View) {
        val pill = view.findViewById<TextView>(R.id.txtSayacOturumPill) ?: return
        val btnDers = view.findViewById<Button>(R.id.btnSayacDersSec) ?: return
        val btnStat = view.findViewById<Button>(R.id.btnSayacIstatistikGor) ?: return

        fun guncelle() {
            pill.text = KpssSayacAtolye.oturumMetniGetir(requireContext())
            btnDers.text = KpssSayacAtolye.seciliDersGetir(requireContext())
        }
        guncelle()

        pill.setOnClickListener {
            KpssSayacAtolye.sonrakiOturumaGec(requireContext())
            guncelle()
            android.widget.Toast.makeText(requireContext(), pill.text, android.widget.Toast.LENGTH_SHORT).show()
        }

        btnStat.setOnClickListener {
            KpssSayacIstatistikActivity.ac(requireContext())
        }

        btnDers.setOnClickListener {
            val dersler = KpssSayacAtolye.desteklenenDersler(requireContext())
            val layout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(40, 20, 40, 10)
            }
            lateinit var dlg: androidx.appcompat.app.AlertDialog
            for (d in dersler) {
                val b = Button(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                    text = d
                    setOnClickListener {
                        KpssSayacAtolye.dersSecKaydet(requireContext(), d)
                        guncelle()
                        dlg.dismiss()
                        android.widget.Toast.makeText(requireContext(), "📚 Çalışılan Ders: $d", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                layout.addView(b)
            }
            val btnRow = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.END
            }
            val btnTemizle = Button(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                text = "Temizle"
                setOnClickListener {
                    KpssSayacAtolye.temizleSeciliDers(requireContext())
                    guncelle()
                    dlg.dismiss()
                }
            }
            val btnKapat = Button(requireContext()).apply {
                text = "Kapat"
                setOnClickListener { dlg.dismiss() }
            }
            btnRow.addView(btnTemizle)
            btnRow.addView(btnKapat)
            layout.addView(btnRow)

            dlg = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("📚 Ders Seçimi")
                .setView(layout)
                .show()
        }
    }
}
