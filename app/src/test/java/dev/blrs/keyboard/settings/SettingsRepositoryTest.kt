package dev.blrs.keyboard.settings

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests para SettingsRepository.
 * Verifica defaults y que la definición de opciones es consistente.
 */
class SettingsRepositoryTest {
    
    @Test
    fun testDefaults() {
        // Verificar defaults de las opciones
        assertEquals("azerty", SettingsRepository.Option.Layout.defaultValue)
        assertEquals(true, SettingsRepository.Option.Numbers.defaultValue)
        assertEquals("c", SettingsRepository.Option.Accent.defaultValue)
        assertEquals(true, SettingsRepository.Option.Bar.defaultValue)
        assertEquals(true, SettingsRepository.Option.Vibration.defaultValue)
        assertEquals("bluerose", SettingsRepository.Option.Theme.defaultValue)
        assertEquals(46, SettingsRepository.Option.KeyHeight.defaultValue)
    }
    
    @Test
    fun testLayoutValues() {
        val values = SettingsRepository.Option.Layout.values
        assertEquals(2, values.size)
        assertTrue(values.any { it.first == "azerty" })
        assertTrue(values.any { it.first == "qwerty" })
    }
    
    @Test
    fun testAccentValues() {
        val values = SettingsRepository.Option.Accent.values
        assertEquals(3, values.size)
        assertTrue(values.any { it.first == "c" && it.second == "Default" })
        assertTrue(values.any { it.first == "a" && it.second == "Acento" })
        assertTrue(values.any { it.first == "b" && it.second == "Acento + detalles" })
    }
    
    @Test
    fun testThemeValues() {
        val values = SettingsRepository.Option.Theme.values
        assertEquals(5, values.size)
        assertTrue(values.any { it.first == "bluerose" })
        assertTrue(values.any { it.first == "oled" })
        assertTrue(values.any { it.first == "thorn" })
        assertTrue(values.any { it.first == "amethyst" })
        assertTrue(values.any { it.first == "azure" })
    }
    
    @Test
    fun testOptionKeys() {
        // Verificar que las claves coinciden con el HTML
        assertEquals("ly", SettingsRepository.Option.Layout.key)
        assertEquals("nums", SettingsRepository.Option.Numbers.key)
        assertEquals("ac", SettingsRepository.Option.Accent.key)
        assertEquals("bar", SettingsRepository.Option.Bar.key)
        assertEquals("vib", SettingsRepository.Option.Vibration.key)
        assertEquals("theme", SettingsRepository.Option.Theme.key)
        assertEquals("kh", SettingsRepository.Option.KeyHeight.key)
    }
}
