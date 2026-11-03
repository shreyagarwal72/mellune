package app.lawnchair.qsb.providers

import app.lawnchair.qsb.ThemingMethod
import com.android.launcher3.R

data object SearXNG : QsbSearchProvider(
    id = "searxng",
    name = R.string.search_provider_searxng,
    icon = R.drawable.ic_search,
    themedIcon = R.drawable.ic_search,
    themingMethod = ThemingMethod.TINT,
    packageName = "",
    action = "",
    website = "https://searx.be/search?q=",
)
