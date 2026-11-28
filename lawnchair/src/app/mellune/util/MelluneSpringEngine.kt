package app.mellune.util

import android.animation.ValueAnimator
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import androidx.annotation.RequiresApi
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import app.mellune.preferences2.PreferenceManager2
import app.mellune.preferences2.firstCached
import com.android.launcher3.BubbleTextView
import com.android.launcher3.CellLayout
import com.android.launcher3.Hotseat
import com.android.launcher3.Workspace
import com.android.launcher3.util.Themes
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Three-layer flagship spring parallax engine for Mellune Launcher.
 *
 * Coordinates foreground (launched icon / hotseat), midground (workspace container and active
 * page icons/widgets), and background (wallpaper zoom-out and API 31+ RenderEffect blur) across
 * app open, app close/return, and screen unlock transitions.
 */
object MelluneSpringEngine {

    private const val STIFFNESS_TACTILE = 1200f
    private const val STIFFNESS_MID = 520f
    private const val STIFFNESS_BLOOM = 420f
    private const val STIFFNESS_RETURN_ICON = 560f
    private const val STIFFNESS_UNLOCK = 380f
    private const val STIFFNESS_UNLOCK_HOTSEAT = 430f

    private const val DAMPING_TACTILE = 0.78f
    private const val DAMPING_BOUNCY = 0.72f
    private const val DAMPING_SMOOTH = 0.86f
    private const val DAMPING_BLOOM = 0.84f
    private const val DAMPING_UNLOCK = 0.80f

    private const val WALLPAPER_ZOOM_OPEN = 0.42f
    private const val WALLPAPER_ZOOM_UNLOCK_START = 0.36f
    private const val LAUNCH_BLUR_RADIUS = 8f

    private val zoomInterpolator = PathInterpolator(0.22f, 1.0f, 0.36f, 1.0f)

    private val activeSprings =
        WeakHashMap<View, MutableMap<DynamicAnimation.ViewProperty, SpringAnimation>>()
    private val trackedChildren = mutableListOf<WeakReference<View>>()

    private var wallpaperZoomAnimator: ValueAnimator? = null
    private var workspaceBlurAnimator: ValueAnimator? = null
    private var currentWallpaperZoom = 0f
    private var currentWorkspaceBlur = 0f
    private var appliedWorkspaceBlur = false
    private var transitionGeneration = 0

    /**
     * Triggers 3-layer app-open parallax when launching an item from the workspace or hotseat.
     */
    fun playAppOpenParallax(
        workspace: Workspace<*>?,
        hotseat: Hotseat?,
        launchedView: View?,
        windowToken: IBinder?,
        context: Context,
    ) {
        val gen = ++transitionGeneration
        val density = context.resources.displayMetrics.density

        if (launchedView != null) {
            playLaunchSink(launchedView)
        }

        if (workspace != null) {
            animateSpring(workspace, DynamicAnimation.SCALE_X, 0.95f, STIFFNESS_MID, DAMPING_SMOOTH)
            animateSpring(workspace, DynamicAnimation.SCALE_Y, 0.95f, STIFFNESS_MID, DAMPING_SMOOTH)
            animateSpring(workspace, DynamicAnimation.ALPHA, 0.94f, STIFFNESS_MID, DAMPING_SMOOTH)
            animateWorkspaceBlur(workspace, targetRadius = LAUNCH_BLUR_RADIUS, durationMs = 180L)

            spreadSurroundingItems(
                container = currentPageContainer(workspace),
                launchedView = launchedView,
                density = density,
            )
        }

        if (hotseat != null && launchedView !== hotseat) {
            val sinkY = 10f * density
            animateSpring(hotseat, DynamicAnimation.SCALE_X, 0.93f, STIFFNESS_MID, DAMPING_SMOOTH)
            animateSpring(hotseat, DynamicAnimation.SCALE_Y, 0.93f, STIFFNESS_MID, DAMPING_SMOOTH)
            animateSpring(hotseat, DynamicAnimation.TRANSLATION_Y, sinkY, STIFFNESS_MID, DAMPING_SMOOTH)
            animateSpring(hotseat, DynamicAnimation.ALPHA, 0.88f, STIFFNESS_MID, DAMPING_SMOOTH)
        }

        animateWallpaperZoom(
            context = context,
            windowToken = windowToken,
            fromZoom = null,
            targetZoom = WALLPAPER_ZOOM_OPEN,
            durationMs = 220L,
            generation = gen,
        )
    }

    /**
     * Triggers 3-layer return parallax when resuming home after closing or leaving an app.
     */
    fun playAppCloseParallax(
        workspace: Workspace<*>?,
        hotseat: Hotseat?,
        lastLaunchedView: View?,
        windowToken: IBinder?,
        context: Context,
    ) {
        val gen = ++transitionGeneration
        val density = context.resources.displayMetrics.density

        animateWallpaperZoom(
            context = context,
            windowToken = windowToken,
            fromZoom = max(currentWallpaperZoom, 0.34f),
            targetZoom = 0f,
            durationMs = 380L,
            generation = gen,
        )

        if (workspace != null) {
            val startScale = if (workspace.scaleX < 0.96f) workspace.scaleX else 0.92f
            val startAlpha = if (workspace.alpha < 0.96f) workspace.alpha else 0.82f
            animateWorkspaceBlur(workspace, targetRadius = 0f, durationMs = 220L)
            animateSpring(
                workspace,
                DynamicAnimation.SCALE_X,
                1.0f,
                STIFFNESS_BLOOM,
                DAMPING_BLOOM,
                startValue = startScale,
            )
            animateSpring(
                workspace,
                DynamicAnimation.SCALE_Y,
                1.0f,
                STIFFNESS_BLOOM,
                DAMPING_BLOOM,
                startValue = startScale,
            )
            animateSpring(
                workspace,
                DynamicAnimation.ALPHA,
                1.0f,
                STIFFNESS_BLOOM,
                DAMPING_BLOOM,
                startValue = startAlpha,
            )
        }

        restoreTrackedChildren(lastLaunchedView)
        settleActivePageChildren(currentPageContainer(workspace), lastLaunchedView)

        if (hotseat != null) {
            val startScale = if (hotseat.scaleX < 0.97f) hotseat.scaleX else 0.95f
            val startY = if (hotseat.translationY > 2f) hotseat.translationY else 12f * density
            val startAlpha = if (hotseat.alpha < 0.97f) hotseat.alpha else 0.88f
            animateSpring(
                hotseat,
                DynamicAnimation.SCALE_X,
                1.0f,
                STIFFNESS_BLOOM,
                DAMPING_BOUNCY,
                startValue = startScale,
            )
            animateSpring(
                hotseat,
                DynamicAnimation.SCALE_Y,
                1.0f,
                STIFFNESS_BLOOM,
                DAMPING_BOUNCY,
                startValue = startScale,
            )
            animateSpring(
                hotseat,
                DynamicAnimation.TRANSLATION_Y,
                0f,
                STIFFNESS_BLOOM,
                DAMPING_BOUNCY,
                startValue = startY,
            )
            animateSpring(
                hotseat,
                DynamicAnimation.ALPHA,
                1.0f,
                STIFFNESS_BLOOM,
                DAMPING_SMOOTH,
                startValue = startAlpha,
            )
        }

        if (lastLaunchedView != null &&
            lastLaunchedView.isAttachedToWindow &&
            lastLaunchedView.isShown
        ) {
            playReturnRebound(lastLaunchedView)
        }
    }

    /**
     * Triggers staggered 3-layer bloom parallax when unlocking onto the home screen.
     */
    /**
     * Triggers 3-layer unlock parallax when returning from keyguard / screen-off to home.
     * Supports Pixel Launcher 17's concentric Ring-Appear reveal and API 33+ AGSL unlock shimmer.
     */
    fun playUnlockParallax(
        workspace: Workspace<*>?,
        hotseat: Hotseat?,
        windowToken: IBinder?,
        context: Context,
        dragLayer: ViewGroup? = null,
        useRingAnimation: Boolean = true,
        useShaderShimmer: Boolean = true,
    ) {
        val gen = ++transitionGeneration
        val density = context.resources.displayMetrics.density

        animateWallpaperZoom(
            context = context,
            windowToken = windowToken,
            fromZoom = WALLPAPER_ZOOM_UNLOCK_START,
            targetZoom = 0f,
            durationMs = 460L,
            generation = gen,
        )

        if (workspace != null) {
            clearWorkspaceBlurImmediately(workspace)
            animateSpring(
                workspace,
                DynamicAnimation.SCALE_X,
                1.0f,
                STIFFNESS_UNLOCK,
                DAMPING_UNLOCK,
                startValue = 0.96f,
            )
            animateSpring(
                workspace,
                DynamicAnimation.SCALE_Y,
                1.0f,
                STIFFNESS_UNLOCK,
                DAMPING_UNLOCK,
                startValue = 0.96f,
            )
            animateSpring(
                workspace,
                DynamicAnimation.ALPHA,
                1.0f,
                STIFFNESS_UNLOCK,
                DAMPING_SMOOTH,
                startValue = 0.85f,
            )
            val currentPage = currentCellLayout(workspace)
            val container = currentPage?.shortcutsAndWidgets
            if (useRingAnimation && currentPage != null && container != null) {
                staggerUnlockRingChildren(
                    cellLayout = currentPage,
                    container = container,
                    generation = gen,
                )
            } else {
                staggerUnlockPageChildren(
                    container = container,
                    density = density,
                    generation = gen,
                )
            }
        }

        if (hotseat != null) {
            animateSpring(
                hotseat,
                DynamicAnimation.SCALE_X,
                1.0f,
                STIFFNESS_UNLOCK_HOTSEAT,
                DAMPING_UNLOCK,
                startValue = 0.94f,
            )
            animateSpring(
                hotseat,
                DynamicAnimation.SCALE_Y,
                1.0f,
                STIFFNESS_UNLOCK_HOTSEAT,
                DAMPING_UNLOCK,
                startValue = 0.94f,
            )
            animateSpring(
                hotseat,
                DynamicAnimation.TRANSLATION_Y,
                0f,
                STIFFNESS_UNLOCK_HOTSEAT,
                DAMPING_UNLOCK,
                startValue = 20f * density,
            )
            animateSpring(
                hotseat,
                DynamicAnimation.ALPHA,
                1.0f,
                STIFFNESS_UNLOCK_HOTSEAT,
                DAMPING_SMOOTH,
                startValue = 0.4f,
            )
        }

        if (useShaderShimmer && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && dragLayer != null) {
            playUnlockShaderShimmer(dragLayer, context, gen)
        }
    }

    /**
     * Triggers flagship self-created spring choreography when returning to the home screen
     * via gesture swipe-up, 3-button Home, or predictive back-to-home.
     */
    @JvmOverloads
    fun playHomeReturnChoreography(
        workspace: Workspace<*>?,
        hotseat: Hotseat?,
        dragLayer: ViewGroup?,
        targetRect: RectF?,
        context: Context,
        fromPredictiveBack: Boolean = false,
    ) {
        val prefs = PreferenceManager2.getInstance(context)
        if (!prefs.homeReturnAnimations.firstCached()) {
            return
        }
        val style = prefs.homeReturnStyle.firstCached()
        val intensity = prefs.homeReturnIntensity.firstCached().coerceIn(0.4f, 1.5f)
        val useSheen = prefs.homeReturnSheen.firstCached()
        val gen = ++transitionGeneration
        val density = context.resources.displayMetrics.density

        if (workspace != null) {
            clearWorkspaceBlurImmediately(workspace)
            val container = currentPageContainer(workspace)
            when (style) {
                "minimal_elastic" -> {
                    val startScale = (1f - 0.035f * intensity).coerceIn(0.92f, 0.99f)
                    animateSpring(
                        workspace,
                        DynamicAnimation.SCALE_X,
                        1.0f,
                        STIFFNESS_RETURN_ICON,
                        DAMPING_UNLOCK,
                        startValue = startScale,
                    )
                    animateSpring(
                        workspace,
                        DynamicAnimation.SCALE_Y,
                        1.0f,
                        STIFFNESS_RETURN_ICON,
                        DAMPING_UNLOCK,
                        startValue = startScale,
                    )
                }

                "parallax_lift" -> {
                    if (container != null) {
                        staggerParallaxLiftChildren(
                            container = container,
                            density = density,
                            intensity = intensity,
                            fromPredictiveBack = fromPredictiveBack,
                            generation = gen,
                        )
                    }
                }

                else -> {
                    if (container != null) {
                        staggerHomeReturnChildren(
                            container = container,
                            targetRect = targetRect,
                            density = density,
                            intensity = intensity,
                            fromPredictiveBack = fromPredictiveBack,
                            generation = gen,
                        )
                    }
                }
            }
        }

        if (hotseat != null && style != "minimal_elastic") {
            val hotseatScale = (1f - 0.05f * intensity).coerceIn(0.90f, 0.98f)
            val liftY = 14f * density * intensity
            animateSpring(
                hotseat,
                DynamicAnimation.SCALE_X,
                1.0f,
                STIFFNESS_UNLOCK_HOTSEAT,
                DAMPING_UNLOCK,
                startValue = hotseatScale,
            )
            animateSpring(
                hotseat,
                DynamicAnimation.SCALE_Y,
                1.0f,
                STIFFNESS_UNLOCK_HOTSEAT,
                DAMPING_UNLOCK,
                startValue = hotseatScale,
            )
            animateSpring(
                hotseat,
                DynamicAnimation.TRANSLATION_Y,
                0f,
                STIFFNESS_UNLOCK_HOTSEAT,
                DAMPING_UNLOCK,
                startValue = liftY,
            )
        }

        if (useSheen && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && dragLayer != null) {
            playUnlockShaderShimmer(dragLayer, context, gen)
        }
    }

    private fun staggerHomeReturnChildren(
        container: ViewGroup,
        targetRect: RectF?,
        density: Float,
        intensity: Float,
        fromPredictiveBack: Boolean,
        generation: Int,
    ) {
        if (container.childCount == 0) return

        val containerLoc = IntArray(2)
        container.getLocationInWindow(containerLoc)
        val hasTarget = targetRect != null && !targetRect.isEmpty
        val anchorX = if (hasTarget) targetRect!!.centerX() else containerLoc[0] + container.width / 2f
        val anchorY = if (hasTarget) targetRect!!.centerY() else containerLoc[1] + container.height / 2f
        val maxRadius = max(hypot(container.width.toFloat(), container.height.toFloat()), 1f)
        val childLoc = IntArray(2)

        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i) ?: continue
            if (child.visibility != View.VISIBLE) continue

            trackChild(child)
            child.getLocationInWindow(childLoc)
            val cx = childLoc[0] + child.width / 2f
            val cy = childLoc[1] + child.height / 2f
            val dx = cx - anchorX
            val dy = cy - anchorY
            val dist = hypot(dx, dy)

            if (hasTarget && dist < min(child.width, child.height) * 0.55f) {
                playReturnRebound(child)
                continue
            }

            val normDist = min(dist / maxRadius, 1f)
            val delayMs = (normDist * 65f).toLong().coerceAtMost(75L)
            val safeDist = max(dist, 1f)
            val nudge = 7f * density * intensity * (1f - normDist * 0.45f)
            val startScale = (1f - 0.06f * intensity).coerceIn(0.88f, 0.98f)
            val startAlpha = if (fromPredictiveBack) 1f else 0.90f

            val runSpring = {
                if (transitionGeneration == generation && child.isAttachedToWindow) {
                    animateSpring(
                        child,
                        DynamicAnimation.SCALE_X,
                        1.0f,
                        STIFFNESS_RETURN_ICON,
                        DAMPING_UNLOCK,
                        startValue = startScale,
                    )
                    animateSpring(
                        child,
                        DynamicAnimation.SCALE_Y,
                        1.0f,
                        STIFFNESS_RETURN_ICON,
                        DAMPING_UNLOCK,
                        startValue = startScale,
                    )
                    animateSpring(
                        child,
                        DynamicAnimation.TRANSLATION_X,
                        0f,
                        STIFFNESS_RETURN_ICON,
                        DAMPING_UNLOCK,
                        startValue = (dx / safeDist) * nudge,
                    )
                    animateSpring(
                        child,
                        DynamicAnimation.TRANSLATION_Y,
                        0f,
                        STIFFNESS_RETURN_ICON,
                        DAMPING_UNLOCK,
                        startValue = (dy / safeDist) * nudge,
                    )
                    if (startAlpha < 1f) {
                        animateSpring(
                            child,
                            DynamicAnimation.ALPHA,
                            1.0f,
                            STIFFNESS_RETURN_ICON,
                            DAMPING_SMOOTH,
                            startValue = startAlpha,
                        )
                    }
                }
            }

            if (delayMs == 0L) {
                runSpring()
            } else {
                child.postDelayed(runSpring, delayMs)
            }
        }
    }

    private fun staggerParallaxLiftChildren(
        container: ViewGroup,
        density: Float,
        intensity: Float,
        fromPredictiveBack: Boolean,
        generation: Int,
    ) {
        if (container.childCount == 0) return
        val containerHeight = max(container.height.toFloat(), 1f)
        val startScale = (1f - 0.05f * intensity).coerceIn(0.90f, 0.98f)
        val startAlpha = if (fromPredictiveBack) 1f else 0.88f

        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i) ?: continue
            if (child.visibility != View.VISIBLE) continue

            trackChild(child)
            val centerY = child.top + child.height / 2f
            val fromBottom = (1f - (centerY / containerHeight).coerceIn(0f, 1f))
            val delayMs = (fromBottom * 60f).toLong().coerceAtMost(65L)
            val liftY = (10f + 8f * fromBottom) * density * intensity

            val runSpring = {
                if (transitionGeneration == generation && child.isAttachedToWindow) {
                    animateSpring(
                        child,
                        DynamicAnimation.SCALE_X,
                        1.0f,
                        STIFFNESS_RETURN_ICON,
                        DAMPING_UNLOCK,
                        startValue = startScale,
                    )
                    animateSpring(
                        child,
                        DynamicAnimation.SCALE_Y,
                        1.0f,
                        STIFFNESS_RETURN_ICON,
                        DAMPING_UNLOCK,
                        startValue = startScale,
                    )
                    animateSpring(
                        child,
                        DynamicAnimation.TRANSLATION_Y,
                        0f,
                        STIFFNESS_RETURN_ICON,
                        DAMPING_UNLOCK,
                        startValue = liftY,
                    )
                    if (startAlpha < 1f) {
                        animateSpring(
                            child,
                            DynamicAnimation.ALPHA,
                            1.0f,
                            STIFFNESS_RETURN_ICON,
                            DAMPING_SMOOTH,
                            startValue = startAlpha,
                        )
                    }
                }
            }

            if (delayMs == 0L) {
                runSpring()
            } else {
                child.postDelayed(runSpring, delayMs)
            }
        }
    }

    /**
     * Plays immediate tactile sink on the tapped icon at launch start.
     */
    fun playLaunchSink(view: View) {
        trackChild(view)
        animateSpring(view, DynamicAnimation.SCALE_X, 0.86f, STIFFNESS_TACTILE, DAMPING_TACTILE)
        animateSpring(view, DynamicAnimation.SCALE_Y, 0.86f, STIFFNESS_TACTILE, DAMPING_TACTILE)
        animateSpring(view, DynamicAnimation.ALPHA, 0.92f, STIFFNESS_TACTILE, DAMPING_SMOOTH)
    }

    /**
     * Rebounds a launched icon back to resting scale and full opacity.
     */
    fun playReturnRebound(view: View) {
        trackChild(view)
        val startScale = if (view.scaleX < 0.94f) view.scaleX else 0.84f
        animateSpring(
            view,
            DynamicAnimation.SCALE_X,
            1.0f,
            STIFFNESS_RETURN_ICON,
            DAMPING_BOUNCY,
            startValue = startScale,
        )
        animateSpring(
            view,
            DynamicAnimation.SCALE_Y,
            1.0f,
            STIFFNESS_RETURN_ICON,
            DAMPING_BOUNCY,
            startValue = startScale,
        )
        animateSpring(view, DynamicAnimation.TRANSLATION_X, 0f, STIFFNESS_RETURN_ICON, DAMPING_BOUNCY)
        animateSpring(view, DynamicAnimation.TRANSLATION_Y, 0f, STIFFNESS_RETURN_ICON, DAMPING_BOUNCY)
        animateSpring(view, DynamicAnimation.ALPHA, 1.0f, STIFFNESS_RETURN_ICON, DAMPING_SMOOTH)
    }

    /**
     * Immediately resets any parallax transforms, blur, and wallpaper zoom to resting values.
     */
    fun resetRestState(
        workspace: Workspace<*>?,
        hotseat: Hotseat?,
        windowToken: IBinder?,
        context: Context,
    ) {
        ++transitionGeneration
        wallpaperZoomAnimator?.cancel()
        wallpaperZoomAnimator = null
        currentWallpaperZoom = 0f
        setWallpaperZoomSafe(context, windowToken, 0f)

        workspaceBlurAnimator?.cancel()
        workspaceBlurAnimator = null
        currentWorkspaceBlur = 0f
        if (workspace != null) {
            clearWorkspaceBlurImmediately(workspace)
            resetViewImmediately(workspace)
            resetContainerChildrenImmediately(currentPageContainer(workspace))
        }
        if (hotseat != null) {
            resetViewImmediately(hotseat)
        }
        for (ref in trackedChildren) {
            val child = ref.get() ?: continue
            resetViewImmediately(child)
        }
        trackedChildren.clear()
    }

    private fun currentCellLayout(workspace: Workspace<*>?): CellLayout? {
        if (workspace == null || workspace.pageCount <= 0) return null
        return workspace.getPageAt(workspace.currentPage) as? CellLayout
    }

    private fun currentPageContainer(workspace: Workspace<*>?): ViewGroup? =
        currentCellLayout(workspace)?.shortcutsAndWidgets

    private fun staggerUnlockRingChildren(
        cellLayout: CellLayout,
        container: ViewGroup,
        generation: Int,
    ) {
        if (container.childCount == 0) return

        val countX = max(cellLayout.countX, 1)
        val countY = max(cellLayout.countY, 1)
        val gridCenterX = container.width / 2f
        val gridCenterY = container.height / 2f

        // Partition workspace children into concentric rectangular rings walking inward
        // (matching Pixel Launcher 17's RingAppearAnimation choreography).
        val seen = LinkedHashSet<View>()
        val rings = mutableListOf<List<View>>()
        var left = 0
        var top = 0
        var right = countX - 1
        var bottom = countY - 1

        while (top <= bottom && left <= right) {
            val ringViews = mutableListOf<View>()
            for (y in top..bottom) {
                val xs = if (y == top || y == bottom) (left..right) else listOf(left, right)
                for (x in xs) {
                    val child = container.let {
                        cellLayout.getChildAt(x, y)
                    } ?: continue
                    if (child.visibility == View.VISIBLE && seen.add(child)) {
                        ringViews.add(child)
                    }
                }
            }
            if (ringViews.isNotEmpty()) {
                rings.add(ringViews)
            }
            top++
            bottom--
            left++
            right--
        }

        // Fallback for any child outside cell bounds
        val leftovers = (0 until container.childCount)
            .mapNotNull { container.getChildAt(it) }
            .filter { it.visibility == View.VISIBLE && seen.add(it) }
        if (leftovers.isNotEmpty()) {
            rings.add(leftovers)
        }

        rings.forEachIndexed { ringIndex, ringViews ->
            val delayMs = min(ringIndex * 42L, 210L)
            for (child in ringViews) {
                trackChild(child)
                cancelSprings(child)
                val childCenterX = child.x + child.width / 2f
                val childCenterY = child.y + child.height / 2f
                val startTx = (gridCenterX - childCenterX) * 0.28f
                val startTy = (gridCenterY - childCenterY) * 0.28f

                child.scaleX = 0.68f
                child.scaleY = 0.68f
                child.translationX = startTx
                child.translationY = startTy
                child.alpha = 0f

                child.postDelayed(
                    {
                        if (generation != transitionGeneration || !child.isAttachedToWindow) return@postDelayed
                        animateSpring(child, DynamicAnimation.SCALE_X, 1.0f, STIFFNESS_UNLOCK, DAMPING_UNLOCK, child.scaleX)
                        animateSpring(child, DynamicAnimation.SCALE_Y, 1.0f, STIFFNESS_UNLOCK, DAMPING_UNLOCK, child.scaleY)
                        animateSpring(child, DynamicAnimation.TRANSLATION_X, 0f, STIFFNESS_UNLOCK, DAMPING_UNLOCK, child.translationX)
                        animateSpring(child, DynamicAnimation.TRANSLATION_Y, 0f, STIFFNESS_UNLOCK, DAMPING_UNLOCK, child.translationY)
                        animateSpring(child, DynamicAnimation.ALPHA, 1.0f, STIFFNESS_UNLOCK, DAMPING_SMOOTH, child.alpha)
                    },
                    delayMs,
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun playUnlockShaderShimmer(
        dragLayer: ViewGroup,
        context: Context,
        generation: Int,
    ) {
        if (!dragLayer.isAttachedToWindow || dragLayer.width <= 0 || dragLayer.height <= 0) return
        val accentColor = Themes.getColorAccent(context)
        val overlayView = UnlockShaderOverlayView(context, accentColor).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            isClickable = false
            isFocusable = false
        }
        dragLayer.addView(overlayView)
        overlayView.startEffect {
            if (overlayView.parent === dragLayer || generation != transitionGeneration) {
                dragLayer.removeView(overlayView)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private class UnlockShaderOverlayView(
        context: Context,
        accentColor: Int,
    ) : View(context) {

        private val shader = runCatching {
            RuntimeShader(UNLOCK_SHIMMER_AGSL)
        }.getOrNull()
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val r = ((accentColor shr 16) and 0xFF) / 255f
        private val g = ((accentColor shr 8) and 0xFF) / 255f
        private val b = (accentColor and 0xFF) / 255f
        private var startTimeMs = 0L
        private var animator: ValueAnimator? = null

        fun startEffect(onFinished: () -> Unit) {
            val runtimeShader = shader
            if (runtimeShader == null) {
                onFinished()
                return
            }
            startTimeMs = SystemClock.uptimeMillis()
            paint.shader = runtimeShader
            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 620L
                interpolator = zoomInterpolator
                addUpdateListener { anim ->
                    val progress = anim.animatedValue as Float
                    val elapsedSec = (SystemClock.uptimeMillis() - startTimeMs) / 1000f
                    runtimeShader.setFloatUniform("in_resolution", width.toFloat(), height.toFloat())
                    runtimeShader.setFloatUniform("in_progress", progress)
                    runtimeShader.setFloatUniform("in_time", elapsedSec)
                    runtimeShader.setFloatUniform("in_color", r, g, b)
                    postInvalidateOnAnimation()
                }
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        onFinished()
                    }

                    override fun onAnimationCancel(animation: android.animation.Animator) {
                        onFinished()
                    }
                })
                start()
            }
        }

        override fun onDraw(canvas: Canvas) {
            if (shader != null && width > 0 && height > 0) {
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }
        }

        override fun onDetachedFromWindow() {
            animator?.cancel()
            super.onDetachedFromWindow()
        }

        companion object {
            private const val UNLOCK_SHIMMER_AGSL = """
                uniform float2 in_resolution;
                uniform float in_progress;
                uniform float in_time;
                uniform float3 in_color;

                float hash(float2 p) {
                    p = fract(p * float2(123.34, 456.21));
                    p += dot(p, p + 45.32);
                    return fract(p.x * p.y);
                }

                float noise(float2 p) {
                    float2 i = floor(p);
                    float2 f = fract(p);
                    float2 u = f * f * (3.0 - 2.0 * f);
                    float a = hash(i);
                    float b = hash(i + float2(1.0, 0.0));
                    float c = hash(i + float2(0.0, 1.0));
                    float d = hash(i + float2(1.0, 1.0));
                    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
                }

                half4 main(float2 fragCoord) {
                    float2 uv = fragCoord / max(in_resolution, float2(1.0));
                    float2 centered = uv - float2(0.5);
                    float dist = length(centered);
                    float wavePos = in_progress * 0.95;
                    float ring = exp(-pow((dist - wavePos) * 5.2, 2.0));
                    float n = noise(uv * 6.0 + float2(in_time * 1.4, -in_time * 1.1));
                    float fade = sin(in_progress * 3.14159);
                    float intensity = ring * (0.55 + 0.45 * n) * fade * 0.22;
                    return half4(in_color * intensity, intensity);
                }
            """
        }
    }

    private fun spreadSurroundingItems(
        container: ViewGroup?,
        launchedView: View?,
        density: Float,
    ) {
        if (container == null || container.childCount == 0) return

        val origin = IntArray(2)
        val hasOrigin = launchedView != null && launchedView.isAttachedToWindow
        if (hasOrigin) {
            launchedView!!.getLocationInWindow(origin)
            origin[0] += launchedView.width / 2
            origin[1] += launchedView.height / 2
        } else {
            container.getLocationInWindow(origin)
            origin[0] += container.width / 2
            origin[1] += container.height / 2
        }

        val maxRadius = max(hypot(container.width.toFloat(), container.height.toFloat()), 1f)
        val childLoc = IntArray(2)

        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i) ?: continue
            if (child === launchedView || child.visibility != View.VISIBLE) continue

            trackChild(child)
            child.getLocationInWindow(childLoc)
            val cx = childLoc[0] + child.width / 2f
            val cy = childLoc[1] + child.height / 2f
            val dx = cx - origin[0]
            val dy = cy - origin[1]
            val dist = max(hypot(dx, dy), 1f)
            val proximity = 1f - min(dist / maxRadius, 1f)
            val maxShift = (if (child is BubbleTextView) 14f else 6f) * density
            val shift = maxShift * (0.35f + 0.65f * proximity)

            animateSpring(
                child,
                DynamicAnimation.TRANSLATION_X,
                (dx / dist) * shift,
                STIFFNESS_MID,
                DAMPING_SMOOTH,
            )
            animateSpring(
                child,
                DynamicAnimation.TRANSLATION_Y,
                (dy / dist) * shift,
                STIFFNESS_MID,
                DAMPING_SMOOTH,
            )
            animateSpring(child, DynamicAnimation.SCALE_X, 0.94f, STIFFNESS_MID, DAMPING_SMOOTH)
            animateSpring(child, DynamicAnimation.SCALE_Y, 0.94f, STIFFNESS_MID, DAMPING_SMOOTH)
            animateSpring(child, DynamicAnimation.ALPHA, 0.88f, STIFFNESS_MID, DAMPING_SMOOTH)
        }
    }

    private fun restoreTrackedChildren(excludeView: View?) {
        val iterator = trackedChildren.iterator()
        while (iterator.hasNext()) {
            val child = iterator.next().get()
            if (child == null) {
                iterator.remove()
                continue
            }
            if (child === excludeView) continue
            animateSpring(child, DynamicAnimation.TRANSLATION_X, 0f, STIFFNESS_BLOOM, DAMPING_BLOOM)
            animateSpring(child, DynamicAnimation.TRANSLATION_Y, 0f, STIFFNESS_BLOOM, DAMPING_BLOOM)
            animateSpring(child, DynamicAnimation.SCALE_X, 1.0f, STIFFNESS_BLOOM, DAMPING_BLOOM)
            animateSpring(child, DynamicAnimation.SCALE_Y, 1.0f, STIFFNESS_BLOOM, DAMPING_BLOOM)
            animateSpring(child, DynamicAnimation.ALPHA, 1.0f, STIFFNESS_BLOOM, DAMPING_SMOOTH)
        }
    }

    private fun settleActivePageChildren(container: ViewGroup?, excludeView: View?) {
        if (container == null) return
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i) ?: continue
            if (child === excludeView || child.visibility != View.VISIBLE) continue
            trackChild(child)
            val startScale = if (child.scaleX < 0.97f) child.scaleX else 0.96f
            animateSpring(
                child,
                DynamicAnimation.SCALE_X,
                1.0f,
                STIFFNESS_BLOOM,
                DAMPING_BLOOM,
                startValue = startScale,
            )
            animateSpring(
                child,
                DynamicAnimation.SCALE_Y,
                1.0f,
                STIFFNESS_BLOOM,
                DAMPING_BLOOM,
                startValue = startScale,
            )
            animateSpring(child, DynamicAnimation.TRANSLATION_X, 0f, STIFFNESS_BLOOM, DAMPING_BLOOM)
            animateSpring(child, DynamicAnimation.TRANSLATION_Y, 0f, STIFFNESS_BLOOM, DAMPING_BLOOM)
            animateSpring(child, DynamicAnimation.ALPHA, 1.0f, STIFFNESS_BLOOM, DAMPING_SMOOTH)
        }
    }

    private fun staggerUnlockPageChildren(
        container: ViewGroup?,
        density: Float,
        generation: Int,
    ) {
        if (container == null || container.childCount == 0) return

        val centerX = container.width / 2f
        val centerY = container.height / 2f
        val maxDist = max(hypot(centerX, centerY), 1f)

        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i) ?: continue
            if (child.visibility != View.VISIBLE) continue
            trackChild(child)

            val cx = child.x + child.width / 2f
            val cy = child.y + child.height / 2f
            val distRatio = min(hypot(cx - centerX, cy - centerY) / maxDist, 1f)
            val startScale = 0.85f
            val startY = (24f + distRatio * 20f) * density
            val delayMs = min((distRatio * 110f).toLong(), 140L)

            cancelSprings(child)
            child.scaleX = startScale
            child.scaleY = startScale
            child.translationX = 0f
            child.translationY = startY
            child.alpha = 0.14f

            child.postDelayed(
                {
                    if (generation != transitionGeneration || !child.isAttachedToWindow) return@postDelayed
                    animateSpring(
                        child,
                        DynamicAnimation.SCALE_X,
                        1.0f,
                        STIFFNESS_UNLOCK,
                        DAMPING_UNLOCK,
                        startValue = child.scaleX,
                    )
                    animateSpring(
                        child,
                        DynamicAnimation.SCALE_Y,
                        1.0f,
                        STIFFNESS_UNLOCK,
                        DAMPING_UNLOCK,
                        startValue = child.scaleY,
                    )
                    animateSpring(
                        child,
                        DynamicAnimation.TRANSLATION_Y,
                        0f,
                        STIFFNESS_UNLOCK,
                        DAMPING_UNLOCK,
                        startValue = child.translationY,
                    )
                    animateSpring(
                        child,
                        DynamicAnimation.ALPHA,
                        1.0f,
                        STIFFNESS_UNLOCK,
                        DAMPING_SMOOTH,
                        startValue = child.alpha,
                    )
                },
                delayMs,
            )
        }
    }

    private fun animateWallpaperZoom(
        context: Context,
        windowToken: IBinder?,
        fromZoom: Float?,
        targetZoom: Float,
        durationMs: Long,
        generation: Int,
    ) {
        wallpaperZoomAnimator?.cancel()
        wallpaperZoomAnimator = null

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || windowToken == null) {
            currentWallpaperZoom = targetZoom
            return
        }

        val start = fromZoom ?: currentWallpaperZoom
        currentWallpaperZoom = start
        setWallpaperZoomSafe(context, windowToken, start)

        wallpaperZoomAnimator = ValueAnimator.ofFloat(start, targetZoom).apply {
            duration = durationMs
            interpolator = zoomInterpolator
            addUpdateListener { anim ->
                if (generation != transitionGeneration) {
                    anim.cancel()
                    return@addUpdateListener
                }
                val value = anim.animatedValue as Float
                currentWallpaperZoom = value
                setWallpaperZoomSafe(context, windowToken, value)
            }
            start()
        }
    }

    private fun setWallpaperZoomSafe(
        context: Context,
        windowToken: IBinder?,
        zoom: Float,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || windowToken == null) return
        runCatching {
            WallpaperManager.getInstance(context).setWallpaperZoomOut(
                windowToken,
                zoom.coerceIn(0f, 1f),
            )
        }
    }

    private fun animateWorkspaceBlur(
        workspace: Workspace<*>,
        targetRadius: Float,
        durationMs: Long,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        if (!appliedWorkspaceBlur && targetRadius <= 0f) return

        workspaceBlurAnimator?.cancel()
        val start = currentWorkspaceBlur
        if (start <= 0.01f && targetRadius <= 0.01f) {
            clearWorkspaceBlurImmediately(workspace)
            return
        }

        appliedWorkspaceBlur = true
        workspaceBlurAnimator = ValueAnimator.ofFloat(start, targetRadius).apply {
            duration = durationMs
            interpolator = zoomInterpolator
            addUpdateListener { anim ->
                val radius = anim.animatedValue as Float
                currentWorkspaceBlur = radius
                if (radius <= 0.35f) {
                    workspace.setRenderEffect(null)
                    if (targetRadius == 0f) {
                        appliedWorkspaceBlur = false
                    }
                } else {
                    workspace.setRenderEffect(
                        RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP),
                    )
                }
            }
            start()
        }
    }

    private fun clearWorkspaceBlurImmediately(workspace: Workspace<*>) {
        workspaceBlurAnimator?.cancel()
        workspaceBlurAnimator = null
        currentWorkspaceBlur = 0f
        if (appliedWorkspaceBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            workspace.setRenderEffect(null)
        }
        appliedWorkspaceBlur = false
    }

    private fun animateSpring(
        view: View,
        property: DynamicAnimation.ViewProperty,
        finalPosition: Float,
        stiffness: Float,
        dampingRatio: Float,
        startValue: Float? = null,
    ) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            view.post {
                animateSpring(view, property, finalPosition, stiffness, dampingRatio, startValue)
            }
            return
        }

        val viewMap = activeSprings.getOrPut(view) { mutableMapOf() }
        viewMap.remove(property)?.cancel()

        if (startValue != null) {
            applyPropertyValue(view, property, startValue)
        }

        val spring = SpringAnimation(view, property, finalPosition).apply {
            spring = SpringForce(finalPosition).apply {
                this.stiffness = stiffness
                this.dampingRatio = dampingRatio
            }
            if (startValue != null) {
                setStartValue(startValue)
            }
            addEndListener { _, _, _, _ ->
                activeSprings[view]?.remove(property)
            }
        }
        viewMap[property] = spring
        spring.start()
    }

    private fun cancelSprings(view: View) {
        if (Looper.myLooper() != Looper.getMainLooper()) return
        activeSprings.remove(view)?.values?.forEach { it.cancel() }
    }

    private fun resetViewImmediately(view: View) {
        cancelSprings(view)
        view.scaleX = 1.0f
        view.scaleY = 1.0f
        view.translationX = 0f
        view.translationY = 0f
        view.alpha = 1.0f
    }

    private fun resetContainerChildrenImmediately(container: ViewGroup?) {
        if (container == null) return
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i) ?: continue
            resetViewImmediately(child)
        }
    }

    private fun applyPropertyValue(
        view: View,
        property: DynamicAnimation.ViewProperty,
        value: Float,
    ) {
        when (property) {
            DynamicAnimation.SCALE_X -> view.scaleX = value
            DynamicAnimation.SCALE_Y -> view.scaleY = value
            DynamicAnimation.TRANSLATION_X -> view.translationX = value
            DynamicAnimation.TRANSLATION_Y -> view.translationY = value
            DynamicAnimation.ALPHA -> view.alpha = value
        }
    }

    private fun trackChild(view: View) {
        if (trackedChildren.any { it.get() === view }) return
        trackedChildren.add(WeakReference(view))
    }
}
