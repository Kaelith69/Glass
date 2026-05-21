package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val username: String, // globally unique, profanity filtered, one-time claim
    val optionalName: String = "",
    val bio: String = "",
    val avatarEmoji: String = "🎬",
    val avatarBgColorHex: String = "#FF5722",
    val tasteTags: String = "", // comma-separated, e.g. "historical, sci fi, slow burn lover"
    val pinnedBadges: String = "", // comma-separated IDs (max 6)
    val watchedCount: Int = 0,
    val reviewCount: Int = 0,
    val likedReviewsCount: Int = 0,
    val tasteDNA: String = "Drama: 35%, Sci-Fi: 25%, Action: 20%, Thriller: 20%", // serialized string representing taste analysis
    val trustScore: Int = 50 // collaborative editing trust rating, 0 to 100.
)

@Entity(tableName = "movies")
data class MovieEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val altTitles: String = "",
    val poster: String = "", // local drawable/res identifier or text pattern representing visual canvas
    val backdrop: String = "",
    val releaseYear: Int,
    val language: String = "English",
    val country: String = "USA",
    val runtime: Int = 120, // in minutes
    val plot: String = "",
    val genres: String = "", // comma-separated
    val subgenres: String = "", // comma-separated
    val themes: String = "", // comma-separated
    val cast: String = "", // comma-separated
    val director: String = "",
    val writers: String = "",
    val cinematographer: String = "",
    val composer: String = "",
    val tags: String = "", // comma-separated
    val awards: String = "",
    val contentWarnings: String = "",
    val trivia: String = "", // double-hash (##) separated
    val trailerLinks: String = "",
    val gallery: String = "", // double-hash (##) separated
    val contributors: String = "", // comma-separated contributor usernames
    val qualityScore: Int = 100 // upvoted quality
)

@Entity(tableName = "reviews", primaryKeys = ["movieId", "username"])
data class ReviewEntity(
    val movieId: Int,
    val username: String, // one editable review per movie per user
    val rating: Float, // 0.0 to 10.0, half precision
    val reviewText: String = "", // max 2000 chars
    val isSpoiler: Boolean = false,
    val likesCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "movie_lists")
data class MovieListEntity(
    @PrimaryKey(autoGenerate = true) val listId: Int = 0,
    val listName: String,
    val description: String = "",
    val creatorUsername: String,
    val movieIds: String = "", // comma-separated movie IDs
    val isSystemWatchlist: Boolean = false,
    val isSystemWishlist: Boolean = false,
    val isPinnedByMe: Boolean = false,
    val pinCount: Int = 0,
    val repostCount: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "badges")
data class BadgeEntity(
    @PrimaryKey val badgeId: String,
    val title: String,
    val category: String, // "Starter", "Milestone", "Genre", "Culture"
    val description: String,
    val iconEmoji: String,
    val colorHex: String,
    val isUnlocked: Boolean = false,
    val unlockTime: Long? = null,
    val tier: Int = 1 // 1=Bronze, 2=Silver, 3=Gold, 4=Platinum
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val category: String = "general", // "like", "badge", "system", "moderation"
    val isRead: Boolean = false
)

@Entity(tableName = "movie_edit_history")
data class MovieEditHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val movieId: Int,
    val editorUsername: String,
    val timestamp: Long = System.currentTimeMillis(),
    val summaryOfChanges: String,
    val originalStateJson: String = "", // serialised to support simple rollbacks
    val status: String = "Approved", // "Approved", "Pending", "Rejected", "RolledBack"
    val proposedTitle: String = "",
    val proposedPlot: String = "",
    val proposedCast: String = "",
    val proposedDirector: String = "",
    val proposedAltTitles: String = "",
    val proposedTrivia: String = "",
    val proposedTags: String = "",
    val proposedPoster: String = "",
    val approverUsername: String? = null
)
