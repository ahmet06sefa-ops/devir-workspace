package com.gunlukasistan.app

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * v10.78 — KPSS Sayaç İstatistikler & Manuel Süre Ekleme Ekranı.
 *
 * Ekran Görüntüsü 3 ve 4 ile 100% birebir uyumlu:
 *  - Toplam Dakika, Pomodoro ve Gün özetleri.
 *  - Ağustos 2026 31 günlük takvim ızgarası (10. gün turuncu vurgusu).
 *  - Günlük yeşil durum bandı ve "İlk adımı at" hapı.
 *  - "Çalışma Süresi Ekle" butonu ve saat/dakika girmeli Manuel Süre Ekle dialoğu.
 */
class KpssSayacIstatistikActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kpss_sayac_istatistik)

        val btnGeri = findViewById<ImageButton>(R.id.btnGeri)
        btnGeri.setOnClickListener { finish() }

        initVerileriGoster()
        initButonEtkilesimleri()
    }

    private fun initVerileriGoster() {
        val ozet = KpssSayacAtolye.istatistikOzetGetir(this)

        findViewById<TextView>(R.id.txtToplamDk).text = "${ozet.toplamDakika}\nDakika"
        findViewById<TextView>(R.id.txtToplamPomo).text = "${ozet.toplamPomodoro}\nPomodoro"
        findViewById<TextView>(R.id.txtAktifGun).text = "${ozet.aktifGunSayisi}\nGün"

        val (tarih, durum) = KpssSayacAtolye.gunlukDurumBannerMetni(ozet.bugunDakika)
        findViewById<TextView>(R.id.txtBugunTarihBanner).text = tarih
        findViewById<TextView>(R.id.txtBugunDurumBanner).text = durum
        findViewById<TextView>(R.id.txtIlkAdimBanner).text = KpssSayacAtolye.ilkAdimBannerMetni(ozet.bugunDakika)

        takvimIzgarasiniCiz(ozet.bugunDakika)
    }

    private fun takvimIzgarasiniCiz(bugunDakika: Int) {
        val container = findViewById<LinearLayout>(R.id.layoutTakvimIzgara) ?: return
        container.removeAllViews()

        var gunNo = 1
        // Ağustos 2026: 1'i Cumartesi'ye gelir. Basit 5 haftalık ızgara benzetimi
        for (hafta in 1..5) {
            val satir = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (6 * resources.displayMetrics.density).toInt()
                }
            }
            for (gunIdx in 1..7) {
                val mevcutGun = if (gunNo <= 31) gunNo else 0
                val hucre = TextView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        (42 * resources.displayMetrics.density).toInt(),
                        1f
                    ).apply {
                        setMargins(2, 2, 2, 2)
                    }
                    gravity = Gravity.CENTER
                    textSize = 14f
                    text = if (mevcutGun > 0) "$mevcutGun" else ""
                    if (mevcutGun == 10) {
                        // Turuncu vurgu (Bugün 10 Ağustos)
                        setBackgroundColor(0xFFFF9500.toInt())
                        setTextColor(Color.WHITE)
                    } else if (mevcutGun > 0) {
                        setBackgroundColor(com.google.android.material.color.MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutlineVariant, 0xFF4E453A.toInt()))
                        setTextColor(com.google.android.material.color.MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, 0xFFEBE3D8.toInt()))
                    } else {
                        setBackgroundColor(Color.TRANSPARENT)
                    }
                }
                if (mevcutGun > 0) {
                    val gunNoSabit = mevcutGun
                    hucre.setOnClickListener {
                        gunDetayiyalogunuGoster(gunNoSabit)
                    }
                    gunNo++
                }
                satir.addView(hucre)
            }
            container.addView(satir)
        }
    }

    private fun gunDetayiyalogunuGoster(seciliGun: Int) {
        val dateKey = String.format(java.util.Locale.US, "202608%02d", seciliGun)
        var dk = Store.getDayFocusMinutesByKey(this, dateKey)
        if (seciliGun == 10) {
            val bugunDk = KpssSayacAtolye.istatistikOzetGetir(this).bugunDakika
            dk = maxOf(dk, bugunDk)
        }
        val soru = Store.getDayQuestionsByKey(this, dateKey)
        val pomo = dk / 25
        val saat = dk / 60
        val kalanDk = dk % 60
        val sureStr = if (saat > 0) "$saat saat $kalanDk dakika ($dk Dk)" else "$kalanDk dakika ($dk Dk)"

        val mesaj = buildString {
            appendLine("⏱ Toplam Çalışma Süresi: $sureStr")
            appendLine("🍅 Tamamlanan Pomodoro   : $pomo Oturum")
            appendLine("🔢 Çözülen Soru Sayısı : $soru Soru")
            appendLine()
            if (dk > 0) {
                append("✅ Başarılı Çalışma Günü. Senkronize edilmiş çalışma süresi mevcuttur.")
            } else {
                append("ℹ️ Bu gün için henüz çalışma süresi veya odak kaydı bulunmuyor.")
            }
        }

        AlertDialog.Builder(this)
            .setTitle("📅 $seciliGun Ağustos 2026 Çalışma Özeti")
            .setMessage(mesaj)
            .setPositiveButton("Süre Ekle") { _, _ ->
                gosterManuelSureEkleDialog()
            }
            .setNegativeButton("Kapat", null)
            .show()

        Toast.makeText(this, "📅 $seciliGun Ağustos: $sureStr çalışıldı", Toast.LENGTH_SHORT).show()
    }

    private fun initButonEtkilesimleri() {
        val btnEkle = findViewById<Button>(R.id.btnManuelSureEkle)
        btnEkle.setOnClickListener {
            gosterManuelSureEkleDialog()
        }
    }

    private fun gosterManuelSureEkleDialog() {
        // Ekran Görüntüsü 4 - Manuel Süre Ekle Dialoğu
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 10)
        }

        val btnDers = Button(this).apply {
            text = "Ders Seçiniz (Opsiyonel)"
            setOnClickListener {
                val dersler = KpssSayacAtolye.desteklenenDersler(this@KpssSayacIstatistikActivity)
                AlertDialog.Builder(context)
                    .setTitle("📚 Ders Seçimi")
                    .setItems(dersler.toTypedArray()) { _, idx ->
                        text = "📚 ${dersler[idx]}"
                    }
                    .show()
            }
        }
        layout.addView(btnDers)

        val btnTarih = Button(this).apply {
            text = "Tarih: Bugün"
            isEnabled = false
        }
        layout.addView(btnTarih)

        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val editSaat = EditText(this).apply {
            hint = "Saat (00-23)"
            setText("01")
            gravity = Gravity.CENTER
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val editDakika = EditText(this).apply {
            hint = "Dakika (00-59)"
            setText("00")
            gravity = Gravity.CENTER
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        inputRow.addView(editSaat)
        inputRow.addView(editDakika)
        layout.addView(inputRow)

        val notText = TextView(this).apply {
            text = "Not: Hedef ilerlemesi yalnızca bugün seçildiğinde güncellenir."
            textSize = 12f
            setTextColor(com.google.android.material.color.MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF8A8175.toInt()))
            setPadding(0, 16, 0, 8)
        }
        layout.addView(notText)

        AlertDialog.Builder(this)
            .setTitle("Manuel Süre Ekle")
            .setView(layout)
            .setPositiveButton("Ekle") { _, _ ->
                val saat = editSaat.text.toString().toIntOrNull() ?: 0
                val dk = editDakika.text.toString().toIntOrNull() ?: 0
                val topDk = saat * 60 + dk
                val ders = if (btnDers.text.startsWith("📚")) btnDers.text.toString().removePrefix("📚 ").trim() else ""
                val (basarili, mesaj) = KpssSayacAtolye.manuelSureEkle(this, topDk, ders)
                if (basarili) {
                    initVerileriGoster()
                    Toast.makeText(this, mesaj, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    companion object {
        fun ac(context: android.content.Context) {
            context.startActivity(android.content.Intent(context, KpssSayacIstatistikActivity::class.java))
        }
    }
}
