package com.bsikar.helix.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bsikar.helix.data.Book
import com.bsikar.helix.data.ReadingStatus
import com.bsikar.helix.theme.AppTheme
import com.bsikar.helix.ui.components.HighlightedText

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookCard(
    book: Book, 
    showProgress: Boolean, 
    theme: AppTheme,
    searchQuery: String = "",
    onBookClick: (Book) -> Unit = {},
    onStartReading: (String) -> Unit = {},
    onMarkCompleted: (String) -> Unit = {},
    onMoveToPlanToRead: (String) -> Unit = {},
    onSetProgress: (String, Float) -> Unit = { _, _ -> },
    onEditTags: (String, List<String>) -> Unit = { _, _ -> }
) {
    var showContextMenu by remember { mutableStateOf(false) }
    var showTagEditor by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .width(if (showProgress) 120.dp else 110.dp)
            .combinedClickable(
                onClick = { onBookClick(book) },
                onLongClick = { showContextMenu = true }
            )
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(0.68f)
                .clip(RoundedCornerShape(8.dp))
                .background(book.coverColor)
        ) {
            // Status indicator in top-right corner
            val statusColor = when (book.readingStatus) {
                ReadingStatus.PLAN_TO_READ -> theme.secondaryTextColor.copy(alpha = 0.7f)
                ReadingStatus.READING -> theme.accentColor
                ReadingStatus.COMPLETED -> Color(0xFF4CAF50)
            }
            
            val statusIcon = when (book.readingStatus) {
                ReadingStatus.PLAN_TO_READ -> Icons.Filled.Schedule
                ReadingStatus.READING -> Icons.Filled.PlayArrow
                ReadingStatus.COMPLETED -> Icons.Filled.CheckCircle
            }
            
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
            
            if (showProgress && book.progress > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color.Black.copy(alpha = 0.15f))
                        .align(Alignment.BottomCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(book.progress)
                            .fillMaxHeight()
                            .background(theme.accentColor)
                            .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        HighlightedText(
            text = book.title,
            searchQuery = searchQuery,
            normalColor = theme.primaryTextColor,
            highlightColor = theme.accentColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(2.dp))
        
        HighlightedText(
            text = book.author,
            searchQuery = searchQuery,
            normalColor = theme.secondaryTextColor,
            highlightColor = theme.accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal
        )
        
        if (showContextMenu) {
            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false },
                modifier = Modifier.background(theme.surfaceColor)
            ) {
                when (book.readingStatus) {
                    ReadingStatus.PLAN_TO_READ -> {
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    "Start Reading",
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
                                    "Move to Plan to Read",
                                    color = theme.primaryTextColor
                                )
                            },
                            onClick = {
                                onMoveToPlanToRead(book.id)
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
                                    "Move to Plan to Read",
                                    color = theme.primaryTextColor
                                )
                            },
                            onClick = {
                                onMoveToPlanToRead(book.id)
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
                                    "Mark as Reading",
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
}