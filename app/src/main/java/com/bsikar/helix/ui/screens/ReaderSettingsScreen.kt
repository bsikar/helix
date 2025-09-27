package com.bsikar.helix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bsikar.helix.data.ReadingMode
import com.bsikar.helix.data.ReaderPreset
import com.bsikar.helix.data.ReaderSettings
import com.bsikar.helix.data.TextAlignment
import com.bsikar.helix.data.PresetType
import com.bsikar.helix.data.UserPreferencesManager
import com.bsikar.helix.theme.AppTheme
import com.bsikar.helix.theme.ThemeManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsScreen(
    settings: ReaderSettings,
    onSettingsChange: (ReaderSettings) -> Unit,
    theme: AppTheme,
    onBackClick: () -> Unit,
    preferencesManager: UserPreferencesManager
) {
    // Preset management state using UserPreferencesManager
    val savedPresets = preferencesManager.loadCustomPresets()
    var showOverrideDialog by remember { mutableStateOf(false) }
    var presetToOverride by remember { mutableIntStateOf(-1) }
    
    // Reactive state that automatically updates when preferences change
    val preferences by preferencesManager.preferences
    val currentActivePreset: Pair<String?, PresetType> = (preferences.currentPresetName to preferences.lastSelectedPresetType)
    
    // Helper function to clear active preset when manual changes are made
    fun clearActivePresetOnManualChange() {
        preferencesManager.updateReaderSettings(settings, null, PresetType.DEFAULT)
    }
    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = theme.surfaceColor,
                    titleContentColor = theme.primaryTextColor,
                ),
                title = { },
                actions = {
                    Text(
                        "Reading Settings",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = theme.primaryTextColor,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = theme.primaryTextColor
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(theme.backgroundColor),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Quick Presets Section (Default presets only)
            item {
                ReaderSettingsSection(title = "Quick Presets", theme = theme) {
                    QuickPresetManagement(
                        currentSettings = settings,
                        currentPresetInfo = currentActivePreset,
                        onLoadPreset = { preset, presetType ->
                            // Update settings
                            onSettingsChange(preset.settings)
                            // Save to preferences (state will update automatically via reactive flow)
                            preferencesManager.updateReaderSettings(
                                preset.settings, 
                                preset.name, 
                                presetType
                            )
                        },
                        onResetToDefaults = { 
                            // Update settings
                            onSettingsChange(ReaderPreset.getDefaultSettings())
                            // Save to preferences (state will update automatically via reactive flow)
                            preferencesManager.resetToDefaults()
                        },
                        theme = theme
                    )
                }
            }

            // Reading Mode Section
            item {
                ReaderSettingsSection(title = "Reading Mode", theme = theme) {
                    ReadingModeSelector(
                        currentMode = settings.readingMode,
                        onModeChange = { newMode ->
                            clearActivePresetOnManualChange()
                            onSettingsChange(settings.copy(readingMode = newMode))
                        },
                        theme = theme
                    )
                }
            }
            
            // Font Size Section
            item {
                ReaderSettingsSection(title = "Font Size", theme = theme) {
                    FontSizeSelector(
                        currentSize = settings.fontSize,
                        onSizeChange = { newSize ->
                            clearActivePresetOnManualChange()
                            onSettingsChange(settings.copy(fontSize = newSize))
                        },
                        theme = theme
                    )
                }
            }

            // Text Alignment Section
            item {
                ReaderSettingsSection(title = "Text Alignment", theme = theme) {
                    TextAlignmentSelector(
                        currentAlignment = settings.textAlign,
                        onAlignmentChange = { newAlignment ->
                            clearActivePresetOnManualChange()
                            onSettingsChange(settings.copy(textAlign = newAlignment))
                        },
                        theme = theme
                    )
                }
            }

            // Line Height Section
            item {
                ReaderSettingsSection(title = "Line Spacing", theme = theme) {
                    LineHeightSelector(
                        currentLineHeight = settings.lineHeight,
                        onLineHeightChange = { newLineHeight ->
                            clearActivePresetOnManualChange()
                            onSettingsChange(settings.copy(lineHeight = newLineHeight))
                        },
                        theme = theme
                    )
                }
            }

            // Brightness Section
            item {
                ReaderSettingsSection(title = "Brightness", theme = theme) {
                    BrightnessSelector(
                        currentBrightness = settings.brightness,
                        onBrightnessChange = { newBrightness ->
                            clearActivePresetOnManualChange()
                            onSettingsChange(settings.copy(brightness = newBrightness))
                        },
                        theme = theme
                    )
                }
            }

            // Margins Section
            item {
                ReaderSettingsSection(title = "Text Margins", theme = theme) {
                    MarginSelector(
                        currentHorizontalMargin = settings.marginHorizontal,
                        currentVerticalMargin = settings.marginVertical,
                        onMarginsChange = { horizontal, vertical ->
                            clearActivePresetOnManualChange()
                            onSettingsChange(settings.copy(
                                marginHorizontal = horizontal,
                                marginVertical = vertical
                            ))
                        },
                        theme = theme
                    )
                }
            }

            // Custom Presets Section (moved to bottom)
            item {
                ReaderSettingsSection(title = "Custom Presets", theme = theme) {
                    CustomPresetManagement(
                        currentSettings = settings,
                        savedPresets = savedPresets,
                        currentPresetInfo = currentActivePreset,
                        onLoadPreset = { preset, presetType ->
                            // Update settings
                            onSettingsChange(preset.settings)
                            // Save to preferences (state will update automatically via reactive flow)
                            preferencesManager.updateReaderSettings(
                                preset.settings, 
                                preset.name, 
                                presetType
                            )
                        },
                        onSavePreset = { slot ->
                            if (savedPresets[slot] != null) {
                                presetToOverride = slot
                                showOverrideDialog = true
                            } else {
                                val newPreset = ReaderPreset(
                                    name = "Custom ${slot + 1}",
                                    settings = settings
                                )
                                // Save to preferences (state will update automatically via reactive flow)
                                preferencesManager.saveCustomPreset(slot, newPreset)
                                preferencesManager.updateReaderSettings(
                                    settings,
                                    newPreset.name,
                                    PresetType.CUSTOM
                                )
                            }
                        },
                        theme = theme
                    )
                }
            }
        }
        
        // Override confirmation dialog
        if (showOverrideDialog) {
            AlertDialog(
                onDismissRequest = { showOverrideDialog = false },
                title = {
                    Text(
                        text = "Override Preset",
                        color = theme.primaryTextColor
                    )
                },
                text = {
                    Text(
                        text = "This will replace the existing preset \"${savedPresets[presetToOverride]?.name}\". Are you sure?",
                        color = theme.primaryTextColor
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val newPreset = ReaderPreset(
                                name = "Custom ${presetToOverride + 1}",
                                settings = settings
                            )
                            // Save to preferences (state will update automatically via reactive flow)
                            preferencesManager.saveCustomPreset(presetToOverride, newPreset)
                            preferencesManager.updateReaderSettings(
                                settings,
                                newPreset.name,
                                PresetType.CUSTOM
                            )
                            showOverrideDialog = false
                        }
                    ) {
                        Text("Override", color = theme.accentColor)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showOverrideDialog = false }
                    ) {
                        Text("Cancel", color = theme.secondaryTextColor)
                    }
                },
                containerColor = theme.surfaceColor
            )
        }
    }
}

@Composable
fun ReaderSettingsSection(
    title: String,
    theme: AppTheme,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = theme.accentColor,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = theme.surfaceColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun FontSizeSelector(
    currentSize: Int,
    onSizeChange: (Int) -> Unit,
    theme: AppTheme
) {
    val fontSizes = listOf(12, 14, 16, 18, 20, 22, 24, 26, 28)
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { 
                val currentIndex = fontSizes.indexOf(currentSize)
                if (currentIndex > 0) onSizeChange(fontSizes[currentIndex - 1])
            },
            enabled = currentSize > fontSizes.first()
        ) {
            Icon(
                Icons.Filled.Remove,
                contentDescription = "Decrease Font Size",
                tint = if (currentSize > fontSizes.first()) theme.primaryTextColor else theme.secondaryTextColor
            )
        }
        
        Text(
            text = "${currentSize}sp",
            fontSize = currentSize.sp,
            color = theme.primaryTextColor,
            fontWeight = FontWeight.Medium
        )
        
        IconButton(
            onClick = { 
                val currentIndex = fontSizes.indexOf(currentSize)
                if (currentIndex < fontSizes.lastIndex) onSizeChange(fontSizes[currentIndex + 1])
            },
            enabled = currentSize < fontSizes.last()
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "Increase Font Size",
                tint = if (currentSize < fontSizes.last()) theme.primaryTextColor else theme.secondaryTextColor
            )
        }
    }
}

@Composable
fun QuickPresetManagement(
    currentSettings: ReaderSettings,
    currentPresetInfo: Pair<String?, PresetType>,
    onLoadPreset: (ReaderPreset, PresetType) -> Unit,
    onResetToDefaults: () -> Unit,
    theme: AppTheme
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Default presets
        val defaultPresets = ReaderPreset.getDefaultPresets()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            defaultPresets.forEach { preset ->
                val isActive = currentPresetInfo.first == preset.name && 
                              currentPresetInfo.second == PresetType.DEFAULT
                OutlinedButton(
                    onClick = { onLoadPreset(preset, PresetType.DEFAULT) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isActive) theme.surfaceColor else theme.primaryTextColor,
                        containerColor = if (isActive) theme.accentColor else androidx.compose.ui.graphics.Color.Transparent
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, 
                        if (isActive) theme.accentColor else theme.secondaryTextColor.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = preset.name,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
        }
        
        HorizontalDivider(color = theme.secondaryTextColor.copy(alpha = 0.2f))
        
        // Reset to defaults
        OutlinedButton(
            onClick = onResetToDefaults,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = theme.secondaryTextColor
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp, 
                theme.secondaryTextColor.copy(alpha = 0.3f)
            )
        ) {
            Icon(
                Icons.Filled.Refresh,
                contentDescription = "Reset",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Reset to Defaults")
        }
    }
}

@Composable
fun CustomPresetManagement(
    currentSettings: ReaderSettings,
    savedPresets: List<ReaderPreset?>,
    currentPresetInfo: Pair<String?, PresetType>,
    onLoadPreset: (ReaderPreset, PresetType) -> Unit,
    onSavePreset: (Int) -> Unit,
    theme: AppTheme
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Custom preset slots
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            savedPresets.forEachIndexed { index, preset ->
                val isActive = preset != null && 
                              currentPresetInfo.first == preset.name && 
                              currentPresetInfo.second == PresetType.CUSTOM
                PresetSlot(
                    slot = index + 1,
                    preset = preset,
                    isActive = isActive,
                    onLoad = { onLoadPreset(it, PresetType.CUSTOM) },
                    onSave = { onSavePreset(index) },
                    theme = theme,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun PresetManagement(
    currentSettings: ReaderSettings,
    savedPresets: List<ReaderPreset?>,
    currentPresetInfo: Pair<String?, PresetType>,
    onLoadPreset: (ReaderPreset, PresetType) -> Unit,
    onSavePreset: (Int) -> Unit,
    onResetToDefaults: () -> Unit,
    theme: AppTheme
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Default presets
        Text(
            text = "Quick Presets",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = theme.primaryTextColor
        )
        
        val defaultPresets = ReaderPreset.getDefaultPresets()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            defaultPresets.forEach { preset ->
                val isActive = currentPresetInfo.first == preset.name && 
                              currentPresetInfo.second == PresetType.DEFAULT
                OutlinedButton(
                    onClick = { onLoadPreset(preset, PresetType.DEFAULT) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isActive) theme.surfaceColor else theme.primaryTextColor,
                        containerColor = if (isActive) theme.accentColor else androidx.compose.ui.graphics.Color.Transparent
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, 
                        if (isActive) theme.accentColor else theme.secondaryTextColor.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = preset.name,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
        }
        
        HorizontalDivider(color = theme.secondaryTextColor.copy(alpha = 0.2f))
        
        // Custom preset slots
        Text(
            text = "Custom Presets",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = theme.primaryTextColor
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            savedPresets.forEachIndexed { index, preset ->
                val isActive = preset != null && 
                              currentPresetInfo.first == preset.name && 
                              currentPresetInfo.second == PresetType.CUSTOM
                PresetSlot(
                    slot = index + 1,
                    preset = preset,
                    isActive = isActive,
                    onLoad = { onLoadPreset(it, PresetType.CUSTOM) },
                    onSave = { onSavePreset(index) },
                    theme = theme,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        HorizontalDivider(color = theme.secondaryTextColor.copy(alpha = 0.2f))
        
        // Reset to defaults
        OutlinedButton(
            onClick = onResetToDefaults,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = theme.secondaryTextColor
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp, 
                theme.secondaryTextColor.copy(alpha = 0.3f)
            )
        ) {
            Icon(
                Icons.Filled.Refresh,
                contentDescription = "Reset",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Reset to Defaults")
        }
    }
}

@Composable
fun PresetSlot(
    slot: Int,
    preset: ReaderPreset?,
    isActive: Boolean,
    onLoad: (ReaderPreset) -> Unit,
    onSave: () -> Unit,
    theme: AppTheme,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) theme.accentColor.copy(alpha = 0.1f) else theme.backgroundColor
        ),
        border = androidx.compose.foundation.BorderStroke(
            2.dp, 
            if (isActive) theme.accentColor else theme.secondaryTextColor.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Slot $slot",
                fontSize = 12.sp,
                color = theme.secondaryTextColor,
                fontWeight = FontWeight.Medium
            )
            
            if (preset != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = preset.name,
                        fontSize = 10.sp,
                        color = theme.primaryTextColor,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { onLoad(preset) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = "Load",
                                tint = theme.accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = onSave,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Filled.Save,
                                contentDescription = "Save",
                                tint = theme.primaryTextColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            } else {
                IconButton(
                    onClick = onSave,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Save Preset",
                        tint = theme.accentColor
                    )
                }
            }
        }
    }
}

@Composable
fun ReadingModeSelector(
    currentMode: ReadingMode,
    onModeChange: (ReadingMode) -> Unit,
    theme: AppTheme
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ReadingMode.values().forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .selectable(
                        selected = currentMode == mode,
                        onClick = { onModeChange(mode) },
                        role = Role.RadioButton
                    )
                    .background(
                        if (currentMode == mode) theme.accentColor.copy(alpha = 0.1f)
                        else theme.backgroundColor
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = currentMode == mode,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = theme.accentColor,
                        unselectedColor = theme.secondaryTextColor
                    )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(mode.backgroundColor)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = mode.displayName,
                    color = theme.primaryTextColor,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun TextAlignmentSelector(
    currentAlignment: TextAlignment,
    onAlignmentChange: (TextAlignment) -> Unit,
    theme: AppTheme
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        TextAlignment.values().forEach { alignment ->
            FilterChip(
                onClick = { onAlignmentChange(alignment) },
                label = {
                    Text(
                        text = alignment.displayName,
                        fontSize = 12.sp,
                        color = if (currentAlignment == alignment) {
                            theme.surfaceColor
                        } else {
                            theme.primaryTextColor
                        }
                    )
                },
                selected = currentAlignment == alignment,
                enabled = true,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = theme.accentColor,
                    containerColor = theme.surfaceColor,
                    selectedLabelColor = theme.surfaceColor,
                    labelColor = theme.primaryTextColor
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = theme.secondaryTextColor.copy(alpha = 0.3f),
                    selectedBorderColor = theme.accentColor,
                    enabled = true,
                    selected = currentAlignment == alignment
                )
            )
        }
    }
}

@Composable
fun LineHeightSelector(
    currentLineHeight: Float,
    onLineHeightChange: (Float) -> Unit,
    theme: AppTheme
) {
    val lineHeights = listOf(1.0f, 1.2f, 1.4f, 1.5f, 1.6f, 1.8f, 2.0f)
    
    Column {
        Text(
            text = "Spacing: ${String.format("%.1f", currentLineHeight)}x",
            fontSize = 14.sp,
            color = theme.primaryTextColor,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = currentLineHeight,
            onValueChange = { value ->
                val closest = lineHeights.minByOrNull { kotlin.math.abs(it - value) } ?: currentLineHeight
                onLineHeightChange(closest)
            },
            valueRange = 1.0f..2.0f,
            colors = SliderDefaults.colors(
                thumbColor = theme.accentColor,
                activeTrackColor = theme.accentColor,
                inactiveTrackColor = theme.secondaryTextColor.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
fun BrightnessSelector(
    currentBrightness: Float,
    onBrightnessChange: (Float) -> Unit,
    theme: AppTheme
) {
    Column {
        Text(
            text = "Brightness: ${(currentBrightness * 100).toInt()}%",
            fontSize = 14.sp,
            color = theme.primaryTextColor,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = currentBrightness,
            onValueChange = onBrightnessChange,
            valueRange = 0.2f..1.0f,
            colors = SliderDefaults.colors(
                thumbColor = theme.accentColor,
                activeTrackColor = theme.accentColor,
                inactiveTrackColor = theme.secondaryTextColor.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
fun MarginSelector(
    currentHorizontalMargin: Int,
    currentVerticalMargin: Int,
    onMarginsChange: (Int, Int) -> Unit,
    theme: AppTheme
) {
    val marginOptions = listOf(16, 20, 24, 28, 32, 40, 48)
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Horizontal margin
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Side margins:",
                fontSize = 14.sp,
                color = theme.primaryTextColor
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { 
                        val currentIndex = marginOptions.indexOf(currentHorizontalMargin)
                        if (currentIndex > 0) {
                            onMarginsChange(marginOptions[currentIndex - 1], currentVerticalMargin)
                        }
                    },
                    enabled = currentHorizontalMargin > marginOptions.first()
                ) {
                    Icon(
                        Icons.Filled.Remove,
                        contentDescription = "Decrease Horizontal Margin",
                        tint = if (currentHorizontalMargin > marginOptions.first()) theme.primaryTextColor else theme.secondaryTextColor
                    )
                }
                Text(
                    text = "${currentHorizontalMargin}dp",
                    fontSize = 14.sp,
                    color = theme.primaryTextColor,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(
                    onClick = { 
                        val currentIndex = marginOptions.indexOf(currentHorizontalMargin)
                        if (currentIndex < marginOptions.lastIndex) {
                            onMarginsChange(marginOptions[currentIndex + 1], currentVerticalMargin)
                        }
                    },
                    enabled = currentHorizontalMargin < marginOptions.last()
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Increase Horizontal Margin",
                        tint = if (currentHorizontalMargin < marginOptions.last()) theme.primaryTextColor else theme.secondaryTextColor
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReaderSettingsScreenPreview() {
    val theme = ThemeManager.lightTheme
    var settings by remember { mutableStateOf(ReaderSettings()) }
    
    MaterialTheme {
        ReaderSettingsScreen(
            settings = settings,
            onSettingsChange = { settings = it },
            theme = theme,
            onBackClick = { },
            preferencesManager = TODO("Preview requires dependency injection")
        )
    }
}