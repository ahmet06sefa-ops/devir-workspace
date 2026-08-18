package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

/**
 * v9.5 — Fotoğraftan soru çözme ekranı (öneri 25).
 *
 * ── Akış ──
 * Kamera/galeri → önizleme → "Çöz" veya "İpucu ver" → sonuç
 *
 * ── İki mod neden ──
 * "Çöz" doğrudan cevap veriyor; sınava hazırlanan biri için bu
 * bazen zararlı (kopyalayıp geçer). "İpucu" yalnız sorunun ne
 * istediğini ve hangi bilginin gerektiğini söylüyor — önce kendin
 * dene, takılırsan çöz.
 *
 * ── Neden ayrı Activity, dialog değil ──
 * Çözüm metni uzun olabiliyor (adım adım anlatım). Dialog'da
 * kaydırma kötü çalışıyor ve paylaşma/kaydetme düğmeleri sığmıyor.
 */
class SoruCozActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            context.startActivity(Intent(context, SoruCozActivity::class.java))
            (context as? android.app.Activity)?.let { Canlandir.activityGirisi(it) }
        }
    }

    private lateinit var kok: LinearLayout
    private var seciliUri: Uri? = null
    private var kameraUri: Uri? = null
    private var sonCozum: SoruCoz.Cozum? = null
    private var yukleniyor = false

    // ---- Seçiciler ----

    private val galeriSec = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { seciliUri = it; sonCozum = null; ciz() } }

    private val kameraCek = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        if (ok) { seciliUri = kameraUri; sonCozum = null; ciz() }
    }

    private val kameraIzni = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { verildi ->
        if (verildi) kamerayiAc()
        else Bildir.hata(kok, getString(R.string.sc_kamera_izni_yok))
    }

    // ══════════════════════════════════════════════════════════

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
        UstBar.kur(this, getString(R.string.sc_baslik))
        kok = findViewById(R.id.gaKok)
        ciz()
    }

    override fun finish() {
        super.finish()
        Canlandir.activityCikisi(this)
    }

    // ══════════════════════════════════════════════════════════

    private fun ciz() {
        kok.removeAllViews()
        val yg = resources.displayMetrics.density
        fun dp(v: Int) = (v * yg).toInt()

        // ---- AI kapalıysa uyar ----
        if (!AiSettings.isReady(this)) {
            bilgiKarti(getString(R.string.sc_ai_kapali), vurgulu = true)
            kok.addView(MaterialButton(this).apply {
                setText(R.string.sc_ai_ayarla)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(10) }
                setOnClickListener { (this@SoruCozActivity).aiAyarlariniAc() }
            })
            return
        }

        // ---- Fotoğraf yoksa: seçim ----
        val uri = seciliUri
        if (uri == null) {
            bosDurum()
            return
        }

        // ---- Önizleme ----
        val onizlemeKart = MaterialCardView(this).apply {
            radius = 18 * yg
            cardElevation = 0f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(14) }
        }
        onizlemeKart.addView(ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(240)
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = getString(R.string.sc_onizleme)
            runCatching {
                GorselHazirla.onizleme(this@SoruCozActivity, uri)?.let { setImageBitmap(it) }
            }
        })
        kok.addView(onizlemeKart)

        // ---- Yükleniyor ----
        if (yukleniyor) {
            kok.addView(TextView(this).apply {
                setText(R.string.sc_cozuluyor)
                textSize = 13.5f
                gravity = Gravity.CENTER
                setPadding(0, dp(18), 0, dp(8))
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            })
            kok.addView(Iskelet(this).apply {
                sekil = Iskelet.SEKIL_METIN
                satirSayisi = 3
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(130)
                )
            })
            return
        }

        // ---- Sonuç varsa göster ----
        val c = sonCozum
        if (c != null) {
            sonucGoster(c)
        } else {
            // ---- Çöz / İpucu düğmeleri ----
            kok.addView(MaterialButton(this).apply {
                setText(R.string.sc_coz)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(14) }
                setOnClickListener { Titresim.dokunus(it); coz(ipucu = false) }
            })
            kok.addView(MaterialButton(
                this, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply {
                setText(R.string.sc_ipucu)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(8) }
                setOnClickListener { Titresim.dokunus(it); coz(ipucu = true) }
            })
            kok.addView(TextView(this).apply {
                setText(R.string.sc_ipucu_alt)
                textSize = 11.5f
                gravity = Gravity.CENTER
                setLineSpacing(0f, 1.3f)
                setPadding(dp(12), dp(8), dp(12), 0)
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            })
        }

        // ---- Başka fotoğraf ----
        kok.addView(MaterialButton(
            this, null, com.google.android.material.R.attr.borderlessButtonStyle
        ).apply {
            setText(R.string.sc_baska_foto)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10) }
            setOnClickListener { seciliUri = null; sonCozum = null; ciz() }
        })

        kok.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(30)
            )
        })
    }

    private fun bosDurum() {
        val yg = resources.displayMetrics.density
        fun dp(v: Int) = (v * yg).toInt()

        kok.addView(TextView(this).apply {
            setText(R.string.sc_aciklama)
            textSize = 14f
            gravity = Gravity.CENTER
            setLineSpacing(0f, 1.4f)
            setPadding(dp(16), dp(30), dp(16), dp(24))
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
        })

        kok.addView(MaterialButton(this).apply {
            setText(R.string.sc_kamera)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                Titresim.dokunus(it)
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                        this@SoruCozActivity, android.Manifest.permission.CAMERA
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) kamerayiAc() else kameraIzni.launch(android.Manifest.permission.CAMERA)
            }
        })
        kok.addView(MaterialButton(
            this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            setText(R.string.sc_galeri)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
            setOnClickListener {
                Titresim.dokunus(it)
                runCatching { galeriSec.launch("image/*") }
            }
        })

        // ---- Geçmiş ----
        val gecmis = SoruCoz.gecmis(this)
        if (gecmis.isNotEmpty()) {
            baslik(getString(R.string.sc_gecmis, gecmis.size))
            gecmis.take(8).forEach { c -> gecmisSatiri(c) }
        }
    }

    private fun gecmisSatiri(c: SoruCoz.Cozum) {
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
            ).apply { bottomMargin = dp(6) }
            setOnClickListener {
                Titresim.dokunus(it)
                sonCozum = c
                seciliUri = null
                kok.removeAllViews()
                sonucGoster(c)
                kok.addView(MaterialButton(
                    this@SoruCozActivity, null,
                    com.google.android.material.R.attr.borderlessButtonStyle
                ).apply {
                    setText(R.string.done)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setOnClickListener { sonCozum = null; ciz() }
                })
            }
        }
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
        }
        if (c.konu.isNotBlank()) {
            ic.addView(TextView(this).apply {
                text = c.konu
                textSize = 11f
                setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
            })
        }
        ic.addView(TextView(this).apply {
            text = c.soru.take(90) + if (c.soru.length > 90) "…" else ""
            textSize = 13.5f
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
        })
        kart.addView(ic)
        kok.addView(kart)
    }

    // ══════════════════════════════════════════════════════════

    private fun coz(ipucu: Boolean) {
        val uri = seciliUri ?: return
        yukleniyor = true
        ciz()

        // v8.9 · ArkaPlan: Activity kapanınca iş otomatik iptal
        ArkaPlan.calisGuvenli(
            this,
            is_ = { SoruCoz.coz(this, uri, ipucuModu = ipucu) },
            hata = { e ->
                yukleniyor = false
                ciz()
                Bildir.hata(kok, e.message ?: getString(R.string.sc_ai_hata))
            }
        ) { sonuc ->
            yukleniyor = false
            if (sonuc.ok && sonuc.cozum != null) {
                sonCozum = sonuc.cozum
                // İpucu modunda kaydetme — yarım kayıt geçmişi kirletir
                if (!ipucu) SoruCoz.kaydet(this, sonuc.cozum)
                Titresim.dogru(this)
            } else {
                Bildir.hata(kok, sonuc.hata)
            }
            ciz()
        }
    }

    private fun sonucGoster(c: SoruCoz.Cozum) {
        val yg = resources.displayMetrics.density
        fun dp(v: Int) = (v * yg).toInt()

        if (c.konu.isNotBlank()) {
            kok.addView(TextView(this).apply {
                text = c.konu
                textSize = 12f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
                setPadding(dp(4), dp(16), 0, dp(4))
            })
        }

        if (c.soru.isNotBlank()) {
            bolumKarti(getString(R.string.sc_soru), c.soru, vurgulu = false)
        }
        if (c.ipucu.isNotBlank()) {
            bolumKarti(getString(R.string.sc_ne_isteniyor), c.ipucu, vurgulu = true)
        }
        if (c.cozum.isNotBlank()) {
            bolumKarti(getString(R.string.sc_cozum), c.cozum, vurgulu = false)
        }
        if (c.sonuc.isNotBlank()) {
            bolumKarti(getString(R.string.sc_sonuc), c.sonuc, vurgulu = true)
        }

        // ---- Eylemler ----
        kok.addView(MaterialButton(
            this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            setText(R.string.sc_hata_defteri)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12) }
            setOnClickListener {
                Titresim.dokunus(it)
                if (SoruCoz.hataDefterineEkle(this@SoruCozActivity, c)) {
                    Bildir.basari(kok, getString(R.string.sc_eklendi))
                } else {
                    Bildir.hata(kok, getString(R.string.sc_eklenemedi))
                }
            }
        })
        kok.addView(MaterialButton(
            this, null, com.google.android.material.R.attr.borderlessButtonStyle
        ).apply {
            setText(R.string.dp_paylas)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                runCatching {
                    val metin = buildString {
                        if (c.soru.isNotBlank()) appendLine(c.soru).appendLine()
                        if (c.cozum.isNotBlank()) appendLine(c.cozum)
                        if (c.sonuc.isNotBlank()) appendLine().append(c.sonuc)
                    }
                    startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, metin)
                            },
                            getString(R.string.dp_paylas)
                        )
                    )
                }
            }
        })
    }

    // ══════════════════════════════════════════════════════════

    private fun kamerayiAc() {
        runCatching {
            val dosya = java.io.File(
                java.io.File(filesDir, "soru").apply { mkdirs() },
                "s_${System.currentTimeMillis()}.jpg"
            )
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this, "$packageName.fileprovider", dosya
            )
            kameraUri = uri
            kameraCek.launch(uri)
        }.onFailure {
            android.util.Log.w("SoruCozActivity", "Kamera açılamadı", it)
            Bildir.hata(kok, getString(R.string.sc_kamera_acilmadi))
        }
    }

    private fun aiAyarlariniAc() {
        runCatching {
            startActivity(Intent(this, MainActivity::class.java).apply {
                putExtra("screen", 7)   // Ayarlar
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            })
        }
    }

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

    private fun bolumKarti(baslik: String, metin: String, vurgulu: Boolean) {
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
            ).apply { topMargin = dp(8) }
        }
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        ic.addView(TextView(this).apply {
            text = baslik
            textSize = 11.5f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(0, 0, 0, dp(6))
        })
        ic.addView(TextView(this).apply {
            text = metin
            textSize = 14f
            setLineSpacing(0f, 1.45f)
            setTextIsSelectable(true)
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
        })
        kart.addView(ic)
        kok.addView(kart)
    }

    private fun bilgiKarti(metin: String, vurgulu: Boolean) {
        val yg = resources.displayMetrics.density
        val kart = MaterialCardView(this).apply {
            radius = 16 * yg
            cardElevation = 0f
            setCardBackgroundColor(
                if (vurgulu) renk(com.google.android.material.R.attr.colorSecondaryContainer)
                else renk(com.google.android.material.R.attr.colorSurface)
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (14 * yg).toInt() }
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
