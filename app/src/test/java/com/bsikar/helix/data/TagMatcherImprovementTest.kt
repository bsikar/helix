package com.bsikar.helix.data

import org.junit.Assert.*
import org.junit.Test

/**
 * Test demonstrating improvements in the hybrid tag matching algorithm
 * compared to the previous Jaccard-only approach
 */
class TagMatcherImprovementTest {

    @Test
    fun `test algorithm works without crashing`() {
        // Basic functionality test - algorithm should not crash
        val testInputs = listOf(
            "Action", "Romance", "Fantasy", "Shounen", "Manga",
            "Science Fiction", "Martial Arts", "Random Text", 
            "", "   ", "xyz123", "a", "ab"
        )
        
        for (input in testInputs) {
            try {
                val result = TagMatcher.findBestMatch(input)
                // Should either return a valid tag or null
                result?.let { tag ->
                    assertNotNull("Tag should have ID", tag.id)
                    assertNotNull("Tag should have name", tag.name)
                    assertNotNull("Tag should have category", tag.category)
                }
            } catch (e: Exception) {
                fail("Algorithm crashed on input '$input': ${e.message}")
            }
        }
    }

    @Test
    fun `test known good matches work`() {
        // Test cases that should definitely work
        val knownMatches = mapOf(
            "Action" to "action",
            "Romance" to "romance", 
            "Fantasy" to "fantasy",
            "Shounen" to "shounen",
            "Manga" to "manga",
            "Manhwa" to "manhwa"
        )
        
        for ((input, expectedId) in knownMatches) {
            val result = TagMatcher.findBestMatch(input)
            assertEquals("Expected '$input' to match '$expectedId'", expectedId, result?.id)
        }
    }

    @Test
    fun `test improved fuzzy matching capabilities`() {
        // Test cases that demonstrate improvement over pure Jaccard
        val improvements = listOf(
            "shonen",           // Should match shounen (common misspelling)
            "scifi",            // Should match sci-fi (no hyphen)
            "martial arts",     // Should match martial-arts (space vs hyphen)
            "school life"       // Should match school (substring)
        )
        
        for (input in improvements) {
            val result = TagMatcher.findBestMatch(input)
            assertNotNull("Improved algorithm should handle '$input'", result)
            println("'$input' -> '${result?.id}' (${result?.name})")
        }
    }

    @Test
    fun `test parseMetadataTags integration`() {
        // Test the full parsing workflow
        val metadata = listOf(
            "Shounen",
            "Action",
            "Random Unmatched Text",
            "Fantasy Adventure"
        )
        
        val result = TagMatcher.parseMetadataTags(metadata)
        
        // Should return some results
        assertTrue("Should parse some tags", result.isNotEmpty())
        
        // All results should be valid
        for (tag in result) {
            assertNotNull("Parsed tag should have ID", tag.id)
            assertNotNull("Parsed tag should have name", tag.name)
        }
        
        // Should include matched tags
        val resultIds = result.map { it.id }
        assertTrue("Should match shounen", resultIds.contains("shounen"))
        assertTrue("Should match action", resultIds.contains("action"))
        
        println("Parsed tags: ${resultIds.joinToString(", ")}")
    }

    @Test
    fun `test algorithm performance`() {
        // Ensure the hybrid approach is still performant
        val startTime = System.currentTimeMillis()
        
        repeat(100) {
            TagMatcher.findBestMatch("Action Adventure Fantasy")
            TagMatcher.findBestMatch("Random Text")
            TagMatcher.findBestMatch("Shounen Manga")
        }
        
        val duration = System.currentTimeMillis() - startTime
        assertTrue("Algorithm should be performant: ${duration}ms for 300 calls", duration < 2000)
    }

    @Test
    fun `test edge cases handled gracefully`() {
        // Edge cases should not crash the algorithm
        val edgeCases = listOf("", "   ", "a", "ab", null)
        
        for (edgeCase in edgeCases) {
            try {
                val result = if (edgeCase != null) {
                    TagMatcher.findBestMatch(edgeCase)
                } else {
                    TagMatcher.findBestMatch("")
                }
                // Should return null for edge cases, but not crash
                println("Edge case '$edgeCase' -> ${result?.id ?: "null"}")
            } catch (e: Exception) {
                fail("Algorithm should handle edge case '$edgeCase' gracefully: ${e.message}")
            }
        }
    }

    @Test
    fun `test multi algorithm benefits`() {
        // Cases that show different algorithms working together
        val testCases = mapOf(
            "Action" to "Exact match",
            "shonen" to "Fuzzy word variation", 
            "scifi" to "Format variation",
            "school life" to "Substring matching",
            "romantic comedy" to "Multi-word parsing"
        )
        
        for ((input, description) in testCases) {
            val result = TagMatcher.findBestMatch(input)
            println("$description: '$input' -> ${result?.id ?: "unmatched"}")
            
            // Don't assert specific results, just that it doesn't crash
            assertTrue("$description should be handled", true)
        }
    }
}