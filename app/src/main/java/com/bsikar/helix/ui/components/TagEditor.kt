package com.bsikar.helix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bsikar.helix.data.Book
import com.bsikar.helix.data.Tag
import com.bsikar.helix.data.TagCategory
import com.bsikar.helix.data.PresetTags
import com.bsikar.helix.theme.AppTheme

@Composable
fun TagEditorDialog(
    book: Book,
    theme: AppTheme,
    onDismiss: () -> Unit,
    onTagsUpdated: (List<String>) -> Unit
) {
    var selectedTags by remember { mutableStateOf(book.tags.toSet()) }
    var expandedCategory by remember { mutableStateOf<TagCategory?>(null) }
    var showAddCustomTag by remember { mutableStateOf(false) }
    var customTagText by remember { mutableStateOf("") }
    var selectedCategoryForCustom by remember { mutableStateOf(TagCategory.THEME) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            colors = CardDefaults.cardColors(containerColor = theme.surfaceColor),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Surface(
                    color = theme.accentColor,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Edit Tags",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = book.title,
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }
                }
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Current Tags Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = theme.backgroundColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = null,
                                    tint = theme.accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Selected Tags (${selectedTags.size})",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = theme.primaryTextColor
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            if (selectedTags.isEmpty()) {
                                Text(
                                    text = "No tags selected",
                                    fontSize = 13.sp,
                                    color = theme.secondaryTextColor,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                )
                            } else {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(selectedTags.toList()) { tagId ->
                                        PresetTags.findTagById(tagId)?.let { tag ->
                                            ModernTagChip(
                                                tag = tag,
                                                onRemove = { selectedTags = selectedTags - tagId },
                                                theme = theme
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Add Custom Tag Section
                    CustomTagSection(
                        showAddCustomTag = showAddCustomTag,
                        customTagText = customTagText,
                        selectedCategoryForCustom = selectedCategoryForCustom,
                        onShowAddCustomTagChanged = { showAddCustomTag = it },
                        onCustomTagTextChanged = { customTagText = it },
                        onSelectedCategoryForCustomChanged = { selectedCategoryForCustom = it },
                        onAddCustomTag = { tagText, category ->
                            // Add custom tag to PresetTags
                            val customTag = PresetTags.addCustomTag(tagText, category)
                            selectedTags = selectedTags + customTag.id
                            customTagText = ""
                            showAddCustomTag = false
                        },
                        theme = theme
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Category-based tag selection
                    CategoryTagsSection(
                        expandedCategory = expandedCategory,
                        selectedTags = selectedTags,
                        onCategoryToggled = { category ->
                            expandedCategory = if (expandedCategory == category) null else category
                        },
                        onToggleTag = { tagId ->
                            selectedTags = if (selectedTags.contains(tagId)) {
                                selectedTags - tagId
                            } else {
                                selectedTags + tagId
                            }
                        },
                        theme = theme,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = theme.secondaryTextColor
                            )
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = { 
                                onTagsUpdated(selectedTags.toList())
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = theme.accentColor
                            )
                        ) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Changes", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernTagChip(
    tag: Tag,
    onRemove: () -> Unit,
    theme: AppTheme
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp)),
        color = tag.color,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tag.name,
                fontSize = 13.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onRemove() },
                color = Color.White.copy(alpha = 0.3f),
                shape = RoundedCornerShape(9.dp)
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Remove tag",
                    tint = Color.White,
                    modifier = Modifier
                        .size(12.dp)
                        .padding(2.dp)
                )
            }
        }
    }
}

@Composable
fun CustomTagSection(
    showAddCustomTag: Boolean,
    customTagText: String,
    selectedCategoryForCustom: TagCategory,
    onShowAddCustomTagChanged: (Boolean) -> Unit,
    onCustomTagTextChanged: (String) -> Unit,
    onSelectedCategoryForCustomChanged: (TagCategory) -> Unit,
    onAddCustomTag: (String, TagCategory) -> Unit,
    theme: AppTheme
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = theme.backgroundColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add Custom Tag",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.primaryTextColor
                )
                
                IconButton(
                    onClick = { onShowAddCustomTagChanged(!showAddCustomTag) }
                ) {
                    Icon(
                        if (showAddCustomTag) Icons.Filled.KeyboardArrowUp else Icons.Filled.Add,
                        contentDescription = if (showAddCustomTag) "Collapse" else "Add Custom Tag",
                        tint = theme.accentColor
                    )
                }
            }
            
            if (showAddCustomTag) {
                Spacer(modifier = Modifier.height(12.dp))
                
                // Category selector for custom tag
                Text(
                    text = "Category:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = theme.secondaryTextColor
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val availableCategories = listOf(
                        TagCategory.GENRE,
                        TagCategory.DEMOGRAPHIC,
                        TagCategory.THEME,
                        TagCategory.STATUS,
                        TagCategory.FORMAT
                    )
                    
                    items(availableCategories) { category ->
                        FilterChip(
                            onClick = { onSelectedCategoryForCustomChanged(category) },
                            label = {
                                Text(
                                    text = category.displayName,
                                    fontSize = 11.sp,
                                    color = if (selectedCategoryForCustom == category) Color.White else theme.primaryTextColor
                                )
                            },
                            selected = selectedCategoryForCustom == category,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = theme.accentColor,
                                containerColor = theme.surfaceColor.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Custom tag input
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customTagText,
                        onValueChange = onCustomTagTextChanged,
                        label = { Text("Tag Name", fontSize = 12.sp) },
                        placeholder = { Text("Enter custom tag", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = theme.accentColor,
                            focusedLabelColor = theme.accentColor,
                            cursorColor = theme.accentColor
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (customTagText.isNotBlank()) {
                                    onAddCustomTag(customTagText.trim(), selectedCategoryForCustom)
                                }
                                keyboardController?.hide()
                            }
                        ),
                        singleLine = true
                    )
                    
                    Button(
                        onClick = {
                            if (customTagText.isNotBlank()) {
                                onAddCustomTag(customTagText.trim(), selectedCategoryForCustom)
                            }
                        },
                        enabled = customTagText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.accentColor
                        ),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Add Tag",
                            tint = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = "Custom tags will be added to the ${selectedCategoryForCustom.displayName} category",
                    fontSize = 10.sp,
                    color = theme.secondaryTextColor,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

@Composable
fun CategoryTagsSection(
    expandedCategory: TagCategory?,
    selectedTags: Set<String>,
    onCategoryToggled: (TagCategory) -> Unit,
    onToggleTag: (String) -> Unit,
    theme: AppTheme,
    modifier: Modifier = Modifier
) {
    val categories = listOf(
        TagCategory.FORMAT,
        TagCategory.GENRE,
        TagCategory.DEMOGRAPHIC,
        TagCategory.THEME,
        TagCategory.STATUS
    )
    
    Column(modifier = modifier) {
        Text(
            text = "Browse by Category",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = theme.primaryTextColor
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            categories.forEach { category ->
                CategoryCard(
                    category = category,
                    isExpanded = expandedCategory == category,
                    selectedTags = selectedTags,
                    onCategoryToggled = onCategoryToggled,
                    onToggleTag = onToggleTag,
                    theme = theme
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun CategoryCard(
    category: TagCategory,
    isExpanded: Boolean,
    selectedTags: Set<String>,
    onCategoryToggled: (TagCategory) -> Unit,
    onToggleTag: (String) -> Unit,
    theme: AppTheme
) {
    val categoryTags = PresetTags.getTagsByCategory(category)
    val selectedCount = categoryTags.count { selectedTags.contains(it.id) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) theme.accentColor.copy(alpha = 0.1f) else theme.backgroundColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // Category header
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCategoryToggled(category) },
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = category.displayName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = theme.primaryTextColor
                        )
                        if (selectedCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = theme.accentColor,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = selectedCount.toString(),
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Icon(
                        if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = theme.secondaryTextColor
                    )
                }
            }
            
            // Category tags (when expanded)
            if (isExpanded && categoryTags.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                        .heightIn(max = 200.dp)
                ) {
                    items(categoryTags) { tag ->
                        CategoryTagButton(
                            tag = tag,
                            isSelected = selectedTags.contains(tag.id),
                            onToggle = { onToggleTag(tag.id) },
                            theme = theme
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryTagButton(
    tag: Tag,
    isSelected: Boolean,
    onToggle: () -> Unit,
    theme: AppTheme
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        color = if (isSelected) tag.color else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isSelected) tag.color else tag.color.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(12.dp),
                    color = tag.color,
                    shape = RoundedCornerShape(6.dp)
                ) {}
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tag.name,
                    fontSize = 13.sp,
                    color = if (isSelected) Color.White else theme.primaryTextColor,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
                if (PresetTags.isCustomTag(tag.id)) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        modifier = Modifier.size(14.dp),
                        color = if (isSelected) Color.White.copy(alpha = 0.3f) else tag.color.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(7.dp)
                    ) {
                        Text(
                            text = "✦",
                            fontSize = 8.sp,
                            color = if (isSelected) Color.White else tag.color,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.wrapContentSize(Alignment.Center)
                        )
                    }
                }
            }
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
}

