package app.mellune.ui.m3e

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

private fun TextStyle.withTallerLineHeight(extraSp: Float = 2f): TextStyle {
    val next = when {
        lineHeight != TextUnit.Unspecified && lineHeight.isSp -> (lineHeight.value + extraSp).sp
        fontSize != TextUnit.Unspecified && fontSize.isSp -> (fontSize.value + 6f + extraSp).sp
        else -> 22.sp
    }
    return copy(lineHeight = next)
}

/**
 * Shared semantic typography role layer matching Spectra's M3E hierarchy.
 */
object M3EText {
    val rowTitle: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

    val rowSubtitle: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.bodyMedium
            .withTallerLineHeight(2f)
            .copy(color = MaterialTheme.colorScheme.onSurfaceVariant)

    val sectionHeader: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

    val pageTitle: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
        )

    val pageSubtitle: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.bodySmall
            .withTallerLineHeight(1f)
            .copy(color = MaterialTheme.colorScheme.onSurfaceVariant)

    val cardTitle: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

    val cardBody: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.bodyMedium
            .withTallerLineHeight(2f)
            .copy(color = MaterialTheme.colorScheme.onSurfaceVariant)

    val monoText: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.bodySmall.copy(
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

    val chip: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.SemiBold,
            color = Color.Unspecified,
        )

    val button: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.SemiBold,
            color = Color.Unspecified,
        )

    val badge: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.3.sp,
            color = Color.Unspecified,
        )

    val tabLabel: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.SemiBold,
            color = Color.Unspecified,
        )

    val value: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Bold,
            fontFeatureSettings = "tnum",
            color = MaterialTheme.colorScheme.primary,
        )
}
