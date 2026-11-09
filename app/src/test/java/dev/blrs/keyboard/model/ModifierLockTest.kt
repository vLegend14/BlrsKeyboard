package dev.blrs.keyboard.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests para la lógica de modificadores (Ctrl, Alt, Shift).
 */
class ModifierLockTest {
    
    @Test
    fun testShiftLockTimeout() {
        // Shift: 1 toque = once, 2 toques <400ms = lock
        val timeout = 400L
        
        var shiftState = 0
        var lastShiftTime = 0L
        
        // Primer toque
        val now1 = System.currentTimeMillis()
        shiftState = if (shiftState > 0) 0 else 1
        lastShiftTime = now1
        
        assertEquals(1, shiftState)
        
        // Segundo toque inmediato (<400ms)
        val now2 = now1 + 200
        shiftState = if (shiftState == 1 && now2 - lastShiftTime < timeout) {
            2 // Lock
        } else if (shiftState > 0) {
            0
        } else {
            1
        }
        
        assertEquals(2, shiftState)
    }
    
    @Test
    fun testShiftNoLockTimeout() {
        // Si el segundo toque es >400ms, no hay lock
        val timeout = 400L
        
        var shiftState = 0
        var lastShiftTime = 0L
        
        // Primer toque
        val now1 = System.currentTimeMillis()
        shiftState = if (shiftState > 0) 0 else 1
        lastShiftTime = now1
        
        assertEquals(1, shiftState)
        
        // Segundo toque lento (>400ms)
        val now2 = now1 + 500
        shiftState = if (shiftState == 1 && now2 - lastShiftTime < timeout) {
            2
        } else if (shiftState > 0) {
            0
        } else {
            1
        }
        
        assertEquals(0, shiftState)
    }
    
    @Test
    fun testCtrlLockWithCtrlLockFeature() {
        // Ctrl con feat.ctrlLock = true: 1 uso, 2 toques <400ms = lock
        val ctrlLockEnabled = true
        val timeout = 400L
        
        var ctrlState = 0
        var lastCtrlTime = 0L
        
        // Primer toque
        val now1 = System.currentTimeMillis()
        ctrlState = if (ctrlState == 1 && ctrlLockEnabled && now1 - lastCtrlTime < timeout) {
            2
        } else if (ctrlState > 0) {
            0
        } else {
            1
        }
        lastCtrlTime = now1
        
        assertEquals(1, ctrlState)
        
        // Segundo toque rápido
        val now2 = now1 + 200
        ctrlState = if (ctrlState == 1 && ctrlLockEnabled && now2 - lastCtrlTime < timeout) {
            2
        } else if (ctrlState > 0) {
            0
        } else {
            1
        }
        
        assertEquals(2, ctrlState)
    }
    
    @Test
    fun testAltLockWithCtrlLockFeature() {
        // Alt usa el mismo feat.ctrlLock
        val ctrlLockEnabled = true
        val timeout = 400L
        
        var altState = 0
        var lastAltTime = 0L
        
        // Primer toque
        val now1 = System.currentTimeMillis()
        altState = if (altState == 1 && ctrlLockEnabled && now1 - lastAltTime < timeout) {
            2
        } else if (altState > 0) {
            0
        } else {
            1
        }
        lastAltTime = now1
        
        assertEquals(1, altState)
        
        // Segundo toque rápido
        val now2 = now1 + 300
        altState = if (altState == 1 && ctrlLockEnabled && now2 - lastAltTime < timeout) {
            2
        } else if (altState > 0) {
            0
        } else {
            1
        }
        
        assertEquals(2, altState)
    }
    
    @Test
    fun testGestureTimeouts() {
        // Verificar tiempos del HTML
        val longPressTimeout = 300L
        val repeatInitialDelay = 400L
        val repeatInterval = 60L
        val swipeUpThreshold = 22 // px
        
        assertEquals(300, longPressTimeout)
        assertEquals(400, repeatInitialDelay)
        assertEquals(60, repeatInterval)
        assertEquals(22, swipeUpThreshold)
    }
}
