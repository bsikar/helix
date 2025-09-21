package com.bsikar.helix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

@Composable
fun HighlightedText(
    text: String,
    searchQuery: String,
    normalColor: Color,
    highlightColor: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight = FontWeight.Normal,
    modifier: Modifier = Modifier
) {
    if (searchQuery.isBlank()) {
        Text(
            text = text,
            fontSize = fontSize,
            color = normalColor,
            fontWeight = fontWeight,
            modifier = modifier
        )
        return
    }
    
    val startIndex = text.indexOf(searchQuery, ignoreCase = true)
    if (startIndex == -1) {
        Text(
            text = text,
            fontSize = fontSize,
            color = normalColor,
            fontWeight = fontWeight,
            modifier = modifier
        )
        return
    }
    
    val endIndex = startIndex + searchQuery.length
    
    Row(modifier = modifier) {
        // Text before match
        if (startIndex > 0) {
            Text(
                text = text.substring(0, startIndex),
                fontSize = fontSize,
                color = normalColor,
                fontWeight = fontWeight
            )
        }
        
        // Highlighted match
        Text(
            text = text.substring(startIndex, endIndex),
            fontSize = fontSize,
            color = highlightColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.background(
                color = highlightColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp)
            ).padding(horizontal = 2.dp)
        )
        
        // Text after match
        if (endIndex < text.length) {
            Text(
                text = text.substring(endIndex),
                fontSize = fontSize,
                color = normalColor,
                fontWeight = fontWeight
            )
        }
    }
}