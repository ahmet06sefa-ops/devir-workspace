package com.gunlukasistan.app

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * v8.8 — API anahtarı kasası (öneri 1).
 *
 * ══════════════════════════════════════════════════════════════════
 * ÖNCEKİ DURUM — neden yetersizdi
 * ══════════════════════════════════════════════════════════════════
 * `AiSettings.mask()` şunu yapıyordu:
 *
 *     bytes[i] XOR "gunlukasistan"[i % 13]
 *
 * Bu **şifreleme değil, gizleme**. Sorunları:
 *   1. Anahtar kaynak kodda düz metin — APK'yı açan herkes görür
 *   2. XOR tersine çevrilebilir; 13 baytlık tekrarlı desen
 *      istatistiksel analizle anahtarsız bile kırılır
 *   3. Cihaza bağlı değil — bir cihazdan alınan veri başka cihazda çözülür
 *
 * Gerçek risk: root'lu cihazda `/data/data/.../ai_settings.xml`
 * okunabiliyor. Kullanıcının OpenAI/Gemini anahtarı çalınırsa
 * faturası başkasına çıkar.
 *
 * ══════════════════════════════════════════════════════════════════
 * YENİ YAKLAŞIM — Android Keystore
 * ══════════════════════════════════════════════════════════════════
 * AES-256-GCM. Şifreleme anahtarı **Android Keystore** içinde üretilir
 * ve oradan asla çıkmaz — donanım destekli cihazlarda (çoğu modern
 * telefon) güvenli öğede (TEE/StrongBox) tutulur. Uygulama anahtarın
 * kendisine erişemez, yalnızca "şunu şifrele/çöz" diyebilir.
 *
 * Sonuç: veri dosyası kopyalansa bile başka cihazda çözülemez.
 *
 * ── Neden `EncryptedSharedPreferences` kullanılmadı ──
 * androidx.security-crypto kütüphanesi tam da bunu yapıyor ama:
 *   · +300 KB APK (öneri 13'te 7,9 MB attık, geri koymanın anlamı yok)
 *   · Tüm prefs dosyasını sarıyor; bizde yalnız 3-4 anahtar hassas
 *   · 1.0.0 sürümü 2021'den beri güncellenmedi, 1.1.0 hâlâ alpha
 * Doğrudan Keystore kullanmak 120 satır ve tam denetim veriyor.
 *
 * ── Geriye dönük uyum ──
 * Eski XOR biçimiyle kaydedilmiş anahtarlar ilk okumada otomatik
 * olarak yeni biçime taşınıyor ([tasi]). Kullanıcı hiçbir şey fark
 * etmiyor, anahtarını yeniden girmesi gerekmiyor.
 *
 * ── Biçim ──
 *     GAK2|<base64 iv>|<base64 şifreli>
 * Önek sayesinde eski ve yeni biçim ayırt ediliyor.
 */
object AnahtarKasa {

    private const val TAG = "AnahtarKasa"

    private const val KEYSTORE = "AndroidKeyStore"
    private const val ANAHTAR_ADI = "gunlukasistan_api_key_v1"
    private const val ONEK = "GAK2"
    private const val AYRAC = "|"

    /** GCM etiket uzunluğu (bit). 128 önerilen değer. */
    private const val ETIKET_BIT = 128

    // ══════════════════════════════════════════════════════════
    // Genel arayüz
    // ══════════════════════════════════════════════════════════

    /**
     * Metni şifreler. Başarısız olursa **eski XOR yöntemine düşer** —
     * anahtarın kaybolmasındansa zayıf korunması yeğdir.
     */
    fun sifrele(acikMetin: String): String {
        if (acikMetin.isBlank()) return ""
        return runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, anahtariAlVeyaUret())
            val iv = cipher.iv
            val sifreli = cipher.doFinal(acikMetin.toByteArray(Charsets.UTF_8))
            ONEK + AYRAC +
                Base64.encodeToString(iv, Base64.NO_WRAP) + AYRAC +
                Base64.encodeToString(sifreli, Base64.NO_WRAP)
        }.getOrElse {
            android.util.Log.w(TAG, "Keystore şifreleme başarısız, eski yönteme düşülüyor", it)
            eskiXor(acikMetin, coz = false)
        }
    }

    /**
     * Şifreli metni çözer.
     *
     * Hem yeni (`GAK2|...`) hem eski (düz Base64+XOR) biçimi anlıyor.
     */
    fun coz(saklanan: String): String {
        if (saklanan.isBlank()) return ""
        // Yeni biçim mi?
        if (!saklanan.startsWith(ONEK + AYRAC)) {
            // Eski XOR biçimi
            return eskiXor(saklanan, coz = true)
        }
        return runCatching {
            val parcalar = saklanan.split(AYRAC)
            if (parcalar.size != 3) return ""
            val iv = Base64.decode(parcalar[1], Base64.NO_WRAP)
            val sifreli = Base64.decode(parcalar[2], Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                anahtariAlVeyaUret(),
                GCMParameterSpec(ETIKET_BIT, iv)
            )
            String(cipher.doFinal(sifreli), Charsets.UTF_8)
        }.getOrElse {
            // Keystore anahtarı silinmiş olabilir (kullanıcı ekran
            // kilidini kaldırdıysa bazı cihazlarda olur). Bu durumda
            // anahtar kurtarılamaz; kullanıcıdan yeniden istenmeli.
            android.util.Log.w(TAG, "Çözme başarısız — anahtar yeniden girilmeli", it)
            ""
        }
    }

    /** Saklanan değer yeni biçimde mi? (taşıma gerekip gerekmediğini söyler) */
    fun yeniBicimMi(saklanan: String): Boolean = saklanan.startsWith(ONEK + AYRAC)

    /**
     * Keystore gerçekten kullanılabiliyor mu?
     *
     * Ayarlar ekranında "Anahtarların donanım korumalı" bilgisini
     * göstermek için. Bazı eski/özel ROM'larda Keystore bozuk olabiliyor.
     */
    fun kullanilabilirMi(): Boolean = runCatching {
        anahtariAlVeyaUret()
        true
    }.getOrDefault(false)

    /**
     * Anahtar donanım destekli güvenli öğede mi tutuluyor?
     * (TEE veya StrongBox — yazılım anahtarından çok daha güvenli)
     */
    fun donanimKorumaliMi(context: Context): Boolean = runCatching {
        if (Build.VERSION.SDK_INT < 23) return false
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        val giris = ks.getEntry(ANAHTAR_ADI, null) as? KeyStore.SecretKeyEntry
            ?: return false
        val fabrika = javax.crypto.SecretKeyFactory.getInstance(
            giris.secretKey.algorithm, KEYSTORE
        )
        val bilgi = fabrika.getKeySpec(
            giris.secretKey,
            android.security.keystore.KeyInfo::class.java
        ) as android.security.keystore.KeyInfo
        if (Build.VERSION.SDK_INT >= 31) {
            bilgi.securityLevel != android.security.keystore.KeyProperties.SECURITY_LEVEL_SOFTWARE
        } else {
            @Suppress("DEPRECATION")
            bilgi.isInsideSecureHardware
        }
    }.getOrDefault(false)

    // ══════════════════════════════════════════════════════════
    // Keystore anahtarı
    // ══════════════════════════════════════════════════════════

    private fun anahtariAlVeyaUret(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (ks.getEntry(ANAHTAR_ADI, null) as? KeyStore.SecretKeyEntry)?.let {
            return it.secretKey
        }

        val uretici = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, KEYSTORE
        )
        val ozellikler = KeyGenParameterSpec.Builder(
            ANAHTAR_ADI,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            // Kullanıcı kimlik doğrulaması İSTEMİYORUZ: arka planda
            // (alarm, widget, bildirim) da anahtara erişmek gerekiyor.
            // Ekran kilidi arkasına alsaydık sayaç bildirimi AI'ya
            // erişemezdi.
            .setUserAuthenticationRequired(false)
            .build()

        uretici.init(ozellikler)
        return uretici.generateKey()
    }

    // ══════════════════════════════════════════════════════════
    // Eski biçim (geriye dönük uyum)
    // ══════════════════════════════════════════════════════════

    /**
     * v8.7 ve öncesindeki XOR gizlemesi.
     *
     * Yalnızca ESKİ kayıtları okumak ve Keystore hiç çalışmazsa
     * yedek olarak kullanmak için duruyor. Yeni kayıtlar her zaman
     * Keystore ile yapılıyor.
     */
    private fun eskiXor(metin: String, coz: Boolean): String = runCatching {
        val baytlar = if (coz) {
            Base64.decode(metin, Base64.NO_WRAP)
        } else {
            metin.toByteArray(Charsets.UTF_8)
        }
        val tuz = "gunlukasistan".toByteArray(Charsets.UTF_8)
        val cikti = ByteArray(baytlar.size) { i ->
            (baytlar[i].toInt() xor tuz[i % tuz.size].toInt()).toByte()
        }
        if (coz) String(cikti, Charsets.UTF_8)
        else Base64.encodeToString(cikti, Base64.NO_WRAP)
    }.getOrDefault("")
}
