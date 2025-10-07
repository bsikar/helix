package com.bsikar.helix.ui.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bsikar.helix.data.ReaderSettings
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

/**
 * Represents elements that can be rendered in the modern reader experience.
 */
sealed class ContentElement {
    data class TextElement(val text: AnnotatedString) : ContentElement()
    data class ImageElement(
        val imagePath: String,
        val alt: String = "",
        val originalSrc: String = ""
    ) : ContentElement()
}

/**
 * High level composable that renders EPUB content emitted by [ReaderViewModel].
 */
@Composable
fun ReaderContent(
    elements: List<ContentElement>,
    modifier: Modifier = Modifier,
    onImageFailed: ((String) -> Unit)? = null
) {
    Column(modifier = modifier) {
        elements.forEach { element ->
            when (element) {
                is ContentElement.TextElement -> {
                    Text(
                        text = element.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .fillMaxWidth()
                    )
                }
                is ContentElement.ImageElement -> {
                    AsyncImage(
                        model = element.imagePath,
                        contentDescription = if (element.alt.isBlank()) null else element.alt,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentScale = ContentScale.Fit,
                        onError = {
                            onImageFailed?.invoke(element.originalSrc)
                        }
                    )
                }
            }
        }
    }
}

fun parseHtmlToContentElements(
    htmlContent: String,
    readerSettings: ReaderSettings,
    images: Map<String, String>
): List<ContentElement> {
    val doc = Jsoup.parse(htmlContent)
    val elements = mutableListOf<ContentElement>()

    try {
        doc.select("script, style").remove()
        parseElementToContentList(doc.body(), elements, readerSettings, images)
    } catch (e: Exception) {
        elements.add(
            ContentElement.TextElement(
                AnnotatedString("Error parsing chapter content: ${e.message ?: "Unknown error"}")
            )
        )
    }

    return if (elements.isEmpty()) {
        listOf(ContentElement.TextElement(AnnotatedString("No content available")))
    } else {
        elements
    }
}

private fun parseElementToContentList(
    element: Element?,
    elements: MutableList<ContentElement>,
    readerSettings: ReaderSettings,
    images: Map<String, String>
) {
    if (element == null) return

    var builder = AnnotatedString.Builder()
    var hasText = false

    element.childNodes().forEach { node ->
        when (node) {
            is TextNode -> {
                val text = node.text()
                if (text.isNotBlank()) {
                    builder.append(text)
                    hasText = true
                }
            }
            is Element -> {
                when (node.tagName().lowercase()) {
                    "img" -> {
                        if (hasText) {
                            elements.add(ContentElement.TextElement(builder.toAnnotatedString()))
                            builder = AnnotatedString.Builder()
                            hasText = false
                        }
                        val src = node.attr("src")
                        val alt = node.attr("alt")
                        val normalizedSrc = normalizeImagePath(src)
                        val imagePath = images[normalizedSrc] ?: images[src]
                        if (imagePath != null) {
                            elements.add(
                                ContentElement.ImageElement(
                                    imagePath = imagePath,
                                    alt = alt,
                                    originalSrc = src
                                )
                            )
                        }
                    }
                    else -> {
                        val nestedImages = node.select("img")
                        if (nestedImages.isNotEmpty()) {
                            if (hasText) {
                                elements.add(ContentElement.TextElement(builder.toAnnotatedString()))
                                builder = AnnotatedString.Builder()
                                hasText = false
                            }
                            parseElementToContentList(node, elements, readerSettings, images)
                        } else {
                            parseElementRecursive(node, builder, readerSettings, images)
                            hasText = true
                        }
                    }
                }
            }
        }
    }

    if (hasText) {
        elements.add(ContentElement.TextElement(builder.toAnnotatedString()))
    }
}

private fun parseElementRecursive(
    node: Element,
    builder: AnnotatedString.Builder,
    readerSettings: ReaderSettings,
    images: Map<String, String>
) {
    when (node.tagName().lowercase()) {
        "h1" -> {
            builder.append("\n\n")
            builder.pushStyle(
                SpanStyle(
                    fontSize = (readerSettings.fontSize * if (readerSettings.useSystemFontSize) 2f else 1.8f).sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = readerSettings.letterSpacing.sp
                )
            )
            parseTextContent(node, builder, readerSettings, images)
            builder.pop()
            builder.append("\n\n")
        }
        "h2" -> {
            builder.append("\n\n")
            builder.pushStyle(
                SpanStyle(
                    fontSize = (readerSettings.fontSize * if (readerSettings.useSystemFontSize) 1.8f else 1.6f).sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = readerSettings.letterSpacing.sp
                )
            )
            parseTextContent(node, builder, readerSettings, images)
            builder.pop()
            builder.append("\n\n")
        }
        "h3" -> {
            builder.append("\n\n")
            builder.pushStyle(
                SpanStyle(
                    fontSize = (readerSettings.fontSize * 1.4f).sp,
                    fontWeight = FontWeight.Bold
                )
            )
            parseTextContent(node, builder, readerSettings, images)
            builder.pop()
            builder.append("\n\n")
        }
        "h4", "h5", "h6" -> {
            builder.append("\n\n")
            builder.pushStyle(
                SpanStyle(
                    fontSize = (readerSettings.fontSize * 1.2f).sp,
                    fontWeight = FontWeight.Bold
                )
            )
            parseTextContent(node, builder, readerSettings, images)
            builder.pop()
            builder.append("\n\n")
        }
        "p" -> {
            parseTextContent(node, builder, readerSettings, images)
            builder.append("\n\n")
        }
        "br" -> builder.append("\n")
        "strong", "b" -> {
            builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            parseTextContent(node, builder, readerSettings, images)
            builder.pop()
        }
        "em", "i" -> {
            builder.pushStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
            parseTextContent(node, builder, readerSettings, images)
            builder.pop()
        }
        "ul" -> {
            node.children().forEach { child ->
                if (child.tagName().equals("li", ignoreCase = true)) {
                    builder.append("\n• ")
                    parseTextContent(child, builder, readerSettings, images)
                }
            }
            builder.append("\n")
        }
        "ol" -> {
            node.children().forEachIndexed { index, child ->
                if (child.tagName().equals("li", ignoreCase = true)) {
                    builder.append("\n${index + 1}. ")
                    parseTextContent(child, builder, readerSettings, images)
                }
            }
            builder.append("\n")
        }
        "blockquote" -> {
            builder.append("\n")
            builder.pushStyle(
                SpanStyle(
                    fontSize = (readerSettings.fontSize * 0.9f).sp,
                    color = Color(0x99000000)
                )
            )
            parseTextContent(node, builder, readerSettings, images)
            builder.pop()
            builder.append("\n")
        }
        else -> {
            parseTextContent(node, builder, readerSettings, images)
        }
    }
}

private fun parseTextContent(
    element: Element,
    builder: AnnotatedString.Builder,
    readerSettings: ReaderSettings,
    images: Map<String, String>
) {
    element.childNodes().forEach { child ->
        when (child) {
            is TextNode -> builder.append(child.text())
            is Element -> parseElementRecursive(child, builder, readerSettings, images)
        }
    }
}

private fun normalizeImagePath(path: String): String {
    return path.trim().removePrefix("./").removePrefix("../")
}
