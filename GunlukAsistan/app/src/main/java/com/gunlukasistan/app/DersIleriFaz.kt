package com.gunlukasistan.app

import java.util.Locale

/**
 * v10.60 — Ders Çalışma İleri Fazı: Bilişsel Leitner Kutusu & PDF Flaş Kart Üretim Merkezi
 * (saf mantık motoru).
 *
 * Kullanıcının "Kategori 1: Bilişsel Öğrenme / Leitner Kutusu İleri Fazı veya Kategori 5:
 * PDF Sayfa Üzeri Flaş Kart Üretimi özel alt sayfalarına... Devam edebilirsin" talebini
 * 7 ileri seviye alt-sistemle karşılar:
 *  1. İleri Leitner Flaş Kart ve SR-2-7-30 Aralıklı Tekrar Motoru ([Ileri1_LeitnerMotoru])
 *  2. PDF Sayfa Üzeri Otomatik Flaş Kart & Vurgu Üreticisi ([Ileri2_PdfFlasKart])
 *  3. KPSS / YKS Deneme Sınavı Net Eğrisi & Hız Radarı ([Ileri3_DenemeEgrisi])
 *  4. Aktif Geri Çağırma (Active Recall) Boş Sayfa Çıktı Testi ([Ileri4_ActiveRecall])
 *  5. Animedoro (40/20) & 90m Ultradian Biyo-Ritm Akış Simülatörü ([Ileri5_BiyoSprint])
 *  6. AI Sokratik İpucu Koçu & Çeldirici Şık Not Defteri ([Ileri6_CeldiriciVeSokratik])
 *  7. Çevrimdışı Altın Formül Kasası & CSV Deneme Dışa Aktarıcı ([Ileri7_FormulVeCsv])
 */
object DersIleriFaz {

    // ── 1. İleri Leitner Flaş Kart ve SR-2-7-30 Motoru ──
    data class LeitnerKart(
        val id: String,
        val soru: String,
        val cevap: String,
        val kutuNo: Int = 1, // 1: Günlük, 2: Haftalık, 3: Aylık
        val calismaGunIndex: Int = 0
    )

    object Ileri1_LeitnerMotoru {
        fun varsayilanDeste(): List<LeitnerKart> = listOf(
            LeitnerKart("k1", "Lozan'da çözülemeyen tek konu nedir?", "Irak Sınırı / Musul Sorunu (1926 Ankara Antlaşması)", 1),
            LeitnerKart("k2", "Bir üçgenin iç açıları toplamı kaç derecedir?", "180 Derece (Pisagor kurallarıyla ilişkili)", 2),
            LeitnerKart("k3", "Türkçede 'ki' bağlacının ayrı yazılmadığı istisnalar?", "SOMBAHÇEM kodlaması (Sanki, Oysaki, Mademki...)", 3)
        )

        fun kartDogruBildim(kart: LeitnerKart): LeitnerKart {
            val yeniKutu = (kart.kutuNo + 1).coerceAtMost(3)
            return kart.copy(kutuNo = yeniKutu)
        }

        fun kartYanlisBildim(kart: LeitnerKart): LeitnerKart {
            return kart.copy(kutuNo = 1)
        }

        fun kutuDagilimOzeti(kartlar: List<LeitnerKart>): String {
            val k1 = kartlar.count { it.kutuNo == 1 }
            val k2 = kartlar.count { it.kutuNo == 2 }
            val k3 = kartlar.count { it.kutuNo == 3 }
            return "Kutu 1 (Günlük): $k1 Kart | Kutu 2 (Haftalık): $k2 Kart | Kutu 3 (Aylık): $k3 Kart"
        }
    }

    // ── 2. PDF Sayfa Üzeri Otomatik Flaş Kart & Vurgu Üreticisi ──
    object Ileri2_PdfFlasKart {
        fun pdfVurgudanFlasKartUret(vurguMetni: String): LeitnerKart {
            val temiz = vurguMetni.trim()
            val id = "pdf_" + (temiz.hashCode() and 0x7FFFFFFF).toString()
            val parcalar = temiz.split(" - ", " : ", "->", "=>")
            return if (parcalar.size >= 2) {
                LeitnerKart(id = id, soru = "${parcalar[0].trim()} nedir?", cevap = parcalar[1].trim(), kutuNo = 1)
            } else {
                LeitnerKart(id = id, soru = "Tanım: $temiz", cevap = "Kavramın detaylarını hatırladığınızdan emin olun.", kutuNo = 1)
            }
        }
    }

    // ── 3. KPSS / YKS Deneme Sınavı Net Eğrisi & Hız Radarı ──
    data class DenemeKaydi(
        val sinavAd: String,
        val dogru: Int,
        val yanlis: Int,
        val sureDk: Int,
        val net: Float = (dogru - (yanlis / 4.0f)).coerceAtLeast(0f)
    )

    object Ileri3_DenemeEgrisi {
        fun denemeKayitEkle(list: List<DenemeKaydi>, yeni: DenemeKaydi): List<DenemeKaydi> {
            return list + yeni
        }

        fun egriAnalizi(list: List<DenemeKaydi>): String {
            if (list.isEmpty()) return "Henüz deneme kaydı yok."
            val ortNet = list.map { it.net }.average().toFloat()
            val sonNet = list.last().net
            val trend = if (sonNet >= ortNet) "📈 YÜKSELİŞTE" else "📉 TEKRAR GEREKLİ"
            val ortSaniye = list.map { if (it.dogru + it.yanlis > 0) (it.sureDk * 60) / (it.dogru + it.yanlis) else 0 }.average().toInt()
            return String.format(Locale.US, "Ortalama NET: %.2f | Son NET: %.2f (%s) | Hız: %d sn/soru", ortNet, sonNet, trend, ortSaniye)
        }
    }

    // ── 4. Aktif Geri Çağırma (Active Recall) Boş Sayfa Testi ──
    object Ileri4_ActiveRecall {
        fun activeRecallSkoru(ozetMetin: String): Int {
            val kelimeSayisi = ozetMetin.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
            val cumleSayisi = ozetMetin.split(".", "!", "?").count { it.trim().isNotEmpty() }.coerceAtLeast(1)
            return when {
                kelimeSayisi < 5 -> 25
                cumleSayisi >= 3 && kelimeSayisi >= 10 -> 95
                cumleSayisi >= 2 && kelimeSayisi >= 8 -> 80
                else -> 60
            }
        }
    }

    // ── 5. Animedoro & Ultradian Biyo-Ritm Akış Simülatörü ──
    data class SprintSeans(
        val ad: String,
        val odakDk: Int,
        val molaDk: Int,
        val aciklama: String
    )

    object Ileri5_BiyoSprint {
        fun seansSecimi(id: Int): SprintSeans {
            return when (id) {
                1 -> SprintSeans("Animedoro (40m/20m)", 40, 20, "40m Yüksek Odak -> 20m Anime/Ödül Molası (Sıkılmadan 4 saat çalışma)")
                2 -> SprintSeans("Ultradian Ritm (90m/20m)", 90, 20, "90m Derin Odak -> 20m Zihinsel Reset (Beynin doğal biyo-ritm periyodu)")
                else -> SprintSeans("Standart Pomodoro (25m/5m)", 25, 5, "25m Odak -> 5m Kısa Mola (Temel Klasik Ritm)")
            }
        }
    }

    // ── 6. AI Sokratik Koç & Çeldirici Şık Not Defteri ──
    object Ileri6_CeldiriciVeSokratik {
        fun celdiriciUyarisi(ders: String): String {
            return when (ders.trim().lowercase(Locale("tr", "TR"))) {
                "tarih" -> "⚠️ ÖSYM Çeldiricisi (Tarih): Soru kökünde 'İlk defa' veya 'Kesinlikle' ifadelerine dikkat! Tanzimat ile Islahat fermanını karıştırmayın."
                "türkçe", "turkce" -> "⚠️ ÖSYM Çeldiricisi (Paragraf): 'Yalnız I' ile 'I ve II' şıkları arasında metne sadık kalın, kendi yorumunuzu eklemeyin."
                else -> "⚠️ ÖSYM Çeldiricisi ($ders): Soru kökündeki 'değildir' ve 'ulaşılamaz' olumsuz ifadelerini mutlaka daire içine alın."
            }
        }
    }

    // ── 7. Çevrimdışı Formül & CSV Dışa Aktarıcı ──
    object Ileri7_FormulVeCsv {
        fun kartlariCsvUret(kartlar: List<LeitnerKart>): String {
            val sb = StringBuilder()
            sb.appendLine("ID,Soru,Cevap,KutuNo")
            for (k in kartlar) {
                sb.appendLine("${k.id},\"${k.soru}\",\"${k.cevap}\",${k.kutuNo}")
            }
            return sb.toString().trim()
        }

        fun denemeleriCsvUret(denemeler: List<DenemeKaydi>): String {
            val sb = StringBuilder()
            sb.appendLine("SinavAd,Dogru,Yanlis,SureDk,Net")
            for (d in denemeler) {
                sb.appendLine(String.format(Locale.US, "\"%s\",%d,%d,%d,%.2f", d.sinavAd, d.dogru, d.yanlis, d.sureDk, d.net))
            }
            return sb.toString().trim()
        }
    }
}
