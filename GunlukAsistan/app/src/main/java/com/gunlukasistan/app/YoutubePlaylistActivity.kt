package com.gunlukasistan.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * v11.03 — YouTube Çevrimdışı Oynatma Listesi Sıralayıcı ve Video Kitaplığı Ekranı.
 *
 * Kullanıcının "Çevrimdışı YouTube Oynatma Listeleri & Video Sıralayıcı sekmesini temaya uygun yap ve
 * videolarin aciklama yazilarini kucult ve kaydirilabilir sekilde yap. Videoyu silmek için sola tasimak icin
 * saga kaydirma hareketini kullandirma ekle. Videoyu tasirken basili tut ve sekmeler arasi gecis yapabileyim.
 * Daha profesyonel bir liste olsun." talimatı doğrultusunda:
 *
 *  • Tüm ekran Günlük Asistan tasarım dili ve temasıyla uyumlu hale getirildi.
 *  • Açıklama yazıları küçültüldü (@dimen/ga_yazi_mini = 11sp) ve yatay kaydırılabilir / marquee (isSelected = true) yapıldı.
 *  • RecyclerView + ItemTouchHelper ile sola kaydırınca (LEFT) videoyu silme, sağa kaydırınca (RIGHT) grubu
 *    değiştirme / taşıma desteği eklendi; özel görsel arka plan animasyonları (onChildDraw) tasarlandı.
 *  • Videoya basılı tutunca sekmeler arası hızlı geçiş ve taşıma modu (sekmelerArasiTasiVeGecisDiyalogu) açılır,
 *    istenen sekmeye anında geçiş yapılarak video o gruba dahil edilir.
 */
class YoutubePlaylistActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            context.startActivity(Intent(context, YoutubePlaylistActivity::class.java))
        }
    }

    private var seciliPlaylistId: String = ""
    private var seciliPlaylist: YoutubePlaylistMotoru.CevrimdisiPlaylist? = null
    private lateinit var adapter: VideoAdapter
    private lateinit var recyclerView: RecyclerView

    private val videoSeciciLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val secilenDosyalar = mutableListOf<Pair<String, String>>()

            if (data?.clipData != null) {
                val clipData = data.clipData!!
                for (i in 0 until clipData.itemCount) {
                    val uri = clipData.getItemAt(i).uri
                    val dosyaAdi = uriDosyaAdiniGetir(uri)
                    secilenDosyalar.add(Pair(uri.toString(), dosyaAdi))
                    tryPersistablePermission(uri)
                }
            } else if (data?.data != null) {
                val uri = data.data!!
                val dosyaAdi = uriDosyaAdiniGetir(uri)
                secilenDosyalar.add(Pair(uri.toString(), dosyaAdi))
                tryPersistablePermission(uri)
            }

            if (secilenDosyalar.isNotEmpty()) {
                val gruplar = YapayZekaYoutubeSiralamaMotoru.topluDosyalariGruplayipSirala(
                    this,
                    secilenDosyalar
                )
                Toast.makeText(
                    this,
                    "⚡ ${secilenDosyalar.size} video tarandı; ${gruplar.size} farklı YouTube oynatma listesine grup grup ayrıldı ve YouTube dışı olanlar ayrı listelendi!",
                    Toast.LENGTH_LONG
                ).show()
                guncellePlaylistCipleri()
                val ilk = gruplar.firstOrNull()?.id ?: ""
                playlistiYukle(ilk)
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(GorunumAyar.yaziOlcegiUygula(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.styleFor(this))
        ThemeManager.applyAccent(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_youtube_playlist)

        findViewById<ImageButton>(R.id.btnGeri).setOnClickListener { finish() }

        recyclerView = findViewById(R.id.listeYoutubeVideolar)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = VideoAdapter()
        recyclerView.adapter = adapter

        kurKaydirmaVeTasimaJestleri()

        findViewById<Button>(R.id.btnKlasorSecVeAiSirala).setOnClickListener {
            klasordenVideoSecimDiyalogu()
        }

        findViewById<Button>(R.id.btnGrubuSil)?.setOnClickListener {
            grubunTamaminiSilDiyalogu()
        }

        val hepsi = YoutubePlaylistMotoru.tumPlaylistleriGetir(this)
        seciliPlaylistId = hepsi.firstOrNull()?.id ?: ""

        guncellePlaylistCipleri()
        playlistiYukle(seciliPlaylistId)
    }

    override fun onResume() {
        super.onResume()
        GlassmorphismTemaMotoru.sekmeleriVeKartlariStille(window.decorView, this)
    }

    /**
     * v11.03: Sola kaydırma (Silme), sağa kaydırma (Grubu Değiştir / Taşı) ve
     * yukarı-aşağı sürükleme jestlerini yapılandırır.
     */
    private fun kurKaydirmaVeTasimaJestleri() {
        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.bindingAdapterPosition
                val toPos = target.bindingAdapterPosition
                if (fromPos == RecyclerView.NO_POSITION || toPos == RecyclerView.NO_POSITION) return false

                YoutubePlaylistMotoru.videolarinSirasiniDegistir(
                    this@YoutubePlaylistActivity,
                    seciliPlaylistId,
                    fromPos,
                    toPos
                )
                playlistiYukle(seciliPlaylistId)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return
                val video = adapter.getItem(pos) ?: return

                if (direction == ItemTouchHelper.LEFT) {
                    // Sola kaydırma -> Videoyu Sil / Kaldır
                    YoutubePlaylistMotoru.videoyuKaldir(
                        this@YoutubePlaylistActivity,
                        seciliPlaylistId,
                        video.sira
                    )
                    Toast.makeText(
                        this@YoutubePlaylistActivity,
                        "🗑️ '${video.youtubeBaslik}' sola kaydırıldı: Listeden silindi (#1, #2... güncellendi).",
                        Toast.LENGTH_SHORT
                    ).show()
                    playlistiYukle(seciliPlaylistId)
                } else if (direction == ItemTouchHelper.RIGHT) {
                    // Sağa kaydırma -> Videoyu Taşı / Grubu Değiştir
                    adapter.notifyItemChanged(pos) // Öğeyi görsel olarak yerine geri al
                    videoyuTasiVeyaKopyalaDiyalogu(video)
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val yaziOlcegi = resources.displayMetrics.density

                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val paint = Paint().apply { isAntiAlias = true }
                    val textPaint = Paint().apply {
                        color = 0xFFFFFFFF.toInt()
                        textSize = 14 * yaziOlcegi
                        typeface = Typeface.DEFAULT_BOLD
                        isAntiAlias = true
                    }

                    if (dX < 0) {
                        // Sola Kaydırma -> Kırmızı Silme Arka Planı
                        paint.color = 0xFFD32F2F.toInt() // Kırmızı (Hata rengi)
                        c.drawRect(
                            itemView.right.toFloat() + dX,
                            itemView.top.toFloat(),
                            itemView.right.toFloat(),
                            itemView.bottom.toFloat(),
                            paint
                        )
                        val text = "🗑️ Sil / Kaldır"
                        val textWidth = textPaint.measureText(text)
                        val x = itemView.right - textWidth - (24 * yaziOlcegi)
                        val y = itemView.top + (itemView.height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
                        if (itemView.right + dX < x + textWidth + (24 * yaziOlcegi)) {
                            c.drawText(text, x, y, textPaint)
                        }
                    } else if (dX > 0) {
                        // Sağa Kaydırma -> Mor / Ana Tema Taşıma Arka Planı
                        paint.color = 0xFF6200EE.toInt() // Mor (Ana tema rengi)
                        c.drawRect(
                            itemView.left.toFloat(),
                            itemView.top.toFloat(),
                            itemView.left.toFloat() + dX,
                            itemView.bottom.toFloat(),
                            paint
                        )
                        val text = "🔀 Grubu Değiştir / Taşı →"
                        val x = itemView.left + (20 * yaziOlcegi)
                        val y = itemView.top + (itemView.height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
                        if (itemView.left + dX > x + textPaint.measureText(text)) {
                            c.drawText(text, x, y, textPaint)
                        }
                    }
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        ItemTouchHelper(callback).attachToRecyclerView(recyclerView)
    }

    private fun tryPersistablePermission(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
            // İzin zaten alınmış veya desteklenmeyen URI türü
        }
    }

    private fun uriDosyaAdiniGetir(uri: Uri): String {
        var ad = "video_${System.currentTimeMillis()}.mp4"
        try {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        ad = it.getString(idx) ?: ad
                    }
                }
            }
        } catch (_: Exception) {
            ad = uri.lastPathSegment ?: ad
        }
        return ad
    }

    private fun klasordenVideoSecimDiyalogu() {
        val secenekler = arrayOf(
            "📁 Telefonumdan Toplu Video Seç (Yapay Zekâ ile Oynatma Listesi Grubuna Ayır)",
            "🧪 Örnek Toplu Senaryo ile Anında Deneyimle (Mat, Tarih, Türkçe & YouTube Dışı)"
        )

        AlertDialog.Builder(this)
            .setTitle("📁 Toplu Video Seçimi & Yapay Zekâ Gruplayıcı")
            .setItems(secenekler) { _, which ->
                if (which == 0) {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "video/*"
                        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    }
                    try {
                        videoSeciciLauncher.launch(intent)
                    } catch (e: Exception) {
                        Toast.makeText(
                            this,
                            "❌ Dosya seçici açılamadı: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    val ornekDosyalar = YapayZekaYoutubeSiralamaMotoru.ornekTopluDosyaSenaryosuGetir()
                    val gruplar = YapayZekaYoutubeSiralamaMotoru.topluDosyalariGruplayipSirala(
                        this,
                        ornekDosyalar
                    )
                    Toast.makeText(
                        this,
                        "⚡ 11 adet karışık video incelendi; ${gruplar.size} farklı oynatma listesine (Matematik, Tarih, Türkçe vb. + YouTube Dışı Özel Videolar) grup grup ayrıldı!",
                        Toast.LENGTH_LONG
                    ).show()
                    guncellePlaylistCipleri()
                    playlistiYukle(gruplar.first().id)
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun grubunTamaminiSilDiyalogu() {
        val p = seciliPlaylist ?: return
        AlertDialog.Builder(this)
            .setTitle("🗑️ Grubun / Listenin Tamamını Sil")
            .setMessage("Tek tek silmek yerine '${p.baslik}' adlı oynatma listesi grubunu tüm videolarıyla birlikte tamamen kaldırmak istiyor musunuz?")
            .setPositiveButton("Evet, Tüm Grubu Sil") { _, _ ->
                YoutubePlaylistMotoru.playlistSil(this, p.id)
                Toast.makeText(
                    this,
                    "🗑️ '${p.baslik}' grubu tüm videolarıyla birlikte silindi!",
                    Toast.LENGTH_LONG
                ).show()
                val hepsi = YoutubePlaylistMotoru.tumPlaylistleriGetir(this)
                val ilk = hepsi.firstOrNull()?.id ?: ""
                guncellePlaylistCipleri()
                playlistiYukle(ilk)
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun guncellePlaylistCipleri() {
        val kap = findViewById<LinearLayout>(R.id.layoutPlaylistCipleri) ?: return
        kap.removeAllViews()

        val listeler = YoutubePlaylistMotoru.tumPlaylistleriGetir(this)
        val yogunluk = resources.displayMetrics.density

        listeler.forEach { pl ->
            val cip = TextView(this).apply {
                text = pl.baslik
                textSize = 13f
                gravity = Gravity.CENTER
                val padH = (14 * yogunluk).toInt()
                val padV = (8 * yogunluk).toInt()
                setPadding(padH, padV, padH, padV)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = (8 * yogunluk).toInt() }
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    playlistiYukle(pl.id)
                    guncelleCipStilleri()
                }
            }
            kap.addView(cip)
        }
        guncelleCipStilleri()
    }

    private fun guncelleCipStilleri() {
        val kap = findViewById<LinearLayout>(R.id.layoutPlaylistCipleri) ?: return
        val yogunluk = resources.displayMetrics.density

        val listeler = YoutubePlaylistMotoru.tumPlaylistleriGetir(this)

        for (i in 0 until kap.childCount) {
            val cip = kap.getChildAt(i) as? TextView ?: continue
            val seciliMi = (listeler.getOrNull(i)?.id == seciliPlaylistId)
            cip.background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 16 * yogunluk
                if (seciliMi) {
                    setColor(com.google.android.material.color.MaterialColors.getColor(cip, com.google.android.material.R.attr.colorPrimary, 0xFF6200EE.toInt()))
                } else {
                    setColor(com.google.android.material.color.MaterialColors.getColor(cip, com.google.android.material.R.attr.colorSurfaceVariant, 0xFFE0E0E0.toInt()))
                }
            }
            cip.setTextColor(
                if (seciliMi) ContextCompat.getColor(this, android.R.color.white)
                else com.google.android.material.color.MaterialColors.getColor(cip, com.google.android.material.R.attr.colorOnSurface, 0xFF222222.toInt())
            )
            cip.setTypeface(null, if (seciliMi) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    private fun playlistiYukle(playlistId: String) {
        val hepsi = YoutubePlaylistMotoru.tumPlaylistleriGetir(this)
        seciliPlaylist = hepsi.find { it.id == playlistId } ?: hepsi.firstOrNull()
        seciliPlaylistId = seciliPlaylist?.id ?: ""

        val layoutBos = findViewById<View>(R.id.layoutBosKitaplik)
        val layoutKunye = findViewById<View>(R.id.layoutPlaylistKunye)
        val layoutJest = findViewById<View>(R.id.layoutJestBilgi)

        if (seciliPlaylist == null) {
            layoutBos.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            layoutKunye.visibility = View.GONE
            layoutJest?.visibility = View.GONE
        } else {
            layoutBos.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            layoutKunye.visibility = View.VISIBLE
            layoutJest?.visibility = View.VISIBLE

            findViewById<TextView>(R.id.txtPlaylistBaslik).text = seciliPlaylist!!.baslik
            findViewById<TextView>(R.id.txtPlaylistDurum).text =
                "${seciliPlaylist!!.durumOzetMetni} — Çevrimdışı Oynatmaya Hazır"
        }
        adapter.listeGuncelle(seciliPlaylist?.videolar ?: emptyList())
    }

    /**
     * v11.03: Videoya basılı tutunca sekmeler arası hızlı geçiş ve taşıma diyaloğu açılır.
     * Kullanıcı sekmeyi seçince video anında oraya taşınır ve ekran o sekmeye odaklanır.
     */
    private fun sekmelerArasiTasiVeGecisDiyalogu(video: YoutubePlaylistMotoru.PlaylistVideo) {
        val tumListeler = YoutubePlaylistMotoru.tumPlaylistleriGetir(this)
        val digerListeler = tumListeler.filter { it.id != seciliPlaylistId }

        val secenekMetinleri = mutableListOf<String>()
        secenekMetinleri.add("➕ Yeni Bir YouTube Kampı / Sekmesi Oluştur, Taşı ve Geçiş Yap")
        digerListeler.forEach { pl ->
            secenekMetinleri.add("⚡ '${pl.baslik}' Sekmesine TAŞI ve O SEKMEYE GEÇİŞ YAP")
            secenekMetinleri.add("➕ '${pl.baslik}' Sekmesine KOPYALA / EKLE")
        }

        AlertDialog.Builder(this)
            .setTitle("⚡ Sekmeler Arası Hızlı Taşıma & Geçiş Modu")
            .setMessage("👆 '${video.youtubeBaslik}' videosunu hangi kampa / sekmeye taşımak ve geçiş yapmak istersiniz?")
            .setItems(secenekMetinleri.toTypedArray()) { _, which ->
                if (which == 0) {
                    yeniGrupOlusturVeTasiDiyalogu(video)
                } else {
                    val idx = (which - 1) / 2
                    val islemTipi = (which - 1) % 2 // 0: Taşı ve Geçiş Yap, 1: Kopyala
                    val hedef = digerListeler[idx]

                    if (islemTipi == 0) {
                        val (ok, msg) = YoutubePlaylistMotoru.videoyuSekmelerArasiTasiVeGecisYap(
                            this,
                            seciliPlaylistId,
                            hedef.id,
                            video.sira
                        )
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                        if (ok) {
                            // Hedef sekmeye geçiş yap!
                            guncellePlaylistCipleri()
                            playlistiYukle(hedef.id)
                        }
                    } else {
                        YoutubePlaylistMotoru.videoyuBaskaListeyeKopyala(
                            this,
                            hedef.id,
                            video
                        )
                        Toast.makeText(
                            this,
                            "➕ '${video.youtubeBaslik}' videonuz '${hedef.baslik}' grubuna kopyalandı!",
                            Toast.LENGTH_LONG
                        ).show()
                        guncellePlaylistCipleri()
                        playlistiYukle(seciliPlaylistId)
                    }
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun videoyuTasiVeyaKopyalaDiyalogu(video: YoutubePlaylistMotoru.PlaylistVideo) {
        val tumListeler = YoutubePlaylistMotoru.tumPlaylistleriGetir(this)
        val digerListeler = tumListeler.filter { it.id != seciliPlaylistId }

        val secenekMetinleri = mutableListOf<String>()
        secenekMetinleri.add("➕ Yeni Bir YouTube Kampı / Oynatma Listesi Oluştur ve Oraya Taşı")
        digerListeler.forEach { pl ->
            secenekMetinleri.add("🔀 '${pl.baslik}' Grubuna TAŞI (Buradan kaldır)")
            secenekMetinleri.add("➕ '${pl.baslik}' Grubuna KOPYALA / EKLE")
        }

        AlertDialog.Builder(this)
            .setTitle("🔀 Grubu Değiştir / Videoyu Taşı")
            .setItems(secenekMetinleri.toTypedArray()) { _, which ->
                if (which == 0) {
                    yeniGrupOlusturVeTasiDiyalogu(video)
                } else {
                    val idx = (which - 1) / 2
                    val islemTipi = (which - 1) % 2 // 0: Taşı, 1: Kopyala
                    val hedef = digerListeler[idx]

                    if (islemTipi == 0) {
                        YoutubePlaylistMotoru.videoyuBaskaListeyeTasi(
                            this,
                            seciliPlaylistId,
                            hedef.id,
                            video.sira
                        )
                        Toast.makeText(
                            this,
                            "🔀 '${video.youtubeBaslik}' videonuz '${hedef.baslik}' grubuna taşındı ve sıra numaraları güncellendi!",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        YoutubePlaylistMotoru.videoyuBaskaListeyeKopyala(
                            this,
                            hedef.id,
                            video
                        )
                        Toast.makeText(
                            this,
                            "➕ '${video.youtubeBaslik}' videonuz '${hedef.baslik}' grubuna kopyalandı!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    guncellePlaylistCipleri()
                    playlistiYukle(seciliPlaylistId)
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun yeniGrupOlusturVeTasiDiyalogu(video: YoutubePlaylistMotoru.PlaylistVideo) {
        val edt = EditText(this).apply {
            hint = "Yeni Kamp / Oynatma Listesi Adı (Örn: KPSS Coğrafya Kampı)"
        }
        val pad = (18 * resources.displayMetrics.density).toInt()
        val cont = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            addView(edt)
        }
        AlertDialog.Builder(this)
            .setTitle("➕ Yeni Kamp Grubu Oluştur")
            .setView(cont)
            .setPositiveButton("Oluştur ve Videoyu Taşı") { _, _ ->
                val ad = edt.text.toString().trim()
                if (ad.isNotEmpty()) {
                    val yeniPl = YoutubePlaylistMotoru.klasordenPlaylistOlustur(
                        this,
                        ad,
                        listOf(Pair(video.yerelDosyaUri ?: "", video.yerelDosyaAdi ?: video.youtubeBaslik))
                    )
                    YoutubePlaylistMotoru.videoyuKaldir(this, seciliPlaylistId, video.sira)
                    Toast.makeText(
                        this,
                        "✅ Yeni '$ad' grubu oluşturuldu ve video oraya taşındı!",
                        Toast.LENGTH_LONG
                    ).show()
                    guncellePlaylistCipleri()
                    playlistiYukle(yeniPl.id)
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    /**
     * v11.03: RecyclerView için modernize edilmiş Video Adapter.
     */
    private inner class VideoAdapter : RecyclerView.Adapter<VideoAdapter.ViewHolder>() {

        private val videolar = mutableListOf<YoutubePlaylistMotoru.PlaylistVideo>()

        fun listeGuncelle(yeniListe: List<YoutubePlaylistMotoru.PlaylistVideo>) {
            videolar.clear()
            videolar.addAll(yeniListe)
            notifyDataSetChanged()
        }

        fun getItem(pos: Int): YoutubePlaylistMotoru.PlaylistVideo? = videolar.getOrNull(pos)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_youtube_video, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount(): Int = videolar.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val video = videolar[position]

            holder.txtVideoSira.text = video.siraMetni
            holder.txtVideoSure.text = video.sureMetni

            // v11.03: Başlık ve açıklama yazıları kaydırılabilir (marquee isSelected = true)
            holder.txtYoutubeBaslik.text = video.youtubeBaslik
            holder.txtYoutubeBaslik.isSelected = true

            holder.txtVideoAciklama.text = video.aciklama
            holder.txtVideoAciklama.isSelected = true

            if (video.eslesti) {
                holder.txtYerelDosyaDurum.text = "✅ Cihazda Hazır: ${video.yerelDosyaAdi} (Telefon Videosu)"
                holder.txtYerelDosyaDurum.setTextColor(
                    com.google.android.material.color.MaterialColors.getColor(
                        holder.txtYerelDosyaDurum,
                        com.google.android.material.R.attr.colorPrimary,
                        0xFF6200EE.toInt()
                    )
                )
            } else {
                holder.txtYerelDosyaDurum.text = "⚠️ Yerel Dosya Bekliyor (Cihazdaki indirilmiş videodan eşleştir)"
                holder.txtYerelDosyaDurum.setTextColor(
                    com.google.android.material.color.MaterialColors.getColor(
                        holder.txtYerelDosyaDurum,
                        com.google.android.material.R.attr.colorOnSurfaceVariant,
                        0xFF666666.toInt()
                    )
                )
            }

            holder.btnVideoyuOynat.setOnClickListener {
                val (ok, msg) = YoutubePlaylistMotoru.videoyuCihazdanOynat(this@YoutubePlaylistActivity, video)
                Toast.makeText(this@YoutubePlaylistActivity, msg, Toast.LENGTH_LONG).show()
            }

            holder.btnVideoTasi.setOnClickListener {
                videoyuTasiVeyaKopyalaDiyalogu(video)
            }

            holder.btnVideoSil.setOnClickListener {
                YoutubePlaylistMotoru.videoyuKaldir(
                    this@YoutubePlaylistActivity,
                    seciliPlaylist!!.id,
                    video.sira
                )
                Toast.makeText(
                    this@YoutubePlaylistActivity,
                    "🗑️ '${video.youtubeBaslik}' listeden kaldırıldı ve sıralama (#1, #2...) güncellendi!",
                    Toast.LENGTH_SHORT
                ).show()
                playlistiYukle(seciliPlaylist!!.id)
            }

            // v11.03: Satıra uzun basınca "Sekmeler Arası Hızlı Geçiş & Taşıma Modu" çalışır
            holder.itemView.setOnLongClickListener {
                sekmelerArasiTasiVeGecisDiyalogu(video)
                true
            }
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val txtVideoSira: TextView = view.findViewById(R.id.txtVideoSira)
            val txtVideoSure: TextView = view.findViewById(R.id.txtVideoSure)
            val txtYoutubeBaslik: TextView = view.findViewById(R.id.txtYoutubeBaslik)
            val txtYerelDosyaDurum: TextView = view.findViewById(R.id.txtYerelDosyaDurum)
            val txtVideoAciklama: TextView = view.findViewById(R.id.txtVideoAciklama)
            val btnVideoyuOynat: Button = view.findViewById(R.id.btnVideoyuOynat)
            val btnVideoTasi: Button = view.findViewById(R.id.btnVideoTasi)
            val btnVideoSil: Button = view.findViewById(R.id.btnVideoSil)
        }
    }
}
