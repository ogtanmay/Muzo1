package com.shashwat.muzo.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.shashwat.muzo.data.model.MuzoItem
import com.shashwat.muzo.ui.theme.MuzoCyan
import com.shashwat.muzo.ui.theme.MuzoPurple

/**
 * Replicates the exact Shashwat-CODING/Muzo floating MiniPlayer and Dual-Pill Glass Dock.
 */

@Composable
fun MiniPlayer(
    currentSong: MuzoItem?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    progress: Float,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onExpandPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (currentSong == null) return

    val glassBg = Brush.linearGradient(
        listOf(
            Color.Black.copy(alpha = 0.40f),
            Color(0xFF141520).copy(alpha = 0.65f)
        )
    )
    val glassBorder = Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = 0.22f),
            Color.White.copy(alpha = 0.08f)
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .shadow(
                elevation = 18.dp,
                shape = RoundedCornerShape(25.dp),
                ambientColor = Color.Black.copy(alpha = 0.45f),
                spotColor = MuzoPurple.copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(25.dp))
            .background(glassBg)
            .border(0.75.dp, glassBorder, RoundedCornerShape(25.dp))
            .clickable(onClick = onExpandPlayer)
            .testTag("mini_player")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Track scrubber line
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = MuzoCyan,
                trackColor = Color.White.copy(alpha = 0.06f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 32x32 Thumbnail (from Muzo mini_player.dart)
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    AsyncImage(
                        model = currentSong.displayThumbnail,
                        contentDescription = currentSong.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Title and Artist
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = currentSong.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentSong.displayArtist,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color.White.copy(alpha = 0.60f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Play / Pause Icon
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("mini_player_play_pause")
                ) {
                    if (isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Next Track Icon
                IconButton(
                    onClick = onNext,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("mini_player_next")
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Exact Muzo Dual-Pill Floating Dock:
 * - Left Glass Pill: Home, Community, Library, Settings
 * - Right Glass Circle: Dedicated Search button / Home toggle button!
 */
@Composable
fun MuzoBottomNav(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSearchActive = selectedIndex == 1
    val glassBg = Brush.linearGradient(
        listOf(
            Color.Black.copy(alpha = 0.45f),
            Color(0xFF141520).copy(alpha = 0.60f)
        )
    )
    val glassBorder = Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = 0.20f),
            Color.White.copy(alpha = 0.08f)
        )
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Main Pill: Home, Community, Library, Settings
        Box(
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(28.dp),
                    ambientColor = Color.Black.copy(alpha = 0.4f),
                    spotColor = MuzoPurple.copy(alpha = 0.35f)
                )
                .clip(RoundedCornerShape(28.dp))
                .background(glassBg)
                .border(0.75.dp, glassBorder, RoundedCornerShape(28.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MuzoNavItem(
                    iconRegular = Icons.Outlined.Home,
                    iconFilled = Icons.Filled.Home,
                    label = "Home",
                    isSelected = selectedIndex == 0,
                    onClick = { onTabSelected(0) }
                )
                MuzoNavItem(
                    iconRegular = Icons.Outlined.People,
                    iconFilled = Icons.Filled.People,
                    label = "Community",
                    isSelected = selectedIndex == 2,
                    onClick = { onTabSelected(2) }
                )
                MuzoNavItem(
                    iconRegular = Icons.Outlined.LibraryMusic,
                    iconFilled = Icons.Filled.LibraryMusic,
                    label = "Library",
                    isSelected = selectedIndex == 3,
                    onClick = { onTabSelected(3) }
                )
                MuzoNavItem(
                    iconRegular = Icons.Outlined.Settings,
                    iconFilled = Icons.Filled.Settings,
                    label = "Settings",
                    isSelected = selectedIndex == 4,
                    onClick = { onTabSelected(4) }
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Right Glass Circle (56x56) - Dedicated Search or Home button from Muzo main_layout.dart
        Box(
            modifier = Modifier
                .size(56.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.4f),
                    spotColor = MuzoCyan.copy(alpha = 0.35f)
                )
                .clip(CircleShape)
                .background(glassBg)
                .border(0.75.dp, glassBorder, CircleShape)
                .clickable {
                    if (isSearchActive) {
                        onTabSelected(0) // Back to Home
                    } else {
                        onTabSelected(1) // Open Search
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = isSearchActive,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "nav_search_toggle"
            ) { searchActive ->
                Icon(
                    imageVector = if (searchActive) Icons.Default.Home else Icons.Default.Search,
                    contentDescription = if (searchActive) "Home" else "Search",
                    tint = if (searchActive) MuzoCyan else Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun MuzoNavItem(
    iconRegular: ImageVector,
    iconFilled: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val activeBg = if (isSelected) {
        RadialGradientBrush(
            colors = listOf(MuzoCyan.copy(alpha = 0.20f), Color.Transparent),
            radius = 60f
        )
    } else {
        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
    }

    Box(
        modifier = Modifier
            .height(48.dp)
            .widthIn(min = 58.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(activeBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isSelected) iconFilled else iconRegular,
                contentDescription = label,
                tint = if (isSelected) MuzoCyan else Color.White.copy(alpha = 0.60f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MuzoCyan else Color.White.copy(alpha = 0.60f),
                maxLines = 1
            )
        }
    }
}

private fun RadialGradientBrush(colors: List<Color>, radius: Float): Brush {
    return Brush.radialGradient(colors = colors, radius = radius)
}
