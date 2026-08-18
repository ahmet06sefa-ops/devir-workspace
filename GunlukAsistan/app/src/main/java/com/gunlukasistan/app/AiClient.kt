package com.gunlukasistan.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * Çevrimiçi yapay zekâ istemcisi (v5.5).
 *
 * Gizlilik notu: bu sınıf YALNIZCA kullanıcı çevrimiçi modu açıkça seçtiğinde çalışır.
 * Anahtar cihazda saklanır, sunucumuz yoktur — istek doğrudan seçilen sağlayıcıya gider.
 * Çevrimdışı modda hiçbir ağ çağrısı yapılmaz.
 */
object AiClient {

    /** Desteklenen sağlayıcılar. */
    enum class Provider(
        val id: String,
        val label: String,
        val endpoint: String,
        val defaultModel: String,
        val keyUrl: String
    ) {
        GEMINI(
            "gemini", "Google Gemini ⭐ önerilen",
            "https://generativelanguage.googleapis.com/v1beta/models",
            "gemini-3-flash-preview",
            "aistudio.google.com/apikey"
        ),
        OPENAI(
            "openai", "OpenAI (ChatGPT)",
            "https://api.openai.com/v1/chat/completions",
            "gpt-5.6-luna",
            "platform.openai.com/api-keys"
        ),
        OPENROUTER(
            "openrouter", "OpenRouter (çok model)",
            "https://openrouter.ai/api/v1/chat/completions",
            "google/gemini-3-flash-preview",
            "openrouter.ai/keys"
        ),
        CUSTOM(
            "custom", "Özel (OpenAI uyumlu)",
            "",
            "",
            "—"
        );

        /** Ayarlar ekranında sunulan hazır model seçenekleri. */
        val presetModels: List<String>
            get() = when (this) {
                GEMINI -> listOf(
                    "gemini-3-flash-preview",
                    "gemini-3.1-flash-lite",
                    "gemini-3.1-pro-preview",
                    "gemini-2.5-flash",
                    "gemini-2.5-flash-lite",
                    "gemini-2.5-pro"
                )
                OPENAI -> listOf(
                    "gpt-5.6-luna",
                    "gpt-5.6-terra",
                    "gpt-5.6-sol",
                    "gpt-5.4-mini",
                    "gpt-5.5"
                )
                // v7.25: OpenRouter canlı listesinden (30 Tem 2026) doğrulandı.
                // ":free" ile bitenler ücretsiz — kredi harcamaz.
                OPENROUTER -> listOf(
                    "google/gemini-3-flash-preview",
                    "google/gemini-3.1-flash-lite",
                    "inclusionai/ling-3.0-flash:free",
                    "google/gemma-4-31b-it:free",
                    "nvidia/nemotron-3-super-120b-a12b:free",
                    "openai/gpt-oss-20b:free",
                    "openai/gpt-5.6-luna",
                    "anthropic/claude-opus-5"
                )
                CUSTOM -> emptyList()
            }

        companion object {
            fun fromId(id: String): Provider = entries.firstOrNull { it.id == id } ?: GEMINI
        }
    }

    class Result(val ok: Boolean, val text: String)

    /** Cihazda kullanılabilir bir ağ var mı? */
    fun isOnline(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val net = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(net) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Asistanın kişiliğini ve kullanıcının verilerini içeren sistem talimatı.
     * Böylece çevrimiçi model de kullanıcının gerçek durumunu bilir.
     */
    fun buildSystemPrompt(context: Context): String {
        val (streak, best) = Store.streakInfo(context)
        val focus = Store.getTodayFocusMinutes(context)
        val goal = Store.getGoalMinutes(context)
        val questions = Store.getTodayQuestions(context)
        // v11.13: kalıcı kullanıcı hafızası — AI kullanıcıyı tanısın
        val hafiza = runCatching {
            KullaniciHafizasi.profilMetni(
                focus, goal, streak, best, questions
            ) + " Haftalık: " + KullaniciHafizasi.haftalikOzet(
                Store.weekFocus(context)
            )
        }.getOrElse { "" }
        val topics = Store.loadTopics(context)
        val tasks = Store.loadTasks(context).filter { !it.done }
        val habits = Store.loadHabits(context).filterNot { it.archived }
        val (habitDone, habitTotal) = Store.habitProgressToday(context)
        val event = Store.highlightEvent(context)

        return buildString {
            append("Sen 'Günlük Asistan' adlı Türkçe kişisel asistansın. ")
            append("Kullanıcıya samimi, kısa ve uygulanabilir cevaplar ver. ")
            append("Gereksiz uzatma, madde işaretleri ve emoji kullanabilirsin.\n\n")
            append("KULLANICININ GÜNCEL DURUMU:\n")
            if (hafiza.isNotBlank()) append("Hafıza: $hafiza\n")
            append("- Bugün: $focus/$goal dk odaklanma, $questions soru çözüldü\n")
            append("- Seri: $streak gün (en iyi: $best)\n")
            if (event != null) {
                append("- Yaklaşan: ${event.title} (${event.daysLeft} gün kaldı)\n")
            }
            if (topics.isNotEmpty()) {
                append("- Konular: ")
                append(topics.take(8).joinToString(", ") { "${it.title} %${it.percent}" })
                append("\n")
            }
            if (tasks.isNotEmpty()) {
                append("- Bekleyen görevler: ")
                append(tasks.take(8).joinToString(", ") { it.text })
                append("\n")
            }
            if (habits.isNotEmpty()) {
                append("- Alışkanlıklar: $habitDone/$habitTotal tamamlandı (")
                append(habits.take(6).joinToString(", ") { it.title })
                append(")\n")
            }

            // v7.26: kurs verileri — uygulamanın en büyük içerik alanı
            try {
                val ist = Store.kursIstatistik(context)
                if (ist.toplamDers > 0) {
                    append("- Kurslar: ${ist.bitenDers}/${ist.toplamDers} ders ")
                    append("(%${ist.yuzde}) tamamlandı, ")
                    append("${ist.kalanDakika / 60} saat içerik kaldı\n")
                }
                val seri = Store.kursSeri(context)
                if (seri.gunSayisi > 0) {
                    append("- Kurs serisi: ${seri.gunSayisi} gün üst üste ")
                    append("(rekor: ${seri.rekor})\n")
                }
                Store.sonDers(context)?.let { d ->
                    append("- Kaldığı ders: ${d.title}\n")
                }
                val notlu = Store.notluDersler(context)
                if (notlu.isNotEmpty()) {
                    append("- Ders notu aldığı konular: ")
                    append(notlu.take(5).joinToString(", ") { it.title })
                    append("\n")
                }
                val kaynak = Store.kaynakSayisi(context)
                if (kaynak > 0) append("- Kaydettiği kaynak: $kaynak adet\n")
            } catch (_: Exception) {
                // Kurs verisi okunamazsa asistan yine çalışsın
            }

            append("\nBu verilere göre kişiselleştirilmiş cevap ver.\n\n")

            // v7.26: asistan artık uygulamada iş yapabiliyor
            append("YAPABİLECEĞİN İŞLEMLER — TAM YETKİ:\n")
            append("Kullanıcı bir şey isterse cevabının EN SONUNA komut satır(lar)ı ekle. ")
            append("Komut satırları kullanıcıya gösterilmez, uygulama onları çalıştırır.\n")
            append("Biçim: >>KOMUT: ad | değer\n")
            append("Birden çok iş için ALT ALTA birden çok komut yazabilirsin (en fazla 8).\n\n")

            append("GÖREVLER\n")
            append("  >>KOMUT: gorev_ekle | Metin :: yarın 14:00   (tarih isteğe bağlı)\n")
            append("  >>KOMUT: gorev_tamamla | Görev adı\n")
            append("  >>KOMUT: gorev_sil | Görev adı\n")
            append("  >>KOMUT: gorev_duzenle | Eski ad :: Yeni metin\n\n")

            append("NOTLAR\n")
            append("  >>KOMUT: not_ekle | Başlık :: İçerik\n")
            append("  >>KOMUT: not_sil | Not başlığı\n")
            append("  >>KOMUT: not_duzenle | Başlık :: Yeni içerik\n\n")

            append("KONULAR\n")
            append("  >>KOMUT: konu_ekle | Başlık :: madde1 ;; madde2   (maddeler isteğe bağlı)\n")
            append("  >>KOMUT: konu_sil | Konu adı\n")
            append("  >>KOMUT: konu_duzenle | Eski ad :: Yeni ad\n")
            append("  >>KOMUT: alt_madde_ekle | Konu adı :: madde1 ;; madde2 ;; madde3\n")
            append("  >>KOMUT: alt_madde_tamamla | Konu adı :: madde adı\n")
            append("  >>KOMUT: alt_madde_sil | Konu adı :: madde adı\n\n")

            append("ALIŞKANLIKLAR\n")
            append("  >>KOMUT: aliskanlik_ekle | Ad :: 3        (günlük hedef sayısı)\n")
            append("  >>KOMUT: aliskanlik_isaretle | Ad         (bugün için +1)\n")
            append("  >>KOMUT: aliskanlik_sil | Ad\n\n")

            append("ETKİNLİK / TAKVİM\n")
            append("  >>KOMUT: etkinlik_ekle | Başlık :: 15 Mart 2027\n")
            append("  >>KOMUT: etkinlik_sil | Başlık\n\n")

            append("KURSLAR\n")
            append("  >>KOMUT: kurs_ekle | Kurs adı :: açıklama\n")
            append("  >>KOMUT: kurs_sil | Kurs adı\n")
            append("  >>KOMUT: bolum_ekle | Kurs adı :: bölüm1 ;; bölüm2\n")
            append("  >>KOMUT: ders_ekle | Kurs :: Bölüm :: ders1 ;; ders2\n")
            append("  >>KOMUT: ders_tamamla | Ders adı\n")
            append("  >>KOMUT: ders_sil | Ders adı\n\n")

            append("KARTLAR / SINAV / AYARLAR\n")
            append("  >>KOMUT: kart_ekle | Deste :: ön1 = arka1 ;; ön2 = arka2\n")
            append("  >>KOMUT: sinav_ekle | Deneme 5 :: Türkçe=25 ;; Matematik=20\n")
            append("  >>KOMUT: hedef_ayarla | 120                (günlük dakika)\n")
            append("  >>KOMUT: soz_ayarla | Motivasyon sözü\n")
            append("  >>KOMUT: sinav_tarihi | 6 Eylül 2026\n\n")

            append("EYLEMLER\n")
            append("  >>KOMUT: zamanlayici | 25\n")
            append("  >>KOMUT: ekran_ac | kurslar   (kurslar/gorevler/notlar/konular/")
            append("aliskanliklar/zamanlayici/kaynaklar/araclar/istatistik/bugun/")
            append("sinav/etkinlik/ayarlar/tema/asistan/ana)\n")
            append("  >>KOMUT: ders_devam |\n")
            append("  >>KOMUT: analiz_ac |            (detaylı ilerleme analizi)\n")
            append("  >>KOMUT: pdf_ara |              (ders PDF içinde arama)\n")
            append("  >>KOMUT: namaz_ac |            (namaz vakitleri ve gün planı)\n")
            append("  >>KOMUT: film_ac |             (günlük dizi/film önerisi)\n")
            append("  >>KOMUT: online_ac |           (iki kişilik ortak liste)\n")
            append("  >>KOMUT: atolye_ac | canva    (canva/kisisel/gorunum/binmadde/youtube/yedek/depolama)\n")
            append("  >>KOMUT: uygulamalar_ac |       (Uygulamalarım ekranı)\n")
            append("  >>KOMUT: uygulama_ac | WhatsApp   (WhatsApp/YouTube/Spotify… açar)\n")
            append("  >>KOMUT: telefon_ara | 05321234567   (çeviriciyi numara dolu açar)\n")
            append("  >>KOMUT: yaz | Merhaba dünya        (odaklanmış alana yazar — önce alana dokun)\n")
            append("  >>KOMUT: yedek_al |\n")
            append("  >>KOMUT: yedek_geri_al |          (son işlemi geri alır)\n\n")

            // v11.13: AI uygulamanın ayarlarını da kontrol eder
            append("UYGULAMA AYARLARI (kullanıcı isterse)\n")
            append("  >>KOMUT: ayar_ses | acik        (acik / kapanik)\n")
            append("  >>KOMUT: ayar_titresim | acik   (acik / kapanik)\n")
            append("  >>KOMUT: ayar_animasyon | acik  (acik / kapanik)\n")
            append("  >>KOMUT: ayar_namaz | acik      (acik / kapanik)\n")
            append("  >>KOMUT: ayar_gece | koyu       (koyu / acik / sistem)\n")
            append("  >>KOMUT: widget_yenile |\n")
            append("  >>KOMUT: ozet_ver |            (tüm veri durumunu özetle)\n\n")
            append("  >>KOMUT: koc_mesaj |              (günün vaktine göre proaktif koç mesajı)\n")
            append("  >>KOMUT: haftalik_rapor |           (haftalık koç raporu)\n")
            append("  >>KOMUT: akilli_plan |              (bekleyen görevlerden günlük plan)\n\n")
            append("  >>KOMUT: kocluk_programi | ders    (hazır koçluk programı: ders/erken kalk/odak)\n")
            append("  >>KOMUT: xp_durum |                 (oyunlaştırma: XP, seviye, rütbe)\n\n")
            append("  >>KOMUT: hesap_durum |              (bulut senkron hesap bilgisi)\n")
            append("  >>KOMUT: saglik_hedef | 10000   (günlük adım hedefi önerisi)\n")
            append("  >>KOMUT: takvim_plan | A,B,C   (görevleri haftaya dağıt)\n\n")
            append("  >>KOMUT: dil_sec | en             (dil seç: tr/en/de/fr/ar/es/ru)\n")
            append("  >>KOMUT: disa_aktar |              (zengin markdown rapor üret)\n\n")
            append("  >>KOMUT: meydan_okuma | Ders    (grup meydan okuma durumu)\n")
            append("  >>KOMUT: bildirim_durum |         (akıllı bildirim filtre özeti)\n\n")
            append("  >>KOMUT: disa_aktar_csv |         (görev/alışkanlık CSV dışa aktar)\n\n")
            append("  >>KOMUT: depolama_durum |         (depolama kullanımı + temizlik önerisi)\n\n")
            append("  >>KOMUT: basari_raporu |          (bu ayın başarı/istikrar raporu)\n\n")
            append("  >>KOMUT: trend_analiz |            (son 14 gün eğilim + tahmin)\n\n")
            append("  >>KOMUT: gorev_takvimi |          (bugün/yarın/gecikmiş görevler)\n")
            append("  >>KOMUT: pomodoro_durum |        (odak/blok/mola/verimlilik durumu)\n")
            append("  >>KOMUT: onceliklendir | [metin] (görevleri önem/aciliyetle sıralar)\n\n")

            append("KURALLAR:\n")
            append("1. Yalnızca kullanıcı AÇIKÇA istediğinde komut ekle. ")
            append("Sohbet ederken, soru cevaplarken ekleme.\n")
            append("2. Silme komutlarını kullanıcı net biçimde istemedikçe ASLA üretme. ")
            append("Silmeler kullanıcıya ayrıca onaylatılır.\n")
            append("3. Kayıt adlarını kullanıcının listesinde YAZILDIĞI GİBİ kullan.\n")
            append("4. Birden çok iş istenmişse her biri için ayrı komut satırı yaz.\n")
            append("5. Alt başlık istenirse: önce maddeleri normal metin olarak yaz, ")
            append("sonra alt_madde_ekle komutunu koy.\n")
            append("6. Ayraçlar: alanlar arası \" :: \", liste içi \" ;; \"\n\n")

            append("ÖRNEK — kullanıcı: \"Revit konusuna alt başlık ekle ve odak başlat\"\n")
            append("Cevap metni + şu iki satır:\n")
            append(">>KOMUT: alt_madde_ekle | Revit :: Arayüz tanıtımı ;; Duvar çizimi ;; ")
            append("Kapı pencere yerleştirme\n")
            append(">>KOMUT: zamanlayici | 25")
        }
    }

    /**
     * v7.50 — SADE İSTEK: sistem istemi olmadan, büyük bütçeli tek atış.
     *
     * ── Neden gerekti? ──
     * `chat()` sohbet için tasarlandı: 40 komut talimatı + kurs verilerini
     * sistem istemi olarak gönderiyor ve çıktıyı 1200 token'a sınırlıyor.
     * Film listesi gibi uzun JSON isteyen çağrılarda iki sorun çıkıyordu:
     *   1. Yanıt yarıda kesiliyordu (8 film ~850 token + düşünme payı)
     *   2. Model film yerine ">>KOMUT:" üretmeye çalışıyordu
     *
     * Bu fonksiyon sistem istemi eklemez ve bütçeyi çağıran belirler.
     *
     * @param butce istenen çıktı token sayısı (düşünen modelde otomatik büyür)
     */
    fun sadeIstek(context: Context, istem: String, butce: Int = 4096): Result {
        if (!AiSettings.isOnlineMode(context)) {
            return Result(false, context.getString(R.string.ai_err_offline_mode))
        }
        if (!isOnline(context)) {
            return Result(false, context.getString(R.string.ai_err_no_net))
        }
        if (AiSettings.getApiKey(context).isBlank() &&
            AiSettings.anahtarliSaglayicilar(context).isEmpty()
        ) {
            return Result(false, context.getString(R.string.ai_err_no_key))
        }

        sonGecisBilgisi = null
        val saglayicilar = saglayiciSirasi(context)
        val ilkSaglayici = saglayicilar.firstOrNull() ?: Provider.GEMINI
        var sonHata: Result? = null

        for (provider in saglayicilar) {
            val key = AiSettings.getKeyFor(context, provider.id)
                .ifBlank { if (provider == ilkSaglayici) AiSettings.getApiKey(context) else "" }
            if (key.isBlank()) continue

            val modeller = if (provider == Provider.CUSTOM) {
                listOf(AiSettings.getModel(context).ifBlank { provider.defaultModel })
            } else {
                modelSirasi(context, provider)
            }
            if (modeller.isEmpty()) {
                sonHata = Result(
                    false, context.getString(R.string.ai_err_no_free_model, provider.label)
                )
                continue
            }

            for ((sira, model) in modeller.withIndex()) {
                try {
                    val sonuc = if (provider == Provider.GEMINI) {
                        sadeGemini(context, key, model, istem, butce)
                    } else {
                        sadeOpenAi(context, provider, key, model, istem, butce)
                    }
                    if (sonuc.ok && sonuc.text.isNotBlank()) {
                        if (sira > 0) calisanModeliKaydet(context, provider, model)
                        sonKullanilanSaglayici = provider
                        if (provider != ilkSaglayici) {
                            gecisBilgisiYaz(context, ilkSaglayici, provider)
                        }
                        return sonuc
                    }
                    sonHata = sonuc
                    val modelHatasi = sonuc.text.contains("model", true) ||
                        sonuc.text.contains("bulunamadı", true)
                    if (!modelHatasi) break
                } catch (e: Exception) {
                    sonHata = Result(
                        false,
                        context.getString(R.string.ai_err_generic, e.message ?: "bilinmeyen")
                    )
                }
            }

            val mesaj = sonHata?.text.orEmpty()
            if (!saglayiciDegistirmeliMi(mesaj)) {
                return sonHata ?: Result(
                    false, context.getString(R.string.ai_err_generic, "bilinmeyen")
                )
            }
        }
        return sonHata ?: Result(false, context.getString(R.string.ai_err_generic, "model"))
    }

    /** Sistem istemi olmadan Gemini çağrısı. */
    private fun sadeGemini(
        context: Context, key: String, model: String, istem: String, butce: Int
    ): Result {
        val url = Provider.GEMINI.endpoint + "/" + model + ":generateContent?key=" + key
        val govde = JSONObject()
            .put(
                "contents",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put("parts", JSONArray().put(JSONObject().put("text", istem)))
                )
            )
            .put(
                "generationConfig",
                JSONObject()
                    .put("temperature", 0.8)
                    .put("maxOutputTokens", tokenButcesi(model, butce))
                    .apply { dusunmeAyari(model)?.let { put("thinkingConfig", it) } }
            )
            .toString()

        val conn = (URL(url).openConnection() as HttpsURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 25000
            readTimeout = 90000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(govde) }

        val kod = conn.responseCode
        val akis = if (kod in 200..299) conn.inputStream else conn.errorStream
        val cevap = BufferedReader(InputStreamReader(akis, Charsets.UTF_8)).use { it.readText() }
        conn.disconnect()

        if (kod !in 200..299) return Result(false, hataMesaji(context, kod, cevap))

        return try {
            val aday = JSONObject(cevap).optJSONArray("candidates")?.optJSONObject(0)
            val parcalar = aday?.optJSONObject("content")?.optJSONArray("parts")
            val sb = StringBuilder()
            if (parcalar != null) {
                for (i in 0 until parcalar.length()) {
                    sb.append(parcalar.optJSONObject(i)?.optString("text", "").orEmpty())
                }
            }
            val metin = sb.toString().trim()
            if (metin.isBlank()) {
                // Genelde MAX_TOKENS: düşünme payı çıktıyı yemiş
                val sebep = aday?.optString("finishReason").orEmpty()
                Result(false, context.getString(R.string.ai_err_bos_sebep, sebep))
            } else {
                Result(true, metin)
            }
        } catch (e: Exception) {
            android.util.Log.w("AiClient", "Yanıt okunamadı", e)
            Result(false, context.getString(R.string.ai_err_generic, "yanıt"))
        }
    }

    /** Sistem istemi olmadan OpenAI uyumlu çağrı. */
    private fun sadeOpenAi(
        context: Context, provider: Provider, key: String,
        model: String, istem: String, butce: Int
    ): Result {
        val endpoint = if (provider == Provider.CUSTOM) {
            AiSettings.getCustomEndpoint(context).ifBlank {
                return Result(false, context.getString(R.string.ai_err_no_endpoint))
            }
        } else provider.endpoint

        val govde = JSONObject()
            .put("model", model)
            .put(
                "messages",
                JSONArray().put(JSONObject().put("role", "user").put("content", istem))
            )
            .put("temperature", 0.8)
            .put("max_tokens", butce)
            .toString()

        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20000
            readTimeout = 90000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Authorization", "Bearer " + key)
            if (provider == Provider.OPENROUTER) {
                setRequestProperty("HTTP-Referer", "https://gunlukasistan.app")
                setRequestProperty("X-Title", "Gunluk Asistan")
            }
        }
        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(govde) }

        val kod = conn.responseCode
        val akis = if (kod in 200..299) conn.inputStream else conn.errorStream
        val cevap = BufferedReader(InputStreamReader(akis, Charsets.UTF_8)).use { it.readText() }
        conn.disconnect()

        if (kod !in 200..299) return Result(false, hataMesaji(context, kod, cevap))

        return try {
            val metin = JSONObject(cevap).optJSONArray("choices")
                ?.optJSONObject(0)?.optJSONObject("message")
                ?.optString("content", "").orEmpty().trim()
            if (metin.isBlank()) {
                Result(false, context.getString(R.string.ai_err_bos_sebep, ""))
            } else Result(true, metin)
        } catch (e: Exception) {
            android.util.Log.w("AiClient", "Yanıt okunamadı", e)
            Result(false, context.getString(R.string.ai_err_generic, "yanıt"))
        }
    }

    /** HTTP hata kodunu okunur Türkçe mesaja çevirir. */
    private fun hataMesaji(context: Context, kod: Int, cevap: String): String {
        val ayrinti = try {
            JSONObject(cevap).optJSONObject("error")?.optString("message", "").orEmpty()
        } catch (_: Exception) { "" }
        return when (kod) {
            401, 403 -> context.getString(R.string.ai_err_key_bad)
            402 -> context.getString(R.string.ai_err_credit)
            429 -> context.getString(R.string.ai_err_quota)
            in 500..599 -> context.getString(R.string.ai_err_server)
            else -> context.getString(
                R.string.ai_err_generic,
                if (ayrinti.isBlank()) "HTTP " + kod else ayrinti.take(140)
            )
        }
    }

    /**
     * Seçili sağlayıcıya sohbet isteği gönderir. Ağ işlemi olduğu için
     * mutlaka arka plan iş parçacığından çağrılmalıdır.
     */
    fun chat(
        context: Context,
        userMessage: String,
        history: List<Pair<String, String>> = emptyList()
    ): Result {
        if (!AiSettings.isOnlineMode(context)) {
            return Result(false, context.getString(R.string.ai_err_offline_mode))
        }
        if (!isOnline(context)) {
            return Result(false, context.getString(R.string.ai_err_no_net))
        }
        if (AiSettings.getApiKey(context).isBlank() &&
            AiSettings.anahtarliSaglayicilar(context).isEmpty()
        ) {
            return Result(false, context.getString(R.string.ai_err_no_key))
        }

        sonGecisBilgisi = null
        val saglayicilar = saglayiciSirasi(context)
        val ilkSaglayici = saglayicilar.firstOrNull() ?: Provider.GEMINI
        var sonHata: Result? = null

        // v7.24: Dış döngü sağlayıcılar, iç döngü modeller.
        // Bir sağlayıcı tükendiğinde anahtarı olan diğerine geçilir.
        for (provider in saglayicilar) {
            val key = AiSettings.getKeyFor(context, provider.id)
                .ifBlank { if (provider == ilkSaglayici) AiSettings.getApiKey(context) else "" }
            if (key.isBlank()) continue

            val modeller = if (provider == Provider.CUSTOM) {
                listOf(AiSettings.getModel(context).ifBlank { provider.defaultModel })
            } else {
                modelSirasi(context, provider)
            }

            // v7.34: Ücretsiz modda bu sağlayıcının ücretsiz modeli yoksa hiç çağırma.
            if (modeller.isEmpty()) {
                sonHata = Result(
                    false,
                    context.getString(R.string.ai_err_no_free_model, provider.label)
                )
                continue
            }

            var buSaglayicidaModelHatasi = false
            for ((sira, model) in modeller.withIndex()) {
                try {
                    val sonuc = when (provider) {
                        Provider.GEMINI -> callGemini(context, key, model, userMessage, history)
                        else -> callOpenAiCompatible(
                            context, provider, key, model, userMessage, history
                        )
                    }
                    if (sonuc.ok) {
                        if (sira > 0) calisanModeliKaydet(context, provider, model)
                        sonKullanilanSaglayici = provider
                        // Başka sağlayıcıya geçildiyse kullanıcıya bildir
                        if (provider != ilkSaglayici) {
                            gecisBilgisiYaz(context, ilkSaglayici, provider)
                        }
                        return sonuc
                    }
                    sonHata = sonuc
                    val modelHatasi = sonuc.text.contains("model", ignoreCase = true) ||
                        sonuc.text.contains("bulunamadı", ignoreCase = true)
                    if (modelHatasi) {
                        buSaglayicidaModelHatasi = true
                        continue   // sıradaki modeli dene
                    }
                    // Model hatası değil: bu sağlayıcıda başka model denemeye gerek yok
                    break
                } catch (e: Exception) {
                    sonHata = Result(
                        false,
                        context.getString(R.string.ai_err_generic, e.message ?: "bilinmeyen")
                    )
                }
            }

            // Bu sağlayıcı olmadı — başka sağlayıcı denemeye değer mi?
            val mesaj = sonHata?.text.orEmpty()
            if (!buSaglayicidaModelHatasi && !saglayiciDegistirmeliMi(mesaj)) {
                return sonHata ?: Result(
                    false, context.getString(R.string.ai_err_generic, "bilinmeyen")
                )
            }
            android.util.Log.w(
                "AiClient",
                "${provider.label} başarısız, sıradaki sağlayıcı deneniyor: ${mesaj.take(90)}"
            )
        }

        return sonHata
            ?: Result(false, context.getString(R.string.ai_err_generic, "model"))
    }

    // ---------------- OpenAI / OpenRouter / Özel ----------------

    private fun callOpenAiCompatible(
        context: Context,
        provider: Provider,
        key: String,
        model: String,
        userMessage: String,
        history: List<Pair<String, String>>
    ): Result {
        val endpoint = if (provider == Provider.CUSTOM) {
            AiSettings.getCustomEndpoint(context).ifBlank {
                return Result(false, context.getString(R.string.ai_err_no_endpoint))
            }
        } else provider.endpoint

        val messages = JSONArray()
        messages.put(
            JSONObject().put("role", "system").put("content", buildSystemPrompt(context))
        )
        history.takeLast(8).forEach { (role, content) ->
            messages.put(JSONObject().put("role", role).put("content", content))
        }
        messages.put(JSONObject().put("role", "user").put("content", userMessage))

        val body = JSONObject()
            .put("model", model)
            .put("messages", messages)
            .put("temperature", 0.7)
            .put("max_tokens", 800)
            .toString()

        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20000
            readTimeout = 45000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Authorization", "Bearer $key")
            if (provider == Provider.OPENROUTER) {
                setRequestProperty("HTTP-Referer", "https://gunlukasistan.app")
                setRequestProperty("X-Title", "Gunluk Asistan")
            }
        }

        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val response = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
        conn.disconnect()

        if (code !in 200..299) {
            return Result(false, humanError(context, code, response))
        }

        val json = JSONObject(response)
        val content = json.optJSONArray("choices")
            ?.optJSONObject(0)
            ?.optJSONObject("message")
            ?.optString("content")
            ?.trim()

        return if (content.isNullOrBlank()) {
            Result(false, context.getString(R.string.ai_err_empty))
        } else {
            Result(true, content)
        }
    }

    // ---------------- Google Gemini ----------------

    private fun callGemini(
        context: Context,
        key: String,
        model: String,
        userMessage: String,
        history: List<Pair<String, String>>
    ): Result {
        val url = "${Provider.GEMINI.endpoint}/$model:generateContent?key=$key"

        val contents = JSONArray()
        history.takeLast(8).forEach { (role, text) ->
            contents.put(
                JSONObject()
                    .put("role", if (role == "assistant") "model" else "user")
                    .put("parts", JSONArray().put(JSONObject().put("text", text)))
            )
        }
        contents.put(
            JSONObject()
                .put("role", "user")
                .put("parts", JSONArray().put(JSONObject().put("text", userMessage)))
        )

        val body = JSONObject()
            .put("contents", contents)
            .put(
                "systemInstruction",
                JSONObject().put(
                    "parts",
                    JSONArray().put(JSONObject().put("text", buildSystemPrompt(context)))
                )
            )
            .put(
                "generationConfig",
                JSONObject()
                    .put("temperature", 0.7)
                    // v7.23: düşünen modelde 800 token yetmiyordu — yanıt boş dönüyordu
                    .put("maxOutputTokens", tokenButcesi(model, 1200))
                    .apply { dusunmeAyari(model)?.let { put("thinkingConfig", it) } }
            )
            .toString()

        val conn = (URL(url).openConnection() as HttpsURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20000
            readTimeout = 45000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }

        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val response = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
        conn.disconnect()

        if (code !in 200..299) {
            return Result(false, humanError(context, code, response))
        }

        val root = JSONObject(response)
        val candidate = root.optJSONArray("candidates")?.optJSONObject(0)

        // Güvenlik filtresi veya token limiti nedeniyle kesilmiş olabilir
        val finishReason = candidate?.optString("finishReason") ?: ""
        val text = candidate
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
            ?.optJSONObject(0)
            ?.optString("text")
            ?.trim()

        if (!text.isNullOrBlank()) return Result(true, text)

        // İstek tamamen engellendiyse sebebini bildir
        val blockReason = root.optJSONObject("promptFeedback")?.optString("blockReason") ?: ""
        return when {
            blockReason.isNotBlank() ->
                Result(false, context.getString(R.string.ai_err_blocked, blockReason))
            finishReason == "SAFETY" ->
                Result(false, context.getString(R.string.ai_err_blocked, "SAFETY"))
            finishReason == "MAX_TOKENS" ->
                Result(false, context.getString(R.string.ai_err_truncated))
            else -> Result(false, context.getString(R.string.ai_err_empty))
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // v7.19 — FOTOĞRAFTAN KONU OKUMA (el yazısı destekli)
    // ═══════════════════════════════════════════════════════════════

    /** Görselden çıkarılan konu ve alt maddeleri. */
    class OkunanKonu(val baslik: String, val maddeler: List<String>)

    /**
     * El yazısı okuma talimatı.
     *
     * Kritik tasarım kararları:
     *  · Sıcaklık 0 — yaratıcılık değil, birebir okuma isteniyor
     *  · Model tahmin etmeye değil, emin olmadığında işaretlemeye yönlendiriliyor
     *  · Çıktı katı JSON — ayrıştırma hatası olmasın
     *  · Türkçe karakter ve kısaltmalar özellikle vurgulanıyor
     */
    private const val OKUMA_TALIMATI = """Sen bir el yazısı okuma uzmanısın. Görseldeki NOTU birebir okuyacaksın.

MUTLAK KURALLAR:
1. SADECE görselde YAZAN kelimeleri yaz. Kendi kafandan kelime EKLEME.
2. Okuyamadığın bir kelime varsa ??? yaz — TAHMİN ETME, UYDURMA.
3. Türkçe karakterlere dikkat: ı-i, ş-s, ğ-g, ü-u, ö-o, ç-c ayrımını doğru yap.
4. Kısaltmaları olduğu gibi bırak (örn. "vb.", "TS500", "min.").
5. Sayıları ve birimleri birebir kopyala (örn. "2h+b=63", "25/50", "C25").
6. Madde işaretlerini (-, *, •, 1., a)) maddenin PARÇASI SAYMA, at.
7. Üstü çizili yazıları atla.

BAŞLIK SEÇİMİ:
- Sayfanın en üstündeki, altı çizili, büyük harfle yazılmış veya kutu içindeki yazı başlıktır.
- Belirgin bir başlık yoksa maddelerin ortak konusunu 2-4 kelimeyle özetle.

ÇIKTI — yalnızca şu JSON, başka hiçbir şey yazma:
{"baslik":"Konu başlığı","maddeler":["birinci madde","ikinci madde"]}

Madde yoksa: {"baslik":"...","maddeler":[]}
Görsel okunamıyorsa: {"baslik":"","maddeler":[]}"""

    /**
     * Fotoğraftan konu başlığı ve maddelerini okur.
     *
     * @param base64Jpeg JPEG görselin base64 kodu (veri öneki olmadan)
     * @param ekNot kullanıcının eklemek istediği yönlendirme (boş olabilir)
     */
    /**
     * v7.19: İki okuma sonucunu karşılaştırıp en güveniliri seçer.
     *
     * El yazısında model bazen ilk denemede kelime atlar. İki bağımsız okuma
     * yapıp karşılaştırmak doğruluğu belirgin artırır:
     *  · İkisi aynıysa sonuç güvenilirdir
     *  · Farklıysa "???" içermeyen ve daha çok madde bulan tercih edilir
     */
    private fun enIyisiniSec(a: OkunanKonu?, b: OkunanKonu?): OkunanKonu? {
        if (a == null) return b
        if (b == null) return a

        fun puan(k: OkunanKonu): Int {
            var p = 0
            // Okunamayan kelime ağır ceza
            val supheli = (k.baslik + k.maddeler.joinToString(" ")).split("???").size - 1
            p -= supheli * 25
            // Daha çok madde bulmak iyi
            p += k.maddeler.size * 6
            // Başlık bulmak iyi
            if (k.baslik.isNotBlank()) p += 12
            // Aşırı kısa maddeler şüpheli (tek harf, anlamsız)
            p -= k.maddeler.count { it.length <= 2 } * 8
            // Toplam içerik uzunluğu (daha fazla bilgi = daha iyi okuma)
            p += (k.baslik.length + k.maddeler.sumOf { it.length }) / 12
            return p
        }
        return if (puan(b) > puan(a)) b else a
    }

    fun konuOku(context: Context, base64Jpeg: String, ekNot: String = ""): Pair<Result, OkunanKonu?> {
        if (!AiSettings.isOnlineMode(context)) {
            return Result(false, context.getString(R.string.ai_err_offline_mode)) to null
        }
        if (!isOnline(context)) {
            return Result(false, context.getString(R.string.ai_err_no_net)) to null
        }
        val key = AiSettings.getApiKey(context)
        if (key.isBlank()) {
            return Result(false, context.getString(R.string.ai_err_no_key)) to null
        }

        val provider = Provider.fromId(AiSettings.getProviderId(context))
        val model = gorselModeli(context)
        if (model.isBlank()) {
            return Result(
                false, context.getString(R.string.ai_err_no_free_model, provider.label)
            ) to null
        }

        fun tekOkuma(ek: String): Result = when (provider) {
            Provider.GEMINI -> geminiGorsel(context, key, model, base64Jpeg, ek)
            else -> openAiGorsel(context, provider, key, model, base64Jpeg, ek)
        }

        return try {
            // 1. geçiş — normal okuma
            val ilk = tekOkuma(ekNot)
            if (!ilk.ok) return ilk to null
            val konuA = jsonKonuAyristir(ilk.text)

            // 2. geçiş — yalnızca ilk okuma şüpheliyse.
            // Her seferinde iki istek atmak kotayı boşa harcar; sadece gerekince yap.
            val supheli = konuA == null ||
                konuA.maddeler.isEmpty() ||
                (konuA.baslik + konuA.maddeler.joinToString(" ")).contains("???")

            val secilen = if (supheli) {
                val ikinci = tekOkuma(
                    (if (ekNot.isBlank()) "" else "$ekNot\n") +
                        "Bu ikinci okuma denemesi. Önceki denemede bazı kelimeler " +
                        "okunamadı. Harflere tek tek dikkat ederek yeniden oku."
                )
                val konuB = if (ikinci.ok) jsonKonuAyristir(ikinci.text) else null
                enIyisiniSec(konuA, konuB)
            } else {
                konuA
            }

            if (secilen == null || (secilen.baslik.isBlank() && secilen.maddeler.isEmpty())) {
                Result(false, context.getString(R.string.ocr_err_unreadable)) to null
            } else {
                Result(true, ilk.text) to secilen
            }
        } catch (e: Exception) {
            Result(false, context.getString(R.string.ai_err_generic, e.message ?: "bilinmeyen")) to null
        }
    }

    /**
     * Görsel okuma için model seçimi.
     * Kullanıcının seçtiği model görsel desteklemiyorsa güvenli bir modele düşer.
     */
    private fun gorselModeli(context: Context): String {
        val secili = AiSettings.getModel(context).ifBlank { "" }
        val provider = Provider.fromId(AiSettings.getProviderId(context))
        return when (provider) {
            // "lite" modeller el yazısında zayıf kalıyor — tam flash kullan
            Provider.GEMINI -> when {
                secili.contains("lite", true) || secili.isBlank() ->
                    calisanModel(context, provider) ?: "gemini-3-flash-preview"
                else -> secili
            }
            Provider.OPENAI -> if (secili.isBlank()) "gpt-5.6-terra" else secili
            else -> secili.ifBlank { provider.defaultModel }
        }.let { m ->
            // v7.34: ücretsiz modda görsel okuma da kredi harcamamalı
            if (!AiSettings.isUcretsizMod(context) || modelUcretsizMi(provider, m)) m
            else ucretsizModeller(provider).firstOrNull().orEmpty()
        }
    }

    private fun geminiGorsel(
        context: Context,
        key: String,
        model: String,
        base64Jpeg: String,
        ekNot: String
    ): Result {
        val url = "${Provider.GEMINI.endpoint}/$model:generateContent?key=$key"

        val istem = if (ekNot.isBlank()) OKUMA_TALIMATI
        else OKUMA_TALIMATI + "\n\nKULLANICI NOTU: " + ekNot

        val parcalar = JSONArray()
            .put(JSONObject().put("text", istem))
            .put(
                JSONObject().put(
                    "inline_data",
                    JSONObject()
                        .put("mime_type", "image/jpeg")
                        .put("data", base64Jpeg)
                )
            )

        val body = JSONObject()
            .put(
                "contents",
                JSONArray().put(JSONObject().put("role", "user").put("parts", parcalar))
            )
            .put(
                "generationConfig",
                JSONObject()
                    // Birebir okuma istiyoruz — yaratıcılık kapalı
                    .put("temperature", 0.0)
                    .put("topP", 0.1)
                    // v7.23: düşünen modelde JSON çıktısı kesilmesin
                    .put("maxOutputTokens", tokenButcesi(model, 2048))
                    .put("responseMimeType", "application/json")
                    .apply { dusunmeAyari(model)?.let { put("thinkingConfig", it) } }
            )
            .toString()

        val conn = (URL(url).openConnection() as HttpsURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 25000
            readTimeout = 90000   // görsel işleme uzun sürebilir
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val response = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
        conn.disconnect()

        if (code !in 200..299) return Result(false, humanError(context, code, response))

        val candidate = JSONObject(response).optJSONArray("candidates")?.optJSONObject(0)
        val text = candidate?.optJSONObject("content")?.optJSONArray("parts")
            ?.optJSONObject(0)?.optString("text")?.trim()
        return if (!text.isNullOrBlank()) Result(true, text)
        else Result(false, context.getString(R.string.ai_err_empty))
    }

    private fun openAiGorsel(
        context: Context,
        provider: Provider,
        key: String,
        model: String,
        base64Jpeg: String,
        ekNot: String
    ): Result {
        val istem = if (ekNot.isBlank()) OKUMA_TALIMATI
        else OKUMA_TALIMATI + "\n\nKULLANICI NOTU: " + ekNot

        val icerik = JSONArray()
            .put(JSONObject().put("type", "text").put("text", istem))
            .put(
                JSONObject()
                    .put("type", "image_url")
                    .put(
                        "image_url",
                        JSONObject().put("url", "data:image/jpeg;base64,$base64Jpeg")
                    )
            )

        val body = JSONObject()
            .put("model", model)
            .put(
                "messages",
                JSONArray().put(JSONObject().put("role", "user").put("content", icerik))
            )
            .put("temperature", 0.0)
            .put("max_tokens", 2048)
            .toString()

        val hedef = provider.endpoint.ifBlank { AiSettings.getCustomEndpoint(context) }
        val conn = (URL(hedef).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 25000
            readTimeout = 90000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Authorization", "Bearer $key")
        }
        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val response = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
        conn.disconnect()

        if (code !in 200..299) return Result(false, humanError(context, code, response))

        val text = JSONObject(response).optJSONArray("choices")?.optJSONObject(0)
            ?.optJSONObject("message")?.optString("content")?.trim()
        return if (!text.isNullOrBlank()) Result(true, text)
        else Result(false, context.getString(R.string.ai_err_empty))
    }

    /**
     * Modelin döndürdüğü metinden JSON'u ayıklar.
     * Model bazen ```json bloğu veya açıklama ekleyebiliyor — ona da dayanıklı.
     */
    fun jsonKonuAyristir(ham: String): OkunanKonu? {
        return try {
            var s = ham.trim()
            // Kod bloğu işaretlerini temizle
            if (s.startsWith("```")) {
                s = s.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            }
            // İlk { ile son } arasını al — sınırlar her durumda güvenli olmalı
            val bas = s.indexOf('{')
            val son = s.lastIndexOf('}')
            if (bas < 0 || son <= bas || son >= s.length) return null
            val govde = s.take(son + 1).drop(bas)
            if (govde.isBlank()) return null
            val o = JSONObject(govde)

            val baslik = o.optString("baslik", "").trim()
            val dizi = o.optJSONArray("maddeler")
            val maddeler = mutableListOf<String>()
            if (dizi != null) {
                for (i in 0 until dizi.length()) {
                    val m = dizi.optString(i, "").trim()
                        // Modelin bıraktığı madde işaretlerini temizle
                        .removePrefix("-").removePrefix("*").removePrefix("•")
                        .trim()
                    if (m.isNotBlank()) maddeler.add(m)
                }
            }
            OkunanKonu(baslik, maddeler)
        } catch (_: Exception) {
            null
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // v7.22 — MODEL DAYANIKLILIĞI
    // ═══════════════════════════════════════════════════════════════
    //
    // Sorun: Sağlayıcılar model adlarını sık değiştiriyor ve eskilerini
    // kapatıyor. Sabit yazılmış bir model adı 6 ay sonra 404 veriyor ve
    // uygulama "API kabul etmiyor" gibi görünüyor.
    //
    // Çözüm üç katmanlı:
    //   1. Güncel varsayılanlar (yukarıda)
    //   2. 404/400 alınca sıradaki yedek modele otomatik geçiş
    //   3. Sağlayıcıdan canlı model listesi çekme (Ayarlar'da "Modelleri yenile")

    /**
     * Bir model çalışmazsa denenecek yedekler.
     * Yeniden eskiye doğru sıralı — biri tutana kadar denenir.
     */
    fun yedekModeller(provider: Provider): List<String> = when (provider) {
        Provider.GEMINI -> listOf(
            "gemini-3-flash-preview",
            "gemini-2.5-flash",
            "gemini-flash-latest",
            "gemini-2.5-flash-lite",
            "gemini-2.0-flash"
        )
        Provider.OPENAI -> listOf(
            "gpt-5.6-luna",
            "gpt-5.4-mini",
            "gpt-5-mini",
            "gpt-4o-mini"
        )
        // v7.25: ücretli → ücretsiz sırası. Kredi biterse ücretsizler devreye girer.
        Provider.OPENROUTER -> listOf(
            "google/gemini-3-flash-preview",
            "google/gemini-3.1-flash-lite",
            "inclusionai/ling-3.0-flash:free",
            "google/gemma-4-31b-it:free",
            "openai/gpt-oss-20b:free"
        )
        Provider.CUSTOM -> emptyList()
    }

    // ═══════════════════════════════════════════════════════════════
    // v7.24 — SAĞLAYICILAR ARASI OTOMATİK GEÇİŞ
    // ═══════════════════════════════════════════════════════════════
    //
    // Bir sağlayıcı kota doldurduğunda, anahtarı geçersizleştiğinde veya
    // sunucusu düştüğünde iş durmasın: anahtarı olan diğer sağlayıcıya
    // geçilir ve işleme devam edilir.
    //
    // Kritik ayrım — HER hatada geçiş yapılmaz:
    //   · Kota/yetki/sunucu hatası  → geçiş YAPILIR (diğeri çalışabilir)
    //   · Ağ yok / mod kapalı       → geçiş YAPILMAZ (hepsinde aynı sonuç)
    //   · İçerik engeli (SAFETY)    → geçiş YAPILMAZ (istem sorunlu)

    /** Bu hata başka sağlayıcıda düzelebilir mi? */
    fun saglayiciDegistirmeliMi(mesaj: String): Boolean {
        val m = mesaj.lowercase()
        // Ağ ve yapılandırma sorunları her sağlayıcıda aynı
        if (m.contains("internet") || m.contains("bağlantı") ||
            m.contains("çevrimdışı") || m.contains("çevrimiçi mod")
        ) return false
        // İçerik engeli istemle ilgili, sağlayıcı değiştirmek çözmez
        if (m.contains("engellendi") || m.contains("safety")) return false
        // Kota, yetki, sunucu, model — diğerinde çalışabilir
        return m.contains("kredi") || m.contains("credit") ||
            m.contains("kota") || m.contains("quota") ||
            m.contains("anahtar") || m.contains("key") ||
            m.contains("sunucu") || m.contains("server") ||
            m.contains("model") || m.contains("bulunamadı") ||
            m.contains("429") || m.contains("5")
    }

    // ═══════════════════════════════════════════════════════════════
    // v7.34 — ÜCRET SINIFLANDIRMASI VE "SADECE ÜCRETSİZ" MODU
    // ═══════════════════════════════════════════════════════════════
    //
    // Kullanıcının sorusu: "Yapay zekayı tamamen ücretsiz versiyonlarda mı?"
    // Dürüst cevap v7.33'e kadar HAYIR idi:
    //   • Gemini varsayılanı ücretsiz katmandaydı  ✅
    //   • OpenAI'nin ücretsiz katmanı hiç yok      ❌
    //   • OpenRouter varsayılanı kredi harcıyordu  ❌ (":free" olanlar sıranın sonundaydı)
    //
    // v7.34'te "Sadece ücretsiz modeller" anahtarı eklendi (VARSAYILAN AÇIK).
    // Açıkken uygulama parasal risk taşıyan hiçbir modeli çağırmaz.

    /**
     * Bu model, sağlayıcının ücretsiz katmanında çalışır mı?
     * Emin olunamayan her durumda ÜCRETLİ kabul edilir (güvenli taraf).
     */
    fun modelUcretsizMi(provider: Provider, model: String): Boolean {
        val m = model.lowercase().trim()
        if (m.isBlank()) return false
        return when (provider) {
            // Gemini ücretsiz katmanı: Flash / Flash-Lite / Gemma sınıfı.
            // Pro modelleri ücretsiz katmanda pratikte kullanılamaz (günde ~50 istek,
            // çoğu hesapta yalnızca deneme) — bu yüzden ücretli sayılır.
            Provider.GEMINI ->
                (m.contains("flash") || m.startsWith("gemma")) && !m.contains("pro")
            // OpenRouter'da yalnızca ":free" son ekli modeller kredi harcamaz.
            Provider.OPENROUTER -> m.endsWith(":free")
            // OpenAI'nin API'sinde ücretsiz katman yok — her istek faturalanır.
            Provider.OPENAI -> false
            // Kendi sunucusu (yerel LLM, kurum sunucusu): maliyeti kullanıcı bilir.
            Provider.CUSTOM -> true
        }
    }

    /** Sağlayıcının kullanılabilir bir ücretsiz katmanı var mı? */
    fun saglayicidaUcretsizVarMi(provider: Provider): Boolean = when (provider) {
        Provider.OPENAI -> false
        else -> true
    }

    /**
     * Sağlayıcının ücretsiz modelleri.
     * v7.34: artık tüm sağlayıcılar için çalışır (eskiden yalnızca OpenRouter).
     */
    fun ucretsizModeller(provider: Provider): List<String> =
        (provider.presetModels + yedekModeller(provider))
            .distinct()
            .filter { modelUcretsizMi(provider, it) }

    /** Ücretsiz modda kullanılacak güvenli varsayılan model. */
    fun ucretsizVarsayilan(provider: Provider): String =
        if (modelUcretsizMi(provider, provider.defaultModel)) provider.defaultModel
        else ucretsizModeller(provider).firstOrNull() ?: provider.defaultModel

    /**
     * v7.34: Tüm modüllerin (KaynakBulucu, KursUretici, DersAsistan…) kullanacağı
     * tek model seçici. Ücretsiz mod açıkken kredi harcayan model asla dönmez.
     *
     * Sıra: kullanıcının seçtiği → çalıştığı bilinen → sağlayıcı varsayılanı
     * Ücretsiz modda bu sıradaki ilk ÜCRETSİZ model kullanılır.
     * Hiç ücretsiz yoksa boş dönerek çağıranı durdurur.
     */
    fun guvenliModel(context: Context, provider: Provider): String {
        val aday = AiSettings.getModel(context).trim()
            .takeIf { it.isNotBlank() && provider != Provider.CUSTOM }
            ?: calisanModel(context, provider)
            ?: provider.defaultModel
        if (!AiSettings.isUcretsizMod(context)) return aday
        if (modelUcretsizMi(provider, aday)) return aday
        return ucretsizModeller(provider).firstOrNull().orEmpty()
    }

    /** Ekranda gösterilecek rozet: ücretsiz mi, kredi mi harcıyor? */
    fun ucretEtiketi(provider: Provider, model: String): String =
        if (modelUcretsizMi(provider, model)) "\u2705 ücretsiz" else "\uD83D\uDCB3 ücretli"

    /**
     * v7.24: Denenecek sağlayıcı sırası.
     * Önce kullanıcının seçtiği, sonra anahtarı olan diğerleri.
     */
    fun saglayiciSirasi(context: Context): List<Provider> {
        val aktif = Provider.fromId(AiSettings.getProviderId(context))
        if (!AiSettings.isAutoSwitch(context)) return listOf(aktif)

        val sira = LinkedHashSet<Provider>()
        sira.add(aktif)
        // Gemini'yi öne al: ücretsiz katmanı en geniş olan
        AiSettings.anahtarliSaglayicilar(context)
            .map { Provider.fromId(it) }
            .sortedBy { if (it == Provider.GEMINI) 0 else 1 }
            .forEach { sira.add(it) }
        // Anahtarı olmayanları ele (aktif hariç — hata mesajı kullanıcıya gitsin)
        val liste = sira.filterIndexed { i, p ->
            i == 0 || AiSettings.hasKeyFor(context, p.id)
        }
        // v7.34: ücretsiz modda, ücretsiz katmanı olmayan sağlayıcıya hiç gidilmez.
        if (!AiSettings.isUcretsizMod(context)) return liste
        return liste.filter { saglayicidaUcretsizVarMi(it) }.ifEmpty { liste.take(1) }
    }

    /** v7.24: Geçiş yapıldığında kullanıcıya bilgi vermek için son durum. */
    @Volatile
    var sonKullanilanSaglayici: Provider? = null
        private set

    @Volatile
    var sonGecisBilgisi: String? = null
        private set

    private fun gecisBilgisiYaz(context: Context, kaynak: Provider, hedef: Provider) {
        sonGecisBilgisi = try {
            context.getString(R.string.ai_switched, kaynak.label, hedef.label)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * v7.23: Gemini 3 serisi "düşünen" modeldir — iç muhakeme token'ları
     * çıktı bütçesinden düşer. Bütçe küçükse model düşünürken tükenir ve
     * `finishReason=MAX_TOKENS` ile **boş metin** döner.
     *
     * Bu yüzden Gemini 3'te düşünme seviyesi düşürülür ve bütçe büyütülür.
     * Eski modellerde bu alan yok sayılır, o yüzden sadece 3.x'e eklenir.
     */
    fun dusunmeAyari(model: String): JSONObject? =
        if (model.contains("gemini-3", ignoreCase = true)) {
            JSONObject().put("thinkingLevel", "low")
        } else {
            null
        }

    /** v7.23: Düşünen modelde güvenli çıktı bütçesi. */
    fun tokenButcesi(model: String, istenen: Int): Int =
        if (model.contains("gemini-3", ignoreCase = true)) {
            // Düşünme payı için en az 8k, istenenin 4 katı
            maxOf(istenen * 4, 8192)
        } else {
            istenen
        }

    /** Model bulunamadı hatası mı? (404 veya "model not found" içeren 400) */
    private fun modelHatasiMi(kod: Int, cevap: String): Boolean {
        if (kod == 404) return true
        if (kod != 400) return false
        val d = cevap.lowercase()
        return d.contains("model") &&
            (d.contains("not found") || d.contains("does not exist") ||
                d.contains("deprecated") || d.contains("unsupported"))
    }

    /**
     * v7.22: Çalıştığı doğrulanmış modeli hatırlar.
     * Bir kez yedeğe düşüldüyse sonraki isteklerde doğrudan o kullanılır.
     */
    private fun calisanModeliKaydet(context: Context, provider: Provider, model: String) {
        try {
            context.getSharedPreferences("ai_model_cache", Context.MODE_PRIVATE)
                .edit().putString("ok_" + provider.id, model).apply()
        } catch (_: Exception) {
        }
    }

    fun calisanModel(context: Context, provider: Provider): String? = try {
        context.getSharedPreferences("ai_model_cache", Context.MODE_PRIVATE)
            .getString("ok_" + provider.id, null)
    } catch (_: Exception) {
        null
    }

    /**
     * v7.22: Denenecek model sırasını kurar.
     * Sıra: kullanıcının seçtiği → daha önce çalıştığı bilinen → yedekler
     */
    fun modelSirasi(context: Context, provider: Provider): List<String> {
        val liste = LinkedHashSet<String>()
        AiSettings.getModel(context).trim().takeIf { it.isNotBlank() }?.let { liste.add(it) }
        calisanModel(context, provider)?.let { liste.add(it) }
        liste.add(provider.defaultModel)
        liste.addAll(yedekModeller(provider))
        // v7.25: OpenRouter'da kredi biterse ücretsizler en sonda devrede kalsın
        liste.addAll(ucretsizModeller(provider))
        val tum = liste.filter { it.isNotBlank() }

        // v7.34: "Sadece ücretsiz" açıksa kredi harcayan hiçbir model denenmez.
        if (!AiSettings.isUcretsizMod(context)) return tum
        val sadeceUcretsiz = tum.filter { modelUcretsizMi(provider, it) }
        return sadeceUcretsiz.ifEmpty {
            // Bu sağlayıcıda hiç ücretsiz model yok — boş dönerek çağrıyı engelle.
            emptyList()
        }
    }

    /**
     * v7.22: Sağlayıcıdan gerçek model listesini çeker (Gemini).
     * Ayarlar ekranındaki "Modelleri yenile" düğmesi bunu kullanır.
     * Böylece kullanıcı, o an gerçekten var olan modelleri görür.
     */
    fun canliModelListesi(context: Context): Pair<Boolean, List<String>> {
        val provider = Provider.fromId(AiSettings.getProviderId(context))
        val key = AiSettings.getApiKey(context)
        if (key.isBlank()) return false to emptyList()

        return try {
            when (provider) {
                Provider.GEMINI -> {
                    val url = "${Provider.GEMINI.endpoint}?key=$key&pageSize=100"
                    val conn = (URL(url).openConnection() as HttpsURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 15000
                        readTimeout = 25000
                    }
                    val kod = conn.responseCode
                    val akis = if (kod in 200..299) conn.inputStream else conn.errorStream
                    val cevap = BufferedReader(InputStreamReader(akis, Charsets.UTF_8))
                        .use { it.readText() }
                    conn.disconnect()
                    if (kod !in 200..299) return false to emptyList()

                    val dizi = JSONObject(cevap).optJSONArray("models") ?: return false to emptyList()
                    val cikti = mutableListOf<String>()
                    for (i in 0 until dizi.length()) {
                        val m = dizi.optJSONObject(i) ?: continue
                        // Yalnızca metin üretebilen modeller
                        val yetenek = m.optJSONArray("supportedGenerationMethods")
                        var uygun = yetenek == null
                        if (yetenek != null) {
                            for (j in 0 until yetenek.length()) {
                                if (yetenek.optString(j) == "generateContent") {
                                    uygun = true; break
                                }
                            }
                        }
                        if (!uygun) continue
                        val ad = m.optString("name", "").removePrefix("models/")
                        // Gömme/görsel modellerini listeleme
                        if (ad.isBlank() || ad.contains("embedding") || ad.contains("aqa")) continue
                        cikti.add(ad)
                    }
                    // Yeni sürümler üstte görünsün
                    true to cikti.sortedByDescending { it }
                }
                Provider.OPENAI -> {
                    val conn = (URL("https://api.openai.com/v1/models").openConnection()
                        as HttpsURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 15000
                        readTimeout = 25000
                        setRequestProperty("Authorization", "Bearer $key")
                    }
                    val kod = conn.responseCode
                    val akis = if (kod in 200..299) conn.inputStream else conn.errorStream
                    val cevap = BufferedReader(InputStreamReader(akis, Charsets.UTF_8))
                        .use { it.readText() }
                    conn.disconnect()
                    if (kod !in 200..299) return false to emptyList()

                    val dizi = JSONObject(cevap).optJSONArray("data") ?: return false to emptyList()
                    val cikti = mutableListOf<String>()
                    for (i in 0 until dizi.length()) {
                        val ad = dizi.optJSONObject(i)?.optString("id", "").orEmpty()
                        if (ad.startsWith("gpt-") && !ad.contains("audio") &&
                            !ad.contains("realtime") && !ad.contains("tts") &&
                            !ad.contains("image")
                        ) cikti.add(ad)
                    }
                    true to cikti.sortedDescending()
                }
                Provider.OPENROUTER -> {
                    // v7.25: OpenRouter listesi anahtarsız da alınabilir
                    val conn = (URL("https://openrouter.ai/api/v1/models").openConnection()
                        as HttpsURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 15000
                        readTimeout = 30000
                        setRequestProperty("Authorization", "Bearer $key")
                    }
                    val kod = conn.responseCode
                    val akis = if (kod in 200..299) conn.inputStream else conn.errorStream
                    val cevap = BufferedReader(InputStreamReader(akis, Charsets.UTF_8))
                        .use { it.readText() }
                    conn.disconnect()
                    if (kod !in 200..299) return false to emptyList()

                    val dizi = JSONObject(cevap).optJSONArray("data")
                        ?: return false to emptyList()
                    val ucretsiz = mutableListOf<String>()
                    val ucretli = mutableListOf<String>()
                    for (i in 0 until dizi.length()) {
                        val m = dizi.optJSONObject(i) ?: continue
                        val ad = m.optString("id", "")
                        if (ad.isBlank() || ad.endsWith(":batch")) continue
                        // Yalnızca metin üretenler
                        val mim = m.optJSONObject("architecture")
                            ?.optJSONArray("output_modalities")
                        var metinVar = mim == null
                        if (mim != null) {
                            for (j in 0 until mim.length()) {
                                if (mim.optString(j) == "text") { metinVar = true; break }
                            }
                        }
                        if (!metinVar) continue
                        if (ad.endsWith(":free")) ucretsiz.add(ad) else ucretli.add(ad)
                    }
                    // Ücretsizler üstte görünsün — kredi harcamak istemeyen seçsin
                    true to (ucretsiz.sorted() + ucretli.sorted())
                }
                else -> false to emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.w("AiClient", "Model listesi alınamadı", e)
            false to emptyList()
        }
    }

    /** HTTP hatasını anlaşılır Türkçe mesaja çevirir. */
    private fun humanError(context: Context, code: Int, raw: String): String {
        val detail = try {
            JSONObject(raw).optJSONObject("error")?.optString("message") ?: ""
        } catch (_: Exception) {
            ""
        }
        return when {
            // Gemini geçersiz anahtarda 400 döndürür
            code == 400 && detail.contains("API key", ignoreCase = true) ->
                context.getString(R.string.ai_err_key_bad)
            code == 400 -> context.getString(R.string.ai_err_bad_request, detail.take(160))
            else -> whenCode(context, code, detail)
        }
    }

    private fun whenCode(context: Context, code: Int, detail: String): String {
        return when (code) {
            401, 403 -> context.getString(R.string.ai_err_key_bad)
            // v7.25: OpenRouter kredi bittiğinde 402 döner
            402 -> context.getString(R.string.ai_err_credit)
            404 -> context.getString(R.string.ai_err_model, detail)
            408 -> context.getString(R.string.ai_err_server)
            429 -> context.getString(R.string.ai_err_quota)
            in 500..599 -> context.getString(R.string.ai_err_server)
            else -> context.getString(R.string.ai_err_http, code, detail.take(160))
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // v7.78 — SERBEST İSTEMLİ GÖRSEL SORGUSU
    // ═══════════════════════════════════════════════════════════════

    /**
     * Bir görseli **çağıranın verdiği istemle** modele sorar.
     *
     * [konuOku] el yazısı okumaya sabitlenmiştir (OKUMA_TALIMATI + konu
     * ayrıştırma). Kanıt denetimi ([KanitDenetci]) bambaşka bir çıktı
     * istediği için bu genel giriş eklendi: istem dışarıdan gelir, ham
     * metin döner, yorumlamayı çağıran yapar.
     *
     * Sağlayıcı/model düşme zinciri [konuOku] ile aynıdır.
     */
    fun gorselDenetim(context: Context, base64Jpeg: String, istem: String): Result {
        if (!AiSettings.isOnlineMode(context)) {
            return Result(false, context.getString(R.string.ai_err_offline_mode))
        }
        if (!isOnline(context)) {
            return Result(false, context.getString(R.string.ai_err_no_net))
        }
        val key = AiSettings.getApiKey(context)
        if (key.isBlank()) {
            return Result(false, context.getString(R.string.ai_err_no_key))
        }

        val provider = Provider.fromId(AiSettings.getProviderId(context))
        val model = gorselModeli(context)
        if (model.isBlank()) {
            return Result(
                false, context.getString(R.string.ai_err_no_free_model, provider.label)
            )
        }

        return try {
            if (provider == Provider.GEMINI) {
                geminiGorselIstem(context, key, model, base64Jpeg, istem)
            } else {
                openAiGorselIstem(context, provider, key, model, base64Jpeg, istem)
            }
        } catch (e: Exception) {
            Result(false, context.getString(R.string.ai_err_generic, e.message ?: "bilinmeyen"))
        }
    }

    /** Serbest istemli Gemini görsel çağrısı. */
    private fun geminiGorselIstem(
        context: Context,
        key: String,
        model: String,
        base64Jpeg: String,
        istem: String
    ): Result {
        val url = "${Provider.GEMINI.endpoint}/$model:generateContent?key=$key"

        val parcalar = JSONArray()
            .put(JSONObject().put("text", istem))
            .put(
                JSONObject().put(
                    "inline_data",
                    JSONObject()
                        .put("mime_type", "image/jpeg")
                        .put("data", base64Jpeg)
                )
            )

        val body = JSONObject()
            .put(
                "contents",
                JSONArray().put(JSONObject().put("role", "user").put("parts", parcalar))
            )
            .put(
                "generationConfig",
                JSONObject()
                    // Yargı istiyoruz, yaratıcılık değil
                    .put("temperature", 0.1)
                    .put("topP", 0.5)
                    .put("maxOutputTokens", tokenButcesi(model, 1024))
                    .put("responseMimeType", "application/json")
                    .apply { dusunmeAyari(model)?.let { put("thinkingConfig", it) } }
            )
            .toString()

        val conn = (URL(url).openConnection() as HttpsURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 25000
            readTimeout = 90000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val response = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
        conn.disconnect()

        if (code !in 200..299) return Result(false, humanError(context, code, response))

        val candidate = JSONObject(response).optJSONArray("candidates")?.optJSONObject(0)
        val text = candidate?.optJSONObject("content")?.optJSONArray("parts")
            ?.optJSONObject(0)?.optString("text")?.trim()
        return if (!text.isNullOrBlank()) Result(true, text)
        else Result(false, context.getString(R.string.ai_err_empty))
    }

    /** Serbest istemli OpenAI uyumlu görsel çağrısı. */
    private fun openAiGorselIstem(
        context: Context,
        provider: Provider,
        key: String,
        model: String,
        base64Jpeg: String,
        istem: String
    ): Result {
        val icerik = JSONArray()
            .put(JSONObject().put("type", "text").put("text", istem))
            .put(
                JSONObject()
                    .put("type", "image_url")
                    .put(
                        "image_url",
                        JSONObject().put("url", "data:image/jpeg;base64,$base64Jpeg")
                    )
            )

        val body = JSONObject()
            .put("model", model)
            .put(
                "messages",
                JSONArray().put(JSONObject().put("role", "user").put("content", icerik))
            )
            .put("temperature", 0.1)
            .put("max_tokens", 1024)
            .toString()

        val hedef = provider.endpoint.ifBlank { AiSettings.getCustomEndpoint(context) }
        val conn = (URL(hedef).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 25000
            readTimeout = 90000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Authorization", "Bearer $key")
        }
        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val response = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
        conn.disconnect()

        if (code !in 200..299) return Result(false, humanError(context, code, response))

        val text = JSONObject(response).optJSONArray("choices")?.optJSONObject(0)
            ?.optJSONObject("message")?.optString("content")?.trim()
        return if (!text.isNullOrBlank()) Result(true, text)
        else Result(false, context.getString(R.string.ai_err_empty))
    }
}
