package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

/**
 * v7.39 — 105 gömülü ders PDF'i içinde tam metin arama.
 *
 * ── Sorun ──
 * Uygulamada 58 AutoCAD + 47 Revit PDF'i var ama içlerinde arama yapılamıyordu.
 * Kullanıcı "kolon donatısı nerede geçiyor" diye soramıyordu.
 *
 * ── Çözüm ──
 * `DersMetni` zaten pdfbox ile metin çıkarıp önbelleğe alıyor. Bu sınıf onun
 * üzerine bir **indeks** kuruyor: her dersin her sayfasının metni bir kez
 * çıkarılıp diskte saklanıyor, sonraki aramalar önbellekten anında dönüyor.
 *
 * ── Tasarım kararları ──
 *  1. TAMAMEN ÇEVRİMDIŞI — yapay zekâ gerekmez, internet gerekmez, ücretsizdir.
 *  2. TEMBEL İNDEKSLEME — 105 PDF'i açılışta işlemek dakikalar sürer ve
 *     telefonu kilitler. Bunun yerine arama sırasında ilerleme göstererek
 *     tek tek işlenir; sonuç diske yazılır, ikinci arama anında olur.
 *  3. TÜRKÇE DUYARLI — "sarj" yazınca "şarj", "cizgi" yazınca "çizgi" bulunur.
 *  4. KESİLEBİLİR — kullanıcı ekrandan çıkarsa döngü durur (iptal bayrağı).
 */
object PdfArama {

    private const val TAG = "PdfArama"
    private const val INDEKS_DOSYA = "pdf_indeks_v1.json"

    /** İndeks sürümü — biçim değişirse artırılır, eski indeks atılır. */
    private const val SURUM = 1

    /** Bir sonuç parçacığında gösterilecek karakter sayısı. */
    private const val PARCA_UZUNLUK = 150

    // ═══════════════════════════════════════════════════════════════
    // MODELLER
    // ═══════════════════════════════════════════════════════════════

    /**
     * Tek bir arama sonucu.
     *
     * @param sayfa 0 tabanlı sayfa numarası
     * @param parca eşleşmenin geçtiği metin parçası
     * @param vurguBas parça içinde eşleşmenin başladığı indeks (-1 = yok)
     */
    data class Sonuc(
        val lessonId: Long,
        val dersAdi: String,
        val kursAdi: String,
        val assetPath: String,
        val sayfa: Int,
        val parca: String,
        val vurguBas: Int,
        val vurguUzunluk: Int
    )

    /** İndeksleme ilerlemesi. */
    data class Ilerleme(val islenen: Int, val toplam: Int, val suAnki: String)

    // ═══════════════════════════════════════════════════════════════
    // TÜRKÇE NORMALLEŞTİRME
    // ═══════════════════════════════════════════════════════════════

    /**
     * Türkçe karakterleri sadeleştirir ve küçük harfe çevirir.
     * Böylece "SARJ", "şarj", "Şarj" hepsi eşleşir.
     *
     * Not: İngilizce 'I' sorunu için Türkçe locale kullanılıyor.
     */
    fun normalle(s: String): String {
        val kucuk = s.lowercase(Locale("tr", "TR"))
        val sb = StringBuilder(kucuk.length)
        kucuk.forEach { c ->
            sb.append(
                when (c) {
                    'ı' -> 'i'
                    'ş' -> 's'
                    'ğ' -> 'g'
                    'ü' -> 'u'
                    'ö' -> 'o'
                    'ç' -> 'c'
                    'â' -> 'a'
                    'î' -> 'i'
                    'û' -> 'u'
                    else -> c
                }
            )
        }
        return sb.toString()
    }

    // ═══════════════════════════════════════════════════════════════
    // İNDEKS DOSYASI
    // ═══════════════════════════════════════════════════════════════

    private fun indeksDosyasi(context: Context): File =
        File(context.cacheDir, INDEKS_DOSYA)

    /**
     * İndeks: assetPath → sayfa metinleri dizisi.
     * Bellekte tutulur; ilk erişimde diskten yüklenir.
     */
    @Volatile
    private var bellekIndeks: MutableMap<String, List<String>>? = null

    @Synchronized
    private fun indeksYukle(context: Context): MutableMap<String, List<String>> {
        bellekIndeks?.let { return it }

        val harita = mutableMapOf<String, List<String>>()
        try {
            val f = indeksDosyasi(context)
            if (f.exists() && f.length() > 0) {
                val kok = JSONObject(f.readText())
                if (kok.optInt("surum") == SURUM) {
                    val veri = kok.optJSONObject("veri") ?: JSONObject()
                    veri.keys().forEach { yol ->
                        val dizi = veri.optJSONArray(yol) ?: return@forEach
                        val sayfalar = mutableListOf<String>()
                        for (i in 0 until dizi.length()) sayfalar.add(dizi.optString(i))
                        harita[yol] = sayfalar
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "İndeks okunamadı, sıfırdan kurulacak", e)
        }
        bellekIndeks = harita
        return harita
    }

    @Synchronized
    private fun indeksKaydet(context: Context, harita: Map<String, List<String>>) {
        try {
            val veri = JSONObject()
            harita.forEach { (yol, sayfalar) ->
                val dizi = JSONArray()
                sayfalar.forEach { dizi.put(it) }
                veri.put(yol, dizi)
            }
            indeksDosyasi(context).writeText(
                JSONObject().put("surum", SURUM).put("veri", veri).toString()
            )
        } catch (e: Exception) {
            android.util.Log.w(TAG, "İndeks yazılamadı", e)
        }
    }

    /** Kaç ders indekslenmiş? */
    fun indekslenenSayisi(context: Context): Int = indeksYukle(context).size

    /** İndeks dosyasının diskteki boyutu (KB). */
    fun indeksBoyutu(context: Context): Long = try {
        indeksDosyasi(context).length() / 1024
    } catch (_: Exception) {
        0
    }

    /** İndeksi siler — disk boşaltmak ya da yeniden kurmak için. */
    @Synchronized
    fun indeksiSil(context: Context) {
        try {
            indeksDosyasi(context).delete()
        } catch (e: Exception) {
            android.util.Log.w(TAG, "İndeks silinemedi", e)
        }
        bellekIndeks = null
    }

    // ═══════════════════════════════════════════════════════════════
    // ARAMA
    // ═══════════════════════════════════════════════════════════════

    /** Aramayı iptal etmek için bayrak — ekran kapanınca true yapılır. */
    @Volatile
    var iptal = false

    /**
     * PDF'lerde arama yapar.
     *
     * Arka plan iş parçacığından çağrılmalıdır. İndekslenmemiş dersler
     * sırayla işlenir ve [ilerleme] geri çağrısı ile bildirilir.
     *
     * @param sorgu aranacak metin (en az 2 karakter)
     * @param sadeceIndeksli true ise yeni PDF işlenmez, sadece hazır indeks taranır
     * @param limit en fazla kaç sonuç döndürülsün
     */
    fun ara(
        context: Context,
        sorgu: String,
        sadeceIndeksli: Boolean = false,
        limit: Int = 120,
        ilerleme: ((Ilerleme) -> Unit)? = null
    ): List<Sonuc> {
        iptal = false
        val temizSorgu = sorgu.trim()
        if (temizSorgu.length < 2) return emptyList()

        val hedef = normalle(temizSorgu)
        val sonuclar = mutableListOf<Sonuc>()

        // PDF'i olan tüm dersler
        val dersler = Store.loadLessons(context).filter { it.pdfAsset.isNotBlank() }
        val kurslar = Store.loadCourses(context).associateBy { it.id }
        val indeks = indeksYukle(context)

        var yeniIndekslendi = false
        var islenen = 0

        for (ders in dersler) {
            if (iptal) break
            islenen++

            // Sayfaları al: indekste varsa oradan, yoksa çıkar
            var sayfalar = indeks[ders.pdfAsset]
            if (sayfalar == null) {
                if (sadeceIndeksli) continue
                ilerleme?.invoke(Ilerleme(islenen, dersler.size, ders.title))

                val metin = try {
                    DersMetni.metniAl(context, ders.pdfAsset)
                } catch (e: Exception) {
                    android.util.Log.w(TAG, "Metin çıkarılamadı: " + ders.pdfAsset, e)
                    null
                }
                if (metin.isNullOrBlank()) {
                    // Boş kaydet ki tekrar denenmesin
                    indeks[ders.pdfAsset] = emptyList()
                    yeniIndekslendi = true
                    continue
                }
                sayfalar = DersMetni.sayfalar(metin)
                indeks[ders.pdfAsset] = sayfalar
                yeniIndekslendi = true
            }

            // Sayfalarda ara
            val kursAdi = kurslar[ders.courseId]?.let { it.emoji + " " + it.title }.orEmpty()
            sayfalar.forEachIndexed { sayfaNo, sayfaMetni ->
                if (sonuclar.size >= limit) return@forEachIndexed
                val normalSayfa = normalle(sayfaMetni)
                var arananIndeks = normalSayfa.indexOf(hedef)
                // Aynı sayfada en fazla 2 eşleşme göster (liste şişmesin)
                var sayfaEslesme = 0
                while (arananIndeks >= 0 && sayfaEslesme < 2 && sonuclar.size < limit) {
                    val (parca, vurguBas) = parcaCikar(sayfaMetni, arananIndeks, hedef.length)
                    sonuclar.add(
                        Sonuc(
                            lessonId = ders.id,
                            dersAdi = ders.title,
                            kursAdi = kursAdi,
                            assetPath = ders.pdfAsset,
                            sayfa = sayfaNo,
                            parca = parca,
                            vurguBas = vurguBas,
                            vurguUzunluk = hedef.length
                        )
                    )
                    sayfaEslesme++
                    arananIndeks = normalSayfa.indexOf(hedef, arananIndeks + hedef.length)
                }
            }
        }

        if (yeniIndekslendi && !iptal) indeksKaydet(context, indeks)
        return sonuclar
    }

    /**
     * Eşleşmenin çevresinden okunabilir bir parça çıkarır.
     *
     * Normalleştirilmiş metinle orijinal metin **aynı uzunlukta** olduğu için
     * (karakter karaktere dönüşüm) indeksler birebir örtüşür.
     *
     * @return (parça metni, parça içindeki vurgu başlangıcı)
     */
    private fun parcaCikar(
        orijinal: String,
        eslesmeIndeks: Int,
        eslesmeUzunluk: Int
    ): Pair<String, Int> {
        val yaricap = (PARCA_UZUNLUK - eslesmeUzunluk) / 2
        var bas = (eslesmeIndeks - yaricap).coerceAtLeast(0)
        var son = (eslesmeIndeks + eslesmeUzunluk + yaricap).coerceAtMost(orijinal.length)

        // Kelime ortasından kesmemeye çalış
        if (bas > 0) {
            val bosluk = orijinal.indexOf(' ', bas)
            if (bosluk in bas until eslesmeIndeks) bas = bosluk + 1
        }
        if (son < orijinal.length) {
            val bosluk = orijinal.lastIndexOf(' ', son)
            if (bosluk > eslesmeIndeks + eslesmeUzunluk) son = bosluk
        }

        val ham = orijinal.substring(bas, son)
            .replace(Regex("\\s+"), " ")
            .trim()

        // Kırpma sonrası vurgu konumu — boşluk sadeleştirmesi kaydırabilir,
        // bu yüzden parça içinde yeniden aranır (güvenli yol).
        val parcaOnEk = if (bas > 0) "…" else ""
        val parcaSonEk = if (son < orijinal.length) "…" else ""
        val tamParca = parcaOnEk + ham + parcaSonEk

        val aranan = orijinal.substring(eslesmeIndeks, eslesmeIndeks + eslesmeUzunluk)
            .replace(Regex("\\s+"), " ")
        val vurgu = normalle(tamParca).indexOf(normalle(aranan))

        return tamParca to vurgu
    }

    // ═══════════════════════════════════════════════════════════════
    // TOPLU İNDEKSLEME
    // ═══════════════════════════════════════════════════════════════

    /**
     * Tüm PDF'leri önceden indeksler ("Tümünü indeksle" düğmesi).
     * Böylece sonraki aramalar anında sonuç verir.
     *
     * @return indekslenen ders sayısı
     */
    fun tumunuIndeksle(
        context: Context,
        ilerleme: ((Ilerleme) -> Unit)? = null
    ): Int {
        iptal = false
        val dersler = Store.loadLessons(context).filter { it.pdfAsset.isNotBlank() }
        val indeks = indeksYukle(context)
        var yeni = 0

        dersler.forEachIndexed { i, ders ->
            if (iptal) return@forEachIndexed
            if (indeks.containsKey(ders.pdfAsset)) return@forEachIndexed

            ilerleme?.invoke(Ilerleme(i + 1, dersler.size, ders.title))
            try {
                val metin = DersMetni.metniAl(context, ders.pdfAsset)
                indeks[ders.pdfAsset] =
                    if (metin.isNullOrBlank()) emptyList() else DersMetni.sayfalar(metin)
                yeni++
            } catch (e: Exception) {
                android.util.Log.w(TAG, "İndekslenemedi: " + ders.pdfAsset, e)
                indeks[ders.pdfAsset] = emptyList()
            }
            // Her 10 derste bir diske yaz — yarıda kesilirse ilerleme kaybolmasın
            if (yeni % 10 == 0) indeksKaydet(context, indeks)
        }

        indeksKaydet(context, indeks)
        return yeni
    }

    /** PDF'i olan toplam ders sayısı. */
    fun pdfliDersSayisi(context: Context): Int =
        Store.loadLessons(context).count { it.pdfAsset.isNotBlank() }
}
