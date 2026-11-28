package app.mellune.ui.preferences.components.controls

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.mellune.preferences.PreferenceAdapter
import app.mellune.ui.m3e.M3ESpring
import app.mellune.ui.m3e.M3ESwitch
import app.mellune.ui.m3e.M3EText
import app.mellune.ui.m3e.m3eGroupSurfaceColor
import app.mellune.ui.m3e.m3ePressScale
import app.mellune.ui.preferences.components.layout.ExpandAndShrink
import app.mellune.ui.theme.MelluneTheme
import app.mellune.ui.util.preview.PreferenceGroupPreviewContainer
import app.mellune.ui.util.preview.PreviewMellune
import com.android.launcher3.util.MSDLPlayerWrapper
import com.google.android.msdl.data.model.MSDLToken

/**
 * A toggle to enable a list of preferences.
 */
@Composable
fun MainSwitchPreference(
    adapter: PreferenceAdapter<Boolean>,
    label: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    MainSwitchPreference(
        checked = adapter.state.value,
        onCheckedChange = adapter::onChange,
        label = label,
        modifier = modifier,
        description = description,
        enabled = enabled,
        content = content,
    )
}

@Composable
fun MainSwitchPreference(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    MainSwitchPreference(
        checked = checked,
        onCheckedChange = onCheckedChange,
        label = label,
        modifier = modifier,
        enabled = enabled,
    )

    ExpandAndShrink(description != null) {
        if (description != null) {
            Row(
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 12.dp),
            ) {
                Text(
                    text = description,
                    style = M3EText.rowSubtitle,
                )
            }
        }
    }
    Crossfade(targetState = checked, label = "mainSwitchContent") { targetState ->
        if (targetState) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun MainSwitchPreference(
    checked: Boolean,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val mMSDLPlayerWrapper = MSDLPlayerWrapper.INSTANCE.get(LocalContext.current)
    val containerColor by animateColorAsState(
        targetValue = if (checked) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            m3eGroupSurfaceColor()
        },
        animationSpec = M3ESpring.quick(),
        label = "mainSwitchContainer",
    )
    val contentColor = if (checked) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        onClick = {
            if (enabled) {
                val next = !checked
                mMSDLPlayerWrapper.playToken(
                    if (next) MSDLToken.SWITCH_ON else MSDLToken.SWITCH_OFF,
                )
                onCheckedChange(next)
            }
        },
        enabled = enabled,
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        contentColor = contentColor,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .m3ePressScale(interactionSource, targetScale = 0.97f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = M3EText.rowTitle,
                color = contentColor,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
            )
            M3ESwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                interactionSource = interactionSource,
            )
        }
    }
}

@PreviewMellune
@Composable
private fun MainSwitchPreferenceCheckedPreview() {
    MelluneTheme {
        PreferenceGroupPreviewContainer {
            MainSwitchPreference(
                checked = true,
                onCheckedChange = {},
                label = "Main Switch Preference",
                description = "Description of the main switch preference",
            ) {}
        }
    }
}
