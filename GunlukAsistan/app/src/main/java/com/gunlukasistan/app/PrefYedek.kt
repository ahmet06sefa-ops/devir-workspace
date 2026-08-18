package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * v7.98 — Genel SharedPreferences yedekleyici.
 *
 * ── Sorun ──
 * v7.78'den beri eklenen 11 modülün verisi yedeğe **hiç girmiyordu**:
 * Mufredat (program ilerlemesi), Hatalarim (hata defteri), Pomodoro,
 * OdakKaydi (çalışma oturumları), Koc (karne/seri/borç), Kanit,
 * KonuUretici (üretilmiş tüm AI anlatımları), SayacAyar, OkumaAyar...
 *
 * Kullanıcı telefon değiştirdiğinde bunların hepsi kayboluyordu; üstelik
 * yedek "her şeyi aldım" izlenimi verdiği için fark edilmesi zordu.
 *
 * ── Neden tek tek `disaAktar` yazılmadı ──
 * 11 modüle ayrı ayrı `disaAktar`/`iceAktar` eklemek ~400 satır tekrar
 * eden kod demekti. Daha kötüsü: 12. modül eklendiğinde yine unutulurdu.
 *
 * Bu sınıf **dosya adına göre** tüm SharedPreferences'ı geziyor. Yeni bir
 * modül eklendiğinde hiçbir şey yazmaya gerek yok — deposu otomatik
 * yedekleniyor.
 *
 * ── Neden beyaz liste değil kara liste ──
 * "Şunları yedekle" demek yeni modülü yine unutmak demek. Bunun yerine
 * "şunları yedekleme" ([HARIC]) diyoruz: hassas veri (API anahtarları)
 * ve anlamsız veri (önbellek, çökme kaydı) dışarıda kalıyor, kalan her şey
 * içeri giriyor.
 *
 * ── v10.25 🔴 DÜZELTME — söz ile kod ayrışması giderildi ──
 * Yukarıdaki "otomatik yedekleniyor" sözü v7.98'den beri **doğru değildi**:
 * tarama yerine sabit [DEPOLAR] listesi kullanılıyordu. Ölçüm: uygulamada
 * ~90 tercih deposu var, yedeğe giren 19'uydu. Ekran Atölyesi düzeni,
 * Widget Atölyesi ayarları, PIN kilidi (kilit_v1), bildirim saatleri,
 * sessiz türler, mikro günlük... yedeğe hiç girmiyordu.
 *
 * Artık gerçek tarama var: `shared_prefs` klasöründeki her dosya
 * [yedegeGirerMi] süzgecinden geçip yedeğe giriyor; yeni eklenen modül
 * için kod yazmak GEREKMİYOR. Hassas/oturum/türetilmiş depolar ise
 * genişletilmiş [HARIC] ile bilinçli olarak dışarıda (gerekçeleri yerinde).
 * [DEPOLAR] yalnızca klasör okunamayan aykırı cihazlarda yedek liste
 * olarak duruyor.
 */
object PrefYedek {

    private const val TAG = "PrefYedek"

    /**
     * Ana Store deposu — zaten `exportJson` tarafından ayrıca işleniyor.
     * Not: HARIC'ten ÖNCE bildirilmeli (ileri referans derleyici hatası).
     * 🔴 v10.25 düzeltme: v7.98'de "gunluk_asistan_prefs" yazıyordu;
     * Store'un gerçek dosyası bu adda.
     */
    private const val ANA_DEPO = "gunluk_asistan_store"

    /**
     * Yedeğe **girmeyecek** depolar.
     *
     * · `ai_settings` — API anahtarları. Yedek paylaşılabiliyor;
     *   anahtarların sızması hesabın çalınması demek. v7.34'ten beri
     *   bilinçli olarak dışarıda.
     * · `ai_model_cache`, `veri_gecis_v1` — türetilmiş/geçici veri.
     * · `crash_log` — sonraki kurulumda anlamsız.
     * · `konu_anlatim_v1` — AI anlatımları çok yer kaplıyor (yüzlerce KB);
     *   [anlatimlariDahilEt] ile isteğe bağlı eklenir.
     */
    private val HARIC = setOf(
        // ── Hassas veri ──
        // Yedek paylaşılabiliyor; API anahtarlarının sızması hesabın
        // çalınması demek. v7.34'ten beri bilinçli olarak dışarıda.
        "ai_settings",

        // ── Türetilmiş / önbellek (yeniden üretilir) ──
        "ai_model_cache", "ai_onbellek_v1", "crash_log", "veri_gecis_v1",
        "guncelleme_v1", "arkaplan_is_v1",
        "online_bekci_v1", // son çalışma zamanı
        // v9.8: kullanım istatistiği cihaza özgü ve alışkanlık
        // bilgisi taşıyor — paylaşılan yedekte yeri yok.
        "kullanim_v1",

        // ── v10.25: tanı / bildirim defteri (cihaza özgü kayıt) ──
        "alarm_sagligi_v1", "bildirim_tani", "bildirim_test_v1",
        "rekor_bildirim_v1", "rozet_bildirim_v1",

        // ── v10.25: OTURUM durumu — cihazlar arası tehlikeli ──
        // Çalışan sayaç/ileri sayım eski zaman damgasıyla geri
        // yüklenirse saatlerce sapık süre gösterir; zincir ve ay-ofset
        // görünüm durumu, kritik alarm da yarım kalmış adım sayar.
        "timer_engine_v1", "ileri_sayim_v1", "sayac_zincir_v1",
        "wg_ay_ofset_v1", "kritik_alarm_v1",

        // ── v10.25: kendi dışa/içe aktarım kanalı OLAN depolar ──
        // exportJson bunları ayrıca yazıyor; buradan ikinci kez
        // yazılması hem şişkinlik hem çift-yazım çakışma riski.
        // (🔴 ek düzeltme: ANA_DEPO v7.98'de "gunluk_asistan_prefs"
        // yazıyordu; Store'un gerçek dosyası AŞAĞIDAKİ sabitteki ad.)
        ANA_DEPO, "gunluk_asistan_prefs",
        "quiz_store", "kart_store", "ogretmen_store",
        "namaz_v1", "namaz_plan_v1", "zorunlu_uyari_v1",
        "ai_sohbet_v1", "film_v1", "online_v1",

        // ── Yedek altyapısının kendi defteri ──
        "yedek_rotasyon_v1"
    )

    /** v10.25: adı bu öneklerle başlayan sistem çöpleri asla yedeğe girmez. */
    private val ATILAN_ONEKLER = listOf(
        "androidx.", "com.google.", "WebViewChromiumPrefs", "com.android."
    )

    /**
     * v10.25: bu depo yedeğe girer mi? (saf karar — birim testli)
     *
     * [HARIC] dışındaki her şey içeri girer; böylece YENİ eklenen
     * modüller hiçbir kod değişikliği olmadan yedeklenir. Büyük
     * depo (`konu_anlatim_v1`) kapıdan geçer ama varsayılan taramaya
     * dahil edilmez — ancak istenirse [disaAktar] ekler.
     */
    fun yedegeGirerMi(depoAdi: String): Boolean {
        if (depoAdi.isBlank()) return false
        if (depoAdi in HARIC) return false
        if (ATILAN_ONEKLER.any { depoAdi.startsWith(it) }) return false
        return true
    }

    /** Varsayılan olarak hariç ama istenirse eklenen büyük depolar. */
    private val BUYUK = setOf("konu_anlatim_v1")

    /**
     * Yedek liste (fallback).
     *
     * v10.25'ten beri birincil kaynak [depoAdlari]: `shared_prefs`
     * klasörü taranıyor. Bu liste yalnızca klasör okunamayan aykırı
     * ROM'larda ve henüz dosyası oluşmamış depolarda devreye girer;
     * burada olup HARIC'te olmayan her ad [yedegeGirerMi]'den
     * koşulsuz geçer.
     */
    private val DEPOLAR = listOf(
        "mufredat_v1",      // program: seçili kurs/konu, aktif adım, süreler
        "hatalarim_v1",     // hata defteri + Leitner kutuları
        "sozluk_v1",        // terim sözlüğü
        "pomodoro_v1",      // döngü ayarları + odak modu
        "odak_kaydi_v1",    // sayaç oturum geçmişi
        "hafta_plan_v1",    // haftalık hedefler
        "sayfa_imi_v1",     // PDF yer imleri
        "koc_v1",           // karne, seri, borç, mazeretler
        "kanit_v1",         // kanıt kayıtları (fotoğraflar hariç)
        "sayac_ayar_v1",    // zamanlayıcı ayarları
        "okuma_ayar_v1",    // anlatım okuma tercihleri
        "yedek_sifre_v1",   // şifreleme tercihi
        "widget_tema_v1",   // widget görünümü
        "bildirim_ayar_v1", // bildirim tür anahtarları
        // v8.2: animasyon/haptic/kaydırma tercihleri. Cihaz
        // değiştirince kullanıcının bunları yeniden ayarlamasına
        // gerek kalmasın.
        "gunluk_asistan_gorunum",
        // v8.3: konu renk/simge eşlemesi (öneri 13)
        "konu_gorunum_v1",
        // ── v9.7 · Grup F: günlük hayat ──
        // Bu üç depo kullanıcının EN ZOR yeniden gireceği veriyi
        // tutuyor: aylarca biriken harcama kayıtları, ilaç saatleri,
        // belge tarihleri. Yedeğe girmemesi telefon değişiminde
        // hepsinin kaybolması demekti.
        "takip_v1",              // ilaç, fatura, belge, araç + ödeme geçmişi
        "butce_v1",              // gelir/gider kalemleri + aylık limit
        "konum_hatirlatma_v1"    // kayıtlı yerler ve konum hatırlatmaları

        // ── v9.8 · Grup G — bilinçli olarak DAHİL EDİLMEDİ ──
        //
        // "kullanim_v1"   → cihaza özgü istatistik. Yeni telefonda
        //                   eski cihazın ekran sayaçları anlamsız;
        //                   ayrıca yedek paylaşılabiliyor ve bu veri
        //                   kullanıcının alışkanlıklarını ele verir.
        // "guncelleme_v1" → türetilmiş önbellek, yeniden oluşur.
        // "arkaplan_is_v1"→ yalnızca son çalışma zamanı.
        // "crash_log"     → zaten HARIC listesinde (v8.8'den beri).
    )

    // ═══════════════════════════════════════════════════════════════
    // DEPO TARAMA (v10.25)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Cihazdaki SharedPreferences dosyalarından yedeğe girecek adlar.
     *
     * Kaynak 1: `shared_prefs/` klasöründeki gerçek dosyalar (tarama).
     * Kaynak 2: [DEPOLAR] — klasör okunamazsa veya dosya henüz
     *           oluşmamışsa devreye giren bilinen liste.
     *
     * Her iki kaynağın birleşimi [yedegeGirerMi] süzgecinden geçer;
     * [BUYUK] depolar varsayılan taramaya girmez (yalnız istenince).
     */
    fun depoAdlari(context: Context): List<String> {
        val bulunan = runCatching {
            val kok = java.io.File(context.applicationInfo.dataDir, "shared_prefs")
            kok.listFiles()
                ?.asSequence()
                ?.filter { it.isFile && it.name.endsWith(".xml") }
                ?.map { it.name.removeSuffix(".xml") }
                ?.toList()
        }.getOrNull().orEmpty()

        return (bulunan + DEPOLAR)
            .distinct()
            .filter { yedegeGirerMi(it) && it !in BUYUK }
            .sorted()
    }

    // ═══════════════════════════════════════════════════════════════
    // DIŞA AKTAR
    // ═══════════════════════════════════════════════════════════════

    /**
     * Tüm modül depolarını tek JSON nesnesine toplar.
     *
     * @param anlatimlariDahilEt AI anlatımları da eklensin mi (büyük)
     */
    fun disaAktar(context: Context, anlatimlariDahilEt: Boolean = false): JSONObject {
        val kok = JSONObject()
        val hedefler = if (anlatimlariDahilEt) depoAdlari(context) + BUYUK else depoAdlari(context)

        hedefler.forEach { depoAdi ->
            if (!yedegeGirerMi(depoAdi)) return@forEach
            runCatching { kok.put(depoAdi, depoyuOku(context, depoAdi)) }
                .onFailure { android.util.Log.w(TAG, "Depo okunamadı: $depoAdi", it) }
        }
        return kok
    }

    /**
     * Bir SharedPreferences dosyasını JSON'a çevirir.
     *
     * Tür bilgisi korunmalı: `getInt` ile yazılan bir değeri `String`
     * olarak geri yüklemek `ClassCastException` fırlatır. Bu yüzden her
     * değer `{"t": tür, "v": değer}` biçiminde saklanıyor.
     */
    private fun depoyuOku(context: Context, depoAdi: String): JSONObject {
        val p = context.getSharedPreferences(depoAdi, Context.MODE_PRIVATE)
        val cikti = JSONObject()

        p.all.forEach { (anahtar, deger) ->
            runCatching {
                val kayit = JSONObject()
                when (deger) {
                    is Boolean -> kayit.put("t", "b").put("v", deger)
                    is Int -> kayit.put("t", "i").put("v", deger)
                    is Long -> kayit.put("t", "l").put("v", deger)
                    is Float -> kayit.put("t", "f").put("v", deger.toDouble())
                    is String -> kayit.put("t", "s").put("v", deger)
                    is Set<*> -> kayit.put("t", "ss")
                        .put("v", JSONArray(deger.filterIsInstance<String>()))
                    else -> return@runCatching
                }
                cikti.put(anahtar, kayit)
            }
        }
        return cikti
    }

    // ═══════════════════════════════════════════════════════════════
    // İÇE AKTAR
    // ═══════════════════════════════════════════════════════════════

    /**
     * Yedekten modül depolarını geri yükler.
     *
     * @return geri yüklenen depo sayısı
     */
    fun iceAktar(context: Context, kok: JSONObject): Int {
        var sayac = 0
        val adlar = kok.keys()

        while (adlar.hasNext()) {
            val depoAdi = adlar.next()
            // v10.25: tarama ile aynı kapı — iki yönde tek karar.
            // Yedeğe hiç girmemesi gereken bir depo geri de yazılmaz.
            if (!yedegeGirerMi(depoAdi)) continue

            runCatching {
                val icerik = kok.optJSONObject(depoAdi) ?: return@runCatching
                depoyuYaz(context, depoAdi, icerik)
                sayac++
            }.onFailure {
                android.util.Log.w(TAG, "Depo yazılamadı: $depoAdi", it)
            }
        }
        return sayac
    }

    /**
     * JSON'u SharedPreferences'a yazar.
     *
     * Depo **temizlenmiyor**: yedekte olmayan yeni ayarlar (uygulamanın
     * sonraki sürümünde eklenmiş olabilir) silinmesin. Yedekteki anahtarlar
     * mevcut değerlerin üzerine yazılıyor.
     */
    private fun depoyuYaz(context: Context, depoAdi: String, icerik: JSONObject) {
        val e = context.getSharedPreferences(depoAdi, Context.MODE_PRIVATE).edit()
        val anahtarlar = icerik.keys()

        while (anahtarlar.hasNext()) {
            val anahtar = anahtarlar.next()
            runCatching {
                val kayit = icerik.optJSONObject(anahtar) ?: return@runCatching
                when (kayit.optString("t")) {
                    "b" -> e.putBoolean(anahtar, kayit.optBoolean("v"))
                    "i" -> e.putInt(anahtar, kayit.optInt("v"))
                    "l" -> e.putLong(anahtar, kayit.optLong("v"))
                    "f" -> e.putFloat(anahtar, kayit.optDouble("v").toFloat())
                    "s" -> e.putString(anahtar, kayit.optString("v"))
                    "ss" -> {
                        val dizi = kayit.optJSONArray("v") ?: JSONArray()
                        val kume = mutableSetOf<String>()
                        for (i in 0 until dizi.length()) kume.add(dizi.optString(i))
                        e.putStringSet(anahtar, kume)
                    }
                }
            }
        }
        e.apply()
    }

    // ═══════════════════════════════════════════════════════════════
    // BİLGİ
    // ═══════════════════════════════════════════════════════════════

    /** Yedeğe girecek verinin kabaca boyutu (bayt). */
    fun tahminiBoyut(context: Context, anlatimlariDahilEt: Boolean = false): Int =
        runCatching { disaAktar(context, anlatimlariDahilEt).toString().length }
            .getOrDefault(0)

    /** Kaç modülde veri var — kullanıcıya "neler yedekleniyor" demek için. */
    fun doluDepoSayisi(context: Context): Int = depoAdlari(context).count { depoAdi ->
        runCatching {
            context.getSharedPreferences(depoAdi, Context.MODE_PRIVATE).all.isNotEmpty()
        }.getOrDefault(false)
    }

    /** Anlatım önbelleğinin boyutu — kullanıcı dahil etmeye karar versin. */
    fun anlatimBoyutu(context: Context): Int = runCatching {
        context.getSharedPreferences("konu_anlatim_v1", Context.MODE_PRIVATE)
            .all.values.sumOf { (it as? String)?.length ?: 0 }
    }.getOrDefault(0)

    fun boyutMetni(bayt: Int): String = when {
        bayt < 1024 -> "$bayt B"
        bayt < 1024 * 1024 -> "${bayt / 1024} KB"
        else -> String.format(java.util.Locale.US, "%.1f MB", bayt / 1048576.0)
    }
}
