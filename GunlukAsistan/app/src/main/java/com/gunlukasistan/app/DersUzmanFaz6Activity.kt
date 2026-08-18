package com.gunlukasistan.app

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/**
 * v10.66 — Ders Çalışma Uzman Faz 6 (14 Oyunlaştırma Rozetleri, Sınav Uyku-Biyoloji Ritmü & Akıllı Taktik Kütüphanesi).
 *
 * Kullanıcının haftalık çalışma rütbesini, zor canavar konuları yenme statüsünü, günlük trivia bilgi sandığını,
 * sınav sabahı olumlamalarını ve branşa özel taktik/önkoşulları interaktif olarak kontrol etmesini sağlar.
 */
class DersUzmanFaz6Activity : AppCompatActivity() {

    private var sandikIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ders_uzman_faz6)

        val btnGeri = findViewById<ImageButton>(R.id.btnGeri)
        btnGeri.setOnClickListener { finish() }

        initOzetAlanlari()
        initButonEtkilesimleri()
    }

    private fun initOzetAlanlari() {
        // Rütbe ve maraton
        val txtRutbe = findViewById<TextView>(R.id.txtRutbeDurum)
        val rutbe = DersUzmanFaz6.Faz6_1_RutbeVePrestij.rutbeHesapla(18)
        val maraton = DersUzmanFaz6.Faz6_1_RutbeVePrestij.maratonMadalyasiKontrol(3)
        txtRutbe.text = "${rutbe.sembol} Rütbe: ${rutbe.unvan} (18 saat/hafta · +${rutbe.xpBonus} XP)\n${maraton.second}"

        // Sandık
        val txtSandik = findViewById<TextView>(R.id.txtSandikOzeti)
        val not = DersUzmanFaz6.Faz6_2_CanavarVeSandik.gununSandikNotunuSec(0)
        txtSandik.text = "📜 [${not.ders} Sandık Notu - #79]: ${not.bilgi}"

        // Strateji
        val txtStrateji = findViewById<TextView>(R.id.txtStratejiOzeti)
        val taktik = DersUzmanFaz6.Faz6_6_StratejiVeOnkosul.bransStratejisiGetir("TURKCE")
        val onkosul = DersUzmanFaz6.Faz6_6_StratejiVeOnkosul.onkosulKontrolu("Türev")
        txtStrateji.text = "$taktik\n${onkosul.second}"
    }

    private fun initButonEtkilesimleri() {
        // Rütbe Sorgula
        val btnRutbe = findViewById<Button>(R.id.btnRutbeTest)
        val txtRutbe = findViewById<TextView>(R.id.txtRutbeDurum)
        btnRutbe.setOnClickListener {
            val saatler = arrayOf("5 Saat / Hafta (Çırak)", "15 Saat / Hafta (Usta)", "30 Saat / Hafta (Altın Efsane)")
            val saatDegerleri = arrayOf(5, 15, 30)
            AlertDialog.Builder(this)
                .setTitle("Haftalık Çalışma Saatini Seçin (#74)")
                .setItems(saatler) { _, idx ->
                    val r = DersUzmanFaz6.Faz6_1_RutbeVePrestij.rutbeHesapla(saatDegerleri[idx])
                    txtRutbe.text = "${r.sembol} Rütbe: ${r.unvan} (${saatDegerleri[idx]} saat/hafta · +${r.xpBonus} XP)"
                    Toast.makeText(this, "Rütbe Güncellendi: ${r.unvan}", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Kapat", null)
                .show()
        }

        // Canavarı Yen
        val btnCanavar = findViewById<Button>(R.id.btnCanavarTest)
        val txtSandik = findViewById<TextView>(R.id.txtSandikOzeti)
        btnCanavar.setOnClickListener {
            val userTopics = try {
                Store.loadTopics(this).map { t ->
                    val sub = t.items.firstOrNull { !it.done }?.text ?: t.items.firstOrNull()?.text
                    if (sub != null) "${t.title} -> $sub" else t.title
                }
            } catch (_: Exception) { emptyList() }
            val konular = if (userTopics.isNotEmpty()) userTopics.toTypedArray() else arrayOf("Genel Çalışma Konusu 1", "Genel Çalışma Konusu 2", "Genel Çalışma Konusu 3")
            AlertDialog.Builder(this)
                .setTitle("Yenilecek 'Canavar Konu'yu Seçin (#77)")
                .setItems(konular) { _, idx ->
                    val zafer = DersUzmanFaz6.Faz6_2_CanavarVeSandik.canavarKonuTamamla(konular[idx])
                    txtSandik.text = "🎉 ${zafer.second}"
                    Toast.makeText(this, "+100 XP Kazanıldı! Konfeti Efekti!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Kapat", null)
                .show()
        }

        // Sandık Aç
        val btnSandik = findViewById<Button>(R.id.btnSandikTest)
        btnSandik.setOnClickListener {
            sandikIndex++
            val not = DersUzmanFaz6.Faz6_2_CanavarVeSandik.gununSandikNotunuSec(sandikIndex)
            txtSandik.text = "📜 [${not.ders} Sandık Notu - #79]: ${not.bilgi}"
            Toast.makeText(this, "Sandık Açıldı: ${not.ders}", Toast.LENGTH_SHORT).show()
        }

        // Sınav Sabahı Olumlamaları
        val btnOlumlama = findViewById<Button>(R.id.btnOlumlamaTest)
        btnOlumlama.setOnClickListener {
            val olumlamalar = DersUzmanFaz6.Faz6_4_OlumlamaVeSabbath.sinavSabahiOlumlamalari()
            AlertDialog.Builder(this)
                .setTitle("Sınav Sabahı Olumlamaları (#87)")
                .setItems(olumlamalar.toTypedArray()) { _, _ ->
                    Toast.makeText(this, "💖 Özgüven Aşılandı!", Toast.LENGTH_SHORT).show()
                }
                .setPositiveButton("Tümünü Onayla") { _, _ ->
                    Toast.makeText(this, "🌟 Sınava 100% Hazırsınız!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Kapat", null)
                .show()
        }
    }

    companion object {
        fun ac(context: android.content.Context) {
            context.startActivity(android.content.Intent(context, DersUzmanFaz6Activity::class.java))
        }
    }
}
