package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.materialswitch.MaterialSwitch
import kotlin.math.ceil

/**
 * v10.12 · ULTRA-30 / D19 — Nefes stüdyosu ekranı.
 *
 * Üç desen (4-7-8, kutu, sakin), döngü sayısı seçimi (4 / 8 / süresiz),
 * faz değişiminde kısa titreşim. Halkanın büyüyüp küçülmesi kalbe değil
 * desene bağlıdır: matematik [NefesProgrami]'nde, çizim [NefesView]'de.
 *
 * "Uyku öncesi" varyant zincir şablonu olarak da durur
 * ([SayacZincir.sablonlar] içindeki -5 numaralı şablon): nefes evreleri
 * arka arkaya sayaç zinciri gibi koşar, eller serbest uyku hazırlığı
 * yapılır.
 *
 * ── Kare döngüsü ──
 * Animasyon açıksa ~30 fps, kapalıysa (erişilebilirlik) 250 ms'lik
 * adımlarla nefes yine izlenebilir. Ekran önden çıkınca otomatik
 * duraklar; dönüşte "Sürdür" tek dokunuş.
 */
class NefesActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            context.startActivity(Intent(context, NefesActivity::class.java))
        }

        private const val PREF = "fo_nefes_v1"
        private const val K_DESEN = "desen"
        private const val K_TUR = "tur"
        private const val K_TITRESIM = "titresim"
    }

    private val d get() = resources.displayMetrics.density

    private lateinit var halka: NefesView
    private lateinit var anaIslem: MaterialButton
    private lateinit var durumYazi: TextView
    private lateinit var titresimAnahtar: MaterialSwitch

    private val desenKartlari = mutableListOf<MaterialCardView>()
    private val turKartlari = mutableListOf<MaterialCardView>()

    private var desenId = NefesProgrami.DESEN_478
    private var turSecim = 8          // -1 = süresiz
    private var calisiyor = false
    private var birikenMs = 0L        // duraklamalarda korunan ilerleme
    private var baslangicDamgasi = 0L
    private var sonFaz = -1
    private var bittiMi = false

    private val handler = Handler(Looper.getMainLooper())
    private val kare = object : Runnable {
        override fun run() {
            adim()
            if (calisiyor) {
                handler.postDelayed(this, kareAraligi())
            }
        }
    }

    private fun kareAraligi(): Long =
        if (GorunumAyar.animasyonAcik(this)) 33L else 250L

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(GorunumAyar.yaziOlcegiUygula(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.styleFor(this))
        ThemeManager.applyAccent(this)
        ThemeManager.dinamikRengiUygula(this)
        super.onCreate(savedInstanceState)

        // Son seçimler hatırlanır
        getSharedPreferences(PREF, MODE_PRIVATE).let { p ->
            desenId = p.getInt(K_DESEN, NefesProgrami.DESEN_478)
            turSecim = p.getInt(K_TUR, 8)
        }

        buildEkran()
        tazele()
        renkleriUygula()
    }

    // ---------------- Arayüz ----------------

    private fun buildEkran() {
        val kap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((20 * d).toInt(), (20 * d).toInt(), (20 * d).toInt(), (28 * d).toInt())
        }

        kap.addView(TextView(this).apply {
            setText(R.string.fo_nefes_baslik)
            textSize = 22f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
        })
        kap.addView(TextView(this).apply {
            setText(R.string.fo_nefes_alt)
            textSize = 13f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(0, (4 * d).toInt(), 0, (16 * d).toInt())
        })

        // Desen kartları
        kap.addView(baslik(getString(R.string.fo_nefes_desen)))
        val desenSirasi = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        NefesProgrami.desenler().forEach { desen ->
            val kart = cip("${desen.emoji}\n${getString(desen.adRes)}") {
                desenId = desen.id
                kaydet()
                tazele()
            }
            desenKartlari.add(kart)
            desenSirasi.addView(
                kart,
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { marginStart = (4 * d).toInt(); marginEnd = (4 * d).toInt() }
            )
        }
        kap.addView(desenSirasi)

        // Döngü seçimi
        kap.addView(baslik(getString(R.string.fo_nefes_tur)))
        val turSirasi = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        listOf(
            4 to getString(R.string.fo_nefes_tur_4),
            8 to getString(R.string.fo_nefes_tur_8),
            -1 to getString(R.string.fo_nefes_tur_sinirsiz)
        ).forEach { (deger, etiket) ->
            val kart = cip(etiket) {
                turSecim = deger
                kaydet()
                tazele()
            }
            turKartlari.add(kart)
            turSirasi.addView(
                kart,
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { marginStart = (4 * d).toInt(); marginEnd = (4 * d).toInt() }
            )
        }
        kap.addView(turSirasi)

        // Halka
        halka = NefesView(this)
        kap.addView(
            halka,
            LinearLayout.LayoutParams((250 * d).toInt(), (250 * d).toInt()).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = (18 * d).toInt()
            }
        )

        durumYazi = TextView(this).apply {
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(0, (10 * d).toInt(), 0, (6 * d).toInt())
        }
        kap.addView(durumYazi)

        // Kontroller
        val kontroller = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        anaIslem = MaterialButton(this).apply {
            setText(R.string.fo_nefes_basla)
            setOnClickListener { anaIslemTik() }
        }
        kontroller.addView(anaIslem)
        val bitirDugme = MaterialButton(
            this, null, com.google.android.material.R.attr.borderlessButtonStyle
        ).apply {
            setText(R.string.fo_nefes_sifirla)
            setOnClickListener { sifirla() }
        }
        kontroller.addView(
            bitirDugme,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = (10 * d).toInt() }
        )
        kap.addView(kontroller)

        titresimAnahtar = MaterialSwitch(this).apply {
            setText(R.string.fo_nefes_titresim)
            isChecked = getSharedPreferences(PREF, MODE_PRIVATE).getBoolean(K_TITRESIM, true)
            setOnCheckedChangeListener { _, b ->
                getSharedPreferences(PREF, MODE_PRIVATE).edit().putBoolean(K_TITRESIM, b).apply()
            }
        }
        kap.addView(
            titresimAnahtar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER_HORIZONTAL; topMargin = (8 * d).toInt() }
        )

        setContentView(
            ScrollView(this).apply {
                setBackgroundColor(renk(com.google.android.material.R.attr.colorSurface))
                addView(kap)
            }
        )
    }

    private fun baslik(metin: String): TextView = TextView(this).apply {
        text = metin
        textSize = 12f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
        setPadding(0, (14 * d).toInt(), 0, (8 * d).toInt())
    }

    /** Seçilebilir küçük kart — tema ekranındaki paket çipi kalıbı. */
    private fun cip(metin: String, tikla: () -> Unit): MaterialCardView =
        MaterialCardView(this).apply {
            radius = 16 * d
            cardElevation = 0f
            isClickable = true
            isFocusable = true
            val yazi = TextView(this@NefesActivity).apply {
                text = metin
                textSize = 12f
                gravity = Gravity.CENTER
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
                setPadding((8 * d).toInt(), (12 * d).toInt(), (8 * d).toInt(), (12 * d).toInt())
            }
            addView(yazi)
            setOnClickListener { tikla() }
        }

    private fun renk(attr: Int): Int = MaterialColors.getColor(this, attr, 0)

    private fun renkleriUygula() {
        val vurgu = renk(com.google.android.material.R.attr.colorPrimary)
        val zemin = MaterialColors.getColor(
            this,
            com.google.android.material.R.attr.colorSurfaceContainerHigh,
            renk(com.google.android.material.R.attr.colorSecondaryContainer)
        )
        halka.renkleriAyarla(
            vurgu = vurgu,
            zemin = zemin,
            metin = renk(com.google.android.material.R.attr.colorOnSurface),
            ikincil = renk(com.google.android.material.R.attr.colorOnSurfaceVariant),
            soluk = MaterialColors.getColor(
                this,
                com.google.android.material.R.attr.colorOutlineVariant,
                0x33888888
            )
        )
    }

    /** Çiplerin seçim görünümü + durdurulmuşsa halka sıfır durumu. */
    private fun tazele() {
        val vurguKap = renk(com.google.android.material.R.attr.colorPrimaryContainer)
        val vurgu = renk(com.google.android.material.R.attr.colorPrimary)
        val notral = renk(com.google.android.material.R.attr.colorSecondaryContainer)

        desenKartlari.forEachIndexed { i, kart ->
            val secili = NefesProgrami.desenler()[i].id == desenId
            kart.setCardBackgroundColor(if (secili) vurguKap else notral)
            kart.strokeWidth = if (secili) (2 * d).toInt() else 0
            kart.strokeColor = vurgu
        }
        val turDegerleri = listOf(4, 8, -1)
        turKartlari.forEachIndexed { i, kart ->
            val secili = turDegerleri[i] == turSecim
            kart.setCardBackgroundColor(if (secili) vurguKap else notral)
            kart.strokeWidth = if (secili) (2 * d).toInt() else 0
            kart.strokeColor = vurgu
        }
        if (!calisiyor) halkayiGoster(birikenMs.toDouble() / 1000.0)
    }

    // ---------------- Motor ----------------

    private fun anaIslemTik() {
        if (calisiyor) duraklat() else baslat()
    }

    private fun baslat() {
        if (bittiMi) {
            birikenMs = 0L
            bittiMi = false
        }
        calisiyor = true
        baslangicDamgasi = SystemClock.elapsedRealtime()
        anaIslem.setText(R.string.fo_nefes_dur)
        sonFaz = -1
        handler.removeCallbacks(kare)
        handler.post(kare)
    }

    private fun duraklat() {
        calisiyor = false
        handler.removeCallbacks(kare)
        anaIslem.setText(R.string.fo_nefes_devam)
    }

    private fun sifirla() {
        calisiyor = false
        bittiMi = false
        handler.removeCallbacks(kare)
        birikenMs = 0L
        anaIslem.setText(R.string.fo_nefes_basla)
        durumYazi.text = ""
        tazele()
    }

    private fun kaydet() {
        getSharedPreferences(PREF, MODE_PRIVATE).edit()
            .putInt(K_DESEN, desenId).putInt(K_TUR, turSecim).apply()
    }

    private fun toplamGecenMs(): Long =
        birikenMs + if (calisiyor) SystemClock.elapsedRealtime() - baslangicDamgasi else 0L

    private fun adim() {
        val gecenMs = toplamGecenMs()
        val desen = NefesProgrami.desen(desenId)

        // Döngü sınırı: tur seçimli tur bitince kutla ve dur
        if (turSecim > 0 && gecenMs >= desen.donguSn * 1000L * turSecim) {
            calisiyor = false
            bittiMi = true
            handler.removeCallbacks(kare)
            birikenMs = 0L
            anaIslem.setText(R.string.fo_nefes_basla)
            durumYazi.setText(R.string.fo_nefes_tamam)
            fazTitret(uzun = true)
            tazele()
            Toast.makeText(this, R.string.fo_nefes_tamam, Toast.LENGTH_LONG).show()
            return
        }
        halkayiGoster(gecenMs.toDouble() / 1000.0)
    }

    private fun halkayiGoster(gecenSn: Double) {
        val desen = NefesProgrami.desen(desenId)
        val dongu = desen.donguSn.toDouble().coerceAtLeast(1.0)
        val donguIci = gecenSn % dongu
        val (fazIndex, oran) = NefesProgrami.fazBul(desen, donguIci)
        val faz = desen.fazlar[fazIndex]

        // Faz değişimi: titreşim + iz sürme
        if (calisiyor && fazIndex != sonFaz) {
            if (sonFaz >= 0) fazTitret(uzun = false)
            sonFaz = fazIndex
        }

        val turNo = (gecenSn / dongu).toInt() + 1
        val ust = if (turSecim > 0) {
            getString(R.string.fo_nefes_kalan, (turSecim - turNo + 1).coerceAtLeast(0))
        } else {
            "$turNo. tur"
        }
        val kalan = ceil(faz.sn * (1.0 - oran)).toInt().coerceAtLeast(1)

        halka.durumAyarla(
            yeniOlcek = NefesProgrami.olcek(desen, fazIndex, oran),
            faz = getString(NefesProgrami.tipAdRes(faz.tip)),
            kalan = kalan.toString(),
            ust = ust
        )
    }

    private fun fazTitret(uzun: Boolean) {
        if (!titresimAnahtar.isChecked) return
        if (!Store.getVibEnabled(this)) return
        runCatching {
            val v = if (Build.VERSION.SDK_INT >= 31) {
                (getSystemService(VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as? Vibrator
            } ?: return@runCatching
            val sure = if (uzun) 220L else 45L
            if (Build.VERSION.SDK_INT >= 26) {
                v.vibrate(VibrationEffect.createOneShot(sure, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(sure)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Önden çıkınca durakla: dönüşte "Sürdür" tek dokunuşla devam eder
        if (calisiyor) {
            birikenMs = toplamGecenMs()
            duraklat()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
