package com.gunlukasistan.app

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/**
 * v10.67 — Yaşam Sağlığı & Finans — Uzman Faz 3 (14 Çevrimdışı Güvenlik, Deprem Tahliye, Tıbbi Kart & Yedekleme Modülü).
 *
 * Kullanıcının deprem kontrol listesini, SOS acil mesajını, tıbbi alerji kartını, depolama/çökme tanılarını,
 * çevrimdışı pusula referansını ve JSON yedekleme paketlerini interaktif olarak yönetmesini sağlar.
 */
class YasamSaglikFinansFaz3Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_yasam_saglik_finans_faz3)

        val btnGeri = findViewById<ImageButton>(R.id.btnGeri)
        btnGeri.setOnClickListener { finish() }

        initOzetBilgiler()
        initButonEtkilesimleri()
    }

    private fun initOzetBilgiler() {
        // Depolama ve çökme arşivi
        val txtDepolama = findViewById<TextView>(R.id.txtDepolamaOzeti)
        val kalemler = YasamSaglikFinansFaz3.Faz3_4_DepolamaVeCokme.varsayilanDepolama()
        val depoOzet = YasamSaglikFinansFaz3.Faz3_4_DepolamaVeCokme.depolamaOzetle(kalemler)
        val cokme = YasamSaglikFinansFaz3.Faz3_4_DepolamaVeCokme.sonCokmeTanisiGetir()
        txtDepolama.text = "${depoOzet.second}\n$cokme"

        // Canlı bildirim
        val txtCanli = findViewById<TextView>(R.id.txtCanliOzeti)
        val kilit = YasamSaglikFinansFaz3.Faz3_7_CanliDurum.kilitEkraniMesajiGetir(true)
        val hap = YasamSaglikFinansFaz3.Faz3_7_CanliDurum.yuzebilenDurumSeridiMetni(40)
        txtCanli.text = "$kilit\n$hap"
    }

    private fun initButonEtkilesimleri() {
        // Deprem Tahliye Kontrol Listesi
        val btnDeprem = findViewById<Button>(R.id.btnDepremTest)
        val txtSos = findViewById<TextView>(R.id.txtSosSonuc)
        btnDeprem.setOnClickListener {
            val adimlar = YasamSaglikFinansFaz3.Faz3_1_DepremVeSos.depremTahliyeAdimlari()
            AlertDialog.Builder(this)
                .setTitle("Deprem & Acil Durum Tahliye Planı (#52)")
                .setItems(adimlar.toTypedArray()) { _, index ->
                    val durum = YasamSaglikFinansFaz3.Faz3_1_DepremVeSos.tahliyeHazirlikDurumu(index + 1, adimlar.size)
                    txtSos.text = "✅ ${adimlar[index]}\n${durum.second}"
                    Toast.makeText(this, "Adım Onaylandı: #${index + 1}", Toast.LENGTH_SHORT).show()
                }
                .setPositiveButton("Tümünü Onayla") { _, _ ->
                    val durum = YasamSaglikFinansFaz3.Faz3_1_DepremVeSos.tahliyeHazirlikDurumu(adimlar.size, adimlar.size)
                    txtSos.text = durum.second
                    Toast.makeText(this, "🚨 Tahliye Planı 100% Hazır!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Kapat", null)
                .show()
        }

        // SOS Acil Mesajı
        val btnSos = findViewById<Button>(R.id.btnSosTest)
        btnSos.setOnClickListener {
            val mesaj = YasamSaglikFinansFaz3.Faz3_1_DepremVeSos.sosAcilMesajiOlustur(
                "Ankara / Kızılay", "A Rh+", "Ahmet (0555 123 45 67)"
            )
            txtSos.text = "📲 [SOS MESAJI HAZIR]: $mesaj"
            Toast.makeText(this, "SOS Mesajı Kopyalamaya Hazır!", Toast.LENGTH_SHORT).show()
        }

        // Acil Tıbbi Kart
        val btnKart = findViewById<Button>(R.id.btnKartTest)
        val txtKart = findViewById<TextView>(R.id.txtKartOzeti)
        btnKart.setOnClickListener {
            val kart = YasamSaglikFinansFaz3.Faz3_3_AcilIlacKarti.varsayilanTibbiKart()
            val metin = YasamSaglikFinansFaz3.Faz3_3_AcilIlacKarti.tibbiKartMetniOlustur(kart)
            txtKart.text = metin
            AlertDialog.Builder(this)
                .setTitle("🚨 ACİL TIBBİ ALERJİ VE İLAÇ KARTI (#59)")
                .setMessage(metin)
                .setPositiveButton("Tamam", null)
                .show()
        }

        // Bütüncül JSON Export
        val btnExport = findViewById<Button>(R.id.btnExportTest)
        val txtExport = findViewById<TextView>(R.id.txtExportOzeti)
        btnExport.setOnClickListener {
            val kart = YasamSaglikFinansFaz3.Faz3_3_AcilIlacKarti.varsayilanTibbiKart()
            val json = YasamSaglikFinansFaz3.Faz3_6_ButunculExport.jsonPaketiOlustur(kart)
            txtExport.text = "📦 BÜTÜNCÜL JSON YEDEK:\n$json"
            Toast.makeText(this, "✅ JSON Yedek Paketi Üretildi!", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        fun ac(context: android.content.Context) {
            context.startActivity(android.content.Intent(context, YasamSaglikFinansFaz3Activity::class.java))
        }
    }
}
