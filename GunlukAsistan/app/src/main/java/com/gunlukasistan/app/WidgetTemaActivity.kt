package com.gunlukasistan.app

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.color.MaterialColors
import com.google.android.material.materialswitch.MaterialSwitch

/**
 * v7.68 — Widget tema ayarları.
 *
 * ── Kullanıcının isteği ──
 * "Ayarlar kısmına ekstra olarak widget tema seçimi ekleme yap ve bütün
 *  temalara uygula. Widget senkronizasyonu anlık olsun."
 *
 * ── İçerik ──
 * · **Zemin modu**: Karanlık / Aydınlık / Sistemi izle / Uygulama teması
 * · **Saydamlık**: opak → çok saydam (duvar kâğıdı sızar)
 * · **Vurgu rengi**: uygulama temasından ya da widget'lara özel 12 renk
 * · **Anlık senkron** anahtarı
 * · Canlı **önizleme** kartı — seçimi kaydetmeden görürsün
 *
 * v10.20 · SINIRSIZ KONTROL: kullanıcının "sınır koyma" isteğiyle tüm
 * çipler/kaydırıcılar serbest değer diyaloglarına çevrildi (yazı %, dolgu
 * dp, köşe dp, saydamlık %, yatay %, girinti dp, kısıt ms, karartma /
 * canlılık / kontrast %, örnek-başına ölçek) + özel renk şablonu (hex).
 *
 * Her değişiklik anında kaydedilir ve tüm widget'lar zorla tazelenir.
 */
class WidgetTemaActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            context.startActivity(Intent(context, WidgetTemaActivity::class.java))
        }
    }

    private val yogunluk get() = resources.displayMetrics.density
    private lateinit var kap: LinearLayout

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

        kap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                (18 * yogunluk).toInt(), (16 * yogunluk).toInt(),
                (18 * yogunluk).toInt(), (28 * yogunluk).toInt()
            )
        }
        setContentView(
            ScrollView(this).apply {
                setBackgroundColor(
                    MaterialColors.getColor(
                        this@WidgetTemaActivity,
                        com.google.android.material.R.attr.colorSurface, 0
                    )
                )
                addView(kap)
            }
        )
        ciz()
    }

    /** Değişiklikten sonra: kaydet → widget'ları zorla tazele → ekranı çiz. */
    private fun uygula() {
        try {
            WidgetCommon.refreshAll(this, true)
        } catch (e: Exception) {
            android.util.Log.w("WidgetTemaAyar", "Tazelenemedi", e)
        }
        ciz()
    }

    private fun ciz() {
        kap.removeAllViews()

        kap.addView(baslik(getString(R.string.wt_baslik), 20f))
        kap.addView(bilgi(getString(R.string.wt_aciklama)))

        // ── Canlı önizleme ──
        kap.addView(onizleme())

        // ── Zemin modu ──
        kap.addView(baslik(getString(R.string.wt_mod), 15f))
        val modlar = listOf(
            Triple(WidgetTema.MOD_KOYU, R.string.wt_m_koyu, R.string.wt_m_koyu_d),
            Triple(WidgetTema.MOD_ACIK, R.string.wt_m_acik, R.string.wt_m_acik_d),
            Triple(WidgetTema.MOD_SISTEM, R.string.wt_m_sistem, R.string.wt_m_sistem_d),
            Triple(WidgetTema.MOD_TEMA, R.string.wt_m_tema, R.string.wt_m_tema_d)
        )
        val secili = WidgetTema.mod(this)
        modlar.forEach { (deger, ad, aciklama) ->
            kap.addView(
                secenek(getString(ad), getString(aciklama), deger == secili) {
                    WidgetTema.setMod(this, deger)
                    uygula()
                }
            )
        }

        kap.addView(ayirici())

        // ── Saydamlık (v10.20: serbest yüzde — kademe bitti) ──
        kap.addView(baslik(getString(R.string.wt_saydam), 15f))
        kap.addView(bilgi(getString(R.string.w20_saydam_d)))
        serbestSayi(
            R.string.w20_saydam_satir,
            getString(R.string.w20_pct_fmt, WidgetTema.saydamlikPct(this))
        ) { v -> WidgetTema.setSaydamlikPct(this, v); uygula() }

        kap.addView(ayirici())

        // ── Köşe yuvarlaklığı (v10.20: serbest dp — artık TÜM widget'larda) ──
        kap.addView(baslik(getString(R.string.wt_kose), 15f))
        kap.addView(bilgi(getString(R.string.w20_kose_d)))
        serbestSayi(
            R.string.w20_kose_satir,
            getString(R.string.w20_dp_fmt, WidgetTema.koseDpF(this).toInt())
        ) { v -> WidgetTema.setKoseDpF(this, v); uygula() }

        // ── Widget yazı boyutu (v10.20: sınır yok — serbest yüzde) ──
        kap.addView(baslik(getString(R.string.wt_yazi), 15f))
        kap.addView(bilgi(getString(R.string.w20_yazi_d)))
        serbestSayi(
            R.string.w20_yazi_satir,
            getString(R.string.w20_pct_fmt, WidgetAtolye.yaziYuzde(this))
        ) { v -> WidgetAtolye.setYaziYuzde(this, v); uygula() }

        // ── İç dolgu (v10.20: serbest dp) ──
        kap.addView(baslik(getString(R.string.wt_dolgu), 15f))
        kap.addView(bilgi(getString(R.string.wt_dolgu_d)))
        serbestSayi(
            R.string.w20_dolgu_satir,
            getString(R.string.w20_dp_fmt, WidgetAtolye.kokDolguDp(this))
        ) { v -> WidgetAtolye.setDolguDp(this, v); uygula() }

        // ── Satır nefesi (v10.20: serbest dp) ──
        kap.addView(baslik(getString(R.string.wt_nefes), 15f))
        kap.addView(bilgi(getString(R.string.wt_nefes_d)))
        serbestSayi(
            R.string.w20_nefes_satir,
            getString(R.string.w20_dp_fmt, WidgetAtolye.satirDolguDp(this))
        ) { v -> WidgetAtolye.setSatirDp(this, v); uygula() }

        // ── Birleştirilebilir widget notu ──
        kap.addView(bilgi(getString(R.string.wa_birlestir_not)))

        kap.addView(ayirici())

        // ── Vurgu rengi ──
        kap.addView(baslik(getString(R.string.wt_vurgu_baslik), 15f))
        val ozel = WidgetTema.ozelVurgu(this)
        kap.addView(
            secenek(getString(R.string.wt_vurgu_tema), "", ozel < 0) {
                WidgetTema.setOzelVurgu(this, -1)
                uygula()
            }
        )
        kap.addView(bilgi(getString(R.string.wt_vurgu_ozel)))
        kap.addView(renkIzgarasi(ozel))

        kap.addView(ayirici())

        // ═══════════════════════════════════════════════════════════
        // v10.17 — WIDGET AYAR ENVANTERİ (33 yeni ayar; yeni widget yok,
        // mevcut widget'ların işlemesi ve parçaları denetlenir)
        // ═══════════════════════════════════════════════════════════

        kap.addView(ayirici())
        kap.addView(baslik(getString(R.string.w17_renk_baslik), 15f))
        kap.addView(bilgi(getString(R.string.w17_renk_d)))

        cipGrubu(R.string.w17_metin, 0, listOf(
            R.string.w17_m_otomatik, R.string.w17_m_acik,
            R.string.w17_m_koyu, R.string.w17_m_vurgulu
        ), WidgetSecim.metinMod(this)) { WidgetSecim.setMetinMod(this, it); uygula() }

        // v10.20: canlılık serbest yüzde
        serbestSayi(
            R.string.w17_canli,
            getString(R.string.w20_pct_fmt, WidgetSecim.canliPct(this))
        ) { v -> WidgetSecim.setCanliPct(this, v); uygula() }

        cipGrubu(R.string.w17_tamam, 0, listOf(
            R.string.w17_t_yesil, R.string.w17_t_mavi,
            R.string.w17_t_gri, R.string.w17_t_vurgu
        ), WidgetSecim.tamamMod(this)) { WidgetSecim.setTamamMod(this, it); uygula() }

        // v10.20: kontrast serbest yüzde (0 = kapalı)
        serbestSayi(
            R.string.w17_kontrast,
            getString(R.string.w20_pct_fmt, WidgetSecim.kontrastPct(this)),
            getString(R.string.w20_kontrast_d)
        ) { v -> WidgetSecim.setKontrastPct(this, v); uygula() }

        // ── v10.20: ÖZEL RENK ŞABLONU — kullanıcı kendi paletini yazar ──
        kap.addView(baslik(getString(R.string.w20_renk_baslik), 13.5f))
        kap.addView(bilgi(getString(R.string.w20_renk_d)))
        ozelRenkSatiri(R.string.w20_r_zemin, WidgetSecim.K_OZ_ZEMIN)
        ozelRenkSatiri(R.string.w20_r_metin, WidgetSecim.K_OZ_METIN)
        ozelRenkSatiri(R.string.w20_r_vurgu, WidgetSecim.K_OZ_VURGU)
        ozelRenkSatiri(R.string.w20_r_yesil, WidgetSecim.K_OZ_YESIL)

        kap.addView(ayirici())
        kap.addView(baslik(getString(R.string.w17_gece_baslik), 15f))
        kap.addView(anahtar(
            getString(R.string.w17_gece), getString(R.string.w17_gece_d),
            WidgetSecim.karartAcik(this)
        ) { a -> WidgetSecim.setKarartAcik(this, a); uygula() })
        saatSatiri(R.string.w17_bas, WidgetSecim.basSaat(this)) { saat ->
            WidgetSecim.setBasSaat(this, saat); uygula()
        }
        saatSatiri(R.string.w17_bit, WidgetSecim.bitSaat(this)) { saat ->
            WidgetSecim.setBitSaat(this, saat); uygula()
        }
        // v10.20: karartma şiddeti serbest yüzde
        serbestSayi(
            R.string.w17_siddet,
            getString(R.string.w20_pct_fmt, WidgetSecim.siddetPct(this))
        ) { v -> WidgetSecim.setSiddetPct(this, v); uygula() }

        kap.addView(ayirici())
        kap.addView(baslik(getString(R.string.w17_duzen_baslik), 15f))
        kap.addView(bilgi(getString(R.string.w17_duzen_d)))
        // v10.20: yatay oran ve girinti serbest (kademe bitti)
        serbestSayi(
            R.string.w17_yatay,
            getString(R.string.w20_pct_fmt, WidgetSecim.yatayPct(this))
        ) { v -> WidgetSecim.setYatayPct(this, v); uygula() }
        serbestSayi(
            R.string.w17_girinti,
            getString(R.string.w20_dp_fmt, WidgetSecim.girintiDpC(this))
        ) { v -> WidgetSecim.setGirintiDp(this, v); uygula() }

        kap.addView(ayirici())
        kap.addView(baslik(getString(R.string.w17_davranis_baslik), 15f))
        // v10.20: tazeleme kısıtı serbest ms (0 = kısıt yok)
        serbestSayi(
            R.string.w17_kisit,
            getString(R.string.w20_ms_fmt, WidgetSecim.kisitMs(this).toInt()),
            getString(R.string.w20_kisit_not)
        ) { v -> WidgetSecim.setKisitMs(this, v); uygula() }

        kap.addView(ayirici())
        kap.addView(baslik(getString(R.string.w17_widget_baslik), 15f))
        kap.addView(bilgi(getString(R.string.w17_widget_d)))

        parcaGrubu(R.string.ws_countdown, listOf(
            Triple(WidgetSecim.W_CD_ETIKET, R.string.w17_cd_etiket, 0),
            Triple(WidgetSecim.W_CD_EMOJI, R.string.w17_cd_emoji, 0)
        ), WidgetSecim.ORNEK_CD)
        parcaGrubu(R.string.ws_summary, listOf(
            Triple(WidgetSecim.W_SUM_SELAM, R.string.w17_sum_selam, 0),
            Triple(WidgetSecim.W_SUM_GERI, R.string.w17_sum_geri, 0),
            Triple(WidgetSecim.W_SUM_KUTU, R.string.w17_sum_kutu, 0),
            Triple(WidgetSecim.W_SUM_SERI, R.string.w17_sum_seri, 0)
        ), WidgetSecim.ORNEK_SUM)
        parcaGrubu(R.string.w17_g_eylem, listOf(
            Triple(WidgetSecim.W_ACT_ODAK, R.string.w17_act_odak, 0),
            Triple(WidgetSecim.W_ACT_SORU, R.string.w17_act_soru, 0),
            Triple(WidgetSecim.W_ACT_GOREV, R.string.w17_act_gorev, 0),
            Triple(WidgetSecim.W_ACT_BUGUN, R.string.w17_act_bugun, 0),
            Triple(WidgetSecim.W_ACT_SES, R.string.w17_act_ses, 0)
        ), WidgetSecim.ORNEK_ACT)
        parcaGrubu(R.string.ws_sayac, listOf(
            Triple(WidgetSecim.W_SY_PRESET, R.string.w17_sy_preset, R.string.w17_sy_preset_d),
            Triple(WidgetSecim.W_SY_SIFIRLA, R.string.w17_sy_sifirla, 0),
            Triple(WidgetSecim.W_SY_BAR, R.string.w17_sy_bar, 0)
        ), WidgetSecim.ORNEK_SY)
        // Hedef: görünürlük anahtarı + % ⇄ kalan dk mod anahtarı
        kap.addView(baslik(getString(R.string.ws_hedef), 13.5f))
        // v10.20: örnek-başına yazı ölçeği
        serbestSayi(
            R.string.w20_ornek_satir,
            getString(R.string.w20_pct_fmt, WidgetSecim.ornekPct(this, WidgetSecim.ORNEK_HD)),
            getString(R.string.w20_ornek_not)
        ) { v -> WidgetSecim.setOrnekPct(this, WidgetSecim.ORNEK_HD, v); uygula() }
        kap.addView(anahtar(
            getString(R.string.w17_hd_mod), getString(R.string.w17_hd_mod_d),
            WidgetSecim.hedefMod(this) == 1
        ) { a -> WidgetSecim.setHedefMod(this, if (a) 1 else 0); uygula() })
        kap.addView(anahtar(
            getString(R.string.w17_hd_alt), "",
            WidgetSecim.goster(this, WidgetSecim.W_HD_ALT)
        ) { a -> WidgetSecim.setGoster(this, WidgetSecim.W_HD_ALT, a); uygula() })
        parcaGrubu(R.string.ws_namaz, listOf(
            Triple(WidgetSecim.W_NW_AD, R.string.w17_nw_ad, 0),
            Triple(WidgetSecim.W_NW_KALAN, R.string.w17_nw_kalan, 0)
        ), WidgetSecim.ORNEK_NW)
        parcaGrubu(R.string.w17_g_uyku, listOf(
            Triple(WidgetSecim.W_UY_ORT, R.string.w17_uy_ort, 0),
            Triple(WidgetSecim.W_UY_HEDEF, R.string.w17_uy_hedef, 0),
            Triple(WidgetSecim.W_UY_PLAN, R.string.w17_uy_plan, 0),
            Triple(WidgetSecim.W_UY_HARF, R.string.w17_uy_harf, 0)
        ), WidgetSecim.ORNEK_UY)

        // ═══════════════════════════════════════════════════════════
        // v10.21 · BAŞLIK ÇUBUKLARI + LİSTE FİLTRELERİ + DOKUNMA HEDEFİ
        // (havuzun kalan maddeleri — "her şeyin yetkisi ayarlarda" turu)
        // ═══════════════════════════════════════════════════════════
        kap.addView(ayirici())
        kap.addView(baslik(getString(R.string.w21_baslik_baslik), 15f))
        kap.addView(bilgi(getString(R.string.w21_baslik_d)))
        listOf(
            Triple(R.string.w21_b_gorev, WidgetSecim.W_TW_BASLIK, 0),
            Triple(R.string.w21_b_geri, WidgetSecim.W_EV_BASLIK, 0),
            Triple(R.string.w21_b_uyku, WidgetSecim.W_UY_BASLIK, 0),
            Triple(R.string.w21_b_hafta, WidgetSecim.W_HW_BASLIK, 0)
        ).forEach { (ad, anahtar2, _) ->
            kap.addView(
                anahtar(getString(ad), "", WidgetSecim.goster(this, anahtar2)) { a ->
                    WidgetSecim.setGoster(this, anahtar2, a); uygula()
                }
            )
        }

        kap.addView(ayirici())
        kap.addView(baslik(getString(R.string.w21_liste_baslik), 15f))
        kap.addView(bilgi(getString(R.string.w21_liste_d)))
        // — Görev listesi —
        kap.addView(baslik(getString(R.string.w21_gorev_liste), 13.5f))
        kap.addView(anahtar(
            getString(R.string.w21_g_biten), getString(R.string.w21_g_biten_d),
            WidgetListe.gosterBool(this, WidgetListe.K_TW_BITEN, false)
        ) { a -> WidgetListe.setBool(this, WidgetListe.K_TW_BITEN, a); uygula() })
        kap.addView(anahtar(
            getString(R.string.w21_g_tarihsiz), "",
            WidgetListe.gosterBool(this, WidgetListe.K_TW_TARIHSIZ, true)
        ) { a -> WidgetListe.setBool(this, WidgetListe.K_TW_TARIHSIZ, a); uygula() })
        kap.addView(anahtar(
            getString(R.string.w21_g_ilerisi), "",
            WidgetListe.gosterBool(this, WidgetListe.K_TW_ILERISI, true)
        ) { a -> WidgetListe.setBool(this, WidgetListe.K_TW_ILERISI, a); uygula() })
        serbestSayi(
            R.string.w21_g_satir,
            getString(R.string.w21_satir_fmt, WidgetListe.satir(this, WidgetListe.K_TW_SATIR, 40))
        ) { v -> WidgetListe.setSatir(this, WidgetListe.K_TW_SATIR, v); uygula() }
        // — Geri sayım listesi —
        kap.addView(baslik(getString(R.string.w21_geri_liste), 13.5f))
        kap.addView(anahtar(
            getString(R.string.w21_e_gecmis), getString(R.string.w21_e_gecmis_d),
            WidgetListe.gosterBool(this, WidgetListe.K_EV_GECMIS, true)
        ) { a -> WidgetListe.setBool(this, WidgetListe.K_EV_GECMIS, a); uygula() })
        kap.addView(anahtar(
            getString(R.string.w21_e_sabit), "",
            WidgetListe.gosterBool(this, WidgetListe.K_EV_SABIT, false)
        ) { a -> WidgetListe.setBool(this, WidgetListe.K_EV_SABIT, a); uygula() })
        serbestSayi(
            R.string.w21_e_satir,
            getString(R.string.w21_satir_fmt, WidgetListe.satir(this, WidgetListe.K_EV_SATIR, 6))
        ) { v -> WidgetListe.setSatir(this, WidgetListe.K_EV_SATIR, v); uygula() }

        kap.addView(ayirici())
        kap.addView(baslik(getString(R.string.w21_dokunma_baslik), 15f))
        kap.addView(bilgi(getString(R.string.w21_dokunma_d)))
        dokunmaSatiri(R.string.ws_countdown, WidgetDokunma.CD, getString(R.string.w21_e_etkinlik))
        dokunmaSatiri(R.string.ws_sayac, WidgetDokunma.SY, getString(R.string.w21_e_zaman))
        dokunmaSatiri(R.string.ws_hedef, WidgetDokunma.HD, getString(R.string.w21_e_bugun))
        dokunmaSatiri(R.string.ws_summary, WidgetDokunma.SUM, getString(R.string.w21_e_bugun))
        dokunmaSatiri(R.string.ws_tasks, WidgetDokunma.TASKS, getString(R.string.w21_e_gorevler))
        dokunmaSatiri(R.string.ws_events, WidgetDokunma.EV, getString(R.string.w21_e_etkinlik))
        dokunmaSatiri(R.string.w21_odak, WidgetDokunma.ODAK, getString(R.string.w21_e_zaman))

        // ── Anlık senkron ──
        kap.addView(
            anahtar(
                getString(R.string.wt_anlik),
                getString(R.string.wt_anlik_d),
                WidgetTema.anlikSenkron(this)
            ) { acik ->
                WidgetTema.setAnlikSenkron(this, acik)
            }
        )

        // ── Şimdi yenile ──
        kap.addView(
            dugme(getString(R.string.wt_simdi_yenile)) {
                val adet = widgetSayisi()
                try {
                    WidgetCommon.refreshAll(this, true)
                } catch (e: Exception) {
                    android.util.Log.w("WidgetTemaAyar", "Yenilenemedi", e)
                }
                Toast.makeText(
                    this,
                    if (adet == 0) getString(R.string.wt_yok)
                    else getString(R.string.wt_yenilendi, adet),
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        // ── v10.5 · C29: Widget stüdyosu — tek dokunuşla sabitle ──
        kap.addView(ayirici())
        kap.addView(baslik(getString(R.string.ws_pin_baslik), 15f))
        kap.addView(bilgi(getString(R.string.ws_pin_aciklama)))
        kap.addView(bilgi(getString(R.string.ws_pin_ozet, sabitlenebilirListe().size)))
        sabitlenebilirListe().forEach { (adRes, sinif) ->
            kap.addView(
                secenek(getString(adRes), "", false) {
                    sabitle(sinif)
                }
            )
        }
    }

    /**
     * v10.5 · C29 — `requestPinAppWidget` destekleyen widget listesi.
     * Koleksiyon (Liste) widget'ları da sistem tarafından sabitlenebilir.
     */
    private fun sabitlenebilirListe(): List<Pair<Int, Class<*>>> = listOf(
        R.string.ws_sayac to SayacWidget::class.java,
        R.string.ws_events to EventsListWidget::class.java,
        R.string.ws_hedef to HedefWidget::class.java,
        R.string.ws_tasks to TasksWidget::class.java,
        R.string.ws_brifing to BrifingWidget::class.java,
        R.string.ws_countdown to CountdownWidget::class.java,
        R.string.ws_namaz to NamazWidget::class.java,
        R.string.ws_summary to SummaryWidget::class.java
    )

    /** Tek dokunuşla ana ekrana sabitleme isteği (API 26+). */
    private fun sabitle(sinif: Class<*>) {
        try {
            val m = AppWidgetManager.getInstance(this)
            if (Build.VERSION.SDK_INT >= 26 && m.isRequestPinAppWidgetSupported) {
                m.requestPinAppWidget(ComponentName(this, sinif), null, null)
            } else {
                Toast.makeText(
                    this, R.string.ws_pin_desteklenmiyor, Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            android.util.Log.w("WidgetTemaAyar", "Sabitleme başarısız", e)
            Toast.makeText(
                this, R.string.ws_pin_desteklenmiyor, Toast.LENGTH_LONG
            ).show()
        }
    }

    /** Ana ekranda kaç widget kurulu? */
    private fun widgetSayisi(): Int = try {
        val m = AppWidgetManager.getInstance(this)
        listOf(
            PlanWidget::class.java, NamazWidget::class.java, TasksWidget::class.java,
            SummaryWidget::class.java, CountdownWidget::class.java,
            ActionsWidget::class.java, BrifingWidget::class.java
        ).sumOf { m.getAppWidgetIds(ComponentName(this, it)).size }
    } catch (e: Exception) {
        android.util.Log.w("WidgetTemaAyar", "Sayılamadı", e)
        0
    }

    // ═══════════════════════════════════════════════════════════════
    // ÖNİZLEME
    // ═══════════════════════════════════════════════════════════════

    /** Seçili ayarlarla bir widget'ın nasıl görüneceğini gösterir. */
    private fun onizleme(): View {
        val p = WidgetTema.palet(this)
        // v10.20: serbest yüzde + serbest dolgu + serbest köşe + serbest
        // saydamlık + özel renkler (palet() içinden) önizlemede yaşar
        val carpan = WidgetAtolye.yaziCarpan(this)
        val kutu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val ek = (WidgetAtolye.kokDolguDp(this@WidgetTemaActivity) * yogunluk).toInt()
            setPadding(
                (16 * yogunluk).toInt() + ek, (14 * yogunluk).toInt() + ek,
                (16 * yogunluk).toInt() + ek, (14 * yogunluk).toInt() + ek
            )
            val zeminAlfa = WidgetZemin.saydamlikYuzdeAlfa(
                WidgetTema.saydamlikPct(this@WidgetTemaActivity)
            )
            background = GradientDrawable().apply {
                cornerRadius = WidgetTema.koseDpF(this@WidgetTemaActivity) * yogunluk
                setColor((zeminAlfa shl 24) or (p.zemin and 0x00FFFFFF))
                setStroke((1 * yogunluk).toInt(), p.vurgu and 0x44FFFFFF)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (14 * yogunluk).toInt() }
        }
        kutu.addView(TextView(this).apply {
            text = "🕌 " + getString(R.string.pw_baslik)
            textSize = 14f * carpan
            setTextColor(p.metin)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        kutu.addView(TextView(this).apply {
            text = getString(R.string.wt_onizleme)
            textSize = 11.5f * carpan
            setTextColor(p.metinSoluk)
            setPadding(0, (3 * yogunluk).toInt(), 0, 0)
        })
        kutu.addView(TextView(this).apply {
            text = "▸ 45 dk · Örnek iş"
            textSize = 12.5f * carpan
            setTextColor(p.vurgu)
            setPadding(0, (8 * yogunluk).toInt(), 0, 0)
        })
        return kutu
    }

    /** 12 vurgu renginden seçim. */
    private fun renkIzgarasi(secili: Int): View {
        val satir = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        ThemeManager.accents.forEachIndexed { indeks, accent ->
            satir.addView(
                View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        (34 * yogunluk).toInt(), (34 * yogunluk).toInt()
                    ).apply { marginEnd = (8 * yogunluk).toInt() }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(accent.swatch)
                        if (indeks == secili) {
                            setStroke(
                                (3 * yogunluk).toInt(),
                                MaterialColors.getColor(
                                    this@WidgetTemaActivity,
                                    com.google.android.material.R.attr.colorOnSurface, 0
                                )
                            )
                        }
                    }
                    isClickable = true
                    setOnClickListener {
                        WidgetTema.setOzelVurgu(this@WidgetTemaActivity, indeks)
                        uygula()
                    }
                }
            )
        }
        return android.widget.HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(0, (6 * yogunluk).toInt(), 0, (4 * yogunluk).toInt())
            addView(satir)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ARAYÜZ YARDIMCILARI
    // ═══════════════════════════════════════════════════════════════

    private fun baslik(metin: String, boyut: Float) = TextView(this).apply {
        text = metin
        textSize = boyut
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, (12 * yogunluk).toInt(), 0, (6 * yogunluk).toInt())
    }

    private fun bilgi(metin: String) = TextView(this).apply {
        text = metin
        textSize = 12f
        alpha = 0.7f
        setLineSpacing(0f, 1.2f)
        setPadding(0, 0, 0, (6 * yogunluk).toInt())
    }

    private fun ayirici() = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, (1 * yogunluk).toInt()
        ).apply {
            topMargin = (12 * yogunluk).toInt()
            bottomMargin = (4 * yogunluk).toInt()
        }
        setBackgroundColor(
            (MaterialColors.getColor(
                this@WidgetTemaActivity,
                com.google.android.material.R.attr.colorOnSurface, 0
            ) and 0x00FFFFFF) or 0x22000000
        )
    }

    /** Radyo benzeri seçim satırı. */
    private fun secenek(
        ad: String,
        aciklama: String,
        secili: Boolean,
        tikla: () -> Unit
    ): View {
        val satir = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, (9 * yogunluk).toInt(), 0, (9 * yogunluk).toInt())
            background = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x22888888), null, null
            )
            isClickable = true
            setOnClickListener { tikla() }
        }
        satir.addView(TextView(this).apply {
            text = if (secili) "◉" else "○"
            textSize = 17f
            setPadding(0, 0, (12 * yogunluk).toInt(), 0)
            if (secili) setTextColor(
                MaterialColors.getColor(
                    this, com.google.android.material.R.attr.colorPrimary, 0
                )
            )
        })
        satir.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                )
                addView(TextView(this@WidgetTemaActivity).apply {
                    text = ad
                    textSize = 14f
                })
                if (aciklama.isNotBlank()) {
                    addView(TextView(this@WidgetTemaActivity).apply {
                        text = aciklama
                        textSize = 11.5f
                        alpha = 0.7f
                    })
                }
            }
        )
        return satir
    }

    private fun cip(metin: String, secili: Boolean, tikla: () -> Unit) =
        TextView(this).apply {
            text = metin
            textSize = 12.5f
            gravity = Gravity.CENTER
            setPadding(
                (14 * yogunluk).toInt(), (8 * yogunluk).toInt(),
                (14 * yogunluk).toInt(), (8 * yogunluk).toInt()
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = (6 * yogunluk).toInt() }
            val vurgu = MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorPrimary, 0
            )
            background = GradientDrawable().apply {
                cornerRadius = 18 * yogunluk
                if (secili) {
                    setColor((vurgu and 0x00FFFFFF) or 0x33000000)
                    setStroke((1.5f * yogunluk).toInt(), vurgu)
                } else {
                    setColor(0x14888888)
                }
            }
            if (secili) setTextColor(vurgu)
            isClickable = true
            setOnClickListener { tikla() }
        }

    private fun anahtar(
        ad: String,
        aciklama: String,
        acik: Boolean,
        degisince: (Boolean) -> Unit
    ): View {
        val satir = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, (8 * yogunluk).toInt(), 0, (8 * yogunluk).toInt())
        }
        satir.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                )
                addView(TextView(this@WidgetTemaActivity).apply {
                    text = ad
                    textSize = 14f
                })
                if (aciklama.isNotBlank()) {
                    addView(TextView(this@WidgetTemaActivity).apply {
                        text = aciklama
                        textSize = 11.5f
                        alpha = 0.7f
                    })
                }
            }
        )
        satir.addView(
            MaterialSwitch(this).apply {
                isChecked = acik
                setOnCheckedChangeListener { _, v -> degisince(v) }
            }
        )
        return satir
    }

    private fun dugme(metin: String, tikla: () -> Unit) = TextView(this).apply {
        text = metin
        textSize = 13.5f
        setTextColor(
            MaterialColors.getColor(
                this@WidgetTemaActivity,
                com.google.android.material.R.attr.colorPrimary, 0
            )
        )
        setPadding(0, (12 * yogunluk).toInt(), 0, (12 * yogunluk).toInt())
        background = android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf(0x22888888), null, null
        )
        isClickable = true
        setOnClickListener { tikla() }
    }

    /** v10.17: küçük başlık + yatay çip satırı (envanter bölümlerinde çok kullanılır). */
    private fun cipGrubu(baslikRes: Int, bilgiRes: Int, adlar: List<Int>, secili: Int, sec: (Int) -> Unit) {
        kap.addView(baslik(getString(baslikRes), 13.5f))
        if (bilgiRes != 0) kap.addView(bilgi(getString(bilgiRes)))
        val satir = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        adlar.forEachIndexed { i, res ->
            satir.addView(cip(getString(res), i == secili) { sec(i) })
        }
        kap.addView(
            android.widget.HorizontalScrollView(this).apply {
                isHorizontalScrollBarEnabled = false
                addView(satir)
            }
        )
    }

    /** v10.17: saat (0-23) seçim satırı — dokununca NumberPicker diyaloğu. */
    private fun saatSatiri(adRes: Int, saat: Int, secildi: (Int) -> Unit) {
        kap.addView(
            secenek(getString(adRes), getString(R.string.w17_saat_fmt, saat), false) {
                val secici = android.widget.NumberPicker(this).apply {
                    minValue = 0
                    maxValue = 23
                    value = saat
                    wrapSelectorWheel = true
                }
                androidx.appcompat.app.AlertDialog.Builder(this@WidgetTemaActivity)
                    .setTitle(getString(adRes))
                    .setView(secici)
                    .setPositiveButton(android.R.string.ok) { _, _ -> secildi(secici.value) }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        )
    }

    /** v10.17: bir widget'ın parça görünürlükleri — küçük başlık + anahtar dizisi.
     *  v10.20: [olcekKodu] verilirse grup başına serbest yazı ölçeği satırı eklenir. */
    private fun parcaGrubu(
        adRes: Int,
        parcalar: List<Triple<String, Int, Int>>,
        olcekKodu: String? = null
    ) {
        kap.addView(baslik(getString(adRes), 13.5f))
        if (olcekKodu != null) {
            serbestSayi(
                R.string.w20_ornek_satir,
                getString(R.string.w20_pct_fmt, WidgetSecim.ornekPct(this, olcekKodu)),
                getString(R.string.w20_ornek_not)
            ) { v -> WidgetSecim.setOrnekPct(this, olcekKodu, v); uygula() }
        }
        parcalar.forEach { (kod, ad, aciklama) ->
            kap.addView(
                anahtar(
                    getString(ad),
                    if (aciklama != 0) getString(aciklama) else "",
                    WidgetSecim.goster(this, kod)
                ) { a ->
                    WidgetSecim.setGoster(this, kod, a)
                    uygula()
                }
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // v10.20 · SERBEST DEĞER SATIRLARI — "sınır koyma" isteğinin köprüsü:
    // çipler ve kaydırıcılar yerine dokun-yaz diyalogları. Teknik güvenlik
    // (negatif setTextSize çökertir vb.) dışında engel YOK.
    // ═══════════════════════════════════════════════════════════════

    /** Serbest sayı ayarı satırı — mevcut değeri gösterir, dokununca diyalog açar. */
    private fun serbestSayi(adRes: Int, degerMetin: String, not: String? = null, kaydet: (Int) -> Unit) {
        kap.addView(secenek(getString(adRes), degerMetin, false) {
            sayiDiyalog(getString(adRes), not, kaydet)
        })
    }

    /** Sınırsız tam sayı diyaloğu — geçersiz giriş reddedilir, değer kelepçesi katmanda. */
    private fun sayiDiyalog(baslik: String, not: String?, kaydet: (Int) -> Unit) {
        val kutu = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
            setPadding(
                (18 * yogunluk).toInt(), (6 * yogunluk).toInt(),
                (18 * yogunluk).toInt(), 0
            )
        }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(baslik)
            .setMessage(
                getString(R.string.w20_diyalog) +
                    if (not.isNullOrBlank()) "" else "\n\n" + not
            )
            .setView(kutu)
            .setPositiveButton(R.string.save) { _, _ ->
                val v = kutu.text.toString().trim().toIntOrNull()
                if (v == null) {
                    Toast.makeText(this, R.string.w20_gecersiz, Toast.LENGTH_LONG).show()
                } else {
                    kaydet(v)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** Özel renk satırı — mevcut hex'i (ya da "Otomatik") gösterir. */
    private fun ozelRenkSatiri(adRes: Int, anahtar: String) {
        val mevcut = WidgetSecim.ozelRenk(this, anahtar)
        val deger = if (mevcut == null) getString(R.string.w20_oto) else WidgetSecim.hexYaz(mevcut)
        kap.addView(secenek(getString(adRes), deger, false) {
            hexDiyalog(getString(adRes), anahtar)
        })
    }

    /** Serbest hex renk diyaloğu — boş bırakılırsa tema otomatiğine döner. */
    private fun hexDiyalog(baslik: String, anahtar: String) {
        val kutu = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setPadding(
                (18 * yogunluk).toInt(), (6 * yogunluk).toInt(),
                (18 * yogunluk).toInt(), 0
            )
            setText(
                WidgetSecim.ozelRenk(this@WidgetTemaActivity, anahtar)
                    ?.let { WidgetSecim.hexYaz(it) } ?: ""
            )
        }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(baslik)
            .setMessage(getString(R.string.w20_hex_d))
            .setView(kutu)
            .setPositiveButton(R.string.save) { _, _ ->
                val yazi = kutu.text.toString()
                val renk = WidgetSecim.hexOku(yazi)
                when {
                    yazi.isBlank() -> {
                        WidgetSecim.setOzelRenk(this@WidgetTemaActivity, anahtar, null)
                        uygula()
                    }
                    renk == null ->
                        Toast.makeText(this@WidgetTemaActivity, R.string.w20_gecersiz, Toast.LENGTH_LONG).show()
                    else -> {
                        WidgetSecim.setOzelRenk(this@WidgetTemaActivity, anahtar, renk)
                        uygula()
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .setNeutralButton(R.string.w20_temizle) { _, _ ->
                WidgetSecim.setOzelRenk(this@WidgetTemaActivity, anahtar, null)
                uygula()
            }
            .show()
    }

    // ── v10.21 · Gövde dokunma hedefi ──

    /** Satır: mevcut hedefi gösterir; dokununca sekme listesi diyaloğu açar. */
    private fun dokunmaSatiri(widgetAdRes: Int, anahtar: String, varsayilanAd: String) {
        val secim = WidgetDokunma.secili(this, anahtar)
        val mevcut = if (secim == -1) {
            getString(R.string.w21_vars, varsayilanAd)
        } else {
            getString(WidgetDokunma.EKRAN_ADLARI.getValue(secim))
        }
        kap.addView(secenek(getString(widgetAdRes), mevcut, false) {
            dokunmaDiyalog(getString(widgetAdRes), anahtar, varsayilanAd)
        })
    }

    /** İlk madde "Varsayılan (…)"; ardından seçilebilir 12 sekme. */
    private fun dokunmaDiyalog(widgetAd: String, anahtar: String, varsayilanAd: String) {
        val adlar = mutableListOf(getString(R.string.w21_vars, varsayilanAd))
        WidgetDokunma.EKRANLAR.forEach { kod ->
            adlar.add(getString(WidgetDokunma.EKRAN_ADLARI.getValue(kod)))
        }
        val seciliIdx = WidgetDokunma.secili(this, anahtar)
            .let { if (it == -1) 0 else WidgetDokunma.EKRANLAR.indexOf(it) + 1 }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(widgetAd)
            .setSingleChoiceItems(adlar.toTypedArray(), seciliIdx) { diyalog, hangi ->
                WidgetDokunma.setEkran(
                    this@WidgetTemaActivity, anahtar,
                    if (hangi == 0) -1 else WidgetDokunma.EKRANLAR[hangi - 1]
                )
                diyalog.dismiss()
                uygula()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
