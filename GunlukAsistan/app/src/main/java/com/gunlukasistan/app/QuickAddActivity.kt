package com.gunlukasistan.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.util.Calendar

/**
 * v7.1 — "Sıfır sürtünme" hızlı kaydet ekranı.
 *
 * Ana ekran kısayollarından, paylaş menüsünden ve widget'lardan açılır.
 * Uygulamanın tamamını yüklemeden tek bir kutuya yazıp kaydetmeyi sağlar.
 * Kaydettikten sonra kendini kapatır — kullanıcı bulunduğu yere geri döner.
 */
class QuickAddActivity : AppCompatActivity() {

    companion object {
        /** Ne kaydedileceğini belirler: "task" | "note" */
        const val EXTRA_MODE = "quick_mode"
        const val MODE_TASK = "task"
        const val MODE_NOTE = "note"

        /** Kutuya önceden doldurulacak metin (paylaş menüsünden gelir). */
        const val EXTRA_TEXT = "quick_text"
    }

    private var mode = MODE_TASK
    private var dueAt = 0L

    /** v7.2: doğal dilden çıkarılan zaman (yoksa null). */
    private var naturalDue: NaturalDate.Result? = null

    /** Kullanıcı hatırlatmayı elle seçtiyse doğal dil ayrıştırıcı devreye girmez. */
    private var manualDue = false
    private lateinit var input: TextInputEditText
    private lateinit var dueButton: MaterialButton

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
        setTheme(ThemeManager.styleFor(this))
        ThemeManager.applyAccent(this)
        // v8.3 · Öneri 10: Material You (açıksa duvar kâğıdı paleti)
        ThemeManager.dinamikRengiUygula(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quick_add)

        mode = intent?.getStringExtra(EXTRA_MODE) ?: MODE_TASK
        val shared = readSharedText(intent)

        input = findViewById(R.id.qaInput)
        dueButton = findViewById(R.id.qaDue)
        val title = findViewById<android.widget.TextView>(R.id.qaTitle)
        val saveButton = findViewById<MaterialButton>(R.id.qaSave)
        val tabTask = findViewById<MaterialButton>(R.id.qaTabTask)
        val tabNote = findViewById<MaterialButton>(R.id.qaTabNote)

        // Paylaşılan metin varsa doğrudan kutuya koy
        if (!shared.isNullOrBlank()) {
            input.setText(shared)
            input.setSelection(shared.length)
            // Uzun metin paylaşıldıysa büyük ihtimalle nottur
            if (shared.length > 90 || shared.contains("\n")) mode = MODE_NOTE
        }

        fun paintTabs() {
            val isTask = mode == MODE_TASK
            tabTask.alpha = if (isTask) 1f else 0.45f
            tabNote.alpha = if (isTask) 0.45f else 1f
            dueButton.visibility = if (isTask) View.VISIBLE else View.GONE
            title.text = getString(if (isTask) R.string.qa_title_task else R.string.qa_title_note)
            input.hint = getString(if (isTask) R.string.qa_hint_task else R.string.qa_hint_note)
        }
        paintTabs()

        tabTask.setOnClickListener { mode = MODE_TASK; paintTabs() }
        tabNote.setOnClickListener { mode = MODE_NOTE; paintTabs() }

        dueButton.setOnClickListener { pickDue() }
        findViewById<View>(R.id.qaCancel).setOnClickListener { finish() }
        saveButton.setOnClickListener { save() }

        // v7.2: yazarken tarih/saat ifadelerini canlı yakala
        input.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (mode == MODE_TASK) updateNaturalHint(s?.toString().orEmpty())
            }
        })
        updateNaturalHint(input.text?.toString().orEmpty())

        // Klavyeyi hemen aç — bir dokunuş daha kazandırır
        input.requestFocus()
        window.setSoftInputMode(
            android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
        )
    }

    /** Paylaş menüsünden gelen düz metni veya konuyu okur. */
    private fun readSharedText(intent: Intent?): String? {
        // Metin seçince çıkan "işlem" menüsünden geldiyse
        if (intent?.action == Intent.ACTION_PROCESS_TEXT) {
            return intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()?.trim()
        }
        if (intent?.action != Intent.ACTION_SEND) {
            return intent?.getStringExtra(EXTRA_TEXT)
        }
        val body = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)?.trim()
        return when {
            !body.isNullOrBlank() && !subject.isNullOrBlank() && !body.contains(subject) ->
                "$subject\n$body"
            !body.isNullOrBlank() -> body
            else -> subject
        }
    }

    /** Basit hatırlatma seçici: bugün/yarın + saat. */
    private fun pickDue() {
        val options = arrayOf(
            getString(R.string.qa_due_none),
            getString(R.string.qa_due_1h),
            getString(R.string.qa_due_evening),
            getString(R.string.qa_due_tomorrow),
            getString(R.string.qa_due_pick)
        )
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(R.string.qa_due_title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> setDue(0L)
                    1 -> setDue(System.currentTimeMillis() + 3_600_000L)
                    2 -> setDue(atToday(20, 0))
                    3 -> setDue(atTomorrow(9, 0))
                    else -> pickExact()
                }
            }
            .show()
    }

    private fun atToday(hour: Int, minute: Int): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        // Saat geçtiyse yarına at
        if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
    }.timeInMillis

    private fun atTomorrow(hour: Int, minute: Int): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun pickExact() {
        val now = Calendar.getInstance()
        android.app.DatePickerDialog(
            this,
            { _, y, m, d ->
                android.app.TimePickerDialog(
                    this,
                    { _, h, min ->
                        setDue(Calendar.getInstance().apply {
                            set(y, m, d, h, min, 0); set(Calendar.MILLISECOND, 0)
                        }.timeInMillis)
                    },
                    now.get(Calendar.HOUR_OF_DAY), 0, true
                ).show()
            },
            now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    /**
     * v7.2 — Yazılan metinde tarih/saat ifadesi varsa düğmeyi otomatik doldurur.
     * Kullanıcı düğmeye elle dokunduysa (manualDue) karışmaz.
     */
    private fun updateNaturalHint(raw: String) {
        if (manualDue) return
        val parsed = NaturalDate.parse(raw)
        if (parsed.found) {
            naturalDue = parsed
            dueAt = parsed.millis
            dueButton.text =
                NaturalDate.describe(parsed.millis, parsed.hasTime, parsed.repeatDow) +
                    "  ·  " + getString(R.string.qa_nd_auto)
        } else if (naturalDue != null) {
            // Tarih ifadesi silindiyse temizle
            naturalDue = null
            dueAt = 0L
            dueButton.text = getString(R.string.qa_due_button)
        }
    }

    private fun setDue(millis: Long) {
        manualDue = true
        naturalDue = null
        dueAt = millis
        dueButton.text = if (millis <= 0L) {
            getString(R.string.qa_due_button)
        } else {
            "⏰ " + java.text.SimpleDateFormat("d MMM HH:mm", java.util.Locale("tr"))
                .format(java.util.Date(millis))
        }
    }

    private fun save() {
        val text = input.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) {
            Toast.makeText(this, R.string.qa_empty, Toast.LENGTH_SHORT).show()
            return
        }
        val now = System.currentTimeMillis()
        if (mode == MODE_TASK) {
            val tasks = Store.loadTasks(this)
            // v7.2: doğal dil kullanıldıysa tarih ifadesi metinden çıkarılır
            // ("yarın 14:00 dişçi" → görev adı yalnızca "dişçi" olur)
            val nd = naturalDue
            val cleanText = if (!manualDue && nd != null && nd.text.isNotBlank()) {
                nd.text
            } else {
                text
            }
            val task = Store.Task(
                id = now,
                text = cleanText.replace("\n", " ").take(200),
                done = false,
                createdAt = now,
                dueAt = dueAt
            )
            tasks.add(0, task)
            Store.saveTasks(this, tasks)
            if (dueAt > now) {
                try {
                    AlarmScheduler.schedule(this, task.id, task.text, dueAt)
                } catch (_: Exception) {
                }
            }
            Toast.makeText(this, R.string.qa_saved_task, Toast.LENGTH_SHORT).show()
        } else {
            val notes = Store.loadNotes(this)
            // İlk satır başlık, kalanı içerik olsun
            val lines = text.split("\n", limit = 2)
            val head = lines[0].take(80).ifBlank { getString(R.string.qa_note_untitled) }
            val body = if (lines.size > 1) lines[1] else ""
            notes.add(0, Store.Note(now, head, body, now))
            Store.saveNotes(this, notes)
            Toast.makeText(this, R.string.qa_saved_note, Toast.LENGTH_SHORT).show()
        }
        WidgetCommon.refreshAll(this)
        setResult(Activity.RESULT_OK)
        finish()
    }
}
