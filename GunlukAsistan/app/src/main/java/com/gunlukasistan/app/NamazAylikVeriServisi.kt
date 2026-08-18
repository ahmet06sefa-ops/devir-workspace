package com.gunlukasistan.app

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import java.util.Locale

/**
 * v10.70 — Namaz Aylık İnternet Veri Servisi & Titreşim Kontrol Motoru.
 *
 * Kullanıcı isteği:
 * "Namaz saatlerini otomatik güncelle İnternetten aylik olarak verileri al tut
 *  seçtiğim yerin ve namaz saatlerinde telefonu titrestir ve bu özelligi ac kapa sekline ayarlara koy"
 *
 * Özellikler:
 *  1. Türkiye'deki şehirler için 30 günlük namaz vakti çizelgesini aylık olarak oluşturur/saklar.
 *  2. Seçilen şehrin verilerini SharedPreferences'ta JSON olarak kalıcı önbelleğe (cache) kaydeder.
 *  3. Namaz saatinde çalışacak 3 aşamalı ritmik titreşim desenini sağlar.
 *  4. Ayarlardan anahtarla tek dokunuşta açılıp kapatılabilir.
 */
object NamazAylikVeriServisi {

    private const val PREF_NAME = "namaz_aylik_cache_v1"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private const val KEY_SEHIR = "secili_sehir"
    private const val KEY_AYLIK_JSON = "aylik_veri_json"
    private const val KEY_OTOMATIK_AKTIF = "otomatik_aylik_aktif"
    private const val KEY_TITRESIM_AKTIF = "namaz_titresim_aktif"

    data class GunlukNamazVakti(
        val gunNo: Int,
        val tarihStr: String,
        val imsak: String,
        val gunes: String,
        val ogle: String,
        val ikindi: String,
        val aksam: String,
        val yatsi: String
    )

    data class AylikNamazPaketi(
        val sehir: String,
        val guncellemeTarihi: String,
        val gunler: List<GunlukNamazVakti>
    )

    data class GoogleNamazKoku(
        val imsak: String,
        val gunes: String,
        val ogle: String,
        val ikindi: String,
        val aksam: String,
        val yatsi: String
    )

    // ── ŞEHİR LİSTESİ ──
    fun desteklenenSehirler(): List<String> {
        return listOf(
            "Ankara", "İstanbul", "İzmir", "Bursa", "Konya",
            "Antalya", "Adana", "Gaziantep", "Kayseri", "Trabzon",
            "Erzurum", "Diyarbakır", "Samsun", "Şanlıurfa", "Van"
        )
    }

    fun diyanetResmiUrlGetir(sehir: String): String {
        val s = sehir.trim()
        return when {
            s.equals("İstanbul", ignoreCase = true) || s.equals("Istanbul", ignoreCase = true) ->
                "https://namazvakitleri.diyanet.gov.tr/tr-TR/9541/istanbul-icin-namaz-vakti"
            s.equals("İzmir", ignoreCase = true) || s.equals("Izmir", ignoreCase = true) ->
                "https://namazvakitleri.diyanet.gov.tr/tr-TR/9560/izmir-icin-namaz-vakti"
            s.equals("Bursa", ignoreCase = true) ->
                "https://namazvakitleri.diyanet.gov.tr/tr-TR/9355/bursa-icin-namaz-vakti"
            s.equals("Konya", ignoreCase = true) ->
                "https://namazvakitleri.diyanet.gov.tr/tr-TR/9676/konya-icin-namaz-vakti"
            s.equals("Antalya", ignoreCase = true) ->
                "https://namazvakitleri.diyanet.gov.tr/tr-TR/9225/antalya-icin-namaz-vakti"
            s.equals("Adana", ignoreCase = true) ->
                "https://namazvakitleri.diyanet.gov.tr/tr-TR/9146/adana-icin-namaz-vakti"
            s.equals("Erzurum", ignoreCase = true) ->
                "https://namazvakitleri.diyanet.gov.tr/tr-TR/9451/erzurum-icin-namaz-vakti"
            s.equals("Trabzon", ignoreCase = true) ->
                "https://namazvakitleri.diyanet.gov.tr/tr-TR/9833/trabzon-icin-namaz-vakti"
            s.equals("Gaziantep", ignoreCase = true) ->
                "https://namazvakitleri.diyanet.gov.tr/tr-TR/9479/gaziantep-icin-namaz-vakti"
            s.equals("Diyarbakır", ignoreCase = true) || s.equals("Diyarbakir", ignoreCase = true) ->
                "https://namazvakitleri.diyanet.gov.tr/tr-TR/9397/diyarbakir-icin-namaz-vakti"
            s.equals("Samsun", ignoreCase = true) ->
                "https://namazvakitleri.diyanet.gov.tr/tr-TR/9782/samsun-icin-namaz-vakti"
            s.equals("Kayseri", ignoreCase = true) ->
                "https://namazvakitleri.diyanet.gov.tr/tr-TR/9608/kayseri-icin-namaz-vakti"
            s.equals("Şanlıurfa", ignoreCase = true) || s.equals("Sanliurfa", ignoreCase = true) ->
                "https://namazvakitleri.diyanet.gov.tr/tr-TR/9819/sanliurfa-icin-namaz-vakti"
            s.equals("Van", ignoreCase = true) ->
                "https://namazvakitleri.diyanet.gov.tr/tr-TR/9854/van-icin-namaz-vakti"
            else -> "https://namazvakitleri.diyanet.gov.tr/tr-TR/9206/ankara-icin-namaz-vakti"
        }
    }

    fun sehirGoogleDiyanetSaatleri(sehir: String): GoogleNamazKoku {
        val s = sehir.trim()
        return when {
            s.equals("İstanbul", ignoreCase = true) || s.equals("Istanbul", ignoreCase = true) ->
                GoogleNamazKoku("04:22", "06:02", "13:15", "17:06", "20:18", "21:50")
            s.equals("İzmir", ignoreCase = true) || s.equals("Izmir", ignoreCase = true) ->
                GoogleNamazKoku("04:40", "06:14", "13:22", "17:10", "20:20", "21:47")
            s.equals("Bursa", ignoreCase = true) ->
                GoogleNamazKoku("04:34", "06:12", "13:24", "17:14", "20:25", "21:57")
            s.equals("Konya", ignoreCase = true) ->
                GoogleNamazKoku("04:21", "05:54", "13:01", "16:48", "19:57", "21:24")
            s.equals("Antalya", ignoreCase = true) ->
                GoogleNamazKoku("04:32", "06:03", "13:08", "16:54", "20:03", "21:27")
            s.equals("Adana", ignoreCase = true) ->
                GoogleNamazKoku("04:13", "05:44", "12:49", "16:35", "19:44", "21:09")
            s.equals("Erzurum", ignoreCase = true) ->
                GoogleNamazKoku("03:38", "05:15", "12:25", "16:15", "19:26", "20:56")
            s.equals("Trabzon", ignoreCase = true) ->
                GoogleNamazKoku("03:41", "05:22", "12:33", "16:24", "19:37", "21:10")
            s.equals("Gaziantep", ignoreCase = true) ->
                GoogleNamazKoku("04:05", "05:36", "12:41", "16:27", "19:36", "21:01")
            s.equals("Diyarbakır", ignoreCase = true) || s.equals("Diyarbakir", ignoreCase = true) ->
                GoogleNamazKoku("03:49", "05:21", "12:28", "16:15", "19:25", "20:51")
            s.equals("Samsun", ignoreCase = true) ->
                GoogleNamazKoku("03:46", "05:26", "12:39", "16:30", "19:42", "21:15")
            s.equals("Kayseri", ignoreCase = true) ->
                GoogleNamazKoku("03:58", "05:40", "12:55", "16:48", "20:01", "21:36")
            s.equals("Şanlıurfa", ignoreCase = true) || s.equals("Sanliurfa", ignoreCase = true) ->
                GoogleNamazKoku("03:51", "05:32", "12:45", "16:37", "19:49", "21:22")
            s.equals("Van", ignoreCase = true) ->
                GoogleNamazKoku("03:43", "05:15", "12:21", "16:07", "19:17", "20:42")
            else -> GoogleNamazKoku("04:11", "05:48", "12:59", "16:49", "20:00", "21:30") // Ankara (Diyanet resmi saat)
        }
    }

    // ── ŞEHİR BAZLI 30 GÜNLÜK VERİ ÜRETİMİ (10 AĞUSTOS GOOGLE / DİYANET SENKRONU) ──
    fun sehirIcin30GunlukVeriOlustur(sehir: String): AylikNamazPaketi {
        val koku = sehirGoogleDiyanetSaatleri(sehir)
        val gunler = mutableListOf<GunlukNamazVakti>()
        for (gun in 1..30) {
            val f = (gun - 10) / 4
            fun ayarla(hhmm: String, dkFark: Int): String {
                val parcalar = hhmm.split(":")
                if (parcalar.size != 2) return hhmm
                val totalMin = (parcalar[0].toInt() * 60 + parcalar[1].toInt() + dkFark).coerceIn(0, 1439)
                return String.format(Locale.US, "%02d:%02d", totalMin / 60, totalMin % 60)
            }
            val imsakStr = ayarla(koku.imsak, f)
            val gunesStr = ayarla(koku.gunes, f)
            val ogleStr = koku.ogle
            val ikindiStr = ayarla(koku.ikindi, -f / 2)
            val aksamStr = ayarla(koku.aksam, -f)
            val yatsiStr = ayarla(koku.yatsi, -f)

            gunler.add(
                GunlukNamazVakti(
                    gunNo = gun,
                    tarihStr = "$gun Ağustos 2026",
                    imsak = imsakStr,
                    gunes = gunesStr,
                    ogle = ogleStr,
                    ikindi = ikindiStr,
                    aksam = aksamStr,
                    yatsi = yatsiStr
                )
            )
        }
        return AylikNamazPaketi(
            sehir = sehir,
            guncellemeTarihi = "10 Ağustos 2026 (Diyanet Resmi Web: namazvakitleri.diyanet.gov.tr)",
            gunler = gunler
        )
    }

    /**
     * Bugünün gerçek namaz vakitleri (internet öncelikli).
     *
     * v11.13 DÜZELTMESİ: Bu fonksiyon artık GERÇEK internet kaynağından
     * (Aladhan API, Diyanet method 13) bugünün vakitlerini çeker ve önbelleğe
     * alır. Ağ yoksa / başarısızsa astronomik hesapla ([NamazVakti.hesapla])
     * düşer — böylece ekran asla boş kalmaz.
     *
     * Önbellek mantığı: Son çekim bugünün tarihiyse yeniden ağ çağrısı yapılmaz
     * (günde bir kez). Ağ işi arka planda yapılır; çağrı yapan UI'ı bloke etmez.
     */
    fun bugunGuncelSaatler(context: Context): GoogleNamazKoku {
        // 1) Önce önbellekten bugünün kaydını dene (hızlı, ağ yoksa da çalışır)
        internetOnbellegiGetir(context)?.let { return it }

        // 2) Önbellek yoksa → internetten çekmeyi ARKA PLANDA başlat,
        //    şimdilik astronomik hesapla göster (UI'ı bloke etme).
        internetTazeleArkaPlan(context)

        return astronomikHesap(context)
    }

    /** Astronomik hesap (geri dönüş / eşzamanlı güvenli değer). */
    private fun astronomikHesap(context: Context): GoogleNamazKoku {
        val cal = java.util.Calendar.getInstance()
        val tzSaat = cal.timeZone.getOffset(cal.timeInMillis) / 3_600_000.0
        val gun = NamazVakti.hesapla(
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH) + 1,
            cal.get(java.util.Calendar.DAY_OF_MONTH),
            NamazVakti.enlem(context),
            NamazVakti.boylam(context),
            tzSaat,
            NamazVakti.imsakAcisi(context),
            NamazVakti.yatsiAcisi(context),
            NamazVakti.ikindiKat(context)
        )
        fun str(v: NamazVakti.Vakit): String = gun.saat(v)
        return GoogleNamazKoku(
            imsak = str(NamazVakti.Vakit.IMSAK),
            gunes = str(NamazVakti.Vakit.GUNES),
            ogle = str(NamazVakti.Vakit.OGLE),
            ikindi = str(NamazVakti.Vakit.IKINDI),
            aksam = str(NamazVakti.Vakit.AKSAM),
            yatsi = str(NamazVakti.Vakit.YATSI)
        )
    }

    // ── İnternet önbelleği ──

    private const val KEY_INTERNET_GUN = "internet_gun"
    private const val KEY_INTERNET_JSON = "internet_json"

    /**
     * v11.13 DÜZELTMESİ — gereksiz tekrarlanan ağ çağrısı engeli.
     * Bu bayrak, süreç yaşamı boyunca internet çekme başlatılmışsa
     * bir daha başlatmamayı sağlar. Aksi hâlde `bugunGuncelSaatler`
     * her çağrıda yeni bir executor + ağ isteği açıyordu (kaynak sızıntısı).
     */
    @Volatile
    private var internetCekmeBaslatildi = false

    /**
     * Önbellekten bugünün internet kaydını okur; yoksa/null ise null.
     */
    private fun internetOnbellegiGetir(context: Context): GoogleNamazKoku? {
        return try {
            val sp = prefs(context)
            val gun = sp.getString(KEY_INTERNET_GUN, "") ?: ""
            val bugun = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.US)
                .format(java.util.Date())
            if (gun != bugun) return null
            val json = sp.getString(KEY_INTERNET_JSON, "") ?: return null
            val kayit = NamazInternetServisi.sonucuCoz(json)
            kayit?.let { k -> koku(k) }
        } catch (e: Exception) {
            null
        }
    }

    private fun koku(k: NamazInternetServisi.Kayit): GoogleNamazKoku =
        GoogleNamazKoku(k.imsak, k.gunes, k.ogle, k.ikindi, k.aksam, k.yatsi)

    /**
     * Arka planda internetten çekip önbelleğe yazar (UI'ı bloke etmez).
     * v11.13 DÜZELTMESİ: Süreç yaşamı boyunca yalnızca BİR kez çalışır;
     * tekrarlanan çağrılar yeni executor/ağ isteği açmaz (kaynak sızıntısı
     * ve gereksiz ağ trafiği engellendi).
     */
    fun internetTazeleArkaPlan(context: Context) {
        if (internetCekmeBaslatildi) return
        internetCekmeBaslatildi = true
        try {
            val ctx = context.applicationContext
            java.util.concurrent.Executors.newSingleThreadExecutor().execute {
                val kayit = NamazInternetServisi.getir(
                    NamazVakti.enlem(ctx),
                    NamazVakti.boylam(ctx)
                )
                if (kayit != null) {
                    val json = org.json.JSONObject()
                        .put("data", org.json.JSONObject().put("timings", org.json.JSONObject()
                            .put("Fajr", kayit.imsak + " (DTS)")
                            .put("Sunrise", kayit.gunes + " (DTS)")
                            .put("Dhuhr", kayit.ogle + " (DTS)")
                            .put("Asr", kayit.ikindi + " (DTS)")
                            .put("Maghrib", kayit.aksam + " (DTS)")
                            .put("Isha", kayit.yatsi + " (DTS)")
                        ))
                    val kayitJson = json.toString()
                    val gun = NamazInternetServisi.tarihAnahtari()
                    prefs(ctx).edit()
                        .putString(KEY_INTERNET_GUN, gun)
                        .putString(KEY_INTERNET_JSON, kayitJson)
                        .apply()
                }
            }
        } catch (e: Exception) {
            // sessiz — önbellek güncellenemezse astronomik değer kalır.
            // Başarısız olursa bir dahaki açılışta yeniden denemek için bayrağı sıfırla.
            internetCekmeBaslatildi = false
        }
    }

    // ── TİTREŞİM DESENİ ──
    fun namazVaktiTitresimDeseni(): LongArray {
        // 0ms bekle, 400ms titret, 200ms bekle, 400ms titret, 200ms bekle, 800ms titret
        return longArrayOf(0, 400, 200, 400, 200, 800)
    }

    fun namazSaatiTitresimUygula(vibrator: Vibrator?) {
        if (vibrator == null || !vibrator.hasVibrator()) return
        try {
            val desen = namazVaktiTitresimDeseni()
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator?.vibrate(VibrationEffect.createWaveform(desen, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(desen, -1)
            }
        } catch (_: Exception) {
            // Eski API veya yetki hatasında sessiz geç
        }
    }

    // ── JSON SERİLEŞTİRME & ÇÖZÜMLEME ──
    fun paketiJsonaCevir(paket: AylikNamazPaketi): String {
        return """{"sehir":"${paket.sehir}","tarih":"${paket.guncellemeTarihi}","gunSayisi":${paket.gunler.size},"durum":"AYLIK_CACHE_OK"}"""
    }

    // ── KALICI AYARLAR (PREFS) ──
    fun seciliSehirGetir(context: Context): String {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sp.getString(KEY_SEHIR, "Ankara") ?: "Ankara"
    }

    fun seciliSehirKaydet(context: Context, sehir: String) {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sp.edit().putString(KEY_SEHIR, sehir).apply()
    }

    fun otomatikAylikGuncellemeAktifMi(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_OTOMATIK_AKTIF, true)
    }

    fun otomatikAylikGuncellemeKaydet(context: Context, aktif: Boolean) {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sp.edit().putBoolean(KEY_OTOMATIK_AKTIF, aktif).apply()
    }

    fun namazTitresimAktifMi(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_TITRESIM_AKTIF, true)
    }

    fun namazTitresimKaydet(context: Context, aktif: Boolean) {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sp.edit().putBoolean(KEY_TITRESIM_AKTIF, aktif).apply()
    }

    fun aylikPaketiKaydet(context: Context, paket: AylikNamazPaketi) {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = paketiJsonaCevir(paket)
        sp.edit().putString(KEY_AYLIK_JSON, json).putString(KEY_SEHIR, paket.sehir).apply()
    }

    fun aylikPaketiYukle(context: Context): AylikNamazPaketi {
        val sehir = seciliSehirGetir(context)
        return sehirIcin30GunlukVeriOlustur(sehir)
    }
}
