package com.bsikar.helix.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsikar.helix.data.model.AudioChapter
import com.bsikar.helix.data.model.Book
import com.bsikar.helix.data.repository.AudioChapterRepository
import com.bsikar.helix.data.repository.BookRepository
import com.bsikar.helix.player.AudioBookPlayer
import com.bsikar.helix.player.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AudioBookReaderViewModel @Inject constructor(
    private val audioBookPlayer: AudioBookPlayer,
    private val bookRepository: BookRepository,
    private val audioChapterRepository: AudioChapterRepository
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = audioBookPlayer.playbackState

    private val _chapters = MutableStateFlow<List<AudioChapter>>(emptyList())
    val chapters: StateFlow<List<AudioChapter>> = _chapters.asStateFlow()

    private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
    val sleepTimerMinutes: StateFlow<Int?> = _sleepTimerMinutes.asStateFlow()

    private val _sleepTimerSecondsRemaining = MutableStateFlow<Int>(0)
    val sleepTimerSecondsRemaining: StateFlow<Int> = _sleepTimerSecondsRemaining.asStateFlow()

    init {
        audioBookPlayer.initialize()
        startPositionUpdater()
    }

    fun loadBook(book: Book) {
        viewModelScope.launch {
            // Load chapters from database
            val bookChapters = audioChapterRepository.getChaptersByBookIdSync(book.id)
            _chapters.value = bookChapters
            
            // Load book into player
            audioBookPlayer.loadAudiobook(book, bookChapters)
        }
    }

    fun play() {
        audioBookPlayer.play()
    }

    fun pause() {
        audioBookPlayer.pause()
    }

    fun togglePlayPause() {
        audioBookPlayer.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        audioBookPlayer.seekTo(positionMs)
        updateBookProgress()
    }

    fun setPlaybackSpeed(speed: Float) {
        audioBookPlayer.setPlaybackSpeed(speed)
        updateBookProgress()
    }

    fun skipForward(amountMs: Long = 30000L) {
        audioBookPlayer.skipForward(amountMs)
        updateBookProgress()
    }

    fun skipBackward(amountMs: Long = 30000L) {
        audioBookPlayer.skipBackward(amountMs)
        updateBookProgress()
    }

    fun jumpToChapter(chapter: AudioChapter) {
        audioBookPlayer.jumpToChapter(chapter)
        updateBookProgress()
    }

    fun previousChapter() {
        audioBookPlayer.previousChapter()
        updateBookProgress()
    }

    fun nextChapter() {
        audioBookPlayer.nextChapter()
        updateBookProgress()
    }

    fun setSleepTimer(minutes: Int?) {
        _sleepTimerMinutes.value = minutes
        
        if (minutes == null) {
            _sleepTimerSecondsRemaining.value = 0
            return
        }
        
        _sleepTimerSecondsRemaining.value = minutes * 60
        
        viewModelScope.launch {
            while (_sleepTimerSecondsRemaining.value > 0) {
                delay(1000)
                _sleepTimerSecondsRemaining.value--
                
                if (_sleepTimerSecondsRemaining.value == 0) {
                    pause()
                    _sleepTimerMinutes.value = null
                }
            }
        }
    }

    fun cancelSleepTimer() {
        setSleepTimer(null)
    }

    private fun startPositionUpdater() {
        viewModelScope.launch {
            while (true) {
                delay(500) // Update every 500ms
                audioBookPlayer.updatePosition()
                
                // Save progress every 5 seconds if playing
                if (playbackState.value.isPlaying && 
                    playbackState.value.currentPositionMs % 5000 < 500) {
                    updateBookProgress()
                }
            }
        }
    }

    private fun updateBookProgress() {
        viewModelScope.launch {
            playbackState.value.currentBook?.let { book ->
                bookRepository.updatePlaybackPosition(
                    bookId = book.id,
                    positionMs = audioBookPlayer.getCurrentPosition(),
                    playbackSpeed = audioBookPlayer.getCurrentSpeed()
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        updateBookProgress()
        audioBookPlayer.release()
    }
}