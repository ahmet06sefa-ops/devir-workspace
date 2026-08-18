package com.gunlukasistan.app

import android.content.Context
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * v10.14 · ULTRA-30 / E30 — Senenin Filmi (kapanış sahnesi).
 *
 * ── Tarama kanıtı ──
 * `YilIsiView` yıl ızgarasını ÇİZİYOR ama anlatmıyordu: "en uzun gün
 * hangisiydi, en güçlü seri kaçtı, hangi ay en çalışkandın" sorularının
 * cevabını hesaplayan hiçbir şey yoktu.
 *
 * ── Akış ──
 * Aralık ayında ana ekranda bir kez önerilir ([aralikOnerisi]);
 * [SeneFilmiActivity] sahneleri çalar, son sahnede özet kartı
 * [KartUretici] ile paylaşılabilir.
 *
 * Tüm hesaplar framework'süzdür ([hesapla], [enUzunSeri]); birim
 * testleri elle kurulmuş günlük kayıtlarıyla doğrular.
 */
object SeneFilmi {

    private val tr = Locale("tr", "TR")

    /** Filmin ham özeti. [enUzunGunMetin] ekran tarafında biçimlenir. */
    data class Ozet(
        val yil: Int,
        val aktifGun: Int,
        val toplamDk: Int,
        val enUzunSeri: Int,
        val enCaliskanAy: Int,      // 0..11 · -1 = veri yok
        val enCaliskanAyDk: Int,
        val enUzunGunAnahtar: String, // "yyyyMMdd" · "" = veri yok
        val enUzunGunDk: Int,
        val enUzunGunMetin: String = ""
    )

    /** Bir sonraki günün anahtarı (yıl/ay taşması dahil). */
    fun gunSonrasi(anahtar: String): String {
        return try {
            val cal = Calendar.getInstance().apply {
                set(
                    anahtar.substring(0, 4).toInt(),
                    anahtar.substring(4, 6).toInt() - 1,
                    anahtar.substring(6, 8).toInt()
                )
            }
            cal.add(Calendar.DAY_OF_YEAR, 1)
            String.format(
                Locale.US, "%04d%02d%02d",
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
            )
        } catch (e: Exception) {
            ""
        }
    }

    /** Ardışık aktif günlerin en uzun zinciri. Boş kümede 0. */
    fun enUzunSeri(aktifAnahtarlar: Set<String>): Int {
        if (aktifAnahtarlar.isEmpty()) return 0
        val sirali = aktifAnahtarlar.sorted()
        var enUzun = 1
        var kosan = 1
        for (i in 1 until sirali.size) {
            if (gunSonrasi(sirali[i - 1]) == sirali[i]) {
                kosan++
                if (kosan > enUzun) enUzun = kosan
            } else {
                kosan = 1
            }
        }
        return enUzun
    }

    /**
     * Yıllık özet hesabı.
     * Günlük kayıt alanları: "c" (biten görev), "f" (odak dakikası).
     * Aktif gün: en az bir çaba izi (c+f > 0).
     */
    fun hesapla(yil: Int, gunluk: JSONObject): Ozet {
        val onek = yil.toString()
        var aktif = 0
        var toplamDk = 0
        val ayDk = IntArray(12)
        val aktifler = mutableSetOf<String>()
        var enGun = ""
        var enGunDk = 0

        val anahtarlar = gunluk.keys()
        while (anahtarlar.hasNext()) {
            val k = anahtarlar.next()
            if (k.length != 8 || !k.startsWith(onek)) continue
            val o = gunluk.optJSONObject(k) ?: continue
            val c = o.optInt("c", 0)
            val f = o.optInt("f", 0)
            if (c + f <= 0) continue
            aktif++
            toplamDk += f
            aktifler.add(k)
            val ay0 = (k.substring(4, 6).toIntOrNull() ?: 1) - 1
            if (ay0 in 0..11) ayDk[ay0] += f
            if (f > enGunDk) {
                enGunDk = f
                enGun = k
            }
        }

        var enAy = -1
        ayDk.forEachIndexed { i, dk -> if (dk > 0 && (enAy < 0 || dk > ayDk[enAy])) enAy = i }

        return Ozet(
            yil = yil,
            aktifGun = aktif,
            toplamDk = toplamDk,
            enUzunSeri = enUzunSeri(aktifler),
            enCaliskanAy = enAy,
            enCaliskanAyDk = if (enAy >= 0) ayDk[enAy] else 0,
            enUzunGunAnahtar = enGun,
            enUzunGunDk = enGunDk
        )
    }

    // ---------------- Context'li yardımcılar ----------------

    /** Depodan yılın filmini üretir; gün metni de doldurulur. */
    fun olustur(context: Context, yil: Int): Ozet {
        val ham = try {
            hesapla(yil, Store.gunlukKayitKopyasi(context))
        } catch (e: Exception) {
            android.util.Log.w("SeneFilmi", "Özet hesaplanamadı", e)
            Ozet(yil, 0, 0, 0, -1, 0, "", 0)
        }
        val metin = if (ham.enUzunGunAnahtar.length == 8) {
            try {
                val d = SimpleDateFormat("yyyyMMdd", Locale.US).parse(ham.enUzunGunAnahtar)
                if (d != null) SimpleDateFormat("d MMMM", tr).format(d) else ""
            } catch (e: Exception) {
                ""
            }
        } else ""
        return ham.copy(enUzunGunMetin = metin)
    }

    fun ayAdi(context: Context, ay0: Int): String {
        val adlar = context.resources.getStringArray(R.array.w_ay_adlari)
        return if (ay0 in adlar.indices) adlar[ay0] else ""
    }

    // ---------------- Aralık önerisi ----------------

    private const val PREF = "ge_sene_filmi_v1"

    /**
     * Aralık'ta yılda bir kez ana ekranda "filmi izle" önerir.
     * Başka ayda ya da bu yıl gösterildiyse sessiz geçer.
     */
    fun aralikOnerisi(aktivite: androidx.fragment.app.FragmentActivity) {
        val simdi = Calendar.getInstance()
        if (simdi.get(Calendar.MONTH) != Calendar.DECEMBER) return
        val yil = simdi.get(Calendar.YEAR)
        val p = aktivite.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        if (p.getInt("onerilen", 0) >= yil) return
        val ozet = olustur(aktivite, yil)
        if (ozet.aktifGun < 5) return   // anlatacak hikâye yoksa sahne kurulmaz
        p.edit().putInt("onerilen", yil).apply()
        com.google.android.material.dialog.MaterialAlertDialogBuilder(aktivite)
            .setTitle(R.string.ge_film_oneri_baslik)
            .setMessage(aktivite.getString(R.string.ge_film_oneri_metin, yil))
            .setPositiveButton(R.string.ge_film_izle) { _, _ ->
                aktivite.startActivity(
                    android.content.Intent(aktivite, SeneFilmiActivity::class.java)
                )
            }
            .setNegativeButton(R.string.ge_film_sonra, null)
            .show()
    }
}
