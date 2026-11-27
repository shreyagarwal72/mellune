package app.mellune.gestures.handlers

import android.content.Context
import app.mellune.MelluneLauncher
import app.mellune.preferences2.PreferenceManager2
import app.mellune.qsb.LawnQsbLayout

class OpenSearchGestureHandler(context: Context) : GestureHandler(context) {

    override suspend fun onTrigger(launcher: MelluneLauncher) {
        val prefs = PreferenceManager2.getInstance(launcher)
        val searchProvider = LawnQsbLayout.getSearchProvider(launcher, prefs)
        searchProvider.launch(launcher)
    }
}
