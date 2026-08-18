package com.gunlukasistan.app

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * v10.9 — Gün çerçevesi ayarları ("her şeyini değiştirebileyim").
 *
 * ── Tasarım ──
 * [SayacAyarActivity] ile birebir aynı görsel dil: grup kartları,
 * küçük soluk başlıklar, ince ayırıcılar. Değişen her ayar
 * [UykuZamanla.kur] üzerinden alarmları anında yeniden kurar —
 * "kaydet" düğmesi yok, dokununca uygulanır.
 *
 * ── Bölümler ──
 * DURUM (ana anahtar + sıradaki alarmlar) · SABAH (saat, sessizlik,
 * onay, tekrar, son çare) · AKŞAM (saat, tekrar) · ÖZET İÇERİĞİ
 * (4 satır ayrı kapatılabilir) · UYKU KAYDI (ortalamalar + 7 gün)
 * · DENE VE SİSTEM (test bildirimleri + Android kanal ayarları).
 */
class UykuAyarActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            context.startActivity(Intent(context, UykuAyarActivity::class.java))
        }
    }

    private val d get() = resources.displayMetrics.density
    private lateinit var kap: LinearLayout

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(GorunumAyar.yaziOlcegiUygula(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.styleFor(this))
        ThemeManager.applyAccent(this)
        ThemeManager.dinamikRengiUygula(this)
        super.onCreate(savedInstanceState)

        // Kanallar burada da kurulur — "şimdi dene" ve sistem ayarı
        // satırları kanal varlığına güvenir.
        UykuZamanla.kanallariKur(this)

        kap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((16 * d).toInt(), (16 * d).toInt(), (16 * d).toInt(), (32 * d).toInt())
        }
        setContentView(
            ScrollView(this).apply {
                setBackgroundColor(
                    MaterialColors.getColor(
                        this@UykuAyarActivity,
                        com.google.android.material.R.attr.colorSurface, 0
                    )
                )
                addView(kap)
            }
        )
        ciz()
    }

    // ═══════════════════════════════════════════════════════════════
    // ÇİZİM
    // ═══════════════════════════════════════════════════════════════

    private fun ciz() {
        kap.removeAllViews()

        val acik = UykuCerceve.acik(this)
        val notifAcik = Store.getNotifEnabled(this)
        val cerceveCalisir = acik && notifAcik
        val simdi = System.currentTimeMillis()

        kap.addView(TextView(this).apply {
            text = getString(R.string.uy_baslik)
            textSize = 21f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, (12 * d).toInt())
        })

        // Genel bildirim anahtarı kapalıysa çerçeve teknik olarak
        // çalışamaz — ayar ekranı bunu gizlemek yerine bağlantı verir.
        if (!notifAcik) {
            kap.addView(
                grup(
                    tiklanabilirSatir(getString(R.string.uy_notif_kapali_uyari), "") {
                        BildirimAyarActivity.ac(this)
                    }
                )
            )
        }

        // ── DURUM ──
        grupBaslik(getString(R.string.uy_grup_durum))
        val durumDetay = if (cerceveCalisir) {
            sonrakiSatir(simdi, UykuCerceve.sabahDk(this), R.string.uy_sonraki_sabah) + "\n" +
                sonrakiSatir(simdi, UykuCerceve.aksamDk(this), R.string.uy_sonraki_aksam)
        } else {
            ""
        }
        kap.addView(
            grup(
                anahtarSatiri(
                    getString(R.string.uy_anahtar),
                    getString(R.string.uy_anahtar_alt),
                    acik,
                    aktif = notifAcik
                ) { v ->
                    UykuCerceve.setAcik(this, v)
                    UykuZamanla.kur(this)
                    ciz()
                },
                ayirici(),
                bilgiSatiri(
                    getString(
                        if (cerceveCalisir) R.string.uy_durum_acik else R.string.uy_durum_kapali
                    ),
                    durumDetay
                )
            )
        )

        // ── SABAH ──
        val onay = UykuCerceve.onaySart(this)
        grupBaslik(getString(R.string.uy_grup_sabah))
        kap.addView(
            grup(
                tiklanabilirSatir(
                    getString(R.string.uy_sabah_saat),
                    UykuCerceve.saatMetni(UykuCerceve.sabahDk(this)),
                    aktif = cerceveCalisir
                ) { saatSec(sabahMi = true) },
                ayirici(),
                bilgiSatiri("", getString(R.string.uy_sabah_saat_alt)),
                ayirici(),
                anahtarSatiri(
                    getString(R.string.uy_sessiz),
                    getString(R.string.uy_sessiz_alt),
                    UykuCerceve.sabahSessiz(this),
                    aktif = cerceveCalisir
                ) { v ->
                    UykuCerceve.setSabahSessiz(this, v)
                    ciz()
                },
                ayirici(),
                anahtarSatiri(
                    getString(R.string.uy_onay),
                    getString(R.string.uy_onay_alt),
                    onay,
                    aktif = cerceveCalisir
                ) { v ->
                    UykuCerceve.setOnaySart(this, v)
                    UykuZamanla.kur(this)
                    ciz()
                },
                ayirici(),
                tiklanabilirSatir(
                    getString(R.string.uy_tekrar),
                    getString(R.string.uy_dk_birim, UykuCerceve.tekrarDkSabah(this)),
                    aktif = cerceveCalisir && onay
                ) { tekrarSec(sabahMi = true) },
                ayirici(),
                tiklanabilirSatir(
                    getString(R.string.uy_maks),
                    maksMetni(UykuCerceve.maksTekrarSabah(this)),
                    aktif = cerceveCalisir && onay
                ) { maksSec(sabahMi = true) },
                ayirici(),
                anahtarSatiri(
                    getString(R.string.uy_soncare),
                    getString(R.string.uy_soncare_alt),
                    UykuCerceve.sonCare(this),
                    aktif = cerceveCalisir && onay
                ) { v ->
                    UykuCerceve.setSonCare(this, v)
                    ciz()
                }
            )
        )

        // ── AKŞAM ──
        grupBaslik(getString(R.string.uy_grup_aksam))
        kap.addView(
            grup(
                tiklanabilirSatir(
                    getString(R.string.uy_aksam_saat),
                    UykuCerceve.saatMetni(UykuCerceve.aksamDk(this)),
                    aktif = cerceveCalisir
                ) { saatSec(sabahMi = false) },
                ayirici(),
                bilgiSatiri("", getString(R.string.uy_aksam_saat_alt)),
                ayirici(),
                tiklanabilirSatir(
                    getString(R.string.uy_tekrar),
                    getString(R.string.uy_dk_birim, UykuCerceve.tekrarDkAksam(this)),
                    aktif = cerceveCalisir
                ) { tekrarSec(sabahMi = false) },
                ayirici(),
                tiklanabilirSatir(
                    getString(R.string.uy_maks),
                    maksMetni(UykuCerceve.maksTekrarAksam(this)),
                    aktif = cerceveCalisir
                ) { maksSec(sabahMi = false) }
            )
        )

        // ── ÖZET İÇERİĞİ ──
        grupBaslik(getString(R.string.uy_grup_ozet))
        kap.addView(
            grup(
                anahtarSatiri(
                    getString(R.string.uy_ozet_odak), "",
                    UykuCerceve.ozetOdak(this), aktif = cerceveCalisir
                ) { v -> UykuCerceve.setOzetOdak(this, v) },
                ayirici(),
                anahtarSatiri(
                    getString(R.string.uy_ozet_gorev), "",
                    UykuCerceve.ozetGorev(this), aktif = cerceveCalisir
                ) { v -> UykuCerceve.setOzetGorev(this, v) },
                ayirici(),
                anahtarSatiri(
                    getString(R.string.uy_ozet_zincir), "",
                    UykuCerceve.ozetZincir(this), aktif = cerceveCalisir
                ) { v -> UykuCerceve.setOzetZincir(this, v) },
                ayirici(),
                anahtarSatiri(
                    getString(R.string.uy_ozet_seri), "",
                    UykuCerceve.ozetSeri(this), aktif = cerceveCalisir
                ) { v -> UykuCerceve.setOzetSeri(this, v) }
            )
        )

        // ── UYKU KAYDI ──
        grupBaslik(getString(R.string.uy_grup_defter))
        kap.addView(
            grup(
                tiklanabilirSatir(
                    getString(R.string.mk_btn_uyku_ayar),
                    "Bugün veya geçmiş günlerin uyku ve uyanma saatlerini elle değiştir"
                ) { ManuelKontrolActivity.ac(this) }
            )
        )
        cizDefter()

        // ── DENE VE SİSTEM ──
        grupBaslik(getString(R.string.uy_grup_dene))
        kap.addView(
            grup(
                tiklanabilirSatir(
                    getString(R.string.uy_dene_sabah),
                    getString(R.string.uy_dene_sabah_alt)
                ) { dene(UykuAksiyonReceiver.ACTION_TEST_SABAH) },
                ayirici(),
                tiklanabilirSatir(
                    getString(R.string.uy_dene_aksam),
                    getString(R.string.uy_dene_aksam_alt)
                ) { dene(UykuAksiyonReceiver.ACTION_TEST_AKSAM) },
                ayirici(),
                tiklanabilirSatir(
                    getString(R.string.uy_sistem),
                    getString(R.string.uy_sistem_alt)
                ) {
                    runCatching {
                        startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                        )
                    }
                }
            )
        )
    }

    /** Uyku kaydı kartı: ortalamalar + son 7 gün + temizleme. */
    private fun cizDefter() {
        val defter = UykuCerceve.defter(this)
        val satirlar = mutableListOf<View>()
        if (defter.isEmpty()) {
            satirlar.add(bilgiSatiri(getString(R.string.uy_defter_bos), ""))
        } else {
            val uyanmalar = defter.filter { it.uyandiMs > 0 }
                .map { UykuCerceve.dakikaOfMs(it.uyandiMs) }
            val uyumalar = defter.filter { it.uyuduMs > 0 }
                .map { UykuCerceve.dakikaOfMs(it.uyuduMs) }
            satirlar.add(
                bilgiSatiri(
                    getString(R.string.uy_ort_uyanma),
                    UykuCerceve.ortalamaUyanmaDk(uyanmalar)
                        ?.let { UykuCerceve.saatMetni(it) } ?: "—"
                )
            )
            satirlar.add(
                bilgiSatiri(
                    getString(R.string.uy_ort_uyuma),
                    UykuCerceve.ortalamaUyumaDk(uyumalar)
                        ?.let { UykuCerceve.saatMetni(it) } ?: "—"
                )
            )
            satirlar.add(
                bilgiSatiri(
                    getString(R.string.uy_ort_sure),
                    UykuCerceve.ortalamaUykuMs(defter)
                        ?.let { UykuCerceve.sureKisa(it) } ?: "—"
                )
            )
            satirlar.add(ayirici())
            val tarihOkuyucu = SimpleDateFormat("yyyyMMdd", Locale.US)
            val tarihYazici = SimpleDateFormat("d MMM EEE", Locale("tr", "TR"))
            defter.takeLast(7).asReversed().forEach { gun ->
                val tarih = runCatching {
                    tarihOkuyucu.parse(gun.gunKey)?.let { tarihYazici.format(it) }
                }.getOrNull() ?: gun.gunKey
                val metin = when {
                    gun.uykuMs > 0 && gun.uyuduMs > 0 && gun.uyandiMs > 0 -> getString(
                        R.string.uy_defter_satir, tarih,
                        UykuCerceve.saatMetni(UykuCerceve.dakikaOfMs(gun.uyuduMs)),
                        UykuCerceve.saatMetni(UykuCerceve.dakikaOfMs(gun.uyandiMs)),
                        UykuCerceve.sureKisa(gun.uykuMs)
                    )
                    gun.uyuduMs > 0 && gun.uyandiMs <= 0 -> getString(
                        R.string.uy_defter_bekliyor, tarih,
                        UykuCerceve.saatMetni(UykuCerceve.dakikaOfMs(gun.uyuduMs))
                    )
                    else -> getString(R.string.uy_defter_eksik, tarih)
                }
                satirlar.add(bilgiSatiri(metin, ""))
            }
            satirlar.add(ayirici())
            satirlar.add(
                tiklanabilirSatir(getString(R.string.uy_temizle), "") { defterTemizle() }
            )
        }
        kap.addView(grup(*satirlar.toTypedArray()))
    }

    // ═══════════════════════════════════════════════════════════════
    // SEÇİCİLER
    // ═══════════════════════════════════════════════════════════════

    /** "yarın 07:00 (8 sa 5 dk sonra)" biçiminde sıradaki alarm satırı. */
    private fun sonrakiSatir(simdi: Long, hedefDk: Int, kalipRes: Int): String {
        val hedef = UykuCerceve.sonrakiAlarm(simdi, hedefDk)
        val gunAd = if (UykuCerceve.gunKey(hedef) == UykuCerceve.gunKey(simdi)) {
            getString(R.string.uy_bugun)
        } else {
            getString(R.string.uy_yarin)
        }
        val kalan = getString(R.string.uy_kalan_sonra, UykuCerceve.sureKisa(hedef - simdi))
        return getString(kalipRes, "$gunAd ${UykuCerceve.saatMetni(hedefDk)}", kalan)
    }

    private fun maksMetni(maks: Int): String =
        if (maks == 0) getString(R.string.uy_maks_sifir)
        else getString(R.string.uy_maks_deger, maks)

    private fun saatSec(sabahMi: Boolean) {
        val mevcut = if (sabahMi) UykuCerceve.sabahDk(this) else UykuCerceve.aksamDk(this)
        TimePickerDialog(this, { _, h, m ->
            if (sabahMi) UykuCerceve.setSabahDk(this, h * 60 + m)
            else UykuCerceve.setAksamDk(this, h * 60 + m)
            UykuZamanla.kur(this)
            ciz()
        }, mevcut / 60, mevcut % 60, true).show()
    }

    private fun tekrarSec(sabahMi: Boolean) {
        val degerler = if (sabahMi) {
            intArrayOf(5, 10, 15, 20, 30, 45, 60)
        } else {
            intArrayOf(5, 10, 15, 30, 45, 60, 90)
        }
        val etiketler = degerler.map { getString(R.string.uy_dk_birim, it) }.toTypedArray()
        val mevcut = if (sabahMi) UykuCerceve.tekrarDkSabah(this) else UykuCerceve.tekrarDkAksam(this)
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.uy_tekrar))
            .setSingleChoiceItems(etiketler, degerler.indexOf(mevcut)) { dialog, hangi ->
                if (sabahMi) UykuCerceve.setTekrarDkSabah(this, degerler[hangi])
                else UykuCerceve.setTekrarDkAksam(this, degerler[hangi])
                dialog.dismiss()
                ciz()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun maksSec(sabahMi: Boolean) {
        val degerler = intArrayOf(0, 1, 2, 3, 4, 5, 6)
        val etiketler = degerler.map { maksMetni(it) }.toTypedArray()
        val mevcut = if (sabahMi) UykuCerceve.maksTekrarSabah(this) else UykuCerceve.maksTekrarAksam(this)
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.uy_maks))
            .setSingleChoiceItems(etiketler, degerler.indexOf(mevcut)) { dialog, hangi ->
                if (sabahMi) UykuCerceve.setMaksTekrarSabah(this, degerler[hangi])
                else UykuCerceve.setMaksTekrarAksam(this, degerler[hangi])
                dialog.dismiss()
                ciz()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /** "Şimdi dene" — test eylemleri sayaçları ve gün işaretlerini TÜKETMEZ. */
    private fun dene(eylem: String) {
        runCatching {
            sendBroadcast(
                Intent(this, UykuAksiyonReceiver::class.java).setAction(eylem)
            )
            Toast.makeText(this, R.string.uy_dene_gonderildi, Toast.LENGTH_SHORT).show()
        }
    }

    private fun defterTemizle() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.uy_temizle))
            .setMessage(getString(R.string.uy_temizle_onay))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                UykuCerceve.defteriTemizle(this)
                Toast.makeText(this, R.string.uy_temizlendi, Toast.LENGTH_SHORT).show()
                ciz()
            }
            .show()
    }

    // ═══════════════════════════════════════════════════════════════
    // GÖRÜNÜM YARDIMCILARI (SayacAyarActivity ile aynı dil)
    // ═══════════════════════════════════════════════════════════════

    private fun grupBaslik(m: String) {
        kap.addView(TextView(this).apply {
            text = m
            textSize = 12.5f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            alpha = 0.65f
            setPadding((6 * d).toInt(), (18 * d).toInt(), 0, (7 * d).toInt())
        })
    }

    private fun grup(vararg satirlar: View): View {
        val ic = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        satirlar.forEach { ic.addView(it) }
        return MaterialCardView(this).apply {
            radius = 18 * d
            cardElevation = 0f
            strokeWidth = 0
            setCardBackgroundColor(
                MaterialColors.getColor(
                    this@UykuAyarActivity,
                    com.google.android.material.R.attr.colorSurfaceVariant, 0
                )
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            addView(ic)
        }
    }

    private fun ayirici(): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, (1 * d).toInt()
        ).apply {
            leftMargin = (16 * d).toInt()
            rightMargin = (16 * d).toInt()
        }
        setBackgroundColor(
            (MaterialColors.getColor(
                this@UykuAyarActivity,
                com.google.android.material.R.attr.colorOnSurface, 0
            ) and 0x00FFFFFF) or 0x1A000000
        )
    }

    /** Tıklanmayan bilgi satırı — ikinci metin vurgu renginde. */
    private fun bilgiSatiri(ad: String, deger: String): View {
        val satir = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((16 * d).toInt(), (13 * d).toInt(), (16 * d).toInt(), (13 * d).toInt())
        }
        if (ad.isNotBlank()) {
            satir.addView(TextView(this).apply {
                text = ad
                textSize = 14.5f
                setLineSpacing(0f, 1.25f)
            })
        }
        if (deger.isNotBlank()) {
            satir.addView(TextView(this).apply {
                text = deger
                textSize = 12.5f
                setLineSpacing(0f, 1.25f)
                setPadding(0, (2 * d).toInt(), 0, 0)
                setTextColor(
                    MaterialColors.getColor(
                        this@UykuAyarActivity,
                        com.google.android.material.R.attr.colorPrimary, 0
                    )
                )
            })
        }
        return satir
    }

    private fun anahtarSatiri(
        ad: String,
        alt: String,
        acik: Boolean,
        aktif: Boolean = true,
        degisti: (Boolean) -> Unit
    ): View {
        val satir = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((16 * d).toInt(), (15 * d).toInt(), (16 * d).toInt(), (15 * d).toInt())
            alpha = if (aktif) 1f else 0.45f
        }
        satir.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
            addView(TextView(this@UykuAyarActivity).apply {
                text = ad
                textSize = 15.5f
            })
            if (alt.isNotBlank()) {
                addView(TextView(this@UykuAyarActivity).apply {
                    text = alt
                    textSize = 12.5f
                    alpha = 0.7f
                    setLineSpacing(0f, 1.2f)
                    setPadding(0, (3 * d).toInt(), (10 * d).toInt(), 0)
                })
            }
        })
        satir.addView(MaterialSwitch(this).apply {
            isChecked = acik
            isEnabled = aktif
            setOnCheckedChangeListener { _, v -> degisti(v) }
        })
        return satir
    }

    private fun tiklanabilirSatir(
        ad: String,
        deger: String,
        aktif: Boolean = true,
        tikla: () -> Unit
    ): View {
        val satir = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((16 * d).toInt(), (15 * d).toInt(), (16 * d).toInt(), (15 * d).toInt())
            alpha = if (aktif) 1f else 0.45f
            background = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x22888888), null, null
            )
            isClickable = aktif
            if (aktif) setOnClickListener { tikla() }
        }
        satir.addView(TextView(this).apply {
            text = ad
            textSize = 15.5f
        })
        if (deger.isNotBlank()) {
            satir.addView(TextView(this).apply {
                text = deger
                textSize = 12.5f
                setLineSpacing(0f, 1.25f)
                setPadding(0, (3 * d).toInt(), 0, 0)
                setTextColor(
                    MaterialColors.getColor(
                        this@UykuAyarActivity,
                        com.google.android.material.R.attr.colorPrimary, 0
                    )
                )
            })
        }
        return satir
    }
}
