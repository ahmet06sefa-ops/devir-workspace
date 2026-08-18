package com.gunlukasistan.app

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/**
 * v10.64 — Ders Çalışma Uzman Merkezi — Faz 2..5 (28 İleri Etkileşimli Araç & Otonom Koçluk).
 *
 * Kullanıcıya hem Faz 2..4 özetlerini sunar, hem de Faz 5 (#7..#70) kapsamındaki
 * - AI Koç Kişilik Modları (#70)
 * - Çalışma Masası Öncesi Ritüel Check-List (#38)
 * - Pomodoro İçi Mikro-Tekrar & Hafıza Çengeli (#7, #8)
 * - Haftalık Bilişsel Konsolidasyon Raporu (#10) & ÖSYM Çeldiricileri (#20)
 * - Akıllı PDF TOC Atlayıcı (#44) & Kes-Yapıştır Yanlış Panosu (#49)
 * modüllerini interaktif olarak test etme imkanı tanır.
 */
class DersUzmanMerkezActivity : AppCompatActivity() {

    private var seciliKocModuIndex = 0
    private val kocModlari = listOf("SEFKATLI", "SERT", "SOKRATIK")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ders_uzman_merkez)

        val btnGeri = findViewById<ImageButton>(R.id.btnGeri)
        btnGeri.setOnClickListener { finish() }

        initBiliselVeTocOzeti()
        initButonEtkilesimleri()
    }

    private fun initBiliselVeTocOzeti() {
        // Konsolidasyon skoru ve çeldirici özeti
        val txtKonsolidasyon = findViewById<TextView>(R.id.txtKonsolidasyonOzeti)
        val kayitlar = DersUzmanFaz5.Faz5_2_BiliselKonsolidasyon.varsayilanHaftalikKayitlar()
        val skor = DersUzmanFaz5.Faz5_2_BiliselKonsolidasyon.konsolidasyonSkoruHesapla(kayitlar)
        val celdiriciler = DersUzmanFaz5.Faz5_3_CeldiriciVeRituel.varsayilanCeldiriciler()
        txtKonsolidasyon.text = "Konsolidasyon Skoru: %${skor.first} — ${skor.second}\nÖSYM Çeldirici Arşivi: ${celdiriciler.size} kritik şık uyarısı listelendi."

        // TOC ve Kes-Yapıştır Yanlış Panosu özeti
        val txtTocYanlis = findViewById<TextView>(R.id.txtTocVeYanlisOzeti)
        val tocList = DersUzmanFaz5.Faz5_5_PdfTocVeYanlisPano.varsayilanTocListesi()
        val yanlislar = DersUzmanFaz5.Faz5_5_PdfTocVeYanlisPano.varsayilanYanlisPanosu()
        val cozulmeyen = yanlislar.count { !it.cozulduMu }
        txtTocYanlis.text = "PDF TOC: ${tocList.size} ana bölüm indekslendi (s.12 - s.210).\nYanlış Panosu: Toplam ${yanlislar.size} soru, ${cozulmeyen} tanesi tekrar bekliyor."
    }

    private fun initButonEtkilesimleri() {
        val btnKocDemo = findViewById<Button>(R.id.btnKocDemo)
        val txtKocYaniti = findViewById<TextView>(R.id.txtKocYaniti)
        btnKocDemo.setOnClickListener {
            val mod = kocModlari[seciliKocModuIndex]
            seciliKocModuIndex = (seciliKocModuIndex + 1) % kocModlari.size
            val mesaj = DersUzmanFaz5.Faz5_7_AiKocVeOtomatikQuiz.aiKocYanitiAl(
                mod, "Bugün derse başlamak istemiyorum"
            )
            txtKocYaniti.text = mesaj
            Toast.makeText(this, "Mod Değişti: $mod", Toast.LENGTH_SHORT).show()
        }

        val btnRituel = findViewById<Button>(R.id.btnRituelBasla)
        val txtRituelDurum = findViewById<TextView>(R.id.txtRituelDurum)
        btnRituel.setOnClickListener {
            val adimlar = DersUzmanFaz5.Faz5_3_CeldiriciVeRituel.rituelAdimlar()
            AlertDialog.Builder(this)
                .setTitle("Çalışma Masası Öncesi Ritüeli")
                .setItems(adimlar.toTypedArray()) { _, index ->
                    val tamamlandimi = DersUzmanFaz5.Faz5_3_CeldiriciVeRituel.rituelDurumuSorgula(
                        adimlar.size, adimlar.size
                    )
                    txtRituelDurum.text = "✅ ${adimlar[index]}\n${tamamlandimi.second}"
                    Toast.makeText(this, "Adım onaylandı: ${adimlar[index]}", Toast.LENGTH_SHORT).show()
                }
                .setPositiveButton("Tümünü Onayla") { _, _ ->
                    val tamamlandimi = DersUzmanFaz5.Faz5_3_CeldiriciVeRituel.rituelDurumuSorgula(
                        adimlar.size, adimlar.size
                    )
                    txtRituelDurum.text = tamamlandimi.second
                    Toast.makeText(this, "🌟 Ritüel tamamlandı! Başarılar!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Kapat", null)
                .show()
        }

        val btnCengel = findViewById<Button>(R.id.btnCengelSoru)
        val txtCengelDurum = findViewById<TextView>(R.id.txtCengelDurum)
        btnCengel.setOnClickListener {
            val sorular = DersUzmanFaz5.Faz5_1_PomodoroCengel.varsayilanCengelSorulari()
            val secilen = sorular.first()
            AlertDialog.Builder(this)
                .setTitle("Hafıza Çengeli (#8): Seans Kilit Sorusu")
                .setMessage("${secilen.soruMetni}\n\nİpucu: ${secilen.ipucu}")
                .setPositiveButton("Örnek Yanıtı Onayla") { _, _ ->
                    val sonuc = DersUzmanFaz5.Faz5_1_PomodoroCengel.cengelKontroluTamamla(
                        secilen.id, "Özgürlük ve bağımsızlık ana ideolojidir."
                    )
                    txtCengelDurum.text = "🎉 ${sonuc.second}"
                    Toast.makeText(this, "+15 XP Kazanıldı!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("İptal", null)
                .show()
        }
    }

    companion object {
        fun ac(context: android.content.Context) {
            context.startActivity(android.content.Intent(context, DersUzmanMerkezActivity::class.java))
        }
    }
}
