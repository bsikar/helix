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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import com.bsikar.helix.data.LibraryManager
import com.bsikar.helix.theme.AppTheme
import com.bsikar.helix.theme.ThemeManager
import com.bsikar.helix.theme.ThemeMode
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    theme: AppTheme,
    onBackClick: () -> Unit,
    libraryManager: LibraryManager? = null,
    scrollToSection: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var importMessage by remember { mutableStateOf("") }
    
    // Get import progress from LibraryManager
    val importProgress by libraryManager?.importProgress ?: remember { mutableStateOf(null) }
    val isLoading by libraryManager?.isLoading ?: remember { mutableStateOf(false) }
    
    // Scroll state for auto-scrolling to sections
    val listState = rememberLazyListState()
    
    // Auto-scroll to specific section when requested
    LaunchedEffect(scrollToSection) {
        if (scrollToSection == "progress" && (importProgress != null || importMessage.isNotEmpty())) {
            // Scroll to the progress section (item index 2: Library Management section)
            listState.animateScrollToItem(2)
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
                            }
                        )
                    }
                }
            }
            
            // Show import progress
            importProgress?.let { progress ->
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
                                    progress = progress.percentage / 100f,
                                    modifier = Modifier.size(24.dp),
                                    color = theme.accentColor,
                                    strokeWidth = 3.dp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Importing... ${progress.percentage}%",
                                        color = theme.primaryTextColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${progress.current} of ${progress.total} files",
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
                            if (progress.currentFile.isNotEmpty()) {
                                Text(
                                    text = progress.currentFile,
                                    color = theme.secondaryTextColor,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
            
            // Show result message
            if (importMessage.isNotEmpty() && importProgress == null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (importMessage.contains("successfully")) 
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
                            color = if (importMessage.contains("successfully")) 
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
                            theme = theme
                        )
                    }
                }
            }
            
            // Individual Import Files Section
            if (libraryManager != null) {
                item {
                    SettingsSection(title = "Individual Imports", theme = theme) {
                        IndividualImportsView(
                            libraryManager = libraryManager,
                            theme = theme
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
fun WatchedDirectoriesView(
    libraryManager: LibraryManager,
    theme: AppTheme
) {
    val watchedDirectories by libraryManager.watchedDirectories
    
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
                    libraryManager.rescanWatchedDirectoriesAsync { success, message, count ->
                        // Result will be shown in scan status
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
                                text = "Last scanned: ${formatTimestamp(dir.lastScanned)}",
                                fontSize = 10.sp,
                                color = theme.secondaryTextColor
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
                text = "No watched directories. Import from a directory to add one.",
                fontSize = 12.sp,
                color = theme.secondaryTextColor,
                style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            )
        }
    }
}

@Composable
fun IndividualImportsView(
    libraryManager: LibraryManager,
    theme: AppTheme
) {
    val importedFiles by libraryManager.importedFiles
    val books by libraryManager.books
    
    // Filter to show only individual file imports (not from directories)
    val individualFiles = importedFiles.filter { file ->
        // A file is considered individual if it's not part of a watched directory
        val watchedDirs = libraryManager.watchedDirectories.value
        watchedDirs.none { dir -> file.path.contains(dir.path, ignoreCase = true) }
    }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "${individualFiles.size} individual files imported",
            fontSize = 14.sp,
            color = theme.primaryTextColor
        )
        
        if (individualFiles.isNotEmpty()) {
            individualFiles.takeLast(5).forEach { file ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = theme.backgroundColor.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = File(file.path).name,
                            fontSize = 12.sp,
                            color = theme.primaryTextColor,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Imported: ${formatTimestamp(file.importedAt)}",
                            fontSize = 10.sp,
                            color = theme.secondaryTextColor
                        )
                    }
                }
            }
            
            if (individualFiles.size > 5) {
                Text(
                    text = "... and ${individualFiles.size - 5} more files",
                    fontSize = 10.sp,
                    color = theme.secondaryTextColor,
                    style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                )
            }
        } else {
            Text(
                text = "No individual files imported. Use 'Import EPUB File' to add individual files.",
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
    val books by libraryManager.books
    val watchedDirectories by libraryManager.watchedDirectories
    val importedFiles by libraryManager.importedFiles
    
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
        
        Divider(color = theme.secondaryTextColor.copy(alpha = 0.2f))
        
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
        
        Divider(color = theme.secondaryTextColor.copy(alpha = 0.2f))
        
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
    onImportComplete: (Boolean, String) -> Unit
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
                    val file = File(context.cacheDir, "temp_epub_${System.currentTimeMillis()}.epub")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        file.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    
                    libraryManager.importEpubFileAsync(file) { success, message ->
                        onImportComplete(success, message)
                    }
                    
                    file.delete() // Clean up temp file
                } catch (e: Exception) {
                    onImportComplete(false, "Error importing EPUB: ${e.message}")
                }
            }
        }
    }
    
    // Directory picker for bulk import
    val directoryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            onImportStart()
            libraryManager.importEpubsFromDirectoryAsync(uri.toString(), context) { success, message ->
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
        
        // Import from directory
        SettingsActionItem(
            title = "Import from Directory",
            subtitle = "Import multiple EPUB files from a folder",
            icon = Icons.Filled.FolderOpen,
            theme = theme,
            enabled = !isLoading,
            onClick = { 
                if (!isLoading) {
                    directoryPicker.launch(null)
                }
            }
        )
        
        // Add fake data for testing
        SettingsActionItem(
            title = "Import Fake Data",
            subtitle = "Add sample books for testing",
            icon = Icons.AutoMirrored.Filled.LibraryBooks,
            theme = theme,
            enabled = !isLoading,
            onClick = {
                if (!isLoading) {
                    onImportStart()
                    libraryManager.addFakeDataAsync { success, message ->
                        onImportComplete(success, message)
                    }
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