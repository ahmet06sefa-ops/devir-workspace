package com.gunlukasistan.app

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.Calendar

/**
 * v9.7 — Günlük hayat merkezi (öneri 41-46).
 *
 * Altı özellik tek ekranda:
 *   💊 İlaç · 🧾 Fatura · 🪪 Belge · 🚗 Araç · 💰 Bütçe · 📍 Konum
 *
 * ══════════════════════════════════════════════════════════════════
 * NEDEN TEK EKRAN
 * ══════════════════════════════════════════════════════════════════
 * Altı ayrı ekran açmak altı ayrı menü girişi demekti. Ayarlar
 * listesi zaten uzun; kullanıcı hiçbirini bulamazdı.
 *
 * Bunun yerine üstte **sekme çubuğu** var. Aynı ekran, altı görünüm.
 * Uyarısı olan sekmeler kırmızı nokta gösteriyor — kullanıcı hangi
 * sekmeye bakması gerektiğini anında görüyor.
 */
class TakipActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SEKME = "sekme"

        const val S_UYARI = 0
        const val S_ILAC = 1
        const val S_FATURA = 2
        const val S_BELGE = 3
        const val S_ARAC = 4
        const val S_BUTCE = 5
        const val S_KONUM = 6

        fun ac(context: Context, sekme: Int = S_UYARI) {
            runCatching {
                context.startActivity(
                    Intent(context, TakipActivity::class.java).putExtra(EXTRA_SEKME, sekme)
                )
                (context as? android.app.Activity)?.let { Canlandir.activityGirisi(it) }
            }
        }
    }

    private lateinit var kok: LinearLayout
    private lateinit var sekmeSatiri: LinearLayout
    private var sekme = S_UYARI

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
        UstBar.kur(this, getString(R.string.gh_baslik))
        kok = findViewById(R.id.gaKok)

        sekme = intent?.getIntExtra(EXTRA_SEKME, S_UYARI) ?: S_UYARI
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
    // Çizim
    // ══════════════════════════════════════════════════════════

    private fun yukle() {
        kok.removeAllViews()
        sekmeleriCiz()

        when (sekme) {
            S_UYARI -> uyariSekmesi()
            S_ILAC -> turSekmesi(Takip.Tur.ILAC)
            S_FATURA -> turSekmesi(Takip.Tur.FATURA)
            S_BELGE -> turSekmesi(Takip.Tur.BELGE)
            S_ARAC -> aracSekmesi()
            S_BUTCE -> butceSekmesi()
            S_KONUM -> konumSekmesi()
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
        sekmeSatiri = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

        val acilSayi = runCatching { Takip.acilSayisi(this) }.getOrDefault(0)
        val etiketler = listOf(
            Triple(S_UYARI, "⚠️", getString(R.string.tk_s_uyari)),
            Triple(S_ILAC, "💊", getString(R.string.tk_s_ilac)),
            Triple(S_FATURA, "🧾", getString(R.string.tk_s_fatura)),
            Triple(S_BELGE, "🪪", getString(R.string.tk_s_belge)),
            Triple(S_ARAC, "🚗", getString(R.string.tk_s_arac)),
            Triple(S_BUTCE, "💰", getString(R.string.tk_s_butce)),
            Triple(S_KONUM, "📍", getString(R.string.tk_s_konum))
        )

        etiketler.forEach { (kod, emoji, ad) ->
            val secili = kod == sekme
            val rozet = if (kod == S_UYARI && acilSayi > 0) " ($acilSayi)" else ""
            val d = MaterialButton(
                this, null,
                if (secili) com.google.android.material.R.attr.materialButtonStyle
                else com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply {
                text = "$emoji $ad$rozet"
                textSize = 12f
                isAllCaps = false
                minWidth = 0
                minimumWidth = 0
                insetTop = 0
                insetBottom = 0
                setPadding(dp(14), dp(6), dp(14), dp(6))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = dp(6) }
                contentDescription = ad
                setOnClickListener {
                    Titresim.dokunus(it)
                    sekme = kod
                    yukle()
                }
            }
            sekmeSatiri.addView(d)
        }
        kaydir.addView(sekmeSatiri)
        kok.addView(kaydir)
    }

    // ══════════════════════════════════════════════════════════
    // Sekme: Uyarılar
    // ══════════════════════════════════════════════════════════

    private fun uyariSekmesi() {
        val uyarilar = runCatching { Takip.uyarilar(this) }.getOrDefault(emptyList())

        if (uyarilar.isEmpty()) {
            bilgiKarti(getString(R.string.tk_uyari_yok), false)
        } else {
            baslik(getString(R.string.tk_dikkat, uyarilar.size))
            uyarilar.forEach { u -> uyariKarti(u) }
        }

        // Konum hatırlatmaları da burada — "şu an yakınındakiler"
        val yakin = runCatching { KonumHatirlatma.yakindakiler(this, 1500) }
            .getOrDefault(emptyList())
        if (yakin.isNotEmpty()) {
            baslik(getString(R.string.kh_yakinda))
            yakin.take(3).forEach { t ->
                bilgiKarti(
                    "${t.yer.emoji} ${t.yer.ad} · ${mesafeMetni(t.mesafe)}\n${t.hatirlatma.metin}",
                    false
                )
            }
        }

        // Bütçe özeti
        val ozet = runCatching { Butce.ayOzeti(this) }.getOrDefault(null)
        if (ozet != null && ozet.kalemSayisi > 0) {
            baslik(getString(R.string.bt_bu_ay))
            bilgiKarti(
                getString(
                    R.string.bt_ozet_satir,
                    Takip.paraMetni(ozet.gider), Takip.paraMetni(ozet.gelir)
                ),
                ozet.bakiye < 0
            )
        }

        baslik(getString(R.string.tk_ayarlar))
        ayarSatiri(
            getString(R.string.tk_alarm_acik),
            if (TakipAlarm.acikMi(this)) getString(R.string.tk_acik) else getString(R.string.tk_kapali)
        ) {
            TakipAlarm.acikAyarla(this, !TakipAlarm.acikMi(this))
            yukle()
        }
        ayarSatiri(
            getString(R.string.tk_ozet_saati),
            Takip.saatMetni(TakipAlarm.ozetSaati(this))
        ) { saatSec(TakipAlarm.ozetSaati(this)) { d -> TakipAlarm.ozetSaatiAyarla(this, d); yukle() } }
    }

    private fun uyariKarti(u: Takip.Uyari) {
        val renkKodu = if (u.seviye == 2) GrafikDili.hata(this) else GrafikDili.uyari(this)
        val kart = cerceve(renkKodu)
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        val ust = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        ust.addView(TextView(this).apply {
            text = "${u.kayit.tur.emoji}  ${u.kayit.ad}"
            textSize = 15.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        })
        ust.addView(TextView(this).apply {
            text = u.mesaj
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(renkKodu)
        })
        ic.addView(ust)

        if (u.kayit.tutar > 0) {
            ic.addView(TextView(this).apply {
                text = Takip.paraMetni(u.kayit.tutar)
                textSize = 13f
                setPadding(0, dp(4), 0, 0)
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            })
        }

        val dugmeler = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, 0)
        }
        dugmeler.addView(MaterialButton(
            this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            setText(
                if (u.kayit.tur == Takip.Tur.ILAC) R.string.tk_aldim else R.string.tk_yapildi
            )
            textSize = 12f
            isAllCaps = false
            insetTop = 0; insetBottom = 0
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ).apply { marginEnd = dp(6) }
            setOnClickListener {
                Titresim.basari(this@TakipActivity)
                if (u.kayit.tur == Takip.Tur.ILAC && u.kayit.stok >= 0) {
                    Takip.dozAl(this@TakipActivity, u.kayit.id)
                } else {
                    Takip.tamamla(this@TakipActivity, u.kayit.id)
                }
                Bildir.basari(kok, getString(R.string.tk_kaydedildi))
                yukle()
            }
        })
        dugmeler.addView(MaterialButton(
            this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            setText(R.string.tk_duzenle)
            textSize = 12f
            isAllCaps = false
            insetTop = 0; insetBottom = 0
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
            setOnClickListener { Titresim.dokunus(it); kayitPenceresi(u.kayit.tur, u.kayit) }
        })
        ic.addView(dugmeler)
        kart.addView(ic)
        kok.addView(kart)
    }

    // ══════════════════════════════════════════════════════════
    // Sekme: İlaç / Fatura / Belge (tarih bazlı)
    // ══════════════════════════════════════════════════════════

    private fun turSekmesi(tur: Takip.Tur) {
        val kayitlar = runCatching { Takip.turdekiler(this, tur) }.getOrDefault(emptyList())

        if (tur == Takip.Tur.FATURA) {
            val aylik = runCatching { Takip.aylikYuk(this) }.getOrDefault(0.0)
            if (aylik > 0) {
                bilgiKarti(getString(R.string.tk_aylik_yuk, Takip.paraMetni(aylik)), true)
            }
            val buAy = runCatching { Takip.buAyOdenen(this) }.getOrDefault(0.0)
            if (buAy > 0) {
                bilgiKarti(getString(R.string.tk_bu_ay_odenen, Takip.paraMetni(buAy)), false)
            }
        }

        if (kayitlar.isEmpty()) {
            bilgiKarti(aciklamaMetni(tur), false)
        } else {
            kayitlar.sortedBy {
                if (it.kmBazli) Long.MAX_VALUE else
                    if (it.sonrakiMillis > 0) it.sonrakiMillis else Long.MAX_VALUE
            }.forEach { k -> kayitKarti(k) }
        }

        kok.addView(MaterialButton(this).apply {
            text = getString(R.string.tk_yeni_ekle, getString(tur.adRes))
            isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10) }
            setOnClickListener { Titresim.dokunus(it); kayitPenceresi(tur, null) }
        })

        // Arşiv
        val arsiv = runCatching { Takip.hepsi(this).filter { it.arsiv && it.tur == tur } }
            .getOrDefault(emptyList())
        if (arsiv.isNotEmpty()) {
            baslik(getString(R.string.tk_arsiv, arsiv.size))
            arsiv.take(8).forEach { k ->
                ayarSatiri("${k.tur.emoji} ${k.ad}", Takip.kisaTarih(k.sonrakiMillis)) {
                    AltSayfa.menu(this, k.ad, listOf(
                        AltSayfa.Oge(getString(R.string.tk_arsivden_cikar), simge = "↩️") {
                            Takip.arsivle(this, k.id, false); yukle()
                        },
                        AltSayfa.Oge(getString(R.string.tk_sil), simge = "🗑", yikici = true) {
                            Takip.sil(this, k.id); yukle()
                        }
                    ))
                }
            }
        }
    }

    private fun aciklamaMetni(tur: Takip.Tur): String = when (tur) {
        Takip.Tur.ILAC -> getString(R.string.tk_bos_ilac)
        Takip.Tur.FATURA -> getString(R.string.tk_bos_fatura)
        Takip.Tur.BELGE -> getString(R.string.tk_bos_belge)
        Takip.Tur.ARAC -> getString(R.string.tk_bos_arac)
    }

    private fun kayitKarti(k: Takip.Kayit) {
        val kalan = if (k.kmBazli) Int.MAX_VALUE else k.kalanGun()
        val stokGun = k.stokGun()
        val kritik = (kalan != Int.MAX_VALUE && kalan <= k.esik) ||
                (stokGun != null && stokGun <= 3)
        val kart = cerceve(
            if (kritik) GrafikDili.hata(this)
            else renk(com.google.android.material.R.attr.colorOutlineVariant)
        )
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }

        val ust = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        ust.addView(TextView(this).apply {
            text = "${k.tur.emoji}  ${k.ad}"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        })
        if (k.tutar > 0) {
            ust.addView(TextView(this).apply {
                text = Takip.paraMetni(k.tutar)
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
            })
        }
        ic.addView(ust)

        val detaylar = mutableListOf<String>()
        if (!k.kmBazli && k.sonrakiMillis > 0) {
            detaylar.add(
                Takip.tarihMetni(k.sonrakiMillis) + when {
                    kalan < 0 -> " · " + getString(R.string.tk_gecti, -kalan)
                    kalan == 0 -> " · " + getString(R.string.tk_bugun)
                    kalan <= 60 -> " · " + getString(R.string.tk_kalan_gun, kalan)
                    else -> ""
                }
            )
        }
        if (k.kmBazli && k.sonrakiKm > 0) {
            val mevcut = Takip.mevcutKm(this)
            detaylar.add(
                Takip.kmMetni(k.sonrakiKm) +
                        if (mevcut > 0) " · " + getString(
                            R.string.tk_kalan_km, k.sonrakiKm - mevcut
                        ) else ""
            )
        }
        if (k.tekrar != Takip.TEKRAR_YOK) detaylar.add(Takip.tekrarAdi(this, k.tekrar))
        if (k.stok >= 0) {
            detaylar.add(
                getString(R.string.tk_stok_adet, k.stok) +
                        (stokGun?.let { " · " + getString(R.string.tk_stok_gun, it) } ?: "")
            )
        }
        if (k.saatler.isNotEmpty()) {
            detaylar.add(k.saatler.sorted().joinToString(", ") { Takip.saatMetni(it) })
        }
        if (k.not.isNotBlank()) detaylar.add(k.not)

        detaylar.forEach { d ->
            ic.addView(TextView(this).apply {
                text = d
                textSize = 12.5f
                setPadding(0, dp(3), 0, 0)
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            })
        }

        val dugmeler = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, 0)
        }
        // İlaç: "Aldım" + "Kutu ekle" · diğerleri: "Yapıldı"
        if (k.tur == Takip.Tur.ILAC && k.stok >= 0) {
            dugmeler.addView(kucukDugme(getString(R.string.tk_aldim), 1f) {
                Takip.dozAl(this, k.id); yukle()
            })
            dugmeler.addView(kucukDugme(getString(R.string.tk_kutu_ekle), 1f) {
                sayiPenceresi(getString(R.string.tk_kutu_ekle), "") { s ->
                    s.toIntOrNull()?.let { Takip.stokEkle(this, k.id, it); yukle() }
                }
            })
        } else {
            dugmeler.addView(kucukDugme(getString(R.string.tk_yapildi), 1f) {
                Takip.tamamla(this, k.id)
                Bildir.basari(kok, getString(R.string.tk_kaydedildi))
                yukle()
            })
        }
        dugmeler.addView(kucukDugme("⋯", 0.5f) {
            AltSayfa.menu(this, k.ad, listOf(
                AltSayfa.Oge(getString(R.string.tk_duzenle), simge = "✏️") {
                    kayitPenceresi(k.tur, k)
                },
                AltSayfa.Oge(getString(R.string.tk_arsivle), simge = "📦") {
                    Takip.arsivle(this, k.id, true); yukle()
                },
                AltSayfa.Oge(getString(R.string.tk_sil), simge = "🗑", yikici = true) {
                    Takip.sil(this, k.id); yukle()
                }
            ))
        })
        ic.addView(dugmeler)
        kart.addView(ic)
        kok.addView(kart)
    }

    // ══════════════════════════════════════════════════════════
    // Sekme: Araç
    // ══════════════════════════════════════════════════════════

    private fun aracSekmesi() {
        val mevcut = Takip.mevcutKm(this)
        val kart = cerceve(renk(com.google.android.material.R.attr.colorPrimary))
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        ic.addView(TextView(this).apply {
            setText(R.string.tk_mevcut_km)
            textSize = 12f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
        })
        ic.addView(TextView(this).apply {
            text = if (mevcut > 0) Takip.kmMetni(mevcut) else getString(R.string.tk_km_girilmedi)
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
        })
        val kmZaman = Takip.kmZamani(this)
        if (kmZaman > 0) {
            ic.addView(TextView(this).apply {
                text = getString(R.string.tk_km_guncelleme, Takip.kisaTarih(kmZaman))
                textSize = 11.5f
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            })
        }
        ic.addView(MaterialButton(
            this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            setText(R.string.tk_km_guncelle)
            isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10) }
            setOnClickListener {
                Titresim.dokunus(it)
                sayiPenceresi(
                    getString(R.string.tk_km_guncelle),
                    if (mevcut > 0) mevcut.toString() else ""
                ) { s ->
                    s.toLongOrNull()?.let { km ->
                        // Geri gitme kontrolü: km sayacı azalmaz.
                        // Kullanıcı yanlış yazmış olabilir; uyarıyoruz
                        // ama engellemiyoruz (araç değişmiş olabilir).
                        //
                        // NOT: `this@TakipActivity` şart — bu blok
                        // MaterialButton.apply{} içinde, çıplak `this`
                        // düğmeye bağlanır ve Context beklenen yerde
                        // derleme hatası verir.
                        if (mevcut > 0 && km < mevcut) {
                            MaterialAlertDialogBuilder(this@TakipActivity)
                                .setTitle(R.string.tk_km_uyari_baslik)
                                .setMessage(getString(R.string.tk_km_uyari, mevcut, km))
                                .setPositiveButton(R.string.tk_yine_de) { _, _ ->
                                    Takip.kmGuncelle(this@TakipActivity, km); yukle()
                                }
                                .setNegativeButton(R.string.cancel, null)
                                .show()
                        } else {
                            Takip.kmGuncelle(this@TakipActivity, km); yukle()
                        }
                    }
                }
            }
        })
        kart.addView(ic)
        kok.addView(kart)

        val bakimlar = runCatching { Takip.turdekiler(this, Takip.Tur.ARAC) }
            .getOrDefault(emptyList())
        if (bakimlar.isEmpty()) {
            bilgiKarti(getString(R.string.tk_bos_arac), false)
        } else {
            baslik(getString(R.string.tk_bakimlar))
            bakimlar.sortedBy { it.sonrakiKm }.forEach { k -> bakimKarti(k, mevcut) }
        }

        kok.addView(MaterialButton(this).apply {
            text = getString(R.string.tk_yeni_ekle, getString(R.string.tk_tur_arac))
            isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10) }
            setOnClickListener { Titresim.dokunus(it); kayitPenceresi(Takip.Tur.ARAC, null) }
        })
    }

    private fun bakimKarti(k: Takip.Kayit, mevcutKm: Long) {
        val kalanKm = if (mevcutKm > 0 && k.sonrakiKm > 0) k.sonrakiKm - mevcutKm else Long.MAX_VALUE
        val kritik = kalanKm != Long.MAX_VALUE && kalanKm <= k.esik
        val kart = cerceve(
            if (kritik) GrafikDili.hata(this)
            else renk(com.google.android.material.R.attr.colorOutlineVariant)
        )
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        val ust = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        ust.addView(TextView(this).apply {
            text = "🔧  ${k.ad}"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        })
        if (kalanKm != Long.MAX_VALUE) {
            ust.addView(TextView(this).apply {
                text = if (kalanKm <= 0) getString(R.string.tk_km_gecti, -kalanKm)
                else getString(R.string.tk_kalan_km, kalanKm)
                textSize = 12.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(
                    if (kritik) GrafikDili.hata(this@TakipActivity)
                    else renk(com.google.android.material.R.attr.colorOnSurfaceVariant)
                )
            })
        }
        ic.addView(ust)
        ic.addView(TextView(this).apply {
            text = getString(R.string.tk_bakim_detay, Takip.kmMetni(k.sonrakiKm), k.tekrarKm)
            textSize = 12.5f
            setPadding(0, dp(3), 0, 0)
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
        })

        val dugmeler = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, 0)
        }
        dugmeler.addView(kucukDugme(getString(R.string.tk_bakim_yapildi), 1f) {
            if (mevcutKm <= 0) {
                Bildir.hata(kok, getString(R.string.tk_once_km))
            } else {
                Takip.bakimYapildi(this, k.id)
                Bildir.basari(kok, getString(R.string.tk_kaydedildi))
                yukle()
            }
        })
        dugmeler.addView(kucukDugme("⋯", 0.5f) {
            AltSayfa.menu(this, k.ad, listOf(
                AltSayfa.Oge(getString(R.string.tk_duzenle), simge = "✏️") {
                    kayitPenceresi(Takip.Tur.ARAC, k)
                },
                AltSayfa.Oge(getString(R.string.tk_sil), simge = "🗑", yikici = true) {
                    Takip.sil(this, k.id); yukle()
                }
            ))
        })
        ic.addView(dugmeler)
        kart.addView(ic)
        kok.addView(kart)
    }

    // ══════════════════════════════════════════════════════════
    // Sekme: Bütçe
    // ══════════════════════════════════════════════════════════

    private fun butceSekmesi() {
        val ozet = runCatching { Butce.ayOzeti(this) }.getOrDefault(null)

        // Özet kartı
        val kart = cerceve(renk(com.google.android.material.R.attr.colorPrimary))
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        ic.addView(TextView(this).apply {
            text = getString(R.string.bt_bu_ay)
            textSize = 12f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
        })
        ic.addView(TextView(this).apply {
            text = Takip.paraMetni(ozet?.gider ?: 0.0)
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
        })
        if (ozet != null && ozet.gelir > 0) {
            ic.addView(TextView(this).apply {
                text = getString(
                    R.string.bt_gelir_bakiye,
                    Takip.paraMetni(ozet.gelir),
                    Takip.paraMetni(ozet.bakiye)
                )
                textSize = 12.5f
                setTextColor(
                    if (ozet.bakiye < 0) GrafikDili.hata(this@TakipActivity)
                    else renk(com.google.android.material.R.attr.colorOnSurfaceVariant)
                )
            })
        }
        // Limit çubuğu
        Butce.limitDurumu(this)?.let { yuzde ->
            ic.addView(android.widget.ProgressBar(
                this, null, android.R.attr.progressBarStyleHorizontal
            ).apply {
                max = 100
                progress = yuzde.coerceIn(0, 100)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(8)
                ).apply { topMargin = dp(10) }
                progressTintList = android.content.res.ColorStateList.valueOf(
                    when {
                        yuzde >= 100 -> GrafikDili.hata(this@TakipActivity)
                        yuzde >= 80 -> GrafikDili.uyari(this@TakipActivity)
                        else -> GrafikDili.basari(this@TakipActivity)
                    }
                )
            })
            ic.addView(TextView(this).apply {
                text = getString(
                    R.string.bt_limit_durum, yuzde, Takip.paraMetni(Butce.aylikLimit(this@TakipActivity))
                )
                textSize = 11.5f
                setPadding(0, dp(4), 0, 0)
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            })
        }
        kart.addView(ic)
        kok.addView(kart)

        // Hızlı ekleme
        val hizli = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        }
        hizli.addView(MaterialButton(this).apply {
            setText(R.string.bt_gider_ekle)
            isAllCaps = false
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ).apply { marginEnd = dp(6) }
            setOnClickListener { Titresim.dokunus(it); kalemPenceresi(false) }
        })
        hizli.addView(MaterialButton(
            this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            setText(R.string.bt_gelir_ekle)
            isAllCaps = false
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
            setOnClickListener { Titresim.dokunus(it); kalemPenceresi(true) }
        })
        kok.addView(hizli)

        // Çıkarımlar
        val cikarimlar = runCatching { Butce.cikarimlar(this) }.getOrDefault(emptyList())
        cikarimlar.forEach { c -> bilgiKarti(c, false) }

        // Kategori dağılımı
        val dagilim = runCatching { Butce.kategoriDagilimi(this) }.getOrDefault(emptyList())
        if (dagilim.isNotEmpty()) {
            baslik(getString(R.string.bt_dagilim))
            kok.addView(DagilimHalkasi(this).apply {
                birim = "₺"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(210)
                )
                ayarla(dagilim.map {
                    DagilimHalkasi.Dilim(
                        getString(it.kategori.adRes), it.toplam.toInt(), it.kategori.renk
                    )
                })
            })
            dagilim.take(6).forEach { pay ->
                ayarSatiri(
                    "${pay.kategori.emoji} ${getString(pay.kategori.adRes)}",
                    "${Takip.paraMetni(pay.toplam)} · %${pay.yuzde}"
                ) {}
            }
        }

        // Aylık eğilim
        val aylar = runCatching { Butce.sonAylar(this, 6) }.getOrDefault(emptyList())
        if (aylar.count { it.kalemSayisi > 0 } >= 2) {
            baslik(getString(R.string.bt_aylik_egilim))
            aylikCubuklar(aylar)
        }

        // Son kalemler
        val kalemler = runCatching { Butce.hepsi(this).take(12) }.getOrDefault(emptyList())
        if (kalemler.isNotEmpty()) {
            baslik(getString(R.string.bt_son_kalemler))
            kalemler.forEach { k -> kalemSatiri(k) }
        }

        baslik(getString(R.string.tk_ayarlar))
        ayarSatiri(
            getString(R.string.bt_aylik_limit),
            Butce.aylikLimit(this).let { if (it > 0) Takip.paraMetni(it) else getString(R.string.gh_yok) }
        ) {
            sayiPenceresi(
                getString(R.string.bt_aylik_limit),
                Butce.aylikLimit(this).takeIf { it > 0 }?.toInt()?.toString() ?: "",
                ondalik = true
            ) { s ->
                Butce.aylikLimitAyarla(this, s.replace(',', '.').toDoubleOrNull() ?: 0.0)
                yukle()
            }
        }
    }

    /**
     * Aylık gelir/gider çubukları.
     *
     * `View` yerine `TextView` kullanıyorum — RemoteViews tuzağı
     * burada geçerli değil (bu bir Activity), ama `LinearLayout`
     * ağırlığıyla çizim yapmak özel View yazmaktan basit ve tema
     * renklerini otomatik alıyor.
     */
    private fun aylikCubuklar(aylar: List<Butce.AyOzet>) {
        val enBuyuk = aylar.maxOfOrNull { maxOf(it.gelir, it.gider) } ?: 0.0
        if (enBuyuk <= 0) return

        val satir = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(150)
            )
        }
        aylar.forEach { ay ->
            val sutun = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.MATCH_PARENT, 1f
                )
            }
            val ikili = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
                )
            }
            fun cubuk(deger: Double, renkKodu: Int) {
                val yukseklik = ((deger / enBuyuk) * dp(105)).toInt().coerceAtLeast(if (deger > 0) dp(3) else 1)
                ikili.addView(View(this).apply {
                    setBackgroundColor(renkKodu)
                    layoutParams = LinearLayout.LayoutParams(dp(11), yukseklik).apply {
                        marginEnd = dp(3)
                    }
                })
            }
            cubuk(ay.gelir, GrafikDili.basari(this))
            cubuk(ay.gider, GrafikDili.hata(this))
            sutun.addView(ikili)
            sutun.addView(TextView(this).apply {
                text = ay.etiket
                textSize = 10.5f
                gravity = Gravity.CENTER
                setPadding(0, dp(5), 0, 0)
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            })
            satir.addView(sutun)
        }
        kok.addView(satir)
        kok.addView(TextView(this).apply {
            setText(R.string.bt_cubuk_aciklama)
            textSize = 11f
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(4))
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
        })
    }

    private fun kalemSatiri(k: Butce.Kalem) {
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
            setOnLongClickListener {
                Titresim.uzunBasma(it)
                AltSayfa.menu(this@TakipActivity, k.aciklama.ifBlank {
                    getString(k.kategori.adRes)
                }, listOf(
                    AltSayfa.Oge(getString(R.string.tk_sil), simge = "🗑", yikici = true) {
                        Butce.sil(this@TakipActivity, k.id); yukle()
                    }
                ))
                true
            }
        }
        val satir = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
        }
        satir.addView(TextView(this).apply {
            text = k.kategori.emoji
            textSize = 17f
            setPadding(0, 0, dp(10), 0)
        })
        val orta = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        }
        orta.addView(TextView(this).apply {
            text = k.aciklama.ifBlank { getString(k.kategori.adRes) }
            textSize = 13.5f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
        })
        orta.addView(TextView(this).apply {
            text = Takip.kisaTarih(k.millis)
            textSize = 11f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
        })
        satir.addView(orta)
        satir.addView(TextView(this).apply {
            text = (if (k.gelir) "+" else "−") + Takip.paraMetni(k.tutar)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (k.gelir) GrafikDili.basari(this@TakipActivity) else GrafikDili.hata(this@TakipActivity))
        })
        kart.addView(satir)
        kok.addView(kart)
    }

    // ══════════════════════════════════════════════════════════
    // Sekme: Konum
    // ══════════════════════════════════════════════════════════

    private fun konumSekmesi() {
        bilgiKarti(getString(R.string.kh_aciklama), false)

        if (!KonumHatirlatma.izinVarMi(this)) {
            bilgiKarti(getString(R.string.kh_izin_gerek), true)
            kok.addView(MaterialButton(this).apply {
                setText(R.string.kh_izin_ver)
                isAllCaps = false
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener { Titresim.dokunus(it); izinIste() }
            })
            return
        }

        val konum = KonumHatirlatma.sonKonum(this)
        if (konum == null) {
            bilgiKarti(getString(R.string.kh_konum_yok), true)
        } else {
            val yas = KonumHatirlatma.konumYasiDk(konum) ?: 0
            bilgiKarti(
                getString(
                    R.string.kh_mevcut_konum,
                    String.format(java.util.Locale.US, "%.4f, %.4f", konum.latitude, konum.longitude),
                    yas
                ),
                false
            )
        }

        // Yerler
        val yerler = runCatching { KonumHatirlatma.yerler(this) }.getOrDefault(emptyList())
        baslik(getString(R.string.kh_yerler))
        if (yerler.isEmpty()) {
            bilgiKarti(getString(R.string.kh_yer_yok), false)
        } else {
            yerler.forEach { y ->
                val d = konum?.let {
                    KonumHatirlatma.mesafe(it.latitude, it.longitude, y.enlem, y.boylam)
                }
                ayarSatiri(
                    "${y.emoji} ${y.ad}",
                    if (d != null) mesafeMetni(d) else "${y.yaricap} m"
                ) {
                    AltSayfa.menu(this, y.ad, listOf(
                        AltSayfa.Oge(getString(R.string.kh_hatirlatma_ekle), simge = "🔔") {
                            hatirlatmaPenceresi(y)
                        },
                        AltSayfa.Oge(getString(R.string.tk_sil), simge = "🗑", yikici = true) {
                            KonumHatirlatma.yerSil(this, y.id); yukle()
                        }
                    ))
                }
            }
        }
        kok.addView(MaterialButton(this).apply {
            setText(R.string.kh_yer_ekle)
            isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
            setOnClickListener { Titresim.dokunus(it); yerPenceresi(konum) }
        })

        // Hatırlatmalar
        val hatirlatmalar = runCatching { KonumHatirlatma.hatirlatmalar(this) }
            .getOrDefault(emptyList())
        if (hatirlatmalar.isNotEmpty()) {
            baslik(getString(R.string.kh_hatirlatmalar))
            val yerHarita = yerler.associateBy { it.id }
            hatirlatmalar.forEach { h ->
                val yer = yerHarita[h.yerId]
                ayarSatiri(
                    h.metin,
                    (yer?.let { "${it.emoji} ${it.ad}" } ?: "?") + " · " +
                            getString(if (h.varista) R.string.kh_varista else R.string.kh_ayrilinca)
                ) {
                    AltSayfa.menu(this, h.metin, listOf(
                        AltSayfa.Oge(
                            if (h.aktif) getString(R.string.kh_duraklat)
                            else getString(R.string.kh_devam), simge = if (h.aktif) "⏸" else "▶️"
                        ) {
                            KonumHatirlatma.hatirlatmaEkle(this, h.copy(aktif = !h.aktif)); yukle()
                        },
                        AltSayfa.Oge(getString(R.string.tk_sil), simge = "🗑", yikici = true) {
                            KonumHatirlatma.hatirlatmaSil(this, h.id); yukle()
                        }
                    ))
                }
            }
        }
    }

    private fun izinIste() {
        runCatching {
            androidx.core.app.ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ),
                4501
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 4501) yukle()
    }

    private fun mesafeMetni(metre: Float): String =
        if (metre < 1000) getString(R.string.kh_metre, metre.toInt())
        else getString(R.string.kh_km, String.format(java.util.Locale.US, "%.1f", metre / 1000f))

    // ══════════════════════════════════════════════════════════
    // Pencereler
    // ══════════════════════════════════════════════════════════

    /**
     * Kayıt ekleme/düzenleme.
     *
     * Tek pencere dört türe hizmet ediyor; alanlar türe göre
     * gösterilip gizleniyor. Dört ayrı pencere yazmak dört kez
     * aynı doğrulama kodunu yazmak olurdu.
     */
    private fun kayitPenceresi(tur: Takip.Tur, mevcut: Takip.Kayit?) {
        val kapsayici = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(8), dp(22), dp(4))
        }

        val adGiris = metinAlani(getString(R.string.tk_ad), mevcut?.ad ?: "")
        kapsayici.addView(adGiris.first)

        // Tarih (km bazlı olmayanlar)
        var seciliTarih = mevcut?.sonrakiMillis ?: 0L
        val tarihDugme = MaterialButton(
            this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            isAllCaps = false
            text = if (seciliTarih > 0) Takip.tarihMetni(seciliTarih)
            else getString(R.string.tk_tarih_sec)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(6) }
            setOnClickListener {
                tarihSec(seciliTarih) { t -> seciliTarih = t; text = Takip.tarihMetni(t) }
            }
        }
        if (!tur.kmBazli) kapsayici.addView(tarihDugme)

        // Km alanları
        val kmGiris = sayiAlani(getString(R.string.tk_sonraki_km), mevcut?.sonrakiKm?.takeIf { it > 0 }?.toString() ?: "")
        val kmAralikGiris = sayiAlani(
            getString(R.string.tk_bakim_araligi),
            mevcut?.tekrarKm?.takeIf { it > 0 }?.toString() ?: "10000"
        )
        if (tur.kmBazli) {
            kapsayici.addView(kmGiris.first)
            kapsayici.addView(kmAralikGiris.first)
        }

        // Tekrar
        var seciliTekrar = mevcut?.tekrar ?: when (tur) {
            Takip.Tur.FATURA -> Takip.TEKRAR_AY
            Takip.Tur.ILAC -> Takip.TEKRAR_YOK
            else -> Takip.TEKRAR_YOK
        }
        val tekrarSecenekleri = intArrayOf(
            Takip.TEKRAR_YOK, 1, 7, 15, Takip.TEKRAR_AY, -2, Takip.TEKRAR_UC_AY,
            -6, Takip.TEKRAR_YIL, -24
        )
        val tekrarDugme = MaterialButton(
            this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            isAllCaps = false
            text = getString(R.string.tk_tekrar_etiket, Takip.tekrarAdi(this@TakipActivity, seciliTekrar))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(6) }
            setOnClickListener {
                AltSayfa.secim(
                    this@TakipActivity, getString(R.string.tk_tekrar),
                    tekrarSecenekleri.map { Takip.tekrarAdi(this@TakipActivity, it) }.toTypedArray()
                ) { i ->
                    seciliTekrar = tekrarSecenekleri[i]
                    text = getString(
                        R.string.tk_tekrar_etiket,
                        Takip.tekrarAdi(this@TakipActivity, seciliTekrar)
                    )
                }
            }
        }
        if (!tur.kmBazli) kapsayici.addView(tekrarDugme)

        // Tutar (fatura)
        val tutarGiris = sayiAlani(
            getString(R.string.tk_tutar),
            mevcut?.tutar?.takeIf { it > 0 }?.let { it.toInt().toString() } ?: "",
            ondalik = true
        )
        if (tur == Takip.Tur.FATURA) kapsayici.addView(tutarGiris.first)

        // İlaç alanları
        val stokGiris = sayiAlani(
            getString(R.string.tk_stok),
            mevcut?.stok?.takeIf { it >= 0 }?.toString() ?: ""
        )
        val dozGiris = sayiAlani(
            getString(R.string.tk_gunluk_doz),
            mevcut?.gunlukDoz?.takeIf { it > 0 }?.toString() ?: ""
        )
        val saatler = (mevcut?.saatler ?: emptyList()).toMutableList()
        val saatDugme = MaterialButton(
            this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            isAllCaps = false
            text = saatMetniListe(saatler)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(6) }
            setOnClickListener {
                AltSayfa.menu(
                    this@TakipActivity, getString(R.string.tk_saatler),
                    listOf(
                        AltSayfa.Oge(getString(R.string.tk_saat_ekle), simge = "➕") {
                            saatSec(8 * 60) { d ->
                                if (d !in saatler) saatler.add(d)
                                text = saatMetniListe(saatler)
                            }
                        },
                        AltSayfa.Oge(
                            getString(R.string.tk_saat_temizle), simge = "🧹",
                            etkin = saatler.isNotEmpty()
                        ) {
                            saatler.clear()
                            text = saatMetniListe(saatler)
                        }
                    )
                )
            }
        }
        if (tur == Takip.Tur.ILAC) {
            kapsayici.addView(stokGiris.first)
            kapsayici.addView(dozGiris.first)
            kapsayici.addView(saatDugme)
        }

        // Eşik
        val esikGiris = sayiAlani(
            if (tur.kmBazli) getString(R.string.tk_esik_km) else getString(R.string.tk_esik_gun),
            (mevcut?.esik ?: tur.varsayilanEsik).toString()
        )
        kapsayici.addView(esikGiris.first)

        val notGiris = metinAlani(getString(R.string.tk_not), mevcut?.not ?: "")
        kapsayici.addView(notGiris.first)

        val kaydirici = android.widget.ScrollView(this).apply { addView(kapsayici) }

        MaterialAlertDialogBuilder(this)
            .setTitle(
                if (mevcut == null) getString(R.string.tk_yeni_ekle, getString(tur.adRes))
                else getString(R.string.tk_duzenle)
            )
            .setView(kaydirici)
            .setPositiveButton(R.string.save) { _, _ ->
                val ad = adGiris.second.text?.toString()?.trim().orEmpty()
                if (ad.isEmpty()) {
                    Bildir.hata(kok, getString(R.string.tk_ad_gerek))
                    return@setPositiveButton
                }
                val kayit = Takip.Kayit(
                    id = mevcut?.id ?: System.currentTimeMillis(),
                    tur = tur,
                    ad = ad,
                    sonrakiMillis = if (tur.kmBazli) 0L else seciliTarih,
                    sonrakiKm = if (tur.kmBazli)
                        (kmGiris.second.text?.toString()?.toLongOrNull() ?: 0L) else 0L,
                    tekrar = if (tur.kmBazli) Takip.TEKRAR_YOK else seciliTekrar,
                    tekrarKm = if (tur.kmBazli)
                        (kmAralikGiris.second.text?.toString()?.toLongOrNull() ?: 10000L) else 0L,
                    esik = esikGiris.second.text?.toString()?.toIntOrNull() ?: tur.varsayilanEsik,
                    tutar = if (tur == Takip.Tur.FATURA)
                        (tutarGiris.second.text?.toString()?.replace(',', '.')?.toDoubleOrNull() ?: 0.0)
                    else mevcut?.tutar ?: 0.0,
                    stok = if (tur == Takip.Tur.ILAC)
                        (stokGiris.second.text?.toString()?.toIntOrNull() ?: -1) else -1,
                    gunlukDoz = if (tur == Takip.Tur.ILAC)
                        (dozGiris.second.text?.toString()?.toIntOrNull() ?: 0) else 0,
                    saatler = if (tur == Takip.Tur.ILAC) saatler.sorted() else emptyList(),
                    not = notGiris.second.text?.toString()?.trim().orEmpty(),
                    arsiv = false,
                    olusturma = mevcut?.olusturma ?: System.currentTimeMillis()
                )
                Takip.kaydet(this, kayit)
                Bildir.basari(kok, getString(R.string.tk_kaydedildi))
                yukle()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun saatMetniListe(saatler: List<Int>): String =
        if (saatler.isEmpty()) getString(R.string.tk_saat_sec)
        else saatler.sorted().joinToString(", ") { Takip.saatMetni(it) }

    private fun kalemPenceresi(gelir: Boolean) {
        val kapsayici = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(8), dp(22), dp(4))
        }
        val tutarGiris = sayiAlani(getString(R.string.bt_tutar), "", ondalik = true)
        kapsayici.addView(tutarGiris.first)
        val aciklamaGiris = metinAlani(getString(R.string.bt_aciklama), "")
        kapsayici.addView(aciklamaGiris.first)

        val secenekler = if (gelir) Butce.Kategori.gelirler else Butce.Kategori.giderler
        var seciliKategori = secenekler.first()
        val katDugme = MaterialButton(
            this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            isAllCaps = false
            text = "${seciliKategori.emoji} ${getString(seciliKategori.adRes)}"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(6) }
            setOnClickListener {
                AltSayfa.secim(
                    this@TakipActivity, getString(R.string.bt_kategori),
                    secenekler.map { "${it.emoji} ${getString(it.adRes)}" }.toTypedArray()
                ) { i ->
                    seciliKategori = secenekler[i]
                    text = "${seciliKategori.emoji} ${getString(seciliKategori.adRes)}"
                }
            }
        }
        kapsayici.addView(katDugme)

        var seciliTarih = System.currentTimeMillis()
        kapsayici.addView(MaterialButton(
            this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            isAllCaps = false
            text = Takip.tarihMetni(seciliTarih)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(6) }
            setOnClickListener {
                tarihSec(seciliTarih) { t -> seciliTarih = t; text = Takip.tarihMetni(t) }
            }
        })

        MaterialAlertDialogBuilder(this)
            .setTitle(if (gelir) R.string.bt_gelir_ekle else R.string.bt_gider_ekle)
            .setView(android.widget.ScrollView(this).apply { addView(kapsayici) })
            .setPositiveButton(R.string.save) { _, _ ->
                val tutar = tutarGiris.second.text?.toString()?.replace(',', '.')?.toDoubleOrNull()
                if (tutar == null || tutar <= 0) {
                    Bildir.hata(kok, getString(R.string.bt_tutar_gerek))
                    return@setPositiveButton
                }
                Butce.ekle(
                    this,
                    Butce.Kalem(
                        id = System.currentTimeMillis(),
                        tutar = tutar,
                        kategori = seciliKategori,
                        aciklama = aciklamaGiris.second.text?.toString()?.trim().orEmpty(),
                        millis = seciliTarih,
                        gelir = gelir
                    )
                )
                Titresim.basari(this@TakipActivity)
                Bildir.basari(kok, getString(R.string.tk_kaydedildi))
                yukle()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun yerPenceresi(konum: android.location.Location?) {
        val kapsayici = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(8), dp(22), dp(4))
        }
        val adGiris = metinAlani(getString(R.string.kh_yer_adi), "")
        kapsayici.addView(adGiris.first)
        val latGiris = sayiAlani(
            getString(R.string.kh_enlem),
            konum?.latitude?.let { String.format(java.util.Locale.US, "%.6f", it) } ?: "",
            ondalik = true
        )
        val lonGiris = sayiAlani(
            getString(R.string.kh_boylam),
            konum?.longitude?.let { String.format(java.util.Locale.US, "%.6f", it) } ?: "",
            ondalik = true
        )
        kapsayici.addView(latGiris.first)
        kapsayici.addView(lonGiris.first)
        val yaricapGiris = sayiAlani(getString(R.string.kh_yaricap), "200")
        kapsayici.addView(yaricapGiris.first)

        if (konum != null) {
            kapsayici.addView(TextView(this).apply {
                setText(R.string.kh_konum_dolduruldu)
                textSize = 11.5f
                setPadding(0, dp(6), 0, 0)
                setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            })
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.kh_yer_ekle)
            .setView(android.widget.ScrollView(this).apply { addView(kapsayici) })
            .setPositiveButton(R.string.save) { _, _ ->
                val ad = adGiris.second.text?.toString()?.trim().orEmpty()
                val lat = latGiris.second.text?.toString()?.replace(',', '.')?.toDoubleOrNull()
                val lon = lonGiris.second.text?.toString()?.replace(',', '.')?.toDoubleOrNull()
                if (ad.isEmpty() || lat == null || lon == null) {
                    Bildir.hata(kok, getString(R.string.kh_eksik))
                    return@setPositiveButton
                }
                // Geçerli koordinat aralığı — yanlış girdi sessizce
                // kabul edilirse mesafe hesabı saçmalar
                if (lat !in -90.0..90.0 || lon !in -180.0..180.0) {
                    Bildir.hata(kok, getString(R.string.kh_gecersiz_koordinat))
                    return@setPositiveButton
                }
                KonumHatirlatma.yerEkle(
                    this,
                    KonumHatirlatma.Yer(
                        System.currentTimeMillis(), ad, lat, lon,
                        yaricapGiris.second.text?.toString()?.toIntOrNull()?.coerceIn(50, 5000) ?: 200
                    )
                )
                yukle()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun hatirlatmaPenceresi(yer: KonumHatirlatma.Yer) {
        val kapsayici = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(8), dp(22), dp(4))
        }
        val metinGiris = metinAlani(getString(R.string.kh_hatirlatma_metni), "")
        kapsayici.addView(metinGiris.first)
        var varista = true
        kapsayici.addView(MaterialButton(
            this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            isAllCaps = false
            text = getString(R.string.kh_varista)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(6) }
            setOnClickListener {
                varista = !varista
                text = getString(if (varista) R.string.kh_varista else R.string.kh_ayrilinca)
            }
        })

        MaterialAlertDialogBuilder(this)
            .setTitle("${yer.emoji} ${yer.ad}")
            .setView(kapsayici)
            .setPositiveButton(R.string.save) { _, _ ->
                val metin = metinGiris.second.text?.toString()?.trim().orEmpty()
                if (metin.isEmpty()) {
                    Bildir.hata(kok, getString(R.string.kh_eksik))
                    return@setPositiveButton
                }
                KonumHatirlatma.hatirlatmaEkle(
                    this,
                    KonumHatirlatma.Hatirlatma(
                        System.currentTimeMillis(), yer.id, metin, true, varista
                    )
                )
                yukle()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ══════════════════════════════════════════════════════════
    // Ortak giriş bileşenleri
    // ══════════════════════════════════════════════════════════

    private fun metinAlani(etiket: String, deger: String): Pair<View, TextInputEditText> {
        val alan = TextInputLayout(
            this, null,
            com.google.android.material.R.attr.textInputOutlinedStyle
        ).apply {
            hint = etiket
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(6) }
        }
        val giris = TextInputEditText(alan.context).apply {
            setText(deger)
            maxLines = 2
            setSingleLine(false)
        }
        alan.addView(giris)
        return alan to giris
    }

    private fun sayiAlani(
        etiket: String, deger: String, ondalik: Boolean = false
    ): Pair<View, TextInputEditText> {
        val alan = TextInputLayout(
            this, null,
            com.google.android.material.R.attr.textInputOutlinedStyle
        ).apply {
            hint = etiket
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(6) }
        }
        val giris = TextInputEditText(alan.context).apply {
            setText(deger)
            inputType = if (ondalik)
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            else InputType.TYPE_CLASS_NUMBER
            setSingleLine(true)
        }
        alan.addView(giris)
        return alan to giris
    }

    private fun sayiPenceresi(
        baslik: String, mevcut: String, ondalik: Boolean = false, tamam: (String) -> Unit
    ) {
        val (kapsayici, giris) = sayiAlani(baslik, mevcut, ondalik)
        val sar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(8), dp(22), dp(4))
            addView(kapsayici)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(baslik)
            .setView(sar)
            .setPositiveButton(R.string.save) { _, _ ->
                tamam(giris.text?.toString()?.trim().orEmpty())
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun tarihSec(mevcut: Long, secildi: (Long) -> Unit) {
        val c = Calendar.getInstance().apply {
            if (mevcut > 0) timeInMillis = mevcut
        }
        runCatching {
            DatePickerDialog(
                this,
                { _, yil, ay, gun ->
                    val secim = Calendar.getInstance().apply {
                        set(yil, ay, gun, 9, 0, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    secildi(secim.timeInMillis)
                },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun saatSec(mevcutDakika: Int, secildi: (Int) -> Unit) {
        runCatching {
            TimePickerDialog(
                this,
                { _, saat, dakika -> secildi(saat * 60 + dakika) },
                mevcutDakika / 60, mevcutDakika % 60, true
            ).show()
        }
    }

    // ══════════════════════════════════════════════════════════
    // Ortak görünümler
    // ══════════════════════════════════════════════════════════

    private fun cerceve(cizgiRengi: Int): MaterialCardView = MaterialCardView(this).apply {
        radius = 16 * yg()
        cardElevation = 0f
        strokeWidth = dp(if (cizgiRengi == renk(com.google.android.material.R.attr.colorOutlineVariant)) 1 else 2)
        strokeColor = cizgiRengi
        setCardBackgroundColor(renk(com.google.android.material.R.attr.colorSurface))
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(8) }
    }

    private fun kucukDugme(metin: String, agirlik: Float, tiklandi: () -> Unit): MaterialButton =
        MaterialButton(
            this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = metin
            textSize = 12f
            isAllCaps = false
            insetTop = 0; insetBottom = 0
            minWidth = 0; minimumWidth = 0
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, agirlik
            ).apply { marginEnd = dp(6) }
            setOnClickListener { Titresim.dokunus(it); tiklandi() }
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
            textSize = 13.5f
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
