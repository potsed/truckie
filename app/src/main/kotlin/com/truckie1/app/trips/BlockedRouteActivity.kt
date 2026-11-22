package com.truckie1.app.trips

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.truckie1.app.R
import com.truckie1.app.TruckieApp
import com.truckie1.app.profiles.VehicleListActivity
import com.truckie1.app.ui.BlockedRouteScreen
import com.truckie1.app.ui.TruckieHostedScreen
import com.truckie1.app.ui.navigateToPrimaryDestination
import com.truckie1.app.ui.setTruckieContent
import com.truckie1.flags.FlagKeys

class BlockedRouteActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val reason = intent.getStringExtra(EXTRA_REASON)
            ?: getString(R.string.blocked_route_default_reason)
        val navChrome = (application as TruckieApp).featureFlags.isEnabled(FlagKeys.NAV_CHROME)
        setTruckieContent {
            TruckieHostedScreen(
                title = getString(R.string.blocked_route_title),
                activitySimpleName = "BlockedRouteActivity",
                navChromeEnabled = navChrome,
                onNavigate = { navigateToPrimaryDestination(it) },
                onNavigateUp = if (navChrome) ({ finish() }) else null,
            ) {
                BlockedRouteScreen(
                    reason = reason,
                    onEditStops = { finish() },
                    onEditVehicle = {
                        val app = application as TruckieApp
                        if (app.featureFlags.isEnabled(FlagKeys.VEHICLE_PROFILES)) {
                            startActivity(Intent(this@BlockedRouteActivity, VehicleListActivity::class.java))
                        }
                        finish()
                    },
                )
            }
        }
    }

    companion object {
        const val EXTRA_REASON = "blocked_reason"
    }
}
