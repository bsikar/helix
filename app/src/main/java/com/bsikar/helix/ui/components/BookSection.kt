package com.bsikar.helix.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bsikar.helix.data.Book
import com.bsikar.helix.theme.AppTheme

@Composable
fun BookSection(
    title: String,
    subtitle: String,
    books: List<Book>,
    showProgress: Boolean,
    theme: AppTheme
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "• $title",
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
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
                modifier = Modifier.height(240.dp)
            ) {
                items(books) { book ->
                    BookCard(book = book, showProgress = showProgress, theme = theme)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.height(200.dp)
            ) {
                items(books) { book ->
                    BookCard(book = book, showProgress = showProgress, theme = theme)
                }
            }
        }
    }
}