package com.gunlukasistan.app

import android.content.Context
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * v7.96 — Yedek şifreleme.
 *
 * ── Kullanıcı isteği (öneri 8) ──
 * "Yedek şu an düz JSON. Parola ile şifreleme. Telefon kaybolursa her şey
 *  gider — bu gerçek bir risk."
 *
 * ── Neden gerekliydi ──
 * `Store.exportJson` her şeyi düz metin veriyor: notlar, görevler, sohbet
 * geçmişi, **API anahtarları**. Bu dosya WhatsApp'tan gönderilse ya da
 * bulut klasöründe kalsa okuyan herkes her şeyi görür.
 *
 * ── Şifreleme seçimi ──
 * · **AES-256-GCM**: hem gizlilik hem bütünlük. Dosya kurcalanırsa çözme
 *   başarısız olur — sessizce bozuk veri yüklemek yerine hata verir.
 * · **PBKDF2-HMAC-SHA256, 120.000 tur**: parolayı anahtara çevirir.
 *   Yüksek tur sayısı kaba kuvvet saldırısını yavaşlatır.
 * · Her yedekte **yeni tuz ve IV** üretilir; aynı parola aynı çıktıyı
 *   vermez.
 *
 * ── Dosya biçimi ──
 * `GAENC1|<base64 tuz>|<base64 iv>|<base64 şifreli>`
 *
 * Başlık sayesinde geri yüklerken dosyanın şifreli olduğu anlaşılır ve
 * kullanıcıdan parola istenir. Şifresiz yedekler eskisi gibi çalışır.
 */
object YedekSifre {

    private const val TAG = "YedekSifre"

    /** Dosya başlığı — biçim sürümü değişirse artırılır. */
    const val BASLIK = "GAENC1"

    private const val TUZ_UZUNLUK = 16
    private const val IV_UZUNLUK = 12
    private const val ETIKET_BIT = 128
    private const val TUR_SAYISI = 120_000
    private const val ANAHTAR_BIT = 256

    /** Metin şifreli bir yedek mi? */
    fun sifreliMi(metin: String): Boolean = metin.trimStart().startsWith(BASLIK)

    // ═══════════════════════════════════════════════════════════════
    // ŞİFRELE
    // ═══════════════════════════════════════════════════════════════

    /**
     * Yedek metnini parolayla şifreler.
     * **Yavaş işlem (~1 sn) — arka planda çağır.**
     *
     * @return şifreli metin, hata olursa null
     */
    fun sifrele(duzMetin: String, parola: CharArray): String? = try {
        val rastgele = SecureRandom()
        val tuz = ByteArray(TUZ_UZUNLUK).also { rastgele.nextBytes(it) }
        val iv = ByteArray(IV_UZUNLUK).also { rastgele.nextBytes(it) }

        val anahtar = anahtarTuret(parola, tuz)
        val sifreleyici = Cipher.getInstance("AES/GCM/NoPadding")
        sifreleyici.init(
            Cipher.ENCRYPT_MODE, anahtar, GCMParameterSpec(ETIKET_BIT, iv)
        )
        val sifreli = sifreleyici.doFinal(duzMetin.toByteArray(Charsets.UTF_8))

        buildString {
            append(BASLIK).append('|')
            append(b64(tuz)).append('|')
            append(b64(iv)).append('|')
            append(b64(sifreli))
        }
    } catch (e: Exception) {
        android.util.Log.w(TAG, "Şifrelenemedi", e)
        null
    }

    // ═══════════════════════════════════════════════════════════════
    // ÇÖZ
    // ═══════════════════════════════════════════════════════════════

    /** Çözme sonucu — hata sebebini ayırt edebilmek için. */
    sealed class Sonuc {
        data class Basarili(val metin: String) : Sonuc()

        /** Parola yanlış ya da dosya bozuk — ikisi ayırt edilemez (GCM). */
        object ParolaYanlis : Sonuc()
        data class Hata(val mesaj: String) : Sonuc()
    }

    /**
     * Şifreli yedeği çözer.
     * **Yavaş işlem — arka planda çağır.**
     */
    fun coz(sifreliMetin: String, parola: CharArray): Sonuc {
        return try {
            val parcalar = sifreliMetin.trim().split('|')
            if (parcalar.size != 4 || parcalar[0] != BASLIK) {
                return Sonuc.Hata("bicim")
            }

            val tuz = deB64(parcalar[1])
            val iv = deB64(parcalar[2])
            val sifreli = deB64(parcalar[3])

            val anahtar = anahtarTuret(parola, tuz)
            val cozucu = Cipher.getInstance("AES/GCM/NoPadding")
            cozucu.init(Cipher.DECRYPT_MODE, anahtar, GCMParameterSpec(ETIKET_BIT, iv))

            Sonuc.Basarili(String(cozucu.doFinal(sifreli), Charsets.UTF_8))
        } catch (e: javax.crypto.AEADBadTagException) {
            // GCM doğrulama başarısız: yanlış parola ya da bozuk dosya
            Sonuc.ParolaYanlis
        } catch (e: javax.crypto.BadPaddingException) {
            Sonuc.ParolaYanlis
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Çözülemedi", e)
            Sonuc.Hata(e.message ?: "bilinmeyen")
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // PAROLA GÜCÜ
    // ═══════════════════════════════════════════════════════════════

    /** @return 0 zayıf · 1 orta · 2 güçlü */
    fun parolaGucu(parola: String): Int {
        if (parola.length < 6) return 0
        var puan = 0
        if (parola.length >= 8) puan++
        if (parola.length >= 12) puan++
        if (parola.any { it.isDigit() }) puan++
        if (parola.any { it.isLetter() }) puan++
        if (parola.any { !it.isLetterOrDigit() }) puan++
        return when {
            puan >= 4 -> 2
            puan >= 2 -> 1
            else -> 0
        }
    }

    fun gucAdi(context: Context, guc: Int): String = context.getString(
        when (guc) {
            2 -> R.string.ys_guc_guclu
            1 -> R.string.ys_guc_orta
            else -> R.string.ys_guc_zayif
        }
    )

    // ═══════════════════════════════════════════════════════════════
    // YARDIMCILAR
    // ═══════════════════════════════════════════════════════════════

    private fun anahtarTuret(parola: CharArray, tuz: ByteArray): SecretKeySpec {
        val fabrika = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(parola, tuz, TUR_SAYISI, ANAHTAR_BIT)
        return SecretKeySpec(fabrika.generateSecret(spec).encoded, "AES")
    }

    private fun b64(veri: ByteArray): String =
        Base64.encodeToString(veri, Base64.NO_WRAP)

    private fun deB64(metin: String): ByteArray =
        Base64.decode(metin, Base64.NO_WRAP)
}
