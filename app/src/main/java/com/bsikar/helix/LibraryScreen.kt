@file:Suppress("LongMethod", "TooManyFunctions", "MatchingDeclarationName")
package com.bsikar.helix

import android.net.Uri
import android.provider.DocumentsContract
import android.provider.Settings
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
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
import com.bsikar.helix.data.LibraryRepository
import java.io.File
import java.net.URLEncoder

data class EpubItem(
    val uri: Uri,
    val displayName: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("FunctionNaming", "LongMethod")
fun LibraryScreen(navController: NavController) {
    val context = LocalContext.current
    val progressRepository = remember { ReadingProgressRepository.getInstance(context) }
    val libraryRepository = remember { LibraryRepository.getInstance(context) }
    val userPreferences = remember { com.bsikar.helix.data.UserPreferences.getInstance(context) }
    val allRecentBooks by progressRepository.recentBooks.collectAsState(initial = emptyList())
    val librarySources by libraryRepository.librarySources.collectAsState()
    val excludedBooks by libraryRepository.excludedBooks.collectAsState()
    val recentBooksCount by userPreferences.recentBooksCount.collectAsState()

    var epubItems by remember { mutableStateOf<List<EpubItem>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val updateFiles = {
        scope.launch {
            isLoading = true
            try {
                val newItems = withContext(Dispatchers.IO) {
                    scanLibrarySourcesWithMetadata(context, libraryRepository)
                }
                epubItems = newItems
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(librarySources) {
        updateFiles()
    }

    val filteredItems = epubItems.filter { item ->
        item.displayName.lowercase().contains(searchQuery.lowercase()) &&
        !libraryRepository.isBookExcluded(item.uri)
    }

    // Filter recent books to only show those that exist in current library sources
    val filteredRecentBooks = allRecentBooks.filter { progress ->
        isBookInLibraryItems(progress.epubPath, epubItems)
    }.take(recentBooksCount)

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

        if (isLoading) {
            LoadingStateView()
        } else if (filteredItems.isEmpty() && epubItems.isNotEmpty()) {
            EmptySearchStateView()
        } else if (epubItems.isEmpty()) {
            EmptyStateView(
                onAddBooks = { navController.navigate("settings") }
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
                            recentBooks = filteredRecentBooks,
                            onBookClick = { bookPath ->
                                val encodedPath = URLEncoder.encode(bookPath, "UTF-8")
                                navController.navigate("reader/$encodedPath")
                            }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }

                // Show all available books
                items(filteredItems) { item ->
                    BookCard(
                        uri = item.uri,
                        displayName = item.displayName,
                        onBookClick = {
                            val file = UriFileResolver.resolveUriToFile(context, item.uri)
                            file?.let {
                                val encodedPath = URLEncoder.encode(it.absolutePath, "UTF-8")
                                navController.navigate("reader/$encodedPath")
                            }
                        },
                        onBookHide = {
                            libraryRepository.addExcludedBook(item.uri)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
@Suppress("FunctionNaming", "LongMethod")
private fun BookCard(
    uri: Uri,
    displayName: String,
    onBookClick: () -> Unit,
    onBookHide: () -> Unit
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onBookClick,
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
            val file = UriFileResolver.resolveUriToFile(context, uri)
            if (file != null) {
                BookCoverImage(
                    epubFile = file,
                    size = 48.dp,
                    cornerRadius = 8.dp
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📚",
                        fontSize = 24.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Text(
                    text = displayName.removeSuffix(".epub"),
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
        val file = UriFileResolver.resolveUriToFile(context, uri)
        if (file != null) {
            BookInfoDialog(
                epubFile = file,
                onDismiss = { showDialog = false },
                onHide = {
                    onBookHide()
                    showDialog = false
                }
            )
        }
    }
}

@Composable
@Suppress("FunctionNaming")
private fun LoadingStateView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Scanning library sources...",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "This may take a moment for large folders",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
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
    onAddBooks: () -> Unit
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
            text = "Add folders or EPUB files to your library to get started",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onAddBooks,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Add Books to Library")
        }
    }
}

@Suppress("NestedBlockDepth")
private suspend fun scanLibrarySourcesWithMetadata(
    context: android.content.Context,
    libraryRepository: LibraryRepository
): List<EpubItem> {
    val epubItems = mutableListOf<EpubItem>()
    val sourceUris = libraryRepository.getLibrarySourceUris()

    for (sourceUri in sourceUris) {
        kotlinx.coroutines.yield() // Allow other coroutines to run
        try {
            if (isDirectoryUri(sourceUri)) {
                epubItems.addAll(scanDirectoryForEpubsWithMetadata(context, sourceUri))
            } else {
                val fileName = getDisplayNameFromUri(context, sourceUri) ?: ""
                if (fileName.endsWith(".epub", ignoreCase = true)) {
                    epubItems.add(EpubItem(sourceUri, fileName))
                }
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            e.printStackTrace()
        }
    }

    return epubItems.distinctBy { it.uri }
}

@Suppress("NestedBlockDepth")
private suspend fun scanLibrarySources(
    context: android.content.Context,
    libraryRepository: LibraryRepository
): List<Uri> {
    val epubUris = mutableListOf<Uri>()
    val sourceUris = libraryRepository.getLibrarySourceUris()

    for (sourceUri in sourceUris) {
        kotlinx.coroutines.yield() // Allow other coroutines to run
        try {
            if (isDirectoryUri(sourceUri)) {
                epubUris.addAll(scanDirectoryForEpubs(context, sourceUri))
            } else {
                val fileName = getDisplayNameFromUri(context, sourceUri) ?: ""
                if (fileName.endsWith(".epub", ignoreCase = true)) {
                    epubUris.add(sourceUri)
                }
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            e.printStackTrace()
        }
    }

    return epubUris.distinct()
}

private fun isDirectoryUri(uri: Uri): Boolean {
    return uri.toString().contains("/tree/")
}

@Suppress("NestedBlockDepth")
private suspend fun scanDirectoryForEpubsWithMetadata(
    context: android.content.Context,
    directoryUri: Uri
): List<EpubItem> {
    val epubItems = mutableListOf<EpubItem>()

    try {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            directoryUri, DocumentsContract.getTreeDocumentId(directoryUri)
        )

        context.contentResolver.query(childrenUri, null, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                kotlinx.coroutines.yield() // Allow other coroutines to run between each file

                val documentId = cursor.getString(
                    cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                )
                val displayName = cursor.getString(
                    cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                )
                val mimeType = cursor.getString(
                    cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                )

                val documentUri = DocumentsContract.buildDocumentUriUsingTree(directoryUri, documentId)

                if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    epubItems.addAll(scanDirectoryForEpubsWithMetadata(context, documentUri))
                } else if (displayName?.endsWith(".epub", ignoreCase = true) == true) {
                    epubItems.add(EpubItem(documentUri, displayName))
                }
            }
        }
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        e.printStackTrace()
    }

    return epubItems
}

@Suppress("NestedBlockDepth")
private suspend fun scanDirectoryForEpubs(context: android.content.Context, directoryUri: Uri): List<Uri> {
    val epubUris = mutableListOf<Uri>()

    try {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            directoryUri, DocumentsContract.getTreeDocumentId(directoryUri)
        )

        context.contentResolver.query(childrenUri, null, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                kotlinx.coroutines.yield() // Allow other coroutines to run between each file

                val documentId = cursor.getString(
                    cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                )
                val displayName = cursor.getString(
                    cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                )
                val mimeType = cursor.getString(
                    cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                )

                val documentUri = DocumentsContract.buildDocumentUriUsingTree(directoryUri, documentId)

                if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    epubUris.addAll(scanDirectoryForEpubs(context, documentUri))
                } else if (displayName?.endsWith(".epub", ignoreCase = true) == true) {
                    epubUris.add(documentUri)
                }
            }
        }
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        e.printStackTrace()
    }

    return epubUris
}

@Suppress("NestedBlockDepth")
private fun getDisplayNameFromUri(context: android.content.Context, uri: Uri): String? {
    return try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    cursor.getString(nameIndex)
                } else {
                    null
                }
            } else {
                null
            }
        }
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        null
    }
}

private fun isBookInLibraryItems(
    bookPath: String,
    availableItems: List<EpubItem>
): Boolean {
    val bookFile = File(bookPath)

    // Check if the book file exists
    if (!bookFile.exists()) {
        return false
    }

    // Check if any of the available items point to this book by comparing book names
    val bookFileName = bookFile.name
    return availableItems.any { item ->
        item.displayName == bookFileName
    }
}

private fun isBookInLibrarySources(
    bookPath: String,
    availableUris: List<Uri>,
    context: android.content.Context
): Boolean {
    val bookFile = File(bookPath)

    // Check if the book file exists
    if (!bookFile.exists()) {
        return false
    }

    // Check if any of the available URIs point to this book
    return availableUris.any { uri ->
        try {
            val resolvedFile = UriFileResolver.resolveUriToFile(context, uri)
            resolvedFile?.absolutePath == bookFile.absolutePath
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            false
        }
    }
}
