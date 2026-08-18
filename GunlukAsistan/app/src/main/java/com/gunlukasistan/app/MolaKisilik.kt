package com.gunlukasistan.app

/**
 * v10.4 · Öneri A7 — Mola kişiliği.
 *
 * ── Dürüst not ──
 * Pomodoro mola evreleri v7.94'ten beri var (`Pomodoro.EVRE_*`),
 * mola bildirimi de "Mola başladı · 5 dk" diyordu. Ama mola
 * süresince kullanıcıya hiçbir şey **önerilmiyordu** — 5 dakika
 * boyunca telefona bakıp kalma riski aynen duruyor. Bu sınıf mola
 * bildirimine dönüşümlü, kısa ve uygulanabilir öneriler ekler.
 *
 * ── Neden rotasyon ──
 * Her molada aynı "su iç" cümlesi üç günde görünmez olur (bildirim
 * körlüğü). Öneri, tur numarasına göre deterministik döner; aynı
 * turun bildirimi tekrar kurulursa metin değişmez (titreme yok).
 *
 * ── Saf bölge ──
 * Tamamı saf: context yok, birim testli.
 */
object MolaKisilik {

    data class Oneri(val emoji: String, val metin: String)

    private val KISA = listOf(
        Oneri("💧", "Bir bardak su iç — odak susuz düşer"),
        Oneri("👀", "Gözlerini uzaktaki bir noktaya sabitle (20-20-20)"),
        Oneri("🧍", "Ayağa kalk, omuzlarını geriye yuvarla"),
        Oneri("🌬️", "4 saniye al, 6 saniye ver — üç tur"),
        Oneri("🖐️", "Bileklerini ve parmaklarını esnet")
    )

    private val UZUN = listOf(
        Oneri("🚶", "Kısa bir yürüyüş — pencereden dışarı bakarak"),
        Oneri("🍎", "Hafif bir şeyler atıştır; ağır yemek odağı düşürür"),
        Oneri("🧘", "Gözlerin kapalı iki dakika nefes say"),
        Oneri("📝", "Kafanda kalanları kâğıda boşalt, sonra bırak")
    )

    fun oneri(tur: Int, uzunMola: Boolean): Oneri {
        val havuz = if (uzunMola) UZUN else KISA
        val indeks = ((tur % havuz.size) + havuz.size) % havuz.size
        return havuz[indeks]
    }

    /** Bildirim gövdesi: "5 dk mola · 💧 Bir bardak su iç…" */
    fun govde(temel: String, tur: Int, uzunMola: Boolean): String {
        val o = oneri(tur, uzunMola)
        return "$temel · ${o.emoji} ${o.metin}"
    }
}
