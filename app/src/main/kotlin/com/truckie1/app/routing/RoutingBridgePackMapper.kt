package com.truckie1.app.routing

import com.truckie1.app.ui.BridgePassage
import com.truckie1.app.ui.BridgePoint
import com.truckie1.routing.RoutingBridgeConstraint
import com.truckie1.routing.RoutingBridgePassage

/**
 * Maps on-device pack [BridgePoint] rows into routing-layer constraints for
 * [com.truckie1.routing.MultiStopTourComposer] (ATNA-71).
 *
 * Thin pack → routing bridge only. Does **not** own BridgePassage UI policy
 * (sibling worker) or pack version merge (sibling worker).
 */
object RoutingBridgePackMapper {

    fun toRoutingConstraints(
        packBridges: List<BridgePoint>,
    ): List<RoutingBridgeConstraint> = packBridges.map { bridge ->
        RoutingBridgeConstraint(
            id = bridge.id,
            latitude = bridge.latitude,
            longitude = bridge.longitude,
            clearanceHeightM = bridge.clearanceHeightM,
            passage = bridge.passage.toRouting(),
        )
    }

    /**
     * When [bridgePassageEnabled] (`ff_bridge_passage`) is off, the composer
     * must not receive pack bridges (under-passage hard-block stays dormant).
     */
    fun bridgesForComposer(
        packBridges: List<BridgePoint>,
        bridgePassageEnabled: Boolean,
    ): List<RoutingBridgeConstraint> =
        if (bridgePassageEnabled) toRoutingConstraints(packBridges) else emptyList()

    private fun BridgePassage.toRouting(): RoutingBridgePassage = when (this) {
        BridgePassage.UNDER -> RoutingBridgePassage.UNDER
        BridgePassage.OVER -> RoutingBridgePassage.OVER
        BridgePassage.BOTH -> RoutingBridgePassage.BOTH
        BridgePassage.UNKNOWN -> RoutingBridgePassage.UNKNOWN
    }
}
