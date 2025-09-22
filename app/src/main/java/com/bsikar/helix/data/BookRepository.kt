package com.bsikar.helix.data

import androidx.compose.ui.graphics.Color

object BookRepository {
    
    fun getAllBooks(): List<Book> {
        // Start with a clean slate - most books are "Plan to Read"
        return listOf(
            // Plan to Read Books (progress = 0) - Most books start here
            Book(
                title = "Akane-Banashi",
                author = "Yuki Suenaga",
                coverColor = 0xFF4169E1,
                progress = 0f,
                tags = listOf("shounen", "drama", "slice-of-life", "ongoing", "manga"),
                originalMetadataTags = listOf("Shounen", "Drama", "Traditional Arts", "Rakugo")
            ),
            Book(
                title = "Dandadan",
                author = "Yukinobu Tatsu",
                coverColor = 0xFF32CD32,
                progress = 0f,
                tags = listOf("shounen", "supernatural", "comedy", "romance", "ongoing", "manga"),
                originalMetadataTags = listOf("Supernatural", "Comedy", "School", "Aliens", "Ghosts")
            ),
            Book(
                title = "Jujutsu Kaisen",
                author = "Gege Akutami",
                coverColor = 0xFF8A2BE2,
                progress = 0f,
                tags = listOf("shounen", "action", "supernatural", "completed", "manga"),
                originalMetadataTags = listOf("Supernatural", "School", "Action", "Curses")
            ),
            Book(
                title = "Tokyo Ghoul",
                author = "Sui Ishida",
                coverColor = 0xFF696969,
                progress = 0f,
                tags = listOf("seinen", "supernatural", "dark", "completed", "manga"),
                originalMetadataTags = listOf("Dark Fantasy", "Supernatural", "Horror", "Tragedy")
            ),
            Book(
                title = "Monster",
                author = "Naoki Urasawa",
                coverColor = 0xFF8B0000,
                progress = 0f,
                tags = listOf("seinen", "thriller", "psychological", "completed", "manga"),
                originalMetadataTags = listOf("Psychological", "Thriller", "Mystery", "Medical")
            ),
            Book(
                title = "20th Century Boys",
                author = "Naoki Urasawa",
                coverColor = 0xFF4682B4,
                progress = 0f,
                tags = listOf("seinen", "mystery", "thriller", "completed", "manga"),
                originalMetadataTags = listOf("Mystery", "Sci-Fi", "Thriller", "Friendship")
            ),
            Book(
                title = "Pluto",
                author = "Naoki Urasawa",
                coverColor = 0xFF2F4F4F,
                progress = 0f,
                tags = listOf("seinen", "sci-fi", "mystery", "completed", "manga"),
                originalMetadataTags = listOf("Sci-Fi", "Mystery", "Robots", "Philosophy")
            ),
            
            // Currently Reading Books (0 < progress < 1)
            Book(
                title = "Chainsaw Man",
                author = "Tatsuki Fujimoto",
                coverColor = 0xFFDC143C,
                progress = 0.45f,
                lastReadTimestamp = System.currentTimeMillis() - (2 * 24 * 60 * 60 * 1000), // 2 days ago
                tags = listOf("shounen", "action", "supernatural", "ongoing", "manga"),
                originalMetadataTags = listOf("Action", "Supernatural", "Gore", "Dark Comedy")
            ),
            Book(
                title = "One Piece",
                author = "Eiichiro Oda",
                coverColor = 0xFFFF6347,
                progress = 0.72f,
                lastReadTimestamp = System.currentTimeMillis() - (5 * 24 * 60 * 60 * 1000), // 5 days ago
                tags = listOf("shounen", "adventure", "action", "ongoing", "manga"),
                originalMetadataTags = listOf("Adventure", "Friendship", "Pirates", "Comedy")
            ),
            Book(
                title = "Berserk",
                author = "Kentaro Miura",
                coverColor = 0xFF800080,
                progress = 0.38f,
                lastReadTimestamp = System.currentTimeMillis() - (1 * 24 * 60 * 60 * 1000), // 1 day ago
                tags = listOf("seinen", "dark-fantasy", "action", "ongoing", "manga"),
                originalMetadataTags = listOf("Dark Fantasy", "Medieval", "Action", "Mature")
            ),
            Book(
                title = "Vagabond",
                author = "Takehiko Inoue",
                coverColor = 0xFF8B4513,
                progress = 0.61f,
                lastReadTimestamp = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000), // 1 week ago
                tags = listOf("seinen", "historical", "action", "martial-arts", "hiatus", "manga"),
                originalMetadataTags = listOf("Historical", "Samurai", "Martial Arts", "Philosophy")
            ),
            Book(
                title = "Attack on Titan",
                author = "Hajime Isayama",
                coverColor = 0xFF8FBC8F,
                progress = 0.89f,
                lastReadTimestamp = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000), // 3 days ago
                tags = listOf("shounen", "action", "drama", "completed", "manga"),
                originalMetadataTags = listOf("Action", "Drama", "Military", "Titans")
            ),
            
            // Completed Books (progress = 1.0)
            Book(
                title = "Death Note",
                author = "Tsugumi Ohba",
                coverColor = 0xFF000000,
                progress = 1.0f,
                lastReadTimestamp = System.currentTimeMillis() - (14 * 24 * 60 * 60 * 1000), // 2 weeks ago
                tags = listOf("shounen", "psychological", "thriller", "completed", "manga"),
                originalMetadataTags = listOf("Psychological", "Supernatural", "Crime", "Justice")
            ),
            Book(
                title = "Fullmetal Alchemist",
                author = "Hiromu Arakawa",
                coverColor = 0xFFB8860B,
                progress = 1.0f,
                lastReadTimestamp = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000), // 1 month ago
                tags = listOf("shounen", "adventure", "action", "completed", "manga"),
                originalMetadataTags = listOf("Adventure", "Military", "Alchemy", "Brotherhood")
            ),
            Book(
                title = "Hunter x Hunter",
                author = "Yoshihiro Togashi",
                coverColor = 0xFF228B22,
                progress = 1.0f,
                lastReadTimestamp = System.currentTimeMillis() - (21 * 24 * 60 * 60 * 1000), // 3 weeks ago
                tags = listOf("shounen", "adventure", "action", "hiatus", "manga"),
                originalMetadataTags = listOf("Adventure", "Supernatural", "Strategic", "Complex")
            ),
            Book(
                title = "Spirited Away",
                author = "Hayao Miyazaki",
                coverColor = 0xFF9370DB,
                progress = 1.0f,
                lastReadTimestamp = System.currentTimeMillis() - (45 * 24 * 60 * 60 * 1000), // 1.5 months ago
                tags = listOf("family", "fantasy", "adventure", "completed", "manga"),
                originalMetadataTags = listOf("Fantasy", "Family", "Magic", "Coming of Age")
            ),
            Book(
                title = "Your Name",
                author = "Makoto Shinkai",
                coverColor = 0xFFFF69B4,
                progress = 1.0f,
                lastReadTimestamp = System.currentTimeMillis() - (60 * 24 * 60 * 60 * 1000), // 2 months ago
                tags = listOf("romance", "supernatural", "drama", "completed", "manga"),
                originalMetadataTags = listOf("Romance", "Time Travel", "Drama", "Slice of Life")
            ),
            Book(
                title = "Akira",
                author = "Katsuhiro Otomo",
                coverColor = 0xFF483D8B,
                progress = 1.0f,
                lastReadTimestamp = System.currentTimeMillis() - (90 * 24 * 60 * 60 * 1000), // 3 months ago
                tags = listOf("seinen", "sci-fi", "action", "completed", "manga"),
                originalMetadataTags = listOf("Cyberpunk", "Post-Apocalyptic", "Psychic Powers", "Classic")
            ),
            Book(
                title = "Ghost in the Shell",
                author = "Masamune Shirow",
                coverColor = 0xFF708090,
                progress = 1.0f,
                lastReadTimestamp = System.currentTimeMillis() - (120 * 24 * 60 * 60 * 1000), // 4 months ago
                tags = listOf("seinen", "sci-fi", "action", "completed", "manga"),
                originalMetadataTags = listOf("Cyberpunk", "Philosophy", "AI", "Technology")
            )
        )
    }
    
    fun getRecentBooks(): List<Book> {
        return getAllBooks()
            .filter { it.lastReadTimestamp > 0 }
            .sortedByDescending { it.lastReadTimestamp }
            .take(10) // Recent 10 books
    }
}