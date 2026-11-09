package dev.blrs.keyboard

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import dev.blrs.keyboard.input.KeySink
import dev.blrs.keyboard.model.BlrsKeyboardPreset
import dev.blrs.keyboard.settings.SettingsRepository
import dev.blrs.keyboard.theme.BlrsDesign

/**
 * Activity de configuración.
 * Muestra las mismas opciones que el panel de Ajustes del teclado.
 * Vista previa en vivo con KeyboardView embebida.
 */
class SettingsActivity : AppCompatActivity() {
    
    private lateinit var settings: SettingsRepository
    private var keyboardView: dev.blrs.keyboard.ui.KeyboardView? = null
    private var previewSink: PreviewSink? = null
    
    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        settings.let { repo ->
            when (key) {
                "ly", "nums", "bar" -> rebuildLayout()
                "theme" -> updateTheme()
                "ac" -> updateTheme()
                "kh" -> updateKeyHeight()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        settings = SettingsRepository.getInstance(applicationContext)
        settings.registerListener(prefListener)
        
        setupInstallationCard()
        setupSettingsPanel()
        setupPreview()
    }
    
    override fun onResume() {
        super.onResume()
        updateInstallationStatus()
    }
    
    override fun onDestroy() {
        settings.unregisterListener(prefListener)
        super.onDestroy()
    }
    
    private fun setupInstallationCard() {
        val step1 = findViewById<Button>(R.id.install_step1)
        val step2 = findViewById<Button>(R.id.install_step2)
        val card = findViewById<View>(R.id.install_card)
        
        step1.setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
        
        step2.setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }
    }
    
    private fun updateInstallationStatus() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val enabled = imm.enabledInputMethodList.any { 
            it.packageName == packageName 
        }
        val selected = imm.currentInputMethodSubtype?.let {
            imm.inputMethodList.any { it.packageName == packageName }
        } ?: false
        
        val step1Check = findViewById<TextView>(R.id.install_step1_check)
        val step2Check = findViewById<TextView>(R.id.install_step2_check)
        val card = findViewById<View>(R.id.install_card)
        
        step1Check?.visibility = if (enabled) View.VISIBLE else View.GONE
        step2Check?.visibility = if (selected) View.VISIBLE else View.GONE
        
        // Colapsar tarjeta si todo está listo
        if (enabled && selected) {
            card?.visibility = View.GONE
        } else {
            card?.visibility = View.VISIBLE
        }
    }
    
    private fun setupSettingsPanel() {
        // Cargar bento de opciones
        rebuildLayout()
        updateTheme()
        
        // Layout
        setupOption(
            R.id.layout_azerty,
            SettingsRepository.Option.Layout,
            "azerty"
        )
        setupOption(
            R.id.layout_qwerty,
            SettingsRepository.Option.Layout,
            "qwerty"
        )
        
        // Numbers
        setupOption(
            R.id.nums_visible,
            SettingsRepository.Option.Numbers,
            true
        )
        setupOption(
            R.id.nums_longpress,
            SettingsRepository.Option.Numbers,
            false
        )
        
        // Accent
        setupOption(
            R.id.accent_default,
            SettingsRepository.Option.Accent,
            "c"
        )
        setupOption(
            R.id.accent_accent,
            SettingsRepository.Option.Accent,
            "a"
        )
        setupOption(
            R.id.accent_details,
            SettingsRepository.Option.Accent,
            "b"
        )
        
        // Bar
        setupOption(
            R.id.bar_visible,
            SettingsRepository.Option.Bar,
            true
        )
        setupOption(
            R.id.bar_hidden,
            SettingsRepository.Option.Bar,
            false
        )
        
        // Vibration
        setupOption(
            R.id.vib_yes,
            SettingsRepository.Option.Vibration,
            true
        )
        setupOption(
            R.id.vib_no,
            SettingsRepository.Option.Vibration,
            false
        )
        
        // Theme
        setupOption(
            R.id.theme_bluerose,
            SettingsRepository.Option.Theme,
            "bluerose"
        )
        setupOption(
            R.id.theme_oled,
            SettingsRepository.Option.Theme,
            "oled"
        )
        setupOption(
            R.id.theme_thorn,
            SettingsRepository.Option.Theme,
            "thorn"
        )
        setupOption(
            R.id.theme_amethyst,
            SettingsRepository.Option.Theme,
            "amethyst"
        )
        setupOption(
            R.id.theme_azure,
            SettingsRepository.Option.Theme,
            "azure"
        )
        
        // Key height slider
        val heightSlider = findViewById<SeekBar>(R.id.key_height_slider)
        val heightValue = findViewById<TextView>(R.id.key_height_value)
        
        heightSlider?.progress = settings.keyHeight - 38
        heightValue?.text = "${settings.keyHeight}px"
        
        heightSlider?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val height = progress + 38
                heightValue?.text = "${height}px"
                if (fromUser) {
                    settings.set(SettingsRepository.Option.KeyHeight, height)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
    
    private fun <T> setupOption(buttonId: Int, option: SettingsRepository.Option<T>, value: T) {
        val button = findViewById<Button>(buttonId) ?: return
        
        // Estado inicial
        if (settings.getValue(option) == value) {
            button.isSelected = true
        }
        
        button.setOnClickListener {
            settings.set(option, value)
            updateOptionButtons(option)
        }
    }
    
    private fun <T> updateOptionButtons(option: SettingsRepository.Option<T>) {
        val currentValue = settings.getValue(option)
        
        // Actualizar todos los botones de esta opción
        val buttonIds = when (option) {
            SettingsRepository.Option.Layout -> listOf(
                R.id.layout_azerty, R.id.layout_qwerty
            )
            SettingsRepository.Option.Numbers -> listOf(
                R.id.nums_visible, R.id.nums_longpress
            )
            SettingsRepository.Option.Accent -> listOf(
                R.id.accent_default, R.id.accent_accent, R.id.accent_details
            )
            SettingsRepository.Option.Bar -> listOf(
                R.id.bar_visible, R.id.bar_hidden
            )
            SettingsRepository.Option.Vibration -> listOf(
                R.id.vib_yes, R.id.vib_no
            )
            SettingsRepository.Option.Theme -> listOf(
                R.id.theme_bluerose, R.id.theme_oled, R.id.theme_thorn,
                R.id.theme_amethyst, R.id.theme_azure
            )
            else -> emptyList()
        }
        
        buttonIds.forEach { id ->
            findViewById<Button>(id)?.isSelected = false
        }
    }
    
    private fun setupPreview() {
        keyboardView = findViewById(R.id.keyboard_preview)
        previewSink = PreviewSink(findViewById(R.id.preview_text))
        
        keyboardView?.setKeySink(previewSink)
        keyboardView?.setKeyHeight(settings.keyHeight)
        rebuildLayout()
        updateTheme()
    }
    
    private fun rebuildLayout() {
        val layout = BlrsKeyboardPreset.generate(
            layout = settings.layout,
            showNumbers = settings.showNumbers,
            showBar = settings.showBar
        )
        
        keyboardView?.setLayout(layout)
    }
    
    private fun updateTheme() {
        var theme = BlrsDesign.fromName(settings.themeName)
        theme = theme.withAccentOption(settings.accentOption)
        
        keyboardView?.setTheme(theme)
        
        // Aplicar tema a la Activity también
        applyThemeToActivity(theme)
    }
    
    private fun updateKeyHeight() {
        keyboardView?.setKeyHeight(settings.keyHeight)
    }
    
    private fun applyThemeToActivity(theme: BlrsDesign) {
        window?.decorView?.setBackgroundColor(theme.bgPrimary)
        findViewById<View>(R.id.root_layout)?.setBackgroundColor(theme.bgPrimary)
    }
    
    /**
     * KeySink de prueba para la vista previa
     */
    private class PreviewSink(private val editText: EditText?) : KeySink {
        override fun commitText(text: String) {
            editText?.text?.append(text)
        }
        
        override fun deleteBeforeCursor(count: Int) {
            editText?.text?.let {
                if (it.length > 0) {
                    it.delete(it.length - count, it.length)
                }
            }
        }
        
        override fun deleteAfterCursor(count: Int) {}
        
        override fun setSelection(position: Int) {}
        
        override fun moveCursor(delta: Int) {}
        
        override fun extendSelection(delta: Int) {}
        
        override fun getCursorPosition(): Int = editText?.text?.length ?: 0
        
        override fun getSelection(): Pair<Int, Int> = Pair(0, 0)
        
        override fun sendKeyEvent(keyCode: Int, metaState: Int, isDown: Boolean) {}
        
        override fun performEditorAction(actionCode: Int) {
            editText?.text?.append("\n")
        }
        
        override fun getInputType(): Int = 0
        
        override fun getImeOptions(): Int = 0
        
        override fun onLayerChange(layerName: String) {}
        
        override fun vibrate(durationMs: Long) {}
    }
}
