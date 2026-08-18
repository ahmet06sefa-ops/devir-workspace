package com.gunlukasistan.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/**
 * v10.75 — Günlük Açıklamalı ve Detaylı Çalışma & Yaşam Tablosu Ekranı.
 *
 * Kullanıcının 30 gün boyunca her günün odak süresi, pomodoro sayısı, çalışılan dersler,
 * soru doğruluk oranı, namaz senkronu, tansiyon/şeker sağlığı ve günlük koçluk yorumlarını
 * eksiksiz bir tabloda (ASCII Executive Table) gün gün incelemesini sağlar.
 */
class GunlukDetayTabloActivity : AppCompatActivity() {

    private var seciliGunNo = 10 // Varsayılan: Bugün (10 Ağustos 2026)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gunluk_detay_tablo)

        seciliGunNo = intent.getIntExtra(EXTRA_GUN_NO, 10).coerceIn(1, 30)

        val btnGeri = findViewById<ImageButton>(R.id.btnGeri)
        btnGeri.setOnClickListener { finish() }

        initTabloyuGuncelle()
        initButonEtkilesimleri()
    }

    private fun initTabloyuGuncelle() {
        val kayit = GunlukAktiviteTabloMotoru.gunKaydiGetir(seciliGunNo, this@GunlukDetayTabloActivity)

        val txtBaslik = findViewById<TextView>(R.id.txtSeciliTarih)
        txtBaslik.text = "📍 Seçili Gün: ${kayit.tarihStr} ${kayit.gunAdi} (${kayit.harfNotu} Karne Notu)"

        val txtKarne = findViewById<TextView>(R.id.txtGunlukKarne)
        txtKarne.text = GunlukAktiviteTabloMotoru.asciiGunlukKarneOlustur(kayit)
    }

    private fun initButonEtkilesimleri() {
        val btnOnceki = findViewById<Button>(R.id.btnOncekiGun)
        btnOnceki.setOnClickListener {
            if (seciliGunNo > 1) {
                seciliGunNo--
                initTabloyuGuncelle()
            } else {
                Toast.makeText(this, "İlk gündesiniz (1 Ağustos)", Toast.LENGTH_SHORT).show()
            }
        }

        val btnSonraki = findViewById<Button>(R.id.btnSonrakiGun)
        btnSonraki.setOnClickListener {
            if (seciliGunNo < 30) {
                seciliGunNo++
                initTabloyuGuncelle()
            } else {
                Toast.makeText(this, "Son gündesiniz (30 Ağustos)", Toast.LENGTH_SHORT).show()
            }
        }

        val btnGunSec = findViewById<Button>(R.id.btnGunSec)
        btnGunSec.setOnClickListener {
            val gunler = (1..30).map { "$it Ağustos 2026 (${GunlukAktiviteTabloMotoru.gunKaydiGetir(it, this@GunlukDetayTabloActivity).gunAdi})" }.toTypedArray()
            AlertDialog.Builder(this)
                .setTitle("İncelenecek Günü Seçin (1-30 Ağustos)")
                .setItems(gunler) { _, index ->
                    seciliGunNo = index + 1
                    initTabloyuGuncelle()
                    Toast.makeText(this, "📅 Gün Yüklendi: $seciliGunNo Ağustos", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Kapat", null)
                .show()
        }

        val btnKopyala = findViewById<Button>(R.id.btnKarneKopyala)
        btnKopyala.setOnClickListener {
            val kayit = GunlukAktiviteTabloMotoru.gunKaydiGetir(seciliGunNo, this@GunlukDetayTabloActivity)
            val ascii = GunlukAktiviteTabloMotoru.asciiGunlukKarneOlustur(kayit)
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            cm?.setPrimaryClip(ClipData.newPlainText("Gunluk Tablo", ascii))
            Toast.makeText(this, "📋 Günlük Detaylı Tablo Kopyalandı!", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val EXTRA_GUN_NO = "secili_gun_no"
        fun ac(context: Context, gunNo: Int = 10) {
            val intent = android.content.Intent(context, GunlukDetayTabloActivity::class.java)
            intent.putExtra(EXTRA_GUN_NO, gunNo)
            context.startActivity(intent)
        }
    }
}
