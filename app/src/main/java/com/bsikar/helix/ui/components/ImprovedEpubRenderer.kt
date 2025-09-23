package com.bsikar.helix.ui.components

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.bsikar.helix.data.ReaderSettings
import com.bsikar.helix.data.ReadingMode
import com.bsikar.helix.data.TextAlignment

/**
 * Improved EPUB renderer using WebView for better HTML/CSS support and performance.
 * This renderer provides:
 * - Superior formatting fidelity
 * - Better performance for complex content
 * - Full CSS and JavaScript support
 * - Responsive design capabilities
 * - Better accessibility features
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ImprovedEpubRenderer(
    htmlContent: String,
    readerSettings: ReaderSettings,
    images: Map<String, String> = emptyMap(),
    onScrollChanged: (Int, Int) -> Unit = { _, _ -> },
    onPageReady: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current
    
    // Generate optimized CSS styles
    val cssStyles = remember(readerSettings) {
        generateOptimizedCssStyles(readerSettings)
    }
    
    // Preprocess HTML content efficiently
    val processedHtml = remember(htmlContent, cssStyles, images) {
        preprocessHtmlContentOptimized(htmlContent, cssStyles, images)
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                            onPageReady()
                            
                            // Inject optimized JavaScript for enhanced functionality
                            loadUrl(getOptimizedJavaScript())
                        }
                    }
                    
                    // Optimized WebView settings for EPUB rendering
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        builtInZoomControls = false
                        displayZoomControls = false
                        setSupportZoom(false)
                        
                        // Enhanced text rendering settings
                        textZoom = 100
                        // Note: setRenderPriority is deprecated in API 29+, modern WebView automatically optimizes rendering
                        cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                        
                        // Optimize for reading
                        setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                        allowFileAccess = true
                        allowContentAccess = true
                    }
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
            }
        )
        
        // Loading indicator
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

/**
 * Generate optimized CSS styles for better performance and appearance
 */
private fun generateOptimizedCssStyles(settings: ReaderSettings): String {
    val (backgroundColor, textColor) = getThemeColors(settings.readingMode)
    val textAlign = getTextAlignment(settings.textAlign)
    
    return """
        <style>
            /* CSS Reset and Base Styles */
            * {
                margin: 0;
                padding: 0;
                box-sizing: border-box;
            }
            
            html, body {
                height: 100%;
                font-size: ${settings.fontSize}px;
                line-height: ${settings.lineHeight};
                color: $textColor;
                background-color: $backgroundColor;
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Liberation Serif', serif;
                text-align: $textAlign;
                word-wrap: break-word;
                overflow-wrap: break-word;
                -webkit-text-size-adjust: 100%;
                -webkit-font-smoothing: antialiased;
                -moz-osx-font-smoothing: grayscale;
                hyphens: auto;
                -webkit-hyphens: auto;
                -moz-hyphens: auto;
            }
            
            body {
                margin: ${settings.marginVertical}px ${settings.marginHorizontal}px;
                padding: 0;
                max-width: 100%;
            }
            
            /* Typography Hierarchy */
            h1, h2, h3, h4, h5, h6 {
                font-weight: bold;
                margin: 1.5em 0 0.75em 0;
                line-height: 1.3;
                color: $textColor;
                page-break-after: avoid;
            }
            
            h1 { font-size: 1.8em; margin-top: 0; }
            h2 { font-size: 1.6em; }
            h3 { font-size: 1.4em; }
            h4 { font-size: 1.2em; }
            h5 { font-size: 1.1em; }
            h6 { font-size: 1.0em; }
            
            /* Paragraph and Text */
            p {
                margin: 0.8em 0;
                text-indent: ${if (settings.textAlign == TextAlignment.JUSTIFY) "1.2em" else "0"};
                orphans: 2;
                widows: 2;
            }
            
            p:first-child {
                text-indent: 0;
            }
            
            /* Lists */
            ul, ol {
                margin: 0.8em 0;
                padding-left: 2em;
            }
            
            li {
                margin: 0.3em 0;
                page-break-inside: avoid;
            }
            
            /* Text Formatting */
            strong, b {
                font-weight: bold;
            }
            
            em, i {
                font-style: italic;
            }
            
            u {
                text-decoration: underline;
            }
            
            small {
                font-size: 0.85em;
            }
            
            /* Links */
            a {
                color: ${if (settings.readingMode == ReadingMode.DARK) "#64B5F6" else "#007AFF"};
                text-decoration: none;
                word-break: break-word;
            }
            
            a:visited {
                color: ${if (settings.readingMode == ReadingMode.DARK) "#B39DDB" else "#5856D6"};
            }
            
            a:hover, a:focus {
                text-decoration: underline;
            }
            
            /* Images */
            img {
                max-width: 100%;
                height: auto;
                display: block;
                margin: 1em auto;
                border-radius: 4px;
                page-break-inside: avoid;
                ${if (settings.readingMode == ReadingMode.DARK) "opacity: 0.9;" else ""}
            }
            
            /* Figures and Captions */
            figure {
                margin: 1em 0;
                page-break-inside: avoid;
            }
            
            figcaption {
                font-size: 0.9em;
                font-style: italic;
                text-align: center;
                margin-top: 0.5em;
                color: ${if (settings.readingMode == ReadingMode.DARK) "#B0B0B0" else "#666"};
            }
            
            /* Blockquotes */
            blockquote {
                margin: 1em 2em;
                padding: 0.75em 1em;
                border-left: 4px solid ${if (settings.readingMode == ReadingMode.DARK) "#64B5F6" else "#007AFF"};
                background-color: ${getBlockquoteBackground(settings.readingMode)};
                font-style: italic;
                page-break-inside: avoid;
            }
            
            /* Code and Preformatted Text */
            code {
                font-family: 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', Consolas, 'Courier New', monospace;
                background-color: ${getCodeBackground(settings.readingMode)};
                padding: 0.2em 0.4em;
                border-radius: 3px;
                font-size: 0.9em;
                word-break: break-word;
            }
            
            pre {
                background-color: ${getCodeBackground(settings.readingMode)};
                padding: 1em;
                border-radius: 6px;
                overflow-x: auto;
                font-family: 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', Consolas, 'Courier New', monospace;
                font-size: 0.9em;
                line-height: 1.4;
                margin: 1em 0;
                page-break-inside: avoid;
            }
            
            pre code {
                background: none;
                padding: 0;
                border-radius: 0;
            }
            
            /* Tables */
            table {
                width: 100%;
                border-collapse: collapse;
                margin: 1em 0;
                font-size: 0.95em;
                page-break-inside: avoid;
            }
            
            th, td {
                border: 1px solid ${getBorderColor(settings.readingMode)};
                padding: 0.75em 0.5em;
                text-align: left;
                vertical-align: top;
            }
            
            th {
                background-color: ${getTableHeaderBackground(settings.readingMode)};
                font-weight: bold;
            }
            
            /* Horizontal Rules */
            hr {
                border: none;
                border-top: 1px solid ${getBorderColor(settings.readingMode)};
                margin: 2em 0;
                page-break-after: avoid;
            }
            
            /* Responsive Design */
            @media (max-width: 600px) {
                body {
                    font-size: ${(settings.fontSize * 0.95).toInt()}px;
                    margin: ${(settings.marginVertical * 0.8).toInt()}px ${(settings.marginHorizontal * 0.7).toInt()}px;
                }
                
                h1 { font-size: 1.6em; }
                h2 { font-size: 1.4em; }
                h3 { font-size: 1.2em; }
                
                blockquote {
                    margin: 1em 0.5em;
                }
                
                table {
                    font-size: 0.9em;
                }
                
                th, td {
                    padding: 0.5em 0.3em;
                }
            }
            
            /* Print Styles */
            @media print {
                body {
                    background: white;
                    color: black;
                    font-size: 12pt;
                    line-height: 1.5;
                }
                
                a {
                    color: black;
                    text-decoration: underline;
                }
                
                img {
                    max-width: 100%;
                    page-break-inside: avoid;
                }
                
                h1, h2, h3, h4, h5, h6 {
                    page-break-after: avoid;
                }
                
                p, li {
                    orphans: 3;
                    widows: 3;
                }
            }
            
            /* Accessibility Enhancements */
            @media (prefers-reduced-motion: reduce) {
                *, *::before, *::after {
                    animation-duration: 0.01ms !important;
                    animation-iteration-count: 1 !important;
                    transition-duration: 0.01ms !important;
                    scroll-behavior: auto !important;
                }
            }
            
            /* High Contrast Mode */
            @media (prefers-contrast: high) {
                body {
                    background: ${if (settings.readingMode == ReadingMode.DARK) "black" else "white"};
                    color: ${if (settings.readingMode == ReadingMode.DARK) "white" else "black"};
                }
                
                a {
                    color: ${if (settings.readingMode == ReadingMode.DARK) "yellow" else "blue"};
                }
                
                code, pre {
                    background: ${if (settings.readingMode == ReadingMode.DARK) "#333" else "#f0f0f0"};
                    border: 1px solid ${if (settings.readingMode == ReadingMode.DARK) "white" else "black"};
                }
            }
            
            /* Smooth Scrolling */
            html {
                scroll-behavior: smooth;
            }
            
            /* Text Selection */
            ::selection {
                background: ${if (settings.readingMode == ReadingMode.DARK) "#0066CC" else "#B3D4FC"};
                color: ${if (settings.readingMode == ReadingMode.DARK) "white" else "black"};
            }
            
            /* Focus Styles for Accessibility */
            a:focus, button:focus, input:focus, textarea:focus, select:focus {
                outline: 2px solid ${if (settings.readingMode == ReadingMode.DARK) "#64B5F6" else "#007AFF"};
                outline-offset: 2px;
            }
        </style>
    """.trimIndent()
}

/**
 * Preprocess HTML content with optimizations
 */
private fun preprocessHtmlContentOptimized(
    htmlContent: String,
    cssStyles: String,
    images: Map<String, String>
): String {
    var processedContent = htmlContent
    
    // Replace image sources efficiently
    images.forEach { (originalSrc, imagePath) ->
        processedContent = processedContent.replace(
            "src=\"$originalSrc\"",
            "src=\"file://$imagePath\"",
            ignoreCase = true
        )
    }
    
    // Clean up HTML structure
    processedContent = processedContent
        .replace(Regex("<script[^>]*>.*?</script>", RegexOption.DOT_MATCHES_ALL), "")
        .replace(Regex("<style[^>]*>.*?</style>", RegexOption.DOT_MATCHES_ALL), "")
    
    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
            <meta name="apple-mobile-web-app-capable" content="yes">
            <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
            <title>EPUB Content</title>
            $cssStyles
        </head>
        <body>
            $processedContent
        </body>
        </html>
    """.trimIndent()
}

/**
 * Get optimized JavaScript for enhanced functionality
 */
private fun getOptimizedJavaScript(): String {
    return """
        javascript:(function() {
            // Performance optimizations
            document.addEventListener('DOMContentLoaded', function() {
                // Lazy load images
                const images = document.querySelectorAll('img');
                if ('IntersectionObserver' in window) {
                    const imageObserver = new IntersectionObserver((entries, observer) => {
                        entries.forEach(entry => {
                            if (entry.isIntersecting) {
                                const img = entry.target;
                                if (img.dataset.src) {
                                    img.src = img.dataset.src;
                                    img.removeAttribute('data-src');
                                    imageObserver.unobserve(img);
                                }
                            }
                        });
                    });
                    
                    images.forEach(img => imageObserver.observe(img));
                }
                
                // Smooth scrolling for internal links
                const internalLinks = document.querySelectorAll('a[href^="#"]');
                internalLinks.forEach(link => {
                    link.addEventListener('click', function(e) {
                        e.preventDefault();
                        const target = document.querySelector(this.getAttribute('href'));
                        if (target) {
                            target.scrollIntoView({ behavior: 'smooth' });
                        }
                    });
                });
                
                // Optimize text rendering
                document.body.style.textRendering = 'optimizeLegibility';
                document.body.style.fontKerning = 'normal';
                document.body.style.fontVariantLigatures = 'common-ligatures';
            });
            
            // Memory cleanup
            window.addEventListener('beforeunload', function() {
                const images = document.querySelectorAll('img');
                images.forEach(img => {
                    img.src = '';
                    img.removeAttribute('src');
                });
            });
        })();
    """.trimIndent()
}

// Helper functions for theme colors
private fun getThemeColors(readingMode: ReadingMode): Pair<String, String> {
    return when (readingMode) {
        ReadingMode.LIGHT -> "#FFFFFF" to "#000000"
        ReadingMode.DARK -> "#121212" to "#E0E0E0"
        ReadingMode.SEPIA -> "#F4ECD8" to "#5C4B37"
        ReadingMode.BLACK -> "#000000" to "#FFFFFF"
        ReadingMode.HIGH_CONTRAST_LIGHT -> "#FFFFFF" to "#000000"
        ReadingMode.HIGH_CONTRAST_DARK -> "#000000" to "#FFFFFF"
        ReadingMode.HIGH_CONTRAST_YELLOW -> "#FFFF00" to "#000000"
    }
}

private fun getTextAlignment(textAlign: TextAlignment): String {
    return when (textAlign) {
        TextAlignment.LEFT -> "left"
        TextAlignment.CENTER -> "center"
        TextAlignment.JUSTIFY -> "justify"
    }
}

private fun getBlockquoteBackground(readingMode: ReadingMode): String {
    return when (readingMode) {
        ReadingMode.DARK, ReadingMode.BLACK -> "#1E1E1E"
        else -> "#F8F9FA"
    }
}

private fun getCodeBackground(readingMode: ReadingMode): String {
    return when (readingMode) {
        ReadingMode.DARK, ReadingMode.BLACK -> "#2D2D2D"
        else -> "#F5F5F5"
    }
}

private fun getBorderColor(readingMode: ReadingMode): String {
    return when (readingMode) {
        ReadingMode.DARK, ReadingMode.BLACK -> "#444"
        else -> "#DDD"
    }
}

private fun getTableHeaderBackground(readingMode: ReadingMode): String {
    return when (readingMode) {
        ReadingMode.DARK, ReadingMode.BLACK -> "#333"
        else -> "#F5F5F5"
    }
}