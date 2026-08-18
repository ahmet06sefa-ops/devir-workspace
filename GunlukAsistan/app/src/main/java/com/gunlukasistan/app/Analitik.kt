package com.gunlukasistan.app

import android.content.Context
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * v7.38 — Detaylı ilerleme analitiği.
 *
 * ── Neden gerekti? ──
 * ProgressFragment yalnızca 168 satırdı: aylık ısı haritası + iki özet satırı.
 * Veri zaten toplanıyordu ama hiç analiz edilmiyordu. Bu sınıf ham günlük
 * kayıtları anlamlı çıkarımlara çevirir.
 *
 * ── Üretilen analizler ──
 *  1. Saat dağılımı      — günün hangi saatinde verimlisin (v7.38'de eklenen "h" alanı)
 *  2. Gün dağılımı       — haftanın hangi günü daha çok çalışıyorsun
 *  3. Kurs hızı          — ders başına ortalama dakika, kalan süre
 *  4. Bitiş tahmini      — mevcut hızla kurs ne zaman biter
 *  5. Aylık karşılaştırma— bu ay vs geçen ay
 *  6. Haftalık eğilim    — son 4 hafta yükseliyor mu düşüyor mu
 *
 * Tüm hesaplar saf Kotlin; Android'e yalnızca Store ve string kaynakları için bağlı.
 */
object Analitik {

    private const val TAG = "Analitik"
    private val trLocale = Locale("tr", "TR")

    // ═══════════════════════════════════════════════════════════════
    // 1) SAAT DAĞILIMI
    // ═══════════════════════════════════════════════════════════════

    /** Saat dilimi özeti. */
    data class SaatDilimi(val ad: String, val baslangic: Int, val bitis: Int, val puan: Int)

    /**
     * Son [gun] günün saat bazlı verim dağılımı.
     * @return 24 elemanlı dizi — indeks = saat, değer = toplam puan
     */
    fun saatDagilimi(context: Context, gun: Int = 60): IntArray {
        val sonuc = IntArray(24)
        try {
            val kok = gunlukKok(context)
            val cal = Calendar.getInstance()
            repeat(gun) {
                val anahtar = SimpleDateFormat("yyyyMMdd", Locale.US).format(cal.time)
                val g = kok.optJSONObject(anahtar)
                val saatler = g?.optJSONArray("h")
                if (saatler != null) {
                    for (i in 0 until minOf(24, saatler.length())) {
                        sonuc[i] += saatler.optInt(i, 0)
                    }
                }
                cal.add(Calendar.DAY_OF_YEAR, -1)
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Saat dağılımı okunamadı", e)
        }
        return sonuc
    }

    /** Saatleri 4 dilime böler: sabah / öğle / akşam / gece. */
    fun saatDilimleri(context: Context, gun: Int = 60): List<SaatDilimi> {
        val d = saatDagilimi(context, gun)
        fun topla(bas: Int, bit: Int): Int {
            var t = 0
            for (i in bas until bit) t += d[i]
            return t
        }
        return listOf(
            SaatDilimi(context.getString(R.string.an_slot_morning), 6, 12, topla(6, 12)),
            SaatDilimi(context.getString(R.string.an_slot_noon), 12, 18, topla(12, 18)),
            SaatDilimi(context.getString(R.string.an_slot_evening), 18, 24, topla(18, 24)),
            SaatDilimi(context.getString(R.string.an_slot_night), 0, 6, topla(0, 6))
        )
    }

    /** En verimli saat (0-23). Veri yoksa -1. */
    fun enVerimliSaat(context: Context, gun: Int = 60): Int {
        val d = saatDagilimi(context, gun)
        val enBuyuk = d.maxOrNull() ?: 0
        if (enBuyuk <= 0) return -1
        return d.indexOfFirst { it == enBuyuk }
    }

    /** Saat dağılımında hiç veri var mı? (Yeni kullanıcıda boş olur.) */
    fun saatVerisiVarMi(context: Context, gun: Int = 60): Boolean =
        saatDagilimi(context, gun).any { it > 0 }

    // ═══════════════════════════════════════════════════════════════
    // 2) HAFTANIN GÜNLERİ
    // ═══════════════════════════════════════════════════════════════

    /** Gün adı + toplam puan + aktif gün sayısı. */
    data class GunOzet(val ad: String, val kisaAd: String, val puan: Int, val aktifGun: Int) {
        val ortalama: Int get() = if (aktifGun == 0) 0 else puan / aktifGun
    }

    /**
     * Haftanın günlerine göre dağılım (Pazartesi'den başlar).
     * "Hangi gün daha verimlisin" sorusunu yanıtlar.
     */
    fun gunDagilimi(context: Context, hafta: Int = 12): List<GunOzet> {
        val adlar = listOf(
            R.string.an_day_mon to R.string.an_day_mon_s,
            R.string.an_day_tue to R.string.an_day_tue_s,
            R.string.an_day_wed to R.string.an_day_wed_s,
            R.string.an_day_thu to R.string.an_day_thu_s,
            R.string.an_day_fri to R.string.an_day_fri_s,
            R.string.an_day_sat to R.string.an_day_sat_s,
            R.string.an_day_sun to R.string.an_day_sun_s
        )
        val puanlar = IntArray(7)
        val aktifler = IntArray(7)

        try {
            val kok = gunlukKok(context)
            val cal = Calendar.getInstance()
            repeat(hafta * 7) {
                val anahtar = SimpleDateFormat("yyyyMMdd", Locale.US).format(cal.time)
                val g = kok.optJSONObject(anahtar)
                if (g != null) {
                    // Calendar.MONDAY = 2 → indeks 0
                    val i = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
                    val puan = g.optInt("c") * 3 + g.optInt("f") + g.optInt("q") / 4
                    if (puan > 0) {
                        puanlar[i] += puan
                        aktifler[i]++
                    }
                }
                cal.add(Calendar.DAY_OF_YEAR, -1)
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Gün dağılımı okunamadı", e)
        }

        return adlar.mapIndexed { i, (uzun, kisa) ->
            GunOzet(
                context.getString(uzun), context.getString(kisa), puanlar[i], aktifler[i]
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 3) KURS HIZI VE BİTİŞ TAHMİNİ
    // ═══════════════════════════════════════════════════════════════

    /**
     * Bir kursun ilerleme analizi.
     *
     * @param haftalikHiz son 4 haftada tamamlanan ders / hafta
     * @param kalanHafta bu hızla kaç hafta kalır (-1 = hesaplanamaz)
     * @param tahminiTarih bitiş tarihi metni (boş = hesaplanamaz)
     */
    data class KursHiz(
        val kursId: Long,
        val kursAdi: String,
        val emoji: String,
        val toplamDers: Int,
        val bitenDers: Int,
        val toplamDakika: Int,
        val bitenDakika: Int,
        val haftalikHiz: Float,
        val kalanHafta: Int,
        val tahminiTarih: String
    ) {
        val yuzde: Int get() = if (toplamDers == 0) 0 else bitenDers * 100 / toplamDers
        val kalanDers: Int get() = (toplamDers - bitenDers).coerceAtLeast(0)
        val kalanDakika: Int get() = (toplamDakika - bitenDakika).coerceAtLeast(0)
        /** Ders başına ortalama süre. */
        val dersBasiDakika: Int get() = if (toplamDers == 0) 0 else toplamDakika / toplamDers
    }

    /**
     * Tüm kursların hız analizi.
     *
     * Hız hesabı: son 4 haftada tamamlanan ders sayısına bakılır.
     * Ders tamamlanma tarihi ayrıca tutulmadığı için, günlük kayıttaki
     * tamamlama sayısı kurs oranına göre dağıtılır — yaklaşık ama tutarlı.
     */
    fun kursHizlari(context: Context): List<KursHiz> {
        val kurslar = Store.loadCourses(context)
        if (kurslar.isEmpty()) return emptyList()

        val dersler = Store.loadLessons(context)
        // Son 4 haftada toplam kaç madde tamamlanmış
        val sonDortHafta = Store.recentDayStats(context, 28).sumOf { it.second }
        val toplamBiten = dersler.count { it.done }.coerceAtLeast(1)

        return kurslar.map { kurs ->
            val kursDersleri = dersler.filter { it.courseId == kurs.id }
            val biten = kursDersleri.count { it.done }
            val toplam = kursDersleri.size

            // Bu kursun son 4 haftadaki payı — tamamlanan ders oranına göre
            val pay = if (toplamBiten == 0) 0f else biten.toFloat() / toplamBiten
            val haftalikHiz = (sonDortHafta * pay / 4f).coerceAtLeast(0f)

            val kalan = (toplam - biten).coerceAtLeast(0)
            val kalanHafta = if (haftalikHiz < 0.2f || kalan == 0) -1
            else Math.ceil((kalan / haftalikHiz).toDouble()).toInt().coerceAtMost(520)

            val tarih = if (kalanHafta <= 0) "" else {
                val c = Calendar.getInstance()
                c.add(Calendar.WEEK_OF_YEAR, kalanHafta)
                SimpleDateFormat("d MMMM yyyy", trLocale).format(c.time)
            }

            KursHiz(
                kursId = kurs.id,
                kursAdi = kurs.title,
                emoji = kurs.emoji,
                toplamDers = toplam,
                bitenDers = biten,
                toplamDakika = kursDersleri.sumOf { it.minutes },
                bitenDakika = kursDersleri.filter { it.done }.sumOf { it.minutes },
                haftalikHiz = haftalikHiz,
                kalanHafta = kalanHafta,
                tahminiTarih = tarih
            )
        }.sortedByDescending { it.yuzde }
    }

    // ═══════════════════════════════════════════════════════════════
    // 4) AYLIK KARŞILAŞTIRMA
    // ═══════════════════════════════════════════════════════════════

    data class AyOzet(
        val ad: String,
        val madde: Int,
        val dakika: Int,
        val aktifGun: Int,
        val soru: Int
    )

    /** Son [ay] ayın özeti — en yeniden eskiye. */
    fun aylikOzet(context: Context, ay: Int = 6): List<AyOzet> {
        val liste = mutableListOf<AyOzet>()
        val cal = Calendar.getInstance()
        repeat(ay) {
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH)
            liste.add(
                AyOzet(
                    ad = SimpleDateFormat("MMMM yyyy", trLocale).format(cal.time),
                    madde = Store.monthCompletions(context, y, m),
                    dakika = Store.monthFocus(context, y, m),
                    aktifGun = Store.monthActiveDays(context, y, m),
                    soru = Store.monthQuestions(context, y, m)
                )
            )
            cal.add(Calendar.MONTH, -1)
        }
        return liste
    }

    /** Bu ay geçen aya göre yüzde kaç değişmiş? (odak dakikası üzerinden) */
    fun aylikDegisim(context: Context): Int {
        val now = Calendar.getInstance()
        val buAy = Store.monthFocus(context, now.get(Calendar.YEAR), now.get(Calendar.MONTH))
        val gecen = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
        val gecenAy = Store.monthFocus(
            context, gecen.get(Calendar.YEAR), gecen.get(Calendar.MONTH)
        )
        if (gecenAy <= 0) return if (buAy > 0) 100 else 0
        return (buAy - gecenAy) * 100 / gecenAy
    }

    // ═══════════════════════════════════════════════════════════════
    // 5) HAFTALIK EĞİLİM
    // ═══════════════════════════════════════════════════════════════

    data class HaftaOzet(val etiket: String, val madde: Int, val dakika: Int)

    /** Son [hafta] haftanın özeti — eskiden yeniye (grafik için). */
    fun haftalikEgilim(context: Context, hafta: Int = 8): List<HaftaOzet> {
        val gunler = Store.recentDayStats(context, hafta * 7)
        val liste = mutableListOf<HaftaOzet>()
        // recentDayStats bugünden geriye sıralı — 7'şerli grupla
        for (h in 0 until hafta) {
            val dilim = gunler.drop(h * 7).take(7)
            if (dilim.isEmpty()) continue
            liste.add(
                HaftaOzet(
                    etiket = if (h == 0) context.getString(R.string.an_this_week)
                    else context.getString(R.string.an_weeks_ago, h),
                    madde = dilim.sumOf { it.second },
                    dakika = dilim.sumOf { it.third }
                )
            )
        }
        return liste.reversed()
    }

    // ═══════════════════════════════════════════════════════════════
    // 6) AKILLI ÇIKARIMLAR
    // ═══════════════════════════════════════════════════════════════

    /**
     * Verilerden okunabilir çıkarımlar üretir.
     * Ekranın en üstünde "senin için ne anlama geliyor" bölümü.
     */
    // ═══════════════════════════════════════════════════════════════
    // v7.75 — HAFTALIK KARŞILAŞTIRMA
    // ═══════════════════════════════════════════════════════════════

    /**
     * Bu hafta ile geçen haftanın karşılaştırması.
     *
     * @param buHafta bu haftanın toplam odak dakikası
     * @param gecenHafta geçen haftanın toplamı
     * @param yuzde değişim yüzdesi (+ artış, − azalış)
     * @param yeterliVeri geçen hafta hiç veri yoksa karşılaştırma anlamsız
     */
    data class HaftaKarsilastirma(
        val buHafta: Int,
        val gecenHafta: Int,
        val yuzde: Int,
        val yeterliVeri: Boolean
    )

    /**
     * Son 7 gün ile ondan önceki 7 günü karşılaştırır.
     *
     * Takvim haftası yerine kayan pencere kullanıldı: pazartesi sabahı
     * "bu hafta" bomboş olurdu ve karşılaştırma anlamsız görünürdü.
     */
    fun haftaKarsilastir(context: Context): HaftaKarsilastirma {
        return try {
            val kok = gunlukKok(context)
            val bicim = java.text.SimpleDateFormat("yyyyMMdd", Locale.US)

            fun araToplam(baslangicGunOnce: Int, adet: Int): Int {
                var toplam = 0
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -baslangicGunOnce)
                repeat(adet) {
                    val anahtar = bicim.format(cal.time)
                    toplam += kok.optJSONObject(anahtar)?.optInt("f", 0) ?: 0
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                }
                return toplam
            }

            val bu = araToplam(0, 7)
            val gecen = araToplam(7, 7)
            val yuzde = when {
                gecen > 0 -> (bu - gecen) * 100 / gecen
                bu > 0 -> 100
                else -> 0
            }
            HaftaKarsilastirma(bu, gecen, yuzde, gecen > 0)
        } catch (e: Exception) {
            android.util.Log.w("Analitik", "Hafta karşılaştırılamadı", e)
            HaftaKarsilastirma(0, 0, 0, false)
        }
    }

    fun cikarimlar(context: Context): List<String> {
        val liste = mutableListOf<String>()

        // Saat önerisi
        val saat = enVerimliSaat(context)
        if (saat >= 0) {
            val dilim = saatDilimleri(context).maxByOrNull { it.puan }
            if (dilim != null && dilim.puan > 0) {
                liste.add(
                    context.getString(
                        R.string.an_insight_hour,
                        String.format(Locale.US, "%02d:00", saat),
                        dilim.ad
                    )
                )
            }
        }

        // En verimli gün
        val gunler = gunDagilimi(context)
        val enIyiGun = gunler.filter { it.aktifGun > 0 }.maxByOrNull { it.ortalama }
        val enKotuGun = gunler.filter { it.aktifGun > 0 }.minByOrNull { it.ortalama }
        if (enIyiGun != null && enIyiGun.puan > 0) {
            liste.add(context.getString(R.string.an_insight_day, enIyiGun.ad))
        }
        if (enKotuGun != null && enIyiGun != null &&
            enKotuGun.ad != enIyiGun.ad && enKotuGun.ortalama * 2 < enIyiGun.ortalama
        ) {
            liste.add(context.getString(R.string.an_insight_weak_day, enKotuGun.ad))
        }

        // Aylık değişim
        val degisim = aylikDegisim(context)
        if (degisim != 0) {
            liste.add(
                context.getString(
                    if (degisim > 0) R.string.an_insight_up else R.string.an_insight_down,
                    Math.abs(degisim)
                )
            )
        }

        // Kurs bitiş tahmini — en ilerideki kurs
        val kurs = kursHizlari(context)
            .filter { it.kalanHafta > 0 && it.tahminiTarih.isNotBlank() }
            .maxByOrNull { it.yuzde }
        if (kurs != null) {
            liste.add(
                context.getString(
                    R.string.an_insight_finish,
                    kurs.kursAdi, kurs.tahminiTarih
                )
            )
        }

        // Seri durumu
        val (seri, rekor) = Store.streakInfo(context)
        if (seri > 0 && seri >= rekor && rekor > 1) {
            liste.add(context.getString(R.string.an_insight_record, seri))
        } else if (seri >= 3) {
            liste.add(context.getString(R.string.an_insight_streak, seri))
        }

        return liste
    }

    // ═══════════════════════════════════════════════════════════════
    // ORTAK
    // ═══════════════════════════════════════════════════════════════

    /**
     * Günlük kayıt kökünü okur.
     * Store içindeki logRoot private olduğu için burada yeniden okunuyor.
     */
    private fun gunlukKok(context: Context): JSONObject =
        Store.gunlukKayitKopyasi(context)

    /** Toplam çalışma günü sayısı — "kaç gündür kullanıyorsun". */
    fun toplamAktifGun(context: Context): Int {
        var n = 0
        try {
            val kok = gunlukKok(context)
            kok.keys().forEach { anahtar ->
                val g = kok.optJSONObject(anahtar) ?: return@forEach
                if (g.optInt("c") > 0 || g.optInt("f") > 0 || g.optInt("q") > 0) n++
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Aktif gün sayılamadı", e)
        }
        return n
    }

    // ═══════════════════════════════════════════════════════════════
    // v7.97 — DERİN İSTATİSTİK (öneri 10)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Program bitiş tahmini.
     *
     * @param kalanAdim bitmemiş ders/madde sayısı
     * @param gunlukHiz son dönemde günde ortalama kaç adım bitiyor
     * @param tahminiGun kalan gün sayısı; -1 = hesaplanamıyor
     */
    data class BitisTahmini(
        val kalanAdim: Int,
        val gunlukHiz: Double,
        val tahminiGun: Int,
        val tarihMetni: String
    ) {
        val hesaplanabildi: Boolean get() = tahminiGun > 0
    }

    /**
     * "Bu hızla program ne zaman biter?"
     *
     * ── Hesap ──
     * Son 30 günde tamamlanan adım sayısına bakılır. Günlük hız
     * (adım/gün) bulunur, kalan adım buna bölünür.
     *
     * ── Neden 30 gün ──
     * Daha kısa aralık tek bir yoğun günden etkilenir; daha uzun aralık
     * kullanıcının hızlanmasını/yavaşlamasını geç yansıtır.
     *
     * Hiç adım bitirilmemişse tahmin yapılamaz — uydurma bir tarih
     * göstermek yanıltıcı olurdu.
     */
    fun bitisTahmini(context: Context): BitisTahmini {
        return try {
            val adimlar = Mufredat.adimlar(context)
            val kalan = adimlar.count { !it.bitti }
            if (kalan == 0) {
                return BitisTahmini(0, 0.0, 0, context.getString(R.string.dst_bitti))
            }

            // Son 30 günde bitirilen adımları say
            val sinir = System.currentTimeMillis() - 30L * 86_400_000L
            val sonBitenler = adimlar.count { adim ->
                val kayit = Mufredat.adimKaydi(context, adim.id)
                kayit.bitirildi in (sinir + 1)..System.currentTimeMillis()
            }

            if (sonBitenler == 0) {
                return BitisTahmini(kalan, 0.0, -1, context.getString(R.string.dst_veri_yok))
            }

            val hiz = sonBitenler / 30.0
            val gun = kotlin.math.ceil(kalan / hiz).toInt().coerceAtMost(3650)

            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, gun)
            val bicim = java.text.SimpleDateFormat("d MMMM yyyy", Locale("tr"))

            BitisTahmini(kalan, hiz, gun, bicim.format(cal.time))
        } catch (e: Exception) {
            android.util.Log.w("Analitik", "Bitiş tahmini yapılamadı", e)
            BitisTahmini(0, 0.0, -1, "")
        }
    }

    /**
     * Zayıf konu — hata defteri ve sözlük verisinden.
     *
     * @param konu ders/konu adı
     * @param hataSayisi o konudan kaç yanlış
     * @param terimSayisi o konuda kaç terim soruldu
     */
    data class ZayifKonu(val konu: String, val hataSayisi: Int, val terimSayisi: Int) {
        /** Toplam zorlanma göstergesi. */
        val puan: Int get() = hataSayisi * 2 + terimSayisi
    }

    /**
     * "Hangi konuda en çok takılıyorsun?"
     *
     * Hata defteri (yanlış cevaplar) ve sözlük (sorulan terimler)
     * birleştirilir. Hata daha ağır sayılır (×2): bir terimi merak etmek
     * öğrenmenin parçası, aynı soruyu yanlış yapmak bilgi eksikliği.
     */
    fun zayifKonular(context: Context, adet: Int = 5): List<ZayifKonu> {
        return try {
            val harita = mutableMapOf<String, Pair<Int, Int>>()

            Hatalarim.kaynakDagilimi(context).forEach { (kaynak, sayi) ->
                if (kaynak.isNotBlank() && kaynak != "—") {
                    val mevcut = harita[kaynak] ?: (0 to 0)
                    harita[kaynak] = (mevcut.first + sayi) to mevcut.second
                }
            }

            Sozluk.baglamlar(context).forEach { (baglam, sayi) ->
                if (baglam.isNotBlank()) {
                    val mevcut = harita[baglam] ?: (0 to 0)
                    harita[baglam] = mevcut.first to (mevcut.second + sayi)
                }
            }

            harita.map { (konu, ciftler) ->
                ZayifKonu(konu, ciftler.first, ciftler.second)
            }.sortedByDescending { it.puan }.take(adet)
        } catch (e: Exception) {
            android.util.Log.w("Analitik", "Zayıf konular bulunamadı", e)
            emptyList()
        }
    }

    /**
     * Odak oturumlarından çıkan içgörüler.
     *
     * [OdakKaydi] her sayaç oturumunu derse yazıyor; buradan
     * "ortalama oturum uzunluğu" ve "en çok çalışılan konu" çıkarılıyor.
     */
    data class OdakOzet(
        val toplamOturum: Int,
        val toplamDakika: Int,
        val ortalamaOturum: Int,
        val enCokKonu: String,
        val enCokDakika: Int
    )

    fun odakOzeti(context: Context, gun: Int = 30): OdakOzet {
        return try {
            val sinir = System.currentTimeMillis() - gun * 86_400_000L
            val oturumlar = OdakKaydi.oturumlar(context).filter { it.zaman >= sinir }

            if (oturumlar.isEmpty()) return OdakOzet(0, 0, 0, "", 0)

            val toplamDk = oturumlar.sumOf { it.dakika }
            val dagilim = oturumlar
                .filter { it.baslik.isNotBlank() }
                .groupBy { it.baslik }
                .map { (ad, liste) -> ad to liste.sumOf { it.dakika } }
                .maxByOrNull { it.second }

            OdakOzet(
                toplamOturum = oturumlar.size,
                toplamDakika = toplamDk,
                ortalamaOturum = toplamDk / oturumlar.size,
                enCokKonu = dagilim?.first.orEmpty(),
                enCokDakika = dagilim?.second ?: 0
            )
        } catch (e: Exception) {
            android.util.Log.w("Analitik", "Odak özeti çıkarılamadı", e)
            OdakOzet(0, 0, 0, "", 0)
        }
    }

    /**
     * v7.97 — Genişletilmiş çıkarımlar.
     *
     * Mevcut [cikarimlar] odak/soru verisine bakıyordu. Bu sürüm program
     * ilerlemesi, zayıf konular ve oturum alışkanlıklarını da ekliyor.
     */
    fun derinCikarimlar(context: Context): List<String> {
        val liste = mutableListOf<String>()

        // Not: her blok Unit dönmeli. runCatching'in son ifadesi bir `if`
        // olursa Kotlin onu değer sayar ve "else şart" hatası verir;
        // bu yüzden bloklar açıkça Unit ile kapatılıyor.
        runCatching {
            val tahmin = bitisTahmini(context)
            when {
                tahmin.hesaplanabildi -> liste.add(
                    context.getString(
                        R.string.dst_tahmin,
                        tahmin.kalanAdim, tahmin.tahminiGun, tahmin.tarihMetni
                    )
                )
                tahmin.kalanAdim > 0 -> liste.add(
                    context.getString(R.string.dst_hiz_yok, tahmin.kalanAdim)
                )
            }
            Unit
        }

        runCatching {
            val zayif = zayifKonular(context, 1).firstOrNull()
            if (zayif != null && zayif.puan >= 3) {
                liste.add(
                    context.getString(R.string.dst_zayif, zayif.konu, zayif.hataSayisi)
                )
            }
            Unit
        }

        runCatching {
            val odak = odakOzeti(context, 30)
            if (odak.toplamOturum >= 3) {
                liste.add(
                    context.getString(
                        R.string.dst_oturum, odak.ortalamaOturum, odak.toplamOturum
                    )
                )
                if (odak.enCokKonu.isNotBlank()) {
                    liste.add(
                        context.getString(
                            R.string.dst_en_cok, odak.enCokKonu, odak.enCokDakika
                        )
                    )
                }
            }
            Unit
        }

        runCatching {
            val saat = enVerimliSaat(context, 60)
            if (saat >= 0) {
                liste.add(context.getString(R.string.dst_saat, saat, saat + 1))
            }
            Unit
        }

        return liste
    }
}
