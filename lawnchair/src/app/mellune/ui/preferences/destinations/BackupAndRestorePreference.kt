package app.mellune.ui.preferences.destinations

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.mellune.backup.SystemLauncherImporter
import app.mellune.backup.ui.restoreBackupOpener
import app.mellune.backup.ui.restoreNovaBackupOpener
import app.mellune.ui.ModalBottomSheetContent
import app.mellune.ui.components.MelluneModalBottomSheet
import app.mellune.ui.m3e.M3ESegmentedActionPair
import app.mellune.ui.preferences.LocalIsExpandedScreen
import app.mellune.ui.preferences.components.NavigationActionPreference
import app.mellune.ui.preferences.components.controls.ClickablePreference
import app.mellune.ui.preferences.components.layout.PreferenceGroup
import app.mellune.ui.preferences.components.layout.PreferenceLayout
import app.mellune.ui.preferences.navigation.CreateBackup
import app.mellune.util.restartLauncher
import com.android.launcher3.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupAndRestorePreference(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val importer = remember(context) { SystemLauncherImporter(context) }

    var showSystemImportSheet by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf<SystemLauncherImporter.ScanResult?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isScanning = true
                scanResult = importer.scanBackupFile(uri)
                isScanning = false
            }
        }
    }

    PreferenceLayout(
        label = stringResource(R.string.backup_and_restore_label),
        backArrowVisible = !LocalIsExpandedScreen.current,
        modifier = modifier,
    ) {
        PreferenceGroup {
            NavigationActionPreference(
                label = stringResource(R.string.create_backup),
                subtitle = stringResource(R.string.create_backup_description),
                destination = CreateBackup,
            )
            ClickablePreference(
                label = stringResource(R.string.restore_backup),
                subtitle = stringResource(R.string.restore_backup_description),
                onClick = restoreBackupOpener(),
            )
        }
        PreferenceGroup {
            ClickablePreference(
                label = stringResource(R.string.restore_nova_backup),
                subtitle = stringResource(R.string.restore_nova_backup_description),
                onClick = restoreNovaBackupOpener(),
            )
            ClickablePreference(
                label = stringResource(R.string.import_system_launcher_label),
                subtitle = stringResource(R.string.import_system_launcher_desc),
                onClick = {
                    showSystemImportSheet = true
                    scope.launch {
                        isScanning = true
                        scanResult = importer.scanLiveSystemLauncher()
                        isScanning = false
                    }
                },
            )
        }
    }

    if (showSystemImportSheet) {
        MelluneModalBottomSheet(onDismissRequest = { showSystemImportSheet = false }) {
            val result = scanResult
            val canRestore = result != null && result.items.isNotEmpty() && !isScanning
            ModalBottomSheetContent(
                title = { Text(text = stringResource(R.string.import_system_launcher_label)) },
                content = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (isScanning) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text(
                                text = stringResource(R.string.import_system_launcher_scanning),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        } else if (result != null) {
                            Text(
                                text = result.sourceName,
                                style = MaterialTheme.typography.titleSmall,
                            )
                            if (result.items.isNotEmpty()) {
                                Text(
                                    text = stringResource(
                                        R.string.import_system_launcher_summary,
                                        result.appCount,
                                        result.folderCount,
                                        result.widgetCount,
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else if (result.errorMessage != null) {
                                Text(
                                    text = result.errorMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                },
                buttons = {
                    M3ESegmentedActionPair(
                        dismissLabel = stringResource(R.string.import_system_launcher_pick_file),
                        onDismiss = {
                            filePicker.launch(arrayOf("*/*"))
                        },
                        confirmLabel = stringResource(R.string.import_system_launcher_restore),
                        onConfirm = {
                            val items = result?.items.orEmpty()
                            if (items.isNotEmpty()) {
                                scope.launch {
                                    isScanning = true
                                    runCatching {
                                        importer.restoreItems(items)
                                    }.onSuccess {
                                        showSystemImportSheet = false
                                        restartLauncher(context)
                                    }.onFailure { err ->
                                        isScanning = false
                                        Toast.makeText(
                                            context,
                                            err.message ?: "Import failed",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                }
                            }
                        },
                        confirmEnabled = canRestore,
                        modifier = Modifier.weight(1f),
                    )
                },
            )
        }
    }
}
