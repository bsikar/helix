package com.bsikar.helix.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bsikar.helix.theme.AppTheme
import com.bsikar.helix.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    theme: AppTheme,
    onClose: () -> Unit
) {
    val themeOptions = listOf(
        ThemeMode.LIGHT,
        ThemeMode.DARK,
        ThemeMode.SEPIA,
        ThemeMode.WARM,
        ThemeMode.COOL,
        ThemeMode.SYSTEM,
        ThemeMode.DYNAMIC
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = theme.primaryTextColor) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = theme.primaryTextColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.surfaceColor)
            )
        },
        containerColor = theme.backgroundColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium,
                color = theme.primaryTextColor
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                themeOptions.forEach { option ->
                    FilterChip(
                        selected = option == currentTheme,
                        onClick = { onThemeChange(option) },
                        label = { Text(option.displayName()) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = theme.accentColor,
                            selectedLabelColor = theme.backgroundColor
                        )
                    )
                }
            }

            Text(
                text = "Need more controls?",
                style = MaterialTheme.typography.titleMedium,
                color = theme.primaryTextColor
            )
            Text(
                text = "Head to the import manager on your device to add new books and audiobooks. Watched directories will appear here automatically after a scan.",
                style = MaterialTheme.typography.bodyMedium,
                color = theme.secondaryTextColor
            )

            TextButton(onClick = onClose) {
                Text("Close", color = theme.accentColor)
            }
        }
    }
}

private fun ThemeMode.displayName(): String = when (this) {
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
    ThemeMode.SEPIA -> "Sepia"
    ThemeMode.WARM -> "Warm"
    ThemeMode.COOL -> "Cool"
    ThemeMode.HIGH_CONTRAST -> "High contrast"
    ThemeMode.NIGHT_MODE -> "Night"
    ThemeMode.SYSTEM -> "System"
    ThemeMode.DYNAMIC -> "Dynamic"
}
