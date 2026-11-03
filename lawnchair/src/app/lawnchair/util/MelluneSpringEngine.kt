package app.lawnchair.util

import android.view.View
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce

/**
 * AndroidX Physics-based Spring Motion Engine for Mellune.
 * Emulates fluid flagship OEM physics (low damping ratio, realistic mass/stiffness).
 */
object MelluneSpringEngine {

    // Spring constants tuned for smooth, fluid flagship feel (ColorOS / iOS / Pixel hybrid)
    private const val STIFFNESS_TACTILE = 1200f
    private const val STIFFNESS_BLOOM = 600f
    private const val DAMPING_BOUNCY = 0.72f
    private const val DAMPING_SMOOTH = 0.82f

    /**
     * Plays a fluid 3D press sink when an icon is tapped.
     */
    fun playLaunchSink(view: View) {
        view.cameraDistance = view.resources.displayMetrics.density * 8000f

        SpringAnimation(view, DynamicAnimation.SCALE_X).apply {
            spring = SpringForce(0.86f).apply {
                stiffness = STIFFNESS_TACTILE
                dampingRatio = DAMPING_SMOOTH
            }
            start()
        }
        SpringAnimation(view, DynamicAnimation.SCALE_Y).apply {
            spring = SpringForce(0.86f).apply {
                stiffness = STIFFNESS_TACTILE
                dampingRatio = DAMPING_SMOOTH
            }
            start()
        }
    }

    /**
     * Plays an elastic spring rebound when returning from an app back to the desktop.
     */
    fun playReturnRebound(view: View) {
        SpringAnimation(view, DynamicAnimation.SCALE_X).apply {
            spring = SpringForce(1.0f).apply {
                stiffness = STIFFNESS_TACTILE
                dampingRatio = DAMPING_BOUNCY
            }
            start()
        }
        SpringAnimation(view, DynamicAnimation.SCALE_Y).apply {
            spring = SpringForce(1.0f).apply {
                stiffness = STIFFNESS_TACTILE
                dampingRatio = DAMPING_BOUNCY
            }
            start()
        }
    }

    /**
     * Springs the entire workspace forward smoothly when returning home.
     */
    fun playWorkspaceReturnSpring(workspace: View) {
        workspace.scaleX = 0.94f
        workspace.scaleY = 0.94f

        SpringAnimation(workspace, DynamicAnimation.SCALE_X).apply {
            spring = SpringForce(1.0f).apply {
                stiffness = STIFFNESS_BLOOM
                dampingRatio = DAMPING_SMOOTH
            }
            start()
        }
        SpringAnimation(workspace, DynamicAnimation.SCALE_Y).apply {
            spring = SpringForce(1.0f).apply {
                stiffness = STIFFNESS_BLOOM
                dampingRatio = DAMPING_SMOOTH
            }
            start()
        }
    }

    /**
     * Plays a full layered unlock bloom animation across workspace & dock with staggered rise physics.
     */
    fun playUnlockBloom(workspace: View?, hotseat: View?) {
        val density = workspace?.resources?.displayMetrics?.density
            ?: hotseat?.resources?.displayMetrics?.density
            ?: 2f

        val riseDistance = 30f * density

        workspace?.let { ws ->
            ws.scaleX = 0.91f
            ws.scaleY = 0.91f
            ws.translationY = riseDistance
            ws.alpha = 0.3f

            ws.animate()
                .alpha(1.0f)
                .setDuration(280)
                .start()

            SpringAnimation(ws, DynamicAnimation.SCALE_X).apply {
                spring = SpringForce(1.0f).apply {
                    stiffness = STIFFNESS_BLOOM
                    dampingRatio = DAMPING_SMOOTH
                }
                start()
            }
            SpringAnimation(ws, DynamicAnimation.SCALE_Y).apply {
                spring = SpringForce(1.0f).apply {
                    stiffness = STIFFNESS_BLOOM
                    dampingRatio = DAMPING_SMOOTH
                }
                start()
            }
            SpringAnimation(ws, DynamicAnimation.TRANSLATION_Y).apply {
                spring = SpringForce(0f).apply {
                    stiffness = STIFFNESS_BLOOM
                    dampingRatio = DAMPING_SMOOTH
                }
                start()
            }
        }

        hotseat?.let { hs ->
            hs.translationY = riseDistance * 0.7f
            hs.alpha = 0.3f

            hs.animate()
                .alpha(1.0f)
                .setDuration(280)
                .start()

            SpringAnimation(hs, DynamicAnimation.TRANSLATION_Y).apply {
                spring = SpringForce(0f).apply {
                    stiffness = STIFFNESS_BLOOM
                    dampingRatio = DAMPING_SMOOTH
                }
                start()
            }
        }
    }
}
