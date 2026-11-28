package app.mellune.organizer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Badge
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.mellune.ui.theme.EdgeToEdge
import app.mellune.ui.theme.MelluneTheme
import com.android.launcher3.R
import java.util.Collections
import kotlinx.coroutines.launch

/**
 * Visual workspace page manager modeled on Pixel Launcher 17's Workspace Organizer.
 * Allows reordering home screen pages, changing the default home page, adding blank pages,
 * removing unneeded pages, or launching [SpaceCreatorActivity].
 */
class WorkspaceOrganizerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MelluneTheme {
                EdgeToEdge()
                WorkspaceOrganizerScreen(
                    onBack = { finish() },
                    onOpenSpaceCreator = {
                        startActivity(Intent(this, SpaceCreatorActivity::class.java))
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkspaceOrganizerScreen(
    onBack: () -> Unit,
    onOpenSpaceCreator: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pages by remember { mutableStateOf<List<WorkspacePageSummary>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    suspend fun reload() {
        isLoading = true
        pages = WorkspaceRepository.loadWorkspacePages(context)
        isLoading = false
    }

    LaunchedEffect(Unit) {
        reload()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.organize_pages_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                WorkspaceRepository.addBlankPage(context)
                                reload()
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.organize_pages_add_blank),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenSpaceCreator,
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                    )
                },
                text = { Text(text = stringResource(R.string.create_space_title)) },
            )
        },
    ) { paddingValues ->
        if (isLoading && pages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            val listState = rememberLazyListState()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(pages, key = { _, page -> page.screenId }) { index, page ->
                        WorkspacePageCard(
                            page = page,
                            canMoveUp = index > 0,
                            canMoveDown = index < pages.lastIndex,
                            canDelete = pages.size > 1,
                            onMoveUp = {
                                scope.launch {
                                    val updated = pages.toMutableList()
                                    Collections.swap(updated, index, index - 1)
                                    val defaultScreenId = pages.firstOrNull { it.isDefaultPage }?.screenId
                                    WorkspaceRepository.reorderPages(
                                        context = context,
                                        orderedScreenIds = updated.map { it.screenId },
                                        defaultScreenId = defaultScreenId,
                                    )
                                    reload()
                                }
                            },
                            onMoveDown = {
                                scope.launch {
                                    val updated = pages.toMutableList()
                                    Collections.swap(updated, index, index + 1)
                                    val defaultScreenId = pages.firstOrNull { it.isDefaultPage }?.screenId
                                    WorkspaceRepository.reorderPages(
                                        context = context,
                                        orderedScreenIds = updated.map { it.screenId },
                                        defaultScreenId = defaultScreenId,
                                    )
                                    reload()
                                }
                            },
                            onSetDefault = {
                                scope.launch {
                                    WorkspaceRepository.setDefaultPage(context, index)
                                    reload()
                                }
                            },
                            onDelete = {
                                scope.launch {
                                    WorkspaceRepository.deletePage(context, page.screenId)
                                    reload()
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkspacePageCard(
    page: WorkspacePageSummary,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    canDelete: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onSetDefault: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (page.isDefaultPage) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.organize_pages_page_n, page.pageIndex + 1),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (page.isDefaultPage) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge {
                            Text(
                                text = stringResource(R.string.organize_pages_default_badge),
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                val countsText = if (page.appCount == 0 && page.folderCount == 0 && page.widgetCount == 0) {
                    stringResource(R.string.organize_pages_empty)
                } else {
                    stringResource(
                        R.string.organize_pages_counts,
                        page.appCount,
                        page.folderCount,
                        page.widgetCount,
                    )
                }
                Text(
                    text = countsText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (page.previewTitles.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = page.previewTitles.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                Icon(imageVector = Icons.Rounded.KeyboardArrowUp, contentDescription = null)
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                Icon(imageVector = Icons.Rounded.KeyboardArrowDown, contentDescription = null)
            }
            IconButton(onClick = onSetDefault, enabled = !page.isDefaultPage) {
                Icon(
                    imageVector = Icons.Rounded.Home,
                    contentDescription = stringResource(R.string.organize_pages_set_default),
                )
            }
            IconButton(onClick = onDelete, enabled = canDelete) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = stringResource(R.string.organize_pages_delete),
                )
            }
        }
    }
}
