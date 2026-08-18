package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch

/**
 * v8.5 — Ana ekran düzenleyici (öneri 16).
 *
 * ── Ne yapıyor ──
 * Ana ekrandaki 8 kartı sürükleyerek sıralama, anahtarla gizleme.
 *
 * ── Neden ayrı ekran, neden "düzenleme modu" değil ──
 * İlk tasarımda ana ekranda uzun basınca kartların sallanması ve orada
 * sürüklenmesi vardı (iOS tarzı). İki sorun: (1) ana ekran bir
 * `ScrollView`; sürükleme ile kaydırma çakışıyor, (2) kartların
 * yükseklikleri çok farklı (kahraman kart 200dp, rozet şeridi 40dp),
 * sürüklerken ekran zıplıyor. Ayrı ekranda hepsi eşit yükseklikte
 * satır — sürükleme akıcı.
 *
 * ── Sürükleme tutamağı ──
 * Satırın tamamı değil, yalnız soldaki ⠿ tutamağı sürüklüyor. Böylece
 * listeyi kaydırmak ile sıralamak karışmıyor.
 */
class AnaEkranDuzenActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            context.startActivity(Intent(context, AnaEkranDuzenActivity::class.java))
            (context as? android.app.Activity)?.let { Canlandir.activityGirisi(it) }
        }
    }

    private val liste = mutableListOf<AnaEkranDuzen.Blok>()
    private lateinit var uyarlayici: Uyarlayici
    private var degisti = false

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
        ThemeManager.dinamikRengiUygula(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ana_duzen)

        findViewById<TextView>(R.id.adBaslik).setText(R.string.ad_row)
        findViewById<View>(R.id.adGeri).setOnClickListener { finish() }
        findViewById<View>(R.id.adSifirla).setOnClickListener { sifirlaSor() }

        liste.clear()
        liste.addAll(AnaEkranDuzen.sira(this))

        val recycler = findViewById<RecyclerView>(R.id.adListe)
        uyarlayici = Uyarlayici()
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = uyarlayici
        Canlandir.liste(recycler)

        surukleyiKur(recycler)
    }

    override fun finish() {
        // Değişiklik varsa ana ekran tazelensin
        if (degisti) {
            runCatching { WidgetCommon.refreshAll(this, false) }
        }
        super.finish()
        Canlandir.activityCikisi(this)
    }

    // ------------------------------------------------------------------

    /**
     * Sürükleyerek sıralama.
     *
     * `isLongPressDragEnabled = false` — sürükleme yalnız tutamaçtan
     * başlıyor. Uzun basma açık olsaydı listeyi kaydırmaya çalışan
     * kullanıcı yanlışlıkla sıralama yapardı.
     */
    private fun surukleyiKur(recycler: RecyclerView) {
        val geriCagri = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun isLongPressDragEnabled(): Boolean = false

            override fun onMove(
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder,
                hedef: RecyclerView.ViewHolder
            ): Boolean {
                val a = vh.bindingAdapterPosition
                val b = hedef.bindingAdapterPosition
                if (a == RecyclerView.NO_POSITION || b == RecyclerView.NO_POSITION) return false
                java.util.Collections.swap(liste, a, b)
                uyarlayici.notifyItemMoved(a, b)
                Titresim.tik(vh.itemView)
                return true
            }

            override fun onSwiped(vh: RecyclerView.ViewHolder, yon: Int) = Unit

            override fun onSelectedChanged(vh: RecyclerView.ViewHolder?, durum: Int) {
                super.onSelectedChanged(vh, durum)
                if (durum == ItemTouchHelper.ACTION_STATE_DRAG) {
                    vh?.itemView?.alpha = 0.82f
                    vh?.itemView?.let { Titresim.uzunBasma(it) }
                }
            }

            override fun clearView(rv: RecyclerView, vh: RecyclerView.ViewHolder) {
                super.clearView(rv, vh)
                vh.itemView.alpha = 1f
                // Sürükleme bitti — sırayı kaydet
                AnaEkranDuzen.siraKaydet(this@AnaEkranDuzenActivity, liste)
                degisti = true
            }
        }
        val yardimci = ItemTouchHelper(geriCagri)
        yardimci.attachToRecyclerView(recycler)
        uyarlayici.surukleyici = yardimci
    }

    private fun sifirlaSor() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(R.string.ad_sifirla)
            .setMessage(R.string.ad_sifirla_soru)
            .setPositiveButton(R.string.ad_sifirla) { _, _ ->
                AnaEkranDuzen.varsayilanaDon(this)
                liste.clear()
                liste.addAll(AnaEkranDuzen.bloklar)
                uyarlayici.notifyDataSetChanged()
                degisti = true
                Titresim.dokunus(findViewById(R.id.adListe))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ------------------------------------------------------------------

    private inner class Uyarlayici : RecyclerView.Adapter<Uyarlayici.Tutucu>() {

        var surukleyici: ItemTouchHelper? = null

        inner class Tutucu(v: View) : RecyclerView.ViewHolder(v) {
            val tutamac: TextView = v.findViewById(R.id.adTutamac)
            val simge: TextView = v.findViewById(R.id.adSimge)
            val baslik: TextView = v.findViewById(R.id.adAd)
            val alt: TextView = v.findViewById(R.id.adAlt)
            val svic: MaterialSwitch = v.findViewById(R.id.adSvic)
            val boyut: android.widget.LinearLayout = v.findViewById(R.id.adBoyut)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Tutucu =
            Tutucu(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_ana_duzen, parent, false)
            )

        override fun getItemCount(): Int = liste.size

        override fun onBindViewHolder(holder: Tutucu, position: Int) {
            val blok = liste[position]
            val ctx = holder.itemView.context

            holder.simge.text = blok.simge
            holder.baslik.setText(blok.baslikRes)

            val gizli = AnaEkranDuzen.gizliMi(ctx, blok.kod)
            holder.svic.setOnCheckedChangeListener(null)
            holder.svic.isChecked = !gizli
            holder.svic.isEnabled = !blok.zorunlu
            holder.alt.text = when {
                blok.zorunlu -> getString(R.string.ad_zorunlu)
                gizli -> getString(R.string.ad_gizli)
                else -> getString(R.string.ad_gorunur)
            }
            holder.itemView.alpha = if (gizli && !blok.zorunlu) 0.55f else 1f

            holder.svic.setOnCheckedChangeListener { dugme, gorunur ->
                if (!dugme.isPressed) return@setOnCheckedChangeListener
                AnaEkranDuzen.gizle(ctx, blok.kod, !gorunur)
                degisti = true
                Titresim.dokunus(dugme)
                notifyItemChanged(holder.bindingAdapterPosition)
            }

            // v10.18 — boyut kademesi + katlama çipleri (programatik; satır XML'i sade)
            boyutCipleriKur(holder, blok, ctx)

            // Yalnız tutamaçtan sürükleme
            holder.tutamac.setOnTouchListener { _, olay ->
                if (olay.actionMasked == android.view.MotionEvent.ACTION_DOWN) {
                    surukleyici?.startDrag(holder)
                }
                false
            }
        }

        /** v10.18: satır içi boyut kademesi + katlama çipleri (programatik doldurulur). */
        private fun boyutCipleriKur(
            holder: Tutucu,
            blok: AnaEkranDuzen.Blok,
            ctx: android.content.Context
        ) {
            val kap = holder.boyut
            kap.removeAllViews()
            val yog = ctx.resources.displayMetrics.density

            fun cip(metin: String, secili: Boolean, tikla: () -> Unit) = TextView(ctx).apply {
                text = metin
                textSize = 11.5f
                gravity = android.view.Gravity.CENTER
                setPadding(
                    (10 * yog).toInt(), (5 * yog).toInt(),
                    (10 * yog).toInt(), (5 * yog).toInt()
                )
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = (5 * yog).toInt() }
                val vurgu = com.google.android.material.color.MaterialColors.getColor(
                    this, com.google.android.material.R.attr.colorPrimary, 0
                )
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = 14 * yog
                    if (secili) {
                        setColor((vurgu and 0x00FFFFFF) or 0x33000000)
                        setStroke((1.2f * yog).toInt(), vurgu)
                    } else {
                        setColor(0x14888888)
                    }
                }
                if (secili) setTextColor(vurgu)
                isClickable = true
                setOnClickListener { tikla() }
            }

            val seciliBoyut = AnaEkranDuzen.boyutKademe(ctx, blok.kod)
            listOf(
                getString(R.string.du_kompakt) to 0,
                getString(R.string.du_normal) to 1,
                getString(R.string.du_genis) to 2
            ).forEach { (ad, kademe) ->
                kap.addView(
                    cip(ad, seciliBoyut == kademe) {
                        AnaEkranDuzen.setBoyutKademe(ctx, blok.kod, kademe)
                        degisti = true
                        notifyItemChanged(holder.bindingAdapterPosition)
                    }
                )
            }
            if (blok.katlanabilir) {
                val katli = AnaEkranDuzen.katliMi(ctx, blok.kod)
                kap.addView(
                    cip(getString(if (katli) R.string.du_ac else R.string.du_katla), katli) {
                        AnaEkranDuzen.setKatli(ctx, blok.kod, !katli)
                        degisti = true
                        notifyItemChanged(holder.bindingAdapterPosition)
                    }
                )
            }
        }
    }
}
