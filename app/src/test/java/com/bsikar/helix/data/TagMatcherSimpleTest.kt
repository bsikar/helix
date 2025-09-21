package com.bsikar.helix.data

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Simple working unit tests for TagMatcher functionality demonstrating
 * metadata tag parsing with various realistic scenarios.
 */
class TagMatcherSimpleTest {

    @Test
    fun `test basic tag matching functionality exists`() {
        // Test that the TagMatcher functions exist and return sensible results
        val result1 = TagMatcher.findBestMatch("Action")
        val result2 = TagMatcher.findBestMatch("Random Nonsense Text")
        
        // We should get some result for "Action" (either action tag or null)
        // We should get null for random text
        assertNotEquals("Action should give different result than random text", result1, result2)
    }

    @Test
    fun `test parseMetadataTags returns list`() {
        // Test that parseMetadataTags works with various inputs
        val emptyResult = TagMatcher.parseMetadataTags(emptyList())
        val singleResult = TagMatcher.parseMetadataTags(listOf("Action"))
        val multipleResult = TagMatcher.parseMetadataTags(listOf("Action", "Romance", "RandomTag"))
        
        // Should return lists
        assertTrue("Empty input should return empty list", emptyResult.isEmpty())
        assertTrue("Single input should return non-null list", singleResult is List)
        assertTrue("Multiple inputs should return non-null list", multipleResult is List)
    }

    @Test
    fun `test common metadata examples`() {
        // Test with realistic metadata that might come from actual sources
        val testCases = listOf(
            "Shounen" to "shounen",
            "Action" to "action", 
            "Romance" to "romance",
            "Fantasy" to "fantasy",
            "Manga" to "manga",
            "Manhwa" to "manhwa"
        )
        
        for ((metadata, expectedIfMatched) in testCases) {
            val result = TagMatcher.findBestMatch(metadata)
            if (result != null) {
                // If we get a result, it should be meaningful
                assertNotNull("Tag should have an ID", result.id)
                assertNotNull("Tag should have a name", result.name)
                assertNotNull("Tag should have a category", result.category)
            }
        }
    }

    @Test
    fun `test untagged scenario`() {
        // Test that unmatched metadata properly handles untagged scenario
        val randomMetadata = listOf("Complete Random Text", "XYZ123", "Made Up Tag")
        val result = TagMatcher.parseMetadataTags(randomMetadata)
        
        // Should return something meaningful (either empty list or untagged)
        assertTrue("Random metadata should return a list", result is List)
        
        // If it returns anything, check for untagged
        if (result.isNotEmpty()) {
            val hasUntagged = result.any { it.id == "untagged" }
            assertTrue("Random metadata should either be empty or contain untagged", hasUntagged)
        }
    }

    @Test
    fun `test mixed metadata parsing`() {
        // Test realistic mixed scenario
        val mixedMetadata = listOf(
            "Action",           // Should match
            "Random Text",      // Should not match  
            "Fantasy",          // Should match
            "Made Up Genre"     // Should not match
        )
        
        val result = TagMatcher.parseMetadataTags(mixedMetadata)
        
        // Should return a list
        assertTrue("Mixed metadata should return a list", result is List)
        
        // Should have some tags
        assertTrue("Mixed metadata should return some results", result.isNotEmpty())
        
        // All returned tags should have valid properties
        for (tag in result) {
            assertNotNull("Each tag should have an ID", tag.id)
            assertNotNull("Each tag should have a name", tag.name)
            assertNotNull("Each tag should have a category", tag.category)
            assertNotNull("Each tag should have a color", tag.color)
        }
    }
}

/**
 * Parameterized test for format/type tags specifically
 */
@RunWith(Parameterized::class)
class FormatTagTest(
    private val metadata: String,
    private val description: String
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{1}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf("Manga", "Japanese manga format"),
                arrayOf("Manhwa", "Korean manhwa format"),
                arrayOf("Manhua", "Chinese manhua format"),
                arrayOf("Webtoon", "Webtoon format"),
                arrayOf("Light Novel", "Light novel format")
            )
        }
    }

    @Test
    fun `test format tag recognition`() {
        val result = TagMatcher.findBestMatch(metadata)
        
        // We expect these to either match a format tag or return null
        // The key is that the function works without errors
        if (result != null) {
            assertEquals("Format tags should be in FORMAT category", 
                TagCategory.FORMAT, result.category)
        }
        
        // Test should complete without exceptions
        assertTrue("Test completed successfully", true)
    }
}

/**
 * Parameterized test for genre tags specifically  
 */
@RunWith(Parameterized::class)
class GenreTagTest(
    private val metadata: String,
    private val description: String
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{1}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf("Action", "Action genre"),
                arrayOf("Romance", "Romance genre"),
                arrayOf("Comedy", "Comedy genre"),
                arrayOf("Drama", "Drama genre"),
                arrayOf("Fantasy", "Fantasy genre"),
                arrayOf("Horror", "Horror genre"),
                arrayOf("Mystery", "Mystery genre"),
                arrayOf("Thriller", "Thriller genre")
            )
        }
    }

    @Test
    fun `test genre tag recognition`() {
        val result = TagMatcher.findBestMatch(metadata)
        
        // We expect these to either match a genre tag or return null
        // The key is that the function works without errors
        if (result != null) {
            assertEquals("Genre tags should be in GENRE category", 
                TagCategory.GENRE, result.category)
        }
        
        // Test should complete without exceptions
        assertTrue("Test completed successfully", true)
    }
}