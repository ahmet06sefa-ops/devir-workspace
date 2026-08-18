package com.gunlukasistan.app

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * "🗓️ Bugün" ekranı (v5.2).
 * Günün özeti: odak/soru durumu, bugünün görevleri, yaklaşan geri sayımlar,
 * duruma göre değişen asistan önerisi ve hızlı eylemler.
 */
class TodayFragment : Fragment(R.layout.fragment_today) {

    private val turkish = Locale("tr", "TR")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // v8.6 · Öneri 29: yatay/tablet genişlik sınırı + yoğunluk
        Duzen.uygula(view)
        // v10.18 · Ekran Atölyesi: Bugün düzeni + blok basılı tut → yerinde düzenleme
        BugunDuzen.uygula(requireContext(), view)
        duzenSeridi = DuzenSeridi(requireContext(), bugunKapi())
        duzenSeridi?.bagla(view)
        view.findViewById<View>(R.id.todayTasksAll).setOnClickListener {
            (activity as? MainActivity)?.openTasks()
        }
        view.findViewById<View>(R.id.todayEventsAll).setOnClickListener {
            (activity as? MainActivity)?.openEvents()
        }
        view.findViewById<View>(R.id.todayHabitsAll).setOnClickListener {
            (activity as? MainActivity)?.openHabits()
        }
        view.findViewById<View>(R.id.todayChipFocus).setOnClickListener {
            (activity as? MainActivity)?.openTimer()
        }
        view.findViewById<View>(R.id.todayChipQuestions).setOnClickListener {
            (activity as? MainActivity)?.quickAddQuestions()
        }
        view.findViewById<MaterialButton>(R.id.todayQuickTimer).setOnClickListener {
            (activity as? MainActivity)?.openTimer()
        }
        view.findViewById<MaterialButton>(R.id.todayQuickQuestions).setOnClickListener {
            (activity as? MainActivity)?.quickAddQuestions()
        }
        view.findViewById<MaterialButton>(R.id.todayQuickTask).setOnClickListener {
            (activity as? MainActivity)?.openTasksWithEditor()
        }
        view.findViewById<MaterialButton>(R.id.todayQuickAsistan).setOnClickListener {
            (activity as? MainActivity)?.openAsistan()
        }
        // v7.46: namaz kartına dokun → namaz & plan ekranı
        view.findViewById<View>(R.id.todayNamazCard).setOnClickListener {
            NamazActivity.ac(requireContext())
        }
        bugunKartlariniKur(view)
    }

    override fun onResume() {
        super.onResume()
        // v10.18: düzenlemeden dönülmüş olabilir — sıra/gizleme tazelensin
        view?.let { BugunDuzen.uygula(requireContext(), it) }
        bind()
        view?.let {
            GlassmorphismTemaMotoru.sekmeleriVeKartlariStille(it, requireContext())
            bugunSiraVeTasinanUygula(it)
        }
    }

    private var duzenSeridi: DuzenSeridi? = null

    /** v10.18 — Bugün ekranının yerinde düzenleme kapısı (saklama: BugunDuzen). */
    private fun bugunKapi(): DuzenSeridi.Kapi = object : DuzenSeridi.Kapi {
        override val bloklar get() = BugunDuzen.bloklar
        override fun sira() = BugunDuzen.sira(requireContext())
        override fun tasi(kod: String, yon: Int) =
            BugunDuzen.tasi(requireContext(), kod, yon)
        override fun gizliMi(kod: String) = kod in BugunDuzen.gizliler(requireContext())
        override fun gizle(kod: String, gizli: Boolean) =
            BugunDuzen.gizle(requireContext(), kod, gizli)
        override fun boyutKademe(kod: String) =
            BugunDuzen.boyutKademe(requireContext(), kod)
        override fun boyutYaz(kod: String, kademe: Int) =
            BugunDuzen.setBoyutKademe(requireContext(), kod, kademe)
        override fun katliMi(kod: String) = BugunDuzen.katliMi(requireContext(), kod)
        override fun katlaYaz(kod: String, katli: Boolean) =
            BugunDuzen.setKatli(requireContext(), kod, katli)
        override fun sifirla() = BugunDuzen.varsayilanaDon(requireContext())
        override fun kok(): View? = view
        override fun tazele() {
            view?.let { BugunDuzen.uygula(requireContext(), it) }
            bind()
        }
    }

    /** Dışarıdan (ör. soru eklendikten sonra) tazelemek için. */
    fun refresh() = bind()

    /**
     * v7.95 — "Şimdi ne yapmalıyım?" kartını çizer (öneri 3).
     *
     * Altı ayrı liste yerine tek karar. Gerekçe de gösteriliyor:
     * kullanıcı önerinin neden çıktığını bilmezse güvenmiyor.
     */
    private fun simdiNeCiz(view: View) {
        val ctx = context ?: return
        val kart = view.findViewById<View>(R.id.simdiNeCard) ?: return
        val oneri = runCatching { SimdiNe.oner(ctx) }.getOrNull()

        val simge = view.findViewById<TextView>(R.id.simdiNeSimge)
        val baslik = view.findViewById<TextView>(R.id.simdiNeBaslik)
        val aciklama = view.findViewById<TextView>(R.id.simdiNeAciklama)
        val neden = view.findViewById<TextView>(R.id.simdiNeNeden)
        val digerKap = view.findViewById<LinearLayout>(R.id.simdiNeDigerler)
        digerKap?.removeAllViews()

        if (oneri == null) {
            simge?.text = "✅"
            baslik?.text = getString(R.string.sn_bos_baslik)
            aciklama?.text = SimdiNe.bosMesaj(ctx)
            neden?.visibility = View.GONE
            kart.setOnClickListener(null)
            kart.isClickable = false
            return
        }

        simge?.text = oneri.simge
        baslik?.text = oneri.baslik
        aciklama?.text = oneri.aciklama
        neden?.text = oneri.neden
        neden?.visibility = View.VISIBLE
        kart.isClickable = true
        kart.setOnClickListener { oneriyiUygula(oneri) }

        // İkincil öneriler — tek satırlık kısayollar
        runCatching { SimdiNe.digerleri(ctx, 2) }.getOrNull()?.forEach { d ->
            val satir = TextView(ctx).apply {
                text = d.simge + "  " + d.baslik
                textSize = 12.5f
                alpha = 0.8f
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setTextColor(
                    com.google.android.material.color.MaterialColors.getColor(
                        ctx, com.google.android.material.R.attr.colorOnPrimaryContainer, 0
                    )
                )
                setPadding(0, (6 * resources.displayMetrics.density).toInt(), 0, 0)
                isClickable = true
                setOnClickListener { oneriyiUygula(d) }
            }
            digerKap?.addView(satir)
        }
    }

    /** Öneriye dokununca ilgili ekranı açar ya da eylemi çalıştırır. */
    private fun oneriyiUygula(oneri: SimdiNe.Oneri) {
        val ana = activity as? MainActivity
        val ctx = context ?: return
        when (oneri.eylem) {
            SimdiNe.Eylem.NAMAZ_AC -> runCatching { NamazActivity.ac(ctx) }
            SimdiNe.Eylem.GOREV_AC -> ana?.openTasks()
            SimdiNe.Eylem.ALISKANLIK_AC -> ana?.openHabits()
            SimdiNe.Eylem.HATA_TEKRAR -> HatalarimActivity.ac(ctx)
            // v9.0 · Öneri 53
            SimdiNe.Eylem.KONU_TEKRAR -> TekrarActivity.ac(ctx)
            // v9.4 · Öneri 11: boş blok → sayaç
            SimdiNe.Eylem.BOS_ZAMAN -> (activity as? MainActivity)?.openTimer()
            SimdiNe.Eylem.PLAN_AC -> ana?.openPlan()
            SimdiNe.Eylem.MOLA_VER, SimdiNe.Eylem.SAYAC_BASLAT -> {
                ana?.openTimer()
            }
            SimdiNe.Eylem.DERS_CALIS -> {
                // Sayacı başlat ve sayaç ekranına geç — tek dokunuşla çalışmaya başla
                runCatching {
                    if (!TimerEngine.isRunning(ctx)) {
                        TimerEngine.setMode(ctx, TimerEngine.MODE_DOWN)
                        TimerEngine.start(ctx)
                        TimerAlarm.reschedule(ctx)
                    }
                }
                ana?.openTimer()
            }
        }
    }

    private fun bind() {
        val view = view ?: return
        simdiNeCiz(view)
        val context = context ?: return

        bindHeader(view, context)
        bindNamaz(view, context)
        bindCizelge(view, context)   // v8.5 · Öneri 23
        bindStatus(view, context)
        bindTasks(view, context)
        bindHabits(view, context)
        bindEvents(view, context)
        bindTip(view, context)
    }

    // ---------------- Selamlama ----------------

    /**
     * v8.5 · Öneri 23 — Günün zaman çizelgesi.
     *
     * ── Neden görev listesinin yerine geçmedi ──
     * Vadesi olmayan görevler çizelgede gösterilemez (saati yok) ama
     * yine de yapılmalı. Çizelge "ne zaman" sorusunu, alttaki liste
     * "ne" sorusunu yanıtlıyor. İkisi birlikte çalışıyor.
     *
     * ── Ne gösteriliyor ──
     * · Bugün vadesi olan görevler (tamamlananlar soluk)
     * · Bugünkü sayaç oturumları (geçmiş odak blokları)
     * · Namaz vakitleri (ince şeritler, modül açıksa)
     * · Şu an kırmızı çizgi
     */
    private fun bindCizelge(view: View, context: Context) {
        val kart = view.findViewById<View>(R.id.cizelgeKart) ?: return
        val cizelge = view.findViewById<ZamanCizelgesiView>(R.id.cizelge) ?: return

        val ogeler = mutableListOf<ZamanCizelgesiView.Oge>()
        val takvim = java.util.Calendar.getInstance()

        runCatching {
            // ---- Bugün vadesi olan görevler ----
            val gunBasi = (takvim.clone() as java.util.Calendar).apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
            val gunSonu = gunBasi + 86_400_000L

            val gorevRengi = com.google.android.material.color.MaterialColors.getColor(
                cizelge, com.google.android.material.R.attr.colorPrimary, 0xFFB08968.toInt()
            )
            Store.aktifGorevler(context)
                .filter { it.dueAt in gunBasi until gunSonu }
                .forEach { g ->
                    val c = java.util.Calendar.getInstance().apply { timeInMillis = g.dueAt }
                    ogeler.add(
                        ZamanCizelgesiView.Oge(
                            baslik = g.text,
                            baslangicDk = c.get(java.util.Calendar.HOUR_OF_DAY) * 60 +
                                c.get(java.util.Calendar.MINUTE),
                            sureDk = 45,
                            // Etiket rengi varsa onu kullan; yoksa tema vurgusu.
                            // Etiket.renk atanmamışta TRANSPARENT döndürüyor.
                            renk = Etiket.renk(g.etiket)
                                .takeIf { it != android.graphics.Color.TRANSPARENT } ?: gorevRengi,
                            tur = ZamanCizelgesiView.Tur.GOREV,
                            tamamlandi = g.done,
                            veri = g
                        )
                    )
                }

            // ---- Bugünkü sayaç oturumları ----
            val odakRengi = com.google.android.material.color.MaterialColors.getColor(
                cizelge, com.google.android.material.R.attr.colorSecondary, 0xFF7C9070.toInt()
            )
            OdakKaydi.bugunkuler(context).forEach { o ->
                val c = java.util.Calendar.getInstance().apply { timeInMillis = o.zaman }
                // Oturum BİTİŞ zamanı kaydediliyor; başlangıcı geri hesapla
                val bitisDk = c.get(java.util.Calendar.HOUR_OF_DAY) * 60 +
                    c.get(java.util.Calendar.MINUTE)
                ogeler.add(
                    ZamanCizelgesiView.Oge(
                        baslik = o.baslik.ifBlank { getString(R.string.zc_odak) },
                        baslangicDk = (bitisDk - o.dakika).coerceAtLeast(0),
                        sureDk = o.dakika,
                        renk = odakRengi,
                        tur = ZamanCizelgesiView.Tur.ODAK,
                        tamamlandi = true
                    )
                )
            }

            // ---- v9.4 · Öneri 10: telefonun takvimindeki etkinlikler ----
            //
            // Uygulama "bugün boşsun" derken saat 14'te dersin olması
            // saçmaydı. Artık takvim de çizelgede.
            // Bizim yazdıklarımız (damgalı) filtreleniyor — iki kez
            // görünmesinler.
            runCatching {
                if (TakvimKopru.okumaAcik(context)) {
                    val takvimRengi = 0xFF7A6BC7.toInt()
                    TakvimKopru.gununEtkinlikleri(context).forEach { e ->
                        if (e.tumGun) return@forEach
                        ogeler.add(
                            ZamanCizelgesiView.Oge(
                                baslik = e.baslik,
                                baslangicDk = e.baslangicDk,
                                sureDk = e.sureDk,
                                renk = if (e.renk != 0) e.renk else takvimRengi,
                                tur = ZamanCizelgesiView.Tur.DERS
                            )
                        )
                    }
                }
            }

            // ---- Namaz vakitleri ----
            if (NamazVakti.acikMi(context)) {
                val gun = NamazVakti.bugunDuzeltilmis(context)
                NamazVakti.Vakit.entries.forEach { v ->
                    val dk = gun.dakika(v)
                    if (dk >= 0) {
                        ogeler.add(
                            ZamanCizelgesiView.Oge(
                                baslik = v.emoji + " " + getString(v.adRes),
                                baslangicDk = dk,
                                sureDk = 0,
                                renk = 0xFF7A8FA6.toInt(),
                                tur = ZamanCizelgesiView.Tur.NAMAZ
                            )
                        )
                    }
                }
            }
        }.onFailure { android.util.Log.w("TodayFragment", "Çizelge verisi", it) }

        // Hiç öğe yoksa kartı gizle — boş bir saat şeridi göstermek anlamsız
        // Namaz dışında bir şey varsa kart gösterilsin (takvim dahil)
        val gosterilecek = ogeler.any { it.tur != ZamanCizelgesiView.Tur.NAMAZ }
        kart.visibility = if (gosterilecek) View.VISIBLE else View.GONE
        if (!gosterilecek) return

        cizelge.ayarla(ogeler)

        val gorevSayisi = ogeler.count { it.tur == ZamanCizelgesiView.Tur.GOREV }
        val odakDk = ogeler.filter { it.tur == ZamanCizelgesiView.Tur.ODAK }.sumOf { it.sureDk }
        view.findViewById<TextView>(R.id.cizelgeOzet)?.text =
            getString(R.string.zc_ozet, gorevSayisi, odakDk)

        cizelge.ogeSecildi = { oge ->
            when (oge.tur) {
                ZamanCizelgesiView.Tur.GOREV ->
                    (activity as? MainActivity)?.openTasks()
                ZamanCizelgesiView.Tur.ODAK ->
                    (activity as? MainActivity)?.openTimer()
                else -> Unit
            }
        }
        cizelge.bosSaatSecildi = { saat ->
            // Boş saate dokun → o saate görev ekle
            (activity as? MainActivity)?.openTasksWithEditor()
        }
    }

    /**
     * v7.46: Namaz vakti kartı — modül kapalıysa hiç görünmez.
     * Sıradaki vakit, kalan süre ve o dilim için öneri gösterilir.
     */
    private fun bindNamaz(view: View, context: Context) {
        val kart = view.findViewById<View>(R.id.todayNamazCard) ?: return
        if (!NamazVakti.acikMi(context)) {
            kart.visibility = View.GONE
            return
        }
        try {
            val gun = NamazVakti.bugunDuzeltilmis(context)
            val simdi = NamazVakti.simdiDakika()
            val (vakit, kalan) = gun.sonraki(simdi)

            kart.visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.todayNamazNext).text =
                vakit.emoji + " " + getString(vakit.adRes) + " · " +
                    NamazPlan.sureMetni(kalan) + " kaldı"
            view.findViewById<TextView>(R.id.todayNamazTime).text = gun.saat(vakit)
            view.findViewById<TextView>(R.id.todayNamazSuggest).text =
                NamazPlan.simdiNeYapmali(context)
        } catch (e: Exception) {
            android.util.Log.w("TodayFragment", "Namaz kartı çizilemedi", e)
            kart.visibility = View.GONE
        }
    }

    private fun bindHeader(view: View, context: Context) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
            in 5..11 -> getString(R.string.today_hi_morning)
            in 12..17 -> getString(R.string.today_hi_noon)
            in 18..22 -> getString(R.string.today_hi_evening)
            else -> getString(R.string.today_hi_night)
        }
        view.findViewById<TextView>(R.id.todayGreeting).text = greeting
        view.findViewById<TextView>(R.id.todayDate).text =
            SimpleDateFormat("d MMMM yyyy, EEEE", turkish).format(Date())
    }

    // ---------------- Günün durumu ----------------

    private fun bindStatus(view: View, context: Context) {
        val goal = Store.getGoalMinutes(context)
        val focus = Store.getTodayFocusMinutes(context)
        val questions = Store.getTodayQuestions(context)
        val (streak, _) = Store.streakInfo(context)

        val percent = if (goal > 0) (focus * 100 / goal).coerceIn(0, 100) else 0
        view.findViewById<LinearProgressIndicator>(R.id.todayGoalBar).progress = percent

        // v8.6 · Öneri 24: günlük hedefe ilk ulaşıldığında kutlama.
        //
        // Neden "ilk kez" kontrolü: bindStatus her onResume'da çalışıyor.
        // Kontrol olmasaydı hedefi tutturan kullanıcı ekrana her
        // dönüşünde konfeti görürdü — kutlama değil rahatsızlık olurdu.
        if (goal > 0 && focus >= goal) {
            Basari.birKez(context, "hedef_" + Basari.bugun()) {
                Kutlama.goster(this, Kutlama.TUR_KONFETI)
                Titresim.basari(context)
            }
        }

        view.findViewById<TextView>(R.id.todayStatusTitle).text = when {
            focus >= goal && goal > 0 -> getString(R.string.today_status_done)
            percent >= 50 -> getString(R.string.today_status_half)
            focus > 0 -> getString(R.string.today_status_started)
            else -> getString(R.string.today_status_empty)
        }

        view.findViewById<TextView>(R.id.todayGoalText).text =
            getString(R.string.today_goal_line, focus, goal, percent)

        view.findViewById<TextView>(R.id.todayChipFocus).text =
            getString(R.string.today_chip_focus, focus)
        view.findViewById<TextView>(R.id.todayChipQuestions).text =
            getString(R.string.today_chip_questions, questions)
        view.findViewById<TextView>(R.id.todayChipStreak).text =
            getString(R.string.today_chip_streak, streak)
    }

    // ---------------- Bugünün görevleri ----------------

    private fun bindTasks(view: View, context: Context) {
        val box = view.findViewById<LinearLayout>(R.id.todayTasksBox)
        val empty = view.findViewById<TextView>(R.id.todayTasksEmpty)
        box.removeAllViews()

        val all = Store.loadTasks(context)
        val endOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        // Öncelik: bugüne/geçmişe tarihli olanlar, sonra tarihsizler
        val pending = all.filter { !it.done }
        val dueToday = pending.filter { it.dueAt in 1..endOfToday }.sortedBy { it.dueAt }
        val undated = pending.filter { it.dueAt == 0L }.sortedByDescending { it.createdAt }
        val shown = (dueToday + undated).take(5)

        if (shown.isEmpty()) {
            empty.visibility = View.VISIBLE
            if (GorunumAyar.tekAkisKarti(context)) {
                (box.parent as? View)?.visibility = View.GONE
            }
            return
        }
        (box.parent as? View)?.visibility = View.VISIBLE
        empty.visibility = View.GONE

        val inflater = LayoutInflater.from(context)
        shown.forEach { task ->
            val row = inflater.inflate(R.layout.item_today_task, box, false)
            row.findViewById<TextView>(R.id.rowText).text = task.text

            val badge = row.findViewById<TextView>(R.id.rowBadge)
            badge.text = when {
                task.dueAt == 0L -> ""
                task.dueAt < System.currentTimeMillis() -> getString(R.string.today_task_late)
                else -> SimpleDateFormat("HH:mm", turkish).format(Date(task.dueAt))
            }

            row.setOnClickListener { completeTask(context, task) }
            row.findViewById<ImageView>(R.id.rowCheck).setOnClickListener {
                completeTask(context, task)
            }
            box.addView(row)
        }
    }

    private fun completeTask(context: Context, task: Store.Task) {
        val list = Store.loadTasks(context)
        val target = list.firstOrNull { it.id == task.id } ?: return
        target.done = true
        Store.recordCompletion(context)
        try {
            AlarmScheduler.cancel(context, target.id)
        } catch (_: Exception) {
        }
        Store.saveTasks(context, list)
        WidgetCommon.refreshAll(context)
        Toast.makeText(context, R.string.today_task_done, Toast.LENGTH_SHORT).show()
        bind()
    }

    // ---------------- Alışkanlıklar ----------------

    private fun bindHabits(view: View, context: Context) {
        val box = view.findViewById<LinearLayout>(R.id.todayHabitsBox)
        val empty = view.findViewById<TextView>(R.id.todayHabitsEmpty)
        box.removeAllViews()

        val habits = Store.loadHabits(context).filterNot { it.archived }.take(5)
        if (habits.isEmpty()) {
            empty.visibility = View.VISIBLE
            if (GorunumAyar.tekAkisKarti(context)) {
                (box.parent as? View)?.visibility = View.GONE
            }
            return
        }
        (box.parent as? View)?.visibility = View.VISIBLE
        empty.visibility = View.GONE

        val inflater = LayoutInflater.from(context)
        habits.forEach { habit ->
            val row = inflater.inflate(R.layout.item_today_task, box, false)
            val count = Store.habitCount(context, habit.id)
            val done = count >= habit.target

            val check = row.findViewById<ImageView>(R.id.rowCheck)
            check.setImageResource(
                if (done) R.drawable.ic_check_circle else R.drawable.ic_circle_outline
            )

            row.findViewById<TextView>(R.id.rowText).text = "${habit.emoji}  ${habit.title}"
            row.findViewById<TextView>(R.id.rowBadge).text =
                if (habit.target > 1) "$count/${habit.target}" else if (done) "✓" else ""
            row.alpha = if (done) 0.55f else 1f

            val tap = View.OnClickListener {
                Store.toggleHabit(context, habit)
                bind()
            }
            row.setOnClickListener(tap)
            check.setOnClickListener(tap)
            box.addView(row)
        }
    }

    // ---------------- Yaklaşan geri sayımlar ----------------

    private fun bindEvents(view: View, context: Context) {
        val box = view.findViewById<LinearLayout>(R.id.todayEventsBox)
        val empty = view.findViewById<TextView>(R.id.todayEventsEmpty)
        box.removeAllViews()

        val upcoming = Store.upcomingEvents(context).filter { !it.isPast }.take(3)
        if (upcoming.isEmpty()) {
            empty.visibility = View.VISIBLE
            if (GorunumAyar.tekAkisKarti(context)) {
                (box.parent as? View)?.visibility = View.GONE
            }
            return
        }
        (box.parent as? View)?.visibility = View.VISIBLE
        empty.visibility = View.GONE

        val inflater = LayoutInflater.from(context)
        upcoming.forEach { event ->
            val row = inflater.inflate(R.layout.item_today_task, box, false)
            row.findViewById<ImageView>(R.id.rowCheck).visibility = View.GONE
            row.findViewById<TextView>(R.id.rowText).text = "${event.emoji}  ${event.title}"

            val left = event.daysLeft
            row.findViewById<TextView>(R.id.rowBadge).text = when {
                left > 0 -> getString(R.string.event_days_left, left)
                left == 0 -> getString(R.string.event_today)
                else -> ""
            }
            row.setOnClickListener { (activity as? MainActivity)?.openEvents() }
            box.addView(row)
        }
    }

    // ---------------- Asistan önerisi ----------------

    private fun bindTip(view: View, context: Context) {
        val tipView = view.findViewById<TextView>(R.id.todayTip)
        val actionBtn = view.findViewById<MaterialButton>(R.id.todayTipAction)

        val goal = Store.getGoalMinutes(context)
        val focus = Store.getTodayFocusMinutes(context)
        val questions = Store.getTodayQuestions(context)
        val (streak, _) = Store.streakInfo(context)
        val pendingCount = Store.loadTasks(context).count { !it.done }
        val nearest = Store.upcomingEvents(context).firstOrNull { !it.isPast }
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        // Duruma göre en anlamlı öneriyi seç
        val (text, label, action) = when {
            focus == 0 && hour < 12 -> Triple(
                getString(R.string.tip_start_morning),
                getString(R.string.today_quick_timer)
            ) { (activity as? MainActivity)?.openTimer() }

            focus == 0 -> Triple(
                getString(R.string.tip_start_any),
                getString(R.string.today_quick_timer)
            ) { (activity as? MainActivity)?.openTimer() }

            focus in 1 until goal -> Triple(
                getString(R.string.tip_keep_going, goal - focus),
                getString(R.string.today_quick_timer)
            ) { (activity as? MainActivity)?.openTimer() }

            questions == 0 -> Triple(
                getString(R.string.tip_solve_questions),
                getString(R.string.today_quick_questions)
            ) { (activity as? MainActivity)?.quickAddQuestions() }

            pendingCount > 0 -> Triple(
                getString(R.string.tip_finish_tasks, pendingCount),
                getString(R.string.today_see_all)
            ) { (activity as? MainActivity)?.openTasks() }

            Store.habitProgressToday(context).let { it.second > 0 && it.first < it.second } -> {
                val (doneH, totalH) = Store.habitProgressToday(context)
                Triple(
                    getString(R.string.tip_habits_left, totalH - doneH),
                    getString(R.string.today_habits_head)
                ) { (activity as? MainActivity)?.openHabits() }
            }

            nearest != null && nearest.daysLeft <= 30 -> Triple(
                getString(R.string.tip_event_near, nearest.title, nearest.daysLeft),
                getString(R.string.today_quick_timer)
            ) { (activity as? MainActivity)?.openTimer() }

            streak >= 3 -> Triple(
                getString(R.string.tip_streak_proud, streak),
                getString(R.string.today_quick_asistan)
            ) { (activity as? MainActivity)?.openAsistan() }

            else -> Triple(
                getString(R.string.tip_all_good),
                getString(R.string.today_quick_asistan)
            ) { (activity as? MainActivity)?.openAsistan() }
        }

        tipView.text = text
        actionBtn.text = label
        actionBtn.setOnClickListener { action() }
    }

    /**
     * v7.58: Üstten aşağı çekince yenileme.
     * onViewCreated yerine onStart'ta kuruluyor — görünüm ağacı
     * o an ebeveynine bağlanmış oluyor, sarmalama güvenli.
     */
    override fun onStart() {
        super.onStart()
        Yenileyici.kur(this) { refresh() }
        Yenileyici.gorunurluguEsitle(this)
    }

    // ═══════════════════════════════════════════════════════════════
    // v11.08: BUGÜN EKRANINDAN VERİ / KART TAŞIMA VE BOYUT / SIRA YÖNETİMİ
    // ═══════════════════════════════════════════════════════════════
    private fun bugunKartlariniKur(kok: View) {
        val bloklar = listOf(
            Pair(kok.findViewById<View>(R.id.blokBugunSimdi), "☀️ Şimdi Ne Yapmalı?"),
            Pair(kok.findViewById<View>(R.id.blokBugunNamaz), "🕌 Vakit Planı & Namaz"),
            Pair(kok.findViewById<View>(R.id.blokBugunDurum), "📊 Günlük Durum"),
            Pair(kok.findViewById<View>(R.id.blokBugunGorevler), "✅ Görevler"),
            Pair(kok.findViewById<View>(R.id.blokBugunAliskanlik), "🌱 Alışkanlıklar"),
            Pair(kok.findViewById<View>(R.id.blokBugunEtkinlik), "📅 Etkinlikler"),
            Pair(kok.findViewById<View>(R.id.blokBugunIpucu), "💡 Günlük İpucu"),
            Pair(kok.findViewById<View>(R.id.blokBugunHizli), "⚡ Hızlı Komutlar")
        )
        bloklar.forEach { (v, ad) ->
            v?.setOnLongClickListener {
                bugunKartMenusu(ad, v)
                true
            }
        }
        bugunSiraVeTasinanUygula(kok)
    }

    private fun bugunKartMenusu(kartAdi: String, view: View) {
        val ctx = context ?: return
        val secenekler = arrayOf(
            "⬆️ Bu Kartı Yukarı Taşı (Günün Akışında Sırayı Değiştir)",
            "⬇️ Bu Kartı Aşağı Taşı (Günün Akışında Sırayı Değiştir)",
            "⚡ Bu Kartı / Veriyi Başka Bir Sekmeye Taşı veya Kopyala (Ana Ekran ⇄ Konular ⇄ İlerleme ⇄ Plan)",
            "📐 Kart Boyutu Ölçeğini Değiştir (${GorunumAyar.kartBoyutuAd(ctx)})",
            "✨ Günün Akışı Sıralamasını Varsayılana Sıfırla"
        )
        com.google.android.material.dialog.MaterialAlertDialogBuilder(ctx)
            .setTitle("🔀 '$kartAdi' — Kart & Sekme Yönetimi")
            .setItems(secenekler) { _, idx ->
                when (idx) {
                    0 -> kartSirasiDegistir(view, true)
                    1 -> kartSirasiDegistir(view, false)
                    2 -> SekmeVeVeriTasimaMotoru.sekmeArasiTasimaDiyalogu(ctx, "today", kartAdi, "Günün Akışı Kartı / İçeriği") {
                        view.rootView?.let { bugunSiraVeTasinanUygula(it) }
                    }
                    3 -> kartBoyutuDegistirDiyalogu()
                    4 -> {
                        SekmeVeVeriTasimaMotoru.siraSifirla(ctx, "today")
                        view.rootView?.let { bugunSiraVeTasinanUygula(it) }
                        android.widget.Toast.makeText(ctx, "✨ Günün Akışı sıralaması sıfırlandı!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    private fun kartSirasiDegistir(view: View, yukariMi: Boolean) {
        val ebeveyn = view.parent as? android.view.ViewGroup ?: return
        val pos = ebeveyn.indexOfChild(view)
        val hedefPos = if (yukariMi) pos - 1 else pos + 1
        if (hedefPos in 0 until ebeveyn.childCount) {
            ebeveyn.removeViewAt(pos)
            ebeveyn.addView(view, hedefPos)
            val idler = mutableListOf<String>()
            for (i in 0 until ebeveyn.childCount) {
                val c = ebeveyn.getChildAt(i)
                if (c.id != View.NO_ID) idler.add(c.id.toString())
            }
            SekmeVeVeriTasimaMotoru.siralamaKaydet(requireContext(), "today", idler)
            android.widget.Toast.makeText(requireContext(), "🔀 Kart sırası değiştirildi!", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            android.widget.Toast.makeText(requireContext(), "⚠️ Daha fazla taşınamaz.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun kartBoyutuDegistirDiyalogu() {
        val ctx = context ?: return
        val secenekler = arrayOf("Kompakt (%85)", "Normal (%100)", "Geniş (%115)", "Devasa (%130)")
        com.google.android.material.dialog.MaterialAlertDialogBuilder(ctx)
            .setTitle("📐 Günün Akışı & Kart Boyutu Seç")
            .setItems(secenekler) { _, idx ->
                GorunumAyar.setKartBoyutuOlcegi(ctx, idx)
                view?.let { bugunSiraVeTasinanUygula(it) }
                android.widget.Toast.makeText(ctx, "📐 Kart boyutu '${secenekler[idx]}' olarak ayarlandı!", android.widget.Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun bugunSiraVeTasinanUygula(kok: View) {
        val ctx = context ?: return
        val blokSimdi = kok.findViewById<View>(R.id.blokBugunSimdi)
        val ebeveyn = blokSimdi?.parent as? android.view.ViewGroup ?: return
        SekmeVeVeriTasimaMotoru.sekmeTasinanVerileriCiz(ctx, "today", ebeveyn)
        SekmeVeVeriTasimaMotoru.siralamayiVeBoyutuUygula(ctx, "today", ebeveyn)
    }
}
