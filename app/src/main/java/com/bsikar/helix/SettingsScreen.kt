package com.bsikar.helix

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bsikar.helix.data.LibraryRepository
import com.bsikar.helix.data.ReadingProgressRepository
import com.bsikar.helix.data.ThemeMode
import com.bsikar.helix.data.rememberUserPreferences
import java.io.File
import java.util.Locale

private const val PREVIEW_BOOKS_COUNT = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("FunctionNaming", "LongMethod")
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val userPreferences = rememberUserPreferences()
    val libraryRepository = remember { LibraryRepository.getInstance(context) }
    val progressRepository = remember { ReadingProgressRepository.getInstance(context) }

    val fontSize by userPreferences.fontSize.collectAsState()
    val lineHeight by userPreferences.lineHeight.collectAsState()
    val onlyShowImages by userPreferences.onlyShowImages.collectAsState()
    val themeMode by userPreferences.themeMode.collectAsState()
    val accentColor by userPreferences.accentColor.collectAsState()
    val recentBooksCount by userPreferences.recentBooksCount.collectAsState()
    val librarySources by libraryRepository.librarySources.collectAsState()

    var showThemeDropdown by remember { mutableStateOf(false) }
    var sourceToRemove by remember { mutableStateOf<String?>(null) }
    var resetProgress by remember { mutableStateOf(false) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            libraryRepository.addLibrarySource(it)
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { uri ->
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            libraryRepository.addLibrarySource(uri)
        }
    }

    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars),
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Library Sources Section
            Text(
                text = "Library Sources",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Manage your book library sources",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { folderPickerLauncher.launch(null) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Folder")
                        }

                        Button(
                            onClick = { filePickerLauncher.launch(arrayOf("application/epub+zip")) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Files")
                        }
                    }

                    if (librarySources.isNotEmpty()) {
                        Text(
                            text = "Current Sources:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        LazyColumn(
                            modifier = Modifier.height(200.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(librarySources.toList()) { sourceUri ->
                                LibrarySourceItem(
                                    sourceUri = sourceUri,
                                    onRemove = {
                                        sourceToRemove = sourceUri
                                    }
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "No library sources added yet",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            // Appearance Section
            Text(
                text = "Appearance",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Theme Selection
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Theme",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Box {
                                OutlinedButton(
                                    onClick = { showThemeDropdown = true }
                                ) {
                                    Text(
                                        text = when (themeMode) {
                                            ThemeMode.LIGHT -> "Light"
                                            ThemeMode.DARK -> "Dark"
                                            ThemeMode.SYSTEM -> "System"
                                        }
                                    )
                                }
                                DropdownMenu(
                                    expanded = showThemeDropdown,
                                    onDismissRequest = { showThemeDropdown = false }
                                ) {
                                    ThemeMode.entries.forEach { mode ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = when (mode) {
                                                        ThemeMode.LIGHT -> "Light"
                                                        ThemeMode.DARK -> "Dark"
                                                        ThemeMode.SYSTEM -> "System"
                                                    }
                                                )
                                            },
                                            onClick = {
                                                userPreferences.setThemeMode(mode)
                                                showThemeDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Accent Color Selection
                    Column {
                        Text(
                            text = "Accent Color",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(predefinedColors) { color ->
                                ColorCircle(
                                    color = color,
                                    isSelected = color == accentColor,
                                    onClick = { userPreferences.setAccentColor(color) }
                                )
                            }
                        }
                    }
                }
            }

            // Reading Section
            Text(
                text = "Reading",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Font Size",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${fontSize.toInt()}sp",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = fontSize,
                            onValueChange = { userPreferences.setFontSize(it) },
                            valueRange = 12f..24f,
                            steps = 11
                        )
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Line Height",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", lineHeight)}x",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = lineHeight,
                            onValueChange = { userPreferences.setLineHeight(it) },
                            valueRange = 1.0f..2.0f,
                            steps = 9
                        )
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent Books Count",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "$recentBooksCount",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = recentBooksCount.toFloat(),
                            onValueChange = { userPreferences.setRecentBooksCount(it.toInt()) },
                            valueRange = 1f..5f,
                            steps = 3
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Only Show Images",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Hide all text content, show only images",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = onlyShowImages,
                            onCheckedChange = { userPreferences.setOnlyShowImages(it) }
                        )
                    }
                }
            }

            // Preview Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Preview",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "This is how your text will appear in the reader. " +
                                "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                                "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * lineHeight).sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Show confirmation dialog when removing a source
    sourceToRemove?.let { sourceUri ->
        val currentRecentBooks by progressRepository.recentBooks.collectAsState(initial = emptyList())
        RemoveSourceDialog(
            sourceUri = sourceUri,
            recentBooks = currentRecentBooks,
            onDismiss = {
                sourceToRemove = null
                resetProgress = false
            },
            onConfirm = { resetProgressFlag ->
                val uri = Uri.parse(sourceUri)
                libraryRepository.removeLibrarySource(uri)

                if (resetProgressFlag) {
                    // Find books that would be affected and reset their progress
                    val affectedBooks = findAffectedBooks(sourceUri, currentRecentBooks)
                    affectedBooks.forEach { bookPath ->
                        progressRepository.removeRecentBookWithOptions(bookPath, true)
                    }
                }

                sourceToRemove = null
                resetProgress = false
            }
        )
    }
}

@Composable
@Suppress("FunctionNaming")
private fun ColorCircle(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .background(
                color = color,
                shape = CircleShape
            )
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .clickable { onClick() }
    )
}

@Suppress("MagicNumber")
private val predefinedColors = listOf(
    Color(0xFF6200EE), // Purple (Material Purple 500)
    Color(0xFF2196F3), // Blue (Material Blue 500)
    Color(0xFF4CAF50), // Green (Material Green 500)
    Color(0xFFFF9800), // Orange (Material Orange 500)
    Color(0xFFE91E63), // Pink (Material Pink 500)
    Color(0xFF9C27B0), // Purple Variant (Material Purple 500)
    Color(0xFF00BCD4), // Cyan (Material Cyan 500)
    Color(0xFF8BC34A), // Light Green (Material Light Green 500)
    Color(0xFFFF5722), // Deep Orange (Material Deep Orange 500)
    Color(0xFF607D8B), // Blue Grey (Material Blue Grey 500)
    Color(0xFFCDDC39), // Lime (Material Lime 500)
    Color(0xFF795548) // Brown (Material Brown 500)
)

@Composable
@Suppress("FunctionNaming")
private fun LibrarySourceItem(
    sourceUri: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                val displayPath = try {
                    val uri = Uri.parse(sourceUri)
                    when {
                        uri.path?.contains("/document/primary:") == true -> {
                            "Internal Storage/${uri.path?.substringAfter("/document/primary:")}"
                        }
                        uri.lastPathSegment != null -> uri.lastPathSegment
                        else -> "Unknown source"
                    }
                } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                    "Invalid source"
                }

                Text(
                    text = displayPath ?: "Unknown source",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = if (sourceUri.contains("/tree/")) "Folder" else "File",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onRemove
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove source",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
@Suppress("FunctionNaming", "LongMethod")
private fun RemoveSourceDialog(
    sourceUri: String,
    recentBooks: List<com.bsikar.helix.data.ReadingProgress>,
    onDismiss: () -> Unit,
    onConfirm: (resetProgress: Boolean) -> Unit
) {
    var resetProgress by remember { mutableStateOf(false) }
    val affectedBooks = remember(sourceUri, recentBooks) { findAffectedBooks(sourceUri, recentBooks) }

    val displayPath = try {
        val uri = Uri.parse(sourceUri)
        when {
            uri.path?.contains("/document/primary:") == true -> {
                "Internal Storage/${uri.path?.substringAfter("/document/primary:")}"
            }
            uri.lastPathSegment != null -> uri.lastPathSegment
            else -> "Unknown source"
        }
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        "Invalid source"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Remove Library Source",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Are you sure you want to remove this library source?",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = displayPath ?: "Unknown source",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )

                if (affectedBooks.isNotEmpty()) {
                    HorizontalDivider()

                    Text(
                        text = "This will affect ${affectedBooks.size} book(s) in your recent reading list:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        affectedBooks.take(PREVIEW_BOOKS_COUNT).forEach { bookPath ->
                            Text(
                                text = "• ${File(bookPath).nameWithoutExtension}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (affectedBooks.size > PREVIEW_BOOKS_COUNT) {
                            Text(
                                text = "• ... and ${affectedBooks.size - PREVIEW_BOOKS_COUNT} more",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = resetProgress,
                            onCheckedChange = { resetProgress = it }
                        )
                        Text(
                            text = "Also reset reading progress for these books",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    if (resetProgress) {
                        Text(
                            text = "⚠️ This will permanently delete bookmarks and reading positions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        onConfirm(resetProgress)
                    }
                ) {
                    Text("Remove")
                }
            }
        },
        dismissButton = null
    )
}

private fun findAffectedBooks(
    @Suppress("UnusedParameter") sourceUri: String,
    recentBooks: List<com.bsikar.helix.data.ReadingProgress>
): List<String> {
    return try {
        // For now, return all recent books as potentially affected
        // This ensures the user is always prompted when removing any library source
        // A more sophisticated approach would check actual path relationships,
        // but SAF URIs don't map directly to file system paths in a simple way
        if (recentBooks.isNotEmpty()) {
            recentBooks.map { it.epubPath }
        } else {
            emptyList()
        }
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        emptyList()
    }
}
