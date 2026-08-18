package com.gunlukasistan.app

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.util.Calendar
import java.util.Locale

/**
 * Alışkanlık takibi ekranı (v5.4).
 * Su içme, spor, kitap, ilaç… günlük işaretleme, seri takibi ve haftalık şerit.
 */
class HabitsFragment : Fragment(R.layout.fragment_habits) {

    /** v8.3: zengin boş durum düzeni. */
    private var bosDurum: android.view.View? = null

    private lateinit var adapter: HabitsAdapter
    private val habits = mutableListOf<Store.Habit>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // v8.6 · Öneri 29: yatay/tablet genişlik sınırı + yoğunluk
        Duzen.uygula(view)
        val recycler = view.findViewById<RecyclerView>(R.id.recycler)
        adapter = HabitsAdapter(
            habits,
            onTap = { toggle(it) },
            onDelete = { confirmDelete(it) },
            onEdit = { showHabitEditor(it) },
            onNot = { notDefteriDiyalog(it) }
        )
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        view.findViewById<MaterialButton>(R.id.habitAddBtn).setOnClickListener {
            showHabitEditor(null)
        }
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    fun refresh() = reload()

    private fun reload() {
        val view = view ?: return
        val context = context ?: return

        // v8.9 · Öneri 17: hedefli güncelleme
        val eskiListe = habits.toList()
        habits.clear()
        habits.addAll(Store.loadHabits(context).filterNot { it.archived })
        ListeFark.aliskanliklar(adapter, eskiListe, habits)

        // v8.3 · Öneri 11: zengin boş durum
        val bosGorunum = view.findViewById<TextView>(R.id.habitsEmpty)
        if (bosDurum == null) {
            bosDurum = BosEkran.kur(
                bosGorunum, BosEkran.Tur.ALISKANLIK,
                getString(R.string.be_aliskanlik_baslik),
                getString(R.string.be_aliskanlik_aciklama),
                getString(R.string.be_aliskanlik_eylem)
            ) { showHabitEditor(null) }
        }
        bosGorunum.visibility = View.GONE
        BosEkran.goster(bosDurum, habits.isEmpty())

        val (done, total) = Store.habitProgressToday(context)
        val percent = if (total > 0) done * 100 / total else 0
        view.findViewById<LinearProgressIndicator>(R.id.habitsBar).progress = percent
        view.findViewById<TextView>(R.id.habitsSummary).text = when {
            total == 0 -> getString(R.string.habits_caption)
            done == total -> getString(R.string.habits_all_done, total)
            else -> getString(R.string.habits_progress, done, total)
        }
    }

    private fun toggle(habit: Store.Habit) {
        val context = context ?: return
        val next = Store.toggleHabit(context, habit)
        // v8.2 · Öneri 2: hedefe ulaşınca kutlama deseni, ara adımda tik
        if (next >= habit.target) Titresim.basari(context) else Titresim.dokunus(view)
        if (next >= habit.target) {
            val streak = Store.habitStreak(context, habit)
            // v8.6 · Öneri 24: 7'nin katlarında kutlama (7, 14, 21...)
            // Her gün konfeti atmak anlamını yitirir; kilometre taşları
            // seçildi.
            if (streak > 0 && streak % 7 == 0) {
                Kutlama.goster(this, Kutlama.TUR_HAVAI)
            }
            Toast.makeText(
                context,
                getString(R.string.habit_done_toast, habit.title, streak),
                Toast.LENGTH_SHORT
            ).show()
        }
        reload()
        (activity as? MainActivity)?.refreshHome()
    }

    private fun confirmDelete(habit: Store.Habit) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.habit_delete_title)
            .setMessage(getString(R.string.habit_delete_msg, habit.title))
            .setPositiveButton(R.string.delete) { _, _ ->
                Store.deleteHabitUndoable(requireContext(), habit.id)
                reload()
                geriAlSun(getString(R.string.undo_habit))

            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** Yeni alışkanlık ekler veya mevcut olanı düzenler. */
    fun showHabitEditor(existing: Store.Habit?) {
        val context = requireContext()
        val pad = (20 * resources.displayMetrics.density).toInt()

        val nameInput = EditText(context).apply {
            hint = getString(R.string.habit_name_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setText(existing?.title ?: "")
        }

        var selectedEmoji = existing?.emoji ?: "💧"
        val emojiViews = mutableListOf<TextView>()
        val emojiRows = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        Store.HABIT_EMOJIS.chunked(8).forEach { chunk ->
            val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
            chunk.forEach { emoji ->
                val tv = TextView(context).apply {
                    text = emoji
                    textSize = 21f
                    gravity = Gravity.CENTER
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

        // Günlük hedef sayısı
        val targetPicker = NumberPicker(context).apply {
            minValue = 1
            maxValue = 20
            value = existing?.target ?: 1
        }

        // Renk seçimi
        var selectedColor = existing?.colorIndex ?: 0
        val colorViews = mutableListOf<TextView>()
        val colorRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        Store.HABIT_COLORS.forEachIndexed { index, hex ->
            val dot = TextView(context).apply {
                text = "●"
                textSize = 26f
                setTextColor(Color.parseColor(hex))
                setPadding(pad / 3, pad / 6, pad / 3, pad / 6)
                alpha = if (index == selectedColor) 1f else 0.35f
                setOnClickListener {
                    selectedColor = index
                    colorViews.forEachIndexed { i, v -> v.alpha = if (i == index) 1f else 0.35f }
                }
            }
            colorViews.add(dot)
            colorRow.addView(dot)
        }

        fun label(textRes: Int) = TextView(context).apply {
            setText(textRes)
            textSize = 12f
            setPadding(0, pad / 2, 0, pad / 8)
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, 0)
            addView(nameInput)
            addView(label(R.string.habit_emoji_label))
            addView(emojiRows)
            addView(label(R.string.habit_target_label))
            addView(targetPicker)
            addView(label(R.string.habit_color_label))
            addView(colorRow)
            // v10.39/v10.40 · düzenlemede: ikinci seri satırı + mola düğmesi
            if (existing != null) {
                addView(TextView(context).apply {
                    text = getString(
                        R.string.w40_ikinci_seri_fmt,
                        SeriAnaliz.ikinciEnUzun(Store.habitTumGunler(context, existing))
                    )
                    textSize = 12f
                    setPadding(0, pad / 2, 0, pad / 8)
                })
                val molaBtn = MaterialButton(context)
                fun molaEtiketYaz() {
                    molaBtn.text = getString(
                        if (AliskanlikMola.aktifMi(context, existing.id)) R.string.w39_mola_don
                        else R.string.w39_mola_al
                    )
                }
                molaEtiketYaz()
                molaBtn.setOnClickListener {
                    if (AliskanlikMola.aktifMi(context, existing.id)) {
                        val kapanan = AliskanlikMola.don(context, existing.id)
                        Toast.makeText(
                            context,
                            getString(R.string.w39_mola_kapandi, kapanan),
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        AliskanlikMola.al(context, existing.id)
                        Toast.makeText(context, R.string.w39_mola_acildi, Toast.LENGTH_LONG).show()
                    }
                    molaEtiketYaz()
                }
                addView(molaBtn)
            }
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(if (existing == null) R.string.habit_new_title else R.string.habit_edit_title)
            .setView(androidx.core.widget.NestedScrollView(context).apply { addView(container) })
            .setPositiveButton(R.string.save) { _, _ ->
                val title = nameInput.text.toString().trim()
                if (title.isEmpty()) {
                    Toast.makeText(context, R.string.habit_need_name, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (existing == null) {
                    Store.addHabit(context, title, selectedEmoji, targetPicker.value, selectedColor)
                } else {
                    val list = Store.loadHabits(context)
                    list.firstOrNull { it.id == existing.id }?.apply {
                        this.title = title
                        this.emoji = selectedEmoji
                        this.target = targetPicker.value
                        this.colorIndex = selectedColor
                    }
                    Store.saveHabits(context, list)
                }
                reload()
                (activity as? MainActivity)?.refreshHome()
                Toast.makeText(context, R.string.habit_saved, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ---------------- Liste bağdaştırıcısı ----------------

    private class HabitsAdapter(
        private val items: List<Store.Habit>,
        private val onTap: (Store.Habit) -> Unit,
        private val onDelete: (Store.Habit) -> Unit,
        private val onEdit: (Store.Habit) -> Unit,
        private val onNot: (Store.Habit) -> Unit
    ) : RecyclerView.Adapter<HabitsAdapter.HabitHolder>() {

        class HabitHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tapZone: View = view.findViewById(R.id.habitTapZone)
            val ring: CircularProgressIndicator = view.findViewById(R.id.habitRing)
            val emoji: TextView = view.findViewById(R.id.habitEmoji)
            val title: TextView = view.findViewById(R.id.habitTitle)
            val sub: TextView = view.findViewById(R.id.habitSub)
            val count: TextView = view.findViewById(R.id.habitCount)
            val delete: ImageView = view.findViewById(R.id.habitDelete)
            val week: LinearLayout = view.findViewById(R.id.habitWeek)
            val kural21: LinearProgressIndicator = view.findViewById(R.id.habitKural21)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_habit, parent, false)
            return HabitHolder(view)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: HabitHolder, position: Int) {
            val habit = items[position]
            val context = holder.itemView.context

            val today = Store.habitCount(context, habit.id)
            val streak = Store.habitStreak(context, habit)
            val rate = Store.habitRate(context, habit, 30)
            val color = if (ThemeManager.isNeon(context)) {
                ThemeManager.NEON_PALETTE[habit.colorIndex % ThemeManager.NEON_PALETTE.size]
            } else {
                Color.parseColor(Store.HABIT_COLORS[habit.colorIndex % Store.HABIT_COLORS.size])
            }

            holder.emoji.text = habit.emoji
            holder.title.text = habit.title
            holder.count.text = if (habit.target > 1) "$today/${habit.target}" else ""
            holder.count.setTextColor(color)

            holder.ring.progress = (today * 100 / habit.target).coerceIn(0, 100)
            holder.ring.setIndicatorColor(color)

            // v10.39/v10.40: mola + gün notu rozetleri, 21 gün kuralı
            val bugun = AliskanlikMola.gunAnahtari(System.currentTimeMillis())
            val gun21 = Store.habitTumGunler(context, habit).size.coerceIn(0, Kural21.HEDEF)
            holder.sub.text = buildString {
                if (streak > 0) {
                    append(context.getString(R.string.habit_streak_fmt, streak))
                } else {
                    append(context.getString(R.string.habit_no_streak))
                }
                append(" · ")
                append(context.getString(R.string.habit_rate_fmt, rate))
                append(" · ")
                append(context.getString(R.string.w41_kural_kisa, gun21, Kural21.HEDEF))
                if (AliskanlikMola.moladaMi(context, habit.id, bugun)) {
                    append(context.getString(R.string.w40_molada_etiket))
                }
                if (AliskanlikNot.varMi(context, habit.id, bugun)) {
                    append(context.getString(R.string.w40_not_etiket))
                }
            }
            holder.kural21.progress = Kural21.yuzde(gun21)
            holder.kural21.setIndicatorColor(color)

            // Son 7 günün mini şeridi
            holder.week.removeAllViews()
            val recent = Store.habitRecent(context, habit, 7)
            val density = context.resources.displayMetrics.density
            recent.forEach { done ->
                val dot = View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        0, (7 * density).toInt(), 1f
                    ).apply { marginEnd = (4 * density).toInt() }
                    setBackgroundColor(if (done) color else Color.parseColor("#22888888"))
                }
                holder.week.addView(dot)
            }

            holder.tapZone.setOnClickListener { onTap(habit) }
            // v10.40 · Katalog #46: halkaya uzun basış → gün notu defteri
            holder.tapZone.setOnLongClickListener { onNot(habit); true }
            holder.delete.setOnClickListener { onDelete(habit) }
            holder.itemView.setOnClickListener { onEdit(habit) }
        }
    }
    /**
     * v10.40 · Katalog #46: uzun basış → gün notu defteri.
     * "Bugün neden olmadı?" — günün sebep notu; boş kayıt notu siler.
     */
    private fun notDefteriDiyalog(habit: Store.Habit) {
        val context = context ?: return
        val bugun = AliskanlikMola.gunAnahtari(System.currentTimeMillis())
        val giris = EditText(context).apply {
            hint = getString(R.string.w40_gun_notu_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setText(AliskanlikNot.notOku(context, habit.id, bugun))
            setSelection(text.length)
        }
        MaterialAlertDialogBuilder(context)
            .setTitle(getString(R.string.w40_gun_notu_baslik, habit.title))
            .setView(giris)
            .setPositiveButton(R.string.save) { _, _ ->
                AliskanlikNot.notYaz(context, habit.id, bugun, giris.text.toString())
                reload()
                if (giris.text.toString().isNotBlank()) {
                    Toast.makeText(context, R.string.w40_not_kaydedildi, Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton(R.string.w40_not_sil) { _, _ ->
                AliskanlikNot.notYaz(context, habit.id, bugun, "")
                reload()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
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
