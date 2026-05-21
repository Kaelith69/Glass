package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NotificationEntity
import com.example.ui.MainViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

// Sealed class representing type-safe screen routes for CineCommon
sealed class Screen {
    object Onboarding : Screen()
    object Home : Screen()
    object Discover : Screen()
    object ListsHub : Screen()
    object Profile : Screen()
    data class MovieDetails(val movieId: Int) : Screen()
}

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CineCommonApp(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CineCommonApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val currentUsername by viewModel.currentUsername.collectAsState()
    val notificationList by viewModel.notifications.collectAsState()
    val isMotionReduced by viewModel.isMotionReduced.collectAsState()

    var activeScreen by remember { mutableStateOf<Screen>(Screen.Onboarding) }
    var previousScreen by remember { mutableStateOf<Screen>(Screen.Onboarding) }

    // Intercept username claims to redirect from Onboarding to Home
    LaunchedEffect(currentUsername) {
        if (currentUsername != null) {
            if (activeScreen == Screen.Onboarding) {
                activeScreen = Screen.Home
            }
        } else {
            activeScreen = Screen.Onboarding
        }
    }

    var showNotificationsSheet by remember { mutableStateOf(false) }

    // Navigation spring specs
    val springSpec = remember(isMotionReduced) {
        if (isMotionReduced) {
            spring<Float>(dampingRatio = 1.0f, stiffness = 800f) // Very minimal slide
        } else {
            spring<Float>(dampingRatio = 0.8f, stiffness = 380f) // Tactile fluid bounce
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentUsername != null && activeScreen != Screen.Onboarding) {
                // Bottom translucent menu
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                        .testTag("bottom_nav_bar")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BottomNavItem(
                            icon = Icons.Default.Home,
                            label = "Home",
                            isSelected = activeScreen == Screen.Home,
                            onClick = { activeScreen = Screen.Home }
                        )

                        BottomNavItem(
                            icon = Icons.Default.Search,
                            label = "Discover",
                            isSelected = activeScreen == Screen.Discover,
                            onClick = { activeScreen = Screen.Discover }
                        )

                        BottomNavItem(
                            icon = Icons.AutoMirrored.Filled.List,
                            label = "Lists",
                            isSelected = activeScreen == Screen.ListsHub,
                            onClick = { activeScreen = Screen.ListsHub }
                        )

                        BottomNavItem(
                            icon = Icons.Default.AccountCircle,
                            label = "Profile",
                            isSelected = activeScreen == Screen.Profile,
                            onClick = { activeScreen = Screen.Profile }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (currentUsername != null) 0.dp else innerPadding.calculateBottomPadding())
        ) {
            // Elegant transitions on screen switches
            AnimatedContent(
                targetState = activeScreen,
                transitionSpec = {
                    fadeIn(animationSpec = spring(stiffness = 400f)) +
                            slideInHorizontally(
                                initialOffsetX = { 200 },
                                animationSpec = spring(stiffness = 300f)
                            ) togetherWith
                            fadeOut(animationSpec = spring(stiffness = 400f)) +
                            slideOutHorizontally(
                                targetOffsetX = { -200 },
                                animationSpec = spring(stiffness = 300f)
                            )
                },
                label = "screen_routing"
            ) { targetScreen ->
                when (targetScreen) {
                    is Screen.Onboarding -> {
                        OnboardingScreen(
                            viewModel = viewModel,
                            onComplete = {
                                activeScreen = Screen.Home
                            }
                        )
                    }

                    is Screen.Home -> {
                        HomeScreen(
                            viewModel = viewModel,
                            onMovieClick = { id -> activeScreen = Screen.MovieDetails(id) },
                            onNotificationsClick = { showNotificationsSheet = true }
                        )
                    }

                    is Screen.Discover -> {
                        DiscoverScreen(
                            viewModel = viewModel,
                            onMovieClick = { id -> activeScreen = Screen.MovieDetails(id) },
                            onReviewClick = { id -> activeScreen = Screen.MovieDetails(id) }
                        )
                    }

                    is Screen.ListsHub -> {
                        ListsHubScreen(
                            viewModel = viewModel,
                            onMovieClick = { id -> activeScreen = Screen.MovieDetails(id) }
                        )
                    }

                    is Screen.Profile -> {
                        ProfileScreen(
                            viewModel = viewModel,
                            onSignOutClick = {
                                viewModel.signOut()
                                activeScreen = Screen.Onboarding
                            }
                        )
                    }

                    is Screen.MovieDetails -> {
                        MovieDetailsScreen(
                            viewModel = viewModel,
                            movieId = targetScreen.movieId,
                            onBackClick = { activeScreen = Screen.Discover }
                        )
                    }
                }
            }
        }
    }

    // Interactive Bottom Sheet Modal for Notification logs
    if (showNotificationsSheet) {
        AlertDialog(
            onDismissRequest = { showNotificationsSheet = false },
            confirmButton = {},
            dismissButton = {},
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.testTag("notifications_dialog"),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("CineCommon Alerts", fontWeight = FontWeight.Bold)
                    IconButton(onClick = { showNotificationsSheet = false }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }
            },
            text = {
                if (notificationList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No notifications registered yet.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(notificationList) { notify ->
                            NotificationRowItem(notify)
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp)
            .testTag("nav_item_${label.lowercase()}")
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun NotificationRowItem(notify: NotificationEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = notify.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(text = notify.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
