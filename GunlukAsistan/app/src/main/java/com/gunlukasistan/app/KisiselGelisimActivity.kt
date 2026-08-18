package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * v11.04 — Kişisel Gelişim & Farkındalık Merkezi Ekranı (`KisiselGelisimActivity`).
 *
 * Kullanıcının talimatı doğrultusunda 5 sekmeyi otonom ve görsel grafiklerle yönetir:
 *  1. 🗓️ Retroperspektif: Son 1 yılın ay ay analizi (Neler Kattı, Neler Değişti, Farkındalık Puanı).
 *  2. 📜 Manifesto: Temel Değerler, Kimlik Tanımı, 5 Yıl Sonraki Vizyon Tablosu ve Netlik Skoru.
 *  3. 📊 SWOT Analizi: Güçlü, Zayıf, Fırsatlar ve Tehditler kadranları ve Objektif Denge Çubuğu.
 *  4. ⚡ Derin Çalışma Periyodu: 3-4 saatlik odak süresi, sevilen konular havuzu ve sayaca aktarma.
 *  5. 🧹 Reset Günü: Oda, Bilgisayar, Hedefler ve Yapılacaklar kontrol listesi ile Dağınıklık Giderme Grafiği.
 */
class KisiselGelisimActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            context.startActivity(Intent(context, KisiselGelisimActivity::class.java))
        }
    }

    private var aktifSekmeIndex: Int = 0
    private var seciliDerinKonu: String = "💻 Yazılım ve Yapay Zekâ Geliştirme"
    private var seciliDerinSureDk: Int = 180 // 3 Saat Varsayılan

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(GorunumAyar.yaziOlcegiUygula(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.styleFor(this))
        ThemeManager.applyAccent(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kisisel_gelisim)

        findViewById<ImageButton>(R.id.btnGeri).setOnClickListener { finish() }

        sekmeCipleriniKur()
        sekmeGoster(aktifSekmeIndex)
    }

    override fun onResume() {
        super.onResume()
        GlassmorphismTemaMotoru.sekmeleriVeKartlariStille(window.decorView, this)
    }

    private fun sekmeCipleriniKur() {
        val kap = findViewById<LinearLayout>(R.id.layoutKisiselGelisimCipleri) ?: return
        kap.removeAllViews()

        val sekmeAdlari = listOf(
            "🗓️ 1. Retroperspektif (Son 1 Yıl)",
            "📜 2. Manifesto & Vizyon",
            "📊 3. SWOT Analizi",
            "⚡ 4. Derin Çalışma (3-4 Saat)",
            "🧹 5. Reset Günü (Hayatı Toparla)"
        )
        val yogunluk = resources.displayMetrics.density

        sekmeAdlari.forEachIndexed { index, ad ->
            val cip = TextView(this).apply {
                text = ad
                textSize = 13f
                gravity = Gravity.CENTER
                val padH = (14 * yogunluk).toInt()
                val padV = (8 * yogunluk).toInt()
                setPadding(padH, padV, padH, padV)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = (8 * yogunluk).toInt() }
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    aktifSekmeIndex = index
                    sekmeGoster(index)
                    cipStilleriniGuncelle()
                }
            }
            kap.addView(cip)
        }
        cipStilleriniGuncelle()
    }

    private fun cipStilleriniGuncelle() {
        val kap = findViewById<LinearLayout>(R.id.layoutKisiselGelisimCipleri) ?: return
        val yogunluk = resources.displayMetrics.density

        for (i in 0 until kap.childCount) {
            val cip = kap.getChildAt(i) as? TextView ?: continue
            val seciliMi = (i == aktifSekmeIndex)
            cip.background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 16 * yogunluk
                if (seciliMi) {
                    setColor(
                        com.google.android.material.color.MaterialColors.getColor(
                            cip,
                            com.google.android.material.R.attr.colorPrimary,
                            0xFF6200EE.toInt()
                        )
                    )
                } else {
                    setColor(
                        com.google.android.material.color.MaterialColors.getColor(
                            cip,
                            com.google.android.material.R.attr.colorSurfaceVariant,
                            0xFFE0E0E0.toInt()
                        )
                    )
                }
            }
            cip.setTextColor(
                if (seciliMi) ContextCompat.getColor(this, android.R.color.white)
                else com.google.android.material.color.MaterialColors.getColor(
                    cip,
                    com.google.android.material.R.attr.colorOnSurface,
                    0xFF222222.toInt()
                )
            )
            cip.setTypeface(null, if (seciliMi) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    private fun sekmeGoster(index: Int) {
        findViewById<View>(R.id.layoutRetroPanel).visibility = if (index == 0) View.VISIBLE else View.GONE
        findViewById<View>(R.id.layoutManifestoPanel).visibility = if (index == 1) View.VISIBLE else View.GONE
        findViewById<View>(R.id.layoutSwotPanel).visibility = if (index == 2) View.VISIBLE else View.GONE
        findViewById<View>(R.id.layoutDerinPanel).visibility = if (index == 3) View.VISIBLE else View.GONE
        findViewById<View>(R.id.layoutResetPanel).visibility = if (index == 4) View.VISIBLE else View.GONE

        when (index) {
            0 -> retroPaneliniYukle()
            1 -> manifestoPaneliniYukle()
            2 -> swotPaneliniYukle()
            3 -> derinCalismaPaneliniYukle()
            4 -> resetGunuPaneliniYukle()
        }
    }

    // ─── 1. RETROPERSPEKTİF PANELİ ───
    private fun retroPaneliniYukle() {
        val aylar = KisiselGelisimMotoru.retroperspektifGetir(this)
        val ort = KisiselGelisimMotoru.yillikFarkindalikOrtalamasi(aylar)

        findViewById<TextView>(R.id.txtRetroOrtalama).text =
            "🌟 Son 1 Yılın Ortalama Farkındalık Puanı: ${String.format("%.1f", ort)} / 10"

        // Görsel ASCII Puan Grafiği
        val grafikSb = StringBuilder()
        grafikSb.append("📊 12 AYLIK FARKINDALIK VE DÖNÜŞÜM BAR GRAFİĞİ:\n")
        aylar.forEach { ay ->
            val bar = "█".repeat(ay.farkindalikPuan)
            grafikSb.append(String.format("%-8s | %-10s (%d/10)\n", ay.ayAd.take(8), bar, ay.farkindalikPuan))
        }
        findViewById<TextView>(R.id.txtRetroGrafik).text = grafikSb.toString()

        val listeKap = findViewById<LinearLayout>(R.id.listeRetroAylar)
        listeKap.removeAllViews()

        val inflater = LayoutInflater.from(this)
        val edtKattiList = mutableListOf<EditText>()
        val edtDegistiList = mutableListOf<EditText>()

        aylar.forEach { ay ->
            val satir = inflater.inflate(R.layout.item_retro_ay, listeKap, false)
            satir.findViewById<TextView>(R.id.txtRetroAyAd).text = ay.ayAd
            val txtPuan = satir.findViewById<TextView>(R.id.txtRetroPuan)
            txtPuan.text = "🌟 ${ay.farkindalikPuan} / 10"

            val edtKatti = satir.findViewById<EditText>(R.id.edtRetroKatti)
            edtKatti.setText(ay.nelerKatti)
            edtKattiList.add(edtKatti)

            val edtDegisti = satir.findViewById<EditText>(R.id.edtRetroDegisti)
            edtDegisti.setText(ay.nelerDegisti)
            edtDegistiList.add(edtDegisti)

            txtPuan.setOnClickListener {
                puanDegistirDiyalogu(ay)
            }
            listeKap.addView(satir)
        }

        findViewById<Button>(R.id.btnRetroKaydet).setOnClickListener {
            aylar.forEachIndexed { idx, ay ->
                ay.nelerKatti = edtKattiList[idx].text.toString()
                ay.nelerDegisti = edtDegistiList[idx].text.toString()
            }
            KisiselGelisimMotoru.retroperspektifKaydet(this, aylar)
            Toast.makeText(this, "💾 Son 1 Yılın Retroperspektif incelemeleri kaydedildi!", Toast.LENGTH_LONG).show()
            retroPaneliniYukle()
        }
    }

    private fun puanDegistirDiyalogu(ay: KisiselGelisimMotoru.RetroAy) {
        val secenekler = (1..10).map { "$it / 10 Puan" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("${ay.ayAd} — Farkındalık Puanını Seç")
            .setItems(secenekler) { _, which ->
                ay.farkindalikPuan = which + 1
                val aylar = KisiselGelisimMotoru.retroperspektifGetir(this)
                val idx = aylar.indexOfFirst { it.ayNo == ay.ayNo }
                if (idx >= 0) aylar[idx].farkindalikPuan = ay.farkindalikPuan
                KisiselGelisimMotoru.retroperspektifKaydet(this, aylar)
                retroPaneliniYukle()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    // ─── 2. MANİFESTO PANELİ ───
    private fun manifestoPaneliniYukle() {
        val veri = KisiselGelisimMotoru.manifestoGetir(this)

        val netlikYuzde = veri.netlikSkoruYuzdesi
        findViewById<ProgressBar>(R.id.progressManifestoNetlik).progress = netlikYuzde
        findViewById<TextView>(R.id.txtManifestoNetlik).text = veri.netlikOzeti

        val kapDegerler = findViewById<LinearLayout>(R.id.layoutManifestoDegerler)
        kapDegerler.removeAllViews()

        veri.degerler.forEachIndexed { i, deg ->
            val chipSatir = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 4, 0, 4)
            }
            val txt = TextView(this).apply {
                text = "💎 $deg"
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val btnSil = Button(this, null, android.R.attr.borderlessButtonStyle).apply {
                text = "✖"
                textSize = 14f
                setTextColor(0xFFD32F2F.toInt())
                setOnClickListener {
                    veri.degerler.removeAt(i)
                    KisiselGelisimMotoru.manifestoKaydet(this@KisiselGelisimActivity, veri)
                    manifestoPaneliniYukle()
                }
            }
            chipSatir.addView(txt)
            chipSatir.addView(btnSil)
            kapDegerler.addView(chipSatir)
        }

        val edtYeni = findViewById<EditText>(R.id.edtYeniDeger)
        findViewById<Button>(R.id.btnDegerEkle).setOnClickListener {
            val yeni = edtYeni.text.toString().trim()
            if (yeni.isNotEmpty()) {
                veri.degerler.add(yeni)
                edtYeni.setText("")
                KisiselGelisimMotoru.manifestoKaydet(this, veri)
                manifestoPaneliniYukle()
            }
        }

        val edtKimlik = findViewById<EditText>(R.id.edtKimlikTanimi)
        val edtKariyer = findViewById<EditText>(R.id.edtBesYilKariyer)
        val edtSaglik = findViewById<EditText>(R.id.edtBesYilSaglik)
        val edtFinans = findViewById<EditText>(R.id.edtBesYilFinans)
        val edtSosyal = findViewById<EditText>(R.id.edtBesYilSosyal)
        val edtBilgelik = findViewById<EditText>(R.id.edtBesYilBilgelik)

        edtKimlik.setText(veri.kimlikTanimi)
        edtKariyer.setText(veri.besYilKariyer)
        edtSaglik.setText(veri.besYilSaglik)
        edtFinans.setText(veri.besYilFinans)
        edtSosyal.setText(veri.besYilSosyal)
        edtBilgelik.setText(veri.besYilBilgelik)

        findViewById<Button>(R.id.btnManifestoKaydet).setOnClickListener {
            veri.kimlikTanimi = edtKimlik.text.toString().trim()
            veri.besYilKariyer = edtKariyer.text.toString().trim()
            veri.besYilSaglik = edtSaglik.text.toString().trim()
            veri.besYilFinans = edtFinans.text.toString().trim()
            veri.besYilSosyal = edtSosyal.text.toString().trim()
            veri.besYilBilgelik = edtBilgelik.text.toString().trim()

            KisiselGelisimMotoru.manifestoKaydet(this, veri)
            Toast.makeText(this, "💾 Manifestonuz ve 5 Yıllık Vizyon Haritanız kaydedildi!", Toast.LENGTH_LONG).show()
            manifestoPaneliniYukle()
        }
    }

    // ─── 3. SWOT ANALİZİ PANELİ ───
    private fun swotPaneliniYukle() {
        val swot = KisiselGelisimMotoru.swotGetir(this)

        val gucluOran = swot.gucluOraniYuzde
        findViewById<ProgressBar>(R.id.progressSwotDenge).progress = gucluOran
        findViewById<TextView>(R.id.txtSwotDengeMetni).text =
            "⚖️ SWOT Objektif Denge Skoru: %$gucluOran Güçlü ve Fırsat Kapasitesi (${swot.gucluVeFirsatSayisi} Güç/Fırsat vs ${swot.zayifVeTehditSayisi} Zayıf/Tehdit)"

        swotBolumYukle("GUCLU", R.id.listeGucluler, R.id.edtGucluEkle, R.id.btnGucluEkle, swot.gucluler)
        swotBolumYukle("ZAYIF", R.id.listeZayiflar, R.id.edtZayifEkle, R.id.btnZayifEkle, swot.zayiflar)
        swotBolumYukle("FIRSAT", R.id.listeFirsatlar, R.id.edtFirsatEkle, R.id.btnFirsatEkle, swot.firsatlar)
        swotBolumYukle("TEHDIT", R.id.listeTehditler, R.id.edtTehditEkle, R.id.btnTehditEkle, swot.tehditler)
    }

    private fun swotBolumYukle(
        kod: String,
        listeId: Int,
        edtId: Int,
        btnId: Int,
        maddeler: List<String>
    ) {
        val kap = findViewById<LinearLayout>(listeId) ?: return
        kap.removeAllViews()

        val inflater = LayoutInflater.from(this)
        maddeler.forEachIndexed { index, madde ->
            val satir = inflater.inflate(R.layout.item_swot_madde, kap, false)
            satir.findViewById<TextView>(R.id.txtSwotMadde).text = "• $madde"
            satir.findViewById<Button>(R.id.btnSwotMaddeSil).setOnClickListener {
                KisiselGelisimMotoru.swotMaddeSil(this, kod, index)
                swotPaneliniYukle()
            }
            satir.setOnLongClickListener {
                swotMaddeTasimaMenusu(kod, index, madde)
                true
            }
            kap.addView(satir)
        }

        val edt = findViewById<EditText>(edtId)
        val btn = findViewById<Button>(btnId)
        btn.setOnClickListener {
            val txt = edt.text.toString().trim()
            if (txt.isNotEmpty()) {
                edt.setText("")
                KisiselGelisimMotoru.swotMaddeEkle(this, kod, txt)
                swotPaneliniYukle()
            }
        }
    }

    private fun swotMaddeTasimaMenusu(kaynakKod: String, index: Int, madde: String) {
        val secenekler = arrayOf(
            "💪 Güçlü Yönler (GUCLU) Tablosuna Taşı",
            "⚠️ Zayıf Yönler (ZAYIF) Tablosuna Taşı",
            "🌟 Fırsatlar (FIRSAT) Tablosuna Taşı",
            "🛡️ Tehditler (TEHDIT) Tablosuna Taşı",
            "🗑️ Maddeyi Sil"
        )
        val kodlar = arrayOf("GUCLU", "ZAYIF", "FIRSAT", "TEHDIT")
        AlertDialog.Builder(this)
            .setTitle("⚡ SWOT Sekmeleri Arası Taşıma: '$madde'")
            .setItems(secenekler) { _, idx ->
                if (idx == 4) {
                    KisiselGelisimMotoru.swotMaddeSil(this, kaynakKod, index)
                } else {
                    val hedefKod = kodlar[idx]
                    if (hedefKod != kaynakKod) {
                        KisiselGelisimMotoru.swotMaddeSil(this, kaynakKod, index)
                        KisiselGelisimMotoru.swotMaddeEkle(this, hedefKod, madde)
                        Toast.makeText(this, "⚡ Madde '$hedefKod' sekmesine taşındı!", Toast.LENGTH_SHORT).show()
                    }
                }
                swotPaneliniYukle()
            }
            .show()
    }

    // ─── 4. DERİN ÇALIŞMA PERİYODU PANELİ ───
    private fun derinCalismaPaneliniYukle() {
        val veri = KisiselGelisimMotoru.derinCalismaGetir(this)
        seciliDerinSureDk = veri.seciliSureDk
        seciliDerinKonu = veri.konular.firstOrNull() ?: "💻 Yazılım ve Yapay Zekâ Geliştirme"

        // Haftalık Grafik
        val sb = StringBuilder()
        sb.append("📊 HAFTALIK DERİN ÇALIŞMA PERİYODU BAR GRAFİĞİ:\n")
        val gunler = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
        gunler.forEachIndexed { i, g ->
            val s = veri.haftalikSaatler.getOrNull(i) ?: 3
            val bar = "█".repeat(s * 2)
            sb.append(String.format("%-4s: %-16s %d Saat\n", g, bar, s))
        }
        findViewById<TextView>(R.id.txtDerinHaftalikGrafik).text = sb.toString()
        findViewById<TextView>(R.id.txtDerinHaftalikToplam).text =
            "🔥 Bu Haftaki Toplam Derin Odak Süresi: ${veri.haftalikToplamSaat} Saat (Günlük Ort: ${String.format("%.1f", veri.haftalikOrtalamaSaat)} Saat)"

        val listeKap = findViewById<LinearLayout>(R.id.listeDerinKonular)
        listeKap.removeAllViews()

        val inflater = LayoutInflater.from(this)
        veri.konular.forEachIndexed { index, konu ->
            val satir = inflater.inflate(R.layout.item_derin_konu, listeKap, false)
            val txt = satir.findViewById<TextView>(R.id.txtDerinKonu)
            txt.text = if (konu == seciliDerinKonu) "🎯 [SEÇİLDİ] $konu" else konu

            satir.findViewById<Button>(R.id.btnDerinKonuSec).setOnClickListener {
                seciliDerinKonu = konu
                derinCalismaPaneliniYukle()
            }
            satir.findViewById<Button>(R.id.btnDerinKonuSil).setOnClickListener {
                veri.konular.removeAt(index)
                KisiselGelisimMotoru.derinCalismaKaydet(this, veri)
                derinCalismaPaneliniYukle()
            }
            listeKap.addView(satir)
        }

        val edtYeni = findViewById<EditText>(R.id.edtYeniKonu)
        findViewById<Button>(R.id.btnKonuEkle).setOnClickListener {
            val yeni = edtYeni.text.toString().trim()
            if (yeni.isNotEmpty()) {
                edtYeni.setText("")
                veri.konular.add(yeni)
                KisiselGelisimMotoru.derinCalismaKaydet(this, veri)
                derinCalismaPaneliniYukle()
            }
        }

        val btn180 = findViewById<Button>(R.id.btn180Dk)
        val btn240 = findViewById<Button>(R.id.btn240Dk)

        btn180.setOnClickListener {
            veri.seciliSureDk = 180
            KisiselGelisimMotoru.derinCalismaKaydet(this, veri)
            derinCalismaPaneliniYukle()
        }
        btn240.setOnClickListener {
            veri.seciliSureDk = 240
            KisiselGelisimMotoru.derinCalismaKaydet(this, veri)
            derinCalismaPaneliniYukle()
        }

        if (veri.seciliSureDk == 180) {
            btn180.alpha = 1.0f
            btn240.alpha = 0.5f
        } else {
            btn180.alpha = 0.5f
            btn240.alpha = 1.0f
        }

        val btnBaslat = findViewById<Button>(R.id.btnDerinCalismayiBaslat)
        btnBaslat.text = "⚡ '$seciliDerinKonu' İçin ${veri.seciliSureDk / 60} Saatlik Derin Çalışmayı Başlat (Sayaca Gönder & Aç)"
        btnBaslat.setOnClickListener {
            val (ok, msg) = KisiselGelisimMotoru.derinCalismayiSayacaGonder(this, seciliDerinKonu, veri.seciliSureDk)
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            if (ok) {
                // Ana zamanlayıcı ekranını aç
                val i = Intent(this, MainActivity::class.java).apply {
                    putExtra("ACILIS_SEKMESI", "ZAMANLAYICI")
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(i)
            }
        }
    }

    // ─── 5. RESET GÜNÜ PANELİ ───
    private fun resetGunuPaneliniYukle() {
        val gorevler = KisiselGelisimMotoru.resetGorevleriGetir(this)

        val yuzde = KisiselGelisimMotoru.daginiklikGidermeYuzdesi(gorevler)
        findViewById<ProgressBar>(R.id.progressReset).progress = yuzde
        findViewById<TextView>(R.id.txtResetDurum).text =
            KisiselGelisimMotoru.daginiklikGidermeDurumMetni(gorevler)

        val kap = findViewById<LinearLayout>(R.id.listeResetGorevler)
        kap.removeAllViews()

        val inflater = LayoutInflater.from(this)
        gorevler.forEach { g ->
            val satir = inflater.inflate(R.layout.item_reset_gorev, kap, false)
            val chk = satir.findViewById<CheckBox>(R.id.chkResetGorev)
            val txtKat = satir.findViewById<TextView>(R.id.txtResetKategori)
            val txtBas = satir.findViewById<TextView>(R.id.txtResetBaslik)

            chk.isChecked = g.tamamlandi
            txtKat.text = g.kategori
            txtBas.text = g.baslik

            chk.setOnCheckedChangeListener { _, _ ->
                KisiselGelisimMotoru.gorevDurumuDegistir(this, g.id)
                resetGunuPaneliniYukle()
            }

            satir.findViewById<Button>(R.id.btnResetGorevSil).setOnClickListener {
                KisiselGelisimMotoru.resetGorevSil(this, g.id)
                resetGunuPaneliniYukle()
            }
            kap.addView(satir)
        }

        val edtBaslik = findViewById<EditText>(R.id.edtResetBaslik)
        findViewById<Button>(R.id.btnResetEkle).setOnClickListener {
            val bas = edtBaslik.text.toString().trim()
            if (bas.isNotEmpty()) {
                edtBaslik.setText("")
                KisiselGelisimMotoru.yeniResetGoreviEkle(this, "🏠 Oda / Bilgisayar / Hedefler", bas)
                resetGunuPaneliniYukle()
            }
        }

        findViewById<Button>(R.id.btnResetSifirla).setOnClickListener {
            val yeniList = KisiselGelisimMotoru.varsayilanResetGorevleri()
            KisiselGelisimMotoru.resetGorevleriKaydet(this, yeniList)
            Toast.makeText(this, "✨ Tüm Reset Günü görevleri sıfırlandı ve yenilendi!", Toast.LENGTH_SHORT).show()
            resetGunuPaneliniYukle()
        }
    }
}
