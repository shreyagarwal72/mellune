package app.lawnchair.ui.preferences.components.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lawnchair.preferences.PreferenceAdapter
import app.lawnchair.ui.components.expressive.MelluneSegmentItem
import app.lawnchair.ui.components.expressive.MelluneSegmentedButtonGroup
import app.lawnchair.ui.preferences.components.layout.PreferenceTemplate

@Composable
fun <T> SegmentedButtonPreference(
    label: String,
    adapter: PreferenceAdapter<T>,
    items: List<MelluneSegmentItem<T>>,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
) {
    var selectedValue by adapter

    SegmentedButtonPreference(
        label = label,
        selected = selectedValue,
        onSelect = { selectedValue = it },
        items = items,
        modifier = modifier,
        description = description,
        enabled = enabled,
    )
}

@Composable
fun <T> SegmentedButtonPreference(
    label: String,
    selected: T,
    onSelect: (T) -> Unit,
    items: List<MelluneSegmentItem<T>>,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
) {
    PreferenceTemplate(
        title = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        },
        modifier = modifier,
        description = {
            MelluneSegmentedButtonGroup(
                items = items,
                selected = selected,
                onSelect = onSelect,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            )
        },
        enabled = enabled,
    )
}
