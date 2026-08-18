package com.gunlukasistan.app

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

/**
 * v8.9 — Hedefli liste güncellemesi (öneri 17).
 *
 * ══════════════════════════════════════════════════════════════════
 * ÖLÇÜLEN SORUN
 * ══════════════════════════════════════════════════════════════════
 * Kod tabanında **24 × `notifyDataSetChanged()`** var, **0 × DiffUtil**.
 *
 * `notifyDataSetChanged()` RecyclerView'a "her şey değişti" diyor:
 *   · Görünen tüm satırlar yeniden bağlanıyor (onBindViewHolder)
 *   · Görünüm geri dönüşümü devre dışı kalıyor
 *   · Kaydırma konumu ve animasyonlar kayboluyor
 *   · Tek bir görev işaretlendiğinde 200 satırlık liste baştan çiziliyor
 *
 * Kullanıcı için sonucu: bir kutucuğa dokununca listenin "sıçraması".
 *
 * ══════════════════════════════════════════════════════════════════
 * NEDEN `ListAdapter`'A GEÇİLMEDİ
 * ══════════════════════════════════════════════════════════════════
 * Doğru çözüm `ListAdapter` + `submitList()`. Ama mevcut adapter'lar
 * dıştaki listeye **referansla** bağlı:
 *
 *     private val tasks = mutableListOf<Store.Task>()
 *     adapter = TasksAdapter(items = tasks, ...)   // aynı nesne
 *     ...
 *     tasks.clear(); tasks.addAll(yeni)            // yerinde değişiyor
 *
 * `ListAdapter`'a geçmek `TasksFragment` (1315 satır) ve
 * `TopicsFragment` (1294 satır) içindeki tüm veri akışını yeniden
 * yazmayı gerektirir. Faz 2'nin amacı hız kazanmak, mimari kumar
 * oynamak değil.
 *
 * ── Seçilen yol ──
 * Liste güncellenmeden ÖNCE eski hâlin kopyası alınıyor, sonra
 * `DiffUtil` iki hâli karşılaştırıp yalnızca değişen satırları
 * bildiriyor. Adapter'lara dokunmuyoruz; yalnız
 * `notifyDataSetChanged()` çağrısı `ListeFark.uygula(...)` oluyor.
 *
 * Kazanç: aynı görsel sonuç, yalnız değişen satır çiziliyor, üstelik
 * ekleme/silme animasyonları bedava geliyor.
 */
object ListeFark {

    private const val TAG = "ListeFark"

    /**
     * Eski ve yeni liste arasındaki farkı RecyclerView'a bildirir.
     *
     * ```
     * val eski = tasks.toList()          // güncellemeden ÖNCE
     * tasks.clear(); tasks.addAll(yeni)
     * ListeFark.uygula(adapter, eski, tasks, { a, b -> a.id == b.id })
     * ```
     *
     * @param ayniOge iki öğe **aynı varlık** mı (genellikle id karşılaştırması)
     * @param ayniIcerik içerik de aynı mı — null ise `equals` kullanılır
     *   (data class'larda doğru sonuç verir)
     */
    fun <T> uygula(
        adapter: RecyclerView.Adapter<*>?,
        eski: List<T>,
        yeni: List<T>,
        ayniOge: (T, T) -> Boolean,
        ayniIcerik: ((T, T) -> Boolean)? = null
    ) {
        adapter ?: return
        runCatching {
            // Çok büyük listelerde DiffUtil'in kendisi pahalı olabilir.
            // 1000 öğe üstünde tam yenileme daha ucuz.
            if (eski.size > 1000 || yeni.size > 1000) {
                adapter.notifyDataSetChanged()
                return
            }
            // İlk yükleme: eski liste boşsa fark hesaplamanın anlamı yok
            if (eski.isEmpty()) {
                adapter.notifyDataSetChanged()
                return
            }

            val sonuc = DiffUtil.calculateDiff(
                object : DiffUtil.Callback() {
                    override fun getOldListSize() = eski.size
                    override fun getNewListSize() = yeni.size

                    override fun areItemsTheSame(e: Int, y: Int): Boolean =
                        ayniOge(eski[e], yeni[y])

                    override fun areContentsTheSame(e: Int, y: Int): Boolean =
                        ayniIcerik?.invoke(eski[e], yeni[y]) ?: (eski[e] == yeni[y])
                },
                // detectMoves = true: taşınan öğeler sil+ekle yerine
                // taşıma olarak bildirilir. Sıralama değişen listelerde
                // (görevler tamamlanınca alta iner) çok daha akıcı.
                true
            )
            sonuc.dispatchUpdatesTo(adapter)
        }.onFailure {
            // Fark hesaplama başarısız olursa tam yenilemeye düş —
            // liste asla bozuk kalmasın
            android.util.Log.w(TAG, "DiffUtil başarısız, tam yenileme", it)
            runCatching { adapter.notifyDataSetChanged() }
        }
    }

    // ══════════════════════════════════════════════════════════
    // Tür bazlı kısayollar — çağrı yerlerini kısaltmak için
    // ══════════════════════════════════════════════════════════

    fun gorevler(
        adapter: RecyclerView.Adapter<*>?,
        eski: List<Store.Task>,
        yeni: List<Store.Task>
    ) = uygula(
        adapter, eski, yeni,
        ayniOge = { a, b -> a.id == b.id },
        // Task bir data class ama `done` gibi var alanları var;
        // equals tüm alanları karşılaştırıyor — doğru davranış.
        ayniIcerik = { a, b -> a == b }
    )

    fun notlar(
        adapter: RecyclerView.Adapter<*>?,
        eski: List<Store.Note>,
        yeni: List<Store.Note>
    ) = uygula(
        adapter, eski, yeni,
        ayniOge = { a, b -> a.id == b.id }
    )

    fun konular(
        adapter: RecyclerView.Adapter<*>?,
        eski: List<Store.Topic>,
        yeni: List<Store.Topic>
    ) = uygula(
        adapter, eski, yeni,
        ayniOge = { a, b -> a.id == b.id },
        // Topic.items bir MutableList; data class equals'ı listeyi de
        // karşılaştırıyor, yani alt madde değişimi de yakalanıyor.
        ayniIcerik = { a, b -> a.title == b.title && a.items == b.items }
    )

    fun aliskanliklar(
        adapter: RecyclerView.Adapter<*>?,
        eski: List<Store.Habit>,
        yeni: List<Store.Habit>
    ) = uygula(
        adapter, eski, yeni,
        ayniOge = { a, b -> a.id == b.id }
    )
}
