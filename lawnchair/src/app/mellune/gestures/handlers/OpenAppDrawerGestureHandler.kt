package app.mellune.gestures.handlers

import android.content.Context
import app.mellune.MelluneLauncher
import app.mellune.animateToAllApps

class OpenAppDrawerGestureHandler(context: Context) : GestureHandler(context) {

    override suspend fun onTrigger(launcher: MelluneLauncher) {
        launcher.animateToAllApps()
    }
}
