package com.gunlukasistan.app.veri

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction

/**
 * v7.76 — Görevin veritabanı karşılığı.
 *
 * ── Neden Room ──
 * Eskiden her `loadTasks()` çağrısı **tüm görev listesini** JSON'dan baştan
 * ayrıştırıyordu. Tek ekran çiziminde bu 5-10 kez olabiliyordu. Veri
 * büyüdükçe maliyet doğrusal artıyordu. v7.61'de önbellekle yamandı ama
 * kök neden duruyordu.
 *
 * Room ile: indeksli sorgu, kısmi okuma, satır bazlı güncelleme.
 *
 * ── `adimlar` neden JSON metin ──
 * Alt adımlar ayrı tabloya alınabilirdi ama bu her okumada JOIN gerektirir
 * ve `Store.Task` API'sini bozar. Liste kısa (birkaç madde) olduğu için
 * tek sütunda JSON olarak saklamak hem basit hem yeterince hızlı.
 */
@Entity(
    tableName = "gorevler",
    indices = [
        // Ana liste sorgusu: arşivlenmemişler + bitiş tarihine göre
        Index(value = ["arsiv", "bitti"]),
        Index(value = ["sonTarih"])
    ]
)
data class GorevVarlik(
    @PrimaryKey val id: Long,
    val metin: String,
    val bitti: Boolean,
    val olusturuldu: Long,
    val sonTarih: Long,
    val tekrar: String,
    val tekrarBitis: Long,
    val yapildi: Int,
    val etiket: String,
    val arsiv: Boolean,
    val arsivZaman: Long,
    /** Alt adımlar — JSON dizi metni. */
    val adimlarJson: String
)

@Dao
interface GorevDao {

    @Query("SELECT * FROM gorevler")
    fun hepsi(): List<GorevVarlik>

    @Query("SELECT COUNT(*) FROM gorevler")
    fun sayi(): Int

    /** Ana listede görünenler — arşivlenmemişler. */
    @Query("SELECT * FROM gorevler WHERE arsiv = 0")
    fun aktifler(): List<GorevVarlik>

    @Query("SELECT COUNT(*) FROM gorevler WHERE bitti = 1 AND arsiv = 0")
    fun bitenSayisi(): Int

    /**
     * Listeyi olduğu gibi değiştirir.
     *
     * `Store.saveTasks(liste)` sözleşmesi "verilen liste = tüm görevler"
     * olduğu için sil-ve-yaz yapılıyor. Tek işlemde (transaction)
     * çalıştığı için yarıda kesilme riski yok.
     */
    @Transaction
    fun tumunuDegistir(kayitlar: List<GorevVarlik>) {
        temizle()
        // Büyük listelerde tek seferde eklemek bellek zorlar — parçala
        kayitlar.chunked(200).forEach { topluEkle(it) }
    }

    @Query("DELETE FROM gorevler")
    fun temizle()

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    fun topluEkle(kayitlar: List<GorevVarlik>)
}
