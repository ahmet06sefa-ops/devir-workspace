package com.gunlukasistan.app

import android.content.Context

/**
 * v7.95 — Sesli komut çözümleyici.
 *
 * ── Kullanıcı isteği (öneri 5) ──
 * "SesliNot var ama sadece not alıyor. Genişletilsin: '25 dakika sayaç
 *  başlat', 'yarına market görevi ekle', 'bugün ne çalışacağım' → doğrudan
 *  komut."
 *
 * ── [SesliNot]'tan farkı ──
 * SesliNot söyleneni **kaydeder** (görev/not/plan olarak sınıflandırır).
 * SesliKomut söyleneni **yapar**: sayaç başlatır, ekran açar, soru cevaplar.
 *
 * ── Neden kural tabanlı ──
 * Komut çözümleme anında olmalı; kullanıcı "sayaç başlat" deyip 5 saniye
 * beklememeli. Ayrıca çevrimdışı çalışmalı. Anlaşılmayan cümle SesliNot'a
 * devredilir — orada AI sınıflandırma zaten var.
 */
object SesliKomut {

    private const val TAG = "SesliKomut"

    /** Çözümlenen komut. */
    sealed class Komut {
        /** Sayacı belirtilen süreyle başlat. */
        data class SayacBaslat(val dakika: Int) : Komut()
        object SayacDurdur : Komut()
        object SayacDuraklat : Komut()

        /** Bir ekranı aç. */
        data class EkranAc(val ekran: Int, val ad: String) : Komut()

        /** Aktif dersi bitir. */
        object DersBitir : Komut()

        /** Hata defterinden tekrar başlat. */
        object HataTekrar : Komut()

        /** Sözlükte terim ara. */
        data class TerimSor(val terim: String) : Komut()

        /** Bugün ne çalışacağım / durum sorusu. */
        object DurumSor : Komut()

        /** Komut değil — SesliNot'a devret. */
        data class Anlasilmadi(val metin: String) : Komut()
    }

    // ═══════════════════════════════════════════════════════════════
    // ÇÖZÜMLEME
    // ═══════════════════════════════════════════════════════════════

    /**
     * Söyleneni komuta çevirir.
     *
     * Sıra önemli: daha özgül kalıplar önce denenir. "sayaç durdur"
     * hem "sayaç" hem "durdur" içeriyor; durdurma kontrolü başlatmadan
     * önce gelmeli.
     */
    fun coz(context: Context, ham: String): Komut {
        val m = normalle(ham)
        if (m.isBlank()) return Komut.Anlasilmadi(ham)

        // ── Sayaç durdurma/duraklatma (başlatmadan önce) ──
        if (iceriyorHepsi(m, listOf("sayac", "durdur")) ||
            iceriyorHepsi(m, listOf("zamanlayici", "durdur")) ||
            iceriyorHepsi(m, listOf("sayac", "iptal"))
        ) return Komut.SayacDurdur

        if (iceriyorHepsi(m, listOf("sayac", "duraklat")) ||
            iceriyorHepsi(m, listOf("zamanlayici", "duraklat")) ||
            iceriyorHepsi(m, listOf("sayac", "beklet"))
        ) return Komut.SayacDuraklat

        // ── Sayaç başlatma ──
        val sayacKelimesi = m.contains("sayac") || m.contains("zamanlayici") ||
            m.contains("kronometre") || m.contains("odak") || m.contains("pomodoro")
        val baslatKelimesi = m.contains("basla") || m.contains("baslat") ||
            m.contains("kur") || m.contains("ayarla")

        if (sayacKelimesi && (baslatKelimesi || dakikaBul(m) != null)) {
            return Komut.SayacBaslat(dakikaBul(m) ?: varsayilanSure(context))
        }
        // "25 dakika çalış" gibi sayaç kelimesi olmayan ama net kalıplar
        if (dakikaBul(m) != null &&
            (m.contains("calis") || m.contains("odaklan") || baslatKelimesi)
        ) {
            return Komut.SayacBaslat(dakikaBul(m)!!)
        }

        // ── Ders bitirme ──
        if (iceriyorHepsi(m, listOf("ders", "bitir")) ||
            iceriyorHepsi(m, listOf("konu", "bitir")) ||
            iceriyorHepsi(m, listOf("dersi", "tamamla"))
        ) return Komut.DersBitir

        // ── Hata defteri ──
        if (m.contains("hata") && (m.contains("tekrar") || m.contains("coz") ||
                m.contains("defter"))
        ) return Komut.HataTekrar

        // ── Terim sorusu: "X ne demek" ──
        if (m.contains("ne demek") || m.contains("nedir") || m.contains("anlami")) {
            val terim = terimAyikla(ham)
            if (terim.length >= 2) return Komut.TerimSor(terim)
        }

        // ── Durum sorusu ──
        if (m.contains("ne calis") || m.contains("ne yapmali") ||
            m.contains("ne yapayim") || m.contains("bugun ne") ||
            m.contains("durum") || m.contains("nerede kaldim")
        ) return Komut.DurumSor

        // ── Ekran açma ──
        ekranEsle(context, m)?.let { return it }

        return Komut.Anlasilmadi(ham)
    }

    /** "aç/göster" fiiliyle birlikte ekran adı geçiyorsa. */
    private fun ekranEsle(context: Context, m: String): Komut.EkranAc? {
        val acFiili = m.contains("ac") || m.contains("goster") || m.contains("git")
        if (!acFiili) return null

        val esler = listOf(
            Triple(listOf("gorev"), 6, R.string.tab_tasks),
            Triple(listOf("not"), 5, R.string.tab_notes),
            Triple(listOf("konu"), 3, R.string.tab_topics),
            Triple(listOf("sayac", "zamanlayici"), 4, R.string.tab_timer),
            Triple(listOf("kurs", "ders"), 13, R.string.sk_e_kurs),
            Triple(listOf("aliskanlik"), 12, R.string.sk_e_aliskanlik),
            Triple(listOf("takvim", "etkinlik"), 11, R.string.nav_events),
            Triple(listOf("istatistik", "ilerleme"), 1, R.string.nav_progress),
            Triple(listOf("plan", "namaz"), 16, R.string.nav_plan),
            Triple(listOf("asistan", "yapay zeka"), 9, R.string.sk_e_asistan),
            Triple(listOf("ayar"), 7, R.string.sk_e_ayar)
        )

        for ((anahtarlar, ekran, adRes) in esler) {
            if (anahtarlar.any { m.contains(it) }) {
                return Komut.EkranAc(ekran, context.getString(adRes))
            }
        }
        return null
    }

    // ═══════════════════════════════════════════════════════════════
    // UYGULAMA
    // ═══════════════════════════════════════════════════════════════

    /**
     * Komutu çalıştırır.
     *
     * @return kullanıcıya gösterilecek sonuç mesajı; boşsa çağıran
     *         kendi geri bildirimini verir
     */
    fun uygula(context: Context, komut: Komut): String = when (komut) {
        is Komut.SayacBaslat -> {
            TimerEngine.setMode(context, TimerEngine.MODE_DOWN)
            TimerEngine.setTotalMs(context, komut.dakika * 60_000L)
            TimerEngine.start(context)
            TimerAlarm.reschedule(context)
            context.getString(R.string.sk_sayac_basladi, komut.dakika)
        }

        Komut.SayacDurdur -> {
            TimerEngine.creditWatch(context)
            TimerEngine.reset(context)
            TimerAlarm.cancel(context)
            context.getString(R.string.sk_sayac_durdu)
        }

        Komut.SayacDuraklat -> {
            if (TimerEngine.isRunning(context)) {
                TimerEngine.creditWatch(context)
                TimerEngine.pause(context)
                TimerAlarm.cancel(context)
                context.getString(R.string.sk_sayac_duraklatildi)
            } else {
                context.getString(R.string.sk_sayac_zaten_durgun)
            }
        }

        Komut.DersBitir -> {
            val aktif = Mufredat.aktifAdim(context)
            if (aktif == null) {
                context.getString(R.string.mf_aktif_yok)
            } else {
                val sonraki = Mufredat.aktifAdimiBitir(context)
                if (sonraki == null) context.getString(R.string.mf_program_tamam)
                else context.getString(R.string.mf_sirada, sonraki.baslik)
            }
        }

        Komut.DurumSor -> durumOzeti(context)

        else -> ""
    }

    /** "Bugün ne çalışacağım" sorusunun kısa cevabı. */
    private fun durumOzeti(context: Context): String {
        val sb = StringBuilder()
        val oneri = runCatching { SimdiNe.oner(context) }.getOrNull()
        if (oneri != null) {
            sb.append(oneri.simge).append(" ").append(oneri.baslik)
            if (oneri.aciklama.isNotBlank()) sb.append("\n").append(oneri.aciklama)
        } else {
            sb.append(SimdiNe.bosMesaj(context))
        }
        if (Koc.acikMi(context)) {
            val kalan = Koc.bugunKalan(context)
            sb.append("\n\n")
            sb.append(
                if (kalan > 0) context.getString(R.string.sk_hedef_kalan, kalan)
                else context.getString(R.string.koc_m_tamam)
            )
        }
        return sb.toString()
    }

    // ═══════════════════════════════════════════════════════════════
    // YARDIMCILAR
    // ═══════════════════════════════════════════════════════════════

    /** Metinden dakika değeri çıkarır: "25 dakika", "yarım saat", "bir saat". */
    private fun dakikaBul(m: String): Int? {
        // Sayı + dakika/dk
        Regex("(\\d{1,3})\\s*(dakika|dakka|dk|minute)").find(m)?.let {
            return it.groupValues[1].toIntOrNull()?.coerceIn(1, 600)
        }
        // Sayı + saat
        Regex("(\\d{1,2})\\s*saat").find(m)?.let {
            return (it.groupValues[1].toIntOrNull() ?: 0).times(60).coerceIn(1, 600)
        }
        // Yazıyla yaygın süreler
        if (m.contains("yarim saat")) return 30
        if (m.contains("bir saat")) return 60
        if (m.contains("iki saat")) return 120
        if (m.contains("ceyrek saat")) return 15

        val yaziliSayilar = mapOf(
            "bes" to 5, "on" to 10, "onbes" to 15, "on bes" to 15,
            "yirmi" to 20, "yirmibes" to 25, "yirmi bes" to 25,
            "otuz" to 30, "kirk" to 40, "kirkbes" to 45, "kirk bes" to 45,
            "elli" to 50, "altmis" to 60, "doksan" to 90
        )
        for ((yazi, deger) in yaziliSayilar) {
            if (m.contains("$yazi dakika") || m.contains("$yazi dk")) return deger
        }

        // Çıplak sayı — yalnızca sayaç bağlamında anlamlı
        Regex("\\b(\\d{1,3})\\b").find(m)?.let {
            val n = it.groupValues[1].toIntOrNull() ?: return null
            if (n in 1..600) return n
        }
        return null
    }

    private fun varsayilanSure(context: Context): Int =
        if (Pomodoro.acikMi(context)) Pomodoro.calismaDk(context)
        else SayacAyar.varsayilanDk(context)

    /** "X ne demek" kalıbından X'i çıkarır. */
    private fun terimAyikla(ham: String): String {
        var t = ham.trim()
        listOf(" ne demek", " nedir", " anlamı ne", " anlamı nedir", "?").forEach {
            t = t.replace(it, "", ignoreCase = true)
        }
        return t.trim().take(60)
    }

    private fun iceriyorHepsi(m: String, kelimeler: List<String>): Boolean =
        kelimeler.all { m.contains(it) }

    /** Türkçe duyarlı normalleştirme. */
    private fun normalle(s: String): String = s.lowercase(java.util.Locale("tr"))
        .replace("ı", "i").replace("ş", "s").replace("ğ", "g")
        .replace("ü", "u").replace("ö", "o").replace("ç", "c")
        .replace("â", "a").replace("î", "i")
        .trim()

    /** Komutun okunabilir adı — onay ekranında gösterilir. */
    fun komutAdi(context: Context, komut: Komut): String = when (komut) {
        is Komut.SayacBaslat -> context.getString(R.string.sk_ad_sayac, komut.dakika)
        Komut.SayacDurdur -> context.getString(R.string.sk_ad_durdur)
        Komut.SayacDuraklat -> context.getString(R.string.sk_ad_duraklat)
        is Komut.EkranAc -> context.getString(R.string.sk_ad_ekran, komut.ad)
        Komut.DersBitir -> context.getString(R.string.sk_ad_ders_bitir)
        Komut.HataTekrar -> context.getString(R.string.sk_ad_hata)
        is Komut.TerimSor -> context.getString(R.string.sk_ad_terim, komut.terim)
        Komut.DurumSor -> context.getString(R.string.sk_ad_durum)
        is Komut.Anlasilmadi -> ""
    }
}
