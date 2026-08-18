package com.gunlukasistan.app

import java.util.Locale

/**
 * v10.51 — Kullanıcı maddesi 1-16: Sıfırdan Minimalist & Modern Arayüz Devrimi (Tasarım Sistemi v2).
 *
 * 'Az ama öz' felsefesi, #0E0E13 / #1A1B23 / #22232C nötr zeminler,
 * #4C7DFF tek ana vurgu, 8px ızgara, 16px kart yuvarlaklığı, 1px #2A2B35 kenarlık.
 * Tüm görsel kararlar ve durum dönüşümleri bu modülde toplanmıştır (saf, JVM testli).
 */
object TasarimDili {

    // ── RENK PALETİ SABİTLERİ (HEX DİZELERİ & STATİK RENK KODLARI) ──
    const val RENK_ARKA_PLAN = "#0E0E13"
    const val RENK_YUZEY_KART = "#1A1B23"
    const val RENK_YUZEY_IC = "#22232C"
    const val RENK_KENARLIK = "#2A2B35"
    const val RENK_VURGU_ANA = "#4C7DFF"
    const val RENK_METIN_BIRINCIL = "#F5F5F7"
    const val RENK_METIN_IKINCIL = "#8B8D98"

    // ── İKİNCİL DURUM RENKLERİ (Sadece Durum Bildirimi İçin) ──
    const val DURUM_TAMAMLANDI = "#22C55E"    // Yeşil
    const val DURUM_DEVAM_EDIYOR = "#F59E0B"  // Sarı / Amber
    const val DURUM_GECIKMIS = "#EF4444"      // Kırmızı

    enum class DurumSeviyesi {
        TAMAMLANDI,
        DEVAM_EDIYOR,
        GECIKMIS,
        NORMAL
    }

    /**
     * İlerleme yüzdesine göre durum seviyesini belirler.
     */
    fun durumSeviyesi(yuzde: Int, gecikmisMi: Boolean = false): DurumSeviyesi {
        if (gecikmisMi) return DurumSeviyesi.GECIKMIS
        return when {
            yuzde >= 100 -> DurumSeviyesi.TAMAMLANDI
            yuzde > 0 -> DurumSeviyesi.DEVAM_EDIYOR
            else -> DurumSeviyesi.NORMAL
        }
    }

    /**
     * Durum seviyesine uygun hex renk kodunu döndürür.
     */
    fun durumRengiHex(seviye: DurumSeviyesi): String {
        return when (seviye) {
            DurumSeviyesi.TAMAMLANDI -> DURUM_TAMAMLANDI
            DurumSeviyesi.DEVAM_EDIYOR -> DURUM_DEVAM_EDIYOR
            DurumSeviyesi.GECIKMIS -> DURUM_GECIKMIS
            DurumSeviyesi.NORMAL -> RENK_VURGU_ANA
        }
    }

    /**
     * Konu kartları için ilerleme yüzdesine bağlı tek renk skalası (mavi ton yoğunluğu).
     * Yüzde arttıkça opaklık veya yoğunluk katsayısı artar.
     */
    fun konuIlerlemeOpaklik(yuzde: Int): Float {
        val kelepce = yuzde.coerceIn(0, 100)
        return 0.35f + (kelepce / 100f) * 0.65f
    }

    /**
     * Vakit Saati rozet metnini formatlar (Ör: "Öğle 13:02").
     */
    fun vakitRozetMetni(vakitAd: String, saatMetin: String): String {
        val ad = vakitAd.trim()
        val st = saatMetin.trim()
        if (ad.isEmpty() && st.isEmpty()) return ""
        if (ad.isEmpty()) return st
        if (st.isEmpty()) return ad
        return "$ad $st"
    }

    /**
     * Günün Özeti kartı için karşılama ve durum metni bileşimi (Ör: "Kararmaya 7sa 24dk").
     */
    fun gununOzetiMetni(kalanSaat: Int, kalanDk: Int, sinavGunKalan: Int?): String {
        val s = kalanSaat.coerceAtLeast(0)
        val d = kalanDk.coerceIn(0, 59)
        val kararma = if (s > 0 || d > 0) "Kararmaya ${s}sa ${d}dk" else "Akşam oldu"
        if (sinavGunKalan != null && sinavGunKalan > 0) {
            return "$kararma · $sinavGunKalan gün kaldı sınava"
        }
        return kararma
    }

    /**
     * Plan (Vakit Planı) ekranında aktif vakit dilimini indeks olarak döndürür.
     * 0: Seher (00:00-06:00), 1: Kuşluk (06:00-11:00), 2: Öğle (11:00-15:00),
     * 3: İkindi (15:00-18:00), 4: Akşam (18:00-21:00), 5: Gece (21:00-24:00).
     */
    fun aktifVakitDilimiIndeksi(saat24: Int): Int {
        val s = saat24.coerceIn(0, 23)
        return when (s) {
            in 0..5 -> 0   // Seher
            in 6..10 -> 1  // Kuşluk
            in 11..14 -> 2 // Öğleden sonra
            in 15..17 -> 3 // İkindi
            in 18..20 -> 4 // Akşam
            else -> 5      // Gece
        }
    }

    /**
     * Akordiyon (accordion) bölüm durumunu yönetir.
     * Liste boşsa otomatik daraltılmış (collapsed = true) olmalıdır.
     */
    fun akordiyonDaraltilmaliMi(elemanSayisi: Int, kullaniciAcikMi: Boolean): Boolean {
        if (elemanSayisi <= 0) return true // Boşsa her zaman daraltılmış
        return !kullaniciAcikMi
    }

    /**
     * İkonografi denetimi: emojileri temizleyip line-icon sembol tanımı döner.
     */
    fun ikonSembolu(modul: String): String {
        return when (modul.lowercase(Locale.US)) {
            "namaz", "vakit", "camii" -> "◊"
            "kitap", "ders", "konu" -> "□"
            "gorev", "task" -> "☑"
            "odak", "sayac", "timer" -> "○"
            "plan", "takvim" -> "▤"
            else -> "▪"
        }
    }
}
