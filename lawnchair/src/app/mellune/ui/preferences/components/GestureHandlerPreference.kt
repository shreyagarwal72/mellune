package app.mellune.ui.preferences.components

import android.R as AndroidR
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.mellune.gestures.config.GestureHandlerConfig
import app.mellune.gestures.config.GestureHandlerOption
import app.mellune.gestures.config.buildConfigFrom
import app.mellune.gestures.config.filterGestureHandlerOptions
import app.mellune.gestures.type.GestureType
import app.mellune.gestures.ui.MelluneShortcutActivity
import app.mellune.preferences.PreferenceAdapter
import app.mellune.preferences.getAdapter
import app.mellune.preferences2.preferenceManager2
import app.mellune.ui.ModalBottomSheetContent
import app.mellune.ui.preferences.components.layout.PreferenceDivider
import app.mellune.ui.preferences.components.layout.PreferenceTemplate
import app.mellune.ui.util.LocalBottomSheetHandler
import com.android.launcher3.util.ComponentKey
import com.android.launcher3.util.MSDLPlayerWrapper
import com.google.android.msdl.data.model.MSDLToken
import kotlinx.coroutines.launch

@Composable
fun GestureHandlerPreference(
    adapter: PreferenceAdapter<GestureHandlerConfig>,
    label: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bottomSheetHandler = LocalBottomSheetHandler.current
    val prefs2 = preferenceManager2()
    val mMSDLPlayerWrapper = MSDLPlayerWrapper.INSTANCE.get(context)

    val currentConfig = adapter.state.value

    fun onSelect(option: GestureHandlerOption) {
        scope.launch {
            val config = option.buildConfigFrom(context) ?: return@launch
            adapter.onChange(config)
        }
    }

    val newOptions =
        filterGestureHandlerOptions(deckLayoutEnabled = prefs2.deckLayout.getAdapter().state.value)

    PreferenceTemplate(
        title = { Text(text = label) },
        modifier = modifier,
        description = { Text(text = currentConfig.getLabel(context)) },
        onClick = {
            mMSDLPlayerWrapper.playToken(MSDLToken.TAP_MEDIUM_EMPHASIS)
            bottomSheetHandler.show {
                ModalBottomSheetContent(
                    title = { Text(label) },
                    buttons = {
                        app.mellune.ui.m3e.M3EOutlinedButton(
                            onClick = { bottomSheetHandler.hide() },
                        ) {
                            Text(text = stringResource(id = AndroidR.string.cancel))
                        }
                    },
                ) {
                    LazyColumn {
                        itemsIndexed(newOptions) { index, option ->
                            if (index > 0) {
                                PreferenceDivider(startIndent = 40.dp)
                            }
                            val selected = currentConfig::class.java == option.configClass
                            PreferenceTemplate(
                                title = { Text(option.getLabel(context)) },
                                onClick = {
                                    bottomSheetHandler.hide()
                                    mMSDLPlayerWrapper.playToken(MSDLToken.TAP_LOW_EMPHASIS)
                                    onSelect(option)
                                },
                                startWidget = {
                                    RadioButton(
                                        selected = selected,
                                        onClick = null,
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            )
                        }
                    }
                }
            }
        },
    )
}

@Composable
fun AppGesturePreference(
    cmp: ComponentKey,
    gestureType: GestureType,
    label: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = preferenceManager2()
    val mMSDLPlayerWrapper = MSDLPlayerWrapper.INSTANCE.get(context)

    val currentConfig by produceState<GestureHandlerConfig>(initialValue = GestureHandlerConfig.NoOp) {
        prefs.getGestureForApp(cmp, gestureType).collect { value = it }
    }

    val resultReceiver = remember {
        object : ResultReceiver(Handler(Looper.getMainLooper())) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                if (resultCode == android.app.Activity.RESULT_OK) {
                    val handlerString = resultData?.getString(MelluneShortcutActivity.EXTRA_HANDLER)
                    if (handlerString != null) {
                        val config = GestureHandlerConfig.fromString(handlerString)
                        scope.launch {
                            prefs.setGestureForApp(cmp, gestureType, config)
                        }
                    }
                }
            }
        }
    }

    PreferenceTemplate(
        title = { Text(text = label) },
        description = { Text(text = currentConfig.getLabel(context)) },
        modifier = modifier.fillMaxWidth(),
        onClick = {
            mMSDLPlayerWrapper.playToken(MSDLToken.TAP_MEDIUM_EMPHASIS)
            val intent = Intent(context, MelluneShortcutActivity::class.java).apply {
                putExtra(MelluneShortcutActivity.EXTRA_RESULT_RECEIVER, resultReceiver)
            }
            context.startActivity(intent)
        },
    )
}
