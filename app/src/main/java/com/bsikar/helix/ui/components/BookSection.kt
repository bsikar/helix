package com.bsikar.helix.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bsikar.helix.data.Book
import com.bsikar.helix.theme.AppTheme
import kotlinx.coroutines.launch

@Composable
fun BookSection(
    title: String,
    subtitle: String,
    books: List<Book>,
    showProgress: Boolean,
    theme: AppTheme,
    searchQuery: String = "",
    onBookClick: (Book) -> Unit = {},
    onStartReading: (String) -> Unit = {},
    onMarkCompleted: (String) -> Unit = {},
    onMoveToPlanToRead: (String) -> Unit = {},
    onSetProgress: (String, Float) -> Unit = { _, _ -> },
    onEditTags: (String, List<String>) -> Unit = { _, _ -> },
    onUpdateBookSettings: (com.bsikar.helix.data.Book) -> Unit = { _ -> }
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = theme.primaryTextColor
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = theme.secondaryTextColor
                )
                Icon(
                    Icons.Filled.KeyboardArrowUp,
                    contentDescription = "Sort",
                    modifier = Modifier.size(16.dp),
                    tint = theme.secondaryTextColor
                )
                IconButton(onClick = { }) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = "Refresh",
                        modifier = Modifier.size(18.dp),
                        tint = theme.secondaryTextColor
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (showProgress) {
            InfiniteHorizontalBookScroll(
                books = books,
                showProgress = showProgress,
                theme = theme,
                searchQuery = searchQuery,
                modifier = Modifier.height(240.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
                onBookClick = onBookClick,
                onStartReading = onStartReading,
                onMarkCompleted = onMarkCompleted,
                onMoveToPlanToRead = onMoveToPlanToRead,
                onSetProgress = onSetProgress,
                onEditTags = onEditTags,
                onUpdateBookSettings = onUpdateBookSettings
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.height(200.dp)
            ) {
                items(books) { book ->
                    BookCard(
                        book = book, 
                        showProgress = showProgress, 
                        theme = theme, 
                        searchQuery = searchQuery,
                        onBookClick = onBookClick,
                        onStartReading = onStartReading,
                        onMarkCompleted = onMarkCompleted,
                        onMoveToPlanToRead = onMoveToPlanToRead,
                        onSetProgress = onSetProgress,
                        onEditTags = onEditTags,
                        onUpdateBookSettings = onUpdateBookSettings
                    )
                }
            }
        }
    }
}

class CircularLinkedNode<T>(val data: T) {
    var next: CircularLinkedNode<T>? = null
}

class CircularLinkedList<T>(items: List<T>) {
    private var head: CircularLinkedNode<T>? = null
    val size: Int = items.size
    
    init {
        if (items.isNotEmpty()) {
            head = CircularLinkedNode(items[0])
            var current = head
            
            for (i in 1 until items.size) {
                current?.next = CircularLinkedNode(items[i])
                current = current?.next
            }
            // Make it circular
            current?.next = head
        }
    }
    
    fun getWindow(startNode: CircularLinkedNode<T>, windowSize: Int): List<T> {
        val result = mutableListOf<T>()
        var current = startNode
        repeat(windowSize) {
            result.add(current.data)
            current = current.next!!
        }
        return result
    }
    
    fun getNodeAt(position: Int): CircularLinkedNode<T>? {
        if (head == null || size == 0) return null
        var current = head
        repeat(position % size) {
            current = current?.next
        }
        return current
    }
}

@Composable
fun InfiniteHorizontalBookScroll(
    books: List<Book>,
    showProgress: Boolean,
    theme: AppTheme,
    searchQuery: String = "",
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    onBookClick: (Book) -> Unit = {},
    onStartReading: (String) -> Unit = {},
    onMarkCompleted: (String) -> Unit = {},
    onMoveToPlanToRead: (String) -> Unit = {},
    onSetProgress: (String, Float) -> Unit = { _, _ -> },
    onEditTags: (String, List<String>) -> Unit = { _, _ -> },
    onUpdateBookSettings: (com.bsikar.helix.data.Book) -> Unit = { _ -> }
) {
    if (books.isEmpty()) return
    
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    
    // Calculate how many books fit on screen
    val bookCardWidth = 120.dp
    val bookSpacing = 12.dp
    val horizontalPadding = 16.dp * 2
    val booksVisibleOnScreen = ((screenWidthDp.dp - horizontalPadding) / (bookCardWidth + bookSpacing)).toInt()
    
    // If we can fit all books on screen or have only 1 book, don't use infinite scrolling
    if (books.size <= 1 || books.size <= booksVisibleOnScreen) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = contentPadding,
            modifier = modifier
        ) {
            items(
                items = books,
                key = { book -> book.id }
            ) { book ->
                BookCard(
                    book = book,
                    showProgress = showProgress,
                    theme = theme,
                    searchQuery = searchQuery,
                    onBookClick = onBookClick,
                    onStartReading = onStartReading,
                    onMarkCompleted = onMarkCompleted,
                    onMoveToPlanToRead = onMoveToPlanToRead,
                    onSetProgress = onSetProgress,
                    onEditTags = onEditTags,
                    onUpdateBookSettings = onUpdateBookSettings
                )
            }
        }
        return
    }
    
    // For infinite scrolling: create enough repetitions to ensure smooth scrolling
    // but not so many that duplicates are visible on screen
    val minRepetitions = maxOf(10, (booksVisibleOnScreen * 3) / books.size + 1)
    val repeatedBooks = remember(books, minRepetitions) {
        books.let { originalList ->
            (0 until minRepetitions).flatMap { originalList }
        }
    }
    
    // Start position: find a position where no duplicates are visible on initial load
    val initialIndex = remember(books, booksVisibleOnScreen) {
        val safeCycles = booksVisibleOnScreen / books.size + 1
        books.size * safeCycles
    }
    
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val coroutineScope = rememberCoroutineScope()
    
    // Simplified scroll management to avoid measurement issues
    // Only reset position when significantly far from center to avoid rapid jumps
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { firstVisibleItemIndex ->
                val threshold = books.size * 3 // Increased threshold for stability
                val centerStart = books.size * 4 // Safe center position
                
                // Only jump when really close to boundaries and not during rapid scrolling
                when {
                    firstVisibleItemIndex >= repeatedBooks.size - threshold -> {
                        val cyclePosition = firstVisibleItemIndex % books.size
                        val newIndex = centerStart + cyclePosition
                        if (newIndex < repeatedBooks.size - threshold) {
                            coroutineScope.launch {
                                try {
                                    listState.scrollToItem(newIndex)
                                } catch (e: Exception) {
                                    // Ignore scroll errors during rapid state changes
                                }
                            }
                        }
                    }
                    firstVisibleItemIndex <= threshold -> {
                        val cyclePosition = firstVisibleItemIndex % books.size
                        val newIndex = centerStart + cyclePosition
                        if (newIndex >= threshold) {
                            coroutineScope.launch {
                                try {
                                    listState.scrollToItem(newIndex)
                                } catch (e: Exception) {
                                    // Ignore scroll errors during rapid state changes
                                }
                            }
                        }
                    }
                }
            }
    }
    
    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = contentPadding,
        modifier = modifier
    ) {
        items(
            count = repeatedBooks.size,
            key = { index -> "${books[index % books.size].id}_$index" }
        ) { index ->
            val book = repeatedBooks[index]
            BookCard(
                book = book,
                showProgress = showProgress,
                theme = theme,
                searchQuery = searchQuery,
                onBookClick = onBookClick,
                onStartReading = onStartReading,
                onMarkCompleted = onMarkCompleted,
                onMoveToPlanToRead = onMoveToPlanToRead,
                onSetProgress = onSetProgress,
                onEditTags = onEditTags,
                onUpdateBookSettings = onUpdateBookSettings
            )
        }
    }
}