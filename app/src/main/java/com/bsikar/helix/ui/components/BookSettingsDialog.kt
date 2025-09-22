package com.bsikar.helix.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bsikar.helix.data.Book
import com.bsikar.helix.data.CoverDisplayMode
import com.bsikar.helix.theme.AppTheme

/**
 * Settings dialog for individual book preferences
 */
data class BookMetadataUpdate(
    val title: String,
    val author: String,
    val description: String?,
    val publisher: String?,
    val language: String?,
    val isbn: String?,
    val publishedDate: String?,
    val coverDisplayMode: CoverDisplayMode,
    val userSelectedColor: Color?
)

@Composable
fun BookSettingsDialog(
    book: Book,
    theme: AppTheme,
    onDismiss: () -> Unit,
    onSaveSettings: (BookMetadataUpdate) -> Unit
) {
    var selectedDisplayMode by remember { mutableStateOf(book.coverDisplayMode) }
    var selectedColor by remember { 
        mutableStateOf(
            if (book.userSelectedColor != null) Color(book.userSelectedColor) else book.coverColorComposeColor
        ) 
    }
    
    // Metadata editing state
    var editingTitle by remember { mutableStateOf(book.title) }
    var editingAuthor by remember { mutableStateOf(book.author) }
    var editingDescription by remember { mutableStateOf(book.description ?: "") }
    var editingPublisher by remember { mutableStateOf(book.publisher ?: "") }
    var editingLanguage by remember { mutableStateOf(book.language ?: "") }
    var editingIsbn by remember { mutableStateOf(book.isbn ?: "") }
    var editingPublishedDate by remember { mutableStateOf(book.publishedDate ?: "") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            colors = CardDefaults.cardColors(containerColor = theme.surfaceColor),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Surface(
                    color = theme.accentColor,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Settings,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Book Settings",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = book.title,
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.9f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }
                }
                
                // Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Metadata editing section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = theme.backgroundColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = null,
                                    tint = theme.accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Edit Metadata",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = theme.primaryTextColor
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Title field
                            OutlinedTextField(
                                value = editingTitle,
                                onValueChange = { editingTitle = it },
                                label = { Text("Title") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = theme.accentColor,
                                    focusedLabelColor = theme.accentColor
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Author field
                            OutlinedTextField(
                                value = editingAuthor,
                                onValueChange = { editingAuthor = it },
                                label = { Text("Author") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = theme.accentColor,
                                    focusedLabelColor = theme.accentColor
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Description field
                            OutlinedTextField(
                                value = editingDescription,
                                onValueChange = { editingDescription = it },
                                label = { Text("Description") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 4,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = theme.accentColor,
                                    focusedLabelColor = theme.accentColor
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Publisher and Language in a row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = editingPublisher,
                                    onValueChange = { editingPublisher = it },
                                    label = { Text("Publisher") },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = theme.accentColor,
                                        focusedLabelColor = theme.accentColor
                                    )
                                )
                                
                                OutlinedTextField(
                                    value = editingLanguage,
                                    onValueChange = { editingLanguage = it },
                                    label = { Text("Language") },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = theme.accentColor,
                                        focusedLabelColor = theme.accentColor
                                    )
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // ISBN and Published Date in a row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = editingIsbn,
                                    onValueChange = { editingIsbn = it },
                                    label = { Text("ISBN") },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = theme.accentColor,
                                        focusedLabelColor = theme.accentColor
                                    )
                                )
                                
                                OutlinedTextField(
                                    value = editingPublishedDate,
                                    onValueChange = { editingPublishedDate = it },
                                    label = { Text("Published Date") },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = theme.accentColor,
                                        focusedLabelColor = theme.accentColor
                                    )
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Cover display and color settings
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = theme.backgroundColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Cover Display",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = theme.primaryTextColor
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            ColorPicker(
                                currentColor = selectedColor,
                                currentDisplayMode = selectedDisplayMode,
                                onColorSelected = { selectedColor = it },
                                onDisplayModeChanged = { selectedDisplayMode = it },
                                theme = theme
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Preview card
                    Text(
                        text = "Preview",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = theme.primaryTextColor
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Mini book preview
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        colors = CardDefaults.cardColors(containerColor = theme.backgroundColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mini book cover preview
                            val previewColor = when (selectedDisplayMode) {
                                CoverDisplayMode.COLOR_ONLY -> selectedColor
                                CoverDisplayMode.AUTO -> {
                                    if (book.hasCoverArt()) {
                                        // In real implementation, would show cover art
                                        Color.LightGray
                                    } else {
                                        selectedColor
                                    }
                                }
                                CoverDisplayMode.COVER_ART_ONLY -> {
                                    if (book.hasCoverArt()) {
                                        Color.LightGray
                                    } else {
                                        Color(0xFF424242)
                                    }
                                }
                            }
                            
                            Card(
                                modifier = Modifier
                                    .width(60.dp)
                                    .height(80.dp),
                                colors = CardDefaults.cardColors(containerColor = previewColor),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (selectedDisplayMode != CoverDisplayMode.COLOR_ONLY && book.hasCoverArt()) {
                                    // Would show actual cover art here
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Cover\nArt",
                                            fontSize = 10.sp,
                                            color = Color.White,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column {
                                Text(
                                    text = editingTitle,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = theme.primaryTextColor,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = editingAuthor,
                                    fontSize = 12.sp,
                                    color = theme.secondaryTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Display: ${when (selectedDisplayMode) {
                                        CoverDisplayMode.AUTO -> "Auto"
                                        CoverDisplayMode.COLOR_ONLY -> "Color Only"
                                        CoverDisplayMode.COVER_ART_ONLY -> "Cover Art Only"
                                    }}",
                                    fontSize = 10.sp,
                                    color = theme.accentColor
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = theme.secondaryTextColor
                            )
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = { 
                                val colorToSave = when (selectedDisplayMode) {
                                    CoverDisplayMode.COLOR_ONLY, CoverDisplayMode.AUTO -> selectedColor
                                    CoverDisplayMode.COVER_ART_ONLY -> null
                                }
                                val metadataUpdate = BookMetadataUpdate(
                                    title = editingTitle.trim(),
                                    author = editingAuthor.trim(),
                                    description = editingDescription.trim().takeIf { it.isNotEmpty() },
                                    publisher = editingPublisher.trim().takeIf { it.isNotEmpty() },
                                    language = editingLanguage.trim().takeIf { it.isNotEmpty() },
                                    isbn = editingIsbn.trim().takeIf { it.isNotEmpty() },
                                    publishedDate = editingPublishedDate.trim().takeIf { it.isNotEmpty() },
                                    coverDisplayMode = selectedDisplayMode,
                                    userSelectedColor = colorToSave
                                )
                                onSaveSettings(metadataUpdate)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = theme.accentColor
                            )
                        ) {
                            Icon(
                                Icons.Filled.Save,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}