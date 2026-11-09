package dev.blrs.keyboard.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import dev.blrs.keyboard.model.BlrsKeyboardPreset

/**
 * Repositorio de ajustes.
 * Única fuente de verdad, consumida tanto por la Activity como por el IME.
 * 
 * Defaults (del HTML):
 * - Distribución: AZERTY
 * - Números: Fila visible
 * - Acento: "c" (Default)
 * - Barra: Visible
 * - Vibración: Sí
 * - Tema: BlueRose
 * - Alto de tecla: 46dp
 */
class SettingsRepository private constructor(context: Context) {
    
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    
    /**
     * Definición declarativa de opciones (equivalente a OP en HTML)
     */
    sealed class Option<T>(val key: String, val defaultValue: T) {
        object Layout : Option<String>("ly", "azerty") {
            val values = listOf("azerty" to "AZERTY", "qwerty" to "QWERTY")
        }
        
        object Numbers : Option<Boolean>("nums", true) {
            val values = listOf(true to "Fila visible", false to "Pulsación larga")
        }
        
        object Accent : Option<String>("ac", "c") {
            val values = listOf(
                "c" to "Default",
                "a" to "Acento",
                "b" to "Acento + detalles"
            )
        }
        
        object Bar : Option<Boolean>("bar", true) {
            val values = listOf(true to "Visible", false to "Oculta")
        }
        
        object Vibration : Option<Boolean>("vib", true) {
            val values = listOf(true to "Sí", false to "No")
        }
        
        object Theme : Option<String>("theme", "bluerose") {
            val values = listOf(
                "bluerose" to "BlueRose",
                "oled" to "OLED",
                "thorn" to "Thorn",
                "amethyst" to "AmethystBreeze",
                "azure" to "AzureGuardian"
            )
        }
        
        object KeyHeight : Option<Int>("kh", 46)
    }
    
    // Valores actuales (con defaults aplicados)
    val layout: BlrsKeyboardPreset.LayoutType
        get() = when (getString(Option.Layout)) {
            "qwerty" -> BlrsKeyboardPreset.LayoutType.QWERTY
            else -> BlrsKeyboardPreset.LayoutType.AZERTY
        }
    
    val showNumbers: Boolean
        get() = getBoolean(Option.Numbers)
    
    val accentOption: String
        get() = getString(Option.Accent)
    
    val showBar: Boolean
        get() = getBoolean(Option.Bar)
    
    val vibrationEnabled: Boolean
        get() = getBoolean(Option.Vibration)
    
    val themeName: String
        get() = getString(Option.Theme)
    
    val keyHeight: Int
        get() = getInt(Option.KeyHeight)
    
    // Métodos de acceso genéricos
    @Suppress("UNCHECKED_CAST")
    private fun <T> get(option: Option<T>): T {
        return when (option.defaultValue) {
            is String -> prefs.getString(option.key, option.defaultValue as String) as T
            is Boolean -> prefs.getBoolean(option.key, option.defaultValue as Boolean) as T
            is Int -> prefs.getInt(option.key, option.defaultValue as Int) as T
            else -> throw IllegalArgumentException("Unsupported option type")
        }
    }
    
    fun <T> getValue(option: Option<T>): T = get(option)
    
    fun getString(option: Option<String>): String = get(option)
    
    fun getBoolean(option: Option<Boolean>): Boolean = get(option)
    
    fun getInt(option: Option<Int>): Int = get(option)
    
    // Setters
    fun <T> set(option: Option<T>, value: T) {
        prefs.edit().apply {
            when (value) {
                is String -> putString(option.key, value)
                is Boolean -> putBoolean(option.key, value)
                is Int -> putInt(option.key, value)
                else -> throw IllegalArgumentException("Unsupported option type")
            }
            apply()
        }
    }
    
    // Listener para cambios en tiempo real
    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }
    
    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }
    
    companion object {
        @Volatile
        private var instance: SettingsRepository? = null
        
        fun getInstance(context: Context): SettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: SettingsRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
