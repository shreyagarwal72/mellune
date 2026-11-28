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

package app.mellune.ui.preferences.components.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.mellune.ui.m3e.M3EText

@Composable
fun PreferenceGroup(
    modifier: Modifier = Modifier,
    heading: String? = null,
    description: String? = null,
    showDescription: Boolean = true,
    itemSpacing: Dp = 3.dp,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier,
    ) {
        PreferenceGroupHeading(heading)

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(24.dp)),
            verticalArrangement = Arrangement.spacedBy(itemSpacing),
        ) {
            content()
        }
        PreferenceGroupDescription(description = description, showDescription = showDescription)
    }
}

@Composable
fun PreferenceGroupHeading(
    heading: String?,
    modifier: Modifier = Modifier,
) {
    if (heading != null) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = modifier
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp)
                .fillMaxWidth(),
        ) {
            Text(
                text = heading,
                style = M3EText.sectionHeader,
                modifier = Modifier.semantics { this.heading() },
            )
        }
    } else {
        Spacer(modifier = modifier.requiredHeight(8.dp))
    }
}

@Composable
fun PreferenceGroupDescription(
    modifier: Modifier = Modifier,
    description: String? = null,
    showDescription: Boolean = true,
) {
    description?.let {
        ExpandAndShrink(
            modifier = modifier,
            visible = showDescription,
        ) {
            Row(modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 10.dp)) {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
