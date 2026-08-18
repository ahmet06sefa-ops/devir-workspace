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

/**
 * v10.60 — 7 Uzman Bilişsel Öğrenme & Sınav Net Alt-Modülü ([DersIleriFaz]) Arayüzü.
 *
 * Kullanıcının "Kategori 1: Bilişsel Öğrenme / Leitner Kutusu İleri Fazı veya Kategori 5:
 * PDF Sayfa Üzeri Flaş Kart Üretimi özel alt sayfalarına... Devam edebilirsin" talebini
 * tek ekrandan interaktif olarak yönettiği aktivite.
 */
class DersIleriFazActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            context.startActivity(Intent(context, DersIleriFazActivity::class.java))
        }
    }

    private var leitnerKartlar = DersIleriFaz.Ileri1_LeitnerMotoru.varsayilanDeste()
    private var aktifKartIndex = 0
    private var denemeler = listOf(
        DersIleriFaz.DenemeKaydi("KPSS Lisans - Deneme 1", 45, 12, 60),
        DersIleriFaz.DenemeKaydi("KPSS Lisans - Deneme 2", 50, 10, 60)
    )
    private var aktifSeansId = 1
    private var aktifCeldiriciDers = "tarih"

    private lateinit var txtLeitnerKartSoru: TextView
    private lateinit var txtLeitnerKartCevap: TextView
    private lateinit var txtLeitnerDagilim: TextView
    private lateinit var txtPdfVurguDurum: TextView
    private lateinit var txtDenemeEgriDurum: TextView
    private lateinit var txtActiveRecallDurum: TextView
    private lateinit var txtBiyoSprintDurum: TextView
    private lateinit var txtCeldiriciDurum: TextView
    private lateinit var txtIleriCsvDurum: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ders_ileri_faz)

        txtLeitnerKartSoru = findViewById(R.id.txtLeitnerKartSoru)
        txtLeitnerKartCevap = findViewById(R.id.txtLeitnerKartCevap)
        txtLeitnerDagilim = findViewById(R.id.txtLeitnerDagilim)
        txtPdfVurguDurum = findViewById(R.id.txtPdfVurguDurum)
        txtDenemeEgriDurum = findViewById(R.id.txtDenemeEgriDurum)
        txtActiveRecallDurum = findViewById(R.id.txtActiveRecallDurum)
        txtBiyoSprintDurum = findViewById(R.id.txtBiyoSprintDurum)
        txtCeldiriciDurum = findViewById(R.id.txtCeldiriciDurum)
        txtIleriCsvDurum = findViewById(R.id.txtIleriCsvDurum)

        ekraniGuncelle()

        // 100 Öneri Katalog Butonu
        findViewById<MaterialButton>(R.id.btnDersKatalogRehber).setOnClickListener {
            Toast.makeText(this, "📚 100 Uzman Öğrenme & Kolaylık Katalogu 100-DERS-VE-KOLAYLIK-ONERISI.md içinde yayında!", Toast.LENGTH_LONG).show()
        }

        // Modül 1: Kart Doğru Bildim
        findViewById<MaterialButton>(R.id.btnKartDogru).setOnClickListener {
            val mevcut = leitnerKartlar[aktifKartIndex]
            val yeni = DersIleriFaz.Ileri1_LeitnerMotoru.kartDogruBildim(mevcut)
            leitnerKartlar = leitnerKartlar.map { if (it.id == mevcut.id) yeni else it }
            aktifKartIndex = (aktifKartIndex + 1) % leitnerKartlar.size
            ekraniGuncelle()
            Toast.makeText(this, "✅ DOĞRU! Kart Kutu ${yeni.kutuNo}'ya Yükseldi!", Toast.LENGTH_SHORT).show()
        }

        // Modül 1: Kart Yanlış Bildim
        findViewById<MaterialButton>(R.id.btnKartYanlis).setOnClickListener {
            val mevcut = leitnerKartlar[aktifKartIndex]
            val yeni = DersIleriFaz.Ileri1_LeitnerMotoru.kartYanlisBildim(mevcut)
            leitnerKartlar = leitnerKartlar.map { if (it.id == mevcut.id) yeni else it }
            aktifKartIndex = (aktifKartIndex + 1) % leitnerKartlar.size
            ekraniGuncelle()
            Toast.makeText(this, "❌ YANLIŞ! Kart Kutu 1'e İndirildi", Toast.LENGTH_SHORT).show()
        }

        // Modül 2: PDF Vurgudan Flaş Kart Üret
        findViewById<MaterialButton>(R.id.btnPdfFlasKartUret).setOnClickListener {
            val vurgu = "Lozan Boğazlar - 1936 Montrö sözleşmesi ile tam egemenlik sağlandı"
            val yeniKart = DersIleriFaz.Ileri2_PdfFlasKart.pdfVurgudanFlasKartUret(vurgu)
            leitnerKartlar = leitnerKartlar + yeniKart
            ekraniGuncelle()
            Toast.makeText(this, "✂️ Yeni Kart Desteye Eklendi: '${yeniKart.soru}'", Toast.LENGTH_LONG).show()
        }

        // Modül 3: Yeni Deneme Kaydı Ekle
        findViewById<MaterialButton>(R.id.btnDenemeKayitEkle).setOnClickListener {
            val yeni = DersIleriFaz.DenemeKaydi("KPSS Lisans - Deneme 3", 52, 8, 60)
            denemeler = DersIleriFaz.Ileri3_DenemeEgrisi.denemeKayitEkle(denemeler, yeni)
            ekraniGuncelle()
            Toast.makeText(this, "📈 Yeni Deneme Eklendi (50.00 NET! YÜKSELİŞTE)", Toast.LENGTH_SHORT).show()
        }

        // Modül 4: Active Recall Skor Test
        findViewById<MaterialButton>(R.id.btnActiveRecallTest).setOnClickListener {
            val ozet = "Lozan antlaşması boğazlar sözleşmesidir. Montrö ile komisyon kalkmıştır. Türkiye tam egemen olmuştur."
            val skor = DersIleriFaz.Ileri4_ActiveRecall.activeRecallSkoru(ozet)
            Toast.makeText(this, "📝 Active Recall Skoru: %$skor (İdeal 3+ Cümle ve Net Kelimeler)", Toast.LENGTH_LONG).show()
        }

        // Modül 5: Biyo Sprint Toggle
        findViewById<MaterialButton>(R.id.btnBiyoSprintToggle).setOnClickListener {
            aktifSeansId = if (aktifSeansId == 1) 2 else 1
            ekraniGuncelle()
            Toast.makeText(this, "⏱️ Seans Seçildi: ${DersIleriFaz.Ileri5_BiyoSprint.seansSecimi(aktifSeansId).ad}", Toast.LENGTH_SHORT).show()
        }

        // Modül 6: Çeldirici Ders Değiştir
        findViewById<MaterialButton>(R.id.btnCeldiriciDersSec).setOnClickListener {
            aktifCeldiriciDers = when (aktifCeldiriciDers) {
                "tarih" -> "turkce"
                "turkce" -> "matematik"
                else -> "tarih"
            }
            ekraniGuncelle()
            Toast.makeText(this, "⚠️ Çeldirici Rehberi: $aktifCeldiriciDers", Toast.LENGTH_SHORT).show()
        }

        // Modül 7: Leitner CSV Kopyala
        findViewById<MaterialButton>(R.id.btnLeitnerCsvKopyala).setOnClickListener {
            val csv = DersIleriFaz.Ileri7_FormulVeCsv.kartlariCsvUret(leitnerKartlar)
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("LeitnerCsv", csv))
            Toast.makeText(this, "📋 Leitner Destesi CSV Olarak Kopyalandı (Excel'e Yapıştırın!)", Toast.LENGTH_LONG).show()
        }

        // Modül 7: Net Eğri CSV Kopyala
        findViewById<MaterialButton>(R.id.btnDenemeEgriCsvKopyala).setOnClickListener {
            val csv = DersIleriFaz.Ileri7_FormulVeCsv.denemeleriCsvUret(denemeler)
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("DenemeEgriCsv", csv))
            Toast.makeText(this, "📋 KPSS Deneme Net Eğrisi CSV Olarak Kopyalandı!", Toast.LENGTH_LONG).show()
        }
    }

    private fun ekraniGuncelle() {
        if (leitnerKartlar.isNotEmpty()) {
            val k = leitnerKartlar[aktifKartIndex]
            txtLeitnerKartSoru.text = "❓ Soru: ${k.soru} (Kutu: ${k.kutuNo})"
            txtLeitnerKartCevap.text = "💡 Cevap: ${k.cevap}"
        }
        txtLeitnerDagilim.text = DersIleriFaz.Ileri1_LeitnerMotoru.kutuDagilimOzeti(leitnerKartlar)
        txtDenemeEgriDurum.text = DersIleriFaz.Ileri3_DenemeEgrisi.egriAnalizi(denemeler)
        txtBiyoSprintDurum.text = "⏱️ ${DersIleriFaz.Ileri5_BiyoSprint.seansSecimi(aktifSeansId).ad}: ${DersIleriFaz.Ileri5_BiyoSprint.seansSecimi(aktifSeansId).aciklama}"
        txtCeldiriciDurum.text = DersIleriFaz.Ileri6_CeldiriciVeSokratik.celdiriciUyarisi(aktifCeldiriciDers)
        txtIleriCsvDurum.text = "📦 Leitner Destesi: ${leitnerKartlar.size} Kart hazır · Deneme Eğrisi: ${denemeler.size} Sınav hazır"
    }
}
