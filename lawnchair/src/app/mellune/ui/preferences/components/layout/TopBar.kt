package app.mellune.ui.preferences.components.layout

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.mellune.ui.m3e.M3EExpressiveHeader
import com.android.launcher3.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("UNUSED_PARAMETER")
fun TopBar(
    backArrowVisible: Boolean,
    label: String,
    isExpandedScreen: Boolean,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val backLabel = stringResource(id = R.string.accessibility_close)

    M3EExpressiveHeader(
        title = label,
        modifier = modifier,
        onBack = if (backArrowVisible && backDispatcher != null) {
            { backDispatcher.onBackPressed() }
        } else {
            null
        },
        backContentDescription = backLabel,
        maxTitleLines = if (isExpandedScreen) 1 else 2,
        actions = actions,
    )
}
