package dev.blrs.keyboard

import android.content.SharedPreferences
import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import dev.blrs.keyboard.input.InputConnectionSink
import dev.blrs.keyboard.input.KeySink
import dev.blrs.keyboard.model.BlrsKeyboardPreset
import dev.blrs.keyboard.model.KeyboardLayout
import dev.blrs.keyboard.settings.SettingsRepository
import dev.blrs.keyboard.ui.KeyboardView
import dev.blrs.keyboard.ui.EmojiPanel
import dev.blrs.keyboard.theme.BlrsDesign

/**
 * Servicio de método de entrada (IME) para BlrsKeyboard.
 */
class BlrsKeyboardService : InputMethodService() {
    
    private var containerView: FrameLayout? = null
    private var keyboardView: KeyboardView? = null
    private var emojiPanel: EmojiPanel? = null
    private var currentLayout: KeyboardLayout? = null
    private var sink: KeySink? = null
    private var settings: SettingsRepository? = null
    
    private var showingEmojiPanel = false
    
    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        settings?.let { repo ->
            when (key) {
                "ly", "nums", "bar" -> rebuildLayout()
                "theme" -> updateTheme()
                "ac" -> updateTheme()
                "kh" -> updateKeyHeight()
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        settings = SettingsRepository.getInstance(applicationContext)
        settings?.registerListener(prefListener)
    }
    
    override fun onCreateInputView(): View {
        // Contenedor principal
        containerView = FrameLayout(this)
        
        // Vista de teclado
        keyboardView = KeyboardView(this)
        keyboardView?.id = View.generateViewId()
        keyboardView?.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        keyboardView?.onOpenEmojiPanel = {
            showEmojiPanel()
        }
        
        // Panel de emojis
        emojiPanel = EmojiPanel(this)
        emojiPanel?.id = View.generateViewId()
        emojiPanel?.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        emojiPanel?.visibility = View.GONE
        emojiPanel?.onBackToKeyboard = {
            hideEmojiPanel()
        }
        
        containerView?.addView(keyboardView)
        containerView?.addView(emojiPanel)
        
        return containerView!!
    }
    
    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        
        sink = InputConnectionSink(
            applicationContext,
            currentInputConnection,
            attribute
        )
        
        keyboardView?.setKeySink(sink)
        emojiPanel?.setKeySink(sink!!)
        
        // Construir layout inicial
        rebuildLayout()
        
        // Respetar inputType
        handleInputType(attribute?.inputType ?: 0)
    }
    
    override fun onFinishInput() {
        super.onFinishInput()
        sink = null
    }
    
    override fun onDestroy() {
        settings?.unregisterListener(prefListener)
        super.onDestroy()
    }
    
    private fun rebuildLayout() {
        val repo = settings ?: return
        
        currentLayout = BlrsKeyboardPreset.generate(
            layout = repo.layout,
            showNumbers = repo.showNumbers,
            showBar = repo.showBar
        )
        
        keyboardView?.setLayout(currentLayout!!)
        keyboardView?.setKeyHeight(repo.keyHeight)
        updateTheme()
    }
    
    private fun updateTheme() {
        val repo = settings ?: return
        
        var theme = BlrsDesign.fromName(repo.themeName)
        theme = theme.withAccentOption(repo.accentOption)
        
        keyboardView?.setTheme(theme)
        emojiPanel?.setTheme(theme)
    }
    
    private fun updateKeyHeight() {
        settings?.let { repo ->
            keyboardView?.setKeyHeight(repo.keyHeight)
        }
    }
    
    private fun showEmojiPanel() {
        showingEmojiPanel = true
        keyboardView?.visibility = View.GONE
        emojiPanel?.visibility = View.VISIBLE
    }
    
    private fun hideEmojiPanel() {
        showingEmojiPanel = false
        emojiPanel?.visibility = View.GONE
        keyboardView?.visibility = View.VISIBLE
    }
    
    private fun handleInputType(inputType: Int) {
        val maskedInputType = inputType and InputType.TYPE_MASK_CLASS
        
        when (maskedInputType) {
            InputType.TYPE_CLASS_NUMBER,
            InputType.TYPE_CLASS_PHONE -> {
                // Entrada numérica: abrir directo la capa de símbolos
                keyboardView?.setLayout(currentLayout!!)
                // TODO: cambiar a capa sym automáticamente
            }
            InputType.TYPE_TEXT_VARIATION_PASSWORD -> {
                // Contraseña: sin popups de emojis ni macros
                // TODO: deshabilitar emojis y macros
            }
        }
    }
    
    override fun onWindowShown() {
        super.onWindowShown()
        // Respetar insets del sistema (barra de gestos)
        // TODO: aplicar insets
    }
    
    override fun onEvaluateFullscreenMode(): Boolean {
        // Nunca usar extract view (pantalla completa)
        return false
    }
}
