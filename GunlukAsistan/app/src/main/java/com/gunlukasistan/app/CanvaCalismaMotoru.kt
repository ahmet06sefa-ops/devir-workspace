package com.gunlukasistan.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * v11.11 — Canva Çalışma Ekranı ve 10 Uygulama Arayüzü Motoru (`CanvaCalismaMotoru`).
 *
 * Kullanıcının "bana 10 adet farklı uygulamayı canva ekranı gibi çalışma erkanı oluşturmmanu
 * istiyoırum aç kapa özellğiğ ekleyeyim önerTekrwr dene" talimatı doğrultusunda:
 *
 *  1. Uygulamadaki en kritik 10 mini-uygulama / modülü tek bir görsel çalışma ekranında toplar.
 *  2. Her modülün Açık / Kapalı (`acik`) durumunu SharedPreferences üzerinde kalıcı olarak saklar.
 *  3. "💡 Akıllı Öneri / Öner" fonksiyonuyla günün saatine ve iş temposuna en uygun 3-4 modülü
 *     otomatik olarak AÇIK hale getirir.
 *  4. "🔄 Tekrar Dene / Karıştır" fonksiyonuyla yepyeni alternatif yaratıcı çalışma kombinasyonları sunar.
 */
object CanvaCalismaMotoru {

    private const val PREF_NAME = "canva_calisma_ekrani_v1"
    private const val KEY_MODULLER = "canva_moduller_json"

    data class CanvaModul(
        val kod: String,
        val ad: String,
        val simge: String,
        val aciklama: String,
        var acik: Boolean,
        val renkHex: String
    )

    private val VARSAYILAN_MODULLER = listOf(
        CanvaModul("CANVA_POMODORO", "⏱️ Çalışma Zamanı & Pomodoro Sayacı", "⏱️", "Odak oturumları ve geri sayım", true, "#6200EE"),
        CanvaModul("CANVA_GOREVLER", "✅ Görevler ve Günlük Öncelikler", "✅", "En öncelikli bekleyen görevler", true, "#2E7D32"),
        CanvaModul("CANVA_NAMAZ", "🕌 Vakit Planı & Sıradaki Namaz", "🕌", "Sıradaki vakit ve vaktin sözü", true, "#1565C0"),
        CanvaModul("CANVA_BUGUN", "☀️ Günün Akışı & Şimdi Ne Yapmalı?", "☀️", "Yapay zekâ destekli güncel adım", true, "#E65100"),
        CanvaModul("CANVA_KURSLAR", "🎓 Mühendislik & Atölye Kursları", "🏗", "Mühendislik dersleri ve atölye", false, "#6A1B9A"),
        CanvaModul("CANVA_ISTATISTIK", "📊 İlerleme ve Verimlilik Karnesi", "📊", "Günlük seri ve istatistik rozetleri", false, "#00838F"),
        CanvaModul("CANVA_KISISEL", "🌱 Kişisel Gelişim ve Farkındalık", "🌱", "Retroperspektif ve manifesto vizyonu", false, "#283593"),
        CanvaModul("CANVA_YOUTUBE", "📺 YouTube Çevrimdışı Oynatma Listesi", "📺", "Ders videoları ve çevrimdışı oynatma", false, "#C62828"),
        CanvaModul("CANVA_GORUNUM", "🎨 Evrensel Görünüm ve Arayüz", "🎨", "10.000-Madde UI/UX özelleştirme", false, "#AD1457"),
        CanvaModul("CANVA_INOVASYON", "⚡ 10.000-Madde İnovasyon & Komutlar", "⚡", "Hızlı otonom komut ve kataloğu", false, "#4E342E")
    )

    fun varsayilanModulleriGetir(): List<CanvaModul> = VARSAYILAN_MODULLER

    fun tumModulleriGetir(context: Context?): List<CanvaModul> {
        if (context == null) return VARSAYILAN_MODULLER.map { it.copy() }
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonStr = sp.getString(KEY_MODULLER, null) ?: return VARSAYILAN_MODULLER.map { it.copy() }
        val list = mutableListOf<CanvaModul>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val kod = obj.optString("kod", "")
                val varsayilan = VARSAYILAN_MODULLER.find { it.kod == kod }
                if (varsayilan != null) {
                    list.add(
                        varsayilan.copy(
                            acik = obj.optBoolean("acik", varsayilan.acik)
                        )
                    )
                }
            }
            // Katalogta olup JSON'da olmayan yeni modül varsa ekle
            VARSAYILAN_MODULLER.forEach { varMod ->
                if (list.none { it.kod == varMod.kod }) {
                    list.add(varMod.copy())
                }
            }
        } catch (_: Exception) {
            return VARSAYILAN_MODULLER.map { it.copy() }
        }
        return list
    }

    fun modulleriKaydet(context: Context?, list: List<CanvaModul>): Boolean {
        if (context == null) return true
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        list.forEach { m ->
            val obj = JSONObject()
            obj.put("kod", m.kod)
            obj.put("acik", m.acik)
            arr.put(obj)
        }
        sp.edit().putString(KEY_MODULLER, arr.toString()).apply()
        return true
    }

    fun modulDurumuDegistir(context: Context?, kod: String, acik: Boolean): List<CanvaModul> {
        val list = tumModulleriGetir(context)
        val idx = list.indexOfFirst { it.kod == kod }
        if (idx >= 0) {
            list[idx].acik = acik
            modulleriKaydet(context, list)
        }
        return list
    }

    /**
     * "💡 Akıllı Öneri / Öner" — Günün saatine göre en verimli çalışma ekranı
     * kombinasyonunu seçer ve AÇIK yapar.
     */
    fun akilliOneriUygula(context: Context?): Pair<String, List<CanvaModul>> {
        val list = tumModulleriGetir(context)
        val saat = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)

        val onerilenKodlar = when (saat) {
            in 5..11 -> listOf("CANVA_POMODORO", "CANVA_GOREVLER", "CANVA_NAMAZ", "CANVA_BUGUN") // Sabah Odak
            in 12..17 -> listOf("CANVA_POMODORO", "CANVA_KURSLAR", "CANVA_YOUTUBE", "CANVA_GOREVLER") // Öğle Odak
            in 18..21 -> listOf("CANVA_ISTATISTIK", "CANVA_GOREVLER", "CANVA_KISISEL", "CANVA_NAMAZ") // Akşam Değerlendirme
            else -> listOf("CANVA_KISISEL", "CANVA_NAMAZ", "CANVA_INOVASYON", "CANVA_POMODORO") // Gece Planı
        }

        list.forEach { m ->
            m.acik = m.kod in onerilenKodlar
        }
        modulleriKaydet(context, list)

        val donemAdi = when (saat) {
            in 5..11 -> "Sabah Üretkenliği & Güne Başlangıç"
            in 12..17 -> "Öğle Odak & Atölye Çalışması"
            in 18..21 -> "Akşam Değerlendirmesi & İstatistikler"
            else -> "Gece Sükûneti & Kişisel Gelişim"
        }
        return Pair("💡 Akıllı Öneri Uygulandı: $donemAdi (${onerilenKodlar.size} uygulama açık)", list)
    }

    /**
     * "🔄 Tekrar Dene / Karıştır" — Rastgele 4 adet farklı modülü AÇIK hale getirerek
     * alternatif yaratıcı Canva çalışma ekranları oluşturur.
     */
    fun tekrarDeneKaristir(context: Context?): Pair<String, List<CanvaModul>> {
        val list = tumModulleriGetir(context)
        val rastgeleKodlar = list.shuffled().take(4).map { it.kod }.toSet()

        list.forEach { m ->
            m.acik = m.kod in rastgeleKodlar
        }
        modulleriKaydet(context, list)

        val acikIsimler = list.filter { it.acik }.joinToString(", ") { it.ad.substringBefore(" ") }
        return Pair("🔄 Alternatif Çalışma Ekranı Kuruldu: 4 yeni uygulama açık ($acikIsimler)", list)
    }

    fun tumunuAc(context: Context?): List<CanvaModul> {
        val list = tumModulleriGetir(context)
        list.forEach { it.acik = true }
        modulleriKaydet(context, list)
        return list
    }

    fun tumunuKapat(context: Context?): List<CanvaModul> {
        val list = tumModulleriGetir(context)
        list.forEach { it.acik = false }
        modulleriKaydet(context, list)
        return list
    }

    fun testIcinSifirla(context: Context? = null) {
        if (context != null) {
            val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            sp.edit().clear().apply()
        }
    }
}
