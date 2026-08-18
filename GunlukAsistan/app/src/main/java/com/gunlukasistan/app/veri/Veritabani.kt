package com.gunlukasistan.app.veri

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Uygulama veritabanı.
 *
 * ── v7.76: yalnızca görevler ──
 * Store 2500 satır ve ~190 çağrı noktası tarafından kullanılıyordu.
 * Hepsini tek seferde taşımak veri kaybı riski taşırdı; en sık okunan
 * tablo (görevler) önce taşındı.
 *
 * ── v8.1: notlar eklendi (öneri 3) ──
 * Şema sürümü 1 → 2. **Gerçek migration yazıldı.**
 *
 * `fallbackToDestructiveMigration` kaldırıldı: o ayar şema değiştiğinde
 * veritabanını **siliyordu**. Görevler v7.76'dan beri Room'da yaşıyor;
 * JSON gölge kopyası olsa bile silinip yeniden doldurma sırasında bir
 * hata olsa kullanıcının tüm görevleri giderdi. Migration ile mevcut
 * `gorevler` tablosuna hiç dokunulmuyor, yalnızca yeni tablo ekleniyor.
 */
@Database(
    entities = [GorevVarlik::class, NotVarlik::class],
    version = 2,
    exportSchema = false
)
abstract class Veritabani : RoomDatabase() {

    abstract fun gorevDao(): GorevDao

    abstract fun notDao(): NotDao

    companion object {
        @Volatile
        private var ornek: Veritabani? = null

        /**
         * v8.1 — Sürüm 1'den 2'ye geçiş.
         *
         * Yalnızca `notlar` tablosu ekleniyor. `gorevler` tablosuna
         * dokunulmadığı için mevcut görev verisi olduğu gibi korunuyor.
         *
         * Sütun tipleri ve NOT NULL kısıtları [NotVarlik] ile birebir
         * eşleşmeli; aksi halde Room açılışta şema doğrulama hatası verir.
         */
        private val GECIS_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `notlar` (
                        `id` INTEGER NOT NULL,
                        `baslik` TEXT NOT NULL,
                        `icerik` TEXT NOT NULL,
                        `olusturuldu` INTEGER NOT NULL,
                        `gorsel` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_notlar_olusturuldu` " +
                        "ON `notlar` (`olusturuldu`)"
                )
            }
        }

        fun al(context: Context): Veritabani =
            ornek ?: synchronized(this) {
                ornek ?: Room.databaseBuilder(
                    context.applicationContext,
                    Veritabani::class.java,
                    "gunluk_asistan.db"
                )
                    // v8.1: gerçek geçiş — veri silinmiyor
                    .addMigrations(GECIS_1_2)
                    // Store çağrıları ana iş parçacığından geliyor.
                    // Asıl kazanç JSON ayrıştırmasının kalkması; sorgular
                    // indeksli ve milisaniye altı.
                    .allowMainThreadQueries()
                    .build()
                    .also { ornek = it }
            }
    }
}
