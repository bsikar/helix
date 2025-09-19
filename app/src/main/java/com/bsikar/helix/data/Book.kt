package com.bsikar.helix.data

import androidx.compose.ui.graphics.Color

data class Book(
    val title: String,
    val author: String,
    val coverColor: Color,
    val progress: Float = 0f
)