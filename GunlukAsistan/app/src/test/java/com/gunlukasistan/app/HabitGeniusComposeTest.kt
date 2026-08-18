package com.gunlukasistan.app
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
/** v11.23 — HabitGenius 2. Görünümü (tek APK, Compose) koruma testleri. */
class HabitGeniusComposeTest {
    @Test fun `compose activity yuklenebilir`() { assertNotNull(Class.forName("com.gunlukasistan.app.HabitGeniusComposeActivity")) }
    @Test fun `main ve ayarlar ekrani derlenmistir`() {
        val m = Class.forName("com.gunlukasistan.app.HabitGeniusComposeKt").declaredMethods.map { it.name }
        assertTrue(m.any { it.contains("HabitGeniusMainScreen") })
        assertTrue(m.any { it.contains("HabitGeniusSettingsScreen") })
        assertTrue(m.any { it.contains("AyarKarti") })
    }
    @Test fun `compose ac yardimcisi mevcuttur`() { assertNotNull(Class.forName("com.gunlukasistan.app.HabitGeniusComposeActivity\$Companion")) }
}
