package com.bsikar.helix.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bsikar.helix.theme.AppTheme

@Composable
fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    theme: AppTheme
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { 
            Text(
                "Search your library",
                color = theme.secondaryTextColor,
                fontSize = 14.sp
            )
        },
        leadingIcon = {
            Icon(
                Icons.Filled.Search, 
                contentDescription = "Search",
                tint = theme.secondaryTextColor
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(28.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = theme.accentColor,
            unfocusedBorderColor = theme.secondaryTextColor.copy(alpha = 0.3f),
            focusedContainerColor = theme.surfaceColor,
            unfocusedContainerColor = theme.surfaceColor,
            focusedTextColor = theme.primaryTextColor,
            unfocusedTextColor = theme.primaryTextColor
        ),
        singleLine = true
    )
}