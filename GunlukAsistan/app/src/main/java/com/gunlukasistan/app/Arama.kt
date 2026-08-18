package com.gunlukasistan.app

import android.content.Context

/**
 * v7.73 — Global arama.
 *
 * ── Kullanıcının isteği (10 iyileştirmeden 1. madde) ──
 * "Tek kutudan her şeyde ara: görev, not, konu, ders, sohbet, plan,
 *  alışkanlık."
 *
 * ── Neden gerekti ──
 * Uygulamada yalnızca **PDF içi arama** vardı (`PdfArama`). Veri
 * biriktikçe "o notu nereye yazmıştım" sorunu büyüyordu.
 *
 * ── Tasarım ──
 * · Tüm kaynaklar tek listede toplanır, kategoriye göre gruplanır
 * · **Türkçe duyarlı** karşılaştırma: "İ/ı/ş/ğ/ü/ö/ç" normalleştirilir,
 *   böylece "cimento" yazınca "çimento" bulunur
 * · Puanlama: başlıkta geçen > içerikte geçen, baştan eşleşme > ortadan
 * · Ağır iş — arka planda çağrılmalı ([Performans.arkaPlan])
 */
object Arama {

    private const val TAG = "Arama"

    /** Sonucun hangi ekrana ait olduğu. */
    enum class Tur(val adRes: Int, val emoji: String) {
        GOREV(R.string.ar_k_gorev, "✅"),
        NOT(R.string.ar_k_not, "📝"),
        KONU(R.string.ar_k_konu, "📚"),
        DERS(R.string.ar_k_ders, "🎓"),
        SOHBET(R.string.ar_k_sohbet, "💬"),
        PLAN(R.string.ar_k_plan, "🕌"),
        ALISKANLIK(R.string.ar_k_aliskanlik, "✨"),
        ETKINLIK(R.string.ar_k_etkinlik, "📅"),

        // v8.0: v7.78'den sonra eklenen modüller aramaya dahil edildi.
        // Kullanıcı sözlüğüne terim ekliyor, hata defteri birikiyor,
        // AI anlatımları üretiliyordu — hiçbiri aranamıyordu.
        TERIM(R.string.ar_k_terim, "📖"),
        HATA(R.string.ar_k_hata, "🎯"),
        ANLATIM(R.string.ar_k_anlatim, "📄"),
        YERIMI(R.string.ar_k_yerimi, "🔖")
    }

    /**
     * Tek bir arama sonucu.
     *
     * @param kimlik kaynağın kendi id'si — tıklanınca oraya gidilir
     * @param puan sıralama için; yüksek olan üstte
     */
    data class Sonuc(
        val tur: Tur,
        val kimlik: Long,
        val baslik: String,
        val altYazi: String = "",
        val puan: Int = 0,
        /** Ekstra bilgi (ör. ders için kurs kimliği). */
        val ek: Long = 0L
    )

    // ═══════════════════════════════════════════════════════════════
    // METİN EŞLEŞTİRME
    // ═══════════════════════════════════════════════════════════════

    /** Türkçe duyarlı karşılaştırma anahtarı. */
    fun normalle(s: String): String =
        s.lowercase(java.util.Locale("tr", "TR"))
            .replace("ı", "i").replace("İ", "i")
            .replace("ş", "s").replace("ğ", "g")
            .replace("ü", "u").replace("ö", "o").replace("ç", "c")
            .trim()

    /**
     * Eşleşme puanı hesaplar. 0 = eşleşme yok.
     *
     * · Başlıkta baştan eşleşme: 100
     * · Başlıkta geçiyor: 60
     * · İçerikte geçiyor: 25
     * Tam kelime eşleşmesi +15 bonus alır.
     */
    private fun puanla(sorgu: String, baslik: String, icerik: String = ""): Int {
        val q = normalle(sorgu)
        if (q.isBlank()) return 0
        val b = normalle(baslik)
        val i = normalle(icerik)

        var puan = when {
            b.startsWith(q) -> 100
            b.contains(q) -> 60
            i.contains(q) -> 25
            else -> 0
        }
        if (puan == 0) return 0
        // Tam kelime eşleşmesi daha değerli
        if (b.split(" ").any { it == q } || i.split(" ").any { it == q }) puan += 15
        return puan
    }

    // ═══════════════════════════════════════════════════════════════
    // ARAMA
    // ═══════════════════════════════════════════════════════════════

    /**
     * Tüm kaynaklarda arar.
     *
     * @param turler yalnızca bu türlerde ara; boşsa hepsinde
     * @return puana göre sıralı sonuçlar
     */
    fun ara(
        context: Context,
        sorgu: String,
        turler: Set<Tur> = emptySet()
    ): List<Sonuc> {
        if (sorgu.trim().length < 2) return emptyList()
        val hepsi = mutableListOf<Sonuc>()
        val istenen = { t: Tur -> turler.isEmpty() || turler.contains(t) }

        if (istenen(Tur.GOREV)) hepsi += gorevlerde(context, sorgu)
        if (istenen(Tur.NOT)) hepsi += notlarda(context, sorgu)
        if (istenen(Tur.KONU)) hepsi += konularda(context, sorgu)
        if (istenen(Tur.DERS)) hepsi += derslerde(context, sorgu)
        if (istenen(Tur.SOHBET)) hepsi += sohbetlerde(context, sorgu)
        if (istenen(Tur.PLAN)) hepsi += planda(context, sorgu)
        if (istenen(Tur.ALISKANLIK)) hepsi += aliskanliklarda(context, sorgu)
        if (istenen(Tur.ETKINLIK)) hepsi += etkinliklerde(context, sorgu)
        // v8.0: yeni kaynaklar
        if (istenen(Tur.TERIM)) hepsi += terimlerde(context, sorgu)
        if (istenen(Tur.HATA)) hepsi += hatalarda(context, sorgu)
        if (istenen(Tur.ANLATIM)) hepsi += anlatimlarda(context, sorgu)
        if (istenen(Tur.YERIMI)) hepsi += yerImlerinde(context, sorgu)

        return hepsi.sortedByDescending { it.puan }
    }

    private fun gorevlerde(context: Context, q: String): List<Sonuc> = try {
        Store.loadTasks(context).mapNotNull { g ->
            val p = puanla(q, g.text)
            if (p == 0) null else Sonuc(
                Tur.GOREV, g.id, g.text,
                if (g.done) "✓" else Tekrar.tarihMetni(g.dueAt), p
            )
        }
    } catch (e: Exception) {
        android.util.Log.w(TAG, "Görevlerde aranamadı", e); emptyList()
    }

    private fun notlarda(context: Context, q: String): List<Sonuc> = try {
        Store.loadNotes(context).mapNotNull { n ->
            val p = puanla(q, n.title, n.content)
            if (p == 0) null else Sonuc(
                Tur.NOT, n.id, n.title.ifBlank { n.content.take(40) },
                n.content.replace("\n", " ").take(70), p
            )
        }
    } catch (e: Exception) {
        android.util.Log.w(TAG, "Notlarda aranamadı", e); emptyList()
    }

    /** Konu başlığı ve alt maddelerinde arar. */
    private fun konularda(context: Context, q: String): List<Sonuc> = try {
        Store.loadTopics(context).mapNotNull { k ->
            val altMetin = k.items.joinToString(" ") { it.text }
            val p = puanla(q, k.title, altMetin)
            if (p == 0) null else Sonuc(
                Tur.KONU, k.id, k.title,
                k.doneCount.toString() + "/" + k.items.size, p
            )
        }
    } catch (e: Exception) {
        android.util.Log.w(TAG, "Konularda aranamadı", e); emptyList()
    }

    private fun derslerde(context: Context, q: String): List<Sonuc> = try {
        Store.loadLessons(context).mapNotNull { d ->
            val p = puanla(q, d.title, d.desc + " " + d.note)
            if (p == 0) null else Sonuc(
                Tur.DERS, d.id, d.title,
                if (d.done) "✓" else "", p, d.courseId
            )
        }
    } catch (e: Exception) {
        android.util.Log.w(TAG, "Derslerde aranamadı", e); emptyList()
    }

    /** Sohbet başlıklarında ve mesaj içeriklerinde arar. */
    private fun sohbetlerde(context: Context, q: String): List<Sonuc> = try {
        SohbetGecmisi.tumu(context).mapNotNull { s ->
            val govde = s.mesajlar.joinToString(" ") { it.metin }
            val p = puanla(q, s.baslik, govde)
            if (p == 0) null else Sonuc(
                Tur.SOHBET, s.id, s.baslik,
                s.mesajlar.size.toString() + " mesaj", p
            )
        }
    } catch (e: Exception) {
        android.util.Log.w(TAG, "Sohbetlerde aranamadı", e); emptyList()
    }

    private fun planda(context: Context, q: String): List<Sonuc> = try {
        NamazPlan.isleriYukle(context).mapNotNull { i ->
            val p = puanla(q, i.metin)
            if (p == 0) null else {
                val dilim = NamazPlan.Dilim.entries.firstOrNull { it.anahtar == i.dilim }
                Sonuc(
                    Tur.PLAN, i.id, i.metin,
                    dilim?.let { context.getString(it.adRes) }.orEmpty(), p
                )
            }
        }
    } catch (e: Exception) {
        android.util.Log.w(TAG, "Planda aranamadı", e); emptyList()
    }

    private fun aliskanliklarda(context: Context, q: String): List<Sonuc> = try {
        Store.loadHabits(context).mapNotNull { a ->
            val p = puanla(q, a.title)
            if (p == 0) null else Sonuc(Tur.ALISKANLIK, a.id, a.title, a.emoji, p)
        }
    } catch (e: Exception) {
        android.util.Log.w(TAG, "Alışkanlıklarda aranamadı", e); emptyList()
    }

    private fun etkinliklerde(context: Context, q: String): List<Sonuc> = try {
        Store.loadEvents(context).mapNotNull { e ->
            val p = puanla(q, e.title)
            if (p == 0) null else Sonuc(
                Tur.ETKINLIK, e.id, e.emoji + " " + e.title, e.dateKey, p
            )
        }
    } catch (ex: Exception) {
        android.util.Log.w(TAG, "Etkinliklerde aranamadı", ex); emptyList()
    }

    // ═══════════════════════════════════════════════════════════════
    // v8.0 — YENİ KAYNAKLAR (öneri 5)
    // ═══════════════════════════════════════════════════════════════

    /** Terim sözlüğünde arar — terim adı ve tanımında. */
    private fun terimlerde(context: Context, q: String): List<Sonuc> = try {
        Sozluk.hepsi(context).mapNotNull { t ->
            val p = puanla(q, t.terim, t.kisa)
            if (p == 0) null else Sonuc(
                Tur.TERIM,
                // Sözlükte sayısal kimlik yok; terim metninden kararlı bir
                // kimlik türetiliyor (tıklamada geri bulmak için)
                t.terim.hashCode().toLong(),
                t.terim,
                t.kisa.take(70),
                // Yıldızlı terimler öne çıksın
                p + (if (t.yildiz) 12 else 0)
            )
        }
    } catch (e: Exception) {
        android.util.Log.w(TAG, "Terimlerde aranamadı", e); emptyList()
    }

    /** Hata defterindeki sorularda arar. */
    private fun hatalarda(context: Context, q: String): List<Sonuc> = try {
        Hatalarim.hepsi(context).mapNotNull { h ->
            val p = puanla(q, h.metin, h.aciklama)
            if (p == 0) null else Sonuc(
                Tur.HATA, h.soruId, h.metin.take(60),
                h.kaynak.ifBlank { "" },
                // Çok yanlış yapılan soru daha önemli
                p + (h.yanlisSayisi * 2).coerceAtMost(15)
            )
        }
    } catch (e: Exception) {
        android.util.Log.w(TAG, "Hata defterinde aranamadı", e); emptyList()
    }

    /**
     * Üretilmiş AI anlatımlarında arar.
     *
     * Anlatımlar konu maddesi başlığına göre saklanıyor; burada yalnızca
     * **başlık** taranıyor. Gövde metnini taramak tüm önbelleği açıp
     * ayrıştırmak demek — arama kutusuna her harfte bu yapılamaz.
     */
    private fun anlatimlarda(context: Context, q: String): List<Sonuc> = try {
        // Konulardaki maddelerden anlatımı olanları bul
        Store.loadTopics(context).flatMap { konu ->
            konu.items.mapNotNull { madde ->
                if (!KonuUretici.anlatimVarMi(context, madde.text)) return@mapNotNull null
                val p = puanla(q, madde.text, konu.title)
                if (p == 0) null else Sonuc(
                    Tur.ANLATIM, madde.id, madde.text, konu.title, p, konu.id
                )
            }
        }
    } catch (e: Exception) {
        android.util.Log.w(TAG, "Anlatımlarda aranamadı", e); emptyList()
    }

    /** PDF sayfa yer imlerinde arar — not metni ve ders adında. */
    private fun yerImlerinde(context: Context, q: String): List<Sonuc> = try {
        SayfaImi.hepsi(context).mapNotNull { im ->
            val p = puanla(q, im.not, im.dersAdi)
            if (p == 0) null else Sonuc(
                Tur.YERIMI, im.lessonId,
                im.not.ifBlank { im.dersAdi }.take(60),
                im.dersAdi + " · s." + (im.sayfa + 1),
                p,
                im.sayfa.toLong()
            )
        }
    } catch (e: Exception) {
        android.util.Log.w(TAG, "Yer imlerinde aranamadı", e); emptyList()
    }
}
