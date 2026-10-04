package com.shashwat.muzo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.shashwat.muzo.data.auth.AuthUserState
import com.shashwat.muzo.data.model.AppThemeMode
import com.shashwat.muzo.data.model.AudioQuality
import com.shashwat.muzo.ui.theme.MuzoCyan
import com.shashwat.muzo.ui.theme.MuzoPink

@Composable
fun SettingsScreen(
    currentTheme: AppThemeMode,
    currentQuality: AudioQuality,
    userState: AuthUserState = AuthUserState(),
    onSignInWithGoogle: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onSelectTheme: (AppThemeMode) -> Unit,
    onSelectQuality: (AudioQuality) -> Unit,
    onOpenSleepTimer: () -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showFirebaseInfoDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // Account / Firebase Auth Section
        item {
            SettingsCategoryHeader(title = "Account & Authentication")
        }

        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!userState.photoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = userState.photoUrl,
                            contentDescription = userState.displayName,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = MuzoCyan,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userState.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (userState.isSignedIn) userState.email ?: "Signed in with Google" else "Guest Mode (Local Playlists)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (userState.isSignedIn) {
                        OutlinedButton(onClick = onSignOut) {
                            Text("Sign Out")
                        }
                    } else {
                        Button(
                            onClick = onSignInWithGoogle,
                            colors = ButtonDefaults.buttonColors(containerColor = MuzoCyan)
                        ) {
                            Text("Google Sign-In", color = androidx.compose.ui.graphics.Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            SettingsItem(
                icon = Icons.Default.Info,
                title = "Firebase Setup Guide",
                subtitle = if (userState.isFirebaseConfigured) "Firebase is configured" else "Add google-services.json for cloud sync",
                onClick = { showFirebaseInfoDialog = true }
            )
        }

        // Appearance & Audio Section
        item {
            Spacer(modifier = Modifier.height(16.dp))
            SettingsCategoryHeader(title = "Appearance & Audio")
        }

        item {
            SettingsItem(
                icon = Icons.Default.Palette,
                title = "Theme",
                subtitle = when (currentTheme) {
                    AppThemeMode.AMOLED -> "AMOLED (Pure Black)"
                    AppThemeMode.DARK -> "Dark"
                    AppThemeMode.LIGHT -> "Light"
                    AppThemeMode.SYSTEM -> "System Default"
                },
                onClick = { showThemeDialog = true }
            )
        }

        item {
            SettingsItem(
                icon = Icons.Default.HighQuality,
                title = "Streaming Audio Quality",
                subtitle = "${currentQuality.title} (${currentQuality.bitrate})",
                onClick = { showQualityDialog = true }
            )
        }

        item {
            SettingsItem(
                icon = Icons.Default.Timer,
                title = "Sleep Timer",
                subtitle = "Automatically pause music after duration",
                onClick = onOpenSleepTimer
            )
        }

        // Storage & Data Section
        item {
            Spacer(modifier = Modifier.height(16.dp))
            SettingsCategoryHeader(title = "Storage & Data")
        }

        item {
            SettingsItem(
                icon = Icons.Default.DeleteSweep,
                title = "Clear Listening History",
                subtitle = "Delete played songs list",
                onClick = { showClearHistoryDialog = true }
            )
        }

        // About Section
        item {
            Spacer(modifier = Modifier.height(16.dp))
            SettingsCategoryHeader(title = "About Muzo")
        }

        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Muzo for Android (ViMusic Edition)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MuzoCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Native Kotlin • Jetpack Compose • Media3 ExoPlayer",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "100% direct client-side playback using YouTube Music InnerTube and LRCLIB. Zero user-hosted backend servers or private proxies needed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(140.dp))
        }
    }

    // Firebase Info Dialog
    if (showFirebaseInfoDialog) {
        AlertDialog(
            onDismissRequest = { showFirebaseInfoDialog = false },
            title = { Text("Firebase Google OAuth Setup", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "To enable Google OAuth in Muzo:\n\n" +
                                "1. Create a Firebase project at console.firebase.google.com\n" +
                                "2. Add your Android app package: com.aistudio.muzo.kpxvmw\n" +
                                "3. Add your debug SHA-1 key (run ./gradlew signingReport)\n" +
                                "4. Enable Google Sign-In under Authentication\n" +
                                "5. Download google-services.json and place in app/ folder\n\n" +
                                "See FIREBASE_SETUP.md in the project root for full steps!",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showFirebaseInfoDialog = false }) { Text("Got it") }
            }
        )
    }

    // Theme Picker Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Theme", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    AppThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectTheme(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentTheme == mode,
                                onClick = {
                                    onSelectTheme(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (mode) {
                                    AppThemeMode.AMOLED -> "AMOLED (Pure Black)"
                                    AppThemeMode.DARK -> "Dark"
                                    AppThemeMode.LIGHT -> "Light"
                                    AppThemeMode.SYSTEM -> "System Default"
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Close") }
            }
        )
    }

    // Quality Picker Dialog
    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text("Audio Quality", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    AudioQuality.entries.forEach { quality ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectQuality(quality)
                                    showQualityDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentQuality == quality,
                                onClick = {
                                    onSelectQuality(quality)
                                    showQualityDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = quality.title, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = quality.bitrate,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) { Text("Close") }
            }
        )
    }

    // Clear History Dialog
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear History?", fontWeight = FontWeight.Bold) },
            text = { Text("This will clear your recent listening history.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearHistory()
                        showClearHistoryDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsCategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MuzoCyan,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}
