package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * v9.8 — Çökme raporu yönetimi (öneri 49).
 *
 * ══════════════════════════════════════════════════════════════════
 * ⚠️ ÖNERİ 49'U OLDUĞU GİBİ YAPMADIM — NEDEN
 * ══════════════════════════════════════════════════════════════════
 * Öneri listesinde **"Otomatik çökme raporu gönderimi"** yazmıştım.
 * "Otomatik gönderim" kısmını **kasten yapmadım**. Sebep:
 *
 * Bir yığın izi (stack trace) sandığınızdan fazla şey sızdırır:
 *   · Dosya yolları kullanıcı adını içerebilir
 *   · `IllegalArgumentException: "Ahmet'in notu" geçersiz` gibi
 *     istisna mesajları **kullanıcı verisini** taşıyabilir
 *   · Sınıf adları hangi özellikleri kullandığını ele verir
 *     (NamazActivity → inanç, TakipActivity → sağlık)
 *
 * Kullanıcıya sormadan bunu sunucuya göndermek, uygulamanın geri
 * kalanında savunduğum gizlilik ilkesiyle çelişirdi. v9.7'de
 * "sağlık verisi telefondan çıkmıyor" diye yazdım; bir sürüm sonra
 * arka planda yığın izi göndermek tutarsızlık olurdu.
 *
 * ══════════════════════════════════════════════════════════════════
 * BUNUN YERİNE YAPTIĞIM
 * ══════════════════════════════════════════════════════════════════
 * **Otomatik yakalama + tek dokunuşla gönderim.**
 *
 *   1. Çökme **otomatik** kaydediliyor (zaten v8.8'den beri var,
 *      ama tek kayıt tutuyordu — artık **son 10 çökme**)
 *   2. Uygulama açılınca kullanıcıya **kendiliğinden soruluyor**:
 *      "Uygulama geçen sefer çöktü. Raporu göndermek ister misin?"
 *   3. Gönderilecek metin **tam olarak gösteriliyor** — ne
 *      gittiğini görüyor
 *   4. Tek dokunuşla paylaşılıyor
 *
 * v8.8'deki farkı: eskiden kullanıcının Ayarlar → Depolama →
 * "Hatayı bildir" yolunu **kendi bulması** gerekiyordu. Kimse
 * bulmadı. Şimdi uygulama kendisi soruyor.
 *
 * Otomatikleşen kısım **hatırlatma**, gönderim değil.
 *
 * ══════════════════════════════════════════════════════════════════
 * AYRICA: ÇÖKME DESENİ ANALİZİ
 * ══════════════════════════════════════════════════════════════════
 * Son 10 çökmeyi tutmanın asıl değeri: **tekrar eden çökmeyi
 * görmek**. "Aynı hata 5 kez oldu" bilgisi tek seferlik bir
 * hatadan çok daha önemli — ve bu, kullanıcının bildirmeye değer
 * bulup bulmayacağına karar vermesine de yardım ediyor.
 */
object CokmeRapor {

    private const val TAG = "CokmeRapor"

    /** v8.8'den beri kullanılan depo — geriye dönük uyumluluk şart. */
    private const val PREF = "crash_log"

    private const val K_SON = "last_crash"
    private const val K_SON_TS = "last_crash_ts"
    private const val K_SON_THREAD = "last_crash_thread"

    /** v9.8: çoklu kayıt. */
    private const val K_GECMIS = "gecmis_json"
    private const val K_SORULDU = "soruldu_ts"
    private const val K_KAPALI = "sorma_kapali"

    /** Kaç çökme saklansın. */
    private const val TAVAN = 10

    /** Yığın izinin saklanan uzunluğu — SharedPreferences şişmesin. */
    private const val IZ_SINIRI = 4000

    private fun p(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ══════════════════════════════════════════════════════════
    // Veri modeli
    // ══════════════════════════════════════════════════════════

    data class Kayit(
        val zaman: Long,
        val parca: String,
        val tur: String,
        val mesaj: String,
        val iz: String,
        val surumKodu: Int,
        val surumAdi: String
    ) {
        /**
         * Aynı çökme mi? — yığın izinin ilk satırlarına bakıyor.
         *
         * Tam eşitlik işe yaramaz: zaman damgaları ve bellek
         * adresleri değişiyor. İlk 3 kare (frame) pratikte aynı
         * hatayı tanımlamaya yetiyor.
         */
        val imza: String
            get() = runCatching {
                tur + "|" + iz.lineSequence()
                    .filter { it.trimStart().startsWith("at ") }
                    .take(3).joinToString(";") { it.trim() }
            }.getOrDefault(tur)
    }

    // ══════════════════════════════════════════════════════════
    // Kayıt
    // ══════════════════════════════════════════════════════════

    /**
     * Çökmeyi kaydeder. `App`'in `UncaughtExceptionHandler`'ından
     * çağrılıyor.
     *
     * ⚠️ Bu fonksiyon **süreç ölürken** çalışıyor. Kurallar:
     *   · `commit()` kullan, `apply()` YETİŞMEZ
     *   · Hiçbir şey fırlatma — zaten çöküyoruz, ikinci hata
     *     kaydı tamamen kaybettirir
     *   · Hızlı ol — sistem birkaç ms sonra süreci öldürüyor
     */
    fun kaydet(context: Context, parca: String, hata: Throwable) {
        try {
            val iz = android.util.Log.getStackTraceString(hata).take(IZ_SINIRI)
            val simdi = System.currentTimeMillis()
            val pref = p(context)

            // Geçmiş listesi
            val gecmis = try {
                JSONArray(pref.getString(K_GECMIS, "[]") ?: "[]")
            } catch (_: Exception) {
                JSONArray()
            }
            val yeni = JSONObject()
                .put("t", simdi)
                .put("p", parca)
                .put("tur", hata.javaClass.simpleName ?: "Exception")
                .put("m", (hata.message ?: "").take(300))
                .put("iz", iz)
                .put("vc", BuildConfig.VERSION_CODE)
                .put("vn", BuildConfig.VERSION_NAME)
            gecmis.put(yeni)

            // Tavanı aş: en eskileri at
            val kirpik = if (gecmis.length() > TAVAN) {
                val k = JSONArray()
                for (i in gecmis.length() - TAVAN until gecmis.length()) k.put(gecmis.get(i))
                k
            } else gecmis

            pref.edit()
                // v8.8 uyumluluğu: eski alanlar da yazılıyor,
                // DepolamaActivity onları okuyor
                .putString(K_SON, iz)
                .putLong(K_SON_TS, simdi)
                .putString(K_SON_THREAD, parca)
                .putString(K_GECMIS, kirpik.toString())
                // Yeni çökme = yeniden sor. Kullanıcı önceki
                // çökmede "sorma" demiş olsa bile bu yeni bir olay.
                .remove(K_SORULDU)
                .commit()   // ← apply() DEĞİL, süreç ölüyor
        } catch (_: Throwable) {
            // Sessiz: çökme kaydı sırasında çökmek en kötüsü
        }
    }

    // ══════════════════════════════════════════════════════════
    // Okuma
    // ══════════════════════════════════════════════════════════

    fun gecmis(context: Context): List<Kayit> = runCatching {
        val dizi = JSONArray(p(context).getString(K_GECMIS, "[]") ?: "[]")
        val liste = mutableListOf<Kayit>()
        for (i in 0 until dizi.length()) {
            val o = dizi.optJSONObject(i) ?: continue
            liste.add(
                Kayit(
                    zaman = o.optLong("t", 0L),
                    parca = o.optString("p", "?"),
                    tur = o.optString("tur", "Exception"),
                    mesaj = o.optString("m", ""),
                    iz = o.optString("iz", ""),
                    surumKodu = o.optInt("vc", 0),
                    surumAdi = o.optString("vn", "?")
                )
            )
        }
        liste.sortedByDescending { it.zaman }
    }.getOrElse {
        android.util.Log.w(TAG, "Geçmiş okunamadı", it)
        emptyList()
    }

    fun sonCokme(context: Context): Kayit? = gecmis(context).firstOrNull()

    fun cokmeVarMi(context: Context): Boolean = runCatching {
        !p(context).getString(K_SON, "").isNullOrBlank()
    }.getOrDefault(false)

    /**
     * Tekrar eden çökmeler — aynı imzaya sahip olanlar gruplanmış.
     *
     * @return imza → (kayıt, tekrar sayısı), çoktan aza
     */
    fun tekrarEdenler(context: Context): List<Pair<Kayit, Int>> = runCatching {
        gecmis(context).groupBy { it.imza }
            .map { (_, grup) -> grup.first() to grup.size }
            .sortedByDescending { it.second }
    }.getOrDefault(emptyList())

    // ══════════════════════════════════════════════════════════
    // Sorma mantığı
    // ══════════════════════════════════════════════════════════

    /**
     * Kullanıcıya sormalı mıyız?
     *
     * Üç koşul:
     *   1. Çökme kaydı var
     *   2. Bu çökme için daha önce sorulmadı
     *   3. Kullanıcı "bir daha sorma" dememiş
     *
     * Neden "bir daha sorma" seçeneği: sürekli çöken bir cihazda
     * her açılışta pencere görmek işkence olur. Kullanıcı
     * kapatabilmeli — ama Ayarlar'dan geri açabiliyor.
     */
    fun sormaliMi(context: Context): Boolean = runCatching {
        if (p(context).getBoolean(K_KAPALI, false)) return false
        val son = sonCokme(context) ?: return false
        if (son.zaman <= 0) return false
        val soruldu = p(context).getLong(K_SORULDU, 0L)
        // Bu çökmeden sonra sorulmuş mu?
        soruldu < son.zaman
    }.getOrDefault(false)

    /** "Sordum" işareti — aynı çökme için tekrar sorulmasın. */
    fun soruldu(context: Context) {
        runCatching {
            p(context).edit().putLong(K_SORULDU, System.currentTimeMillis()).apply()
        }
    }

    fun sormaKapali(context: Context): Boolean =
        runCatching { p(context).getBoolean(K_KAPALI, false) }.getOrDefault(false)

    fun sormaAyarla(context: Context, kapali: Boolean) {
        runCatching { p(context).edit().putBoolean(K_KAPALI, kapali).apply() }
    }

    // ══════════════════════════════════════════════════════════
    // Rapor üretimi
    // ══════════════════════════════════════════════════════════

    /**
     * Paylaşılacak metni üretir.
     *
     * ── Neden cihaz bilgisi şart ──
     * "Uygulama çöküyor" tek başına işe yaramaz. Üretici, Android
     * sürümü ve uygulama sürümü olmadan sorunu tekrar üretmek
     * imkânsız. Samsung bildirim sorunu (v7.88-v7.93) tam da bu
     * yüzden **altı sürüm** sürmüştü.
     *
     * ── Neden kullanım verisi eklenmiyor ──
     * [Kullanim] verisi hangi ekranları açtığını gösterir; çökme
     * raporu için gerekli değil ve gizlilik açısından fazladan
     * bilgi. Yalnızca **son açılan ekran** ekleniyor, o da
     * çökmenin nerede olduğunu daraltmaya yardım ettiği için.
     */
    fun rapor(context: Context, kayit: Kayit? = null): String {
        val k = kayit ?: sonCokme(context) ?: return ""
        val tekrar = runCatching {
            gecmis(context).count { it.imza == k.imza }
        }.getOrDefault(1)

        return buildString {
            appendLine("=== Günlük Asistan · Hata Raporu ===")
            appendLine("Uygulama : ${k.surumAdi} (${k.surumKodu})")
            appendLine("Android  : ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
            appendLine("Cihaz    : ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            appendLine("İş parçacığı: ${k.parca}")
            if (k.zaman > 0) {
                appendLine(
                    "Zaman    : " + runCatching {
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(k.zaman))
                    }.getOrDefault("?")
                )
            }
            if (tekrar > 1) appendLine("Tekrar   : bu hata $tekrar kez oldu")
            appendLine("Hata     : ${k.tur}${if (k.mesaj.isNotBlank()) " — ${k.mesaj}" else ""}")
            appendLine()
            appendLine(k.iz.take(6000))
        }
    }

    /** Tüm geçmişi tek metinde — geliştirici için detaylı rapor. */
    fun tumRapor(context: Context): String {
        val liste = gecmis(context)
        if (liste.isEmpty()) return ""
        return buildString {
            appendLine("=== Günlük Asistan · ${liste.size} Çökme Kaydı ===")
            appendLine("Android  : ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
            appendLine("Cihaz    : ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            appendLine()
            tekrarEdenler(context).forEach { (k, sayi) ->
                appendLine("──────────────────────────────")
                appendLine("${k.tur} · $sayi kez · v${k.surumAdi}")
                if (k.mesaj.isNotBlank()) appendLine("Mesaj: ${k.mesaj}")
                appendLine(k.iz.take(2500))
                appendLine()
            }
        }
    }

    /** Kısa özet — pencerede gösterilecek. */
    fun ozet(context: Context): String {
        val k = sonCokme(context) ?: return ""
        val ne = if (k.mesaj.isNotBlank()) "${k.tur}: ${k.mesaj.take(120)}" else k.tur
        val zaman = runCatching {
            SimpleDateFormat("d MMM HH:mm", Locale("tr", "TR")).format(Date(k.zaman))
        }.getOrDefault("")
        val tekrar = runCatching {
            gecmis(context).count { it.imza == k.imza }
        }.getOrDefault(1)
        return buildString {
            append(ne)
            if (zaman.isNotBlank()) append("\n$zaman")
            if (tekrar > 1) append(" · $tekrar kez")
        }
    }

    // ══════════════════════════════════════════════════════════
    // Temizleme
    // ══════════════════════════════════════════════════════════

    fun temizle(context: Context) {
        runCatching {
            p(context).edit()
                .remove(K_SON).remove(K_SON_TS).remove(K_SON_THREAD)
                .remove(K_GECMIS).remove(K_SORULDU)
                .apply()
        }
    }

    /**
     * v8.8 biçimindeki tek kaydı yeni geçmiş listesine taşır.
     *
     * v9.7'den güncelleyen kullanıcının elindeki çökme kaydı
     * kaybolmasın. Bir kez çalışıyor.
     */
    fun eskiKaydiTasi(context: Context) {
        runCatching {
            val pref = p(context)
            val gecmisVar = !pref.getString(K_GECMIS, "").isNullOrBlank()
            if (gecmisVar) return
            val eski = pref.getString(K_SON, "") ?: ""
            if (eski.isBlank()) return

            val dizi = JSONArray().put(
                JSONObject()
                    .put("t", pref.getLong(K_SON_TS, System.currentTimeMillis()))
                    .put("p", pref.getString(K_SON_THREAD, "?"))
                    .put("tur", ilkSatirdanTur(eski))
                    .put("m", "")
                    .put("iz", eski.take(IZ_SINIRI))
                    .put("vc", 0)
                    .put("vn", "9.7 öncesi")
            )
            pref.edit().putString(K_GECMIS, dizi.toString()).apply()
        }.onFailure { android.util.Log.w(TAG, "Eski kayıt taşınamadı", it) }
    }

    /**
     * Yığın izinin ilk satırından istisna türünü çıkarır.
     * "java.lang.NullPointerException: ..." → "NullPointerException"
     */
    fun ilkSatirdanTur(iz: String): String = runCatching {
        val ilk = iz.lineSequence().firstOrNull()?.trim() ?: return "Exception"
        val nokta = ilk.substringBefore(':').trim()
        nokta.substringAfterLast('.').ifBlank { "Exception" }
    }.getOrDefault("Exception")
}
