package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * v10.1 — Sayaç geri kurulum kararı testleri.
 *
 * ── Neden test ──
 * v10.0'da telefon yeniden başlayınca/uygulama güncellenince çalışan
 * sayaç sessizce ölüyordu: bitiş alarmı, tazeleme zinciri ve panel
 * bildirimi geri kurulmuyordu. Kullanıcı sayaç bitince haber
 * alamıyordu ve mini zamanlayıcı panelden kayboluyordu.
 *
 * [SayacGeriKur.karar] bu kurtarmanın saf mantığı: duruma göre ne
 * yapılacağını söyler. Yanlış karar iki şekilde zarar verir:
 *   · YOK denirse → kullanıcı sayacın bitişini kaçırır (asıl hata)
 *   · BITIR denip süre bitmemişse → erken bitiş sesi çalar (yalan alarm)
 */
class SayacGeriKurTest {

    @Test
    fun `calismayan sayac icin hicbir sey yapilmaz`() {
        assertEquals(
            SayacGeriKur.Eylem.YOK,
            SayacGeriKur.karar(calisiyor = false, bitti = false)
        )
        // Çalışmıyorsa "bitti" bayrağı geçerli değildir — isFinished
        // yalnız çalışırken okunur; karar yine de sağlam durmalı.
        assertEquals(
            SayacGeriKur.Eylem.YOK,
            SayacGeriKur.karar(calisiyor = false, bitti = true)
        )
    }

    @Test
    fun `calisan ve devam eden sayac altyapisiyla kurulur`() {
        assertEquals(
            SayacGeriKur.Eylem.KUR,
            SayacGeriKur.karar(calisiyor = true, bitti = false)
        )
    }

    @Test
    fun `kapaliyken biten sayacin eksik bitisi teslim edilir`() {
        // Android yeniden başlatmada/güncellemede alarmları siler.
        // Süre o arada bittiyse bitiş akışı (ses + odak kaydı + döngü)
        // geri kurulum anında çalışmalı — aksi hâlde asla gelmezdi.
        assertEquals(
            SayacGeriKur.Eylem.BITIR,
            SayacGeriKur.karar(calisiyor = true, bitti = true)
        )
    }
}
