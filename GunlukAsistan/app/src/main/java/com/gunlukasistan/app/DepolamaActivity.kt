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
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * v8.8 — Depolama ekranı (öneri 10) ve hata bildirimi (öneri 12).
 *
 * ── Öneri 10 ──
 * Kullanıcı uygulamanın kaç MB tuttuğunu bilmiyordu. Android'in
 * "Uygulama bilgisi" ekranı tek toplam gösteriyor; neyin yer
 * kapladığı belli değil. Telefonu dolan kullanıcının tek seçeneği
 * uygulamayı silmekti.
 *
 * Artık kategori bazlı: fotoğraflar, PDF'ler, AI önbelleği,
 * anlatımlar, yedekler. Her biri ayrı ayrı temizlenebiliyor.
 *
 * ── Öneri 12 ──
 * `App.kt` çökmeleri kaydediyordu ama kullanıcı onu gönderemiyordu.
 * Artık "Hatayı bildir" ile kayıt + cihaz bilgisi + sürüm panoya
 * kopyalanıyor veya paylaşılıyor.
 *
 * ── Ölçüm neden arka planda ──
 * `Depolama.olc()` diski geziyor; kanıt klasöründe yüzlerce dosya
 * olabilir. Ana iş parçacığında yapılsa ekran donardı.
 */
class DepolamaActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            context.startActivity(Intent(context, DepolamaActivity::class.java))
            (context as? android.app.Activity)?.let { Canlandir.activityGirisi(it) }
        }
    }

    private lateinit var kok: LinearLayout
    private val isci = java.util.concurrent.Executors.newSingleThreadExecutor()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(GorunumAyar.yaziOlcegiUygula(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.styleFor(this))
        ThemeManager.applyAccent(this)
        ThemeManager.dinamikRengiUygula(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gorunum_ayar)   // aynı iskelet yeniden kullanılıyor

        // v10.0 · Görsel öneri 4: ortak üst bar.
        // Düğme boyutu, yazı boyutu ve dokunma hedefi tek
        // yerden geliyor — ekranlar arası geçerken başlık
        // artık zıplamıyor.
        UstBar.kur(this, getString(R.string.dp_row))
        kok = findViewById(R.id.gaKok)

        yukle()
    }

    override fun finish() {
        super.finish()
        Canlandir.activityCikisi(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        isci.shutdownNow()
    }

    // ══════════════════════════════════════════════════════════

    private fun yukle() {
        kok.removeAllViews()
        // Ölçüm sürerken iskelet göster (v8.6 · öneri 25)
        val iskelet = Iskelet(this).apply {
            sekil = Iskelet.SEKIL_LISTE
            satirSayisi = 5
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (resources.displayMetrics.density * 340).toInt()
            )
        }
        kok.addView(iskelet)

        // v8.9 · Öneri 16: coroutine. Activity kapanınca iş otomatik
        // iptal oluyor; `isFinishing` kontrolü ArkaPlan içinde.
        ArkaPlan.calis(
            this,
            is_ = { runCatching { Depolama.olc(this) }.getOrDefault(emptyList()) }
        ) { kalemler ->
            kok.removeAllViews()
            ciz(kalemler)
        }
    }

    private fun ciz(kalemler: List<Depolama.Kalem>) {
        val yg = resources.displayMetrics.density
        fun dp(v: Int) = (v * yg).toInt()

        // ---- Toplam kartı ----
        val toplam = Depolama.toplam(kalemler)
        val temizlenebilir = Depolama.temizlenebilirToplam(kalemler)

        ozetKarti(toplam, temizlenebilir)

        // ---- Kategoriler ----
        baslik(getString(R.string.dp_kategoriler))
        kalemler.filter { it.bayt > 0 }.forEach { kalemSatiri(it) }

        if (kalemler.none { it.bayt > 0 }) {
            kok.addView(TextView(this).apply {
                setText(R.string.dp_bos)
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(dp(20), dp(30), dp(20), dp(30))
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            })
        }

        // ---- Bakım ----
        baslik(getString(R.string.dp_bakim))
        eylemKarti(
            "🧹", getString(R.string.dp_yetim), getString(R.string.dp_yetim_alt)
        ) { yetimleriTopla() }

        // ---- Güvenlik durumu (öneri 1 görünürlüğü) ----
        baslik(getString(R.string.dp_guvenlik))
        guvenlikKarti()

        // ---- Hata bildirimi (öneri 12) ----
        if (cokmeKaydiVar()) {
            baslik(getString(R.string.dp_hata))
            eylemKarti(
                "🐞", getString(R.string.dp_hata_bildir), getString(R.string.dp_hata_alt)
            ) { hataBildir() }
        }

        kok.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(40)
            )
        })
    }

    // ══════════════════════════════════════════════════════════
    // Yapı taşları
    // ══════════════════════════════════════════════════════════

    private fun ozetKarti(toplam: Long, temizlenebilir: Long) {
        val yg = resources.displayMetrics.density
        fun dp(v: Int) = (v * yg).toInt()

        val kart = MaterialCardView(this).apply {
            radius = 20 * yg
            cardElevation = 0f
            setCardBackgroundColor(renk(com.google.android.material.R.attr.colorPrimaryContainer))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12) }
        }
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }
        val sayi = TextView(this).apply {
            textSize = 30f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
            text = Depolama.bicimle(toplam)
        }
        ic.addView(sayi)
        ic.addView(TextView(this).apply {
            setText(R.string.dp_toplam)
            textSize = 13f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
        })
        if (temizlenebilir > 0) {
            ic.addView(TextView(this).apply {
                text = getString(R.string.dp_temizlenebilir, Depolama.bicimle(temizlenebilir))
                textSize = 12.5f
                setPadding(0, dp(8), 0, 0)
                setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
            })
        }
        kart.addView(ic)
        kok.addView(kart)
    }

    private fun kalemSatiri(kalem: Depolama.Kalem) {
        val yg = resources.displayMetrics.density
        fun dp(v: Int) = (v * yg).toInt()

        val kart = MaterialCardView(this).apply {
            radius = 16 * yg
            cardElevation = 0f
            strokeWidth = dp(1)
            strokeColor = renk(com.google.android.material.R.attr.colorOutlineVariant)
            setCardBackgroundColor(renk(com.google.android.material.R.attr.colorSurface))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        }
        val satir = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(12), dp(14))
        }
        satir.addView(TextView(this).apply {
            text = kalem.simge
            textSize = 20f
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(36))
            gravity = Gravity.CENTER
        })
        val metinler = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ).apply { marginStart = dp(8) }
        }
        metinler.addView(TextView(this).apply {
            setText(kalem.baslikRes)
            textSize = 15f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
        })
        metinler.addView(TextView(this).apply {
            text = buildString {
                append(Depolama.bicimle(kalem.bayt))
                if (kalem.dosyaSayisi > 0) {
                    append(" · ")
                    append(getString(R.string.dp_dosya, kalem.dosyaSayisi))
                }
            }
            textSize = 12f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
        })
        satir.addView(metinler)

        if (kalem.temizlenebilir) {
            satir.addView(MaterialButton(
                this, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply {
                setText(R.string.dp_temizle)
                textSize = 12f
                setOnClickListener { temizleSor(kalem) }
            })
        } else {
            satir.addView(TextView(this).apply {
                setText(R.string.dp_korunuyor)
                textSize = 11f
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            })
        }
        kart.addView(satir)
        kok.addView(kart)
    }

    private fun temizleSor(kalem: Depolama.Kalem) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(kalem.baslikRes))
            .setMessage(
                getString(R.string.dp_temizle_soru, Depolama.bicimle(kalem.bayt)) +
                    "\n\n" + getString(kalem.aciklamaRes)
            )
            .setPositiveButton(R.string.dp_temizle) { _, _ ->
                isci.execute {
                    val silinen = Depolama.temizle(this, kalem.kod)
                    runOnUiThread {
                        if (isFinishing) return@runOnUiThread
                        Titresim.basari(this)
                        Bildir.basari(
                            kok, getString(R.string.dp_temizlendi, Depolama.bicimle(silinen))
                        )
                        yukle()
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun yetimleriTopla() {
        isci.execute {
            val (sayi, bayt) = Depolama.yetimleriTopla(this)
            runOnUiThread {
                if (isFinishing) return@runOnUiThread
                if (sayi > 0) {
                    Titresim.basari(this)
                    Bildir.basari(
                        kok,
                        getString(R.string.dp_yetim_sonuc, sayi, Depolama.bicimle(bayt))
                    )
                    yukle()
                } else {
                    Bildir.bilgi(kok, getString(R.string.dp_yetim_yok))
                }
            }
        }
    }

    private fun guvenlikKarti() {
        val yg = resources.displayMetrics.density
        val durum = when {
            !AnahtarKasa.kullanilabilirMi() -> getString(R.string.ak_yok)
            AnahtarKasa.donanimKorumaliMi(this) -> getString(R.string.ak_donanim)
            else -> getString(R.string.ak_yazilim)
        }
        val kart = MaterialCardView(this).apply {
            radius = 16 * yg
            cardElevation = 0f
            setCardBackgroundColor(renk(com.google.android.material.R.attr.colorSecondaryContainer))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (8 * yg).toInt() }
        }
        kart.addView(TextView(this).apply {
            text = durum
            textSize = 13f
            setLineSpacing(0f, 1.25f)
            setPadding((16 * yg).toInt(), (14 * yg).toInt(), (16 * yg).toInt(), (14 * yg).toInt())
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
        })
        kok.addView(kart)
    }

    private fun eylemKarti(simge: String, baslik: String, alt: String, tiklandi: () -> Unit) {
        val yg = resources.displayMetrics.density
        fun dp(v: Int) = (v * yg).toInt()
        val kart = MaterialCardView(this).apply {
            radius = 16 * yg
            cardElevation = 0f
            strokeWidth = dp(1)
            strokeColor = renk(com.google.android.material.R.attr.colorOutlineVariant)
            setCardBackgroundColor(renk(com.google.android.material.R.attr.colorSurface))
            isClickable = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
            setOnClickListener {
                Titresim.dokunus(it)
                tiklandi()
            }
        }
        val satir = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        satir.addView(TextView(this).apply {
            text = simge
            textSize = 20f
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(36))
            gravity = Gravity.CENTER
        })
        val m = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ).apply { marginStart = dp(8) }
        }
        m.addView(TextView(this).apply {
            text = baslik
            textSize = 15f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
        })
        m.addView(TextView(this).apply {
            text = alt
            textSize = 12f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
        })
        satir.addView(m)
        satir.addView(TextView(this).apply {
            text = "›"
            textSize = 20f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
        })
        kart.addView(satir)
        kok.addView(kart)
    }

    private fun baslik(metin: String) {
        val yg = resources.displayMetrics.density
        kok.addView(TextView(this).apply {
            text = metin
            textSize = 12.5f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
            setPadding((4 * yg).toInt(), (20 * yg).toInt(), 0, (4 * yg).toInt())
        })
    }

    // ══════════════════════════════════════════════════════════
    // Öneri 12 — hata bildirimi
    // ══════════════════════════════════════════════════════════

    private fun cokmeKaydiVar(): Boolean = runCatching {
        getSharedPreferences("crash_log", MODE_PRIVATE)
            .getString("last_crash", "")?.isNotBlank() == true
    }.getOrDefault(false)

    /**
     * Çökme kaydını cihaz bilgisiyle birlikte paylaşır.
     *
     * ── Neden cihaz bilgisi şart ──
     * "Uygulama çöküyor" bildirimi tek başına işe yaramaz. Üretici,
     * Android sürümü ve uygulama sürümü olmadan sorunu tekrar
     * üretmek imkânsız. Samsung bildirim sorunu (v7.92) tam da bu
     * yüzden 6 sürüm sürmüştü.
     */
    private fun hataBildir() {
        val p = getSharedPreferences("crash_log", MODE_PRIVATE)
        val kayit = p.getString("last_crash", "") ?: ""
        if (kayit.isBlank()) return

        val zaman = p.getLong("last_crash_ts", 0L)
        val parca = p.getString("last_crash_thread", "?") ?: "?"

        val rapor = buildString {
            appendLine("=== Günlük Asistan hata raporu ===")
            appendLine("Uygulama : ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Android  : ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
            appendLine("Cihaz    : ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            appendLine("İş parçacığı: $parca")
            if (zaman > 0) {
                appendLine(
                    "Zaman    : " + java.text.SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss", java.util.Locale.US
                    ).format(java.util.Date(zaman))
                )
            }
            appendLine()
            appendLine(kayit.take(6000))   // paylaşım sınırını aşmasın
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dp_hata_bildir)
            .setMessage(rapor.take(600) + "\n…")
            .setPositiveButton(R.string.dp_paylas) { _, _ ->
                runCatching {
                    val niyet = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Günlük Asistan hata raporu")
                        putExtra(Intent.EXTRA_TEXT, rapor)
                    }
                    startActivity(Intent.createChooser(niyet, getString(R.string.dp_paylas)))
                }
            }
            .setNeutralButton(R.string.dp_kopyala) { _, _ ->
                runCatching {
                    val pano = getSystemService(Context.CLIPBOARD_SERVICE)
                        as android.content.ClipboardManager
                    pano.setPrimaryClip(
                        android.content.ClipData.newPlainText("hata", rapor)
                    )
                    Bildir.basari(kok, getString(R.string.dp_kopyalandi))
                }
            }
            .setNegativeButton(R.string.dp_sil) { _, _ ->
                p.edit().clear().apply()
                yukle()
            }
            .show()
    }

    private fun renk(attr: Int): Int = runCatching {
        com.google.android.material.color.MaterialColors.getColor(kok, attr, 0)
    }.getOrDefault(0)
}
