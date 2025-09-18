package com.bsikar.helix

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Loading state for cover images
 */
private sealed class CoverLoadingState {
    object Loading : CoverLoadingState()
    data class Success(val bitmap: Bitmap) : CoverLoadingState()
    object Error : CoverLoadingState()
    object NoCover : CoverLoadingState()
}

/**
 * Composable that displays an EPUB book cover with loading states
 */
@Composable
@Suppress("FunctionNaming", "LongMethod", "MagicNumber", "TooGenericExceptionCaught")
fun BookCoverImage(
    epubFile: File,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    cornerRadius: Dp = 8.dp
) {
    val context = LocalContext.current
    val coverCache = remember { CoverImageCache.getInstance(context) }
    var loadingState by remember(epubFile) { mutableStateOf<CoverLoadingState>(CoverLoadingState.Loading) }

    LaunchedEffect(epubFile) {
        loadingState = CoverLoadingState.Loading
        loadingState = withContext(Dispatchers.IO) {
            try {
                val cover = coverCache.getCover(epubFile)
                if (cover != null) {
                    CoverLoadingState.Success(cover)
                } else {
                    CoverLoadingState.NoCover
                }
            } catch (e: Exception) {
                CoverLoadingState.Error
            }
        }
    }

    Card(
        modifier = modifier.size(size),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(cornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (loadingState) {
                is CoverLoadingState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(size * 0.4f),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                is CoverLoadingState.Success -> {
                    val successState = loadingState as CoverLoadingState.Success
                    Image(
                        bitmap = successState.bitmap.asImageBitmap(),
                        contentDescription = "Book cover for ${epubFile.nameWithoutExtension}",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(cornerRadius)),
                        contentScale = ContentScale.Crop
                    )
                }

                is CoverLoadingState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                MaterialTheme.colorScheme.errorContainer,
                                RoundedCornerShape(cornerRadius)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "❌",
                            fontSize = (size.value * 0.4f).sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                is CoverLoadingState.NoCover -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(cornerRadius)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📚",
                            fontSize = (size.value * 0.4f).sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Larger version for detailed views
 */
@Composable
@Suppress("FunctionNaming")
fun BookCoverImageLarge(
    epubFile: File,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    cornerRadius: Dp = 12.dp
) {
    BookCoverImage(
        epubFile = epubFile,
        modifier = modifier,
        size = size,
        cornerRadius = cornerRadius
    )
}
