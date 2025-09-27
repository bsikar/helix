package com.bsikar.helix.data.model

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
    val marginVertical: Int = 16 // Vertical margin in dp
)

enum class ReadingMode(val displayName: String, val backgroundColor: Color, val textColor: Color) {
    LIGHT("Light", Color(0xFFFFFFFF), Color(0xFF000000)),
    SEPIA("Sepia", Color(0xFFF7F3E9), Color(0xFF5D4E37)),
    DARK("Dark", Color(0xFF1C1C1E), Color(0xFFFFFFFF)),
    SYSTEM("System", Color(0xFFFFFFFF), Color(0xFF000000)) // Will adapt based on system theme
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
                    name = "Day",
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
                    name = "Night",
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
                )
            )
        }
        
        fun getDefaultSettings(): ReaderSettings {
            return ReaderSettings()
        }
    }
}