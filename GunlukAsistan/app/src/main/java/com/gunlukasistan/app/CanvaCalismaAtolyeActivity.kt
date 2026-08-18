package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * v11.11 — Canva Çalışma Ekranı & 10 Uygulama Arayüzü Atölyesi (`CanvaCalismaAtolyeActivity`).
 *
 * Kullanıcının "bana 10 adet farklı uygulamayı canva ekranı gibi çalışma erkanı oluşturmmanu
 * istiyoırum aç kapa özellğiğ ekleyeyim önerTekrwr dene" talimatı doğrultusunda:
 *
 *  1. 10 farklı uygulamayı tek bir yaratıcı Canva tuvali üzerinde görsel kartlar halinde sunar.
 *  2. "Aç / Kapa Özelliği" (`acKapaListesiniKur`): Kullanıcı istediği uygulamanın çipine veya
 *     üzerindeki '✕ Kapa' butonuna dokunarak modülü çalışma alanından kaldırabilir ya da ekleyebilir.
 *  3. "💡 Akıllı Öneri (Öner)" (`btnCanvaOner`): Günün vaktine göre en verimli 3-4 modülü açar.
 *  4. "🔄 Tekrar Dene (Karıştır)" (`btnCanvaTekrarDene`): Alternatif yaratıcı çalışma kombinasyonu dener.
 *  5. A'dan Z'ye sınırsız sürükleme ve basılı tutarak taşıma yetkisi (`EvrenselTasimaVeSuruklemeMotoru`) tanımlıdır.
 */
class CanvaCalismaAtolyeActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            context.startActivity(Intent(context, CanvaCalismaAtolyeActivity::class.java))
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(GorunumAyar.yaziOlcegiUygula(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.styleFor(this))
        ThemeManager.applyAccent(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_canva_atolye)

        findViewById<ImageButton>(R.id.btnGeri).setOnClickListener { finish() }

        findViewById<Button>(R.id.btnCanvaOner).setOnClickListener {
            val (msg, _) = CanvaCalismaMotoru.akilliOneriUygula(this)
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            canvaEkraniTazele()
        }

        findViewById<Button>(R.id.btnCanvaTekrarDene).setOnClickListener {
            val (msg, _) = CanvaCalismaMotoru.tekrarDeneKaristir(this)
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            canvaEkraniTazele()
        }

        findViewById<Button>(R.id.btnCanvaTumunuAc).setOnClickListener {
            CanvaCalismaMotoru.tumunuAc(this)
            canvaEkraniTazele()
            Toast.makeText(this, "⚡ 10 uygulamanın tamamı Canva çalışma ekranına açıldı!", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnCanvaTumunuKapat).setOnClickListener {
            CanvaCalismaMotoru.tumunuKapat(this)
            canvaEkraniTazele()
            Toast.makeText(this, "✕ Tüm uygulamalar Canva çalışma ekranından kapatıldı.", Toast.LENGTH_SHORT).show()
        }

        canvaEkraniTazele()
    }

    override fun onResume() {
        super.onResume()
        GlassmorphismTemaMotoru.sekmeleriVeKartlariStille(window.decorView, this)
    }

    private fun canvaEkraniTazele() {
        acKapaListesiniKur()
        canvaCalismaAlaniniCiz()
    }

    private fun acKapaListesiniKur() {
        val kap = findViewById<LinearLayout>(R.id.layoutCanvaAcKapaListesi) ?: return
        kap.removeAllViews()

        val moduller = CanvaCalismaMotoru.tumModulleriGetir(this)
        val inflater = LayoutInflater.from(this)

        moduller.forEach { m ->
            val cip = inflater.inflate(R.layout.item_canva_ac_kapa, kap, false)
            cip.findViewById<TextView>(R.id.txtCanvaSimge).text = m.simge
            cip.findViewById<TextView>(R.id.txtCanvaAd).text = m.ad.substringBefore(" ")

            val chk = cip.findViewById<CheckBox>(R.id.chkCanvaAcKapa)
            chk.setOnCheckedChangeListener(null)
            chk.isChecked = m.acik

            val degisim: () -> Unit = {
                m.acik = !m.acik
                CanvaCalismaMotoru.modulDurumuDegistir(this, m.kod, m.acik)
                canvaEkraniTazele()
            }

            chk.setOnClickListener { degisim() }
            cip.setOnClickListener { degisim() }

            kap.addView(cip)
        }
    }

    private fun canvaCalismaAlaniniCiz() {
        val kap = findViewById<LinearLayout>(R.id.layoutCanvaCalismaAlani) ?: return
        kap.removeAllViews()

        val moduller = CanvaCalismaMotoru.tumModulleriGetir(this)
        val aciklar = moduller.filter { it.acik }

        if (aciklar.isEmpty()) {
            val bosTv = TextView(this).apply {
                text = "🎨 Şu an Canva çalışma alanındaki tüm uygulamalar kapalı.\n\nYukarıdaki '💡 Akıllı Öneri (Öner)' butonuna basarak günün vaktine en uygun çalışma düzenini hemen kurabilirsiniz!"
                textSize = 14.5f
                gravity = android.view.Gravity.CENTER
                setPadding(32, 48, 32, 48)
                setTextColor(com.google.android.material.color.MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF666666.toInt()))
            }
            kap.addView(bosTv)
            return
        }

        val inflater = LayoutInflater.from(this)

        aciklar.forEach { m ->
            val kart = inflater.inflate(R.layout.item_canva_modul_kart, kap, false)
            kart.tag = m.ad

            kart.findViewById<TextView>(R.id.txtCanvaKartSimge).text = m.simge
            kart.findViewById<TextView>(R.id.txtCanvaKartAd).text = m.ad

            val txtOzet = kart.findViewById<TextView>(R.id.txtCanvaKartOzet)
            val canlıMetin = when (m.kod) {
                "CANVA_POMODORO" -> "⏱️ Çalışma Zamanı ve Pomodoro Sayacı. Odak oturumları, geri sayım ve kronometre arayüzü."
                "CANVA_GOREVLER" -> {
                    val gorevler = Store.aktifGorevler(this)
                    val biten = gorevler.count { it.done }
                    "✅ Bekleyen ${gorevler.size - biten} görev, tamamlanan $biten görev. Alarmsız ve saatsiz esnek görev yönetimi."
                }
                "CANVA_NAMAZ" -> {
                    val gun = NamazVakti.bugunDuzeltilmis(this)
                    val simdi = NamazVakti.simdiDakika()
                    val sonraki = gun.sonraki(simdi)
                    "🕌 ${sonraki.first.emoji} Sıradaki Vakit: ${getString(sonraki.first.adRes)} — ${NamazPlan.sureMetni(sonraki.second)} kaldı. Vaktin Sözü aktif."
                }
                "CANVA_BUGUN" -> {
                    val oneri = runCatching { SimdiNe.oner(this) }.getOrNull()
                    "☀️ Günün Akışı: ${oneri?.baslik ?: "Şimdi odaklanma zamanı"} — ${oneri?.aciklama ?: ""}"
                }
                "CANVA_KURSLAR" -> "🎓 Mühendislik, atölye dersleri ve teknik hesaplama araçları."
                "CANVA_ISTATISTIK" -> "📊 Günlük seriler, ısı haritası ve verimlilik karnesi performansı."
                "CANVA_KISISEL" -> "🌱 12 Aylık retroperspektif puanları, kişisel manifesto ve 5 yıllık vizyon."
                "CANVA_YOUTUBE" -> "📺 YouTube çevrimdışı oynatma listesi ve yapay zekâ video sıralayıcı."
                "CANVA_GORUNUM" -> "🎨 10.000-Madde Evrensel Görünüm ve Arayüz Kişiselleştirme Atölyesi."
                "CANVA_INOVASYON" -> "⚡ 10.000-Madde İnovasyon & Gelişim Atölyesi. Hızlı komut ve otonom sistem."
                else -> m.aciklama
            }
            txtOzet.text = canlıMetin

            kart.findViewById<Button>(R.id.btnCanvaKartKapat).setOnClickListener {
                m.acik = false
                CanvaCalismaMotoru.modulDurumuDegistir(this, m.kod, false)
                canvaEkraniTazele()
                Toast.makeText(this, "✕ '${m.ad}' çalışma ekranından kapatıldı.", Toast.LENGTH_SHORT).show()
            }

            fun anaSayfayiAc(idx: Int = 0) {
                val i = Intent(this, MainActivity::class.java).apply {
                    putExtra("ACILIS_SEKMESI_ID", idx)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(i)
            }

            kart.findViewById<Button>(R.id.btnCanvaKartAc).setOnClickListener {
                when (m.kod) {
                    "CANVA_POMODORO" -> anaSayfayiAc(4)
                    "CANVA_GOREVLER" -> anaSayfayiAc(6)
                    "CANVA_NAMAZ" -> NamazActivity.ac(this)
                    "CANVA_BUGUN" -> anaSayfayiAc(2)
                    "CANVA_KURSLAR" -> anaSayfayiAc(13)
                    "CANVA_ISTATISTIK" -> anaSayfayiAc(1)
                    "CANVA_KISISEL" -> KisiselGelisimActivity.ac(this)
                    "CANVA_YOUTUBE" -> YoutubePlaylistActivity.ac(this)
                    "CANVA_GORUNUM" -> EvrenselGorunumActivity.ac(this)
                    "CANVA_INOVASYON" -> BinMaddeKontrolActivity.ac(this)
                    else -> anaSayfayiAc(0)
                }
            }

            kap.addView(kart)
        }

        // v11.11: A'dan Z'ye sınırsız sürükleme ve basılı tutarak taşıma yetkisini kur!
        EvrenselTasimaVeSuruklemeMotoru.containerIcinSurukleVeTasiKur(
            this,
            "canva",
            kap
        )
    }
}
