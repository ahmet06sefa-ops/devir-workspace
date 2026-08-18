package com.gunlukasistan.app

import android.content.Context
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Calendar

/**
 * v10.14 · ULTRA-30 / E25 — Sabah AI planı.
 *
 * ── Tarama kanıtı ──
 * v10.9 sabah rutini bildirimlerle sınırlıydı (`gunuBaslat` →
 * `BildirimUretici.tumKontroller`); "uyandım" sonrası önceliklendirilmiş
 * gün taslağı üreten hiçbir şey yoktu.
 *
 * ── Akış ──
 * 1. [beklemeyeAl]: "uyandım" onayından sonra bayrak kalkar
 *    (taslak boşsa bayrak kalmaz — boş diyalog gösterilmez).
 * 2. [maybeGoster]: ana ekran ilk açıldığında bayrak varsa tek
 *    seferlik diyalog kurar: dünkü yarım işler + bugünün görevleri.
 * 3. AI hazırsa ([AiSettings.isReady]) 3 madde tek istekle doğal
 *    dile çevrilir; 8 sn'de dönmezse yerel taslak gösterilir
 *    (diyalog zaten kurulmuştur, metin sonra tazelenir).
 * 4. "Görevlere işle": yarım kalanlar bugüne taşınır ve alarmı
 *    kurulur — taslak gerçek görev listesine dökülmüş olur.
 *
 * Seçim mantığı [sec]'te framework'süzdür ve birim testlidir.
 */
object SabahPlani {

    private const val TAG = "SabahPlani"
    private const val PREF = "ge_sabah_plani_v1"

    private fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ---------------- Bayrak ----------------

    /** Uyandım kapısı çağırır: bugün için plan bekliyor. */
    fun beklemeyeAl(c: Context) {
        prefs(c).edit()
            .putBoolean("bekliyor", true)
            .putString("gun", UykuCerceve.gunKey(System.currentTimeMillis()))
            .putBoolean("gosterildi", false)
            .apply()
    }

    private fun bekliyorMu(c: Context): Boolean {
        val p = prefs(c)
        return p.getBoolean("bekliyor", false) &&
            !p.getBoolean("gosterildi", false) &&
            p.getString("gun", "") == UykuCerceve.gunKey(System.currentTimeMillis())
    }

    // ---------------- Saf seçim (birim testli) ----------------

    /** Taslağın tek maddesi. [gorevId] 0 ise serbest metin (özet satırı). */
    data class Madde(val metin: String, val gorevId: Long, val yarimMi: Boolean)

    /** Test motorunun gördüğü en küçük görev görünümü. */
    data class GorevOzet(val id: Long, val metin: String, val dueAt: Long, val done: Boolean)

    /**
     * 3 maddelik odak listesi üretir.
     *
     * Sıralama: önce yarım kalanlar (en eski ilk), sonra bugünlüler,
     * dolmazsa tarihsizler. Hiçbiri yoksa boş döner (diyalog açılmaz).
     *
     * @param bugunBaslangic bugün 00:00 (ms)
     * @param bugunSon bugün 23:59:59 (ms)
     */
    fun sec(gorevler: List<GorevOzet>, bugunBaslangic: Long, bugunSon: Long): List<Madde> {
        val bekleyen = gorevler.filter { !it.done }
        val yarim = bekleyen
            .filter { it.dueAt in 1 until bugunBaslangic }
            .sortedBy { it.dueAt }
            .take(2)
        val bugunlu = bekleyen
            .filter { it.dueAt in bugunBaslangic..bugunSon }
            .sortedBy { it.dueAt }
        val tarihsiz = bekleyen
            .filter { it.dueAt == 0L }
            .sortedByDescending { it.id }

        val sonuc = mutableListOf<Madde>()
        yarim.forEach { sonuc.add(Madde(it.metin, it.id, true)) }
        bugunlu.forEach { if (sonuc.size < 3) sonuc.add(Madde(it.metin, it.id, false)) }
        tarihsiz.forEach { if (sonuc.size < 3) sonuc.add(Madde(it.metin, it.id, false)) }
        return sonuc
    }

    // ---------------- Diyalog ----------------

    private fun gununBaslangici(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /**
     * Ana ekran onResume'unda çağrılır; bayrak varsa tek seferlik
     * plan diyaloğu açar. AI polishesi yapılırken diyalog bekletilmez:
     * önce yerel taslak gelir, metin hazır olunca sessizce güncellenir.
     */
    fun maybeGoster(aktivite: androidx.fragment.app.FragmentActivity) {
        val context = aktivite.applicationContext
        if (!bekliyorMu(context)) return
        val taslak = sec(
            Store.loadTasks(context).map { GorevOzet(it.id, it.text, it.dueAt, it.done) },
            gununBaslangici(), WidgetCommon.endOfToday()
        )
        if (taslak.isEmpty()) {
            prefs(context).edit().putBoolean("bekliyor", false).apply()
            return
        }
        prefs(context).edit().putBoolean("gosterildi", true).apply()

        val yerelMetin = taslak.joinToString("\n") { m ->
            (if (m.yarimMi) "↩ " else "▸ ") + m.metin
        }
        val mesaj = android.widget.TextView(aktivite).apply {
            text = yerelMetin
            textSize = 15f
            setLineSpacing(0f, 1.35f)
            val p = (18 * resources.displayMetrics.density).toInt()
            setPadding(p, (8 * resources.displayMetrics.density).toInt(), p, 0)
        }
        val kutu = android.widget.ScrollView(aktivite).apply { addView(mesaj) }

        val diyalog = com.google.android.material.dialog.MaterialAlertDialogBuilder(aktivite)
            .setTitle(R.string.ge_sabah_baslik)
            .setView(kutu)
            .setPositiveButton(R.string.ge_sabah_isle) { _, _ ->
                gorevlereIsle(context, taslak)
            }
            .setNegativeButton(R.string.ge_sabah_gec, null)
            .create()
        diyalog.show()

        // AI hazırsa maddeleri doğal dile çevirir; dönmezse yerel kalır
        if (AiSettings.isReady(context)) {
            aktivite.lifecycleScope.launch {
                val islak = withContext(Dispatchers.IO) {
                    withTimeoutOrNull(8_000L) {
                        aiIleIsle(context, taslak)
                    }
                }
                if (!islak.isNullOrBlank() && diyalog.isShowing) {
                    mesaj.text = islak
                }
            }
        }
    }

    /** Taslağı AI ile tek cümlelik plana çevirir; hata durumunda null. */
    private fun aiIleIsle(context: Context, taslak: List<Madde>): String? {
        return try {
            val istem = buildString {
                append("Kullanıcının bugünkü 3 önceliğini 3 kısa satır olarak yaz.\n")
                append("Her satır ▸ ile başlasın, sıcak ve net olsun, ")
                append("yarım kalanlar için ↩ kullan. Başka metin YAZMA.\n\n")
                taslak.forEach {
                    append(if (it.yarimMi) "YARIM: " else "BUGUN: ")
                    append(it.metin.take(80)).append("\n")
                }
            }
            val sonuc = AiClient.sadeIstek(context, istem, 220)
            if (sonuc.ok) sonuc.text.trim().take(500) else null
        } catch (e: Exception) {
            android.util.Log.w(TAG, "AI plan üretilemedi", e)
            null
        }
    }

    /** Onay: yarım kalanlar bugüne (1 saat sonrasına) taşınır. */
    private fun gorevlereIsle(context: Context, taslak: List<Madde>) {
        try {
            val tasinan = taslak.filter { it.yarimMi && it.gorevId > 0L }.map { it.gorevId }.toSet()
            if (tasinan.isNotEmpty()) {
                val hedefMs = System.currentTimeMillis() + 3_600_000L
                val gorevler = Store.loadTasks(context)
                gorevler.forEach { g ->
                    if (g.id in tasinan) {
                        g.dueAt = hedefMs
                        runCatching { AlarmScheduler.schedule(context, g.id, g.text, g.dueAt) }
                    }
                }
                Store.saveTasks(context, gorevler)
            }
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.ge_sabah_islendi, taslak.size),
                android.widget.Toast.LENGTH_SHORT
            ).show()
            WidgetCommon.refreshAll(context, true)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Görevlere işlenemedi", e)
        }
    }
}
