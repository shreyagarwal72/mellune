package app.mellune.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Mellune logo that follows the device's Material You colors.
 * Needs a MaterialTheme that uses dynamicLightColorScheme / dynamicDarkColorScheme
 * (Android 12+). On older devices it uses your fallback color scheme.
 */
@Composable
fun MelluneLogo(
    modifier: Modifier = Modifier,
    logoSize: Dp = 96.dp,
) {
    val cs = MaterialTheme.colorScheme
    val tile = cs.surfaceContainer
    val circleColor = cs.primary
    val squircleColor = cs.primaryContainer
    val cloverColor = cs.tertiaryContainer
    val cookieColor = cs.tertiary

    Canvas(modifier = modifier.size(logoSize)) {
        val u = this.size.minDimension / 108f

        drawRoundRect(
            color = tile,
            cornerRadius = CornerRadius(this.size.minDimension * 0.3f),
        )

        withTransform({ scale(1.3f, 1.3f, pivot = center) }) {
            fun p(x: Float, y: Float) = Offset(x * u, y * u)

            // Top-left: circle
            drawCircle(circleColor, radius = 12f * u, center = p(40f, 40f))

            // Top-right: squircle
            drawRoundRect(
                color = squircleColor,
                topLeft = p(56f, 28f),
                size = Size(24f * u, 24f * u),
                cornerRadius = CornerRadius(8f * u),
            )

            // Bottom-left: clover
            for (dx in listOf(-5f, 5f)) {
                for (dy in listOf(-5f, 5f)) {
                    drawCircle(cloverColor, radius = 7.5f * u, center = p(40f + dx, 68f + dy))
                }
            }
            drawCircle(cloverColor, radius = 6f * u, center = p(40f, 68f))

            // Bottom-right: 9-lobe cookie
            drawCircle(cookieColor, radius = 9.25f * u, center = p(68f, 68f))
            for (i in 0 until 9) {
                val angle = Math.toRadians((-90.0 + 40.0 * i))
                val lx = 68f + 8.5f * cos(angle).toFloat()
                val ly = 68f + 8.5f * sin(angle).toFloat()
                drawCircle(cookieColor, radius = 4f * u, center = p(lx, ly))
            }
        }
    }
}
