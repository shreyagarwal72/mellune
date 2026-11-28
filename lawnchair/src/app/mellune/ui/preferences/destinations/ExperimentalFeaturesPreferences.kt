package app.mellune.ui.preferences.destinations

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.mellune.preferences.getAdapter
import app.mellune.preferences.preferenceManager
import app.mellune.preferences2.preferenceManager2
import app.mellune.ui.preferences.LocalIsExpandedScreen
import app.mellune.ui.preferences.components.WallpaperAccessPermissionDialog
import app.mellune.ui.preferences.components.controls.ListPreference
import app.mellune.ui.preferences.components.controls.ListPreferenceEntry
import app.mellune.ui.preferences.components.controls.SliderPreference
import app.mellune.ui.preferences.components.controls.SwitchPreference
import app.mellune.ui.preferences.components.controls.WarningPreference
import app.mellune.ui.preferences.components.layout.ExpandAndShrink
import app.mellune.ui.preferences.components.layout.PreferenceGroup
import app.mellune.ui.preferences.components.layout.PreferenceLayout
import app.mellune.util.FileAccessManager
import app.mellune.util.FileAccessState
import app.mellune.util.isGestureNavContractCompatible
import com.android.launcher3.R
import com.android.launcher3.Utilities
import com.google.android.msdl.data.model.FeedbackLevel

@Composable
fun ExperimentalFeaturesPreferences(
    modifier: Modifier = Modifier,
) {
    val prefs = preferenceManager()
    val prefs2 = preferenceManager2()

    PreferenceLayout(
        label = stringResource(id = R.string.experimental_features_label),
        backArrowVisible = !LocalIsExpandedScreen.current,
        modifier = modifier,
    ) {
        val enableWallpaperBlur = prefs.enableWallpaperBlur.getAdapter()
        val context = LocalContext.current
        val fileAccessManager = remember { FileAccessManager.getInstance(context) }
        val allFilesAccessState by fileAccessManager.allFilesAccessState.collectAsStateWithLifecycle()
        val wallpaperAccessState by fileAccessManager.wallpaperAccessState.collectAsStateWithLifecycle()
        val hasPermission = wallpaperAccessState != FileAccessState.Denied
        var showPermissionDialog by remember { mutableStateOf(false) }

        PreferenceGroup(
            modifier = Modifier,
            heading = stringResource(R.string.workspace_label),
        ) {
            SwitchPreference(
                adapter = prefs2.enableFontSelection.getAdapter(),
                label = stringResource(id = R.string.font_picker_label),
                description = stringResource(id = R.string.font_picker_description),
            )
            SwitchPreference(
                adapter = prefs.workspaceIncreaseMaxGridSize.getAdapter(),
                label = stringResource(id = R.string.workspace_increase_max_grid_size_label),
                description = stringResource(id = R.string.workspace_increase_max_grid_size_description),
            )
            SwitchPreference(
                adapter = prefs2.showDeckLayout.getAdapter(),
                label = stringResource(R.string.show_deck_layout),
                description = stringResource(R.string.show_deck_layout_description),
            )
            SwitchPreference(
                checked = hasPermission && enableWallpaperBlur.state.value,
                onCheckedChange = {
                    if (!hasPermission) {
                        showPermissionDialog = true
                    } else {
                        enableWallpaperBlur.onChange(it)
                    }
                },
                label = stringResource(id = R.string.wallpaper_blur),
            )

            val canBlur = hasPermission && enableWallpaperBlur.state.value
            ExpandAndShrink(visible = canBlur) {
                SliderPreference(
                    label = stringResource(id = R.string.wallpaper_background_blur),
                    adapter = prefs.wallpaperBlur.getAdapter(),
                    step = 5,
                    valueRange = 0..100,
                    showUnit = "%",
                )
            }
            ExpandAndShrink(visible = canBlur) {
                SliderPreference(
                    label = stringResource(id = R.string.wallpaper_background_blur_factor),
                    adapter = prefs.wallpaperBlurFactorThreshold.getAdapter(),
                    step = 1F,
                    valueRange = 0F..10F,
                )
            }
        }
        if (showPermissionDialog) {
            WallpaperAccessPermissionDialog(
                managedFilesChecked = allFilesAccessState != FileAccessState.Denied,
                onDismiss = {
                    showPermissionDialog = false
                },
                onPermissionRequest = { fileAccessManager.refresh() },
            )
        }
        LifecycleResumeEffect(Unit) {
            showPermissionDialog = false
            fileAccessManager.refresh()
            onPauseOrDispose { }
        }

        val enableGncAdapter = prefs.enableGnc.getAdapter()
        val vibrationFeedbackLevelAdapter = prefs.vibrationFeedbackLevel.getAdapter()

        PreferenceGroup(
            modifier = Modifier,
            heading = stringResource(R.string.internal_label),
            description = stringResource(R.string.internal_description),
        ) {
            SwitchPreference(
                adapter = enableGncAdapter,
                label = stringResource(id = R.string.gesturenavcontract_label),
                description = stringResource(id = R.string.gesturenavcontract_description),
                enabled = Utilities.ATLEAST_Q,
            )
            ExpandAndShrink(visible = enableGncAdapter.state.value && !isGestureNavContractCompatible) {
                WarningPreference(stringResource(R.string.gesturenavcontract_warning_incompatibility))
            }
            ListPreference(
                adapter = vibrationFeedbackLevelAdapter,
                entries = listOf(
                    ListPreferenceEntry(FeedbackLevel.NO_FEEDBACK.ordinal) {
                        stringResource(R.string.vibration_feedback_no_feedback_choice)
                    },
                    ListPreferenceEntry(FeedbackLevel.MINIMAL.ordinal) {
                        stringResource(R.string.vibration_feedback_minimal_choice)
                    },
                    ListPreferenceEntry(FeedbackLevel.DEFAULT.ordinal) {
                        stringResource(R.string.vibration_feedback_default_choice)
                    },
                    ListPreferenceEntry(FeedbackLevel.EXPRESSIVE.ordinal) {
                        stringResource(R.string.vibration_feedback_expressive_choice)
                    },
                ),
                label = stringResource(R.string.vibration_feedback_level_label),
            )
        }
    }
}
