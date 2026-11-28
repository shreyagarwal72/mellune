package app.mellune.organizer

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.mellune.ui.components.expressive.ExpressiveScrollBar
import app.mellune.ui.components.expressive.extractFastScrollGlyph
import app.mellune.ui.theme.EdgeToEdge
import app.mellune.ui.theme.MelluneTheme
import com.android.launcher3.R
import com.android.launcher3.model.data.AppInfo
import kotlinx.coroutines.launch

/**
 * Guided Space Creator modeled on Pixel Launcher 17's SpaceCreator flow: picks apps by
 * Flowerpot category or usage rank and builds a dedicated home screen page or folder.
 */
class SpaceCreatorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MelluneTheme {
                EdgeToEdge()
                SpaceCreatorScreen(onFinished = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpaceCreatorScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mostUsedLabel = stringResource(R.string.create_space_most_used)
    val defaultSpaceName = stringResource(R.string.create_space_default_name)

    var data by remember { mutableStateOf<CategorizedSpaceData?>(null) }
    var selectedTopic by remember { mutableStateOf(mostUsedLabel) }
    var spaceName by remember { mutableStateOf(mostUsedLabel) }
    var groupIntoFolder by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    val selectedComponentKeys = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        val loaded = WorkspaceRepository.loadCategorizedApps(context)
        data = loaded
        selectedComponentKeys.clear()
        loaded.mostUsedApps.forEach { app ->
            selectedComponentKeys.add(app.toComponentKey().toString())
        }
    }

    val loaded = data
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.create_space_title)) },
                navigationIcon = {
                    IconButton(onClick = onFinished) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (loaded != null && selectedComponentKeys.isNotEmpty() && !isSaving) {
                ExtendedFloatingActionButton(
                    onClick = {
                        isSaving = true
                        val chosenApps = loaded.allApps.filter {
                            selectedComponentKeys.contains(it.toComponentKey().toString())
                        }
                        val resolvedName = spaceName.trim().ifBlank { defaultSpaceName }
                        scope.launch {
                            WorkspaceRepository.createTopicSpace(
                                context = context,
                                spaceName = resolvedName,
                                apps = chosenApps,
                                groupIntoFolder = groupIntoFolder,
                            )
                            Toast.makeText(
                                context,
                                context.getString(R.string.create_space_done, resolvedName),
                                Toast.LENGTH_SHORT,
                            ).show()
                            onFinished()
                        }
                    },
                    icon = { Icon(imageVector = Icons.Rounded.Check, contentDescription = null) },
                    text = {
                        Text(text = stringResource(R.string.create_space_confirm, selectedComponentKeys.size))
                    },
                )
            }
        },
    ) { paddingValues ->
        if (loaded == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            val topics = remember(loaded, mostUsedLabel) {
                listOf(mostUsedLabel) + loaded.categories.keys.toList()
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    OutlinedTextField(
                        value = spaceName,
                        onValueChange = { spaceName = it },
                        label = { Text(text = stringResource(R.string.create_space_name_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(topics) { topic ->
                            FilterChip(
                                selected = selectedTopic == topic,
                                onClick = {
                                    selectedTopic = topic
                                    spaceName = topic
                                    val targetApps = if (topic == mostUsedLabel) {
                                        loaded.mostUsedApps
                                    } else {
                                        loaded.categories[topic].orEmpty()
                                    }
                                    selectedComponentKeys.clear()
                                    targetApps.forEach { app ->
                                        selectedComponentKeys.add(app.toComponentKey().toString())
                                    }
                                },
                                label = { Text(text = topic) },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.create_space_group_folder),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = groupIntoFolder,
                            onCheckedChange = { groupIntoFolder = it },
                        )
                    }
                }

                val listState = rememberLazyListState()
                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(loaded.allApps, key = { it.toComponentKey().toString() }) { app ->
                            val key = app.toComponentKey().toString()
                            val checked = selectedComponentKeys.contains(key)
                            SpaceAppRow(
                                app = app,
                                checked = checked,
                                onToggle = {
                                    if (checked) {
                                        selectedComponentKeys.remove(key)
                                    } else {
                                        selectedComponentKeys.add(key)
                                    }
                                },
                            )
                        }
                    }
                    ExpressiveScrollBar(
                        listState = listState,
                        dragLabelProvider = { index ->
                            extractFastScrollGlyph(loaded.allApps.getOrNull(index)?.title?.toString())
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(top = 8.dp, bottom = 96.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SpaceAppRow(
    app: AppInfo,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(16.dp),
        color = if (checked) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = checked, onCheckedChange = { onToggle() })
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.title?.toString().orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = app.targetPackage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
