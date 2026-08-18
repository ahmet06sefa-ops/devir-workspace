package com.gunlukasistan.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText

/**
 * v7.4 — "🏗️ Mühendislik" ekranı.
 *
 * Udemy düzeninde kurs listesi. Bir kursa dokununca bölümleri açılır,
 * bölüme dokununca dersleri. Her ders işaretlenebilir, notu ve linki olur.
 */
class CoursesFragment : Fragment(R.layout.fragment_courses) {

    private lateinit var adapter: CourseAdapter
    private lateinit var emptyView: View

    /** v7.5: PDF bağlama bir kez yapılır. */
    private var pdfLinked = false

    /** v7.7: arama metni (boşsa normal ağaç görünümü). */
    private var query = ""

    /** v7.8: yalnızca yer imli dersleri göster. */
    private var favOnly = false

    /** v7.12: en son açılan ders (hızlı devam için). */
    private var sonDersId = 0L

    /** Açık olan kursların kimlikleri (akordiyon durumu). */
    private val expandedCourses = mutableSetOf<Long>()
    private val expandedSections = mutableSetOf<Long>()

    /** Listede gösterilen düz satırlar. */
    private val rows = mutableListOf<Row>()

    sealed class Row {
        data class CourseRow(val course: Store.Course) : Row()
        data class SectionRow(val section: Store.Section, val courseColor: Int) : Row()
        data class LessonRow(val lesson: Store.Lesson) : Row()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // v8.6 · Öneri 29: yatay/tablet genişlik sınırı + yoğunluk
        Duzen.uygula(view)
        super.onViewCreated(view, savedInstanceState)

        emptyView = view.findViewById(R.id.coEmpty)
        val recycler = view.findViewById<RecyclerView>(R.id.coRecycler)
        adapter = CourseAdapter()
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        view.findViewById<FloatingActionButton>(R.id.coFab).apply {
            setOnClickListener { yeniKursMenusu() }
            // v7.9: uzun bas → günlük hatırlatıcı ayarı
            setOnLongClickListener { showReminderDialog(); true }
        }
        view.findViewById<View>(R.id.coInstall).setOnClickListener { installPack() }
        // v7.39: ders PDF'lerinin içinde tam metin arama
        view.findViewById<View>(R.id.coPdfSearch)?.setOnClickListener {
            PdfAramaActivity.ac(requireContext())
        }

        // v7.11: özete uzun bas → ayrıntılı istatistik
        view.findViewById<TextView>(R.id.coSummary).setOnLongClickListener {
            showStats(); true
        }

        // v7.8: yer imi filtresi
        view.findViewById<TextView>(R.id.coFavFilter).apply {
            setOnClickListener { v ->
                favOnly = !favOnly
                (v as TextView).text = getString(
                    if (favOnly) R.string.co_fav_filter_on else R.string.co_fav_filter_off
                )
                rebuild()
            }
            // v7.14: uzun bas → tüm ders notlarını göster
            setOnLongClickListener { showNotes(); true }
        }

        // v7.7: canlı arama
        view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.coSearch)
            .addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    query = s?.toString()?.trim().orEmpty()
                    rebuild()
                }
            })

        rebuild()
    }

    override fun onResume() {
        super.onResume()
        rebuild()
    }

    /** Hazır kurs paketini yükler. */
    private fun installPack() {
        val context = requireContext()
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.co_pack_title)
            .setMessage(
                getString(
                    R.string.co_pack_msg,
                    CoursePack.courses.size,
                    CoursePack.lessonCount(),
                    CoursePack.totalMinutes() / 60
                )
            )
            .setPositiveButton(R.string.co_pack_add) { _, _ ->
                val added = CoursePack.install(context)
                Toast.makeText(
                    context,
                    if (added > 0) getString(R.string.co_pack_ok, added)
                    else getString(R.string.co_pack_none),
                    Toast.LENGTH_SHORT
                ).show()
                rebuild()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** Listeyi baştan kurar (akordiyon durumuna göre). */
    private fun rebuild() {
        val context = context ?: return
        // v7.5: eski sürümde eklenmiş derslere PDF yollarını bağla (bir kez)
        if (!pdfLinked) {
            pdfLinked = true
            try { CoursePack.linkPdfs(context) } catch (_: Exception) {}
        }
        rows.clear()
        val courses = Store.loadCourses(context)
        devamKartiniYenile()
        tekrarKartiniYenile()

        if (favOnly) {
            // v7.8: yalnızca yer imli dersler
            val favs = Store.favLessons(context)
            for (course in courses) {
                val mine = favs.filter { it.courseId == course.id }.sortedBy { it.order }
                if (mine.isEmpty()) continue
                rows.add(Row.CourseRow(course))
                for (lesson in mine) rows.add(Row.LessonRow(lesson))
            }
            adapter.notifyDataSetChanged()
            emptyView.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
            view?.findViewById<View>(R.id.coRecycler)?.visibility =
                if (rows.isEmpty()) View.GONE else View.VISIBLE
            view?.findViewById<TextView>(R.id.coSummary)?.text =
                if (favs.isEmpty()) getString(R.string.co_fav_empty)
                else getString(R.string.co_fav_count, favs.size)
            return
        }

        if (query.length >= 2) {
            // v7.7: arama modu — eşleşen dersler düz liste olarak
            val q = query.lowercase(java.util.Locale("tr", "TR"))
            val allLessons = Store.loadLessons(context)
            val eslesen = allLessons.filter {
                it.title.lowercase(java.util.Locale("tr", "TR")).contains(q) ||
                    it.desc.lowercase(java.util.Locale("tr", "TR")).contains(q)
            }.sortedBy { it.order }
            // kursa göre grupla
            for (course in courses) {
                val mine = eslesen.filter { it.courseId == course.id }
                if (mine.isEmpty()) continue
                rows.add(Row.CourseRow(course))
                for (lesson in mine) rows.add(Row.LessonRow(lesson))
            }
            adapter.notifyDataSetChanged()
            emptyView.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
            view?.findViewById<View>(R.id.coRecycler)?.visibility =
                if (rows.isEmpty()) View.GONE else View.VISIBLE
            view?.findViewById<TextView>(R.id.coSummary)?.text =
                if (eslesen.isEmpty()) getString(R.string.co_search_none, query)
                else getString(R.string.co_search_found, eslesen.size)
            return
        }

        for (course in courses) {
            rows.add(Row.CourseRow(course))
            if (!expandedCourses.contains(course.id)) continue
            for (section in Store.sectionsOf(context, course.id)) {
                rows.add(Row.SectionRow(section, course.color))
                if (!expandedSections.contains(section.id)) continue
                for (lesson in Store.lessonsOf(context, section.id)) {
                    rows.add(Row.LessonRow(lesson))
                }
            }
        }
        adapter.notifyDataSetChanged()

        val empty = courses.isEmpty()
        emptyView.visibility = if (empty) View.VISIBLE else View.GONE
        view?.findViewById<View>(R.id.coRecycler)?.visibility =
            if (empty) View.GONE else View.VISIBLE

        // Üst özet
        val allLessons = Store.loadLessons(context)
        val done = allLessons.count { it.done }
        view?.findViewById<TextView>(R.id.coSummary)?.text = if (allLessons.isEmpty()) {
            getString(R.string.co_summary_empty)
        } else {
            val pct = done * 100 / allLessons.size
            getString(R.string.co_summary, courses.size, done, allLessons.size, pct)
        }
    }

    // ─────────────────────────── Kurs düzenleyici ───────────────────────────

    /**
     * v7.21: + düğmesi — yapay zekâ ile mi elle mi kurs eklenecek?
     * Yapay zekâ seçeneği Kaynak Merkezi'ndeki üreticiyi açar.
     */
    private fun yeniKursMenusu() {
        val context = requireContext()
        val secenekler = arrayOf(
            getString(R.string.gen_new_course),
            getString(R.string.co_new_manual)
        )
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.co_new_course)
            .setItems(secenekler) { _, hangi ->
                if (hangi == 0) {
                    (activity as? MainActivity)?.openKaynaklar()
                } else {
                    showCourseEditor(null)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    fun showCourseEditor(existing: Store.Course?) {
        val context = requireContext()
        val dialogView = layoutInflater.inflate(R.layout.dialog_course, null)
        val titleInput = dialogView.findViewById<TextInputEditText>(R.id.dcTitle)
        val emojiInput = dialogView.findViewById<TextInputEditText>(R.id.dcEmoji)
        val descInput = dialogView.findViewById<TextInputEditText>(R.id.dcDesc)

        existing?.let {
            titleInput.setText(it.title)
            emojiInput.setText(it.emoji)
            descInput.setText(it.desc)
        }
        if (existing == null) emojiInput.setText("📘")

        MaterialAlertDialogBuilder(context)
            .setTitle(if (existing == null) R.string.co_new_course else R.string.co_edit_course)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val title = titleInput.text?.toString()?.trim().orEmpty()
                if (title.isEmpty()) return@setPositiveButton
                val emoji = emojiInput.text?.toString()?.trim()?.ifBlank { "📘" } ?: "📘"
                val desc = descInput.text?.toString()?.trim().orEmpty()
                if (existing == null) {
                    val color = Store.loadCourses(context).size % 7
                    Store.addCourse(context, title, emoji, color, desc)
                } else {
                    val list = Store.loadCourses(context)
                    list.firstOrNull { it.id == existing.id }?.apply {
                        this.title = title; this.emoji = emoji; this.desc = desc
                    }
                    Store.saveCourses(context, list)
                }
                rebuild()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showSectionEditor(courseId: Long, existing: Store.Section?) {
        val context = requireContext()
        val input = TextInputEditText(context).apply {
            hint = getString(R.string.co_section_hint)
            setText(existing?.title ?: "")
            setPadding(48, 36, 48, 36)
        }
        MaterialAlertDialogBuilder(context)
            .setTitle(if (existing == null) R.string.co_new_section else R.string.co_edit_section)
            .setView(input)
            .setPositiveButton(R.string.save) { _, _ ->
                val title = input.text?.toString()?.trim().orEmpty()
                if (title.isEmpty()) return@setPositiveButton
                if (existing == null) {
                    Store.addSection(context, courseId, title)
                    expandedCourses.add(courseId)
                } else {
                    val list = Store.loadSections(context)
                    list.firstOrNull { it.id == existing.id }?.title = title
                    Store.saveSections(context, list)
                }
                rebuild()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showLessonEditor(courseId: Long, sectionId: Long, existing: Store.Lesson?) {
        val context = requireContext()
        val dialogView = layoutInflater.inflate(R.layout.dialog_lesson, null)
        val titleInput = dialogView.findViewById<TextInputEditText>(R.id.dlTitle)
        val minInput = dialogView.findViewById<TextInputEditText>(R.id.dlMinutes)
        val descInput = dialogView.findViewById<TextInputEditText>(R.id.dlDesc)
        val linkInput = dialogView.findViewById<TextInputEditText>(R.id.dlLink)
        val noteInput = dialogView.findViewById<TextInputEditText>(R.id.dlNote)

        existing?.let {
            titleInput.setText(it.title)
            if (it.minutes > 0) minInput.setText(it.minutes.toString())
            descInput.setText(it.desc)
            linkInput.setText(it.link)
            noteInput.setText(it.note)
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(if (existing == null) R.string.co_new_lesson else R.string.co_edit_lesson)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val title = titleInput.text?.toString()?.trim().orEmpty()
                if (title.isEmpty()) return@setPositiveButton
                val minutes = minInput.text?.toString()?.trim()?.toIntOrNull() ?: 0
                val desc = descInput.text?.toString()?.trim().orEmpty()
                val link = linkInput.text?.toString()?.trim().orEmpty()
                val note = noteInput.text?.toString()?.trim().orEmpty()
                if (existing == null) {
                    val lesson = Store.addLesson(context, courseId, sectionId, title, minutes, desc)
                    if (link.isNotEmpty() || note.isNotEmpty()) {
                        val list = Store.loadLessons(context)
                        list.firstOrNull { it.id == lesson.id }?.apply {
                            this.link = link; this.note = note
                        }
                        Store.saveLessons(context, list)
                    }
                    expandedSections.add(sectionId)
                } else {
                    val list = Store.loadLessons(context)
                    list.firstOrNull { it.id == existing.id }?.apply {
                        this.title = title; this.minutes = minutes; this.desc = desc
                        this.link = link; this.note = note
                    }
                    Store.saveLessons(context, list)
                }
                rebuild()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** v7.5: dersin PDF'ini açar. */
    private fun openLessonPdf(lesson: Store.Lesson) {
        val context = requireContext()
        sonDersId = lesson.id
        Store.setSonDers(context, lesson.id)
        val intent = android.content.Intent(context, LessonPdfActivity::class.java).apply {
            putExtra(LessonPdfActivity.EXTRA_ASSET, lesson.pdfAsset)
            putExtra(LessonPdfActivity.EXTRA_TITLE, "${lesson.order}. ${lesson.title}")
            val sec = Store.loadSections(context).firstOrNull { it.id == lesson.sectionId }
            val crs = Store.loadCourses(context).firstOrNull { it.id == lesson.courseId }
            putExtra(
                LessonPdfActivity.EXTRA_SUB,
                listOfNotNull(crs?.title, sec?.title).joinToString(" · ")
            )
            putExtra(LessonPdfActivity.EXTRA_LESSON_ID, lesson.id)
        }
        startActivity(intent)
    }

    /** Derse dokununca: PDF varsa okuyucu, yoksa ayrıntı penceresi. */
    private fun showLessonDetail(lesson: Store.Lesson) {
        if (CoursePack.assetExists(requireContext(), lesson.pdfAsset)) {
            openLessonPdf(lesson)
        } else {
            showLessonDetailDialog(lesson)
        }
    }

    /** Ders ayrıntı penceresi (açıklama, not, link). */
    private fun showLessonDetailDialog(lesson: Store.Lesson) {
        val context = requireContext()
        val sb = StringBuilder()
        if (lesson.desc.isNotBlank()) sb.append(lesson.desc).append("\n\n")
        if (lesson.minutes > 0) sb.append("⏱️ ${lesson.minutes} dakika\n")
        if (lesson.note.isNotBlank()) sb.append("\n📝 Notun:\n${lesson.note}\n")
        if (lesson.link.isNotBlank()) sb.append("\n🔗 ${lesson.link}")
        if (sb.isBlank()) sb.append(getString(R.string.co_no_detail))

        val builder = MaterialAlertDialogBuilder(context)
            .setTitle((if (lesson.done) "✓ " else "") + lesson.title)
            .setMessage(sb.toString())
            .setPositiveButton(
                if (lesson.done) R.string.co_mark_undone else R.string.co_mark_done
            ) { _, _ ->
                Store.toggleLesson(context, lesson.id)
                rebuild()
            }
            .setNeutralButton(R.string.co_edit) { _, _ ->
                showLessonEditor(lesson.courseId, lesson.sectionId, lesson)
            }

        if (CoursePack.assetExists(context, lesson.pdfAsset)) {
            builder.setNegativeButton(R.string.co_open_pdf) { _, _ -> openLessonPdf(lesson) }
        } else if (lesson.link.isNotBlank()) {
            builder.setNegativeButton(R.string.co_open_link) { _, _ ->
                try {
                    var url = lesson.link
                    if (!url.startsWith("http")) url = "https://$url"
                    startActivity(
                        android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(url)
                        )
                    )
                } catch (_: Exception) {
                    Toast.makeText(context, R.string.co_link_error, Toast.LENGTH_SHORT).show()
                }
            }
        }
        builder.show()
    }

    // ─────────────────────────── Adapter ───────────────────────────

    private inner class CourseAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun getItemViewType(position: Int): Int = when (rows[position]) {
            is Row.CourseRow -> 0
            is Row.SectionRow -> 1
            is Row.LessonRow -> 2
        }

        override fun getItemCount(): Int = rows.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return when (viewType) {
                0 -> CourseVH(inflater.inflate(R.layout.item_course, parent, false))
                1 -> SectionVH(inflater.inflate(R.layout.item_course_section, parent, false))
                else -> LessonVH(inflater.inflate(R.layout.item_course_lesson, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val row = rows[position]) {
                is Row.CourseRow -> (holder as CourseVH).bind(row.course)
                is Row.SectionRow -> (holder as SectionVH).bind(row.section)
                is Row.LessonRow -> (holder as LessonVH).bind(row.lesson)
            }
        }

        inner class CourseVH(view: View) : RecyclerView.ViewHolder(view) {
            private val emoji: TextView = view.findViewById(R.id.icEmoji)
            private val title: TextView = view.findViewById(R.id.icTitle)
            private val desc: TextView = view.findViewById(R.id.icDesc)
            private val meta: TextView = view.findViewById(R.id.icMeta)
            private val chevron: TextView = view.findViewById(R.id.icChevron)
            private val bar: View = view.findViewById(R.id.icBar)
            private val barBg: View = view.findViewById(R.id.icBarBg)

            fun bind(course: Store.Course) {
                val context = itemView.context
                emoji.text = course.emoji
                title.text = course.title
                desc.text = course.desc
                desc.visibility = if (course.desc.isBlank()) View.GONE else View.VISIBLE

                val (done, total) = Store.courseProgress(context, course.id)
                val (doneMin, totalMin) = Store.courseMinutes(context, course.id)
                val pct = if (total == 0) 0 else done * 100 / total
                meta.text = context.getString(
                    R.string.co_course_meta, done, total, pct, totalMin / 60, totalMin % 60
                )

                // İlerleme çubuğu
                barBg.post {
                    val w = barBg.width
                    val lp = bar.layoutParams
                    lp.width = if (total == 0) 0 else w * pct / 100
                    bar.layoutParams = lp
                }
                val palette = ThemeManager.NEON_PALETTE
                bar.setBackgroundColor(palette[course.color % palette.size])

                val open = expandedCourses.contains(course.id)
                chevron.text = if (open) "⌄" else "›"

                itemView.setOnClickListener {
                    if (open) expandedCourses.remove(course.id)
                    else expandedCourses.add(course.id)
                    rebuild()
                }
                itemView.setOnLongClickListener {
                    showCourseMenu(course); true
                }
            }
        }

        inner class SectionVH(view: View) : RecyclerView.ViewHolder(view) {
            private val title: TextView = view.findViewById(R.id.isTitle)
            private val meta: TextView = view.findViewById(R.id.isMeta)
            private val chevron: TextView = view.findViewById(R.id.isChevron)

            fun bind(section: Store.Section) {
                val context = itemView.context
                title.text = section.title
                val lessons = Store.lessonsOf(context, section.id)
                val done = lessons.count { it.done }
                // v7.13: bölüm ilerlemesini yüzde ve görsel çubukla göster
                val pct = if (lessons.isEmpty()) 0 else done * 100 / lessons.size
                val dolu = pct / 10
                val cubuk = "▰".repeat(dolu) + "▱".repeat(10 - dolu)
                meta.text = context.getString(
                    R.string.co_section_meta2, done, lessons.size, cubuk, pct
                )

                val open = expandedSections.contains(section.id)
                chevron.text = if (open) "⌄" else "›"

                itemView.setOnClickListener {
                    if (open) expandedSections.remove(section.id)
                    else expandedSections.add(section.id)
                    rebuild()
                }
                itemView.setOnLongClickListener {
                    showSectionMenu(section); true
                }
            }
        }

        inner class LessonVH(view: View) : RecyclerView.ViewHolder(view) {
            private val check: ImageView = view.findViewById(R.id.ilCheck)
            private val title: TextView = view.findViewById(R.id.ilTitle)
            private val meta: TextView = view.findViewById(R.id.ilMeta)

            fun bind(lesson: Store.Lesson) {
                val context = itemView.context
                title.text = lesson.title
                title.paintFlags = if (lesson.done) {
                    title.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    title.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                }
                title.alpha = if (lesson.done) 0.5f else 1f

                val parts = mutableListOf<String>()
                if (lesson.minutes > 0) parts.add("⏱️ ${lesson.minutes} dk")
                if (lesson.fav) parts.add("⭐")
                if (CoursePack.assetExists(context, lesson.pdfAsset)) parts.add("📄 PDF")
                if (lesson.note.isNotBlank()) parts.add("📝")
                if (lesson.link.isNotBlank()) parts.add("🔗")
                meta.text = parts.joinToString("  ")
                meta.visibility = if (parts.isEmpty()) View.GONE else View.VISIBLE

                check.setImageResource(
                    if (lesson.done) R.drawable.ic_check_circle else R.drawable.ic_circle_outline
                )
                check.setOnClickListener {
                    Store.toggleLesson(context, lesson.id)
                    rebuild()
                }
                itemView.setOnClickListener { showLessonDetail(lesson) }
                itemView.setOnLongClickListener {
                    showLessonMenu(lesson); true
                }
            }
        }
    }

    // ─────────────────────────── Uzun basma menüleri ───────────────────────────

    private fun showCourseMenu(course: Store.Course) {
        val context = requireContext()
        val options = arrayOf(
            getString(R.string.co_add_section),
            getString(R.string.co_edit),
            getString(R.string.delete)
        )
        MaterialAlertDialogBuilder(context)
            .setTitle("${course.emoji} ${course.title}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showSectionEditor(course.id, null)
                    1 -> showCourseEditor(course)
                    2 -> confirmDelete(getString(R.string.co_del_course_q)) {
                        Store.deleteCourseUndoable(context, course.id)
                        rebuild()
                        geriAlSun(getString(R.string.undo_course))
                    }
                }
            }
            .show()
    }

    private fun showSectionMenu(section: Store.Section) {
        val context = requireContext()
        val options = arrayOf(
            getString(R.string.co_add_lesson),
            getString(R.string.co_edit),
            getString(R.string.delete)
        )
        MaterialAlertDialogBuilder(context)
            .setTitle(section.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showLessonEditor(section.courseId, section.id, null)
                    1 -> showSectionEditor(section.courseId, section)
                    2 -> confirmDelete(getString(R.string.co_del_section_q)) {
                        Store.deleteSectionUndoable(context, section.id)
                        rebuild()
                        geriAlSun(getString(R.string.undo_section))
                    }
                }
            }
            .show()
    }

    private fun showLessonMenu(lesson: Store.Lesson) {
        val context = requireContext()
        val hasPdf = CoursePack.assetExists(context, lesson.pdfAsset)
        val favLabel = getString(
            if (lesson.fav) R.string.co_unfav else R.string.co_fav
        )
        // v7.20: kaynak sayısı etiketi
        val kaynakSayi = Store.kaynaklariOf(context, lesson.id).size
        val kaynakLabel = if (kaynakSayi > 0) {
            getString(R.string.co_sources_n, kaynakSayi)
        } else {
            getString(R.string.co_sources_find)
        }

        // Seçenekleri eylemlerle birlikte kur — indeks kayması olmasın
        val etiketler = mutableListOf<String>()
        val eylemler = mutableListOf<() -> Unit>()

        if (hasPdf) {
            etiketler.add(getString(R.string.co_detail))
            eylemler.add { showLessonDetailDialog(lesson) }
        }
        etiketler.add(favLabel)
        eylemler.add {
            val yeni = Store.toggleLessonFav(context, lesson.id)
            rebuild()
            Toast.makeText(
                context,
                getString(if (yeni) R.string.co_fav_added else R.string.co_fav_removed),
                Toast.LENGTH_SHORT
            ).show()
        }
        etiketler.add(kaynakLabel)
        eylemler.add { (activity as? MainActivity)?.openKaynaklar(lesson.id) }

        // v7.29: quiz — soru varsa çöz, yoksa üret
        val soruVar = QuizStore.soruVarMi(context, lesson.id)
        val enIyi = QuizStore.enIyiSonuc(context, lesson.id)
        val quizLabel = when {
            enIyi != null -> getString(R.string.co_quiz_best, enIyi.yuzde)
            soruVar -> getString(R.string.co_quiz_start)
            else -> getString(R.string.co_quiz_create)
        }
        etiketler.add(quizLabel)
        eylemler.add { quizAc(lesson, soruVar) }

        // v7.37: özel öğretmen modu — anlatır, sorar, seviyeye göre ayarlar
        val ogr = OgretmenStore.oturum(context, lesson.id)
        val ogrLabel = when {
            ogr != null && ogr.tamamlandi -> getString(R.string.tut_menu_done, ogr.yuzde)
            ogr != null && ogr.adim > 0 -> getString(R.string.tut_menu_resume, ogr.adim + 1)
            else -> getString(R.string.tut_menu)
        }
        etiketler.add(ogrLabel)
        eylemler.add {
            OgretmenActivity.ac(context, lesson.id, lesson.title, lesson.pdfAsset)
        }
        etiketler.add(getString(R.string.co_edit))
        eylemler.add { showLessonEditor(lesson.courseId, lesson.sectionId, lesson) }
        etiketler.add(getString(R.string.delete))
        eylemler.add {
            confirmDelete(getString(R.string.co_del_lesson_q)) {
                Store.deleteLessonUndoable(context, lesson.id)
                rebuild()
                geriAlSun(getString(R.string.undo_lesson))
            }
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(lesson.title)
            .setItems(etiketler.toTypedArray()) { _, which ->
                eylemler.getOrNull(which)?.invoke()
            }
            .show()
    }

    /** v7.11: kurs ilerleme istatistikleri penceresi. */
    /**
     * v7.29: Dersin quizini açar. Soru yoksa yapay zekâ ile üretir.
     */
    private fun quizAc(lesson: Store.Lesson, soruVar: Boolean) {
        val context = requireContext()
        if (soruVar) {
            QuizActivity.ac(context, lesson.id, lesson.title)
            return
        }
        if (!AiSettings.isReady(context)) {
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.quiz_title)
                .setMessage(R.string.quiz_need_ai)
                .setPositiveButton(R.string.ocr_open_settings) { _, _ ->
                    (activity as? MainActivity)?.openSettings()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
            return
        }

        val kurs = Store.loadCourses(context)
            .firstOrNull { it.id == lesson.courseId }?.title.orEmpty()
        val bolum = Store.loadSections(context)
            .firstOrNull { it.id == lesson.sectionId }?.title.orEmpty()

        val bekleme = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.quiz_creating)
            .setMessage(R.string.quiz_creating_body)
            .setCancelable(false)
            .show()

        Thread {
            val sonuc = QuizUretici.uret(context, lesson, kurs, bolum, 5)
            activity?.runOnUiThread {
                try {
                    bekleme.dismiss()
                } catch (_: Exception) {
                }
                if (!sonuc.ok) {
                    MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.quiz_err_title)
                        .setMessage(sonuc.mesaj)
                        .setPositiveButton(R.string.done, null)
                        .show()
                    return@runOnUiThread
                }
                Toast.makeText(
                    context,
                    getString(R.string.quiz_created, sonuc.sorular.size),
                    Toast.LENGTH_SHORT
                ).show()
                QuizActivity.ac(context, lesson.id, lesson.title)
            }
        }.start()
    }

    /** v7.29: Tekrar kartını günceller — bekleyen tekrar yoksa gizlenir. */
    private fun tekrarKartiniYenile() {
        val kok = view ?: return
        val kart = kok.findViewById<com.google.android.material.card.MaterialCardView>(
            R.id.coReviewCard
        ) ?: return
        val context = context ?: return

        val adet = if (favOnly || query.length >= 2) 0 else QuizStore.tekrarSayisi(context)
        if (adet == 0) {
            kart.visibility = View.GONE
            return
        }
        kart.visibility = View.VISIBLE
        kok.findViewById<TextView>(R.id.coReviewText).text =
            getString(R.string.rev_count, adet)
        kart.setOnClickListener { tekrarListesi() }
    }

    /** v7.29: Bugün tekrar edilecek dersleri listeler. */
    private fun tekrarListesi() {
        val context = requireContext()
        val idler = QuizStore.bugunTekrarEdilecekler(context)
        if (idler.isEmpty()) {
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.rev_title)
                .setMessage(R.string.rev_empty)
                .setPositiveButton(R.string.done, null)
                .show()
            return
        }
        val dersler = Store.loadLessons(context).filter { it.id in idler }
        if (dersler.isEmpty()) return

        val etiketler = dersler.map { d ->
            val t = QuizStore.tekrarDurumu(context, d.id)
            val kutu = t?.kutu ?: 0
            "${d.title}\n     ${getString(R.string.rev_box, kutu + 1)}"
        }.toTypedArray()

        MaterialAlertDialogBuilder(context)
            .setTitle(getString(R.string.rev_today, dersler.size))
            .setItems(etiketler) { _, hangi ->
                val d = dersler[hangi]
                if (QuizStore.soruVarMi(context, d.id)) {
                    QuizActivity.ac(context, d.id, d.title, tekrarModu = true)
                } else {
                    quizAc(d, false)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * v7.16: "Kaldığın yerden devam et" kartını günceller.
     * Son açılan ders (yoksa ilk tamamlanmamış ders) tek dokunuşla açılır.
     */
    private fun devamKartiniYenile() {
        val kok = view ?: return
        val kart = kok.findViewById<com.google.android.material.card.MaterialCardView>(
            R.id.coResumeCard
        ) ?: return
        val context = context ?: return

        // Arama veya filtre açıkken kart gizlensin — ekran kalabalık olmasın
        val ders = if (favOnly || query.length >= 2) null else Store.sonDers(context)
        if (ders == null) {
            kart.visibility = View.GONE
            return
        }

        val kurs = Store.loadCourses(context).firstOrNull { it.id == ders.courseId }
        val bolum = Store.loadSections(context).firstOrNull { it.id == ders.sectionId }

        kart.visibility = View.VISIBLE
        kok.findViewById<TextView>(R.id.coResumeLabel).text = getString(
            if (ders.done) R.string.co_resume_done else R.string.co_resume_title
        )
        kok.findViewById<TextView>(R.id.coResumeTitle).text = ders.title
        kok.findViewById<TextView>(R.id.coResumeSub).text = buildString {
            kurs?.let { append(it.title) }
            bolum?.let { if (isNotEmpty()) append(" · "); append(it.title) }
            if (ders.minutes > 0) {
                if (isNotEmpty()) append(" · ")
                append(getString(R.string.co_resume_min, ders.minutes))
            }
        }
        kart.setOnClickListener {
            if (ders.pdfAsset.isNotBlank()) openLessonPdf(ders) else showLessonDetail(ders)
        }
        kart.setOnLongClickListener { showLessonDetail(ders); true }
    }

    /**
     * v7.15: son 7 günün çalışma haritasını görsel çubuk olarak yazar.
     * Gün adları bugüne göre dinamik üretilir (liste sonu = bugün).
     */
    private fun seriCubugu(gunler: List<Boolean>): String {
        val bicim = java.text.SimpleDateFormat("EEE", java.util.Locale("tr", "TR"))
        val takvim = java.util.Calendar.getInstance()
        val ust = StringBuilder()
        val alt = StringBuilder()
        val n = gunler.size
        gunler.forEachIndexed { i, calisti ->
            takvim.timeInMillis = System.currentTimeMillis()
            takvim.add(java.util.Calendar.DAY_OF_YEAR, -(n - 1 - i))
            val ad = bicim.format(takvim.time).take(2)
            if (i > 0) { ust.append("  "); alt.append(' ') }
            ust.append(if (calisti) "▣" else "▢")
            alt.append(ad)
        }
        return ust.toString() + "\n" + alt.toString()
    }

    /** v7.14: notu olan tüm dersleri tek listede gösterir. */
    private fun showNotes() {
        val context = requireContext()
        val notlu = Store.notluDersler(context)
        if (notlu.isEmpty()) {
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.co_notes_title)
                .setMessage(R.string.co_notes_empty)
                .setPositiveButton(R.string.ok_close, null)
                .show()
            return
        }
        val kurslar = Store.loadCourses(context).associateBy { it.id }
        val govde = buildString {
            notlu.forEachIndexed { i, ders ->
                if (i > 0) append("\n\n")
                val kursAdi = kurslar[ders.courseId]?.title.orEmpty()
                if (kursAdi.isNotBlank()) append("[").append(kursAdi).append("]\n")
                append("• ").append(ders.title).append("\n")
                append(ders.note)
            }
        }
        val basliklar = notlu.map { it.title }.toTypedArray()
        MaterialAlertDialogBuilder(context)
            .setTitle(getString(R.string.co_notes_title) + " (" + notlu.size + ")")
            .setItems(basliklar) { _, hangi ->
                val secilen = notlu[hangi]
                MaterialAlertDialogBuilder(context)
                    .setTitle(secilen.title)
                    .setMessage(secilen.note)
                    .setPositiveButton(R.string.ok_close, null)
                    .setNegativeButton(R.string.co_note_open) { _, _ ->
                        if (secilen.pdfAsset.isNotBlank()) openLessonPdf(secilen)
                        else showLessonDetail(secilen)
                    }
                    .show()
            }
            .setNeutralButton(R.string.co_note_share) { _, _ ->
                try {
                    val gonder = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_SUBJECT,
                            getString(R.string.co_notes_title))
                        putExtra(android.content.Intent.EXTRA_TEXT, govde)
                    }
                    startActivity(android.content.Intent.createChooser(
                        gonder, getString(R.string.co_note_share)))
                } catch (e: Exception) {
                    android.util.Log.w("Courses", "Not paylaşımı başarısız", e)
                    Toast.makeText(context, R.string.lp_share_error, Toast.LENGTH_SHORT).show()
                }
            }
            .setPositiveButton(R.string.ok_close, null)
            .show()
    }

    private fun showStats() {
        val context = requireContext()
        val ist = Store.kursIstatistik(context)
        if (ist.toplamDers == 0) {
            Toast.makeText(context, R.string.co_summary_empty, Toast.LENGTH_SHORT).show()
            return
        }
        val gunluk = 30  // günde 30 dakika varsayımı
        val govde = buildString {
            append(getString(R.string.st_lessons, ist.bitenDers, ist.toplamDers, ist.yuzde))
            append("\n\n")
            append(getString(
                R.string.st_time,
                ist.bitenDakika / 60, ist.bitenDakika % 60,
                ist.toplamDakika / 60, ist.toplamDakika % 60
            ))
            append("\n")
            append(getString(R.string.st_remaining, ist.kalanDakika / 60, ist.kalanDakika % 60))
            append("\n\n")
            append(getString(R.string.st_forecast, gunluk, ist.kalanGun(gunluk)))
            append("\n\n")
            append(getString(R.string.st_bookmark, ist.yerImi))
            append("\n")
            append(getString(R.string.st_pdf, ist.pdfliDers))

            // v7.15: çalışma serisi
            val seri = Store.kursSeri(context)
            append("\n\n")
            append(getString(R.string.st_streak, seri.gunSayisi))
            append("\n")
            append(getString(R.string.st_streak_record, seri.rekor))
            append("\n")
            append(seriCubugu(Store.kursSonYediGun(context)))
            append("\n")
            append(
                when {
                    seri.bugunCalisildi -> getString(R.string.st_streak_today_ok)
                    seri.gunSayisi > 0 -> getString(R.string.st_streak_today_pending)
                    else -> getString(R.string.st_streak_start)
                }
            )
        }
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.st_title)
            .setMessage(govde)
            .setPositiveButton(R.string.ok_close, null)
            .show()
    }

    /** v7.9: günlük ders hatırlatıcısı ayar penceresi. */
    private fun showReminderDialog() {
        val context = requireContext()
        val acik = CourseReminderReceiver.isEnabled(context)
        val secenekler = arrayOf(
            if (acik) getString(R.string.cr_off) else getString(R.string.cr_enable),
            getString(R.string.cr_time, CourseReminderReceiver.timeLabel(context))
        )
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.cr_dialog_title)
            .setItems(secenekler) { _, which ->
                when (which) {
                    0 -> {
                        CourseReminderReceiver.setEnabled(context, !acik)
                        Toast.makeText(
                            context,
                            if (!acik) getString(
                                R.string.cr_on,
                                CourseReminderReceiver.timeLabel(context)
                            ) else getString(R.string.cr_off),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    1 -> {
                        android.app.TimePickerDialog(
                            context,
                            { _, h, m ->
                                CourseReminderReceiver.setTime(context, h, m)
                                if (!CourseReminderReceiver.isEnabled(context)) {
                                    CourseReminderReceiver.setEnabled(context, true)
                                }
                                Toast.makeText(
                                    context,
                                    getString(
                                        R.string.cr_on,
                                        CourseReminderReceiver.timeLabel(context)
                                    ),
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            CourseReminderReceiver.hour(context),
                            CourseReminderReceiver.minute(context),
                            true
                        ).show()
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** v7.7: silme sonrası "Geri Al" şeridi gösterir. */
    private fun geriAlSun(mesaj: String) {
        val kok = view ?: return
        com.google.android.material.snackbar.Snackbar
            .make(kok, mesaj, com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
            .setAction(R.string.undo_action) {
                if (Store.geriAl()) {
                    rebuild()
                    Toast.makeText(requireContext(), R.string.undo_ok, Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun confirmDelete(message: String, onConfirm: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete)
            .setMessage(message)
            .setPositiveButton(R.string.delete) { _, _ -> onConfirm() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * v7.58: Üstten aşağı çekince yenileme.
     * onViewCreated yerine onStart'ta kuruluyor — görünüm ağacı
     * o an ebeveynine bağlanmış oluyor, sarmalama güvenli.
     */
    override fun onStart() {
        super.onStart()
        Yenileyici.kur(this) { rebuild() }
        Yenileyici.gorunurluguEsitle(this)
    }
}
