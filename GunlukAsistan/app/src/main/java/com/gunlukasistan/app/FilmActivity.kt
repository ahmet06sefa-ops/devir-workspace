package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.concurrent.Executors

/**
 * v7.49 — Günlük dizi/film önerisi ekranı.
 *
 * Her yapım için: poster yerine renkli kart, IMDb puanı, özet, tür, süre,
 * yönetmen, oyuncular, izlenebileceği platformlar.
 *
 * "İzle" → yasal platform listesi (Netflix, Prime, Disney+, BluTV…)
 * "Kalite" → 480p/720p/1080p/4K açıklaması ve hangi platformda bulunduğu
 */
class FilmActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            context.startActivity(Intent(context, FilmActivity::class.java))
        }
    }

    private val worker = Executors.newSingleThreadExecutor()
    private val yogunluk get() = resources.displayMetrics.density

    private lateinit var sonucKabi: LinearLayout
    private lateinit var yukleniyor: LinearLayout
    private lateinit var yukleniyorYazi: TextView
    private lateinit var altYazi: TextView
    private lateinit var arama: EditText

    @Volatile private var calisiyor = false

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
        setContentView(R.layout.activity_film)

        sonucKabi = findViewById(R.id.flResults)
        yukleniyor = findViewById(R.id.flLoading)
        yukleniyorYazi = findViewById(R.id.flLoadingText)
        altYazi = findViewById(R.id.flSubtitle)
        arama = findViewById(R.id.flSearch)

        findViewById<TextView>(R.id.flClose).setOnClickListener { finish() }
        findViewById<TextView>(R.id.flSettings).setOnClickListener { ayarlar() }
        findViewById<TextView>(R.id.flList).setOnClickListener { izlemeListesi() }
        findViewById<TextView>(R.id.flRefresh).setOnClickListener { yenile(true) }

        arama.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                aramaYap(); true
            } else false
        }

        // Bugünün önerisi önbellekte varsa anında göster
        val kayitli = FilmStore.gununOnerileri(this)
        if (kayitli.isNotEmpty()) {
            altYaziGuncelle()
            listeyiCiz(kayitli)
        } else {
            yenile(false)
        }
    }

    private fun altYaziGuncelle() {
        // v7.50: TMDb isteğe bağlı — yokluğu eksiklik gibi sunulmuyor
        altYazi.text = if (FilmStore.tmdbVarMi(this)) {
            getString(R.string.fl_sub_tmdb)
        } else {
            getString(R.string.fl_sub_ai)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // VERİ
    // ═══════════════════════════════════════════════════════════════

    private fun yenile(zorla: Boolean) {
        if (calisiyor) return
        if (!zorla && FilmStore.bugunVarMi(this)) {
            listeyiCiz(FilmStore.gununOnerileri(this))
            return
        }
        calisiyor = true
        sonucKabi.removeAllViews()
        yukleniyor.visibility = View.VISIBLE
        yukleniyorYazi.text = if (FilmStore.tmdbVarMi(this))
            getString(R.string.fl_loading) else getString(R.string.fl_loading_ai)
        altYaziGuncelle()

        worker.execute {
            val sonuc = FilmServis.gununOnerileri(this, 8)
            runOnUiThread {
                calisiyor = false
                if (isFinishing || isDestroyed) return@runOnUiThread
                yukleniyor.visibility = View.GONE
                if (!sonuc.ok || sonuc.liste.isEmpty()) {
                    hataGoster(sonuc.mesaj)
                    return@runOnUiThread
                }
                FilmStore.gununOnerileriniKaydet(this, sonuc.liste)
                listeyiCiz(sonuc.liste)
            }
        }
    }

    private fun aramaYap() {
        val sorgu = arama.text?.toString()?.trim().orEmpty()
        if (sorgu.length < 2) {
            Toast.makeText(this, R.string.fl_search_short, Toast.LENGTH_SHORT).show()
            return
        }
        if (calisiyor) return
        calisiyor = true
        klavyeKapat()
        sonucKabi.removeAllViews()
        yukleniyor.visibility = View.VISIBLE
        yukleniyorYazi.text = getString(R.string.fl_searching)

        worker.execute {
            val sonuc = FilmServis.ara(this, sorgu)
            runOnUiThread {
                calisiyor = false
                if (isFinishing || isDestroyed) return@runOnUiThread
                yukleniyor.visibility = View.GONE
                if (!sonuc.ok || sonuc.liste.isEmpty()) {
                    hataGoster(sonuc.mesaj)
                } else {
                    listeyiCiz(sonuc.liste)
                }
            }
        }
    }

    private fun hataGoster(mesaj: String) {
        sonucKabi.removeAllViews()
        sonucKabi.addView(TextView(this).apply {
            text = mesaj.ifBlank { getString(R.string.fl_err_generic) }
            textSize = 13.5f
            setPadding(4, (20 * yogunluk).toInt(), 4, (10 * yogunluk).toInt())
        })
        sonucKabi.addView(dugme(getString(R.string.fl_retry)) { yenile(true) })
        // v7.50: hata yapay zekâ kaynaklıysa doğrudan AI ayarlarına götür
        sonucKabi.addView(dugme(getString(R.string.fl_ai_ayar)) {
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(WidgetCommon.EXTRA_OPEN_SCREEN, 7)
                }
            )
        })
    }

    // ═══════════════════════════════════════════════════════════════
    // ÇİZİM
    // ═══════════════════════════════════════════════════════════════

    private fun listeyiCiz(liste: List<FilmStore.Yapim>) {
        sonucKabi.removeAllViews()
        liste.forEach { sonucKabi.addView(yapimKarti(it)) }
        // Kaynak dürüstlüğü notu
        sonucKabi.addView(TextView(this).apply {
            text = if (FilmStore.tmdbVarMi(this@FilmActivity))
                getString(R.string.fl_note_tmdb) else getString(R.string.fl_note_ai)
            textSize = 11f
            alpha = 0.6f
            setPadding(4, (14 * yogunluk).toInt(), 4, 0)
        })
    }

    private fun yapimKarti(y: FilmStore.Yapim): View {
        val kart = MaterialCardView(this).apply {
            radius = 16 * yogunluk
            cardElevation = 0f
            strokeWidth = (1 * yogunluk).toInt()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (10 * yogunluk).toInt() }
        }

        val ic = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                (14 * yogunluk).toInt(), (13 * yogunluk).toInt(),
                (14 * yogunluk).toInt(), (13 * yogunluk).toInt()
            )
        }

        // Başlık + puan
        ic.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(TextView(this@FilmActivity).apply {
                    text = (if (y.tur == "dizi") "📺 " else "🎬 ") + y.basligiTam
                    textSize = 15f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    layoutParams = LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                    )
                })
                addView(TextView(this@FilmActivity).apply {
                    text = "⭐ " + y.puanMetni
                    textSize = 14f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(0xFFF5A623.toInt())
                })
            }
        )

        // Tür · süre · yönetmen
        val ustBilgi = listOf(y.turler, y.sure).filter { it.isNotBlank() }.joinToString(" · ")
        if (ustBilgi.isNotBlank()) {
            ic.addView(satir(ustBilgi, 12f, 0.75f))
        }
        if (y.yonetmen.isNotBlank()) {
            ic.addView(satir(getString(R.string.fl_yonetmen, y.yonetmen), 12f, 0.75f))
        }
        if (y.oyuncular.isNotBlank()) {
            ic.addView(satir(getString(R.string.fl_oyuncular, y.oyuncular), 12f, 0.75f))
        }

        // Özet
        if (y.ozet.isNotBlank()) {
            ic.addView(TextView(this).apply {
                text = y.ozet
                textSize = 13f
                setLineSpacing(3f * yogunluk, 1f)
                setPadding(0, (8 * yogunluk).toInt(), 0, 0)
            })
        }

        // Platformlar
        if (y.platformlar.isNotBlank()) {
            ic.addView(satir("📡 " + y.platformlar, 12f, 0.9f).apply {
                setPadding(0, (8 * yogunluk).toInt(), 0, 0)
            })
        }

        // Düğme şeridi
        val dugmeler = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, (10 * yogunluk).toInt(), 0, 0)
        }
        dugmeler.addView(kucukDugme("▶ " + getString(R.string.fl_izle)) { izleSec(y) })
        dugmeler.addView(kucukDugme("⬇ " + FilmStore.kalite(this)) { kaliteSec(y) })
        dugmeler.addView(kucukDugme("ⓘ IMDb") { ac(FilmServis.imdbBaglantisi(y)) })

        val listede = FilmStore.listedeMi(this, y)
        dugmeler.addView(kucukDugme(if (listede) "🔖" else "＋") {
            val yeni = FilmStore.listeyeAlDegistir(this, y)
            Toast.makeText(
                this,
                if (yeni) R.string.fl_eklendi else R.string.fl_cikarildi,
                Toast.LENGTH_SHORT
            ).show()
            // Kartı yeniden çiz
            (sonucKabi.getChildAt(sonucKabi.indexOfChild(kart)) as? View)?.let {
                sonucKabi.removeView(kart)
            }
            sonucKabi.addView(yapimKarti(y), sonucKabi.childCount - 1)
        })
        ic.addView(dugmeler)

        kart.addView(ic)

        // Karta dokun → ayrıntı yükle (TMDb varsa)
        kart.isClickable = true
        kart.dalgaEkle()
        kart.setOnClickListener { detayGoster(y) }
        return kart
    }

    // ═══════════════════════════════════════════════════════════════
    // İZLE / KALİTE
    // ═══════════════════════════════════════════════════════════════

    private fun izleSec(y: FilmStore.Yapim) {
        val baglantilar = FilmServis.izlemeBaglantilari(y)
        val adlar = baglantilar.map { it.first }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.fl_izle_baslik, y.ad))
            .setItems(adlar) { _, hangi -> ac(baglantilar[hangi].second) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Kalite seçimi ve çevrimdışı izleme yönlendirmesi.
     *
     * Dürüstlük notu: uygulama telif korumalı içerik indirmez. Yasal
     * platformların kendi indirme özelliğine yönlendirir — hepsinde var.
     */
    private fun kaliteSec(y: FilmStore.Yapim) {
        val kaliteler = FilmServis.KALITELER.toTypedArray()
        val secili = FilmServis.KALITELER.indexOf(FilmStore.kalite(this)).coerceAtLeast(0)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.fl_kalite_baslik)
            .setSingleChoiceItems(kaliteler, secili) { d, hangi ->
                FilmStore.setKalite(this, FilmServis.KALITELER[hangi])
                d.dismiss()
                indirmeBilgisi(y)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun indirmeBilgisi(y: FilmStore.Yapim) {
        val kalite = FilmStore.kalite(this)
        val govde = StringBuilder()
        govde.append(FilmServis.kaliteAciklamasi(this, kalite)).append("\n\n")
        if (y.platformlar.isNotBlank()) {
            govde.append(getString(R.string.fl_indir_platform, y.platformlar)).append("\n\n")
        }
        govde.append(getString(R.string.fl_indir_nasil))

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.fl_indir_baslik, kalite))
            .setMessage(govde.toString())
            .setPositiveButton(R.string.fl_izle) { _, _ -> izleSec(y) }
            .setNegativeButton(R.string.done, null)
            .show()
    }

    // ═══════════════════════════════════════════════════════════════
    // DETAY
    // ═══════════════════════════════════════════════════════════════

    private fun detayGoster(y: FilmStore.Yapim) {
        if (!FilmStore.tmdbVarMi(this) || y.tmdbId == 0) {
            metinDetay(y)
            return
        }
        yukleniyor.visibility = View.VISIBLE
        yukleniyorYazi.text = getString(R.string.fl_detay_yukleniyor)
        worker.execute {
            val tam = FilmServis.detayGetir(this, y)
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                yukleniyor.visibility = View.GONE
                metinDetay(tam)
            }
        }
    }

    private fun metinDetay(y: FilmStore.Yapim) {
        val sb = StringBuilder()
        if (y.orijinalAd.isNotBlank() && y.orijinalAd != y.ad) {
            sb.append(y.orijinalAd).append("\n\n")
        }
        sb.append("⭐ ").append(y.puanMetni)
        if (y.oySayisi > 0) sb.append(" (").append(y.oySayisi).append(" oy)")
        sb.append("\n")
        if (y.turler.isNotBlank()) sb.append("🎭 ").append(y.turler).append("\n")
        if (y.sure.isNotBlank()) sb.append("⏱ ").append(y.sure).append("\n")
        if (y.yonetmen.isNotBlank()) sb.append("🎥 ").append(y.yonetmen).append("\n")
        if (y.oyuncular.isNotBlank()) sb.append("👥 ").append(y.oyuncular).append("\n")
        if (y.platformlar.isNotBlank()) sb.append("📡 ").append(y.platformlar).append("\n")
        if (y.ozet.isNotBlank()) sb.append("\n").append(y.ozet)

        MaterialAlertDialogBuilder(this)
            .setTitle(y.basligiTam)
            .setMessage(sb.toString())
            .setPositiveButton(R.string.fl_izle) { _, _ -> izleSec(y) }
            .setNeutralButton("IMDb") { _, _ -> ac(FilmServis.imdbBaglantisi(y)) }
            .setNegativeButton(R.string.done, null)
            .show()
    }

    // ═══════════════════════════════════════════════════════════════
    // LİSTE VE AYARLAR
    // ═══════════════════════════════════════════════════════════════

    private fun izlemeListesi() {
        val liste = FilmStore.listeyiYukle(this)
        if (liste.isEmpty()) {
            Toast.makeText(this, R.string.fl_liste_bos, Toast.LENGTH_SHORT).show()
            return
        }
        val adlar = liste.map {
            (if (it.izlendi) "✅ " else "🔖 ") + it.basligiTam + "  ⭐" + it.puanMetni
        }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.fl_liste_baslik, liste.size))
            .setItems(adlar) { _, hangi -> listeMenusu(liste[hangi]) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun listeMenusu(y: FilmStore.Yapim) {
        val secenekler = arrayOf(
            getString(R.string.fl_detay),
            if (y.izlendi) getString(R.string.fl_izlenmedi_yap)
            else getString(R.string.fl_izlendi_yap),
            getString(R.string.fl_izle),
            getString(R.string.delete)
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(y.basligiTam)
            .setItems(secenekler) { _, hangi ->
                when (hangi) {
                    0 -> metinDetay(y)
                    1 -> {
                        FilmStore.izlendiDegistir(this, y)
                        izlemeListesi()
                    }
                    2 -> izleSec(y)
                    3 -> {
                        FilmStore.listeyeAlDegistir(this, y)
                        Toast.makeText(this, R.string.fl_cikarildi, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun ayarlar() {
        val secenekler = arrayOf(
            getString(R.string.fl_set_tur),
            getString(R.string.fl_set_sevilen),
            getString(R.string.fl_set_kalite),
            getString(R.string.fl_set_tmdb_opt)
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.fl_ayarlar)
            .setItems(secenekler) { _, hangi ->
                when (hangi) {
                    0 -> turSec()
                    1 -> sevilenTurler()
                    2 -> kaliteVarsayilan()
                    3 -> tmdbGir()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun turSec() {
        val degerler = listOf("hepsi", "film", "dizi")
        val adlar = arrayOf(
            getString(R.string.fl_tur_hepsi),
            getString(R.string.fl_tur_film),
            getString(R.string.fl_tur_dizi)
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.fl_set_tur)
            .setSingleChoiceItems(
                adlar, degerler.indexOf(FilmStore.turTercihi(this)).coerceAtLeast(0)
            ) { d, hangi ->
                FilmStore.setTurTercihi(this, degerler[hangi])
                d.dismiss()
                yenile(true)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun sevilenTurler() {
        val girdi = EditText(this).apply {
            hint = getString(R.string.fl_sevilen_hint)
            setText(FilmStore.sevilenTurler(this@FilmActivity))
            inputType = InputType.TYPE_CLASS_TEXT
            setPadding(
                (20 * yogunluk).toInt(), (14 * yogunluk).toInt(),
                (20 * yogunluk).toInt(), (10 * yogunluk).toInt()
            )
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.fl_set_sevilen)
            .setMessage(R.string.fl_sevilen_msg)
            .setView(girdi)
            .setPositiveButton(R.string.save) { _, _ ->
                FilmStore.setSevilenTurler(this, girdi.text?.toString().orEmpty())
                yenile(true)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun kaliteVarsayilan() {
        val kaliteler = FilmServis.KALITELER.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.fl_set_kalite)
            .setSingleChoiceItems(
                kaliteler,
                FilmServis.KALITELER.indexOf(FilmStore.kalite(this)).coerceAtLeast(0)
            ) { d, hangi ->
                FilmStore.setKalite(this, FilmServis.KALITELER[hangi])
                d.dismiss()
                listeyiCiz(FilmStore.gununOnerileri(this))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun tmdbGir() {
        val girdi = EditText(this).apply {
            hint = getString(R.string.fl_tmdb_hint)
            setText(FilmStore.tmdbAnahtar(this@FilmActivity))
            inputType = InputType.TYPE_CLASS_TEXT
            setPadding(
                (20 * yogunluk).toInt(), (14 * yogunluk).toInt(),
                (20 * yogunluk).toInt(), (10 * yogunluk).toInt()
            )
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.fl_set_tmdb)
            .setMessage(R.string.fl_tmdb_msg)
            .setView(girdi)
            .setPositiveButton(R.string.save) { _, _ ->
                FilmStore.setTmdbAnahtar(this, girdi.text?.toString().orEmpty())
                yenile(true)
            }
            .setNeutralButton(R.string.fl_tmdb_ac) { _, _ ->
                ac("https://www.themoviedb.org/settings/api")
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ═══════════════════════════════════════════════════════════════
    // YARDIMCI
    // ═══════════════════════════════════════════════════════════════

    private fun ac(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            android.util.Log.w("FilmActivity", "Bağlantı açılamadı", e)
            Toast.makeText(this, R.string.fl_err_link, Toast.LENGTH_SHORT).show()
        }
    }

    private fun satir(metin: String, boyut: Float, saydam: Float) = TextView(this).apply {
        text = metin
        textSize = boyut
        alpha = saydam
        setPadding(0, (2 * yogunluk).toInt(), 0, 0)
    }

    private fun kucukDugme(metin: String, tiklayinca: () -> Unit) = TextView(this).apply {
        text = metin
        textSize = 12f
        gravity = Gravity.CENTER
        setPadding(
            (10 * yogunluk).toInt(), (7 * yogunluk).toInt(),
            (10 * yogunluk).toInt(), (7 * yogunluk).toInt()
        )
        setTextColor(
            MaterialColors.getColor(
                this@FilmActivity, com.google.android.material.R.attr.colorPrimary, 0
            )
        )
        background = android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf(0x22888888), null, null
        )
        isClickable = true
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { marginEnd = (4 * yogunluk).toInt() }
        setOnClickListener { tiklayinca() }
    }

    private fun dugme(metin: String, tiklayinca: () -> Unit) = TextView(this).apply {
        text = metin
        textSize = 13.5f
        setTextColor(
            MaterialColors.getColor(
                this@FilmActivity, com.google.android.material.R.attr.colorPrimary, 0
            )
        )
        setPadding(0, (10 * yogunluk).toInt(), 0, (10 * yogunluk).toInt())
        isClickable = true
        setOnClickListener { tiklayinca() }
    }

    private fun klavyeKapat() {
        try {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE)
                as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(arama.windowToken, 0)
        } catch (e: Exception) {
            android.util.Log.w("FilmActivity", "Klavye kapatılamadı", e)
        }
    }

    override fun onDestroy() {
        worker.shutdownNow()
        super.onDestroy()
    }
}
