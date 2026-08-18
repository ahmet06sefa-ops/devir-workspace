package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import java.util.Calendar

/**
 * v10.48 — Kullanıcı maddesi #10: Otonom AI Ajanı ve Otopilot Merkezi.
 *
 * Kullanıcı isteği:
 * "Farklı öner, uygulamada olmayan yapay zeka ile ilgili olsun, uygulamayı yönetsin. (Hepsini)"
 *
 * 5 Otonom Ajan modülünü tek ekranda buluşturur:
 * 1. Otopilot anahtarı (`Store.getOtopilotAcik`)
 * 2. Eylem yetkili AI Ajanı (`OtonomMotor.ajanKomutuAyristir`)
 * 3. Akıllı Gündem Orkestratörü (`OtonomMotor.gundemOrkestrasyonu`)
 * 4. Akıllı Alışkanlık & Seri Bekçisi (`OtonomMotor.seriKurtarmaAnalizi`)
 * 5. Otonom Kütüphaneci (`OtonomMotor.notlardanGorevCikar`)
 */
class OtonomMerkezActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            context.startActivity(Intent(context, OtonomMerkezActivity::class.java))
        }
    }

    private lateinit var switchOtopilot: MaterialSwitch
    private lateinit var modAciklama: TextView
    private lateinit var ajanInput: EditText
    private lateinit var ajanSonuc: TextView
    private lateinit var gundemDurum: TextView
    private lateinit var bekciDurum: TextView
    private lateinit var kutuphaneDurum: TextView

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(GorunumAyar.yaziOlcegiUygula(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(ThemeManager.styleFor(this))
        ThemeManager.applyAccent(this)
        ThemeManager.dinamikRengiUygula(this)
        setContentView(R.layout.activity_otonom_merkez)

        switchOtopilot = findViewById(R.id.otSwitch)
        modAciklama = findViewById(R.id.otModAciklama)
        ajanInput = findViewById(R.id.otAjanInput)
        ajanSonuc = findViewById(R.id.otAjanSonuc)
        gundemDurum = findViewById(R.id.otGundemDurum)
        bekciDurum = findViewById(R.id.otBekciDurum)
        kutuphaneDurum = findViewById(R.id.otKutuphaneDurum)

        findViewById<MaterialButton>(R.id.otKapat).setOnClickListener { finish() }

        // 1. Otopilot Anahtarı
        switchOtopilot.isChecked = Store.getOtopilotAcik(this)
        otopilotArayuzTazele(switchOtopilot.isChecked)
        switchOtopilot.setOnCheckedChangeListener { _, acik ->
            Store.setOtopilotAcik(this, acik)
            otopilotArayuzTazele(acik)
            Toast.makeText(
                this,
                if (acik) "🤖 AI Otopilot Açıldı" else "Otopilot Kapatıldı",
                Toast.LENGTH_SHORT
            ).show()
        }

        // 2. Eylem Yetkili AI Ajanı
        findViewById<MaterialButton>(R.id.otBtnAjanCalistir).setOnClickListener { ajaniCalistir() }

        // 3. Akıllı Gündem Orkestratörü
        findViewById<MaterialButton>(R.id.otBtnGundemOrkestre).setOnClickListener { gundemiOrkestreEt() }

        // 4. Akıllı Alışkanlık & Seri Bekçisi
        findViewById<MaterialButton>(R.id.otBtnSeriKurtar).setOnClickListener { seriBekcisiniCalistir() }

        // 5. Otonom Kütüphaneci
        findViewById<MaterialButton>(R.id.otBtnNotTara).setOnClickListener { kutuphaneciyiCalistir() }

        durumOzetleriniTazele()
    }

    private fun otopilotArayuzTazele(acik: Boolean) {
        modAciklama.text = if (acik) {
            getString(R.string.ot_mod_alt_acik)
        } else {
            getString(R.string.ot_mod_alt_kapali)
        }
    }

    private fun durumOzetleriniTazele() {
        val simdi = System.currentTimeMillis()
        val gunKey = UykuCerceve.gunKey(simdi)
        val kayit = UykuCerceve.gunBul(this, gunKey)
        val uykuMs = kayit?.uykuMs ?: 0L
        val azUyku = uykuMs in 1 until 6 * 3600_000L

        gundemDurum.text = if (azUyku) {
            "Uyku süreniz 6 saatin altında. Zihinsel ağır işlerin öğleden sonraki odak penceresine alınması öneriliyor."
        } else {
            "Uyku süreniz normal. Gündem orkestratörü sabahın ilk saatlerinde en öncelikli işlere odaklandırır."
        }

        bekciDurum.text = "Alışkanlık serileri akşam saat 18:00 ile 23:59 arasında izlenir, kırılma riskinde 10 dk kurtarma sayacı açılır."

        val notlar = Store.loadNotes(this)
        kutuphaneDurum.text = "Depoda ${notlar.size} not incelenmeyi bekliyor. Kütüphaneci eylem maddelerini ve hata tekrarlarını görevlere dönüştürür."
    }

    private fun ajaniCalistir() {
        val komut = ajanInput.text.toString().trim()
        if (komut.isBlank()) {
            Toast.makeText(this, "Lütfen ajana bir komut yazın", Toast.LENGTH_SHORT).show()
            return
        }

        val eylemler = OtonomMotor.ajanKomutuAyristir(komut)
        if (eylemler.isEmpty()) {
            ajanSonuc.text = "Komuttan eylem çıkarılamadı. Örnek: 'Sabah uyanma saatimi 07:30 yap, 25 dk sayaç kur ve görev ekle'"
            return
        }

        val log = mutableListOf<String>()
        eylemler.forEach { e ->
            when (e.tur) {
                OtonomMotor.EylemTuru.UYKU_SAATI_GUNCELLE -> {
                    val ms = ManuelKontrol.uyanmaZamaniHesapla(System.currentTimeMillis(), e.saat, e.dakika)
                    UykuCerceve.uyandiKaydet(this, ms)
                    UykuCerceve.sabahVerildi(this, ms)
                    log.add("✅ " + e.ozet)
                }
                OtonomMotor.EylemTuru.SAYAC_KUR -> {
                    TimerEngine.setTotalMs(this, e.sayacDk * 60_000L)
                    log.add("✅ " + e.ozet)
                }
                OtonomMotor.EylemTuru.GOREV_EKLE -> {
                    Store.addTask(this, e.metinParam)
                    log.add("✅ " + e.ozet)
                }
                OtonomMotor.EylemTuru.HEDEF_GUNCELLE -> {
                    val fark = e.sayacDk - Store.getTodayFocusMinutes(this)
                    if (fark > 0) Store.addTodayFocusMinutes(this, fark)
                    log.add("✅ " + e.ozet)
                }
            }
        }

        WidgetCommon.refreshAll(this)
        durumOzetleriniTazele()
        ajanSonuc.text = "AJAN ÇALIŞTI:\n" + log.joinToString("\n")
        Toast.makeText(this, "🤖 ${log.size} eylem başarıyla uygulandı!", Toast.LENGTH_SHORT).show()
    }

    private fun gundemiOrkestreEt() {
        val gorevler = Store.loadTasks(this)
        if (gorevler.isEmpty()) {
            Toast.makeText(this, "Listede henüz görev yok", Toast.LENGTH_SHORT).show()
            return
        }
        val simdi = System.currentTimeMillis()
        val kayit = UykuCerceve.gunBul(this, UykuCerceve.gunKey(simdi))
        val uykuMs = kayit?.uykuMs ?: 8 * 3600_000L
        val saat = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        val orkestre = OtonomMotor.gundemOrkestrasyonu(
            gorevler.map { it.text },
            uykuMs,
            saat
        )

        val metinler = orkestre.map { o ->
            "▶ ${o.baslik}\n   ⏰ Önerilen: ${o.onerilenSaat}\n   💡 ${o.gerekce}"
        }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle("🤖 Gündem Orkestrasyonu")
            .setItems(metinler, null)
            .setPositiveButton("Tamam", null)
            .show()
    }

    private fun seriBekcisiniCalistir() {
        val aliskanliklar = Store.loadHabits(this)
        if (aliskanliklar.isEmpty()) {
            Toast.makeText(this, "Listede henüz alışkanlık yok", Toast.LENGTH_SHORT).show()
            return
        }

        val cal = Calendar.getInstance()
        val saat = cal.get(Calendar.HOUR_OF_DAY)
        val oneriler = OtonomMotor.seriKurtarmaAnalizi(
            aliskanliklar.map { it.title },
            aliskanliklar.map { Store.habitCount(this, it.id) >= it.target },
            aliskanliklar.map { Store.habitStreak(this, it) },
            if (saat < 18) 21 else saat // Gündüz denense bile akşam modu simüle et
        )

        if (oneriler.isEmpty()) {
            MaterialAlertDialogBuilder(this)
                .setTitle("🛡️ Seri Bekçisi")
                .setMessage("Tebrikler! Şu an kırılma riskinde olan veya tamamlanmamış aktif seriniz bulunmuyor.")
                .setPositiveButton("Tamam", null)
                .show()
            return
        }

        val ilk = oneriler.first()
        MaterialAlertDialogBuilder(this)
            .setTitle("🛡️ Seri Bekçisi Alarmi")
            .setMessage(ilk.mesaj + "\n\n10 dakikalık mikro odak sayacını başlatıp seriyi otomatik kurtaralım mı?")
            .setPositiveButton("▶ 10 Dk Sayaç Başlat & Kurtar") { _, _ ->
                TimerEngine.setTotalMs(this, ilk.kurtarmaDk * 60_000L)
                TimerEngine.start(this)
                val h = aliskanliklar.find { it.title == ilk.aliskanlikAd }
                if (h != null) {
                    Store.toggleHabit(this, h)
                }
                WidgetCommon.refreshAll(this)
                durumOzetleriniTazele()
                Toast.makeText(this, "▶ 10 dk kurtarma sayacı başladı, '${ilk.aliskanlikAd}' güvende!", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Kapat", null)
            .show()
    }

    private fun kutuphaneciyiCalistir() {
        val notlar = Store.loadNotes(this)
        if (notlar.isEmpty()) {
            Toast.makeText(this, "Notlar depoda boş", Toast.LENGTH_SHORT).show()
            return
        }

        val eylemler = OtonomMotor.notlardanGorevCikar(notlar.map { it.content })
        if (eylemler.isEmpty()) {
            MaterialAlertDialogBuilder(this)
                .setTitle("🧹 Otonom Kütüphaneci")
                .setMessage("Notlarınız tarandı. Yeni bir eylem maddesi veya TODO bulunamadı.")
                .setPositiveButton("Tamam", null)
                .show()
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("🧹 Kütüphaneci Bulguları (${eylemler.size} eylem)")
            .setItems(eylemler.toTypedArray(), null)
            .setPositiveButton("Hepsini Görevlere Ekle") { _, _ ->
                eylemler.forEach { e -> Store.addTask(this, e) }
                WidgetCommon.refreshAll(this)
                Toast.makeText(this, "✅ ${eylemler.size} madde Görevlere eklendi!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("İptal", null)
            .show()
    }
}
