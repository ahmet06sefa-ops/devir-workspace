package com.gunlukasistan.app

import android.content.Context
import android.os.StatFs
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * v10.37 — A'dan Z'ye sistem sağlık kontrolü.
 *
 * Tek tek her modülü denetler; ilerleme (yapılan/toplam), yüzde ve
 * tahmini kalan süre canlı bildirilir. Güvenli sorunlar (yetim kayıt,
 * bozuk JSON, eski önbellek dosyaları) isteğe bağlı OTOMATİK ONARILIR.
 *
 * Onarım motoru kural tabanlıdır ve tamamen cihaz içinde çalışır;
 * hiçbir veri cihazdan çıkmaz. Onarımda bozuk veri önce yedek
 * anahtara taşınır — bilinçli veri kaybı olmaz.
 */
object SaglikMotoru {

    private const val TAG = "SaglikMotoru"
    private const val STORE_PREF = "gunluk_asistan_store"

    enum class Durum { IYI, BILGI, UYARI, HATA, ONARILDI }

    /** Tek bir kontrol maddesinin sonucu. */
    data class Madde(
        val id: String,
        val ad: String,
        val durum: Durum,
        val detay: String,
        val onarildi: Boolean = false
    )

    /** Canlı ilerleme bildirimi. */
    fun interface Ilerleme {
        fun bildir(yapilan: Int, toplam: Int, madde: Madde)
    }

    // ══════════════════════════════════════════════════════════
    // Saf (birim testi yapılabilir) yardımcılar
    // ══════════════════════════════════════════════════════════

    /** İlerleme yüzdesi. */
    fun yuzde(yapilan: Int, toplam: Int): Int =
        if (toplam <= 0) 100 else (yapilan * 100 / toplam).coerceIn(0, 100)

    /** Ortalama madde süresinden kalan süre tahmini (ms). */
    fun tahminiKalanMs(gecenMs: Long, yapilan: Int, toplam: Int): Long {
        if (yapilan <= 0 || toplam <= 0 || yapilan >= toplam) return 0L
        return (gecenMs.toDouble() / yapilan * (toplam - yapilan)).toLong()
    }

    /** Geçerli kimliklerde olmayan (yetim) kayıtları döndürür. */
    fun yetimBul(kume: Set<Long>, gecerli: Set<Long>): Set<Long> = kume - gecerli

    /** Art arda boşlukları teke indirir, uçları kırpar. */
    fun temizYazi(ham: String): String = ham.replace(Regex(" {2,}"), " ").trim()

    /** JSON metni çözümlenebiliyor mu? (dizi veya nesne) */
    internal fun jsonGecerliMi(ham: String?): Boolean {
        if (ham.isNullOrBlank()) return false
        return try {
            JSONArray(ham)
            true
        } catch (_: Exception) {
            try {
                JSONObject(ham)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    /** Bir JSON dizisindeki öge kimliklerini ("id") çıkarır; bozuksa null. */
    internal fun jsonKimlikler(ham: String?): Set<Long>? {
        return try {
            val dizi = JSONArray(ham ?: "[]")
            val kume = mutableSetOf<Long>()
            for (i in 0 until dizi.length()) {
                val o = dizi.optJSONObject(i) ?: return null
                if (!o.has("id")) return null
                kume.add(o.getLong("id"))
            }
            kume
        } catch (_: Exception) {
            null
        }
    }

    /** [iyi, uyari, hata, onarilan, bilgi] — ONARILDI aynı zamanda iyi sayılır. */
    fun ozetSayilari(liste: List<Madde>): IntArray {
        var iyi = 0; var uyari = 0; var hata = 0; var onarilan = 0; var bilgi = 0
        for (m in liste) {
            when (m.durum) {
                Durum.IYI -> iyi++
                Durum.UYARI -> uyari++
                Durum.HATA -> hata++
                Durum.ONARILDI -> { onarilan++; iyi++ }
                Durum.BILGI -> bilgi++
            }
        }
        return intArrayOf(iyi, uyari, hata, onarilan, bilgi)
    }

    /** Duruma göre rozet emojisi. */
    fun emoji(d: Durum): String = when (d) {
        Durum.IYI -> "✅"
        Durum.BILGI -> "ℹ️"
        Durum.UYARI -> "⚠️"
        Durum.HATA -> "🔴"
        Durum.ONARILDI -> "🔧"
    }

    // ══════════════════════════════════════════════════════════
    // Ana koşucu
    // ══════════════════════════════════════════════════════════

    /** 21 maddeyi sırayla çalıştırır; her madde bitiminde [ile] çağrılır. */
    fun calistir(c: Context, onar: Boolean, ile: Ilerleme? = null): List<Madde> {
        val kontroller: List<(Context, Boolean) -> Madde> = listOf(
            ::kDepoNotlar, ::kDepoGorevler, ::kDepoKonular, ::kDepoGunluk,
            ::kNotArsiv, ::kNotSabitle, ::kNotKilit, ::kGorevBekliyor,
            ::kNotRenk, ::kNotSurum, ::kIleriGecmis, ::kOnbellek,
            ::kBildirim, ::kTamAlarm, ::kPil, ::kUretici,
            ::kAlan, ::kCokme, ::kAi, ::kPin, ::kYazi
        )
        val sonuc = ArrayList<Madde>(kontroller.size)
        kontroller.forEachIndexed { i, k ->
            val m = runCatching { k(c, onar) }.getOrElse {
                android.util.Log.w(TAG, "madde $i patladi", it)
                Madde(
                    "hata_$i", c.getString(R.string.w37_hata_ad), Durum.HATA,
                    c.getString(R.string.w37_hata_metni, it.javaClass.simpleName)
                )
            }
            sonuc.add(m)
            try {
                ile?.bildir(i + 1, kontroller.size, m)
            } catch (_: Exception) {
                // ilerleme bildirimi patlarsa kontrol akışı etkilenmez
            }
        }
        return sonuc
    }

    // ══════════════════════════════════════════════════════════
    // Depo bütünlüğü
    // ══════════════════════════════════════════════════════════

    private fun storePref(c: Context) =
        c.getSharedPreferences(STORE_PREF, Context.MODE_PRIVATE)

    private fun notKimlikleri(c: Context): Set<Long>? =
        jsonKimlikler(storePref(c).getString("notes_json", "[]"))

    private fun gorevKimlikleri(c: Context): Set<Long>? =
        jsonKimlikler(storePref(c).getString("tasks_json", "[]"))

    private fun depoKontrol(c: Context, anahtar: String, adRes: Int, onar: Boolean): Madde {
        val ad = c.getString(adRes)
        val id = "depo_$anahtar"
        val ham = storePref(c).getString(anahtar, null)
        if (ham.isNullOrBlank()) {
            return Madde(id, ad, Durum.IYI, c.getString(R.string.w37_d_bos))
        }
        if (jsonGecerliMi(ham)) {
            val adet = runCatching { JSONArray(ham).length() }.getOrDefault(-1)
            val detay = if (adet >= 0) {
                c.getString(R.string.w37_d_saglam, adet)
            } else {
                c.getString(R.string.w37_d_saglam_nesne)
            }
            return Madde(id, ad, Durum.IYI, detay)
        }
        if (!onar) return Madde(id, ad, Durum.HATA, c.getString(R.string.w37_d_bozuk))
        val yedek = "${anahtar}_bozuk_${System.currentTimeMillis()}"
        storePref(c).edit().putString(yedek, ham).remove(anahtar).apply()
        return Madde(id, ad, Durum.ONARILDI, c.getString(R.string.w37_d_onarildi_yedek), onarildi = true)
    }

    private fun kDepoNotlar(c: Context, onar: Boolean) =
        depoKontrol(c, "notes_json", R.string.w37_m_depo_notlar, onar)

    private fun kDepoGorevler(c: Context, onar: Boolean) =
        depoKontrol(c, "tasks_json", R.string.w37_m_depo_gorevler, onar)

    private fun kDepoKonular(c: Context, onar: Boolean) =
        depoKontrol(c, "topics_json", R.string.w37_m_depo_konular, onar)

    private fun kDepoGunluk(c: Context, onar: Boolean) =
        depoKontrol(c, "daily_log_json", R.string.w37_m_depo_gunluk, onar)

    // ══════════════════════════════════════════════════════════
    // Küme tutarlılığı (arşiv, sabitleme, kilit, bekliyor)
    // ══════════════════════════════════════════════════════════

    private fun kumeKontrol(
        c: Context, id: String, adRes: Int, prefAd: String, anahtar: String,
        gecerli: Set<Long>?, onar: Boolean, stringSetMi: Boolean = false
    ): Madde {
        val ad = c.getString(adRes)
        if (gecerli == null) {
            return Madde(id, ad, Durum.UYARI, c.getString(R.string.w37_d_depo_okunamadi))
        }
        val p = c.getSharedPreferences(prefAd, Context.MODE_PRIVATE)
        val mevcut: Set<Long> = if (stringSetMi) {
            p.getStringSet(anahtar, emptySet<String>()).orEmpty()
                .mapNotNullTo(mutableSetOf()) { it.toLongOrNull() }
        } else {
            runCatching { NotKilit.kumeJsondan(p.getString(anahtar, "[]")) }.getOrDefault(emptySet())
        }
        val yetim = yetimBul(mevcut, gecerli)
        if (yetim.isEmpty()) {
            return Madde(id, ad, Durum.IYI, c.getString(R.string.w37_d_tutarli, mevcut.size))
        }
        if (!onar) return Madde(id, ad, Durum.UYARI, c.getString(R.string.w37_d_yetim, yetim.size))
        val yeni = mevcut - yetim
        if (stringSetMi) {
            p.edit().putStringSet(anahtar, yeni.mapTo(mutableSetOf()) { it.toString() }).apply()
        } else {
            p.edit().putString(anahtar, NotKilit.kumeJsonaYaz(yeni)).apply()
        }
        return Madde(id, ad, Durum.ONARILDI, c.getString(R.string.w37_d_yetim_onarildi, yetim.size), onarildi = true)
    }

    private fun kNotArsiv(c: Context, onar: Boolean) = kumeKontrol(
        c, "not_arsiv", R.string.w37_m_arsiv, "not_arsiv_v1", "arsivli", notKimlikleri(c), onar
    )

    private fun kNotSabitle(c: Context, onar: Boolean) = kumeKontrol(
        c, "not_sabitle", R.string.w37_m_sabitle, "not_sabitle_v1", "kume",
        notKimlikleri(c), onar, stringSetMi = true
    )

    private fun kNotKilit(c: Context, onar: Boolean) = kumeKontrol(
        c, "not_kilit", R.string.w37_m_kilit, "not_kilit_v1", "kilitli", notKimlikleri(c), onar
    )

    private fun kGorevBekliyor(c: Context, onar: Boolean) = kumeKontrol(
        c, "gorev_bekliyor", R.string.w37_m_bekliyor, "gorev_bekliyor_v1", "kume",
        gorevKimlikleri(c), onar
    )

    // ══════════════════════════════════════════════════════════
    // Renk etiketleri ve sürüm geçmişi
    // ══════════════════════════════════════════════════════════

    private fun kNotRenk(c: Context, onar: Boolean): Madde {
        val ad = c.getString(R.string.w37_m_renk)
        val gecerli = notKimlikleri(c)
            ?: return Madde("not_renk", ad, Durum.UYARI, c.getString(R.string.w37_d_depo_okunamadi))
        val p = c.getSharedPreferences("not_renk_v1", Context.MODE_PRIVATE)
        val ham = p.getString("harita", "{}") ?: "{}"
        val obj = try {
            JSONObject(ham)
        } catch (_: Exception) {
            null
        }
        if (obj == null) {
            if (!onar) return Madde("not_renk", ad, Durum.HATA, c.getString(R.string.w37_d_bozuk))
            p.edit().putString("harita", "{}").apply()
            return Madde("not_renk", ad, Durum.ONARILDI, c.getString(R.string.w37_d_renk_sifirlandi), onarildi = true)
        }
        val yetimAnahtar = mutableListOf<String>()
        val it = obj.keys()
        while (it.hasNext()) {
            val k = it.next()
            val notId = k.toLongOrNull()
            if (notId == null || notId !in gecerli) yetimAnahtar.add(k)
        }
        if (yetimAnahtar.isEmpty()) {
            return Madde("not_renk", ad, Durum.IYI, c.getString(R.string.w37_d_tutarli, obj.length()))
        }
        if (!onar) {
            return Madde("not_renk", ad, Durum.UYARI, c.getString(R.string.w37_d_yetim, yetimAnahtar.size))
        }
        for (k in yetimAnahtar) obj.remove(k)
        p.edit().putString("harita", obj.toString()).apply()
        return Madde("not_renk", ad, Durum.ONARILDI, c.getString(R.string.w37_d_yetim_onarildi, yetimAnahtar.size), onarildi = true)
    }

    private fun kNotSurum(c: Context, onar: Boolean): Madde {
        val ad = c.getString(R.string.w37_m_surum)
        val gecerli = notKimlikleri(c)
            ?: return Madde("not_surum", ad, Durum.UYARI, c.getString(R.string.w37_d_depo_okunamadi))
        val p = c.getSharedPreferences("not_surum_v1", Context.MODE_PRIVATE)
        val yetim = p.all.keys.filter { k ->
            if (!k.startsWith("g_")) {
                false
            } else {
                val notId = k.removePrefix("g_").toLongOrNull()
                notId == null || notId !in gecerli
            }
        }
        val toplam = p.all.keys.count { it.startsWith("g_") }
        if (yetim.isEmpty()) {
            return Madde("not_surum", ad, Durum.IYI, c.getString(R.string.w37_d_surum_temiz, toplam))
        }
        if (!onar) {
            return Madde("not_surum", ad, Durum.UYARI, c.getString(R.string.w37_d_surum_yetim, yetim.size))
        }
        val e = p.edit()
        yetim.forEach { e.remove(it) }
        e.apply()
        return Madde("not_surum", ad, Durum.ONARILDI, c.getString(R.string.w37_d_surum_onarildi, yetim.size), onarildi = true)
    }

    // ══════════════════════════════════════════════════════════
    // İleri sayım geçmişi ve paylaşım önbelleği
    // ══════════════════════════════════════════════════════════

    private fun kIleriGecmis(c: Context, onar: Boolean): Madde {
        val ad = c.getString(R.string.w37_m_ileri)
        val p = c.getSharedPreferences("ileri_sayim_gecmis_v1", Context.MODE_PRIVATE)
        val ham = p.getString("oturumlar", "[]") ?: "[]"
        if (jsonGecerliMi(ham)) {
            val adet = runCatching { SurecPlan.jsondanOku(ham).size }.getOrDefault(0)
            return Madde("ileri_gecmis", ad, Durum.IYI, c.getString(R.string.w37_d_oturum, adet))
        }
        if (!onar) return Madde("ileri_gecmis", ad, Durum.HATA, c.getString(R.string.w37_d_bozuk))
        p.edit()
            .putString("oturumlar_bozuk_${System.currentTimeMillis()}", ham)
            .remove("oturumlar")
            .apply()
        return Madde("ileri_gecmis", ad, Durum.ONARILDI, c.getString(R.string.w37_d_onarildi_yedek), onarildi = true)
    }

    private fun kOnbellek(c: Context, onar: Boolean): Madde {
        val ad = c.getString(R.string.w37_m_onbellek)
        val sinir = System.currentTimeMillis() - 24L * 60 * 60 * 1000
        val klasorler = listOf(File(c.cacheDir, "notlar"), File(c.cacheDir, "kartlar"))
        val eskiler = klasorler.flatMap { d ->
            d.listFiles()?.filter { it.isFile && it.lastModified() < sinir } ?: emptyList()
        }
        if (eskiler.isEmpty()) {
            return Madde("onbellek", ad, Durum.IYI, c.getString(R.string.w37_d_onbellek_temiz))
        }
        if (!onar) {
            return Madde("onbellek", ad, Durum.BILGI, c.getString(R.string.w37_d_onbellek_eski, eskiler.size))
        }
        var silindi = 0
        eskiler.forEach { runCatching { if (it.delete()) silindi++ } }
        return Madde("onbellek", ad, Durum.ONARILDI, c.getString(R.string.w37_d_onbellek_silindi, silindi), onarildi = true)
    }

    // ══════════════════════════════════════════════════════════
    // Sistem tarafı (izinler, pil, üretici, alan, çökme)
    // ══════════════════════════════════════════════════════════

    @Suppress("UNUSED_PARAMETER")
    private fun kBildirim(c: Context, onar: Boolean): Madde {
        val ad = c.getString(R.string.w37_m_bildirim)
        return if (BildirimTani.bildirimIzniVar(c)) {
            Madde("bildirim", ad, Durum.IYI, c.getString(R.string.w37_d_izin_var))
        } else {
            Madde("bildirim", ad, Durum.HATA, c.getString(R.string.w37_d_izin_yok))
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun kTamAlarm(c: Context, onar: Boolean): Madde {
        val ad = c.getString(R.string.w37_m_alarm)
        return if (AlarmSagligi.tamAlarmIzniVar(c)) {
            Madde("tam_alarm", ad, Durum.IYI, c.getString(R.string.w37_d_alarm_var))
        } else {
            Madde("tam_alarm", ad, Durum.UYARI, c.getString(R.string.w37_d_alarm_yok))
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun kPil(c: Context, onar: Boolean): Madde {
        val ad = c.getString(R.string.w37_m_pil)
        return if (AlarmSagligi.pilKisitsizMi(c)) {
            Madde("pil", ad, Durum.IYI, c.getString(R.string.w37_d_pil_serbest))
        } else {
            Madde("pil", ad, Durum.UYARI, c.getString(R.string.w37_d_pil_kisitli))
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun kUretici(c: Context, onar: Boolean): Madde {
        val ad = c.getString(R.string.w37_m_uretici)
        return if (AlarmSagligi.agresifUreticiMi()) {
            Madde(
                "uretici", ad, Durum.BILGI,
                c.getString(R.string.w37_d_uretici_riskli, AlarmSagligi.ureticiAdi(), AlarmSagligi.ureticiYonergesi() ?: "-")
            )
        } else {
            Madde("uretici", ad, Durum.IYI, c.getString(R.string.w37_d_uretici_sakin))
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun kAlan(c: Context, onar: Boolean): Madde {
        val ad = c.getString(R.string.w37_m_alan)
        val sf = StatFs(c.filesDir.absolutePath)
        val mb = sf.availableBytes / (1024L * 1024L)
        return if (mb < 50) {
            Madde("alan", ad, Durum.UYARI, c.getString(R.string.w37_d_alan_az, mb))
        } else {
            Madde("alan", ad, Durum.IYI, c.getString(R.string.w37_d_alan, mb))
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun kCokme(c: Context, onar: Boolean): Madde {
        val ad = c.getString(R.string.w37_m_cokme)
        val varMi = runCatching { CokmeRapor.cokmeVarMi(c) }.getOrDefault(false)
        if (!varMi) return Madde("cokme", ad, Durum.IYI, c.getString(R.string.w37_d_cokme_yok))
        val tekrar = runCatching { CokmeRapor.tekrarEdenler(c).size }.getOrDefault(0)
        return Madde("cokme", ad, Durum.UYARI, c.getString(R.string.w37_d_cokme_var, tekrar))
    }

    @Suppress("UNUSED_PARAMETER")
    private fun kAi(c: Context, onar: Boolean): Madde {
        val ad = c.getString(R.string.w37_m_ai)
        val kurulu = runCatching { AiSettings.hasApiKey(c) }.getOrDefault(false)
        return if (kurulu) {
            Madde("ai", ad, Durum.BILGI, c.getString(R.string.w37_d_ai_var))
        } else {
            Madde("ai", ad, Durum.BILGI, c.getString(R.string.w37_d_ai_yok))
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun kPin(c: Context, onar: Boolean): Madde {
        val ad = c.getString(R.string.w37_m_pin)
        val kurulu = c.getSharedPreferences("kilit_v1", Context.MODE_PRIVATE)
            .getString("hash", null)?.isNotBlank() == true
        return Madde(
            "pin", ad, Durum.BILGI,
            c.getString(if (kurulu) R.string.w37_kilit_kurulu else R.string.w37_kilit_yok)
        )
    }

    // ══════════════════════════════════════════════════════════
    // Yazı düzeni (yerleşik metinlerde çift boşluk / taşma taraması)
    // ══════════════════════════════════════════════════════════

    @Suppress("UNUSED_PARAMETER")
    private fun kYazi(c: Context, onar: Boolean): Madde {
        val ad = c.getString(R.string.w37_m_yazi)
        val ornekler = intArrayOf(
            R.string.row_ai, R.string.row_backup, R.string.row_notif,
            R.string.w22_satir, R.string.w34_kilit_baslik,
            R.string.w36_bekliyor, R.string.w37_onar_cb, R.string.w37_baslik
        )
        var sorun = 0
        for (r in ornekler) {
            val s = runCatching { c.getString(r) }.getOrDefault("")
            if (s.isNotEmpty() && s != temizYazi(s)) sorun++
        }
        return if (sorun == 0) {
            Madde("yazi", ad, Durum.IYI, c.getString(R.string.w37_d_yazi_duzenli, ornekler.size))
        } else {
            Madde("yazi", ad, Durum.UYARI, c.getString(R.string.w37_d_yazi_sorun, sorun))
        }
    }

    // ══════════════════════════════════════════════════════════
    // Paylaşılabilir rapor metni
    // ══════════════════════════════════════════════════════════

    fun raporMetni(c: Context, liste: List<Madde>, sureMs: Long): String {
        val s = ozetSayilari(liste)
        val simdi = java.text.SimpleDateFormat("d MMM yyyy HH:mm", java.util.Locale("tr"))
            .format(java.util.Date())
        val sn = String.format(java.util.Locale.US, "%.1f", sureMs / 1000.0)
        val sb = StringBuilder()
        sb.append(c.getString(R.string.w37_rapor_baslik, simdi)).append('\n')
        sb.append(c.getString(R.string.w37_rapor_ozet, s[0], s[1], s[2], s[3], sn)).append('\n').append('\n')
        for (m in liste) {
            sb.append(emoji(m.durum)).append(' ').append(m.ad)
            if (m.detay.isNotBlank()) sb.append(" — ").append(m.detay)
            sb.append('\n')
        }
        return sb.toString().trim()
    }
}
