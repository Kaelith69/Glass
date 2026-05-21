package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
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
import com.example.data.MovieListEntity
import com.example.ui.MainViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ListsHubScreen(
    viewModel: MainViewModel,
    onMovieClick: (Int) -> Unit
) {
    val context = LocalContext.current
    val allLists by viewModel.allLists.collectAsState()
    val userLists by viewModel.userMovieLists.collectAsState()
    val allMovies by viewModel.allMovies.collectAsState()

    var showCreateListDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("lists_screen"),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Column {
                    Text(
                        text = "Lists Hub",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Curation sets from CineCommon collaborators",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalIconButton(
                    onClick = { showCreateListDialog = true },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .testTag("create_list_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Create Custom List")
                }
            }
        }

        // Section: System Automatic Lists (Watchlist, Wishlist)
        item {
            Text(
                text = "My Core Vaults",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
            )
        }

        val systemLists = userLists.filter { it.isSystemWatchlist || it.isSystemWishlist }
        if (systemLists.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Register on profile to lock core movie vaults.", style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            items(systemLists) { list ->
                SystemListTile(list = list, allMovies = allMovies, onMovieClick = onMovieClick)
            }
        }

        // Section: Community Custom Lists
        item {
            Text(
                text = "Community Lists",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 10.dp)
            )
        }

        val customLists = allLists.filter { !it.isSystemWatchlist && !it.isSystemWishlist }
        if (customLists.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No custom catalogs written yet. Be the first Architect!",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            items(customLists) { list ->
                CustomListCard(
                    list = list,
                    allMovies = allMovies,
                    onPinClick = { viewModel.togglePinCustomList(list.listId) },
                    onMovieClick = onMovieClick
                )
            }
        }
    }

    // Modal to create custom movie list
    if (showCreateListDialog) {
        AlertDialog(
            onDismissRequest = { showCreateListDialog = false },
            confirmButton = {},
            dismissButton = {},
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.testTag("create_list_dialog"),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Create Custom Catalog", fontWeight = FontWeight.Bold)
                    IconButton(onClick = { showCreateListDialog = false }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }
            },
            text = {
                var nameText by remember { mutableStateOf("") }
                var descText by remember { mutableStateOf("") }
                
                // Selectable movies to add
                val selectedMovieIds = remember { mutableStateListOf<Int>() }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        label = { Text("List Name") },
                        modifier = Modifier.fillMaxWidth().testTag("custom_list_name")
                    )

                    OutlinedTextField(
                        value = descText,
                        onValueChange = { descText = it },
                        label = { Text("List Description") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Select Movies to Add",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )

                    allMovies.forEach { movie ->
                        val isChecked = selectedMovieIds.contains(movie.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) selectedMovieIds.remove(movie.id)
                                    else selectedMovieIds.add(movie.id)
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {
                                    if (isChecked) selectedMovieIds.remove(movie.id)
                                    else selectedMovieIds.add(movie.id)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "${movie.title} (${movie.releaseYear})", maxLines = 1)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (nameText.isBlank()) {
                                Toast.makeText(context, "List Name is required.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.createCustomMovieList(
                                name = nameText,
                                description = descText,
                                movieIdsStr = selectedMovieIds.joinToString(",")
                            )
                            Toast.makeText(context, "'$nameText' customized successfully!", Toast.LENGTH_SHORT).show()
                            showCreateListDialog = false
                        },
                        modifier = Modifier.fillMaxWidth().testTag("custom_list_submit"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Publish Community Catalog", fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    }
}

@Composable
fun SystemListTile(
    list: MovieListEntity,
    allMovies: List<com.example.data.MovieEntity>,
    onMovieClick: (Int) -> Unit
) {
    val listMovieIds = remember(list) { list.movieIds.split(",").filter { it.isNotEmpty() }.map { it.toInt() } }
    val displayMovies = remember(listMovieIds, allMovies) { allMovies.filter { listMovieIds.contains(it.id) } }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = list.listName.uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "${displayMovies.size} Movies",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (displayMovies.isEmpty()) {
                Text(
                    text = "No movies added yet. Flip card backs and tap 'Add to Watchlist/Wishlist'.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    displayMovies.forEach { movie ->
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(8.dp)
                                        )
                                .clickable { onMovieClick(movie.id) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = movie.title,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomListCard(
    list: MovieListEntity,
    allMovies: List<com.example.data.MovieEntity>,
    onPinClick: () -> Unit,
    onMovieClick: (Int) -> Unit
) {
    val listMovieIds = remember(list) { list.movieIds.split(",").filter { it.isNotEmpty() }.map { it.toInt() } }
    val displayMovies = remember(listMovieIds, allMovies) { allMovies.filter { listMovieIds.contains(it.id) } }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = list.listName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Curator: @${list.creatorUsername}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onPinClick) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Pin List",
                        tint = if (list.isPinnedByMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            if (list.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = list.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                displayMovies.take(3).forEach { movie ->
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onMovieClick(movie.id) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = movie.title,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (displayMovies.size > 3) {
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(text = "+${displayMovies.size - 3}", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
