package com.gunlukasistan.app

import android.content.Context
import android.view.DragEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * v11.10 — A'dan Z'ye Sınırsız Sürükleme ve Taşıma Motoru (`EvrenselTasimaVeSuruklemeMotoru`).
 *
 * Kullanıcının "uygulamanin a dan z ye herseyini sınırsız taşıma istediğim yere yetkisini
 * vermeni istiyorum. Tasimak istediğim herseyin üstüne basılı tutup istediğim yone veya
 * yere sürüklemem yeterli olsun." talimatı doğrultusunda:
 *
 *  1. Bir ekrandaki (Ana Ekran, Bugün, Konular, İlerleme vb.) HER GÖRÜNÜME basılı tutma
 *     ve sürükle-bırak (drag and drop) yetkisi kazandırır.
 *  2. Basılı tutarak açılan menüden kartın gerçek bir bileşen olarak (`EvrenselKartKatalogu`)
 *     diğer sekmelere taşınmasını veya kopyalanmasını yönetir.
 *  3. Hem sırayı yukarı/aşağı butonla hem de sürükleyerek değiştirmeye izin verir.
 */
object EvrenselTasimaVeSuruklemeMotoru {

    /**
     * ViewGroup içindeki tüm çocuk görünümlere A'dan Z'ye sınırsız taşıma
     * ve sürükleme yetkisi tanımlar.
     *
     * v11.13 DÜZELTMESİ: Eskiden `child.id == View.NO_ID && child.tag == null`
     * olan (id'siz ve tag'siz) kartlar ATLANIYORDU → bazı sekmelerde (Konular,
     * Bugün vb. programatik kartlar) uzun basış hiç bağlanmıyor, "taşıyamıyorum"
     * hissi doğuyordu. Artık id/tag olmasa da her kart bağlanır ve görünür
     * ad "Kart #N" olur.
     */
    fun containerIcinSurukleVeTasiKur(
        context: Context?,
        sekmeAnahtari: String,
        container: ViewGroup?,
        bilesenAdBulucu: ((View) -> String)? = null
    ) {
        if (context == null || container == null) return

        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (child == null) continue
            bileseniKur(context, sekmeAnahtari, child, i, container, bilesenAdBulucu)
        }
    }

    /** Tek bir çocuk görünüme uzun basış + sürükle-bırak bağlar. */
    private fun bileseniKur(
        context: Context,
        sekmeAnahtari: String,
        child: View,
        indeks: Int,
        container: ViewGroup,
        bilesenAdBulucu: ((View) -> String)?
    ) {
        val bilesenAd = bilesenAdBulucu?.invoke(child)
            ?: child.tag?.toString()
            ?: "Kart #${indeks + 1}"

        // 1) Uzun Basma: Evrensel Taşıma Menüsü
        child.setOnLongClickListener { v ->
            evrenselTasimaMenusuGoster(context, sekmeAnahtari, bilesenAd, v, container)
            true
        }

        // 2) Sürükle-Bırak (Drag and Drop) Dinleyicisi
        child.setOnDragListener { v, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> true
                DragEvent.ACTION_DRAG_ENTERED -> {
                    v.alpha = 0.7f
                    true
                }
                DragEvent.ACTION_DRAG_EXITED -> {
                    v.alpha = 1.0f
                    true
                }
                DragEvent.ACTION_DROP -> {
                    v.alpha = 1.0f
                    val kaynakView = event.localState as? View
                    if (kaynakView != null && kaynakView != v && kaynakView.parent == container) {
                        val kaynakPos = container.indexOfChild(kaynakView)
                        val hedefPos = container.indexOfChild(v)
                        if (kaynakPos >= 0 && hedefPos >= 0) {
                            container.removeViewAt(kaynakPos)
                            container.addView(kaynakView, hedefPos)
                            siraKaydet(context, sekmeAnahtari, container)
                            Toast.makeText(
                                context,
                                "🔀 '$bilesenAd' sürüklenerek yeni sırasına taşındı!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    true
                }
                DragEvent.ACTION_DRAG_ENDED -> {
                    v.alpha = 1.0f
                    true
                }
                else -> false
            }
        }
    }

    private fun evrenselTasimaMenusuGoster(
        context: Context,
        kaynakSekme: String,
        bilesenAd: String,
        view: View,
        container: ViewGroup
    ) {
        val secenekler = arrayOf(
            "⬆️ Bu Kartı / Bölümü Yukarı Taşı (Sırayı Değiştir)",
            "⬇️ Bu Kartı / Bölümü Aşağı Taşı (Sırayı Değiştir)",
            "⚡ Bu Kartı Başka Bir Sekmeye Gerçek Kart Olarak Taşı (Ana Ekran ⇄ Bugün ⇄ Konular ⇄ İlerleme)",
            "➕ Bu Kartı Başka Bir Sekmeye Gerçek Kart Olarak Kopyala",
            "📐 Kart Boyutunu / Yüksekliğini Değiştir (${GorunumAyar.kartBoyutuAd(context)})",
            "✨ A'dan Z'ye Tüm Sıralama ve Taşıma Yetkilerini Varsayılana Sıfırla"
        )

        MaterialAlertDialogBuilder(context)
            .setTitle("⚡ A'dan Z'ye Taşıma Yetkisi: '$bilesenAd'")
            .setItems(secenekler) { _, idx ->
                when (idx) {
                    0 -> satirTasi(context, kaynakSekme, view, container, true)
                    1 -> satirTasi(context, kaynakSekme, view, container, false)
                    2 -> sekmeArasiGercekKartTasiDiyalogu(context, kaynakSekme, bilesenAd, kopyalaMi = false)
                    3 -> sekmeArasiGercekKartTasiDiyalogu(context, kaynakSekme, bilesenAd, kopyalaMi = true)
                    4 -> kartBoyutuDegistirDiyalogu(context, container)
                    5 -> {
                        SekmeVeVeriTasimaMotoru.siraSifirla(context, kaynakSekme)
                        EvrenselKartKatalogu.varsayilanlaraDon(context)
                        GorunumAyar.setKartBoyutuOlcegi(context, 1)
                        SekmeVeVeriTasimaMotoru.siralamayiVeBoyutuUygula(context, kaynakSekme, container)
                        Toast.makeText(context, "✨ A'dan Z'ye tüm taşıma ve kart yetkileri sıfırlandı!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    private fun satirTasi(
        context: Context,
        sekmeAnahtari: String,
        view: View,
        container: ViewGroup,
        yukariMi: Boolean
    ) {
        val pos = container.indexOfChild(view)
        val hedefPos = if (yukariMi) pos - 1 else pos + 1
        if (hedefPos in 0 until container.childCount) {
            container.removeViewAt(pos)
            container.addView(view, hedefPos)
            siraKaydet(context, sekmeAnahtari, container)
            Toast.makeText(context, "🔀 Kart sırası değiştirildi!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "⚠️ Daha fazla taşınamaz.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun siraKaydet(context: Context, sekmeAnahtari: String, container: ViewGroup) {
        val idler = mutableListOf<String>()
        for (i in 0 until container.childCount) {
            val c = container.getChildAt(i)
            if (c.id != View.NO_ID) {
                idler.add(c.id.toString())
            } else if (c.tag != null) {
                idler.add(c.tag.toString())
            }
        }
        SekmeVeVeriTasimaMotoru.siralamaKaydet(context, sekmeAnahtari, idler)
    }

    private fun sekmeArasiGercekKartTasiDiyalogu(
        context: Context,
        kaynakSekme: String,
        bilesenAd: String,
        kopyalaMi: Boolean
    ) {
        val hedefSekmeler = arrayOf(
            "🏠 Ana Sayfa (home)",
            "☀️ Bugün / Günün Akışı (today)",
            "📚 Konular (topics)",
            "📋 Vakit Planı (plan)",
            "⏱️ Sayaç (timer)"
        )
        val anahtarlar = arrayOf("home", "today", "topics", "plan", "timer")

        MaterialAlertDialogBuilder(context)
            .setTitle("🏷️ Gerçek Kart Hangi Sekmeye Taşınacak?")
            .setItems(hedefSekmeler) { _, idx ->
                val hedefAnahtar = anahtarlar[idx]
                if (hedefAnahtar == kaynakSekme) {
                    Toast.makeText(context, "⚠️ Zaten bu sekmedesiniz.", Toast.LENGTH_SHORT).show()
                } else {
                    // Kart kodunu bul:
                    val kartKodu = bilesenKoduBul(bilesenAd)
                    val (ok, msg) = if (kopyalaMi) {
                        EvrenselKartKatalogu.kartKopyala(context, kartKodu, hedefAnahtar)
                    } else {
                        EvrenselKartKatalogu.kartTasi(context, kartKodu, kaynakSekme, hedefAnahtar)
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

                    // Hedef ekrana geç:
                    val hedefIndeks = when (hedefAnahtar) {
                        "today" -> 2
                        "topics" -> 3
                        "plan" -> 16
                        "timer" -> 4
                        else -> 0
                    }
                    (context as? MainActivity)?.open(hedefIndeks)
                }
            }
            .show()
    }

    private fun bilesenKoduBul(ad: String): String {
        return when {
            ad.contains("Günün Akışı") || ad.contains("Hero") -> "HERO_KARTI"
            ad.contains("Şimdi Ne Yapmalı") -> "SIMDI_NE_YAPMALI"
            ad.contains("Namaz") || ad.contains("Vakit Planı") -> "NAMAZ_KARTI"
            ad.contains("Görevler") || ad.contains("Öncelikler") -> "GOREVLER_KARTI"
            ad.contains("Motivasyon") || ad.contains("Manşet") -> "MOTIVASYON_MANSET"
            ad.contains("Kurs") -> "KURSLAR_KARTI"
            ad.contains("Modül") || ad.contains("İstatistik") -> "MODULLER_OZET"
            ad.contains("Alışkanlık") -> "ALISKANLIK_KARTI"
            ad.contains("Etkinlik") || ad.contains("Takvim") -> "ETKINLIK_KARTI"
            ad.contains("İpucu") -> "IPUCU_KARTI"
            ad.contains("Hızlı Komut") -> "HIZLI_KOMUTLAR"
            ad.contains("Vaktin Sözü") || ad.contains("Hadis") -> "DINI_SOZ_KARTI"
            // v11.13 DÜZELTMESİ: SekmeVeVeriTasimaMotoru ile tutarlı —
            // bilinmeyen kart en etkileşimli olan görev kartına gider (HERO değil).
            else -> "GOREVLER_KARTI"
        }
    }

    private fun kartBoyutuDegistirDiyalogu(context: Context, container: ViewGroup) {
        val secenekler = arrayOf("Kompakt (%85)", "Normal (%100)", "Geniş (%115)", "Devasa (%130)")
        MaterialAlertDialogBuilder(context)
            .setTitle("📐 Kart Boyutu Ölçeğini Seç")
            .setItems(secenekler) { _, idx ->
                GorunumAyar.setKartBoyutuOlcegi(context, idx)
                SekmeVeVeriTasimaMotoru.siralamayiVeBoyutuUygula(context, "home", container)
                Toast.makeText(context, "📐 Kart boyutu '${secenekler[idx]}' olarak ayarlandı!", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
}
