package com.bsikar.helix.data

import androidx.compose.runtime.*
import com.bsikar.helix.theme.ThemeMode

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val selectedReaderSettings: ReaderSettings = ReaderSettings(),
    val savedCustomPresets: List<ReaderPreset?> = listOf(null, null, null),
    val currentPresetName: String? = null, // Track which preset is currently active
    val lastSelectedPresetType: PresetType = PresetType.DEFAULT
)

enum class PresetType {
    DEFAULT, // Built-in presets like "Day Reading", "Night Mode", etc.
    CUSTOM   // User-saved custom presets
}

class UserPreferencesManager {
    companion object {
        // In a real app, this would use DataStore or SharedPreferences
        // For this demo, we'll use in-memory storage with remember
        
        private var _preferences = mutableStateOf(UserPreferences())
        val preferences: State<UserPreferences> = _preferences
        
        fun updateTheme(themeMode: ThemeMode) {
            _preferences.value = _preferences.value.copy(themeMode = themeMode)
        }
        
        fun updateReaderSettings(
            settings: ReaderSettings, 
            presetName: String? = null,
            presetType: PresetType = PresetType.DEFAULT
        ) {
            _preferences.value = _preferences.value.copy(
                selectedReaderSettings = settings,
                currentPresetName = presetName,
                lastSelectedPresetType = presetType
            )
        }
        
        fun saveCustomPreset(slot: Int, preset: ReaderPreset) {
            val newPresets = _preferences.value.savedCustomPresets.toMutableList()
            newPresets[slot] = preset
            _preferences.value = _preferences.value.copy(savedCustomPresets = newPresets)
        }
        
        fun loadCustomPresets(): List<ReaderPreset?> {
            return _preferences.value.savedCustomPresets
        }
        
        fun getCurrentPresetInfo(): Pair<String?, PresetType> {
            return _preferences.value.currentPresetName to _preferences.value.lastSelectedPresetType
        }
        
        fun isPresetActive(presetName: String, presetType: PresetType): Boolean {
            val current = _preferences.value
            return current.currentPresetName == presetName && current.lastSelectedPresetType == presetType
        }
        
        fun resetToDefaults() {
            val defaultSettings = ReaderPreset.getDefaultSettings()
            updateReaderSettings(defaultSettings, null, PresetType.DEFAULT)
        }
    }
}