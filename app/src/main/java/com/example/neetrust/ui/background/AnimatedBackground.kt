// [KATALOG] ui.background.AnimatedBackground
package com.example.neetrust.ui.background

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.example.neetrust.ui.config.BackgroundPreset
import com.example.neetrust.ui.theme.NeetRust
import com.example.neetrust.ui.theme.NeetRustColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private class Particle(
    val x: Float,      // posisi awal 0..1
    val y: Float,      // posisi awal 0..1
    val size: Float,   // faktor ukuran
    val speed: Float,  // faktor kecepatan 0.5..1.5
    val phase: Float   // fase kedip/gerak
)

/**
 * Pembungkus layar: menggambar warna latar + animasi sesuai preset,
 * lalu menaruh isi layar di atasnya.
 *
 * Pemakaian di MainActivity:
 *   NeetRustTheme(themeStore.settings) {
 *       NeetRustBackground { ...isi aplikasi... }
 *   }
 * Scaffold di dalamnya harus containerColor = Color.Transparent.
 */
@Composable
fun NeetRustBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = NeetRust.colors
    val settings = NeetRust.settings
    val preset = settings.background
    val animate = settings.animationsEnabled && preset != BackgroundPreset.NONE

    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(animate) {
        if (!animate) return@LaunchedEffect
        var last = 0L
        while (true) {
            withFrameNanosCompat { now ->
                if (last != 0L) time += (now - last) / 1_000_000_000f
                last = now
            }
        }
    }

    val particles = remember(preset) {
        val count = when (preset) {
            BackgroundPreset.STARS -> 70
            BackgroundPreset.FIREFLIES -> 22
            else -> 0
        }
        val r = Random(42)
        List(count) {
            Particle(
                x = r.nextFloat(),
                y = r.nextFloat(),
                size = 0.6f + r.nextFloat() * 1.4f,
                speed = 0.5f + r.nextFloat(),
                phase = r.nextFloat() * (2f * PI.toFloat())
            )
        }
    }

    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val t = time
            drawGlow(colors)
            when (preset) {
                BackgroundPreset.STARS -> drawStars(particles, colors, t)
                BackgroundPreset.WAVES -> drawWaves(colors, t)
                BackgroundPreset.FIREFLIES -> drawFireflies(particles, colors, t)
                BackgroundPreset.NONE -> Unit
            }
        }
        content()
    }
}

/** Cahaya gradien di bagian atas (hanya terlihat pada aksen VIOLET). */
private fun DrawScope.drawGlow(colors: NeetRustColors) {
    if (colors.glow == Color.Transparent) return
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(colors.glow.copy(alpha = 0.6f), Color.Transparent),
            startY = 0f,
            endY = size.height * 0.55f
        )
    )
}

private fun DrawScope.drawStars(particles: List<Particle>, colors: NeetRustColors, t: Float) {
    val w = size.width
    val h = size.height
    for (p in particles) {
        val y = ((p.y + t * 0.012f * p.speed) % 1f) * h
        val x = p.x * w + sin(t * 0.5f + p.phase) * 6f
        val twinkle = 0.35f + 0.65f * (0.5f + 0.5f * sin(t * 1.6f + p.phase))
        drawCircle(
            color = colors.primary.copy(alpha = twinkle * 0.7f),
            radius = p.size.dp.toPx(),
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawWaves(colors: NeetRustColors, t: Float) {
    val w = size.width
    val h = size.height
    // (posisi vertikal, amplitudo dp, opasitas)
    val layers = listOf(
        Triple(0.80f, 14f, 0.05f),
        Triple(0.86f, 18f, 0.07f),
        Triple(0.92f, 22f, 0.09f)
    )
    layers.forEachIndexed { i, (base, amp, alpha) ->
        val path = Path().apply {
            moveTo(0f, h)
            var x = 0f
            while (x <= w) {
                val y = h * base +
                    sin(x / w * 2f * PI.toFloat() * (1.5f + i * 0.5f) + t * (0.6f + i * 0.25f) + i) *
                    amp.dp.toPx()
                lineTo(x, y)
                x += 12f
            }
            lineTo(w, h)
            close()
        }
        drawPath(path = path, color = colors.primary.copy(alpha = alpha))
    }
}

private fun DrawScope.drawFireflies(particles: List<Particle>, colors: NeetRustColors, t: Float) {
    val w = size.width
    val h = size.height
    for (p in particles) {
        val x = (p.x + sin(t * 0.15f * p.speed + p.phase) * 0.08f) * w
        val y = (p.y + cos(t * 0.12f * p.speed + p.phase * 1.3f) * 0.08f) * h
        val pulse = 0.5f + 0.5f * sin(t * 1.2f + p.phase)
        val center = Offset(x, y)
        val glowRadius = (6f + p.size * 4f).dp.toPx()
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colors.accent.copy(alpha = 0.55f * pulse), Color.Transparent),
                center = center,
                radius = glowRadius
            ),
            radius = glowRadius,
            center = center
        )
        drawCircle(
            color = colors.accent.copy(alpha = 0.9f * pulse),
            radius = 1.5.dp.toPx(),
            center = center
        )
    }
}

/** Pembungkus tipis agar import tidak menumpuk di atas. */
private suspend fun withFrameNanosCompat(onFrame: (Long) -> Unit) {
    androidx.compose.runtime.withFrameNanos { onFrame(it) }
}
