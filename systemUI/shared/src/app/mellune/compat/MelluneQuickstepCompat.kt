package app.mellune.compat

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import app.mellune.compatlib.ActivityManagerCompat
import app.mellune.compatlib.ActivityOptionsCompat
import app.mellune.compatlib.QuickstepCompatFactory
import app.mellune.compatlib.RemoteTransitionCompat
import app.mellune.compatlib.eleven.QuickstepCompatFactoryVR
import app.mellune.compatlib.fifteen.QuickstepCompatFactoryVV
import app.mellune.compatlib.fourteen.QuickstepCompatFactoryVU
import app.mellune.compatlib.sixteen.QuickstepCompatFactoryVBaklava
import app.mellune.compatlib.ten.QuickstepCompatFactoryVQ
import app.mellune.compatlib.thirteen.QuickstepCompatFactoryVT
import app.mellune.compatlib.twelve.QuickstepCompatFactoryVS

object MelluneQuickstepCompat {

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
    @JvmField
    val ATLEAST_Q: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
    @JvmField
    val ATLEAST_R: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    @JvmField
    val ATLEAST_S: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    @JvmField
    val ATLEAST_T: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @JvmField
    val ATLEAST_U: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @JvmField
    val ATLEAST_V: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.BAKLAVA)
    @JvmField
    val ATLEAST_BAKLAVA: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA

    @JvmStatic
    val factory: QuickstepCompatFactory by lazy {
        when {
            ATLEAST_BAKLAVA -> QuickstepCompatFactoryVBaklava()
            ATLEAST_V -> QuickstepCompatFactoryVV()
            ATLEAST_U -> QuickstepCompatFactoryVU()
            ATLEAST_T -> QuickstepCompatFactoryVT()
            ATLEAST_S -> QuickstepCompatFactoryVS()
            ATLEAST_R -> QuickstepCompatFactoryVR()
            ATLEAST_Q -> QuickstepCompatFactoryVQ()
            else -> error("Unsupported SDK version")
        }
    }

    @JvmStatic
    val activityManagerCompat: ActivityManagerCompat by lazy { factory.activityManagerCompat }

    @JvmStatic
    val activityOptionsCompat: ActivityOptionsCompat by lazy { factory.activityOptionsCompat }

    @JvmStatic
    val remoteTransitionCompat: RemoteTransitionCompat by lazy { factory.remoteTransitionCompat }
}
