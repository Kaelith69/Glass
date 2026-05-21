package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MovieEditHistoryEntity
import com.example.data.MovieEntity
import com.example.data.ReviewEntity
import com.example.ui.MainViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailsScreen(
    viewModel: MainViewModel,
    movieId: Int,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val movie by viewModel.selectedMovie.collectAsState()
    val editHistory by viewModel.selectedMovieEditHistory.collectAsState()
    val reviews by viewModel.selectedMovieReviews.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    // Trigger state trackers on start
    LaunchedEffect(movieId) {
        viewModel.selectMovie(movieId)
        viewModel.refreshWatchWishStates(movieId)
    }

    val watchlistStates by viewModel.watchlistStates.collectAsState()
    val wishlistStates by viewModel.wishlistStates.collectAsState()

    val isMovieInWatch = watchlistStates[movieId] ?: false
    val isMovieInWish = wishlistStates[movieId] ?: false

    var activeSubSection by remember { mutableStateOf("Deep Metadata") } // Deep Metadata, Reviews, Edit History
    val tabs = listOf("Deep Metadata", "Reviews", "Logs & rollback")

    var showEditMetadataDialog by remember { mutableStateOf(false) }
    var showReviewDialog by remember { mutableStateOf(false) }

    movie?.let { currentMovie ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .testTag("movie_details_screen")
        ) {
            // Top Navigation Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }

                Text(
                    text = currentMovie.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            viewModel.toggleWatchlist(movieId)
                        },
                        modifier = Modifier.testTag("details_watchlist_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            tint = if (isMovieInWatch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            contentDescription = "Watchlist Toggle"
                        )
                    }

                    IconButton(
                        onClick = {
                            viewModel.toggleWishlist(movieId)
                        },
                        modifier = Modifier.testTag("details_wishlist_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            tint = if (isMovieInWish) Color.Red else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            contentDescription = "Wishlist Toggle"
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = currentMovie.title,
                                style = MaterialTheme.typography.displaySmall.copy(letterSpacing = (-1).sp),
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "${currentMovie.releaseYear} // ${currentMovie.director.uppercase()}",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                item {
                    TabRow(
                        selectedTabIndex = tabs.indexOf(activeSubSection),
                        containerColor = Color.Transparent,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        tabs.forEach { title ->
                            Tab(
                                selected = activeSubSection == title,
                                onClick = { activeSubSection = title },
                                text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                            )
                        }
                    }
                }

                item {
                    when (activeSubSection) {
                        "Deep Metadata" -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "SYNOPSIS PLOT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = currentMovie.plot,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                                MetadataRow(label = "Alternate Titles", value = currentMovie.altTitles.ifBlank { "—" })
                                MetadataRow(label = "Genres & subgenres", value = "${currentMovie.genres} • ${currentMovie.subgenres.ifBlank { "Uncategorized" }}")
                                MetadataRow(label = "Cast overlap", value = currentMovie.cast)
                                MetadataRow(label = "Country & Language", value = "${currentMovie.country} (${currentMovie.language})")
                                MetadataRow(label = "Composer", value = currentMovie.composer.ifBlank { "—" })
                                MetadataRow(label = "Tags", value = currentMovie.tags.ifBlank { "—" })

                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                                Text(
                                    text = "CINEMATIC TRIVIA",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                currentMovie.trivia.split("##").forEach { item ->
                                    if (item.isNotBlank()) {
                                        Text(
                                            text = "• $item",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = { showEditMetadataDialog = true },
                                    modifier = Modifier.fillMaxWidth().testTag("edit_metadata_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Contribute/Improve Data", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        "Reviews" -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${reviews.size} Community Reviews",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Button(
                                        onClick = { showReviewDialog = true },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.testTag("write_review_button")
                                    ) {
                                        Text("Log Review (0-10)", fontSize = 12.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                if (reviews.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No reviews signed up. Open edit mode to post yours first!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        reviews.forEach { r ->
                                            ReviewCardItem(review = r, onLikeClick = { viewModel.upvoteReview(movieId, r.username) })
                                        }
                                    }
                                }
                            }
                        }

                        "Logs & rollback" -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = "Metadatabase Audit Ledger",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Roll back erroneous/malicious updates by tapping of logs items",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                if (editHistory.isEmpty()) {
                                    Text(
                                        text = "No collaborative edits recorded yet. Movie remains in Initial State.",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        editHistory.forEach { log ->
                                            LogHistoryRow(
                                                log = log,
                                                onRollbackSelect = {
                                                    val userTrust = currentUser?.trustScore ?: 50
                                                    if (userTrust >= 70) {
                                                        viewModel.rollbackToEditHistory(movieId, log)
                                                        Toast.makeText(context, "Metadata rolled back to previous edit from @${log.editorUsername}!", Toast.LENGTH_SHORT).show()
                                                        onBackClick()
                                                    } else {
                                                        Toast.makeText(context, "Senior Editor status (70+ Trust) required to roll back! Yours is $userTrust.", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showEditMetadataDialog) {
            AlertDialog(
                onDismissRequest = { showEditMetadataDialog = false },
                confirmButton = {},
                dismissButton = {},
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.testTag("edit_movie_dialog"),
                title = { Text("Contribute Movie Information", fontWeight = FontWeight.Bold) },
                text = {
                    var editPlot by remember { mutableStateOf(currentMovie.plot) }
                    var editCast by remember { mutableStateOf(currentMovie.cast) }
                    var editDir by remember { mutableStateOf(currentMovie.director) }
                    var editAlt by remember { mutableStateOf(currentMovie.altTitles) }
                    var editTrivia by remember { mutableStateOf(currentMovie.trivia) }
                    var editTags by remember { mutableStateOf(currentMovie.tags) }
                    var editSummary by remember { mutableStateOf("") }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val userScore = currentUser?.trustScore ?: 50
                        
                        // Trust warning/indicator banners
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (userScore >= 70) Color(0xFFD6B36A).copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                )
                                .padding(10.dp)
                        ) {
                            Text(
                                text = if (userScore >= 70) 
                                    "⚡ Senior Editor rank detected! Your edit is fast-tracked and will be applied instantly."
                                    else "✏️ Note: Since your trust rating is $userScore / 100 (< 70), your edit is buffered in the Moderation Queue for Senior Editor vetting.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (userScore >= 70) Color(0xFFD6B36A) else MaterialTheme.colorScheme.primary
                            )
                        }

                        OutlinedTextField(
                            value = editPlot,
                            onValueChange = { editPlot = it },
                            label = { Text("Synopsis Plot") },
                            modifier = Modifier.fillMaxWidth().testTag("edit_plot_input")
                        )

                        OutlinedTextField(
                            value = editCast,
                            onValueChange = { editCast = it },
                            label = { Text("Cast Overlap") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = editDir,
                            onValueChange = { editDir = it },
                            label = { Text("Director") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = editAlt,
                            onValueChange = { editAlt = it },
                            label = { Text("Alt Titles") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = editTrivia,
                            onValueChange = { editTrivia = it },
                            label = { Text("Trivia (## separated)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = editTags,
                            onValueChange = { editTags = it },
                            label = { Text("Tags") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = editSummary,
                            onValueChange = { editSummary = it },
                            label = { Text("Summary of changes (Required)") },
                            placeholder = { Text("e.g., Spelling fix in cast list") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                if (editSummary.isBlank()) {
                                    Toast.makeText(context, "Please insert a brief summary of your changes!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.contributeMovieUpdate(
                                    movieId = movieId,
                                    plot = editPlot,
                                    cast = editCast,
                                    altTitles = editAlt,
                                    director = editDir,
                                    trivia = editTrivia,
                                    tags = editTags,
                                    summaryOfChanges = editSummary
                                )
                                Toast.makeText(
                                    context, 
                                    if (userScore >= 70) "Edit applied and recorded instantly!" 
                                    else "Submitting edit to Senior Moderation queue...", 
                                    Toast.LENGTH_LONG
                                ).show()
                                showEditMetadataDialog = false
                            },
                            modifier = Modifier.fillMaxWidth().testTag("edit_metadata_submit"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Publish Collab Edit", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }

        if (showReviewDialog) {
            AlertDialog(
                onDismissRequest = { showReviewDialog = false },
                confirmButton = {},
                dismissButton = {},
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.testTag("write_review_dialog"),
                title = { Text("Log Personal Review", fontWeight = FontWeight.Bold) },
                text = {
                    var ratingValue by remember { mutableStateOf(8.0f) }
                    var reviewValue by remember { mutableStateOf("") }
                    var isSpoilerState by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Rating score: ${ratingValue} / 10",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Slider(
                            value = ratingValue,
                            onValueChange = { ratingValue = Math.round(it * 2) / 2.0f },
                            valueRange = 0.0f..10.0f,
                            steps = 19
                        )

                        OutlinedTextField(
                            value = reviewValue,
                            onValueChange = { if (it.length <= 2000) reviewValue = it },
                            label = { Text("Review Text (Max 2000 chars)") },
                            modifier = Modifier.fillMaxWidth().testTag("review_editor_text"),
                            placeholder = { Text("Describe composition qualities, pacing elements...") },
                            supportingText = {
                                Text("${reviewValue.length}/2000 chars")
                            }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Spoiler alert", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Blurs review from direct view.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = isSpoilerState,
                                onCheckedChange = { isSpoilerState = it }
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.submitMovieReview(
                                    movieId = movieId,
                                    rating = ratingValue,
                                    reviewText = reviewValue,
                                    isSpoiler = isSpoilerState
                                )
                                Toast.makeText(context, "Review logged under @${viewModel.currentUsername.value}!", Toast.LENGTH_SHORT).show()
                                showReviewDialog = false
                            },
                            modifier = Modifier.fillMaxWidth().testTag("submit_review_button_confirm"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Lock review card", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun MetadataRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ReviewCardItem(review: ReviewEntity, onLikeClick: () -> Unit) {
    var revealSpoiler by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(review.username.take(1).uppercase(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = review.username.lowercase(),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    text = "${review.rating} ★",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Spoiler Toggle and Blurred visual representation
            if (review.isSpoiler && !revealSpoiler) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                        .clickable { revealSpoiler = true }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Spoiler. Tap to reveal.", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Text(
                    text = review.reviewText,
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LOGGED REVIEW",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    letterSpacing = 1.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onLikeClick() }.padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = "Like Review",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${review.likesCount} LIKES",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
fun LogHistoryRow(
    log: MovieEditHistoryEntity,
    onRollbackSelect: () -> Unit
) {
    val statusColor = when (log.status) {
        "Approved" -> Color(0xFFD6B36A) // Gold
        "Pending" -> Color(0xFFEA9E3C) // Orange
        "Rejected" -> Color(0xFFD32F2F) // Red
        else -> Color(0xFF757575) // Grey for RolledBack
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
        colors = CardDefaults.cardColors(
            containerColor = when (log.status) {
                "Rejected" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                "Pending" -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "@${log.editorUsername}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // Status Badge Pill
                    Box(
                        modifier = Modifier
                            .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = log.status.uppercase(),
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp
                        )
                    }
                }

                if (log.status == "Approved") {
                    TextButton(
                        onClick = onRollbackSelect,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = "Rollback ↺",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = log.summaryOfChanges,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (log.status == "Rejected" && !log.approverUsername.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Decline reason: ${log.approverUsername}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            } else if (log.status == "Approved" && !log.approverUsername.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Approved by Moderator @${log.approverUsername}",
                    fontSize = 9.sp,
                    color = Color(0xFFD6B36A),
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            val dateFormated = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(log.timestamp))
            Text(
                text = "Backup snapshot stamp: $dateFormated",
                fontSize = 8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
