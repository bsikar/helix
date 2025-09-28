package com.bsikar.helix.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.bsikar.helix.data.model.AudioChapter
import com.bsikar.helix.data.model.Book
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Playback state for audiobooks
 */
data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isLoading: Boolean = false,
    val currentBook: Book? = null,
    val currentChapter: AudioChapter? = null,
    val error: String? = null
)

/**
 * AudioBook player that manages audio playback for audiobooks
 */
@Singleton
class AudioBookPlayer @Inject constructor(
    private val context: Context
) {
    private var exoPlayer: ExoPlayer? = null
    private var mediaController: MediaController? = null
    
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()
    
    private var currentBook: Book? = null
    private var currentChapters: List<AudioChapter> = emptyList()
    
    /**
     * Initialize the player
     */
    fun initialize() {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context).build()
            setupPlayerListener()
        }
    }
    
    /**
     * Load an audiobook for playback
     */
    fun loadAudiobook(book: Book, chapters: List<AudioChapter>) {
        currentBook = book
        currentChapters = chapters
        
        _playbackState.value = _playbackState.value.copy(
            isLoading = true,
            currentBook = book,
            error = null
        )
        
        try {
            val mediaItem = MediaItem.fromUri(book.filePath ?: "")
            exoPlayer?.setMediaItem(mediaItem)
            exoPlayer?.prepare()
            
            // Seek to the current position if resuming
            if (book.currentPositionMs > 0) {
                seekTo(book.currentPositionMs)
            }
            
            updateCurrentChapter()
            
        } catch (e: Exception) {
            _playbackState.value = _playbackState.value.copy(
                isLoading = false,
                error = "Failed to load audiobook: ${e.message}"
            )
        }
    }
    
    /**
     * Start or resume playback
     */
    fun play() {
        exoPlayer?.play()
    }
    
    /**
     * Pause playback
     */
    fun pause() {
        exoPlayer?.pause()
    }
    
    /**
     * Toggle play/pause
     */
    fun togglePlayPause() {
        if (_playbackState.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }
    
    /**
     * Seek to a specific position
     */
    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        updateCurrentChapter()
    }
    
    /**
     * Set playback speed
     */
    fun setPlaybackSpeed(speed: Float) {
        exoPlayer?.setPlaybackSpeed(speed)
        _playbackState.value = _playbackState.value.copy(playbackSpeed = speed)
    }
    
    /**
     * Skip forward by specified amount
     */
    fun skipForward(amountMs: Long = 30000L) {
        val currentPosition = exoPlayer?.currentPosition ?: 0L
        val newPosition = (currentPosition + amountMs).coerceAtMost(_playbackState.value.durationMs)
        seekTo(newPosition)
    }
    
    /**
     * Skip backward by specified amount
     */
    fun skipBackward(amountMs: Long = 30000L) {
        val currentPosition = exoPlayer?.currentPosition ?: 0L
        val newPosition = (currentPosition - amountMs).coerceAtLeast(0L)
        seekTo(newPosition)
    }
    
    /**
     * Jump to a specific chapter
     */
    fun jumpToChapter(chapter: AudioChapter) {
        seekTo(chapter.startTimeMs)
    }
    
    /**
     * Skip to previous chapter
     */
    fun previousChapter() {
        val currentChapter = getCurrentChapter()
        val currentIndex = currentChapters.indexOf(currentChapter)
        if (currentIndex > 0) {
            jumpToChapter(currentChapters[currentIndex - 1])
        }
    }
    
    /**
     * Skip to next chapter
     */
    fun nextChapter() {
        val currentChapter = getCurrentChapter()
        val currentIndex = currentChapters.indexOf(currentChapter)
        if (currentIndex < currentChapters.size - 1) {
            jumpToChapter(currentChapters[currentIndex + 1])
        }
    }
    
    /**
     * Get current chapter based on playback position
     */
    private fun getCurrentChapter(): AudioChapter? {
        val currentPosition = exoPlayer?.currentPosition ?: 0L
        return currentChapters.find { chapter ->
            chapter.containsPosition(currentPosition)
        }
    }
    
    /**
     * Update current chapter in state
     */
    private fun updateCurrentChapter() {
        val currentChapter = getCurrentChapter()
        _playbackState.value = _playbackState.value.copy(currentChapter = currentChapter)
    }
    
    /**
     * Set up ExoPlayer listener
     */
    private fun setupPlayerListener() {
        exoPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val isPlaying = playbackState == Player.STATE_READY && exoPlayer?.playWhenReady == true
                val isLoading = playbackState == Player.STATE_BUFFERING
                
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = isPlaying,
                    isLoading = isLoading
                )
            }
            
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                val isPlaying = playWhenReady && exoPlayer?.playbackState == Player.STATE_READY
                _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)
            }
            
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                updateCurrentChapter()
            }
        })
    }
    
    /**
     * Start position tracking (call this periodically to update position)
     */
    fun updatePosition() {
        val currentPosition = exoPlayer?.currentPosition ?: 0L
        val duration = exoPlayer?.duration ?: 0L
        
        _playbackState.value = _playbackState.value.copy(
            currentPositionMs = currentPosition,
            durationMs = if (duration > 0) duration else currentBook?.durationMs ?: 0L
        )
        
        updateCurrentChapter()
    }
    
    /**
     * Release the player
     */
    fun release() {
        exoPlayer?.release()
        exoPlayer = null
        mediaController?.release()
        mediaController = null
    }
    
    /**
     * Check if a book is currently loaded
     */
    fun isBookLoaded(bookId: String): Boolean {
        return currentBook?.id == bookId
    }
    
    /**
     * Get current playback position for saving progress
     */
    fun getCurrentPosition(): Long {
        return exoPlayer?.currentPosition ?: 0L
    }
    
    /**
     * Get current playback speed for saving
     */
    fun getCurrentSpeed(): Float {
        return _playbackState.value.playbackSpeed
    }
}