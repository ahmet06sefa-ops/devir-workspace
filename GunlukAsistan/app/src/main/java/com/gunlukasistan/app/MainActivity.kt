package com.gunlukasistan.app

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var drawer: DrawerLayout
    private val books = mutableListOf<Store.Book>()
    private var libAdapter: BookAdapter? = null

    /** Elle bölme ekranından dönen noktalar. */
    private var pendingSplitBook: Store.Book? = null
    private var pendingSplitUri: android.net.Uri? = null

    private val manualSplitLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        val book = pendingSplitBook
        val uri = pendingSplitUri
        pendingSplitBook = null; pendingSplitUri = null
        if (res.resultCode != RESULT_OK || book == null || uri == null) return@registerForActivityResult
        val points = res.data?.getStringExtra(ManualSplitActivity.RESULT_POINTS) ?: return@registerForActivityResult
        runSplit(book, uri, manualPoints = points)
    }

    /** PDF/dosya seçici — kalıcı okuma izni alır. */
    private val pickBook = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        try {
            contentResolver.takePersistableUriPermission(
                uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
        }
        val name = queryFileName(uri) ?: getString(R.string.lib_untitled)
        val book = Store.addBook(this, name, uri.toString())
        reloadLibrary()
        Toast.makeText(this, getString(R.string.lib_added, name), Toast.LENGTH_SHORT).show()
        // v6.7: bölümlere ayırmayı öner
        offerSplit(book, uri)
    }

    /** Arka planda bölüm tespiti yapıp kullanıcıya sorar. */
    private fun offerSplit(book: Store.Book, uri: android.net.Uri) {
        val progress = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.split_scanning)
            .setMessage(R.string.split_scanning_msg)
            .setCancelable(false)
            .show()

        Thread {
            val result = PdfSplitter.detect(this, uri)
            runOnUiThread {
                try { progress.dismiss() } catch (_: Exception) {}
                if (isFinishing || isDestroyed) return@runOnUiThread
                if (!result.ok || result.chapters.size < 2) {
                    // v6.8: sessizce geçme — kullanıcıya bildir ve elle bölme öner
                    MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.split_none_title)
                        .setMessage(result.message + "\n\n" + getString(R.string.split_manual_ask))
                        .setPositiveButton(R.string.split_manual_do) { _, _ ->
                            askManualSplit(book, uri)
                        }
                        .setNegativeButton(R.string.split_skip, null)
                        .show()
                    return@runOnUiThread
                }
                val preview = result.chapters.take(8).joinToString("\n") {
                    "• ${it.title}  (${it.pageCount} sayfa)"
                }
                val more = if (result.chapters.size > 8) {
                    "\n… ve ${result.chapters.size - 8} bölüm daha"
                } else ""
                MaterialAlertDialogBuilder(this)
                    .setTitle(getString(R.string.split_found_title, result.chapters.size))
                    .setMessage(result.message + "\n\n" + preview + more +
                        "\n\n" + getString(R.string.split_ask))
                    .setPositiveButton(R.string.split_do) { _, _ -> runSplit(book, uri) }
                    .setNeutralButton(R.string.split_opt_marker) { _, _ ->
                        openManualSplit(book, uri)
                    }
                    .setNegativeButton(R.string.split_skip, null)
                    .show()
            }
        }.start()
    }

    /** Bölme işlemini yürütür ve bölümleri kitaplığa ekler. */
    private fun runSplit(
        book: Store.Book,
        uri: android.net.Uri,
        equalParts: Int = 0,
        manualPoints: String? = null
    ) {
        val progress = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.split_working)
            .setMessage(R.string.split_working_msg)
            .setCancelable(false)
            .show()

        Thread {
            val result = PdfSplitter.split(
                this, uri, book.title,
                equalParts = equalParts, manualPoints = manualPoints
            )
            runOnUiThread {
                try { progress.dismiss() } catch (_: Exception) {}
                if (isFinishing || isDestroyed) return@runOnUiThread
                if (!result.ok) {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.split_fail_title)
                        .setMessage(result.message)
                        .setPositiveButton(R.string.ok, null)
                        .show()
                    return@runOnUiThread
                }
                // Bölümleri kitaplığa yaz
                result.files.forEachIndexed { i, file ->
                    val ch = result.chapters.getOrNull(i) ?: return@forEachIndexed
                    val fileUri = androidx.core.content.FileProvider.getUriForFile(
                        this, "$packageName.fileprovider", file
                    )
                    Store.addChapter(
                        this, book,
                        "${i + 1}. ${ch.title}",
                        fileUri.toString(),
                        i + 1,
                        ch.pageCount
                    )
                }
                reloadLibrary()
                Toast.makeText(
                    this,
                    getString(R.string.split_done, result.files.size),
                    Toast.LENGTH_LONG
                ).show()
            }
        }.start()
    }
    private var currentIndex = 0

    /**
     * v7.88 — Ekran geçmişi.
     *
     * Ekranlar `hide()/show()` ile yönetildiği için FragmentManager'ın
     * kendi geri yığını hiç dolmuyordu; geri tuşu doğrudan Activity'yi
     * kapatıyor ve uygulamadan çıkıyordu. Kendi yığınımızı tutuyoruz.
     *
     * Tavan 20: sonsuz büyümesin ve "geri geri geri" ile kullanıcı
     * ekranlar arasında kaybolmasın.
     */
    private val ekranGecmisi = ArrayDeque<Int>()

    /** Çıkmak için ikinci geri tuşunun beklendiği an (ms). */
    private var cikisIcinBeklenen = 0L

    /** Menü güncellenirken geri çağrının kendini tekrar tetiklemesini engeller. */
    private var suppressNavCallback = false

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
        // v8.2 · Öneri 6: açılış ekranı.
        //
        // installSplashScreen() super.onCreate()'ten ÖNCE çağrılmalı,
        // aksi halde pencere zaten oluşmuş olur ve splash teması
        // uygulanmaz. setTheme çağrısından da önce olması gerekiyor
        // çünkü kütüphane postSplashScreenTheme'i kendisi uyguluyor;
        // biz hemen sonrasında kendi seçili temamızı ezerek yazıyoruz.
        // Not: installSplashScreen bir uzantı işlevi (extension), sınıf
        // üyesi değil — `SplashScreen.installSplashScreen(this)` biçiminde
        // çağrılamıyor. Doğrusu içe aktarıp `this.installSplashScreen()`.
        val splash = runCatching { installSplashScreen() }.getOrNull()

        // Seçili temayı uygulamadan önce yükle
        setTheme(ThemeManager.styleFor(this))
        ThemeManager.applyAccent(this)
        // v8.3 · Öneri 10: Material You (açıksa duvar kâğıdı paleti)
        ThemeManager.dinamikRengiUygula(this)
        super.onCreate(savedInstanceState)

        // Splash'tan ana ekrana yumuşak devir: ikon yukarı kayıp solarak
        // çıkar. Varsayılan davranış ani kesme; bu, açılışı akıcı yapar.
        runCatching {
            splash?.setOnExitAnimationListener { saglayici ->
                // ══════════════════════════════════════════════════
                // 🔴 v9.2 ÇÖKME DÜZELTMESİ — açılışta NullPointerException
                // ══════════════════════════════════════════════════
                //
                // Kullanıcı bildirdi:
                //   java.lang.NullPointerException
                //     at M.g$b.b(SourceFile:9)
                //     at MainActivity.onCreate$lambda$13$lambda$12
                //     at M.e.onSplashScreenExit
                //
                // ── Sebep ──
                // `saglayici.iconView` Java'dan geliyor ve PLATFORM
                // TİPİ (`View!`). Kotlin bunu null-safe saymıyor, yani
                // `val ikon = saglayici.iconView` satırı derleme
                // hatası vermiyor ama çalışma anında null gelebiliyor.
                //
                // Android 12+ sistem splash'ında ikon görünümü bazı
                // durumlarda oluşturulmuyor:
                //   · Tema `windowSplashScreenAnimatedIcon` çözemezse
                //   · Cihaz üreticisi splash'ı özelleştirmişse
                //   · Uygulama çok hızlı açılıp splash atlanırsa
                //
                // ── Neden runCatching kurtarmadı ──
                // Dıştaki `runCatching` yalnızca `setOnExitAnimationListener`
                // KAYIT çağrısını sarıyordu. Lambda daha SONRA, sistem
                // tarafından çağrılıyor — o an runCatching bloğu çoktan
                // bitmiş oluyor. Kendi try'ı olmalıydı.
                //
                // ── Neden ben yakalayamadım ──
                // Sandbox'ta emülatör yok; splash yalnız gerçek cihazda
                // çalışıyor. v8.2'den beri her sürümde bu satır vardı.
                // Bu benim hatamdı.
                try {
                    val ikon: android.view.View? = saglayici.iconView
                    if (ikon == null || !GorunumAyar.animasyonAcik(this)) {
                        saglayici.remove()
                        return@setOnExitAnimationListener
                    }
                    ikon.animate()
                        .alpha(0f)
                        .scaleX(0.72f)
                        .scaleY(0.72f)
                        .translationY(-ikon.height * 0.22f)
                        .setDuration(280)
                        .setInterpolator(android.view.animation.AccelerateInterpolator(1.4f))
                        .withEndAction {
                            // remove() de patlayabilir (görünüm zaten
                            // kaldırılmışsa); sarmalı olmalı
                            runCatching { saglayici.remove() }
                        }
                        .start()
                } catch (e: Throwable) {
                    // Ne olursa olsun splash KALKMALI. Kaldırılmazsa
                    // kullanıcı donmuş bir açılış ekranıyla kalır —
                    // çökmekten bile kötü.
                    android.util.Log.w("MainActivity", "Splash çıkış animasyonu", e)
                    runCatching { saglayici.remove() }
                }
            }
        }.onFailure { android.util.Log.w("MainActivity", "Splash çıkışı", it) }
        // v10.1 · Öneri 15: kenardan kenara temel katmanı.
        // setContentView'dan önce kurulur; kök view'daki
        // fitsSystemWindows="true" sayesinde yerleşim geometrisi korunur.
        KenardanKenara.uygula(this)

        // v7.61: Ağır açılış işleri ekran çizildikten SONRA yapılır.
        // Eskiden veri kurtarma kontrolü (diskten kalıcı yedek okuma),
        // otomatik yedek ve alarm yeniden kurulumu setContentView'dan
        // ÖNCE ana iş parçacığında çalışıyordu — uygulama açılırken
        // gözle görülür şekilde takılıyordu.
        setContentView(R.layout.activity_main)

        drawer = findViewById(R.id.drawerLayout)
        setupLibrary()
        // v11.11: Canva Çalışma Ekranı (10 Uygulama)
        findViewById<android.view.View>(R.id.drawerCanvaBtn)?.setOnClickListener {
            drawer.closeDrawer(androidx.core.view.GravityCompat.START)
            CanvaCalismaAtolyeActivity.ac(this)
        }
        // v11.04: Kişisel Gelişim ve Farkındalık Merkezi
        findViewById<android.view.View>(R.id.drawerKisiselGelisimBtn)?.setOnClickListener {
            drawer.closeDrawer(androidx.core.view.GravityCompat.START)
            KisiselGelisimActivity.ac(this)
        }
        // v10.93: YouTube Çevrimdışı Oynatma Listesi Sıralayıcı ve Video Kitaplığı
        findViewById<android.view.View>(R.id.drawerYoutubePlaylistBtn)?.setOnClickListener {
            drawer.closeDrawer(androidx.core.view.GravityCompat.START)
            YoutubePlaylistActivity.ac(this)
        }
        // v7.4: yan panelden mühendislik kursları ekranı
        findViewById<android.view.View>(R.id.drawerCoursesBtn).setOnClickListener {
            drawer.closeDrawer(androidx.core.view.GravityCompat.START)
            open(13)
        }
        // v7.20: Kaynak Merkezi
        findViewById<android.view.View>(R.id.drawerKaynakBtn).setOnClickListener {
            drawer.closeDrawer(androidx.core.view.GravityCompat.START)
            open(14)
        }
        // v7.30: Mühendislik araçları
        findViewById<android.view.View>(R.id.drawerAraclarBtn).setOnClickListener {
            drawer.closeDrawer(androidx.core.view.GravityCompat.START)
            open(15)
        }
        // v7.55: Sayaç alt menüden ⋮ paneline taşındı
        findViewById<android.view.View>(R.id.drawerTimerBtn).setOnClickListener {
            drawer.closeDrawer(androidx.core.view.GravityCompat.START)
            open(4)
        }
        // v7.73: global arama
        findViewById<android.view.View>(R.id.drawerAramaBtn).setOnClickListener {
            drawer.closeDrawer(androidx.core.view.GravityCompat.START)
            AramaActivity.ac(this)
        }
        // v7.59: kayıtlı AI sohbetleri — yan panelden erişim
        findViewById<android.view.View>(R.id.drawerSohbetBtn).setOnClickListener {
            drawer.closeDrawer(androidx.core.view.GravityCompat.START)
            SohbetGecmisiActivity.ac(this)
        }
        // v7.49: yapay zekâ düğmesi geri geldi
        findViewById<android.widget.TextView>(R.id.aiButton).apply {
            setOnClickListener { openAsistan() }
            setOnLongClickListener { hizliSor(); true }
        }
        // v7.49: günlük dizi/film önerisi
        findViewById<android.widget.TextView>(R.id.filmButton).setOnClickListener {
            FilmActivity.ac(this)
        }
        // v7.51: iki kişilik online paylaşım
        findViewById<android.widget.TextView>(R.id.onlineButton).setOnClickListener {
            OnlineActivity.ac(this)
        }
        // v7.48: üç noktanın yanındaki namaz düğmesi
        // (yapay zekâ Ana Sayfa ve Bugün ekranından erişilebilir durumda)
        findViewById<android.widget.TextView>(R.id.namazButton).apply {
            setOnClickListener {
                if (NamazVakti.acikMi(this@MainActivity)) {
                    NamazActivity.ac(this@MainActivity)
                } else {
                    // Modül kapalıysa doğrudan ayarlara götür
                    NamazAyarActivity.ac(this@MainActivity)
                }
            }
            // Uzun bas → tüm Diyanet ayarları
            setOnLongClickListener {
                NamazAyarActivity.ac(this@MainActivity)
                true
            }
        }
        // v10.6 · D39: ⌘ komut paleti — her köşeye tek kutudan atlama
        findViewById<android.widget.TextView>(R.id.komutButton).setOnClickListener {
            komutPaletiPenceresi()
        }
        // Sıradaki vakit rozeti — dokununca plan ekranı
        findViewById<android.widget.TextView>(R.id.namazVakit).setOnClickListener {            NamazActivity.ac(this)
        }
        // v10.51 #7: Taşma (overflow) menüsü (⋮)
        findViewById<View>(R.id.overflowMenuButton)?.setOnClickListener { view ->
            Titresim.dokunus(view)
            val secenekler = arrayOf(
                "🌱 Kişisel Gelişim ve Farkındalık",
                "🎨 Canva Çalışma Ekranı (10 Uygulama)",
                "🦴 Kas Sistemi 3D",
                "🎬 Günlük Film Önerisi",
                "👥 İki Kişilik Paylaşım",
                "🕌 Namaz Vakitleri & Planı",
                "⌘ Komut Paleti",
                "🎛️ Manuel Kontrol Merkezi",
                "🤖 AI Otopilot & Ajan"
            )
            MaterialAlertDialogBuilder(this)
                .setTitle("Araçlar & Modüller")
                .setItems(secenekler) { _, idx ->
                    when (idx) {
                        0 -> KisiselGelisimActivity.ac(this)
                        1 -> CanvaCalismaAtolyeActivity.ac(this)
                        2 -> FitnessActivity.ac(this)
                        3 -> FilmActivity.ac(this)
                        4 -> OnlineActivity.ac(this)
                        5 -> if (NamazVakti.acikMi(this)) NamazActivity.ac(this) else NamazAyarActivity.ac(this)
                        6 -> komutPaletiPenceresi()
                        7 -> ManuelKontrolActivity.ac(this)
                        8 -> OtonomMerkezActivity.ac(this)
                    }
                }
                .show()
        }
        findViewById<android.view.View>(R.id.menuButton).setOnClickListener {
            if (drawer.isDrawerOpen(androidx.core.view.GravityCompat.START)) {
                drawer.closeDrawer(androidx.core.view.GravityCompat.START)
            } else {
                reloadLibrary()
                drawer.openDrawer(androidx.core.view.GravityCompat.START)
            }
        }
        // v10.87: Detaylı Analiz butonu 3 noktanın (⋮) hemen yanında
        findViewById<android.view.View>(R.id.btnTopBarAnaliz)?.setOnClickListener {
            AnalitikActivity.ac(this)
        }

        bottomNav = findViewById(R.id.bottomNav)
        val fab = findViewById<FloatingActionButton>(R.id.fab)

        bottomNav.setOnItemSelectedListener { item ->
            if (!suppressNavCallback) {
                // v8.2 · Öneri 2: sekme değişiminde hafif dokunuş
                Titresim.dokunus(bottomNav)
                when (item.itemId) {
                    R.id.nav_home -> open(0)
                    R.id.nav_today -> open(2)
                    R.id.nav_topics -> open(3)
                    R.id.nav_plan -> open(16)
                    // v11.37: Sayaç alt bardan kaldırıldı, Hızlı Ekle panelinde.
                }
            }
            true
        }

        // Ortadaki büyük + butonu: ne eklemek istediğini sorar.
        fab.setOnClickListener {
            Titresim.dokunus(it)
            when (GorunumAyar.fabIslev(this)) {
                1 -> {
                    TimerEngine.setTotalMs(this, 25 * 60_000L)
                    TimerEngine.start(this)
                    TimerNotifier.show(this)
                    openTimer()
                }
                2 -> hizliKomutPenceresi()
                3 -> OtonomMerkezActivity.ac(this)
                // v11.35: varsayılan → HabitGenius tarzı Hızlı Ekle alt paneli
                else -> habitusHizliEkle()
            }
        }
        // v9.5 · Öneri 29: FAB'a uzun basınca tek satır hızlı komut.
        //
        // Normal akış altı dokunuş: FAB → tür seç → başlık → tarih →
        // saat → kaydet. Uzun basma tek kutuya indiriyor:
        // "gorev: rapor yaz cuma 17:00"
        fab.setOnLongClickListener {
            Titresim.uzunBasma(it)
            hizliKomutPenceresi()
            true
        }

        if (savedInstanceState == null) {
            // Widget'tan veya kısayoldan belirli bir ekran istendiyse oraya aç
            val requested = requestedScreen(intent)
            val varsayilan = when (GorunumAyar.acilisEkran(this)) {
                1 -> 6  // Görevler
                2 -> 4  // Sayaç
                3 -> 9  // Asistan
                4 -> 2  // Bugün / Günün Akışı (TodayFragment)
                5 -> 16 // Vakit Planı (PlanFragment)
                6 -> 1  // İlerleme (ProgressFragment)
                else -> 0 // Ana Ekran
            }
            open(if (requested >= 0) requested else varsayilan)
            handleQuickAction(intent)
        }
        // v7.57: online arka plan kontrolü açıksa alarmı taze tut
        try {
            OnlineBekci.kanalKur(this)
            OnlineBekci.kur(this)
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "Online bekçi kurulamadı", e)
        }
        // v7.61: alarmlar arka planda yeniden kurulur (ilk kareyi bloklamaz)
        Performans.arkaPlan {
            try {
                AlarmScheduler.rescheduleAll(this)
                AlarmScheduler.scheduleWeeklyReport(this)
                CourseReminderReceiver.schedule(this)
            } catch (e: Exception) {
                android.util.Log.w("MainActivity", "Alarmlar kurulamadı", e)
            }
        }
        // v7.61: veri kurtarma kontrolü ekran çizildikten sonra
        drawer.post {
            veriKurtarmaKontrolu()
            Store.maybeAutoBackup(this)
        }
        // v7.63: Android 13+ bildirim izni bir kez istenir.
        // Bu izin olmadan NamazBildirim dahil HICBIR bildirim gorunmuyordu.
        drawer.post { BildirimTani.acilistaIzinIste(this) }
        showCrashReportIfNeeded()
        // v7.3: kısayolları kod ile kaydet (statik XML bazı başlatıcılarda görünmüyordu)
        Shortcuts.install(this)
    }

    /** Widget'a dokunulduğunda uygulama zaten açıksa doğru ekrana geç. */
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        val requested = requestedScreen(intent)
        if (requested >= 0) open(requested)
        handleQuickAction(intent)
    }

    /**
     * İstenen ekran indeksini okur.
     *
     * v7.3 düzeltmesi: `res/xml/shortcuts.xml` içindeki `android:value="4"`
     * sisteme **String** olarak geçiyordu, bu yüzden `getIntExtra` her zaman
     * -1 döndürüp kısayol Ana Sayfa'yı açıyordu. Artık her iki tip de okunur.
     */
    private fun requestedScreen(intent: android.content.Intent?): Int {
        if (intent == null) return -1
        val asInt = intent.getIntExtra(WidgetCommon.EXTRA_OPEN_SCREEN, -1)
        if (asInt >= 0) return asInt
        val asText = intent.getStringExtra(WidgetCommon.EXTRA_OPEN_SCREEN)
        return asText?.trim()?.toIntOrNull() ?: -1
    }

    /** Widget kısayolundan gelen doğrudan eylemi uygular (soru/görev ekleme). */
    private fun handleQuickAction(intent: android.content.Intent?) {
        when (intent?.getStringExtra(WidgetCommon.EXTRA_QUICK_ACTION)) {
            WidgetCommon.QUICK_QUESTION -> {
                intent.removeExtra(WidgetCommon.EXTRA_QUICK_ACTION)
                bottomNav.post { showQuestionsQuickAdd() }
            }
            WidgetCommon.QUICK_TASK -> {
                intent.removeExtra(WidgetCommon.EXTRA_QUICK_ACTION)
                bottomNav.post { openTasksWithEditor() }
            }
        }
    }

    private fun createFragment(index: Int): Fragment = when (index) {
        0 -> HomeFragment()
        1 -> ProgressFragment()
        2 -> TodayFragment()
        3 -> TopicsFragment()
        4 -> TimerFragment()
        5 -> NotesFragment()
        6 -> TasksFragment()
        7 -> SettingsFragment()
        8 -> ThemeFragment()
        10 -> ExamsFragment()
        11 -> EventsFragment()
        12 -> HabitsFragment()
        13 -> CoursesFragment()
        14 -> KaynaklarFragment()
        15 -> AraclarFragment()
        // v7.55: Plan sekmesi — alt menüde Sayac'in yerini aldi
        16 -> PlanFragment()
        else -> AsistanFragment()
    }

    private fun navItemFor(index: Int): Int? = when (index) {
        0 -> R.id.nav_home
        2 -> R.id.nav_today
        3 -> R.id.nav_topics
        16 -> R.id.nav_plan
        // v11.37: Sayaç (4) alt barda değil → null döner (Hızlı Ekle'de).
        else -> null
    }

    /**
     * v9.8 · Öneri 50 — ekran indeksi → analitik anahtarı.
     *
     * ⚠️ Bu fonksiyon zaten var olan `ekranAdi`'ndan AYRI ve bu
     * bilinçli. `ekranAdi` `getString()` ile ÇEVRİLMİŞ ad
     * döndürüyor; başlık göstermek için doğru ama analitik
     * anahtarı olarak yanlış olurdu:
     *
     *   · Kullanıcı dili değiştirirse "Görevler" ve "Tasks" iki
     *     ayrı sayaç olur, geçmiş bölünür
     *   · Bir çeviri metni düzeltilirse eski sayaç yetim kalır
     *
     * Bu yüzden analitik SABİT Türkçe anahtar kullanıyor.
     * Sınıf adı da kullanmıyorum: R8 onları karıştırıyor.
     */
    private fun analitikEkranAdi(index: Int): String = when (index) {
        0 -> Kullanim.Ekran.ANA
        1 -> Kullanim.Ekran.ILERLEME
        2 -> Kullanim.Ekran.BUGUN
        3 -> Kullanim.Ekran.KONULAR
        4 -> Kullanim.Ekran.SAYAC
        5 -> Kullanim.Ekran.NOTLAR
        6 -> Kullanim.Ekran.GOREVLER
        7 -> Kullanim.Ekran.AYARLAR
        13 -> Kullanim.Ekran.KURSLAR
        14 -> Kullanim.Ekran.KAYNAKLAR
        15 -> Kullanim.Ekran.ARACLAR
        16 -> Kullanim.Ekran.PLAN
        9 -> Kullanim.Ekran.ASISTAN
        else -> ""
    }

    /** Bir ekranı açar (zaten oluşturulduysa gösterir). */
    fun open(index: Int) {
        // v7.88: aynı ekrana tekrar geçilmiyorsa öncekini geçmişe yaz
        if (currentIndex != index) {
            ekranGecmisi.addLast(currentIndex)
            while (ekranGecmisi.size > 20) ekranGecmisi.removeFirst()
        }
        // v9.8 · Öneri 50: kullanım sayacı.
        //
        // Tek nokta: her fragment'e ayrı ayrı `Kullanim.ekran`
        // eklemek 17 dosyayı değiştirmek ve birini unutmak
        // demekti. `open` zaten tüm geçişlerin geçtiği yer.
        // Maliyet: bir HashMap güncellemesi (mikrosaniye).
        runCatching {
            val ad = analitikEkranAdi(index)
            if (ad.isNotEmpty()) Kullanim.ekran(this, ad)
        }
        val oncekiIndex = currentIndex
        currentIndex = index
        val tag = "scr_$index"
        val fm = supportFragmentManager
        val tx = fm.beginTransaction()

        // v8.2 · Öneri 1: ekran geçiş animasyonu.
        //
        // Ekranlar hide()/show() ile yönetiliyor (add/replace değil), bu
        // yüzden setCustomAnimations show/hide'a da uygulanır. Yön,
        // ekran indeksinin artıp azalmasına göre seçiliyor: ileri
        // gidiyorsa sağdan, geri geliyorsa soldan.
        if (oncekiIndex != index) {
            Canlandir.fragmentGecisi(tx, this, ileri = index >= oncekiIndex)
        }

        fm.fragments
            .filter { it.tag?.startsWith("scr_") == true }
            .forEach { tx.hide(it) }

        var fragment = fm.findFragmentByTag(tag)
        if (fragment == null) {
            fragment = createFragment(index)
            tx.add(R.id.container, fragment, tag)
        } else {
            tx.show(fragment)
        }
        // commitNow() animasyonla birlikte kullanılabilir; işlem
        // eşzamanlı uygulanır, animasyon ardından oynar.
        tx.commitNow()

        // v11.09: Sekmeler arası taşıma işleminin ANINDA GÖRÜNMESİ için:
        SekmeVeVeriTasimaMotoru.aktifSekmeTasinanlariGuncelle(this, index, fragment)

        // v7.62 KRITIK DUZELTME:
        // Ekranlar hide()/show() ile yonetiliyor. v7.58'de her fragment'in
        // kok gorunumu bir SwipeRefreshLayout icine alindi; hide() yalnizca
        // fragment gorunumunu GONE yapiyor, SARMALAYICI gorunur kaliyordu.
        // Ust uste binen bu seffaf katmanlar acik ekranin dokunuslarini
        // yutuyordu ve hicbir dugme calismiyordu. Her gecin sonunda tum
        // ekranlarin sarmalayici gorunurlugunu fragment durumuyla esitle.
        fm.fragments
            .filter { it.tag?.startsWith("scr_") == true }
            .forEach { Yenileyici.gorunurluguEsitle(it) }

        navItemFor(index)?.let { itemId ->
            if (bottomNav.selectedItemId != itemId) {
                suppressNavCallback = true
                bottomNav.selectedItemId = itemId
                suppressNavCallback = false
            }
        }

        // v8.3 · Öneri 15: alt menü rozetleri (bekleyen iş sayıları).
        // Ekran değişince veri de değişmiş olabilir; zorla tazele.
        Rozet.tazele(bottomNav, this, zorla = true)
    }

    // Ekranlar arası geçiş yardımcıları:
    fun openTopics() = open(3)
    fun openTimer() = open(4)
    fun openNotes() = open(5)
    fun openTasks() = open(6)
    fun openSettings() = open(7)
    fun openThemes() = open(8)
    fun openAsistan() = open(9)
    fun openExams() = open(10)
    fun openEvents() = open(11)
    fun openToday() = open(2)

    /** v7.20: Kaynak Merkezi — yapay zekâ ile bulunan PDF ve videolar. */
    fun openKaynaklar() = open(14)

    /** v7.30: Mühendislik araçları — hesaplar ve yönetmelik. */
    fun openAraclar() = open(15)

    /** v7.55: Vakit Planı sekmesi (alt menüde Sayaç'ın yerinde). */
    fun openPlan() = open(16)

    /** v7.20: belirli bir ders için kaynak aramaya git. */
    fun openKaynaklar(lessonId: Long) {
        val tag = "scr_14"
        val fm = supportFragmentManager
        // Var olan örneği kaldır ki ders bilgisi güncellensin
        fm.findFragmentByTag(tag)?.let { fm.beginTransaction().remove(it).commitNow() }
        currentIndex = 14
        val tx = fm.beginTransaction()
        fm.fragments.filter { it.tag?.startsWith("scr_") == true }.forEach { tx.hide(it) }
        tx.add(R.id.container, KaynaklarFragment.yeni(lessonId), tag)
        tx.commitNow()
    }
    fun openHabits() = open(12)

    /** Görevler ekranını açıp doğrudan yeni görev penceresini gösterir. */
    fun openTasksWithEditor() {
        open(6)
        supportFragmentManager.executePendingTransactions()
        (supportFragmentManager.findFragmentByTag("scr_6") as? TasksFragment)?.showTaskEditor()
    }

    /** Bugün ekranındaki kısayoldan çağrılır: hızlı soru ekleme. */
    fun quickAddQuestions() = showQuestionsQuickAdd()

    /** Ana ekran ve Bugün ekranı görünürse verilerini tazeler. */
    fun refreshHome() {
        (supportFragmentManager.findFragmentByTag("scr_0") as? HomeFragment)?.refreshData()
        (supportFragmentManager.findFragmentByTag("scr_2") as? TodayFragment)?.refresh()
        (supportFragmentManager.findFragmentByTag("scr_12") as? HabitsFragment)?.refresh()
    }

    /** + menüsünden: bugünkü çözülen soru sayısına hızlı ekleme. */
    private fun showQuestionsQuickAdd() {
        val picker = android.widget.NumberPicker(this).apply {
            minValue = 5
            maxValue = 500
            value = 50
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.q_dialog_title)
            .setView(picker)
            .setPositiveButton(R.string.add) { _, _ ->
                Store.addQuestions(this, picker.value)
                android.widget.Toast.makeText(
                    this,
                    getString(R.string.q_added, picker.value, Store.getTodayQuestions(this)),
                    android.widget.Toast.LENGTH_LONG
                ).show()
                refreshHome()
                WidgetCommon.refreshAll(this)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * v9.5 · Öneri 29 — Tek satır hızlı komut penceresi.
     *
     * Yazdıkça canlı önizleme gösteriyor: "Görev · yarın 14:00".
     * Kullanıcı ne ekleyeceğini kaydetmeden önce görüyor.
     */
    /**
     * v10.6 · D39 — ⌘K komut paleti penceresi.
     *
     * HizliKomut doğal dilde VERİ girer; bu palet uygulama içinde
     * GEZİNİR: 15 komuta titremeli arama, ilk 6 sonuç gösterilir.
     */
    private fun komutPaletiPenceresi() {
        val yg = resources.displayMetrics.density
        fun dp(v: Int) = (v * yg).toInt()

        val liste = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
        }
        var pencere: androidx.appcompat.app.AlertDialog? = null

        fun goster(sorgu: String) {
            liste.removeAllViews()
            val bulunanlar = KomutPaleti.sirala(sorgu).take(6)
            if (bulunanlar.isEmpty()) {
                liste.addView(android.widget.TextView(this).apply {
                    setText(R.string.kp_bos)
                    textSize = 13f
                    setPadding(dp(4), dp(12), 0, dp(4))
                })
                return
            }
            bulunanlar.forEach { komut ->
                val satir = android.widget.TextView(this).apply {
                    text = "${komut.emoji}  ${komut.baslik}"
                    textSize = 15f
                    setPadding(dp(10), dp(12), dp(10), dp(12))
                    val ta = obtainStyledAttributes(
                        intArrayOf(android.R.attr.selectableItemBackground)
                    )
                    foreground = ta.getDrawable(0)
                    ta.recycle()
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        komutCalistir(komut)
                        if (pencere?.isShowing == true) pencere?.dismiss()
                    }
                }
                liste.addView(satir)
            }
        }

        val giris = android.widget.EditText(this).apply {
            hint = getString(R.string.kp_giris)
            setSingleLine(true)
            textSize = 15f
            inputType = android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setPadding(dp(10), dp(12), dp(10), dp(12))
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
                override fun afterTextChanged(s: android.text.Editable?) {
                    goster(s?.toString() ?: "")
                }
            })
        }

        val kok = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(6))
            addView(giris)
            addView(liste)
        }

        goster("")
        pencere = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.kp_baslik)
            .setView(kok)
            .setNegativeButton(R.string.cancel, null)
            .show()
        // Kutu açılır açılmaz klavye gelsin — paletin tek amacı hız
        giris.post {
            giris.requestFocus()
            val ime = getSystemService(
                android.content.Context.INPUT_METHOD_SERVICE
            ) as android.view.inputmethod.InputMethodManager
            ime.showSoftInput(giris, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
    }

    /** Paleden seçilen komutu uygular. Çalışan sayacı bölmez. */
    private fun komutCalistir(k: KomutPaleti.PaletKomut) {
        try {
            when {
                k.sayaciDurdur -> {
                    TimerEngine.reset(this)
                    TimerAlarm.cancel(this)
                    TimerNotifier.cancel(this)
                }
                k.sayacDakika > 0 -> {
                    if (!TimerEngine.isRunning(this)) {
                        TimerEngine.setMode(this, TimerEngine.MODE_DOWN)
                        TimerEngine.setTotalMs(this, k.sayacDakika * 60_000L)
                        TimerEngine.start(this)
                        TimerAlarm.reschedule(this)
                        TimerNotifier.show(this)
                    }
                }
            }
            when {
                k.ekran != null -> open(k.ekran)
                k.sayacDakika > 0 || k.sayaciDurdur -> open(4)
            }
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "Komut çalıştırılamadı", e)
        }
    }

    private fun hizliKomutPenceresi() {
        val yg = resources.displayMetrics.density
        fun dp(v: Int) = (v * yg).toInt()

        val kok = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(dp(22), dp(14), dp(22), dp(4))
        }
        kok.addView(android.widget.TextView(this).apply {
            setText(R.string.hk_ipucu)
            textSize = 12.5f
            setPadding(0, 0, 0, dp(10))
        })

        val giris = com.google.android.material.textfield.TextInputEditText(this).apply {
            hint = getString(R.string.hk_giris)
            setSingleLine(true)
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
        }
        kok.addView(
            com.google.android.material.textfield.TextInputLayout(this).apply {
                addView(giris)
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
        )

        val onizleme = android.widget.TextView(this).apply {
            textSize = 12.5f
            setPadding(dp(4), dp(8), 0, 0)
            setTextColor(
                com.google.android.material.color.MaterialColors.getColor(
                    this, com.google.android.material.R.attr.colorPrimary, 0
                )
            )
        }
        kok.addView(onizleme)

        // Örnekler — dokununca kutuya yazılıyor
        HizliKomut.ornekler(this).forEach { ornek ->
            kok.addView(android.widget.TextView(this).apply {
                text = "• $ornek"
                textSize = 12f
                setPadding(dp(4), dp(6), 0, 0)
                alpha = 0.7f
                isClickable = true
                setOnClickListener { giris.setText(ornek) }
            })
        }

        // Canlı önizleme
        giris.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun afterTextChanged(s: android.text.Editable?) {
                val metin = s?.toString() ?: ""
                if (metin.isBlank()) { onizleme.text = ""; return }
                val c = HizliKomut.coz(this@MainActivity, metin)
                onizleme.text = if (!c.gecerli) {
                    getString(R.string.hk_gecersiz)
                } else if (c.zamanMs > 0) {
                    getString(
                        R.string.hk_onizleme_tarih,
                        HizliKomut.turAdi(this@MainActivity, c.tur),
                        NaturalDate.describe(c.zamanMs, c.saatVar, c.tekrarGun)
                    )
                } else {
                    getString(R.string.hk_onizleme, HizliKomut.turAdi(this@MainActivity, c.tur))
                }
            }
        })

        val pencere = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.hk_baslik)
            .setView(android.widget.ScrollView(this).apply { addView(kok) })
            .setPositiveButton(R.string.hk_ekle, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        pencere.setOnShowListener {
            pencere.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val metin = giris.text?.toString()?.trim() ?: ""
                val onay = HizliKomut.calistir(this, metin)
                if (onay != null) {
                    Titresim.dogru(this)
                    Toast.makeText(this, onay, Toast.LENGTH_LONG).show()
                    refreshHome()
                    pencere.dismiss()
                } else {
                    Titresim.ret(giris)
                    Toast.makeText(this, R.string.hk_gecersiz, Toast.LENGTH_SHORT).show()
                }
            }
        }
        pencere.show()
        giris.requestFocus()
    }

    /**
     * v11.46 — Geliştirilmiş "Hızlı Ekle" alt paneli.
     * Arama kutusu + gruplu + ikonlu + alt açıklamalı, kaydırılabilir.
     */
    private fun habitusHizliEkle() {
        val yogunluk = resources.displayMetrics.density
        fun dp(v: Int) = (v * yogunluk).toInt()

        val kok = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(28))
        }

        // Başlık
        kok.addView(
            android.widget.TextView(this).apply {
                text = getString(R.string.quickadd_title)
                textSize = 20f
                setTextColor(
                    com.google.android.material.color.MaterialColors.getColor(
                        this@MainActivity,
                        com.google.android.material.R.attr.colorPrimary,
                        0xFFB08968.toInt()
                    )
                )
                setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                setPadding(dp(8), dp(0), dp(8), dp(2))
            },
            android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
        )
        kok.addView(
            android.widget.TextView(this).apply {
                text = getString(R.string.quickadd_subtitle)
                textSize = 12f
                setTextColor(
                    com.google.android.material.color.MaterialColors.getColor(
                        this@MainActivity,
                        com.google.android.material.R.attr.colorOnSurfaceVariant,
                        0xFF8A7F6E.toInt()
                    )
                )
                setPadding(dp(8), dp(0), dp(8), dp(6))
            },
            android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
        )

        // Arama kutusu
        val arama = android.widget.EditText(this).apply {
            hint = getString(R.string.quickadd_search)
            setSingleLine(true)
            textSize = 14f
            setTextColor(
                com.google.android.material.color.MaterialColors.getColor(
                    this@MainActivity,
                    com.google.android.material.R.attr.colorOnSurface,
                    0xFF3A3226.toInt()
                )
            )
            setHintTextColor(
                com.google.android.material.color.MaterialColors.getColor(
                    this@MainActivity,
                    com.google.android.material.R.attr.colorOnSurfaceVariant,
                    0xFF8A7F6E.toInt()
                )
            )
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = null
            setBackgroundColor(0x22B08968)
        }
        kok.addView(arama, android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT))

        // İçerik alanı — arama sonucuna göre yeniden çizilir
        val liste = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
        }
        kok.addView(liste, android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT))

        // Sarmalayıcı: kaydırılabilir + yükseklik sınırlı.
        val sari = android.widget.ScrollView(this)
        sari.addView(kok)
        sari.layoutParams = android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            (resources.displayMetrics.heightPixels * 0.72f).toInt()
        )
        val panel = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        panel.setContentView(sari)

        // Bölüm başlığı
        fun bolumBasligi(metin: String): TextView = TextView(this).apply {
            text = metin.uppercase(java.util.Locale("tr", "TR"))
            textSize = 11.5f
            setTextColor(
                com.google.android.material.color.MaterialColors.getColor(
                    this@MainActivity,
                    com.google.android.material.R.attr.colorOnSurfaceVariant,
                    0xFF8A7F6E.toInt()
                )
            )
            setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            setPadding(dp(8), dp(12), dp(8), dp(4))
        }

        // Eylem tanımı
        data class Eylem(val grup: String, val metin: String, val alt: String, val calis: () -> Unit)

        val eylemler = listOf(
            Eylem(getString(R.string.quickadd_group_ekle), getString(R.string.quickadd_task), getString(R.string.quickadd_task_alt)) {
                panel.dismiss(); open(6); supportFragmentManager.executePendingTransactions()
                (supportFragmentManager.findFragmentByTag("scr_6") as? TasksFragment)?.showTaskEditor()
            },
            Eylem(getString(R.string.quickadd_group_ekle), getString(R.string.quickadd_habit), getString(R.string.quickadd_habit_alt)) {
                panel.dismiss(); open(12); supportFragmentManager.executePendingTransactions()
                (supportFragmentManager.findFragmentByTag("scr_12") as? HabitsFragment)?.showHabitEditor(null)
            },
            Eylem(getString(R.string.quickadd_group_ekle), getString(R.string.quickadd_note), getString(R.string.quickadd_note_alt)) {
                panel.dismiss(); open(5); supportFragmentManager.executePendingTransactions()
                (supportFragmentManager.findFragmentByTag("scr_5") as? NotesFragment)?.showNoteEditor(null)
            },
            Eylem(getString(R.string.quickadd_group_ekle), getString(R.string.quickadd_event), getString(R.string.quickadd_event_alt)) {
                panel.dismiss(); open(11); supportFragmentManager.executePendingTransactions()
                (supportFragmentManager.findFragmentByTag("scr_11") as? EventsFragment)?.showEventEditor(null)
            },
            Eylem(getString(R.string.quickadd_group_ekle), getString(R.string.quickadd_exam), getString(R.string.quickadd_exam_alt)) {
                panel.dismiss(); open(10); supportFragmentManager.executePendingTransactions()
                (supportFragmentManager.findFragmentByTag("scr_10") as? ExamsFragment)?.showExamEditor()
            },
            Eylem(getString(R.string.quickadd_group_ekle), getString(R.string.quickadd_topic), getString(R.string.quickadd_topic_alt)) {
                panel.dismiss(); open(3); (supportFragmentManager.findFragmentByTag("scr_3") as? TopicsFragment)?.showTopicDialog()
            },
            Eylem(getString(R.string.quickadd_group_ekle), getString(R.string.quickadd_questions), getString(R.string.quickadd_questions_alt)) {
                panel.dismiss(); showQuestionsQuickAdd()
            },
            Eylem(getString(R.string.quickadd_group_ekle), getString(R.string.quickadd_plan), getString(R.string.quickadd_plan_alt)) {
                panel.dismiss(); open(16)
            },
            Eylem(getString(R.string.quickadd_group_arac), getString(R.string.quickadd_saglik), getString(R.string.quickadd_saglik_alt)) {
                panel.dismiss(); SaglikOzetActivity.ac(this)
            },
            Eylem(getString(R.string.quickadd_group_arac), getString(R.string.quickadd_mood), getString(R.string.quickadd_mood_alt)) {
                panel.dismiss(); MoodActivity.ac(this)
            },
            Eylem(getString(R.string.quickadd_group_arac), getString(R.string.quickadd_stats), getString(R.string.quickadd_stats_alt)) {
                panel.dismiss(); AnalitikActivity.ac(this)
            },
            Eylem(getString(R.string.quickadd_group_arac), getString(R.string.quickadd_timer), getString(R.string.quickadd_timer_alt)) {
                panel.dismiss(); openTimer()
            },
            Eylem(getString(R.string.quickadd_group_arac), getString(R.string.quickadd_butce), getString(R.string.quickadd_butce_alt)) {
                panel.dismiss(); TakipActivity.ac(this, TakipActivity.S_BUTCE)
            },
            Eylem(getString(R.string.quickadd_group_arac), getString(R.string.quickadd_fitness), getString(R.string.quickadd_fitness_alt)) {
                panel.dismiss(); FitnessActivity.ac(this)
            },
            Eylem(getString(R.string.quickadd_group_arac), getString(R.string.quickadd_beslenme), getString(R.string.quickadd_beslenme_alt)) {
                panel.dismiss(); BeslenmeActivity.ac(this)
            },
            Eylem(getString(R.string.quickadd_group_arac), getString(R.string.quickadd_uyku), getString(R.string.quickadd_uyku_alt)) {
                panel.dismiss(); UykuActivity.ac(this)
            }
        )

        // Eylem satırı
        fun satirEkle(e: Eylem) {
            val satir = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                setPadding(dp(8), dp(12), dp(8), dp(12))
                gravity = android.view.Gravity.CENTER_VERTICAL
                isClickable = true
                isFocusable = true
                val tip = android.util.TypedValue()
                this@MainActivity.theme.resolveAttribute(
                    android.R.attr.selectableItemBackground, tip, true
                )
                setBackgroundResource(tip.resourceId)
                setOnClickListener { e.calis() }
            }
            val metinKol = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            metinKol.addView(android.widget.TextView(this).apply {
                text = e.metin
                textSize = 15f
                setTextColor(
                    com.google.android.material.color.MaterialColors.getColor(
                        this@MainActivity,
                        com.google.android.material.R.attr.colorOnSurface,
                        0xFF3A3226.toInt()
                    )
                )
            })
            if (e.alt.isNotBlank()) {
                metinKol.addView(android.widget.TextView(this).apply {
                    text = e.alt
                    textSize = 11.5f
                    setTextColor(
                        com.google.android.material.color.MaterialColors.getColor(
                            this@MainActivity,
                            com.google.android.material.R.attr.colorOnSurfaceVariant,
                            0xFF8A7F6E.toInt()
                        )
                    )
                })
            }
            satir.addView(metinKol)
            satir.addView(android.widget.TextView(this).apply {
                text = "›"
                textSize = 22f
                setTextColor(
                    com.google.android.material.color.MaterialColors.getColor(
                        this@MainActivity,
                        com.google.android.material.R.attr.colorPrimary,
                        0xFFB08968.toInt()
                    )
                )
            })
            liste.addView(satir, android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT))
        }

        // Listeyi (arama filtreli) çiz
        fun ciz(sorgu: String) {
            liste.removeAllViews()
            val q = sorgu.trim().lowercase(java.util.Locale("tr", "TR"))
            val sonuc = if (q.isEmpty()) eylemler
            else eylemler.filter { e ->
                e.metin.lowercase(java.util.Locale("tr", "TR")).contains(q) ||
                    e.alt.lowercase(java.util.Locale("tr", "TR")).contains(q) ||
                    e.grup.lowercase(java.util.Locale("tr", "TR")).contains(q)
            }
            if (sonuc.isEmpty()) {
                liste.addView(android.widget.TextView(this).apply {
                    text = getString(R.string.quickadd_noresult)
                    textSize = 13f
                    setTextColor(
                        com.google.android.material.color.MaterialColors.getColor(
                            this@MainActivity,
                            com.google.android.material.R.attr.colorOnSurfaceVariant,
                            0xFF8A7F6E.toInt()
                        )
                    )
                    setPadding(dp(8), dp(20), dp(8), dp(8))
                })
                return
            }
            var sonGrup: String? = null
            sonuc.forEach { e ->
                if (e.grup != sonGrup) {
                    sonGrup = e.grup
                    liste.addView(bolumBasligi(e.grup))
                }
                satirEkle(e)
            }
        }

        // Arama dinleyicisi
        arama.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun afterTextChanged(s: android.text.Editable?) {
                ciz(s?.toString() ?: "")
            }
        })

        ciz("")
        panel.show()
    }

    private fun showAddChooser() {
        val options = arrayOf(
            getString(R.string.add_opt_topic),
            getString(R.string.add_opt_task),
            getString(R.string.add_opt_note),
            getString(R.string.add_opt_questions),
            getString(R.string.add_opt_exam),
            getString(R.string.add_opt_event),
            getString(R.string.add_opt_habit)
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_sheet_title)
            .setItems(options) { _, which ->
                when (which) {
                    3 -> {
                        showQuestionsQuickAdd()
                        return@setItems
                    }
                    4 -> {
                        open(10)
                        supportFragmentManager.executePendingTransactions()
                        (supportFragmentManager.findFragmentByTag("scr_10") as? ExamsFragment)
                            ?.showExamEditor()
                        return@setItems
                    }
                    6 -> {
                        open(12)
                        supportFragmentManager.executePendingTransactions()
                        (supportFragmentManager.findFragmentByTag("scr_12") as? HabitsFragment)
                            ?.showHabitEditor(null)
                        return@setItems
                    }
                    5 -> {
                        open(11)
                        supportFragmentManager.executePendingTransactions()
                        (supportFragmentManager.findFragmentByTag("scr_11") as? EventsFragment)
                            ?.showEventEditor(null)
                        return@setItems
                    }
                    0 -> {
                        open(3)
                        (supportFragmentManager.findFragmentByTag("scr_3") as? TopicsFragment)
                            ?.showTopicDialog()
                    }
                    1 -> {
                        open(6)
                        (supportFragmentManager.findFragmentByTag("scr_6") as? TasksFragment)
                            ?.showTaskEditor()
                    }
                    else -> {
                        open(5)
                        (supportFragmentManager.findFragmentByTag("scr_5") as? NotesFragment)
                            ?.showNoteEditor(null)
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Uygulama bir önceki oturumda çöktüyse kullanıcıya sorar.
     *
     * ══════════════════════════════════════════════════════════
     * v9.8 · Öneri 49 — YENİLENDİ
     * ══════════════════════════════════════════════════════════
     * Eski hâli her açılışta ham yığın izini pencereye basıyordu.
     * Üç sorunu vardı:
     *
     *   1. Kullanıcı 2500 karakterlik teknik metni görüp korkuyordu
     *   2. "Kapat" deyince kayıt SİLİNİYORDU — sonradan bakılamıyordu
     *   3. Tekrar eden hatalar görünmüyordu (tek kayıt tutuluyordu)
     *
     * Yeni hâli:
     *   · Okunabilir özet gösteriyor (tür + mesaj + kaç kez)
     *   · Kayıt SİLİNMİYOR, "soruldu" işareti konuyor
     *   · Detay ve paylaşım için Sistem ekranına yönlendiriyor
     *   · "Bir daha sorma" seçeneği var
     */
    private fun showCrashReportIfNeeded() {
        if (!runCatching { CokmeRapor.sormaliMi(this) }.getOrDefault(false)) return
        val ozet = runCatching { CokmeRapor.ozet(this) }.getOrDefault("")
        if (ozet.isBlank()) return

        // Sorulduğunu hemen işaretle: kullanıcı pencereyi kaydırıp
        // kapatırsa da bir daha aynı çökme için sorulmasın.
        runCatching { CokmeRapor.soruldu(this) }

        runCatching {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.cr_baslik)
                .setMessage(getString(R.string.cr_mesaj, ozet))
                .setPositiveButton(R.string.cr_gonder) { _, _ ->
                    runCatching {
                        // NOT: bu dosya `android.content.Intent` import
                        // ETMİYOR (yalnızca ClipData/ClipboardManager var).
                        // Tam nitelikli ad kullanıyorum — import eklemek
                        // 55 KB'lik dosyada başka bir `Intent` adıyla
                        // çakışma riski taşır.
                        startActivity(
                            android.content.Intent.createChooser(
                                android.content.Intent(
                                    android.content.Intent.ACTION_SEND
                                ).apply {
                                    type = "text/plain"
                                    putExtra(
                                        android.content.Intent.EXTRA_SUBJECT,
                                        "Günlük Asistan hata raporu"
                                    )
                                    putExtra(
                                        android.content.Intent.EXTRA_TEXT,
                                        CokmeRapor.rapor(this@MainActivity)
                                    )
                                },
                                getString(R.string.dp_paylas)
                            )
                        )
                    }
                }
                .setNeutralButton(R.string.cr_detay) { _, _ ->
                    SistemActivity.ac(this, SistemActivity.S_COKME)
                }
                .setNegativeButton(R.string.close, null)
                .show()
        }
    }
    // ---------------- Kitaplık (v6.6) ----------------

    private fun setupLibrary() {
        val recycler = findViewById<RecyclerView>(R.id.libRecycler)
        libAdapter = BookAdapter(
            books,
            onOpen = { openBook(it) },
            onDelete = { confirmDeleteBook(it) }
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = libAdapter

        findViewById<android.view.View>(R.id.libAddBtn).setOnClickListener {
            // PDF öncelikli, diğer belgeler de seçilebilsin
            pickBook.launch(arrayOf("application/pdf", "application/epub+zip", "*/*"))
        }
        reloadLibrary()
    }

    fun reloadLibrary() {
        books.clear()
        // Ana kitaplar, hemen altlarında kendi bölümleri
        Store.rootBooks(this).forEach { root ->
            books.add(root)
            books.addAll(Store.chaptersOf(this, root.id))
        }
        libAdapter?.notifyDataSetChanged()
        findViewById<android.widget.TextView>(R.id.libEmpty).visibility =
            if (books.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        findViewById<android.widget.TextView>(R.id.libCount).text =
            if (books.isEmpty()) "" else books.size.toString()
    }

    /** Kitabı sistemin PDF görüntüleyicisinde açar. */
    private fun openBook(book: Store.Book) {
        try {
            val uri = android.net.Uri.parse(book.uri)
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            drawer.closeDrawer(androidx.core.view.GravityCompat.START)
        } catch (_: Exception) {
            Toast.makeText(this, R.string.lib_open_error, Toast.LENGTH_LONG).show()
        }
    }

    private fun confirmDeleteBook(book: Store.Book) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.lib_delete_title)
            .setMessage(getString(R.string.lib_delete_msg, book.title))
            .setPositiveButton(R.string.delete) { _, _ ->
                Store.deleteBookUndoable(this, book.id)
                reloadLibrary()
                geriAlSun(getString(R.string.undo_book))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** İçerik adresinden görünen dosya adını okur. */
    private fun queryFileName(uri: android.net.Uri): String? {
        return try {
            contentResolver.query(uri, null, null, null, null)?.use { c ->
                val i = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (i >= 0 && c.moveToFirst()) {
                    c.getString(i)?.removeSuffix(".pdf")?.removeSuffix(".PDF")
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * v7.88 — Geri tuşu davranışı.
     *
     * ── Sorun ──
     * Ekranlar `hide()/show()` ile yönetildiği için FragmentManager'ın geri
     * yığını boştu; hangi ekranda olursa olsun geri tuşu doğrudan
     * uygulamadan çıkarıyordu.
     *
     * ── Sıra ──
     *   1. Yan panel açıksa kapat
     *   2. Ekran geçmişinde kayıt varsa bir öncekine dön
     *   3. Ana ekranda değilsek ana ekrana dön
     *   4. Ana ekrandaysak: ilk dokunuşta uyar, 2 saniye içinde
     *      ikinci dokunuşta çık
     */
    override fun onBackPressed() {
        // 1. Yan panel
        if (::drawer.isInitialized &&
            drawer.isDrawerOpen(androidx.core.view.GravityCompat.START)
        ) {
            drawer.closeDrawer(androidx.core.view.GravityCompat.START)
            return
        }

        // 2. Geçmişte bir önceki ekran
        if (ekranGecmisi.isNotEmpty()) {
            val onceki = ekranGecmisi.removeLast()
            if (onceki != currentIndex) {
                // open() yeniden geçmişe yazmasın diye doğrudan geçiş
                gecmissizAc(onceki)
                return
            }
        }

        // 3. Ana ekranda değilsek oraya dön
        if (currentIndex != 0) {
            gecmissizAc(0)
            return
        }

        // 4. Ana ekrandayız — çıkmak için çift dokunuş
        val simdi = System.currentTimeMillis()
        if (simdi - cikisIcinBeklenen < 2000L) {
            @Suppress("DEPRECATION")
            super.onBackPressed()
            return
        }
        cikisIcinBeklenen = simdi
        android.widget.Toast.makeText(
            this, R.string.cikis_icin_tekrar, android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Geçmişe kayıt eklemeden ekran açar.
     *
     * Geri tuşu için gerekli: normal [open] her çağrıda geçmişe yazdığı
     * için geri gitmek yeni bir geçmiş kaydı oluşturur ve kullanıcı
     * iki ekran arasında sonsuz döngüye girerdi.
     */
    private fun gecmissizAc(index: Int) {
        val yedek = ArrayDeque(ekranGecmisi)
        open(index)
        ekranGecmisi.clear()
        ekranGecmisi.addAll(yedek)
    }

    /** Kitaplık liste bağdaştırıcısı. */
    private class BookAdapter(
        private val items: List<Store.Book>,
        private val onOpen: (Store.Book) -> Unit,
        private val onDelete: (Store.Book) -> Unit
    ) : RecyclerView.Adapter<BookAdapter.Holder>() {

        class Holder(v: android.view.View) : RecyclerView.ViewHolder(v) {
            val spine: android.view.View = v.findViewById(R.id.bookSpine)
            val title: android.widget.TextView = v.findViewById(R.id.bookTitle)
            val sub: android.widget.TextView = v.findViewById(R.id.bookSub)
            val delete: android.widget.ImageView = v.findViewById(R.id.bookDelete)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): Holder {
            val v = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_book, parent, false)
            return Holder(v)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val book = items[position]
            val ctx = holder.itemView.context
            holder.title.text = book.title
            holder.spine.setBackgroundColor(
                ThemeManager.NEON_PALETTE[book.color % ThemeManager.NEON_PALETTE.size]
            )

            // Bölümler girintili ve daha küçük görünür
            val density = ctx.resources.displayMetrics.density
            val params = holder.itemView.layoutParams
            if (params is android.view.ViewGroup.MarginLayoutParams) {
                params.marginStart = if (book.isChapter) (22 * density).toInt() else 0
                params.topMargin = if (book.isChapter) (4 * density).toInt() else (8 * density).toInt()
                holder.itemView.layoutParams = params
            }
            holder.title.textSize = if (book.isChapter) 13f else 14f
            holder.itemView.alpha = if (book.isChapter) 0.92f else 1f

            val date = java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale("tr", "TR"))
                .format(java.util.Date(book.addedAt))
            holder.sub.text = when {
                book.isChapter && book.pages > 0 ->
                    ctx.getString(R.string.lib_chapter_pages, book.pages)
                book.chapterCount > 0 ->
                    ctx.getString(R.string.lib_chapters, book.chapterCount)
                book.progress > 0 -> ctx.getString(R.string.lib_progress, book.progress, date)
                else -> date
            }
            holder.itemView.setOnClickListener { onOpen(book) }
            holder.delete.setOnClickListener { onDelete(book) }
        }
    }

    /** Otomatik tespit başarısızsa: kullanıcıdan bölüm sayısı alıp eşit böler. */
    private fun askManualSplit(book: Store.Book, uri: android.net.Uri) {
        val options = arrayOf(
            getString(R.string.split_opt_marker),
            getString(R.string.split_opt_equal)
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.split_manual_title)
            .setItems(options) { _, which ->
                if (which == 0) openManualSplit(book, uri) else askEqualParts(book, uri)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** Elle bölme noktası seçme ekranını açar. */
    fun openManualSplit(book: Store.Book, uri: android.net.Uri) {
        pendingSplitBook = book
        pendingSplitUri = uri
        manualSplitLauncher.launch(
            android.content.Intent(this, ManualSplitActivity::class.java)
                .putExtra(ManualSplitActivity.EXTRA_URI, uri.toString())
                .putExtra(ManualSplitActivity.EXTRA_TITLE, book.title)
                .putExtra(ManualSplitActivity.EXTRA_BOOK_ID, book.id)
        )
    }

    private fun askEqualParts(book: Store.Book, uri: android.net.Uri) {
        val picker = android.widget.NumberPicker(this).apply {
            minValue = 2; maxValue = 40; value = 10
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.split_manual_title)
            .setMessage(R.string.split_manual_msg)
            .setView(picker)
            .setPositiveButton(R.string.split_do) { _, _ -> runSplit(book, uri, picker.value) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** v7.9: silme sonrası "Geri Al" şeridi gösterir. */
    /**
     * v7.18: Güncelleme sonrası veri kaybına karşı son güvenlik ağı.
     *
     * Uygulama bomboş açıldıysa ve İndirilenler klasöründe kalıcı yedek varsa,
     * kullanıcıya geri yükleme teklif eder. Reddedilirse bir daha sorulmaz.
     */
    private fun veriKurtarmaKontrolu() {
        // v7.61: hizli on kontroller ana is parcaciginda (SharedPreferences),
        // agir olan MediaStore okumasi arka planda.
        if (Store.kurtarmaSoruldu(this)) return
        if (!Store.veriBosMu(this)) return
        Performans.arkaPlan {
            val ham = Store.kaliciYedekOku(this)
            if (ham != null) {
                Performans.anaIs {
                    if (!isFinishing && !isDestroyed) kurtarmaSor(ham)
                }
            }
        }
    }

    /** v7.61: Kurtarma penceresi — veri arka planda okunduktan sonra. */
    private fun kurtarmaSor(yedek: String) {
        try {
            val ozet = Store.yedekOzeti(yedek) ?: return
            if (ozet.ders == 0 && ozet.gorev == 0 && ozet.not == 0) return

            val tarih = if (ozet.tarih > 0) {
                java.text.SimpleDateFormat("d MMMM yyyy HH:mm", java.util.Locale("tr", "TR"))
                    .format(java.util.Date(ozet.tarih))
            } else {
                getString(R.string.restore_no_date)
            }

            val govde = buildString {
                append(getString(R.string.recover_body, tarih))
                append("\n\n")
                append(getString(R.string.restore_row_lessons, ozet.bitenDers, ozet.ders))
                append("\n")
                append(getString(R.string.restore_row_notes, ozet.dersNotu))
                append("\n")
                append(getString(R.string.restore_row_tasks, ozet.gorev))
                if (ozet.seriRekor > 0) {
                    append("\n")
                    append(getString(R.string.restore_row_streak, ozet.seriRekor))
                }
            }

            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.recover_title)
                .setMessage(govde)
                .setCancelable(false)
                .setPositiveButton(R.string.recover_yes) { _, _ ->
                    Store.kurtarmaSoruldu(this, true)
                    val ok = Store.importJson(this, yedek)
                    Toast.makeText(
                        this,
                        if (ok) R.string.recover_ok else R.string.restore_fail,
                        Toast.LENGTH_LONG
                    ).show()
                    if (ok) recreate()
                }
                .setNegativeButton(R.string.recover_no) { _, _ ->
                    Store.kurtarmaSoruldu(this, true)
                }
                .show()
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "Veri kurtarma kontrolü başarısız", e)
        }
    }

    /**
     * v7.26: Ekran değiştirmeden yapay zekâya soru sorma penceresi.
     * Bulunduğun ekranın bağlamı da soruya eklenir.
     */
    fun hizliSor(onIstem: String = "") {
        if (!AiSettings.isReady(this)) {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.asistan_title)
                .setMessage(R.string.src_ai_needed)
                .setPositiveButton(R.string.ocr_open_settings) { _, _ -> openSettings() }
                .setNegativeButton(R.string.cancel, null)
                .show()
            return
        }

        val dp = resources.displayMetrics.density
        val giris = android.widget.EditText(this).apply {
            hint = getString(R.string.ai_quick_hint)
            setText(onIstem)
            setSelection(text?.length ?: 0)
            minLines = 2
            maxLines = 5
            val p = (16 * dp).toInt()
            setPadding(p, p, p, p)
        }
        val sarmal = android.widget.FrameLayout(this).apply { addView(giris) }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(R.string.ai_quick_title)
            .setView(sarmal)
            .setPositiveButton(R.string.ai_quick_send) { _, _ ->
                val soru = giris.text?.toString()?.trim().orEmpty()
                if (soru.isNotBlank()) hizliSorGonder(soru)
            }
            .setNeutralButton(R.string.ai_quick_full) { _, _ -> openAsistan() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** v7.26: Soruyu arka planda sorar, cevabı pencerede gösterir. */
    private fun hizliSorGonder(soru: String) {
        val bekleme = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(R.string.ai_quick_title)
            .setMessage(R.string.ai_quick_waiting)
            .setCancelable(false)
            .show()

        Thread {
            // Hangi ekranda olduğunu da bildir — bağlamlı cevap versin
            val ekran = ekranAdi(currentIndex)
            val tamSoru = if (ekran.isBlank()) soru
            else "[Kullanıcı şu an '" + ekran + "' ekranında] " + soru
            val sonuc = AiClient.chat(this, tamSoru)

            runOnUiThread {
                try {
                    bekleme.dismiss()
                } catch (_: Exception) {
                }
                if (!sonuc.ok) {
                    com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.ai_quick_title)
                        .setMessage(sonuc.text)
                        .setPositiveButton(R.string.done, null)
                        .show()
                    return@runOnUiThread
                }

                // v7.36: cevaptaki tüm komutlar sırayla, onay gerekenler sorularak
                val (metin, komutlar) = AsistanKomut.ayiklaHepsi(sonuc.text)

                fun sonucuGoster(bildirimler: List<String>) {
                    val govde = buildString {
                        append(metin)
                        bildirimler.filter { it.isNotBlank() }.forEach {
                            append("\n\n✓ ").append(it)
                        }
                        AiClient.sonGecisBilgisi?.let { append("\n\nℹ ").append(it) }
                    }
                    com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.ai_quick_title)
                        .setMessage(govde)
                        .setPositiveButton(R.string.done, null)
                        .setNeutralButton(R.string.ai_quick_full) { _, _ -> openAsistan() }
                        .show()
                    // Ekranı tazele — komutlar veri değiştirmiş olabilir
                    // Not: open(currentIndex) çağrılmaz — ekran_ac komutu
                    // kullanıcıyı başka sekmeye götürmüş olabilir, geri çekmeyelim.
                    reloadLibrary()
                }

                if (komutlar.isEmpty()) {
                    sonucuGoster(emptyList())
                } else {
                    AsistanKomut.calistirSirayla(this, komutlar) { sonucuGoster(it) }
                }
            }
        }.start()
    }

    /**
     * v7.48: Üst bardaki namaz rozetini günceller.
     * Modül kapalıysa gizlenir; açıksa "Akşam 19:58" biçiminde görünür.
     */
    fun namazRozetiniTazele() {
        val rozet = findViewById<android.widget.TextView>(R.id.namazVakit) ?: return
        if (!NamazVakti.acikMi(this)) {
            rozet.visibility = android.view.View.GONE
            return
        }
        try {
            val gun = NamazVakti.bugunDuzeltilmis(this)
            val (vakit, _) = gun.sonraki(NamazVakti.simdiDakika())
            rozet.text = getString(vakit.adRes) + " " + gun.saat(vakit)
            rozet.visibility = android.view.View.VISIBLE
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "Namaz rozeti çizilemedi", e)
            rozet.visibility = android.view.View.GONE
        }
    }

    /** v7.26: Ekran indeksinden okunabilir ad. */
    private fun ekranAdi(index: Int): String = when (index) {
        1 -> getString(R.string.nav_progress)
        2 -> getString(R.string.nav_today)
        3 -> getString(R.string.tab_topics)
        4 -> getString(R.string.tab_timer)
        5 -> getString(R.string.tab_notes)
        6 -> getString(R.string.tab_tasks)
        10 -> getString(R.string.exams_title)
        12 -> getString(R.string.habits_title)
        13 -> getString(R.string.drawer_courses)
        14 -> getString(R.string.src_center_title)
        15 -> getString(R.string.tools_title)
        16 -> getString(R.string.nav_plan)
        else -> ""
    }

    private fun geriAlSun(mesaj: String) {
        val kok = findViewById<android.view.View>(R.id.drawerLayout) ?: return
        com.google.android.material.snackbar.Snackbar
            .make(kok, mesaj, com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
            .setAction(R.string.undo_action) {
                if (Store.geriAl()) {
                    reloadLibrary()
                    Toast.makeText(this, R.string.undo_ok, Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }


    /**
     * v7.41: Uygulama arka plana alınınca widget'ları tazele.
     *
     * Tek tek save* çağrıları artık tazeliyor ama bazı ekranlar veriyi
     * doğrudan SharedPreferences'a yazıyor olabilir. Bu, her durumda
     * ana ekrana dönüldüğünde widget'ın güncel olmasını garantiler.
     */
    override fun onResume() {
        super.onResume()
        // v10.11 · ULTRA-30 A1: Güneş modunda uygulama açıkken de
        // doğuş/batış geçişi yakalansın — uygulamaya her dönüşte
        // kip yeniden hesaplanır (değişiklik yoksa no-op).
        try {
            if (ThemeManager.geceModu(this) == ThemeManager.GECE_GUNES) {
                ThemeManager.geceModunuUygula(this)
            }
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "Güneş kipi tazelenemedi", e)
        }
        // v7.48: vakit ilerledikçe rozet güncel kalsın
        namazRozetiniTazele()
        // v8.3 · Öneri 15: uygulamaya dönünce sayılar taze olsun
        // (bildirimden görev tamamlanmış olabilir)
        Rozet.tazele(bottomNav, this, zorla = true)
        yuzenSeritiTazele()
    }

    private val gunSerisiGizleyici = Runnable {
        findViewById<View>(R.id.yuzenSeritKart)?.visibility = View.GONE
    }

    /** v10.49 · #10 & v11.01: Yüzen Canlı Durum Şeridi ve Gün Seriniz Açılışta Göster / Gizle. */
    fun yuzenSeritiTazele() {
        val kart = findViewById<View>(R.id.yuzenSeritKart) ?: return
        val metinView = findViewById<TextView>(R.id.yuzenSeritMetin) ?: return
        if (!GorunumAyar.yuzenSeritAcik(this)) {
            kart.visibility = View.GONE
            return
        }
        val metin = GorunumAyar.yuzenSeritMetni(this)
        metinView.text = metin
        kart.visibility = View.VISIBLE
        kart.setOnClickListener {
            if (TimerEngine.isRunning(this)) {
                openTimer()
            } else {
                OtonomMerkezActivity.ac(this)
            }
        }

        // v11.01: "Gün seriniz yazısı altta sürekli duruyor açılışta göstersin sonra kaybolsun"
        if (!TimerEngine.isRunning(this) && GorunumAyar.isGunSerisiOtoGizle(this)) {
            kart.removeCallbacks(gunSerisiGizleyici)
            kart.postDelayed(gunSerisiGizleyici, GorunumAyar.GUN_SERISI_GIZLEME_SURESI_MS)
        } else {
            kart.removeCallbacks(gunSerisiGizleyici)
        }
    }

    override fun onStop() {
        super.onStop()
        // v7.61: bekleyen otomatik yedegi kaybetme — hemen tamamla
        try {
            Store.bekleyenYedegiBitir()
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "Bekleyen yedek bitirilemedi", e)
        }
        try {
            WidgetCommon.refreshAll(this)
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "Widget tazelenemedi", e)
        }
        // v9.8 · Öz denetimde bulunan boşluk:
        // `Kullanim` sayaçları bellekte biriktirilip 10 olayda bir
        // diske yazılıyordu. Kullanıcı 6 ekran gezip uygulamayı
        // kapatırsa o 6 kayıt KAYBOLUYORDU. Ekran başına 1 olay
        // düşünüldüğünde çoğu oturum eşiğe hiç ulaşmazdı — yani
        // analitik verinin büyük kısmı hiç yazılmayacaktı.
        try {
            Kullanim.bitir(this)
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "Kullanım kaydı yazılamadı", e)
        }
    }
}
