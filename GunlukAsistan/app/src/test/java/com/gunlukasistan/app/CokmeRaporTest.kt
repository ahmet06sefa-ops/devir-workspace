package com.gunlukasistan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v9.8 — Çökme raporu testleri (öneri 49).
 *
 * Asıl test edilen: **imza** mantığı. Aynı hatanın tekrar ettiğini
 * anlamak bu özelliğin can damarı. İmza çok gevşek olursa farklı
 * hatalar birleşir, çok katı olursa aynı hata her seferinde yeni
 * sayılır ve "5 kez oldu" bilgisi hiç çıkmaz.
 */
class CokmeRaporTest {

    private fun kayit(tur: String, iz: String, zaman: Long = 1000L) =
        CokmeRapor.Kayit(
            zaman = zaman, parca = "main", tur = tur,
            mesaj = "", iz = iz, surumKodu = 154, surumAdi = "9.8"
        )

    private val izA = """
        java.lang.NullPointerException: null cannot be cast
            at com.gunlukasistan.app.MainActivity.onCreate(MainActivity.kt:120)
            at android.app.Activity.performCreate(Activity.java:8000)
            at android.app.Instrumentation.callActivityOnCreate(Instrumentation.java:1300)
            at android.app.ActivityThread.performLaunchActivity(ActivityThread.java:3600)
    """.trimIndent()

    private val izAFarkliSatir = """
        java.lang.NullPointerException: null cannot be cast
            at com.gunlukasistan.app.MainActivity.onCreate(MainActivity.kt:120)
            at android.app.Activity.performCreate(Activity.java:8000)
            at android.app.Instrumentation.callActivityOnCreate(Instrumentation.java:1300)
            at android.app.ActivityThread.handleLaunchActivity(ActivityThread.java:9999)
    """.trimIndent()

    private val izB = """
        java.lang.IllegalStateException: Fragment not attached
            at com.gunlukasistan.app.TasksFragment.yenile(TasksFragment.kt:88)
            at com.gunlukasistan.app.TasksFragment.onResume(TasksFragment.kt:40)
            at androidx.fragment.app.Fragment.performResume(Fragment.java:3000)
    """.trimIndent()

    // ══════════════════════════════════════════════════════════
    // İmza
    // ══════════════════════════════════════════════════════════

    @Test
    fun `ayni cokme ayni imzayi verir`() {
        val a = kayit("NullPointerException", izA, 1000L)
        val b = kayit("NullPointerException", izA, 99999L)
        // Zaman farklı ama hata aynı → aynı imza
        assertEquals(a.imza, b.imza)
    }

    @Test
    fun `farkli cokme farkli imza verir`() {
        val a = kayit("NullPointerException", izA)
        val b = kayit("IllegalStateException", izB)
        assertNotEquals(a.imza, b.imza)
    }

    @Test
    fun `ilk uc kare ayniysa imza ayni kalir`() {
        // Alt katmandaki değişiklik (4. kare) aynı hatayı
        // farklı göstermemeli — yoksa "5 kez oldu" hiç çıkmaz
        val a = kayit("NullPointerException", izA)
        val b = kayit("NullPointerException", izAFarkliSatir)
        assertEquals(a.imza, b.imza)
    }

    @Test
    fun `ayni iz farkli tur farkli imza`() {
        // Tür imzanın parçası: aynı satırda farklı istisna
        // farklı sorundur
        val a = kayit("NullPointerException", izA)
        val b = kayit("ClassCastException", izA)
        assertNotEquals(a.imza, b.imza)
    }

    @Test
    fun `bos iz cokmez`() {
        val k = kayit("Exception", "")
        assertTrue(k.imza.isNotBlank())
        assertTrue(k.imza.contains("Exception"))
    }

    @Test
    fun `at satiri olmayan iz cokmez`() {
        val k = kayit("OutOfMemoryError", "java.lang.OutOfMemoryError: pthread_create failed")
        assertTrue(k.imza.isNotBlank())
    }

    @Test
    fun `imza tur ile basliyor`() {
        val k = kayit("NullPointerException", izA)
        assertTrue(k.imza.startsWith("NullPointerException"))
    }

    // ══════════════════════════════════════════════════════════
    // Tür çıkarma
    // ══════════════════════════════════════════════════════════

    @Test
    fun `ilk satirdan tur cikarilir`() {
        assertEquals(
            "NullPointerException",
            CokmeRapor.ilkSatirdanTur("java.lang.NullPointerException: bir şey null")
        )
    }

    @Test
    fun `mesajsiz satirdan tur cikarilir`() {
        assertEquals(
            "IllegalStateException",
            CokmeRapor.ilkSatirdanTur("java.lang.IllegalStateException")
        )
    }

    @Test
    fun `paket adi olmayan tur cikarilir`() {
        assertEquals("MyException", CokmeRapor.ilkSatirdanTur("MyException: oops"))
    }

    @Test
    fun `bos girdi varsayilan doner`() {
        assertEquals("Exception", CokmeRapor.ilkSatirdanTur(""))
    }

    @Test
    fun `uygulama istisnasi dogru okunur`() {
        assertEquals(
            "ActivityNotFoundException",
            CokmeRapor.ilkSatirdanTur(
                "android.content.ActivityNotFoundException: Unable to find explicit activity"
            )
        )
    }

    // ══════════════════════════════════════════════════════════
    // Kayıt alanları
    // ══════════════════════════════════════════════════════════

    @Test
    fun `kayit alanlari korunur`() {
        val k = CokmeRapor.Kayit(
            zaman = 5000L, parca = "worker-1", tur = "IOException",
            mesaj = "disk dolu", iz = izB, surumKodu = 154, surumAdi = "9.8"
        )
        assertEquals(5000L, k.zaman)
        assertEquals("worker-1", k.parca)
        assertEquals("disk dolu", k.mesaj)
        assertEquals(154, k.surumKodu)
    }
}
