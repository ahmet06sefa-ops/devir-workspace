package com.gunlukasistan.app

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * KPSS deneme sınavı net takibi: grafik, ortalama, en iyi sonuç ve geçmiş listesi.
 */
class ExamsFragment : Fragment(R.layout.fragment_exams) {

    /** v8.3: zengin boş durum düzeni. */
    private var bosDurum: android.view.View? = null

    private lateinit var adapter: ExamsAdapter
    private val exams = mutableListOf<Store.Exam>()
    private val turkish = Locale("tr", "TR")

    /** v7.7: liste satırlarında tekrar oluşturmamak için (performans). */
    private val examDateFormatter = SimpleDateFormat("d MMMM yyyy", turkish)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // v8.6 · Öneri 29: yatay/tablet genişlik sınırı + yoğunluk
        Duzen.uygula(view)
        val recycler = view.findViewById<RecyclerView>(R.id.recycler)
        adapter = ExamsAdapter(exams, onDelete = { exam -> confirmDelete(exam) })
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    private fun reload() {
        val view = view ?: return
        // v10.1 · Öneri 13: sınav silinince akordeon kapanması — kopya
        // güncellemeden ÖNCE alınır (DiffUtil iki hâli karşılaştırır).
        val eskiListe = exams.toList()
        exams.clear()
        exams.addAll(Store.loadExams(requireContext()).sortedByDescending { it.createdAt })
        ListeFark.uygula(adapter, eskiListe, exams, ayniOge = { a, b -> a.id == b.id })

        // v8.3 · Öneri 11: zengin boş durum
        val bosGorunum = view.findViewById<TextView>(R.id.examsEmpty)
        if (bosDurum == null) {
            bosDurum = BosEkran.kur(
                bosGorunum, BosEkran.Tur.SINAV,
                getString(R.string.be_sinav_baslik),
                getString(R.string.be_sinav_aciklama),
                getString(R.string.be_sinav_eylem)
            ) { showExamEditor() }
        }
        bosGorunum.visibility = View.GONE
        BosEkran.goster(bosDurum, exams.isEmpty())

        val chart = view.findViewById<NetChartView>(R.id.netChart)
        val summary = view.findViewById<TextView>(R.id.examsSummary)
        if (exams.isEmpty()) {
            chart.visibility = View.GONE
            summary.visibility = View.GONE
        } else {
            chart.visibility = View.VISIBLE
            summary.visibility = View.VISIBLE
            chart.setData(exams.sortedBy { it.createdAt }.map { it.totalNet })

            val avg = exams.map { it.totalNet }.average().toInt()
            val best = exams.maxOf { it.totalNet }
            val last = exams.firstOrNull()?.totalNet ?: 0
            summary.text = getString(
                R.string.exams_summary_format, exams.size, avg, best, last
            )
        }
    }

    /** + menüsünden çağrılır: yeni deneme sonucu giriş penceresi. */
    fun showExamEditor() {
        val ctx = requireContext()
        val density = resources.displayMetrics.density
        val pad = (16 * density).toInt()

        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, 0)
        }
        val titleInput = EditText(ctx).apply {
            hint = getString(R.string.exam_title_hint)
            inputType = InputType.TYPE_CLASS_TEXT
        }
        container.addView(titleInput)

        val inputs = LinkedHashMap<String, EditText>()
        Store.EXAM_SUBJECTS.forEach { subject ->
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            val label = TextView(ctx).apply {
                text = subject
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val input = EditText(ctx).apply {
                hint = "0"
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
                setEms(4)
            }
            row.addView(label)
            row.addView(input)
            container.addView(row)
            inputs[subject] = input
        }

        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.exam_new_title)
            .setView(container)
            .setPositiveButton(R.string.save) { _, _ ->
                val nets = LinkedHashMap<String, Int>()
                var anyValue = false
                inputs.forEach { (subject, edit) ->
                    val v = edit.text.toString().trim().toIntOrNull() ?: 0
                    if (v != 0) anyValue = true
                    nets[subject] = v
                }
                if (!anyValue) {
                    Toast.makeText(ctx, R.string.exam_empty_warning, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val title = titleInput.text.toString().trim().ifEmpty {
                    getString(R.string.exam_default_title, exams.size + 1)
                }
                exams.add(
                    0,
                    Store.Exam(
                        id = System.currentTimeMillis(),
                        title = title,
                        createdAt = System.currentTimeMillis(),
                        nets = nets
                    )
                )
                Store.saveExams(ctx, exams)
                reload()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun confirmDelete(exam: Store.Exam) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.exam_delete_title)
            .setMessage(exam.title)
            .setPositiveButton(R.string.delete) { _, _ ->
                exams.remove(exam)
                Store.saveExams(requireContext(), exams)
                reload()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    inner class ExamsAdapter(
        private val items: List<Store.Exam>,
        private val onDelete: (Store.Exam) -> Unit
    ) : RecyclerView.Adapter<ExamsAdapter.Holder>() {

        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.examTitle)
            val date: TextView = view.findViewById(R.id.examDate)
            val total: TextView = view.findViewById(R.id.examTotal)
            val detail: TextView = view.findViewById(R.id.examDetail)
            val delete: View = view.findViewById(R.id.examDelete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_exam, parent, false)
            return Holder(view)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val exam = items[position]
            holder.title.text = exam.title
            holder.date.text = examDateFormatter
                .format(Date(exam.createdAt))
            holder.total.text = getString(R.string.exam_total_net, exam.totalNet)
            holder.detail.text = exam.nets.entries
                .filter { it.value != 0 }
                .joinToString(" · ") { "${it.key}: ${it.value}" }
                .ifEmpty { "—" }
            holder.delete.setOnClickListener { onDelete(exam) }
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
