package dev.blrs.keyboard.model

/**
 * Genera el preset BlrsKeyboard (mk4 del HTML).
 * Único preset, con soporte para configuración de:
 * - Distribución: AZERTY (default) o QWERTY
 * - Números: Fila visible (default) o pulsación larga
 * - Barra superior: Visible (default) u oculta
 */
object BlrsKeyboardPreset {
    
    // Variantes de caracteres (holds del HTML)
    private val DEFAULT_HOLDS = mapOf(
        "a" to "àâáäæ",
        "e" to "éèêë€",
        "i" to "îïíì",
        "o" to "ôóöœ",
        "u" to "ùûúü",
        "c" to "çć",
        "n" to "ñ",
        "y" to "ÿ",
        "s" to "ß",
        "." to "…",
        "," to "¿¡",
        "?" to "¿",
        "!" to "¡",
        "-" to "–—",
        "'" to "''",
        "\"" to "\"\"«»",
        "/" to "÷",
        "=" to "≠≈",
        "<" to "≤«",
        ">" to "≥»"
    )
    
    // Barra superior (TB2 del HTML)
    private val TB2_ROWS = listOf(
        listOf("@esc", "@tab", "/", "\\", "@home", "@up", "@end"),
        listOf("@ctrl", "@alt", "_", "-", "@left", "@down", "@right")
    )
    
    // Símbolos (SY3 del HTML)
    private val SY3_A = listOf("(", ")", "[", "]", "{", "}", "<", ">", "/", "\\")
    private val SY3_B = listOf("@", "#", "$", "%", "&", "*", "-", "+", "=", "_")
    private val SY3_C = listOf("!", "?", ":", ";", "\"", "'", "|")
    private val SY3_S = listOf(
        listOf("~", "^", "`", "€", "£", "¥", "¢", "°", "§", "¶"),
        listOf("¿", "¡", "«", "»", "…", "·", "×", "÷", "±", "¬"),
        listOf("≠", "≤", "≥", "≈", "∞", "√", "π", "∆", "∑", "∫"),
        listOf("←", "→", "↑", "↓", "⇒", "✓", "✗")
    )
    
    /**
     * Genera el layout de BlrsKeyboard según la configuración
     */
    fun generate(
        layout: LayoutType = LayoutType.AZERTY,
        showNumbers: Boolean = true,
        showBar: Boolean = true
    ): KeyboardLayout {
        // Determinar filas de texto según el layout
        val textRows = when (layout) {
            LayoutType.AZERTY -> listOf(
                listOf("a", "z", "e", "r", "t", "y", "u", "i", "o", "p"),
                listOf("q", "s", "d", "f", "g", "h", "j", "k", "l", "m"),
                listOf("w", "x", "c", "v", "b", "n", "'")
            )
            LayoutType.QWERTY -> listOf(
                listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
                listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
                listOf("z", "x", "c", "v", "b", "n", "m")
            )
        }
        
        val digits = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        
        // Construir capas
        val barRows = if (showBar) TB2_ROWS else emptyList()
        val barRowCount = if (showBar) 2 else 0
        
        // Capa ABC
        val abcRows = mutableListOf<List<String>>()
        abcRows.addAll(barRows)
        
        if (showNumbers) {
            abcRows.add(digits)
        }
        
        textRows.dropLast(1).forEach { row ->
            abcRows.add(row)
        }
        
        // Agregar fila inferior con shift y bksp
        val lastRow = mutableListOf<String>()
        lastRow.add("@shift|1.4")
        textRows.last().forEach { lastRow.add(it) }
        lastRow.add("@bksp|1.4")
        abcRows.add(lastRow)
        
        // Agregar fila de acciones (space, enter)
        abcRows.add(listOf("@go:sym|1.4", ",", "@space|5", ".", "@enter|1.4"))
        
        // Capa Sym (?123)
        val symRows = mutableListOf<List<String>>()
        symRows.addAll(barRows)
        
        if (showNumbers) {
            symRows.add(digits)
            symRows.add(SY3_A)
            symRows.add(SY3_B)
        } else {
            // Sin fila de números: dígitos en pulsación larga arriba
            symRows.add(SY3_A)
            symRows.add(SY3_B)
        }
        
        // Fila inferior de sym
        val symLastRow = mutableListOf<String>()
        symLastRow.add("@go:sym2|1.4")
        if (showNumbers) {
            SY3_C.forEach { symLastRow.add(it) }
        } else {
            SY3_C.dropLast(1).forEach { symLastRow.add(it) }
        }
        symLastRow.add("@bksp|1.4")
        symRows.add(symLastRow)
        
        symRows.add(listOf("@go:abc|1.4", ",", "@space|5", ".", "@enter|1.4"))
        
        // Capa Sym2 (=\<)
        val sym2Rows = mutableListOf<List<String>>()
        sym2Rows.addAll(barRows)
        
        if (showNumbers) {
            sym2Rows.add(digits)
            sym2Rows.addAll(SY3_S.dropLast(1))
        } else {
            sym2Rows.addAll(SY3_S.drop(1).dropLast(1))
        }
        
        // Fila inferior de sym2
        val sym2LastRow = mutableListOf<String>()
        sym2LastRow.add("@go:sym|1.4")
        if (showNumbers) {
            SY3_S.last().forEach { sym2LastRow.add(it) }
        } else {
            SY3_S.last().forEach { sym2LastRow.add(it) }
        }
        sym2LastRow.add("@bksp|1.4")
        sym2Rows.add(sym2LastRow)
        
        sym2Rows.add(listOf("@go:abc|1.4", ",", "@space|5", ".", "@enter|1.4"))
        
        // Long-press actions (lp)
        val lpActions = mutableMapOf<String, String>()
        lpActions.putAll(DEFAULT_HOLDS.mapValues { it.value.substring(0, 1) })
        lpActions[","] = "@emoji"
        lpActions["."] = "@settings"
        
        // Macros
        lpActions["/"] = "../"
        lpActions["-"] = "--"
        lpActions["&"] = "&&"
        lpActions["|"] = " | "
        lpActions["@home"] = "~/"
        
        // Construir KeyboardLayout
        return KeyboardLayout(
            bar = barRowCount,
            feat = Features(
                ctrlLock = true,
                spaceCursor = true,
                pairs = true
            ),
            holds = DEFAULT_HOLDS,
            up = emptyMap(), // holds + up se combinan en runtime
            lp = lpActions,
            layers = mapOf(
                "abc" to KeyLayer("abc", abcRows.map { KeyRow.parse(it) }),
                "sym" to KeyLayer("sym", symRows.map { KeyRow.parse(it) }),
                "sym2" to KeyLayer("sym2", sym2Rows.map { KeyRow.parse(it) })
            )
        )
    }
    
    /**
     * Tipo de distribución del teclado
     */
    enum class LayoutType {
        AZERTY,
        QWERTY
    }
}
