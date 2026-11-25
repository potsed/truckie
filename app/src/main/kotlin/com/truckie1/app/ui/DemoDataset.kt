package com.truckie1.app.ui

import com.truckie1.profiles.Vehicle
import com.truckie1.trips.Route
import com.truckie1.trips.RouteEndpoint
import com.truckie1.trips.RouteStatus
import com.truckie1.trips.RouteStop
import com.truckie1.trips.StopKind
import java.time.LocalDate
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class DemoStopAreaKind {
    INDUSTRIAL,
    RESIDENTIAL,
}

/**
 * Fixed SEQ demo run for `ff_demo_mode` (ATNA-56): DHL Boronia Rd depot, six stops
 * (2 pickup / 4 delivery; industrial Yatala/Lytton-style + one residential near a
 * verified low bridge), and a 3.5 m demo vehicle. Used for route-optimisation and
 * W5-8 / clearance-filter dogfood.
 *
 * Gherkin: features/demo_mode.feature
 */
object DemoDataset {
    const val ROUTE_ID: String = "demo-route-seq-dhl"
    const val ROUTE_NAME: String = "Demo SEQ run"
    const val VEHICLE_REGO: String = "DEMO350"

    /**
     * QR SEQ underbridge next to the residential demo stop
     * (`qld-qr-wynnum-colina-street-berrima-street`, 1.9 m) — conflicts with the
     * 3.5 m demo vehicle so W5-8 filtering is exercisable on the demo route.
     */
    const val NEAR_LOW_BRIDGE_ID: String = "qld-qr-wynnum-colina-street-berrima-street"
    const val NEAR_LOW_BRIDGE_NAME: String = "Colina Street / Berrima Street (Wynnum)"
    const val NEAR_LOW_BRIDGE_CLEARANCE_M: Double = 1.9
    const val NEAR_LOW_BRIDGE_LAT: Double = -27.443741
    const val NEAR_LOW_BRIDGE_LNG: Double = 153.168832
    /** Max distance (metres) for a stop to count as "near" a low bridge. */
    const val NEAR_LOW_BRIDGE_MAX_M: Double = 250.0

    /** DHL Express, 1A Boronia Road, Brisbane Airport QLD 4008. */
    val depot: RouteEndpoint = RouteEndpoint(
        addressLabel = "DHL Express, 1A Boronia Road, Brisbane Airport QLD 4008",
        latitude = -27.419949,
        longitude = 153.099544,
    )

    val vehicle: Vehicle = Vehicle(
        rego = VEHICLE_REGO,
        nickname = "Demo 3.5 m",
        vehicleType = "Rigid",
        make = "Demo",
        model = "Sample",
        year = 2024,
        heightM = 3.5,
        widthM = 2.5,
        lengthM = 8.0,
        massT = 8.0,
        tareT = 5.0,
        gvmT = 12.0,
        gcmT = 12.0,
        axleCount = 2,
        notes = "Demo mode only — not a real vehicle",
    )

    data class DemoStop(
        val id: String,
        val addressLabel: String,
        val suburb: String,
        val latitude: Double,
        val longitude: Double,
        val kind: StopKind,
        val areaKind: DemoStopAreaKind,
        /** When true, stop is within [NEAR_LOW_BRIDGE_MAX_M] of [NEAR_LOW_BRIDGE_ID]. */
        val nearLowBridge: Boolean = false,
    )

    val stops: List<DemoStop> = listOf(
        DemoStop(
            id = "demo-stop-pickup-lytton",
            addressLabel = "42 South Street, Lytton QLD 4178",
            suburb = "Lytton",
            latitude = -27.4168,
            longitude = 153.1542,
            kind = StopKind.PICKUP_LOAD,
            areaKind = DemoStopAreaKind.INDUSTRIAL,
        ),
        DemoStop(
            id = "demo-stop-pickup-yatala",
            addressLabel = "18 Christensen Road, Yatala QLD 4207",
            suburb = "Yatala",
            latitude = -27.7665,
            longitude = 153.2368,
            kind = StopKind.PICKUP_LOAD,
            areaKind = DemoStopAreaKind.INDUSTRIAL,
        ),
        DemoStop(
            id = "demo-stop-delivery-eagle-farm",
            addressLabel = "55 Schneider Road, Eagle Farm QLD 4009",
            suburb = "Eagle Farm",
            latitude = -27.4305,
            longitude = 153.0912,
            kind = StopKind.DROP_OFF_UNLOAD,
            areaKind = DemoStopAreaKind.INDUSTRIAL,
        ),
        DemoStop(
            id = "demo-stop-delivery-hemmant",
            addressLabel = "120 Tingalpa Road, Hemmant QLD 4174",
            suburb = "Hemmant",
            latitude = -27.4502,
            longitude = 153.1295,
            kind = StopKind.DROP_OFF_UNLOAD,
            areaKind = DemoStopAreaKind.INDUSTRIAL,
        ),
        DemoStop(
            id = "demo-stop-delivery-acacia-ridge",
            addressLabel = "25 Beatty Road, Acacia Ridge QLD 4110",
            suburb = "Acacia Ridge",
            latitude = -27.5861,
            longitude = 153.0264,
            kind = StopKind.DROP_OFF_UNLOAD,
            areaKind = DemoStopAreaKind.INDUSTRIAL,
        ),
        // Residential delivery beside QR Colina St / Berrima St underbridge (1.9 m).
        DemoStop(
            id = "demo-stop-delivery-wynnum-residential",
            addressLabel = "8 Colina Street, Wynnum QLD 4178",
            suburb = "Wynnum",
            latitude = -27.44395,
            longitude = 153.16905,
            kind = StopKind.DROP_OFF_UNLOAD,
            areaKind = DemoStopAreaKind.RESIDENTIAL,
            nearLowBridge = true,
        ),
    )

    fun isDemoVehicleRego(rego: String): Boolean =
        Vehicle.normalizeRego(rego) == Vehicle.normalizeRego(VEHICLE_REGO)

    fun isDemoRouteId(routeId: String): Boolean = routeId == ROUTE_ID

    fun stopsNearLowBridge(): List<DemoStop> = stops.filter { it.nearLowBridge }

    /** Haversine distance in metres between a stop and the curated low bridge. */
    fun distanceToNearLowBridgeM(stop: DemoStop): Double =
        haversineM(
            stop.latitude,
            stop.longitude,
            NEAR_LOW_BRIDGE_LAT,
            NEAR_LOW_BRIDGE_LNG,
        )

    fun buildRoute(today: LocalDate = LocalDate.now()): Route =
        Route(
            id = ROUTE_ID,
            name = ROUTE_NAME,
            dateEpochDay = today.toEpochDay(),
            status = RouteStatus.ACTIVE,
            start = depot,
            end = depot,
            stops = stops.mapIndexed { index, stop ->
                RouteStop(
                    id = stop.id,
                    addressLabel = stop.addressLabel,
                    latitude = stop.latitude,
                    longitude = stop.longitude,
                    kind = stop.kind,
                    order = index,
                )
            },
        )

    private fun haversineM(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
    ): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
