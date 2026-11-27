package com.truckie1.app.ui

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import com.truckie1.app.MainActivity
import com.truckie1.app.R
import com.truckie1.app.TruckieApp
import com.truckie1.app.profiles.VehicleListActivity
import com.truckie1.app.settings.SettingsActivity
import com.truckie1.app.trips.TripStopsActivity
import com.truckie1.flags.FlagKeys

/**
 * Cross-activity navigation for the legacy multi-Activity path only.
 * When [PrimaryNavigation.shouldUseChromeHost] is true, [TruckieRootApp] owns
 * primary tabs via [PrimaryNavigation] inside the single-activity NavHost;
 * legacy Activities should [redirectToChromeHostIfEnabled] instead.
 */
fun Activity.navigateToPrimaryDestination(destination: TruckiePrimaryDestination) {
    val app = application as TruckieApp
    when (destination) {
        TruckiePrimaryDestination.HOME -> {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_ROUTE, TruckieRoutes.HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            )
        }
        TruckiePrimaryDestination.PLAN_TRIP -> {
            if (!app.featureFlags.isEnabled(FlagKeys.MULTI_STOP)) {
                Toast.makeText(this, getString(R.string.flag_off, FlagKeys.MULTI_STOP), Toast.LENGTH_SHORT).show()
                return
            }
            if (this is TripStopsActivity) return
            startActivity(
                Intent(this, TripStopsActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            )
        }
        TruckiePrimaryDestination.VEHICLES -> {
            if (!app.featureFlags.isEnabled(FlagKeys.VEHICLE_PROFILES)) {
                Toast.makeText(this, getString(R.string.flag_off, FlagKeys.VEHICLE_PROFILES), Toast.LENGTH_SHORT).show()
                return
            }
            if (this is VehicleListActivity) return
            startActivity(
                Intent(this, VehicleListActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            )
        }
        TruckiePrimaryDestination.SETTINGS -> {
            if (this is SettingsActivity) return
            startActivity(
                Intent(this, SettingsActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            )
        }
    }
}
