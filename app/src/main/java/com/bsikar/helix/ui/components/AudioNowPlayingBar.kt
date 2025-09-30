package com.bsikar.helix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bsikar.helix.player.PlaybackState
import com.bsikar.helix.theme.AppTheme
import kotlin.math.roundToInt

@Composable
fun AudioNowPlayingBar(
    playbackState: PlaybackState,
    theme: AppTheme,
    modifier: Modifier = Modifier,
    onExpand: () -> Unit = {},
    onPlayPause: () -> Unit = {},
    onSkipForward: () -> Unit = {},
    onSkipBackward: () -> Unit = {}
) {
    val book = playbackState.currentBook ?: return
    val progress by remember(playbackState.currentPositionMs, playbackState.durationMs) {
        derivedStateOf {
            if (playbackState.durationMs <= 0L) 0f
            else (playbackState.currentPositionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
        }
    }
    val progressDescription by remember(progress, playbackState.durationMs) {
        derivedStateOf {
            if (playbackState.durationMs <= 0L) {
                ""
            } else {
                val percent = (progress * 100).coerceIn(0f, 100f).roundToInt()
                "$percent% complete"
            }
        }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = theme.surfaceColor.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .background(Color.Transparent)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onExpand)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(book.getEffectiveCoverColor()),
                    contentAlignment = Alignment.Center
                ) {
                    if (book.shouldShowCoverArt() && !book.coverImagePath.isNullOrEmpty()) {
                        AsyncImage(
                            model = book.coverImagePath,
                            contentDescription = book.title,
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = theme.primaryTextColor.copy(alpha = theme.alphaMedium)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = book.title,
                        color = theme.primaryTextColor,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (book.author.isNotBlank()) {
                        Text(
                            text = book.author,
                            color = theme.secondaryTextColor,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    playbackState.currentChapter?.let { chapter ->
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = chapter.title,
                            color = theme.accentColor,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = buildString {
                            val position = book.getFormattedPosition()
                            if (position.isNotEmpty()) {
                                append(position)
                                append(" · ")
                            }
                            val duration = book.getFormattedDuration()
                            if (duration.isNotEmpty()) {
                                append(duration)
                            }
                        },
                        color = theme.secondaryTextColor.copy(alpha = theme.alphaHigh),
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onSkipBackward) {
                        Icon(
                            imageVector = Icons.Filled.SkipPrevious,
                            contentDescription = "Previous",
                            tint = theme.primaryTextColor
                        )
                    }

                    IconButton(onClick = onPlayPause) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                            tint = theme.primaryTextColor
                        )
                    }

                    IconButton(onClick = onSkipForward) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Next",
                            tint = theme.primaryTextColor
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .semantics {
                        progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                        if (progressDescription.isNotEmpty()) {
                            contentDescription = progressDescription
                        }
                    },
                color = theme.accentColor,
                trackColor = theme.accentColor.copy(alpha = theme.alphaSubtle)
            )
        }
    }
}
