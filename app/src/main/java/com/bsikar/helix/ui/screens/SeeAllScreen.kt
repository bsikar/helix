package com.bsikar.helix.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bsikar.helix.data.model.Book
import com.bsikar.helix.theme.AppTheme
import com.bsikar.helix.ui.components.BookCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeeAllScreen(
    title: String,
    books: List<com.bsikar.helix.data.model.Book>,
    theme: AppTheme,
    onBackClick: () -> Unit,
    onBookClick: (Book) -> Unit = {}
) {
    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = theme.surfaceColor,
                    titleContentColor = theme.primaryTextColor,
                ),
                title = {
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = theme.primaryTextColor
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = theme.primaryTextColor
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        val configuration = LocalConfiguration.current
        val screenWidthDp = configuration.screenWidthDp
        
        // Calculate responsive column count for See All screen
        // Book card width ~120dp + spacing ~12dp = ~132dp per book
        val columns = maxOf(3, minOf(6, screenWidthDp / 132)) // 3-6 columns based on screen width
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(books) { book ->
                BookCard(
                    book = book,
                    showProgress = false,
                    theme = theme,
                    onBookClick = onBookClick
                )
            }
        }
    }
}