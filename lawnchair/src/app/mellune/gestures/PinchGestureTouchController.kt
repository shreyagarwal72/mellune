package app.mellune.gestures

import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.lifecycle.lifecycleScope
import app.mellune.MelluneLauncher
import app.mellune.gestures.config.GestureHandlerConfig
import app.mellune.preferences2.PreferenceManager2
import com.android.launcher3.AbstractFloatingView
import com.android.launcher3.LauncherState
import com.android.launcher3.util.TouchController
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Detects pinch-in and pinch-out gestures on the workspace (Nova-style desktop gestures).
 */
class PinchGestureTouchController(
    private val launcher: MelluneLauncher,
    private val gestureController: GestureController,
) : TouchController {

    private val prefs = PreferenceManager2.getInstance(launcher)
    private var enablePinchIn = false
    private var enablePinchOut = false
    private var triggeredThisGesture = false

    private val scaleDetector = ScaleGestureDetector(
        launcher,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                triggeredThisGesture = false
                return enablePinchIn || enablePinchOut
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (triggeredThisGesture) return true
                val factor = detector.scaleFactor
                if (enablePinchIn && factor < 0.85f) {
                    triggeredThisGesture = true
                    gestureController.onPinchIn()
                    return true
                }
                if (enablePinchOut && factor > 1.18f) {
                    triggeredThisGesture = true
                    gestureController.onPinchOut()
                    return true
                }
                return false
            }
        },
    )

    init {
        launcher.lifecycleScope.launch {
            prefs.pinchInGestureHandler.get()
                .onEach { enablePinchIn = it !is GestureHandlerConfig.NoOp }
                .launchIn(this)
            prefs.pinchOutGestureHandler.get()
                .onEach { enablePinchOut = it !is GestureHandlerConfig.NoOp }
                .launchIn(this)
        }
    }

    override fun onControllerInterceptTouchEvent(ev: MotionEvent): Boolean {
        if ((!enablePinchIn && !enablePinchOut) ||
            !launcher.isInState(LauncherState.NORMAL) ||
            AbstractFloatingView.getTopOpenView(launcher) != null
        ) {
            return false
        }
        if (ev.pointerCount >= 2) {
            scaleDetector.onTouchEvent(ev)
        }
        return triggeredThisGesture || scaleDetector.isInProgress
    }

    override fun onControllerTouchEvent(ev: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(ev)
        return true
    }
}
