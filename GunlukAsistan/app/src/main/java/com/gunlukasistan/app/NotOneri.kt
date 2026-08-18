package com.gunlukasistan.app

/**
 * v10.33 · Katalog #38 — başlığı boş bırakılan not için otomatik öneri
 * (saf, JVM testli): içeriğin ilk dolu satırı (liste işaretleri soyulmuş),
 * en çok 60 karakter.
 */
object NotOneri {

    fun baslik(icerik: String): String {
        val ilk = NotOlcum.satirlariAyikla(icerik).firstOrNull() ?: return ""
        return ilk.take(60)
    }
}
