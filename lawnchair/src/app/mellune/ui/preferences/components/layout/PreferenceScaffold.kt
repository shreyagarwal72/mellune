/*
 * Copyright 2022, Lawnchair
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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.mellune.ui.m3e.M3EPageBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferenceScaffold(
    label: String,
    isExpandedScreen: Boolean,
    modifier: Modifier = Modifier,
    backArrowVisible: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = { BottomSpacer() },
    content: @Composable (PaddingValues) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        M3EPageBackground(pageKey = label)
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                TopBar(
                    backArrowVisible = backArrowVisible,
                    label = label,
                    isExpandedScreen = isExpandedScreen,
                    actions = actions,
                )
            },
            bottomBar = bottomBar,
        ) {
            content(it)
        }
    }
}
