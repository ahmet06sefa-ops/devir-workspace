package com.gunlukasistan.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * v7.20 — Kaynak Merkezi.
 *
 * Yapay zekâ ile internetten bulunan PDF ve videoların toplandığı ekran.
 * Buradan ders seçilir, arama yapılır, bulunanlar onaylanıp kaydedilir.
 *
 * Uydurma link sorununa karşı: arama `KaynakBulucu` üzerinden gerçek
 * Google Arama / YouTube API ile yapılır ve sonuçlar HTTP ile doğrulanır.
 */
class KaynaklarFragment : Fragment(R.layout.fragment_kaynaklar) {

    private lateinit var adapter: KaynakAdapter
    private lateinit var bosYazi: TextView
    private lateinit var ozet: TextView

    private val satirlar = mutableListOf<Satir>()
    private var seciliDers: Store.Lesson? = null
    private var filtre = Filtre.HEPSI

    enum class Filtre { HEPSI, PDF, VIDEO }

    /** Liste satırı: kaynak + ait olduğu dersin adı. */
    data class Satir(val kaynak: Store.DersKaynak, val dersAdi: String, val kursAdi: String)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // v8.6 · Öneri 29: yatay/tablet genişlik sınırı + yoğunluk
        Duzen.uygula(view)
        bosYazi = view.findViewById(R.id.krEmpty)
        ozet = view.findViewById(R.id.krSummary)

        val recycler = view.findViewById<RecyclerView>(R.id.krRecycler)
        adapter = KaynakAdapter()
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        view.findViewById<TextView>(R.id.krLessonPick).setOnClickListener { dersSec() }
        view.findViewById<MaterialButton>(R.id.krFindPdf).setOnClickListener { ara(video = false) }
        view.findViewById<MaterialButton>(R.id.krFindVideo).setOnClickListener { ara(video = true) }
        // v7.21: yeni kurs üret / tek tuşla hepsini bul
        view.findViewById<MaterialButton>(R.id.krNewCourse).setOnClickListener { kursUretMenusu() }
        view.findViewById<MaterialButton>(R.id.krAutoAll).setOnClickListener { otomatikHepsi() }
        view.findViewById<TextView>(R.id.krFilterBtn).apply {
            setOnClickListener { filtreDegistir(it as TextView) }
            // v7.23: uzun bas → yapay zekâ tanı ekranı
            setOnLongClickListener { taniCalistir(); true }
        }

        // Kurs ekranından "bu ders için kaynak bul" ile gelinmişse
        arguments?.getLong(ARG_LESSON_ID, 0L)?.takeIf { it > 0L }?.let { id ->
            seciliDers = Store.loadLessons(requireContext()).firstOrNull { it.id == id }
        }

        yenile()
    }

    override fun onResume() {
        super.onResume()
        yenile()
    }

    private fun yenile() {
        val ctx = context ?: return
        val kok = view ?: return

        // Seçili ders etiketi
        kok.findViewById<TextView>(R.id.krLessonPick).text =
            seciliDers?.let { "▸ " + it.title } ?: getString(R.string.src_pick_lesson)

        // Yapay zekâ hazır mı uyarısı
        val hazir = AiSettings.isReady(ctx)
        kok.findViewById<TextView>(R.id.krAiWarn).visibility =
            if (hazir) View.GONE else View.VISIBLE
        kok.findViewById<MaterialButton>(R.id.krFindPdf).isEnabled = hazir
        kok.findViewById<MaterialButton>(R.id.krFindVideo).isEnabled = hazir

        // Kaynak listesi
        val dersler = Store.loadLessons(ctx).associateBy { it.id }
        val kurslar = Store.loadCourses(ctx).associateBy { it.id }
        satirlar.clear()
        Store.loadKaynaklar(ctx)
            .sortedByDescending { it.eklendi }
            .forEach { k ->
                val d = dersler[k.lessonId]
                satirlar.add(
                    Satir(
                        kaynak = k,
                        dersAdi = d?.title ?: getString(R.string.src_unknown_lesson),
                        kursAdi = d?.let { kurslar[it.courseId]?.title }.orEmpty()
                    )
                )
            }

        val gorunen = filtrele()
        adapter.submit(gorunen)
        bosYazi.visibility = if (gorunen.isEmpty()) View.VISIBLE else View.GONE

        val pdf = satirlar.count { it.kaynak.tur == "pdf" }
        val video = satirlar.count { it.kaynak.tur == "video" }
        ozet.text = if (satirlar.isEmpty()) getString(R.string.src_summary_empty)
        else getString(R.string.src_summary, satirlar.size, pdf, video)
    }

    private fun filtrele(): List<Satir> = when (filtre) {
        Filtre.HEPSI -> satirlar
        Filtre.PDF -> satirlar.filter { it.kaynak.tur == "pdf" || it.kaynak.tur == "sayfa" }
        Filtre.VIDEO -> satirlar.filter { it.kaynak.tur == "video" }
    }

    private fun filtreDegistir(dugme: TextView) {
        filtre = when (filtre) {
            Filtre.HEPSI -> Filtre.PDF
            Filtre.PDF -> Filtre.VIDEO
            Filtre.VIDEO -> Filtre.HEPSI
        }
        dugme.setText(
            when (filtre) {
                Filtre.HEPSI -> R.string.src_filter_all
                Filtre.PDF -> R.string.src_filter_pdf
                Filtre.VIDEO -> R.string.src_filter_video
            }
        )
        yenile()
    }

    // ─────────────────── Ders seçimi ───────────────────

    private fun dersSec() {
        val ctx = requireContext()
        val dersler = Store.loadLessons(ctx)
        if (dersler.isEmpty()) {
            Toast.makeText(ctx, R.string.src_no_lessons, Toast.LENGTH_LONG).show()
            return
        }
        val kurslar = Store.loadCourses(ctx).associateBy { it.id }
        // Önce tamamlanmamışlar, sonra sıraya göre
        val sirali = dersler.sortedWith(compareBy({ it.done }, { it.order }))
        val etiketler = sirali.map { d ->
            val kurs = kurslar[d.courseId]?.title?.take(18).orEmpty()
            (if (d.done) "✓ " else "") + d.title + (if (kurs.isNotBlank()) "  ·  $kurs" else "")
        }.toTypedArray()

        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.src_pick_lesson_title)
            .setItems(etiketler) { _, hangi ->
                seciliDers = sirali[hangi]
                yenile()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ═══════════════════════════════════════════════════════════════
    // v7.21 — Otomatik kurs üretme
    // ═══════════════════════════════════════════════════════════════

    private fun kursUretMenusu() {
        val ctx = requireContext()
        if (!AiSettings.isReady(ctx)) {
            aiUyarisi()
            return
        }
        val secenekler = arrayOf(
            getString(R.string.gen_opt_new),
            getString(R.string.gen_opt_expand)
        )
        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.gen_new_course)
            .setItems(secenekler) { _, hangi ->
                if (hangi == 0) yeniKursDiyalogu() else kursGenisletSec()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** Kurs adı + seviye + ders sayısı sorar, sonra üretir. */
    private fun yeniKursDiyalogu() {
        val ctx = requireContext()
        val dp = resources.displayMetrics.density
        val pad = (20 * dp).toInt()

        val adAlani = android.widget.EditText(ctx).apply {
            hint = getString(R.string.gen_name_hint)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setSingleLine()
        }

        val ornekler = TextView(ctx).apply {
            setText(R.string.gen_examples)
            textSize = 11f
            alpha = 0.7f
            setPadding(0, (6 * dp).toInt(), 0, (10 * dp).toInt())
        }

        val seviyeler = listOf(
            getString(R.string.gen_lvl_all) to "hepsi",
            getString(R.string.gen_lvl_basic) to "temel",
            getString(R.string.gen_lvl_mid) to "orta",
            getString(R.string.gen_lvl_adv) to "ileri"
        )
        val seviyeSpin = android.widget.Spinner(ctx).apply {
            adapter = android.widget.ArrayAdapter(
                ctx, android.R.layout.simple_spinner_dropdown_item,
                seviyeler.map { it.first }
            )
        }

        val boyutlar = listOf(
            getString(R.string.gen_size_s) to 18,
            getString(R.string.gen_size_m) to 30,
            getString(R.string.gen_size_l) to 45
        )
        val boyutSpin = android.widget.Spinner(ctx).apply {
            adapter = android.widget.ArrayAdapter(
                ctx, android.R.layout.simple_spinner_dropdown_item,
                boyutlar.map { it.first }
            )
            setSelection(1)
        }

        fun etiket(res: Int) = TextView(ctx).apply {
            setText(res)
            textSize = 12f
            setPadding(0, (10 * dp).toInt(), 0, (2 * dp).toInt())
        }

        val kap = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(pad, (8 * dp).toInt(), pad, 0)
            addView(adAlani)
            addView(ornekler)
            addView(etiket(R.string.gen_lvl_label))
            addView(seviyeSpin)
            addView(etiket(R.string.gen_size_label))
            addView(boyutSpin)
        }

        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.gen_opt_new)
            .setView(kap)
            .setPositiveButton(R.string.gen_create) { _, _ ->
                val ad = adAlani.text?.toString()?.trim().orEmpty()
                if (ad.length < 2) {
                    Toast.makeText(ctx, R.string.gen_name_empty, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                kursUret(
                    ad,
                    seviyeler[seviyeSpin.selectedItemPosition].second,
                    boyutlar[boyutSpin.selectedItemPosition].second
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun kursUret(ad: String, seviye: String, hedefDers: Int) {
        val ctx = requireContext()
        val bekleme = MaterialAlertDialogBuilder(ctx)
            .setTitle(getString(R.string.gen_working_title, ad))
            .setMessage(R.string.gen_working_body)
            .setCancelable(false)
            .show()

        Thread {
            val sonuc = KursUretici.uret(ctx, ad, seviye, hedefDers)
            activity?.runOnUiThread {
                try {
                    bekleme.dismiss()
                } catch (_: Exception) {
                }
                val m = sonuc.mufredat
                if (!sonuc.ok || m == null) {
                    MaterialAlertDialogBuilder(ctx)
                        .setTitle(R.string.gen_err_title)
                        .setMessage(sonuc.mesaj.ifBlank { getString(R.string.gen_err_parse) })
                        .setPositiveButton(R.string.done, null)
                        .show()
                    return@runOnUiThread
                }
                mufredatOnizle(m)
            }
        }.start()
    }

    /** Üretilen müfredatı kaydetmeden önce gösterir. */
    private fun mufredatOnizle(m: KursUretici.Mufredat) {
        val ctx = requireContext()
        val govde = buildString {
            if (m.aciklama.isNotBlank()) {
                append(m.aciklama).append("\n\n")
            }
            append(
                getString(
                    R.string.gen_summary,
                    m.bolumler.size, m.dersSayisi,
                    m.toplamDakika / 60, m.toplamDakika % 60
                )
            )
            append("\n\n")
            m.bolumler.forEachIndexed { i, b ->
                append("${i + 1}. ${b.baslik}  (${b.dersler.size})\n")
                b.dersler.take(3).forEach { d ->
                    append("     • ${d.baslik}\n")
                }
                if (b.dersler.size > 3) {
                    append("     … ${b.dersler.size - 3} ders daha\n")
                }
            }
        }

        MaterialAlertDialogBuilder(ctx)
            .setTitle("${m.emoji}  ${m.kursAdi}")
            .setMessage(govde)
            .setPositiveButton(R.string.gen_save) { _, _ ->
                val kurs = KursUretici.kaydet(ctx, m)
                yenile()
                MaterialAlertDialogBuilder(ctx)
                    .setTitle(R.string.gen_saved_title)
                    .setMessage(getString(R.string.gen_saved_body, m.dersSayisi, kurs.title))
                    .setPositiveButton(R.string.gen_goto_course) { _, _ ->
                        (activity as? MainActivity)?.open(13)
                    }
                    .setNegativeButton(R.string.done, null)
                    .show()
            }
            .setNeutralButton(R.string.gen_retry) { _, _ ->
                kursUret(m.kursAdi, "hepsi", m.dersSayisi.coerceAtLeast(18))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** Var olan kursu genişlet — eksik konuları ekle. */
    private fun kursGenisletSec() {
        val ctx = requireContext()
        val kurslar = Store.loadCourses(ctx)
        if (kurslar.isEmpty()) {
            Toast.makeText(ctx, R.string.src_no_lessons, Toast.LENGTH_LONG).show()
            return
        }
        val etiketler = kurslar.map { k ->
            val n = Store.loadLessons(ctx).count { it.courseId == k.id }
            "${k.emoji} ${k.title}  ($n ders)"
        }.toTypedArray()

        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.gen_expand_pick)
            .setItems(etiketler) { _, hangi ->
                val kurs = kurslar[hangi]
                val bekleme = MaterialAlertDialogBuilder(ctx)
                    .setTitle(getString(R.string.gen_working_title, kurs.title))
                    .setMessage(R.string.gen_expand_body)
                    .setCancelable(false)
                    .show()

                Thread {
                    val sonuc = KursUretici.genislet(ctx, kurs, 12)
                    activity?.runOnUiThread {
                        try {
                            bekleme.dismiss()
                        } catch (_: Exception) {
                        }
                        val m = sonuc.mufredat
                        if (!sonuc.ok || m == null) {
                            Toast.makeText(
                                ctx,
                                sonuc.mesaj.ifBlank { getString(R.string.gen_err_parse) },
                                Toast.LENGTH_LONG
                            ).show()
                            return@runOnUiThread
                        }
                        // Var olan kursa bölüm olarak ekle
                        MaterialAlertDialogBuilder(ctx)
                            .setTitle(R.string.gen_expand_found)
                            .setMessage(
                                getString(
                                    R.string.gen_expand_summary,
                                    m.bolumler.size, m.dersSayisi, kurs.title
                                ) + "\n\n" + m.bolumler.joinToString("\n") {
                                    "• ${it.baslik} (${it.dersler.size})"
                                }
                            )
                            .setPositiveButton(R.string.gen_save) { _, _ ->
                                m.bolumler.forEach { b ->
                                    val bolum = Store.addSection(ctx, kurs.id, b.baslik)
                                    b.dersler.forEach { d ->
                                        Store.addLesson(
                                            ctx, kurs.id, bolum.id,
                                            d.baslik, d.dakika, d.aciklama
                                        )
                                    }
                                }
                                yenile()
                                Toast.makeText(
                                    ctx,
                                    getString(R.string.gen_expanded, m.dersSayisi),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            .setNegativeButton(R.string.cancel, null)
                            .show()
                    }
                }.start()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ═══════════════════════════════════════════════════════════════
    // v7.21 — Tek tuşla: PDF + video birlikte
    // ═══════════════════════════════════════════════════════════════

    private fun otomatikHepsi() {
        val ctx = requireContext()
        val ders = seciliDers
        if (ders == null) {
            Toast.makeText(ctx, R.string.src_pick_first, Toast.LENGTH_SHORT).show()
            dersSec()
            return
        }
        if (!AiSettings.isReady(ctx)) {
            aiUyarisi()
            return
        }
        val kursAdi = Store.loadCourses(ctx)
            .firstOrNull { it.id == ders.courseId }?.title.orEmpty()

        val bekleme = MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.gen_auto_working)
            .setMessage(R.string.src_searching_body)
            .setCancelable(false)
            .show()

        Thread {
            val hepsi = mutableListOf<KaynakBulucu.Kaynak>()
            // Önce video (genelde daha isabetli), sonra belge
            val v = KaynakBulucu.ara(ctx, ders.title, kursAdi, video = true)
            if (v.ok) hepsi.addAll(v.kaynaklar)
            val p = KaynakBulucu.ara(ctx, ders.title, kursAdi, video = false)
            if (p.ok) hepsi.addAll(p.kaynaklar)

            if (hepsi.isNotEmpty()) KaynakBulucu.hepsiniDogrula(hepsi)

            activity?.runOnUiThread {
                try {
                    bekleme.dismiss()
                } catch (_: Exception) {
                }
                if (hepsi.isEmpty()) {
                    MaterialAlertDialogBuilder(ctx)
                        .setTitle(R.string.src_err_title)
                        .setMessage(getString(R.string.src_err_none))
                        .setPositiveButton(R.string.done, null)
                        .show()
                    return@runOnUiThread
                }
                sonuclariSun(ders, hepsi, video = false)
            }
        }.start()
    }

    /**
     * v7.23: Yapay zekâ neden çalışmıyor — adım adım sınar ve sonucu gösterir.
     * "Çalışmıyor" geri bildirimini somut bir tanıya çevirir.
     */
    private fun taniCalistir() {
        val ctx = requireContext()
        val bekleme = MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.diag_title)
            .setMessage(R.string.diag_running)
            .setCancelable(false)
            .show()

        Thread {
            val rapor = StringBuilder()
            fun satir(ad: String, ok: Boolean, not: String = "") {
                rapor.append(if (ok) "✓ " else "✗ ").append(ad)
                if (not.isNotBlank()) rapor.append("\n     ").append(not)
                rapor.append("\n")
            }

            // 1. Ağ
            val net = AiClient.isOnline(ctx)
            satir(getString(R.string.diag_net), net)

            // 2. Çevrimiçi mod
            val mod = AiSettings.isOnlineMode(ctx)
            satir(getString(R.string.diag_mode), mod,
                if (mod) "" else getString(R.string.diag_mode_fix))

            // 3. Anahtar
            val anahtar = AiSettings.hasApiKey(ctx)
            satir(getString(R.string.diag_key), anahtar,
                if (anahtar) AiSettings.maskedKeyPreview(ctx)
                else getString(R.string.diag_key_fix))

            // 4. Sağlayıcı ve model
            val prov = AiClient.Provider.fromId(AiSettings.getProviderId(ctx))
            val model = AiClient.calisanModel(ctx, prov)
                ?: AiSettings.getModel(ctx).ifBlank { prov.defaultModel }
            satir(getString(R.string.diag_model), true, "${prov.label} · $model")

            // 5. v7.24: yedek sağlayıcılar
            val yedekler = AiClient.saglayiciSirasi(ctx)
            val yedekAdlari = yedekler.joinToString(" → ") { it.label.take(18) }
            satir(
                getString(R.string.diag_providers),
                yedekler.size > 1,
                if (yedekler.size > 1) yedekAdlari
                else getString(R.string.diag_providers_single)
            )

            // 5. Gerçek istek
            var cevapNotu = ""
            var cevapOk = false
            if (net && mod && anahtar) {
                val r = AiClient.chat(ctx, "Test: sadece OK yaz.")
                cevapOk = r.ok
                cevapNotu = if (r.ok) r.text.take(60) else r.text.take(160)
            } else {
                cevapNotu = getString(R.string.diag_skipped)
            }
            satir(getString(R.string.diag_request), cevapOk, cevapNotu)
            AiClient.sonGecisBilgisi?.let { rapor.append("\n").append(it).append("\n") }

            activity?.runOnUiThread {
                try {
                    bekleme.dismiss()
                } catch (_: Exception) {
                }
                MaterialAlertDialogBuilder(ctx)
                    .setTitle(R.string.diag_title)
                    .setMessage(rapor.toString())
                    .setPositiveButton(R.string.done, null)
                    .setNeutralButton(R.string.ocr_open_settings) { _, _ ->
                        (activity as? MainActivity)?.openSettings()
                    }
                    .show()
            }
        }.start()
    }

    private fun aiUyarisi() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.src_center_title)
            .setMessage(R.string.src_ai_needed)
            .setPositiveButton(R.string.ocr_open_settings) { _, _ ->
                (activity as? MainActivity)?.openSettings()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ─────────────────── Arama ───────────────────

    private fun ara(video: Boolean) {
        val ctx = requireContext()
        val ders = seciliDers
        if (ders == null) {
            Toast.makeText(ctx, R.string.src_pick_first, Toast.LENGTH_SHORT).show()
            dersSec()
            return
        }
        val kursAdi = Store.loadCourses(ctx)
            .firstOrNull { it.id == ders.courseId }?.title.orEmpty()

        val bekleme = MaterialAlertDialogBuilder(ctx)
            .setTitle(if (video) R.string.src_searching_video else R.string.src_searching_pdf)
            .setMessage(R.string.src_searching_body)
            .setCancelable(false)
            .show()

        Thread {
            val sonuc = KaynakBulucu.ara(ctx, ders.title, kursAdi, video)
            // Linkleri gerçekten açılıyor mu diye sına
            if (sonuc.ok) KaynakBulucu.hepsiniDogrula(sonuc.kaynaklar)

            activity?.runOnUiThread {
                try {
                    bekleme.dismiss()
                } catch (_: Exception) {
                }
                if (!sonuc.ok || sonuc.kaynaklar.isEmpty()) {
                    MaterialAlertDialogBuilder(ctx)
                        .setTitle(R.string.src_err_title)
                        .setMessage(
                            sonuc.mesaj.ifBlank { getString(R.string.src_err_none) } +
                                "\n\n" + getString(R.string.src_err_tips)
                        )
                        .setPositiveButton(R.string.done, null)
                        .setNeutralButton(R.string.diag_run) { _, _ -> taniCalistir() }
                        .show()
                    return@runOnUiThread
                }
                sonuclariSun(ders, sonuc.kaynaklar, video)
            }
        }.start()
    }

    /** Bulunanları onay listesiyle gösterir — kullanıcı seçer, sonra kaydedilir. */
    private fun sonuclariSun(
        ders: Store.Lesson,
        kaynaklar: List<KaynakBulucu.Kaynak>,
        video: Boolean
    ) {
        val ctx = requireContext()
        // Ölü linkleri sona at
        val sirali = kaynaklar.sortedByDescending { it.dogrulandi == true }

        val etiketler = sirali.map { k ->
            val isaret = when (k.dogrulandi) {
                true -> "✓ "
                false -> "⚠ "
                else -> ""
            }
            val alt = when {
                k.kanal.isNotBlank() -> k.kanal
                else -> k.url.removePrefix("https://").removePrefix("http://").take(42)
            }
            "$isaret${k.baslik.take(70)}\n$alt"
        }.toTypedArray()
        val secimler = BooleanArray(sirali.size) { sirali[it].dogrulandi == true }

        MaterialAlertDialogBuilder(ctx)
            .setTitle(
                getString(
                    if (video) R.string.src_found_video else R.string.src_found_pdf,
                    sirali.size
                )
            )
            .setMultiChoiceItems(etiketler, secimler) { _, hangi, isaretli ->
                secimler[hangi] = isaretli
            }
            .setPositiveButton(R.string.src_add_selected) { _, _ ->
                val secili = sirali.filterIndexed { i, _ -> secimler[i] }
                if (secili.isEmpty()) {
                    Toast.makeText(ctx, R.string.src_none_selected, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                // PDF ise "link mi indir mi" sor
                val pdfler = secili.filter { it.tur == KaynakBulucu.Tur.PDF }
                if (pdfler.isNotEmpty()) {
                    pdfKaydetmeSekli(ders, secili)
                } else {
                    kaydet(ders, secili, indir = false)
                }
            }
            .setNeutralButton(R.string.src_open_first) { _, _ ->
                sirali.firstOrNull()?.let { linkAc(it.url) }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** Kullanıcı tercihi: PDF sadece link olarak mı kalsın, indirilsin mi? */
    private fun pdfKaydetmeSekli(ders: Store.Lesson, secili: List<KaynakBulucu.Kaynak>) {
        val ctx = requireContext()
        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.src_pdf_how_title)
            .setMessage(R.string.src_pdf_how_body)
            .setPositiveButton(R.string.src_pdf_link_only) { _, _ ->
                kaydet(ders, secili, indir = false)
            }
            .setNeutralButton(R.string.src_pdf_download) { _, _ ->
                kaydet(ders, secili, indir = true)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun kaydet(ders: Store.Lesson, secili: List<KaynakBulucu.Kaynak>, indir: Boolean) {
        val ctx = requireContext()
        var eklenen = 0
        var atlanan = 0
        secili.forEach { k ->
            val sonuc = Store.kaynakEkle(
                ctx, ders.id, k.baslik, k.url,
                when (k.tur) {
                    KaynakBulucu.Tur.PDF -> "pdf"
                    KaynakBulucu.Tur.VIDEO -> "video"
                    else -> "sayfa"
                },
                k.aciklama, k.kanal
            )
            if (sonuc != null) eklenen++ else atlanan++
            if (indir && k.tur == KaynakBulucu.Tur.PDF) indirmeBaslat(k)
        }
        yenile()
        val mesaj = if (atlanan > 0) {
            getString(R.string.src_added_with_dup, eklenen, atlanan)
        } else {
            getString(R.string.src_added, eklenen)
        }
        Toast.makeText(ctx, mesaj, Toast.LENGTH_LONG).show()
    }

    /** PDF'i sistem indirme yöneticisiyle indirir. */
    private fun indirmeBaslat(k: KaynakBulucu.Kaynak) {
        val ctx = context ?: return
        try {
            val ad = k.baslik.replace(Regex("[^A-Za-z0-9ğüşıöçĞÜŞİÖÇ ._-]"), "")
                .trim().take(60).ifBlank { "kaynak" } + ".pdf"
            val istek = android.app.DownloadManager.Request(Uri.parse(k.url)).apply {
                setTitle(ad)
                setDescription(getString(R.string.src_downloading))
                setNotificationVisibility(
                    android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                setDestinationInExternalPublicDir(
                    android.os.Environment.DIRECTORY_DOWNLOADS, ad
                )
                allowScanningByMediaScanner()
            }
            val yonetici = ctx.getSystemService(android.content.Context.DOWNLOAD_SERVICE)
                as android.app.DownloadManager
            yonetici.enqueue(istek)
            Toast.makeText(ctx, R.string.src_download_started, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.util.Log.w("Kaynaklar", "İndirme başlatılamadı", e)
            Toast.makeText(ctx, R.string.src_download_fail, Toast.LENGTH_LONG).show()
        }
    }

    private fun linkAc(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            android.util.Log.w("Kaynaklar", "Link açılamadı", e)
            Toast.makeText(requireContext(), R.string.src_open_fail, Toast.LENGTH_SHORT).show()
        }
    }

    private fun kaynakMenusu(satir: Satir) {
        val ctx = requireContext()
        val secenekler = arrayOf(
            getString(R.string.src_menu_open),
            getString(R.string.src_menu_copy),
            getString(R.string.src_menu_share),
            getString(R.string.src_menu_delete)
        )
        MaterialAlertDialogBuilder(ctx)
            .setTitle(satir.kaynak.baslik.take(60))
            .setItems(secenekler) { _, hangi ->
                when (hangi) {
                    0 -> linkAc(satir.kaynak.url)
                    1 -> {
                        val pano = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                            as android.content.ClipboardManager
                        pano.setPrimaryClip(
                            android.content.ClipData.newPlainText("url", satir.kaynak.url)
                        )
                        Toast.makeText(ctx, R.string.src_copied, Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        val g = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                satir.kaynak.baslik + "\n" + satir.kaynak.url
                            )
                        }
                        startActivity(Intent.createChooser(g, getString(R.string.src_menu_share)))
                    }
                    3 -> {
                        val silinen = Store.kaynakSil(ctx, satir.kaynak.id)
                        yenile()
                        if (silinen != null) geriAlSun(silinen)
                    }
                }
            }
            .show()
    }

    private fun geriAlSun(silinen: Store.DersKaynak) {
        val kok = view ?: return
        com.google.android.material.snackbar.Snackbar
            .make(kok, getString(R.string.src_deleted), com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
            .setAction(R.string.undo_action) {
                Store.kaynakGeriEkle(requireContext(), silinen)
                yenile()
            }
            .show()
    }

    // ─────────────────── Liste ───────────────────

    private inner class KaynakAdapter : RecyclerView.Adapter<KaynakAdapter.Holder>() {
        private val veri = mutableListOf<Satir>()

        fun submit(yeni: List<Satir>) {
            veri.clear()
            veri.addAll(yeni)
            notifyDataSetChanged()
        }

        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val ikon: TextView = view.findViewById(R.id.ikIcon)
            val baslik: TextView = view.findViewById(R.id.ikTitle)
            val alt: TextView = view.findViewById(R.id.ikSub)
            val ders: TextView = view.findViewById(R.id.ikLesson)
            val menu: TextView = view.findViewById(R.id.ikMenu)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
            Holder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_kaynak, parent, false)
            )

        override fun getItemCount(): Int = veri.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val s = veri[position]
            holder.ikon.text = when (s.kaynak.tur) {
                "video" -> "▶"
                "pdf" -> "📄"
                else -> "🔗"
            }
            holder.baslik.text = s.kaynak.baslik
            holder.alt.text = when {
                s.kaynak.kanal.isNotBlank() -> s.kaynak.kanal
                s.kaynak.aciklama.isNotBlank() -> s.kaynak.aciklama
                else -> s.kaynak.url.removePrefix("https://").removePrefix("http://").take(46)
            }
            holder.ders.text = buildString {
                append(s.dersAdi)
                if (s.kursAdi.isNotBlank()) append("  ·  ").append(s.kursAdi)
            }
            holder.itemView.setOnClickListener { linkAc(s.kaynak.url) }
            holder.itemView.setOnLongClickListener { kaynakMenusu(s); true }
            holder.menu.setOnClickListener { kaynakMenusu(s) }
        }
    }

    companion object {
        const val ARG_LESSON_ID = "lesson_id"

        fun yeni(lessonId: Long): KaynaklarFragment = KaynaklarFragment().apply {
            arguments = Bundle().apply { putLong(ARG_LESSON_ID, lessonId) }
        }
    }

    /**
     * v7.58: Üstten aşağı çekince yenileme.
     * onViewCreated yerine onStart'ta kuruluyor — görünüm ağacı
     * o an ebeveynine bağlanmış oluyor, sarmalama güvenli.
     */
    override fun onStart() {
        super.onStart()
        Yenileyici.kur(this) { yenile() }
        Yenileyici.gorunurluguEsitle(this)
    }
}
