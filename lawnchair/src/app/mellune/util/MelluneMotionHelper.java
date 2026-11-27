package app.mellune.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.view.View;
import android.view.animation.PathInterpolator;

/**
 * Provides 3D parallax depth scaling, unlock bloom, and spring-return animations,
 * giving native-like fluid feel universally across all devices without requiring root privileges.
 */
public final class MelluneMotionHelper {

    // Spring-like decelerate interpolators mimicking modern flagship system physics
    private static final PathInterpolator LAUNCH_INTERPOLATOR =
            new PathInterpolator(0.2f, 0f, 0f, 1f);
    private static final PathInterpolator RETURN_SPRING_INTERPOLATOR =
            new PathInterpolator(0.34f, 1.56f, 0.64f, 1f);
    private static final PathInterpolator UNLOCK_BLOOM_INTERPOLATOR =
            new PathInterpolator(0.05f, 0.7f, 0.1f, 1f);

    private MelluneMotionHelper() {}

    /**
     * Triggers a subtle 3D scale-and-sink animation on the clicked icon view to provide tactile depth
     * as the application window opens.
     */
    public static void playLaunchParallax(View view) {
        if (view == null) return;

        view.setCameraDistance(view.getResources().getDisplayMetrics().density * 8000);
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.88f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.88f);

        ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY);
        anim.setDuration(180);
        anim.setInterpolator(LAUNCH_INTERPOLATOR);
        anim.start();
    }

    /**
     * Plays a fluid spring rebound on the last launched view when returning to the desktop.
     */
    public static void playReturnRebound(View view) {
        if (view == null) return;

        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f);

        ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY);
        anim.setDuration(350);
        anim.setInterpolator(RETURN_SPRING_INTERPOLATOR);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.setScaleX(1f);
                view.setScaleY(1f);
            }
        });
        anim.start();
    }

    /**
     * Plays a subtle incoming desktop parallax scale effect when the launcher is resumed from home gesture.
     */
    public static void playWorkspaceReturnMotion(View workspace) {
        if (workspace == null) return;

        workspace.setScaleX(0.96f);
        workspace.setScaleY(0.96f);

        workspace.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(280)
                .setInterpolator(LAUNCH_INTERPOLATOR)
                .start();
    }

    /**
     * Plays a seamless system-like unlocking bloom animation when the device keyguard is dismissed.
     * Animates the workspace & hotseat rising and expanding smoothly into view.
     */
    public static void playUnlockBloom(View workspace, View hotseat) {
        if (workspace == null && hotseat == null) return;

        float density = workspace != null
                ? workspace.getResources().getDisplayMetrics().density
                : (hotseat != null ? hotseat.getResources().getDisplayMetrics().density : 2f);
        float riseTranslation = 24f * density;

        if (workspace != null) {
            workspace.setScaleX(0.92f);
            workspace.setScaleY(0.92f);
            workspace.setTranslationY(riseTranslation);
            workspace.setAlpha(0.4f);

            workspace.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .translationY(0f)
                    .alpha(1.0f)
                    .setDuration(360)
                    .setInterpolator(UNLOCK_BLOOM_INTERPOLATOR)
                    .start();
        }

        if (hotseat != null) {
            hotseat.setTranslationY(riseTranslation * 0.75f);
            hotseat.setAlpha(0.4f);

            hotseat.animate()
                    .translationY(0f)
                    .alpha(1.0f)
                    .setDuration(360)
                    .setInterpolator(UNLOCK_BLOOM_INTERPOLATOR)
                    .start();
        }
    }
}
