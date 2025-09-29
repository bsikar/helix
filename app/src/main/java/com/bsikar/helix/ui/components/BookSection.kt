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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bsikar.helix.data.model.Book
import com.bsikar.helix.theme.AppTheme
import kotlinx.coroutines.launch

@Composable
fun BookSection(
    title: String,
    subtitle: String,
    books: List<com.bsikar.helix.data.model.Book>,
    showProgress: Boolean,
    theme: AppTheme,
    searchQuery: String = "",
    onBookClick: (Book) -> Unit = {},
    onStartReading: (String) -> Unit = {},
    onMarkCompleted: (String) -> Unit = {},
    onMoveToOnDeck: (String) -> Unit = {},
    onSetProgress: (String, Float) -> Unit = { _, _ -> },
    onEditTags: (String, List<String>) -> Unit = { _, _ -> },
    onUpdateBookSettings: (Book) -> Unit = { _ -> }
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
                onMoveToOnDeck = onMoveToOnDeck,
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
                        onMoveToOnDeck = onMoveToOnDeck,
                        onSetProgress = onSetProgress,
                        onEditTags = onEditTags,
                        onUpdateBookSettings = onUpdateBookSettings
                    )
                }
            }
        }
    }
}

// Removed CircularLinkedList - replaced with simpler modulo-based approach

@Composable
fun InfiniteHorizontalBookScroll(
    books: List<com.bsikar.helix.data.model.Book>,
    showProgress: Boolean,
    theme: AppTheme,
    searchQuery: String = "",
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    isBrowseMode: Boolean = false,
    onBookClick: (Book) -> Unit = {},
    onStartReading: (String) -> Unit = {},
    onMarkCompleted: (String) -> Unit = {},
    onMoveToOnDeck: (String) -> Unit = {},
    onSetProgress: (String, Float) -> Unit = { _, _ -> },
    onEditTags: (String, List<String>) -> Unit = { _, _ -> },
    onUpdateBookSettings: (Book) -> Unit = { _ -> },
    onRemoveFromLibrary: (String) -> Unit = { _ -> }
) {
    if (books.isEmpty()) return
    
    // Stabilize book list to prevent rapid key changes that cause deactivated node issues
    val stableBooks by remember(books.map { it.id }.joinToString(",")) {
        mutableStateOf(books)
    }
    
    // Use stable book list for calculations
    val booksToUse = if (stableBooks.map { it.id } == books.map { it.id }) books else stableBooks
    
    // Calculate visible items to determine if infinite scrolling is needed
    val configuration = LocalConfiguration.current
    val bookCardWidth = 120.dp
    val bookSpacing = 12.dp
    val horizontalPadding = contentPadding.calculateLeftPadding(LayoutDirection.Ltr) + 
                          contentPadding.calculateRightPadding(LayoutDirection.Ltr)
    val availableWidth = configuration.screenWidthDp.dp - horizontalPadding
    val booksVisibleOnScreen = maxOf(1, (availableWidth / (bookCardWidth + bookSpacing)).toInt())
    
    // If we can fit all books on screen or have very few books, use simple LazyRow
    if (booksToUse.size <= booksVisibleOnScreen || booksToUse.size <= 3) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = contentPadding,
            modifier = modifier
        ) {
            items(
                items = booksToUse,
                key = { book -> "simple_${book.id}" }
            ) { book ->
                BookCard(
                    book = book,
                    showProgress = showProgress,
                    theme = theme,
                    searchQuery = searchQuery,
                    isBrowseMode = isBrowseMode,
                    onBookClick = onBookClick,
                    onStartReading = onStartReading,
                    onMarkCompleted = onMarkCompleted,
                    onMoveToOnDeck = onMoveToOnDeck,
                    onSetProgress = onSetProgress,
                    onEditTags = onEditTags,
                    onUpdateBookSettings = onUpdateBookSettings,
                    onRemoveFromLibrary = onRemoveFromLibrary
                )
            }
        }
        return
    }
    
    // For infinite scrolling: use more conservative approach
    val maxItems = 1000 // Reduced from Int.MAX_VALUE to prevent memory issues
    val repeatCount = maxItems / booksToUse.size
    val startIndex = (repeatCount / 2) * booksToUse.size
    
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    
    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = contentPadding,
        modifier = modifier
    ) {
        items(
            count = maxItems,
            key = { index -> 
                val bookIndex = index % booksToUse.size
                val cycle = index / booksToUse.size
                "infinite_${booksToUse[bookIndex].id}_cycle_$cycle"
            }
        ) { index ->
            val bookIndex = index % booksToUse.size
            if (bookIndex < booksToUse.size) {
                val book = booksToUse[bookIndex]
                BookCard(
                    book = book,
                    showProgress = showProgress,
                    theme = theme,
                    searchQuery = searchQuery,
                    isBrowseMode = isBrowseMode,
                    onBookClick = onBookClick,
                    onStartReading = onStartReading,
                    onMarkCompleted = onMarkCompleted,
                    onMoveToOnDeck = onMoveToOnDeck,
                    onSetProgress = onSetProgress,
                    onEditTags = onEditTags,
                    onUpdateBookSettings = onUpdateBookSettings,
                    onRemoveFromLibrary = onRemoveFromLibrary
                )
            }
        }
    }
}