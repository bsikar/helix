package com.bsikar.helix.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LibraryRepository private constructor(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("helix_library_prefs", Context.MODE_PRIVATE)

    private val _librarySources = MutableStateFlow(getStoredSources())
    val librarySources: StateFlow<Set<String>> = _librarySources.asStateFlow()

    private val _excludedBooks = MutableStateFlow(getStoredExcludedBooks())
    val excludedBooks: StateFlow<Set<String>> = _excludedBooks.asStateFlow()

    private fun getStoredSources(): Set<String> {
        return prefs.getStringSet(KEY_LIBRARY_SOURCES, emptySet()) ?: emptySet()
    }

    private fun getStoredExcludedBooks(): Set<String> {
        return prefs.getStringSet(KEY_EXCLUDED_BOOKS, emptySet()) ?: emptySet()
    }

    fun addLibrarySource(uri: Uri) {
        val uriString = uri.toString()
        val currentSources = _librarySources.value.toMutableSet()
        currentSources.add(uriString)
        _librarySources.value = currentSources
        prefs.edit {
            putStringSet(KEY_LIBRARY_SOURCES, currentSources)
        }
    }

    fun removeLibrarySource(uri: Uri) {
        val uriString = uri.toString()
        val currentSources = _librarySources.value.toMutableSet()
        currentSources.remove(uriString)
        _librarySources.value = currentSources
        prefs.edit {
            putStringSet(KEY_LIBRARY_SOURCES, currentSources)
        }
    }

    fun addExcludedBook(uri: Uri) {
        val uriString = uri.toString()
        val currentExcluded = _excludedBooks.value.toMutableSet()
        currentExcluded.add(uriString)
        _excludedBooks.value = currentExcluded
        prefs.edit {
            putStringSet(KEY_EXCLUDED_BOOKS, currentExcluded)
        }
    }

    fun removeExcludedBook(uri: Uri) {
        val uriString = uri.toString()
        val currentExcluded = _excludedBooks.value.toMutableSet()
        currentExcluded.remove(uriString)
        _excludedBooks.value = currentExcluded
        prefs.edit {
            putStringSet(KEY_EXCLUDED_BOOKS, currentExcluded)
        }
    }

    fun clearAllSources() {
        _librarySources.value = emptySet()
        prefs.edit {
            remove(KEY_LIBRARY_SOURCES)
        }
    }

    fun getLibrarySourceUris(): List<Uri> {
        return _librarySources.value.mapNotNull { uriString ->
            try {
                Uri.parse(uriString)
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                null
            }
        }
    }

    fun getExcludedBookUris(): List<Uri> {
        return _excludedBooks.value.mapNotNull { uriString ->
            try {
                Uri.parse(uriString)
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                null
            }
        }
    }

    fun isBookExcluded(uri: Uri): Boolean {
        return _excludedBooks.value.contains(uri.toString())
    }

    companion object {
        private const val KEY_LIBRARY_SOURCES = "library_sources"
        private const val KEY_EXCLUDED_BOOKS = "excluded_books"

        @Volatile
        private var INSTANCE: LibraryRepository? = null

        fun getInstance(context: Context): LibraryRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LibraryRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
