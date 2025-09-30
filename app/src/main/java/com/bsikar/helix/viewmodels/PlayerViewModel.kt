package com.bsikar.helix.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlayerViewModel : ViewModel() {

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Stopped)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    fun play(trackDetails: String) {
        _playbackState.value = PlaybackState.Playing(trackDetails)
    }

    fun pause() {
        val currentState = _playbackState.value
        if (currentState is PlaybackState.Playing) {
            _playbackState.value = PlaybackState.Paused(currentState.trackDetails)
        }
    }

    fun resume() {
        val currentState = _playbackState.value
        if (currentState is PlaybackState.Paused) {
            _playbackState.value = PlaybackState.Playing(currentState.trackDetails)
        }
    }

    fun stop() {
        _playbackState.value = PlaybackState.Stopped
    }

    sealed class PlaybackState {
        object Stopped : PlaybackState()
        data class Playing(val trackDetails: String) : PlaybackState()
        data class Paused(val trackDetails: String) : PlaybackState()
    }
}
