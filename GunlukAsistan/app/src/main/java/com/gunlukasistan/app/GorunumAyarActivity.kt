package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch

/**
 * v8.2 — Görünüm ve hareket ayarları (Grup A ayar ekranı).
 *
 * ── Neden ayrı ekran ──
 * Grup A yedi ayrı davranış getirdi (animasyon, haptic, kaydırma,
 * liste girişi, sayı sayacı...). Bunları Ayarlar listesine tek tek
 * satır olarak koysak liste 7 satır uzardı ve zaten uzun olan ekran
 * daha da karışırdı. Tek bir "Görünüm ve hareket" satırı, altında
 * gruplu kartlar.
 *
 * ── Neden hepsi varsayılan AÇIK ──
 * Yeni özellik kapalı gelirse kimse fark etmez. Rahatsız eden
 * kapatır; bu ekran tam olarak bunun için var.
 *
 * ── Sistem ayarına saygı ──
 * Telefonun kendi "animasyonları kaldır" ayarı açıksa üstte bir uyarı
 * kartı çıkıyor ve animasyon anahtarı devre dışı görünüyor — kullanıcı
 * "açtım ama çalışmıyor" diye kafa yormasın.
 */
class GorunumAyarActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            context.startActivity(Intent(context, GorunumAyarActivity::class.java))
            (context as? android.app.Activity)?.let { Canlandir.activityGirisi(it) }
        }
    }

    private lateinit var kok: LinearLayout

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
        setContentView(R.layout.activity_gorunum_ayar)

        // v10.0 · Görsel öneri 4: ortak üst bar.
        // Düğme boyutu, yazı boyutu ve dokunma hedefi tek
        // yerden geliyor — ekranlar arası geçerken başlık
        // artık zıplamıyor.
        UstBar.kur(this, getString(R.string.gr_row))

        kok = findViewById(R.id.gaKok)
        ciz()
    }

    override fun finish() {
        super.finish()
        Canlandir.activityCikisi(this)
    }

    private fun ciz() {
        kok.removeAllViews()

        // Sistem uyarısı
        if (GorunumAyar.hareketAzalt(this)) {
            uyariKarti(getString(R.string.gr_sistem_uyari))
        }

        // v10.53: 32 Maddelik Tasarım ve Yerleşim Özelleştirme Atölyesi (#1..#32)
        grup(getString(R.string.ta_setting_satir)) {
            tiklanabilir(
                R.string.ta_setting_satir,
                R.string.ta_setting_alt
            ) { TasarimAtolyeActivity.ac(this@GorunumAyarActivity) }
        }

        grup(getString(R.string.gr_g_hareket)) {
            anahtar(
                R.string.gr_animasyon, R.string.gr_animasyon_sub,
                GorunumAyar.animasyonTercihi(this@GorunumAyarActivity)
            ) { acik ->
                GorunumAyar.animasyonAcik(this@GorunumAyarActivity, acik)
                ciz() // alt anahtarların etkinliği değişir
            }
            anahtar(
                R.string.gr_liste_anim, R.string.gr_liste_anim_sub,
                GorunumAyar.listeAnimasyonu(this@GorunumAyarActivity),
                etkin = GorunumAyar.animasyonAcik(this@GorunumAyarActivity)
            ) { acik -> GorunumAyar.listeAnimasyonu(this@GorunumAyarActivity, acik) }
            anahtar(
                R.string.gr_sayi_anim, R.string.gr_sayi_anim_sub,
                GorunumAyar.sayiAnimasyonu(this@GorunumAyarActivity),
                etkin = GorunumAyar.animasyonAcik(this@GorunumAyarActivity)
            ) { acik -> GorunumAyar.sayiAnimasyonu(this@GorunumAyarActivity, acik) }
        }

        grup(getString(R.string.gr_g_dokunma)) {
            anahtar(
                R.string.gr_haptik, R.string.gr_haptik_sub,
                GorunumAyar.haptikAcik(this@GorunumAyarActivity)
            ) { acik ->
                GorunumAyar.haptikAcik(this@GorunumAyarActivity, acik)
                // Açarken hemen örnek versin — ayar çalışıyor mu belli olsun
                if (acik) Titresim.basari(this@GorunumAyarActivity)
            }
            anahtar(
                R.string.gr_kaydirma, R.string.gr_kaydirma_sub,
                GorunumAyar.kaydirmaJesti(this@GorunumAyarActivity)
            ) { acik -> GorunumAyar.kaydirmaJesti(this@GorunumAyarActivity, acik) }
        }

        // v8.6 · Öneri 27: yazı boyutu ve yoğunluk
        grup(getString(R.string.gr_g_okuma)) {
            secici(
                R.string.gr_yazi, R.string.gr_yazi_sub,
                arrayOf(
                    getString(R.string.gr_yazi_kucuk),
                    getString(R.string.gr_yazi_normal),
                    getString(R.string.gr_yazi_buyuk),
                    getString(R.string.gr_yazi_cok)
                ),
                GorunumAyar.yaziOlcek(this@GorunumAyarActivity)
            ) { secim ->
                GorunumAyar.yaziOlcek(this@GorunumAyarActivity, secim)
                Titresim.dokunus(kok)
                // Yazı ölçeği attachBaseContext'te uygulanıyor;
                // görmek için Activity'nin yeniden doğması gerekiyor.
                recreate()
            }
            secici(
                R.string.gr_yogunluk, R.string.gr_yogunluk_sub,
                arrayOf(
                    getString(R.string.gr_yogunluk_siki),
                    getString(R.string.gr_yogunluk_normal),
                    getString(R.string.gr_yogunluk_rahat)
                ),
                GorunumAyar.yogunluk(this@GorunumAyarActivity)
            ) { secim ->
                GorunumAyar.yogunluk(this@GorunumAyarActivity, secim)
                Titresim.dokunus(kok)
            }
        }

        // 1. #2 Yoğunluk / Zihinsel Ferahlık Seçici
        grup(getString(R.string.gr_g_ferahlik)) {
            secici(
                R.string.gr_kart_modu, R.string.gr_kart_modu_sub,
                arrayOf(getString(R.string.gr_km_tam), getString(R.string.gr_km_kompakt), getString(R.string.gr_km_satir)),
                GorunumAyar.kartModu(this@GorunumAyarActivity)
            ) { m ->
                GorunumAyar.setKartModu(this@GorunumAyarActivity, m)
                Titresim.dokunus(kok)
            }
        }

        // 2. #3 Öncelik Vurgu Seviyesi
        grup(getString(R.string.gr_g_vurgu)) {
            secici(
                R.string.gr_vurgu_modu, R.string.gr_vurgu_modu_sub,
                arrayOf(getString(R.string.gr_vm_nokta), getString(R.string.gr_vm_serit), getString(R.string.gr_vm_zemin)),
                GorunumAyar.oncelikVurgu(this@GorunumAyarActivity)
            ) { v ->
                GorunumAyar.setOncelikVurgu(this@GorunumAyarActivity, v)
                Titresim.dokunus(kok)
            }
        }

        // 3. #5 Görevine Göre Tipografi Vitrini
        grup(getString(R.string.gr_g_tipografi)) {
            secici(
                R.string.gr_font_sablon, R.string.gr_font_sablon_sub,
                arrayOf(getString(R.string.gr_fs_poppins), getString(R.string.gr_fs_atkinson), getString(R.string.gr_fs_lora)),
                GorunumAyar.fontSablon(this@GorunumAyarActivity)
            ) { f ->
                GorunumAyar.setFontSablon(this@GorunumAyarActivity, f)
                Titresim.dokunus(kok)
                recreate()
            }
        }

        // 4. #6 Serbest Yazı Ölçeği & Satır Nefesi Atölyesi
        grup(getString(R.string.gr_g_nefes)) {
            secici(
                R.string.gr_nefes_dp, R.string.gr_nefes_dp_sub,
                arrayOf(getString(R.string.gr_nd_siki), getString(R.string.gr_nd_normal), getString(R.string.gr_nd_ferah)),
                when (GorunumAyar.satirNefesiDp(this@GorunumAyarActivity)) {
                    0 -> 0
                    12, 16 -> 2
                    else -> 1
                }
            ) { n ->
                val dpVal = when (n) {
                    0 -> 0
                    2 -> 12
                    else -> 6
                }
                GorunumAyar.setSatirNefesiDp(this@GorunumAyarActivity, dpVal)
                Titresim.dokunus(kok)
            }
        }

        // 5. #7 Zen Odak vs Canlı Kadran Modu
        grup(getString(R.string.gr_g_sayac)) {
            anahtar(
                R.string.gr_zen_odak, R.string.gr_zen_odak_sub,
                GorunumAyar.zenOdakMi(this@GorunumAyarActivity)
            ) { z ->
                GorunumAyar.setZenOdak(this@GorunumAyarActivity, z)
            }
        }

        // 6. #8 Varsayılan Açılış Ekranı & Kokpit Gizlilik Kontrolü
        grup(getString(R.string.gr_g_acilis)) {
            secici(
                R.string.gr_acilis_secim, R.string.gr_acilis_secim_sub,
                arrayOf(
                    getString(R.string.gr_as_ana),
                    getString(R.string.gr_as_gorev),
                    getString(R.string.gr_as_sayac),
                    getString(R.string.gr_as_ajan),
                    "☀️ Bugün / Günün Akışı",
                    "📋 Vakit Planı",
                    "📊 İlerleme"
                ),
                GorunumAyar.acilisEkran(this@GorunumAyarActivity)
            ) { asId ->
                GorunumAyar.setAcilisEkran(this@GorunumAyarActivity, asId)
                Titresim.dokunus(kok)
            }
            anahtar(
                R.string.gr_hero_gizli, R.string.gr_hero_gizli_sub,
                GorunumAyar.heroGizliMi(this@GorunumAyarActivity)
            ) { h ->
                GorunumAyar.setHeroGizli(this@GorunumAyarActivity, h)
            }
        }

        // 7. #9 Akıllı FAB & Hızlı Buton Özelleştirmesi
        grup(getString(R.string.gr_g_kisayol)) {
            secici(
                R.string.gr_fab_islev, R.string.gr_fab_islev_sub,
                arrayOf(
                    getString(R.string.gr_fi_gorev),
                    getString(R.string.gr_fi_odak),
                    getString(R.string.gr_fi_komut),
                    getString(R.string.gr_fi_ajan)
                ),
                GorunumAyar.fabIslev(this@GorunumAyarActivity)
            ) { i ->
                GorunumAyar.setFabIslev(this@GorunumAyarActivity, i)
                Titresim.dokunus(kok)
            }
        }

        // 8. #10 Yüzen Canlı Durum Şeridi (Mini Status Bar)
        grup(getString(R.string.gr_g_serit)) {
            anahtar(
                R.string.gr_yuzen_serit, R.string.gr_yuzen_serit_sub,
                GorunumAyar.yuzenSeritAcik(this@GorunumAyarActivity)
            ) { s ->
                GorunumAyar.setYuzenSeritAcik(this@GorunumAyarActivity, s)
            }
        }

        // v10.51 · Tasarım Sistemi v2 (16 Maddelik Arayüz Devrimi)
        grup("TASARIM SİSTEMİ V2 (v10.51)") {
            anahtar(
                R.string.td_tasarim_dili_v2, R.string.td_tasarim_dili_v2_alt,
                GorunumAyar.tasarimDiliV2(this@GorunumAyarActivity)
            ) { acik ->
                GorunumAyar.setTasarimDiliV2(this@GorunumAyarActivity, acik)
                recreate()
            }
            anahtar(
                R.string.td_alt_nav_ince, R.string.td_alt_nav_ince_alt,
                GorunumAyar.altNavInce(this@GorunumAyarActivity)
            ) { acik -> GorunumAyar.setAltNavInce(this@GorunumAyarActivity, acik) }
            anahtar(
                R.string.td_ozet_serid, R.string.td_ozet_serid_alt,
                GorunumAyar.ozetSeridModu(this@GorunumAyarActivity)
            ) { acik -> GorunumAyar.setOzetSeridModu(this@GorunumAyarActivity, acik) }
            anahtar(
                R.string.td_tek_akis, R.string.td_tek_akis_alt,
                GorunumAyar.tekAkisKarti(this@GorunumAyarActivity)
            ) { acik -> GorunumAyar.setTekAkisKarti(this@GorunumAyarActivity, acik) }
            anahtar(
                R.string.td_kompakt_konu, R.string.td_kompakt_konu_alt,
                GorunumAyar.kompaktKonu(this@GorunumAyarActivity)
            ) { acik -> GorunumAyar.setKompaktKonu(this@GorunumAyarActivity, acik) }
            anahtar(
                R.string.td_plan_hero, R.string.td_plan_hero_alt,
                GorunumAyar.planHeroModu(this@GorunumAyarActivity)
            ) { acik -> GorunumAyar.setPlanHeroModu(this@GorunumAyarActivity, acik) }
            anahtar(
                R.string.td_sayac_alt_menu, R.string.td_sayac_alt_menu_alt,
                GorunumAyar.sayacAltMenu(this@GorunumAyarActivity)
            ) { acik -> GorunumAyar.setSayacAltMenu(this@GorunumAyarActivity, acik) }
        }

        // Deneme alanı — ayarların etkisi burada anında görülür
        denemeKarti()
    }

    private fun LinearLayout.tiklanabilir(
        baslikRes: Int,
        altRes: Int,
        tikla: () -> Unit
    ) {
        val yg = resources.displayMetrics.density
        val satir = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((16 * yg).toInt(), (14 * yg).toInt(), (16 * yg).toInt(), (14 * yg).toInt())
            isClickable = true
            setOnClickListener { tikla() }
        }
        satir.addView(TextView(context).apply {
            setText(baslikRes)
            textSize = 15f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
        })
        satir.addView(TextView(context).apply {
            setText(altRes)
            textSize = 13f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
        })
        addView(satir)
    }

    /**
     * v8.6 — Çok seçenekli satır (MaterialButtonToggleGroup).
     *
     * Anahtar iki durumluk; yazı boyutu dört durumlu. Açılır liste
     * yerine düğme grubu seçildi: seçenekler görünür kalıyor, tek
     * dokunuşla değişiyor.
     */
    private fun LinearLayout.secici(
        baslikRes: Int,
        altRes: Int,
        secenekler: Array<String>,
        secili: Int,
        degisti: (Int) -> Unit
    ) {
        val yg = resources.displayMetrics.density
        val satir = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((16 * yg).toInt(), (14 * yg).toInt(), (16 * yg).toInt(), (14 * yg).toInt())
        }
        satir.addView(TextView(context).apply {
            setText(baslikRes)
            textSize = 15f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
        })
        satir.addView(TextView(context).apply {
            setText(altRes)
            textSize = 12f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(0, 0, 0, (8 * yg).toInt())
        })

        val grup = com.google.android.material.button.MaterialButtonToggleGroup(context).apply {
            isSingleSelection = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val idler = IntArray(secenekler.size) { View.generateViewId() }
        secenekler.forEachIndexed { i, metin ->
            val dugme = com.google.android.material.button.MaterialButton(
                context, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply {
                id = idler[i]
                text = metin
                textSize = 12f
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                )
            }
            grup.addView(dugme)
        }
        if (secili in idler.indices) grup.check(idler[secili])
        grup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val yeni = idler.indexOf(checkedId)
            if (yeni >= 0 && yeni != secili) degisti(yeni)
        }
        satir.addView(grup)
        addView(satir)
    }

    // ------------------------------------------------------------------
    // Yapı taşları
    // ------------------------------------------------------------------

    private fun grup(baslik: String, icerik: LinearLayout.() -> Unit) {
        val yg = resources.displayMetrics.density

        val etiket = TextView(this).apply {
            text = baslik
            textSize = 12.5f
            setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding((4 * yg).toInt(), (18 * yg).toInt(), 0, (6 * yg).toInt())
        }
        kok.addView(etiket)

        val kart = MaterialCardView(this).apply {
            radius = 18 * yg
            cardElevation = 0f
            setCardBackgroundColor(renk(com.google.android.material.R.attr.colorSurface))
            strokeWidth = (1 * yg).toInt()
            strokeColor = renk(com.google.android.material.R.attr.colorOutlineVariant)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val ic = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        kart.addView(ic)
        kok.addView(kart)
        ic.icerik()
    }

    private fun LinearLayout.anahtar(
        baslikRes: Int,
        altRes: Int,
        deger: Boolean,
        etkin: Boolean = true,
        degisti: (Boolean) -> Unit
    ) {
        val yg = resources.displayMetrics.density
        val satir = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding((16 * yg).toInt(), (14 * yg).toInt(), (16 * yg).toInt(), (14 * yg).toInt())
            isEnabled = etkin
            alpha = if (etkin) 1f else 0.45f
        }

        val metinler = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        metinler.addView(TextView(context).apply {
            setText(baslikRes)
            textSize = 15f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
        })
        metinler.addView(TextView(context).apply {
            setText(altRes)
            textSize = 12f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
        })
        satir.addView(metinler)

        val svic = MaterialSwitch(context).apply {
            isChecked = deger
            isEnabled = etkin
            setOnCheckedChangeListener { dugme, secili ->
                if (!dugme.isPressed && !dugme.isFocused) return@setOnCheckedChangeListener
                Titresim.dokunus(dugme)
                degisti(secili)
            }
        }
        satir.addView(svic)

        // Satıra dokunmak da anahtarı çevirsin — dokunma alanı büyüsün
        if (etkin) {
            satir.isClickable = true
            satir.setOnClickListener { svic.toggle() }
            satir.setBackgroundResource(
                android.R.attr.selectableItemBackground.let {
                    val tv = android.util.TypedValue()
                    theme.resolveAttribute(android.R.attr.selectableItemBackground, tv, true)
                    tv.resourceId
                }
            )
        }
        addView(satir)
    }

    private fun uyariKarti(metin: String) {
        val yg = resources.displayMetrics.density
        val kart = MaterialCardView(this).apply {
            radius = 16 * yg
            cardElevation = 0f
            setCardBackgroundColor(renk(com.google.android.material.R.attr.colorSecondaryContainer))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (12 * yg).toInt() }
        }
        kart.addView(TextView(this).apply {
            text = metin
            textSize = 13f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
            setPadding((16 * yg).toInt(), (14 * yg).toInt(), (16 * yg).toInt(), (14 * yg).toInt())
            setLineSpacing(0f, 1.25f)
        })
        kok.addView(kart)
    }

    /** Ayarların etkisini anında denemek için. */
    private fun denemeKarti() {
        val yg = resources.displayMetrics.density

        kok.addView(TextView(this).apply {
            setText(R.string.gr_g_deneme)
            textSize = 12.5f
            setTextColor(renk(com.google.android.material.R.attr.colorPrimary))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding((4 * yg).toInt(), (18 * yg).toInt(), 0, (6 * yg).toInt())
        })

        val kart = MaterialCardView(this).apply {
            radius = 18 * yg
            cardElevation = 0f
            setCardBackgroundColor(renk(com.google.android.material.R.attr.colorPrimaryContainer))
            isClickable = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((18 * yg).toInt(), (18 * yg).toInt(), (18 * yg).toInt(), (18 * yg).toInt())
        }
        val sayac = TextView(this).apply {
            textSize = 30f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurface))
            text = "0"
        }
        ic.addView(sayac)
        ic.addView(TextView(this).apply {
            setText(R.string.gr_deneme_sub)
            textSize = 12.5f
            setTextColor(renk(com.google.android.material.R.attr.colorOnSurfaceVariant))
        })
        kart.addView(ic)
        kart.dalgaEkle()
        kart.setOnClickListener {
            Titresim.dokunus(it)
            Canlandir.sayi(sayac, 0, (37..248).random())
            Canlandir.nabiz(it)
        }
        kok.addView(kart)

        // Alt boşluk
        kok.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (40 * yg).toInt()
            )
        })
    }

    private fun renk(attr: Int): Int = runCatching {
        com.google.android.material.color.MaterialColors.getColor(kok, attr, 0)
    }.getOrDefault(0)
}
