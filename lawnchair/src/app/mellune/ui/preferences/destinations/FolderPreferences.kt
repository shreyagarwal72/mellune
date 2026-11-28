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

package app.mellune.ui.preferences.destinations

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.mellune.preferences.getAdapter
import app.mellune.preferences.preferenceManager
import app.mellune.preferences2.preferenceManager2
import app.mellune.ui.preferences.LocalIsExpandedScreen
import app.mellune.ui.preferences.components.NavigationActionPreference
import app.mellune.ui.preferences.components.colorpreference.ColorPreference
import app.mellune.ui.preferences.components.controls.ListPreference
import app.mellune.ui.preferences.components.controls.ListPreferenceEntry
import app.mellune.ui.preferences.components.controls.SliderPreference
import app.mellune.ui.preferences.components.controls.SwitchPreference
import app.mellune.ui.preferences.components.layout.ExpandAndShrink
import app.mellune.ui.preferences.components.layout.PreferenceGroup
import app.mellune.ui.preferences.components.layout.PreferenceLayout
import app.mellune.ui.preferences.navigation.GeneralIconShape
import com.android.launcher3.R
import com.android.launcher3.Utilities

@Composable
fun FolderPreferences(
    modifier: Modifier = Modifier,
) {
    PreferenceLayout(
        label = stringResource(id = R.string.folders_label),
        backArrowVisible = !LocalIsExpandedScreen.current,
        modifier = modifier,
    ) {
        val context = LocalContext.current
        val prefs = preferenceManager()
        val prefs2 = preferenceManager2()
        val folderIconShapeAdapter = prefs2.folderShape.getAdapter()
        val folderIconShapeSubtitle = iconShapeEntries(context)
            .firstOrNull { it.value == folderIconShapeAdapter.state.value }
            ?.label?.invoke()
            ?: stringResource(id = R.string.custom)
        PreferenceGroup(heading = stringResource(id = R.string.general_label)) {
            NavigationActionPreference(
                label = stringResource(id = R.string.folder_shape_label),
                destination = GeneralIconShape(ShapeRoute.FOLDER_SHAPE),
                subtitle = folderIconShapeSubtitle,
                endWidget = {
                    IconShapePreview(iconShape = folderIconShapeAdapter.state.value)
                },
            )
            ColorPreference(preference = prefs2.folderColor)
            SliderPreference(
                label = stringResource(id = R.string.folder_preview_bg_opacity_label),
                adapter = prefs2.folderPreviewBackgroundOpacity.getAdapter(),
                step = 0.1F,
                valueRange = 0F..1F,
                showAsPercentage = true,
            )
            SliderPreference(
                label = stringResource(id = R.string.folder_bg_opacity_label),
                adapter = prefs2.folderBackgroundOpacity.getAdapter(),
                step = 0.1F,
                valueRange = 0F..1F,
                showAsPercentage = true,
            )
            if (Utilities.ATLEAST_S) {
                val flagshipBlurEnabled = prefs2.flagshipBlur.getAdapter().state.value
                val folderBlurAdapter = prefs2.folderBlur.getAdapter()
                ExpandAndShrink(visible = flagshipBlurEnabled) {
                    Column {
                        SwitchPreference(
                            adapter = folderBlurAdapter,
                            label = stringResource(id = R.string.folder_blur_label),
                            description = stringResource(id = R.string.folder_blur_desc),
                        )
                        ExpandAndShrink(visible = folderBlurAdapter.state.value) {
                            SliderPreference(
                                label = stringResource(id = R.string.folder_blur_intensity_label),
                                adapter = prefs2.folderBlurIntensity.getAdapter(),
                                step = 0.05F,
                                valueRange = 0F..1F,
                                showAsPercentage = true,
                            )
                        }
                        SwitchPreference(
                            adapter = prefs2.folderGlassEffect.getAdapter(),
                            label = stringResource(id = R.string.folder_glass_effect_label),
                            description = stringResource(id = R.string.folder_glass_effect_desc),
                        )
                    }
                }
            }
            val layoutEntries = listOf(
                ListPreferenceEntry(
                    value = "grid",
                    label = { stringResource(id = R.string.folder_preview_layout_grid) },
                ),
                ListPreferenceEntry(
                    value = "stack",
                    label = { stringResource(id = R.string.folder_preview_layout_stack) },
                ),
                ListPreferenceEntry(
                    value = "fan",
                    label = { stringResource(id = R.string.folder_preview_layout_fan) },
                ),
                ListPreferenceEntry(
                    value = "line",
                    label = { stringResource(id = R.string.folder_preview_layout_line) },
                ),
            )
            ListPreference(
                adapter = prefs2.folderPreviewLayout.getAdapter(),
                entries = layoutEntries,
                label = stringResource(id = R.string.folder_preview_layout_label),
            )
            val shapeEntries = listOf(
                ListPreferenceEntry(
                    value = "default",
                    label = { stringResource(id = R.string.folder_preview_shape_default) },
                ),
                ListPreferenceEntry(
                    value = "circle",
                    label = { stringResource(id = R.string.folder_preview_shape_circle) },
                ),
                ListPreferenceEntry(
                    value = "square",
                    label = { stringResource(id = R.string.folder_preview_shape_square) },
                ),
                ListPreferenceEntry(
                    value = "platform",
                    label = { stringResource(id = R.string.folder_preview_shape_platform) },
                ),
            )
            ListPreference(
                adapter = prefs2.folderPreviewShape.getAdapter(),
                entries = shapeEntries,
                label = stringResource(id = R.string.folder_preview_shape_label),
            )
            val swipeEntries = listOf(
                ListPreferenceEntry(
                    value = "none",
                    label = { stringResource(id = R.string.folder_swipe_action_none) },
                ),
                ListPreferenceEntry(
                    value = "open_folder",
                    label = { stringResource(id = R.string.folder_swipe_action_open_folder) },
                ),
                ListPreferenceEntry(
                    value = "launch_first",
                    label = { stringResource(id = R.string.folder_swipe_action_launch_first) },
                ),
            )
            ListPreference(
                adapter = prefs2.folderSwipeAction.getAdapter(),
                entries = swipeEntries,
                label = stringResource(id = R.string.folder_swipe_action_label),
            )
        }
        PreferenceGroup(heading = stringResource(id = R.string.grid)) {
            SliderPreference(
                label = stringResource(id = R.string.max_folder_columns),
                adapter = prefs2.folderColumns.getAdapter(),
                step = 1,
                valueRange = 2..5,
            )
            SliderPreference(
                label = stringResource(id = R.string.max_folder_rows),
                adapter = prefs.folderRows.getAdapter(),
                step = 1,
                valueRange = 2..5,
            )
        }

        val homeScreenLabelsAdapter = prefs2.showIconLabelsOnHomeScreenFolder.getAdapter()
        PreferenceGroup(heading = stringResource(id = R.string.icons)) {
            SwitchPreference(
                adapter = homeScreenLabelsAdapter,
                label = stringResource(id = R.string.show_labels),
            )
            ExpandAndShrink(visible = homeScreenLabelsAdapter.state.value) {
                SliderPreference(
                    label = stringResource(id = R.string.label_size),
                    adapter = prefs2.homeIconLabelFolderSizeFactor.getAdapter(),
                    step = 0.1f,
                    valueRange = 0.5F..1.5F,
                    showAsPercentage = true,
                )
            }
        }
    }
}
