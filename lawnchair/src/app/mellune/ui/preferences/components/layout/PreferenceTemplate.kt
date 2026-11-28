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

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.mellune.ui.m3e.M3EText
import app.mellune.ui.m3e.m3eGroupSurfaceColor
import app.mellune.ui.m3e.m3ePressScale

/***
 * A template used to create most preference-related components in the Preference UI.
 */
@Composable
fun PreferenceTemplate(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    enabled: Boolean = true,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    description: (@Composable () -> Unit)? = null,
    startWidget: (@Composable () -> Unit)? = null,
    endWidget: (@Composable () -> Unit)? = null,
    shapes: ListItemShapes = ListItemDefaults.shapes().copy(
        shape = RoundedCornerShape(6.dp),
        pressedShape = RoundedCornerShape(18.dp),
        focusedShape = RoundedCornerShape(18.dp),
        hoveredShape = RoundedCornerShape(18.dp),
        selectedShape = RoundedCornerShape(18.dp),
    ),
    onClick: (() -> Unit)? = null,
    colors: ListItemColors = ListItemDefaults.segmentedColors(
        containerColor = m3eGroupSurfaceColor(),
        disabledContainerColor = m3eGroupSurfaceColor().copy(alpha = 0.5f),
        supportingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ),
    interactionSource: MutableInteractionSource? = null,
) {
    val resolvedInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val titleContent = @Composable {
        ProvideTitleTextStyle {
            title()
        }
    }

    val descriptionContent = description?.let {
        @Composable {
            ProvideDescriptionTextStyle {
                description()
            }
        }
    }

    val interactiveModifier = if (onClick != null && enabled) {
        modifier.m3ePressScale(resolvedInteraction, targetScale = 0.97f)
    } else {
        modifier
    }

    Column(interactiveModifier) {
        if (onClick == null) {
            SegmentedListItem(
                content = titleContent,
                shapes = shapes,
                modifier = contentModifier.heightIn(min = 66.dp),
                enabled = enabled,
                leadingContent = startWidget,
                trailingContent = endWidget,
                supportingContent = descriptionContent,
                verticalAlignment = verticalAlignment,
                colors = colors,
            )
        } else {
            SegmentedListItem(
                onClick = onClick,
                content = titleContent,
                shapes = shapes,
                modifier = contentModifier.heightIn(min = 66.dp),
                enabled = enabled,
                leadingContent = startWidget,
                trailingContent = endWidget,
                supportingContent = descriptionContent,
                verticalAlignment = verticalAlignment,
                colors = colors,
                interactionSource = resolvedInteraction,
            )
        }
    }
}

@Composable
fun ProvideTitleTextStyle(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalTextStyle provides M3EText.rowTitle,
        content = content,
    )
}

@Composable
fun ProvideDescriptionTextStyle(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalTextStyle provides M3EText.rowSubtitle,
        content = content,
    )
}
