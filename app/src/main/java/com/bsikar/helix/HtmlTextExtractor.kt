package com.bsikar.helix

internal class HtmlTextExtractor {

    fun extractTextFromHtml(htmlContent: String): String {
        return htmlContent
            .replace(Regex("<script[^>]*>.*?</script>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<style[^>]*>.*?</style>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<(p|div|h[1-6]|br)\\s*[^>]*>", RegexOption.IGNORE_CASE), "\n\n")
            .replace(Regex("</(p|div|h[1-6])>", RegexOption.IGNORE_CASE), "\n\n")
            .replace(Regex("<[^>]+>"), " ")
            .replace(Regex("\\n\\s*\\n\\s*"), "\n\n")
            .replace(Regex("[ \\t]+"), " ")
            .let { decodeHtmlEntities(it) }
            .trim()
    }

    fun extractParagraphsFromHtml(htmlContent: String): List<String> {
        val cleanHtml = htmlContent
            .replace(Regex("<script[^>]*>.*?</script>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<style[^>]*>.*?</style>", RegexOption.DOT_MATCHES_ALL), "")

        val paragraphs = mutableListOf<String>()

        val paragraphRegex =
            Regex("<(p|div|h[1-6])[^>]*>(.*?)</\\1>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        val matches = paragraphRegex.findAll(cleanHtml)

        for (match in matches) {
            val content = match.groupValues[2]
                .replace(Regex("<[^>]+>"), " ")
                .replace(Regex("\\s+"), " ")
                .let { decodeHtmlEntities(it) }
                .trim()

            if (content.isNotBlank()) {
                paragraphs.add(content)
            }
        }

        if (paragraphs.isEmpty()) {
            val fallbackText = cleanHtml
                .replace(Regex("<[^>]+>"), " ")
                .replace(Regex("\\s+"), " ")
                .let { decodeHtmlEntities(it) }
                .trim()

            if (fallbackText.isNotBlank()) {
                paragraphs.add(fallbackText)
            }
        }

        return paragraphs
    }

    private fun decodeHtmlEntities(text: String): String {
        return text
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&#8217;", "'")
            .replace(
                "&#8220;",
                """)
            .replace("&#8221;", """
            )
            .replace("&#8230;", "…")
            .replace("&#160;", " ")
    }
}
