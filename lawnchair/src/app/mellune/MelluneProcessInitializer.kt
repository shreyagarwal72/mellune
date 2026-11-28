package app.mellune

import android.content.Context
import androidx.annotation.Keep
import app.mellune.bugreport.MelluneBugReporter
import app.mellune.theme.color.tokens.ColorTokens
import com.android.launcher3.Utilities
import com.android.launcher3.icons.mono.ThemedIconDrawable
import com.android.quickstep.QuickstepProcessInitializer

@Keep
class MelluneProcessInitializer(context: Context) : QuickstepProcessInitializer(context) {

    override fun init(context: Context) {
        MelluneBugReporter.INSTANCE.get(context)
        ensureColorsLoader()
        super.init(context)
    }

    companion object {
        /**
         * Installs the default monochrome icon palette loader when not yet registered.
         */
        fun ensureColorsLoader() {
            if (ThemedIconDrawable.COLORS_LOADER != null) return
            ThemedIconDrawable.COLORS_LOADER = {
                if (Utilities.isDarkTheme(it)) {
                    intArrayOf(
                        ColorTokens.Accent2_800.resolveColor(it),
                        ColorTokens.Accent1_200.resolveColor(it),
                    )
                } else {
                    intArrayOf(
                        ColorTokens.Accent1_100.resolveColor(it),
                        ColorTokens.Accent1_700.resolveColor(it),
                    )
                }
            }
        }
    }
}
