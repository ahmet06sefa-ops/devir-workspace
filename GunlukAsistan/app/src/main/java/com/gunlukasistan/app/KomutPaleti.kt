package com.gunlukasistan.app

/**
 * v10.6 · Öneri D39 — ⌘K komut paleti.
 *
 * ── Dürüst not ──
 * `HizliKomut` (v9.5 · FAB uzun bas) zaten var: **doğal dilde
 * veri girer** ("gorev: rapor yaz cuma 17:00"). O, cümle yazma
 * aracıdır. Bu palet farklı iş yapar: **uygulamanın her köşesine
 * tek kutudan atlarsın** — ekran açma + hazır sayaç eylemleri,
 * titremeli (fuzzy) aramayla. İki araç birbirini tamamlar;
 * palet, HizliKomut'un tetikleyemediği "sadece oraya git"
 * ihtiyacını karşılar.
 *
 * ── Puanlama ──
 * Başlıkla başlama > başlıkta geçme > anahtar kelimede geçme.
 * Eşleşme [AyarAra] ile aynı Türkçe katlamayı kullanır.
 *
 * Saf bölge (puan/sırala) birim testlidir.
 */
object KomutPaleti {

    data class PaletKomut(
        val emoji: String,
        val baslik: String,
        val anahtarlar: List<String>,
        /** null ise ekran açılmaz; >0 ise sayaç bu dakikayla başlar */
        val ekran: Int? = null,
        val sayacDakika: Int = 0,
        val sayaciDurdur: Boolean = false
    )

    /** Palette listelenen tüm komutlar. */
    val KOMUTLAR: List<PaletKomut> = listOf(
        PaletKomut("🗓️", "Bugün", listOf("bugun", "plan", "gunluk"), ekran = 2),
        PaletKomut("⏱️", "Sayaç", listOf("sayac", "zamanlayici", "kronometre"), ekran = 4),
        PaletKomut("✅", "Görevler", listOf("gorev", "yapilacak", "is"), ekran = 6),
        PaletKomut("📚", "Konular", listOf("konu", "ders", "calis"), ekran = 3),
        PaletKomut("📝", "Notlar", listOf("not", "yazi"), ekran = 5),
        PaletKomut("📈", "İlerleme", listOf("ilerleme", "istatistik", "grafik", "analiz"), ekran = 1),
        PaletKomut("🎯", "Sınavlar", listOf("sinav", "yks", "deneme"), ekran = 10),
        PaletKomut("🔥", "Alışkanlıklar", listOf("aliskanlik", "takip", "seri", "streak"), ekran = 12),
        PaletKomut("🎓", "Kurslar", listOf("kurs", "egitim"), ekran = 13),
        PaletKomut("🧰", "Araçlar", listOf("arac", "tool", "alet"), ekran = 15),
        PaletKomut("🎬", "Kaynak Merkezi", listOf("kaynak", "video", "pdf", "merkez"), ekran = 14),
        PaletKomut("▶️", "25 dk odak başlat", listOf("odak", "pomodoro", "konsantre", "25"), sayacDakika = 25),
        PaletKomut("⏩", "15 dk sayaç başlat", listOf("15", "orta"), sayacDakika = 15),
        PaletKomut("⏩", "5 dk sayaç başlat", listOf("5", "kisa", "esneme"), sayacDakika = 5),
        PaletKomut("⏹️", "Sayacı durdur", listOf("durdur", "bitir", "iptal", "stop"), sayaciDurdur = true)
    )

    fun puan(sorgu: String, k: PaletKomut): Int {
        val sor = AyarAra.normal(sorgu)
        if (sor.isEmpty()) return 1 // sorgu yokken alfabetik öncelik yok; sıra korunur
        val baslik = AyarAra.normal(k.baslik)
        if (baslik.startsWith(sor)) return 4
        if (baslik.split(" ").any { it.startsWith(sor) }) return 3
        if (baslik.contains(sor)) return 2
        if (k.anahtarlar.any { AyarAra.normal(it).contains(sor) }) return 1
        return 0
    }

    /** Sorguya göre en iyi eşleşmeler; eşleşmeyenler düşer, sıra puanla. */
    fun sirala(sorgu: String, komutlar: List<PaletKomut> = KOMUTLAR): List<PaletKomut> {
        if (AyarAra.normal(sorgu).isEmpty()) return komutlar
        return komutlar
            .map { it to puan(sorgu, it) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
    }
}
