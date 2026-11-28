package app.mellune.ui.preferences.components.layout

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.mellune.ui.m3e.expressiveIconShape
import app.mellune.ui.m3e.m3ePressScale
import com.android.launcher3.util.MSDLPlayerWrapper
import com.google.android.msdl.data.model.MSDLToken

@Composable
fun ClickableIcon(
    painter: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = LocalContentColor.current,
) {
    val context = LocalContext.current
    val mMSDLPlayerWrapper = remember(context) { MSDLPlayerWrapper.INSTANCE.get(context) }
    val interactionSource = remember { MutableInteractionSource() }
    FilledTonalIconButton(
        onClick = {
            mMSDLPlayerWrapper.playToken(MSDLToken.TAP_MEDIUM_EMPHASIS)
            onClick()
        },
        modifier = modifier
            .size(44.dp)
            .m3ePressScale(interactionSource, targetScale = 0.90f),
        enabled = enabled,
        shape = expressiveIconShape(),
        interactionSource = interactionSource,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        val contentAlpha = if (enabled) tint.alpha else 0.38f
        val alpha by animateFloatAsState(targetValue = contentAlpha, label = "clickableIconAlpha")
        Icon(
            painter = painter,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = alpha),
        )
    }
}

@Composable
fun ClickableIcon(
    imageVector: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = LocalContentColor.current,
) {
    ClickableIcon(
        painter = rememberVectorPainter(image = imageVector),
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        tint = tint,
    )
}
