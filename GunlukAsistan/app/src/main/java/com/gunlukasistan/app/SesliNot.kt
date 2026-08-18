package com.gunlukasistan.app

import android.content.Context

/**
 * v7.71 — Sesli notu doğru yere yönlendiren sınıflandırıcı.
 *
 * ── Kullanıcının isteği (10 öneriden 10. madde) ──
 * "Widget'tan bas, konuş, bırak → metne çevrilir, AI kategorize eder
 *  (görev mi, not mu, alışveriş mi)."
 *
 * ── İki aşamalı sınıflandırma ──
 * 1. **Yerel kural motoru** — anahtar kelimelere bakar, anında sonuç verir.
 *    İnternet gerekmez, gecikme yok, ücretsiz.
 * 2. **Yapay zekâ** — yerel motor kararsız kalırsa (güven düşükse) devreye
 *    girer. AI kapalıysa kullanıcıya seçim menüsü gösterilir.
 *
 * Bu sıralama bilinçli: "süt al" demek için ağ isteği atmak gereksiz.
 * Kural motoru vakaların çoğunu zaten doğru yakalıyor.
 */
object SesliNot {

    private const val TAG = "SesliNot"

    /** Kaydedilebilecek hedefler. */
    enum class Hedef {
        GOREV, NOT, PLAN, ALISVERIS, ASISTAN
    }

    /**
     * Sınıflandırma sonucu.
     * @param guven 0..1 — 0.6 altındaysa AI'a danışılır
     */
    data class Sonuc(val hedef: Hedef, val guven: Float, val gerekce: String = "")

    // ═══════════════════════════════════════════════════════════════
    // YEREL KURAL MOTORU
    // ═══════════════════════════════════════════════════════════════

    /** Alışveriş sinyalleri — ürün alma bağlamı. */
    private val ALISVERIS = listOf(
        "al ", "alacağım", "alalım", "alınacak", "market", "bakkal",
        "manav", "eczane", "kasap", "fırın", "süt", "ekmek", "yumurta",
        "peynir", "deterjan", "şampuan", "sebze", "meyve", "kilo",
        "paket", "şişe", "kutu", "listeye ekle", "alışveriş"
    )

    /** Görev sinyalleri — yapılacak iş bağlamı. */
    private val GOREV = listOf(
        "yapmam lazım", "yapmalıyım", "unutma", "hatırlat", "ara",
        "gönder", "öde", "fatura", "randevu", "toplantı", "git",
        "getir", "teslim", "başvur", "kaydol", "temizle", "yıka",
        "çöp", "bitir", "tamamla", "kontrol et", "görev"
    )

    /** Namaz/vakit planı sinyalleri. */
    private val PLAN = listOf(
        "namaz", "vakit", "sabah namaz", "öğleden sonra", "ikindiden",
        "akşamdan", "imsak", "kuşluk", "vakit planı", "dilim"
    )

    /** Soru/asistan sinyalleri. */
    private val SORU = listOf(
        "nedir", "nasıl", "neden", "kim", "kaç", "hangi", "ne kadar",
        "açıkla", "anlat", "hesapla", "çevir", "özetle", "?"
    )

    /** Not sinyalleri — akılda tutulacak bilgi. */
    private val NOT = listOf(
        "not al", "aklımda", "fikir", "şifre", "kod", "adres",
        "telefon numarası", "plaka", "kayıt", "yazdım"
    )

    /**
     * Metni yerel kurallarla sınıflandırır.
     *
     * Puanlama: her eşleşen anahtar kelime 1 puan. En yüksek puanlı
     * kategori kazanır. Güven, kazananın toplam içindeki payına göre
     * hesaplanır — birden çok kategori eşleşirse güven düşer.
     */
    fun yerelSinifla(metin: String): Sonuc {
        val t = normalle(metin)
        if (t.isBlank()) return Sonuc(Hedef.NOT, 0f)

        val puanlar = mapOf(
            Hedef.ALISVERIS to say(t, ALISVERIS),
            Hedef.GOREV to say(t, GOREV),
            Hedef.PLAN to say(t, PLAN),
            Hedef.ASISTAN to say(t, SORU),
            Hedef.NOT to say(t, NOT)
        )

        val toplam = puanlar.values.sum()
        if (toplam == 0) {
            // Hiç sinyal yok: kısa metin görev, uzun metin not olma eğiliminde
            val kelime = t.split(" ").size
            return if (kelime <= 5) Sonuc(Hedef.GOREV, 0.35f, "kısa ifade")
            else Sonuc(Hedef.NOT, 0.35f, "uzun ifade")
        }

        val kazanan = puanlar.maxByOrNull { it.value } ?: return Sonuc(Hedef.NOT, 0f)
        val guven = (kazanan.value.toFloat() / toplam).coerceIn(0f, 1f)

        // Soru işareti güçlü sinyaldir
        if (metin.trim().endsWith("?")) {
            return Sonuc(Hedef.ASISTAN, 0.9f, "soru işareti")
        }
        return Sonuc(kazanan.key, guven, "anahtar kelime")
    }

    private fun say(metin: String, anahtarlar: List<String>): Int =
        anahtarlar.count { metin.contains(normalle(it)) }

    /** Türkçe duyarlı karşılaştırma anahtarı. */
    private fun normalle(s: String): String =
        s.lowercase(java.util.Locale("tr", "TR"))
            .replace("ı", "i").replace("ş", "s").replace("ğ", "g")
            .replace("ü", "u").replace("ö", "o").replace("ç", "c")
            .trim()

    // ═══════════════════════════════════════════════════════════════
    // YAPAY ZEKÂ DESTEĞİ
    // ═══════════════════════════════════════════════════════════════

    /**
     * Yerel motor kararsızsa yapay zekâya danışır.
     *
     * **Ağ işlemi** — arka planda çağrılmalı.
     * AI hazır değilse veya hata olursa yerel sonuç aynen döner.
     */
    fun aiSinifla(context: Context, metin: String, yerel: Sonuc): Sonuc {
        if (yerel.guven >= 0.6f) return yerel
        if (!AiSettings.isReady(context)) return yerel

        return try {
            val istem = buildString {
                append("Aşağıdaki Türkçe ifadeyi tek kelimeyle sınıflandır.\n")
                append("Seçenekler: GOREV, NOT, PLAN, ALISVERIS, SORU\n\n")
                append("GOREV = yapılacak iş, hatırlatma\n")
                append("NOT = akılda tutulacak bilgi\n")
                append("PLAN = namaz vakitlerine bağlı iş\n")
                append("ALISVERIS = satın alınacak ürün\n")
                append("SORU = bilgi sorusu, hesaplama\n\n")
                append("İfade: \"").append(metin.take(300)).append("\"\n\n")
                append("YALNIZCA tek kelime yaz, başka hiçbir şey yazma.")
            }
            val sonuc = AiClient.sadeIstek(context, istem, 32)
            if (!sonuc.ok) return yerel

            val cevap = normalle(sonuc.text).replace(Regex("[^a-z]"), "")
            val hedef = when {
                cevap.contains("gorev") -> Hedef.GOREV
                cevap.contains("alisveris") -> Hedef.ALISVERIS
                cevap.contains("plan") -> Hedef.PLAN
                cevap.contains("soru") -> Hedef.ASISTAN
                cevap.contains("not") -> Hedef.NOT
                else -> return yerel
            }
            Sonuc(hedef, 0.85f, "yapay zekâ")
        } catch (e: Exception) {
            android.util.Log.w(TAG, "AI sınıflandırma başarısız", e)
            yerel
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // KAYDETME
    // ═══════════════════════════════════════════════════════════════

    /**
     * Metni seçilen hedefe kaydeder.
     *
     * Görevlerde `NaturalDate` ile doğal dilden zaman ayıklanır:
     * "yarın saat 3'te doktoru ara" → görev metni + hatırlatma kurulur.
     *
     * @return kullanıcıya gösterilecek onay metni
     */
    fun kaydet(context: Context, hedef: Hedef, metin: String): String {
        val temiz = metin.trim()
        if (temiz.isBlank()) return ""
        return try {
            when (hedef) {
                Hedef.GOREV -> gorevEkle(context, temiz)
                Hedef.ALISVERIS -> alisverisEkle(context, temiz)
                Hedef.NOT -> notEkle(context, temiz)
                Hedef.PLAN -> planEkle(context, temiz)
                Hedef.ASISTAN -> temiz   // çağıran asistanı açar
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Kaydedilemedi", e)
            ""
        }
    }

    /** Görev + varsa doğal dilden hatırlatma. */
    private fun gorevEkle(context: Context, metin: String): String {
        val zaman = try {
            NaturalDate.parse(metin)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Zaman ayıklanamadı", e)
            null
        }
        val govde = if (zaman != null && zaman.found && zaman.text.isNotBlank()) {
            zaman.text
        } else {
            metin
        }
        val sonTarih = if (zaman != null && zaman.found) zaman.millis else 0L

        val liste = Store.loadTasks(context)
        val gorev = Store.Task(
            id = System.currentTimeMillis(),
            text = govde.take(200),
            done = false,
            createdAt = System.currentTimeMillis(),
            dueAt = sonTarih
        )
        liste.add(gorev)
        Store.saveTasks(context, liste)

        if (sonTarih > 0) {
            try {
                AlarmScheduler.schedule(context, gorev.id, gorev.text, sonTarih)
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Alarm kurulamadı", e)
            }
            return context.getString(R.string.sn_gorev) + " · " +
                context.getString(R.string.sn_zaman_bulundu, Tekrar.tarihMetni(sonTarih))
        }
        return context.getString(R.string.sn_gorev)
    }

    /**
     * Alışveriş: "🛒 Alışveriş" başlıklı tek nota biriktirilir.
     * Her ürün ayrı satır — markette tek ekrandan bakılsın.
     */
    private fun alisverisEkle(context: Context, metin: String): String {
        val baslik = context.getString(R.string.sn_alisveris_notu)
        val notlar = Store.loadNotes(context)
        val mevcut = notlar.firstOrNull { it.title == baslik }
        if (mevcut != null) {
            mevcut.content = (mevcut.content.trimEnd() + "\n• " + metin).take(4000)
        } else {
            notlar.add(
                Store.Note(
                    id = System.currentTimeMillis(),
                    title = baslik,
                    content = "• " + metin,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
        Store.saveNotes(context, notlar)
        return context.getString(R.string.sn_alisveris)
    }

    /** Not: ilk satır başlık, kalanı içerik. */
    private fun notEkle(context: Context, metin: String): String {
        val notlar = Store.loadNotes(context)
        val baslik = metin.take(40).let { if (metin.length > 40) it.trimEnd() + "…" else it }
        notlar.add(
            Store.Note(
                id = System.currentTimeMillis(),
                title = baslik,
                content = metin.take(4000),
                createdAt = System.currentTimeMillis()
            )
        )
        Store.saveNotes(context, notlar)
        return context.getString(R.string.sn_not)
    }

    /** Vakit planı: o an aktif olan dilime eklenir. */
    private fun planEkle(context: Context, metin: String): String {
        val gun = NamazVakti.bugunDuzeltilmis(context)
        val dilim = NamazPlan.aktifDilim(gun, NamazVakti.simdiDakika())
        NamazPlan.isEkle(context, dilim, metin)
        return context.getString(R.string.sn_plan) + " · " + context.getString(dilim.adRes)
    }

    /** Hedefin kullanıcıya gösterilecek adı. */
    fun hedefAdi(context: Context, hedef: Hedef): String = context.getString(
        when (hedef) {
            Hedef.GOREV -> R.string.sn_gorev
            Hedef.NOT -> R.string.sn_not
            Hedef.PLAN -> R.string.sn_plan
            Hedef.ALISVERIS -> R.string.sn_alisveris
            Hedef.ASISTAN -> R.string.sn_asistan
        }
    )
}
