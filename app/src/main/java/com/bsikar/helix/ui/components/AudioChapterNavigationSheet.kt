package com.bsikar.helix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bsikar.helix.data.model.AudioChapter
import com.bsikar.helix.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioChapterNavigationSheet(
    chapters: List<AudioChapter>,
    currentChapter: AudioChapter?,
    onChapterSelected: (AudioChapter) -> Unit,
    onDismiss: () -> Unit,
    theme: AppTheme,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = theme.backgroundColor,
        contentColor = theme.primaryTextColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "Chapters",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = theme.primaryTextColor,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Chapter List
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chapters) { chapter ->
                    AudioChapterItem(
                        chapter = chapter,
                        isCurrentChapter = chapter.id == currentChapter?.id,
                        onClick = { onChapterSelected(chapter) },
                        theme = theme
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AudioChapterItem(
    chapter: AudioChapter,
    isCurrentChapter: Boolean,
    onClick: () -> Unit,
    theme: AppTheme,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentChapter) {
                theme.accentColor.copy(alpha = 0.2f)
            } else {
                theme.backgroundColor
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCurrentChapter) 4.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Chapter Number
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isCurrentChapter) {
                            theme.accentColor
                        } else {
                            theme.secondaryTextColor.copy(alpha = 0.1f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCurrentChapter) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Currently Playing",
                        tint = theme.backgroundColor,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = chapter.order.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = theme.primaryTextColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Chapter Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = chapter.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isCurrentChapter) FontWeight.Bold else FontWeight.Normal,
                    color = theme.primaryTextColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = chapter.getFormattedStartTime(),
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.primaryTextColor.copy(alpha = 0.7f)
                    )
                    
                    Text(
                        text = chapter.getFormattedDuration(),
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.primaryTextColor.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}