package com.gunlukasistan.app

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TasksFragment : Fragment(R.layout.fragment_tasks) {

    private lateinit var adapter: TasksAdapter
    private lateinit var emptyText: TextView
    private val tasks = mutableListOf<Store.Task>()

    /** v8.2: liste animasyonunu tazelemek ve jesti yeniden kurmak için. */
    private var recycler: RecyclerView? = null

    /** v8.3: zengin boş durum düzeni. */
    private var bosDurum: View? = null
    private val turkish = Locale("tr", "TR")

    // ── v7.72: filtre + coklu secim ──
    /** 0 Tumu · 1 Bugun · 2 Bu hafta · 3 Geciken · 4 Tekrarli · 5 Bitenler */
    private var filtre = 0
    private val secilenler = mutableSetOf<Long>()
    private var secimModu = false
    /** v7.74: etikete gore daraltma. Bos = tum etiketler. */
    private var etiketFiltre: String = Etiket.YOK

    /** v7.7: satır başına yeniden oluşturmamak için (performans). */
    private val rowDateFormatter = SimpleDateFormat("d MMMM EEE · HH:mm", turkish)

    // ── v7.78: resimli kanıt ──
    /** Kanıt bekleyen görev — kamera dönüşünde hangisi olduğunu bilmek için. */
    private var kanitGorevi: Store.Task? = null

    /** Çekilen fotoğrafın hedefi. */
    private var kanitUri: android.net.Uri? = null

    private val kanitKamera = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.TakePicture()
    ) { basarili: Boolean ->
        val gorev = kanitGorevi
        val uri = kanitUri
        if (basarili && gorev != null && uri != null) {
            kanitiDenetle(gorev, uri)
        } else {
            // Kullanıcı vazgeçti — görev açık kalır
            kanitGorevi = null
        }
    }

    private val kanitGaleri = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        val gorev = kanitGorevi
        if (uri != null && gorev != null) {
            // Galeriden gelen dosyayı kendi klasörümüze kopyala:
            // içerik adresi kalıcı değil, geçmişte gösteremeyiz.
            val hedef = kanitiKopyala(uri, gorev.id)
            if (hedef != null) kanitiDenetle(gorev, hedef)
            else Toast.makeText(requireContext(), R.string.kn_foto_okunamadi, Toast.LENGTH_LONG).show()
        } else {
            kanitGorevi = null
        }
    }

    private val kanitIzni = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { verildi: Boolean ->
        val gorev = kanitGorevi
        if (verildi && gorev != null) kanitKamerasiniAc(gorev)
        else Toast.makeText(requireContext(), R.string.ocr_camera_denied, Toast.LENGTH_LONG).show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // v8.6 · Öneri 29: yatay/tablet genişlik sınırı + yoğunluk
        Duzen.uygula(view)
        view.findViewById<TextView>(R.id.headerText).setText(R.string.tab_tasks)
        emptyText = view.findViewById(R.id.emptyText)
        emptyText.setText(R.string.tasks_empty)
        // v8.3 · Öneri 11: boş ekran artık tek satır gri yazı değil —
        // çizim + başlık + yol gösteren düğme.
        bosDurum = BosEkran.kur(
            emptyText, BosEkran.Tur.GOREV,
            getString(R.string.be_gorev_baslik),
            getString(R.string.be_gorev_aciklama),
            getString(R.string.be_gorev_eylem)
        ) { showTaskEditor() }

        val recycler = view.findViewById<RecyclerView>(R.id.recycler)
        adapterKur(recycler)
        recycler.layoutManager = LinearLayoutManager(requireContext())

        // v10.36 · Katalog #19: kompakt ⇄ kart görünümü geçişi
        val tg = view.findViewById<TextView>(R.id.gorunumToggle)
        fun gorunumYaz() {
            tg?.text = getString(
                if (GorevGorunum.kompaktMi(requireContext())) {
                    R.string.w35_gorunum_kart
                } else {
                    R.string.w35_gorunum_kompakt
                }
            )
        }
        gorunumYaz()
        tg?.setOnClickListener {
            Titresim.dokunus(it)
            GorevGorunum.degistir(requireContext())
            gorunumYaz()
            adapterKur(recycler)
        }

        // v10.38 · Katalog #13: bugün bitecekleri üstte sabitleme seçeneği
        val bu = view.findViewById<TextView>(R.id.tiBugunUstte)
        fun bugunUstteYaz() {
            bu?.text = getString(
                if (GorevBugunUstte.acikMi(requireContext())) {
                    R.string.w38_bugun_acik
                } else {
                    R.string.w38_bugun_kapali
                }
            )
        }
        bugunUstteYaz()
        bu?.setOnClickListener {
            val acik = GorevBugunUstte.acKapa(requireContext())
            bugunUstteYaz()
            android.widget.Toast.makeText(
                requireContext(),
                if (acik) R.string.w38_bugun_acildi else R.string.w38_bugun_kapatildi,
                android.widget.Toast.LENGTH_SHORT
            ).show()
            reload()
        }

        // v8.2 · Öneri 5: liste öğeleri sırayla belirsin
        Canlandir.liste(recycler)

        // v8.2 · Öneri 4: kaydırma jesti.
        //
        // Eskiden bir görevi silmek beş adımdı: uzun bas → menü →
        // "Sil" → onay penceresi → "Sil". Artık sola kaydır yeter,
        // yanlış olursa Snackbar'dan geri alınıyor. Onay penceresi
        // KOYMADIK; koysaydık kaydırmanın bütün hızı giderdi.
        this.recycler = recycler
        kaydirmayiKur(recycler)

        // v7.72: filtre cipleri ve toplu islem seridi
        filtreleriKur(view)
        topluIslemleriKur(view)
    }

    /** v8.2: sola kaydır = sil · sağa kaydır = tamamla/geri al. */
    private fun kaydirmayiKur(recycler: RecyclerView) {
        Kaydirma.kur(
            recycler,
            Kaydirma.silTamamla(recycler),
            sola = { pos ->
                val task = tasks.getOrNull(pos)
                if (task == null) {
                    adapter.notifyDataSetChanged()
                } else {
                    runCatching { AlarmScheduler.cancel(requireContext(), task.id) }
                    Store.deleteTaskUndoable(requireContext(), task.id)
                    reload()
                    geriAlSun(getString(R.string.ga_gorev_silindi))
                }
            },
            saga = { pos ->
                val task = tasks.getOrNull(pos)
                if (task == null) {
                    adapter.notifyDataSetChanged()
                } else {
                    // Kanıt gerekiyorsa kaydırma tamamlamaz; kartı geri
                    // koyup normal akışa yönlendiriyoruz.
                    if (!task.done && Kanit.gerekliMi(requireContext(), task)) {
                        adapter.notifyItemChanged(pos)
                        Titresim.ret(view)
                        kanitIste(task)
                    } else {
                        tamamlamaYap(task)
                        Titresim.dogru(context)
                    }
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        reload()
        view?.let {
            GlassmorphismTemaMotoru.sekmeleriVeKartlariStille(it, requireContext())
            val recycler = it.findViewById<RecyclerView>(R.id.recycler)
            if (recycler != null) {
                val ebeveyn = recycler.parent as? android.view.ViewGroup
                SekmeVeVeriTasimaMotoru.sekmeTasinanVerileriCiz(requireContext(), "tasks", ebeveyn)
            }
        }
    }

    private fun reload() {
        // v8.9 · Öneri 17: değişmeden önceki hâli sakla ki DiffUtil
        // yalnız değişen satırı bildirebilsin. Kopya şart — `tasks`
        // birazdan yerinde temizleniyor.
        val eskiListe = tasks.toList()
        tasks.clear()
        // Tamamlanmamış görevler üstte; hatırlatıcısı yaklaşanlar en başta.
        // v7.75: arsivdekiler ana listede gorunmez
        val loaded = Store.aktifGorevler(requireContext())
        // v10.38 · Katalog #13: "bugün bitecek" üstte sabitleme seçeneği
        val bugunUstte = GorevBugunUstte.acikMi(requireContext())
        val bugunBitis = if (bugunUstte) {
            GorevBugunUstte.bugunAraligi(System.currentTimeMillis()).second
        } else {
            0L
        }
        val karsilastirici: Comparator<Store.Task> = if (bugunUstte) {
            compareBy(
                { it.done },
                { GorevBugunUstte.oncelik(it.dueAt, bugunBitis) },
                { if (it.dueAt > 0) it.dueAt else Long.MAX_VALUE },
                { it.createdAt }
            )
        } else {
            compareBy(
                { it.done },
                { if (it.dueAt > 0) it.dueAt else Long.MAX_VALUE },
                { it.createdAt }
            )
        }
        tasks.addAll(filtrele(loaded).sortedWith(karsilastirici))
        // v8.9 · Öneri 17: notifyDataSetChanged yerine hedefli güncelleme.
        //
        // Eskiden tek bir görev işaretlendiğinde 200 satırlık liste
        // baştan çiziliyor, kaydırma konumu sıçrıyordu. Artık yalnız
        // değişen satır yeniden bağlanıyor ve ekleme/silme/taşıma
        // animasyonları bedava geliyor.
        ListeFark.gorevler(adapter, eskiListe, tasks)
        // v8.2 · Öneri 5: ilk yüklemede giriş animasyonu oynasın.
        // Artımlı güncellemede oynatmıyoruz — DiffUtil'in kendi
        // animasyonuyla çakışır ve liste titrer.
        if (eskiListe.isEmpty()) Canlandir.tekrarOynat(recycler)
        // v8.3: filtreliyken zengin boş durum yerine sade uyarı göster —
        // "ilk görevini ekle" demek yanlış olur, görev var ama süzülmüş.
        val bosMu = tasks.isEmpty()
        if (filtre == 0 && etiketFiltre.isBlank()) {
            emptyText.visibility = View.GONE
            BosEkran.goster(bosDurum, bosMu)
        } else {
            BosEkran.goster(bosDurum, false)
            emptyText.visibility = if (bosMu) View.VISIBLE else View.GONE
            emptyText.setText(R.string.fl_bos_filtre)
        }
        // v10.38 · Katalog #13: "bugün bitecek" bölüm şeridi
        runCatching {
            val serit = view?.findViewById<TextView>(R.id.tiBugunSerit)
            if (serit != null) {
                val acik = GorevBugunUstte.acikMi(requireContext())
                val b1 = GorevBugunUstte.bugunAraligi(System.currentTimeMillis()).second
                val adet = loaded.count { !it.done && GorevBugunUstte.oncelik(it.dueAt, b1) == 0 }
                serit.visibility = if (acik && adet > 0) View.VISIBLE else View.GONE
                serit.text = getString(R.string.w38_bugun_serit, adet)
            }
            // v10.38 · Katalog #18: haftalık en çok ertelenen uyarısı
            val ert = view?.findViewById<TextView>(R.id.tiErtelemeSerit)
            if (ert != null) {
                val vuran = GorevErteleme.enCokErilenen(requireContext(), loaded)
                if (vuran != null) {
                    ert.visibility = View.VISIBLE
                    ert.text = getString(R.string.w38_erteleme_serit, vuran.first.text, vuran.second)
                } else {
                    ert.visibility = View.GONE
                }
            }
        }

        arsivSeridiTazele()
        // Secili ama artik gorunmeyen gorevleri secimden dus
        val gorunen = tasks.map { it.id }.toSet()
        if (secilenler.retainAll(gorunen)) seridiTazele()
        filtreCipleriniTazele()
    }

    /**
     * v7.72 — Filtre uygular.
     *
     * "Bitenler" disindaki tum filtreler tamamlanmislari gizler; boylece
     * gunluk kullanimda liste temiz kalir.
     */
    private fun filtrele(hepsi: List<Store.Task>): List<Store.Task> {
        val simdi = System.currentTimeMillis()
        val gunSonu = gunSonuMs(0)
        val haftaSonu = gunSonuMs(7)
        // v7.74: once etiket suzgeci
        val kaynak = if (etiketFiltre.isBlank()) hepsi
        else hepsi.filter { it.etiket == etiketFiltre }
        return when (filtre) {
            1 -> kaynak.filter { !it.done && it.dueAt in 1..gunSonu }
            2 -> kaynak.filter { !it.done && it.dueAt in 1..haftaSonu }
            3 -> kaynak.filter { !it.done && it.dueAt in 1 until simdi }
            4 -> kaynak.filter { !it.done && it.tekrarliMi }
            5 -> kaynak.filter { it.done }
            else -> kaynak
        }
    }

    /** Bugunden [gunSonra] gun sonrasinin gece yarisi. */
    private fun gunSonuMs(gunSonra: Int): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, gunSonra)
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis

    // ═══════════════════════════════════════════════════════════════
    // v7.72 — FILTRE
    // ═══════════════════════════════════════════════════════════════

    private val filtreAdlari = listOf(
        R.string.fl_tumu, R.string.fl_bugun, R.string.fl_hafta,
        R.string.fl_geciken, R.string.fl_tekrarli, R.string.fl_biten
    )

    private fun filtreleriKur(kok: View) {
        val kap = kok.findViewById<LinearLayout>(R.id.filtreCipler) ?: return
        kap.removeAllViews()
        filtreAdlari.forEachIndexed { indeks, res ->
            kap.addView(cipEt(getString(res)) {
                filtre = indeks
                secimBitir()
                reload()
            })
        }
        // v7.74: etiket cipleri — dokununca o etikete daraltir,
        // tekrar dokununca daraltmayi kaldirir
        Etiket.hepsi.forEach { t ->
            kap.addView(cipEt(t.emoji) {
                etiketFiltre = if (etiketFiltre == t.kod) Etiket.YOK else t.kod
                secimBitir()
                reload()
            })
        }
        filtreCipleriniTazele()
    }

    /** Secili filtreyi vurgular, sayaci gunceller. */
    private fun filtreCipleriniTazele() {
        val kap = view?.findViewById<LinearLayout>(R.id.filtreCipler) ?: return
        val sabitAdet = filtreAdlari.size
        for (i in 0 until kap.childCount) {
            val tv = kap.getChildAt(i) as? TextView ?: continue
            if (i < sabitAdet) {
                val secili = i == filtre
                tv.text = if (secili && tasks.isNotEmpty()) {
                    getString(filtreAdlari[i]) + "  " + tasks.size
                } else {
                    getString(filtreAdlari[i])
                }
                cipBoya(tv, secili)
            } else {
                // v7.74: etiket cipleri
                val t = Etiket.hepsi.getOrNull(i - sabitAdet) ?: continue
                cipBoya(tv, etiketFiltre == t.kod)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // v7.72 — COKLU SECIM
    // ═══════════════════════════════════════════════════════════════

    private fun topluIslemleriKur(kok: View) {
        kok.findViewById<TextView>(R.id.tiKapat)?.setOnClickListener { secimBitir() }
        kok.findViewById<TextView>(R.id.tiTumu)?.setOnClickListener {
            secilenler.clear()
            secilenler.addAll(tasks.map { it.id })
            adapter.notifyDataSetChanged()
            seridiTazele()
        }
        kok.findViewById<Button>(R.id.tiTamamla)?.setOnClickListener {
            topluGuncelle { g ->
                if (!g.done) {
                    g.done = true
                    try {
                        AlarmScheduler.cancel(requireContext(), g.id)
                    } catch (e: Exception) {
                        android.util.Log.w("TasksFragment", "Alarm iptal edilemedi", e)
                    }
                }
            }
        }
        kok.findViewById<Button>(R.id.tiBugune)?.setOnClickListener {
            val hedef = tarihMs(0)
            topluGuncelle { g -> g.dueAt = hedef; g.done = false }
        }
        kok.findViewById<Button>(R.id.tiYarina)?.setOnClickListener {
            val hedef = tarihMs(1)
            secilenler.forEach { GorevErteleme.kaydet(requireContext(), it) }
            topluGuncelle { g -> g.dueAt = hedef; g.done = false }
        }
        // v10.26 (öneri #20): ileri bir güne toplu taşıma — bugün/yarınla
        // yetinmeyenler için takvimden gün seçimi (saat 09:00'a kurulur).
        kok.findViewById<Button>(R.id.tiTarihSec)?.setOnClickListener {
            gunSec { hedefMs ->
                val adet = secilenler.size
                secilenler.forEach { GorevErteleme.kaydet(requireContext(), it) }
                topluGuncelle(
                    getString(R.string.w24_toplu_tarih_msg, adet, rowDateFormatter.format(hedefMs))
                ) { g -> g.dueAt = hedefMs; g.done = false }
            }
        }
        kok.findViewById<Button>(R.id.tiTarihKaldir)?.setOnClickListener {
            topluGuncelle { g ->
                g.dueAt = 0L
                try {
                    AlarmScheduler.cancel(requireContext(), g.id)
                } catch (e: Exception) {
                    android.util.Log.w("TasksFragment", "Alarm iptal edilemedi", e)
                }
            }
        }
        kok.findViewById<Button>(R.id.tiSil)?.setOnClickListener { topluSilSor() }
        // v10.33 · Katalog #15: seçili görevleri WhatsApp'a hazır metinle paylaş
        kok.findViewById<Button>(R.id.tiPaylas)?.setOnClickListener { secilileriPaylas() }
        // v10.36 · Katalog #16: seçili görevlerde "bekliyor" bayrağı
        kok.findViewById<Button>(R.id.tiBekliyor)?.setOnClickListener { bekliyorDegistir() }
    }

    /** v10.36 · Katalog #19: yoğunluğa göre adaptör kurar. */
    private fun adapterKur(recycler: RecyclerView) {
        adapter = TasksAdapter(
            items = tasks,
            onToggle = { task ->
                // v7.72: secim modundaysa dokunmak secer, tamamlamaz
                if (secimModu) secimDegistir(task) else toggleTask(task)
            },
            onDelete = { task -> confirmDelete(task) },
            onLongPress = { task -> secimBaslat(task) },
            seciliMi = { id -> secilenler.contains(id) },
            secimModuMu = { secimModu },
            duzenKaynak = if (GorevGorunum.kompaktMi(requireContext())) {
                R.layout.item_task_compact
            } else {
                R.layout.item_task
            }
        )
        recycler.adapter = adapter
    }

    /**
     * v10.36 · Katalog #16: seçili görevlerde ⏳ "bekliyor" durumunu aç/kapat.
     * Karışık seçimde hepsi bekliyor yapılır; hepsi zaten bekliyorsa kaldırılır.
     */
    private fun bekliyorDegistir() {
        val ctx = context ?: return
        val idler = secilenler.toSet()
        if (idler.isEmpty()) return
        val kume = GorevBekliyor.kume(ctx)
        val hepsiMi = idler.all { kume.contains(it) }
        idler.forEach { GorevBekliyor.yaz(ctx, it, !hepsiMi) }
        reload()
        Toast.makeText(
            ctx,
            getString(
                if (hepsiMi) R.string.w36_bekliyor_coz else R.string.w36_bekliyor_adet,
                idler.size
            ),
            Toast.LENGTH_LONG
        ).show()
    }

    /**
     * v10.33 · Katalog #15: seçili görevler metin bloğuna çevrilip paylaşılır.
     * Biçim saf [GorevDisAktar]'da üretilir (JVM testli); tarihler burada biçimlenir.
     */
    private fun secilileriPaylas() {
        val ctx = context ?: return
        val secili = tasks.filter { secilenler.contains(it.id) }
        if (secili.isEmpty()) return
        val satirlar = secili.map { g ->
            GorevDisAktar.Satir(
                metin = g.text,
                bitti = g.done,
                tarihMetin = if (g.dueAt > 0) rowDateFormatter.format(java.util.Date(g.dueAt)) else null
            )
        }
        val metin = GorevDisAktar.metin(satirlar, getString(R.string.w33_paylas_baslik))
        runCatching {
            val niyet = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_TEXT, metin)
            }
            startActivity(
                android.content.Intent.createChooser(niyet, getString(R.string.w33_paylas_secici))
            )
        }
    }

    /** Bugunden [gunSonra] gun sonrasi saat 09:00. */
    private fun tarihMs(gunSonra: Int): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, gunSonra)
        set(Calendar.HOUR_OF_DAY, 9)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /**
     * v10.26 (öneri #20/#4): takvimden gün seçtirir; seçimi 09:00
     * damgasıyla geri çağrıya verir. Geçmiş güne taşımak anlamsız
     * olduğu için minDate bugündür.
     */
    private fun gunSec(onSec: (Long) -> Unit) {
        val simdi = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, y, m, g ->
                val hedef = Calendar.getInstance().apply {
                    set(y, m, g, 9, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                onSec(hedef)
            },
            simdi.get(Calendar.YEAR),
            simdi.get(Calendar.MONTH),
            simdi.get(Calendar.DAY_OF_MONTH)
        ).apply { datePicker.minDate = System.currentTimeMillis() - 60_000 }.show()
    }

    /**
     * v10.26 (öneri #4): vadesi geçmiş görevin kırmızı rozetine
     * dokununca açılan hızlı erteleme. Liste yeniden yüklenmez;
     * satırda gösterilen görev üzerinden tek adımda taşınır.
     */
    private fun gecikenDiyalog(task: Store.Task) {
        val secenekler = arrayOf(
            getString(R.string.w24_bugune_al),
            getString(R.string.w24_yarina_at),
            getString(R.string.w24_tarih_sec)
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.w24_geciken_baslik)
            .setItems(secenekler) { _, hangi ->
                when (hangi) {
                    0 -> goreviErtele(task, tarihMs(0))
                    1 -> goreviErtele(task, tarihMs(1))
                    else -> gunSec { hedef -> goreviErtele(task, hedef) }
                }
            }
            .show()
    }

    /**
     * v10.27 (öneri #12) — görev bitiş mikro kutlaması.
     * İki kanal da hataya kapalıdır: kutlama asla veri akışını bozmamalı.
     */
    private fun kutlaMikro() {
        runCatching { Titresim.basari(context) }
        runCatching { Kutlama.goster(this, Kutlama.TUR_YILDIZ) }
    }

    /** Görevi hedef güne taşır, alarmı yeniden kurar, geri-al sunar. */
    private fun goreviErtele(task: Store.Task, hedefMs: Long) {
        // v10.38 · Katalog #18: ileri taşıma = erteleme sayacı
        if (task.dueAt > 0L && hedefMs > task.dueAt) GorevErteleme.kaydet(requireContext(), task.id)
        val ctx = requireContext()
        Store.gorevleriGuncelleUndoable(ctx, setOf(task.id)) { g ->
            g.dueAt = hedefMs
            g.done = false
        }
        try {
            AlarmScheduler.schedule(ctx, task.id, task.text, hedefMs)
        } catch (e: Exception) {
            android.util.Log.w("TasksFragment", "Alarm kurulamadı", e)
        }
        reload()
        geriAlSun(
            getString(
                R.string.w24_ertelendi,
                if (task.text.length > 28) task.text.take(28) + "…" else task.text,
                rowDateFormatter.format(hedefMs)
            )
        )
    }

    private fun secimBaslat(gorev: Store.Task) {
        secimModu = true
        secilenler.add(gorev.id)
        adapter.notifyDataSetChanged()
        seridiTazele()
    }

    private fun secimDegistir(gorev: Store.Task) {
        if (!secilenler.remove(gorev.id)) secilenler.add(gorev.id)
        if (secilenler.isEmpty()) {
            secimBitir()
        } else {
            adapter.notifyDataSetChanged()
            seridiTazele()
        }
    }

    private fun secimBitir() {
        secimModu = false
        secilenler.clear()
        adapter.notifyDataSetChanged()
        seridiTazele()
    }

    /** Secim seridini ve islem dugmelerini gorunur/gizli yapar. */
    private fun seridiTazele() {
        val kok = view ?: return
        val gorunur = if (secimModu) View.VISIBLE else View.GONE
        kok.findViewById<View>(R.id.tiBar)?.visibility = gorunur
        kok.findViewById<View>(R.id.tiIslemler)?.visibility = gorunur
        kok.findViewById<View>(R.id.filtreKaydirma)?.visibility =
            if (secimModu) View.GONE else View.VISIBLE
        kok.findViewById<TextView>(R.id.tiSayac)?.text =
            getString(R.string.ti_secildi, secilenler.size)
    }

    /**
     * Secili gorevleri toplu gunceller.
     * Tek "geri al" adimi olusur — 20 gorevi tek dokunusla geri alabilirsin.
     */
    private fun topluGuncelle(degistir: (Store.Task) -> Unit) =
        topluGuncelle(null, degistir)

    /** v10.26: ozelMesaj verilirse geri-al seridinde jenerik yerine o görünür. */
    private fun topluGuncelle(ozelMesaj: String?, degistir: (Store.Task) -> Unit) {
        val idler = secilenler.toSet()
        if (idler.isEmpty()) return
        Store.gorevleriGuncelleUndoable(requireContext(), idler, degistir)
        val adet = idler.size
        secimBitir()
        reload()
        geriAlSun(ozelMesaj ?: getString(R.string.ti_yapildi, adet))
    }

    private fun topluSilSor() {
        val idler = secilenler.toSet()
        if (idler.isEmpty()) return
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(getString(R.string.ti_sil_sor, idler.size))
            .setPositiveButton(R.string.delete) { _, _ ->
                idler.forEach {
                    try {
                        AlarmScheduler.cancel(requireContext(), it)
                    } catch (e: Exception) {
                        android.util.Log.w("TasksFragment", "Alarm iptal edilemedi", e)
                    }
                }
                Store.deleteTasksUndoable(requireContext(), idler)
                val adet = idler.size
                secimBitir()
                reload()
                geriAlSun(getString(R.string.ti_silindi, adet))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** v7.72: Silme/toplu islem sonrasi "Geri al" seridi. */
    /**
     * v8.6 · Öneri 26 — Tek tip bildirim.
     *
     * Eskiden ham Snackbar kullanılıyordu ve **FAB'ın altında
     * kalıyordu** (v7.72'den beri "geri al" şeridi kısmen
     * görünmüyordu). `Bildir` alt menüye tutturuyor ve temaya
     * uygun renklendiriyor.
     */
    private fun geriAlSun(mesaj: String) {
        Bildir.eylemli(view, mesaj, getString(R.string.ga_geri_al)) {
            if (Store.geriAl()) {
                reload()
                Bildir.basari(view, getString(R.string.ga_geri_alindi))
            }
        }
    }

    // ---------------- Görev ekleme (tarih + saat + alarm) ----------------

    fun showTaskEditor(mevcutGorev: Store.Task? = null) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_task, null)
        val input = dialogView.findViewById<EditText>(R.id.inputTask)
        val btnDate = dialogView.findViewById<Button>(R.id.btnDate)
        val btnTime = dialogView.findViewById<Button>(R.id.btnTime)
        val dueSummary = dialogView.findViewById<TextView>(R.id.dueSummary)
        val btnClear = dialogView.findViewById<TextView>(R.id.btnClearDue)

        val dueCal = Calendar.getInstance()
        var dateSet = false
        var timeSet = false
        // v7.70: secili tekrar kodu
        var tekrarKodu = Tekrar.YOK

        if (mevcutGorev != null) {
            input.setText(mevcutGorev.text)
            input.setSelection(input.text.length)
            if (mevcutGorev.dueAt > 0L) {
                dueCal.timeInMillis = mevcutGorev.dueAt
                dateSet = true
                timeSet = true
            }
            tekrarKodu = mevcutGorev.tekrar
        }

        val dateFmt = SimpleDateFormat("d MMMM EEEE", turkish)
        val timeFmt = SimpleDateFormat("HH:mm", Locale.US)

        fun refreshSummary() {
            if (!dateSet && !timeSet) {
                dueSummary.setText(R.string.task_no_reminder)
                btnClear.visibility = View.GONE
                return
            }
            val sb = StringBuilder("⏰ ")
            sb.append(if (dateSet) dateFmt.format(dueCal.time) else getString(R.string.task_today))
            if (timeSet) sb.append(" · ").append(timeFmt.format(dueCal.time))
            dueSummary.text = sb.toString()
            btnClear.visibility = View.VISIBLE
        }

        btnDate.setOnClickListener {
            val now = Calendar.getInstance()
            val picker = DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    dueCal.set(Calendar.YEAR, year)
                    dueCal.set(Calendar.MONTH, month)
                    dueCal.set(Calendar.DAY_OF_MONTH, day)
                    dateSet = true
                    refreshSummary()
                },
                now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)
            )
            picker.datePicker.minDate = System.currentTimeMillis() - 60_000
            picker.show()
        }

        btnTime.setOnClickListener {
            val now = Calendar.getInstance()
            TimePickerDialog(
                requireContext(),
                { _, hour, minute ->
                    dueCal.set(Calendar.HOUR_OF_DAY, hour)
                    dueCal.set(Calendar.MINUTE, minute)
                    dueCal.set(Calendar.SECOND, 0)
                    timeSet = true
                    refreshSummary()
                },
                now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true
            ).show()
        }

        btnClear.setOnClickListener {
            dateSet = false
            timeSet = false
            refreshSummary()
        }

        // v7.70: tekrar cipleri
        val tekrarKap = dialogView.findViewById<LinearLayout>(R.id.tekrarCipler)
        val tekrarOzet = dialogView.findViewById<TextView>(R.id.tekrarOzet)
        tekrarKurulumu(tekrarKap, tekrarOzet, Tekrar.YOK) { yeni -> tekrarKodu = yeni }

        val baslikMetin = if (mevcutGorev != null) "✏️ Görevi Detaylıca Düzenle" else getString(R.string.new_task)
        val butonMetin = if (mevcutGorev != null) "💾 Güncelle" else getString(R.string.add)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(baslikMetin)
            .setView(dialogView)
            .setPositiveButton(butonMetin) { _, _ ->
                val text = input.text.toString().trim()
                if (text.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.empty_task_warning, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (!dateSet && timeSet) {
                    val now = System.currentTimeMillis()
                    if (dueCal.timeInMillis <= now) {
                        dueCal.add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
                // v11.07: Alarm/saat zorunluluğu yoktur. Tarih seçilmemişse dueAt = 0L.
                val dueAt = if (dateSet || timeSet) dueCal.timeInMillis else 0L

                if (mevcutGorev != null) {
                    val idx = tasks.indexOfFirst { it.id == mevcutGorev.id }
                    val updated = mevcutGorev.copy(
                        text = text,
                        dueAt = dueAt,
                        tekrar = tekrarKodu
                    )
                    if (idx >= 0) {
                        tasks[idx] = updated
                    } else {
                        tasks.add(updated)
                    }
                    Store.saveTasks(requireContext(), tasks)
                    if (dueAt > 0) {
                        AlarmScheduler.schedule(requireContext(), updated.id, updated.text, dueAt)
                        Toast.makeText(requireContext(), R.string.task_alarm_set, Toast.LENGTH_SHORT).show()
                    } else {
                        AlarmScheduler.cancel(requireContext(), updated.id)
                        Toast.makeText(requireContext(), "💾 Görev güncellendi (alarmsız)", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val task = Store.Task(
                        id = System.currentTimeMillis(),
                        text = text,
                        done = false,
                        createdAt = System.currentTimeMillis(),
                        dueAt = dueAt,
                        tekrar = tekrarKodu
                    )
                    tasks.add(task)
                    Store.saveTasks(requireContext(), tasks)
                    if (dueAt > 0) {
                        AlarmScheduler.schedule(requireContext(), task.id, task.text, dueAt)
                        Toast.makeText(requireContext(), R.string.task_alarm_set, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "💾 Yeni görev eklendi (alarmsız)", Toast.LENGTH_SHORT).show()
                    }
                }
                reload()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * v11.07: Göreve basılı tutunca sekmeler arası taşıma, sıra değiştirme ve düzenleme menüsü açılır.
     */
    private fun gorevUzuMenu(task: Store.Task, position: Int) {
        val secenekler = arrayOf(
            "✏️ Görevi Detaylıca Düzenle",
            "⬆️ Görevi Tabloda Yukarı Taşı (Sırayı Değiştir)",
            "⬇️ Görevi Tabloda Aşağı Taşı (Sırayı Değiştir)",
            "🏷️ Etiket / Kategori Tablosuna Taşı (Sekmeler Arası Değişiklik)",
            "⏳ 'Bekliyor' Durumuna Taşı / Kaldır",
            "☑ Çoklu Seçim Modunu Başlat",
            "🗑️ Görevi Sil"
        )
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("⚡ Görev Yönetimi & Sekmeler Arası Taşıma")
            .setItems(secenekler) { _, idx ->
                when (idx) {
                    0 -> showTaskEditor(task)
                    1 -> gorevSirasiDegistir(position, position - 1)
                    2 -> gorevSirasiDegistir(position, position + 1)
                    3 -> etiketDegistirDiyalogu(task)
                    4 -> {
                        GorevBekliyor.yaz(requireContext(), task.id, !GorevBekliyor.bekliyorMu(requireContext(), task.id))
                        reload()
                        Toast.makeText(requireContext(), "⚡ Görev durumu güncellendi!", Toast.LENGTH_SHORT).show()
                    }
                    5 -> secimBaslat(task)
                    6 -> confirmDelete(task)
                }
            }
            .show()
    }

    private fun gorevSirasiDegistir(fromPos: Int, toPos: Int) {
        if (fromPos !in 0 until tasks.size || toPos !in 0 until tasks.size) {
            Toast.makeText(requireContext(), "⚠️ Daha fazla taşınamaz.", Toast.LENGTH_SHORT).show()
            return
        }
        val tutulan = tasks.removeAt(fromPos)
        tasks.add(toPos, tutulan)
        Store.saveTasks(requireContext(), tasks)
        reload()
        Toast.makeText(requireContext(), "🔀 Görevin sırası değiştirildi!", Toast.LENGTH_SHORT).show()
    }

    private fun etiketDegistirDiyalogu(task: Store.Task) {
        val etiketler = arrayOf("💼 İş", "🏠 Kişisel", "📚 Ders & Eğitim", "🚀 Proje", "🏷️ Etiketsiz")
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("🏷️ Hangi Kategori / Sekmeye Taşıyacaksınız?")
            .setItems(etiketler) { _, idx ->
                val yeniEtiket = if (idx == 4) "" else etiketler[idx].substringAfter(" ")
                val pos = tasks.indexOfFirst { it.id == task.id }
                if (pos >= 0) {
                    tasks[pos] = task.copy(etiket = yeniEtiket)
                    Store.saveTasks(requireContext(), tasks)
                    reload()
                    Toast.makeText(requireContext(), "⚡ Görev '$yeniEtiket' sekmesine / tablosuna taşındı!", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    /**
     * v7.70 — Tekrar cipleri.
     *
     * Cip listesi: Yok · Her gun · Her hafta · 2 haftada bir · Her ay ·
     * 3 ayda bir · 6 ayda bir · Her yil · Gunler… · Ozel…
     *
     * "Gunler…" ve "Ozel…" ek pencere acar; secim yapilinca kod guncellenir.
     *
     * @param degisince secilen tekrar kodunu bildirir
     */
    private fun tekrarKurulumu(
        kap: LinearLayout,
        ozet: TextView,
        baslangic: String,
        degisince: (String) -> Unit
    ) {
        val ctx = context ?: return
        val dp = resources.displayMetrics.density
        var secili = baslangic

        // (etiket, kod) — son iki tanesi pencere acar
        val sabitler = listOf(
            getString(R.string.tk_yok) to Tekrar.YOK,
            getString(R.string.tk_gunluk) to Tekrar.GUN,
            getString(R.string.tk_haftalik) to Tekrar.HAFTA,
            getString(R.string.tk_2hafta) to Tekrar.IKI_HAFTA,
            getString(R.string.tk_aylik) to Tekrar.AY,
            getString(R.string.tk_3ay) to Tekrar.UC_AY,
            getString(R.string.tk_6ay) to Tekrar.ALTI_AY,
            getString(R.string.tk_yillik) to Tekrar.YIL
        )

        val cipler = mutableListOf<Pair<TextView, String>>()

        fun ozetiTazele() {
            if (Tekrar.aktifMi(secili)) {
                ozet.text = getString(R.string.tk_bilgi)
                ozet.visibility = View.VISIBLE
            } else {
                ozet.visibility = View.GONE
            }
        }

        fun boya() {
            cipler.forEach { (tv, kod) ->
                val aktif = when (kod) {
                    "__gunler" -> secili.startsWith(Tekrar.ON_GUNLER)
                    "__ozel" -> secili.startsWith(Tekrar.ON_OZEL)
                    else -> secili == kod
                }
                cipBoya(tv, aktif)
                // Gunler/ozel secildiyse etiket secimi gostersin
                if (kod == "__gunler" && aktif) tv.text = Tekrar.ad(ctx, secili)
                if (kod == "__ozel" && aktif) tv.text = Tekrar.ad(ctx, secili)
            }
            ozetiTazele()
        }

        fun yap(tv: TextView, kod: String) {
            cipler.add(tv to kod)
            kap.addView(tv)
        }

        sabitler.forEach { (etiket, kod) ->
            yap(cipEt(etiket) {
                secili = kod
                degisince(secili)
                boya()
            }, kod)
        }

        // Haftanin gunleri
        yap(cipEt(getString(R.string.tk_hafta_gun)) {
            gunSecici(secili) { kod ->
                secili = kod
                degisince(secili)
                boya()
            }
        }, "__gunler")

        // Ozel aralik
        yap(cipEt(getString(R.string.tk_ozel_gun)) {
            ozelSecici { kod ->
                secili = kod
                degisince(secili)
                boya()
            }
        }, "__ozel")

        boya()
    }

    /** Tek bir cip gorunumu uretir. */
    private fun cipEt(metin: String, tikla: () -> Unit): TextView {
        val dp = resources.displayMetrics.density
        return TextView(requireContext()).apply {
            text = metin
            textSize = 12.5f
            gravity = Gravity.CENTER
            setPadding((14 * dp).toInt(), (8 * dp).toInt(), (14 * dp).toInt(), (8 * dp).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = (6 * dp).toInt() }
            isClickable = true
            setOnClickListener { tikla() }
        }
    }

    /** Cipi secili/secilmemis gorunume boyar. */
    private fun cipBoya(tv: TextView, secili: Boolean) {
        try {
            val dp = resources.displayMetrics.density
            val vurgu = com.google.android.material.color.MaterialColors.getColor(
                tv, com.google.android.material.R.attr.colorPrimary, 0
            )
            tv.background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 18 * dp
                if (secili) {
                    setColor((vurgu and 0x00FFFFFF) or 0x33000000)
                    setStroke((1.5f * dp).toInt(), vurgu)
                } else {
                    setColor(0x14888888)
                }
            }
            tv.setTextColor(
                if (secili) vurgu
                else com.google.android.material.color.MaterialColors.getColor(
                    tv, com.google.android.material.R.attr.colorOnSurface, 0
                )
            )
        } catch (e: Exception) {
            android.util.Log.w("TasksFragment", "Cip boyanamadi", e)
        }
    }

    /** Haftanin gunlerini coklu secim ile alir. */
    private fun gunSecici(mevcut: String, sonuc: (String) -> Unit) {
        val adlar = arrayOf(
            getString(R.string.tk_g_pzt), getString(R.string.tk_g_sal),
            getString(R.string.tk_g_car), getString(R.string.tk_g_per),
            getString(R.string.tk_g_cum), getString(R.string.tk_g_cmt),
            getString(R.string.tk_g_paz)
        )
        val secilenler = Tekrar.gunleriCoz(mevcut)
        val isaretli = BooleanArray(7) { secilenler.contains(it + 1) }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.tk_gun_sec)
            .setMultiChoiceItems(adlar, isaretli) { _, hangi, secildi ->
                isaretli[hangi] = secildi
            }
            .setPositiveButton(R.string.save) { _, _ ->
                val liste = (1..7).filter { isaretli[it - 1] }
                if (liste.isEmpty()) {
                    Toast.makeText(
                        requireContext(), R.string.tk_gun_gerekli, Toast.LENGTH_SHORT
                    ).show()
                } else {
                    sonuc(Tekrar.gunleriKodla(liste))
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** "Her N gunde bir" araligini alir. */
    private fun ozelSecici(sonuc: (String) -> Unit) {
        val secici = android.widget.NumberPicker(requireContext()).apply {
            minValue = 2
            maxValue = 180
            value = 10
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.tk_ozel_gun)
            .setView(secici)
            .setPositiveButton(R.string.save) { _, _ ->
                sonuc(Tekrar.ON_OZEL + secici.value)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * v7.78 — Görev tamamlanmadan önce kanıt kapısı.
     *
     * Kanıt gereken bir görev işaretleniyorsa önce fotoğraf istenir.
     * Onay gelirse [tamamlamaYap] çağrılır. Görev **açılırken** (done=false)
     * kanıt sorulmaz — geri alma her zaman serbest olmalı.
     */
    private fun toggleTask(task: Store.Task) {
        val tamamlaniyor = !task.done
        if (tamamlaniyor && Kanit.gerekliMi(requireContext(), task)) {
            kanitIste(task)
            return
        }
        tamamlamaYap(task)
    }

    /** Kanıt kontrolünden geçmiş (ya da hiç gerekmeyen) tamamlama. */
    private fun tamamlamaYap(task: Store.Task) {
        task.done = !task.done
        // v8.2 · Öneri 2: tamamlarken onay dokunuşu, geri alırken hafif tik
        if (task.done) Titresim.dogru(context) else Titresim.tik(view)
        // v10.27 (öneri #12): tamamlama anında mikro kutlama —
        // güçlü haptic + yıldız patlaması. GorunumAyar.animasyonAcik
        // kapalıysa Kutlama hiç oluşmaz (haptic'e de Titreşim karar verir).
        if (task.done) kutlaMikro()
        if (task.done) {
            Store.recordCompletion(requireContext())
            AlarmScheduler.cancel(requireContext(), task.id)
            // v7.70: tekrarli gorev silinmez, sonraki tarihe tasinir
            if (task.tekrarliMi) {
                // v7.78: tekrarli gorev yenilenince kaniti sifirla —
                // eski fotograf yeni tekrari kanitlamaz
                Kanit.tekrarIcinSifirla(requireContext(), task.id)
                val yeni = Tekrar.gorevYenile(requireContext(), task)
                if (yeni > 0) {
                    Toast.makeText(
                        requireContext(),
                        getString(
                            R.string.tk_yenilendi, task.text, Tekrar.tarihMetni(yeni)
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(), R.string.tk_bitti, Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else if (task.dueAt > System.currentTimeMillis()) {
            AlarmScheduler.schedule(requireContext(), task.id, task.text, task.dueAt)
        }
        Store.saveTasks(requireContext(), tasks)
        reload()
    }

    /**
     * v7.73 — Goreve uzun basinca / silme simgesine dokununca acilan menu.
     *
     * Tek secenek "sil" olmamali: alt adimlari duzenlemek, tekrarli
     * gorevde bu seferi atlamak da buradan yapilir.
     */
    private fun confirmDelete(task: Store.Task) {
        val ctx = requireContext()
        val ogeler = mutableListOf<AltSayfa.Oge>()

        ogeler.add(AltSayfa.Oge(
            getString(R.string.ag_yonet), simge = "☑"
        ) { adimlariYonet(task) })

        // v7.74: etiket ata/degistir
        ogeler.add(AltSayfa.Oge(
            getString(R.string.et_baslik),
            altBaslik = Etiket.ad(ctx, task.etiket),
            simge = "🏷"
        ) { etiketSec(task) })

        // v7.78: bu gorev kanit istesin mi
        val kanitli = Kanit.isaretliMi(ctx, task.id)
        ogeler.add(AltSayfa.Oge(
            getString(if (kanitli) R.string.kn_menu_kapat else R.string.kn_menu_ac),
            simge = "📷"
        ) {
            Kanit.isaretle(ctx, task.id, !kanitli)
            Toast.makeText(
                ctx,
                if (kanitli) R.string.kn_menu_kapandi else R.string.kn_menu_acildi,
                Toast.LENGTH_SHORT
            ).show()
            reload()
        })

        if (task.tekrarliMi) {
            ogeler.add(AltSayfa.Oge(getString(R.string.tk_atla), simge = "⏭") {
                tekrariAtla(task)
            })
        }

        // v7.75: bitmis gorev arsive tasinabilir
        if (task.done && !task.tekrarliMi) {
            ogeler.add(AltSayfa.Oge(getString(R.string.ars_menu), simge = "📦") {
                Store.arsiveTasi(ctx, task.id, true)
                reload()
                Toast.makeText(
                    ctx, getString(R.string.ars_tasindi, 1), Toast.LENGTH_SHORT
                ).show()
            })
        }

        // v10.14 · E29: görevi paylaşım kartına dök
        ogeler.add(AltSayfa.Oge(
            getString(R.string.ge_kart_paylas), simge = "🖼"
        ) {
            runCatching {
                val kart = KartUretici.gorevKarti(ctx, task)
                KartUretici.paylas(ctx, kart, "gorev_karti_${task.id}.png")
            }
        })

        ogeler.add(AltSayfa.Oge(
            getString(R.string.delete), simge = "🗑", yikici = true
        ) { silOnayla(task) })

        // v8.4 · Öneri 18: ortadaki pencere yerine alt sayfa.
        // Menü artık başparmağın altında açılıyor ve sürüklenerek
        // kapatılabiliyor.
        AltSayfa.menu(ctx, task.text, ogeler)
    }

    /**
     * v7.75 — Arsiv seridi.
     *
     * Arsivlenebilir (bitmis, tekrarsiz) gorev varsa "Bitenleri arsivle (3)"
     * gorunur. Arsivde kayit varsa "Arsivi ac" her zaman gorunur.
     */
    private fun arsivSeridiTazele() {
        val kok = view ?: return
        val bar = kok.findViewById<View>(R.id.arsivBar) ?: return
        val tasiBtn = kok.findViewById<TextView>(R.id.arsivTasi) ?: return
        val acBtn = kok.findViewById<TextView>(R.id.arsivAc) ?: return

        val bekleyen = Store.arsivlenebilirSayi(requireContext())
        val arsivAdet = Store.arsivGorevleri(requireContext()).size

        bar.visibility = if (bekleyen > 0 || arsivAdet > 0) View.VISIBLE else View.GONE
        tasiBtn.visibility = if (bekleyen > 0) View.VISIBLE else View.INVISIBLE
        tasiBtn.text = getString(R.string.ars_bitenleri_tasi, bekleyen)
        acBtn.visibility = if (arsivAdet > 0) View.VISIBLE else View.GONE

        tasiBtn.setOnClickListener {
            val adet = Store.bitenleriArsivle(requireContext())
            if (adet > 0) {
                reload()
                geriAlSun(getString(R.string.ars_tasindi, adet))
            }
        }
        acBtn.setOnClickListener { ArsivActivity.ac(requireContext()) }
    }

    /** v7.74: Goreve etiket atar. */
    private fun etiketSec(task: Store.Task) {
        Etiket.sec(requireContext(), task.etiket) { kod ->
            task.etiket = kod
            Store.saveTasks(requireContext(), tasks)
            reload()
        }
    }

    /** Gorevi geri alinabilir bicimde siler. */
    private fun silOnayla(task: Store.Task) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_task_title)
            .setMessage(task.text)
            .setPositiveButton(R.string.delete) { _, _ ->
                try {
                    AlarmScheduler.cancel(requireContext(), task.id)
                } catch (e: Exception) {
                    android.util.Log.w("TasksFragment", "Alarm iptal edilemedi", e)
                }
                Store.deleteTaskUndoable(requireContext(), task.id)
                reload()
                geriAlSun(getString(R.string.ga_gorev_silindi))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** v7.70: Tekrarli gorevde bu seferi atlar, sonraki tarihe gecer. */
    private fun tekrariAtla(task: Store.Task) {
        val yeni = Tekrar.sonraki(task.tekrar, task.dueAt, task.tekrarBitis)
        if (yeni > 0) {
            task.dueAt = yeni
            task.done = false
            try {
                AlarmScheduler.schedule(requireContext(), task.id, task.text, yeni)
            } catch (e: Exception) {
                android.util.Log.w("TasksFragment", "Alarm kurulamadi", e)
            }
            Toast.makeText(
                requireContext(),
                getString(R.string.tk_atlandi, Tekrar.tarihMetni(yeni)),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            task.tekrar = Tekrar.YOK
            Toast.makeText(requireContext(), R.string.tk_bitti, Toast.LENGTH_SHORT).show()
        }
        Store.saveTasks(requireContext(), tasks)
        reload()
    }

    // ═══════════════════════════════════════════════════════════════
    // v7.73 — ALT ADIMLAR (checklist)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Gorevin alt adimlarini yoneten pencere.
     *
     * Adimlar dokununca isaretlenir, uzun basinca silinir.
     * Tum adimlar bitince gorev de otomatik tamamlanir — kullanicinin
     * ayrica ana gorevi isaretlemesi gerekmez.
     */
    private fun adimlariYonet(task: Store.Task) {
        val ctx = requireContext()
        val dp = resources.displayMetrics.density

        val kap = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((20 * dp).toInt(), (8 * dp).toInt(), (20 * dp).toInt(), 0)
        }
        val liste = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val girdi = EditText(ctx).apply {
            hint = getString(R.string.ag_hint)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            maxLines = 2
        }

        fun cizListe() {
            liste.removeAllViews()
            if (task.adimlar.isEmpty()) {
                liste.addView(TextView(ctx).apply {
                    text = getString(R.string.ag_bos)
                    textSize = 12.5f
                    alpha = 0.6f
                    setPadding(0, (8 * dp).toInt(), 0, (8 * dp).toInt())
                })
                return
            }
            task.adimlar.forEach { adim ->
                liste.addView(
                    LinearLayout(ctx).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(0, (8 * dp).toInt(), 0, (8 * dp).toInt())
                        background = android.graphics.drawable.RippleDrawable(
                            android.content.res.ColorStateList.valueOf(0x22888888), null, null
                        )
                        isClickable = true
                        setOnClickListener {
                            adim.done = !adim.done
                            Store.saveTasks(ctx, tasks)
                            cizListe()
                        }
                        setOnLongClickListener {
                            MaterialAlertDialogBuilder(ctx)
                                .setMessage(R.string.ag_sil_sor)
                                .setPositiveButton(R.string.delete) { _, _ ->
                                    task.adimlar.remove(adim)
                                    Store.saveTasks(ctx, tasks)
                                    cizListe()
                                }
                                .setNegativeButton(R.string.cancel, null)
                                .show()
                            true
                        }
                        addView(TextView(ctx).apply {
                            text = if (adim.done) "☑" else "☐"
                            textSize = 17f
                            setPadding(0, 0, (10 * dp).toInt(), 0)
                            if (adim.done) setTextColor(GrafikDili.BASARI)
                        })
                        addView(TextView(ctx).apply {
                            text = adim.text
                            textSize = 14f
                            if (adim.done) {
                                paintFlags = paintFlags or
                                    android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                                alpha = 0.55f
                            }
                            layoutParams = LinearLayout.LayoutParams(
                                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                            )
                        })
                    }
                )
            }
        }

        cizListe()
        kap.addView(liste)
        kap.addView(girdi)

        MaterialAlertDialogBuilder(ctx)
            .setTitle(getString(R.string.ag_baslik))
            .setView(android.widget.ScrollView(ctx).apply { addView(kap) })
            .setPositiveButton(R.string.ag_ekle, null)
            .setNegativeButton(R.string.done) { _, _ -> adimlariBitir(task) }
            .show()
            .also { pencere ->
                // Pencere kapanmasin — arka arkaya adim eklenebilsin
                pencere.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
                    ?.setOnClickListener {
                        val metin = girdi.text?.toString()?.trim().orEmpty()
                        if (metin.isBlank()) return@setOnClickListener
                        task.adimlar.add(
                            Store.SubItem(
                                id = System.currentTimeMillis(),
                                text = metin.take(150),
                                done = false,
                                createdAt = System.currentTimeMillis()
                            )
                        )
                        Store.saveTasks(ctx, tasks)
                        girdi.setText("")
                        cizListe()
                    }
            }
    }

    /** Pencere kapaninca: tum adimlar bittiyse gorevi de tamamla. */
    private fun adimlariBitir(task: Store.Task) {
        if (task.adimlar.isNotEmpty() &&
            task.adimlar.all { it.done } &&
            !task.done
        ) {
            task.done = true
            Store.recordCompletion(requireContext())
            try {
                AlarmScheduler.cancel(requireContext(), task.id)
            } catch (e: Exception) {
                android.util.Log.w("TasksFragment", "Alarm iptal edilemedi", e)
            }
            if (task.tekrarliMi) Tekrar.gorevYenile(requireContext(), task)
            Store.saveTasks(requireContext(), tasks)
            Toast.makeText(requireContext(), R.string.ag_tumu_bitti, Toast.LENGTH_SHORT).show()
        }
        reload()
    }

    inner class TasksAdapter(
        private val items: List<Store.Task>,
        private val onToggle: (Store.Task) -> Unit,
        private val onDelete: (Store.Task) -> Unit,
        // v7.72: coklu secim geri cagrilari
        private val onLongPress: (Store.Task) -> Unit = {},
        private val seciliMi: (Long) -> Boolean = { false },
        private val secimModuMu: () -> Boolean = { false },
        // v10.36 · Katalog #19: şişirilecek satır düzeni (kart/kompakt)
        private val duzenKaynak: Int = R.layout.item_task
    ) : RecyclerView.Adapter<TasksAdapter.TaskViewHolder>() {

        inner class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val checkBox: CheckBox = view.findViewById(R.id.taskCheckBox)
            val text: TextView = view.findViewById(R.id.taskText)
            val due: TextView = view.findViewById(R.id.dueText)
            val delete: ImageButton = view.findViewById(R.id.deleteButton)
            /** v7.74: sol kenardaki etiket rengi seridi. */
            val serit: TextView = view.findViewById(R.id.etiketSerit)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(duzenKaynak, parent, false)
            return TaskViewHolder(view)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
            val task = items[position]

            holder.checkBox.setOnCheckedChangeListener(null)
            holder.checkBox.isChecked = task.done

            // v10.36 · Katalog #16: "bekliyor" görevler ⏳ rozetli
            holder.text.text = GorevBekliyor.rozetliMetin(
                task.text, GorevBekliyor.bekliyorMu(holder.itemView.context, task.id)
            )
            holder.text.paintFlags = if (task.done) {
                holder.text.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                holder.text.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
            holder.text.alpha = if (task.done) 0.45f else 1.0f
            // v10.26: ViewHolder geri dönüşür — eski erteleme dinleyicisi kalmasın
            holder.due.setOnClickListener(null)

            // Tarih/saat rozeti
            if (task.dueAt > 0) {
                val now = System.currentTimeMillis()
                val overdue = task.dueAt < now && !task.done
                val fmt = rowDateFormatter
                // v7.73: alt adim ilerlemesi
                val adimEk = if (task.adimlar.isNotEmpty()) {
                    "  ☑ " + task.bitenAdim + "/" + task.adimlar.size
                } else ""
                // v7.70: tekrarli gorevlerde rozete 🔁 ve tekrar adi eklenir
                val tekrarEk = if (task.tekrarliMi) {
                    "  " + holder.due.context.getString(
                        R.string.tk_rozet, Tekrar.ad(holder.due.context, task.tekrar)
                    )
                } else ""
                holder.due.text =
                    (if (overdue) "⚠️ " else "⏰ ") + fmt.format(task.dueAt) +
                        tekrarEk + adimEk
                val colorAttr = if (overdue) {
                    com.google.android.material.R.attr.colorError
                } else {
                    com.google.android.material.R.attr.colorOnSurfaceVariant
                }
                holder.due.setTextColor(
                    MaterialColors.getColor(holder.due.context, colorAttr, 0xFF888888.toInt())
                )
                holder.due.visibility = View.VISIBLE
                // v10.26 (öneri #4): gecikmiş rozete dokun → hızlı erteleme diyaloğu
                if (overdue) holder.due.setOnClickListener { gecikenDiyalog(task) }
            } else if (task.tekrarliMi || task.adimlar.isNotEmpty()) {
                // v7.70/v7.73: tarihi olmayan ama tekrarli veya adimli gorev
                holder.due.text = buildString {
                    if (task.tekrarliMi) {
                        append(
                            holder.due.context.getString(
                                R.string.tk_rozet,
                                Tekrar.ad(holder.due.context, task.tekrar)
                            )
                        )
                    }
                    if (task.adimlar.isNotEmpty()) {
                        if (isNotEmpty()) append("  ")
                        append("☑ ").append(task.bitenAdim).append("/")
                            .append(task.adimlar.size)
                    }
                }
                holder.due.setTextColor(
                    MaterialColors.getColor(
                        holder.due.context,
                        com.google.android.material.R.attr.colorOnSurfaceVariant,
                        0xFF888888.toInt()
                    )
                )
                holder.due.visibility = View.VISIBLE
            } else {
                holder.due.visibility = View.GONE
            }

            // v7.74: etiket rengi seridi
            holder.serit.setBackgroundColor(Etiket.renk(task.etiket))

            // v7.72: secim modunda satir vurgusu ve gizlenen silme dugmesi
            val secimAcik = secimModuMu()
            val secili = seciliMi(task.id)
            holder.itemView.setBackgroundColor(
                if (secili) {
                    (MaterialColors.getColor(
                        holder.itemView,
                        com.google.android.material.R.attr.colorPrimary, 0
                    ) and 0x00FFFFFF) or 0x33000000
                } else {
                    android.graphics.Color.TRANSPARENT
                }
            )
            holder.delete.visibility = if (secimAcik) View.GONE else View.VISIBLE
            holder.checkBox.isChecked = if (secimAcik) secili else task.done

            holder.checkBox.setOnCheckedChangeListener { _, _ -> onToggle(task) }
            holder.itemView.setOnClickListener { showTaskEditor(task) }
            holder.text.setOnClickListener { showTaskEditor(task) }
            holder.itemView.setOnLongClickListener { gorevUzuMenu(task, position); true }
            holder.delete.setOnClickListener { onDelete(task) }
        }
    }

    /**
     * v7.58: Üstten aşağı çekince yenileme.
     * onViewCreated yerine onStart'ta kuruluyor — görünüm ağacı
     * o an ebeveynine bağlanmış oluyor, sarmalama güvenli.
     */
    override fun onStart() {
        super.onStart()
        Yenileyici.kur(this) { reload() }
        Yenileyici.gorunurluguEsitle(this)
    }

    // ═══════════════════════════════════════════════════════════════
    // v7.78 — RESİMLİ KANIT
    // ═══════════════════════════════════════════════════════════════

    /**
     * Kanıt gereken görev için fotoğraf ister.
     *
     * Kullanıcı vazgeçebilir — o zaman görev açık kalır. Bu bilinçli:
     * "kanıt yoksa tamamlanmasın" kuralının anlamı bu.
     */
    private fun kanitIste(gorev: Store.Task) {
        kanitGorevi = gorev

        val secenekler = arrayOf(
            getString(R.string.kn_sec_kamera),
            getString(R.string.kn_sec_galeri),
            getString(R.string.kn_sec_vazgec)
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.kn_iste_baslik))
            .setMessage(getString(R.string.kn_iste_mesaj, gorev.text))
            .setItems(secenekler) { _, hangi ->
                when (hangi) {
                    0 -> kanitIzniniKontrolEt(gorev)
                    1 -> kanitGaleri.launch("image/*")
                    else -> kanitGorevi = null
                }
            }
            .setOnCancelListener { kanitGorevi = null }
            .show()
    }

    private fun kanitIzniniKontrolEt(gorev: Store.Task) {
        val izin = android.Manifest.permission.CAMERA
        val verilmis = androidx.core.content.ContextCompat.checkSelfPermission(
            requireContext(), izin
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (verilmis) kanitKamerasiniAc(gorev) else kanitIzni.launch(izin)
    }

    private fun kanitKamerasiniAc(gorev: Store.Task) {
        try {
            val dosya = Kanit.yeniDosya(requireContext(), gorev.id)
            val uri = Kanit.uriVer(requireContext(), dosya)
            kanitUri = uri
            kanitKamera.launch(uri)
        } catch (e: Exception) {
            android.util.Log.w("TasksFragment", "Kanıt kamerası açılamadı", e)
            Toast.makeText(requireContext(), R.string.ocr_camera_fail, Toast.LENGTH_LONG).show()
            kanitGorevi = null
        }
    }

    /** Galeriden seçilen görseli kalıcı kanıt klasörüne kopyalar. */
    private fun kanitiKopyala(kaynak: android.net.Uri, gorevId: Long): android.net.Uri? = try {
        val hedef = Kanit.yeniDosya(requireContext(), gorevId)
        requireContext().contentResolver.openInputStream(kaynak)?.use { girdi ->
            hedef.outputStream().use { cikti -> girdi.copyTo(cikti) }
        }
        if (hedef.exists() && hedef.length() > 0) Kanit.uriVer(requireContext(), hedef) else null
    } catch (e: Exception) {
        android.util.Log.w("TasksFragment", "Kanıt kopyalanamadı", e)
        null
    }

    /**
     * Çekilen kanıtı denetletir ve sonuca göre görevi tamamlar.
     *
     * Onaylanırsa kanıt geçmişe yazılır ve görev tamamlanır.
     * Reddedilirse görev açık kalır (kullanıcı ayarı gevşetmediyse).
     */
    private fun kanitiDenetle(gorev: Store.Task, uri: android.net.Uri) {
        val etkinlik = activity ?: return
        KanitAkisi.denetle(etkinlik, gorev, uri) { onaylandi, tekrarCek ->
            when {
                tekrarCek -> kanitIste(gorev)
                onaylandi -> {
                    // Kanıtı geçmişe yaz — görev listeden çıksa da kalsın
                    Kanit.bul(requireContext(), gorev.id)?.let { kayit ->
                        Kanit.gecmiseAt(requireContext(), gorev.id, kayit, gorev.text)
                    }
                    kanitGorevi = null
                    tamamlamaYap(gorev)
                }
                else -> {
                    kanitGorevi = null
                    reload()
                }
            }
        }
    }
}
