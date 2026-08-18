package com.gunlukasistan.app

import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.color.MaterialColors

/**
 * v10.22 — Gizlilik Kilidi ekranı.
 *
 * PIN kuruluysa, App.kt yaşam döngüsü bekçisi uygulama öne gelirken
 * bu ekranı açar. Görünüm koddan kurulur (XML yok) — TasarimOlcegiTest
 * yalnız XML taradığı için burada programatik kurulum serbesttir ve
 * her şey tek dosyada kalır.
 *
 * Kurallar:
 *  · Geri tuşu uygulamayı arka plana atar — kilit atlatılamaz.
 *  · FLAG_SECURE: ekran görüntüsü ve son-uygulamalar önizlemesi kapalı.
 *  · 5 yanlışta giriş ve düğme 30 sn kilitlenir (sayacı ekran yazar).
 */
class KilitActivity : AppCompatActivity() {

    companion object {
        /** Ekran şu an önde mi — bekçi ikinci kez açmasın diye. */
        @Volatile var gosteriliyor: Boolean = false
    }

    private lateinit var giris: EditText
    private lateinit var hata: TextView
    private lateinit var acDugme: Button
    private val sayac = Handler(Looper.getMainLooper())
    private var bekletiliyor = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        val yg = resources.displayMetrics.density
        fun dp(v: Int) = (v * yg).toInt()

        val renkMetin = MaterialColors.getColor(
            this, com.google.android.material.R.attr.colorOnSurface, 0xFF222222.toInt()
        )
        val renkIkincil = MaterialColors.getColor(
            this, com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF666666.toInt()
        )
        val renkHata = MaterialColors.getColor(
            this, com.google.android.material.R.attr.colorError, 0xFFD9534F.toInt()
        )

        val simge = TextView(this).apply {
            text = "🔒"
            textSize = 44f
            gravity = Gravity.CENTER
        }
        val baslik = TextView(this).apply {
            setText(R.string.w22_baslik)
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(renkMetin)
            gravity = Gravity.CENTER
        }
        val alt = TextView(this).apply {
            setText(R.string.w22_alt)
            textSize = 13f
            setTextColor(renkIkincil)
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(18))
        }
        giris = EditText(this).apply {
            hint = getString(R.string.w22_pin_ipucu)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            filters = arrayOf(InputFilter.LengthFilter(KilitMantik.PIN_MAX))
            gravity = Gravity.CENTER
            textSize = 20f
            imeOptions = EditorInfo.IME_ACTION_DONE
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) { dene(); true } else false
            }
        }
        acDugme = Button(this).apply {
            setText(R.string.w22_ac)
            setOnClickListener { dene() }
        }
        hata = TextView(this).apply {
            textSize = 13f
            setTextColor(renkHata)
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, 0)
        }

        val icerik = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(simge)
            addView(baslik)
            addView(alt)
            addView(
                giris, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
            addView(
                acDugme, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(14) }
            )
            addView(hata)
        }

        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(32), dp(24), dp(32), dp(24))
                addView(
                    icerik, LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                )
            }
        )

        beklemeTazele()
        giris.requestFocus()
    }

    override fun onResume() {
        super.onResume()
        gosteriliyor = true
    }

    override fun onPause() {
        // finish() ÇAĞRILMADAN arka plana giderse (ana ekran tuşu vb.)
        // bayrak temizlenmeli; yoksa bekçi sonsuza dek "zaten açık" sanır.
        gosteriliyor = false
        super.onPause()
    }

    override fun onDestroy() {
        sayac.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    @Deprecated("Kilit ekranı geri tuşuyla atlatılamaz; uygulama arka plana gider.")
    override fun onBackPressed() {
        @Suppress("DEPRECATION")
        moveTaskToBack(true)
    }

    /** Girilen PIN'i dener; başarı → kapan, hata → sayacı işlet. */
    private fun dene() {
        val simdi = System.currentTimeMillis()
        if (KilitMantik.beklemedeMi(KilitDepo.denemeDurumu(this), simdi)) {
            beklemeTazele()
            return
        }
        val pin = giris.text?.toString().orEmpty()
        // v10.23 · Hatasızlık md 1: biçimi bozuk giriş DENEME HAKKI YAKMASIN.
        // Boş veya 3 haneli bir giriş, doğrulamaya hiç gitmeden sayacı
        // artırıyordu — masum kullanıcı haksız 30 sn beklemeye düşerdi.
        if (!KilitMantik.pinGecerliMi(pin)) {
            bekletiliyor = false
            hata.setText(R.string.w22_hata_kural)
            giris.text?.clear()
            giris.requestFocus()
            return
        }
        if (KilitDepo.pinDogruMu(this, pin)) {
            KilitDepo.dogruKaydet(this)
            sayac.removeCallbacksAndMessages(null)
            finish()
            overridePendingTransition(0, 0)
        } else {
            val durum = KilitDepo.yanlisKaydet(this, simdi)
            giris.text?.clear()
            if (KilitMantik.beklemedeMi(durum, simdi)) {
                beklemeTazele()
            } else {
                bekletiliyor = false
                hata.text = getString(R.string.w22_deneme, KilitMantik.kalanHak(durum))
                giris.requestFocus()
            }
        }
    }

    /** Bekleme dalında kalan süreyi her saniye yazar; bitince girişi açar. */
    private fun beklemeTazele() {
        val simdi = System.currentTimeMillis()
        val kalan = KilitMantik.kalanBeklemeSn(KilitDepo.denemeDurumu(this), simdi)
        if (kalan > 0) {
            bekletiliyor = true
            giris.isEnabled = false
            acDugme.isEnabled = false
            hata.text = getString(R.string.w22_bekle, kalan)
            sayac.postDelayed({ if (!isFinishing) beklemeTazele() }, 1000L)
        } else {
            giris.isEnabled = true
            acDugme.isEnabled = true
            if (bekletiliyor) {
                hata.text = ""
                bekletiliyor = false
            }
        }
    }
}
