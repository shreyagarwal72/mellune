/*
 * Copyright 2021, Lawnchair
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.mellune.ui.preferences.components.controls

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import app.mellune.preferences.PreferenceAdapter
import app.mellune.ui.m3e.M3ESwitch
import app.mellune.ui.preferences.components.layout.PreferenceTemplate
import app.mellune.ui.theme.MelluneTheme
import app.mellune.ui.util.preview.PreferenceGroupPreviewContainer
import app.mellune.ui.util.preview.PreviewMellune

@Composable
fun SwitchPreference(
    adapter: PreferenceAdapter<Boolean>,
    label: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val checked = adapter.state.value
    SwitchPreference(
        checked = checked,
        onCheckedChange = adapter::onChange,
        label = label,
        modifier = modifier,
        description = description,
        onClick = onClick,
        enabled = enabled,
    )
}

/**
 * A Preference that provides a two-state toggleable option.
 */
@Composable
fun SwitchPreference(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }

    PreferenceTemplate(
        title = { Text(text = label) },
        modifier = modifier,
        enabled = enabled,
        description = description?.let { { Text(text = it) } },
        endWidget = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onClick != null) {
                    VerticalDivider(
                        modifier = Modifier.height(32.dp),
                    )
                }
                M3ESwitch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    enabled = enabled,
                    interactionSource = interactionSource,
                    modifier = Modifier.padding(start = if (onClick != null) 12.dp else 0.dp),
                )
            }
        },
        onClick = {
            if (onClick != null) {
                onClick()
            } else {
                onCheckedChange(!checked)
            }
        },
        interactionSource = interactionSource,
    )
}

@PreviewMellune
@Composable
private fun SwitchPreferencePreview(
    @PreviewParameter(SwitchPreferencePreviewParameterProvider::class) checked: Boolean,
) {
    MelluneTheme {
        PreferenceGroupPreviewContainer {
            SwitchPreference(
                checked = checked,
                onCheckedChange = {},
                label = "Label",
                description = "Description",
                onClick = { },
            )
            SwitchPreference(
                checked = checked,
                onCheckedChange = {},
                label = "Label",
                description = "Description",
                onClick = { },
            )
            SwitchPreference(
                checked = checked,
                onCheckedChange = {},
                label = "Label",
                description = "Description",
                onClick = { },
            )
        }
    }
}

private class SwitchPreferencePreviewParameterProvider : PreviewParameterProvider<Boolean> {
    override val values = sequenceOf(true, false)
}
