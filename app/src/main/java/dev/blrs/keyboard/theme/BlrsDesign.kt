package dev.blrs.keyboard.theme

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import dev.blrs.keyboard.R

/**
 * Sistema de diseño BlrsDesign.
 * Dark-only. Tokens del HTML, con variantes de tema.
 * 
 * Los nombres de tokens coinciden con el HTML:
 * - bg-primary, bg-secondary, bg-tertiary, bg-overlay
 * - border-default, border-subtle, border-focus
 * - text-primary, text-secondary, text-tertiary, text-disabled
 * - accent, accent-secondary, accent-tertiary, accent-text
 * - error, success, warning, info
 * 
 * Geometría: radius-sm (2), radius-md (4), radius-panel (8)
 */
data class BlrsDesign(
    // Fondos (Obsidian)
    @ColorInt val bgPrimary: Int,
    @ColorInt val bgSecondary: Int,
    @ColorInt val bgTertiary: Int,
    @ColorInt val bgOverlay: Int,
    
    // Bordes
    @ColorInt val borderDefault: Int,
    @ColorInt val borderSubtle: Int,
    @ColorInt val borderFocus: Int,
    
    // Texto (Bloom)
    @ColorInt val textPrimary: Int,
    @ColorInt val textSecondary: Int,
    @ColorInt val textTertiary: Int,
    @ColorInt val textDisabled: Int,
    
    // Acento (Rose - true blue, NO CYAN)
    @ColorInt val accent: Int,
    @ColorInt val accentSecondary: Int,
    @ColorInt val accentTertiary: Int,
    @ColorInt val accentText: Int,
    
    // Señales
    @ColorInt val error: Int,
    @ColorInt val success: Int,
    @ColorInt val warning: Int,
    @ColorInt val info: Int,
    
    // Interacción
    @ColorInt val hover: Int,
    @ColorInt val focus: Int
) {
    /**
     * Obtiene colores con ajuste de acento aplicado.
     * 
     * Opciones de acento (cfg.ac en HTML):
     * - "c" (Default): sin tinte; el acento solo vive en la interacción
     * - "a" (Acento): barra superior y teclas especiales con tinte de acento al 14%
     * - "b" (Acento + detalles): lo de "a" + hints en accent-tertiary + borde 22%
     */
    fun withAccentOption(accentOption: String): BlrsDesign {
        return when (accentOption) {
            "a", "b" -> {
                // Tinte 14% sobre bg-secondary para teclas especiales
                val tintedSecondary = ColorUtils.blendARGB(bgSecondary, accent, 0.14f)
                // Borde 22% de acento para opción "b"
                val borderAccent = if (accentOption == "b") {
                    ColorUtils.blendARGB(Color.TRANSPARENT, accent, 0.22f)
                } else {
                    borderDefault
                }
                
                this.copy(
                    bgSecondary = tintedSecondary,
                    borderDefault = if (accentOption == "b") borderAccent else borderDefault
                )
            }
            else -> this // "c" (Default) sin cambios
        }
    }
    
    companion object {
        /**
         * Tema base BlueRose (default, sin overrides)
         */
        val BLUE_ROSE = BlrsDesign(
            bgPrimary = Color.parseColor("#FF121212"),
            bgSecondary = Color.parseColor("#FF1E1E1E"),
            bgTertiary = Color.parseColor("#FF2A2A2A"),
            bgOverlay = Color.parseColor("#FF1D1B33"),
            borderDefault = Color.parseColor("#FF333333"),
            borderSubtle = Color.parseColor("#FF444444"),
            borderFocus = Color.parseColor("#FF3E83F5"),
            textPrimary = Color.parseColor("#FFFAFAFA"),
            textSecondary = Color.parseColor("#FFE8E8E8"),
            textTertiary = Color.parseColor("#FFCCCCCC"),
            textDisabled = Color.parseColor("#FF504983"),
            accent = Color.parseColor("#FF2D68D8"),
            accentSecondary = Color.parseColor("#FF2452B8"),
            accentTertiary = Color.parseColor("#FF6FA7FF"),
            accentText = Color.parseColor("#FF0A0A0A"),
            error = Color.parseColor("#FFE8384F"),
            success = Color.parseColor("#FF2452B8"),
            warning = Color.parseColor("#FF6FA7FF"),
            info = Color.parseColor("#FF2D68D8"),
            hover = Color.parseColor("#FF6FA7FF"),
            focus = Color.parseColor("#FF3E83F5")
        )
        
        /**
         * Variante OLED
         */
        val OLED = BLUE_ROSE.copy(
            bgPrimary = Color.parseColor("#FF000000"),
            bgSecondary = Color.parseColor("#FF0A0A0A"),
            bgTertiary = Color.parseColor("#FF161616"),
            borderDefault = Color.parseColor("#FF262626")
        )
        
        /**
         * Variante Thorn
         */
        val THORN = BLUE_ROSE.copy(
            bgPrimary = Color.parseColor("#FF12111F"),
            bgSecondary = Color.parseColor("#FF1D1B33"),
            bgTertiary = Color.parseColor("#FF2C294A"),
            borderDefault = Color.parseColor("#FF3D3865"),
            textSecondary = Color.parseColor("#FFC9C6E8"),
            accent = Color.parseColor("#FF8A82C2"),
            accentSecondary = Color.parseColor("#FF6B61A8"),
            error = Color.parseColor("#FFD16B8A"),
            info = Color.parseColor("#FF8A82C2")
        )
        
        /**
         * Variante AmethystBreeze
         */
        val AMETHYST_BREEZE = BLUE_ROSE.copy(
            bgPrimary = Color.parseColor("#FF070712"),
            bgSecondary = Color.parseColor("#FF141427"),
            bgTertiary = Color.parseColor("#FF181832"),
            bgOverlay = Color.parseColor("#FF241A40"),
            borderDefault = Color.parseColor("#FF1C1A33"),
            borderSubtle = Color.parseColor("#FF1C1A33"),
            textPrimary = Color.parseColor("#FFF0EAFF"),
            textSecondary = Color.parseColor("#FF8B7BBD"),
            textTertiary = Color.parseColor("#FF5F5AA2"),
            textDisabled = Color.parseColor("#FF5F5AA2"),
            accent = Color.parseColor("#FF7C4DFF"),
            accentSecondary = Color.parseColor("#FF9A7CFF"),
            hover = Color.parseColor("#FF9A7CFF"),
            focus = Color.parseColor("#FF7C4DFF"),
            borderFocus = Color.parseColor("#FF7C4DFF"),
            info = Color.parseColor("#FF7C4DFF"),
            accentText = Color.parseColor("#FFF0EAFF")
        )
        
        /**
         * Variante AzureGuardian
         */
        val AZURE_GUARDIAN = BLUE_ROSE.copy(
            bgPrimary = Color.parseColor("#FF000814"),
            bgSecondary = Color.parseColor("#FF0A1426"),
            bgTertiary = Color.parseColor("#FF0A1426"),
            bgOverlay = Color.parseColor("#FF1A3352"),
            borderDefault = Color.parseColor("#FF1A2833"),
            borderSubtle = Color.parseColor("#FF1A2833"),
            textPrimary = Color.parseColor("#FFE6F2FF"),
            textSecondary = Color.parseColor("#FF4D6B8A"),
            textTertiary = Color.parseColor("#FF336699"),
            textDisabled = Color.parseColor("#FF336699"),
            accent = Color.parseColor("#FF0066CC"),
            accentSecondary = Color.parseColor("#FF6699FF"),
            hover = Color.parseColor("#FF6699FF"),
            focus = Color.parseColor("#FF0066CC"),
            borderFocus = Color.parseColor("#FF0066CC"),
            info = Color.parseColor("#FF0066CC"),
            accentText = Color.parseColor("#FFE6F2FF")
        )
        
        /**
         * Obtiene el tema por nombre
         */
        fun fromName(name: String): BlrsDesign {
            return when (name) {
                "oled" -> OLED
                "thorn" -> THORN
                "amethyst" -> AMETHYST_BREEZE
                "azure" -> AZURE_GUARDIAN
                else -> BLUE_ROSE // "bluerose" o default
            }
        }
        
        /**
         * Lista de nombres de temas disponibles
         */
        val themeNames = listOf("bluerose", "oled", "thorn", "amethyst", "azure")
    }
}
