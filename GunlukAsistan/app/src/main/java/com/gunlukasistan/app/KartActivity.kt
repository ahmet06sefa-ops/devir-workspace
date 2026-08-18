package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator

/**
 * v7.33 — Bilgi kartı çalışma ekranı.
 *
 * Akış: kart ön yüzü → dokun → arka yüz → "Biliyorum" / "Tekrar göster"
 *
 * "Biliyorum" kartı bir üst Leitner kutusuna taşır (aralık uzar),
 * "Tekrar göster" kutuyu sıfırlar **ve kartı oturumun sonuna ekler** —
 * bilmediğin kart aynı oturumda tekrar karşına çıkar.
 */
class KartActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DESTE = "kart_deste"

        fun ac(context: Context, deste: String = "") {
            context.startActivity(
                Intent(context, KartActivity::class.java).putExtra(EXTRA_DESTE, deste)
            )
        }
    }

    private val kuyruk = mutableListOf<KartStore.Kart>()
    private var indeks = 0
    private var arkaYuzde = false
    private var bilinen = 0
    private var tekrarlanan = 0
    private var baslangicAdet = 0

    private lateinit var kart: MaterialCardView
    private lateinit var metin: TextView
    private lateinit var yuzEtiket: TextView
    private lateinit var ipucu: TextView
    private lateinit var dokunIpucu: TextView
    private lateinit var dugmeler: View
    private lateinit var progress: LinearProgressIndicator

    /**
     * v8.6 · Öneri 27 — Kullanıcının yazı boyutu tercihini uygular.
     *
     * `Configuration.fontScale` tüm `sp` birimlerini bir kerede
     * ölçekliyor; 71 layout'a tek tek dokunmaya gerek kalmıyor.
     */
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(GorunumAyar.yaziOlcegiUygula(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.styleFor(this))
        ThemeManager.applyAccent(this)
        // v8.3 · Öneri 10: Material You (açıksa duvar kâğıdı paleti)
        ThemeManager.dinamikRengiUygula(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kart)

        val deste = intent?.getStringExtra(EXTRA_DESTE).orEmpty()

        kart = findViewById(R.id.kzCard)
        metin = findViewById(R.id.kzText)
        yuzEtiket = findViewById(R.id.kzSide)
        ipucu = findViewById(R.id.kzHint)
        dokunIpucu = findViewById(R.id.kzTapHint)
        dugmeler = findViewById(R.id.kzButtons)
        progress = findViewById(R.id.kzProgress)

        findViewById<TextView>(R.id.kzDeck).text =
            deste.ifBlank { getString(R.string.card_all_decks) }
        findViewById<View>(R.id.kzClose).setOnClickListener { finish() }

        kuyruk.addAll(KartStore.bugunkuKartlar(this, deste, 20))
        baslangicAdet = kuyruk.size

        if (kuyruk.isEmpty()) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.card_title)
                .setMessage(R.string.card_none_today)
                .setPositiveButton(R.string.done) { _, _ -> finish() }
                .setCancelable(false)
                .show()
            return
        }

        progress.max = baslangicAdet
        kart.setOnClickListener { cevir() }
        findViewById<MaterialButton>(R.id.kzKnow).setOnClickListener { cevapla(true) }
        findViewById<MaterialButton>(R.id.kzAgain).setOnClickListener { cevapla(false) }

        goster()
    }

    private fun goster() {
        if (indeks >= kuyruk.size) {
            bitir()
            return
        }
        val k = kuyruk[indeks]
        arkaYuzde = false

        yuzEtiket.setText(R.string.card_front)
        metin.text = k.on
        metin.textSize = 20f
        ipucu.visibility = View.GONE
        dokunIpucu.visibility = View.VISIBLE
        dugmeler.visibility = View.INVISIBLE
        kart.strokeColor = 0x33888888

        findViewById<TextView>(R.id.kzCounter).text =
            getString(R.string.card_counter, indeks + 1, kuyruk.size)
        progress.max = kuyruk.size
        progress.setProgressCompat(indeks, true)
    }

    /** Kartı çevirir — arka yüzü gösterir. */
    private fun cevir() {
        if (arkaYuzde) return
        if (indeks >= kuyruk.size) return
        arkaYuzde = true
        val k = kuyruk[indeks]

        yuzEtiket.setText(R.string.card_back)
        metin.text = k.arka
        // Uzun cevaplarda yazıyı küçült
        metin.textSize = if (k.arka.length > 90) 16f else 19f
        if (k.ipucu.isNotBlank()) {
            ipucu.visibility = View.VISIBLE
            ipucu.text = k.ipucu
        }
        dokunIpucu.visibility = View.GONE
        dugmeler.visibility = View.VISIBLE
        kart.strokeColor = GrafikDili.BASARI
    }

    private fun cevapla(biliyorum: Boolean) {
        if (indeks >= kuyruk.size) return
        val k = kuyruk[indeks]
        KartStore.cevapla(this, k.id, biliyorum)

        if (biliyorum) {
            bilinen++
        } else {
            tekrarlanan++
            // Bilmediği kartı oturumun sonuna ekle — aynı seansta tekrar görsün
            if (kuyruk.count { it.id == k.id } < 2) kuyruk.add(k)
        }
        indeks++
        goster()
    }

    private fun bitir() {
        progress.setProgressCompat(kuyruk.size, true)
        val toplam = bilinen + tekrarlanan
        val yuzde = if (toplam == 0) 0 else bilinen * 100 / toplam

        val govde = buildString {
            append(getString(R.string.card_result, bilinen, tekrarlanan))
            append("\n\n")
            append(
                getString(
                    when {
                        yuzde >= 85 -> R.string.card_msg_great
                        yuzde >= 55 -> R.string.card_msg_ok
                        else -> R.string.card_msg_more
                    }
                )
            )
            val kalan = KartStore.bekleyenSayisi(this@KartActivity)
            if (kalan > 0) {
                append("\n\n")
                append(getString(R.string.card_remaining, kalan))
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.card_done)
            .setMessage(govde)
            .setCancelable(false)
            .setPositiveButton(R.string.done) { _, _ -> finish() }
            .setNegativeButton(R.string.card_continue) { _, _ ->
                // Yeni oturum başlat
                val deste = intent?.getStringExtra(EXTRA_DESTE).orEmpty()
                kuyruk.clear()
                kuyruk.addAll(KartStore.bugunkuKartlar(this, deste, 20))
                indeks = 0
                bilinen = 0
                tekrarlanan = 0
                if (kuyruk.isEmpty()) finish() else goster()
            }
            .show()
    }
}
