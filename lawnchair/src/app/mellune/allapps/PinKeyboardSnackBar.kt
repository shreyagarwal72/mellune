package app.mellune.allapps

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import app.mellune.launcher
import app.mellune.preferences2.PreferenceManager2
import app.mellune.preferences2.firstCached
import app.mellune.ui.theme.MelluneTheme
import app.mellune.util.ProvideLifecycleState
import com.android.launcher3.AbstractFloatingView
import com.android.launcher3.Insettable
import com.android.launcher3.R
import com.android.launcher3.views.ActivityContext
import kotlinx.coroutines.launch

/**
 * Floating snackbar prompting users to pin the keyboard when opening app drawer search.
 */
@SuppressLint("ViewConstructor")
class PinKeyboardSnackBar(
    context: Context,
) : AbstractFloatingView(context, null, 0),
    Insettable {

    private val activityContext: ActivityContext = ActivityContext.lookupContext(context)
    private val prefs2 = PreferenceManager2.getInstance(context)
    private val composeView = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            ProvideLifecycleState {
                MelluneTheme {
                    PinKeyboardSnackBarContent(
                        onConfirm = {
                            close(true)
                        },
                        onDismiss = {
                            close(true)
                        },
                        prefs2 = prefs2,
                    )
                }
            }
        }
    }

    init {
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            Gravity.BOTTOM,
        )
        addView(composeView)
    }

    override fun handleClose(animate: Boolean) {
        if (!mIsOpen) return
        if (animate) {
            animate()
                .alpha(0f)
                .translationY(24f * resources.displayMetrics.density)
                .setDuration(180L)
                .withEndAction {
                    activityContext.dragLayer?.removeView(this)
                }
                .start()
        } else {
            activityContext.dragLayer?.removeView(this)
        }
    }

    override fun isOfType(@FloatingViewType type: Int): Boolean = (type and TYPE_PIN_IME_POPUP) != 0

    override fun onControllerInterceptTouchEvent(ev: MotionEvent): Boolean = onControllerTouchEvent(ev)

    override fun onControllerTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            val child = composeView
            val location = IntArray(2)
            child.getLocationInWindow(location)
            // Dismiss silently if user taps in the upper drawer area above the bottom banner.
            val bannerTopThreshold = height - (180f * resources.displayMetrics.density)
            if (ev.y < bannerTopThreshold) {
                close(true)
            }
        }
        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean = false

    override fun setInsets(insets: Rect) {
        setPadding(insets.left, insets.top, insets.right, 0)
    }

    companion object {
        private const val DIRECT_FOCUS_THRESHOLD = 2

        fun recordDirectSearchAndMaybeShow(context: Context, targetView: AllAppsSearchInput) {
            val prefs2 = PreferenceManager2.getInstance(context)
            if (prefs2.autoShowKeyboardInDrawer.firstCached() ||
                prefs2.pinKeyboardSnackbarDismissed.firstCached()
            ) {
                return
            }
            val launcher = runCatching { context.launcher }.getOrNull() ?: return
            val nextCount = prefs2.pinKeyboardDirectFocusCount.firstCached() + 1
            launcher.lifecycleScope.launch {
                prefs2.pinKeyboardDirectFocusCount.set(nextCount)
            }
            if (nextCount >= DIRECT_FOCUS_THRESHOLD) {
                targetView.postDelayed(
                    {
                        if (targetView.isAttachedToWindow) {
                            show(context, launcher)
                        }
                    },
                    350L,
                )
            }
        }

        fun show(context: Context, activityContext: ActivityContext) {
            val prefs2 = PreferenceManager2.getInstance(context)
            if (prefs2.autoShowKeyboardInDrawer.firstCached() ||
                prefs2.pinKeyboardSnackbarDismissed.firstCached()
            ) {
                return
            }
            if (hasOpenView(activityContext, TYPE_PIN_IME_POPUP)) {
                return
            }
            val appsView = activityContext.appsView
            if (appsView.currentPage != 0) {
                return
            }
            val dragLayer = activityContext.dragLayer ?: return
            val snackBar = PinKeyboardSnackBar(context)
            dragLayer.addView(snackBar)
            snackBar.mIsOpen = true
            snackBar.alpha = 0f
            snackBar.translationY = 20f * snackBar.resources.displayMetrics.density
            snackBar.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(220L)
                .start()
        }
    }
}

@Composable
private fun PinKeyboardSnackBarContent(
    prefs2: PreferenceManager2,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 6.dp,
        ) {
            Row(
                modifier = Modifier.padding(start = 18.dp, top = 10.dp, end = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.pin_keyboard_snackbar_question),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = {
                        scope.launch {
                            prefs2.pinKeyboardSnackbarDismissed.set(true)
                        }
                        onDismiss()
                    },
                ) {
                    Text(text = stringResource(R.string.pin_keyboard_snackbar_dismiss))
                }
                Spacer(modifier = Modifier.width(4.dp))
                FilledTonalButton(
                    onClick = {
                        scope.launch {
                            prefs2.autoShowKeyboardInDrawer.set(true)
                            prefs2.pinKeyboardSnackbarDismissed.set(true)
                        }
                        onConfirm()
                    },
                ) {
                    Text(text = stringResource(R.string.pin_keyboard_snackbar_yes))
                }
            }
        }
    }
}
