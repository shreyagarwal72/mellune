package app.mellune.ui.m3e.sliders

import android.graphics.Matrix
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Indication
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.mellune.ui.m3e.M3ESpring

val MaterialStarShape: Shape = object : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val baseWidth = 865.0807f
        val baseHeight = 865.0807f

        val path = Path().apply {
            moveTo(403.3913f, 8.7356f)
            cubicTo(421.0787f, -2.9119f, 444.002f, -2.9119f, 461.6894f, 8.7356f)
            lineTo(518.743f, 46.3066f)
            cubicTo(528.2839f, 52.5895f, 539.5995f, 55.6215f, 551.0036f, 54.9508f)
            lineTo(619.1989f, 50.9402f)
            cubicTo(640.3404f, 49.6968f, 660.1926f, 61.1585f, 669.6865f, 80.0892f)
            lineTo(700.3109f, 141.1534f)
            cubicTo(705.4321f, 151.365f, 713.7157f, 159.6486f, 723.9273f, 164.7699f)
            lineTo(784.9915f, 195.3942f)
            cubicTo(803.9222f, 204.8881f, 815.3839f, 224.7403f, 814.1406f, 245.8818f)
            lineTo(810.1299f, 314.0771f)
            cubicTo(809.4593f, 325.4812f, 812.4913f, 336.7969f, 818.7742f, 346.3378f)
            lineTo(856.3451f, 403.3913f)
            cubicTo(867.9926f, 421.0787f, 867.9927f, 444.002f, 856.3452f, 461.6894f)
            lineTo(818.7742f, 518.743f)
            cubicTo(812.4913f, 528.2839f, 809.4593f, 539.5995f, 810.1299f, 551.0036f)
            lineTo(814.1406f, 619.1989f)
            cubicTo(815.3839f, 640.3404f, 803.9223f, 660.1926f, 784.9916f, 669.6865f)
            lineTo(723.9274f, 700.3109f)
            cubicTo(713.7158f, 705.4321f, 705.4321f, 713.7157f, 700.3109f, 723.9273f)
            lineTo(669.6866f, 784.9915f)
            cubicTo(660.1926f, 803.9222f, 640.3404f, 815.3839f, 619.1989f, 814.1406f)
            lineTo(551.0036f, 810.1299f)
            cubicTo(539.5995f, 809.4593f, 528.2839f, 812.4913f, 518.743f, 818.7742f)
            lineTo(461.6894f, 856.3451f)
            cubicTo(444.0021f, 867.9926f, 421.0787f, 867.9927f, 403.3914f, 856.3452f)
            lineTo(346.3378f, 818.7742f)
            cubicTo(336.7969f, 812.4913f, 325.4812f, 809.4593f, 314.0771f, 810.1299f)
            lineTo(245.8818f, 814.1406f)
            cubicTo(224.7404f, 815.3839f, 204.8882f, 803.9223f, 195.3942f, 784.9916f)
            lineTo(164.7699f, 723.9274f)
            cubicTo(159.6486f, 713.7158f, 151.365f, 705.4321f, 141.1534f, 700.3109f)
            lineTo(80.0892f, 669.6866f)
            cubicTo(61.1585f, 660.1926f, 49.6968f, 640.3404f, 50.9402f, 619.199f)
            lineTo(54.9508f, 551.0036f)
            cubicTo(55.6215f, 539.5995f, 52.5895f, 528.2839f, 46.3066f, 518.743f)
            lineTo(8.7356f, 461.6894f)
            cubicTo(-2.9119f, 444.0021f, -2.9119f, 421.0787f, 8.7356f, 403.3914f)
            lineTo(46.3066f, 346.3378f)
            cubicTo(52.5895f, 336.7969f, 55.6215f, 325.4813f, 54.9508f, 314.0771f)
            lineTo(50.9402f, 245.8818f)
            cubicTo(49.6968f, 224.7404f, 61.1585f, 204.8882f, 80.0892f, 195.3942f)
            lineTo(141.1534f, 164.7699f)
            cubicTo(151.365f, 159.6486f, 159.6486f, 151.365f, 164.7699f, 141.1534f)
            lineTo(195.3942f, 80.0892f)
            cubicTo(204.8882f, 61.1585f, 224.7403f, 49.6968f, 245.8818f, 50.9402f)
            lineTo(314.0771f, 54.9508f)
            cubicTo(325.4813f, 55.6215f, 336.7969f, 52.5895f, 346.3378f, 46.3066f)
            lineTo(403.3913f, 8.7356f)
            close()
        }

        return Outline.Generic(
            path
                .asAndroidPath()
                .apply {
                    transform(
                        Matrix().apply {
                            setScale(size.width / baseWidth, size.height / baseHeight)
                        },
                    )
                }
                .asComposePath(),
        )
    }
}

object SliderSettingsDefaults {
    const val drawSliderShadows: Boolean = false
}

object LocalSettingsState {
    val current: SliderSettingsDefaults = SliderSettingsDefaults
}

object ShapeDefaults {
    val circle: CornerBasedShape = RoundedCornerShape(50)
    val small: CornerBasedShape = RoundedCornerShape(12.dp)
    val default: CornerBasedShape = RoundedCornerShape(20.dp)
}

@Composable
fun AutoCircleShape(): CornerBasedShape = ShapeDefaults.circle

@Composable
fun <T> ProvidableCompositionLocal<T>.ProvidesValue(
    value: T,
    content: @Composable () -> Unit,
) = CompositionLocalProvider(value = this provides value, content = content)

val SafeLocalContainerColor: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceContainerHigh

@Stable
fun Color.compositeOverSafe(background: Color): Color =
    (if (this != Color.Unspecified) this else Color.Transparent)
        .compositeOver(if (background != Color.Unspecified) background else Color.Transparent)

@Stable
fun ColorScheme.outlineVariant(
    luminance: Float = 0.3f,
    onTopOf: Color = surfaceContainer,
): Color = onSecondaryContainer
    .copy(alpha = luminance)
    .compositeOverSafe(onTopOf.takeOrElse { surface })

@Composable
fun rememberRipple(
    bounded: Boolean = true,
    radius: Dp = Dp.Unspecified,
    contentColor: Color = Color.Unspecified,
): Indication {
    val color = contentColor.takeOrElse { LocalContentColor.current }
    return remember(bounded, radius, color) {
        ripple(color = color, bounded = bounded, radius = radius)
    }
}

@Composable
fun animateFloatingRangeAsState(
    range: ClosedFloatingPointRange<Float>,
    animationSpec: AnimationSpec<Float> = M3ESpring.calm(),
): State<ClosedFloatingPointRange<Float>> {
    val start = animateFloatAsState(
        targetValue = range.start,
        animationSpec = animationSpec,
        label = "rangeStart",
    )
    val end = animateFloatAsState(
        targetValue = range.endInclusive,
        animationSpec = animationSpec,
        label = "rangeEnd",
    )
    return remember(start, end) { derivedStateOf { start.value..end.value } }
}

private fun Shape.toSafeShadowShape(): Shape =
    if (this is CornerBasedShape) this else RoundedCornerShape(50)

@Composable
fun Modifier.materialShadow(
    shape: Shape,
    elevation: Dp,
    enabled: Boolean = true,
    isClipped: Boolean = true,
    color: Color = Color.Black,
): Modifier = materialShadow(
    shape = shape,
    elevation = animateDpAsState(
        if (enabled) elevation else 0.dp,
        animationSpec = M3ESpring.calm(),
        label = "materialShadow",
    ),
    isClipped = isClipped,
    color = color,
)

@Composable
@Suppress("UNUSED_PARAMETER")
fun Modifier.materialShadow(
    shape: Shape,
    elevation: State<Dp>,
    isClipped: Boolean = true,
    color: Color = Color.Black,
): Modifier = if (elevation.value > 0.dp) {
    this.shadow(
        elevation = elevation.value,
        shape = shape.toSafeShadowShape(),
        clip = false,
        ambientColor = color,
        spotColor = color,
    )
} else {
    this
}

@Composable
@Suppress("UNUSED_PARAMETER")
fun Modifier.container(
    shape: Shape? = null,
    color: Color = Color.Unspecified,
    resultPadding: Dp = 4.dp,
    borderWidth: Dp = Dp.Unspecified,
    borderColor: Color? = null,
    autoShadowElevation: Dp = 0.dp,
    clip: Boolean = true,
    composeColorOnTopOfBackground: Boolean = true,
    isShadowClip: Boolean = true,
    isStandaloneContainer: Boolean = true,
    shadowColor: Color = Color.Black,
): Modifier {
    val resultShape = shape ?: ShapeDefaults.default
    val scheme = MaterialTheme.colorScheme
    val containerColor = when {
        color == Color.Unspecified -> SafeLocalContainerColor
        composeColorOnTopOfBackground -> color.compositeOverSafe(scheme.background)
        else -> color
    }
    return this
        .then(if (clip) Modifier.clip(resultShape) else Modifier)
        .background(containerColor, resultShape)
}
