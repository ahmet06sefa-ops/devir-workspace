package com.gunlukasistan.app

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/**
 * v10.77 — KPSS / YKS Merkezi Yönetim, Hedef & Ayarlar Atölyesi Ekranı.
 *
 * Kullanıcının "ayarlarda kpss icin herseyini yonetebilecegim bir yer ayarla ve
 * bütün ayarlarini ordan yapabileceyim." talimatı doğrultusunda: hedef puan,
 * hedef net, mevcut net, sınav adı, turlama hızı ve tüm 9 çalışma atölyesini
 * tek bir ekrandan yönetmesini sağlar.
 */
class KpssMerkeziYonetimActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kpss_merkezi_yonetim)

        val btnGeri = findViewById<ImageButton>(R.id.btnGeri)
        btnGeri.setOnClickListener { finish() }

        initDurumYaz()
        initButonEtkilesimleri()
    }

    private fun initDurumYaz() {
        val txtDurum = findViewById<TextView>(R.id.txtKpssDurum)
        txtDurum.text = KpssModuKararMotoru.merkeziOzetMetniGetir(this)
    }

    private fun initButonEtkilesimleri() {
        // Hedef Puan Değiştir
        val btnPuan = findViewById<Button>(R.id.btnPuanAyarla)
        btnPuan.setOnClickListener {
            val puanlar = arrayOf("400 Puan (Temel Baraj)", "450 Puan (Atanma Hedefi)", "480 Puan (Derece Hedefi)")
            val degerler = intArrayOf(400, 450, 480)
            AlertDialog.Builder(this)
                .setTitle("Hedef KPSS / YKS Puanı Seçin")
                .setItems(puanlar) { _, idx ->
                    KpssModuKararMotoru.hedefPuanKaydet(this, degerler[idx])
                    initDurumYaz()
                    Toast.makeText(this, "🎯 Hedef Puan: ${degerler[idx]} olarak kaydedildi!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Kapat", null)
                .show()
        }

        // Hedef / Mevcut Net
        val btnNet = findViewById<Button>(R.id.btnNetAyarla)
        btnNet.setOnClickListener {
            val senaryolar = arrayOf(
                "Hedef 85.0 Net / Mevcut 70.0 Net",
                "Hedef 90.0 Net / Mevcut 78.5 Net",
                "Hedef 105.0 Net / Mevcut 92.0 Net"
            )
            val netCiftleri = listOf(
                Pair(85.0f, 70.0f),
                Pair(90.0f, 78.5f),
                Pair(105.0f, 92.0f)
            )
            AlertDialog.Builder(this)
                .setTitle("Hedef & Mevcut Deneme Neti Ayarla")
                .setItems(senaryolar) { _, idx ->
                    val (h, m) = netCiftleri[idx]
                    KpssModuKararMotoru.hedefNetKaydet(this, h)
                    KpssModuKararMotoru.mevcutNetKaydet(this, m)
                    initDurumYaz()
                    Toast.makeText(this, "📈 Net Senaryosu Güncellendi!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Kapat", null)
                .show()
        }

        // Sınav Adı Seç
        val btnSinav = findViewById<Button>(R.id.btnSinavSec)
        btnSinav.setOnClickListener {
            val sinavlar = arrayOf("KPSS Lisans 2026", "KPSS Önlisans 2026", "YKS / TYT-AYT 2026", "ALES / DGS 2026")
            AlertDialog.Builder(this)
                .setTitle("Hedef Sınav Türü Seçin")
                .setItems(sinavlar) { _, idx ->
                    KpssModuKararMotoru.sinavAdiKaydet(this, sinavlar[idx])
                    initDurumYaz()
                    Toast.makeText(this, "📌 Sınav Türü Değişti: ${sinavlar[idx]}", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Kapat", null)
                .show()
        }

        // 4 Çalışma Atölyesi Butonları
        findViewById<View>(R.id.btnDersKolaylikAc)?.setOnClickListener {
            DersKolaylikActivity.ac(this)
        }
        findViewById<View>(R.id.btnDersIleriFazAc)?.setOnClickListener {
            DersIleriFazActivity.ac(this)
        }
        findViewById<View>(R.id.btnDersUzmanMerkezAc)?.setOnClickListener {
            DersUzmanMerkezActivity.ac(this)
        }
        findViewById<View>(R.id.btnDersUzmanFaz6Ac)?.setOnClickListener {
            DersUzmanFaz6Activity.ac(this)
        }
    }

    companion object {
        fun ac(context: android.content.Context) {
            context.startActivity(android.content.Intent(context, KpssMerkeziYonetimActivity::class.java))
        }
    }
}
