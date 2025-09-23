package com.bsikar.helix.data.model

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import android.content.Context
import android.util.Log
import kotlinx.serialization.json.Json
import java.io.IOException

/**
 * Represents a tag that can be applied to books
 */
data class Tag(
    val id: String,
    val name: String,
    val color: Color,
    val category: TagCategory
)

/**
 * Categories for organizing tags
 */
enum class TagCategory(val displayName: String) {
    GENRE("Genre"),
    DEMOGRAPHIC("Demographic"), 
    THEME("Theme"),
    STATUS("Status"),
    FORMAT("Format"),
    CUSTOM("Custom")
}

/**
 * JSON data classes for loading tags from assets
 */
@Serializable
data class TagJson(
    val id: String,
    val name: String,
    val color: String
)

@Serializable
data class TagCategoryJson(
    val name: String,
    val displayName: String,
    val tags: List<TagJson>
)

@Serializable
data class TagsDataJson(
    val categories: List<TagCategoryJson>
)

/**
 * Predefined tags that the system recognizes
 * Now loads from assets/tags.json instead of hardcoded values
 */
object PresetTags {
    
    private var isInitialized = false
    private val customTags = mutableListOf<Tag>()
    private var predefinedTags: List<Tag> = emptyList()
    
    /**
     * Initialize tags by loading from assets/tags.json
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        
        try {
            val tagsData = loadTagsFromAssets(context)
            predefinedTags = parseTagsFromJson(tagsData)
            isInitialized = true
            Log.d("PresetTags", "Successfully loaded ${predefinedTags.size} predefined tags")
        } catch (e: Exception) {
            Log.e("PresetTags", "Failed to load tags from assets, falling back to hardcoded tags", e)
            predefinedTags = getFallbackTags()
            isInitialized = true
        }
    }
    
    /**
     * Initialize tags with fallback data for testing purposes
     */
    fun initializeForTesting() {
        if (isInitialized) return
        
        predefinedTags = getFallbackTags()
        isInitialized = true
        Log.d("PresetTags", "Initialized for testing with ${predefinedTags.size} fallback tags")
    }
    
    /**
     * Load tags JSON from assets
     */
    private fun loadTagsFromAssets(context: Context): TagsDataJson {
        val json = try {
            context.assets.open("tags.json").bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            throw Exception("Failed to read tags.json from assets", e)
        }
        
        return try {
            Json.decodeFromString<TagsDataJson>(json)
        } catch (e: Exception) {
            throw Exception("Failed to parse tags.json", e)
        }
    }
    
    /**
     * Parse tags from JSON data
     */
    private fun parseTagsFromJson(tagsData: TagsDataJson): List<Tag> {
        val tags = mutableListOf<Tag>()
        
        for (categoryJson in tagsData.categories) {
            val category = try {
                TagCategory.valueOf(categoryJson.name)
            } catch (e: IllegalArgumentException) {
                Log.w("PresetTags", "Unknown tag category: ${categoryJson.name}")
                continue
            }
            
            for (tagJson in categoryJson.tags) {
                try {
                    val color = Color(android.graphics.Color.parseColor(tagJson.color))
                    val tag = Tag(
                        id = tagJson.id,
                        name = tagJson.name,
                        color = color,
                        category = category
                    )
                    tags.add(tag)
                } catch (e: Exception) {
                    Log.w("PresetTags", "Failed to parse tag: ${tagJson.id}", e)
                }
            }
        }
        
        return tags
    }
    
    /**
     * Fallback hardcoded tags in case JSON loading fails
     * Contains essential tags needed for tests and basic functionality
     */
    private fun getFallbackTags(): List<Tag> {
        return listOf(
            // Genre tags
            Tag("action", "Action", Color(0xFFE53E3E), TagCategory.GENRE),
            Tag("adventure", "Adventure", Color(0xFF38A169), TagCategory.GENRE),
            Tag("comedy", "Comedy", Color(0xFFD69E2E), TagCategory.GENRE),
            Tag("drama", "Drama", Color(0xFF805AD5), TagCategory.GENRE),
            Tag("fantasy", "Fantasy", Color(0xFF9F7AEA), TagCategory.GENRE),
            Tag("horror", "Horror", Color(0xFF2D3748), TagCategory.GENRE),
            Tag("mystery", "Mystery", Color(0xFF2B6CB0), TagCategory.GENRE),
            Tag("romance", "Romance", Color(0xFFE53E3E), TagCategory.GENRE),
            Tag("sci-fi", "Sci-Fi", Color(0xFF3182CE), TagCategory.GENRE),
            Tag("thriller", "Thriller", Color(0xFF2D3748), TagCategory.GENRE),
            Tag("slice-of-life", "Slice of Life", Color(0xFF38A169), TagCategory.GENRE),
            Tag("isekai", "Isekai", Color(0xFF9F7AEA), TagCategory.GENRE),
            Tag("martial-arts", "Martial Arts", Color(0xFFE53E3E), TagCategory.GENRE),
            
            // Demographics
            Tag("shounen", "Shounen", Color(0xFF3182CE), TagCategory.DEMOGRAPHIC),
            Tag("shoujo", "Shoujo", Color(0xFFE53E3E), TagCategory.DEMOGRAPHIC),
            Tag("seinen", "Seinen", Color(0xFF2D3748), TagCategory.DEMOGRAPHIC),
            Tag("josei", "Josei", Color(0xFF805AD5), TagCategory.DEMOGRAPHIC),
            Tag("young-adult", "Young Adult", Color(0xFF38A169), TagCategory.DEMOGRAPHIC),
            Tag("adult", "Adult", Color(0xFF2B6CB0), TagCategory.DEMOGRAPHIC),
            Tag("children", "Children", Color(0xFF38A169), TagCategory.DEMOGRAPHIC),
            
            // Themes
            Tag("school", "School", Color(0xFF38A169), TagCategory.THEME),
            Tag("magic", "Magic", Color(0xFF9F7AEA), TagCategory.THEME),
            
            // Status
            Tag("ongoing", "Ongoing", Color(0xFF38A169), TagCategory.STATUS),
            Tag("completed", "Completed", Color(0xFF3182CE), TagCategory.STATUS),
            
            // Formats
            Tag("novel", "Novel", Color(0xFF2B6CB0), TagCategory.FORMAT),
            Tag("manga", "Manga", Color(0xFF2B6CB0), TagCategory.FORMAT),
            Tag("manhwa", "Manhwa", Color(0xFFE53E3E), TagCategory.FORMAT),
            Tag("manhua", "Manhua", Color(0xFFD69E2E), TagCategory.FORMAT),
            Tag("webtoon", "Webtoon", Color(0xFF9F7AEA), TagCategory.FORMAT),
            Tag("light-novel", "Light Novel", Color(0xFF805AD5), TagCategory.FORMAT),
            
            // Custom/Special
            Tag("untagged", "Untagged", Color(0xFF718096), TagCategory.CUSTOM),
            Tag("favorite", "Favorite", Color(0xFFED8936), TagCategory.CUSTOM)
        )
    }
    
    /**
     * All tags (predefined + custom)
     */
    val allTags: List<Tag> 
        get() {
            if (!isInitialized) {
                throw IllegalStateException("PresetTags must be initialized with context before use")
            }
            return predefinedTags + customTags
        }
    
    /**
     * Get tags by category
     */
    fun getTagsByCategory(category: TagCategory): List<Tag> {
        if (!isInitialized) {
            throw IllegalStateException("PresetTags must be initialized with context before use")
        }
        return allTags.filter { it.category == category }
    }
    
    /**
     * Find tag by ID
     */
    fun findTagById(id: String): Tag? {
        if (!isInitialized) {
            throw IllegalStateException("PresetTags must be initialized with context before use")
        }
        return allTags.find { it.id == id }
    }
    
    /**
     * Find tag by name (case insensitive)
     */
    fun findTagByName(name: String): Tag? {
        if (!isInitialized) {
            throw IllegalStateException("PresetTags must be initialized with context before use")
        }
        return allTags.find { it.name.equals(name, ignoreCase = true) }
    }
    
    /**
     * Get all tag names for similarity matching
     */
    fun getAllTagNames(): List<String> {
        if (!isInitialized) {
            throw IllegalStateException("PresetTags must be initialized with context before use")
        }
        return allTags.map { it.name.lowercase() }
    }
    
    /**
     * Get all tag IDs for similarity matching
     */
    fun getAllTagIds(): List<String> {
        if (!isInitialized) {
            throw IllegalStateException("PresetTags must be initialized with context before use")
        }
        return allTags.map { it.id.lowercase() }
    }
    
    /**
     * Add a custom tag to the specified category
     */
    fun addCustomTag(name: String, category: TagCategory): Tag {
        if (!isInitialized) {
            throw IllegalStateException("PresetTags must be initialized with context before use")
        }
        
        val id = name.lowercase().replace(" ", "-").replace(Regex("[^a-z0-9-]"), "")
        
        // Check if tag already exists
        findTagById(id)?.let { return it }
        
        // Generate a color for the custom tag based on category
        val color = when (category) {
            TagCategory.GENRE -> Color(0xFF9C27B0)
            TagCategory.DEMOGRAPHIC -> Color(0xFF607D8B)
            TagCategory.THEME -> Color(0xFF795548)
            TagCategory.STATUS -> Color(0xFF009688)
            TagCategory.FORMAT -> Color(0xFFFF5722)
            TagCategory.CUSTOM -> Color(0xFF757575)
        }
        
        val customTag = Tag(id, name, color, category)
        customTags.add(customTag)
        return customTag
    }
    
    /**
     * Remove a custom tag
     */
    fun removeCustomTag(id: String): Boolean {
        return customTags.removeAll { it.id == id }
    }
    
    /**
     * Check if a tag is custom (not predefined)
     */
    fun isCustomTag(id: String): Boolean {
        return customTags.any { it.id == id }
    }
}

/**
 * Advanced tag similarity matching using hybrid multi-algorithm approach
 * Combines exact matching, Jaro-Winkler, Jaccard, and fuzzy word matching
 */
object TagMatcher {
    
    /**
     * Calculate Levenshtein distance between two strings
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }
        
        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j
        
        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i-1] == s2[j-1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i-1][j] + 1,      // deletion
                    dp[i][j-1] + 1,      // insertion
                    dp[i-1][j-1] + cost  // substitution
                )
            }
        }
        return dp[len1][len2]
    }
    
    /**
     * Calculate Jaro similarity between two strings
     */
    private fun jaroSimilarity(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        
        val len1 = s1.length
        val len2 = s2.length
        val matchWindow = maxOf(len1, len2) / 2 - 1
        
        if (matchWindow < 0) return 0.0
        
        val s1Matches = BooleanArray(len1)
        val s2Matches = BooleanArray(len2)
        
        var matches = 0
        var transpositions = 0
        
        // Find matches
        for (i in 0 until len1) {
            val start = maxOf(0, i - matchWindow)
            val end = minOf(i + matchWindow + 1, len2)
            
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
     * Calculate Jaro-Winkler similarity with prefix bonus
     */
    private fun jaroWinklerSimilarity(s1: String, s2: String, prefixScale: Double = 0.1): Double {
        val jaro = jaroSimilarity(s1, s2)
        if (jaro < 0.7) return jaro
        
        val prefix = s1.zip(s2).takeWhile { it.first == it.second }.size
        return jaro + (minOf(prefix, 4) * prefixScale * (1 - jaro))
    }
    
    /**
     * Calculate Jaccard similarity between two sets of words
     */
    private fun jaccardSimilarity(set1: Set<String>, set2: Set<String>): Double {
        val intersection = set1.intersect(set2).size
        val union = set1.union(set2).size
        return if (union == 0) 0.0 else intersection.toDouble() / union
    }
    
    /**
     * Convert string to set of tokens for comparison
     */
    private fun tokenize(text: String): Set<String> {
        return text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() }
            .toSet()
    }
    
    /**
     * Fuzzy word matching for common variations and misspellings
     */
    private fun fuzzyWordMatch(input: String, tagName: String, tagId: String): Double {
        val variations = mapOf(
            // Common misspellings
            "shonen" to "shounen",
            "seien" to "seinen",
            
            // Format variations
            "scifi" to "sci-fi",
            "science fiction" to "sci-fi",
            "sci fi" to "sci-fi",
            "lite novel" to "light-novel",
            "web comic" to "webtoon",
            "webcomic" to "webtoon",
            "graphic novel" to "graphic-novel",
            "ebook" to "ebook",
            "e-book" to "ebook",
            "audio book" to "audiobook",
            
            // Genre variations
            "romantic" to "romance",
            "comedic" to "comedy",
            "magical" to "magic",
            "futuristic" to "sci-fi",
            "battle" to "action",
            "fighting" to "action",
            "humor" to "comedy",
            "funny" to "comedy",
            "scary" to "horror",
            "detective" to "mystery",
            "crime fiction" to "crime",
            "thriller" to "thriller",
            "suspenseful" to "suspense",
            
            // Hyphen vs space variations
            "martial arts" to "martial-arts",
            "slice of life" to "slice-of-life",
            "post apocalyptic" to "post-apocalyptic",
            "coming of age" to "coming-of-age",
            "true crime" to "true-crime",
            "self help" to "self-help",
            "urban fantasy" to "urban-fantasy",
            "historical fiction" to "historical-fiction",
            "science fiction" to "sci-fi",
            "space opera" to "space-opera",
            "time travel" to "time-travel",
            "alternate history" to "alternate-history",
            "hard science fiction" to "hard-sf",
            "soft science fiction" to "soft-sf",
            "sword and sorcery" to "sword-and-sorcery",
            "high fantasy" to "high-fantasy",
            "low fantasy" to "low-fantasy",
            "dark fantasy" to "dark-fantasy",
            "epic fantasy" to "epic-fantasy",
            "magical realism" to "magical-realism",
            "young adult" to "young-adult",
            "new adult" to "new-adult",
            "middle grade" to "middle-grade",
            "all ages" to "all-ages",
            
            // Demographics
            "ya" to "young-adult",
            "teen" to "young-adult",
            "teenager" to "teen",
            "kids" to "children",
            "child" to "children",
            "adult fiction" to "adult",
            
            // Theme variations
            "school life" to "school",
            "high school" to "school",
            "student life" to "school",
            "college" to "school",
            "university" to "school",
            "office" to "workplace",
            "work" to "workplace",
            "job" to "workplace",
            "business" to "workplace",
            "corporate" to "workplace",
            "war" to "military",
            "army" to "military",
            "soldier" to "military",
            "battle" to "military",
            "cooking" to "cooking",
            "chef" to "cooking",
            "food" to "cooking",
            "music" to "music",
            "musician" to "musicians",
            "band" to "music",
            "singer" to "musicians",
            "game" to "gaming",
            "video game" to "gaming",
            "gamer" to "gaming",
            "sports" to "sports",
            "athlete" to "athletes",
            "competition" to "sports",
            "magic" to "magic",
            "wizard" to "magic",
            "witch" to "magic",
            "sorcery" to "magic",
            "spell" to "magic",
            "vampire" to "vampires",
            "werewolf" to "werewolves",
            "zombie" to "zombies",
            "dragon" to "dragons",
            "demon" to "demons",
            "angel" to "angels",
            "god" to "gods",
            "spirit" to "spirits",
            "ghost" to "spirits",
            "monster" to "monsters",
            "beast" to "beasts",
            "creature" to "monsters",
            "pirate" to "pirates",
            "viking" to "vikings",
            "knight" to "knights",
            "cowboy" to "cowboys",
            "detective" to "detectives",
            "spy" to "spies",
            "hacker" to "hackers",
            "artist" to "artists",
            "writer" to "writers",
            "author" to "writers",
            "scientist" to "scientists",
            "doctor" to "doctors",
            "teacher" to "teachers",
            "farmer" to "farmers",
            "sailor" to "sailors",
            "pilot" to "pilots",
            
            // Manga/Webtoon specific
            "cultivation" to "cultivation",
            "xianxia" to "cultivation",
            "wuxia" to "martial-arts",
            "murim" to "murim",
            "system" to "system",
            "leveling" to "level-up",
            "level up" to "level-up",
            "overpowered" to "overpowered",
            "op" to "overpowered",
            "weak to strong" to "weak-to-strong",
            "reincarnation" to "reincarnation",
            "regression" to "regression",
            "transmigration" to "transmigration",
            "isekai" to "isekai",
            "another world" to "isekai",
            "parallel world" to "parallel-world",
            "game world" to "game-world",
            "virtual reality" to "virtual-reality",
            "vr" to "virtual-reality",
            "dungeon" to "dungeon",
            "tower" to "tower",
            "academy" to "academy",
            "guild" to "guild",
            "hero" to "hero",
            "villain" to "villain",
            "villainess" to "villainess",
            "revenge" to "revenge",
            "betrayal" to "betrayal",
            "redemption" to "redemption"
        )
        
        val lowerInput = input.lowercase()
        val lowerTagName = tagName.lowercase()
        val lowerTagId = tagId.lowercase()
        
        // Check direct variations
        for ((variation, canonical) in variations) {
            if ((lowerInput.contains(variation) && (lowerTagName.contains(canonical) || lowerTagId.contains(canonical))) ||
                (lowerInput.contains(canonical) && (lowerTagName.contains(variation) || lowerTagId.contains(variation)))) {
                return 0.9
            }
        }
        
        // Check reverse mappings
        for ((canonical, variation) in variations) {
            if ((lowerInput.contains(variation) && (lowerTagName.contains(canonical) || lowerTagId.contains(canonical))) ||
                (lowerInput.contains(canonical) && (lowerTagName.contains(variation) || lowerTagId.contains(variation)))) {
                return 0.9
            }
        }
        
        return 0.0
    }
    
    /**
     * Find the best matching preset tag using hybrid multi-algorithm approach
     * Returns null if no good match is found (similarity < threshold)
     */
    fun findBestMatch(metadataTag: String, threshold: Double = 0.6): Tag? {
        val input = metadataTag.lowercase().trim()
        if (input.isBlank()) return null
        
        var bestMatch: Tag? = null
        var bestScore = 0.0
        
        for (presetTag in PresetTags.allTags) {
            // Skip special tags like "untagged"
            if (presetTag.category == TagCategory.CUSTOM) continue
            
            val tagName = presetTag.name.lowercase()
            val tagId = presetTag.id.lowercase()
            
            // Method 1: Exact/substring matching (highest priority)
            val exactScore = when {
                input == tagName || input == tagId -> 1.0
                input.contains(tagName) && tagName.length >= 3 -> 0.95
                tagName.contains(input) && input.length >= 3 -> 0.95
                input.contains(tagId) && tagId.length >= 3 -> 0.95
                tagId.contains(input) && input.length >= 3 -> 0.95
                else -> 0.0
            }
            
            // Method 2: Jaro-Winkler for character similarity (handles typos)
            val jaroNameScore = jaroWinklerSimilarity(input, tagName)
            val jaroIdScore = jaroWinklerSimilarity(input, tagId)
            val jaroScore = maxOf(jaroNameScore, jaroIdScore)
            
            // Method 3: Token-based Jaccard (for multi-word phrases)
            val jaccardScore = if (input.contains(" ") || tagName.contains(" ")) {
                val inputTokens = tokenize(input)
                val nameTokens = tokenize(tagName)
                val idTokens = tokenize(tagId)
                maxOf(
                    jaccardSimilarity(inputTokens, nameTokens),
                    jaccardSimilarity(inputTokens, idTokens)
                )
            } else 0.0
            
            // Method 4: Fuzzy word matching for common variations
            val fuzzyScore = fuzzyWordMatch(input, tagName, tagId)
            
            // Combined score with weights (exact match takes priority)
            val finalScore = maxOf(
                exactScore,
                jaroScore * 0.8,
                jaccardScore * 0.7,
                fuzzyScore * 0.85
            )
            
            if (finalScore > bestScore && finalScore >= threshold) {
                bestMatch = presetTag
                bestScore = finalScore
            }
        }
        
        return bestMatch
    }
    
    /**
     * Parse metadata tags and return matching preset tags
     * Unmatched tags result in the "untagged" tag being included
     */
    fun parseMetadataTags(metadataTags: List<String>): List<Tag> {
        val matchedTags = mutableSetOf<Tag>()
        var hasUnmatched = false
        
        for (metadataTag in metadataTags) {
            val match = findBestMatch(metadataTag)
            if (match != null) {
                matchedTags.add(match)
            } else {
                hasUnmatched = true
            }
        }
        
        // If we had any unmatched tags, include the "untagged" tag
        if (hasUnmatched || matchedTags.isEmpty()) {
            PresetTags.findTagById("untagged")?.let { matchedTags.add(it) }
        }
        
        return matchedTags.toList()
    }
}