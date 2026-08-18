package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * v10.9 — Gün çerçevesi: uyku düzeni ve gün kapıları.
 *
 * ── Kullanıcının isteği ──
 * "Sabah belirlediğim saatte SESSİZ bir 'uyandın mı?' bildirimi
 * alayım; onay verince gün başlasın (görevler, plan). Akşam
 * belirlediğim saatte 'uyuyacak mısın?' + günümün özeti gelsin.
 * Her şeyini değiştirebileyim."
 *
 * ── Mevcut sistemle ilişki ──
 * [BildirimZamanlayici]'nin sabah 09:00 rutini (özet teslimi, gün
 * odağı, kart) bu çerçeve AÇIKKEN tamamen buraya devredilir:
 * ya "uyandım" onayıyla ya da `sonCare` açıksa son tekrarın
 * ardından çalışır. Çerçeve kapalıysa eski davranış aynen sürer.
 *
 * ── Veri modeli ──
 * İki tür kayıt tutulur: gün içi koşu durumu (tekrar sayaçları,
 * "sabah teslim edildi" işareti) ve 14 günlük uyku defteri
 * (uyanma/uyuma saatleri + gerçekleşen uyku süresi). Defter
 * [UykuAyarActivity]'deki istatistik kartının verisidir.
 *
 * ── Saf bölge ──
 * Saat/ortlama/süre/özet-seçim/JSON hesapları Context'siz ve birim
 * testli; prefs katmanı bu fonksiyonların üstüne oturur.
 */
object UykuCerceve {

    private const val PREF = "uyku_cerceve_v1"

    // Kanal kimlikleri — sessiz/sesli ayrımı kanal düzeyinde yapılır
    // (Android'de kanal önemi sonradan değiştirilemediği için iki kanal).
    const val KANAL_SABAH_SESSIZ = "uyku_sabah_sessiz_v1"
    const val KANAL_SABAH_SESLI = "uyku_sabah_sesli_v1"
    const val KANAL_AKSAM = "uyku_aksam_v1"

    // Bildirim kimlikleri — proje geneli çakışma denetiminden geçti.
    const val BILDIRIM_SABAH = 4801
    const val BILDIRIM_AKSAM = 4802
    const val BILDIRIM_IYIGECELER = 4803

    /** Uyku süresi üst sınırı — üstü ölçüm hatası sayılır. */
    const val MAKS_UYKU_SAAT = 20

    /** Defterde tutulan en eski gün sayısı. */
    const val DEFTER_GUN = 14

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ═══════════════════════════════════════════════════════════════
    // AYARLAR
    // ═══════════════════════════════════════════════════════════════

    /**
     * Ana anahtar. Varsayılan AÇIK — özellik kullanıcının doğrudan
     * isteği; onayı olmayan kurulumda 09:00 rutini buraya taşınır
     * (zamane: 09:00→07:00 anahtar görünürlüğü ayarlardandır).
     */
    fun acik(context: Context): Boolean = prefs(context).getBoolean("acik", true)

    fun setAcik(context: Context, deger: Boolean) {
        prefs(context).edit().putBoolean("acik", deger).apply()
    }

    /** Sabah kapısı — gün içi dakika (07:00 = 420). */
    fun sabahDk(context: Context): Int = prefs(context).getInt("sabah_dk", 420)

    fun setSabahDk(context: Context, dk: Int) {
        prefs(context).edit().putInt("sabah_dk", dk.coerceIn(0, 1439)).apply()
    }

    /** Akşam kapısı — gün içi dakika (23:00 = 1380). */
    fun aksamDk(context: Context): Int = prefs(context).getInt("aksam_dk", 1380)

    fun setAksamDk(context: Context, dk: Int) {
        prefs(context).edit().putInt("aksam_dk", dk.coerceIn(0, 1439)).apply()
    }

    /**
     * Sabah sorusu sessiz mi gelsin?
     *
     * Varsayılan SESSİZ (kullanıcının isteği): IMPORTANCE_LOW kanal,
     * ses/titreşim yok. Kapatılırsa yüksek önemli kanala geçilir.
     */
    fun sabahSessiz(context: Context): Boolean =
        prefs(context).getBoolean("sabah_sessiz", true)

    fun setSabahSessiz(context: Context, deger: Boolean) {
        prefs(context).edit().putBoolean("sabah_sessiz", deger).apply()
    }

    /**
     * Sabah rutini yalnızca "uyandım" onayıyla mı başlasın?
     * Kapalıysa soru sorulmaz, saat gelince rutin doğrudan çalışır
     * (eski sistemin saatini değiştirir gibi düşün).
     */
    fun onaySart(context: Context): Boolean = prefs(context).getBoolean("onay_sart", true)

    fun setOnaySart(context: Context, deger: Boolean) {
        prefs(context).edit().putBoolean("onay_sart", deger).apply()
    }

    /** Cevap gelmezse kaç dakikada bir yeniden sorsun (sabah). */
    fun tekrarDkSabah(context: Context): Int =
        prefs(context).getInt("tekrar_sabah", 15).coerceIn(5, 60)

    fun setTekrarDkSabah(context: Context, dk: Int) {
        prefs(context).edit().putInt("tekrar_sabah", dk.coerceIn(5, 60)).apply()
    }

    fun maksTekrarSabah(context: Context): Int =
        prefs(context).getInt("maks_sabah", 3).coerceIn(0, 6)

    fun setMaksTekrarSabah(context: Context, n: Int) {
        prefs(context).edit().putInt("maks_sabah", n.coerceIn(0, 6)).apply()
    }

    /**
     * Son çare: hiç cevap gelmezse gün yine de başlasın mı?
     * Kapalıysa o gün sabah rutini hiç çalışmaz (09:00 eski turu da
     * engellenir — "cevapsızsa sessiz kal" tercihinin bedeli budur).
     */
    fun sonCare(context: Context): Boolean = prefs(context).getBoolean("son_care", true)

    fun setSonCare(context: Context, deger: Boolean) {
        prefs(context).edit().putBoolean("son_care", deger).apply()
    }

    fun tekrarDkAksam(context: Context): Int =
        prefs(context).getInt("tekrar_aksam", 30).coerceIn(5, 90)

    fun setTekrarDkAksam(context: Context, dk: Int) {
        prefs(context).edit().putInt("tekrar_aksam", dk.coerceIn(5, 90)).apply()
    }

    fun maksTekrarAksam(context: Context): Int =
        prefs(context).getInt("maks_aksam", 2).coerceIn(0, 6)

    fun setMaksTekrarAksam(context: Context, n: Int) {
        prefs(context).edit().putInt("maks_aksam", n.coerceIn(0, 6)).apply()
    }

    /**
     * Akşam özeti, onayı beklemeden de gösterilsin mi?
     * (Varsayılan ATLARZ: özet her zaman gelir; sorular ondaki
     * eylem düğmeleridir.)
     */
    fun ozetOdak(context: Context): Boolean = prefs(context).getBoolean("ozet_odak", true)
    fun setOzetOdak(context: Context, v: Boolean) { prefs(context).edit().putBoolean("ozet_odak", v).apply() }
    fun ozetGorev(context: Context): Boolean = prefs(context).getBoolean("ozet_gorev", true)
    fun setOzetGorev(context: Context, v: Boolean) { prefs(context).edit().putBoolean("ozet_gorev", v).apply() }
    fun ozetSeri(context: Context): Boolean = prefs(context).getBoolean("ozet_seri", true)
    fun setOzetSeri(context: Context, v: Boolean) { prefs(context).edit().putBoolean("ozet_seri", v).apply() }
    fun ozetZincir(context: Context): Boolean = prefs(context).getBoolean("ozet_zincir", true)
    fun setOzetZincir(context: Context, v: Boolean) { prefs(context).edit().putBoolean("ozet_zincir", v).apply() }

    // ═══════════════════════════════════════════════════════════════
    // KOŞU DURUMU (gün içi, kalıcı ama kısa ömürlü)
    // ═══════════════════════════════════════════════════════════════

    /** Sabah rutini bu gün teslim edildi mi — gün anahtarıyla işaret. */
    fun sabahVerildiMi(context: Context, simdiMs: Long): Boolean =
        prefs(context).getString("sabah_gun", "") == gunKey(simdiMs)

    fun sabahVerildi(context: Context, simdiMs: Long) {
        prefs(context).edit().putString("sabah_gun", gunKey(simdiMs)).apply()
    }

    fun sabahTekrar(context: Context): Int = prefs(context).getInt("st_sayac", 0)
    fun setSabahTekrar(context: Context, n: Int) { prefs(context).edit().putInt("st_sayac", n.coerceAtLeast(0)).apply() }
    fun aksamTekrar(context: Context): Int = prefs(context).getInt("at_sayac", 0)
    fun setAksamTekrar(context: Context, n: Int) { prefs(context).edit().putInt("at_sayac", n.coerceAtLeast(0)).apply() }

    /**
     * Akşam döngüsünün "günü": sabah 06:00'ya kadar hâlâ ÖNCEKİ
     * akşamın döngüsündeyiz. Bu kaydırma olmazsa 00:15'te "Uyuyorum"
     * diyen kullanıcı o akşam 23:00 özetini de tüketmiş sayılırdı —
     * bütün gün uyanık geçen günün özeti bir daha hiç gelmezdi.
     *
     * Bilinen sınır: 6 saat × 90 dk'lık azami tekrar zinciri 06:00'ı
     * aşarsa sınır çizgisinde bir ek gönderi olabilir; zararsızdır.
     */
    fun aksamGunAnahtari(simdiMs: Long): String = gunKey(simdiMs - 6 * 3600_000L)

    /** Akşam sorusu bu akşam döngüsünde cevaplandı/tükendi mi. */
    fun aksamVerildiMi(context: Context, simdiMs: Long): Boolean =
        prefs(context).getString("aksam_gun", "") == aksamGunAnahtari(simdiMs)

    /** Döngü gününe işaretler (gece 06:00 öncesi önceki akşama yazılır). */
    fun aksamVerildi(context: Context, simdiMs: Long) {
        prefs(context).edit().putString("aksam_gun", aksamGunAnahtari(simdiMs)).apply()
    }

    // ═══════════════════════════════════════════════════════════════
    // UYKU DEFTERİ (14 gün)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Bir gecenin kaydı. [uykuMs] ertesi sabah uyanınca hesaplanır;
     * hesaplanamadıysa 0 durur (çift yok — uykusu bilinmeyen gün).
     */
    data class Gun(
        val gunKey: String,
        val uyandiMs: Long,
        val uyuduMs: Long,
        val uykuMs: Long
    ) {
        fun json(): JSONObject = JSONObject()
            .put("g", gunKey).put("ua", uyandiMs).put("uu", uyuduMs).put("uk", uykuMs)

        companion object {
            fun jsondan(o: JSONObject): Gun? {
                val g = o.optString("g", "")
                if (g.isBlank()) return null
                return Gun(g, o.optLong("ua", 0), o.optLong("uu", 0), o.optLong("uk", 0))
            }
        }
    }

    fun defter(context: Context): List<Gun> {
        val ham = prefs(context).getString("defter", "[]") ?: "[]"
        return try {
            val dizi = JSONArray(ham)
            val liste = mutableListOf<Gun>()
            for (i in 0 until dizi.length()) {
                dizi.optJSONObject(i)?.let { Gun.jsondan(it) }?.let { liste.add(it) }
            }
            liste
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun defterYaz(context: Context, liste: List<Gun>) {
        val kirpik = liste.takeLast(DEFTER_GUN)
        val dizi = JSONArray()
        kirpik.forEach { dizi.put(it.json()) }
        prefs(context).edit().putString("defter", dizi.toString()).apply()
    }

    /** v10.47: Kullanıcı maddesi #9 — Uyku veya uyanma saatini elle değiştirme/kaydetme. */
    fun elleKaydet(context: Context, gunKey: String, uyandiMs: Long, uyuduMs: Long) {
        val mevcut = defter(context).toMutableList()
        val idx = mevcut.indexOfFirst { it.gunKey == gunKey }
        val suresi = uykuSuresiMs(uyuduMs, uyandiMs) ?: 0L
        val yeni = Gun(gunKey, uyandiMs, uyuduMs, suresi)
        if (idx >= 0) {
            mevcut[idx] = yeni
        } else {
            mevcut.add(yeni)
        }
        defterYaz(context, mevcut)
    }

    /** Belirtilen günün kaydını siler. */
    fun gunSil(context: Context, gunKey: String) {
        val mevcut = defter(context).filter { it.gunKey != gunKey }
        defterYaz(context, mevcut)
    }

    /** Gün anahtarındaki satırı döner (yoksa null). */
    fun gunBul(context: Context, gunKey: String): Gun? =
        defter(context).firstOrNull { it.gunKey == gunKey }

    /**
     * "Uyandım" onayı: bugünün satırına uyanma saati yazılır.
     * Dün gece "uyuyorum" dediyse uyku süresi de kapanır.
     */
    fun uyandiKaydet(context: Context, simdiMs: Long) {
        val liste = defter(context).toMutableList()
        val anahtar = gunKey(simdiMs)
        val eski = liste.indexOfFirst { it.gunKey == anahtar }
        val dunUyudu = dinOncekiGunUyudu(liste, simdiMs)
        // Gece yarısını geçip "uyuyorum" diyenin (örn. 00:30) kaydı
        // BUGÜNÜN satırına düşer; çiftleşme önce düne, bulamazsa
        // aynı güne bakar — aksi hâlde geç yatanların uykusu hiç
        // ölçülemezdi.
        val ayniGunUyudu = if (eski >= 0) liste[eski].uyuduMs else 0L
        val eslesen = if (dunUyudu > 0) dunUyudu else ayniGunUyudu
        val uyku = if (eslesen > 0) uykuSuresiMs(eslesen, simdiMs) ?: 0L else 0L
        val yeni = Gun(anahtar, simdiMs, eslesen, uyku)
        if (eski >= 0) liste[eski] = yeni else liste.add(yeni)
        defterYaz(context, liste)
    }

    /** "Uyuyorum" onayı: bugünün satırına uyuma saati yazılır. */
    fun uyuduKaydet(context: Context, simdiMs: Long) {
        val liste = defter(context).toMutableList()
        val anahtar = gunKey(simdiMs)
        val eski = liste.indexOfFirst { it.gunKey == anahtar }
        if (eski >= 0) {
            liste[eski] = liste[eski].copy(uyuduMs = simdiMs)
        } else {
            liste.add(Gun(anahtar, 0, simdiMs, 0))
        }
        defterYaz(context, liste)
    }

    /** [simdiMs]'ten bir önceki günün defter kaydındaki uyuma saati. */
    fun dinOncekiGunUyudu(liste: List<Gun>, simdiMs: Long): Long {
        val dun = gunKey(simdiMs - 86400_000L)
        return liste.firstOrNull { it.gunKey == dun }?.uyuduMs ?: 0L
    }

    fun defteriTemizle(context: Context) {
        prefs(context).edit().putString("defter", "[]").apply()
    }

    // ═══════════════════════════════════════════════════════════════
    // SAF MANTIK — saat, süre, ortalama, özet, karar
    // ═══════════════════════════════════════════════════════════════

    /** "20260807" biçiminde gün anahtarı. */
    fun gunKey(ms: Long): String =
        SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(ms))

    /** 420 → "07:00". */
    fun saatMetni(dk: Int): String {
        val d = dk.coerceIn(0, 1439)
        return String.format(Locale.US, "%02d:%02d", d / 60, d % 60)
    }

    /**
     * Bir sonraki [hedefDk] alarmının milisaniyesi.
     * Bugünkü hedef geçmişse yarına taşınır.
     */
    fun sonrakiAlarm(simdiMs: Long, hedefDk: Int): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = simdiMs
        cal.set(Calendar.HOUR_OF_DAY, hedefDk / 60)
        cal.set(Calendar.MINUTE, hedefDk % 60)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis <= simdiMs) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    /**
     * Gerçekleşen uyku süresi (uyuma → uyanma).
     * Geçersiz girdi (ters sıra veya [MAKS_UYKU_SAAT] üstü) null —
     * çift gün aşırı uzun "uyku"lar ölçüm hatasıdır, defteri kirletmez.
     */
    fun uykuSuresiMs(uyuduMs: Long, uyandiMs: Long): Long? {
        if (uyuduMs <= 0 || uyandiMs <= uyuduMs) return null
        val fark = uyandiMs - uyuduMs
        return if (fark <= MAKS_UYKU_SAAT * 3600_000L) fark else null
    }

    /** ms → gün içi dakika (0..1439). */
    fun dakikaOfMs(ms: Long): Int {
        val cal = Calendar.getInstance()
        cal.timeInMillis = ms
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    /**
     * Uyanma saatlerinin düz ortalaması (dakika cinsinden).
     * Uyanışlar sabah kümeslidir; gece yarısı sarması beklenmez.
     */
    fun ortalamaUyanmaDk(liste: List<Int>): Int? {
        if (liste.isEmpty()) return null
        return (liste.sum().toDouble() / liste.size).toInt()
    }

    /**
     * Uyuma saatlerinin sarmal ortalaması.
     *
     * Uyuşmalar gece yarısını SIGMAZ: 23:50 ile 00:10'un düz
     * ortalaması öğlen 12:00 çıkardı. Referansı öğlene kaydırıp
     * ((dk + 720) % 1440) ortalaması alınır, geri kaydırılır —
     * küme gece etrafında dağıldığı için doğru merkez bulunur.
     */
    fun ortalamaUyumaDk(liste: List<Int>): Int? {
        if (liste.isEmpty()) return null
        val kayik = liste.map { (it + 720) % 1440 }
        val ort = (kayik.sum().toDouble() / kayik.size).toInt()
        return (ort - 720 + 1440) % 1440
    }

    /** Ortalama uyku süresi (ms) — 0 olmayan kayıtlar arasında. */
    fun ortalamaUykuMs(liste: List<Gun>): Long? {
        val gecerli = liste.map { it.uykuMs }.filter { it > 0 }
        if (gecerli.isEmpty()) return null
        return gecerli.sum() / gecerli.size
    }

    /** ms süre → "7 sa 32 dk" metni. */
    fun sureKisa(ms: Long): String {
        val dk = (ms / 60_000L).toInt().coerceAtLeast(0)
        val saat = dk / 60
        val kalan = dk % 60
        return if (saat > 0) "${saat} sa ${kalan} dk" else "${kalan} dk"
    }

    // ── Akşam özeti seçimi ──

    /** Akşam özetindeki bir satır türü. */
    enum class OzetParca { ODAK, GOREV, SERI, ZINCIR }

    /**
     * Bayraklara göre özet satırlarını seçer. Hepsi kapalıysa boş
     * liste — çağıran "özet satırı yok" durumunu yazar.
     */
    fun ozetSecimi(odak: Boolean, gorev: Boolean, seri: Boolean, zincir: Boolean): List<OzetParca> {
        val secilen = mutableListOf<OzetParca>()
        if (odak) secilen.add(OzetParca.ODAK)
        if (gorev) secilen.add(OzetParca.GOREV)
        if (zincir) secilen.add(OzetParca.ZINCIR)
        if (seri) secilen.add(OzetParca.SERI)
        return secilen
    }

    /**
     * Tekrar sorgusu gerekli mi?
     *
     * Cevaplanmış ya da teslim edilmiş kapının tekrarı olmaz;
     * sayaç [maks]'a ulaşmadıysa bir tur daha sorulur.
     */
    fun tekrarGerekliMi(yapilan: Int, maks: Int, cevaplandi: Boolean, verildi: Boolean): Boolean =
        !cevaplandi && !verildi && yapilan < maks

    /**
     * Yeni tekrar sayacı (ilk sorgu 0 → yanıt yoksa 1. tekrar).
     */
    fun sonrakiTekrar(me: Int): Int = (me + 1).coerceAtLeast(0)
}
