package com.bsikar.helix

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object UriFileResolver {
    private const val CACHE_DIR_NAME = "epub_cache"

    fun resolveUriToFile(context: Context, uri: Uri): File? {
        return try {
            val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            val fileName = getFileNameFromUri(context, uri) ?: "temp_${System.currentTimeMillis()}.epub"
            val cacheFile = File(cacheDir, fileName)

            if (cacheFile.exists()) {
                return cacheFile
            }

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(cacheFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            if (cacheFile.exists() && cacheFile.length() > 0) {
                cacheFile
            } else {
                null
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: IOException) {
            e.printStackTrace()
            null
        } catch (@Suppress("TooGenericExceptionCaught") e: SecurityException) {
            e.printStackTrace()
            null
        }
    }

    @Suppress("NestedBlockDepth")
    private fun getFileNameFromUri(context: Context, uri: Uri): String? {
        return try {
            when (uri.scheme) {
                "content" -> {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (nameIndex >= 0) {
                                cursor.getString(nameIndex)
                            } else {
                                null
                            }
                        } else {
                            null
                        }
                    }
                }
                "file" -> {
                    File(uri.path ?: "").name
                }
                else -> null
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            null
        }
    }

    fun clearCache(context: Context) {
        try {
            val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCacheSize(context: Context): Long {
        return try {
            val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
            if (cacheDir.exists()) {
                cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
            } else {
                0L
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            0L
        }
    }
}
