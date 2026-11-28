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

import android.R as AndroidR
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.mellune.preferences.PreferenceAdapter
import app.mellune.ui.ModalBottomSheetContent
import app.mellune.ui.m3e.M3EOutlinedButton
import app.mellune.ui.m3e.M3ESelectableOptionCard
import app.mellune.ui.preferences.components.layout.PreferenceTemplate
import app.mellune.ui.theme.MelluneTheme
import app.mellune.ui.util.bottomSheetHandler
import app.mellune.ui.util.preview.PreferenceGroupPreviewContainer
import app.mellune.ui.util.preview.PreviewMellune
import com.android.launcher3.util.MSDLPlayerWrapper
import com.google.android.msdl.data.model.MSDLToken

@Composable
fun <T> ListPreference(
    adapter: PreferenceAdapter<T>,
    entries: List<ListPreferenceEntry<T>>,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    description: String? = null,
    endWidget: (@Composable () -> Unit)? = null,
) {
    val value = adapter.state.value
    ListPreference(
        entries = entries,
        value = value,
        onValueChange = adapter::onChange,
        label = label,
        modifier = modifier,
        enabled = enabled,
        description = description,
        endWidget = endWidget,
    )
}

@Composable
fun <T> ListPreference(
    entries: List<ListPreferenceEntry<T>>,
    value: T,
    onValueChange: (T) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    description: String? = null,
    endWidget: (@Composable () -> Unit)? = null,
) {
    val bottomSheetHandler = bottomSheetHandler
    val currentDescription = description ?: entries
        .firstOrNull { it.value == value }
        ?.label?.invoke()
    val mMSDLPlayerWrapper = MSDLPlayerWrapper.INSTANCE.get(LocalContext.current)

    PreferenceTemplate(
        title = { Text(text = label) },
        modifier = modifier,
        enabled = enabled,
        description = currentDescription?.let { { Text(text = it) } },
        endWidget = endWidget,
        onClick = if (enabled) {
            {
                mMSDLPlayerWrapper.playToken(MSDLToken.TAP_MEDIUM_EMPHASIS)
                bottomSheetHandler.show {
                    ModalBottomSheetContent(
                        title = { Text(label) },
                        buttons = {
                            M3EOutlinedButton(onClick = { bottomSheetHandler.hide() }) {
                                Text(text = stringResource(id = AndroidR.string.cancel))
                            }
                        },
                    ) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            itemsIndexed(entries) { _, item ->
                                M3ESelectableOptionCard(
                                    title = item.label(),
                                    selected = item.value == value,
                                    modifier = Modifier.alpha(if (item.enabled) 1f else 0.45f),
                                    onClick = {
                                        mMSDLPlayerWrapper.playToken(MSDLToken.TAP_LOW_EMPHASIS)
                                        if (item.enabled) {
                                            onValueChange(item.value)
                                            bottomSheetHandler.hide()
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        } else {
            null
        },
    )
}

class ListPreferenceEntry<T>(
    val value: T,
    val enabled: Boolean = true,
    val endWidget: (@Composable () -> Unit)? = null,
    val label: @Composable () -> String,
)

@PreviewMellune
@Composable
private fun ListPreferencePreview() {
    val entries = listOf(
        ListPreferenceEntry(value = "option1", label = { "Option 1" }),
        ListPreferenceEntry(value = "option2", label = { "Option 2" }),
        ListPreferenceEntry(value = "option3", label = { "Option 3" }, enabled = false),
    )
    MelluneTheme {
        PreferenceGroupPreviewContainer {
            ListPreference(
                entries = entries,
                value = "option1",
                onValueChange = {},
                label = "List Preference",
                description = "Description",
            )
        }
    }
}
