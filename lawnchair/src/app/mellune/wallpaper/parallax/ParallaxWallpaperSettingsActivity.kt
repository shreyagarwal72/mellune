package app.mellune.wallpaper.parallax

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.launcher3.R

/** Small settings screen for the parallax wallpaper, also reachable from the system picker. */
class ParallaxWallpaperSettingsActivity : ComponentActivity() {

    private val picker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@registerForActivityResult
        Thread {
            val ok = ParallaxWallpaperStore.importFromUri(applicationContext, uri)
            if (ok) ParallaxWallpaperStore.notifyChanged(applicationContext)
            runOnUiThread {
                Toast.makeText(
                    this,
                    if (ok) R.string.mellune_parallax_image_set else R.string.mellune_parallax_image_failed,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.mellune_parallax_wallpaper_label)
        val pad = (20 * resources.displayMetrics.density).toInt()
        val prefs = ParallaxWallpaperStore.prefs(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(pad + bars.left, pad + bars.top, pad + bars.right, pad + bars.bottom)
            insets
        }

        fun label(res: Int) = TextView(this).apply {
            setText(res)
            textSize = 16f
            gravity = Gravity.START
            setPadding(0, pad / 2, 0, pad / 4)
        }
        fun button(res: Int, onClick: () -> Unit) = Button(this).apply {
            setText(res)
            setOnClickListener { onClick() }
        }

        root.addView(label(R.string.mellune_parallax_intensity))
        root.addView(
            SeekBar(this).apply {
                max = 140
                progress = ((ParallaxWallpaperStore.intensity(this@ParallaxWallpaperSettingsActivity) - 0.2f) * 100f).toInt()
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(bar: SeekBar, value: Int, fromUser: Boolean) {
                        if (fromUser) prefs.edit().putFloat(ParallaxWallpaperStore.KEY_INTENSITY, 0.2f + value / 100f).apply()
                    }
                    override fun onStartTrackingTouch(bar: SeekBar) = Unit
                    override fun onStopTrackingTouch(bar: SeekBar) = Unit
                })
            },
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
        )
        root.addView(
            Switch(this).apply {
                setText(R.string.mellune_parallax_tilt)
                isChecked = ParallaxWallpaperStore.tiltEnabled(this@ParallaxWallpaperSettingsActivity)
                setPadding(0, pad / 2, 0, pad / 2)
                setOnCheckedChangeListener { _, checked ->
                    prefs.edit().putBoolean(ParallaxWallpaperStore.KEY_TILT, checked).apply()
                }
            },
        )
        root.addView(button(R.string.mellune_parallax_choose_image) { picker.launch("image/*") })
        root.addView(
            button(R.string.mellune_parallax_regenerate_depth) {
                ParallaxWallpaperStore.regenerateDepth(applicationContext)
            },
        )
        root.addView(
            button(R.string.mellune_parallax_apply) {
                runCatching { startActivity(ParallaxWallpaperController.applyIntent(this)) }
            },
        )
        setContentView(root)
    }
}
