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
                coverColor = Color(0xFF4169E1),
                progress = 0f,
                tags = listOf("shounen", "drama", "slice-of-life", "ongoing", "manga"),
                originalMetadataTags = listOf("Shounen", "Drama", "Traditional Arts", "Rakugo")
            ),
            Book(
                title = "Dandadan",
                author = "Yukinobu Tatsu",
                coverColor = Color(0xFF32CD32),
                progress = 0f,
                tags = listOf("shounen", "supernatural", "comedy", "romance", "ongoing", "manga"),
                originalMetadataTags = listOf("Supernatural", "Comedy", "School", "Aliens", "Ghosts")
            ),
            Book(
                title = "Jujutsu Kaisen",
                author = "Gege Akutami",
                coverColor = Color(0xFF8A2BE2),
                progress = 0f,
                tags = listOf("shounen", "supernatural", "action", "school", "completed", "manga"),
                originalMetadataTags = listOf("Dark Fantasy", "Supernatural", "Action", "School")
            ),
            Book(
                title = "Chainsaw Man",
                author = "Tatsuki Fujimoto",
                coverColor = Color(0xFFFF4500),
                progress = 0f,
                tags = listOf("shounen", "horror", "action", "supernatural", "completed", "manga"),
                originalMetadataTags = listOf("Horror", "Dark", "Gore", "Devils", "Action")
            ),
            Book(
                title = "One Punch Man",
                author = "ONE",
                coverColor = Color(0xFFD4AF37),
                progress = 0f,
                tags = listOf("seinen", "comedy", "action", "superhero", "ongoing", "manga"),
                originalMetadataTags = listOf("Superhero", "Parody", "Comedy", "Action")
            ),
            Book(
                title = "Vinland Saga",
                author = "Makoto Yukimura",
                coverColor = Color(0xFFC0C0C0),
                progress = 0f,
                tags = listOf("seinen", "historical", "drama", "adventure", "ongoing", "manga"),
                originalMetadataTags = listOf("Historical", "Vikings", "War", "Drama", "Mature")
            ),
            Book(
                title = "Kaguya-sama",
                author = "Aka Akasaka",
                coverColor = Color(0xFFFF69B4),
                progress = 0f,
                tags = listOf("seinen", "romance", "comedy", "school", "completed", "manga"),
                originalMetadataTags = listOf("Romance", "Comedy", "Psychological", "School")
            ),
            // More Manga with tags
            Book(
                title = "Dr. Stone",
                author = "Riichiro Inagaki",
                coverColor = Color(0xFF00CED1),
                progress = 0f,
                tags = listOf("shounen", "sci-fi", "adventure", "comedy", "ongoing", "manga"),
                originalMetadataTags = listOf("Science Fiction", "Post-Apocalyptic", "Comedy", "Educational")
            ),
            Book(
                title = "Attack on Titan",
                author = "Hajime Isayama",
                coverColor = Color(0xFF8B4513),
                progress = 0f,
                tags = listOf("shounen", "action", "drama", "military", "completed", "manga"),
                originalMetadataTags = listOf("Dark Fantasy", "Military", "Political", "Titans")
            ),
            Book(
                title = "Death Note",
                author = "Tsugumi Ohba",
                coverColor = Color(0xFF000000),
                progress = 0f,
                tags = listOf("shounen", "thriller", "supernatural", "psychological", "completed", "manga"),
                originalMetadataTags = listOf("Psychological", "Supernatural", "Crime", "Thriller")
            ),
            Book(
                title = "My Hero Academia",
                author = "Kohei Horikoshi",
                coverColor = Color(0xFF32CD32),
                progress = 0f,
                tags = listOf("shounen", "superhero", "action", "school", "ongoing", "manga"),
                originalMetadataTags = listOf("Superhero", "School", "Coming of Age", "Quirks")
            ),
            
            // Korean Manhwa
            Book(
                title = "Solo Leveling",
                author = "Chugong",
                coverColor = Color(0xFF9400D3),
                progress = 0f,
                tags = listOf("action", "fantasy", "adventure", "completed", "manhwa"),
                originalMetadataTags = listOf("Leveling System", "Dungeons", "Monsters", "OP MC")
            ),
            Book(
                title = "Tower of God",
                author = "SIU",
                coverColor = Color(0xFF2E8B57),
                progress = 0f,
                tags = listOf("action", "adventure", "fantasy", "mystery", "ongoing", "manhwa"),
                originalMetadataTags = listOf("Tower", "Climbing", "Complex Plot", "Friendship")
            ),
            Book(
                title = "The God of High School",
                author = "Yongje Park",
                coverColor = Color(0xFFFF6347),
                progress = 0f,
                tags = listOf("action", "martial-arts", "supernatural", "competition", "completed", "manhwa"),
                originalMetadataTags = listOf("Martial Arts", "Tournament", "Gods", "High School")
            ),
            Book(
                title = "Noblesse",
                author = "Son Jeho",
                coverColor = Color(0xFF8B0000),
                progress = 0f,
                tags = listOf("action", "supernatural", "vampire", "school", "completed", "manhwa"),
                originalMetadataTags = listOf("Vampires", "Nobles", "Modern Day", "School Life")
            ),
            Book(
                title = "Hardcore Leveling Warrior",
                author = "Sehoon Kim",
                coverColor = Color(0xFF4B0082),
                progress = 0f,
                tags = listOf("action", "gaming", "fantasy", "comedy", "ongoing", "manhwa"),
                originalMetadataTags = listOf("Virtual Reality", "Gaming", "Redemption", "Competition")
            ),
            
            // Chinese Manhua
            Book(
                title = "Tales of Demons and Gods",
                author = "Mad Snail",
                coverColor = Color(0xFFFF4500),
                progress = 0f,
                tags = listOf("action", "fantasy", "cultivation", "reincarnation", "ongoing", "manhua"),
                originalMetadataTags = listOf("Cultivation", "Reincarnation", "Demons", "Gods", "Xianxia")
            ),
            Book(
                title = "Battle Through the Heavens",
                author = "Tiancan Tudou",
                coverColor = Color(0xFFDC143C),
                progress = 0f,
                tags = listOf("action", "fantasy", "cultivation", "romance", "ongoing", "manhua"),
                originalMetadataTags = listOf("Cultivation", "Alchemy", "Romance", "Revenge")
            ),
            Book(
                title = "Martial Peak",
                author = "Momo",
                coverColor = Color(0xFF228B22),
                progress = 0f,
                tags = listOf("action", "fantasy", "cultivation", "martial-arts", "ongoing", "manhua"),
                originalMetadataTags = listOf("Martial Arts", "Cultivation", "Peak", "Journey")
            ),
            
            // Web Comics/Webtoons
            Book(
                title = "UnOrdinary",
                author = "uru-chan",
                coverColor = Color(0xFF9932CC),
                progress = 0f,
                tags = listOf("action", "school", "superhero", "drama", "ongoing", "webtoon"),
                originalMetadataTags = listOf("School", "Superpowers", "Hierarchy", "Social Commentary")
            ),
            Book(
                title = "Lore Olympus",
                author = "Rachel Smythe",
                coverColor = Color(0xFFFF69B4),
                progress = 0f,
                tags = listOf("romance", "fantasy", "mythology", "drama", "ongoing", "webtoon"),
                originalMetadataTags = listOf("Greek Mythology", "Romance", "Modern Retelling", "Hades")
            ),
            Book(
                title = "Castle Swimmer",
                author = "Wendy Lian Martin",
                coverColor = Color(0xFF00CED1),
                progress = 0f,
                tags = listOf("fantasy", "adventure", "lgbtq", "romance", "ongoing", "webtoon"),
                originalMetadataTags = listOf("Underwater", "Prophecy", "LGBT", "Adventure")
            ),
            
            // Light Novels
            Book(
                title = "That Time I Got Reincarnated as a Slime",
                author = "Fuse",
                coverColor = Color(0xFF32CD32),
                progress = 0f,
                tags = listOf("fantasy", "isekai", "comedy", "adventure", "ongoing", "light-novel"),
                originalMetadataTags = listOf("Isekai", "Reincarnation", "Slime", "Nation Building")
            ),
            Book(
                title = "Overlord",
                author = "Kugane Maruyama",
                coverColor = Color(0xFF2F4F4F),
                progress = 0f,
                tags = listOf("fantasy", "isekai", "dark", "adventure", "ongoing", "light-novel"),
                originalMetadataTags = listOf("Isekai", "VRMMO", "Undead", "World Domination")
            )
        )
    }

    fun getReadingBooks(): List<Book> = getAllBooks().filter { 
        it.readingStatus == ReadingStatus.READING 
    }.sortedByDescending { it.lastReadTimestamp }

    fun getPlanToReadBooks(): List<Book> = getAllBooks().filter { 
        it.readingStatus == ReadingStatus.PLAN_TO_READ 
    }.sortedBy { it.title }

    fun getCompletedBooks(): List<Book> = getAllBooks().filter { 
        it.readingStatus == ReadingStatus.COMPLETED 
    }.sortedBy { it.title }

    fun getRecentBooks(): List<Book> = getAllBooks().filter { 
        it.lastReadTimestamp > 0 
    }.sortedByDescending { it.lastReadTimestamp }
}