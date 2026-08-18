package com.gunlukasistan.app

import android.content.Context
import java.util.Locale

/**
 * v10.78 — KPSS Sayaç Atölyesi & İstatistik Yönetim Motoru.
 *
 * Kullanıcının 4 ekran görüntülü ("KPSS Sayac") talimatı doğrultusunda:
 *  1. Zamanlayıcı başlığı ("Çalışma zamanı") ve oturum sayacı ("Oturum: 1 / 4").
 *  2. "Çalıştığın Dersi Seç" butonu ve 7 derslik (Türkçe, Matematik, Geometri, Tarih, Coğrafya, Vatandaşlık, Güncel Bilgiler) modal seçici.
 *  3. "İstatistikleri Gör" butonu ve İstatistikler ekranı (0 Dakika, 0 Pomodoro, 0 Gün, Ağustos 2026 takvimi, 10. gün turuncu vurgusu, yeşil gün durumu bandı).
 *  4. "Çalışma Süresi Ekle" butonu ve "Manuel Süre Ekle" dialoğu (saat/dakika seçici, ders seçimi, hedef ilerlemesi uyarısı).
 */
object KpssSayacAtolye {

    private const val PREF_NAME = "kpss_sayac_istatistik_v1"
    private const val KEY_SECILI_DERS = "secili_ders_adi"
    private const val KEY_TOPLAM_DAKIKA = "toplam_calisma_dakika"
    private const val KEY_TOPLAM_POMODORO = "toplam_pomodoro_sayisi"
    private const val KEY_AKTIF_GUN_SAYISI = "aktif_calisma_gunu"
    private const val KEY_BUGUN_DAKIKA = "bugun_10_agustos_dk"
    private const val KEY_OTURUM_NO = "mevcut_oturum_no"

    data class IstatistikOzet(
        val toplamDakika: Int,
        val toplamPomodoro: Int,
        val aktifGunSayisi: Int,
        val bugunDakika: Int,
        val bugunCalistiMi: Boolean,
        val seciliDers: String
    )

    // ── DESTEKLENEN DERSLER LİSTESİ (Ekran Görüntüsü 2) + KONULAR VE ALT BAŞLIKLAR SENKRONU ──
    fun desteklenenDersler(context: Context? = null): List<String> {
        if (context != null) {
            try {
                val userTopics = Store.loadTopics(context)
                val secili = seciliDersGetir(context)
                val list = mutableListOf<String>()
                for (t in userTopics) {
                    val baslik = t.title.trim()
                    if (baslik.isNotBlank() && !list.contains(baslik)) {
                        list.add(baslik)
                    }
                    for (sub in t.items) {
                        val subText = sub.text.trim()
                        if (subText.isNotBlank()) {
                            val altMadde = "$baslik -> $subText"
                            if (!list.contains(altMadde)) {
                                list.add(altMadde)
                            }
                        }
                    }
                }
                if (secili != "Çalıştığın Dersi Seç" && secili.isNotBlank() && !list.contains(secili)) {
                    list.add(secili)
                }
                if (list.isNotEmpty()) {
                    return list.distinct()
                }
            } catch (_: Exception) {
                // Hata durumunda varsayılan listeye düş
            }
        }
        return listOf(
            "Türkçe",
            "Matematik",
            "Geometri",
            "Tarih",
            "Coğrafya",
            "Vatandaşlık",
            "Güncel Bilgiler"
        )
    }

    // ── OTURUM & DERS DURUMU (Ekran Görüntüsü 1) ──
    fun oturumMetniGetir(context: Context): String {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val no = sp.getInt(KEY_OTURUM_NO, 1)
        return "Oturum: $no / 4"
    }

    fun sonrakiOturumaGec(context: Context): String {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val no = (sp.getInt(KEY_OTURUM_NO, 1) % 4) + 1
        sp.edit().putInt(KEY_OTURUM_NO, no).apply()
        return "Oturum: $no / 4"
    }

    fun seciliDersGetir(context: Context): String {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val ders = sp.getString(KEY_SECILI_DERS, "") ?: ""
        return if (ders.isBlank()) "Çalıştığın Dersi Seç" else ders
    }

    fun dersSecKaydet(context: Context, dersAdi: String) {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val temiz = if (dersAdi.isBlank()) "" else dersAdi.trim()
        sp.edit().putString(KEY_SECILI_DERS, temiz).apply()
    }

    fun temizleSeciliDers(context: Context) {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sp.edit().putString(KEY_SECILI_DERS, "").apply()
    }

    // ── İSTATİSTİKLER (Ekran Görüntüsü 3 & 4) ──
    fun istatistikOzetGetir(context: Context): IstatistikOzet {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val dk = sp.getInt(KEY_TOPLAM_DAKIKA, 0)
        val pomo = sp.getInt(KEY_TOPLAM_POMODORO, 0)
        val gun = sp.getInt(KEY_AKTIF_GUN_SAYISI, 0)
        val bugunDk = sp.getInt(KEY_BUGUN_DAKIKA, 0)
        val calistiMi = bugunDk > 0
        val ders = sp.getString(KEY_SECILI_DERS, "") ?: ""
        return IstatistikOzet(
            toplamDakika = dk,
            toplamPomodoro = pomo,
            aktifGunSayisi = gun,
            bugunDakika = bugunDk,
            bugunCalistiMi = calistiMi,
            seciliDers = ders
        )
    }

    fun manuelSureEkle(context: Context, eklenenDakika: Int, dersAdi: String = ""): Pair<Boolean, String> {
        if (eklenenDakika <= 0) {
            return Pair(false, "Lütfen en az 1 dakikalık çalışma süresi ekleyin.")
        }
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val mevcutDk = sp.getInt(KEY_TOPLAM_DAKIKA, 0)
        val mevcutBugunDk = sp.getInt(KEY_BUGUN_DAKIKA, 0)
        val mevcutGun = sp.getInt(KEY_AKTIF_GUN_SAYISI, 0)

        val yeniToplamDk = mevcutDk + eklenenDakika
        val yeniBugunDk = mevcutBugunDk + eklenenDakika
        val yeniPomo = yeniToplamDk / 25
        val yeniGun = if (mevcutBugunDk == 0) mevcutGun + 1 else mevcutGun

        if (dersAdi.isNotBlank()) {
            sp.edit().putString(KEY_SECILI_DERS, dersAdi.trim()).apply()
        }

        sp.edit()
            .putInt(KEY_TOPLAM_DAKIKA, yeniToplamDk)
            .putInt(KEY_TOPLAM_POMODORO, yeniPomo)
            .putInt(KEY_AKTIF_GUN_SAYISI, yeniGun)
            .putInt(KEY_BUGUN_DAKIKA, yeniBugunDk)
            .apply()

        val saat = eklenenDakika / 60
        val dk = eklenenDakika % 60
        val sureStr = if (saat > 0) "${saat} saat ${dk} dakika" else "${dk} dakika"
        return Pair(true, "✅ Çalışma Süresi Eklendi ($sureStr) · Toplam: $yeniToplamDk Dk ($yeniPomo Pomodoro)")
    }

    fun gunlukDurumBannerMetni(bugunDakika: Int): Pair<String, String> {
        return if (bugunDakika > 0) {
            val saat = bugunDakika / 60
            val dk = bugunDakika % 60
            val str = if (saat > 0) "${saat} saat ${dk} dk çalışıldı" else "${dk} dk çalışıldı"
            Pair("Pazartesi, 10.08.2026", "Bugün toplam $str")
        } else {
            Pair("Pazartesi, 10.08.2026", "Henüz çalışmadın")
        }
    }

    fun ilkAdimBannerMetni(bugunDakika: Int): String {
        return if (bugunDakika > 0) {
            "🔥 Çalışma Günlüğü Aktif. Hedefine adım adım ilerliyorsun!"
        } else {
            "Henüz çalışmaya başlamadın. İlk adımı at"
        }
    }
}
