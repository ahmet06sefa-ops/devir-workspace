package com.gunlukasistan.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * v7.85 — Halka kadranlı geri sayım göstergesi.
 *
 * ── Kullanıcının isteği ──
 * Google Saat uygulamasındaki gibi: dışta çizgilerden oluşan bir halka,
 * ortada dolgu daire, içinde büyük süre metni, altında bitiş saati.
 *
 * ── Neden özel View ──
 * Bu görünüm hazır bileşenlerle yapılamıyor:
 *   · `CircularProgressIndicator` düz bir yay çizer, çizgili kadran yok
 *   · Çizgileri tek tek ImageView ile koymak 60 görünüm demek — ağır
 * `onDraw` içinde döngüyle çizmek hem hafif hem tam istenen görüntü.
 *
 * ── Çizim mantığı ──
 * 60 çizgi (saniye kadranı gibi) daire çevresine yerleştirilir. Kalan
 * süreye düşen çizgiler **vurgu rengiyle ve kalın**, geçenler soluk çizilir.
 * Böylece hem sayısal hem görsel geri bildirim olur.
 *
 * ── Performans ──
 * Paint nesneleri bir kez oluşturulur (onDraw içinde nesne yaratmak
 * çizim başına çöp üretir ve kasmaya yol açar).
 */
class SayacKadraniView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    /** Kadrandaki çizgi sayısı — 60 saat kadranı hissi verir. */
    private val cizgiSayisi = 60

    private var oran = 1f          // 0..1 kalan oran
    private var sureMetni = "00:00"
    private var ustMetin = ""      // "30 d" — toplam süre
    private var altMetin = ""      // "🔔 12:26" — bitiş saati
    private var calisiyor = false

    private var vurguRenk = 0xFF7C6BF5.toInt()
    private var solukRenk = 0x33FFFFFF
    private var metinRenk = Color.WHITE
    private var ikincilRenk = 0xB3FFFFFF.toInt()
    private var daireRenk = 0xFF2A2A2E.toInt()

    private val cizgiBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val daireBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val sureBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val ustBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val altBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    // ── v10.12 · D20/D23: hayalet yaylar ve seans işaretleri ──
    private var isaretDolu = 0
    private var isaretToplam = 0
    private var macSen = 0f
    private var macRakip = 0f

    private val isaretBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val macRakipBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val macSenBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val gecici = RectF()

    // ══════════════════════════════════════════════════════════════
    // v8.4 · Öneri 19 — Nefes alan halka
    // ══════════════════════════════════════════════════════════════
    //
    // ── Ne eksikti ──
    // Kadran doğruydu ama tamamen sabitti: 30 dakika kalmışla 5 saniye
    // kalmış aynı görünüyordu. Sayıyı okumadan durumu anlamak mümkün
    // değildi.
    //
    // ── Eklenenler ──
    // 1. Kalan süreye göre renk geçişi (yeşil → sarı → kırmızı)
    // 2. Son 10 saniyede halkanın nabız gibi atması
    // 3. Bitişte kısa bir parlama
    //
    // ── Neden ValueAnimator değil ──
    // Nabız için ayrı bir animatör çalıştırmak, sayaç zaten saniyede
    // bir invalidate ederken ikinci bir çizim döngüsü açardı. Bunun
    // yerine faz `System.currentTimeMillis()`'ten hesaplanıyor ve
    // yalnız nabız etkinken `postInvalidateOnAnimation()` çağrılıyor.

    /** Kalan saniye — nabız ve renk geçişi buna bakıyor. */
    private var kalanSaniye = Long.MAX_VALUE

    /** v10.2 · A4: spurt bölgesi hesabı için toplam süre (-1 = zaman bazlı zemine düş). */
    private var toplamMillis = -1L

    /**
     * v10.41 · Kullanıcı maddesi #2: kadran yazı ölçeği.
     * Özel View'da çizilen metinler `sp`/fontScale sisteminden muaftır;
     * bu yüzden uygulama yazı boyu büyütülse de küçültülse de kadran
     * orantısız kalıyordu. [SayacAyar.kadranOlcek] buraya bağlanır.
     */
    var yaziOlcek = 1f
        set(v) { field = v; invalidate() }

    /** Kalan süreye göre renk değişsin mi (ayardan kapatılabilir). */
    var renkGecisi = true
        set(v) { field = v; invalidate() }

    /** Son 10 saniyede nabız atsın mı. */
    var nabizAcik = true
        set(v) { field = v; invalidate() }

    /** Bitiş parlaması 0..1 (TimerFragment tetikliyor). */
    private var parlama = 0f

    // ══════════════════════════════════════════════════════════════
    // v10.7 · Öneri A3 — Halkadan süre seçimi
    // ══════════════════════════════════════════════════════════════
    //
    // ── Ne eksikti ──
    // Kadran hiç dokunma işlemi yapmıyordu (bu dosyada onTouch kodu
    // yoktu). Süre yalnızca hazır çiplerden (5/15/25) ya da diyalogdan
    // seçilebiliyordu. Artık dış halka sürüklenerek süre kurulur.
    //
    // ── Tıklama ile çakışmama kuralı ──
    // Ortaya dokunmak başlat/duraklat olarak kalır; süre seçimi yalnız
    // **dış halka bandında** başlar ([HalkaSecti.halkadaMi]). Parmak
    // [scaledTouchSlop] kadar gezmediyse olay tıklama sayılıp
    // performClick'e düşer — eski davranış değişmez.
    //
    // ── Geometri ──
    // Matematik tamamen [HalkaSecti]'de ve birim testli; bu katman
    // yalnızca olayları devredip titreşim (CLOCK_TICK) üretir.

    /** Süre seçimi köprüsü — TimerFragment uygular. */
    interface SureSecici {
        /** Şu an seçime uygun mu (geri sayım, boşta, zincir koşmuyor). */
        fun izinVar(): Boolean

        /** Jest başladı (slop geçildi) — ekran seçim durumuna geçsin. */
        fun secimBasladi()

        /** Sürükleme sırasında her dakika değişiminde. */
        fun dakikaSecildi(dakika: Int)

        /** Parmak kalktı — kesinleşen dakika. */
        fun secimBitti(dakika: Int)

        /**
         * Jest yarıda kesildi (ebeveyn olayı çaldı vb.) — yapılan
         * canlı değişiklik geri alınmalı; asla yarım dakika commit
         * edilmez.
         */
        fun secimIptal()
    }

    var sureSecici: SureSecici? = null

    // Jest durumu
    private var dokunmaAday = false
    private var surukleniyor = false
    private var basX = 0f
    private var basY = 0f
    private var sonDakika = -1

    // Çizimle aynı geometri (nabız ölçeği hariç — eşik sabit kalmalı)
    private var merkezXe = 0f
    private var merkezYe = 0f
    private var disSinir = 0f
    private var icSinir = 0f
    private val dokunmaSlop = ViewConfiguration.get(context).scaledTouchSlop

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        merkezXe = w / 2f
        merkezYe = h / 2f
        // onDraw ile aynı formül, nabız çarpanı olmadan
        val dis = (min(w, h) / 2f - 4f)
        disSinir = dis
        icSinir = dis - dis * 0.11f
    }

    private val parlamaBoya = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    init {
        // Sistem yazı tipleri — poppins varsa onu kullan
        runCatching {
            val kalin = androidx.core.content.res.ResourcesCompat
                .getFont(context, R.font.poppins_bold)
            val normal = androidx.core.content.res.ResourcesCompat
                .getFont(context, R.font.poppins_medium)
            sureBoya.typeface = kalin
            ustBoya.typeface = normal
            altBoya.typeface = normal
        }
    }

    /**
     * v10.12 · D23 — Günlük seans hedefi işaretleri.
     * 0/0 verilirse halka çizilmez; dolu, toplamı aşamaz.
     */
    fun isaretleriAyarla(dolu: Int, toplam: Int) {
        val t = toplam.coerceAtLeast(0)
        val d = dolu.coerceIn(0, t)
        if (d == isaretDolu && t == isaretToplam) return
        isaretDolu = d
        isaretToplam = t
        invalidate()
    }

    /**
     * v10.12 · D20 — Hayalet maç yayları (0..1 oranlar; 0 = yok).
     * Rakip yay dışta, senin yayın bir tık içte.
     */
    fun maciAyarla(sen: Float, rakip: Float) {
        val s = sen.coerceIn(0f, 1f)
        val r = rakip.coerceIn(0f, 1f)
        if (s == macSen && r == macRakip) return
        macSen = s
        macRakip = r
        invalidate()
    }

    /** Tema renklerini dışarıdan uygular — koyu/açık temada okunur kalsın. */
    fun renkleriAyarla(
        vurgu: Int,
        metin: Int,
        ikincil: Int,
        daire: Int,
        soluk: Int
    ) {
        vurguRenk = vurgu
        metinRenk = metin
        ikincilRenk = ikincil
        daireRenk = daire
        solukRenk = soluk
        invalidate()
    }

    /**
     * Gösterilecek durumu günceller.
     *
     * @param kalanOran 1 = hiç harcanmadı, 0 = bitti
     */
    fun guncelle(
        kalanOran: Float,
        sure: String,
        ust: String,
        alt: String,
        aktif: Boolean,
        kalanSn: Long = Long.MAX_VALUE,
        toplamMs: Long = -1L
    ) {
        oran = kalanOran.coerceIn(0f, 1f)
        sureMetni = sure
        ustMetin = ust
        altMetin = alt
        calisiyor = aktif
        kalanSaniye = kalanSn
        if (toplamMs > 0) toplamMillis = toplamMs
        invalidate()
    }

    /**
     * v8.4 · Öneri 19 — Bitişte tam ekran renk patlaması.
     *
     * `TimerFragment` sayaç bitince çağırıyor. Halkanın üstünde
     * merkezden dışa doğru genişleyen bir parlama çiziliyor.
     */
    fun bitisParlamasi() {
        if (!GorunumAyar.animasyonAcik(context)) return
        runCatching {
            android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 900
                addUpdateListener {
                    parlama = it.animatedValue as Float
                    invalidate()
                }
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        parlama = 0f
                        invalidate()
                    }
                })
                start()
            }
        }
    }

    /**
     * Kalan süreye göre kadran rengi.
     *
     * v10.2 · A4 — Uyarı bölgesi [SayacSpurt]'a taşındı: artık
     * `max(5 dk, toplamın %10'u)`. 25 dk Pomodoro'da davranış aynı,
     * 2 saatlik oturumda spurt son 12 dakikada başlıyor.
     *
     * Tek renkten diğerine sıçramak yerine aralarda karışım
     * yapılıyor — geçiş fark edilmeden oluyor.
     */
    private fun aktifRenk(): Int {
        if (!renkGecisi || kalanSaniye == Long.MAX_VALUE) return vurguRenk
        val amber = 0xFFE0A33A.toInt()
        val kirmizi = 0xFFD9534F.toInt()
        val baslangic = SayacSpurt.uyariBaslangiciSn(toplamMillis)
        return when {
            kalanSaniye > baslangic -> vurguRenk
            kalanSaniye > SayacSpurt.KRITIK_SN -> {
                // uyarı bandı: vurgudan ambere
                karistir(vurguRenk, amber, SayacSpurt.bandOrani(kalanSaniye, toplamMillis))
            }
            kalanSaniye > 10 -> {
                // 60 → 10 sn arası: amberden kırmızıya
                val t = 1f - ((kalanSaniye - 10f) / 50f)
                karistir(amber, kirmizi, t)
            }
            else -> kirmizi
        }
    }

    private fun karistir(a: Int, b: Int, oran: Float): Int {
        val o = oran.coerceIn(0f, 1f)
        val t = 1f - o
        return Color.argb(
            255,
            (Color.red(a) * t + Color.red(b) * o).toInt(),
            (Color.green(a) * t + Color.green(b) * o).toInt(),
            (Color.blue(a) * t + Color.blue(b) * o).toInt()
        )
    }

    /** Son 10 saniyede 0.94..1.06 arası nabız ölçeği. */
    private fun nabizOlcegi(): Float {
        if (!nabizAcik || !calisiyor) return 1f
        if (kalanSaniye > 10 || kalanSaniye < 0) return 1f
        if (!GorunumAyar.animasyonAcik(context)) return 1f
        // 1 saniyelik döngü; sinüs ile yumuşak
        val faz = (System.currentTimeMillis() % 1000L) / 1000.0
        return 1f + 0.06f * kotlin.math.sin(faz * 2 * Math.PI).toFloat()
    }

    companion object {
        const val KOMPAKT_KADRAN_ORANI = 0.46f
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        val tekEkranMi = SayacAyar.isTekEkranKompaktMod(context)
        val kenar = when {
            tekEkranMi -> (w * KOMPAKT_KADRAN_ORANI).toInt()
            MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED -> w
            else -> min(w, h)
        }
        setMeasuredDimension(kenar, kenar)
    }

    override fun onDraw(canvas: Canvas) {
        val merkezX = width / 2f
        val merkezY = height / 2f
        // v8.4 · Öneri 19: son 10 saniyede nabız
        val nabiz = nabizOlcegi()
        val disYaricap = (min(width, height) / 2f - 4f) * nabiz
        if (disYaricap <= 0f) return
        // Nabız etkinse bir sonraki kareyi iste (saniyelik tazelemeye
        // güvenmiyoruz; nabız 60 fps akmalı)
        if (nabiz != 1f) postInvalidateOnAnimation()

        // Kalan süreye göre kadran rengi
        val kadranRengi = aktifRenk()

        val cizgiUzunluk = disYaricap * 0.11f
        val icYaricap = disYaricap - cizgiUzunluk - disYaricap * 0.07f

        // ── 1. Dolgu daire ──
        daireBoya.color = daireRenk
        canvas.drawCircle(merkezX, merkezY, icYaricap, daireBoya)

        // ── 1.1 · v10.12 / D23: seans işaretleri (çember içi noktalar) ──
        if (isaretToplam > 0) {
            val noktaYaricap = disYaricap * 0.020f
            val merkezYaricap = icYaricap - noktaYaricap - disYaricap * 0.012f
            for (i in 0 until isaretToplam) {
                val aci = Math.toRadians((i * 360.0 / isaretToplam) - 90.0)
                isaretBoya.color = if (i < isaretDolu) vurguRenk else solukRenk
                canvas.drawCircle(
                    merkezX + cos(aci).toFloat() * merkezYaricap,
                    merkezY + sin(aci).toFloat() * merkezYaricap,
                    noktaYaricap,
                    isaretBoya
                )
            }
        }

        // ── 1.2 · v10.12 / D20: hayalet maç yayları ──
        // Rakip: ikincil renk, yarı saydam; sen: vurgu. İkisi de tepe
        // noktadan saat yönünde ilerler — günün aynı ölçeğinde yarış.
        if (macRakip > 0f) {
            macRakipBoya.color = (ikincilRenk and 0x00FFFFFF) or (140 shl 24)
            macRakipBoya.strokeWidth = disYaricap * 0.016f
            val r = icYaricap * 0.80f
            gecici.set(merkezX - r, merkezY - r, merkezX + r, merkezY + r)
            canvas.drawArc(gecici, -90f, 360f * macRakip, false, macRakipBoya)
        }
        if (macSen > 0f) {
            macSenBoya.color = vurguRenk
            macSenBoya.strokeWidth = disYaricap * 0.020f
            val r = icYaricap * 0.73f
            gecici.set(merkezX - r, merkezY - r, merkezX + r, merkezY + r)
            canvas.drawArc(gecici, -90f, 360f * macSen, false, macSenBoya)
        }

        // ── 2. Kadran çizgileri ──
        // Kalan orana düşen çizgi sayısı; en az 1 (bitmeden tamamen sönmesin)
        val aktifCizgi = if (oran <= 0f) 0 else (cizgiSayisi * oran).toInt().coerceAtLeast(1)

        for (i in 0 until cizgiSayisi) {
            // -90° = tepe noktası; saat yönünde ilerle
            val aci = Math.toRadians((i * 360.0 / cizgiSayisi) - 90.0)
            val kos = cos(aci).toFloat()
            val sin = sin(aci).toFloat()

            val aktifMi = i < aktifCizgi
            cizgiBoya.color = if (aktifMi) kadranRengi else solukRenk
            cizgiBoya.strokeWidth = if (aktifMi) disYaricap * 0.022f else disYaricap * 0.014f

            val bas = disYaricap - cizgiUzunluk
            canvas.drawLine(
                merkezX + kos * bas,
                merkezY + sin * bas,
                merkezX + kos * disYaricap,
                merkezY + sin * disYaricap,
                cizgiBoya
            )
        }

        // ── 3. Üst etiket (toplam süre) ──
        if (ustMetin.isNotBlank()) {
            ustBoya.color = ikincilRenk
            ustBoya.textSize = icYaricap * 0.155f * yaziOlcek
            canvas.drawText(ustMetin, merkezX, merkezY - icYaricap * 0.34f, ustBoya)
        }

        // ── 4. Büyük süre metni ──
        sureBoya.color = metinRenk
        // Uzun metin (1:23:45) taşmasın diye ölçüye göre küçült
        sureBoya.textSize = when {
            sureMetni.length >= 7 -> icYaricap * 0.40f * yaziOlcek
            sureMetni.length >= 5 -> icYaricap * 0.52f * yaziOlcek
            else -> icYaricap * 0.60f * yaziOlcek
        }
        // Metni dikeyde ortala: baseline düzeltmesi
        // v11.13 DÜZELTMESİ: kompakt modda da süre metni çizilir.
        // Eskiden kompakt modda atlanıyor ve timeText TextView'ine
        // güveniliyordu; timeText görünmez yapılınca süre kayboluyordu.
        // Kadran artık her modda süreyi kendisi çizer (tek gerçek kaynak).
        val olcum = sureBoya.fontMetrics
        val taban = merkezY - (olcum.ascent + olcum.descent) / 2f
        canvas.drawText(sureMetni, merkezX, taban, sureBoya)

        // ── 5. Alt etiket (bitiş saati) ──
        if (altMetin.isNotBlank()) {
            altBoya.color = if (calisiyor) ikincilRenk else (ikincilRenk and 0x88FFFFFF.toInt())
            altBoya.textSize = icYaricap * 0.145f * yaziOlcek
            canvas.drawText(altMetin, merkezX, merkezY + icYaricap * 0.44f, altBoya)
        }

        // ── 6. v8.4 · Öneri 19: bitiş parlaması ──
        // Merkezden dışa doğru genişleyen, solarak kaybolan halka.
        if (parlama > 0f) {
            val yaricap = disYaricap * (0.3f + parlama * 0.95f)
            val alfa = ((1f - parlama) * 170).toInt().coerceIn(0, 255)
            parlamaBoya.color = Color.argb(
                alfa,
                Color.red(kadranRengi), Color.green(kadranRengi), Color.blue(kadranRengi)
            )
            canvas.drawCircle(merkezX, merkezY, yaricap, parlamaBoya)
        }
    }

    // ── v10.7 · A3: dokunma — halkadan süre seçimi ──

    override fun onTouchEvent(olay: MotionEvent): Boolean {
        val secici = sureSecici
        if (secici == null || !secici.izinVar()) {
            // Seçici yoksa ya da sayaç meşgulse klasik davranış
            // (tık = başlat/duraklat) olduğu gibi devam eder.
            dokunmaAday = false
            surukleniyor = false
            return super.onTouchEvent(olay)
        }

        when (olay.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dokunmaAday = HalkaSecti.halkadaMi(
                    olay.x, olay.y, merkezXe, merkezYe, icSinir, disSinir
                )
                if (!dokunmaAday) {
                    // Merkez — tıklama akışına bırak
                    return super.onTouchEvent(olay)
                }
                basX = olay.x
                basY = olay.y
                surukleniyor = false
                sonDakika = -1
                // Halka üzerinde başlayan jest ScrollView'a kaptırılmasın —
                // yoksa dikey kaydırma slop'u önce davranır ve jest
                // yarıda kalırdı. Bu yüzden halkadan kaydırma başlatılamaz;
                // diğer tüm bölgeler normal kaydırır.
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!dokunmaAday) return super.onTouchEvent(olay)
                if (!surukleniyor) {
                    if (!HalkaSecti.suruklemeMi(basX, basY, olay.x, olay.y, dokunmaSlop.toFloat())) {
                        return true
                    }
                    surukleniyor = true
                    secici.secimBasladi()
                }
                bildir(secici, olay.x, olay.y)
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (!dokunmaAday) return super.onTouchEvent(olay)
                parent?.requestDisallowInterceptTouchEvent(false)
                val sonuc: Boolean
                if (surukleniyor) {
                    secici.secimBitti(sonDakika.coerceAtLeast(HalkaSecti.MIN_DAKIKA))
                    sonuc = true
                } else {
                    // Halkaya dokunup bırakılması da başlat/duraklat demek
                    sonuc = performClick()
                }
                dokunmaAday = false
                surukleniyor = false
                return sonuc
            }

            MotionEvent.ACTION_CANCEL -> {
                // Jest yarıda kesildi — ASLA yarım seçim uygulanmaz;
                // ekran jest öncesi süreye geri döner.
                if (dokunmaAday && surukleniyor) {
                    secici.secimIptal()
                }
                dokunmaAday = false
                surukleniyor = false
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(olay)
    }

    /** Dakika değiştiyse bildirir ve saat tıkı titreşimi verir. */
    private fun bildir(secici: SureSecici, x: Float, y: Float) {
        val dk = HalkaSecti.acidanDakika(HalkaSecti.aci(x, y, merkezXe, merkezYe))
        if (dk == sonDakika) return
        sonDakika = dk
        secici.dakikaSecildi(dk)
        runCatching {
            performHapticFeedback(
                HapticFeedbackConstants.CLOCK_TICK,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
