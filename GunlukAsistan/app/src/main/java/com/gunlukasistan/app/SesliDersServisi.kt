package com.gunlukasistan.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import java.util.Locale

/**
 * v7.32 — Ders metnini sesli okur.
 *
 * Arka planda çalışan bir foreground servis: uygulama kapansa bile
 * okuma devam eder, bildirimden duraklat/devam/durdur yapılabilir.
 *
 * Tasarım kararları:
 *  · Metin **cümlelere bölünür** — TTS'in 4000 karakter sınırı var ve
 *    parça parça okumak duraklat/devam'ı hassas yapar.
 *  · Nerede kalındığı saklanır; aynı dersi tekrar açınca kaldığı
 *    cümleden devam eder.
 *  · Türkçe ses yoksa kullanıcı uyarılır (cihazda TR paketi gerekir).
 */
class SesliDersServisi : Service(), TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "SesliDers"
        const val CHANNEL_ID = "sesli_ders_v1"
        const val NOTIF_ID = 7320

        const val EYLEM_BASLAT = "sesli.baslat"
        const val EYLEM_DURAKLAT = "sesli.duraklat"
        const val EYLEM_DEVAM = "sesli.devam"
        const val EYLEM_DUR = "sesli.dur"
        const val EYLEM_ILERI = "sesli.ileri"
        const val EYLEM_GERI = "sesli.geri"

        const val EK_METIN = "metin"
        const val EK_BASLIK = "baslik"
        const val EK_ASSET = "asset"
        const val EK_HIZ = "hiz"

        /** Servis şu an okuyor mu — arayüzün durumu bilmesi için. */
        @Volatile
        var calisiyor = false
            private set

        @Volatile
        var duraklatildi = false
            private set

        /** Okunan dersin asset yolu. */
        @Volatile
        var aktifAsset = ""
            private set

        fun baslat(context: Context, metin: String, baslik: String, asset: String, hiz: Float) {
            val i = Intent(context, SesliDersServisi::class.java).apply {
                action = EYLEM_BASLAT
                putExtra(EK_METIN, metin)
                putExtra(EK_BASLIK, baslik)
                putExtra(EK_ASSET, asset)
                putExtra(EK_HIZ, hiz)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }
        }

        fun komut(context: Context, eylem: String) {
            try {
                context.startService(
                    Intent(context, SesliDersServisi::class.java).apply { action = eylem }
                )
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Komut gönderilemedi", e)
            }
        }
    }

    private var tts: TextToSpeech? = null
    private var cumleler: List<String> = emptyList()
    private var indeks = 0
    private var baslik = ""
    private var asset = ""
    private var hiz = 1.0f
    private var hazir = false
    private var bekleyenBaslat = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        kanalOlustur()
        tts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            android.util.Log.w(TAG, "TTS başlatılamadı: $status")
            durdur()
            return
        }
        val motor = tts ?: return

        // Türkçe ses ayarla
        val sonuc = motor.setLanguage(Locale("tr", "TR"))
        if (sonuc == TextToSpeech.LANG_MISSING_DATA ||
            sonuc == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            android.util.Log.w(TAG, "Türkçe TTS yok, varsayılan dil kullanılacak")
        }
        motor.setSpeechRate(hiz)

        motor.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                // Sıradaki cümleye geç
                if (duraklatildi) return
                indeks++
                konumKaydet()
                if (indeks < cumleler.size) {
                    oku(indeks)
                    bildirimGuncelle()
                } else {
                    // v8.1: oynatma listesi varsa sıradaki konuya geç (öneri 9).
                    // Liste modunda "asset" değeri "liste:" ile başlıyor.
                    if (asset.startsWith("liste:") && sonrakiListeOgesi()) return
                    // Ders bitti
                    bittiBildir()
                    durdur()
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                android.util.Log.w(TAG, "Okuma hatası: $utteranceId")
                indeks++
                if (indeks < cumleler.size) oku(indeks) else durdur()
            }
        })

        hazir = true
        if (bekleyenBaslat) {
            bekleyenBaslat = false
            okumayaBasla()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            EYLEM_BASLAT -> {
                val metin = intent.getStringExtra(EK_METIN).orEmpty()
                baslik = intent.getStringExtra(EK_BASLIK).orEmpty()
                asset = intent.getStringExtra(EK_ASSET).orEmpty()
                hiz = intent.getFloatExtra(EK_HIZ, 1.0f)
                cumleler = cumlelereBol(metin)
                indeks = kayitliKonum()
                if (indeks >= cumleler.size) indeks = 0

                if (cumleler.isEmpty()) {
                    durdur()
                    return START_NOT_STICKY
                }
                startForeground(NOTIF_ID, bildirimKur())
                if (hazir) okumayaBasla() else bekleyenBaslat = true
            }

            EYLEM_DURAKLAT -> {
                duraklatildi = true
                tts?.stop()
                bildirimGuncelle()
            }

            EYLEM_DEVAM -> {
                duraklatildi = false
                oku(indeks)
                bildirimGuncelle()
            }

            EYLEM_ILERI -> {
                tts?.stop()
                indeks = (indeks + 1).coerceAtMost(cumleler.size - 1)
                konumKaydet()
                if (!duraklatildi) oku(indeks)
                bildirimGuncelle()
            }

            EYLEM_GERI -> {
                tts?.stop()
                indeks = (indeks - 1).coerceAtLeast(0)
                konumKaydet()
                if (!duraklatildi) oku(indeks)
                bildirimGuncelle()
            }

            EYLEM_DUR -> durdur()
        }
        return START_NOT_STICKY
    }

    // ─────────────────── Okuma ───────────────────

    private fun okumayaBasla() {
        calisiyor = true
        duraklatildi = false
        aktifAsset = asset
        oku(indeks)
        bildirimGuncelle()
    }

    private fun oku(i: Int) {
        if (i !in cumleler.indices) return
        val motor = tts ?: return
        motor.setSpeechRate(hiz)
        motor.speak(cumleler[i], TextToSpeech.QUEUE_FLUSH, null, "c$i")
    }

    /**
     * Metni cümlelere böler.
     * TTS'in 4000 karakter sınırı var; ayrıca küçük parçalar
     * duraklat/ileri/geri kontrolünü hassas yapar.
     */
    private fun cumlelereBol(metin: String): List<String> {
        val temiz = metin
            .replace("<<<SAYFA>>>", " ")
            // Sayfa altbilgisi ve tekrar eden başlıkları at
            .replace(Regex("Günlük Asistan"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (temiz.isBlank()) return emptyList()

        val parcalar = mutableListOf<String>()
        // Cümle sonlarından böl
        val ham = temiz.split(Regex("(?<=[.!?:])\\s+"))
        val tampon = StringBuilder()

        for (c in ham) {
            val cc = c.trim()
            if (cc.isBlank()) continue
            // Çok uzun cümleyi de böl (TTS sınırı)
            if (cc.length > 3500) {
                if (tampon.isNotEmpty()) {
                    parcalar.add(tampon.toString().trim()); tampon.clear()
                }
                cc.chunked(3000).forEach { parcalar.add(it) }
                continue
            }
            // Kısa parçaları birleştir — çok sık duraklamasın
            if (tampon.length + cc.length < 220) {
                tampon.append(if (tampon.isEmpty()) "" else " ").append(cc)
            } else {
                if (tampon.isNotEmpty()) parcalar.add(tampon.toString().trim())
                tampon.clear()
                tampon.append(cc)
            }
        }
        if (tampon.isNotEmpty()) parcalar.add(tampon.toString().trim())
        return parcalar.filter { it.length > 1 }
    }

    // ─────────────────── Konum hatırlama ───────────────────

    private fun prefs() = getSharedPreferences("sesli_ders", Context.MODE_PRIVATE)

    private fun konumKaydet() {
        if (asset.isBlank()) return
        try {
            prefs().edit().putInt("k_" + asset.replace('/', '_'), indeks).apply()
        } catch (_: Exception) {
        }
    }

    private fun kayitliKonum(): Int = try {
        prefs().getInt("k_" + asset.replace('/', '_'), 0)
    } catch (_: Exception) {
        0
    }

    // ─────────────────── Bildirim ───────────────────

    private fun kanalOlustur() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val yonetici = getSystemService(NotificationManager::class.java) ?: return
        if (yonetici.getNotificationChannel(CHANNEL_ID) != null) return
        yonetici.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.tts_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.tts_channel_desc)
                setShowBadge(false)
            }
        )
    }

    private fun eylemIntent(eylem: String): PendingIntent {
        val i = Intent(this, SesliDersServisi::class.java).apply { action = eylem }
        val bayrak = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_IMMUTABLE else 0)
        return PendingIntent.getService(this, eylem.hashCode(), i, bayrak)
    }

    private fun bildirimKur(): Notification {
        val ac = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val ilerleme = if (cumleler.isEmpty()) 0
        else (indeks + 1) * 100 / cumleler.size

        val b = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(baslik.ifBlank { getString(R.string.tts_title) })
            .setContentText(
                getString(R.string.tts_progress, indeks + 1, cumleler.size, ilerleme)
            )
            .setContentIntent(ac)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setProgress(cumleler.size.coerceAtLeast(1), indeks + 1, false)

        b.addAction(
            android.R.drawable.ic_media_previous,
            getString(R.string.tts_prev), eylemIntent(EYLEM_GERI)
        )
        if (duraklatildi) {
            b.addAction(
                android.R.drawable.ic_media_play,
                getString(R.string.tts_resume), eylemIntent(EYLEM_DEVAM)
            )
        } else {
            b.addAction(
                android.R.drawable.ic_media_pause,
                getString(R.string.tts_pause), eylemIntent(EYLEM_DURAKLAT)
            )
        }
        b.addAction(
            android.R.drawable.ic_media_next,
            getString(R.string.tts_next), eylemIntent(EYLEM_ILERI)
        )
        b.addAction(
            android.R.drawable.ic_delete,
            getString(R.string.tts_stop), eylemIntent(EYLEM_DUR)
        )
        return b.build()
    }

    private fun bildirimGuncelle() {
        try {
            val yonetici = getSystemService(NotificationManager::class.java) ?: return
            yonetici.notify(NOTIF_ID, bildirimKur())
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Bildirim güncellenemedi", e)
        }
    }

    private fun bittiBildir() {
        try {
            // Ders bitti — baştan başlasın
            prefs().edit().putInt("k_" + asset.replace('/', '_'), 0).apply()
        } catch (_: Exception) {
        }
    }

    private fun durdur() {
        calisiyor = false
        duraklatildi = false
        aktifAsset = ""
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (_: Exception) {
        }
        stopSelf()
    }

    override fun onDestroy() {
        calisiyor = false
        aktifAsset = ""
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            android.util.Log.w(TAG, "TTS kapatılamadı", e)
        }
        tts = null
        super.onDestroy()
    }

    /**
     * v8.1 — Oynatma listesindeki sıradaki konuya geçer (öneri 9).
     *
     * Anlatımı silinmiş öğeler atlanıyor: kullanıcı "yeniden üret" demiş
     * olabilir ve o an metin yok. Sonsuz döngüye girmemek için en fazla
     * liste uzunluğu kadar deneniyor.
     *
     * @return sıradaki okumaya geçildiyse true
     */
    private fun sonrakiListeOgesi(): Boolean {
        return try {
            var deneme = 0
            val tavan = SesliListe.sayi(applicationContext).coerceAtLeast(1)

            while (deneme < tavan) {
                deneme++
                val sonraki = SesliListe.ilerle(applicationContext) ?: return false
                val metin = SesliListe.aktifMetin(applicationContext)
                if (metin.isNullOrBlank()) continue

                // Yeni metinle baştan başla
                cumleler = cumlelereBol(metin)
                indeks = 0
                baslik = sonraki.madde + "  " +
                    SesliListe.ilerlemeMetni(applicationContext)
                konumKaydet()
                if (cumleler.isEmpty()) continue
                oku(0)
                bildirimGuncelle()
                return true
            }
            false
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Liste ilerletilemedi", e)
            false
        }
    }
}
