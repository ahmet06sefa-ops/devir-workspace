package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * v11.04 — Kişisel Gelişim ve Farkındalık Merkezi Motoru (`KisiselGelisimMotoru`).
 *
 * Kullanıcının talimatı doğrultusunda 5 ana sekmenin tüm veri modelini, JSON kalıcı
 * hafızasını, grafik oranlarını ve analiz algoritmalarını yönetir:
 *
 *  1. 🗓️ Retroperspektif (Son 1 Yıl Ay Ay İnceleme): Neler Kattı, Neler Değişti, Farkındalık Puanı (1-10).
 *  2. 📜 Manifesto: Değerler listesi, kimlik tanımı, 5 yıl sonraki kariyer, sağlık, finans, sosyal, bilgelik vizyonu.
 *  3. 📊 SWOT Analizi: Güçlü Yönler, Zayıf Yönler, Fırsatlar, Tehditler matrisi ve Denge Grafiği.
 *  4. ⚡ Derin Çalışma Periyodu (Deep Work): 3-4 saatlik odak kurucusu, sevilen konu havuzu ve zamanlayıcıya gönderme.
 *  5. 🧹 Reset Günü: Oda toplama, bilgisayar düzenleme, hedefler ve dağınıklık giderme yapılacaklar listesi.
 */
object KisiselGelisimMotoru {

    private const val PREF_NAME = "kisisel_gelisim_farkindalik_v1"
    private const val KEY_RETRO = "retro_aylar_json"
    private const val KEY_MANIFESTO = "manifesto_veri_json"
    private const val KEY_SWOT = "swot_veri_json"
    private const val KEY_DERIN = "derin_calisma_json"
    private const val KEY_RESET = "reset_gunu_json"

    // ─── 1. RETROPERSPEKTİF VERİ MODELİ ───
    data class RetroAy(
        val ayNo: Int,
        val ayAd: String,
        var nelerKatti: String,
        var nelerDegisti: String,
        var farkindalikPuan: Int
    )

    // ─── 2. MANİFESTO VERİ MODELİ ───
    data class ManifestoVeri(
        var degerler: MutableList<String>,
        var kimlikTanimi: String,
        var besYilKariyer: String,
        var besYilSaglik: String,
        var besYilFinans: String,
        var besYilSosyal: String,
        var besYilBilgelik: String
    ) {
        val netlikSkoruYuzdesi: Int
            get() {
                var doluSayisi = 0
                if (degerler.isNotEmpty()) doluSayisi += 2
                if (kimlikTanimi.isNotBlank()) doluSayisi += 2
                if (besYilKariyer.isNotBlank()) doluSayisi += 2
                if (besYilSaglik.isNotBlank()) doluSayisi += 1
                if (besYilFinans.isNotBlank()) doluSayisi += 1
                if (besYilSosyal.isNotBlank()) doluSayisi += 1
                if (besYilBilgelik.isNotBlank()) doluSayisi += 1
                return (doluSayisi * 100) / 10
            }

        val netlikOzeti: String
            get() = "🧠 Manifesto Netlik Skoru: %$netlikSkoruYuzdesi — Kafadaki Karışıklık Siliniyor"
    }

    // ─── 3. SWOT ANALİZİ VERİ MODELİ ───
    data class SwotVeri(
        var gucluler: MutableList<String>,
        var zayiflar: MutableList<String>,
        var firsatlar: MutableList<String>,
        var tehditler: MutableList<String>
    ) {
        val gucluVeFirsatSayisi: Int get() = gucluler.size + firsatlar.size
        val zayifVeTehditSayisi: Int get() = zayiflar.size + tehditler.size
        val toplamMadde: Int get() = gucluVeFirsatSayisi + zayifVeTehditSayisi

        val gucluOraniYuzde: Int
            get() = if (toplamMadde == 0) 50 else (gucluVeFirsatSayisi * 100) / toplamMadde
    }

    // ─── 4. DERİN ÇALIŞMA PERİYODU VERİ MODELİ ───
    data class DerinCalismaVeri(
        var konular: MutableList<String>,
        var seciliSureDk: Int,
        var haftalikSaatler: MutableList<Int> // Pzt..Paz (7 gün)
    ) {
        val haftalikToplamSaat: Int get() = haftalikSaatler.sum()
        val haftalikOrtalamaSaat: Float get() = haftalikToplamSaat / 7.0f
    }

    // ─── 5. RESET GÜNÜ VERİ MODELİ ───
    data class ResetGorev(
        val id: String,
        val kategori: String,
        var baslik: String,
        var tamamlandi: Boolean
    )

    // ══════════════════════════════════════════════════════════════
    // 1. RETROPERSPEKTİF FONKSİYONLARI
    // ══════════════════════════════════════════════════════════════

    fun varsayilanRetroAylariGetir(): List<RetroAy> {
        val ayAdlari = listOf(
            "Eylül 2025", "Ekim 2025", "Kasım 2025", "Aralık 2025",
            "Ocak 2026", "Şubat 2026", "Mart 2026", "Nisan 2026",
            "Mayıs 2026", "Haziran 2026", "Temmuz 2026", "Ağustos 2026"
        )
        return ayAdlari.mapIndexed { index, ad ->
            val puan = 7 + (index % 3)
            val katti = "Odaklanma disiplini, yeni projeler ve zaman yönetimi becerisi kazandırdı."
            val degisti = "Gereksiz dikkat dağıtıcılardan uzaklaştım, daha planlı yaşama alıştım."
            RetroAy(index + 1, ad, katti, degisti, puan)
        }
    }

    fun retroperspektifGetir(context: Context?): List<RetroAy> {
        if (context == null) return varsayilanRetroAylariGetir()
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonStr = sp.getString(KEY_RETRO, null) ?: return varsayilanRetroAylariGetir()
        val list = mutableListOf<RetroAy>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    RetroAy(
                        ayNo = obj.optInt("ayNo", i + 1),
                        ayAd = obj.optString("ayAd", "Ay ${i + 1}"),
                        nelerKatti = obj.optString("nelerKatti", ""),
                        nelerDegisti = obj.optString("nelerDegisti", ""),
                        farkindalikPuan = obj.optInt("farkindalikPuan", 8)
                    )
                )
            }
        } catch (_: Exception) {
            return varsayilanRetroAylariGetir()
        }
        return if (list.isEmpty()) varsayilanRetroAylariGetir() else list
    }

    fun retroperspektifKaydet(context: Context?, aylar: List<RetroAy>): Boolean {
        if (context == null) return true
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        aylar.forEach { ay ->
            val obj = JSONObject()
            obj.put("ayNo", ay.ayNo)
            obj.put("ayAd", ay.ayAd)
            obj.put("nelerKatti", ay.nelerKatti)
            obj.put("nelerDegisti", ay.nelerDegisti)
            obj.put("farkindalikPuan", ay.farkindalikPuan)
            arr.put(obj)
        }
        sp.edit().putString(KEY_RETRO, arr.toString()).apply()
        return true
    }

    fun yillikFarkindalikOrtalamasi(aylar: List<RetroAy>): Float {
        if (aylar.isEmpty()) return 0f
        return aylar.map { it.farkindalikPuan }.average().toFloat()
    }

    fun retroAylikOzetMetni(aylar: List<RetroAy>): String {
        val ort = yillikFarkindalikOrtalamasi(aylar)
        val formatli = String.format("%.1f", ort)
        return "🗓️ Son 1 Yılın Ortalama Farkındalık Puanı: $formatli / 10 — Dönüşüm Yılı"
    }

    // ══════════════════════════════════════════════════════════════
    // 2. MANİFESTO FONKSİYONLARI
    // ══════════════════════════════════════════════════════════════

    fun varsayilanManifesto(): ManifestoVeri {
        return ManifestoVeri(
            degerler = mutableListOf("🌟 Özgürlük", "🚀 Sürekli Gelişim", "💎 Disiplin & Odak", "❤️ Aile & Sadakat"),
            kimlikTanimi = "Kararlı, hedeflerinden sapmayan, sorunlar karşısında çözüm üreten ve vaktini kıymetli kullanan üretken bir bireyim.",
            besYilKariyer = "Alanımda aranan, liderlik vasıflarına sahip uzman bir profesyonel olmak.",
            besYilSaglik = "Düzenli spor yapan, dinç, yüksek enerjili ve sağlam bir fizyoloji.",
            besYilFinans = "Finansal bağımsızlığı sağlamış, sürdürülebilir pasif gelir kaynakları kurmuş olmak.",
            besYilSosyal = "Güvenilir, ilham veren dostluklar ve sağlam bir aile temeli.",
            besYilBilgelik = "Kafasındaki karışıklıkları silmiş, huzurlu, okuyan ve hakikati arayan bir zihin."
        )
    }

    fun manifestoGetir(context: Context?): ManifestoVeri {
        if (context == null) return varsayilanManifesto()
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonStr = sp.getString(KEY_MANIFESTO, null) ?: return varsayilanManifesto()
        try {
            val obj = JSONObject(jsonStr)
            val dArr = obj.optJSONArray("degerler") ?: JSONArray()
            val degerList = mutableListOf<String>()
            for (i in 0 until dArr.length()) {
                degerList.add(dArr.getString(i))
            }
            return ManifestoVeri(
                degerler = if (degerList.isEmpty()) varsayilanManifesto().degerler else degerList,
                kimlikTanimi = obj.optString("kimlikTanimi", varsayilanManifesto().kimlikTanimi),
                besYilKariyer = obj.optString("besYilKariyer", varsayilanManifesto().besYilKariyer),
                besYilSaglik = obj.optString("besYilSaglik", varsayilanManifesto().besYilSaglik),
                besYilFinans = obj.optString("besYilFinans", varsayilanManifesto().besYilFinans),
                besYilSosyal = obj.optString("besYilSosyal", varsayilanManifesto().besYilSosyal),
                besYilBilgelik = obj.optString("besYilBilgelik", varsayilanManifesto().besYilBilgelik)
            )
        } catch (_: Exception) {
            return varsayilanManifesto()
        }
    }

    fun manifestoKaydet(context: Context?, veri: ManifestoVeri): Boolean {
        if (context == null) return true
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val obj = JSONObject()
        val dArr = JSONArray()
        veri.degerler.forEach { dArr.put(it) }
        obj.put("degerler", dArr)
        obj.put("kimlikTanimi", veri.kimlikTanimi)
        obj.put("besYilKariyer", veri.besYilKariyer)
        obj.put("besYilSaglik", veri.besYilSaglik)
        obj.put("besYilFinans", veri.besYilFinans)
        obj.put("besYilSosyal", veri.besYilSosyal)
        obj.put("besYilBilgelik", veri.besYilBilgelik)
        sp.edit().putString(KEY_MANIFESTO, obj.toString()).apply()
        return true
    }

    // ══════════════════════════════════════════════════════════════
    // 3. SWOT ANALİZİ FONKSİYONLARI
    // ══════════════════════════════════════════════════════════════

    fun varsayilanSwot(): SwotVeri {
        return SwotVeri(
            gucluler = mutableListOf(
                "Yüksek öğrenme ve araştırma kapasitesi",
                "Odaklanmaya karar verdiğimde saatlerce çalışabilme gücü",
                "Teknolojik gelişmeleri hızlı takip etme"
            ),
            zayiflar = mutableListOf(
                "Bazen aşırı detaylarda zaman kaybetme eğilimi",
                "Sosyal medyaya ara sıra fazla dalabilme"
            ),
            firsatlar = mutableListOf(
                "Yapay zekâ ve dijital dönüşüm alanındaki büyük kariyer açığı",
                "İnternet üzerinden dünyaya açılabilme kolaylığı"
            ),
            tehditler = mutableListOf(
                "Sürekli değişen ekonomik parametreler ve rekabet",
                "Bilgi kirliliği ve dijital dikkat dağıtıcılar"
            )
        )
    }

    fun swotGetir(context: Context?): SwotVeri {
        if (context == null) return varsayilanSwot()
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonStr = sp.getString(KEY_SWOT, null) ?: return varsayilanSwot()
        try {
            val obj = JSONObject(jsonStr)
            return SwotVeri(
                gucluler = jsonDiziStringListe(obj.optJSONArray("gucluler")),
                zayiflar = jsonDiziStringListe(obj.optJSONArray("zayiflar")),
                firsatlar = jsonDiziStringListe(obj.optJSONArray("firsatlar")),
                tehditler = jsonDiziStringListe(obj.optJSONArray("tehditler"))
            )
        } catch (_: Exception) {
            return varsayilanSwot()
        }
    }

    private fun jsonDiziStringListe(arr: JSONArray?): MutableList<String> {
        val list = mutableListOf<String>()
        if (arr == null) return list
        for (i in 0 until arr.length()) {
            list.add(arr.getString(i))
        }
        return list
    }

    fun swotKaydet(context: Context?, veri: SwotVeri): Boolean {
        if (context == null) return true
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val obj = JSONObject()
        obj.put("gucluler", JSONArray(veri.gucluler))
        obj.put("zayiflar", JSONArray(veri.zayiflar))
        obj.put("firsatlar", JSONArray(veri.firsatlar))
        obj.put("tehditler", JSONArray(veri.tehditler))
        sp.edit().putString(KEY_SWOT, obj.toString()).apply()
        return true
    }

    fun swotMaddeEkle(context: Context?, bolumKodu: String, madde: String): SwotVeri {
        val swot = swotGetir(context)
        val temiz = madde.trim()
        if (temiz.isNotEmpty()) {
            when (bolumKodu) {
                "GUCLU" -> swot.gucluler.add(temiz)
                "ZAYIF" -> swot.zayiflar.add(temiz)
                "FIRSAT" -> swot.firsatlar.add(temiz)
                "TEHDIT" -> swot.tehditler.add(temiz)
            }
            swotKaydet(context, swot)
        }
        return swot
    }

    fun swotMaddeSil(context: Context?, bolumKodu: String, index: Int): SwotVeri {
        val swot = swotGetir(context)
        when (bolumKodu) {
            "GUCLU" -> if (index in 0 until swot.gucluler.size) swot.gucluler.removeAt(index)
            "ZAYIF" -> if (index in 0 until swot.zayiflar.size) swot.zayiflar.removeAt(index)
            "FIRSAT" -> if (index in 0 until swot.firsatlar.size) swot.firsatlar.removeAt(index)
            "TEHDIT" -> if (index in 0 until swot.tehditler.size) swot.tehditler.removeAt(index)
        }
        swotKaydet(context, swot)
        return swot
    }

    // ══════════════════════════════════════════════════════════════
    // 4. DERİN ÇALIŞMA PERİYODU (3-4 SAATLİK ODAK) FONKSİYONLARI
    // ══════════════════════════════════════════════════════════════

    fun varsayilanDerinCalisma(): DerinCalismaVeri {
        return DerinCalismaVeri(
            konular = mutableListOf(
                "💻 Yazılım ve Yapay Zekâ Geliştirme",
                "🌍 Yabancı Dil & Kelime Ezberi",
                "📚 Felsefe, Tarih ve Kitap Tahlili",
                "🚀 Özel Proje & Portföy Geliştirme"
            ),
            seciliSureDk = 180, // 3 Saat Derin Çalışma
            haftalikSaatler = mutableListOf(3, 4, 3, 4, 3, 2, 4) // Pzt-Paz Toplam 23 Saat
        )
    }

    fun derinCalismaGetir(context: Context?): DerinCalismaVeri {
        if (context == null) return varsayilanDerinCalisma()
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonStr = sp.getString(KEY_DERIN, null) ?: return varsayilanDerinCalisma()
        try {
            val obj = JSONObject(jsonStr)
            val konular = jsonDiziStringListe(obj.optJSONArray("konular"))
            val sure = obj.optInt("seciliSureDk", 180)
            val saatArr = obj.optJSONArray("haftalikSaatler")
            val saatler = mutableListOf<Int>()
            if (saatArr != null) {
                for (i in 0 until saatArr.length()) {
                    saatler.add(saatArr.optInt(i, 3))
                }
            }
            return DerinCalismaVeri(
                konular = if (konular.isEmpty()) varsayilanDerinCalisma().konular else konular,
                seciliSureDk = sure,
                haftalikSaatler = if (saatler.size == 7) saatler else varsayilanDerinCalisma().haftalikSaatler
            )
        } catch (_: Exception) {
            return varsayilanDerinCalisma()
        }
    }

    fun derinCalismaKaydet(context: Context?, veri: DerinCalismaVeri): Boolean {
        if (context == null) return true
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val obj = JSONObject()
        obj.put("konular", JSONArray(veri.konular))
        obj.put("seciliSureDk", veri.seciliSureDk)
        obj.put("haftalikSaatler", JSONArray(veri.haftalikSaatler))
        sp.edit().putString(KEY_DERIN, obj.toString()).apply()
        return true
    }

    /**
     * Boş zamanı derin çalışmaya çevirmek için 3-4 saatlik odağı (180 veya 240 dk)
     * doğrudan ana zamanlayıcı (Pomodoro/Sayaç) ayarlarına yazar.
     */
    fun derinCalismayiSayacaGonder(context: Context?, konu: String, sureDk: Int): Pair<Boolean, String> {
        if (context == null) {
            return Pair(true, "✅ [Test] '$konu' konusu için ${sureDk / 60} saatlik derin çalışma sayaca aktarıldı.")
        }
        try {
            val spSayac = context.getSharedPreferences("SayacAyar", Context.MODE_PRIVATE)
            spSayac.edit()
                .putInt("odakSureDk", sureDk)
                .putString("seciliKonuBaslik", konu)
                .putBoolean("derinCalismaModu", true)
                .apply()
            return Pair(
                true,
                "⚡ '$konu' konusu için ${sureDk / 60} saatlik (${sureDk} dakika) derin çalışma periyodu sayaca yüklendi!"
            )
        } catch (e: Exception) {
            return Pair(false, "❌ Sayaca aktarırken hata: ${e.localizedMessage}")
        }
    }

    // ══════════════════════════════════════════════════════════════
    // 5. RESET GÜNÜ (HAYATI TOPARLA & DAĞINIKLIĞI GİDER) FONKSİYONLARI
    // ══════════════════════════════════════════════════════════════

    fun varsayilanResetGorevleri(): List<ResetGorev> {
        return listOf(
            ResetGorev("rs-1", "🏠 Oda Toplama", "Çalışma masasını temizle ve eşyaları yerli yerine kaldır", false),
            ResetGorev("rs-2", "🏠 Oda Toplama", "Kıyafet dolabını düzenle ve gereksiz eşyaları ayıkla", true),
            ResetGorev("rs-3", "🏠 Oda Toplama", "Odayı iyice havalandır ve aydınlatmayı gözden geçir", false),
            ResetGorev("rs-4", "💻 Bilgisayar Düzenleme", "Masaüstü ikonlarını temizle ve dosyaları klasörle", true),
            ResetGorev("rs-5", "💻 Bilgisayar Düzenleme", "İndirilenler (Downloads) ve çöp kutusunu temizle", true),
            ResetGorev("rs-6", "💻 Bilgisayar Düzenleme", "Tarayıcıdaki gereksiz sekmeleri ve eski önbelleği temizle", false),
            ResetGorev("rs-7", "🎯 Hedefler & Yapılacaklar", "Önümüzdeki haftanın 3 ana hedefini net bir şekilde yaz", true),
            ResetGorev("rs-8", "🎯 Hedefler & Yapılacaklar", "Kafandaki dağınıklıkları ve bekleyen işleri listele", false)
        )
    }

    fun resetGorevleriGetir(context: Context?): List<ResetGorev> {
        if (context == null) return varsayilanResetGorevleri()
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonStr = sp.getString(KEY_RESET, null) ?: return varsayilanResetGorevleri()
        val list = mutableListOf<ResetGorev>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ResetGorev(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        kategori = obj.optString("kategori", "🏠 Oda Toplama"),
                        baslik = obj.optString("baslik", "Görev"),
                        tamamlandi = obj.optBoolean("tamamlandi", false)
                    )
                )
            }
        } catch (_: Exception) {
            return varsayilanResetGorevleri()
        }
        return if (list.isEmpty()) varsayilanResetGorevleri() else list
    }

    fun resetGorevleriKaydet(context: Context?, list: List<ResetGorev>): Boolean {
        if (context == null) return true
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        list.forEach { g ->
            val obj = JSONObject()
            obj.put("id", g.id)
            obj.put("kategori", g.kategori)
            obj.put("baslik", g.baslik)
            obj.put("tamamlandi", g.tamamlandi)
            arr.put(obj)
        }
        sp.edit().putString(KEY_RESET, arr.toString()).apply()
        return true
    }

    fun daginiklikGidermeYuzdesi(gorevler: List<ResetGorev>): Int {
        if (gorevler.isEmpty()) return 100
        val tamamlanan = gorevler.count { it.tamamlandi }
        return (tamamlanan * 100) / gorevler.size
    }

    fun daginiklikGidermeDurumMetni(gorevler: List<ResetGorev>): String {
        val yuzde = daginiklikGidermeYuzdesi(gorevler)
        return "🧹 Hayatı Toparlama İlerlemesi: %$yuzde — Dağınıklık Ortadan Kalkıyor!"
    }

    fun gorevDurumuDegistir(context: Context?, gorevId: String): List<ResetGorev> {
        val list = resetGorevleriGetir(context).toMutableList()
        val index = list.indexOfFirst { it.id == gorevId }
        if (index >= 0) {
            list[index].tamamlandi = !list[index].tamamlandi
            resetGorevleriKaydet(context, list)
        }
        return list
    }

    fun yeniResetGoreviEkle(context: Context?, kategori: String, baslik: String): List<ResetGorev> {
        val temiz = baslik.trim()
        if (temiz.isEmpty()) return resetGorevleriGetir(context)
        val list = resetGorevleriGetir(context).toMutableList()
        list.add(
            0,
            ResetGorev(
                id = "rs-" + UUID.randomUUID().toString().take(6),
                kategori = kategori,
                baslik = temiz,
                tamamlandi = false
            )
        )
        resetGorevleriKaydet(context, list)
        return list
    }

    fun resetGorevSil(context: Context?, gorevId: String): List<ResetGorev> {
        val list = resetGorevleriGetir(context).toMutableList()
        val silindi = list.removeAll { it.id == gorevId }
        if (silindi) {
            resetGorevleriKaydet(context, list)
        }
        return list
    }

    fun testIcinSifirla(context: Context? = null) {
        if (context != null) {
            val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            sp.edit().clear().apply()
        }
    }
}
