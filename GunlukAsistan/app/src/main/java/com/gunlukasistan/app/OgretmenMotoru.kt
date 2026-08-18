package com.gunlukasistan.app

import android.content.Context
import org.json.JSONObject

/**
 * v7.37 — Özel öğretmen modunun beyni.
 *
 * ── DersAsistan'dan farkı ──
 * DersAsistan (v7.31): kullanıcı sorar → model cevaplar. Tek atış, hafızasız.
 * OgretmenMotoru: **model yönetir**. Konuyu parçalara böler, anlatır,
 * soru sorar, cevaba göre seviyeyi ayarlar, kaldığı yeri hatırlar.
 *
 * ── Akış ──
 *   1. ANLAT   → bir adım anlatılır + anlama sorusu sorulur
 *   2. CEVAPLA → kullanıcının cevabı değerlendirilir
 *   3a. Doğruysa  → seviye +1, sonraki adım
 *   3b. Yanlışsa  → seviye -1, AYNI konu daha basit anlatılır
 *   4. Ders bitince özet + zayıf noktalar
 *
 * ── Neden JSON çıktı? ──
 * Modelin serbest metni ayrıştırmak kırılgan. JSON isteyip savunmacı
 * ayrıştırma yapıyoruz; bozuk gelirse düz metne düşülüyor (asla çökmez).
 */
object OgretmenMotoru {

    private const val TAG = "OgretmenMotoru"

    /** Bir dersin kaç adımda anlatılacağı (seviyeye göre değişir). */
    private const val ADIM_AZ = 5
    private const val ADIM_COK = 9

    /**
     * Öğretmenin tek bir hamlesi.
     *
     * @param anlatim ekranda gösterilecek ders metni
     * @param soru kullanıcıya sorulan anlama sorusu (boşsa soru yok)
     * @param secenekler çoktan seçmeli şıklar (boşsa serbest cevap)
     * @param dogruIndeks doğru şıkkın sırası (-1 = serbest cevap)
     * @param konuBasligi bu adımın konusu — zayıf nokta kaydı için
     * @param sonAdim ders bitti mi
     */
    class Ders(
        val ok: Boolean,
        val anlatim: String,
        val soru: String = "",
        val secenekler: List<String> = emptyList(),
        val dogruIndeks: Int = -1,
        val konuBasligi: String = "",
        val sonAdim: Boolean = false,
        val hata: String = ""
    )

    /** Kullanıcının cevabının değerlendirmesi. */
    class Degerlendirme(
        val ok: Boolean,
        val dogruMu: Boolean,
        val geriBildirim: String,
        val hata: String = ""
    )

    // ═══════════════════════════════════════════════════════════════
    // 1) ANLATIM ADIMI
    // ═══════════════════════════════════════════════════════════════

    /**
     * Sıradaki anlatım adımını üretir.
     *
     * @param basitAnlat true ise aynı konu daha basit anlatılır (yanlış sonrası)
     */
    fun anlat(
        context: Context,
        lessonId: Long,
        dersAdi: String,
        assetPath: String,
        adim: Int,
        basitAnlat: Boolean = false,
        oncekiOzet: String = ""
    ): Ders {
        val onKontrol = hazirMi(context)
        if (onKontrol != null) return Ders(false, "", hata = onKontrol)

        val seviye = OgretmenStore.seviye(context, lessonId)
        val toplamAdim = if (seviye >= 4) ADIM_AZ else ADIM_COK

        // Ders metni varsa RAG bağlamı kur — yoksa modelin genel bilgisi
        val baglam = if (assetPath.isNotBlank()) {
            DersMetni.metniAl(context, assetPath)?.let {
                DersMetni.baglamHazirla(it, tahminiSayfa(adim, toplamAdim, it))
            }.orEmpty()
        } else ""

        val istem = anlatimIstemi(
            context, dersAdi, baglam, adim, toplamAdim, seviye, basitAnlat, oncekiOzet
        )

        val sonuc = AiClient.chat(context, istem)
        if (!sonuc.ok) return Ders(false, "", hata = sonuc.text)

        return anlatimAyristir(sonuc.text, adim, toplamAdim)
    }

    /** Adım numarasından yaklaşık PDF sayfası tahmin eder. */
    private fun tahminiSayfa(adim: Int, toplamAdim: Int, metin: String): Int {
        val sayfaSayisi = DersMetni.sayfalar(metin).size
        if (sayfaSayisi <= 1) return 0
        return ((adim.toFloat() / toplamAdim) * sayfaSayisi).toInt()
            .coerceIn(0, sayfaSayisi - 1)
    }

    private fun anlatimIstemi(
        context: Context,
        dersAdi: String,
        baglam: String,
        adim: Int,
        toplamAdim: Int,
        seviye: Int,
        basitAnlat: Boolean,
        oncekiOzet: String
    ): String {
        val seviyeTarifi = when (seviye) {
            1 -> "HİÇ BİLMİYOR. Sıfırdan, terim kullanmadan, günlük dille anlat. " +
                "Her teknik kelimeyi parantez içinde açıkla."
            2 -> "YENİ BAŞLIYOR. Temel terimleri açıklayarak anlat, örnek ver."
            3 -> "ORTA SEVİYE. Terimleri biliyor, detaya inebilirsin."
            4 -> "İYİ BİLİYOR. Doğrudan konuya gir, incelikleri anlat."
            else -> "İLERİ DÜZEY. Uzman diliyle konuş, istisnaları ve püf noktaları anlat."
        }

        val basitNot = if (basitAnlat) {
            "\n\nÖNEMLİ: Kullanıcı bir önceki soruya YANLIŞ cevap verdi. " +
                "AYNI konuyu şimdi DAHA BASİT, farklı bir açıdan, somut örnekle " +
                "yeniden anlat. Yeni konuya GEÇME.\n"
        } else ""

        val gecmis = if (oncekiOzet.isBlank()) "" else
            "\n\nÖNCEKİ ADIMDA ŞUNU ANLATTIN:\n" + oncekiOzet + "\nBunu tekrarlama, devam et.\n"

        val kaynakBolumu = if (baglam.isBlank()) {
            "Bu ders için hazır metin yok — kendi bilginle anlat, ama uydurma."
        } else {
            "=== DERS KAYNAĞI ===\n" + baglam + "\n=== KAYNAK SONU ===\n" +
                "Anlatımını ÖNCELİKLE bu kaynağa dayandır."
        }

        val sonAdimMi = adim >= toplamAdim - 1

        return """Sen deneyimli bir eğitmensin. "$dersAdi" dersini
birebir özel ders gibi anlatıyorsun. Konu hangi alandan olursa olsun
(mühendislik, dil, tarih, sınav hazırlık, kişisel beceri...) o alanın
uzmanı gibi davran.

ÖĞRENCİ SEVİYESİ: $seviyeTarifi

$kaynakBolumu$gecmis$basitNot

GÖREV: Bu dersin ${adim + 1}. adımını anlat (toplam $toplamAdim adım).
${if (sonAdimMi) "Bu SON adım — dersi toparla ve özetle." else ""}

Yanıtını SADECE şu JSON biçiminde ver, başka hiçbir şey yazma:
{
  "konu": "bu adımın kısa başlığı",
  "anlatim": "anlatım metni — 3 ile 6 paragraf, madde işareti kullanabilirsin",
  "soru": "anladığını ölçen tek soru",
  "secenekler": ["A şıkkı", "B şıkkı", "C şıkkı", "D şıkkı"],
  "dogru": 0,
  "son": ${if (sonAdimMi) "true" else "false"}
}

KURALLAR:
1. "anlatim" Türkçe, akıcı ve ÖĞRETİCİ olsun. Sohbet etme, DERS ANLAT.
2. Somut örnek ver — dersin ALANINA uygun olsun (teknik konuda proje/ölçü,
   dil dersinde cümle, tarihte olay, sınavda soru tipi).
3. "soru" anlatılanı ölçsün — ezber değil, ANLAMA sorusu olsun.
4. "secenekler" tam 4 şık, biri kesin doğru, diğerleri mantıklı ama yanlış.
5. "dogru" doğru şıkkın 0 tabanlı sırası (0,1,2 veya 3).
6. Sayı, standart veya yönetmelik maddesi UYDURMA.
7. JSON dışında hiçbir metin yazma. Kod bloğu işareti kullanma."""
    }

    /**
     * Model çıktısını Ders nesnesine çevirir.
     * Savunmacı: JSON bozuksa metni olduğu gibi anlatım olarak gösterir.
     */
    private fun anlatimAyristir(ham: String, adim: Int, toplamAdim: Int): Ders {
        val temizMetin = kodBloguTemizle(ham)

        try {
            val bas = temizMetin.indexOf('{')
            val son = temizMetin.lastIndexOf('}')
            if (bas >= 0 && son > bas) {
                val o = JSONObject(temizMetin.substring(bas, son + 1))
                val anlatim = o.optString("anlatim").trim()
                if (anlatim.isNotBlank()) {
                    val secDizi = o.optJSONArray("secenekler")
                    val secenekler = mutableListOf<String>()
                    if (secDizi != null) {
                        for (i in 0 until secDizi.length()) {
                            secDizi.optString(i).trim().takeIf { it.isNotBlank() }
                                ?.let { secenekler.add(it) }
                        }
                    }
                    // Şık sayısı 2'den azsa serbest cevaba düş
                    val gecerliSecenek = if (secenekler.size >= 2) secenekler else emptyList()
                    val dogru = o.optInt("dogru", -1)
                        .let { if (gecerliSecenek.isEmpty()) -1 else it.coerceIn(0, gecerliSecenek.size - 1) }

                    return Ders(
                        ok = true,
                        anlatim = anlatim,
                        soru = o.optString("soru").trim(),
                        secenekler = gecerliSecenek,
                        dogruIndeks = dogru,
                        konuBasligi = o.optString("konu").trim(),
                        sonAdim = o.optBoolean("son", adim >= toplamAdim - 1)
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Anlatım JSON'u ayrıştırılamadı, düz metne düşülüyor", e)
        }

        // Düz metin yedeği — model JSON vermediyse yine de ders göster
        val duz = temizMetin.trim()
        if (duz.isBlank()) {
            return Ders(false, "", hata = "bos")
        }
        return Ders(
            ok = true,
            anlatim = duz,
            sonAdim = adim >= toplamAdim - 1
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // 2) SERBEST CEVAP DEĞERLENDİRME
    // ═══════════════════════════════════════════════════════════════

    /**
     * Kullanıcı yazıyla cevap verdiyse modele değerlendirtir.
     * Çoktan seçmelide bu çağrılmaz — karşılaştırma yerelde yapılır.
     */
    fun degerlendir(
        context: Context,
        dersAdi: String,
        soru: String,
        beklenen: String,
        kullaniciCevabi: String
    ): Degerlendirme {
        if (kullaniciCevabi.isBlank()) {
            return Degerlendirme(false, false, "", context.getString(R.string.tut_empty_answer))
        }
        val onKontrol = hazirMi(context)
        if (onKontrol != null) return Degerlendirme(false, false, "", onKontrol)

        val istem = """"$dersAdi" dersinde öğrenciye şu soruyu sordun:
SORU: $soru
${if (beklenen.isBlank()) "" else "BEKLENEN CEVAP: " + beklenen}

ÖĞRENCİNİN CEVABI: $kullaniciCevabi

Bu cevabı değerlendir. Yanıtını SADECE şu JSON biçiminde ver:
{"dogru": true, "geri_bildirim": "kısa açıklama"}

KURALLAR:
1. Cevap özünde doğruysa "dogru": true — kelimesi kelimesine aynı olmasa da.
2. Eksik ama yanlış değilse doğru say, eksiği geri bildirimde söyle.
3. "geri_bildirim" en fazla 3 cümle, Türkçe, YAPICI olsun.
4. Yanlışsa neden yanlış olduğunu kısaca açıkla.
5. JSON dışında hiçbir şey yazma."""

        val sonuc = AiClient.chat(context, istem)
        if (!sonuc.ok) return Degerlendirme(false, false, "", sonuc.text)

        val temiz = kodBloguTemizle(sonuc.text)
        try {
            val bas = temiz.indexOf('{')
            val son = temiz.lastIndexOf('}')
            if (bas >= 0 && son > bas) {
                val o = JSONObject(temiz.substring(bas, son + 1))
                return Degerlendirme(
                    ok = true,
                    dogruMu = o.optBoolean("dogru", false),
                    geriBildirim = o.optString("geri_bildirim").trim()
                        .ifBlank { context.getString(R.string.tut_no_feedback) }
                )
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Değerlendirme ayrıştırılamadı", e)
        }

        // Ayrıştırılamadıysa: metinde olumlu ifade ara (savunmacı)
        val dusuk = temiz.lowercase()
        val olumlu = dusuk.contains("doğru") && !dusuk.contains("yanlış") &&
            !dusuk.contains("değil")
        return Degerlendirme(true, olumlu, temiz.take(400))
    }

    // ═══════════════════════════════════════════════════════════════
    // 3) DERS SONU ÖZETİ
    // ═══════════════════════════════════════════════════════════════

    /** Ders bitince kişiselleştirilmiş kapanış metni üretir. */
    fun bitirmeOzeti(
        context: Context,
        dersAdi: String,
        dogru: Int,
        yanlis: Int,
        zayifNoktalar: List<String>
    ): String {
        val yuzde = if (dogru + yanlis == 0) 0 else dogru * 100 / (dogru + yanlis)
        val sb = StringBuilder()

        sb.append(context.getString(R.string.tut_finish_head, dersAdi)).append("\n\n")
        sb.append(context.getString(R.string.tut_finish_score, dogru, dogru + yanlis, yuzde))

        val yorum = when {
            yuzde >= 85 -> R.string.tut_finish_great
            yuzde >= 60 -> R.string.tut_finish_good
            else -> R.string.tut_finish_retry
        }
        sb.append("\n\n").append(context.getString(yorum))

        if (zayifNoktalar.isNotEmpty()) {
            sb.append("\n\n").append(context.getString(R.string.tut_finish_weak)).append("\n")
            zayifNoktalar.take(6).forEach { sb.append("• ").append(it).append("\n") }
        }
        return sb.toString().trim()
    }

    // ═══════════════════════════════════════════════════════════════
    // ORTAK
    // ═══════════════════════════════════════════════════════════════

    /** Yapay zekâ kullanılabilir mi? Hata varsa mesajı döner, yoksa null. */
    private fun hazirMi(context: Context): String? {
        if (!AiSettings.isOnlineMode(context)) {
            return context.getString(R.string.ai_err_offline_mode)
        }
        if (!AiClient.isOnline(context)) {
            return context.getString(R.string.ai_err_no_net)
        }
        if (AiSettings.getApiKey(context).isBlank() &&
            AiSettings.anahtarliSaglayicilar(context).isEmpty()
        ) {
            return context.getString(R.string.ai_err_no_key)
        }
        return null
    }

    /** ```json ... ``` sarmalını ve komut satırlarını temizler. */
    private fun kodBloguTemizle(ham: String): String {
        // Öğretmen komut üretmemeli — sızarsa ayıkla
        val (komutsuz, _) = AsistanKomut.ayikla(ham)
        var s = komutsuz.trim().ifBlank { ham.trim() }
        if (s.startsWith("```")) {
            s = s.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        }
        return s
    }
}
