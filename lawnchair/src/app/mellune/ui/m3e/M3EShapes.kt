package app.mellune.ui.m3e

import android.graphics.Matrix
import android.graphics.RectF
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath

enum class ExpressiveButtonPreset(val id: String, val label: String) {
    PILL("pill", "Pill"),
    CIRCLE("circle", "Circle"),
    COOKIE("cookie", "Cookie"),
    FLOWER("flower", "Flower"),
    CLOVER("clover", "Clover"),
    SUNNY("sunny", "Sunny"),
    GEM("gem", "Gem");

    companion object {
        val variety = listOf(COOKIE, FLOWER, CLOVER, GEM, SUNNY)
        fun fromId(id: String?): ExpressiveButtonPreset =
            entries.firstOrNull { it.id == id } ?: COOKIE
    }
}

object M3EShapeState {
    var preset by mutableStateOf(ExpressiveButtonPreset.COOKIE)
    var enabled by mutableStateOf(true)
}

fun RoundedPolygon.toShape(): Shape {
    val normalized = this.normalized()
    val unit = android.graphics.Path().also { normalized.toPath(it) }
    return GenericShape { size, _ ->
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@GenericShape
        val bounds = RectF()
        unit.computeBounds(bounds, true)
        val bw = bounds.width().coerceAtLeast(1e-4f)
        val bh = bounds.height().coerceAtLeast(1e-4f)
        val scale = minOf(w / bw, h / bh)
        val matrix = Matrix()
        matrix.postTranslate(-bounds.centerX(), -bounds.centerY())
        matrix.postScale(scale, scale)
        matrix.postTranslate(w / 2f, h / 2f)
        val out = android.graphics.Path()
        unit.transform(matrix, out)
        addPath(out.asComposePath())
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun buildExpressivePath(
    preset: ExpressiveButtonPreset,
    cx: Float,
    cy: Float,
    radius: Float,
): Path {
    val poly = when (preset) {
        ExpressiveButtonPreset.CIRCLE -> MaterialShapes.Circle
        ExpressiveButtonPreset.PILL -> MaterialShapes.Pill
        ExpressiveButtonPreset.COOKIE -> MaterialShapes.Cookie7Sided
        ExpressiveButtonPreset.FLOWER -> MaterialShapes.Flower
        ExpressiveButtonPreset.CLOVER -> MaterialShapes.Clover4Leaf
        ExpressiveButtonPreset.SUNNY -> MaterialShapes.Sunny
        ExpressiveButtonPreset.GEM -> MaterialShapes.Gem
    }
    val unit = android.graphics.Path().also { poly.normalized().toPath(it) }
    val bounds = RectF()
    unit.computeBounds(bounds, true)
    val maxDim = maxOf(bounds.width(), bounds.height()).coerceAtLeast(1e-4f)
    val scale = (radius * 2f) / maxDim
    val matrix = Matrix()
    matrix.postTranslate(-bounds.centerX(), -bounds.centerY())
    matrix.postScale(scale, scale)
    matrix.postTranslate(cx, cy)
    val out = android.graphics.Path()
    unit.transform(matrix, out)
    return out.asComposePath()
}

data class ExpressiveShapeHolder(
    val name: String,
    val polygon: RoundedPolygon,
) {
    val cachedShape: Shape by lazy { polygon.toShape() }
    fun toShape(): Shape = cachedShape
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object M3EMaterialShapes {
    val Circle by lazy { ExpressiveShapeHolder("Circle", MaterialShapes.Circle) }
    val Square by lazy { ExpressiveShapeHolder("Square", MaterialShapes.Square) }
    val Pill by lazy { ExpressiveShapeHolder("Pill", MaterialShapes.Pill) }
    val Sunny by lazy { ExpressiveShapeHolder("Sunny", MaterialShapes.Sunny) }
    val Cookie4Sided by lazy { ExpressiveShapeHolder("Cookie 4-Sided", MaterialShapes.Cookie4Sided) }
    val Cookie7Sided by lazy { ExpressiveShapeHolder("Cookie 7-Sided", MaterialShapes.Cookie7Sided) }
    val Cookie9Sided by lazy { ExpressiveShapeHolder("Cookie 9-Sided", MaterialShapes.Cookie9Sided) }
    val Cookie12Sided by lazy { ExpressiveShapeHolder("Cookie 12-Sided", MaterialShapes.Cookie12Sided) }
    val Flower by lazy { ExpressiveShapeHolder("Flower", MaterialShapes.Flower) }
    val Clover4Leaf by lazy { ExpressiveShapeHolder("Clover 4-Leaf", MaterialShapes.Clover4Leaf) }
    val Clover8Leaf by lazy { ExpressiveShapeHolder("Clover 8-Leaf", MaterialShapes.Clover8Leaf) }
    val SoftBurst by lazy { ExpressiveShapeHolder("Soft Burst", MaterialShapes.SoftBurst) }
    val Puffy by lazy { ExpressiveShapeHolder("Puffy", MaterialShapes.Puffy) }
    val PuffyDiamond by lazy { ExpressiveShapeHolder("Puffy Diamond", MaterialShapes.PuffyDiamond) }
    val Burst by lazy { ExpressiveShapeHolder("Burst", MaterialShapes.Burst) }
    val Gem by lazy { ExpressiveShapeHolder("Gem", MaterialShapes.Gem) }
}

private val M3EBadgeMixedPool by lazy {
    listOf(
        M3EMaterialShapes.Cookie12Sided,
        M3EMaterialShapes.Sunny,
        M3EMaterialShapes.Flower,
        M3EMaterialShapes.Clover4Leaf,
        M3EMaterialShapes.Cookie9Sided,
        M3EMaterialShapes.SoftBurst,
        M3EMaterialShapes.Puffy,
        M3EMaterialShapes.Cookie7Sided,
    )
}

val ExpressiveButtonShape: Shape = RoundedCornerShape(50)

fun expressiveIconShape(preset: ExpressiveButtonPreset = M3EShapeState.preset): Shape = when (preset) {
    ExpressiveButtonPreset.PILL, ExpressiveButtonPreset.CIRCLE -> RoundedCornerShape(50)
    ExpressiveButtonPreset.COOKIE -> M3EMaterialShapes.Cookie12Sided.toShape()
    ExpressiveButtonPreset.FLOWER -> M3EMaterialShapes.Flower.toShape()
    ExpressiveButtonPreset.CLOVER -> M3EMaterialShapes.Clover4Leaf.toShape()
    ExpressiveButtonPreset.SUNNY -> M3EMaterialShapes.Sunny.toShape()
    ExpressiveButtonPreset.GEM -> M3EMaterialShapes.Gem.toShape()
}

fun expressiveBadgeShapeForKey(key: Any?): Shape {
    if (!M3EShapeState.enabled) return RoundedCornerShape(14.dp)
    val idx = Math.floorMod(key?.hashCode() ?: 0, M3EBadgeMixedPool.size)
    return M3EBadgeMixedPool[idx].toShape()
}

fun smoothRoundedShape(
    topStart: Dp = 0.dp,
    topEnd: Dp = 0.dp,
    bottomEnd: Dp = 0.dp,
    bottomStart: Dp = 0.dp,
): RoundedCornerShape = RoundedCornerShape(
    topStart = topStart,
    topEnd = topEnd,
    bottomEnd = bottomEnd,
    bottomStart = bottomStart,
)

@Composable
fun ExpressiveIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    preset: ExpressiveButtonPreset? = null,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = M3ESpring.quick(),
        label = "expressiveIconScale",
    )
    val shape = expressiveIconShape(preset ?: M3EShapeState.preset)
    Surface(
        onClick = onClick,
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(shape),
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        interactionSource = source,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(iconSize))
        }
    }
}

@Composable
fun ExpressiveIconButton(
    painter: Painter,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    preset: ExpressiveButtonPreset? = null,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = M3ESpring.quick(),
        label = "expressivePainterIconScale",
    )
    val shape = expressiveIconShape(preset ?: M3EShapeState.preset)
    Surface(
        onClick = onClick,
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(shape),
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        interactionSource = source,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(painter, contentDescription = contentDescription, modifier = Modifier.size(iconSize))
        }
    }
}

enum class M3EGroupPosition { TOP, MIDDLE, BOTTOM, SINGLE }

fun m3eGroupPositionFor(index: Int, count: Int): M3EGroupPosition = when {
    count <= 1 -> M3EGroupPosition.SINGLE
    index == 0 -> M3EGroupPosition.TOP
    index == count - 1 -> M3EGroupPosition.BOTTOM
    else -> M3EGroupPosition.MIDDLE
}

fun m3eGroupPositionFor(cutTop: Boolean, cutBottom: Boolean): M3EGroupPosition = when {
    !cutTop && !cutBottom -> M3EGroupPosition.SINGLE
    !cutTop && cutBottom -> M3EGroupPosition.TOP
    cutTop && !cutBottom -> M3EGroupPosition.BOTTOM
    else -> M3EGroupPosition.MIDDLE
}

fun m3eGroupShape(position: M3EGroupPosition): CornerBasedShape = when (position) {
    M3EGroupPosition.SINGLE -> RoundedCornerShape(24.dp)
    M3EGroupPosition.TOP -> RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 24.dp,
        bottomStart = 6.dp,
        bottomEnd = 6.dp,
    )
    M3EGroupPosition.BOTTOM -> RoundedCornerShape(
        topStart = 6.dp,
        topEnd = 6.dp,
        bottomStart = 24.dp,
        bottomEnd = 24.dp,
    )
    M3EGroupPosition.MIDDLE -> RoundedCornerShape(6.dp)
}
