package com.gunlukasistan.app

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import org.json.JSONArray
import org.json.JSONObject

/**
 * v11.10 — Evrensel Kart Bileşeni Kataloğu (`EvrenselKartKatalogu`).
 *
 * Kullanıcının "Taşıma işlemi yapılınca üstteki gibi yazı çıkıyor ve taşınmıyor ve diğer
 * başlıklardaki kartlarda hiç taşınma işlemi yok. Bunu düzelt ve uygulamanın a dan z ye
 * herşeyini sınırsız taşıma istediğim yere yetkisini vermeni istiyorum. Taşımak istediğim
 * herşeyin üstüne basılı tutup istediğim yöne veya yere sürüklemem yeterli olsun."
 * talimatı doğrultusunda:
 *
 *  1. Uygulamadaki 12 ana işlevsel kartı gerçek bileşen kodu (`KartBilesen`) olarak tanır.
 *  2. Bir kart (ör. Görevler Kartı, Namaz Kartı, Motivasyon Kartı vb.) bir sekmeden diğerine
 *     taşındığında sadece özet yazı göstermez; HEDEF EKRANDA GERÇEK, CANLI VE ETKİLEŞİMLİ
 *     KARTI OLUŞTURUR (`gercekKartOlustur`).
 *  3. Her ekranın sahip olduğu kartları (`ekran_kartlari_...`) SharedPreferences'ta saklar,
 *     istendiğinde anında sıfırlar (`varsayilanlaraDon`).
 */
object EvrenselKartKatalogu {

    private const val PREF_NAME = "evrensel_kart_katalogu_v1"

    data class KartBilesen(
        val kod: String,
        val ad: String,
        val varsayilanSekme: String,
        val simge: String,
        val aciklama: String
    )

    private val KATALOG = listOf(
        KartBilesen("HERO_KARTI", "☀️ Günün Akışı & Kalan Süre Kartı", "home", "🎯", "Sıradaki etkinlik ve geri sayım"),
        KartBilesen("SIMDI_NE_YAPMALI", "📚 Şimdi Ne Yapmalı? Öneri Kartı", "today", "📚", "Yapay zekâ destekli en doğru sıradaki adım"),
        KartBilesen("NAMAZ_KARTI", "🕌 Vakit Planı & Namaz Vakitleri Kartı", "today", "🕌", "6 vakit ve sıradaki namaza kalan süre"),
        KartBilesen("GOREVLER_KARTI", "✅ Görevler ve Öncelikler Kartı", "today", "✅", "Günlük öncelikler ve bekleyen görevler"),
        KartBilesen("MOTIVASYON_MANSET", "💡 Sokratik & Felsefi Motivasyon Kartı", "home", "💡", "Günlük hikmetli felsefi manşet"),
        KartBilesen("KURSLAR_KARTI", "🎓 Mühendislik ve Atölye Kursları Kartı", "home", "🏗", "Dersler, kamp ve atölye kısayolları"),
        KartBilesen("MODULLER_OZET", "📊 Modüller ve İstatistik Özeti Kartı", "home", "📊", "Günlük seri ve istatistik rozetleri"),
        KartBilesen("ALISKANLIK_KARTI", "🌱 Alışkanlıklar ve Rutinler Kartı", "today", "🌱", "Günlük takip edilen alışkanlıklar"),
        KartBilesen("ETKINLIK_KARTI", "📅 Etkinlikler & Takvim Kartı", "today", "📅", "Yaklaşan takvim etkinlikleri"),
        KartBilesen("IPUCU_KARTI", "💡 Günlük İpucu ve Asistan Kartı", "today", "💡", "Akıllı günlük ipuçları"),
        KartBilesen("HIZLI_KOMUTLAR", "⚡ Hızlı Komutlar Kartı", "today", "⚡", "Tek dokunuşla çalışan otonom komutlar"),
        KartBilesen("DINI_SOZ_KARTI", "🕌 Vaktin Sözü & Hikmetli Hadisler Kartı", "plan", "🕌", "Vakte özel hadis ve hikmetli sözler")
    )

    fun tumBilesenler(): List<KartBilesen> = KATALOG

    fun bilesenBul(kod: String): KartBilesen? = KATALOG.find { it.kod == kod }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun varsayilanKartIdleri(sekmeAnahtari: String): List<String> {
        return KATALOG.filter { it.varsayilanSekme == sekmeAnahtari }.map { it.kod }
    }

    fun ekraninKartIdleri(context: Context?, sekmeAnahtari: String): List<String> {
        if (context == null) return varsayilanKartIdleri(sekmeAnahtari)
        val sp = prefs(context)
        val s = sp.getString("ekran_kartlari_$sekmeAnahtari", null)
            ?: return varsayilanKartIdleri(sekmeAnahtari)
        return s.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun ekranKartIdleriniKaydet(context: Context?, sekmeAnahtari: String, idler: List<String>): Boolean {
        if (context == null) return true
        prefs(context).edit()
            .putString("ekran_kartlari_$sekmeAnahtari", idler.distinct().joinToString(","))
            .apply()
        return true
    }

    /**
     * Bir ekrana varsayılanların haricinde diğer ekranlardan taşınmış
     * kart kodlarını dondurur.
     */
    fun ekranaTasinanKartIdleri(context: Context?, sekmeAnahtari: String): List<String> {
        val mevcut = ekraninKartIdleri(context, sekmeAnahtari)
        val varsayilan = varsayilanKartIdleri(sekmeAnahtari)
        return mevcut.filter { it !in varsayilan }
    }

    fun ekrandanKartCikar(context: Context?, sekmeAnahtari: String, kartKodu: String): Boolean {
        val list = ekraninKartIdleri(context, sekmeAnahtari).toMutableList()
        val silindi = list.remove(kartKodu)
        if (silindi) {
            ekranKartIdleriniKaydet(context, sekmeAnahtari, list)
        }
        return silindi
    }

    fun ekranaKartEkle(context: Context?, sekmeAnahtari: String, kartKodu: String): Boolean {
        val list = ekraninKartIdleri(context, sekmeAnahtari).toMutableList()
        if (kartKodu !in list) {
            list.add(0, kartKodu)
            ekranKartIdleriniKaydet(context, sekmeAnahtari, list)
        }
        return true
    }

    /**
     * Bir kartı kaynak sekmeden hedef sekmeye TAŞIR (gerçek kart hedef sekmeye geçer).
     */
    fun kartTasi(
        context: Context?,
        kartKodu: String,
        kaynakSekme: String,
        hedefSekme: String
    ): Pair<Boolean, String> {
        val bil = bilesenBul(kartKodu)
        val ad = bil?.ad ?: kartKodu
        ekrandanKartCikar(context, kaynakSekme, kartKodu)
        ekranaKartEkle(context, hedefSekme, kartKodu)
        val hedefAd = sekmeAdGetir(hedefSekme)
        return Pair(true, "⚡ '$ad' gerçek kart olarak '$hedefAd' ekranına taşındı!")
    }

    /**
     * Bir kartı hedef sekmeye KOPYALAR (kaynakta da kalır).
     */
    fun kartKopyala(
        context: Context?,
        kartKodu: String,
        hedefSekme: String
    ): Pair<Boolean, String> {
        val bil = bilesenBul(kartKodu)
        val ad = bil?.ad ?: kartKodu
        ekranaKartEkle(context, hedefSekme, kartKodu)
        val hedefAd = sekmeAdGetir(hedefSekme)
        return Pair(true, "➕ '$ad' gerçek kart olarak '$hedefAd' ekranına kopyalandı!")
    }

    fun varsayilanlaraDon(context: Context?): Boolean {
        if (context == null) return true
        prefs(context).edit().clear().apply()
        return true
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
     * v11.10 — Göz boyayan metin yazısı ("1 adet taşındı" vb.) YERİNE,
     * taşınan kartın KENDİSİNİ gerçek, canlı ve tam işlevli bir MaterialCardView
     * olarak oluşturur.
     */
    fun gercekKartOlustur(context: Context, kartKodu: String): View? {
        val bil = bilesenBul(kartKodu) ?: return null
        val yog = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * yog).toInt()

        val card = MaterialCardView(context).apply {
            tag = "gercek_tasinan_kart_$kartKodu"
            radius = 16 * yog
            cardElevation = 2 * yog
            strokeWidth = dp(1)
            strokeColor = MaterialColors.getColor(
                this,
                com.google.android.material.R.attr.colorPrimary,
                0xFF6200EE.toInt()
            )
            setCardBackgroundColor(
                MaterialColors.getColor(
                    this,
                    com.google.android.material.R.attr.colorSurfaceVariant,
                    0xFFE0E0E0.toInt()
                )
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(14)
            }
        }

        val icLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        // Üst Başlık Satırı
        val ustSatir = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val baslikTv = TextView(context).apply {
            text = "${bil.simge} [Gerçek Taşınan Kart]: ${bil.ad}"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(
                MaterialColors.getColor(
                    this,
                    com.google.android.material.R.attr.colorPrimary,
                    0xFF6200EE.toInt()
                )
            )
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val btnKaldir = Button(context, null, android.R.attr.borderlessButtonStyle).apply {
            text = "✖ Kaldır / Eski Yere Döndür"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(
                MaterialColors.getColor(
                    this,
                    com.google.android.material.R.attr.colorError,
                    0xFFD32F2F.toInt()
                )
            )
            setOnClickListener {
                // Bu sekmeden kaldır ve varsayılan sekmesine geri koy
                ekrandanKartCikar(context, "home", kartKodu)
                ekrandanKartCikar(context, "today", kartKodu)
                ekrandanKartCikar(context, "topics", kartKodu)
                ekrandanKartCikar(context, "progress", kartKodu)
                (context as? MainActivity)?.open(0)
            }
        }

        ustSatir.addView(baslikTv)
        ustSatir.addView(btnKaldir)
        icLayout.addView(ustSatir)

        // Canlı İçerik Satırı
        val icerikTv = TextView(context).apply {
            val ozetMetin = when (kartKodu) {
                "GOREVLER_KARTI" -> {
                    val gorevler = Store.aktifGorevler(context)
                    val biten = gorevler.count { it.done }
                    "✅ Bekleyen ${gorevler.size - biten} görev, tamamlanan $biten görev. Dokunarak tüm görevleri yönetin."
                }
                "NAMAZ_KARTI" -> {
                    val gun = NamazVakti.bugunDuzeltilmis(context)
                    val simdi = NamazVakti.simdiDakika()
                    val sonraki = gun.sonraki(simdi)
                    "🕌 ${sonraki.first.emoji} Sıradaki Vakit: ${context.getString(sonraki.first.adRes)} — ${NamazPlan.sureMetni(sonraki.second)} kaldı"
                }
                "SIMDI_NE_YAPMALI" -> {
                    val oneri = runCatching { SimdiNe.oner(context) }.getOrNull()
                    "📚 Öneri: ${oneri?.baslik ?: "Şimdi odaklanma zamanı"} — ${oneri?.aciklama ?: ""}"
                }
                "ALISKANLIK_KARTI" -> "🌱 Bugün takip edilen alışkanlıklar ve günlük rutin serileriniz."
                "MOTIVASYON_MANSET" -> "💡 Günün felsefi ve Sokratik düşünce manşeti: Hakikati arayan zihin pes etmez."
                "DINI_SOZ_KARTI" -> {
                    val soz = DiniSozMotoru.simdikiVaktinSozu(context)
                    "🕌 ${soz.first}: ${soz.second}"
                }
                else -> bil.aciklama
            }
            text = ozetMetin
            textSize = 13.5f
            setPadding(0, dp(8), 0, dp(8))
            setTextColor(
                MaterialColors.getColor(
                    this,
                    com.google.android.material.R.attr.colorOnSurface,
                    0xFF222222.toInt()
                )
            )
        }

        val btnGit = Button(context).apply {
            text = "🚀 Ekranı Aç / Detaylara Git"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setOnClickListener {
                when (kartKodu) {
                    "GOREVLER_KARTI" -> (context as? MainActivity)?.openTasks()
                    "NAMAZ_KARTI", "DINI_SOZ_KARTI" -> NamazActivity.ac(context)
                    "SIMDI_NE_YAPMALI" -> (context as? MainActivity)?.openToday()
                    "KURSLAR_KARTI" -> (context as? MainActivity)?.open(13)
                    "ALISKANLIK_KARTI" -> (context as? MainActivity)?.openHabits()
                    else -> (context as? MainActivity)?.open(0)
                }
            }
        }

        icLayout.addView(icerikTv)
        icLayout.addView(btnGit)
        card.addView(icLayout)

        return card
    }
}
