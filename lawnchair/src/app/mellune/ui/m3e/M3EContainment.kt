package app.mellune.ui.m3e

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ChipColors
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.launcher3.util.MSDLPlayerWrapper
import com.google.android.msdl.data.model.MSDLToken
import kotlinx.coroutines.delay

enum class M3EBadgeVariant {
    PRIMARY,
    SECONDARY,
    TERTIARY,
    SURFACE_TONAL,
    OUTLINE,
    ERROR;

    @Composable
    fun colors(): Pair<Color, Color> = when (this) {
        PRIMARY ->
            MaterialTheme.colorScheme.primaryContainer to
                MaterialTheme.colorScheme.onPrimaryContainer
        SECONDARY ->
            MaterialTheme.colorScheme.secondaryContainer to
                MaterialTheme.colorScheme.onSecondaryContainer
        TERTIARY ->
            MaterialTheme.colorScheme.tertiaryContainer to
                MaterialTheme.colorScheme.onTertiaryContainer
        SURFACE_TONAL, OUTLINE ->
            MaterialTheme.colorScheme.surfaceContainerHighest to
                MaterialTheme.colorScheme.onSurface
        ERROR ->
            MaterialTheme.colorScheme.errorContainer to
                MaterialTheme.colorScheme.onErrorContainer
    }
}

@Composable
fun m3eGroupSurfaceColor(): Color {
    val bloom = LocalBloomEnabled.current
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    return if (bloom) base.copy(alpha = 0.88f) else base
}

@Composable
fun M3ESectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, bottom = 8.dp),
        style = M3EText.sectionHeader,
    )
}

@Composable
fun rememberM3EGroupPressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.96f,
): Float {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = M3ESpring.quick(),
        label = "m3eGroupPressScale",
    )
    return scale
}

@Composable
fun M3EGroupIconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.primaryContainer,
    tint: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    size: Dp = 42.dp,
    iconSize: Dp = 21.dp,
    shape: Shape = expressiveBadgeShapeForKey(icon.name),
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
fun M3EGroupIconBadge(
    painter: Painter,
    shapeKey: Any,
    modifier: Modifier = Modifier,
    variant: M3EBadgeVariant = M3EBadgeVariant.PRIMARY,
    size: Dp = 42.dp,
    iconSize: Dp = 21.dp,
) {
    val (container, tint) = variant.colors()
    Box(
        modifier = modifier
            .size(size)
            .clip(expressiveBadgeShapeForKey(shapeKey))
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painter, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
fun M3EGroupIconBadge(
    painter: Painter,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.primaryContainer,
    tint: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    size: Dp = 42.dp,
    iconSize: Dp = 21.dp,
    shape: Shape = expressiveBadgeShapeForKey("painter"),
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painter, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/**
 * Material 3 Expressive Switch modeled after Spectra's PetalSwitch.
 */
@Composable
fun M3ESwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbIcon: ImageVector? = Icons.Rounded.Check,
    enabled: Boolean = true,
    colors: SwitchColors? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val msdl = remember(context) { MSDLPlayerWrapper.INSTANCE.get(context) }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = M3ESpring.quick(),
        label = "m3eSwitchScale",
    )

    val switchColors = colors ?: SwitchDefaults.colors(
        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
        checkedTrackColor = MaterialTheme.colorScheme.primary,
        checkedIconColor = MaterialTheme.colorScheme.primary,
        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        uncheckedBorderColor = MaterialTheme.colorScheme.outline,
        uncheckedIconColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    )

    val thumbContent: (@Composable () -> Unit)? = thumbIcon?.let { icon ->
        {
            AnimatedContent(checked to icon, label = "m3eSwitchThumbIcon") { (isOn, ic) ->
                Icon(
                    imageVector = if (isOn) ic else Icons.Rounded.Close,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                )
            }
        }
    }

    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange?.let { cb ->
                { next ->
                    msdl.playToken(if (next) MSDLToken.SWITCH_ON else MSDLToken.SWITCH_OFF)
                    cb(next)
                    focus.clearFocus()
                }
            },
            modifier = modifier.scale(scale),
            thumbContent = thumbContent,
            enabled = enabled,
            colors = switchColors,
            interactionSource = interactionSource,
        )
    }
}

@Composable
fun M3EGroupRow(
    icon: ImageVector,
    title: String,
    position: M3EGroupPosition,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    iconContainer: Color = MaterialTheme.colorScheme.primaryContainer,
    iconTint: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    danger: Boolean = false,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    val source = remember { MutableInteractionSource() }
    val scale = rememberM3EGroupPressScale(source)
    val shape = m3eGroupShape(position)
    val activeSurfaceColor = m3eGroupSurfaceColor()
    Card(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = activeSurfaceColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        interactionSource = source,
        modifier = modifier
            .fillMaxWidth()
            .scale(scale),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            M3EGroupIconBadge(icon = icon, container = iconContainer, tint = iconTint)
            Spacer(Modifier.width(16.dp))
            Column(
                Modifier
                    .weight(1f)
                    .padding(end = 6.dp),
            ) {
                Text(
                    title,
                    style = M3EText.rowTitle,
                    color = if (danger) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                subtitle?.let { Text(it, style = M3EText.rowSubtitle) }
            }
            if (trailing != null) {
                trailing()
            } else {
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = if (danger) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
fun M3EGroupListRow(
    position: M3EGroupPosition,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    minHeight: Dp = 68.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    trailing: (@Composable () -> Unit)? = null,
) {
    val source = remember { MutableInteractionSource() }
    val scale = rememberM3EGroupPressScale(source)
    val haptics = LocalHapticFeedback.current
    val shape = m3eGroupShape(position)
    val rowContainerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    } else {
        m3eGroupSurfaceColor()
    }
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = rowContainerColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(shape)
            .combinedClickable(
                interactionSource = source,
                indication = ripple(),
                onClick = onClick,
                onLongClick = onLongClick?.let {
                    {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        it()
                    }
                },
            ),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading()
            Spacer(Modifier.width(16.dp))
            Column(
                Modifier
                    .weight(1f)
                    .padding(end = 6.dp),
                content = content,
            )
            trailing?.invoke()
        }
    }
}

@Composable
fun M3EGroupContainerCard(
    modifier: Modifier = Modifier,
    position: M3EGroupPosition = M3EGroupPosition.SINGLE,
    containerColor: Color = m3eGroupSurfaceColor(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = m3eGroupShape(position),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        content = content,
    )
}

@Composable
fun M3EHeroCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(30.dp),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.contentColorFor(containerColor).takeOrElse {
                MaterialTheme.colorScheme.onSurface
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        content = content,
    )
}

@Composable
fun M3EInfoCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val containerColor = if (isError) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.78f)
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.60f)
    }
    val badgeBg = if (isError) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }
    val badgeFg = if (isError) {
        MaterialTheme.colorScheme.onError
    } else {
        MaterialTheme.colorScheme.onPrimary
    }
    val contentColor = if (isError) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    M3EHeroCard(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        containerColor = containerColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            M3EGroupIconBadge(
                icon = icon,
                container = badgeBg,
                tint = badgeFg,
                size = 42.dp,
                iconSize = 20.dp,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = M3EText.cardTitle, color = contentColor)
                Text(subtitle, style = M3EText.cardBody, color = contentColor.copy(alpha = 0.85f))
                if (actionLabel != null && onAction != null) {
                    TextButton(
                        onClick = onAction,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Text(actionLabel, style = M3EText.button, color = badgeBg)
                    }
                }
            }
        }
    }
}

@Composable
fun M3ESelectableOptionCard(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
) {
    val cardShape = RoundedCornerShape(24.dp)
    val source = remember { MutableInteractionSource() }
    val scale = rememberM3EGroupPressScale(source)
    val container by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            m3eGroupSurfaceColor()
        },
        animationSpec = M3ESpring.quick(),
        label = "m3eOptionContainer",
    )
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .scale(scale),
        shape = cardShape,
        color = container,
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        interactionSource = source,
        border = null,
    ) {
        Row(
            Modifier
                .heightIn(min = 66.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading?.invoke()
            if (leading != null) Spacer(Modifier.width(16.dp))
            Column(
                Modifier
                    .weight(1f)
                    .padding(end = 6.dp),
            ) {
                Text(title, style = M3EText.rowTitle)
                subtitle?.let { Text(it, style = M3EText.rowSubtitle) }
            }
            if (selected) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(ExpressiveButtonShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

data class M3EConnectedButtonItem(
    val label: String,
    val icon: ImageVector? = null,
    val painter: Painter? = null,
    val selected: Boolean = false,
)

private fun m3eConnectedShape(index: Int, count: Int): Shape = when {
    count <= 1 -> RoundedCornerShape(50.dp)
    index == 0 -> RoundedCornerShape(
        topStart = 50.dp,
        bottomStart = 50.dp,
        topEnd = 6.dp,
        bottomEnd = 6.dp,
    )
    index == count - 1 -> RoundedCornerShape(
        topStart = 6.dp,
        bottomStart = 6.dp,
        topEnd = 50.dp,
        bottomEnd = 50.dp,
    )
    else -> RoundedCornerShape(6.dp)
}

@Composable
fun M3EConnectedButtonGroup(
    items: List<M3EConnectedButtonItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
) {
    if (items.isEmpty()) return
    val resolvedMinHeight = height.coerceAtLeast(48.dp)
    val context = LocalContext.current
    val msdl = remember(context) { MSDLPlayerWrapper.INSTANCE.get(context) }
    var pressedIndex by remember { mutableStateOf<Int?>(null) }
    var clickedIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(clickedIndex) {
        if (clickedIndex != null) {
            delay(240L)
            clickedIndex = null
        }
    }
    val currentActiveIndex = pressedIndex ?: clickedIndex
    val hasSelection = selectedIndex in items.indices || items.any { it.selected }

    val rowContent: @Composable RowScope.() -> Unit = {
        val count = items.size
        items.forEachIndexed { index, item ->
            val selected = item.selected || (selectedIndex >= 0 && index == selectedIndex)
            val baseShape = m3eConnectedShape(index, count)
            val pillCorner by animateDpAsState(
                targetValue = if (selected && count > 1) 50.dp else 6.dp,
                animationSpec = M3ESpring.quick(),
                label = "M3ESegmentPillCorner",
            )
            val shape: Shape =
                if (selected && count > 1) RoundedCornerShape(pillCorner) else baseShape
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()

            LaunchedEffect(isPressed) {
                if (isPressed) {
                    pressedIndex = index
                } else if (pressedIndex == index) {
                    pressedIndex = null
                }
            }

            val animatedWeight by animateFloatAsState(
                targetValue = when {
                    currentActiveIndex != null ->
                        if (currentActiveIndex == index) 1.15f else 0.88f
                    hasSelection -> if (selected) 1.12f else 0.90f
                    else -> 1f
                },
                animationSpec = M3ESpring.bouncy(),
                label = "M3EConnectedWeight",
            )

            val containerColor by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    m3eGroupSurfaceColor()
                },
                animationSpec = M3ESpring.quick(),
                label = "M3EConnectedBg",
            )
            val contentColor = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }

            Surface(
                onClick = {
                    clickedIndex = index
                    msdl.playToken(MSDLToken.TAP_LOW_EMPHASIS)
                    onSelect(index)
                },
                modifier = if (count <= 4) {
                    Modifier
                        .weight(animatedWeight)
                        .defaultMinSize(minHeight = resolvedMinHeight)
                        .semantics { role = Role.RadioButton }
                } else {
                    Modifier
                        .defaultMinSize(minHeight = resolvedMinHeight)
                        .semantics { role = Role.RadioButton }
                },
                shape = shape,
                color = containerColor,
                contentColor = contentColor,
                interactionSource = interactionSource,
            ) {
                val compact = count >= 3
                Row(
                    modifier = Modifier
                        .defaultMinSize(minHeight = resolvedMinHeight)
                        .padding(horizontal = if (compact) 10.dp else 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (item.icon != null) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(if (compact) 16.dp else 18.dp),
                        )
                        Spacer(Modifier.width(if (compact) 4.dp else 6.dp))
                    } else if (item.painter != null) {
                        Icon(
                            painter = item.painter,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(if (compact) 16.dp else 18.dp),
                        )
                        Spacer(Modifier.width(if (compact) 4.dp else 6.dp))
                    } else if (selected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(if (compact) 16.dp else 18.dp),
                        )
                        Spacer(Modifier.width(if (compact) 4.dp else 6.dp))
                    }
                    Text(
                        text = item.label,
                        style = (if (compact) M3EText.tabLabel else M3EText.chip).copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                        ),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        color = contentColor,
                    )
                }
            }
        }
    }

    if (items.size > 4) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = rowContent,
        )
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = rowContent,
        )
    }
}

@Composable
fun M3EButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        modifier = modifier.m3ePressScale(source, targetScale = 0.95f),
        enabled = enabled,
        shape = ExpressiveButtonShape,
        colors = colors,
        contentPadding = contentPadding,
        interactionSource = source,
        content = content,
    )
}

@Composable
fun M3ETonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 11.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.m3ePressScale(source, targetScale = 0.95f),
        enabled = enabled,
        shape = ExpressiveButtonShape,
        contentPadding = contentPadding,
        interactionSource = source,
        content = content,
    )
}

@Composable
fun M3EOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 11.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.m3ePressScale(source, targetScale = 0.95f),
        enabled = enabled,
        shape = ExpressiveButtonShape,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        contentPadding = contentPadding,
        interactionSource = source,
        content = content,
    )
}

@Composable
fun M3ESegmentedActionPair(
    dismissLabel: String,
    onDismiss: () -> Unit,
    confirmLabel: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmEnabled: Boolean = true,
) {
    val context = LocalContext.current
    val msdl = remember(context) { MSDLPlayerWrapper.INSTANCE.get(context) }
    val dismissSource = remember { MutableInteractionSource() }
    val confirmSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalButton(
            onClick = {
                msdl.playToken(MSDLToken.TAP_LOW_EMPHASIS)
                onDismiss()
            },
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 46.dp)
                .m3ePressScale(dismissSource, targetScale = 0.96f),
            shape = m3eConnectedShape(index = 0, count = 2),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 11.dp),
            interactionSource = dismissSource,
        ) {
            Text(text = dismissLabel, maxLines = 1)
        }
        Button(
            onClick = {
                msdl.playToken(MSDLToken.TAP_MEDIUM_EMPHASIS)
                onConfirm()
            },
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 46.dp)
                .m3ePressScale(confirmSource, targetScale = 0.96f),
            enabled = confirmEnabled,
            shape = m3eConnectedShape(index = 1, count = 2),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 11.dp),
            interactionSource = confirmSource,
        ) {
            Text(text = confirmLabel, maxLines = 1)
        }
    }
}

@Composable
fun M3EFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(14.dp),
    colors: SelectableChipColors? = null,
    border: BorderStroke? = null,
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        FilterChip(
            selected = selected,
            onClick = onClick,
            label = label,
            modifier = modifier,
            enabled = enabled,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            shape = shape,
            colors = colors ?: FilterChipDefaults.filterChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                labelColor = MaterialTheme.colorScheme.onSurface,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
            ),
            border = border,
        )
    }
}

@Composable
fun M3EAssistChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(14.dp),
    colors: ChipColors? = null,
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        AssistChip(
            onClick = onClick,
            label = label,
            modifier = modifier,
            enabled = enabled,
            leadingIcon = leadingIcon,
            shape = shape,
            colors = colors ?: AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                labelColor = MaterialTheme.colorScheme.onSurface,
            ),
            border = null,
        )
    }
}

@Composable
fun M3EDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
    )
}
