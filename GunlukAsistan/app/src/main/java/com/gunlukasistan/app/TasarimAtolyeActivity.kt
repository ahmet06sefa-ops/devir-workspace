package com.gunlukasistan.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * v10.53 — Kullanıcı maddesi 1-30 + Bonus 31-32:
 * 32 Maddelik Tasarım ve Yerleşim Özelleştirme Atölyesi.
 *
 * Kullanıcının her bir rengi, köşe yuvarlaklığını, fontu, boşluğu ve yerleşimi
 * serbestçe yönetmesine, canlı önizleme aynası (#31) üzerinde hissetmesine
 * ve profili JSON olarak kaydetmesine (#30) veya fabrika ayarlarına sıfırlamasına (#32) olanak tanır.
 */
class TasarimAtolyeActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            context.startActivity(Intent(context, TasarimAtolyeActivity::class.java))
        }
    }

    private var aktifProfil = TasarimAtolye.AtolyeProfili()
    private lateinit var onizlemeMetin: TextView

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(GorunumAyar.yaziOlcegiUygula(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(ThemeManager.styleFor(this))
        ThemeManager.applyAccent(this)
        ThemeManager.dinamikRengiUygula(this)
        setContentView(R.layout.activity_tasarim_atolye)

        onizlemeMetin = findViewById(R.id.taOnizlemeMetin)
        findViewById<MaterialButton>(R.id.taKapat).setOnClickListener { finish() }

        // #30 & #32 Profil Kaydet ve Fabrika Sıfırla
        findViewById<MaterialButton>(R.id.taBtnProfilPaylas).setOnClickListener { profiliJsonPaylas() }
        findViewById<MaterialButton>(R.id.taBtnFabrikaSifirla).setOnClickListener { fabrikaAyarlarinaSifirla() }

        // Grup 1
        findViewById<MaterialButton>(R.id.taBtnHexPalet).setOnClickListener { hexPaletDiyalogu() }
        findViewById<MaterialButton>(R.id.taBtnKartAlfa).setOnClickListener { kartSaydamlikDiyalogu() }

        // Grup 2
        findViewById<MaterialButton>(R.id.taBtnKoseYaricap).setOnClickListener { koseYaricapDiyalogu() }
        findViewById<MaterialButton>(R.id.taBtnIlerlemeBicim).setOnClickListener { ilerlemeBicimDiyalogu() }

        // Grup 3
        findViewById<MaterialButton>(R.id.taBtnFontSec).setOnClickListener { fontSecimDiyalogu() }
        findViewById<MaterialButton>(R.id.taBtnMaxLines).setOnClickListener { maxLinesDiyalogu() }

        // Grup 4
        findViewById<MaterialButton>(R.id.taBtnBlokSira).setOnClickListener { AnaEkranDuzenActivity.ac(this) }
        findViewById<MaterialButton>(R.id.taBtnAkordiyonDurum).setOnClickListener { akordiyonDurumDiyalogu() }

        // Grup 5 & 6
        findViewById<MaterialButton>(R.id.taBtnSaniyeEfekt).setOnClickListener { saniyeEfektiDiyalogu() }
        findViewById<MaterialButton>(R.id.taBtnFabIslev).setOnClickListener { fabIslevDiyalogu() }

        canliAynayiTazele()
    }

    private fun canliAynayiTazele() {
        onizlemeMetin.text = TasarimAtolye.canliOnizlemeKartMetni(aktifProfil)
    }

    private fun profiliJsonPaylas() {
        val json = TasarimAtolye.profilJsonUret(aktifProfil).toString(2)
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText("Atölye Profili JSON", json))
        Toast.makeText(this, "💾 Profil JSON panoya kopyalandı!", Toast.LENGTH_SHORT).show()
    }

    private fun fabrikaAyarlarinaSifirla() {
        aktifProfil = TasarimAtolye.fabrikaVarsayilanProfili()
        GorunumAyar.setKartModu(this, 0)
        GorunumAyar.setOncelikVurgu(this, 1)
        GorunumAyar.setFontSablon(this, 0)
        GorunumAyar.setYaziYuzdesi(this, 100)
        GorunumAyar.setSatirNefesiDp(this, 6)
        GorunumAyar.setZenOdak(this, false)
        GorunumAyar.setAcilisEkran(this, 0)
        GorunumAyar.setHeroGizli(this, false)
        GorunumAyar.setFabIslev(this, 0)
        GorunumAyar.setYuzenSeritAcik(this, true)
        canliAynayiTazele()
        Toast.makeText(this, "🔄 Fabrika ayarlarına sıfıklandı (#32)", Toast.LENGTH_SHORT).show()
    }

    private fun hexPaletDiyalogu() {
        val secenekler = arrayOf("#4C7DFF (Varsayılan Mavi)", "#22C55E (Yeşil)", "#F59E0B (Amber)", "#EF4444 (Kırmızı)", "#C97C5D (Terracotta)", "✏️ Özel Hex Yaz (#RRGGBB)")
        val hexler = arrayOf("#4C7DFF", "#22C55E", "#F59E0B", "#EF4444", "#C97C5D")

        MaterialAlertDialogBuilder(this)
            .setTitle("#1 Serbest Hex Ana Vurgu Rengi")
            .setItems(secenekler) { _, idx ->
                if (idx < hexler.size) {
                    aktifProfil = aktifProfil.copy(ozelHexVurgu = hexler[idx])
                    canliAynayiTazele()
                    Toast.makeText(this, "Vurgu rengi: ${hexler[idx]}", Toast.LENGTH_SHORT).show()
                } else {
                    ozelHexGirisDiyalogu()
                }
            }
            .show()
    }

    private fun ozelHexGirisDiyalogu() {
        val input = EditText(this).apply {
            setText(aktifProfil.ozelHexVurgu)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Özel Hex Kodu (#RRGGBB)")
            .setView(input)
            .setPositiveButton("Uygula") { _, _ ->
                val hex = TasarimAtolye.parseHexVeyaVarsayilan(input.text.toString(), aktifProfil.ozelHexVurgu)
                aktifProfil = aktifProfil.copy(ozelHexVurgu = hex)
                canliAynayiTazele()
                Toast.makeText(this, "Hex uygulandı: $hex", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun kartSaydamlikDiyalogu() {
        val secenekler = arrayOf("%100 (Tam Opak)", "%90 (Hafif Saydam)", "%80 (Orta Saydam)", "%70 (Belirgin Saydam)", "%50 (Yarı Saydam)")
        val alfalar = intArrayOf(100, 90, 80, 70, 50)
        MaterialAlertDialogBuilder(this)
            .setTitle("#2 Kart İç Zemin Saydamlığı")
            .setItems(secenekler) { _, idx ->
                aktifProfil = aktifProfil.copy(kartSaydamlikYuzde = alfalar[idx])
                canliAynayiTazele()
                Toast.makeText(this, "Saydamlık: %${alfalar[idx]}", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun koseYaricapDiyalogu() {
        val secenekler = arrayOf("0dp (Keskin Köşe)", "12dp (Küçük)", "16dp (Varsayılan v2)", "24dp (Devasa Oval)")
        MaterialAlertDialogBuilder(this)
            .setTitle("#6 Serbest Köşe Yuvarlaklığı")
            .setItems(secenekler) { _, idx ->
                aktifProfil = aktifProfil.copy(koseYaricapiIndeks = idx)
                canliAynayiTazele()
                Toast.makeText(this, "Köşe yarıçapı: ${TasarimAtolye.koseYaricapiDp(idx)}dp", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun ilerlemeBicimDiyalogu() {
        val secenekler = arrayOf("Yatay Çubuk (Varsayılan)", "Kalın Çubuk", "Mini Halka", "Sadece Yüzde Metni")
        MaterialAlertDialogBuilder(this)
            .setTitle("#10 İlerleme Göstergesi Biçimi")
            .setItems(secenekler) { _, idx ->
                aktifProfil = aktifProfil.copy(ilerlemeBicimi = TasarimAtolye.IlerlemeBicimi.entries[idx])
                canliAynayiTazele()
                Toast.makeText(this, "Biçim: ${secenekler[idx]}", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun fontSecimDiyalogu() {
        val secenekler = arrayOf("Poppins (Modern / Şık)", "Atkinson Hyperlegible (Erişilebilir)", "Lora (Kitap Dokusu / Serif)")
        MaterialAlertDialogBuilder(this)
            .setTitle("#11 Başlık / Gövde Font Şablonu")
            .setItems(secenekler) { _, idx ->
                aktifProfil = aktifProfil.copy(baslikFontIndeks = idx, govdeFontIndeks = idx)
                GorunumAyar.setFontSablon(this, idx)
                canliAynayiTazele()
                recreate()
            }
            .show()
    }

    private fun maxLinesDiyalogu() {
        val secenekler = arrayOf("1 Satır (En Kompakt)", "2 Satır", "3 Satır (Varsayılan)", "5 Satır", "10 Satır (Tam Metin)")
        val satirlar = intArrayOf(1, 2, 3, 5, 10)
        MaterialAlertDialogBuilder(this)
            .setTitle("#13 Görev & Not Satır Sınırı")
            .setItems(secenekler) { _, idx ->
                aktifProfil = aktifProfil.copy(maxLines = satirlar[idx])
                canliAynayiTazele()
                Toast.makeText(this, "Satır sınırı: ${satirlar[idx]}", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun akordiyonDurumDiyalogu() {
        val secenekler = arrayOf("Her Zaman Açık", "Her Zaman Kapalı", "Sadece Doluyken Açık (Varsayılan)")
        MaterialAlertDialogBuilder(this)
            .setTitle("#18 Akordiyon Varsayılan Durumu")
            .setItems(secenekler) { _, idx ->
                aktifProfil = aktifProfil.copy(akordiyonDurum = TasarimAtolye.AkordiyonDurum.entries[idx])
                canliAynayiTazele()
                Toast.makeText(this, "Akordiyon durumu ayarlandı", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun saniyeEfektiDiyalogu() {
        val secenekler = arrayOf("Rulo / Flip Efekti (Varsayılan)", "Düz Metin Akışı", "Gizli / Zen Odak Modu")
        MaterialAlertDialogBuilder(this)
            .setTitle("#22 Saniye Akış Efekti")
            .setItems(secenekler) { _, idx ->
                val ef = TasarimAtolye.SaniyeEfekti.entries[idx]
                aktifProfil = aktifProfil.copy(saniyeEfekti = ef)
                GorunumAyar.setZenOdak(this, ef == TasarimAtolye.SaniyeEfekti.GIZLI_ZEN)
                canliAynayiTazele()
                Toast.makeText(this, "Saniye efekti: ${secenekler[idx]}", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun fabIslevDiyalogu() {
        val secenekler = arrayOf("Görev Ekle (Varsayılan)", "25 dk Odak Sayacı Başlat", "Komut Paleti Aç (⌘K)", "AI Ajanı & Otopilotunu Aç")
        MaterialAlertDialogBuilder(this)
            .setTitle("#29 Akıllı Artı (+) Butonu İşlevi")
            .setItems(secenekler) { _, idx ->
                aktifProfil = aktifProfil.copy(fabKonumu = TasarimAtolye.FabKonum.SAG_ALT)
                GorunumAyar.setFabIslev(this, idx)
                canliAynayiTazele()
                Toast.makeText(this, "FAB işlevi: ${secenekler[idx]}", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
}
