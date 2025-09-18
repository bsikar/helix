package com.bsikar.helix

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

@Suppress("TooManyFunctions")
internal class RichHtmlParser {

    private companion object {
        const val PREVIEW_CHAR_LIMIT = 500
        const val SHORT_PREVIEW_LIMIT = 50
    }

    fun parseToRichContent(htmlContent: String): List<ContentElement> {
        val elements = mutableListOf<ContentElement>()

        val cleanHtml = preprocessHtmlContent(htmlContent)

        // Check for image tags before parsing
        val imageMatches = Regex("<img[^>]*>").findAll(cleanHtml)

        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(StringReader("<root>$cleanHtml</root>"))

            parseElements(parser, elements)
        } catch (@Suppress("TooGenericExceptionCaught") ignored: Exception) {
            // XML parsing failed - use fallback regex parsing (no logging to avoid spam)

            // Fallback: Extract images using regex even if XML parsing fails
            extractImagesWithRegex(cleanHtml, elements)

            val fallbackText = extractPlainText(cleanHtml)
            if (fallbackText.isNotBlank()) {
                elements.add(ContentElement.TextParagraph(fallbackText))
            }
        }

        // Additional safety check: if XML parsing succeeded but no images were found,
        // also try regex extraction as a backup
        val imageCount = elements.count { it is ContentElement.Image }
        val regexImageCount = imageMatches.count()

        if (imageCount == 0 && regexImageCount > 0) {
            extractImagesWithRegex(cleanHtml, elements)
        }

        return elements.ifEmpty {
            listOf(ContentElement.TextParagraph("No content found"))
        }
    }

    private fun parseElements(parser: XmlPullParser, elements: MutableList<ContentElement>) {
        var eventType = parser.eventType
        val textBuilder = StringBuilder()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name.lowercase()) {
                        "img" -> {
                            flushText(textBuilder, elements)
                            parseImage(parser, elements)
                        }
                        "h1", "h2", "h3", "h4", "h5", "h6" -> {
                            flushText(textBuilder, elements)
                            parseHeading(parser, elements)
                        }
                        "blockquote" -> {
                            flushText(textBuilder, elements)
                            parseQuote(parser, elements)
                        }
                        "ul", "ol" -> {
                            flushText(textBuilder, elements)
                            parseList(parser, elements, parser.name == "ol")
                        }
                        "p", "div" -> {
                            flushText(textBuilder, elements)
                        }
                        "br" -> {
                            textBuilder.append("\n")
                        }
                        "hr" -> {
                            flushText(textBuilder, elements)
                            elements.add(ContentElement.Divider)
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim()
                    if (!text.isNullOrBlank()) {
                        textBuilder.append(text).append(" ")
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name.lowercase()) {
                        "p", "div" -> {
                            flushText(textBuilder, elements)
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        flushText(textBuilder, elements)
    }

    private fun flushText(textBuilder: StringBuilder, elements: MutableList<ContentElement>) {
        val text = textBuilder.toString().trim()
        if (text.isNotBlank()) {
            elements.add(ContentElement.TextParagraph(decodeHtmlEntities(text)))
            textBuilder.clear()
        }
    }

    private fun parseImage(parser: XmlPullParser, elements: MutableList<ContentElement>) {
        val src = parser.getAttributeValue(null, "src") ?: ""
        val alt = parser.getAttributeValue(null, "alt") ?: ""

        if (src.isNotBlank()) {
            elements.add(ContentElement.Image(src = src, alt = alt))
        }
    }

    private fun parseHeading(parser: XmlPullParser, elements: MutableList<ContentElement>) {
        val level = parser.name.removePrefix("h").toIntOrNull() ?: 1
        val text = extractTextContent(parser)

        if (text.isNotBlank()) {
            elements.add(ContentElement.Heading(text = text, level = level))
        }
    }

    private fun parseQuote(parser: XmlPullParser, elements: MutableList<ContentElement>) {
        val text = extractTextContent(parser)

        if (text.isNotBlank()) {
            elements.add(ContentElement.Quote(text = text))
        }
    }

    private fun parseList(parser: XmlPullParser, elements: MutableList<ContentElement>, isOrdered: Boolean) {
        val items = mutableListOf<String>()
        val tagName = parser.name
        var eventType = parser.next()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "li") {
                val itemText = extractTextContent(parser)
                if (itemText.isNotBlank()) {
                    items.add(itemText)
                }
            } else if (eventType == XmlPullParser.END_TAG && parser.name == tagName) {
                break
            }
            eventType = parser.next()
        }

        if (items.isNotEmpty()) {
            elements.add(ContentElement.List(items = items, isOrdered = isOrdered))
        }
    }

    private fun extractTextContent(parser: XmlPullParser): String {
        val tagName = parser.name
        val textBuilder = StringBuilder()
        var eventType = parser.next()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.TEXT -> {
                    textBuilder.append(parser.text)
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == tagName) {
                        break
                    }
                }
            }
            eventType = parser.next()
        }

        return decodeHtmlEntities(textBuilder.toString().trim())
    }

    private fun preprocessHtmlContent(htmlContent: String): String {
        return htmlContent
            // Remove XML processing instructions that cause parser errors
            .replace(Regex("<\\?xml[^>]*\\?>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<\\?[^>]*\\?>"), "") // Remove any other processing instructions
            // Remove DOCTYPE declarations
            .replace(Regex("<!DOCTYPE[^>]*>", RegexOption.IGNORE_CASE), "")
            // Remove XML namespace declarations that can cause issues
            .replace(Regex("xmlns[^=]*=\"[^\"]*\"", RegexOption.IGNORE_CASE), "")
            // Remove script and style tags
            .replace(Regex("<script[^>]*>.*?</script>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<style[^>]*>.*?</style>", RegexOption.DOT_MATCHES_ALL), "")
            // Remove comments
            .replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")
            // Fix self-closing tags to be XML-compliant
            .replace(
                Regex("<(img|br|hr|meta|input|area|base|col|embed|source|track|wbr)([^>]*?)>", RegexOption.IGNORE_CASE)
            ) { matchResult ->
                val tagName = matchResult.groupValues[1].lowercase()
                val attributes = matchResult.groupValues[2]
                "<$tagName$attributes />"
            }
            // Ensure proper tag closing for common HTML tags that might be unclosed
            .replace(Regex("<p([^>]*)>([^<]*?)(?=<p|$)", RegexOption.IGNORE_CASE)) { matchResult ->
                val attributes = matchResult.groupValues[1]
                val content = matchResult.groupValues[2]
                "<p$attributes>$content</p>"
            }
    }

    private fun extractPlainText(htmlContent: String): String {
        return htmlContent
            .replace(Regex("<[^>]+>"), " ")
            .replace(Regex("\\s+"), " ")
            .let { decodeHtmlEntities(it) }
            .trim()
    }

    private fun extractImagesWithRegex(htmlContent: String, elements: MutableList<ContentElement>) {
        val imageRegex = Regex("""<img\s+([^>]*?)>""", RegexOption.IGNORE_CASE)
        val srcRegex = Regex("""src\s*=\s*["']([^"']*?)["']""", RegexOption.IGNORE_CASE)
        val altRegex = Regex("""alt\s*=\s*["']([^"']*?)["']""", RegexOption.IGNORE_CASE)

        val matches = imageRegex.findAll(htmlContent)
        matches.forEach { match ->
            val imgTag = match.value

            val srcMatch = srcRegex.find(imgTag)
            val altMatch = altRegex.find(imgTag)

            val src = srcMatch?.groupValues?.get(1) ?: ""
            val alt = altMatch?.groupValues?.get(1) ?: ""

            if (src.isNotBlank()) {
                elements.add(ContentElement.Image(src = src, alt = alt))
            }
        }
    }

    private fun decodeHtmlEntities(text: String): String {
        return text
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&#8217;", "'")
            .replace(
                "&#8220;",
                """)
            .replace("&#8221;", """
            )
            .replace("&#8230;", "…")
            .replace("&#160;", " ")
    }
}
