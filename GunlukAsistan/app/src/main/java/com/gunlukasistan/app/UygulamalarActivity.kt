package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * v11.13 — Telefondaki diğer uygulamalara erişim ekranı.
 *
 * Kullanıcı isteği: "Uygulama telefondaki diğer uygulamalara erişim sağlasın."
 *
 *  · Yüklü uygulamaları listeler (arama destekli, kategori rozetli).
 *  · Bir uygulamaya dokununca onu başlatır.
 *  · "Galeriden Al" / "Dosyadan Al" — sistem seçicisiyle fotoğraf/PDF gibi
 *    içeriği uygulamaya (not olarak) alır.
 *
 * Motor (`UygulamaMotoru`) saf ve testli; Android'e bağımlı liste/başlatma
 * burada.
 */
class UygulamalarActivity : AppCompatActivity() {

    private lateinit var adapter: UygulamaAdapter
    private val tumUygulamalar = mutableListOf<UygulamaMotoru.Uygulama>()
    private val simgeOnbellegi = mutableMapOf<String, Drawable?>()

    private val galeriAl =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) icerigiNotaKaydet(uri, "galeri")
        }
    private val dosyaAl =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) icerigiNotaKaydet(uri, "dosya")
        }

    companion object {
        fun ac(context: Context) {
            context.startActivity(Intent(context, UygulamalarActivity::class.java))
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(GorunumAyar.yaziOlcegiUygula(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.styleFor(this))
        ThemeManager.applyAccent(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_uygulamalar)

        findViewById<ImageButton>(R.id.btnGeri).setOnClickListener { finish() }

        adapter = UygulamaAdapter { u ->
            baslat(u)
        }
        val list = findViewById<RecyclerView>(R.id.appList)
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        findViewById<EditText>(R.id.searchApps).let { et ->
            et.setOnClickListener { }
            et.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    listeGoster(s?.toString().orEmpty())
                }
            })
        }

        findViewById<View>(R.id.btnGaleridenAl).setOnClickListener {
            galeriAl.launch("image/*")
        }
        findViewById<View>(R.id.btnDosyadanAl).setOnClickListener {
            dosyaAl.launch("*/*")
        }

        yukluUygulamalariOku()
    }

    override fun onResume() {
        super.onResume()
        GlassmorphismTemaMotoru.sekmeleriVeKartlariStille(window.decorView, this)
    }

    private fun yukluUygulamalariOku() {
        val pm = packageManager
        tumUygulamalar.clear()
        runCatching {
            val niyet = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            pm.queryIntentActivities(niyet, 0).forEach { ri ->
                val paket = ri.activityInfo.packageName
                val ad = ri.loadLabel(pm)?.toString() ?: paket
                tumUygulamalar.add(UygulamaMotoru.Uygulama(paket, ad))
                // simge onbelleği
                runCatching { simgeOnbellegi[paket] = ri.loadIcon(pm) }
            }
            // alfabetik sırala
            tumUygulamalar.sortBy { it.ad.lowercase() }
        }
        listeGoster("")
    }

    private fun listeGoster(arama: String) {
        val filtrelenmis = UygulamaMotoru.filtrle(tumUygulamalar, arama)
        adapter.yenile(filtrelenmis)
    }

    private fun baslat(u: UygulamaMotoru.Uygulama) {
        runCatching {
            val niyet = packageManager.getLaunchIntentForPackage(u.paket)
            if (niyet == null) {
                Toast.makeText(this, getString(R.string.uygulamalar_acilamadi, u.ad), Toast.LENGTH_SHORT).show()
                return
            }
            startActivity(niyet)
        }.onFailure {
            Toast.makeText(this, getString(R.string.uygulamalar_acilamadi, u.ad), Toast.LENGTH_SHORT).show()
        }
    }

    private fun icerigiNotaKaydet(uri: Uri, tur: String) {
        runCatching {
            val ad = try {
                contentResolver.getDisplayName(uri) ?: "İçerik"
            } catch (_: Exception) { "İçerik" }
            val metin = "📎 [$tur seçildi] $ad\n$uri"
            Store.addNote(this, metin.take(1000))
            Toast.makeText(this, getString(R.string.uygulamalar_nota_eklendi, ad), Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, R.string.uygulamalar_icerik_okunamadi, Toast.LENGTH_SHORT).show()
        }
    }

    private fun android.content.ContentResolver.getDisplayName(uri: Uri): String? {
        var ad: String? = null
        runCatching {
            query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) ad = c.getString(0)
            }
        }
        return ad
    }

    inner class UygulamaAdapter(
        private val tikla: (UygulamaMotoru.Uygulama) -> Unit
    ) : RecyclerView.Adapter<UygulamaAdapter.VH>() {

        private val gorunur = mutableListOf<UygulamaMotoru.Uygulama>()

        fun yenile(liste: List<UygulamaMotoru.Uygulama>) {
            gorunur.clear()
            gorunur.addAll(liste)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_uygulama, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(h: VH, position: Int) {
            val u = gorunur[position]
            h.ad.text = u.ad
            h.paket.text = u.paket
            h.kategori.text = UygulamaMotoru.kategori(u.paket)
            val ikon = simgeOnbellegi[u.paket]
            if (ikon != null) h.ikon.setImageDrawable(ikon) else h.ikon.setImageResource(android.R.drawable.sym_def_app_icon)
            h.kok.setOnClickListener { tikla(u) }
        }

        override fun getItemCount(): Int = gorunur.size

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val kok: View = v.findViewById(R.id.uygKok)
            val ikon: ImageView = v.findViewById(R.id.uygIkon)
            val ad: TextView = v.findViewById(R.id.uygAd)
            val paket: TextView = v.findViewById(R.id.uygPaket)
            val kategori: TextView = v.findViewById(R.id.uygKategori)
        }
    }
}
