package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * v7.80 — Koçun takip ettiği program. **İki kaynak destekler.**
 *
 * ── v7.79'daki eksik ──
 * Program yalnızca [Store.Course] (Kurslar sekmesi) üzerinden kurulabiliyordu.
 * Kullanıcı haklı olarak itiraz etti:
 *   "Sadece mühendislik kursları vb şeylerde yardımcı olmasın, Konular
 *    kısmındaki konuları da seçme hakkım olsun, onu sırasıyla bitirtsin"
 *
 * ── Çözüm ──
 * Program kaynağı iki türlü olabilir:
 *   · [KAYNAK_KURS] → Kurs > Bölüm > Ders  (ders = `Store.Lesson`)
 *   · [KAYNAK_KONU] → Konu > Maddeler      (ders = `Store.SubItem`)
 *
 * İki yapı da "sıralı, tek tek bitirilen birim listesi" olduğu için ortak
 * bir soyutlamada ([Adim]) birleştirildi. Koçun geri kalanı hangi kaynağın
 * kullanıldığını **bilmez** — sadece `aktifAdim()` sorar.
 *
 * ── Neden ortak `Adim` tipi ──
 * `Store.Lesson` ve `Store.SubItem` farklı sınıflar; ikisini de kabul eden
 * kod her yerde `when` yazmak zorunda kalırdı. Tek okuma-amaçlı sarmalayıcı
 * ile çağıran taraf sadeleşiyor. Yazma işlemleri (bitirme) kaynağa göre
 * ayrışıyor — orası zaten tek noktada ([adimiBitir]).
 */
object Mufredat {

    private const val TAG = "Mufredat"
    private const val PREF = "mufredat_v1"

    /** Program kaynağı yok. */
    const val KAYNAK_YOK = 0

    /** Kurslar sekmesindeki bir kurs. */
    const val KAYNAK_KURS = 1

    /** Konular sekmesindeki bir konu. */
    const val KAYNAK_KONU = 2

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ═══════════════════════════════════════════════════════════════
    // ORTAK ADIM MODELİ
    // ═══════════════════════════════════════════════════════════════

    /**
     * Programdaki tek bir birim — ders ya da konu maddesi.
     *
     * @param id kaynak kaydın kimliği (Lesson.id veya SubItem.id)
     * @param baslik ekranda görünen ad
     * @param bitti tamamlandı mı
     * @param aciklama varsa ek metin (yalnızca derste dolu)
     * @param dakika planlanan süre (yalnızca derste dolu, 0 = yok)
     * @param assetYolu ders PDF'i (yalnızca derste dolu)
     * @param ustBaslik bölüm adı (derste) — bağlam için
     */
    data class Adim(
        val id: Long,
        val baslik: String,
        val bitti: Boolean,
        val aciklama: String = "",
        val dakika: Int = 0,
        val assetYolu: String = "",
        val ustBaslik: String = ""
    )

    // ═══════════════════════════════════════════════════════════════
    // KAYNAK SEÇİMİ
    // ═══════════════════════════════════════════════════════════════

    fun kaynakTuru(context: Context): Int =
        prefs(context).getInt("kaynak_tur", KAYNAK_YOK)

    /** Seçili kurs/konu kimliği. 0 = seçilmedi. */
    fun kaynakId(context: Context): Long = prefs(context).getLong("kaynak_id", 0L)

    fun kaynakSec(context: Context, tur: Int, id: Long) {
        prefs(context).edit()
            .putInt("kaynak_tur", tur)
            .putLong("kaynak_id", id)
            .remove("kilit_adim")   // kaynak değişti, eski kilit geçersiz
            .apply()
    }

    fun kaynagiKaldir(context: Context) {
        prefs(context).edit()
            .putInt("kaynak_tur", KAYNAK_YOK)
            .putLong("kaynak_id", 0L)
            .remove("kilit_adim")
            .apply()
    }

    fun secildiMi(context: Context): Boolean =
        kaynakTuru(context) != KAYNAK_YOK && kaynakId(context) != 0L

    /**
     * v7.79 uyumluluğu — eski `kurs_id` anahtarını yeni biçime taşır.
     *
     * v7.79'da yalnızca kurs vardı ve `kurs_id` anahtarına yazılıyordu.
     * Güncelleyen kullanıcı seçimini kaybetmesin diye bir kez taşınır.
     */
    fun eskiSecimiTasi(context: Context) {
        val p = prefs(context)
        if (p.getBoolean("tasindi_v80", false)) return
        val eski = p.getLong("kurs_id", 0L)
        if (eski != 0L && p.getLong("kaynak_id", 0L) == 0L) {
            p.edit()
                .putInt("kaynak_tur", KAYNAK_KURS)
                .putLong("kaynak_id", eski)
                .apply()
        }
        p.edit().putBoolean("tasindi_v80", true).apply()
    }

    /** Programın adı — kurs başlığı ya da konu başlığı. */
    fun programAdi(context: Context): String {
        val id = kaynakId(context)
        return when (kaynakTuru(context)) {
            KAYNAK_KURS ->
                Store.loadCourses(context).firstOrNull { it.id == id }?.title
                    ?: context.getString(R.string.mf_kurs_yok)
            KAYNAK_KONU ->
                Store.loadTopics(context).firstOrNull { it.id == id }?.title
                    ?: context.getString(R.string.mf_konu_yok)
            else -> context.getString(R.string.mf_kurs_yok)
        }
    }

    /** Kaynak türünün okunabilir adı — "Kurs" / "Konu". */
    fun kaynakAdi(context: Context): String = context.getString(
        when (kaynakTuru(context)) {
            KAYNAK_KURS -> R.string.mf_tur_kurs
            KAYNAK_KONU -> R.string.mf_tur_konu
            else -> R.string.mf_tur_yok
        }
    )

    // ═══════════════════════════════════════════════════════════════
    // ADIM LİSTESİ
    // ═══════════════════════════════════════════════════════════════

    /**
     * Programdaki tüm adımlar, **sırayla**.
     *
     * Kursta sıralama: bölüm sırası → ders sırası → id.
     * Konuda sıralama: maddelerin eklenme sırası (listedeki hâli).
     * Her ikisi de ilgili sekmedeki görünümle birebir aynı olmalı.
     */
    fun adimlar(context: Context): List<Adim> {
        val id = kaynakId(context)
        if (id == 0L) return emptyList()

        return when (kaynakTuru(context)) {
            KAYNAK_KURS -> {
                val bolumSirasi = Store.loadSections(context)
                    .filter { it.courseId == id }
                    .sortedBy { it.order }
                    .mapIndexed { i, s -> s.id to i }
                    .toMap()
                val bolumAdi = Store.loadSections(context)
                    .associate { it.id to it.title }

                Store.loadLessons(context)
                    .filter { it.courseId == id }
                    .sortedWith(
                        compareBy(
                            { bolumSirasi[it.sectionId] ?: Int.MAX_VALUE },
                            { it.order },
                            { it.id }
                        )
                    )
                    .map { d ->
                        Adim(
                            id = d.id,
                            baslik = d.title,
                            bitti = d.done,
                            aciklama = d.desc,
                            dakika = d.minutes,
                            assetYolu = d.pdfAsset,
                            ustBaslik = bolumAdi[d.sectionId].orEmpty()
                        )
                    }
            }

            KAYNAK_KONU -> {
                val konu = Store.loadTopics(context).firstOrNull { it.id == id }
                    ?: return emptyList()
                konu.items.map { m ->
                    Adim(
                        id = m.id,
                        baslik = m.text,
                        bitti = m.done,
                        ustBaslik = konu.title
                    )
                }
            }

            else -> emptyList()
        }
    }

    /**
     * Sırada bekleyen ilk bitmemiş adım — koçun **aktif konusu**.
     *
     * Elle kilitlenmiş adım varsa ve hâlâ bitmemişse o döner.
     */
    fun aktifAdim(context: Context): Adim? {
        val liste = adimlar(context)
        if (liste.isEmpty()) return null

        val kilit = prefs(context).getLong("kilit_adim", 0L)
        if (kilit != 0L) {
            val a = liste.firstOrNull { it.id == kilit }
            if (a != null && !a.bitti) return a
            prefs(context).edit().remove("kilit_adim").apply()
        }
        return liste.firstOrNull { !it.bitti }
    }

    fun adimKilitle(context: Context, adimId: Long) {
        prefs(context).edit().putLong("kilit_adim", adimId).apply()
    }

    fun kilidiKaldir(context: Context) {
        prefs(context).edit().remove("kilit_adim").apply()
    }

    fun kilitliMi(context: Context): Boolean =
        prefs(context).getLong("kilit_adim", 0L) != 0L

    fun aktifSira(context: Context): Int {
        val aktif = aktifAdim(context) ?: return 0
        return adimlar(context).indexOfFirst { it.id == aktif.id } + 1
    }

    // ═══════════════════════════════════════════════════════════════
    // İLERLEME
    // ═══════════════════════════════════════════════════════════════

    data class Ilerleme(
        val toplam: Int,
        val biten: Int,
        val aktifAd: String,
        val aktifSira: Int
    ) {
        val yuzde: Int get() = if (toplam == 0) 0 else biten * 100 / toplam
        val bittiMi: Boolean get() = toplam > 0 && biten >= toplam
        val kalan: Int get() = (toplam - biten).coerceAtLeast(0)
    }

    fun ilerleme(context: Context): Ilerleme {
        val liste = adimlar(context)
        val aktif = aktifAdim(context)
        return Ilerleme(
            toplam = liste.size,
            biten = liste.count { it.bitti },
            aktifAd = aktif?.baslik.orEmpty(),
            aktifSira = if (aktif == null) 0
            else liste.indexOfFirst { it.id == aktif.id } + 1
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // BİTİRME
    // ═══════════════════════════════════════════════════════════════

    /**
     * Aktif adımı tamamlandı olarak işaretler ve sıradakine geçer.
     *
     * Kaynağa göre farklı tabloya yazar — bu, iki kaynak ayrımının
     * gerçekten önemli olduğu tek yer.
     *
     * @return sıradaki adım, hepsi bittiyse null
     */
    fun aktifAdimiBitir(context: Context): Adim? {
        val aktif = aktifAdim(context) ?: return null
        adimDurumu(context, aktif.id, true)
        kilidiKaldir(context)
        bitirmeKaydet(context, aktif.id)
        return aktifAdim(context)
    }

    /** Bir adımın bitti/bitmedi durumunu değiştirir. */
    fun adimDurumu(context: Context, adimId: Long, bitti: Boolean) {
        when (kaynakTuru(context)) {
            KAYNAK_KURS -> {
                val hepsi = Store.loadLessons(context)
                hepsi.firstOrNull { it.id == adimId }?.done = bitti
                Store.saveLessons(context, hepsi)
            }
            KAYNAK_KONU -> {
                val konular = Store.loadTopics(context)
                konular.firstOrNull { it.id == kaynakId(context) }
                    ?.items?.firstOrNull { it.id == adimId }?.done = bitti
                Store.saveTopics(context, konular)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ADIM BAZLI ÇALIŞMA KAYDI
    // ═══════════════════════════════════════════════════════════════

    private const val K_KAYIT = "ders_kayit_json"

    data class AdimKayit(
        val adimId: Long,
        var dakika: Int = 0,
        var oturum: Int = 0,
        var sonCalisma: Long = 0L,
        var bitirildi: Long = 0L
    )

    private fun kayitlar(context: Context): MutableList<AdimKayit> {
        val ham = prefs(context).getString(K_KAYIT, "[]") ?: "[]"
        val liste = mutableListOf<AdimKayit>()
        try {
            val dizi = JSONArray(ham)
            for (i in 0 until dizi.length()) {
                val o = dizi.optJSONObject(i) ?: continue
                liste.add(
                    AdimKayit(
                        adimId = o.optLong("id"),
                        dakika = o.optInt("dk"),
                        oturum = o.optInt("ot"),
                        sonCalisma = o.optLong("son"),
                        bitirildi = o.optLong("bit")
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Adım kayıtları okunamadı", e)
        }
        return liste
    }

    private fun kayitlariYaz(context: Context, liste: List<AdimKayit>) {
        val dizi = JSONArray()
        liste.takeLast(300).forEach { k ->
            dizi.put(
                JSONObject()
                    .put("id", k.adimId).put("dk", k.dakika)
                    .put("ot", k.oturum).put("son", k.sonCalisma)
                    .put("bit", k.bitirildi)
            )
        }
        prefs(context).edit().putString(K_KAYIT, dizi.toString()).apply()
    }

    fun adimKaydi(context: Context, adimId: Long): AdimKayit =
        kayitlar(context).firstOrNull { it.adimId == adimId } ?: AdimKayit(adimId)

    fun dakikaEkle(context: Context, adimId: Long, dakika: Int) {
        if (dakika <= 0) return
        val liste = kayitlar(context)
        val k = liste.firstOrNull { it.adimId == adimId }
        if (k != null) {
            k.dakika += dakika
            k.oturum += 1
            k.sonCalisma = System.currentTimeMillis()
        } else {
            liste.add(AdimKayit(adimId, dakika, 1, System.currentTimeMillis(), 0L))
        }
        kayitlariYaz(context, liste)
    }

    private fun bitirmeKaydet(context: Context, adimId: Long) {
        val liste = kayitlar(context)
        val k = liste.firstOrNull { it.adimId == adimId }
        if (k != null) k.bitirildi = System.currentTimeMillis()
        else liste.add(AdimKayit(adimId, 0, 0, 0L, System.currentTimeMillis()))
        kayitlariYaz(context, liste)
    }

    // ═══════════════════════════════════════════════════════════════
    // YAPAY ZEKÂ BAĞLAMI
    // ═══════════════════════════════════════════════════════════════

    /**
     * Aktif adımın yapay zekâya verilecek tanıtımı.
     *
     * Koçun **her** isteminin başına eklenir; model başka konuya kaymasın.
     * Alan (mühendislik/dil/tarih…) sabitlenmez — modelin başlıktan
     * anlaması istenir, çünkü kullanıcı her türlü konuyu takip edebilir.
     */
    fun aiBaglami(context: Context): String {
        if (!secildiMi(context)) return ""
        val ilerleme = ilerleme(context)
        val aktif = aktifAdim(context)
        val turAdi = kaynakAdi(context)
        val program = programAdi(context)

        if (aktif == null) {
            return "PROGRAM: \"$program\" ($turAdi) — TÜMÜ BİTTİ " +
                "(${ilerleme.toplam}/${ilerleme.toplam})."
        }

        val birim = context.getString(
            if (kaynakTuru(context) == KAYNAK_KURS) R.string.mf_birim_ders
            else R.string.mf_birim_konu
        )

        val sb = StringBuilder()
        sb.append("=== ÖĞRENCİNİN PROGRAMI ===\n")
        sb.append("$turAdi: $program\n")
        if (aktif.ustBaslik.isNotBlank() && aktif.ustBaslik != program) {
            sb.append("Bölüm: ${aktif.ustBaslik}\n")
        }
        sb.append("ŞU AN ÇALIŞILAN $birim: \"${aktif.baslik}\" ")
        sb.append("(${ilerleme.aktifSira}. / toplam ${ilerleme.toplam})\n")
        if (aktif.aciklama.isNotBlank()) {
            sb.append("İçerik: ${aktif.aciklama.take(300)}\n")
        }
        if (aktif.dakika > 0) sb.append("Planlanan süre: ${aktif.dakika} dk\n")

        val kayit = adimKaydi(context, aktif.id)
        if (kayit.dakika > 0) {
            sb.append("Buna verdiği süre: ${kayit.dakika} dk (${kayit.oturum} oturum)\n")
        }
        sb.append("Tamamlanan: ${ilerleme.biten}/${ilerleme.toplam}\n")

        sb.append("\nMUTLAK KURAL: Öğrenci ŞU AN sadece \"${aktif.baslik}\" ")
        sb.append("konusunu çalışıyor. BAŞKA hiçbir konuya, derse veya programa ")
        sb.append("DEĞİNME. Yeni konu önerme, program değiştirme, \"şunu da çalış\" deme. ")
        sb.append("Sadece bunu bitirtmeye odaklan.\n")
        sb.append("Konunun alanını başlıktan anla ve o alanın uzmanı gibi davran; ")
        sb.append("kendi uzmanlık alanını varsayma.\n")
        sb.append("=== PROGRAM SONU ===")

        return sb.toString()
    }

    fun aktifAsset(context: Context): String = aktifAdim(context)?.assetYolu.orEmpty()

    // ═══════════════════════════════════════════════════════════════
    // ÖZET
    // ═══════════════════════════════════════════════════════════════

    fun durumMetni(context: Context): String {
        if (!secildiMi(context)) return context.getString(R.string.mf_kurs_secilmedi)
        val i = ilerleme(context)
        if (i.toplam == 0) return context.getString(R.string.mf_bos_program)
        if (i.bittiMi) return context.getString(R.string.mf_program_bitti, programAdi(context))
        return context.getString(R.string.mf_aktif, i.aktifSira, i.toplam, i.aktifAd)
    }
}
