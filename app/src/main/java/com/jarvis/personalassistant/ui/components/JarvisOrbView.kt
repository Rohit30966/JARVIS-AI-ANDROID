package com.jarvis.personalassistant.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jarvis.personalassistant.ui.theme.JarvisPalette
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun JarvisOrbView(
    isListening: Boolean,
    isSpeaking: Boolean,
    audioRmsLevel: Float = 0.25f,
    orbSize: Dp = 180.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "jarvis_orb_transition")

    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 4500 else 11000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_ring_rotation"
    )

    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 3200 else 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_ring_rotation"
    )

    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening || isSpeaking) 700 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_pulse"
    )

    val activeColor = when {
        isListening -> JarvisPalette.NeonTeal
        isSpeaking -> JarvisPalette.ArcCyan
        else -> JarvisPalette.HologramBlue
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(orbSize)) {
        Canvas(modifier = Modifier.size(orbSize)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.minDimension / 2f

            // Outer radial holographic aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        activeColor.copy(alpha = 0.35f * corePulse),
                        JarvisPalette.ArcCyan.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxRadius
                ),
                radius = maxRadius,
                center = center
            )

            // Outersegmented reactor ring
            rotate(degrees = outerRotation, pivot = center) {
                for (segment in 0 until 6) {
                    drawArc(
                        color = JarvisPalette.ArcCyan.copy(alpha = 0.85f),
                        startAngle = segment * 60f + 8f,
                        sweepAngle = 38f,
                        useCenter = false,
                        topLeft = Offset(center.x - maxRadius * 0.88f, center.y - maxRadius * 0.88f),
                        size = Size(maxRadius * 1.76f, maxRadius * 1.76f),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // Counter-rotating telemetry ring
            rotate(degrees = innerRotation, pivot = center) {
                for (segment in 0 until 4) {
                    drawArc(
                        color = activeColor,
                        startAngle = segment * 90f + 12f,
                        sweepAngle = 55f,
                        useCenter = false,
                        topLeft = Offset(center.x - maxRadius * 0.66f, center.y - maxRadius * 0.66f),
                        size = Size(maxRadius * 1.32f, maxRadius * 1.32f),
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // Inner glowing core
            val dynamicBoost = (audioRmsLevel.coerceIn(0f, 1f) * 0.18f)
            val coreRadius = maxRadius * (0.36f * corePulse + dynamicBoost)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        activeColor,
                        JarvisPalette.DeepVoid
                    ),
                    center = center,
                    radius = coreRadius * 1.3f
                ),
                radius = coreRadius,
                center = center
            )
        }
    }
}

@Composable
fun VoiceWaveformAnimation(
    isActive: Boolean,
    rmsLevel: Float,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "waveform_transition")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveform_phase"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(40.dp)) {
            val barCount = 28
            val spacing = size.width / (barCount + 4)
            val centerY = size.height / 2f
            for (i in 0 until barCount) {
                val wave = if (isActive) {
                    abs(sin(phase + i * 0.45f)) * (0.35f + rmsLevel.coerceIn(0.1f, 1f) * 0.65f)
                } else {
                    0.12f
                }
                val barHeight = (size.height * wave).coerceAtLeast(4.dp.toPx())
                val x = spacing * (i + 2)
                drawLine(
                    color = if (isActive) JarvisPalette.ArcCyan else JarvisPalette.HologramBlue.copy(alpha = 0.45f),
                    start = Offset(x, centerY - barHeight / 2f),
                    end = Offset(x, centerY + barHeight / 2f),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
