package com.bsikar.helix.data

import androidx.compose.ui.graphics.Color

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
 * Predefined tags that the system recognizes
 */
object PresetTags {
    
    // Genre tags - expanded for both manga/webtoons and regular books
    private val genreTags = listOf(
        // Universal genres
        Tag("action", "Action", Color(0xFFE53E3E), TagCategory.GENRE),
        Tag("adventure", "Adventure", Color(0xFF38A169), TagCategory.GENRE),
        Tag("comedy", "Comedy", Color(0xFFD69E2E), TagCategory.GENRE),
        Tag("drama", "Drama", Color(0xFF805AD5), TagCategory.GENRE),
        Tag("fantasy", "Fantasy", Color(0xFF9F7AEA), TagCategory.GENRE),
        Tag("horror", "Horror", Color(0xFF2D3748), TagCategory.GENRE),
        Tag("mystery", "Mystery", Color(0xFF2B6CB0), TagCategory.GENRE),
        Tag("psychological", "Psychological", Color(0xFF553C9A), TagCategory.GENRE),
        Tag("romance", "Romance", Color(0xFFE53E3E), TagCategory.GENRE),
        Tag("sci-fi", "Sci-Fi", Color(0xFF3182CE), TagCategory.GENRE),
        Tag("slice-of-life", "Slice of Life", Color(0xFF38A169), TagCategory.GENRE),
        Tag("sports", "Sports", Color(0xFFD69E2E), TagCategory.GENRE),
        Tag("supernatural", "Supernatural", Color(0xFF805AD5), TagCategory.GENRE),
        Tag("thriller", "Thriller", Color(0xFF2D3748), TagCategory.GENRE),
        
        // Book-specific genres
        Tag("literary-fiction", "Literary Fiction", Color(0xFF4A5568), TagCategory.GENRE),
        Tag("crime", "Crime", Color(0xFF1A202C), TagCategory.GENRE),
        Tag("suspense", "Suspense", Color(0xFF2D3748), TagCategory.GENRE),
        Tag("western", "Western", Color(0xFF744210), TagCategory.GENRE),
        Tag("contemporary", "Contemporary", Color(0xFF38A169), TagCategory.GENRE),
        Tag("historical-fiction", "Historical Fiction", Color(0xFF553C9A), TagCategory.GENRE),
        Tag("paranormal", "Paranormal", Color(0xFF805AD5), TagCategory.GENRE),
        Tag("urban-fantasy", "Urban Fantasy", Color(0xFF9F7AEA), TagCategory.GENRE),
        Tag("dystopian", "Dystopian", Color(0xFF4A5568), TagCategory.GENRE),
        Tag("utopian", "Utopian", Color(0xFF38A169), TagCategory.GENRE),
        Tag("apocalyptic", "Apocalyptic", Color(0xFF2D3748), TagCategory.GENRE),
        Tag("space-opera", "Space Opera", Color(0xFF3182CE), TagCategory.GENRE),
        Tag("hard-sf", "Hard Science Fiction", Color(0xFF2B6CB0), TagCategory.GENRE),
        Tag("soft-sf", "Soft Science Fiction", Color(0xFF3182CE), TagCategory.GENRE),
        Tag("alternate-history", "Alternate History", Color(0xFF553C9A), TagCategory.GENRE),
        Tag("time-travel", "Time Travel", Color(0xFF805AD5), TagCategory.GENRE),
        Tag("steampunk", "Steampunk", Color(0xFF744210), TagCategory.GENRE),
        Tag("cyberpunk", "Cyberpunk", Color(0xFF2D3748), TagCategory.GENRE),
        Tag("biopunk", "Biopunk", Color(0xFF38A169), TagCategory.GENRE),
        Tag("gothic", "Gothic", Color(0xFF2D3748), TagCategory.GENRE),
        Tag("satire", "Satire", Color(0xFFD69E2E), TagCategory.GENRE),
        Tag("parody", "Parody", Color(0xFFD69E2E), TagCategory.GENRE),
        Tag("magical-realism", "Magical Realism", Color(0xFF9F7AEA), TagCategory.GENRE),
        Tag("dark-fantasy", "Dark Fantasy", Color(0xFF2D3748), TagCategory.GENRE),
        Tag("high-fantasy", "High Fantasy", Color(0xFF9F7AEA), TagCategory.GENRE),
        Tag("low-fantasy", "Low Fantasy", Color(0xFF805AD5), TagCategory.GENRE),
        Tag("epic-fantasy", "Epic Fantasy", Color(0xFF9F7AEA), TagCategory.GENRE),
        Tag("sword-and-sorcery", "Sword and Sorcery", Color(0xFFE53E3E), TagCategory.GENRE),
        Tag("cozy-mystery", "Cozy Mystery", Color(0xFF38A169), TagCategory.GENRE),
        Tag("noir", "Noir", Color(0xFF1A202C), TagCategory.GENRE),
        Tag("police-procedural", "Police Procedural", Color(0xFF2B6CB0), TagCategory.GENRE),
        Tag("espionage", "Espionage", Color(0xFF2D3748), TagCategory.GENRE),
        Tag("legal-thriller", "Legal Thriller", Color(0xFF2B6CB0), TagCategory.GENRE),
        Tag("medical-thriller", "Medical Thriller", Color(0xFFE53E3E), TagCategory.GENRE),
        Tag("techno-thriller", "Techno Thriller", Color(0xFF3182CE), TagCategory.GENRE),
        
        // Manga/Webtoon specific genres
        Tag("isekai", "Isekai", Color(0xFF9F7AEA), TagCategory.GENRE),
        Tag("mecha", "Mecha", Color(0xFF4A5568), TagCategory.GENRE),
        Tag("ecchi", "Ecchi", Color(0xFFE53E3E), TagCategory.GENRE),
        Tag("harem", "Harem", Color(0xFF805AD5), TagCategory.GENRE),
        Tag("reverse-harem", "Reverse Harem", Color(0xFF9F7AEA), TagCategory.GENRE),
        Tag("yaoi", "Yaoi", Color(0xFF3182CE), TagCategory.GENRE),
        Tag("yuri", "Yuri", Color(0xFFE53E3E), TagCategory.GENRE),
        Tag("shounen-ai", "Shounen-Ai", Color(0xFF3182CE), TagCategory.GENRE),
        Tag("shoujo-ai", "Shoujo-Ai", Color(0xFFE53E3E), TagCategory.GENRE),
        Tag("smut", "Smut", Color(0xFF2D3748), TagCategory.GENRE),
        Tag("cultivation", "Cultivation", Color(0xFF38A169), TagCategory.GENRE),
        Tag("murim", "Murim", Color(0xFFE53E3E), TagCategory.GENRE),
        Tag("villainess", "Villainess", Color(0xFF805AD5), TagCategory.GENRE),
        Tag("reincarnation", "Reincarnation", Color(0xFF9F7AEA), TagCategory.GENRE),
        Tag("regression", "Regression", Color(0xFF3182CE), TagCategory.GENRE),
        Tag("system", "System", Color(0xFF2B6CB0), TagCategory.GENRE),
        Tag("dungeon", "Dungeon", Color(0xFF4A5568), TagCategory.GENRE),
        Tag("tower", "Tower", Color(0xFF2D3748), TagCategory.GENRE),
        Tag("game-elements", "Game Elements", Color(0xFF38A169), TagCategory.GENRE),
        Tag("level-up", "Level Up", Color(0xFFD69E2E), TagCategory.GENRE),
        Tag("overpowered", "Overpowered", Color(0xFFE53E3E), TagCategory.GENRE),
        Tag("weak-to-strong", "Weak to Strong", Color(0xFF38A169), TagCategory.GENRE),
    )
    
    // Demographic tags - expanded for books and manga/webtoons
    private val demographicTags = listOf(
        // Manga/Webtoon demographics
        Tag("shounen", "Shounen", Color(0xFF3182CE), TagCategory.DEMOGRAPHIC),
        Tag("shoujo", "Shoujo", Color(0xFFE53E3E), TagCategory.DEMOGRAPHIC),
        Tag("seinen", "Seinen", Color(0xFF2D3748), TagCategory.DEMOGRAPHIC),
        Tag("josei", "Josei", Color(0xFF805AD5), TagCategory.DEMOGRAPHIC),
        Tag("kodomomuke", "Kodomomuke", Color(0xFFD69E2E), TagCategory.DEMOGRAPHIC),
        
        // Book demographics
        Tag("young-adult", "Young Adult", Color(0xFF38A169), TagCategory.DEMOGRAPHIC),
        Tag("new-adult", "New Adult", Color(0xFF9F7AEA), TagCategory.DEMOGRAPHIC),
        Tag("adult", "Adult", Color(0xFF2B6CB0), TagCategory.DEMOGRAPHIC),
        Tag("middle-grade", "Middle Grade", Color(0xFFD69E2E), TagCategory.DEMOGRAPHIC),
        Tag("children", "Children", Color(0xFF38A169), TagCategory.DEMOGRAPHIC),
        Tag("teen", "Teen", Color(0xFF3182CE), TagCategory.DEMOGRAPHIC),
        Tag("mature", "Mature", Color(0xFF2D3748), TagCategory.DEMOGRAPHIC),
        Tag("all-ages", "All Ages", Color(0xFF38A169), TagCategory.DEMOGRAPHIC),
    )
    
    // Theme tags - massively expanded for comprehensive coverage
    private val themeTags = listOf(
        // Universal themes
        Tag("school", "School", Color(0xFF38A169), TagCategory.THEME),
        Tag("workplace", "Workplace", Color(0xFF2B6CB0), TagCategory.THEME),
        Tag("military", "Military", Color(0xFF2D3748), TagCategory.THEME),
        Tag("historical", "Historical", Color(0xFF553C9A), TagCategory.THEME),
        Tag("post-apocalyptic", "Post-Apocalyptic", Color(0xFF4A5568), TagCategory.THEME),
        Tag("magic", "Magic", Color(0xFF9F7AEA), TagCategory.THEME),
        Tag("martial-arts", "Martial Arts", Color(0xFFE53E3E), TagCategory.THEME),
        Tag("cooking", "Cooking", Color(0xFFD69E2E), TagCategory.THEME),
        Tag("music", "Music", Color(0xFF805AD5), TagCategory.THEME),
        Tag("gaming", "Gaming", Color(0xFF38A169), TagCategory.THEME),
        
        // Book-specific themes
        Tag("biography", "Biography", Color(0xFF2B6CB0), TagCategory.THEME),
        Tag("memoir", "Memoir", Color(0xFF805AD5), TagCategory.THEME),
        Tag("autobiography", "Autobiography", Color(0xFF553C9A), TagCategory.THEME),
        Tag("true-crime", "True Crime", Color(0xFF2D3748), TagCategory.THEME),
        Tag("philosophy", "Philosophy", Color(0xFF4A5568), TagCategory.THEME),
        Tag("religion", "Religion", Color(0xFF744210), TagCategory.THEME),
        Tag("spirituality", "Spirituality", Color(0xFF9F7AEA), TagCategory.THEME),
        Tag("self-help", "Self Help", Color(0xFF38A169), TagCategory.THEME),
        Tag("business", "Business", Color(0xFF2B6CB0), TagCategory.THEME),
        Tag("economics", "Economics", Color(0xFF3182CE), TagCategory.THEME),
        Tag("politics", "Politics", Color(0xFF2D3748), TagCategory.THEME),
        Tag("history", "History", Color(0xFF553C9A), TagCategory.THEME),
        Tag("science", "Science", Color(0xFF3182CE), TagCategory.THEME),
        Tag("nature", "Nature", Color(0xFF38A169), TagCategory.THEME),
        Tag("travel", "Travel", Color(0xFFD69E2E), TagCategory.THEME),
        Tag("health", "Health", Color(0xFFE53E3E), TagCategory.THEME),
        Tag("fitness", "Fitness", Color(0xFF38A169), TagCategory.THEME),
        Tag("psychology", "Psychology", Color(0xFF805AD5), TagCategory.THEME),
        Tag("sociology", "Sociology", Color(0xFF2B6CB0), TagCategory.THEME),
        Tag("anthropology", "Anthropology", Color(0xFF744210), TagCategory.THEME),
        Tag("technology", "Technology", Color(0xFF3182CE), TagCategory.THEME),
        Tag("computer-science", "Computer Science", Color(0xFF2B6CB0), TagCategory.THEME),
        Tag("mathematics", "Mathematics", Color(0xFF553C9A), TagCategory.THEME),
        Tag("engineering", "Engineering", Color(0xFF4A5568), TagCategory.THEME),
        Tag("medicine", "Medicine", Color(0xFFE53E3E), TagCategory.THEME),
        Tag("law", "Law", Color(0xFF2D3748), TagCategory.THEME),
        Tag("education", "Education", Color(0xFF38A169), TagCategory.THEME),
        Tag("parenting", "Parenting", Color(0xFFD69E2E), TagCategory.THEME),
        Tag("relationships", "Relationships", Color(0xFFE53E3E), TagCategory.THEME),
        Tag("family", "Family", Color(0xFF38A169), TagCategory.THEME),
        Tag("friendship", "Friendship", Color(0xFF805AD5), TagCategory.THEME),
        Tag("coming-of-age", "Coming of Age", Color(0xFF9F7AEA), TagCategory.THEME),
        Tag("loss-and-grief", "Loss and Grief", Color(0xFF2D3748), TagCategory.THEME),
        Tag("identity", "Identity", Color(0xFF553C9A), TagCategory.THEME),
        Tag("social-issues", "Social Issues", Color(0xFF2B6CB0), TagCategory.THEME),
        Tag("war", "War", Color(0xFF2D3748), TagCategory.THEME),
        Tag("espionage", "Espionage", Color(0xFF4A5568), TagCategory.THEME),
        Tag("survival", "Survival", Color(0xFF38A169), TagCategory.THEME),
        Tag("exploration", "Exploration", Color(0xFFD69E2E), TagCategory.THEME),
        Tag("space", "Space", Color(0xFF3182CE), TagCategory.THEME),
        Tag("time-period-ancient", "Ancient Times", Color(0xFF744210), TagCategory.THEME),
        Tag("time-period-medieval", "Medieval", Color(0xFF553C9A), TagCategory.THEME),
        Tag("time-period-renaissance", "Renaissance", Color(0xFF805AD5), TagCategory.THEME),
        Tag("time-period-victorian", "Victorian", Color(0xFF2D3748), TagCategory.THEME),
        Tag("time-period-wwi", "World War I", Color(0xFF4A5568), TagCategory.THEME),
        Tag("time-period-wwii", "World War II", Color(0xFF2D3748), TagCategory.THEME),
        Tag("time-period-cold-war", "Cold War", Color(0xFF2B6CB0), TagCategory.THEME),
        Tag("time-period-modern", "Modern Era", Color(0xFF38A169), TagCategory.THEME),
        Tag("small-town", "Small Town", Color(0xFF38A169), TagCategory.THEME),
        Tag("big-city", "Big City", Color(0xFF2B6CB0), TagCategory.THEME),
        Tag("rural", "Rural", Color(0xFF744210), TagCategory.THEME),
        Tag("urban", "Urban", Color(0xFF4A5568), TagCategory.THEME),
        Tag("suburban", "Suburban", Color(0xFF38A169), TagCategory.THEME),
        Tag("island", "Island", Color(0xFF3182CE), TagCategory.THEME),
        Tag("desert", "Desert", Color(0xFFD69E2E), TagCategory.THEME),
        Tag("forest", "Forest", Color(0xFF38A169), TagCategory.THEME),
        Tag("mountain", "Mountain", Color(0xFF4A5568), TagCategory.THEME),
        Tag("ocean", "Ocean", Color(0xFF3182CE), TagCategory.THEME),
        Tag("royalty", "Royalty", Color(0xFF805AD5), TagCategory.THEME),
        Tag("nobility", "Nobility", Color(0xFF553C9A), TagCategory.THEME),
        Tag("peasantry", "Peasantry", Color(0xFF744210), TagCategory.THEME),
        Tag("pirates", "Pirates", Color(0xFF2D3748), TagCategory.THEME),
        Tag("vikings", "Vikings", Color(0xFF4A5568), TagCategory.THEME),
        Tag("knights", "Knights", Color(0xFF553C9A), TagCategory.THEME),
        Tag("samurai", "Samurai", Color(0xFFE53E3E), TagCategory.THEME),
        Tag("ninjas", "Ninjas", Color(0xFF2D3748), TagCategory.THEME),
        Tag("cowboys", "Cowboys", Color(0xFF744210), TagCategory.THEME),
        Tag("detectives", "Detectives", Color(0xFF2B6CB0), TagCategory.THEME),
        Tag("spies", "Spies", Color(0xFF4A5568), TagCategory.THEME),
        Tag("hackers", "Hackers", Color(0xFF3182CE), TagCategory.THEME),
        Tag("artists", "Artists", Color(0xFF805AD5), TagCategory.THEME),
        Tag("writers", "Writers", Color(0xFF553C9A), TagCategory.THEME),
        Tag("scientists", "Scientists", Color(0xFF3182CE), TagCategory.THEME),
        Tag("doctors", "Doctors", Color(0xFFE53E3E), TagCategory.THEME),
        Tag("teachers", "Teachers", Color(0xFF38A169), TagCategory.THEME),
        Tag("athletes", "Athletes", Color(0xFFD69E2E), TagCategory.THEME),
        Tag("musicians", "Musicians", Color(0xFF805AD5), TagCategory.THEME),
        Tag("actors", "Actors", Color(0xFF9F7AEA), TagCategory.THEME),
        Tag("farmers", "Farmers", Color(0xFF38A169), TagCategory.THEME),
        Tag("sailors", "Sailors", Color(0xFF3182CE), TagCategory.THEME),
        Tag("pilots", "Pilots", Color(0xFF2B6CB0), TagCategory.THEME),
        
        // Manga/Webtoon specific themes
        Tag("academy", "Academy", Color(0xFF38A169), TagCategory.THEME),
        Tag("guild", "Guild", Color(0xFF2B6CB0), TagCategory.THEME),
        Tag("demon-lord", "Demon Lord", Color(0xFF2D3748), TagCategory.THEME),
        Tag("hero", "Hero", Color(0xFFD69E2E), TagCategory.THEME),
        Tag("villain", "Villain", Color(0xFF2D3748), TagCategory.THEME),
        Tag("nobility-politics", "Nobility Politics", Color(0xFF553C9A), TagCategory.THEME),
        Tag("contract-marriage", "Contract Marriage", Color(0xFFE53E3E), TagCategory.THEME),
        Tag("fake-relationship", "Fake Relationship", Color(0xFF805AD5), TagCategory.THEME),
        Tag("arranged-marriage", "Arranged Marriage", Color(0xFF9F7AEA), TagCategory.THEME),
        Tag("revenge", "Revenge", Color(0xFF2D3748), TagCategory.THEME),
        Tag("betrayal", "Betrayal", Color(0xFF4A5568), TagCategory.THEME),
        Tag("redemption", "Redemption", Color(0xFF38A169), TagCategory.THEME),
        Tag("transmigration", "Transmigration", Color(0xFF9F7AEA), TagCategory.THEME),
        Tag("time-loop", "Time Loop", Color(0xFF805AD5), TagCategory.THEME),
        Tag("parallel-world", "Parallel World", Color(0xFF3182CE), TagCategory.THEME),
        Tag("game-world", "Game World", Color(0xFF38A169), TagCategory.THEME),
        Tag("virtual-reality", "Virtual Reality", Color(0xFF3182CE), TagCategory.THEME),
        Tag("monsters", "Monsters", Color(0xFF2D3748), TagCategory.THEME),
        Tag("beasts", "Beasts", Color(0xFF744210), TagCategory.THEME),
        Tag("dragons", "Dragons", Color(0xFFE53E3E), TagCategory.THEME),
        Tag("gods", "Gods", Color(0xFFD69E2E), TagCategory.THEME),
        Tag("demons", "Demons", Color(0xFF2D3748), TagCategory.THEME),
        Tag("angels", "Angels", Color(0xFF805AD5), TagCategory.THEME),
        Tag("spirits", "Spirits", Color(0xFF9F7AEA), TagCategory.THEME),
        Tag("zombies", "Zombies", Color(0xFF2D3748), TagCategory.THEME),
        Tag("vampires", "Vampires", Color(0xFFE53E3E), TagCategory.THEME),
        Tag("werewolves", "Werewolves", Color(0xFF744210), TagCategory.THEME),
        Tag("cultivation-world", "Cultivation World", Color(0xFF38A169), TagCategory.THEME),
        Tag("clan-wars", "Clan Wars", Color(0xFFE53E3E), TagCategory.THEME),
        Tag("tournament", "Tournament", Color(0xFFD69E2E), TagCategory.THEME),
        Tag("competition", "Competition", Color(0xFF2B6CB0), TagCategory.THEME),
        Tag("training", "Training", Color(0xFF38A169), TagCategory.THEME),
        Tag("master-disciple", "Master-Disciple", Color(0xFF553C9A), TagCategory.THEME),
        Tag("teacher-student", "Teacher-Student", Color(0xFF38A169), TagCategory.THEME),
        Tag("mentor", "Mentor", Color(0xFF2B6CB0), TagCategory.THEME),
        Tag("childhood-friends", "Childhood Friends", Color(0xFF805AD5), TagCategory.THEME),
        Tag("enemies-to-lovers", "Enemies to Lovers", Color(0xFFE53E3E), TagCategory.THEME),
        Tag("friends-to-lovers", "Friends to Lovers", Color(0xFF38A169), TagCategory.THEME),
        Tag("love-triangle", "Love Triangle", Color(0xFF9F7AEA), TagCategory.THEME),
        Tag("forbidden-love", "Forbidden Love", Color(0xFF2D3748), TagCategory.THEME),
        Tag("unrequited-love", "Unrequited Love", Color(0xFF805AD5), TagCategory.THEME),
        Tag("second-chance", "Second Chance", Color(0xFF38A169), TagCategory.THEME),
        Tag("age-gap", "Age Gap", Color(0xFF553C9A), TagCategory.THEME),
        Tag("cross-dressing", "Cross-dressing", Color(0xFF9F7AEA), TagCategory.THEME),
        Tag("gender-bender", "Gender Bender", Color(0xFF805AD5), TagCategory.THEME),
        Tag("body-swap", "Body Swap", Color(0xFF3182CE), TagCategory.THEME),
        Tag("shapeshifter", "Shapeshifter", Color(0xFF744210), TagCategory.THEME),
    )
    
    // Status tags
    private val statusTags = listOf(
        Tag("ongoing", "Ongoing", Color(0xFF38A169), TagCategory.STATUS),
        Tag("completed", "Completed", Color(0xFF3182CE), TagCategory.STATUS),
        Tag("hiatus", "Hiatus", Color(0xFFD69E2E), TagCategory.STATUS),
        Tag("cancelled", "Cancelled", Color(0xFFE53E3E), TagCategory.STATUS),
    )
    
    // Format tags - expanded for all types of books and media
    private val formatTags = listOf(
        // Manga/Webtoon formats
        Tag("manga", "Manga", Color(0xFF2B6CB0), TagCategory.FORMAT),
        Tag("manhwa", "Manhwa", Color(0xFFE53E3E), TagCategory.FORMAT),
        Tag("manhua", "Manhua", Color(0xFFD69E2E), TagCategory.FORMAT),
        Tag("webtoon", "Webtoon", Color(0xFF9F7AEA), TagCategory.FORMAT),
        Tag("light-novel", "Light Novel", Color(0xFF805AD5), TagCategory.FORMAT),
        Tag("web-novel", "Web Novel", Color(0xFF38A169), TagCategory.FORMAT),
        Tag("doujinshi", "Doujinshi", Color(0xFF553C9A), TagCategory.FORMAT),
        Tag("graphic-novel", "Graphic Novel", Color(0xFF4A5568), TagCategory.FORMAT),
        Tag("comic", "Comic", Color(0xFF3182CE), TagCategory.FORMAT),
        Tag("webcomic", "Webcomic", Color(0xFF38A169), TagCategory.FORMAT),
        
        // Book formats
        Tag("novel", "Novel", Color(0xFF2B6CB0), TagCategory.FORMAT),
        Tag("novella", "Novella", Color(0xFF805AD5), TagCategory.FORMAT),
        Tag("short-story", "Short Story", Color(0xFF38A169), TagCategory.FORMAT),
        Tag("collection", "Collection", Color(0xFF553C9A), TagCategory.FORMAT),
        Tag("anthology", "Anthology", Color(0xFF9F7AEA), TagCategory.FORMAT),
        Tag("poetry", "Poetry", Color(0xFFE53E3E), TagCategory.FORMAT),
        Tag("essay", "Essay", Color(0xFF744210), TagCategory.FORMAT),
        Tag("textbook", "Textbook", Color(0xFF2B6CB0), TagCategory.FORMAT),
        Tag("reference", "Reference", Color(0xFF4A5568), TagCategory.FORMAT),
        Tag("manual", "Manual", Color(0xFF2D3748), TagCategory.FORMAT),
        Tag("guide", "Guide", Color(0xFF38A169), TagCategory.FORMAT),
        Tag("handbook", "Handbook", Color(0xFF553C9A), TagCategory.FORMAT),
        Tag("dictionary", "Dictionary", Color(0xFF2B6CB0), TagCategory.FORMAT),
        Tag("encyclopedia", "Encyclopedia", Color(0xFF805AD5), TagCategory.FORMAT),
        Tag("atlas", "Atlas", Color(0xFF3182CE), TagCategory.FORMAT),
        Tag("cookbook", "Cookbook", Color(0xFFD69E2E), TagCategory.FORMAT),
        Tag("art-book", "Art Book", Color(0xFF805AD5), TagCategory.FORMAT),
        Tag("coffee-table", "Coffee Table Book", Color(0xFF744210), TagCategory.FORMAT),
        Tag("picture-book", "Picture Book", Color(0xFF38A169), TagCategory.FORMAT),
        Tag("board-book", "Board Book", Color(0xFFD69E2E), TagCategory.FORMAT),
        Tag("chapter-book", "Chapter Book", Color(0xFF2B6CB0), TagCategory.FORMAT),
        Tag("graphic-memoir", "Graphic Memoir", Color(0xFF553C9A), TagCategory.FORMAT),
        Tag("photo-book", "Photo Book", Color(0xFF3182CE), TagCategory.FORMAT),
        
        // Digital/Interactive formats
        Tag("ebook", "E-book", Color(0xFF3182CE), TagCategory.FORMAT),
        Tag("audiobook", "Audiobook", Color(0xFF805AD5), TagCategory.FORMAT),
        Tag("interactive", "Interactive", Color(0xFF38A169), TagCategory.FORMAT),
        Tag("enhanced-ebook", "Enhanced E-book", Color(0xFF9F7AEA), TagCategory.FORMAT),
        Tag("multimedia", "Multimedia", Color(0xFFD69E2E), TagCategory.FORMAT),
        
        // Academic formats
        Tag("thesis", "Thesis", Color(0xFF2D3748), TagCategory.FORMAT),
        Tag("dissertation", "Dissertation", Color(0xFF4A5568), TagCategory.FORMAT),
        Tag("research-paper", "Research Paper", Color(0xFF2B6CB0), TagCategory.FORMAT),
        Tag("journal-article", "Journal Article", Color(0xFF553C9A), TagCategory.FORMAT),
        Tag("white-paper", "White Paper", Color(0xFF744210), TagCategory.FORMAT),
        Tag("case-study", "Case Study", Color(0xFF38A169), TagCategory.FORMAT),
        
        // Series formats
        Tag("series", "Series", Color(0xFF805AD5), TagCategory.FORMAT),
        Tag("trilogy", "Trilogy", Color(0xFF9F7AEA), TagCategory.FORMAT),
        Tag("duology", "Duology", Color(0xFF3182CE), TagCategory.FORMAT),
        Tag("standalone", "Standalone", Color(0xFF38A169), TagCategory.FORMAT),
        Tag("sequel", "Sequel", Color(0xFF2B6CB0), TagCategory.FORMAT),
        Tag("prequel", "Prequel", Color(0xFF553C9A), TagCategory.FORMAT),
        Tag("spin-off", "Spin-off", Color(0xFFD69E2E), TagCategory.FORMAT),
        Tag("companion", "Companion", Color(0xFF805AD5), TagCategory.FORMAT),
    )
    
    // Special tags
    private val specialTags = listOf(
        Tag("untagged", "Untagged", Color(0xFF718096), TagCategory.CUSTOM),
        Tag("favorite", "Favorite", Color(0xFFED8936), TagCategory.CUSTOM),
    )
    
    /**
     * Mutable list of custom tags added by users
     */
    private val customTags = mutableListOf<Tag>()
    
    /**
     * All predefined tags
     */
    private val predefinedTags: List<Tag> = genreTags + demographicTags + themeTags + statusTags + formatTags + specialTags
    
    /**
     * All tags (predefined + custom)
     */
    val allTags: List<Tag> 
        get() = predefinedTags + customTags
    
    /**
     * Get tags by category
     */
    fun getTagsByCategory(category: TagCategory): List<Tag> {
        return allTags.filter { it.category == category }
    }
    
    /**
     * Find tag by ID
     */
    fun findTagById(id: String): Tag? {
        return allTags.find { it.id == id }
    }
    
    /**
     * Find tag by name (case insensitive)
     */
    fun findTagByName(name: String): Tag? {
        return allTags.find { it.name.equals(name, ignoreCase = true) }
    }
    
    /**
     * Get all tag names for similarity matching
     */
    fun getAllTagNames(): List<String> {
        return allTags.map { it.name.lowercase() }
    }
    
    /**
     * Get all tag IDs for similarity matching
     */
    fun getAllTagIds(): List<String> {
        return allTags.map { it.id.lowercase() }
    }
    
    /**
     * Add a custom tag to the specified category
     */
    fun addCustomTag(name: String, category: TagCategory): Tag {
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