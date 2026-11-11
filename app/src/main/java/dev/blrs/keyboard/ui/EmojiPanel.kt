package dev.blrs.keyboard.ui

import android.content.Context
import android.graphics.*
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.GridView
import android.widget.BaseAdapter
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.HorizontalScrollView
import dev.blrs.keyboard.input.KeySink
import dev.blrs.keyboard.theme.BlrsDesign

/**
 * Panel de emojis usando emojis nativos del sistema Android.
 * Grid de emojis Unicode estándar organizados por categorías.
 * 
 * Categorías (del HTML):
 * 1. Smileys - 😊😂🥰😍🤗😎🤩😋🤔🙄😢😭😤😱🥵...
 * 2. People - 👋🤚🖐️✋🖖👌🤏✌️🤟🤘🤙👈👉👆...
 * 3. Animals - 🐶🐱🐭🐹🐰🦊🐻🐼🐨🐯🦁🐮🐷🐸...
 * 4. Food - 🍎🍐🍊🍋🍌🍉🍇🍓🫐🍈🍒🍑🥭🍍🥥...
 * 5. Travel - 🚗🚕🚙🚌🚎🏎️🚓🚑🚒🚐🚚🚛🚜🦯🦽...
 * 
 * Barra inferior: ABC (volver), páginas (• • •), ⌫
 */
class EmojiPanel(context: Context) : LinearLayout(context) {
    
    companion object {
        // Emojis Unicode estándar (compatible con Android 8+)
        // Cada categoría tiene 45 emojis (3 páginas de 15)
        val EMOJIS = mapOf(
            "smileys" to listOf(
                // Página 1
                "😊", "😂", "🥰", "😍", "🤗", "😎", "🤩", "😋", "🤔", "🙄", "😢", "😭", "😤", "😱", "🥵",
                // Página 2
                "😴", "🤮", "🤢", "😷", "🤒", "🤕", "🤠", "🥳", "🥸", "😇", "🤓", "😈", "👿", "💀", "👻",
                // Página 3
                "👽", "🤖", "💩", "👏", "👍", "👎", "👊", "✊", "🤛", "🤜", "🤞", "✌️", "🤟", "🤘", "👌"
            ),
            "people" to listOf(
                "👋", "🤚", "🖐️", "✋", "🖖", "👌", "🤏", "✌️", "🤟", "🤘", "🤙", "👈", "👉", "👆", "🖕",
                "👇", "☝️", "👍", "👎", "✊", "👊", "🤛", "🤜", "👏", "🙌", "👐", "🤲", "🤝", "🙏", "💅",
                "🤳", "💪", "🦾", "🦿", "🦵", "🦶", "👂", "🦻", "👃", "🧠", "🦷", "🦴", "👀", "👁️", "👅"
            ),
            "animals" to listOf(
                "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐨", "🐯", "🦁", "🐮", "🐷", "🐸", "🐵",
                "🐔", "🐧", "🐦", "🐤", "🦆", "🦅", "🦉", "🦇", "🐺", "🐗", "🐴", "🦄", "🐝", "🐛", "🦋",
                "🐌", "🐞", "🐜", "🦟", "🦗", "🕷️", "🦂", "🐢", "🐍", "🦎", "🦖", "🦕", "🐙", "🦑", "🦐"
            ),
            "food" to listOf(
                "🍎", "🍐", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🫐", "🍈", "🍒", "🍑", "🥭", "🍍", "🥥",
                "🥝", "🍅", "🍆", "🥑", "🥦", "🥬", "🥒", "🌶️", "🫑", "🌽", "🥕", "🫒", "🧄", "🧅", "🥔",
                "🍠", "🥐", "🥯", "🍞", "🥖", "🥨", "🧀", "🥚", "🍳", "🧈", "🥞", "🧇", "🥓", "🥩", "🍗"
            ),
            "travel" to listOf(
                "🚗", "🚕", "🚙", "🚌", "🚎", "🏎️", "🚓", "🚑", "🚒", "🚐", "🚚", "🚛", "🚜", "🦯", "🦽",
                "🦼", "🛴", "🚲", "🛵", "🏍️", "🛺", "🚨", "🚔", "🚍", "🚘", "🚖", "🚡", "🚠", "🚟", "🚃",
                "🚋", "🚞", "🚝", "🚄", "🚅", "🚈", "🚂", "🚆", "🚇", "🚊", "🚉", "✈️", "🛫", "🛬", "🛩️"
            )
        )
        
        // Iconos de categorías
        val CATEGORY_ICONS = listOf("😊", "👋", "🐶", "🍎", "🚗")
        val CATEGORY_KEYS = listOf("smileys", "people", "animals", "food", "travel")
    }
    
    private var sink: KeySink? = null
    private var theme: BlrsDesign = BlrsDesign.BLUE_ROSE
    
    private var currentCategory = 0
    private var currentPage = 0
    private val emojisPerPage = 15
    
    // Componentes UI
    private val categoryBar: LinearLayout
    private val emojiGrid: GridView
    private val pageIndicator: LinearLayout
    private val bottomBar: LinearLayout
    
    var onBackToKeyboard: (() -> Unit)? = null
    
    init {
        orientation = VERTICAL
        setBackgroundColor(theme.bgPrimary)
        
        // Barra de categorías
        categoryBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                dpToPx(48)
            )
            setBackgroundColor(Color.parseColor("#1E1E1E"))
        }
        
        CATEGORY_ICONS.forEachIndexed { index, icon ->
            val catButton = TextView(context).apply {
                text = icon
                textSize = 20f
                gravity = Gravity.CENTER
                setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
                layoutParams = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
                setBackgroundColor(if (index == currentCategory) Color.parseColor("#2D68D8") else Color.TRANSPARENT)
                setTextColor(Color.WHITE)
                setOnClickListener {
                    currentCategory = index
                    currentPage = 0
                    updateCategoryBar()
                    updateEmojiGrid()
                    updatePageIndicator()
                }
            }
            categoryBar.addView(catButton)
        }
        addView(categoryBar)
        
        // Grid de emojis
        emojiGrid = GridView(context).apply {
            numColumns = 5
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            verticalSpacing = dpToPx(8)
            horizontalSpacing = dpToPx(8)
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
        }
        addView(emojiGrid)
        
        // Indicador de páginas
        pageIndicator = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                dpToPx(24)
            )
        }
        addView(pageIndicator)
        
        // Barra inferior
        bottomBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                dpToPx(48)
            )
            setBackgroundColor(Color.parseColor("#1E1E1E"))
        }
        
        // Botón ABC
        val abcButton = TextView(context).apply {
            text = "ABC"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#2D68D8"))
            setPadding(dpToPx(16), 0, dpToPx(16), 0)
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
            setOnClickListener {
                onBackToKeyboard?.invoke()
            }
        }
        bottomBar.addView(abcButton)
        
        // Espacio para indicadores
        val spacer = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 2f)
        }
        bottomBar.addView(spacer)
        
        // Botón ⌫
        val backspaceButton = TextView(context).apply {
            text = "⌫"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(dpToPx(16), 0, dpToPx(16), 0)
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
            setOnClickListener {
                sink?.sendKeyEvent(KeyEvent.KEYCODE_DEL)
            }
        }
        bottomBar.addView(backspaceButton)
        addView(bottomBar)
        
        // Inicializar
        updateCategoryBar()
        updateEmojiGrid()
        updatePageIndicator()
    }
    
    private fun updateCategoryBar() {
        for (i in 0 until categoryBar.childCount) {
            val child = categoryBar.getChildAt(i) as? TextView
            child?.setBackgroundColor(
                if (i == currentCategory) Color.parseColor("#2D68D8") else Color.TRANSPARENT
            )
        }
    }
    
    private fun updateEmojiGrid() {
        val categoryKey = CATEGORY_KEYS[currentCategory]
        val emojis = EMOJIS[categoryKey] ?: emptyList()
        val startIndex = currentPage * emojisPerPage
        val endIndex = minOf(startIndex + emojisPerPage, emojis.size)
        val pageEmojis = emojis.subList(startIndex, endIndex)
        
        emojiGrid.adapter = object : BaseAdapter() {
            override fun getCount() = pageEmojis.size
            override fun getItem(position: Int) = pageEmojis[position]
            override fun getItemId(position: Int) = position.toLong()
            
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val textView = (convertView as? TextView) ?: TextView(context).apply {
                    textSize = 28f
                    gravity = Gravity.CENTER
                    setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
                }
                textView.text = pageEmojis[position]
                return textView
            }
        }
        
        emojiGrid.setOnItemClickListener { _, _, position, _ ->
            val emoji = pageEmojis[position]
            sink?.commitText(emoji)
        }
    }
    
    private fun updatePageIndicator() {
        pageIndicator.removeAllViews()
        
        for (i in 0 until 3) {
            val dot = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(8), dpToPx(8)).apply {
                    marginStart = dpToPx(4)
                    marginEnd = dpToPx(4)
                }
                setBackgroundResource(
                    if (i == currentPage) android.R.drawable.presence_online
                    else android.R.drawable.presence_invisible
                )
            }
            pageIndicator.addView(dot)
        }
    }
    
    fun setKeySink(sink: KeySink) {
        this.sink = sink
    }
    
    fun setTheme(theme: BlrsDesign) {
        this.theme = theme
        setBackgroundColor(theme.bgPrimary)
        categoryBar.setBackgroundColor(theme.bgSecondary)
        bottomBar.setBackgroundColor(theme.bgSecondary)
    }
    
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
