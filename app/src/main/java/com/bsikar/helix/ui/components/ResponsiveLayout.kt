package com.bsikar.helix.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Responsive layout configuration based on screen size
 */
data class ResponsiveConfig(
    val columns: Int,
    val spacing: Dp,
    val contentPadding: PaddingValues,
    val itemMinWidth: Dp,
    val isTablet: Boolean,
    val isFoldable: Boolean
) {
    companion object {
        /**
         * Create responsive configuration based on screen width
         */
        @Composable
        fun fromScreenWidth(): ResponsiveConfig {
            val configuration = LocalConfiguration.current
            val density = LocalDensity.current
            
            val screenWidthDp = configuration.screenWidthDp.dp
            val screenHeightDp = configuration.screenHeightDp.dp
            
            // Determine device type
            val isTablet = screenWidthDp >= 600.dp
            val isFoldable = screenWidthDp >= 840.dp // Unfolded tablet/foldable
            
            return when {
                // Foldable/Large tablets (840dp+)
                isFoldable -> ResponsiveConfig(
                    columns = 6,
                    spacing = 16.dp,
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                    itemMinWidth = 120.dp,
                    isTablet = true,
                    isFoldable = true
                )
                // Regular tablets (600-839dp)
                isTablet -> ResponsiveConfig(
                    columns = 4,
                    spacing = 12.dp,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    itemMinWidth = 110.dp,
                    isTablet = true,
                    isFoldable = false
                )
                // Large phones (480-599dp)
                screenWidthDp >= 480.dp -> ResponsiveConfig(
                    columns = 3,
                    spacing = 8.dp,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    itemMinWidth = 100.dp,
                    isTablet = false,
                    isFoldable = false
                )
                // Regular phones (360-479dp)
                screenWidthDp >= 360.dp -> ResponsiveConfig(
                    columns = 3,
                    spacing = 8.dp,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    itemMinWidth = 90.dp,
                    isTablet = false,
                    isFoldable = false
                )
                // Small phones (<360dp)
                else -> ResponsiveConfig(
                    columns = 2,
                    spacing = 6.dp,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    itemMinWidth = 80.dp,
                    isTablet = false,
                    isFoldable = false
                )
            }
        }
        
        /**
         * Create adaptive columns based on available width and minimum item width
         */
        @Composable
        fun adaptiveColumns(
            availableWidth: Dp,
            minItemWidth: Dp,
            spacing: Dp = 8.dp,
            maxColumns: Int = 8
        ): Int {
            val totalSpacing = spacing * (maxColumns - 1)
            val availableForItems = availableWidth - totalSpacing
            val possibleColumns = (availableForItems / minItemWidth).toInt()
            return possibleColumns.coerceIn(1, maxColumns)
        }
    }
}

/**
 * Responsive book grid that adapts to screen size
 */
@Composable
fun <T> ResponsiveBookGrid(
    items: List<T>,
    modifier: Modifier = Modifier,
    config: ResponsiveConfig = ResponsiveConfig.fromScreenWidth(),
    key: ((item: T) -> Any)? = null,
    itemContent: @Composable LazyGridItemScope.(item: T) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(config.columns),
        modifier = modifier,
        contentPadding = config.contentPadding,
        horizontalArrangement = Arrangement.spacedBy(config.spacing),
        verticalArrangement = Arrangement.spacedBy(config.spacing)
    ) {
        items(
            count = items.size,
            key = if (key != null) { index -> key(items[index]) } else null
        ) { index ->
            itemContent(items[index])
        }
    }
}

/**
 * Adaptive book grid that adjusts columns based on available width
 */
@Composable
fun <T> AdaptiveBookGrid(
    items: List<T>,
    modifier: Modifier = Modifier,
    minItemWidth: Dp = 100.dp,
    spacing: Dp = 8.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    maxColumns: Int = 6,
    key: ((item: T) -> Any)? = null,
    itemContent: @Composable LazyGridItemScope.(item: T) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minItemWidth),
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        items(
            count = items.size,
            key = if (key != null) { index -> key(items[index]) } else null
        ) { index ->
            itemContent(items[index])
        }
    }
}

/**
 * Responsive layout that switches between list and grid based on screen size
 */
@Composable
fun <T> ResponsiveListOrGrid(
    items: List<T>,
    modifier: Modifier = Modifier,
    forceGrid: Boolean = false,
    listContent: LazyListScope.(items: List<T>) -> Unit,
    gridContent: LazyGridScope.(items: List<T>) -> Unit
) {
    val config = ResponsiveConfig.fromScreenWidth()
    
    if (forceGrid || config.isTablet) {
        // Use grid layout for tablets and when forced
        LazyVerticalGrid(
            columns = GridCells.Fixed(config.columns),
            modifier = modifier,
            contentPadding = config.contentPadding,
            horizontalArrangement = Arrangement.spacedBy(config.spacing),
            verticalArrangement = Arrangement.spacedBy(config.spacing)
        ) {
            gridContent(items)
        }
    } else {
        // Use list layout for phones
        LazyColumn(
            modifier = modifier,
            contentPadding = config.contentPadding,
            verticalArrangement = Arrangement.spacedBy(config.spacing)
        ) {
            listContent(items)
        }
    }
}

/**
 * Window size class for responsive design
 */
enum class WindowSizeClass {
    COMPACT,  // < 600dp width
    MEDIUM,   // 600-839dp width  
    EXPANDED  // >= 840dp width
}

/**
 * Get current window size class
 */
@Composable
fun getWindowSizeClass(): WindowSizeClass {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    
    return when {
        screenWidth < 600.dp -> WindowSizeClass.COMPACT
        screenWidth < 840.dp -> WindowSizeClass.MEDIUM
        else -> WindowSizeClass.EXPANDED
    }
}

/**
 * Responsive spacing values based on screen size
 */
object ResponsiveSpacing {
    @Composable
    fun small(): Dp {
        val windowSize = getWindowSizeClass()
        return when (windowSize) {
            WindowSizeClass.COMPACT -> 4.dp
            WindowSizeClass.MEDIUM -> 6.dp
            WindowSizeClass.EXPANDED -> 8.dp
        }
    }
    
    @Composable
    fun medium(): Dp {
        val windowSize = getWindowSizeClass()
        return when (windowSize) {
            WindowSizeClass.COMPACT -> 8.dp
            WindowSizeClass.MEDIUM -> 12.dp
            WindowSizeClass.EXPANDED -> 16.dp
        }
    }
    
    @Composable
    fun large(): Dp {
        val windowSize = getWindowSizeClass()
        return when (windowSize) {
            WindowSizeClass.COMPACT -> 16.dp
            WindowSizeClass.MEDIUM -> 20.dp
            WindowSizeClass.EXPANDED -> 24.dp
        }
    }
    
    @Composable
    fun contentPadding(): PaddingValues {
        val windowSize = getWindowSizeClass()
        return when (windowSize) {
            WindowSizeClass.COMPACT -> PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            WindowSizeClass.MEDIUM -> PaddingValues(horizontal = 20.dp, vertical = 12.dp)
            WindowSizeClass.EXPANDED -> PaddingValues(horizontal = 24.dp, vertical = 16.dp)
        }
    }
}

/**
 * Extension function to make BookCard responsive to different layouts
 */
@Composable
fun ResponsiveBookCard(
    book: com.bsikar.helix.data.model.Book,
    showProgress: Boolean,
    theme: com.bsikar.helix.theme.AppTheme,
    searchQuery: String = "",
    config: ResponsiveConfig = ResponsiveConfig.fromScreenWidth(),
    isBrowseMode: Boolean = false,
    onBookClick: (com.bsikar.helix.data.model.Book) -> Unit = {},
    onStartReading: (String) -> Unit = {},
    onMarkCompleted: (String) -> Unit = {},
    onMoveToOnDeck: (String) -> Unit = {},
    onSetProgress: (String, Float) -> Unit = { _, _ -> },
    onEditTags: (String, List<String>) -> Unit = { _, _ -> },
    onUpdateBookSettings: (com.bsikar.helix.data.model.Book) -> Unit = { _ -> },
    onRemoveFromLibrary: (String) -> Unit = { _ -> }
) {
    // Adjust card width based on grid configuration
    val cardWidth = when {
        config.isFoldable -> 140.dp
        config.isTablet -> 130.dp
        else -> if (showProgress) 120.dp else 110.dp
    }
    
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