package com.truckie1.app.trips

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.truckie1.app.R
import com.truckie1.app.TruckieApp
import com.truckie1.app.profiles.VehicleListActivity
import com.truckie1.flags.FlagKeys

class BlockedRouteActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blocked_route)

        val reason = intent.getStringExtra(EXTRA_REASON)
            ?: getString(R.string.blocked_route_default_reason)
        findViewById<TextView>(R.id.blockedReason).text = reason

        findViewById<Button>(R.id.editStops).setOnClickListener {
            finish()
        }
        findViewById<Button>(R.id.editVehicle).setOnClickListener {
            val app = application as TruckieApp
            if (app.featureFlags.isEnabled(FlagKeys.VEHICLE_PROFILES)) {
                startActivity(Intent(this, VehicleListActivity::class.java))
            }
            finish()
        }
    }

    companion object {
        const val EXTRA_REASON = "blocked_reason"
    }
}
