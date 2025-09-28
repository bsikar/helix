package com.bsikar.helix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay30
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.bsikar.helix.data.model.Book
import com.bsikar.helix.data.model.AudioChapter
import com.bsikar.helix.theme.AppTheme
import com.bsikar.helix.ui.components.AudioChapterNavigationSheet
import com.bsikar.helix.viewmodels.AudioBookReaderViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioBookReaderScreen(
    book: Book,
    theme: AppTheme,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AudioBookReaderViewModel = hiltViewModel()
) {
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val sleepTimerMinutes by viewModel.sleepTimerMinutes.collectAsStateWithLifecycle()
    val sleepTimerSecondsRemaining by viewModel.sleepTimerSecondsRemaining.collectAsStateWithLifecycle()
    
    var showChapterList by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isSliderBeingDragged by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    LaunchedEffect(book) {
        viewModel.loadBook(book)
    }
    
    LaunchedEffect(playbackState.currentPositionMs) {
        if (!isSliderBeingDragged) {
            sliderPosition = playbackState.currentPositionMs.toFloat()
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(theme.backgroundColor)
            .verticalScroll(scrollState)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = book.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = theme.primaryTextColor
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = theme.primaryTextColor
                    )
                }
            },
            actions = {
                IconButton(onClick = { showChapterList = true }) {
                    Icon(
                        Icons.AutoMirrored.Filled.List,
                        contentDescription = "Chapters",
                        tint = theme.primaryTextColor
                    )
                }
                IconButton(onClick = { showSleepTimer = true }) {
                    Icon(
                        Icons.Default.Bedtime,
                        contentDescription = "Sleep Timer",
                        tint = theme.primaryTextColor
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = theme.backgroundColor
            )
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Cover Art
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            if (book.shouldShowCoverArt() && !book.coverImagePath.isNullOrBlank()) {
                AsyncImage(
                    model = book.coverImagePath,
                    contentDescription = "Cover",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Fit
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(book.getEffectiveCoverColor()),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AudioFile,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Book Info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = theme.primaryTextColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = book.author,
                style = MaterialTheme.typography.bodyLarge,
                color = theme.primaryTextColor.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Current Chapter
            playbackState.currentChapter?.let { chapter ->
                Text(
                    text = chapter.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = theme.primaryTextColor.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Progress Slider
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Slider(
                value = sliderPosition,
                onValueChange = { 
                    sliderPosition = it
                    isSliderBeingDragged = true
                },
                onValueChangeFinished = {
                    viewModel.seekTo(sliderPosition.toLong())
                    isSliderBeingDragged = false
                },
                valueRange = 0f..playbackState.durationMs.toFloat(),
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = theme.accentColor,
                    activeTrackColor = theme.accentColor,
                    inactiveTrackColor = theme.secondaryTextColor.copy(alpha = 0.3f)
                )
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(if (isSliderBeingDragged) sliderPosition.toLong() else playbackState.currentPositionMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.primaryTextColor.copy(alpha = 0.7f)
                )
                Text(
                    text = formatTime(playbackState.durationMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.primaryTextColor.copy(alpha = 0.7f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Control Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Chapter
            IconButton(
                onClick = { viewModel.previousChapter() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.SkipPrevious,
                    contentDescription = "Previous Chapter",
                    tint = theme.primaryTextColor,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            // Rewind 30s
            IconButton(
                onClick = { viewModel.skipBackward() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.Replay30,
                    contentDescription = "Rewind 30s",
                    tint = theme.primaryTextColor,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            // Play/Pause
            FloatingActionButton(
                onClick = { viewModel.togglePlayPause() },
                modifier = Modifier.size(72.dp),
                containerColor = theme.accentColor,
                contentColor = Color.White
            ) {
                Icon(
                    if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(36.dp)
                )
            }
            
            // Forward 30s
            IconButton(
                onClick = { viewModel.skipForward() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.Forward30,
                    contentDescription = "Forward 30s",
                    tint = theme.primaryTextColor,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            // Next Chapter
            IconButton(
                onClick = { viewModel.nextChapter() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = "Next Chapter",
                    tint = theme.primaryTextColor,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Secondary Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            // Playback Speed
            OutlinedButton(
                onClick = { showSpeedDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = theme.primaryTextColor
                )
            ) {
                Text("${playbackState.playbackSpeed}x")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
    
    // Speed Selection Dialog
    if (showSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Playback Speed") },
            text = {
                Column {
                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickableWithoutRipple {
                                    viewModel.setPlaybackSpeed(speed)
                                    showSpeedDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = playbackState.playbackSpeed == speed,
                                onClick = {
                                    viewModel.setPlaybackSpeed(speed)
                                    showSpeedDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("${speed}x")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
    
    // Sleep Timer Dialog
    if (showSleepTimer) {
        AlertDialog(
            onDismissRequest = { showSleepTimer = false },
            title = { 
                Text(
                    if (sleepTimerMinutes != null) 
                        "Sleep Timer (${formatSleepTime(sleepTimerSecondsRemaining)})"
                    else 
                        "Set Sleep Timer"
                )
            },
            text = {
                Column {
                    if (sleepTimerMinutes != null) {
                        Text("Timer will stop playback in ${formatSleepTime(sleepTimerSecondsRemaining)}")
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            OutlinedButton(onClick = {
                                viewModel.cancelSleepTimer()
                                showSleepTimer = false
                            }) {
                                Text("Cancel Timer")
                            }
                        }
                    } else {
                        listOf(5, 10, 15, 30, 45, 60).forEach { minutes ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickableWithoutRipple {
                                        viewModel.setSleepTimer(minutes)
                                        showSleepTimer = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = false,
                                    onClick = {
                                        viewModel.setSleepTimer(minutes)
                                        showSleepTimer = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("$minutes minutes")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepTimer = false }) {
                    Text("Close")
                }
            }
        )
    }
    
    // Chapter Navigation Sheet
    if (showChapterList) {
        AudioChapterNavigationSheet(
            chapters = chapters,
            currentChapter = playbackState.currentChapter,
            onChapterSelected = { chapter ->
                viewModel.jumpToChapter(chapter)
                showChapterList = false
            },
            onDismiss = { showChapterList = false },
            theme = theme
        )
    }
}

private fun formatSleepTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%d:%02d".format(minutes, remainingSeconds)
}

private fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

@Composable
private fun Modifier.clickableWithoutRipple(onClick: () -> Unit): Modifier {
    return this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}