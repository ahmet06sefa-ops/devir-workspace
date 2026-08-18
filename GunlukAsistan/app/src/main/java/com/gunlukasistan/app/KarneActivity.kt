package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import java.util.Calendar
import java.util.Locale

/**
 * v10.54 — Sesli "Gündem & Vakit Brifingi" + Akıllı "Odak & Verimlilik Karnesi"
 * (Haftalık AI Raporu).
 *
 * Kullanıcının o günkü namaz vakitlerini, bekleyen görev sayısını, odak
 * süresini ve gün serisini sesli okuyan ([SesliBrifing]) akıllı brifing
 * modülünü ve son 7 günün emeğini değerlendiren harf notlu karneyi ([VerimlilikKarnesi])
 * sunar.
 */
class KarneActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    companion object {
        fun ac(context: Context) {
            context.startActivity(Intent(context, KarneActivity::class.java))
        }
    }

    private var tts: TextToSpeech? = null
    private var ttsHazir = false
    private var sonBrifingMetni: String = ""

    private lateinit var brifingBaslik: TextView
    private lateinit var brifingMetin: TextView
    private lateinit var harfNotu: TextView
    private lateinit var odakSaat: TextView
    private lateinit var enVerimliGun: TextView
    private lateinit var kocTavsiyesi: TextView

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(GorunumAyar.yaziOlcegiUygula(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(ThemeManager.styleFor(this))
        ThemeManager.applyAccent(this)
        ThemeManager.dinamikRengiUygula(this)
        setContentView(R.layout.activity_karne)

        brifingBaslik = findViewById(R.id.krBrifingBaslik)
        brifingMetin = findViewById(R.id.krBrifingMetin)
        harfNotu = findViewById(R.id.krHarfNotu)
        odakSaat = findViewById(R.id.krOdakSaat)
        enVerimliGun = findViewById(R.id.krEnVerimliGun)
        kocTavsiyesi = findViewById(R.id.krKocTavsiyesi)

        findViewById<MaterialButton>(R.id.krKapat).setOnClickListener { finish() }

        tts = TextToSpeech(this, this)

        findViewById<MaterialButton>(R.id.krBtnSesliOku).setOnClickListener {
            sesliOku(sonBrifingMetni)
        }

        arayuzuYukle()
    }

    private fun arayuzuYukle() {
        // 1. Sesli Brifing Verileri
        val saat = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val sabahMi = saat < 12
        val vakitAd = try {
            if (NamazVakti.acikMi(this)) {
                val g = NamazVakti.bugun(this)
                val (v, _) = g.sonraki(NamazVakti.simdiDakika())
                v.name
            } else "Öğle"
        } catch (_: Exception) {
            "Öğle"
        }

        val gorevler = Store.loadTasks(this)
        val bekleyen = gorevler.count { !it.done }
        val odakDk = Store.getTodayFocusMinutes(this)
        val seri = Store.streakInfo(this).first

        brifingBaslik.text = SesliBrifing.brifingBasligi(sabahMi)
        sonBrifingMetni = SesliBrifing.brifingMetniUret(
            vakitAd = vakitAd,
            kalanGorevSayisi = bekleyen,
            odakDk = odakDk,
            seri = seri,
            sabahMi = sabahMi
        )
        brifingMetin.text = sonBrifingMetni

        // 2. Haftalık Verimlilik Karnesi Verileri
        val son7 = Store.recentDayStats(this, 7)
        val odakListesi = son7.map { it.third }
        val gorevListesi = son7.map { it.second }

        val ozet = VerimlilikKarnesi.karneAnalizEt(
            haftalikOdakDk = odakListesi,
            haftalikGorevTamam = gorevListesi,
            haftalikKesinti = emptyList()
        )

        harfNotu.text = ozet.haftalikNot
        odakSaat.text = "Haftalık Toplam: ${ozet.toplamOdakSaat} Saat"
        enVerimliGun.text = "En Verimli Gün: ${ozet.enVerimliGunAd} (${ozet.ortalamaOdakDk} dk/gün ort.)"
        kocTavsiyesi.text = ozet.kocTavsiyesi
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val res = tts?.setLanguage(Locale("tr"))
            if (res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED) {
                ttsHazir = true
            }
        }
    }

    private fun sesliOku(metin: String) {
        if (!ttsHazir) {
            Toast.makeText(this, "Ses motoru yükleniyor veya Türkçe ses verisi eksik", Toast.LENGTH_SHORT).show()
            return
        }
        tts?.speak(metin, TextToSpeech.QUEUE_FLUSH, null, "BRIFING_TTS")
        Toast.makeText(this, "🔊 Brifing sesli okunuyor...", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        super.onDestroy()
    }
}
