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
 * v10.91 — 10.000-Madde İnovasyon, Eksik & Gelişim Atölyesi Ekranı (Otomatik Senkronizasyonlu Tablo).
 *
 * Kullanıcının "bana farklı 10000 adet alt basliklara ayrilmis otomatik senkronizasyonlu
 * tablolar seklinde ve aciklamali aninda uygulanabilir bir yer olarak güncelle" talimatı
 * doğrultusunda:
 *
 *  • 20 Tematik Modül ve 100 Alt Başlık altında 10.000 benzersiz öneri maddesini listeler.
 *  • Kategori çipleri ve Alt Başlık çipleriyle çift kademeli filtreleme sağlar.
 *  • Her madde satırında kalıcı işaretleme (CheckBox) ve "⚡ Uygula" butonu sunar.
 */
class BinMaddeKontrolActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            context.startActivity(Intent(context, BinMaddeKontrolActivity::class.java))
        }
    }

    private var seciliKategoriNo = 0
    private var seciliAltBaslikKodu = ""
    private var aramaSorgusu = ""
    private var gosterilenMaddeler: List<BinMaddeAtolye.Madde> = emptyList()
    private lateinit var adapter: MaddeAdapter

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(GorunumAyar.yaziOlcegiUygula(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.styleFor(this))
        ThemeManager.applyAccent(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bin_madde_kontrol)

        findViewById<ImageButton>(R.id.btnGeri).setOnClickListener { finish() }

        adapter = MaddeAdapter()
        findViewById<ListView>(R.id.listeMaddeler).adapter = adapter

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
                .setTitle("🔄 Seçimleri Sıfırla")
                .setMessage("İşaretlediğiniz tüm maddelerin seçimini temizlemek istiyor musunuz?")
                .setPositiveButton("Evet, Sıfırla") { _, _ ->
                    BinMaddeAtolye.secimleriSifirla(this)
                    Toast.makeText(this, "✅ Tüm seçimler sıfırlandı.", Toast.LENGTH_SHORT).show()
                    listeyiTazele()
                }
                .setNegativeButton("İptal", null)
                .show()
        }

        findViewById<Button>(R.id.btnUygula).setOnClickListener {
            val (n, ozetMsg) = BinMaddeAtolye.seciliMaddeleriUygula(this)
            AlertDialog.Builder(this)
                .setTitle("⚡ Otomatik Senkronizasyon Raporu ($n Madde)")
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
        val kap = findViewById<LinearLayout>(R.id.layoutKategoriCipleri)
        kap.removeAllViews()

        val kategoriler = listOf(
            0 to "Tümü (10.000)",
            1 to "1. Odak & Akustik",
            2 to "2. Konularım & Sınav",
            3 to "3. Yaşam Sağlığı",
            4 to "4. Akıllı Gündem",
            5 to "5. Diyanet İbadet",
            6 to "6. Oyunlaştırma",
            7 to "7. Otonom AI Koç",
            8 to "8. UI/UX & Tema",
            9 to "9. Widget & Medya",
            10 to "10. Depolama & Sistem",
            11 to "11. Grafikler & Isı",
            12 to "12. Soru Radarı",
            13 to "13. Mikro-Günlük",
            14 to "14. Ses Tanıma",
            15 to "15. E-Mürekkep Modu",
            16 to "16. NFC/QR İstasyon",
            17 to "17. Wi-Fi Odaları",
            18 to "18. LaTeX & Markdown",
            19 to "19. Biyometrik",
            20 to "20. Wear OS Saat"
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
        val kap = findViewById<LinearLayout>(R.id.layoutAltBaslikCipleri)
        kap.removeAllViews()

        val list: List<Pair<String, String>> = if (seciliKategoriNo == 0) {
            listOf("" to "Tüm Alt Başlıklar (100 Alt Başlık)")
        } else {
            val sub = BinMaddeAtolye.ALT_BASLIKLAR_BY_KAT[seciliKategoriNo] ?: emptyList()
            listOf("" to "Tüm Alt Başlıklar (500)") + sub
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

        // Ana Kategori Çipleri
        val kapKat = findViewById<LinearLayout>(R.id.layoutKategoriCipleri)
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

        // Alt Başlık Çipleri
        val kapAlt = findViewById<LinearLayout>(R.id.layoutAltBaslikCipleri)
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
        gosterilenMaddeler = BinMaddeAtolye.ara(this, aramaSorgusu, seciliKategoriNo, seciliAltBaslikKodu)
        adapter.notifyDataSetChanged()
        guncelleSayac()
    }

    private fun guncelleSayac() {
        val toplamSecili = BinMaddeAtolye.seciliMaddeleriGetir(this).size
        findViewById<TextView>(R.id.txtSeciliSayaci).text =
            "Seçili: $toplamSecili / 10000 Madde"
    }

    private inner class MaddeAdapter : BaseAdapter() {
        override fun getCount(): Int = gosterilenMaddeler.size
        override fun getItem(position: Int): BinMaddeAtolye.Madde = gosterilenMaddeler[position]
        override fun getItemId(position: Int): Long = getItem(position).id.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val satir = convertView ?: LayoutInflater.from(this@BinMaddeKontrolActivity)
                .inflate(R.layout.item_bin_madde, parent, false)

            val madde = getItem(position)

            val chk = satir.findViewById<CheckBox>(R.id.chkSecili)
            val txtBaslik = satir.findViewById<TextView>(R.id.txtMaddeBaslik)
            val txtKategori = satir.findViewById<TextView>(R.id.txtMaddeKategori)
            val txtAciklama = satir.findViewById<TextView>(R.id.txtMaddeAciklama)
            val btnAninda = satir.findViewById<Button>(R.id.btnAnindaUygula)

            chk.setOnCheckedChangeListener(null)
            chk.isChecked = madde.secili

            txtBaslik.text = "${madde.noMetni} — ${madde.baslik}"
            txtKategori.text = buildString {
                append("📚 ")
                append(madde.altBaslikKodu)
                append(" ")
                append(madde.kategoriAdi.substringAfter(".").trim())
                append(" · ")
                append(madde.altBaslikAdi)
            }
            txtAciklama.text = buildString {
                if (madde.tamamlandi) {
                    append("✅ ANINDA UYGULANDI — ")
                }
                append(madde.aciklama)
            }

            chk.setOnCheckedChangeListener { _, isChecked ->
                madde.secili = isChecked
                BinMaddeAtolye.maddeSecimDurumunuDegistir(
                    this@BinMaddeKontrolActivity,
                    madde.id,
                    isChecked
                )
                guncelleSayac()
            }

            btnAninda.setOnClickListener {
                val res = BinMaddeAtolye.tekilMaddeyiUygula(this@BinMaddeKontrolActivity, madde.id)
                Toast.makeText(this@BinMaddeKontrolActivity, res.second, Toast.LENGTH_LONG).show()
                listeyiTazele()
            }

            return satir
        }
    }
}
