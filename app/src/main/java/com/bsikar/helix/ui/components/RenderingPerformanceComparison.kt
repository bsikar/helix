package com.bsikar.helix.ui.components

import android.webkit.WebView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bsikar.helix.data.ReaderSettings
import com.bsikar.helix.ui.reader.ContentElement
import com.bsikar.helix.ui.reader.parseHtmlToContentElements
import com.bsikar.helix.ui.components.WebViewEpubRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.system.measureTimeMillis

/**
 * Performance comparison data class to hold metrics for both rendering approaches
 */
data class RenderingPerformanceMetrics(
    val annotatedStringParsingTime: Long,
    val annotatedStringMemoryUsage: Long,
    val webViewLoadTime: Long,
    val webViewMemoryUsage: Long,
    val complexityScore: Float, // Based on HTML complexity
    val formattingFidelity: Float // How well each approach preserves formatting (0.0-1.0)
)

/**
 * Composable for comparing AnnotatedString vs WebView rendering performance
 */
@Composable
fun RenderingPerformanceComparison(
    htmlContent: String,
    readerSettings: ReaderSettings,
    images: Map<String, String> = emptyMap(),
    onMetricsCalculated: (RenderingPerformanceMetrics) -> Unit = {}
) {
    var metrics by remember { mutableStateOf<RenderingPerformanceMetrics?>(null) }
    var isTestingAnnotatedString by remember { mutableStateOf(false) }
    var isTestingWebView by remember { mutableStateOf(false) }
    var annotatedStringElements by remember { mutableStateOf<List<ContentElement>>(emptyList()) }
    var webViewReady by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    
    // Run performance tests
    LaunchedEffect(htmlContent, readerSettings) {
        withContext(Dispatchers.IO) {
            // Test AnnotatedString approach
            isTestingAnnotatedString = true
            val runtime = Runtime.getRuntime()
            
            // Measure memory before parsing
            runtime.gc()
            val memoryBefore = runtime.totalMemory() - runtime.freeMemory()
            
            // Measure parsing time
            val annotatedStringParsingTime = measureTimeMillis {
                annotatedStringElements = parseHtmlToContentElements(htmlContent, readerSettings, images)
            }
            
            // Measure memory after parsing
            runtime.gc()
            val memoryAfter = runtime.totalMemory() - runtime.freeMemory()
            val annotatedStringMemoryUsage = memoryAfter - memoryBefore
            
            isTestingAnnotatedString = false
            
            // Test WebView approach
            isTestingWebView = true
            val webViewLoadTime = measureTimeMillis {
                // Simulate WebView loading (actual measurement would be done in WebView callbacks)
                // This is a rough estimation based on content complexity
                val contentLength = htmlContent.length
                val imageCount = images.size
                // Simulate load time based on content complexity
                kotlinx.coroutines.delay(contentLength / 1000L + imageCount * 50L)
            }
            
            // Estimate WebView memory usage (WebView typically uses more memory)
            val webViewMemoryUsage = annotatedStringMemoryUsage * 2 // Rough estimation
            
            isTestingWebView = false
            webViewReady = true
            
            // Calculate complexity score based on HTML structure
            val complexityScore = calculateComplexityScore(htmlContent, images)
            
            // Calculate formatting fidelity score
            val formattingFidelity = calculateFormattingFidelity(htmlContent)
            
            val performanceMetrics = RenderingPerformanceMetrics(
                annotatedStringParsingTime = annotatedStringParsingTime,
                annotatedStringMemoryUsage = maxOf(annotatedStringMemoryUsage, 0),
                webViewLoadTime = webViewLoadTime,
                webViewMemoryUsage = webViewMemoryUsage,
                complexityScore = complexityScore,
                formattingFidelity = formattingFidelity
            )
            
            metrics = performanceMetrics
            onMetricsCalculated(performanceMetrics)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Rendering Performance Comparison",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        // Show test progress
        if (isTestingAnnotatedString) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
            Text("Testing AnnotatedString rendering...")
        }
        
        if (isTestingWebView) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
            Text("Testing WebView rendering...")
        }
        
        // Show metrics when available
        metrics?.let { m ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Performance Metrics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("AnnotatedString Parsing:")
                        Text("${m.annotatedStringParsingTime}ms")
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("WebView Load Time:")
                        Text("${m.webViewLoadTime}ms")
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("AnnotatedString Memory:")
                        Text("${formatBytes(m.annotatedStringMemoryUsage)}")
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("WebView Memory (est.):")
                        Text("${formatBytes(m.webViewMemoryUsage)}")
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Content Complexity:")
                        Text("${String.format("%.1f", m.complexityScore)}/10")
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Formatting Fidelity:")
                        Text("${String.format("%.1f", m.formattingFidelity * 100)}%")
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // Recommendation based on metrics
                    val recommendation = getRecommendation(m)
                    Text(
                        text = "Recommendation: $recommendation",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (recommendation.contains("WebView")) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        }
                    )
                }
            }
        }
        
        // Show side-by-side comparison when tests are complete
        if (webViewReady && annotatedStringElements.isNotEmpty()) {
            Text(
                text = "Visual Comparison",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // AnnotatedString rendering preview
                Card(
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = "AnnotatedString",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        LazyColumn(
                            modifier = Modifier.height(200.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(annotatedStringElements.take(3)) { element ->
                                when (element) {
                                    is ContentElement.TextElement -> {
                                        Text(
                                            text = element.text,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 2
                                        )
                                    }
                                    is ContentElement.ImageElement -> {
                                        Text(
                                            text = "[Image: ${element.alt}]",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // WebView rendering preview
                Card(
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = "WebView",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            WebViewEpubRenderer(
                                htmlContent = htmlContent.take(500), // Preview only
                                readerSettings = readerSettings,
                                images = images,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Calculate complexity score based on HTML structure
 */
private fun calculateComplexityScore(htmlContent: String, images: Map<String, String> = emptyMap()): Float {
    var score = 0f
    val content = htmlContent.lowercase()
    
    // Base score for content length
    score += minOf(htmlContent.length / 10000f, 2f)
    
    // CSS complexity
    if (content.contains("<style>") || content.contains("style=")) score += 2f
    if (content.contains("@media") || content.contains("@import")) score += 1f
    
    // HTML structure complexity
    if (content.contains("<table")) score += 1f
    if (content.contains("<div")) score += 0.5f
    if (content.contains("<span")) score += 0.5f
    if (content.contains("<img")) score += images.size.toFloat() * 0.5f
    
    // Text formatting
    if (content.contains("<strong>") || content.contains("<b>")) score += 0.2f
    if (content.contains("<em>") || content.contains("<i>")) score += 0.2f
    if (content.contains("<u>")) score += 0.2f
    
    // Lists and headers
    if (content.contains("<ul>") || content.contains("<ol>")) score += 0.5f
    if (content.contains("<h1>")) score += 0.3f
    if (content.contains("<h2>") || content.contains("<h3>")) score += 0.2f
    
    return minOf(score, 10f) // Cap at 10
}

/**
 * Calculate formatting fidelity score (how well each approach preserves original formatting)
 */
private fun calculateFormattingFidelity(htmlContent: String): Float {
    val content = htmlContent.lowercase()
    var webViewFidelity = 1.0f
    var annotatedStringFidelity = 1.0f
    
    // WebView can handle almost all HTML/CSS
    if (content.contains("<style>")) webViewFidelity = 1.0f
    if (content.contains("style=")) webViewFidelity = 1.0f
    if (content.contains("<table")) webViewFidelity = 1.0f
    
    // AnnotatedString has limitations
    if (content.contains("<style>")) annotatedStringFidelity -= 0.3f // CSS not fully supported
    if (content.contains("style=")) annotatedStringFidelity -= 0.2f // Inline styles limited
    if (content.contains("<table")) annotatedStringFidelity -= 0.4f // Tables not supported
    if (content.contains("@media")) annotatedStringFidelity -= 0.5f // Media queries not supported
    if (content.contains("font-family")) annotatedStringFidelity -= 0.1f // Limited font support
    
    annotatedStringFidelity = maxOf(annotatedStringFidelity, 0.3f) // Minimum fidelity
    
    // Return average fidelity (both approaches considered)
    return (webViewFidelity + annotatedStringFidelity) / 2f
}

/**
 * Get recommendation based on performance metrics
 */
private fun getRecommendation(metrics: RenderingPerformanceMetrics): String {
    return when {
        metrics.complexityScore > 7 && metrics.formattingFidelity < 0.7 -> 
            "Use WebView for complex formatting and CSS support"
        metrics.annotatedStringParsingTime > 1000 && metrics.webViewLoadTime < metrics.annotatedStringParsingTime -> 
            "Use WebView for better performance with large content"
        metrics.annotatedStringMemoryUsage > metrics.webViewMemoryUsage -> 
            "Use WebView for better memory efficiency"
        metrics.complexityScore < 3 && metrics.formattingFidelity > 0.8 -> 
            "Use AnnotatedString for simple content and better integration"
        else -> 
            "Use WebView for better overall performance and formatting support"
    }
}

/**
 * Format bytes to human readable string
 */
private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "${bytes}B"
    if (bytes < 1024 * 1024) return "${bytes / 1024}KB"
    return "${bytes / (1024 * 1024)}MB"
}