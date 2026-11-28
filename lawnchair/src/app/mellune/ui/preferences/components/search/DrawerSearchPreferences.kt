package app.mellune.ui.preferences.components.search

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.mellune.preferences.PreferenceManager
import app.mellune.preferences.getAdapter
import app.mellune.preferences.not
import app.mellune.preferences.preferenceManager
import app.mellune.preferences2.PreferenceManager2
import app.mellune.preferences2.preferenceManager2
import app.mellune.search.algorithms.MelluneSearchAlgorithm
import app.mellune.search.algorithms.engine.provider.web.CustomWebSearchProvider
import app.mellune.ui.preferences.LocalNavController
import app.mellune.ui.preferences.components.HiddenAppsInSearchPreference
import app.mellune.ui.preferences.components.controls.ListPreference
import app.mellune.ui.preferences.components.controls.ListPreferenceEntry
import app.mellune.ui.preferences.components.controls.MainSwitchPreference
import app.mellune.ui.preferences.components.controls.SwitchPreference
import app.mellune.ui.preferences.components.controls.TwoTargetSwitchPreference
import app.mellune.ui.preferences.components.layout.PreferenceGroup
import app.mellune.ui.preferences.navigation.SearchProviderPreference
import app.mellune.util.FileAccessManager
import com.android.launcher3.R
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@Composable
fun DrawerSearchPreference(
    modifier: Modifier = Modifier,
) {
    val prefs = preferenceManager()
    val prefs2 = preferenceManager2()
    val context = LocalContext.current

    val showDrawerSearchBar = !prefs2.hideAppDrawerSearchBar.getAdapter()
    val hiddenApps = prefs2.hiddenApps.getAdapter().state.value

    MainSwitchPreference(
        adapter = showDrawerSearchBar,
        label = stringResource(id = R.string.show_app_search_bar),
        modifier = modifier,
    ) {
        PreferenceGroup(heading = stringResource(R.string.general_label)) {
            if (hiddenApps.isNotEmpty()) {
                HiddenAppsInSearchPreference()
            }
            SwitchPreference(
                adapter = prefs2.autoShowKeyboardInDrawer.getAdapter(),
                label = stringResource(id = R.string.pref_search_auto_show_keyboard),
            )
            SearchProvider(
                context = context,
            )
            SwitchPreference(
                label = stringResource(R.string.allapps_match_qsb_style_label),
                description = stringResource(R.string.allapps_match_qsb_style_description),
                adapter = prefs2.matchHotseatQsbStyle.getAdapter(),
            )
        }

        val searchAlgorithm = preferenceManager2().searchAlgorithm.getAdapter().state.value
        val navController = LocalNavController.current
        PreferenceGroup(heading = stringResource(id = R.string.show_search_result_types)) {
            if (searchAlgorithm != MelluneSearchAlgorithm.ASI_SEARCH) {
                val canDisable = searchAlgorithm != MelluneSearchAlgorithm.APP_SEARCH
                val adapter = prefs.searchResultApps.getAdapter()

                TwoTargetSwitchPreference(
                    checked = if (canDisable) adapter.state.value else true,
                    onCheckedChange = if (canDisable) adapter::onChange else ({}),
                    enabled = canDisable,
                    label = stringResource(R.string.search_pref_result_apps_and_shortcuts_title),
                    onClick = {
                        navController.navigate(SearchProviderPreference(SearchProviderId.APPS))
                    },
                )
            }
            when (searchAlgorithm) {
                MelluneSearchAlgorithm.LOCAL_SEARCH -> {
                    LocalSearchSettings(
                        prefs = prefs,
                        prefs2 = prefs2,
                        context = context,
                    )
                }

                MelluneSearchAlgorithm.ASI_SEARCH -> {
                    ASISearchSettings(prefs)
                }
            }
        }
    }
}

@Composable
private fun ASISearchSettings(prefs: PreferenceManager) {
    SwitchPreference(
        adapter = prefs.searchResultShortcuts.getAdapter(),
        label = stringResource(id = R.string.search_pref_result_shortcuts_title),
    )
    SwitchPreference(
        adapter = prefs.searchResultPeople.getAdapter(),
        label = stringResource(id = R.string.search_pref_result_people_title),
    )
    SwitchPreference(
        adapter = prefs.searchResultSettings.getAdapter(),
        label = stringResource(id = R.string.search_pref_result_settings_title),
    )
}

@Composable
private fun SearchProvider(
    context: Context,
) {
    val searchAlgorithmEntries = remember {
        sequenceOf(
            ListPreferenceEntry(MelluneSearchAlgorithm.APP_SEARCH) { stringResource(R.string.search_algorithm_app_search) },
            ListPreferenceEntry(MelluneSearchAlgorithm.LOCAL_SEARCH) { stringResource(R.string.search_algorithm_global_search_on_device) },
            ListPreferenceEntry(MelluneSearchAlgorithm.ASI_SEARCH) { stringResource(R.string.search_algorithm_global_search_via_asi) },
        ).filter {
            when (it.value) {
                MelluneSearchAlgorithm.ASI_SEARCH -> MelluneSearchAlgorithm.isASISearchEnabled(
                    context,
                )

                else -> true
            }
        }.toList()
    }

    ListPreference(
        adapter = preferenceManager2().searchAlgorithm.getAdapter(),
        entries = searchAlgorithmEntries,
        label = stringResource(R.string.app_search_algorithm),
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun LocalSearchSettings(
    prefs: PreferenceManager,
    prefs2: PreferenceManager2,
    context: Context,
) {
    val navController = LocalNavController.current
    val currentWebProvider = prefs2.webSuggestionProvider.getAdapter().state.value
    val webSuggestionProvider = stringResource(currentWebProvider.label)

    TwoTargetSwitchPreference(
        adapter = prefs.searchResultStartPageSuggestion.getAdapter(),
        label = stringResource(id = R.string.search_pref_result_web_title),
        description = if (currentWebProvider == CustomWebSearchProvider) {
            webSuggestionProvider
        } else {
            stringResource(
                id = R.string.search_pref_result_web_provider_description,
                webSuggestionProvider,
            )
        },
        onClick = {
            navController.navigate(SearchProviderPreference(SearchProviderId.WEB))
        },
    )
    val peopleAdapter = prefs.searchResultPeople.getAdapter()
    val peopleEnabled = rememberPermissionState(android.Manifest.permission.READ_CONTACTS).status.isGranted
    TwoTargetSwitchPreference(
        checked = peopleEnabled && peopleAdapter.state.value,
        onCheckedChange = peopleAdapter::onChange,
        switchEnabled = peopleEnabled,
        label = stringResource(id = R.string.search_pref_result_people_title),
        description = stringResource(id = R.string.search_pref_result_contacts_description),
        onClick = {
            navController.navigate(SearchProviderPreference(SearchProviderId.CONTACTS))
        },
    )
    val filesAdapter = prefs.searchResultFilesToggle.getAdapter()
    val filesEnabled = remember { FileAccessManager.getInstance(context) }.hasAnyPermission.collectAsStateWithLifecycle().value
    TwoTargetSwitchPreference(
        checked = filesEnabled && filesAdapter.state.value,
        onCheckedChange = filesAdapter::onChange,
        switchEnabled = filesEnabled,
        label = stringResource(R.string.search_pref_result_files_title),
        description = stringResource(R.string.search_pref_result_files_description),
        onClick = {
            navController.navigate(SearchProviderPreference(SearchProviderId.FILES))
        },
    )
    TwoTargetSwitchPreference(
        adapter = prefs.searchResultSettingsEntry.getAdapter(),
        label = stringResource(id = R.string.search_pref_result_settings_title),
        onClick = {
            navController.navigate(SearchProviderPreference(SearchProviderId.SETTINGS))
        },
    )
    TwoTargetSwitchPreference(
        adapter = prefs.searchResulRecentSuggestion.getAdapter(),
        label = stringResource(id = R.string.search_pref_result_history_title),
        onClick = {
            navController.navigate(SearchProviderPreference(SearchProviderId.HISTORY))
        },
    )
    SwitchPreference(
        adapter = prefs.searchResultCalculator.getAdapter(),
        label = stringResource(R.string.all_apps_search_result_calculator),
    )
    SwitchPreference(
        adapter = prefs2.searchResultQuickToggles.getAdapter(),
        label = stringResource(R.string.search_pref_quick_toggles_title),
        description = stringResource(R.string.search_pref_quick_toggles_desc),
    )
}
