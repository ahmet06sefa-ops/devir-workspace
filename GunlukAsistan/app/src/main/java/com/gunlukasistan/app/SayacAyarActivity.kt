package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch

/**
 * v7.86 — Zamanlayıcı ayarları ekranı.
 *
 * ── Tasarım ──
 * Ekran görüntüsündeki Google Saat düzeni örnek alındı: gruplanmış
 * yuvarlak kartlar, grup başlıkları küçük ve soluk, satırlar arasında
 * ince ayırıcı. Tek tek kart yerine **grup kartı** kullanıldı; 15 ayrı
 * kart görsel gürültü yaratırdı.
 *
 * ── Neden ayrı ekran ──
 * Ayarlar bugüne kadar üç yere dağılmıştı (TimerEngine ses indeksi,
 * Store titreşim, ZorunluUyari ısrarlı uyarı). Kullanıcı "zamanlayıcının
 * her şeyini ayarlayabileyim" dedi — hepsi burada toplandı.
 */
class SayacAyarActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            context.startActivity(Intent(context, SayacAyarActivity::class.java))
        }
    }

    private val d get() = resources.displayMetrics.density
    private lateinit var kap: LinearLayout

    /** Sistem zil sesi seçici. */
    /** v10.43 · Madde #3: odak sesi için dosya seçici. */
    private val odakZilSecici = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { sonuc ->
        if (sonuc.resultCode != RESULT_OK) return@registerForActivityResult
        val uri = sonuc.data?.getParcelableExtra<android.net.Uri>(
            RingtoneManager.EXTRA_RINGTONE_PICKED_URI
        )
        if (uri == null) {
            SesManzarasi.setOzel(this, "", "")
        } else {
            val ad = runCatching {
                RingtoneManager.getRingtone(this, uri)?.getTitle(this)
            }.getOrNull().orEmpty()
            SesManzarasi.setOzel(this, uri.toString(), ad)
        }
        ciz()
    }

    private val zilSecici = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { sonuc ->
        if (sonuc.resultCode != RESULT_OK) return@registerForActivityResult
        val uri = sonuc.data?.getParcelableExtra<android.net.Uri>(
            RingtoneManager.EXTRA_RINGTONE_PICKED_URI
        )
        if (uri == null) {
            // "Yok" seçildi — sessiz bitiş
            SayacAyar.setSesSecimi(this, "", getString(R.string.sa_ses_yok))
        } else {
            val ad = runCatching {
                RingtoneManager.getRingtone(this, uri)?.getTitle(this)
            }.getOrNull().orEmpty()
            SayacAyar.setSesSecimi(this, uri.toString(), ad)
        }
        ciz()
    }

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
            setPadding((16 * d).toInt(), (16 * d).toInt(), (16 * d).toInt(), (32 * d).toInt())
        }
        setContentView(
            ScrollView(this).apply {
                setBackgroundColor(
                    MaterialColors.getColor(
                        this@SayacAyarActivity,
                        com.google.android.material.R.attr.colorSurface, 0
                    )
                )
                addView(kap)
            }
        )
        ciz()
    }

    /** v10.12 · Grup D: kullanım erişimi / kaçamak sayacı dönüşte tazelensin. */
    override fun onResume() {
        super.onResume()
        if (::kap.isInitialized) ciz()
    }

    // ═══════════════════════════════════════════════════════════════
    // v10.12 · GRUP D — diyaloglar ve etiket yardımcıları
    // ═══════════════════════════════════════════════════════════════

    private fun hayaletModAdi(): String = getString(
        when (Hayalet.mod(this)) {
            Hayalet.MOD_DUN -> R.string.fo_hayalet_dun
            Hayalet.MOD_HAFTA -> R.string.fo_hayalet_hafta
            else -> R.string.fo_hayalet_kapali
        }
    )

    private fun hayaletDiyalog() {
        val adlar = arrayOf(
            getString(R.string.fo_hayalet_kapali),
            getString(R.string.fo_hayalet_dun),
            getString(R.string.fo_hayalet_hafta)
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.fo_hayalet_secim)
            .setSingleChoiceItems(adlar, Hayalet.mod(this)) { diyalog, hangi ->
                Hayalet.setMod(this, hangi)
                diyalog.dismiss()
                ciz()
            }
            .show()
    }

    private fun ritimAdi(): String {
        val h = OdakRitim.hedef(this)
        return if (h <= 0) {
            getString(R.string.fo_ritim_kapali)
        } else {
            getString(R.string.fo_ritim_adet, h)
        }
    }

    /** v10.41: kadran yazı boyutu seçimi (×0,80 … ×1,15). */
    private fun kadranYaziSec() {
        val adlar = (0..3).map { SayacAyar.kadranOlcekAdi(this, it) }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.w41_sa_kadran_yazi)
            .setSingleChoiceItems(adlar, SayacAyar.kadranYaziKademe(this)) { diyalog, hangi ->
                SayacAyar.setKadranYaziKademe(this, hangi)
                diyalog.dismiss()
                ciz()
            }
            .show()
    }

    private fun ritimDiyalog() {
        val secici = NumberPicker(this).apply {
            minValue = 0
            maxValue = OdakRitim.MAKS_HEDEF
            value = OdakRitim.hedef(this@SayacAyarActivity)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.fo_ritim_hedef)
            .setMessage(R.string.fo_ritim_alt)
            .setView(secici)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                OdakRitim.setHedef(this, secici.value)
                ciz()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun manzaraSecimDiyalog() {
        // v10.43: son satır kullanıcının kendi dosyasıdır (kod 900)
        val ozelEtiket = if (SesManzarasi.ozelVar(this)) {
            getString(R.string.w43_manzara_ozel, SesManzarasi.ozelAd(this))
        } else getString(R.string.w43_manzara_ozel_sec)
        val adlar = arrayOf(getString(R.string.fo_manzara_yok)) +
            SesManzarasi.SESLER.map { "${it.emoji} ${getString(it.adRes)}" } + ozelEtiket
        val isaret = if (SesManzarasi.secim(this) == SesManzarasi.OZEL_KOD) {
            adlar.lastIndex
        } else SesManzarasi.secim(this) + 1
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.fo_manzara_sec)
            .setSingleChoiceItems(adlar, isaret) { diyalog, hangi ->
                if (hangi == adlar.lastIndex) {
                    diyalog.dismiss()
                    odakZilSecici.launch(
                        Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.w43_manzara_ozel_sec))
                        }
                    )
                } else {
                    SesManzarasi.setSecim(this, hangi - 1)
                    diyalog.dismiss()
                    ciz()
                }
            }
            .show()
    }

    /** Kalkan açılırken izin ve kısıtlı liste eksikse adım adım yönlendirir. */
    private fun kalkanAnahtar(ac: Boolean) {
        OdakKalkani.setAcik(this, ac)
        if (ac) {
            if (!OdakKalkani.izinVarMi(this)) {
                Toast.makeText(this, R.string.fo_kalkan_izin_yok, Toast.LENGTH_LONG).show()
                kalkanIzinAc()
            }
            if (OdakKalkani.paketler(this).isEmpty()) {
                kalkanUygulamaDiyalog()
            }
        }
        ciz()
    }

    private fun kalkanIzinAc() {
        runCatching {
            startActivity(Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }

    /** Başlatıcıda görünen uygulamalardan kısıtlı listesi seçtirir. */
    private fun kalkanUygulamaDiyalog() {
        val yonetici = packageManager
        val niyet = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val uygulamalar = yonetici.queryIntentActivities(niyet, 0)
            .map { it.activityInfo.packageName to it.loadLabel(yonetici).toString() }
            .distinctBy { it.first }
            .filter { it.first != packageName }
            .sortedBy { it.second.lowercase() }
        if (uygulamalar.isEmpty()) return
        val secili = OdakKalkani.paketler(this).toMutableSet()
        val etiketler = uygulamalar.map { it.second }.toTypedArray()
        val isaretler = uygulamalar.map { secili.contains(it.first) }.toBooleanArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.fo_kalkan_uyg)
            .setMultiChoiceItems(etiketler, isaretler) { _, hangi, isaret ->
                val paket = uygulamalar[hangi].first
                if (isaret) secili.add(paket) else secili.remove(paket)
            }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                OdakKalkani.setPaketler(this, secili)
                ciz()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // ═══════════════════════════════════════════════════════════════
    // ÇİZİM
    // ═══════════════════════════════════════════════════════════════

    private fun presetlerDiyalogunuGoster() {
        val mevcut = SayacPreset.getPresetler(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24 * d).toInt(), (16 * d).toInt(), (24 * d).toInt(), (8 * d).toInt())
        }

        fun inputAlani(baslik: String, varDk: Int): android.widget.EditText {
            val satir = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, (8 * d).toInt(), 0, (8 * d).toInt())
            }
            val label = TextView(this).apply {
                text = baslik
                textSize = 15f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val edit = android.widget.EditText(this).apply {
                setText(varDk.toString())
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                textSize = 16f
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams((80 * d).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            satir.addView(label)
            satir.addView(edit)
            layout.addView(satir)
            return edit
        }

        val e1 = inputAlani("1. Hazır Buton (Kısa Mola vb.):", mevcut.getOrElse(0) { 5 })
        val e2 = inputAlani("2. Hazır Buton (Orta Mola vb.):", mevcut.getOrElse(1) { 10 })
        val e3 = inputAlani("3. Hazır Buton (Odak Seansı):", mevcut.getOrElse(2) { 25 })

        MaterialAlertDialogBuilder(this)
            .setTitle("⏱ Hazır Sayaç Sürelerini Belirle")
            .setView(layout)
            .setPositiveButton("Kaydet") { _, _ ->
                val p1 = e1.text.toString().toIntOrNull()?.coerceIn(1, 300) ?: 5
                val p2 = e2.text.toString().toIntOrNull()?.coerceIn(1, 300) ?: 10
                val p3 = e3.text.toString().toIntOrNull()?.coerceIn(1, 300) ?: 25
                SayacAyar.presetlerKaydet(this, p1, p2, p3)
                ciz()
                Toast.makeText(
                    this,
                    "✅ Sayaç Presetleri Güncellendi: $p1 Dk · $p2 Dk · $p3 Dk",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("İptal", null)
            .setNeutralButton("Varsayılana Dön (5, 10, 25)") { _, _ ->
                SayacAyar.presetlerKaydet(this, 5, 10, 25)
                ciz()
                Toast.makeText(this, "🔄 Varsayılan Sürelere Dönüldü (5, 10, 25 Dk)", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun ciz() {
        kap.removeAllViews()

        kap.addView(TextView(this).apply {
            text = getString(R.string.sa_baslik)
            textSize = 21f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, (12 * d).toInt())
        })

        // ── v10.83: HAZIR SAYAÇ SÜRELERİ (PRESETLER) & ODAK SESLERİ/GÖRSEL TEMA ──
        grupBaslik("⏱ HAZIR SAYAÇ SÜRELERİ (PRESETLER) & GÖRSEL TEMA")
        val pr = SayacPreset.getPresetler(this)
        val (temaBaslik, temaAlt) = SayacAyar.odakSesVeTemaDurumMetni(this)
        kap.addView(
            grup(
                tiklanabilirSatir(
                    "⏱ Hazır Sayaç Sürelerini (5, 10, 25 vb.) Özelleştir",
                    "1. Buton: ${pr[0]} Dk · 2. Buton: ${pr[1]} Dk · 3. Buton: ${pr[2]} Dk (Değiştirmek için dokunun)"
                ) { presetlerDiyalogunuGoster() },
                ayirici(),
                anahtarSatiri(
                    temaBaslik,
                    temaAlt,
                    SayacAyar.odakSesVeTemaAcikMi(this)
                ) {
                    SayacAyar.setOdakSesVeTemaAcik(this, it)
                    ciz()
                    val msg = if (it) "AÇIK: Odak sesleri ve görsel saat temaları aktif" else "KAPALI: Odak sesleri ve alev vb. saat temaları gizlendi"
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                },
                ayirici(),
                anahtarSatiri(
                    "🎵 Arka Plan Müzik / Radyo Kumandası",
                    "YouTube, Spotify, Karnaval Radyo vb. için sayaç ekranında Durdur/Başlat, İleri ve Geri tuşlarını göster",
                    SayacAyar.arkaPlanMedyaKumandasiAcikMi(this)
                ) {
                    SayacAyar.setArkaPlanMedyaKumandasiAcik(this, it)
                    ciz()
                    val msg = if (it) "AÇIK: Arka plan medya kumandası sayaç ekranına eklendi" else "KAPALI: Arka plan medya kumandası gizlendi"
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                }
            )
        )

        // ── Sessiz mod davranışı (en üstte, ekran görüntüsündeki gibi) ──
        kap.addView(
            grup(
                anahtarSatiri(
                    getString(R.string.sa_sessizde_sustur),
                    getString(R.string.sa_sessizde_sustur_alt),
                    SayacAyar.sessizdeSustur(this)
                ) { SayacAyar.setSessizdeSustur(this, it); ciz() },
                ayirici(),
                anahtarSatiri(
                    SayacAyar.kapatmaTusuyleAlarmDurdurDurumMetni(this).first,
                    SayacAyar.kapatmaTusuyleAlarmDurdurDurumMetni(this).second,
                    SayacAyar.isKapatmaTusuyleAlarmDurdur(this)
                ) {
                    SayacAyar.setKapatmaTusuyleAlarmDurdur(this, it)
                    Toast.makeText(
                        this,
                        if (it) "✅ Telefon kapatma (güç) tuşuyla alarmları durdurma açıldı!"
                        else "ℹ️ Kapatma tuşuyla alarm durdurma kapandı.",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                ayirici(),
                anahtarSatiri(
                    GorunumAyar.gunSerisiOtoGizleDurumMetni(this).first,
                    GorunumAyar.gunSerisiOtoGizleDurumMetni(this).second,
                    GorunumAyar.isGunSerisiOtoGizle(this)
                ) {
                    GorunumAyar.setGunSerisiOtoGizle(this, it)
                    Toast.makeText(
                        this,
                        if (it) "🔥 Gün seriniz yazısı açılışta gösterilip 4 saniyede gizlenecek."
                        else "ℹ️ Gün seriniz yazısı altta sürekli kalacak.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        )

        // ── SES ────────────────────────────────────────────────────
        grupBaslik(getString(R.string.sa_g_ses))
        kap.addView(
            grup(
                anahtarSatiri(
                    getString(R.string.sa_ses),
                    if (SayacAyar.ses(this)) sesAdiMetni() else getString(R.string.sa_ses_kapali_alt),
                    SayacAyar.ses(this)
                ) { SayacAyar.setSes(this, it); ciz() },
                ayirici(),
                tiklanabilirSatir(
                    getString(R.string.sa_zil_sec),
                    sesAdiMetni(),
                    aktif = SayacAyar.ses(this)
                ) { zilSec() },
                ayirici(),
                tiklanabilirSatir(
                    getString(R.string.sa_ses_sure),
                    getString(R.string.sa_saniye, SayacAyar.sesSureSn(this)),
                    aktif = SayacAyar.ses(this)
                ) { sesSuresiSec() },
                ayirici(),
                anahtarSatiri(
                    getString(R.string.sa_kademeli),
                    getString(R.string.sa_kademeli_alt),
                    SayacAyar.kademeliSes(this),
                    aktif = SayacAyar.ses(this)
                ) { SayacAyar.setKademeliSes(this, it) }
            )
        )

        // ── TİTREŞİM ───────────────────────────────────────────────
        grupBaslik(getString(R.string.sa_g_titresim))
        kap.addView(
            grup(
                anahtarSatiri(
                    getString(R.string.sa_titresim),
                    if (Store.getVibEnabled(this)) getString(R.string.sa_titresim_alt)
                    else getString(R.string.sa_titresim_genel_kapali),
                    SayacAyar.titresim(this)
                ) { SayacAyar.setTitresim(this, it); ciz() },
                ayirici(),
                tiklanabilirSatir(
                    getString(R.string.sa_desen),
                    SayacAyar.desenAdi(this, SayacAyar.titresimDeseni(this)),
                    aktif = SayacAyar.titresimEtkinMi(this)
                ) { desenSec() },
                ayirici(),
                tiklanabilirSatir(
                    getString(R.string.sa_test),
                    getString(R.string.sa_test_alt),
                    aktif = SayacAyar.titresimEtkinMi(this)
                ) { titresimiDene() }
            )
        )

        // ── ZAMANLAYICI ────────────────────────────────────────────
        grupBaslik(getString(R.string.sa_g_zamanlayici))
        kap.addView(
            grup(
                tiklanabilirSatir(
                    getString(R.string.sa_varsayilan),
                    getString(R.string.koc_dk, SayacAyar.varsayilanDk(this))
                ) { varsayilanSec() },
                ayirici(),
                // v10.41 · Kullanıcı maddesi #2: kadran yazı boyutu
                tiklanabilirSatir(
                    getString(R.string.w41_sa_kadran_yazi),
                    SayacAyar.kadranOlcekAdi(this, SayacAyar.kadranYaziKademe(this))
                ) { kadranYaziSec() },
                ayirici(),
                tiklanabilirSatir(
                    getString(R.string.sa_yaklasan),
                    SayacAyar.yaklasanAdi(this, SayacAyar.yaklasanSn(this))
                ) { yaklasanSec() },
                ayirici(),
                anahtarSatiri(
                    getString(R.string.sa_mini),
                    getString(R.string.sa_mini_alt),
                    SayacAyar.miniGoster(this)
                ) { SayacAyar.setMiniGoster(this, it); ciz() },
                ayirici(),
                // v7.93: Samsung/Xiaomi uyumluluk modu — varsayılan AÇIK
                anahtarSatiri(
                    getString(R.string.sa_uyumluluk),
                    getString(R.string.sa_uyumluluk_alt),
                    SayacAyar.uyumlulukModu(this),
                    aktif = SayacAyar.miniGoster(this)
                ) {
                    SayacAyar.setUyumlulukModu(this, it)
                    runCatching {
                        TimerNotifier.show(this)
                        TimerAlarm.tazelemeyiKur(this)
                    }
                    ciz()
                },
                ayirici(),
                // v7.92: bazı cihazlarda bildirimi gizliyor — varsayılan kapalı
                anahtarSatiri(
                    getString(R.string.sa_on_plan),
                    getString(R.string.sa_on_plan_alt),
                    SayacAyar.onPlanServisi(this),
                    aktif = SayacAyar.miniGoster(this) && !SayacAyar.uyumlulukModu(this)
                ) {
                    SayacAyar.setOnPlanServisi(this, it)
                    // Ayar hemen geçerli olsun — çalışan sayaç varsa tazele
                    runCatching { TimerNotifier.show(this) }
                    ciz()
                },
                ayirici(),
                // v10.2 · A1: uyanık bitiş — kilit üstünde tam ekran alarm
                anahtarSatiri(
                    getString(R.string.sa_uyanik),
                    if (tamEkranIzniYok()) {
                        getString(R.string.sa_uyanik_izin_yok)
                    } else {
                        getString(R.string.sa_uyanik_alt)
                    },
                    SayacAyar.uyanikBitis(this)
                ) {
                    SayacAyar.setUyanikBitis(this, it)
                    // İzin eksikse doğrudan sistem ayarına götür (Android 14+)
                    if (it && tamEkranIzniYok()) tamEkranIzniIste()
                    ciz()
                },
                ayirici(),
                // v10.2 · A14: bitişte flaş çakması
                anahtarSatiri(
                    getString(R.string.sa_flas),
                    getString(R.string.sa_flas_alt),
                    SayacAyar.flasBildirim(this)
                ) { SayacAyar.setFlasBildirim(this, it) },
                ayirici(),
                // v10.2 · A11: 3-2-1 başlangıç ritüeli
                anahtarSatiri(
                    getString(R.string.sa_321),
                    getString(R.string.sa_321_alt),
                    SayacAyar.baslangic321(this)
                ) { SayacAyar.setBaslangic321(this, it) },
                ayirici(),
                // v10.4 · A9: sesli geri sayım (sayfa açıkken konuşur)
                anahtarSatiri(
                    getString(R.string.sa_tts),
                    getString(R.string.sa_tts_alt),
                    SayacAyar.tts(this)
                ) { SayacAyar.setTts(this, it) },
                ayirici(),
                anahtarSatiri(
                    getString(R.string.sa_oto_tekrar),
                    getString(R.string.sa_oto_tekrar_alt),
                    SayacAyar.otomatikTekrar(this)
                ) { SayacAyar.setOtomatikTekrar(this, it) }
            )
        )

        // ── v10.50 · ODAK VE AKIŞ MİMARİSİ (#1..#10) ─────────────────
        grupBaslik(getString(R.string.om_g_baslik))
        kap.addView(
            grup(
                anahtarSatiri(
                    getString(R.string.om_kesinti_kaydi),
                    getString(R.string.om_kesinti_kaydi_alt),
                    SayacAyar.kesintiKaydiAcik(this)
                ) { SayacAyar.setKesintiKaydiAcik(this, it) },
                ayirici(),
                anahtarSatiri(
                    getString(R.string.om_tasma_acik),
                    getString(R.string.om_tasma_acik_alt),
                    SayacAyar.tasmaAcik(this)
                ) { SayacAyar.setTasmaAcik(this, it) },
                ayirici(),
                anahtarSatiri(
                    getString(R.string.om_cikti_hasadi),
                    getString(R.string.om_cikti_hasadi_alt),
                    SayacAyar.ciktiHasadiAcik(this)
                ) { SayacAyar.setCiktiHasadiAcik(this, it) },
                ayirici(),
                tiklanabilirSatir(
                    getString(R.string.om_binaural_mod),
                    when (SayacAyar.binauralMod(this)) {
                        1 -> getString(R.string.om_binaural_alfa)
                        2 -> getString(R.string.om_binaural_gama)
                        else -> getString(R.string.om_binaural_kapali)
                    }
                ) { binauralSecimDiyalogu() },
                ayirici(),
                anahtarSatiri(
                    getString(R.string.om_carpisma_bekcisi),
                    getString(R.string.om_carpisma_bekcisi_alt),
                    SayacAyar.carpismaBekcisiAcik(this)
                ) { SayacAyar.setCarpismaBekcisiAcik(this, it) },
                ayirici(),
                anahtarSatiri(
                    getString(R.string.om_masaya_donus),
                    getString(R.string.om_masaya_donus_alt),
                    SayacAyar.masayaDonusAcik(this)
                ) { SayacAyar.setMasayaDonusAcik(this, it) }
            )
        )

        // ── POMODORO (v7.94) ───────────────────────────────────────
        grupBaslik(getString(R.string.pm_g_baslik))
        kap.addView(
            grup(
                anahtarSatiri(
                    getString(R.string.pm_acik),
                    getString(R.string.pm_acik_alt),
                    Pomodoro.acikMi(this)
                ) {
                    Pomodoro.setAcik(this, it)
                    // Açılınca sayacı çalışma süresine ayarla
                    if (it && !TimerEngine.isRunning(this)) {
                        TimerEngine.setTotalMs(this, Pomodoro.calismaDk(this) * 60_000L)
                    }
                    ciz()
                },
                ayirici(),
                tiklanabilirSatir(
                    getString(R.string.pm_calisma_sure),
                    getString(R.string.koc_dk, Pomodoro.calismaDk(this)),
                    aktif = Pomodoro.acikMi(this)
                ) { pomodoroSure(0) },
                ayirici(),
                tiklanabilirSatir(
                    getString(R.string.pm_kisa_sure),
                    getString(R.string.koc_dk, Pomodoro.kisaMolaDk(this)),
                    aktif = Pomodoro.acikMi(this)
                ) { pomodoroSure(1) },
                ayirici(),
                tiklanabilirSatir(
                    getString(R.string.pm_uzun_sure),
                    getString(R.string.koc_dk, Pomodoro.uzunMolaDk(this)),
                    aktif = Pomodoro.acikMi(this)
                ) { pomodoroSure(2) },
                ayirici(),
                tiklanabilirSatir(
                    getString(R.string.pm_aralik),
                    getString(R.string.pm_aralik_deger, Pomodoro.uzunMolaAraligi(this)),
                    aktif = Pomodoro.acikMi(this)
                ) { pomodoroAralik() },
                ayirici(),
                anahtarSatiri(
                    getString(R.string.pm_mola_oto),
                    getString(R.string.pm_mola_oto_alt),
                    Pomodoro.molaOtomatik(this),
                    aktif = Pomodoro.acikMi(this)
                ) { Pomodoro.setMolaOtomatik(this, it) },
                ayirici(),
                anahtarSatiri(
                    getString(R.string.pm_calisma_oto),
                    getString(R.string.pm_calisma_oto_alt),
                    Pomodoro.molaSonrasiOtomatik(this),
                    aktif = Pomodoro.acikMi(this)
                ) { Pomodoro.setMolaSonrasiOtomatik(this, it) }
            )
        )

        // ── ODAK VE KAYIT (v7.94) ──────────────────────────────────
        grupBaslik(getString(R.string.od_g_baslik))
        kap.addView(
            grup(
                anahtarSatiri(
                    getString(R.string.od_odak),
                    if (ZorunluUyari.dndIzniVar(this)) getString(R.string.od_odak_alt)
                    else getString(R.string.od_odak_izin),
                    Pomodoro.odakModu(this)
                ) {
                    if (it && !ZorunluUyari.dndIzniVar(this)) {
                        // İzin yoksa önce onu iste
                        ZorunluUyari.dndAyarlariniAc(this)
                        Toast.makeText(this, R.string.od_izin_ver, Toast.LENGTH_LONG).show()
                        ciz()
                    } else {
                        Pomodoro.setOdakModu(this, it)
                        ciz()
                    }
                },
                ayirici(),
                tiklanabilirSatir(
                    getString(R.string.ok_baslik),
                    OdakKaydi.modAdi(this, OdakKaydi.mod(this))
                ) { odakKayitModu() },
                ayirici(),
                tiklanabilirSatir(
                    getString(R.string.ok_bugun),
                    bugunOzeti()
                ) { bugunDagilimGoster() }
            )
        )

        // ── SES MANZARASI (v10.12 · D22) ────────────────────────────
        grupBaslik(getString(R.string.fo_g_manzara))
        kap.addView(
            grup(
                anahtarSatiri(
                    getString(R.string.fo_manzara_oto),
                    getString(R.string.fo_manzara_oto_alt),
                    SesManzarasi.otomatik(this)
                ) { SesManzarasi.setOtomatik(this, it) },
                ayirici(),
                tiklanabilirSatir(
                    getString(R.string.fo_manzara_sec),
                    SesManzarasi.seciliSes(this)?.let { getString(it.adRes) }
                        ?: getString(R.string.fo_manzara_yok)
                ) { manzaraSecimDiyalog() },
                ayirici(),
                // v10.43: ezan sesi zaten seçilebiliyor — kapıyı buradan da aç
                tiklanabilirSatir(
                    getString(R.string.w43_ezan_satir),
                    getString(
                        R.string.w43_ezan_alt,
                        NamazBildirim.sesAdi(this).ifBlank { getString(R.string.sa_ses_yok) }
                    )
                ) { startActivity(Intent(this, NamazAyarActivity::class.java)) },
                ayirici(),
                anahtarSatiri(
                    getString(R.string.fo_manzara_mola),
                    getString(R.string.fo_manzara_mola_alt),
                    SesManzarasi.moladaKis(this)
                ) { SesManzarasi.setMoladaKis(this, it); SesManzarasi.esitle(this) }
            )
        )

        // ── KENDİNLE MAÇ (v10.12 · D20/D23) ────────────────────────────
        grupBaslik(getString(R.string.fo_g_mac))
        kap.addView(
            grup(
                tiklanabilirSatir(
                    getString(R.string.fo_hayalet_secim),
                    hayaletModAdi()
                ) { hayaletDiyalog() },
                ayirici(),
                tiklanabilirSatir(
                    getString(R.string.fo_ritim_hedef),
                    ritimAdi()
                ) { ritimDiyalog() }
            )
        )

        // ── ODAK KALKANI (v10.12 · D21) ────────────────────────────────
        grupBaslik(getString(R.string.fo_g_kalkan))
        kap.addView(
            grup(
                anahtarSatiri(
                    getString(R.string.fo_kalkan_ac),
                    getString(R.string.fo_kalkan_ac_alt),
                    OdakKalkani.acik(this)
                ) { kalkanAnahtar(it) },
                ayirici(),
                tiklanabilirSatir(
                    getString(R.string.fo_kalkan_izin),
                    if (OdakKalkani.izinVarMi(this)) {
                        getString(R.string.fo_kalkan_izin_var)
                    } else {
                        getString(R.string.fo_kalkan_izin_yok)
                    }
                ) { kalkanIzinAc() },
                ayirici(),
                tiklanabilirSatir(
                    getString(R.string.fo_kalkan_uyg),
                    OdakKalkani.paketler(this).size.let {
                        if (it == 0) {
                            getString(R.string.fo_kalkan_uyg_yok)
                        } else {
                            getString(R.string.fo_kalkan_uyg_adet, it)
                        }
                    }
                ) { kalkanUygulamaDiyalog() },
                ayirici(),
                tiklanabilirSatir(
                    getString(R.string.fo_kalkan_bugun, OdakKalkani.bugunkuIhlal(this)),
                    ""
                ) { }
            )
        )

        // ── EKRAN ──────────────────────────────────────────────────
        grupBaslik(getString(R.string.sa_g_ekran))
        kap.addView(
            grup(
                anahtarSatiri(
                    getString(R.string.sa_ekran_acik),
                    getString(R.string.sa_ekran_acik_alt),
                    SayacAyar.ekranAcikKalsin(this)
                ) { SayacAyar.setEkranAcikKalsin(this, it) },
                ayirici(),
                anahtarSatiri(
                    getString(R.string.sa_tam_ekran),
                    getString(R.string.sa_tam_ekran_alt),
                    SayacAyar.tamEkranUyari(this)
                ) { SayacAyar.setTamEkranUyari(this, it) }
            )
        )

        // ── BİLDİRİM ───────────────────────────────────────────────
        grupBaslik(getString(R.string.sa_g_bildirim))
        kap.addView(
            grup(
                tiklanabilirSatir(
                    getString(R.string.sa_bildirim_ayar),
                    getString(R.string.sa_bildirim_ayar_alt)
                ) { BildirimAyarActivity.ac(this) },
                ayirici(),
                tiklanabilirSatir(
                    getString(R.string.sa_zorunlu),
                    if (ZorunluUyari.acikMi(this)) getString(R.string.sa_acik)
                    else getString(R.string.sa_kapali)
                ) { ZorunluUyariActivity.ac(this) },
                ayirici(),
                tiklanabilirSatir(
                    getString(R.string.sa_sistem_ayar),
                    getString(R.string.sa_sistem_ayar_alt)
                ) { sistemBildirimAyari() },
                ayirici(),
                // v7.90: canlı bildirim neden görünmüyor — tanılama
                tiklanabilirSatir(
                    getString(R.string.sa_tani),
                    getString(R.string.sa_tani_alt)
                ) { taniGoster() }
            )
        )

        // Bilgi
        kap.addView(TextView(this).apply {
            text = getString(R.string.sa_bilgi)
            textSize = 11.5f
            alpha = 0.65f
            setLineSpacing(0f, 1.25f)
            setPadding(
                (4 * d).toInt(), (16 * d).toInt(), (4 * d).toInt(), 0
            )
        })
    }

    private fun sesAdiMetni(): String {
        val ad = SayacAyar.sesAdi(this)
        return when {
            !SayacAyar.ses(this) -> getString(R.string.sa_kapali)
            ad.isNotBlank() -> ad
            else -> getString(R.string.sa_varsayilan_alarm)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // SEÇİCİLER
    // ═══════════════════════════════════════════════════════════════

    private fun zilSec() {
        if (!SayacAyar.ses(this)) {
            Toast.makeText(this, R.string.sa_once_sesi_ac, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val mevcut = SayacAyar.sesUri(this)
            val niyet = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.sa_zil_sec))
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                if (mevcut.isNotBlank()) {
                    putExtra(
                        RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                        android.net.Uri.parse(mevcut)
                    )
                }
            }
            zilSecici.launch(niyet)
        } catch (e: Exception) {
            Toast.makeText(this, R.string.sa_zil_secilemedi, Toast.LENGTH_LONG).show()
        }
    }

    private fun sesSuresiSec() {
        if (!SayacAyar.ses(this)) {
            Toast.makeText(this, R.string.sa_once_sesi_ac, Toast.LENGTH_SHORT).show()
            return
        }
        val degerler = intArrayOf(5, 10, 15, 30, 60, 120)
        val adlar = degerler.map { getString(R.string.sa_saniye, it) }.toTypedArray()
        val simdiki = degerler.indexOf(SayacAyar.sesSureSn(this)).coerceAtLeast(0)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.sa_ses_sure)
            .setSingleChoiceItems(adlar, simdiki) { dlg, hangi ->
                SayacAyar.setSesSureSn(this, degerler[hangi])
                dlg.dismiss()
                ciz()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun desenSec() {
        if (!SayacAyar.titresimEtkinMi(this)) {
            Toast.makeText(this, R.string.sa_once_titresimi_ac, Toast.LENGTH_SHORT).show()
            return
        }
        val adlar = (0..2).map { SayacAyar.desenAdi(this, it) }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.sa_desen)
            .setSingleChoiceItems(adlar, SayacAyar.titresimDeseni(this)) { _, hangi ->
                SayacAyar.setTitresimDeseni(this, hangi)
                titresimiDene()
                ciz()
            }
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    /** Seçilen deseni hemen hissettirir — kullanıcı körlemesine seçmesin. */
    private fun titresimiDene() {
        if (!SayacAyar.titresimEtkinMi(this)) {
            Toast.makeText(this, R.string.sa_once_titresimi_ac, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val desen = SayacAyar.desenDizisi(SayacAyar.titresimDeseni(this))
            val titresici = if (android.os.Build.VERSION.SDK_INT >= 31) {
                (getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
                    as? android.os.VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            } ?: return

            if (android.os.Build.VERSION.SDK_INT >= 26) {
                titresici.vibrate(
                    android.os.VibrationEffect.createWaveform(desen, -1),
                    SayacAyar.sesNiteligi()
                )
            } else {
                @Suppress("DEPRECATION")
                titresici.vibrate(desen, -1)
            }
        } catch (e: Exception) {
            android.util.Log.w("SayacAyar", "Titreşim denenemedi", e)
        }
    }

    /** v10.50 · #6: Binaural Ritim Modu seçim diyaloğu. */
    private fun binauralSecimDiyalogu() {
        val secenekler = arrayOf(
            getString(R.string.om_binaural_kapali),
            getString(R.string.om_binaural_alfa),
            getString(R.string.om_binaural_gama)
        )
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.om_binaural_mod))
            .setSingleChoiceItems(secenekler, SayacAyar.binauralMod(this)) { d, s ->
                SayacAyar.setBinauralMod(this, s)
                d.dismiss()
                ciz()
            }
            .show()
    }

    private fun varsayilanSec() {
        val secici = NumberPicker(this).apply {
            minValue = 1
            maxValue = 180
            value = SayacAyar.varsayilanDk(this@SayacAyarActivity)
            wrapSelectorWheel = false
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.sa_varsayilan)
            .setView(LinearLayout(this).apply {
                gravity = Gravity.CENTER
                setPadding(0, (16 * d).toInt(), 0, 0)
                addView(secici)
            })
            .setPositiveButton(R.string.ok) { _, _ ->
                SayacAyar.setVarsayilanDk(this, secici.value)
                // Sayaç duruyorsa yeni varsayılanı hemen uygula
                if (!TimerEngine.isRunning(this)) {
                    TimerEngine.setTotalMs(this, secici.value * 60_000L)
                }
                ciz()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun yaklasanSec() {
        val degerler = intArrayOf(0, 10, 30, 60, 120, 300)
        val adlar = degerler.map { SayacAyar.yaklasanAdi(this, it) }.toTypedArray()
        val simdiki = degerler.indexOf(SayacAyar.yaklasanSn(this)).coerceAtLeast(0)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.sa_yaklasan)
            .setSingleChoiceItems(adlar, simdiki) { dlg, hangi ->
                SayacAyar.setYaklasanSn(this, degerler[hangi])
                dlg.dismiss()
                ciz()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * v7.90 — Canlı bildirim tanılaması.
     *
     * "Bildirim gelmiyor" sorununun beş olası sebebi var ve kullanıcı
     * hangisinin geçerli olduğunu göremiyor. Bu ekran hepsini tek tek
     * kontrol edip gösteriyor; sorunlu maddeye dokununca çözüm yerine
     * götürüyor.
     */
    private fun taniGoster() {
        val satirlar = mutableListOf<String>()

        // 1. Sistem bildirim izni
        val izin = try {
            androidx.core.app.NotificationManagerCompat.from(this).areNotificationsEnabled()
        } catch (e: Exception) {
            true
        }
        satirlar.add(isaret(izin) + " " + getString(R.string.sa_t_izin))

        // 2. Uygulama içi bildirim anahtarı
        satirlar.add(
            isaret(Store.getNotifEnabled(this)) + " " + getString(R.string.sa_t_uygulama)
        )

        // 3. Mini zamanlayıcı ayarı
        satirlar.add(
            isaret(SayacAyar.miniGoster(this)) + " " + getString(R.string.sa_t_mini)
        )

        // 4. Kanal durumu — asıl şüpheli
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            val yonetici = getSystemService(android.app.NotificationManager::class.java)
            val kanal = yonetici?.getNotificationChannel(TimerNotifier.CHANNEL_ID)
            when {
                kanal == null ->
                    satirlar.add("• " + getString(R.string.sa_t_kanal_yok))
                kanal.importance == android.app.NotificationManager.IMPORTANCE_NONE ->
                    satirlar.add("✕ " + getString(R.string.sa_t_kanal_kapali))
                kanal.importance < android.app.NotificationManager.IMPORTANCE_DEFAULT ->
                    satirlar.add("⚠ " + getString(R.string.sa_t_kanal_dusuk))
                else ->
                    satirlar.add("✓ " + getString(R.string.sa_t_kanal_ok))
            }
        }

        // 5. Sayaç durumu
        satirlar.add(
            isaret(TimerEngine.isRunning(this)) + " " + getString(R.string.sa_t_calisiyor)
        )

        // 6. Ön plan servisi
        // v7.92: servis isteğe bağlı — kapalıysa bu bir sorun değil
        if (SayacAyar.onPlanServisi(this)) {
            satirlar.add(
                isaret(SayacServisi.ayakta) + " " + getString(R.string.sa_t_servis)
            )
        } else {
            satirlar.add("• " + getString(R.string.sa_t_servis_kapali))
        }

        // 7. v7.91: bildirim ŞU AN panelde duruyor mu?
        // Diğer maddeler "olması gerekiyor" der; bu madde gerçeği söyler.
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            val panelde = try {
                getSystemService(android.app.NotificationManager::class.java)
                    ?.activeNotifications
                    ?.any { it.id == TimerNotifier.NOTIF_ID } == true
            } catch (e: Exception) {
                false
            }
            satirlar.add(isaret(panelde) + " " + getString(R.string.sa_t_panelde))
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.sa_tani)
            .setMessage(satirlar.joinToString("\n\n") + "\n\n" + getString(R.string.sa_t_ipucu))
            .setPositiveButton(R.string.sa_t_kanal_ac) { _, _ -> kanalAyariniAc() }
            .setNeutralButton(R.string.sa_t_dene) { _, _ -> bildirimiDene() }
            .setNegativeButton(R.string.ok, null)
            .show()
    }

    /**
     * v7.91 — Bildirimi elle tetikler.
     *
     * Sayaç çalışmıyorsa bile bir test bildirimi göndererek kanalın
     * gerçekten çalışıp çalışmadığını gösterir. Böylece sorunun kanalda
     * mı yoksa sayaç akışında mı olduğu ayırt edilebilir.
     */
    private fun bildirimiDene() {
        val vardi = TimerEngine.isRunning(this)
        if (!vardi) {
            Toast.makeText(this, R.string.sa_t_once_baslat, Toast.LENGTH_LONG).show()
            return
        }
        TimerNotifier.show(this)
        Toast.makeText(this, R.string.sa_t_gonderildi, Toast.LENGTH_SHORT).show()
    }

    private fun isaret(tamam: Boolean): String = if (tamam) "✓" else "✕"

    /** v10.2 · A1: tam ekran uyandırma izni eksik mi (Android 14+ onayı). */
    private fun tamEkranIzniYok(): Boolean =
        android.os.Build.VERSION.SDK_INT >= 34 && runCatching {
            getSystemService(android.app.NotificationManager::class.java)
                ?.canUseFullScreenIntent() == false
        }.getOrDefault(false)

    /** v10.2 · A1: kullanıcıyı FSI izin ekranına götürür. */
    private fun tamEkranIzniIste() {
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            runCatching {
                startActivity(
                    android.content.Intent(
                        android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT
                    ).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)
                )
            }
        }
    }

    /** Doğrudan sayaç kanalının sistem ayarını açar. */
    private fun kanalAyariniAc() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                startActivity(
                    Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                        .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)
                        .putExtra(
                            android.provider.Settings.EXTRA_CHANNEL_ID,
                            TimerNotifier.CHANNEL_ID
                        )
                )
            } else {
                sistemBildirimAyari()
            }
        } catch (e: Exception) {
            sistemBildirimAyari()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // v7.94 — POMODORO / ODAK SEÇİCİLERİ
    // ═══════════════════════════════════════════════════════════════

    /** @param hangi 0 çalışma · 1 kısa mola · 2 uzun mola */
    private fun pomodoroSure(hangi: Int) {
        if (!Pomodoro.acikMi(this)) {
            Toast.makeText(this, R.string.pm_once_ac, Toast.LENGTH_SHORT).show()
            return
        }
        val secici = NumberPicker(this).apply {
            minValue = 1
            maxValue = if (hangi == 0) 180 else 60
            value = when (hangi) {
                1 -> Pomodoro.kisaMolaDk(this@SayacAyarActivity)
                2 -> Pomodoro.uzunMolaDk(this@SayacAyarActivity)
                else -> Pomodoro.calismaDk(this@SayacAyarActivity)
            }
            wrapSelectorWheel = false
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(
                when (hangi) {
                    1 -> R.string.pm_kisa_sure
                    2 -> R.string.pm_uzun_sure
                    else -> R.string.pm_calisma_sure
                }
            )
            .setView(LinearLayout(this).apply {
                gravity = Gravity.CENTER
                setPadding(0, (16 * d).toInt(), 0, 0)
                addView(secici)
            })
            .setPositiveButton(R.string.ok) { _, _ ->
                when (hangi) {
                    1 -> Pomodoro.setKisaMolaDk(this, secici.value)
                    2 -> Pomodoro.setUzunMolaDk(this, secici.value)
                    else -> {
                        Pomodoro.setCalismaDk(this, secici.value)
                        // Sayaç duruyorsa yeni süreyi hemen uygula
                        if (!TimerEngine.isRunning(this) && !Pomodoro.molada(this)) {
                            TimerEngine.setTotalMs(this, secici.value * 60_000L)
                        }
                    }
                }
                ciz()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun pomodoroAralik() {
        if (!Pomodoro.acikMi(this)) {
            Toast.makeText(this, R.string.pm_once_ac, Toast.LENGTH_SHORT).show()
            return
        }
        val degerler = intArrayOf(2, 3, 4, 5, 6)
        val adlar = degerler.map { getString(R.string.pm_aralik_deger, it) }.toTypedArray()
        val simdiki = degerler.indexOf(Pomodoro.uzunMolaAraligi(this)).coerceAtLeast(0)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.pm_aralik)
            .setSingleChoiceItems(adlar, simdiki) { dlg, hangi ->
                Pomodoro.setUzunMolaAraligi(this, degerler[hangi])
                dlg.dismiss()
                ciz()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun odakKayitModu() {
        val adlar = arrayOf(
            getString(R.string.ok_mod_otomatik) + " — " + getString(R.string.ok_mod_otomatik_d),
            getString(R.string.ok_mod_sor) + " — " + getString(R.string.ok_mod_sor_d),
            getString(R.string.ok_mod_kapali) + " — " + getString(R.string.ok_mod_kapali_d)
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.ok_baslik)
            .setSingleChoiceItems(adlar, OdakKaydi.mod(this)) { dlg, hangi ->
                OdakKaydi.setMod(this, hangi)
                dlg.dismiss()
                ciz()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun bugunOzeti(): String {
        val dk = OdakKaydi.bugunToplamDk(this)
        val adet = OdakKaydi.bugunOturumSayisi(this)
        return if (adet == 0) getString(R.string.ok_bugun_yok)
        else getString(R.string.ok_bugun_ozet, dk, adet)
    }

    /** Bugün hangi derse ne kadar verildiği + son oturumu düzeltme. */
    private fun bugunDagilimGoster() {
        val dagilim = OdakKaydi.bugunDagilim(this)
        val son = OdakKaydi.sonOturum(this)

        val govde = buildString {
            if (dagilim.isEmpty()) {
                append(getString(R.string.ok_bugun_yok))
            } else {
                dagilim.forEach { (ad, dk) ->
                    append("• ").append(ad).append(" — ").append(dk).append(" dk\n")
                }
            }
            if (son != null) {
                append("\n")
                append(
                    getString(
                        R.string.ok_son_oturum,
                        OdakKaydi.saatMetni(son.zaman),
                        son.dakika,
                        son.baslik.ifBlank { getString(R.string.ok_yazilmadi) }
                    )
                )
            }
        }

        val yapici = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.ok_bugun)
            .setMessage(govde)
            .setPositiveButton(R.string.ok, null)

        if (son != null) {
            yapici.setNegativeButton(R.string.ok_tasi) { _, _ -> sonOturumuTasi() }
            yapici.setNeutralButton(R.string.ok_geri_al) { _, _ ->
                if (OdakKaydi.sonuGeriAl(this)) {
                    Toast.makeText(this, R.string.ok_geri_alindi, Toast.LENGTH_SHORT).show()
                    ciz()
                }
            }
        }
        yapici.show()
    }

    /** Son oturumu başka bir derse taşır — yanlış derse yazıldıysa. */
    private fun sonOturumuTasi() {
        val adimlar = Mufredat.adimlar(this)
        if (adimlar.isEmpty()) {
            Toast.makeText(this, R.string.mf_bos_program, Toast.LENGTH_SHORT).show()
            return
        }
        val adlar = adimlar.map { (if (it.bitti) "✓ " else "○ ") + it.baslik }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.ok_tasi)
            .setItems(adlar) { _, hangi ->
                if (OdakKaydi.sonuTasi(this, adimlar[hangi].id)) {
                    Toast.makeText(
                        this,
                        getString(R.string.ok_tasindi, adimlar[hangi].baslik),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                ciz()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun sistemBildirimAyari() {
        try {
            startActivity(
                Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)
            )
        } catch (e: Exception) {
            Toast.makeText(this, R.string.sa_acilamadi, Toast.LENGTH_SHORT).show()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ARAYÜZ YARDIMCILARI
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

    /** Birden çok satırı tek yuvarlak kartta toplar. */
    private fun grup(vararg satirlar: View): View {
        val ic = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        satirlar.forEach { ic.addView(it) }
        return MaterialCardView(this).apply {
            radius = 18 * d
            cardElevation = 0f
            strokeWidth = 0
            setCardBackgroundColor(
                MaterialColors.getColor(
                    this@SayacAyarActivity,
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
                this@SayacAyarActivity,
                com.google.android.material.R.attr.colorOnSurface, 0
            ) and 0x00FFFFFF) or 0x1A000000
        )
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
            addView(TextView(this@SayacAyarActivity).apply {
                text = ad
                textSize = 15.5f
            })
            if (alt.isNotBlank()) {
                addView(TextView(this@SayacAyarActivity).apply {
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
            isClickable = true
            setOnClickListener { tikla() }
        }
        satir.addView(TextView(this).apply {
            text = ad
            textSize = 15.5f
        })
        if (deger.isNotBlank()) {
            satir.addView(TextView(this).apply {
                text = deger
                textSize = 12.5f
                setPadding(0, (3 * d).toInt(), 0, 0)
                setTextColor(
                    MaterialColors.getColor(
                        this@SayacAyarActivity,
                        com.google.android.material.R.attr.colorPrimary, 0
                    )
                )
            })
        }
        return satir
    }
}
