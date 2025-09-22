package com.bsikar.helix.data

import android.content.Context
import androidx.compose.runtime.*
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.bsikar.helix.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val selectedReaderSettings: ReaderSettings = ReaderSettings(),
    val savedCustomPresets: List<ReaderPreset?> = listOf(null, null, null),
    val currentPresetName: String? = null,
    val lastSelectedPresetType: PresetType = PresetType.DEFAULT,
    val bookmarks: List<Bookmark> = emptyList()
)

enum class PresetType {
    DEFAULT,
    CUSTOM
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO)
    
    private val _preferences = mutableStateOf<UserPreferences?>(null)
    val preferences: State<UserPreferences> = derivedStateOf {
        _preferences.value ?: UserPreferences()
    }
    
    // Add a flag to ensure we've attempted to load preferences
    private var hasInitialLoad = false
    
    private object PreferenceKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val FONT_SIZE = intPreferencesKey("font_size")
        val LINE_HEIGHT = floatPreferencesKey("line_height")
        val BRIGHTNESS = floatPreferencesKey("brightness")
        val READING_MODE = stringPreferencesKey("reading_mode")
        val FONT_FAMILY = stringPreferencesKey("font_family")
        val TEXT_ALIGN = stringPreferencesKey("text_align")
        val MARGIN_HORIZONTAL = intPreferencesKey("margin_horizontal")
        val MARGIN_VERTICAL = intPreferencesKey("margin_vertical")
        val CURRENT_PRESET_NAME = stringPreferencesKey("current_preset_name")
        val LAST_SELECTED_PRESET_TYPE = stringPreferencesKey("last_selected_preset_type")
        
        val CUSTOM_PRESET_1_NAME = stringPreferencesKey("custom_preset_1_name")
        val CUSTOM_PRESET_1_FONT_SIZE = intPreferencesKey("custom_preset_1_font_size")
        val CUSTOM_PRESET_1_LINE_HEIGHT = floatPreferencesKey("custom_preset_1_line_height")
        val CUSTOM_PRESET_1_BRIGHTNESS = floatPreferencesKey("custom_preset_1_brightness")
        val CUSTOM_PRESET_1_READING_MODE = stringPreferencesKey("custom_preset_1_reading_mode")
        val CUSTOM_PRESET_1_FONT_FAMILY = stringPreferencesKey("custom_preset_1_font_family")
        val CUSTOM_PRESET_1_TEXT_ALIGN = stringPreferencesKey("custom_preset_1_text_align")
        val CUSTOM_PRESET_1_MARGIN_H = intPreferencesKey("custom_preset_1_margin_h")
        val CUSTOM_PRESET_1_MARGIN_V = intPreferencesKey("custom_preset_1_margin_v")
        
        val CUSTOM_PRESET_2_NAME = stringPreferencesKey("custom_preset_2_name")
        val CUSTOM_PRESET_2_FONT_SIZE = intPreferencesKey("custom_preset_2_font_size")
        val CUSTOM_PRESET_2_LINE_HEIGHT = floatPreferencesKey("custom_preset_2_line_height")
        val CUSTOM_PRESET_2_BRIGHTNESS = floatPreferencesKey("custom_preset_2_brightness")
        val CUSTOM_PRESET_2_READING_MODE = stringPreferencesKey("custom_preset_2_reading_mode")
        val CUSTOM_PRESET_2_FONT_FAMILY = stringPreferencesKey("custom_preset_2_font_family")
        val CUSTOM_PRESET_2_TEXT_ALIGN = stringPreferencesKey("custom_preset_2_text_align")
        val CUSTOM_PRESET_2_MARGIN_H = intPreferencesKey("custom_preset_2_margin_h")
        val CUSTOM_PRESET_2_MARGIN_V = intPreferencesKey("custom_preset_2_margin_v")
        
        val CUSTOM_PRESET_3_NAME = stringPreferencesKey("custom_preset_3_name")
        val CUSTOM_PRESET_3_FONT_SIZE = intPreferencesKey("custom_preset_3_font_size")
        val CUSTOM_PRESET_3_LINE_HEIGHT = floatPreferencesKey("custom_preset_3_line_height")
        val CUSTOM_PRESET_3_BRIGHTNESS = floatPreferencesKey("custom_preset_3_brightness")
        val CUSTOM_PRESET_3_READING_MODE = stringPreferencesKey("custom_preset_3_reading_mode")
        val CUSTOM_PRESET_3_FONT_FAMILY = stringPreferencesKey("custom_preset_3_font_family")
        val CUSTOM_PRESET_3_TEXT_ALIGN = stringPreferencesKey("custom_preset_3_text_align")
        val CUSTOM_PRESET_3_MARGIN_H = intPreferencesKey("custom_preset_3_margin_h")
        val CUSTOM_PRESET_3_MARGIN_V = intPreferencesKey("custom_preset_3_margin_v")
        
        val BOOKMARKS = stringPreferencesKey("bookmarks")
    }
    
    init {
        loadPreferences()
    }
    
    private fun loadPreferences() {
        scope.launch {
            try {
                context.dataStore.data.collect { preferences ->
                    updatePreferencesFromDataStore(preferences)
                    if (!hasInitialLoad) {
                        hasInitialLoad = true
                    }
                }
            } catch (e: Exception) {
                // If there's an error loading, initialize with defaults but preserve any existing theme
                e.printStackTrace()
                if (_preferences.value == null) {
                    _preferences.value = UserPreferences()
                    hasInitialLoad = true
                }
            }
        }
    }
    
    private fun updatePreferencesFromDataStore(preferences: Preferences) {
        val themeMode = try {
            ThemeMode.valueOf(
                preferences[PreferenceKeys.THEME_MODE] ?: ThemeMode.LIGHT.name
            )
        } catch (e: IllegalArgumentException) {
            // If somehow an invalid theme mode was saved, default to LIGHT
            ThemeMode.LIGHT
        }
        
        val readerSettings = ReaderSettings(
            fontSize = preferences[PreferenceKeys.FONT_SIZE] ?: 16,
            lineHeight = preferences[PreferenceKeys.LINE_HEIGHT] ?: 1.5f,
            brightness = preferences[PreferenceKeys.BRIGHTNESS] ?: 1.0f,
            readingMode = ReadingMode.valueOf(
                preferences[PreferenceKeys.READING_MODE] ?: ReadingMode.LIGHT.name
            ),
            fontFamily = preferences[PreferenceKeys.FONT_FAMILY] ?: "Default",
            textAlign = TextAlignment.valueOf(
                preferences[PreferenceKeys.TEXT_ALIGN] ?: TextAlignment.JUSTIFY.name
            ),
            marginHorizontal = preferences[PreferenceKeys.MARGIN_HORIZONTAL] ?: 24,
            marginVertical = preferences[PreferenceKeys.MARGIN_VERTICAL] ?: 16
        )
        
        val customPresets = listOf(
            loadCustomPreset(preferences, 1),
            loadCustomPreset(preferences, 2),
            loadCustomPreset(preferences, 3)
        )
        
        val currentPresetName = preferences[PreferenceKeys.CURRENT_PRESET_NAME]
        val lastSelectedPresetType = PresetType.valueOf(
            preferences[PreferenceKeys.LAST_SELECTED_PRESET_TYPE] ?: PresetType.DEFAULT.name
        )
        
        // Load bookmarks from DataStore
        val bookmarks = try {
            val bookmarksJson = preferences[PreferenceKeys.BOOKMARKS] ?: "[]"
            Json.decodeFromString<List<Bookmark>>(bookmarksJson)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList<Bookmark>()
        }
        
        _preferences.value = UserPreferences(
            themeMode = themeMode,
            selectedReaderSettings = readerSettings,
            savedCustomPresets = customPresets,
            currentPresetName = currentPresetName,
            lastSelectedPresetType = lastSelectedPresetType,
            bookmarks = bookmarks
        )
    }
    
    private fun loadCustomPreset(preferences: Preferences, slot: Int): ReaderPreset? {
        val nameKey = when (slot) {
            1 -> PreferenceKeys.CUSTOM_PRESET_1_NAME
            2 -> PreferenceKeys.CUSTOM_PRESET_2_NAME
            3 -> PreferenceKeys.CUSTOM_PRESET_3_NAME
            else -> return null
        }
        
        val name = preferences[nameKey] ?: return null
        
        val settings = when (slot) {
            1 -> ReaderSettings(
                fontSize = preferences[PreferenceKeys.CUSTOM_PRESET_1_FONT_SIZE] ?: 16,
                lineHeight = preferences[PreferenceKeys.CUSTOM_PRESET_1_LINE_HEIGHT] ?: 1.5f,
                brightness = preferences[PreferenceKeys.CUSTOM_PRESET_1_BRIGHTNESS] ?: 1.0f,
                readingMode = ReadingMode.valueOf(
                    preferences[PreferenceKeys.CUSTOM_PRESET_1_READING_MODE] ?: ReadingMode.LIGHT.name
                ),
                fontFamily = preferences[PreferenceKeys.CUSTOM_PRESET_1_FONT_FAMILY] ?: "Default",
                textAlign = TextAlignment.valueOf(
                    preferences[PreferenceKeys.CUSTOM_PRESET_1_TEXT_ALIGN] ?: TextAlignment.JUSTIFY.name
                ),
                marginHorizontal = preferences[PreferenceKeys.CUSTOM_PRESET_1_MARGIN_H] ?: 24,
                marginVertical = preferences[PreferenceKeys.CUSTOM_PRESET_1_MARGIN_V] ?: 16
            )
            2 -> ReaderSettings(
                fontSize = preferences[PreferenceKeys.CUSTOM_PRESET_2_FONT_SIZE] ?: 16,
                lineHeight = preferences[PreferenceKeys.CUSTOM_PRESET_2_LINE_HEIGHT] ?: 1.5f,
                brightness = preferences[PreferenceKeys.CUSTOM_PRESET_2_BRIGHTNESS] ?: 1.0f,
                readingMode = ReadingMode.valueOf(
                    preferences[PreferenceKeys.CUSTOM_PRESET_2_READING_MODE] ?: ReadingMode.LIGHT.name
                ),
                fontFamily = preferences[PreferenceKeys.CUSTOM_PRESET_2_FONT_FAMILY] ?: "Default",
                textAlign = TextAlignment.valueOf(
                    preferences[PreferenceKeys.CUSTOM_PRESET_2_TEXT_ALIGN] ?: TextAlignment.JUSTIFY.name
                ),
                marginHorizontal = preferences[PreferenceKeys.CUSTOM_PRESET_2_MARGIN_H] ?: 24,
                marginVertical = preferences[PreferenceKeys.CUSTOM_PRESET_2_MARGIN_V] ?: 16
            )
            3 -> ReaderSettings(
                fontSize = preferences[PreferenceKeys.CUSTOM_PRESET_3_FONT_SIZE] ?: 16,
                lineHeight = preferences[PreferenceKeys.CUSTOM_PRESET_3_LINE_HEIGHT] ?: 1.5f,
                brightness = preferences[PreferenceKeys.CUSTOM_PRESET_3_BRIGHTNESS] ?: 1.0f,
                readingMode = ReadingMode.valueOf(
                    preferences[PreferenceKeys.CUSTOM_PRESET_3_READING_MODE] ?: ReadingMode.LIGHT.name
                ),
                fontFamily = preferences[PreferenceKeys.CUSTOM_PRESET_3_FONT_FAMILY] ?: "Default",
                textAlign = TextAlignment.valueOf(
                    preferences[PreferenceKeys.CUSTOM_PRESET_3_TEXT_ALIGN] ?: TextAlignment.JUSTIFY.name
                ),
                marginHorizontal = preferences[PreferenceKeys.CUSTOM_PRESET_3_MARGIN_H] ?: 24,
                marginVertical = preferences[PreferenceKeys.CUSTOM_PRESET_3_MARGIN_V] ?: 16
            )
            else -> return null
        }
        
        return ReaderPreset(name = name, settings = settings, isCustom = true)
    }
    
    fun updateTheme(themeMode: ThemeMode) {
        // Update the in-memory state immediately to prevent flashing
        _preferences.value = _preferences.value?.copy(themeMode = themeMode) 
            ?: UserPreferences(themeMode = themeMode)
        
        // Then persist to DataStore
        scope.launch {
            try {
                context.dataStore.edit { preferences ->
                    preferences[PreferenceKeys.THEME_MODE] = themeMode.name
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // If saving fails, at least keep the in-memory state
            }
        }
    }
    
    fun updateReaderSettings(
        settings: ReaderSettings,
        presetName: String? = null,
        presetType: PresetType = PresetType.DEFAULT
    ) {
        scope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferenceKeys.FONT_SIZE] = settings.fontSize
                preferences[PreferenceKeys.LINE_HEIGHT] = settings.lineHeight
                preferences[PreferenceKeys.BRIGHTNESS] = settings.brightness
                preferences[PreferenceKeys.READING_MODE] = settings.readingMode.name
                preferences[PreferenceKeys.FONT_FAMILY] = settings.fontFamily
                preferences[PreferenceKeys.TEXT_ALIGN] = settings.textAlign.name
                preferences[PreferenceKeys.MARGIN_HORIZONTAL] = settings.marginHorizontal
                preferences[PreferenceKeys.MARGIN_VERTICAL] = settings.marginVertical
                
                if (presetName != null) {
                    preferences[PreferenceKeys.CURRENT_PRESET_NAME] = presetName
                } else {
                    preferences.remove(PreferenceKeys.CURRENT_PRESET_NAME)
                }
                preferences[PreferenceKeys.LAST_SELECTED_PRESET_TYPE] = presetType.name
            }
        }
    }
    
    fun saveCustomPreset(slot: Int, preset: ReaderPreset) {
        scope.launch {
            context.dataStore.edit { preferences ->
                when (slot) {
                    0 -> {
                        preferences[PreferenceKeys.CUSTOM_PRESET_1_NAME] = preset.name
                        preferences[PreferenceKeys.CUSTOM_PRESET_1_FONT_SIZE] = preset.settings.fontSize
                        preferences[PreferenceKeys.CUSTOM_PRESET_1_LINE_HEIGHT] = preset.settings.lineHeight
                        preferences[PreferenceKeys.CUSTOM_PRESET_1_BRIGHTNESS] = preset.settings.brightness
                        preferences[PreferenceKeys.CUSTOM_PRESET_1_READING_MODE] = preset.settings.readingMode.name
                        preferences[PreferenceKeys.CUSTOM_PRESET_1_FONT_FAMILY] = preset.settings.fontFamily
                        preferences[PreferenceKeys.CUSTOM_PRESET_1_TEXT_ALIGN] = preset.settings.textAlign.name
                        preferences[PreferenceKeys.CUSTOM_PRESET_1_MARGIN_H] = preset.settings.marginHorizontal
                        preferences[PreferenceKeys.CUSTOM_PRESET_1_MARGIN_V] = preset.settings.marginVertical
                    }
                    1 -> {
                        preferences[PreferenceKeys.CUSTOM_PRESET_2_NAME] = preset.name
                        preferences[PreferenceKeys.CUSTOM_PRESET_2_FONT_SIZE] = preset.settings.fontSize
                        preferences[PreferenceKeys.CUSTOM_PRESET_2_LINE_HEIGHT] = preset.settings.lineHeight
                        preferences[PreferenceKeys.CUSTOM_PRESET_2_BRIGHTNESS] = preset.settings.brightness
                        preferences[PreferenceKeys.CUSTOM_PRESET_2_READING_MODE] = preset.settings.readingMode.name
                        preferences[PreferenceKeys.CUSTOM_PRESET_2_FONT_FAMILY] = preset.settings.fontFamily
                        preferences[PreferenceKeys.CUSTOM_PRESET_2_TEXT_ALIGN] = preset.settings.textAlign.name
                        preferences[PreferenceKeys.CUSTOM_PRESET_2_MARGIN_H] = preset.settings.marginHorizontal
                        preferences[PreferenceKeys.CUSTOM_PRESET_2_MARGIN_V] = preset.settings.marginVertical
                    }
                    2 -> {
                        preferences[PreferenceKeys.CUSTOM_PRESET_3_NAME] = preset.name
                        preferences[PreferenceKeys.CUSTOM_PRESET_3_FONT_SIZE] = preset.settings.fontSize
                        preferences[PreferenceKeys.CUSTOM_PRESET_3_LINE_HEIGHT] = preset.settings.lineHeight
                        preferences[PreferenceKeys.CUSTOM_PRESET_3_BRIGHTNESS] = preset.settings.brightness
                        preferences[PreferenceKeys.CUSTOM_PRESET_3_READING_MODE] = preset.settings.readingMode.name
                        preferences[PreferenceKeys.CUSTOM_PRESET_3_FONT_FAMILY] = preset.settings.fontFamily
                        preferences[PreferenceKeys.CUSTOM_PRESET_3_TEXT_ALIGN] = preset.settings.textAlign.name
                        preferences[PreferenceKeys.CUSTOM_PRESET_3_MARGIN_H] = preset.settings.marginHorizontal
                        preferences[PreferenceKeys.CUSTOM_PRESET_3_MARGIN_V] = preset.settings.marginVertical
                    }
                }
            }
        }
    }
    
    fun loadCustomPresets(): List<ReaderPreset?> {
        return (_preferences.value ?: UserPreferences()).savedCustomPresets
    }
    
    fun getCurrentPresetInfo(): Pair<String?, PresetType> {
        val current = _preferences.value ?: UserPreferences()
        return current.currentPresetName to current.lastSelectedPresetType
    }
    
    fun isPresetActive(presetName: String, presetType: PresetType): Boolean {
        val current = _preferences.value ?: UserPreferences()
        return current.currentPresetName == presetName && current.lastSelectedPresetType == presetType
    }
    
    fun resetToDefaults() {
        val defaultSettings = ReaderPreset.getDefaultSettings()
        updateReaderSettings(defaultSettings, null, PresetType.DEFAULT)
    }
    
    fun hasInitialLoadCompleted(): Boolean {
        return hasInitialLoad
    }
    
    // Bookmark management functions
    fun getBookmarks(bookId: String? = null): List<Bookmark> {
        val bookmarks = (_preferences.value ?: UserPreferences()).bookmarks
        return if (bookId != null) {
            bookmarks.filter { it.bookId == bookId }.sortedByDescending { it.timestamp }
        } else {
            bookmarks.sortedByDescending { it.timestamp }
        }
    }
    
    fun addBookmark(bookmark: Bookmark) {
        val currentPrefs = _preferences.value ?: UserPreferences()
        val updatedBookmarks = currentPrefs.bookmarks.toMutableList()
        
        // Remove existing bookmark at same location
        updatedBookmarks.removeAll { 
            it.bookId == bookmark.bookId && 
            it.chapterNumber == bookmark.chapterNumber && 
            it.pageNumber == bookmark.pageNumber 
        }
        
        // Add new bookmark
        updatedBookmarks.add(bookmark)
        
        // Update state immediately
        _preferences.value = currentPrefs.copy(bookmarks = updatedBookmarks)
        
        // Persist to DataStore
        scope.launch {
            try {
                context.dataStore.edit { preferences ->
                    val bookmarksJson = Json.encodeToString(updatedBookmarks)
                    preferences[PreferenceKeys.BOOKMARKS] = bookmarksJson
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun removeBookmark(bookmarkId: String) {
        val currentPrefs = _preferences.value ?: UserPreferences()
        val updatedBookmarks = currentPrefs.bookmarks.filter { it.id != bookmarkId }
        
        // Update state immediately
        _preferences.value = currentPrefs.copy(bookmarks = updatedBookmarks)
        
        // Persist to DataStore
        scope.launch {
            try {
                context.dataStore.edit { preferences ->
                    val bookmarksJson = Json.encodeToString(updatedBookmarks)
                    preferences[PreferenceKeys.BOOKMARKS] = bookmarksJson
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun updateBookmarkNote(bookmarkId: String, note: String) {
        val currentPrefs = _preferences.value ?: UserPreferences()
        val updatedBookmarks = currentPrefs.bookmarks.map { bookmark ->
            if (bookmark.id == bookmarkId) {
                bookmark.copy(note = note)
            } else {
                bookmark
            }
        }
        
        // Update state immediately
        _preferences.value = currentPrefs.copy(bookmarks = updatedBookmarks)
        
        // Persist to DataStore
        scope.launch {
            try {
                context.dataStore.edit { preferences ->
                    val bookmarksJson = Json.encodeToString(updatedBookmarks)
                    preferences[PreferenceKeys.BOOKMARKS] = bookmarksJson
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun isPageBookmarked(bookId: String, chapterNumber: Int, pageNumber: Int): Boolean {
        return getBookmarks(bookId).any { 
            it.chapterNumber == chapterNumber && it.pageNumber == pageNumber 
        }
    }
}