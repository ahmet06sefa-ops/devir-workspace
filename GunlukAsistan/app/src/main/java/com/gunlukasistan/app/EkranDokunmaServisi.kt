package com.gunlukasistan.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * v11.13 — Ekran dokunma yetkisi (AccessibilityService).
 *
 * Kullanıcı isteği: "Yapay zeka asistanın ekrana dokunabilme yetkisi olsun;
 * sesli komut vereyim, AI ekrandan benim için gerçekleştirsin."
 *
 * Bu servis, kullanıcı Sistem → Erişilebilirlik → "Günlük Asistan Ekran
 * Dokunma" servisini etkinleştirince ekran içeriğini okuyabilir ve ekrandaki
 * öğelere (düğme, metin) dokunup tıklayabilir, global aksiyonlar (geri, ana
 * ekran) çalıştırabilir.
 *
 * AI tarafı [EkranDokunmaMotoru] üzerinden istek gönderir; servis sırayla
 * işler. Sıra boşsa hiçbir şey yapmaz (kullanıcı gizliliği ve güvenliği).
 *
 * NOT: Bu servis kullanıcı tarafından bilinçli olarak etkinleştirilmedikçe
 * çalışmaz; izin yoksa [acikMi] false döner ve hiçbir ekran tıklaması yapılmaz.
 */
class EkranDokunmaServisi : AccessibilityService() {

    /** Son aktif servis örneği (AI isteklerini buraya gönderir). */
    companion object {
        var aktif: EkranDokunmaServisi? = null
        private val istekKuyrugu = ArrayDeque<String>()
        private val kilit = Any()

        /** AI katmanından statik erişim: servis aktif örneğini döndürür. */
        val aktiGonder: EkranDokunmaServisi? get() = aktif

        /** Statik istek gönderimi: komut|deger iletir, servis aktifse true. */
        fun istekGonderStatik(komut: String, deger: String): Boolean {
            val s = aktif ?: return false
            runCatching { s.istegiIsle("$komut|$deger") }
            return true
        }
    }

    /** Servis aktif (kullanıcı etkinleştirdi) mi? */
    fun aktifMi(): Boolean = aktif == this

    override fun onServiceConnected() {
        super.onServiceConnected()
        aktif = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Sürekli dinleme yok; yalnızca istek kuyruğunda iş varsa işle.
        synchronized(kilit) {
            if (istekKuyrugu.isEmpty()) return
            val istek = istekKuyrugu.removeFirst()
            runCatching { istegiIsle(istek) }
        }
    }

    override fun onInterrupt() {
        // Yalnız kuyruğu boşalt — AI'nın yarım isteği kalmasın.
        synchronized(kilit) { istekKuyrugu.clear() }
    }

    override fun onDestroy() {
        aktif = null
        super.onDestroy()
    }

    private fun istegiIsle(istek: String) {
        val (komut, deger) = istek.split("|", limit = 2)
        when (komut) {
            "tikla" -> ekrandaTikla(deger)
            "yaz" -> odaklanmisaYaz(deger)
            "ara" -> telefonlaAra(deger)
            "geri" -> performGlobalAction(GLOBAL_ACTION_BACK)
            "ana" -> performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    /**
     * v11.13 — Başka bir uygulamadaki ODAKLANMIŞ metin alanına yazı yazar.
     * (Erişilebilirlik yetkisi: `ACCESSIBILITY_ACTION_SET_TEXT`.) Kullanıcı
     * yazı girilecek alana önce odaklanmalı; AI metni doldurur. Boş değer
     * alanı temizler.
     */
    private fun odaklanmisaYaz(metin: String) {
        val kok = rootInActiveWindow ?: return
        // Aktif pencere içinde odaklanmış veya metin girişi olan ilk düğümü bul
        val hedef = kok.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?.takeIf { it.isEditable }
            ?: kok.findEditable()
            ?: return
        val bundle = android.os.Bundle().apply { putString(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, metin) }
        runCatching { hedef.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle) }
    }

    /**
     * v11.13 — Telefon çeviriciyi numara dolu olarak açar (`ACTION_DIAL`).
     * İzin gerektirmez; kullanıcı tek dokunuşla aramayı kendisi başlatır.
     * Döner: başlatıldıysa true.
     */
    fun telefonlaAra(numara: String): Boolean {
        val temiz = numara.replace(Regex("[^0-9+*#]"), "")
        if (temiz.isBlank()) return false
        return runCatching {
            val niyet = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$temiz")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(niyet)
            true
        }.getOrDefault(false)
    }

    private fun AccessibilityNodeInfo.findEditable(): AccessibilityNodeInfo? {
        if (this.isEditable) return this
        for (i in 0 until childCount) {
            val c = getChild(i) ?: continue
            val sonuc = c.findEditable()
            if (sonuc != null) return sonuc
        }
        return null
    }

    /** Ekrandaki görünür metin/içerik açıklamasına dokunup tıklar. */
    private fun ekrandaTikla(metin: String) {
        val kok = rootInActiveWindow ?: return
        val hedef = kok.findByText(metin) ?: return
        // Tıklanabilir değilse üst atasına çık
        var node: AccessibilityNodeInfo? = hedef
        while (node != null && !node.isClickable) {
            node = node.parent
        }
        node?.let { runCatching { it.performAction(AccessibilityNodeInfo.ACTION_CLICK) } }
    }

    private fun AccessibilityNodeInfo.findByText(aranan: String): AccessibilityNodeInfo? {
        val norm = aranan.trim().lowercase()
        if (this.text?.toString()?.lowercase()?.contains(norm) == true ||
            this.contentDescription?.toString()?.lowercase()?.contains(norm) == true
        ) {
            return this
        }
        for (i in 0 until childCount) {
            val c = getChild(i) ?: continue
            val sonuc = c.findByText(aranan)
            if (sonuc != null) return sonuc
        }
        return null
    }

    /** AI'ya "ekrana dokunma" yetkisi verilmiş ve servis aktif mi? */
    fun servisAcikMi(): Boolean = aktif == this

    /**
     * AI'nın ekranda bir öğeye dokunması için istek gönderir.
     * Biçim: "tikla|metin", "geri|", "ana|".
     * Servis aktif değilse false döner (kullanıcı etkinleştirmemiş).
     */
    fun istekGonder(komut: String, deger: String): Boolean {
        val s = aktif ?: return false
        runCatching { s.istegiIsle("$komut|$deger") }
        return true
    }

    /**
     * v11.13 — GERÇEK ekran görüntüsünü (piksel) yakalar (API 30+).
     * `takeScreenshot` cihazın erişilebilirlik ekran görüntüsü kotalarıyla
     * sınırlıdır; başarılıysa `sonuc` ile Bitmap döner, değilse null.
     * Sonucu görsel modele ([AiClient.konuOku] benzeri) göndermek AI'ya
     * bırakılır. API 30 altında ya da servis aktif değilse null döner.
     */
    fun ekranGoruntusuAl(onSonuc: (Bitmap?) -> Unit) {
        val s = aktif ?: return onSonuc(null)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return onSonuc(null)
        val yurutucu = java.util.concurrent.Executors.newSingleThreadExecutor()
        runCatching {
            s.takeScreenshot(
                android.view.Display.DEFAULT_DISPLAY,
                yurutucu,
                object : AccessibilityService.TakeScreenshotCallback {
                    override fun onSuccess(screenshot: AccessibilityService.ScreenshotResult) {
                        runCatching {
                            val hb = screenshot.hardwareBuffer
                            val bitmap = if (hb != null) {
                                Bitmap.wrapHardwareBuffer(hb, screenshot.colorSpace)
                            } else null
                            onSonuc(bitmap)
                        }
                    }
                    override fun onFailure(errorCode: Int) {
                        onSonuc(null)
                    }
                }
            )
        }.onFailure { onSonuc(null) }
    }
}
