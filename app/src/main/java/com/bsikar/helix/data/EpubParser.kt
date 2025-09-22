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

class EpubParser(private val context: Context) {
    
    suspend fun parseEpub(file: File): Result<ParsedEpub> = withContext(Dispatchers.IO) {
        try {
            val zipFile = ZipFile(file)
            
            // Read container.xml to find OPF file location
            val containerXml = readFileFromZip(zipFile, "META-INF/container.xml")
                ?: return@withContext Result.failure(Exception("Container.xml not found"))
            
            val opfPath = extractOpfPath(containerXml)
                ?: return@withContext Result.failure(Exception("OPF path not found in container.xml"))
            
            // Read OPF file
            val opfContent = readFileFromZip(zipFile, opfPath)
                ?: return@withContext Result.failure(Exception("OPF file not found: $opfPath"))
            
            val opfDoc = Jsoup.parse(opfContent)
            
            // Extract metadata
            val metadata = extractMetadata(opfDoc)
            
            // Extract spine (reading order)
            val spine = extractSpine(opfDoc)
            
            // Extract manifest (file list)
            val manifest = extractManifest(opfDoc)
            
            // Read chapter contents
            val chapters = extractChapters(zipFile, spine, manifest, opfPath)
            
            // Extract table of contents if available
            val toc = extractTableOfContents(zipFile, manifest, opfPath)
            
            // Find cover image
            val coverImagePath = findCoverImage(manifest, opfDoc)
            
            val parsedEpub = ParsedEpub(
                metadata = metadata,
                chapters = chapters,
                tableOfContents = toc,
                coverImagePath = coverImagePath,
                filePath = file.absolutePath,
                fileSize = file.length(),
                lastModified = file.lastModified()
            )
            
            Result.success(parsedEpub)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun readFileFromZip(zipFile: ZipFile, fileName: String): String? {
        return try {
            zipFile.getInputStream(zipFile.getFileHeader(fileName))?.use { inputStream ->
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
        val basePath = File(opfPath).parent ?: ""
        val chapters = mutableListOf<EpubChapter>()
        
        spine.forEachIndexed { index, itemId ->
            val manifestItem = manifest[itemId]
            if (manifestItem != null) {
                val fullPath = if (basePath.isNotEmpty()) "$basePath/${manifestItem.href}" else manifestItem.href
                val content = readFileFromZip(zipFile, fullPath) ?: ""
                val doc = Jsoup.parse(content)
                val title = doc.select("title").text().ifEmpty { "Chapter ${index + 1}" }
                
                chapters.add(EpubChapter(
                    id = itemId,
                    title = title,
                    href = manifestItem.href,
                    content = content,
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
    
    data class ManifestItem(
        val id: String,
        val href: String,
        val mediaType: String
    )
}