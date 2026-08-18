package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import java.util.Calendar

/**
 * v7.78 — Koç ekranı: karne, hesap verme ve ayarlar.
 *
 * ── Üç bölüm ──
 *   1. **Bugün**  — hedef, yapılan, kalan, borç. En üstte çünkü en sık bakılan.
 *   2. **Karne**  — son 30 gün, seri, rekor. Baskının görünür yüzü.
 *   3. **Ayarlar** — sertlik, saatler, günler, kanıt.
 *
 * ── Hesap modu ──
 * Bildirimden `EXTRA_HESAP` ile açılırsa doğrudan hesap sorma penceresi
 * açılır. Kullanıcı ekranı arayıp bulmak zorunda kalmasın.
 */
class KocActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_HESAP = "hesap"

        fun ac(context: Context) {
            context.startActivity(Intent(context, KocActivity::class.java))
        }
    }

    private val d get() = resources.displayMetrics.density
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
            setPadding((16 * d).toInt(), (16 * d).toInt(), (16 * d).toInt(), (28 * d).toInt())
        }
        setContentView(
            ScrollView(this).apply {
                setBackgroundColor(
                    MaterialColors.getColor(
                        this@KocActivity,
                        com.google.android.material.R.attr.colorSurface, 0
                    )
                )
                addView(kap)
            }
        )

        Koc.gecmisiDenkleştir(this)
        ciz()

        if (intent.getBooleanExtra(EXTRA_HESAP, false)) {
            kap.post { hesapVer() }
        }
    }

    override fun onResume() {
        super.onResume()
        ciz()
    }

    // ═══════════════════════════════════════════════════════════════
    // ÇİZİM
    // ═══════════════════════════════════════════════════════════════

    private fun ciz() {
        kap.removeAllViews()

        baslik(getString(R.string.koc_baslik))

        // ── Ana anahtar ────────────────────────────────────────────
        kap.addView(anahtarKart(
            getString(R.string.koc_acik),
            getString(R.string.koc_acik_alt),
            Koc.acikMi(this)
        ) { acik ->
            Koc.setAcik(this, acik)
            ciz()
        })

        if (!Koc.acikMi(this)) {
            kap.addView(bilgi(getString(R.string.koc_kapali_bilgi)))
            return
        }

        // ── 0. PROGRAM (v7.79) ─────────────────────────────────────
        ayirici()
        baslikKucuk(getString(R.string.mf_baslik))
        kap.addView(programKarti())

        // ── 1. BUGÜN ───────────────────────────────────────────────
        ayirici()
        baslikKucuk(getString(R.string.koc_bugun))
        kap.addView(bugunKarti())

        if (!Koc.bugunCalismaGunuMu(this)) {
            kap.addView(bilgi(getString(R.string.koc_bugun_tatil)))
        }

        // Hesap verilmemişse düğme
        if (!Koc.kapatildiMi(this) && !Koc.bugunTamamMi(this)) {
            kap.addView(dugme(getString(R.string.koc_hesap_ver), vurgulu = true) { hesapVer() })
        }
        kap.addView(dugme(getString(R.string.koc_ne_calisayim)) { neCalisayim() })
        // v7.79: program düğmeleri
        if (Mufredat.secildiMi(this)) {
            if (Mufredat.aktifAdim(this) != null) {
                kap.addView(
                    dugme(
                        getString(
                            if (konuMu()) R.string.mf_konuyu_bitir
                            else R.string.mf_dersi_bitir
                        ),
                        vurgulu = true
                    ) { dersiBitirSor() }
                )
            }
            // v7.81: aktif konuyu derinlemesine anlat
            Mufredat.aktifAdim(this)?.let { aktif ->
                kap.addView(dugme(getString(R.string.ku_anlat)) {
                    KonuAnlatimActivity.ac(
                        this, aktif.baslik, Mufredat.programAdi(this)
                    )
                })
            }
            kap.addView(
                dugme(
                    getString(if (konuMu()) R.string.mf_maddeler else R.string.mf_dersler)
                ) { dersListesi() }
            )
            kap.addView(dugme(getString(R.string.mf_kurs_degistir)) { programSec() })
        }

        // ── 2. KARNE ───────────────────────────────────────────────
        ayirici()
        baslikKucuk(getString(R.string.koc_karne))
        kap.addView(karneKarti())
        kap.addView(dugme(getString(R.string.koc_gecmis)) { gecmisGoster() })
        // v7.83: hata defteri — bugün tekrar edilecek soru varsa vurgula
        val hOzet = Hatalarim.ozet(this)
        if (hOzet.toplam > 0) {
            kap.addView(
                dugme(
                    if (hOzet.bugun > 0) getString(R.string.ht_tekrar_et, hOzet.bugun)
                    else getString(R.string.ht_row) + " (" + hOzet.toplam + ")",
                    vurgulu = hOzet.bugun > 0
                ) { HatalarimActivity.ac(this) }
            )
        }

        // ── 3. AYARLAR ─────────────────────────────────────────────
        ayirici()
        baslikKucuk(getString(R.string.koc_ayarlar))

        kap.addView(satir(
            getString(R.string.koc_sertlik),
            Koc.sertlikAdi(this, Koc.sertlik(this))
        ) { sertlikSec() })

        kap.addView(satir(
            getString(R.string.koc_hedef),
            getString(R.string.koc_dk, Koc.gunlukHedef(this))
        ) { hedefSec() })

        kap.addView(satir(
            getString(R.string.koc_calisma_saati),
            String.format("%02d:00", Koc.calismaSaati(this))
        ) { saatSec(true) })

        kap.addView(satir(
            getString(R.string.koc_hesap_saati),
            String.format("%02d:00", Koc.hesapSaati(this))
        ) { saatSec(false) })

        kap.addView(satir(
            getString(R.string.koc_gunler),
            gunlerMetni()
        ) { gunlerSec() })

        // v7.97: haftalık plan (öneri 6)
        kap.addView(satir(
            getString(R.string.hp_baslik),
            HaftaPlan.ozet(this)
        ) { HaftaPlanActivity.ac(this) })

        kap.addView(anahtarKart(
            getString(R.string.koc_kanit),
            getString(R.string.koc_kanit_alt),
            Koc.kanitIster(this)
        ) { Koc.setKanitIster(this, it) })

        // Acımasız modda zorunlu uyarı bağlantısı
        if (Koc.sertlik(this) == Koc.SERT_ACIMASIZ) {
            kap.addView(bilgi(
                if (ZorunluUyari.acikMi(this)) getString(R.string.koc_zorunlu_acik)
                else getString(R.string.koc_zorunlu_kapali)
            ))
            if (!ZorunluUyari.acikMi(this)) {
                kap.addView(dugme(getString(R.string.koc_zorunlu_ac)) {
                    ZorunluUyariActivity.ac(this)
                })
            }
        }
    }

    // ── Program kartı (v7.79) ──────────────────────────────────────

    /**
     * Koçun takip ettiği kurs ve aktif ders.
     *
     * Kullanıcının isteği: "belirlediğim derslere yönelim sağlasın,
     * ders ders bitirtsin, karmakarışık program yapmasın."
     * Bu kart o programın görünen yüzü.
     */
    private fun programKarti(): View {
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((14 * d).toInt(), (14 * d).toInt(), (14 * d).toInt(), (14 * d).toInt())
        }

        if (!Mufredat.secildiMi(this)) {
            ic.addView(TextView(this).apply {
                text = getString(R.string.mf_kurs_secilmedi)
                textSize = 15f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            ic.addView(TextView(this).apply {
                text = getString(R.string.mf_kurs_sec_bilgi)
                textSize = 12.5f
                alpha = 0.75f
                setLineSpacing(0f, 1.2f)
                setPadding(0, (6 * d).toInt(), 0, 0)
            })
            val kart = kartSar(ic)
            (kart as MaterialCardView).isClickable = true
            kart.setOnClickListener { programSec() }
            return kart
        }

        val ilerleme = Mufredat.ilerleme(this)

        ic.addView(TextView(this).apply {
            // v7.80: kurs mu konu mu belli olsun
            text = (if (Mufredat.kaynakTuru(this@KocActivity) == Mufredat.KAYNAK_KONU)
                "📝 " else "📚 ") + Mufredat.programAdi(this@KocActivity)
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })

        if (ilerleme.toplam == 0) {
            ic.addView(TextView(this).apply {
                text = getString(R.string.mf_bos_program)
                textSize = 12.5f
                alpha = 0.75f
                setPadding(0, (6 * d).toInt(), 0, 0)
            })
            return kartSar(ic).apply {
                isClickable = true
                setOnClickListener { programSec() }
            }
        }

        if (ilerleme.bittiMi) {
            ic.addView(TextView(this).apply {
                text = getString(R.string.mf_bitti_kutlama, ilerleme.toplam)
                textSize = 13.5f
                setPadding(0, (8 * d).toInt(), 0, 0)
                setTextColor(
                    MaterialColors.getColor(
                        this@KocActivity,
                        com.google.android.material.R.attr.colorPrimary, 0
                    )
                )
            })
        } else {
            // Aktif ders — en önemli bilgi
            ic.addView(TextView(this).apply {
                text = getString(
                    if (Mufredat.kaynakTuru(this@KocActivity) == Mufredat.KAYNAK_KONU)
                        R.string.mf_su_an_konu else R.string.mf_su_an
                )
                textSize = 11.5f
                alpha = 0.7f
                setPadding(0, (10 * d).toInt(), 0, (2 * d).toInt())
            })
            ic.addView(TextView(this).apply {
                text = ilerleme.aktifAd
                textSize = 15.5f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(
                    MaterialColors.getColor(
                        this@KocActivity,
                        com.google.android.material.R.attr.colorPrimary, 0
                    )
                )
            })
            if (Mufredat.kilitliMi(this)) {
                ic.addView(TextView(this).apply {
                    text = getString(R.string.mf_kilitli)
                    textSize = 11.5f
                    alpha = 0.8f
                    setPadding(0, (3 * d).toInt(), 0, 0)
                })
            }
            val kayit = Mufredat.adimKaydi(this, Mufredat.aktifAdim(this)?.id ?: 0L)
            if (kayit.dakika > 0) {
                ic.addView(TextView(this).apply {
                    text = getString(R.string.mf_ders_sure, kayit.dakika, kayit.oturum)
                    textSize = 11.5f
                    alpha = 0.75f
                    setPadding(0, (3 * d).toInt(), 0, 0)
                })
            }
        }

        // İlerleme çubuğu
        ic.addView(TextView(this).apply {
            text = getString(R.string.mf_ilerleme, ilerleme.biten, ilerleme.toplam, ilerleme.yuzde)
            textSize = 12f
            alpha = 0.8f
            setPadding(0, (10 * d).toInt(), 0, (5 * d).toInt())
        })
        ic.addView(ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = ilerleme.yuzde
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (8 * d).toInt()
            )
        })

        return kartSar(ic)
    }

    /**
     * Koçun takip edeceği programı seçtirir.
     *
     * v7.80: artık yalnızca kurslar değil, **Konular sekmesindeki konular**
     * da seçilebilir. Kullanıcının isteği: "sadece mühendislik kursları vb
     * şeylerde yardımcı olmasın, konular kısmındaki konuları da seçme
     * hakkım olsun."
     */
    private fun programSec() {
        val kurslar = Store.loadCourses(this).sortedBy { it.order }
        val konular = Store.loadTopics(this)

        if (kurslar.isEmpty() && konular.isEmpty()) {
            Toast.makeText(this, R.string.mf_hic_yok, Toast.LENGTH_LONG).show()
            return
        }

        // Tek listede birleştir: önce kurslar, sonra konular.
        // Ayrı iki diyalog yerine tek liste — kullanıcı "hangi sekmedeydi"
        // diye düşünmek zorunda kalmasın.
        data class Secenek(val tur: Int, val id: Long, val etiket: String)

        val secenekler = mutableListOf<Secenek>()
        kurslar.forEach { k ->
            val d = Store.loadLessons(this).filter { it.courseId == k.id }
            secenekler.add(
                Secenek(
                    Mufredat.KAYNAK_KURS, k.id,
                    "📚 " + k.emoji + " " + k.title + "  (" +
                        d.count { it.done } + "/" + d.size + ")"
                )
            )
        }
        konular.forEach { t ->
            secenekler.add(
                Secenek(
                    Mufredat.KAYNAK_KONU, t.id,
                    "📝 " + t.title + "  (" + t.doneCount + "/" + t.items.size + ")"
                )
            )
        }

        val simdiki = secenekler.indexOfFirst {
            it.tur == Mufredat.kaynakTuru(this) && it.id == Mufredat.kaynakId(this)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.mf_program_sec)
            .setSingleChoiceItems(
                secenekler.map { it.etiket }.toTypedArray(), simdiki
            ) { dlg, hangi ->
                val sec = secenekler[hangi]
                if (sec.tur == Mufredat.KAYNAK_KONU) {
                    val konu = konular.firstOrNull { it.id == sec.id }
                    if (konu != null && konu.items.isEmpty()) {
                        Toast.makeText(
                            this, R.string.mf_konu_bos, Toast.LENGTH_LONG
                        ).show()
                        return@setSingleChoiceItems
                    }
                }
                Mufredat.kaynakSec(this, sec.tur, sec.id)
                dlg.dismiss()
                ciz()
            }
            .setNeutralButton(R.string.mf_kurs_kaldir) { _, _ ->
                Mufredat.kaynagiKaldir(this)
                ciz()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun konuMu(): Boolean =
        Mufredat.kaynakTuru(this) == Mufredat.KAYNAK_KONU

    /** Adım listesi — sıradakini değiştirmek veya geri almak için. */
    private fun dersListesi() {
        val dersler = Mufredat.adimlar(this)
        if (dersler.isEmpty()) {
            Toast.makeText(this, R.string.mf_bos_program, Toast.LENGTH_SHORT).show()
            return
        }
        val aktifId = Mufredat.aktifAdim(this)?.id
        val adlar = dersler.mapIndexed { i, ders ->
            val isaret = when {
                ders.bitti -> "✓"
                ders.id == aktifId -> "▶"
                else -> "○"
            }
            "$isaret ${i + 1}. ${ders.baslik}"
        }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle(if (konuMu()) R.string.mf_maddeler else R.string.mf_dersler)
            .setItems(adlar) { _, hangi -> dersSecenekleri(dersler[hangi]) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun dersSecenekleri(ders: Mufredat.Adim) {
        val secenekler = mutableListOf<String>()
        secenekler.add(getString(R.string.ku_anlat))
        if (!ders.bitti) secenekler.add(getString(R.string.mf_bu_derse_odaklan))
        secenekler.add(
            getString(if (ders.bitti) R.string.mf_geri_al else R.string.mf_bitir)
        )

        MaterialAlertDialogBuilder(this)
            .setTitle(ders.baslik)
            .setItems(secenekler.toTypedArray()) { _, hangi ->
                when (secenekler[hangi]) {
                    getString(R.string.ku_anlat) ->
                        KonuAnlatimActivity.ac(this, ders.baslik, Mufredat.programAdi(this))
                    getString(R.string.mf_bu_derse_odaklan) -> {
                        Mufredat.adimKilitle(this, ders.id)
                        Toast.makeText(
                            this,
                            getString(R.string.mf_odaklanildi, ders.baslik),
                            Toast.LENGTH_SHORT
                        ).show()
                        ciz()
                    }
                    getString(R.string.mf_geri_al) -> {
                        Mufredat.adimDurumu(this, ders.id, false)
                        ciz()
                    }
                    else -> dersiBitirSor()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Aktif dersi bitirme — koç önce hesap sorar.
     *
     * "Sadece o konuyu hesap yapsın" isteği burada karşılanıyor:
     * ders bitmeden önce o dersin içeriğinden bir soru sorulur.
     */
    private fun dersiBitirSor() {
        val aktif = Mufredat.aktifAdim(this)
        if (aktif == null) {
            Toast.makeText(this, R.string.mf_aktif_yok, Toast.LENGTH_SHORT).show()
            return
        }

        // Nazik modda soru sorulmaz — doğrudan bitir
        if (Koc.sertlik(this) == Koc.SERT_NAZIK) {
            dersiKapat(aktif)
            return
        }

        val bekle = MaterialAlertDialogBuilder(this)
            .setMessage(R.string.mf_soru_hazirlaniyor)
            .setCancelable(false)
            .create()
        bekle.show()

        Performans.arkaPlan {
            val soru = KocMesaj.dersHesabiSorusu(this)
            Performans.anaIs {
                if (isFinishing || isDestroyed) return@anaIs
                runCatching { bekle.dismiss() }
                if (soru.isBlank()) dersiKapat(aktif) else dersSorusuSor(aktif, soru)
            }
        }
    }

    private fun dersSorusuSor(ders: Mufredat.Adim, soru: String) {
        val giris = EditText(this).apply {
            hint = getString(R.string.mf_cevap_ipucu)
            setSingleLine(false)
            minLines = 3
        }
        val kutu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((22 * d).toInt(), (10 * d).toInt(), (22 * d).toInt(), 0)
            addView(giris)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.mf_hesap_baslik, ders.baslik))
            .setMessage(soru)
            .setView(kutu)
            .setPositiveButton(R.string.mf_cevapla) { _, _ ->
                dersCevabiniIsle(ders, soru, giris.text?.toString()?.trim().orEmpty())
            }
            .setNegativeButton(R.string.cancel, null)
            .setCancelable(false)
            .show()
    }

    private fun dersCevabiniIsle(ders: Mufredat.Adim, soru: String, cevap: String) {
        val bekle = MaterialAlertDialogBuilder(this)
            .setMessage(R.string.mf_cevap_degerlendiriliyor)
            .setCancelable(false)
            .create()
        bekle.show()

        Performans.arkaPlan {
            val (yeterli, karsilik) = KocMesaj.dersCevabiDegerlendir(this, soru, cevap)
            Performans.anaIs {
                if (isFinishing || isDestroyed) return@anaIs
                runCatching { bekle.dismiss() }

                if (yeterli) {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.mf_ders_gecti)
                        .setMessage(karsilik)
                        .setPositiveButton(R.string.ok) { _, _ -> dersiKapat(ders) }
                        .setOnDismissListener { dersiKapat(ders) }
                        .show()
                } else {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.mf_ders_kaldi)
                        .setMessage(
                            karsilik + "\n\n" + getString(R.string.mf_ders_kaldi_alt)
                        )
                        .setPositiveButton(R.string.mf_tekrar_cevapla) { _, _ ->
                            dersSorusuSor(ders, soru)
                        }
                        .setNegativeButton(R.string.mf_yine_de_bitir) { _, _ ->
                            dersiKapat(ders)
                        }
                        .show()
                }
            }
        }
    }

    private fun dersiKapat(ders: Mufredat.Adim) {
        val sonraki = Mufredat.aktifAdimiBitir(this)
        val mesaj = if (sonraki == null) {
            getString(R.string.mf_program_tamam)
        } else {
            getString(R.string.mf_sirada, sonraki.baslik)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.mf_ders_bitti, ders.baslik))
            .setMessage(mesaj)
            .setPositiveButton(R.string.ok, null)
            .setOnDismissListener {
                ciz()
                // v8.0: yanlış dersi bitirmek geri alınabilsin (öneri 6)
                GeriAl.sun(
                    kap,
                    getString(R.string.mf_ders_bitti, ders.baslik),
                    tazele = { ciz() }
                ) {
                    Mufredat.adimDurumu(this, ders.id, false)
                }
            }
            .show()
    }

    // ── Bugün kartı ────────────────────────────────────────────────

    private fun bugunKarti(): View {
        val yapilan = Koc.bugunCalisilan(this)
        val hedef = Koc.bugunHedefi(this)
        val kalan = Koc.bugunKalan(this)
        val yuzde = Koc.bugunYuzde(this)

        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((14 * d).toInt(), (14 * d).toInt(), (14 * d).toInt(), (14 * d).toInt())
        }

        ic.addView(TextView(this).apply {
            text = if (kalan == 0) getString(R.string.koc_tamamlandi)
            else getString(R.string.koc_kalan, kalan)
            textSize = 19f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(
                MaterialColors.getColor(
                    this@KocActivity,
                    if (kalan == 0) com.google.android.material.R.attr.colorPrimary
                    else com.google.android.material.R.attr.colorOnSurface, 0
                )
            )
        })

        ic.addView(TextView(this).apply {
            text = getString(R.string.koc_ilerleme, yapilan, hedef)
            textSize = 12.5f
            alpha = 0.75f
            setPadding(0, (4 * d).toInt(), 0, (10 * d).toInt())
        })

        ic.addView(ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = yuzde
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (8 * d).toInt()
            )
        })

        val borc = Koc.borc(this)
        if (borc > 0) {
            ic.addView(TextView(this).apply {
                text = getString(R.string.koc_borc, borc)
                textSize = 12.5f
                setPadding(0, (10 * d).toInt(), 0, 0)
                setTextColor(
                    MaterialColors.getColor(
                        this@KocActivity,
                        com.google.android.material.R.attr.colorError, 0
                    )
                )
            })
        }

        val erteleme = getSharedPreferences("koc_v1", Context.MODE_PRIVATE)
            .getInt("bugun_erteleme", 0)
        if (erteleme > 0) {
            ic.addView(TextView(this).apply {
                text = getString(R.string.koc_erteleme_sayisi, erteleme)
                textSize = 12f
                alpha = 0.8f
                setPadding(0, (6 * d).toInt(), 0, 0)
            })
        }

        return kartSar(ic)
    }

    // ── Karne kartı ────────────────────────────────────────────────

    private fun karneKarti(): View {
        val k = Koc.karne(this, 30)
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((14 * d).toInt(), (14 * d).toInt(), (14 * d).toInt(), (14 * d).toInt())
        }

        ic.addView(TextView(this).apply {
            text = getString(R.string.koc_seri, k.seri)
            textSize = 17f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })

        ic.addView(TextView(this).apply {
            text = getString(R.string.koc_rekor, k.rekor)
            textSize = 12.5f
            alpha = 0.75f
            setPadding(0, (3 * d).toInt(), 0, (10 * d).toInt())
        })

        if (k.toplamGun > 0) {
            ic.addView(TextView(this).apply {
                text = getString(R.string.koc_karne_ozet, k.basariliGun, k.toplamGun, k.yuzde)
                textSize = 13f
            })
            ic.addView(TextView(this).apply {
                text = getString(R.string.koc_toplam_dk, k.toplamDakika, k.toplamDakika / 60)
                textSize = 12.5f
                alpha = 0.75f
                setPadding(0, (4 * d).toInt(), 0, 0)
            })
        } else {
            ic.addView(TextView(this).apply {
                text = getString(R.string.koc_karne_bos)
                textSize = 12.5f
                alpha = 0.75f
            })
        }

        return kartSar(ic)
    }

    // ═══════════════════════════════════════════════════════════════
    // HESAP VERME
    // ═══════════════════════════════════════════════════════════════

    /**
     * Gün sonu hesabı.
     *
     * Hedef tutmuşsa kutlar. Tutmamışsa mazeret ister ve mazereti
     * [KocMesaj.mazeretDegerlendir] ile denetletir.
     */
    private fun hesapVer() {
        if (Koc.kapatildiMi(this)) {
            Toast.makeText(this, R.string.koc_zaten_kapali, Toast.LENGTH_SHORT).show()
            return
        }

        if (Koc.bugunTamamMi(this)) {
            Koc.gunuKapat(this)
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.koc_h_tebrik)
                .setMessage(Koc.hesapMetni(this))
                .setPositiveButton(R.string.ok, null)
                .setOnDismissListener { ciz() }
                .show()
            return
        }

        val giris = EditText(this).apply {
            hint = getString(R.string.koc_mazeret_ipucu)
            setSingleLine(false)
            minLines = 2
        }
        val kutu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((22 * d).toInt(), (10 * d).toInt(), (22 * d).toInt(), 0)
            addView(giris)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.koc_hesap_baslik)
            .setMessage(Koc.hesapMetni(this))
            .setView(kutu)
            .setPositiveButton(R.string.koc_mazeret_gonder) { _, _ ->
                mazeretiIsle(giris.text?.toString()?.trim().orEmpty())
            }
            .setNegativeButton(R.string.koc_mazeret_yok) { _, _ ->
                Koc.gunuKapat(this, "", false)
                Toast.makeText(this, R.string.koc_borc_yazildi, Toast.LENGTH_LONG).show()
                ciz()
            }
            .setNeutralButton(R.string.cancel, null)
            .show()
    }

    private fun mazeretiIsle(mazeret: String) {
        if (mazeret.isBlank()) {
            Koc.gunuKapat(this, "", false)
            Toast.makeText(this, R.string.koc_borc_yazildi, Toast.LENGTH_LONG).show()
            ciz()
            return
        }

        val bekle = MaterialAlertDialogBuilder(this)
            .setMessage(R.string.koc_mazeret_degerlendiriliyor)
            .setCancelable(false)
            .create()
        bekle.show()

        Performans.arkaPlan {
            val karar = KocMesaj.mazeretDegerlendir(this, mazeret)
            Performans.anaIs {
                if (isFinishing || isDestroyed) return@anaIs
                runCatching { bekle.dismiss() }

                Koc.gunuKapat(this, mazeret, karar.kabul)

                MaterialAlertDialogBuilder(this)
                    .setTitle(
                        if (karar.kabul) getString(R.string.koc_mazeret_kabul)
                        else getString(R.string.koc_mazeret_red)
                    )
                    .setMessage(
                        karar.cevap + "\n\n" + (
                            if (karar.kabul) getString(R.string.koc_borc_silindi)
                            else getString(R.string.koc_borc_eklendi, Koc.borc(this))
                            )
                    )
                    .setPositiveButton(R.string.ok, null)
                    .setOnDismissListener { ciz() }
                    .show()
            }
        }
    }

    private fun neCalisayim() {
        if (!AiSettings.isReady(this)) {
            Toast.makeText(this, R.string.kn_ai_hazir_degil, Toast.LENGTH_LONG).show()
            return
        }
        val bekle = MaterialAlertDialogBuilder(this)
            .setMessage(R.string.koc_plan_uretiliyor)
            .setCancelable(false)
            .create()
        bekle.show()

        Performans.arkaPlan {
            val plan = KocMesaj.bugunNeCalisayim(this)
            Performans.anaIs {
                if (isFinishing || isDestroyed) return@anaIs
                runCatching { bekle.dismiss() }
                if (plan.isBlank()) {
                    Toast.makeText(this, R.string.koc_plan_yok, Toast.LENGTH_LONG).show()
                } else {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.koc_ne_calisayim)
                        .setMessage(plan)
                        .setPositiveButton(R.string.ok, null)
                        .show()
                }
            }
        }
    }

    private fun gecmisGoster() {
        val kayitlar = Koc.gunKayitlari(this).takeLast(30).reversed()
        if (kayitlar.isEmpty()) {
            Toast.makeText(this, R.string.koc_karne_bos, Toast.LENGTH_SHORT).show()
            return
        }
        val metin = kayitlar.joinToString("\n") { g ->
            val isaret = if (g.basarili) "✓" else if (g.kabul) "~" else "✕"
            val tarih = g.gun.let {
                if (it.length == 8) "${it.substring(6)}.${it.substring(4, 6)}" else it
            }
            "$isaret $tarih  ${g.yapilan}/${g.hedef} dk" +
                (if (g.mazeret.isNotBlank()) "  · ${g.mazeret.take(30)}" else "")
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.koc_gecmis)
            .setMessage(metin)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    // ═══════════════════════════════════════════════════════════════
    // AYAR SEÇİCİLERİ
    // ═══════════════════════════════════════════════════════════════

    private fun sertlikSec() {
        val secenekler = arrayOf(
            getString(R.string.koc_sert_nazik) + " — " + getString(R.string.koc_sert_nazik_d),
            getString(R.string.koc_sert_kararli) + " — " + getString(R.string.koc_sert_kararli_d),
            getString(R.string.koc_sert_acimasiz) + " — " + getString(R.string.koc_sert_acimasiz_d)
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.koc_sertlik)
            .setSingleChoiceItems(secenekler, Koc.sertlik(this)) { dlg, hangi ->
                Koc.setSertlik(this, hangi)
                dlg.dismiss()
                ciz()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun hedefSec() {
        val degerler = intArrayOf(15, 30, 45, 60, 90, 120, 180, 240)
        val etiketler = degerler.map { getString(R.string.koc_dk, it) }.toTypedArray()
        val simdiki = degerler.indexOf(Koc.gunlukHedef(this)).coerceAtLeast(0)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.koc_hedef)
            .setSingleChoiceItems(etiketler, simdiki) { dlg, hangi ->
                Koc.setGunlukHedef(this, degerler[hangi])
                dlg.dismiss()
                ciz()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun saatSec(calismaMi: Boolean) {
        val etiketler = (0..23).map { String.format("%02d:00", it) }.toTypedArray()
        val simdiki = if (calismaMi) Koc.calismaSaati(this) else Koc.hesapSaati(this)
        MaterialAlertDialogBuilder(this)
            .setTitle(
                if (calismaMi) R.string.koc_calisma_saati else R.string.koc_hesap_saati
            )
            .setSingleChoiceItems(etiketler, simdiki) { dlg, hangi ->
                if (calismaMi) Koc.setCalismaSaati(this, hangi)
                else Koc.setHesapSaati(this, hangi)
                dlg.dismiss()
                ciz()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** Hafta günleri — Calendar sabitleriyle (1=Pazar … 7=Cumartesi). */
    private val gunSirasi = intArrayOf(2, 3, 4, 5, 6, 7, 1)

    private fun gunAdlari(): Array<String> = arrayOf(
        getString(R.string.gun_pzt), getString(R.string.gun_sal),
        getString(R.string.gun_car), getString(R.string.gun_per),
        getString(R.string.gun_cum), getString(R.string.gun_cmt),
        getString(R.string.gun_paz)
    )

    private fun gunlerMetni(): String {
        val secili = Koc.gunler(this)
        if (secili.size == 7) return getString(R.string.koc_her_gun)
        if (secili.isEmpty()) return getString(R.string.koc_gun_yok)
        val adlar = gunAdlari()
        return gunSirasi.toList().mapIndexedNotNull { i, g ->
            if (secili.contains(g)) adlar[i] else null
        }.joinToString(", ")
    }

    private fun gunlerSec() {
        val secili = Koc.gunler(this).toMutableSet()
        val isaretli = gunSirasi.map { secili.contains(it) }.toBooleanArray()

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.koc_gunler)
            .setMultiChoiceItems(gunAdlari(), isaretli) { _, hangi, secildi ->
                if (secildi) secili.add(gunSirasi[hangi]) else secili.remove(gunSirasi[hangi])
            }
            .setPositiveButton(R.string.ok) { _, _ ->
                Koc.setGunler(this, secili)
                ciz()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ═══════════════════════════════════════════════════════════════
    // ARAYÜZ YARDIMCILARI
    // ═══════════════════════════════════════════════════════════════

    private fun baslik(m: String) {
        kap.addView(TextView(this).apply {
            text = m
            textSize = 20f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, (10 * d).toInt())
        })
    }

    private fun baslikKucuk(m: String) {
        kap.addView(TextView(this).apply {
            text = m
            textSize = 13f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            alpha = 0.7f
            setPadding(0, (4 * d).toInt(), 0, (8 * d).toInt())
        })
    }

    private fun kartSar(ic: View): View = MaterialCardView(this).apply {
        radius = 14 * d
        cardElevation = 0f
        strokeWidth = (1 * d).toInt()
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = (8 * d).toInt() }
        addView(ic)
    }

    private fun anahtarKart(
        ad: String,
        alt: String,
        acik: Boolean,
        degisti: (Boolean) -> Unit
    ): View {
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((14 * d).toInt(), (12 * d).toInt(), (14 * d).toInt(), (12 * d).toInt())
        }
        ic.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
            addView(TextView(this@KocActivity).apply {
                text = ad
                textSize = 15f
            })
            if (alt.isNotBlank()) {
                addView(TextView(this@KocActivity).apply {
                    text = alt
                    textSize = 12f
                    alpha = 0.7f
                    setPadding(0, (2 * d).toInt(), 0, 0)
                })
            }
        })
        ic.addView(MaterialSwitch(this).apply {
            isChecked = acik
            setOnCheckedChangeListener { _, v -> degisti(v) }
        })
        return kartSar(ic)
    }

    private fun satir(ad: String, deger: String, tikla: () -> Unit): View {
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((14 * d).toInt(), (13 * d).toInt(), (14 * d).toInt(), (13 * d).toInt())
        }
        ic.addView(TextView(this).apply {
            text = ad
            textSize = 15f
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        })
        ic.addView(TextView(this).apply {
            text = deger
            textSize = 13f
            alpha = 0.8f
        })
        return kartSar(ic).apply {
            isClickable = true
            setOnClickListener { tikla() }
        }
    }

    private fun bilgi(m: String) = TextView(this).apply {
        text = m
        textSize = 12.5f
        alpha = 0.75f
        setLineSpacing(0f, 1.2f)
        setPadding(0, (8 * d).toInt(), 0, (8 * d).toInt())
    }

    private fun ayirici() {
        kap.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (1 * d).toInt()
            ).apply {
                topMargin = (14 * d).toInt()
                bottomMargin = (6 * d).toInt()
            }
            setBackgroundColor(
                (MaterialColors.getColor(
                    this@KocActivity,
                    com.google.android.material.R.attr.colorOnSurface, 0
                ) and 0x00FFFFFF) or 0x22000000
            )
        })
    }

    private fun dugme(m: String, vurgulu: Boolean = false, tikla: () -> Unit) =
        TextView(this).apply {
            text = m
            textSize = 14f
            gravity = Gravity.CENTER
            setTypeface(typeface, if (vurgulu) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            setTextColor(
                MaterialColors.getColor(
                    this@KocActivity,
                    com.google.android.material.R.attr.colorPrimary, 0
                )
            )
            setPadding(0, (13 * d).toInt(), 0, (13 * d).toInt())
            background = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x22888888), null, null
            )
            isClickable = true
            setOnClickListener { tikla() }
        }
}
