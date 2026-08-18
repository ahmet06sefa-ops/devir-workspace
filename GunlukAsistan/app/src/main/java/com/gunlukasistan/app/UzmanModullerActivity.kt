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
 * v10.57 — Faz 2: C, D, E, G, H, I ve J İleri Seviye Uzman Modülleri ([UzmanModuller]) Arayüzü.
 *
 * Kullanıcının 7 kategorinin uzman araçlarını tek ekrandan test edip kontrol ettiği
 * merkezi uzman aktivite.
 */
class UzmanModullerActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            context.startActivity(Intent(context, UzmanModullerActivity::class.java))
        }
    }

    private var yorgunluk = UzmanModuller.YorgunlukEndeksi(ardikPomodoroSayisi = 2, zihinselYorgunlukYuzde = 50)
    private var ayna = UzmanModuller.AynaDurumu()
    private var alarmTani = UzmanModuller.AlarmTani()
    private val nadirRozetler = UzmanModuller.UzmanD_RozetVitrini.nadirlikListesi()
    private val sinavlar = UzmanModuller.UzmanI_PdfVeSinav.sinavListesi()

    private lateinit var txtBiyoVakitDurum: TextView
    private lateinit var txtSeriKurtarmaDurum: TextView
    private lateinit var txtNadirlikDurum: TextView
    private lateinit var txtFadeDurum: TextView
    private lateinit var txtYorgunlukDurum: TextView
    private lateinit var txtAynaDurum: TextView
    private lateinit var txtSinavDurum: TextView
    private lateinit var txtAlarmSaglikDurum: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_uzman_moduller)

        txtBiyoVakitDurum = findViewById(R.id.txtBiyoVakitDurum)
        txtSeriKurtarmaDurum = findViewById(R.id.txtSeriKurtarmaDurum)
        txtNadirlikDurum = findViewById(R.id.txtNadirlikDurum)
        txtFadeDurum = findViewById(R.id.txtFadeDurum)
        txtYorgunlukDurum = findViewById(R.id.txtYorgunlukDurum)
        txtAynaDurum = findViewById(R.id.txtAynaDurum)
        txtSinavDurum = findViewById(R.id.txtSinavDurum)
        txtAlarmSaglikDurum = findViewById(R.id.txtAlarmSaglikDurum)

        ekraniGuncelle()

        // Faz 2 Rehberi Butonu
        findViewById<MaterialButton>(R.id.btnFaz2Rehber).setOnClickListener {
            Toast.makeText(this, "🔬 Faz 2 Uzman Modülleri devrede! Tüm 7 ileri seviye kategori kontrolünüzde.", Toast.LENGTH_LONG).show()
        }

        // Modül C: 23:30 Acil Seri Kurtarma Testi
        findViewById<MaterialButton>(R.id.btnSeriKurtarmaTest).setOnClickListener {
            val gerekliMi = UzmanModuller.UzmanC_BiyoVakit.seriKurtarmaGerekliMi(23, 35, 0)
            val mesaj = UzmanModuller.UzmanC_BiyoVakit.seriKurtarmaMesaji(gerekliMi)
            txtSeriKurtarmaDurum.text = mesaj
            Toast.makeText(this, "🚨 23:30 Simülasyonu: 10 Dakikalık Kurtarma Oturumu Öneriliyor!", Toast.LENGTH_LONG).show()
        }

        // Modül D: Sosyal Başarı Kartını Kopyala
        findViewById<MaterialButton>(R.id.btnSosyalKartKopyala).setOnClickListener {
            val kart = UzmanModuller.UzmanD_RozetVitrini.sosyalPaylasimMetni("👑 Efsane", 150, true)
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("SosyalKart", kart))
            Toast.makeText(this, "📋 ASCII Sosyal Başarı Kartı Panoya Kopyalandı!", Toast.LENGTH_SHORT).show()
        }

        // Modül E: Kulaklık Çıkarıldı Auto-Pause Simülasyonu
        findViewById<MaterialButton>(R.id.btnAutoPauseTest).setOnClickListener {
            val durum = UzmanModuller.UzmanE_FadeVeAutoPause.kulaklikCiktiDurumu(true)
            Toast.makeText(this, durum, Toast.LENGTH_LONG).show()
        }

        // Modül G: +1 Pomodoro & Yorgunluk Radarı
        findViewById<MaterialButton>(R.id.btnPomodoroEkleRadar).setOnClickListener {
            yorgunluk = UzmanModuller.UzmanG_YorgunlukVeCikti.pomodoroEkle(yorgunluk)
            ekraniGuncelle()
            val uyari = UzmanModuller.UzmanG_YorgunlukVeCikti.yorgunlukRadariUyari(yorgunluk)
            if (uyari.contains("YORGUNLUK RADARI")) {
                Toast.makeText(this, uyari, Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "⚡ Pomodoro Eklendi (Yorgunluk: %${yorgunluk.zihinselYorgunlukYuzde})", Toast.LENGTH_SHORT).show()
            }
        }

        // Modül G: Çıktı Hasadı Testi
        findViewById<MaterialButton>(R.id.btnCiktiHasatTest).setOnClickListener {
            val hasat = UzmanModuller.UzmanG_YorgunlukVeCikti.ciktiHasadiMetni("KPSS Tarih", "20 soru çözüldü ve notlar özetlendi")
            Toast.makeText(this, "📝 $hasat", Toast.LENGTH_LONG).show()
        }

        // Modül H: Yüzebilen Canlı Durum Şeridi
        findViewById<MaterialButton>(R.id.btnYuzenSeritTest).setOnClickListener {
            val serit = UzmanModuller.UzmanH_AynaVeYuzenSerit.yuzenSeritMetni(18, "40Hz Gamma", "👑 Efsane")
            Toast.makeText(this, "⚡ YÜZEN ŞERİT: '$serit'", Toast.LENGTH_LONG).show()
        }

        // Modül I: PDF Sayfa Bölücü
        findViewById<MaterialButton>(R.id.btnPdfBolucuTest).setOnClickListener {
            val bolum = UzmanModuller.UzmanI_PdfVeSinav.pdfBolmeHesapla(400, 120, 134)
            Toast.makeText(this, bolum, Toast.LENGTH_LONG).show()
        }

        // Modül J: 'FATURA' Anahtar Kelime Arama
        findViewById<MaterialButton>(R.id.btnKelimeAraTest).setOnClickListener {
            val arama = UzmanModuller.UzmanJ_AramaVeAlarmSagligi.anahtarKelimeAra("fatura")
            Toast.makeText(this, arama, Toast.LENGTH_LONG).show()
        }

        // Modül J: Alarm Sağlık Tanısı
        findViewById<MaterialButton>(R.id.btnAlarmTestiYap).setOnClickListener {
            val rapor = UzmanModuller.UzmanJ_AramaVeAlarmSagligi.alarmSaglikRaporu(alarmTani)
            Toast.makeText(this, "🔔 $rapor", Toast.LENGTH_LONG).show()
        }
    }

    private fun ekraniGuncelle() {
        txtBiyoVakitDurum.text = UzmanModuller.UzmanC_BiyoVakit.biyoVakitTavsiyesi(10)
        txtNadirlikDurum.text = UzmanModuller.UzmanD_RozetVitrini.nadirlikVitriniOzeti(nadirRozetler)
        txtFadeDurum.text = UzmanModuller.UzmanE_FadeVeAutoPause.fadeOzetiGetir(UzmanModuller.FadeAyari())
        txtYorgunlukDurum.text = "⚡ Yorgunluk: %${yorgunluk.zihinselYorgunlukYuzde} · (${yorgunluk.ardikPomodoroSayisi} Pomodoro) · ${UzmanModuller.UzmanG_YorgunlukVeCikti.yorgunlukRadariUyari(yorgunluk)}"
        txtAynaDurum.text = UzmanModuller.UzmanH_AynaVeYuzenSerit.aynaKartMetni(ayna)
        txtSinavDurum.text = UzmanModuller.UzmanI_PdfVeSinav.sinavOzetMetni(sinavlar.first())
        txtAlarmSaglikDurum.text = UzmanModuller.UzmanJ_AramaVeAlarmSagligi.alarmSaglikRaporu(alarmTani)
    }
}
