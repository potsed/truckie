package com.truckie1.app.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.truckie1.app.R
import com.truckie1.app.TruckieApp
import com.truckie1.app.ui.SettingsScreen
import com.truckie1.app.ui.TruckieHostedScreen
import com.truckie1.app.ui.navigateToPrimaryDestination
import com.truckie1.app.ui.setTruckieContent
import com.truckie1.flags.FlagKeys

/**
 * Settings stub: clearance buffer readout until full settings land.
 * Primary destination under `ff_nav_chrome`.
 */
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as TruckieApp
        val navChrome = app.featureFlags.isEnabled(FlagKeys.NAV_CHROME)
        setTruckieContent {
            TruckieHostedScreen(
                title = getString(R.string.settings),
                activitySimpleName = "SettingsActivity",
                navChromeEnabled = navChrome,
                onNavigate = { navigateToPrimaryDestination(it) },
            ) {
                SettingsScreen(bufferM = app.clearanceBufferM)
            }
        }
    }
}
