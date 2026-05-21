package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.RegistrationResult

@OptIn(ExperimentalLayoutApi::class, ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    viewModel: MainViewModel,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(1) } // 1 for Identity, 2 for Taste/Persona

    // Step 1 State
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Step 2 State
    var bio by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("🎬") }
    var selectedColor by remember { mutableStateOf("#E53935") }
    val selectedTags = remember { mutableStateListOf<String>() }

    val emojis = listOf("🎬", "🍿", "🎞️", "👽", "🦄", "🤠", "🤖", "🕶️", "🐉")
    val colors = listOf("#E53935", "#D84315", "#FBBF24", "#43A047", "#1E88E5", "#8E24AA", "#3949AB", "#546E7A")
    val availableTags = listOf(
        "casual watcher", "historical", "war", "romance", "horror", 
        "body horror", "thriller", "sci fi", "adventure", "action", 
        "indie", "art house", "documentary", "Oscar watcher", 
        "cult cinema", "slow burn lover", "rewatcher"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally(animationSpec = tween(400), initialOffsetX = { fullWidth -> fullWidth }) + fadeIn() togetherWith
                    slideOutHorizontally(animationSpec = tween(400), targetOffsetX = { fullWidth -> -fullWidth }) + fadeOut()
                } else {
                    slideInHorizontally(animationSpec = tween(400), initialOffsetX = { fullWidth -> -fullWidth }) + fadeIn() togetherWith
                    slideOutHorizontally(animationSpec = tween(400), targetOffsetX = { fullWidth -> fullWidth }) + fadeOut()
                }.using(SizeTransform(clip = false))
            },
            label = "onboarding_step_animation",
            modifier = Modifier.weight(1f)
        ) { step ->
            when (step) {
                1 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 32.dp, vertical = 40.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "CineCommon",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Your cinematic identity starts here. Choose a unique handle to connect with the community.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 24.sp
                        )
                        
                        Spacer(modifier = Modifier.height(48.dp))

                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it.replace(" ", "").lowercase() },
                            label = { Text("Unique Username") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("username_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("email_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("password_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
                2 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 32.dp, vertical = 40.dp)
                    ) {
                        Text(
                            text = "Aesthetics & Taste",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Personalize your profile display and select the genres that define your watchlist.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Avatar
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(selectedColor))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = selectedEmoji, fontSize = 48.sp)
                            }
                            
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 5) {
                                    emojis.forEach { emoji ->
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(if (selectedEmoji == emoji) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f) else Color.Transparent)
                                                .clickable { selectedEmoji = emoji },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = emoji, fontSize = 20.sp)
                                        }
                                    }
                                }
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 8) {
                                    colors.forEach { hex ->
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color(android.graphics.Color.parseColor(hex)))
                                                .clickable { selectedColor = hex },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (selectedColor == hex) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it },
                            label = { Text("Short Bio (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            placeholder = { Text("Wanderer in cinema halls...") }
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Text("Taste DNA", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            availableTags.forEach { tag ->
                                val isSelected = selectedTags.contains(tag)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { if (isSelected) selectedTags.remove(tag) else selectedTags.add(tag) },
                                    label = { Text(tag.lowercase()) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Bottom Action Bar
        Surface(
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep == 2) {
                    TextButton(onClick = { currentStep = 1 }) {
                        Text("BACK", letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    onClick = {
                        if (currentStep == 1) {
                            if (username.isBlank() || email.isBlank() || password.isBlank()) {
                                Toast.makeText(context, "All credentials required to continue.", Toast.LENGTH_SHORT).show()
                            } else {
                                currentStep = 2
                            }
                        } else {
                            val regResult = viewModel.claimProfileOnboarding(
                                usernameStr = username,
                                emailStr = email,
                                avatarEmoji = selectedEmoji,
                                avatarColor = selectedColor,
                                bio = bio,
                                selectedTags = selectedTags.toList()
                            )
                            when (regResult) {
                                is RegistrationResult.Success -> {
                                    Toast.makeText(context, "Welcome aboard, $username!", Toast.LENGTH_SHORT).show()
                                    onComplete()
                                }
                                is RegistrationResult.Error -> {
                                    Toast.makeText(context, regResult.message, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.height(56.dp).testTag("submit_button"),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 32.dp)
                ) {
                    if (currentStep == 1) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("CONTINUE", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    } else {
                        Text("FINISH", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                }
            }
        }
    }
}
