package app.lawnchair.ui.components.expressive

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import kotlin.math.roundToInt

private val TrackComponentHeight = 44.dp
private val ThumbSize = 28.dp
private val HorizontalPadding = 14.dp

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
 * Direct high-performance gesture-driven slider with a pill track and a rolling scalloped
 * cookie thumb (MaterialShapes.Cookie12Sided) whose rotation is derived from travel distance.
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
) {
    val rangeSpan = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)
    val fraction = ((value - valueRange.start) / rangeSpan).coerceIn(0f, 1f)

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackBgColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val activeTrackColor = if (enabled) primaryColor else MaterialTheme.colorScheme.outlineVariant

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(TrackComponentHeight),
        contentAlignment = Alignment.CenterStart,
    ) {
        val totalWidth = maxWidth
        val travel = (totalWidth - HorizontalPadding * 2).coerceAtLeast(0.dp)

        fun updateFromX(xPx: Float, density: Float) {
            val padPx = HorizontalPadding.value * density
            val travelPx = travel.value * density
            if (travelPx <= 0f) return
            val newFraction = ((xPx - padPx) / travelPx).coerceIn(0f, 1f)
            val stepFraction = if (steps > 0) {
                (newFraction * (steps + 1)).roundToInt().toFloat() / (steps + 1)
            } else newFraction
            val newValue = valueRange.start + stepFraction * rangeSpan
            onValueChange(newValue)
        }

        val gestureModifier = if (enabled) {
            Modifier
                .pointerInput(totalWidth, valueRange, steps) {
                    detectTapGestures(
                        onPress = { offset ->
                            updateFromX(offset.x, density)
                            tryAwaitRelease()
                            onValueChangeFinished?.invoke()
                        },
                    )
                }
                .pointerInput(totalWidth, valueRange, steps) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            updateFromX(offset.x, density)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            updateFromX(change.position.x, density)
                        },
                        onDragEnd = { onValueChangeFinished?.invoke() },
                        onDragCancel = { onValueChangeFinished?.invoke() },
                    )
                }
        } else Modifier

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(gestureModifier),
        ) {
            // Track canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .align(Alignment.Center)
                    .padding(horizontal = HorizontalPadding),
            ) {
                val trackHeight = 16.dp.toPx()
                val radius = trackHeight / 2f
                val y = size.height / 2f
                val activeEnd = size.width * fraction

                // Inactive track pill
                drawRoundRect(
                    color = trackBgColor,
                    topLeft = Offset(0f, y - radius),
                    size = Size(size.width, trackHeight),
                    cornerRadius = CornerRadius(radius, radius),
                )

                // Active track pill
                if (fraction > 0.001f) {
                    drawRoundRect(
                        color = activeTrackColor,
                        topLeft = Offset(0f, y - radius),
                        size = Size(activeEnd, trackHeight),
                        cornerRadius = CornerRadius(radius, radius),
                    )
                }

                // Ticks
                if (steps > 0) {
                    val tickColor = if (enabled) Color.White.copy(alpha = 0.5f) else Color.Transparent
                    for (i in 1..steps) {
                        val tickFraction = i.toFloat() / (steps + 1)
                        drawCircle(
                            color = tickColor,
                            radius = 2.dp.toPx(),
                            center = Offset(size.width * tickFraction, y),
                        )
                    }
                }
            }

            // Rolling cookie thumb
            val thumbStart = HorizontalPadding + travel * fraction - (ThumbSize / 2f)
            Box(
                modifier = Modifier
                    .offset(x = thumbStart.coerceAtLeast(0.dp))
                    .size(ThumbSize)
                    .align(Alignment.CenterStart)
                    .rotate(1080f * fraction)
                    .clip(CookieThumbShape)
                    .background(if (enabled) primaryColor else MaterialTheme.colorScheme.outlineVariant),
            )
        }
    }
}
