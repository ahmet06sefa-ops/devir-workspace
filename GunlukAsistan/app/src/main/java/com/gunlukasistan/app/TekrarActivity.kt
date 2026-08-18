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

/**
 * v9.0 — Tekrar oturumu (öneri 53, 57).
 *
 * ══════════════════════════════════════════════════════════════════
 * AKIŞ
 * ══════════════════════════════════════════════════════════════════
 * 1. Bugün tekrarı gelen maddeler karışık sırada gösteriliyor
 * 2. Kullanıcı önce SADECE başlığı görüyor ("Türev kuralları")
 * 3. "Hatırlamaya çalış" — düşünme süresi
 * 4. "Göster"e basınca varsa anlatım özeti açılıyor
 * 5. Dört seçenek: Unuttum · Zor · İyi · Kolay
 * 6. SM-2 bir sonraki tarihi hesaplıyor
 *
 * ── Neden önce sadece başlık ──
 * Aktif hatırlama (active recall) öğrenmenin en güçlü aracı.
 * Cevabı hemen göstermek "tanıma" (recognition) oluyor — çok daha
 * zayıf. Kullanıcıyı önce hatırlamaya zorlamak şart.
 *
 * ── Neden 4 seçenek, SM-2'nin 6'sı değil ──
 * 0-5 arası altı derece kullanıcı için anlamsız ("3 mü 4 mü?").
 * Anki de 4 düğme kullanıyor. İçeride 0/3/4/5'e eşleniyor.
 */
class TekrarActivity : AppCompatActivity() {

    companion object {
        /** Yalnız bu konunun maddeleri (0 = hepsi). */
        private const val EK_KONU = "konu_id"

        fun ac(context: Context, konuId: Long = 0L) {
            context.startActivity(
                Intent(context, TekrarActivity::class.java)
                    .putExtra(EK_KONU, konuId)
            )
            (context as? android.app.Activity)?.let { Canlandir.activityGirisi(it) }
        }
    }

    private lateinit var kok: LinearLayout
    private var sira: List<KonuTekrar.Durum> = emptyList()
    private var indeks = 0
    private var cevapAcik = false

    /** Oturum istatistiği. */
    private var sayacUnuttum = 0
    private var sayacHatirladim = 0
    private val baslangic = System.currentTimeMillis()

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
        UstBar.kur(this, getString(R.string.kt_oturum))
        kok = findViewById(R.id.gaKok)

        val konuId = intent.getLongExtra(EK_KONU, 0L)
        // v9.0 · Öneri 57: karışık sıra — aynı konudan iki madde
        // peş peşe gelmiyor. Blocking yerine interleaving.
        sira = if (konuId != 0L) {
            KonuTekrar.konununTekrarlari(this, konuId)
        } else {
            KonuTekrar.karisikSira(this)
        }

        ciz()
    }

    override fun finish() {
        super.finish()
        Canlandir.activityCikisi(this)
    }

    // ══════════════════════════════════════════════════════════

    private fun ciz() {
        kok.removeAllViews()

        if (indeks >= sira.size) {
            bitisEkrani()
            return
        }

        val d = sira[indeks]
        val yg = resources.displayMetrics.density
        fun dp(v: Int) = (v * yg).toInt()

        // ---- İlerleme ----
        kok.addView(TextView(this).apply {
            text = getString(R.string.kt_ilerleme, indeks + 1, sira.size)
            textSize = 12.5f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(dp(4), dp(14), 0, dp(6))
        })

        val cubuk = com.google.android.material.progressindicator.LinearProgressIndicator(this)
            .apply {
                max = sira.size
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(6)
                )
                trackCornerRadius = dp(3)
            }
        kok.addView(cubuk)
        Canlandir.cubuk(cubuk, indeks)

        // ---- Soru kartı ----
        val kart = MaterialCardView(this).apply {
            radius = 20 * yg
            cardElevation = 0f
            setCardBackgroundColor(renk(com.google.android.material.R.attr.colorPrimaryContainer))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(16) }
        }
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
        }

        // Konu adı (bağlam)
        val konuAdi = runCatching {
            Store.loadTopics(this).firstOrNull { it.id == d.konuId }?.title ?: ""
        }.getOrDefault("")
        if (konuAdi.isNotBlank()) {
            ic.addView(TextView(this).apply {
                text = KonuGorunum.baslikla(this@TekrarActivity, d.konuId, konuAdi)
                textSize = 12f
                setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
                setPadding(0, 0, 0, dp(8))
            })
        }

        // Madde başlığı — büyük
        ic.addView(TextView(this).apply {
            text = d.baslik
            textSize = 20f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setLineSpacing(0f, 1.25f)
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
        })

        // Zorluk rozeti
        if (d.zorMu) {
            ic.addView(TextView(this).apply {
                setText(R.string.kt_zor_madde)
                textSize = 11.5f
                setTextColor(0xFFD9534F.toInt())
                setPadding(0, dp(8), 0, 0)
            })
        }
        kart.addView(ic)
        kok.addView(kart)

        if (!cevapAcik) {
            // ---- Hatırlama aşaması ----
            kok.addView(TextView(this).apply {
                setText(R.string.kt_hatirla)
                textSize = 13.5f
                gravity = Gravity.CENTER
                setLineSpacing(0f, 1.3f)
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
                setPadding(dp(16), dp(26), dp(16), dp(20))
            })
            kok.addView(MaterialButton(this).apply {
                setText(R.string.kt_goster)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener {
                    Titresim.dokunus(it)
                    cevapAcik = true
                    ciz()
                }
            })
        } else {
            // ---- Cevap + değerlendirme ----
            anlatimOzeti(d)?.let { ozet ->
                val ozetKart = MaterialCardView(this).apply {
                    radius = 16 * yg
                    cardElevation = 0f
                    strokeWidth = dp(1)
                    strokeColor = renk(com.google.android.material.R.attr.colorOutlineVariant)
                    setCardBackgroundColor(renk(com.google.android.material.R.attr.colorSurface))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = dp(12) }
                }
                ozetKart.addView(TextView(this).apply {
                    text = ozet
                    textSize = 13.5f
                    setLineSpacing(0f, 1.4f)
                    setPadding(dp(16), dp(14), dp(16), dp(14))
                    setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
                })
                kok.addView(ozetKart)
            }

            kok.addView(TextView(this).apply {
                setText(R.string.kt_ne_kadar)
                textSize = 13f
                gravity = Gravity.CENTER
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
                setPadding(dp(16), dp(22), dp(16), dp(12))
            })

            // Dört değerlendirme düğmesi
            kaliteDugmesi(
                "😵", R.string.kt_unuttum, R.string.kt_unuttum_alt,
                0xFFD9534F.toInt(), KonuTekrar.KALITE_UNUTTUM, d
            )
            kaliteDugmesi(
                "😐", R.string.kt_zor, R.string.kt_zor_alt,
                0xFFE0A33A.toInt(), KonuTekrar.KALITE_ZOR, d
            )
            kaliteDugmesi(
                "🙂", R.string.kt_iyi, R.string.kt_iyi_alt,
                0xFF4C9A5A.toInt(), KonuTekrar.KALITE_IYI, d
            )
            kaliteDugmesi(
                "😄", R.string.kt_kolay, R.string.kt_kolay_alt,
                0xFF3FA0C4.toInt(), KonuTekrar.KALITE_KOLAY, d
            )
        }

        kok.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(30)
            )
        })
    }

    /**
     * Değerlendirme düğmesi.
     *
     * Alt yazıda bir sonraki tekrarın ne zaman olacağı yazıyor —
     * kullanıcı seçiminin sonucunu önceden görüyor. Anki'nin en
     * sevilen özelliği bu.
     */
    private fun kaliteDugmesi(
        simge: String,
        baslikRes: Int,
        altRes: Int,
        renk: Int,
        kalite: Int,
        d: KonuTekrar.Durum
    ) {
        val yg = resources.displayMetrics.density
        fun dp(v: Int) = (v * yg).toInt()

        // Bu seçenek seçilirse aralık ne olacak — önden hesapla
        val (_, tahminiAralik, _) = KonuTekrar.sm2(d.tekrarSayisi, d.aralik, d.ef, kalite)

        val kart = MaterialCardView(this).apply {
            radius = 16 * yg
            cardElevation = 0f
            strokeWidth = dp(2)
            strokeColor = renk
            setCardBackgroundColor(
                android.graphics.Color.argb(
                    22, android.graphics.Color.red(renk),
                    android.graphics.Color.green(renk),
                    android.graphics.Color.blue(renk)
                )
            )
            isClickable = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        }
        val satir = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        satir.addView(TextView(this).apply {
            text = simge
            textSize = 22f
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
            gravity = Gravity.CENTER
        })
        val m = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ).apply { marginStart = dp(10) }
        }
        m.addView(TextView(this).apply {
            setText(baslikRes)
            textSize = 15.5f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(this@TekrarActivity.renk(com.google.android.material.R.attr.colorOnSurface))
        })
        m.addView(TextView(this).apply {
            setText(altRes)
            textSize = 11.5f
            setTextColor(this@TekrarActivity.renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
        })
        satir.addView(m)
        // Sonraki tekrar zamanı
        satir.addView(TextView(this).apply {
            text = KonuTekrar.araliklMetni(this@TekrarActivity, tahminiAralik)
            textSize = 12f
            setTextColor(renk)
        })
        kart.addView(satir)
        kart.dalgaEkle()
        kart.setOnClickListener {
            if (kalite < 3) {
                Titresim.yanlis(this)
                sayacUnuttum++
            } else {
                Titresim.dogru(this)
                sayacHatirladim++
            }
            KonuTekrar.tekrarSonucu(this, d.maddeId, kalite)
            indeks++
            cevapAcik = false
            ciz()
        }
        kok.addView(kart)
    }

    /**
     * Varsa maddenin AI anlatımından özet.
     *
     * `KonuUretici.anlatimOku` bir `Anlatim` nesnesi döndürüyor;
     * içinde hazır `ozet` alanı var. Yoksa ilk bölümün metninden
     * kısa bir parça alınıyor.
     */
    private fun anlatimOzeti(d: KonuTekrar.Durum): String? = runCatching {
        val a = KonuUretici.anlatimOku(this, d.baslik) ?: return null
        if (!a.ok) return null
        val ham = a.ozet.ifBlank { a.duzMetin() }
        val temiz = ham.replace(Regex("[#*_`>]"), "").trim()
        if (temiz.isBlank()) return null
        temiz.take(400) + if (temiz.length > 400) "…" else ""
    }.getOrNull()

    // ══════════════════════════════════════════════════════════

    private fun bitisEkrani() {
        val yg = resources.displayMetrics.density
        fun dp(v: Int) = (v * yg).toInt()
        val toplam = sayacUnuttum + sayacHatirladim

        if (toplam == 0) {
            // Hiç tekrar yoktu
            kok.addView(TextView(this).apply {
                setText(R.string.kt_bos)
                textSize = 15f
                gravity = Gravity.CENTER
                setLineSpacing(0f, 1.35f)
                setPadding(dp(24), dp(60), dp(24), dp(24))
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            })
            return
        }

        val yuzde = sayacHatirladim * 100 / toplam
        val dakika = ((System.currentTimeMillis() - baslangic) / 60_000L).toInt()

        kok.addView(PuanHalkasi(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(160), dp(160)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(30)
            }
            ayarla(sayacHatirladim, toplam)
        })

        kok.addView(TextView(this).apply {
            text = getString(R.string.kt_bitti_baslik)
            textSize = 19f
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, dp(18), 0, dp(4))
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
        })
        kok.addView(TextView(this).apply {
            text = getString(R.string.kt_bitti_ozet, toplam, dakika.coerceAtLeast(1))
            textSize = 13.5f
            gravity = Gravity.CENTER
            setLineSpacing(0f, 1.3f)
            setPadding(dp(20), 0, dp(20), dp(20))
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
        })

        // Dürüst geri bildirim (öneri 68'in çekirdeği)
        val yorum = when {
            yuzde >= 90 -> getString(R.string.kt_yorum_harika)
            yuzde >= 70 -> getString(R.string.kt_yorum_iyi)
            yuzde >= 50 -> getString(R.string.kt_yorum_orta)
            else -> getString(R.string.kt_yorum_zayif)
        }
        val yorumKart = MaterialCardView(this).apply {
            radius = 16 * yg
            cardElevation = 0f
            setCardBackgroundColor(renk(com.google.android.material.R.attr.colorSecondaryContainer))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        yorumKart.addView(TextView(this).apply {
            text = yorum
            textSize = 13.5f
            setLineSpacing(0f, 1.35f)
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
        })
        kok.addView(yorumKart)

        kok.addView(MaterialButton(this).apply {
            setText(R.string.done)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(20) }
            setOnClickListener { finish() }
        })

        if (yuzde >= 80) Kutlama.goster(this, Kutlama.TUR_YILDIZ)
        Titresim.basari(this)
    }

    private fun renk(attr: Int): Int = runCatching {
        com.google.android.material.color.MaterialColors.getColor(kok, attr, 0)
    }.getOrDefault(0)
}
