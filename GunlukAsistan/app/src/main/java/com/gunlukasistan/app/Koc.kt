package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * v7.78 — Zorlayıcı öğretmen modu.
 *
 * ── Kullanıcının isteği ──
 * "ders çalışmam için öğretmen rolü üstlen beni zorla ders çalışmam için"
 *
 * ── Mevcut OgretmenMotoru'ndan farkı ──
 * [OgretmenMotoru] (v7.37) kullanıcı bir derse **girdiğinde** onu anlatır.
 * Pasiftir: kullanıcı gelmezse hiçbir şey olmaz. Koç ise **kullanıcıyı
 * arar**: günlük hedef koyar, saati gelince bildirim gönderir, gün sonunda
 * hesap sorar, mazeret ister, seriyi kırarsa yüzüne söyler.
 *
 * ── Zorlama neye dayanıyor ──
 * Uygulama kullanıcıyı fiziksel olarak zorlayamaz. Elimizdeki gerçek
 * kaldıraçlar:
 *   1. **Sessizde bile çalan alarm** — [ZorunluUyari] (v7.56) zaten var
 *   2. **Hesap sorma** — gün sonu "neden çalışmadın" ekranı, mazeret kaydı
 *   3. **Görünür kayıp** — seri sayacı, borç dakikası, başarısız gün sayısı
 *   4. **Kanıt** — çalıştım demek yetmez, [Kanit] ile fotoğraf ister
 *
 * ── Borç sistemi ──
 * Hedefin altında kalınan dakikalar **borç** olarak birikir ve ertesi günün
 * hedefine eklenir (tavan: hedefin 2 katı). Böylece bir günü boş geçmek
 * bedelsiz olmaz ama borç sonsuza kadar büyüyüp umutsuzluk da yaratmaz.
 */
object Koc {

    private const val TAG = "Koc"
    private const val PREF = "koc_v1"

    // ═══════════════════════════════════════════════════════════════
    // SERTLİK KADEMELERİ
    // ═══════════════════════════════════════════════════════════════

    /** Nazik: hatırlatır, geçer. */
    const val SERT_NAZIK = 0

    /** Kararlı: ısrar eder, hesap sorar. */
    const val SERT_KARARLI = 1

    /** Acımasız: sessizde alarm çalar, mazeret ister, borç yazar. */
    const val SERT_ACIMASIZ = 2

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    private fun bugunAnahtari(): String =
        SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

    private fun gunAnahtari(gunOnce: Int): String {
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_YEAR, -gunOnce)
        return SimpleDateFormat("yyyyMMdd", Locale.US).format(c.time)
    }

    // ═══════════════════════════════════════════════════════════════
    // TEMEL AYARLAR
    // ═══════════════════════════════════════════════════════════════

    fun acikMi(context: Context): Boolean = prefs(context).getBoolean("acik", false)

    fun setAcik(context: Context, acik: Boolean) {
        prefs(context).edit().putBoolean("acik", acik).apply()
        if (acik) KocZamanlayici.kur(context) else KocZamanlayici.iptal(context)
    }

    fun sertlik(context: Context): Int =
        prefs(context).getInt("sertlik", SERT_KARARLI).coerceIn(0, 2)

    fun setSertlik(context: Context, s: Int) {
        prefs(context).edit().putInt("sertlik", s.coerceIn(0, 2)).apply()
    }

    /** Günlük çalışma hedefi (dakika). */
    fun gunlukHedef(context: Context): Int =
        prefs(context).getInt("hedef_dk", 60).coerceIn(10, 600)

    fun setGunlukHedef(context: Context, dk: Int) {
        prefs(context).edit().putInt("hedef_dk", dk.coerceIn(10, 600)).apply()
    }

    /** Ders çalışma saati (0-23). Bu saatte koç seni arar. */
    fun calismaSaati(context: Context): Int =
        prefs(context).getInt("saat", 20).coerceIn(0, 23)

    fun setCalismaSaati(context: Context, saat: Int) {
        prefs(context).edit().putInt("saat", saat.coerceIn(0, 23)).apply()
        if (acikMi(context)) KocZamanlayici.kur(context)
    }

    /** Gün sonu hesap sorma saati (0-23). */
    fun hesapSaati(context: Context): Int =
        prefs(context).getInt("hesap_saat", 22).coerceIn(0, 23)

    fun setHesapSaati(context: Context, saat: Int) {
        prefs(context).edit().putInt("hesap_saat", saat.coerceIn(0, 23)).apply()
        if (acikMi(context)) KocZamanlayici.kur(context)
    }

    /** Hangi günler çalışılacak — Calendar.MONDAY..SUNDAY kümesi. */
    fun gunler(context: Context): Set<Int> {
        val ham = prefs(context).getStringSet("gunler", null)
            ?: return setOf(2, 3, 4, 5, 6, 7, 1) // varsayılan: her gün
        return ham.mapNotNull { it.toIntOrNull() }.toSet()
    }

    fun setGunler(context: Context, gunler: Set<Int>) {
        prefs(context).edit()
            .putStringSet("gunler", gunler.map { it.toString() }.toSet()).apply()
    }

    fun bugunCalismaGunuMu(context: Context): Boolean {
        // v7.96: haftalık planda 0 dakika = izin günü
        if (HaftaPlan.bugunIzinMi(context)) return false
        return gunler(context).contains(Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
    }

    /** Çalışma kanıtı (fotoğraf) istensin mi. */
    fun kanitIster(context: Context): Boolean =
        prefs(context).getBoolean("kanit", false)

    fun setKanitIster(context: Context, ister: Boolean) {
        prefs(context).edit().putBoolean("kanit", ister).apply()
    }

    // ═══════════════════════════════════════════════════════════════
    // GÜNLÜK HEDEF VE BORÇ
    // ═══════════════════════════════════════════════════════════════

    /** Birikmiş borç (dakika). */
    fun borc(context: Context): Int = prefs(context).getInt("borc", 0).coerceAtLeast(0)

    private fun setBorc(context: Context, dk: Int) {
        prefs(context).edit().putInt("borc", dk.coerceAtLeast(0)).apply()
    }

    /**
     * Bugünün gerçek hedefi = temel hedef + borç.
     *
     * Borç tavanı temel hedefin 1 katı: 60 dk hedefte en fazla 120 dk
     * istenir. Aksi halde bir hafta çalışmayan biri 500 dakikalık
     * imkânsız bir hedefle karşılaşıp tamamen pes ederdi.
     */
    fun bugunHedefi(context: Context): Int {
        // v7.96: haftalık plan varsa o günün hedefi kullanılır (öneri 6).
        // Plan kapalıysa ya da o gün tanımsızsa eski davranış sürer.
        val temel = HaftaPlan.bugunHedefi(context) ?: gunlukHedef(context)
        // İzin gününde borç eklenmez — dinlenme günü ceza günü olmamalı
        if (temel == 0) return 0
        return temel + borc(context).coerceAtMost(temel)
    }

    /** Bugün çalışılan dakika — Store'un odak sayacından. */
    fun bugunCalisilan(context: Context): Int = Store.getTodayFocusMinutes(context)

    fun bugunKalan(context: Context): Int =
        (bugunHedefi(context) - bugunCalisilan(context)).coerceAtLeast(0)

    fun bugunTamamMi(context: Context): Boolean = bugunKalan(context) == 0

    fun bugunYuzde(context: Context): Int {
        val h = bugunHedefi(context)
        return if (h == 0) 100 else (bugunCalisilan(context) * 100 / h).coerceIn(0, 100)
    }

    // ═══════════════════════════════════════════════════════════════
    // SERİ VE GÜN KAYITLARI
    // ═══════════════════════════════════════════════════════════════

    private const val K_GUNLER = "gun_kayit_json"

    /**
     * Bir günün sonucu.
     *
     * @param mazeret kullanıcının yazdığı mazeret (boşsa yok)
     * @param kabul koç mazereti kabul etti mi
     */
    data class GunKayit(
        val gun: String,
        val hedef: Int,
        val yapilan: Int,
        val mazeret: String = "",
        val kabul: Boolean = false
    ) {
        val basarili: Boolean get() = yapilan >= hedef
        val yuzde: Int get() = if (hedef == 0) 100 else (yapilan * 100 / hedef).coerceIn(0, 100)
    }

    fun gunKayitlari(context: Context): MutableList<GunKayit> {
        val ham = prefs(context).getString(K_GUNLER, "[]") ?: "[]"
        val liste = mutableListOf<GunKayit>()
        try {
            val dizi = JSONArray(ham)
            for (i in 0 until dizi.length()) {
                val o = dizi.optJSONObject(i) ?: continue
                liste.add(
                    GunKayit(
                        gun = o.optString("gun"),
                        hedef = o.optInt("hedef"),
                        yapilan = o.optInt("yapilan"),
                        mazeret = o.optString("mazeret"),
                        kabul = o.optBoolean("kabul")
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Gün kayıtları okunamadı", e)
        }
        return liste
    }

    private fun gunleriYaz(context: Context, liste: List<GunKayit>) {
        // Son 120 gün yeter — sonsuz büyümesin
        val kirpik = liste.takeLast(120)
        val dizi = JSONArray()
        kirpik.forEach { g ->
            dizi.put(
                JSONObject()
                    .put("gun", g.gun).put("hedef", g.hedef).put("yapilan", g.yapilan)
                    .put("mazeret", g.mazeret).put("kabul", g.kabul)
            )
        }
        prefs(context).edit().putString(K_GUNLER, dizi.toString()).apply()
    }

    fun gunKaydet(context: Context, kayit: GunKayit) {
        val liste = gunKayitlari(context)
        val yer = liste.indexOfFirst { it.gun == kayit.gun }
        if (yer >= 0) liste[yer] = kayit else liste.add(kayit)
        gunleriYaz(context, liste)
    }

    /**
     * Gün sonu kapanışı — hesap sorma ekranı bunu çağırır.
     *
     * Hedefe ulaşıldıysa borç silinir; ulaşılmadıysa eksik borca eklenir.
     * Mazeret kabul edildiyse borç yazılmaz (koç insaflı olabilir).
     */
    fun gunuKapat(context: Context, mazeret: String = "", mazeretKabul: Boolean = false) {
        val gun = bugunAnahtari()
        if (kapatildiMi(context, gun)) return

        val hedef = bugunHedefi(context)
        val yapilan = bugunCalisilan(context)

        gunKaydet(context, GunKayit(gun, hedef, yapilan, mazeret, mazeretKabul))
        // v7.79: günün emeği aktif derse işlensin
        runCatching { sureyiDerseYaz(context) }

        if (yapilan >= hedef) {
            setBorc(context, 0)
            seriArtir(context)
        } else {
            if (!mazeretKabul) {
                val eksik = hedef - yapilan
                setBorc(context, (borc(context) + eksik).coerceAtMost(gunlukHedef(context) * 3))
                seriSifirla(context)
            }
            // Mazeret kabul edildiyse seri korunur — bu bilinçli bir taviz:
            // hastalık gibi gerçek engellerde seriyi kırmak cesaret kırıyor.
        }
        prefs(context).edit().putString("son_kapanis", gun).apply()
    }

    fun kapatildiMi(context: Context, gun: String = bugunAnahtari()): Boolean =
        prefs(context).getString("son_kapanis", "") == gun

    /** Dün kapatılmadan geçildiyse otomatik kapat (uygulama açılışında). */
    fun gecmisiDenkleştir(context: Context) {
        if (!acikMi(context)) return
        val dun = gunAnahtari(1)
        val kayitlar = gunKayitlari(context)
        if (kayitlar.any { it.gun == dun }) return
        val sonKapanis = prefs(context).getString("son_kapanis", "") ?: ""
        if (sonKapanis.isBlank() || sonKapanis >= dun) return
        // Dün hiç kaydedilmemiş: kayıp gün olarak yaz
        val hedef = gunlukHedef(context)
        gunKaydet(context, GunKayit(dun, hedef, 0))
        setBorc(context, (borc(context) + hedef).coerceAtMost(hedef * 3))
        seriSifirla(context)
        prefs(context).edit().putString("son_kapanis", dun).apply()
    }

    // ── Seri ──────────────────────────────────────────────────────

    fun seri(context: Context): Int = prefs(context).getInt("seri", 0)

    fun enUzunSeri(context: Context): Int = prefs(context).getInt("seri_rekor", 0)

    private fun seriArtir(context: Context) {
        val yeni = seri(context) + 1
        val rekor = maxOf(enUzunSeri(context), yeni)
        prefs(context).edit().putInt("seri", yeni).putInt("seri_rekor", rekor).apply()
    }

    private fun seriSifirla(context: Context) {
        prefs(context).edit().putInt("seri", 0).apply()
    }

    // ═══════════════════════════════════════════════════════════════
    // İSTATİSTİK
    // ═══════════════════════════════════════════════════════════════

    data class Karne(
        val toplamGun: Int,
        val basariliGun: Int,
        val toplamDakika: Int,
        val seri: Int,
        val rekor: Int,
        val borc: Int
    ) {
        val yuzde: Int get() = if (toplamGun == 0) 0 else basariliGun * 100 / toplamGun
    }

    fun karne(context: Context, sonGun: Int = 30): Karne {
        val kayitlar = gunKayitlari(context).takeLast(sonGun)
        return Karne(
            toplamGun = kayitlar.size,
            basariliGun = kayitlar.count { it.basarili },
            toplamDakika = kayitlar.sumOf { it.yapilan },
            seri = seri(context),
            rekor = enUzunSeri(context),
            borc = borc(context)
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // KOÇUN SÖZLERİ
    // ═══════════════════════════════════════════════════════════════

    /**
     * Duruma göre koçun tonu.
     *
     * Yapay zekâ yoksa da çalışmalı — bu yüzden hazır cümleler var.
     * AI varsa [KocMesaj] daha kişisel bir cümle üretir.
     */
    fun hatirlatmaMetni(context: Context): String {
        val kalan = bugunKalan(context)
        val s = seri(context)
        val sertlik = sertlik(context)

        if (kalan == 0) return context.getString(R.string.koc_m_tamam)

        // v7.79: program seçiliyse dersin adıyla çağır — genel laf etme
        val ders = Mufredat.aktifAdim(context)
        if (ders != null) {
            val sira = Mufredat.aktifSira(context)
            val toplam = Mufredat.ilerleme(context).toplam
            return when (sertlik) {
                SERT_NAZIK -> context.getString(
                    R.string.koc_md_nazik, ders.baslik, kalan
                )
                SERT_ACIMASIZ -> context.getString(
                    R.string.koc_md_acimasiz, sira, toplam, ders.baslik, kalan
                )
                else -> context.getString(
                    R.string.koc_md_kararli, ders.baslik, kalan, sira, toplam
                )
            }
        }

        return when (sertlik) {
            SERT_NAZIK -> context.getString(R.string.koc_m_nazik, kalan)
            SERT_ACIMASIZ -> when {
                borc(context) > 0 -> context.getString(
                    R.string.koc_m_borclu, kalan, borc(context)
                )
                s == 0 -> context.getString(R.string.koc_m_seri_yok, kalan)
                else -> context.getString(R.string.koc_m_acimasiz, kalan, s)
            }
            else -> if (s > 0) context.getString(R.string.koc_m_kararli_seri, kalan, s)
            else context.getString(R.string.koc_m_kararli, kalan)
        }
    }

    /** Gün sonu hesap metni. */
    fun hesapMetni(context: Context): String {
        val yapilan = bugunCalisilan(context)
        val hedef = bugunHedefi(context)
        val temel = if (yapilan >= hedef) {
            context.getString(R.string.koc_h_basarili, yapilan, hedef)
        } else {
            context.getString(R.string.koc_h_eksik, yapilan, hedef, hedef - yapilan)
        }
        // v7.79: hangi dersin hesabı olduğunu belirt
        val ders = Mufredat.aktifAdim(context) ?: return temel
        return temel + "\n\n" + context.getString(R.string.koc_h_ders, ders.baslik)
    }

    /**
     * v7.79 — Bugün çalışılan süreyi aktif derse yazar.
     *
     * Gün kapanışında çağrılır. Store'un günlük odak sayacı ders bazlı
     * değil; müfredat ilerlemesini görebilmek için süre derse de işlenir.
     */
    private fun sureyiDerseYaz(context: Context) {
        val ders = Mufredat.aktifAdim(context) ?: return
        val dakika = bugunCalisilan(context)
        if (dakika > 0) Mufredat.dakikaEkle(context, ders.id, dakika)
    }

    /** Sertlik adı — ayar ekranında gösterilir. */
    fun sertlikAdi(context: Context, s: Int): String = context.getString(
        when (s) {
            SERT_NAZIK -> R.string.koc_sert_nazik
            SERT_ACIMASIZ -> R.string.koc_sert_acimasiz
            else -> R.string.koc_sert_kararli
        }
    )
}
