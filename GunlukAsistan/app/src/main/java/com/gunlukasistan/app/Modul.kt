package com.gunlukasistan.app

/**
 * v10.16 · KULLANICI İSTEĞİ — Birleştirilebilir widget'ın saf çekirdeği.
 *
 * Kullanıcı 7 modülden istediklerini seçer, sıralar; `ModulWidget`
 * yükseklik bütçesine sığdığı kadarını üstten aşağı dizer. Modül
 * çalışma anında içerik üretemiyorsa (sayac koşmuyor, uyku kaydı yok)
 * düşer ve altındaki modül yukarı kayar — karar render'a aittir,
 * bu sınıf yalnız sıra + bütçe aritmetiğini bilir (test burada).
 */
object Modul {

    /** @property satir kapladığı slot adedi (yükseklik maliyeti). */
    data class Tanim(val anahtar: String, val satir: Int, val adRes: Int, val emoji: String)

    val TANIMLAR = listOf(
        Tanim("saat", 2, R.string.wa_modul_saat, "🕐"),
        Tanim("sayac", 1, R.string.wa_modul_sayac, "⏱"),
        Tanim("gorevler", 2, R.string.wa_modul_gorevler, "✅"),
        Tanim("seri", 1, R.string.wa_modul_seri, "🔥"),
        Tanim("uyku", 1, R.string.wa_modul_uyku, "😴"),
        Tanim("kapi", 1, R.string.wa_modul_kapi, "🚪"),
        Tanim("kronotip", 1, R.string.wa_modul_kronotip, "🐣"),
    )

    fun tanim(anahtar: String): Tanim? = TANIMLAR.firstOrNull { it.anahtar == anahtar }

    fun varsayilanSira(): List<String> = listOf("saat", "sayac", "gorevler", "seri")

    /**
     * Listeyi sağlamlaştırır: bilinmeyen anahtarlar atılır, tekrarlar
     * tekle iner. BOŞ liste GEÇERLİDİR — kullanıcının bütün modülleri
     * kapattığı durumdur; varsayılan zorlamaz (varsayılana dönmek
     * kullanıcının ayrı düğmesidir). Hiç yapılandırılmamış örn.
     * `varsayilanSira` [ModulWidget.siraOku] tarafından verilir.
     */
    fun temizle(sira: List<String>): List<String> =
        sira.filter { tanim(it) != null }.distinct()

    /**
     * Bütçeye sığan öndeğer modülleri sırayla seçer. İlk sığmayan
     * modülde DURUR (üstten kesintisiz dizgi — boşluklu görünmez).
     */
    fun sigacaklar(sira: List<String>, butce: Int): List<String> {
        val cikti = mutableListOf<String>()
        var kalan = butce
        for (a in sira) {
            val maliyet = tanim(a)?.satir ?: continue
            if (maliyet > kalan) break
            kalan -= maliyet
            cikti.add(a)
        }
        return cikti
    }

    /** Sıra düzenleme yardımcıları (sınır dışı istekler aynen döner). */
    fun yukariTasi(sira: List<String>, i: Int): List<String> {
        if (i <= 0 || i >= sira.size) return sira
        val t = sira.toMutableList()
        val e = t.removeAt(i); t.add(i - 1, e)
        return t
    }

    fun asagiTasi(sira: List<String>, i: Int): List<String> {
        if (i < 0 || i >= sira.size - 1) return sira
        val t = sira.toMutableList()
        val e = t.removeAt(i); t.add(i + 1, e)
        return t
    }
}
