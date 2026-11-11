package dev.blrs.keyboard.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import dev.blrs.keyboard.input.InputConnectionSink
import dev.blrs.keyboard.input.KeySink
import dev.blrs.keyboard.model.*
import dev.blrs.keyboard.settings.SettingsRepository
import dev.blrs.keyboard.theme.BlrsDesign
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Vista de teclado dibujada con Canvas.
 * Desacoplada del servicio mediante KeySink.
 * 
 * Gestos (del HTML):
 * - Pulsación larga (300ms): inserta lp directo
 * - Swipe up >22px: popup con variantes
 * - Repetición en ⌫, Supr, flechas: 400ms inicio, 60ms intervalo
 * - Shift: 1 toque mayúscula, doble toque <400ms lock
 * - Ctrl/Alt: 1 uso, doble toque <400ms lock
 * - Space: arrastra cursor 1 char cada 14px
 */
class KeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    // Callbacks
    var onOpenEmojiPanel: (() -> Unit)? = null
    
    private var layout: KeyboardLayout? = null
    private var currentLayer: KeyLayer? = null
    private var sink: KeySink? = null
    
    private val settings = SettingsRepository.getInstance(context)
    private var theme: BlrsDesign = BlrsDesign.fromName(settings.themeName)
    private var keyHeightPx: Int = dpToPx(settings.keyHeight)
    
    // Paints
    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val spaceBarPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    // Estado de modificadores
    private var shiftState = 0 // 0=off, 1=once, 2=lock
    private var ctrlState = 0
    private var altState = 0
    private var lastShiftTime = 0L
    private var lastCtrlTime = 0L
    private var lastAltTime = 0L
    
    // Gestos
    private var activeKey: Key? = null
    private var activeRowIndex = -1
    private var activeKeyIndex = -1
    private var pressStartTime = 0L
    private var pressStartY = 0f
    private var pressStartX = 0f
    private var hasTriggeredLongPress = false
    private var isRepeating = false
    private var repeatStarted = false
    private var touchId = 0
    private var cursorDragStart = 0
    private var cursorDragAnchor = 0
    
    // Popup
    private var popupVariants: List<String>? = null
    private var popupSelectedIndex = 0
    private var popupRect: RectF? = null
    
    // Campos reutilizables para rendimiento
    private val keyRect = RectF()
    private val variantRect = RectF()
    private val edgePaint = Paint().apply {
        color = 0x59000000
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }
    private val optionPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cornerPx = dpToPx(4).toFloat()
    private val gapPx = dpToPx(4).toFloat()
    private val keySpacingPx = dpToPx(4).toFloat()
    
    private val repeatRunnable = object : Runnable {
        override fun run() {
            if (isRepeating && repeatStarted && activeKey != null) {
                handleKeyPress(activeKey!!)
                postDelayed(this, 60)
            }
        }
    }
    
    init {
        // Configurar paints
        textPaint.textSize = dpToPx(16).toFloat()
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = Typeface.DEFAULT
        
        hintPaint.textSize = dpToPx(10).toFloat()
        hintPaint.textAlign = Paint.Align.RIGHT
        hintPaint.typeface = Typeface.DEFAULT
        
        spaceBarPaint.style = Paint.Style.FILL
        spaceBarPaint.strokeWidth = dpToPx(3).toFloat()
        
        isClickable = true
        isFocusable = true
    }
    
    fun setLayout(layout: KeyboardLayout) {
        this.layout = layout
        this.currentLayer = layout.getDefaultLayer()
        requestLayout()
        invalidate()
    }
    
    fun setKeySink(sink: KeySink?) {
        this.sink = sink
    }
    
    fun setTheme(theme: BlrsDesign) {
        this.theme = theme
        invalidate()
    }
    
    fun setKeyHeight(dp: Int) {
        this.keyHeightPx = dpToPx(dp)
        requestLayout()
        invalidate()
    }
    
    private fun rowUnitWidth(row: KeyRow, keySpacing: Float): Float {
        val weight = row.keys.sumOf { it.width.toDouble() }.toFloat()
        val total = width - paddingStart - paddingEnd - (row.keys.size - 1) * keySpacing
        return if (weight > 0f) total / weight else 0f
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val rows = currentLayer?.rows?.size ?: 0
        val height = if (rows == 0) 0 else rows * keyHeightPx + (rows - 1) * dpToPx(4) + paddingTop + paddingBottom
        setMeasuredDimension(width, height)
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Dibujar fondo opaco
        canvas.drawColor(theme.bgPrimary)
        
        val layer = currentLayer ?: return
        
        var y = paddingTop.toFloat()
        
        layer.rows.forEachIndexed { rowIndex, row ->
            var x = paddingStart.toFloat()
            val unit = rowUnitWidth(row, keySpacingPx)
            
            row.keys.forEachIndexed { keyIndex, key ->
                val actualWidth = unit * key.width
                keyRect.set(x, y, x + actualWidth, y + keyHeightPx)
                
                drawKey(canvas, keyRect, key, rowIndex, keyIndex)
                
                x += actualWidth + keySpacingPx
            }
            
            y += keyHeightPx + gapPx
        }
        
        // Dibujar popup si está activo
        popupVariants?.let { variants ->
            popupRect?.let { rect ->
                drawPopup(canvas, rect, variants)
            }
        }
    }
    
    private fun drawKey(canvas: Canvas, rect: RectF, key: Key, rowIndex: Int, keyIndex: Int) {
        val isToolbar = rowIndex < (layout?.bar ?: 0)
        val isPressed = activeKey == key && activeRowIndex == rowIndex && activeKeyIndex == keyIndex
        val isSpecial = key.isSpecial()
        
        // Determinar fondo según rol
        val bgColor = when {
            isPressed -> theme.accent
            isToolbar -> theme.bgSecondary
            isSpecial -> theme.bgSecondary
            else -> theme.bgTertiary
        }
        
        keyPaint.color = bgColor
        canvas.drawRoundRect(rect, cornerPx, cornerPx, keyPaint)
        
        // Sombra de canto (estructural, no token)
        if (!isPressed) {
            canvas.drawLine(rect.left, rect.bottom, rect.right, rect.bottom, edgePaint)
        }
        
        // Texto de la tecla
        val label = getKeyLabel(key)
        val textY = rect.centerY() + (textPaint.textSize / 3)
        
        textPaint.color = when {
            isPressed -> theme.accentText
            isSpecial -> theme.textTertiary
            else -> theme.textPrimary
        }
        
        // Modificadores activos
        if (isSpecial) {
            val name = key.getSpecialName()
            when (name) {
                "shift" -> {
                    if (shiftState == 2) {
                        // Lock: relleno de acento
                        keyPaint.color = theme.accent
                        canvas.drawRoundRect(rect, cornerPx, cornerPx, keyPaint)
                        textPaint.color = theme.accentText
                    } else if (shiftState == 1) {
                        // Una vez: contorno
                        textPaint.color = theme.accentTertiary
                    }
                }
                "ctrl" -> {
                    if (ctrlState == 2) {
                        keyPaint.color = theme.accent
                        canvas.drawRoundRect(rect, cornerPx, cornerPx, keyPaint)
                        textPaint.color = theme.accentText
                    } else if (ctrlState == 1) {
                        textPaint.color = theme.accentTertiary
                    }
                }
                "alt" -> {
                    if (altState == 2) {
                        keyPaint.color = theme.accent
                        canvas.drawRoundRect(rect, cornerPx, cornerPx, keyPaint)
                        textPaint.color = theme.accentText
                    } else if (altState == 1) {
                        textPaint.color = theme.accentTertiary
                    }
                }
            }
        }
        
        canvas.drawText(label, rect.centerX(), textY, textPaint)
        
        // Hint (esquina superior derecha)
        val hint = getKeyHint(key, rowIndex)
        if (hint.isNotEmpty()) {
            hintPaint.color = theme.textTertiary
            canvas.drawText(hint, rect.right - dpToPx(4), rect.top + dpToPx(12), hintPaint)
        }
        
        // Marca de barra espaciadora
        if (key.getSpecialName() == "space") {
            val markWidth = rect.width() * 0.35f
            val markHeight = dpToPx(3).toFloat()
            val markLeft = rect.centerX() - markWidth / 2
            val markTop = rect.centerY() - markHeight / 2
            
            spaceBarPaint.color = theme.textTertiary
            spaceBarPaint.alpha = 128 // 50%
            canvas.drawRoundRect(
                markLeft, markTop, markLeft + markWidth, markTop + markHeight,
                markHeight, markHeight, spaceBarPaint
            )
        }
    }
    
    private fun getKeyLabel(key: Key): String {
        if (key.isSpecial()) {
            return when (val name = key.getSpecialName()) {
                "shift" -> if (shiftState == 2) "⇪" else "⇧"
                "bksp" -> "⌫"
                "del" -> "⌦"
                "enter" -> "⏎"
                "space" -> ""
                "esc" -> "Esc"
                "tab" -> "Tab"
                "ctrl" -> "Ctrl"
                "alt" -> "Alt"
                "left" -> "←"
                "right" -> "→"
                "up" -> "↑"
                "down" -> "↓"
                "home" -> "Home"
                "end" -> "End"
                "pgup" -> "PgUp"
                "pgdn" -> "PgDn"
                "sym" -> "?123"
                "sym2" -> "=\\<"
                "abc" -> "ABC"
                else -> {
                    if (name?.startsWith("go:") == true) {
                        val target = name.substring(3)
                        when (target) {
                            "sym" -> "?123"
                            "sym2" -> "=\\<"
                            "abc" -> "ABC"
                            else -> target.uppercase()
                        }
                    } else {
                        name?.uppercase() ?: "?"
                    }
                }
            }
        }
        
        val char = key.label
        return if (shiftState > 0) char.uppercase() else char
    }
    
    private fun getKeyHint(key: Key, rowIndex: Int): String {
        val lp = layout?.lp?.get(key.label.lowercase())
        if (lp != null) {
            return when {
                lp.startsWith("@") -> if (lp == "@emoji") "☺" else if (lp == "@settings") "⚙" else ""
                lp.length == 1 -> lp
                else -> "" // Macros no muestran hint
            }
        }
        
        val holds = layout?.holds?.get(key.label.lowercase())
        if (holds != null && holds.isNotEmpty()) {
            return holds.substring(0, 1)
        }
        
        return ""
    }
    
    private fun drawPopup(canvas: Canvas, rect: RectF, variants: List<String>) {
        val popupPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        popupPaint.color = theme.bgOverlay
        
        // Fondo del popup
        canvas.drawRoundRect(rect, dpToPx(8).toFloat(), dpToPx(8).toFloat(), popupPaint)
        
        // Borde de acento
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        borderPaint.color = theme.accent
        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = dpToPx(1).toFloat()
        canvas.drawRoundRect(rect, dpToPx(8).toFloat(), dpToPx(8).toFloat(), borderPaint)
        
        // Dibujar variantes
        val variantWidth = dpToPx(42)
        val gap = dpToPx(4)
        var x = rect.left + dpToPx(4)
        
        variants.forEachIndexed { index, variant ->
            variantRect.set(
                x, rect.top + dpToPx(4),
                x + variantWidth, rect.bottom - dpToPx(4)
            )
            
            val isSelected = index == popupSelectedIndex
            optionPaint.color = if (isSelected) theme.accent else theme.bgTertiary
            
            canvas.drawRoundRect(variantRect, cornerPx, cornerPx, optionPaint)
            
            textPaint.color = if (isSelected) theme.accentText else theme.textPrimary
            canvas.drawText(variant, variantRect.centerX(), variantRect.centerY() + dpToPx(6), textPaint)
            
            x += variantWidth + gap
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                handlePointerDown(event)
            }
            MotionEvent.ACTION_MOVE -> {
                handleMove(event)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                handlePointerUp(event)
            }
            MotionEvent.ACTION_CANCEL -> {
                handleCancel()
            }
        }
        return true
    }
    
    private fun handlePointerDown(event: MotionEvent) {
        val actionIndex = event.actionIndex
        val x = event.getX(actionIndex)
        val y = event.getY(actionIndex)
        
        val (rowIndex, keyIndex, key) = findKeyAt(x, y) ?: return
        
        val currentTouchId = ++touchId
        
        activeKey = key
        activeRowIndex = rowIndex
        activeKeyIndex = keyIndex
        pressStartTime = System.currentTimeMillis()
        pressStartY = y
        pressStartX = x
        hasTriggeredLongPress = false
        isRepeating = false
        repeatStarted = false
        
        sink?.vibrate()
        invalidate()
        
        // Iniciar repetición si es tecla repetible
        if (isRepeatable(key)) {
            isRepeating = true
            postDelayed({
                if (activeKey == key && touchId == currentTouchId) {
                    repeatStarted = true
                    handleKeyPress(key)
                    postDelayed(repeatRunnable, 60)
                }
            }, 400)
        }
        
        // Iniciar long-press
        val lp = layout?.lp?.get(key.label.lowercase())
        if (lp != null || hasVariants(key)) {
            postDelayed({
                if (activeKey == key && touchId == currentTouchId && !repeatStarted) {
                    if (lp != null && !lp.startsWith("@")) {
                        // Long-press directo
                        commitText(lp)
                        sink?.vibrate()
                        hasTriggeredLongPress = true
                    } else if (hasVariants(key)) {
                        // Mostrar popup de variantes
                        showVariantsPopup(key, x, y)
                    }
                }
            }, 300)
        }
    }
    
    private fun handleMove(event: MotionEvent) {
        val key = activeKey ?: return
        val actionIndex = event.actionIndex
        val x = event.getX(actionIndex)
        val y = event.getY(actionIndex)
        
        // Space drag cursor
        if (key.getSpecialName() == "space" && layout?.feat?.spaceCursor == true) {
            val dx = x - pressStartX
            if (abs(dx) >= dpToPx(14)) {
                val steps = (dx / dpToPx(14)).toInt()
                pressStartX += steps * dpToPx(14)
                
                if (shiftState > 0) {
                    sink?.extendSelection(steps)
                } else {
                    sink?.moveCursor(steps)
                }
                sink?.vibrate()
            }
            return
        }
        
        // Swipe up para variantes
        if (pressStartY - y > dpToPx(22) && hasVariants(key) && !hasTriggeredLongPress) {
            removeCallbacks(repeatRunnable)
            showVariantsPopup(key, x, pressStartY - dpToPx(54))
            hasTriggeredLongPress = true
        }
        
        // Selección en popup
        popupVariants?.let { variants ->
            val popupRect = popupRect ?: return@let
            if (x >= popupRect.left && x <= popupRect.right &&
                y >= popupRect.top && y <= popupRect.bottom) {
                val variantWidth = dpToPx(42) + dpToPx(4)
                val relativeX = x - popupRect.left - dpToPx(4)
                val newIndex = (relativeX / variantWidth).toInt().coerceIn(0, variants.size - 1)
                if (newIndex != popupSelectedIndex) {
                    popupSelectedIndex = newIndex
                    sink?.vibrate()
                    invalidate()
                }
            }
        }
    }
    
    private fun handlePointerUp(event: MotionEvent) {
        val key = activeKey ?: return
        
        removeCallbacks(repeatRunnable)
        repeatStarted = false
        
        // Popup activo
        popupVariants?.let { variants ->
            val selected = variants[popupSelectedIndex]
            commitText(selected)
            hidePopup()
            activeKey = null
            invalidate()
            return
        }
        
        if (!hasTriggeredLongPress && !repeatStarted) {
            handleKeyPress(key)
        }
        
        activeKey = null
        activeRowIndex = -1
        activeKeyIndex = -1
        invalidate()
    }
    
    private fun handleCancel() {
        removeCallbacks(repeatRunnable)
        activeKey = null
        popupVariants = null
        popupRect = null
        invalidate()
    }
    
    private fun handleKeyPress(key: Key) {
        if (key.isSpecial()) {
            handleSpecialKey(key)
        } else {
            val char = if (shiftState > 0) key.label.uppercase() else key.label
            commitText(char)
            
            // Reset shift after use
            if (shiftState == 1) {
                shiftState = 0
                invalidate()
            }
        }
    }
    
    private fun handleSpecialKey(key: Key) {
        val name = key.getSpecialName() ?: return
        
        when (name) {
            "shift" -> {
                val now = System.currentTimeMillis()
                shiftState = if (shiftState == 1 && now - lastShiftTime < 400) {
                    2 // Lock
                } else if (shiftState > 0) {
                    0 // Off
                } else {
                    1 // Once
                }
                lastShiftTime = now
                invalidate()
            }
            "ctrl" -> {
                val now = System.currentTimeMillis()
                ctrlState = if (ctrlState == 1 && now - lastCtrlTime < 400 && layout?.feat?.ctrlLock == true) {
                    2 // Lock
                } else if (ctrlState > 0) {
                    0 // Off
                } else {
                    1 // Once
                }
                lastCtrlTime = now
                invalidate()
            }
            "alt" -> {
                val now = System.currentTimeMillis()
                altState = if (altState == 1 && now - lastAltTime < 400 && layout?.feat?.ctrlLock == true) {
                    2 // Lock
                } else if (altState > 0) {
                    0 // Off
                } else {
                    1 // Once
                }
                lastAltTime = now
                invalidate()
            }
            "bksp" -> {
                if (ctrlState > 0 || altState > 0) {
                    sendKeyEvent(KeyEvent.KEYCODE_DEL)
                    clearModifiers()
                } else {
                    sink?.deleteBeforeCursor()
                }
            }
            "del" -> {
                if (ctrlState > 0 || altState > 0) {
                    sendKeyEvent(KeyEvent.KEYCODE_FORWARD_DEL)
                    clearModifiers()
                } else {
                    sink?.deleteAfterCursor()
                }
            }
            "enter" -> {
                if (ctrlState > 0 || altState > 0) {
                    sendKeyEvent(KeyEvent.KEYCODE_ENTER)
                    clearModifiers()
                } else {
                    sink?.performEditorAction(0)
                }
            }
            "space" -> {
                if (ctrlState > 0 || altState > 0) {
                    sendKeyEvent(KeyEvent.KEYCODE_SPACE)
                    clearModifiers()
                } else {
                    sink?.commitText(" ")
                }
            }
            "tab" -> {
                sendKeyEvent(KeyEvent.KEYCODE_TAB)
                clearModifiers()
            }
            "esc" -> {
                sendKeyEvent(KeyEvent.KEYCODE_ESCAPE)
                clearModifiers()
            }
            "left" -> sendKeyEvent(KeyEvent.KEYCODE_DPAD_LEFT)
            "right" -> sendKeyEvent(KeyEvent.KEYCODE_DPAD_RIGHT)
            "up" -> sendKeyEvent(KeyEvent.KEYCODE_DPAD_UP)
            "down" -> sendKeyEvent(KeyEvent.KEYCODE_DPAD_DOWN)
            "home" -> {
                if (ctrlState > 0 || altState > 0) {
                    sendKeyEvent(KeyEvent.KEYCODE_HOME)
                    clearModifiers()
                } else {
                    // Ir al inicio de línea
                    sink?.let { s ->
                        val pos = s.getCursorPosition()
                        val text = getCurrentText()
                        val lineStart = text.lastIndexOf('\n', pos - 1) + 1
                        s.setSelection(lineStart)
                    }
                }
            }
            "end" -> {
                if (ctrlState > 0 || altState > 0) {
                    sendKeyEvent(KeyEvent.KEYCODE_MOVE_END)
                    clearModifiers()
                } else {
                    // Ir al fin de línea
                    sink?.let { s ->
                        val pos = s.getCursorPosition()
                        val text = getCurrentText()
                        val lineEnd = text.indexOf('\n', pos).let { if (it < 0) text.length else it }
                        s.setSelection(lineEnd)
                    }
                }
            }
            "pgup" -> sendKeyEvent(KeyEvent.KEYCODE_PAGE_UP)
            "pgdn" -> sendKeyEvent(KeyEvent.KEYCODE_PAGE_DOWN)
            
            // Cambio de capa
            "go:sym", "sym" -> {
                layout?.getLayer("sym")?.let {
                    currentLayer = it
                    sink?.onLayerChange("sym")
                    requestLayout()
                    invalidate()
                }
            }
            "go:sym2", "sym2" -> {
                layout?.getLayer("sym2")?.let {
                    currentLayer = it
                    sink?.onLayerChange("sym2")
                    requestLayout()
                    invalidate()
                }
            }
            "go:abc", "abc" -> {
                layout?.getLayer("abc")?.let {
                    currentLayer = it
                    sink?.onLayerChange("abc")
                    requestLayout()
                    invalidate()
                }
            }
            
            // Otros
            "emoji" -> {
                onOpenEmojiPanel?.invoke()
            }
            "settings" -> {
                // TODO: abrir panel de ajustes
            }
        }
    }
    
    private fun commitText(text: String) {
        if (ctrlState > 0 || altState > 0) {
            // Combinación con modificador
            val metaState = buildMetaState()
            text.firstOrNull()?.let { char ->
                val keyCode = charToKeyCode(char)
                if (keyCode != KeyEvent.KEYCODE_UNKNOWN) {
                    sink?.sendKeyEvent(keyCode, metaState, true)
                    sink?.sendKeyEvent(keyCode, metaState, false)
                } else {
                    // Fallback: commit sin modificador
                    sink?.commitText(text)
                }
            }
            clearModifiers()
        } else {
            sink?.commitText(text)
        }
    }
    
    private fun charToKeyCode(char: Char): Int {
        return when (char.lowercaseChar()) {
            'a' -> KeyEvent.KEYCODE_A
            'b' -> KeyEvent.KEYCODE_B
            'c' -> KeyEvent.KEYCODE_C
            'd' -> KeyEvent.KEYCODE_D
            'e' -> KeyEvent.KEYCODE_E
            'f' -> KeyEvent.KEYCODE_F
            'g' -> KeyEvent.KEYCODE_G
            'h' -> KeyEvent.KEYCODE_H
            'i' -> KeyEvent.KEYCODE_I
            'j' -> KeyEvent.KEYCODE_J
            'k' -> KeyEvent.KEYCODE_K
            'l' -> KeyEvent.KEYCODE_L
            'm' -> KeyEvent.KEYCODE_M
            'n' -> KeyEvent.KEYCODE_N
            'o' -> KeyEvent.KEYCODE_O
            'p' -> KeyEvent.KEYCODE_P
            'q' -> KeyEvent.KEYCODE_Q
            'r' -> KeyEvent.KEYCODE_R
            's' -> KeyEvent.KEYCODE_S
            't' -> KeyEvent.KEYCODE_T
            'u' -> KeyEvent.KEYCODE_U
            'v' -> KeyEvent.KEYCODE_V
            'w' -> KeyEvent.KEYCODE_W
            'x' -> KeyEvent.KEYCODE_X
            'y' -> KeyEvent.KEYCODE_Y
            'z' -> KeyEvent.KEYCODE_Z
            '0' -> KeyEvent.KEYCODE_0
            '1' -> KeyEvent.KEYCODE_1
            '2' -> KeyEvent.KEYCODE_2
            '3' -> KeyEvent.KEYCODE_3
            '4' -> KeyEvent.KEYCODE_4
            '5' -> KeyEvent.KEYCODE_5
            '6' -> KeyEvent.KEYCODE_6
            '7' -> KeyEvent.KEYCODE_7
            '8' -> KeyEvent.KEYCODE_8
            '9' -> KeyEvent.KEYCODE_9
            ' ' -> KeyEvent.KEYCODE_SPACE
            '\n' -> KeyEvent.KEYCODE_ENTER
            '\t' -> KeyEvent.KEYCODE_TAB
            else -> KeyEvent.KEYCODE_UNKNOWN
        }
    }
    
    private fun sendKeyEvent(keyCode: Int) {
        val metaState = buildMetaState()
        sink?.sendKeyEvent(keyCode, metaState, true)
        sink?.sendKeyEvent(keyCode, metaState, false)
        clearModifiers()
    }
    
    private fun buildMetaState(): Int {
        var meta = 0
        if (ctrlState > 0) meta = meta or InputConnectionSink.META_CTRL_ON
        if (altState > 0) meta = meta or InputConnectionSink.META_ALT_ON
        if (shiftState > 0) meta = meta or InputConnectionSink.META_SHIFT_ON
        return meta
    }
    
    private fun clearModifiers() {
        if (ctrlState == 1) ctrlState = 0
        if (altState == 1) altState = 0
        if (shiftState == 1) shiftState = 0
        invalidate()
    }
    
    private fun isRepeatable(key: Key): Boolean {
        val name = key.getSpecialName() ?: return false
        return name in listOf("bksp", "del", "left", "right", "up", "down")
    }
    
    private fun hasVariants(key: Key): Boolean {
        val char = key.label.lowercase()
        return layout?.holds?.containsKey(char) == true || layout?.up?.containsKey(char) == true
    }
    
    private fun showVariantsPopup(key: Key, x: Float, y: Float) {
        val char = key.label.lowercase()
        val holds = layout?.holds?.get(char) ?: ""
        val up = layout?.up?.get(char) ?: ""
        
        val variants = (holds + up).toSet().map { it.toString() }
        if (variants.isEmpty()) return
        
        popupVariants = if (shiftState > 0) variants.map { it.uppercase() } else variants
        popupSelectedIndex = 0
        
        val variantWidth = dpToPx(42) + dpToPx(4)
        val popupWidth = variants.size * variantWidth + dpToPx(8)
        val popupHeight = dpToPx(50)
        
        val left = (x - popupWidth / 2).coerceIn(0f, (width - popupWidth).toFloat())
        val top = y.coerceAtLeast(0f)
        
        popupRect = RectF(left, top, left + popupWidth, top + popupHeight)
        
        sink?.vibrate()
        invalidate()
    }
    
    private fun hidePopup() {
        popupVariants = null
        popupRect = null
        popupSelectedIndex = 0
    }
    
    private fun findKeyAt(x: Float, y: Float): Triple<Int, Int, Key>? {
        val layer = currentLayer ?: return null
        val gap = dpToPx(4).toFloat()
        val keySpacing = dpToPx(4).toFloat()
        
        var currentY = paddingTop.toFloat()
        
        layer.rows.forEachIndexed { rowIndex, row ->
            var currentX = paddingStart.toFloat()
            val unit = rowUnitWidth(row, keySpacing)
            
            if (y >= currentY && y < currentY + keyHeightPx) {
                row.keys.forEachIndexed { keyIndex, key ->
                    val actualWidth = unit * key.width
                    
                    if (x >= currentX && x < currentX + actualWidth) {
                        return Triple(rowIndex, keyIndex, key)
                    }
                    
                    currentX += actualWidth + keySpacing
                }
            }
            
            currentY += keyHeightPx + gap
        }
        
        return null
    }
    
    private fun getCurrentText(): String {
        // TODO: obtener texto actual del InputConnection
        return ""
    }
    
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
