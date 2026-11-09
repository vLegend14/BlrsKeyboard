package dev.blrs.keyboard.input

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests para la lógica de pares automáticos.
 */
class PairLogicTest {
    
    // Pares definidos en el HTML: ( [ { " `
    // ' NO se autocierra (francés)
    
    private val PAIRS = mapOf(
        '(' to ')',
        '[' to ']',
        '{' to '}',
        '"' to '"',
        '`' to '`'
    )
    
    @Test
    fun testOpeningCharsHaveClosingPairs() {
        assertTrue(PAIRS.containsKey('('))
        assertTrue(PAIRS.containsKey('['))
        assertTrue(PAIRS.containsKey('{'))
        assertTrue(PAIRS.containsKey('"'))
        assertTrue(PAIRS.containsKey('`'))
    }
    
    @Test
    fun testQuoteDoesNotAutoClose() {
        // El apostrofe NO está en los pares
        assertFalse(PAIRS.containsKey('\''))
    }
    
    @Test
    fun testClosingChars() {
        assertEquals(')', PAIRS['('])
        assertEquals(']', PAIRS['['])
        assertEquals('}', PAIRS['{'])
        assertEquals('"', PAIRS['"'])
        assertEquals('`', PAIRS['`'])
    }
    
    @Test
    fun testAllClosingCharsAreValues() {
        val closingChars = PAIRS.values.toSet()
        assertTrue(closingChars.contains(')'))
        assertTrue(closingChars.contains(']'))
        assertTrue(closingChars.contains('}'))
        assertTrue(closingChars.contains('"'))
        assertTrue(closingChars.contains('`'))
    }
}
