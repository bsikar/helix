package com.bsikar.helix

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
@Suppress("LongMethod", "CyclomaticComplexMethod", "MagicNumber")
fun renderContentElement(
    element: ContentElement,
    config: RenderConfig,
    modifier: Modifier = Modifier
) {
    when (element) {
        is ContentElement.TextParagraph -> {
            Text(
                text = element.text,
                fontSize = config.fontSize.sp,
                lineHeight = (config.fontSize * config.lineHeight).sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Justify,
                modifier = modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }

        is ContentElement.Heading -> {
            val headingSize = when (element.level) {
                1 -> config.fontSize + 8f
                2 -> config.fontSize + 6f
                3 -> config.fontSize + 4f
                4 -> config.fontSize + 2f
                5 -> config.fontSize + 1f
                else -> config.fontSize
            }

            Text(
                text = element.text,
                fontSize = headingSize.sp,
                lineHeight = (headingSize * config.lineHeight).sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = if (element.level <= 2) TextAlign.Center else TextAlign.Start,
                modifier = modifier
                    .fillMaxWidth()
                    .padding(
                        top = if (element.level <= 2) 24.dp else 16.dp,
                        bottom = if (element.level <= 2) 16.dp else 12.dp
                    )
            )
        }

        is ContentElement.Image -> {
            epubImageRenderer(
                imageSrc = element.src,
                alt = element.alt,
                caption = element.caption,
                config = config,
                modifier = modifier
                    .fillMaxWidth()
            )
        }

        is ContentElement.Quote -> {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "\"${element.text}\"",
                    fontSize = config.fontSize.sp,
                    lineHeight = (config.fontSize * config.lineHeight).sp,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Justify,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        is ContentElement.List -> {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                element.items.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = 16.dp)
                    ) {
                        Text(
                            text = if (element.isOrdered) "${index + 1}." else "•",
                            fontSize = config.fontSize.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(24.dp)
                        )
                        Text(
                            text = item,
                            fontSize = config.fontSize.sp,
                            lineHeight = (config.fontSize * config.lineHeight).sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        is ContentElement.Divider -> {
            HorizontalDivider(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 32.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
@Suppress("LongMethod", "CyclomaticComplexMethod")
fun epubImageRenderer(
    imageSrc: String,
    alt: String,
    caption: String,
    config: RenderConfig,
    modifier: Modifier = Modifier
) {
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var imageHeight by remember { mutableStateOf(200.dp) }

    LaunchedEffect(imageSrc, config.epubFile) {
        // Reset states when imageSrc changes
        isLoading = true
        hasError = false
        errorMessage = ""
        imageBitmap = null

        if (config.epubFile != null && !imageSrc.startsWith("http")) {
            try {
                val imageLoader = OptimizedImageLoader.getInstance()

                val bitmap = imageLoader.loadImage(
                    epubFile = config.epubFile,
                    imagePath = imageSrc
                )

                if (bitmap != null) {
                    imageBitmap = bitmap
                    hasError = false
                    // Calculate the actual image height to maintain stable layout
                    val aspectRatio = bitmap.height.toFloat() / bitmap.width.toFloat()
                    imageHeight = (300.dp * aspectRatio).coerceAtMost(600.dp)
                } else {
                    hasError = true
                    errorMessage = "Image not found in EPUB"
                }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                hasError = true
                errorMessage = if (e is kotlinx.coroutines.CancellationException) {
                    return@LaunchedEffect // Don't set error state for cancellation
                } else {
                    "Failed to load image"
                }
            }
        } else {
            if (config.epubFile == null) {
                errorMessage = "No EPUB file"
            } else if (imageSrc.startsWith("http")) {
                errorMessage = "HTTP URLs not supported"
            }
            hasError = true
        }
        isLoading = false
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (!config.onlyShowImages) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Loading image...",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            hasError -> {
                if (!config.onlyShowImages) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Image not available",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Medium
                            )
                            if (errorMessage.isNotBlank()) {
                                Text(
                                    text = errorMessage,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            Text(
                                text = "Path: $imageSrc",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            if (alt.isNotBlank()) {
                                Text(
                                    text = alt,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            imageBitmap != null -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        bitmap = imageBitmap!!,
                        contentDescription = alt.ifBlank { "EPUB image" },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!config.onlyShowImages && (caption.isNotBlank() || alt.isNotBlank())) {
                        Text(
                            text = caption.ifBlank { alt },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.padding(top = 4.dp).padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}
