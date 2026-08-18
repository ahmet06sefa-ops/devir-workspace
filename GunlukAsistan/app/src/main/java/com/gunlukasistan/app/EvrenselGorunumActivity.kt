package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * v10.92 — 10.000-Madde Evrensel Görünüm ve Arayüz (UI/UX) Kişiselleştirme Atölyesi Ekranı.
 *
 * Kullanıcının talimatı (Hem Ayarlar Hem Ana Ekran, Saf Siyah OLED varsayılan tema,
 * Tam Markdown Tablo Kataloğu) doğrultusunda:
 *
 *  • 10 Ana Görünüm Boyutu ve 100 Alt Başlık altında 10.000 görünüm/arayüz maddesini yönetir.
 *  • Her madde için anında "🎨 Değiştir" butonu ve kalıcı işaretleme imkanı sunar.
 */
class EvrenselGorunumActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            context.startActivity(Intent(context, EvrenselGorunumActivity::class.java))
        }
    }

    private var seciliKategoriNo = 0
    private var seciliAltBaslikKodu = ""
    private var aramaSorgusu = ""
    private var gosterilenMaddeler: List<EvrenselGorunumAtolye.GorunumMadde> = emptyList()
    private lateinit var adapter: GorunumAdapter

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(GorunumAyar.yaziOlcegiUygula(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.styleFor(this))
        ThemeManager.applyAccent(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_evrensel_gorunum)

        findViewById<ImageButton>(R.id.btnGeri).setOnClickListener { finish() }

        adapter = GorunumAdapter()
        findViewById<ListView>(R.id.listeGorunumMaddeler).adapter = adapter

        findViewById<EditText>(R.id.editArama).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                aramaSorgusu = s?.toString() ?: ""
                listeyiTazele()
            }
        })

        findViewById<Button>(R.id.btnSifirla).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("🔄 Görünüm Seçimlerini Sıfırla")
                .setMessage("İşaretlediğiniz tüm görünüm öğesi seçimlerini temizlemek istiyor musunuz?")
                .setPositiveButton("Evet, Sıfırla") { _, _ ->
                    EvrenselGorunumAtolye.secimleriSifirla(this)
                    Toast.makeText(this, "✅ Tüm görünüm seçimleri sıfırlandı.", Toast.LENGTH_SHORT).show()
                    listeyiTazele()
                }
                .setNegativeButton("İptal", null)
                .show()
        }

        findViewById<Button>(R.id.btnGorunumUygula).setOnClickListener {
            val (n, ozetMsg) = EvrenselGorunumAtolye.seciliGorunumleriUygula(this)
            AlertDialog.Builder(this)
                .setTitle("🎨 Evrensel Görünüm Senkronizasyonu ($n Madde)")
                .setMessage(ozetMsg)
                .setPositiveButton("Tamam") { _, _ -> listeyiTazele() }
                .show()
        }

        guncelleKategoriCipleri()
        guncelleAltBaslikCipleri()
        listeyiTazele()
    }

    override fun onResume() {
        super.onResume()
        GlassmorphismTemaMotoru.sekmeleriVeKartlariStille(window.decorView, this)
    }

    private fun guncelleKategoriCipleri() {
        val kap = findViewById<LinearLayout>(R.id.layoutGorunumKategoriCipleri) ?: return
        kap.removeAllViews()

        val kategoriler = listOf(
            0 to "Tümü (10.000)",
            1 to "1. Renkler & Gece/Gündüz",
            2 to "2. Tipografi & Boyutlar",
            3 to "3. Kartlar & Köşeler",
            4 to "4. Gölgeler & Ripple",
            5 to "5. Tablolar & Grafikler",
            6 to "6. Sayaç & Medya Teması",
            7 to "7. Widget & Kilit Ekranı",
            8 to "8. Namaz & İbadet UI",
            9 to "9. Ayarlar & Menü Stili",
            10 to "10. İkonlar & E-Mürekkep"
        )

        val yogunluk = resources.displayMetrics.density

        kategoriler.forEach { (no, baslik) ->
            val cip = TextView(this).apply {
                text = baslik
                textSize = 13f
                gravity = Gravity.CENTER
                val padH = (12 * yogunluk).toInt()
                val padV = (6 * yogunluk).toInt()
                setPadding(padH, padV, padH, padV)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = (8 * yogunluk).toInt() }
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    seciliKategoriNo = no
                    seciliAltBaslikKodu = ""
                    guncelleAltBaslikCipleri()
                    guncelleCipStilleri()
                    listeyiTazele()
                }
            }
            kap.addView(cip)
        }
        guncelleCipStilleri()
    }

    private fun guncelleAltBaslikCipleri() {
        val kap = findViewById<LinearLayout>(R.id.layoutGorunumAltBaslikCipleri) ?: return
        kap.removeAllViews()

        val list: List<Pair<String, String>> = if (seciliKategoriNo == 0) {
            listOf("" to "Tüm Görünüm Alt Başlıkları (100 Alt Başlık)")
        } else {
            val sub = EvrenselGorunumAtolye.ALT_BASLIKLAR_BY_KAT[seciliKategoriNo] ?: emptyList()
            listOf("" to "Tüm Alt Başlıklar (1.000)") + sub
        }

        val yogunluk = resources.displayMetrics.density

        list.forEach { (kod, baslik) ->
            val cip = TextView(this).apply {
                text = if (kod.isBlank()) baslik else "$kod ${baslik.take(18)}…"
                textSize = 12f
                gravity = Gravity.CENTER
                val padH = (10 * yogunluk).toInt()
                val padV = (5 * yogunluk).toInt()
                setPadding(padH, padV, padH, padV)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = (6 * yogunluk).toInt() }
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    seciliAltBaslikKodu = kod
                    guncelleCipStilleri()
                    listeyiTazele()
                }
            }
            kap.addView(cip)
        }
        guncelleCipStilleri()
    }

    private fun guncelleCipStilleri() {
        val yogunluk = resources.displayMetrics.density

        val kapKat = findViewById<LinearLayout>(R.id.layoutGorunumKategoriCipleri)
        if (kapKat != null) {
            for (i in 0 until kapKat.childCount) {
                val cip = kapKat.getChildAt(i) as? TextView ?: continue
                val seciliMi = (i == seciliKategoriNo)
                cip.background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = 16 * yogunluk
                    if (seciliMi) {
                        setColor(com.google.android.material.color.MaterialColors.getColor(cip, com.google.android.material.R.attr.colorPrimary, 0xFF6200EE.toInt()))
                    } else {
                        setColor(com.google.android.material.color.MaterialColors.getColor(cip, com.google.android.material.R.attr.colorSurfaceVariant, 0xFFE0E0E0.toInt()))
                    }
                }
                cip.setTextColor(
                    if (seciliMi) ContextCompat.getColor(this, android.R.color.white)
                    else com.google.android.material.color.MaterialColors.getColor(cip, com.google.android.material.R.attr.colorOnSurface, 0xFF222222.toInt())
                )
                cip.setTypeface(null, if (seciliMi) Typeface.BOLD else Typeface.NORMAL)
            }
        }

        val kapAlt = findViewById<LinearLayout>(R.id.layoutGorunumAltBaslikCipleri)
        if (kapAlt != null) {
            for (i in 0 until kapAlt.childCount) {
                val cip = kapAlt.getChildAt(i) as? TextView ?: continue
                val seciliMi = if (seciliAltBaslikKodu.isBlank()) {
                    i == 0
                } else {
                    cip.text.toString().startsWith(seciliAltBaslikKodu)
                }
                cip.background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = 14 * yogunluk
                    if (seciliMi) {
                        setColor(com.google.android.material.color.MaterialColors.getColor(cip, com.google.android.material.R.attr.colorSecondary, 0xFF03DAC6.toInt()))
                    } else {
                        setColor(com.google.android.material.color.MaterialColors.getColor(cip, com.google.android.material.R.attr.colorSurface, 0xFFFFFFFF.toInt()))
                    }
                }
                cip.setTextColor(
                    if (seciliMi) ContextCompat.getColor(this, android.R.color.white)
                    else com.google.android.material.color.MaterialColors.getColor(cip, com.google.android.material.R.attr.colorOnSurface, 0xFF222222.toInt())
                )
                cip.setTypeface(null, if (seciliMi) Typeface.BOLD else Typeface.NORMAL)
            }
        }
    }

    private fun listeyiTazele() {
        gosterilenMaddeler = EvrenselGorunumAtolye.ara(this, aramaSorgusu, seciliKategoriNo, seciliAltBaslikKodu)
        adapter.notifyDataSetChanged()
        guncelleSayac()
    }

    private fun guncelleSayac() {
        val toplamSecili = EvrenselGorunumAtolye.seciliGorunumleriGetir(this).size
        findViewById<TextView>(R.id.txtSeciliSayaci).text =
            "Seçili: $toplamSecili / 10000 Görünüm Öğesi"
    }

    private inner class GorunumAdapter : BaseAdapter() {
        override fun getCount(): Int = gosterilenMaddeler.size
        override fun getItem(position: Int): EvrenselGorunumAtolye.GorunumMadde = gosterilenMaddeler[position]
        override fun getItemId(position: Int): Long = getItem(position).id.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val satir = convertView ?: LayoutInflater.from(this@EvrenselGorunumActivity)
                .inflate(R.layout.item_evrensel_gorunum, parent, false)

            val madde = getItem(position)

            val chk = satir.findViewById<CheckBox>(R.id.chkSecili)
            val txtBaslik = satir.findViewById<TextView>(R.id.txtGorunumBaslik)
            val txtKategori = satir.findViewById<TextView>(R.id.txtGorunumKategori)
            val txtAciklama = satir.findViewById<TextView>(R.id.txtGorunumAciklama)
            val btnAninda = satir.findViewById<Button>(R.id.btnAnindaUygula)

            chk.setOnCheckedChangeListener(null)
            chk.isChecked = madde.secili

            txtBaslik.text = "${madde.noMetni} — ${madde.baslik}"
            txtKategori.text = buildString {
                append("🎨 ")
                append(madde.altBaslikKodu)
                append(" ")
                append(madde.kategoriAdi.substringAfter(".").trim())
                append(" · ")
                append(madde.altBaslikAdi)
            }
            txtAciklama.text = buildString {
                if (madde.uygulandı) {
                    append("🎨 ANINDA UYGULANDI — ")
                }
                append(madde.aciklama)
            }

            chk.setOnCheckedChangeListener { _, isChecked ->
                madde.secili = isChecked
                EvrenselGorunumAtolye.gorunumSecimDurumunuDegistir(
                    this@EvrenselGorunumActivity,
                    madde.id,
                    isChecked
                )
                guncelleSayac()
            }

            btnAninda.setOnClickListener {
                val res = EvrenselGorunumAtolye.tekilGorunumuUygula(this@EvrenselGorunumActivity, madde.id)
                Toast.makeText(this@EvrenselGorunumActivity, res.second, Toast.LENGTH_LONG).show()
                listeyiTazele()
            }

            return satir
        }
    }
}
