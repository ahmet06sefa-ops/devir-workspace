package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch

/**
 * v7.43 — Bildirim ayarları ekranı (öneri 26).
 *
 * Bu ekran olmadan 30 bildirim eklemek kullanıcıyı rahatsız ederdi.
 * Burada her tür ayrı ayrı açılıp kapatılabilir; ayrıca:
 *   · Rahatsız etmeyin saatleri (27)
 *   · Günlük bildirim tavanı
 *   · Sabah/akşam tur saatleri
 *   · Test bildirimi
 *
 * Arayüz tamamen kodla kurulur — 20+ anahtar için XML yazmak
 * bakımı zorlaştırırdı, enum üzerinden döngüyle üretmek daha sağlam.
 */
class BildirimAyarActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            context.startActivity(Intent(context, BildirimAyarActivity::class.java))
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

        BildirimMerkezi.kanallariKur(this)

        kap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                (18 * yogunluk).toInt(), (14 * yogunluk).toInt(),
                (18 * yogunluk).toInt(), (28 * yogunluk).toInt()
            )
        }
        setContentView(
            ScrollView(this).apply {
                setBackgroundColor(
                    MaterialColors.getColor(
                        this@BildirimAyarActivity,
                        com.google.android.material.R.attr.colorSurface, 0
                    )
                )
                addView(kap)
            }
        )
        ekraniKur()
    }

    private fun ekraniKur() {
        kap.removeAllViews()

        // ── Başlık ──
        kap.addView(baslik(getString(R.string.nset_title), 20f))

        // ── Ana anahtar ──
        val anaAcik = Store.getNotifEnabled(this)
        kap.addView(
            anahtar(
                BildirimKilit.etiket(
                    this, getString(R.string.nset_master),
                    OnlineStore.Islem.BILDIRIM_TUM_KAPAT
                ),
                "", anaAcik
            ) { acik ->
                // v7.56: yonetici kilidi — kapatmak izne bagli, acmak serbest
                if (kilitliUyar(OnlineStore.Islem.BILDIRIM_TUM_KAPAT, acik)) {
                    ekraniKur()
                } else {
                    Store.setNotifEnabled(this, acik)
                    if (acik) BildirimZamanlayici.kur(this) else BildirimZamanlayici.iptal(this)
                    ekraniKur()
                }
            }
        )
        // v7.56: odada kilit varsa aciklama serididi
        if (BildirimKilit.herhangiKilitVar(this)) {
            kap.addView(bilgi(getString(R.string.nset_kilit_bilgi)))
        }
        if (!anaAcik) {
            kap.addView(bilgi(getString(R.string.nset_master_off)))
        }

        // v7.63: bildirim gelmiyorsa once buraya bak
        val sorun = try { BildirimTani.sorunSayisi(this) } catch (_: Exception) { 0 }
        kap.addView(
            dugme(
                if (sorun == 0) getString(R.string.bt_menu) + "  ✓"
                else getString(R.string.bt_sorun_var, sorun)
            ) { BildirimTaniActivity.ac(this) }
        )

        // v7.44: kaç tür açık — tek bakışta durum
        val acikSayi = BildirimMerkezi.Tur.entries.count {
            BildirimMerkezi.acikMi(this, it)
        }
        kap.addView(
            bilgi(
                getString(
                    R.string.nset_summary, acikSayi, BildirimMerkezi.Tur.entries.size
                )
            )
        )

        // v7.44: ses ve titreşim de burada olsun (eski pencereden taşındı)
        // v7.56: yönetici kilidi eklendi
        kap.addView(
            anahtar(
                BildirimKilit.etiket(
                    this, getString(R.string.notif_sound), OnlineStore.Islem.SES_KAPAT
                ),
                "", Store.getSoundEnabled(this)
            ) { acik ->
                if (kilitliUyar(OnlineStore.Islem.SES_KAPAT, acik)) ekraniKur()
                else Store.setSoundEnabled(this, acik)
            }
        )
        kap.addView(
            anahtar(
                BildirimKilit.etiket(
                    this, getString(R.string.notif_vib), OnlineStore.Islem.TITRESIM_KAPAT
                ),
                "", Store.getVibEnabled(this)
            ) { acik ->
                if (kilitliUyar(OnlineStore.Islem.TITRESIM_KAPAT, acik)) ekraniKur()
                else Store.setVibEnabled(this, acik)
            }
        )

        // ── v7.56: Israrlı uyarı (sessizde de çalar) ──
        kap.addView(
            dugme(
                getString(R.string.zu_menu) +
                    if (ZorunluUyari.acikMi(this)) "  ✓" else ""
            ) { ZorunluUyariActivity.ac(this) }
        )

        // v7.44: toplu aç/kapat — 20 anahtarı tek tek çevirmek zahmetli
        kap.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(
                    dugme(getString(R.string.nset_all_on)) { tumunuAyarla(true) }.apply {
                        layoutParams = LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                        )
                    }
                )
                addView(
                    dugme(getString(R.string.nset_all_off)) { tumunuAyarla(false) }.apply {
                        layoutParams = LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                        )
                    }
                )
                addView(
                    dugme(getString(R.string.nset_reset)) { varsayilanaDon() }.apply {
                        layoutParams = LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                        )
                    }
                )
            }
        )

        kap.addView(ayirici())

        // ── 27. Rahatsız etmeyin ──
        kap.addView(baslik(getString(R.string.nset_quiet), 15f))
        kap.addView(
            anahtar(
                getString(R.string.nset_quiet),
                getString(
                    R.string.nset_quiet_desc,
                    BildirimMerkezi.sessizBaslangic(this),
                    BildirimMerkezi.sessizBitis(this)
                ),
                BildirimMerkezi.sessizModAcik(this)
            ) { acik ->
                BildirimMerkezi.setSessizMod(this, acik)
                ekraniKur()
            }
        )
        kap.addView(
            dugme(getString(R.string.nset_quiet_set)) { sessizSaatSec() }
        )

        // ── v10.15 · C14: tür bazlı sessiz pencereler ──
        kap.addView(baslik(getString(R.string.gc_sessiz_baslik), 15f))
        kap.addView(bilgi(getString(R.string.gc_sessiz_aciklama)))
        SessizTurler.Tur.values()
            .filter { it != SessizTurler.Tur.DIGER }
            .forEach { tur -> kap.addView(turSatiri(tur)) }

        // ── v10.15 · C15: kilit ekranı gün paneli anahtarı ──
        kap.addView(ayirici())
        kap.addView(
            anahtar(
                getString(R.string.gc_panel_baslik),
                getString(R.string.gc_panel_aciklama),
                GunPaneli.acikMi(this)
            ) { acik ->
                GunPaneli.ayarla(this, acik)
                ekraniKur()
            }
        )

        // ── v10.3 · B24: Sessizlik / DND haritası ──
        // "Sessiz saatte ne olur?" sorusunun cevabı kodda dağınıktı:
        // merkez kapısı gonder()'de, sayaç bitişi acil bayrağında,
        // namaz kendi kanalında. Burada tek yerde okunuyor.
        kap.addView(baslik(getString(R.string.bh_baslik), 15f))
        kap.addView(bilgi(getString(R.string.bh_giris)))
        kap.addView(bilgi(getString(R.string.bh_merkez)))
        kap.addView(bilgi(getString(R.string.bh_sayac)))
        kap.addView(bilgi(getString(R.string.bh_gorev)))
        kap.addView(bilgi(getString(R.string.bh_namaz)))
        kap.addView(bilgi(getString(R.string.bh_rapor)))
        kap.addView(
            dugme(getString(R.string.bh_dnd)) {
                try {
                    startActivity(
                        Intent(
                            android.provider.Settings
                                .ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
                        )
                    )
                } catch (e: Exception) {
                    android.widget.Toast.makeText(
                        this, R.string.bh_dnd_acilmadi, android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )

        // ── Bildirim saatleri ──
        kap.addView(
            dugme(
                getString(
                    R.string.nset_times_desc,
                    BildirimZamanlayici.sabahSaati(this),
                    BildirimZamanlayici.aksamSaati(this)
                )
            ) { turSaatiSec() }
        )

        // ── Günlük tavan ──
        kap.addView(
            dugme(getString(R.string.nset_cap, BildirimMerkezi.gunlukTavan(this))) {
                tavanSec()
            }
        )
        kap.addView(bilgi(getString(R.string.nset_cap_desc)))

        kap.addView(ayirici())

        // ── Tür bazlı anahtarlar, gruplanmış ──
        val gruplar = linkedMapOf(
            R.string.ng_hatirlatici to listOf(
                BildirimMerkezi.Tur.GOREV, BildirimMerkezi.Tur.KURS_GUNLUK
            ),
            R.string.ng_ogrenme to listOf(
                BildirimMerkezi.Tur.KART_TEKRAR, BildirimMerkezi.Tur.QUIZ_TEKRAR,
                BildirimMerkezi.Tur.YARIM_DERS, BildirimMerkezi.Tur.UNUTMA,
                BildirimMerkezi.Tur.GUNLUK_KART, BildirimMerkezi.Tur.SINAV_SAYAC
            ),
            R.string.ng_basarim to listOf(
                BildirimMerkezi.Tur.ROZET, BildirimMerkezi.Tur.HEDEF_TAMAM,
                BildirimMerkezi.Tur.SERI_REKOR, BildirimMerkezi.Tur.SERI_RISK,
                BildirimMerkezi.Tur.HEDEF_ILERLEME, BildirimMerkezi.Tur.GERI_DONUS,
                BildirimMerkezi.Tur.ODAK_ONERI, BildirimMerkezi.Tur.UZUN_OTURUM
            ),
            R.string.ng_rapor to listOf(
                BildirimMerkezi.Tur.HAFTALIK, BildirimMerkezi.Tur.AYLIK
            ),
            R.string.ng_arkaplan to listOf(
                BildirimMerkezi.Tur.ARKAPLAN_IS, BildirimMerkezi.Tur.YEDEK
            )
        )

        gruplar.forEach { (grupAd, turler) ->
            // v7.44: grup başlığı + o grubun tamamını çeviren kısayol
            val grupAcik = turler.count { BildirimMerkezi.acikMi(this, it) }
            kap.addView(
                LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    addView(
                        baslik(getString(grupAd), 15f).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                            )
                        }
                    )
                    addView(
                        dugme(
                            if (grupAcik == turler.size) getString(R.string.nset_group_off)
                            else getString(R.string.nset_group_on)
                        ) {
                            val hedef = grupAcik != turler.size
                            turler.forEach { BildirimMerkezi.ayarla(this@BildirimAyarActivity, it, hedef) }
                            ekraniKur()
                        }
                    )
                }
            )
            turler.forEach { tur ->
                kap.addView(
                    anahtar(
                        getString(tur.adRes),
                        getString(tur.aciklamaRes),
                        BildirimMerkezi.acikMi(this, tur)
                    ) { acik ->
                        BildirimMerkezi.ayarla(this, tur, acik)
                        // Özet sayısı güncellensin
                        ekraniKur()
                    }
                )
            }
            kap.addView(ayirici())
        }

        // ── Test ve sistem ayarları ──
        kap.addView(dugme(getString(R.string.nset_test)) { testGonder() })
        kap.addView(dugme(getString(R.string.nset_sys)) { sistemAyarlariniAc() })
    }

    // ═══════════════════════════════════════════════════════════════
    // v7.44 — TOPLU İŞLEMLER
    // ═══════════════════════════════════════════════════════════════

    /** 20 türü tek seferde açar veya kapatır. */
    private fun tumunuAyarla(acik: Boolean) {
        BildirimMerkezi.Tur.entries.forEach { BildirimMerkezi.ayarla(this, it, acik) }
        ekraniKur()
        Toast.makeText(
            this,
            if (acik) R.string.nset_all_on_done else R.string.nset_all_off_done,
            Toast.LENGTH_SHORT
        ).show()
    }

    /** Fabrika ayarlarına döner — her tür kendi varsayılanına. */
    private fun varsayilanaDon() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.nset_reset)
            .setMessage(R.string.nset_reset_ask)
            .setPositiveButton(R.string.cmd_confirm_yes) { _, _ ->
                BildirimMerkezi.Tur.entries.forEach {
                    BildirimMerkezi.ayarla(this, it, it.varsayilanAcik)
                }
                BildirimMerkezi.setSessizMod(this, true)
                BildirimMerkezi.setSessizSaatler(this, 23, 8)
                BildirimMerkezi.setGunlukTavan(this, 6)
                BildirimZamanlayici.setSaatler(this, 9, 19)
                ekraniKur()
                Toast.makeText(this, R.string.nset_reset_done, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ═══════════════════════════════════════════════════════════════
    // SEÇİM PENCERELERİ
    // ═══════════════════════════════════════════════════════════════

    private fun sessizSaatSec() {
        val bas = NumberPicker(this).apply {
            minValue = 0; maxValue = 23
            value = BildirimMerkezi.sessizBaslangic(this@BildirimAyarActivity)
        }
        val bit = NumberPicker(this).apply {
            minValue = 0; maxValue = 23
            value = BildirimMerkezi.sessizBitis(this@BildirimAyarActivity)
        }
        ikiliSecici(
            getString(R.string.nset_quiet),
            getString(R.string.nset_hour_start), bas,
            getString(R.string.nset_hour_end), bit
        ) {
            BildirimMerkezi.setSessizSaatler(this, bas.value, bit.value)
            ekraniKur()
        }
    }

    private fun turSaatiSec() {
        val sabah = NumberPicker(this).apply {
            minValue = 0; maxValue = 23
            value = BildirimZamanlayici.sabahSaati(this@BildirimAyarActivity)
        }
        val aksam = NumberPicker(this).apply {
            minValue = 0; maxValue = 23
            value = BildirimZamanlayici.aksamSaati(this@BildirimAyarActivity)
        }
        ikiliSecici(
            getString(R.string.nset_times),
            getString(R.string.nset_morning), sabah,
            getString(R.string.nset_evening), aksam
        ) {
            BildirimZamanlayici.setSaatler(this, sabah.value, aksam.value)
            ekraniKur()
        }
    }

    private fun ikiliSecici(
        baslikMetni: String,
        etiket1: String, secici1: NumberPicker,
        etiket2: String, secici2: NumberPicker,
        onay: () -> Unit
    ) {
        val govde = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding((20 * yogunluk).toInt(), (10 * yogunluk).toInt(),
                (20 * yogunluk).toInt(), 0)
            addView(
                LinearLayout(this@BildirimAyarActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    addView(TextView(this@BildirimAyarActivity).apply {
                        text = etiket1; textSize = 13f
                        gravity = android.view.Gravity.CENTER
                    })
                    addView(secici1)
                }
            )
            addView(
                LinearLayout(this@BildirimAyarActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    addView(TextView(this@BildirimAyarActivity).apply {
                        text = etiket2; textSize = 13f
                        gravity = android.view.Gravity.CENTER
                    })
                    addView(secici2)
                }
            )
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(baslikMetni)
            .setView(govde)
            .setPositiveButton(R.string.save) { _, _ -> onay() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** v10.15 · C14: tek tür satırı (anahtar + saat düzenleme). */
    private fun turSatiri(tur: SessizTurler.Tur): View {
        val p = SessizTurler.oku(this, tur)
        val ad = getString(
            when (tur) {
                SessizTurler.Tur.GOREV -> R.string.gc_tur_gorev
                SessizTurler.Tur.SAYAC -> R.string.gc_tur_sayac
                SessizTurler.Tur.RAPOR -> R.string.gc_tur_rapor
                SessizTurler.Tur.MOTIVASYON -> R.string.gc_tur_motivasyon
                SessizTurler.Tur.DIGER -> R.string.gc_tur_diger
            }
        )
        val ozet = if (!p.acik) {
            getString(R.string.gc_tur_ozet_kapali)
        } else {
            getString(R.string.gc_tur_ozet, p.bas / 60, p.bas % 60, p.bit / 60, p.bit % 60) +
                if (p.haftaSonuAyrimi) {
                    getString(R.string.gc_tur_ozet_hs, p.hsBas / 60, p.hsBas % 60, p.hsBit / 60, p.hsBit % 60)
                } else ""
        }
        val govde = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        govde.addView(
            anahtar(ad, ozet, p.acik) { yeni ->
                SessizTurler.yaz(this, tur, p.copy(acik = yeni))
                ekraniKur()
            }
        )
        govde.addView(dugme(getString(R.string.gc_tur_sec)) { turSaatleriSec(tur) })
        return govde
    }

    /** v10.15 · C14: baş/bitiş + hafta sonu ayrımı seçici diyaloğu. */
    private fun turSaatleriSec(tur: SessizTurler.Tur) {
        val eski = SessizTurler.oku(this, tur)
        val dp = resources.displayMetrics.density
        fun np(saatDk: Int) = NumberPicker(this).apply {
            minValue = 0; maxValue = 23
            value = (saatDk / 60).coerceIn(0, 23)
        }
        val pBas = np(eski.bas); val pBit = np(eski.bit)
        val pHsBas = np(eski.hsBas); val pHsBit = np(eski.hsBit)
        val hsAnahtar = MaterialSwitch(this).apply {
            text = getString(R.string.gc_hs_ayrimi)
            isChecked = eski.haftaSonuAyrimi
        }
        val hsSatir = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            visibility = if (eski.haftaSonuAyrimi) View.VISIBLE else View.GONE
            addView(pHsBas); addView(pHsBit)
        }
        hsAnahtar.setOnCheckedChangeListener { _, b ->
            hsSatir.visibility = if (b) View.VISIBLE else View.GONE
        }
        val kok = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((20 * dp).toInt(), (6 * dp).toInt(), (20 * dp).toInt(), 0)
            addView(
                LinearLayout(this@BildirimAyarActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER
                    addView(pBas); addView(pBit)
                }
            )
            addView(hsAnahtar)
            addView(hsSatir)
        }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.gc_sessiz_baslik)
            .setView(kok)
            .setPositiveButton(R.string.gc_kaydet) { _, _ ->
                SessizTurler.yaz(
                    this, tur, eski.copy(
                        acik = true,
                        bas = pBas.value * 60, bit = pBit.value * 60,
                        haftaSonuAyrimi = hsAnahtar.isChecked,
                        hsBas = pHsBas.value * 60, hsBit = pHsBit.value * 60
                    )
                )
                ekraniKur()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun tavanSec() {
        val secici = NumberPicker(this).apply {
            minValue = 1; maxValue = 20
            value = BildirimMerkezi.gunlukTavan(this@BildirimAyarActivity)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.nset_cap_desc)
            .setView(secici)
            .setPositiveButton(R.string.save) { _, _ ->
                BildirimMerkezi.setGunlukTavan(this, secici.value)
                ekraniKur()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun testGonder() {
        val ok = BildirimMerkezi.gonder(
            this,
            BildirimMerkezi.Tur.ROZET,
            9999,
            getString(R.string.nset_title),
            getString(R.string.nset_test_sent),
            acil = true
        )
        Toast.makeText(
            this,
            if (ok) R.string.nset_test_sent else R.string.nset_test_fail,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun sistemAyarlariniAc() {
        try {
            startActivity(
                Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)
            )
        } catch (e: Exception) {
            android.util.Log.w("BildirimAyar", "Sistem ayarları açılamadı", e)
            try {
                startActivity(
                    Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(android.net.Uri.parse("package:" + packageName))
                )
            } catch (_: Exception) {
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GÖRSEL YARDIMCILAR
    // ═══════════════════════════════════════════════════════════════

    /**
     * v7.56: Kilitli ayarı değiştirmeye çalışan üyeyi uyarır.
     * @return true ise engellendi — çağıran işlemi yapmamalı
     */
    private fun kilitliUyar(islem: OnlineStore.Islem, acmaIstegi: Boolean): Boolean =
        BildirimKilit.engellendiMi(this, islem, acmaIstegi) { mesaj ->
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.nset_kilitli)
                .setMessage(mesaj)
                .setPositiveButton(R.string.done, null)
                .show()
        }

    private fun baslik(metin: String, boyut: Float) = TextView(this).apply {
        text = metin
        textSize = boyut
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, (14 * yogunluk).toInt(), 0, (6 * yogunluk).toInt())
    }

    private fun bilgi(metin: String) = TextView(this).apply {
        text = metin
        textSize = 12f
        alpha = 0.7f
        setPadding(0, 0, 0, (8 * yogunluk).toInt())
    }

    private fun ayirici() = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, (1 * yogunluk).toInt()
        ).apply {
            topMargin = (10 * yogunluk).toInt()
            bottomMargin = (4 * yogunluk).toInt()
        }
        setBackgroundColor(
            (MaterialColors.getColor(
                this@BildirimAyarActivity,
                com.google.android.material.R.attr.colorOnSurface, 0
            ) and 0x00FFFFFF) or 0x22000000
        )
    }

    private fun anahtar(
        ad: String,
        aciklama: String,
        acik: Boolean,
        degisince: (Boolean) -> Unit
    ): View {
        val satir = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, (7 * yogunluk).toInt(), 0, (7 * yogunluk).toInt())
        }
        satir.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                )
                addView(TextView(this@BildirimAyarActivity).apply {
                    text = ad
                    textSize = 14f
                })
                if (aciklama.isNotBlank()) {
                    addView(TextView(this@BildirimAyarActivity).apply {
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
                setOnCheckedChangeListener { _, yeni -> degisince(yeni) }
            }
        )
        return satir
    }

    private fun dugme(metin: String, tiklayinca: () -> Unit) = TextView(this).apply {
        text = metin
        textSize = 13.5f
        setTextColor(
            MaterialColors.getColor(
                this@BildirimAyarActivity,
                com.google.android.material.R.attr.colorPrimary, 0
            )
        )
        setPadding(0, (10 * yogunluk).toInt(), 0, (10 * yogunluk).toInt())
        background = android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf(0x22888888), null, null
        )
        isClickable = true
        setOnClickListener { tiklayinca() }
    }
}
