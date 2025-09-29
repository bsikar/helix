package com.bsikar.helix.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.bsikar.helix.theme.AppTheme

@Composable
fun SmoothProgressBar(
    currentPosition: Long,
    duration: Long,
    theme: AppTheme,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(0f) }
    var lastKnownPosition by remember { mutableLongStateOf(currentPosition) }
    var ignoreUpdatesUntil by remember { mutableLongStateOf(0L) }
    
    val density = LocalDensity.current
    
    // Update lastKnownPosition only when we're not dragging and not ignoring updates
    LaunchedEffect(currentPosition, isDragging) {
        if (!isDragging && System.currentTimeMillis() > ignoreUpdatesUntil) {
            lastKnownPosition = currentPosition
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(duration) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                            dragPosition = fraction
                        },
                        onDragEnd = {
                            val seekPosition = (dragPosition * duration).toLong()
                            onSeek(seekPosition)
                            lastKnownPosition = seekPosition
                            ignoreUpdatesUntil = System.currentTimeMillis() + 1000 // Ignore updates for 1 second
                            isDragging = false
                        },
                        onDrag = { change, _ ->
                            val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                            dragPosition = fraction
                        }
                    )
                }
                .pointerInput(duration) {
                    detectTapGestures { offset ->
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        val seekPosition = (fraction * duration).toLong()
                        onSeek(seekPosition)
                        lastKnownPosition = seekPosition
                        ignoreUpdatesUntil = System.currentTimeMillis() + 1000 // Ignore updates for 1 second
                    }
                }
        ) {
            val trackHeight = with(density) { 6.dp.toPx() }
            val trackY = (size.height - trackHeight) / 2
            val cornerRadius = trackHeight / 2
            
            // Background track
            drawRoundRect(
                color = theme.secondaryTextColor.copy(alpha = 0.3f),
                topLeft = Offset(0f, trackY),
                size = androidx.compose.ui.geometry.Size(size.width, trackHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
            )
            
            // Progress track - use our controlled position source
            val progress = if (isDragging) {
                dragPosition
            } else if (duration > 0) {
                // Use lastKnownPosition instead of currentPosition to avoid jumps
                val fraction = (lastKnownPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                fraction
            } else {
                0f
            }
            
            if (progress > 0f) {
                drawRoundRect(
                    color = theme.accentColor,
                    topLeft = Offset(0f, trackY),
                    size = androidx.compose.ui.geometry.Size(size.width * progress, trackHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
                )
            }
        }
    }
}