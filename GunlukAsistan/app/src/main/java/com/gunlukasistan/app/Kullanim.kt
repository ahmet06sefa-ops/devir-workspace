package com.gunlukasistan.app

import android.content.Context
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * v9.8 — Yerel kullanım analitiği (öneri 50).
 *
 * ══════════════════════════════════════════════════════════════════
 * NEDEN GEREKLİ
 * ══════════════════════════════════════════════════════════════════
 * Uygulamada **218 Kotlin dosyası** ve 40'tan fazla ekran var.
 * Bunların kaçının gerçekten kullanıldığını **bilmiyoruz**.
 *
 * Bu bilgisizliğin somut bedeli var:
 *   · Hiç açılmayan bir ekranı iyileştirmek için gün harcayabiliriz
 *   · Günde 20 kez açılan bir ekrandaki yavaşlık fark edilmez
 *   · "Şu özelliği kaldıralım mı" sorusuna cevap veremeyiz
 *
 * v8.2-v9.7 arasında 50'den fazla özellik ekledim. Hangisi
 * kullanılıyor? Elimde tek veri yok. Bu sürüm onu değiştiriyor.
 *
 * ══════════════════════════════════════════════════════════════════
 * GİZLİLİK — TASARIM İLKESİ
 * ══════════════════════════════════════════════════════════════════
 * Bu **telemetri değil**. Hiçbir veri cihazdan çıkmıyor.
 *
 *   ✓ Yalnızca SharedPreferences'a yazılıyor
 *   ✓ Hiçbir ağ isteğine eklenmiyor
 *   ✓ AI istemlerine girmiyor
 *   ✓ Online senkrona girmiyor
 *   ✓ Kullanıcı tek dokunuşla silebiliyor
 *   ✓ Tek dokunuşla tamamen kapatabiliyor
 *
 * Ne saklanıyor: **ekran adı + sayaç + son açılış zamanı**.
 * Ne saklanMIYOR: not içerikleri, görev metinleri, arama sorguları,
 * ilaç adları, konum, hiçbir kişisel veri.
 *
 * ── Neden "ekran adı" bile hassas olabilir düşündüm ──
 * "NamazActivity 45 kez açıldı" kaydı kullanıcının inancını ele
 * verir. "TakipActivity → ilaç sekmesi" sağlık durumunu ima eder.
 * Bu yüzden veri **asla dışarı çıkmıyor** ve kullanıcı listeyi
 * kendi gözüyle görüp silebiliyor.
 *
 * ══════════════════════════════════════════════════════════════════
 * NEDEN AĞIR DEĞİL
 * ══════════════════════════════════════════════════════════════════
 * `kaydet()` her ekran açılışında çağrılıyor. Bunun ucuz olması şart.
 *
 *   · Bellekte biriktiriliyor, diske **toplu** yazılıyor
 *   · Yazma eşiği: 10 olay veya 60 saniye
 *   · `apply()` kullanılıyor (arka planda yazar, bloklamaz)
 *   · Tek JSON nesnesi — 40 ekran için ~2 KB
 *
 * Ölçtüğüm: ekran açılışına eklediği maliyet mikrosaniye düzeyinde
 * (yalnızca bir `HashMap` güncellemesi).
 */
object Kullanim {

    private const val TAG = "Kullanim"
    private const val PREF = "kullanim_v1"
    private const val K_SAYAC = "sayaclar_json"
    private const val K_ACIK = "acik"
    private const val K_BASLANGIC = "baslangic"
    private const val K_OTURUM = "oturum_sayisi"
    private const val K_SON_OTURUM = "son_oturum"
    private const val K_GUN_LOG = "gun_log_json"

    /** Kaç olay biriktikten sonra diske yazılsın. */
    private const val YAZMA_ESIGI = 10

    /** En fazla bu kadar süre bellekte kalsın (ms). */
    private const val YAZMA_SURESI = 60_000L

    /** Aynı ekranın arka arkaya sayılmasını engelleyen süre (ms). */
    private const val TEKRAR_ESIGI = 1500L

    /** Günlük kayıt kaç gün tutulsun. */
    private const val GUN_TAVANI = 60

    private fun p(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ══════════════════════════════════════════════════════════
    // Bellek arabelleği
    // ══════════════════════════════════════════════════════════

    private val arabellek = HashMap<String, Int>()
    private var bekleyen = 0
    private var sonYazma = 0L
    private var sonEkran = ""
    private var sonEkranZaman = 0L

    // ══════════════════════════════════════════════════════════
    // Açık / kapalı
    // ══════════════════════════════════════════════════════════

    /**
     * Varsayılan **açık**.
     *
     * Bunu uzun düşündüm. Varsayılan kapalı olsaydı hiç kimse
     * açmazdı ve özellik anlamsız olurdu. Açık olması savunulabilir
     * çünkü veri cihazdan çıkmıyor — kullanıcının kendi telefonunda,
     * kendi göreceği bir istatistik. Ayarlarda tek dokunuşla
     * kapatılabiliyor ve kapatınca **geçmiş de siliniyor**.
     */
    fun acikMi(context: Context): Boolean = runCatching {
        p(context).getBoolean(K_ACIK, true)
    }.getOrDefault(true)

    fun ayarla(context: Context, acik: Boolean) {
        runCatching {
            p(context).edit().putBoolean(K_ACIK, acik).apply()
            // Kapatınca geçmişi de sil — "kapattım ama eski kayıtlar
            // duruyor" durumu kullanıcı açısından kandırmaca olurdu.
            if (!acik) temizle(context)
        }
    }

    // ══════════════════════════════════════════════════════════
    // Kayıt
    // ══════════════════════════════════════════════════════════

    /**
     * Bir ekranın açıldığını kaydeder.
     *
     * @param ad ekran adı — sınıf adı yerine **okunabilir etiket**
     *   kullanın ("Görevler", "Sayaç"). Sınıf adı R8 ile
     *   karıştırıldığında anlamsızlaşır ve kullanıcıya
     *   "TakipActivity" göstermek kötü arayüz olurdu.
     */
    fun ekran(context: Context, ad: String) {
        if (ad.isBlank()) return
        if (!acikMi(context)) return

        val simdi = System.currentTimeMillis()
        // Yapılandırma değişikliği (ekran döndürme, tema değişimi)
        // Activity'yi yeniden oluşturuyor. Bu, kullanıcının ekranı
        // ikinci kez açması DEĞİL. 1,5 saniye içindeki tekrarı yut.
        if (ad == sonEkran && simdi - sonEkranZaman < TEKRAR_ESIGI) return
        sonEkran = ad
        sonEkranZaman = simdi

        synchronized(arabellek) {
            arabellek[ad] = (arabellek[ad] ?: 0) + 1
            bekleyen++
        }
        belkiYaz(context, simdi)
    }

    /**
     * Bir eylemin gerçekleştiğini kaydeder.
     *
     * Ekranlardan ayrı tutuluyor: "Notlar ekranı 50 kez açıldı" ile
     * "50 not oluşturuldu" farklı şeyler. Önek ile ayırıyorum.
     */
    fun eylem(context: Context, ad: String) {
        if (ad.isBlank() || !acikMi(context)) return
        synchronized(arabellek) {
            val anahtar = "!$ad"
            arabellek[anahtar] = (arabellek[anahtar] ?: 0) + 1
            bekleyen++
        }
        belkiYaz(context, System.currentTimeMillis())
    }

    private fun belkiYaz(context: Context, simdi: Long) {
        val yazmali = bekleyen >= YAZMA_ESIGI || (simdi - sonYazma) > YAZMA_SURESI
        if (!yazmali) return
        sonYazma = simdi
        // applicationContext: Activity referansı tutup sızdırmayalım
        val uyg = context.applicationContext
        runCatching { Performans.arkaPlan { diskeYaz(uyg) } }
            .onFailure { runCatching { diskeYaz(uyg) } }
    }

    /**
     * Arabelleği diske aktarır.
     *
     * Uygulama kapanırken de çağrılmalı ([bitir]) — yoksa son
     * oturumun verisi kaybolur.
     */
    @Synchronized
    fun diskeYaz(context: Context) {
        val kopya: Map<String, Int>
        synchronized(arabellek) {
            if (arabellek.isEmpty()) return
            kopya = HashMap(arabellek)
            arabellek.clear()
            bekleyen = 0
        }
        runCatching {
            val mevcut = JSONObject(p(context).getString(K_SAYAC, "{}") ?: "{}")
            kopya.forEach { (anahtar, artis) ->
                mevcut.put(anahtar, mevcut.optInt(anahtar, 0) + artis)
            }
            p(context).edit().putString(K_SAYAC, mevcut.toString()).apply()
        }.onFailure {
            android.util.Log.w(TAG, "Sayaç yazılamadı", it)
            // Yazamadıysak veriyi geri koy — kaybolmasın
            synchronized(arabellek) {
                kopya.forEach { (k, v) -> arabellek[k] = (arabellek[k] ?: 0) + v }
            }
        }
    }

    /** Uygulama kapanırken bekleyeni yaz. */
    fun bitir(context: Context) {
        runCatching { diskeYaz(context.applicationContext) }
    }

    // ══════════════════════════════════════════════════════════
    // Oturum
    // ══════════════════════════════════════════════════════════

    /**
     * Uygulama açılışını kaydeder. `App.onCreate`'ten çağrılıyor.
     *
     * Ayrıca **ilk açılış tarihini** kaydediyor — "günde ortalama
     * kaç kez açıyorum" hesabı buna dayanıyor.
     */
    fun oturumBasladi(context: Context) {
        if (!acikMi(context)) return
        runCatching {
            val pref = p(context)
            val e = pref.edit()
            if (pref.getLong(K_BASLANGIC, 0L) == 0L) {
                e.putLong(K_BASLANGIC, System.currentTimeMillis())
            }
            e.putInt(K_OTURUM, pref.getInt(K_OTURUM, 0) + 1)
            e.putLong(K_SON_OTURUM, System.currentTimeMillis())
            e.apply()
            gunKaydet(context)
        }
    }

    /** Bugünü aktif gün olarak işaretler. */
    private fun gunKaydet(context: Context) {
        runCatching {
            val bugun = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
            val log = JSONObject(p(context).getString(K_GUN_LOG, "{}") ?: "{}")
            log.put(bugun, log.optInt(bugun, 0) + 1)

            // Eski günleri at — 60 günden fazlası anlamsız
            if (log.length() > GUN_TAVANI) {
                val anahtarlar = log.keys().asSequence().toList().sorted()
                val silinecek = anahtarlar.take(log.length() - GUN_TAVANI)
                silinecek.forEach { log.remove(it) }
            }
            p(context).edit().putString(K_GUN_LOG, log.toString()).apply()
        }
    }

    fun oturumSayisi(context: Context): Int =
        runCatching { p(context).getInt(K_OTURUM, 0) }.getOrDefault(0)

    fun baslangicTarihi(context: Context): Long =
        runCatching { p(context).getLong(K_BASLANGIC, 0L) }.getOrDefault(0L)

    /** Kaç farklı günde uygulama açıldı. */
    fun aktifGunSayisi(context: Context): Int = runCatching {
        JSONObject(p(context).getString(K_GUN_LOG, "{}") ?: "{}").length()
    }.getOrDefault(0)

    /**
     * Günde ortalama kaç kez açılıyor.
     *
     * Takvim günü değil **aktif gün** üzerinden hesaplıyorum:
     * 30 gün önce kurup 3 gün kullanan biri için "günde 0,4 kez"
     * yanıltıcı. "Kullandığın günlerde ortalama 4 kez" doğru bilgi.
     */
    fun gunlukOrtalama(context: Context): Double {
        val gun = aktifGunSayisi(context)
        if (gun <= 0) return 0.0
        return oturumSayisi(context).toDouble() / gun
    }

    // ══════════════════════════════════════════════════════════
    // Okuma
    // ══════════════════════════════════════════════════════════

    data class Satir(val ad: String, val sayi: Int, val eylemMi: Boolean) {
        /** Görüntülenecek temiz ad (eylem öneki çıkarılmış). */
        val gosterim: String get() = if (eylemMi) ad.removePrefix("!") else ad
    }

    /**
     * Tüm sayaçlar — çoktan aza sıralı.
     *
     * Bellek arabelleğini de dahil ediyor: kullanıcı istatistik
     * ekranını açtığında henüz diske yazılmamış olaylar da
     * görünsün. Aksi halde "az önce açtığım ekran listede yok"
     * durumu olurdu.
     */
    fun hepsi(context: Context): List<Satir> = runCatching {
        val nesne = JSONObject(p(context).getString(K_SAYAC, "{}") ?: "{}")
        val harita = mutableMapOf<String, Int>()
        nesne.keys().forEach { k -> harita[k] = nesne.optInt(k, 0) }
        synchronized(arabellek) {
            arabellek.forEach { (k, v) -> harita[k] = (harita[k] ?: 0) + v }
        }
        harita.map { (k, v) -> Satir(k, v, k.startsWith("!")) }
            .sortedByDescending { it.sayi }
    }.getOrElse {
        android.util.Log.w(TAG, "Sayaçlar okunamadı", it)
        emptyList()
    }

    fun ekranlar(context: Context): List<Satir> = hepsi(context).filter { !it.eylemMi }

    fun eylemler(context: Context): List<Satir> = hepsi(context).filter { it.eylemMi }

    /** Toplam kayıtlı olay sayısı. */
    fun toplamOlay(context: Context): Int = hepsi(context).sumOf { it.sayi }

    // ══════════════════════════════════════════════════════════
    // Çıkarımlar
    // ══════════════════════════════════════════════════════════

    /**
     * Veriden okunabilir gözlemler.
     *
     * Ham sayı listesi kullanıcıya bir şey söylemiyor. "En çok
     * Görevler ekranını açıyorsun (%34)" anlamlı bir cümle.
     */
    fun cikarimlar(context: Context): List<String> {
        val sonuc = mutableListOf<String>()
        val ekranlar = ekranlar(context)
        if (ekranlar.isEmpty()) return sonuc

        val toplam = ekranlar.sumOf { it.sayi }
        if (toplam < 10) {
            sonuc.add(context.getString(R.string.ku_c_az_veri))
            return sonuc
        }

        // 1. En çok kullanılan
        ekranlar.firstOrNull()?.let { ilk ->
            val yuzde = ((ilk.sayi.toDouble() / toplam) * 100).toInt()
            sonuc.add(context.getString(R.string.ku_c_en_cok, ilk.gosterim, yuzde))
        }

        // 2. Kullanım yoğunluğu
        val ortalama = gunlukOrtalama(context)
        if (ortalama >= 1) {
            sonuc.add(
                context.getString(
                    R.string.ku_c_gunluk,
                    String.format(Locale.US, "%.1f", ortalama),
                    aktifGunSayisi(context)
                )
            )
        }

        // 3. İlk üç ekranın payı — dağınık mı odaklı mı kullanıyor
        if (ekranlar.size >= 5) {
            val ilkUc = ekranlar.take(3).sumOf { it.sayi }
            val pay = ((ilkUc.toDouble() / toplam) * 100).toInt()
            sonuc.add(
                if (pay >= 70) context.getString(R.string.ku_c_odakli, pay)
                else context.getString(R.string.ku_c_dagitik, ekranlar.size)
            )
        }

        // 4. En sık eylem
        eylemler(context).firstOrNull()?.let { e ->
            sonuc.add(context.getString(R.string.ku_c_eylem, e.gosterim, e.sayi))
        }

        return sonuc
    }

    /**
     * Hiç kullanılmayan ekranlar.
     *
     * Bu liste **geliştirici için** değerli: "şu ekranı kimse
     * açmıyor" bilgisi. Kullanıcıya da gösteriyorum çünkü
     * "bunları hiç denemedin, bir bak" önerisi işe yarayabilir.
     *
     * @param bilinenler uygulamadaki tüm ekran adları
     */
    fun hicKullanilmayanlar(context: Context, bilinenler: List<String>): List<String> {
        val kullanilanlar = ekranlar(context).map { it.ad }.toSet()
        return bilinenler.filter { it !in kullanilanlar }
    }

    // ══════════════════════════════════════════════════════════
    // Temizleme
    // ══════════════════════════════════════════════════════════

    fun temizle(context: Context) {
        runCatching {
            synchronized(arabellek) {
                arabellek.clear()
                bekleyen = 0
            }
            p(context).edit()
                .remove(K_SAYAC).remove(K_OTURUM)
                .remove(K_BASLANGIC).remove(K_GUN_LOG)
                .remove(K_SON_OTURUM)
                .apply()
        }
    }

    /**
     * Panoya kopyalanabilir özet.
     *
     * Kullanıcı isterse bunu bana gönderebilir — ama **kendi
     * iradesiyle**. Otomatik gönderim yok.
     */
    fun metinOzet(context: Context): String = buildString {
        appendLine("=== Günlük Asistan · Kullanım Özeti ===")
        appendLine("Sürüm: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("Oturum: ${oturumSayisi(context)} · Aktif gün: ${aktifGunSayisi(context)}")
        appendLine("Günlük ortalama: ${String.format(Locale.US, "%.1f", gunlukOrtalama(context))}")
        appendLine()
        appendLine("--- Ekranlar ---")
        ekranlar(context).take(25).forEach { appendLine("${it.gosterim}: ${it.sayi}") }
        val ey = eylemler(context)
        if (ey.isNotEmpty()) {
            appendLine()
            appendLine("--- Eylemler ---")
            ey.take(25).forEach { appendLine("${it.gosterim}: ${it.sayi}") }
        }
    }

    // ══════════════════════════════════════════════════════════
    // Ekran adı sabitleri
    // ══════════════════════════════════════════════════════════

    /**
     * Ekran adları tek yerde.
     *
     * Neden sabit: her Activity'de elle dize yazmak yazım hatasına
     * açık ("Görevler" vs "Gorevler" iki ayrı kayıt olurdu) ve
     * yeniden adlandırma imkânsızlaşırdı.
     */
    object Ekran {
        const val ANA = "Ana ekran"
        const val GOREVLER = "Görevler"
        const val NOTLAR = "Notlar"
        const val KONULAR = "Konular"
        const val SAYAC = "Sayaç"
        const val ILERLEME = "İlerleme"
        const val BUGUN = "Bugün"
        const val AYARLAR = "Ayarlar"
        const val ASISTAN = "Asistan"
        const val KURSLAR = "Kurslar"
        const val ARACLAR = "Araçlar"
        const val PLAN = "Plan"
        const val KAYNAKLAR = "Kaynaklar"
        const val OGRENME = "Öğrenme merkezi"
        const val GUNLUK_HAYAT = "Günlük hayat"
        const val TAKVIM = "Takvim ayarları"
        const val SORU_COZ = "Soru çözme"
        const val TEKRAR = "Tekrar"
        const val ANALITIK = "Analitik"
        const val DEPOLAMA = "Depolama"
        const val ONLINE = "Online"
        const val NAMAZ = "Namaz"
        const val KOC = "Koç"
        const val QUIZ = "Quiz"
        const val KART = "Bilgi kartları"
        const val OGRETMEN = "Özel öğretmen"
        const val FILM = "Film/dizi"
        const val GORUNUM = "Görünüm ayarları"
        const val BILDIRIM = "Bildirim ayarları"
        const val ISTATISTIK = "Kullanım istatistiği"

        /** Bilinen tüm ekranlar — "hiç kullanılmayan" hesabı için. */
        val HEPSI = listOf(
            ANA, GOREVLER, NOTLAR, KONULAR, SAYAC, ILERLEME, BUGUN,
            AYARLAR, ASISTAN, KURSLAR, ARACLAR, PLAN, KAYNAKLAR,
            OGRENME, GUNLUK_HAYAT, TAKVIM, SORU_COZ, TEKRAR, ANALITIK,
            DEPOLAMA, ONLINE, NAMAZ, KOC, QUIZ, KART, OGRETMEN, FILM,
            GORUNUM, BILDIRIM, ISTATISTIK
        )
    }

    /** Eylem adları. */
    object Eylem {
        const val GOREV_EKLE = "Görev eklendi"
        const val GOREV_TAMAM = "Görev tamamlandı"
        const val NOT_EKLE = "Not eklendi"
        const val KONU_EKLE = "Konu eklendi"
        const val SAYAC_BASLA = "Sayaç başlatıldı"
        const val QUIZ_COZ = "Quiz çözüldü"
        const val KART_TEKRAR = "Kart tekrarlandı"
        const val YEDEK_AL = "Yedek alındı"
        const val AI_ISTEK = "AI isteği"
        const val HARCAMA_EKLE = "Harcama eklendi"
        const val ILAC_ALINDI = "İlaç alındı"
        const val HIZLI_KOMUT = "Hızlı komut"
    }
}
