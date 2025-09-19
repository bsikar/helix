package com.bsikar.helix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bsikar.helix.data.Book
import com.bsikar.helix.data.ReaderSettings
import com.bsikar.helix.data.ReadingMode
import com.bsikar.helix.data.TextAlignment
import com.bsikar.helix.data.UserPreferencesManager
import com.bsikar.helix.theme.AppTheme
import com.bsikar.helix.theme.ThemeManager
import com.bsikar.helix.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    book: Book,
    theme: AppTheme,
    onBackClick: () -> Unit
) {
    var currentPage by remember { mutableIntStateOf(1) }
    val totalPages = 150 // Sample total pages
    var showSettings by remember { mutableStateOf(false) }
    
    // Use persistent reader settings from UserPreferencesManager
    val userPreferences by UserPreferencesManager.preferences
    var readerSettings by remember { mutableStateOf(userPreferences.selectedReaderSettings) }
    
    // Update settings when preferences change
    LaunchedEffect(userPreferences.selectedReaderSettings) {
        readerSettings = userPreferences.selectedReaderSettings
    }
    
    // Sample book content
    val sampleContent = """
        Chapter 1: The Beginning
        
        Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.
        
        Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
        
        Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo.
        
        Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt.
        
        Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit, sed quia non numquam eius modi tempora incidunt ut labore et dolore magnam aliquam quaerat voluptatem.
        
        Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid ex ea commodi consequatur? Quis autem vel eum iure reprehenderit qui in ea voluptate velit esse quam nihil molestiae consequatur.
        
        At vero eos et accusamus et iusto odio dignissimos ducimus qui blanditiis praesentium voluptatum deleniti atque corrupti quos dolores et quas molestias excepturi sint occaecati cupiditate non provident.
    """.trimIndent()

    if (showSettings) {
        ReaderSettingsScreen(
            settings = readerSettings,
            onSettingsChange = { newSettings -> 
                readerSettings = newSettings
                UserPreferencesManager.updateReaderSettings(newSettings)
            },
            theme = theme,
            onBackClick = { showSettings = false }
        )
        return
    }
    
    // Apply brightness to background color
    val adjustedBackgroundColor = applyBrightness(readerSettings.readingMode.backgroundColor, readerSettings.brightness)

    Scaffold(
        containerColor = adjustedBackgroundColor,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = theme.backgroundColor,
                    titleContentColor = theme.primaryTextColor,
                ),
                title = {
                    Column {
                        Text(
                            text = book.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = theme.primaryTextColor,
                            maxLines = 1
                        )
                        Text(
                            text = book.author,
                            fontSize = 12.sp,
                            color = theme.secondaryTextColor,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = theme.primaryTextColor
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(
                            Icons.Filled.Bookmark,
                            contentDescription = "Bookmark",
                            tint = theme.secondaryTextColor
                        )
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "Reading Settings",
                            tint = theme.secondaryTextColor
                        )
                    }
                }
            )
        },
        bottomBar = {
            ReaderBottomBar(
                currentPage = currentPage,
                totalPages = totalPages,
                progress = book.progress,
                onPreviousPage = { if (currentPage > 1) currentPage-- },
                onNextPage = { if (currentPage < totalPages) currentPage++ },
                theme = theme
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = { currentPage.toFloat() / totalPages },
                modifier = Modifier.fillMaxWidth(),
                color = theme.accentColor,
                trackColor = theme.secondaryTextColor.copy(alpha = 0.2f)
            )
            
            // Content area
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = readerSettings.marginHorizontal.dp,
                        vertical = readerSettings.marginVertical.dp
                    )
            ) {
                Text(
                    text = sampleContent,
                    fontSize = readerSettings.fontSize.sp,
                    lineHeight = (readerSettings.fontSize * readerSettings.lineHeight).sp,
                    color = readerSettings.readingMode.textColor,
                    textAlign = when (readerSettings.textAlign) {
                        TextAlignment.LEFT -> TextAlign.Start
                        TextAlignment.CENTER -> TextAlign.Center
                        TextAlignment.JUSTIFY -> TextAlign.Justify
                    }
                )
            }
        }
    }
}

@Composable
fun ReaderBottomBar(
    currentPage: Int,
    totalPages: Int,
    progress: Float,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    theme: AppTheme
) {
    Surface(
        color = theme.surfaceColor,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous page button
            IconButton(
                onClick = onPreviousPage,
                enabled = currentPage > 1
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous Page",
                    tint = if (currentPage > 1) theme.primaryTextColor else theme.secondaryTextColor
                )
            }
            
            // Page info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$currentPage of $totalPages",
                    fontSize = 14.sp,
                    color = theme.primaryTextColor,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${(progress * 100).toInt()}% complete",
                    fontSize = 12.sp,
                    color = theme.secondaryTextColor
                )
            }
            
            // Next page button
            IconButton(
                onClick = onNextPage,
                enabled = currentPage < totalPages
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next Page",
                    tint = if (currentPage < totalPages) theme.primaryTextColor else theme.secondaryTextColor
                )
            }
        }
    }
}

// Helper function to apply brightness to colors
@Composable
fun applyBrightness(color: Color, brightness: Float): Color {
    val argb = color.toArgb()
    val alpha = (argb shr 24) and 0xFF
    val red = ((argb shr 16) and 0xFF) * brightness
    val green = ((argb shr 8) and 0xFF) * brightness
    val blue = (argb and 0xFF) * brightness
    
    return Color(
        red = (red.coerceIn(0f, 255f) / 255f),
        green = (green.coerceIn(0f, 255f) / 255f),
        blue = (blue.coerceIn(0f, 255f) / 255f),
        alpha = alpha / 255f
    )
}

@Preview(showBackground = true)
@Composable
fun ReaderScreenPreview() {
    val theme = ThemeManager.lightTheme
    val sampleBook = Book(
        title = "Clockwork Planet",
        author = "Yuu Kamiya",
        coverColor = androidx.compose.ui.graphics.Color(0xFFFFD700),
        progress = 0.3f
    )
    
    MaterialTheme {
        ReaderScreen(
            book = sampleBook,
            theme = theme,
            onBackClick = { }
        )
    }
}