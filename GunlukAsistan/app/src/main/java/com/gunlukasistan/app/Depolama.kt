package com.gunlukasistan.app

import android.content.Context
import java.io.File

/**
 * v8.8 — Depolama yönetimi (öneri 9, 10).
 *
 * ══════════════════════════════════════════════════════════════════
 * SORUN
 * ══════════════════════════════════════════════════════════════════
 * **Öneri 9 — yetim dosyalar:** `Kanit` fotoğrafları kaydediyor.
 * İlgili görev silinince fotoğraf diskte kalıyor. Aynı şey not
 * resimleri, indirilen PDF'ler ve AI önbelleği için de geçerli.
 * Uygulama yıllar içinde sessizce yüzlerce MB biriktiriyor.
 *
 * **Öneri 10 — görünürlük yok:** Kullanıcı uygulamanın kaç MB
 * tuttuğunu bilmiyor. Android'in "Uygulama bilgisi" ekranı tek bir
 * toplam gösteriyor; neyin yer kapladığı belli değil. Telefonu
 * dolan kullanıcının tek seçeneği uygulamayı silmek oluyor.
 *
 * ══════════════════════════════════════════════════════════════════
 * ÇÖZÜM
 * ══════════════════════════════════════════════════════════════════
 * Kategori bazlı ölçüm + kategori bazlı temizlik. Kullanıcı neyin ne
 * kadar yer kapladığını görüyor ve **sadece istediğini** siliyor.
 *
 * ── Güvenlik ──
 * Silme işlemleri asla kullanıcı verisine dokunmuyor. Yalnız
 * yeniden üretilebilir şeyler siliniyor: önbellek, yetim dosya,
 * eski yedek kopyaları. Görev/not/konu metni hiç ellenmiyor.
 */
object Depolama {

    private const val TAG = "Depolama"

    /** Bir depolama kategorisi. */
    data class Kalem(
        val kod: String,
        val baslikRes: Int,
        val aciklamaRes: Int,
        val bayt: Long,
        val dosyaSayisi: Int,
        /** Temizlenebilir mi? (kullanıcı verisi temizlenemez) */
        val temizlenebilir: Boolean,
        val simge: String
    )

    // ══════════════════════════════════════════════════════════
    // Ölçüm
    // ══════════════════════════════════════════════════════════

    /** Tüm kategorileri ölçer. Arka planda çağrılmalı — diski gezer. */
    fun olc(context: Context): List<Kalem> {
        val liste = mutableListOf<Kalem>()

        // 1) Kanıt fotoğrafları
        val kanit = klasorBilgi(kanitDizini(context))
        liste.add(
            Kalem(
                "kanit", R.string.dp_kanit, R.string.dp_kanit_alt,
                kanit.first, kanit.second, temizlenebilir = false, simge = "📷"
            )
        )

        // 2) Not resimleri
        val notResim = klasorBilgi(File(context.filesDir, "not_resim"))
        liste.add(
            Kalem(
                "not_resim", R.string.dp_not_resim, R.string.dp_not_resim_alt,
                notResim.first, notResim.second, temizlenebilir = false, simge = "🖼"
            )
        )

        // 3) PDF ve kitaplar
        val pdf = klasorBilgi(File(context.filesDir, "pdf")) +
            klasorBilgi(File(context.getExternalFilesDir(null), "pdf"))
        liste.add(
            Kalem(
                "pdf", R.string.dp_pdf, R.string.dp_pdf_alt,
                pdf.first, pdf.second, temizlenebilir = false, simge = "📕"
            )
        )

        // 4) AI önbelleği — güvenle silinebilir
        val aiBayt = prefBoyut(context, "ai_onbellek_v1") +
            prefBoyut(context, "ai_model_cache")
        liste.add(
            Kalem(
                "ai_onbellek", R.string.dp_ai, R.string.dp_ai_alt,
                aiBayt, 0, temizlenebilir = true, simge = "🤖"
            )
        )

        // 5) Üretilmiş konu anlatımları — büyük olabilir, yeniden üretilebilir
        val anlatim = prefBoyut(context, "konu_anlatim_v1")
        liste.add(
            Kalem(
                "anlatim", R.string.dp_anlatim, R.string.dp_anlatim_alt,
                anlatim, 0, temizlenebilir = true, simge = "📖"
            )
        )

        // 6) Yedekler (rotasyon kopyaları dahil)
        val yedek = yedekBilgi(context)
        liste.add(
            Kalem(
                "yedek", R.string.dp_yedek, R.string.dp_yedek_alt,
                yedek.first, yedek.second, temizlenebilir = true, simge = "💾"
            )
        )

        // 7) Bozuk veri arşivi (v8.8 · öneri 8)
        val dis = context.getExternalFilesDir(null) ?: context.filesDir
        val bozuk = GuvenliDosya.bozukDosyalar(dis)
        liste.add(
            Kalem(
                "bozuk", R.string.dp_bozuk, R.string.dp_bozuk_alt,
                bozuk.sumOf { it.length() }, bozuk.size,
                temizlenebilir = true, simge = "⚠"
            )
        )

        // 8) Sistem önbelleği
        val onbellek = klasorBilgi(context.cacheDir)
        liste.add(
            Kalem(
                "onbellek", R.string.dp_onbellek, R.string.dp_onbellek_alt,
                onbellek.first, onbellek.second, temizlenebilir = true, simge = "🧹"
            )
        )

        return liste.sortedByDescending { it.bayt }
    }

    /** Toplam kullanım. */
    fun toplam(kalemler: List<Kalem>): Long = kalemler.sumOf { it.bayt }

    /** Temizlenebilir toplam. */
    fun temizlenebilirToplam(kalemler: List<Kalem>): Long =
        kalemler.filter { it.temizlenebilir }.sumOf { it.bayt }

    // ══════════════════════════════════════════════════════════
    // Temizlik
    // ══════════════════════════════════════════════════════════

    /**
     * Bir kategoriyi temizler.
     *
     * @return silinen bayt
     */
    fun temizle(context: Context, kod: String): Long = runCatching {
        when (kod) {
            "ai_onbellek" -> {
                val once = prefBoyut(context, "ai_onbellek_v1")
                context.getSharedPreferences("ai_onbellek_v1", Context.MODE_PRIVATE)
                    .edit().clear().apply()
                once
            }
            "anlatim" -> {
                val once = prefBoyut(context, "konu_anlatim_v1")
                context.getSharedPreferences("konu_anlatim_v1", Context.MODE_PRIVATE)
                    .edit().clear().apply()
                once
            }
            "yedek" -> eskiYedekleriSil(context)
            "bozuk" -> {
                val dis = context.getExternalFilesDir(null) ?: context.filesDir
                val once = GuvenliDosya.bozukBoyut(dis)
                GuvenliDosya.bozuklariSil(dis)
                once
            }
            "onbellek" -> {
                val once = klasorBilgi(context.cacheDir).first
                context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
                once
            }
            else -> 0L
        }
    }.onFailure { android.util.Log.w(TAG, "temizle($kod)", it) }.getOrDefault(0L)

    /**
     * v8.8 · Öneri 9 — Yetim dosya toplayıcı.
     *
     * Hiçbir kayda bağlı olmayan fotoğrafları siler.
     *
     * ── Neden dikkatli olmak gerekiyor ──
     * Bir dosyanın "yetim" olduğuna karar vermek risklidir: kayıt
     * henüz yazılmamış olabilir (yarış durumu). Bu yüzden yalnızca
     * **7 günden eski** ve hiçbir kayıtta adı geçmeyen dosyalar
     * siliniyor.
     *
     * @return (silinen dosya sayısı, kazanılan bayt)
     */
    fun yetimleriTopla(context: Context): Pair<Int, Long> {
        var sayi = 0
        var bayt = 0L
        runCatching {
            val esik = System.currentTimeMillis() - 7L * 86_400_000L

            // Kayıtlarda geçen tüm dosya adlarını topla.
            //
            // Not: ilk yazımda `Kanit.tumYollar()` ve `Note.imagePath`
            // diye API'ler varsaymıştım — İKİSİ DE YOK. Gerçek adlar
            // aşağıdaki gibi; kodu okuyup düzelttim. Var olmayan bir
            // API'ye dayanan temizlik, dosyaları yanlışlıkla yetim
            // sayıp SİLERDİ.
            val kullanilan = HashSet<String>()
            runCatching {
                // Aktif kanıt kayıtları
                Kanit.hepsi(context).forEach { k ->
                    if (k.yol.isNotBlank()) kullanilan.add(File(k.yol).name)
                }
                // Arşivdeki geçmiş kayıtlar da fotoğrafı tutuyor
                Kanit.gecmis(context).forEach { g ->
                    if (g.yol.isNotBlank()) kullanilan.add(File(g.yol).name)
                }
            }.onFailure {
                // Kayıtlar okunamadıysa HİÇBİR ŞEY SİLME — yoksa
                // "kayıt yok, hepsi yetim" sanıp tümünü silerdik.
                android.util.Log.w(TAG, "Kanıt kayıtları okunamadı, temizlik iptal", it)
                return 0 to 0L
            }
            runCatching {
                Store.loadNotes(context).forEach { n ->
                    if (n.image.isNotBlank()) kullanilan.add(File(n.image).name)
                }
            }

            // Kanıt klasörünü tara
            kanitDizini(context)?.listFiles()?.forEach { f ->
                if (!f.isFile) return@forEach
                if (f.lastModified() > esik) return@forEach       // çok yeni
                if (f.name in kullanilan) return@forEach          // kullanımda
                val boyut = f.length()
                if (f.delete()) { sayi++; bayt += boyut }
            }
        }.onFailure { android.util.Log.w(TAG, "yetimleriTopla", it) }
        return sayi to bayt
    }

    // ══════════════════════════════════════════════════════════
    // Yardımcılar
    // ══════════════════════════════════════════════════════════

    private fun kanitDizini(context: Context): File? = runCatching {
        File(context.filesDir, "kanit").takeIf { it.exists() }
    }.getOrNull()

    /** (toplamBayt, dosyaSayısı) */
    private fun klasorBilgi(dizin: File?): Pair<Long, Int> {
        dizin ?: return 0L to 0
        if (!dizin.exists()) return 0L to 0
        return runCatching {
            var bayt = 0L
            var adet = 0
            dizin.walkTopDown().forEach {
                if (it.isFile) { bayt += it.length(); adet++ }
            }
            bayt to adet
        }.getOrDefault(0L to 0)
    }

    private operator fun Pair<Long, Int>.plus(o: Pair<Long, Int>) =
        (first + o.first) to (second + o.second)

    /** SharedPreferences XML dosyasının disk boyutu. */
    private fun prefBoyut(context: Context, ad: String): Long = runCatching {
        File(File(context.filesDir.parentFile, "shared_prefs"), "$ad.xml").length()
    }.getOrDefault(0L)

    private fun yedekBilgi(context: Context): Pair<Long, Int> {
        val dis = context.getExternalFilesDir(null) ?: context.filesDir
        return runCatching {
            var bayt = 0L
            var adet = 0
            dis.listFiles()?.forEach { f ->
                if (f.isFile && (f.name.endsWith(".json") || f.name.contains("yedek"))) {
                    bayt += f.length(); adet++
                }
            }
            bayt to adet
        }.getOrDefault(0L to 0)
    }

    /** Ana yedek dışındaki rotasyon kopyalarını siler. */
    private fun eskiYedekleriSil(context: Context): Long {
        val dis = context.getExternalFilesDir(null) ?: context.filesDir
        var silinen = 0L
        runCatching {
            dis.listFiles()?.forEach { f ->
                // Ana yedek ve son kopyası korunuyor
                if (!f.isFile) return@forEach
                val koru = f.name == "gunluk-asistan-yedek.json" ||
                    f.name.endsWith(".onceki")
                if (koru) return@forEach
                if (f.name.contains("yedek") && f.name.endsWith(".json")) {
                    val b = f.length()
                    if (f.delete()) silinen += b
                }
            }
        }
        return silinen
    }

    // ══════════════════════════════════════════════════════════
    // Biçimlendirme
    // ══════════════════════════════════════════════════════════

    /** 1536000 → "1,5 MB" */
    fun bicimle(bayt: Long): String = when {
        bayt <= 0 -> "0 B"
        bayt < 1024 -> "$bayt B"
        bayt < 1024 * 1024 -> String.format(java.util.Locale("tr"), "%.0f KB", bayt / 1024.0)
        bayt < 1024L * 1024 * 1024 ->
            String.format(java.util.Locale("tr"), "%.1f MB", bayt / (1024.0 * 1024))
        else ->
            String.format(java.util.Locale("tr"), "%.2f GB", bayt / (1024.0 * 1024 * 1024))
    }
}
