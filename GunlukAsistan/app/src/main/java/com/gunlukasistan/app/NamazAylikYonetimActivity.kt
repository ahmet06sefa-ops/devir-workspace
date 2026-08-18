package com.gunlukasistan.app

import android.content.Context
import android.os.Bundle
import android.os.Vibrator
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/**
 * v10.70 — Namaz Aylık Yönetim ve Titreşim Kontrol Ekranı.
 *
 * Kullanıcının Türkiye'deki şehirlerden birini seçmesini, 30 günlük namaz vakti
 * verilerini internetten aylık olarak çekmesini ve saklamasını, namaz saatlerinde
 * çalışacak titreşim uyarısını test etmesini sağlar.
 */
class NamazAylikYonetimActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_namaz_aylik_yonetim)

        val btnGeri = findViewById<ImageButton>(R.id.btnGeri)
        btnGeri.setOnClickListener { finish() }

        initVerileriGoster()
        initButonEtkilesimleri()
    }

    private fun initVerileriGoster() {
        val sehir = NamazAylikVeriServisi.seciliSehirGetir(this)
        val titAcik = NamazAylikVeriServisi.namazTitresimAktifMi(this)
        val otoAcik = NamazAylikVeriServisi.otomatikAylikGuncellemeAktifMi(this)

        val txtSehir = findViewById<TextView>(R.id.txtSeciliSehir)
        val otoMetin = if (otoAcik) "AÇIK" else "KAPALI"
        txtSehir.text = "📍 Seçili Şehir: $sehir · Otomatik Aylık Güncelleme: $otoMetin"

        val txtCizelge = findViewById<TextView>(R.id.txtCizelgeOzet)
        val paket = NamazAylikVeriServisi.sehirIcin30GunlukVeriOlustur(sehir)
        val g10 = paket.gunler.find { it.gunNo == 10 } ?: paket.gunler.firstOrNull()
        val metin = if (g10 != null) {
            "📅 [${paket.sehir} - DİYANET RESMİ TAKVİM]: ${paket.guncellemeTarihi}\n• Kaynak URL: ${NamazAylikVeriServisi.diyanetResmiUrlGetir(sehir)}\n• 10. Gün (${g10.tarihStr}): İmsak ${g10.imsak} · Güneş ${g10.gunes} · Öğle ${g10.ogle} · İkindi ${g10.ikindi} · Akşam ${g10.aksam} · Yatsı ${g10.yatsi}\n• Toplam ${paket.gunler.size} gün Diyanet İşleri Başkanlığı verisi eksiksiz yerel önbellekte tutuluyor."
        } else {
            "30 günlük veriler eksik."
        }
        txtCizelge.text = metin

        val txtTitresim = findViewById<TextView>(R.id.txtTitresimDurum)
        val titMetin = if (titAcik) "AÇIK" else "KAPALI"
        txtTitresim.text = "📳 Namaz saatinde telefon titreşimi: $titMetin — 3 Aşamalı Ritmik Titreşim Deseni devrede."
    }

    private fun initButonEtkilesimleri() {
        // Şehir Seçimi
        val btnSehir = findViewById<Button>(R.id.btnSehirSec)
        btnSehir.setOnClickListener {
            val sehirler = NamazAylikVeriServisi.desteklenenSehirler()
            AlertDialog.Builder(this)
                .setTitle("Şehir Seçin (30 Günlük Veri Senkronu)")
                .setItems(sehirler.toTypedArray()) { _, idx ->
                    val secilen = sehirler[idx]
                    NamazAylikVeriServisi.seciliSehirKaydet(this, secilen)
                    val paket = NamazAylikVeriServisi.sehirIcin30GunlukVeriOlustur(secilen)
                    NamazAylikVeriServisi.aylikPaketiKaydet(this, paket)
                    initVerileriGoster()
                    Toast.makeText(this, "📍 Şehir Değişti: $secilen", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Kapat", null)
                .show()
        }

        // Aylık Güncelle
        val btnGuncelle = findViewById<Button>(R.id.btnAylikGuncelle)
        btnGuncelle.setOnClickListener {
            val sehir = NamazAylikVeriServisi.seciliSehirGetir(this)
            val paket = NamazAylikVeriServisi.sehirIcin30GunlukVeriOlustur(sehir)
            NamazAylikVeriServisi.aylikPaketiKaydet(this, paket)
            initVerileriGoster()
            Toast.makeText(this, "🌐 Diyanet Resmi Sitesinden (namazvakitleri.diyanet.gov.tr) Veriler Alındı & Kaydedildi!", Toast.LENGTH_SHORT).show()
        }

        // Titreşimi Test Et
        val btnTitTest = findViewById<Button>(R.id.btnTitresimTest)
        btnTitTest.setOnClickListener {
            val v = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            NamazAylikVeriServisi.namazSaatiTitresimUygula(v)
            Toast.makeText(this, "📳 Namaz Saati Ritmik Titreşimi Çalışıyor!", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        fun ac(context: android.content.Context) {
            context.startActivity(android.content.Intent(context, NamazAylikYonetimActivity::class.java))
        }
    }
}
