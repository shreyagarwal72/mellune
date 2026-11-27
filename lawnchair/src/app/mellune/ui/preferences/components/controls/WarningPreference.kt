package app.mellune.ui.preferences.components.controls

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.mellune.ui.preferences.components.layout.PreferenceTemplate
import app.mellune.ui.preferences.components.layout.ProvideDescriptionTextStyle
import app.mellune.ui.theme.MelluneTheme
import app.mellune.ui.util.preview.PreferenceGroupPreviewContainer
import app.mellune.ui.util.preview.PreviewMellune

@Composable
fun WarningPreference(
    text: String,
    modifier: Modifier = Modifier,
    colors: ListItemColors = ListItemDefaults.segmentedColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ),
    iconTint: Color = MaterialTheme.colorScheme.error,
    textColor: Color = MaterialTheme.colorScheme.error,
) {
    WarningPreference(
        text = text,
        modifier = modifier,
        standalone = false,
        colors = colors,
        iconTint = iconTint,
        textColor = textColor,
    )
}

@Composable
fun WarningPreference(
    text: String,
    modifier: Modifier = Modifier,
    standalone: Boolean = true,
    colors: ListItemColors = ListItemDefaults.segmentedColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ),
    iconTint: Color = MaterialTheme.colorScheme.error,
    textColor: Color = MaterialTheme.colorScheme.error,
) {
    PreferenceTemplate(
        modifier = modifier,
        title = {
            ProvideDescriptionTextStyle {
                Text(
                    text = text,
                    color = textColor,
                )
            }
        },
        startWidget = {
            Icon(
                imageVector = Icons.Rounded.Warning,
                tint = iconTint,
                contentDescription = null,
            )
        },
        shapes = if (standalone) {
            ListItemShapes(
                shape = MaterialTheme.shapes.large,
                selectedShape = MaterialTheme.shapes.large,
                pressedShape = MaterialTheme.shapes.large,
                focusedShape = MaterialTheme.shapes.large,
                hoveredShape = MaterialTheme.shapes.large,
                draggedShape = MaterialTheme.shapes.large,
            )
        } else {
            ListItemDefaults.shapes()
        },
        colors = colors,
    )
}

@PreviewMellune
@Composable
private fun WarningPreferencePreview() {
    MelluneTheme {
        PreferenceGroupPreviewContainer {
            WarningPreference(
                text = "Text",
            )
        }
    }
}

@PreviewMellune
@Composable
private fun WarningPreferenceStandalonePreview() {
    MelluneTheme {
        WarningPreference(
            text = "Text",
            standalone = true,
        )
    }
}
