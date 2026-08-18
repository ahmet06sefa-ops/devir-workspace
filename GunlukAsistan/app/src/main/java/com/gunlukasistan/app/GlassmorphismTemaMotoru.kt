package com.gunlukasistan.app

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView

/**
 * v10.76 — Evrensel Glassmorphism & Cyber-Zen 3D Cam Teması Motoru.
 *
 * Kullanıcının "Bana o kadar büyük bir değişiklik oner ki 1 tane öneri ama devasa değişiklik
 * gibi olsun ekstra birsey eklemeyecegim görünüm olarak devasa bir değişiklik olsun ve begenmezsem
 * eski haline cevirebileyim. Bütün sekmelerde ayarlarda heryerde o değişikliği hissedeyim"
 * talimatı doğrultusunda geliştirilen evrensel görünüm ve katman motoru:
 *
 *  1. Tüm sekmelerdeki (Ana Sayfa, Bugün, Konular, İlerleme, Plan) ve Ayarlar'daki kartları
 *     yarı saydam buzlu cam (Frosted Glass / Glassmorphism: alpha = 0.88f) ve parlayan ince
 *     neon kenarlıkla (StrokeWidth = 2, Elevation = 10f) kaplar.
 *  2. Ayarlar ekranından tek bir anahtarla (rowGlassmorphismToggle / swGlassmorphism) açılıp kapatılır.
 *  3. Kapatıldığında 1 saniyede hiçbir ayar kaybolmadan orijinal mat v2 görünüme döner.
 */
object GlassmorphismTemaMotoru {

    private const val PREF_NAME = "glassmorphism_tema_v1"
    private const val KEY_AKTIF = "glassmorphism_3d_cam_aktif"

    // ── TEMEL DURUM KONTROLÜ ──
    fun temaAktifMi(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_AKTIF, true)
    }

    fun setTemaAktif(context: Context, aktif: Boolean) {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sp.edit().putBoolean(KEY_AKTIF, aktif).apply()
    }

    // ── CAM & KLASİK TEMA GÖRSEL DEĞERLERİ ──
    fun kartYariSaydamlikAlpha(aktifMi: Boolean): Float {
        return if (aktifMi) 0.88f else 1.0f
    }

    fun kartDerinlikElevationDp(aktifMi: Boolean): Float {
        return if (aktifMi) 10.0f else 2.0f
    }

    fun kartKenarOlcegiDp(aktifMi: Boolean): Int {
        return if (aktifMi) 2 else 0
    }

    fun temaDurumMetniGetir(aktifMi: Boolean): Pair<String, String> {
        return if (aktifMi) {
            Pair(
                "💎 AÇIK: 'Glassmorphism & Cyber-Zen' 3D Cam Teması",
                "Bütün sekmelerde ve kartlarda yarı saydam buzlu cam, neon zümrüt kenarlık & 3D derinlik aktif"
            )
        } else {
            Pair(
                "⚪ KAPALI: Orijinal Mat v2 Minimalist Görünüm",
                "Klasik opak kart yüzeyleri ve standart tasarım aktif. Temayı açarak 3D cam görünüme geçebilirsiniz."
            )
        }
    }

    // ── TÜM SEKMELERİN & KARTLARIN EKRANDA STİLLENMESİ (RECURSIVE VIEW TRAVERSAL) ──
    fun sekmeleriVeKartlariStille(rootView: View?, context: Context) {
        if (rootView == null) return
        val aktif = temaAktifMi(context)
        val alphaVal = kartYariSaydamlikAlpha(aktif)
        val elevVal = kartDerinlikElevationDp(aktif)
        val strokeVal = kartKenarOlcegiDp(aktif)

        fun gezVeUygula(view: View) {
            val strokeColor = if (aktif) {
                com.google.android.material.color.MaterialColors.getColor(view, com.google.android.material.R.attr.colorPrimary, 0xFF4CAF50.toInt())
            } else {
                0 // şeffaf / çerçevesiz
            }
            if (view is MaterialCardView) {
                // Özel zemin renkli kartların rengini bozmadan saydamlık, derinlik ve kenarlık uygula
                view.alpha = alphaVal
                view.cardElevation = elevVal
                view.strokeWidth = strokeVal
                if (aktif) {
                    view.strokeColor = strokeColor
                }
            }
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    gezVeUygula(view.getChildAt(i))
                }
            }
        }
        gezVeUygula(rootView)
    }
}
