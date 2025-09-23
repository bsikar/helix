package com.bsikar.helix.ui.components

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.bsikar.helix.data.model.TagMatcher
import kotlin.math.max
import kotlin.math.min

/**
 * Search utilities for fuzzy matching and highlighting
 */
object SearchUtils {
    
    /**
     * Score for fuzzy matching - higher is better match
     */
    data class SearchResult<T>(
        val item: T,
        val score: Double,
        val matchedText: String = ""
    )
    
    /**
     * Fuzzy search with scoring using similar algorithm to TagMatcher
     */
    fun <T> fuzzySearch(
        items: List<T>,
        query: String,
        getText: (T) -> String,
        getSecondaryText: (T) -> String = { "" },
        threshold: Double = 0.3
    ): List<SearchResult<T>> {
        if (query.isBlank()) return items.map { SearchResult(it, 1.0) }
        
        val lowerQuery = query.lowercase().trim()
        
        return items.mapNotNull { item ->
            val primaryText = getText(item).lowercase()
            val secondaryText = getSecondaryText(item).lowercase()
            
            // Prioritize substring matches for predictable search behavior
            val score = when {
                // Exact match
                primaryText == lowerQuery -> 1.0
                secondaryText == lowerQuery -> 0.95
                
                // Contains query (most common user expectation)
                primaryText.contains(lowerQuery) -> {
                    if (primaryText.startsWith(lowerQuery)) 0.9
                    else 0.85
                }
                secondaryText.contains(lowerQuery) -> {
                    if (secondaryText.startsWith(lowerQuery)) 0.8
                    else 0.75
                }
                
                // Word boundaries - query matches start of any word
                primaryText.split(" ").any { it.startsWith(lowerQuery) } -> 0.7
                secondaryText.split(" ").any { it.startsWith(lowerQuery) } -> 0.65
                
                // Only use fuzzy matching for very similar strings
                else -> {
                    val primaryFuzzy = jaroWinklerSimilarity(lowerQuery, primaryText)
                    val secondaryFuzzy = if (secondaryText.isNotBlank()) {
                        jaroWinklerSimilarity(lowerQuery, secondaryText) * 0.8
                    } else 0.0
                    
                    // Only return fuzzy matches if they're very close (0.8+ threshold)
                    val bestFuzzy = kotlin.math.max(primaryFuzzy, secondaryFuzzy)
                    if (bestFuzzy >= 0.8) bestFuzzy else 0.0
                }
            }
            
            if (score >= threshold) {
                SearchResult(item, score, primaryText)
            } else null
        }.sortedByDescending { it.score }
    }
    
    /**
     * Create highlighted text with search term highlighted
     */
    fun createHighlightedText(
        text: String,
        query: String,
        baseColor: Color,
        highlightColor: Color,
        fontSize: TextUnit = 14.sp,
        highlightFontWeight: FontWeight = FontWeight.Bold
    ): AnnotatedString {
        if (query.isBlank()) {
            return AnnotatedString(
                text = text,
                spanStyle = SpanStyle(color = baseColor, fontSize = fontSize)
            )
        }
        
        val lowerText = text.lowercase()
        val lowerQuery = query.lowercase().trim()
        
        return buildAnnotatedString {
            var currentIndex = 0
            var searchIndex = 0
            
            while (searchIndex < lowerText.length) {
                val matchIndex = lowerText.indexOf(lowerQuery, searchIndex)
                if (matchIndex == -1) break
                
                // Add text before match
                if (matchIndex > currentIndex) {
                    withStyle(SpanStyle(color = baseColor, fontSize = fontSize)) {
                        append(text.substring(currentIndex, matchIndex))
                    }
                }
                
                // Add highlighted match
                withStyle(SpanStyle(
                    color = highlightColor,
                    fontWeight = highlightFontWeight,
                    fontSize = fontSize
                )) {
                    append(text.substring(matchIndex, matchIndex + lowerQuery.length))
                }
                
                currentIndex = matchIndex + lowerQuery.length
                searchIndex = currentIndex
            }
            
            // Add remaining text
            if (currentIndex < text.length) {
                withStyle(SpanStyle(color = baseColor, fontSize = fontSize)) {
                    append(text.substring(currentIndex))
                }
            }
        }
    }
    
    /**
     * Calculate Jaro-Winkler similarity
     */
    private fun jaroWinklerSimilarity(s1: String, s2: String, prefixScale: Double = 0.1): Double {
        val jaro = jaroSimilarity(s1, s2)
        if (jaro < 0.7) return jaro
        
        val prefix = s1.zip(s2).takeWhile { it.first == it.second }.size
        return jaro + (min(prefix, 4) * prefixScale * (1 - jaro))
    }
    
    /**
     * Calculate Jaro similarity
     */
    private fun jaroSimilarity(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        
        val len1 = s1.length
        val len2 = s2.length
        val matchWindow = max(len1, len2) / 2 - 1
        
        if (matchWindow < 0) return 0.0
        
        val s1Matches = BooleanArray(len1)
        val s2Matches = BooleanArray(len2)
        
        var matches = 0
        var transpositions = 0
        
        // Find matches
        for (i in 0 until len1) {
            val start = kotlin.math.max(0, i - matchWindow)
            val end = kotlin.math.min(i + matchWindow + 1, len2)
            
            for (j in start until end) {
                if (s2Matches[j] || s1[i] != s2[j]) continue
                s1Matches[i] = true
                s2Matches[j] = true
                matches++
                break
            }
        }
        
        if (matches == 0) return 0.0
        
        // Count transpositions
        var k = 0
        for (i in 0 until len1) {
            if (!s1Matches[i]) continue
            while (!s2Matches[k]) k++
            if (s1[i] != s2[k]) transpositions++
            k++
        }
        
        return (matches.toDouble() / len1 + 
                matches.toDouble() / len2 + 
                (matches - transpositions / 2.0) / matches) / 3.0
    }
    
    /**
     * Calculate Jaccard similarity between two sets
     */
    private fun jaccardSimilarity(set1: Set<String>, set2: Set<String>): Double {
        val intersection = set1.intersect(set2).size
        val union = set1.union(set2).size
        return if (union == 0) 0.0 else intersection.toDouble() / union
    }
    
    /**
     * Tokenize text into words
     */
    private fun tokenize(text: String): Set<String> {
        return text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() }
            .toSet()
    }
}

/**
 * Enhanced search field component with fuzzy search
 */
@Composable
fun <T> SmartSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    items: List<T>,
    getText: (T) -> String,
    getSecondaryText: (T) -> String = { "" },
    placeholder: String = "Search...",
    threshold: Double = 0.3,
    maxResults: Int = 50
): List<SearchUtils.SearchResult<T>> {
    
    // Perform fuzzy search
    val searchResults = SearchUtils.fuzzySearch(
        items = items,
        query = query,
        getText = getText,
        getSecondaryText = getSecondaryText,
        threshold = threshold
    ).take(maxResults)
    
    return searchResults
}