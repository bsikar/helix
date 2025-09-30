package com.bsikar.helix.ui.screens

import androidx.annotation.StringRes
import com.bsikar.helix.R

enum class MainTab(@StringRes val labelRes: Int) {
    Home(R.string.home),
    Search(R.string.search),
    Library(R.string.library)
}
