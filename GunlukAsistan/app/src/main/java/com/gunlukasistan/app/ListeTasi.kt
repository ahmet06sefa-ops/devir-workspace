package com.gunlukasistan.app

/**
 * v10.44 — Kullanıcı maddesi #4: listede tek kademe taşıma (saf, JVM testli).
 *
 * Alt madde satırındaki ▲▼ düğmeleri bu iki fonksiyona bağlanır; sınır
 * dışı isteklerde liste aynen kalır ve false döner (tıklama yutulur).
 */
object ListeTasi {

    /** i'nci öğeyi bir üste taşır; ilk öğede ya da geçersizde hiçbir şey yapmaz. */
    fun <T> yukariTasi(liste: MutableList<T>, i: Int): Boolean {
        if (i !in 1..liste.lastIndex) return false
        liste.add(i - 1, liste.removeAt(i))
        return true
    }

    /** i'nci öğeyi bir alta taşır; son öğede ya da geçersizde hiçbir şey yapmaz. */
    fun <T> asagiTasi(liste: MutableList<T>, i: Int): Boolean {
        if (i !in 0 until liste.lastIndex) return false
        liste.add(i + 1, liste.removeAt(i))
        return true
    }
}
