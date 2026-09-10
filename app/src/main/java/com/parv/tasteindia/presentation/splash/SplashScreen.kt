package com.parv.tasteindia.presentation.splash

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/*
 * Launch screen.
 *
 * A single Canvas driven by a withFrameNanos loop: no animation library, one frame source, and
 * every heart's position/opacity/rotation is a pure function of (elapsed time, per-particle
 * spec) — so nothing is allocated per frame and the whole thing stops the instant the composable
 * leaves composition (the LaunchedEffect coroutine is cancelled). Purely cosmetic; loads no data.
 *
 * Timeline: the main heart eases + fades in over the first ~450ms; heart particles rise from the
 * bottom straight away — a few at first, then more — each with its own size / speed / opacity /
 * rotation / horizontal drift; at ~1.9s onFinished() hands off to the welcome page while the
 * NavHost cross-fades (the loop runs a beat longer so the last hearts keep drifting during it).
 */

private const val NAVIGATE_AT_MS = 1_900f
private const val TAIL_MS = 650f
private const val MAX_WALL_CLOCK_MS = 5_000f // hard ceiling so a stalled device never gets stuck
private const val MAX_FRAME_DELTA_MS = 48f   // ~3 frames: a hitch pauses the animation, never jumps
private const val PARTICLE_COUNT = 16
private const val TWO_PI = (2.0 * PI).toFloat()

private class HeartParticle(
    val xFraction: Float,
    val spawnDelayMs: Float,
    val riseDurationMs: Float,
    val sizeDp: Float,
    val maxAlpha: Float,
    val baseRotationDeg: Float,
    val rotationSpeedDeg: Float,
    val driftAmplitudeDp: Float,
    val driftFrequency: Float,
    val driftPhase: Float,
)

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val finish by rememberUpdatedState(onFinished)
    val heartColor = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.background

    // Unit heart path (fits a 0..1 box), scaled per-heart at draw time.
    val heartPath = remember { unitHeartPath() }
    // Stable for this screen's life; a per-launch seed gives subtle variety each time.
    val particles = remember { buildParticles(PARTICLE_COUNT, System.nanoTime()) }
    var elapsedMs by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val firstNanos = withFrameNanos { it }
        var lastNanos = firstNanos
        var animMs = 0f
        var handedOff = false
        while (true) {
            val now = withFrameNanos { it }
            // Advance by a clamped delta: a startup hitch on a slow device pauses the
            // animation for that frame instead of skipping it forward.
            animMs += ((now - lastNanos) / 1_000_000f).coerceIn(0f, MAX_FRAME_DELTA_MS)
            lastNanos = now
            elapsedMs = animMs

            val wallClockMs = (now - firstNanos) / 1_000_000f
            if (!handedOff && (animMs >= NAVIGATE_AT_MS || wallClockMs >= MAX_WALL_CLOCK_MS)) {
                handedOff = true
                finish() // exactly once
            }
            if (animMs >= NAVIGATE_AT_MS + TAIL_MS || wallClockMs >= MAX_WALL_CLOCK_MS + TAIL_MS) break
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .semantics { contentDescription = "TasteIndia" },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val t = elapsedMs
            val w = size.width
            val h = size.height

            // --- main heart: subtle scale + fade in, then held ------------------------------
            val mainProgress = smoothstep(0f, 450f, t)
            if (mainProgress > 0f) {
                val mainPx = 132.dp.toPx()
                val scale = 0.9f + 0.1f * easeOutCubic(mainProgress)
                drawHeart(
                    path = heartPath,
                    centerX = w / 2f,
                    centerY = h / 2f,
                    sizePx = mainPx * scale,
                    rotationDeg = 0f,
                    alpha = mainProgress,
                    color = heartColor,
                )
            }

            // --- floating heart particles --------------------------------------------------
            for (p in particles) {
                val local = t - p.spawnDelayMs
                if (local <= 0f) continue
                val life = local / p.riseDurationMs
                if (life >= 1f) continue

                val alpha = p.maxAlpha * fadeCurve(life)
                if (alpha < 0.01f) continue

                val sizePx = p.sizeDp.dp.toPx()
                val startY = h + sizePx
                val endY = -sizePx
                val centerY = startY + (endY - startY) * easeOutSine(life) // soft, non-linear rise
                val drift = sin(life * p.driftFrequency * TWO_PI + p.driftPhase) *
                    p.driftAmplitudeDp.dp.toPx()
                val centerX = p.xFraction * w + drift
                val rotation = p.baseRotationDeg + life * p.rotationSpeedDeg

                drawHeart(
                    path = heartPath,
                    centerX = centerX,
                    centerY = centerY,
                    sizePx = sizePx,
                    rotationDeg = rotation,
                    alpha = alpha,
                    color = heartColor,
                )
            }
        }
    }
}

private const val HEART_REF = 100f

private fun DrawScope.drawHeart(
    path: Path,
    centerX: Float,
    centerY: Float,
    sizePx: Float,
    rotationDeg: Float,
    alpha: Float,
    color: Color,
) {
    val half = sizePx / 2f
    val k = sizePx / HEART_REF
    withTransform({
        translate(centerX - half, centerY - half)
        rotate(rotationDeg, pivot = Offset(half, half))
        scale(k, k, pivot = Offset.Zero)
    }) {
        drawPath(path = path, color = color, alpha = alpha)
    }
}

/** A heart shape sized to a [HEART_REF]-wide box, tip pointing down. */
private fun unitHeartPath(): Path = Path().apply {
    val s = HEART_REF
    moveTo(0.50f * s, 0.92f * s)
    cubicTo(0.16f * s, 0.66f * s, 0.00f * s, 0.42f * s, 0.00f * s, 0.26f * s)
    cubicTo(0.00f * s, 0.10f * s, 0.12f * s, 0.00f * s, 0.28f * s, 0.00f * s)
    cubicTo(0.40f * s, 0.00f * s, 0.47f * s, 0.07f * s, 0.50f * s, 0.15f * s)
    cubicTo(0.53f * s, 0.07f * s, 0.60f * s, 0.00f * s, 0.72f * s, 0.00f * s)
    cubicTo(0.88f * s, 0.00f * s, 1.00f * s, 0.10f * s, 1.00f * s, 0.26f * s)
    cubicTo(1.00f * s, 0.42f * s, 0.84f * s, 0.66f * s, 0.50f * s, 0.92f * s)
    close()
}

private fun buildParticles(count: Int, seed: Long): List<HeartParticle> {
    val random = Random(seed)
    val earlyCount = 3 // a few hearts visible almost immediately, the rest introduced gradually
    return List(count) { i ->
        val laterProgress =
            if (i < earlyCount) 0f else (i - earlyCount).toFloat() / (count - earlyCount)
        HeartParticle(
            xFraction = 0.08f + random.nextFloat() * 0.84f,
            spawnDelayMs = if (i < earlyCount) {
                random.nextFloat() * 130f
            } else {
                150f + laterProgress * 1250f + (random.nextFloat() - 0.5f) * 180f
            },
            riseDurationMs = 1700f + random.nextFloat() * 1150f,
            sizeDp = 12f + random.nextFloat() * 22f,          // small..large, for depth
            maxAlpha = 0.20f + random.nextFloat() * 0.52f,    // soft, never fully opaque
            baseRotationDeg = -22f + random.nextFloat() * 44f,
            rotationSpeedDeg = -26f + random.nextFloat() * 52f,
            driftAmplitudeDp = 6f + random.nextFloat() * 32f,
            driftFrequency = 0.55f + random.nextFloat() * 1.1f,
            driftPhase = random.nextFloat() * TWO_PI,
        )
    }
}

/** Fade in over the first ~14% of life, hold, fade out over the last ~40%. */
private fun fadeCurve(life: Float): Float {
    val fadeIn = smoothstep(0f, 0.14f, life)
    val fadeOut = 1f - smoothstep(0.60f, 1f, life)
    return fadeIn * fadeOut
}

private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun easeOutCubic(x: Float): Float {
    val u = 1f - x
    return 1f - u * u * u
}

private fun easeOutSine(x: Float): Float = sin(x * (PI.toFloat() / 2f))
