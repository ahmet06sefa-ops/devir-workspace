package com.gunlukasistan.app

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * v10.47 — Kullanıcı maddesi #9: Manuel Kontrol Merkezi.
 *
 * Kullanıcı isteği:
 * "Uyanma ve uyuma saatlerini elle manuel kontrol edebilmek istiyorum
 *  ve bir çok şeyi manuel kontrol edebilme yeri koy."
 *
 * Bu merkez üzerinden:
 * 1. Bugünkü uyku/uyanma saati elle seçilir ve geçmiş 14 günün uyku defteri düzenlenir.
 * 2. Günlük odak dakikası artırılır/azaltılır veya serbest değer yazılır.
 * 3. Gün serisi (streak) ve hedef tamamlama bayrağı elle yönetilir.
 * 4. Sabah ve akşam rutin bildirimleri anında tetiklenir.
 * 5. Bugünkü uyku/odak kayıtları istendiğinde sıfırlanabilir.
 */
class ManuelKontrolActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            context.startActivity(Intent(context, ManuelKontrolActivity::class.java))
        }
    }

    private lateinit var uyanmaDurum: TextView
    private lateinit var uyumaDurum: TextView
    private lateinit var odakDurum: TextView
    private lateinit var seriDurum: TextView

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(GorunumAyar.yaziOlcegiUygula(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(ThemeManager.styleFor(this))
        ThemeManager.applyAccent(this)
        ThemeManager.dinamikRengiUygula(this)
        setContentView(R.layout.activity_manuel_kontrol)

        uyanmaDurum = findViewById(R.id.mkUyanmaDurum)
        uyumaDurum = findViewById(R.id.mkUyumaDurum)
        odakDurum = findViewById(R.id.mkOdakDurum)
        seriDurum = findViewById(R.id.mkSeriDurum)

        findViewById<MaterialButton>(R.id.mkKapat).setOnClickListener { finish() }

        // BÖLÜM 1: Uyku & Uyanma saatleri elle seçim
        findViewById<MaterialButton>(R.id.mkBtnUyanmaSec).setOnClickListener { uyanmaSaatiSec() }
        findViewById<MaterialButton>(R.id.mkBtnUyumaSec).setOnClickListener { uyumaSaatiSec() }
        findViewById<MaterialButton>(R.id.mkBtnGecmisDuzenle).setOnClickListener { gecmisUykuDuzenle() }

        // BÖLÜM 2: Odak süresi
        findViewById<MaterialButton>(R.id.mkBtnOdakArti15).setOnClickListener { odakDakikaDegistir(15) }
        findViewById<MaterialButton>(R.id.mkBtnOdakArti30).setOnClickListener { odakDakikaDegistir(30) }
        findViewById<MaterialButton>(R.id.mkBtnOdakEksi15).setOnClickListener { odakDakikaDegistir(-15) }
        findViewById<MaterialButton>(R.id.mkBtnOdakSerbest).setOnClickListener { odakSerbestGir() }

        // BÖLÜM 3: Gün serisi ve hedef tamamlama
        findViewById<MaterialButton>(R.id.mkBtnSeriDegistir).setOnClickListener { seriElleDegistir() }
        findViewById<MaterialButton>(R.id.mkBtnBugunTamam).setOnClickListener { bugunuBasariliSay() }

        // BÖLÜM 4: Rutin tetikleme
        findViewById<MaterialButton>(R.id.mkBtnSabahCalistir).setOnClickListener { sabahRutininiCalistir() }
        findViewById<MaterialButton>(R.id.mkBtnAksamCalistir).setOnClickListener { aksamRutininiCalistir() }

        // BÖLÜM 5: Sıfırlama
        findViewById<MaterialButton>(R.id.mkBtnBugunSifirla).setOnClickListener { bugunuSifirla() }

        durumuTazele()
    }

    private fun durumuTazele() {
        val simdi = System.currentTimeMillis()
        val gunKey = UykuCerceve.gunKey(simdi)
        val kayit = UykuCerceve.gunBul(this, gunKey)

        if (kayit != null && kayit.uyandiMs > 0L) {
            val cal = Calendar.getInstance().apply { timeInMillis = kayit.uyandiMs }
            val saatMetni = String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
            uyanmaDurum.text = "Bugünkü uyanma: $saatMetni ✅"
        } else {
            uyanmaDurum.text = "Bugünkü uyanma: Henüz kaydedilmedi"
        }

        if (kayit != null && kayit.uyuduMs > 0L) {
            val cal = Calendar.getInstance().apply { timeInMillis = kayit.uyuduMs }
            val saatMetni = String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
            val sure = UykuCerceve.sureKisa(kayit.uykuMs)
            uyumaDurum.text = "Uyuma saati: $saatMetni ($sure uyku) ✅"
        } else {
            uyumaDurum.text = "Uyuma saati: Henüz kaydedilmedi"
        }

        val dk = Store.getTodayFocusMinutes(this)
        odakDurum.text = "Bugünkü odak süresi: $dk dakika"

        val seri = Store.streakInfo(this).first
        seriDurum.text = "Mevcut gün serisi: $seri gün 🔥"
    }

    private fun uyanmaSaatiSec() {
        val cal = Calendar.getInstance()
        TimePickerDialog(
            this,
            { _, saat, dakika ->
                val uyandiMs = ManuelKontrol.uyanmaZamaniHesapla(System.currentTimeMillis(), saat, dakika)
                UykuCerceve.uyandiKaydet(this, uyandiMs)
                UykuCerceve.sabahVerildi(this, uyandiMs)
                durumuTazele()
                Toast.makeText(this, "⏰ Uyanma saati $saat:$dakika olarak kaydedildi", Toast.LENGTH_SHORT).show()
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun uyumaSaatiSec() {
        val cal = Calendar.getInstance()
        TimePickerDialog(
            this,
            { _, saat, dakika ->
                val uyuduMs = ManuelKontrol.uyumaZamaniHesapla(System.currentTimeMillis(), saat, dakika)
                UykuCerceve.uyuduKaydet(this, uyuduMs)
                durumuTazele()
                Toast.makeText(this, "🌙 Uyuma saati $saat:$dakika olarak kaydedildi", Toast.LENGTH_SHORT).show()
            },
            23,
            0,
            true
        ).show()
    }

    private fun gecmisUykuDuzenle() {
        val gunler = ManuelKontrol.gecmisGunListeYarat(14, System.currentTimeMillis())
        val etiketler = gunler.map { key ->
            val g = UykuCerceve.gunBul(this, key)
            val ad = ManuelKontrol.gunAdiFormatla(key)
            if (g != null && (g.uyandiMs > 0 || g.uyuduMs > 0)) {
                val uykuMetin = if (g.uykuMs > 0) UykuCerceve.sureKisa(g.uykuMs) else "kısmi kayıt"
                "$ad · $uykuMetin ✅"
            } else {
                "$ad · kayıt yok"
            }
        }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle("Geçmiş Gün Seç")
            .setItems(etiketler) { _, secilen ->
                gunIslemSec(gunler[secilen])
            }
            .setNegativeButton("Kapat", null)
            .show()
    }

    private fun gunIslemSec(gunKey: String) {
        val ad = ManuelKontrol.gunAdiFormatla(gunKey)
        val secenekler = arrayOf("⏰ Uyanma saatini seç", "🌙 Uyuma saatini seç", "🗑️ O günün kaydını sil")
        MaterialAlertDialogBuilder(this)
            .setTitle("$ad için işlem")
            .setItems(secenekler) { _, i ->
                when (i) {
                    0 -> uyanmaDuzenle(gunKey)
                    1 -> uyumaDuzenle(gunKey)
                    2 -> {
                        UykuCerceve.gunSil(this, gunKey)
                        durumuTazele()
                        Toast.makeText(this, "$ad kaydı silindi", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun uyanmaDuzenle(gunKey: String) {
        val mevcut = UykuCerceve.gunBul(this, gunKey)
        val baslangicSaat = if (mevcut != null && mevcut.uyandiMs > 0) {
            val c = Calendar.getInstance().apply { timeInMillis = mevcut.uyandiMs }
            c.get(Calendar.HOUR_OF_DAY) to c.get(Calendar.MINUTE)
        } else 7 to 0

        TimePickerDialog(this, { _, saat, dakika ->
            val refMs = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(gunKey)?.time ?: System.currentTimeMillis()
            val uyanMs = ManuelKontrol.uyanmaZamaniHesapla(refMs, saat, dakika)
            val uyuMs = mevcut?.uyuduMs ?: 0L
            UykuCerceve.elleKaydet(this, gunKey, uyanMs, uyuMs)
            durumuTazele()
            Toast.makeText(this, "$gunKey uyanma saati güncellendi", Toast.LENGTH_SHORT).show()
        }, baslangicSaat.first, baslangicSaat.second, true).show()
    }

    private fun uyumaDuzenle(gunKey: String) {
        val mevcut = UykuCerceve.gunBul(this, gunKey)
        val baslangicSaat = if (mevcut != null && mevcut.uyuduMs > 0) {
            val c = Calendar.getInstance().apply { timeInMillis = mevcut.uyuduMs }
            c.get(Calendar.HOUR_OF_DAY) to c.get(Calendar.MINUTE)
        } else 23 to 0

        TimePickerDialog(this, { _, saat, dakika ->
            val refMs = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(gunKey)?.time ?: System.currentTimeMillis()
            val uyuMs = ManuelKontrol.uyumaZamaniHesapla(refMs, saat, dakika)
            val uyanMs = mevcut?.uyandiMs ?: 0L
            UykuCerceve.elleKaydet(this, gunKey, uyanMs, uyuMs)
            durumuTazele()
            Toast.makeText(this, "$gunKey uyuma saati güncellendi", Toast.LENGTH_SHORT).show()
        }, baslangicSaat.first, baslangicSaat.second, true).show()
    }

    private fun odakDakikaDegistir(fark: Int) {
        val mevcut = Store.getTodayFocusMinutes(this)
        val yeni = ManuelKontrol.odakDakikaSinirla(mevcut, fark)
        val gercekFark = yeni - mevcut
        if (gercekFark != 0) Store.addTodayFocusMinutes(this, gercekFark)
        WidgetCommon.refreshAll(this)
        durumuTazele()
        Toast.makeText(this, "Odak süresi: $yeni dk", Toast.LENGTH_SHORT).show()
    }

    private fun odakSerbestGir() {
        val mevcut = Store.getTodayFocusMinutes(this)
        val input = EditText(this).apply {
            setText(mevcut.toString())
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Bugünkü Odak Süresi (dk)")
            .setView(input)
            .setPositiveButton("Kaydet") { _, _ ->
                val yazilan = input.text.toString().toIntOrNull() ?: mevcut
                val yeni = ManuelKontrol.odakDakikaSinirla(0, yazilan)
                val gercekFark = yeni - mevcut
                if (gercekFark != 0) Store.addTodayFocusMinutes(this, gercekFark)
                WidgetCommon.refreshAll(this)
                durumuTazele()
                Toast.makeText(this, "Odak süresi $yeni dk oldu", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun seriElleDegistir() {
        val mevcut = Store.streakInfo(this).first
        val input = EditText(this).apply {
            setText(mevcut.toString())
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Gün Serisi (Streak)")
            .setMessage("Serini elle değiştirebilirsin:")
            .setView(input)
            .setPositiveButton("Kaydet") { _, _ ->
                val yazilan = input.text.toString().toIntOrNull() ?: mevcut
                val yeni = ManuelKontrol.seriSinirla(yazilan)
                Store.setStreakDays(this, yeni)
                WidgetCommon.refreshAll(this)
                durumuTazele()
                Toast.makeText(this, "Mevcut seri: $yeni gün 🔥", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun bugunuBasariliSay() {
        Store.addTodayFocusMinutes(this, 30)
        WidgetCommon.refreshAll(this)
        durumuTazele()
        Toast.makeText(this, "✅ Bugün başarılı / tamamlandı olarak işaretlendi! (+30 dk eklendi)", Toast.LENGTH_SHORT).show()
    }

    private fun sabahRutininiCalistir() {
        runCatching {
            UykuAksiyonReceiver.elleSabahCalistir(this)
        }
        Toast.makeText(this, "🌅 Sabah günaydın planı tetiklendi", Toast.LENGTH_SHORT).show()
    }

    private fun aksamRutininiCalistir() {
        runCatching {
            UykuAksiyonReceiver.elleAksamCalistir(this)
        }
        Toast.makeText(this, "🌙 Akşam kapanış sorusu tetiklendi", Toast.LENGTH_SHORT).show()
    }

    private fun bugunuSifirla() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Bugünü Sıfırla")
            .setMessage("Bugünkü uyku, uyanma ve odak kayıtları sıfırlanacak. Emin misin?")
            .setPositiveButton("Sıfırla") { _, _ ->
                val gunKey = UykuCerceve.gunKey(System.currentTimeMillis())
                UykuCerceve.gunSil(this, gunKey)
                val mevcutOdak = Store.getTodayFocusMinutes(this)
                if (mevcutOdak > 0) {
                    Store.addTodayFocusMinutes(this, -mevcutOdak)
                }
                WidgetCommon.refreshAll(this)
                durumuTazele()
                Toast.makeText(this, "🔄 Bugünkü kayıtlar sıfırlandı", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("İptal", null)
            .show()
    }
}
