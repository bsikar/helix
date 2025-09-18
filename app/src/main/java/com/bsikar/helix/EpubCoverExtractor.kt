package com.bsikar.helix

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile

/**
 * Utility class for extracting cover images from EPUB files
 */
@Suppress("TooGenericExceptionCaught", "CyclomaticComplexMethod", "NestedBlockDepth", "MagicNumber")
class EpubCoverExtractor {

    /**
     * Extract cover image from EPUB file
     * @param epubFile The EPUB file to extract cover from
     * @return Bitmap of the cover image or null if not found
     */
    fun extractCover(epubFile: File): Bitmap? {
        return try {
            ZipFile(epubFile).use { zipFile ->
                // First, try to find cover image through OPF metadata
                val coverImagePath = findCoverImagePath(zipFile)
                if (coverImagePath != null) {
                    extractImageFromZip(zipFile, coverImagePath)
                } else {
                    // Fallback: look for common cover image names
                    findCoverByCommonNames(zipFile)
                }
            }
        } catch (@Suppress("TooGenericExceptionCaught") ignored: Exception) {
            null
        }
    }

    private fun findCoverImagePath(zipFile: ZipFile): String? {
        try {
            // Find the OPF file first
            val containerEntry = zipFile.getEntry("META-INF/container.xml")
            if (containerEntry != null) {
                val opfPath = parseContainerXml(zipFile.getInputStream(containerEntry))
                if (opfPath != null) {
                    val opfEntry = zipFile.getEntry(opfPath)
                    if (opfEntry != null) {
                        return parseCoverFromOpf(zipFile.getInputStream(opfEntry), opfPath)
                    }
                }
            }
        } catch (@Suppress("TooGenericExceptionCaught") ignored: Exception) {
        }
        return null
    }

    private fun parseContainerXml(inputStream: InputStream): String? {
        try {
            val parser = Xml.newPullParser()
            parser.setInput(inputStream, null)
            var eventType = parser.eventType

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "rootfile") {
                    val fullPath = parser.getAttributeValue(null, "full-path")
                    if (fullPath != null) {
                        return fullPath
                    }
                }
                eventType = parser.next()
            }
        } catch (@Suppress("TooGenericExceptionCaught") ignored: Exception) {
        }
        return null
    }

    private fun parseCoverFromOpf(inputStream: InputStream, opfPath: String): String? {
        try {
            val parser = Xml.newPullParser()
            parser.setInput(inputStream, null)
            var eventType = parser.eventType

            val items = mutableMapOf<String, String>() // id -> href
            var coverItemId: String? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "meta" -> {
                            // Look for cover metadata
                            val name = parser.getAttributeValue(null, "name")
                            val content = parser.getAttributeValue(null, "content")
                            if (name == "cover" && content != null) {
                                coverItemId = content
                            }
                        }
                        "item" -> {
                            // Store item mapping
                            val id = parser.getAttributeValue(null, "id")
                            val href = parser.getAttributeValue(null, "href")
                            val mediaType = parser.getAttributeValue(null, "media-type")

                            if (id != null && href != null) {
                                items[id] = href

                                // Also check if this is a cover by media type and id/href
                                if (mediaType?.startsWith("image/") == true) {
                                    val lowerHref = href.lowercase()
                                    val lowerId = id.lowercase()
                                    if (lowerHref.contains("cover") || lowerId.contains("cover")) {
                                        coverItemId = id
                                    }
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }

            // If we found a cover item ID, get its href
            if (coverItemId != null && items.containsKey(coverItemId)) {
                val coverHref = items[coverItemId]!!
                // Resolve relative path based on OPF location
                return resolveImagePath(opfPath, coverHref)
            }
        } catch (@Suppress("TooGenericExceptionCaught") ignored: Exception) {
        }
        return null
    }

    private fun resolveImagePath(opfPath: String, imageHref: String): String {
        val opfDir = if (opfPath.contains("/")) {
            opfPath.substring(0, opfPath.lastIndexOf("/") + 1)
        } else {
            ""
        }
        return opfDir + imageHref
    }

    private fun findCoverByCommonNames(zipFile: ZipFile): Bitmap? {
        val commonCoverNames = listOf(
            "cover.jpg", "cover.jpeg", "cover.png", "cover.gif",
            "Cover.jpg", "Cover.jpeg", "Cover.png", "Cover.gif",
            "COVER.jpg", "COVER.jpeg", "COVER.png", "COVER.gif",
            "Images/cover.jpg", "Images/cover.jpeg", "Images/cover.png",
            "images/cover.jpg", "images/cover.jpeg", "images/cover.png",
            "OEBPS/Images/cover.jpg", "OEBPS/Images/cover.png",
            "OEBPS/images/cover.jpg", "OEBPS/images/cover.png"
        )

        for (coverName in commonCoverNames) {
            val entry = zipFile.getEntry(coverName)
            if (entry != null) {
                val bitmap = extractImageFromZip(zipFile, coverName)
                if (bitmap != null) {
                    return bitmap
                }
            }
        }

        // Last resort: find any image in common image directories
        return findFirstImageInCommonDirs(zipFile)
    }

    private fun findFirstImageInCommonDirs(zipFile: ZipFile): Bitmap? {
        val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "webp")

        zipFile.entries().asSequence()
            .filter { !it.isDirectory }
            .filter { entry ->
                val extension = entry.name.substringAfterLast(".", "").lowercase()
                extension in imageExtensions
            }
            .sortedBy { entry ->
                // Prioritize images that might be covers
                val name = entry.name.lowercase()
                when {
                    name.contains("cover") -> 0
                    name.contains("title") -> 1
                    name.contains("front") -> 2
                    name.startsWith("images/") || name.startsWith("oebps/images/") -> 3
                    else -> 4
                }
            }
            .forEach { entry ->
                val bitmap = extractImageFromZip(zipFile, entry.name)
                if (bitmap != null) {
                    return bitmap
                }
            }

        return null
    }

    private fun extractImageFromZip(zipFile: ZipFile, imagePath: String): Bitmap? {
        return try {
            val entry = zipFile.getEntry(imagePath)
            if (entry != null) {
                zipFile.getInputStream(entry).use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            } else {
                null
            }
        } catch (@Suppress("TooGenericExceptionCaught") ignored: Exception) {
            null
        }
    }
}
