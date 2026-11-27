package com.truckie1.app.ui

import android.app.Activity
import android.content.Intent
import com.truckie1.app.MainActivity
import com.truckie1.app.TruckieApp

/**
 * When the chrome host is active (`ff_nav_chrome` and/or `ff_map_home`),
 * secondary/legacy Activities hand off to the single [MainActivity] NavHost
 * via [PrimaryNavigation] — one gate shared with MainActivity.
 *
 * @return true if this Activity should [Activity.finish] immediately.
 */
fun Activity.redirectToChromeHostIfEnabled(route: String): Boolean {
    val app = application as? TruckieApp ?: return false
    if (!PrimaryNavigation.shouldUseChromeHost(app.featureFlags)) return false
    startActivity(
        Intent(this, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_ROUTE, route)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
    )
    return true
}
