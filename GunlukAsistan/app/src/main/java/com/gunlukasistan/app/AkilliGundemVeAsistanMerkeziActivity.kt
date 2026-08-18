package com.gunlukasistan.app

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/**
 * v10.69 — Akıllı Gündem, Biyo-Ritim Brifingi & Otonom Asistan Merkezi.
 *
 * Kullanıcının günlük brifinglerini almasını, 24 saatlik biyo-vakit bloklarını izlemesini,
 * kararsızlık anında tek dokunuşla "Bugün ne yapmalıyım?" tavsiyesi almasını ve haftalık
 * bütüncül karnesini (ASCII Executive Card) görüntülemesini sağlar.
 */
class AkilliGundemVeAsistanMerkeziActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_akilli_gundem_merkezi)

        val btnGeri = findViewById<ImageButton>(R.id.btnGeri)
        btnGeri.setOnClickListener { finish() }

        initOzetBilgiler()
        initButonEtkilesimleri()
    }

    private fun initOzetBilgiler() {
        // Biyo-Vakit ve DND
        val txtBiyoDnd = findViewById<TextView>(R.id.txtBiyoVeDnd)
        val blok = AkilliGundemVeAsistanMerkezi.BiyoVakitOrkestratoru.suAnkiBlokuBul(14)
        val dnd = AkilliGundemVeAsistanMerkezi.AkilliDndOtomasyonu.dndDurumuGetir(false)
        txtBiyoDnd.text = "🕰️ [14:00] ${blok.blokAdi}: ${blok.onerilenAktivite}\n${dnd.second}"

        // Öneri ve Sokratik
        val txtOneri = findViewById<TextView>(R.id.txtOneriVeSokratik)
        val oneri = AkilliGundemVeAsistanMerkezi.BugunNeYapayimAsistan.anlikOneriUret(10, false)
        val sokratik = AkilliGundemVeAsistanMerkezi.AnlikMotivasyonKocu.sokratikRehberlikAl("istemi")
        txtOneri.text = "💡 Öneri: '${oneri.baslik}' (${oneri.sureDakika} dk • +${oneri.xpOdulu} XP)\n$sokratik"
    }

    private fun initButonEtkilesimleri() {
        // Sabah / Akşam Brifingi
        val btnBrifing = findViewById<Button>(R.id.btnBrifingTest)
        val txtBrifing = findViewById<TextView>(R.id.txtBrifingSonuc)
        btnBrifing.setOnClickListener {
            val secimler = arrayOf("Sabah Gündem Brifingi (08:00)", "Akşam Değerlendirme Brifingi (20:30)")
            val vakitler = arrayOf("SABAH", "AKSAM")
            AlertDialog.Builder(this)
                .setTitle("Sesli / Görsel Gündem Brifingi")
                .setItems(secimler) { _, index ->
                    val b = AkilliGundemVeAsistanMerkezi.GundemBrifingMotoru.brifingOlustur(vakitler[index], "Ahmet", context = this@AkilliGundemVeAsistanMerkeziActivity)
                    val metin = AkilliGundemVeAsistanMerkezi.GundemBrifingMotoru.brifingMetniFormatla(b)
                    txtBrifing.text = metin
                    Toast.makeText(this, "Brifing Aktif: ${vakitler[index]}", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Kapat", null)
                .show()
        }

        // Bugün Ne Yapmalıyım?
        val btnOneri = findViewById<Button>(R.id.btnOneriTest)
        val txtOneri = findViewById<TextView>(R.id.txtOneriVeSokratik)
        btnOneri.setOnClickListener {
            val senaryolar = arrayOf(
                "Sabah Dinç Odak (Saat 09:30)",
                "Öğleden Sonra Pratik (Saat 14:30)",
                "Zihinsel Yorgunluk Seziyorum"
            )
            AlertDialog.Builder(this)
                .setTitle("Akıllı Otonom Karar Asistanı")
                .setItems(senaryolar) { _, index ->
                    val oneri = when (index) {
                        0 -> AkilliGundemVeAsistanMerkezi.BugunNeYapayimAsistan.anlikOneriUret(9, false)
                        1 -> AkilliGundemVeAsistanMerkezi.BugunNeYapayimAsistan.anlikOneriUret(14, false)
                        else -> AkilliGundemVeAsistanMerkezi.BugunNeYapayimAsistan.anlikOneriUret(14, true)
                    }
                    txtOneri.text = "💡 [OTONOM ÖNERİ]: ${oneri.baslik}\n${oneri.gerekce} (+${oneri.xpOdulu} XP)"
                    Toast.makeText(this, "Hedef Seçildi: ${oneri.baslik}", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Kapat", null)
                .show()
        }

        // Sokratik Koç
        val btnSokratik = findViewById<Button>(R.id.btnSokratikTest)
        btnSokratik.setOnClickListener {
            val sorular = arrayOf(
                "Canım hiç çalışmak istemiyor",
                "Deneme netlerim bir türlü artmıyor",
                "Nereden başlayacağımı bilemiyorum"
            )
            AlertDialog.Builder(this)
                .setTitle("Sokratik Soru-Cevap Koçu")
                .setItems(sorular) { _, index ->
                    val yanit = AkilliGundemVeAsistanMerkezi.AnlikMotivasyonKocu.sokratikRehberlikAl(sorular[index])
                    val txtOneriBox = findViewById<TextView>(R.id.txtOneriVeSokratik)
                    txtOneriBox.text = "$yanit"
                    Toast.makeText(this, "Sokratik Rehberlik Aktif!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Kapat", null)
                .show()
        }

        // Haftalık Bütüncül Karne
        val btnRapor = findViewById<Button>(R.id.btnRaporTest)
        val txtRapor = findViewById<TextView>(R.id.txtRaporSonuc)
        btnRapor.setOnClickListener {
            val rapor = AkilliGundemVeAsistanMerkezi.HaftalikButunculRapor.raporOlustur()
            val ascii = AkilliGundemVeAsistanMerkezi.HaftalikButunculRapor.asciiKarneFormatla(rapor)
            txtRapor.text = ascii
            AlertDialog.Builder(this)
                .setTitle("HAFTALIK BÜTÜNCÜL ASİSTAN KARNESİ")
                .setMessage("$ascii\n\n${rapor.ozetYorum}")
                .setPositiveButton("Tamam", null)
                .show()
        }
    }

    companion object {
        fun ac(context: android.content.Context) {
            context.startActivity(android.content.Intent(context, AkilliGundemVeAsistanMerkeziActivity::class.java))
        }
    }
}
