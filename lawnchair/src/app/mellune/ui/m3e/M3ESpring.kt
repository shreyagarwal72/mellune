package app.mellune.ui.m3e

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext

/**
 * Shared spring specs and transition presets for Mellune's Material 3 Expressive UI.
 */
object M3ESpring {
    val EmphasizedEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

    fun <T> spatial(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    fun <T> calm(): FiniteAnimationSpec<T> = spatial()

    fun <T> quick(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.78f,
        stiffness = Spring.StiffnessMedium,
    )

    fun <T> bouncy(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    fun <T> enter(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.85f,
        stiffness = Spring.StiffnessMediumLow,
    )

    fun <T> exit(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
    )
}

object M3EMotion {
    fun <T> spatialFast(): FiniteAnimationSpec<T> = M3ESpring.quick()
    fun <T> spatialDefault(): FiniteAnimationSpec<T> = M3ESpring.calm()
    fun <T> effectsFast(): FiniteAnimationSpec<T> = M3ESpring.calm()
    fun <T> effectsDefault(): FiniteAnimationSpec<T> = M3ESpring.calm()

    fun forwardEnter(): EnterTransition =
        fadeIn(effectsDefault()) +
            slideInHorizontally(spatialDefault()) { it / 10 } +
            scaleIn(spatialFast(), initialScale = 0.96f)

    fun forwardExit(): ExitTransition =
        fadeOut(effectsFast()) + scaleOut(effectsDefault(), targetScale = 0.985f)

    fun backEnter(): EnterTransition =
        fadeIn(effectsDefault()) +
            slideInHorizontally(spatialDefault()) { -it / 12 } +
            scaleIn(spatialFast(), initialScale = 0.96f)

    fun backExit(): ExitTransition =
        fadeOut(effectsFast()) +
            slideOutHorizontally(spatialDefault()) { it / 12 } +
            scaleOut(effectsDefault(), targetScale = 0.985f)
}

@Composable
fun rememberM3EPressScale(
    interactionSource: MutableInteractionSource,
    targetScale: Float = 0.97f,
): Float {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) targetScale else 1f,
        animationSpec = M3ESpring.quick(),
        label = "m3ePressScale",
    )
    return scale
}

@Composable
fun Modifier.m3ePressScale(
    interactionSource: MutableInteractionSource,
    targetScale: Float = 0.97f,
): Modifier = scale(rememberM3EPressScale(interactionSource, targetScale))

@Composable
fun rememberReduceMotion(context: Context = LocalContext.current): Boolean = remember(context) {
    try {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    } catch (_: Throwable) {
        false
    }
}
