package app.mellune.nexuslauncher

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.IBinder
import android.os.Process
import android.util.Log
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Built-in Google Overlay proxy service inspired by Nova Google Companion.
 * Bridges `com.android.launcher3.WINDOW_OVERLAY` requests to the Google App
 * so Mellune's -1 Discover feed works seamlessly as a built-in preference option.
 */
class GoogleOverlayProxyService : Service() {

    private var remoteConnection: ServiceConnection? = null
    private var remoteBinder: IBinder? = null

    override fun onBind(intent: Intent?): IBinder? {
        val targetIntent = buildGoogleOverlayIntent(intent) ?: return null
        val latch = CountDownLatch(1)
        var boundBinder: IBinder? = null

        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                boundBinder = service
                remoteBinder = service
                latch.countDown()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                remoteBinder = null
            }
        }

        val bound = runCatching {
            bindService(targetIntent, connection, BIND_AUTO_CREATE or BIND_IMPORTANT)
        }.getOrDefault(false)

        if (!bound) {
            return null
        }

        remoteConnection = connection
        runCatching {
            latch.await(1500L, TimeUnit.MILLISECONDS)
        }
        return boundBinder ?: remoteBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        remoteConnection?.let { conn ->
            runCatching { unbindService(conn) }
        }
        remoteConnection = null
        remoteBinder = null
        return super.onUnbind(intent)
    }

    private fun buildGoogleOverlayIntent(sourceIntent: Intent?): Intent? {
        val googlePkg = GOOGLE_APP_PACKAGE
        val installed = runCatching {
            packageManager.getPackageInfo(googlePkg, 0)
            true
        }.getOrDefault(false)
        if (!installed) {
            Log.w(TAG, "Google App ($googlePkg) is not installed; cannot proxy overlay")
            return null
        }

        val dataUri = sourceIntent?.data ?: Uri.parse("app://$packageName:${Process.myUid()}")
            .buildUpon()
            .appendQueryParameter("v", "7")
            .appendQueryParameter("cv", "9")
            .build()

        val candidate = Intent(OVERLAY_ACTION)
            .setPackage(googlePkg)
            .setData(dataUri)

        val resolved = packageManager.resolveService(candidate, PackageManager.MATCH_DEFAULT_ONLY)
        return if (resolved != null) candidate else null
    }

    companion object {
        private const val TAG = "MelluneGoogleCompanion"
        private const val OVERLAY_ACTION = "com.android.launcher3.WINDOW_OVERLAY"
        const val GOOGLE_APP_PACKAGE = "com.google.android.googlequicksearchbox"

        fun isGoogleAppInstalled(pm: PackageManager): Boolean =
            runCatching {
                pm.getPackageInfo(GOOGLE_APP_PACKAGE, 0)
                true
            }.getOrDefault(false)
    }
}
