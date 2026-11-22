package com.truckie1.app.profiles

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.truckie1.app.R
import com.truckie1.app.TruckieApp
import com.truckie1.flags.FlagKeys
import com.truckie1.profiles.Vehicle

class VehicleListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as TruckieApp
        if (!app.featureFlags.isEnabled(FlagKeys.VEHICLE_PROFILES)) {
            Toast.makeText(this, getString(R.string.flag_off, FlagKeys.VEHICLE_PROFILES), Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        setContentView(R.layout.activity_vehicle_list)

        findViewById<Button>(R.id.addVehicle).setOnClickListener {
            startActivity(Intent(this, VehicleEditActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        bindList()
    }

    private fun bindList() {
        val app = application as TruckieApp
        val vehicles = app.vehicleProfiles.all()
        val list = findViewById<ListView>(R.id.vehicleList)
        val empty = findViewById<TextView>(R.id.emptyState)
        if (vehicles.isEmpty()) {
            list.visibility = View.GONE
            empty.visibility = View.VISIBLE
            return
        }
        empty.visibility = View.GONE
        list.visibility = View.VISIBLE
        list.adapter = VehicleRowAdapter(
            vehicles = vehicles,
            activeRego = app.vehicleProfiles.activeRego,
            onSelect = { vehicle ->
                app.vehicleProfiles.setActive(vehicle.rego)
                bindList()
                Toast.makeText(this, getString(R.string.active_set, vehicle.rego), Toast.LENGTH_SHORT).show()
            },
            onEdit = { vehicle ->
                startActivity(
                    Intent(this, VehicleEditActivity::class.java).putExtra(
                        VehicleEditActivity.EXTRA_REGO,
                        vehicle.rego,
                    ),
                )
            },
        )
    }

    private class VehicleRowAdapter(
        private val vehicles: List<Vehicle>,
        private val activeRego: String?,
        private val onSelect: (Vehicle) -> Unit,
        private val onEdit: (Vehicle) -> Unit,
    ) : BaseAdapter() {
        override fun getCount(): Int = vehicles.size
        override fun getItem(position: Int): Vehicle = vehicles[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(parent.context)
                .inflate(R.layout.item_vehicle_row, parent, false)
            val vehicle = getItem(position)
            val isActive = vehicle.rego == activeRego
            view.findViewById<TextView>(R.id.activeMarker).text = if (isActive) "★" else " "
            view.findViewById<TextView>(R.id.rowTitle).text =
                "${vehicle.rego}  ${vehicle.vehicleType}  ${"%.1f".format(vehicle.heightM)}m"
            view.findViewById<TextView>(R.id.rowSubtitle).text =
                "${vehicle.nickname} · ${vehicle.make} ${vehicle.model} (${vehicle.year})"
            view.setOnClickListener { onSelect(vehicle) }
            view.findViewById<Button>(R.id.editVehicle).setOnClickListener { onEdit(vehicle) }
            return view
        }
    }
}
