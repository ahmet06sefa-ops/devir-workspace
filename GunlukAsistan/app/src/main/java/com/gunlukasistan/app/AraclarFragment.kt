package com.gunlukasistan.app

import android.os.Bundle
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
import com.google.android.material.tabs.TabLayout

/**
 * v7.30 — Mühendislik Araçları ekranı.
 *
 * İki sekme:
 *   HESAPLAR   — 12 hesap aracı (kiriş, kolon, merdiven, metraj…)
 *   YÖNETMELİK — TS 500 / TBDY 2018 / İmar özetleri, aranabilir
 *
 * Tamamen çevrimdışı çalışır — sahada internet olmadan kullanılır.
 */
class AraclarFragment : Fragment(R.layout.fragment_araclar) {

    private enum class Sekme { HESAP, YONETMELIK, KART }

    private var sekme = Sekme.HESAP
    private var sorgu = ""
    private lateinit var adapter: SatirAdapter
    private val satirlar = mutableListOf<Satir>()

    /** Liste öğesi: başlık ayracı veya tıklanabilir kart. */
    private sealed class Satir {
        data class Baslik(val metin: String) : Satir()
        data class Hesap(val arac: HesapMotoru.Arac) : Satir()
        data class Bilgi(val bolum: YonetmelikVeri.Bolum, val kayit: YonetmelikVeri.Kayit) : Satir()
        data class Deste(val ozet: KartStore.DesteOzet) : Satir()
        data class Eylem(val simge: String, val ad: String, val aciklama: String, val is_: () -> Unit) : Satir()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // v8.6 · Öneri 29: yatay/tablet genişlik sınırı + yoğunluk
        Duzen.uygula(view)
        val recycler = view.findViewById<RecyclerView>(R.id.arRecycler)
        adapter = SatirAdapter()
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        val tabs = view.findViewById<TabLayout>(R.id.arTabs)
        tabs.addTab(tabs.newTab().setText(getString(R.string.tools_tab_calc)))
        tabs.addTab(tabs.newTab().setText(getString(R.string.tools_tab_rules)))
        tabs.addTab(tabs.newTab().setText(getString(R.string.tools_tab_cards)))
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                sekme = when (tab?.position) {
                    1 -> Sekme.YONETMELIK
                    2 -> Sekme.KART
                    else -> Sekme.HESAP
                }
                yenile()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.arSearch)
            .addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    sorgu = s?.toString()?.trim().orEmpty()
                    yenile()
                }
            })

        yenile()
    }

    private fun yenile() {
        val kok = view ?: return
        satirlar.clear()

        if (sekme == Sekme.HESAP) {
            kok.findViewById<TextView>(R.id.arSummary).text =
                getString(R.string.tools_calc_count, HesapMotoru.ARACLAR.size)

            // v9.5 · Öneri 25: fotoğraftan soru çözme.
            // Hesap araçlarının başına konuyor — "takıldım" anında
            // ilk bakılacak yer burası.
            if (sorgu.length < 2) {
                satirlar.add(
                    Satir.Eylem(
                        "📷", getString(R.string.sc_row), getString(R.string.sc_row_sub)
                    ) { SoruCozActivity.ac(requireContext()) }
                )
            }
            val liste = if (sorgu.length >= 2) {
                val q = sorgu.lowercase()
                HesapMotoru.ARACLAR.filter {
                    it.ad.lowercase().contains(q) || it.aciklama.lowercase().contains(q)
                }
            } else {
                HesapMotoru.ARACLAR
            }
            liste.forEach { satirlar.add(Satir.Hesap(it)) }
        } else if (sekme == Sekme.KART) {
            kartSekmesi(kok)
        } else {
            kok.findViewById<TextView>(R.id.arSummary).text =
                getString(R.string.tools_rule_count, YonetmelikVeri.toplamKayit)
            if (sorgu.length >= 2) {
                val bulunan = YonetmelikVeri.ara(sorgu)
                if (bulunan.isEmpty()) {
                    satirlar.add(Satir.Baslik(getString(R.string.tools_no_result, sorgu)))
                } else {
                    satirlar.add(Satir.Baslik(getString(R.string.tools_found, bulunan.size)))
                    bulunan.forEach { (b, k) -> satirlar.add(Satir.Bilgi(b, k)) }
                }
            } else {
                YonetmelikVeri.BOLUMLER.forEach { b ->
                    satirlar.add(Satir.Baslik("${b.simge}  ${b.ad}"))
                    b.kayitlar.forEach { k -> satirlar.add(Satir.Bilgi(b, k)) }
                }
            }
        }
        adapter.notifyDataSetChanged()
    }

    /** v7.33: Bilgi kartları sekmesi. */
    private fun kartSekmesi(kok: View) {
        val context = requireContext()
        val toplam = KartStore.toplamKart(context)
        val bekleyen = KartStore.bekleyenSayisi(context)
        val ogrenilen = KartStore.ogrenilenToplam(context)

        kok.findViewById<TextView>(R.id.arSummary).text =
            if (toplam == 0) getString(R.string.card_empty_summary)
            else getString(R.string.card_summary, toplam, ogrenilen, bekleyen)

        // Hazır desteler yüklenmemişse yükleme kartı göster
        if (!KartStore.hazirYuklendiMi(context)) {
            satirlar.add(
                Satir.Eylem(
                    "📦", getString(R.string.card_install),
                    getString(R.string.card_install_desc, HazirDesteler.toplamKart)
                ) {
                    val n = KartStore.hazirDesteleriYukle(context)
                    Toast.makeText(
                        context, getString(R.string.card_installed, n), Toast.LENGTH_LONG
                    ).show()
                    yenile()
                }
            )
        }

        if (bekleyen > 0) {
            satirlar.add(
                Satir.Eylem(
                    "▶", getString(R.string.card_study_all),
                    getString(R.string.card_study_desc, bekleyen)
                ) { KartActivity.ac(context) }
            )
        }

        satirlar.add(
            Satir.Eylem("✍", getString(R.string.card_add), getString(R.string.card_add_desc)) {
                kartEkleDiyalogu()
            }
        )

        val desteler = KartStore.desteler(context)
        if (desteler.isNotEmpty()) {
            satirlar.add(Satir.Baslik(getString(R.string.card_decks)))
            desteler.forEach { satirlar.add(Satir.Deste(it)) }
        }
    }

    /** v7.33: Kendi kartını ekleme. */
    private fun kartEkleDiyalogu() {
        val context = requireContext()
        val dp = resources.displayMetrics.density
        val pad = (20 * dp).toInt()

        val onAlan = EditText(context).apply {
            hint = getString(R.string.card_front_hint)
            setSingleLine()
        }
        val arkaAlan = EditText(context).apply {
            hint = getString(R.string.card_back_hint)
            minLines = 2
            maxLines = 4
        }
        val desteAlan = EditText(context).apply {
            hint = getString(R.string.card_deck_hint)
            setText(getString(R.string.card_my_deck))
            setSingleLine()
        }

        fun etiket(res: Int) = TextView(context).apply {
            setText(res)
            textSize = 12f
            setPadding(0, (10 * dp).toInt(), 0, (2 * dp).toInt())
        }

        val kap = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, (8 * dp).toInt(), pad, 0)
            addView(etiket(R.string.card_front))
            addView(onAlan)
            addView(etiket(R.string.card_back))
            addView(arkaAlan)
            addView(etiket(R.string.card_deck))
            addView(desteAlan)
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.card_add)
            .setView(androidx.core.widget.NestedScrollView(context).apply { addView(kap) })
            .setPositiveButton(R.string.save) { _, _ ->
                val on = onAlan.text?.toString()?.trim().orEmpty()
                val arka = arkaAlan.text?.toString()?.trim().orEmpty()
                if (on.isBlank() || arka.isBlank()) {
                    Toast.makeText(context, R.string.card_need_both, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                KartStore.kartEkle(
                    context,
                    desteAlan.text?.toString()?.trim().orEmpty()
                        .ifBlank { getString(R.string.card_my_deck) },
                    on, arka
                )
                Toast.makeText(context, R.string.card_added, Toast.LENGTH_SHORT).show()
                yenile()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ═══════════════════════════════════════════════════════════════
    // HESAP PENCERESİ
    // ═══════════════════════════════════════════════════════════════

    private fun hesapAc(arac: HesapMotoru.Arac) {
        val context = requireContext()
        val dp = resources.displayMetrics.density
        val pad = (20 * dp).toInt()

        val girisler = mutableMapOf<String, () -> String>()
        val kap = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, (8 * dp).toInt(), pad, 0)
        }

        kap.addView(TextView(context).apply {
            text = arac.aciklama
            textSize = 12f
            alpha = 0.75f
            setPadding(0, 0, 0, (10 * dp).toInt())
        })

        arac.alanlar.forEach { alan ->
            kap.addView(TextView(context).apply {
                text = if (alan.birim.isBlank()) alan.etiket else "${alan.etiket} (${alan.birim})"
                textSize = 12f
                setPadding(0, (8 * dp).toInt(), 0, (2 * dp).toInt())
            })

            if (alan.secenekler.isNotEmpty()) {
                val spinner = android.widget.Spinner(context).apply {
                    adapter = android.widget.ArrayAdapter(
                        context, android.R.layout.simple_spinner_dropdown_item, alan.secenekler
                    )
                    val i = alan.secenekler.indexOf(alan.varsayilan)
                    if (i >= 0) setSelection(i)
                }
                kap.addView(spinner)
                girisler[alan.anahtar] = { spinner.selectedItem?.toString().orEmpty() }
            } else {
                val giris = EditText(context).apply {
                    setText(alan.varsayilan)
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                        android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                    setSingleLine()
                }
                kap.addView(giris)
                girisler[alan.anahtar] = { giris.text?.toString().orEmpty() }
            }
        }

        MaterialAlertDialogBuilder(context)
            .setTitle("${arac.simge}  ${arac.ad}")
            .setView(androidx.core.widget.NestedScrollView(context).apply { addView(kap) })
            .setPositiveButton(R.string.tools_calculate) { _, _ ->
                val degerler = girisler.mapValues { it.value() }
                sonucGoster(arac, degerler)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun sonucGoster(arac: HesapMotoru.Arac, degerler: Map<String, String>) {
        val context = requireContext()
        val sonuc = HesapMotoru.hesapla(arac.id, degerler)

        if (!sonuc.basarili) {
            Toast.makeText(context, sonuc.uyari, Toast.LENGTH_LONG).show()
            return
        }

        val govde = buildString {
            sonuc.satirlar.forEach { s ->
                if (s.vurgu) append("▸ ") else append("   ")
                append(s.etiket).append(": ").append(s.deger).append("\n")
            }
            if (sonuc.uyari.isNotBlank()) {
                append("\n⚠ ").append(sonuc.uyari)
            }
        }

        MaterialAlertDialogBuilder(context)
            .setTitle("${arac.simge}  ${arac.ad}")
            .setMessage(govde)
            .setPositiveButton(R.string.done, null)
            .setNeutralButton(R.string.tools_recalc) { _, _ -> hesapAc(arac) }
            .setNegativeButton(R.string.tools_share) { _, _ ->
                try {
                    val metin = "${arac.ad}\n\n$govde\n\n— Günlük Asistan"
                    startActivity(
                        android.content.Intent.createChooser(
                            android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, metin)
                            },
                            getString(R.string.tools_share)
                        )
                    )
                } catch (e: Exception) {
                    android.util.Log.w("Araclar", "Paylaşım başarısız", e)
                }
            }
            .show()
    }

    // ═══════════════════════════════════════════════════════════════
    // YÖNETMELİK DETAYI
    // ═══════════════════════════════════════════════════════════════

    private fun bilgiAc(bolum: YonetmelikVeri.Bolum, kayit: YonetmelikVeri.Kayit) {
        val govde = buildString {
            append(kayit.deger)
            if (kayit.not.isNotBlank()) append("\n\n").append(kayit.not)
            append("\n\n📖 ").append(kayit.kaynak)
            append("\n\n").append(getString(R.string.tools_disclaimer))
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(kayit.baslik)
            .setMessage(govde)
            .setPositiveButton(R.string.done, null)
            .setNeutralButton(R.string.src_menu_copy) { _, _ ->
                val pano = requireContext()
                    .getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                    as android.content.ClipboardManager
                pano.setPrimaryClip(
                    android.content.ClipData.newPlainText(
                        kayit.baslik, "${kayit.baslik}\n${kayit.deger}\n${kayit.kaynak}"
                    )
                )
                Toast.makeText(requireContext(), R.string.src_copied, Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    // ═══════════════════════════════════════════════════════════════
    // LİSTE
    // ═══════════════════════════════════════════════════════════════

    private inner class SatirAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val TIP_BASLIK = 0
        private val TIP_KART = 1

        inner class BaslikVH(v: View) : RecyclerView.ViewHolder(v) {
            val metin: TextView = v.findViewById(R.id.ibTitle)
        }

        inner class KartVH(v: View) : RecyclerView.ViewHolder(v) {
            val ikon: TextView = v.findViewById(R.id.iaIcon)
            val baslik: TextView = v.findViewById(R.id.iaTitle)
            val alt: TextView = v.findViewById(R.id.iaSub)
            val kaynak: TextView = v.findViewById(R.id.iaSource)
        }

        override fun getItemViewType(position: Int): Int =
            if (satirlar[position] is Satir.Baslik) TIP_BASLIK else TIP_KART

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inf = LayoutInflater.from(parent.context)
            return if (viewType == TIP_BASLIK) {
                BaslikVH(inf.inflate(R.layout.item_arac_baslik, parent, false))
            } else {
                KartVH(inf.inflate(R.layout.item_arac, parent, false))
            }
        }

        override fun getItemCount(): Int = satirlar.size

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val s = satirlar[position]) {
                is Satir.Baslik -> (holder as BaslikVH).metin.text = s.metin

                is Satir.Hesap -> {
                    val h = holder as KartVH
                    h.ikon.text = s.arac.simge
                    h.baslik.text = s.arac.ad
                    h.alt.text = s.arac.aciklama
                    h.kaynak.visibility = View.GONE
                    h.itemView.setOnClickListener { hesapAc(s.arac) }
                }

                is Satir.Bilgi -> {
                    val h = holder as KartVH
                    h.ikon.text = s.bolum.simge
                    h.baslik.text = s.kayit.baslik
                    h.alt.text = s.kayit.deger
                    h.kaynak.visibility = View.VISIBLE
                    h.kaynak.text = s.kayit.kaynak
                    h.itemView.setOnClickListener { bilgiAc(s.bolum, s.kayit) }
                }

                is Satir.Deste -> {
                    val h = holder as KartVH
                    h.ikon.text = s.ozet.simge
                    h.baslik.text = s.ozet.ad
                    h.alt.text = getString(
                        R.string.card_deck_info,
                        s.ozet.toplam, s.ozet.ogrenilen, s.ozet.yuzde
                    )
                    if (s.ozet.bekleyen > 0) {
                        h.kaynak.visibility = View.VISIBLE
                        h.kaynak.text = getString(R.string.card_deck_due, s.ozet.bekleyen)
                    } else {
                        h.kaynak.visibility = View.GONE
                    }
                    h.itemView.setOnClickListener {
                        if (s.ozet.bekleyen > 0) {
                            KartActivity.ac(requireContext(), s.ozet.ad)
                        } else {
                            Toast.makeText(
                                requireContext(), R.string.card_deck_clear, Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                is Satir.Eylem -> {
                    val h = holder as KartVH
                    h.ikon.text = s.simge
                    h.baslik.text = s.ad
                    h.alt.text = s.aciklama
                    h.kaynak.visibility = View.GONE
                    h.itemView.setOnClickListener { s.is_() }
                }
            }
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
