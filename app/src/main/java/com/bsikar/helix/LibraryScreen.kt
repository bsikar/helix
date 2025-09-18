package com.bsikar.helix

import android.content.Intent
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.bsikar.helix.data.ReadingProgressRepository
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import java.io.File
import java.net.URLEncoder
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("FunctionNaming", "LongMethod")
fun LibraryScreen(navController: NavController) {
    val context = LocalContext.current
    val progressRepository = remember { ReadingProgressRepository.getInstance(context) }
    val recentBooks by progressRepository.recentBooks.collectAsState(initial = emptyList())

    var epubFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var permissionStatus by remember { mutableStateOf("Checking permissions...") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }

    val updateFiles = { epubFiles = findEpubFiles() }
    val updatePermissionStatus = { status: String -> permissionStatus = status }

    createPermissionLauncher(updatePermissionStatus, updateFiles)
    val settingsLauncher = createSettingsLauncher(updatePermissionStatus, updateFiles)

    HandlePermissions(updatePermissionStatus, updateFiles)

    val filteredFiles = epubFiles.filter {
        it.name.lowercase().contains(searchQuery.lowercase())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Header with Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Helix EPUB Reader",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row {
                IconButton(
                    onClick = { updateFiles() }
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh books",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = { isSearchVisible = !isSearchVisible }
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search books",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = { navController.navigate("settings") }
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Search Bar
        if (isSearchVisible) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search books...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                }
            )
        }

        if (filteredFiles.isEmpty() && epubFiles.isNotEmpty()) {
            EmptySearchStateView()
        } else if (epubFiles.isEmpty()) {
            EmptyStateView(
                onGrantPermission = {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = "package:${context.packageName}".toUri()
                    settingsLauncher.launch(intent)
                }
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Always show recent books section (handles empty state internally)
                if (searchQuery.isEmpty()) {
                    item {
                        RecentBooksSection(
                            recentBooks = recentBooks,
                            onBookClick = { bookPath ->
                                val encodedPath = URLEncoder.encode(bookPath, "UTF-8")
                                navController.navigate("reader/$encodedPath")
                            }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }

                // Show all available books
                items(filteredFiles) { file ->
                    BookCard(
                        bookName = file.name,
                        filePath = file.absolutePath,
                        onClick = {
                            val encodedPath = URLEncoder.encode(file.absolutePath, "UTF-8")
                            navController.navigate("reader/$encodedPath")
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
@Suppress("FunctionNaming")
private fun BookCard(
    bookName: String,
    filePath: String,
    onClick: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showDialog = true }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BookCoverImage(
                epubFile = File(filePath),
                size = 48.dp,
                cornerRadius = 8.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Text(
                    text = bookName.removeSuffix(".epub"),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "EPUB Book",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Show book info dialog on long press
    if (showDialog) {
        BookInfoDialog(
            epubFile = File(filePath),
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
@Suppress("FunctionNaming")
private fun EmptySearchStateView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.size(80.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🔍",
                    fontSize = 32.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No matching books",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Try adjusting your search terms",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
@Suppress("FunctionNaming")
private fun EmptyStateView(
    onGrantPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.size(80.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📚",
                    fontSize = 32.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No books found",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Add EPUB files to your Downloads folder to get started",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (!Environment.isExternalStorageManager()) {
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onGrantPermission,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Grant Storage Permission")
            }
        }
    }
}

@Composable
@Suppress("FunctionNaming")
private fun createPermissionLauncher(
    updatePermissionStatus: (String) -> Unit,
    updateFiles: () -> Unit
) = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
    onResult = { isGranted: Boolean ->
        if (isGranted) {
            updatePermissionStatus("Permission granted")
            updateFiles()
        } else {
            updatePermissionStatus("Permission denied")
        }
    }
)

@Composable
@Suppress("FunctionNaming")
private fun createSettingsLauncher(
    updatePermissionStatus: (String) -> Unit,
    updateFiles: () -> Unit
) = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
) {
    if (Environment.isExternalStorageManager()) {
        updatePermissionStatus("External storage manager permission granted")
        updateFiles()
    } else {
        updatePermissionStatus("Need external storage manager permission (Android 11+)")
    }
}

@Composable
@Suppress("FunctionNaming")
private fun HandlePermissions(
    updatePermissionStatus: (String) -> Unit,
    updateFiles: () -> Unit
) {
    LaunchedEffect(Unit) {
        if (Environment.isExternalStorageManager()) {
            updatePermissionStatus("External storage manager permission granted")
            updateFiles()
        } else {
            updatePermissionStatus("Need external storage manager permission (Android 11+)")
        }
    }
}

private fun findEpubFiles(): List<File> {
    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    return downloadsDir.walk().filter { it.isFile && it.extension == "epub" }.toList()
}
