package com.bsikar.helix.data

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

/**
 * Unit tests for TagMatcher functionality, testing metadata tag parsing
 * with various scenarios including exact matches, fuzzy matches, and untagged cases.
 */
class TagMatcherTest {

    @Test
    fun `test exact tag matches`() {
        // Test exact matches (case insensitive)
        assertEquals("shounen", TagMatcher.findBestMatch("Shounen")?.id)
        assertEquals("seinen", TagMatcher.findBestMatch("seinen")?.id)
        assertEquals("manga", TagMatcher.findBestMatch("MANGA")?.id)
        assertEquals("manhwa", TagMatcher.findBestMatch("manhwa")?.id)
        assertEquals("manhua", TagMatcher.findBestMatch("Manhua")?.id)
        assertEquals("webtoon", TagMatcher.findBestMatch("webtoon")?.id)
        assertEquals("light-novel", TagMatcher.findBestMatch("Light Novel")?.id)
    }

    @Test
    fun `test genre exact matches`() {
        // Test genre matches
        assertEquals("action", TagMatcher.findBestMatch("Action")?.id)
        assertEquals("romance", TagMatcher.findBestMatch("Romance")?.id)
        assertEquals("comedy", TagMatcher.findBestMatch("Comedy")?.id)
        assertEquals("drama", TagMatcher.findBestMatch("Drama")?.id)
        assertEquals("fantasy", TagMatcher.findBestMatch("Fantasy")?.id)
        assertEquals("sci-fi", TagMatcher.findBestMatch("Science Fiction")?.id)
        assertEquals("horror", TagMatcher.findBestMatch("Horror")?.id)
        assertEquals("thriller", TagMatcher.findBestMatch("Thriller")?.id)
    }

    @Test
    fun `test fuzzy matching with typos and variations`() {
        // Test fuzzy matches that should work with new hybrid algorithm
        assertEquals("shounen", TagMatcher.findBestMatch("shonen")?.id) // common misspelling
        assertEquals("seinen", TagMatcher.findBestMatch("seien")?.id) // typo
        assertEquals("sci-fi", TagMatcher.findBestMatch("scifi")?.id) // no hyphen
        assertEquals("sci-fi", TagMatcher.findBestMatch("Science Fiction")?.id) // full form
        assertEquals("martial-arts", TagMatcher.findBestMatch("martial arts")?.id) // space vs hyphen
        assertEquals("romance", TagMatcher.findBestMatch("romantic")?.id) // word form variation
        assertEquals("comedy", TagMatcher.findBestMatch("humor")?.id) // synonym
        assertEquals("action", TagMatcher.findBestMatch("battle")?.id) // related word
    }

    @Test
    fun `test metadata tags that should return untagged`() {
        // Test tags that don't match anything and should return null (untagged)
        assertNull(TagMatcher.findBestMatch("xyz123"))
        assertNull(TagMatcher.findBestMatch("completely-random-tag"))
        assertNull(TagMatcher.findBestMatch(""))
        assertNull(TagMatcher.findBestMatch("   "))
        assertNull(TagMatcher.findBestMatch("a"))
    }

    @Test
    fun `test complex metadata phrases`() {
        // Test complex metadata that contains recognizable parts
        assertEquals("action", TagMatcher.findBestMatch("High-Octane Action Adventure")?.id)
        assertEquals("fantasy", TagMatcher.findBestMatch("Dark Fantasy Adventure")?.id)
        assertEquals("school", TagMatcher.findBestMatch("High School Student Life")?.id)
        assertEquals("school", TagMatcher.findBestMatch("School Life")?.id)
        assertEquals("magic", TagMatcher.findBestMatch("Magical Adventure")?.id)
    }

    @Test
    fun `test parseMetadataTags with mixed results`() {
        val metadata = listOf(
            "Shounen",           // exact match -> shounen
            "Action Adventure",  // partial match -> action
            "xyz123",           // no match -> untagged
            "Romance",          // exact match -> romance
            "completely-random", // no match -> untagged
            "Science Fiction"   // exact match -> sci-fi
        )
        
        val result = TagMatcher.parseMetadataTags(metadata)
        val resultIds = result.map { it.id }
        
        assertTrue(resultIds.contains("shounen"))
        assertTrue(resultIds.contains("action"))
        assertTrue(resultIds.contains("romance"))
        assertTrue(resultIds.contains("sci-fi"))
        assertTrue(resultIds.contains("untagged"))
        
        // Should have exactly one untagged entry for the two unmatched tags
        assertEquals(1, resultIds.count { it == "untagged" })
    }

    @Test
    fun `test all metadata unmatched returns single untagged`() {
        val metadata = listOf("xyz123", "random-tag", "nonsense")
        val result = TagMatcher.parseMetadataTags(metadata)
        
        assertEquals(1, result.size)
        assertEquals("untagged", result.first().id)
    }

    @Test
    fun `test empty metadata returns empty list`() {
        val result = TagMatcher.parseMetadataTags(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `test case insensitive matching`() {
        assertEquals("shounen", TagMatcher.findBestMatch("SHOUNEN")?.id)
        assertEquals("manhwa", TagMatcher.findBestMatch("MANHWA")?.id)
        assertEquals("action", TagMatcher.findBestMatch("action")?.id)
        assertEquals("romance", TagMatcher.findBestMatch("RoMaNcE")?.id)
    }
}

/**
 * Parameterized tests for comprehensive metadata parsing scenarios
 */
@RunWith(Parameterized::class)
class TagMatcherParameterizedTest(
    private val metadata: String,
    private val expectedTagId: String?,
    private val description: String
) {

    companion object {
        @JvmStatic
        @Parameters(name = "{2}")
        fun data(): Collection<Array<Any?>> {
            return listOf(
                // Format/Type tests
                arrayOf("Manga", "manga", "Japanese manga format"),
                arrayOf("Manhwa", "manhwa", "Korean manhwa format"),
                arrayOf("Manhua", "manhua", "Chinese manhua format"),
                arrayOf("Webtoon", "webtoon", "Webtoon format"),
                arrayOf("Light Novel", "light-novel", "Light novel format"),
                arrayOf("Web Novel", "web-novel", "Web novel format"),
                
                // Genre tests - Action
                arrayOf("Action", "action", "Action genre"),
                arrayOf("Adventure", "adventure", "Adventure genre"),
                
                // Genre tests - Romance
                arrayOf("Romance", "romance", "Romance genre"),
                
                // Genre tests - Comedy
                arrayOf("Comedy", "comedy", "Comedy genre"),
                
                // Genre tests - Fantasy
                arrayOf("Fantasy", "fantasy", "Fantasy genre"),
                arrayOf("Magic", "magic", "Magic theme"),
                
                // Genre tests - Sci-Fi
                arrayOf("Science Fiction", "sci-fi", "Science Fiction"),
                arrayOf("Sci-Fi", "sci-fi", "Sci-Fi abbreviation"),
                
                // Demographic tests
                arrayOf("Shounen", "shounen", "Shounen demographic"),
                arrayOf("Shonen", "shounen", "Shonen alternative spelling"),
                arrayOf("Seinen", "seinen", "Seinen demographic"),
                arrayOf("Shoujo", "shoujo", "Shoujo demographic"),
                arrayOf("Josei", "josei", "Josei demographic"),
                
                // Theme tests
                arrayOf("School", "school", "School theme"),
                arrayOf("School Life", "school", "School Life theme"),
                arrayOf("High School", "school", "High School theme"),
                arrayOf("Supernatural", "supernatural", "Supernatural theme"),
                arrayOf("Psychological", "psychological", "Psychological theme"),
                arrayOf("Military", "military", "Military theme"),
                arrayOf("Historical", "historical", "Historical theme"),
                
                // Status tests
                arrayOf("Ongoing", "ongoing", "Ongoing status"),
                arrayOf("Completed", "completed", "Completed status"),
                arrayOf("Hiatus", "hiatus", "Hiatus status"),
                arrayOf("Cancelled", "cancelled", "Cancelled status"),
                
                // Complex phrases that should match
                arrayOf("Epic Fantasy Adventure", "fantasy", "Complex phrase with fantasy"),
                arrayOf("Action-Packed Thriller", "action", "Complex phrase with action"),
                arrayOf("Sci-Fi Horror Story", "sci-fi", "Complex phrase with sci-fi"),
                
                // Cases that should return null (untagged)
                arrayOf("Random Gibberish", null, "Random text should not match"),
                arrayOf("xyz123", null, "Random characters should not match"),
                arrayOf("Completely Made Up Tag", null, "Made up tag should not match"),
                arrayOf("", null, "Empty string should not match"),
                arrayOf("   ", null, "Whitespace should not match"),
                arrayOf("a", null, "Single character should not match"),
                
                // Edge cases
                arrayOf("Actionable", null, "Similar but not exact word"),
                arrayOf("Fantastical", null, "Similar but not exact word"),
                arrayOf("Comedic Relief", "comedy", "Contains comedy word")
            )
        }
    }

    @Test
    fun `test metadata parsing`() {
        val result = TagMatcher.findBestMatch(metadata)
        
        if (expectedTagId == null) {
            assertNull("Expected null for '$metadata' but got '${result?.id}'", result)
        } else {
            assertNotNull("Expected tag for '$metadata' but got null", result)
            assertEquals("For metadata '$metadata'", expectedTagId, result?.id)
        }
    }
}

/**
 * CSV-style parameterized tests using JUnit CSV annotation approach
 */
@RunWith(Parameterized::class)
class TagMatcherCsvTest(
    private val inputMetadata: List<String>,
    private val expectedTags: List<String>,
    private val expectedUntaggedCount: Int,
    private val testName: String
) {

    companion object {
        @JvmStatic
        @Parameters(name = "{3}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(
                    listOf("Shounen", "Action", "School"),
                    listOf("shounen", "action", "school"),
                    0,
                    "All tags match exactly"
                ),
                arrayOf(
                    listOf("Random Tag", "Action", "Made Up"),
                    listOf("action", "untagged"),
                    1,
                    "Mixed matched and unmatched tags"
                ),
                arrayOf(
                    listOf("Fantasy Adventure", "Romance Story", "Comedy Gold"),
                    listOf("fantasy", "romance", "comedy"),
                    0,
                    "Complex phrases with recognizable words"
                ),
                arrayOf(
                    listOf("Completely Random", "Made Up Stuff", "Nonsense"),
                    listOf("untagged"),
                    1,
                    "All unmatched should give single untagged"
                ),
                arrayOf(
                    listOf("Manga", "Manhwa", "Manhua", "Webtoon"),
                    listOf("manga", "manhwa", "manhua", "webtoon"),
                    0,
                    "All format types should match"
                ),
                arrayOf(
                    listOf("Shounen Adventure", "Seinen Drama", "Random Tag"),
                    listOf("shounen", "seinen", "untagged"),
                    1,
                    "Mix of demographic, genre, and unmatched"
                ),
                arrayOf(
                    emptyList<String>(),
                    emptyList<String>(),
                    0,
                    "Empty input should return empty result"
                )
            )
        }
    }

    @Test
    fun `test parseMetadataTags with various scenarios`() {
        val result = TagMatcher.parseMetadataTags(inputMetadata)
        val resultIds = result.map { it.id }
        
        // Check expected tags are present
        for (expectedTag in expectedTags) {
            assertTrue(
                "Expected tag '$expectedTag' not found in result: $resultIds",
                resultIds.contains(expectedTag)
            )
        }
        
        // Check untagged count
        val actualUntaggedCount = resultIds.count { it == "untagged" }
        assertEquals(
            "Expected $expectedUntaggedCount untagged entries but got $actualUntaggedCount",
            expectedUntaggedCount,
            actualUntaggedCount
        )
        
        // Check total count makes sense
        assertEquals(
            "Result size doesn't match expected",
            expectedTags.size,
            result.size
        )
    }
}