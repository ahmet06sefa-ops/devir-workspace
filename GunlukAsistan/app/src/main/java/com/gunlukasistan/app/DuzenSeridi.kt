package com.gunlukasistan.app

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.color.MaterialColors

/**
 * v10.18 · EKRAN ATÖLYESİ — yerinde düzenleme şeridi.
 *
 * ── Kullanıcının isteği ──
 * "Öğelerin üstüne basılı tutunca boyutlarını, yerlerini
 * değiştirebileyim."
 *
 * ── Nasıl çalışır ──
 * 1) Kullanıcı bir bloğa BASILI TUTAR → düzenleme modu açılır:
 *    blok vurgulu çerçeve alır, altında şerit belirir ve o ekrandaki
 *    tüm blokların çocuk düğmeleri susar ([DuzenBlokLayout] dokunuşu
 *    keser; dokunmak artık "blok seç" demektir).
 * 2) Şeritten: ▲ Yukarı · ▼ Aşağı · Boyut (Kompakt/Normal/Geniş) ·
 *    Katla/Aç · Gizle/Göster · ↺ Sıfırla · ✔ Bitti.
 * 3) Her işlem ANINDA kaydedilir ve ekran yeniden uygulanır;
 *    "Bitti" modu kapatır, düğmeler yeniden çalışır.
 *
 * ── Dürüstlük notu ──
 * Bu, serbest piksel sürüklemesi değildir: ekran dikey bir akışta
 * olduğu için taşıma "bir yukarı/aşağı" adımlarıyla yapılır. Sürükle
 * bırak `ScrollView` kaydırmasıyla çakışırdı (v8.5 kararı hâlâ geçerli);
 * adım taşıma her boyutta güvenlidir. Boyutlar piksel serbestliğinde
 * değil üç kademedir — native bileşenlerin güvenli aralığı budur.
 *
 * Aynı şerit Ana ekran ve Bugün ekranında çalışır; ekrana özgü saklama
 * [Kapi] arkasındadır.
 */
class DuzenSeridi(
    private val c: Context,
    private val kapi: Kapi
) {

    /** Ekranla şerit arasındaki köprü (Ana/Bugün için iki gerçekleşim var). */
    interface Kapi {
        val bloklar: List<AnaEkranDuzen.Blok>
        fun sira(): List<AnaEkranDuzen.Blok>
        fun tasi(kod: String, yon: Int)
        fun gizliMi(kod: String): Boolean
        fun gizle(kod: String, gizli: Boolean)
        fun boyutKademe(kod: String): Int
        fun boyutYaz(kod: String, kademe: Int)
        fun katliMi(kod: String): Boolean
        fun katlaYaz(kod: String, katli: Boolean)
        fun sifirla()
        fun kok(): View?
        fun tazele()
    }

    companion object {
        /** Düzenleme modu açıkken o ekranın fragment kökü (zayıf değil: kısa ömürlü). */
        var aktifKokTutucu: View? = null
            private set
        var duzenModuAktif: Boolean = false
            private set

        fun soyundanMi(v: View, kok: View): Boolean {
            var e = v.parent
            while (e != null) {
                if (e === kok) return true
                e = e.parent
            }
            return false
        }
    }

    private val yog = c.resources.displayMetrics.density
    private var secili: String? = null
    private var seridi: View? = null

    var aktifMi = false
        private set

    // ---------------------------------------------------------------
    // Bağlama: her blok basılı tutmayla modu açar / seçilir
    // ---------------------------------------------------------------

    /** Fragment onViewCreated'de bir kez çağrılır. */
    fun bagla(kok: View) {
        kapi.bloklar.forEach { blok ->
            kok.findViewById<View>(blok.viewId)?.setOnLongClickListener {
                if (!aktifMi) ac(blok.kod) else sec(blok.kod)
                true
            }
        }
    }

    fun ac(kod: String) {
        if (aktifMi) { sec(kod); return }
        aktifMi = true
        duzenModuAktif = true
        aktifKokTutucu = kapi.kok()
        // Dokunuş kesiciler: blokların kendisi "seçim" üretir.
        // (DuzenBlokLayout çocuklara ulaşımı zaten kesiyor.)
        kapi.bloklar.forEach { blok ->
            gorunum(blok.kod)?.setOnTouchListener { _, ev ->
                if (ev.actionMasked == MotionEvent.ACTION_DOWN) sec(blok.kod)
                true
            }
        }
        sec(kod)
    }

    fun sec(kod: String) {
        if (!aktifMi) return
        secili = kod
        kenarliklariCiz()
        seridiYerlestir()
        gorunum(kod)?.let { runCatching { Titresim.tik(it) } }
    }

    fun kapat() {
        if (!aktifMi) return
        aktifMi = false
        duzenModuAktif = false
        aktifKokTutucu = null
        secili = null
        seridi?.let { (it.parent as? ViewGroup)?.removeView(it) }
        seridi = null
        kapi.bloklar.forEach { blok ->
            gorunum(blok.kod)?.let { g ->
                g.setOnTouchListener(null)
                g.foreground = null
            }
        }
    }

    // ---------------------------------------------------------------
    // Yardımcılar
    // ---------------------------------------------------------------

    private fun blokBul(kod: String?) = kapi.bloklar.firstOrNull { it.kod == kod }

    private fun gorunum(kod: String): View? =
        blokBul(kod)?.let { kapi.kok()?.findViewById<View>(it.viewId) }

    private fun kap(): LinearLayout? =
        kapi.kok()?.findViewById<View>(kapi.bloklar.first().viewId)?.parent as? LinearLayout

    private val birincilRengi: Int by lazy {
        MaterialColors.getColor(
            TextView(c),
            com.google.android.material.R.attr.colorPrimary, 0
        )
    }

    private fun birincil(): Int = birincilRengi

    // ---------------------------------------------------------------
    // Görsel durum
    // ---------------------------------------------------------------

    private fun kenarliklariCiz() {
        kapi.bloklar.forEach { blok ->
            val g = gorunum(blok.kod) ?: return@forEach
            g.foreground = if (blok.kod == secili) {
                GradientDrawable().apply {
                    cornerRadius = 14 * yog
                    setStroke((2.5f * yog).toInt(), birincil())
                }
            } else null
        }
    }

    private fun seridiYerlestir() {
        val kap = kap() ?: return
        val blokG = gorunum(secili ?: return) ?: return
        if (seridi == null) seridi = seridiOlustur()
        (seridi?.parent as? ViewGroup)?.removeView(seridi)
        val idx = kap.indexOfChild(blokG)
        kap.addView(seridi, (idx + 1).coerceAtMost(kap.childCount))
        seridiTazele()
    }

    // ---------------------------------------------------------------
    // İşlemler (her biri anında kaydeder + yeniden uygular)
    // ---------------------------------------------------------------

    private fun islemTasi(yon: Int) {
        val kod = secili ?: return
        kapi.tasi(kod, yon)
        kapi.tazele()
        seridiYerlestir()
    }

    private fun islemBoyut(kademe: Int) {
        val kod = secili ?: return
        kapi.boyutYaz(kod, kademe)
        kapi.tazele()
        seridiTazele()
    }

    private fun islemKatla() {
        val kod = secili ?: return
        if (blokBul(kod)?.katlanabilir != true) return
        kapi.katlaYaz(kod, !kapi.katliMi(kod))
        kapi.tazele()
        seridiTazele()
    }

    private fun islemGizle() {
        val kod = secili ?: return
        if (blokBul(kod)?.zorunlu == true && !kapi.gizliMi(kod)) return
        kapi.gizle(kod, !kapi.gizliMi(kod))
        kapi.tazele()
        seridiYerlestir()
    }

    private fun islemSifirla() {
        kapi.sifirla()
        kapi.tazele()
        kapat()
    }

    // ---------------------------------------------------------------
    // Şerit görünümü
    // ---------------------------------------------------------------

    private fun seridiOlustur(): View {
        val dis = LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            // İçteki apply'ın `this`'ine karışmamak için renk önce alınır
            val zeminRengi = MaterialColors.getColor(
                this,
                com.google.android.material.R.attr.colorSecondaryContainer, 0
            )
            background = GradientDrawable().apply {
                cornerRadius = 14 * yog
                setColor(zeminRengi)
            }
            setPadding(
                (8 * yog).toInt(), (6 * yog).toInt(),
                (8 * yog).toInt(), (6 * yog).toInt()
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (6 * yog).toInt()
                bottomMargin = (6 * yog).toInt()
            }
        }
        return android.widget.HorizontalScrollView(c).apply {
            isHorizontalScrollBarEnabled = false
            addView(dis)
        }
    }

    /** Şeridin içeriğini mevcut seçime göre yeniden kurar. */
    private fun seridiTazele() {
        val blok = blokBul(secili) ?: return
        val dis = ((seridi as? android.widget.HorizontalScrollView)
            ?.getChildAt(0) as? LinearLayout) ?: return
        dis.removeAllViews()

        dis.addView(TextView(c).apply {
            text = blok.simge + " " + c.getString(blok.baslikRes)
            textSize = 13f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(birincil())
            setPadding(0, 0, (10 * yog).toInt(), 0)
        })

        dis.addView(cip(c.getString(R.string.du_yukari), false) { islemTasi(-1) })
        dis.addView(cip(c.getString(R.string.du_asagi), false) { islemTasi(+1) })

        val boyut = kapi.boyutKademe(blok.kod)
        dis.addView(cip(c.getString(R.string.du_kompakt), boyut == 0) { islemBoyut(0) })
        dis.addView(cip(c.getString(R.string.du_normal), boyut == 1) { islemBoyut(1) })
        dis.addView(cip(c.getString(R.string.du_genis), boyut == 2) { islemBoyut(2) })

        if (blok.katlanabilir) {
            val katli = kapi.katliMi(blok.kod)
            dis.addView(
                cip(c.getString(if (katli) R.string.du_ac else R.string.du_katla), katli) {
                    islemKatla()
                }
            )
        }

        val gizli = kapi.gizliMi(blok.kod)
        if (!blok.zorunlu || gizli) {
            dis.addView(
                cip(c.getString(if (gizli) R.string.du_goster else R.string.du_gizle), gizli) {
                    islemGizle()
                }
            )
        }

        dis.addView(cip(c.getString(R.string.du_sifirla), false) { sifirlaSor() })
        dis.addView(cip(c.getString(R.string.du_bitti), true) { kapat() })
    }

    private fun sifirlaSor() {
        androidx.appcompat.app.AlertDialog.Builder(c)
            .setTitle(R.string.du_sifirla_sor)
            .setMessage(R.string.du_sifirla_sor_mesaj)
            .setPositiveButton(R.string.du_sifirla_evet) { _, _ -> islemSifirla() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun cip(metin: String, secili: Boolean, tikla: () -> Unit) =
        TextView(c).apply {
            text = metin
            textSize = 12.5f
            gravity = Gravity.CENTER
            setPadding(
                (12 * yog).toInt(), (8 * yog).toInt(),
                (12 * yog).toInt(), (8 * yog).toInt()
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = (6 * yog).toInt() }
            val vurgu = birincil()
            background = GradientDrawable().apply {
                cornerRadius = 16 * yog
                if (secili) {
                    setColor((vurgu and 0x00FFFFFF) or 0x33000000)
                    setStroke((1.5f * yog).toInt(), vurgu)
                } else {
                    setColor(0x14888888)
                }
            }
            if (secili) setTextColor(vurgu)
            isClickable = true
            setOnClickListener { tikla() }
        }
}
