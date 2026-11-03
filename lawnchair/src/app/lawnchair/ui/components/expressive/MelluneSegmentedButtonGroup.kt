package app.lawnchair.ui.components.expressive

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class MelluneSegmentItem<T>(
    val value: T,
    val label: String,
    val icon: ImageVector? = null,
    val enabled: Boolean = true,
)

/**
 * Petal v4.5 Expressive Segmented Button Group with dynamic shape-morphing active states,
 * spring physics, and animated corner radii transitions.
 */
@Composable
fun <T> MelluneSegmentedButtonGroup(
    items: List<MelluneSegmentItem<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    scrollableIfCrowded: Boolean = true,
) {
    if (items.isEmpty()) return

    val outerRadius = height / 2
    val buttonGroupShape = RoundedCornerShape(outerRadius)
    val shouldScroll = scrollableIfCrowded && (items.size > 4 || items.any { it.label.length > 12 })

    Surface(
        modifier = modifier.height(height),
        shape = buttonGroupShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
    ) {
        val rowModifier = if (shouldScroll) {
            Modifier
                .fillMaxHeight()
                .padding(4.dp)
                .horizontalScroll(rememberScrollState())
        } else {
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(4.dp)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = rowModifier,
        ) {
            items.forEach { item ->
                val isSelected = item.value == selected

                val animatedCornerRadius by animateDpAsState(
                    targetValue = if (isSelected) outerRadius - 4.dp else 8.dp,
                    animationSpec = spring(
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                    ),
                    label = "MelluneSegmentCornerAnimation",
                )

                val containerColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        Color.Transparent
                    },
                    animationSpec = spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow),
                    label = "MelluneSegmentColorAnimation",
                )

                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow),
                    label = "MelluneSegmentContentColorAnimation",
                )

                val shape = RoundedCornerShape(animatedCornerRadius)

                val itemModifier = if (shouldScroll) {
                    Modifier
                        .fillMaxHeight()
                        .clip(shape)
                        .background(containerColor)
                        .clickable(enabled = item.enabled) { onSelect(item.value) }
                        .padding(horizontal = 16.dp)
                        .semantics { role = Role.RadioButton }
                } else {
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(shape)
                        .background(containerColor)
                        .clickable(enabled = item.enabled) { onSelect(item.value) }
                        .padding(horizontal = 8.dp)
                        .semantics { role = Role.RadioButton }
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = itemModifier,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        if (item.icon != null) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = contentColor,
                        )
                    }
                }
            }
        }
    }
}
