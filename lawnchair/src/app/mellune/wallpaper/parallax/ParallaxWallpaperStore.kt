package app.mellune.wallpaper.parallax

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Produces an 8-bit depth map for a wallpaper photo. 0 = far, 255 = near. */
fun interface ParallaxDepthGenerator {
    fun generate(source: Bitmap): Bitmap
}

/**
 * Dependency-free fallback depth: blurred luminance (bright areas read as far, dark detail as
 * near) mixed with a "ground is closer" vertical ramp and a gentle centre bias. It is a
 * heuristic, not real depth; plug a model-based [ParallaxDepthGenerator] into
 * [ParallaxWallpaperStore.depthGenerator] to replace it.
 */
object ProceduralDepthGenerator : ParallaxDepthGenerator {
    private const val SIZE_LONG_EDGE = 256

    override fun generate(source: Bitmap): Bitmap {
        val scale = SIZE_LONG_EDGE.toFloat() / max(source.width, source.height)
        val w = max(8, (source.width * scale).toInt())
        val h = max(8, (source.height * scale).toInt())
        val small = Bitmap.createScaledBitmap(source, w, h, true)
        val px = IntArray(w * h)
        small.getPixels(px, 0, w, 0, 0, w, h)
        if (small !== source) small.recycle()

        var lum = FloatArray(w * h) { i ->
            val c = px[i]
            (0.299f * Color.red(c) + 0.587f * Color.green(c) + 0.114f * Color.blue(c)) / 255f
        }
        repeat(3) { lum = boxBlur(lum, w, h, 4) }

        val out = IntArray(w * h)
        for (y in 0 until h) {
            val ny = y / (h - 1f)
            for (x in 0 until w) {
                val nx = x / (w - 1f) - 0.5f
                val centre = 1f - min(1f, sqrt(nx * nx + (ny - 0.5f) * (ny - 0.5f)) * 1.6f)
                val d = 0.45f * (1f - lum[y * w + x]) + 0.40f * ny + 0.15f * centre
                val v = (d.coerceIn(0f, 1f) * 255f).toInt()
                out[y * w + x] = Color.rgb(v, v, v)
            }
        }
        return Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888)
    }

    private fun boxBlur(src: FloatArray, w: Int, h: Int, r: Int): FloatArray {
        val tmp = FloatArray(w * h)
        val dst = FloatArray(w * h)
        for (y in 0 until h) {
            var sum = 0f
            var count = 0
            for (x in 0..min(r, w - 1)) { sum += src[y * w + x]; count++ }
            for (x in 0 until w) {
                tmp[y * w + x] = sum / count
                val add = x + r + 1
                val rem = x - r
                if (add < w) { sum += src[y * w + add]; count++ }
                if (rem >= 0) { sum -= src[y * w + rem]; count-- }
            }
        }
        for (x in 0 until w) {
            var sum = 0f
            var count = 0
            for (y in 0..min(r, h - 1)) { sum += tmp[y * w + x]; count++ }
            for (y in 0 until h) {
                dst[y * w + x] = sum / count
                val add = y + r + 1
                val rem = y - r
                if (add < h) { sum += tmp[add * w + x]; count++ }
                if (rem >= 0) { sum -= tmp[rem * w + x]; count-- }
            }
        }
        return dst
    }
}

/** Files and preferences for the Mellune parallax live wallpaper. */
object ParallaxWallpaperStore {
    private const val PREFS = "mellune_parallax_wallpaper"
    const val KEY_INTENSITY = "intensity"
    const val KEY_TILT = "tilt"
    const val KEY_REVISION = "revision"
    const val MAX_SOURCE_EDGE = 2048

    @Volatile
    var depthGenerator: ParallaxDepthGenerator = ProceduralDepthGenerator

    fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun intensity(context: Context): Float = prefs(context).getFloat(KEY_INTENSITY, 1f).coerceIn(0.2f, 1.6f)

    fun tiltEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_TILT, true)

    private fun dir(context: Context) = File(context.applicationContext.filesDir, "parallax").apply { mkdirs() }

    private fun sourceFile(context: Context) = File(dir(context), "source.jpg")

    private fun depthFile(context: Context) = File(dir(context), "depth.png")

    fun hasSource(context: Context) = sourceFile(context).exists()

    /** Decodes the stored photo, downsampled so its long edge is at most [MAX_SOURCE_EDGE]. */
    fun loadSource(context: Context): Bitmap? {
        val f = sourceFile(context)
        if (!f.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, bounds)
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SOURCE_EDGE) sample *= 2
        return BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    /** Loads the cached depth map, generating (and caching) it from [source] if needed. */
    fun loadDepth(context: Context, source: Bitmap): Bitmap {
        val f = depthFile(context)
        if (f.exists() && f.lastModified() >= sourceFile(context).lastModified()) {
            BitmapFactory.decodeFile(f.path)?.let { return it }
        }
        val depth = depthGenerator.generate(source)
        runCatching { f.outputStream().use { depth.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        return depth
    }

    /** Stores [bitmap] as the wallpaper photo and drops the cached depth map. */
    fun saveSource(context: Context, bitmap: Bitmap) {
        sourceFile(context).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        depthFile(context).delete()
    }

    fun importFromUri(context: Context, uri: Uri): Boolean = runCatching {
        val bmp = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            ?: return false
        saveSource(context, bmp)
        true
    }.getOrDefault(false)

    fun regenerateDepth(context: Context) {
        depthFile(context).delete()
        notifyChanged(context)
    }

    /** Wakes the wallpaper engine so it re-checks the photo/depth files. */
    fun notifyChanged(context: Context) {
        prefs(context).edit().putLong(KEY_REVISION, System.currentTimeMillis()).apply()
    }

    /** Cheap hash so the engine can tell when the photo or depth changed. */
    fun version(context: Context): Long =
        abs(sourceFile(context).lastModified() xor (depthFile(context).lastModified() shl 1))
}
