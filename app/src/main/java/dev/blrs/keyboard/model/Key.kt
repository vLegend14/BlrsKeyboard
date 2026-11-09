package dev.blrs.keyboard.model

/**
 * Representa una tecla individual en el layout.
 * Puede ser de texto (un carácter) o especial (@nombre).
 */
data class Key(
    val label: String,
    val width: Float = 1f, // Multiplicador de ancho relativo
    val lp: String? = null, // Long-press character or macro
    val up: String? = null, // Swipe-up variants
    val holds: String? = null // Comma-separated variants for holds
) {
    /**
     * Determina si la tecla es especial (comienza con @)
     */
    fun isSpecial(): Boolean = label.startsWith("@")
    
    /**
     * Obtiene el nombre base de una tecla especial (@shift -> shift)
     */
    fun getSpecialName(): String? = if (isSpecial()) label.substring(1) else null
    
    /**
     * Parsea una cadena de definición de tecla
     * Formato: "texto|ancho" o "@especial|ancho"
     */
    companion object {
        fun parse(s: String): Key {
            val parts = s.split("|")
            val label = parts[0]
            val width = if (parts.size > 1) parts[1].toFloatOrNull() ?: 1f else 1f
            return Key(label, width)
        }
        
        /**
         * Crea una tecla de texto simple
         */
        fun text(char: String, width: Float = 1f) = Key(char, width)
        
        /**
         * Crea una tecla especial
         */
        fun special(name: String, width: Float = 1f) = Key("@$name", width)
    }
}

/**
 * Representa una fila de teclas
 */
data class KeyRow(
    val keys: List<Key>
) {
    companion object {
        /**
         * Parsea una lista de strings a una fila de teclas
         */
        fun parse(strings: List<String>): KeyRow {
            return KeyRow(strings.map { Key.parse(it) })
        }
    }
}

/**
 * Representa una capa completa del teclado (abc, sym, sym2)
 */
data class KeyLayer(
    val name: String,
    val rows: List<KeyRow>
) {
    /**
     * Número de filas en esta capa
     */
    fun rowCount(): Int = rows.size
    
    /**
     * Obtiene la fila de índice dado
     */
    fun getRow(index: Int): KeyRow? = rows.getOrNull(index)
}

/**
 * Representa el layout completo del teclado, serializable a JSON
 */
data class KeyboardLayout(
    val bar: Int = 0, // Número de filas de la barra superior
    val feat: Features = Features(),
    val holds: Map<String, String> = emptyMap(), // Variantes por carácter
    val up: Map<String, String> = emptyMap(), // Variants for swipe-up
    val lp: Map<String, String> = emptyMap(), // Long-press actions
    val layers: Map<String, KeyLayer> = emptyMap() // Layer name -> layer
) {
    /**
     * Obtiene la capa por nombre
     */
    fun getLayer(name: String): KeyLayer? = layers[name]
    
    /**
     * Obtiene la capa por defecto (primera en el mapa)
     */
    fun getDefaultLayer(): KeyLayer? = layers.values.firstOrNull()
    
    /**
     * Nombres de todas las capas
     */
    fun getLayerNames(): Set<String> = layers.keys
}

/**
 * Características activas del teclado
 */
data class Features(
    val ctrlLock: Boolean = false,
    val spaceCursor: Boolean = false,
    val pairs: Boolean = false
)
