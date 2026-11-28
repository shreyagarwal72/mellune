package app.mellune.ui.components.expressive

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import app.mellune.ui.m3e.M3ESpring
import app.mellune.ui.m3e.sliders.FancySlider
import app.mellune.ui.m3e.sliders.fancyThumbShape
import kotlin.math.roundToInt

/**
 * Spectra-style Material 3 Expressive FancySlider with chunky pill track,
 * tick dots, and a 1080-degree rotating star thumb.
 */
@Composable
fun MelluneSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
    steps: Int = 0,
    drawContainer: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    val dragging by interactionSource.collectIsDraggedAsState()
    val pressing by interactionSource.collectIsPressedAsState()
    val eased by animateFloatAsState(
        targetValue = value,
        animationSpec = M3ESpring.quick(),
        label = "melluneSliderValue",
    )
    val shown = if (dragging || pressing) value else eased

    var lastStep by remember { mutableIntStateOf(-1) }
    val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(1e-4f)
    val currentStep = if (steps > 0) {
        (((value - valueRange.start) / span) * (steps + 1)).roundToInt()
    } else {
        -1
    }
    LaunchedEffect(currentStep) {
        if (steps > 0 && lastStep != -1 && lastStep != currentStep && (dragging || pressing)) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        lastStep = currentStep
    }

    val scheme = MaterialTheme.colorScheme
    val sliderColors = SliderDefaults.colors(
        thumbColor = scheme.onPrimaryContainer,
        activeTrackColor = scheme.primaryContainer,
        inactiveTrackColor = scheme.secondaryContainer.copy(alpha = 0.55f),
        activeTickColor = scheme.inverseSurface,
        inactiveTickColor = scheme.surface,
        disabledThumbColor = SwitchDefaults.colors().disabledCheckedThumbColor,
        disabledActiveTrackColor = scheme.primaryContainer.copy(alpha = 0.38f),
        disabledInactiveTrackColor = SwitchDefaults.colors().disabledCheckedTrackColor,
    )

    FancySlider(
        value = shown.coerceIn(valueRange.start, valueRange.endInclusive),
        enabled = enabled,
        colors = sliderColors,
        interactionSource = interactionSource,
        thumbShape = fancyThumbShape(),
        modifier = modifier,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        steps = steps,
        drawContainer = drawContainer,
    )
}
