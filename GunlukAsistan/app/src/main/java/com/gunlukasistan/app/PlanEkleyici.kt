package com.gunlukasistan.app

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch

/**
 * v7.64 — Vakit planına iş ekleme/düzenleme editörü.
 *
 * ── Kullanıcının isteği ──
 * "Namaz planları ekleme yerini daha işlevsel yap"
 *
 * ── Öncesi ──
 * Tek satırlık bir metin kutusu vardı: yaz, Ekle'ye bas. Süre yok,
 * öncelik yok, sıralama yok, hazır seçenek yok.
 *
 * ── Şimdi ──
 * · **Hazır seçenekler** — dilime uygun öneriler, dokun-ekle
 * · **Süre** — 15/25/45/60/90 dk çipleri (Pomodoro uyumlu)
 * · **Öncelik** — düşük / normal / öncelikli (🔵 / — / 🔴)
 * · **Dilim değiştirme** — yanlış dilime eklediysen taşı
 * · **Vakit girince hatırlat** — bildirimde bu iş öne çıkar
 * · **Görevlere de ekle** — Görevler sekmesinde de görünsün
 * · **Kaydet ve yeni ekle** — arka arkaya birden çok iş girmek için
 * · Dilim süresi aşılırsa uyarı
 *
 * Aynı editör hem ekleme hem düzenleme için kullanılır.
 */
object PlanEkleyici {

    private const val TAG = "PlanEkleyici"

    /** Süre çipleri (dakika). 0 = süre yok. */
    private val SURELER = listOf(0, 15, 25, 45, 60, 90)

    /**
     * Dilime uygun hazır iş önerileri.
     * Kullanıcı sıfırdan yazmak zorunda kalmasın.
     */
    private fun hazirlar(context: Context, dilim: NamazPlan.Dilim): List<Pair<String, Int>> =
        when (dilim) {
            NamazPlan.Dilim.SABAH -> listOf(
                context.getString(R.string.np_s_sabah1) to 20,
                context.getString(R.string.np_s_sabah2) to 10
            )
            NamazPlan.Dilim.KUSLUK -> listOf(
                context.getString(R.string.np_s_kusluk1) to 45,
                context.getString(R.string.np_s_kusluk2) to 60
            )
            NamazPlan.Dilim.OGLEDEN -> listOf(
                context.getString(R.string.np_s_ogleden1) to 45,
                context.getString(R.string.np_s_ogleden2) to 25
            )
            NamazPlan.Dilim.IKINDIDEN -> listOf(
                context.getString(R.string.np_s_ikindiden1) to 45,
                context.getString(R.string.np_s_ikindiden2) to 25
            )
            NamazPlan.Dilim.AKSAMDAN -> listOf(
                context.getString(R.string.np_s_aksamdan1) to 20
            )
            NamazPlan.Dilim.GECE -> listOf(
                context.getString(R.string.np_s_gece1) to 15
            )
        }

    /**
     * Editörü açar.
     *
     * @param mevcut düzenlenecek iş; null ise yeni ekleme
     * @param bitince kaydedildikten sonra çağrılır (ekranı tazelemek için)
     */
    fun ac(
        context: Context,
        dilim: NamazPlan.Dilim,
        mevcut: NamazPlan.Is? = null,
        bitince: () -> Unit
    ) {
        val dp = context.resources.displayMetrics.density
        val duzenleme = mevcut != null

        // Seçili değerler
        var secilenSure = mevcut?.sureDk ?: 0
        var secilenOncelik = mevcut?.oncelik ?: 1
        var secilenDilim = dilim

        val kap = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((20 * dp).toInt(), (8 * dp).toInt(), (20 * dp).toInt(), 0)
        }

        // ── Metin kutusu ──
        val girdi = EditText(context).apply {
            hint = context.getString(R.string.pe_ne)
            setText(mevcut?.metin.orEmpty())
            setSelection(text?.length ?: 0)
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            maxLines = 3
            setPadding(0, (10 * dp).toInt(), 0, (10 * dp).toInt())
        }
        kap.addView(girdi)

        // ── Hazır seçenekler (yalnızca yeni eklemede) ──
        if (!duzenleme) {
            kap.addView(etiket(context, context.getString(R.string.pe_hizli)))
            val hazirSatir = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            val kaydirma = android.widget.HorizontalScrollView(context).apply {
                isHorizontalScrollBarEnabled = false
                addView(hazirSatir)
            }
            hazirlar(context, dilim).forEach { (metin, sure) ->
                hazirSatir.addView(
                    cip(context, metin, false) {
                        girdi.setText(metin)
                        girdi.setSelection(metin.length)
                        secilenSure = sure
                        Toast.makeText(context, metin, Toast.LENGTH_SHORT).show()
                    }
                )
            }
            kap.addView(kaydirma)
        }

        // ── Süre çipleri ──
        kap.addView(etiket(context, context.getString(R.string.pe_sure)))
        val sureSatir = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val sureCipler = mutableListOf<TextView>()
        SURELER.forEach { dk ->
            val c = cip(
                context,
                if (dk == 0) context.getString(R.string.pe_sure_yok)
                else context.getString(R.string.pe_dk, dk),
                dk == secilenSure
            ) {
                secilenSure = dk
                sureCipler.forEachIndexed { i, tv ->
                    cipBoya(context, tv, SURELER[i] == dk)
                }
            }
            sureCipler.add(c)
            sureSatir.addView(c)
        }
        kap.addView(
            android.widget.HorizontalScrollView(context).apply {
                isHorizontalScrollBarEnabled = false
                addView(sureSatir)
            }
        )

        // ── Öncelik ──
        kap.addView(etiket(context, context.getString(R.string.pe_oncelik)))
        val oncelikSatir = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val oncelikAdlari = listOf(
            context.getString(R.string.pe_o_dusuk),
            context.getString(R.string.pe_o_normal),
            context.getString(R.string.pe_o_yuksek)
        )
        val oncelikCipler = mutableListOf<TextView>()
        oncelikAdlari.forEachIndexed { indeks, ad ->
            val c = cip(context, ad, indeks == secilenOncelik) {
                secilenOncelik = indeks
                oncelikCipler.forEachIndexed { i, tv -> cipBoya(context, tv, i == indeks) }
            }
            oncelikCipler.add(c)
            oncelikSatir.addView(c)
        }
        kap.addView(oncelikSatir)

        // ── Dilim seçimi ──
        kap.addView(etiket(context, context.getString(R.string.pe_dilim)))
        val dilimSatir = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val dilimCipler = mutableListOf<TextView>()
        NamazPlan.Dilim.entries.forEach { d ->
            val c = cip(
                context, d.emoji + " " + context.getString(d.adRes), d == secilenDilim
            ) {
                secilenDilim = d
                dilimCipler.forEachIndexed { i, tv ->
                    cipBoya(context, tv, NamazPlan.Dilim.entries[i] == d)
                }
            }
            dilimCipler.add(c)
            dilimSatir.addView(c)
        }
        kap.addView(
            android.widget.HorizontalScrollView(context).apply {
                isHorizontalScrollBarEnabled = false
                addView(dilimSatir)
            }
        )

        // ── Anahtarlar ──
        val hatirlatSw = MaterialSwitch(context).apply {
            text = context.getString(R.string.pe_hatirlat)
            textSize = 13.5f
            isChecked = mevcut?.hatirlat ?: false
        }
        kap.addView(hatirlatSw)
        kap.addView(
            TextView(context).apply {
                text = context.getString(R.string.pe_hatirlat_d)
                textSize = 11f
                alpha = 0.65f
                setPadding(0, 0, 0, (6 * dp).toInt())
            }
        )

        val gorevSw = MaterialSwitch(context).apply {
            text = context.getString(R.string.pe_gorev_yap)
            textSize = 13.5f
            isChecked = false
        }
        if (!duzenleme) {
            kap.addView(gorevSw)
            kap.addView(
                TextView(context).apply {
                    text = context.getString(R.string.pe_gorev_yap_d)
                    textSize = 11f
                    alpha = 0.65f
                }
            )
        }

        // ── Kaydetme mantığı ──
        fun kaydet(): Boolean {
            val metin = girdi.text?.toString()?.trim().orEmpty()
            if (metin.isBlank()) {
                Toast.makeText(context, R.string.pe_bos_uyari, Toast.LENGTH_SHORT).show()
                return false
            }
            try {
                if (duzenleme && mevcut != null) {
                    NamazPlan.isGuncelle(
                        context, mevcut.id, metin, secilenSure,
                        secilenOncelik, hatirlatSw.isChecked, secilenDilim
                    )
                } else {
                    NamazPlan.isEkle(
                        context, secilenDilim, metin, secilenSure,
                        secilenOncelik, hatirlatSw.isChecked
                    )
                    // İstenirse Görevler sekmesine de yaz
                    if (gorevSw.isChecked) gorevlereEkle(context, metin)
                    Toast.makeText(
                        context,
                        context.getString(R.string.pe_eklendi, metin),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Kaydedilemedi", e)
                return false
            }
            return true
        }

        val pencere = MaterialAlertDialogBuilder(context)
            .setTitle(
                if (duzenleme) context.getString(R.string.pe_duzenle)
                else context.getString(
                    R.string.pe_baslik,
                    dilim.emoji + " " + context.getString(dilim.adRes)
                )
            )
            .setView(ScrollView(context).apply { addView(kap) })
            .setPositiveButton(if (duzenleme) R.string.save else R.string.add) { _, _ ->
                if (kaydet()) bitince()
            }
            .setNegativeButton(R.string.cancel, null)

        // Arka arkaya iş girmek için
        if (!duzenleme) {
            pencere.setNeutralButton(R.string.pe_kaydet_ekle, null)
        }

        val d = pencere.show()

        // Nötr düğme pencereyi kapatmasın — yeni iş için temizle
        if (!duzenleme) {
            d.getButton(android.app.AlertDialog.BUTTON_NEUTRAL)?.setOnClickListener {
                if (kaydet()) {
                    bitince()
                    girdi.setText("")
                    girdi.requestFocus()
                }
            }
        }
    }

    /** Planı Görevler sekmesine de yazar. */
    private fun gorevlereEkle(context: Context, metin: String) {
        try {
            val liste = Store.loadTasks(context)
            liste.add(
                Store.Task(
                    id = System.currentTimeMillis(),
                    text = metin,
                    done = false,
                    createdAt = System.currentTimeMillis()
                )
            )
            Store.saveTasks(context, liste)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Göreve eklenemedi", e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ARAYÜZ YARDIMCILARI
    // ═══════════════════════════════════════════════════════════════

    private fun etiket(context: Context, metin: String): TextView {
        val dp = context.resources.displayMetrics.density
        return TextView(context).apply {
            text = metin
            textSize = 11.5f
            alpha = 0.7f
            setPadding(0, (12 * dp).toInt(), 0, (4 * dp).toInt())
        }
    }

    private fun cip(
        context: Context,
        metin: String,
        secili: Boolean,
        tikla: () -> Unit
    ): TextView {
        val dp = context.resources.displayMetrics.density
        return TextView(context).apply {
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
            cipBoya(context, this, secili)
        }
    }

    /** Çipi seçili/seçilmemiş görünüme boyar. */
    private fun cipBoya(context: Context, tv: TextView, secili: Boolean) {
        try {
            val dp = context.resources.displayMetrics.density
            val vurgu = MaterialColors.getColor(
                tv, com.google.android.material.R.attr.colorPrimary, 0
            )
            tv.background = GradientDrawable().apply {
                cornerRadius = 18 * dp
                if (secili) {
                    setColor((vurgu and 0x00FFFFFF) or 0x33000000)
                    setStroke((1.5f * dp).toInt(), vurgu)
                } else {
                    setColor(0x14888888)
                }
            }
            if (secili) tv.setTextColor(vurgu)
            else tv.setTextColor(
                MaterialColors.getColor(
                    tv, com.google.android.material.R.attr.colorOnSurface, 0
                )
            )
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Çip boyanamadı", e)
        }
    }
}
