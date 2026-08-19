package com.gunlukasistan.app

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView

/**
 * Görünüm ekranı: 9 tema kartından birini seç, tema anında uygulanır.
 */
class ThemeFragment : Fragment(R.layout.fragment_theme) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // v8.6 · Öneri 29: yatay/tablet genişlik sınırı + yoğunluk
        Duzen.uygula(view)
        baglamSatiriKur(view) // v10.11 · ULTRA-30 A2
        paketSatiriKur(view)  // v10.8 · D40
        buildGrid(view.findViewById(R.id.themesGrid))
        buildAccentGrid(view.findViewById(R.id.accentGrid))
        // v8.3
        geceModunuKur(view)   // öneri 9 (+v10.11 A1 dördüncü düğme)
        gunesPaneliKur(view)  // v10.11 · ULTRA-30 A1
        dinamikRengiKur(view) // öneri 10
        simgeGridiKur(view)   // öneri 12
        fontSatiriKur(view)   // v10.11 · ULTRA-30 A4
    }

    // ================= v8.3 · Öneri 9: gece modu =================

    private fun geceModunuKur(view: View) {
        val grup = view.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(
            R.id.geceGrup
        ) ?: return
        val ctx = requireContext()

        // v10.11 · A1: dördüncü düğme GECE_GUNES (=3)
        val idler = intArrayOf(R.id.geceSistem, R.id.geceKapali, R.id.geceAcik, R.id.geceGunes)
        grup.check(idler[ThemeManager.geceModu(ctx)])

        grup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val yeni = idler.indexOf(checkedId)
            if (yeni < 0 || yeni == ThemeManager.geceModu(ctx)) return@addOnButtonCheckedListener
            Titresim.dokunus(grup)
            ThemeManager.geceModu(ctx, yeni)
            // AppCompatDelegate açık Activity'yi kendisi yeniden
            // oluşturuyor; ayrıca recreate() çağırmak çift yükleme olur.
            runCatching { WidgetCommon.refreshAll(ctx, true) }
            // Güneş alt paneli yalnız o mod seçiliyken görünür
            gunesPaneliKur(view)
        }
    }

    // ================= v8.3 · Öneri 10: Material You =================

    private fun dinamikRengiKur(view: View) {
        val satir = view.findViewById<View>(R.id.dinamikSatir) ?: return
        val svic = view.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(
            R.id.dinamikSvic
        ) ?: return
        val alt = view.findViewById<TextView>(R.id.dinamikAlt)
        val ctx = requireContext()

        if (!ThemeManager.dinamikDesteklenir()) {
            // Android 12 öncesi: anahtarı kapalı ve devre dışı göster,
            // sebebini yaz. Gizlemek yerine açıklamak daha iyi —
            // kullanıcı "bende neden yok?" diye aramasın.
            svic.isEnabled = false
            satir.alpha = 0.5f
            alt?.setText(R.string.my_desteklenmiyor)
            return
        }

        svic.isChecked = ThemeManager.dinamikAcik(ctx)
        val degistir = { acik: Boolean ->
            ThemeManager.dinamikAcik(ctx, acik)
            Titresim.dokunus(svic)
            requireActivity().recreate()
        }
        svic.setOnCheckedChangeListener { dugme, secili ->
            if (!dugme.isPressed) return@setOnCheckedChangeListener
            degistir(secili)
        }
        satir.setOnClickListener { svic.toggle() }
    }

    // ================= v8.3 · Öneri 12: uygulama simgesi =================

    private fun simgeGridiKur(view: View) {
        val grid = view.findViewById<LinearLayout>(R.id.simgeGrid) ?: return
        val ctx = requireContext()
        val yg = resources.displayMetrics.density
        fun dp(v: Int) = (v * yg).toInt()

        grid.removeAllViews()
        val secili = Simge.seciliIndeks(ctx)

        Simge.secenekler.chunked(3).forEachIndexed { satirNo, parca ->
            val satir = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            parca.forEachIndexed { sutunNo, secenek ->
                val index = satirNo * 3 + sutunNo
                satir.addView(simgeKarti(secenek, index, index == secili))
            }
            repeat(3 - parca.size) {
                satir.addView(View(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
                })
            }
            grid.addView(satir)
        }
    }

    private fun simgeKarti(
        secenek: Simge.Secenek,
        index: Int,
        seciliMi: Boolean
    ): View {
        val ctx = requireContext()
        val yg = resources.displayMetrics.density
        fun dp(v: Int) = (v * yg).toInt()

        val kart = MaterialCardView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, dp(92), 1f).apply {
                setMargins(dp(4), dp(4), dp(4), dp(4))
            }
            radius = dp(16).toFloat()
            cardElevation = 0f
            strokeWidth = if (seciliMi) dp(2) else dp(1)
            strokeColor = if (seciliMi) {
                com.google.android.material.color.MaterialColors.getColor(
                    this, com.google.android.material.R.attr.colorPrimary, 0
                )
            } else {
                com.google.android.material.color.MaterialColors.getColor(
                    this, com.google.android.material.R.attr.colorOutlineVariant, 0
                )
            }
            isClickable = true
        }

        val ic = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Simge önizlemesi: yuvarlak renk + tik
        val onizleme = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(secenek.renk)
            }
            text = "✓"
            gravity = Gravity.CENTER
            textSize = 17f
            // Açık zeminde koyu tik, koyu zeminde beyaz
            setTextColor(if (parlakMi(secenek.renk)) 0xFF3B332A.toInt() else 0xFFFFFFFF.toInt())
        }

        val ad = TextView(ctx).apply {
            setText(secenek.baslikRes)
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(
                com.google.android.material.color.MaterialColors.getColor(
                    this, com.google.android.material.R.attr.colorOnSurface, 0
                )
            )
            setPadding(0, dp(5), 0, 0)
        }

        ic.addView(onizleme)
        ic.addView(ad)
        if (seciliMi) {
            ic.addView(TextView(ctx).apply {
                text = "●"
                textSize = 8f
                gravity = Gravity.CENTER
                setTextColor(
                    com.google.android.material.color.MaterialColors.getColor(
                        this, com.google.android.material.R.attr.colorPrimary, 0
                    )
                )
            })
        }
        kart.addView(ic)

        kart.dalgaEkle()
        kart.setOnClickListener {
            if (seciliMi) return@setOnClickListener
            Titresim.dokunus(it)
            simgeOnayla(secenek)
        }
        return kart
    }

    /**
     * Simge değişimi uygulamanın launcher girişini değiştiriyor;
     * bazı başlatıcılarda ana ekran kısayolu düşebiliyor. Bu Android'in
     * davranışı ama kullanıcı bunu bilmeden yaşarsa hata sanır.
     */
    private fun simgeOnayla(secenek: Simge.Secenek) {
        val ctx = requireContext()
        com.google.android.material.dialog.MaterialAlertDialogBuilder(ctx)
            .setTitle(getString(secenek.baslikRes))
            .setMessage(R.string.sm_uyari)
            .setPositiveButton(R.string.sm_degistir) { _, _ ->
                val oldu = Simge.sec(ctx, secenek.kod)
                Toast.makeText(
                    ctx,
                    if (oldu) R.string.sm_degisti else R.string.sm_olmadi,
                    Toast.LENGTH_LONG
                ).show()
                view?.let { simgeGridiKur(it) }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** Rengin üzerine koyu mu açık mı yazı gideceğini belirler. */
    private fun parlakMi(renk: Int): Boolean {
        val r = (renk shr 16) and 0xFF
        val g = (renk shr 8) and 0xFF
        val b = renk and 0xFF
        // ITU-R BT.601 parlaklık
        return (r * 299 + g * 587 + b * 114) / 1000 > 150
    }

    // ================= v10.8 · D40: tema paketleri ==================

    /**
     * Paket satırını doldurur: 4 şablon + kullanıcı paketleri +
     * [ts_kaydet] çipi. Dokun = uygula, uzun bas = (kullanıcı
     * paketiyse) silme sorusu.
     */
    private fun paketSatiriKur(view: View) {
        val row = view.findViewById<LinearLayout>(R.id.paketRow) ?: return
        val ctx = requireContext()
        row.removeAllViews()
        (TemaPaketi.sablonlar() + TemaPaketi.listele(ctx)).forEach { paket ->
            row.addView(paketCipi(paket))
        }
        row.addView(kaydetCipi(row))
    }

    private fun paketCipi(paket: TemaPaketi.Paket): View {
        val ctx = requireContext()
        val yogunluk = resources.displayMetrics.density
        fun dp(v: Int) = (v * yogunluk).toInt()

        val kart = MaterialCardView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = dp(8) }
            radius = dp(16).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(
                com.google.android.material.color.MaterialColors.getColor(
                    ctx, com.google.android.material.R.attr.colorSecondaryContainer, 0
                )
            )
            isClickable = true
            isFocusable = true
        }
        kart.addView(
            TextView(ctx).apply {
                text = "${paket.emoji} ${paket.ad}"
                textSize = 13f
                setPadding(dp(14), dp(10), dp(14), dp(10))
                setTextColor(
                    com.google.android.material.color.MaterialColors.getColor(
                        ctx, com.google.android.material.R.attr.colorOnSurface, 0
                    )
                )
                maxLines = 1
            }
        )
        kart.dalgaEkle()
        kart.setOnClickListener { paketiUygula(paket) }
        if (paket.id > 0) {
            kart.setOnLongClickListener {
                paketSilSoru(paket)
                true
            }
        }
        return kart
    }

    private fun paketiUygula(paket: TemaPaketi.Paket) {
        val ctx = requireContext()
        TemaPaketi.uygula(ctx, paket)
        Titresim.dokunus(view ?: return)
        Toast.makeText(
            ctx,
            getString(R.string.ts_uygulandi, paket.emoji, paket.ad),
            Toast.LENGTH_SHORT
        ).show()
        runCatching { WidgetCommon.refreshAll(ctx, true) }
        // Gece modu değişimi delegate üzerinden, tema+vurgu+yazı
        // değişimi recreate üzerinden yaşar — ikisi de gerekli.
        requireActivity().recreate()
    }

    private fun paketSilSoru(paket: TemaPaketi.Paket) {
        val ctx = requireContext()
        com.google.android.material.dialog.MaterialAlertDialogBuilder(ctx)
            .setTitle(getString(R.string.ts_sil_soru, "${paket.emoji} ${paket.ad}"))
            .setPositiveButton(R.string.delete) { _, _ ->
                TemaPaketi.sil(ctx, paket.id)
                Toast.makeText(ctx, R.string.ts_silindi, Toast.LENGTH_SHORT).show()
                view?.let { paketSatiriKur(it) }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** Satır sonundaki "kombinasyonu kaydet" çipi. */
    private fun kaydetCipi(row: LinearLayout): View {
        val ctx = requireContext()
        val yogunluk = resources.displayMetrics.density
        fun dp(v: Int) = (v * yogunluk).toInt()

        val kart = MaterialCardView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = dp(8) }
            radius = dp(16).toFloat()
            cardElevation = 0f
            strokeWidth = dp(1)
            strokeColor = com.google.android.material.color.MaterialColors.getColor(
                ctx, com.google.android.material.R.attr.colorPrimary, 0
            )
            setCardBackgroundColor(
                com.google.android.material.color.MaterialColors.getColor(
                    ctx, com.google.android.material.R.attr.colorPrimaryContainer, 0
                )
            )
            isClickable = true
            isFocusable = true
        }
        kart.addView(
            TextView(ctx).apply {
                setText(R.string.ts_kaydet)
                textSize = 13f
                setPadding(dp(14), dp(10), dp(14), dp(10))
                setTextColor(
                    com.google.android.material.color.MaterialColors.getColor(
                        ctx, com.google.android.material.R.attr.colorPrimary, 0
                    )
                )
            }
        )
        kart.dalgaEkle()
        kart.setOnClickListener { paketKaydetDiyalog(row) }
        return kart
    }

    /**
     * Mevcut altı tercihi paket olarak kaydeder.
     *
     * Diyalog iki aşama tek akışta: ad yazılır, altından emoji satırı
     * seçilir (işaretli olan geçerli). Kota dolmuşsa hiç açılmaz.
     */
    private fun paketKaydetDiyalog(row: LinearLayout) {
        val ctx = requireContext()
        if (!TemaPaketi.eklenebilirMi(TemaPaketi.listele(ctx).size)) {
            Toast.makeText(
                ctx,
                getString(R.string.ts_dolu, TemaPaketi.MAKS),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val yogunluk = resources.displayMetrics.density
        fun dp(v: Int) = (v * yogunluk).toInt()

        val emojiler = resources.getStringArray(R.array.ts_emojiler)
        var seciliEmoji = emojiler.first()

        val govde = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(10), dp(20), dp(4))
        }
        val adGirdi = android.widget.EditText(ctx).apply {
            hint = getString(R.string.ts_ad_hint)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS
            filters = arrayOf(android.text.InputFilter.LengthFilter(20))
        }
        govde.addView(adGirdi)

        govde.addView(
            TextView(ctx).apply {
                setText(R.string.ts_emoji_sec)
                textSize = 12f
                setPadding(0, dp(10), 0, 0)
            }
        )
        val emojiRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val emojiGorunumleri = mutableListOf<TextView>()
        emojiler.forEach { emoji ->
            val t = TextView(ctx).apply {
                text = emoji
                textSize = 24f
                setPadding(dp(8), dp(6), dp(8), dp(6))
                isClickable = true
                isFocusable = true
            }
            t.setOnClickListener {
                seciliEmoji = emoji
                emojiGorunumleri.forEach { g -> g.alpha = 0.45f }
                t.alpha = 1f
                t.scaleX = 1.25f
                t.scaleY = 1.25f
                emojiGorunumleri.filter { it != t }.forEach { g ->
                    g.scaleX = 1f
                    g.scaleY = 1f
                }
            }
            // İlk görünümler silik; seçili belirgin başlar
            if (emoji == seciliEmoji) {
                t.alpha = 1f
                t.scaleX = 1.25f
                t.scaleY = 1.25f
            } else {
                t.alpha = 0.45f
            }
            emojiGorunumleri.add(t)
            emojiRow.addView(t)
        }
        govde.addView(emojiRow)

        com.google.android.material.dialog.MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.ts_diyalog_ad)
            .setView(govde)
            .setPositiveButton(R.string.save) { _, _ ->
                val ad = adGirdi.text.toString().trim().ifEmpty {
                    getString(R.string.ts_baslik)
                }
                val kaydedilen = TemaPaketi.kaydet(
                    ctx, TemaPaketi.simdikiDurum(ctx, ad, seciliEmoji)
                )
                if (kaydedilen != null) {
                    Toast.makeText(
                        ctx,
                        getString(R.string.ts_kaydedildi, kaydedilen.ad),
                        Toast.LENGTH_SHORT
                    ).show()
                    view?.let { paketSatiriKur(it) }
                }
                // null = yarış durumunda kota dolmuş; sessizce geç
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ---------------- Kendi rengini seç ----------------

    private fun buildAccentGrid(grid: LinearLayout) {
        grid.removeAllViews()
        val context = requireContext()
        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        val selected = ThemeManager.accentIndex(context)

        ThemeManager.accents.chunked(6).forEach { rowAccents ->
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_HORIZONTAL
            }
            rowAccents.forEach { accent ->
                val index = ThemeManager.accents.indexOf(accent)
                val holder = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(dp(8), dp(6), dp(8), dp(6)) }
                }
                val circle = View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(accent.swatch)
                        if (index == selected) {
                            setStroke(dp(3), 0xFFFFFFFF.toInt())
                            setStroke(dp(6), accent.swatch)
                        }
                    }
                }
                val name = TextView(context).apply {
                    text = if (index == selected) "✓ ${accent.title}" else accent.title
                    textSize = 9.5f
                    gravity = Gravity.CENTER
                    setTextColor(
                        com.google.android.material.color.MaterialColors.getColor(
                            context,
                            com.google.android.material.R.attr.colorOnSurface,
                            0xFF888888.toInt()
                        )
                    )
                    setPadding(0, dp(3), 0, 0)
                }
                holder.addView(circle)
                holder.addView(name)
                holder.setOnClickListener {
                    if (index == selected) {
                        // Seçiliyken dokun → temanın kendi rengine dön
                        ThemeManager.selectAccent(context, -1)
                        Toast.makeText(context, R.string.accent_cleared, Toast.LENGTH_SHORT).show()
                    } else {
                        ThemeManager.selectAccent(context, index)
                        Toast.makeText(
                            context,
                            getString(R.string.theme_applied, accent.title),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    // v7.66: widget'lar da yeni renge gecsin
                    try {
                        WidgetCommon.refreshAll(context, true)
                    } catch (e: Exception) {
                        android.util.Log.w("ThemeFragment", "Widget tazelenemedi", e)
                    }
                    requireActivity().recreate()
                }
                row.addView(holder)
            }
            grid.addView(row)
        }
    }

    private fun buildGrid(grid: LinearLayout) {
        val context = requireContext()
        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        grid.removeAllViews()
        val selected = ThemeManager.selected(context)

        ThemeManager.specs.chunked(3).forEachIndexed { rowIndex, rowSpecs ->
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            rowSpecs.forEachIndexed { colIndex, spec ->
                val index = rowIndex * 3 + colIndex
                row.addView(makeThemeCard(spec, index, index == selected, grid))
            }
            // Son satırda eksik hücreleri boşlukla doldur
            repeat(3 - rowSpecs.size) {
                val spacer = View(context)
                spacer.layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
                row.addView(spacer)
            }
            grid.addView(row)
        }
    }

    private fun makeThemeCard(
        spec: ThemeManager.Spec,
        index: Int,
        isSelected: Boolean,
        grid: LinearLayout
    ): View {
        val context = requireContext()
        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        val card = MaterialCardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, dp(150), 1f).apply {
                setMargins(dp(5), dp(5), dp(5), dp(5))
            }
            radius = dp(20).toFloat()
            cardElevation = 1f
            strokeWidth = if (isSelected) dp(3) else 0
            strokeColor = spec.ringColor
            setCardBackgroundColor(spec.cardColor)
        }

        val inner = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Renk halkası
        val ring = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(52), dp(52))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(spec.cardColor)
                setStroke(dp(5), spec.ringColor)
            }
        }

        val emoji = TextView(context).apply {
            text = spec.emoji
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        }

        val name = TextView(context).apply {
            text = spec.title
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(if (spec.dark) 0xFFF5ECE5.toInt() else 0xFF3B332A.toInt())
            setPadding(0, dp(2), 0, dp(6))
        }

        if (isSelected) {
            val badge = TextView(context).apply {
                text = "✓"
                textSize = 13f
                setTextColor(spec.ringColor)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
            }
            inner.addView(badge)
        }

        inner.addView(ring)
        inner.addView(emoji)
        inner.addView(name)
        card.addView(inner)

        card.dalgaEkle()
        card.setOnClickListener {
            ThemeManager.selected(context)
            ThemeManager.select(context, index)
            Toast.makeText(
                context,
                getString(R.string.theme_applied, spec.title),
                Toast.LENGTH_SHORT
            ).show()
            // v7.66: widget'lar da yeni temaya gecsin
            try {
                WidgetCommon.refreshAll(context, true)
            } catch (e: Exception) {
                android.util.Log.w("ThemeFragment", "Widget tazelenemedi", e)
            }
            requireActivity().recreate()
        }
        return card
    }

    // ══════════════════════════════════════════════════════════════
    // v10.11 · ULTRA-30 A1 — GÜNEŞ MODU PANELİ
    // ══════════════════════════════════════════════════════════════

    /**
     * Güneş alt paneli: kaynak çipi (☀️/⏰) + saat satırları.
     * Yalnız GECE_GUNES seçiliyken görünür; her seçimde baştan kurulur.
     */
    private fun gunesPaneliKur(view: View) {
        val alt = view.findViewById<LinearLayout>(R.id.geceGunesAlt) ?: return
        val ctx = requireContext()
        alt.removeAllViews()
        val acikMi = ThemeManager.geceModu(ctx) == ThemeManager.GECE_GUNES
        alt.visibility = if (acikMi) View.VISIBLE else View.GONE
        if (!acikMi) return

        val ozelMi = ThemeManager.gunesKaynak(ctx) == ThemeManager.GUNES_OZEL
        // Kaynak seçimi
        alt.addView(
            gunesSecimSatiri(
                getString(R.string.ax_gunes_kaynak),
                if (ozelMi) getString(R.string.ax_gunes_ozel) else getString(R.string.ax_gunes_oto)
            ) {
                ThemeManager.gunesKaynak(
                    ctx,
                    if (ozelMi) ThemeManager.GUNES_AUTO else ThemeManager.GUNES_OZEL
                )
                gunesPaneliKur(view)
            }
        )
        if (ozelMi) {
            alt.addView(
                gunesSecimSatiri(
                    getString(R.string.ax_gunes_acil),
                    UykuCerceve.saatMetni(ThemeManager.gunesAcilDk(ctx))
                ) {
                    saatSecGunes(view, acilMi = true)
                }
            )
            alt.addView(
                gunesSecimSatiri(
                    getString(R.string.ax_gunes_karan),
                    UykuCerceve.saatMetni(ThemeManager.gunesKaranDk(ctx))
                ) {
                    saatSecGunes(view, acilMi = false)
                }
            )
        } else {
            alt.addView(TextView(ctx).apply {
                text = getString(R.string.ax_gunes_note)
                textSize = 12f
                alpha = 0.65f
                setPadding(0, resources.displayMetrics.density.toInt() * 4, 0, 0)
            })
        }
    }

    /** Küçük "etiket + değer" satırı — değer vurgu renginde, dokununca değişir. */
    private fun gunesSecimSatiri(etiket: String, deger: String, tikla: () -> Unit): View {
        val ctx = requireContext()
        val d = resources.displayMetrics.density
        val satir = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, (8 * d).toInt(), 0, (8 * d).toInt())
            background = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x22888888), null, null
            )
            isClickable = true
            isFocusable = true
            setOnClickListener { tikla() }
        }
        satir.addView(TextView(ctx).apply {
            text = etiket
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        satir.addView(TextView(ctx).apply {
            text = deger
            textSize = 14f
            setTextColor(
                com.google.android.material.color.MaterialColors.getColor(
                    ctx, com.google.android.material.R.attr.colorPrimary, 0
                )
            )
        })
        return satir
    }

    private fun saatSecGunes(view: View, acilMi: Boolean) {
        val ctx = requireContext()
        val mevcut = if (acilMi) ThemeManager.gunesAcilDk(ctx) else ThemeManager.gunesKaranDk(ctx)
        android.app.TimePickerDialog(ctx, { _, h, m ->
            if (acilMi) ThemeManager.gunesAcilDk(ctx, h * 60 + m)
            else ThemeManager.gunesKaranDk(ctx, h * 60 + m)
            gunesPaneliKur(view)
        }, mevcut / 60, mevcut % 60, true).show()
    }

    // ══════════════════════════════════════════════════════════════
    // v10.11 · ULTRA-30 A2 — BAĞLAM PROFİLLERİ
    // ══════════════════════════════════════════════════════════════

    private fun baglamSatiriKur(view: View) {
        val row = view.findViewById<LinearLayout>(R.id.baglamRow) ?: return
        val ctx = requireContext()
        row.removeAllViews()
        (BaglamProfili.sablonlar() + BaglamProfili.listele(ctx)).forEach { profil ->
            row.addView(baglamCipi(profil))
        }
        row.addView(baglamKaydetCipi())
    }

    private fun baglamCipi(profil: BaglamProfili.Profil): View {
        val ctx = requireContext()
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        val kart = MaterialCardView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = dp(8) }
            radius = dp(16).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(
                com.google.android.material.color.MaterialColors.getColor(
                    ctx, com.google.android.material.R.attr.colorSecondaryContainer, 0
                )
            )
            isClickable = true
            isFocusable = true
        }
        kart.addView(TextView(ctx).apply {
            text = "${profil.emoji} ${profil.ad}"
            textSize = 13f
            setPadding(dp(14), dp(10), dp(14), dp(10))
            setTextColor(
                com.google.android.material.color.MaterialColors.getColor(
                    ctx, com.google.android.material.R.attr.colorOnSurface, 0
                )
            )
            maxLines = 1
        })
        kart.dalgaEkle()
        kart.setOnClickListener { profiliUygula(profil) }
        if (profil.id > 0) {
            kart.setOnLongClickListener { profilSilSoru(profil); true }
        }
        return kart
    }

    private fun profiliUygula(profil: BaglamProfili.Profil) {
        val ctx = requireContext()
        BaglamProfili.uygula(ctx, profil)
        Titresim.dokunus(view ?: return)
        Toast.makeText(
            ctx,
            getString(R.string.ax_baglam_uygulandi, profil.emoji, profil.ad),
            Toast.LENGTH_SHORT
        ).show()
        // Tema/vurgu/yazı recreate ile yaşar — TemaPaketi akışıyla aynı.
        requireActivity().recreate()
    }

    private fun profilSilSoru(profil: BaglamProfili.Profil) {
        val ctx = requireContext()
        com.google.android.material.dialog.MaterialAlertDialogBuilder(ctx)
            .setTitle(getString(R.string.ax_baglam_sil_soru, "${profil.emoji} ${profil.ad}"))
            .setPositiveButton(R.string.delete) { _, _ ->
                BaglamProfili.sil(ctx, profil.id)
                Toast.makeText(ctx, R.string.ax_baglam_silindi, Toast.LENGTH_SHORT).show()
                view?.let { baglamSatiriKur(it) }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun baglamKaydetCipi(): View {
        val ctx = requireContext()
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        val kart = MaterialCardView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = dp(8) }
            radius = dp(16).toFloat()
            cardElevation = 0f
            strokeWidth = (1 * d).toInt()
            setCardBackgroundColor(android.graphics.Color.TRANSPARENT)
            isClickable = true
            isFocusable = true
        }
        kart.addView(TextView(ctx).apply {
            setText(R.string.ax_baglam_kaydet)
            textSize = 13f
            setPadding(dp(14), dp(10), dp(14), dp(10))
            setTextColor(
                com.google.android.material.color.MaterialColors.getColor(
                    ctx, com.google.android.material.R.attr.colorPrimary, 0
                )
            )
        })
        kart.dalgaEkle()
        kart.setOnClickListener { baglamKaydetDiyalog() }
        return kart
    }

    /** Şu anki durum profil yapılır: ad + emoji (paket kalıbıyla aynı). */
    private fun baglamKaydetDiyalog() {
        val ctx = requireContext()
        val mevcut = BaglamProfili.listele(ctx).size
        if (mevcut >= 5) {
            Toast.makeText(ctx, getString(R.string.ax_baglam_dolu, 5), Toast.LENGTH_SHORT).show()
            return
        }
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        val emojiler = resources.getStringArray(R.array.ts_emojiler)
        var seciliEmoji = emojiler.first()

        val govde = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(10), dp(20), dp(4))
        }
        val adGirdi = android.widget.EditText(ctx).apply {
            hint = getString(R.string.ax_baglam_ad_hint)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS
            filters = arrayOf(android.text.InputFilter.LengthFilter(20))
        }
        govde.addView(adGirdi)
        val emojiRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val gorunumler = mutableListOf<TextView>()
        emojiler.forEach { emoji ->
            val t = TextView(ctx).apply {
                text = emoji
                textSize = 24f
                setPadding(dp(8), dp(6), dp(8), dp(6))
                isClickable = true
            }
            t.setOnClickListener {
                seciliEmoji = emoji
                gorunumler.forEach { it.alpha = 0.4f }
                t.alpha = 1f
            }
            gorunumler.add(t)
            emojiRow.addView(t)
        }
        gorunumler.firstOrNull()?.alpha = 1f
        gorunumler.drop(1).forEach { it.alpha = 0.4f }
        govde.addView(emojiRow)

        com.google.android.material.dialog.MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.ax_baglam_kaydet)
            .setView(govde)
            .setPositiveButton(R.string.ok) { _, _ ->
                val ad = adGirdi.text.toString().trim()
                if (ad.isBlank()) return@setPositiveButton
                BaglamProfili.kaydet(
                    ctx, BaglamProfili.simdikiDurum(ctx, ad, seciliEmoji)
                )
                view?.let { baglamSatiriKur(it) }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ══════════════════════════════════════════════════════════════
    // v10.11 · ULTRA-30 A4 — YAZI KARAKTERİ VİTRİNİ
    // ══════════════════════════════════════════════════════════════

    private fun fontSatiriKur(view: View) {
        val row = view.findViewById<LinearLayout>(R.id.fontRow) ?: return
        val ctx = requireContext()
        row.removeAllViews()
        val secili = ThemeManager.yaziTur(ctx)
        listOf(
            Triple(ThemeManager.YAZI_POPPINS, getString(R.string.ax_font_poppins), R.font.poppins_regular),
            Triple(ThemeManager.YAZI_ATKINSON, getString(R.string.ax_font_atkinson), R.font.atkinson_regular),
            Triple(ThemeManager.YAZI_LORA, getString(R.string.ax_font_lora), R.font.lora_regular)
        ).forEach { (tur, ad, fontRes) ->
            row.addView(fontKarti(view, tur, ad, fontRes, secili == tur))
        }
    }

    private fun fontKarti(view: View, tur: Int, ad: String, fontRes: Int, secili: Boolean): View {
        val ctx = requireContext()
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        val kart = MaterialCardView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ).apply { marginEnd = dp(8) }
            radius = dp(16).toFloat()
            cardElevation = 0f
            isClickable = true
            isFocusable = true
            if (secili) {
                strokeWidth = dp(2)
                strokeColor = com.google.android.material.color.MaterialColors.getColor(
                    ctx, com.google.android.material.R.attr.colorPrimary, 0
                )
                setCardBackgroundColor(
                    com.google.android.material.color.MaterialColors.getColor(
                        ctx, com.google.android.material.R.attr.colorPrimaryContainer, 0
                    )
                )
            } else {
                setCardBackgroundColor(
                    com.google.android.material.color.MaterialColors.getColor(
                        ctx, com.google.android.material.R.attr.colorSecondaryContainer, 0
                    )
                )
            }
        }
        val ic = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            addView(TextView(ctx).apply {
                text = ad
                textSize = 12f
                alpha = 0.7f
                maxLines = 1
            })
            addView(TextView(ctx).apply {
                text = getString(R.string.ax_font_ornek)
                textSize = 15f
                maxLines = 2
                runCatching {
                    typeface = androidx.core.content.res.ResourcesCompat.getFont(ctx, fontRes)
                }
                setPadding(0, dp(4), 0, 0)
            })
        }
        kart.addView(ic)
        kart.dalgaEkle()
        kart.setOnClickListener {
            Titresim.dokunus(kart)
            ThemeManager.yaziTur(ctx, tur)
            Toast.makeText(ctx, getString(R.string.ax_font_uygulandi, ad), Toast.LENGTH_SHORT).show()
            // Font katmanı setTheme/applyAccent'te basılıyor —
            // yaşaması için recreate şart.
            requireActivity().recreate()
        }
        return kart
    }
}
