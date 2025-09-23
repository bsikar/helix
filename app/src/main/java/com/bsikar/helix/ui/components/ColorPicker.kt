package com.bsikar.helix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bsikar.helix.data.model.CoverDisplayMode
import com.bsikar.helix.theme.AppTheme

/**
 * Color picker component for book cover color selection
 */
@Composable
fun ColorPicker(
    currentColor: Color,
    currentDisplayMode: CoverDisplayMode,
    onColorSelected: (Color) -> Unit,
    onDisplayModeChanged: (CoverDisplayMode) -> Unit,
    theme: AppTheme,
    modifier: Modifier = Modifier
) {
    val predefinedColors = listOf(
        // Vibrant color options
        Color(0xFFE53E3E), // Red
        Color(0xFFD69E2E), // Yellow/Orange
        Color(0xFF38A169), // Green
        Color(0xFF3182CE), // Blue
        Color(0xFF805AD5), // Purple
        Color(0xFFEC4899), // Pink
        Color(0xFF319795), // Teal
        Color(0xFFD53F8C), // Pink/Magenta
        Color(0xFF744210), // Brown
        Color(0xFF2D3748), // Dark gray
        Color(0xFF718096), // Light gray
        Color(0xFFED8936), // Orange
        Color(0xFF9F7AEA), // Light purple
        Color(0xFF4FD1C7), // Cyan
        Color(0xFFF687B3), // Light pink
        Color(0xFFBEE3F8), // Light blue
        Color(0xFFC6F6D5), // Light green
        Color(0xFFFED7D7), // Light red
        Color(0xFFFEFCBF), // Light yellow
        Color(0xFFE9D8FD), // Light purple
    )

    Column(modifier = modifier) {
        // Display mode selector
        Text(
            text = "Cover Display",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = theme.primaryTextColor
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            CoverDisplayMode.values().forEach { mode ->
                FilterChip(
                    onClick = { onDisplayModeChanged(mode) },
                    label = {
                        Text(
                            text = when (mode) {
                                CoverDisplayMode.AUTO -> "Auto"
                                CoverDisplayMode.COLOR_ONLY -> "Color Only"
                                CoverDisplayMode.COVER_ART_ONLY -> "Cover Art"
                            },
                            fontSize = 12.sp,
                            color = if (currentDisplayMode == mode) Color.White else theme.primaryTextColor
                        )
                    },
                    selected = currentDisplayMode == mode,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = theme.accentColor,
                        containerColor = theme.surfaceColor.copy(alpha = 0.5f)
                    )
                )
            }
        }
        
        // Color picker (only show when relevant)
        if (currentDisplayMode == CoverDisplayMode.COLOR_ONLY || currentDisplayMode == CoverDisplayMode.AUTO) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Palette,
                    contentDescription = "Color",
                    tint = theme.accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cover Color",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = theme.primaryTextColor
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(160.dp)
            ) {
                items(predefinedColors) { color ->
                    ColorSwatch(
                        color = color,
                        isSelected = currentColor == color,
                        onClick = { onColorSelected(color) },
                        theme = theme
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    theme: AppTheme
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) theme.accentColor else theme.secondaryTextColor.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}