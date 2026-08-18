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
import com.google.android.material.materialswitch.MaterialSwitch

/**
 * v9.4 — Takvim ayarları (öneri 9, 10, 11) ve süre analizi (13, 14, 15).
 *
 * ── İzin akışı ──
 * Takvim izni tehlikeli izin; çalışma anında isteniyor. Kullanıcı
 * reddederse ekran "izin gerekiyor" kartı gösteriyor ve uygulama
 * ayarlarına yönlendiriyor. Hiçbir yerde çökmüyor.
 *
 * ── Neden tek ekranda hem takvim hem süre analizi ──
 * İkisi de "zaman yönetimi" başlığı altında ve birbirini besliyor:
 * boş zaman bulucu takvimi kullanıyor, bütçe hesabı boş zamanı
 * kullanıyor. Ayrı ekranlara bölmek kullanıcıyı gezdirirdi.
 */
class TakvimAyarActivity : AppCompatActivity() {

    companion object {
        private const val IZIN_KODU = 4401

        fun ac(context: Context) {
            context.startActivity(Intent(context, TakvimAyarActivity::class.java))
            (context as? android.app.Activity)?.let { Canlandir.activityGirisi(it) }
        }
    }

    private lateinit var kok: LinearLayout

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
        UstBar.kur(this, getString(R.string.tkv_baslik))
        kok = findViewById(R.id.gaKok)
        ciz()
    }

    override fun finish() {
        super.finish()
        Canlandir.activityCikisi(this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != IZIN_KODU) return
        val verildi = grantResults.isNotEmpty() &&
            grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }
        if (verildi) {
            TakvimKopru.ac(this, true)
            Bildir.basari(kok, getString(R.string.tk_izin_ver))
        } else {
            Bildir.hata(kok, getString(R.string.tk_izin_red))
        }
        ciz()
    }

    // ══════════════════════════════════════════════════════════

    private fun ciz() {
        kok.removeAllViews()
        val yg = resources.displayMetrics.density
        fun dp(v: Int) = (v * yg).toInt()

        // ---- İzin yoksa önce onu iste ----
        if (!TakvimKopru.izinVar(this)) {
            izinKarti()
        } else {
            takvimBolumu()
        }

        // ---- Süre analizi (izinden bağımsız çalışıyor) ----
        sureBolumu()

        kok.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(40)
            )
        })
    }

    private fun izinKarti() {
        val yg = resources.displayMetrics.density
        fun dp(v: Int) = (v * yg).toInt()

        val kart = MaterialCardView(this).apply {
            radius = 18 * yg
            cardElevation = 0f
            setCardBackgroundColor(renk(com.google.android.material.R.attr.colorSecondaryContainer))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(14) }
        }
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }
        ic.addView(TextView(this).apply {
            setText(R.string.tk_izin_gerek)
            textSize = 16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
        })
        ic.addView(TextView(this).apply {
            setText(R.string.tk_row_sub)
            textSize = 13f
            setLineSpacing(0f, 1.3f)
            setPadding(0, dp(6), 0, dp(14))
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
        })
        ic.addView(MaterialButton(this).apply {
            setText(R.string.tk_izin_ver)
            setOnClickListener {
                Titresim.dokunus(it)
                androidx.core.app.ActivityCompat.requestPermissions(
                    this@TakvimAyarActivity, TakvimKopru.IZINLER, IZIN_KODU
                )
            }
        })
        kart.addView(ic)
        kok.addView(kart)
    }

    private fun takvimBolumu() {
        val yg = resources.displayMetrics.density
        fun dp(v: Int) = (v * yg).toInt()

        baslik(getString(R.string.tkv_baslik))

        anahtar(
            getString(R.string.tk_row), getString(R.string.tk_row_sub),
            TakvimKopru.acikMi(this)
        ) { acik ->
            TakvimKopru.ac(this, acik)
            if (acik) esitle()
            ciz()
        }

        if (!TakvimKopru.acikMi(this)) return

        // ---- Takvim seçimi ----
        val takvimler = TakvimKopru.takvimler(this)
        if (takvimler.isEmpty()) {
            kok.addView(TextView(this).apply {
                setText(R.string.tk_takvim_yok)
                textSize = 13f
                setPadding(dp(16), dp(10), dp(16), dp(10))
                setTextColor(0xFFD9534F.toInt())
            })
            return
        }

        baslik(getString(R.string.tk_takvim_sec))
        val secili = TakvimKopru.seciliTakvim(this)
        takvimler.forEach { t ->
            val kart = MaterialCardView(this).apply {
                radius = 14 * yg
                cardElevation = 0f
                strokeWidth = if (t.id == secili) dp(2) else dp(1)
                strokeColor = if (t.id == secili) {
                    renk(com.google.android.material.R.attr.colorPrimary)
                } else {
                    renk(com.google.android.material.R.attr.colorOutlineVariant)
                }
                setCardBackgroundColor(renk(com.google.android.material.R.attr.colorSurface))
                isClickable = true
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(6) }
                setOnClickListener {
                    Titresim.dokunus(it)
                    TakvimKopru.takvimSec(this@TakvimAyarActivity, t.id)
                    ciz()
                }
            }
            val satir = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
            }
            satir.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(14), dp(14))
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(if (t.renk != 0) t.renk else 0xFF888888.toInt())
                }
            })
            val m = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                ).apply { marginStart = dp(12) }
            }
            m.addView(TextView(this).apply {
                text = t.ad
                textSize = 14.5f
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
            })
            if (t.hesap.isNotBlank() && t.hesap != t.ad) {
                m.addView(TextView(this).apply {
                    text = t.hesap
                    textSize = 11.5f
                    setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
                })
            }
            satir.addView(m)
            if (t.id == secili) {
                satir.addView(TextView(this).apply {
                    text = "✓"
                    textSize = 17f
                    setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
                })
            }
            kart.addView(satir)
            kok.addView(kart)
        }

        // ---- Ne yazılsın ----
        baslik(getString(R.string.tk_esitle))
        anahtar(getString(R.string.tk_yaz_sinav), "", TakvimKopru.sinavYaz(this)) {
            TakvimKopru.sinavYaz(this, it)
        }
        anahtar(getString(R.string.tk_yaz_etkinlik), "", TakvimKopru.etkinlikYaz(this)) {
            TakvimKopru.etkinlikYaz(this, it)
        }
        anahtar(
            getString(R.string.tk_oku), getString(R.string.tk_oku_alt),
            TakvimKopru.okumaAcik(this)
        ) { TakvimKopru.okumaAcik(this, it) }

        kok.addView(MaterialButton(this).apply {
            setText(R.string.tk_esitle)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12) }
            setOnClickListener { Titresim.dokunus(it); esitle() }
        })
    }

    private fun esitle() {
        ArkaPlan.calis(
            this,
            is_ = { runCatching { TakvimKopru.tumunuEsitle(this) }.getOrDefault(0) }
        ) { sayi ->
            Bildir.basari(kok, getString(R.string.tk_esitlendi, sayi))
        }
    }

    // ══════════════════════════════════════════════════════════
    // Süre analizi (öneri 13, 14, 15)
    // ══════════════════════════════════════════════════════════

    private fun sureBolumu() {
        val yg = resources.displayMetrics.density
        fun dp(v: Int) = (v * yg).toInt()

        baslik(getString(R.string.sua_baslik))

        // ---- Süre tahmini kalibrasyonu ----
        val ozet = SureAnalizi.ozetMetni(this)
        bilgiKarti(
            if (ozet != null) ozet else getString(R.string.sa_veri_yok),
            alt = getString(R.string.sa_ornek, SureAnalizi.ornekSayisi(this)),
            vurgulu = ozet != null
        )

        // v10.4 · A12 bağları: değerlendirme puanları artık görünüyor.
        // v10.2'de zil ekranına konulan 🙁😐😄 şeridi kayıtlara
        // `kalite` yazıyordu ama ortalama HİÇBİR yerde gösterilmiyordu —
        // puanlar kaydediliyor, geri okunmuyordu.
        val kaliteOrt = try { SureAnalizi.kaliteOrtalamasi(this) } catch (_: Exception) { 0f }
        if (kaliteOrt > 0f) {
            bilgiKarti(
                getString(R.string.sa_kalite_ozet, kaliteOrt),
                alt = getString(R.string.sa_kalite_alt),
                vurgulu = kaliteOrt >= 2.2f
            )
        }

        // ---- Pomodoro ----
        val po = SureAnalizi.pomodoroOzeti(this)
        if (po.toplam > 0) {
            val metin = buildString {
                append(getString(R.string.sa_pom_ozet, po.toplam, po.basariOrani))
                if (po.bugunToplam > 0) {
                    append("\n")
                    append(getString(R.string.sa_pom_bugun, po.bugunToplam))
                }
                if (po.enIyiSaat >= 0) {
                    append("\n")
                    append(getString(R.string.sa_pom_en_iyi, po.enIyiSaat))
                }
            }
            bilgiKarti(metin, alt = getString(R.string.sa_pom_baslik), vurgulu = false)
        } else {
            bilgiKarti(getString(R.string.sa_pom_yok), alt = null, vurgulu = false)
        }

        // ---- Günün bütçesi ----
        val b = SureAnalizi.butce(this)
        if (b.hedefDk > 0) {
            val metin = buildString {
                append(getString(R.string.sa_butce, b.hedefDk, b.yapilanDk, b.kalanDk))
                if (TakvimKopru.acikMi(this@TakvimAyarActivity) && b.kalanDk > 0) {
                    append("\n\n")
                    append(
                        if (b.yetisirMi) getString(R.string.sa_butce_yetisir, b.bosDk)
                        else getString(R.string.sa_butce_yetmez, b.bosDk)
                    )
                }
            }
            bilgiKarti(metin, alt = getString(R.string.sa_butce_baslik), vurgulu = !b.yetisirMi)
        }
    }

    // ══════════════════════════════════════════════════════════
    // Yapı taşları
    // ══════════════════════════════════════════════════════════

    private fun baslik(metin: String) {
        val yg = resources.displayMetrics.density
        kok.addView(TextView(this).apply {
            text = metin
            textSize = 12.5f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
            setPadding((4 * yg).toInt(), (20 * yg).toInt(), 0, (6 * yg).toInt())
        })
    }

    private fun anahtar(
        baslik: String, alt: String, deger: Boolean, degisti: (Boolean) -> Unit
    ) {
        val yg = resources.displayMetrics.density
        fun dp(v: Int) = (v * yg).toInt()
        val satir = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        val m = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        }
        m.addView(TextView(this).apply {
            text = baslik
            textSize = 14.5f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
        })
        if (alt.isNotBlank()) {
            m.addView(TextView(this).apply {
                text = alt
                textSize = 11.5f
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            })
        }
        satir.addView(m)
        satir.addView(MaterialSwitch(this).apply {
            isChecked = deger
            setOnCheckedChangeListener { d, secili ->
                if (!d.isPressed) return@setOnCheckedChangeListener
                Titresim.dokunus(d)
                degisti(secili)
            }
        })
        kok.addView(satir)
    }

    private fun bilgiKarti(metin: String, alt: String?, vurgulu: Boolean) {
        val yg = resources.displayMetrics.density
        fun dp(v: Int) = (v * yg).toInt()
        val kart = MaterialCardView(this).apply {
            radius = 16 * yg
            cardElevation = 0f
            setCardBackgroundColor(
                if (vurgulu) renk(com.google.android.material.R.attr.colorPrimaryContainer)
                else renk(com.google.android.material.R.attr.colorSurface)
            )
            strokeWidth = if (vurgulu) 0 else dp(1)
            strokeColor = renk(com.google.android.material.R.attr.colorOutlineVariant)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
        }
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        if (!alt.isNullOrBlank()) {
            ic.addView(TextView(this).apply {
                text = alt
                textSize = 11.5f
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
                setPadding(0, 0, 0, dp(4))
            })
        }
        ic.addView(TextView(this).apply {
            text = metin
            textSize = 13.5f
            setLineSpacing(0f, 1.35f)
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
        })
        kart.addView(ic)
        kok.addView(kart)
    }

    private fun renk(attr: Int): Int = runCatching {
        com.google.android.material.color.MaterialColors.getColor(kok, attr, 0)
    }.getOrDefault(0)
}
