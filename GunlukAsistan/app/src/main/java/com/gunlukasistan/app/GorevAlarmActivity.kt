package com.gunlukasistan.app

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * v10.15 · ULTRA-30 / C13 — Tam ekran görev alarmı (kilit üstü).
 *
 * Yalnız 🔴 "acil" etiketli görevlerin hatırlatıcısı buraya düşer
 * (`ReminderReceiver` fullScreenIntent + CATEGORY_ALARM kurar).
 * Kademeli erteleme ve bedel kararları [KritikAlarm]'dadır (saf, testli).
 *
 * ── Butonlar ──
 * ✓ Yaptım  → `TaskActionReceiver.ACTION_DONE` yayını (tek kapı —
 *              Store güncellemesi, seri, bildirim kapatma aynı akışta),
 *              erteleme sayacı sıfırlanır.
 * ⏰ Ertele → kademe 5·10·15 dk ([KritikAlarm.ertelemeDakikasi]);
 *              4. istek bedele düşer: ertesi gün 09:00, sayaç sıfır.
 */
class GorevAlarmActivity : AppCompatActivity() {

    private var gorevId: Long = -1L
    private var baslik: String = ""

    private val ekranKapatmaAlicisi = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            if (intent?.action == android.content.Intent.ACTION_SCREEN_OFF ||
                intent?.action == android.content.Intent.ACTION_SCREEN_ON) {
                if (SayacAyar.isKapatmaTusuyleAlarmDurdur(this@GorevAlarmActivity)) {
                    zil?.stop()
                    finish()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            val f = android.content.IntentFilter(android.content.Intent.ACTION_SCREEN_OFF).apply {
                addAction(android.content.Intent.ACTION_SCREEN_ON)
            }
            registerReceiver(ekranKapatmaAlicisi, f)
        } catch (_: Exception) {}
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(ekranKapatmaAlicisi) } catch (_: Exception) {}
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (SayacAyar.isKapatmaTusuyleAlarmDurdur(this)) {
            if (keyCode == android.view.KeyEvent.KEYCODE_POWER ||
                keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN ||
                keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP) {
                zil?.stop()
                finish()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Kilit üstü + ekranı aç: API 27+ attribute, altına bayrak.
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true); setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        gorevId = intent.getLongExtra("gorev_id", -1L)
        baslik = intent.getStringExtra("gorev_baslik") ?: ""
        if (gorevId < 0L) { finish(); return }

        // ZorunluUyari'dan öğrenilen ders (v7.56): kanal sesi telefon
        // sessizdeyken sistemce yutulabilir; alarm HİSSİ için sesi
        // activity kendisi çalar (USAGE_ALARM akışı, butonda durur).
        zil = runCatching {
            android.media.RingtoneManager.getRingtone(
                this,
                android.media.RingtoneManager.getDefaultUri(
                    android.media.RingtoneManager.TYPE_ALARM
                )
            )?.apply {
                if (Build.VERSION.SDK_INT >= 21) {
                    audioAttributes = android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .build()
                }
                if (Build.VERSION.SDK_INT >= 28) isLooping = true
                play()
            }
        }.getOrNull()

        kur()
    }

    private var zil: android.media.Ringtone? = null

    private fun ziliDurdur() {
        runCatching { if (zil?.isPlaying == true) zil?.stop() }
        zil = null
    }

    override fun onDestroy() {
        ziliDurdur()
        super.onDestroy()
    }

    private fun kur() {
        val dp = resources.displayMetrics.density
        val kok = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#161210"))
            setPadding((24 * dp).toInt(), (32 * dp).toInt(), (24 * dp).toInt(), (32 * dp).toInt())
        }
        fun yazi(m: String, sp: Float, renk: Int, kalin: Boolean = false): TextView =
            TextView(this).apply {
                text = m; textSize = sp; setTextColor(renk)
                gravity = Gravity.CENTER
                if (kalin) typeface = Typeface.DEFAULT_BOLD
            }

        kok.addView(yazi("🔴 KRİTİK GÖREV", 20f, Color.parseColor("#FF8A80"), true))
        kok.addView(yazi(baslik, 30f, Color.parseColor("#F4EDE4"), true).apply {
            setPadding(0, (18 * dp).toInt(), 0, (8 * dp).toInt())
        })
        val sayac = getSharedPreferences(PREF, 0).getInt("k_$gorevId", 0)
        kok.addView(yazi(
            if (sayac > 0) getString(R.string.gc_alarm_erteleme_durum, sayac, KritikAlarm.MAKS_ERTELEME) else getString(R.string.gc_alarm_ilk),
            14f, Color.parseColor("#9B9082")
        ).apply { setPadding(0, 0, 0, (28 * dp).toInt()) })

        fun dugme(m: String, renk: Int, metinRenk: Int): TextView = TextView(this).apply {
            text = m; textSize = 17f; setTextColor(metinRenk)
            gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD
            val r = 18 * dp
            background = GradientDrawable().apply {
                cornerRadius = r; setColor(renk)
            }
            setPadding((20 * dp).toInt(), (16 * dp).toInt(), (20 * dp).toInt(), (16 * dp).toInt())
        }
        val satir = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        val p = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = (8 * dp).toInt(); marginEnd = (8 * dp).toInt()
        }
        val bErtele = dugme(getString(R.string.gc_alarm_ertele), Color.parseColor("#3A332C"), Color.parseColor("#F4EDE4"))
        val bYapildi = dugme(getString(R.string.gc_alarm_yapildi), Color.parseColor("#D9B892"), Color.parseColor("#241C14"))
        satir.addView(bErtele, p); satir.addView(bYapildi, p)
        kok.addView(satir)

        bErtele.setOnClickListener { ertele() }
        bYapildi.setOnClickListener { yapildi() }
        setContentView(kok)
    }

    private fun bildirimKapat() {
        val notifId = ((gorevId % 50000).toInt() + 7000)
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(notifId)
    }

    private fun yapildi() {
        ziliDurdur()
        getSharedPreferences(PREF, 0).edit().remove("k_$gorevId").apply()
        // Tek kapı: mevcut akışla birebir aynı yayın.
        sendBroadcast(Intent(this, TaskActionReceiver::class.java).apply {
            action = TaskActionReceiver.ACTION_DONE
            putExtra(TaskActionReceiver.EXTRA_TASK_ID, gorevId)
            putExtra(TaskActionReceiver.EXTRA_TASK_TITLE, baslik)
        })
        GorevErteleme.kaydet(this, gorevId)
        bildirimKapat()
        Toast.makeText(this, R.string.gc_alarm_yapildi_tamam, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun ertele() {
        ziliDurdur()
        val sp = getSharedPreferences(PREF, 0)
        val n = sp.getInt("k_$gorevId", 0) + 1
        val simdi = System.currentTimeMillis()
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = simdi
            set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
        }
        val sonraki = KritikAlarm.sonrakiUyariMs(n, simdi, cal.timeInMillis)
        bildirimKapat()
        if (KritikAlarm.bedelGerekliMi(n)) {
            // Bedel: erteleme kredisi yarınla sıfırlanır.
            sp.edit().remove("k_$gorevId").apply()
            runCatching { AlarmScheduler.schedule(this, gorevId, baslik, sonraki) }
            Toast.makeText(this, R.string.gc_alarm_bedel, Toast.LENGTH_LONG).show()
        } else {
            sp.edit().putInt("k_$gorevId", n).apply()
            runCatching { AlarmScheduler.schedule(this, gorevId, baslik, sonraki) }
            Toast.makeText(
                this,
                getString(R.string.gc_alarm_ertelendi, KritikAlarm.ertelemeDakikasi(n)),
                Toast.LENGTH_SHORT
            ).show()
        }
        finish()
    }

    companion object {
        const val PREF = "kritik_alarm_v1"
        fun ertelemeSayisi(context: Context, gorevId: Long): Int =
            context.getSharedPreferences(PREF, 0).getInt("k_$gorevId", 0)
    }
}
