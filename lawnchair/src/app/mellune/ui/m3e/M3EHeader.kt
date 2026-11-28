package app.mellune.ui.m3e

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

private val HeaderShape = smoothRoundedShape(
    topStart = 0.dp,
    topEnd = 0.dp,
    bottomEnd = 36.dp,
    bottomStart = 36.dp,
)

/**
 * Material 3 Expressive Header modeled after Spectra's ExpressiveHeader.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun M3EExpressiveHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    backContentDescription: String = "Back",
    maxTitleLines: Int = 2,
    maxSubtitleLines: Int = Int.MAX_VALUE,
    bottomContent: (@Composable ColumnScope.() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val motion = MaterialTheme.motionScheme
    val scheme = MaterialTheme.colorScheme
    val isDarkSurface = scheme.background.luminance() < 0.5f
    val headerColor = lerp(
        scheme.surfaceContainer,
        scheme.primaryContainer,
        if (isDarkSurface) 0.30f else 0.42f,
    )
    val decorColor = scheme.primary
    val decorAccent = scheme.tertiary

    Box(modifier.fillMaxWidth().zIndex(1f)) {
        Surface(
            shape = HeaderShape,
            color = headerColor,
            contentColor = scheme.onSurface,
            tonalElevation = 0.dp,
            shadowElevation = 3.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .expressiveHeaderDecor(decorColor, decorAccent)
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp)
                        .padding(
                            top = 8.dp,
                            bottom = if (bottomContent != null) 10.dp else 16.dp,
                        ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AnimatedVisibility(
                            visible = onBack != null,
                            enter = fadeIn(motion.defaultEffectsSpec()) +
                                scaleIn(
                                    initialScale = 0.92f,
                                    animationSpec = motion.defaultSpatialSpec(),
                                ) +
                                slideInHorizontally(
                                    initialOffsetX = { -it / 2 },
                                    animationSpec = motion.defaultSpatialSpec(),
                                ),
                            exit = fadeOut(motion.fastEffectsSpec()) +
                                scaleOut(targetScale = 0.92f) +
                                slideOutHorizontally(
                                    targetOffsetX = { -it / 2 },
                                    animationSpec = motion.defaultSpatialSpec(),
                                ),
                        ) {
                            Row {
                                ExpressiveIconButton(
                                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = backContentDescription,
                                    onClick = { onBack?.invoke() },
                                    size = 44.dp,
                                    containerColor = scheme.primaryContainer,
                                    contentColor = scheme.onPrimaryContainer,
                                )
                                Spacer(Modifier.width(12.dp))
                            }
                        }

                        AnimatedContent(
                            targetState = title to (subtitle ?: ""),
                            transitionSpec = {
                                (fadeIn(motion.defaultEffectsSpec()) +
                                    slideInVertically(motion.defaultSpatialSpec()) { it / 3 } +
                                    scaleIn(
                                        initialScale = 0.92f,
                                        animationSpec = motion.defaultSpatialSpec(),
                                    ))
                                    .togetherWith(
                                        fadeOut(motion.fastEffectsSpec()) +
                                            slideOutVertically(motion.defaultSpatialSpec()) { -it / 3 } +
                                            scaleOut(targetScale = 0.92f),
                                    )
                            },
                            label = "M3EExpressiveHeaderTransition",
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                        ) { (currentTitle, currentSubtitle) ->
                            HeaderTitleBlock(
                                title = currentTitle,
                                subtitle = currentSubtitle.takeIf { it.isNotBlank() },
                                hasBack = onBack != null,
                                maxTitleLines = maxTitleLines,
                                maxSubtitleLines = maxSubtitleLines,
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            actions()
                        }
                    }

                    if (bottomContent != null) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                        ) {
                            bottomContent()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderTitleBlock(
    title: String,
    subtitle: String?,
    hasBack: Boolean,
    maxTitleLines: Int = 2,
    maxSubtitleLines: Int = Int.MAX_VALUE,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds(),
    ) {
        val baseTitleStyle = when {
            hasBack && title.length > 22 -> MaterialTheme.typography.titleMedium
            title.length > 22 -> MaterialTheme.typography.titleLarge
            else -> M3EText.pageTitle
        }
        Text(
            text = title,
            style = baseTitleStyle.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = maxTitleLines,
            softWrap = maxTitleLines > 1,
            modifier = Modifier.fillMaxWidth(),
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = M3EText.pageSubtitle,
                maxLines = maxSubtitleLines,
                softWrap = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun M3EHeaderActionIcon(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    preset: ExpressiveButtonPreset? = null,
    emphasized: Boolean = false,
) {
    ExpressiveIconButton(
        icon = icon,
        contentDescription = contentDescription,
        onClick = onClick,
        modifier = modifier,
        size = 44.dp,
        containerColor = if (emphasized) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        contentColor = if (emphasized) {
            MaterialTheme.colorScheme.onTertiaryContainer
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        },
        preset = preset,
    )
}

private fun Modifier.expressiveHeaderDecor(color: Color, accent: Color): Modifier =
    this.drawWithCache {
        val preset = M3EShapeState.preset
        val show = preset != ExpressiveButtonPreset.CIRCLE && preset != ExpressiveButtonPreset.PILL
        val path = if (show) {
            buildExpressivePath(
                ExpressiveButtonPreset.SUNNY,
                cx = size.width * 0.98f,
                cy = size.height * 0.02f,
                radius = size.height * 0.62f,
            )
        } else {
            null
        }
        val path2 = if (show) {
            buildExpressivePath(
                ExpressiveButtonPreset.SUNNY,
                cx = size.width * 0.04f,
                cy = size.height * 1.02f,
                radius = size.height * 0.42f,
            )
        } else {
            null
        }
        onDrawBehind {
            if (path != null) drawPath(path, color.copy(alpha = 0.16f))
            if (path2 != null) drawPath(path2, accent.copy(alpha = 0.14f))
        }
    }
