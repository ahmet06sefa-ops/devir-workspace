package com.gunlukasistan.app

/**
 * v11.13 — CSV veri dışa aktarma motoru (SAF, JVM testli).
 *
 * Kullanıcı isteği: "Rakiplerde olup bende olmayan zengin veri dışa aktarma
 * ekle." Excel/Google Sheets'e açılabilen CSV dışa aktarımı, görev ve
 * alışkanlık verisini paylaşılabilir bir biçime çevirir.
 *
 *  · [CSV hücre güvenliği] — virgül, tırnak, satır sonu içeren değerleri sarar.
 *  · [gorevSatirlari] — görevleri başlıklı CSV satırlarına çevirir.
 *  · [aliskanlikSatirlari] — alışkanlıkları CSV satırlarına çevirir.
 *  · [birlestir] — başlık + satırları tek CSV metnine birleştirir.
 */
object CsvDisAktarMotoru {

    /** CSV hücre güvenliği: gerekirse tırnakla sar, iç tırnakları ikiye katla. */
    fun hucre(deger: String): String {
        val gerekli = deger.contains(",") || deger.contains("\"") ||
            deger.contains("\n") || deger.contains(";")
        return if (gerekli) "\"" + deger.replace("\"", "\"\"") + "\"" else deger
    }

    /** Görev CSV başlığı. */
    val GOREV_BASLIK = "id,metin,durum,tekrar,etiket"

    /** Görevleri CSV satırlarına çevirir (başlıksız, sadece veri). */
    fun gorevSatirlari(gorevler: List<Store.Task>): List<String> =
        gorevler.map { t ->
            listOf(
                t.id.toString(),
                hucre(t.text),
                if (t.done) "tamamlandı" else "bekliyor",
                hucre(t.tekrar),
                hucre(t.etiket)
            ).joinToString(",")
        }

    /** Alışkanlık CSV başlığı. */
    val ALISKANLIK_BASLIK = "id,ad,hedef,arşiv"

    /** Alışkanlıkları CSV satırlarına çevirir. */
    fun aliskanlikSatirlari(aliskanliklar: List<Store.Habit>): List<String> =
        aliskanliklar.map { h ->
            listOf(
                h.id.toString(),
                hucre(h.title),
                h.target.toString(),
                h.archived.toString()
            ).joinToString(",")
        }

    /** Başlık + satırları tek CSV metnine birleştirir (satır sonları \n). */
    fun birlestir(baslik: String, satirlar: List<String>): String =
        (listOf(baslik) + satirlar).joinToString("\n")
}
