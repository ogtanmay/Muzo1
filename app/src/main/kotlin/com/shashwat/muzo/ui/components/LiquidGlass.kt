package com.shashwat.muzo.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shashwat.muzo.ui.theme.MuzoCyan
import com.shashwat.muzo.ui.theme.MuzoPink
import com.shashwat.muzo.ui.theme.MuzoPurple

/**
 * Muzo's signature Liquid Glass UI components.
 * Provides shimmering translucent glass surfaces, specular light borders,
 * and ambient fluid glows.
 */

@Composable
fun LiquidGlassBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    glowColor: Color = MuzoCyan,
    elevation: Dp = 16.dp,
    borderWidth: Dp = 1.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    val glassFill = if (isDark) {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.14f),
                Color.White.copy(alpha = 0.05f),
                Color.Black.copy(alpha = 0.40f)
            ),
            start = Offset(0f, 0f),
            end = Offset(400f, 600f)
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.85f),
                Color.White.copy(alpha = 0.65f)
            )
        )
    }

    val glassBorder = if (isDark) {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.50f),
                Color.White.copy(alpha = 0.10f),
                glowColor.copy(alpha = 0.45f)
            ),
            start = Offset(0f, 0f),
            end = Offset(300f, 300f)
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.90f),
                Color.Black.copy(alpha = 0.08f)
            )
        )
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = glowColor.copy(alpha = 0.35f),
                spotColor = MuzoPurple.copy(alpha = 0.40f)
            )
            .clip(shape)
            .background(glassFill)
            .border(borderWidth, glassBorder, shape),
        content = content
    )
}

@Composable
fun AmbientLiquidBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_liquid")

    val offsetAnim1 by infiniteTransition.animateFloat(
        initialValue = -80f,
        targetValue = 80f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liquid_orb_1"
    )

    val offsetAnim2 by infiniteTransition.animateFloat(
        initialValue = 60f,
        targetValue = -60f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liquid_orb_2"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Fluid luminous ambient gradients behind the glass
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MuzoPurple.copy(alpha = 0.22f),
                            Color.Transparent
                        ),
                        center = Offset(200f + offsetAnim1, 350f),
                        radius = 800f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MuzoCyan.copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        center = Offset(700f, 900f + offsetAnim2),
                        radius = 750f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MuzoPink.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(400f - offsetAnim2, 1400f),
                        radius = 650f
                    )
                )
        )

        content()
    }
}
