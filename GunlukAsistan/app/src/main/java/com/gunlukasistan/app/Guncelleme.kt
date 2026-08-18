package com.gunlukasistan.app

import android.content.Context
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * v9.8 — Uygulama içi güncelleme kontrolü (öneri 48).
 *
 * ══════════════════════════════════════════════════════════════════
 * SORUN
 * ══════════════════════════════════════════════════════════════════
 * Uygulama Play Store'da değil. APK elden dağıtılıyor. Sonuç:
 * kullanıcının yeni sürümden haberi olmuyor.
 *
 * v9.2'de bir çökme düzelttim. v9.3'te bir tane daha. Bu düzeltmeler
 * ancak kullanıcı yeni APK'yı indirirse işe yarıyor. Elinde v9.1
 * kalan biri hâlâ çöken bir uygulama kullanıyor ve bunu bilmiyor.
 *
 * ══════════════════════════════════════════════════════════════════
 * ÇÖZÜM — textdb.online üzerinden sürüm bildirimi
 * ══════════════════════════════════════════════════════════════════
 * Zaten [OnlineStore] için kullandığımız ücretsiz servisi
 * kullanıyorum. Sabit bir anahtara sürüm bilgisi yazılıyor:
 *
 * ```json
 * {
 *   "code": 154,
 *   "name": "9.8",
 *   "url": "https://gofile.io/d/XXXX",
 *   "notes": "Grup G: WorkManager, güncelleme kontrolü...",
 *   "min": 100,
 *   "critical": false
 * }
 * ```
 *
 * `min`: bu sürümün altındakiler için güncelleme **zorunlu** sayılır
 * (kritik çökme düzeltmesi varsa). Yine de zorla kapatmıyoruz —
 * sadece uyarı daha görünür oluyor.
 *
 * ══════════════════════════════════════════════════════════════════
 * TASARIM KARARLARI
 * ══════════════════════════════════════════════════════════════════
 *
 * ── 1. Neden otomatik indirme yok ──
 * APK indirip kurmak `REQUEST_INSTALL_PACKAGES` izni istiyor. Bu
 * izin Play Store politikalarında kırmızı bayrak ve kullanıcı
 * açısından da ürkütücü. Kullanıcıyı indirme sayfasına
 * yönlendiriyorum, gerisini o yapıyor.
 *
 * ── 2. Neden günde bir kez ──
 * Her açılışta ağ isteği yapmak hem pil hem veri israfı. Sürüm
 * günde birden fazla değişmiyor. Son kontrol zamanı kaydediliyor.
 *
 * ── 3. Neden sessiz başarısızlık ──
 * Sunucuya ulaşılamazsa **hiçbir şey gösterilmiyor**. "Güncelleme
 * kontrol edilemedi" uyarısı kullanıcı için gürültü — yapabileceği
 * bir şey yok. Yalnızca elle kontrol ettiğinde hata gösteriliyor.
 *
 * ── 4. Neden "ertele" var ──
 * Kullanıcı güncellemeyi görüp "sonra" diyebilmeli. Aksi halde her
 * açılışta aynı pencereyle karşılaşır ve uygulamadan soğur.
 * Ertelenen sürüm bir daha sorulmuyor (yeni sürüm çıkana kadar).
 */
object Guncelleme {

    private const val TAG = "Guncelleme"
    private const val PREF = "guncelleme_v1"

    private const val K_SON_KONTROL = "son_kontrol"
    private const val K_SON_KOD = "son_gorulen_kod"
    private const val K_ERTELENEN = "ertelenen_kod"
    private const val K_ONBELLEK = "onbellek_json"
    private const val K_ACIK = "acik"

    /** Kontrol aralığı — 20 saat (günde bir ama saat kaymasına toleranslı). */
    private const val ARALIK_MS = 20 * 60 * 60 * 1000L

    /**
     * Sürüm bilgisinin tutulduğu textdb anahtarı.
     *
     * `OnlineStore` kullanıcı odaları için `gunlukasistan-v1-KOD`
     * kullanıyor. Bu ayrı ve sabit bir anahtar; kullanıcı odalarıyla
     * çakışmaması için farklı önek verdim.
     */
    private const val ANAHTAR = "gunlukasistan-surum-v1"
    private const val TABAN = "https://textdb.online"

    private fun p(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ══════════════════════════════════════════════════════════

    fun acikMi(context: Context): Boolean =
        runCatching { p(context).getBoolean(K_ACIK, true) }.getOrDefault(true)

    fun ayarla(context: Context, acik: Boolean) {
        runCatching { p(context).edit().putBoolean(K_ACIK, acik).apply() }
    }

    fun sonKontrol(context: Context): Long =
        runCatching { p(context).getLong(K_SON_KONTROL, 0L) }.getOrDefault(0L)

    // ══════════════════════════════════════════════════════════
    // Veri modeli
    // ══════════════════════════════════════════════════════════

    data class Surum(
        val kod: Int,
        val ad: String,
        val url: String,
        val notlar: String,
        /** Bu kodun altındaki sürümler için güncelleme kritik sayılır. */
        val enAz: Int = 0,
        val kritik: Boolean = false
    ) {
        val gecerli: Boolean get() = kod > 0 && ad.isNotBlank()
    }

    /**
     * Kontrol sonucu.
     *
     * `Surum?` yerine sealed class kullanıyorum çünkü üç farklı
     * durum var ve `null` hangisi olduğunu söylemiyor:
     * güncel miyiz, ağ mı yok, yoksa sunucuda veri mi yok.
     */
    sealed class Sonuc {
        /** Yeni sürüm var. */
        data class Yeni(val surum: Surum, val kritik: Boolean) : Sonuc()
        /** Zaten güncel. */
        object Guncel : Sonuc()
        /** Kontrol edilemedi (ağ yok, sunucu hatası, bozuk veri). */
        data class Hata(val mesaj: String) : Sonuc()
    }

    // ══════════════════════════════════════════════════════════
    // Kontrol
    // ══════════════════════════════════════════════════════════

    /**
     * Sunucudan sürüm bilgisini okur.
     *
     * **Ağ işlemi** — arka planda çağırın.
     *
     * @param zorla true ise 20 saat kuralını atla (elle kontrol)
     */
    fun kontrolEt(context: Context, zorla: Boolean = false): Sonuc {
        if (!zorla) {
            if (!acikMi(context)) return Sonuc.Guncel
            val gecen = System.currentTimeMillis() - sonKontrol(context)
            if (gecen < ARALIK_MS) {
                // Süre dolmadı — önbellekteki bilgiyi kullan
                return onbellektenSonuc(context)
            }
        }
        if (!cevrimici(context)) {
            return Sonuc.Hata(context.getString(R.string.ai_err_no_net))
        }

        return try {
            val govde = oku()
            if (govde.isBlank()) {
                return Sonuc.Hata(context.getString(R.string.gc_err_bos))
            }
            val surum = ayristir(govde)
                ?: return Sonuc.Hata(context.getString(R.string.gc_err_bozuk))

            // Başarılı — zamanı ve önbelleği güncelle
            runCatching {
                p(context).edit()
                    .putLong(K_SON_KONTROL, System.currentTimeMillis())
                    .putString(K_ONBELLEK, govde)
                    .putInt(K_SON_KOD, surum.kod)
                    .apply()
            }
            degerlendir(context, surum)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Kontrol başarısız", e)
            Sonuc.Hata(context.getString(R.string.on_err_ag))
        }
    }

    private fun onbellektenSonuc(context: Context): Sonuc {
        val ham = runCatching { p(context).getString(K_ONBELLEK, "") }.getOrNull()
        if (ham.isNullOrBlank()) return Sonuc.Guncel
        val surum = ayristir(ham) ?: return Sonuc.Guncel
        return degerlendir(context, surum)
    }

    /**
     * Sürümü mevcut kurulumla karşılaştırır.
     *
     * Ertelenmiş sürüm kontrolü burada: kullanıcı "sonra" dediyse
     * aynı sürüm için bir daha rahatsız edilmiyor. Ama **kritik**
     * güncellemeler erteleme dinlemiyor — çökme düzeltmesi
     * ertelenemez.
     */
    private fun degerlendir(context: Context, surum: Surum): Sonuc {
        val mevcut = BuildConfig.VERSION_CODE
        if (surum.kod <= mevcut) return Sonuc.Guncel

        val kritik = surum.kritik || (surum.enAz > 0 && mevcut < surum.enAz)

        if (!kritik) {
            val ertelenen = runCatching { p(context).getInt(K_ERTELENEN, 0) }.getOrDefault(0)
            if (ertelenen >= surum.kod) return Sonuc.Guncel
        }
        return Sonuc.Yeni(surum, kritik)
    }

    /** Kullanıcı "sonra" dedi — bu sürüm bir daha sorulmasın. */
    fun ertele(context: Context, kod: Int) {
        runCatching { p(context).edit().putInt(K_ERTELENEN, kod).apply() }
    }

    // ══════════════════════════════════════════════════════════
    // Ağ
    // ══════════════════════════════════════════════════════════

    private fun oku(): String {
        val url = "$TABAN/$ANAHTAR/"
        val conn = (URL(url).openConnection() as HttpsURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12000
            readTimeout = 15000
            instanceFollowRedirects = true
        }
        return try {
            val kod = conn.responseCode
            val akis = if (kod in 200..299) conn.inputStream else conn.errorStream
            val govde = BufferedReader(InputStreamReader(akis, Charsets.UTF_8))
                .use { it.readText() }.trim()
            if (kod !in 200..299) "" else govde
        } finally {
            runCatching { conn.disconnect() }
        }
    }

    /**
     * JSON'u çözer.
     *
     * Savunmacı: sunucudaki veriyi ben yazıyorum ama yazım hatası
     * yapabilirim. Bozuk veri yüzünden uygulama çökmemeli — bu
     * özellik "olsa iyi olur" kategorisinde, kritik değil.
     */
    fun ayristir(ham: String): Surum? = runCatching {
        val temiz = ham.trim()
        if (!temiz.startsWith("{")) return null
        val o = JSONObject(temiz)
        val s = Surum(
            kod = o.optInt("code", 0),
            ad = o.optString("name", ""),
            url = o.optString("url", ""),
            notlar = o.optString("notes", ""),
            enAz = o.optInt("min", 0),
            kritik = o.optBoolean("critical", false)
        )
        if (s.gecerli) s else null
    }.getOrNull()

    private fun cevrimici(context: Context): Boolean = runCatching {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as? android.net.ConnectivityManager ?: return false
        val ag = cm.activeNetwork ?: return false
        val yetenek = cm.getNetworkCapabilities(ag) ?: return false
        yetenek.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }.getOrDefault(false)

    // ══════════════════════════════════════════════════════════
    // Arayüz yardımcısı
    // ══════════════════════════════════════════════════════════

    /**
     * Arka planda kontrol edip sonucu ana iş parçacığında verir.
     *
     * `App.onCreate`'ten çağrılıyor — kullanıcı arayüzü hazır
     * olmadığı için sonuç bir sonraki Activity'de gösteriliyor.
     */
    fun arkaPlandaKontrol(context: Context, sonra: ((Sonuc) -> Unit)? = null) {
        val uyg = context.applicationContext
        runCatching {
            Performans.arkaPlan {
                val sonuc = runCatching { kontrolEt(uyg) }
                    .getOrElse { Sonuc.Hata(it.message ?: "?") }
                if (sonra != null) {
                    runCatching {
                        android.os.Handler(android.os.Looper.getMainLooper())
                            .post { sonra(sonuc) }
                    }
                }
            }
        }
    }

    /**
     * Bekleyen güncelleme var mı — ağ isteği YAPMADAN.
     *
     * Ayarlar ekranında rozet göstermek için. Önbellekteki son
     * bilgiye bakıyor, hızlı.
     */
    fun bekleyenVar(context: Context): Surum? {
        val sonuc = runCatching { onbellektenSonuc(context) }.getOrNull()
        return (sonuc as? Sonuc.Yeni)?.surum
    }

    /** İndirme sayfasını tarayıcıda açar. */
    fun indirmeyiAc(context: Context, surum: Surum): Boolean = runCatching {
        if (surum.url.isBlank()) return false
        val niyet = android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse(surum.url)
        ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(niyet)
        true
    }.getOrDefault(false)

    /**
     * Sunucuya yazılacak JSON'u üretir — **geliştirici aracı**.
     *
     * Bu fonksiyonu uygulama çağırmıyor. Yeni sürüm yayınlarken
     * doğru biçimi hatırlamak için burada duruyor; elle
     * textdb.online'a yazılıyor.
     */
    fun yayinJsonu(
        kod: Int, ad: String, url: String, notlar: String,
        enAz: Int = 0, kritik: Boolean = false
    ): String = JSONObject()
        .put("code", kod).put("name", ad).put("url", url)
        .put("notes", notlar).put("min", enAz).put("critical", kritik)
        .toString()
}
