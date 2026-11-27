package app.mellune.search

import android.content.SearchRecentSuggestionsProvider
import com.android.launcher3.BuildConfig

class MelluneRecentSuggestionProvider : SearchRecentSuggestionsProvider() {
    companion object {
        const val AUTHORITY = BuildConfig.APPLICATION_ID + ".search.MelluneRecentSuggestionProvider"
        const val MODE = DATABASE_MODE_QUERIES
    }

    init {
        setupSuggestions(AUTHORITY, MODE)
    }
}
