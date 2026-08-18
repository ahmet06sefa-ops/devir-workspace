package com.gunlukasistan.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Ayarlar ekranı: Görünüm, Bildirimler, Sesler, Yedekleme ve Hakkında.
 *
 * v7.17 — yedekleme yenilendi:
 *   · Dosyaya kaydetme (Storage Access Framework)
 *   · Dosyadan geri yükleme + içerik önizlemesi
 *   · Geri yüklemeden önce otomatik güvenlik kopyası
 */
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    /** v7.17: yedeği kullanıcının seçtiği konuma yazar. */
    private val yedekKaydet = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        val ctx = context ?: return@registerForActivityResult
        try {
            ctx.contentResolver.openOutputStream(uri)?.use { cikis ->
                cikis.write(Store.exportJson(ctx).toByteArray(Charsets.UTF_8))
            }
            Toast.makeText(ctx, R.string.backup_file_ok, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            android.util.Log.w("Settings", "Yedek yazılamadı", e)
            Toast.makeText(ctx, R.string.backup_file_fail, Toast.LENGTH_LONG).show()
        }
    }

    /** v7.17: seçilen dosyayı okuyup önizleme ile geri yükler. */
    private val yedekAc = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        val ctx = context ?: return@registerForActivityResult
        try {
            val metin = ctx.contentResolver.openInputStream(uri)?.use { giris ->
                giris.readBytes().toString(Charsets.UTF_8)
            }.orEmpty()
            if (metin.isBlank()) {
                Toast.makeText(ctx, R.string.restore_fail, Toast.LENGTH_LONG).show()
                return@registerForActivityResult
            }
            onizlemeIleGeriYukle(metin)
        } catch (e: Exception) {
            android.util.Log.w("Settings", "Yedek okunamadı", e)
            Toast.makeText(ctx, R.string.restore_fail, Toast.LENGTH_LONG).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        aramaKutusunuKur(view)
        // v8.6 · Öneri 29: yatay/tablet genişlik sınırı + yoğunluk
        Duzen.uygula(view)
        view.findViewById<TextView>(R.id.versionText).text =
            getString(R.string.settings_version_format, getString(R.string.app_version))

        view.findViewById<View>(R.id.rowTheme).setOnClickListener {
            (activity as? MainActivity)?.openThemes()
        }
        // v11.07: İlk Açılış Ekranı Seçimi (Bugün, Ana Ekran, Görevler vb.)
        val txtAcilisAlt = view.findViewById<TextView>(R.id.txtAcilisEkranAlt)
        fun tazeleAcilisAlt() {
            txtAcilisAlt?.text = "Seçili: ${GorunumAyar.acilisEkranAd(requireContext())} (Uygulama açılınca görünür)"
        }
        tazeleAcilisAlt()
        view.findViewById<View>(R.id.rowAcilisEkran)?.setOnClickListener {
            val secenekler = arrayOf(
                "🏠 Ana Ekran (Varsayılan)",
                "✅ Görevler",
                "⏱️ Sayaç",
                "🤖 Asistan",
                "☀️ Bugün / Günün Akışı",
                "📋 Vakit Planı",
                "📊 İlerleme"
            )
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("🚀 İlk Açılış Ekranını Seç")
                .setItems(secenekler) { _, idx ->
                    GorunumAyar.setAcilisEkran(requireContext(), idx)
                    tazeleAcilisAlt()
                    Toast.makeText(requireContext(), "🚀 Uygulama artık '${secenekler[idx]}' ekranıyla açılacak!", Toast.LENGTH_SHORT).show()
                }
                .show()
        }
        // v7.68: widget tema ayarlari
        view.findViewById<View>(R.id.rowWidgetTheme).setOnClickListener {
            WidgetTemaActivity.ac(requireContext())
        }
        // v9.6: ogrenme merkezi (oneri 31, 33, 35, 36)
        view.findViewById<View>(R.id.rowOgrenme).setOnClickListener {
            if (!KartAcilis.ac(it, OgrenmeActivity::class.java)) {
                OgrenmeActivity.ac(requireContext())
            }
        }
        // v9.7: gunluk hayat (oneri 41-46)
        view.findViewById<View>(R.id.rowTakip).setOnClickListener {
            // v9.9 · Görsel öneri 9: ekran dokunulan satırdan büyüyerek açılıyor
            if (!KartAcilis.ac(it, TakipActivity::class.java)) {
                TakipActivity.ac(requireContext())
            }
        }
        // v9.8: sistem ve kullanim (oneri 47-50)
        view.findViewById<View>(R.id.rowSistem).setOnClickListener {
            if (!KartAcilis.ac(it, SistemActivity::class.java)) {
                SistemActivity.ac(requireContext())
            }
        }
        // v9.4: takvim ve sure analizi (oneri 9-15)
        view.findViewById<View>(R.id.rowTakvim).setOnClickListener {
            TakvimAyarActivity.ac(requireContext())
        }
        // v9.0: konu tekrari (oneri 53)
        //
        // Satira dokunmak tekrar oturumunu aciyor, anahtar sistemi
        // aciyor/kapatiyor. Ikisi ayri: kullanici acmadan once ne
        // olacagini gormek isteyebilir.
        view.findViewById<View>(R.id.rowKonuTekrar).setOnClickListener {
            if (KonuTekrar.acikMi(requireContext())) {
                TekrarActivity.ac(requireContext())
            } else {
                view.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(
                    R.id.svicKonuTekrar
                )?.toggle()
            }
        }
        view.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(
            R.id.svicKonuTekrar
        )?.apply {
            isChecked = KonuTekrar.acikMi(requireContext())
            setOnCheckedChangeListener { dugme, acik ->
                if (!dugme.isPressed) return@setOnCheckedChangeListener
                KonuTekrar.ac(requireContext(), acik)
                Titresim.dokunus(dugme)
                if (acik) {
                    // Açılınca mevcut BİTMİŞ maddeleri programa al —
                    // yoksa kullanıcı özelliği açıp boş ekran görürdü
                    // ve "çalışmıyor" sanırdı.
                    var eklenen = 0
                    runCatching {
                        Store.loadTopics(requireContext()).forEach { konu ->
                            konu.items.filter { it.done }.forEach { madde ->
                                KonuTekrar.programaAl(
                                    requireContext(), madde.id, konu.id, madde.text
                                )
                                eklenen++
                            }
                        }
                    }
                    Bildir.basari(
                        view,
                        getString(R.string.kt_acildi, eklenen)
                    )
                }
                tekrarSatiriniTazele(view)
            }
        }
        tekrarSatiriniTazele(view)

        // v8.8: depolama yonetimi (oneri 10)
        view.findViewById<View>(R.id.rowDepolama).setOnClickListener {
            DepolamaActivity.ac(requireContext())
        }
        // v10.22: gizlilik kilidi (PIN) — VERİ bölümündeki satır
        view.findViewById<View>(R.id.rowKilit).setOnClickListener {
            kilitMenusunuGoster()
        }
        kilitOzetiTazele()
        // v8.5: ana ekran duzeni (oneri 16)
        view.findViewById<View>(R.id.rowAnaDuzen).setOnClickListener {
            AnaEkranDuzenActivity.ac(requireContext())
        }
        // v8.2: gorunum ve hareket ayarlari (Grup A)
        view.findViewById<View>(R.id.rowGorunum).setOnClickListener {
            GorunumAyarActivity.ac(requireContext())
        }
        // v7.78: zorlayici ogretmen kocu
        view.findViewById<View>(R.id.rowKoc).setOnClickListener {
            KocActivity.ac(requireContext())
        }
        // v7.78: resimli kanit ayarlari
        view.findViewById<View>(R.id.rowKanit).setOnClickListener {
            KanitActivity.ac(requireContext())
        }
        // v7.83: hata defteri
        view.findViewById<View>(R.id.rowHatalarim).setOnClickListener {
            HatalarimActivity.ac(requireContext())
        }
        // v7.84: terim sözlüğü
        view.findViewById<View>(R.id.rowSozluk).setOnClickListener {
            SozlukActivity.ac(requireContext())
        }
        view.findViewById<View>(R.id.rowSounds).setOnClickListener {
            (activity as? MainActivity)?.openTimer()
        }
        view.findViewById<View>(R.id.rowNotifications).setOnClickListener {
            // v7.44: doğrudan tam bildirim ekranı — ara pencere kaldırıldı.
            // Tür bazlı anahtarlar eskiden 3. seviyede gizliydi, bulunamıyordu.
            BildirimAyarActivity.ac(requireContext())
        }
        // v10.9: gün çerçevesi (uyku düzeni, sabah kapısı, akşam özeti)
        view.findViewById<View>(R.id.rowUyku).setOnClickListener {
            UykuAyarActivity.ac(requireContext())
        }
        // v10.47: Manuel Kontrol Merkezi
        view.findViewById<View>(R.id.rowManuelKontrol).setOnClickListener {
            ManuelKontrolActivity.ac(requireContext())
        }
        // v10.48: Otonom AI Ajanı ve Otopilot Merkezi
        view.findViewById<View>(R.id.rowOtonomMerkez)?.setOnClickListener {
            OtonomMerkezActivity.ac(requireContext())
        }
        // v10.53: 32 Maddelik Tasarım ve Yerleşim Atölyesi
        view.findViewById<View>(R.id.rowTasarimAtolye)?.setOnClickListener {
            TasarimAtolyeActivity.ac(requireContext())
        }
        // v10.54: Sesli Brifing ve Haftalık Verimlilik Karnesi
        view.findViewById<View>(R.id.rowKarne)?.setOnClickListener {
            KarneActivity.ac(requireContext())
        }
        // v10.55: 10 Özel Yaşam Modülü ve Manuel Kontrol Merkezi (#1..#10)
        view.findViewById<View>(R.id.rowYasamModulleri)?.setOnClickListener {
            YasamModulleriActivity.ac(requireContext())
        }
        // v10.56: C, D, E, G, H, I ve J Gelişmiş Hayat Atölyesi (#21..#50 ve #61..#100)
        view.findViewById<View>(R.id.rowGelismiAtolye)?.setOnClickListener {
            GelismiAtolyeActivity.ac(requireContext())
        }
        // v10.57: Faz 2: C-D-E-G-H-I-J Uzman Modülleri & Özel Ekranlar
        view.findViewById<View>(R.id.rowUzmanModuller)?.setOnClickListener {
            UzmanModullerActivity.ac(requireContext())
        }
        // v10.58: Ders Çalışma & Kolaylık Atölyesi (10 Uzman Modül + 100 Öneri)
        view.findViewById<View>(R.id.rowDersKolaylik)?.setOnClickListener {
            DersKolaylikActivity.ac(requireContext())
        }
        // v10.60: Ders Çalışma İleri Fazı (#1, #41, #11 - Leitner & PDF Flaş Kart)
        view.findViewById<View>(R.id.rowDersIleriFaz)?.setOnClickListener {
            DersIleriFazActivity.ac(requireContext())
        }
        // v10.64: Ders Çalışma Uzman Merkezi — Faz 2..5 (#7..#89)
        view.findViewById<View>(R.id.rowDersUzmanMerkez)?.setOnClickListener {
            DersUzmanMerkezActivity.ac(requireContext())
        }
        // v10.65: Yaşam Sağlığı & Finans — Uzman Faz 2 (#4..#54)
        view.findViewById<View>(R.id.rowYasamSaglikFinans)?.setOnClickListener {
            YasamSaglikFinansActivity.ac(requireContext())
        }
        // v10.66: Ders Çalışma Uzman Faz 6 (#64..#99)
        view.findViewById<View>(R.id.rowDersUzmanFaz6)?.setOnClickListener {
            DersUzmanFaz6Activity.ac(requireContext())
        }
        // v10.67: Yaşam Sağlığı & Finans — Uzman Faz 3 (#51..#100)
        view.findViewById<View>(R.id.rowYasamSaglikFinansFaz3)?.setOnClickListener {
            YasamSaglikFinansFaz3Activity.ac(requireContext())
        }
        // v10.68: Evrensel Otonom Yönetim & 200-Madde Kontrol Merkezi
        view.findViewById<View>(R.id.rowEvrenselOtonomMerkez)?.setOnClickListener {
            EvrenselOtonomMerkezActivity.ac(requireContext())
        }
        // v10.69: Akıllı Gündem & Otonom Asistan Merkezi
        view.findViewById<View>(R.id.rowAkilliGundemMerkezi)?.setOnClickListener {
            AkilliGundemVeAsistanMerkeziActivity.ac(requireContext())
        }
        // v10.70: Aylık Namaz Saatleri & Titreşim Yönetimi
        view.findViewById<View>(R.id.rowNamazAylikYonetim)?.setOnClickListener {
            NamazAylikYonetimActivity.ac(requireContext())
        }
        // v10.77: KPSS / YKS Merkezi Yönetim & Ayarlar Atölyesi
        view.findViewById<View>(R.id.rowKpssMerkeziYonetim)?.setOnClickListener {
            KpssMerkeziYonetimActivity.ac(requireContext())
        }
        // v11.11: Canva Çalışma Ekranı (10 Uygulama Arayüzü)
        view.findViewById<View>(R.id.rowCanvaAtolye)?.setOnClickListener {
            CanvaCalismaAtolyeActivity.ac(requireContext())
        }
        // v11.12: Tüm Verileri Yedekle & Geri Yükle
        view.findViewById<View>(R.id.rowVeriYedekle)?.setOnClickListener {
            VeriYedekActivity.ac(requireContext())
        }
        // v11.13: Telefondaki diğer uygulamalara erişim
        view.findViewById<View>(R.id.rowUygulamalar)?.setOnClickListener {
            UygulamalarActivity.ac(requireContext())
        }
        // v11.04: Kişisel Gelişim ve Farkındalık Merkezi
        view.findViewById<View>(R.id.rowKisiselGelisim)?.setOnClickListener {
            KisiselGelisimActivity.ac(requireContext())
        }
        // v10.93: YouTube Çevrimdışı Oynatma Listesi Sıralayıcı ve Video Kitaplığı
        view.findViewById<View>(R.id.rowYoutubePlaylist)?.setOnClickListener {
            YoutubePlaylistActivity.ac(requireContext())
        }
        // v10.89: 1000-Madde Eksik & Gelişim Kontrol Atölyesi (#1..#1000)
        view.findViewById<View>(R.id.rowBinMaddeAtolye)?.setOnClickListener {
            BinMaddeKontrolActivity.ac(requireContext())
        }
        // v10.92: 10.000-Madde Evrensel Görünüm ve Arayüz (UI/UX) Kişiselleştirme Atölyesi (#1..#10000)
        view.findViewById<View>(R.id.rowEvrenselGorunumAtolye)?.setOnClickListener {
            EvrenselGorunumActivity.ac(requireContext())
        }
        // v10.82: Ayarların sadeleştirilmesi, tematik alt başlıklar ve 18 yeni atölye
        view.findViewById<View>(R.id.rowSayacAyar)?.setOnClickListener {
            startActivity(android.content.Intent(requireContext(), SayacAyarActivity::class.java))
        }
        // v11.01: Gün seriniz yazısı açılışta gösterilsin ve sonra kaybolsun ayarı
        val rowGunSerisi = view.findViewById<View>(R.id.rowGunSerisiOtoGizle)
        val txtGunSerisiTitle = view.findViewById<TextView>(R.id.rowGunSerisiOtoGizleTitle)
        val txtGunSerisiSub = view.findViewById<TextView>(R.id.rowGunSerisiOtoGizleSub)
        fun guncelleGunSerisiRow() {
            val durum = GorunumAyar.gunSerisiOtoGizleDurumMetni(requireContext())
            txtGunSerisiTitle?.text = durum.first
            txtGunSerisiSub?.text = durum.second
        }
        guncelleGunSerisiRow()
        rowGunSerisi?.setOnClickListener {
            val yeniDurum = !GorunumAyar.isGunSerisiOtoGizle(requireContext())
            GorunumAyar.setGunSerisiOtoGizle(requireContext(), yeniDurum)
            guncelleGunSerisiRow()
            (activity as? MainActivity)?.yuzenSeritiTazele()
            Toast.makeText(
                requireContext(),
                if (yeniDurum) "🔥 Gün seriniz yazısı açılışta gösterilip 4 saniye sonra gizlenecek."
                else "ℹ️ Gün seriniz yazısı altta sürekli görünecek.",
                Toast.LENGTH_SHORT
            ).show()
        }
        view.findViewById<View>(R.id.rowWidgetFiltre)?.setOnClickListener {
            startActivity(android.content.Intent(requireContext(), WidgetFiltreActivity::class.java))
        }
        view.findViewById<View>(R.id.rowSohbetGecmisi)?.setOnClickListener {
            startActivity(android.content.Intent(requireContext(), SohbetGecmisiActivity::class.java))
        }
        view.findViewById<View>(R.id.rowOgretmen)?.setOnClickListener {
            startActivity(android.content.Intent(requireContext(), OgretmenActivity::class.java))
        }
        view.findViewById<View>(R.id.rowAnalitik)?.setOnClickListener {
            startActivity(android.content.Intent(requireContext(), AnalitikActivity::class.java))
        }
        view.findViewById<View>(R.id.rowHaftaPlan)?.setOnClickListener {
            startActivity(android.content.Intent(requireContext(), HaftaPlanActivity::class.java))
        }
        view.findViewById<View>(R.id.rowFlasKart)?.setOnClickListener {
            startActivity(android.content.Intent(requireContext(), KartActivity::class.java))
        }
        view.findViewById<View>(R.id.rowSoruCoz)?.setOnClickListener {
            startActivity(android.content.Intent(requireContext(), SoruCozActivity::class.java))
        }
        view.findViewById<View>(R.id.rowPdfArama)?.setOnClickListener {
            startActivity(android.content.Intent(requireContext(), PdfAramaActivity::class.java))
        }
        // v11.06: Namaz Vakitlerinde Sesli Alarm Çal Aç/Kapat
        val swNamazSes = view.findViewById<android.widget.Switch>(R.id.swNamazSesliAlarmToggle)
        if (swNamazSes != null) {
            swNamazSes.isChecked = NamazBildirim.sesliAlarmAcik(requireContext())
            view.findViewById<View>(R.id.rowNamazSesliAlarmToggle)?.setOnClickListener {
                swNamazSes.toggle()
            }
            swNamazSes.setOnCheckedChangeListener { _, acik ->
                NamazBildirim.setSesliAlarmAcik(requireContext(), acik)
            }
        }
        view.findViewById<View>(R.id.rowNamazAyar)?.setOnClickListener {
            startActivity(android.content.Intent(requireContext(), NamazAyarActivity::class.java))
        }
        view.findViewById<View>(R.id.rowNefes)?.setOnClickListener {
            startActivity(android.content.Intent(requireContext(), NefesActivity::class.java))
        }
        view.findViewById<View>(R.id.rowMikroGunluk)?.setOnClickListener {
            startActivity(android.content.Intent(requireContext(), MikroGunlukActivity::class.java))
        }
        view.findViewById<View>(R.id.rowFilm)?.setOnClickListener {
            startActivity(android.content.Intent(requireContext(), FilmActivity::class.java))
        }
        view.findViewById<View>(R.id.rowBildirimTani)?.setOnClickListener {
            startActivity(android.content.Intent(requireContext(), BildirimTaniActivity::class.java))
        }
        view.findViewById<View>(R.id.rowOnlineBekci)?.setOnClickListener {
            startActivity(android.content.Intent(requireContext(), OnlineBekciActivity::class.java))
        }
        view.findViewById<View>(R.id.rowArsiv)?.setOnClickListener {
            startActivity(android.content.Intent(requireContext(), ArsivActivity::class.java))
        }
        view.findViewById<View>(R.id.rowSeneFilmi)?.setOnClickListener {
            startActivity(android.content.Intent(requireContext(), SeneFilmiActivity::class.java))
        }
        view.findViewById<View>(R.id.rowWidgetTema)?.setOnClickListener {
            startActivity(android.content.Intent(requireContext(), WidgetTemaActivity::class.java))
        }
        bindAnaEkranButonToggle(view)
        bindNamazAylikToggle(view)
        bindMotivasyonMansetToggle(view)
        bindGlassmorphismToggle(view)
        bindKpssModuToggle(view)
        bindTabloBaslikToggle(view)
        GlassmorphismTemaMotoru.sekmeleriVeKartlariStille(view, requireContext())
        view.findViewById<View>(R.id.rowBackup).setOnClickListener { shareBackup() }
        view.findViewById<View>(R.id.rowRestore).setOnClickListener { showRestoreDialog() }
        view.findViewById<View>(R.id.rowAbout).setOnClickListener { showAboutDialog() }
        view.findViewById<View>(R.id.rowSync).setOnClickListener { toggleAutoBackup() }
        // v10.42 · Madde #5/#6: sabah/akşam plan bildirimleri tercihleri
        planSatirlariniYaz(view)
        view.findViewById<View>(R.id.rowPlanSabah).setOnClickListener {
            val c = requireContext()
            PlanAsistan.sabahAcik(c, !PlanAsistan.sabahAcik(c))
            planSatirlariniYaz(view)
        }
        view.findViewById<View>(R.id.rowPlanAksam).setOnClickListener {
            val c = requireContext()
            PlanAsistan.aksamAcik(c, !PlanAsistan.aksamAcik(c))
            PlanAsistan.kur(c)
            planSatirlariniYaz(view)
        }
        view.findViewById<View>(R.id.planAksamDurum).setOnLongClickListener {
            aksamSaatSec(view)
            true
        }
        view.findViewById<View>(R.id.rowSaglik).setOnClickListener {
            startActivity(Intent(requireContext(), SaglikActivity::class.java))
        }
        view.findViewById<View>(R.id.rowAi).setOnClickListener { showAiDialog() }
        // v7.46: namaz modülü — tema satırına uzun basınca değil, kendi girişiyle
        view.findViewById<View>(R.id.rowAbout).setOnLongClickListener {
            namazModulu()
            true
        }
        updateAiSummary()
        refreshSyncRow()
        kocOzetiTazele()
    }

    // ═══════════════════════════════════════════════════════════════
    // v10.6 · D46 — Ayar içinde arama
    // ═══════════════════════════════════════════════════════════════
    //
    // Ayarlar 30+ satır; aradığını bulmak kaydırma işiydi. Kutu,
    // satırları id deseninden ("row*") bulur, içindeki metinleri toplar
    // ve AyarAra.eslesme ile süzer. Bölüm başlığı, grubunda görünür
    // satır kalmadıysa kendini gizler.

    private val aramaBolumEtiketleri: Set<String> by lazy {
        setOf(
            getString(R.string.asy_sec_hizli_kontroller),
            getString(R.string.asy_sec_gorunum_tema),
            getString(R.string.asy_sec_yapay_zeka),
            getString(R.string.asy_sec_konularim_ders),
            getString(R.string.asy_sec_yasam_saglik),
            getString(R.string.asy_sec_bildirim_alarm),
            getString(R.string.asy_sec_depolama_sistem),
            getString(R.string.asy_sec_hakkinda),
            getString(R.string.sec_app),
            getString(R.string.sec_data),
            getString(R.string.sec_about)
        )
    }

    private var aramaKutusuV: android.widget.EditText? = null

    private fun aramaKutusunuKur(kokGorunum: android.view.View) {
        val kaydirici = kokGorunum as? android.widget.ScrollView ?: return
        val kok = kaydirici.getChildAt(0) as? android.widget.LinearLayout ?: return
        val ctx = requireContext()
        val yg = resources.displayMetrics.density
        fun dp(v: Int) = (v * yg).toInt()

        val kutu = android.widget.EditText(ctx).apply {
            hint = getString(R.string.aa_ipucu)
            setSingleLine(true)
            textSize = 14f
            setPadding(dp(14), dp(10), dp(14), dp(10))
            inputType = android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            val ta = ctx.obtainStyledAttributes(
                intArrayOf(android.R.attr.selectableItemBackground)
            )
            background = ta.getDrawable(0)
            ta.recycle()
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
                override fun afterTextChanged(s: android.text.Editable?) {
                    filtreyiUygula(kok, s?.toString() ?: "")
                }
            })
        }
        aramaKutusuV = kutu
        kok.addView(kutu, 1) // büyük başlığın hemen altı
    }

    /** rowSub / rowRozet gibi satır İÇİ parçaları dışarıda bırakır. */
    private fun satirMi(idAdi: String): Boolean =
        idAdi.startsWith("row") && !idAdi.endsWith("Sub") && !idAdi.endsWith("Rozet")

    /** Ağacı gez: üstte `row*` id'li en dış kapsayıcıları topla. */
    private fun satirlariTopla(
        v: android.view.View,
        biriken: MutableList<android.view.View>,
        ataVar: Boolean
    ) {
        var buSatir = ataVar
        if (!ataVar && v.id != android.view.View.NO_ID) {
            val ad = runCatching { resources.getResourceEntryName(v.id) }.getOrDefault("")
            if (satirMi(ad)) {
                biriken.add(v)
                buSatir = true
            }
        }
        if (v is android.view.ViewGroup) {
            for (i in 0 until v.childCount) satirlariTopla(v.getChildAt(i), biriken, buSatir)
        }
    }

    private fun metniniCikar(v: android.view.View, sb: StringBuilder) {
        if (v is android.widget.TextView) sb.append(v.text).append(' ')
        if (v is android.view.ViewGroup) {
            for (i in 0 until v.childCount) metniniCikar(v.getChildAt(i), sb)
        }
    }

    private fun filtreyiUygula(kok: android.widget.LinearLayout, sorgu: String) {
        val satirlar = mutableListOf<android.view.View>()
        satirlariTopla(kok, satirlar, false)
        satirlar.forEach { s ->
            val sb = StringBuilder()
            metniniCikar(s, sb)
            s.isVisible = AyarAra.eslesme(sorgu, sb.toString())
        }
        // Tüm satırları gizlenen kart boş kalmasın (öz denetim ②):
        // kart, içinde görünür satır varsa görünür kalır.
        val gorunurler = satirlar.filter { it.isVisible }.toSet()
        fun ataIcindeMi(v: android.view.View, ata: android.view.View): Boolean {
            var p: android.view.ViewParent? = v.parent
            while (p != null && p !== kok) {
                if (p === ata) return true
                p = p.parent
            }
            return false
        }
        (0 until kok.childCount).forEach { i ->
            val c = kok.getChildAt(i)
            if (c is com.google.android.material.card.MaterialCardView) {
                c.isVisible = gorunurler.any { ataIcindeMi(it, c) }
            }
        }
        // Bölüm başlıkları: doğrudan çocuk TextView'lar; grubunda görünür
        // satır kalmadıysa gizle.
        val cocuklar = (0 until kok.childCount).map { kok.getChildAt(it) }
        cocuklar.forEachIndexed { i, c ->
            if (c is android.widget.TextView &&
                c !== aramaKutusuV &&
                aramaBolumEtiketleri.contains(c.text.toString())
            ) {
                // dikkat: indeksler üzerinden ilerliyoruz; `it` indekstir
                val grubundaVar = ((i + 1) until cocuklar.size)
                    .takeWhile { j ->
                        !(cocuklar[j] is android.widget.TextView &&
                            aramaBolumEtiketleri.contains(
                                (cocuklar[j] as android.widget.TextView).text.toString()
                            ))
                    }
                    .any { j -> cocuklar[j].isVisible }
                c.isVisible = grubundaVar
            }
        }
    }

    /** v7.78: koc ve kanit satirlarinin alt yazilari. */
    private fun kocOzetiTazele() {
        val kok = view ?: return
        kok.findViewById<TextView>(R.id.rowKocSub)?.text = if (Koc.acikMi(requireContext())) {
            // v7.79: takip edilen ders programını göster
            if (Mufredat.secildiMi(requireContext())) {
                Mufredat.durumMetni(requireContext())
            } else {
                getString(
                    R.string.koc_row_sub_acik,
                    Koc.sertlikAdi(requireContext(), Koc.sertlik(requireContext())),
                    Koc.gunlukHedef(requireContext())
                )
            }
        } else {
            getString(R.string.koc_row_sub)
        }

        // v7.84: sözlük durumu
        val szSayi = Sozluk.sayi(requireContext())
        kok.findViewById<TextView>(R.id.rowSozlukSub)?.text =
            if (szSayi == 0) getString(R.string.sz_row_sub)
            else getString(R.string.sz_row_sub_dolu, szSayi)

        // v7.83: hata defteri durumu
        val hOzet = Hatalarim.ozet(requireContext())
        kok.findViewById<TextView>(R.id.rowHatalarimSub)?.text =
            if (hOzet.toplam == 0) getString(R.string.ht_row_sub)
            else getString(R.string.ht_row_sub_dolu, hOzet.toplam, hOzet.bugun)

        kok.findViewById<TextView>(R.id.rowKanitSub)?.text =
            when (Kanit.politika(requireContext())) {
                Kanit.POL_KAPALI -> getString(R.string.kn_row_sub)
                Kanit.POL_HEPSI -> getString(R.string.kn_row_sub_hepsi)
                Kanit.POL_ETIKETLI -> getString(R.string.kn_row_sub_etiket)
                else -> getString(R.string.kn_row_sub_isaretli)
            }
    }

    /** v10.42: sabah/akşam satır alt metinleri (durum + saat). */
    private fun planSatirlariniYaz(kok: android.view.View) {
        val c = context ?: return
        kok.findViewById<android.widget.TextView>(R.id.planSabahDurum)?.text =
            if (PlanAsistan.sabahAcik(c)) getString(R.string.w42_acik)
            else getString(R.string.w42_kapali)
        kok.findViewById<android.widget.TextView>(R.id.planAksamDurum)?.text =
            if (PlanAsistan.aksamAcik(c)) {
                getString(R.string.w42_aksam_saat_acik, PlanAsistan.dakikaYaz(PlanAsistan.aksamDk(c)))
            } else getString(R.string.w42_kapali)
    }

    /** v10.42: akşam sorusu saati — uzun basışla saat seçimi (20–23). */
    private fun aksamSaatSec(kok: android.view.View) {
        val c = context ?: return
        val secici = android.widget.NumberPicker(c).apply {
            minValue = 20
            maxValue = 23
            value = PlanAsistan.aksamDk(c) / 60
            displayedValues = arrayOf("20:00", "21:00", "22:00", "23:00")
        }
        com.google.android.material.dialog.MaterialAlertDialogBuilder(c)
            .setTitle(R.string.w42_aksam_saat_baslik)
            .setView(secici)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                PlanAsistan.aksamDk(c, secici.value * 60)
                PlanAsistan.kur(c)
                planSatirlariniYaz(kok)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        runCatching {
            val sp = requireContext().getSharedPreferences("saglik_v1", android.content.Context.MODE_PRIVATE)
            val t = view?.findViewById<android.widget.TextView>(R.id.rowSaglikSub)
            if (t != null) {
                t.text = if (sp.getLong("zaman", 0L) > 0L) {
                    getString(
                        R.string.w37_son_ozet,
                        sp.getInt("iyi", 0), sp.getInt("uyari", 0),
                        sp.getInt("hata", 0), sp.getInt("onarildi", 0)
                    )
                } else {
                    getString(R.string.w37_son_yok)
                }
            }
        }
        kocOzetiTazele()
        takipRozetiTazele()
        sistemRozetiTazele()
        runCatching { Kullanim.ekran(requireContext(), Kullanim.Ekran.AYARLAR) }
    }

    /**
     * v9.8: sistem satırında bekleyen güncelleme rozetini gösterir.
     *
     * Ağ isteği YAPMIYOR — `bekleyenVar` yalnızca önbelleğe bakıyor.
     * Ayarlar ekranını açmak ağ beklemesine sebep olmamalı.
     */
    private fun sistemRozetiTazele() {
        val kok = view ?: return
        val rozet = kok.findViewById<TextView>(R.id.rowSistemRozet) ?: return
        val alt = kok.findViewById<TextView>(R.id.rowSistemSub)
        val yeni = runCatching { Guncelleme.bekleyenVar(requireContext()) }.getOrNull()
        if (yeni != null) {
            rozet.text = "v${yeni.ad}"
            rozet.visibility = View.VISIBLE
            runCatching { alt?.text = getString(R.string.gc_yeni_var) }
        } else {
            rozet.visibility = View.GONE
            runCatching { alt?.setText(R.string.sy_satir_alt) }
        }
    }

    /**
     * v9.7: günlük hayat satırında acil uyarı sayısını gösterir.
     *
     * Diski okuyor ama tek SharedPreferences dizesi — ölçülebilir
     * bir gecikme yaratmıyor. Yine de `runCatching` ile sarılı:
     * bozuk JSON ayarlar ekranını çökertmemeli.
     */
    private fun takipRozetiTazele() {
        val kok = view ?: return
        val rozet = kok.findViewById<TextView>(R.id.rowTakipRozet) ?: return
        val alt = kok.findViewById<TextView>(R.id.rowTakipSub)
        val sayi = runCatching { Takip.acilSayisi(requireContext()) }.getOrDefault(0)
        if (sayi > 0) {
            rozet.text = sayi.toString()
            rozet.visibility = View.VISIBLE
        } else {
            rozet.visibility = View.GONE
        }
        runCatching {
            val ozet = Takip.ozet(requireContext())
            alt?.text = ozet ?: getString(R.string.tk_satir_alt)
        }
    }

    private fun refreshSyncRow() {
        val view = view ?: return
        val sub = view.findViewById<TextView>(R.id.rowSyncSub) ?: return
        sub.text = if (Store.getAutoBackupEnabled(requireContext())) {
            val label = Store.lastBackupLabel(requireContext())
            if (label == "henüz yok") {
                getString(R.string.row_sync_sub_none)
            } else {
                getString(R.string.row_sync_sub_on, label)
            }
        } else {
            getString(R.string.row_sync_sub_off)
        }
    }

    private fun toggleAutoBackup() {
        val target = !Store.getAutoBackupEnabled(requireContext())
        Store.setAutoBackupEnabled(requireContext(), target)
        Toast.makeText(
            requireContext(),
            if (target) R.string.sync_toast_on else R.string.sync_toast_off,
            Toast.LENGTH_LONG
        ).show()
        refreshSyncRow()
    }

    private fun showNotificationSettings() {
        val context = requireContext()
        val density = resources.displayMetrics.density
        val pad = (8 * density).toInt()

        fun makeSwitch(textRes: Int, checked: Boolean): SwitchMaterial =
            SwitchMaterial(context).apply {
                setText(textRes)
                isChecked = checked
                setPadding(pad, pad, pad, pad)
            }

        val switchNotif = makeSwitch(R.string.notif_show, Store.getNotifEnabled(context))
        val switchSound = makeSwitch(R.string.notif_sound, Store.getSoundEnabled(context))
        val switchVib = makeSwitch(R.string.notif_vib, Store.getVibEnabled(context))
        // v7.56: kilitli olanlarin yanina 🔒
        switchNotif.text = BildirimKilit.etiket(
            context, getString(R.string.notif_show), OnlineStore.Islem.BILDIRIM_TUM_KAPAT
        )
        switchSound.text = BildirimKilit.etiket(
            context, getString(R.string.notif_sound), OnlineStore.Islem.SES_KAPAT
        )
        switchVib.text = BildirimKilit.etiket(
            context, getString(R.string.notif_vib), OnlineStore.Islem.TITRESIM_KAPAT
        )

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(switchNotif)
            addView(switchSound)
            addView(switchVib)
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.row_notif)
            .setView(container)
            .setPositiveButton(R.string.save) { _, _ ->
                // v7.56: yonetici kilidi — uye kapatamaz, acabilir
                val engel = mutableListOf<String>()
                fun uygula(
                    islem: OnlineStore.Islem,
                    yeni: Boolean,
                    eski: Boolean,
                    yaz: (Boolean) -> Unit
                ) {
                    if (!yeni && eski && BildirimKilit.kilitli(context, islem)) {
                        engel.add(BildirimKilit.mesaj(context, islem))
                    } else {
                        yaz(yeni)
                    }
                }
                uygula(
                    OnlineStore.Islem.BILDIRIM_TUM_KAPAT, switchNotif.isChecked,
                    Store.getNotifEnabled(context)
                ) { Store.setNotifEnabled(context, it) }
                uygula(
                    OnlineStore.Islem.SES_KAPAT, switchSound.isChecked,
                    Store.getSoundEnabled(context)
                ) { Store.setSoundEnabled(context, it) }
                uygula(
                    OnlineStore.Islem.TITRESIM_KAPAT, switchVib.isChecked,
                    Store.getVibEnabled(context)
                ) { Store.setVibEnabled(context, it) }
                if (engel.isNotEmpty()) {
                    MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.nset_kilitli)
                        .setMessage(engel.joinToString("\n\n"))
                        .setPositiveButton(R.string.done, null)
                        .show()
                }
                // v7.43: ana anahtar değişince zamanlayıcıyı güncelle
                if (switchNotif.isChecked) BildirimZamanlayici.kur(context)
                else BildirimZamanlayici.iptal(context)
                Toast.makeText(context, R.string.settings_saved, Toast.LENGTH_SHORT).show()
            }
            // v7.43: 20 türü ayrı ayrı yöneten detaylı ekran (öneri 26)
            .setNeutralButton(R.string.nset_menu) { _, _ ->
                BildirimAyarActivity.ac(context)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * v7.46: Namaz modülü aç/kapat.
     * Kapalıyken Bugün ekranında kart görünmez, hiçbir yerde yer kaplamaz.
     */
    private fun namazModulu() {
        val context = requireContext()
        val acik = NamazVakti.acikMi(context)
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.nm_title)
            .setMessage(
                if (acik) getString(R.string.nm_module_on, NamazVakti.sehirAdi(context))
                else getString(R.string.nm_module_off)
            )
            .setPositiveButton(
                if (acik) R.string.nm_module_open else R.string.nm_module_enable
            ) { _, _ ->
                if (!acik) NamazVakti.setAcik(context, true)
                NamazActivity.ac(context)
            }
            .setNeutralButton(
                if (acik) R.string.nm_module_disable else R.string.cancel
            ) { _, _ ->
                if (acik) {
                    NamazVakti.setAcik(context, false)
                    Toast.makeText(context, R.string.nm_module_closed, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** v7.17: yedekleme yöntemi seçtiren menü. */
    private fun shareBackup() {
        val ctx = requireContext()
        val ozet = Store.yedekOzeti(Store.exportJson(ctx))
        val bilgi = ozet?.let {
            getString(R.string.backup_current, it.ders, it.bitenDers, it.dersNotu)
        } ?: ""

        val secenekler = arrayOf(
            getString(R.string.backup_to_file),
            getString(R.string.backup_permanent),
            getString(R.string.backup_share),
            // v7.96: parola korumalı yedek (öneri 8)
            getString(R.string.ys_sifreli_paylas)
        )
        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.backup_title)
            .setMessage(bilgi)
            .setItems(secenekler) { _, hangi ->
                when (hangi) {
                    0 -> yedekKaydet.launch(yedekDosyaAdi())
                    1 -> kaliciYedekAl()
                    2 -> yedegiPaylas()
                    3 -> sifreliYedekAl()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * v7.18: İndirilenler klasörüne kalıcı yedek yazar.
     * Uygulama kaldırılsa bile bu dosya silinmez; sonraki kurulumda
     * uygulama açılışta bulup geri yüklemeyi teklif eder.
     */
    private fun kaliciYedekAl() {
        val ctx = requireContext()
        val ok = Store.kaliciYedekYaz(ctx)
        MaterialAlertDialogBuilder(ctx)
            .setTitle(if (ok) R.string.backup_permanent_ok else R.string.backup_file_fail)
            .setMessage(
                if (ok) getString(R.string.backup_permanent_detail, Store.KALICI_YEDEK_ADI)
                else getString(R.string.backup_permanent_fail)
            )
            .setPositiveButton(R.string.done, null)
            .show()
    }

    /** v7.17: GunlukAsistan-yedek-20260730.json */
    private fun yedekDosyaAdi(): String {
        val tarih = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
        return "GunlukAsistan-yedek-$tarih.json"
    }

    private fun yedegiPaylas() {
        val json = Store.exportJson(requireContext())
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.backup_title))
            putExtra(Intent.EXTRA_TEXT, json)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.backup_title)))
        Toast.makeText(requireContext(), R.string.backup_hint_saved, Toast.LENGTH_LONG).show()
    }

    /**
     * v7.17: Yedeği geri yüklemeden önce içeriğini gösterir ve onay ister.
     * Onaylanırsa mevcut durumun güvenlik kopyası alınır.
     */
    /**
     * v7.96 — Şifreli yedek algılanırsa önce parola sorulur (öneri 8).
     *
     * Çözme yavaş (PBKDF2 120k tur ≈ 1 sn) olduğu için arka planda
     * yapılıyor; ana iş parçacığında yapılsaydı arayüz donardı.
     */
    private fun sifreliGeriYukle(sifreli: String) {
        val ctx = context ?: return
        val giris = EditText(ctx).apply {
            hint = getString(R.string.ys_parola)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val kutu = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            val d = resources.displayMetrics.density
            setPadding((22 * d).toInt(), (12 * d).toInt(), (22 * d).toInt(), 0)
            addView(giris)
        }

        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.ys_sifreli_yedek)
            .setMessage(R.string.ys_parola_iste)
            .setView(kutu)
            .setPositiveButton(R.string.ys_coz) { _, _ ->
                val parola = giris.text?.toString().orEmpty()
                if (parola.isEmpty()) {
                    Toast.makeText(ctx, R.string.ys_parola_bos, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val bekle = MaterialAlertDialogBuilder(ctx)
                    .setMessage(R.string.ys_cozuluyor)
                    .setCancelable(false)
                    .create()
                bekle.show()

                Performans.arkaPlan {
                    val sonuc = YedekSifre.coz(sifreli, parola.toCharArray())
                    Performans.anaIs {
                        if (!isAdded) return@anaIs
                        runCatching { bekle.dismiss() }
                        when (sonuc) {
                            is YedekSifre.Sonuc.Basarili ->
                                onizlemeIleGeriYukle(sonuc.metin)
                            YedekSifre.Sonuc.ParolaYanlis ->
                                MaterialAlertDialogBuilder(ctx)
                                    .setTitle(R.string.ys_parola_yanlis)
                                    .setMessage(R.string.ys_parola_yanlis_alt)
                                    .setPositiveButton(R.string.ys_tekrar) { _, _ ->
                                        sifreliGeriYukle(sifreli)
                                    }
                                    .setNegativeButton(R.string.cancel, null)
                                    .show()
                            is YedekSifre.Sonuc.Hata ->
                                Toast.makeText(
                                    ctx, R.string.restore_invalid, Toast.LENGTH_LONG
                                ).show()
                        }
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * v7.96 — Parola korumalı yedek üretir ve paylaşır (öneri 8).
     *
     * Yedek dosyası notları, görevleri, sohbet geçmişini ve **API
     * anahtarlarını** içeriyor. Düz metin paylaşılırsa okuyan herkes
     * hepsini görür. Parola kaybedilirse yedek kurtarılamaz — uyarılıyor.
     */
    private fun sifreliYedekAl() {
        val ctx = context ?: return
        val giris = EditText(ctx).apply {
            hint = getString(R.string.ys_parola_belirle)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val gucEtiketi = TextView(ctx).apply {
            textSize = 12f
            alpha = 0.75f
            setPadding(0, (8 * resources.displayMetrics.density).toInt(), 0, 0)
        }
        giris.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(c: CharSequence?, a: Int, b: Int, d: Int) {}
            override fun onTextChanged(c: CharSequence?, a: Int, b: Int, d: Int) {}
            override fun afterTextChanged(e: android.text.Editable?) {
                val p = e?.toString().orEmpty()
                gucEtiketi.text = if (p.isEmpty()) "" else getString(
                    R.string.ys_guc, YedekSifre.gucAdi(ctx, YedekSifre.parolaGucu(p))
                )
            }
        })

        val kutu = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            val d = resources.displayMetrics.density
            setPadding((22 * d).toInt(), (12 * d).toInt(), (22 * d).toInt(), 0)
            addView(giris)
            addView(gucEtiketi)
        }

        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.ys_sifreli_paylas)
            .setMessage(R.string.ys_uyari)
            .setView(kutu)
            .setPositiveButton(R.string.ys_sifrele) { _, _ ->
                val parola = giris.text?.toString().orEmpty()
                if (parola.length < 6) {
                    Toast.makeText(ctx, R.string.ys_parola_kisa, Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                val bekle = MaterialAlertDialogBuilder(ctx)
                    .setMessage(R.string.ys_sifreleniyor)
                    .setCancelable(false)
                    .create()
                bekle.show()

                Performans.arkaPlan {
                    val duz = Store.exportJson(ctx)
                    val sifreli = YedekSifre.sifrele(duz, parola.toCharArray())
                    Performans.anaIs {
                        if (!isAdded) return@anaIs
                        runCatching { bekle.dismiss() }
                        if (sifreli == null) {
                            Toast.makeText(ctx, R.string.ys_hata, Toast.LENGTH_LONG).show()
                        } else {
                            sifreliPaylas(sifreli)
                        }
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** Şifreli metni dosya olarak paylaşır. */
    private fun sifreliPaylas(sifreli: String) {
        val ctx = context ?: return
        try {
            val klasor = java.io.File(ctx.cacheDir, "yedek").apply { mkdirs() }
            val dosya = java.io.File(
                klasor, yedekDosyaAdi().replace(".json", "-sifreli.txt")
            )
            dosya.writeText(sifreli, Charsets.UTF_8)

            val uri = androidx.core.content.FileProvider.getUriForFile(
                ctx, "${ctx.packageName}.fileprovider", dosya
            )
            startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, getString(R.string.ys_sifreli_yedek))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                    getString(R.string.ys_sifreli_paylas)
                )
            )
        } catch (e: Exception) {
            android.util.Log.w("SettingsFragment", "Şifreli yedek paylaşılamadı", e)
            Toast.makeText(ctx, R.string.ys_hata, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * v7.98 — Günlük yedek kopyaları (öneri 2).
     *
     * Tek yedek dosyası bozulursa ya da yanlışlıkla silinen veri yedeğe
     * de yansımışsa, önceki günlerin kopyasına dönmek tek çare.
     */
    private fun yedekGecmisi() {
        val ctx = context ?: return
        val kopyalar = YedekRotasyon.kopyalar(ctx)
        if (kopyalar.isEmpty()) {
            Toast.makeText(ctx, R.string.yr_henuz_yok, Toast.LENGTH_SHORT).show()
            return
        }

        val adlar = kopyalar.map { k ->
            k.tarihMetni() + "  ·  " + YedekRotasyon.boyutMetni(k.boyut)
        }.toTypedArray()

        MaterialAlertDialogBuilder(ctx)
            .setTitle(getString(R.string.yr_gecmis_baslik, kopyalar.size))
            .setItems(adlar) { _, hangi ->
                val icerik = YedekRotasyon.kopyaOku(kopyalar[hangi])
                if (icerik.isNullOrBlank()) {
                    Toast.makeText(ctx, R.string.yr_okunamadi, Toast.LENGTH_LONG).show()
                } else {
                    onizlemeIleGeriYukle(icerik)
                }
            }
            .setNeutralButton(R.string.yr_temizle) { _, _ ->
                MaterialAlertDialogBuilder(ctx)
                    .setMessage(getString(R.string.yr_temizle_sor, kopyalar.size))
                    .setPositiveButton(R.string.delete) { _, _ ->
                        val n = YedekRotasyon.temizle(ctx)
                        Toast.makeText(
                            ctx, getString(R.string.yr_temizlendi, n), Toast.LENGTH_SHORT
                        ).show()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun onizlemeIleGeriYukle(metin: String) {
        val ctx = context ?: return
        // v7.96: şifreli yedek — önce çöz
        if (YedekSifre.sifreliMi(metin)) {
            sifreliGeriYukle(metin)
            return
        }
        val ozet = Store.yedekOzeti(metin)
        if (ozet == null) {
            MaterialAlertDialogBuilder(ctx)
                .setTitle(R.string.restore_title)
                .setMessage(R.string.restore_invalid)
                .setPositiveButton(R.string.done, null)
                .show()
            return
        }

        val tarihMetni = if (ozet.tarih > 0) {
            SimpleDateFormat("d MMMM yyyy HH:mm", Locale("tr", "TR")).format(Date(ozet.tarih))
        } else {
            getString(R.string.restore_no_date)
        }

        val govde = buildString {
            append(getString(R.string.restore_preview_head, tarihMetni))
            append("\n\n")
            append(getString(R.string.restore_row_lessons, ozet.bitenDers, ozet.ders))
            append("\n")
            append(getString(R.string.restore_row_notes, ozet.dersNotu))
            append("\n")
            append(getString(R.string.restore_row_tasks, ozet.gorev))
            append("\n")
            append(getString(R.string.restore_row_topics, ozet.konu))
            append("\n")
            append(getString(R.string.restore_row_habits, ozet.aliskanlik))
            if (ozet.seriRekor > 0) {
                append("\n")
                append(getString(R.string.restore_row_streak, ozet.seriRekor))
            }
            if (ozet.ders == 0) {
                append("\n\n")
                append(getString(R.string.restore_old_format))
            }
            append("\n\n")
            append(getString(R.string.restore_warning))
        }

        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.restore_confirm_title)
            .setMessage(govde)
            .setPositiveButton(R.string.restore_btn) { _, _ ->
                Store.guvenlikKopyasiAl(ctx)
                // v8.8 · Öneri 4: sürüm kontrollü geri yükleme.
                //
                // Eskiden doğrudan importJson çağrılıyordu. Daha YENİ
                // bir sürümden gelen yedeği eski uygulamaya yüklemek
                // sessiz veri kaybına yol açıyordu: tanınmayan alanlar
                // okunmuyor, sonraki kaydetmede siliniyordu.
                when (val sonuc = Store.iceAktarDenetimli(ctx, metin)) {
                    is Store.IceAktarSonuc.Basarili -> geriYuklendiBildir()

                    is Store.IceAktarSonuc.CokYeni -> {
                        MaterialAlertDialogBuilder(ctx)
                            .setTitle(R.string.yd_cok_yeni_baslik)
                            .setMessage(
                                getString(
                                    R.string.yd_cok_yeni,
                                    sonuc.yedekSurum, sonuc.destek
                                )
                            )
                            .setPositiveButton(R.string.done, null)
                            .show()
                    }

                    is Store.IceAktarSonuc.YanlisUygulama ->
                        Toast.makeText(ctx, R.string.yd_yanlis_dosya, Toast.LENGTH_LONG).show()

                    is Store.IceAktarSonuc.Bozuk -> {
                        MaterialAlertDialogBuilder(ctx)
                            .setTitle(R.string.restore_fail)
                            .setMessage(getString(R.string.yd_bozuk, sonuc.neden))
                            .setPositiveButton(R.string.done, null)
                            .show()
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** v7.17: başarı mesajı + geri alma seçeneği. */
    private fun geriYuklendiBildir() {
        val ctx = context ?: return
        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.restore_ok)
            .setMessage(R.string.restore_ok_detail)
            .setPositiveButton(R.string.done) { _, _ ->
                activity?.recreate()
            }
            .setNegativeButton(R.string.restore_undo) { _, _ ->
                val geri = Store.geriAlYedek(ctx)
                Toast.makeText(
                    ctx,
                    if (geri) R.string.restore_undone else R.string.restore_fail,
                    Toast.LENGTH_LONG
                ).show()
                activity?.recreate()
            }
            .setCancelable(false)
            .show()
    }

    /** v7.17: üç geri yükleme yolu — dosya, cihaz yedeği, metin yapıştırma. */
    private fun showRestoreDialog() {
        val ctx = requireContext()
        val otomatik = Store.readAutoBackup(ctx)
        val geriAlVar = Store.geriAlinabilirYedekVar(ctx)

        val etiketler = mutableListOf(getString(R.string.restore_from_file))
        val eylemler = mutableListOf<() -> Unit>({
            yedekAc.launch(arrayOf("application/json", "text/plain", "*/*"))
        })

        if (!otomatik.isNullOrBlank()) {
            val oz = Store.yedekOzeti(otomatik)
            etiketler.add(
                if (oz != null) getString(R.string.restore_auto_with, oz.bitenDers, oz.ders)
                else getString(R.string.restore_auto)
            )
            eylemler.add { onizlemeIleGeriYukle(otomatik) }
        }

        // v7.98: günlük yedek geçmişi (öneri 2)
        val kopyalar = YedekRotasyon.kopyalar(ctx)
        if (kopyalar.isNotEmpty()) {
            etiketler.add(getString(R.string.yr_gecmis_ac, kopyalar.size))
            eylemler.add { yedekGecmisi() }
        }

        etiketler.add(getString(R.string.restore_paste))
        eylemler.add { metinYapistirDiyalogu() }

        if (geriAlVar) {
            etiketler.add(getString(R.string.restore_undo_last))
            eylemler.add {
                val ok = Store.geriAlYedek(ctx)
                Toast.makeText(
                    ctx,
                    if (ok) R.string.restore_undone else R.string.restore_fail,
                    Toast.LENGTH_LONG
                ).show()
                if (ok) activity?.recreate()
            }
        }

        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.restore_title)
            .setItems(etiketler.toTypedArray()) { _, hangi ->
                eylemler.getOrNull(hangi)?.invoke()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** v7.17: JSON metnini elle yapıştırma (eski yöntem, hâlâ kullanışlı). */
    private fun metinYapistirDiyalogu() {
        val ctx = requireContext()
        val input = EditText(ctx).apply {
            hint = getString(R.string.restore_hint)
            minLines = 4
            maxLines = 10
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }
        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.restore_paste)
            .setView(input)
            .setPositiveButton(R.string.restore_btn) { _, _ ->
                val metin = input.text?.toString().orEmpty()
                if (metin.isBlank()) {
                    Toast.makeText(ctx, R.string.restore_fail, Toast.LENGTH_LONG).show()
                } else {
                    onizlemeIleGeriYukle(metin)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showAboutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.app_name) + " · v" + getString(R.string.app_version))
            .setMessage(R.string.about_text)
            .setPositiveButton(R.string.done, null)
            .show()
    }
    // v10.59: Ana ekrandaki atölye/modül kısayol butonlarını açıp kapatma anahtarı
    private fun bindAnaEkranButonToggle(view: View) {
        val row = view.findViewById<View>(R.id.rowAnaEkranButonToggle) ?: return
        val sw = view.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.swAnaEkranButon) ?: return
        val altText = view.findViewById<TextView>(R.id.txtAnaEkranButonAlt) ?: return

        fun guncelleArayuz() {
            val goster = Store.getAtolyeButonlariGoster(requireContext())
            sw.isChecked = goster
            altText.text = AnaEkranButonKarari.altMetinGetir(goster)
        }

        guncelleArayuz()

        row.setOnClickListener {
            val yeniDurum = !Store.getAtolyeButonlariGoster(requireContext())
            Store.setAtolyeButonlariGoster(requireContext(), yeniDurum)
            guncelleArayuz()
            val mesaj = AnaEkranButonKarari.durumMetniGetir(yeniDurum)
            Toast.makeText(requireContext(), mesaj, Toast.LENGTH_SHORT).show()
        }
    }

    private fun bindNamazAylikToggle(view: View) {
        val row = view.findViewById<View>(R.id.rowNamazAylikToggle) ?: return
        val sw = view.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.swNamazAylik) ?: return
        val altText = view.findViewById<TextView>(R.id.txtNamazAylikAlt) ?: return

        fun guncelleArayuz() {
            val otoAcik = NamazAylikVeriServisi.otomatikAylikGuncellemeAktifMi(requireContext())
            val titAcik = NamazAylikVeriServisi.namazTitresimAktifMi(requireContext())
            val acik = otoAcik && titAcik
            sw.isChecked = acik
            val sehir = NamazAylikVeriServisi.seciliSehirGetir(requireContext())
            altText.text = if (acik) {
                "📍 $sehir: 30 günlük namaz saatleri internetten otomatik güncelleniyor · Titreşim AÇIK"
            } else {
                "🕌 Aylık otomatik senkron ve titreşim uyarısı KAPALI"
            }
        }

        guncelleArayuz()

        row.setOnClickListener {
            val mevcutDurum = sw.isChecked
            val yeniDurum = !mevcutDurum
            NamazAylikVeriServisi.otomatikAylikGuncellemeKaydet(requireContext(), yeniDurum)
            NamazAylikVeriServisi.namazTitresimKaydet(requireContext(), yeniDurum)
            guncelleArayuz()
            val mesaj = if (yeniDurum) {
                "🕌 Aylık namaz saatleri otomatik alınacak & vakitte titretecek."
            } else {
                "🕌 Aylık namaz saatleri & titreşim özelliği kapatıldı."
            }
            Toast.makeText(requireContext(), mesaj, Toast.LENGTH_SHORT).show()
        }
    }

    private fun bindMotivasyonMansetToggle(view: View) {
        val row = view.findViewById<View>(R.id.rowMotivasyonMansetToggle) ?: return
        val sw = view.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.swMotivasyonManset) ?: return
        val altText = view.findViewById<TextView>(R.id.txtMotivasyonMansetAlt) ?: return

        fun guncelleArayuz() {
            val goster = MotivasyonMansetMotoru.mansetGosterilsinMi(requireContext())
            sw.isChecked = goster
            altText.text = if (goster) {
                "📜 AÇIK: Ana ekranda Stoacı & Sokratik motivasyon sözü gösteriliyor (#12)"
            } else {
                "📜 KAPALI: Ana ekrandaki motivasyon manşeti gizlendi"
            }
        }

        guncelleArayuz()

        row.setOnClickListener {
            val yeniDurum = !sw.isChecked
            MotivasyonMansetMotoru.setMansetGosterilsinMi(requireContext(), yeniDurum)
            guncelleArayuz()
            val mesaj = if (yeniDurum) {
                "📜 Sokratik & Felsefi Motivasyon Manşeti AÇIK"
            } else {
                "📜 Motivasyon Manşeti KAPALI"
            }
            Toast.makeText(requireContext(), mesaj, Toast.LENGTH_SHORT).show()
        }
    }

    private fun bindGlassmorphismToggle(view: View) {
        val row = view.findViewById<View>(R.id.rowGlassmorphismToggle) ?: return
        val sw = view.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.swGlassmorphism) ?: return
        val altText = view.findViewById<TextView>(R.id.txtGlassmorphismAlt) ?: return

        fun guncelleArayuz() {
            val goster = GlassmorphismTemaMotoru.temaAktifMi(requireContext())
            sw.isChecked = goster
            val (_, detay) = GlassmorphismTemaMotoru.temaDurumMetniGetir(goster)
            altText.text = detay
        }

        guncelleArayuz()

        row.setOnClickListener {
            val yeniDurum = !sw.isChecked
            GlassmorphismTemaMotoru.setTemaAktif(requireContext(), yeniDurum)
            guncelleArayuz()
            GlassmorphismTemaMotoru.sekmeleriVeKartlariStille(view, requireContext())
            val (b, _) = GlassmorphismTemaMotoru.temaDurumMetniGetir(yeniDurum)
            Toast.makeText(requireContext(), b, Toast.LENGTH_SHORT).show()
        }
    }

    private fun bindKpssModuToggle(view: View) {
        val row = view.findViewById<View>(R.id.rowKpssModuToggle) ?: return
        val sw = view.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.swKpssModu) ?: return
        val altText = view.findViewById<TextView>(R.id.txtKpssModuAlt) ?: return
        val yonetimRow = view.findViewById<View>(R.id.rowKpssMerkeziYonetim)

        fun guncelleArayuz() {
            val kpssAcik = KpssModuKararMotoru.kpssModuAktifMi(requireContext())
            sw.isChecked = kpssAcik
            val (_, detay) = KpssModuKararMotoru.durumMetniGetir(kpssAcik)
            altText.text = detay
            yonetimRow?.visibility = if (kpssAcik) View.VISIBLE else View.GONE
        }

        guncelleArayuz()

        row.setOnClickListener {
            val yeniDurum = !sw.isChecked
            KpssModuKararMotoru.setKpssModuAktif(requireContext(), yeniDurum)
            guncelleArayuz()
            val (baslik, _) = KpssModuKararMotoru.durumMetniGetir(yeniDurum)
            Toast.makeText(requireContext(), baslik, Toast.LENGTH_SHORT).show()
        }
    }

    private fun bindTabloBaslikToggle(view: View) {
        val row = view.findViewById<View>(R.id.rowTabloBaslikToggle) ?: return
        val sw = view.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.swTabloBaslik) ?: return
        val altText = view.findViewById<TextView>(R.id.txtTabloBaslikAlt) ?: return

        fun guncelleArayuz() {
            val goster = TabloBaslikYonetimMotoru.tabloBasliklariGosterilsinMi(requireContext())
            sw.isChecked = goster
            val (_, detay) = TabloBaslikYonetimMotoru.durumMetniGetir(requireContext())
            altText.text = detay
        }

        guncelleArayuz()

        row.setOnClickListener {
            val yeniDurum = !sw.isChecked
            TabloBaslikYonetimMotoru.setTabloBasliklariGosterilsin(requireContext(), yeniDurum)
            guncelleArayuz()
            val (baslik, _) = TabloBaslikYonetimMotoru.durumMetniGetir(requireContext())
            Toast.makeText(requireContext(), baslik, Toast.LENGTH_SHORT).show()
        }
    }

    // ---------------- Yapay zekâ ayarları (v5.5) ----------------

    private fun updateAiSummary() {
        val view = view ?: return
        val context = context ?: return
        val sub = view.findViewById<TextView>(R.id.rowAiSub) ?: return
        sub.text = when {
            AiSettings.isReady(context) -> {
                val p = AiClient.Provider.fromId(AiSettings.getProviderId(context))
                getString(R.string.row_ai_sub_on, p.label)
            }
            AiSettings.isOnlineMode(context) -> getString(R.string.row_ai_sub_nokey)
            else -> getString(R.string.row_ai_sub_off)
        }
    }

    /**
     * v7.24: Her sağlayıcının anahtarını ayrı ayrı yönetir.
     * Birden çok anahtar tanımlıysa biri tükendiğinde diğerine geçilir.
     */
    private fun anahtarYoneticisi(onChanged: () -> Unit) {
        val context = requireContext()
        val saglayicilar = AiClient.Provider.entries.filter { it != AiClient.Provider.CUSTOM }

        fun etiketler(): Array<String> = saglayicilar.map { p ->
            val onizleme = AiSettings.maskedKeyPreviewFor(context, p.id)
            val durum = if (onizleme.isBlank()) getString(R.string.ai_key_none) else onizleme
            "${if (onizleme.isBlank()) "○" else "●"}  ${p.label}\n     $durum"
        }.toTypedArray()

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.ai_keys_title)
            .setItems(etiketler()) { _, hangi ->
                val p = saglayicilar[hangi]
                val giris = EditText(context).apply {
                    hint = getString(R.string.ai_key_hint)
                    inputType = InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_VARIATION_PASSWORD
                    setText(AiSettings.getKeyFor(context, p.id))
                    val pad = (16 * resources.displayMetrics.density).toInt()
                    setPadding(pad, pad, pad, pad)
                }
                MaterialAlertDialogBuilder(context)
                    .setTitle(p.label)
                    .setMessage(getString(R.string.ai_key_where, p.keyUrl))
                    .setView(giris)
                    .setPositiveButton(R.string.save) { _, _ ->
                        AiSettings.setKeyFor(context, p.id, giris.text.toString())
                        onChanged()
                        Toast.makeText(context, R.string.settings_saved, Toast.LENGTH_SHORT).show()
                        anahtarYoneticisi(onChanged)
                    }
                    .setNeutralButton(R.string.ai_key_clear) { _, _ ->
                        AiSettings.setKeyFor(context, p.id, "")
                        onChanged()
                        anahtarYoneticisi(onChanged)
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
            .setPositiveButton(R.string.done, null)
            .show()
    }

    private fun showAiDialog() {
        val context = requireContext()
        val dp = resources.displayMetrics.density
        val pad = (20 * dp).toInt()

        val modeSwitch = SwitchMaterial(context).apply {
            setText(R.string.ai_mode_switch)
            isChecked = AiSettings.isOnlineMode(context)
        }

        val info = TextView(context).apply {
            setText(R.string.ai_privacy_note)
            textSize = 12f
            alpha = 0.75f
            setPadding(0, (6 * dp).toInt(), 0, (10 * dp).toInt())
        }

        val providers = AiClient.Provider.entries
        val spinner = Spinner(context).apply {
            adapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                providers.map { it.label }
            )
            setSelection(providers.indexOfFirst { it.id == AiSettings.getProviderId(context) }
                .coerceAtLeast(0))
        }

        val keyInput = EditText(context).apply {
            hint = getString(R.string.ai_key_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setText(AiSettings.getApiKey(context))
        }

        val modelInput = EditText(context).apply {
            hint = getString(R.string.ai_model_hint)
            inputType = InputType.TYPE_CLASS_TEXT
            setText(AiSettings.getModel(context))
        }

        // Hazır model seçenekleri (sağlayıcıya göre değişir)
        val modelSpinner = Spinner(context)

        // v7.22: sağlayıcıdan gerçek model listesini çek.
        // Model adları değiştiğinde kullanıcı güncel listeyi görebilsin.
        val modelYenile = TextView(context).apply {
            setText(R.string.ai_refresh_models)
            textSize = 12f
            setPadding(0, (8 * dp).toInt(), 0, (4 * dp).toInt())
            setTextColor(
                com.google.android.material.color.MaterialColors.getColor(
                    context, com.google.android.material.R.attr.colorPrimary, 0
                )
            )
            setOnClickListener {
                if (!AiSettings.hasApiKey(context)) {
                    Toast.makeText(context, R.string.ai_err_no_key, Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                // Girilen anahtar henüz kaydedilmemiş olabilir — geçici olarak yaz
                AiSettings.setApiKey(context, keyInput.text.toString())
                AiSettings.setProviderId(context, providers[spinner.selectedItemPosition].id)
                setText(R.string.ai_refreshing_models)
                isEnabled = false
                Thread {
                    val (ok, liste) = AiClient.canliModelListesi(context)
                    activity?.runOnUiThread {
                        setText(R.string.ai_refresh_models)
                        isEnabled = true
                        if (!ok || liste.isEmpty()) {
                            Toast.makeText(
                                context, R.string.ai_models_fail, Toast.LENGTH_LONG
                            ).show()
                            return@runOnUiThread
                        }
                        MaterialAlertDialogBuilder(context)
                            .setTitle(getString(R.string.ai_models_found, liste.size))
                            .setItems(liste.toTypedArray()) { _, hangi ->
                                modelInput.setText(liste[hangi])
                                modelInput.visibility = View.VISIBLE
                                Toast.makeText(
                                    context,
                                    getString(R.string.ai_model_picked, liste[hangi]),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .setNegativeButton(R.string.cancel, null)
                            .show()
                    }
                }.start()
            }
        }

        val endpointInput = EditText(context).apply {
            hint = getString(R.string.ai_endpoint_hint)
            inputType = InputType.TYPE_TEXT_VARIATION_URI
            setText(AiSettings.getCustomEndpoint(context))
        }

        val fallbackSwitch = SwitchMaterial(context).apply {
            setText(R.string.ai_fallback_switch)
            isChecked = AiSettings.isFallbackEnabled(context)
        }

        // v7.24: sağlayıcılar arası otomatik geçiş
        val autoSwitch = SwitchMaterial(context).apply {
            setText(R.string.ai_auto_switch)
            isChecked = AiSettings.isAutoSwitch(context)
        }
        val autoSwitchHelp = TextView(context).apply {
            setText(R.string.ai_auto_switch_desc)
            textSize = 11f
            alpha = 0.7f
            setPadding(0, 0, 0, (6 * dp).toInt())
        }

        // v7.34: Sadece ücretsiz modeller
        val ucretsizSwitch = SwitchMaterial(context).apply {
            setText(R.string.ai_free_only)
            isChecked = AiSettings.isUcretsizMod(context)
        }
        val ucretsizHelp = TextView(context).apply {
            setText(R.string.ai_free_only_desc)
            textSize = 11f
            alpha = 0.7f
            setPadding(0, 0, 0, (8 * dp).toInt())
        }
        // Seçili sağlayıcı/modelin ücret durumunu gösteren canlı rozet
        val ucretRozet = TextView(context).apply {
            textSize = 12f
            setPadding(0, (6 * dp).toInt(), 0, (2 * dp).toInt())
        }

        // v7.24: her sağlayıcının anahtarını ayrı yönet
        val keysBtn = TextView(context).apply {
            textSize = 12f
            setPadding(0, (8 * dp).toInt(), 0, (4 * dp).toInt())
            setTextColor(
                com.google.android.material.color.MaterialColors.getColor(
                    context, com.google.android.material.R.attr.colorPrimary, 0
                )
            )
            fun tazele() {
                val kayitli = AiSettings.anahtarliSaglayicilar(context).size
                text = getString(R.string.ai_manage_keys, kayitli)
            }
            tazele()
            setOnClickListener {
                // Ekrandaki anahtarı önce kaydet ki listede görünsün
                val secili = providers[spinner.selectedItemPosition]
                AiSettings.setKeyFor(context, secili.id, keyInput.text.toString())
                anahtarYoneticisi { tazele() }
            }
        }

        // v7.20: Kaynak Merkezi video aramaları için isteğe bağlı YouTube anahtarı
        val ytInput = EditText(context).apply {
            hint = getString(R.string.ai_yt_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setText(AiSettings.getYoutubeKey(context))
        }
        val ytHelp = TextView(context).apply {
            setText(R.string.ai_yt_desc)
            textSize = 11f
            alpha = 0.7f
        }

        val keyHelp = TextView(context).apply {
            textSize = 11f
            alpha = 0.7f
        }

        // Gemini için hızlı başlangıç kutusu (anahtar yoksa görünür)
        val quickStart = TextView(context).apply {
            setText(R.string.ai_gemini_quickstart)
            textSize = 12f
            setPadding(
                (14 * dp).toInt(), (12 * dp).toInt(), (14 * dp).toInt(), (12 * dp).toInt()
            )
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 14 * dp
                setColor(
                    com.google.android.material.color.MaterialColors.getColor(
                        context,
                        com.google.android.material.R.attr.colorPrimaryContainer,
                        0xFFEFE2D0.toInt()
                    )
                )
            }
        }

        fun refreshHelp() {
            val p = providers[spinner.selectedItemPosition]
            keyHelp.text = when (p) {
                AiClient.Provider.CUSTOM -> getString(R.string.ai_help_custom)
                AiClient.Provider.GEMINI -> getString(R.string.ai_help_gemini, p.keyUrl)
                // v7.25: OpenRouter'da ücretsiz model ipucu
                AiClient.Provider.OPENROUTER ->
                    getString(R.string.ai_help_key, p.keyUrl, p.defaultModel) +
                        "\n" + getString(R.string.ai_or_free_hint)
                else -> getString(R.string.ai_help_key, p.keyUrl, p.defaultModel)
            }
            endpointInput.visibility =
                if (p == AiClient.Provider.CUSTOM) View.VISIBLE else View.GONE

            quickStart.visibility =
                if (p == AiClient.Provider.GEMINI && keyInput.text.isBlank()) View.VISIBLE
                else View.GONE

            // v7.34: ücret durumu rozeti
            ucretRozet.text = when (p) {
                AiClient.Provider.GEMINI -> getString(R.string.ai_cost_gemini)
                AiClient.Provider.OPENAI -> getString(R.string.ai_cost_openai)
                AiClient.Provider.OPENROUTER -> getString(R.string.ai_cost_openrouter)
                AiClient.Provider.CUSTOM -> getString(R.string.ai_cost_custom)
            }

            // Model listesini sağlayıcıya göre yenile
            // v7.34: ücretsiz mod açıkken yalnızca ücretsiz modeller listelenir
            val tumPresets = p.presetModels
            val presets = if (ucretsizSwitch.isChecked) {
                tumPresets.filter { AiClient.modelUcretsizMi(p, it) }.ifEmpty { tumPresets }
            } else {
                tumPresets
            }
            if (presets.isEmpty()) {
                modelSpinner.visibility = View.GONE
            } else {
                modelSpinner.visibility = View.VISIBLE
                val labels = presets + getString(R.string.ai_model_custom)
                modelSpinner.adapter = ArrayAdapter(
                    context, android.R.layout.simple_spinner_dropdown_item, labels
                )
                val current = AiSettings.getModel(context).ifBlank { p.defaultModel }
                val idx = presets.indexOf(current)
                modelSpinner.setSelection(if (idx >= 0) idx else labels.size - 1)
                modelInput.visibility = if (idx >= 0) View.GONE else View.VISIBLE
                modelSpinner.onItemSelectedListener =
                    object : android.widget.AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long
                        ) {
                            if (pos < presets.size) {
                                modelInput.setText(presets[pos])
                                modelInput.visibility = View.GONE
                            } else {
                                modelInput.visibility = View.VISIBLE
                            }
                        }
                        override fun onNothingSelected(p0: android.widget.AdapterView<*>?) {}
                    }
            }
        }
        refreshHelp()
        // v7.34: ücretsiz mod değişince model listesi anında filtrelenir
        ucretsizSwitch.setOnCheckedChangeListener { _, _ -> refreshHelp() }
        var oncekiSaglayici = providers[spinner.selectedItemPosition].id
        spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?, v: View?, position: Int, id: Long
            ) {
                // v7.24: ayrılan sağlayıcının anahtarını sakla, yeninin anahtarını göster
                val yeniId = providers[position].id
                if (yeniId != oncekiSaglayici) {
                    AiSettings.setKeyFor(context, oncekiSaglayici, keyInput.text.toString())
                    keyInput.setText(AiSettings.getKeyFor(context, yeniId))
                    oncekiSaglayici = yeniId
                }
                refreshHelp()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        fun label(res: Int) = TextView(context).apply {
            setText(res)
            textSize = 12f
            setPadding(0, (12 * dp).toInt(), 0, (2 * dp).toInt())
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, (8 * dp).toInt(), pad, 0)
            addView(modeSwitch)
            addView(info)
            addView(label(R.string.ai_provider_label))
            addView(spinner)
            addView(ucretRozet)
            addView(quickStart)
            addView(label(R.string.ai_key_label))
            addView(keyInput)
            addView(keyHelp)
            addView(label(R.string.ai_model_label))
            addView(modelSpinner)
            addView(modelInput)
            addView(modelYenile)
            addView(endpointInput)
            addView(keysBtn)
            addView(fallbackSwitch)
            addView(autoSwitch)
            addView(autoSwitchHelp)
            addView(ucretsizSwitch)
            addView(ucretsizHelp)
            addView(label(R.string.ai_yt_title))
            addView(ytInput)
            addView(ytHelp)
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.ai_dialog_title)
            .setView(androidx.core.widget.NestedScrollView(context).apply { addView(container) })
            .setPositiveButton(R.string.save) { _, _ ->
                val chosen = providers[spinner.selectedItemPosition]
                AiSettings.setProviderId(context, chosen.id)
                AiSettings.setApiKey(context, keyInput.text.toString())
                AiSettings.setModel(context, modelInput.text.toString())
                AiSettings.setCustomEndpoint(context, endpointInput.text.toString())
                AiSettings.setFallbackEnabled(context, fallbackSwitch.isChecked)
                AiSettings.setAutoSwitch(context, autoSwitch.isChecked)
                // v7.34: sadece ücretsiz modeller
                AiSettings.setUcretsizMod(context, ucretsizSwitch.isChecked)
                // v7.24: anahtarı sağlayıcısıyla birlikte sakla
                AiSettings.setKeyFor(context, chosen.id, keyInput.text.toString())
                AiSettings.setYoutubeKey(context, ytInput.text.toString())

                val wantOnline = modeSwitch.isChecked
                if (wantOnline && !AiSettings.hasApiKey(context)) {
                    AiSettings.setOnlineMode(context, false)
                    Toast.makeText(context, R.string.ai_need_key, Toast.LENGTH_LONG).show()
                } else {
                    AiSettings.setOnlineMode(context, wantOnline)
                    Toast.makeText(
                        context,
                        if (wantOnline) R.string.ai_saved_online else R.string.ai_saved_offline,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                updateAiSummary()
            }
            .setNeutralButton(R.string.ai_test) { _, _ ->
                val chosen = providers[spinner.selectedItemPosition]
                AiSettings.setProviderId(context, chosen.id)
                AiSettings.setApiKey(context, keyInput.text.toString())
                AiSettings.setModel(context, modelInput.text.toString())
                AiSettings.setCustomEndpoint(context, endpointInput.text.toString())
                AiSettings.setOnlineMode(context, true)
                testConnection()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** Bağlantıyı gerçek bir istekle sınar. */
    private fun testConnection() {
        val context = requireContext()
        Toast.makeText(context, R.string.ai_testing, Toast.LENGTH_SHORT).show()
        Thread {
            val result = AiClient.chat(context, "Merhaba, tek cümlede kendini tanıt.")
            activity?.runOnUiThread {
                if (!isAdded) return@runOnUiThread
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(
                        if (result.ok) R.string.ai_test_ok_title else R.string.ai_test_fail_title
                    )
                    .setMessage(result.text)
                    .setPositiveButton(R.string.ok, null)
                    .show()
                if (!result.ok) AiSettings.setOnlineMode(context, false)
                updateAiSummary()
            }
        }.start()
    }

    /** v9.0: konu tekrarı satırının alt yazısını günceller. */
    private fun tekrarSatiriniTazele(kok: View) {
        val ctx = context ?: return
        val alt = kok.findViewById<TextView>(R.id.rowKonuTekrarSub) ?: return
        alt.text = if (KonuTekrar.acikMi(ctx)) {
            getString(
                R.string.kt_row_acik,
                KonuTekrar.toplamSayi(ctx),
                KonuTekrar.bugunkuSayi(ctx)
            )
        } else {
            getString(R.string.kt_row_sub)
        }
    }

    // ═══════════════════════════════════════════════════════════
    // v10.22 — Gizlilik Kilidi (PIN) ayar akışları
    // ═══════════════════════════════════════════════════════════

    /** Veri bölümündeki satırın alt yazısı: kapalı / açık + kilit süresi. */
    private fun kilitOzetiTazele() {
        val kok = view ?: return
        val alt = kok.findViewById<TextView>(R.id.rowKilitSub) ?: return
        val ctx = context ?: return
        alt.text = if (KilitDepo.kuruluMu(ctx)) {
            val adlar = resources.getStringArray(R.array.w22_zaman_adlari)
            getString(R.string.w22_satir_acik, adlar[KilitDepo.zamanAsimiDizin(ctx)])
        } else {
            getString(R.string.w22_satir_alt)
        }
    }

    private fun kilitMenusunuGoster() {
        val ctx = context ?: return
        if (!KilitDepo.kuruluMu(ctx)) {
            pinKurmaDiyalogu()
            return
        }
        val adlar = resources.getStringArray(R.array.w22_zaman_adlari)
        val secenekler = arrayOf(
            getString(R.string.w22_menu_degistir),
            getString(R.string.w22_menu_zaman, adlar[KilitDepo.zamanAsimiDizin(ctx)]),
            getString(R.string.w22_menu_kaldir)
        )
        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.w22_menu_baslik)
            .setItems(secenekler) { _, hangi ->
                when (hangi) {
                    0 -> pinDogrulaIste { pinKurmaDiyalogu() }
                    1 -> zamanAsimiDiyalogu()
                    2 -> pinDogrulaIste { kilitKaldirmaOnayi() }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Kurma/değiştirme ortak: iki kez girilen yeni PIN eşleşirse kaydeder.
     *
     * v10.23 · Hatasızlık md 2+10: hata anında diyalog KAPANMAZ.
     * Eskiden pozitif düğme Toast basıp diyaloğu kapatıyordu; kullanıcı
     * işleme baştan başlamak zorunda kalıyordu ("hata var" hissi).
     * Standart keep-open deseni: listener null + setOnShowListener.
     */
    private fun pinKurmaDiyalogu() {
        val ctx = context ?: return
        val yeni = pinKutusu(R.string.w22_pin_yeni)
        val tekrar = pinKutusu(R.string.w22_pin_tekrar)
        val hataKutusu = hataMetinKutusu(ctx)
        val kutu = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            val d = resources.displayMetrics.density
            setPadding((22 * d).toInt(), (12 * d).toInt(), (22 * d).toInt(), 0)
            addView(yeni)
            addView(tekrar)
            addView(hataKutusu)
        }
        val diyalog = MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.w22_menu_baslik)
            .setView(kutu)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create()
        diyalog.setOnShowListener {
            diyalog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    val p1 = yeni.text?.toString().orEmpty()
                    val p2 = tekrar.text?.toString().orEmpty()
                    when {
                        !KilitMantik.pinGecerliMi(p1) -> {
                            hataKutusu.setText(R.string.w22_hata_kural)
                            yeni.text?.clear()
                            yeni.requestFocus()
                        }
                        p1 != p2 -> {
                            hataKutusu.setText(R.string.w22_hata_eslesme)
                            tekrar.text?.clear()
                            tekrar.requestFocus()
                        }
                        else -> {
                            KilitDepo.pinKaydet(ctx, p1)
                            // Kuran/değiştiren oturum açık sayılır — ayar yapar
                            // yapmaz kilit ekranına düşmek saçma olurdu.
                            KilitDepo.dogruKaydet(ctx)
                            diyalog.dismiss()
                            Toast.makeText(ctx, R.string.w22_kuruldu, Toast.LENGTH_SHORT).show()
                            kilitOzetiTazele()
                        }
                    }
                }
        }
        diyalog.show()
    }

    /** Değiştir/kaldır öncesi mevcut PIN doğrulaması (hatada açık kalır). */
    private fun pinDogrulaIste(tamam: () -> Unit) {
        val ctx = context ?: return
        val giris = pinKutusu(R.string.w22_pin_mevcut)
        val hataKutusu = hataMetinKutusu(ctx)
        val kutu = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            val d = resources.displayMetrics.density
            setPadding((22 * d).toInt(), (12 * d).toInt(), (22 * d).toInt(), 0)
            addView(giris)
            addView(hataKutusu)
        }
        val diyalog = MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.w22_dogrula_baslik)
            .setView(kutu)
            .setPositiveButton(R.string.w22_dogrula_dugme, null)
            .setNegativeButton(R.string.cancel, null)
            .create()
        diyalog.setOnShowListener {
            diyalog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    val girilen = giris.text?.toString().orEmpty()
                    if (KilitDepo.pinDogruMu(ctx, girilen)) {
                        diyalog.dismiss()
                        tamam()
                    } else {
                        hataKutusu.setText(R.string.w22_hata_yanlis)
                        giris.text?.clear()
                        giris.requestFocus()
                    }
                }
        }
        diyalog.show()
    }

    /** v10.23: diyalog içi hata satırı (keep-open deseninin parçası). */
    private fun hataMetinKutusu(ctx: android.content.Context): TextView {
        return TextView(ctx).apply {
            textSize = 12f
            setTextColor(
                com.google.android.material.color.MaterialColors.getColor(
                    ctx, com.google.android.material.R.attr.colorError, 0xFFD9534F.toInt()
                )
            )
            setPadding(0, (6 * resources.displayMetrics.density).toInt(), 0, 0)
        }
    }

    private fun kilitKaldirmaOnayi() {
        val ctx = context ?: return
        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.w22_kaldir_baslik)
            .setMessage(R.string.w22_kaldir_mesaj)
            .setPositiveButton(R.string.w22_menu_kaldir) { _, _ ->
                KilitDepo.kaldir(ctx)
                Toast.makeText(ctx, R.string.w22_kaldirildi, Toast.LENGTH_SHORT).show()
                kilitOzetiTazele()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun zamanAsimiDiyalogu() {
        val ctx = context ?: return
        val adlar = resources.getStringArray(R.array.w22_zaman_adlari)
        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.w22_zaman_baslik)
            .setSingleChoiceItems(adlar, KilitDepo.zamanAsimiDizin(ctx)) { diyalog, hangi ->
                KilitDepo.zamanAsimiAyarla(ctx, KilitMantik.ZAMAN_ASIMLARI[hangi])
                KilitDepo.Oturum.acik = true // seçim yapan kullanıcı oturumda
                diyalog.dismiss()
                kilitOzetiTazele()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** Rakam + parola maskeli PIN giriş kutusu (azami [KilitMantik.PIN_MAX]). */
    private fun pinKutusu(ipucuRes: Int): EditText {
        return EditText(requireContext()).apply {
            hint = getString(ipucuRes)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            filters = arrayOf(android.text.InputFilter.LengthFilter(KilitMantik.PIN_MAX))
        }
    }
}
