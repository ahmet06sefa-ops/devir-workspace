package com.gunlukasistan.app

import android.content.Context

/**
 * v9.6 — Zayıf nokta radarı (öneri 35).
 *
 * ══════════════════════════════════════════════════════════════════
 * SORUN: VERİ VAR, ANALİZ YOK
 * ══════════════════════════════════════════════════════════════════
 * Uygulama dört ayrı yerde "nerede zorlanıyorsun" bilgisi topluyor:
 *
 *   1. `Hatalarim`   — yanlış yapılan quiz soruları + Leitner kutusu
 *   2. `QuizStore`   — ders bazında quiz sonuçları (yüzde)
 *   3. `KonuTekrar`  — SM-2 kolaylık katsayısı (EF) ve unutma sayısı
 *   4. `Store`       — konu tamamlanma oranları
 *
 * Ama **hiçbiri birleştirilmiyordu**. Kullanıcı "en çok nerede
 * zorlanıyorum" sorusunun cevabını dört ekranı gezerek kendi
 * çıkarmak zorundaydı — kimse yapmıyor.
 *
 * ══════════════════════════════════════════════════════════════════
 * PUANLAMA
 * ══════════════════════════════════════════════════════════════════
 * Her konu için 0-100 arası **zorluk puanı**. Yüksek = daha çok
 * dikkat gerekiyor.
 *
 *   · Hata defteri yoğunluğu      → 35 puan
 *   · Quiz başarı oranı (düşükse) → 30 puan
 *   · SM-2 kolaylık katsayısı     → 25 puan
 *   · Tamamlanma oranı (düşükse)  → 10 puan
 *
 * ── Neden hata defteri en ağır ──
 * Yanlış cevap en doğrudan kanıt. Quiz yüzdesi ortalama; tek bir
 * konudaki zayıflığı gizleyebiliyor. EF ise yalnız tekrar açıksa
 * anlamlı.
 *
 * ── Neden veri yetersizse puan verilmez ──
 * Tek bir yanlış cevaba bakıp "bu konuda çok zayıfsın" demek
 * haksız ve motivasyon kırıcı. En az 3 sinyal gerekiyor.
 */
object ZayifNokta {

    private const val TAG = "ZayifNokta"

    /** Bir konu için puan verebilmek üzere en az kaç sinyal gerekli. */
    private const val EN_AZ_SINYAL = 3

    /**
     * Bir konunun zayıflık analizi.
     *
     * @param puan 0-100, yüksek = daha zayıf
     * @param sinyalSayisi analize giren veri noktası
     * @param sebepler kullanıcıya gösterilecek gerekçeler
     */
    data class Bulgu(
        val konuId: Long,
        val konuAdi: String,
        val puan: Int,
        val sinyalSayisi: Int,
        val hataSayisi: Int,
        val quizYuzde: Int,
        val ef: Double,
        val tamamlanmaYuzde: Int,
        val sebepler: List<String>
    ) {
        val seviye: Int
            get() = when {
                puan >= 65 -> 2   // acil
                puan >= 40 -> 1   // dikkat
                else -> 0         // iyi
            }
    }

    // ══════════════════════════════════════════════════════════

    /**
     * Tüm konuları analiz eder, zayıftan güçlüye sıralar.
     *
     * **Arka planda çağrılmalı** — birkaç depo okuyor.
     *
     * @param enAz yalnız bu puanın üstündekiler (0 = hepsi)
     */
    fun analiz(c: Context, enAz: Int = 0): List<Bulgu> = runCatching {
        val konular = Store.loadTopics(c)
        if (konular.isEmpty()) return emptyList()

        val hatalar = runCatching { Hatalarim.hepsi(c) }.getOrDefault(mutableListOf())
        val quizSonuclari = runCatching { QuizStore.sonuclariYukle(c) }
            .getOrDefault(mutableListOf())
        val tekrarlar = runCatching { KonuTekrar.hepsi(c) }.getOrDefault(mutableListOf())

        val bulgular = mutableListOf<Bulgu>()

        konular.forEach { konu ->
            var puan = 0
            var sinyal = 0
            val sebepler = mutableListOf<String>()

            // ── 1. Hata defteri (35 puan) ──
            //
            // Konu adı hata kaynağında geçiyorsa o hatayı bu konuya
            // sayıyoruz. Kaynak alanı serbest metin ("Türev · Ders 3")
            // olduğu için içerik araması yapılıyor.
            val konuHatalari = hatalar.count { h ->
                h.kaynak.contains(konu.title, ignoreCase = true) ||
                    konu.items.any { m -> h.metin.contains(m.text, ignoreCase = true) }
            }
            if (konuHatalari > 0) {
                sinyal += konuHatalari.coerceAtMost(5)
                // 5 ve üzeri hata tam puan
                puan += (konuHatalari * 7).coerceAtMost(35)
                sebepler.add(c.getString(R.string.zn_sebep_hata, konuHatalari))
            }

            // ── 2. Quiz başarısı (30 puan) ──
            //
            // Konu maddelerinin sanal ders kimlikleri NEGATİF (v7.82).
            // Doğrudan eşleşme yerine konuya ait quiz sonuçlarının
            // ortalaması alınıyor.
            val konuQuizleri = quizSonuclari.filter { s ->
                konu.items.any { m -> s.lessonId == -m.id }
            }
            var quizYuzde = -1
            if (konuQuizleri.isNotEmpty()) {
                quizYuzde = konuQuizleri.map { it.yuzde }.average().toInt()
                sinyal += konuQuizleri.size.coerceAtMost(3)
                if (quizYuzde < 60) {
                    puan += ((60 - quizYuzde) * 30 / 60).coerceAtMost(30)
                    sebepler.add(c.getString(R.string.zn_sebep_quiz, quizYuzde))
                }
            }

            // ── 3. SM-2 kolaylık katsayısı (25 puan) ──
            //
            // Düşük EF = madde tekrar tekrar unutuluyor. En güvenilir
            // uzun vadeli sinyal ama yalnız tekrar açıksa var.
            val konuTekrarlari = tekrarlar.filter { it.konuId == konu.id }
            var ortEf = 2.5
            if (konuTekrarlari.isNotEmpty()) {
                ortEf = konuTekrarlari.map { it.ef }.average()
                val unutma = konuTekrarlari.sumOf { it.unutmaSayisi }
                sinyal += konuTekrarlari.size.coerceAtMost(4)
                if (ortEf < 2.2) {
                    // EF 1.3 (en zor) → 25 puan, 2.2 → 0 puan
                    puan += (((2.2 - ortEf) / 0.9 * 25).toInt()).coerceIn(0, 25)
                    sebepler.add(c.getString(R.string.zn_sebep_ef, unutma))
                }
            }

            // ── 4. Tamamlanma (10 puan) ──
            //
            // Hiç başlanmamış konu "zayıf" değil, sadece yapılmamış.
            // Bu yüzden yalnız BAŞLANMIŞ ama yarım kalmışlar sayılıyor.
            val yuzde = konu.percent
            if (konu.items.isNotEmpty() && yuzde in 1..49) {
                sinyal += 1
                puan += ((50 - yuzde) * 10 / 50).coerceAtMost(10)
                sebepler.add(c.getString(R.string.zn_sebep_yarim, yuzde))
            }

            // Yetersiz veriyle hüküm verme
            if (sinyal < EN_AZ_SINYAL) return@forEach
            if (puan < enAz) return@forEach

            bulgular.add(
                Bulgu(
                    konuId = konu.id,
                    konuAdi = konu.title,
                    puan = puan.coerceIn(0, 100),
                    sinyalSayisi = sinyal,
                    hataSayisi = konuHatalari,
                    quizYuzde = quizYuzde,
                    ef = ortEf,
                    tamamlanmaYuzde = yuzde,
                    sebepler = sebepler
                )
            )
        }

        bulgular.sortedByDescending { it.puan }
    }.onFailure { android.util.Log.w(TAG, "analiz", it) }.getOrDefault(emptyList())

    /** En zayıf konu — Bugün ekranında öneri olarak gösteriliyor. */
    fun enZayif(c: Context): Bulgu? = analiz(c, enAz = 40).firstOrNull()

    /** Dikkat gerektiren konu sayısı (rozet için). */
    fun dikkatSayisi(c: Context): Int = analiz(c, enAz = 40).size

    /**
     * Yeterli veri toplandı mı?
     *
     * Yeni kullanıcıya "zayıf noktan yok" demek yanıltıcı; "henüz
     * yeterli veri yok" demek doğru.
     */
    fun veriYeterliMi(c: Context): Boolean = runCatching {
        val hata = Hatalarim.hepsi(c).size
        val quiz = QuizStore.sonuclariYukle(c).size
        val tekrar = KonuTekrar.hepsi(c).size
        (hata + quiz + tekrar) >= 5
    }.getOrDefault(false)

    /**
     * Dürüst genel değerlendirme.
     *
     * Suçlayıcı değil yönlendirici dil — v9.0'daki tekrar oturumu
     * geri bildirimiyle aynı yaklaşım.
     */
    fun genelYorum(c: Context): String {
        if (!veriYeterliMi(c)) return c.getString(R.string.zn_veri_yok)
        val bulgular = analiz(c)
        if (bulgular.isEmpty()) return c.getString(R.string.zn_temiz)

        val acil = bulgular.count { it.seviye == 2 }
        val dikkat = bulgular.count { it.seviye == 1 }
        return when {
            acil > 0 -> c.getString(R.string.zn_yorum_acil, acil, bulgular.first().konuAdi)
            dikkat > 0 -> c.getString(R.string.zn_yorum_dikkat, dikkat)
            else -> c.getString(R.string.zn_yorum_iyi)
        }
    }
}
