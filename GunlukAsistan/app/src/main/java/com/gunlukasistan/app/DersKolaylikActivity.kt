package com.gunlukasistan.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import java.util.Calendar

/**
 * v10.58 — 10 Uzman Öğrenme & Kullanım Kolaylığı Modülü ([DersKolaylikAtolye]) Arayüzü.
 *
 * Kullanıcının ders çalışma, deneme net takibi, hafıza teknikleri ve kullanım kolaylığı
 * araçlarını tek ekrandan test edip yönettiği aktivite.
 */
class DersKolaylikActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            context.startActivity(Intent(context, DersKolaylikActivity::class.java))
        }
    }

    private var srKonu = DersKolaylikAtolye.TekrarKonusu()
    private var deneme = DersKolaylikAtolye.DenemeSonucu()
    private val antiKalkan = DersKolaylikAtolye.ErtelemeKalkani()
    private val hataKaydi = DersKolaylikAtolye.HataKaydi()
    private var zincirGun = 14
    private var aktifDersFormul = "tarih"

    private lateinit var txtSrDurum: TextView
    private lateinit var txtDenemeNetDurum: TextView
    private lateinit var txtHizliAksiyonDurum: TextView
    private lateinit var txtAntiErtelemeDurum: TextView
    private lateinit var txtHataDefteriDurum: TextView
    private lateinit var txtSprintSablonDurum: TextView
    private lateinit var txtSokratikDurum: TextView
    private lateinit var txtSanalMasaDurum: TextView
    private lateinit var txtNefesKahveDurum: TextView
    private lateinit var txtFormulDurum: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ders_kolaylik)

        txtSrDurum = findViewById(R.id.txtSrDurum)
        txtDenemeNetDurum = findViewById(R.id.txtDenemeNetDurum)
        txtHizliAksiyonDurum = findViewById(R.id.txtHizliAksiyonDurum)
        txtAntiErtelemeDurum = findViewById(R.id.txtAntiErtelemeDurum)
        txtHataDefteriDurum = findViewById(R.id.txtHataDefteriDurum)
        txtSprintSablonDurum = findViewById(R.id.txtSprintSablonDurum)
        txtSokratikDurum = findViewById(R.id.txtSokratikDurum)
        txtSanalMasaDurum = findViewById(R.id.txtSanalMasaDurum)
        txtNefesKahveDurum = findViewById(R.id.txtNefesKahveDurum)
        txtFormulDurum = findViewById(R.id.txtFormulDurum)

        ekraniGuncelle()

        // 100 Öneri Katalog Butonu
        findViewById<MaterialButton>(R.id.btn100DersOneriKatalog).setOnClickListener {
            Toast.makeText(this, "📚 100 Uzman Ders & Kolaylık Önerisi 100-DERS-VE-KOLAYLIK-ONERISI.md dosyasında yayında!", Toast.LENGTH_LONG).show()
        }

        // Modül 1: Leitner Kutu İlerlet
        findViewById<MaterialButton>(R.id.btnLeitnerIlerle).setOnClickListener {
            srKonu = DersKolaylikAtolye.Modul1_AralikliTekrar.kutuIlerle(srKonu)
            ekraniGuncelle()
            Toast.makeText(this, "📚 Kutu: ${srKonu.leitnerKutu} ➔ ${DersKolaylikAtolye.Modul1_AralikliTekrar.sonrakiTekrarGunu(srKonu)}", Toast.LENGTH_SHORT).show()
        }

        // Modül 2: Deneme Net Test
        findViewById<MaterialButton>(R.id.btnDenemeNetTest).setOnClickListener {
            deneme = deneme.copy(dogru = deneme.dogru + 2)
            ekraniGuncelle()
            Toast.makeText(this, "📊 Deneme Güncellendi! Yeni NET: ${DersKolaylikAtolye.Modul2_DenemeNet.netHesapla(deneme.dogru, deneme.yanlis)}", Toast.LENGTH_SHORT).show()
        }

        // Modül 3: Masaya Oturdum
        findViewById<MaterialButton>(R.id.btnMasayaOturdum).setOnClickListener {
            val metin = DersKolaylikAtolye.Modul3_HizliAksiyon.masayaOturKisaYolMetni("KPSS Tarih: Osmanlı Dağılma")
            Toast.makeText(this, metin, Toast.LENGTH_LONG).show()
        }

        // Modül 4: 5 Dakika Kuralı
        findViewById<MaterialButton>(R.id.btn5DakikaKural).setOnClickListener {
            val mot = DersKolaylikAtolye.Modul4_AntiErteleme.besDakikaMotivasyon()
            Toast.makeText(this, mot, Toast.LENGTH_LONG).show()
        }

        // Modül 5: Hata Ekle
        findViewById<MaterialButton>(R.id.btnHataEkle).setOnClickListener {
            Toast.makeText(this, "📝 Hata Defterine Yeni Kart Eklendi (Lozan Boğazlar - Montrö)", Toast.LENGTH_SHORT).show()
        }

        // Modül 6: Sprint Şablon Toggle
        findViewById<MaterialButton>(R.id.btnSprintSablonToggle).setOnClickListener {
            val a = DersKolaylikAtolye.Modul6_SprintSablonlari.animedoroOzeti(40, 20)
            txtSprintSablonDurum.text = a
            Toast.makeText(this, "⏱️ Şablon Seçildi: Animedoro 40m Odak / 20m Ödül Molası", Toast.LENGTH_SHORT).show()
        }

        // Modül 7: Sokratik İpucu Test
        findViewById<MaterialButton>(R.id.btnSokratikIpucuTest).setOnClickListener {
            val ipucu = DersKolaylikAtolye.Modul7_SokratikIpucu.sokratikIpucuUret("Türev nedir?")
            Toast.makeText(this, ipucu, Toast.LENGTH_LONG).show()
        }

        // Modül 8: Zincir Ekle
        findViewById<MaterialButton>(R.id.btnZincirEkle).setOnClickListener {
            zincirGun++
            ekraniGuncelle()
            Toast.makeText(this, "🐼 Pofi: Harikasın! Aktif Çalışma Zinciri: $zincirGun Gün 🔥🔥", Toast.LENGTH_SHORT).show()
        }

        // Modül 9: 4-7-8 Nefes Rehber
        findViewById<MaterialButton>(R.id.btnNefesRehberTest).setOnClickListener {
            val n = DersKolaylikAtolye.Modul9_NefesVeKahve.nefesRehberMetni()
            Toast.makeText(this, n, Toast.LENGTH_LONG).show()
        }

        // Modül 10: Formül Ders Seç
        findViewById<MaterialButton>(R.id.btnFormulDersSec).setOnClickListener {
            aktifDersFormul = when (aktifDersFormul) {
                "tarih" -> "matematik"
                "matematik" -> "turkce"
                else -> "tarih"
            }
            ekraniGuncelle()
            Toast.makeText(this, "🎒 Formül Seçildi: $aktifDersFormul", Toast.LENGTH_SHORT).show()
        }

        // Modül 10: Deneme CSV Kopyala
        findViewById<MaterialButton>(R.id.btnDenemeCsvKopyala).setOnClickListener {
            val csv = DersKolaylikAtolye.Modul10_FormulVeCsv.denemeCsvUret(listOf(deneme))
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("DenemeCsv", csv))
            Toast.makeText(this, "📋 KPSS Deneme Sonucu CSV Olarak Kopyalandı (Excel'e Yapıştırın!)", Toast.LENGTH_LONG).show()
        }
    }

    private fun ekraniGuncelle() {
        txtSrDurum.text = "KPSS Tarih: Osmanlı Dağılma · Kutu ${srKonu.leitnerKutu} ➔ ${DersKolaylikAtolye.Modul1_AralikliTekrar.sonrakiTekrarGunu(srKonu)}"
        txtDenemeNetDurum.text = DersKolaylikAtolye.Modul2_DenemeNet.denemeOzeti(deneme)
        txtHizliAksiyonDurum.text = DersKolaylikAtolye.Modul3_HizliAksiyon.masayaOturKisaYolMetni("KPSS Tarih: Osmanlı Dağılma")
        txtAntiErtelemeDurum.text = DersKolaylikAtolye.Modul4_AntiErteleme.kurbagaKartMetni(antiKalkan)
        txtHataDefteriDurum.text = DersKolaylikAtolye.Modul5_HataDefteri.hataKartMetni(hataKaydi)
        txtSprintSablonDurum.text = DersKolaylikAtolye.Modul6_SprintSablonlari.animedoroOzeti(40, 20)
        
        val tahmin = DersKolaylikAtolye.Modul7_SokratikIpucu.netTahminEt(listOf(45.0f, 47.0f, 50.0f))
        txtSokratikDurum.text = tahmin

        txtSanalMasaDurum.text = DersKolaylikAtolye.Modul8_SanalMasa.pofiMasaMetni(zincirGun)
        
        val saat = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        txtNefesKahveDurum.text = DersKolaylikAtolye.Modul9_NefesVeKahve.kahveUyariMetni(saat)
        txtFormulDurum.text = DersKolaylikAtolye.Modul10_FormulVeCsv.altinFormulGetir(aktifDersFormul)
    }
}
