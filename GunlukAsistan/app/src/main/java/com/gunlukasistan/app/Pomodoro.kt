package com.gunlukasistan.app

import android.content.Context

/**
 * v7.94 — Pomodoro döngüsü ve odak modu.
 *
 * ── Kullanıcı isteği (öneri 2 ve 7) ──
 * "Çalış → mola → çalış" döngüsü ve sayaç çalışırken Rahatsız Etmeyin'in
 * otomatik açılması.
 *
 * ── v7.86'daki `otomatikTekrar`'dan farkı ──
 * O ayar süre bitince **aynı süreyle** yeniden başlatıyordu: 25 dk çalış,
 * 25 dk çalış, 25 dk çalış... Mola yoktu. Gerçek pomodoro çalışma ve mola
 * sürelerini ayırır ve belirli tur sayısından sonra uzun mola verir.
 *
 * ── Durum neden burada tutuluyor ──
 * [TimerEngine] tek bir süreyi bilir; "şu an çalışma mı mola mı" kavramı
 * yok. Bu bilgi ayrı tutulup sayaç bittiğinde okunuyor, böylece
 * TimerEngine'in sözleşmesi değişmiyor.
 */
object Pomodoro {

    private const val TAG = "Pomodoro"
    private const val PREF = "pomodoro_v1"

    /** Şu anki evre. */
    const val EVRE_CALISMA = 0
    const val EVRE_KISA_MOLA = 1
    const val EVRE_UZUN_MOLA = 2

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ═══════════════════════════════════════════════════════════════
    // AYARLAR
    // ═══════════════════════════════════════════════════════════════

    fun acikMi(context: Context): Boolean = prefs(context).getBoolean("acik", false)

    fun setAcik(context: Context, acik: Boolean) {
        prefs(context).edit().putBoolean("acik", acik).apply()
        if (!acik) sifirla(context)
    }

    fun calismaDk(context: Context): Int =
        prefs(context).getInt("calisma", 25).coerceIn(1, 180)

    fun setCalismaDk(context: Context, dk: Int) {
        prefs(context).edit().putInt("calisma", dk.coerceIn(1, 180)).apply()
    }

    fun kisaMolaDk(context: Context): Int =
        prefs(context).getInt("kisa", 5).coerceIn(1, 60)

    fun setKisaMolaDk(context: Context, dk: Int) {
        prefs(context).edit().putInt("kisa", dk.coerceIn(1, 60)).apply()
    }

    fun uzunMolaDk(context: Context): Int =
        prefs(context).getInt("uzun", 15).coerceIn(1, 120)

    fun setUzunMolaDk(context: Context, dk: Int) {
        prefs(context).edit().putInt("uzun", dk.coerceIn(1, 120)).apply()
    }

    /** Kaç çalışma turundan sonra uzun mola. */
    fun uzunMolaAraligi(context: Context): Int =
        prefs(context).getInt("aralik", 4).coerceIn(2, 10)

    fun setUzunMolaAraligi(context: Context, tur: Int) {
        prefs(context).edit().putInt("aralik", tur.coerceIn(2, 10)).apply()
    }

    /**
     * Mola bitince çalışma otomatik başlasın mı?
     *
     * Varsayılan **true**: molanın bitip çalışmanın başlamaması pomodoro
     * akışını bozar. Ama çalışma bitince mola otomatik başlar
     * ([molaOtomatik]) — bu ayrı, çünkü kullanıcı çalışmayı bitirip
     * telefonu bırakmış olabilir.
     */
    fun molaSonrasiOtomatik(context: Context): Boolean =
        prefs(context).getBoolean("mola_sonrasi", true)

    fun setMolaSonrasiOtomatik(context: Context, acik: Boolean) {
        prefs(context).edit().putBoolean("mola_sonrasi", acik).apply()
    }

    fun molaOtomatik(context: Context): Boolean =
        prefs(context).getBoolean("mola_oto", true)

    fun setMolaOtomatik(context: Context, acik: Boolean) {
        prefs(context).edit().putBoolean("mola_oto", acik).apply()
    }

    // ═══════════════════════════════════════════════════════════════
    // DURUM
    // ═══════════════════════════════════════════════════════════════

    fun evre(context: Context): Int =
        prefs(context).getInt("evre", EVRE_CALISMA).coerceIn(0, 2)

    private fun setEvre(context: Context, e: Int) {
        prefs(context).edit().putInt("evre", e).apply()
    }

    /** Bugün tamamlanan çalışma turu sayısı. */
    fun tur(context: Context): Int = prefs(context).getInt("tur", 0)

    private fun setTur(context: Context, t: Int) {
        prefs(context).edit().putInt("tur", t).apply()
    }

    fun molada(context: Context): Boolean = evre(context) != EVRE_CALISMA

    /** Döngüyü başa alır. */
    fun sifirla(context: Context) {
        prefs(context).edit().putInt("evre", EVRE_CALISMA).putInt("tur", 0).apply()
    }

    /** Bu evrenin süresi (dakika). */
    fun evreSuresi(context: Context): Int = when (evre(context)) {
        EVRE_KISA_MOLA -> kisaMolaDk(context)
        EVRE_UZUN_MOLA -> uzunMolaDk(context)
        else -> calismaDk(context)
    }

    fun evreAdi(context: Context, e: Int = evre(context)): String = context.getString(
        when (e) {
            EVRE_KISA_MOLA -> R.string.pm_kisa_mola
            EVRE_UZUN_MOLA -> R.string.pm_uzun_mola
            else -> R.string.pm_calisma
        }
    )

    // ═══════════════════════════════════════════════════════════════
    // DÖNGÜ
    // ═══════════════════════════════════════════════════════════════

    /**
     * Bir evre bittiğinde çağrılır; sıradaki evreye geçer.
     *
     * @return (yeni evre, süre dakika, otomatik başlasın mı)
     */
    fun sonrakiEvre(context: Context): Triple<Int, Int, Boolean> {
        val simdiki = evre(context)

        return if (simdiki == EVRE_CALISMA) {
            // Çalışma bitti → tur say, mola belirle
            val yeniTur = tur(context) + 1
            setTur(context, yeniTur)

            val uzunMu = yeniTur % uzunMolaAraligi(context) == 0
            val yeni = if (uzunMu) EVRE_UZUN_MOLA else EVRE_KISA_MOLA
            setEvre(context, yeni)

            Triple(
                yeni,
                if (uzunMu) uzunMolaDk(context) else kisaMolaDk(context),
                molaOtomatik(context)
            )
        } else {
            // Mola bitti → çalışmaya dön
            setEvre(context, EVRE_CALISMA)
            Triple(EVRE_CALISMA, calismaDk(context), molaSonrasiOtomatik(context))
        }
    }

    /** Durum özeti — ekranda ve bildirimde gösterilir. */
    fun ozet(context: Context): String {
        if (!acikMi(context)) return ""
        val t = tur(context)
        val aralik = uzunMolaAraligi(context)
        val turIcinde = if (aralik == 0) 0 else t % aralik
        return context.getString(
            R.string.pm_ozet, evreAdi(context), turIcinde, aralik
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // v7.94 — ODAK MODU (öneri 7)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Sayaç çalışırken Rahatsız Etmeyin açılsın mı.
     *
     * Varsayılan **kapalı**: kullanıcının bildirimlerini habersizce
     * susturmak agresif bir davranış. İzin de gerektiriyor.
     */
    fun odakModu(context: Context): Boolean =
        prefs(context).getBoolean("odak", false)

    fun setOdakModu(context: Context, acik: Boolean) {
        prefs(context).edit().putBoolean("odak", acik).apply()
        if (!acik) dndKapat(context)
    }

    /** DND'yi biz mi açtık — başkasının açtığını kapatmayalım. */
    private fun bizActik(context: Context): Boolean =
        prefs(context).getBoolean("dnd_biz", false)

    private fun setBizActik(context: Context, deger: Boolean) {
        prefs(context).edit().putBoolean("dnd_biz", deger).apply()
    }

    /**
     * Odak modunu uygular.
     *
     * @param calisiyor sayaç çalışıyor mu
     *
     * Molada DND açılmaz — mola zaten telefona bakma zamanı.
     */
    fun odagiEsitle(context: Context, calisiyor: Boolean) {
        if (!odakModu(context)) return
        if (!ZorunluUyari.dndIzniVar(context)) return

        val acilsin = calisiyor && !molada(context)
        if (acilsin) dndAc(context) else dndKapat(context)
    }

    private fun dndAc(context: Context) {
        if (bizActik(context)) return
        try {
            val nm = context.getSystemService(android.app.NotificationManager::class.java)
                ?: return
            // Zaten açıksa dokunma — kullanıcının kendi ayarını bozmayalım
            if (nm.currentInterruptionFilter !=
                android.app.NotificationManager.INTERRUPTION_FILTER_ALL
            ) return

            // PRIORITY: alarmlar geçer, bildirimler susar.
            // NONE seçilseydi sayacın bitiş alarmı da susardı.
            nm.setInterruptionFilter(
                android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY
            )
            setBizActik(context, true)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "DND açılamadı", e)
        }
    }

    private fun dndKapat(context: Context) {
        if (!bizActik(context)) return
        try {
            context.getSystemService(android.app.NotificationManager::class.java)
                ?.setInterruptionFilter(
                    android.app.NotificationManager.INTERRUPTION_FILTER_ALL
                )
        } catch (e: Exception) {
            android.util.Log.w(TAG, "DND kapatılamadı", e)
        } finally {
            setBizActik(context, false)
        }
    }
}
