package com.bsikar.helix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bsikar.helix.theme.AppTheme
import com.bsikar.helix.theme.ThemeManager
import com.bsikar.helix.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    theme: AppTheme,
    onBackClick: () -> Unit
) {
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
                        "Settings",
                        fontSize = 20.sp,
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
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Theme Section
            item {
                SettingsSection(title = "Appearance", theme = theme) {
                    ThemeSelector(
                        currentTheme = currentTheme,
                        onThemeChange = onThemeChange,
                        theme = theme
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
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
fun ThemeSelector(
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    theme: AppTheme
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ThemeMode.values().forEach { themeMode ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                RadioButton(
                    selected = currentTheme == themeMode,
                    onClick = { onThemeChange(themeMode) },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = theme.accentColor,
                        unselectedColor = theme.secondaryTextColor
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (themeMode) {
                        ThemeMode.LIGHT -> "Light"
                        ThemeMode.DARK -> "Dark"
                        ThemeMode.SYSTEM -> "System"
                    },
                    color = theme.primaryTextColor,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    theme: AppTheme
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = theme.primaryTextColor
        )
        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = theme.secondaryTextColor
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    val theme = ThemeManager.lightTheme
    MaterialTheme {
        SettingsScreen(
            currentTheme = ThemeMode.LIGHT,
            onThemeChange = { },
            theme = theme,
            onBackClick = { }
        )
    }
}