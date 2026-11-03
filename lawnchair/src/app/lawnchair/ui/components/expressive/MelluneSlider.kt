package app.lawnchair.ui.components.expressive

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath

private val TrackHeight = 44.dp
private val ThumbSize = 30.dp
private val ThumbInset = 7.dp

/**
 * Shape representing a normalized [RoundedPolygon] scaled to outline dimensions.
 */
private class PolygonShape(polygon: RoundedPolygon) : Shape {
    private val basePath = polygon.toPath().asComposePath()

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val matrix = Matrix()
        matrix.scale(size.width, size.height)
        val path = Path()
        path.addPath(basePath)
        path.transform(matrix)
        return Outline.Generic(path)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val CookieThumbShape: Shape = PolygonShape(MaterialShapes.Cookie12Sided)

/**
 * Petal v4.5 rolling-thumb chunky expressive slider:
 * A pill track with a filled progress section and a scalloped cookie thumb (MaterialShapes.Cookie12Sided)
 * that physically rolls as it travels along the track.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MelluneSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
    steps: Int = 0,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val sliderState = remember(valueRange, steps) {
        SliderState(
            value = value,
            steps = steps,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
        )
    }

    LaunchedEffect(value) {
        if (sliderState.value != value) {
            sliderState.value = value
        }
    }

    sliderState.onValueChange = onValueChange

    val rangeDiff = sliderState.valueRange.endInclusive - sliderState.valueRange.start
    val target = if (rangeDiff <= 0f) 0f else {
        ((sliderState.value - sliderState.valueRange.start) / rangeDiff).coerceIn(0f, 1f)
    }

    val animatedFraction by animateFloatAsState(
        targetValue = target,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = 1200f
        ),
        label = "melluneSliderFraction"
    )
    val fraction = if (isDragged || isPressed) target else animatedFraction

    Slider(
        state = sliderState,
        modifier = modifier.height(TrackHeight),
        enabled = enabled,
        interactionSource = interactionSource,
        thumb = {},
        track = {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TrackHeight)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                val travel = maxWidth - ThumbSize - ThumbInset * 2
                val thumbStart = ThumbInset + travel * fraction

                // Rolling: rotation proportional to physical travel distance
                val circumference = ThumbSize.value * Math.PI.toFloat()
                val rollDegrees = if (circumference > 0f) {
                    (travel.value * fraction / circumference) * 360f
                } else 0f

                val fillWidth = if (fraction <= 0.001f) 0.dp else thumbStart + ThumbSize + ThumbInset
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(fillWidth)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (enabled) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = thumbStart)
                        .size(ThumbSize)
                        .graphicsLayer { rotationZ = rollDegrees }
                        .clip(CookieThumbShape)
                        .background(
                            if (enabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainer
                        )
                )
            }
        }
    )
}
