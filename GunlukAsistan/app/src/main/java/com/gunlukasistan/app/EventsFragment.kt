package com.gunlukasistan.app

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Geri sayımlar ekranı (v5.2).
 * Sınav dışında düğün, doğum günü, tatil… her şey için sınırsız geri sayım.
 */
class EventsFragment : Fragment(R.layout.fragment_events) {

    /** v8.3: zengin boş durum düzeni. */
    private var bosDurum: android.view.View? = null

    private lateinit var adapter: EventsAdapter
    private val events = mutableListOf<Store.DayEvent>()
    private val turkish = Locale("tr", "TR")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // v8.6 · Öneri 29: yatay/tablet genişlik sınırı + yoğunluk
        Duzen.uygula(view)
        val recycler = view.findViewById<RecyclerView>(R.id.recycler)
        adapter = EventsAdapter(
            events,
            onDelete = { confirmDelete(it) },
            onPin = { togglePin(it) },
            onEdit = { showEventEditor(it) }
        )
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        view.findViewById<MaterialButton>(R.id.eventAddBtn).setOnClickListener {
            showEventEditor(null)
        }
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    private fun reload() {
        val view = view ?: return
        // v10.1 · Öneri 13: silme/sabitleme animasyonlu kapanır — kopya
        // güncellemeden ÖNCE alınır (DiffUtil iki hâli karşılaştırır).
        val eskiListe = events.toList()
        events.clear()
        events.addAll(Store.upcomingEvents(requireContext()))
        ListeFark.uygula(adapter, eskiListe, events, ayniOge = { a, b -> a.id == b.id })
        // v8.3 · Öneri 11: zengin boş durum
        val bosGorunum = view.findViewById<TextView>(R.id.eventsEmpty)
        if (bosDurum == null) {
            bosDurum = BosEkran.kur(
                bosGorunum, BosEkran.Tur.ETKINLIK,
                getString(R.string.be_etkinlik_baslik),
                getString(R.string.be_etkinlik_aciklama),
                getString(R.string.be_etkinlik_eylem)
            ) { showEventEditor(null) }
        }
        bosGorunum.visibility = View.GONE
        BosEkran.goster(bosDurum, events.isEmpty())
    }

    private fun togglePin(event: Store.DayEvent) {
        Store.setPinnedEvent(requireContext(), event.id)
        reload()
        WidgetCommon.refreshAll(requireContext())
        Toast.makeText(
            requireContext(),
            if (event.pinned) R.string.event_unpinned else R.string.event_pinned,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun confirmDelete(event: Store.DayEvent) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.event_delete_title)
            .setMessage(getString(R.string.event_delete_msg, event.title))
            .setPositiveButton(R.string.delete) { _, _ ->
                Store.deleteEventUndoable(requireContext(), event.id)
                reload()
                geriAlSun(getString(R.string.undo_event))

                WidgetCommon.refreshAll(requireContext())
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** Yeni etkinlik ekler ya da mevcut olanı düzenler. */
    fun showEventEditor(existing: Store.DayEvent?) {
        val context = requireContext()
        val pad = (20 * resources.displayMetrics.density).toInt()

        val nameInput = EditText(context).apply {
            hint = getString(R.string.event_name_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setText(existing?.title ?: "")
        }

        // Seçilen tarih; varsayılan: 30 gün sonrası
        val cal = Calendar.getInstance()
        if (existing != null && existing.millis > 0) {
            cal.timeInMillis = existing.millis
        } else {
            cal.add(Calendar.DAY_OF_YEAR, 30)
        }

        val dateBtn = MaterialButton(context).apply {
            text = formatDate(cal.time)
            setOnClickListener {
                DatePickerDialog(
                    context,
                    { _, y, m, d ->
                        cal.set(y, m, d)
                        text = formatDate(cal.time)
                    },
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
        }

        // Simge seçici: 16 hazır emoji, 8'li iki satır
        var selectedEmoji = existing?.emoji ?: "🎯"
        val emojiRows = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val emojiViews = mutableListOf<TextView>()
        Store.EVENT_EMOJIS.chunked(8).forEach { chunk ->
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, pad / 4, 0, pad / 4)
            }
            chunk.forEach { emoji ->
                val tv = TextView(context).apply {
                    text = emoji
                    textSize = 22f
                    gravity = android.view.Gravity.CENTER
                    setPadding(pad / 3, pad / 5, pad / 3, pad / 5)
                    alpha = if (emoji == selectedEmoji) 1f else 0.4f
                    setOnClickListener {
                        selectedEmoji = emoji
                        emojiViews.forEach { v -> v.alpha = if (v.text == emoji) 1f else 0.4f }
                    }
                }
                emojiViews.add(tv)
                row.addView(tv)
            }
            emojiRows.addView(row)
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, 0)
            addView(nameInput)
            addView(TextView(context).apply {
                setText(R.string.event_date_label)
                textSize = 12f
                setPadding(0, pad / 2, 0, pad / 6)
            })
            addView(dateBtn)
            addView(TextView(context).apply {
                setText(R.string.event_emoji_label)
                textSize = 12f
                setPadding(0, pad / 2, 0, 0)
            })
            addView(emojiRows)
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(if (existing == null) R.string.event_new_title else R.string.event_edit_title)
            .setView(container)
            .setPositiveButton(R.string.save) { _, _ ->
                val title = nameInput.text.toString().trim()
                if (title.isEmpty()) {
                    Toast.makeText(context, R.string.event_need_name, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val key = String.format(
                    Locale.US, "%04d%02d%02d",
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)
                )
                if (existing == null) {
                    Store.addEvent(context, title, key, selectedEmoji)
                } else {
                    val list = Store.loadEvents(context)
                    list.firstOrNull { it.id == existing.id }?.apply {
                        this.title = title
                        this.dateKey = key
                        this.emoji = selectedEmoji
                    }
                    Store.saveEvents(context, list)
                }
                reload()
                WidgetCommon.refreshAll(context)
                (activity as? MainActivity)?.refreshHome()
                Toast.makeText(context, R.string.event_saved, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun formatDate(date: Date): String =
        SimpleDateFormat("d MMMM yyyy, EEEE", turkish).format(date)

    // ---------------- Liste bağdaştırıcısı ----------------

    private class EventsAdapter(
        private val items: List<Store.DayEvent>,
        private val onDelete: (Store.DayEvent) -> Unit,
        private val onPin: (Store.DayEvent) -> Unit,
        private val onEdit: (Store.DayEvent) -> Unit
    ) : RecyclerView.Adapter<EventsAdapter.EventHolder>() {

        private val turkish = Locale("tr", "TR")
        /** v7.7: satır başına yeniden oluşturmamak için (performans). */
        private val dateFormatter = SimpleDateFormat("d MMMM yyyy, EEEE", turkish)

        class EventHolder(view: View) : RecyclerView.ViewHolder(view) {
            val emoji: TextView = view.findViewById(R.id.eventEmoji)
            val title: TextView = view.findViewById(R.id.eventTitle)
            val date: TextView = view.findViewById(R.id.eventDate)
            val days: TextView = view.findViewById(R.id.eventDays)
            val delete: ImageView = view.findViewById(R.id.eventDelete)
            /** v9.9 · Görsel öneri 11: aciliyet şeridi. */
            val serit: View = view.findViewById(R.id.eventSerit)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_event, parent, false)
            return EventHolder(view)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: EventHolder, position: Int) {
            val item = items[position]
            val context = holder.itemView.context

            holder.emoji.text = item.emoji
            holder.title.text = if (item.pinned) "📌 ${item.title}" else item.title

            val dateText = if (item.millis > 0) {
                dateFormatter.format(Date(item.millis))
            } else {
                "—"
            }
            holder.date.text = dateText

            val left = item.daysLeft
            holder.days.text = when {
                left > 0 -> context.getString(R.string.event_days_left, left)
                left == 0 -> context.getString(R.string.event_today)
                else -> context.getString(R.string.event_passed, -left)
            }
            holder.days.alpha = if (item.isPast) 0.45f else 1f
            holder.itemView.alpha = if (item.isPast) 0.6f else 1f

            // v9.9 · Görsel öneri 11 — aciliyet şeridi.
            //
            // Renk anlamı GrafikDili'nden geliyor (tek kaynak).
            // Eskiden bu bilgi yalnızca sağdaki "12 gün" yazısındaydı;
            // listeyi gözle taramak için her satırı OKUMAK gerekiyordu.
            // Şerit sayesinde kırmızıyı görüp okumadan anlıyorsun.
            // v10.0: tema duyarlı renkler — koyu temada açılmış
            // tonlar kullanılıyor (values-night/colors.xml).
            // Sabit yerine fonksiyon çağırmak şart: sabitler açık
            // temaya göre seçilmiş, koyu zeminde kontrast düşüyor.
            holder.serit.setBackgroundColor(
                when {
                    item.isPast -> GrafikDili.notr(context)
                    left <= 0 -> GrafikDili.hata(context)      // bugün
                    left <= 7 -> GrafikDili.uyari(context)     // bu hafta
                    else -> GrafikDili.basari(context)         // uzak
                }
            )

            holder.delete.setOnClickListener { onDelete(item) }
            holder.itemView.setOnClickListener { onEdit(item) }
            holder.itemView.setOnLongClickListener { onPin(item); true }
        }
    }
    /** v7.7: silme sonrası "Geri Al" şeridi. */
    private fun geriAlSun(mesaj: String) {
        val kok = view ?: return
        com.google.android.material.snackbar.Snackbar
            .make(kok, mesaj, com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
            .setAction(R.string.undo_action) {
                if (Store.geriAl()) reload()
            }
            .show()
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
