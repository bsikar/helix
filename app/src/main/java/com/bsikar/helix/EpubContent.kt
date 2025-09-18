package com.bsikar.helix

data class EpubContent(
    val metadata: String = "",
    val chapters: List<EpubChapter> = emptyList()
)

data class EpubChapter(
    val title: String = "",
    val paragraphs: List<String> = emptyList()
)
