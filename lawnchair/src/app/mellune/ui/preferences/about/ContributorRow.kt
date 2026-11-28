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

package app.mellune.ui.preferences.about

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import app.mellune.ui.m3e.expressiveBadgeShapeForKey
import app.mellune.ui.placeholder.PlaceholderHighlight
import app.mellune.ui.placeholder.fade
import app.mellune.ui.placeholder.placeholder
import app.mellune.ui.preferences.components.layout.PreferenceTemplate
import coil.compose.SubcomposeAsyncImage
import com.android.launcher3.R

/**
 * Displays a row with contributor information.
 */
@Composable
fun ContributorRow(
    member: TeamMember,
    modifier: Modifier = Modifier,
) {
    val statusText = when (member.status) {
        ContributorStatus.Active -> stringResource(R.string.contributor_status_active)
        ContributorStatus.Idle -> ""
    }

    val context = LocalContext.current
    val description = "${
        stringResource(member.role.descriptionResId)
    } ${if (member.status == ContributorStatus.Active && statusText.isNotBlank()) "• $statusText" else ""}".trim()

    ContributorRow(
        name = member.name,
        description = description,
        photoUrl = member.photoUrl,
        onClick = {
            val webpage = member.socialUrl.toUri()
            val intent = Intent(Intent.ACTION_VIEW, webpage)
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            }
        },
        modifier = modifier,
    )
}

@Composable
fun ContributorRow(
    name: String,
    description: String,
    photoUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PreferenceTemplate(
        title = { Text(text = name) },
        modifier = modifier,
        onClick = onClick,
        description = { Text(text = description) },
        startWidget = {
            SubcomposeAsyncImage(
                model = photoUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(42.dp)
                    .clip(expressiveBadgeShapeForKey(name))
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .placeholder(
                                visible = true,
                                highlight = PlaceholderHighlight.fade(),
                            ),
                    )
                },
            )
        },
        endWidget = {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}
