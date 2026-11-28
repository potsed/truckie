package com.truckie1.routing

/**
 * Minimal bridge constraint for GraphHopper tour filtering (no app/UI types).
 *
 * Passage semantics live on [BridgePassagePolicy] — this type is the routing
 * wire shape only (ATNA-73).
 */
data class RoutingBridgeConstraint(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val clearanceHeightM: Double?,
    val passage: BridgePassageRelation,
)

/**
 * Routing facade over [BridgePassagePolicy] (ATNA-73).
 *
 * Hard-block **under** when clearance &lt; vehicle height + buffer; **over** deck OK.
 * [MultiStopTourComposer] calls through here; map filters use the same policy.
 *
 * Gherkin: features/bridge_passage_pipeline.feature,
 * features/route_optimisation_graphhopper.feature
 */
object BridgePassageConstraint {
    /** Metres — treat a path vertex within this radius as "near" the structure. */
    const val NEAR_PATH_M: Double = BridgePassagePolicy.NEAR_PATH_M

    const val ROUTING_RULE: String = BridgePassagePolicy.ROUTING_RULE

    fun restrictsUnderPassage(bridge: RoutingBridgeConstraint): Boolean =
        BridgePassagePolicy.restrictsUnder(bridge.passage)

    fun bridgesRestrictingUnder(
        bridges: List<RoutingBridgeConstraint>,
    ): List<RoutingBridgeConstraint> =
        BridgePassagePolicy.bridgesRestrictingUnder(bridges)

    /**
     * Whether a height conflict blocks travel for the given path relation.
     * Missing heights never invent a value. Delegates to [BridgePassagePolicy].
     */
    fun heightConflictBlocksPath(
        bridge: RoutingBridgeConstraint,
        vehicleHeightM: Double,
        bufferM: Double,
        pathRelation: BridgePassageRelation,
    ): Boolean =
        BridgePassagePolicy.heightConflictBlocksPath(
            clearanceHeightM = bridge.clearanceHeightM,
            structurePassage = bridge.passage,
            vehicleHeightM = vehicleHeightM,
            bufferM = bufferM,
            pathRelation = pathRelation,
        )

    /** See [BridgePassagePolicy.ALLOWED_STOP_NEAR_M]. */
    const val ALLOWED_STOP_NEAR_M: Double = BridgePassagePolicy.ALLOWED_STOP_NEAR_M

    /**
     * True when any under-restricting low bridge lies near [points] and conflicts
     * with vehicle height + buffer (assumes under path relation along the tour).
     *
     * [allowedNearPoints] — intentional stops; proximity there is delivery, not
     * through under-passage (see [BridgePassagePolicy.pathConflictsWithUnderBridges]).
     */
    fun pathConflictsWithUnderBridges(
        points: List<LatLngPoint>,
        bridges: List<RoutingBridgeConstraint>,
        vehicleHeightM: Double,
        bufferM: Double,
        nearPathM: Double = NEAR_PATH_M,
        allowedNearPoints: List<LatLngPoint> = emptyList(),
        allowedNearM: Double = ALLOWED_STOP_NEAR_M,
    ): Boolean =
        BridgePassagePolicy.pathConflictsWithUnderBridges(
            points = points,
            bridges = bridges,
            vehicleHeightM = vehicleHeightM,
            bufferM = bufferM,
            nearPathM = nearPathM,
            allowedNearPoints = allowedNearPoints,
            allowedNearM = allowedNearM,
        )
}
