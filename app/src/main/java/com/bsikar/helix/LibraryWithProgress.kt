package com.bsikar.helix

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.bsikar.helix.data.ReadingProgress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Constants for time formatting
private const val ONE_MINUTE_MILLIS = 60_000L
private const val ONE_HOUR_MILLIS = 3600_000L
private const val ONE_DAY_MILLIS = 86400_000L
private const val ONE_WEEK_MILLIS = 7 * ONE_DAY_MILLIS

/**
 * Enhanced library item showing reading progress
 */
@Composable
@Suppress("FunctionNaming", "LongMethod")
fun RecentBookItem(
    progress: ReadingProgress,
    onBookClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { com.bsikar.helix.data.ReadingProgressRepository.getInstance(context) }
    var showDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onBookClick(progress.epubPath) },
                    onLongPress = {
                        showDialog = true
                    }
                )
            }
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover image
            BookCoverImageLarge(
                epubFile = java.io.File(progress.epubPath),
                size = 64.dp,
                cornerRadius = 8.dp
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Book content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Book title and status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = progress.epubFileName.removeSuffix(".epub"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    BookStatusBadge(progress = progress)
                }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress.estimatedProgress },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Progress details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = progress.getProgressDescription(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${progress.getProgressPercentage()}% complete",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Reading stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (progress.bookmarks.isNotEmpty()) {
                    Text(
                        text = "🔖 ${progress.bookmarks.size} bookmarks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = formatLastRead(progress.lastReadTimestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            }
        }
    }

    // Show combined book info and remove dialog on long press
    if (showDialog) {
        RecentBookOptionsDialog(
            progress = progress,
            onDismiss = { showDialog = false },
            onRemove = { resetProgress ->
                repository.removeRecentBookWithOptions(progress.epubPath, resetProgress)
            }
        )
    }
}

@Composable
@Suppress("FunctionNaming")
private fun BookStatusBadge(progress: ReadingProgress) {
    val (text, color) = when {
        progress.isFinished() -> "Finished" to MaterialTheme.colorScheme.tertiary
        progress.isNewBook() -> "New" to MaterialTheme.colorScheme.secondary
        else -> "Reading" to MaterialTheme.colorScheme.primary
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.Medium
    )
}

/**
 * Recent books section for library
 */
@Composable
@Suppress("FunctionNaming")
fun RecentBooksSection(
    recentBooks: List<ReadingProgress>,
    onBookClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📚",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Recent Books",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (recentBooks.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = "No recent books found.\nOpen an EPUB file to start reading!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            // Don't use LazyColumn here - items will be added to parent LazyColumn
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                recentBooks.forEach { progress ->
                    RecentBookItem(
                        progress = progress,
                        onBookClick = onBookClick
                    )
                }
            }
        }
    }
}

/**
 * Combined dialog showing book information with remove option
 */
@Composable
@Suppress("FunctionNaming", "LongMethod")
private fun RecentBookOptionsDialog(
    progress: ReadingProgress,
    onDismiss: () -> Unit,
    onRemove: (resetProgress: Boolean) -> Unit
) {
    val context = LocalContext.current
    val metadataExtractor = remember { DetailedEpubMetadataExtractor(context) }
    var bookInfo by remember { mutableStateOf<BookInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var resetProgress by remember { mutableStateOf(false) }

    LaunchedEffect(progress.epubPath) {
        isLoading = true
        bookInfo = withContext(Dispatchers.IO) {
            metadataExtractor.extractBookInfo(java.io.File(progress.epubPath))
        }
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(16.dp),
        title = {
            Text(
                text = "Book Options",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isLoading) {
                    Text(
                        text = "Loading book information...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    bookInfo?.let { info ->
                        // Book info section (simplified)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Cover image
                            if (info.cover != null) {
                                BookCoverImageLarge(
                                    epubFile = java.io.File(progress.epubPath),
                                    size = 80.dp,
                                    cornerRadius = 8.dp
                                )
                            }

                            // Basic info
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = info.getDisplayTitle(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = "by ${info.getDisplayAuthor()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Separator
                        androidx.compose.material3.HorizontalDivider()

                        // Remove options section
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Remove from recent books",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )

                            Text(
                                text = "This will remove the book from your recent reading list.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Reset progress checkbox
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = resetProgress,
                                    onCheckedChange = { resetProgress = it }
                                )
                                Text(
                                    text = "Also reset reading progress",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            if (resetProgress) {
                                Text(
                                    text = "⚠️ This will permanently delete your bookmarks and reading position.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
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
                        onRemove(resetProgress)
                        onDismiss()
                    }
                ) {
                    Text("Remove")
                }
            }
        },
        dismissButton = null
    )
}

private fun formatLastRead(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < ONE_MINUTE_MILLIS -> "Just now"
        diff < ONE_HOUR_MILLIS -> "${diff / ONE_MINUTE_MILLIS}m ago"
        diff < ONE_DAY_MILLIS -> "${diff / ONE_HOUR_MILLIS}h ago"
        diff < ONE_WEEK_MILLIS -> "${diff / ONE_DAY_MILLIS}d ago"
        else -> {
            val formatter = SimpleDateFormat("MMM dd", Locale.getDefault())
            formatter.format(Date(timestamp))
        }
    }
}
