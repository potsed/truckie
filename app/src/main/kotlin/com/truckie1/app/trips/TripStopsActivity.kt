package com.truckie1.app.trips

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.truckie1.app.R
import com.truckie1.app.TruckieApp
import com.truckie1.flags.FlagKeys
import com.truckie1.routing.OptimizeResult
import com.truckie1.trips.ClearanceCalculator
import com.truckie1.trips.OptimizeMode
import com.truckie1.trips.TripStop
import com.truckie1.trips.TripStopSummary

class TripStopsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as TruckieApp
        if (!app.featureFlags.isEnabled(FlagKeys.MULTI_STOP)) {
            Toast.makeText(this, getString(R.string.flag_off, FlagKeys.MULTI_STOP), Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        setContentView(R.layout.activity_trip_stops)

        findViewById<Button>(R.id.addStop).setOnClickListener {
            startActivity(Intent(this, AddStopActivity::class.java))
        }

        val optimizePanel = findViewById<LinearLayout>(R.id.optimizePanel)
        if (OptimizeForVehicleGate.isAvailable(app.featureFlags)) {
            optimizePanel.visibility = View.VISIBLE
            findViewById<Button>(R.id.runOptimize).setOnClickListener { runOptimize() }
        } else {
            optimizePanel.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        bindList()
    }

    private fun runOptimize() {
        val app = application as TruckieApp
        if (!OptimizeForVehicleGate.isAvailable(app.featureFlags)) {
            Toast.makeText(this, getString(R.string.flag_off, FlagKeys.ROUTE_OPTIMIZE), Toast.LENGTH_SHORT).show()
            return
        }
        val vehicle = app.vehicleProfiles.activeRego?.let { app.vehicleProfiles.findByRego(it) }
        if (vehicle == null) {
            Toast.makeText(this, getString(R.string.optimize_need_vehicle), Toast.LENGTH_SHORT).show()
            return
        }
        val stops = app.tripSession.stops()
        if (stops.isEmpty()) {
            Toast.makeText(this, getString(R.string.optimize_need_stops), Toast.LENGTH_SHORT).show()
            return
        }

        val mode = if (findViewById<RadioButton>(R.id.modeCheapest).isChecked) {
            OptimizeMode.CHEAPEST
        } else {
            OptimizeMode.FASTEST
        }

        when (
            val result = app.routeOptimizer.optimize(
                stops = stops,
                vehicle = vehicle,
                bufferM = app.clearanceBufferM,
                mode = mode,
            )
        ) {
            is OptimizeResult.Ok -> {
                app.tripSession.replaceStops(result.orderedStops)
                app.tripSession.lastOptimizeMode = result.mode
                findViewById<TextView>(R.id.optimizeStatus).text = getString(
                    R.string.optimize_success,
                    result.mode.name.lowercase().replaceFirstChar { it.titlecase() },
                    result.route.durationSeconds,
                )
                bindList()
            }
            is OptimizeResult.Blocked -> {
                startActivity(
                    Intent(this, BlockedRouteActivity::class.java)
                        .putExtra(BlockedRouteActivity.EXTRA_REASON, result.reason),
                )
            }
        }
    }

    private fun bindList() {
        val app = application as TruckieApp
        val stops = app.tripSession.stops()
        val list = findViewById<ListView>(R.id.stopList)
        val empty = findViewById<TextView>(R.id.emptyStops)
        val clearance = findViewById<TextView>(R.id.clearanceSummary)

        clearance.text = clearanceText(app, stops)

        if (stops.isEmpty()) {
            list.visibility = View.GONE
            empty.visibility = View.VISIBLE
            return
        }
        empty.visibility = View.GONE
        list.visibility = View.VISIBLE
        list.adapter = StopRowAdapter(
            stops = stops,
            onMoveUp = { index ->
                if (index > 0) {
                    app.tripSession.moveStop(index, index - 1)
                    bindList()
                }
            },
            onMoveDown = { index ->
                if (index < stops.lastIndex) {
                    app.tripSession.moveStop(index, index + 1)
                    bindList()
                }
            },
            onDelete = { stop ->
                app.tripSession.removeStop(stop.id)
                bindList()
            },
        )
    }

    private fun clearanceText(app: TruckieApp, stops: List<TripStop>): String {
        val vehicle = app.vehicleProfiles.activeRego?.let { app.vehicleProfiles.findByRego(it) }
        if (vehicle == null) return getString(R.string.set_vehicle_for_clearance)
        val effective = if (app.featureFlags.isEnabled(FlagKeys.STOP_CARGO)) {
            ClearanceCalculator.maxEffectiveHeightAlongTripM(
                vehicleHeightM = vehicle.heightM,
                stops = stops,
                bufferM = app.clearanceBufferM,
            )
        } else {
            ClearanceCalculator.effectiveHeightM(
                vehicleHeightM = vehicle.heightM,
                cargoHeightsM = emptyList(),
                bufferM = app.clearanceBufferM,
            )
        }
        return getString(R.string.trip_eff_clearance, "%.2f".format(effective))
    }

    private class StopRowAdapter(
        private val stops: List<TripStop>,
        private val onMoveUp: (Int) -> Unit,
        private val onMoveDown: (Int) -> Unit,
        private val onDelete: (TripStop) -> Unit,
    ) : BaseAdapter() {
        override fun getCount(): Int = stops.size
        override fun getItem(position: Int): TripStop = stops[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(parent.context)
                .inflate(R.layout.item_trip_stop_row, parent, false)
            val stop = getItem(position)
            view.findViewById<TextView>(R.id.stopTitle).text =
                TripStopSummary.title(position, stop)
            view.findViewById<Button>(R.id.moveUp).setOnClickListener { onMoveUp(position) }
            view.findViewById<Button>(R.id.moveDown).setOnClickListener { onMoveDown(position) }
            view.findViewById<Button>(R.id.deleteStop).setOnClickListener { onDelete(stop) }
            return view
        }
    }
}
