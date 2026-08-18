package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File

/**
 * v11.12 — Tüm Verileri Yedekle & Geri Yükle Ekranı (`VeriYedekActivity`).
 *
 * `TumVeriYedeklemeMotoru` motorunun Android arayüzü:
 *  · "📤 Dışa Aktar": tüm SharedPreferences içeriklerini JSON yedeğine çevirir,
 *    önbelleğe yazar ve FileProvider üzerinden paylaşma ekranını açar.
 *  · "📥 İçe Aktar": .json dosyası seçtirir, motor ile bütünlüğü doğrular ve
 *    tüm anahtarları birebir geri yükler.
 */
class VeriYedekActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            context.startActivity(Intent(context, VeriYedekActivity::class.java))
        }
    }

    private val dosyaSecici =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) dosyadanGeriYukle(uri) else {
                Toast.makeText(this, "Dosya seçilmedi.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(GorunumAyar.yaziOlcegiUygula(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.styleFor(this))
        ThemeManager.applyAccent(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_veri_yedek)

        findViewById<ImageButton>(R.id.btnGeri).setOnClickListener { finish() }

        findViewById<android.widget.Button>(R.id.btnDisarAktar).setOnClickListener { disarAktar() }
        findViewById<android.widget.Button>(R.id.btnIcerAktar).setOnClickListener {
            dosyaSecici.launch("application/json")
        }
    }

    override fun onResume() {
        super.onResume()
        GlassmorphismTemaMotoru.sekmeleriVeKartlariStille(window.decorView, this)
    }

    // ════════════════════════════════════════════════
    // Dışa aktarma
    // ════════════════════════════════════════════════

    private fun disarAktar() {
        val veri = tumPrefsOku()
        val json = TumVeriYedeklemeMotoru.yedekOlustur(veri, "11.13")
        val dosya = yedekDosyasiYaz(json)
        val meta = TumVeriYedeklemeMotoru.metaBilgi(json)
        val saglama = meta?.saglama ?: "?"

        findViewById<TextView>(R.id.txtDurum)?.text =
            "✅ Yedek hazır! ${meta?.dosyaSayisi ?: 0} dosya, ${meta?.kalanSayisi ?: 0} anahtar tek dosyada toplandı.\n\nDosyayı WhatsApp, Google Drive, bulut veya dosya yöneticisi ile istediğiniz yere kaydedin."
        findViewById<TextView>(R.id.txtSaglama)?.text = "Sağlama: $saglama"

        val uri: Uri = try {
            FileProvider.getUriForFile(this, "$packageName.fileprovider", dosya)
        } catch (_: Exception) {
            Toast.makeText(this, "Yedek dosyası oluşturulamadı.", Toast.LENGTH_LONG).show()
            return
        }
        val paylas = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Günlük Asistan tam veri yedeği — sağlama: $saglama")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(paylas, "Yedek dosyasını paylaş"))
    }

    // ════════════════════════════════════════════════
    // İçe aktarma (geri yükleme)
    // ════════════════════════════════════════════════

    private fun dosyadanGeriYukle(uri: Uri) {
        val json = try {
            contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
        } catch (_: Exception) {
            null
        }
        if (json.isNullOrBlank()) {
            Toast.makeText(this, "Dosya okunamadı.", Toast.LENGTH_LONG).show()
            return
        }
        if (!TumVeriYedeklemeMotoru.yedekDogrula(json)) {
            Toast.makeText(this, "Geçersiz veya bozulmuş yedek dosyası.", Toast.LENGTH_LONG).show()
            findViewById<TextView>(R.id.txtDurum)?.text =
                "⚠️ Geçersiz yedek: biçim veya sağlama doğrulanamadı."
            return
        }
        val geri = TumVeriYedeklemeMotoru.geriYukle(json)
        geriYuklePrefslere(geri)
        val meta = TumVeriYedeklemeMotoru.metaBilgi(json)
        findViewById<TextView>(R.id.txtDurum)?.text =
            "✅ ${meta?.dosyaSayisi ?: 0} dosya, ${meta?.kalanSayisi ?: 0} anahtar başarıyla geri yüklendi."
        findViewById<TextView>(R.id.txtSaglama)?.text =
            "Sağlama: ${meta?.saglama ?: "?"}"
        Toast.makeText(this, "Veriler geri yüklendi!", Toast.LENGTH_LONG).show()
    }

    // ════════════════════════════════════════════════
    // Yardımcılar
    // ════════════════════════════════════════════════

    /** Tüm SharedPreferences dosyalarını {dosyaAdı → (anahtar → değer)} olarak okur. */
    private fun tumPrefsOku(): Map<String, Map<String, Any?>> {
        val dizin = File(dataDir, "shared_prefs")
        val dosyalar = dizin.listFiles { f -> f.extension == "xml" } ?: return emptyMap()
        val sonuc = mutableMapOf<String, Map<String, Any?>>()
        for (f in dosyalar) {
            try {
                val p = getSharedPreferences(f.nameWithoutExtension, Context.MODE_PRIVATE)
                sonuc[f.nameWithoutExtension] = p.all
            } catch (_: Exception) {
                // tek bir bozuk dosya tüm yedeği engellemesin
            }
        }
        return sonuc
    }

    private fun yedekDosyasiYaz(json: String): File {
        val klasor = File(cacheDir, "yedek").apply { if (!exists()) mkdirs() }
        val dosya = File(klasor, "ga-tam-yedek-${System.currentTimeMillis()}.json")
        dosya.writeText(json, Charsets.UTF_8)
        return dosya
    }

    /** Motorun ürettiği {dosyaAdı → (anahtar → değer)} haritasını prefs'e yazar. */
    private fun geriYuklePrefslere(harita: Map<String, Map<String, Any?>>) {
        for ((adi, kalan) in harita) {
            val p = getSharedPreferences(adi, Context.MODE_PRIVATE)
            val ed = p.edit().clear()
            for ((k, v) in kalan) {
                when (v) {
                    is String -> ed.putString(k, v)
                    is Int -> ed.putInt(k, v)
                    is Long -> ed.putLong(k, v)
                    is Float -> ed.putFloat(k, v)
                    is Boolean -> ed.putBoolean(k, v)
                    is Set<*> -> ed.putStringSet(k, v.map { it.toString() }.toSet())
                }
            }
            ed.commit()
        }
    }
}
