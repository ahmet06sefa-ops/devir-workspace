package com.gunlukasistan.app

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/**
 * v10.65 — Yaşam Sağlığı & Finans — Uzman Faz 2 (14 Gelişmiş Medikal, Bütçe, Otonomasyon & Frekans Modülü).
 *
 * Kullanıcının tansiyon/şeker seyrini, 16:8 aralıklı oruç saatlerini, bütçe/portföy varlığını,
 * abonelik tasarruf simülasyonunu ve binaural odak frekanslarını interaktif olarak yönetmesini sağlar.
 */
class YasamSaglikFinansActivity : AppCompatActivity() {

    private val frekanslar = listOf(40, 14, 10, 4)
    private var seciliFrekansIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_yasam_saglik_finans)

        val btnGeri = findViewById<ImageButton>(R.id.btnGeri)
        btnGeri.setOnClickListener { finish() }

        initMedikalVeFinansOzeti()
        initButonEtkilesimleri()
    }

    private fun initMedikalVeFinansOzeti() {
        // Medikal özet
        val txtMedikal = findViewById<TextView>(R.id.txtMedikalDurum)
        val kayit = YasamSaglikFinansFaz2.TansiyonKaydi(120, 80, 95)
        val yorumlar = YasamSaglikFinansFaz2.Faz2_1_SaglikVeMedikal.tansiyonVeSekerDegerlendir(kayit)
        txtMedikal.text = "${yorumlar.first} · ${yorumlar.second}"

        // Finans özeti
        val txtFinans = findViewById<TextView>(R.id.txtFinansOzeti)
        val radar = YasamSaglikFinansFaz2.Faz2_3_ButceVeBorc.harcamaRadarDurumu(500, 380)
        val borcList = listOf(
            YasamSaglikFinansFaz2.BorcAlacakKaydi("Ahmet", 1500, true),
            YasamSaglikFinansFaz2.BorcAlacakKaydi("Market", 400, false)
        )
        val netBorc = YasamSaglikFinansFaz2.Faz2_3_ButceVeBorc.netAlacakBorcHesapla(borcList)
        val portfoy = YasamSaglikFinansFaz2.Faz2_4_VarlikVeKumbara.toplamPortfoyDegeriTl(
            YasamSaglikFinansFaz2.PortfoyVarlik(5.0, 250.0, 100.0)
        )
        txtFinans.text = "${radar.second}\n${netBorc.second}\n${portfoy.second}"
    }

    private fun initButonEtkilesimleri() {
        // 4-7-8 Nefes Rehberi
        val btnNefes = findViewById<Button>(R.id.btnNefesTest)
        val txtMedikal = findViewById<TextView>(R.id.txtMedikalDurum)
        btnNefes.setOnClickListener {
            val modlar = arrayOf("4-7-8 Sakinleştirici Nefes", "Kare Nefes (4-4-4-4)")
            AlertDialog.Builder(this)
                .setTitle("Nefes Egzersiz Modunu Seçin (#4)")
                .setItems(modlar) { _, index ->
                    val secilenMod = if (index == 0) "478" else "KARE"
                    val (baslik, detay) = YasamSaglikFinansFaz2.Faz2_1_SaglikVeMedikal.nefesEgzersiziMetniGetir(secilenMod)
                    txtMedikal.text = "🧘 $baslik: $detay"
                    Toast.makeText(this, "Nefes Rehberi Aktif: $baslik", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Kapat", null)
                .show()
        }

        // 16:8 Aralıklı Oruç
        val btnOruc = findViewById<Button>(R.id.btnOrucTest)
        val txtOruc = findViewById<TextView>(R.id.txtOrucDurum)
        btnOruc.setOnClickListener {
            val saatler = arrayOf("18:00 (Erken Akşam)", "20:00 (Klasik Akşam)", "22:00 (Gece Öğünü)")
            val saatDegerleri = arrayOf(18, 20, 22)
            AlertDialog.Builder(this)
                .setTitle("Son Öğün Saatinizi Seçin (#10)")
                .setItems(saatler) { _, index ->
                    val (bitis, mesaj) = YasamSaglikFinansFaz2.Faz2_2_BeslenmeVeOruc.aralikliOruc168Hesapla(saatDegerleri[index])
                    txtOruc.text = "⏳ $mesaj"
                    Toast.makeText(this, "Oruç Bitiş: ${bitis}:00", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Kapat", null)
                .show()
        }

        // Abonelik Tasarruf Simülatörü
        val btnSim = findViewById<Button>(R.id.btnSimTest)
        val txtSim = findViewById<TextView>(R.id.txtSimSonuc)
        btnSim.setOnClickListener {
            val abonelikler = YasamSaglikFinansFaz2.Faz2_5_AbonelikTasarruf.varsayilanAbonelikler()
            val sim = YasamSaglikFinansFaz2.Faz2_5_AbonelikTasarruf.yillikTasarrufSimuleEt(abonelikler)
            txtSim.text = "🎉 ${sim.second}"
            Toast.makeText(this, "Yıllık ${sim.first} ₺ Tasarruf Hesaplandı!", Toast.LENGTH_SHORT).show()
        }

        // Binaural Odak Frekans Mikseri
        val btnFrekans = findViewById<Button>(R.id.btnFrekansTest)
        val txtFrekans = findViewById<TextView>(R.id.txtFrekansOzeti)
        btnFrekans.setOnClickListener {
            val f = frekanslar[seciliFrekansIndex]
            seciliFrekansIndex = (seciliFrekansIndex + 1) % frekanslar.size
            val aciklama = YasamSaglikFinansFaz2.Faz2_7_FrekansVeGuvenlik.binauralFrekansAciklamasi(f)
            txtFrekans.text = "🎧 $aciklama"
            Toast.makeText(this, "Frekans Değişti: $f Hz", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        fun ac(context: android.content.Context) {
            context.startActivity(android.content.Intent(context, YasamSaglikFinansActivity::class.java))
        }
    }
}
