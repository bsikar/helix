package com.bsikar.helix.utils

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.security.MessageDigest

object FileUtils {
    
    /**
     * Calculate SHA-256 checksum of a file from URI
     */
    suspend fun calculateChecksum(context: Context, uri: String): String? = withContext(Dispatchers.IO) {
        try {
            val parsedUri = Uri.parse(uri)
            context.contentResolver.openInputStream(parsedUri)?.use { inputStream ->
                calculateChecksumFromStream(inputStream)
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Calculate SHA-256 checksum from an InputStream
     */
    private fun calculateChecksumFromStream(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        var bytesRead: Int
        
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
        
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Calculate a fast checksum using file size and modification time
     * Faster than SHA-256 for large files, good enough for change detection
     */
    suspend fun calculateFastChecksum(context: Context, uri: String): String? = withContext(Dispatchers.IO) {
        try {
            val parsedUri = Uri.parse(uri)
            val documentFile = androidx.documentfile.provider.DocumentFile.fromSingleUri(context, parsedUri)
            documentFile?.let {
                val size = it.length()
                val lastModified = it.lastModified()
                "${size}_${lastModified}".hashCode().toString()
            }
        } catch (e: Exception) {
            null
        }
    }
}