package com.bsikar.helix.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay30
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.bsikar.helix.ui.components.SmoothProgressBar
import com.bsikar.helix.ui.components.ResponsiveSpacing
import com.bsikar.helix.viewmodels.AudioBookReaderViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs

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
    
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val swipeThresholdPx = remember(density) { density.run { 64.dp.toPx() } }
    var horizontalDragAmount by remember { mutableStateOf(0f) }
    
    LaunchedEffect(book) {
        viewModel.loadBook(book)
    }
    
    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
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
                    containerColor = theme.surfaceColor,
                    titleContentColor = theme.primaryTextColor
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Cover Art with enhanced design
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .padding(horizontal = ResponsiveSpacing.large() * 2)
                .pointerInput(book.id, playbackState.currentChapter?.id) {
                    detectHorizontalDragGestures(
                        onDragStart = { horizontalDragAmount = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            horizontalDragAmount += dragAmount
                        },
                        onDragEnd = {
                            if (abs(horizontalDragAmount) > swipeThresholdPx) {
                                if (horizontalDragAmount < 0f) {
                                    viewModel.nextChapter()
                                } else {
                                    viewModel.previousChapter()
                                }
                            }
                            horizontalDragAmount = 0f
                        },
                        onDragCancel = { horizontalDragAmount = 0f }
                    )
                }
                .pointerInput(playbackState.isPlaying) {
                    detectTapGestures(onDoubleTap = { viewModel.togglePlayPause() })
                },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = book.getEffectiveCoverColor().copy(alpha = 0.3f),
                        spotColor = book.getEffectiveCoverColor().copy(alpha = 0.3f)
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (book.shouldShowCoverArt() && !book.coverImagePath.isNullOrBlank()) {
                        Color.Transparent
                    } else {
                        book.getEffectiveCoverColor()
                    }
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (book.shouldShowCoverArt() && !book.coverImagePath.isNullOrBlank()) {
                        AsyncImage(
                            model = book.coverImagePath,
                            contentDescription = "Cover",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.AudioFile,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = Color.White.copy(alpha = 0.9f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "AUDIOBOOK",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.7f),
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Book Info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ResponsiveSpacing.large()),
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
            
            // Current Chapter with enhanced design
            playbackState.currentChapter?.let { chapter ->
                Card(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = theme.surfaceColor.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = chapter.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.primaryTextColor.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Smooth Progress Bar for Audiobooks
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            SmoothProgressBar(
                currentPosition = playbackState.currentPositionMs,
                duration = playbackState.durationMs,
                theme = theme,
                onSeek = { position -> viewModel.seekTo(position) }
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(playbackState.currentPositionMs),
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
        
        // Control Buttons with enhanced animations
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ResponsiveSpacing.large()),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val buttonElevation by animateDpAsState(
                targetValue = if (playbackState.isPlaying) 8.dp else 4.dp,
                animationSpec = tween(300),
                label = "button_elevation"
            )
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
            
            // Play/Pause with animation
            FloatingActionButton(
                onClick = { viewModel.togglePlayPause() },
                modifier = Modifier
                    .size(72.dp)
                    .shadow(
                        elevation = buttonElevation,
                        shape = CircleShape,
                        ambientColor = theme.accentColor.copy(alpha = 0.3f),
                        spotColor = theme.accentColor.copy(alpha = 0.3f)
                    ),
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
        
        // Secondary Controls with enhanced design
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ResponsiveSpacing.large()),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Playback Speed with better visual
            Card(
                modifier = Modifier
                    .clickable { showSpeedDialog = true },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = theme.surfaceColor.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = "Speed",
                        modifier = Modifier.size(20.dp),
                        tint = theme.primaryTextColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "${playbackState.playbackSpeed}x",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = theme.primaryTextColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Sleep timer indicator if active
            if (sleepTimerMinutes != null) {
                Card(
                    modifier = Modifier
                        .clickable { showSleepTimer = true },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = theme.accentColor.copy(alpha = theme.alphaSubtle)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Bedtime,
                            contentDescription = "Sleep Timer",
                            modifier = Modifier.size(20.dp),
                            tint = theme.accentColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            formatSleepTime(sleepTimerSecondsRemaining),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = theme.accentColor
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
    
    // Enhanced Speed Selection Dialog
    if (showSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            containerColor = theme.surfaceColor,
            titleContentColor = theme.primaryTextColor,
            textContentColor = theme.primaryTextColor,
            title = { 
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = theme.accentColor
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Playback Speed",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f).forEach { speed ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    viewModel.setPlaybackSpeed(speed)
                                    showSpeedDialog = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (playbackState.playbackSpeed == speed) 
                                    theme.accentColor.copy(alpha = theme.alphaSubtle) 
                                else 
                                    theme.backgroundColor
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${speed}x",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (playbackState.playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal,
                                    color = if (playbackState.playbackSpeed == speed) theme.accentColor else theme.primaryTextColor
                                )
                                if (playbackState.playbackSpeed == speed) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        modifier = Modifier.size(20.dp),
                                        tint = theme.accentColor
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showSpeedDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = theme.accentColor
                    )
                ) {
                    Text("Done")
                }
            }
        )
    }
    
    // Enhanced Sleep Timer Dialog
    if (showSleepTimer) {
        AlertDialog(
            onDismissRequest = { showSleepTimer = false },
            containerColor = theme.surfaceColor,
            titleContentColor = theme.primaryTextColor,
            textContentColor = theme.primaryTextColor,
            title = { 
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Bedtime,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = theme.accentColor
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        if (sleepTimerMinutes != null) 
                            "Sleep Timer Active"
                        else 
                            "Set Sleep Timer",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (sleepTimerMinutes != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = theme.accentColor.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Timer Active",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = theme.accentColor
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    formatSleepTime(sleepTimerSecondsRemaining),
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.primaryTextColor
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        viewModel.cancelSleepTimer()
                                        showSleepTimer = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = theme.accentColor
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Cancel Timer")
                                }
                            }
                        }
                    } else {
                        listOf(5, 10, 15, 30, 45, 60, 90).forEach { minutes ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        viewModel.setSleepTimer(minutes)
                                        showSleepTimer = false
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = theme.backgroundColor
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Timer,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = theme.secondaryTextColor
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "$minutes minutes",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = theme.primaryTextColor
                                        )
                                    }
                                    Text(
                                        text = if (minutes < 60) "$minutes min" else "${minutes/60}h ${minutes%60}m",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = theme.secondaryTextColor
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showSleepTimer = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = theme.accentColor
                    )
                ) {
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