package com.gunlukasistan.app

import android.content.Context
import android.view.View
import java.util.Locale

/**
 * v10.73 — Akıllı "Sokratik & Felsefi Motivasyon Manşeti" Motoru (Madde #12).
 *
 * Kullanıcının ana ekranının üst kısmında Stoacı felsefe, Sokratik bilgelik,
 * bilim insanları ve önderlerden ilham verici kısa sözler sunan, tek tuşla
 * yenilenebilen, kendi kişisel mottosunu yazıp ekrana sabitleyebileceği ve
 * ayarlardan açıp kapatabileceği otonom motivasyon şeridi.
 */
object MotivasyonMansetMotoru {

    private const val PREF_NAME = "motivasyon_manset_v1"
    private const val KEY_GOSTER = "manset_goster_mi"
    private const val KEY_SABITLI_MI = "soz_sabitli_mi"
    private const val KEY_OZEL_SOZ = "ozel_soz_metni"
    private const val KEY_OZEL_YAZAR = "ozel_yazar_adi"
    private const val KEY_SON_INDEX = "son_gosterilen_index"

    data class MotivasyonSozu(
        val id: Int,
        val soz: String,
        val yazar: String,
        val kategori: String
    )

    // ── 20 SEÇİLMİŞ STOACI, SOKRATİK, BİLİMSEL & AKADEMİK MOTTOLAR ──
    fun varsayilanSozListesi(): List<MotivasyonSozu> {
        return listOf(
            MotivasyonSozu(1, "Zorluklar zihni güçlendirir, tıpkı çalışmanın bedeni güçlendirdiği gibi.", "Seneca", "Stoacı Felsefe"),
            MotivasyonSozu(2, "Dünyayı değiştirmek isteyen önce kendinden başlamalıdır.", "Sokrates", "Sokratik Bilge"),
            MotivasyonSozu(3, "Bilemezsin ne zaman başaracağını, ama vazgeçtiğin an kaybettiğin andır.", "Marcus Aurelius", "Stoacı Felsefe"),
            MotivasyonSozu(4, "Zafer, 'Zafer benimdir' diyebilenindir; Başarı ise 'Başaracağım' diye başlayıp 'Başardım' diyebilenindir.", "Gazi Mustafa Kemal Atatürk", "Liderlik & Vizyon"),
            MotivasyonSozu(5, "Öğrenmek, akıntıya karşı kürek çekmek gibidir; durursanız geriye gidersiniz.", "Çin Atasözü", "Akademik Disiplin"),
            MotivasyonSozu(6, "Kontrol edemediğin şeyler için endişelenmeyi bırak, sadece kendi çabana odaklan.", "Epiktetos", "Stoacı Felsefe"),
            MotivasyonSozu(7, "Sadece 5 dakika için masaya otur; en zor kısım başlamaktır.", "Sokratik Koç (#12)", "Çalışma Ergonomisi"),
            MotivasyonSozu(8, "Hiç hata yapmayan bir insan, hiçbir şey denememiş demektir.", "Albert Einstein", "Bilimsel Keşif"),
            MotivasyonSozu(9, "Damlaya damlaya göl olur; her pomodoro seansı seni hayaline bir adım daha yaklaştırır.", "Farabi", "İslam Bilim & Felsefe"),
            MotivasyonSozu(10, "İlim ilim bilmektir, ilim kendin bilmektir.", "Yunus Emre", "Anadolu İrfanı"),
            MotivasyonSozu(11, "Bir şeyi basitçe açıklayamıyorsan, onu yeterince iyi anlamamışsın demektir.", "Richard Feynman", "Feynman Tekniği"),
            MotivasyonSozu(12, "İyi bir başlangıç, yarı yarıya başarıdır.", "Aristo", "Felsefe"),
            MotivasyonSozu(13, "Bilgi bir ışık gibidir, onu paylaştıkça çoğalır ve aydınlatır.", "İbn-i Sina", "Tıp & Bilim"),
            MotivasyonSozu(14, "Geçmişi değiştiremezsin, ama bugünkü kararlarınla geleceğini şekillendirebilirsin.", "Seneca", "Stoacı Felsefe"),
            MotivasyonSozu(15, "Sorgulanmamış bir hayat, yaşanmaya değmez.", "Sokrates", "Sokratik Bilge"),
            MotivasyonSozu(16, "Mükemmellikte sınır yoktur, her gün dünden %1 daha iyi olmaya çalış.", "Kaizen Prensibi", "Sürekli Gelişim"),
            MotivasyonSozu(17, "Sabır, acıdır ama meyvesi tatlıdır.", "Aristo", "Felsefe"),
            MotivasyonSozu(18, "İnsan neye odaklanırsa, hayatı oraya doğru büyür.", "Marcus Aurelius", "Stoacı Felsefe"),
            MotivasyonSozu(19, "Hiç kimse başarı merdivenlerini elleri cebinde tırmanmamıştır.", "Sokratik Koç (#12)", "İrade & Emek"),
            MotivasyonSozu(20, "Bugünün çabası, yarının özgürlüğü ve bağımsızlığıdır.", "Günlük Asistan", "Motivasyon Çapası")
        )
    }

    // ── SIRADAKİ SÖZÜ GETİR (DÖNGÜSEL) ──
    fun siradakiSozuGetir(index: Int): MotivasyonSozu {
        val list = varsayilanSozListesi()
        return list[(index.coerceAtLeast(0)) % list.size]
    }

    // ── METİN FORMATLAMA ──
    fun sozMetniFormatla(soz: MotivasyonSozu): String {
        return "📜 \"${soz.soz}\" — ${soz.yazar} [${soz.kategori}]"
    }

    // ── KALICI AYARLAR (PREFERENCES) ──
    fun mansetGosterilsinMi(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_GOSTER, true)
    }

    fun setMansetGosterilsinMi(context: Context, goster: Boolean) {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sp.edit().putBoolean(KEY_GOSTER, goster).apply()
    }

    fun gorunurlukKarari(gosterMi: Boolean): Int {
        return if (gosterMi) View.VISIBLE else View.GONE
    }

    fun sozSabitliMi(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_SABITLI_MI, false)
    }

    fun kisiselSozuKaydet(context: Context, sozMetni: String, yazarAdi: String) {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val temizSoz = if (sozMetni.isBlank()) "Hedefime adım adım yürüyorum!" else sozMetni.trim()
        val temizYazar = if (yazarAdi.isBlank()) "Kişisel Motto" else yazarAdi.trim()
        sp.edit()
            .putString(KEY_OZEL_SOZ, temizSoz)
            .putString(KEY_OZEL_YAZAR, temizYazar)
            .putBoolean(KEY_SABITLI_MI, true)
            .apply()
    }

    fun sabitlemeyiKaldir(context: Context) {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sp.edit().putBoolean(KEY_SABITLI_MI, false).apply()
    }

    fun aktifSozuGetir(context: Context): MotivasyonSozu {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (sozSabitliMi(context)) {
            val ozelSoz = sp.getString(KEY_OZEL_SOZ, "Hedefime adım adım yürüyorum!") ?: "Hedefime adım adım yürüyorum!"
            val ozelYazar = sp.getString(KEY_OZEL_YAZAR, "Kişisel Motto") ?: "Kişisel Motto"
            return MotivasyonSozu(999, ozelSoz, ozelYazar, "📌 Sabitlenmiş Söz")
        }
        val idx = sp.getInt(KEY_SON_INDEX, 0)
        return siradakiSozuGetir(idx)
    }

    fun sonrakiIndexeGec(context: Context): MotivasyonSozu {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val idx = sp.getInt(KEY_SON_INDEX, 0) + 1
        sp.edit().putInt(KEY_SON_INDEX, idx).apply()
        return siradakiSozuGetir(idx)
    }
}
