package com.gunlukasistan.app

import android.content.Context

/**
 * v7.74 — Görev etiketleri.
 *
 * ── Kullanıcının isteği (10 iyileştirmeden 6. madde) ──
 * "Renk kodlu etiket (İş · Ev · Okul · Acil), etikete göre filtre ve
 *  renkli şerit."
 *
 * ── Tasarım kararı: sabit liste, kullanıcı tanımlı değil ──
 * Etiket yönetimi ekranı eklemek yerine 6 hazır etiket sunuldu.
 * Gerekçe: kullanıcı tek kişi ve günlük kullanım için 6 kategori
 * fazlasıyla yeterli; etiket oluşturma/silme/renk seçme ekranı
 * karmaşıklığı arttırıp faydayı arttırmıyordu.
 *
 * Kodlama: `Task.etiket` alanında **tek karakter** saklanır
 * ("i" = İş, "e" = Ev …). Boş = etiketsiz. Tek karakter seçildi
 * çünkü JSON'da yer kaplamıyor ve eski kayıtlarla uyumlu.
 */
object Etiket {

    /** Etiketsiz görevler için. */
    const val YOK = ""

    /**
     * Bir etiket tanımı.
     * @param kod tek karakterlik saklama anahtarı
     * @param renk sol kenardaki şerit rengi
     */
    data class Tanim(
        val kod: String,
        val adRes: Int,
        val renk: Int,
        val emoji: String
    )

    /** Hazır etiketler — sıralama listede de aynı. */
    val hepsi = listOf(
        Tanim("i", R.string.et_is, 0xFF4C7BD9.toInt(), "💼"),
        Tanim("e", R.string.et_ev, 0xFF5FA855.toInt(), "🏠"),
        Tanim("o", R.string.et_okul, 0xFF9B6BD6.toInt(), "🎓"),
        Tanim("s", R.string.et_saglik, 0xFF3FA9A0.toInt(), "🩺"),
        Tanim("a", R.string.et_alisveris, 0xFFD9922E.toInt(), "🛒"),
        Tanim("c", R.string.et_acil, 0xFFD64545.toInt(), "🔴")
    )

    fun bul(kod: String?): Tanim? =
        if (kod.isNullOrBlank()) null else hepsi.firstOrNull { it.kod == kod }

    /** Kullanıcıya gösterilecek ad ("💼 İş"). */
    fun ad(context: Context, kod: String?): String {
        val t = bul(kod) ?: return context.getString(R.string.et_yok)
        return t.emoji + " " + context.getString(t.adRes)
    }

    /** Şerit rengi; etiketsizde saydam. */
    fun renk(kod: String?): Int = bul(kod)?.renk ?: android.graphics.Color.TRANSPARENT

    /** Etiket seçme penceresi açar. */
    fun sec(context: Context, mevcut: String?, secildi: (String) -> Unit) {
        val adlar = mutableListOf(context.getString(R.string.et_yok))
        val kodlar = mutableListOf(YOK)
        hepsi.forEach {
            adlar.add(it.emoji + "  " + context.getString(it.adRes))
            kodlar.add(it.kod)
        }
        val simdiki = kodlar.indexOf(mevcut ?: YOK).coerceAtLeast(0)
        com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
            .setTitle(R.string.et_sec)
            .setSingleChoiceItems(adlar.toTypedArray(), simdiki) { d, hangi ->
                d.dismiss()
                secildi(kodlar[hangi])
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
