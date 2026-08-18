package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * v7.81 — Konu maddelerini toplu üreten ve derinlemesine anlatan motor.
 *
 * ── Kullanıcının isteği ──
 * "Konu maddelerini AI ile toplu üretme ve PDF olarak uzun uzun destekleme
 *  (kitaplardan destekleme) resimlerle destekleme vb konuyu anlamam için
 *  her şeyi ekle"
 *
 * ── [AltBaslikBulucu]'dan farkı (v6.x) ──
 * O sınıf tek seferde ~10 alt başlık bulur ve orada biter. Burada:
 *   1. **Toplu üretim** — 10/25/50/100 madde, parça parça (kota dostu)
 *   2. **Müfredat kipi** — maddeler kolaydan zora sıralanır
 *   3. **Derin anlatım** — her madde için uzun ders metni ([anlat])
 *   4. **Kaynak desteği** — cihazdaki PDF'lerden alıntı ([pdfDesteği])
 *   5. **Görsel desteği** — anlatımı somutlaştıran şema tarifi + arama
 *
 * ── Neden parça parça üretim ──
 * "100 madde üret" tek istekte istenirse model yarıda kesiliyor (token
 * sınırı) ya da kalitesi düşüyor. 20'şerlik gruplar hâlinde, önceki
 * maddeler bağlam olarak verilerek isteniyor — hem tekrar olmuyor hem
 * her grup tam çıkıyor.
 */
object KonuUretici {

    private const val TAG = "KonuUretici"

    /** Tek istekte istenecek madde sayısı — daha fazlası kaliteyi düşürüyor. */
    private const val GRUP = 20

    // ═══════════════════════════════════════════════════════════════
    // ZORLUK / KAPSAM
    // ═══════════════════════════════════════════════════════════════

    const val SEVIYE_BASLANGIC = 0
    const val SEVIYE_ORTA = 1
    const val SEVIYE_ILERI = 2
    const val SEVIYE_KARISIK = 3

    fun seviyeAdi(context: Context, s: Int): String = context.getString(
        when (s) {
            SEVIYE_BASLANGIC -> R.string.ku_sv_baslangic
            SEVIYE_ORTA -> R.string.ku_sv_orta
            SEVIYE_ILERI -> R.string.ku_sv_ileri
            else -> R.string.ku_sv_karisik
        }
    )

    private fun seviyeTarifi(s: Int): String = when (s) {
        SEVIYE_BASLANGIC ->
            "BAŞLANGIÇ seviyesi. Sıfırdan başlayan biri için temel maddeler."
        SEVIYE_ORTA ->
            "ORTA seviye. Temeli bilen birinin ilerlemesi için maddeler."
        SEVIYE_ILERI ->
            "İLERİ seviye. Konuyu bilen birinin uzmanlaşması için derin maddeler."
        else ->
            "KARIŞIK. Kolaydan zora doğru sıralanmış, baştan sona öğrenme yolu."
    }

    // ═══════════════════════════════════════════════════════════════
    // 1) TOPLU MADDE ÜRETİMİ
    // ═══════════════════════════════════════════════════════════════

    class Sonuc(
        val ok: Boolean,
        val maddeler: List<String>,
        val hata: String = ""
    )

    /**
     * Konu başlığından çok sayıda alt madde üretir.
     *
     * **Ağ isteği yapar — arka planda çağır.**
     *
     * @param adet toplam istenen madde (parçalara bölünür)
     * @param seviye bkz. SEVIYE_*
     * @param mevcut zaten listede olanlar — tekrar üretilmesin
     * @param ilerleme her grup bittiğinde çağrılır (üretilen, hedef)
     */
    fun uret(
        context: Context,
        konuBasligi: String,
        adet: Int,
        seviye: Int = SEVIYE_KARISIK,
        mevcut: List<String> = emptyList(),
        ilerleme: ((Int, Int) -> Unit)? = null
    ): Sonuc {
        if (!AiSettings.isReady(context)) {
            return Sonuc(false, emptyList(), context.getString(R.string.kn_ai_hazir_degil))
        }

        val hedef = adet.coerceIn(5, 200)
        val toplananlar = mutableListOf<String>()
        val gorulen = mevcut.map { normalle(it) }.toMutableSet()
        var sonHata = ""

        // Kaç tur gerekiyor — her turda GRUP kadar iste
        var tur = 0
        val maxTur = (hedef + GRUP - 1) / GRUP + 2   // +2 tolerans (tekrarlar için)

        while (toplananlar.size < hedef && tur < maxTur) {
            tur++
            val kalan = hedef - toplananlar.size
            val isteneni = minOf(GRUP, kalan)

            val istem = uretimIstemi(
                konuBasligi = konuBasligi,
                adet = isteneni,
                seviye = seviye,
                oncekiler = (mevcut + toplananlar).takeLast(60),
                sira = toplananlar.size
            )

            val cevap = try {
                AiClient.sadeIstek(context, istem, butce = 1600)
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Üretim isteği başarısız", e)
                sonHata = e.message.orEmpty()
                break
            }

            if (!cevap.ok) {
                sonHata = cevap.text
                // İlk turda hata varsa devam etmenin anlamı yok
                if (toplananlar.isEmpty()) break else break
            }

            val yeniler = ayristir(cevap.text).filter { m ->
                val n = normalle(m)
                if (n.isBlank() || n in gorulen) false else { gorulen.add(n); true }
            }

            // Model tekrar üretmeye başladıysa durdur — boşuna kota harcama
            if (yeniler.isEmpty()) break

            toplananlar.addAll(yeniler)
            ilerleme?.invoke(toplananlar.size.coerceAtMost(hedef), hedef)
        }

        return if (toplananlar.isEmpty()) {
            Sonuc(false, emptyList(), sonHata.ifBlank {
                context.getString(R.string.ku_uretilemedi)
            })
        } else {
            Sonuc(true, toplananlar.take(hedef))
        }
    }

    private fun uretimIstemi(
        konuBasligi: String,
        adet: Int,
        seviye: Int,
        oncekiler: List<String>,
        sira: Int
    ): String {
        val oncekiBolum = if (oncekiler.isEmpty()) "" else
            "\n\nBU MADDELER ZATEN VAR — TEKRARLAMA:\n" +
                oncekiler.joinToString("\n") { "- $it" }

        val devamNotu = if (sira > 0)
            "\nBu ${sira + 1}. maddeden itibaren DEVAM listesi. Öncekilerin " +
                "üzerine, bir sonraki zorluk kademesinden devam et."
        else ""

        return """
"$konuBasligi" konusunu öğrenmek isteyen birine, bu konuyu parçalara ayıran
bir çalışma listesi hazırlıyorsun.

${seviyeTarifi(seviye)}$devamNotu$oncekiBolum

GÖREV: Tam $adet adet alt madde yaz.

KURALLAR:
1. Her madde TEK SATIR, en fazla 10 kelime.
2. Maddeler ÖĞRENME SIRASINA göre olsun (önce temel, sonra ileri).
3. Her madde tek bir şeyi kapsasın — "ve" ile iki konu birleştirme.
4. Konunun ALANINI başlıktan anla (dil, tarih, teknik, sınav, beceri...)
   ve o alana uygun maddeler üret. Kendi uzmanlık alanını varsayma.
5. Numara, tire, madde işareti KOYMA — sadece metin.
6. Genel laf etme ("konuya giriş" gibi) — somut, öğrenilebilir madde yaz.
7. Türkçe yaz.

Yanıtını SADECE şu JSON biçiminde ver:
{"maddeler": ["birinci madde", "ikinci madde", "..."]}
        """.trim()
    }

    /** Modelin cevabından madde listesini çıkarır — savunmacı. */
    fun ayristir(ham: String): List<String> {
        val temiz = ham.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()

        // 1. Yol: düzgün JSON
        runCatching {
            val bas = temiz.indexOf('{')
            val son = temiz.lastIndexOf('}')
            if (bas in 0 until son) {
                val o = JSONObject(temiz.substring(bas, son + 1))
                val dizi = o.optJSONArray("maddeler")
                if (dizi != null) {
                    val liste = mutableListOf<String>()
                    for (i in 0 until dizi.length()) {
                        dizi.optString(i).trim().takeIf { it.isNotBlank() }
                            ?.let { liste.add(kirp(it)) }
                    }
                    if (liste.isNotEmpty()) return liste
                }
            }
        }

        // 2. Yol: çıplak JSON dizisi
        runCatching {
            val bas = temiz.indexOf('[')
            val son = temiz.lastIndexOf(']')
            if (bas in 0 until son) {
                val dizi = JSONArray(temiz.substring(bas, son + 1))
                val liste = mutableListOf<String>()
                for (i in 0 until dizi.length()) {
                    dizi.optString(i).trim().takeIf { it.isNotBlank() }
                        ?.let { liste.add(kirp(it)) }
                }
                if (liste.isNotEmpty()) return liste
            }
        }

        // 3. Yol: satır satır metin (model JSON vermediyse)
        return temiz.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && it.length in 3..120 }
            .filterNot { it.startsWith("{") || it.startsWith("}") || it.startsWith("[") }
            .map { kirp(it) }
            .filter { it.isNotBlank() }
            .take(GRUP)
    }

    /** Baştaki numara/tire işaretlerini temizler. */
    private fun kirp(ham: String): String = ham
        .replace(Regex("^\\s*[-•*–]\\s*"), "")
        .replace(Regex("^\\s*\\d+[.)]\\s*"), "")
        .replace(Regex("^\"|\"$"), "")
        .trim()
        .take(150)

    private fun normalle(s: String): String = s.lowercase()
        .replace("ı", "i").replace("ş", "s").replace("ğ", "g")
        .replace("ü", "u").replace("ö", "o").replace("ç", "c")
        .replace(Regex("[^a-z0-9]"), "")

    // ═══════════════════════════════════════════════════════════════
    // 2) DERİNLEMESİNE ANLATIM
    // ═══════════════════════════════════════════════════════════════

    /**
     * Bir maddenin uzun ders anlatımı.
     *
     * @param bolumler ["Tanım", "Nasıl çalışır", ...] başlıklı bölümler
     * @param gorseller anlatımı destekleyen görsel tarifleri
     * @param kaynakAlintilari cihazdaki PDF'lerden bulunan ilgili parçalar
     */
    class Anlatim(
        val ok: Boolean,
        val baslik: String = "",
        val bolumler: List<Bolum> = emptyList(),
        val gorseller: List<GorselFikri> = emptyList(),
        val kaynakAlintilari: List<PdfArama.Sonuc> = emptyList(),
        val ozet: String = "",
        val hata: String = ""
    ) {
        /** Düz metin hâli — PDF ve paylaşım için. */
        fun duzMetin(): String {
            val sb = StringBuilder()
            sb.append(baslik).append("\n\n")
            bolumler.forEach { b ->
                sb.append("── ").append(b.baslik).append(" ──\n")
                sb.append(b.metin).append("\n\n")
            }
            if (ozet.isNotBlank()) sb.append("── ÖZET ──\n").append(ozet).append("\n")
            return sb.toString()
        }
    }

    class Bolum(val baslik: String, val metin: String)

    /**
     * Görsel önerisi.
     *
     * Model resim üretemez ama **ne tür bir görselin yardımcı olacağını**
     * tarif edebilir. Bu tarif hem kullanıcıya gösterilir hem de görsel
     * arama sorgusu olarak kullanılır ([aramaSorgusu]).
     */
    class GorselFikri(
        val aciklama: String,
        val aramaSorgusu: String
    )

    /**
     * Bir konu maddesini derinlemesine anlatır.
     *
     * **Ağ isteği + disk okuma yapar — arka planda çağır.**
     *
     * @param uzunluk 0 kısa · 1 normal · 2 uzun (varsayılan uzun)
     * @param pdfDestegi cihazdaki ders PDF'lerinde alıntı aransın mı
     */
    fun anlat(
        context: Context,
        madde: String,
        konuBasligi: String = "",
        uzunluk: Int = 2,
        pdfDestegi: Boolean = true
    ): Anlatim {
        if (!AiSettings.isReady(context)) {
            return Anlatim(false, hata = context.getString(R.string.kn_ai_hazir_degil))
        }

        // 1. Cihazdaki PDF'lerden ilgili parçaları bul (varsa)
        val alintilar = if (pdfDestegi) pdfAlintilariBul(context, madde) else emptyList()

        // 2. Modelden anlatım iste — alıntılar bağlam olarak verilir
        val istem = anlatimIstemi(madde, konuBasligi, uzunluk, alintilar)
        val butce = when (uzunluk) {
            0 -> 1200
            1 -> 2400
            else -> 4000
        }

        val cevap = try {
            AiClient.sadeIstek(context, istem, butce)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Anlatım isteği başarısız", e)
            return Anlatim(false, hata = e.message.orEmpty())
        }

        if (!cevap.ok) return Anlatim(false, hata = cevap.text)

        return anlatimAyristir(cevap.text, madde, alintilar)
    }

    /**
     * Cihazdaki ders PDF'lerinde bu maddeyle ilgili parçaları arar.
     *
     * "Kitaplardan destekleme" isteğinin karşılığı: uydurma kaynak
     * göstermek yerine **kullanıcının kendi PDF'lerinden** alıntı yapılır.
     */
    private fun pdfAlintilariBul(context: Context, madde: String): List<PdfArama.Sonuc> = try {
        // Sadece indekslenmiş PDF'lerde ara — yeni indeksleme uzun sürer,
        // anlatımı bekletmesin. Kullanıcı Kaynaklar ekranından indeksleyebilir.
        PdfArama.ara(context, madde, sadeceIndeksli = true, limit = 4)
    } catch (e: Exception) {
        android.util.Log.w(TAG, "PDF alıntısı bulunamadı", e)
        emptyList()
    }

    private fun anlatimIstemi(
        madde: String,
        konuBasligi: String,
        uzunluk: Int,
        alintilar: List<PdfArama.Sonuc>
    ): String {
        val uzunlukTarifi = when (uzunluk) {
            0 -> "KISA: 2-3 bölüm, her bölüm 1 paragraf."
            1 -> "NORMAL: 3-4 bölüm, her bölüm 2 paragraf."
            else -> "UZUN VE DETAYLI: 5-7 bölüm, her bölüm 2-4 paragraf. " +
                "Konuyu gerçekten öğretecek derinlikte yaz, yüzeysel geçme."
        }

        val baglamBolumu = if (konuBasligi.isBlank()) "" else
            "\nBu madde \"$konuBasligi\" konusunun bir parçası."

        val kaynakBolumu = if (alintilar.isEmpty()) "" else buildString {
            append("\n\n=== KULLANICININ KENDİ KAYNAKLARINDAN ALINTILAR ===\n")
            alintilar.forEachIndexed { i, a ->
                append("[Kaynak ${i + 1}: ${a.dersAdi}, sayfa ${a.sayfa + 1}]\n")
                append(a.parca.take(600)).append("\n\n")
            }
            append("=== ALINTI SONU ===\n")
            append("Anlatımında bu kaynaklara ÖNCELİK ver ve tutarlı ol. ")
            append("İlgili yerde \"(kaynağındaki X dersi, s.Y)\" diye atıf yap.")
        }

        return """
Sen bir öğretmensin. Öğrencine "$madde" konusunu öğreteceksin.$baglamBolumu$kaynakBolumu

UZUNLUK: $uzunlukTarifi

KURALLAR:
1. Konunun ALANINI başlıktan anla (dil, tarih, teknik, sınav, beceri...)
   ve o alanın uzmanı gibi anlat. Kendi uzmanlık alanını varsayma.
2. Öğretici ol — sohbet etme, DERS ANLAT.
3. Somut örnek ver. Alan neyse ona uygun örnek seç.
4. Zor terimleri ilk geçtiğinde parantez içinde açıkla.
5. Sayı, tarih, standart UYDURMA. Emin değilsen "yaklaşık" de.
6. Türkçe yaz.

Yanıtını SADECE şu JSON biçiminde ver, başka hiçbir şey yazma:
{
  "baslik": "konunun tam başlığı",
  "bolumler": [
    {"baslik": "bölüm adı", "metin": "bölüm metni, paragraflar \\n\\n ile ayrılsın"}
  ],
  "gorseller": [
    {"aciklama": "bu konuyu anlatan nasıl bir görsel yardımcı olur",
     "arama": "görseli bulmak için 2-4 kelimelik arama terimi"}
  ],
  "ozet": "3-5 maddelik özet, her madde yeni satırda, başında • olsun"
}

"gorseller" en fazla 3 tane olsun ve GERÇEKTEN yardımcı olacak türde olsun.
        """.trim()
    }

    private fun anlatimAyristir(
        ham: String,
        madde: String,
        alintilar: List<PdfArama.Sonuc>
    ): Anlatim {
        val temiz = ham.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()

        try {
            val bas = temiz.indexOf('{')
            val son = temiz.lastIndexOf('}')
            if (bas in 0 until son) {
                val o = JSONObject(temiz.substring(bas, son + 1))

                val bolumler = mutableListOf<Bolum>()
                o.optJSONArray("bolumler")?.let { d ->
                    for (i in 0 until d.length()) {
                        val b = d.optJSONObject(i) ?: continue
                        val bBaslik = b.optString("baslik").trim()
                        val bMetin = b.optString("metin").trim()
                        if (bMetin.isNotBlank()) {
                            bolumler.add(Bolum(bBaslik.ifBlank { "—" }, bMetin))
                        }
                    }
                }

                val gorseller = mutableListOf<GorselFikri>()
                o.optJSONArray("gorseller")?.let { d ->
                    for (i in 0 until d.length()) {
                        val g = d.optJSONObject(i) ?: continue
                        val aciklama = g.optString("aciklama").trim()
                        val arama = g.optString("arama").trim()
                        if (aciklama.isNotBlank()) {
                            gorseller.add(
                                GorselFikri(aciklama, arama.ifBlank { madde })
                            )
                        }
                    }
                }

                if (bolumler.isNotEmpty()) {
                    return Anlatim(
                        ok = true,
                        baslik = o.optString("baslik").trim().ifBlank { madde },
                        bolumler = bolumler,
                        gorseller = gorseller.take(3),
                        kaynakAlintilari = alintilar,
                        ozet = o.optString("ozet").trim()
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Anlatım ayrıştırılamadı", e)
        }

        // JSON bozuksa ham metni tek bölüm yap — kullanıcı yine de okusun
        val duz = temiz.replace(Regex("[{}\\[\\]]"), "").trim()
        return if (duz.length > 40) {
            Anlatim(
                ok = true,
                baslik = madde,
                bolumler = listOf(Bolum("", duz)),
                kaynakAlintilari = alintilar
            )
        } else {
            Anlatim(false, hata = "Anlatım üretilemedi")
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 3) ANLATIM ÖNBELLEĞİ
    // ═══════════════════════════════════════════════════════════════

    private const val PREF = "konu_anlatim_v1"

    /**
     * Üretilen anlatımı saklar — her açılışta yeniden üretmek
     * hem yavaş hem kota israfı.
     */
    fun anlatimKaydet(context: Context, madde: String, anlatim: Anlatim) {
        try {
            val o = JSONObject()
                .put("baslik", anlatim.baslik)
                .put("ozet", anlatim.ozet)
                .put("zaman", System.currentTimeMillis())
            val bolumler = JSONArray()
            anlatim.bolumler.forEach { b ->
                bolumler.put(JSONObject().put("b", b.baslik).put("m", b.metin))
            }
            o.put("bolumler", bolumler)
            val gorseller = JSONArray()
            anlatim.gorseller.forEach { g ->
                gorseller.put(JSONObject().put("a", g.aciklama).put("q", g.aramaSorgusu))
            }
            o.put("gorseller", gorseller)

            context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit().putString(anahtar(madde), o.toString()).apply()
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Anlatım kaydedilemedi", e)
        }
    }

    fun anlatimOku(context: Context, madde: String): Anlatim? {
        val ham = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(anahtar(madde), null) ?: return null
        return try {
            val o = JSONObject(ham)
            val bolumler = mutableListOf<Bolum>()
            o.optJSONArray("bolumler")?.let { d ->
                for (i in 0 until d.length()) {
                    val b = d.optJSONObject(i) ?: continue
                    bolumler.add(Bolum(b.optString("b"), b.optString("m")))
                }
            }
            val gorseller = mutableListOf<GorselFikri>()
            o.optJSONArray("gorseller")?.let { d ->
                for (i in 0 until d.length()) {
                    val g = d.optJSONObject(i) ?: continue
                    gorseller.add(GorselFikri(g.optString("a"), g.optString("q")))
                }
            }
            if (bolumler.isEmpty()) null
            else Anlatim(
                ok = true,
                baslik = o.optString("baslik"),
                bolumler = bolumler,
                gorseller = gorseller,
                ozet = o.optString("ozet")
            )
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Anlatım okunamadı", e)
            null
        }
    }

    fun anlatimVarMi(context: Context, madde: String): Boolean =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .contains(anahtar(madde))

    fun anlatimSil(context: Context, madde: String) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().remove(anahtar(madde)).apply()
    }

    fun onbellekSayisi(context: Context): Int =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).all.size

    fun onbellegiTemizle(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun anahtar(madde: String): String = "a_" + normalle(madde).take(60)

    // ═══════════════════════════════════════════════════════════════
    // 4) ANLATIMDAN QUIZ ÜRETİMİ  (v7.82)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Konu maddesi için sanal ders kimliği.
     *
     * [QuizStore] soruları `lessonId` ile gruplar; konu maddelerinin ise
     * gerçek bir `Store.Lesson` kaydı yok. Madde metninden **kararlı ve
     * negatif** bir kimlik türetiliyor:
     *   · Kararlı → aynı madde her zaman aynı kimliği alır, sorular kaybolmaz
     *   · Negatif → gerçek ders kimlikleri (System.currentTimeMillis tabanlı,
     *     hep pozitif) ile asla çakışmaz
     */
    fun sanalDersId(madde: String): Long {
        var h = 1125899906842597L   // büyük asal — çakışmayı azaltır
        normalle(madde).take(80).forEach { c -> h = 31 * h + c.code }
        return -(kotlin.math.abs(h) % 1_000_000_000_000L + 1)
    }

    class QuizSonucu(
        val ok: Boolean,
        val sorular: List<QuizStore.Soru> = emptyList(),
        val hata: String = ""
    )

    /**
     * Üretilmiş anlatımdan sınav soruları çıkarır.
     *
     * ── Neden [QuizUretici] kullanılmadı ──
     * O sınıf `Store.Lesson` alıyor ve soruları dersin **başlığı + PDF'i**
     * üzerinden üretiyor. Burada elimizde çok daha iyi bir kaynak var:
     * az önce üretilmiş **anlatım metninin kendisi**. Sorular birebir
     * okunan metinden çıkınca "anlatımda geçmeyen şey sorulmuş" durumu
     * ortadan kalkıyor.
     *
     * **Ağ isteği yapar — arka planda çağır.**
     */
    fun quizUret(
        context: Context,
        madde: String,
        anlatim: Anlatim,
        adet: Int = 5
    ): QuizSonucu {
        if (!AiSettings.isReady(context)) {
            return QuizSonucu(false, hata = context.getString(R.string.kn_ai_hazir_degil))
        }

        val govde = anlatim.duzMetin().take(9000)
        if (govde.length < 100) {
            return QuizSonucu(false, hata = context.getString(R.string.kq_anlatim_kisa))
        }

        val istem = """
Aşağıda bir öğrenciye anlatılmış ders metni var. Bu metni gerçekten
anlayıp anlamadığını ölçen ${adet.coerceIn(3, 12)} adet çoktan seçmeli soru hazırla.

=== DERS METNİ ===
$govde
=== METİN SONU ===

KURALLAR:
1. Sorular SADECE yukarıdaki metinden çıkarılsın. Metinde olmayan bilgi sorma.
2. Ezber değil ANLAMA ölçsün ("hangisi doğrudur", "neden", "hangi durumda").
3. Her soruda tam 4 şık olsun; biri kesin doğru, diğerleri mantıklı ama yanlış.
4. Şıklar birbirine yakın uzunlukta olsun (uzun şık doğru ipucu vermesin).
5. "aciklama" alanında doğru cevabın NEDEN doğru olduğunu 1-2 cümleyle yaz.
6. Türkçe yaz.

Yanıtını SADECE şu JSON biçiminde ver:
{"sorular":[{"soru":"...","siklar":["A","B","C","D"],"dogru":0,"aciklama":"..."}]}
        """.trim()

        val cevap = try {
            AiClient.sadeIstek(context, istem, butce = 2600)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Quiz isteği başarısız", e)
            return QuizSonucu(false, hata = e.message.orEmpty())
        }
        if (!cevap.ok) return QuizSonucu(false, hata = cevap.text)

        val sorular = quizAyristir(cevap.text, sanalDersId(madde))
        return if (sorular.isEmpty()) {
            QuizSonucu(false, hata = context.getString(R.string.quiz_err_parse))
        } else {
            // v8.0: eskiyi silme, havuza EKLE (öneri 8).
            // Böylece her üretimde soru havuzu büyüyor ve sınavda
            // farklı sorular çıkıyor.
            QuizStore.havuzaEkle(context, sanalDersId(madde), sorular)
            QuizSonucu(true, sorular)
        }
    }

    /** Model çıktısını [QuizStore.Soru] listesine çevirir — savunmacı. */
    private fun quizAyristir(ham: String, lessonId: Long): List<QuizStore.Soru> {
        val temiz = ham.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()

        val sonuc = mutableListOf<QuizStore.Soru>()
        try {
            val bas = temiz.indexOf('{')
            val son = temiz.lastIndexOf('}')
            if (bas !in 0 until son) return emptyList()

            val dizi = JSONObject(temiz.substring(bas, son + 1))
                .optJSONArray("sorular") ?: return emptyList()

            for (i in 0 until dizi.length()) {
                val o = dizi.optJSONObject(i) ?: continue
                val metin = o.optString("soru").trim()
                val sikDizi = o.optJSONArray("siklar") ?: continue
                val siklar = mutableListOf<String>()
                for (j in 0 until sikDizi.length()) {
                    sikDizi.optString(j).trim().takeIf { it.isNotBlank() }?.let { siklar.add(it) }
                }
                val dogru = o.optInt("dogru", -1)
                if (metin.isBlank() || siklar.size < 2 || dogru !in siklar.indices) continue

                sonuc.add(
                    QuizStore.Soru(
                        id = System.currentTimeMillis() + i,
                        lessonId = lessonId,
                        metin = metin,
                        siklar = siklar,
                        dogru = dogru,
                        aciklama = o.optString("aciklama").trim()
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Quiz ayrıştırılamadı", e)
        }
        return sonuc
    }

    /** Bu madde için daha önce soru üretilmiş mi. */
    fun quizVarMi(context: Context, madde: String): Boolean =
        QuizStore.soruVarMi(context, sanalDersId(madde))

    /**
     * Sesli okuma için anlatımı düz metne çevirir.
     *
     * Başlıklar cümle sonu noktalamasıyla ayrılır; yoksa TTS motoru
     * başlık ile paragrafı tek cümle gibi okuyup anlaşılmaz hâle getiriyor.
     */
    fun seslendirmeMetni(anlatim: Anlatim): String {
        val sb = StringBuilder()
        sb.append(anlatim.baslik).append(". \n\n")
        anlatim.bolumler.forEach { b ->
            if (b.baslik.isNotBlank() && b.baslik != "—") {
                sb.append(b.baslik).append(". \n")
            }
            sb.append(b.metin.replace(Regex("\\s+"), " ").trim()).append("\n\n")
        }
        if (anlatim.ozet.isNotBlank()) {
            sb.append("Özet. \n")
            // Madde işaretleri sesli okumada "nokta nokta" diye okunuyor
            sb.append(anlatim.ozet.replace("•", ". ").replace(Regex("\\s+"), " ").trim())
        }
        return sb.toString()
    }
}
