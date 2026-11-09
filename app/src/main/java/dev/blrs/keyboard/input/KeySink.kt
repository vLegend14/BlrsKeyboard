package dev.blrs.keyboard.input

/**
 * Interfaz para recibir eventos de teclado.
 * Desacopla KeyboardView del servicio IME, permitiendo usarla
 * tanto en el IME como embebida en la Activity.
 */
interface KeySink {
    /**
     * Inserta texto en la posición actual del cursor
     */
    fun commitText(text: String)
    
    /**
     * Borra N caracteres antes del cursor
     */
    fun deleteBeforeCursor(count: Int = 1)
    
    /**
     * Borra N caracteres después del cursor
     */
    fun deleteAfterCursor(count: Int = 1)
    
    /**
     * Mueve el cursor a una posición absoluta
     */
    fun setSelection(position: Int)
    
    /**
     * Mueve el cursor relativamente
     */
    fun moveCursor(delta: Int)
    
    /**
     * Selecciona texto (mueve el fin de selección)
     */
    fun extendSelection(delta: Int)
    
    /**
     * Obtiene la posición actual del cursor
     */
    fun getCursorPosition(): Int
    
    /**
     * Obtiene el texto seleccionado (inicio y fin)
     */
    fun getSelection(): Pair<Int, Int>
    
    /**
     * Envía un KeyEvent
     */
    fun sendKeyEvent(keyCode: Int, metaState: Int = 0, isDown: Boolean = true)
    
    /**
     * Ejecuta una acción de editor (Enter)
     */
    fun performEditorAction(actionCode: Int)
    
    /**
     * Obtiene el InputType actual
     */
    fun getInputType(): Int
    
    /**
     * Obtiene las ImeOptions actuales
     */
    fun getImeOptions(): Int
    
    /**
     * Notifica un cambio de capa
     */
    fun onLayerChange(layerName: String)
    
    /**
     * Vibra (si está habilitado)
     */
    fun vibrate(durationMs: Long = 8)
}
