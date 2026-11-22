package com.truckie1.app.ui

import android.app.Activity
import android.content.Intent
import com.truckie1.app.MainActivity
import com.truckie1.app.TruckieApp
import com.truckie1.flags.FlagKeys

/**
 * When [FlagKeys.NAV_CHROME] is on, secondary/legacy Activities hand off to the
 * single [MainActivity] NavHost so TopAppBar + bottom bar stay the global shell.
 *
 * @return true if this Activity should [Activity.finish] immediately.
 */
fun Activity.redirectToChromeHostIfEnabled(route: String): Boolean {
    val app = application as? TruckieApp ?: return false
    if (!app.featureFlags.isEnabled(FlagKeys.NAV_CHROME)) return false
    startActivity(
        Intent(this, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_ROUTE, route)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
    )
    return true
}
