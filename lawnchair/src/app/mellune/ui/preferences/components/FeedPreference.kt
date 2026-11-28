package app.mellune.ui.preferences.components

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.lawnchair.icons.CustomAdaptiveIconDrawable
import app.mellune.FeedBridge
import app.mellune.nexuslauncher.GoogleOverlayProxyService
import app.mellune.preferences.getAdapter
import app.mellune.preferences.preferenceManager
import app.mellune.ui.ModalBottomSheetContent
import app.mellune.ui.components.MelluneModalBottomSheet
import app.mellune.ui.m3e.M3ESegmentedActionPair
import app.mellune.ui.preferences.components.controls.ListPreference
import app.mellune.ui.preferences.components.controls.ListPreferenceEntry
import app.mellune.ui.preferences.components.layout.PreferenceDivider
import app.mellune.ui.preferences.components.layout.PreferenceTemplate
import com.android.launcher3.R
import com.google.accompanist.drawablepainter.rememberDrawablePainter

data class ProviderInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable?,
)

fun getProviders(context: Context): List<ProviderInfo> {
    val builtinLabel = context.getString(R.string.google_companion_builtin_name)
    return FeedBridge.getAvailableProviders(context).map {
        val label = if (it.packageName == context.packageName) {
            builtinLabel
        } else {
            it.loadLabel(context.packageManager).toString()
        }
        ProviderInfo(
            name = label,
            packageName = it.packageName,
            icon = CustomAdaptiveIconDrawable.wrapNonNull(it.loadIcon(context.packageManager)),
        )
    }.toList()
}

fun getEntries(context: Context) = getProviders(context).map {
    ListPreferenceEntry(
        value = it.packageName,
        endWidget = {
            if (it.icon != null) {
                Image(
                    painter = rememberDrawablePainter(drawable = it.icon),
                    contentDescription = null,
                    modifier = Modifier.requiredSize(48.dp),
                )
            }
        },
        label = { it.name },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedPreference(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val adapter = preferenceManager().feedProvider.getAdapter()
    val preferredPackage = adapter.state.value
    val entries = remember { getEntries(context) }
    val resolvedPackage = remember(preferredPackage) {
        FeedBridge.getInstance(context).resolveBridge(preferredPackage)?.packageName
            ?: context.packageName
    }
    val resolvedEntry = entries.firstOrNull { it.value == resolvedPackage }
    val googleInstalled = remember {
        GoogleOverlayProxyService.isGoogleAppInstalled(context.packageManager)
    }
    var showCompanionSheet by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        ListPreference(
            value = resolvedEntry?.value ?: "",
            onValueChange = adapter::onChange,
            entries = entries,
            label = stringResource(R.string.feed_provider),
            endWidget = resolvedEntry?.endWidget,
        )
        PreferenceDivider()
        val companionDesc = when {
            !googleInstalled -> stringResource(R.string.google_companion_status_missing_google)
            resolvedPackage == context.packageName ->
                stringResource(R.string.google_companion_status_ready)
            else -> stringResource(
                R.string.google_companion_status_external,
                resolvedEntry?.label?.invoke() ?: resolvedPackage,
            )
        }
        PreferenceTemplate(
            title = { Text(text = stringResource(R.string.google_companion_status_title)) },
            description = { Text(text = companionDesc) },
            onClick = { showCompanionSheet = true },
        )
    }

    if (showCompanionSheet) {
        MelluneModalBottomSheet(onDismissRequest = { showCompanionSheet = false }) {
            ModalBottomSheetContent(
                title = { Text(text = stringResource(R.string.google_companion_status_title)) },
                text = { Text(text = stringResource(R.string.google_companion_sheet_subtitle)) },
                content = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "${stringResource(R.string.feed_provider)}: " +
                                (resolvedEntry?.label?.invoke() ?: resolvedPackage),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "${stringResource(R.string.google_companion_google_app_label)}: " +
                                stringResource(
                                    if (googleInstalled) {
                                        R.string.google_companion_installed
                                    } else {
                                        R.string.google_companion_not_installed
                                    },
                                ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                buttons = {
                    M3ESegmentedActionPair(
                        dismissLabel = stringResource(android.R.string.cancel),
                        onDismiss = { showCompanionSheet = false },
                        confirmLabel = stringResource(R.string.google_companion_reconnect),
                        onConfirm = {
                            adapter.onChange(context.packageName)
                            showCompanionSheet = false
                        },
                        modifier = Modifier.weight(1f),
                    )
                },
            )
        }
    }
}
