package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors

/**
 * v7.73 — Global arama ekranı.
 *
 * Tek kutudan görev · not · konu · ders · sohbet · plan · alışkanlık ·
 * etkinlik aranır. Sonuçlar kategoriye göre gruplanır; dokununca ilgili
 * ekrana gidilir.
 *
 * Arama **arka planda** çalışır (tüm kaynaklar taranıyor) ve yazma
 * durduktan 250 ms sonra tetiklenir — her harfte tam tarama yapılmaz.
 */
class AramaActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            context.startActivity(Intent(context, AramaActivity::class.java))
        }
    }

    private val yogunluk get() = resources.displayMetrics.density
    private lateinit var kutu: EditText
    private lateinit var sonucKap: LinearLayout
    private lateinit var durum: TextView

    private var filtre: Arama.Tur? = null
    private var sonSorgu = ""
    private val elci = android.os.Handler(android.os.Looper.getMainLooper())
    private var bekleyen: Runnable? = null

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

        val kok = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(
                MaterialColors.getColor(
                    this@AramaActivity,
                    com.google.android.material.R.attr.colorSurface, 0
                )
            )
            setPadding(
                (16 * yogunluk).toInt(), (16 * yogunluk).toInt(),
                (16 * yogunluk).toInt(), 0
            )
        }

        // ── Arama kutusu ──
        kutu = EditText(this).apply {
            hint = getString(R.string.ar_hint)
            inputType = InputType.TYPE_CLASS_TEXT
            maxLines = 1
            textSize = 16f
            setPadding(
                (12 * yogunluk).toInt(), (14 * yogunluk).toInt(),
                (12 * yogunluk).toInt(), (14 * yogunluk).toInt()
            )
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) = gecikmeliAra()
                override fun beforeTextChanged(c: CharSequence?, a: Int, b: Int, d: Int) {}
                override fun onTextChanged(c: CharSequence?, a: Int, b: Int, d: Int) {}
            })
        }
        kok.addView(
            MaterialCardView(this).apply {
                radius = 22 * yogunluk
                cardElevation = 1f
                addView(kutu)
            }
        )

        // ── Kategori filtreleri ──
        kok.addView(filtreSeridi())

        durum = TextView(this).apply {
            text = getString(R.string.ar_bos)
            textSize = 12.5f
            alpha = 0.7f
            setPadding(0, (10 * yogunluk).toInt(), 0, (6 * yogunluk).toInt())
        }
        kok.addView(durum)

        sonucKap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        kok.addView(
            ScrollView(this).apply {
                isFillViewport = true
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
                )
                addView(sonucKap)
            }
        )

        setContentView(kok)
        kutu.requestFocus()
    }

    /** Yazma durduktan 250 ms sonra ara — her harfte tarama yapma. */
    private fun gecikmeliAra() {
        bekleyen?.let { elci.removeCallbacks(it) }
        val gorev = Runnable { ara() }
        bekleyen = gorev
        elci.postDelayed(gorev, 250L)
    }

    private fun ara() {
        val sorgu = kutu.text?.toString()?.trim().orEmpty()
        sonSorgu = sorgu
        if (sorgu.length < 2) {
            sonucKap.removeAllViews()
            durum.text = getString(R.string.ar_bos)
            return
        }
        // Tüm kaynaklar taranıyor — arka planda
        Performans.arkaPlan {
            val turler = filtre?.let { setOf(it) } ?: emptySet()
            val sonuclar = try {
                Arama.ara(this, sorgu, turler)
            } catch (e: Exception) {
                android.util.Log.w("AramaActivity", "Arama başarısız", e)
                emptyList()
            }
            Performans.anaIs {
                if (isFinishing || isDestroyed) return@anaIs
                if (sorgu != sonSorgu) return@anaIs   // eski sonuç gelmesin
                sonuclariCiz(sorgu, sonuclar)
            }
        }
    }

    private fun sonuclariCiz(sorgu: String, sonuclar: List<Arama.Sonuc>) {
        sonucKap.removeAllViews()
        if (sonuclar.isEmpty()) {
            durum.text = getString(R.string.ar_sonuc_yok, sorgu)
            return
        }
        durum.text = getString(R.string.ar_sonuc, sonuclar.size)

        // Kategoriye göre grupla — her grupta en fazla 6 sonuç
        sonuclar.groupBy { it.tur }.forEach { (tur, liste) ->
            sonucKap.addView(grupBasligi(tur, liste.size))
            liste.take(6).forEach { sonucKap.addView(sonucKarti(it)) }
            if (liste.size > 6) {
                sonucKap.addView(TextView(this).apply {
                    text = getString(R.string.ar_daha, liste.size - 6)
                    textSize = 11.5f
                    alpha = 0.6f
                    setPadding(
                        (4 * yogunluk).toInt(), (2 * yogunluk).toInt(),
                        0, (6 * yogunluk).toInt()
                    )
                })
            }
        }
    }

    private fun grupBasligi(tur: Arama.Tur, adet: Int) = TextView(this).apply {
        text = tur.emoji + "  " + getString(tur.adRes) + "  (" + adet + ")"
        textSize = 12.5f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setTextColor(
            MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorPrimary, 0
            )
        )
        setPadding(0, (12 * yogunluk).toInt(), 0, (4 * yogunluk).toInt())
    }

    private fun sonucKarti(s: Arama.Sonuc): View {
        val kart = MaterialCardView(this).apply {
            radius = 14 * yogunluk
            cardElevation = 0f
            strokeWidth = (1 * yogunluk).toInt()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (6 * yogunluk).toInt() }
            isClickable = true
            setOnClickListener { sonucaGit(s) }
        }
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                (12 * yogunluk).toInt(), (10 * yogunluk).toInt(),
                (12 * yogunluk).toInt(), (10 * yogunluk).toInt()
            )
        }
        ic.addView(TextView(this).apply {
            text = s.baslik
            textSize = 14f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        })
        if (s.altYazi.isNotBlank()) {
            ic.addView(TextView(this).apply {
                text = s.altYazi
                textSize = 11.5f
                alpha = 0.7f
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setPadding(0, (3 * yogunluk).toInt(), 0, 0)
            })
        }
        kart.addView(ic)
        return kart
    }

    /** Sonuca dokununca ilgili ekranı açar. */
    private fun sonucaGit(s: Arama.Sonuc) {
        try {
            when (s.tur) {
                Arama.Tur.SOHBET -> {
                    SohbetGecmisi.setAktif(this, s.kimlik)
                    ekranAc(9)
                }
                Arama.Tur.GOREV -> ekranAc(6)
                Arama.Tur.NOT -> ekranAc(5)
                Arama.Tur.KONU -> ekranAc(3)
                Arama.Tur.DERS -> ekranAc(13)
                Arama.Tur.PLAN -> ekranAc(WidgetCommon.SCREEN_PLAN)
                Arama.Tur.ALISKANLIK -> ekranAc(12)
                Arama.Tur.ETKINLIK -> ekranAc(11)
                // v8.0: yeni kaynaklar doğrudan ilgili ekrana götürür
                Arama.Tur.TERIM -> SozlukActivity.ac(this)
                Arama.Tur.HATA -> HatalarimActivity.ac(this)
                Arama.Tur.ANLATIM -> {
                    // Anlatım ekranı madde metniyle açılır; başlık sonuçta var
                    KonuAnlatimActivity.ac(this, s.baslik, s.altYazi)
                }
                Arama.Tur.YERIMI -> {
                    // Yer iminin ait olduğu dersi o sayfadan aç
                    val ders = Store.loadLessons(this).firstOrNull { it.id == s.kimlik }
                    if (ders != null && ders.pdfAsset.isNotBlank()) {
                        startActivity(
                            android.content.Intent(this, LessonPdfActivity::class.java).apply {
                                putExtra(LessonPdfActivity.EXTRA_ASSET, ders.pdfAsset)
                                putExtra(LessonPdfActivity.EXTRA_TITLE, ders.title)
                                putExtra(LessonPdfActivity.EXTRA_LESSON_ID, ders.id)
                                putExtra(LessonPdfActivity.EXTRA_START_PAGE, s.ek.toInt())
                            }
                        )
                    } else {
                        ekranAc(13)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("AramaActivity", "Ekran açılamadı", e)
        }
    }

    private fun ekranAc(indeks: Int) {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(WidgetCommon.EXTRA_OPEN_SCREEN, indeks)
            }
        )
        finish()
    }

    /** Kategori filtre çipleri. */
    private fun filtreSeridi(): View {
        val satir = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val cipler = mutableListOf<Pair<TextView, Arama.Tur?>>()

        fun ekle(etiket: String, tur: Arama.Tur?) {
            val tv = cip(etiket, tur == filtre) {
                filtre = tur
                cipler.forEach { (v, t) -> cipBoya(v, t == filtre) }
                ara()
            }
            cipler.add(tv to tur)
            satir.addView(tv)
        }

        ekle(getString(R.string.ar_tumu), null)
        Arama.Tur.entries.forEach { ekle(it.emoji + " " + getString(it.adRes), it) }

        return android.widget.HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(0, (10 * yogunluk).toInt(), 0, 0)
            addView(satir)
        }
    }

    private fun cip(metin: String, secili: Boolean, tikla: () -> Unit) =
        TextView(this).apply {
            text = metin
            textSize = 12.5f
            gravity = Gravity.CENTER
            setPadding(
                (13 * yogunluk).toInt(), (7 * yogunluk).toInt(),
                (13 * yogunluk).toInt(), (7 * yogunluk).toInt()
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = (6 * yogunluk).toInt() }
            isClickable = true
            setOnClickListener { tikla() }
            cipBoya(this, secili)
        }

    private fun cipBoya(tv: TextView, secili: Boolean) {
        try {
            val vurgu = MaterialColors.getColor(
                tv, com.google.android.material.R.attr.colorPrimary, 0
            )
            tv.background = GradientDrawable().apply {
                cornerRadius = 16 * yogunluk
                if (secili) {
                    setColor((vurgu and 0x00FFFFFF) or 0x33000000)
                    setStroke((1.5f * yogunluk).toInt(), vurgu)
                } else {
                    setColor(0x14888888)
                }
            }
            tv.setTextColor(
                if (secili) vurgu
                else MaterialColors.getColor(
                    tv, com.google.android.material.R.attr.colorOnSurface, 0
                )
            )
        } catch (e: Exception) {
            android.util.Log.w("AramaActivity", "Çip boyanamadı", e)
        }
    }
}
