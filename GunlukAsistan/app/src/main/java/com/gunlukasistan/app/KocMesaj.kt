package com.gunlukasistan.app

import android.content.Context
import org.json.JSONObject

/**
 * v7.78 — Koçun yapay zekâ destekli sözleri.
 *
 * ── Neden ayrı ──
 * [Koc] veri ve kural katmanı; ağ bilmez, her koşulda çalışır.
 * Buradaki her şey **isteğe bağlı süs**: AI yoksa uygulama aynen çalışır,
 * sadece cümleler hazır kalıplardan gelir.
 *
 * ── Neden önceden üretilip saklanıyor ──
 * Bildirim `BroadcastReceiver` içinde oluşuyor; orada 5-10 saniyelik ağ
 * isteği beklemek sistemin alıcıyı öldürmesine yol açar. Bu yüzden mesaj
 * kullanıcı uygulamayı açtığında arka planda üretilip saklanır, bildirim
 * anında hazır metin okunur ([hazirMesaj]).
 */
object KocMesaj {

    private const val TAG = "KocMesaj"
    private const val PREF = "koc_mesaj_v1"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ═══════════════════════════════════════════════════════════════
    // HAZIR MESAJ ÖNBELLEĞİ
    // ═══════════════════════════════════════════════════════════════

    /** Bildirimin kullanacağı, önceden üretilmiş cümle. */
    fun hazirMesaj(context: Context): String {
        val metin = prefs(context).getString("mesaj", "") ?: ""
        val zaman = prefs(context).getLong("zaman", 0L)
        // 12 saatten eski mesaj bayat — kullanma
        return if (metin.isNotBlank() &&
            System.currentTimeMillis() - zaman < 12 * 3600_000L
        ) metin else ""
    }

    private fun mesajKaydet(context: Context, metin: String) {
        prefs(context).edit()
            .putString("mesaj", metin)
            .putLong("zaman", System.currentTimeMillis())
            .apply()
    }

    /**
     * Arka planda yeni bir motivasyon cümlesi üretir.
     *
     * Uygulama açılışında çağrılır. Sessizdir: başarısız olursa hiçbir şey
     * olmaz, bildirim hazır kalıba düşer.
     */
    fun arkaPlandaUret(context: Context) {
        if (!Koc.acikMi(context)) return
        if (!AiSettings.isReady(context)) return
        // Günde bir kez yeter — kota harcamayalım
        if (hazirMesaj(context).isNotBlank()) return

        Performans.arkaPlan {
            runCatching {
                val istem = motivasyonIstemi(context)
                // v7.99: aynı durum için tekrar üretme (öneri 4)
                val sonuc = AiOnbellek.getir(context, istem, AiOnbellek.GUN) {
                    AiClient.sadeIstek(context, istem, butce = 220)
                }
                if (sonuc.ok) {
                    val temiz = sonuc.text.trim()
                        .removePrefix("\"").removeSuffix("\"")
                        .lines().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
                    if (temiz.length in 8..300) mesajKaydet(context, temiz)
                }
            }
        }
    }

    private fun motivasyonIstemi(context: Context): String {
        val karne = Koc.karne(context, 14)
        val kalan = Koc.bugunKalan(context)
        val ton = when (Koc.sertlik(context)) {
            Koc.SERT_NAZIK -> "Nazik, destekleyici ve anlayışlı"
            Koc.SERT_ACIMASIZ -> "Sert, tavizsiz, sorgulayıcı. Yağcılık yapma, gerçeği söyle"
            else -> "Kararlı, net, hesap soran ama aşağılamayan"
        }

        // v7.79: program bağlamı — model başka derse kaymasın
        val program = Mufredat.aiBaglami(context)
        val programBolumu = if (program.isBlank()) "" else "\n" + program + "\n"

        return """
Sen kullanıcının özel çalışma koçusun. Ona ders çalıştırmak için tek bir cümle yazacaksın.
$programBolumu
DURUM:
- Bugün kalan hedef: $kalan dakika
- Üst üste çalışma serisi: ${karne.seri} gün
- Son 14 günde başarı: ${karne.basariliGun}/${karne.toplamGun} gün
- Birikmiş borç: ${karne.borc} dakika

TON: $ton

Kurallar:
- SADECE tek cümle yaz, tırnak veya açıklama ekleme
- En fazla 20 kelime
- Türkçe, kullanıcıya "sen" diye hitap et
- Klişe motivasyon sözü kullanma ("başarı yolculuktur" gibi), somut duruma değin
- Varsa ÇALIŞILAN DERSİN ADINI kullan, başka ders adı GEÇİRME
        """.trim()
    }

    // ═══════════════════════════════════════════════════════════════
    // MAZERET DEĞERLENDİRME
    // ═══════════════════════════════════════════════════════════════

    /**
     * Mazeret kararı.
     *
     * @param kabul koç mazereti geçerli buldu mu
     * @param cevap kullanıcıya söylenecek karşılık
     */
    class MazeretKarari(
        val calisti: Boolean,
        val kabul: Boolean,
        val cevap: String
    )

    /**
     * Kullanıcının mazeretini yapay zekâya değerlendirtir.
     * **Ağ isteği yapar — arka planda çağır.**
     *
     * Kabul edilen mazerette borç yazılmaz ve seri korunur; bu yüzden
     * model bilerek şüpheci olması için yönlendirilir. Yoksa her akşam
     * "yorgundum" yazıp sistem anlamsızlaşır.
     */
    fun mazeretDegerlendir(context: Context, mazeret: String): MazeretKarari {
        if (mazeret.isBlank()) {
            return MazeretKarari(true, false, context.getString(R.string.koc_mz_bos))
        }
        if (!AiSettings.isReady(context)) {
            // AI yoksa yerel kural: sertliğe göre karar
            return yerelMazeret(context, mazeret)
        }

        return try {
            val sonuc = AiClient.sadeIstek(context, mazeretIstemi(context, mazeret), butce = 300)
            if (!sonuc.ok) return yerelMazeret(context, mazeret)

            val json = jsonAyikla(sonuc.text)
                ?: return yerelMazeret(context, mazeret)

            MazeretKarari(
                calisti = true,
                kabul = json.optBoolean("kabul", false),
                cevap = json.optString("cevap").trim().ifBlank {
                    context.getString(R.string.koc_mz_varsayilan)
                }
            )
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Mazeret değerlendirilemedi", e)
            yerelMazeret(context, mazeret)
        }
    }

    private fun mazeretIstemi(context: Context, mazeret: String): String {
        val karne = Koc.karne(context, 14)
        val gecmisMazeretler = Koc.gunKayitlari(context)
            .takeLast(14)
            .filter { it.mazeret.isNotBlank() }
            .joinToString(" | ") { it.mazeret.take(60) }

        val sertlikNotu = when (Koc.sertlik(context)) {
            Koc.SERT_NAZIK -> "Anlayışlı ol, gerçek bir engel varsa kabul et."
            Koc.SERT_ACIMASIZ ->
                "Çok şüpheci ol. Sadece hastalık, acil durum, kaza gibi GERÇEK engelleri " +
                    "kabul et. Yorgunluk, isteksizlik, 'vaktim olmadı', 'canım istemedi' " +
                    "gibi mazeretleri KESİNLİKLE reddet."
            else ->
                "Dengeli ol. Gerçek engelleri kabul et, tembellik bahanelerini reddet."
        }

        val tekrarNotu = if (gecmisMazeretler.isBlank()) "" else
            "\n\nSON 14 GÜNDEKİ MAZERETLERİ: $gecmisMazeretler" +
                "\nAynı mazereti tekrarlıyorsa bunu yüzüne vur ve reddet."

        return """
Sen kullanıcının çalışma koçusun. Bugün ${karne.borc} dakika borcu var ve hedefini tutturamadı.
Şu mazereti sundu:

"$mazeret"

$sertlikNotu$tekrarNotu

SADECE şu JSON'u döndür:
{"kabul": true veya false, "cevap": "kullanıcıya söyleyeceğin karşılık, en fazla 25 kelime, Türkçe, 'sen' diye hitap et"}
        """.trim()
    }

    /**
     * AI yokken mazeret kararı.
     *
     * Anahtar kelime tabanlı basit bir süzgeç. Mükemmel değil ama
     * çevrimdışıyken hiç değerlendirmemekten iyi.
     */
    private fun yerelMazeret(context: Context, mazeret: String): MazeretKarari {
        val m = mazeret.lowercase()

        val gercekEngeller = listOf(
            "hasta", "ateş", "grip", "ameliyat", "hastane", "acil", "kaza",
            "cenaze", "vefat", "deprem", "elektrik yok", "internet yok"
        )
        val bahaneler = listOf(
            "yorgun", "canım istemedi", "isteksiz", "unuttum", "vakit yok",
            "vaktim olmadı", "keyfim yok", "sonra", "yarın", "üşendim", "tembellik"
        )

        val gercek = gercekEngeller.any { m.contains(it) }
        val bahane = bahaneler.any { m.contains(it) }

        return when {
            gercek && !bahane -> MazeretKarari(
                true, true, context.getString(R.string.koc_mz_kabul_yerel)
            )
            bahane -> MazeretKarari(
                true, false, context.getString(R.string.koc_mz_red_yerel)
            )
            // Belirsiz: sertliğe göre
            else -> MazeretKarari(
                true,
                Koc.sertlik(context) == Koc.SERT_NAZIK,
                context.getString(R.string.koc_mz_varsayilan)
            )
        }
    }

    private fun jsonAyikla(ham: String): JSONObject? {
        val temiz = ham.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()
        runCatching { return JSONObject(temiz) }
        val bas = temiz.indexOf('{')
        val son = temiz.lastIndexOf('}')
        if (bas in 0 until son) {
            runCatching { return JSONObject(temiz.substring(bas, son + 1)) }
        }
        return null
    }

    // ═══════════════════════════════════════════════════════════════
    // GÜNLÜK PLAN ÖNERİSİ
    // ═══════════════════════════════════════════════════════════════

    /**
     * "Bugün ne çalışayım?" — v7.79'da tamamen değişti.
     *
     * ── Eski davranış (v7.78) ve neden yanlıştı ──
     * Tüm kursların adı modele veriliyor, "3 maddelik plan üret" deniyordu.
     * Model her gün farklı derslerden rastgele öneriler üretiyordu; hiçbiri
     * bitmiyordu. Kullanıcı buna "karmakarışık program" dedi — haklıydı.
     *
     * ── Yeni davranış ──
     * Yalnızca [Mufredat.aktifDers] üzerine konuşulur. Model program
     * yapmaz; **tek dersi bugün nasıl ilerleteceğini** anlatır.
     */
    fun bugunNeCalisayim(context: Context): String {
        if (!Mufredat.secildiMi(context)) {
            return context.getString(R.string.mf_once_kurs_sec)
        }
        val aktif = Mufredat.aktifAdim(context)
            ?: return context.getString(R.string.mf_program_bitti, Mufredat.programAdi(context))

        if (!AiSettings.isReady(context)) {
            // AI yoksa da işe yarar bir cevap ver — dersin kendi bilgisi yeter
            return yerelDersPlani(context, aktif)
        }

        return try {
            val hedef = Koc.bugunKalan(context).coerceAtLeast(15)
            val istem = """
${Mufredat.aiBaglami(context)}

Öğrencinin bugün $hedef dakikası var.

GÖREV: Sadece "${aktif.baslik}" konusunu bugün nasıl ilerleteceğini anlat.

Kurallar:
- BAŞKA KONU ÖNERME. Sadece bunu.
- Bunu $hedef dakikaya bölünmüş 3 somut adıma ayır.
- Her adım tek satır, başına süre yaz. Örnek: "20 dk — ..."
- Giriş cümlesi, başlık veya kapanış yazma. Sadece 3 satır.
- Türkçe yaz.
            """.trim()
            // v7.99: gün içinde aynı plan tekrar üretilmesin
            val sonuc = AiOnbellek.getir(context, istem, AiOnbellek.GUN) {
                AiClient.sadeIstek(context, istem, butce = 400)
            }
            if (sonuc.ok && sonuc.text.isNotBlank()) sonuc.text.trim()
            else yerelDersPlani(context, aktif)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Plan üretilemedi", e)
            yerelDersPlani(context, aktif)
        }
    }

    /** AI yokken dersin kendi verisinden basit plan. */
    private fun yerelDersPlani(context: Context, ders: Mufredat.Adim): String {
        val hedef = Koc.bugunKalan(context).coerceAtLeast(15)
        val pay = (hedef / 3).coerceAtLeast(5)
        val sb = StringBuilder()
        sb.append(context.getString(R.string.mf_yerel_p1, pay, ders.baslik)).append("\n")
        sb.append(context.getString(R.string.mf_yerel_p2, pay)).append("\n")
        sb.append(context.getString(R.string.mf_yerel_p3, hedef - 2 * pay))
        if (ders.aciklama.isNotBlank()) {
            sb.append("\n\n").append(ders.aciklama.take(200))
        }
        return sb.toString()
    }

    // ═══════════════════════════════════════════════════════════════
    // v7.79 — DERS BAZLI HESAP SORMA
    // ═══════════════════════════════════════════════════════════════

    /**
     * Aktif ders üzerinden hesap sorar: "bugün bu derste ne öğrendin?"
     *
     * Kullanıcının isteği: "sadece o konuyu hesap vb şeyler yapsın".
     * Bu yüzden soru genel değil, **dersin içeriğinden** üretilir.
     *
     * @return sorulacak soru, üretilemezse boş
     */
    fun dersHesabiSorusu(context: Context): String {
        val aktif = Mufredat.aktifAdim(context) ?: return ""
        if (!AiSettings.isReady(context)) {
            return context.getString(R.string.mf_hesap_yerel, aktif.baslik)
        }
        return try {
            val istem = """
${Mufredat.aiBaglami(context)}

GÖREV: Öğrenciye bugün "${aktif.baslik}" konusundan ne öğrendiğini ölçen
TEK bir soru sor.

Kurallar:
- Sadece bu konudan sor, başka konuya girme
- Konunun alanını başlıktan anla
- Ezber değil ANLAMA sorusu olsun
- Tek cümle, en fazla 25 kelime
- Soru dışında hiçbir şey yazma
- Türkçe
            """.trim()
            val sonuc = AiClient.sadeIstek(context, istem, butce = 200)
            if (sonuc.ok && sonuc.text.isNotBlank()) {
                sonuc.text.trim().lines().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
            } else context.getString(R.string.mf_hesap_yerel, aktif.baslik)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Ders sorusu üretilemedi", e)
            context.getString(R.string.mf_hesap_yerel, aktif.baslik)
        }
    }

    /**
     * Öğrencinin ders hesabına verdiği cevabı değerlendirir.
     *
     * @return (yeterli mi, koçun karşılığı)
     */
    fun dersCevabiDegerlendir(
        context: Context,
        soru: String,
        cevap: String
    ): Pair<Boolean, String> {
        val aktif = Mufredat.aktifAdim(context)
            ?: return true to ""
        if (cevap.isBlank()) {
            return false to context.getString(R.string.mf_cevap_bos)
        }
        if (!AiSettings.isReady(context)) {
            // AI yoksa uzunluğa bakan basit ölçüt — hiç değerlendirmemekten iyi
            val yeterli = cevap.trim().length >= 25
            return yeterli to context.getString(
                if (yeterli) R.string.mf_cevap_kabul_yerel else R.string.mf_cevap_kisa
            )
        }

        val sertlik = when (Koc.sertlik(context)) {
            Koc.SERT_NAZIK -> "Anlayışlı ol, çaba varsa kabul et."
            Koc.SERT_ACIMASIZ ->
                "Çok katı ol. Yüzeysel, kopyala-yapıştır veya konuyu anlamadığını " +
                    "gösteren cevapları reddet."
            else -> "Dengeli ol. Konuyu anladığını gösteriyorsa kabul et."
        }

        return try {
            val istem = """
${Mufredat.aiBaglami(context)}

Öğrenciye şu soruyu sordun: "$soru"
Öğrencinin cevabı: "$cevap"

$sertlik

SADECE şu JSON'u döndür:
{"yeterli": true veya false, "cevap": "öğrenciye söyleyeceğin karşılık, en fazla 25 kelime, Türkçe, 'sen' diye hitap et"}
            """.trim()
            val sonuc = AiClient.sadeIstek(context, istem, butce = 300)
            if (!sonuc.ok) return true to ""
            val json = jsonAyikla(sonuc.text) ?: return true to ""
            json.optBoolean("yeterli", true) to
                json.optString("cevap").trim().ifBlank {
                    context.getString(R.string.mf_cevap_kabul_yerel)
                }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Ders cevabı değerlendirilemedi", e)
            true to ""
        }
    }
}
