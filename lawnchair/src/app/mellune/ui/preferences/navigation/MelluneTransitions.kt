package app.mellune.ui.preferences.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.graphics.TransformOrigin

private fun <T> petalCalmSpring() = spring<T>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = 380f,
)

private fun <T> petalIntOffsetSpring() = spring<T>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = 380f,
)

fun petalEnterTransition(isRtl: Boolean = false): EnterTransition =
    slideInHorizontally(
        animationSpec = petalIntOffsetSpring(),
        initialOffsetX = { if (isRtl) -(it * 0.5f).toInt() else (it * 0.5f).toInt() },
    ) + scaleIn(
        animationSpec = petalCalmSpring(),
        initialScale = 0.92f,
        transformOrigin = TransformOrigin(0.5f, 0.5f),
    ) + fadeIn(
        animationSpec = petalCalmSpring(),
    )

fun petalExitTransition(isRtl: Boolean = false): ExitTransition =
    slideOutHorizontally(
        animationSpec = petalIntOffsetSpring(),
        targetOffsetX = { if (isRtl) (it * 0.25f).toInt() else -(it * 0.25f).toInt() },
    ) + fadeOut(
        animationSpec = petalCalmSpring(),
    )

fun petalPopEnterTransition(isRtl: Boolean = false): EnterTransition =
    slideInHorizontally(
        animationSpec = petalIntOffsetSpring(),
        initialOffsetX = { if (isRtl) (it * 0.25f).toInt() else -(it * 0.25f).toInt() },
    ) + scaleIn(
        animationSpec = petalCalmSpring(),
        initialScale = 0.95f,
    ) + fadeIn(
        animationSpec = petalCalmSpring(),
    )

fun petalPopExitTransition(isRtl: Boolean = false): ExitTransition =
    slideOutHorizontally(
        animationSpec = petalIntOffsetSpring(),
        targetOffsetX = { if (isRtl) -(it * 0.5f).toInt() else (it * 0.5f).toInt() },
    ) + scaleOut(
        animationSpec = petalCalmSpring(),
        targetScale = 0.92f,
        transformOrigin = TransformOrigin(0.5f, 0.5f),
    ) + fadeOut(
        animationSpec = petalCalmSpring(),
    )

fun petalPredictivePopEnterTransition(isRtl: Boolean = false): EnterTransition =
    slideInHorizontally(
        animationSpec = petalIntOffsetSpring(),
        initialOffsetX = { if (isRtl) it / 3 else -it / 3 },
    ) + scaleIn(
        animationSpec = petalCalmSpring(),
        initialScale = 0.94f,
    ) + fadeIn(
        animationSpec = petalCalmSpring(),
    )

fun petalPredictivePopExitTransition(isRtl: Boolean = false): ExitTransition =
    slideOutHorizontally(
        animationSpec = petalIntOffsetSpring(),
        targetOffsetX = { if (isRtl) -it else it },
    ) + scaleOut(
        animationSpec = petalCalmSpring(),
        targetScale = 0.85f,
        transformOrigin = TransformOrigin(0.5f, 0.5f),
    ) + fadeOut(
        animationSpec = petalCalmSpring(),
    )
