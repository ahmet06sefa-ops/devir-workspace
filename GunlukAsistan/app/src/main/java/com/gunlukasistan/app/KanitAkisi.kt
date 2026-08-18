package com.gunlukasistan.app

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

/**
 * v7.78 — Kanıt fotoğrafı çekildikten sonraki akış.
 *
 * ── Akış ──
 *   1. Fotoğraf önizlemesi + "Denetleniyor…" göstergesi
 *   2. Arka planda [KanitDenetci.denetle]
 *   3a. ONAY  → görev tamamlanır, kısa kutlama
 *   3b. RED   → gerekçe + ipucu, "Tekrar çek" / "İtiraz et"
 *   3c. AI yok → [Kanit.cevrimdisiKabul] ayarına göre karar
 *
 * ── Neden ayrı sınıf ──
 * Bu akış hem [TasksFragment]'ten hem de bildirimden ([KanitActivity])
 * tetikleniyor. Diyalog kodunu iki yere kopyalamamak için ortak yer.
 *
 * ── İş parçacığı ──
 * Ağ isteği [Performans.arkaPlan] ile yapılır; sonuç ana iş parçacığında
 * işlenir. Activity kapandıysa sessizce çıkılır (sızıntı olmasın).
 */
object KanitAkisi {

    private const val TAG = "KanitAkisi"

    /**
     * @param onaylandi kullanıcı görevi gerçekten tamamlayabildi mi
     * @param tekrarCek kullanıcı yeniden fotoğraf çekmek istedi
     */
    fun interface Sonuc {
        fun bitti(onaylandi: Boolean, tekrarCek: Boolean)
    }

    private val yogunluk: (Activity) -> Float = { it.resources.displayMetrics.density }

    /**
     * Çekilen fotoğrafı denetler ve kullanıcıya sonucu gösterir.
     *
     * @param gorev denetlenecek görev
     * @param uri çekilen/seçilen fotoğrafın adresi
     */
    fun denetle(
        activity: Activity,
        gorev: Store.Task,
        uri: Uri,
        sonuc: Sonuc
    ) {
        val d = yogunluk(activity)

        // ── Bekleme penceresi: önizleme + çubuk ───────────────────
        val kap = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((22 * d).toInt(), (22 * d).toInt(), (22 * d).toInt(), (14 * d).toInt())
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val onizleme = ImageView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (190 * d).toInt()
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            kucukResmiYukle(activity, uri)?.let { setImageBitmap(it) }
        }
        kap.addView(onizleme)

        val durum = TextView(activity).apply {
            text = activity.getString(R.string.kn_denetleniyor)
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, (16 * d).toInt(), 0, (10 * d).toInt())
        }
        kap.addView(durum)

        kap.addView(ProgressBar(activity).apply {
            isIndeterminate = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        })

        val bekleme = MaterialAlertDialogBuilder(activity)
            .setTitle(gorev.text)
            .setView(kap)
            .setCancelable(false)
            .create()
        bekleme.show()

        // ── AI hazır değilse ağa hiç çıkma ────────────────────────
        if (!AiSettings.isReady(activity)) {
            kapat(bekleme)
            cevrimdisiKarar(activity, gorev, uri, sonuc)
            return
        }

        // ── Arka planda denetim ───────────────────────────────────
        Performans.arkaPlan {
            val karar = KanitDenetci.denetle(activity, uri, gorev.text)
            Performans.anaIs {
                if (activity.isFinishing || activity.isDestroyed) return@anaIs
                kapat(bekleme)

                if (!karar.calisti) {
                    // Ağ/anahtar sorunu — kullanıcıyı cezalandırma
                    cevrimdisiKarar(activity, gorev, uri, sonuc, karar.hata)
                    return@anaIs
                }

                if (karar.onay) {
                    kaydet(activity, gorev, uri, Kanit.ONAYLI, karar.guven, karar.gerekce)
                    onayPenceresi(activity, karar.gerekce, karar.guven) {
                        sonuc.bitti(true, false)
                    }
                } else {
                    kaydet(activity, gorev, uri, Kanit.RED, karar.guven, karar.gerekce)
                    redPenceresi(activity, gorev, karar, sonuc)
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // KARAR PENCERELERİ
    // ═══════════════════════════════════════════════════════════════

    private fun onayPenceresi(
        activity: Activity,
        gerekce: String,
        guven: Int,
        kapandi: () -> Unit
    ) {
        MaterialAlertDialogBuilder(activity)
            .setTitle(activity.getString(R.string.kn_onaylandi))
            .setMessage(
                gerekce + "\n\n" +
                    activity.getString(R.string.kn_guven_yuzde, guven)
            )
            .setPositiveButton(R.string.ok) { _, _ -> kapandi() }
            .setOnDismissListener { kapandi() }
            .show()
    }

    private fun redPenceresi(
        activity: Activity,
        gorev: Store.Task,
        karar: KanitDenetci.Sonuc,
        sonuc: Sonuc
    ) {
        val mesaj = StringBuilder(karar.gerekce)
        if (karar.ipucu.isNotBlank()) {
            mesaj.append("\n\n").append(activity.getString(R.string.kn_ipucu, karar.ipucu))
        }
        mesaj.append("\n\n").append(activity.getString(R.string.kn_guven_yuzde, karar.guven))

        val engelli = Kanit.redEngeller(activity)
        if (engelli) {
            mesaj.append("\n\n").append(activity.getString(R.string.kn_red_engel))
        }

        MaterialAlertDialogBuilder(activity)
            .setTitle(activity.getString(R.string.kn_reddedildi))
            .setMessage(mesaj.toString())
            .setPositiveButton(R.string.kn_tekrar_cek) { _, _ -> sonuc.bitti(false, true) }
            .setNegativeButton(R.string.kn_itiraz) { _, _ ->
                itirazPenceresi(activity, gorev, sonuc)
            }
            .setNeutralButton(R.string.cancel) { _, _ ->
                // Red engellemiyorsa görev yine de tamamlanır
                sonuc.bitti(!engelli, false)
            }
            .setCancelable(false)
            .show()
    }

    /**
     * İtiraz — model yanılmış olabilir.
     *
     * Kullanıcı gerekçe yazarsa kayıt [Kanit.ITIRAZ] olur ve görev geçer.
     * Bu bilinçli bir kaçış kapısı: yapay zekâ %100 doğru değil, kullanıcıyı
     * yanlış bir kararla kilitlemek uygulamayı kullanılmaz yapardı.
     */
    private fun itirazPenceresi(activity: Activity, gorev: Store.Task, sonuc: Sonuc) {
        val d = yogunluk(activity)
        val giris = android.widget.EditText(activity).apply {
            hint = activity.getString(R.string.kn_itiraz_ipucu)
            setSingleLine(false)
            minLines = 2
        }
        val kap = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((22 * d).toInt(), (12 * d).toInt(), (22 * d).toInt(), 0)
            addView(giris)
        }

        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.kn_itiraz)
            .setMessage(R.string.kn_itiraz_aciklama)
            .setView(kap)
            .setPositiveButton(R.string.kn_itiraz_gonder) { _, _ ->
                val neden = giris.text?.toString()?.trim().orEmpty()
                val kayit = Kanit.bul(activity, gorev.id) ?: Kanit.Kayit(gorev.id)
                kayit.durum = Kanit.ITIRAZ
                kayit.gerekce = activity.getString(R.string.kn_itiraz_edildi) +
                    (if (neden.isBlank()) "" else ": $neden")
                Kanit.kaydet(activity, kayit)
                sonuc.bitti(true, false)
            }
            .setNegativeButton(R.string.cancel) { _, _ -> sonuc.bitti(false, false) }
            .show()
    }

    /**
     * Yapay zekâ kullanılamadığında ne olacak.
     *
     * Kullanıcı çevrimdışıyken görevini tamamlayamamalı değil — bu
     * uygulamayı kullanılmaz yapar. Varsayılan: fotoğraf çekildiyse kabul,
     * kayıt [Kanit.BEKLIYOR] olarak işaretlenir.
     */
    private fun cevrimdisiKarar(
        activity: Activity,
        gorev: Store.Task,
        uri: Uri,
        sonuc: Sonuc,
        hata: String = ""
    ) {
        kaydet(activity, gorev, uri, Kanit.BEKLIYOR, 0, hata)

        if (Kanit.cevrimdisiKabul(activity)) {
            MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.kn_kaydedildi)
                .setMessage(
                    activity.getString(R.string.kn_cevrimdisi_kabul) +
                        (if (hata.isBlank()) "" else "\n\n($hata)")
                )
                .setPositiveButton(R.string.ok) { _, _ -> sonuc.bitti(true, false) }
                .setOnDismissListener { sonuc.bitti(true, false) }
                .show()
        } else {
            MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.kn_denetlenemedi)
                .setMessage(
                    activity.getString(R.string.kn_cevrimdisi_red) +
                        (if (hata.isBlank()) "" else "\n\n($hata)")
                )
                .setPositiveButton(R.string.ok, null)
                .setOnDismissListener { sonuc.bitti(false, false) }
                .show()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // YARDIMCILAR
    // ═══════════════════════════════════════════════════════════════

    private fun kaydet(
        activity: Activity,
        gorev: Store.Task,
        uri: Uri,
        durum: Int,
        guven: Int,
        gerekce: String
    ) {
        val onceki = Kanit.bul(activity, gorev.id)
        val kayit = onceki ?: Kanit.Kayit(gorev.id)
        kayit.yol = uri.path?.let { yol ->
            // FileProvider uri'sinden gerçek dosya yolunu çıkar
            val dosya = uriDosyasi(activity, uri)
            dosya?.absolutePath ?: kayit.yol
        } ?: kayit.yol
        kayit.durum = durum
        kayit.guven = guven
        kayit.gerekce = gerekce
        kayit.zaman = System.currentTimeMillis()
        kayit.deneme = (onceki?.deneme ?: 0) + 1
        Kanit.kaydet(activity, kayit)
    }

    /** FileProvider adresinden dosyayı bulur (kendi klasörümüz olduğu için mümkün). */
    private fun uriDosyasi(activity: Activity, uri: Uri): File? {
        val ad = uri.lastPathSegment ?: return null
        val dosya = File(Kanit.klasor(activity), ad.substringAfterLast('/'))
        return if (dosya.exists()) dosya else null
    }

    /** Bellek dostu önizleme — tam çözünürlük gerekmiyor. */
    private fun kucukResmiYukle(activity: Activity, uri: Uri): android.graphics.Bitmap? = try {
        val olcu = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        activity.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, olcu)
        }
        var orn = 1
        while (olcu.outWidth / orn > 900) orn *= 2
        val secenek = BitmapFactory.Options().apply { inSampleSize = orn }
        activity.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, secenek)
        }
    } catch (e: Exception) {
        android.util.Log.w(TAG, "Önizleme yüklenemedi", e)
        null
    }

    private fun kapat(d: AlertDialog?) {
        runCatching { if (d?.isShowing == true) d.dismiss() }
    }
}
