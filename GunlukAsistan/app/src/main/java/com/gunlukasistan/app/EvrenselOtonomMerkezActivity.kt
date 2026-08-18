package com.gunlukasistan.app

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/**
 * v10.68 — Evrensel Otonom Yönetim & 200-Madde Tam Kontrol Merkezi
 * (14 Atölye, Yaşam-Ders Denge Endeksi, Otonomi Seviyesi, Hızlı Komut Paleti & Bütüncül Arşiv).
 *
 * Kullanıcının her iki 100-maddelik katalogdaki tüm araçları tek bir merkezi ekrandan yönetmesini,
 * aramasını, otonomi düzeyini seçmesini ve yaşam-ders dengesini izlemesini sağlar.
 */
class EvrenselOtonomMerkezActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_evrensel_otonom_merkez)

        val btnGeri = findViewById<ImageButton>(R.id.btnGeri)
        btnGeri.setOnClickListener { finish() }

        initOzetBilgiler()
        initButonEtkilesimleri()
    }

    private fun initOzetBilgiler() {
        // Denge ve Rütbe
        val txtDenge = findViewById<TextView>(R.id.txtDengeVeRutbe)
        val skor = EvrenselOtonomMerkez.YasamDersSkor(88, 82)
        val denge = EvrenselOtonomMerkez.YasamDersDengeleyici.butunculDengeEndeksiHesapla(skor)
        val rutbe = EvrenselOtonomMerkez.EvrenselUstalikRutbesi.ustalikRutbesiHesapla(160)
        txtDenge.text = "${denge.second}\n${rutbe.rozetSembol} Evrensel Rütbe: ${rutbe.unvan} (+${rutbe.xpBonus} XP)"

        // Otonomi ve Kasa
        val txtOtonomi = findViewById<TextView>(R.id.txtOtonomiDurum)
        val (baslik, detay) = EvrenselOtonomMerkez.OtonomiSeviyesiKalkani.otonomiAciklamasiGetir("MANUEL")
        val kasa = EvrenselOtonomMerkez.EvrenselCevrimdisiKasa.cevrimdisiArsivDogrula()
        txtOtonomi.text = "$baslik: $detay\n${kasa.second}"

        // Komut ve Sağlık
        val txtKomut = findViewById<TextView>(R.id.txtKomutVeSaglik)
        val saglik = EvrenselOtonomMerkez.EvrenselSistemDenetci.evrenselSaglikRaporuGetir()
        txtKomut.text = "⚡ Komut Paleti: 5 evrensel işlem tek tuşla tetiklenmeye hazır.\n$saglik"
    }

    private fun initButonEtkilesimleri() {
        // 200-Madde İndekste Ara
        val btnArama = findViewById<Button>(R.id.btnAramaTest)
        val txtArama = findViewById<TextView>(R.id.txtAramaSonuc)
        btnArama.setOnClickListener {
            val sorgular = arrayOf("TANSIYON (Yaşam #6)", "POMODORO (Ders #8)", "DEPREM (Yaşam #52)", "LEITNER (Ders #2)")
            val kelimeler = arrayOf("TANSIYON", "POMODORO", "DEPREM", "LEITNER")
            AlertDialog.Builder(this)
                .setTitle("200-Madde Katalogunda Anahtar Kelime Ara")
                .setItems(sorgular) { _, index ->
                    val sonuc = EvrenselOtonomMerkez.EvrenselAramaMotoru.evrenselAra(kelimeler[index])
                    val ilk = sonuc.firstOrNull()
                    if (ilk != null) {
                        txtArama.text = "🌐 [BULUNDU — ${ilk.katalog} KATALOGU]: ${ilk.baslik}\n${ilk.aciklama}"
                    } else {
                        txtArama.text = "Sonuç bulunamadı."
                    }
                    Toast.makeText(this, "Arama Tamamlandı: ${kelimeler[index]}", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Kapat", null)
                .show()
        }

        // Yaşam-Ders Denge Endeksi
        val btnDenge = findViewById<Button>(R.id.btnDengeTest)
        val txtDenge = findViewById<TextView>(R.id.txtDengeVeRutbe)
        btnDenge.setOnClickListener {
            val senaryolar = arrayOf(
                "Yaşam 90 / Ders 85 (Mükemmel Denge)",
                "Yaşam 40 / Ders 85 (Dengesizlik Uyarısı)",
                "Yaşam 65 / Ders 65 (Dengeli Seviye)"
            )
            val skorlar = arrayOf(
                EvrenselOtonomMerkez.YasamDersSkor(90, 85),
                EvrenselOtonomMerkez.YasamDersSkor(40, 85),
                EvrenselOtonomMerkez.YasamDersSkor(65, 65)
            )
            AlertDialog.Builder(this)
                .setTitle("Yaşam-Ders Senaryosu Seçin")
                .setItems(senaryolar) { _, index ->
                    val res = EvrenselOtonomMerkez.YasamDersDengeleyici.butunculDengeEndeksiHesapla(skorlar[index])
                    val r = EvrenselOtonomMerkez.EvrenselUstalikRutbesi.ustalikRutbesiHesapla(160)
                    txtDenge.text = "${res.second}\n${r.rozetSembol} Evrensel Rütbe: ${r.unvan} (+${r.xpBonus} XP)"
                    Toast.makeText(this, "Denge Endeksi: ${res.first}/100", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Kapat", null)
                .show()
        }

        // Otonomi Seviyesi Override
        val btnOtonomi = findViewById<Button>(R.id.btnOtonomiTest)
        val txtOtonomi = findViewById<TextView>(R.id.txtOtonomiDurum)
        btnOtonomi.setOnClickListener {
            val modlar = arrayOf("100% Manuel Kontrol Modu", "Yarı-Otonom Rehber Modu", "Tam Otopilot AI Modu")
            val kodlar = arrayOf("MANUEL", "YARI", "OTOPILOT")
            AlertDialog.Builder(this)
                .setTitle("Otonomi Derecesi Override Anahtarı")
                .setItems(modlar) { _, index ->
                    val (b, d) = EvrenselOtonomMerkez.OtonomiSeviyesiKalkani.otonomiAciklamasiGetir(kodlar[index])
                    val kasa = EvrenselOtonomMerkez.EvrenselCevrimdisiKasa.cevrimdisiArsivDogrula()
                    txtOtonomi.text = "$b: $d\n${kasa.second}"
                    Toast.makeText(this, "Otonomi Anahtarı: $b", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Kapat", null)
                .show()
        }

        // Hızlı Komut Paleti
        val btnKomut = findViewById<Button>(R.id.btnKomutTest)
        val txtKomut = findViewById<TextView>(R.id.txtKomutVeSaglik)
        btnKomut.setOnClickListener {
            val komutlar = EvrenselOtonomMerkez.EvrenselHizliKomut.varsayilanKomutlar()
            val isimler = komutlar.map { it.komutAdi }.toTypedArray()
            AlertDialog.Builder(this)
                .setTitle("Evrensel Hızlı Komut Paleti (Action Launcher)")
                .setItems(isimler) { _, index ->
                    val sonuc = EvrenselOtonomMerkez.EvrenselHizliKomut.komutCalistir(komutlar[index].komutId)
                    val saglik = EvrenselOtonomMerkez.EvrenselSistemDenetci.evrenselSaglikRaporuGetir()
                    txtKomut.text = "${sonuc.second}\n$saglik"
                    Toast.makeText(this, "Komut Çalıştırıldı: ${komutlar[index].komutId}", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Kapat", null)
                .show()
        }
    }

    companion object {
        fun ac(context: android.content.Context) {
            context.startActivity(android.content.Intent(context, EvrenselOtonomMerkezActivity::class.java))
        }
    }
}
