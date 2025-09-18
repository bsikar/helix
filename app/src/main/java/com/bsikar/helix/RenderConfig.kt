package com.bsikar.helix

import java.io.File

data class RenderConfig(
    val fontSize: Float,
    val lineHeight: Float,
    val epubFile: File? = null,
    val onlyShowImages: Boolean = false
)
