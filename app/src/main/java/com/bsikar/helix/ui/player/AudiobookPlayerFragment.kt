package com.bsikar.helix.ui.player

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bsikar.helix.R
import com.bsikar.helix.viewmodels.PlayerViewModel
import com.bsikar.helix.viewmodels.PlayerViewModel.PlaybackState
import kotlinx.coroutines.launch

class AudiobookPlayerFragment : Fragment(R.layout.fragment_audiobook_player) {

    private val playerViewModel: PlayerViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val statusText: TextView = view.findViewById(R.id.player_status)
        val pauseButton: Button = view.findViewById(R.id.pause_button)
        val resumeButton: Button = view.findViewById(R.id.resume_button)

        pauseButton.setOnClickListener { playerViewModel.pause() }
        resumeButton.setOnClickListener { playerViewModel.resume() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                playerViewModel.playbackState.collect { state ->
                    when (state) {
                        is PlaybackState.Playing -> {
                            statusText.text = getString(R.string.playing_format, state.trackDetails)
                            pauseButton.isEnabled = true
                            resumeButton.isEnabled = false
                        }
                        is PlaybackState.Paused -> {
                            statusText.text = getString(R.string.paused_format, state.trackDetails)
                            pauseButton.isEnabled = false
                            resumeButton.isEnabled = true
                        }
                        PlaybackState.Stopped -> {
                            statusText.text = getString(R.string.no_audiobook_playing)
                            pauseButton.isEnabled = false
                            resumeButton.isEnabled = false
                        }
                    }
                }
            }
        }
    }
}
