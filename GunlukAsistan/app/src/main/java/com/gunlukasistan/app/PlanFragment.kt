package com.gunlukasistan.app

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * v7.55 — Vakit Planı sekmesi.
 *
 * ── Kullanıcının isteği ──
 * "Namaz programındaki plan kısmını sayaç kısmının oraya al,
 *  sayacı da üst sekmedeki soldaki 3 noktanın içine entegre et."
 *
 * ── Yapılan ──
 * Namaz ekranındaki (NamazActivity) "vakit aralarındaki işlerim" bölümü
 * buraya, alt menüdeki eski **Sayaç** sekmesinin yerine taşındı.
 * Sayaç ekranı kaldırılmadı — ⋮ yan panelinden ve buradaki "⏱ Sayaç"
 * kısayolundan açılıyor.
 *
 * NamazActivity artık sadece vakitleri gösteriyor, plan burada yönetiliyor.
 * Veri kaynağı aynı: [NamazPlan] — iki ekran da aynı işleri görür.
 */
class PlanFragment : Fragment(R.layout.fragment_plan) {

    private val yogunluk get() = resources.displayMetrics.density

    /** Vakit listesi katlanmış mı? Ekran ömrü boyunca hatırlanır. */
    private var vakitlerAcik = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // v8.6 · Öneri 29: yatay/tablet genişlik sınırı + yoğunluk
        Duzen.uygula(view)
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.plTemplate).setOnClickListener { sablonSor() }

        // Sayaç sekmesi kalktı — erişim buradan ve ⋮ panelinden
        view.findViewById<TextView>(R.id.plTimerBtn).setOnClickListener {
            (activity as? MainActivity)?.openTimer()
        }

        view.findViewById<TextView>(R.id.plTimesToggle).setOnClickListener {
            vakitlerAcik = !vakitlerAcik
            ciz()
        }

        // Namaz modülü kapalıysa: tek dokunuşla aç
        view.findViewById<TextView>(R.id.plClosedOpen).setOnClickListener {
            val ctx = context ?: return@setOnClickListener
            NamazVakti.setAcik(ctx, true)
            try {
                NamazBildirim.hepsiniKur(ctx)
            } catch (e: Exception) {
                android.util.Log.w("PlanFragment", "Namaz bildirimi kurulamadı", e)
            }
            (activity as? MainActivity)?.namazRozetiniTazele()
            ciz()
        }

        ciz()
    }

    override fun onResume() {
        super.onResume()
        ciz()
        view?.let { GlassmorphismTemaMotoru.sekmeleriVeKartlariStille(it, requireContext()) }
    }

    /** MainActivity sekme değiştirdiğinde çağırır — veriler tazelensin. */
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) ciz()
    }

    // ═══════════════════════════════════════════════════════════════
    // ÇİZİM (v10.87: 150 ms çizim engeliyle donma ve takılmalar önlenir)
    // ═══════════════════════════════════════════════════════════════

    private var sonCizimMs = 0L

    fun ciz() {
        val simdi = System.currentTimeMillis()
        if (simdi - sonCizimMs < 150L) return
        sonCizimMs = simdi
        val kok = view ?: return
        val ctx = context ?: return
        try {
            // v11.13: kaydırma pozisyonunu koru — içerik yeniden çizilse de
            // kullanıcı aşağıda kaldığı yerden devam edebilsin (yukarı çıkış garanti).
            val scroll = kok.findViewById<androidx.core.widget.NestedScrollView>(R.id.plScroll)
            val oncekiScroll = scroll?.scrollY ?: 0
            ustKisim(kok, ctx)
            vakitleriCiz(kok, ctx)
            planiCiz(kok, ctx)
            scroll?.post { scroll.scrollTo(0, oncekiScroll) }
        } catch (e: Exception) {
            android.util.Log.w("PlanFragment", "Plan çizilemedi", e)
        }
    }

    private fun ustKisim(kok: View, ctx: android.content.Context) {
        // Modül kapalı uyarısı
        kok.findViewById<View>(R.id.plClosedBanner).visibility =
            if (NamazVakti.acikMi(ctx)) View.GONE else View.VISIBLE

        val gun = NamazVakti.bugunDuzeltilmis(ctx)
        val simdi = NamazVakti.simdiDakika()
        val (sonrakiVakit, kalan) = gun.sonraki(simdi)

        kok.findViewById<TextView>(R.id.plCity).text = getString(
            R.string.nm_city_date,
            NamazVakti.sehirAdi(ctx),
            SimpleDateFormat("d MMMM EEEE", Locale("tr", "TR")).format(Date())
        )
        kok.findViewById<TextView>(R.id.plNextLabel).text =
            getString(R.string.nm_next, getString(sonrakiVakit.adRes))
        kok.findViewById<TextView>(R.id.plNextTime).text = gun.saat(sonrakiVakit)
        kok.findViewById<TextView>(R.id.plNextLeft).text =
            getString(R.string.nm_left, NamazPlan.sureMetni(kalan))

        val oneri = NamazPlan.simdiNeYapmali(ctx)
        kok.findViewById<TextView>(R.id.plSuggest).apply {
            text = oneri
            visibility = if (oneri.isBlank()) View.GONE else View.VISIBLE
            setTextColor(
                MaterialColors.getColor(
                    this, com.google.android.material.R.attr.colorPrimary, 0
                )
            )
        }
    }

    private fun vakitleriCiz(kok: View, ctx: android.content.Context) {
        val kap = kok.findViewById<LinearLayout>(R.id.plTimes)
        kok.findViewById<TextView>(R.id.plTimesToggle).text =
            getString(if (vakitlerAcik) R.string.np_vakit_gizle else R.string.np_vakit_goster)

        if (!vakitlerAcik) {
            kap.visibility = View.GONE
            kap.removeAllViews()
            return
        }
        kap.visibility = View.VISIBLE
        kap.removeAllViews()

        val gun = NamazVakti.bugunDuzeltilmis(ctx)
        val aktif = gun.aktifVakit(NamazVakti.simdiDakika())
        val vurgu = MaterialColors.getColor(
            kap, com.google.android.material.R.attr.colorPrimary, 0
        )

        NamazVakti.Vakit.entries.forEach { v ->
            val bu = v == aktif
            val satir = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(
                    (8 * yogunluk).toInt(), (7 * yogunluk).toInt(),
                    (8 * yogunluk).toInt(), (7 * yogunluk).toInt()
                )
                if (bu) {
                    background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = 12 * yogunluk
                        setColor((vurgu and 0x00FFFFFF) or 0x22000000)
                    }
                }
            }
            satir.addView(TextView(ctx).apply {
                text = v.emoji
                textSize = 14f
                setPadding(0, 0, (10 * yogunluk).toInt(), 0)
            })
            satir.addView(TextView(ctx).apply {
                text = getString(v.adRes)
                textSize = 13.5f
                if (bu) setTypeface(typeface, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                )
            })
            satir.addView(TextView(ctx).apply {
                text = gun.saat(v)
                textSize = 14f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                if (bu) setTextColor(vurgu)
            })
            kap.addView(satir)
        }
    }

    /** v11.13: İlerleme sekmesi kaldırıldığı için genel ilerleme özetini buraya ekler. */
    private fun ilerlemeOzetiEkle(kok: View, ctx: android.content.Context) {
        val kap = kok.findViewById<LinearLayout>(R.id.plSlots) ?: return
        val d = yogunluk
        val (seri, enIyi) = Store.streakInfo(ctx)
        val haftaOdak = Store.weekFocus(ctx)
        val hedef = Store.getGoalMinutes(ctx)
        val tamamlanan = runCatching { Store.weekCompletions(ctx) }.getOrDefault(0)

        val kart = MaterialCardView(ctx).apply {
            radius = 14 * d
            cardElevation = 0f
            strokeWidth = 0
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (8 * d).toInt() }
            setCardBackgroundColor(
                MaterialColors.getColor(
                    ctx, com.google.android.material.R.attr.colorSurfaceContainer, 0xFFE4E9DC.toInt()
                )
            )
        }
        val ic = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((12 * d).toInt(), (10 * d).toInt(), (12 * d).toInt(), (10 * d).toInt())
        }
        ic.addView(TextView(ctx).apply {
            text = "📊 Genel İlerleme"
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        ic.addView(TextView(ctx).apply {
            text = "🔥 $seri günlük seri (en iyi $enIyi) • ✅ $tamamlanan haftalık tamamlama"
            textSize = 12f
            alpha = 0.9f
            setPadding(0, (4 * d).toInt(), 0, 0)
        })
        ic.addView(TextView(ctx).apply {
            text = if (hedef > 0) "⏱️ $haftaOdak dk haftalık odak (hedef $hedef/gün)" else "⏱️ $haftaOdak dk haftalık odak"
            textSize = 12f
            alpha = 0.9f
            setPadding(0, (2 * d).toInt(), 0, 0)
        })
        kart.addView(ic)
        kart.setOnClickListener { AnalitikActivity.ac(ctx) }
        kap.addView(kart, 0)
    }

    private fun planiCiz(kok: View, ctx: android.content.Context) {
        val kap = kok.findViewById<LinearLayout>(R.id.plSlots)
        kap.removeAllViews()

        val gun = NamazVakti.bugunDuzeltilmis(ctx)
        val simdi = NamazVakti.simdiDakika()
        val aktifDilim = NamazPlan.aktifDilim(gun, simdi)

        val (biten, toplam) = NamazPlan.bugunOzet(ctx)
        kok.findViewById<TextView>(R.id.plSummary).text =
            if (toplam == 0) getString(R.string.nm_plan_empty)
            else getString(R.string.nm_plan_summary, biten, toplam)

        // v11.13: İlerleme sekmesi kaldırıldı — verisi sekmelere paylaştırıldı.
        // Plan sekmesine genel ilerleme özeti (odak, seri, tamamlanan) eklenir.
        ilerlemeOzetiEkle(kok, ctx)

        // 1) v11.06: Gösterişli Vaktin Sözü / Hikmetli Dini Sözler ve Hadisler Kartı
        kap.addView(vaktinSozuKarti(ctx, aktifDilim))
        SekmeVeVeriTasimaMotoru.sekmeTasinanVerileriCiz(ctx, "plan", kap)

        // 2) v11.06: Seher, Kuşluk vb. tüm vakit dilimleri ALT ALTA (dikey, tam genişlikte) sıralanır
        val siralama = if (GorunumAyar.planHeroModu(ctx)) {
            listOf(aktifDilim) + (NamazPlan.Dilim.entries - aktifDilim)
        } else {
            NamazPlan.Dilim.entries.toList()
        }
        siralama.forEach { dilim ->
            val kart = dilimKarti(ctx, dilim, gun, aktifDilim, simdi).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = (12 * yogunluk).toInt()
                }
            }
            kap.addView(kart)
        }
    }

    private fun vaktinSozuKarti(
        ctx: android.content.Context,
        dilim: NamazPlan.Dilim
    ): View {
        val (baslik, metin) = DiniSozMotoru.vaktinSozunuGetir(dilim)
        val yogunluk = ctx.resources.displayMetrics.density
        fun dp(v: Int) = (v * yogunluk).toInt()

        val card = com.google.android.material.card.MaterialCardView(ctx).apply {
            radius = 16 * yogunluk
            cardElevation = 2 * yogunluk
            strokeWidth = dp(1)
            strokeColor = com.google.android.material.color.MaterialColors.getColor(
                ctx,
                com.google.android.material.R.attr.colorPrimary,
                0xFF6200EE.toInt()
            )
            setCardBackgroundColor(
                com.google.android.material.color.MaterialColors.getColor(
                    ctx,
                    com.google.android.material.R.attr.colorPrimaryContainer,
                    0xFFE8EAF6.toInt()
                )
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(14)
            }
        }

        val icLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        val ustSatir = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val emojiTv = TextView(ctx).apply {
            text = "🕌✨ "
            textSize = 20f
        }

        val baslikTv = TextView(ctx).apply {
            text = baslik
            textSize = 15f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(
                com.google.android.material.color.MaterialColors.getColor(
                    ctx,
                    com.google.android.material.R.attr.colorOnPrimaryContainer,
                    0xFF1A237E.toInt()
                )
            )
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val metinTv = TextView(ctx).apply {
            text = metin
            textSize = 14f
            setLineSpacing(dp(3).toFloat(), 1f)
            setTypeface(null, android.graphics.Typeface.ITALIC)
            setTextColor(
                com.google.android.material.color.MaterialColors.getColor(
                    ctx,
                    com.google.android.material.R.attr.colorOnPrimaryContainer,
                    0xFF1A237E.toInt()
                )
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(8)
            }
        }

        val btnDegistir = android.widget.Button(ctx, null, android.R.attr.borderlessButtonStyle).apply {
            text = "🔄 Başka Söz"
            textSize = 12f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(
                com.google.android.material.color.MaterialColors.getColor(
                    ctx,
                    com.google.android.material.R.attr.colorPrimary,
                    0xFF6200EE.toInt()
                )
            )
            setOnClickListener {
                val (yeniBaslik, yeniMetin) = DiniSozMotoru.sonrakiSozuGetir(dilim, metinTv.text.toString())
                baslikTv.text = yeniBaslik
                metinTv.text = yeniMetin
            }
        }

        ustSatir.addView(emojiTv)
        ustSatir.addView(baslikTv)
        ustSatir.addView(btnDegistir)

        icLayout.addView(ustSatir)
        icLayout.addView(metinTv)
        card.addView(icLayout)

        return card
    }

    private fun dilimKarti(
        ctx: android.content.Context,
        dilim: NamazPlan.Dilim,
        gun: NamazVakti.Gun,
        aktifDilim: NamazPlan.Dilim,
        simdi: Int
    ): View {
        val aktif = dilim == aktifDilim
        val kart = MaterialCardView(ctx).apply {
            radius = 16 * yogunluk
            cardElevation = 0f
            strokeWidth = ((if (aktif) 2 else 1) * yogunluk).toInt()
            if (aktif) {
                strokeColor = MaterialColors.getColor(
                    this, com.google.android.material.R.attr.colorPrimary, 0
                )
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (10 * yogunluk).toInt() }
        }

        val ic = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                (14 * yogunluk).toInt(), (12 * yogunluk).toInt(),
                (14 * yogunluk).toInt(), (12 * yogunluk).toInt()
            )
        }

        val bas = gun.saat(dilim.baslangic)
        val bit = gun.saat(dilim.bitis)
        val sure = NamazPlan.dilimSuresi(gun, dilim)

        // Başlık: emoji + ad + ekle düğmesi
        ic.addView(LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(TextView(ctx).apply {
                text = dilim.emoji + " " + getString(dilim.adRes)
                textSize = 14.5f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                )
            })
            // v7.64: bu dilimde biten is varsa hizli temizleme
            if (NamazPlan.dilimIsleri(ctx, dilim).any { it.tamamlandi }) {
                addView(TextView(ctx).apply {
                    text = "🧹"
                    textSize = 14f
                    alpha = 0.7f
                    setPadding((6 * yogunluk).toInt(), 0, (6 * yogunluk).toInt(), 0)
                    background = android.graphics.drawable.RippleDrawable(
                        android.content.res.ColorStateList.valueOf(0x22888888), null, null
                    )
                    isClickable = true
                    setOnClickListener {
                        val adet = NamazPlan.bitenleriTemizle(ctx, dilim)
                        if (adet > 0) {
                            Toast.makeText(
                                ctx, getString(R.string.pe_biten_silindi, adet),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        ciz()
                    }
                })
            }
            addView(TextView(ctx).apply {
                text = "+"
                textSize = 20f
                setPadding((12 * yogunluk).toInt(), 0, (6 * yogunluk).toInt(), 0)
                setTextColor(
                    MaterialColors.getColor(
                        this, com.google.android.material.R.attr.colorPrimary, 0
                    )
                )
                background = android.graphics.drawable.RippleDrawable(
                    android.content.res.ColorStateList.valueOf(0x22888888), null, null
                )
                isClickable = true
                setOnClickListener { isEkleSor(dilim) }
            })
        })

        // Saat aralığı / kalan süre
        ic.addView(TextView(ctx).apply {
            text = if (aktif) {
                getString(
                    R.string.nm_slot_active, bas, bit,
                    NamazPlan.sureMetni(NamazPlan.kalanDakika(gun, dilim, simdi))
                )
            } else {
                getString(R.string.nm_slot_range, bas, bit, NamazPlan.sureMetni(sure))
            }
            textSize = 11.5f
            alpha = 0.75f
            setPadding(0, (2 * yogunluk).toInt(), 0, (2 * yogunluk).toInt())
        })

        // v7.64: planlanan toplam sure — dilime sigiyor mu?
        val planli = NamazPlan.dilimPlanliSure(ctx, dilim)
        if (planli > 0) {
            val tasiyor = sure > 0 && planli > sure
            ic.addView(TextView(ctx).apply {
                text = if (tasiyor) {
                    getString(R.string.pe_toplam_sure, NamazPlan.sureMetni(planli)) +
                        "  " + getString(R.string.pe_dolu)
                } else {
                    getString(R.string.pe_toplam_sure, NamazPlan.sureMetni(planli))
                }
                textSize = 11f
                alpha = 0.8f
                setPadding(0, 0, 0, (6 * yogunluk).toInt())
                if (tasiyor) setTextColor(GrafikDili.HATA)
            })
        }

        // İşler
        val isler = NamazPlan.dilimIsleri(ctx, dilim)
        if (isler.isEmpty()) {
            ic.addView(TextView(ctx).apply {
                text = getString(R.string.nm_slot_hint, getString(dilim.varsayilanIsRes))
                textSize = 12f
                alpha = 0.6f
                setPadding(0, (2 * yogunluk).toInt(), 0, 0)
            })
        } else {
            isler.forEach { i -> ic.addView(isSatiri(ctx, i)) }
        }

        kart.addView(ic)
        return kart
    }

    private fun isSatiri(ctx: android.content.Context, i: NamazPlan.Is): View {
        val satir = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, (6 * yogunluk).toInt(), 0, (6 * yogunluk).toInt())
            background = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x18888888), null, null
            )
            isClickable = true
            setOnClickListener {
                NamazPlan.isTamamla(ctx, i.id)
                ciz()
                // Tamamlanan iş günlük sayaca yazıldı — ana ekran da tazelensin
                (activity as? MainActivity)?.refreshHome()
            }
            setOnLongClickListener {
                isMenusu(i)
                true
            }
        }
        satir.addView(TextView(ctx).apply {
            text = if (i.tamamlandi) "☑" else "☐"
            textSize = 16f
            setPadding(0, 0, (9 * yogunluk).toInt(), 0)
            if (i.tamamlandi) setTextColor(GrafikDili.BASARI)
        })
        satir.addView(TextView(ctx).apply {
            // v7.64: oncelik simgesi + hatirlatma isareti metinle birlikte
            text = buildString {
                if (i.oncelikSimgesi.isNotBlank()) append(i.oncelikSimgesi).append(" ")
                append(i.metin)
                if (i.hatirlat) append("  🔔")
            }
            textSize = 13.5f
            if (i.tamamlandi) {
                paintFlags = paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                alpha = 0.55f
            }
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        })
        // v7.64: sure rozeti
        if (i.sureDk > 0) {
            satir.addView(TextView(ctx).apply {
                text = getString(R.string.pe_dk, i.sureDk)
                textSize = 11f
                alpha = if (i.tamamlandi) 0.4f else 0.8f
                setPadding((8 * yogunluk).toInt(), 0, 0, 0)
                setTextColor(
                    MaterialColors.getColor(
                        this, com.google.android.material.R.attr.colorPrimary, 0
                    )
                )
            })
        }
        return satir
    }

    // ═══════════════════════════════════════════════════════════════
    // İŞ EKLEME / DÜZENLEME
    // ═══════════════════════════════════════════════════════════════

    /** v7.64: zengin ekleme editoru. */
    private fun isEkleSor(dilim: NamazPlan.Dilim) {
        val ctx = context ?: return
        PlanEkleyici.ac(ctx, dilim, null) { ciz() }
    }

    /** v7.64: uzun basinca cikan islem menusu genisletildi. */
    private fun isMenusu(i: NamazPlan.Is) {
        val ctx = context ?: return
        val secenekler = arrayOf(
            getString(R.string.co_edit),
            getString(R.string.pe_sayac_baslat),
            getString(R.string.pe_yukari),
            getString(R.string.pe_asagi),
            "⚡ Başka Bir Vakit Sekmesine Taşı (Sekmeler Arası Değişiklik)",
            getString(R.string.pe_kopyala),
            getString(R.string.delete)
        )
        MaterialAlertDialogBuilder(ctx)
            .setTitle(i.metin)
            .setItems(secenekler) { _, hangi ->
                when (hangi) {
                    0 -> isDuzenleSor(i)
                    1 -> sayacaGonder(i)
                    2 -> { NamazPlan.isTasi(ctx, i.id, true); ciz() }
                    3 -> { NamazPlan.isTasi(ctx, i.id, false); ciz() }
                    4 -> dilimTasiDiyalogu(i)
                    5 -> { NamazPlan.isCogalt(ctx, i.id); ciz() }
                    6 -> { NamazPlan.isSil(ctx, i.id); ciz() }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun dilimTasiDiyalogu(i: NamazPlan.Is) {
        val ctx = context ?: return
        val secenekler = NamazPlan.Dilim.entries.map { it.emoji + " " + getString(it.adRes) }.toTypedArray()
        MaterialAlertDialogBuilder(ctx)
            .setTitle("🏷️ Hangi Vakit Sekmesine Taşıyacaksınız?")
            .setItems(secenekler) { _, idx ->
                val hedefDilim = NamazPlan.Dilim.entries[idx]
                NamazPlan.dilimDegistir(ctx, i.id, hedefDilim)
                ciz()
                Toast.makeText(ctx, "⚡ İş '${getString(hedefDilim.adRes)}' sekmesine taşındı!", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    /** v7.64: isi sayac ekraninda baslatir. */
    private fun sayacaGonder(i: NamazPlan.Is) {
        (activity as? MainActivity)?.openTimer()
        val ctx = context ?: return
        Toast.makeText(ctx, i.metin, Toast.LENGTH_SHORT).show()
    }

    /** v7.64: duzenleme de ayni zengin editorde. */
    private fun isDuzenleSor(i: NamazPlan.Is) {
        val ctx = context ?: return
        val dilim = NamazPlan.Dilim.entries.firstOrNull { it.anahtar == i.dilim }
            ?: NamazPlan.Dilim.SABAH
        PlanEkleyici.ac(ctx, dilim, i) { ciz() }
    }

    private fun sablonSor() {
        val ctx = context ?: return
        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.nm_template)
            .setMessage(R.string.nm_template_ask)
            .setPositiveButton(R.string.add) { _, _ ->
                NamazPlan.sablonYukle(ctx)
                ciz()
                Toast.makeText(ctx, R.string.nm_template_done, Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton(R.string.nm_clear) { _, _ ->
                NamazPlan.tumunuTemizle(ctx)
                ciz()
            }
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
        Yenileyici.kur(this) { ciz() }
        Yenileyici.gorunurluguEsitle(this)
    }
}
