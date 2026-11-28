package app.mellune.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.mellune.ui.m3e.M3EText
import app.mellune.ui.preferences.components.layout.BottomSpacer

@Composable
fun ModalBottomSheetContent(
    buttons: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    title: (@Composable () -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    content: @Composable (() -> Unit)? = null,
) {
    val contentPadding = Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .safeDrawingPadding(),
    ) {
        title?.let {
            Box(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp)) {
                ProvideTextStyle(M3EText.pageTitle, title)
            }
        }
        text?.let {
            Box(modifier = contentPadding) {
                ProvideTextStyle(M3EText.rowSubtitle, text)
            }
        }
        content?.let {
            Box(
                modifier = Modifier.padding(
                    top = if (title != null || text != null) 16.dp else 0.dp,
                    start = 16.dp,
                    end = 16.dp,
                ),
            ) {
                content()
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .fillMaxWidth(),
        ) {
            buttons()
            BottomSpacer()
        }
    }
}
