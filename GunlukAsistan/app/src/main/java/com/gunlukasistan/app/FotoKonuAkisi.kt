package com.gunlukasistan.app

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

/**
 * v7.19 — Fotoğraftan konu ekleme akışı.
 *
 * Adımlar:
 *   1. Kullanıcı fotoğraf seçer (galeri veya kamera)
 *   2. Görsel hazırlanır (döndürme, ölçekleme, kontrast)
 *   3. Yapay zekâ el yazısını okur
 *   4. **Kullanıcı sonucu düzeltir** — bu adım atlanamaz
 *   5. Konu ve maddeler kaydedilir
 *
 * Dördüncü adım tasarımın kalbi: yapay zekâ el yazısında hata yapabilir,
 * bu yüzden hiçbir şey onaysız kaydedilmez.
 */
object FotoKonuAkisi {

    private const val TAG = "FotoKonu"

    /** Okuma sonucu kaydedildiğinde çağrılır. */
    fun interface Dinleyici {
        fun kaydedildi(baslik: String, maddeler: List<String>)
    }

    /**
     * Görseli okur ve düzeltme ekranını açar.
     * Ağ çağrısı arka planda yapılır, arayüz kilitlenmez.
     */
    fun oku(
        activity: Activity,
        uri: Uri,
        dinleyici: Dinleyici
    ) {
        val ctx: Context = activity

        // Ön kontrol: yapay zekâ hazır mı
        if (!AiSettings.isOnlineMode(ctx)) {
            ayarUyarisi(activity, ctx.getString(R.string.ocr_need_ai_mode))
            return
        }
        if (AiSettings.getApiKey(ctx).isBlank()) {
            ayarUyarisi(activity, ctx.getString(R.string.ocr_need_key))
            return
        }

        val bekleme = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.ocr_reading_title)
            .setMessage(R.string.ocr_reading_body)
            .setCancelable(false)
            .show()

        Thread {
            val base64 = GorselHazirla.base64Uret(ctx, uri, netlestir = true)
            if (base64 == null) {
                activity.runOnUiThread {
                    kapat(bekleme)
                    Toast.makeText(ctx, R.string.ocr_err_image, Toast.LENGTH_LONG).show()
                }
                return@Thread
            }

            val (sonuc, konu) = AiClient.konuOku(ctx, base64)

            activity.runOnUiThread {
                kapat(bekleme)
                if (!sonuc.ok || konu == null) {
                    hataGoster(activity, sonuc.text, uri, dinleyici)
                    return@runOnUiThread
                }
                duzeltmeEkrani(activity, uri, konu, dinleyici)
            }
        }.start()
    }

    private fun kapat(d: AlertDialog?) {
        try {
            d?.dismiss()
        } catch (_: Exception) {
        }
    }

    private fun ayarUyarisi(activity: Activity, mesaj: String) {
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.ocr_title)
            .setMessage(mesaj)
            .setPositiveButton(R.string.ocr_open_settings) { _, _ ->
                (activity as? MainActivity)?.openSettings()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** Okuma başarısızsa: sebebi göster, tekrar deneme veya elle giriş sun. */
    private fun hataGoster(
        activity: Activity,
        mesaj: String,
        uri: Uri,
        dinleyici: Dinleyici
    ) {
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.ocr_err_title)
            .setMessage(mesaj + "\n\n" + activity.getString(R.string.ocr_err_tips))
            .setPositiveButton(R.string.ocr_retry) { _, _ -> oku(activity, uri, dinleyici) }
            .setNeutralButton(R.string.ocr_manual) { _, _ ->
                duzeltmeEkrani(
                    activity, uri,
                    AiClient.OkunanKonu("", emptyList()),
                    dinleyici
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Okunanı kullanıcıya gösterip düzelttiren ekran.
     * Her madde ayrı ayrı düzenlenebilir ve silinebilir.
     */
    private fun duzeltmeEkrani(
        activity: Activity,
        uri: Uri,
        konu: AiClient.OkunanKonu,
        dinleyici: Dinleyici
    ) {
        val gorunum = LayoutInflater.from(activity).inflate(R.layout.dialog_foto_konu, null)
        val baslikAlani = gorunum.findViewById<TextInputEditText>(R.id.fkBaslik)
        val maddeKabi = gorunum.findViewById<LinearLayout>(R.id.fkMaddeler)
        val bosYazi = gorunum.findViewById<TextView>(R.id.fkBos)
        val uyari = gorunum.findViewById<TextView>(R.id.fkUyari)
        val onizleme = gorunum.findViewById<ImageView>(R.id.fkOnizleme)

        // Önizleme
        try {
            GorselHazirla.onizleme(activity, uri)?.let { onizleme.setImageBitmap(it) }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Önizleme yüklenemedi", e)
            onizleme.visibility = View.GONE
        }

        baslikAlani.setText(konu.baslik)

        // Şüpheli okuma varsa uyarıyı vurgula
        val supheliVar = konu.baslik.contains("???") || konu.maddeler.any { it.contains("???") }
        uyari.text = activity.getString(
            if (supheliVar) R.string.ocr_check_hint_warn else R.string.ocr_check_hint
        )

        val satirlar = mutableListOf<View>()

        fun bosluguGuncelle() {
            bosYazi.visibility = if (satirlar.isEmpty()) View.VISIBLE else View.GONE
            // Sıra numaralarını tazele
            satirlar.forEachIndexed { i, satir ->
                satir.findViewById<TextView>(R.id.fmSira).text = "${i + 1}."
            }
        }

        fun satirEkle(metin: String) {
            val satir = LayoutInflater.from(activity)
                .inflate(R.layout.item_foto_madde, maddeKabi, false)
            satir.findViewById<EditText>(R.id.fmMetin).setText(metin)
            satir.findViewById<TextView>(R.id.fmSil).setOnClickListener {
                maddeKabi.removeView(satir)
                satirlar.remove(satir)
                bosluguGuncelle()
            }
            maddeKabi.addView(satir)
            satirlar.add(satir)
            bosluguGuncelle()
        }

        konu.maddeler.forEach { satirEkle(it) }
        bosluguGuncelle()

        gorunum.findViewById<TextView>(R.id.fkMaddeEkle).setOnClickListener {
            satirEkle("")
            satirlar.lastOrNull()?.findViewById<EditText>(R.id.fmMetin)?.requestFocus()
        }

        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.ocr_result_title)
            .setView(gorunum)
            .setPositiveButton(R.string.save) { _, _ ->
                val baslik = baslikAlani.text?.toString()?.trim().orEmpty()
                val maddeler = satirlar
                    .mapNotNull {
                        it.findViewById<EditText>(R.id.fmMetin).text?.toString()?.trim()
                    }
                    .filter { it.isNotBlank() }

                if (baslik.isBlank() && maddeler.isEmpty()) {
                    Toast.makeText(activity, R.string.ocr_empty_warn, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val sonBaslik = baslik.ifBlank {
                    activity.getString(R.string.ocr_default_title)
                }
                dinleyici.kaydedildi(sonBaslik, maddeler)
            }
            .setNeutralButton(R.string.ocr_retry) { _, _ -> oku(activity, uri, dinleyici) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
