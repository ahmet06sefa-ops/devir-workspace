package com.gunlukasistan.app

import android.content.Context

/**
 * v7.83 — Anlatım okuma tercihleri.
 *
 * ── Neden ayrı bir tema yönetimi ──
 * Uygulamanın genel teması ([ThemeManager]) tüm ekranları etkiliyor.
 * Uzun ders metni okurken ise farklı ihtiyaçlar var: gece okurken koyu
 * zemin, gündüz sepya, yaşlı gözde büyük punto. Bunu genel temaya bağlamak
 * "sırf ders okumak için tüm uygulamayı karartmak" demekti.
 *
 * Bu yüzden yalnızca [KonuAnlatimActivity] içinde geçerli, bağımsız bir
 * okuma modu tutuluyor — e-kitap uygulamalarındaki gibi.
 */
object OkumaAyar {

    private const val PREF = "okuma_ayar_v1"

    // ── Zemin modları ──
    const val ZEMIN_TEMA = 0     // uygulama temasını kullan
    const val ZEMIN_ACIK = 1     // beyaz
    const val ZEMIN_SEPYA = 2    // krem — uzun okumada göz yormaz
    const val ZEMIN_KOYU = 3     // koyu gri
    const val ZEMIN_SIYAH = 4    // tam siyah (OLED)

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ═══════════════════════════════════════════════════════════════
    // YAZI BOYUTU
    // ═══════════════════════════════════════════════════════════════

    /** Gövde metni punto — 12..26 arası. */
    fun yaziBoyutu(context: Context): Float =
        prefs(context).getFloat("boyut", 14.5f).coerceIn(12f, 26f)

    fun setYaziBoyutu(context: Context, sp: Float) {
        prefs(context).edit().putFloat("boyut", sp.coerceIn(12f, 26f)).apply()
    }

    fun buyut(context: Context): Float {
        val yeni = (yaziBoyutu(context) + 1.5f).coerceAtMost(26f)
        setYaziBoyutu(context, yeni)
        return yeni
    }

    fun kucult(context: Context): Float {
        val yeni = (yaziBoyutu(context) - 1.5f).coerceAtLeast(12f)
        setYaziBoyutu(context, yeni)
        return yeni
    }

    /** Başlık puntoları gövdeye orantılı büyüsün. */
    fun basligBoyutu(context: Context): Float = yaziBoyutu(context) + 1.5f
    fun anaBaslikBoyutu(context: Context): Float = yaziBoyutu(context) + 6.5f

    // ═══════════════════════════════════════════════════════════════
    // SATIR ARALIĞI
    // ═══════════════════════════════════════════════════════════════

    fun satirAraligi(context: Context): Float =
        prefs(context).getFloat("satir", 1.42f).coerceIn(1.0f, 2.2f)

    fun setSatirAraligi(context: Context, deger: Float) {
        prefs(context).edit().putFloat("satir", deger.coerceIn(1.0f, 2.2f)).apply()
    }

    // ═══════════════════════════════════════════════════════════════
    // ZEMİN
    // ═══════════════════════════════════════════════════════════════

    fun zemin(context: Context): Int =
        prefs(context).getInt("zemin", ZEMIN_TEMA).coerceIn(0, 4)

    fun setZemin(context: Context, mod: Int) {
        prefs(context).edit().putInt("zemin", mod.coerceIn(0, 4)).apply()
    }

    fun zeminAdi(context: Context, mod: Int): String = context.getString(
        when (mod) {
            ZEMIN_ACIK -> R.string.oa_zemin_acik
            ZEMIN_SEPYA -> R.string.oa_zemin_sepya
            ZEMIN_KOYU -> R.string.oa_zemin_koyu
            ZEMIN_SIYAH -> R.string.oa_zemin_siyah
            else -> R.string.oa_zemin_tema
        }
    )

    /** Zemin rengi. ZEMIN_TEMA'da null döner — çağıran tema rengini kullanır. */
    fun zeminRengi(mod: Int): Int? = when (mod) {
        ZEMIN_ACIK -> 0xFFFFFFFF.toInt()
        ZEMIN_SEPYA -> 0xFFF6EEDC.toInt()
        ZEMIN_KOYU -> 0xFF1B1B1F.toInt()
        ZEMIN_SIYAH -> 0xFF000000.toInt()
        else -> null
    }

    /** Zemine uygun metin rengi. */
    fun metinRengi(mod: Int): Int? = when (mod) {
        ZEMIN_ACIK -> 0xFF1A1A1A.toInt()
        ZEMIN_SEPYA -> 0xFF3B2F1E.toInt()
        ZEMIN_KOYU -> 0xFFE6E1E5.toInt()
        ZEMIN_SIYAH -> 0xFFD8D8D8.toInt()
        else -> null
    }

    /** İkincil (soluk) metin rengi. */
    fun soluk(mod: Int): Int? = metinRengi(mod)?.let { (it and 0x00FFFFFF) or 0xB0000000.toInt() }

    // ═══════════════════════════════════════════════════════════════
    // OKUMA MODU EK SEÇENEKLERİ
    // ═══════════════════════════════════════════════════════════════

    /** Ekran açık kalsın mı — okurken sürekli dokunmak zorunda kalma. */
    fun ekranAcikKalsin(context: Context): Boolean =
        prefs(context).getBoolean("ekran_acik", false)

    fun setEkranAcikKalsin(context: Context, acik: Boolean) {
        prefs(context).edit().putBoolean("ekran_acik", acik).apply()
    }

    /** İki yana yaslı metin — uzun paragraflarda daha derli toplu görünür. */
    fun yaslaMetin(context: Context): Boolean =
        prefs(context).getBoolean("yasla", false)

    fun setYaslaMetin(context: Context, yasla: Boolean) {
        prefs(context).edit().putBoolean("yasla", yasla).apply()
    }

    /** Kısa özet — ayar satırında gösterilir. */
    fun ozet(context: Context): String =
        "${yaziBoyutu(context).toInt()}sp · ${zeminAdi(context, zemin(context))}"
}
