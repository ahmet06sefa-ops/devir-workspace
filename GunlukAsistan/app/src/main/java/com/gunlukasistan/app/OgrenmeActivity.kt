package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * v9.6 — Öğrenme merkezi (öneri 31, 33, 35, 36).
 *
 * Dört özellik tek ekranda toplandı çünkü hepsi aynı soruyu farklı
 * açılardan yanıtlıyor: **gerçekten öğreniyor muyum?**
 *
 *   · Zayıf nokta radarı  → nerede zorlanıyorum
 *   · Ön/son test         → ne kadar ilerledim
 *   · Feynman             → gerçekten anladım mı
 *   · Sınav simülasyonu   → baskı altında ne yapıyorum
 *
 * Ayrı ekranlara bölmek kullanıcıyı gezdirirdi ve hiçbiri
 * keşfedilmezdi.
 */
class OgrenmeActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            context.startActivity(Intent(context, OgrenmeActivity::class.java))
            (context as? android.app.Activity)?.let { Canlandir.activityGirisi(it) }
        }
    }

    private lateinit var kok: LinearLayout
    private var yukleniyor = false

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(GorunumAyar.yaziOlcegiUygula(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.styleFor(this))
        ThemeManager.applyAccent(this)
        ThemeManager.dinamikRengiUygula(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gorunum_ayar)

        // v10.0 · Görsel öneri 4: ortak üst bar.
        // Düğme boyutu, yazı boyutu ve dokunma hedefi tek
        // yerden geliyor — ekranlar arası geçerken başlık
        // artık zıplamıyor.
        UstBar.kur(this, getString(R.string.og_baslik))
        kok = findViewById(R.id.gaKok)
        yukle()
    }

    override fun finish() {
        super.finish()
        Canlandir.activityCikisi(this)
    }

    override fun onResume() {
        super.onResume()
        if (!yukleniyor) yukle()
    }

    // ══════════════════════════════════════════════════════════

    private fun yukle() {
        kok.removeAllViews()
        val yg = resources.displayMetrics.density

        // Zayıf nokta analizi diski geziyor — arka planda
        kok.addView(Iskelet(this).apply {
            sekil = Iskelet.SEKIL_LISTE
            satirSayisi = 4
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (yg * 280).toInt()
            )
        })

        ArkaPlan.calis(
            this,
            is_ = {
                Triple(
                    runCatching { ZayifNokta.analiz(this) }.getOrDefault(emptyList()),
                    runCatching { OlcmeTest.kazanimlar(this) }.getOrDefault(emptyList()),
                    runCatching { Feynman.ortalamaPuan(this) }.getOrNull()
                )
            }
        ) { (zayiflar, kazanimlar, feynmanOrt) ->
            kok.removeAllViews()
            ciz(zayiflar, kazanimlar, feynmanOrt)
        }
    }

    private fun ciz(
        zayiflar: List<ZayifNokta.Bulgu>,
        kazanimlar: List<OlcmeTest.Kazanim>,
        feynmanOrt: Int?
    ) {
        val yg = resources.displayMetrics.density
        fun dp(v: Int) = (v * yg).toInt()

        // ══════ 1. Zayıf nokta radarı (öneri 35) ══════
        baslik(getString(R.string.zn_baslik))
        bilgiKarti(ZayifNokta.genelYorum(this), vurgulu = zayiflar.any { it.seviye == 2 })

        zayiflar.take(5).forEach { b -> zayifKarti(b) }

        // ══════ 2. Öğrenme kazanımı (öneri 31) ══════
        baslik(getString(R.string.ot_kazanim_baslik))
        if (kazanimlar.isEmpty()) {
            bilgiKarti(getString(R.string.ot_kazanim_yok), vurgulu = false)
        } else {
            OlcmeTest.ortalamaKazanim(this)?.let { ort ->
                bilgiKarti(
                    getString(R.string.ot_ort_kazanim, (ort * 100).toInt()),
                    vurgulu = true
                )
            }
            kazanimlar.take(4).forEach { k -> kazanimKarti(k) }
        }
        kok.addView(MaterialButton(
            this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            setText(R.string.ot_test_yap)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
            setOnClickListener { Titresim.dokunus(it); testKonusuSec() }
        })

        // ══════ 3. Feynman (öneri 33) ══════
        baslik(getString(R.string.fy_baslik))
        bilgiKarti(
            if (feynmanOrt != null) getString(R.string.fy_ortalama, feynmanOrt)
            else getString(R.string.fy_aciklama),
            vurgulu = false
        )
        kok.addView(MaterialButton(this).apply {
            setText(R.string.fy_anlat)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener { Titresim.dokunus(it); feynmanPenceresi() }
        })

        val sonDenemeler = Feynman.denemeler(this).take(3)
        sonDenemeler.forEach { d -> feynmanKarti(d) }

        // ══════ 4. Sınav simülasyonu (öneri 36) ══════
        baslik(getString(R.string.ot_sim_baslik))
        val simulasyonlar = OlcmeTest.simulasyonlar(this)
        if (simulasyonlar.isEmpty()) {
            bilgiKarti(getString(R.string.ot_sim_aciklama), vurgulu = false)
        } else {
            simulasyonlar.take(3).forEach { s -> simulasyonKarti(s) }
        }
        kok.addView(MaterialButton(this).apply {
            setText(R.string.ot_sim_basla)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
            setOnClickListener { Titresim.dokunus(it); simulasyonBaslat() }
        })

        kok.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(40)
            )
        })
    }

    // ══════════════════════════════════════════════════════════
    // Kartlar
    // ══════════════════════════════════════════════════════════

    private fun zayifKarti(b: ZayifNokta.Bulgu) {
        val yg = resources.displayMetrics.density
        fun dp(v: Int) = (v * yg).toInt()
        val renkKodu = when (b.seviye) {
            2 -> 0xFFD9534F.toInt()
            1 -> 0xFFE0A33A.toInt()
            else -> 0xFF4C9A5A.toInt()
        }
        val kart = MaterialCardView(this).apply {
            radius = 16 * yg
            cardElevation = 0f
            strokeWidth = dp(2)
            strokeColor = renkKodu
            setCardBackgroundColor(renk(com.google.android.material.R.attr.colorSurface))
            isClickable = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
            setOnClickListener {
                Titresim.dokunus(it)
                // Konuya git
                startActivity(Intent(this@OgrenmeActivity, MainActivity::class.java).apply {
                    putExtra("screen", 3)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                })
            }
        }
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        val ust = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        ust.addView(TextView(this).apply {
            text = KonuGorunum.baslikla(this@OgrenmeActivity, b.konuId, b.konuAdi)
            textSize = 15.5f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
        })
        ust.addView(TextView(this).apply {
            text = b.puan.toString()
            textSize = 19f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(renkKodu)
        })
        ic.addView(ust)
        b.sebepler.take(3).forEach { s ->
            ic.addView(TextView(this).apply {
                text = "• $s"
                textSize = 12.5f
                setPadding(0, dp(4), 0, 0)
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            })
        }
        kart.addView(ic)
        kok.addView(kart)
    }

    private fun kazanimKarti(k: OlcmeTest.Kazanim) {
        val yg = resources.displayMetrics.density
        fun dp(v: Int) = (v * yg).toInt()
        val renkKodu = when (k.seviye) {
            2 -> 0xFF4C9A5A.toInt()
            1 -> 0xFFE0A33A.toInt()
            else -> 0xFFD9534F.toInt()
        }
        val kart = MaterialCardView(this).apply {
            radius = 14 * yg
            cardElevation = 0f
            strokeWidth = dp(1)
            strokeColor = renk(com.google.android.material.R.attr.colorOutlineVariant)
            setCardBackgroundColor(renk(com.google.android.material.R.attr.colorSurface))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(6) }
        }
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        ic.addView(TextView(this).apply {
            text = k.konuAdi
            textSize = 14f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
        })
        ic.addView(TextView(this).apply {
            text = getString(
                R.string.ot_kazanim_satir,
                k.onYuzde, k.sonYuzde,
                if (k.hamFark >= 0) "+${k.hamFark}" else "${k.hamFark}"
            )
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, dp(4), 0, 0)
            setTextColor(renkKodu)
        })
        kart.addView(ic)
        kok.addView(kart)
    }

    private fun feynmanKarti(d: Feynman.Deneme) {
        val yg = resources.displayMetrics.density
        fun dp(v: Int) = (v * yg).toInt()
        val kart = MaterialCardView(this).apply {
            radius = 14 * yg
            cardElevation = 0f
            strokeWidth = dp(1)
            strokeColor = renk(com.google.android.material.R.attr.colorOutlineVariant)
            setCardBackgroundColor(renk(com.google.android.material.R.attr.colorSurface))
            isClickable = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(6) }
            setOnClickListener { Titresim.dokunus(it); feynmanDetay(d) }
        }
        val satir = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        satir.addView(TextView(this).apply {
            text = d.konu
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
        })
        satir.addView(TextView(this).apply {
            text = "%${d.puan}"
            textSize = 15f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(
                when (d.seviye) {
                    2 -> 0xFF4C9A5A.toInt()
                    1 -> 0xFFE0A33A.toInt()
                    else -> 0xFFD9534F.toInt()
                }
            )
        })
        kart.addView(satir)
        kok.addView(kart)
    }

    private fun simulasyonKarti(s: OlcmeTest.Simulasyon) {
        val yg = resources.displayMetrics.density
        fun dp(v: Int) = (v * yg).toInt()
        val kart = MaterialCardView(this).apply {
            radius = 14 * yg
            cardElevation = 0f
            strokeWidth = dp(1)
            strokeColor = renk(com.google.android.material.R.attr.colorOutlineVariant)
            setCardBackgroundColor(renk(com.google.android.material.R.attr.colorSurface))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(6) }
        }
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        ic.addView(TextView(this).apply {
            text = getString(
                R.string.ot_sim_satir,
                s.dogru, s.yanlis, s.bos,
                String.format(java.util.Locale.US, "%.2f", s.net)
            )
            textSize = 13.5f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
        })
        ic.addView(TextView(this).apply {
            text = java.text.SimpleDateFormat("d MMM HH:mm", java.util.Locale("tr"))
                .format(java.util.Date(s.zaman))
            textSize = 11.5f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
        })
        kart.addView(ic)
        kok.addView(kart)
    }

    // ══════════════════════════════════════════════════════════
    // Eylemler
    // ══════════════════════════════════════════════════════════

    /** Ön/son test için konu seçimi. */
    private fun testKonusuSec() {
        val konular = Store.loadTopics(this).filter { it.items.isNotEmpty() }
        if (konular.isEmpty()) {
            Bildir.bilgi(kok, getString(R.string.ot_konu_yok))
            return
        }
        AltSayfa.menu(
            this, getString(R.string.ot_test_yap),
            konular.take(20).map { konu ->
                val onVar = OlcmeTest.onTestVarMi(this, konu.id)
                val bekliyor = OlcmeTest.sonTestBekliyorMu(this, konu.id)
                AltSayfa.Oge(
                    konu.title,
                    altBaslik = when {
                        bekliyor -> getString(R.string.ot_son_test_bekliyor)
                        onVar -> getString(R.string.ot_tamamlandi)
                        else -> getString(R.string.ot_on_test_yap)
                    },
                    simge = if (bekliyor) "🎯" else "📝"
                ) { testBaslat(konu, sonTest = bekliyor) }
            },
            altBaslik = getString(R.string.ot_test_aciklama)
        )
    }

    /**
     * Ön veya son testi başlatır.
     *
     * Sorular konunun quiz havuzundan geliyor. Havuz boşsa
     * kullanıcı önce quiz üretmeli.
     */
    private fun testBaslat(konu: Store.Topic, sonTest: Boolean) {
        val sorular = mutableListOf<QuizStore.Soru>()
        konu.items.forEach { m ->
            runCatching { sorular.addAll(QuizStore.havuzdanSinav(this, -m.id, adet = 3)) }
        }
        val secilen = sorular.distinctBy { it.metin }.filter { it.gecerli }.shuffled().take(10)

        if (secilen.size < 3) {
            Bildir.hata(kok, getString(R.string.ot_soru_yetersiz))
            return
        }

        // Geçici soru havuzu mekanizması (v7.84) yeniden kullanılıyor
        runCatching {
            // Hatalarim.geciciAyarla(sorular, baslik) — Context almıyor
                Hatalarim.geciciAyarla(
                    secilen,
                    getString(
                        if (sonTest) R.string.ot_son_test else R.string.ot_on_test,
                        konu.title
                    )
                )
            OlcmeBekleyen.ayarla(
                this, konu.id, konu.title,
                if (sonTest) OlcmeTest.TUR_SON else OlcmeTest.TUR_ON,
                secilen.size
            )
            QuizActivity.acGecici(
                this,
                getString(
                    if (sonTest) R.string.ot_son_test else R.string.ot_on_test,
                    konu.title
                )
            )
        }.onFailure {
            android.util.Log.w("OgrenmeActivity", "Test başlatılamadı", it)
            Bildir.hata(kok, getString(R.string.ot_baslatilamadi))
        }
    }

    private fun simulasyonBaslat() {
        if (!OlcmeTest.havuzYeterliMi(this, 10)) {
            Bildir.hata(kok, getString(R.string.ot_sim_yetersiz))
            return
        }
        AltSayfa.menu(
            this, getString(R.string.ot_sim_basla),
            listOf(
                AltSayfa.Oge(getString(R.string.ot_sim_kisa), simge = "⚡") {
                    simulasyonAc(OlcmeTest.Ayar(soruSayisi = 10, sureDk = 12))
                },
                AltSayfa.Oge(getString(R.string.ot_sim_orta_s), simge = "📋") {
                    simulasyonAc(OlcmeTest.Ayar(soruSayisi = 20, sureDk = 25))
                },
                AltSayfa.Oge(getString(R.string.ot_sim_uzun), simge = "🎓") {
                    simulasyonAc(OlcmeTest.Ayar(soruSayisi = 40, sureDk = 50))
                }
            ),
            altBaslik = getString(R.string.ot_sim_aciklama)
        )
    }

    private fun simulasyonAc(ayar: OlcmeTest.Ayar) {
        ArkaPlan.calis(
            this,
            is_ = { OlcmeTest.sorulariHazirla(this, ayar) }
        ) { sorular ->
            if (sorular.size < 3) {
                Bildir.hata(kok, getString(R.string.ot_sim_yetersiz))
                return@calis
            }
            runCatching {
                Hatalarim.geciciAyarla(sorular, getString(R.string.ot_sim_baslik))
                OlcmeBekleyen.simulasyonAyarla(this, ayar.sureDk, sorular.size)
                QuizActivity.acGecici(this, getString(R.string.ot_sim_baslik))
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // Feynman
    // ══════════════════════════════════════════════════════════

    private fun feynmanPenceresi() {
        val yg = resources.displayMetrics.density
        fun dp(v: Int) = (v * yg).toInt()

        val govde = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(12), dp(22), dp(4))
        }
        govde.addView(TextView(this).apply {
            setText(R.string.fy_yonerge)
            textSize = 12.5f
            setLineSpacing(0f, 1.3f)
            setPadding(0, 0, 0, dp(12))
        })

        val konuGiris = TextInputEditText(this).apply {
            hint = getString(R.string.fy_konu_hint)
            setSingleLine(true)
        }
        govde.addView(TextInputLayout(this).apply { addView(konuGiris) })

        val anlatimGiris = TextInputEditText(this).apply {
            hint = getString(R.string.fy_anlatim_hint)
            minLines = 5
            maxLines = 10
            gravity = Gravity.TOP or Gravity.START
        }
        govde.addView(TextInputLayout(this).apply {
            addView(anlatimGiris)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        })

        val pencere = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(R.string.fy_anlat)
            .setView(android.widget.ScrollView(this).apply { addView(govde) })
            .setPositiveButton(R.string.fy_degerlendir, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        pencere.setOnShowListener {
            pencere.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val konu = konuGiris.text?.toString()?.trim() ?: ""
                val anlatim = anlatimGiris.text?.toString()?.trim() ?: ""
                if (konu.isBlank()) {
                    Titresim.ret(konuGiris); return@setOnClickListener
                }
                if (anlatim.length < Feynman.EN_AZ_UZUNLUK) {
                    Titresim.ret(anlatimGiris)
                    Bildir.hata(kok, getString(R.string.fy_cok_kisa, Feynman.EN_AZ_UZUNLUK))
                    return@setOnClickListener
                }
                pencere.dismiss()
                feynmanDegerlendir(konu, anlatim)
            }
        }
        pencere.show()
    }

    private fun feynmanDegerlendir(konu: String, anlatim: String) {
        yukleniyor = true
        kok.removeAllViews()
        kok.addView(TextView(this).apply {
            setText(R.string.fy_degerlendiriliyor)
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, (resources.displayMetrics.density * 30).toInt(), 0, 0)
        })
        kok.addView(Iskelet(this).apply {
            sekil = Iskelet.SEKIL_METIN
            satirSayisi = 3
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (resources.displayMetrics.density * 140).toInt()
            )
        })

        ArkaPlan.calisGuvenli(
            this,
            is_ = { Feynman.degerlendir(this, konu, anlatim) },
            hata = { e ->
                yukleniyor = false
                Bildir.hata(kok, e.message ?: getString(R.string.sc_ai_hata))
                yukle()
            }
        ) { sonuc ->
            yukleniyor = false
            if (sonuc.ok && sonuc.deneme != null) {
                Feynman.kaydet(this, sonuc.deneme)
                Titresim.dogru(this)
                yukle()
                feynmanDetay(sonuc.deneme)
            } else {
                Bildir.hata(kok, sonuc.hata)
                yukle()
            }
        }
    }

    private fun feynmanDetay(d: Feynman.Deneme) {
        val metin = buildString {
            appendLine(getString(R.string.fy_puan, d.puan))
            if (d.ozet.isNotBlank()) { appendLine(); appendLine(d.ozet) }
            if (d.eksikler.isNotEmpty()) {
                appendLine()
                appendLine(getString(R.string.fy_eksikler))
                d.eksikler.forEach { appendLine("• $it") }
            }
            if (d.jargon.isNotEmpty()) {
                appendLine()
                appendLine(getString(R.string.fy_jargon))
                d.jargon.forEach { appendLine("• $it") }
            }
        }
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(d.konu)
            .setMessage(metin)
            .setPositiveButton(R.string.done, null)
            .setNegativeButton(R.string.delete) { _, _ ->
                Feynman.sil(this, d.id); yukle()
            }
            .show()
    }

    // ══════════════════════════════════════════════════════════

    private fun baslik(metin: String) {
        val yg = resources.displayMetrics.density
        kok.addView(TextView(this).apply {
            text = metin
            textSize = 12.5f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
            setPadding((4 * yg).toInt(), (22 * yg).toInt(), 0, (6 * yg).toInt())
        })
    }

    private fun bilgiKarti(metin: String, vurgulu: Boolean) {
        val yg = resources.displayMetrics.density
        val kart = MaterialCardView(this).apply {
            radius = 16 * yg
            cardElevation = 0f
            setCardBackgroundColor(
                if (vurgulu) renk(com.google.android.material.R.attr.colorPrimaryContainer)
                else renk(com.google.android.material.R.attr.colorSurface)
            )
            strokeWidth = if (vurgulu) 0 else (1 * yg).toInt()
            strokeColor = renk(com.google.android.material.R.attr.colorOutlineVariant)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (8 * yg).toInt() }
        }
        kart.addView(TextView(this).apply {
            text = metin
            textSize = 13.5f
            setLineSpacing(0f, 1.35f)
            setPadding(
                (16 * yg).toInt(), (14 * yg).toInt(),
                (16 * yg).toInt(), (14 * yg).toInt()
            )
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
        })
        kok.addView(kart)
    }

    private fun renk(attr: Int): Int = runCatching {
        com.google.android.material.color.MaterialColors.getColor(kok, attr, 0)
    }.getOrDefault(0)
}
