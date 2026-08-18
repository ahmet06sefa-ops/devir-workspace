package com.gunlukasistan.app

import android.content.Context

/**
 * v7.31 — Ders içeriğine dayalı soru-cevap (RAG).
 *
 * Normal asistandan farkı: yapay zekâ **genel bilgisiyle değil**, o dersin
 * gerçek metniyle cevap verir. Böylece:
 *   · Cevaplar kursla tutarlı olur
 *   · "Bu tabloda ne yazıyor" gibi sorular yanıtlanabilir
 *   · Uydurma bilgi riski düşer
 *
 * Metinde olmayan bir şey sorulursa model bunu açıkça söyler —
 * uydurmaya zorlanmaz.
 */
object DersAsistan {

    private const val TAG = "DersAsistan"

    /** Hazır soru kalıpları — kullanıcı yazmadan tek dokunuşla sorabilsin. */
    data class HazirSoru(val etiket: String, val soru: String)

    val HAZIR_SORULAR = listOf(
        HazirSoru("📝 Özetle", "Bu dersi 5 maddede özetle."),
        HazirSoru("🧒 Basitleştir", "Bu konuyu hiç bilmeyen birine anlatır gibi, çok basit anlat."),
        HazirSoru("💡 Örnek ver", "Bu konuyla ilgili somut, gerçek hayattan bir örnek ver."),
        HazirSoru("❓ Neden böyle", "Buradaki kuralın mantığı ne? Neden böyle yapılıyor?"),
        HazirSoru("⚠ Sık hatalar", "Bu konuda en sık yapılan hatalar neler?"),
        HazirSoru("🎯 Sınavda", "Bu dersten sınavda/mülakatta ne sorulur?"),
        HazirSoru("🔗 Bağlantı", "Bu konu diğer konularla nasıl ilişkili?"),
        HazirSoru("📋 Adımlar", "Bu işlemi adım adım sırala.")
    )

    class Cevap(val ok: Boolean, val metin: String)

    /**
     * Ders metnine dayanarak soruyu yanıtlar.
     *
     * @param assetPath ders PDF yolu
     * @param aktifSayfa kullanıcının baktığı sayfa (0 tabanlı)
     * @param soru kullanıcının sorusu
     * @param dersAdi bağlam için
     */
    fun sor(
        context: Context,
        assetPath: String,
        aktifSayfa: Int,
        soru: String,
        dersAdi: String
    ): Cevap {
        if (soru.isBlank()) {
            return Cevap(false, context.getString(R.string.rag_empty_question))
        }
        if (AiSettings.getApiKey(context).isBlank() &&
            AiSettings.anahtarliSaglayicilar(context).isEmpty()
        ) {
            return Cevap(false, context.getString(R.string.ai_err_no_key))
        }
        if (!AiClient.isOnline(context)) {
            return Cevap(false, context.getString(R.string.ai_err_no_net))
        }

        val tamMetin = DersMetni.metniAl(context, assetPath)
        if (tamMetin.isNullOrBlank()) {
            return Cevap(false, context.getString(R.string.rag_no_text))
        }

        val baglam = DersMetni.baglamHazirla(tamMetin, aktifSayfa)
        val istem = istemKur(dersAdi, baglam, soru)

        val sonuc = AiClient.chat(context, istem)
        return if (sonuc.ok) {
            // Komut satırı sızmasın — ders asistanı komut üretmemeli
            val (temiz, _) = AsistanKomut.ayikla(sonuc.text)
            Cevap(true, temiz.ifBlank { sonuc.text })
        } else {
            Cevap(false, sonuc.text)
        }
    }

    /**
     * İstem tasarımı.
     *
     * Kritik kural: model **yalnızca verilen metne** dayanmalı. Metinde
     * olmayan bir şey sorulursa uydurmak yerine bunu söylemeli — ama
     * tamamen reddetmemeli, genel bilgiyle destekleyebileceğini belirtmeli.
     */
    private fun istemKur(dersAdi: String, baglam: String, soru: String): String =
        """Aşağıda "$dersAdi" dersinin içeriği var. Kullanıcı bu dersi okurken soru soruyor.

=== DERS İÇERİĞİ ===
$baglam
=== İÇERİK SONU ===

KULLANICININ SORUSU: $soru

CEVAP KURALLARI:
1. Öncelikle YUKARIDAKİ İÇERİĞE dayan. Dersin kendi anlatımıyla tutarlı ol.
2. İçerikte olmayan bir şey soruluyorsa şöyle belirt:
   "Bu ders bunu kapsamıyor ama genel olarak..." diyerek devam et.
3. UYDURMA. Emin olmadığın sayı, standart veya kural verme.
4. Kısa ve net yaz. En fazla 6-8 cümle veya 5 madde.
5. Türkçe yaz, teknik terimleri koru.
6. Gerekiyorsa madde işareti kullan, tablo kurma.
7. Seviyeyi dersin alanına ve içeriğine göre ayarla; alanı içerikten anla.
8. Cevabının sonuna KOMUT satırı EKLEME."""
}
