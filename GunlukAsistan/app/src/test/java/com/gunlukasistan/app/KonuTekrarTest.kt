package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v9.0 — SM-2 algoritması testleri (öneri 53 güvencesi).
 *
 * ── Neden bu testler kritik ──
 * SM-2 kullanıcının aylarca sürecek tekrar programını belirliyor.
 * Bir hata olursa belirtisi geç çıkar:
 *   · Aralık çok hızlı büyürse → kullanıcı her şeyi unutur
 *   · Çok yavaş büyürse → aynı maddeler sürekli gelir, bıktırır
 *   · EF sınırı yoksa → negatif/sıfır aralık, çökme
 *
 * Hiçbiri anında görünmüyor; haftalar sonra "bu uygulama işe
 * yaramıyor" olarak ortaya çıkıyor. Cihazda test edemediğim için
 * matematiği burada doğruluyorum.
 *
 * ── Referans ──
 * P.A. Wozniak, SuperMemo 2 (1987). Anki de aynı temeli kullanıyor.
 */
class KonuTekrarTest {

    // ══════════════════════════════════════════════════════════
    // Temel SM-2 davranışı
    // ══════════════════════════════════════════════════════════

    @Test
    fun `ilk basarili tekrar 1 gun sonra`() {
        val (n, aralik, _) = KonuTekrar.sm2(0, 1, 2.5, KonuTekrar.KALITE_IYI)
        assertEquals(1, n)
        assertEquals("SM-2: n=1 → 1 gün", 1, aralik)
    }

    @Test
    fun `ikinci basarili tekrar 6 gun sonra`() {
        val (n, aralik, _) = KonuTekrar.sm2(1, 1, 2.5, KonuTekrar.KALITE_IYI)
        assertEquals(2, n)
        assertEquals("SM-2: n=2 → 6 gün", 6, aralik)
    }

    @Test
    fun `ucuncu tekrardan sonra aralik EF ile carpilir`() {
        // n=2, aralık=6, EF=2.5 → 6 × 2.5 = 15
        val (n, aralik, _) = KonuTekrar.sm2(2, 6, 2.5, KonuTekrar.KALITE_IYI)
        assertEquals(3, n)
        assertEquals(15, aralik)
    }

    @Test
    fun `aralik ustel olarak buyur`() {
        var n = 0; var aralik = 1; var ef = 2.5
        val gecmis = mutableListOf<Int>()
        repeat(6) {
            val (yn, ya, ye) = KonuTekrar.sm2(n, aralik, ef, KonuTekrar.KALITE_IYI)
            n = yn; aralik = ya; ef = ye
            gecmis.add(aralik)
        }
        // Her adım öncekinden büyük olmalı (ikinciden itibaren)
        for (i in 1 until gecmis.size) {
            assertTrue(
                "Aralık büyümeli: ${gecmis[i-1]} → ${gecmis[i]}",
                gecmis[i] >= gecmis[i - 1]
            )
        }
        assertTrue("6 başarılı tekrardan sonra aralık en az 30 gün olmalı", aralik >= 30)
    }

    // ══════════════════════════════════════════════════════════
    // Unutma davranışı
    // ══════════════════════════════════════════════════════════

    @Test
    fun `unutunca bastan baslar`() {
        // 5 tekrar yapılmış, aralık 60 gün — sonra unutuldu
        val (n, aralik, _) = KonuTekrar.sm2(5, 60, 2.5, KonuTekrar.KALITE_UNUTTUM)
        assertEquals("Tekrar sayısı sıfırlanmalı", 0, n)
        assertEquals("Aralık 1 güne dönmeli", 1, aralik)
    }

    @Test
    fun `unutmak EF yi dusurur`() {
        val (_, _, ef) = KonuTekrar.sm2(3, 15, 2.5, KonuTekrar.KALITE_UNUTTUM)
        assertTrue("Unutunca EF düşmeli: $ef", ef < 2.5)
    }

    @Test
    fun `kalite 3 esik degeri - sifirlamaz ama EF dusurur`() {
        // q=3 SM-2'de "zorlanarak hatırladım": ilerleme var ama EF düşer
        val (n, aralik, ef) = KonuTekrar.sm2(2, 6, 2.5, KonuTekrar.KALITE_ZOR)
        assertEquals("q>=3 ilerlemeli", 3, n)
        assertTrue("Aralık ilerlemeli", aralik > 1)
        assertTrue("Zorlanınca EF düşmeli: $ef", ef < 2.5)
    }

    @Test
    fun `kolay cevap EF yi yukseltir`() {
        val (_, _, ef) = KonuTekrar.sm2(2, 6, 2.5, KonuTekrar.KALITE_KOLAY)
        assertTrue("Kolay cevapta EF artmalı: $ef", ef > 2.5)
    }

    // ══════════════════════════════════════════════════════════
    // 🔴 Sınır durumları — çökme ve saçma değer koruması
    // ══════════════════════════════════════════════════════════

    @Test
    fun `EF asla 1_3 altina inmez`() {
        // Sürekli unutan bir kullanıcı: EF dibe vurmamalı
        var ef = 2.5
        repeat(20) {
            val (_, _, ye) = KonuTekrar.sm2(0, 1, ef, KonuTekrar.KALITE_UNUTTUM)
            ef = ye
        }
        assertTrue("EF alt sınırı 1.3 olmalı, bulunan: $ef", ef >= 1.3)
    }

    @Test
    fun `aralik asla sifir veya negatif olmaz`() {
        // EF minimumdayken bile aralık pozitif kalmalı
        val kaliteler = listOf(0, 1, 2, 3, 4, 5)
        val efler = listOf(1.3, 1.5, 2.0, 2.5, 3.0)
        val araliklar = listOf(0, 1, 6, 15, 60, 365)
        val nler = listOf(0, 1, 2, 5, 20)

        for (q in kaliteler) for (ef in efler) for (a in araliklar) for (n in nler) {
            val (yn, ya, ye) = KonuTekrar.sm2(n, a, ef, q)
            assertTrue("Aralık pozitif olmalı (n=$n a=$a ef=$ef q=$q) → $ya", ya >= 1)
            assertTrue("EF alt sınırın altına inmemeli → $ye", ye >= 1.3)
            assertTrue("Tekrar sayısı negatif olmamalı → $yn", yn >= 0)
        }
    }

    @Test
    fun `aralik bir yili gecmez`() {
        // Çok yüksek EF ve uzun aralıkta bile tavan uygulanmalı.
        // 5 yıl sonra gelecek bir tekrar anlamsız.
        val (_, aralik, _) = KonuTekrar.sm2(20, 300, 3.0, KonuTekrar.KALITE_KOLAY)
        assertTrue("Aralık 365 günü geçmemeli: $aralik", aralik <= 365)
    }

    @Test
    fun `gecersiz kalite degeri cokmez`() {
        // Arayüz 0/3/4/5 gönderiyor ama savunmalı olalım
        listOf(-10, -1, 6, 100).forEach { q ->
            val (n, a, ef) = KonuTekrar.sm2(2, 6, 2.5, q)
            assertTrue("n geçerli olmalı (q=$q)", n >= 0)
            assertTrue("aralık geçerli olmalı (q=$q)", a >= 1)
            assertTrue("EF geçerli olmalı (q=$q)", ef >= 1.3)
        }
    }

    // ══════════════════════════════════════════════════════════
    // Gerçekçi senaryolar
    // ══════════════════════════════════════════════════════════

    @Test
    fun `kolay madde hizla seyreklesir`() {
        // Hep "kolay" diyen kullanıcı: 4 tekrarda aylık aralığa çıkmalı
        var n = 0; var aralik = 1; var ef = 2.5
        repeat(4) {
            val (yn, ya, ye) = KonuTekrar.sm2(n, aralik, ef, KonuTekrar.KALITE_KOLAY)
            n = yn; aralik = ya; ef = ye
        }
        assertTrue("Kolay madde 4 tekrarda 30+ güne çıkmalı: $aralik", aralik >= 30)
    }

    @Test
    fun `zor madde sik gelir`() {
        // Hep "zor" diyen: aralık yavaş büyümeli
        var n = 0; var aralik = 1; var ef = 2.5
        repeat(4) {
            val (yn, ya, ye) = KonuTekrar.sm2(n, aralik, ef, KonuTekrar.KALITE_ZOR)
            n = yn; aralik = ya; ef = ye
        }
        // Kolay senaryoda 30+ olurken burada belirgin şekilde az olmalı
        assertTrue("Zor madde sık gelmeli, aralık: $aralik", aralik < 30)
        assertTrue("EF düşmüş olmalı: $ef", ef < 2.0)
    }

    @Test
    fun `unutup tekrar ogrenme dongusu tutarli`() {
        // Öğren → unut → yeniden öğren
        var n = 0; var aralik = 1; var ef = 2.5

        // 3 başarılı tekrar
        repeat(3) {
            val (yn, ya, ye) = KonuTekrar.sm2(n, aralik, ef, KonuTekrar.KALITE_IYI)
            n = yn; aralik = ya; ef = ye
        }
        val efOnce = ef
        assertTrue("Aralık büyümüş olmalı", aralik > 6)

        // Unut
        val (n2, a2, ef2) = KonuTekrar.sm2(n, aralik, ef, KonuTekrar.KALITE_UNUTTUM)
        assertEquals(0, n2)
        assertEquals(1, a2)
        assertTrue("EF düşmüş olmalı", ef2 < efOnce)

        // Yeniden öğren — EF düşük olduğu için aralık daha yavaş büyür
        var n3 = n2; var a3 = a2; var ef3 = ef2
        repeat(3) {
            val (yn, ya, ye) = KonuTekrar.sm2(n3, a3, ef3, KonuTekrar.KALITE_IYI)
            n3 = yn; a3 = ya; ef3 = ye
        }
        assertTrue(
            "Unutulan madde ikinci turda daha sık gelmeli ($a3 vs $aralik)",
            a3 <= aralik
        )
    }

    // ══════════════════════════════════════════════════════════
    // Unutma eğrisi (öneri 55)
    // ══════════════════════════════════════════════════════════

    @Test
    fun `hatirlama tahmini 0-100 arasinda`() {
        val simdi = System.currentTimeMillis()
        val ornekler = listOf(
            KonuTekrar.Durum(1, 1, "a", aralik = 1, sonTekrar = simdi),
            KonuTekrar.Durum(2, 1, "b", aralik = 30, sonTekrar = simdi - 86_400_000L * 10),
            KonuTekrar.Durum(3, 1, "c", aralik = 5, sonTekrar = simdi - 86_400_000L * 100),
            KonuTekrar.Durum(4, 1, "d", aralik = 0, sonTekrar = 0)
        )
        ornekler.forEach {
            val t = KonuTekrar.hatirlamaTahmini(it)
            assertTrue("Tahmin 0-100 arasında olmalı: $t", t in 0..100)
        }
    }

    @Test
    fun `zaman gectikce hatirlama duser`() {
        val simdi = System.currentTimeMillis()
        val taze = KonuTekrar.Durum(1, 1, "a", aralik = 10, sonTekrar = simdi - 86_400_000L)
        val eski = KonuTekrar.Durum(2, 1, "b", aralik = 10, sonTekrar = simdi - 86_400_000L * 20)
        assertTrue(
            "Eski tekrar daha düşük hatırlama vermeli",
            KonuTekrar.hatirlamaTahmini(eski) < KonuTekrar.hatirlamaTahmini(taze)
        )
    }

    @Test
    fun `uzun aralikli madde daha yavas unutulur`() {
        val simdi = System.currentTimeMillis()
        val gecenGun = 10L
        val kisa = KonuTekrar.Durum(1, 1, "a", aralik = 2, sonTekrar = simdi - 86_400_000L * gecenGun)
        val uzun = KonuTekrar.Durum(2, 1, "b", aralik = 60, sonTekrar = simdi - 86_400_000L * gecenGun)
        assertTrue(
            "Uzun aralıklı (iyi oturmuş) madde daha iyi hatırlanmalı",
            KonuTekrar.hatirlamaTahmini(uzun) > KonuTekrar.hatirlamaTahmini(kisa)
        )
    }

    // ══════════════════════════════════════════════════════════
    // Durum yardımcıları
    // ══════════════════════════════════════════════════════════

    @Test
    fun `ogrenildi esigi dogru`() {
        assertTrue(KonuTekrar.Durum(1, 1, "a", aralik = 60).ogrenildi)
        assertTrue(KonuTekrar.Durum(1, 1, "a", aralik = 120).ogrenildi)
        assertTrue(!KonuTekrar.Durum(1, 1, "a", aralik = 59).ogrenildi)
        assertTrue(!KonuTekrar.Durum(1, 1, "a", aralik = 1).ogrenildi)
    }

    @Test
    fun `zor madde tespiti`() {
        assertTrue("Düşük EF zor sayılmalı", KonuTekrar.Durum(1, 1, "a", ef = 1.5).zorMu)
        assertTrue("Çok unutma zor sayılmalı", KonuTekrar.Durum(1, 1, "a", unutmaSayisi = 3).zorMu)
        assertTrue("Normal madde zor değil", !KonuTekrar.Durum(1, 1, "a", ef = 2.5).zorMu)
    }

    @Test
    fun `kalite sabitleri SM-2 araliginda`() {
        listOf(
            KonuTekrar.KALITE_UNUTTUM, KonuTekrar.KALITE_ZOR,
            KonuTekrar.KALITE_IYI, KonuTekrar.KALITE_KOLAY
        ).forEach {
            assertTrue("Kalite 0-5 arasında olmalı: $it", it in 0..5)
        }
        // Unuttum eşiğin altında, diğerleri üstünde olmalı
        assertTrue(KonuTekrar.KALITE_UNUTTUM < 3)
        assertTrue(KonuTekrar.KALITE_ZOR >= 3)
    }
}
