package com.truckie1.app.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.truckie1.app.R
import com.truckie1.app.TruckieApp
import com.truckie1.app.ui.SettingsScreen
import com.truckie1.app.ui.TruckieHostedScreen
import com.truckie1.app.ui.TruckieRoutes
import com.truckie1.app.ui.navigateToPrimaryDestination
import com.truckie1.app.ui.redirectToChromeHostIfEnabled
import com.truckie1.app.ui.setTruckieContent

/**
 * Legacy Settings host when `ff_nav_chrome` is off.
 * With chrome on, [com.truckie1.app.MainActivity] NavHost owns Settings.
 */
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as TruckieApp
        if (redirectToChromeHostIfEnabled(TruckieRoutes.SETTINGS)) {
            finish()
            return
        }
        setTruckieContent {
            TruckieHostedScreen(
                title = getString(R.string.settings),
                activitySimpleName = "SettingsActivity",
                navChromeEnabled = false,
                onNavigate = { navigateToPrimaryDestination(it) },
            ) {
                SettingsScreen(bufferM = app.clearanceBufferM)
            }
        }
    }
}
