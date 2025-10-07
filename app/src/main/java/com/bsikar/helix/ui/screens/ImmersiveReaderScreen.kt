package com.bsikar.helix.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsikar.helix.data.model.Book
import com.bsikar.helix.data.model.BookType
import com.bsikar.helix.theme.AppTheme
import com.bsikar.helix.ui.reader.ContentElement
import com.bsikar.helix.ui.reader.ReaderContent
import com.bsikar.helix.viewmodels.AudioBookReaderViewModel
import com.bsikar.helix.viewmodels.ReaderViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImmersiveReaderScreen(
    book: Book,
    pairedAudiobook: Book?,
    availableAudiobooks: List<Book>,
    onBack: () -> Unit,
    onPairAudiobook: (Book?) -> Unit,
    onUpdateReadingPosition: (String, Int, Int, Int) -> Unit,
    onAudiobookProgress: (String, Long, Float) -> Unit,
    theme: AppTheme,
    readerViewModel: ReaderViewModel = hiltViewModel(),
    audioViewModel: AudioBookReaderViewModel = hiltViewModel()
) {
    val readerState = rememberReaderState(readerViewModel, book)
    val playbackState by audioViewModel.playbackState.collectAsStateWithLifecycle()
    val lazyListState = rememberLazyListState()

    var showAudioPicker by remember { mutableStateOf(false) }
    var speedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(pairedAudiobook?.id) {
        pairedAudiobook?.let { target ->
            if (playbackState.currentBook?.id != target.id) {
                audioViewModel.loadBook(target)
            }
        }
    }

    LaunchedEffect(lazyListState.firstVisibleItemScrollOffset, readerState.currentChapterIndex) {
        if (book.bookType == BookType.EPUB) {
            readerViewModel.updateScrollPosition(
                lazyListState.firstVisibleItemIndex,
                lazyListState.firstVisibleItemScrollOffset
            )
            onUpdateReadingPosition(
                book.id,
                readerState.currentChapterIndex + 1,
                readerState.currentChapterIndex + 1,
                readerViewModel.getCurrentScrollPosition()
            )
        }
    }

    LaunchedEffect(playbackState.currentPositionMs / 5000) {
        val active = playbackState.currentBook ?: pairedAudiobook
        if (active != null) {
            onAudiobookProgress(active.id, playbackState.currentPositionMs, playbackState.playbackSpeed)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            readerViewModel.endReadingSession()
            val active = playbackState.currentBook ?: pairedAudiobook
            if (active != null) {
                onAudiobookProgress(active.id, playbackState.currentPositionMs, playbackState.playbackSpeed)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(book.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            book.author,
                            style = MaterialTheme.typography.bodySmall,
                            color = theme.secondaryTextColor
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (book.bookType == BookType.EPUB) {
                        TextButton(onClick = { showAudioPicker = true }) {
                            Text(if (pairedAudiobook == null) "Add audio" else "Switch audio")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = theme.surfaceColor,
                    titleContentColor = theme.primaryTextColor
                )
            )
        },
        containerColor = theme.backgroundColor,
        bottomBar = {
            ImmersiveAudioPanel(
                playbackState = playbackState,
                pairedAudiobook = pairedAudiobook,
                onPlayPause = { audioViewModel.togglePlayPause() },
                onSeek = { fraction ->
                    val duration = playbackState.durationMs.takeIf { it > 0 } ?: pairedAudiobook?.durationMs ?: 0L
                    if (duration > 0) {
                        audioViewModel.seekTo((duration * fraction).toLong())
                    }
                },
                onSkipBack = { audioViewModel.skipBackward() },
                onSkipForward = { audioViewModel.skipForward() },
                onSpeedClick = { speedDialog = true },
                onChooseAudio = { showAudioPicker = true },
                modifier = Modifier.navigationBarsPadding()
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp)
        ) {
            item {
                ReaderBody(
                    book = book,
                    readerState = readerState,
                    theme = theme,
                    readerViewModel = readerViewModel
                )
            }
        }
    }

    if (showAudioPicker) {
        AudioPickerDialog(
            current = pairedAudiobook,
            books = availableAudiobooks,
            onDismiss = { showAudioPicker = false },
            onSelect = {
                onPairAudiobook(it)
                showAudioPicker = false
            }
        )
    }

    if (speedDialog) {
        PlaybackSpeedDialog(
            currentSpeed = playbackState.playbackSpeed,
            onDismiss = { speedDialog = false },
            onSpeedSelected = { audioViewModel.setPlaybackSpeed(it) }
        )
    }
}

@Composable
private fun ReaderBody(
    book: Book,
    readerState: ReaderUiState,
    theme: AppTheme,
    readerViewModel: ReaderViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        when {
            book.bookType == BookType.AUDIOBOOK -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = theme.surfaceColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "This is an audiobook",
                            style = MaterialTheme.typography.titleMedium,
                            color = theme.primaryTextColor
                        )
                        Text(
                            text = "Pair it with an EPUB to follow along while you listen.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = theme.secondaryTextColor
                        )
                    }
                }
            }
            readerState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            readerState.error != null -> {
                Text(
                    text = readerState.error,
                    color = theme.errorColor
                )
            }
            readerState.content.isNotEmpty() -> {
                    ReaderContent(
                        elements = readerState.content,
                        onImageFailed = { _ -> },
                        modifier = Modifier.fillMaxWidth()
                    )
            }
            else -> {
                Text(
                    text = "No content available for this chapter.",
                    color = theme.secondaryTextColor
                )
            }
        }

        if (readerState.chapters.isNotEmpty()) {
            Text(
                text = "Chapters",
                style = MaterialTheme.typography.titleMedium,
                color = theme.primaryTextColor
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                readerState.chapters.forEachIndexed { index, chapter ->
                    val isActive = index == readerState.currentChapterIndex
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isActive) theme.accentColor.copy(alpha = 0.08f) else Color.Transparent,
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = chapter.title,
                                color = theme.primaryTextColor,
                                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                            )
                            AnimatedVisibility(visible = isActive) {
                                Text(
                                    text = "Currently reading",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = theme.secondaryTextColor
                                )
                            }
                        }
                        if (!isActive) {
                            TextButton(onClick = { readerViewModel.navigateToChapter(chapter) }) {
                                Text("Open")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImmersiveAudioPanel(
    playbackState: com.bsikar.helix.player.PlaybackState,
    pairedAudiobook: Book?,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onSkipBack: () -> Unit,
    onSkipForward: () -> Unit,
    onSpeedClick: () -> Unit,
    onChooseAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (pairedAudiobook == null) {
                Text(
                    text = "No audiobook paired",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Add an audiobook to listen while you read.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onChooseAudio) {
                    Text("Choose audiobook")
                }
            } else {
                Text(
                    text = pairedAudiobook.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = pairedAudiobook.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val duration = playbackState.durationMs.takeIf { it > 0 } ?: pairedAudiobook.durationMs
                val position = playbackState.currentPositionMs
                val progress = if (duration > 0) position / duration.toFloat() else 0f

                Slider(
                    value = progress.coerceIn(0f, 1f),
                    onValueChange = onSeek,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatDuration(position), style = MaterialTheme.typography.bodySmall)
                    Text(formatDuration(duration), style = MaterialTheme.typography.bodySmall)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onSkipBack) { Text("-30s") }
                    IconButton(onClick = onPlayPause) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null
                        )
                    }
                    TextButton(onClick = onSkipForward) { Text("+30s") }
                    IconButton(onClick = onSpeedClick) {
                        Icon(Icons.Outlined.Speed, contentDescription = "Speed")
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioPickerDialog(
    current: Book?,
    books: List<Book>,
    onDismiss: () -> Unit,
    onSelect: (Book?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose an audiobook") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                books.forEach { book ->
                    OutlinedButton(onClick = { onSelect(book) }, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(book.title, fontWeight = FontWeight.SemiBold)
                            Text(book.author, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (books.isEmpty()) {
                    Text("No audiobooks available in your library yet.")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(null) }) {
                Text(if (current != null) "Remove pairing" else "Close")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PlaybackSpeedDialog(
    currentSpeed: Float,
    onDismiss: () -> Unit,
    onSpeedSelected: (Float) -> Unit
) {
    val speeds = listOf(0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Playback speed") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                speeds.forEach { speed ->
                    OutlinedButton(onClick = {
                        onSpeedSelected(speed)
                        onDismiss()
                    }) {
                        Text(text = "${speed}x", fontWeight = if (speed == currentSpeed) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

private data class ReaderUiState(
    val isLoading: Boolean,
    val error: String?,
    val content: List<ContentElement>,
    val chapters: List<com.bsikar.helix.data.model.EpubChapter>,
    val currentChapterIndex: Int
)

@Composable
private fun rememberReaderState(
    readerViewModel: ReaderViewModel,
    book: Book
): ReaderUiState {
    LaunchedEffect(book.id) {
        if (book.bookType == BookType.EPUB) {
            readerViewModel.loadBook(book)
        }
    }

    val isLoading by readerViewModel.isLoadingContent.collectAsState()
    val isLoadingChapter by readerViewModel.isLoadingChapter.collectAsState()
    val error by readerViewModel.loadingError.collectAsState()
    val content by readerViewModel.currentContent.collectAsState()
    val chapters by readerViewModel.chapters.collectAsState()
    val currentChapterIndex by readerViewModel.currentChapterIndex.collectAsState()

    return remember(isLoading, isLoadingChapter, error, content, chapters, currentChapterIndex) {
        ReaderUiState(
            isLoading = isLoading || isLoadingChapter,
            error = error,
            content = content,
            chapters = chapters,
            currentChapterIndex = currentChapterIndex
        )
    }
}

private fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0) return "0:00"
    val totalSeconds = (durationMs / 1000f).roundToInt()
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
