package com.gunlukasistan.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Notlar: başlık + içerik + isteğe bağlı fotoğraf eki.
 * Fotoğraflar küçültülerek uygulama klasörüne kopyalanır.
 */
class NotesFragment : Fragment(R.layout.fragment_notes) {

    private lateinit var adapter: NotesAdapter
    private lateinit var emptyText: TextView
    private val notes = mutableListOf<Store.Note>()

    /** v8.2: liste animasyonu için. */
    private var recycler: RecyclerView? = null

    /** v10.32 · Katalog #23: seçili renk filtresi (null = tümü; NotRenk ton dizini). */
    private var renkFiltre: Int? = null

    /** v10.34 · Katalog #21: liste arama metni (kilitli notlar asla filtrelenmez aramaya). */
    private var aramaMetin = ""

    /** v10.35 · Katalog #37: arşiv görünümünde miyiz? */
    private var arsivModu = false

    /** v10.35: çip şeridindeki özel "arşiv" çipi için ayrık değer (ton değildir). */
    private val ARSIV_CIP = -7


    /** v8.3: zengin boş durum düzeni. */
    private var bosDurum: View? = null

    // Düzenlenen nota seçilen fotoğrafın geçici adı
    private var pendingImage: String = ""
    private var dialogPreview: ImageView? = null
    private var dialogRemoveBtn: TextView? = null

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) attachImage(uri)
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // v8.6 · Öneri 29: yatay/tablet genişlik sınırı + yoğunluk
        Duzen.uygula(view)
        view.findViewById<TextView>(R.id.headerText).setText(R.string.tab_notes)

        // v10.34 · Katalog #21: liste üstü arama çubuğu
        view.findViewById<android.widget.FrameLayout>(R.id.searchSlot)?.let { slot ->
            val yo = resources.displayMetrics.density
            val giris = android.widget.EditText(requireContext()).apply {
                hint = getString(R.string.w34_ara_hint)
                setSingleLine(true)
                setPadding((20 * yo).toInt(), 0, (20 * yo).toInt(), 0)
            }
            giris.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    aramaMetin = s?.toString()?.trim() ?: ""
                    reload()
                }
            })
            slot.removeAllViews()
            slot.addView(giris)
            slot.visibility = View.VISIBLE
        }
        emptyText = view.findViewById(R.id.emptyText)
        emptyText.setText(R.string.notes_empty)
        bosDurum = BosEkran.kur(
            emptyText, BosEkran.Tur.NOT,
            getString(R.string.be_not_baslik),
            getString(R.string.be_not_aciklama),
            getString(R.string.be_not_eylem)
        ) { showNoteEditor(null) }

        val recycler = view.findViewById<RecyclerView>(R.id.recycler)
        adapterKur(recycler)
        recycler.layoutManager = LinearLayoutManager(requireContext())

        // v10.35 · Katalog #34: kompakt ⇄ kart görünümü geçişi
        val tg = view.findViewById<TextView>(R.id.gorunumToggle)
        fun gorunumYaz() {
            tg?.text = getString(
                if (NotGorunum.kompaktMi(requireContext())) {
                    R.string.w35_gorunum_kart
                } else {
                    R.string.w35_gorunum_kompakt
                }
            )
        }
        gorunumYaz()
        tg?.setOnClickListener {
            NotGorunum.degistir(requireContext())
            gorunumYaz()
            adapterKur(recycler)
        }

        // v8.2 · Öneri 5 + 4
        this.recycler = recycler
        Canlandir.liste(recycler)
        Kaydirma.kur(
            recycler,
            Kaydirma.Ayar(
                solaEtiket = getString(R.string.delete),
                solaIkon = "🗑",
                solaRenk = 0xFFD9534F.toInt()
            ),
            sola = { pos ->
                val note = notes.getOrNull(pos)
                if (note == null) {
                    adapter.notifyDataSetChanged()
                } else {
                    // Resim dosyası hemen silinmiyor: geri alınırsa kayıp
                    // olmasın diye. Temizlik açılışta yapılıyor (v7.72).
                    // v10.34 · Katalog #26: kilitli not kaydırarak da PIN'siz silinemez
                    notGuvenli(note) { h ->
                        Store.deleteNoteUndoable(requireContext(), h.id)
                        reload()
                        geriAlSun(getString(R.string.ga_not_silindi))
                    }
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    private fun reload() {
        // v8.9 · Öneri 17: hedefli güncelleme için önceki hâl
        val eskiListe = notes.toList()
        notes.clear()
        // v10.27 (öneri #22): sabitlenen notlar listenin başında kalır
        val sirali = Store.loadNotes(requireContext()).sortedByDescending { it.createdAt }
        // v10.32 · Katalog #23: renk filtresi (null = tümü geçer)
        // v10.35 · Katalog #37: arşiv bölmesi (mod dışında arşivli görünmez)
        val arsivKume = NotArsiv.kume(requireContext())
        val bolunmus = if (arsivModu) {
            sirali.filter { arsivKume.contains(it.id) }
        } else {
            sirali.filterNot { arsivKume.contains(it.id) }
        }
        val seciliTon = renkFiltre
        val gorunen = if (seciliTon == null) {
            bolunmus
        } else {
            bolunmus.filter { NotRenk.ton(requireContext(), it.id) == seciliTon }
        }
        // v10.34 · Katalog #21: metin araması — kilitli notlar içerikten aranmaz
        val ara = aramaMetin
        val gorunen2 = if (ara.isEmpty()) {
            gorunen
        } else {
            gorunen.filter {
                NotKilit.kilitliMi(requireContext(), it.id) ||
                    it.title.contains(ara, ignoreCase = true) ||
                    it.content.contains(ara, ignoreCase = true)
            }
        }
        notes.addAll(NotSabitle.sabitOnce(gorunen2, NotSabitle.pinler(requireContext())) { it.id })
        ListeFark.notlar(adapter, eskiListe, notes)
        if (eskiListe.isEmpty()) Canlandir.tekrarOynat(recycler)
        emptyText.visibility = View.GONE
        BosEkran.goster(bosDurum, notes.isEmpty())
        renkFiltreTazele()
    }

    /** v10.32 · Katalog #23: renk şeridini yeniden kurar (renkli not yoksa gizler). */
    private fun renkFiltreTazele() {
        val kok = view ?: return
        val kutu = kok.findViewById<View>(R.id.renkFiltreKutu) ?: return
        val satir = kok.findViewById<android.widget.LinearLayout>(R.id.renkFiltreSatir) ?: return
        val kullanilan = NotRenk.kullanilanTonlar(requireContext())
        val arsivSayi = NotArsiv.sayi(requireContext())
        // Filtre artık geçersizse (renk hiç kalmadıysa) sıfırla
        if (renkFiltre != null && renkFiltre !in kullanilan) renkFiltre = null
        if (arsivSayi == 0) arsivModu = false
        if (kullanilan.isEmpty() && arsivSayi == 0) {
            kutu.visibility = View.GONE
            satir.removeAllViews()
            return
        }
        kutu.visibility = View.VISIBLE
        satir.removeAllViews()
        satir.addView(renkCipi(getString(R.string.w32_renk_filtre_tumu), null))
        kullanilan.forEach { ton -> satir.addView(renkCipi(NotRenk.TONLAR[ton].ad, ton)) }
        satir.addView(renkCipi(getString(R.string.w35_arsiv_cip, arsivSayi), ARSIV_CIP))
    }

    private fun renkCipi(ad: String, ton: Int?): android.widget.TextView {
        val tv = android.widget.TextView(requireContext())
        val yo = resources.displayMetrics.density
        tv.text = ad
        val secili = if (ton == ARSIV_CIP) arsivModu else (renkFiltre == ton && !arsivModu)
        val renk = when {
            ton == ARSIV_CIP -> 0xFF8E8E93.toInt()
            ton == null -> 0xFF9E9E9E.toInt()
            else -> NotRenk.tonRenk(ton)
        }
        val gd = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 18f * yo
            if (secili) setColor(renk) else setStroke((1.5f * yo).toInt(), renk)
        }
        tv.background = gd
        tv.setTextColor(if (secili) 0xFF1A1A1A.toInt() else renk)
        tv.setTextSize(
            android.util.TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.ga_yazi_kucuk)
        )
        tv.setPadding((14 * yo).toInt(), (6 * yo).toInt(), (14 * yo).toInt(), (6 * yo).toInt())
        tv.layoutParams = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { marginEnd = (8 * yo).toInt() }
        tv.setOnClickListener {
            Titresim.dokunus(it)
            if (ton == ARSIV_CIP) {
                arsivModu = !arsivModu
                if (arsivModu) renkFiltre = null
            } else {
                renkFiltre = ton
                arsivModu = false
            }
            reload()
        }
        return tv
    }

    // ---------------- v10.27 (öneri #22): Not sabitleme ----------------

    /** Uzun basışla sabitle/çöz; liste yeniden sıralanır, toast bildirir. */
    /** v10.28 · Katalog #30: uzun basış menüsü — sabitleme + .txt paylaşımı. */
    private fun notMenuGoster(note: Store.Note) {
        val ctx = context ?: return
        val sabitMi = NotSabitle.sabitMi(ctx, note.id)
        val pinMetni = if (sabitMi) {
            getString(R.string.w28_not_menu_coz)
        } else {
            getString(R.string.w28_not_menu_pin)
        }
        // v10.30: dinamik eylem listesi — bağlantı yalnız varsa görünür
        val eylemler = mutableListOf<Pair<String, () -> Unit>>()
        eylemler.add(pinMetni to { notSabitToggle(note) })
        eylemler.add(getString(R.string.w32_menu_renk) to { renkDiyalog(note) })
        // v10.35 · Katalog #37: arşive kaldır / geri getir
        if (NotArsiv.arsivliMi(ctx, note.id)) {
            eylemler.add(getString(R.string.w35_menu_arsivden) to {
                NotArsiv.yaz(ctx, note.id, false)
                reload()
                Toast.makeText(ctx, R.string.w35_arsivden_cikti, Toast.LENGTH_SHORT).show()
            })
        } else {
            eylemler.add(getString(R.string.w35_menu_arsivle) to {
                NotArsiv.yaz(ctx, note.id, true)
                reload()
                Toast.makeText(ctx, R.string.w35_arsivlendi, Toast.LENGTH_SHORT).show()
            })
        }
        // v10.34 · Katalog #26: tek notu PIN arkasına al/aç
        if (NotKilit.kilitliMi(ctx, note.id)) {
            eylemler.add(getString(R.string.w34_menu_kilitac) to {
                notGuvenli(note) { h ->
                    NotKilit.yaz(ctx, h.id, false)
                    reload()
                    Toast.makeText(ctx, R.string.w34_kilit_acildi, Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            eylemler.add(getString(R.string.w34_menu_kilitle) to {
                if (!KilitDepo.kuruluMu(ctx)) {
                    Toast.makeText(ctx, R.string.w34_kilit_pin_gerek, Toast.LENGTH_LONG).show()
                } else {
                    NotKilit.yaz(ctx, note.id, true)
                    reload()
                    Toast.makeText(ctx, R.string.w34_kilitlendi, Toast.LENGTH_SHORT).show()
                }
            })
        }
        eylemler.add(getString(R.string.w28_not_menu_paylas) to { notuPaylas(note) })
        if (NotSurum.gecmis(ctx, note.id).isNotEmpty()) {
            eylemler.add(getString(R.string.w32_menu_surum) to { surumDiyalog(note) })
        }
        if (NotBaglant.ilkUrl(note.content + "\n" + note.title) != null) {
            eylemler.add(getString(R.string.w30_not_menu_baglant) to { baglantiAc(note) })
        }
        eylemler.add(getString(R.string.w30_not_menu_birlestir) to { birlestirSec(note) })
        com.google.android.material.dialog.MaterialAlertDialogBuilder(ctx)
            .setItems(eylemler.map { it.first }.toTypedArray()) { _, i ->
                eylemler[i].second()
            }
            .show()
    }

    /** v10.30 · Katalog #31: nottaki ilk bağlantıyı tarayıcıda açar. */
    private fun baglantiAc(note: Store.Note) {
        val ctx = context ?: return
        val url = NotBaglant.ilkUrl(note.content + "\n" + note.title)
        if (url == null) {
            Toast.makeText(ctx, R.string.w30_url_yok, Toast.LENGTH_SHORT).show()
            return
        }
        runCatching {
            startActivity(
                android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
            )
        }.onFailure {
            Toast.makeText(ctx, R.string.w30_url_acilmadi, Toast.LENGTH_SHORT).show()
        }
    }

    /** v10.30 · Katalog #33: birleştirilecek ikinci notu seçtirir. */
    private fun birlestirSec(note: Store.Note) {
        val ctx = context ?: return
        val digerleri = Store.loadNotes(requireContext()).filter { it.id != note.id }
        if (digerleri.isEmpty()) {
            Toast.makeText(ctx, R.string.w30_birlestir_yok, Toast.LENGTH_SHORT).show()
            return
        }
        val adlar = digerleri.map {
            it.title.ifBlank { it.content.replace("\n", " ").take(40) }.ifBlank { "…" }
        }.toTypedArray()
        com.google.android.material.dialog.MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.w30_birlestir_baslik)
            .setItems(adlar) { _, i -> birlestirOnay(note, digerleri[i]) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun birlestirOnay(hedef: Store.Note, diger: Store.Note) {
        val ctx = context ?: return
        val ad = diger.title.ifBlank { diger.content.take(40) }
        com.google.android.material.dialog.MaterialAlertDialogBuilder(ctx)
            .setMessage(getString(R.string.w30_birlestir_soru, ad))
            .setPositiveButton(R.string.w29_olustur) { _, _ -> birlestir(hedef, diger) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** Birleşim: önce hedef güncellenip diske yazılır, sonra diğer geri-alınabilir silinir. */
    private fun birlestir(hedef: Store.Note, diger: Store.Note) {
        val ctx = context ?: return
        // v10.32: birleşim öncesi hâl sürüm geçmişine; TAM liste üstünden (filtre güvenli)
        NotSurum.gecmiseIt(ctx, hedef)
        val tam = Store.loadNotes(ctx)
        val h = tam.firstOrNull { it.id == hedef.id } ?: return
        h.title = NotBirlestir.baslik(h.title, diger.title)
        h.content = NotBirlestir.govde(h.content, diger.content)
        Store.saveNotes(ctx, tam)
        Store.deleteNoteUndoable(ctx, diger.id)
        reload()
        geriAlSun(getString(R.string.w30_birlestirildi))
    }

    /** Tek notu .txt dosyası olarak paylaşır (FileProvider + geçici okuma izni). */
    private fun notuPaylas(note: Store.Note) {
        val ctx = context ?: return
        try {
            val govde = if (note.title.isBlank()) {
                note.content
            } else {
                note.title + "\n\n" + note.content
            }
            val klasor = File(ctx.cacheDir, "notlar").apply { mkdirs() }
            val dosya = File(klasor, "not_" + note.id + ".txt")
            dosya.writeText(govde)
            val uri = androidx.core.content.FileProvider.getUriForFile(
                ctx, ctx.packageName + ".fileprovider", dosya
            )
            val niyet = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                putExtra(android.content.Intent.EXTRA_TEXT, govde)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(
                android.content.Intent.createChooser(niyet, getString(R.string.w28_not_paylas_secici))
            )
        } catch (e: Exception) {
            android.util.Log.w("NotesFragment", "Not paylaşılamadı", e)
            Toast.makeText(ctx, R.string.w28_not_paylas_hata, Toast.LENGTH_SHORT).show()
        }
    }

    /** v10.29 · Katalog #28: notun her satırını ayrı göreve çevirir (onaylı). */
    private fun satirlariGoreveCevir(metin: String) {
        val ctx = context ?: return
        val satirlar = NotOlcum.satirlariAyikla(metin)
        if (satirlar.isEmpty()) {
            Toast.makeText(ctx, R.string.w29_satir_yok, Toast.LENGTH_SHORT).show()
            return
        }
        com.google.android.material.dialog.MaterialAlertDialogBuilder(ctx)
            .setMessage(getString(R.string.w29_gorev_soru, satirlar.size))
            .setPositiveButton(R.string.w29_olustur) { _, _ ->
                runCatching {
                    val tasks = Store.loadTasks(ctx)
                    val simdi = System.currentTimeMillis()
                    satirlar.forEachIndexed { i, satir ->
                        tasks.add(
                            Store.Task(
                                id = simdi + i,
                                text = satir.take(120),
                                done = false,
                                createdAt = simdi
                            )
                        )
                    }
                    Store.saveTasks(ctx, tasks)
                    runCatching { Titresim.basari(ctx) }
                    Toast.makeText(
                        ctx, getString(R.string.w29_gorev_oldu, satirlar.size), Toast.LENGTH_LONG
                    ).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** v10.32 · Katalog #23: 5 pastel ton + renksiz seçici. */
    /**
     * v10.38 · Katalog #25: not hatırlatıcısı diyaloğu.
     * Hatırlatıcı kuruluysa kaldır/yeniden-kur seçeneği sunar.
     */
    private fun notHatirlaticiDiyalog(note: Store.Note, sonrasi: () -> Unit) {
        val mevcut = NotHatirlatici.zaman(requireContext(), note.id)
        if (mevcut > 0L) {
            val fmt = java.text.SimpleDateFormat("d MMM HH:mm", java.util.Locale("tr"))
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.w38_hatirlat_mevcut, fmt.format(java.util.Date(mevcut))))
                .setItems(
                    arrayOf(
                        getString(R.string.w38_hatirlat_kaldir),
                        getString(R.string.w38_hatirlat_yeniden)
                    )
                ) { _, hangi ->
                    when (hangi) {
                        0 -> {
                            NotHatirlatici.iptal(requireContext(), note.id)
                            android.widget.Toast.makeText(
                                requireContext(), R.string.w38_hatirlat_kaldirildi,
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            sonrasi()
                        }
                        else -> notTarihSaatSec(note, sonrasi)
                    }
                }
                .show()
        } else {
            notTarihSaatSec(note, sonrasi)
        }
    }

    /** v10.38 · Katalog #25: tarih + saat seçtirir ve hatırlatıcıyı kurar. */
    private fun notTarihSaatSec(note: Store.Note, sonrasi: () -> Unit) {
        val simdi = java.util.Calendar.getInstance()
        android.app.DatePickerDialog(
            requireContext(),
            { _, y, m, g ->
                android.app.TimePickerDialog(
                    requireContext(),
                    { _, sa, dk ->
                        val hedef = java.util.Calendar.getInstance().apply {
                            set(y, m, g, sa, dk, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        if (hedef <= System.currentTimeMillis()) {
                            android.widget.Toast.makeText(
                                requireContext(), R.string.w38_hatirlat_gecmis,
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            val baslik = note.title.ifBlank { note.content.take(40) }
                            val ok = NotHatirlatici.kur(requireContext(), note.id, baslik, hedef)
                            if (ok) {
                                val fmt = java.text.SimpleDateFormat("d MMM HH:mm", java.util.Locale("tr"))
                                android.widget.Toast.makeText(
                                    requireContext(),
                                    getString(R.string.w38_hatirlat_kuruldu, fmt.format(java.util.Date(hedef))),
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                            sonrasi()
                        }
                    },
                    simdi.get(java.util.Calendar.HOUR_OF_DAY),
                    simdi.get(java.util.Calendar.MINUTE),
                    true
                ).show()
            },
            simdi.get(java.util.Calendar.YEAR),
            simdi.get(java.util.Calendar.MONTH),
            simdi.get(java.util.Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis() - 1000
        }.show()
    }

    private fun renkDiyalog(note: Store.Note) {
        val ctx = context ?: return
        val simdiki = NotRenk.ton(ctx, note.id)
        val adlar = NotRenk.TONLAR.mapIndexed { i, t ->
            (if (simdiki == i) "● " else "○ ") + t.ad
        }.toMutableList()
        adlar.add(getString(R.string.w32_renk_yok))
        com.google.android.material.dialog.MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.w32_renk_baslik)
            .setItems(adlar.toTypedArray()) { _, i ->
                NotRenk.yaz(ctx, note.id, if (i == adlar.lastIndex) null else i)
                reload()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** v10.32 · Katalog #29: son 5 sürüm listesi → geri yükleme. */
    private fun surumDiyalog(note: Store.Note) {
        val ctx = context ?: return
        val kayitlar = NotSurum.gecmis(ctx, note.id)
        if (kayitlar.isEmpty()) {
            Toast.makeText(ctx, R.string.w32_surum_bos, Toast.LENGTH_SHORT).show()
            return
        }
        val fmt = java.text.SimpleDateFormat("d MMM HH:mm", java.util.Locale("tr", "TR"))
        val adlar = kayitlar.map { k ->
            val oz = k.baslik.ifBlank { k.icerik }.replace("\n", " ").take(34)
            getString(R.string.w32_surum_madde, fmt.format(java.util.Date(k.zaman)), oz)
        }.toTypedArray()
        com.google.android.material.dialog.MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.w32_surum_baslik)
            .setItems(adlar) { _, i -> surumGeriOnay(note, kayitlar[i]) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun surumGeriOnay(note: Store.Note, k: NotSurum.Kayit) {
        val ctx = context ?: return
        val fmt = java.text.SimpleDateFormat("d MMM HH:mm", java.util.Locale("tr", "TR"))
        com.google.android.material.dialog.MaterialAlertDialogBuilder(ctx)
            .setMessage(getString(R.string.w32_surum_geri, fmt.format(java.util.Date(k.zaman))))
            .setPositiveButton(R.string.w32_surum_geri_yukle) { _, _ -> surumGeriYukle(note, k) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** Geri yükleme de geçmişe itilir — yanlış seçim bile geri alınabilir. */
    private fun surumGeriYukle(note: Store.Note, k: NotSurum.Kayit) {
        val ctx = context ?: return
        NotSurum.gecmiseIt(ctx, note)
        val tam = Store.loadNotes(ctx)
        val h = tam.firstOrNull { it.id == note.id } ?: return
        h.title = k.baslik
        h.content = k.icerik
        h.image = k.goruntu
        Store.saveNotes(ctx, tam)
        reload()
        Toast.makeText(ctx, R.string.w32_surum_geri_alindi, Toast.LENGTH_LONG).show()
    }

    /** v10.35 · Katalog #34: görünüme göre (kart/kompakt) adaptörü kurar. */
    private fun adapterKur(recycler: RecyclerView) {
        adapter = NotesAdapter(
            items = notes,
            duzenKaynak = if (NotGorunum.kompaktMi(requireContext())) {
                R.layout.item_note_compact
            } else {
                R.layout.item_note
            },
            onClick = { note -> notGuvenli(note) { showNoteEditor(it) } },
            onDelete = { note -> confirmDelete(note) },
            // v10.27 (öneri #22): satıra uzun bas → sabitle/çöz
            onLong = { note -> notGuvenli(note) { notMenuGoster(it) } }
        )
        recycler.adapter = adapter
    }

    /**
     * v10.34 · Katalog #26: not kilitliyse eylemden önce PIN doğrulaması.
     * Kaba-kuvvet koruması uygulama kilidiyle ORTAK havuz ([KilitDepo]).
     */
    private fun notGuvenli(note: Store.Note, devam: (Store.Note) -> Unit) {
        val ctx = context ?: return
        if (!NotKilit.kilitliMi(ctx, note.id)) {
            devam(note)
            return
        }
        if (!KilitDepo.kuruluMu(ctx)) {
            Toast.makeText(ctx, R.string.w34_kilit_pin_gerek, Toast.LENGTH_LONG).show()
            return
        }
        val simdi = System.currentTimeMillis()
        val bekle = KilitDepo.denemeDurumu(ctx)
        if (KilitMantik.beklemedeMi(bekle, simdi)) {
            Toast.makeText(
                ctx,
                getString(R.string.w34_kilit_bekle, KilitMantik.kalanBeklemeSn(bekle, simdi)),
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val giris = android.widget.EditText(ctx).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            filters = arrayOf(android.text.InputFilter.LengthFilter(KilitMantik.PIN_MAX))
            hint = getString(R.string.w34_kilit_baslik)
        }
        val kutu = android.widget.FrameLayout(ctx).apply {
            val bo = (20 * resources.displayMetrics.density).toInt()
            setPadding(bo, 0, bo, 0)
            addView(giris)
        }
        com.google.android.material.dialog.MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.w34_kilit_baslik)
            .setView(kutu)
            .setPositiveButton(R.string.ok) { _, _ ->
                val pin = giris.text.toString()
                if (KilitDepo.pinDogruMu(ctx, pin)) {
                    KilitDepo.dogruKaydet(ctx)
                    devam(note)
                } else {
                    val yeni = KilitDepo.yanlisKaydet(ctx, System.currentTimeMillis())
                    Toast.makeText(
                        ctx,
                        getString(R.string.w34_kilit_yanlis, KilitMantik.kalanHak(yeni)),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun notSabitToggle(note: Store.Note) {
        val sabit = NotSabitle.degistir(requireContext(), note.id)
        Titresim.uzunBasma(view)
        reload()
        Toast.makeText(
            requireContext(),
            if (sabit) R.string.w24_not_sabitlendi else R.string.w24_not_cozuldu,
            Toast.LENGTH_SHORT
        ).show()
    }

    // ---------------- Fotoğraf eki ----------------

    private fun imagesDir(): File =
        File(requireContext().filesDir, "notes_img").apply { mkdirs() }

    private fun attachImage(uri: Uri) {
        try {
            // 1. aşama: boyutları öğren
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            requireContext().contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            // 2. aşama: hedef ~1280px olacak şekilde küçült
            var sample = 1
            val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
            while (maxSide / (sample * 2) > 1280) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = requireContext().contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: throw IllegalStateException("bitmap yok")

            val name = "img_${System.currentTimeMillis()}.jpg"
            FileOutputStream(File(imagesDir(), name)).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 82, out)
            }
            bitmap.recycle()
            pendingImage = name
            refreshDialogImagePreview()
            Toast.makeText(requireContext(), R.string.note_photo_added, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), R.string.note_photo_error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshDialogImagePreview() {
        val preview = dialogPreview ?: return
        if (pendingImage.isEmpty()) {
            preview.visibility = View.GONE
            dialogRemoveBtn?.visibility = View.GONE
            return
        }
        val file = File(imagesDir(), pendingImage)
        if (file.exists()) {
            preview.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
            preview.visibility = View.VISIBLE
            dialogRemoveBtn?.visibility = View.VISIBLE
        }
    }

    /** [existing] null ise yeni not, değilse düzenleme penceresi açar. */
    fun showNoteEditor(existing: Store.Note?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_note, null)
        val titleInput = dialogView.findViewById<EditText>(R.id.inputTitle)
        val contentInput = dialogView.findViewById<EditText>(R.id.inputContent)
        val btnPick = dialogView.findViewById<Button>(R.id.btnPickImage)
        dialogPreview = dialogView.findViewById(R.id.noteImagePreview)
        dialogRemoveBtn = dialogView.findViewById(R.id.btnRemoveImage)

        pendingImage = existing?.image ?: ""

        existing?.let {
            titleInput.setText(it.title)
            contentInput.setText(it.content)
        }
        refreshDialogImagePreview()

        // v10.29 · Katalog #27: canlı kelime/karakter sayacı
        val sayac = dialogView.findViewById<android.widget.TextView>(R.id.notSayac)
        fun sayacYaz() {
            val m = contentInput.text?.toString() ?: ""
            sayac?.text = getString(R.string.w29_not_sayac, NotOlcum.kelimeS(m), NotOlcum.karakterS(m))
        }
        contentInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { sayacYaz() }
        })
        sayacYaz()

        // v10.29 · Katalog #28: not satırlarını görev listesine çevir
        dialogView.findViewById<android.widget.Button>(R.id.btSatirGorev)?.setOnClickListener {
            satirlariGoreveCevir(contentInput.text?.toString() ?: "")
        }

        // v10.33 · Katalog #39: tek dokunuşla tarih-saat damgası (imleç konumuna)
        dialogView.findViewById<android.widget.Button>(R.id.btDamga)?.setOnClickListener {
            val damga = java.text.SimpleDateFormat("d MMMM yyyy, HH:mm", java.util.Locale("tr", "TR"))
                .format(java.util.Date())
            val metin = contentInput.text
            if (metin != null) {
                val pos = contentInput.selectionStart.coerceIn(0, metin.length)
                metin.insert(pos, damga)
            } else {
                contentInput.setText(damga)
            }
            sayacYaz()
        }

        // v10.38 · Katalog #25: nota hatırlatıcı (yalnız kayıtlı nota kurulur)
        val btHatirlat = dialogView.findViewById<android.widget.Button>(R.id.btHatirlatic)
        fun hatirlatButonuTazele() {
            val z = existing?.let { NotHatirlatici.zaman(requireContext(), it.id) } ?: 0L
            btHatirlat?.text = if (z > 0L) {
                val fmt = java.text.SimpleDateFormat("d MMM HH:mm", java.util.Locale("tr"))
                getString(R.string.w38_hatirlat_var, fmt.format(java.util.Date(z)))
            } else {
                getString(R.string.w38_hatirlat)
            }
        }
        hatirlatButonuTazele()
        btHatirlat?.setOnClickListener {
            if (existing == null) {
                android.widget.Toast.makeText(
                    requireContext(), R.string.w38_hatirlat_once, android.widget.Toast.LENGTH_SHORT
                ).show()
            } else {
                notHatirlaticiDiyalog(existing) { hatirlatButonuTazele() }
            }
        }

        btnPick.setOnClickListener { pickImage.launch("image/*") }
        dialogRemoveBtn?.setOnClickListener {
            pendingImage = ""
            refreshDialogImagePreview()
        }

        val titleRes = if (existing == null) R.string.new_note else R.string.edit_note

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(titleRes)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val title = titleInput.text.toString().trim()
                val content = contentInput.text.toString().trim()
                // v10.33 · Katalog #38: başlık boşsa ilk dolu satırdan öneri (işaretler soyulmuş)
                val sonBaslik = title.ifEmpty { NotOneri.baslik(content) }

                if (title.isEmpty() && content.isEmpty() && pendingImage.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.empty_note_warning, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // v10.32: kayıt her zaman TAM liste üstünde (renk filtresi güvenliği)
                val kayitCtx = requireContext()
                val tam = Store.loadNotes(kayitCtx)
                if (existing == null) {
                    tam.add(
                        0,
                        Store.Note(
                            id = System.currentTimeMillis(),
                            title = sonBaslik,
                            content = content,
                            createdAt = System.currentTimeMillis(),
                            image = pendingImage
                        )
                    )
                } else {
                    // Fotoğraf değiştiyse eski dosyayı temizle
                    if (existing.image.isNotEmpty() && existing.image != pendingImage) {
                        File(imagesDir(), existing.image).delete()
                    }
                    // v10.32 · Katalog #29: eski hâl kaydetmeden ÖNCE sürüm geçmişine
                    NotSurum.gecmiseIt(kayitCtx, existing)
                    tam.firstOrNull { it.id == existing.id }?.let { h ->
                        h.title = sonBaslik
                        h.content = content
                        h.image = pendingImage
                    }
                }
                Store.saveNotes(kayitCtx, tam)
                reload()
            }
            .setNegativeButton(R.string.cancel, null)
            .setOnDismissListener {
                dialogPreview = null
                dialogRemoveBtn = null
            }
            .show()
    }

    /** v10.31 · Katalog #24: **kalın** / # başlık işaretlerini Spannable'a çevirir. */
    private fun bicimli(metin: String): CharSequence {
        val p = NotBicim.cozumle(metin)
        if (p.all { it.tip == NotBicim.Tip.DUZ }) return metin // en hızlı yol
        val ssb = android.text.SpannableStringBuilder()
        for (parca in p) {
            val st = ssb.length
            ssb.append(parca.metin)
            val son = ssb.length
            when (parca.tip) {
                NotBicim.Tip.KALIN -> ssb.setSpan(
                    android.text.style.StyleSpan(android.graphics.Typeface.BOLD), st, son, 0
                )
                NotBicim.Tip.BASLIK -> {
                    ssb.setSpan(
                        android.text.style.StyleSpan(android.graphics.Typeface.BOLD), st, son, 0
                    )
                    ssb.setSpan(android.text.style.RelativeSizeSpan(1.12f), st, son, 0)
                }
                else -> {}
            }
        }
        return ssb
    }

    private fun confirmDelete(note: Store.Note) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_note_title)
            .setMessage(R.string.delete_note_message)
            .setPositiveButton(R.string.delete) { _, _ ->
                // v7.72: geri alinabilir silme.
                // Not: resim dosyasi hemen silinmiyor — geri alinirsa
                // kayip olmasin diye. Temizlik acilista yapiliyor.
                Store.deleteNoteUndoable(requireContext(), note.id)
                reload()
                geriAlSun(getString(R.string.ga_not_silindi))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** v7.72: Silme sonrasi "Geri al" seridi. */
    /**
     * v8.6 · Öneri 26 — Tek tip bildirim.
     *
     * Eskiden ham Snackbar kullanılıyordu ve **FAB'ın altında
     * kalıyordu** (v7.72'den beri "geri al" şeridi kısmen
     * görünmüyordu). `Bildir` alt menüye tutturuyor ve temaya
     * uygun renklendiriyor.
     */
    private fun geriAlSun(mesaj: String) {
        Bildir.eylemli(view, mesaj, getString(R.string.ga_geri_al)) {
            if (Store.geriAl()) {
                reload()
                Bildir.basari(view, getString(R.string.ga_geri_alindi))
            }
        }
    }

    inner class NotesAdapter(
        private val items: List<Store.Note>,
        private val onClick: (Store.Note) -> Unit,
        private val onDelete: (Store.Note) -> Unit,
        // v10.35 · Katalog #34: şişirilecek satır düzeni (kart/kompakt)
        private val duzenKaynak: Int = R.layout.item_note,
        // v10.27 (öneri #22): uzun basış geri çağrısı (varsayılan: yok)
        private val onLong: (Store.Note) -> Unit = {}
    ) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

        private val dateFormat = SimpleDateFormat("d MMMM yyyy, HH:mm", Locale("tr", "TR"))

        inner class NoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.noteTitle)
            val preview: TextView = view.findViewById(R.id.notePreview)
            val photoBadge: TextView = view.findViewById(R.id.notePhotoBadge)
            /** v10.32 · Katalog #23: renk etiketi noktası. */
            val renkNokta: View = view.findViewById(R.id.noteRenkNokta)
            val date: TextView = view.findViewById(R.id.noteDate)
            val delete: ImageButton = view.findViewById(R.id.deleteButton)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(duzenKaynak, parent, false)
            return NoteViewHolder(view)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
            val note = items[position]
            // v10.27: sabit not başlıkta 📌 rozeti taşır
            val sabit = NotSabitle.sabitMi(holder.itemView.context, note.id)
            // v10.34 · Katalog #26: kilitli not maskele — içerik asla sızmasın
            val kilitli = NotKilit.kilitliMi(holder.itemView.context, note.id)
            val baslikHam = if (kilitli) {
                getString(R.string.w34_kilitli_baslik)
            } else {
                note.title.ifEmpty { getString(R.string.untitled) }
            }
            holder.title.text = (if (sabit) "📌 " else "") +
                    (if (kilitli) baslikHam else bicimli(baslikHam))

            if (kilitli || note.content.isEmpty()) {
                holder.preview.visibility = View.GONE
            } else {
                holder.preview.visibility = View.VISIBLE
                holder.preview.text = bicimli(note.content)
            }

            holder.photoBadge.visibility =
                if (note.image.isNotEmpty()) View.VISIBLE else View.GONE

            // v10.32 · Katalog #23: renk noktası (renksizse gizli)
            run {
                val ton = NotRenk.ton(holder.itemView.context, note.id)
                if (ton == null) {
                    holder.renkNokta.visibility = View.GONE
                } else {
                    holder.renkNokta.visibility = View.VISIBLE
                    val gd = android.graphics.drawable.GradientDrawable()
                    gd.shape = android.graphics.drawable.GradientDrawable.OVAL
                    gd.setColor(NotRenk.tonRenk(ton))
                    holder.renkNokta.background = gd
                }
            }

            // v10.31 · Katalog #36: okuma süresi rozeti (içerik olan notlarda)
            run {
                val dk = NotBicim.okumaDk(note.content)
                holder.date.text = if (dk > 0) {
                    dateFormat.format(Date(note.createdAt)) +
                            " · " + getString(R.string.w31_okuma, dk)
                } else {
                    dateFormat.format(Date(note.createdAt))
                }
            }
            holder.itemView.setOnClickListener { onClick(note) }
            holder.itemView.setOnLongClickListener { onLong(note); true }
            holder.delete.setOnClickListener { onDelete(note) }
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
