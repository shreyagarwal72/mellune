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

package app.mellune

import android.animation.AnimatorSet
import android.app.ActivityOptions
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.SystemClock
import android.util.Pair
import android.view.Display
import android.view.View
import android.view.ViewTreeObserver
import android.window.SplashScreen
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import app.mellune.MelluneApp.Companion.showQuickstepWarningIfNecessary
import app.mellune.compat.MelluneQuickstepCompat
import app.mellune.data.AppDatabase
import app.mellune.data.wallpaper.service.WallpaperService
import app.mellune.gestures.GestureController
import app.mellune.gestures.PinchGestureTouchController
import app.mellune.gestures.VerticalSwipeTouchController
import app.mellune.gestures.config.GestureHandlerConfig
import app.mellune.gestures.ui.MelluneShortcutActivity
import app.mellune.nexuslauncher.OverlayCallbackImpl
import app.mellune.preferences.PreferenceManager
import app.mellune.preferences2.PreferenceManager2
import app.mellune.preferences2.firstCached
import app.mellune.root.RootHelperManager
import app.mellune.root.RootNotAvailableException
import app.mellune.theme.ThemeProvider
import app.mellune.ui.popup.LauncherOptionsPopup
import app.mellune.ui.popup.MelluneShortcut
import app.mellune.util.MelluneSpringEngine
import app.mellune.util.getThemedIconPacksInstalled
import app.mellune.util.unsafeLazy
import app.mellune.views.MelluneFloatingSurfaceView
import com.android.launcher3.AbstractFloatingView
import com.android.launcher3.BaseActivity
import com.android.launcher3.BubbleTextView
import com.android.launcher3.GestureNavContract
import com.android.launcher3.LauncherAppState
import com.android.launcher3.LauncherSettings.Favorites.CONTAINER_ALL_APPS_PREDICTION
import com.android.launcher3.LauncherSettings.Favorites.CONTAINER_HOTSEAT_PREDICTION
import com.android.launcher3.LauncherSettings.Favorites.CONTAINER_WIDGETS_PREDICTION
import com.android.launcher3.LauncherState
import com.android.launcher3.R
import com.android.launcher3.Utilities
import com.android.launcher3.folder.FolderIcon
import com.android.launcher3.model.data.ItemInfo
import com.android.launcher3.model.data.PredictedContainerInfo
import com.android.launcher3.popup.SystemShortcut
import com.android.launcher3.shortcuts.DeepShortcutView
import com.android.launcher3.statemanager.StateManager
import com.android.launcher3.statemanager.StateManager.StateHandler
import com.android.launcher3.uioverrides.QuickstepLauncher
import com.android.launcher3.uioverrides.states.AllAppsState
import com.android.launcher3.uioverrides.states.BackgroundAppState
import com.android.launcher3.uioverrides.states.OverviewState
import com.android.launcher3.util.ActivityOptionsWrapper
import com.android.launcher3.util.Executors
import com.android.launcher3.util.RunnableList
import com.android.launcher3.util.SystemUiController.UI_STATE_BASE_WINDOW
import com.android.launcher3.util.Themes
import com.android.launcher3.util.TouchController
import com.android.launcher3.views.ActivityContext
import com.android.launcher3.views.OptionsPopupView
import com.android.launcher3.views.OptionsPopupView.OptionItem
import com.android.launcher3.widget.LauncherWidgetHolder
import com.android.launcher3.widget.RoundedCornerEnforcement
import com.android.systemui.plugins.shared.LauncherOverlayManager
import com.android.systemui.shared.system.QuickStepContract
import com.kieronquinn.app.smartspacer.sdk.client.SmartspacerClient
import com.patrykmichalik.opto.core.onEach
import dev.kdrag0n.monet.theme.ColorScheme
import java.lang.ref.WeakReference
import java.util.stream.Stream
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class MelluneLauncher : QuickstepLauncher() {

    private var pendingUnlockParallax = false
    private var pendingAppReturnParallax = false
    private var unlockSignalUptimeMs = 0L
    private var lastLaunchedView: WeakReference<View>? = null

    private val keyguardManager by unsafeLazy { getSystemService(KeyguardManager::class.java) }

    private val screenLockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    if (!pendingAppReturnParallax) {
                        pendingUnlockParallax = true
                        unlockSignalUptimeMs = 0L
                    }
                    MelluneSpringEngine.resetRestState(
                        workspace = workspace,
                        hotseat = hotseat,
                        windowToken = dragLayer?.windowToken,
                        context = this@MelluneLauncher,
                    )
                }

                Intent.ACTION_USER_PRESENT -> {
                    if (!pendingAppReturnParallax) {
                        unlockSignalUptimeMs = SystemClock.uptimeMillis()
                        pendingUnlockParallax = true
                        if (hasWindowFocus() &&
                            lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
                            stateManager.state == LauncherState.NORMAL
                        ) {
                            consumePendingUnlockParallax()
                        }
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus || stateManager.state != LauncherState.NORMAL) return
        consumePendingUnlockParallax()
    }

    private fun consumePendingUnlockParallax(): Boolean {
        if (!pendingUnlockParallax) return false
        if (keyguardManager?.isKeyguardLocked == true) return false
        val withinUnlockWindow = unlockSignalUptimeMs == 0L ||
            (SystemClock.uptimeMillis() - unlockSignalUptimeMs) <= UNLOCK_PARALLAX_WINDOW_MS
        pendingUnlockParallax = false
        unlockSignalUptimeMs = 0L
        if (!withinUnlockWindow) return false

        pendingAppReturnParallax = false
        lastLaunchedView = null
        MelluneSpringEngine.playUnlockParallax(
            workspace = workspace,
            hotseat = hotseat,
            windowToken = dragLayer?.windowToken,
            context = this,
            dragLayer = dragLayer,
            useRingAnimation = preferenceManager2.enableUnlockRingAnimation.firstCached(),
            useShaderShimmer = preferenceManager2.enableUnlockShaderShimmer.firstCached(),
        )
        return true
    }
    private val defaultOverlay by unsafeLazy { OverlayCallbackImpl(this) }
    private val prefs by unsafeLazy { PreferenceManager.getInstance(this) }
    private val preferenceManager2 by unsafeLazy { PreferenceManager2.getInstance(this) }
    private val insetsController: WindowInsetsControllerCompat by lazy {
        val window = launcher.window
            ?: throw Exception("WindowInsetsControllerCompat not available.")
        WindowInsetsControllerCompat(window, rootView)
    }
    private val themeProvider by unsafeLazy { ThemeProvider.INSTANCE.get(this) }
    private val noStatusBarStateListener = object : StateManager.StateListener<LauncherState> {
        override fun onStateTransitionStart(toState: LauncherState) {
            if (toState is OverviewState) {
                insetsController.show(WindowInsetsCompat.Type.statusBars())
            }
        }
        override fun onStateTransitionComplete(finalState: LauncherState) {
            if (finalState !is OverviewState) {
                insetsController.hide(WindowInsetsCompat.Type.statusBars())
            }
        }
    }
    private val rememberPositionStateListener = object : StateManager.StateListener<LauncherState> {
        override fun onStateTransitionStart(toState: LauncherState) {
            if (toState is AllAppsState) {
                mAppsView.activeRecyclerView.restoreScrollPosition()
            }
        }
        override fun onStateTransitionComplete(finalState: LauncherState) {}
    }
    private val statusBarClockListener = object : StateManager.StateListener<LauncherState> {
        override fun onStateTransitionStart(toState: LauncherState) {
            when (toState) {
                is BackgroundAppState,
                is OverviewState,
                is AllAppsState,
                -> {
                    MelluneApp.instance.restoreClockInStatusBar()
                }

                else -> {
                    workspace.updateStatusbarClock()
                }
            }
        }
        override fun onStateTransitionComplete(finalState: LauncherState) {}
    }
    private val clearSearchStateListener = object : StateManager.StateListener<LauncherState> {
        override fun onStateTransitionComplete(finalState: LauncherState) {
            if (finalState == LauncherState.NORMAL && mAppsView != null && mAppsView.isSearching) {
                mAppsView?.post {
                    mAppsView.reset(false, true)
                }
            }
        }
    }

    private lateinit var colorScheme: ColorScheme
    private var hasBackGesture = false

    val gestureController by unsafeLazy { GestureController(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        layoutInflater.factory2 = MelluneLayoutFactory(this)
        super.onCreate(savedInstanceState)

        prefs.launcherTheme.subscribeChanges(this, ::updateTheme)
        prefs.feedProvider.subscribeChanges(this, defaultOverlay::reconnect)
        preferenceManager2.enableFeed.get().distinctUntilChanged().onEach { enable ->
            defaultOverlay.setEnableFeed(enable)
        }.launchIn(scope = lifecycleScope)
        launcher.stateManager.addStateListener(clearSearchStateListener)

        if (prefs.autoLaunchRoot.get()) {
            lifecycleScope.launch {
                try {
                    RootHelperManager.INSTANCE.get(this@MelluneLauncher)
                } catch (_: RootNotAvailableException) {
                }
            }
        }

        preferenceManager2.showStatusBar.get().distinctUntilChanged().onEach {
            with(insetsController) {
                if (it) {
                    show(WindowInsetsCompat.Type.statusBars())
                } else {
                    hide(WindowInsetsCompat.Type.statusBars())
                }
            }
            with(launcher.stateManager) {
                if (it) {
                    removeStateListener(noStatusBarStateListener)
                } else {
                    addStateListener(noStatusBarStateListener)
                }
            }
        }.launchIn(scope = lifecycleScope)

        preferenceManager2.statusBarClock.get().distinctUntilChanged().onEach {
            with(launcher.stateManager) {
                if (it) {
                    addStateListener(statusBarClockListener)
                } else {
                    removeStateListener(statusBarClockListener)
                    // Make sure status bar clock is restored when the preference is toggled off
                    MelluneApp.instance.restoreClockInStatusBar()
                }
            }
        }.launchIn(scope = lifecycleScope)
        preferenceManager2.rememberPosition.get().onEach {
            with(launcher.stateManager) {
                if (it) {
                    addStateListener(rememberPositionStateListener)
                } else {
                    removeStateListener(rememberPositionStateListener)
                }
            }
        }.launchIn(scope = lifecycleScope)

        prefs.overrideWindowCornerRadius.subscribeValues(this) {
            QuickStepContract.sHasCustomCornerRadius = it
        }
        prefs.windowCornerRadius.subscribeValues(this) {
            QuickStepContract.sCustomCornerRadius = it.toFloat()
        }
        preferenceManager2.roundedWidgets.onEach(launchIn = lifecycleScope) {
            RoundedCornerEnforcement.sRoundedCornerEnabled = it
        }
        val isWorkspaceDarkText = Themes.getAttrBoolean(this, R.attr.isWorkspaceDarkText)
        preferenceManager2.darkStatusBar.onEach(launchIn = lifecycleScope) { darkStatusBar ->
            systemUiController?.updateUiState(UI_STATE_BASE_WINDOW, isWorkspaceDarkText || darkStatusBar)
        }
        preferenceManager2.backPressGestureHandler.onEach(launchIn = lifecycleScope) { handler ->
            hasBackGesture = handler !is GestureHandlerConfig.NoOp
        }

        LauncherOptionsPopup.restoreMissingPopupOptions(launcher)
        LauncherOptionsPopup.migrateLegacyPreferences(launcher)

        // Handle update from version 12 Alpha 4 to version 12 Alpha 5.
        if (
            prefs.themedIcons.get() &&
            packageManager.getThemedIconPacksInstalled(this).isEmpty()
        ) {
            prefs.themedIcons.set(newValue = false)
        }

        colorScheme = themeProvider.colorScheme

        showQuickstepWarningIfNecessary()

        reloadIconsIfNeeded()

        ContextCompat.registerReceiver(
            this,
            screenLockReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        AppDatabase.INSTANCE.get(this).checkpointSync()
    }

    override fun onNewIntent(intent: Intent?) {
        if (intent != null && intent.action == MelluneShortcutActivity.START_ACTION) {
            val handlerString = intent.getStringExtra(MelluneShortcutActivity.EXTRA_HANDLER)
            val config = handlerString?.let { GestureHandlerConfig.fromString(it) }
            if (config != null && config.isExternallyInvokable()) {
                gestureController.handle(config)
            }
        }

        super.onNewIntent(intent)
    }

    override fun collectStateHandlers(out: MutableList<StateHandler<LauncherState>>) {
        super.collectStateHandlers(out)
        out.add(SearchBarStateHandler(this))
    }

    override fun getAllAppsItemLongClickListener(): View.OnLongClickListener {
        return View.OnLongClickListener { view ->
            if (view is FolderIcon && view.mInfo.id != ItemInfo.NO_ID) {
                MelluneShortcut.showAppDrawerFolderPopup(this, view)
            } else {
                super.getAllAppsItemLongClickListener().onLongClick(view)
            }
        }
    }

    override fun getSupportedShortcuts(container: Int): Stream<SystemShortcut.Factory<*>> = Stream.concat(
        super.getSupportedShortcuts(container),
        Stream.concat(
            Stream.of(MelluneShortcut.UNINSTALL, MelluneShortcut.CUSTOMIZE, MelluneShortcut.OPEN_IN_STORE),
            Stream.of(MelluneShortcut.PAUSE_APPS),
        ),
    )

    fun updateTheme() {
        if (themeProvider.colorScheme != colorScheme) {
            recreate()
        } else {
            mWallpaperThemeManager.updateTheme()
        }
    }

    override fun onStateBack() {
        val searchInput = mAppsView?.searchUiManager?.editText
        val isSearching = mAppsView?.isSearching == true || searchInput?.hasFocus() == true
        if (isSearching) {
            mAppsView?.searchUiManager?.resetSearch()
            allAppsController.animateAllAppsToNoScale()
        } else {
            super.onStateBack()
        }
    }

    override fun createTouchControllers(): Array<TouchController> {
        val verticalSwipeController = VerticalSwipeTouchController(this, gestureController)
        val pinchGestureController = PinchGestureTouchController(this, gestureController)
        return arrayOf<TouchController>(verticalSwipeController, pinchGestureController) +
            super.createTouchControllers()
    }

    override fun handleHomeTap() {
        gestureController.onHomePressed()
    }

    fun bindItems(items: List<ItemInfo>, forceAnimateIcons: Boolean) {
        // pE-TODO(QPR1): Note: null is modelWriter + bindItems override something
        val inflatedItems = items.map { i ->
            Pair.create(
                i,
                itemInflater?.inflateItem(
                    i,
                    null,
                ),
            )
        }.toList()
        bindInflatedItems(inflatedItems, if (forceAnimateIcons) AnimatorSet() else null)
    }

    override fun handleGestureContract(intent: Intent) {
        if (!MelluneApp.isRecentsEnabled && prefs.enableGnc.get()) {
            val gnc = GestureNavContract.fromIntent(intent)
            if (gnc != null) {
                AbstractFloatingView.closeOpenViews(
                    this,
                    false,
                    AbstractFloatingView.TYPE_ICON_SURFACE,
                )
                MelluneFloatingSurfaceView.show(this, gnc)
            }
        }
    }

    override fun onUiChangedWhileSleeping() {
        if (Utilities.ATLEAST_S) {
            super.onUiChangedWhileSleeping()
        }
    }

    override fun showDefaultOptions(x: Float, y: Float) {
        val showWallpaperCarousel = "+carousel" in preferenceManager2.launcherPopupOrder.firstCached()

        if (showWallpaperCarousel) {
            show<MelluneLauncher>(
                this,
                getPopupTarget(x, y),
                OptionsPopupView.getOptions(this),
            )
        } else {
            super.showDefaultOptions(x, y)
        }
    }

    private fun <T> show(
        activityContext: ActivityContext?,
        targetRect: RectF,
        items: List<OptionItem>,
        shouldAddArrow: Boolean = false,
        width: Int = 0,
    ): OptionsPopupView<T>? where T : Context?, T : ActivityContext? {
        if (activityContext == null) return null

        val isEmpty = WallpaperService.INSTANCE.get(this).getTopWallpapers().isEmpty()
        val layout = if (isEmpty) R.layout.longpress_options_menu else R.layout.wallpaper_options_popup

        val popup = activityContext.layoutInflater.inflate(layout, activityContext.dragLayer, false) as OptionsPopupView<T>
        popup.setTargetRect(targetRect)
        popup.setShouldAddArrow(shouldAddArrow)

        for (item in items) {
            val deepLayout = if (isEmpty) R.layout.system_shortcut else R.layout.wallpaper_options_popup_item

            val view = popup.inflateAndAdd<DeepShortcutView>(deepLayout, popup)
            if (width > 0) view.layoutParams.width = width
            view.iconView.setBackgroundDrawable(item.icon)
            view.bubbleText.text = item.label
            view.setOnClickListener(popup)
            view.setOnLongClickListener(popup)
            popup.mItemMap[view] = item
        }

        popup.show()
        return popup
    }

    fun createAppWidgetHolder(): LauncherWidgetHolder {
        val holder = LauncherWidgetHolder.newInstance(this)
        holder.setAppWidgetRemovedCallback { appWidgetId ->
            workspace.removeWidget(appWidgetId)
        }
        return holder
    }

    override fun makeDefaultActivityOptions(splashScreenStyle: Int): ActivityOptionsWrapper {
        val callbacks = RunnableList()
        val options = if (Utilities.ATLEAST_Q) {
            MelluneQuickstepCompat.activityOptionsCompat.makeCustomAnimation(
                this,
                0,
                0,
                Executors.MAIN_EXECUTOR.handler,
                null,
            ) {
                callbacks.executeAllAndDestroy()
            }
        } else {
            ActivityOptions.makeBasic()
        }
        if (Utilities.ATLEAST_T) {
            options.splashScreenStyle = splashScreenStyle
        }

        Utilities.allowBGLaunch(options)
        return ActivityOptionsWrapper(options, callbacks)
    }

    override fun getActivityLaunchOptions(v: View?, item: ItemInfo?): ActivityOptionsWrapper {
        val inNormalState = stateManager.state == LauncherState.NORMAL
        pendingUnlockParallax = false
        pendingAppReturnParallax = true
        lastLaunchedView = if (inNormalState && v != null) WeakReference(v) else null

        if (inNormalState) {
            MelluneSpringEngine.playAppOpenParallax(
                workspace = workspace,
                hotseat = hotseat,
                launchedView = v,
                windowToken = dragLayer?.windowToken,
                context = this,
            )
        } else if (v != null) {
            MelluneSpringEngine.playLaunchSink(v)
        }

        val wrapper = runCatching {
            super.getActivityLaunchOptions(v, item)
        }.getOrElse {
            getActivityLaunchOptionsDefault(v)
        }
        if (v is BubbleTextView) {
            wrapper.onEndCallback.add(
                Runnable {
                    if (hasWindowFocus() &&
                        lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                    ) {
                        pendingAppReturnParallax = false
                        MelluneSpringEngine.playReturnRebound(v)
                    }
                },
            )
        }
        return wrapper
    }

    private fun getActivityLaunchOptionsDefault(v: View?): ActivityOptionsWrapper {
        var left = 0
        var top = 0
        var width = v!!.measuredWidth
        var height = v.measuredHeight
        if (v is BubbleTextView) {
            // Launch from center of icon, not entire view
            val icon: Drawable? = v.icon
            if (icon != null) {
                val bounds = icon.bounds
                left = (width - bounds.width()) / 2
                top = v.paddingTop
                width = bounds.width()
                height = bounds.height()
            }
        }
        val options = Utilities.allowBGLaunch(
            ActivityOptions.makeClipRevealAnimation(
                v,
                left,
                top,
                width,
                height,
            ),
        )
        if (Utilities.ATLEAST_T) {
            options.splashScreenStyle = SplashScreen.SPLASH_SCREEN_STYLE_ICON
        }
        options.launchDisplayId = if (v.display != null) v.display.displayId else Display.DEFAULT_DISPLAY
        val callback = RunnableList()
        return ActivityOptionsWrapper(options, callback)
    }

    override fun onResume() {
        super.onResume()
        val isNormalState = stateManager.state == LauncherState.NORMAL
        if (isNormalState && consumePendingUnlockParallax()) {
            // Unlock parallax handles wallpaper zoom, staggered icons, and hotseat bloom.
        } else if (isNormalState && pendingAppReturnParallax) {
            pendingAppReturnParallax = false
            val launched = lastLaunchedView?.get()
            lastLaunchedView = null
            MelluneSpringEngine.playAppCloseParallax(
                workspace = workspace,
                hotseat = hotseat,
                lastLaunchedView = launched,
                windowToken = dragLayer?.windowToken,
                context = this,
            )
        } else {
            pendingAppReturnParallax = false
            lastLaunchedView = null
            MelluneSpringEngine.resetRestState(
                workspace = workspace,
                hotseat = hotseat,
                windowToken = dragLayer?.windowToken,
                context = this,
            )
        }
        restartIfPending()
        refreshPredictionContainersFromModel()

        dragLayer.viewTreeObserver.addOnDrawListener(
            object : ViewTreeObserver.OnDrawListener {
                private var handled = false

                override fun onDraw() {
                    if (handled) {
                        return
                    }
                    handled = true

                    dragLayer.post {
                        dragLayer.viewTreeObserver.removeOnDrawListener(this)
                        // Drop stuck All Apps RenderEffect on icons after returning home.
                        depthController.clearStuckBlurOnResumeIfHome()
                    }
                }
            },
        )
    }

    override fun onStateSetEnd(state: LauncherState) {
        super.onStateSetEnd(state)
        refreshPredictionContainersFromModel()
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenLockReceiver) }
        super.onDestroy()
        // Only actually closes if required, safe to call if not enabled
        SmartspacerClient.close()
    }

    override fun getDefaultOverlay(): LauncherOverlayManager = defaultOverlay

    fun recreateIfNotScheduled() {
        if (sRestartFlags == 0) {
            recreate()
        }
    }

    private fun restartIfPending() {
        when {
            sRestartFlags and FLAG_RESTART != 0 -> melluneApp.restart(false)

            sRestartFlags and FLAG_RECREATE != 0 -> {
                sRestartFlags = 0
                recreate()
            }
        }
    }

    private fun refreshPredictionContainersFromModel() {
        LauncherAppState.getInstance(this).model.loadAsync { dataModel ->
            if (dataModel == null || isDestroyed) return@loadAsync

            val predictedContainers = synchronized(dataModel) {
                listOf(
                    dataModel.itemsIdMap[CONTAINER_ALL_APPS_PREDICTION] as? PredictedContainerInfo,
                    dataModel.itemsIdMap[CONTAINER_HOTSEAT_PREDICTION] as? PredictedContainerInfo,
                    dataModel.itemsIdMap[CONTAINER_WIDGETS_PREDICTION] as? PredictedContainerInfo,
                ).filterNotNull()
            }

            Executors.MAIN_EXECUTOR.execute {
                if (isDestroyed) return@execute
                predictedContainers.forEach(::bindPredictedContainerInfo)
            }
        }
    }

    private val tmpQsbLoc = IntArray(2)
    private val tmpSearchLoc = IntArray(2)

    override fun onAllAppsTransition(progress: Float) {
        super.onAllAppsTransition(progress)
        val homeQsb = hotseat?.qsb
        val drawerSearch = appsView?.searchView
        if (progress <= 0.001f || progress >= 0.999f) {
            homeQsb?.apply {
                translationY = 0f
                scaleX = 1f
                scaleY = 1f
                alpha = 1f
            }
            drawerSearch?.apply {
                translationX = 0f
                translationY = 0f
                scaleX = 1f
                scaleY = 1f
                alpha = 1f
            }
            return
        }

        val clamped = progress.coerceIn(0f, 1f)
        val eased = com.android.app.animation.Interpolators.EMPHASIZED.getInterpolation(clamped)
        val apps = appsView

        if (homeQsb != null && drawerSearch != null &&
            homeQsb.width > 0 && drawerSearch.width > 0 && apps != null
        ) {
            homeQsb.getLocationInWindow(tmpQsbLoc)
            drawerSearch.getLocationInWindow(tmpSearchLoc)

            val qsbCenterX = tmpQsbLoc[0] + homeQsb.width / 2f
            val qsbCenterY = tmpQsbLoc[1] + homeQsb.height / 2f

            // Undo current translations on drawerSearch & appsView to get resting screen center.
            val searchRestCenterX =
                tmpSearchLoc[0] - drawerSearch.translationX + drawerSearch.width / 2f
            val searchRestCenterY =
                tmpSearchLoc[1] - drawerSearch.translationY - apps.translationY +
                    drawerSearch.height / 2f

            val targetCenterX = qsbCenterX + (searchRestCenterX - qsbCenterX) * eased
            val targetCenterY = qsbCenterY + (searchRestCenterY - qsbCenterY) * eased

            val scaleXRatio =
                (homeQsb.width.toFloat() / drawerSearch.width.toFloat()).coerceIn(0.8f, 1.2f)
            val scaleYRatio =
                (homeQsb.height.toFloat() / drawerSearch.height.toFloat()).coerceIn(0.8f, 1.2f)

            // Cross-fade smoothly over the first 30% so homeQsb hands off directly into drawerSearch.
            val qsbFade = (1f - (clamped / 0.28f)).coerceIn(0f, 1f)
            val searchFade = (clamped / 0.28f).coerceIn(0f, 1f)

            homeQsb.alpha = qsbFade
            homeQsb.translationX = targetCenterX - qsbCenterX
            homeQsb.translationY = targetCenterY - qsbCenterY
            val qsbScaleX = 1f + (1f / scaleXRatio - 1f) * eased
            val qsbScaleY = 1f + (1f / scaleYRatio - 1f) * eased
            homeQsb.scaleX = qsbScaleX
            homeQsb.scaleY = qsbScaleY

            drawerSearch.alpha = searchFade
            drawerSearch.translationX = targetCenterX - searchRestCenterX
            drawerSearch.translationY = targetCenterY - (searchRestCenterY + apps.translationY)
            drawerSearch.scaleX = scaleXRatio + (1f - scaleXRatio) * eased
            drawerSearch.scaleY = scaleYRatio + (1f - scaleYRatio) * eased
        } else if (drawerSearch != null) {
            val density = resources.displayMetrics.density
            drawerSearch.alpha = (clamped / 0.35f).coerceIn(0f, 1f)
            drawerSearch.scaleX = 0.94f + 0.06f * eased
            drawerSearch.scaleY = 0.94f + 0.06f * eased
            drawerSearch.translationY = 18f * density * (1f - eased)
        }
    }

    /**
     * Reloads app icons if there is an active icon pack & [PreferenceManager2.alwaysReloadIcons] is enabled.
     */
    private fun reloadIconsIfNeeded() {
        if (
            preferenceManager2.alwaysReloadIcons.firstCached()
        ) {
            LauncherAppState.getInstance(this).model.reloadIfActive()
        }
    }

    companion object {
        private const val FLAG_RECREATE = 1 shl 0
        private const val FLAG_RESTART = 1 shl 1
        private const val UNLOCK_PARALLAX_WINDOW_MS = 1500L

        var sRestartFlags = 0

        val instance get() = MelluneApp.launcher
    }
}

val Context.launcher: MelluneLauncher
    get() = BaseActivity.fromContext(this)

val Context.launcherNullable: MelluneLauncher? get() = try {
    launcher
} catch (_: IllegalArgumentException) {
    null
}
