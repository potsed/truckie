package com.truckie1.routing

import com.truckie1.trips.ClearanceBuffer
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Canonical bridge passage relation — shared by map filters and routing (ATNA-73).
 *
 * Replaces twin app [BridgePassage] / routing [RoutingBridgePassage] enums with
 * one type. App code may typealias this as `BridgePassage`.
 *
 * - [UNDER] — path goes under (rail underbridges, underpasses, signed under clearance)
 * - [OVER] — drive across the deck (river road bridge roadway)
 * - [BOTH] — deck is driveable over, and a published under-path clearance exists
 * - [UNKNOWN] — not classified without inventing data
 */
enum class BridgePassageRelation {
    UNDER,
    OVER,
    BOTH,
    UNKNOWN,
    ;

    fun wireName(): String = name.lowercase()

    /** True when an under-path clearance restriction may apply. */
    fun restrictsUnder(): Boolean = this == UNDER || this == BOTH

    companion object {
        fun fromWire(raw: String?): BridgePassageRelation {
            val key = raw?.trim()?.lowercase().orEmpty()
            return entries.firstOrNull { it.wireName() == key } ?: UNKNOWN
        }
    }
}

/**
 * Compatibility alias — same type as [BridgePassageRelation] (no twin enum).
 */
typealias RoutingBridgePassage = BridgePassageRelation

/**
 * Single shared passage / conflict policy (ATNA-73).
 *
 * Home-map filters and [MultiStopTourComposer] / [BridgePassageConstraint] all
 * reuse this object — one semantics, no duplex height-conflict rules.
 *
 * Routing rule (Daniel): **OK to drive over** a low bridge deck; **not OK to
 * drive under** when clearance conflicts with vehicle height + buffer.
 *
 * Gherkin: features/bridge_passage_pipeline.feature
 */
object BridgePassagePolicy {
    /** Metres — treat a path vertex within this radius as "near" the structure. */
    const val NEAR_PATH_M: Double = 80.0

    const val ROUTING_RULE: String =
        "OK to drive over a low bridge deck; not OK to drive under it when " +
            "clearance conflicts with vehicle height + system buffer."

    fun restrictsUnder(passage: BridgePassageRelation): Boolean =
        passage.restrictsUnder()

    /**
     * Whether a height conflict blocks travel for the given path relation.
     *
     * - Path [BridgePassageRelation.OVER]: never blocked by under clearance.
     * - Path [BridgePassageRelation.UNDER]: blocked when the structure restricts
     *   under and published clearance is below vehicle height + buffer.
     * Missing heights never invent a value.
     */
    fun heightConflictBlocksPath(
        clearanceHeightM: Double?,
        structurePassage: BridgePassageRelation,
        vehicleHeightM: Double,
        bufferM: Double,
        pathRelation: BridgePassageRelation,
    ): Boolean {
        if (pathRelation == BridgePassageRelation.OVER) return false
        if (!structurePassage.restrictsUnder()) return false
        val clearance = clearanceHeightM ?: return false
        val threshold = vehicleHeightM + ClearanceBuffer.clamp(bufferM)
        return clearance < threshold
    }

    fun bridgesRestrictingUnder(
        bridges: List<RoutingBridgeConstraint>,
    ): List<RoutingBridgeConstraint> =
        bridges.filter { restrictsUnder(it.passage) }

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
                    clearanceHeightM = bridge.clearanceHeightM,
                    structurePassage = bridge.passage,
                    vehicleHeightM = vehicleHeightM,
                    bufferM = bufferM,
                    pathRelation = BridgePassageRelation.UNDER,
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
