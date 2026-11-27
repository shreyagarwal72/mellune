package app.mellune.smartspace

import android.os.Bundle
import androidx.activity.ComponentActivity
import app.mellune.ui.preferences.PreferenceActivity
import app.mellune.ui.preferences.navigation.SmartspaceWidget

class SmartspacePreferencesShortcut : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(PreferenceActivity.createIntent(this, SmartspaceWidget))
        finish()
    }
}
