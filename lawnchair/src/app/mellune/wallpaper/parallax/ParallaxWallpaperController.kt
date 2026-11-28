package app.mellune.wallpaper.parallax

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.view.View

/**
 * Launcher-side bridge to [MelluneParallaxWallpaperService]. The launcher only sends small
 * "zoom from A to B over N ms" commands; the wallpaper engine runs the motion itself, so there
 * is no per-frame IPC.
 */
object ParallaxWallpaperController {
    const val ACTION_ZOOM = "app.mellune.parallax.ZOOM"
    const val EXTRA_FROM = "from"
    const val EXTRA_TARGET = "target"
    const val EXTRA_DURATION_MS = "duration_ms"

    /** [EXTRA_FROM] value meaning "continue from wherever the wallpaper currently is". */
    const val FROM_CURRENT = -1f

    fun component(context: Context) =
        ComponentName(context.packageName, MelluneParallaxWallpaperService::class.java.name)

    /** True when Mellune's parallax live wallpaper is the active home wallpaper. */
    fun isActive(context: Context): Boolean = runCatching {
        val info = WallpaperManager.getInstance(context).wallpaperInfo ?: return false
        info.packageName == context.packageName &&
            info.serviceName == MelluneParallaxWallpaperService::class.java.name
    }.getOrDefault(false)

    /** Intent that asks the system to preview/apply the parallax live wallpaper. */
    fun applyIntent(context: Context): Intent =
        Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component(context))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /**
     * Sends a zoom command to the wallpaper. [from] null continues from the current zoom.
     * [focalView], if given, is the icon the camera should dolly toward.
     *
     * @return true if the command was delivered, so the caller can skip the system zoom path.
     */
    fun sendZoom(
        context: Context,
        windowToken: IBinder?,
        from: Float?,
        target: Float,
        durationMs: Long,
        focalView: View? = null,
    ): Boolean {
        if (windowToken == null || !isActive(context)) return false
        var fx = -1
        var fy = -1
        if (focalView != null && focalView.width > 0) {
            val loc = IntArray(2)
            focalView.getLocationOnScreen(loc)
            fx = loc[0] + focalView.width / 2
            fy = loc[1] + focalView.height / 2
        }
        val extras = Bundle().apply {
            putFloat(EXTRA_FROM, from ?: FROM_CURRENT)
            putFloat(EXTRA_TARGET, target)
            putLong(EXTRA_DURATION_MS, durationMs)
        }
        return runCatching {
            WallpaperManager.getInstance(context)
                .sendWallpaperCommand(windowToken, ACTION_ZOOM, fx, fy, 0, extras)
            true
        }.getOrDefault(false)
    }
}
