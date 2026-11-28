package app.mellune.wallpaper.parallax

import android.app.WallpaperColors
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.PowerManager
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.Choreographer
import android.view.SurfaceHolder
import androidx.annotation.RequiresApi
import kotlin.math.exp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow

/**
 * Mellune's own 3D parallax live wallpaper. It renders a photo plus depth map and animates a
 * virtual camera (dolly, blur, tilt, home-page scroll) in response to commands from the launcher
 * (see [ParallaxWallpaperController]). It deliberately ignores the system's wallpaper zoom, which
 * third-party launchers cannot rely on.
 */
class MelluneParallaxWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = ParallaxEngine()

    private inner class ParallaxEngine : Engine(), SensorEventListener {
        private val appContext: Context = this@MelluneParallaxWallpaperService.applicationContext
        private val thread = HandlerThread("MelluneParallaxGL")
        private lateinit var handler: Handler
        private var choreographer: Choreographer? = null

        private val renderer = ParallaxGlRenderer()
        private val camera = ParallaxCamera()

        // Everything below is touched only on the render thread.
        private var visible = false
        private var frameQueued = false
        private var lastFrameNanos = 0L
        private var loadedVersion = Long.MIN_VALUE
        private var screenW = 1
        private var screenH = 1

        private var zoom = 0f
        private var zoomFrom = 0f
        private var zoomTarget = 0f
        private var zoomStartMs = 0L
        private var zoomDurationMs = 0L
        private var zoomAnimating = false

        private var scrollTarget = 0f
        private var scrollSmoothed = 0f
        private var tiltTargetX = 0f
        private var tiltTargetY = 0f
        private var tiltX = 0f
        private var tiltY = 0f
        private var baseX = 0f
        private var baseY = 0f
        private var haveBase = false

        private var tiltEnabled = true
        private var sensorActive = false
        private var sensorManager: SensorManager? = null

        private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            handler.post { applyPrefs(); loadImagesIfChanged(); updateSensor(); requestFrame() }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(false)
            thread.start()
            handler = Handler(thread.looper)
            handler.post { choreographer = Choreographer.getInstance() }
            sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            ParallaxWallpaperStore.prefs(appContext).registerOnSharedPreferenceChangeListener(prefsListener)
            handler.post { applyPrefs() }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            handler.post {
                if (renderer.createSurface(holder.surface)) {
                    loadedVersion = Long.MIN_VALUE
                    loadImagesIfChanged()
                    requestFrame()
                }
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            handler.post {
                screenW = max(1, width)
                screenH = max(1, height)
                renderer.setSize(width, height)
                requestFrame()
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            handler.post { renderer.destroySurface() }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            handler.post {
                this.visible = visible
                if (visible) loadImagesIfChanged()
                updateSensor()
                requestFrame()
            }
        }

        override fun onOffsetsChanged(
            xOffset: Float, yOffset: Float, xOffsetStep: Float, yOffsetStep: Float,
            xPixelOffset: Int, yPixelOffset: Int,
        ) {
            super.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset, yPixelOffset)
            val target = if (xOffsetStep > 0f) ((xOffset - 0.5f) * 2f).coerceIn(-1f, 1f) else 0f
            handler.post { scrollTarget = target; requestFrame() }
        }

        override fun onCommand(
            action: String?, x: Int, y: Int, z: Int, extras: Bundle?, resultRequested: Boolean,
        ): Bundle? {
            if (action == ParallaxWallpaperController.ACTION_ZOOM && extras != null) {
                val from = extras.getFloat(ParallaxWallpaperController.EXTRA_FROM, ParallaxWallpaperController.FROM_CURRENT)
                val target = extras.getFloat(ParallaxWallpaperController.EXTRA_TARGET, 0f)
                val duration = extras.getLong(ParallaxWallpaperController.EXTRA_DURATION_MS, 300L)
                handler.post { startZoom(from, target, duration, x, y) }
            }
            return null
        }

        @RequiresApi(Build.VERSION_CODES.O_MR1)
        override fun onComputeColors(): WallpaperColors? {
            val src = ParallaxWallpaperStore.loadSource(appContext) ?: return null
            val small = Bitmap.createScaledBitmap(src, 64, max(1, 64 * src.height / src.width), true)
            return WallpaperColors.fromBitmap(small).also {
                if (small !== src) small.recycle()
                src.recycle()
            }
        }

        override fun onDestroy() {
            ParallaxWallpaperStore.prefs(appContext).unregisterOnSharedPreferenceChangeListener(prefsListener)
            handler.post {
                stopSensor()
                renderer.release()
                thread.quitSafely()
            }
            super.onDestroy()
        }

        // ---- render thread ----

        private fun applyPrefs() {
            camera.strength = ParallaxWallpaperStore.intensity(appContext)
            tiltEnabled = ParallaxWallpaperStore.tiltEnabled(appContext)
        }

        private fun loadImagesIfChanged() {
            if (!renderer.hasSurface) return
            val version = ParallaxWallpaperStore.version(appContext)
            if (version == loadedVersion && renderer.hasImages) return
            val photo = ParallaxWallpaperStore.loadSource(appContext) ?: fallbackPhoto()
            val depth = ParallaxWallpaperStore.loadDepth(appContext, photo)
            renderer.setImages(photo, depth)
            photo.recycle()
            depth.recycle()
            loadedVersion = version
        }

        /** Soft vertical gradient so the wallpaper is never black before a photo is chosen. */
        private fun fallbackPhoto(): Bitmap {
            val bmp = Bitmap.createBitmap(512, 1024, Bitmap.Config.ARGB_8888)
            val paint = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 0f, 1024f,
                    intArrayOf(Color.rgb(24, 32, 64), Color.rgb(112, 84, 140), Color.rgb(236, 150, 120)),
                    null, Shader.TileMode.CLAMP,
                )
            }
            Canvas(bmp).drawRect(0f, 0f, 512f, 1024f, paint)
            return bmp
        }

        private fun startZoom(from: Float, target: Float, durationMs: Long, fx: Int, fy: Int) {
            zoomFrom = if (from == ParallaxWallpaperController.FROM_CURRENT) zoom else from
            zoom = zoomFrom
            zoomTarget = target
            zoomDurationMs = max(1L, durationMs)
            zoomStartMs = SystemClock.uptimeMillis()
            zoomAnimating = true
            camera.focalU = if (fx >= 0) (fx.toFloat() / screenW).coerceIn(0f, 1f) else 0.5f
            camera.focalV = if (fy >= 0) (fy.toFloat() / screenH).coerceIn(0f, 1f) else 0.5f
            requestFrame()
        }

        private fun requestFrame() {
            if (frameQueued || !visible || !renderer.hasSurface) return
            val c = choreographer ?: return
            frameQueued = true
            c.postFrameCallback { nanos -> frameQueued = false; onFrame(nanos) }
        }

        private fun onFrame(nanos: Long) {
            val dt = if (lastFrameNanos == 0L) 1f / 60f else ((nanos - lastFrameNanos) / 1e9f).coerceIn(0.001f, 0.1f)
            lastFrameNanos = nanos

            if (zoomAnimating) {
                val t = ((SystemClock.uptimeMillis() - zoomStartMs).toFloat() / zoomDurationMs).coerceIn(0f, 1f)
                // Ease-out cubic: quick push, soft settle.
                val e = 1f - (1f - t).pow(3)
                zoom = zoomFrom + (zoomTarget - zoomFrom) * e
                if (t >= 1f) { zoom = zoomTarget; zoomAnimating = false }
            }
            val kScroll = 1f - exp(-dt * 7f)
            val kTilt = 1f - exp(-dt * 9f)
            scrollSmoothed += (scrollTarget - scrollSmoothed) * kScroll
            tiltX += (tiltTargetX - tiltX) * kTilt
            tiltY += (tiltTargetY - tiltY) * kTilt

            camera.x = (scrollSmoothed + tiltX).coerceIn(-1.5f, 1.5f)
            camera.y = tiltY.coerceIn(-1.5f, 1.5f)
            camera.dolly = zoom
            renderer.draw(camera)

            val settling = zoomAnimating ||
                abs(scrollTarget - scrollSmoothed) > 0.002f ||
                abs(tiltTargetX - tiltX) > 0.002f || abs(tiltTargetY - tiltY) > 0.002f
            if (settling || sensorActive) requestFrame() else lastFrameNanos = 0L
        }

        // ---- tilt sensor ----

        private fun updateSensor() {
            val powerSave = (appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isPowerSaveMode == true
            if (visible && tiltEnabled && !powerSave) startSensor() else stopSensor()
        }

        private fun startSensor() {
            if (sensorActive) return
            val sm = sensorManager ?: return
            val sensor = sm.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR) ?: return
            haveBase = false
            sensorActive = sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME, handler)
        }

        private fun stopSensor() {
            if (!sensorActive) return
            sensorManager?.unregisterListener(this)
            sensorActive = false
            tiltTargetX = 0f
            tiltTargetY = 0f
        }

        private val rot = FloatArray(9)

        override fun onSensorChanged(event: SensorEvent) {
            SensorManager.getRotationMatrixFromVector(rot, event.values)
            // World "up" expressed in device axes: x = left/right lean, z = top/bottom lean.
            val gx = rot[6]
            val gz = rot[8]
            if (!haveBase) { baseX = gx; baseY = gz; haveBase = true }
            // Slowly re-centre so holding the phone at a new angle becomes the new rest pose.
            baseX += (gx - baseX) * 0.01f
            baseY += (gz - baseY) * 0.01f
            tiltTargetX = ((gx - baseX) / TILT_RANGE).coerceIn(-1f, 1f)
            tiltTargetY = ((gz - baseY) / TILT_RANGE).coerceIn(-1f, 1f)
            requestFrame()
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private companion object {
        /** Gravity-component change (~20 degrees) that maps to full camera travel. */
        const val TILT_RANGE = 0.35f
    }
}
