package com.bsikar.helix.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bsikar.helix.player.PlaybackState
import com.bsikar.helix.theme.AppTheme

@Composable
fun PersistentAudioBar(
    playbackState: PlaybackState,
    theme: AppTheme,
    modifier: Modifier = Modifier,
    onExpand: () -> Unit,
    onPlayPause: () -> Unit
) {
    val book = playbackState.currentBook ?: return
    val progress = when {
        playbackState.durationMs > 0 -> (playbackState.currentPositionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
        book.durationMs > 0 -> book.getAudioProgress()
        else -> 0f
    }

    Surface(
        modifier = modifier,
        color = theme.surfaceColor,
        tonalElevation = 6.dp,
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onExpand() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier
                        .size(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (book.shouldShowCoverArt() && !book.coverImagePath.isNullOrBlank()) {
                            Color.Transparent
                        } else {
                            book.getEffectiveCoverColor()
                        }
                    )
                ) {
                    if (book.shouldShowCoverArt() && !book.coverImagePath.isNullOrBlank()) {
                        AsyncImage(
                            model = book.coverImagePath,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = book.title,
                        color = theme.primaryTextColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val subtitle = buildString {
                        if (book.author.isNotBlank()) {
                            append(book.author)
                        }
                        val position = book.getFormattedPosition()
                        val duration = book.getFormattedDuration()
                        if (position.isNotBlank() && duration.isNotBlank()) {
                            if (isNotEmpty()) {
                                append(" • ")
                            }
                            append(position)
                            append(" / ")
                            append(duration)
                        }
                    }
                    Text(
                        text = subtitle,
                        color = theme.secondaryTextColor,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                        tint = theme.accentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                color = theme.accentColor,
                trackColor = theme.secondaryTextColor.copy(alpha = 0.2f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
            )
        }
    }
}
