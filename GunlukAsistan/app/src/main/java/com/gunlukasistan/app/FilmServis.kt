package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection

/**
 * v7.49 — Film/dizi verisi getiren servis.
 *
 * ── İki kaynak ──
 *  1. TMDb API — anahtar varsa: gerçek puan, poster, oyuncu, platform
 *  2. Yapay zekâ — anahtar yoksa: Gemini'den öneri listesi
 *
 * Kullanıcı "ikisi birden" istedi. TMDb daha doğru; ama anahtar almak
 * istemeyen kullanıcı da boş ekranla karşılaşmasın diye AI yedeği var.
 *
 * ── Neden TMDb ──
 * IMDb'nin ücretsiz halka açık API'si yok. TMDb ücretsiz, Türkçe özet
 * veriyor ve IMDb kimliğini de döndürüyor — IMDb sayfasına link kurulabiliyor.
 * "IMDb puanı" olarak TMDb oy ortalaması gösteriliyor; ikisi birbirine
 * çok yakın olur ama aynı değildir, ekranda bu dürüstçe belirtiliyor.
 */
object FilmServis {

    private const val TAG = "FilmServis"
    private const val TMDB = "https://api.themoviedb.org/3"
    private const val POSTER = "https://image.tmdb.org/t/p/w300"

    class Sonuc(val ok: Boolean, val mesaj: String, val liste: List<FilmStore.Yapim>)

    // ═══════════════════════════════════════════════════════════════
    // GİRİŞ NOKTASI
    // ═══════════════════════════════════════════════════════════════

    /**
     * Günün önerilerini getirir.
     * Arka plan iş parçacığından çağrılmalıdır.
     */
    fun gununOnerileri(context: Context, adet: Int = 8): Sonuc {
        // v7.50: TMDb tamamen isteğe bağlı. Anahtar yoksa ya da çalışmazsa
        // sessizce yapay zekâya düşülür — kullanıcı hata görmez.
        if (FilmStore.tmdbVarMi(context)) {
            val t = tmdbKesfet(context, adet)
            if (t.ok && t.liste.isNotEmpty()) return t
            android.util.Log.w(TAG, "TMDb başarısız, yapay zekâya düşülüyor: " + t.mesaj)
        }
        return aiOner(context, adet)
    }

    /** Ad ile arama — TMDb varsa oradan, yoksa yapay zekâdan. */
    fun ara(context: Context, sorgu: String): Sonuc {
        if (sorgu.isBlank()) return Sonuc(false, "", emptyList())
        if (FilmStore.tmdbVarMi(context)) {
            val t = tmdbAra(context, sorgu)
            if (t.ok && t.liste.isNotEmpty()) return t
        }
        return aiAra(context, sorgu)
    }

    // ═══════════════════════════════════════════════════════════════
    // TMDb
    // ═══════════════════════════════════════════════════════════════

    private fun http(url: String): String? = try {
        val conn = (URL(url).openConnection() as HttpsURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15000
            readTimeout = 25000
        }
        val kod = conn.responseCode
        val akis = if (kod in 200..299) conn.inputStream else conn.errorStream
        val cevap = BufferedReader(InputStreamReader(akis, Charsets.UTF_8)).use { it.readText() }
        conn.disconnect()
        if (kod in 200..299) cevap else null
    } catch (e: Exception) {
        android.util.Log.w(TAG, "HTTP başarısız", e)
        null
    }

    /**
     * Popüler/keşfet listesi. Sevilen türler varsa filtrelenir.
     * Her gün farklı sonuç gelsin diye sayfa numarası güne göre değişir.
     */
    private fun tmdbKesfet(context: Context, adet: Int): Sonuc {
        val key = FilmStore.tmdbAnahtar(context)
        val tur = FilmStore.turTercihi(context)
        // Gün numarasına göre sayfa — her gün farklı öneri
        val sayfa = (java.util.Calendar.getInstance()
            .get(java.util.Calendar.DAY_OF_YEAR) % 5) + 1

        val hedefler = when (tur) {
            "film" -> listOf("movie")
            "dizi" -> listOf("tv")
            else -> listOf("movie", "tv")
        }

        val toplam = mutableListOf<FilmStore.Yapim>()
        hedefler.forEach { medya ->
            val url = TMDB + "/discover/" + medya +
                "?api_key=" + key +
                "&language=tr-TR&sort_by=popularity.desc" +
                "&vote_count.gte=200&page=" + sayfa
            val ham = http(url) ?: return@forEach
            try {
                val dizi = JSONObject(ham).optJSONArray("results") ?: return@forEach
                for (i in 0 until minOf(dizi.length(), adet)) {
                    val o = dizi.optJSONObject(i) ?: continue
                    toplam.add(tmdbCoz(o, medya))
                }
            } catch (e: Exception) {
                android.util.Log.w(TAG, "TMDb ayrıştırılamadı", e)
            }
        }

        if (toplam.isEmpty()) {
            return Sonuc(false, context.getString(R.string.fl_err_tmdb), emptyList())
        }
        // Karıştır, istenen adede indir
        return Sonuc(true, "", toplam.shuffled().take(adet))
    }

    private fun tmdbAra(context: Context, sorgu: String): Sonuc {
        val key = FilmStore.tmdbAnahtar(context)
        val url = TMDB + "/search/multi?api_key=" + key +
            "&language=tr-TR&query=" + URLEncoder.encode(sorgu, "UTF-8")
        val ham = http(url) ?: return Sonuc(false, context.getString(R.string.fl_err_tmdb), emptyList())
        return try {
            val dizi = JSONObject(ham).optJSONArray("results")
                ?: return Sonuc(false, "", emptyList())
            val liste = mutableListOf<FilmStore.Yapim>()
            for (i in 0 until minOf(dizi.length(), 12)) {
                val o = dizi.optJSONObject(i) ?: continue
                val medya = o.optString("media_type")
                if (medya != "movie" && medya != "tv") continue
                liste.add(tmdbCoz(o, medya))
            }
            Sonuc(true, "", liste)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Arama ayrıştırılamadı", e)
            Sonuc(false, context.getString(R.string.fl_err_tmdb), emptyList())
        }
    }

    private fun tmdbCoz(o: JSONObject, medya: String): FilmStore.Yapim {
        val dizi = medya == "tv"
        val ad = if (dizi) o.optString("name") else o.optString("title")
        val orijinal = if (dizi) o.optString("original_name") else o.optString("original_title")
        val tarih = if (dizi) o.optString("first_air_date") else o.optString("release_date")
        val poster = o.optString("poster_path")

        return FilmStore.Yapim(
            id = System.nanoTime(),
            ad = ad.ifBlank { orijinal },
            orijinalAd = orijinal,
            tur = if (dizi) "dizi" else "film",
            yil = tarih.take(4),
            puan = o.optDouble("vote_average", 0.0),
            oySayisi = o.optInt("vote_count"),
            ozet = o.optString("overview"),
            turler = turAdlari(o.optJSONArray("genre_ids")),
            posterUrl = if (poster.isBlank()) "" else POSTER + poster,
            tmdbId = o.optInt("id")
        )
    }

    /** TMDb tür kimliklerini Türkçe adlara çevirir. */
    private fun turAdlari(dizi: JSONArray?): String {
        if (dizi == null) return ""
        val harita = mapOf(
            28 to "Aksiyon", 12 to "Macera", 16 to "Animasyon", 35 to "Komedi",
            80 to "Suç", 99 to "Belgesel", 18 to "Dram", 10751 to "Aile",
            14 to "Fantastik", 36 to "Tarih", 27 to "Korku", 10402 to "Müzik",
            9648 to "Gizem", 10749 to "Romantik", 878 to "Bilim Kurgu",
            53 to "Gerilim", 10752 to "Savaş", 37 to "Western",
            10759 to "Aksiyon", 10762 to "Çocuk", 10763 to "Haber",
            10764 to "Realite", 10765 to "Bilim Kurgu", 10766 to "Pembe Dizi",
            10767 to "Talk Show", 10768 to "Savaş"
        )
        val adlar = mutableListOf<String>()
        for (i in 0 until dizi.length()) {
            harita[dizi.optInt(i)]?.let { if (it !in adlar) adlar.add(it) }
        }
        return adlar.take(3).joinToString(", ")
    }

    /**
     * Bir yapımın ayrıntısı: süre, yönetmen, oyuncular, IMDb kimliği,
     * izlenebileceği platformlar.
     */
    fun detayGetir(context: Context, y: FilmStore.Yapim): FilmStore.Yapim {
        if (!FilmStore.tmdbVarMi(context) || y.tmdbId == 0) return y
        val key = FilmStore.tmdbAnahtar(context)
        val medya = if (y.tur == "dizi") "tv" else "movie"

        return try {
            val url = TMDB + "/" + medya + "/" + y.tmdbId +
                "?api_key=" + key + "&language=tr-TR" +
                "&append_to_response=credits,external_ids,watch/providers"
            val ham = http(url) ?: return y
            val o = JSONObject(ham)

            // Süre
            val sure = if (medya == "movie") {
                val dk = o.optInt("runtime")
                if (dk > 0) dk.toString() + " dk" else ""
            } else {
                val sezon = o.optInt("number_of_seasons")
                val bolum = o.optInt("number_of_episodes")
                if (sezon > 0) sezon.toString() + " sezon · " + bolum + " bölüm" else ""
            }

            // Yönetmen ve oyuncular
            val credits = o.optJSONObject("credits")
            val yonetmen = credits?.optJSONArray("crew")?.let { crew ->
                (0 until crew.length())
                    .mapNotNull { crew.optJSONObject(it) }
                    .firstOrNull { it.optString("job") == "Director" }
                    ?.optString("name").orEmpty()
            }.orEmpty()

            val oyuncular = credits?.optJSONArray("cast")?.let { cast ->
                (0 until minOf(cast.length(), 4))
                    .mapNotNull { cast.optJSONObject(it)?.optString("name") }
                    .joinToString(", ")
            }.orEmpty()

            // Türler (ayrıntıda tam adlarıyla gelir)
            val turDizi = o.optJSONArray("genres")
            val turler = if (turDizi != null) {
                (0 until minOf(turDizi.length(), 3))
                    .mapNotNull { turDizi.optJSONObject(it)?.optString("name") }
                    .joinToString(", ")
            } else y.turler

            // Türkiye'de hangi platformlarda
            val platformlar = try {
                val tr = o.optJSONObject("watch/providers")
                    ?.optJSONObject("results")?.optJSONObject("TR")
                val adlar = linkedSetOf<String>()
                listOf("flatrate", "free", "rent", "buy").forEach { anahtar ->
                    tr?.optJSONArray(anahtar)?.let { d ->
                        for (i in 0 until d.length()) {
                            d.optJSONObject(i)?.optString("provider_name")
                                ?.takeIf { it.isNotBlank() }?.let { adlar.add(it) }
                        }
                    }
                }
                adlar.take(5).joinToString(", ")
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Platform bilgisi alınamadı", e)
                ""
            }

            y.copy(
                sure = sure,
                yonetmen = yonetmen,
                oyuncular = oyuncular,
                turler = turler.ifBlank { y.turler },
                imdbId = o.optJSONObject("external_ids")?.optString("imdb_id").orEmpty(),
                platformlar = platformlar,
                ozet = o.optString("overview").ifBlank { y.ozet }
            )
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Detay alınamadı", e)
            y
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // YAPAY ZEKÂ YEDEĞİ
    // ═══════════════════════════════════════════════════════════════

    private fun aiHazirMi(context: Context): String? {
        if (!AiSettings.isOnlineMode(context)) {
            return context.getString(R.string.ai_err_offline_mode)
        }
        if (!AiClient.isOnline(context)) return context.getString(R.string.ai_err_no_net)
        if (AiSettings.getApiKey(context).isBlank() &&
            AiSettings.anahtarliSaglayicilar(context).isEmpty()
        ) return context.getString(R.string.fl_err_nokey)
        return null
    }

    private fun aiOner(context: Context, adet: Int): Sonuc {
        aiHazirMi(context)?.let { return Sonuc(false, it, emptyList()) }

        val tur = when (FilmStore.turTercihi(context)) {
            "film" -> "film"
            "dizi" -> "dizi"
            else -> "film ve dizi"
        }
        val sevilen = FilmStore.sevilenTurler(context)
        val zevkNotu = if (sevilen.isBlank()) "" else
            "\nKullanıcının sevdiği türler: " + sevilen + " — bunlara ağırlık ver."

        // Her gün farklı öneri gelsin diye tohum
        val tohum = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)

        val istem = """$adet adet $tur öner (liste no: $tohum).
İzlemeye değer, IMDb 7.0 üzeri yapımlar. Popüler ve az bilinen karışık olsun.$zevkNotu

SADECE bu JSON'u yaz, başka hiçbir şey yazma:
{"sonuclar":[
{"ad":"Türkçe ad","orijinal":"Original Title","tur":"film","yil":"2024","puan":8.2,"ozet":"2-3 cümle özet","turler":"Dram, Gerilim","sure":"122 dk","yonetmen":"Ad Soyad","oyuncular":"Ad1, Ad2","platform":"Netflix"}
]}

Kurallar: puan = IMDb puanı (0-10). tur = "film" veya "dizi". ozet Türkçe ve
spoilersız. platform = Türkiye'de yasal izleme yeri (Netflix/Prime Video/
Disney+/BluTV/Exxen/MUBI/TOD), emin değilsen boş bırak. Dizide sure yerine
"3 sezon" yaz. Gerçek yapımlar olsun, uydurma. Kod bloğu işareti kullanma."""

        // v7.50: sadeIstek — sistem istemi yok, bütçe 6144 (8 film ~850 token +
        // düşünme payı). chat() ile 1200 token'da yanıt yarıda kesiliyordu.
        val sonuc = AiClient.sadeIstek(context, istem, 6144)
        if (!sonuc.ok) return Sonuc(false, sonuc.text, emptyList())
        val liste = aiAyristir(sonuc.text)
        return if (liste.isEmpty()) {
            Sonuc(false, context.getString(R.string.fl_err_parse), emptyList())
        } else {
            Sonuc(true, "", liste)
        }
    }

    private fun aiAra(context: Context, sorgu: String): Sonuc {
        aiHazirMi(context)?.let { return Sonuc(false, it, emptyList()) }

        val istem = """"$sorgu" ile ilgili film/dizi bilgisi ver. Tam eşleşme varsa onu,
yoksa benzer 3-5 yapım öner.

Yanıtını SADECE şu JSON biçiminde ver:
{"sonuclar":[{"ad":"Türkçe ad","orijinal":"Original Title","tur":"film","yil":"2024","puan":8.2,"ozet":"özet","turler":"Dram","sure":"122 dk","yonetmen":"Ad","oyuncular":"Ad1, Ad2","platform":"Netflix"}]}

"puan" IMDb puanı. UYDURMA. JSON dışında bir şey yazma."""

        val sonuc = AiClient.sadeIstek(context, istem, 4096)
        if (!sonuc.ok) return Sonuc(false, sonuc.text, emptyList())
        val liste = aiAyristir(sonuc.text)
        return if (liste.isEmpty()) {
            Sonuc(false, context.getString(R.string.fl_err_parse), emptyList())
        } else Sonuc(true, "", liste)
    }

    /** Savunmacı ayrıştırma — model JSON'u bozarsa çökmesin. */
    private fun aiAyristir(ham: String): List<FilmStore.Yapim> {
        val liste = mutableListOf<FilmStore.Yapim>()
        try {
            val (temiz, _) = AsistanKomut.ayikla(ham)
            var s = temiz.trim().ifBlank { ham.trim() }
            if (s.startsWith("```")) {
                s = s.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            }
            val bas = s.indexOf('{')
            val son = s.lastIndexOf('}')
            if (bas < 0 || son <= bas) return liste

            // v7.50: Yanıt token sınırında kesilmişse JSON yarım kalır.
            // Önce normal ayrıştırma denenir; olmazsa tek tek nesne kurtarılır.
            val dizi = try {
                JSONObject(s.substring(bas, son + 1)).optJSONArray("sonuclar")
            } catch (e: Exception) {
                android.util.Log.w(TAG, "JSON bozuk, parça kurtarma deneniyor", e)
                null
            } ?: return parcaKurtar(s)
            for (i in 0 until dizi.length()) {
                val o = dizi.optJSONObject(i) ?: continue
                val ad = o.optString("ad").trim()
                if (ad.isBlank()) continue
                liste.add(
                    FilmStore.Yapim(
                        id = System.nanoTime() + i,
                        ad = ad,
                        orijinalAd = o.optString("orijinal").trim(),
                        tur = if (o.optString("tur").contains("dizi", true)) "dizi" else "film",
                        yil = o.optString("yil").trim(),
                        puan = o.optDouble("puan", 0.0).coerceIn(0.0, 10.0),
                        ozet = o.optString("ozet").trim(),
                        turler = o.optString("turler").trim(),
                        sure = o.optString("sure").trim(),
                        yonetmen = o.optString("yonetmen").trim(),
                        oyuncular = o.optString("oyuncular").trim(),
                        platformlar = o.optString("platform").trim()
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "AI yanıtı ayrıştırılamadı", e)
        }
        return liste
    }

    /**
     * v7.50: Yarım kalan JSON'dan tam olan film nesnelerini kurtarır.
     *
     * Model token sınırına takılırsa son nesne yarıda kalır ve tüm dizi
     * ayrıştırılamaz. Bu durumda kullanıcıya boş ekran göstermek yerine
     * sağlam olan filmleri gösteriyoruz.
     */
    private fun parcaKurtar(ham: String): List<FilmStore.Yapim> {
        val liste = mutableListOf<FilmStore.Yapim>()
        try {
            // {"ad": ... } biçimindeki tam nesneleri tek tek yakala
            val desen = Regex("""\{[^{}]*"ad"\s*:[^{}]*\}""")
            desen.findAll(ham).forEachIndexed { i, m ->
                try {
                    val o = JSONObject(m.value)
                    val ad = o.optString("ad").trim()
                    if (ad.isBlank()) return@forEachIndexed
                    liste.add(
                        FilmStore.Yapim(
                            id = System.nanoTime() + i,
                            ad = ad,
                            orijinalAd = o.optString("orijinal").trim(),
                            tur = if (o.optString("tur").contains("dizi", true)) "dizi" else "film",
                            yil = o.optString("yil").trim(),
                            puan = o.optDouble("puan", 0.0).coerceIn(0.0, 10.0),
                            ozet = o.optString("ozet").trim(),
                            turler = o.optString("turler").trim(),
                            sure = o.optString("sure").trim(),
                            yonetmen = o.optString("yonetmen").trim(),
                            oyuncular = o.optString("oyuncular").trim(),
                            platformlar = o.optString("platform").trim()
                        )
                    )
                } catch (_: Exception) {
                    // Bu nesne bozuk, diğerlerine devam
                }
            }
            if (liste.isNotEmpty()) {
                android.util.Log.i(TAG, "Parça kurtarma: " + liste.size + " film alındı")
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Parça kurtarma başarısız", e)
        }
        return liste
    }

    // ═══════════════════════════════════════════════════════════════
    // İZLEME BAĞLANTILARI
    // ═══════════════════════════════════════════════════════════════

    /**
     * Yasal izleme platformları için arama bağlantıları.
     *
     * Not: Doğrudan yapım sayfasına link kurmak platform kimliği gerektirir;
     * arama sayfası her zaman çalışır ve bozulmaz.
     */
    fun izlemeBaglantilari(y: FilmStore.Yapim): List<Pair<String, String>> {
        val q = URLEncoder.encode(
            (y.orijinalAd.ifBlank { y.ad }), "UTF-8"
        )
        return listOf(
            "Netflix" to "https://www.netflix.com/search?q=" + q,
            "Prime Video" to "https://www.primevideo.com/search?phrase=" + q,
            "Disney+" to "https://www.disneyplus.com/tr-tr/search?q=" + q,
            "BluTV" to "https://www.blutv.com/arama?q=" + q,
            "Exxen" to "https://www.exxen.com/arama?query=" + q,
            "TOD" to "https://www.todtv.com.tr/arama?q=" + q,
            "MUBI" to "https://mubi.com/tr/tr/search/" + q,
            "YouTube (fragman)" to "https://www.youtube.com/results?search_query=" + q + "+fragman"
        )
    }

    /** IMDb sayfası — kimlik varsa doğrudan, yoksa arama. */
    fun imdbBaglantisi(y: FilmStore.Yapim): String =
        if (y.imdbId.isNotBlank()) "https://www.imdb.com/title/" + y.imdbId
        else "https://www.imdb.com/find?q=" +
            URLEncoder.encode(y.orijinalAd.ifBlank { y.ad }, "UTF-8")

    /** Kalite seçenekleri — platformların sunduğu çözünürlükler. */
    val KALITELER = listOf("480p", "720p", "1080p", "4K")

    /**
     * Seçilen kalitenin hangi platformlarda bulunduğunu açıklar.
     * Bilgi amaçlıdır; indirme uygulamanın kendisinde yapılır.
     */
    fun kaliteAciklamasi(context: Context, kalite: String): String = when (kalite) {
        "480p" -> context.getString(R.string.fl_q_480)
        "720p" -> context.getString(R.string.fl_q_720)
        "4K" -> context.getString(R.string.fl_q_4k)
        else -> context.getString(R.string.fl_q_1080)
    }
}
