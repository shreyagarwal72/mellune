package app.mellune.search.algorithms.data

import android.content.Intent
import androidx.annotation.DrawableRes

/**
 * Domain model representing an actionable system quick-toggle tile in drawer search.
 */
data class QuickToggleInfo(
    val id: String,
    val title: String,
    val subtitle: String,
    @DrawableRes val iconRes: Int,
    val intent: Intent,
    val directActionKey: String? = null,
)
