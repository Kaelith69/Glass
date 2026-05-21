package com.example.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "cinecommon_database"
        )
        .fallbackToDestructiveMigration(true)
        .build()
    }

    private val movieDao = database.movieDao()
    private val userDao = database.userDao()
    private val reviewDao = database.reviewDao()
    private val listDao = database.movieListDao()
    private val badgeDao = database.badgeDao()
    private val notificationDao = database.notificationDao()
    private val editHistoryDao = database.editHistoryDao()

    // Flow Streams
    val allMoviesFlow: Flow<List<MovieEntity>> = movieDao.getAllMovies()
    val allListsFlow: Flow<List<MovieListEntity>> = listDao.getAllLists()
    val allBadgesFlow: Flow<List<BadgeEntity>> = badgeDao.getAllBadges()
    val unlockedBadgesFlow: Flow<List<BadgeEntity>> = badgeDao.getUnlockedBadges()
    val allNotificationsFlow: Flow<List<NotificationEntity>> = notificationDao.getAllNotifications()
    val pendingEditsFlow: Flow<List<MovieEditHistoryEntity>> = editHistoryDao.getPendingEdits()
    val allHistoryFlow: Flow<List<MovieEditHistoryEntity>> = editHistoryDao.getAllHistory()

    fun getMovieFlow(id: Int): Flow<MovieEntity?> = movieDao.getMovieFlowById(id)
    fun getReviewsForMovieFlow(movieId: Int): Flow<List<ReviewEntity>> = reviewDao.getReviewsForMovie(movieId)
    fun getReviewsByCreatorFlow(username: String): Flow<List<ReviewEntity>> = reviewDao.getReviewsByUsername(username)
    fun getUserFlow(username: String): Flow<UserEntity?> = userDao.getUserFlowByUsername(username)
    fun getAllUsersFlow(): Flow<List<UserEntity>> = userDao.getAllUsersFlow()
    fun getEditHistoryForMovieFlow(movieId: Int): Flow<List<MovieEditHistoryEntity>> = editHistoryDao.getEditHistoryForMovie(movieId)
    fun getListsForUserFlow(username: String): Flow<List<MovieListEntity>> = listDao.getListsForUser(username)

    // User Operations
    suspend fun getUserByUsername(username: String): UserEntity? = withContext(Dispatchers.IO) {
        userDao.getUserByUsername(username)
    }

    suspend fun saveUser(user: UserEntity) = withContext(Dispatchers.IO) {
        userDao.insertUser(user)
    }

    // Movie Operations
    suspend fun getMovieById(id: Int): MovieEntity? = withContext(Dispatchers.IO) {
        movieDao.getMovieById(id)
    }

    private fun parseSerializedState(serialized: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        serialized.split("##").forEach { part ->
            val colonIndex = part.indexOf(":")
            if (colonIndex != -1) {
                val key = part.substring(0, colonIndex).trim()
                val value = part.substring(colonIndex + 1).trim()
                result[key] = value
            }
        }
        return result
    }

    suspend fun proposeOrApplyMovieEdit(
        movieId: Int,
        editorUsername: String,
        proposedTitle: String,
        proposedPlot: String,
        proposedCast: String,
        proposedDirector: String,
        proposedAltTitles: String,
        proposedTrivia: String,
        proposedTags: String,
        proposedPoster: String,
        summaryOfChanges: String
    ): String = withContext(Dispatchers.IO) {
        val user = userDao.getUserByUsername(editorUsername)
        val trust = user?.trustScore ?: 50
        val isAutoApproved = trust >= 70
        
        val movie = movieDao.getMovieById(movieId) ?: return@withContext "Movie not found"
        
        // Log the previous state for rollbacks or records
        val originalState = "Title:${movie.title}##Plot:${movie.plot}##Cast:${movie.cast}##Director:${movie.director}##AltTitles:${movie.altTitles}##Trivia:${movie.trivia}##Tags:${movie.tags}##Poster:${movie.poster}"
        
        if (isAutoApproved) {
            // Apply immediately
            val updatedMovie = movie.copy(
                title = proposedTitle.ifBlank { movie.title },
                plot = proposedPlot.ifBlank { movie.plot },
                cast = proposedCast.ifBlank { movie.cast },
                director = proposedDirector.ifBlank { movie.director },
                altTitles = proposedAltTitles.ifBlank { movie.altTitles },
                trivia = proposedTrivia.ifBlank { movie.trivia },
                tags = proposedTags.ifBlank { movie.tags },
                poster = proposedPoster.ifBlank { movie.poster },
                contributors = if (movie.contributors.contains(editorUsername)) movie.contributors else "${movie.contributors}, $editorUsername"
            )
            movieDao.insertMovie(updatedMovie)
            
            editHistoryDao.insertHistory(
                MovieEditHistoryEntity(
                    movieId = movieId,
                    editorUsername = editorUsername,
                    summaryOfChanges = summaryOfChanges.ifBlank { "Direct metadata edit" },
                    originalStateJson = originalState,
                    status = "Approved"
                )
            )
            
            // Auto approved edit can slightly reward trust points (+2, cap at 100)
            if (user != null && user.trustScore < 100) {
                userDao.insertUser(user.copy(trustScore = minOf(100, user.trustScore + 2)))
            }
            
            return@withContext "Approved"
        } else {
            // Put it into the Moderation Queue
            editHistoryDao.insertHistory(
                MovieEditHistoryEntity(
                    movieId = movieId,
                    editorUsername = editorUsername,
                    summaryOfChanges = summaryOfChanges.ifBlank { "Suggested metadata card enhancement" },
                    originalStateJson = originalState,
                    status = "Pending",
                    proposedTitle = proposedTitle,
                    proposedPlot = proposedPlot,
                    proposedCast = proposedCast,
                    proposedDirector = proposedDirector,
                    proposedAltTitles = proposedAltTitles,
                    proposedTrivia = proposedTrivia,
                    proposedTags = proposedTags,
                    proposedPoster = proposedPoster
                )
            )
            
            // Notify system/moderation
            notificationDao.insertNotification(
                NotificationEntity(
                    title = "Edit Queued for Review",
                    description = "Your edit to '${movie.title}' is pending review by Senior Editors. Your trust score is $trust.",
                    category = "moderation"
                )
            )
            
            return@withContext "Pending"
        }
    }

    suspend fun insertOrUpdateMovie(movie: MovieEntity, editor: String, summaryOfChange: String): Int = withContext(Dispatchers.IO) {
        val existing = movie.id.let { if (it > 0) movieDao.getMovieById(it) else null }
        val id = movieDao.insertMovie(movie).toInt()
        
        val originalState = existing?.let {
            "Title:${it.title}##Plot:${it.plot}##Cast:${it.cast}##Director:${it.director}##AltTitles:${it.altTitles}##Trivia:${it.trivia}##Tags:${it.tags}##Poster:${it.poster}"
        } ?: "New Movie Created"
        
        editHistoryDao.insertHistory(
            MovieEditHistoryEntity(
                movieId = id,
                editorUsername = editor,
                summaryOfChanges = summaryOfChange,
                originalStateJson = originalState,
                status = "Approved"
            )
        )
        id
    }

    suspend fun approvePendingEdit(historyId: Int, approverUsername: String) = withContext(Dispatchers.IO) {
        val historyItem = editHistoryDao.getHistoryItemById(historyId) ?: return@withContext
        if (historyItem.status != "Pending") return@withContext
        
        val movie = movieDao.getMovieById(historyItem.movieId) ?: return@withContext
        
        // 1. Update the movie with the proposed fields
        val updatedMovie = movie.copy(
            title = if (historyItem.proposedTitle.isNotBlank()) historyItem.proposedTitle else movie.title,
            plot = if (historyItem.proposedPlot.isNotBlank()) historyItem.proposedPlot else movie.plot,
            cast = if (historyItem.proposedCast.isNotBlank()) historyItem.proposedCast else movie.cast,
            director = if (historyItem.proposedDirector.isNotBlank()) historyItem.proposedDirector else movie.director,
            altTitles = if (historyItem.proposedAltTitles.isNotBlank()) historyItem.proposedAltTitles else movie.altTitles,
            trivia = if (historyItem.proposedTrivia.isNotBlank()) historyItem.proposedTrivia else movie.trivia,
            tags = if (historyItem.proposedTags.isNotBlank()) historyItem.proposedTags else movie.tags,
            poster = if (historyItem.proposedPoster.isNotBlank()) historyItem.proposedPoster else movie.poster,
            contributors = if (movie.contributors.contains(historyItem.editorUsername)) movie.contributors
                          else "${movie.contributors}, ${historyItem.editorUsername}"
        )
        movieDao.insertMovie(updatedMovie)
        
        // 2. Update the edit history status
        val updatedHistoryItem = historyItem.copy(
            status = "Approved",
            approverUsername = approverUsername,
            timestamp = System.currentTimeMillis()
        )
        editHistoryDao.insertHistory(updatedHistoryItem)
        
        // 3. Increment the editor's trustScore (+10, cap at 100)
        val editor = userDao.getUserByUsername(historyItem.editorUsername)
        if (editor != null) {
            val newScore = minOf(100, editor.trustScore + 10)
            userDao.insertUser(editor.copy(trustScore = newScore))
            
            // Notify editor
            notificationDao.insertNotification(
                NotificationEntity(
                    title = "Edit Approved! 🏆",
                    description = "Your edit to '${movie.title}' was approved by @$approverUsername. Trust raised to $newScore.",
                    category = "moderation"
                )
            )
            
            // Unlock milestone badges based on trust
            if (newScore >= 90) {
                unlockBadge("oscar_snob", historyItem.editorUsername)
            }
        }
    }

    suspend fun rejectPendingEdit(historyId: Int, approverUsername: String, reason: String) = withContext(Dispatchers.IO) {
        val historyItem = editHistoryDao.getHistoryItemById(historyId) ?: return@withContext
        if (historyItem.status != "Pending") return@withContext
        
        val movie = movieDao.getMovieById(historyItem.movieId)
        
        // 1. Log rejection
        val updatedHistoryItem = historyItem.copy(
            status = "Rejected",
            approverUsername = approverUsername,
            summaryOfChanges = "${historyItem.summaryOfChanges} (Rejected: $reason)",
            timestamp = System.currentTimeMillis()
        )
        editHistoryDao.insertHistory(updatedHistoryItem)
        
        // 2. Decrement the editor's trust score (-15, floor at 0)
        val editor = userDao.getUserByUsername(historyItem.editorUsername)
        if (editor != null) {
            val newScore = maxOf(0, editor.trustScore - 15)
            userDao.insertUser(editor.copy(trustScore = newScore))
            
            // Notify editor
            notificationDao.insertNotification(
                NotificationEntity(
                    title = "Edit Rejected ⚠️",
                    description = "Your edit on '${movie?.title ?: "Movie"}' was rejected by @$approverUsername: $reason. Trust score decreased to $newScore.",
                    category = "moderation"
                )
            )
        }
    }

    suspend fun rollbackMovieMetadata(movieId: Int, historyItem: MovieEditHistoryEntity, restorerUsername: String) = withContext(Dispatchers.IO) {
        val movie = movieDao.getMovieById(movieId) ?: return@withContext
        
        // 1. Current state is stored as backup
        val backupStateStr = "Title:${movie.title}##Plot:${movie.plot}##Cast:${movie.cast}##Director:${movie.director}##AltTitles:${movie.altTitles}##Trivia:${movie.trivia}##Tags:${movie.tags}##Poster:${movie.poster}"
        
        // 2. Parse target rollback state
        val stateMap = parseSerializedState(historyItem.originalStateJson)
        
        // 3. Restore complete movie state
        val updatedMovie = if (stateMap.isNotEmpty()) {
            movie.copy(
                title = stateMap["Title"] ?: movie.title,
                plot = stateMap["Plot"] ?: movie.plot,
                cast = stateMap["Cast"] ?: movie.cast,
                director = stateMap["Director"] ?: movie.director,
                altTitles = stateMap["AltTitles"] ?: movie.altTitles,
                trivia = stateMap["Trivia"] ?: movie.trivia,
                tags = stateMap["Tags"] ?: movie.tags,
                poster = stateMap["Poster"] ?: movie.poster,
                contributors = if (movie.contributors.contains(restorerUsername)) movie.contributors else "${movie.contributors}, $restorerUsername"
            )
        } else {
            // Old format fallback parser (just in case)
            val oldPlot = if (historyItem.originalStateJson.contains("Plot: ")) {
                historyItem.originalStateJson.substringAfter("Plot: ").substringBefore(", Cast:")
            } else movie.plot
            movie.copy(plot = oldPlot)
        }
        
        movieDao.insertMovie(updatedMovie)
        
        // 4. Mark history item as RolledBack
        val markedHistory = historyItem.copy(status = "RolledBack", approverUsername = restorerUsername)
        editHistoryDao.insertHistory(markedHistory)
        
        // 5. Apply severe trust penalty to the editor whose edit we rolled back (-15 trust points)
        val badEditor = userDao.getUserByUsername(historyItem.editorUsername)
        if (badEditor != null && historyItem.editorUsername != restorerUsername) {
            val penaltyScore = maxOf(0, badEditor.trustScore - 15)
            userDao.insertUser(badEditor.copy(trustScore = penaltyScore))
            
            // Notify editor
            notificationDao.insertNotification(
                NotificationEntity(
                    title = "Edit Rolled Back ↩️",
                    description = "@$restorerUsername rolled back your edit on '${movie.title}'. Trust rating decreased to $penaltyScore.",
                    category = "moderation"
                )
            )
        }
        
        // 6. Log the rollback action as an approved history item
        editHistoryDao.insertHistory(
            MovieEditHistoryEntity(
                movieId = movieId,
                editorUsername = restorerUsername,
                summaryOfChanges = "Rolled back edits by @${historyItem.editorUsername}",
                originalStateJson = backupStateStr,
                status = "Approved"
            )
        )
    }

    suspend fun checkDuplicateMovie(title: String, year: Int): MovieEntity? = withContext(Dispatchers.IO) {
        movieDao.findDuplicateExact(title, year)
    }

    suspend fun searchMovies(query: String): List<MovieEntity> = withContext(Dispatchers.IO) {
        if (query.isBlank()) {
            return@withContext emptyList()
        }
        movieDao.searchMovies("%$query%")
    }

    // Review Operations
    suspend fun submitReview(review: ReviewEntity) = withContext(Dispatchers.IO) {
        reviewDao.insertReview(review)
        
        // Generate notifications & adjust stats and unlocked badges!
        val movie = movieDao.getMovieById(review.movieId)
        if (movie != null) {
            notificationDao.insertNotification(
                NotificationEntity(
                    title = "Review Logged!",
                    description = "Your 0-10 review for '${movie.title}' is saved.",
                    category = "system"
                )
            )
            
            // Check for Badges unlock
            val existingReviews = reviewDao.getReviewsByUsername(review.username).firstOrNull() ?: emptyList()
            if (existingReviews.size == 1) {
                unlockBadge("first_review", review.username)
            } else if (existingReviews.size >= 5) {
                unlockBadge("review_milestone", review.username)
            }
            
            // Update User Stats in background
            val user = userDao.getUserByUsername(review.username)
            if (user != null) {
                // Approximate Taste DNA computation
                val reviews = reviewDao.getReviewsByUsername(review.username).firstOrNull() ?: emptyList()
                val genreCounts = mutableMapOf<String, Int>()
                reviews.forEach { r ->
                    val m = movieDao.getMovieById(r.movieId)
                    m?.genres?.split(",")?.forEach { g ->
                        val trimmed = g.trim()
                        if (trimmed.isNotEmpty()) {
                            genreCounts[trimmed] = (genreCounts[trimmed] ?: 0) + 1
                        }
                    }
                }
                val dnaText = if (genreCounts.isNotEmpty()) {
                    val total = genreCounts.values.sum().toFloat()
                    genreCounts.entries.joinToString(", ") { "${it.key}: ${((it.value / total) * 100).toInt()}%" }
                } else user.tasteDNA

                userDao.insertUser(
                    user.copy(
                        reviewCount = reviews.size,
                        watchedCount = reviews.size, // in this simple platform, reviews count towards watch histories
                        tasteDNA = dnaText
                    )
                )
            }
        }
    }

    suspend fun likeReview(movieId: Int, authorUsername: String, likerUsername: String) = withContext(Dispatchers.IO) {
        val review = reviewDao.getReviewByMovieAndUser(movieId, authorUsername)
        if (review != null) {
            val updated = review.copy(likesCount = review.likesCount + 1)
            reviewDao.insertReview(updated)
            
            // Send notification to author
            notificationDao.insertNotification(
                NotificationEntity(
                    title = "Review Liked!",
                    description = "$likerUsername liked your review of movie ID $movieId",
                    category = "like"
                )
            )
            
            // Update author's count
            val author = userDao.getUserByUsername(authorUsername)
            if (author != null) {
                userDao.insertUser(author.copy(likedReviewsCount = author.likedReviewsCount + 1))
            }
        }
    }

    // List Operations
    suspend fun createList(list: MovieListEntity) = withContext(Dispatchers.IO) {
        listDao.insertList(list)
    }

    suspend fun getListById(listId: Int): MovieListEntity? = withContext(Dispatchers.IO) {
        listDao.getListById(listId)
    }

    suspend fun togglePinList(listId: Int) = withContext(Dispatchers.IO) {
        val list = listDao.getListById(listId)
        if (list != null) {
            val updated = list.copy(
                isPinnedByMe = !list.isPinnedByMe,
                pinCount = if (!list.isPinnedByMe) list.pinCount + 1 else maxOf(0, list.pinCount - 1)
            )
            listDao.insertList(updated)
        }
    }

    suspend fun toggleMovieInSystemList(username: String, movieId: Int, isWatchlist: Boolean) = withContext(Dispatchers.IO) {
        val list = if (isWatchlist) {
            listDao.getSystemWatchlist(username) ?: MovieListEntity(
                listName = "Watchlist",
                creatorUsername = username,
                isSystemWatchlist = true
            )
        } else {
            listDao.getSystemWishlist(username) ?: MovieListEntity(
                listName = "Wishlist",
                creatorUsername = username,
                isSystemWishlist = true
            )
        }

        val currentIds = list.movieIds.split(",").filter { it.isNotEmpty() }.toMutableList()
        val mIdStr = movieId.toString()
        if (currentIds.contains(mIdStr)) {
            currentIds.remove(mIdStr)
        } else {
            currentIds.add(mIdStr)
        }

        val updatedList = list.copy(
            movieIds = currentIds.joinToString(","),
            lastUpdated = System.currentTimeMillis()
        )
        listDao.insertList(updatedList)
    }

    suspend fun isMovieInSystemList(username: String, movieId: Int, isWatchlist: Boolean): Boolean = withContext(Dispatchers.IO) {
        val list = if (isWatchlist) {
            listDao.getSystemWatchlist(username)
        } else {
            listDao.getSystemWishlist(username)
        } ?: return@withContext false
        list.movieIds.split(",").contains(movieId.toString())
    }

    // Badge Operations
    suspend fun registerBaseBadges() = withContext(Dispatchers.IO) {
        // Pre-feed some excellent badges
        val badges = listOf(
            BadgeEntity("starter_cadet", "Screen Cadet", "Starter", "Claimed username and configured profile.", "🎬", "#4CAF50", false, null, 1),
            BadgeEntity("first_review", "First Light", "Starter", "Logged the first editable review.", "🌅", "#3F51B5", false, null, 1),
            BadgeEntity("review_milestone", "Review Warden", "Milestone", "Wrote 5 complete community reviews.", "✍️", "#FF9800", false, null, 2),
            BadgeEntity("cosmic_explorer", "Cosmic Explorer", "Genre", "Rated multiple sci-fi masterpieces.", "🚀", "#9C27B0", false, null, 2),
            BadgeEntity("slow_burn", "Slow Burn Whisperer", "Culture", "Reviewed pacing-heavy artheatrical masterworks.", "🍵", "#795548", false, null, 3),
            BadgeEntity("cult_follower", "Cult Follower", "Culture", "Reviewed highly niche cinematic selections.", "🧿", "#E91E63", false, null, 3),
            BadgeEntity("celluloid_purist", "Celluloid Purist", "Culture", "Reviewed classical pieces from CineCommon archives.", "🎞️", "#607D8B", false, null, 4),
            BadgeEntity("list_architect", "List Architect", "Milestone", "Crafted a movie list for the CineCommon catalog.", "📊", "#00BCD4", false, null, 2),
            BadgeEntity("streak_keeper", "Streak Keeper", "Milestone", "Daily CineCommon contributions checklist completed.", "🔥", "#FF5722", false, null, 3),
            BadgeEntity("oscar_snob", "Academy Snob", "Culture", "Engaged with multiple Oscar-decorated listings.", "🏆", "#FFD700", false, null, 4)
        )
        badgeDao.insertBadges(badges)
    }

    suspend fun unlockBadge(badgeId: String, username: String) = withContext(Dispatchers.IO) {
        val badge = badgeDao.getBadgeById(badgeId)
        if (badge != null && !badge.isUnlocked) {
            badgeDao.unlockBadge(badgeId, System.currentTimeMillis())
            notificationDao.insertNotification(
                NotificationEntity(
                    title = "Badge Unlocked! 🏆",
                    description = "You earned the and unlocked badge '${badge.title}'!",
                    category = "badge"
                )
            )
            // also append badge to user entity
            val user = userDao.getUserByUsername(username)
            if (user != null) {
                val currentBadges = user.pinnedBadges.split(",").filter { it.isNotEmpty() }.toMutableList()
                if (!currentBadges.contains(badgeId) && currentBadges.size < 6) {
                    currentBadges.add(badgeId)
                }
                userDao.insertUser(user.copy(pinnedBadges = currentBadges.joinToString(",")))
            }
        }
    }

    suspend fun togglePinBadge(badgeId: String, username: String) = withContext(Dispatchers.IO) {
        val user = userDao.getUserByUsername(username) ?: return@withContext
        val currentBadges = user.pinnedBadges.split(",").filter { it.isNotEmpty() }.toMutableList()
        if (currentBadges.contains(badgeId)) {
            currentBadges.remove(badgeId)
        } else {
            if (currentBadges.size >= 6) {
                // Blocked - maximum 6 badges
                notificationDao.insertNotification(
                    NotificationEntity(
                        title = "Pin Limit Reached",
                        description = "You can only pin up to 6 custom badges on your profile.",
                        category = "system"
                    )
                )
                return@withContext
            }
            currentBadges.add(badgeId)
        }
        userDao.insertUser(user.copy(pinnedBadges = currentBadges.joinToString(",")))
    }

    // Movies Preseeding
    suspend fun preseedInitialMovies() = withContext(Dispatchers.IO) {
        val initial = listOf(
            MovieEntity(
                title = "Dune: Part Two",
                altTitles = "Dune 2, Dune Part II",
                releaseYear = 2024,
                language = "English",
                country = "USA",
                runtime = 166,
                plot = "Paul Atreides unites with Chani and the Fremen while seeking revenge against the conspirators who destroyed his family.",
                genres = "Sci-Fi, Adventure, Drama",
                subgenres = "Space Opera, Epic",
                themes = "Revenge, Prophecy, Power",
                cast = "Timothée Chalamet, Zendaya, Rebecca Ferguson, Austin Butler, Florence Pugh",
                director = "Denis Villeneuve",
                writers = "Denis Villeneuve, Jon Spaihts, Frank Herbert",
                cinematographer = "Greig Fraser",
                composer = "Hans Zimmer",
                tags = "Space, Desert, Epic, Masterpiece",
                awards = "Highly Acclaimed, Multiple Technical Nominations",
                contentWarnings = "War violence, Intense situations",
                trivia = "Greig Fraser shot parts in IMAX digital formats using custom Arri Alexa cameras.##Denis Villeneuve noted this part features more action than the first film.",
                trailerLinks = "https://youtube.com/dune_part_two_trailer",
                gallery = "desert_sunset##imperial_armada",
                contributors = "SystemInitiator, CinePurist"
            ),
            MovieEntity(
                title = "Spirited Away",
                altTitles = "Sen to Chihiro no Kamikakushi",
                releaseYear = 2001,
                language = "Japanese",
                country = "Japan",
                runtime = 125,
                plot = "During her family's move to the suburbs, a sullen 10-year-old girl wanders into a world ruled by gods, witches, and spirits, where humans are changed into beasts.",
                genres = "Animation, Fantasy, Family",
                subgenres = "Magic Realism",
                themes = "Greed, Identity, Growth, Spiritualism",
                cast = "Rumi Hiiragi, Miyu Irino, Mari Natsuki, Takashi Naitô",
                director = "Hayao Miyazaki",
                writers = "Hayao Miyazaki",
                cinematographer = "Atsushi Okui",
                composer = "Joe Hisaishi",
                tags = "Ghibli, Classic, Masterpiece, Surreal",
                awards = "Academy Award winner for Best Animated Feature (2002)",
                contentWarnings = "Slightly scary ghost imagery",
                trivia = "Miyazaki wrote the screenplay with his friends' daughters in mind.##It of course remained Japan's highest-grossing film for almost two decades.",
                trailerLinks = "https://youtube.com/spirited_away_trailer",
                gallery = "bathhouse_view##haku_dragon",
                contributors = "SystemInitiator, StudioAdicionado"
            ),
            MovieEntity(
                title = "Interstellar",
                altTitles = "Flora's Letter",
                releaseYear = 2014,
                language = "English",
                country = "USA",
                runtime = 169,
                plot = "When Earth becomes uninhabitable, a team of explorers travels through a wormhole in space in an attempt to ensure humanity's survival.",
                genres = "Sci-Fi, Adventure, Mystery",
                subgenres = "Hard Sci-Fi",
                themes = "Fatherhood, Space Exploration, Relativism, Time Voyage",
                cast = "Matthew McConaughey, Anne Hathaway, Jessica Chastain, Michael Caine, Yurina Nomura",
                director = "Christopher Nolan",
                writers = "Jonathan Nolan, Christopher Nolan",
                cinematographer = "Hoyte van Hoytema",
                composer = "Hans Zimmer",
                tags = "Black Hole, Physics, Wormhole, Tears",
                awards = "Best Visual Effects Academy Award Winner",
                contentWarnings = "Intoxication context, Space peril",
                trivia = "Physicist Kip Thorne acted as scientific consultant to render the black hole 'Gargantua' with actual equations.##The dust storms in the early scenes used real agricultural soil blowing.",
                trailerLinks = "https://youtube.com/interstellar_trailer",
                gallery = "gargantua_render##cornfield_ride",
                contributors = "SystemInitiator, SpaceGeek"
            ),
            MovieEntity(
                title = "Everything Everywhere All at Once",
                altTitles = "EEAAO",
                releaseYear = 2022,
                language = "English, Mandarin",
                country = "USA",
                runtime = 139,
                plot = "A middle-aged Chinese immigrant is swept up into an insane adventure in which she alone can save existence by exploring other universes and connecting with the lives she could have led.",
                genres = "Sci-Fi, Comedy, Action, Drama",
                subgenres = "Multiverse Journey",
                themes = "Nihilism, Family, Asian-American Identity, Generational Gap",
                cast = "Michelle Yeoh, Ke Huy Quan, Stephanie Hsu, Jamie Lee Curtis",
                director = "Daniel Kwan, Daniel Scheinert",
                writers = "Daniel Kwan, Daniel Scheinert",
                cinematographer = "Larkin Seiple",
                composer = "Son Lux",
                tags = "Multiverse, Hot Dogs, Googly Eyes, Bagel",
                awards = "Winner of 7 Academy Awards including Best Picture",
                contentWarnings = "Absurdist humor, Mild adult themes",
                trivia = "The directors completed the VFX editing in their bedrooms during quarantine.##Shirley Kurata styled the colorful and whimsical outfits.",
                trailerLinks = "https://youtube.com/eeaao_trailer",
                gallery = "bagel_void##hot_dog_hands",
                contributors = "SystemInitiator, IndieBuff"
            ),
            MovieEntity(
                title = "Parasite",
                altTitles = "Gisaengchung",
                releaseYear = 2019,
                language = "Korean",
                country = "South Korea",
                runtime = 132,
                plot = "Greed and class discrimination threaten the newly formed symbiotic relationship between the wealthy Park family and the destitute Kim clan.",
                genres = "Thriller, Drama, Comedy",
                subgenres = "Dark Satire, Suspense",
                themes = "Class Divide, Symbiosis, Family Sacrifice, Parasitism",
                cast = "Song Kang-ho, Lee Sun-kyun, Cho Yeo-jeong, Choi Woo-shik",
                director = "Bong Joon Ho",
                writers = "Bong Joon Ho, Han Jin Won",
                cinematographer = "Hong Kyung-pyo",
                composer = "Jung Jae-il",
                tags = "Stairs, Ram-don, Secret Basement, Rainstorm",
                awards = "First non-English language film to win Best Picture at the Oscars",
                contentWarnings = "Sudden bloody violence",
                trivia = "The wealthy Park home was a luxury set custom-built from architectural parameters specified by Bong Joon Ho.##The Scholar's Stone in the film symbolizes wealth and heavy burden.",
                trailerLinks = "https://youtube.com/parasite_trailer",
                gallery = "architectural_living##basement_leak",
                contributors = "SystemInitiator, BongFan"
            )
        )
        
        initial.forEach { movie ->
            val duplicate = movieDao.findDuplicateExact(movie.title, movie.releaseYear)
            if (duplicate == null) {
                movieDao.insertMovie(movie)
            }
        }
    }
}
