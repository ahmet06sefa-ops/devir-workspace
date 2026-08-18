package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * v7.81 — Bir konu maddesinin derinlemesine anlatım ekranı.
 *
 * ── Kullanıcının isteği ──
 * "Konuyu anlamam için her şeyi ekle" — bu ekran o "her şey"i tek yerde
 * toplar:
 *   · Uzun, bölümlere ayrılmış ders anlatımı
 *   · Kullanıcının kendi PDF'lerinden alıntılar (uydurma kaynak yok)
 *   · Görsel önerileri + tek dokunuşla web'de arama
 *   · Özet
 *   · PDF olarak dışa aktarma / paylaşma
 *   · "Anlamadım, daha basit anlat" düğmesi
 *
 * ── Neden önbellek ──
 * Anlatım üretmek 10-20 saniye sürüyor ve kota harcıyor. Bir kez üretilen
 * anlatım [KonuUretici.anlatimKaydet] ile saklanır; ekran ikinci açılışta
 * anında gelir. "Yeniden üret" düğmesi bilinçli bir tercih olarak durur.
 */
class KonuAnlatimActivity : AppCompatActivity() {

    companion object {
        /** v7.84: metin seçim menüsündeki "Ne demek?" öğesi. */
        private const val MENU_SOZLUK = 9001

        private const val EXTRA_MADDE = "madde"
        private const val EXTRA_KONU = "konu"

        fun ac(context: Context, madde: String, konuBasligi: String = "") {
            context.startActivity(
                Intent(context, KonuAnlatimActivity::class.java)
                    .putExtra(EXTRA_MADDE, madde)
                    .putExtra(EXTRA_KONU, konuBasligi)
            )
        }
    }

    private val d get() = resources.displayMetrics.density
    private lateinit var kap: LinearLayout

    /** v7.83: okuma zemini bunun üzerine uygulanır. */
    private lateinit var kokGorunum: ScrollView

    private var madde = ""
    private var konuBasligi = ""
    private var anlatim: KonuUretici.Anlatim? = null
    private var yukleniyor = false

    /**
     * v8.6 · Öneri 27 — Kullanıcının yazı boyutu tercihini uygular.
     *
     * `Configuration.fontScale` tüm `sp` birimlerini bir kerede
     * ölçekliyor; 71 layout'a tek tek dokunmaya gerek kalmıyor.
     */
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(GorunumAyar.yaziOlcegiUygula(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.styleFor(this))
        ThemeManager.applyAccent(this)
        // v8.3 · Öneri 10: Material You (açıksa duvar kâğıdı paleti)
        ThemeManager.dinamikRengiUygula(this)
        super.onCreate(savedInstanceState)

        madde = intent?.getStringExtra(EXTRA_MADDE).orEmpty()
        konuBasligi = intent?.getStringExtra(EXTRA_KONU).orEmpty()
        if (madde.isBlank()) {
            finish()
            return
        }
        title = madde

        kap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((16 * d).toInt(), (16 * d).toInt(), (16 * d).toInt(), (32 * d).toInt())
        }
        kokGorunum = ScrollView(this).apply { addView(kap) }
        setContentView(kokGorunum)
        zeminiUygula()

        // Önbellekte varsa anında göster
        anlatim = KonuUretici.anlatimOku(this, madde)
        if (anlatim == null) uretmeyeBasla(uzunluk = 2) else ciz()
    }

    override fun onResume() {
        super.onResume()
        ekranBayraginiAyarla()
    }

    /** Okurken ekran sönmesin — kullanıcı sürekli dokunmak zorunda kalmasın. */
    private fun ekranBayraginiAyarla() {
        if (OkumaAyar.ekranAcikKalsin(this)) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    /** Seçili okuma zeminini uygular. */
    private fun zeminiUygula() {
        val mod = OkumaAyar.zemin(this)
        val renk = OkumaAyar.zeminRengi(mod) ?: MaterialColors.getColor(
            this, com.google.android.material.R.attr.colorSurface, 0
        )
        kokGorunum.setBackgroundColor(renk)
    }

    /** Zemine uygun gövde metni rengi (tema modunda null = varsayılan). */
    private fun metinRengi(): Int? = OkumaAyar.metinRengi(OkumaAyar.zemin(this))

    // ═══════════════════════════════════════════════════════════════
    // ÜRETİM
    // ═══════════════════════════════════════════════════════════════

    private fun uretmeyeBasla(uzunluk: Int, pdfDestegi: Boolean = true) {
        if (yukleniyor) return
        if (!AiSettings.isReady(this)) {
            ciz()
            Toast.makeText(this, R.string.kn_ai_hazir_degil, Toast.LENGTH_LONG).show()
            return
        }
        yukleniyor = true
        ciz()

        Performans.arkaPlan {
            val sonuc = KonuUretici.anlat(this, madde, konuBasligi, uzunluk, pdfDestegi)
            Performans.anaIs {
                if (isFinishing || isDestroyed) return@anaIs
                yukleniyor = false
                if (sonuc.ok) {
                    anlatim = sonuc
                    KonuUretici.anlatimKaydet(this, madde, sonuc)
                } else {
                    Toast.makeText(
                        this,
                        sonuc.hata.ifBlank { getString(R.string.ku_uretilemedi) },
                        Toast.LENGTH_LONG
                    ).show()
                }
                ciz()
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ÇİZİM
    // ═══════════════════════════════════════════════════════════════

    private fun ciz() {
        kap.removeAllViews()

        val govdeBoyut = OkumaAyar.yaziBoyutu(this)
        val satirAra = OkumaAyar.satirAraligi(this)
        val renk = metinRengi()
        val solukRenk = OkumaAyar.soluk(OkumaAyar.zemin(this))

        // Başlık
        kap.addView(TextView(this).apply {
            text = anlatim?.baslik?.ifBlank { madde } ?: madde
            textSize = OkumaAyar.anaBaslikBoyutu(this@KonuAnlatimActivity)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setLineSpacing(0f, 1.15f)
            renk?.let { setTextColor(it) }
        })
        if (konuBasligi.isNotBlank()) {
            kap.addView(TextView(this).apply {
                text = konuBasligi
                textSize = 12.5f
                alpha = 0.7f
                setPadding(0, (4 * d).toInt(), 0, 0)
                solukRenk?.let { setTextColor(it) }
            })
        }

        // Yükleniyor
        if (yukleniyor) {
            kap.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(0, (50 * d).toInt(), 0, (40 * d).toInt())
                addView(ProgressBar(this@KonuAnlatimActivity).apply {
                    isIndeterminate = true
                })
                addView(TextView(this@KonuAnlatimActivity).apply {
                    text = getString(R.string.ka_uretiliyor)
                    textSize = 13.5f
                    gravity = Gravity.CENTER
                    setPadding(0, (16 * d).toInt(), 0, 0)
                })
                addView(TextView(this@KonuAnlatimActivity).apply {
                    text = getString(R.string.ka_uretiliyor_alt)
                    textSize = 11.5f
                    alpha = 0.7f
                    gravity = Gravity.CENTER
                    setPadding(0, (6 * d).toInt(), 0, 0)
                })
            })
            return
        }

        val a = anlatim
        if (a == null) {
            ayirici()
            kap.addView(bilgi(getString(R.string.ka_henuz_yok)))
            kap.addView(dugme(getString(R.string.ka_uret), vurgulu = true) {
                uretmeyeBasla(2)
            })
            return
        }

        // ── Okuma ayarları çubuğu (v7.83) ──────────────────────────
        kap.addView(okumaCubugu())

        // ── Bölümler ───────────────────────────────────────────────
        ayirici()
        a.bolumler.forEach { b ->
            if (b.baslik.isNotBlank() && b.baslik != "—") {
                kap.addView(TextView(this).apply {
                    text = b.baslik
                    textSize = OkumaAyar.basligBoyutu(this@KonuAnlatimActivity)
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setPadding(0, (14 * d).toInt(), 0, (7 * d).toInt())
                    setTextColor(
                        MaterialColors.getColor(
                            this@KonuAnlatimActivity,
                            com.google.android.material.R.attr.colorPrimary, 0
                        )
                    )
                })
            }
            kap.addView(TextView(this).apply {
                text = b.metin
                textSize = govdeBoyut
                setLineSpacing(0f, satirAra)
                setTextIsSelectable(true)
                setPadding(0, 0, 0, (8 * d).toInt())
                renk?.let { setTextColor(it) }
                // v7.84: kelime seçince "Ne demek?" seçeneği çıksın
                customSelectionActionModeCallback = sozlukSecimi(this)
                // İki yana yaslama yalnızca Android 8+ destekliyor
                if (OkumaAyar.yaslaMetin(this@KonuAnlatimActivity) &&
                    android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
                ) {
                    justificationMode = android.text.Layout.JUSTIFICATION_MODE_INTER_WORD
                }
            })
        }

        // ── Özet ───────────────────────────────────────────────────
        if (a.ozet.isNotBlank()) {
            ayirici()
            kap.addView(baslikKucuk(getString(R.string.ka_ozet)))
            kap.addView(kartSar(TextView(this).apply {
                text = a.ozet
                textSize = govdeBoyut
                setLineSpacing(0f, satirAra + 0.08f)
                setTextIsSelectable(true)
                setPadding(
                    (14 * d).toInt(), (14 * d).toInt(),
                    (14 * d).toInt(), (14 * d).toInt()
                )
            }))
        }

        // ── Görsel önerileri ───────────────────────────────────────
        if (a.gorseller.isNotEmpty()) {
            ayirici()
            kap.addView(baslikKucuk(getString(R.string.ka_gorseller)))
            kap.addView(bilgi(getString(R.string.ka_gorseller_alt)))
            a.gorseller.forEach { g -> kap.addView(gorselKarti(g)) }
        }

        // ── Kaynak alıntıları ──────────────────────────────────────
        if (a.kaynakAlintilari.isNotEmpty()) {
            ayirici()
            kap.addView(baslikKucuk(getString(R.string.ka_kaynaklar)))
            kap.addView(bilgi(getString(R.string.ka_kaynaklar_alt)))
            a.kaynakAlintilari.forEach { k -> kap.addView(alintiKarti(k)) }
        }

        // ── Eylemler ───────────────────────────────────────────────
        ayirici()

        // v7.82: anlatımdan sınav
        val quizVar = KonuUretici.quizVarMi(this, madde)
        kap.addView(
            dugme(
                getString(if (quizVar) R.string.kq_sinava_gir else R.string.kq_uret),
                vurgulu = true
            ) { if (quizVar) sinavaGir() else quizUret() }
        )
        if (quizVar) {
            kap.addView(dugme(getString(R.string.kq_yeniden)) { quizUret() })
        }

        // v7.82: sesli anlatım
        kap.addView(dugme(sesliDugmeMetni()) { sesliOkuyaBasla() })

        kap.addView(dugme(getString(R.string.ka_basitlestir)) { uretmeyeBasla(1) })
        kap.addView(dugme(getString(R.string.ka_daha_detay)) { uretmeyeBasla(2) })
        kap.addView(dugme(getString(R.string.ka_pdf), vurgulu = true) { pdfCikar() })
        kap.addView(dugme(getString(R.string.ka_web_ara)) { webAra(madde) })
        kap.addView(dugme(getString(R.string.ka_yeniden)) {
            KonuUretici.anlatimSil(this, madde)
            anlatim = null
            uretmeyeBasla(2)
        })
    }

    // ── Kartlar ────────────────────────────────────────────────────

    private fun gorselKarti(g: KonuUretici.GorselFikri): View {
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((14 * d).toInt(), (12 * d).toInt(), (14 * d).toInt(), (12 * d).toInt())
        }
        ic.addView(TextView(this).apply {
            text = g.aciklama
            textSize = 13.5f
            setLineSpacing(0f, 1.3f)
        })
        ic.addView(TextView(this).apply {
            text = getString(R.string.ka_gorsel_ara, g.aramaSorgusu)
            textSize = 12f
            setPadding(0, (7 * d).toInt(), 0, 0)
            setTextColor(
                MaterialColors.getColor(
                    this@KonuAnlatimActivity,
                    com.google.android.material.R.attr.colorPrimary, 0
                )
            )
        })
        return kartSar(ic).apply {
            isClickable = true
            setOnClickListener { gorselAra(g.aramaSorgusu) }
        }
    }

    private fun alintiKarti(k: PdfArama.Sonuc): View {
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((14 * d).toInt(), (12 * d).toInt(), (14 * d).toInt(), (12 * d).toInt())
        }
        ic.addView(TextView(this).apply {
            text = k.dersAdi + "  ·  " + getString(R.string.kp_sayfa, k.sayfa + 1)
            textSize = 12f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            alpha = 0.85f
        })
        ic.addView(TextView(this).apply {
            text = k.parca.take(320).replace("\n", " ")
            textSize = 12.5f
            alpha = 0.85f
            setLineSpacing(0f, 1.3f)
            setPadding(0, (6 * d).toInt(), 0, 0)
        })
        return kartSar(ic).apply {
            isClickable = true
            setOnClickListener {
                // Kaynağın geçtiği sayfayı doğrudan aç
                runCatching {
                    startActivity(
                        Intent(this@KonuAnlatimActivity, LessonPdfActivity::class.java).apply {
                            putExtra(LessonPdfActivity.EXTRA_ASSET, k.assetPath)
                            putExtra(LessonPdfActivity.EXTRA_TITLE, k.dersAdi)
                            putExtra(LessonPdfActivity.EXTRA_SUB, k.kursAdi)
                            putExtra(LessonPdfActivity.EXTRA_LESSON_ID, k.lessonId)
                            putExtra(LessonPdfActivity.EXTRA_START_PAGE, k.sayfa)
                        }
                    )
                }.onFailure {
                    Toast.makeText(
                        this@KonuAnlatimActivity, R.string.ka_pdf_acilamadi, Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // EYLEMLER
    // ═══════════════════════════════════════════════════════════════

    private fun pdfCikar() {
        val a = anlatim ?: return
        val bekle = MaterialAlertDialogBuilder(this)
            .setMessage(R.string.ka_pdf_hazirlaniyor)
            .setCancelable(false)
            .create()
        bekle.show()

        Performans.arkaPlan {
            val dosya = KonuPdf.tekAnlatim(this, a, konuBasligi.ifBlank { a.baslik })
            Performans.anaIs {
                if (isFinishing || isDestroyed) return@anaIs
                runCatching { bekle.dismiss() }
                if (dosya == null) {
                    Toast.makeText(this, R.string.ka_pdf_hata, Toast.LENGTH_LONG).show()
                } else {
                    pdfSecenekleri(dosya)
                }
            }
        }
    }

    private fun pdfSecenekleri(dosya: java.io.File) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.ka_pdf_hazir)
            .setMessage(getString(R.string.ka_pdf_yol, dosya.name))
            .setPositiveButton(R.string.ka_pdf_ac) { _, _ -> pdfAc(dosya) }
            .setNegativeButton(R.string.ka_pdf_paylas) { _, _ -> pdfPaylas(dosya) }
            .setNeutralButton(R.string.ok, null)
            .show()
    }

    private fun pdfAc(dosya: java.io.File) {
        try {
            val uri = KonuPdf.uriVer(this, dosya)
            startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            )
        } catch (e: Exception) {
            Toast.makeText(this, R.string.ka_pdf_okuyucu_yok, Toast.LENGTH_LONG).show()
        }
    }

    private fun pdfPaylas(dosya: java.io.File) {
        try {
            val uri = KonuPdf.uriVer(this, dosya)
            startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                    getString(R.string.ka_pdf_paylas)
                )
            )
        } catch (e: Exception) {
            Toast.makeText(this, R.string.ka_pdf_hata, Toast.LENGTH_SHORT).show()
        }
    }

    /** Görsel araması — tarayıcıda resim sekmesini açar. */
    private fun gorselAra(sorgu: String) {
        val q = android.net.Uri.encode(sorgu)
        acWeb("https://www.google.com/search?tbm=isch&q=$q")
    }

    private fun webAra(sorgu: String) {
        val q = android.net.Uri.encode(sorgu)
        acWeb("https://www.google.com/search?q=$q")
    }

    private fun acWeb(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
        } catch (e: Exception) {
            Toast.makeText(this, R.string.ka_tarayici_yok, Toast.LENGTH_SHORT).show()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // v7.84 — TERİM SÖZLÜĞÜ
    // ═══════════════════════════════════════════════════════════════

    /**
     * Metin seçim menüsüne "Ne demek?" ekler.
     *
     * ── Neden seçim menüsü ──
     * Alternatifler: (a) her terimi tıklanabilir yapmak — hangi kelimenin
     * terim olduğunu bilemeyiz, modelden işaretlemesini istemek kırılgan;
     * (b) ayrı arama kutusu — kullanıcı kelimeyi elle yazmak zorunda kalır.
     * Seçim menüsü ikisinin de sorununu çözüyor: kullanıcı hangi kelimeyi
     * merak ettiyse onu seçiyor, sistem menüsü zaten oradaymış gibi geliyor.
     */
    private fun sozlukSecimi(hedef: TextView) = object : android.view.ActionMode.Callback {
        override fun onCreateActionMode(
            mode: android.view.ActionMode?,
            menu: android.view.Menu?
        ): Boolean {
            menu?.add(0, MENU_SOZLUK, 0, getString(R.string.sz_ne_demek))
            return true
        }

        override fun onPrepareActionMode(
            mode: android.view.ActionMode?,
            menu: android.view.Menu?
        ): Boolean = false

        override fun onActionItemClicked(
            mode: android.view.ActionMode?,
            item: android.view.MenuItem?
        ): Boolean {
            if (item?.itemId != MENU_SOZLUK) return false
            val bas = hedef.selectionStart
            val son = hedef.selectionEnd
            if (bas in 0 until son) {
                val secilen = hedef.text.substring(bas, son).trim()
                if (secilen.isNotBlank()) terimAcikla(secilen)
            }
            mode?.finish()
            return true
        }

        override fun onDestroyActionMode(mode: android.view.ActionMode?) {}
    }

    /**
     * Seçilen terimi açıklar ve sözlüğe kaydeder.
     *
     * Sözlükte varsa AI'ya gidilmez — anında açılır, kota harcanmaz.
     */
    private fun terimAcikla(terim: String) {
        val kisaltilmis = terim.take(80)

        // Önbellekte varsa anında göster
        Sozluk.bul(this, kisaltilmis)?.let {
            Sozluk.bakildiArtir(this, kisaltilmis)
            terimGoster(it, onbellekten = true)
            return
        }

        if (!AiSettings.isReady(this)) {
            Toast.makeText(this, R.string.kn_ai_hazir_degil, Toast.LENGTH_LONG).show()
            return
        }

        val bekle = MaterialAlertDialogBuilder(this)
            .setMessage(getString(R.string.sz_araniyor, kisaltilmis))
            .setCancelable(false)
            .create()
        bekle.show()

        Performans.arkaPlan {
            val sonuc = Sozluk.acikla(
                this, kisaltilmis,
                baglam = konuBasligi.ifBlank { anlatim?.baslik.orEmpty() }
            )
            Performans.anaIs {
                if (isFinishing || isDestroyed) return@anaIs
                runCatching { bekle.dismiss() }
                if (sonuc.ok && sonuc.terim != null) {
                    terimGoster(sonuc.terim, sonuc.onbellekten)
                } else {
                    Toast.makeText(
                        this,
                        sonuc.hata.ifBlank { getString(R.string.sz_anlasilmadi) },
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun terimGoster(t: Sozluk.Terim, onbellekten: Boolean) {
        val govde = buildString {
            append(t.kisa)
            if (t.uzun.isNotBlank() && t.uzun != t.kisa) {
                append("\n\n").append(t.uzun)
            }
            if (onbellekten && t.bakildi > 1) {
                append("\n\n").append(getString(R.string.sz_kayitliydi, t.bakildi))
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(t.terim)
            .setMessage(govde)
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(
                if (t.yildiz) R.string.sz_yildiz_kaldir else R.string.sz_yildizla
            ) { _, _ ->
                val yeni = Sozluk.yildizDegistir(this, t.terim)
                Toast.makeText(
                    this,
                    if (yeni) R.string.sz_yildizlandi else R.string.sz_yildiz_kalkti,
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNeutralButton(R.string.sz_sozluk) { _, _ -> SozlukActivity.ac(this) }
            .show()
    }

    // ═══════════════════════════════════════════════════════════════
    // v7.83 — OKUMA AYARLARI
    // ═══════════════════════════════════════════════════════════════

    /**
     * Metnin üstündeki hızlı okuma çubuğu: A- / A+ / zemin / diğer.
     *
     * Ayarları ayrı bir ekrana koymak yerine buraya yerleştirdim; punto
     * ayarlamak metne bakarken yapılan bir iş, ekran değiştirip geri
     * dönmek akışı bozar.
     */
    private fun okumaCubugu(): View {
        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((6 * d).toInt(), (4 * d).toInt(), (6 * d).toInt(), (4 * d).toInt())
        }

        fun kucukDugme(metin: String, agirlik: Float, tikla: () -> Unit) =
            TextView(this).apply {
                text = metin
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(0, (10 * d).toInt(), 0, (10 * d).toInt())
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, agirlik
                )
                background = android.graphics.drawable.RippleDrawable(
                    android.content.res.ColorStateList.valueOf(0x22888888), null, null
                )
                isClickable = true
                setOnClickListener { tikla() }
                OkumaAyar.soluk(OkumaAyar.zemin(this@KonuAnlatimActivity))
                    ?.let { setTextColor(it) }
            }

        ic.addView(kucukDugme("A−", 1f) {
            OkumaAyar.kucult(this)
            ciz()
        })
        ic.addView(kucukDugme("A+", 1f) {
            OkumaAyar.buyut(this)
            ciz()
        })
        ic.addView(kucukDugme(
            OkumaAyar.zeminAdi(this, OkumaAyar.zemin(this)), 1.6f
        ) { zeminSec() })
        ic.addView(kucukDugme("⚙", 0.7f) { okumaAyarPaneli() })

        return kartSar(ic)
    }

    private fun zeminSec() {
        val modlar = intArrayOf(
            OkumaAyar.ZEMIN_TEMA, OkumaAyar.ZEMIN_ACIK, OkumaAyar.ZEMIN_SEPYA,
            OkumaAyar.ZEMIN_KOYU, OkumaAyar.ZEMIN_SIYAH
        )
        val adlar = modlar.map { OkumaAyar.zeminAdi(this, it) }.toTypedArray()
        val simdiki = modlar.indexOf(OkumaAyar.zemin(this)).coerceAtLeast(0)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.oa_zemin)
            .setSingleChoiceItems(adlar, simdiki) { dlg, hangi ->
                OkumaAyar.setZemin(this, modlar[hangi])
                dlg.dismiss()
                zeminiUygula()
                ciz()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** Satır aralığı, ekran açık kalsın, iki yana yasla. */
    private fun okumaAyarPaneli() {
        val kutu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((22 * d).toInt(), (12 * d).toInt(), (22 * d).toInt(), 0)
        }

        kutu.addView(TextView(this).apply {
            text = getString(R.string.oa_satir_ara)
            textSize = 13f
            alpha = 0.8f
            setPadding(0, 0, 0, (6 * d).toInt())
        })

        val araDegerler = floatArrayOf(1.15f, 1.42f, 1.7f, 2.0f)
        val araAdlar = arrayOf(
            getString(R.string.oa_ara_sik), getString(R.string.oa_ara_normal),
            getString(R.string.oa_ara_genis), getString(R.string.oa_ara_cok_genis)
        )
        val araCipleri = mutableListOf<TextView>()
        val araSatiri = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        araAdlar.forEachIndexed { i, ad ->
            val cip = TextView(this).apply {
                text = ad
                textSize = 12f
                gravity = Gravity.CENTER
                setPadding(0, (9 * d).toInt(), 0, (9 * d).toInt())
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                ).apply { rightMargin = (4 * d).toInt() }
                isClickable = true
            }
            araCipleri.add(cip)
            araSatiri.addView(cip)
        }
        fun araBoya() {
            val simdi = OkumaAyar.satirAraligi(this)
            araCipleri.forEachIndexed { i, cip ->
                val secili = kotlin.math.abs(araDegerler[i] - simdi) < 0.05f
                cip.setBackgroundColor(if (secili) 0x332196F3 else 0x11888888)
            }
        }
        araCipleri.forEachIndexed { i, cip ->
            cip.setOnClickListener {
                OkumaAyar.setSatirAraligi(this, araDegerler[i])
                araBoya()
            }
        }
        araBoya()
        kutu.addView(araSatiri)

        // Anahtarlar
        fun anahtar(ad: String, alt: String, acik: Boolean, degisti: (Boolean) -> Unit) {
            val satir = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, (14 * d).toInt(), 0, 0)
            }
            satir.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                )
                addView(TextView(this@KonuAnlatimActivity).apply {
                    text = ad
                    textSize = 14f
                })
                addView(TextView(this@KonuAnlatimActivity).apply {
                    text = alt
                    textSize = 11.5f
                    alpha = 0.7f
                })
            })
            satir.addView(
                com.google.android.material.materialswitch.MaterialSwitch(this).apply {
                    isChecked = acik
                    setOnCheckedChangeListener { _, v -> degisti(v) }
                }
            )
            kutu.addView(satir)
        }

        anahtar(
            getString(R.string.oa_ekran_acik), getString(R.string.oa_ekran_acik_alt),
            OkumaAyar.ekranAcikKalsin(this)
        ) {
            OkumaAyar.setEkranAcikKalsin(this, it)
            ekranBayraginiAyarla()
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            anahtar(
                getString(R.string.oa_yasla), getString(R.string.oa_yasla_alt),
                OkumaAyar.yaslaMetin(this)
            ) { OkumaAyar.setYaslaMetin(this, it) }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.oa_baslik)
            .setView(ScrollView(this).apply { addView(kutu) })
            .setPositiveButton(R.string.ok) { _, _ -> ciz() }
            .setOnDismissListener { ciz() }
            .show()
    }

    // ═══════════════════════════════════════════════════════════════
    // v7.82 — SINAV
    // ═══════════════════════════════════════════════════════════════

    /**
     * Anlatımdan sınav soruları üretir.
     *
     * Sorular anlatım metninin kendisinden çıkarıldığı için "okumadığım
     * şey soruldu" durumu olmuyor.
     */
    private fun quizUret() {
        val a = anlatim ?: return
        if (!AiSettings.isReady(this)) {
            Toast.makeText(this, R.string.kn_ai_hazir_degil, Toast.LENGTH_LONG).show()
            return
        }

        val bekle = MaterialAlertDialogBuilder(this)
            .setMessage(R.string.kq_uretiliyor)
            .setCancelable(false)
            .create()
        bekle.show()

        Performans.arkaPlan {
            val sonuc = KonuUretici.quizUret(this, madde, a, adet = 6)
            Performans.anaIs {
                if (isFinishing || isDestroyed) return@anaIs
                runCatching { bekle.dismiss() }
                if (!sonuc.ok) {
                    Toast.makeText(
                        this,
                        sonuc.hata.ifBlank { getString(R.string.quiz_err_parse) },
                        Toast.LENGTH_LONG
                    ).show()
                    return@anaIs
                }
                ciz()
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.kq_hazir)
                    .setMessage(getString(R.string.kq_hazir_alt, sonuc.sorular.size))
                    .setPositiveButton(R.string.kq_sinava_gir) { _, _ -> sinavaGir() }
                    .setNegativeButton(R.string.ok, null)
                    .show()
            }
        }
    }

    private fun sinavaGir() {
        runCatching {
            QuizActivity.ac(
                this,
                KonuUretici.sanalDersId(madde),
                anlatim?.baslik?.ifBlank { madde } ?: madde
            )
        }.onFailure {
            Toast.makeText(this, R.string.quiz_err_parse, Toast.LENGTH_SHORT).show()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // v7.82 — SESLİ ANLATIM
    // ═══════════════════════════════════════════════════════════════

    /** Bu anlatımı tanımlayan sanal "asset" — servis hangi metni okuduğunu bilsin. */
    private fun sesAnahtari(): String = "konu:" + madde.take(60)

    private fun okuyorMu(): Boolean =
        SesliDersServisi.calisiyor && SesliDersServisi.aktifAsset == sesAnahtari()

    private fun sesliDugmeMetni(): String = getString(
        when {
            okuyorMu() && SesliDersServisi.duraklatildi -> R.string.ks_devam
            okuyorMu() -> R.string.ks_durdur
            else -> R.string.ks_oku
        }
    )

    /**
     * Anlatımı sesli okur.
     *
     * Mevcut [SesliDersServisi] yeniden kullanılıyor — o servis zaten
     * ön plan bildirimi, cümle cümle ilerleme ve kaldığı yeri hatırlama
     * işlerini yapıyor. Yeni bir TTS servisi yazmak bunları kopyalamak olurdu.
     */
    private fun sesliOkuyaBasla() {
        val a = anlatim ?: return

        // Zaten bu metni okuyorsa duraklat/devam et
        if (okuyorMu()) {
            SesliDersServisi.komut(
                this,
                if (SesliDersServisi.duraklatildi) SesliDersServisi.EYLEM_DEVAM
                else SesliDersServisi.EYLEM_DURAKLAT
            )
            kap.postDelayed({ if (!isFinishing) ciz() }, 350)
            return
        }

        val metin = KonuUretici.seslendirmeMetni(a)
        if (metin.length < 20) {
            Toast.makeText(this, R.string.ks_metin_yok, Toast.LENGTH_SHORT).show()
            return
        }

        try {
            SesliDersServisi.baslat(
                context = this,
                metin = metin,
                baslik = a.baslik.ifBlank { madde },
                asset = sesAnahtari(),
                hiz = 1.0f
            )
            Toast.makeText(this, R.string.ks_basladi, Toast.LENGTH_SHORT).show()
            kap.postDelayed({ if (!isFinishing) ciz() }, 600)
        } catch (e: Exception) {
            android.util.Log.w("KonuAnlatim", "Sesli okuma başlatılamadı", e)
            Toast.makeText(this, R.string.ks_hata, Toast.LENGTH_LONG).show()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ARAYÜZ YARDIMCILARI
    // ═══════════════════════════════════════════════════════════════

    private fun baslikKucuk(m: String) = TextView(this).apply {
        text = m
        textSize = 13f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        alpha = 0.75f
        setPadding(0, (4 * d).toInt(), 0, (6 * d).toInt())
    }

    private fun kartSar(ic: View): View = MaterialCardView(this).apply {
        radius = 14 * d
        cardElevation = 0f
        strokeWidth = (1 * d).toInt()
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = (8 * d).toInt() }
        addView(ic)
    }

    private fun bilgi(m: String) = TextView(this).apply {
        text = m
        textSize = 12.5f
        alpha = 0.75f
        setLineSpacing(0f, 1.25f)
        setPadding(0, (4 * d).toInt(), 0, (8 * d).toInt())
    }

    private fun ayirici() {
        kap.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (1 * d).toInt()
            ).apply {
                topMargin = (16 * d).toInt()
                bottomMargin = (8 * d).toInt()
            }
            setBackgroundColor(
                (MaterialColors.getColor(
                    this@KonuAnlatimActivity,
                    com.google.android.material.R.attr.colorOnSurface, 0
                ) and 0x00FFFFFF) or 0x22000000
            )
        })
    }

    private fun dugme(m: String, vurgulu: Boolean = false, tikla: () -> Unit) =
        TextView(this).apply {
            text = m
            textSize = 14f
            gravity = Gravity.CENTER
            setTypeface(
                typeface,
                if (vurgulu) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL
            )
            setTextColor(
                MaterialColors.getColor(
                    this@KonuAnlatimActivity,
                    com.google.android.material.R.attr.colorPrimary, 0
                )
            )
            setPadding(0, (13 * d).toInt(), 0, (13 * d).toInt())
            background = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x22888888), null, null
            )
            isClickable = true
            setOnClickListener { tikla() }
        }
}
