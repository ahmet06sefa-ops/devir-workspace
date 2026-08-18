package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.Executors

/**
 * v10.37 — A'dan Z'ye sistem sağlık kontrolü ekranı.
 *
 * 21 madde arka plan ipliğinde tek tek çalışır; her madde bitiminde
 * satır listeye eklenir, ilerleme çubuğu + yüzde + geçen/kalan süre
 * 200 ms'lik tik ile canlı güncellenir. "Otomatik onar" açıksa
 * güvenli sorunlar (yetim kayıt, bozuk JSON, eski önbellek) anında
 * kural tabanlı onarılır; sonuç raporu paylaşılabilir ve AI anahtarı
 * kuruluysa ek akıllı öneri istenebilir.
 */
class SaglikActivity : AppCompatActivity() {

    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    @Volatile private var yapilan = 0
    @Volatile private var toplam = 0
    @Volatile private var calisiyor = false

    private var baslangicMs = 0L
    private var toplamSureMs = 0L
    private var sonListe: List<SaglikMotoru.Madde> = emptyList()

    private lateinit var adim: TextView
    private lateinit var sayac: TextView
    private lateinit var sure: TextView
    private lateinit var bar: ProgressBar
    private lateinit var onarCb: CheckBox
    private lateinit var baslat: Button
    private lateinit var paylas: Button
    private lateinit var ai: Button
    private lateinit var ozet: TextView
    private lateinit var liste: LinearLayout
    private lateinit var aiMetin: TextView

    private val tikTak = object : Runnable {
        override fun run() {
            if (calisiyor) {
                sureGuncelle()
                handler.postDelayed(this, 200)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saglik)

        adim = findViewById(R.id.skAdim)
        sayac = findViewById(R.id.skSayac)
        sure = findViewById(R.id.skSure)
        bar = findViewById(R.id.skBar)
        onarCb = findViewById(R.id.skOnar)
        baslat = findViewById(R.id.skBaslat)
        paylas = findViewById(R.id.skPaylas)
        ai = findViewById(R.id.skAi)
        ozet = findViewById(R.id.skOzet)
        liste = findViewById(R.id.skListe)
        aiMetin = findViewById(R.id.skAiMetin)

        baslat.setOnClickListener { kontroluBaslat() }
        paylas.setOnClickListener { raporuPaylas() }
        ai.setOnClickListener { aiOnerisiIste() }
    }

    override fun onDestroy() {
        handler.removeCallbacks(tikTak)
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun kontroluBaslat() {
        if (calisiyor) return
        calisiyor = true
        yapilan = 0
        toplam = 0
        baslangicMs = System.currentTimeMillis()
        baslat.isEnabled = false
        onarCb.isEnabled = false
        paylas.visibility = View.GONE
        ai.visibility = View.GONE
        aiMetin.visibility = View.GONE
        ozet.visibility = View.GONE
        liste.removeAllViews()
        bar.progress = 0
        handler.post(tikTak)

        val onar = onarCb.isChecked
        executor.execute {
            val sonuc = SaglikMotoru.calistir(applicationContext, onar) { y, t, madde ->
                handler.post {
                    yapilan = y
                    toplam = t
                    bar.max = t
                    bar.progress = y
                    adim.text = getString(R.string.w37_adim, madde.ad)
                    sayac.text = getString(R.string.w37_sayac, y, t, SaglikMotoru.yuzde(y, t))
                    satirEkle(madde)
                }
            }
            handler.post { kontrolBitti(sonuc) }
        }
    }

    private fun kontrolBitti(sonuc: List<SaglikMotoru.Madde>) {
        calisiyor = false
        sonListe = sonuc
        toplamSureMs = System.currentTimeMillis() - baslangicMs
        baslat.isEnabled = true
        onarCb.isEnabled = true
        baslat.setText(R.string.w37_yeniden)
        adim.text = getString(R.string.w37_bitti_ad)
        sureGuncelle()
        val s = SaglikMotoru.ozetSayilari(sonuc)
        ozet.text = getString(R.string.w37_ozet, s[0], s[1], s[2], s[3])
        ozet.visibility = View.VISIBLE
        paylas.visibility = View.VISIBLE
        ai.visibility = if (runCatching { AiSettings.hasApiKey(this) }.getOrDefault(false)) {
            View.VISIBLE
        } else {
            View.GONE
        }
        sonDurumuKaydet(s)
    }

    private fun sonDurumuKaydet(s: IntArray) {
        runCatching {
            getSharedPreferences("saglik_v1", Context.MODE_PRIVATE).edit()
                .putLong("zaman", System.currentTimeMillis())
                .putInt("iyi", s[0])
                .putInt("uyari", s[1])
                .putInt("hata", s[2])
                .putInt("onarildi", s[3])
                .apply()
        }
    }

    private fun sureGuncelle() {
        val gecen = System.currentTimeMillis() - baslangicMs
        sure.text = if (!calisiyor && toplam > 0) {
            getString(R.string.w37_sure_bitti, mmss(toplamSureMs))
        } else {
            val kalan = SaglikMotoru.tahminiKalanMs(gecen, yapilan, toplam)
            getString(R.string.w37_sure, mmss(gecen), mmss(kalan))
        }
    }

    private fun satirEkle(m: SaglikMotoru.Madde) {
        val satir = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(7), 0, dp(7))
        }
        val ikon = TextView(this).apply {
            text = SaglikMotoru.emoji(m.durum)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.ga_yazi_buyuk))
        }
        val kutu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        }
        val ad = TextView(this).apply {
            text = m.ad
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.ga_yazi_normal))
        }
        val detay = TextView(this).apply {
            text = m.detay
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.ga_yazi_kucuk))
            alpha = 0.75f
            if (m.detay.isBlank()) visibility = View.GONE
        }
        kutu.addView(ad)
        kutu.addView(detay)
        satir.addView(ikon)
        satir.addView(kutu)
        liste.addView(satir)
    }

    private fun raporuPaylas() {
        val metin = SaglikMotoru.raporMetni(this, sonListe, toplamSureMs)
        val i = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, metin)
        }
        runCatching {
            startActivity(Intent.createChooser(i, getString(R.string.w37_paylas_secici)))
        }
    }

    private fun aiOnerisiIste() {
        ai.isEnabled = false
        aiMetin.visibility = View.VISIBLE
        aiMetin.text = getString(R.string.w37_ai_calisiyor)
        val rapor = SaglikMotoru.raporMetni(this, sonListe, toplamSureMs)
        executor.execute {
            val prompt = getString(R.string.w37_ai_prompt) + "\n\n" + rapor
            val sonuc = runCatching { AiClient.sadeIstek(applicationContext, prompt, 1200) }.getOrNull()
            handler.post {
                ai.isEnabled = true
                aiMetin.text = when {
                    sonuc == null ->
                        getString(R.string.w37_ai_hata, getString(R.string.w37_ai_hata_baglanti))
                    sonuc.ok -> sonuc.text
                    else -> getString(R.string.w37_ai_hata, sonuc.text)
                }
            }
        }
    }

    private fun mmss(ms: Long): String {
        val s = (ms / 1000).coerceAtLeast(0)
        return "%d:%02d".format(s / 60, s % 60)
    }

    private fun dp(d: Int): Int = (resources.displayMetrics.density * d).toInt()
}
