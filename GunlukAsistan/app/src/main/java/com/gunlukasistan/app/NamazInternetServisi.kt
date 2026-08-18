package com.gunlukasistan.app

import android.content.Context
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * v11.13 — Namaz vakitleri GERÇEK internet servisi (SAF testli kısım dahil).
 *
 * Kullanıcı isteği: "Namaz saatleri gerçek namaz saatlerine uymuyor; aldığın
 * yeri değiştir ve internetten güncel namaz saatlerini getir."
 *
 * Önceki sürüm namaz vakitlerini YEREL astronomik hesapla üretiyordu; bu,
 * Diyanet'in resmi takviminden birkaç dakika sapabiliyordu. Bu servis, ücretsiz
 * ve HTTPS destekli **Aladhan API**'sinden (hesaplama yöntemi **13 = Diyanet**)
 * bugünün GERÇEK vakitlerini çeker.
 *
 *  · [Kayit] — çekilen vakitlerin modeli (SABAH = imsak, GUNES, OGLEN, IKINDI,
 *    AKSAM, YATSI).
 *  · [tarihAnahtari] — bugünün "DD-MM-YYYY" anahtarı (saf, testli).
 *  · [sonucuCoz] — API yanıtını Kayit'a çevirir (saf, testli).
 *  · [getir] — eşzamanlı HTTP çağrısı (kısa timeout). Arka planda çağrılmalı.
 *
 * UI katmanı önce önbelleğe bakar ([NamazAylikVeriServisi]); bu servis yalnız
 * gerçek ağ işini yapar. Ağ yoksa / başarısızsa astronomik hesaba düşülür.
 */
object NamazInternetServisi {

    /** Çekilen günlük vakitler. */
    data class Kayit(
        val imsak: String,
        val gunes: String,
        val ogle: String,
        val ikindi: String,
        val aksam: String,
        val yatsi: String
    )

    /** Aladhan API — method 13 = Diyanet İşleri Başkanlığı (Türkiye). */
    private const val API = "https://api.aladhan.com/v1/timings/%s" +
        "?latitude=%s&longitude=%s&method=13"

    private const val TIMEOUT_MS = 5000

    /**
     * Bugünün "DD-MM-YYYY" tarih anahtarı (saf — JVM testli).
     * Aladhan, tarihi bu biçimde ister.
     */
    fun tarihAnahtari(now: Date = Date()): String =
        SimpleDateFormat("dd-MM-yyyy", Locale.US).format(now)

    /**
     * Aladhan API yanıtını [Kayit]'a çevirir (saf — JVM testli).
     * Yanıt `data.timings.{Fajr,Sunrise,Dhuhr,Asr,Maghrib,Isha}` biçimindedir.
     * Geçersiz/eksikse null döner.
     */
    fun sonucuCoz(ham: String): Kayit? {
        return try {
            val kok = JSONObject(ham)
            val data = kok.optJSONObject("data") ?: return null
            val timings = data.optJSONObject("timings") ?: return null
            val imsak = timings.optString("Fajr", "").substringBefore(" ")
            val gunes = timings.optString("Sunrise", "").substringBefore(" ")
            val ogle = timings.optString("Dhuhr", "").substringBefore(" ")
            val ikindi = timings.optString("Asr", "").substringBefore(" ")
            val aksam = timings.optString("Maghrib", "").substringBefore(" ")
            val yatsi = timings.optString("Isha", "").substringBefore(" ")
            if (imsak.isBlank() || aksam.isBlank()) null
            else Kayit(imsak, gunes, ogle, ikindi, aksam, yatsi)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Gerçek HTTP çağrısı ile bugünün vakitlerini çeker (eşzamanlı).
     * @return çekilen kayıt; ağ yoksa / hata varsa / timeout ise null.
     * NOT: UI thread'de ÇAĞIRMA (5sn timeout'lu bloklama yapar) — arka planda çalıştır.
     */
    fun getir(enlem: Double, boylam: Double): Kayit? {
        val urlStr = String.format(
            Locale.US, API,
            tarihAnahtari(),
            "%.4f".format(enlem), "%.4f".format(boylam)
        )
        return try {
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.setRequestProperty("Accept", "application/json")
            val code = conn.responseCode
            if (code !in 200..299) {
                conn.disconnect()
                return null
            }
            val yanit = BufferedReader(
                InputStreamReader(conn.inputStream, Charsets.UTF_8)
            ).use { it.readText() }
            conn.disconnect()
            sonucuCoz(yanit)
        } catch (e: Exception) {
            null
        }
    }
}
