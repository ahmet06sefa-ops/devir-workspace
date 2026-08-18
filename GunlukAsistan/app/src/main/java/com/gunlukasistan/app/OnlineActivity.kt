package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.concurrent.Executors

/**
 * v7.51 — İki kişilik online paylaşım ekranı.
 *
 * ── Akış ──
 *  1. İlk açılışta: "Oda kur" veya "Koda katıl"
 *  2. Oda kuran 6 haneli kodu paylaşır (WhatsApp/kopyala)
 *  3. Diğer kişi kodu girer → aynı listeyi görür
 *  4. İki taraf da görev ekler, tamamlar, not bırakır
 *
 * ── Çakışma koruması ──
 * Her yazmadan önce sunucudan taze veri okunur, değişiklik onun üzerine
 * eklenir. Böylece iki kişi aynı anda ekleme yapsa da kimsenin işi kaybolmaz.
 */
class OnlineActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            context.startActivity(Intent(context, OnlineActivity::class.java))
        }
    }

    private val worker = Executors.newSingleThreadExecutor()
    private val yogunluk get() = resources.displayMetrics.density

    // v7.77: oda kurulurken yonetici modu
    private val MOD_BEN = 0
    private val MOD_KARSI = 1
    private val MOD_ESIT = 2

    private lateinit var icerik: LinearLayout
    private lateinit var durum: TextView
    private lateinit var yukleniyor: LinearLayout
    private lateinit var yukleniyorYazi: TextView
    private lateinit var girisCubugu: LinearLayout
    private lateinit var girdi: EditText

    private var oda: OnlineStore.Oda? = null
    @Volatile private var calisiyor = false

    /** v7.53: aktif bölüm. */
    private enum class Bolum(val adRes: Int, val emoji: String) {
        SOHBET(R.string.on_b_sohbet, "💬"),
        GOREV(R.string.on_b_gorev, "✓"),
        NOT(R.string.on_b_not, "📝"),
        KONU(R.string.on_b_konu, "📚"),
        ALISKANLIK(R.string.on_b_alis, "🔥")
    }
    private var aktifBolum = Bolum.SOHBET
    private lateinit var sekmeKabi: LinearLayout
    private lateinit var sekmeScroll: View

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
        setContentView(R.layout.activity_online)

        icerik = findViewById(R.id.onContent)
        durum = findViewById(R.id.onStatus)
        yukleniyor = findViewById(R.id.onLoading)
        yukleniyorYazi = findViewById(R.id.onLoadingText)
        girisCubugu = findViewById(R.id.onInputBar)
        girdi = findViewById(R.id.onInput)
        sekmeKabi = findViewById(R.id.onTabs)
        sekmeScroll = findViewById(R.id.onTabScroll)

        findViewById<TextView>(R.id.onClose).setOnClickListener { finish() }
        findViewById<TextView>(R.id.onSync).setOnClickListener { esitle(true) }
        findViewById<TextView>(R.id.onSettings).setOnClickListener { ayarlar() }
        findViewById<TextView>(R.id.onSend).setOnClickListener { gorevEkle() }

        girdi.setOnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_SEND) { gorevEkle(); true } else false
        }

        if (OnlineStore.bagliMi(this)) {
            // Önce önbellekten göster, sonra tazele
            OnlineStore.onbellektenOku(this)?.let { oda = it; ciz() }
            if (OnlineStore.otoSenkron(this)) esitle(false) else ciz()
        } else {
            karsilamaCiz()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // KARŞILAMA — oda kur / katıl
    // ═══════════════════════════════════════════════════════════════

    private fun karsilamaCiz() {
        icerik.removeAllViews()
        girisCubugu.visibility = View.GONE
        sekmeScroll.visibility = View.GONE
        durum.text = getString(R.string.on_bagli_degil)

        icerik.addView(kart {
            it.addView(baslik(getString(R.string.on_hos_baslik), 16f))
            it.addView(yazi(getString(R.string.on_hos_aciklama), 13f, 0.85f))
        })

        icerik.addView(kart {
            it.addView(baslik(getString(R.string.on_kur_baslik), 15f))
            it.addView(yazi(getString(R.string.on_kur_aciklama), 12.5f, 0.8f))
            it.addView(anaDugme(getString(R.string.on_kur_dugme)) { odaKur() })
        })

        icerik.addView(kart {
            it.addView(baslik(getString(R.string.on_katil_baslik), 15f))
            it.addView(yazi(getString(R.string.on_katil_aciklama), 12.5f, 0.8f))
            it.addView(anaDugme(getString(R.string.on_katil_dugme)) { odayaKatil() })
        })

        icerik.addView(yazi(getString(R.string.on_gizlilik), 11.5f, 0.6f).apply {
            setPadding(4, (14 * yogunluk).toInt(), 4, 0)
        })
    }

    /**
     * Yeni oda kurar.
     *
     * v7.77: Artik once "kim yonetsin" soruluyor. Onceden odayi kuran
     * kisi otomatik yonetici oluyordu; kullanici bunu bastan secmek
     * istedi.
     */
    private fun odaKur() {
        adSor { ad -> yoneticiModuSor(ad) }
    }

    /**
     * v7.77 — Yonetici secimi.
     *
     * Uc secenek:
     *   BEN   → kuran kisi yonetici, sifre koyabilir
     *   KARSI → yonetici bos birakilir; katilan ilk kisi yonetici olur
     *   ESIT  → hic yonetici olmaz, ikisi de her seyi yapabilir
     */
    private fun yoneticiModuSor(ad: String) {
        val secenekler = arrayOf(
            getString(R.string.yn_ben) + "\n" + getString(R.string.yn_ben_d),
            getString(R.string.yn_karsi) + "\n" + getString(R.string.yn_karsi_d),
            getString(R.string.yn_esit) + "\n" + getString(R.string.yn_esit_d)
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.yn_baslik)
            .setMessage(R.string.yn_msg)
            .setItems(secenekler) { _, hangi ->
                when (hangi) {
                    // Ben yoneteyim → sifre de sorulur
                    0 -> yoneticiSifresiSor { sifre -> odayiOlustur(ad, sifre, MOD_BEN) }
                    // Karsi taraf yonetsin → sifre yok, yonetici bos
                    1 -> odayiOlustur(ad, "", MOD_KARSI)
                    // Yonetici olmasin → herkes esit
                    else -> odayiOlustur(ad, "", MOD_ESIT)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * v7.52: Oda kuran kişi yöneticidir. Şifre, üyenin ayarları
     * değiştirmesini engellemek için.
     */
    private fun yoneticiSifresiSor(sonra: (String) -> Unit) {
        val g = EditText(this).apply {
            hint = getString(R.string.on_sifre_hint)
            inputType = InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_NUMBER_VARIATION_PASSWORD
            filters = arrayOf(android.text.InputFilter.LengthFilter(8))
            textSize = 20f
            gravity = Gravity.CENTER
            setPadding(
                (20 * yogunluk).toInt(), (16 * yogunluk).toInt(),
                (20 * yogunluk).toInt(), (10 * yogunluk).toInt()
            )
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.on_sifre_baslik)
            .setMessage(R.string.on_sifre_msg)
            .setView(g)
            .setPositiveButton(R.string.done) { _, _ ->
                val sifre = g.text?.toString()?.trim().orEmpty()
                if (sifre.length < 4) {
                    Toast.makeText(this, R.string.on_sifre_kisa, Toast.LENGTH_SHORT).show()
                } else sonra(sifre)
            }
            .setNeutralButton(R.string.on_sifre_yok) { _, _ -> sonra("") }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun odayiOlustur(ad: String, sifre: String, mod: Int = MOD_BEN) {
            val kod = OnlineStore.kodUret()
            calisiyor = true
            yukle(true, getString(R.string.on_kuruluyor))

            worker.execute {
                OnlineStore.baglan(this, kod, ad)
                val yeni = OnlineStore.Oda(surum = 1)
                yeni.uyeler.add(ad)
                // v7.77: yonetici secilen moda gore atanir
                when (mod) {
                    MOD_BEN -> {
                        yeni.yonetici = ad
                        yeni.sifreHash = if (sifre.isBlank()) "" else
                            OnlineStore.sifreKarma(sifre, kod)
                    }
                    MOD_KARSI -> {
                        // Bos birakilir; katilan ilk kisi yonetici olur
                        yeni.yonetici = ""
                        yeni.sifreHash = ""
                        // Katilan kisi devralsin diye isaret
                        yeni.kural.yoneticiBekliyor = true
                    }
                    else -> {
                        // Yoneticisiz oda — herkes esit yetkili
                        yeni.yonetici = ""
                        yeni.sifreHash = ""
                        yeni.kural.yoneticiBekliyor = false
                    }
                }
                OnlineStore.setYoneticiBayragi(this, mod == MOD_BEN)
                if (mod == MOD_BEN) OnlineStore.yoneticiOturumuAc(this)
                val s = OnlineStore.yaz(this, yeni, kod)

                runOnUiThread {
                    calisiyor = false
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    yukle(false)
                    if (!s.ok) {
                        OnlineStore.ayril(this)
                        hata(s.mesaj)
                        return@runOnUiThread
                    }
                    oda = yeni
                    OnlineStore.onbellegeYaz(this, yeni)
                    ciz()
                    koduGoster(kod)
                }
            }
    }

    /** Var olan odaya kodla katılır. */
    private fun odayaKatil() {
        val kodGirdi = EditText(this).apply {
            hint = getString(R.string.on_kod_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            filters = arrayOf(android.text.InputFilter.LengthFilter(6))
            textSize = 20f
            gravity = Gravity.CENTER
            setPadding(
                (20 * yogunluk).toInt(), (16 * yogunluk).toInt(),
                (20 * yogunluk).toInt(), (12 * yogunluk).toInt()
            )
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.on_katil_baslik)
            .setMessage(R.string.on_kod_msg)
            .setView(kodGirdi)
            .setPositiveButton(R.string.on_katil_dugme) { _, _ ->
                val kod = kodGirdi.text?.toString()?.trim()?.uppercase().orEmpty()
                if (kod.length != 6) {
                    Toast.makeText(this, R.string.on_kod_hatali, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                adSor { ad -> katilmayiDene(kod, ad) }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun katilmayiDene(kod: String, ad: String) {
        calisiyor = true
        yukle(true, getString(R.string.on_katiliyor))

        worker.execute {
            val s = OnlineStore.oku(this, kod)
            runOnUiThread {
                calisiyor = false
                if (isFinishing || isDestroyed) return@runOnUiThread
                yukle(false)
                if (!s.ok || s.oda == null) {
                    hata(getString(R.string.on_oda_yok, kod))
                    return@runOnUiThread
                }
                OnlineStore.baglan(this, kod, ad)
                // Kendimizi üye listesine ekle
                calisiyor = true
                yukle(true, getString(R.string.on_katiliyor))
                worker.execute {
                    // v7.77: oda "yonetici bekliyor" ise katilan kisi devralir
                    var yoneticiOldum = false
                    val g = OnlineStore.guvenliGuncelle(this) { o ->
                        if (!o.uyeler.contains(ad)) o.uyeler.add(ad)
                        if (o.kural.yoneticiBekliyor && o.yonetici.isBlank()) {
                            o.yonetici = ad
                            o.kural.yoneticiBekliyor = false
                            yoneticiOldum = true
                        }
                    }
                    if (yoneticiOldum) {
                        OnlineStore.setYoneticiBayragi(this, true)
                        OnlineStore.yoneticiOturumuAc(this)
                    }
                    val taze = OnlineStore.oku(this)
                    runOnUiThread {
                        calisiyor = false
                        if (isFinishing || isDestroyed) return@runOnUiThread
                        yukle(false)
                        if (!g.ok) { hata(g.mesaj); return@runOnUiThread }
                        oda = taze.oda
                        taze.oda?.let { OnlineStore.onbellegeYaz(this, it) }
                        ciz()
                        // v7.77: yoneticiligi devraldiysa ayrica bildir
                        if (yoneticiOldum) {
                            Toast.makeText(this, R.string.yn_sen_oldun, Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, R.string.on_katildin, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun adSor(sonra: (String) -> Unit) {
        val mevcut = OnlineStore.benimAdim(this)
        if (mevcut.isNotBlank()) { sonra(mevcut); return }

        val g = EditText(this).apply {
            hint = getString(R.string.on_ad_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setPadding(
                (20 * yogunluk).toInt(), (14 * yogunluk).toInt(),
                (20 * yogunluk).toInt(), (10 * yogunluk).toInt()
            )
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.on_ad_baslik)
            .setMessage(R.string.on_ad_msg)
            .setView(g)
            .setPositiveButton(R.string.done) { _, _ ->
                val ad = g.text?.toString()?.trim().orEmpty()
                if (ad.isBlank()) {
                    Toast.makeText(this, R.string.on_ad_bos, Toast.LENGTH_SHORT).show()
                } else sonra(ad)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** Kodu büyük puntoyla gösterir, paylaşma seçenekleri sunar. */
    private fun koduGoster(kod: String) {
        val kap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(
                (20 * yogunluk).toInt(), (18 * yogunluk).toInt(),
                (20 * yogunluk).toInt(), (8 * yogunluk).toInt()
            )
            addView(TextView(this@OnlineActivity).apply {
                text = kod
                textSize = 38f
                gravity = Gravity.CENTER
                letterSpacing = 0.25f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(
                    MaterialColors.getColor(
                        this@OnlineActivity,
                        com.google.android.material.R.attr.colorPrimary, 0
                    )
                )
            })
            addView(TextView(this@OnlineActivity).apply {
                text = getString(R.string.on_kod_paylas_msg)
                textSize = 12.5f
                gravity = Gravity.CENTER
                alpha = 0.8f
                setPadding(0, (12 * yogunluk).toInt(), 0, 0)
            })
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.on_kod_hazir)
            .setView(kap)
            .setPositiveButton(R.string.on_kod_gonder) { _, _ -> koduPaylas(kod) }
            .setNeutralButton(R.string.on_kod_kopyala) { _, _ -> koduKopyala(kod) }
            .setNegativeButton(R.string.done, null)
            .show()
    }

    private fun koduPaylas(kod: String) {
        try {
            startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, getString(R.string.on_davet_metni, kod))
                    },
                    getString(R.string.on_kod_gonder)
                )
            )
        } catch (e: Exception) {
            android.util.Log.w("OnlineActivity", "Paylaşılamadı", e)
        }
    }

    private fun koduKopyala(kod: String) {
        try {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE)
                as android.content.ClipboardManager
            cm.setPrimaryClip(android.content.ClipData.newPlainText("kod", kod))
            Toast.makeText(this, R.string.on_kopyalandi, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.util.Log.w("OnlineActivity", "Kopyalanamadı", e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // EŞİTLEME
    // ═══════════════════════════════════════════════════════════════

    private fun esitle(elle: Boolean) {
        if (calisiyor || !OnlineStore.bagliMi(this)) return
        calisiyor = true
        yukle(true, getString(R.string.on_esitleniyor))

        worker.execute {
            val s = OnlineStore.oku(this)
            runOnUiThread {
                calisiyor = false
                if (isFinishing || isDestroyed) return@runOnUiThread
                yukle(false)
                if (!s.ok) {
                    if (elle) Toast.makeText(this, s.mesaj, Toast.LENGTH_LONG).show()
                    durum.text = getString(
                        R.string.on_cevrimdisi,
                        OnlineStore.zamanMetni(OnlineStore.onbellekZamani(this))
                    )
                    return@runOnUiThread
                }
                oda = s.oda
                s.oda?.let { OnlineStore.onbellegeYaz(this, it) }
                ciz()
                if (elle) Toast.makeText(this, R.string.on_guncel, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ÇİZİM
    // ═══════════════════════════════════════════════════════════════

    private fun ciz() {
        if (!OnlineStore.bagliMi(this)) { karsilamaCiz(); return }
        val o = oda ?: return

        icerik.removeAllViews()
        sekmeScroll.visibility = View.VISIBLE
        sekmeleriCiz()

        val ben = OnlineStore.benimAdim(this)
        durum.text = getString(
            R.string.on_durum,
            OnlineStore.kod(this),
            o.uyeler.size,
            OnlineStore.zamanMetni(o.guncellendi)
        )

        // Giriş çubuğu bölüme göre — alışkanlıkta da ekleme var
        // Sohbet herkese açık; diğer bölümlerde ekleme yetkisi gerekir
        val ekleyebilir = aktifBolum == Bolum.SOHBET ||
            OnlineStore.izinVar(this, o, OnlineStore.Islem.EKLE)
        girisCubugu.visibility = if (ekleyebilir) View.VISIBLE else View.GONE
        girdi.hint = getString(
            when (aktifBolum) {
                Bolum.SOHBET -> R.string.on_sohbet_hint
                Bolum.GOREV -> R.string.on_gorev_hint
                Bolum.NOT -> R.string.on_not_hint2
                Bolum.KONU -> R.string.on_konu_hint
                Bolum.ALISKANLIK -> R.string.on_alis_hint
            }
        )

        // Üyeler kartı — sohbette gizli (mesajlara yer açmak için)
        if (aktifBolum != Bolum.SOHBET) icerik.addView(uyelerKarti(o, ben))

        // Aktif bölümün içeriği
        when (aktifBolum) {
            Bolum.SOHBET -> sohbetiCiz(o, ben)
            Bolum.GOREV -> gorevleriCiz(o, ben)
            Bolum.NOT -> notlariCiz(o, ben)
            Bolum.KONU -> konulariCiz(o, ben)
            Bolum.ALISKANLIK -> aliskanliklariCiz(o, ben)
        }
    }

    /** v7.53: Bölüm sekmeleri — her birinde öğe sayısı rozeti. */
    private fun sekmeleriCiz() {
        sekmeKabi.removeAllViews()
        val o = oda ?: return

        Bolum.entries.forEach { b ->
            val sayi = when (b) {
                Bolum.SOHBET -> o.mesajlar.size
                Bolum.GOREV -> o.gorevler.count { !it.tamam }
                Bolum.NOT -> o.notlar.size
                Bolum.KONU -> o.konular.size
                Bolum.ALISKANLIK -> o.aliskanliklar.size
            }
            val secili = b == aktifBolum
            val etiket = b.emoji + " " + getString(b.adRes) +
                (if (sayi > 0) "  " + sayi else "")

            sekmeKabi.addView(TextView(this).apply {
                text = etiket
                textSize = 13f
                setPadding(
                    (14 * yogunluk).toInt(), (8 * yogunluk).toInt(),
                    (14 * yogunluk).toInt(), (8 * yogunluk).toInt()
                )
                if (secili) {
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(
                        MaterialColors.getColor(
                            this@OnlineActivity,
                            com.google.android.material.R.attr.colorPrimary, 0
                        )
                    )
                    background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = 20 * yogunluk
                        setColor(
                            (MaterialColors.getColor(
                                this@OnlineActivity,
                                com.google.android.material.R.attr.colorPrimary, 0
                            ) and 0x00FFFFFF) or 0x22000000
                        )
                    }
                } else {
                    alpha = 0.7f
                    background = android.graphics.drawable.RippleDrawable(
                        android.content.res.ColorStateList.valueOf(0x22888888), null, null
                    )
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = (6 * yogunluk).toInt() }
                isClickable = true
                setOnClickListener {
                    aktifBolum = b
                    ciz()
                }
            })
        }
    }

    private fun uyelerKarti(o: OnlineStore.Oda, ben: String): View = kart {
        it.addView(baslik(getString(R.string.on_uyeler), 14f))
        if (o.uyeler.isEmpty()) {
            it.addView(yazi(getString(R.string.on_uye_yok), 12.5f, 0.7f))
        } else {
            o.uyeler.forEach { u ->
                val rol = if (u == o.yonetici) " 👑" else ""
                it.addView(yazi(
                    (if (u == ben) "👤 " else "👥 ") + u + rol +
                        (if (u == ben) " " + getString(R.string.on_sen) else ""),
                    13f, 0.9f
                ))
            }
        }
        if (o.uyeler.size < 2) {
            it.addView(anaDugme(getString(R.string.on_davet_et)) {
                koduGoster(OnlineStore.kod(this))
            })
        }
        // Üye kısıtlamaları — yalnızca üyeye gösterilir
        if (!OnlineStore.yoneticiMiyim(this, o)) {
            val kisitlar = mutableListOf<String>()
            if (!o.kural.ekleyebilir) kisitlar.add(getString(R.string.on_y_ekle))
            if (!o.kural.silebilir) kisitlar.add(getString(R.string.on_y_sil))
            if (!o.kural.geriAlabilir) kisitlar.add(getString(R.string.on_y_gerial))
            if (kisitlar.isNotEmpty()) {
                it.addView(yazi("🔒 " + kisitlar.joinToString(", "), 11.5f, 0.65f))
            }
        } else {
            it.addView(anaDugme(getString(R.string.on_yetkiler)) {
                yoneticiIsleminde { yetkiEkrani() }
            })
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // v7.54 — BÖLÜM: SOHBET
    // ═══════════════════════════════════════════════════════════════

    /**
     * Sohbet balonları. Kendi mesajların sağda, karşı tarafınki solda,
     * yapay zekâ cevapları ortada farklı renkte.
     */
    private fun sohbetiCiz(o: OnlineStore.Oda, ben: String) {
        if (o.mesajlar.isEmpty()) {
            icerik.addView(kart { ic ->
                ic.addView(baslik(getString(R.string.on_sohbet_bos_b), 15f))
                ic.addView(yazi(getString(R.string.on_sohbet_bos), 13f, 0.85f))
                ic.addView(yazi(getString(R.string.on_sohbet_ai_ipucu), 12.5f, 0.75f))
            })
            return
        }

        o.mesajlar.sortedBy { it.zaman }.forEach { m ->
            icerik.addView(mesajBalonu(m, ben))
        }

        // AI'ya hızlı soru düğmeleri
        icerik.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, (8 * yogunluk).toInt(), 0, 0)
            addView(kucukDugme("🤖 " + getString(R.string.on_ai_ozet)) {
                aiyaSor(getString(R.string.on_ai_ozet_soru))
            })
            addView(kucukDugme("💡 " + getString(R.string.on_ai_oner)) {
                aiyaSor(getString(R.string.on_ai_oner_soru))
            })
        })
    }

    private fun mesajBalonu(m: OnlineStore.Mesaj, ben: String): View {
        val benim = m.kim == ben && !m.aiMi
        val kap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (8 * yogunluk).toInt() }
            gravity = if (benim) Gravity.END else Gravity.START
        }

        // Gönderen + saat
        kap.addView(TextView(this).apply {
            text = (if (m.aiMi) "🤖 " + getString(R.string.on_ai_adi) else m.kim) +
                "  " + OnlineStore.saatMetni(m.zaman)
            textSize = 10.5f
            alpha = 0.6f
            setPadding((6 * yogunluk).toInt(), 0, (6 * yogunluk).toInt(), (2 * yogunluk).toInt())
        })

        // Balon
        val vurgu = MaterialColors.getColor(
            this, com.google.android.material.R.attr.colorPrimary, 0
        )
        val balon = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                (13 * yogunluk).toInt(), (10 * yogunluk).toInt(),
                (13 * yogunluk).toInt(), (10 * yogunluk).toInt()
            )
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 16 * yogunluk
                setColor(
                    when {
                        m.aiMi -> (vurgu and 0x00FFFFFF) or 0x22000000
                        benim -> (vurgu and 0x00FFFFFF) or 0x33000000
                        else -> 0x18888888
                    }
                )
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        balon.addView(TextView(this).apply {
            text = m.metin
            textSize = 14f
            setLineSpacing(3f * yogunluk, 1f)
            setTextIsSelectable(true)
        })

        // v7.54: AI önerileri — tek dokunuşla ekleme
        if (m.oneriler.isNotEmpty()) {
            balon.addView(TextView(this).apply {
                text = getString(R.string.on_ai_ekle_ipucu)
                textSize = 11f
                alpha = 0.7f
                setPadding(0, (8 * yogunluk).toInt(), 0, (4 * yogunluk).toInt())
            })
            m.oneriler.forEach { satir ->
                val (tur, metin) = SohbetAi.oneriCoz(satir)
                balon.addView(TextView(this).apply {
                    text = SohbetAi.turSimgesi(tur) + "  " + metin + "   ＋"
                    textSize = 12.5f
                    setTextColor(vurgu)
                    setPadding(
                        (10 * yogunluk).toInt(), (8 * yogunluk).toInt(),
                        (10 * yogunluk).toInt(), (8 * yogunluk).toInt()
                    )
                    background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = 10 * yogunluk
                        setStroke((1 * yogunluk).toInt(), (vurgu and 0x00FFFFFF) or 0x55000000)
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = (4 * yogunluk).toInt() }
                    isClickable = true
                    setOnClickListener { oneriyiEkle(tur, metin) }
                })
            }
        }

        kap.addView(balon)
        return kap
    }

    /**
     * v7.54: AI önerisini ilgili bölüme ekler.
     * Ekleme yetkisi kontrol edilir — üye kısıtlıysa uyarı görür.
     */
    private fun oneriyiEkle(tur: String, metin: String) {
        if (!OnlineStore.izinVar(this, oda, OnlineStore.Islem.EKLE)) {
            izinli(OnlineStore.Islem.EKLE) {}
            return
        }
        val ben = OnlineStore.benimAdim(this)
        degistirVeGonder { o ->
            when (tur) {
                "not" -> o.notlar.add(
                    OnlineStore.Not(
                        id = System.currentTimeMillis(),
                        baslik = metin.take(80), icerik = "", sahip = ben
                    )
                )
                "konu" -> o.konular.add(
                    OnlineStore.Konu(
                        id = System.currentTimeMillis(),
                        baslik = metin.take(80), sahip = ben
                    )
                )
                "alis" -> o.aliskanliklar.add(
                    OnlineStore.Aliskanlik(
                        id = System.currentTimeMillis(),
                        ad = metin.take(60), sahip = ben
                    )
                )
                else -> o.gorevler.add(
                    OnlineStore.Gorev(
                        id = System.currentTimeMillis(),
                        metin = metin.take(120), sahip = ben
                    )
                )
            }
            if (!o.uyeler.contains(ben)) o.uyeler.add(ben)
        }
        Toast.makeText(
            this,
            getString(R.string.on_oneri_eklendi, SohbetAi.turEtiketi(this, tur)),
            Toast.LENGTH_SHORT
        ).show()
    }

    /** Sohbete mesaj gönderir; @ai ile başlıyorsa yapay zekâya sorar. */
    private fun mesajGonder(metin: String) {
        girdi.setText("")
        klavyeKapat()
        val ben = OnlineStore.benimAdim(this)

        if (SohbetAi.aiyaMi(metin)) {
            aiyaSor(SohbetAi.soruyuAyikla(metin), ben)
            return
        }
        degistirVeGonder { o ->
            o.mesajlar.add(
                OnlineStore.Mesaj(
                    id = System.currentTimeMillis(), kim = ben, metin = metin.take(500)
                )
            )
            if (!o.uyeler.contains(ben)) o.uyeler.add(ben)
        }
    }

    /**
     * Yapay zekâya sorar: önce soruyu sohbete yazar, sonra cevabı ekler.
     * Böylece karşı taraf da hem soruyu hem cevabı görür.
     */
    private fun aiyaSor(soru: String, kim: String = OnlineStore.benimAdim(this)) {
        if (calisiyor || soru.isBlank()) return
        calisiyor = true
        yukle(true, getString(R.string.on_ai_dusunuyor))

        val gecmis = oda?.mesajlar?.toList().orEmpty()
        val uyeler = oda?.uyeler?.toList().orEmpty()

        worker.execute {
            // 1) Soruyu sohbete yaz
            OnlineStore.guvenliGuncelle(this) { o ->
                o.mesajlar.add(
                    OnlineStore.Mesaj(
                        id = System.currentTimeMillis(),
                        kim = kim, metin = SohbetAi.TETIK + " " + soru.take(400)
                    )
                )
                if (!o.uyeler.contains(kim)) o.uyeler.add(kim)
            }

            // 2) AI'ya sor
            val cevap = SohbetAi.sor(this, soru, gecmis, uyeler)

            // 3) Cevabı sohbete ekle
            OnlineStore.guvenliGuncelle(this) { o ->
                o.mesajlar.add(
                    OnlineStore.Mesaj(
                        id = System.currentTimeMillis() + 1,
                        kim = getString(R.string.on_ai_adi),
                        metin = if (cevap.ok) cevap.metin
                        else getString(R.string.on_ai_hata, cevap.metin.take(150)),
                        tur = "ai",
                        oneriler = cevap.oneriler.toMutableList()
                    )
                )
            }

            val taze = OnlineStore.oku(this)
            runOnUiThread {
                calisiyor = false
                if (isFinishing || isDestroyed) return@runOnUiThread
                yukle(false)
                oda = taze.oda
                taze.oda?.let { OnlineStore.onbellegeYaz(this, it) }
                ciz()
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // BÖLÜM: GÖREVLER
    // ═══════════════════════════════════════════════════════════════

    private fun gorevleriCiz(o: OnlineStore.Oda, ben: String) {
        val bekleyen = o.gorevler.filter { !it.tamam }
        val biten = o.gorevler.filter { it.tamam }

        icerik.addView(baslik(
            getString(R.string.on_gorevler, bekleyen.size, o.gorevler.size), 15f
        ))
        if (o.gorevler.isEmpty()) {
            icerik.addView(yazi(getString(R.string.on_gorev_yok), 12.5f, 0.7f))
            return
        }
        bekleyen.forEach { icerik.addView(gorevSatiri(it, ben)) }
        if (biten.isNotEmpty()) {
            icerik.addView(baslik(getString(R.string.on_bitenler, biten.size), 13f))
            biten.takeLast(10).forEach { icerik.addView(gorevSatiri(it, ben)) }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // BÖLÜM: NOTLAR
    // ═══════════════════════════════════════════════════════════════

    private fun notlariCiz(o: OnlineStore.Oda, ben: String) {
        icerik.addView(baslik(getString(R.string.on_notlar, o.notlar.size), 15f))
        if (o.notlar.isEmpty()) {
            icerik.addView(yazi(getString(R.string.on_not_yok), 12.5f, 0.7f))
            return
        }
        o.notlar.sortedByDescending { it.eklendi }.forEach { n ->
            icerik.addView(kartTiklanir({ notGoster(n) }, { notMenusu(n); true }) { ic ->
                ic.addView(TextView(this).apply {
                    text = n.baslik.ifBlank { getString(R.string.on_not_basliksiz) }
                    textSize = 14.5f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                })
                if (n.icerik.isNotBlank()) {
                    ic.addView(TextView(this).apply {
                        text = n.icerik
                        textSize = 13f
                        maxLines = 3
                        ellipsize = android.text.TextUtils.TruncateAt.END
                        setPadding(0, (4 * yogunluk).toInt(), 0, 0)
                    })
                }
                ic.addView(yazi(
                    (if (n.sahip == ben) "👤 " else "👥 ") + n.sahip, 11f, 0.65f
                ))
            })
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // BÖLÜM: KONULAR
    // ═══════════════════════════════════════════════════════════════

    private fun konulariCiz(o: OnlineStore.Oda, ben: String) {
        icerik.addView(baslik(getString(R.string.on_konular, o.konular.size), 15f))
        if (o.konular.isEmpty()) {
            icerik.addView(yazi(getString(R.string.on_konu_yok), 12.5f, 0.7f))
            return
        }
        o.konular.forEach { k ->
            icerik.addView(kart { ic ->
                // Başlık + yüzde
                ic.addView(LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    addView(TextView(this@OnlineActivity).apply {
                        text = k.baslik
                        textSize = 14.5f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        layoutParams = LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                        )
                    })
                    addView(TextView(this@OnlineActivity).apply {
                        text = "%" + k.yuzde
                        textSize = 13f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        setTextColor(
                            MaterialColors.getColor(
                                this@OnlineActivity,
                                com.google.android.material.R.attr.colorPrimary, 0
                            )
                        )
                    })
                    addView(TextView(this@OnlineActivity).apply {
                        text = "＋"
                        textSize = 19f
                        setPadding((10 * yogunluk).toInt(), 0, 0, 0)
                        isClickable = true
                        background = android.graphics.drawable.RippleDrawable(
                            android.content.res.ColorStateList.valueOf(0x22888888), null, null
                        )
                        setOnClickListener {
                            izinli(OnlineStore.Islem.EKLE) { altMaddeEkle(k) }
                        }
                    })
                })
                ic.addView(yazi(
                    getString(R.string.on_konu_alt, k.bitenSayi, k.maddeler.size) +
                        " · " + k.sahip, 11.5f, 0.7f
                ))

                // Alt maddeler
                k.maddeler.forEach { m ->
                    ic.addView(LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(0, (5 * yogunluk).toInt(), 0, (5 * yogunluk).toInt())
                        isClickable = true
                        background = android.graphics.drawable.RippleDrawable(
                            android.content.res.ColorStateList.valueOf(0x18888888), null, null
                        )
                        setOnClickListener { altMaddeTamamla(k, m) }
                        setOnLongClickListener { altMaddeMenusu(k, m); true }
                        addView(TextView(this@OnlineActivity).apply {
                            text = if (m.tamam) "☑" else "☐"
                            textSize = 15f
                            setPadding(0, 0, (8 * yogunluk).toInt(), 0)
                            if (m.tamam) setTextColor(GrafikDili.BASARI)
                        })
                        addView(TextView(this@OnlineActivity).apply {
                            text = m.metin
                            textSize = 13f
                            if (m.tamam) {
                                paintFlags = paintFlags or
                                    android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                                alpha = 0.55f
                            }
                            layoutParams = LinearLayout.LayoutParams(
                                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                            )
                        })
                        if (m.tamam && m.kim.isNotBlank()) {
                            addView(TextView(this@OnlineActivity).apply {
                                text = m.kim
                                textSize = 10.5f
                                alpha = 0.6f
                            })
                        }
                    })
                }
                if (k.maddeler.isEmpty()) {
                    ic.addView(yazi(getString(R.string.on_madde_yok), 12f, 0.6f))
                }
            })
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // BÖLÜM: ALIŞKANLIKLAR
    // ═══════════════════════════════════════════════════════════════

    private fun aliskanliklariCiz(o: OnlineStore.Oda, ben: String) {
        icerik.addView(baslik(
            getString(R.string.on_aliskanliklar, o.aliskanliklar.size), 15f
        ))
        if (o.aliskanliklar.isEmpty()) {
            icerik.addView(yazi(getString(R.string.on_alis_yok), 12.5f, 0.7f))
            return
        }
        val bugun = OnlineStore.bugunAnahtari()

        o.aliskanliklar.forEach { a ->
            val benYaptim = a.bugunYaptiMi(ben, bugun)
            val yapanlar = a.bugunKimler(bugun)

            icerik.addView(kartTiklanir(
                { aliskanlikIsaretle(a) }, { aliskanlikMenusu(a); true }
            ) { ic ->
                ic.addView(LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    addView(TextView(this@OnlineActivity).apply {
                        text = if (benYaptim) "☑" else "☐"
                        textSize = 17f
                        setPadding(0, 0, (10 * yogunluk).toInt(), 0)
                        if (benYaptim) setTextColor(GrafikDili.BASARI)
                    })
                    addView(TextView(this@OnlineActivity).apply {
                        text = a.emoji + " " + a.ad
                        textSize = 14.5f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        layoutParams = LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                        )
                    })
                })
                // Bugün kim yaptı — karşı tarafı da göster
                val satir = o.uyeler.joinToString("   ") { u ->
                    (if (yapanlar.contains(u)) "✅ " else "⬜ ") + u
                }
                ic.addView(yazi(
                    getString(R.string.on_alis_bugun) + "  " + satir, 12f, 0.8f
                ))
            })
        }
    }

    private fun gorevSatiri(g: OnlineStore.Gorev, ben: String): View {
        val kart = MaterialCardView(this).apply {
            radius = 14 * yogunluk
            cardElevation = 0f
            strokeWidth = (1 * yogunluk).toInt()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (7 * yogunluk).toInt() }
            isClickable = true
            setOnClickListener { gorevTamamla(g) }
            setOnLongClickListener { gorevMenusu(g); true }
        }

        val satir = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                (13 * yogunluk).toInt(), (11 * yogunluk).toInt(),
                (13 * yogunluk).toInt(), (11 * yogunluk).toInt()
            )
        }
        satir.addView(TextView(this).apply {
            text = if (g.tamam) "☑" else "☐"
            textSize = 17f
            setPadding(0, 0, (10 * yogunluk).toInt(), 0)
            if (g.tamam) setTextColor(GrafikDili.BASARI)
        })
        satir.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                )
                addView(TextView(this@OnlineActivity).apply {
                    text = g.metin
                    textSize = 14f
                    if (g.tamam) {
                        paintFlags = paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                        alpha = 0.55f
                    }
                })
                addView(TextView(this@OnlineActivity).apply {
                    text = (if (g.sahip == ben) "👤 " else "👥 ") + g.sahip
                    textSize = 11f
                    alpha = 0.65f
                })
                if (g.not.isNotBlank()) {
                    addView(TextView(this@OnlineActivity).apply {
                        text = "💬 " + g.not
                        textSize = 11.5f
                        alpha = 0.8f
                        setPadding(0, (3 * yogunluk).toInt(), 0, 0)
                    })
                }
            }
        )
        kart.addView(satir)
        return kart
    }

    // ═══════════════════════════════════════════════════════════════
    // v7.52 — YETKİ KONTROLÜ
    // ═══════════════════════════════════════════════════════════════

    /**
     * İşlem izinliyse çalıştırır, değilse gerekçesini gösterir.
     * Yönetici her zaman geçer.
     */
    private fun izinli(islem: OnlineStore.Islem, calistir: () -> Unit) {
        if (OnlineStore.izinVar(this, oda, islem)) {
            calistir()
        } else {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.on_yetki_yok)
                .setMessage(OnlineStore.izinMesaji(this, islem))
                .setPositiveButton(R.string.done, null)
                .show()
        }
    }

    /**
     * Yönetici işlemi: oturum açıksa doğrudan, değilse şifre sorar.
     * Şifre yoksa (oda şifresiz kurulduysa) doğrudan geçer.
     */
    private fun yoneticiIsleminde(calistir: () -> Unit) {
        val o = oda
        if (!OnlineStore.yoneticiMiyim(this, o)) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.on_yetki_yok)
                .setMessage(R.string.on_sadece_yonetici)
                .setPositiveButton(R.string.done, null)
                .show()
            return
        }
        if (o?.sifreHash.isNullOrBlank() || OnlineStore.yoneticiOturumu(this)) {
            calistir()
            return
        }
        sifreSor { calistir() }
    }

    private fun sifreSor(sonra: () -> Unit) {
        val g = EditText(this).apply {
            hint = getString(R.string.on_sifre_hint)
            inputType = InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_NUMBER_VARIATION_PASSWORD
            textSize = 20f
            gravity = Gravity.CENTER
            setPadding(
                (20 * yogunluk).toInt(), (16 * yogunluk).toInt(),
                (20 * yogunluk).toInt(), (10 * yogunluk).toInt()
            )
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.on_sifre_gir)
            .setView(g)
            .setPositiveButton(R.string.done) { _, _ ->
                if (OnlineStore.sifreDogruMu(this, oda, g.text?.toString().orEmpty())) {
                    OnlineStore.yoneticiOturumuAc(this)
                    sonra()
                } else {
                    Toast.makeText(this, R.string.on_sifre_yanlis, Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ═══════════════════════════════════════════════════════════════
    // İŞLEMLER
    // ═══════════════════════════════════════════════════════════════

    /** v7.53: Giriş çubuğu aktif bölüme göre farklı öğe ekler. */
    private fun gorevEkle() {
        val metin = girdi.text?.toString()?.trim().orEmpty()
        if (metin.isBlank()) return
        if (calisiyor) return
        // Sohbet herkese açık — yetki yalnızca diğer bölümlerde aranır
        if (aktifBolum != Bolum.SOHBET &&
            !OnlineStore.izinVar(this, oda, OnlineStore.Islem.EKLE)
        ) {
            izinli(OnlineStore.Islem.EKLE) {}
            return
        }
        when (aktifBolum) {
            Bolum.SOHBET -> { mesajGonder(metin); return }
            Bolum.NOT -> { notEkleYeni(metin); return }
            Bolum.KONU -> { konuEkleYeni(metin); return }
            Bolum.ALISKANLIK -> { aliskanlikEkleYeni(metin); return }
            Bolum.GOREV -> { /* aşağıda devam */ }
        }
        girdi.setText("")
        klavyeKapat()

        val ben = OnlineStore.benimAdim(this)
        calisiyor = true
        yukle(true, getString(R.string.on_gonderiliyor))

        worker.execute {
            val s = OnlineStore.guvenliGuncelle(this) { o ->
                o.gorevler.add(
                    OnlineStore.Gorev(
                        id = System.currentTimeMillis(),
                        metin = metin,
                        sahip = ben
                    )
                )
                if (!o.uyeler.contains(ben)) o.uyeler.add(ben)
            }
            val taze = OnlineStore.oku(this)
            runOnUiThread {
                calisiyor = false
                if (isFinishing || isDestroyed) return@runOnUiThread
                yukle(false)
                if (!s.ok) { Toast.makeText(this, s.mesaj, Toast.LENGTH_LONG).show(); return@runOnUiThread }
                oda = taze.oda
                taze.oda?.let { OnlineStore.onbellegeYaz(this, it) }
                ciz()
            }
        }
    }

    private fun gorevTamamla(g: OnlineStore.Gorev) {
        if (calisiyor) return
        // v7.52: tamamlanmış görevin işaretini kaldırmak ayrı yetki
        if (g.tamam && !OnlineStore.izinVar(this, oda, OnlineStore.Islem.GERI_AL)) {
            izinli(OnlineStore.Islem.GERI_AL) {}
            return
        }
        calisiyor = true
        yukle(true, getString(R.string.on_gonderiliyor))
        worker.execute {
            val s = OnlineStore.guvenliGuncelle(this) { o ->
                o.gorevler.firstOrNull { it.id == g.id }?.let { it.tamam = !it.tamam }
            }
            val taze = OnlineStore.oku(this)
            runOnUiThread {
                calisiyor = false
                if (isFinishing || isDestroyed) return@runOnUiThread
                yukle(false)
                if (!s.ok) { Toast.makeText(this, s.mesaj, Toast.LENGTH_LONG).show(); return@runOnUiThread }
                oda = taze.oda
                taze.oda?.let { OnlineStore.onbellegeYaz(this, it) }
                ciz()
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // v7.53 — NOT İŞLEMLERİ
    // ═══════════════════════════════════════════════════════════════

    private fun notEkleYeni(baslik: String) {
        girdi.setText("")
        klavyeKapat()
        val ben = OnlineStore.benimAdim(this)
        degistirVeGonder { o ->
            o.notlar.add(
                OnlineStore.Not(
                    id = System.currentTimeMillis(),
                    baslik = baslik, icerik = "", sahip = ben
                )
            )
            if (!o.uyeler.contains(ben)) o.uyeler.add(ben)
        }
    }

    private fun notGoster(n: OnlineStore.Not) {
        MaterialAlertDialogBuilder(this)
            .setTitle(n.baslik.ifBlank { getString(R.string.on_not_basliksiz) })
            .setMessage(
                (if (n.icerik.isBlank()) getString(R.string.on_not_bos) else n.icerik) +
                    "\n\n— " + n.sahip
            )
            .setPositiveButton(R.string.co_edit) { _, _ -> notDuzenle(n) }
            .setNegativeButton(R.string.done, null)
            .show()
    }

    private fun notDuzenle(n: OnlineStore.Not) {
        val ben = OnlineStore.benimAdim(this)
        if (n.sahip != ben &&
            !OnlineStore.izinVar(this, oda, OnlineStore.Islem.BASKASINI_DUZENLE)
        ) {
            izinli(OnlineStore.Islem.BASKASINI_DUZENLE) {}
            return
        }
        val kap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                (20 * yogunluk).toInt(), (12 * yogunluk).toInt(),
                (20 * yogunluk).toInt(), 0
            )
        }
        val bas = EditText(this).apply {
            setText(n.baslik)
            hint = getString(R.string.on_not_baslik_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        val ic = EditText(this).apply {
            setText(n.icerik)
            hint = getString(R.string.on_not_icerik_hint)
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            minLines = 4
            maxLines = 10
        }
        kap.addView(bas); kap.addView(ic)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.co_edit)
            .setView(android.widget.ScrollView(this).apply { addView(kap) })
            .setPositiveButton(R.string.save) { _, _ ->
                degistirVeGonder { o ->
                    o.notlar.firstOrNull { it.id == n.id }?.let {
                        it.baslik = bas.text?.toString()?.trim().orEmpty().take(80)
                        it.icerik = ic.text?.toString()?.trim().orEmpty().take(1000)
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun notMenusu(n: OnlineStore.Not) {
        MaterialAlertDialogBuilder(this)
            .setTitle(n.baslik.ifBlank { getString(R.string.on_not_basliksiz) })
            .setItems(
                arrayOf(getString(R.string.co_edit), getString(R.string.delete))
            ) { _, hangi ->
                when (hangi) {
                    0 -> notDuzenle(n)
                    1 -> izinli(OnlineStore.Islem.SIL) {
                        degistirVeGonder { o -> o.notlar.removeAll { it.id == n.id } }
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ═══════════════════════════════════════════════════════════════
    // v7.53 — KONU İŞLEMLERİ
    // ═══════════════════════════════════════════════════════════════

    private fun konuEkleYeni(baslik: String) {
        girdi.setText("")
        klavyeKapat()
        val ben = OnlineStore.benimAdim(this)
        degistirVeGonder { o ->
            o.konular.add(
                OnlineStore.Konu(
                    id = System.currentTimeMillis(), baslik = baslik, sahip = ben
                )
            )
            if (!o.uyeler.contains(ben)) o.uyeler.add(ben)
        }
    }

    private fun altMaddeEkle(k: OnlineStore.Konu) {
        val g = EditText(this).apply {
            hint = getString(R.string.on_madde_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setPadding(
                (20 * yogunluk).toInt(), (14 * yogunluk).toInt(),
                (20 * yogunluk).toInt(), (10 * yogunluk).toInt()
            )
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(k.baslik)
            .setView(g)
            .setPositiveButton(R.string.add) { _, _ ->
                val metin = g.text?.toString()?.trim().orEmpty()
                if (metin.isBlank()) return@setPositiveButton
                degistirVeGonder { o ->
                    o.konular.firstOrNull { it.id == k.id }?.maddeler?.add(
                        OnlineStore.AltMadde(
                            id = System.currentTimeMillis(), metin = metin.take(120)
                        )
                    )
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun altMaddeTamamla(k: OnlineStore.Konu, m: OnlineStore.AltMadde) {
        if (m.tamam && !OnlineStore.izinVar(this, oda, OnlineStore.Islem.GERI_AL)) {
            izinli(OnlineStore.Islem.GERI_AL) {}
            return
        }
        val ben = OnlineStore.benimAdim(this)
        degistirVeGonder { o ->
            o.konular.firstOrNull { it.id == k.id }
                ?.maddeler?.firstOrNull { it.id == m.id }?.let {
                    it.tamam = !it.tamam
                    it.kim = if (it.tamam) ben else ""
                }
        }
    }

    private fun altMaddeMenusu(k: OnlineStore.Konu, m: OnlineStore.AltMadde) {
        MaterialAlertDialogBuilder(this)
            .setTitle(m.metin)
            .setItems(
                arrayOf(getString(R.string.delete), getString(R.string.on_konu_sil))
            ) { _, hangi ->
                izinli(OnlineStore.Islem.SIL) {
                    degistirVeGonder { o ->
                        if (hangi == 0) {
                            o.konular.firstOrNull { it.id == k.id }
                                ?.maddeler?.removeAll { it.id == m.id }
                        } else {
                            o.konular.removeAll { it.id == k.id }
                        }
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ═══════════════════════════════════════════════════════════════
    // v7.53 — ALIŞKANLIK İŞLEMLERİ
    // ═══════════════════════════════════════════════════════════════

    private fun aliskanlikEkleYeni(ad: String) {
        girdi.setText("")
        klavyeKapat()
        val ben = OnlineStore.benimAdim(this)
        degistirVeGonder { o ->
            o.aliskanliklar.add(
                OnlineStore.Aliskanlik(
                    id = System.currentTimeMillis(), ad = ad.take(60), sahip = ben
                )
            )
            if (!o.uyeler.contains(ben)) o.uyeler.add(ben)
        }
    }

    /** Bugünkü işareti koyar/kaldırır — her üye kendi işaretini yönetir. */
    private fun aliskanlikIsaretle(a: OnlineStore.Aliskanlik) {
        val ben = OnlineStore.benimAdim(this)
        val bugun = OnlineStore.bugunAnahtari()
        val anahtar = bugun + "|" + ben
        val vardi = a.bugunYaptiMi(ben, bugun)

        if (vardi && !OnlineStore.izinVar(this, oda, OnlineStore.Islem.GERI_AL)) {
            izinli(OnlineStore.Islem.GERI_AL) {}
            return
        }
        degistirVeGonder { o ->
            o.aliskanliklar.firstOrNull { it.id == a.id }?.let {
                if (vardi) it.isaretler.remove(anahtar) else it.isaretler.add(anahtar)
            }
        }
    }

    private fun aliskanlikMenusu(a: OnlineStore.Aliskanlik) {
        MaterialAlertDialogBuilder(this)
            .setTitle(a.emoji + " " + a.ad)
            .setItems(
                arrayOf(getString(R.string.on_alis_emoji), getString(R.string.delete))
            ) { _, hangi ->
                when (hangi) {
                    0 -> emojiSec(a)
                    1 -> izinli(OnlineStore.Islem.SIL) {
                        degistirVeGonder { o -> o.aliskanliklar.removeAll { it.id == a.id } }
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun emojiSec(a: OnlineStore.Aliskanlik) {
        val emojiler = arrayOf("✨", "💧", "🏃", "📖", "💊", "🧘", "🥗", "😴", "🚭", "🦷", "🙏", "🍎")
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.on_alis_emoji)
            .setItems(emojiler) { _, hangi ->
                degistirVeGonder { o ->
                    o.aliskanliklar.firstOrNull { it.id == a.id }?.emoji = emojiler[hangi]
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** Tıklanabilir kart — not ve alışkanlık kartları için. */
    private fun kartTiklanir(
        tikla: () -> Unit,
        uzunBas: () -> Boolean,
        doldur: (LinearLayout) -> Unit
    ): View {
        val k = com.google.android.material.card.MaterialCardView(this).apply {
            radius = 16 * yogunluk
            cardElevation = 0f
            strokeWidth = (1 * yogunluk).toInt()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (9 * yogunluk).toInt() }
            isClickable = true
            setOnClickListener { tikla() }
            setOnLongClickListener { uzunBas() }
        }
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                (14 * yogunluk).toInt(), (12 * yogunluk).toInt(),
                (14 * yogunluk).toInt(), (12 * yogunluk).toInt()
            )
        }
        doldur(ic)
        k.addView(ic)
        return k
    }

    private fun gorevMenusu(g: OnlineStore.Gorev) {
        MaterialAlertDialogBuilder(this)
            .setTitle(g.metin)
            .setItems(
                arrayOf(
                    getString(R.string.on_not_ekle),
                    getString(R.string.on_bana_al),
                    getString(R.string.delete)
                )
            ) { _, hangi ->
                when (hangi) {
                    0 -> {
                        // Başkasının görevine not eklemek ayrı yetki
                        val ben = OnlineStore.benimAdim(this)
                        if (g.sahip != ben) {
                            izinli(OnlineStore.Islem.BASKASINI_DUZENLE) { notEkle(g) }
                        } else notEkle(g)
                    }
                    1 -> sahipDegistir(g)
                    2 -> izinli(OnlineStore.Islem.SIL) { silmeyiOnayla(g) }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun notEkle(g: OnlineStore.Gorev) {
        val gi = EditText(this).apply {
            setText(g.not)
            hint = getString(R.string.on_not_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setPadding(
                (20 * yogunluk).toInt(), (14 * yogunluk).toInt(),
                (20 * yogunluk).toInt(), (10 * yogunluk).toInt()
            )
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.on_not_ekle)
            .setView(gi)
            .setPositiveButton(R.string.save) { _, _ ->
                degistirVeGonder { o ->
                    o.gorevler.firstOrNull { it.id == g.id }?.not =
                        gi.text?.toString()?.trim().orEmpty().take(120)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun sahipDegistir(g: OnlineStore.Gorev) {
        val ben = OnlineStore.benimAdim(this)
        degistirVeGonder { o ->
            o.gorevler.firstOrNull { it.id == g.id }?.sahip = ben
        }
    }

    private fun silmeyiOnayla(g: OnlineStore.Gorev) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete)
            .setMessage(getString(R.string.on_sil_onay, g.metin))
            .setPositiveButton(R.string.cmd_confirm_yes) { _, _ -> gorevSil(g) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun gorevSil(g: OnlineStore.Gorev) {
        degistirVeGonder { o -> o.gorevler.removeAll { it.id == g.id } }
    }

    /** Ortak: değiştir → gönder → tazele → çiz. */
    private fun degistirVeGonder(degistir: (OnlineStore.Oda) -> Unit) {
        if (calisiyor) return
        calisiyor = true
        yukle(true, getString(R.string.on_gonderiliyor))
        worker.execute {
            val s = OnlineStore.guvenliGuncelle(this, degistir)
            val taze = OnlineStore.oku(this)
            runOnUiThread {
                calisiyor = false
                if (isFinishing || isDestroyed) return@runOnUiThread
                yukle(false)
                if (!s.ok) { Toast.makeText(this, s.mesaj, Toast.LENGTH_LONG).show(); return@runOnUiThread }
                oda = taze.oda
                taze.oda?.let { OnlineStore.onbellegeYaz(this, it) }
                ciz()
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // AYARLAR
    // ═══════════════════════════════════════════════════════════════

    /**
     * Online oda ayarlari.
     *
     * v7.77: Indeks kaydirma mantigi (`hangi - kaydirma`) kaldirildi.
     * Yonetici olup olmamaya gore liste degistigi icin indeksler
     * kayiyordu ve her yeni secenek hata riski tasiyordu. Artik
     * secenekler (etiket, islem) ciftleri olarak tutuluyor —
     * sira degisse bile dogru islem calisir.
     */
    private fun ayarlar() {
        if (!OnlineStore.bagliMi(this)) { karsilamaCiz(); return }
        val yonetici = OnlineStore.yoneticiMiyim(this, oda)
        val yoneticisiz = OnlineStore.yoneticisizMi(oda)

        val secenekler = mutableListOf<Pair<String, () -> Unit>>()

        // Yonetici secenekleri (yoneticisiz odada herkes yonetici sayilir)
        if (yonetici) {
            secenekler.add(getString(R.string.on_yetkiler) to {
                yoneticiIsleminde { yetkiEkrani() }
            })
            if (!yoneticisiz) {
                secenekler.add(getString(R.string.on_sifre_degistir) to {
                    yoneticiIsleminde { sifreDegistir() }
                })
                // v7.77: yoneticiligi baskasina devret
                secenekler.add(getString(R.string.yn_devret) to {
                    yoneticiIsleminde { yoneticiDevretSor() }
                })
            }
        }

        secenekler.add(getString(R.string.on_kod_goster) to {
            koduGoster(OnlineStore.kod(this))
        })
        secenekler.add(getString(R.string.on_ad_degistir) to {
            izinli(OnlineStore.Islem.AD_DEGISTIR) { adDegistir() }
        })
        secenekler.add(
            (if (OnlineStore.otoSenkron(this)) getString(R.string.on_oto_kapat)
            else getString(R.string.on_oto_ac)) to {
                izinli(OnlineStore.Islem.BILDIRIM_KAPAT) {
                    OnlineStore.setOtoSenkron(this, !OnlineStore.otoSenkron(this))
                    Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show()
                }
            }
        )
        secenekler.add(getString(R.string.ob_menu) to {
            OnlineBekciActivity.ac(this)
        })
        // v7.77: ayrilmak izne bagli degil — her zaman en altta
        secenekler.add(getString(R.string.on_ayril) to { ayrilmaSor() })

        MaterialAlertDialogBuilder(this)
            .setTitle(
                if (yonetici && !yoneticisiz) getString(R.string.on_ayarlar_y)
                else getString(R.string.on_ayarlar)
            )
            .setItems(secenekler.map { it.first }.toTypedArray()) { _, hangi ->
                secenekler.getOrNull(hangi)?.second?.invoke()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * v7.52: Yönetici, üyenin nelere izinli olduğunu buradan belirler.
     * Değişiklik anında sunucuya yazılır, karşı taraf yenileyince görür.
     */
    private fun yetkiEkrani() {
        val o = oda ?: return
        val kap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                (20 * yogunluk).toInt(), (10 * yogunluk).toInt(),
                (20 * yogunluk).toInt(), 0
            )
        }
        kap.addView(yazi(getString(R.string.on_yetki_aciklama), 12.5f, 0.8f))

        // (etiket, açıklama, mevcut değer, ayarlayıcı)
        data class Satir(
            val ad: Int, val aciklama: Int,
            val deger: Boolean, val ayarla: (OnlineStore.Kural, Boolean) -> Unit
        )
        val satirlar = listOf(
            Satir(R.string.on_y_ekle, R.string.on_y_ekle_d, o.kural.ekleyebilir)
                { k, v -> k.ekleyebilir = v },
            Satir(R.string.on_y_sil, R.string.on_y_sil_d, o.kural.silebilir)
                { k, v -> k.silebilir = v },
            Satir(R.string.on_y_gerial, R.string.on_y_gerial_d, o.kural.geriAlabilir)
                { k, v -> k.geriAlabilir = v },
            Satir(R.string.on_y_baskasi, R.string.on_y_baskasi_d, o.kural.baskasiniDuzenler)
                { k, v -> k.baskasiniDuzenler = v },
            Satir(R.string.on_y_ad, R.string.on_y_ad_d, o.kural.adDegistirir)
                { k, v -> k.adDegistirir = v },
            Satir(R.string.on_y_bildirim, R.string.on_y_bildirim_d, o.kural.bildirimKapatir)
                { k, v -> k.bildirimKapatir = v },
            Satir(R.string.on_y_ayril, R.string.on_y_ayril_d, o.kural.ayrilabilir)
                { k, v -> k.ayrilabilir = v },
            // v7.56: bildirim kilitleri — kapali ise uye bu ayarlari kapatamaz
            Satir(R.string.on_y_ses, R.string.on_y_ses_d, o.kural.sesKapatir)
                { k, v -> k.sesKapatir = v },
            Satir(R.string.on_y_titresim, R.string.on_y_titresim_d, o.kural.titresimKapatir)
                { k, v -> k.titresimKapatir = v },
            Satir(R.string.on_y_bana, R.string.on_y_bana_d, o.kural.bildirimKapatirTum)
                { k, v -> k.bildirimKapatirTum = v },
            Satir(R.string.on_y_zorunlu, R.string.on_y_zorunlu_d, o.kural.zorunluKapatir)
                { k, v -> k.zorunluKapatir = v }
        )

        val anahtarlar = mutableListOf<Pair<Satir,
            com.google.android.material.materialswitch.MaterialSwitch>>()
        satirlar.forEach { st ->
            val satir = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, (7 * yogunluk).toInt(), 0, (7 * yogunluk).toInt())
            }
            satir.addView(
                LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                    )
                    addView(TextView(this@OnlineActivity).apply {
                        text = getString(st.ad); textSize = 14f
                    })
                    addView(TextView(this@OnlineActivity).apply {
                        text = getString(st.aciklama); textSize = 11.5f; alpha = 0.7f
                    })
                }
            )
            val sw = com.google.android.material.materialswitch.MaterialSwitch(this).apply {
                isChecked = st.deger
            }
            anahtarlar.add(st to sw)
            satir.addView(sw)
            kap.addView(satir)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.on_yetkiler)
            .setView(android.widget.ScrollView(this).apply { addView(kap) })
            .setPositiveButton(R.string.save) { _, _ ->
                degistirVeGonder { od ->
                    anahtarlar.forEach { (st, sw) -> st.ayarla(od.kural, sw.isChecked) }
                }
                Toast.makeText(this, R.string.on_yetki_kaydedildi, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** v7.52: Yönetici şifresini değiştirir. */
    private fun sifreDegistir() {
        val g = EditText(this).apply {
            hint = getString(R.string.on_sifre_hint)
            inputType = InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_NUMBER_VARIATION_PASSWORD
            filters = arrayOf(android.text.InputFilter.LengthFilter(8))
            textSize = 20f
            gravity = Gravity.CENTER
            setPadding(
                (20 * yogunluk).toInt(), (16 * yogunluk).toInt(),
                (20 * yogunluk).toInt(), (10 * yogunluk).toInt()
            )
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.on_sifre_degistir)
            .setMessage(R.string.on_sifre_msg)
            .setView(g)
            .setPositiveButton(R.string.save) { _, _ ->
                val yeni = g.text?.toString()?.trim().orEmpty()
                if (yeni.isNotBlank() && yeni.length < 4) {
                    Toast.makeText(this, R.string.on_sifre_kisa, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val kod = OnlineStore.kod(this)
                degistirVeGonder { od ->
                    od.sifreHash = if (yeni.isBlank()) "" else
                        OnlineStore.sifreKarma(yeni, kod)
                }
                OnlineStore.yoneticiOturumuAc(this)
                Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun adDegistir() {
        val g = EditText(this).apply {
            setText(OnlineStore.benimAdim(this@OnlineActivity))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setPadding(
                (20 * yogunluk).toInt(), (14 * yogunluk).toInt(),
                (20 * yogunluk).toInt(), (10 * yogunluk).toInt()
            )
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.on_ad_degistir)
            .setView(g)
            .setPositiveButton(R.string.save) { _, _ ->
                val yeni = g.text?.toString()?.trim().orEmpty()
                if (yeni.isNotBlank()) {
                    val eski = OnlineStore.benimAdim(this)
                    OnlineStore.setBenimAdim(this, yeni)
                    degistirVeGonder { o ->
                        val i = o.uyeler.indexOf(eski)
                        if (i >= 0) o.uyeler[i] = yeni else o.uyeler.add(yeni)
                        o.gorevler.forEach { if (it.sahip == eski) it.sahip = yeni }
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * v7.77 — Odadan ayrilma.
     *
     * Artik izne bagli degil: kimse kimseyi odada zorla tutamaz.
     * Yoneticiysen ek bir uyari gosterilir ve ayrilmadan once
     * yoneticiligi kalan uyeye devretmeyi deneriz — oda sahipsiz
     * kalmasin diye.
     */
    private fun ayrilmaSor() {
        val yoneticiyim = OnlineStore.yoneticiMiyim(this, oda) &&
            !OnlineStore.yoneticisizMi(oda)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.on_ayril)
            .setMessage(
                if (yoneticiyim) getString(R.string.on_ayril_yonetici)
                else getString(R.string.on_ayril_msg)
            )
            .setPositiveButton(R.string.cmd_confirm_yes) { _, _ ->
                if (yoneticiyim) yoneticiligiBirakVeAyril() else hemenAyril()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** Sunucuya dokunmadan yerel baglantiyi keser. */
    private fun hemenAyril() {
        OnlineStore.ayril(this)
        oda = null
        karsilamaCiz()
    }

    /**
     * Yonetici ayriliyor: kalan ilk uyeye devret, kendini uye
     * listesinden cikar, sonra ayril.
     *
     * Ag hatasi olsa bile YEREL ayrilma yapilir — kullanici internet
     * yokken odada mahsur kalmamali.
     */
    private fun yoneticiligiBirakVeAyril() {
        val ben = OnlineStore.benimAdim(this)
        calisiyor = true
        yukle(true, getString(R.string.on_ayril))
        worker.execute {
            try {
                OnlineStore.guvenliGuncelle(this) { o ->
                    o.uyeler.remove(ben)
                    if (o.yonetici == ben) {
                        // Kalan ilk uye yonetici olsun; kimse yoksa bos kalir
                        o.yonetici = o.uyeler.firstOrNull().orEmpty()
                        o.sifreHash = ""
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("OnlineActivity", "Devir yapilamadi", e)
            }
            runOnUiThread {
                calisiyor = false
                if (isFinishing || isDestroyed) return@runOnUiThread
                yukle(false)
                hemenAyril()
            }
        }
    }

    /** v7.77: Yoneticiligi baska uyeye devreder (ayrilmadan). */
    private fun yoneticiDevretSor() {
        val o = oda ?: return
        val ben = OnlineStore.benimAdim(this)
        val adaylar = o.uyeler.filter { it != ben }
        if (adaylar.isEmpty()) {
            Toast.makeText(this, R.string.yn_kimse_yok, Toast.LENGTH_SHORT).show()
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.yn_devret)
            .setItems(adaylar.toTypedArray()) { _, hangi ->
                val yeni = adaylar[hangi]
                MaterialAlertDialogBuilder(this)
                    .setMessage(getString(R.string.yn_devret_sor, yeni))
                    .setPositiveButton(R.string.cmd_confirm_yes) { _, _ ->
                        calisiyor = true
                        yukle(true, getString(R.string.yn_devret))
                        worker.execute {
                            val g = OnlineStore.guvenliGuncelle(this) { od ->
                                od.yonetici = yeni
                                od.sifreHash = ""
                            }
                            val taze = OnlineStore.oku(this)
                            runOnUiThread {
                                calisiyor = false
                                if (isFinishing || isDestroyed) return@runOnUiThread
                                yukle(false)
                                if (!g.ok) { hata(g.mesaj); return@runOnUiThread }
                                OnlineStore.setYoneticiBayragi(this, false)
                                OnlineStore.yoneticiOturumuKapat(this)
                                oda = taze.oda
                                taze.oda?.let { OnlineStore.onbellegeYaz(this, it) }
                                ciz()
                                Toast.makeText(
                                    this,
                                    getString(R.string.yn_devredildi, yeni),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ═══════════════════════════════════════════════════════════════
    // YARDIMCI
    // ═══════════════════════════════════════════════════════════════

    private fun hata(mesaj: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.on_hata)
            .setMessage(mesaj)
            .setPositiveButton(R.string.done, null)
            .show()
    }

    private fun yukle(goster: Boolean, metin: String = "") {
        yukleniyor.visibility = if (goster) View.VISIBLE else View.GONE
        if (metin.isNotBlank()) yukleniyorYazi.text = metin
    }

    private fun kart(doldur: (LinearLayout) -> Unit): View {
        val k = MaterialCardView(this).apply {
            radius = 16 * yogunluk
            cardElevation = 0f
            strokeWidth = (1 * yogunluk).toInt()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (10 * yogunluk).toInt() }
        }
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                (14 * yogunluk).toInt(), (13 * yogunluk).toInt(),
                (14 * yogunluk).toInt(), (13 * yogunluk).toInt()
            )
        }
        doldur(ic)
        k.addView(ic)
        return k
    }

    private fun baslik(metin: String, boyut: Float) = TextView(this).apply {
        text = metin
        textSize = boyut
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, (8 * yogunluk).toInt(), 0, (4 * yogunluk).toInt())
    }

    private fun yazi(metin: String, boyut: Float, saydam: Float) = TextView(this).apply {
        text = metin
        textSize = boyut
        alpha = saydam
        setLineSpacing(2f * yogunluk, 1f)
        setPadding(0, (2 * yogunluk).toInt(), 0, (2 * yogunluk).toInt())
    }

    /** v7.54: Küçük yan yana düğme — sohbetteki hızlı sorular için. */
    private fun kucukDugme(metin: String, tiklayinca: () -> Unit) = TextView(this).apply {
        text = metin
        textSize = 12f
        gravity = Gravity.CENTER
        setPadding(
            (12 * yogunluk).toInt(), (8 * yogunluk).toInt(),
            (12 * yogunluk).toInt(), (8 * yogunluk).toInt()
        )
        setTextColor(
            MaterialColors.getColor(
                this@OnlineActivity, com.google.android.material.R.attr.colorPrimary, 0
            )
        )
        background = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = 14 * yogunluk
            setStroke(
                (1 * yogunluk).toInt(),
                (MaterialColors.getColor(
                    this@OnlineActivity,
                    com.google.android.material.R.attr.colorPrimary, 0
                ) and 0x00FFFFFF) or 0x55000000
            )
        }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { marginEnd = (6 * yogunluk).toInt() }
        isClickable = true
        setOnClickListener { tiklayinca() }
    }

    private fun anaDugme(metin: String, tiklayinca: () -> Unit) = TextView(this).apply {
        text = metin
        textSize = 14f
        gravity = Gravity.CENTER
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setTextColor(
            MaterialColors.getColor(
                this@OnlineActivity, com.google.android.material.R.attr.colorPrimary, 0
            )
        )
        setPadding(0, (12 * yogunluk).toInt(), 0, (6 * yogunluk).toInt())
        background = android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf(0x22888888), null, null
        )
        isClickable = true
        setOnClickListener { tiklayinca() }
    }

    private fun klavyeKapat() {
        try {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE)
                as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(girdi.windowToken, 0)
        } catch (e: Exception) {
            android.util.Log.w("OnlineActivity", "Klavye kapatılamadı", e)
        }
    }

    override fun onDestroy() {
        worker.shutdownNow()
        super.onDestroy()
    }
}
