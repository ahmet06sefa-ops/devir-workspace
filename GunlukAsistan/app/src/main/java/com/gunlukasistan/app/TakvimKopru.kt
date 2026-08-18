package com.gunlukasistan.app

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.util.Calendar
import java.util.TimeZone

/**
 * v9.4 — Sistem takvimi köprüsü (öneri 9, 10, 11).
 *
 * ══════════════════════════════════════════════════════════════════
 * NEDEN GEREKLİ
 * ══════════════════════════════════════════════════════════════════
 * Uygulama 194 dosya ve 17 ekran ama **telefonun geri kalanından
 * tamamen kopuk**. `CalendarContract` kod tabanında hiç geçmiyordu.
 *
 * Sonuçları:
 *   · Sınav tarihini iki yere girmek gerekiyor
 *   · Uygulama "bugün boşsun, çalış" diyor ama saat 14'te dersin var
 *   · Aile takvimini paylaşanlar planı göremiyor
 *
 * ══════════════════════════════════════════════════════════════════
 * TASARIM KARARLARI
 * ══════════════════════════════════════════════════════════════════
 *
 * ── Neden ayrı bir takvim oluşturmuyoruz ──
 * `CalendarContract.Calendars`'a yeni takvim eklemek `ACCOUNT_TYPE`
 * yönetimi gerektiriyor ve kullanıcı hesabıyla eşleşmezse etkinlikler
 * senkronlanmıyor. Bunun yerine kullanıcının SEÇTİĞİ mevcut takvime
 * yazıyoruz — Google hesabıyla zaten senkronlanan takvime.
 *
 * ── Neden her etkinliğe damga koyuyoruz ──
 * `DESCRIPTION` alanına `[GunlukAsistan#<tip>#<id>]` yazılıyor.
 * Böylece:
 *   · Aynı etkinlik iki kez eklenmiyor (güncelleme yapılıyor)
 *   · Uygulama silinince veya sınav iptal edilince temizlenebiliyor
 *   · Kullanıcının kendi etkinliklerine dokunulmuyor
 *
 * ── İzin ──
 * `READ_CALENDAR` / `WRITE_CALENDAR` tehlikeli izin. Kullanıcı
 * reddederse tüm fonksiyonlar sessizce boş dönüyor — özellik
 * kapalıymış gibi davranıyor, uygulama çalışmaya devam ediyor.
 *
 * ── 🔴 Cihazda test edilemiyor ──
 * Sandbox'ta takvim sağlayıcısı yok. Son iki sürümdeki çökmeler
 * (splash, activity-alias) test edilemeyen cihaz API'lerinden
 * kaynaklandı. Bu yüzden BURADAKİ HER ÇAĞRI `runCatching` içinde ve
 * hata durumunda boş/false dönüyor. Takvim çalışmasa bile uygulama
 * çalışır.
 */
object TakvimKopru {

    private const val TAG = "TakvimKopru"
    private const val PREF = "takvim_v1"
    private const val K_ACIK = "acik"
    private const val K_TAKVIM_ID = "takvim_id"
    private const val K_YAZ_SINAV = "yaz_sinav"
    private const val K_YAZ_ETKINLIK = "yaz_etkinlik"
    private const val K_OKU = "oku"

    /** Etkinlik açıklamasına konan damga. */
    private const val DAMGA_ONEK = "[GunlukAsistan#"

    private fun p(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ══════════════════════════════════════════════════════════
    // Ayarlar
    // ══════════════════════════════════════════════════════════

    fun acikMi(c: Context): Boolean = p(c).getBoolean(K_ACIK, false) && izinVar(c)

    fun ac(c: Context, deger: Boolean) {
        p(c).edit().putBoolean(K_ACIK, deger).apply()
    }

    fun seciliTakvim(c: Context): Long = p(c).getLong(K_TAKVIM_ID, -1L)

    fun takvimSec(c: Context, id: Long) {
        p(c).edit().putLong(K_TAKVIM_ID, id).apply()
    }

    fun sinavYaz(c: Context): Boolean = p(c).getBoolean(K_YAZ_SINAV, true)
    fun sinavYaz(c: Context, d: Boolean) { p(c).edit().putBoolean(K_YAZ_SINAV, d).apply() }

    fun etkinlikYaz(c: Context): Boolean = p(c).getBoolean(K_YAZ_ETKINLIK, true)
    fun etkinlikYaz(c: Context, d: Boolean) { p(c).edit().putBoolean(K_YAZ_ETKINLIK, d).apply() }

    fun okumaAcik(c: Context): Boolean = p(c).getBoolean(K_OKU, true)
    fun okumaAcik(c: Context, d: Boolean) { p(c).edit().putBoolean(K_OKU, d).apply() }

    // ══════════════════════════════════════════════════════════
    // İzin
    // ══════════════════════════════════════════════════════════

    fun okumaIzniVar(c: Context): Boolean = runCatching {
        ContextCompat.checkSelfPermission(c, android.Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    fun yazmaIzniVar(c: Context): Boolean = runCatching {
        ContextCompat.checkSelfPermission(c, android.Manifest.permission.WRITE_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    fun izinVar(c: Context): Boolean = okumaIzniVar(c) && yazmaIzniVar(c)

    val IZINLER = arrayOf(
        android.Manifest.permission.READ_CALENDAR,
        android.Manifest.permission.WRITE_CALENDAR
    )

    // ══════════════════════════════════════════════════════════
    // Takvim listesi
    // ══════════════════════════════════════════════════════════

    data class Takvim(val id: Long, val ad: String, val hesap: String, val renk: Int)

    /** Yazılabilir takvimler. Boş dönerse izin yok veya takvim yok. */
    fun takvimler(c: Context): List<Takvim> {
        if (!okumaIzniVar(c)) return emptyList()
        return runCatching {
            val liste = mutableListOf<Takvim>()
            val sutunlar = arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.CALENDAR_COLOR,
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
            )
            c.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI, sutunlar, null, null, null
            )?.use { imlec ->
                while (imlec.moveToNext()) {
                    val erisim = imlec.getInt(4)
                    // Yalnız yazılabilir takvimler
                    if (erisim < CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR) continue
                    liste.add(
                        Takvim(
                            id = imlec.getLong(0),
                            ad = imlec.getString(1) ?: "?",
                            hesap = imlec.getString(2) ?: "",
                            renk = imlec.getInt(3)
                        )
                    )
                }
            }
            liste
        }.onFailure { android.util.Log.w(TAG, "takvimler", it) }.getOrDefault(emptyList())
    }

    /** Seçili takvim yoksa ilk yazılabiliri seç. */
    private fun hedefTakvim(c: Context): Long {
        val secili = seciliTakvim(c)
        if (secili > 0) return secili
        val ilk = takvimler(c).firstOrNull()?.id ?: -1L
        if (ilk > 0) takvimSec(c, ilk)
        return ilk
    }

    // ══════════════════════════════════════════════════════════
    // Öneri 9 — Takvime yazma
    // ══════════════════════════════════════════════════════════

    /**
     * Etkinlik ekler veya günceller.
     *
     * @param tip damga tipi ("sinav", "etkinlik", "ders")
     * @param kimlik uygulamadaki kayıt kimliği
     * @param tumGun saatsiz etkinlik mi
     * @return takvim etkinlik kimliği, başarısızsa -1
     */
    fun yaz(
        c: Context,
        tip: String,
        kimlik: Long,
        baslik: String,
        baslangicMs: Long,
        bitisMs: Long = baslangicMs + 3_600_000L,
        aciklama: String = "",
        tumGun: Boolean = false
    ): Long {
        if (!acikMi(c) || !yazmaIzniVar(c)) return -1L
        val takvimId = hedefTakvim(c)
        if (takvimId <= 0) return -1L

        return runCatching {
            val damga = "$DAMGA_ONEK$tip#$kimlik]"
            val mevcut = bul(c, damga)

            val degerler = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, takvimId)
                put(CalendarContract.Events.TITLE, baslik)
                put(
                    CalendarContract.Events.DESCRIPTION,
                    if (aciklama.isBlank()) damga else "$aciklama\n\n$damga"
                )
                put(CalendarContract.Events.DTSTART, baslangicMs)
                put(CalendarContract.Events.DTEND, bitisMs)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                if (tumGun) put(CalendarContract.Events.ALL_DAY, 1)
            }

            if (mevcut > 0) {
                // Güncelle — aynı etkinlik iki kez eklenmesin
                val uri = ContentUris.withAppendedId(
                    CalendarContract.Events.CONTENT_URI, mevcut
                )
                c.contentResolver.update(uri, degerler, null, null)
                mevcut
            } else {
                val uri = c.contentResolver.insert(
                    CalendarContract.Events.CONTENT_URI, degerler
                )
                uri?.lastPathSegment?.toLongOrNull() ?: -1L
            }
        }.onFailure { android.util.Log.w(TAG, "yaz($tip#$kimlik)", it) }.getOrDefault(-1L)
    }

    /** Damgaya göre var olan etkinliği bulur. */
    private fun bul(c: Context, damga: String): Long = runCatching {
        c.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            arrayOf(CalendarContract.Events._ID),
            "${CalendarContract.Events.DESCRIPTION} LIKE ?",
            arrayOf("%$damga%"),
            null
        )?.use { if (it.moveToFirst()) it.getLong(0) else -1L } ?: -1L
    }.getOrDefault(-1L)

    /** Uygulamanın eklediği bir etkinliği siler. */
    fun sil(c: Context, tip: String, kimlik: Long): Boolean {
        if (!yazmaIzniVar(c)) return false
        return runCatching {
            val id = bul(c, "$DAMGA_ONEK$tip#$kimlik]")
            if (id <= 0) return false
            c.contentResolver.delete(
                ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id),
                null, null
            ) > 0
        }.getOrDefault(false)
    }

    /**
     * Tüm uygulama etkinliklerini takvimle eşitler.
     *
     * Sınavlar ve kişisel etkinlikler yazılıyor. Ayarlardan
     * "Şimdi eşitle" ile veya etkinlik eklenince çağrılıyor.
     *
     * @return yazılan etkinlik sayısı
     */
    fun tumunuEsitle(c: Context): Int {
        if (!acikMi(c)) return 0
        var sayac = 0
        runCatching {
            // Sınav tarihi
            if (sinavYaz(c)) {
                val ms = Store.getExamDateMillis(c)
                if (ms > 0) {
                    val ad = c.getString(R.string.tk_sinav_baslik)
                    if (yaz(c, "sinav", 1L, ad, ms, ms + 10_800_000L, tumGun = true) > 0) sayac++
                }
            }
            // Kişisel etkinlikler
            if (etkinlikYaz(c)) {
                Store.upcomingEvents(c).forEach { e ->
                    val ms = e.millis
                    if (ms > 0) {
                        val id = yaz(
                            c, "etkinlik", e.id,
                            "${e.emoji} ${e.title}",
                            ms, ms + 3_600_000L, tumGun = true
                        )
                        if (id > 0) sayac++
                    }
                }
            }
        }.onFailure { android.util.Log.w(TAG, "tumunuEsitle", it) }
        return sayac
    }

    // ══════════════════════════════════════════════════════════
    // Öneri 10 — Takvimden okuma
    // ══════════════════════════════════════════════════════════

    /** Takvimden okunan etkinlik. */
    data class Etkinlik(
        val id: Long,
        val baslik: String,
        val baslangicMs: Long,
        val bitisMs: Long,
        val tumGun: Boolean,
        val renk: Int,
        /** Bizim eklediğimiz mi? (çizelgede iki kez görünmesin) */
        val bizimMi: Boolean
    ) {
        val baslangicDk: Int
            get() = Calendar.getInstance().apply { timeInMillis = baslangicMs }
                .let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }

        val sureDk: Int
            get() = ((bitisMs - baslangicMs) / 60_000L).toInt().coerceIn(15, 480)
    }

    /**
     * Belirli bir günün etkinlikleri.
     *
     * `Instances` tablosu kullanılıyor — tekrarlayan etkinliklerin
     * o güne düşen örneklerini de veriyor. `Events` tablosu yalnız
     * ana kaydı verir ve haftalık ders tekrarları görünmezdi.
     */
    fun gununEtkinlikleri(c: Context, gun: Calendar = Calendar.getInstance()): List<Etkinlik> {
        if (!okumaAcik(c) || !okumaIzniVar(c)) return emptyList()

        return runCatching {
            val bas = (gun.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val son = bas + 86_400_000L

            val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
                .appendPath(bas.toString())
                .appendPath(son.toString())
                .build()

            val sutunlar = arrayOf(
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.DISPLAY_COLOR,
                CalendarContract.Instances.DESCRIPTION
            )

            val liste = mutableListOf<Etkinlik>()
            c.contentResolver.query(uri, sutunlar, null, null, "${CalendarContract.Instances.BEGIN} ASC")
                ?.use { imlec ->
                    while (imlec.moveToNext()) {
                        val aciklama = imlec.getString(6) ?: ""
                        liste.add(
                            Etkinlik(
                                id = imlec.getLong(0),
                                baslik = imlec.getString(1) ?: "",
                                baslangicMs = imlec.getLong(2),
                                bitisMs = imlec.getLong(3),
                                tumGun = imlec.getInt(4) == 1,
                                renk = imlec.getInt(5),
                                bizimMi = aciklama.contains(DAMGA_ONEK)
                            )
                        )
                    }
                }
            // Bizim eklediklerimizi ELE: zaten uygulamada gösteriliyorlar,
            // çizelgede iki kez görünmeleri kafa karıştırır.
            liste.filterNot { it.bizimMi }
        }.onFailure { android.util.Log.w(TAG, "gununEtkinlikleri", it) }
            .getOrDefault(emptyList())
    }

    // ══════════════════════════════════════════════════════════
    // Öneri 11 — Boş zaman bulucu
    // ══════════════════════════════════════════════════════════

    /** Boş bir zaman aralığı. */
    data class BosAralik(val baslangicDk: Int, val sureDk: Int) {
        val bitisDk: Int get() = baslangicDk + sureDk

        fun saatMetni(): String = String.format(
            java.util.Locale.US, "%02d:%02d – %02d:%02d",
            baslangicDk / 60, baslangicDk % 60, bitisDk / 60, bitisDk % 60
        )
    }

    /**
     * Bugünün boş zaman aralıklarını bulur.
     *
     * ── Hesap ──
     * Dolu sayılanlar: takvim etkinlikleri + bugün vadeli görevler +
     * namaz vakitleri (±15 dk).
     *
     * ── Neden en az 30 dakika ──
     * 20 dakikalık bir boşlukta anlamlı çalışma olmuyor; kullanıcıyı
     * "15 dakikan var, çalış" diye rahatsız etmenin anlamı yok.
     *
     * @param enAzDk bu süreden kısa boşluklar atlanır
     * @param baslangicSaat günün hangi saatinden itibaren bakılsın
     */
    fun bosAraliklar(
        c: Context,
        enAzDk: Int = 30,
        baslangicSaat: Int = 8,
        bitisSaat: Int = 23
    ): List<BosAralik> = runCatching {
        val dolu = mutableListOf<Pair<Int, Int>>()   // (başlangıç, bitiş) dakika

        // 1) Takvim etkinlikleri
        gununEtkinlikleri(c).forEach { e ->
            if (e.tumGun) return@forEach   // tüm gün etkinliği günü doldurmaz
            dolu.add(e.baslangicDk to (e.baslangicDk + e.sureDk))
        }

        // 2) Bugün vadeli görevler (45 dk varsayılan)
        runCatching {
            val gunBasi = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            Store.aktifGorevler(c)
                .filter { !it.done && it.dueAt in gunBasi until (gunBasi + 86_400_000L) }
                .forEach { g ->
                    val cal = Calendar.getInstance().apply { timeInMillis = g.dueAt }
                    val dk = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                    dolu.add(dk to (dk + 45))
                }
        }

        // 3) Namaz vakitleri (±15 dk)
        runCatching {
            if (NamazVakti.acikMi(c)) {
                val gun = NamazVakti.bugunDuzeltilmis(c)
                NamazVakti.Vakit.entries.forEach { v ->
                    val dk = gun.dakika(v)
                    if (dk >= 0) dolu.add((dk - 5) to (dk + 15))
                }
            }
        }

        if (dolu.isEmpty()) {
            // Hiç doluluk yoksa tüm gün boş
            return@runCatching listOf(
                BosAralik(baslangicSaat * 60, (bitisSaat - baslangicSaat) * 60)
            )
        }

        // Örtüşenleri birleştir
        val sirali = dolu.sortedBy { it.first }.toMutableList()
        val birlesik = mutableListOf<Pair<Int, Int>>()
        var suanki = sirali[0]
        for (i in 1 until sirali.size) {
            val s = sirali[i]
            if (s.first <= suanki.second) {
                suanki = suanki.first to maxOf(suanki.second, s.second)
            } else {
                birlesik.add(suanki)
                suanki = s
            }
        }
        birlesik.add(suanki)

        // Boşlukları çıkar
        val bosluklar = mutableListOf<BosAralik>()
        var imlec = baslangicSaat * 60
        // Şu andan önceki boşluklar anlamsız
        val simdi = Calendar.getInstance()
            .let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
        imlec = maxOf(imlec, simdi)

        birlesik.forEach { (bas, son) ->
            if (bas > imlec) {
                val sure = bas - imlec
                if (sure >= enAzDk) bosluklar.add(BosAralik(imlec, sure))
            }
            imlec = maxOf(imlec, son)
        }
        val gunSonu = bitisSaat * 60
        if (gunSonu > imlec && (gunSonu - imlec) >= enAzDk) {
            bosluklar.add(BosAralik(imlec, gunSonu - imlec))
        }
        bosluklar
    }.onFailure { android.util.Log.w(TAG, "bosAraliklar", it) }.getOrDefault(emptyList())

    /** En uzun boş aralık — "şimdi ne yapmalı" için. */
    fun enUzunBosluk(c: Context): BosAralik? =
        bosAraliklar(c).maxByOrNull { it.sureDk }

    /** Bugün toplam boş dakika. */
    fun toplamBosDk(c: Context): Int = bosAraliklar(c).sumOf { it.sureDk }
}
