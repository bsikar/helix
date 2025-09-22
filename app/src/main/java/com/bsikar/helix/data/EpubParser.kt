package com.bsikar.helix.data

import android.content.Context
import net.lingala.zip4j.ZipFile
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

class EpubParser(private val context: Context) {
    
    /**
     * Metadata parsing from InputStream - copies full file for reliable parsing
     */
    suspend fun parseEpubMetadataFromStream(inputStream: InputStream, fileSize: Long, fileName: String = "Unknown"): Result<ParsedEpub> = withContext(Dispatchers.IO) {
        try {
            Log.d("EpubParser", "Starting stream parsing for '$fileName' (file size: $fileSize bytes)")
            
            // Always copy the entire file to ensure container.xml and metadata are accessible
            Log.d("EpubParser", "Copying entire file '$fileName' for reliable metadata parsing")
            val tempFile = File.createTempFile("epub_full_", ".epub")
            tempFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            Log.d("EpubParser", "Successfully copied ${tempFile.length()} bytes for '$fileName'")
            
            try {
                Log.d("EpubParser", "Parsing metadata from temp file for '$fileName': ${tempFile.absolutePath}")
                val result = parseEpubMetadataOnly(tempFile)
                // Enhance the result with the correct file size
                if (result.isSuccess) {
                    val parsedEpub = result.getOrThrow()
                    Log.d("EpubParser", "Successfully parsed metadata for '$fileName': ${parsedEpub.metadata.title} by ${parsedEpub.metadata.author}")
                    val correctedEpub = parsedEpub.copy(
                        fileSize = fileSize,
                        filePath = "stream_source" // Will be updated by caller
                    )
                    Result.success(correctedEpub)
                } else {
                    val error = result.exceptionOrNull()
                    Log.e("EpubParser", "Failed to parse metadata for '$fileName': ${error?.message}", error)
                    result
                }
            } finally {
                tempFile.delete() // Always clean up
                Log.d("EpubParser", "Cleaned up temp file")
            }
        } catch (e: Exception) {
            Log.e("EpubParser", "Stream parsing failed: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Fast metadata-only parsing for import - extracts minimal data needed for library
     */
    suspend fun parseEpubMetadataOnly(file: File): Result<ParsedEpub> = withContext(Dispatchers.IO) {
        try {
            val zipFile = ZipFile(file)
            
            // Read container.xml to find OPF file location
            Log.d("EpubParser", "Reading container.xml...")
            val containerXml = readFileFromZip(zipFile, "META-INF/container.xml")
            if (containerXml == null) {
                Log.e("EpubParser", "Container.xml not found, listing available files:")
                try {
                    zipFile.fileHeaders?.take(20)?.forEach { header ->
                        Log.d("EpubParser", "  - ${header.fileName}")
                    }
                } catch (e: Exception) {
                    Log.e("EpubParser", "Could not list ZIP contents: ${e.message}")
                }
                return@withContext Result.failure(Exception("Container.xml not found"))
            }
            
            Log.d("EpubParser", "Container.xml content: ${containerXml.take(200)}...")
            val opfPath = extractOpfPath(containerXml)
            if (opfPath == null) {
                Log.e("EpubParser", "OPF path not found in container.xml")
                return@withContext Result.failure(Exception("OPF path not found in container.xml"))
            }
            
            Log.d("EpubParser", "Looking for OPF file at: $opfPath")
            
            // Read OPF file
            var opfContent = readFileFromZip(zipFile, opfPath)
            var actualOpfPath = opfPath
            
            if (opfContent == null) {
                Log.w("EpubParser", "OPF file not found at: $opfPath, attempting fallback for non-standard EPUB")
                
                // Fallback: Look for any .opf file in common locations
                val fallbackPaths = listOf(
                    "content.opf",
                    "package.opf", 
                    "OEBPS/package.opf",
                    "OPS/content.opf",
                    "OPS/package.opf"
                )
                
                for (fallbackPath in fallbackPaths) {
                    opfContent = readFileFromZip(zipFile, fallbackPath)
                    if (opfContent != null) {
                        Log.i("EpubParser", "Found OPF file at fallback location: $fallbackPath")
                        actualOpfPath = fallbackPath
                        break
                    }
                }
                
                // If still no OPF file found, try to generate one from the file structure
                if (opfContent == null) {
                    Log.w("EpubParser", "No OPF file found, attempting to generate metadata from file structure")
                    return@withContext generateMetadataFromFileStructure(zipFile)
                }
            }
            
            val opfDoc = Jsoup.parse(opfContent)
            
            // Extract metadata only
            val metadata = extractMetadata(opfDoc)
            
            // Extract spine for chapter count only
            val spine = extractSpine(opfDoc)
            
            // Skip everything else during import - create minimal structure
            val parsedEpub = ParsedEpub(
                metadata = metadata,
                chapters = emptyList(), // Load on-demand
                tableOfContents = emptyList(), // Load on-demand  
                coverImagePath = null, // Load on-demand
                filePath = file.absolutePath,
                fileSize = file.length(),
                lastModified = file.lastModified(),
                images = emptyMap(), // Load on-demand
                opfPath = actualOpfPath ?: "OEBPS/content.opf",
                totalChapters = maxOf(1, spine.size) // Ensure at least 1 chapter
            )
            
            Result.success(parsedEpub)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun parseEpub(file: File): Result<ParsedEpub> = withContext(Dispatchers.IO) {
        try {
            val zipFile = ZipFile(file)
            
            // Read container.xml to find OPF file location
            val containerXml = readFileFromZip(zipFile, "META-INF/container.xml")
                ?: return@withContext Result.failure(Exception("Container.xml not found"))
            
            val opfPath = extractOpfPath(containerXml)
                ?: return@withContext Result.failure(Exception("OPF path not found in container.xml"))
            
            // Read OPF file
            var opfContent = readFileFromZip(zipFile, opfPath)
            var actualOpfPath = opfPath
            
            if (opfContent == null) {
                Log.w("EpubParser", "OPF file not found at: $opfPath, attempting fallback for non-standard EPUB")
                
                // Fallback: Look for any .opf file in common locations
                val fallbackPaths = listOf(
                    "content.opf",
                    "package.opf", 
                    "OEBPS/package.opf",
                    "OPS/content.opf",
                    "OPS/package.opf"
                )
                
                for (fallbackPath in fallbackPaths) {
                    opfContent = readFileFromZip(zipFile, fallbackPath)
                    if (opfContent != null) {
                        Log.i("EpubParser", "Found OPF file at fallback location: $fallbackPath")
                        actualOpfPath = fallbackPath
                        break
                    }
                }
                
                // If still no OPF file found, try to generate one from the file structure
                if (opfContent == null) {
                    Log.w("EpubParser", "No OPF file found, attempting to generate EPUB from file structure")
                    return@withContext generateFullEpubFromFileStructure(zipFile, file)
                }
            }
            
            val opfDoc = Jsoup.parse(opfContent)
            
            // Extract metadata
            val metadata = extractMetadata(opfDoc)
            
            // Extract spine (reading order)
            val spine = extractSpine(opfDoc)
            
            // Extract manifest (file list)
            val manifest = extractManifest(opfDoc)
            
            // Read chapter contents
            val chapters = extractChapters(zipFile, spine, manifest, actualOpfPath)
            
            // Skip table of contents during import for performance
            val toc = emptyList<EpubTocEntry>() // Load on-demand later if needed
            
            // Find cover image
            val coverImagePath = findCoverImage(manifest, opfDoc)
            
            // Extract images from EPUB
            val images = extractImages(zipFile, manifest, actualOpfPath, file.nameWithoutExtension)
            
            val parsedEpub = ParsedEpub(
                metadata = metadata,
                chapters = chapters,
                tableOfContents = toc,
                coverImagePath = coverImagePath,
                filePath = file.absolutePath,
                fileSize = file.length(),
                lastModified = file.lastModified(),
                images = images,
                opfPath = actualOpfPath
            )
            
            Result.success(parsedEpub)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun readFileFromZip(zipFile: ZipFile, fileName: String, maxSize: Int = 1024 * 1024): String? {
        return try {
            val fileHeader = zipFile.getFileHeader(fileName) ?: return null
            
            // Skip extremely large files during import to prevent memory issues
            if (fileHeader.uncompressedSize > maxSize) {
                return null
            }
            
            zipFile.getInputStream(fileHeader)?.use { inputStream ->
                inputStream.readBytes().toString(Charsets.UTF_8)
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun extractOpfPath(containerXml: String): String? {
        val doc = Jsoup.parse(containerXml)
        return doc.select("rootfile").first()?.attr("full-path")
    }
    
    private fun extractMetadata(opfDoc: Document): EpubMetadata {
        val metadata = opfDoc.select("metadata").first()
        
        return EpubMetadata(
            title = metadata?.select("dc|title, title")?.text() ?: "Unknown Title",
            author = metadata?.select("dc|creator, creator")?.text(),
            description = metadata?.select("dc|description, description")?.text(),
            publisher = metadata?.select("dc|publisher, publisher")?.text(),
            language = metadata?.select("dc|language, language")?.text(),
            isbn = metadata?.select("dc|identifier, identifier")?.find { 
                it.attr("opf:scheme").equals("ISBN", ignoreCase = true) 
            }?.text(),
            publishedDate = metadata?.select("dc|date, date")?.text(),
            rights = metadata?.select("dc|rights, rights")?.text(),
            subjects = metadata?.select("dc|subject, subject")?.map { it.text() } ?: emptyList()
        )
    }
    
    private fun extractSpine(opfDoc: Document): List<String> {
        return opfDoc.select("spine itemref").map { it.attr("idref") }
    }
    
    private fun extractManifest(opfDoc: Document): Map<String, ManifestItem> {
        val manifest = mutableMapOf<String, ManifestItem>()
        opfDoc.select("manifest item").forEach { item ->
            val id = item.attr("id")
            val href = item.attr("href")
            val mediaType = item.attr("media-type")
            manifest[id] = ManifestItem(id, href, mediaType)
        }
        return manifest
    }
    
    private fun extractChapters(
        zipFile: ZipFile, 
        spine: List<String>, 
        manifest: Map<String, ManifestItem>,
        opfPath: String
    ): List<EpubChapter> {
        val chapters = mutableListOf<EpubChapter>()
        
        spine.forEachIndexed { index, itemId ->
            val manifestItem = manifest[itemId]
            if (manifestItem != null) {
                // Skip title extraction during import for maximum performance
                // Titles will be extracted on-demand when needed
                chapters.add(EpubChapter(
                    id = itemId,
                    title = "Chapter ${index + 1}", // Default title
                    href = manifestItem.href,
                    content = "", // No content during import - loaded on demand
                    order = index + 1
                ))
            }
        }
        
        return chapters
    }
    
    private fun extractTableOfContents(
        zipFile: ZipFile,
        manifest: Map<String, ManifestItem>,
        opfPath: String
    ): List<EpubTocEntry> {
        // Look for NCX or nav.xhtml file
        val tocItem = manifest.values.find { 
            it.mediaType == "application/x-dtbncx+xml" || 
            it.href.contains("toc") || 
            it.href.contains("nav")
        }
        
        if (tocItem != null) {
            val basePath = File(opfPath).parent ?: ""
            val fullPath = if (basePath.isNotEmpty()) "$basePath/${tocItem.href}" else tocItem.href
            val tocContent = readFileFromZip(zipFile, fullPath)
            
            if (tocContent != null) {
                return parseTableOfContents(tocContent)
            }
        }
        
        return emptyList()
    }
    
    private fun parseTableOfContents(tocContent: String): List<EpubTocEntry> {
        val doc = Jsoup.parse(tocContent)
        val entries = mutableListOf<EpubTocEntry>()
        
        // Try NCX format first
        doc.select("navPoint").forEach { navPoint ->
            val title = navPoint.select("navLabel text").text()
            val href = navPoint.select("content").attr("src")
            entries.add(EpubTocEntry(title, href))
        }
        
        // If no NCX, try HTML nav format
        if (entries.isEmpty()) {
            doc.select("nav ol li a").forEach { link ->
                val title = link.text()
                val href = link.attr("href")
                entries.add(EpubTocEntry(title, href))
            }
        }
        
        return entries
    }
    
    private fun findCoverImage(manifest: Map<String, ManifestItem>, opfDoc: Document): String? {
        // Look for cover in metadata
        val coverItem = opfDoc.select("meta[name=cover]").attr("content")
        if (coverItem.isNotEmpty()) {
            return manifest[coverItem]?.href
        }
        
        // Look for cover in manifest
        val coverManifestItem = manifest.values.find { 
            it.id.contains("cover", ignoreCase = true) &&
            it.mediaType.startsWith("image/")
        }
        
        return coverManifestItem?.href
    }
    
    private fun extractImages(zipFile: ZipFile, manifest: Map<String, ManifestItem>, opfPath: String, bookName: String): Map<String, String> {
        val images = mutableMapOf<String, String>()
        val opfDir = File(opfPath).parent ?: ""
        
        // Find all image items in manifest
        val imageItems = manifest.values.filter { item ->
            item.mediaType.startsWith("image/")
        }
        
        // Instead of extracting, just map the zip paths for direct access
        imageItems.forEach { item ->
            try {
                // Calculate full path within ZIP
                val fullPath = if (opfDir.isNotEmpty() && !item.href.startsWith("/")) {
                    "$opfDir/${item.href}"
                } else {
                    item.href
                }
                
                // Verify the image exists in the zip
                val fileHeader = zipFile.getFileHeader(fullPath)
                if (fileHeader != null) {
                    // Map multiple possible path variations to the zip path
                    images[item.href] = fullPath
                    
                    // Also map without leading directories for relative path matching
                    val normalizedHref = item.href.removePrefix("./").removePrefix("../")
                    images[normalizedHref] = fullPath
                    
                    // Map just the filename for most basic matching
                    val fileName = File(item.href).name
                    images[fileName] = fullPath
                    
                    // Map with ../ prefix as commonly seen in HTML
                    if (!item.href.startsWith("../")) {
                        images["../${item.href}"] = fullPath
                    }
                    
                    // Handle common EPUB pattern: OEBPS/images/file.jpg -> images/file.jpg
                    if (item.href.contains("OEBPS/images/")) {
                        val pathAfterOEBPS = item.href.substring(item.href.indexOf("OEBPS/images/") + "OEBPS/".length)
                        images[pathAfterOEBPS] = fullPath
                        images["../$pathAfterOEBPS"] = fullPath
                    }
                    
                    // Handle other common patterns: any/path/images/file.jpg -> images/file.jpg
                    if (item.href.contains("/images/")) {
                        val pathAfterImages = "images/" + item.href.substring(item.href.lastIndexOf("/images/") + "/images/".length)
                        images[pathAfterImages] = fullPath
                        images["../$pathAfterImages"] = fullPath
                    }
                }
            } catch (e: Exception) {
                // Skip problematic images
            }
        }
        
        return images
    }
    
    /**
     * Read full file content from zip without size restrictions (for lazy loading)
     */
    private fun readFullFileFromZip(zipFile: ZipFile, fileName: String): String? {
        return try {
            zipFile.getInputStream(zipFile.getFileHeader(fileName))?.use { inputStream ->
                inputStream.readBytes().toString(Charsets.UTF_8)
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Extract image mapping on-demand for reading
     */
    suspend fun loadImageMapping(
        epubFilePath: String,
        opfPath: String = "OEBPS/content.opf"
    ): Result<Map<String, String>> = withContext(Dispatchers.IO) {
        try {
            val epubFile = File(epubFilePath)
            if (!epubFile.exists()) {
                return@withContext Result.failure(Exception("EPUB file not found"))
            }
            
            val zipFile = ZipFile(epubFile)
            
            // Read OPF file to get manifest
            val opfContent = readFileFromZip(zipFile, opfPath)
                ?: return@withContext Result.failure(Exception("OPF file not found: $opfPath"))
            
            val opfDoc = Jsoup.parse(opfContent)
            val manifest = extractManifest(opfDoc)
            
            // Extract image mapping
            val images = extractImages(zipFile, manifest, opfPath, epubFile.nameWithoutExtension)
            
            Result.success(images)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Parse full EPUB from URI with minimal copying - only copies what's needed for reading
     */
    suspend fun parseEpubFromUri(context: Context, uri: String): Result<ParsedEpub> = withContext(Dispatchers.IO) {
        try {
            Log.d("EpubParser", "parseEpubFromUri called with URI: $uri")
            val parsedUri = android.net.Uri.parse(uri)
            
            // Create a temp file for ZIP operations
            val tempFile = File(context.cacheDir, "temp_read_${System.currentTimeMillis()}.epub")
            Log.d("EpubParser", "Created temp file: ${tempFile.absolutePath}")
            
            try {
                context.contentResolver.openInputStream(parsedUri)?.use { inputStream ->
                    Log.d("EpubParser", "Successfully opened input stream for URI")
                    tempFile.outputStream().use { output ->
                        inputStream.copyTo(output)
                    }
                    Log.d("EpubParser", "Successfully copied ${tempFile.length()} bytes to temp file")
                } ?: run {
                    Log.e("EpubParser", "Failed to open input stream for URI: $uri")
                    return@withContext Result.failure(Exception("Failed to open input stream for URI"))
                }
                
                // Parse the EPUB normally now that we have a temp file
                Log.d("EpubParser", "Starting parseEpub on temp file")
                val result = parseEpub(tempFile)
                if (result.isSuccess) {
                    Log.d("EpubParser", "parseEpub successful")
                    val parsedEpub = result.getOrThrow()
                    // Keep the file path null for URI sources to avoid confusion
                    val updatedEpub = parsedEpub.copy(
                        filePath = null, // Don't store URI as file path - that's confusing
                        fileSize = tempFile.length()
                    )
                    Log.d("EpubParser", "Successfully parsed EPUB from URI: ${updatedEpub.metadata.title}")
                    Result.success(updatedEpub)
                } else {
                    val error = result.exceptionOrNull()
                    Log.e("EpubParser", "parseEpub failed: ${error?.message}", error)
                    result
                }
            } finally {
                // Clean up temp file
                if (tempFile.exists()) {
                    tempFile.delete()
                    Log.d("EpubParser", "Cleaned up temp file")
                }
            }
        } catch (e: Exception) {
            Log.e("EpubParser", "parseEpubFromUri failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Load chapter content on demand from file
     */
    suspend fun loadChapterContent(
        epubFilePath: String,
        chapterHref: String,
        opfPath: String = "OEBPS/content.opf"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val epubFile = File(epubFilePath)
            if (!epubFile.exists()) {
                return@withContext Result.failure(Exception("EPUB file not found"))
            }
            
            val zipFile = ZipFile(epubFile)
            val basePath = File(opfPath).parent ?: ""
            val fullPath = if (basePath.isNotEmpty()) "$basePath/$chapterHref" else chapterHref
            
            val content = readFullFileFromZip(zipFile, fullPath)
                ?: return@withContext Result.failure(Exception("Chapter not found: $chapterHref"))
            
            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Load chapter content on demand from URI
     */
    suspend fun loadChapterContentFromUri(
        context: Context,
        uri: String,
        chapterHref: String,
        opfPath: String = "OEBPS/content.opf"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val parsedUri = android.net.Uri.parse(uri)
            val tempFile = File(context.cacheDir, "temp_chapter_${System.currentTimeMillis()}.epub")
            
            try {
                context.contentResolver.openInputStream(parsedUri)?.use { inputStream ->
                    tempFile.outputStream().use { output ->
                        inputStream.copyTo(output)
                    }
                }
                
                val zipFile = ZipFile(tempFile)
                val basePath = File(opfPath).parent ?: ""
                val fullPath = if (basePath.isNotEmpty()) "$basePath/$chapterHref" else chapterHref
                
                val content = readFullFileFromZip(zipFile, fullPath)
                    ?: return@withContext Result.failure(Exception("Chapter not found: $chapterHref"))
                
                Result.success(content)
            } finally {
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Generate full EPUB from file structure for reading non-standard EPUBs
     */
    private suspend fun generateFullEpubFromFileStructure(zipFile: ZipFile, file: File): Result<ParsedEpub> = withContext(Dispatchers.IO) {
        try {
            Log.i("EpubParser", "Generating full EPUB from file structure for reading non-standard EPUB")
            
            // Get basic metadata first
            val metadataResult = generateMetadataFromFileStructure(zipFile)
            if (metadataResult.isFailure) {
                return@withContext metadataResult
            }
            
            val basicEpub = metadataResult.getOrThrow()
            
            // Generate chapters from XHTML files
            val chapters = mutableListOf<EpubChapter>()
            var chapterIndex = 1
            
            zipFile.fileHeaders?.forEach { header ->
                if (header.fileName.endsWith(".xhtml") || header.fileName.endsWith(".html")) {
                    val fileName = File(header.fileName).nameWithoutExtension
                    chapters.add(EpubChapter(
                        id = "chapter_$chapterIndex",
                        title = fileName.replace("_", " ").replace("page ", "").replace("Chapter ", "Ch. "),
                        href = header.fileName,
                        content = "", // Load on-demand
                        order = chapterIndex
                    ))
                    chapterIndex++
                }
            }
            
            // Sort chapters by file name for proper order
            chapters.sortBy { it.href }
            
            Log.i("EpubParser", "Generated ${chapters.size} chapters for non-standard EPUB")
            
            // Generate image mapping from file structure
            val images = mutableMapOf<String, String>()
            zipFile.fileHeaders?.forEach { header ->
                if (header.fileName.lowercase().matches(Regex(".*\\.(jpg|jpeg|png|gif|webp)$"))) {
                    val fileName = File(header.fileName).name
                    val pathWithoutDirs = header.fileName.substringAfterLast("/")
                    
                    // Map multiple possible references to the same image
                    images[fileName] = header.fileName // Direct filename
                    images[pathWithoutDirs] = header.fileName // Filename without directory path
                    images[header.fileName] = header.fileName // Full path
                    
                    // Common relative path patterns
                    images["../$fileName"] = header.fileName
                    images["images/$fileName"] = header.fileName
                    images["../images/$fileName"] = header.fileName
                }
            }
            
            Log.i("EpubParser", "Generated ${images.size / 4} image mappings for non-standard EPUB")
            
            // Create full ParsedEpub for reading
            val fullEpub = ParsedEpub(
                metadata = basicEpub.metadata,
                chapters = chapters,
                tableOfContents = emptyList(), // Could generate from chapters if needed
                coverImagePath = null, // Could scan for cover if needed
                filePath = file.absolutePath,
                fileSize = file.length(),
                lastModified = file.lastModified(),
                images = images,
                opfPath = "generated_full", // Mark as generated
                totalChapters = maxOf(1, chapters.size)
            )
            
            Log.i("EpubParser", "Successfully generated full EPUB structure for reading")
            Result.success(fullEpub)
            
        } catch (e: Exception) {
            Log.e("EpubParser", "Failed to generate full EPUB from file structure: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Generate metadata from file structure for non-standard EPUBs without OPF files
     */
    private suspend fun generateMetadataFromFileStructure(zipFile: ZipFile): Result<ParsedEpub> = withContext(Dispatchers.IO) {
        try {
            Log.i("EpubParser", "Generating metadata from file structure for non-standard EPUB")
            
            // Try to extract title from file names
            var detectedTitle = "Unknown Manga"
            val firstImageFile = zipFile.fileHeaders?.find { it.fileName.contains("images/img_") }
            if (firstImageFile != null) {
                // Extract title from pattern like "img_Chapter_1_-_Title_Name_1.jpg"
                val fileName = firstImageFile.fileName
                val parts = fileName.split("_")
                if (parts.size > 4) {
                    // Try to extract manga name from file pattern
                    val titleParts = parts.drop(4).takeWhile { !it.matches(Regex("\\d+\\.(jpg|jpeg|png)")) }
                    if (titleParts.isNotEmpty()) {
                        detectedTitle = titleParts.joinToString(" ").replace("-", " ").trim()
                        Log.i("EpubParser", "Detected title from file structure: $detectedTitle")
                    }
                }
            }
            
            val defaultAuthor = "MangaCombiner" // Common for these types of files
            
            // Count XHTML files to estimate chapters
            var chapterCount = 0
            zipFile.fileHeaders?.forEach { header ->
                if (header.fileName.endsWith(".xhtml") || header.fileName.endsWith(".html")) {
                    chapterCount++
                }
            }
            
            Log.i("EpubParser", "Found $chapterCount chapters by scanning file structure")
            
            // Create minimal metadata
            val metadata = EpubMetadata(
                title = detectedTitle,
                author = defaultAuthor,
                description = "Manga converted to EPUB format",
                publisher = "MangaCombiner",
                language = "en",
                isbn = null,
                publishedDate = null,
                rights = null,
                subjects = listOf("Manga", "Comics")
            )
            
            // Create minimal ParsedEpub
            val parsedEpub = ParsedEpub(
                metadata = metadata,
                chapters = emptyList(), // Load on-demand
                tableOfContents = emptyList(), // Load on-demand  
                coverImagePath = null, // Load on-demand
                filePath = null, // Will be set by caller
                fileSize = 0L, // Will be set by caller
                lastModified = System.currentTimeMillis(),
                images = emptyMap(), // Load on-demand
                opfPath = "generated", // Mark as generated
                totalChapters = maxOf(1, chapterCount)
            )
            
            Log.i("EpubParser", "Successfully generated metadata for non-standard EPUB")
            Result.success(parsedEpub)
            
        } catch (e: Exception) {
            Log.e("EpubParser", "Failed to generate metadata from file structure: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    data class ManifestItem(
        val id: String,
        val href: String,
        val mediaType: String
    )
}