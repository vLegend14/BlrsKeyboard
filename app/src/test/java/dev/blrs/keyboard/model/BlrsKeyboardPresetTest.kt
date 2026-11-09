package dev.blrs.keyboard.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests para la generación del preset BlrsKeyboard.
 * Verifica que las 8 combinaciones de Distribución×Números×Barra
 * generen layouts con el mismo número de filas en las 3 capas.
 */
class BlrsKeyboardPresetTest {
    
    @Test
    fun testDefaultSettings() {
        val layout = BlrsKeyboardPreset.generate()
        
        // Verificar que tiene las 3 capas
        assertEquals(3, layout.getLayerNames().size)
        assertTrue(layout.getLayerNames().contains("abc"))
        assertTrue(layout.getLayerNames().contains("sym"))
        assertTrue(layout.getLayerNames().contains("sym2"))
        
        // Verificar que tiene features habilitadas
        assertTrue(layout.feat.ctrlLock)
        assertTrue(layout.feat.spaceCursor)
        assertTrue(layout.feat.pairs)
        
        // Verificar que tiene holds
        assertTrue(layout.holds.isNotEmpty())
        assertTrue(layout.holds.containsKey("a"))
        assertTrue(layout.holds.containsKey("e"))
        
        // Verificar que tiene lp (long-press actions)
        assertTrue(layout.lp.isNotEmpty())
        assertEquals("@emoji", layout.lp[","])
        assertEquals("@settings", layout.lp["."])
    }
    
    @Test
    fun testAllCombinations_HaveSameRowCountPerLayer() {
        val layouts = mutableListOf<KeyboardLayout>()
        
        // Probar las 8 combinaciones: 2 layouts × 2 numbers × 2 bar
        for (layoutType in listOf(BlrsKeyboardPreset.LayoutType.AZERTY, BlrsKeyboardPreset.LayoutType.QWERTY)) {
            for (showNumbers in listOf(true, false)) {
                for (showBar in listOf(true, false)) {
                    val layout = BlrsKeyboardPreset.generate(layoutType, showNumbers, showBar)
                    layouts.add(layout)
                    
                    // Verificar que todas las capas tienen el mismo número de filas
                    val abcRows = layout.getLayer("abc")?.rowCount() ?: 0
                    val symRows = layout.getLayer("sym")?.rowCount() ?: 0
                    val sym2Rows = layout.getLayer("sym2")?.rowCount() ?: 0
                    
                    assertEquals(
                        "Rows mismatch for $layoutType, numbers=$showNumbers, bar=$showBar: " +
                        "abc=$abcRows, sym=$symRows, sym2=$sym2Rows",
                        abcRows, symRows
                    )
                    assertEquals(
                        "Rows mismatch for $layoutType, numbers=$showNumbers, bar=$showBar: " +
                        "abc=$abcRows, sym=$sym2Rows",
                        abcRows, sym2Rows
                    )
                }
            }
        }
        
        assertEquals(8, layouts.size)
    }
    
    @Test
    fun testBarRowCount() {
        // Con barra visible
        val withBar = BlrsKeyboardPreset.generate(showBar = true)
        assertEquals(2, withBar.bar)
        
        // Sin barra
        val withoutBar = BlrsKeyboardPreset.generate(showBar = false)
        assertEquals(0, withoutBar.bar)
    }
    
    @Test
    fun testNumbersInTopRow() {
        // Con números visibles
        val withNumbers = BlrsKeyboardPreset.generate(showNumbers = true)
        val abcLayer = withNumbers.getLayer("abc")
        val topRow = abcLayer?.getRow(2) // After toolbar rows
        
        // Verificar que la fila contiene dígitos
        assertNotNull(topRow)
        val labels = topRow?.keys?.map { it.label } ?: emptyList()
        assertTrue(labels.contains("1"))
        assertTrue(labels.contains("0"))
    }
    
    @Test
    fun testAzertyLayout() {
        val layout = BlrsKeyboardPreset.generate(
            layout = BlrsKeyboardPreset.LayoutType.AZERTY
        )
        
        val abcLayer = layout.getLayer("abc")
        assertNotNull(abcLayer)
        
        // Verificar que la primera fila es a-z-e-r-t-y...
        val firstTextRow = abcLayer?.getRow(2) // After toolbar + numbers
        assertNotNull(firstTextRow)
        
        val labels = firstTextRow?.keys?.map { it.label } ?: emptyList()
        assertTrue(labels.contains("a"))
        assertTrue(labels.contains("z"))
        assertTrue(labels.contains("e"))
        assertTrue(labels.contains("r"))
        assertTrue(labels.contains("t"))
    }
    
    @Test
    fun testQwertyLayout() {
        val layout = BlrsKeyboardPreset.generate(
            layout = BlrsKeyboardPreset.LayoutType.QWERTY
        )
        
        val abcLayer = layout.getLayer("abc")
        assertNotNull(abcLayer)
        
        // Verificar que la primera fila es q-w-e-r-t-y...
        val firstTextRow = abcLayer?.getRow(2) // After toolbar + numbers
        assertNotNull(firstTextRow)
        
        val labels = firstTextRow?.keys?.map { it.label } ?: emptyList()
        assertTrue(labels.contains("q"))
        assertTrue(labels.contains("w"))
        assertTrue(labels.contains("e"))
        assertTrue(labels.contains("r"))
        assertTrue(labels.contains("t"))
    }
    
    @Test
    fun testMacrosInLp() {
        val layout = BlrsKeyboardPreset.generate()
        
        // Verificar macros
        assertEquals("../", layout.lp["/"])
        assertEquals("--", layout.lp["-"])
        assertEquals("&&", layout.lp["&"])
        assertEquals(" | ", layout.lp["|"])
        assertEquals("~/", layout.lp["@home"])
    }
    
    @Test
    fun testHoldsHaveVariants() {
        val layout = BlrsKeyboardPreset.generate()
        
        // Verificar variantes para vocales con acentos
        assertTrue(layout.holds["a"]!!.contains("à"))
        assertTrue(layout.holds["e"]!!.contains("é"))
        assertTrue(layout.holds["i"]!!.contains("î"))
        assertTrue(layout.holds["o"]!!.contains("ô"))
        assertTrue(layout.holds["u"]!!.contains("ù"))
        
        // Verificar ñ y ç
        assertTrue(layout.holds["n"]!!.contains("ñ"))
        assertTrue(layout.holds["c"]!!.contains("ç"))
    }
}
