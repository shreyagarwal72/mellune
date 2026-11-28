package app.mellune.search.algorithms.engine.provider

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import app.mellune.preferences2.PreferenceManager2
import app.mellune.preferences2.firstCached
import app.mellune.search.algorithms.data.QuickToggleInfo
import app.mellune.search.algorithms.engine.SearchProvider
import app.mellune.search.algorithms.engine.SearchResult
import com.android.launcher3.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Provides actionable quick-toggle system tiles (Flashlight, Wi-Fi, Bluetooth, Volume,
 * Dark theme, Night Light, Airplane mode) directly inside App Drawer search.
 */
object QuickSettingsSearchProvider : SearchProvider {

    const val EXTRA_QUICK_TOGGLE_ID = "quick_toggle_id"
    const val ACTION_TOGGLE_FLASHLIGHT = "flashlight"

    override val id: String = "quick_toggles"

    @Volatile
    private var torchEnabled: Boolean = false

    @Volatile
    private var torchCallbackRegistered: Boolean = false

    private data class TileSpec(
        val id: String,
        val keywords: List<String>,
        val titleRes: Int,
        val iconRes: Int,
        val directActionKey: String? = null,
        val subtitleProvider: (Context) -> String,
        val intentProvider: () -> Intent,
    )

    private val specs: List<TileSpec> = listOf(
        TileSpec(
            id = ACTION_TOGGLE_FLASHLIGHT,
            keywords = listOf("flashlight", "torch", "flash", "light"),
            titleRes = R.string.quick_toggle_flashlight,
            iconRes = R.drawable.ic_flashlight_off,
            directActionKey = ACTION_TOGGLE_FLASHLIGHT,
            subtitleProvider = { ctx ->
                ensureTorchCallback(ctx)
                ctx.getString(if (torchEnabled) R.string.quick_toggle_state_on else R.string.quick_toggle_state_off)
            },
            intentProvider = { Intent(Settings.ACTION_DISPLAY_SETTINGS) },
        ),
        TileSpec(
            id = "wifi",
            keywords = listOf("wifi", "wi-fi", "internet", "network", "wlan"),
            titleRes = R.string.quick_toggle_wifi,
            iconRes = R.drawable.ic_quick_wifi,
            subtitleProvider = { ctx ->
                if (isWifiConnected(ctx)) {
                    ctx.getString(R.string.quick_toggle_state_on)
                } else {
                    ctx.getString(R.string.quick_toggle_open_panel)
                }
            },
            intentProvider = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
                } else {
                    Intent(Settings.ACTION_WIFI_SETTINGS)
                }
            },
        ),
        TileSpec(
            id = "bluetooth",
            keywords = listOf("bluetooth", "bt", "pair", "headphones", "audio device"),
            titleRes = R.string.quick_toggle_bluetooth,
            iconRes = R.drawable.ic_quick_bluetooth,
            subtitleProvider = { ctx -> ctx.getString(R.string.quick_toggle_open_panel) },
            intentProvider = { Intent(Settings.ACTION_BLUETOOTH_SETTINGS) },
        ),
        TileSpec(
            id = "volume",
            keywords = listOf("volume", "sound", "audio", "ring", "mute", "media"),
            titleRes = R.string.quick_toggle_volume,
            iconRes = R.drawable.ic_quick_volume,
            subtitleProvider = { ctx ->
                val audio = ctx.getSystemService(AudioManager::class.java)
                if (audio != null) {
                    val cur = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                    val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                    "${(cur * 100) / max}%"
                } else {
                    ctx.getString(R.string.quick_toggle_open_panel)
                }
            },
            intentProvider = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Intent(Settings.Panel.ACTION_VOLUME)
                } else {
                    Intent(Settings.ACTION_SOUND_SETTINGS)
                }
            },
        ),
        TileSpec(
            id = "dark_mode",
            keywords = listOf("dark", "dark theme", "dark mode", "theme", "night"),
            titleRes = R.string.quick_toggle_dark_mode,
            iconRes = R.drawable.ic_quick_dark_mode,
            subtitleProvider = { ctx ->
                val nightFlags = ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                ctx.getString(
                    if (nightFlags == Configuration.UI_MODE_NIGHT_YES) {
                        R.string.quick_toggle_state_on
                    } else {
                        R.string.quick_toggle_state_off
                    },
                )
            },
            intentProvider = { Intent(Settings.ACTION_DISPLAY_SETTINGS) },
        ),
        TileSpec(
            id = "airplane",
            keywords = listOf("airplane", "flight", "offline"),
            titleRes = R.string.quick_toggle_airplane,
            iconRes = R.drawable.ic_quick_airplane,
            subtitleProvider = { ctx ->
                val isOn = Settings.Global.getInt(
                    ctx.contentResolver,
                    Settings.Global.AIRPLANE_MODE_ON,
                    0,
                ) != 0
                ctx.getString(if (isOn) R.string.quick_toggle_state_on else R.string.quick_toggle_state_off)
            },
            intentProvider = { Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS) },
        ),
    )

    override fun search(context: Context, query: String): Flow<List<SearchResult>> = flow {
        val prefs2 = PreferenceManager2.getInstance(context)
        if (query.isBlank() || !prefs2.searchResultQuickToggles.firstCached()) {
            emit(emptyList())
            return@flow
        }

        val normalized = query.trim().lowercase()
        val matches = specs.mapNotNull { spec ->
            val title = context.getString(spec.titleRes)
            val isMatch = title.lowercase().contains(normalized) ||
                spec.keywords.any { it.contains(normalized) || normalized.contains(it) }
            if (!isMatch) return@mapNotNull null

            val info = QuickToggleInfo(
                id = spec.id,
                title = title,
                subtitle = spec.subtitleProvider(context),
                iconRes = spec.iconRes,
                intent = spec.intentProvider().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                directActionKey = spec.directActionKey,
            )
            SearchResult.QuickToggle(info)
        }
        emit(matches)
    }

    /**
     * Toggles the device camera torch in-place when available and returns the updated state string.
     */
    fun toggleFlashlight(context: Context): String? {
        val cameraManager = context.getSystemService(CameraManager::class.java) ?: return null
        ensureTorchCallback(context)
        val cameraId = runCatching {
            cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull() ?: return null

        val targetState = !torchEnabled
        return runCatching {
            cameraManager.setTorchMode(cameraId, targetState)
            torchEnabled = targetState
            context.getString(if (targetState) R.string.quick_toggle_state_on else R.string.quick_toggle_state_off)
        }.onFailure {
            Toast.makeText(context, R.string.lawnchair_action_failed, Toast.LENGTH_SHORT).show()
        }.getOrNull()
    }

    private fun ensureTorchCallback(context: Context) {
        if (torchCallbackRegistered) return
        val cameraManager = context.getSystemService(CameraManager::class.java) ?: return
        torchCallbackRegistered = true
        runCatching {
            cameraManager.registerTorchCallback(
                object : CameraManager.TorchCallback() {
                    override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                        torchEnabled = enabled
                    }
                },
                null,
            )
        }
    }

    private fun isWifiConnected(context: Context): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}
