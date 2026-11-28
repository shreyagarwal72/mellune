package app.mellune.ui.components.expressive

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.mellune.ui.m3e.M3EConnectedButtonGroup
import app.mellune.ui.m3e.M3EConnectedButtonItem

data class MelluneSegmentItem<T>(
    val value: T,
    val label: String,
    val icon: ImageVector? = null,
    val enabled: Boolean = true,
)

/**
 * Connected Material 3 Expressive segmented button group modeled after Spectra's ExpressiveButtonGroup.
 */
@Composable
fun <T> MelluneSegmentedButtonGroup(
    items: List<MelluneSegmentItem<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    @Suppress("UNUSED_PARAMETER") scrollableIfCrowded: Boolean = true,
) {
    if (items.isEmpty()) return
    val selectedIndex = items.indexOfFirst { it.value == selected }
    val buttonItems = items.map { item ->
        M3EConnectedButtonItem(
            label = item.label,
            icon = item.icon,
            selected = item.value == selected,
        )
    }

    M3EConnectedButtonGroup(
        items = buttonItems,
        selectedIndex = selectedIndex,
        onSelect = { index ->
            val picked = items.getOrNull(index)
            if (picked != null && picked.enabled) {
                onSelect(picked.value)
            }
        },
        modifier = modifier,
        height = height,
    )
}
