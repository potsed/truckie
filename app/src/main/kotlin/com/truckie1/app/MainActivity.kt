package com.truckie1.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.truckie1.app.ui.PrimaryNavigation
import com.truckie1.app.ui.TruckieRootApp
import com.truckie1.app.ui.TruckieRoutes
import com.truckie1.app.ui.setTruckieContent

/**
 * Sole launcher Activity. Always hosts the Compose NavHost shell
 * ([TruckieRootApp]) — no multi-Activity redirects (ATNA-74).
 */
class MainActivity : ComponentActivity() {
    private var pendingRoute by mutableStateOf(TruckieRoutes.HOME)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as TruckieApp
        pendingRoute = PrimaryNavigation.resolveStartRoute(intent?.getStringExtra(EXTRA_ROUTE))
        setTruckieContent {
            TruckieRootApp(
                app = app,
                activity = this@MainActivity,
                startRoute = pendingRoute,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingRoute = PrimaryNavigation.resolveStartRoute(intent.getStringExtra(EXTRA_ROUTE))
    }

    companion object {
        const val EXTRA_ROUTE = "truckie_route"
    }
}
