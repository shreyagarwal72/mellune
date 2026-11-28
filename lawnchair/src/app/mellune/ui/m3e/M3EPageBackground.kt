package app.mellune.ui.m3e

import android.content.Context
import android.graphics.Matrix
import android.os.Build
import android.os.PowerManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

val LocalBloomEnabled = compositionLocalOf { true }

data class BloomShapeDef(
    val morph: Morph,
    val centerRelX: Float,
    val centerRelY: Float,
    val radiusDp: Float,
    val baseRotation: Float,
    val roleIndex: Int,
    val driftSign: Float,
)

private object BloomShapeCatalog {
    private val morphPairs: List<Pair<RoundedPolygon, RoundedPolygon>> by lazy {
        listOf(
            M3EMaterialShapes.Clover4Leaf.polygon to M3EMaterialShapes.Flower.polygon,
            M3EMaterialShapes.Sunny.polygon to M3EMaterialShapes.Cookie9Sided.polygon,
            M3EMaterialShapes.SoftBurst.polygon to M3EMaterialShapes.Gem.polygon,
            M3EMaterialShapes.Pill.polygon to M3EMaterialShapes.Cookie4Sided.polygon,
            M3EMaterialShapes.Clover8Leaf.polygon to M3EMaterialShapes.Burst.polygon,
        )
    }

    fun createShapesForSeed(pageKey: String): List<BloomShapeDef> {
        val r = java.util.Random(pageKey.hashCode().toLong())
        val p0 = morphPairs[abs(r.nextInt()) % morphPairs.size]
        val p1 = morphPairs[abs(r.nextInt()) % morphPairs.size]
        val p2 = morphPairs[abs(r.nextInt()) % morphPairs.size]
        val p3 = morphPairs[abs(r.nextInt()) % morphPairs.size]

        return listOf(
            BloomShapeDef(
                morph = Morph(p0.first, p0.second),
                centerRelX = 0.08f + r.nextFloat() * 0.06f,
                centerRelY = 0.06f + r.nextFloat() * 0.06f,
                radiusDp = 260f + r.nextFloat() * 40f,
                baseRotation = r.nextFloat() * 360f,
                roleIndex = 1,
                driftSign = 1f,
            ),
            BloomShapeDef(
                morph = Morph(p1.first, p1.second),
                centerRelX = 0.92f - r.nextFloat() * 0.06f,
                centerRelY = 0.90f - r.nextFloat() * 0.06f,
                radiusDp = 290f + r.nextFloat() * 50f,
                baseRotation = r.nextFloat() * 360f,
                roleIndex = 0,
                driftSign = -1f,
            ),
            BloomShapeDef(
                morph = Morph(p2.first, p2.second),
                centerRelX = 0.90f - r.nextFloat() * 0.06f,
                centerRelY = 0.14f + r.nextFloat() * 0.06f,
                radiusDp = 185f + r.nextFloat() * 30f,
                baseRotation = r.nextFloat() * 360f,
                roleIndex = 2,
                driftSign = 1f,
            ),
            BloomShapeDef(
                morph = Morph(p3.first, p3.second),
                centerRelX = 0.06f + r.nextFloat() * 0.05f,
                centerRelY = 0.68f + r.nextFloat() * 0.08f,
                radiusDp = 145f + r.nextFloat() * 25f,
                baseRotation = r.nextFloat() * 360f,
                roleIndex = 3,
                driftSign = -1f,
            ),
        )
    }
}

private fun resolveBloomColorRole(
    roleIndex: Int,
    colorScheme: ColorScheme,
    pageKey: String,
    isDark: Boolean,
): Pair<Color, Float> {
    val effectiveRole = if (roleIndex < 3) {
        (roleIndex + abs(pageKey.hashCode()) % 3) % 3
    } else {
        roleIndex
    }
    return when (effectiveRole) {
        0 -> colorScheme.primaryContainer to if (isDark) 0.42f else 0.48f
        1 -> colorScheme.secondaryContainer to if (isDark) 0.36f else 0.42f
        2 -> colorScheme.tertiaryContainer to if (isDark) 0.32f else 0.38f
        else -> colorScheme.tertiary to if (isDark) 0.13f else 0.15f
    }
}

/**
 * Ambient Material 3 Expressive bloom page background modeled after Spectra.
 */
@Composable
fun M3EPageBackground(
    pageKey: String = "mellune_page",
    modifier: Modifier = Modifier,
) {
    val bloomEnabled = LocalBloomEnabled.current
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f

    if (!bloomEnabled) return

    val context = LocalContext.current
    val density = LocalDensity.current
    val shapes = remember(pageKey) { BloomShapeCatalog.createShapesForSeed(pageKey) }
    val bloomAnim = remember { Animatable(0f) }
    val reduceMotion = rememberReduceMotion(context)
    val batterySaver = remember {
        try {
            (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isPowerSaveMode == true
        } catch (_: Throwable) {
            false
        }
    }
    val allowMotion = !reduceMotion && !batterySaver

    LaunchedEffect(pageKey, allowMotion) {
        if (!allowMotion) {
            bloomAnim.snapTo(1f)
        } else {
            bloomAnim.snapTo(0f)
            bloomAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            )
        }
    }

    val driftProgress = if (allowMotion) {
        rememberInfiniteTransition(label = "bloom_drift").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 24_000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "bloom_drift_progress",
        ).value
    } else {
        0f
    }

    val scratchMatrix = remember { Matrix() }
    val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && allowMotion) {
        Modifier.graphicsLayer {
            try {
                renderEffect = android.graphics.RenderEffect.createBlurEffect(
                    36f,
                    36f,
                    android.graphics.Shader.TileMode.CLAMP,
                ).asComposeRenderEffect()
            } catch (_: Throwable) {
            }
        }
    } else {
        Modifier
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .then(blurModifier)
            .drawWithCache {
                val w = size.width
                val h = size.height
                val baseBg = colorScheme.background
                val topScrim = Brush.verticalGradient(
                    colors = listOf(
                        baseBg.copy(alpha = if (isDark) 0.45f else 0.30f),
                        Color.Transparent,
                    ),
                    startY = 0f,
                    endY = with(density) { 130.dp.toPx() },
                )
                val bottomScrim = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        baseBg.copy(alpha = if (isDark) 0.50f else 0.35f),
                    ),
                    startY = (h - with(density) { 130.dp.toPx() }).coerceAtLeast(0f),
                    endY = h,
                )

                onDrawBehind {
                    drawRect(baseBg)
                    shapes.forEach { shapeDef ->
                        val (roleColor, roleAlpha) = resolveBloomColorRole(
                            roleIndex = shapeDef.roleIndex,
                            colorScheme = colorScheme,
                            pageKey = pageKey,
                            isDark = isDark,
                        )
                        val cx = w * shapeDef.centerRelX
                        val cy = h * shapeDef.centerRelY
                        val morphProgress =
                            (bloomAnim.value * 0.85f + driftProgress * 0.15f * shapeDef.driftSign)
                                .coerceIn(0f, 1f)
                        val breathe =
                            1f + shapeDef.driftSign * 0.03f * sin(driftProgress * PI.toFloat())
                        val animatedRadius = shapeDef.radiusDp.dp.toPx() * bloomAnim.value * breathe
                        val animatedRotation =
                            shapeDef.baseRotation + shapeDef.driftSign * driftProgress * 12f

                        if (animatedRadius > 2f) {
                            rotate(degrees = animatedRotation, pivot = Offset(cx, cy)) {
                                val androidPath = shapeDef.morph.toPath(progress = morphProgress)
                                scratchMatrix.reset()
                                scratchMatrix.postScale(animatedRadius, animatedRadius)
                                scratchMatrix.postTranslate(cx, cy)
                                androidPath.transform(scratchMatrix)
                                val composePath = androidPath.asComposePath()
                                val fillBrush = Brush.radialGradient(
                                    colors = listOf(
                                        roleColor.copy(alpha = roleAlpha),
                                        roleColor.copy(alpha = roleAlpha * 0.45f),
                                        Color.Transparent,
                                    ),
                                    center = Offset(cx, cy),
                                    radius = animatedRadius * 1.15f,
                                )
                                drawPath(path = composePath, brush = fillBrush)
                            }
                        }
                    }
                    drawRect(topScrim)
                    drawRect(bottomScrim)
                }
            },
    ) {}
}
