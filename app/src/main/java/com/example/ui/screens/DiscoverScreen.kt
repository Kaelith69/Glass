package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MovieEntity
import com.example.ui.MainViewModel
import com.example.ui.components.MovieFlipCard

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    viewModel: MainViewModel,
    onMovieClick: (Int) -> Unit,
    onReviewClick: (Int) -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val movies by viewModel.allMovies.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    var activeCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Sci-Fi", "Drama", "Thriller", "Animation", "Comedy")

    // Active screen listing derived from query/filter actions
    val displayMovies = remember(movies, searchResults, query, activeCategory) {
        val baseList = if (query.isNotBlank()) searchResults else movies
        if (activeCategory == "All") {
            baseList
        } else {
            baseList.filter { it.genres.contains(activeCategory, ignoreCase = true) }
        }
    }

    // Modal state for Add Movie card contribution
    var showAddMovieSheet by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("discover_screen"),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Text(
                    text = "Discover",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )

                // Contribute Movie Floating Button
                FilledTonalIconButton(
                    onClick = { showAddMovieSheet = true },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .testTag("add_card_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Movie Card",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Search Bar Search Input
        item {
            SearchBar(
                query = query,
                onQueryChange = {
                    query = it
                    viewModel.performSearch(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )
        }

        // Active Category Slider Chips
        item {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(categories) { category ->
                    val isSelected = activeCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { activeCategory = category },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        if (displayMovies.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🍿",
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isSearching) "Searching cinematic files..." else "No cinematic matches found.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            // Render beautiful Interactive Flip Cards!
            items(displayMovies, key = { it.id }) { movie ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    MovieFlipCard(
                        movie = movie,
                        averageRating = 8.4f, // seeded fallback rating
                        reviewCount = 3,
                        onReviewClick = { onReviewClick(movie.id) },
                        onDetailsClick = { onMovieClick(movie.id) }
                    )
                }
            }
        }
    }

    // Interactive Bottom Sheet Dialog for creating Movie Cards
    if (showAddMovieSheet) {
        AlertDialog(
            onDismissRequest = { showAddMovieSheet = false },
            confirmButton = {},
            dismissButton = {},
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.testTag("add_movie_dialog"),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Propose Movie Card",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )

                    IconButton(onClick = { showAddMovieSheet = false }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }
            },
            text = {
                var titleText by remember { mutableStateOf("") }
                var altTitleText by remember { mutableStateOf("") }
                var yearText by remember { mutableStateOf("") }
                var directorText by remember { mutableStateOf("") }
                var genresText by remember { mutableStateOf("") }
                var plotText by remember { mutableStateOf("") }
                var castText by remember { mutableStateOf("") }

                // Live Duplicate Checking Logic
                val duplicateMatch = remember(titleText, yearText, movies) {
                    val y = yearText.toIntOrNull()
                    if (titleText.trim().length >= 3 && y != null) {
                        movies.find {
                            it.title.equals(titleText.trim(), ignoreCase = true) && it.releaseYear == y
                        }
                    } else null
                }

                val hasDirectorOverlap = remember(directorText, movies) {
                    if (directorText.trim().length >= 4) {
                        movies.any { it.director.equals(directorText.trim(), ignoreCase = true) }
                    } else false
                }

                // Status variables: confidence levels
                val isHighConfidenceDuplicate = duplicateMatch != null
                val isMediumConfidenceWarning = !isHighConfidenceDuplicate && titleText.trim().length >= 3 && (
                    movies.any { it.title.contains(titleText.trim(), ignoreCase = true) } || hasDirectorOverlap
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp)
                        .background(Color.Transparent)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Quick alert logs for duplicate warning triggers!
                    if (isHighConfidenceDuplicate) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    tint = MaterialTheme.colorScheme.error,
                                    contentDescription = "Duplicate Warning"
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Blocked: High confidence duplicate card detected ('${duplicateMatch?.title}' in ${duplicateMatch?.releaseYear}).",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else if (isMediumConfidenceWarning) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFFF9C4)) // soft yellow warning
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    tint = Color(0xFFFBC02D),
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Heuristic Warning: Title overlap or director overlap detected in archives.",
                                    color = Color(0xFF5D4037),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = titleText,
                        onValueChange = { titleText = it },
                        label = { Text("Movie Title *") },
                        modifier = Modifier.fillMaxWidth().testTag("movie_title_input")
                    )

                    OutlinedTextField(
                        value = altTitleText,
                        onValueChange = { altTitleText = it },
                        label = { Text("Alt Titles (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = yearText,
                        onValueChange = { yearText = it.filter { c -> c.isDigit() } },
                        label = { Text("Release Year *") },
                        modifier = Modifier.fillMaxWidth().testTag("movie_year_input")
                    )

                    OutlinedTextField(
                        value = directorText,
                        onValueChange = { directorText = it },
                        label = { Text("Director *") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = genresText,
                        onValueChange = { genresText = it },
                        label = { Text("Genres (Comma-separated) *") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Sci-Fi, Drama") }
                    )

                    OutlinedTextField(
                        value = plotText,
                        onValueChange = { plotText = it },
                        label = { Text("Synopsis Plot *") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = castText,
                        onValueChange = { castText = it },
                        label = { Text("Cast Overlap Members *") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Matthew McC, Anne H") }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val r = viewModel.createMovieCard(
                                title = titleText,
                                releaseYear = yearText.toIntOrNull() ?: 0,
                                director = directorText,
                                plot = plotText,
                                genres = genresText,
                                cast = castText,
                                altTitles = altTitleText
                            )
                            if (r is com.example.ui.CreationResult.Error) {
                                Toast.makeText(context, r.message, Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Community Movie Card Added Successfully!", Toast.LENGTH_SHORT).show()
                                showAddMovieSheet = false
                            }
                        },
                        enabled = !isHighConfidenceDuplicate && titleText.isNotBlank() && yearText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().testTag("add_movie_submit_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add to Collaborative DB", fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search archives...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                
                // Pure text capture
                androidx.compose.foundation.text.BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth().testTag("search_input_field"),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    ),
                    singleLine = true
                )
            }

            if (query.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear",
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onQueryChange("") },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
