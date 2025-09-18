package com.bsikar.helix

import android.content.Context
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.text.DecimalFormat
import java.util.zip.ZipFile
import kotlin.math.log10
import kotlin.math.pow

/**
 * Extracts detailed metadata from EPUB files
 */
@Suppress("TooGenericExceptionCaught", "MagicNumber", "CyclomaticComplexMethod", "LongMethod", "NestedBlockDepth")
class DetailedEpubMetadataExtractor(private val context: Context) {

    private val coverCache = CoverImageCache.getInstance(context)

    /**
     * Extract comprehensive book information from EPUB file
     */
    suspend fun extractBookInfo(epubFile: File): BookInfo = withContext(Dispatchers.IO) {
        try {
            val metadata = extractMetadataFromEpub(epubFile)
            val cover = coverCache.getCover(epubFile)
            val fileSize = formatFileSize(epubFile.length())

            BookInfo(
                title = metadata["title"] ?: "",
                author = metadata["creator"],
                description = metadata["description"],
                publisher = metadata["publisher"],
                language = metadata["language"],
                fileSize = fileSize,
                fileName = epubFile.name,
                filePath = epubFile.absolutePath,
                cover = cover,
                chapterCount = countChapters(epubFile),
                wordCount = 0L // Can be expensive to calculate, leaving as 0 for now
            )
        } catch (@Suppress("TooGenericExceptionCaught") ignored: Exception) {
            // Return basic info even if metadata extraction fails
            BookInfo(
                title = "",
                author = null,
                description = null,
                publisher = null,
                language = null,
                fileSize = formatFileSize(epubFile.length()),
                fileName = epubFile.name,
                filePath = epubFile.absolutePath,
                cover = null
            )
        }
    }

    private fun extractMetadataFromEpub(epubFile: File): Map<String, String> {
        val metadata = mutableMapOf<String, String>()

        try {
            ZipFile(epubFile).use { zipFile ->
                // Find OPF file
                val containerEntry = zipFile.getEntry("META-INF/container.xml")
                val opfPath = if (containerEntry != null) {
                    parseContainerForOpfPath(zipFile.getInputStream(containerEntry))
                } else {
                    null
                }

                if (opfPath != null) {
                    val opfEntry = zipFile.getEntry(opfPath)
                    if (opfEntry != null) {
                        parseOpfMetadata(zipFile.getInputStream(opfEntry), metadata)
                    }
                }
            }
        } catch (@Suppress("TooGenericExceptionCaught") ignored: Exception) {
        }

        return metadata
    }

    private fun parseContainerForOpfPath(inputStream: java.io.InputStream): String? {
        try {
            val parser = Xml.newPullParser()
            parser.setInput(inputStream, null)
            var eventType = parser.eventType

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "rootfile") {
                    return parser.getAttributeValue(null, "full-path")
                }
                eventType = parser.next()
            }
        } catch (@Suppress("TooGenericExceptionCaught") ignored: Exception) {
        }
        return null
    }

    private fun parseOpfMetadata(inputStream: java.io.InputStream, metadata: MutableMap<String, String>) {
        try {
            val parser = Xml.newPullParser()
            parser.setInput(inputStream, null)
            var eventType = parser.eventType

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "dc:title" -> {
                            val title = getTextContent(parser)
                            if (title.isNotBlank()) {
                                metadata["title"] = title
                            }
                        }
                        "dc:creator" -> {
                            val creator = getTextContent(parser)
                            if (creator.isNotBlank()) {
                                metadata["creator"] = creator
                            }
                        }
                        "dc:description" -> {
                            val description = getTextContent(parser)
                            if (description.isNotBlank()) {
                                metadata["description"] = description
                            }
                        }
                        "dc:publisher" -> {
                            val publisher = getTextContent(parser)
                            if (publisher.isNotBlank()) {
                                metadata["publisher"] = publisher
                            }
                        }
                        "dc:language" -> {
                            val language = getTextContent(parser)
                            if (language.isNotBlank()) {
                                metadata["language"] = formatLanguage(language)
                            }
                        }
                        "dc:subject" -> {
                            val subject = getTextContent(parser)
                            if (subject.isNotBlank()) {
                                val existing = metadata["subject"]
                                metadata["subject"] = if (existing != null) {
                                    "$existing, $subject"
                                } else {
                                    subject
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (@Suppress("TooGenericExceptionCaught") ignored: Exception) {
        }
    }

    private fun getTextContent(parser: XmlPullParser): String {
        return try {
            parser.next()
            if (parser.eventType == XmlPullParser.TEXT) {
                parser.text.trim()
            } else {
                ""
            }
        } catch (@Suppress("TooGenericExceptionCaught") ignored: Exception) {
            ""
        }
    }

    private fun formatLanguage(language: String): String {
        return when (language.lowercase()) {
            "en", "en-us", "en-gb" -> "English"
            "es", "es-es" -> "Spanish"
            "fr", "fr-fr" -> "French"
            "de", "de-de" -> "German"
            "it", "it-it" -> "Italian"
            "pt", "pt-br" -> "Portuguese"
            "ru", "ru-ru" -> "Russian"
            "ja", "ja-jp" -> "Japanese"
            "ko", "ko-kr" -> "Korean"
            "zh", "zh-cn", "zh-tw" -> "Chinese"
            else -> language.uppercase()
        }
    }

    private fun countChapters(epubFile: File): Int {
        return try {
            ZipFile(epubFile).use { zipFile ->
                // Count HTML/XHTML files that are likely chapters
                zipFile.entries().asSequence()
                    .filter { !it.isDirectory }
                    .count { entry ->
                        val name = entry.name.lowercase()
                        (name.endsWith(".html") || name.endsWith(".xhtml")) &&
                        !name.contains("toc") && !name.contains("nav") &&
                        !name.contains("index") && !name.contains("cover")
                    }
            }
        } catch (@Suppress("TooGenericExceptionCaught") ignored: Exception) {
            0
        }
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"

        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
        val size = bytes / 1024.0.pow(digitGroups.toDouble())

        return DecimalFormat("#,##0.#").format(size) + " " + units[digitGroups]
    }
}
