package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * v9.8 — Sistem ve kullanım ekranı (öneri 47-50).
 *
 * Dört özellik tek ekranda:
 *   📊 Kullanım istatistiği (öneri 50)
 *   🔄 Güncelleme kontrolü (öneri 48)
 *   🐞 Çökme kayıtları (öneri 49)
 *   ⚙️ Arka plan işleri (öneri 47)
 *
 * Hepsi aynı soruyu farklı açılardan yanıtlıyor: **uygulama
 * gerçekte nasıl davranıyor?**
 */
class SistemActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SEKME = "sekme"
        const val S_KULLANIM = 0
        const val S_GUNCELLEME = 1
        const val S_COKME = 2
        const val S_ARKAPLAN = 3

        fun ac(context: Context, sekme: Int = S_KULLANIM) {
            runCatching {
                context.startActivity(
                    Intent(context, SistemActivity::class.java).putExtra(EXTRA_SEKME, sekme)
                )
                (context as? android.app.Activity)?.let { Canlandir.activityGirisi(it) }
            }
        }
    }

    private lateinit var kok: LinearLayout
    private var sekme = S_KULLANIM

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
        UstBar.kur(this, getString(R.string.sy_baslik))
        kok = findViewById(R.id.gaKok)

        sekme = intent?.getIntExtra(EXTRA_SEKME, S_KULLANIM) ?: S_KULLANIM
        Kullanim.ekran(this, Kullanim.Ekran.ISTATISTIK)
        yukle()
    }

    override fun finish() {
        super.finish()
        Canlandir.activityCikisi(this)
    }

    override fun onResume() {
        super.onResume()
        yukle()
    }

    private fun yg() = resources.displayMetrics.density
    private fun dp(v: Int) = (v * yg()).toInt()
    private fun renk(attr: Int): Int = runCatching {
        com.google.android.material.color.MaterialColors.getColor(kok, attr, 0)
    }.getOrDefault(0)

    // ══════════════════════════════════════════════════════════

    private fun yukle() {
        kok.removeAllViews()
        sekmeleriCiz()
        when (sekme) {
            S_GUNCELLEME -> guncellemeSekmesi()
            S_COKME -> cokmeSekmesi()
            S_ARKAPLAN -> arkaPlanSekmesi()
            else -> kullanimSekmesi()
        }
        kok.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48)
            )
        })
    }

    private fun sekmeleriCiz() {
        val kaydir = android.widget.HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(4); bottomMargin = dp(8) }
        }
        val satir = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

        val cokmeSayi = runCatching { CokmeRapor.gecmis(this).size }.getOrDefault(0)
        val guncelVar = runCatching { Guncelleme.bekleyenVar(this) != null }.getOrDefault(false)

        listOf(
            Triple(S_KULLANIM, "📊", getString(R.string.sy_s_kullanim)),
            Triple(
                S_GUNCELLEME, if (guncelVar) "🔔" else "🔄",
                getString(R.string.sy_s_guncelleme)
            ),
            Triple(
                S_COKME, "🐞",
                getString(R.string.sy_s_cokme) + if (cokmeSayi > 0) " ($cokmeSayi)" else ""
            ),
            Triple(S_ARKAPLAN, "⚙️", getString(R.string.sy_s_arkaplan))
        ).forEach { (kod, emoji, ad) ->
            val secili = kod == sekme
            satir.addView(MaterialButton(
                this, null,
                if (secili) com.google.android.material.R.attr.materialButtonStyle
                else com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply {
                text = "$emoji $ad"
                textSize = 12f
                isAllCaps = false
                minWidth = 0; minimumWidth = 0
                insetTop = 0; insetBottom = 0
                setPadding(dp(14), dp(6), dp(14), dp(6))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = dp(6) }
                contentDescription = ad
                setOnClickListener { Titresim.dokunus(it); sekme = kod; yukle() }
            })
        }
        kaydir.addView(satir)
        kok.addView(kaydir)
    }

    // ══════════════════════════════════════════════════════════
    // Sekme: Kullanım (öneri 50)
    // ══════════════════════════════════════════════════════════

    private fun kullanimSekmesi() {
        if (!Kullanim.acikMi(this)) {
            bilgiKarti(getString(R.string.ku_kapali_aciklama), false)
            kok.addView(MaterialButton(this).apply {
                setText(R.string.ku_ac)
                isAllCaps = false
                layoutParams = genisDugme()
                setOnClickListener {
                    Titresim.dokunus(it); Kullanim.ayarla(this@SistemActivity, true); yukle()
                }
            })
            return
        }

        // Gizlilik notu — en üstte, çünkü en önemli bilgi
        bilgiKarti(getString(R.string.ku_gizlilik), false)

        // Özet
        val oturum = Kullanim.oturumSayisi(this)
        val gun = Kullanim.aktifGunSayisi(this)
        if (oturum > 0) {
            val kart = cerceve(renk(com.google.android.material.R.attr.colorPrimary), 2)
            val ic = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(16), dp(16), dp(16))
            }
            ic.addView(TextView(this).apply {
                setText(R.string.ku_ozet_baslik)
                textSize = 12f
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            })
            ic.addView(TextView(this).apply {
                text = getString(R.string.ku_oturum_sayi, oturum)
                textSize = 26f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
            })
            ic.addView(TextView(this).apply {
                text = getString(
                    R.string.ku_gun_ortalama, gun,
                    String.format(Locale.US, "%.1f", Kullanim.gunlukOrtalama(this@SistemActivity))
                )
                textSize = 12.5f
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            })
            val bas = Kullanim.baslangicTarihi(this)
            if (bas > 0) {
                ic.addView(TextView(this).apply {
                    text = getString(R.string.ku_baslangic, tarih(bas))
                    textSize = 11.5f
                    setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
                })
            }
            kart.addView(ic)
            kok.addView(kart)
        }

        // Çıkarımlar
        runCatching { Kullanim.cikarimlar(this) }.getOrDefault(emptyList())
            .forEach { bilgiKarti(it, false) }

        // Ekran listesi
        val ekranlar = runCatching { Kullanim.ekranlar(this) }.getOrDefault(emptyList())
        if (ekranlar.isEmpty()) {
            bilgiKarti(getString(R.string.ku_veri_yok), false)
        } else {
            baslik(getString(R.string.ku_ekranlar))
            val enBuyuk = ekranlar.firstOrNull()?.sayi ?: 1
            ekranlar.take(20).forEach { s -> cubukSatiri(s.gosterim, s.sayi, enBuyuk) }
        }

        // Eylemler
        val eylemler = runCatching { Kullanim.eylemler(this) }.getOrDefault(emptyList())
        if (eylemler.isNotEmpty()) {
            baslik(getString(R.string.ku_eylemler))
            val enBuyuk = eylemler.firstOrNull()?.sayi ?: 1
            eylemler.take(12).forEach { s -> cubukSatiri(s.gosterim, s.sayi, enBuyuk) }
        }

        // Hiç açılmayanlar
        val hic = runCatching {
            Kullanim.hicKullanilmayanlar(this, Kullanim.Ekran.HEPSI)
        }.getOrDefault(emptyList())
        if (hic.isNotEmpty() && ekranlar.isNotEmpty()) {
            baslik(getString(R.string.ku_hic_acilmayan, hic.size))
            bilgiKarti(hic.joinToString(" · "), false)
        }

        // Eylemler
        baslik(getString(R.string.tk_ayarlar))
        ayarSatiri(getString(R.string.ku_kopyala), "📋") {
            runCatching {
                val pano = getSystemService(Context.CLIPBOARD_SERVICE)
                        as android.content.ClipboardManager
                pano.setPrimaryClip(
                    android.content.ClipData.newPlainText(
                        "kullanim", Kullanim.metinOzet(this)
                    )
                )
                Bildir.basari(kok, getString(R.string.ku_kopyalandi))
            }
        }
        ayarSatiri(getString(R.string.ku_temizle), "🗑") {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.ku_temizle)
                .setMessage(R.string.ku_temizle_onay)
                .setPositiveButton(R.string.tk_sil) { _, _ ->
                    Kullanim.temizle(this); yukle()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
        ayarSatiri(getString(R.string.ku_kapat), "🚫") {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.ku_kapat)
                .setMessage(R.string.ku_kapat_onay)
                .setPositiveButton(R.string.ku_kapat) { _, _ ->
                    Kullanim.ayarla(this, false); yukle()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    /** Yatay çubuklu satır — oransal karşılaştırma için. */
    private fun cubukSatiri(ad: String, sayi: Int, enBuyuk: Int) {
        val kapsayici = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(2), dp(5), dp(2), dp(5))
        }
        val ust = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        ust.addView(TextView(this).apply {
            text = ad
            textSize = 13f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        })
        ust.addView(TextView(this).apply {
            text = sayi.toString()
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
        })
        kapsayici.addView(ust)

        val oran = if (enBuyuk > 0) sayi.toFloat() / enBuyuk else 0f
        val cubukKapsayici = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(5)
            ).apply { topMargin = dp(4) }
        }
        cubukKapsayici.addView(View(this).apply {
            setBackgroundColor(renk(com.google.android.material.R.attr.colorPrimary))
            layoutParams = LinearLayout.LayoutParams(0, dp(5), oran.coerceAtLeast(0.02f))
        })
        cubukKapsayici.addView(View(this).apply {
            setBackgroundColor(renk(com.google.android.material.R.attr.colorSurfaceVariant))
            layoutParams = LinearLayout.LayoutParams(0, dp(5), (1f - oran).coerceAtLeast(0f))
        })
        kapsayici.addView(cubukKapsayici)
        kok.addView(kapsayici)
    }

    // ══════════════════════════════════════════════════════════
    // Sekme: Güncelleme (öneri 48)
    // ══════════════════════════════════════════════════════════

    private fun guncellemeSekmesi() {
        val kart = cerceve(renk(com.google.android.material.R.attr.colorPrimary), 2)
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        ic.addView(TextView(this).apply {
            setText(R.string.gc_mevcut_surum)
            textSize = 12f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
        })
        ic.addView(TextView(this).apply {
            text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
        })
        val son = Guncelleme.sonKontrol(this)
        ic.addView(TextView(this).apply {
            text = if (son > 0) getString(R.string.gc_son_kontrol, tarih(son))
            else getString(R.string.gc_hic_kontrol)
            textSize = 11.5f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
        })
        kart.addView(ic)
        kok.addView(kart)

        // Bekleyen güncelleme
        val bekleyen = runCatching { Guncelleme.bekleyenVar(this) }.getOrNull()
        if (bekleyen != null) {
            baslik(getString(R.string.gc_yeni_var))
            val yeniKart = cerceve(GrafikDili.basari(this), 2)
            val yic = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(14))
            }
            yic.addView(TextView(this).apply {
                text = getString(R.string.gc_surum_satir, bekleyen.ad, bekleyen.kod)
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
            })
            if (bekleyen.notlar.isNotBlank()) {
                yic.addView(TextView(this).apply {
                    text = bekleyen.notlar
                    textSize = 13f
                    setLineSpacing(0f, 1.3f)
                    setPadding(0, dp(6), 0, 0)
                    setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
                })
            }
            yic.addView(MaterialButton(this).apply {
                setText(R.string.gc_indir)
                isAllCaps = false
                layoutParams = genisDugme()
                setOnClickListener {
                    Titresim.dokunus(it)
                    if (!Guncelleme.indirmeyiAc(this@SistemActivity, bekleyen)) {
                        Bildir.hata(kok, getString(R.string.gc_link_yok))
                    }
                }
            })
            yic.addView(MaterialButton(
                this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply {
                setText(R.string.gc_ertele)
                isAllCaps = false
                layoutParams = genisDugme()
                setOnClickListener {
                    Titresim.dokunus(it)
                    Guncelleme.ertele(this@SistemActivity, bekleyen.kod)
                    Bildir.bilgi(kok, getString(R.string.gc_ertelendi))
                    yukle()
                }
            })
            yeniKart.addView(yic)
            kok.addView(yeniKart)
        } else {
            bilgiKarti(getString(R.string.gc_guncel), false)
        }

        kok.addView(MaterialButton(this).apply {
            setText(R.string.gc_simdi_kontrol)
            isAllCaps = false
            layoutParams = genisDugme()
            setOnClickListener {
                Titresim.dokunus(it)
                isEnabled = false
                setText(R.string.gc_kontrol_ediliyor)
                ArkaPlan.calisGuvenli(
                    this@SistemActivity,
                    is_ = { Guncelleme.kontrolEt(this@SistemActivity, zorla = true) },
                    hata = {
                        isEnabled = true
                        setText(R.string.gc_simdi_kontrol)
                        Bildir.hata(kok, getString(R.string.on_err_ag))
                    }
                ) { sonuc ->
                    isEnabled = true
                    setText(R.string.gc_simdi_kontrol)
                    when (sonuc) {
                        is Guncelleme.Sonuc.Yeni ->
                            Bildir.basari(kok, getString(R.string.gc_bulundu, sonuc.surum.ad))
                        is Guncelleme.Sonuc.Guncel ->
                            Bildir.bilgi(kok, getString(R.string.gc_guncel))
                        is Guncelleme.Sonuc.Hata ->
                            Bildir.hata(kok, sonuc.mesaj)
                    }
                    yukle()
                }
            }
        })

        baslik(getString(R.string.tk_ayarlar))
        ayarSatiri(
            getString(R.string.gc_otomatik),
            if (Guncelleme.acikMi(this)) getString(R.string.tk_acik)
            else getString(R.string.tk_kapali)
        ) {
            Guncelleme.ayarla(this, !Guncelleme.acikMi(this)); yukle()
        }
        bilgiKarti(getString(R.string.gc_aciklama), false)
    }

    // ══════════════════════════════════════════════════════════
    // Sekme: Çökme (öneri 49)
    // ══════════════════════════════════════════════════════════

    private fun cokmeSekmesi() {
        val gecmis = runCatching { CokmeRapor.gecmis(this) }.getOrDefault(emptyList())

        if (gecmis.isEmpty()) {
            bilgiKarti(getString(R.string.cr_yok), false)
        } else {
            bilgiKarti(getString(R.string.cr_aciklama), false)
            baslik(getString(R.string.cr_kayitlar, gecmis.size))

            runCatching { CokmeRapor.tekrarEdenler(this) }.getOrDefault(emptyList())
                .forEach { (kayit, sayi) -> cokmeKarti(kayit, sayi) }

            kok.addView(MaterialButton(this).apply {
                setText(R.string.cr_tumunu_paylas)
                isAllCaps = false
                layoutParams = genisDugme()
                setOnClickListener {
                    Titresim.dokunus(it)
                    paylas(CokmeRapor.tumRapor(this@SistemActivity))
                }
            })
            kok.addView(MaterialButton(
                this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply {
                setText(R.string.cr_temizle)
                isAllCaps = false
                layoutParams = genisDugme()
                setOnClickListener {
                    Titresim.dokunus(it)
                    CokmeRapor.temizle(this@SistemActivity)
                    Bildir.basari(kok, getString(R.string.cr_temizlendi))
                    yukle()
                }
            })
        }

        baslik(getString(R.string.tk_ayarlar))
        ayarSatiri(
            getString(R.string.cr_sor),
            if (CokmeRapor.sormaKapali(this)) getString(R.string.tk_kapali)
            else getString(R.string.tk_acik)
        ) {
            CokmeRapor.sormaAyarla(this, !CokmeRapor.sormaKapali(this)); yukle()
        }
        bilgiKarti(getString(R.string.cr_gizlilik), false)
    }

    private fun cokmeKarti(kayit: CokmeRapor.Kayit, tekrar: Int) {
        val kart = cerceve(
            if (tekrar > 1) GrafikDili.hata(this)
            else renk(com.google.android.material.R.attr.colorOutlineVariant),
            if (tekrar > 1) 2 else 1
        )
        kart.isClickable = true
        kart.dalgaEkle()
        kart.setOnClickListener {
            Titresim.dokunus(it)
            MaterialAlertDialogBuilder(this)
                .setTitle(kayit.tur)
                .setMessage(CokmeRapor.rapor(this, kayit).take(3000))
                .setPositiveButton(R.string.dp_paylas) { _, _ ->
                    paylas(CokmeRapor.rapor(this, kayit))
                }
                .setNegativeButton(R.string.close, null)
                .show()
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
            text = kayit.tur
            textSize = 14.5f
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        })
        if (tekrar > 1) {
            ust.addView(TextView(this).apply {
                text = getString(R.string.cr_tekrar, tekrar)
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(GrafikDili.hata(this@SistemActivity))
            })
        }
        ic.addView(ust)
        if (kayit.mesaj.isNotBlank()) {
            ic.addView(TextView(this).apply {
                text = kayit.mesaj.take(150)
                textSize = 12.5f
                setPadding(0, dp(3), 0, 0)
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            })
        }
        ic.addView(TextView(this).apply {
            text = "v${kayit.surumAdi} · ${tarih(kayit.zaman)} · ${kayit.parca}"
            textSize = 11.5f
            setPadding(0, dp(4), 0, 0)
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
        })
        kart.addView(ic)
        kok.addView(kart)
    }

    private fun paylas(metin: String) {
        if (metin.isBlank()) return
        runCatching {
            startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Günlük Asistan hata raporu")
                        putExtra(Intent.EXTRA_TEXT, metin)
                    },
                    getString(R.string.dp_paylas)
                )
            )
        }.onFailure { Bildir.hata(kok, getString(R.string.cr_paylasilamadi)) }
    }

    // ══════════════════════════════════════════════════════════
    // Sekme: Arka plan (öneri 47)
    // ══════════════════════════════════════════════════════════

    private fun arkaPlanSekmesi() {
        bilgiKarti(getString(R.string.ap_aciklama), false)

        baslik(getString(R.string.ap_isler))
        kok.addView(Iskelet(this).apply {
            sekil = Iskelet.SEKIL_LISTE
            satirSayisi = 3
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(150)
            )
        })
        val iskeletIndeks = kok.childCount - 1

        // WorkManager durumu bloklayan bir çağrı — arka planda
        ArkaPlan.calisGuvenli(
            this,
            is_ = {
                Triple(
                    runCatching { ArkaPlanIs.durumlar(this) }.getOrDefault(emptyList()),
                    runCatching { ArkaPlanIs.sonBakim(this) }.getOrDefault(0L),
                    runCatching { alarmOzeti() }.getOrDefault("")
                )
            },
            hata = {
                runCatching { kok.getChildAt(iskeletIndeks)?.visibility = View.GONE }
            }
        ) { (durumlar, sonBakim, alarmOzet) ->
            runCatching { kok.removeViewAt(iskeletIndeks) }
            durumlar.forEach { d ->
                ayarSatiri(
                    d.ad,
                    if (d.calisiyor) getString(R.string.ap_kurulu)
                    else getString(R.string.ap_bekliyor)
                ) {}
            }
            if (sonBakim > 0) {
                bilgiKarti(getString(R.string.ap_son_bakim, tarih(sonBakim)), false)
            } else {
                bilgiKarti(getString(R.string.ap_hic_calismadi), false)
            }
            if (alarmOzet.isNotBlank()) {
                baslik(getString(R.string.ap_alarmlar))
                bilgiKarti(alarmOzet, false)
            }

            baslik(getString(R.string.tk_ayarlar))
            ayarSatiri(getString(R.string.ap_simdi_yedekle), "💾") {
                ArkaPlanIs.yedekKuyrugaAl(this, 0)
                Bildir.basari(kok, getString(R.string.ap_kuyruga_alindi))
            }
            ayarSatiri(getString(R.string.ap_yeniden_kur), "🔁") {
                ArkaPlanIs.bakimiKur(this)
                if (OnlineBekci.acikMi(this)) {
                    ArkaPlanIs.senkronuKur(this, OnlineBekci.siklikDk(this))
                }
                Bildir.basari(kok, getString(R.string.ap_kuruldu))
                yukle()
            }
            bilgiKarti(getString(R.string.ap_neden_alarm), false)
        }
    }

    // ══════════════════════════════════════════════════════════
    // Ortak
    // ══════════════════════════════════════════════════════════

    private fun tarih(ms: Long): String = runCatching {
        SimpleDateFormat("d MMM HH:mm", Locale("tr", "TR")).format(Date(ms))
    }.getOrDefault("—")

    /**
     * Alarm sağlığı özeti.
     *
     * `AlarmSagligi`'nda hazır bir `ozet()` yok — tek tek okuyup
     * birleştiriyorum. Bu bilgi tanılama açısından değerli:
     * "alarmlar en son ne zaman kuruldu ve kaç tane" sorusu
     * bildirim gelmiyor şikâyetlerinin ilk durağı.
     */
    private fun alarmOzeti(): String = buildString {
        val son = runCatching { AlarmSagligi.sonKurulum(this@SistemActivity) }.getOrDefault(0L)
        val sayi = runCatching { AlarmSagligi.kurulanSayi(this@SistemActivity) }.getOrDefault(-1)
        val sebep = runCatching { AlarmSagligi.sonKurulumSebebi(this@SistemActivity) }
            .getOrDefault("")
        if (son > 0) {
            append(getString(R.string.ap_alarm_son, tarih(son)))
            if (sebep.isNotBlank()) append(" · $sebep")
            if (sayi >= 0) append("\n" + getString(R.string.ap_alarm_sayi, sayi))
        } else {
            append(getString(R.string.ap_alarm_yok))
        }
        val tamIzin = runCatching { AlarmSagligi.tamAlarmIzniVar(this@SistemActivity) }
            .getOrDefault(true)
        if (!tamIzin) append("\n" + getString(R.string.ap_alarm_izin_yok))
        val pilKisitsiz = runCatching { AlarmSagligi.pilKisitsizMi(this@SistemActivity) }
            .getOrDefault(true)
        if (!pilKisitsiz) append("\n" + getString(R.string.ap_pil_kisitli))
    }

    private fun genisDugme() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = dp(8) }

    private fun cerceve(cizgi: Int, kalinlik: Int): MaterialCardView = MaterialCardView(this).apply {
        radius = 16 * yg()
        cardElevation = 0f
        strokeWidth = dp(kalinlik)
        strokeColor = cizgi
        setCardBackgroundColor(renk(com.google.android.material.R.attr.colorSurface))
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(8) }
    }

    private fun baslik(metin: String) {
        kok.addView(TextView(this).apply {
            text = metin
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
            setPadding(dp(4), dp(20), 0, dp(6))
        })
    }

    private fun bilgiKarti(metin: String, vurgulu: Boolean) {
        val kart = MaterialCardView(this).apply {
            radius = 16 * yg()
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
        kart.addView(TextView(this).apply {
            text = metin
            textSize = 13f
            setLineSpacing(0f, 1.35f)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setTextColor(
                if (vurgulu) renk(com.google.android.material.R.attr.colorOnPrimaryContainer)
                else renk(com.google.android.material.R.attr.colorOnSurface)
            )
        })
        kok.addView(kart)
    }

    private fun ayarSatiri(baslik: String, deger: String, tiklandi: () -> Unit) {
        val kart = MaterialCardView(this).apply {
            radius = 14 * yg()
            cardElevation = 0f
            strokeWidth = dp(1)
            strokeColor = renk(com.google.android.material.R.attr.colorOutlineVariant)
            setCardBackgroundColor(renk(com.google.android.material.R.attr.colorSurface))
            isClickable = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(6) }
            setOnClickListener { Titresim.dokunus(it); tiklandi() }
        }
        val satir = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        satir.addView(TextView(this).apply {
            text = baslik
            textSize = 13.5f
            maxLines = 2
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        })
        satir.addView(TextView(this).apply {
            text = deger
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
        })
        kart.addView(satir)
        kok.addView(kart)
    }
}
