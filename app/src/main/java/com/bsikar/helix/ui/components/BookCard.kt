package com.bsikar.helix.ui.components

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bsikar.helix.data.model.Book
import com.bsikar.helix.data.model.ReadingStatus
import com.bsikar.helix.data.model.CoverDisplayMode
import com.bsikar.helix.data.model.BookType
import com.bsikar.helix.theme.AppTheme
import com.bsikar.helix.ui.components.SearchUtils
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookCard(
    book: com.bsikar.helix.data.model.Book, 
    showProgress: Boolean, 
    theme: AppTheme,
    searchQuery: String = "",
    isBrowseMode: Boolean = false,
    onBookClick: (Book) -> Unit = {},
    onStartReading: (String) -> Unit = {},
    onMarkCompleted: (String) -> Unit = {},
    onMoveToOnDeck: (String) -> Unit = {},
    onSetProgress: (String, Float) -> Unit = { _, _ -> },
    onEditTags: (String, List<String>) -> Unit = { _, _ -> },
    onUpdateBookSettings: (Book) -> Unit = { _ -> },
    onRemoveFromLibrary: (String) -> Unit = { _ -> }
) {
    var showContextMenu by remember { mutableStateOf(false) }
    var showTagEditor by remember { mutableStateOf(false) }
    var showBookSettings by remember { mutableStateOf(false) }
    
    // Make audiobook cards visually distinct and smaller
    val cardWidth = if (book.isAudiobook()) {
        if (showProgress) 110.dp else 100.dp
    } else {
        if (showProgress) 120.dp else 110.dp
    }
    
    val aspectRatio = if (book.isAudiobook()) 0.75f else 0.68f // More square for audiobooks
    
    Column(
        modifier = Modifier
            .width(cardWidth)
            .combinedClickable(
                onClick = { onBookClick(book) },
                onLongClick = { showContextMenu = true }
            )
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(aspectRatio)
                .clip(RoundedCornerShape(if (book.isAudiobook()) 12.dp else 8.dp))
                .background(
                    // Always show a color background as fallback
                    when (book.coverDisplayMode) {
                        CoverDisplayMode.COLOR_ONLY -> book.getEffectiveCoverColor()
                        CoverDisplayMode.AUTO -> {
                            // Use user selected color or default
                            val colorValue = book.userSelectedColor ?: book.coverColor
                            Log.d("BookCard", "AUTO mode - userSelectedColor: ${book.userSelectedColor}, coverColor: ${book.coverColor}, using: $colorValue")
                            try {
                                val finalColor = Color(colorValue.toULong())
                                Log.d("BookCard", "Final color: $finalColor")
                                finalColor
                            } catch (e: Exception) {
                                Log.e("BookCard", "Error converting color $colorValue", e)
                                Color(0xFF6B73FF)
                            }
                        }
                        CoverDisplayMode.COVER_ART_ONLY -> {
                            if (book.hasCoverArt()) {
                                Color(0xFF424242) // Gray background for cover art
                            } else {
                                Color(0xFF424242) // Gray placeholder
                            }
                        }
                    }
                )
        ) {
            // Display cover art when appropriate
            if ((book.coverDisplayMode == CoverDisplayMode.AUTO && book.hasCoverArt()) || 
                (book.coverDisplayMode == CoverDisplayMode.COVER_ART_ONLY && book.hasCoverArt())) {
                val coverFile = File(book.coverImagePath!!)
                Log.d("BookCard", "Attempting to load cover art: ${book.coverImagePath}, exists: ${coverFile.exists()}, size: ${if (coverFile.exists()) coverFile.length() else "N/A"}")
                
                AsyncImage(
                    model = coverFile,
                    contentDescription = "Book cover",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onSuccess = {
                        Log.d("BookCard", "Successfully loaded cover art for book: ${book.title}")
                    },
                    onError = { error ->
                        Log.e("BookCard", "Failed to load cover art from ${book.coverImagePath}: ${error.result.throwable?.message}")
                        Log.e("BookCard", "File exists: ${coverFile.exists()}, canRead: ${coverFile.canRead()}")
                    }
                )
            }
            
            // Status indicator in top-right corner
            val statusColor = when (book.readingStatus) {
                ReadingStatus.UNREAD -> Color.Transparent // No indicator for unread books
                ReadingStatus.PLAN_TO_READ -> theme.secondaryTextColor.copy(alpha = theme.alphaHigh)
                ReadingStatus.READING -> theme.accentColor
                ReadingStatus.PLAN_TO_LISTEN -> theme.secondaryTextColor.copy(alpha = theme.alphaHigh)
                ReadingStatus.LISTENING -> theme.accentColor
                ReadingStatus.COMPLETED -> theme.successColor
            }
            
            val statusIcon = when (book.readingStatus) {
                ReadingStatus.UNREAD -> Icons.Filled.Circle // Placeholder, won't be shown due to transparent color
                ReadingStatus.PLAN_TO_READ -> Icons.Filled.Schedule
                ReadingStatus.READING -> Icons.Filled.PlayArrow
                ReadingStatus.PLAN_TO_LISTEN -> Icons.Filled.Schedule
                ReadingStatus.LISTENING -> Icons.AutoMirrored.Filled.VolumeUp
                ReadingStatus.COMPLETED -> Icons.Filled.CheckCircle
            }
            
            // Only show status indicator for books that are not UNREAD
            if (book.readingStatus != ReadingStatus.UNREAD) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = (-4).dp, y = 4.dp)
                        .background(
                            statusColor,
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = book.readingStatus.name,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            
            val effectiveProgress = if (book.isAudiobook()) book.getAudioProgress() else book.progress
            if (showProgress && effectiveProgress > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color.Black.copy(alpha = 0.15f))
                        .align(Alignment.BottomCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(effectiveProgress)
                            .fillMaxHeight()
                            .background(theme.accentColor)
                            .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = SearchUtils.createHighlightedText(
                text = book.title,
                query = searchQuery,
                baseColor = theme.primaryTextColor,
                highlightColor = theme.accentColor,
                fontSize = 13.sp,
                highlightFontWeight = FontWeight.Bold
            ),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        
        Spacer(modifier = Modifier.height(2.dp))
        
        Text(
            text = SearchUtils.createHighlightedText(
                text = book.author,
                query = searchQuery,
                baseColor = theme.secondaryTextColor,
                highlightColor = theme.accentColor,
                fontSize = 11.sp,
                highlightFontWeight = FontWeight.Bold
            ),
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        
        // Show enhanced audiobook info
        if (book.isAudiobook()) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(theme.accentColor.copy(alpha = theme.alphaOverlay))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Filled.Headphones,
                        contentDescription = "Audiobook",
                        modifier = Modifier.size(10.dp),
                        tint = theme.accentColor
                    )
                    Text(
                        text = book.getFormattedDuration(),
                        fontSize = 10.sp,
                        color = theme.accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        if (showContextMenu) {
            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false },
                modifier = Modifier.background(theme.surfaceColor)
            ) {
                when (book.readingStatus) {
                    ReadingStatus.UNREAD -> {
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    "Add to On Deck",
                                    color = theme.primaryTextColor
                                )
                            },
                            onClick = {
                                onMoveToOnDeck(book.id)
                                showContextMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint = theme.secondaryTextColor
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    if (book.isAudiobook()) "Start Listening" else "Start Reading",
                                    color = theme.primaryTextColor
                                )
                            },
                            onClick = {
                                onStartReading(book.id)
                                showContextMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = theme.accentColor
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    if (book.isAudiobook()) "Mark as Finished" else "Mark as Read",
                                    color = theme.primaryTextColor
                                )
                            },
                            onClick = {
                                onMarkCompleted(book.id)
                                showContextMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = theme.successColor
                                )
                            }
                        )
                    }
                    ReadingStatus.PLAN_TO_READ -> {
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    if (book.isAudiobook()) "Start Listening" else "Start Reading",
                                    color = theme.primaryTextColor
                                )
                            },
                            onClick = {
                                onStartReading(book.id)
                                showContextMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = theme.accentColor
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    if (book.isAudiobook()) "Mark as Finished" else "Mark as Read",
                                    color = theme.primaryTextColor
                                )
                            },
                            onClick = {
                                onMarkCompleted(book.id)
                                showContextMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = theme.successColor
                                )
                            }
                        )
                    }
                    ReadingStatus.READING -> {
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    "Mark as Completed",
                                    color = theme.primaryTextColor
                                )
                            },
                            onClick = {
                                onMarkCompleted(book.id)
                                showContextMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = theme.accentColor
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    "Move to On Deck",
                                    color = theme.primaryTextColor
                                )
                            },
                            onClick = {
                                onMoveToOnDeck(book.id)
                                showContextMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint = theme.secondaryTextColor
                                )
                            }
                        )
                    }
                    ReadingStatus.PLAN_TO_LISTEN -> {
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    "Start Listening",
                                    color = theme.primaryTextColor
                                )
                            },
                            onClick = {
                                onStartReading(book.id)
                                showContextMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = theme.accentColor
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    "Mark as Finished",
                                    color = theme.primaryTextColor
                                )
                            },
                            onClick = {
                                onMarkCompleted(book.id)
                                showContextMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = theme.successColor
                                )
                            }
                        )
                    }
                    ReadingStatus.LISTENING -> {
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    "Mark as Completed",
                                    color = theme.primaryTextColor
                                )
                            },
                            onClick = {
                                onMarkCompleted(book.id)
                                showContextMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = theme.accentColor
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    "Move to On Deck",
                                    color = theme.primaryTextColor
                                )
                            },
                            onClick = {
                                onMoveToOnDeck(book.id)
                                showContextMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint = theme.secondaryTextColor
                                )
                            }
                        )
                    }
                    ReadingStatus.COMPLETED -> {
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    "Move to On Deck",
                                    color = theme.primaryTextColor
                                )
                            },
                            onClick = {
                                onMoveToOnDeck(book.id)
                                showContextMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint = theme.secondaryTextColor
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    if (book.isAudiobook()) "Mark as Listening" else "Mark as Reading",
                                    color = theme.primaryTextColor
                                )
                            },
                            onClick = {
                                onStartReading(book.id)
                                showContextMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Book,
                                    contentDescription = null,
                                    tint = theme.accentColor
                                )
                            }
                        )
                    }
                }
                
                // Add "Edit Tags" option for all books
                DropdownMenuItem(
                    text = { 
                        Text(
                            "Edit Tags",
                            color = theme.primaryTextColor
                        )
                    },
                    onClick = {
                        showTagEditor = true
                        showContextMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = null,
                            tint = theme.accentColor
                        )
                    }
                )
                
                // Add "Book Settings" option for all books
                DropdownMenuItem(
                    text = { 
                        Text(
                            "Book Settings",
                            color = theme.primaryTextColor
                        )
                    },
                    onClick = {
                        showBookSettings = true
                        showContextMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = null,
                            tint = theme.accentColor
                        )
                    }
                )
                
                // Add "Move to Browse" or "Remove from App" option based on context
                DropdownMenuItem(
                    text = { 
                        Text(
                            if (isBrowseMode) "Remove from App" else "Move to Browse",
                            color = theme.primaryTextColor
                        )
                    },
                    onClick = {
                        onRemoveFromLibrary(book.id)
                        showContextMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.RemoveCircle,
                            contentDescription = null,
                            tint = theme.errorColor
                        )
                    }
                )
            }
        }
    }
    
    // Tag Editor Dialog
    if (showTagEditor) {
        TagEditorDialog(
            book = book,
            theme = theme,
            onDismiss = { showTagEditor = false },
            onTagsUpdated = { newTags ->
                onEditTags(book.id, newTags)
            }
        )
    }
    
    // Book Settings Dialog
    if (showBookSettings) {
        BookSettingsDialog(
            book = book,
            theme = theme,
            onDismiss = { showBookSettings = false },
            onSaveSettings = { metadataUpdate ->
                Log.d("BookCard", "onSaveSettings: userSelectedColor = ${metadataUpdate.userSelectedColor}")
                Log.d("BookCard", "onSaveSettings: coverDisplayMode = ${metadataUpdate.coverDisplayMode}")
                
                // Convert color properly - the issue might be here
                val colorValue = metadataUpdate.userSelectedColor?.let { color ->
                    // Convert ULong color value to Long safely
                    val colorULong = color.value
                    // Always use the full color value as signed Long
                    colorULong.toLong()
                }
                Log.d("BookCard", "onSaveSettings: converted colorValue = $colorValue")
                println("BookCard.onSaveSettings: Before copy - book progress: ${book.progress}, explicitStatus: ${book.explicitReadingStatus}")
                val updatedBook = book.copy(
                    // Update only the metadata fields from the dialog
                    title = metadataUpdate.title,
                    author = metadataUpdate.author,
                    description = metadataUpdate.description,
                    publisher = metadataUpdate.publisher,
                    language = metadataUpdate.language,
                    isbn = metadataUpdate.isbn,
                    publishedDate = metadataUpdate.publishedDate,
                    coverDisplayMode = metadataUpdate.coverDisplayMode,
                    userSelectedColor = colorValue,
                    userEditedMetadata = true
                    // NOTE: All other fields (progress, explicitReadingStatus, currentPage, etc.) 
                    // are automatically preserved by copy()
                )
                println("BookCard.onSaveSettings: After copy - updatedBook progress: ${updatedBook.progress}, explicitStatus: ${updatedBook.explicitReadingStatus}")
                showBookSettings = false
                onUpdateBookSettings(updatedBook)
            }
        )
    }
}