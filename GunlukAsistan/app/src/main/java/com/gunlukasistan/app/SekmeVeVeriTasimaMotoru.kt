package com.gunlukasistan.app

import android.content.Context
import android.view.View
import android.view.ViewGroup
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * v11.08 — Ana Ekran, Bugün, Konular, İlerleme ve Plan Sekmeleri Arası
 * Veri / Kart Taşıma ve Otonom Sıralama Motoru (`SekmeVeVeriTasimaMotoru`).
 *
 * Kullanıcının "Ana sayfadaki en bastaki gunu vb seyleri gösteren sey küçüldü yanlardan
 * onu düzelt ve ana ekran, bugun,konular ilerleme gibi sekmelerden verileri veya komple
 * icindekileri tek tek taşıyamıyorum onu hallet." talimatı doğrultusunda:
 *
 *  1. Tüm ana sekmeler (`sira_home`, `sira_today`, `sira_topics`, `sira_progress`, `sira_plan`)
 *     için esnek sıra kaydı ve uygulama mekanizması sunar.
 *  2. Sekmeler arası verileri tek tek veya komple tüm içerik olarak taşıma / kopyalama
 *     işlemlerini JSON tabanlı kalıcı hafızada yönetir.
 *  3. Yatay küçülme sorununu önlemek için tüm kartlarda genişlik ölçeğinin (`scaleX`) daima
 *     `1.0f` tam genişlikte tutulmasını güvenceye alır.
 */
object SekmeVeVeriTasimaMotoru {

    private const val PREF_NAME = "sekme_ve_veri_tasima_v1"
    private const val KEY_TASINAN_VERILER = "tasinan_sekmeler_veri_json"

    data class TasinanVeri(
        val id: String,
        var kaynakSekme: String,
        var hedefSekme: String,
        var baslik: String,
        var icerik: String,
        val tarihMs: Long = System.currentTimeMillis()
    )

    // ══════════════════════════════════════════════════════════════
    // 1. SIRALAMA VE ÖLÇEK KORUMA FONKSİYONLARI
    // ══════════════════════════════════════════════════════════════

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun siralamaKaydet(context: Context?, sekmeAnahtari: String, idler: List<String>): Boolean {
        if (context == null) return true
        prefs(context).edit().putString("sira_$sekmeAnahtari", idler.joinToString(",")).apply()
        return true
    }

    fun siralamaGetir(context: Context?, sekmeAnahtari: String): List<String> {
        if (context == null) return emptyList()
        val s = prefs(context).getString("sira_$sekmeAnahtari", null) ?: return emptyList()
        return s.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun siraSifirla(context: Context?, sekmeAnahtari: String): Boolean {
        if (context == null) return true
        prefs(context).edit().remove("sira_$sekmeAnahtari").apply()
        return true
    }

    /**
     * ViewGroup içindeki tüm çocuk görünümleri kayıtlı ID sırasına göre dizer
     * ve yatay küçülme (side shrinking) oluşmaması için scaleX değerini 1.0f sabitler.
     */
    fun siralamayiVeBoyutuUygula(context: Context?, sekmeAnahtari: String, container: ViewGroup?) {
        if (container == null || context == null) return

        // 1) Yatay küçülmeyi önle (tüm çocuklarda scaleX = 1.0f)
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            child.scaleX = 1.0f
        }

        // 2) Sıralamayı uygula
        val kayitliIdler = siralamaGetir(context, sekmeAnahtari)
        if (kayitliIdler.isEmpty()) return

        val childMap = mutableMapOf<String, View>()
        for (i in container.childCount - 1 downTo 0) {
            val c = container.getChildAt(i)
            if (c.id != View.NO_ID) {
                childMap[c.id.toString()] = c
            }
        }

        kayitliIdler.forEach { idStr ->
            val v = childMap[idStr]
            if (v != null && v.parent == container) {
                container.removeView(v)
                container.addView(v)
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    // 2. SEKMELER ARASI VERİ / KART TAŞIMA FONKSİYONLARI
    // ══════════════════════════════════════════════════════════════

    fun tumTasinanVerileriGetir(context: Context?): List<TasinanVeri> {
        if (context == null) return emptyList()
        val jsonStr = prefs(context).getString(KEY_TASINAN_VERILER, "[]") ?: "[]"
        val list = mutableListOf<TasinanVeri>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    TasinanVeri(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        kaynakSekme = obj.optString("kaynakSekme", "home"),
                        hedefSekme = obj.optString("hedefSekme", "today"),
                        baslik = obj.optString("baslik", ""),
                        icerik = obj.optString("icerik", ""),
                        tarihMs = obj.optLong("tarihMs", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Exception) {
            return emptyList()
        }
        return list
    }

    fun tasinanVerileriKaydet(context: Context?, list: List<TasinanVeri>): Boolean {
        if (context == null) return true
        val arr = JSONArray()
        list.forEach { v ->
            val obj = JSONObject()
            obj.put("id", v.id)
            obj.put("kaynakSekme", v.kaynakSekme)
            obj.put("hedefSekme", v.hedefSekme)
            obj.put("baslik", v.baslik)
            obj.put("icerik", v.icerik)
            obj.put("tarihMs", v.tarihMs)
            arr.put(obj)
        }
        prefs(context).edit().putString(KEY_TASINAN_VERILER, arr.toString()).apply()
        return true
    }

    fun sekmeIcinTasinanVeriler(context: Context?, sekmeAnahtari: String): List<TasinanVeri> {
        return tumTasinanVerileriGetir(context).filter { it.hedefSekme == sekmeAnahtari }
    }

    /**
     * Belirli bir veriyi (veya komple bir kart içeriğini) kaynak sekmeden
     * hedef sekmeye taşır (kopyalaMi = false ise kaynak sekmeden kaldırılır).
     */
    fun veriTasiVeyaKopyala(
        context: Context?,
        kaynakSekme: String,
        hedefSekme: String,
        baslik: String,
        icerik: String,
        kopyalaMi: Boolean = false
    ): Pair<Boolean, String> {
        val yeni = TasinanVeri(
            id = "t-" + UUID.randomUUID().toString().take(8),
            kaynakSekme = kaynakSekme,
            hedefSekme = hedefSekme,
            baslik = baslik.trim().takeIf { it.isNotEmpty() } ?: "Taşınan İçerik",
            icerik = icerik.trim()
        )
        val list = tumTasinanVerileriGetir(context).toMutableList()
        list.add(0, yeni)
        tasinanVerileriKaydet(context, list)

        // 1) Kart bileşen kodunu bul:
        val kartKodu = bilesenKoduBul(baslik, icerik)
        // 2) Gerçek kartı EvrenselKartKatalogu üzerinden hedef sekmeye taşı veya kopyala!
        if (kopyalaMi) {
            EvrenselKartKatalogu.kartKopyala(context, kartKodu, hedefSekme)
        } else {
            EvrenselKartKatalogu.kartTasi(context, kartKodu, kaynakSekme, hedefSekme)
        }

        val islemAdi = if (kopyalaMi) "kopyalandı" else "taşındı"
        val hedefAd = sekmeAdGetir(hedefSekme)
        return Pair(true, "⚡ '${yeni.baslik}' başarıyla '$hedefAd' sekmesine $islemAdi!")
    }

    fun bilesenKoduBul(baslik: String, icerik: String = ""): String {
        val birlesik = "$baslik $icerik"
        return when {
            birlesik.contains("Günün Akışı") || birlesik.contains("Hero") -> "HERO_KARTI"
            birlesik.contains("Şimdi Ne Yapmalı") -> "SIMDI_NE_YAPMALI"
            birlesik.contains("Namaz") || birlesik.contains("Vakit Planı") -> "NAMAZ_KARTI"
            birlesik.contains("Görevler") || birlesik.contains("Öncelikler") -> "GOREVLER_KARTI"
            birlesik.contains("Motivasyon") || birlesik.contains("Manşet") -> "MOTIVASYON_MANSET"
            birlesik.contains("Kurs") -> "KURSLAR_KARTI"
            birlesik.contains("Modül") || birlesik.contains("İstatistik") -> "MODULLER_OZET"
            birlesik.contains("Alışkanlık") -> "ALISKANLIK_KARTI"
            birlesik.contains("Etkinlik") || birlesik.contains("Takvim") -> "ETKINLIK_KARTI"
            birlesik.contains("İpucu") -> "IPUCU_KARTI"
            birlesik.contains("Hızlı Komut") -> "HIZLI_KOMUTLAR"
            birlesik.contains("Vaktin Sözü") || birlesik.contains("Hadis") -> "DINI_SOZ_KARTI"
            else -> "GOREVLER_KARTI" // Varsayılan olarak etkileşimli bir kart
        }
    }

    fun tasinanVeriSil(context: Context?, veriId: String): Boolean {
        val list = tumTasinanVerileriGetir(context).toMutableList()
        val silindi = list.removeAll { it.id == veriId }
        if (silindi) {
            tasinanVerileriKaydet(context, list)
        }
        return silindi
    }

    fun sekmeAdGetir(anahtar: String): String {
        return when (anahtar) {
            "home" -> "🏠 Ana Sayfa"
            "today" -> "☀️ Bugün / Günün Akışı"
            "topics" -> "📚 Konular"
            "progress" -> "📊 İlerleme"
            "plan" -> "📋 Vakit Planı"
            "tasks" -> "✅ Görevler"
            "timer" -> "⏱️ Sayaç"
            else -> "📁 Diğer Sekme ($anahtar)"
        }
    }

    /**
     * Kullanıcının "ana ekran, bugun,konular ilerleme gibi sekmelerden verileri veya komple
     * icindekileri tek tek taşıma" komutunu yürüten evrensel taşıma diyaloğu.
     */
    fun sekmeArasiTasimaDiyalogu(
        context: Context?,
        kaynakSekme: String,
        baslik: String,
        icerik: String,
        onTamamlandi: (() -> Unit)? = null
    ) {
        if (context == null) return
        val hedefSekmeler = arrayOf(
            "🏠 Ana Sayfa (home)",
            "☀️ Bugün / Günün Akışı (today)",
            "📚 Konular (topics)",
            "📊 İlerleme (progress)",
            "📋 Vakit Planı (plan)"
        )
        val anahtarlar = arrayOf("home", "today", "topics", "progress", "plan")
        com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
            .setTitle("🏷️ Hangi Sekmeye Taşıyacaksınız?")
            .setItems(hedefSekmeler) { _, idx ->
                val hedefAnahtar = anahtarlar[idx]
                if (hedefAnahtar == kaynakSekme) {
                    android.widget.Toast.makeText(context, "⚠️ Zaten bu sekmedesiniz.", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    val secenekler = arrayOf(
                        "📦 Komple İçindekileri Taşı (Tüm Kartı / Bölümü Aktar)",
                        "⚡ Tek Tek Veriyi Taşı (Seçili İçerik / Maddeyi Aktar)",
                        "➕ Hedef Sekmeye Kopyala (Burada da kalsın)"
                    )
                    com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
                        .setTitle("🔀 Taşıma Yöntemi: '$baslik'")
                        .setItems(secenekler) { _, sIdx ->
                            val kopyalaMi = (sIdx == 2)
                            val (ok, msg) = veriTasiVeyaKopyala(
                                context,
                                kaynakSekme,
                                hedefAnahtar,
                                baslik,
                                icerik,
                                kopyalaMi
                            )
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                            onTamamlandi?.invoke()

                            val hedefIndeks = when (hedefAnahtar) {
                                "today" -> 2
                                "topics" -> 3
                                "progress" -> 1
                                "plan" -> 16
                                "tasks" -> 6
                                "timer" -> 4
                                else -> 0
                            }
                            val mainAct = context as? MainActivity
                            if (mainAct != null) {
                                mainAct.open(hedefIndeks)
                                aktifSekmeTasinanlariGuncelle(mainAct, hedefIndeks)
                            } else {
                                val intent = android.content.Intent(context, MainActivity::class.java).apply {
                                    putExtra("ACILIS_SEKMESI_ID", hedefIndeks)
                                    putExtra(WidgetCommon.EXTRA_OPEN_SCREEN, hedefIndeks)
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                }
                                context.startActivity(intent)
                            }
                        }
                        .show()
                }
            }
            .show()
    }

    /**
     * v11.09: Sekmeler arası taşıma işleminin ANINDA GÖRÜNMESİ için:
     * Hedef sekme açıldığında taşınan verileri anında ekrana çizer.
     */
    fun aktifSekmeTasinanlariGuncelle(
        activity: MainActivity,
        sekmeIndex: Int,
        fragment: androidx.fragment.app.Fragment? = null
    ) {
        val kok = fragment?.view ?: activity.findViewById<View>(R.id.container) ?: return
        when (sekmeIndex) {
            0 -> { // Ana Ekran (HomeFragment)
                val ebeveyn = kok.findViewById<View>(R.id.blokHero)?.parent as? ViewGroup
                sekmeTasinanVerileriCiz(activity, "home", ebeveyn)
            }
            2 -> { // Bugün (TodayFragment)
                val ebeveyn = kok.findViewById<View>(R.id.blokBugunSimdi)?.parent as? ViewGroup
                sekmeTasinanVerileriCiz(activity, "today", ebeveyn)
            }
            3 -> { // Konular (TopicsFragment)
                val recycler = kok.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recycler)
                sekmeTasinanVerileriCiz(activity, "topics", recycler?.parent as? ViewGroup)
            }
            1 -> { // İlerleme (ProgressFragment)
                val isi = kok.findViewById<View>(R.id.yilIsi)
                sekmeTasinanVerileriCiz(activity, "progress", isi?.parent as? ViewGroup)
            }
            16 -> { // Vakit Planı (PlanFragment)
                val plSlots = kok.findViewById<ViewGroup>(R.id.plSlots)
                sekmeTasinanVerileriCiz(activity, "plan", plSlots)
            }
            6 -> { // Görevler (TasksFragment)
                val r = kok.findViewById<ViewGroup>(R.id.recycler)
                sekmeTasinanVerileriCiz(activity, "tasks", r?.parent as? ViewGroup)
            }
        }
    }

    /**
     * v11.10: Bir sekmeye diğer sekmelerden taşınan GERÇEK KARTLARI çizer
     * ve ekranın A'dan Z'ye tüm çocuklarına sınırsız sürükleme & basılı tutma
     * taşıma yetkisini verir.
     */
    fun sekmeTasinanVerileriCiz(
        context: Context?,
        sekmeAnahtari: String,
        container: ViewGroup?
    ) {
        if (context == null || container == null) return

        // 1) Eski taşınan metin not kartını veya eski taşınan gerçek kartları temizle
        val silinecekler = mutableListOf<View>()
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            val tagStr = child.tag?.toString() ?: ""
            if (tagStr.startsWith("tasinan_veri_karti_") || tagStr.startsWith("gercek_tasinan_kart_")) {
                silinecekler.add(child)
            }
        }
        silinecekler.forEach { container.removeView(it) }

        // 2) Bu ekrana DİĞER SEKMELERDEN TAŞINAN GERÇEK KARTLARI ekle!
        val tasinanKartKoduListesi = EvrenselKartKatalogu.ekranaTasinanKartIdleri(context, sekmeAnahtari)
        tasinanKartKoduListesi.forEach { kartKodu ->
            val gercekKart = EvrenselKartKatalogu.gercekKartOlustur(context, kartKodu)
            if (gercekKart != null) {
                container.addView(gercekKart, 0)
            }
        }

        // 3) A'dan Z'ye sınırsız basılı tutma ve sürükleme yetkisini kur!
        EvrenselTasimaVeSuruklemeMotoru.containerIcinSurukleVeTasiKur(
            context,
            sekmeAnahtari,
            container
        )
    }

    fun testIcinSifirla(context: Context? = null) {
        if (context != null) {
            prefs(context).edit().clear().apply()
        }
    }
}
