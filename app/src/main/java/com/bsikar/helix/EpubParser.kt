package com.bsikar.helix

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URLDecoder
import java.util.zip.ZipFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("TooManyFunctions")
class EpubParser {
    private val metadataParser = EpubMetadataParser()
    private val spineParser = EpubSpineParser()
    private val textExtractor = HtmlTextExtractor()
    private val richParser = RichHtmlParser()

    private companion object {
        const val CONTENT_PREVIEW_LIMIT = 1000
    }

    fun parse(file: File): String {
        return try {
            parseEpubFile(file)
        } catch (e: IOException) {
            "Error reading EPUB file: ${e.message}\n\nPlease ensure this is a valid EPUB file."
        } catch (e: IllegalArgumentException) {
            "Invalid EPUB format: ${e.message}\n\nThis file may be corrupted or not a valid EPUB."
        } catch (e: SecurityException) {
            "Security error reading EPUB file: ${e.message}\n\nFile access may be restricted."
        } catch (e: OutOfMemoryError) {
            "EPUB file is too large to process: ${e.message}\n\nTry a smaller file."
        } catch (e: XmlPullParserException) {
            "Error parsing EPUB content: ${e.message}\n\nThe EPUB structure may be invalid."
        }
    }

    fun parseStructured(file: File): EpubContent {
        return try {
            parseEpubFileStructured(file)
        } catch (e: IOException) {
            EpubContent(
                metadata = "Error reading EPUB file: ${e.message}",
                chapters = listOf(EpubChapter(paragraphs = listOf("Failed to load EPUB content")))
            )
        } catch (e: IllegalArgumentException) {
            EpubContent(
                metadata = "Invalid EPUB format: ${e.message}",
                chapters = listOf(EpubChapter(paragraphs = listOf("Failed to load EPUB content")))
            )
        } catch (e: SecurityException) {
            EpubContent(
                metadata = "Security error reading EPUB file: ${e.message}",
                chapters = listOf(EpubChapter(paragraphs = listOf("Failed to load EPUB content")))
            )
        } catch (e: OutOfMemoryError) {
            EpubContent(
                metadata = "EPUB file is too large to process: ${e.message}",
                chapters = listOf(EpubChapter(paragraphs = listOf("Failed to load EPUB content")))
            )
        } catch (e: XmlPullParserException) {
            EpubContent(
                metadata = "Error parsing EPUB content: ${e.message}",
                chapters = listOf(EpubChapter(paragraphs = listOf("Failed to load EPUB content")))
            )
        }
    }

    suspend fun parseRichContent(file: File): RichEpubContent = withContext(Dispatchers.IO) {
        try {
            parseEpubFileRich(file)
        } catch (e: IOException) {
            RichEpubContent(
                metadata = "Error reading EPUB file: ${e.message}",
                chapters = listOf(
                    RichEpubChapter(
                    title = "Error",
                    elements = listOf(ContentElement.TextParagraph("Failed to load EPUB content"))
                )
                )
            )
        } catch (e: IllegalArgumentException) {
            RichEpubContent(
                metadata = "Invalid EPUB format: ${e.message}",
                chapters = listOf(
                    RichEpubChapter(
                    title = "Error",
                    elements = listOf(ContentElement.TextParagraph("Failed to load EPUB content"))
                )
                )
            )
        } catch (e: SecurityException) {
            RichEpubContent(
                metadata = "Security error reading EPUB file: ${e.message}",
                chapters = listOf(
                    RichEpubChapter(
                    title = "Error",
                    elements = listOf(ContentElement.TextParagraph("Failed to load EPUB content"))
                )
                )
            )
        } catch (e: OutOfMemoryError) {
            RichEpubContent(
                metadata = "EPUB file is too large to process: ${e.message}",
                chapters = listOf(
                    RichEpubChapter(
                    title = "Error",
                    elements = listOf(ContentElement.TextParagraph("Failed to load EPUB content"))
                )
                )
            )
        } catch (e: XmlPullParserException) {
            RichEpubContent(
                metadata = "Error parsing EPUB content: ${e.message}",
                chapters = listOf(
                    RichEpubChapter(
                    title = "Error",
                    elements = listOf(ContentElement.TextParagraph("Failed to load EPUB content"))
                )
                )
            )
        }
    }

    private fun parseEpubFile(file: File): String {
        ZipFile(file).use { zipFile ->
            val containerInputStream = getInputStream(zipFile, "META-INF/container.xml")
            val contentFilePath = parseContainerFile(containerInputStream)
            return buildEpubContent(zipFile, contentFilePath)
        }
    }

    private fun parseEpubFileStructured(file: File): EpubContent {
        ZipFile(file).use { zipFile ->
            val containerInputStream = getInputStream(zipFile, "META-INF/container.xml")
            val contentFilePath = parseContainerFile(containerInputStream)
            return buildStructuredEpubContent(zipFile, contentFilePath)
        }
    }

    private fun parseEpubFileRich(file: File): RichEpubContent {
        ZipFile(file).use { zipFile ->
            val containerInputStream = getInputStream(zipFile, "META-INF/container.xml")
            val contentFilePath = parseContainerFile(containerInputStream)
            return buildRichEpubContent(zipFile, contentFilePath)
        }
    }

    private fun buildEpubContent(zipFile: ZipFile, contentFilePath: String): String {
        val result = EpubContentBuilder()

        val errorMessage = validateEpubStructure(zipFile, contentFilePath)
        if (errorMessage != null) {
            return result.buildErrorMessage(errorMessage)
        }

        val metadata = extractMetadata(zipFile, contentFilePath)
        if (metadata.isNotEmpty()) {
            result.appendMetadata(metadata)
        }

        val spine = parseSpine(zipFile, contentFilePath)
        val content = extractAllChapters(zipFile, contentFilePath, spine)
        result.appendContent(content)

        return result.build()
    }

    private fun buildStructuredEpubContent(zipFile: ZipFile, contentFilePath: String): EpubContent {
        val errorMessage = validateEpubStructure(zipFile, contentFilePath)
        if (errorMessage != null) {
            return EpubContent(
                metadata = errorMessage,
                chapters = listOf(EpubChapter(paragraphs = listOf("No readable content found")))
            )
        }

        val metadata = extractMetadata(zipFile, contentFilePath)
        val spine = parseSpine(zipFile, contentFilePath)
        val chapters = extractAllChaptersStructured(zipFile, contentFilePath, spine)

        return EpubContent(
            metadata = metadata,
            chapters = chapters
        )
    }

    private fun buildRichEpubContent(zipFile: ZipFile, contentFilePath: String): RichEpubContent {
        val errorMessage = validateEpubStructure(zipFile, contentFilePath)
        if (errorMessage != null) {
            return RichEpubContent(
                metadata = errorMessage,
                chapters = listOf(
                    RichEpubChapter(
                    title = "Error",
                    elements = listOf(ContentElement.TextParagraph("No readable content found"))
                )
                )
            )
        }

        val metadata = extractMetadata(zipFile, contentFilePath)
        val spine = parseSpine(zipFile, contentFilePath)
        val chapters = extractAllChaptersRich(zipFile, contentFilePath, spine)

        return RichEpubContent(
            metadata = metadata,
            chapters = chapters
        )
    }

    private fun validateEpubStructure(zipFile: ZipFile, contentFilePath: String): String? {
        return when {
            contentFilePath.isEmpty() -> "Could not find content.opf file in EPUB"
            parseSpine(zipFile, contentFilePath).isEmpty() -> "No readable chapters found in EPUB"
            else -> null
        }
    }

    private fun extractMetadata(zipFile: ZipFile, contentFilePath: String): String {
        val contentInputStream = getInputStream(zipFile, contentFilePath)
        return metadataParser.extractMetadata(contentInputStream)
    }

    private fun parseSpine(zipFile: ZipFile, contentFilePath: String): List<String> {
        val contentInputStream = getInputStream(zipFile, contentFilePath)
        return spineParser.parseSpine(contentInputStream)
    }

    private fun extractAllChapters(zipFile: ZipFile, contentFilePath: String, spine: List<String>): String {
        val result = StringBuilder()
        val baseDir = contentFilePath.substringBeforeLast('/')

        for (spineItem in spine) {
            val chapterPath = if (baseDir.isNotEmpty()) "$baseDir/$spineItem" else spineItem
            val content = extractChapterContent(zipFile, chapterPath)
            if (content.isNotBlank()) {
                result.append(content.trim()).append("\n\n")
            }
        }

        return result.toString()
    }

    private fun extractAllChaptersStructured(
        zipFile: ZipFile,
        contentFilePath: String,
        spine: List<String>
    ): List<EpubChapter> {
        val chapters = mutableListOf<EpubChapter>()
        val baseDir = contentFilePath.substringBeforeLast('/')

        for ((index, spineItem) in spine.withIndex()) {
            val chapterPath = if (baseDir.isNotEmpty()) "$baseDir/$spineItem" else spineItem
            val paragraphs = extractChapterParagraphs(zipFile, chapterPath)

            if (paragraphs.isNotEmpty()) {
                val title = "Chapter ${index + 1}"
                chapters.add(EpubChapter(title = title, paragraphs = paragraphs))
            }
        }

        return chapters.ifEmpty {
            listOf(EpubChapter(title = "Content", paragraphs = listOf("No readable content found in this EPUB.")))
        }
    }

    private fun extractAllChaptersRich(
        zipFile: ZipFile,
        contentFilePath: String,
        spine: List<String>
    ): List<RichEpubChapter> {
        val chapters = mutableListOf<RichEpubChapter>()
        val baseDir = contentFilePath.substringBeforeLast('/')

        for ((index, spineItem) in spine.withIndex()) {
            val chapterPath = if (baseDir.isNotEmpty()) "$baseDir/$spineItem" else spineItem

            val elements = extractChapterRichElements(zipFile, chapterPath, baseDir)

            if (elements.isNotEmpty()) {
                val title = "Chapter ${index + 1}"
                chapters.add(RichEpubChapter(title = title, elements = elements))
            }
        }

        return chapters.ifEmpty {
            listOf(
                RichEpubChapter(
                title = "Content",
                elements = listOf(ContentElement.TextParagraph("No readable content found in this EPUB."))
            )
            )
        }
    }

    private fun parseContainerFile(inputStream: InputStream?): String {
        if (inputStream == null) return ""

        val parser = Xml.newPullParser()
        parser.setInput(inputStream, null)
        var eventType = parser.eventType

        var result = ""
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "rootfile") {
                result = parser.getAttributeValue(null, "full-path") ?: ""
                break
            }
            eventType = parser.next()
        }

        return result
    }

    private fun extractChapterContent(zipFile: ZipFile, chapterPath: String): String {
        val chapterInputStream = getInputStream(zipFile, chapterPath) ?: return ""

        val content = chapterInputStream.bufferedReader().use { it.readText() }
        return textExtractor.extractTextFromHtml(content)
    }

    private fun extractChapterParagraphs(zipFile: ZipFile, chapterPath: String): List<String> {
        val chapterInputStream = getInputStream(zipFile, chapterPath) ?: return emptyList()

        val content = chapterInputStream.bufferedReader().use { it.readText() }
        return textExtractor.extractParagraphsFromHtml(content)
    }

    private fun extractChapterRichElements(
        zipFile: ZipFile,
        chapterPath: String,
        baseDir: String
    ): List<ContentElement> {
        val chapterInputStream = getInputStream(zipFile, chapterPath)
        if (chapterInputStream == null) {
            return emptyList()
        }

        val content = chapterInputStream.bufferedReader().use { it.readText() }

        val elements = richParser.parseToRichContent(content)

        val processedElements = elements.map { element ->
            when (element) {
                is ContentElement.Image -> {
                    val resolvedSrc = resolveImagePath(element.src, chapterPath, baseDir)
                    element.copy(src = resolvedSrc)
                }
                else -> element
            }
        }

        return processedElements
    }

    private fun resolveImagePath(imageSrc: String, chapterPath: String, baseDir: String): String {
        val resolved = when {
            imageSrc.startsWith("http") -> {
                imageSrc
            }
            imageSrc.startsWith("/") -> {
                imageSrc.removePrefix("/")
            }
            imageSrc.startsWith("../") -> {
                // Handle relative paths that go up directories
                val chapterDir = chapterPath.substringBeforeLast('/')
                val parentDir = chapterDir.substringBeforeLast('/')
                if (parentDir.isNotEmpty()) {
                    "$parentDir/${imageSrc.removePrefix("../")}"
                } else if (baseDir.isNotEmpty()) {
                    "$baseDir/${imageSrc.removePrefix("../")}"
                } else {
                    imageSrc.removePrefix("../")
                }
            }
            else -> {
                val chapterDir = chapterPath.substringBeforeLast('/')

                val result = if (chapterDir.isNotEmpty()) {
                    "$chapterDir/$imageSrc"
                } else if (baseDir.isNotEmpty()) {
                    "$baseDir/$imageSrc"
                } else {
                    // Try common EPUB image directories
                    val commonImagePaths = listOf(
                        "images/$imageSrc",
                        "Images/$imageSrc",
                        "OEBPS/images/$imageSrc",
                        "OEBPS/Images/$imageSrc",
                        "content/images/$imageSrc",
                        "text/images/$imageSrc",
                        imageSrc
                    )
                    commonImagePaths.first() // Return the first one for now, we'll try others in extractImageData
                }
                result
            }
        }

        return resolved
    }

    private fun getInputStream(zipFile: ZipFile, path: String): InputStream? {
        val entry = zipFile.getEntry(path)
        return entry?.let { zipFile.getInputStream(it) }
    }

    @Suppress(
        "TooGenericExceptionCaught",
        "NestedBlockDepth",
        "LongMethod",
        "CyclomaticComplexMethod",
        "ReturnCount",
        "MagicNumber"
    )
    fun extractImageData(file: File, imagePath: String): ByteArray? {
        return try {
            // Try to decode URL encoding in image path
            val decodedPath = try {
                URLDecoder.decode(imagePath, "UTF-8")
            } catch (e: Exception) {
                imagePath
            }

            ZipFile(file).use { zipFile ->

                // First try the exact path (both original and decoded)
                var imageEntry = zipFile.getEntry(imagePath) ?: zipFile.getEntry(decodedPath)

                if (imageEntry != null) {
                    return imageEntry.let { entry ->
                        zipFile.getInputStream(entry).use { inputStream ->
                            val bytes = inputStream.readBytes()
                            bytes
                        }
                    }
                }

                // Try common image path variations
                val imageFileName = imagePath.substringAfterLast('/')
                val decodedImageFileName = decodedPath.substringAfterLast('/')
                val commonImagePaths = listOf(
                    "images/$imageFileName",
                    "Images/$imageFileName",
                    "OEBPS/images/$imageFileName",
                    "OEBPS/Images/$imageFileName",
                    "content/images/$imageFileName",
                    "text/images/$imageFileName",
                    imageFileName,
                    "images/$decodedImageFileName",
                    "Images/$decodedImageFileName",
                    "OEBPS/images/$decodedImageFileName",
                    "OEBPS/Images/$decodedImageFileName",
                    "content/images/$decodedImageFileName",
                    "text/images/$decodedImageFileName",
                    decodedImageFileName,
                    imagePath, // Original path as fallback
                    decodedPath // Decoded path as fallback
                )

                for (tryPath in commonImagePaths) {
                    imageEntry = zipFile.getEntry(tryPath)
                    if (imageEntry != null) {
                        return imageEntry.let { entry ->
                            zipFile.getInputStream(entry).use { inputStream ->
                                val bytes = inputStream.readBytes()
                                bytes
                            }
                        }
                    }
                }

                // If still not found, try partial matching
                val allEntries = zipFile.entries().asSequence().map { it.name }.toList()

                // Try to find images with partial matching
                val targetImageFileName = imagePath.substringAfterLast('/')

                // Look for entries that end with the same filename
                val matchingByName = allEntries.filter { entry ->
                    entry.substringAfterLast('/').equals(targetImageFileName, ignoreCase = true)
                }

                if (matchingByName.isNotEmpty()) {
                    val matchedEntry = zipFile.getEntry(matchingByName.first())
                    return matchedEntry?.let { entry ->
                        zipFile.getInputStream(entry).use { inputStream ->
                            val bytes = inputStream.readBytes()
                            bytes
                        }
                    }
                }

                null
            }
        } catch (@Suppress("TooGenericExceptionCaught") ignored: Exception) {
            null
        }
    }
}

private class EpubContentBuilder {
    private val result = StringBuilder()

    fun appendMetadata(metadata: String) {
        result.append(metadata).append("\n\n")
    }

    fun appendContent(content: String) {
        result.append(content)
    }

    fun buildErrorMessage(message: String): String = message

    fun build(): String {
        return result.toString().trim().ifEmpty {
            "EPUB file appears to contain no readable text content."
        }
    }
}
