package com.bsikar.helix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bsikar.helix.data.model.Bookmark
import com.bsikar.helix.theme.AppTheme

@Composable
fun BookmarkDialog(
    bookmarks: List<com.bsikar.helix.data.model.Bookmark>,
    onDismiss: () -> Unit,
    onBookmarkClick: (Bookmark) -> Unit,
    onBookmarkDelete: (String) -> Unit,
    onBookmarkEditNote: (String, String) -> Unit,
    theme: AppTheme,
    currentChapter: Int? = null,
    currentPage: Int? = null,
    onQuickBookmark: (() -> Unit)? = null
) {
    var editingBookmark by remember { mutableStateOf<String?>(null) }
    var editNote by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bookmarks",
                    color = theme.primaryTextColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${bookmarks.size} bookmark${if (bookmarks.size != 1) "s" else ""}",
                    color = theme.secondaryTextColor,
                    fontSize = 14.sp
                )
            }
        },
        text = {
            Column {
                // Quick bookmark section if current position is provided
                if (currentChapter != null && currentPage != null && onQuickBookmark != null) {
                    val isCurrentBookmarked = bookmarks.any { 
                        it.chapterNumber == currentChapter && it.pageNumber == currentPage 
                    }
                    
                    if (!isCurrentBookmarked) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onQuickBookmark() }
                                .padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = theme.accentColor.copy(alpha = 0.1f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.BookmarkAdd,
                                    contentDescription = "Add bookmark",
                                    tint = theme.accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Bookmark Current Page",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = theme.primaryTextColor
                                    )
                                    Text(
                                        text = "Chapter $currentChapter, Page $currentPage",
                                        fontSize = 12.sp,
                                        color = theme.secondaryTextColor
                                    )
                                }
                                Icon(
                                    Icons.Filled.Add,
                                    contentDescription = "Add",
                                    tint = theme.accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
                
                if (bookmarks.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.BookmarkBorder,
                            contentDescription = "No bookmarks",
                            tint = theme.secondaryTextColor.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No bookmarks yet",
                            fontSize = 18.sp,
                            color = theme.secondaryTextColor,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Tap the bookmark icon to save your reading position",
                            fontSize = 14.sp,
                            color = theme.secondaryTextColor.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.height(400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                    items(bookmarks) { bookmark ->
                        BookmarkItem(
                            bookmark = bookmark,
                            isEditing = editingBookmark == bookmark.id,
                            editNote = editNote,
                            onEditNoteChange = { editNote = it },
                            onEditStart = { 
                                editingBookmark = bookmark.id
                                editNote = bookmark.note
                            },
                            onEditSave = {
                                onBookmarkEditNote(bookmark.id, editNote)
                                editingBookmark = null
                                editNote = ""
                            },
                            onEditCancel = {
                                editingBookmark = null
                                editNote = ""
                            },
                            onClick = { onBookmarkClick(bookmark) },
                            onDelete = { onBookmarkDelete(bookmark.id) },
                            theme = theme
                        )
                    }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = theme.accentColor)
            }
        },
        containerColor = theme.surfaceColor
    )
}

@Composable
fun BookmarkItem(
    bookmark: com.bsikar.helix.data.model.Bookmark,
    isEditing: Boolean,
    editNote: String,
    onEditNoteChange: (String) -> Unit,
    onEditStart: () -> Unit,
    onEditSave: () -> Unit,
    onEditCancel: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    theme: AppTheme
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (!isEditing) onClick() },
        colors = CardDefaults.cardColors(
            containerColor = theme.backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header with location and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Chapter ${bookmark.chapterNumber}, Page ${bookmark.pageNumber}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = theme.primaryTextColor
                    )
                    Text(
                        text = bookmark.getTimeAgoText(),
                        fontSize = 12.sp,
                        color = theme.secondaryTextColor
                    )
                }
                
                if (!isEditing) {
                    Row {
                        IconButton(
                            onClick = onEditStart,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "Edit note",
                                tint = theme.secondaryTextColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Delete bookmark",
                                tint = theme.secondaryTextColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Note section
            if (isEditing) {
                Column {
                    OutlinedTextField(
                        value = editNote,
                        onValueChange = onEditNoteChange,
                        label = { Text("Note (optional)", color = theme.secondaryTextColor) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = theme.accentColor,
                            unfocusedBorderColor = theme.secondaryTextColor.copy(alpha = 0.3f),
                            focusedLabelColor = theme.accentColor,
                            unfocusedLabelColor = theme.secondaryTextColor,
                            cursorColor = theme.accentColor,
                            focusedTextColor = theme.primaryTextColor,
                            unfocusedTextColor = theme.primaryTextColor
                        ),
                        maxLines = 3
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onEditCancel) {
                            Text("Cancel", color = theme.secondaryTextColor)
                        }
                        TextButton(onClick = onEditSave) {
                            Text("Save", color = theme.accentColor)
                        }
                    }
                }
            } else {
                if (bookmark.note.isNotBlank()) {
                    Text(
                        text = bookmark.note,
                        fontSize = 13.sp,
                        color = theme.primaryTextColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(theme.accentColor.copy(alpha = 0.1f))
                            .padding(8.dp)
                    )
                } else {
                    Text(
                        text = "Tap to go to this location",
                        fontSize = 13.sp,
                        color = theme.secondaryTextColor.copy(alpha = 0.7f),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
    }
}