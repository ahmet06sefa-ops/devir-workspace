package com.gunlukasistan.app

import android.content.Context

/**
 * 2026 KPSS Genel Yetenek – Genel Kültür hazır müfredatı.
 * Ders kartları ve tam alt konu listesi; tek dokunuşla Konular'a eklenir.
 * (GY: Türkçe 30 + Matematik 30 soru · GK: Tarih 27 + Coğrafya 18 +
 * Vatandaşlık 9 + Güncel 6 soru = 120 soru / 130 dk)
 */
object KpssPack {

    /** Ders başlığı -> ayrıntılı alt konu listesi */
    val subjects: List<Pair<String, List<String>>> = listOf(
        "📖 KPSS Türkçe (30 soru)" to listOf(
            "Sözcükte anlam (gerçek-mecaz-terim-eyyam)",
            "Söz öbeklerinde anlam (deyimler, ikilemeler)",
            "Cümlede anlam ve yorum",
            "Paragrafta ana düşünce ve yardımcı düşünce",
            "Paragrafta yapı ve anlatım biçimi",
            "Paragraf tamamlama ve akış",
            "Ses bilgisi (ses olayları, yazım kaynaşımı)",
            "Yazım kuralları",
            "Noktalama işaretleri",
            "Sözcük türleri (isim-zamir-sıfat-zarf-edat-bağlaç-ünlem)",
            "Fiilimsi, fiilde çatı, cümle öğeleri",
            "Anlatım bozukluğu",
            "Sözel mantık"
        ),
        "➗ KPSS Matematik (26 soru)" to listOf(
            "Temel kavramlar ve sayılar",
            "Bölme-bölünebilme, EBOB-EKOK",
            "Rasyonel ve ondalık kesirler",
            "Basit eşitsizlikler ve mutlak değer",
            "Üslü ve köklü sayılar",
            "Çarpanlara ayırma ve özdeşlik",
            "Oran-orantı problemleri",
            "Sayı ve kesir problemleri",
            "Yaş problemleri",
            "İşçi-havuz problemleri",
            "Hareket problemleri",
            "Yüzde, kâr-zarar ve karışım problemleri",
            "Kümeler ve mantık",
            "Permütasyon-kombinasyon-olasılık",
            "Veri, istatistik ve grafik okuma",
            "Sayısal mantık"
        ),
        "📐 KPSS Geometri (4 soru)" to listOf(
            "Doğruda ve üçgende açılar",
            "Özel üçgenler, eşlik ve benzerlik",
            "Çokgenler ve dörtgenler",
            "Çember ve daire",
            "Alan-çevre hesapları",
            "Katı cisimler ve temel analitik"
        ),
        "📜 KPSS Tarih (27 soru)" to listOf(
            "İlk ve orta çağ uygarlıkları",
            "İslamiyet öncesi Türk tarihi (Hun, Göktürk, Uygur)",
            "İslamiyet'in doğuşu ve yayılması",
            "Türk-İslam devletleri (Karahanlı, Gazneli, Harzemşah)",
            "Büyük Selçuklular ve Anadolu Selçukluları",
            "Beylikler dönemi ve Anadolu'nun Türkleşmesi",
            "Osmanlı kuruluş dönemi",
            "Osmanlı yükselme dönemi",
            "Osmanlı devlet yapısı ve kültür-medeniyet",
            "Duraklama ve gerileme dönemi nedenleri",
            "Dağılma dönemi ve ıslahatlar",
            "XIX. yüzyıl fikir akımları ve genç Osmanlılar",
            "Meşrutiyet dönemleri",
            "Trablusgarp, Balkan Savaşları ve I. Dünya Savaşı",
            "Mondros, işgaller, cemiyetler ve Kuvayımilliye",
            "TBMM'nin açılması ve Kurtuluş Savaşı cepheleri",
            "Mudanya Ateşkesi ve Lozan Antlaşması",
            "Saltanatın kaldırılması ve Cumhuriyet'in ilanı",
            "İnkılap hareketleri (siyasi-hukuki-toplumsal)",
            "Atatürk ilkeleri",
            "Atatürk dönemi dış politika",
            "II. Dünya Savaşı ve sonrası dünya",
            "Soğuk Savaş dönemi ve Türkiye",
            "Çağdaş Türk ve dünya tarihi (küreselleşme)"
        ),
        "🗺️ KPSS Coğrafya (18 soru)" to listOf(
            "Türkiye'nin coğrafi ve matematik konumu",
            "Yer şekilleri (dağ, ova, plato)",
            "Tektonik yapı ve depremsellik",
            "İklim: sıcaklık, basınç ve rüzgârlar",
            "Yağış, nem ve iklim tipleri",
            "Akarsular ve göller",
            "Topraklar ve doğal bitki örtüsü",
            "Nüfus ve yerleşme",
            "Göçler ve şehirleşme",
            "Coğrafi bölgeler ve özellikleri",
            "Tarım ve hayvancılık",
            "Madenler ve enerji kaynakları",
            "Sanayi ve ticaret",
            "Ulaşım ve turizm",
            "Doğal afetler ve çevre sorunları",
            "Bölge sınıflandırması ve bölgesel farklar"
        ),
        "⚖️ KPSS Vatandaşlık (9 soru)" to listOf(
            "Anayasa tarihçesi (1921-1982)",
            "1982 Anayasası'nın temel ilkeleri",
            "Temel hak ve ödevler",
            "Yasama: TBMM'nin yapısı ve görevleri",
            "Yürütme: Cumhurbaşkanı ve Cumhurbaşkanlığı kararnameleri",
            "Yargı: mahkemeler ve Anayasa yargısı",
            "İdare hukuku: merkezi yönetim",
            "Yerel yönetimler ve il özel idaresi",
            "Kamu memurları (657 sayılı kanun)",
            "Denetim organları (KDK/ombudsman, Sayıştay)",
            "Seçimler ve siyasi partiler hukuku",
            "Uluslararası kuruluşlar ve Türkiye'nin üyelikleri"
        ),
        "📰 KPSS Güncel Bilgiler (6 soru)" to listOf(
            "Son 1 yılın Türkiye gündemi",
            "Son 1 yılın dünya gündemi ve bölgesel gelişmeler",
            "Uluslararası kuruluşların güncel zirveleri",
            "Bilim, teknoloji ve uzay gelişmeleri",
            "Ödüller, spor ve kültür-sanat gündemi",
            "Çevre, iklim ve sürdürülebilirlik gündemi"
        )
    )

    /** Zaten ekli olmayan KPSS derslerini Konular'a ekler. Kaç ders eklediğini döndürür. */
    fun addMissing(context: Context): Int {
        val topics = Store.loadTopics(context)
        val existing = topics.map { it.title }.toMutableSet()
        var added = 0
        val now = System.currentTimeMillis()
        subjects.forEachIndexed { index, (title, subs) ->
            if (title !in existing) {
                topics.add(
                    Store.Topic(
                        id = now + index,
                        title = title,
                        createdAt = now + index,
                        items = subs.mapIndexed { j, sub ->
                            Store.SubItem(
                                id = now * 10 + index * 1000 + j,
                                text = sub,
                                done = false,
                                createdAt = now
                            )
                        }.toMutableList()
                    )
                )
                added++
                existing.add(title)
            }
        }
        if (added > 0) Store.saveTopics(context, topics)
        return added
    }

    /** Eklenebilecek (henüz eklenmemiş) KPSS dersi var mı? */
    fun hasMissing(context: Context): Boolean {
        val existing = Store.loadTopics(context).map { it.title }.toSet()
        return subjects.any { it.first !in existing }
    }

    fun subjectCount(): Int = subjects.size
    fun subtopicCount(): Int = subjects.sumOf { it.second.size }
}
