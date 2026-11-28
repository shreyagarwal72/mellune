package app.mellune.ui.preferences.components.layout

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.mellune.ui.m3e.ExpressiveIconButton
import app.mellune.ui.m3e.M3EPageBackground
import app.mellune.ui.m3e.smoothRoundedShape
import app.mellune.ui.theme.MelluneTheme
import app.mellune.ui.util.preview.PreviewMellune

private val SearchHeaderShape = smoothRoundedShape(
    topStart = 0.dp,
    topEnd = 0.dp,
    bottomEnd = 36.dp,
    bottomStart = 36.dp,
)

@Composable
fun PreferenceSearchScaffold(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        M3EPageBackground(pageKey = "search")
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                val scheme = MaterialTheme.colorScheme
                val isDarkSurface = scheme.background.luminance() < 0.5f
                val headerColor = lerp(
                    scheme.surfaceContainer,
                    scheme.primaryContainer,
                    if (isDarkSurface) 0.30f else 0.42f,
                )
                Surface(
                    shape = SearchHeaderShape,
                    color = headerColor,
                    shadowElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(bottom = 12.dp, top = 8.dp),
                    ) {
                        SearchBar(
                            value = value,
                            onValueChange = onValueChange,
                            backDispatcher = backDispatcher,
                            placeholder = placeholder,
                            actions = actions,
                        )
                    }
                }
            },
            bottomBar = { BottomSpacer() },
        ) {
            content(it)
        }
    }
}

@Composable
private fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    backDispatcher: OnBackPressedDispatcher?,
    modifier: Modifier = Modifier,
    placeholder: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .zIndex(1f)
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
    ) {
        ExpressiveIconButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "Back",
            onClick = { backDispatcher?.onBackPressed() },
            size = 44.dp,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
                    shape = CircleShape,
                )
                .padding(horizontal = 14.dp),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                SearchTextField(
                    value = value,
                    onValueChange = onValueChange,
                ) {
                    if (placeholder != null) {
                        placeholder()
                    }
                }
            }
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                Row(
                    Modifier.fillMaxHeight(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Crossfade(value.isNotEmpty(), label = "Close button animation") { hasText ->
                        if (hasText) {
                            ClickableIcon(
                                imageVector = Icons.Rounded.Clear,
                                onClick = { onValueChange("") },
                            )
                        }
                    }
                    actions()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: @Composable (() -> Unit)? = null,
) {
    val textStyle: TextStyle = LocalTextStyle.current
    val textColor = MaterialTheme.colorScheme.onSurface
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = @Composable { innerTextField ->
                OutlinedTextFieldDefaults.DecorationBox(
                    value = value,
                    innerTextField = innerTextField,
                    enabled = true,
                    singleLine = true,
                    visualTransformation = VisualTransformation.None,
                    interactionSource = remember { MutableInteractionSource() },
                    placeholder = placeholder,
                    colors = OutlinedTextFieldDefaults.colors(),
                    contentPadding = PaddingValues(4.dp),
                    container = {},
                )
            },
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            textStyle = mergedTextStyle,
        )
    }
}

@PreviewMellune
@Composable
private fun SearchTextFieldPreview() {
    MelluneTheme {
        SearchTextField(
            value = "Example",
            onValueChange = {},
            placeholder = { Text("Example placeholder") },
        )
    }
}
