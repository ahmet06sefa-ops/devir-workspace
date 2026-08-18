package com.gunlukasistan.app.veri

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction

/**
 * v8.1 — Notun veritabanı karşılığı (öneri 3).
 *
 * ── Neden Room'a taşınıyor ──
 * v7.76'da yalnızca `Task` taşınmıştı. Notlar hâlâ JSON'du: her
 * `loadNotes()` çağrısı tüm listeyi baştan ayrıştırıyordu. Arama
 * ekranı, widget'lar ve Bugün ekranı bunu sık çağırıyor; not sayısı
 * arttıkça her ekran açılışı yavaşlıyordu.
 *
 * ── `icerik` neden indekslenmedi ──
 * Not gövdesi uzun olabiliyor; tam metin araması için FTS tablosu
 * gerekir. Arama şu an bellekte yapılıyor ve not sayısı (yüzler
 * mertebesinde) bunun için yeterince küçük. FTS eklemek şemayı
 * karmaşıklaştırır, kazanç ölçülebilir değil.
 */
@Entity(
    tableName = "notlar",
    indices = [Index(value = ["olusturuldu"])]
)
data class NotVarlik(
    @PrimaryKey val id: Long,
    val baslik: String,
    val icerik: String,
    val olusturuldu: Long,
    val gorsel: String
)

@Dao
interface NotDao {

    @Query("SELECT * FROM notlar ORDER BY olusturuldu DESC")
    fun hepsi(): List<NotVarlik>

    @Query("SELECT COUNT(*) FROM notlar")
    fun sayi(): Int

    /**
     * Listeyi olduğu gibi değiştirir.
     *
     * `Store.saveNotes(liste)` sözleşmesi "verilen liste = tüm notlar"
     * olduğu için sil-ve-yaz yapılıyor. Tek işlemde çalıştığı için
     * yarıda kesilme riski yok.
     */
    @Transaction
    fun tumunuDegistir(kayitlar: List<NotVarlik>) {
        temizle()
        kayitlar.chunked(200).forEach { topluEkle(it) }
    }

    @Query("DELETE FROM notlar")
    fun temizle()

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    fun topluEkle(kayitlar: List<NotVarlik>)
}
