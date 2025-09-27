package com.bsikar.helix.ui.components

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.bsikar.helix.data.ReaderSettings
import com.bsikar.helix.data.ReadingMode
import com.bsikar.helix.data.TextAlignment

/**
 * WebView-based EPUB content renderer that provides better HTML/CSS support
 * and potentially better performance for complex formatting
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewEpubRenderer(
    htmlContent: String,
    readerSettings: ReaderSettings,
    images: Map<String, String> = emptyMap(),
    onScrollChanged: (Int, Int) -> Unit = { _, _ -> },
    onPageReady: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Generate CSS styles based on reader settings
    val cssStyles = generateCssStyles(readerSettings)
    
    // Preprocess HTML content with embedded images and styles
    val processedHtml = preprocessHtmlContent(htmlContent, cssStyles, images)
    
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        
                        // Inject JavaScript for scroll tracking and other interactions
                        loadUrl("""
                            javascript:(function() {
                                // Track scroll position
                                var lastScrollTop = 0;
                                window.addEventListener('scroll', function() {
                                    var scrollTop = window.pageYOffset || document.documentElement.scrollTop;
                                    var scrollHeight = document.documentElement.scrollHeight;
                                    Android.onScrollChanged(scrollTop, scrollHeight);
                                }, false);
                                
                                // Notify that page is ready
                                Android.onPageReady();
                            })();
                        """.trimIndent())
                    }
                }
                
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    builtInZoomControls = false
                    displayZoomControls = false
                    setSupportZoom(false)
                    
                    // Disable hardware acceleration for better text rendering
                    setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                }
                
                // JavaScript interface for communication will be added when WebView is integrated
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(
                null,
                processedHtml,
                "text/html",
                "UTF-8",
                null
            )
        },
        modifier = modifier.fillMaxSize()
    )
}

/**
 * Generate CSS styles based on reader settings
 */
private fun generateCssStyles(settings: ReaderSettings): String {
    val backgroundColor = when (settings.readingMode) {
        ReadingMode.LIGHT -> "#FFFFFF"
        ReadingMode.DARK -> "#121212"
        ReadingMode.SEPIA -> "#F4ECD8"
        ReadingMode.BLACK -> "#000000"
        ReadingMode.SYSTEM -> "#FFFFFF" // Default to light, will adapt with system theme detection
        ReadingMode.HIGH_CONTRAST_LIGHT -> "#FFFFFF"
        ReadingMode.HIGH_CONTRAST_DARK -> "#000000"
        ReadingMode.HIGH_CONTRAST_YELLOW -> "#FFFF00"
    }
    
    val textColor = when (settings.readingMode) {
        ReadingMode.LIGHT -> "#000000"
        ReadingMode.DARK -> "#E0E0E0"
        ReadingMode.SEPIA -> "#5C4B37"
        ReadingMode.BLACK -> "#FFFFFF"
        ReadingMode.SYSTEM -> "#000000" // Default to light, will adapt with system theme detection
        ReadingMode.HIGH_CONTRAST_LIGHT -> "#000000"
        ReadingMode.HIGH_CONTRAST_DARK -> "#FFFFFF"
        ReadingMode.HIGH_CONTRAST_YELLOW -> "#000000"
    }
    
    val textAlign = when (settings.textAlign) {
        TextAlignment.LEFT -> "left"
        TextAlignment.CENTER -> "center"
        TextAlignment.JUSTIFY -> "justify"
    }
    
    return """
        <style>
            body {
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                font-size: ${settings.fontSize}px;
                line-height: ${settings.lineHeight};
                color: $textColor;
                background-color: $backgroundColor;
                margin: ${settings.marginVertical}px ${settings.marginHorizontal}px;
                padding: 0;
                text-align: $textAlign;
                word-wrap: break-word;
                -webkit-text-size-adjust: none;
            }
            
            /* Headers */
            h1, h2, h3, h4, h5, h6 {
                font-weight: bold;
                margin: 1.5em 0 0.5em 0;
                line-height: 1.3;
            }
            
            h1 { font-size: 1.8em; }
            h2 { font-size: 1.6em; }
            h3 { font-size: 1.4em; }
            h4 { font-size: 1.2em; }
            h5 { font-size: 1.1em; }
            h6 { font-size: 1.0em; }
            
            /* Paragraphs */
            p {
                margin: 0.8em 0;
                text-indent: 1.2em;
            }
            
            /* Lists */
            ul, ol {
                margin: 0.8em 0;
                padding-left: 2em;
            }
            
            li {
                margin: 0.3em 0;
            }
            
            /* Text formatting */
            strong, b {
                font-weight: bold;
            }
            
            em, i {
                font-style: italic;
            }
            
            u {
                text-decoration: underline;
            }
            
            /* Links */
            a {
                color: #007AFF;
                text-decoration: none;
            }
            
            a:visited {
                color: #5856D6;
            }
            
            /* Images */
            img {
                max-width: 100%;
                height: auto;
                display: block;
                margin: 1em auto;
                border-radius: 4px;
            }
            
            /* Tables */
            table {
                width: 100%;
                border-collapse: collapse;
                margin: 1em 0;
            }
            
            th, td {
                border: 1px solid ${if (settings.readingMode == ReadingMode.DARK) "#333" else "#DDD"};
                padding: 0.5em;
                text-align: left;
            }
            
            th {
                background-color: ${if (settings.readingMode == ReadingMode.DARK) "#333" else "#F5F5F5"};
                font-weight: bold;
            }
            
            /* Blockquotes */
            blockquote {
                margin: 1em 2em;
                padding: 0.5em 1em;
                border-left: 4px solid #007AFF;
                background-color: ${if (settings.readingMode == ReadingMode.DARK) "#1E1E1E" else "#F8F9FA"};
                font-style: italic;
            }
            
            /* Code */
            code {
                font-family: 'Courier New', monospace;
                background-color: ${if (settings.readingMode == ReadingMode.DARK) "#2D2D2D" else "#F0F0F0"};
                padding: 0.2em 0.4em;
                border-radius: 3px;
                font-size: 0.9em;
            }
            
            pre {
                background-color: ${if (settings.readingMode == ReadingMode.DARK) "#2D2D2D" else "#F0F0F0"};
                padding: 1em;
                border-radius: 4px;
                overflow-x: auto;
                font-family: 'Courier New', monospace;
                font-size: 0.9em;
                line-height: 1.4;
            }
            
            /* Horizontal rules */
            hr {
                border: none;
                border-top: 1px solid ${if (settings.readingMode == ReadingMode.DARK) "#333" else "#DDD"};
                margin: 2em 0;
            }
            
            /* Dark mode specific adjustments */
            ${if (settings.readingMode == ReadingMode.DARK) """
                img {
                    opacity: 0.9;
                }
                
                a {
                    color: #64B5F6;
                }
                
                a:visited {
                    color: #B39DDB;
                }
            """ else ""}
            
            /* Responsive design */
            @media (max-width: 600px) {
                body {
                    font-size: ${(settings.fontSize * 0.95).toInt()}px;
                    margin: ${(settings.marginVertical * 0.8).toInt()}px ${(settings.marginHorizontal * 0.8).toInt()}px;
                }
                
                h1 { font-size: 1.6em; }
                h2 { font-size: 1.4em; }
                h3 { font-size: 1.2em; }
            }
            
            /* Smooth scrolling */
            html {
                scroll-behavior: smooth;
            }
            
            /* Prevent text selection for better reading experience */
            body {
                -webkit-user-select: none;
                -moz-user-select: none;
                -ms-user-select: none;
                user-select: none;
            }
            
            /* But allow selection for accessibility */
            p, h1, h2, h3, h4, h5, h6, li, blockquote {
                -webkit-user-select: text;
                -moz-user-select: text;
                -ms-user-select: text;
                user-select: text;
            }
        </style>
    """.trimIndent()
}

/**
 * Preprocess HTML content with embedded images and styles
 */
private fun preprocessHtmlContent(
    htmlContent: String,
    cssStyles: String,
    images: Map<String, String>
): String {
    var processedContent = htmlContent
    
    // Replace image sources with base64 encoded images or file paths
    images.forEach { (originalSrc, imagePath) ->
        // For now, we'll use file:// URLs. In a production app, you might want to
        // convert images to base64 or use a different approach
        processedContent = processedContent.replace(
            "src=\"$originalSrc\"",
            "src=\"file://$imagePath\"",
            ignoreCase = true
        )
    }
    
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
            $cssStyles
        </head>
        <body>
            $processedContent
        </body>
        </html>
    """.trimIndent()
}

/**
 * Scroll to a specific position in the WebView
 */
fun scrollToPosition(webView: WebView, position: Int) {
    webView.scrollTo(0, position)
}

/**
 * Scroll to a specific percentage of the content
 */
fun scrollToPercentage(webView: WebView, percentage: Float) {
    webView.post {
        // Use contentHeight directly since scale is deprecated and defaults to 1.0f
        val contentHeight = webView.contentHeight.toFloat()
        val targetY = (contentHeight * percentage).toInt()
        webView.scrollTo(0, targetY)
    }
}