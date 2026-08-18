package com.gunlukasistan.app

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * v10.2 · Öneri A1 — Uyanık bitiş ekranı.
 *
 * ── Ne ──
 * Sayaç bittiğinde telefon uyurken/kilitliyken bile kilit üstünde
 * tam ekran açılır; Samsung Saat'in alarm ekranının karşılığı.
 * [TimerNotifier.showDone] bu ekranı `setFullScreenIntent` ile
 * bağlar; izin yoksa normal bildirime düşülür ve bu ekran ancak
 * bildirime dokunulunca gelir.
 *
 * ── İçerik ──
 *   · Büyük "Süre doldu" başlığı + oturum özeti
 *   · Öneri A12: tek dokunuş değerlendirme (dağınık / fena / odaklı)
 *     → `SureAnalizi` kalite kaydı
 *   · Öneri A2: "＋5 dk uzat" ve "Yeniden başlat"
 *   · "Kapat" — elle kapanış (geri tuşu da çalışır)
 *
 * `excludeFromRecents` manifest'te — son kullanılanlar listesini
 * kirletmesin.
 */
class SayacBittiActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        runCatching { setTheme(ThemeManager.styleFor(this)) }
        super.onCreate(savedInstanceState)

        // Kilit üstünde göster + ekranı uyandır (her iki API kuşağı)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_sayac_bitti)

        val dakika = (TimerEngine.totalMs(this) / 60_000L).toInt()
        findViewById<TextView>(R.id.sbMesaj)?.text =
            getString(R.string.sb_mesaj, dakika)

        // Bitiş bildirimi panelde asılı kalmasın
        runCatching { TimerActionReceiver.bitisBildiriminiKapat(this) }

        findViewById<View>(R.id.sbUzat)?.setOnClickListener {
            TimerEngine.uzat(this, 5 * 60_000L)
            TimerAlarm.reschedule(this)
            Titresim.sayacDurum(this)
            finish()
        }

        findViewById<View>(R.id.sbYeniden)?.setOnClickListener {
            TimerEngine.start(this)
            TimerAlarm.reschedule(this)
            Titresim.sayacDurum(this)
            finish()
        }

        // A12 — tek dokunuş değerlendirme; bir kez yazılır.
        // Pomodoro kaydı döngü tarafından zaten atılmışsa üstüne
        // puan işlenir (çift sayım olmaz); yoksa yeni kayıt açılır.
        var kaydedildi = false
        val kaliteSec = { kalite: Int ->
            if (!kaydedildi) {
                kaydedildi = true
                if (!SureAnalizi.sonKaydiKalitele(this, kalite)) {
                    SureAnalizi.pomodoroKaydetK(this, dakika, tamamlandi = true, kalite)
                }
                runCatching {
                    findViewById<View>(R.id.sbDegerlendirBaslik)?.visibility = View.GONE
                    findViewById<View>(R.id.sbTesekkur)?.visibility = View.VISIBLE
                    findViewById<View>(R.id.sbK1)?.isEnabled = false
                    findViewById<View>(R.id.sbK2)?.isEnabled = false
                    findViewById<View>(R.id.sbK3)?.isEnabled = false
                }
                Titresim.basari(this)
            }
        }
        findViewById<View>(R.id.sbK1)?.setOnClickListener { kaliteSec(1) }
        findViewById<View>(R.id.sbK2)?.setOnClickListener { kaliteSec(2) }
        findViewById<View>(R.id.sbK3)?.setOnClickListener { kaliteSec(3) }

        findViewById<View>(R.id.sbKapat)?.setOnClickListener { finish() }
        if (SayacAyar.ciktiHasadiAcik(this)) {
            hasatDiyalogu(dakika)
        }
    }

    private fun hasatDiyalogu(odakDk: Int) {
        val input = android.widget.EditText(this).apply {
            hint = "💡 Bu seansta ne üretildi / bitti?"
            minLines = 2
        }
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("💡 Oturum Hasadı ($odakDk dk)")
            .setView(input)
            .setPositiveButton("Notlara Kaydet") { _, _ ->
                val metin = input.text.toString().trim()
                if (metin.isNotBlank()) {
                    val notIcerik = OdakMotoru.ciktiNotuFormatla(metin, odakDk, null)
                    Store.addNote(this, notIcerik)
                    android.widget.Toast.makeText(this, "💡 Hasat notlara eklendi!", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Geç", null)
            .show()
    }

    override fun onDestroy() {
        // v11.13: ekran kapanınca/yok edilince çalan alarm sesi de susar.
        // "Kapat" düğmesi veya geri tuşu buraya düşer.
        BitisSesMotoru.durdur()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onDestroy()
    }
}
