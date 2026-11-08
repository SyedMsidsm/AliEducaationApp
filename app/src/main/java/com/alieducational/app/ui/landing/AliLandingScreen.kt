package com.alieducational.app.ui.landing

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alieducational.app.ui.components.AliLogo
import kotlinx.coroutines.delay

@Composable
fun AliLandingScreen(
    isDark: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val animScale = try {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1.0f
        )
    } catch (_: Exception) {
        1.0f
    }

    if (animScale == 0f) {
        LaunchedEffect(Unit) {
            onFinished()
        }
        return
    }

    val sweep = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        sweep.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 850,
                easing = LinearOutSlowInEasing
            )
        )
        delay(100)
        onFinished()
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onFinished
            ),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .width(220.dp)
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                AliLogo(
                    isDark = isDark,
                    modifier = Modifier.fillMaxSize()
                )

                // Gold laser scan line sweeping vertically
                val goldColor = Color(0xFFDCAE41)
                val scanYProgress = sweep.value
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val y = size.height * scanYProgress
                    // Gradient beam behind the line
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                goldColor.copy(alpha = 0f),
                                goldColor.copy(alpha = 0.25f),
                                goldColor.copy(alpha = 0f)
                            ),
                            startY = y - 20f,
                            endY = y + 20f
                        ),
                        topLeft = Offset(0f, y - 20f),
                        size = androidx.compose.ui.geometry.Size(size.width, 40f)
                    )
                    // Sharp scan line
                    drawLine(
                        color = goldColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 3f
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Ali Educational",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Books • Stationery • Uniforms",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
