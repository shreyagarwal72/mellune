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

package app.mellune.ui.preferences.components.controls

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import app.mellune.ui.m3e.M3EBadgeVariant
import app.mellune.ui.m3e.M3EGroupIconBadge
import app.mellune.ui.m3e.m3eGroupSurfaceColor
import app.mellune.ui.preferences.components.layout.PreferenceTemplate
import app.mellune.ui.theme.MelluneTheme
import app.mellune.ui.util.preview.PreviewMellune
import com.android.launcher3.R
import com.android.launcher3.util.MSDLPlayerWrapper
import com.google.android.msdl.data.model.MSDLToken
import kotlin.math.abs

private val CategoryBadgePalette = listOf(
    M3EBadgeVariant.PRIMARY,
    M3EBadgeVariant.SECONDARY,
    M3EBadgeVariant.TERTIARY,
)

@Composable
fun PreferenceCategory(
    label: String,
    @DrawableRes iconResource: Int,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    description: String? = null,
) {
    val context = LocalContext.current
    val mMSDLPlayerWrapper = MSDLPlayerWrapper.INSTANCE.get(context)
    val badgeVariant = if (isSelected) {
        M3EBadgeVariant.PRIMARY
    } else {
        CategoryBadgePalette[abs(label.hashCode()) % CategoryBadgePalette.size]
    }
    val surfaceColor = m3eGroupSurfaceColor()

    PreferenceTemplate(
        title = {
            Text(
                text = label,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        },
        modifier = modifier,
        description = description?.let { { Text(text = it) } },
        colors = ListItemDefaults.segmentedColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
            } else {
                surfaceColor
            },
            disabledContainerColor = surfaceColor.copy(alpha = 0.5f),
            supportingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        startWidget = {
            M3EGroupIconBadge(
                painter = painterResource(id = iconResource),
                shapeKey = label,
                variant = badgeVariant,
            )
        },
        endWidget = {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        },
        onClick = {
            mMSDLPlayerWrapper.playToken(MSDLToken.TAP_LOW_EMPHASIS)
            onNavigate()
        },
    )
}

@PreviewMellune
@Composable
private fun PreferenceCategoryPreview() {
    MelluneTheme {
        PreferenceCategory(
            label = "Example",
            description = "Example description here",
            iconResource = R.drawable.ic_general,
            onNavigate = {},
        )
    }
}
