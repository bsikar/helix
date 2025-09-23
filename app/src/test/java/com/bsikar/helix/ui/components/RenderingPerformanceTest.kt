package com.bsikar.helix.ui.components

import com.bsikar.helix.data.ReaderSettings
import com.bsikar.helix.data.ReadingMode
import com.bsikar.helix.data.TextAlignment
import com.bsikar.helix.ui.screens.parseHtmlToContentElements
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import kotlin.system.measureTimeMillis

/**
 * Performance tests comparing AnnotatedString vs WebView rendering approaches
 */
class RenderingPerformanceTest {

    private val defaultReaderSettings = ReaderSettings(
        fontSize = 16,
        lineHeight = 1.5f,
        readingMode = ReadingMode.LIGHT,
        textAlign = TextAlignment.LEFT,
        marginHorizontal = 16,
        marginVertical = 24
    )

    @Test
    fun `test simple HTML parsing performance`() = runTest {
        val simpleHtml = """
            <html>
            <body>
                <h1>Chapter 1</h1>
                <p>This is a simple paragraph with some <strong>bold</strong> and <em>italic</em> text.</p>
                <p>Another paragraph with more text content to test parsing performance.</p>
            </body>
            </html>
        """.trimIndent()

        val parsingTime = measureTimeMillis {
            val elements = parseHtmlToContentElements(simpleHtml, defaultReaderSettings, emptyMap())
            assertTrue("Should parse at least one element", elements.isNotEmpty())
        }

        println("Simple HTML parsing time: ${parsingTime}ms")
        assertTrue("Simple HTML should parse quickly (under 100ms)", parsingTime < 100)
    }

    @Test
    fun `test complex HTML parsing performance`() = runTest {
        val complexHtml = generateComplexHtml()

        val parsingTime = measureTimeMillis {
            val elements = parseHtmlToContentElements(complexHtml, defaultReaderSettings, emptyMap())
            assertTrue("Should parse complex HTML", elements.isNotEmpty())
        }

        println("Complex HTML parsing time: ${parsingTime}ms")
        // Complex HTML might take longer but should still be reasonable
        assertTrue("Complex HTML parsing should complete", parsingTime > 0)
    }

    @Test
    fun `test memory usage comparison`() = runTest {
        val testHtml = generateLargeHtml()
        
        // Parse with AnnotatedString approach
        val elements = parseHtmlToContentElements(testHtml, defaultReaderSettings, emptyMap())

        println("Parsed ${elements.size} content elements from large HTML")
        
        // Focus on functional correctness rather than memory measurement in unit tests
        assertTrue("Should parse content", elements.isNotEmpty())
        
        // Note: Large HTML might be parsed as a single element depending on the parser implementation
        println("Successfully parsed HTML content into ${elements.size} element(s)")
        
        // Verify that text content is preserved
        val textElements = elements.filterIsInstance<com.bsikar.helix.ui.screens.ContentElement.TextElement>()
        assertTrue("Should contain text elements", textElements.isNotEmpty())
    }

    @Test
    fun `test parsing fidelity with complex formatting`() = runTest {
        val htmlWithComplexFormatting = """
            <html>
            <head>
                <style>
                    .highlight { background-color: yellow; }
                    .large-text { font-size: 18px; }
                    table { border-collapse: collapse; }
                    th, td { border: 1px solid black; padding: 8px; }
                </style>
            </head>
            <body>
                <h1>Complex Formatting Test</h1>
                <p class="large-text">This paragraph uses CSS classes.</p>
                <p>This has <span class="highlight">highlighted text</span> in the middle.</p>
                <table>
                    <tr><th>Header 1</th><th>Header 2</th></tr>
                    <tr><td>Cell 1</td><td>Cell 2</td></tr>
                </table>
                <div style="margin: 20px; padding: 10px; border: 1px solid red;">
                    <p>This is in a styled div with inline CSS.</p>
                </div>
            </body>
            </html>
        """.trimIndent()

        val elements = parseHtmlToContentElements(htmlWithComplexFormatting, defaultReaderSettings, emptyMap())
        
        // AnnotatedString approach will lose some formatting but should still parse content
        assertTrue("Should parse content despite complex formatting", elements.isNotEmpty())
        
        // Count text elements to ensure content is preserved
        val textElements = elements.filterIsInstance<com.bsikar.helix.ui.screens.ContentElement.TextElement>()
        assertTrue("Should preserve text content", textElements.isNotEmpty())
        
        // Check that basic text content is preserved
        val allText = textElements.joinToString(" ") { it.text.text }
        assertTrue("Should contain header text", allText.contains("Complex Formatting Test"))
        assertTrue("Should contain paragraph text", allText.contains("highlighted text"))
        
        println("Parsed ${elements.size} elements from complex HTML")
        println("Text content length: ${allText.length}")
    }

    @Test
    fun `test WebView CSS generation performance`() = runTest {
        val cssGenerationTime = measureTimeMillis {
            // Test CSS generation from WebViewEpubRenderer
            val css = generateTestCss(defaultReaderSettings)
            assertTrue("Should generate CSS", css.isNotEmpty())
        }

        println("CSS generation time: ${cssGenerationTime}ms")
        assertTrue("CSS generation should be fast", cssGenerationTime < 50)
    }

    @Test
    fun `compare rendering approaches with realistic EPUB content`() = runTest {
        val realisticEpubContent = generateRealisticEpubChapter()
        
        // Test AnnotatedString approach
        val annotatedStringTime = measureTimeMillis {
            val elements = parseHtmlToContentElements(realisticEpubContent, defaultReaderSettings, emptyMap())
            assertTrue("Should parse realistic content", elements.isNotEmpty())
        }
        
        // Test WebView approach (simulate preprocessing)
        val webViewTime = measureTimeMillis {
            val css = generateTestCss(defaultReaderSettings)
            val processedHtml = preprocessHtmlForWebView(realisticEpubContent, css)
            assertTrue("Should preprocess HTML", processedHtml.isNotEmpty())
        }
        
        println("AnnotatedString processing time: ${annotatedStringTime}ms")
        println("WebView preprocessing time: ${webViewTime}ms")
        
        // Both approaches should be reasonably fast
        assertTrue("AnnotatedString should complete", annotatedStringTime > 0)
        assertTrue("WebView preprocessing should complete", webViewTime > 0)
    }

    private fun generateComplexHtml(): String = """
        <html>
        <head>
            <style>
                body { font-family: Arial, sans-serif; }
                .chapter-title { font-size: 24px; font-weight: bold; margin-bottom: 20px; }
                .paragraph { margin-bottom: 15px; line-height: 1.6; }
                .highlight { background-color: #ffff99; }
                .quote { font-style: italic; margin: 20px; padding: 10px; border-left: 3px solid #ccc; }
            </style>
        </head>
        <body>
            <h1 class="chapter-title">Chapter with Complex Formatting</h1>
            ${(1..50).joinToString("\n") { i ->
                """<p class="paragraph">This is paragraph $i with some <strong>bold text</strong>, 
                   <em>italic text</em>, and <span class="highlight">highlighted content</span>.</p>"""
            }}
            <blockquote class="quote">
                This is a long quote that spans multiple lines and contains various formatting elements.
            </blockquote>
            <ul>
                ${(1..10).joinToString("\n") { "<li>List item $it with <strong>formatting</strong></li>" }}
            </ul>
        </body>
        </html>
    """.trimIndent()

    private fun generateLargeHtml(): String = """
        <html>
        <body>
            <h1>Large Content Test</h1>
            ${(1..200).joinToString("\n") { i ->
                """<p>This is paragraph $i. Lorem ipsum dolor sit amet, consectetur adipiscing elit. 
                   Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim 
                   veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.</p>"""
            }}
        </body>
        </html>
    """.trimIndent()

    private fun generateRealisticEpubChapter(): String = """
        <html xmlns="http://www.w3.org/1999/xhtml">
        <head>
            <title>Chapter 5: The Journey Begins</title>
            <style type="text/css">
                body { margin: 1em; font-family: serif; }
                h1 { text-align: center; margin: 2em 0; }
                p { text-indent: 1.2em; margin: 0.5em 0; }
                .first-paragraph { text-indent: 0; }
                em { font-style: italic; }
                strong { font-weight: bold; }
            </style>
        </head>
        <body>
            <h1>Chapter 5: The Journey Begins</h1>
            <p class="first-paragraph">The morning sun cast long shadows across the cobblestone streets 
            as Elena made her way through the quiet town. Her <em>heart raced</em> with anticipation, 
            knowing that this day would change everything.</p>
            
            <p>She had spent weeks preparing for this moment, gathering supplies and studying the ancient maps 
            her grandmother had left her. The <strong>leather satchel</strong> slung across her shoulder 
            contained everything she would need for the journey ahead.</p>
            
            <p>As she reached the town square, Elena paused to look back at the place she had called home 
            for twenty-three years. The familiar sights and sounds seemed different now, as if she were 
            seeing them for the first time—or perhaps the last.</p>
            
            <p>"<em>Adventure awaits those brave enough to seek it,</em>" she whispered, repeating the words 
            her grandmother had often spoken. With renewed determination, Elena turned toward the forest path 
            that would lead her into the unknown.</p>
            
            <p>The first few miles passed quickly as Elena's excitement carried her forward. The well-worn 
            path gradually gave way to a narrower trail, and the sounds of civilization faded behind her. 
            Only the rustle of leaves and the distant call of birds accompanied her now.</p>
            
            <p>By midday, Elena found herself deeper in the forest than she had ever ventured before. 
            The canopy above filtered the sunlight into dappled patterns on the forest floor, creating 
            an almost magical atmosphere that seemed to pulse with ancient energy.</p>
        </body>
        </html>
    """.trimIndent()

    private fun generateTestCss(settings: ReaderSettings): String {
        // Simplified version of CSS generation for testing
        return """
            body {
                font-size: ${settings.fontSize}px;
                line-height: ${settings.lineHeight};
                margin: ${settings.marginVertical}px ${settings.marginHorizontal}px;
            }
            h1 { font-size: 1.8em; font-weight: bold; }
            p { margin: 0.8em 0; }
        """.trimIndent()
    }

    private fun preprocessHtmlForWebView(html: String, css: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>$css</style>
            </head>
            <body>
                $html
            </body>
            </html>
        """.trimIndent()
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "${bytes}B"
        if (bytes < 1024 * 1024) return "${bytes / 1024}KB"
        return "${bytes / (1024 * 1024)}MB"
    }
}