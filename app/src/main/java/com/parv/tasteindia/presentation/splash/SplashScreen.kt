package com.parv.tasteindia.presentation.splash

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/*
 * Launch screen.
 *
 * A single Canvas driven by a withFrameNanos loop: no animation library, one frame source, and
 * every value (utensil position/rotation/scale, glow, spice particles) is a pure function of
 * elapsed time — nothing is allocated per frame and the whole thing stops the instant the
 * composable leaves composition (the LaunchedEffect coroutine is cancelled). Purely cosmetic;
 * loads no data.
 *
 * Timeline: a chef's knife slides in from the left edge and a wooden spoon from the right,
 * travelling horizontally at first, then rotating into a crossed X as they reach the shared
 * centre. A very subtle glow + a handful of tiny spice-coloured particles mark the moment they
 * meet, everything settles with a barely-there 0.98 -> 1.0 scale, the "TasteIndia" wordmark fades
 * in underneath, and onFinished() hands off to the welcome page while the NavHost cross-fades.
 */

private const val TRAVEL_START_MS = 220f
private const val ROTATION_START_MS = 550f
private const val TRAVEL_END_MS = 1_170f
private const val SETTLE_END_MS = 1_570f
private const val BRAND_FADE_START_MS = 1_500f
private const val BRAND_FADE_END_MS = 1_850f
private const val NAVIGATE_AT_MS = 2_000f
private const val TAIL_MS = 500f
private const val MAX_WALL_CLOCK_MS = 5_000f // hard ceiling so a stalled device never gets stuck
private const val MAX_FRAME_DELTA_MS = 48f   // ~3 frames: a hitch pauses the animation, never jumps

private const val UTENSIL_HALF_LENGTH_DP = 78f
private const val VERT_OFFSET_DP = 26f
private const val REF_HALF = 70f // authoring scale the knife/spoon paths are drawn at

private const val KNIFE_REST_DEG = 0f
private const val KNIFE_FINAL_DEG = -45f
private const val SPOON_REST_DEG = 180f
private const val SPOON_FINAL_DEG = 225f

private const val SPICE_PARTICLE_COUNT = 4
private const val TWO_PI = (2.0 * PI).toFloat()

private val KnifeHandleColor = Color(0xFF3F2A1C)
private val KnifeRivetColor = Color(0xFFD9B26B)

private class SpiceParticle(
    val angleRad: Float,
    val distanceDp: Float,
    val sizeDp: Float,
    val delayMs: Float,
    val lifeMs: Float,
    val maxAlpha: Float,
    val color: Color,
)

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val finish by rememberUpdatedState(onFinished)
    val background = MaterialTheme.colorScheme.background
    val onBackground = MaterialTheme.colorScheme.onBackground
    val glowColor = MaterialTheme.colorScheme.primary
    val spiceColors = listOf(
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
    )
    val brandStyle = MaterialTheme.typography.titleLarge

    // Static geometry, built once: two Paths per utensil (different fill per part) authored at
    // REF_HALF scale with the pivot at local (0,0), plus brushes that give the blade a metallic
    // sheen and the spoon a warm wood-grain gradient.
    val knifeBlade = remember { buildKnifeBladePath() }
    val knifeHandle = remember { buildKnifeHandlePath() }
    val spoonBody = remember { buildSpoonPath() }
    val bladeBrush = remember { buildBladeBrush() }
    val spoonBrush = remember { buildSpoonBrush() }
    val particles = remember { buildSpiceParticles(SPICE_PARTICLE_COUNT, System.nanoTime(), spiceColors) }

    val density = LocalDensity.current
    val halfLenPx = remember(density) { with(density) { UTENSIL_HALF_LENGTH_DP.dp.toPx() } }
    val vertOffsetPx = remember(density) { with(density) { VERT_OFFSET_DP.dp.toPx() } }
    val glowBrush = remember(glowColor, halfLenPx) {
        Brush.radialGradient(
            colors = listOf(glowColor.copy(alpha = 0.55f), glowColor.copy(alpha = 0f)),
            radius = halfLenPx * 0.9f,
        )
    }

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
            val cx = size.width / 2f
            val cy = size.height / 2f

            // --- travel: off-screen -> centre, ease-out so it settles rather than snaps -------
            val travelLinear =
                ((t - TRAVEL_START_MS) / (TRAVEL_END_MS - TRAVEL_START_MS)).coerceIn(0f, 1f)
            val travelProgress = easeOutCubic(travelLinear)
            // --- rotation: only ramps in once travel is underway, finishing exactly with it ---
            val rotationProgress = smoothstep(ROTATION_START_MS, TRAVEL_END_MS, t)

            val k = halfLenPx / REF_HALF

            val knifeStartX = -halfLenPx * 1.6f
            val knifeX = lerp(knifeStartX, cx, travelProgress)
            val knifeY = cy - vertOffsetPx * (1f - travelProgress)
            val knifeRotation = lerp(KNIFE_REST_DEG, KNIFE_FINAL_DEG, rotationProgress)

            val spoonStartX = size.width + halfLenPx * 1.6f
            val spoonX = lerp(spoonStartX, cx, travelProgress)
            val spoonY = cy + vertOffsetPx * (1f - travelProgress)
            val spoonRotation = lerp(SPOON_REST_DEG, SPOON_FINAL_DEG, rotationProgress)

            // --- settle: a barely-there 0.98 -> 1.0 scale once the utensils meet at centre ----
            val settleLinear = ((t - TRAVEL_END_MS) / (SETTLE_END_MS - TRAVEL_END_MS)).coerceIn(0f, 1f)
            val settleScale = if (t <= TRAVEL_END_MS) 1f else lerp(0.98f, 1f, easeOutCubic(settleLinear))
            val finalScale = k * settleScale

            // --- impact: soft glow + spice particles, both gated by the same bell curve -------
            val impact = smoothstep(TRAVEL_END_MS - 90f, TRAVEL_END_MS, t) *
                (1f - smoothstep(TRAVEL_END_MS, TRAVEL_END_MS + 320f, t))

            if (impact > 0.01f) {
                drawCircle(
                    brush = glowBrush,
                    radius = halfLenPx * 0.9f,
                    center = Offset(cx, cy),
                    alpha = 0.4f * impact,
                )
            }

            drawKnife(
                blade = knifeBlade,
                handle = knifeHandle,
                bladeBrush = bladeBrush,
                centerX = knifeX,
                centerY = knifeY,
                rotationDeg = knifeRotation,
                scale = finalScale,
            )
            drawSpoon(
                body = spoonBody,
                brush = spoonBrush,
                centerX = spoonX,
                centerY = spoonY,
                rotationDeg = spoonRotation,
                scale = finalScale,
            )

            for (p in particles) {
                val local = t - (TRAVEL_END_MS + p.delayMs)
                if (local <= 0f) continue
                val life = local / p.lifeMs
                if (life >= 1f) continue

                val alpha = p.maxAlpha * fadeCurve(life)
                if (alpha < 0.01f) continue

                val dist = p.distanceDp.dp.toPx() * easeOutCubic(life)
                val px = cx + cos(p.angleRad) * dist
                val py = cy + sin(p.angleRad) * dist
                drawCircle(
                    color = p.color,
                    radius = p.sizeDp.dp.toPx() / 2f,
                    center = Offset(px, py),
                    alpha = alpha,
                )
            }
        }

        val brandAlpha = smoothstep(BRAND_FADE_START_MS, BRAND_FADE_END_MS, elapsedMs)
        if (brandAlpha > 0f) {
            Text(
                text = "TasteIndia",
                style = brandStyle,
                fontWeight = FontWeight.SemiBold,
                color = onBackground,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 96.dp)
                    .alpha(brandAlpha),
            )
        }
    }
}

private fun DrawScope.drawKnife(
    blade: Path,
    handle: Path,
    bladeBrush: Brush,
    centerX: Float,
    centerY: Float,
    rotationDeg: Float,
    scale: Float,
) {
    withTransform({
        translate(centerX, centerY)
        rotate(rotationDeg, pivot = Offset.Zero)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        drawPath(path = handle, color = KnifeHandleColor)
        drawCircle(color = KnifeRivetColor, radius = 2.4f, center = Offset(-52f, 0f))
        drawCircle(color = KnifeRivetColor, radius = 2.4f, center = Offset(-26f, 0f))
        drawPath(path = blade, brush = bladeBrush)
    }
}

private fun DrawScope.drawSpoon(
    body: Path,
    brush: Brush,
    centerX: Float,
    centerY: Float,
    rotationDeg: Float,
    scale: Float,
) {
    withTransform({
        translate(centerX, centerY)
        rotate(rotationDeg, pivot = Offset.Zero)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        drawPath(path = body, brush = brush)
    }
}

/** Chef's knife blade: tapered silhouette from base (x=6) to tip (x=70), pivot at (0,0). */
private fun buildKnifeBladePath(): Path = Path().apply {
    moveTo(6f, -10f)
    cubicTo(30f, -11f, 52f, -7f, 70f, 0f)
    cubicTo(52f, 7f, 30f, 10f, 6f, 10f)
    close()
}

/** Knife handle: rounded bar from the far end (x=-70) up to under the blade base (x=8). */
private fun buildKnifeHandlePath(): Path = Path().apply {
    addRoundRect(
        RoundRect(left = -70f, top = -8f, right = 8f, bottom = 8f, cornerRadius = CornerRadius(6f, 6f)),
    )
}

/** Wooden spoon: an oval bowl unioned with a tapered handle, pivot at (0,0). */
private fun buildSpoonPath(): Path {
    val bowl = Path().apply {
        addOval(Rect(left = -59f, top = -11f, right = -25f, bottom = 11f))
    }
    val handle = Path().apply {
        moveTo(-27f, -6f)
        cubicTo(0f, -7f, 38f, -5.5f, 70f, -3.5f)
        cubicTo(74f, -3f, 74f, 3f, 70f, 3.5f)
        cubicTo(38f, 5.5f, 0f, 7f, -27f, 6f)
        close()
    }
    return Path().apply { op(bowl, handle, PathOperation.Union) }
}

private fun buildBladeBrush(): Brush = Brush.linearGradient(
    colors = listOf(Color(0xFFF5F6F7), Color(0xFFC7CDD1), Color(0xFF9199A0)),
    start = Offset(0f, -11f),
    end = Offset(0f, 11f),
)

private fun buildSpoonBrush(): Brush = Brush.linearGradient(
    colors = listOf(Color(0xFFCB9A66), Color(0xFF9C6B3E)),
    start = Offset(0f, -11f),
    end = Offset(0f, 11f),
)

private fun buildSpiceParticles(count: Int, seed: Long, colors: List<Color>): List<SpiceParticle> {
    val random = Random(seed)
    return List(count) { i ->
        val baseAngle = (i.toFloat() / count) * TWO_PI
        SpiceParticle(
            angleRad = baseAngle + (random.nextFloat() - 0.5f) * 0.8f,
            distanceDp = 14f + random.nextFloat() * 12f,
            sizeDp = 3f + random.nextFloat() * 3f,
            delayMs = random.nextFloat() * 60f,
            lifeMs = 380f + random.nextFloat() * 160f,
            maxAlpha = 0.28f + random.nextFloat() * 0.18f,
            color = colors[i % colors.size],
        )
    }
}

/** Fade in over the first ~20% of life, hold, fade out over the last ~55%. */
private fun fadeCurve(life: Float): Float {
    val fadeIn = smoothstep(0f, 0.2f, life)
    val fadeOut = 1f - smoothstep(0.45f, 1f, life)
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
