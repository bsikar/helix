package com.bsikar.helix.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import java.io.File

/**
 * Component that displays images directly from EPUB zip files without extraction
 */
@Composable
fun EpubImageComponent(
    epubFilePath: String?,
    originalUri: String?,
    imageZipPath: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val context = LocalContext.current
    var imageRequest by remember(epubFilePath, originalUri, imageZipPath) { mutableStateOf<ImageRequest?>(null) }
    var hasError by remember(epubFilePath, originalUri, imageZipPath) { mutableStateOf(false) }

    LaunchedEffect(epubFilePath, originalUri, imageZipPath) {
        hasError = false
        try {
            val request = createImageRequest(context, epubFilePath, originalUri, imageZipPath)
            imageRequest = request
        } catch (e: Exception) {
            hasError = true
        }
    }

    Box(modifier = modifier) {
        when {
            hasError -> {
                Text(
                    text = "Image unavailable",
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
            }
            imageRequest != null -> {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = contentScale,
                    onError = { hasError = true }
                )
            }
            else -> {
                Text(
                    text = "Loading image...",
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
            }
        }
    }
}

/**
 * Create an ImageRequest that loads from EPUB zip file, supporting both file paths and URIs
 */
private suspend fun createImageRequest(
    context: android.content.Context,
    epubFilePath: String?,
    originalUri: String?,
    imageZipPath: String
): ImageRequest = withContext(Dispatchers.IO) {
    
    // Strategy 1: Try direct file path first
    epubFilePath?.let { filePath ->
        val epubFile = File(filePath)
        if (epubFile.exists()) {
            try {
                val zipFile = ZipFile(epubFile)
                val fileHeader = zipFile.getFileHeader(imageZipPath)
                if (fileHeader != null) {
                    val inputStream = zipFile.getInputStream(fileHeader)
                    return@withContext ImageRequest.Builder(context)
                        .data(inputStream)
                        .build()
                }
            } catch (e: Exception) {
                // Fall through to next strategy
            }
        }
    }
    
    // Strategy 2: Try original URI with optimized extraction
    originalUri?.let { uriString ->
        try {
            val uri = android.net.Uri.parse(uriString)
            
            // For URI-based EPUBs, extract just the image bytes directly to memory
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val tempFile = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.epub")
                try {
                    tempFile.outputStream().use { output ->
                        inputStream.copyTo(output)
                    }
                    
                    val zipFile = ZipFile(tempFile)
                    val fileHeader = zipFile.getFileHeader(imageZipPath)
                    if (fileHeader != null) {
                        // Extract image to byte array instead of using InputStream
                        val imageBytes = zipFile.getInputStream(fileHeader).use { it.readBytes() }
                        val request = ImageRequest.Builder(context)
                            .data(imageBytes)
                            .build()
                        tempFile.delete() // Clean up
                        return@withContext request
                    }
                } finally {
                    tempFile.delete() // Ensure cleanup
                }
            }
        } catch (e: Exception) {
            // Fall through to error
        }
    }
    
    throw IllegalArgumentException("Could not load image from EPUB: $imageZipPath")
}