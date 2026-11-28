package app.mellune.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .safeDrawingPadding(),
    ) {
        title?.let {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    contentColor = MaterialTheme.colorScheme.primary,
                ) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        ProvideTextStyle(M3EText.pageTitle, title)
                    }
                }
            }
        }
        text?.let {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, top = 10.dp, end = 22.dp),
            ) {
                ProvideTextStyle(M3EText.rowSubtitle, text)
            }
        }
        content?.let {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = if (title != null || text != null) 14.dp else 4.dp,
                        start = 16.dp,
                        end = 16.dp,
                    ),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.72f),
            ) {
                Box(modifier = Modifier.padding(4.dp)) {
                    content()
                }
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
