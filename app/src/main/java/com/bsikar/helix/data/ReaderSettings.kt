package com.bsikar.helix.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

data class ReaderSettings(
    val fontSize: Int = 16, // Font size in sp
    val lineHeight: Float = 1.5f, // Line height multiplier
    val brightness: Float = 1.0f, // Brightness level (0.0 to 1.0)
    val readingMode: ReadingMode = ReadingMode.LIGHT,
    val fontFamily: String = "Default",
    val textAlign: TextAlignment = TextAlignment.JUSTIFY,
    val marginHorizontal: Int = 24, // Horizontal margin in dp
    val marginVertical: Int = 16, // Vertical margin in dp
    // Accessibility features
    val useSystemFontSize: Boolean = false, // Follow system font size
    val highContrast: Boolean = false, // High contrast mode
    val letterSpacing: Float = 0.0f, // Letter spacing in em units
    val wordSpacing: Float = 1.0f, // Word spacing multiplier
    val animationsEnabled: Boolean = true, // Enable/disable animations
    val useSystemTheme: Boolean = false // Follow system dark/light theme
)

enum class ReadingMode(val displayName: String, val backgroundColor: Color, val textColor: Color, val isHighContrast: Boolean = false) {
    LIGHT("Light", Color(0xFFFFFFFF), Color(0xFF000000)),
    SEPIA("Sepia", Color(0xFFF7F3E9), Color(0xFF5D4E37)),
    DARK("Dark", Color(0xFF1C1C1E), Color(0xFFFFFFFF)),
    BLACK("Black", Color(0xFF000000), Color(0xFFFFFFFF)),
    // High contrast modes for accessibility
    HIGH_CONTRAST_LIGHT("High Contrast Light", Color(0xFFFFFFFF), Color(0xFF000000), true),
    HIGH_CONTRAST_DARK("High Contrast Dark", Color(0xFF000000), Color(0xFFFFFFFF), true),
    HIGH_CONTRAST_YELLOW("High Contrast Yellow", Color(0xFFFFFF00), Color(0xFF000000), true)
}

enum class TextAlignment(val displayName: String) {
    LEFT("Left"),
    CENTER("Center"),
    JUSTIFY("Justify")
}

data class ReaderPreset(
    val name: String,
    val settings: ReaderSettings,
    val isCustom: Boolean = true
) {
    companion object {
        fun getDefaultPresets(): List<ReaderPreset> {
            return listOf(
                ReaderPreset(
                    name = "Day Reading",
                    settings = ReaderSettings(
                        fontSize = 16,
                        lineHeight = 1.5f,
                        brightness = 1.0f,
                        readingMode = ReadingMode.LIGHT,
                        textAlign = TextAlignment.JUSTIFY,
                        marginHorizontal = 24,
                        marginVertical = 16
                    ),
                    isCustom = false
                ),
                ReaderPreset(
                    name = "Night Mode",
                    settings = ReaderSettings(
                        fontSize = 18,
                        lineHeight = 1.6f,
                        brightness = 0.8f,
                        readingMode = ReadingMode.DARK,
                        textAlign = TextAlignment.JUSTIFY,
                        marginHorizontal = 28,
                        marginVertical = 20
                    ),
                    isCustom = false
                ),
                ReaderPreset(
                    name = "Comfort Reading",
                    settings = ReaderSettings(
                        fontSize = 20,
                        lineHeight = 1.8f,
                        brightness = 0.9f,
                        readingMode = ReadingMode.SEPIA,
                        textAlign = TextAlignment.JUSTIFY,
                        marginHorizontal = 32,
                        marginVertical = 24
                    ),
                    isCustom = false
                ),
                // Accessibility presets
                ReaderPreset(
                    name = "Large Text",
                    settings = ReaderSettings(
                        fontSize = 24,
                        lineHeight = 2.0f,
                        brightness = 1.0f,
                        readingMode = ReadingMode.LIGHT,
                        textAlign = TextAlignment.LEFT,
                        marginHorizontal = 40,
                        marginVertical = 32,
                        letterSpacing = 0.05f,
                        wordSpacing = 1.2f
                    ),
                    isCustom = false
                ),
                ReaderPreset(
                    name = "High Contrast",
                    settings = ReaderSettings(
                        fontSize = 18,
                        lineHeight = 1.8f,
                        brightness = 1.0f,
                        readingMode = ReadingMode.HIGH_CONTRAST_LIGHT,
                        textAlign = TextAlignment.LEFT,
                        marginHorizontal = 32,
                        marginVertical = 24,
                        highContrast = true,
                        letterSpacing = 0.02f,
                        animationsEnabled = false
                    ),
                    isCustom = false
                ),
                ReaderPreset(
                    name = "Dyslexia Friendly",
                    settings = ReaderSettings(
                        fontSize = 20,
                        lineHeight = 2.2f,
                        brightness = 0.95f,
                        readingMode = ReadingMode.SEPIA,
                        textAlign = TextAlignment.LEFT,
                        marginHorizontal = 48,
                        marginVertical = 36,
                        letterSpacing = 0.08f,
                        wordSpacing = 1.5f,
                        fontFamily = "OpenDyslexic"
                    ),
                    isCustom = false
                )
            )
        }
        
        fun getDefaultSettings(): ReaderSettings {
            return ReaderSettings()
        }
    }
}