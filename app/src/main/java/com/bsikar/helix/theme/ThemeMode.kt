package com.bsikar.helix.theme

enum class ThemeMode(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("Follow System"),
    DYNAMIC("Dynamic Colors"),
    // Reader-specific themes
    SEPIA("Sepia"),
    HIGH_CONTRAST("High Contrast"),
    NIGHT_MODE("Night Mode"),
    WARM("Warm"),
    COOL("Cool")
}