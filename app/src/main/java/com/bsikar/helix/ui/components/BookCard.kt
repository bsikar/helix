package com.bsikar.helix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bsikar.helix.data.Book
import com.bsikar.helix.theme.AppTheme

@Composable
fun BookCard(
    book: Book, 
    showProgress: Boolean, 
    theme: AppTheme,
    onBookClick: (Book) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .width(if (showProgress) 120.dp else 110.dp)
            .clickable { onBookClick(book) }
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(0.68f)
                .clip(RoundedCornerShape(8.dp))
                .background(book.coverColor)
        ) {
            if (showProgress && book.progress > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color.Black.copy(alpha = 0.15f))
                        .align(Alignment.BottomCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(book.progress)
                            .fillMaxHeight()
                            .background(theme.accentColor)
                            .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = book.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            color = theme.primaryTextColor,
            lineHeight = 16.sp
        )
        
        Spacer(modifier = Modifier.height(2.dp))
        
        Text(
            text = book.author,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            color = theme.secondaryTextColor
        )
    }
}