package com.gunlukasistan.app

import android.content.Context
import java.util.Calendar

/**
 * v7.95 — "Şimdi ne yapmalıyım?" karar motoru.
 *
 * ── Kullanıcı isteği (öneri 3) ──
 * "Bugün ekranı listeliyor. Onun yerine saate, namaz vaktine, koç hedefine
 *  ve geciken görevlere bakıp **tek bir öneri** çıkarsın."
 *
 * ── Neden tek öneri ──
 * Bugün ekranı 6 kart gösteriyordu: görevler, etkinlikler, alışkanlıklar,
 * namaz, istatistik, hızlı düğmeler. Hepsi doğru bilgi ama karar vermiyor.
 * Kullanıcı sabah uygulamayı açtığında "şimdi ne yapayım" sorusunun
 * cevabını arıyor; altı listeyi tarayıp kendi kararını vermek zorunda
 * kalıyordu.
 *
 * ── Puanlama neden kural tabanlı, AI değil ──
 * Bu ekran her açılışta anında görünmeli. AI çağrısı 3-10 saniye sürer ve
 * çevrimdışıyken hiç çalışmaz. Kurallar deterministik, hızlı ve
 * açıklanabilir: kullanıcı neden bu önerinin çıktığını görebiliyor.
 */
object SimdiNe {

    private const val TAG = "SimdiNe"

    /**
     * Bir öneri.
     *
     * @param oncelik yüksek olan kazanır
     * @param neden kullanıcıya gösterilen gerekçe — güven için şart
     * @param eylem dokununca ne olacak
     */
    data class Oneri(
        val baslik: String,
        val aciklama: String,
        val neden: String,
        val simge: String,
        val oncelik: Int,
        val eylem: Eylem,
        val hedefId: Long = 0L
    )

    enum class Eylem {
        SAYAC_BASLAT, GOREV_AC, DERS_CALIS, NAMAZ_AC,
        HATA_TEKRAR, ALISKANLIK_AC, MOLA_VER, PLAN_AC,
        /** v9.0 · Öneri 53: konu maddesi aralıklı tekrarı. */
        KONU_TEKRAR,
        /** v9.4 · Öneri 11: takvimde boş blok bulundu. */
        BOS_ZAMAN
    }

    // ═══════════════════════════════════════════════════════════════
    // ANA GİRİŞ
    // ═══════════════════════════════════════════════════════════════

    /**
     * Şu an için en uygun öneriyi verir.
     *
     * Tüm adaylar puanlanır, en yükseği döner. Hiç aday yoksa null —
     * çağıran "her şey tamam" mesajı gösterir.
     */
    fun oner(context: Context): Oneri? = tumOneriler(context).maxByOrNull { it.oncelik }

    /** İkincil öneriler — ana önerinin altında küçük gösterilir. */
    fun digerleri(context: Context, adet: Int = 2): List<Oneri> =
        tumOneriler(context).sortedByDescending { it.oncelik }.drop(1).take(adet)

    private fun tumOneriler(context: Context): List<Oneri> {
        val liste = mutableListOf<Oneri>()
        runCatching { namazOnerisi(context)?.let { liste.add(it) } }
        runCatching { molaOnerisi(context)?.let { liste.add(it) } }
        runCatching { gecikenGorev(context)?.let { liste.add(it) } }
        runCatching { dersOnerisi(context)?.let { liste.add(it) } }
        runCatching { hataTekrari(context)?.let { liste.add(it) } }
        runCatching { konuTekrari(context)?.let { liste.add(it) } }
        runCatching { bosZaman(context)?.let { liste.add(it) } }
        runCatching { bugunGorev(context)?.let { liste.add(it) } }
        runCatching { aliskanlikOnerisi(context)?.let { liste.add(it) } }
        return liste
    }

    // ═══════════════════════════════════════════════════════════════
    // ADAYLAR — öncelik sırasına göre
    // ═══════════════════════════════════════════════════════════════

    /**
     * Namaz vakti yaklaşıyorsa en yüksek öncelik.
     *
     * Zamana bağlı ve kaçırılınca telafisi olmayan tek madde bu.
     */
    private fun namazOnerisi(context: Context): Oneri? {
        if (!NamazVakti.acikMi(context)) return null
        val gun = runCatching { NamazVakti.bugun(context) }.getOrNull() ?: return null
        val simdi = NamazVakti.simdiDakika()
        val (vakit, vakitDk) = gun.sonraki(simdi)
        // Ertesi güne sarkan vakitte fark negatif olabilir
        val kalanDk = if (vakitDk >= simdi) vakitDk - simdi else (1440 - simdi) + vakitDk
        // 25 dakikadan az kaldıysa öne çıkar
        if (kalanDk !in 0..25) return null

        return Oneri(
            baslik = context.getString(R.string.sn_namaz, context.getString(vakit.adRes)),
            aciklama = context.getString(R.string.sn_namaz_alt, kalanDk),
            neden = context.getString(R.string.sn_neden_namaz),
            simge = "🕌",
            oncelik = 100,
            eylem = Eylem.NAMAZ_AC
        )
    }

    /** Pomodoro molasındaysa çalışma önerme. */
    private fun molaOnerisi(context: Context): Oneri? {
        if (!Pomodoro.acikMi(context)) return null
        if (!Pomodoro.molada(context)) return null
        if (TimerEngine.isRunning(context)) return null

        return Oneri(
            baslik = context.getString(R.string.sn_mola),
            aciklama = context.getString(
                R.string.sn_mola_alt, Pomodoro.evreSuresi(context)
            ),
            neden = context.getString(R.string.sn_neden_mola),
            simge = "☕",
            oncelik = 95,
            eylem = Eylem.MOLA_VER
        )
    }

    /** Süresi geçmiş görev — biriktikçe kötüleşir. */
    private fun gecikenGorev(context: Context): Oneri? {
        val simdi = System.currentTimeMillis()
        val gecikenler = Store.loadTasks(context)
            .filter { !it.done && !it.arsiv && it.dueAt in 1 until simdi }
        if (gecikenler.isEmpty()) return null

        val enEski = gecikenler.minByOrNull { it.dueAt } ?: return null
        val gunSayisi = ((simdi - enEski.dueAt) / 86_400_000L).toInt()

        return Oneri(
            baslik = enEski.text,
            aciklama = if (gecikenler.size > 1) {
                context.getString(R.string.sn_geciken_coklu, gecikenler.size)
            } else {
                context.getString(R.string.sn_geciken_tek)
            },
            neden = if (gunSayisi > 0) {
                context.getString(R.string.sn_neden_geciken, gunSayisi)
            } else {
                context.getString(R.string.sn_neden_geciken_bugun)
            },
            simge = "⚠️",
            // Gecikme arttıkça öncelik yükselir ama 90'ı aşmaz
            oncelik = (70 + gunSayisi * 3).coerceAtMost(90),
            eylem = Eylem.GOREV_AC,
            hedefId = enEski.id
        )
    }

    /**
     * Koç hedefi tutmadıysa ders çalış.
     *
     * Günün ilerleyen saatlerinde öncelik artar — akşam 21:00'de kalan
     * 40 dakika, sabah 09:00'daki 40 dakikadan daha acildir.
     */
    private fun dersOnerisi(context: Context): Oneri? {
        if (!Koc.acikMi(context)) return null
        if (!Koc.bugunCalismaGunuMu(context)) return null
        val kalan = Koc.bugunKalan(context)
        if (kalan <= 0) return null
        if (TimerEngine.isRunning(context)) return null

        val aktif = Mufredat.aktifAdim(context)
        val saat = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        // 18:00'den sonra her saat +6 puan
        val gecSaatCarpani = if (saat >= 18) (saat - 17) * 6 else 0

        return Oneri(
            baslik = aktif?.baslik ?: context.getString(R.string.sn_ders_genel),
            aciklama = context.getString(R.string.sn_ders_alt, kalan),
            neden = if (Koc.borc(context) > 0) {
                context.getString(R.string.sn_neden_borc, Koc.borc(context))
            } else {
                context.getString(R.string.sn_neden_hedef)
            },
            simge = "📚",
            oncelik = (55 + gecSaatCarpani).coerceAtMost(88),
            eylem = Eylem.DERS_CALIS,
            hedefId = aktif?.id ?: 0L
        )
    }

    /** Hata defterinde bugün tekrar edilecek soru varsa. */
    private fun hataTekrari(context: Context): Oneri? {
        val bekleyen = Hatalarim.bugunkuSayi(context)
        if (bekleyen == 0) return null

        return Oneri(
            baslik = context.getString(R.string.sn_hata, bekleyen),
            aciklama = context.getString(R.string.sn_hata_alt),
            neden = context.getString(R.string.sn_neden_hata),
            simge = "🎯",
            oncelik = 50,
            eylem = Eylem.HATA_TEKRAR
        )
    }

    /**
     * v9.0 · Öneri 53 — Konu maddesi tekrarı.
     *
     * ── Öncelik neden 62 ──
     * Hata tekrarından (50) YÜKSEK, geciken görevden (70-90) düşük.
     * Gerekçe: aralıklı tekrarın değeri zamanlamasında. Bugün
     * yapılmayan tekrar yarın daha zor hatırlanır ve programı bozar.
     * Ama acil bir görev kadar da bağlayıcı değil.
     *
     * ── Neden riskli maddeler ayrı ──
     * Vakti gelmemiş ama unutulmak üzere olan maddeler varsa
     * kullanıcı uyarılıyor (öneri 55 — unutma eğrisi).
     */
    private fun konuTekrari(context: Context): Oneri? {
        if (!KonuTekrar.acikMi(context)) return null
        val bekleyen = KonuTekrar.bugunkuSayi(context)
        if (bekleyen == 0) return null

        return Oneri(
            baslik = context.getString(R.string.sn_konu_tekrar, bekleyen),
            aciklama = context.getString(R.string.sn_konu_tekrar_alt),
            neden = context.getString(R.string.sn_neden_konu_tekrar),
            simge = "🔁",
            oncelik = 62,
            eylem = Eylem.KONU_TEKRAR
        )
    }

    /**
     * v9.4 · Öneri 11 — Takvimde boş blok.
     *
     * ── Öncelik neden 45 ──
     * En düşük adaylardan biri. "Boşsun" bilgisi yararlı ama acil
     * değil; gerçek bir iş (görev, tekrar) varsa o öne çıkmalı.
     * Bu öneri ancak yapacak somut bir şey kalmadığında görünüyor.
     *
     * ── Neden en az 45 dakika ──
     * Kısa boşluklarda "çalış" demek rahatsız edici. 45 dakika bir
     * pomodoro + mola demek.
     */
    private fun bosZaman(context: Context): Oneri? {
        if (!TakvimKopru.acikMi(context)) return null
        val bosluk = TakvimKopru.enUzunBosluk(context) ?: return null
        if (bosluk.sureDk < 45) return null

        return Oneri(
            baslik = context.getString(R.string.sn_bos_zaman, bosluk.sureDk),
            aciklama = bosluk.saatMetni(),
            neden = context.getString(R.string.sn_neden_bos_zaman),
            simge = "🕳",
            oncelik = 45,
            eylem = Eylem.SAYAC_BASLAT
        )
    }

    /** Bugüne planlanmış görev. */
    private fun bugunGorev(context: Context): Oneri? {
        val simdi = System.currentTimeMillis()
        val gunSonu = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
        }.timeInMillis

        val bugunkuler = Store.loadTasks(context)
            .filter { !it.done && !it.arsiv && it.dueAt in simdi..gunSonu }
        if (bugunkuler.isEmpty()) return null

        val ilk = bugunkuler.minByOrNull { it.dueAt } ?: return null
        return Oneri(
            baslik = ilk.text,
            aciklama = if (bugunkuler.size > 1) {
                context.getString(R.string.sn_bugun_coklu, bugunkuler.size)
            } else {
                context.getString(R.string.sn_bugun_tek)
            },
            neden = context.getString(R.string.sn_neden_bugun),
            simge = "📋",
            oncelik = 45,
            eylem = Eylem.GOREV_AC,
            hedefId = ilk.id
        )
    }

    /** Bugün yapılmamış alışkanlık — gün sonuna yaklaştıkça öne çıkar. */
    private fun aliskanlikOnerisi(context: Context): Oneri? {
        val eksikler = runCatching {
            Store.loadHabits(context)
                .filter { !it.archived }
                .filter { Store.habitCount(context, it.id) < it.target }
        }.getOrNull() ?: return null
        if (eksikler.isEmpty()) return null

        val saat = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        // Akşam 20:00'den sonra hatırlat
        val oncelik = if (saat >= 20) 60 else 30

        return Oneri(
            baslik = eksikler.first().title,
            aciklama = if (eksikler.size > 1) {
                context.getString(R.string.sn_aliskanlik_coklu, eksikler.size)
            } else {
                context.getString(R.string.sn_aliskanlik_tek)
            },
            neden = if (saat >= 20) context.getString(R.string.sn_neden_aliskanlik_gec)
            else context.getString(R.string.sn_neden_aliskanlik),
            simge = "🔄",
            oncelik = oncelik,
            eylem = Eylem.ALISKANLIK_AC
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // BOŞ DURUM
    // ═══════════════════════════════════════════════════════════════

    /** Yapılacak bir şey kalmadığında gösterilecek mesaj. */
    fun bosMesaj(context: Context): String {
        val saat = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return context.getString(
            when {
                saat < 12 -> R.string.sn_bos_sabah
                saat < 18 -> R.string.sn_bos_gunduz
                else -> R.string.sn_bos_aksam
            }
        )
    }
}
