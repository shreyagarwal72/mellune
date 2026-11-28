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

package app.mellune.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.android.launcher3.R

private const val FLEX_FEATURES = "kern, liga, calt, ccmp"

private data class PetalFontAxes(
    val weight: Int,
    val width: Float,
    val opsz: Float,
    val grade: Int,
    val slant: Float,
    val roundness: Float,
)

@OptIn(ExperimentalTextApi::class)
private fun flexFamily(axes: PetalFontAxes): FontFamily = try {
    FontFamily(
        (100..1000 step 50).map { w ->
            Font(
                resId = R.font.googlesansflex_variable,
                weight = FontWeight(w),
                style = FontStyle.Normal,
                variationSettings = FontVariation.Settings(
                    FontVariation.weight(w),
                    FontVariation.width(axes.width),
                    FontVariation.Setting("opsz", axes.opsz.coerceIn(6f, 72f)),
                    FontVariation.grade(axes.grade),
                    FontVariation.slant(axes.slant),
                    FontVariation.Setting("ROND", axes.roundness.coerceIn(0f, 100f)),
                ),
            )
        },
    )
} catch (_: Throwable) {
    FontFamily.Default
}

private val displayAxes = PetalFontAxes(
    weight = 950,
    width = 85f,
    opsz = 30f,
    grade = 0,
    slant = 0f,
    roundness = 100f,
)
private val headlineAxes = PetalFontAxes(
    weight = 700,
    width = 115f,
    opsz = 32f,
    grade = 0,
    slant = 0f,
    roundness = 60f,
)
private val bodyAxes = PetalFontAxes(
    weight = 450,
    width = 100f,
    opsz = 16f,
    grade = 20,
    slant = 0f,
    roundness = 0f,
)

private val displayFamily = flexFamily(displayAxes)
private val headlineFamily = flexFamily(headlineAxes)
private val bodyFamily = flexFamily(bodyAxes)

private fun petalStyle(
    family: FontFamily,
    weight: Int,
    sizeSp: Int,
    lineSp: Int,
    letterSp: Float,
) = TextStyle(
    fontFamily = family,
    fontWeight = FontWeight(weight.coerceIn(100, 1000)),
    fontSynthesis = FontSynthesis.None,
    fontFeatureSettings = FLEX_FEATURES,
    fontSize = sizeSp.sp,
    lineHeight = lineSp.sp,
    letterSpacing = letterSp.sp,
)

val Typography = Typography(
    displayLarge = petalStyle(displayFamily, displayAxes.weight, 57, 64, -0.25f),
    displayMedium = petalStyle(displayFamily, displayAxes.weight, 45, 52, 0f),
    displaySmall = petalStyle(displayFamily, displayAxes.weight, 36, 44, 0f),
    headlineLarge = petalStyle(headlineFamily, headlineAxes.weight, 32, 40, 0f),
    headlineMedium = petalStyle(headlineFamily, headlineAxes.weight, 28, 36, 0f),
    headlineSmall = petalStyle(headlineFamily, headlineAxes.weight, 24, 32, 0f),
    titleLarge = petalStyle(headlineFamily, headlineAxes.weight, 22, 28, 0f),
    titleMedium = petalStyle(headlineFamily, headlineAxes.weight, 16, 24, 0.15f),
    titleSmall = petalStyle(headlineFamily, headlineAxes.weight, 14, 20, 0.1f),
    bodyLarge = petalStyle(bodyFamily, bodyAxes.weight, 16, 24, 0.5f),
    bodyMedium = petalStyle(bodyFamily, bodyAxes.weight, 14, 20, 0.25f),
    bodySmall = petalStyle(bodyFamily, bodyAxes.weight, 12, 16, 0.4f),
    labelLarge = petalStyle(bodyFamily, bodyAxes.weight + 150, 14, 20, 0.1f),
    labelMedium = petalStyle(bodyFamily, bodyAxes.weight + 150, 12, 16, 0.5f),
    labelSmall = petalStyle(bodyFamily, bodyAxes.weight + 150, 11, 16, 0.5f),
    displayLargeEmphasized = petalStyle(displayFamily, displayAxes.weight, 57, 64, -0.25f),
    displayMediumEmphasized = petalStyle(displayFamily, displayAxes.weight, 45, 52, 0f),
    displaySmallEmphasized = petalStyle(displayFamily, displayAxes.weight, 36, 44, 0f),
    headlineLargeEmphasized = petalStyle(headlineFamily, headlineAxes.weight, 32, 40, 0f),
    headlineMediumEmphasized = petalStyle(headlineFamily, headlineAxes.weight, 28, 36, 0f),
    headlineSmallEmphasized = petalStyle(headlineFamily, headlineAxes.weight, 24, 32, 0f),
    titleLargeEmphasized = petalStyle(headlineFamily, headlineAxes.weight, 22, 28, 0f),
    titleMediumEmphasized = petalStyle(headlineFamily, headlineAxes.weight, 16, 24, 0.15f),
    titleSmallEmphasized = petalStyle(headlineFamily, headlineAxes.weight, 14, 20, 0.1f),
    bodyLargeEmphasized = petalStyle(bodyFamily, bodyAxes.weight + 100, 16, 24, 0.5f),
    bodyMediumEmphasized = petalStyle(bodyFamily, bodyAxes.weight + 100, 14, 20, 0.25f),
    bodySmallEmphasized = petalStyle(bodyFamily, bodyAxes.weight + 100, 12, 16, 0.4f),
    labelLargeEmphasized = petalStyle(bodyFamily, bodyAxes.weight + 200, 14, 20, 0.1f),
    labelMediumEmphasized = petalStyle(bodyFamily, bodyAxes.weight + 200, 12, 16, 0.5f),
    labelSmallEmphasized = petalStyle(bodyFamily, bodyAxes.weight + 200, 11, 16, 0.5f),
)
