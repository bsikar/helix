package com.bsikar.helix.ui.reader

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bsikar.helix.MainActivity
import com.bsikar.helix.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class EbookReaderFragment : Fragment(R.layout.fragment_ebook_reader) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.reader_content).text =
            getString(R.string.sample_reader_content)

        (activity as? MainActivity)?.findViewById<BottomNavigationView>(R.id.bottom_nav_view)?.visibility =
            View.GONE
    }

    override fun onDestroyView() {
        (activity as? MainActivity)?.findViewById<BottomNavigationView>(R.id.bottom_nav_view)?.visibility =
            View.VISIBLE
        super.onDestroyView()
    }
}
