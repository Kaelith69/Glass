package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {
    @Query("SELECT * FROM movies ORDER BY id DESC")
    fun getAllMovies(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE id = :id LIMIT 1")
    suspend fun getMovieById(id: Int): MovieEntity?

    @Query("SELECT * FROM movies WHERE id = :id")
    fun getMovieFlowById(id: Int): Flow<MovieEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovie(movie: MovieEntity): Long

    @Query("DELETE FROM movies WHERE id = :id")
    suspend fun deleteMovieById(id: Int)

    @Query("SELECT * FROM movies WHERE LOWER(title) = LOWER(:title) AND releaseYear = :year LIMIT 1")
    suspend fun findDuplicateExact(title: String, year: Int): MovieEntity?

    @Query("SELECT * FROM movies WHERE LOWER(title) LIKE LOWER(:query) OR LOWER(altTitles) LIKE LOWER(:query)")
    suspend fun searchMovies(query: String): List<MovieEntity>
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username")
    fun getUserFlowByUsername(username: String): Flow<UserEntity?>

    @Query("SELECT * FROM users")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)
}

@Dao
interface ReviewDao {
    @Query("SELECT * FROM reviews WHERE movieId = :movieId ORDER BY timestamp DESC")
    fun getReviewsForMovie(movieId: Int): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM reviews WHERE username = :username ORDER BY timestamp DESC")
    fun getReviewsByUsername(username: String): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM reviews WHERE movieId = :movieId AND username = :username LIMIT 1")
    suspend fun getReviewByMovieAndUser(movieId: Int, username: String): ReviewEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ReviewEntity)

    @Query("DELETE FROM reviews WHERE movieId = :movieId AND username = :username")
    suspend fun deleteReview(movieId: Int, username: String)
}

@Dao
interface MovieListDao {
    @Query("SELECT * FROM movie_lists ORDER BY lastUpdated DESC")
    fun getAllLists(): Flow<List<MovieListEntity>>

    @Query("SELECT * FROM movie_lists WHERE creatorUsername = :username OR isPinnedByMe = 1 ORDER BY lastUpdated DESC")
    fun getListsForUser(username: String): Flow<List<MovieListEntity>>

    @Query("SELECT * FROM movie_lists WHERE creatorUsername = :username AND isSystemWatchlist = 1 LIMIT 1")
    suspend fun getSystemWatchlist(username: String): MovieListEntity?

    @Query("SELECT * FROM movie_lists WHERE creatorUsername = :username AND isSystemWishlist = 1 LIMIT 1")
    suspend fun getSystemWishlist(username: String): MovieListEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: MovieListEntity): Long

    @Query("SELECT * FROM movie_lists WHERE listId = :listId LIMIT 1")
    suspend fun getListById(listId: Int): MovieListEntity?
}

@Dao
interface BadgeDao {
    @Query("SELECT * FROM badges ORDER BY isUnlocked DESC, tier DESC")
    fun getAllBadges(): Flow<List<BadgeEntity>>

    @Query("SELECT * FROM badges WHERE isUnlocked = 1")
    fun getUnlockedBadges(): Flow<List<BadgeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBadges(badges: List<BadgeEntity>)

    @Query("UPDATE badges SET isUnlocked = 1, unlockTime = :time WHERE badgeId = :badgeId")
    suspend fun unlockBadge(badgeId: String, time: Long)

    @Query("SELECT * FROM badges WHERE badgeId = :badgeId LIMIT 1")
    suspend fun getBadgeById(badgeId: String): BadgeEntity?
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead()
}

@Dao
interface EditHistoryDao {
    @Query("SELECT * FROM movie_edit_history WHERE movieId = :movieId ORDER BY timestamp DESC")
    fun getEditHistoryForMovie(movieId: Int): Flow<List<MovieEditHistoryEntity>>

    @Query("SELECT * FROM movie_edit_history WHERE status = 'Pending' ORDER BY timestamp DESC")
    fun getPendingEdits(): Flow<List<MovieEditHistoryEntity>>

    @Query("SELECT * FROM movie_edit_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<MovieEditHistoryEntity>>

    @Query("SELECT * FROM movie_edit_history WHERE id = :id LIMIT 1")
    suspend fun getHistoryItemById(id: Int): MovieEditHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: MovieEditHistoryEntity)
}
