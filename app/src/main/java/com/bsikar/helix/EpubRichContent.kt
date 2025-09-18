package com.bsikar.helix

sealed class ContentElement {
    data class TextParagraph(val text: String) : ContentElement()
    data class Image(
        val src: String,
        val alt: String = "",
        val caption: String = ""
    ) : ContentElement()
    data class Heading(
        val text: String,
        val level: Int = 1
    ) : ContentElement()
    data class Quote(val text: String) : ContentElement()
    data class List(
        val items: kotlin.collections.List<String>,
        val isOrdered: Boolean = false
    ) : ContentElement()
    object Divider : ContentElement()
}

data class RichEpubContent(
    val metadata: String = "",
    val chapters: kotlin.collections.List<RichEpubChapter> = emptyList()
)

data class RichEpubChapter(
    val title: String = "",
    val elements: kotlin.collections.List<ContentElement> = emptyList()
)
