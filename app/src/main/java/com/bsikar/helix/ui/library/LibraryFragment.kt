package com.bsikar.helix.ui.library

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bsikar.helix.R
import com.bsikar.helix.viewmodels.PlayerViewModel

class LibraryFragment : Fragment(R.layout.fragment_library) {

    private val playerViewModel: PlayerViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.open_audiobook_button).setOnClickListener {
            playerViewModel.play(getString(R.string.sample_audiobook_title))
            findNavController().navigate(R.id.action_libraryFragment_to_audiobookPlayerFragment)
        }

        view.findViewById<Button>(R.id.open_ebook_button).setOnClickListener {
            findNavController().navigate(R.id.action_libraryFragment_to_ebookReaderFragment)
        }

        view.findViewById<Button>(R.id.stop_playback_button).setOnClickListener {
            playerViewModel.stop()
        }
    }
}
