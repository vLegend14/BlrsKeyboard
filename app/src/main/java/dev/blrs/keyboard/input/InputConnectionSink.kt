package dev.blrs.keyboard.input

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import dev.blrs.keyboard.settings.SettingsRepository

/**
 * Implementación de KeySink usando InputConnection.
 * Puente entre KeyboardView y el sistema Android.
 * 
 * CRÍTICO: Ctrl+C y combinaciones deben funcionar en Termux.
 */
class InputConnectionSink(
    private val context: Context,
    private val inputConnection: InputConnection?,
    private val editorInfo: EditorInfo?
) : KeySink {
    
    private val settings = SettingsRepository.getInstance(context)
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    
    override fun commitText(text: String) {
        inputConnection?.commitText(text, 1)
    }
    
    override fun deleteBeforeCursor(count: Int) {
        inputConnection?.deleteSurroundingText(count, 0)
    }
    
    override fun deleteAfterCursor(count: Int) {
        inputConnection?.deleteSurroundingText(0, count)
    }
    
    override fun setSelection(position: Int) {
        inputConnection?.setSelection(position, position)
    }
    
    override fun moveCursor(delta: Int) {
        val current = getCursorPosition()
        setSelection(current + delta)
    }
    
    override fun extendSelection(delta: Int) {
        val (start, end) = getSelection()
        val newEnd = end + delta
        inputConnection?.setSelection(start, newEnd)
    }
    
    override fun getCursorPosition(): Int {
        val (start, end) = getSelection()
        return if (start == end) start else end
    }
    
    override fun getSelection(): Pair<Int, Int> {
        val extracted = inputConnection?.getExtractedText(ExtractedTextRequest(), 0)
        return Pair(
            extracted?.selectionStart ?: 0,
            extracted?.selectionEnd ?: 0
        )
    }
    
    /**
     * Envía un KeyEvent real con metaState para Ctrl, Alt y Shift.
     * CRÍTICO para Termux y otras apps de terminal.
     */
    override fun sendKeyEvent(keyCode: Int, metaState: Int, isDown: Boolean) {
        val action = if (isDown) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP
        val event = KeyEvent(
            System.currentTimeMillis(),
            System.currentTimeMillis(),
            action,
            keyCode,
            0,
            metaState
        )
        inputConnection?.sendKeyEvent(event)
    }
    
    /**
     * Ejecuta la acción de Enter según ImeOptions.
     * Si no hay acción específica, inserta salto de línea.
     */
    override fun performEditorAction(actionCode: Int) {
        val imeOptions = editorInfo?.imeOptions ?: 0
        
        // Si hay acción de editor, ejecutarla
        if (imeOptions and EditorInfo.IME_MASK_ACTION != EditorInfo.IME_ACTION_NONE) {
            inputConnection?.performEditorAction(imeOptions and EditorInfo.IME_MASK_ACTION)
        } else {
            // Sin acción: salto de línea normal
            commitText("\n")
        }
    }
    
    override fun getInputType(): Int {
        return editorInfo?.inputType ?: 0
    }
    
    override fun getImeOptions(): Int {
        return editorInfo?.imeOptions ?: 0
    }
    
    override fun onLayerChange(layerName: String) {
        // TODO: notificar al servicio del cambio de capa si es necesario
    }
    
    override fun vibrate(durationMs: Long) {
        if (settings.vibrationEnabled && vibrator?.hasVibrator() == true) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
    
    companion object {
        /**
         * Constantes de metaState para KeyEvents
         */
        const val META_CTRL_ON = KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        const val META_ALT_ON = KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
        const val META_SHIFT_ON = KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
    }
}
