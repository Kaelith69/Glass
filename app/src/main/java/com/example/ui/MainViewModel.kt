package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = AppRepository(application)

    // Current logged-in user profile stream
    private val _currentUsername = MutableStateFlow<String?>(null)
    val currentUsername: StateFlow<String?> = _currentUsername.asStateFlow()

    val currentUser: StateFlow<UserEntity?> = _currentUsername
        .flatMapLatest { username ->
            if (username != null) repository.getUserFlow(username) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Global Movies & Discover streams
    val allMovies = repository.allMoviesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private val _searchResults = MutableStateFlow<List<MovieEntity>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    // Active screen elements
    private val _selectedMovieId = MutableStateFlow<Int?>(null)
    val selectedMovieId = _selectedMovieId.asStateFlow()

    val selectedMovie: StateFlow<MovieEntity?> = _selectedMovieId
        .flatMapLatest { id ->
            if (id != null) repository.getMovieFlow(id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedMovieReviews: StateFlow<List<ReviewEntity>> = _selectedMovieId
        .flatMapLatest { id ->
            if (id != null) repository.getReviewsForMovieFlow(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedMovieEditHistory: StateFlow<List<MovieEditHistoryEntity>> = _selectedMovieId
        .flatMapLatest { id ->
            if (id != null) repository.getEditHistoryForMovieFlow(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Profile & Public Profiles
    private val _focusedPublicUsername = MutableStateFlow<String?>(null)
    val focusedPublicUsername = _focusedPublicUsername.asStateFlow()

    val focusedPublicUser: StateFlow<UserEntity?> = _focusedPublicUsername
        .flatMapLatest { username ->
            if (username != null) repository.getUserFlow(username) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val focusedUserReviews: StateFlow<List<ReviewEntity>> = _focusedPublicUsername
        .flatMapLatest { username ->
            if (username != null) repository.getReviewsByCreatorFlow(username) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Badge Gallery / Collective Stream
    val allBadges = repository.allBadgesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Custom Movie Lists Stream
    val allLists = repository.allListsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userMovieLists: StateFlow<List<MovieListEntity>> = _currentUsername
        .flatMapLatest { username ->
            if (username != null) repository.getListsForUserFlow(username) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live notifications
    val notifications = repository.allNotificationsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingEdits: StateFlow<List<MovieEditHistoryEntity>> = repository.pendingEditsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allEditHistory: StateFlow<List<MovieEditHistoryEntity>> = repository.allHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // App Preferences Settings
    val isMotionReduced = MutableStateFlow(false)
    val isSystemAccentEnabled = MutableStateFlow(true)
    val preferredPosterQuality = MutableStateFlow("High Quality") // Highest quality, Selected, Placeholder
    val recommendationsSensitivity = MutableStateFlow(50) // Scale 0-100

    init {
        viewModelScope.launch {
            // Register badges first
            repository.registerBaseBadges()
            // Seed initial items
            repository.preseedInitialMovies()
        }
    }

    // Action Methods
    fun performSearch(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isSearching.value = true
            val results = repository.searchMovies(query)
            _searchResults.value = results
            _isSearching.value = false
        }
    }

    fun selectMovie(id: Int?) {
        _selectedMovieId.value = id
    }

    fun selectPublicProfile(username: String?) {
        _focusedPublicUsername.value = username
    }

    // Attempt to register/claim a globally unique username
    fun claimProfileOnboarding(
        usernameStr: String,
        emailStr: String,
        avatarEmoji: String,
        avatarColor: String,
        bio: String,
        selectedTags: List<String>
    ): RegistrationResult {
        val trimmedUsername = usernameStr.trim()
        if (trimmedUsername.length < 3) {
            return RegistrationResult.Error("Username must be at least 3 characters long.")
        }
        
        // Basic profanity filter
        val forbidden = listOf("admin", "moderator", "staff", "sysop", "spam", "null", "undefined", "system")
        if (forbidden.any { trimmedUsername.lowercase().contains(it) }) {
            return RegistrationResult.Error("Username contains restricted or reserved terms.")
        }

        var result: RegistrationResult = RegistrationResult.Success
        viewModelScope.launch {
            val existing = repository.getUserByUsername(trimmedUsername)
            if (existing != null) {
                result = RegistrationResult.Error("Username already claimed by another cinemuse.")
            } else {
                // Register
                val newUser = UserEntity(
                    username = trimmedUsername,
                    optionalName = trimmedUsername.replaceFirstChar { it.uppercase() },
                    bio = bio.ifBlank { "Cinemuse. Exploring community movies." },
                    avatarEmoji = avatarEmoji,
                    avatarBgColorHex = avatarColor,
                    tasteTags = selectedTags.joinToString(", ")
                )
                repository.saveUser(newUser)
                
                // Also automatically pre-create empty system Lists (Watchlist and Wishlist) for them
                repository.createList(
                    MovieListEntity(
                        listName = "Watchlist",
                        description = "Movies I plan to explore soon",
                        creatorUsername = trimmedUsername,
                        isSystemWatchlist = true
                    )
                )
                repository.createList(
                    MovieListEntity(
                        listName = "Wishlist",
                        description = "Collectible classics and wishlist items",
                        creatorUsername = trimmedUsername,
                        isSystemWishlist = true
                    )
                )

                // Start cadet badge trigger!
                repository.unlockBadge("starter_cadet", trimmedUsername)
                
                _currentUsername.value = trimmedUsername
            }
        }
        return result
    }

    // Log-out option (reset current credentials stream)
    fun signOut() {
        _currentUsername.value = null
    }

    // Submit a review / edit existing review
    fun submitMovieReview(movieId: Int, rating: Float, reviewText: String, isSpoiler: Boolean) {
        val username = _currentUsername.value ?: return
        if (reviewText.length > 2000) return // Over limit restricted!
        
        viewModelScope.launch {
            val review = ReviewEntity(
                movieId = movieId,
                username = username,
                rating = rating, // 0.0 to 10.0, half precision
                reviewText = reviewText,
                isSpoiler = isSpoiler,
                timestamp = System.currentTimeMillis()
            )
            repository.submitReview(review)
        }
    }

    fun upvoteReview(movieId: Int, author: String) {
        val username = _currentUsername.value ?: return
        if (author == username) return // Cannot upvote own reviews!
        viewModelScope.launch {
            repository.likeReview(movieId, author, username)
        }
    }

    // Edit/contribute metadata to movie cards (Community Driven)
    fun contributeMovieUpdate(
        movieId: Int,
        plot: String,
        cast: String,
        altTitles: String,
        director: String,
        trivia: String,
        tags: String,
        posterUrl: String = "",
        summaryOfChanges: String = ""
    ) {
        val username = _currentUsername.value ?: "AnonymousCollaborator"
        viewModelScope.launch {
            repository.proposeOrApplyMovieEdit(
                movieId = movieId,
                editorUsername = username,
                proposedTitle = "", // Keep original title
                proposedPlot = plot,
                proposedCast = cast,
                proposedDirector = director,
                proposedAltTitles = altTitles,
                proposedTrivia = trivia,
                proposedTags = tags,
                proposedPoster = posterUrl,
                summaryOfChanges = summaryOfChanges.ifBlank { "Metadata enhancement proposal" }
            )
            
            // Check list creation badges, etc.
            repository.unlockBadge("streak_keeper", username)
        }
    }

    fun approveEdit(historyId: Int) {
        val username = _currentUsername.value ?: return
        viewModelScope.launch {
            repository.approvePendingEdit(historyId, username)
        }
    }

    fun rejectEdit(historyId: Int, reason: String) {
        val username = _currentUsername.value ?: return
        viewModelScope.launch {
            repository.rejectPendingEdit(historyId, username, reason)
        }
    }

    fun rollbackToEditHistory(movieId: Int, historyItem: MovieEditHistoryEntity) {
        val username = _currentUsername.value ?: "AnonymousRestorer"
        viewModelScope.launch {
            repository.rollbackMovieMetadata(movieId, historyItem, username)
        }
    }

    // Add entirely new movie card (Collaborative Database Creation)
    fun createMovieCard(
        title: String,
        releaseYear: Int,
        director: String,
        plot: String,
        genres: String,
        cast: String,
        altTitles: String = "",
        poster: String = ""
    ): CreationResult {
        if (title.isBlank() || releaseYear < 1880 || director.isBlank() || plot.isBlank() || genres.isBlank() || cast.isBlank()) {
            return CreationResult.Error("Please fill in all mandatory fields to publish a valid movie card.")
        }
        val username = _currentUsername.value ?: "Initiator"
        
        // Immediate return to check duplication state
        viewModelScope.launch {
            val duplicate = repository.checkDuplicateMovie(title, releaseYear)
            if (duplicate != null) {
                // High confidence duplicate detected
                repository.unlockBadge("cult_follower", username) // Reward search behavior
            } else {
                val newMovie = MovieEntity(
                    title = title.trim(),
                    altTitles = altTitles.trim(),
                    releaseYear = releaseYear,
                    director = director.trim(),
                    plot = plot.trim(),
                    genres = genres.trim(),
                    cast = cast.trim(),
                    poster = poster.trim().ifBlank { "custom_movie_card" },
                    contributors = username
                )
                repository.insertOrUpdateMovie(newMovie, username, "First Publish Card")
            }
        }
        return CreationResult.Success
    }

    // User Watchlist / Wishlist helpers
    fun toggleWatchlist(movieId: Int) {
        val username = _currentUsername.value ?: return
        viewModelScope.launch {
            repository.toggleMovieInSystemList(username, movieId, isWatchlist = true)
        }
    }

    fun toggleWishlist(movieId: Int) {
        val username = _currentUsername.value ?: return
        viewModelScope.launch {
            repository.toggleMovieInSystemList(username, movieId, isWatchlist = false)
        }
    }

    // Check movie items
    private val _watchlistStates = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val watchlistStates = _watchlistStates.asStateFlow()

    private val _wishlistStates = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val wishlistStates = _wishlistStates.asStateFlow()

    fun refreshWatchWishStates(movieId: Int) {
        val username = _currentUsername.value ?: return
        viewModelScope.launch {
            val isWatch = repository.isMovieInSystemList(username, movieId, isWatchlist = true)
            val isWish = repository.isMovieInSystemList(username, movieId, isWatchlist = false)
            _watchlistStates.value = _watchlistStates.value + (movieId to isWatch)
            _wishlistStates.value = _wishlistStates.value + (movieId to isWish)
        }
    }

    // Lists operations
    fun createCustomMovieList(name: String, description: String, movieIdsStr: String) {
        val username = _currentUsername.value ?: return
        viewModelScope.launch {
            repository.createList(
                MovieListEntity(
                    listName = name,
                    description = description,
                    creatorUsername = username,
                    movieIds = movieIdsStr
                )
            )
            repository.unlockBadge("list_architect", username)
        }
    }

    fun togglePinCustomList(listId: Int) {
        viewModelScope.launch {
            repository.togglePinList(listId)
        }
    }

    fun toggleBadgePin(badgeId: String) {
        val username = _currentUsername.value ?: return
        viewModelScope.launch {
            repository.togglePinBadge(badgeId, username)
        }
    }
}

sealed interface RegistrationResult {
    object Success : RegistrationResult
    data class Error(val message: String) : RegistrationResult
}

sealed interface CreationResult {
    object Success : CreationResult
    data class Error(val message: String) : CreationResult
}
