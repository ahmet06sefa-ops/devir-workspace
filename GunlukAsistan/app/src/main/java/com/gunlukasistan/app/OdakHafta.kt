package com.gunlukasistan.app

/**
 * v10.27 (öneri #76) — Haftalık odak hedefinin saf kararları.
 *
 * Günlük hedef zaten ayarlanabilir; haftalık hedef ayrı bir ayar yerine
 * günlüğün 7 katı olarak HESAPLANIR. Sıfır yeni ayar = sıfır ek akış;
 * kullanıcı günlük hedefini değiştirirse haftalık da ona uyar.
 * (Özel haftalık hedef ileri bir dalgada düşünülebilir — dürüst kayıt.)
 */
object OdakHafta {

    /** Haftalık hedef: günlük hedefin 7 katı. */
    fun haftalikHedef(gunlukHedef: Int): Int = gunlukHedef.coerceAtLeast(0) * 7

    /** İlerleme yüzdesi 0..100; hedef 0 iken 0 (bölme koruması). */
    fun yuzde(haftaDk: Int, hedef: Int): Int =
        if (hedef <= 0) 0 else (haftaDk.coerceAtLeast(0) * 100 / hedef).coerceIn(0, 100)
}
