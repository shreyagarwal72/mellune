package app.mellune.ui.components

import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.CrossWindowBlurListeners
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import app.mellune.preferences2.asState
import app.mellune.preferences2.preferenceManager2
import com.android.launcher3.R
import kotlin.math.roundToInt

/**
 * Petal-inspired Material 3 Expressive modal bottom sheet with live Android 12+
 * window blur, frosted glass surface, and specular edge highlight.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MelluneModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    scrimColor: Color? = null,
    dragHandle: (@Composable () -> Unit)? = { BottomSheetDefaults.DragHandle() },
    contentWindowInsets: @Composable () -> WindowInsets = { WindowInsets(0.dp) },
    content: @Composable ColumnScope.() -> Unit,
) {
    val prefs2 = preferenceManager2()
    val flagshipBlur by prefs2.flagshipBlur.asState()
    val blurIntensity by prefs2.flagshipBlurIntensity.asState()

    val isVisibleTarget = sheetState.targetValue != SheetValue.Hidden
    val blurFraction by animateFloatAsState(
        targetValue = if (isVisibleTarget) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "MelluneSheetBlurFraction",
    )

    val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val isDark = isSystemInDarkTheme()
    val containerColor = if (flagshipBlur) {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = if (isDark) 0.84f else 0.88f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val resolvedScrimColor = scrimColor ?: Color.Black.copy(
        alpha = (if (flagshipBlur) 0.18f else 0.32f) * blurFraction,
    )
    val edgeBorder = if (flagshipBlur) {
        BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.12f else 0.08f),
        )
    } else {
        null
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier.widthIn(max = 640.dp),
        sheetState = sheetState,
        shape = sheetShape,
        containerColor = Color.Transparent,
        scrimColor = resolvedScrimColor,
        dragHandle = null,
        tonalElevation = 0.dp,
        contentWindowInsets = contentWindowInsets,
    ) {
        MelluneSheetWindowBlur(
            enabled = flagshipBlur,
            intensity = blurIntensity,
            fraction = blurFraction,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = sheetShape,
            color = containerColor,
            border = edgeBorder,
            tonalElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                if (dragHandle != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        dragHandle()
                    }
                }
                content()
            }
        }
    }
}

@Composable
private fun MelluneSheetWindowBlur(
    enabled: Boolean,
    intensity: Float,
    fraction: Float,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

    val view = LocalView.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val maxBlurPx = with(density) {
        dimensionResource(R.dimen.max_depth_blur_radius_enhanced).toPx()
    }
    val targetRadiusPx = if (enabled) {
        (maxBlurPx * 0.72f * intensity.coerceIn(0f, 1.5f) * fraction).roundToInt().coerceAtLeast(0)
    } else {
        0
    }

    DisposableEffect(view, context, targetRadiusPx) {
        val window = (view.parent as? DialogWindowProvider)?.window
        val canCrossBlur = CrossWindowBlurListeners.getInstance().isCrossWindowBlurEnabled
        if (window != null) {
            window.setBackgroundDrawable(ColorDrawable(AndroidColor.TRANSPARENT))
            if (targetRadiusPx > 0 && canCrossBlur) {
                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                window.attributes = window.attributes.apply {
                    blurBehindRadius = targetRadiusPx
                }
                window.setBackgroundBlurRadius(targetRadiusPx)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                window.setBackgroundBlurRadius(0)
            }
        }
        onDispose {
            if (window != null) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                window.setBackgroundBlurRadius(0)
            }
        }
    }
}
