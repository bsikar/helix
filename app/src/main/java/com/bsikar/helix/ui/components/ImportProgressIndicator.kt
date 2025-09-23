package com.bsikar.helix.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bsikar.helix.R
import com.bsikar.helix.data.model.ImportProgress
import com.bsikar.helix.data.model.ImportStatus
import com.bsikar.helix.data.model.ImportTask

/**
 * Composable that displays import progress notifications
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportProgressIndicator(
    importProgress: List<ImportProgress>,
    onDismiss: (String) -> Unit = {},
    onCancel: (String) -> Unit = {},
    onRetry: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val visibleProgress = importProgress.filter { it.isVisible }
    
    AnimatedVisibility(
        visible = visibleProgress.isNotEmpty(),
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut(),
        modifier = modifier
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(visibleProgress, key = { it.id }) { progress ->
                ImportProgressCard(
                    progress = progress,
                    onDismiss = { onDismiss(progress.id) },
                    onCancel = { onCancel(progress.id) },
                    onRetry = { onRetry(progress.id) },
                    modifier = Modifier
                )
            }
        }
    }
}

/**
 * Individual import progress card
 */
@Composable
private fun ImportProgressCard(
    progress: ImportProgress,
    onDismiss: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = getStatusColor(progress.status).copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with file name and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = progress.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = getStatusText(progress.status),
                        style = MaterialTheme.typography.bodySmall,
                        color = getStatusColor(progress.status)
                    )
                }
                
                // Status icon and action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = getStatusIcon(progress.status),
                        contentDescription = getStatusText(progress.status),
                        tint = getStatusColor(progress.status),
                        modifier = Modifier.size(24.dp)
                    )
                    
                    when (progress.status) {
                        ImportStatus.IN_PROGRESS, ImportStatus.PENDING -> {
                            IconButton(
                                onClick = onCancel,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.cancel),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        ImportStatus.FAILED -> {
                            IconButton(
                                onClick = onRetry,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.retry),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        ImportStatus.COMPLETED, ImportStatus.CANCELLED -> {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.dismiss),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Progress message
            if (progress.message.isNotEmpty()) {
                Text(
                    text = progress.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Progress bar for active imports
            if (progress.status in listOf(ImportStatus.PENDING, ImportStatus.IN_PROGRESS)) {
                LinearProgressIndicator(
                    progress = { progress.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = getStatusColor(progress.status),
                    trackColor = getStatusColor(progress.status).copy(alpha = 0.3f),
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "${progress.progress}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Compact import progress overlay for minimal UI
 */
@Composable
fun CompactImportProgress(
    activeImports: List<ImportProgress>,
    modifier: Modifier = Modifier
) {
    val activeCount = activeImports.size
    
    AnimatedVisibility(
        visible = activeCount > 0,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut(),
        modifier = modifier
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = if (activeCount == 1) {
                        "Importing ${activeImports.first().fileName}..."
                    } else {
                        "Importing $activeCount files..."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * Import history list component
 */
@Composable
fun ImportHistoryList(
    importTasks: List<ImportTask>,
    onRetry: (String) -> Unit = {},
    onDelete: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(importTasks, key = { it.id }) { task ->
            ImportHistoryCard(
                task = task,
                onRetry = { onRetry(task.id) },
                onDelete = { onDelete(task.id) }
            )
        }
    }
}

/**
 * Individual import history card
 */
@Composable
private fun ImportHistoryCard(
    task: ImportTask,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = getStatusText(task.status),
                    style = MaterialTheme.typography.bodySmall,
                    color = getStatusColor(task.status)
                )
                
                if (task.errorMessage != null) {
                    Text(
                        text = task.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (task.status == ImportStatus.FAILED) {
                    IconButton(onClick = onRetry) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.retry)
                        )
                    }
                }
                
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete)
                    )
                }
            }
        }
    }
}

// Helper functions
private fun getStatusColor(status: ImportStatus): Color {
    return when (status) {
        ImportStatus.PENDING -> Color(0xFFFF9800) // Orange
        ImportStatus.IN_PROGRESS -> Color(0xFF2196F3) // Blue
        ImportStatus.COMPLETED -> Color(0xFF4CAF50) // Green
        ImportStatus.FAILED -> Color(0xFFF44336) // Red
        ImportStatus.CANCELLED -> Color(0xFF9E9E9E) // Gray
    }
}

private fun getStatusIcon(status: ImportStatus) = when (status) {
    ImportStatus.PENDING -> Icons.Default.Schedule
    ImportStatus.IN_PROGRESS -> Icons.Default.Download
    ImportStatus.COMPLETED -> Icons.Default.CheckCircle
    ImportStatus.FAILED -> Icons.Default.Error
    ImportStatus.CANCELLED -> Icons.Default.Cancel
}

private fun getStatusText(status: ImportStatus): String {
    return when (status) {
        ImportStatus.PENDING -> "Queued"
        ImportStatus.IN_PROGRESS -> "Importing..."
        ImportStatus.COMPLETED -> "Completed"
        ImportStatus.FAILED -> "Failed"
        ImportStatus.CANCELLED -> "Cancelled"
    }
}