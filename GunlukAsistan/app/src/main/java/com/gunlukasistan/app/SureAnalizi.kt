package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

/**
 * v9.4 — Süre tahmini ve Pomodoro istatistiği (öneri 13, 14, 15).
 *
 * ══════════════════════════════════════════════════════════════════
 * ÖNERİ 13 — Süre tahmini öğrenmesi
 * ══════════════════════════════════════════════════════════════════
 * `OdakKaydi` her çalışma oturumunu kaydediyor (v7.94'ten beri) ama
 * **veri hiç kullanılmıyordu**. Elimizde şu var:
 *   · Hangi adıma kaç dakika harcandı
 *   · Kaç oturumda bitti
 *
 * Eksik olan: kullanıcının TAHMİNİ. "Bu konuya 30 dakika" deyip
 * 52 dakika harcadığını kimse ölçmüyordu.
 *
 * Planlama yapmanın en büyük düşmanı "planlama yanılgısı"
 * (planning fallacy): insanlar sistematik olarak işleri olduğundan
 * kısa tahmin ediyor. Ölçülünce düzeliyor.
 *
 * Bu sınıf tahmin ile gerçeği eşleştirip **kişisel çarpan**
 * hesaplıyor: "Sen genelde %70 daha uzun sürüyorsun, 30 dk dediğine
 * 51 dk ayır."
 *
 * ══════════════════════════════════════════════════════════════════
 * ÖNERİ 14 — Pomodoro istatistiği
 * ══════════════════════════════════════════════════════════════════
 * `Pomodoro` v7.94'ten beri çalışıyor ama hiçbir ölçüm yok. Kaç
 * pomodoro tamamlandı, kaçı yarıda kesildi, hangi saatte daha
 * verimlisin — hiçbiri bilinmiyordu.
 *
 * ══════════════════════════════════════════════════════════════════
 * ÖNERİ 15 — Günlük zaman bütçesi
 * ══════════════════════════════════════════════════════════════════
 * Hedef ile planlanan arasındaki farkı gösteriyor: "4 saat hedefin
 * var, 3 saat 20 dakika planladın, 40 dakika açık."
 */
object SureAnalizi {

    private const val TAG = "SureAnalizi"
    private const val PREF = "sure_analizi_v1"
    private const val K_TAHMIN = "tahminler_json"
    private const val K_POMODORO = "pomodoro_json"

    /** Çarpan hesabı için en az kaç örnek gerekli. */
    private const val EN_AZ_ORNEK = 4

    /** Tavan — sonsuz büyümesin. */
    private const val TAVAN = 300

    private fun p(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ══════════════════════════════════════════════════════════
    // Öneri 13 — Tahmin / gerçek eşleştirme
    // ══════════════════════════════════════════════════════════

    /**
     * @param tahminDk kullanıcının verdiği tahmin
     * @param gercekDk gerçekte harcanan
     * @param etiket ne yapıldı (konu adı, ders)
     */
    data class Kayit(
        val zaman: Long,
        val tahminDk: Int,
        val gercekDk: Int,
        val etiket: String
    ) {
        /** Gerçek / tahmin oranı. 1.0 = tam isabet. */
        val oran: Double get() = if (tahminDk <= 0) 1.0 else gercekDk.toDouble() / tahminDk
    }

    fun kayitlar(c: Context): MutableList<Kayit> {
        val ham = p(c).getString(K_TAHMIN, "[]") ?: "[]"
        val liste = mutableListOf<Kayit>()
        runCatching {
            val dizi = JSONArray(ham)
            for (i in 0 until dizi.length()) {
                val o = dizi.optJSONObject(i) ?: continue
                liste.add(
                    Kayit(
                        zaman = o.optLong("z"),
                        tahminDk = o.optInt("t"),
                        gercekDk = o.optInt("g"),
                        etiket = o.optString("e", "")
                    )
                )
            }
        }.onFailure { android.util.Log.w(TAG, "kayitlar", it) }
        return liste
    }

    private fun kaydet(c: Context, liste: List<Kayit>) {
        runCatching {
            val kirpik = if (liste.size > TAVAN) liste.takeLast(TAVAN) else liste
            val dizi = JSONArray()
            kirpik.forEach {
                dizi.put(
                    JSONObject()
                        .put("z", it.zaman).put("t", it.tahminDk)
                        .put("g", it.gercekDk).put("e", it.etiket)
                )
            }
            p(c).edit().putString(K_TAHMIN, dizi.toString()).apply()
        }.onFailure { android.util.Log.w(TAG, "kaydet", it) }
    }

    /** Bir tahmin/gerçek çifti ekler. */
    fun tahminEkle(c: Context, tahminDk: Int, gercekDk: Int, etiket: String = "") {
        if (tahminDk <= 0 || gercekDk <= 0) return
        // Aşırı uçları at: 10 kattan fazla sapma veri hatası olabilir
        // (kullanıcı sayacı kapatmayı unutmuştur)
        val oran = gercekDk.toDouble() / tahminDk
        if (oran > 10.0 || oran < 0.1) {
            android.util.Log.w(TAG, "Aşırı sapma atlandı: $tahminDk → $gercekDk")
            return
        }
        val liste = kayitlar(c)
        liste.add(Kayit(System.currentTimeMillis(), tahminDk, gercekDk, etiket))
        kaydet(c, liste)
    }

    /**
     * Kişisel süre çarpanı.
     *
     * ── Neden medyan, ortalama değil ──
     * Tek bir uzun oturum (sayacı kapatmayı unutmak) ortalamayı
     * bozar. Medyan aykırı değerlere dayanıklı.
     *
     * @return çarpan (1.0 = tahminler isabetli), yetersiz veri varsa null
     */
    fun carpan(c: Context): Double? {
        val liste = kayitlar(c)
        if (liste.size < EN_AZ_ORNEK) return null
        val oranlar = liste.map { it.oran }.sorted()
        val orta = oranlar.size / 2
        val medyan = if (oranlar.size % 2 == 0) {
            (oranlar[orta - 1] + oranlar[orta]) / 2
        } else oranlar[orta]
        // Makul sınırlar içinde tut
        return medyan.coerceIn(0.5, 3.0)
    }

    /**
     * Tahmini düzeltir.
     *
     * "30 dakika" diyorsan ve çarpanın 1.7 ise 51 dakika ayır.
     */
    fun duzeltilmisTahmin(c: Context, tahminDk: Int): Int {
        val k = carpan(c) ?: return tahminDk
        return Math.round(tahminDk * k).toInt().coerceAtLeast(1)
    }

    /** Yeterli veri var mı? (arayüz "henüz öğreniyorum" diyebilsin) */
    fun veriYeterliMi(c: Context): Boolean = kayitlar(c).size >= EN_AZ_ORNEK

    fun ornekSayisi(c: Context): Int = kayitlar(c).size

    /**
     * Kullanıcıya gösterilecek dürüst özet.
     *
     * Suçlayıcı değil, bilgilendirici bir dil seçildi: "kötü tahmin
     * ediyorsun" değil, "işler tahmininden uzun sürüyor, buna göre
     * planla".
     */
    fun ozetMetni(c: Context): String? {
        val k = carpan(c) ?: return null
        val yuzde = Math.round((k - 1.0) * 100).toInt()
        return when {
            yuzde > 15 -> c.getString(R.string.sa_uzun_suruyor, yuzde)
            yuzde < -15 -> c.getString(R.string.sa_kisa_suruyor, -yuzde)
            else -> c.getString(R.string.sa_isabetli)
        }
    }

    // ══════════════════════════════════════════════════════════
    // Öneri 14 — Pomodoro istatistiği
    // ══════════════════════════════════════════════════════════

    /**
     * @param tamamlandi tam süre çalışıldı mı, yoksa yarıda mı kesildi
     * @param saat günün hangi saatinde başladı (0-23)
     */
    data class PomodoroKayit(
        val zaman: Long,
        val sureDk: Int,
        val tamamlandi: Boolean,
        val saat: Int,
        /** v10.2 · Öneri A12: bitişte tek dokunuş değerlendirme. 0=yok, 1=dağınık, 2=orta, 3=odaklı. */
        val kalite: Int = 0
    )

    fun pomodorolar(c: Context): MutableList<PomodoroKayit> {
        val ham = p(c).getString(K_POMODORO, "[]") ?: "[]"
        val liste = mutableListOf<PomodoroKayit>()
        runCatching {
            val dizi = JSONArray(ham)
            for (i in 0 until dizi.length()) {
                val o = dizi.optJSONObject(i) ?: continue
                liste.add(
                    PomodoroKayit(
                        zaman = o.optLong("z"),
                        sureDk = o.optInt("s"),
                        tamamlandi = o.optBoolean("t", true),
                        saat = o.optInt("h", 0),
                        // v10.2: eski kayıtlarda "k" yok — 0 (değerlendirilmemiş) sayılır
                        kalite = o.optInt("k", 0)
                    )
                )
            }
        }
        return liste
    }

    /** Bir pomodoro turu bittiğinde çağrılır. */
    fun pomodoroKaydet(c: Context, sureDk: Int, tamamlandi: Boolean) {
        pomodoroKaydetK(c, sureDk, tamamlandi, kalite = 0)
    }

    /**
     * v10.2 · Öneri A12 — Kalite puanlı kayıt.
     *
     * Bitiş ekranındaki tek dokunuş (dağınık/orta/odaklı) buradan yazılır.
     * Eski çağrılar [pomodoroKaydet] üzerinden aynen çalışır (kalite=0).
     */
    fun pomodoroKaydetK(c: Context, sureDk: Int, tamamlandi: Boolean, kalite: Int) {
        runCatching {
            val liste = pomodorolar(c)
            val saat = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            liste.add(
                PomodoroKayit(
                    System.currentTimeMillis(), sureDk, tamamlandi, saat,
                    kalite.coerceIn(0, 3)
                )
            )
            val kirpik = if (liste.size > TAVAN) liste.takeLast(TAVAN) else liste
            val dizi = JSONArray()
            kirpik.forEach {
                dizi.put(
                    JSONObject().put("z", it.zaman).put("s", it.sureDk)
                        .put("t", it.tamamlandi).put("h", it.saat)
                        .put("k", it.kalite)
                )
            }
            p(c).edit().putString(K_POMODORO, dizi.toString()).apply()
        }.onFailure { android.util.Log.w(TAG, "pomodoroKaydetK", it) }
    }

    /**
     * Değerlendirilmiş oturumların ortalama kalitesi (0.0-3.0).
     * Veri yoksa -1 — arayüz "henüz değerlendirme yok" diyebilsin.
     */
    fun kaliteOrtalamasi(c: Context): Float {
        val puanli = pomodorolar(c).filter { it.kalite > 0 }
        if (puanli.isEmpty()) return -1f
        return puanli.sumOf { it.kalite }.toFloat() / puanli.size
    }

    /**
     * v10.2 · Öz denetim düzeltmesi — bitiş değerlendirmesini YENİ kayıt
     * olarak eklemek ÇİFT SAYIM yapıyordu (Pomodoro açıkken döngü zaten
     * `tamamlandi=true` kaydı yazıyor, değerlendirme üstüne bir tane
     * daha atıyordu).
     *
     * Son puanlanmamış TAMAMLANMIŞ kayda puanı işler. Kayıt [pencereMs]
     * içindeyse "aynı oturum" sayılır.
     *
     * @return true = mevcut kayda işlendi; false = uygun kayıt yok
     */
    fun sonKaydiKalitele(c: Context, kalite: Int, pencereMs: Long = 5 * 60_000L): Boolean {
        return runCatching {
            val liste = pomodorolar(c)
            val simdi = System.currentTimeMillis()
            val indeks = liste.indexOfLast {
                it.tamamlandi && it.kalite == 0 && (simdi - it.zaman) <= pencereMs
            }
            if (indeks < 0) return@runCatching false
            liste[indeks] = liste[indeks].copy(kalite = kalite.coerceIn(0, 3))
            val kirpik = if (liste.size > TAVAN) liste.takeLast(TAVAN) else liste
            val dizi = JSONArray()
            kirpik.forEach {
                dizi.put(
                    JSONObject().put("z", it.zaman).put("s", it.sureDk)
                        .put("t", it.tamamlandi).put("h", it.saat)
                        .put("k", it.kalite)
                )
            }
            p(c).edit().putString(K_POMODORO, dizi.toString()).apply()
            true
        }.getOrDefault(false)
    }

    data class PomodoroOzet(
        val toplam: Int,
        val tamamlanan: Int,
        val yarimKalan: Int,
        val toplamDk: Int,
        /** En verimli saat (en çok tamamlanan pomodoro). -1 = veri yok. */
        val enIyiSaat: Int,
        val bugunToplam: Int
    ) {
        val basariOrani: Int get() = if (toplam == 0) 0 else tamamlanan * 100 / toplam
    }

    fun pomodoroOzeti(c: Context): PomodoroOzet {
        val liste = pomodorolar(c)
        if (liste.isEmpty()) return PomodoroOzet(0, 0, 0, 0, -1, 0)

        val tamamlanan = liste.count { it.tamamlandi }
        val gunBasi = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // En verimli saat: yalnız TAMAMLANAN pomodorolara bak.
        // Yarıda kesilenler o saatte dikkatin dağıldığını gösteriyor.
        val saatDagilimi = liste.filter { it.tamamlandi }
            .groupBy { it.saat }
            .mapValues { it.value.size }
        val enIyi = saatDagilimi.maxByOrNull { it.value }?.key ?: -1

        return PomodoroOzet(
            toplam = liste.size,
            tamamlanan = tamamlanan,
            yarimKalan = liste.size - tamamlanan,
            toplamDk = liste.filter { it.tamamlandi }.sumOf { it.sureDk },
            enIyiSaat = enIyi,
            bugunToplam = liste.count { it.zaman >= gunBasi }
        )
    }

    /** Saat bazlı verimlilik — grafik için. */
    fun saatlikVerim(c: Context): List<Pair<Int, Int>> {
        val liste = pomodorolar(c).filter { it.tamamlandi }
        return (0..23).map { saat -> saat to liste.count { it.saat == saat } }
    }

    // ══════════════════════════════════════════════════════════
    // Öneri 15 — Günlük zaman bütçesi
    // ══════════════════════════════════════════════════════════

    /**
     * @param hedefDk kullanıcının günlük hedefi
     * @param yapilanDk bugün gerçekten çalışılan
     * @param planlananDk bugün için planlanmış görevlerin toplam süresi
     * @param bosDk takvime göre kalan boş süre
     */
    data class Butce(
        val hedefDk: Int,
        val yapilanDk: Int,
        val planlananDk: Int,
        val bosDk: Int
    ) {
        /** Hedefe kalan. */
        val kalanDk: Int get() = (hedefDk - yapilanDk).coerceAtLeast(0)

        /** Planlanan, kalanı karşılıyor mu? */
        val acikDk: Int get() = (kalanDk - planlananDk).coerceAtLeast(0)

        /** Boş zaman kalana yetiyor mu? */
        val yetisirMi: Boolean get() = bosDk >= kalanDk

        val yuzde: Int get() = if (hedefDk <= 0) 0 else (yapilanDk * 100 / hedefDk).coerceIn(0, 100)
    }

    fun butce(c: Context): Butce = runCatching {
        val hedef = Store.getGoalMinutes(c)
        val yapilan = Store.getTodayFocusMinutes(c)

        // Bugün vadeli görevlerin tahmini süresi (45 dk varsayılan,
        // kişisel çarpanla düzeltilmiş)
        val gunBasi = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val gorevSayisi = Store.aktifGorevler(c)
            .count { !it.done && it.dueAt in gunBasi until (gunBasi + 86_400_000L) }
        val planlanan = duzeltilmisTahmin(c, gorevSayisi * 45)

        val bos = runCatching { TakvimKopru.toplamBosDk(c) }.getOrDefault(0)

        Butce(hedef, yapilan, planlanan, bos)
    }.getOrDefault(Butce(0, 0, 0, 0))

    // ══════════════════════════════════════════════════════════
    // Bakım
    // ══════════════════════════════════════════════════════════

    fun temizle(c: Context) {
        p(c).edit().clear().apply()
    }

    fun ozet(c: Context): JSONObject = JSONObject().apply {
        runCatching {
            put("tahmin_ornek", ornekSayisi(c))
            put("carpan", carpan(c) ?: 0.0)
            val po = pomodoroOzeti(c)
            put("pomodoro_toplam", po.toplam)
            put("pomodoro_basari", po.basariOrani)
        }
    }
}
