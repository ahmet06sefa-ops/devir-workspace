package com.gunlukasistan.app

import android.graphics.Paint
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.color.MaterialColors
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator

/**
 * Konular ekranı: ders konuları ve alt maddeleri.
 * Her alt madde tamamlandığında işaretlenir; konu başlığının yanındaki
 * halkada yüzde kaçının bittiği gösterilir.
 */
class TopicsFragment : Fragment(R.layout.fragment_notes) {

    /** v8.3: zengin boş durum düzeni. */
    private var bosDurum: android.view.View? = null

    private lateinit var adapter: TopicsAdapter
    private lateinit var emptyText: TextView
    private val topics = mutableListOf<Store.Topic>()
    private val expandedIds = mutableSetOf<Long>()

    /** v7.19: kamerayla çekilen fotoğrafın geçici konumu. */
    private var kameraUri: android.net.Uri? = null

    /** v7.19: galeriden fotoğraf seçme. */
    private val galeriSec = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) fotografiOku(uri)
    }

    /** v7.19: kamerayla fotoğraf çekme. */
    private val kameraCek = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.TakePicture()
    ) { basarili: Boolean ->
        val uri = kameraUri
        if (basarili && uri != null) fotografiOku(uri)
    }

    /** v7.19: kamera izni istendiğinde. */
    private val kameraIzni = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { verildi: Boolean ->
        if (verildi) kamerayiAc()
        else Toast.makeText(requireContext(), R.string.ocr_camera_denied, Toast.LENGTH_LONG).show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // v8.6 · Öneri 29: yatay/tablet genişlik sınırı + yoğunluk
        Duzen.uygula(view)
        view.findViewById<TextView>(R.id.headerText).setText(R.string.tab_topics)
        emptyText = view.findViewById(R.id.emptyText)
        emptyText.setText(R.string.topics_empty)

        kpssSlot = view.findViewById(R.id.kpssSlot)
        setupSearchSlot(view)

        val recycler = view.findViewById<RecyclerView>(R.id.recycler)
        adapter = TopicsAdapter()
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        reload()
        view?.let {
            GlassmorphismTemaMotoru.sekmeleriVeKartlariStille(it, requireContext())
            val recycler = it.findViewById<RecyclerView>(R.id.recycler)
            if (recycler != null) {
                val ebeveyn = recycler.parent as? android.view.ViewGroup
                SekmeVeVeriTasimaMotoru.sekmeTasinanVerileriCiz(requireContext(), "topics", ebeveyn)
            }
        }
    }

    private fun reload() {
        // v8.9 · Öneri 17: hedefli güncelleme
        val eskiListe = topics.toList()
        topics.clear()
        topics.addAll(Store.loadTopics(requireContext()).sortedByDescending { it.createdAt })
        ListeFark.konular(adapter, eskiListe, topics)
        // v8.3 · Öneri 11
        if (bosDurum == null) {
            bosDurum = BosEkran.kur(
                emptyText, BosEkran.Tur.KONU,
                getString(R.string.be_konu_baslik),
                getString(R.string.be_konu_aciklama),
                getString(R.string.be_konu_eylem)
            ) { showTopicDialog() }
        }
        emptyText.visibility = View.GONE
        BosEkran.goster(bosDurum, topics.isEmpty())
        refreshKpssSlot()
    }

    // ---------------- Konularda arama ----------------

    private var query = ""

    /** Arama metnine uyan konular (başlıkta ya da alt maddede geçen). */
    private fun visibleTopics(): List<Store.Topic> {
        if (query.isBlank()) return topics
        val q = query.lowercase(java.util.Locale("tr", "TR"))
        return topics.filter { topic ->
            topic.title.lowercase(java.util.Locale("tr", "TR")).contains(q) ||
                topic.items.any {
                    it.text.lowercase(java.util.Locale("tr", "TR")).contains(q)
                }
        }
    }

    private fun setupSearchSlot(view: View) {
        val slot = view.findViewById<FrameLayout>(R.id.searchSlot) ?: return
        val ctx = requireContext()
        slot.visibility = View.VISIBLE
        val dp = resources.displayMetrics.density
        val input = com.google.android.material.textfield.TextInputEditText(ctx).apply {
            hint = getString(R.string.search_hint)
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }
        val wrap = com.google.android.material.textfield.TextInputLayout(ctx).apply {
            setPadding((8 * dp).toInt(), 0, (8 * dp).toInt(), 0)
            addView(input)
        }
        input.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                query = s?.toString()?.trim() ?: ""
                adapter.notifyDataSetChanged()
            }
        })
        slot.addView(wrap)
    }

    // ---------------- KPSS hazır müfredat kartı ----------------

    private var kpssSlot: FrameLayout? = null

    private fun refreshKpssSlot() {
        val slot = kpssSlot ?: return
        val ctx = context ?: return
        if (!KpssModuKararMotoru.kpssModuAktifMi(ctx) || !KpssPack.hasMissing(ctx)) {
            slot.visibility = View.GONE
            return
        }
        slot.visibility = View.VISIBLE
        slot.removeAllViews()
        val promo = layoutInflater.inflate(R.layout.item_kpss_promo, slot, false)
        promo.findViewById<TextView>(R.id.kpssPromoSub).text =
            getString(R.string.kpss_card_sub, KpssPack.subjectCount(), KpssPack.subtopicCount())
        promo.findViewById<Button>(R.id.kpssPromoBtn).setOnClickListener {
            MaterialAlertDialogBuilder(ctx)
                .setTitle(R.string.kpss_add_title)
                .setMessage(getString(R.string.kpss_add_msg, KpssPack.subtopicCount()))
                .setPositiveButton(R.string.kpss_card_btn) { _, _ ->
                    KpssPack.addMissing(ctx)
                    Toast.makeText(ctx, R.string.kpss_added_toast, Toast.LENGTH_LONG).show()
                    reload()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
        slot.addView(promo)
    }

    // ═══════════════════════════════════════════════════════════════
    // v7.19 — Fotoğraftan konu ekleme
    // ═══════════════════════════════════════════════════════════════

    /** + menüsünden çağrılır: elle mi fotoğraftan mı sorusu. */
    fun showTopicDialog() {
        val secenekler = arrayOf(
            getString(R.string.ocr_from_photo),
            getString(R.string.ocr_from_camera),
            getString(R.string.ocr_by_hand)
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.new_topic)
            .setItems(secenekler) { _, hangi ->
                when (hangi) {
                    0 -> galeriSec.launch("image/*")
                    1 -> kameraIzniIste()
                    2 -> elleKonuEkle()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun kameraIzniIste() {
        val izin = android.Manifest.permission.CAMERA
        val verilmis = androidx.core.content.ContextCompat.checkSelfPermission(
            requireContext(), izin
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (verilmis) kamerayiAc() else kameraIzni.launch(izin)
    }

    private fun kamerayiAc() {
        try {
            val klasor = java.io.File(requireContext().cacheDir, "fotograf").apply { mkdirs() }
            val dosya = java.io.File(klasor, "konu_${System.currentTimeMillis()}.jpg")
            val uri = androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                dosya
            )
            kameraUri = uri
            kameraCek.launch(uri)
        } catch (e: Exception) {
            android.util.Log.w("TopicsFragment", "Kamera açılamadı", e)
            Toast.makeText(requireContext(), R.string.ocr_camera_fail, Toast.LENGTH_LONG).show()
        }
    }

    /** Seçilen fotoğrafı yapay zekâya okutur, onaydan sonra kaydeder. */
    private fun fotografiOku(uri: android.net.Uri) {
        val etkinlik = activity ?: return
        FotoKonuAkisi.oku(etkinlik, uri) { baslik, maddeler ->
            val simdi = System.currentTimeMillis()
            val konu = Store.Topic(
                id = simdi,
                title = baslik,
                createdAt = simdi,
                items = maddeler.mapIndexed { i, metin ->
                    Store.SubItem(
                        id = simdi + i + 1,
                        text = metin,
                        done = false,
                        createdAt = simdi
                    )
                }.toMutableList()
            )
            topics.add(0, konu)
            expandedIds.add(konu.id)
            Store.saveTopics(requireContext(), topics)
            reload()
            Toast.makeText(
                requireContext(),
                getString(R.string.ocr_saved, maddeler.size),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /** Klasik elle konu ekleme. */
    private fun elleKonuEkle() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_topic, null)
        val input = dialogView.findViewById<EditText>(R.id.inputTopic)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.new_topic)
            .setView(dialogView)
            .setPositiveButton(R.string.add) { _, _ ->
                val title = input.text.toString().trim()
                if (title.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.empty_topic_warning, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val topic = Store.Topic(
                    id = System.currentTimeMillis(),
                    title = title,
                    createdAt = System.currentTimeMillis(),
                    items = mutableListOf()
                )
                topics.add(0, topic)
                expandedIds.add(topic.id)
                Store.saveTopics(requireContext(), topics)
                reload()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ═══════════════════════════════════════════════════════════════
    // v7.35 — Yapay zekâ ile alt başlık bulma
    // ═══════════════════════════════════════════════════════════════
    //
    // v7.34'e kadar "Alt madde ekle" doğrudan elle yazma kutusu açıyordu.
    // Kullanıcı asistana konuyu buldurabiliyordu ama sonucu konuya
    // ekleyecek bir yol yoktu. Artık önce seçim soruluyor.

    /** "Alt madde ekle" satırına dokununca: elle mi, yapay zekâ mı? */
    private fun altMaddeSecimi(topic: Store.Topic) {
        val secenekler = arrayOf(
            getString(R.string.sub_add_manual),
            getString(R.string.sub_add_ai),
            // v7.81: toplu üretim — 10/25/50/100 madde
            getString(R.string.ku_toplu_uret)
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.sub_add_choice_title)
            .setItems(secenekler) { _, hangi ->
                when (hangi) {
                    0 -> showSubItemDialog(topic)
                    1 -> yapayZekaIleBul(topic)
                    2 -> topluUretimAyarlari(topic)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ═══════════════════════════════════════════════════════════════
    // v7.81 — TOPLU MADDE ÜRETİMİ
    // ═══════════════════════════════════════════════════════════════

    /** Kaç madde, hangi seviyede üretilecek. */
    private fun topluUretimAyarlari(topic: Store.Topic) {
        val ctx = requireContext()
        if (!AiSettings.isReady(ctx)) {
            Toast.makeText(ctx, R.string.kn_ai_hazir_degil, Toast.LENGTH_LONG).show()
            return
        }

        val adetler = intArrayOf(10, 25, 50, 100)
        var secilenAdet = 25
        var secilenSeviye = KonuUretici.SEVIYE_KARISIK

        val yogunluk = resources.displayMetrics.density
        val kutu = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                (22 * yogunluk).toInt(), (14 * yogunluk).toInt(),
                (22 * yogunluk).toInt(), 0
            )
        }

        fun etiket(metin: String) = TextView(ctx).apply {
            text = metin
            textSize = 12.5f
            alpha = 0.75f
            setPadding(0, (10 * yogunluk).toInt(), 0, (6 * yogunluk).toInt())
        }

        // ── Adet seçimi ──
        kutu.addView(etiket(getString(R.string.ku_kac_madde)))
        val adetSatiri = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val adetCipleri = mutableListOf<TextView>()
        for (a in adetler) {
            val cip = TextView(ctx).apply {
                text = a.toString()
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(
                    (14 * yogunluk).toInt(), (9 * yogunluk).toInt(),
                    (14 * yogunluk).toInt(), (9 * yogunluk).toInt()
                )
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                ).apply { rightMargin = (6 * yogunluk).toInt() }
                isClickable = true
            }
            adetCipleri.add(cip)
            adetSatiri.addView(cip)
        }

        fun adetBoya() {
            adetCipleri.forEachIndexed { i, cip ->
                val secili = adetler[i] == secilenAdet
                cip.setBackgroundColor(if (secili) 0x332196F3 else 0x11888888)
                cip.setTypeface(
                    cip.typeface,
                    if (secili) android.graphics.Typeface.BOLD
                    else android.graphics.Typeface.NORMAL
                )
            }
        }
        adetCipleri.forEachIndexed { i, cip ->
            cip.setOnClickListener { secilenAdet = adetler[i]; adetBoya() }
        }
        adetBoya()
        kutu.addView(adetSatiri)

        // ── Seviye seçimi ──
        kutu.addView(etiket(getString(R.string.ku_seviye)))
        val seviyeler = intArrayOf(
            KonuUretici.SEVIYE_BASLANGIC, KonuUretici.SEVIYE_ORTA,
            KonuUretici.SEVIYE_ILERI, KonuUretici.SEVIYE_KARISIK
        )
        val seviyeCipleri = mutableListOf<TextView>()
        seviyeler.forEach { sv ->
            val cip = TextView(ctx).apply {
                text = KonuUretici.seviyeAdi(ctx, sv)
                textSize = 13.5f
                setPadding(
                    (14 * yogunluk).toInt(), (10 * yogunluk).toInt(),
                    (14 * yogunluk).toInt(), (10 * yogunluk).toInt()
                )
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = (4 * yogunluk).toInt() }
                isClickable = true
            }
            seviyeCipleri.add(cip)
            kutu.addView(cip)
        }

        fun seviyeBoya() {
            seviyeCipleri.forEachIndexed { i, cip ->
                val secili = seviyeler[i] == secilenSeviye
                cip.setBackgroundColor(if (secili) 0x332196F3 else 0x11888888)
                cip.setTypeface(
                    cip.typeface,
                    if (secili) android.graphics.Typeface.BOLD
                    else android.graphics.Typeface.NORMAL
                )
            }
        }
        seviyeCipleri.forEachIndexed { i, cip ->
            cip.setOnClickListener { secilenSeviye = seviyeler[i]; seviyeBoya() }
        }
        seviyeBoya()

        kutu.addView(TextView(ctx).apply {
            text = getString(R.string.ku_uyari)
            textSize = 11.5f
            alpha = 0.7f
            setLineSpacing(0f, 1.2f)
            setPadding(0, (12 * yogunluk).toInt(), 0, 0)
        })

        MaterialAlertDialogBuilder(ctx)
            .setTitle(getString(R.string.ku_baslik, topic.title))
            .setView(ScrollView(ctx).apply { addView(kutu) })
            .setPositiveButton(R.string.ku_baslat) { _, _ ->
                topluUret(topic, secilenAdet, secilenSeviye)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** Üretimi arka planda çalıştırır, ilerlemeyi gösterir. */
    private fun topluUret(topic: Store.Topic, adet: Int, seviye: Int) {
        val ctx = requireContext()
        val yogunluk = resources.displayMetrics.density

        val durum = TextView(ctx).apply {
            text = getString(R.string.ku_uretiliyor, 0, adet)
            textSize = 13.5f
            gravity = Gravity.CENTER
            setPadding(0, (14 * yogunluk).toInt(), 0, (10 * yogunluk).toInt())
        }
        val cubuk = android.widget.ProgressBar(
            ctx, null, android.R.attr.progressBarStyleHorizontal
        ).apply {
            max = adet
            progress = 0
        }
        val kutu = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                (24 * yogunluk).toInt(), (10 * yogunluk).toInt(),
                (24 * yogunluk).toInt(), (10 * yogunluk).toInt()
            )
            addView(durum)
            addView(cubuk)
        }

        val bekleme = MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.ku_toplu_uret)
            .setView(kutu)
            .setCancelable(false)
            .create()
        bekleme.show()

        val mevcut = topic.items.map { it.text }

        Performans.arkaPlan {
            val sonuc = KonuUretici.uret(
                context = ctx,
                konuBasligi = topic.title,
                adet = adet,
                seviye = seviye,
                mevcut = mevcut
            ) { uretilen, hedef ->
                Performans.anaIs {
                    if (!isAdded) return@anaIs
                    durum.text = getString(R.string.ku_uretiliyor, uretilen, hedef)
                    cubuk.progress = uretilen
                }
            }

            Performans.anaIs {
                if (!isAdded) return@anaIs
                runCatching { bekleme.dismiss() }

                if (!sonuc.ok || sonuc.maddeler.isEmpty()) {
                    MaterialAlertDialogBuilder(ctx)
                        .setTitle(R.string.sub_ai_err_title)
                        .setMessage(sonuc.hata.ifBlank { getString(R.string.ku_uretilemedi) })
                        .setPositiveButton(R.string.ok, null)
                        .show()
                    return@anaIs
                }
                // Mevcut onay ekranını yeniden kullan — kullanıcı seçsin
                bulunanlariGoster(topic, sonuc.maddeler)
            }
        }
    }

    /** Konu başlığını yapay zekâya verip alt başlıkları getirtir. */
    private fun yapayZekaIleBul(topic: Store.Topic) {
        val ctx = requireContext()

        val bekleme = MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.sub_ai_searching_title)
            .setMessage(getString(R.string.sub_ai_searching_body, topic.title))
            .setCancelable(false)
            .show()

        val mevcut = topic.items.map { it.text }

        Thread {
            val sonuc = AltBaslikBulucu.bul(ctx, topic.title, mevcut)
            activity?.runOnUiThread {
                try {
                    bekleme.dismiss()
                } catch (_: Exception) {
                }
                if (!isAdded) return@runOnUiThread

                if (!sonuc.ok || sonuc.maddeler.isEmpty()) {
                    MaterialAlertDialogBuilder(ctx)
                        .setTitle(R.string.sub_ai_err_title)
                        .setMessage(sonuc.mesaj)
                        .setPositiveButton(R.string.sub_ai_retry) { _, _ ->
                            yapayZekaIleBul(topic)
                        }
                        .setNeutralButton(R.string.sub_add_manual) { _, _ ->
                            showSubItemDialog(topic)
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                    return@runOnUiThread
                }
                bulunanlariGoster(topic, sonuc.maddeler)
            }
        }.start()
    }

    /**
     * Bulunan maddeleri işaretlenebilir ve düzenlenebilir biçimde gösterir.
     * Hiçbir şey onaysız kaydedilmez.
     */
    private fun bulunanlariGoster(topic: Store.Topic, maddeler: List<String>) {
        val ctx = requireContext()
        val dp = resources.displayMetrics.density

        val kap = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((20 * dp).toInt(), (8 * dp).toInt(), (20 * dp).toInt(), 0)
        }

        kap.addView(TextView(ctx).apply {
            setText(R.string.sub_ai_result_hint)
            textSize = 12f
            alpha = 0.75f
            setPadding(0, 0, 0, (10 * dp).toInt())
        })

        // Her madde: onay kutusu + düzenlenebilir metin
        val kutular = mutableListOf<android.widget.CheckBox>()
        val alanlar = mutableListOf<EditText>()

        maddeler.forEach { metin ->
            val satir = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            val kutu = android.widget.CheckBox(ctx).apply { isChecked = true }
            val alan = EditText(ctx).apply {
                setText(metin)
                textSize = 14f
                setSingleLine(false)
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                )
            }
            kutular.add(kutu)
            alanlar.add(alan)
            satir.addView(kutu)
            satir.addView(alan)
            kap.addView(satir)
        }

        // Tümünü seç / kaldır
        val tumu = android.widget.CheckBox(ctx).apply {
            setText(R.string.sub_ai_select_all)
            isChecked = true
            textSize = 13f
            setPadding(0, (10 * dp).toInt(), 0, 0)
            setOnCheckedChangeListener { _, secili ->
                kutular.forEach { it.isChecked = secili }
            }
        }
        kap.addView(tumu)

        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.sub_ai_result_title)
            .setView(androidx.core.widget.NestedScrollView(ctx).apply { addView(kap) })
            .setPositiveButton(R.string.add) { _, _ ->
                val secilenler = mutableListOf<String>()
                kutular.forEachIndexed { i, kutu ->
                    if (kutu.isChecked) {
                        val t = alanlar[i].text?.toString()?.trim().orEmpty()
                        if (t.isNotBlank()) secilenler.add(t)
                    }
                }
                if (secilenler.isEmpty()) {
                    Toast.makeText(ctx, R.string.sub_ai_none_selected, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val simdi = System.currentTimeMillis()
                secilenler.forEachIndexed { i, t ->
                    topic.items.add(
                        Store.SubItem(
                            id = simdi + i,
                            text = t,
                            done = false,
                            createdAt = simdi
                        )
                    )
                }
                Store.saveTopics(ctx, topics)
                expandedIds.add(topic.id)
                reload()
                Toast.makeText(
                    ctx,
                    getString(R.string.sub_ai_added, secilenler.size),
                    Toast.LENGTH_LONG
                ).show()
            }
            .setNeutralButton(R.string.sub_ai_retry) { _, _ -> yapayZekaIleBul(topic) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showSubItemDialog(topic: Store.Topic) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_subtopic, null)
        val input = dialogView.findViewById<EditText>(R.id.inputSubTopic)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(topic.title)
            .setView(dialogView)
            .setPositiveButton(R.string.add) { _, _ ->
                val text = input.text.toString().trim()
                if (text.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.empty_subtopic_warning, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                topic.items.add(
                    Store.SubItem(
                        id = System.currentTimeMillis(),
                        text = text,
                        done = false,
                        createdAt = System.currentTimeMillis()
                    )
                )
                Store.saveTopics(requireContext(), topics)
                notifyTopicChanged(topic)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** Konu kartını güncel listedeki gerçek konumundan yeniden çizer. */
    private fun notifyTopicChanged(topic: Store.Topic) {
        val index = visibleTopics().indexOfFirst { it.id == topic.id }
        if (index >= 0) adapter.notifyItemChanged(index)
    }

    private fun confirmDeleteTopic(topic: Store.Topic) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_topic_title)
            .setMessage(getString(R.string.delete_topic_message, topic.title))
            .setPositiveButton(R.string.delete) { _, _ ->
                // v7.72: geri alinabilir silme
                Store.deleteTopicUndoable(requireContext(), topic.id)
                reload()
                geriAlSun(getString(R.string.ga_konu_silindi))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** v7.72: Silme sonrasi "Geri al" seridi. */
    private fun geriAlSun(mesaj: String) {
        val kok = view ?: return
        com.google.android.material.snackbar.Snackbar
            .make(kok, mesaj, com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
            .setAction(R.string.ga_geri_al) {
                if (Store.geriAl()) {
                    reload()
                    Toast.makeText(
                        requireContext(), R.string.ga_geri_alindi, Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .show()
    }

    inner class TopicsAdapter : RecyclerView.Adapter<TopicsAdapter.TopicViewHolder>() {

        inner class TopicViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ring: CircularProgressIndicator = view.findViewById(R.id.progressRing)
            val linearBar: com.google.android.material.progressindicator.LinearProgressIndicator? = view.findViewById(R.id.topicLinearBar)
            val percent: TextView = view.findViewById(R.id.percentText)
            val title: TextView = view.findViewById(R.id.topicTitle)
            val caption: TextView = view.findViewById(R.id.topicCaption)
            val chevron: ImageView = view.findViewById(R.id.chevron)
            val delete: ImageView = view.findViewById(R.id.deleteTopic)
            val subContainer: LinearLayout = view.findViewById(R.id.subContainer)
            /** v8.3 · Öneri 13: sol kenardaki renk şeridi. */
            val serit: View = view.findViewById(R.id.konuSerit)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopicViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_topic, parent, false)
            return TopicViewHolder(view)
        }

        override fun getItemCount(): Int = visibleTopics().size

        override fun onBindViewHolder(holder: TopicViewHolder, position: Int) {
            val topic = visibleTopics()[position]

            // v8.3 · Öneri 13: renk kodu + simge.
            // Başlığın önüne simge, kartın soluna renk şeridi.
            val ctx = holder.itemView.context
            val konuRenk = KonuGorunum.renk(ctx, topic.id)
            if (GorunumAyar.tasarimDiliV2(ctx) || GorunumAyar.kompaktKonu(ctx)) {
                val alfa = (TasarimDili.konuIlerlemeOpaklik(topic.percent) * 255).toInt()
                val mavi = android.graphics.Color.parseColor(TasarimDili.RENK_VURGU_ANA)
                holder.serit.setBackgroundColor((mavi and 0x00FFFFFF) or (alfa shl 24))
                holder.title.text = topic.title
                holder.ring.visibility = View.GONE
                holder.linearBar?.visibility = View.VISIBLE
                holder.linearBar?.progress = topic.percent
                runCatching { holder.linearBar?.setIndicatorColor(mavi) }
            } else {
                holder.serit.setBackgroundColor(konuRenk)
                holder.title.text = KonuGorunum.baslikla(ctx, topic.id, topic.title)
                holder.ring.visibility = View.VISIBLE
                holder.linearBar?.visibility = View.GONE
                runCatching { holder.ring.setIndicatorColor(konuRenk) }
                holder.ring.progress = topic.percent
            }
            holder.percent.text = getString(R.string.percent_format, topic.percent)
            holder.caption.text = if (topic.items.isEmpty()) {
                getString(R.string.no_subtopics)
            } else {
                getString(R.string.completed_of, topic.doneCount, topic.items.size)
            }

            val expanded = topic.id in expandedIds
            holder.subContainer.visibility = if (expanded) View.VISIBLE else View.GONE
            holder.chevron.rotation = if (expanded) 180f else 0f

            val toggleExpand = View.OnClickListener {
                if (topic.id in expandedIds) expandedIds.remove(topic.id) else expandedIds.add(topic.id)
                val pos = holder.bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) notifyItemChanged(pos)
            }
            holder.chevron.setOnClickListener(toggleExpand)
            holder.itemView.setOnClickListener(toggleExpand)
            holder.delete.setOnClickListener { confirmDeleteTopic(topic) }
            // v7.82: konuya uzun basınca kitap/toplu işlem menüsü
            holder.itemView.setOnLongClickListener {
                konuSecenekleri(topic)
                true
            }

            // Alt maddeleri çiz
            holder.subContainer.removeAllViews()
            if (expanded) {
                val inflater = LayoutInflater.from(holder.itemView.context)
                val sonIndeks = topic.items.lastIndex
                topic.items.forEachIndexed { sIndeks, sub ->
                    val row = inflater.inflate(R.layout.item_subtopic, holder.subContainer, false)
                    val check = row.findViewById<ImageView>(R.id.subCheck)
                    val text = row.findViewById<TextView>(R.id.subText)
                    val remove = row.findViewById<ImageView>(R.id.subDelete)
                    // v10.44 · Madde #4: elle sıralama düğmeleri
                    val up = row.findViewById<TextView>(R.id.subUp)
                    val down = row.findViewById<TextView>(R.id.subDown)

                    // v8.5 · Öneri 22: ağaç bağlantı çizgisi.
                    // Son maddede dikey çizgi yarıda biter (└), diğerlerinde
                    // devam eder (├). Tamamlananda çizgi dolu.
                    row.findViewById<AgacCizgiView>(R.id.subAgac)?.apply {
                        sonMu = sIndeks == sonIndeks
                        tamamMi = sub.done
                        cizgiRengi = konuRenk
                    }

                    check.setImageResource(
                        if (sub.done) R.drawable.ic_check_circle else R.drawable.ic_circle_outline
                    )
                    check.setColorFilter(
                        MaterialColors.getColor(
                            row.context,
                            if (sub.done) {
                                com.google.android.material.R.attr.colorSecondary
                            } else {
                                com.google.android.material.R.attr.colorOnSurfaceVariant
                            },
                            0xFF888888.toInt()
                        )
                    )
                    text.text = sub.text
                    text.paintFlags = if (sub.done) {
                        text.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    } else {
                        text.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    }
                    text.alpha = if (sub.done) 0.5f else 1f

                    val toggle = View.OnClickListener {
                        sub.done = !sub.done
                        if (sub.done) {
                            Store.recordCompletion(row.context)
                            // v9.0 · Öneri 53: madde bitince tekrar
                            // programına alınıyor. Eskiden madde
                            // işaretlenince bir daha ASLA karşına
                            // çıkmıyordu — "çalıştım" demek kolaydı ama
                            // "öğrendim" garanti değildi.
                            KonuTekrar.programaAl(
                                row.context, sub.id, topic.id, sub.text
                            )
                        } else {
                            // İşaret kaldırıldı: programdan da çıksın
                            KonuTekrar.programdanCikar(row.context, sub.id)
                        }
                        Store.saveTopics(row.context, topics)
                        notifyTopicChanged(topic)
                    }
                    row.setOnClickListener(toggle)
                    check.setOnClickListener(toggle)
                    // v7.81: uzun basınca derinlemesine anlatım
                    row.setOnLongClickListener {
                        maddeSecenekleri(topic, sub)
                        true
                    }
                    remove.setOnClickListener {
                        topic.items.remove(sub)
                        Store.saveTopics(row.context, topics)
                        notifyTopicChanged(topic)
                    }
                    // v10.44: ▲▼ — ilk/son maddede soluk, taşıma kalıcı + diff animasyonlu
                    up.alpha = if (sIndeks == 0) 0.25f else 1f
                    down.alpha = if (sIndeks == sonIndeks) 0.25f else 1f
                    up.setOnClickListener {
                        if (ListeTasi.yukariTasi(topic.items, sIndeks)) {
                            Store.saveTopics(row.context, topics)
                            notifyTopicChanged(topic)
                        }
                    }
                    down.setOnClickListener {
                        if (ListeTasi.asagiTasi(topic.items, sIndeks)) {
                            Store.saveTopics(row.context, topics)
                            notifyTopicChanged(topic)
                        }
                    }
                    holder.subContainer.addView(row)
                }

                // "Alt madde ekle" satırı
                val addRow = inflater.inflate(R.layout.item_subtopic_add, holder.subContainer, false)
                addRow.setOnClickListener { altMaddeSecimi(topic) }
                holder.subContainer.addView(addRow)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // v7.82 — KONU KİTABI (tüm maddeler tek PDF)
    // ═══════════════════════════════════════════════════════════════

    /** Konu başlığına uzun basınca açılan menü. */
    private fun konuSecenekleri(topic: Store.Topic) {
        val ctx = requireContext()
        val hazirSayisi = topic.items.count { KonuUretici.anlatimVarMi(ctx, it.text) }

        val secenekler = arrayOf(
            getString(R.string.kb_kitap_yap),
            getString(R.string.kb_tumunu_anlat, topic.items.size),
            // v8.1: sırayla sesli dinleme (öneri 9)
            getString(R.string.sl_sirayla_dinle, hazirSayisi),
            // v8.3: renk ve simge (öneri 13)
            getString(R.string.knr_baslik)
        )

        // v8.4 · Öneri 18: alt sayfa menüsü
        AltSayfa.menu(
            ctx,
            KonuGorunum.baslikla(ctx, topic.id, topic.title),
            listOf(
                AltSayfa.Oge(secenekler[0], simge = "📕") { kitapYap(topic) },
                AltSayfa.Oge(secenekler[1], simge = "🤖") { tumunuAnlat(topic) },
                AltSayfa.Oge(
                    secenekler[2], simge = "🔊",
                    etkin = hazirSayisi > 0
                ) { sirayalaDinle(topic) },
                AltSayfa.Oge(secenekler[3], simge = "🎨") { gorunumSec(topic) },
                AltSayfa.Oge("⚡ Başka Bir Sekmeye Taşı veya Kopyala (Bugün ⇄ İlerleme vb.)", simge = "🔀") {
                    SekmeVeVeriTasimaMotoru.sekmeArasiTasimaDiyalogu(ctx, "topics", topic.title, "${topic.items.size} alt konu/madde içeriyor") { onResume() }
                },
                AltSayfa.Oge(
                    getString(R.string.delete), simge = "🗑", yikici = true
                ) { confirmDeleteTopic(topic) }
            ),
            altBaslik = getString(R.string.kb_hazir_sayisi, hazirSayisi, topic.items.size)
        )
    }

    /**
     * v8.3 · Öneri 13 — Konuya renk ve simge atar.
     *
     * Renk paleti 10, simge 20 tane. İkisi de ayrı adımda seçiliyor;
     * tek bir ızgarada 200 hücre göstermek yerine önce renk sorulup
     * sonra simge soruluyor.
     */
    private fun gorunumSec(topic: Store.Topic) {
        val ctx = requireContext()
        val yg = resources.displayMetrics.density
        fun dp(v: Int) = (v * yg).toInt()

        val kok = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(4))
        }

        // ---- Renk satırları ----
        kok.addView(TextView(ctx).apply {
            setText(R.string.kr_renk)
            textSize = 12.5f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(8))
        })

        var seciliRenk = KonuGorunum.renkIndeksi(ctx, topic.id)
        val renkDaireler = mutableListOf<View>()

        fun renkleriTazele() {
            renkDaireler.forEachIndexed { i, d ->
                d.background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(KonuGorunum.RENKLER[i])
                    if (i == seciliRenk) setStroke(dp(3), 0xFF000000.toInt())
                }
            }
        }

        KonuGorunum.RENKLER.toList().chunked(5).forEach { parca ->
            val satir = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_HORIZONTAL
            }
            parca.forEach { renkDegeri ->
                val i = KonuGorunum.RENKLER.indexOf(renkDegeri)
                val daire = View(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(42), dp(42)).apply {
                        setMargins(dp(6), dp(4), dp(6), dp(4))
                    }
                    setOnClickListener {
                        Titresim.tik(it)
                        seciliRenk = i
                        renkleriTazele()
                    }
                }
                renkDaireler.add(daire)
                satir.addView(daire)
            }
            kok.addView(satir)
        }
        renkleriTazele()

        // ---- Simge satırları ----
        kok.addView(TextView(ctx).apply {
            setText(R.string.kr_simge)
            textSize = 12.5f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, dp(14), 0, dp(6))
        })

        var seciliSimge = KonuGorunum.simge(ctx, topic.id)
        val simgeGorunumler = mutableListOf<TextView>()

        fun simgeleriTazele() {
            simgeGorunumler.forEach { g ->
                g.setBackgroundColor(
                    if (g.text.toString() == seciliSimge) 0x33888888 else 0x00000000
                )
            }
        }

        KonuGorunum.SIMGELER.toList().chunked(7).forEach { parca ->
            val satir = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_HORIZONTAL
            }
            parca.forEach { sim ->
                val g = TextView(ctx).apply {
                    text = sim
                    textSize = 19f
                    gravity = android.view.Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(dp(38), dp(38)).apply {
                        setMargins(dp(2), dp(2), dp(2), dp(2))
                    }
                    setOnClickListener {
                        Titresim.tik(it)
                        seciliSimge = if (seciliSimge == sim) "" else sim
                        simgeleriTazele()
                    }
                }
                simgeGorunumler.add(g)
                satir.addView(g)
            }
            kok.addView(satir)
        }
        simgeleriTazele()

        val kaydirici = android.widget.ScrollView(ctx).apply { addView(kok) }

        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.knr_baslik)
            .setView(kaydirici)
            .setPositiveButton(R.string.save) { _, _ ->
                if (seciliRenk in KonuGorunum.RENKLER.indices) {
                    KonuGorunum.renkAta(ctx, topic.id, seciliRenk)
                }
                KonuGorunum.simgeAta(ctx, topic.id, seciliSimge)
                Toast.makeText(ctx, R.string.kr_kaydedildi, Toast.LENGTH_SHORT).show()
                reload()
            }
            .setNeutralButton(R.string.kr_temizle) { _, _ ->
                KonuGorunum.sifirla(ctx, topic.id)
                reload()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Hazır anlatımları tek PDF kitapta toplar.
     *
     * Sadece **önceden üretilmiş** anlatımlar kullanılır — burada AI'ya
     * gidilmez. Hiç anlatım yoksa kullanıcı "Tümünü anlat"a yönlendirilir.
     * Bu ayrım bilinçli: kitap yapmak saniyeler sürmeli, 50 maddelik bir
     * konuyu baştan anlattırmak yarım saat sürebilir.
     */
    private fun kitapYap(topic: Store.Topic) {
        val ctx = requireContext()
        val anlatimlar = topic.items.mapNotNull { KonuUretici.anlatimOku(ctx, it.text) }

        if (anlatimlar.isEmpty()) {
            MaterialAlertDialogBuilder(ctx)
                .setTitle(R.string.kb_bos_baslik)
                .setMessage(R.string.kb_bos_mesaj)
                .setPositiveButton(R.string.kb_tumunu_anlat_kisa) { _, _ -> tumunuAnlat(topic) }
                .setNegativeButton(R.string.cancel, null)
                .show()
            return
        }

        val bekle = MaterialAlertDialogBuilder(ctx)
            .setMessage(getString(R.string.kb_hazirlaniyor, anlatimlar.size))
            .setCancelable(false)
            .create()
        bekle.show()

        Performans.arkaPlan {
            val dosya = KonuPdf.kitap(ctx, topic.title, anlatimlar)
            Performans.anaIs {
                if (!isAdded) return@anaIs
                runCatching { bekle.dismiss() }
                if (dosya == null) {
                    Toast.makeText(ctx, R.string.ka_pdf_hata, Toast.LENGTH_LONG).show()
                } else {
                    kitapSecenekleri(dosya, anlatimlar.size)
                }
            }
        }
    }

    private fun kitapSecenekleri(dosya: java.io.File, adet: Int) {
        val ctx = requireContext()
        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.kb_hazir)
            .setMessage(getString(R.string.kb_hazir_alt, adet, dosya.name))
            .setPositiveButton(R.string.ka_pdf_ac) { _, _ ->
                try {
                    startActivity(
                        android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            setDataAndType(KonuPdf.uriVer(ctx, dosya), "application/pdf")
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    )
                } catch (e: Exception) {
                    Toast.makeText(ctx, R.string.ka_pdf_okuyucu_yok, Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton(R.string.ka_pdf_paylas) { _, _ ->
                try {
                    startActivity(
                        android.content.Intent.createChooser(
                            android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(
                                    android.content.Intent.EXTRA_STREAM,
                                    KonuPdf.uriVer(ctx, dosya)
                                )
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            },
                            getString(R.string.ka_pdf_paylas)
                        )
                    )
                } catch (e: Exception) {
                    Toast.makeText(ctx, R.string.ka_pdf_hata, Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton(R.string.ok, null)
            .show()
    }

    /**
     * Konudaki tüm maddeler için anlatım üretir.
     *
     * Uzun sürer (madde başına 10-30 sn) ve ciddi kota harcar; bu yüzden
     * önce açık bir uyarı gösteriliyor. Zaten anlatımı olan maddeler
     * atlanır — ikinci çalıştırmada kaldığı yerden devam etmiş olur.
     */
    private fun tumunuAnlat(topic: Store.Topic) {
        val ctx = requireContext()
        if (!AiSettings.isReady(ctx)) {
            Toast.makeText(ctx, R.string.kn_ai_hazir_degil, Toast.LENGTH_LONG).show()
            return
        }

        val eksikler = topic.items.filterNot { KonuUretici.anlatimVarMi(ctx, it.text) }
        if (eksikler.isEmpty()) {
            Toast.makeText(ctx, R.string.kb_hepsi_hazir, Toast.LENGTH_LONG).show()
            return
        }

        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.kb_tumunu_anlat_kisa)
            .setMessage(
                getString(R.string.kb_uyari, eksikler.size, (eksikler.size * 20 + 59) / 60)
            )
            .setPositiveButton(R.string.ku_baslat) { _, _ -> topluAnlatimBasla(topic, eksikler) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun topluAnlatimBasla(topic: Store.Topic, eksikler: List<Store.SubItem>) {
        val ctx = requireContext()
        val yogunluk = resources.displayMetrics.density

        val durum = TextView(ctx).apply {
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, (14 * yogunluk).toInt(), 0, (10 * yogunluk).toInt())
            text = getString(R.string.kb_ilerleme, 0, eksikler.size, "")
        }
        val cubuk = android.widget.ProgressBar(
            ctx, null, android.R.attr.progressBarStyleHorizontal
        ).apply { max = eksikler.size; progress = 0 }

        val kutu = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                (24 * yogunluk).toInt(), (10 * yogunluk).toInt(),
                (24 * yogunluk).toInt(), (10 * yogunluk).toInt()
            )
            addView(durum)
            addView(cubuk)
        }

        // İptal edilebilir olmalı — 50 maddelik iş yarıda kesilebilmeli
        var iptal = false
        val pencere = MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.kb_tumunu_anlat_kisa)
            .setView(kutu)
            .setCancelable(false)
            .setNegativeButton(R.string.cancel) { _, _ -> iptal = true }
            .create()
        pencere.show()

        Performans.arkaPlan {
            var basarili = 0
            eksikler.forEachIndexed { i, madde ->
                if (iptal) return@forEachIndexed

                Performans.anaIs {
                    if (isAdded) {
                        durum.text = getString(
                            R.string.kb_ilerleme, i + 1, eksikler.size, madde.text.take(40)
                        )
                        cubuk.progress = i
                    }
                }

                val a = KonuUretici.anlat(ctx, madde.text, topic.title, uzunluk = 2)
                if (a.ok) {
                    KonuUretici.anlatimKaydet(ctx, madde.text, a)
                    basarili++
                }
            }

            Performans.anaIs {
                if (!isAdded) return@anaIs
                runCatching { pencere.dismiss() }
                MaterialAlertDialogBuilder(ctx)
                    .setTitle(R.string.kb_bitti)
                    .setMessage(getString(R.string.kb_bitti_alt, basarili, eksikler.size))
                    .setPositiveButton(R.string.kb_kitap_yap) { _, _ -> kitapYap(topic) }
                    .setNegativeButton(R.string.ok, null)
                    .show()
            }
        }
    }

    /**
     * v8.1 — Konudaki anlatımları sırayla sesli okur (öneri 9).
     *
     * Yalnızca anlatımı hazır maddeler listeye giriyor; olmayanı okumak
     * için AI'ya gitmek gerekir ve dinlemeye başlamış kullanıcıyı
     * 20 saniye bekletmek akışı bozar.
     */
    private fun sirayalaDinle(topic: Store.Topic) {
        val ctx = requireContext()
        val adet = SesliListe.konudanKur(ctx, topic)

        if (adet == 0) {
            MaterialAlertDialogBuilder(ctx)
                .setTitle(R.string.sl_hazir_yok_baslik)
                .setMessage(R.string.sl_hazir_yok)
                .setPositiveButton(R.string.kb_tumunu_anlat_kisa) { _, _ ->
                    tumunuAnlat(topic)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
            return
        }

        val metin = SesliListe.aktifMetin(ctx)
        if (metin.isNullOrBlank()) {
            Toast.makeText(ctx, R.string.ks_metin_yok, Toast.LENGTH_SHORT).show()
            return
        }

        try {
            SesliDersServisi.baslat(
                context = ctx,
                metin = metin,
                baslik = topic.title + "  " + SesliListe.ilerlemeMetni(ctx),
                asset = "liste:" + topic.id,
                hiz = 1.0f
            )
            Toast.makeText(
                ctx, getString(R.string.sl_basladi, adet), Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            android.util.Log.w("TopicsFragment", "Sesli liste başlatılamadı", e)
            Toast.makeText(ctx, R.string.ks_hata, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * v7.81 — Bir maddeye uzun basınca açılan menü.
     *
     * En önemli giriş: "Konuyu anlat" — [KonuAnlatimActivity] uzun ders
     * metni, kaynak alıntıları ve görsel önerileriyle açılır.
     */
    private fun maddeSecenekleri(topic: Store.Topic, sub: Store.SubItem) {
        val ctx = requireContext()
        val anlatimVar = KonuUretici.anlatimVarMi(ctx, sub.text)

        val secenekler = arrayOf(
            getString(if (anlatimVar) R.string.ku_anlatimi_ac else R.string.ku_anlat),
            getString(R.string.ku_web_ara),
            getString(R.string.ku_gorsel_ara),
            getString(R.string.delete)
        )

        MaterialAlertDialogBuilder(ctx)
            .setTitle(sub.text)
            .setItems(secenekler) { _, hangi ->
                when (hangi) {
                    0 -> KonuAnlatimActivity.ac(ctx, sub.text, topic.title)
                    1 -> acWeb("https://www.google.com/search?q=" +
                        android.net.Uri.encode(sub.text + " " + topic.title))
                    2 -> acWeb("https://www.google.com/search?tbm=isch&q=" +
                        android.net.Uri.encode(sub.text))
                    3 -> {
                        topic.items.remove(sub)
                        Store.saveTopics(ctx, topics)
                        notifyTopicChanged(topic)
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun acWeb(url: String) {
        try {
            startActivity(
                android.content.Intent(
                    android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)
                )
            )
        } catch (e: Exception) {
            Toast.makeText(requireContext(), R.string.ka_tarayici_yok, Toast.LENGTH_SHORT).show()
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
}
