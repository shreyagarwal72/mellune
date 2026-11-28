/*
 * Copyright 2022, Lawnchair
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

package app.mellune.ui.preferences.about

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.mellune.preferences.PreferenceManager
import app.mellune.ui.components.MelluneLogo
import app.mellune.ui.m3e.ExpressiveButtonShape
import app.mellune.ui.m3e.M3EBadgeVariant
import app.mellune.ui.m3e.M3EConnectedButtonGroup
import app.mellune.ui.m3e.M3EConnectedButtonItem
import app.mellune.ui.m3e.M3EGroupIconBadge
import app.mellune.ui.m3e.M3EHeroCard
import app.mellune.ui.m3e.M3EText
import app.mellune.ui.preferences.LocalIsExpandedScreen
import app.mellune.ui.preferences.components.NavigationActionPreference
import app.mellune.ui.preferences.components.controls.ClickablePreference
import app.mellune.ui.preferences.components.layout.PreferenceGroupHeading
import app.mellune.ui.preferences.components.layout.PreferenceGroupItem
import app.mellune.ui.preferences.components.layout.PreferenceLayoutLazyColumn
import app.mellune.ui.preferences.components.layout.PreferenceTemplate
import app.mellune.ui.preferences.components.layout.preferenceGroupItems
import app.mellune.ui.preferences.navigation.AboutLicenses
import com.android.launcher3.BuildConfig
import com.android.launcher3.R
import kotlinx.coroutines.launch

private val HighlightPalette = listOf(
    M3EBadgeVariant.PRIMARY,
    M3EBadgeVariant.SECONDARY,
    M3EBadgeVariant.TERTIARY,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun About(
    modifier: Modifier = Modifier,
    viewModel: AboutViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val sheetState = rememberModalBottomSheetState(true)
    var openBottomSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val prefs: PreferenceManager = PreferenceManager.getInstance(context)

    if (openBottomSheet) {
        val updateState = uiState.updateState
        if (updateState is UpdateState.Available) {
            ChangesDialog(
                changelogState = updateState.changelogState,
                onDismiss = {
                    scope.launch {
                        sheetState.hide()
                    }.invokeOnCompletion {
                        openBottomSheet = false
                    }
                },
                onDownload = {
                    viewModel.downloadUpdate()
                },
                sheetState = sheetState,
            )
        }
    }

    PreferenceLayoutLazyColumn(
        label = stringResource(id = R.string.about_label),
        modifier = modifier,
        backArrowVisible = !LocalIsExpandedScreen.current,
    ) {
        item {
            Spacer(Modifier.height(10.dp))
        }
        item {
            AboutHeroBanner(
                versionDisplayName = if (prefs.hideVersionInfo.get()) {
                    prefs.pseudonymVersion.get() + " (pseudonym)"
                } else {
                    BuildConfig.VERSION_DISPLAY_NAME
                },
                onOpenCommit = {
                    val commitUrl = "$MELLUNE_REPO_URL/commit/${BuildConfig.COMMIT_HASH}"
                    val intent = Intent(Intent.ACTION_VIEW, commitUrl.toUri())
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    }
                },
            )
        }
        item {
            Spacer(modifier = Modifier.height(12.dp))
        }
        if (uiState.topLinks.isNotEmpty()) {
            item {
                val quickLinks = uiState.topLinks.map { link ->
                    M3EConnectedButtonItem(
                        label = stringResource(id = link.labelResId),
                        painter = painterResource(id = link.iconResId),
                        selected = link.labelResId == R.string.github,
                    )
                }
                M3EConnectedButtonGroup(
                    items = quickLinks,
                    selectedIndex = uiState.topLinks.indexOfFirst { it.labelResId == R.string.github },
                    onSelect = { index ->
                        val picked = uiState.topLinks.getOrNull(index) ?: return@M3EConnectedButtonGroup
                        val intent = Intent(Intent.ACTION_VIEW, picked.url.toUri())
                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
        item {
            UpdateSection(
                updateState = uiState.updateState,
                onInstall = {
                    viewModel.installUpdate(it)
                },
                onForceInstall = {
                    viewModel.installUpdate(it, forceInstall = true)
                },
                onViewChanges = {
                    openBottomSheet = true
                    scope.launch {
                        sheetState.show()
                    }
                },
                onDismissMajorUpdate = {
                    viewModel.resetToDownloaded(it)
                },
            )
        }
        preferenceGroupItems(
            items = uiState.highlights,
            isFirstChild = false,
            heading = { stringResource(id = R.string.about_highlights_heading) },
            key = { _, it -> it.titleResId },
        ) { index, highlight ->
            AboutHighlightRow(
                highlight = highlight,
                variant = HighlightPalette[index % HighlightPalette.size],
            )
        }
        preferenceGroupItems(
            items = uiState.maintainers,
            isFirstChild = false,
            heading = { stringResource(id = R.string.about_maintainer) },
            key = { _, it -> it.name },
        ) { _, it ->
            ContributorRow(
                member = it,
            )
        }
        preferenceGroupItems(
            items = uiState.coreTeam,
            isFirstChild = false,
            heading = { stringResource(id = R.string.about_upstream_credits) },
            key = { _, it -> it.name },
        ) { _, it ->
            ContributorRow(
                member = it,
            )
        }
        preferenceGroupItems(
            items = uiState.supportAndPr,
            isFirstChild = false,
            heading = { stringResource(id = R.string.about_upstream_support) },
            key = { _, it -> it.name },
        ) { _, it ->
            ContributorRow(
                member = it,
            )
        }
        preferenceGroupItems(
            items = uiState.bottomLinks,
            isFirstChild = false,
            heading = { stringResource(id = R.string.community) },
            key = { _, it -> it.labelResId },
        ) { _, it ->
            HorizontalMelluneLink(
                iconResId = it.iconResId,
                label = stringResource(id = it.labelResId),
                url = it.url,
            )
        }
        item {
            PreferenceGroupHeading(
                stringResource(R.string.legal),
            )
        }
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                PreferenceGroupItem(
                    cutTop = false,
                    cutBottom = true,
                ) {
                    NavigationActionPreference(
                        label = stringResource(id = R.string.acknowledgements),
                        destination = AboutLicenses,
                    )
                }
                PreferenceGroupItem(
                    cutTop = true,
                    cutBottom = false,
                ) {
                    ClickablePreference(
                        label = stringResource(id = R.string.about_source_license),
                        onClick = {
                            val webpage = MELLUNE_LICENSE_URL.toUri()
                            val intent = Intent(Intent.ACTION_VIEW, webpage)
                            if (intent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(intent)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutHeroBanner(
    versionDisplayName: String,
    onOpenCommit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    M3EHeroCard(
        modifier = modifier.padding(horizontal = 16.dp),
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MelluneLogo(logoSize = 84.dp)
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = stringResource(id = R.string.derived_app_name),
                style = M3EText.pageTitle,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = ExpressiveButtonShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Text(
                    text = stringResource(id = R.string.about_fork_tagline),
                    style = M3EText.chip,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(id = R.string.about_description),
                style = M3EText.cardBody,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.88f),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                shape = ExpressiveButtonShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .clip(ExpressiveButtonShape)
                    .combinedClickable(
                        onClick = onOpenCommit,
                        onLongClick = onOpenCommit,
                    ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(ExpressiveButtonShape)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    Text(
                        text = versionDisplayName,
                        style = M3EText.badge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Icon(
                        imageVector = Icons.Rounded.OpenInNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutHighlightRow(
    highlight: FeatureHighlight,
    variant: M3EBadgeVariant,
    modifier: Modifier = Modifier,
) {
    PreferenceTemplate(
        title = { Text(text = stringResource(id = highlight.titleResId)) },
        modifier = modifier,
        description = { Text(text = stringResource(id = highlight.descriptionResId)) },
        startWidget = {
            M3EGroupIconBadge(
                painter = painterResource(id = highlight.iconResId),
                shapeKey = highlight.titleResId,
                variant = variant,
            )
        },
    )
}

private const val MELLUNE_REPO_URL = "https://github.com/shreyagarwal72/mellune"
private const val MELLUNE_LICENSE_URL = "$MELLUNE_REPO_URL/blob/main/LICENSE.txt"
