package com.gunlukasistan.app

import android.content.Context
import kotlin.math.abs

/**
 * v11.06 — Vaktin Sözü / Hikmetli Dini Sözler ve Hadisler Motoru (`DiniSozMotoru`).
 *
 * Kullanıcının "Oraya ekstra olarak dini sozler yeri ekle her vakitte farkli sozler gelsin. Gösterişli olsun."
 * talimatı doğrultusunda, günün 6 ana namaz ve ibadet dilimi (Seher/Sabah, Kuşluk/İşrak, Öğle, İkindi,
 * Akşam, Gece/Yatsı) için özenle seçilmiş hadis-i şerifler, ayet mealleri ve hikmetli sözler sunar.
 */
object DiniSozMotoru {

    data class DiniSoz(
        val dilim: NamazPlan.Dilim,
        val baslik: String,
        val metin: String,
        val kaynak: String
    ) {
        val formatliMetin: String
            get() = if (kaynak.isNotBlank()) "\"$metin\" ($kaynak)" else "\"$metin\""
    }

    private val SOZ_HAVUZU = listOf(
        // ── SABAH / SEHER / İMSAK (`NamazPlan.Dilim.SABAH`) ──
        DiniSoz(
            NamazPlan.Dilim.SABAH,
            "🕌 Vaktin Hikmeti: Seher & Sabah",
            "Sabah namazını kılan kimse Allah'ın güvencesi altındadır.",
            "Müslim, Mesâcid 262"
        ),
        DiniSoz(
            NamazPlan.Dilim.SABAH,
            "🌙 Seher Vaktinin Bereketi",
            "Gecenin son üçte birinde seher vaktinde yapılan istiğfar, kalbin cilası ve duaların kabul vesilesidir.",
            "Hadis-i Şerif"
        ),
        DiniSoz(
            NamazPlan.Dilim.SABAH,
            "☀️ Günün İlk Adımı",
            "Sabahın bereketli ışığıyla uyanan, rızkını ve huzurunu Allah'tan bekler; gününü namazla açan asla kaybetmez.",
            "Hikmetli Söz"
        ),
        DiniSoz(
            NamazPlan.Dilim.SABAH,
            "🕌 Sabahın Huzuru",
            "Sabah namazının iki rekat sünneti, dünyadan ve içindeki her şeyden daha hayırlıdır.",
            "Müslim, Müsâfirîn 96"
        ),

        // ── KUŞLUK / İŞRAK (`NamazPlan.Dilim.KUSLUK`) ──
        DiniSoz(
            NamazPlan.Dilim.KUSLUK,
            "🌅 Vaktin Hikmeti: İşrak & Kuşluk",
            "Kuşluk (Duha) namazı, vücuttaki her eklem için verilen bir sadakadır.",
            "Müslim, Müsâfirîn 84"
        ),
        DiniSoz(
            NamazPlan.Dilim.KUSLUK,
            "☀️ Kuşluk Vaktinin Sırrı",
            "Güneş yükselirken dilini zikre, kalbini şükre alıştıran kimse gün boyu manevî bir zırh içindedir.",
            "Hikmetli Söz"
        ),
        DiniSoz(
            NamazPlan.Dilim.KUSLUK,
            "🌅 İşrak Bereketi",
            "İşrak vaktinde kılınan iki rekat namaz, tam bir hac ve umre sevabı gibidir.",
            "Tirmizî, Cum'a 59"
        ),
        DiniSoz(
            NamazPlan.Dilim.KUSLUK,
            "✨ Güne Doğru",
            "Allah'ım, bu kuşluk vaktinin aydınlığını kalbime, şifasını bedenime rızık olarak ihsan eyle.",
            "Dua"
        ),

        // ── ÖĞLE / GÜN ORTASI (`NamazPlan.Dilim.OGLEDEN`) ──
        DiniSoz(
            NamazPlan.Dilim.OGLEDEN,
            "☀️ Vaktin Hikmeti: Öğle & Gün Ortası",
            "Öğle namazı, gökyüzünün kapılarının açıldığı ve duaların yüce meclise yükseldiği mübarek bir vakittir.",
            "Tirmizî, Vitir 33"
        ),
        DiniSoz(
            NamazPlan.Dilim.OGLEDEN,
            "🌿 Dünya Meşgalesine Mola",
            "Gün ortasında şeytanın vesvesesinden namazla uzaklaşan, ruhunun yorgunluğunu secdede atar.",
            "Hikmetli Söz"
        ),
        DiniSoz(
            NamazPlan.Dilim.OGLEDEN,
            "☀️ Öğlenin Fazileti",
            "Dünyanın meşgalesi en yoğun olduğunda alnını secdeye koymak, kalbin en büyük ferahlığıdır.",
            "İmam-ı Gazalî"
        ),
        DiniSoz(
            NamazPlan.Dilim.OGLEDEN,
            "🕌 Gün Ortası Sekinesi",
            "Kim öğle namazından önce dört, sonra dört rekat kılmaya devam ederse Allah onu cehennem ateşine haram kılar.",
            "Ebû Dâvûd, Tatavvu 7"
        ),

        // ── İKİNDİ / İKİNDİ SONRASI (`NamazPlan.Dilim.IKINDIDEN`) ──
        DiniSoz(
            NamazPlan.Dilim.IKINDIDEN,
            "🌤 Vaktin Hikmeti: İkindi",
            "Sabah ve ikindi namazlarını muhafaza eden kimse cennete girer.",
            "Buhârî, Mevâkît 26"
        ),
        DiniSoz(
            NamazPlan.Dilim.IKINDIDEN,
            "🌇 Amellerin Yükseldiği Vakit",
            "İkindi ile akşam arasındaki vakit, amellerin semaya yükseltildiği kıymetli anlardır; vaktin bereketini kaçırma.",
            "Hadis-i Şerif"
        ),
        DiniSoz(
            NamazPlan.Dilim.IKINDIDEN,
            "🛡️ İkindinin Koruyuculuğu",
            "İkindi namazını kaçıran kimsenin ameli boşa gitmiş gibi büyük bir kayba uğrar.",
            "Buhârî, Mevâkît 15"
        ),
        DiniSoz(
            NamazPlan.Dilim.IKINDIDEN,
            "🌤 Gün Batarken",
            "İkindi namazı vaktinde melekler nöbet değiştirir; seni secdede bulmaları ne büyük bir şereftir.",
            "Buhârî, Tevhîd 23"
        ),

        // ── AKŞAM (`NamazPlan.Dilim.AKSAMDAN`) ──
        DiniSoz(
            NamazPlan.Dilim.AKSAMDAN,
            "🌆 Vaktin Hikmeti: Akşam",
            "Akşamın loşluğunda Allah'a yönelmek, günün yorgunluğunu ibadetle taçlandırmaktır.",
            "Hikmetli Söz"
        ),
        DiniSoz(
            NamazPlan.Dilim.AKSAMDAN,
            "🌇 Günün Muhasebesi",
            "Güneş batarken yapılan dua ve istiğfarlar, kalbi dünyevî tasalardan temizler ve geceye huzurla hazırlar.",
            "Hadis-i Şerif"
        ),
        DiniSoz(
            NamazPlan.Dilim.AKSAMDAN,
            "🌆 Akşamın Sükuneti",
            "Akşam namazı, günün şükrünü eda etmenin ve geceye selametle girmenin habercisidir.",
            "İmam Rabbani"
        ),
        DiniSoz(
            NamazPlan.Dilim.AKSAMDAN,
            "🌙 Akşam Duası",
            "Rabbim! Gün batarken kalbimi masivadan arındır, beni rızana mazhar olan kullarından eyle.",
            "Dua"
        ),

        // ── GECE / YATSI / TEHECCÜD (`NamazPlan.Dilim.GECE`) ──
        DiniSoz(
            NamazPlan.Dilim.GECE,
            "Night Vaktin Hikmeti: Yatsı & Gece",
            "Yatsı namazını cemaatle kılan kimse, gecenin yarısını ibadetle geçirmiş gibidir.",
            "Müslim, Mesâcid 260"
        ),
        DiniSoz(
            NamazPlan.Dilim.GECE,
            "🌌 Gece İbadetinin Nuru",
            "Gece karanlığında kılınan teheccüd namazı, kabir karanlığının nurudur.",
            "Hikmetli Söz"
        ),
        DiniSoz(
            NamazPlan.Dilim.GECE,
            "🌃 Gece Yarısı Duası",
            "İnsanlar uykuda iken Allah'ın huzuruna duran kimsenin duası reddolunmaz.",
            "Tirmizî, Daavât 79"
        ),
        DiniSoz(
            NamazPlan.Dilim.GECE,
            "⭐ Yatsının Ferahlığı",
            "Yatsı namazıyla günü kapatan mümin, uykuya Allah'ın emaneti altında huzurla dalar.",
            "Hadis-i Şerif"
        )
    )

    fun tumSozler(): List<DiniSoz> = SOZ_HAVUZU

    fun dilimIcinSozler(dilim: NamazPlan.Dilim): List<DiniSoz> {
        return SOZ_HAVUZU.filter { it.dilim == dilim }.ifEmpty {
            SOZ_HAVUZU.filter { it.dilim == NamazPlan.Dilim.SABAH }
        }
    }

    fun vaktinSozunuGetir(dilim: NamazPlan.Dilim, index: Int = -1): Pair<String, String> {
        val list = dilimIcinSozler(dilim)
        val secilen = if (index in 0 until list.size) {
            list[index]
        } else {
            val gunSaatIndeksi = abs(System.currentTimeMillis() / (1000 * 60 * 60)).toInt() % list.size
            list[gunSaatIndeksi]
        }
        return Pair(secilen.baslik, secilen.formatliMetin)
    }

    fun simdikiVaktinSozu(context: Context?): Pair<String, String> {
        if (context == null) return vaktinSozunuGetir(NamazPlan.Dilim.SABAH)
        val simdiDk = NamazVakti.simdiDakika()
        val gun = NamazVakti.bugunDuzeltilmis(context)
        val dilim = NamazPlan.aktifDilim(gun, simdiDk)
        return vaktinSozunuGetir(dilim)
    }

    fun sonrakiSozuGetir(dilim: NamazPlan.Dilim, simdikiMetin: String): Pair<String, String> {
        val list = dilimIcinSozler(dilim)
        if (list.size <= 1) {
            val s = list.first()
            return Pair(s.baslik, s.formatliMetin)
        }
        val farklilar = list.filter { it.formatliMetin != simdikiMetin }
        val secilen = if (farklilar.isNotEmpty()) farklilar.random() else list.random()
        return Pair(secilen.baslik, secilen.formatliMetin)
    }

    fun rastgeleSozGetir(dilim: NamazPlan.Dilim): Pair<String, String> {
        val list = dilimIcinSozler(dilim)
        val secilen = list.random()
        return Pair(secilen.baslik, secilen.formatliMetin)
    }
}
