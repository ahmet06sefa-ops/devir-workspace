package com.gunlukasistan.app

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.util.concurrent.Executors
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors

/**
 * AI Asistan sohbet ekranı (çevrimdışı, veri cihazdan çıkmaz).
 * Asistan cevapları eylem butonları içerebilir (örn. planı görevlere ekle).
 */
class AsistanFragment : Fragment(R.layout.fragment_asistan) {

    private var greeted = false
    private var recognizer: SpeechRecognizer? = null
    /** v11.13: asistan cevaplarını sesli okuma (TTS) — ekran öndeyken yaşar. */
    private var seslendirici: AsistanSeslendirici? = null
    /** v11.13: kesintisiz sesli asistan oturumu (ChatGPT/Gemini tarzı). */
    private var sesliAktif = false
    private var sesliTur = 0
    private val worker = Executors.newSingleThreadExecutor()
    /** Çevrimiçi model için son konuşma geçmişi (rol, metin). */
    private val history = mutableListOf<Pair<String, String>>()
    private var thinkingView: View? = null
    /** v7.60: su an ekranda cizili olan sohbetin kimligi. */
    private var gosterilenSohbet: Long = -1L
    private var pendingInput: EditText? = null
    private var pendingSend: (() -> Unit)? = null

    private val micPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startListening() else Toast.makeText(
                requireContext(), R.string.voice_perm, Toast.LENGTH_LONG
            ).show()
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // v8.6 · Öneri 29: yatay/tablet genişlik sınırı + yoğunluk
        Duzen.uygula(view)
        val input = view.findViewById<EditText>(R.id.chatInput)
        val sendButton = view.findViewById<ImageButton>(R.id.sendButton)
        val micButton = view.findViewById<ImageButton>(R.id.micButton)

        fun send() {
            val text = input.text.toString().trim()
            if (text.isEmpty()) return
            input.setText("")
            addUserMessage(text)
            // v7.59: sohbet gecmisine yaz
            context?.let {
                SohbetGecmisi.mesajEkle(it, "user", text)
                // v7.60: yeni sohbetse artik kimligi var — yeniden cizime gerek yok
                gosterilenSohbet = SohbetGecmisi.aktifId(it)
                basligiTazele()
            }
            askAssistant(text)
        }

        sendButton.setOnClickListener { send() }
        input.setOnEditorActionListener { _, _, _ -> send(); true }
        pendingInput = input
        pendingSend = { send() }

        if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            micButton.visibility = View.GONE
        }
        // v11.13: mikrofon artık KESİNTİSİZ sesli asistan oturumu başlatır.
        // Dokun → asistan dinler, AI yanıtlar, sesli söyler, sonra tekrar dinler
        // (eller serbest sohbet). Uzun bas ya da durum çipine dokun → oturumu bitir.
        micButton.setOnClickListener {
            val granted = ContextCompat.checkSelfPermission(
                requireContext(), android.Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                micPermission.launch(android.Manifest.permission.RECORD_AUDIO)
                return@setOnClickListener
            }
            if (sesliAktif) sesliBitir() else sesliBaslat()
        }
        micButton.setOnLongClickListener {
            if (sesliAktif) sesliBitir()
            true
        }
        // v11.13: durum çipi — aktifken gösterilir; dokununca oturum biter.
        view.findViewById<TextView>(R.id.sesliDurumTxt)?.setOnClickListener {
            if (sesliAktif) sesliBitir()
        }

        // Hazır soru kartları
        val chipRow = view.findViewById<LinearLayout>(R.id.chipRow)
        val chips = listOf(
            getString(R.string.chip_eval),
            getString(R.string.chip_plan),
            getString(R.string.chip_daily_plan),
            getString(R.string.chip_howto_add),
            getString(R.string.chip_tip),
            getString(R.string.chip_motive)
        )
        val chipQueries = listOf(
            "durumumu değerlendir",
            "haftalık plan çıkar",
            "günlük plan yap",
            "nasıl ekleme yaparım",
            "çalışma tüyosu ver",
            "motivasyon ver"
        )
        chips.forEachIndexed { index, label ->
            val chip = TextView(requireContext()).apply {
                this.text = label
                textSize = 12.5f
                val dp = resources.displayMetrics.density
                setPadding((14 * dp).toInt(), (10 * dp).toInt(), (14 * dp).toInt(), (10 * dp).toInt())
                background = GradientDrawable().apply {
                    cornerRadius = 18 * dp
                    setColor(
                        MaterialColors.getColor(
                            context,
                            com.google.android.material.R.attr.colorPrimaryContainer,
                            0xFFD9CBB8.toInt()
                        )
                    )
                }
                setTextColor(
                    MaterialColors.getColor(
                        context,
                        com.google.android.material.R.attr.colorOnSurface,
                        0xFF888888.toInt()
                    )
                )
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = (8 * dp).toInt() }
                setOnClickListener {
                    val query = chipQueries[index]
                    addUserMessage(label)
                    context?.let { c ->
                        SohbetGecmisi.mesajEkle(c, "user", query)
                        gosterilenSohbet = SohbetGecmisi.aktifId(c)
                        basligiTazele()
                    }
                    askAssistant(query)
                }
            }
            chipRow.addView(chip)
        }

        view.findViewById<TextView>(R.id.aiModeChip).setOnClickListener {
            (activity as? MainActivity)?.openSettings()
        }
        updateModeChip()

        // v7.60: gecmis paneli (sagdan acilir)
        view.findViewById<TextView>(R.id.sohbetlerimBtn).setOnClickListener { paneliAcKapa() }
        view.findViewById<TextView>(R.id.yeniSohbetBtn).setOnClickListener { yeniSohbet() }
        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.panelYeniBtn)
            .setOnClickListener {
                paneliKapat()
                yeniSohbet()
            }
        view.findViewById<TextView>(R.id.panelYonetBtn).setOnClickListener {
            paneliKapat()
            SohbetGecmisiActivity.ac(requireContext())
        }

        // v11.13: sesli asistan düğmesi — aç/kapa + anında durdur.
        // Motoru ekran öndeyken hazır tutar; asistan mesajları otomatik okunur.
        seslendirici = AsistanSeslendirici(requireContext())
        sesDugmesiniGuncelle()
        view.findViewById<TextView>(R.id.sesliAsistanBtn).setOnClickListener {
            val acik = AsistanSes.sesAcikMi(requireContext())
            AsistanSes.setSesAcik(requireContext(), !acik)
            if (acik) seslendirici?.dur()   // kapatıldı → çalan sesi kes
            sesDugmesiniGuncelle()
        }

        // Ekran ilk kurulusunda aktif sohbeti yukle
        gosterilenSohbet = -1L
        sohbetiEkranaAl()
        greeted = true
    }

    /** v11.13: ses düğmesi ikonunu (🔊/🔇) mevcut tercihe göre günceller. */
    private fun sesDugmesiniGuncelle() {
        val v = view ?: return
        val btn = v.findViewById<TextView>(R.id.sesliAsistanBtn) ?: return
        val acik = AsistanSes.sesAcikMi(requireContext())
        btn.text = if (acik) "🔊" else "🔇"
        btn.contentDescription = getString(
            if (acik) R.string.asistan_ses_acik else R.string.asistan_ses_kapali
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // v7.60 — GECMIS PANELI
    // ═══════════════════════════════════════════════════════════════

    private fun paneliAcKapa() {
        val d = view?.findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.asistanDrawer)
            ?: return
        if (d.isDrawerOpen(androidx.core.view.GravityCompat.END)) {
            d.closeDrawer(androidx.core.view.GravityCompat.END)
        } else {
            paneliCiz()
            d.openDrawer(androidx.core.view.GravityCompat.END)
        }
    }

    private fun paneliKapat() {
        view?.findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.asistanDrawer)
            ?.closeDrawer(androidx.core.view.GravityCompat.END)
    }

    /** Paneldeki sohbet listesini yeniden cizer. */
    private fun paneliCiz() {
        val kok = view ?: return
        val ctx = context ?: return
        val liste = kok.findViewById<LinearLayout>(R.id.panelListe) ?: return
        liste.removeAllViews()

        val sohbetler = SohbetGecmisi.tumu(ctx)
        val aktif = SohbetGecmisi.aktifId(ctx)
        kok.findViewById<TextView>(R.id.panelSayac)?.text =
            getString(R.string.sg_sayac, sohbetler.size)

        if (sohbetler.isEmpty()) {
            liste.addView(TextView(ctx).apply {
                text = getString(R.string.sg_panel_bos)
                textSize = 12.5f
                alpha = 0.7f
                setLineSpacing(0f, 1.3f)
                val dp = resources.displayMetrics.density
                setPadding((6 * dp).toInt(), (16 * dp).toInt(), (6 * dp).toInt(), 0)
            })
            return
        }

        sohbetler.forEach { sh -> liste.addView(panelSatiri(ctx, sh, sh.id == aktif)) }
    }

    private fun panelSatiri(
        ctx: android.content.Context,
        sh: SohbetGecmisi.Sohbet,
        aktifMi: Boolean
    ): View {
        val dp = resources.displayMetrics.density
        val kart = MaterialCardView(ctx).apply {
            radius = 14 * dp
            cardElevation = 0f
            strokeWidth = ((if (aktifMi) 2 else 1) * dp).toInt()
            if (aktifMi) {
                strokeColor = MaterialColors.getColor(
                    this, com.google.android.material.R.attr.colorPrimary, 0
                )
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (8 * dp).toInt() }
            isClickable = true
            setOnClickListener { sohbetSec(sh.id, sh.baslik) }
            setOnLongClickListener {
                paneliKapat()
                SohbetGecmisiActivity.ac(ctx)
                true
            }
        }
        val ic = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                (12 * dp).toInt(), (10 * dp).toInt(),
                (12 * dp).toInt(), (10 * dp).toInt()
            )
        }
        ic.addView(TextView(ctx).apply {
            text = sh.baslik
            textSize = 13.5f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        ic.addView(TextView(ctx).apply {
            val parcalar = mutableListOf(
                getString(R.string.sg_mesaj_sayisi, sh.mesajlar.size),
                SohbetGecmisi.zamanMetni(sh.guncellendi)
            )
            if (aktifMi) parcalar.add(getString(R.string.sg_aktif))
            text = parcalar.filter { it.isNotBlank() }.joinToString(" · ")
            textSize = 10.5f
            alpha = if (aktifMi) 1f else 0.65f
            setPadding(0, (4 * dp).toInt(), 0, 0)
            if (aktifMi) setTextColor(
                MaterialColors.getColor(
                    this, com.google.android.material.R.attr.colorPrimary, 0
                )
            )
        })
        kart.addView(ic)
        return kart
    }

    /** Panelden sohbet secildi — aninda yukle, ekran degistirme. */
    private fun sohbetSec(id: Long, baslik: String) {
        val ctx = context ?: return
        SohbetGecmisi.setAktif(ctx, id)
        paneliKapat()
        gosterilenSohbet = -1L      // zorla yeniden ciz
        sohbetiEkranaAl()
        Toast.makeText(ctx, getString(R.string.sg_gecildi, baslik), Toast.LENGTH_SHORT).show()
    }

    /**
     * v7.60 — Aktif sohbeti ekrana alir.
     *
     * ── Duzeltilen hata (v7.59) ──
     * Ekranlar hide/show ile yonetildigi icin baska bir ekrandan geri
     * donuldugunde onViewCreated yeniden calismiyordu; secilen sohbet
     * ekrana yansimiyor, eski konusma duruyordu. Artik onResume da
     * bunu cagiriyor ve [gosterilenSohbet] ile karsilastirip yalnizca
     * gercekten degistiyse yeniden ciziyor.
     */
    private fun sohbetiEkranaAl() {
        val ctx = context ?: return
        val kap = view?.findViewById<LinearLayout>(R.id.chatContainer) ?: return
        val aktif = SohbetGecmisi.aktifId(ctx)
        if (aktif == gosterilenSohbet) return   // zaten ekranda

        kap.removeAllViews()
        history.clear()
        gosterilenSohbet = aktif

        val sohbet = SohbetGecmisi.aktifSohbet(ctx)
        if (sohbet == null || sohbet.mesajlar.isEmpty()) {
            addAsistanMessage(AsistanBrain.greeting(ctx))
            return
        }
        sohbet.mesajlar.forEach { m ->
            addMessage(m.metin, isUser = m.kullaniciMi)
            history.add(m.rol to m.metin)
        }
        while (history.size > SohbetGecmisi.MODEL_GECMIS) history.removeAt(0)
        addMessage(getString(R.string.sg_devam), isUser = false)
        basligiTazele()
    }

    /** Ust baslikta acik sohbetin adini gosterir. */
    private fun basligiTazele() {
        val ctx = context ?: return
        val bs = view?.findViewById<TextView>(R.id.asistanBaslik) ?: return
        val sohbet = SohbetGecmisi.aktifSohbet(ctx)
        bs.text = if (sohbet == null || sohbet.mesajlar.isEmpty()) {
            getString(R.string.asistan_title)
        } else {
            sohbet.baslik
        }
    }

    /** v7.59: Temiz sayfa — yeni sohbet baslatir. */
    private fun yeniSohbet() {
        val ctx = context ?: return
        val yeni = SohbetGecmisi.yeniBaslat(ctx)
        history.clear()
        view?.findViewById<LinearLayout>(R.id.chatContainer)?.removeAllViews()
        gosterilenSohbet = yeni
        addAsistanMessage(AsistanBrain.greeting(ctx))
        basligiTazele()
    }

    override fun onResume() {
        super.onResume()
        updateModeChip()
        sesDugmesiniGuncelle()
        // v7.60: baska ekrandan/panelden secilen sohbet ekrana yansisin
        sohbetiEkranaAl()
        basligiTazele()
    }

    /**
     * v7.60: Ekranlar hide/show ile yonetiliyor; sekme degisince
     * onResume tetiklenmeyebilir. MainActivity.open() bunu cagirir.
     */
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            updateModeChip()
            sohbetiEkranaAl()
            basligiTazele()
        }
    }

    /** Başlıktaki mod rozetini günceller. */
    private fun updateModeChip() {
        val view = view ?: return
        val context = context ?: return
        val chip = view.findViewById<TextView>(R.id.aiModeChip)
        chip.text = when {
            AiSettings.isReady(context) -> {
                val p = AiClient.Provider.fromId(AiSettings.getProviderId(context))
                getString(R.string.ai_chip_online, p.label.substringBefore(" "))
            }
            AiSettings.isOnlineMode(context) -> getString(R.string.ai_chip_nokey)
            else -> getString(R.string.ai_chip_offline)
        }
    }

    // ---------------- Hibrit cevap üretimi ----------------

    /**
     * Çevrimiçi mod hazırsa yapay zekâya sorar, değilse cihazdaki beyni kullanır.
     * Eylem içeren komutlar (görev ekle, plan yap…) her zaman yerel beyinde kalır,
     * çünkü bunlar uygulamada gerçek değişiklik yapar.
     */
    private fun askAssistant(text: String) {
        val context = context ?: return

        // v11.13 DÜZELTMESİ: ajan modu yalnızca kullanıcı AÇIKÇA bir çalışma
        // hedefi/planı istediğinde tetiklenir. Rastgele sayı içeren sohbetler
        // ("30 sayfa oku", "5 tane görev") zamanlayıcıya takılmaz.
        if (AjanModu.ajanModuGerekliMi(text)) {
            val plan = AjanModu.planaCevir(text)
            if (plan.adimlar.size >= 2) {
                val komutlar = plan.adimlar
                addAsistanMessage(AsistanBrain.Reply("🎯 Ajan modu: \"" + plan.hedef + "\" — ${plan.adimlar.size} adımlık plan kuruldu."))
                SohbetGecmisi.mesajEkle(context, "user", text)
                if (sesliAktif) {
                    adimlariGosterVeCalistir(komutlar) { }
                } else {
                    AsistanKomut.calistirSirayla(requireActivity(), komutlar) { }
                }
                sesliDonguyuTetikle()
                return
            }
        }

        // Yerel beyin bu komutu bir eyleme çeviriyorsa (buton üretiyorsa) onu kullan
        val localReply = AsistanBrain.reply(context, text)
        val isActionable = localReply.actionLabel != null

        if (isActionable || !AiSettings.isReady(context)) {
            addAsistanMessage(localReply)
            // v7.59: yerel cevap da gecmise yazilsin
            SohbetGecmisi.mesajEkle(context, "assistant", localReply.text)
            // v11.13: kesintisiz sesli oturumda tekrar dinlemeye geç
            sesliDonguyuTetikle()
            return
        }

        // Çevrimiçi: arka planda sor
        showThinking()
        val snapshot = history.toList()
        // v8.9 · Öneri 16: coroutine.
        //
        // ── Önceki sorun ──
        // `worker.execute { ... activity?.runOnUiThread { ... } }`
        // deseni kullanılıyordu. AI isteği 3-10 saniye sürüyor; bu
        // sırada kullanıcı ekranı kapatırsa iş devam ediyor, fragment
        // referansı tutuluyor ve `isAdded` kontrolü elle yazılmak
        // zorunda kalıyordu. Unutulduğu yerde çökme oluyordu.
        //
        // `viewLifecycleOwner.lifecycleScope` görünüm yok olunca işi
        // OTOMATİK iptal ediyor — hem sızıntı hem çökme riski gidiyor,
        // hem de boşuna ağ trafiği harcanmıyor.
        ArkaPlan.calisGuvenli(
            this,
            is_ = { AiClient.chat(context, text, snapshot) },
            hata = { e ->
                hideThinking()
                android.util.Log.w("AsistanFragment", "AI isteği başarısız", e)
                addAsistanMessage(AsistanBrain.Reply(getString(R.string.ai_error_generic)))
            }
        ) { result ->
            run {
                hideThinking()
                if (result.ok) {
                    // v7.36: cevaptaki TÜM komutları ayıkla, sırayla çalıştır
                    val (temizMetin, komutlar) = AsistanKomut.ayiklaHepsi(result.text)
                    history.add("user" to text)
                    history.add("assistant" to temizMetin)
                    if (history.size > 16) repeat(2) { history.removeAt(0) }
                    addAsistanMessage(AsistanBrain.Reply(temizMetin))
                    // v7.59: cevabi gecmise yaz
                    SohbetGecmisi.mesajEkle(context, "assistant", temizMetin)

                    val etkinlik = activity
                    if (komutlar.isNotEmpty() && etkinlik != null) {
                        // v11.13: sesli oturumda her adımı TEK TEK göster ve çalıştır
                        if (sesliAktif) {
                            adimlariGosterVeCalistir(komutlar) { bildirimler ->
                                if (!isAdded) return@adimlariGosterVeCalistir
                                bildirimler.filter { it.isNotBlank() }.forEach {
                                    addAsistanMessage(AsistanBrain.Reply("✓ " + it))
                                }
                            }
                        } else {
                            AsistanKomut.calistirSirayla(etkinlik, komutlar) { bildirimler ->
                                if (!isAdded) return@calistirSirayla
                                bildirimler.filter { it.isNotBlank() }.forEach {
                                    addAsistanMessage(AsistanBrain.Reply("✓ " + it))
                                }
                            }
                        }
                    }
                    // Sağlayıcı değiştiyse kullanıcıyı bilgilendir
                    AiClient.sonGecisBilgisi?.let { bilgi ->
                        addAsistanMessage(AsistanBrain.Reply("ℹ " + bilgi))
                    }
                    // v11.13: kesintisiz sesli oturumda tekrar dinlemeye geç
                    sesliDonguyuTetikle()
                } else {
                    // Hata: istenirse cihazdaki beyne düş
                    if (AiSettings.isFallbackEnabled(context)) {
                        addAsistanMessage(
                            AsistanBrain.Reply(
                                getString(R.string.ai_fallback_note, result.text)
                            )
                        )
                        addAsistanMessage(localReply)
                    } else {
                        addAsistanMessage(AsistanBrain.Reply(result.text))
                    }
                    // v11.13: hata/fallback sonrası da kesintisiz oturum sürsün
                    sesliDonguyuTetikle()
                }
            }
        }
    }

    /**
     * v8.6 · Öneri 25 — Bekleme iskeleti.
     *
     * Eskiden yalnız "düşünüyor..." yazıyordu. AI cevabı 3-10 saniye
     * sürebiliyor; düz bir metin o sürede uygulamanın donduğu hissini
     * veriyordu. Artık gelecek cevabın şeklinde parlayan çizgiler
     * beliriyor — bekleme kısa hissediliyor ve ne geleceği belli.
     */
    private fun showThinking() {
        val view = view ?: return
        val context = context ?: return
        val container = view.findViewById<LinearLayout>(R.id.chatContainer)
        val dp = resources.displayMetrics.density

        val kutu = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((14 * dp).toInt(), (8 * dp).toInt(), (14 * dp).toInt(), (10 * dp).toInt())
        }
        kutu.addView(TextView(context).apply {
            text = getString(R.string.ai_thinking)
            textSize = 12.5f
            alpha = 0.65f
            setPadding(0, 0, 0, (6 * dp).toInt())
        })
        kutu.addView(Iskelet(context).apply {
            sekil = Iskelet.SEKIL_METIN
            satirSayisi = 2
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (74 * dp).toInt()
            )
        })

        thinkingView = kutu
        container.addView(kutu)
        view.findViewById<ScrollView>(R.id.chatScroll).post {
            view.findViewById<ScrollView>(R.id.chatScroll).fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun hideThinking() {
        val view = view ?: return
        thinkingView?.let {
            view.findViewById<LinearLayout>(R.id.chatContainer).removeView(it)
        }
        thinkingView = null
    }

    // ---------------- Kesintisiz sesli asistan oturumu ----------------

    /** Sesli asistan oturumunu başlatır: dinler, AI yanıtlar, sesli söyler, tekrar dinler. */
    private fun sesliBaslat() {
        sesliAktif = true
        sesliTur = 0
        sesliDurumGoster(SesliAsistanModu.Durum.DINLIYOR)
        startListening()
    }

    /** Sesli asistan oturumunu durdurur. */
    private fun sesliBitir() {
        sesliAktif = false
        sesliTur = 0
        runCatching { recognizer?.stopListening(); recognizer?.destroy(); recognizer = null }
        seslendirici?.dur()
        sesliDurumGoster(SesliAsistanModu.Durum.KAPALI)
    }

    /** Durum çipini günceller (aktifken görünür, kapalıyken gizli). */
    private fun sesliDurumGoster(durum: SesliAsistanModu.Durum) {
        val v = view ?: return
        val chip = v.findViewById<TextView>(R.id.sesliDurumTxt) ?: return
        when (durum) {
            SesliAsistanModu.Durum.KAPALI -> chip.visibility = View.GONE
            SesliAsistanModu.Durum.DINLIYOR -> {
                chip.visibility = View.VISIBLE
                chip.text = getString(R.string.sa_dinliyorum)
            }
            SesliAsistanModu.Durum.DUSUNUYOR -> {
                chip.visibility = View.VISIBLE
                chip.text = getString(R.string.sa_dusunuyorum)
            }
            SesliAsistanModu.Durum.KONUSUYOR -> {
                chip.visibility = View.VISIBLE
                chip.text = getString(R.string.sa_konusuyorum)
            }
        }
    }

    /**
     * v11.13 — Cevap verildikten sonra sesli oturumu sürdürür.
     * Tur sınırına gelinirse otomatik bitirir; aksi hâlde TTS konuşurken
     * kesmesin diye kısa bir gecikmeyle tekrar dinlemeye geçer.
     */
    private fun sesliDonguyuTetikle() {
        if (!sesliAktif) return
        if (SesliAsistanModu.turSiniri(sesliTur)) {
            sesliBitir()
            Toast.makeText(requireContext(), R.string.sa_sohbet_bitti, Toast.LENGTH_SHORT).show()
            return
        }
        sesliTur = SesliAsistanModu.yeniTur(sesliTur)
        sesliDurumGoster(SesliAsistanModu.Durum.KONUSUYOR)
        view?.postDelayed({
            if (sesliAktif) {
                sesliDurumGoster(SesliAsistanModu.Durum.DINLIYOR)
                startListening()
            }
        }, 1800L)
    }

    /**
     * v11.13 — Sesli oturumda AI'nın ürettiği komutları TEK TEK, kullanıcının
     * göreceği şekilde ekranda göstererek sırayla uygular. Her adım bir kart
     * olarak çizilir, sırayla işlenir ve durumu (bekliyor / yapıldı) işaretlenir.
     */
    private fun adimlariGosterVeCalistir(
        komutlar: List<AsistanKomut.Komut>,
        bitince: (List<String>) -> Unit
    ) {
        val ctx = context ?: run { bitince(emptyList()); return }
        val plan = AdimliEylemMotoru.adimlaraCevir(komutlar)
        if (plan.isEmpty()) { bitince(emptyList()); return }
        val bildirimler = mutableListOf<String>()

        // Görünür "Eylem Adımlarım" başlığı
        addAsistanMessage(AsistanBrain.Reply("🗂️ Sıradaki eylemlerim (${plan.size} adım):"))

        var durum = AdimliEylemMotoru.siradaki(plan)
        fun sonrakiAdimiCalistir() {
            if (durum.bitti || !sesliAktif) {
                bitince(bildirimler)
                return
            }
            val adim = plan[durum.siradaki]
            addAsistanMessage(AsistanBrain.Reply("👉 ${durum.siradaki + 1}/${plan.size} — ${adim.aciklama}"))

            // Yerel beyin/komut motoruyla çalıştır (ekran yönlendirmesi dahil)
            val sonuc = AsistanKomut.calistir(ctx, adim.komut, activity)
            if (sonuc.bildirim.isNotBlank()) bildirimler.add(sonuc.bildirim)
            addAsistanMessage(AsistanBrain.Reply("✅ ${adim.aciklama} — ${sonuc.bildirim.ifBlank { "tamam" }}"))
            durum = AdimliEylemMotoru.tamamla(durum)

            // Her adımı görebilmesi için kısa bir bekleme; sonra bir sonraki adım
            view?.postDelayed({ sonrakiAdimiCalistir() }, 900L)
        }
        sonrakiAdimiCalistir()
    }

    // ---------------- Sesli komut ----------------

    private fun startListening() {
        val context = context ?: return
        try {
            recognizer?.destroy()
            recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            }
            recognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Toast.makeText(context, R.string.voice_listening, Toast.LENGTH_SHORT).show()
                }
                override fun onResults(results: Bundle?) {
                    val said = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                    if (!said.isNullOrBlank()) {
                        // v11.13: "dur/kes/yeter" → AI'yı anında durdur, sohbete gönderme
                        if (KonusmaKesmeMotoru.kesmeMi(said)) {
                            sesliBitir()
                            seslendirici?.dur()
                            addAsistanMessage(AsistanBrain.Reply("⏹️ Durduk. Yeni komutunu dinliyorum…"))
                            return
                        }
                        pendingInput?.setText(said)
                        pendingSend?.invoke()
                    }
                }
                override fun onError(error: Int) {
                    Toast.makeText(context, R.string.voice_error, Toast.LENGTH_SHORT).show()
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            recognizer?.startListening(intent)
        } catch (e: Exception) {
            Toast.makeText(context, R.string.voice_none, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        // v11.13: sesli oturumu da kapat — arka planda dinlemez/konuşmaz
        sesliAktif = false
        sesliTur = 0
        recognizer?.destroy()
        recognizer = null
        thinkingView = null
        // v11.13: ekran kapanınca TTS de kapansın (arka planda konuşmaz)
        seslendirici?.kapat()
        seslendirici = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        worker.shutdownNow()
        super.onDestroy()
    }

    private fun addAsistanMessage(reply: AsistanBrain.Reply) {
        addMessage(reply.text, isUser = false)

        // v11.13: sesli asistan — açıksa yeni asistan cevabını kulağa oku
        runCatching {
            if (AsistanSes.sesAcikMi(requireContext())) {
                seslendirici?.konus(reply.text)
            }
        }

        val view = view ?: return
        if (reply.actionLabel == null || reply.action == null) return

        val context = requireContext()
        val dp = resources.displayMetrics.density
        val container = view.findViewById<LinearLayout>(R.id.chatContainer)
        val scroll = view.findViewById<ScrollView>(R.id.chatScroll)

        val button = MaterialButton(context).apply {
            text = reply.actionLabel
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (4 * dp).toInt()
                bottomMargin = (6 * dp).toInt()
                marginEnd = (56 * dp).toInt()
            }
            setOnClickListener {
                isEnabled = false
                text = "✔ Yapıldı"
                val result = reply.action.invoke()
                addAsistanMessage(AsistanBrain.Reply(result))
            }
        }
        container.addView(button)
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun addMessage(text: String, isUser: Boolean) {
        val view = view ?: return
        val context = requireContext()
        val dp = resources.displayMetrics.density
        val container = view.findViewById<LinearLayout>(R.id.chatContainer)
        val scroll = view.findViewById<ScrollView>(R.id.chatScroll)

        // Gemini tarzı: asistan mesajı solda küçük avatar ile, kullanıcı mesajı
        // sağda dolu (primary) baloncuk ile.
        val satir = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = if (isUser) Gravity.END else Gravity.START
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Asistan için küçük avatar
        if (!isUser) {
            satir.addView(
                TextView(context).apply {
                    this.text = "✨"
                    this.textSize = 15f
                    this.gravity = Gravity.CENTER
                    setBackgroundResource(R.drawable.ai_send_round)
                    setPadding((6 * dp).toInt(), 0, (6 * dp).toInt(), 0)
                    layoutParams = LinearLayout.LayoutParams(
                        (30 * dp).toInt(), (30 * dp).toInt()
                    ).apply {
                        topMargin = (8 * dp).toInt()
                        marginEnd = (6 * dp).toInt()
                    }
                }
            )
        }

        val card = MaterialCardView(context).apply {
            radius = 18 * dp
            cardElevation = 0.5f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (5 * dp).toInt()
                bottomMargin = (5 * dp).toInt()
                marginStart = if (isUser) (48 * dp).toInt() else 0
                marginEnd = if (isUser) 0 else (48 * dp).toInt()
            }
            setCardBackgroundColor(
                MaterialColors.getColor(
                    context,
                    if (isUser) {
                        com.google.android.material.R.attr.colorPrimary
                    } else {
                        com.google.android.material.R.attr.colorSurfaceContainer
                    },
                    0xFFD9CBB8.toInt()
                )
            )
            if (!isUser) {
                strokeWidth = 1
                strokeColor = MaterialColors.getColor(
                    context,
                    com.google.android.material.R.attr.colorOutlineVariant,
                    0xFFCCCCCC.toInt()
                )
            }
        }

        val bubble = TextView(context).apply {
            this.text = text
            textSize = 14.5f
            setTextColor(
                MaterialColors.getColor(
                    context,
                    if (isUser) {
                        com.google.android.material.R.attr.colorOnPrimary
                    } else {
                        com.google.android.material.R.attr.colorOnSurface
                    },
                    0xFF888888.toInt()
                )
            )
            setPadding(
                (14 * dp).toInt(), (11 * dp).toInt(),
                (14 * dp).toInt(), (11 * dp).toInt()
            )
            maxWidth = (300 * dp).toInt()
        }

        card.addView(bubble)
        satir.addView(card)
        container.addView(satir)
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun addUserMessage(text: String) = addMessage(text, true)
    private fun addAsistanMessage(text: String) =
        addAsistanMessage(AsistanBrain.Reply(text))
}
