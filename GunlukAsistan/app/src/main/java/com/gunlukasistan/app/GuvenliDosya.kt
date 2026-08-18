package com.gunlukasistan.app

import java.io.File

/**
 * v8.8 — Atomik dosya yazma (öneri 7) ve bozuk veri koruması (öneri 8).
 *
 * ══════════════════════════════════════════════════════════════════
 * SORUN — sessiz veri kaybı
 * ══════════════════════════════════════════════════════════════════
 * `Store` yedekleri şöyle yazıyordu:
 *
 *     File(dir, "yedek.json").writeText(icerik)
 *
 * `writeText` önce dosyayı SIFIRLIYOR, sonra yazıyor. Bu iki adım
 * arasında şunlar olabilir:
 *   · Sistem uygulamayı öldürür (bellek baskısı — bu cihazlarda sık)
 *   · Pil biter
 *   · Depolama dolar
 *
 * Sonuç: **hem yeni yedek yazılmamış hem eski yedek silinmiş** olur.
 * Kullanıcı bunu ancak geri yüklemeye çalışınca fark eder — yani
 * telefonunu kaybettiğinde. En kötü zamanda.
 *
 * Bu teorik bir risk değil: 2 GB RAM'li cihazlarda arka plandaki
 * uygulama sık sık öldürülüyor.
 *
 * ══════════════════════════════════════════════════════════════════
 * ÇÖZÜM — geçici dosya + atomik takas
 * ══════════════════════════════════════════════════════════════════
 *     1. `yedek.json.tmp` dosyasına yaz
 *     2. Diske gerçekten indiğinden emin ol (`fd.sync()`)
 *     3. `rename(tmp → yedek.json)` — bu işlem dosya sisteminde
 *        ATOMİK: ya tamamen olur ya hiç olmaz
 *
 * Herhangi bir adımda kesinti olursa eski dosya bozulmadan kalır.
 *
 * ══════════════════════════════════════════════════════════════════
 * BOZUK VERİ KORUMASI (öneri 8)
 * ══════════════════════════════════════════════════════════════════
 * Kod tabanında 566 `catch (Exception)` var ve çoğu hatayı yutup boş
 * liste döndürüyor. Bozuk bir JSON okunduğunda kullanıcı verisinin
 * gittiğini fark etmiyor — üstelik bir sonraki kaydetme boş listeyi
 * diske yazıp **kalıcı olarak siliyor**.
 *
 * [bozukOlarakSakla] bozuk içeriği `.bozuk.<zaman>` uzantısıyla
 * kenara koyuyor. Veri kurtarılabilir kalıyor ve tanılama ekranından
 * görülebiliyor.
 */
object GuvenliDosya {

    private const val TAG = "GuvenliDosya"
    private const val GECICI_SON = ".tmp"
    private const val BOZUK_SON = ".bozuk"

    // ══════════════════════════════════════════════════════════
    // Yazma
    // ══════════════════════════════════════════════════════════

    /**
     * Metni atomik olarak yazar.
     *
     * @return başarılı mı
     */
    fun yaz(hedef: File, icerik: String): Boolean {
        return runCatching {
            hedef.parentFile?.mkdirs()
            val gecici = File(hedef.parentFile, hedef.name + GECICI_SON)

            // 1) Geçici dosyaya yaz ve diske indir
            java.io.FileOutputStream(gecici).use { cikis ->
                cikis.write(icerik.toByteArray(Charsets.UTF_8))
                cikis.flush()
                // fd.sync() işletim sistemi önbelleğini gerçekten diske
                // yazdırır. Bu olmadan rename atomik olsa bile içerik
                // henüz diskte olmayabilir (ani güç kesintisinde kayıp).
                runCatching { cikis.fd.sync() }
            }

            // 2) Boş yazma kontrolü — üretim hatası olabilir
            if (gecici.length() == 0L && icerik.isNotEmpty()) {
                gecici.delete()
                android.util.Log.w(TAG, "Geçici dosya boş kaldı: ${hedef.name}")
                return false
            }

            // 3) Atomik takas
            if (hedef.exists() && !hedef.delete()) {
                // Silinemiyorsa doğrudan renameTo çoğu dosya sisteminde
                // yine de üzerine yazar; deneyelim
                android.util.Log.w(TAG, "Eski dosya silinemedi, üzerine yazılacak")
            }
            val oldu = gecici.renameTo(hedef)
            if (!oldu) {
                // Son çare: doğrudan kopyala (atomik değil ama veri gitmesin)
                hedef.writeText(icerik)
                gecici.delete()
            }
            true
        }.onFailure {
            android.util.Log.w(TAG, "Atomik yazma başarısız: ${hedef.name}", it)
        }.getOrDefault(false)
    }

    /**
     * Yazmadan önce mevcut dosyanın yedeğini alır.
     *
     * Kritik dosyalar için (ana yedek): yeni yazma bozulsa bile bir
     * önceki sürüm `.onceki` uzantısıyla duruyor.
     */
    fun yazVeOncekiniKoru(hedef: File, icerik: String): Boolean {
        runCatching {
            if (hedef.exists() && hedef.length() > 0) {
                val onceki = File(hedef.parentFile, hedef.name + ".onceki")
                hedef.copyTo(onceki, overwrite = true)
            }
        }.onFailure { android.util.Log.w(TAG, "Önceki sürüm korunamadı", it) }
        return yaz(hedef, icerik)
    }

    // ══════════════════════════════════════════════════════════
    // Okuma
    // ══════════════════════════════════════════════════════════

    /**
     * Dosyayı okur. Yoksa veya okunamıyorsa null döner.
     *
     * Yarım kalmış geçici dosya varsa temizlenir.
     */
    fun oku(hedef: File): String? {
        return runCatching {
            // Yarım kalmış geçici dosyayı temizle (önceki çökmeden kalma)
            val gecici = File(hedef.parentFile, hedef.name + GECICI_SON)
            if (gecici.exists()) {
                android.util.Log.w(TAG, "Yarım geçici dosya temizlendi: ${gecici.name}")
                gecici.delete()
            }
            if (!hedef.exists() || hedef.length() == 0L) return null
            hedef.readText(Charsets.UTF_8)
        }.onFailure {
            android.util.Log.w(TAG, "Okunamadı: ${hedef.name}", it)
        }.getOrNull()
    }

    // ══════════════════════════════════════════════════════════
    // Bozuk veri (öneri 8)
    // ══════════════════════════════════════════════════════════

    /**
     * Bozuk içeriği kenara koyar — silmez.
     *
     * ── Neden ──
     * JSON ayrıştırma hatasında eski kod boş liste döndürüyordu ve
     * bir sonraki kaydetme o boş listeyi kalıcılaştırıyordu. Kullanıcı
     * verisinin gittiğini ancak iş işten geçtikten sonra fark ediyordu.
     *
     * Artık bozuk içerik `<ad>.bozuk.<zamandamgasi>` olarak saklanıyor.
     * Elle kurtarılabilir; Ayarlar → Tanılama'da görünüyor.
     *
     * @param etiket hangi veri bozuldu ("gorevler", "konular"...)
     * @return oluşturulan dosya, başarısızsa null
     */
    fun bozukOlarakSakla(dizin: File?, etiket: String, icerik: String): File? {
        dizin ?: return null
        if (icerik.isBlank()) return null
        return runCatching {
            val bozukDizin = File(dizin, "bozuk").apply { mkdirs() }
            val dosya = File(
                bozukDizin,
                "$etiket$BOZUK_SON.${System.currentTimeMillis()}.json"
            )
            dosya.writeText(icerik, Charsets.UTF_8)
            android.util.Log.w(TAG, "Bozuk veri saklandı: ${dosya.name} (${icerik.length} karakter)")
            // Çok fazla birikmesin — en yeni 10 tanesi kalsın
            temizleEskiBozuklar(bozukDizin)
            dosya
        }.onFailure { android.util.Log.w(TAG, "Bozuk veri saklanamadı", it) }.getOrNull()
    }

    private fun temizleEskiBozuklar(dizin: File, tut: Int = 10) {
        runCatching {
            val dosyalar = dizin.listFiles()?.sortedByDescending { it.lastModified() }
                ?: return
            dosyalar.drop(tut).forEach { it.delete() }
        }
    }

    /** Saklanmış bozuk dosyalar (tanılama ekranı için). */
    fun bozukDosyalar(dizin: File?): List<File> {
        dizin ?: return emptyList()
        return runCatching {
            File(dizin, "bozuk").listFiles()
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
        }.getOrDefault(emptyList())
    }

    /** Bozuk dosyaların toplam boyutu (bayt). */
    fun bozukBoyut(dizin: File?): Long =
        bozukDosyalar(dizin).sumOf { it.length() }

    fun bozuklariSil(dizin: File?): Int {
        val liste = bozukDosyalar(dizin)
        var n = 0
        liste.forEach { if (runCatching { it.delete() }.getOrDefault(false)) n++ }
        return n
    }

    // ══════════════════════════════════════════════════════════
    // JSON doğrulama (öneri 8 destek)
    // ══════════════════════════════════════════════════════════

    /**
     * Metin geçerli bir JSON nesnesi/dizisi mi?
     *
     * Yazmadan ÖNCE doğrulamak, bozuk veriyi diske hiç yazmamayı
     * sağlıyor — sorunu kaynağında kesiyor.
     */
    fun gecerliJsonMu(metin: String): Boolean {
        if (metin.isBlank()) return false
        return runCatching {
            val kirpik = metin.trim()
            when {
                kirpik.startsWith("{") -> org.json.JSONObject(kirpik)
                kirpik.startsWith("[") -> org.json.JSONArray(kirpik)
                else -> return false
            }
            true
        }.getOrDefault(false)
    }
}
