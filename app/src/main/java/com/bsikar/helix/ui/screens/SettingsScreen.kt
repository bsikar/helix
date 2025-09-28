package com.bsikar.helix.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bsikar.helix.data.ImportProgress
import com.bsikar.helix.data.ImportResult
import com.bsikar.helix.data.ImportFailure
import com.bsikar.helix.data.LibraryManager
import com.bsikar.helix.managers.ImportManager
import com.bsikar.helix.theme.AppTheme
import com.bsikar.helix.theme.ThemeManager
import com.bsikar.helix.theme.ThemeMode
import kotlinx.coroutines.launch
import java.io.File

private object SettingsScreenConstants {
    const val THEME_SECTION_INDEX = 0
    const val LIBRARY_MANAGEMENT_SECTION_INDEX = 1
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    theme: AppTheme,
    onBackClick: () -> Unit,
    libraryManager: LibraryManager? = null,
    scrollToSection: String? = null,
    importManager: ImportManager? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var importMessage by remember { mutableStateOf("") }
    
    // Get import progress from LibraryManager - avoid state delegation issues
    val importProgress = libraryManager?.importProgress?.value
    val lastImportResult = libraryManager?.lastImportResult?.value
    val isLoading = libraryManager?.isLoading?.value ?: false
    
    // Scroll state for auto-scrolling to sections
    val listState = rememberLazyListState()
    
    // Auto-scroll to specific section when requested
    LaunchedEffect(scrollToSection) {
        if (scrollToSection == "progress" && (importProgress != null || importMessage.isNotEmpty())) {
            // Calculate the index of the Library Management section dynamically
            if (libraryManager != null) {
                // If libraryManager exists, Library Management section is at this index
                listState.animateScrollToItem(SettingsScreenConstants.LIBRARY_MANAGEMENT_SECTION_INDEX)
            }
        }
    }
    
    // Don't auto-navigate back to library anymore - user prefers to stay in settings
    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = theme.surfaceColor,
                    titleContentColor = theme.primaryTextColor,
                ),
                title = {
                    Text(
                        "Settings",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = theme.primaryTextColor
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
            state = listState,
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
            
            // Library Management Section
            if (libraryManager != null) {
                item {
                    SettingsSection(title = "Library Management", theme = theme) {
                        LibraryManagementOptions(
                            libraryManager = libraryManager,
                            theme = theme,
                            isLoading = isLoading,
                            onImportStart = { 
                                importMessage = ""
                            },
                            onImportComplete = { success, message ->
                                importMessage = message
                            },
                            importManager = importManager
                        )
                    }
                }
            }
            
            // Show import progress
            if (importProgress != null && importProgress!!.displayTotal > 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = theme.surfaceColor)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    progress = { importProgress!!.percentage / 100f },
                                    modifier = Modifier.size(24.dp),
                                    color = theme.accentColor,
                                    strokeWidth = 3.dp,
                                    trackColor = ProgressIndicatorDefaults.circularIndeterminateTrackColor,
                                    strokeCap = ProgressIndicatorDefaults.CircularDeterminateStrokeCap,
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Importing... ${importProgress!!.percentage}%",
                                        color = theme.primaryTextColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = if (importProgress!!.displayTotal > 0) "${importProgress!!.displayCurrent} of ${importProgress!!.displayTotal} files" else "Preparing...",
                                        color = theme.secondaryTextColor,
                                        fontSize = 12.sp
                                    )
                                }
                                IconButton(
                                    onClick = { 
                                        libraryManager?.cancelCurrentImport()
                                        importMessage = "Import cancelled"
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Cancel Import",
                                        tint = theme.secondaryTextColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            if (importProgress!!.currentFile.isNotEmpty()) {
                                Text(
                                    text = importProgress!!.currentFile,
                                    color = theme.secondaryTextColor,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
            
            // Show import results if available, otherwise show import message
            if (lastImportResult != null && importProgress == null) {
                item {
                    ImportResultCard(
                        importResult = lastImportResult!!,
                        theme = theme,
                        onDismiss = { libraryManager?.clearLastImportResult() }
                    )
                }
            } else if (importMessage.isNotEmpty() && importProgress == null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (importMessage.contains("successfully") || 
                                               importMessage.contains("Found") || 
                                               importMessage.contains("complete") || 
                                               importMessage.contains("added to watch list")) 
                                theme.accentColor.copy(alpha = 0.1f) 
                            else 
                                Color.Red.copy(alpha = 0.1f)
                        )
                    ) {
                        Text(
                            text = importMessage,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            color = if (importMessage.contains("successfully") || 
                                       importMessage.contains("Found") || 
                                       importMessage.contains("complete") || 
                                       importMessage.contains("added to watch list")) 
                                theme.accentColor 
                            else 
                                Color.Red,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            
            // Watched Directories Section
            if (libraryManager != null) {
                item {
                    SettingsSection(title = "Watched Directories", theme = theme) {
                        WatchedDirectoriesView(
                            libraryManager = libraryManager,
                            theme = theme,
                            importMessage = importMessage,
                            onMessageChange = { importMessage = it }
                        )
                    }
                }
            }
            
            // Library Summary Section  
            if (libraryManager != null) {
                item {
                    SettingsSection(title = "Library Summary", theme = theme) {
                        LibrarySummaryView(
                            libraryManager = libraryManager,
                            theme = theme
                        )
                    }
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
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Theme",
            color = theme.primaryTextColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Unified theme selection with Light, Dark, Sepia, and System
        val availableThemes = listOf(
            ThemeMode.LIGHT,
            ThemeMode.DARK,
            ThemeMode.SEPIA,
            ThemeMode.SYSTEM
        )
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            items(availableThemes) { themeMode ->
                ThemeCard(
                    themeMode = themeMode,
                    isSelected = currentTheme == themeMode,
                    onClick = { onThemeChange(themeMode) },
                    theme = theme
                )
            }
        }
    }
}

@Composable
private fun ThemeCard(
    themeMode: ThemeMode,
    isSelected: Boolean,
    onClick: () -> Unit,
    theme: AppTheme
) {
    val themeColors = getThemePreviewColors(themeMode)
    
    Card(
        modifier = Modifier
            .width(100.dp)
            .height(80.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) theme.accentColor.copy(alpha = 0.2f) else theme.surfaceColor
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, theme.accentColor)
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Theme preview
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(themeColors.first)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(themeColors.second)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Theme name
            Text(
                text = themeMode.displayName,
                color = theme.primaryTextColor,
                fontSize = 12.sp,
                maxLines = 2,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun getThemePreviewColors(themeMode: ThemeMode): Pair<Color, Color> {
    return when (themeMode) {
        ThemeMode.LIGHT -> Color(0xFFEFEBE3) to Color(0xFF3C3836)
        ThemeMode.DARK -> Color(0xFF1D2021) to Color(0xFFF9F5D7)
        ThemeMode.SYSTEM -> Color(0xFFE0E0E0) to Color(0xFF424242)
        ThemeMode.DYNAMIC -> Color(0xFF6B4423) to Color(0xFFF2D2A7)
        ThemeMode.SEPIA -> Color(0xFFF7F3E9) to Color(0xFF5D4E37)
        ThemeMode.HIGH_CONTRAST -> Color(0xFFFFFFFF) to Color(0xFF000000)
        ThemeMode.NIGHT_MODE -> Color(0xFF000000) to Color(0xFFE0E0E0)
        ThemeMode.WARM -> Color(0xFFFFF8E1) to Color(0xFF4E342E)
        ThemeMode.COOL -> Color(0xFFE8F4FD) to Color(0xFF0D47A1)
    }
}

@Composable
fun WatchedDirectoriesView(
    libraryManager: LibraryManager,
    theme: AppTheme,
    importMessage: String,
    onMessageChange: (String) -> Unit
) {
    val watchedDirectories = libraryManager.watchedDirectories.value
    val isLoading = libraryManager.isLoading.value
    val importProgress = libraryManager.importProgress.value
    
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Manual rescan button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${watchedDirectories.size} directories",
                fontSize = 14.sp,
                color = theme.primaryTextColor
            )
            
            TextButton(
                onClick = { 
                    onMessageChange("") // Clear any existing messages
                    libraryManager.rescanWatchedDirectoriesAsync { success, message, count ->
                        onMessageChange(if (success) {
                            if (count > 0) "Found $count new books!" else "Rescan complete - no new books found"
                        } else {
                            "Rescan failed: $message"
                        })
                    }
                }
            ) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = theme.accentColor
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Rescan All",
                    color = theme.accentColor,
                    fontSize = 12.sp
                )
            }
        }
        
        if (watchedDirectories.isNotEmpty()) {
            watchedDirectories.forEach { dir ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = theme.backgroundColor.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = dir.path,
                                fontSize = 12.sp,
                                color = theme.primaryTextColor,
                                maxLines = 1,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${if (dir.isUri) "URI Directory" else "File Directory"} • ${if (dir.recursive) "Recursive" else "Single level"}",
                                fontSize = 10.sp,
                                color = theme.secondaryTextColor
                            )
                            Text(
                                text = when {
                                    isLoading && importProgress != null && dir.requiresRescan -> {
                                        "Scanning... ${importProgress?.currentFile ?: "files"}"
                                    }
                                    dir.requiresRescan -> {
                                        "Requires rescan"
                                    }
                                    else -> {
                                        "Last scanned: ${formatTimestamp(dir.lastScanned)}"
                                    }
                                },
                                fontSize = 10.sp,
                                color = when {
                                    isLoading && importProgress != null && dir.requiresRescan -> theme.accentColor
                                    dir.requiresRescan -> theme.accentColor
                                    else -> theme.secondaryTextColor
                                },
                                fontWeight = when {
                                    isLoading && importProgress != null && dir.requiresRescan -> FontWeight.Medium
                                    dir.requiresRescan -> FontWeight.Medium
                                    else -> FontWeight.Normal
                                }
                            )
                        }
                        IconButton(
                            onClick = { libraryManager.removeWatchedDirectory(dir.path) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Remove Directory",
                                tint = theme.secondaryTextColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        } else {
            Text(
                text = "No watched directories. Add a directory to watch for EPUB files.",
                fontSize = 12.sp,
                color = theme.secondaryTextColor,
                style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            )
        }
    }
}


@Composable
fun LibrarySummaryView(
    libraryManager: LibraryManager,
    theme: AppTheme
) {
    val books = libraryManager.books.value
    val watchedDirectories = libraryManager.watchedDirectories.value
    val importedFiles = libraryManager.importedFiles.value
    
    val readingBooks = books.filter { it.progress > 0f && it.progress < 1f }
    val completedBooks = books.filter { it.progress >= 1f }
    val planToReadBooks = books.filter { it.progress == 0f }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Overall stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Total Books",
                fontSize = 14.sp,
                color = theme.primaryTextColor
            )
            Text(
                text = "${books.size}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = theme.accentColor
            )
        }
        
        HorizontalDivider(color = theme.secondaryTextColor.copy(alpha = 0.2f))
        
        // Reading status breakdown
        listOf(
            "Reading" to readingBooks.size,
            "Plan to Read" to planToReadBooks.size,
            "Completed" to completedBooks.size
        ).forEach { (status, count) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = status,
                    fontSize = 12.sp,
                    color = theme.secondaryTextColor
                )
                Text(
                    text = "$count",
                    fontSize = 12.sp,
                    color = theme.primaryTextColor
                )
            }
        }
        
        HorizontalDivider(color = theme.secondaryTextColor.copy(alpha = 0.2f))
        
        // Source breakdown
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Watched Directories",
                fontSize = 12.sp,
                color = theme.secondaryTextColor
            )
            Text(
                text = "${watchedDirectories.size}",
                fontSize = 12.sp,
                color = theme.primaryTextColor
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Total Imports",
                fontSize = 12.sp,
                color = theme.secondaryTextColor
            )
            Text(
                text = "${importedFiles.size}",
                fontSize = 12.sp,
                color = theme.primaryTextColor
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val minutes = diff / (1000 * 60)
    val hours = diff / (1000 * 60 * 60)
    val days = diff / (1000 * 60 * 60 * 24)
    
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> "${days / 7}w ago"
    }
}

@Composable
fun LibraryManagementOptions(
    libraryManager: LibraryManager,
    theme: AppTheme,
    isLoading: Boolean,
    onImportStart: () -> Unit,
    onImportComplete: (Boolean, String) -> Unit,
    importManager: ImportManager? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // File picker for EPUB files
    val epubFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { 
            onImportStart()
            scope.launch {
                try {
                    // Proceed with import using LibraryManager (it has its own duplicate checking)
                    val file = File(context.cacheDir, "temp_epub_${System.currentTimeMillis()}.epub")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        file.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    
                    libraryManager.importEpubFileAsync(file) { success, message ->
                        file.delete() // Clean up temp file after async operation completes
                        onImportComplete(success, message)
                    }
                } catch (e: Exception) {
                    onImportComplete(false, "Error importing EPUB: ${e.message}")
                }
            }
        }
    }
    
    // Directory picker for adding to watch list
    val directoryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            // Take persistent permission to access this directory
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Permission might already be taken or not available
            }
            
            libraryManager.addDirectoryToWatchListAsync(uri.toString(), context) { success, message ->
                onImportComplete(success, message)
            }
        }
    }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Import single EPUB
        SettingsActionItem(
            title = "Import EPUB File",
            subtitle = "Import a single EPUB book",
            icon = Icons.Filled.FileUpload,
            theme = theme,
            enabled = !isLoading,
            onClick = { 
                if (!isLoading) {
                    epubFilePicker.launch("application/epub+zip")
                }
            }
        )
        
        // Add directory to watch list
        SettingsActionItem(
            title = "Add Directory to Watch",
            subtitle = "Add a folder to scan for EPUB files (requires rescan)",
            icon = Icons.Filled.FolderOpen,
            theme = theme,
            enabled = !isLoading,
            onClick = { 
                if (!isLoading) {
                    directoryPicker.launch(null)
                }
            }
        )
        
        // Clear library
        SettingsActionItem(
            title = "Clear Library",
            subtitle = "Remove all books from library",
            icon = Icons.Filled.DeleteSweep,
            theme = theme,
            enabled = !isLoading,
            onClick = {
                if (!isLoading) {
                    onImportStart()
                    libraryManager.clearLibraryAsync { success, message ->
                        onImportComplete(success, message)
                    }
                }
            }
        )
    }
}

@Composable
fun SettingsActionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    theme: AppTheme,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) theme.accentColor else theme.secondaryTextColor,
            modifier = Modifier.size(24.dp)
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (enabled) theme.primaryTextColor else theme.secondaryTextColor
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = theme.secondaryTextColor
            )
        }
        
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = "Action",
            tint = if (enabled) theme.secondaryTextColor else theme.secondaryTextColor.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
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

@Composable
fun ImportResultCard(
    importResult: ImportResult,
    theme: AppTheme,
    onDismiss: () -> Unit = {}
) {
    var showFailureDetails by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (importResult.failedCount > 0) 
                Color(0xFFFF9800).copy(alpha = 0.1f) 
            else 
                theme.accentColor.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = importResult.getDisplayMessage(),
                    color = if (importResult.failedCount > 0) Color(0xFFFF9800) else theme.accentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(16.dp),
                        tint = theme.secondaryTextColor
                    )
                }
            }
            
            if (importResult.failedCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier
                        .clickable { showFailureDetails = !showFailureDetails }
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (showFailureDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (showFailureDetails) "Hide details" else "Show details",
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "View failed imports",
                        color = Color(0xFFFF9800),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                if (showFailureDetails) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    importResult.failures.forEach { failure ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Red.copy(alpha = 0.05f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = failure.fileName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = theme.primaryTextColor
                                )
                                Text(
                                    text = "Path: ${failure.filePath}",
                                    fontSize = 11.sp,
                                    color = theme.secondaryTextColor
                                )
                                Text(
                                    text = "Error: ${failure.errorMessage}",
                                    fontSize = 11.sp,
                                    color = Color.Red
                                )
                            }
                        }
                    }
                }
            }
        }
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