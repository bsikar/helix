package com.bsikar.helix.viewmodels

import androidx.lifecycle.ViewModel
import com.bsikar.helix.player.AudioBookPlayer
import com.bsikar.helix.player.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val audioBookPlayer: AudioBookPlayer
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = audioBookPlayer.playbackState

    fun play() {
        audioBookPlayer.play()
    }

    fun pause() {
        audioBookPlayer.pause()
    }

    fun togglePlayPause() {
        audioBookPlayer.togglePlayPause()
    }
}
