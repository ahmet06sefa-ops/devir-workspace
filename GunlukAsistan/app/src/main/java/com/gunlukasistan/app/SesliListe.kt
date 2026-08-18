package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * v8.1 — Sesli anlatım oynatma listesi (öneri 9).
 *
 * ── Sorun ──
 * v7.82'de sesli anlatım eklendi ama tek seferlikti: bir konu okunuyor,
 * bitiyor, duruyor. "Sıradaki 5 konuyu arka arkaya dinle" yapılamıyordu.
 * Yürürken/yolda çalışma senaryosu bu yüzden işe yaramıyordu — her
 * konudan sonra telefonu çıkarıp yenisini başlatmak gerekiyordu.
 *
 * Ayrıca kaldığı yer hatırlanmıyordu: 20 dakikalık anlatımın 15.
 * dakikasında kesilirse baştan başlamak gerekiyordu.
 *
 * ── Tasarım ──
 * Bu sınıf yalnızca **sırayı** tutuyor; okuma işini mevcut
 * [SesliDersServisi] yapıyor. Servis bir metni bitirdiğinde buradan
 * sıradakini alıyor. Böylece TTS mantığı tek yerde kalıyor.
 *
 * ── Neden madde metni saklanıyor ──
 * Anlatım metni uzun (kilobaytlarca); listede saklamak yerine madde
 * başlığı tutuluyor ve okuma anında [KonuUretici.anlatimOku] ile
 * getiriliyor. Liste hafif kalıyor, anlatım güncellenirse yeni hâli
 * okunuyor.
 */
object SesliListe {

    private const val TAG = "SesliListe"
    private const val PREF = "sesli_liste_v1"
    private const val K_LISTE = "liste_json"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ═══════════════════════════════════════════════════════════════
    // MODEL
    // ═══════════════════════════════════════════════════════════════

    /**
     * Listedeki bir öğe.
     *
     * @param madde konu maddesi metni — anlatım bununla bulunur
     * @param baglam ait olduğu konu adı
     */
    data class Oge(val madde: String, val baglam: String)

    // ═══════════════════════════════════════════════════════════════
    // LİSTE
    // ═══════════════════════════════════════════════════════════════

    fun liste(context: Context): MutableList<Oge> {
        val ham = prefs(context).getString(K_LISTE, "[]") ?: "[]"
        val sonuc = mutableListOf<Oge>()
        try {
            val dizi = JSONArray(ham)
            for (i in 0 until dizi.length()) {
                val o = dizi.optJSONObject(i) ?: continue
                val m = o.optString("m")
                if (m.isNotBlank()) sonuc.add(Oge(m, o.optString("b")))
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Liste okunamadı", e)
        }
        return sonuc
    }

    private fun yaz(context: Context, ogeler: List<Oge>) {
        val dizi = JSONArray()
        ogeler.take(50).forEach { o ->
            dizi.put(JSONObject().put("m", o.madde).put("b", o.baglam))
        }
        prefs(context).edit().putString(K_LISTE, dizi.toString()).apply()
    }

    /** Sıradaki öğenin konumu. */
    fun konum(context: Context): Int = prefs(context).getInt("konum", 0)

    private fun setKonum(context: Context, i: Int) {
        prefs(context).edit().putInt("konum", i.coerceAtLeast(0)).apply()
    }

    fun sayi(context: Context): Int = liste(context).size

    fun bosMu(context: Context): Boolean = liste(context).isEmpty()

    // ═══════════════════════════════════════════════════════════════
    // KURULUM
    // ═══════════════════════════════════════════════════════════════

    /**
     * Bir konunun **anlatımı hazır** maddelerinden liste kurar.
     *
     * Anlatımı olmayan maddeler atlanıyor: sesli okuma için metin gerekli
     * ve AI üretimi burada yapılamaz (kullanıcı dinlemeye başlamışken
     * 20 saniye beklemek akışı bozar).
     *
     * @return listeye alınan öğe sayısı
     */
    fun konudanKur(context: Context, konu: Store.Topic): Int {
        val ogeler = konu.items
            .filter { KonuUretici.anlatimVarMi(context, it.text) }
            .map { Oge(it.text, konu.title) }
        yaz(context, ogeler)
        setKonum(context, 0)
        return ogeler.size
    }

    /** Programdaki (müfredat) bitmemiş adımlardan liste kurar. */
    fun programdanKur(context: Context, adet: Int = 10): Int {
        val ogeler = Mufredat.adimlar(context)
            .filter { !it.bitti && KonuUretici.anlatimVarMi(context, it.baslik) }
            .take(adet)
            .map { Oge(it.baslik, Mufredat.programAdi(context)) }
        yaz(context, ogeler)
        setKonum(context, 0)
        return ogeler.size
    }

    /** Tek öğe ekler (listenin sonuna). */
    fun ekle(context: Context, madde: String, baglam: String) {
        val mevcut = liste(context)
        if (mevcut.any { it.madde == madde }) return
        mevcut.add(Oge(madde, baglam))
        yaz(context, mevcut)
    }

    fun temizle(context: Context) {
        prefs(context).edit().remove(K_LISTE).putInt("konum", 0).apply()
    }

    // ═══════════════════════════════════════════════════════════════
    // OYNATMA
    // ═══════════════════════════════════════════════════════════════

    /** Şu an okunması gereken öğe; liste bittiyse null. */
    fun aktif(context: Context): Oge? {
        val l = liste(context)
        val i = konum(context)
        return if (i in l.indices) l[i] else null
    }

    /**
     * Sıradaki öğeye geçer.
     *
     * @return yeni aktif öğe, liste bittiyse null
     */
    fun ilerle(context: Context): Oge? {
        val l = liste(context)
        val yeni = konum(context) + 1
        setKonum(context, yeni)
        return if (yeni in l.indices) l[yeni] else null
    }

    fun geriAl(context: Context): Oge? {
        val l = liste(context)
        val yeni = (konum(context) - 1).coerceAtLeast(0)
        setKonum(context, yeni)
        return if (yeni in l.indices) l[yeni] else null
    }

    /**
     * Aktif öğenin seslendirme metnini hazırlar.
     *
     * Anlatım silinmişse (kullanıcı "yeniden üret" demiş olabilir)
     * null döner; çağıran sıradakine geçmeli.
     */
    fun aktifMetin(context: Context): String? {
        val oge = aktif(context) ?: return null
        val anlatim = KonuUretici.anlatimOku(context, oge.madde) ?: return null
        return KonuUretici.seslendirmeMetni(anlatim)
    }

    /** İlerleme özeti — "3 / 8" gibi. */
    fun ilerlemeMetni(context: Context): String {
        val toplam = sayi(context)
        if (toplam == 0) return ""
        return "${(konum(context) + 1).coerceAtMost(toplam)} / $toplam"
    }

    fun ozet(context: Context): String {
        val toplam = sayi(context)
        return if (toplam == 0) context.getString(R.string.sl_bos)
        else context.getString(R.string.sl_ozet, toplam, ilerlemeMetni(context))
    }
}
