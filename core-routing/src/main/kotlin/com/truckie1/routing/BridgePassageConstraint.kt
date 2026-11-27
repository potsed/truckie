package com.truckie1.routing

import com.truckie1.trips.ClearanceBuffer
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Routing-layer bridge passage (mirrors ATNA-48 / home-map BridgePassage).
 *
 * - [UNDER] — path goes under (rail underbridges, underpasses)
 * - [OVER] — drive across the deck
 * - [BOTH] — deck OK over; published under clearance may also apply
 * - [UNKNOWN] — unclassified
 */
enum class RoutingBridgePassage {
    UNDER,
    OVER,
    BOTH,
    UNKNOWN,
    ;

    fun restrictsUnder(): Boolean = this == UNDER || this == BOTH
}

/**
 * Minimal bridge constraint for GraphHopper tour filtering (no app/UI types).
 */
data class RoutingBridgeConstraint(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val clearanceHeightM: Double?,
    val passage: RoutingBridgePassage,
)

/**
 * Hard-block **under** when clearance &lt; vehicle height + buffer; **over** deck OK.
 *
 * Gherkin: features/route_optimisation_graphhopper.feature
 */
object BridgePassageConstraint {
    /** Metres — treat a path vertex within this radius as "near" the structure. */
    const val NEAR_PATH_M: Double = 80.0

    const val ROUTING_RULE: String =
        "OK to drive over a low bridge deck; not OK to drive under it when " +
            "clearance conflicts with vehicle height + system buffer."

    fun restrictsUnderPassage(bridge: RoutingBridgeConstraint): Boolean =
        bridge.passage.restrictsUnder()

    fun bridgesRestrictingUnder(
        bridges: List<RoutingBridgeConstraint>,
    ): List<RoutingBridgeConstraint> = bridges.filter { restrictsUnderPassage(it) }

    /**
     * Whether a height conflict blocks travel for the given path relation.
     * Missing heights never invent a value.
     */
    fun heightConflictBlocksPath(
        bridge: RoutingBridgeConstraint,
        vehicleHeightM: Double,
        bufferM: Double,
        pathRelation: RoutingBridgePassage,
    ): Boolean {
        if (pathRelation == RoutingBridgePassage.OVER) return false
        if (!restrictsUnderPassage(bridge)) return false
        val clearance = bridge.clearanceHeightM ?: return false
        val threshold = vehicleHeightM + ClearanceBuffer.clamp(bufferM)
        return clearance < threshold
    }

    /**
     * True when any under-restricting low bridge lies near [points] and conflicts
     * with vehicle height + buffer (assumes under path relation along the tour).
     */
    fun pathConflictsWithUnderBridges(
        points: List<LatLngPoint>,
        bridges: List<RoutingBridgeConstraint>,
        vehicleHeightM: Double,
        bufferM: Double,
        nearPathM: Double = NEAR_PATH_M,
    ): Boolean {
        if (points.isEmpty()) return false
        for (bridge in bridgesRestrictingUnder(bridges)) {
            if (!heightConflictBlocksPath(
                    bridge = bridge,
                    vehicleHeightM = vehicleHeightM,
                    bufferM = bufferM,
                    pathRelation = RoutingBridgePassage.UNDER,
                )
            ) {
                continue
            }
            if (points.any { pointNearBridge(it, bridge, nearPathM) }) {
                return true
            }
        }
        return false
    }

    private fun pointNearBridge(
        point: LatLngPoint,
        bridge: RoutingBridgeConstraint,
        nearPathM: Double,
    ): Boolean = haversineM(
        point.latitude,
        point.longitude,
        bridge.latitude,
        bridge.longitude,
    ) <= nearPathM

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
